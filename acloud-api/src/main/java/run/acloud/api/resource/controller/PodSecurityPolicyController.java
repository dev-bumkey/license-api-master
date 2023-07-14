package run.acloud.api.resource.controller;

import com.google.common.collect.Lists;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import run.acloud.api.configuration.service.ClusterService;
import run.acloud.api.configuration.service.ClusterStateService;
import run.acloud.api.configuration.vo.ClusterVO;
import run.acloud.api.cserver.enums.DeployType;
import run.acloud.api.resource.constants.KubeConstants;
import run.acloud.api.resource.service.PodSecurityPolicyService;
import run.acloud.api.resource.service.RBACResourceService;
import run.acloud.api.resource.vo.*;
import run.acloud.commons.enums.ApiVersionType;
import run.acloud.framework.context.ContextHolder;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;

import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "Kubernetes PodSecurityPolicy Management", description = "쿠버네티스 PodSecurityPolicy에 대한 관리 기능을 제공한다.")
@Slf4j
@RequestMapping(value = "/api/psp")
@RestController
@Validated
public class PodSecurityPolicyController {

    @Autowired
    private ClusterService clusterService;

    @Autowired
    private ClusterStateService clusterStateService;

    @Autowired
    private PodSecurityPolicyService podSecurityPolicyService;

    @Autowired
    private RBACResourceService rbacResourceService;


    @PostMapping("/{apiVersion}/cluster/{clusterSeq}")
    @Operation(summary = "PodSecurityPolicy를 추가한다", description = "cluster에 속한 PodSecurityPolicy를 추가한다.")
    public K8sPodSecurityPolicyVO addPodSecurityPolicy(
            @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v1"}), required = true) @PathVariable String apiVersion,
            @Parameter(name = "clusterSeq", description = "clusterSeq", required = true) @PathVariable Integer clusterSeq,
            @Parameter(name = "podSecurityPolicySpec", description = "추가하려는 PodSecurityPolicy", required = true) @RequestBody PodSecurityPolicyIntegrateVO podSecurityPolicySpec
    ) throws Exception {

        log.debug("[BEGIN] addPodSecurityPolicy");

        if (DeployType.valueOf(podSecurityPolicySpec.getDeployType()) != DeployType.GUI) {
            throw new CocktailException("Only GUI supported.", ExceptionType.InvalidParameter);
        }

        try {
            if (DeployType.valueOf(podSecurityPolicySpec.getDeployType()) == DeployType.GUI) {

                ClusterVO cluster = clusterService.getCluster(clusterSeq);

                /**
                 * k8s 1.25부터 psp가 remove되어 기능 제거됨
                 */
                podSecurityPolicyService.checkSupportedVersionForPsp(cluster.getK8sVersion());

                /**
                 * cluster 상태 체크
                 */
                clusterStateService.checkClusterState(cluster);

                ApiVersionType apiVersionType = ApiVersionType.valueOf(StringUtils.upperCase(apiVersion));
                ContextHolder.exeContext().setApiVersionType(apiVersionType);

                PodSecurityPolicyGuiVO gui = (PodSecurityPolicyGuiVO) podSecurityPolicySpec;

                // psp valid
                podSecurityPolicyService.checkPodSecurityPolicy(cluster, true, gui);

                // clusterRole valid
                rbacResourceService.checkClusterRoleName(cluster, true, gui.getName());

                // psp 생성
                K8sPodSecurityPolicyVO result = podSecurityPolicyService.createPodSecurityPolicy(cluster, gui, ContextHolder.exeContext());

                // cluster role
                try {
                    // cluster role 생성
                    podSecurityPolicyService.createClusterRolePsp(cluster, gui.getName(), gui.getName(), false);
                } catch (CocktailException ce) {
                    // cluster role 생성 실패시 rollback
                    podSecurityPolicyService.deletePodSecurityPolicy(cluster, gui.getName(), ContextHolder.exeContext());
                    throw ce;
                } catch (Exception e) {
                    // cluster role 생성 실패시 rollback
                    podSecurityPolicyService.deletePodSecurityPolicy(cluster, gui.getName(), ContextHolder.exeContext());
                    throw e;
                }

                // 생성된 psp 이외의 psp가 있다면 display-default 라벨 제거
                if (gui.isDisplayDefault()) {
                    podSecurityPolicyService.removeOtherDisplayDefaultPsp(cluster, gui.getName());
                }

                return result;
            }
        } finally {
            log.debug("[END  ] addPodSecurityPolicy");
        }

        return null;
    }

    @PutMapping("/{apiVersion}/cluster/{clusterSeq}/psp/{pspName:.+}")
    @Operation(summary = "지정한 PodSecurityPolicy 수정", description = "클러스터안에 지정한 PodSecurityPolicy를 수정한다.")
    public K8sPodSecurityPolicyVO updatePodSecurityPolicy(
            @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v1"}), required = true) @PathVariable String apiVersion,
            @Parameter(name = "clusterSeq", description = "clusterSeq", required = true) @PathVariable Integer clusterSeq,
            @Parameter(name = "pspName", description = "pspName", required = true) @PathVariable String pspName,
            @Parameter(name = "podSecurityPolicySpec", description = "수정하려는 podSecurityPolicySpec", required = true) @RequestBody PodSecurityPolicyIntegrateVO podSecurityPolicySpec
    ) throws Exception {

        log.debug("[BEGIN] updatePodSecurityPolicy");

        DeployType deployType;
        try {
            deployType = DeployType.valueOf(podSecurityPolicySpec.getDeployType());
        }
        catch (IllegalArgumentException iae) {
            throw new CocktailException("DeployType invalid.", iae, ExceptionType.InvalidParameter);
        }
        catch (Exception e) {
            throw new CocktailException("DeployType invalid.", e, ExceptionType.InvalidParameter);
        }

        ClusterVO cluster = clusterService.getCluster(clusterSeq);

        /**
         * k8s 1.25부터 psp가 remove되어 기능 제거됨
         */
        podSecurityPolicyService.checkSupportedVersionForPsp(cluster.getK8sVersion());

        /**
         * cluster 상태 체크
         */
        clusterStateService.checkClusterState(cluster);

        ApiVersionType apiVersionType = ApiVersionType.valueOf(StringUtils.upperCase(apiVersion));
        ContextHolder.exeContext().setApiVersionType(apiVersionType);

        K8sPodSecurityPolicyVO result;
        boolean isDisplayDefault = false;
        try {
            if (deployType == DeployType.GUI) {
                PodSecurityPolicyGuiVO gui = (PodSecurityPolicyGuiVO) podSecurityPolicySpec;
                if(!pspName.equals(gui.getName())) {
                    throw new CocktailException("PodSecurityPolicy name is different", ExceptionType.K8sPspNameInvalid);
                }
                // valid
                podSecurityPolicyService.checkPodSecurityPolicy(cluster, false, gui);

                // 이름 변경 로직 주석 ( hjchoi.20201020 )
//                if (StringUtils.isNotBlank(gui.getNewName()) && !StringUtils.equals(gui.getName(), gui.getNewName())) {
//                    // 1. 변경된 이름으로 psp 생성
//                    gui.setName(gui.getNewName());
//                    result = podSecurityPolicyService.createPodSecurityPolicy(cluster, gui, ContextHolder.exeContext());
//
//                    // 2. ClusterRole - 변경된 psp명으로 update
//                    podSecurityPolicyService.patchClusterRoleV1ForPsp(cluster, gui.getName(), gui.getNewName());
//
//                    // 3. 기존 psp 삭제
//                    podSecurityPolicyService.deletePodSecurityPolicy(cluster, pspName, ContextHolder.exeContext());
//                }
//                else {
//                    /** 이름 변경이 없으면... 그냥 패치 **/
//                    result = podSecurityPolicyService.patchPodSecurityPolicy(cluster, gui, ContextHolder.exeContext());
//                }

                result = podSecurityPolicyService.patchPodSecurityPolicy(cluster, gui, ContextHolder.exeContext());

                isDisplayDefault = gui.isDisplayDefault();
            } else {
                PodSecurityPolicyYamlVO yaml = (PodSecurityPolicyYamlVO) podSecurityPolicySpec;
                if(!pspName.equals(yaml.getName())) { // uri의 psp이름과 PodSecurityPolicyYamlVO의 psp이름은 같아야 함.. 새로운 이름은 Yaml Spec 안에 존재..
                    throw new CocktailException("PodSecurityPolicy name is different", ExceptionType.K8sPspNameInvalid);
                }

                // valid 하기 위행 GUI로 변환
                PodSecurityPolicyGuiVO gui = podSecurityPolicyService.convertYamlToGui(cluster, yaml.getYaml());
                gui.setName(pspName); // "pspName=기존이름"이므로 gui로 변환한 후 name에 pspName을 넣어줌.. (위에서 convert시 name / newName에 모두 "yaml안의 name=새이름"을 넣어주고 있으므로...)
                // valid
                podSecurityPolicyService.checkPodSecurityPolicy(cluster, false, gui);

                // 이름 변경 로직 주석 ( hjchoi.20201020 )
//                if (StringUtils.isNotBlank(gui.getNewName()) && !StringUtils.equals(gui.getName(), gui.getNewName())) {
//                    // 1. 변경된 이름으로 psp 생성
//                    podSecurityPolicyService.createPodSecurityPolicy(cluster, yaml.getYaml(), ContextHolder.exeContext());
//
//                    // 2. ClusterRole - 변경된 psp명으로 update
//                    podSecurityPolicyService.patchClusterRoleV1ForPsp(cluster, gui.getName(), gui.getNewName());
//
//                    // 3. 기존 psp 삭제
//                    podSecurityPolicyService.deletePodSecurityPolicy(cluster, pspName, ContextHolder.exeContext());
//
//                    // 4. 새로 생성된 psp 이름으로 정보 조회
//                    result = podSecurityPolicyService.getPodSecurityPolicy(cluster, gui.getNewName(), ContextHolder.exeContext());
//                }
//                else {
//                    /** 이름 변경이 없으면... 그냥 패치 **/
//                    result = podSecurityPolicyService.patchPodSecurityPolicy(cluster, yaml.getYaml(), ContextHolder.exeContext());
//                }

                result = podSecurityPolicyService.patchPodSecurityPolicy(cluster, yaml.getYaml(), ContextHolder.exeContext());

                isDisplayDefault = gui.isDisplayDefault();
            }

            // 생성된 psp 이외의 psp가 있다면 display-default 라벨 제거
            if (isDisplayDefault) {
                podSecurityPolicyService.removeOtherDisplayDefaultPsp(cluster, pspName);
            }
        } finally {
            log.debug("[END  ] updatePodSecurityPolicy");
        }

        return result;
    }

    @GetMapping("/{apiVersion}/cluster/{clusterSeq}/psp/{pspName:.+}")
    @Operation(summary = "지정한 Cluster에 속한 PodSecurityPolicy 상세 반환", description = "클러스터 안에 지정한 PodSecurityPolicy의 상세 정보를 응답한다.")
    public K8sPodSecurityPolicyVO getPodSecurityPolicy(
            @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v1"}), required = true) @PathVariable String apiVersion,
            @Parameter(name = "clusterSeq", description = "clusterSeq", required = true) @PathVariable Integer clusterSeq,
            @Parameter(name = "pspName", description = "pspName", required = true) @PathVariable String pspName,
            @Parameter(name = "useRBAC", description = "RBAC 정보 사용 여부", schema = @Schema(allowableValues = {"true","false"}, defaultValue = "false")) @RequestParam(value = "useRBAC", required = false, defaultValue = "false") boolean useRBAC
    ) throws Exception {

        log.debug("[BEGIN] getPodSecurityPolicy");

        ClusterVO cluster = clusterService.getCluster(clusterSeq);

        /**
         * k8s 1.25부터 psp가 remove되어 기능 제거됨
         */
        podSecurityPolicyService.checkSupportedVersionForPsp(cluster.getK8sVersion());

        /**
         * cluster 상태 체크
         */
        clusterStateService.checkClusterState(cluster);

        ApiVersionType apiVersionType = ApiVersionType.valueOf(StringUtils.upperCase(apiVersion));
        ContextHolder.exeContext().setApiVersionType(apiVersionType);

        K8sPodSecurityPolicyVO result = podSecurityPolicyService.getPodSecurityPolicyWithRBAC(cluster, pspName, useRBAC, ContextHolder.exeContext());

        log.debug("[END  ] getPodSecurityPolicy");

        return result;
    }

    @GetMapping("/{apiVersion}/cluster/{clusterSeq}/psp/{pspName:.+}/rbac")
    @Operation(summary = "지정한 Cluster에 속한 PodSecurityPolicy의 RBAC 정보 반환", description = "클러스터안에 지정한 PodSecurityPolicy의 RBAC 정보를 응답한다.")
    public IntegrateRBACVO getIntegrateRBACByPsp(
            @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v1"}), required = true) @PathVariable String apiVersion,
            @Parameter(name = "clusterSeq", description = "clusterSeq", required = true) @PathVariable Integer clusterSeq,
            @Parameter(name = "pspName", description = "pspName", required = true) @PathVariable String pspName
    ) throws Exception {

        log.debug("[BEGIN] getIntegrateRBACByPsp");

        ClusterVO cluster = clusterService.getCluster(clusterSeq);

        /**
         * k8s 1.25부터 psp가 remove되어 기능 제거됨
         */
        podSecurityPolicyService.checkSupportedVersionForPsp(cluster.getK8sVersion());

        /**
         * cluster 상태 체크
         */
        clusterStateService.checkClusterState(cluster);

        ApiVersionType apiVersionType = ApiVersionType.valueOf(StringUtils.upperCase(apiVersion));
        ContextHolder.exeContext().setApiVersionType(apiVersionType);

        IntegrateRBACVO result = podSecurityPolicyService.getIntegrateRBACByPsp(cluster, pspName);

        log.debug("[END  ] getIntegrateRBACByPsp");

        return result;
    }

    @GetMapping("/{apiVersion}/template")
    @Operation(summary = "PodSecurityPolicy 기본 템플릿", description = "PodSecurityPolicy의 기본 Template 정보를 응답한다.")
    public PodSecurityPolicyGuiVO getPodSecurityPolicyTemplate(
            @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v1"}), required = true) @PathVariable String apiVersion
    ) throws Exception {

        log.debug("[BEGIN] getPodSecurityPolicyTemplate");


        ApiVersionType apiVersionType = ApiVersionType.valueOf(StringUtils.upperCase(apiVersion));
        ContextHolder.exeContext().setApiVersionType(apiVersionType);

        PodSecurityPolicyGuiVO result = podSecurityPolicyService.generatePspTemplate();

        log.debug("[END  ] getPodSecurityPolicyTemplate");

        return result;
    }

//    @GetMapping("/{apiVersion}/cluster/{clusterSeq}/psp/default")
//    @Operation(summary = "지정한 Cluster에 속한 기본 PodSecurityPolicy 상세 반환")
//    public K8sPodSecurityPolicyVO getDefaultPodSecurityPolicy(
//            @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v1"}), required = true) @PathVariable String apiVersion,
//            @Parameter(name = "clusterSeq", description = "clusterSeq", required = true) @PathVariable Integer clusterSeq
//    ) throws Exception {
//
//        log.debug("[BEGIN] getDefaultPodSecurityPolicy");
//
//        ClusterVO cluster = clusterService.getCluster(clusterSeq);
//
//        /**
//         * cluster 상태 체크
//         */
//        clusterStateService.checkClusterState(cluster);
//
//        ApiVersionType apiVersionType = ApiVersionType.valueOf(StringUtils.upperCase(apiVersion));
//        ContextHolder.exeContext().setApiVersionType(apiVersionType);
//
//        String label = String.format("%s=%s", KubeConstants.META_LABELS_APP_MANAGED_BY, KubeConstants.LABELS_COCKTAIL_KEY);
//        List<K8sPodSecurityPolicyVO> list = podSecurityPolicyService.getPodSecurityPolicies(cluster, null, label, ContextHolder.exeContext());
//        K8sPodSecurityPolicyVO result = null;
//        if (CollectionUtils.isNotEmpty(list)) {
//            result = list.get(0);
//        }
//
//        log.debug("[END  ] getDefaultPodSecurityPolicy");
//
//        return result;
//    }

    @GetMapping("/{apiVersion}/cluster/{clusterSeq}/psps")
    @Operation(summary = "지정한 cluster에 속한 PodSecurityPolicy 목록 반환", description = "지정한 클러스터안의 전체 PodSecurityPolicy 목록을 응답한다.")
    public List<K8sPodSecurityPolicyVO> getPodSecurityPolicies(
            @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v1"}), required = true) @PathVariable String apiVersion,
            @Parameter(name = "clusterSeq", description = "clusterSeq", required = true) @PathVariable Integer clusterSeq,
            @Parameter(name = "useSystem", description = "System에서 내부적으로 사용하는 Resource 정보 사용 여부", schema = @Schema(allowableValues = {"true","false"}, defaultValue = "false")) @RequestParam(value = "useSystem", required = false, defaultValue = "false") boolean useSystem,
            @Parameter(name = "useCocktail", description = "Cocktail에서 만든 Resource 정보 사용 여부", schema = @Schema(allowableValues = {"true","false"}, defaultValue = "false")) @RequestParam(value = "useCocktail", required = false, defaultValue = "false") boolean useCocktail
    ) throws Exception {

        log.debug("[BEGIN] getPodSecurityPolicies");

        ClusterVO cluster = clusterService.getCluster(clusterSeq);

        /**
         * k8s 1.25부터 psp가 remove되어 기능 제거됨
         */
        podSecurityPolicyService.checkSupportedVersionForPsp(cluster.getK8sVersion());

        /**
         * cluster 상태 체크
         */
        clusterStateService.checkClusterState(cluster);

        ApiVersionType apiVersionType = ApiVersionType.valueOf(StringUtils.upperCase(apiVersion));
        ContextHolder.exeContext().setApiVersionType(apiVersionType);

        List<String> labels = Lists.newArrayList();
        String labelSelector = null;
        if (!useSystem) {
            labels.add(String.format("!%s", KubeConstants.LABELS_ACORNSOFT_SYSTEM_RESOURCE));
        }
        if (useCocktail) {
            labels.add(String.format("%s", KubeConstants.LABELS_ACORNSOFT_PSP_RESOURCE));
        }
        if (CollectionUtils.isNotEmpty(labels)) {
            labelSelector = labels.stream().collect(Collectors.joining(","));
        }

        List<K8sPodSecurityPolicyVO> list = podSecurityPolicyService.getPodSecurityPolicies(cluster, null, labelSelector, ContextHolder.exeContext());


        log.debug("[END  ] getPodSecurityPolicies");

        return list;
    }

    @DeleteMapping("/{apiVersion}/cluster/{clusterSeq}/psp/{pspName:.+}")
    @Operation(summary = "지정한 PodSecurityPolicy 삭제", description = "지정한 PodSecurityPolicy를 삭제한다.")
    public void deletePodSecurityPolicy(
            @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v1"}), required = true) @PathVariable String apiVersion,
            @Parameter(name = "clusterSeq", description = "clusterSeq", required = true) @PathVariable Integer clusterSeq,
            @Parameter(name = "pspName", description = "pspName", required = true) @PathVariable String pspName,
            @Parameter(name = "cascade", description = "종속된 RBAC 모두 삭제", schema = @Schema(allowableValues = {"true","false"}, defaultValue = "false")) @RequestParam(value = "cascade", required = false, defaultValue = "false") boolean cascade
    ) throws Exception {

        log.debug("[BEGIN] deletePodSecurityPolicy");

        ClusterVO cluster = clusterService.getCluster(clusterSeq);

        /**
         * k8s 1.25부터 psp가 remove되어 기능 제거됨
         */
        podSecurityPolicyService.checkSupportedVersionForPsp(cluster.getK8sVersion());

        /**
         * cluster 상태 체크
         */
        clusterStateService.checkClusterState(cluster);

        ApiVersionType apiVersionType = ApiVersionType.valueOf(StringUtils.upperCase(apiVersion));
        ContextHolder.exeContext().setApiVersionType(apiVersionType);

        podSecurityPolicyService.deletePodSecurityPolicy(cluster, pspName, cascade, ContextHolder.exeContext());

        log.debug("[END  ] deletePodSecurityPolicy");

    }

}
