package run.acloud.api.resource.controller;

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
import run.acloud.api.resource.service.NetworkPolicyService;
import run.acloud.api.resource.vo.K8sNetworkPolicyVO;
import run.acloud.api.resource.vo.NetworkPolicyGuiVO;
import run.acloud.api.resource.vo.NetworkPolicyIntegrateVO;
import run.acloud.api.resource.vo.NetworkPolicyYamlVO;
import run.acloud.commons.enums.ApiVersionType;
import run.acloud.framework.context.ContextHolder;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;

import java.util.List;

/**
 * @author wschoi@acornsoft.io
 * Created on 2017. 7. 27.
 */
@Tag(name = "Kubernetes NetworkPolicy Management", description = "쿠버네티스 NetworkPolicy에 대한 관리 기능을 제공한다.")
@Slf4j
@RequestMapping(value = "/api/np")
@RestController
@Validated
public class NetworkPolicyController {

    @Autowired
    private ClusterService clusterService;

    @Autowired
    private ClusterStateService clusterStateService;

    @Autowired
    private NetworkPolicyService networkPolicyService;



    @PostMapping("/{apiVersion}/cluster/{clusterSeq}/namespace/{namespaceName:.+}")
    @Operation(summary = "NetworkPolicy를 추가한다", description = "클러스터의 네임스페이스 NetworkPolicy를 추가한다.")
    public K8sNetworkPolicyVO addNetworkPolicy(
            @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v1"}), required = true) @PathVariable String apiVersion,
            @Parameter(name = "clusterSeq", description = "clusterSeq", required = true) @PathVariable Integer clusterSeq,
            @Parameter(name = "namespaceName", description = "namespaceName", required = true) @PathVariable String namespaceName,
            @Parameter(name = "networkPolicySpec", description = "추가하려는 networkPolicy", required = true) @RequestBody NetworkPolicyIntegrateVO networkPolicySpec
    ) throws Exception {

        log.debug("[BEGIN] addNetworkPolicy");

        DeployType deployType;
        try {
            deployType = DeployType.valueOf(networkPolicySpec.getDeployType());
        }
        catch (IllegalArgumentException iae) {
            throw new CocktailException("DeployType invalid.", iae, ExceptionType.InvalidParameter);
        }
        catch (Exception e) {
            throw new CocktailException("DeployType invalid.", e, ExceptionType.InvalidParameter);
        }

        ClusterVO cluster = clusterService.getCluster(clusterSeq);

        /**
         * cluster 상태 체크
         */
        clusterStateService.checkClusterState(cluster);

        ApiVersionType apiVersionType = ApiVersionType.valueOf(StringUtils.upperCase(apiVersion));
        ContextHolder.exeContext().setApiVersionType(apiVersionType);

        try {
            if (deployType == DeployType.GUI) {

                NetworkPolicyGuiVO gui = (NetworkPolicyGuiVO) networkPolicySpec;

                // valid
                networkPolicyService.checkNetworkPolicy(cluster, namespaceName, true, gui);

                return networkPolicyService.createNetworkPolicy(cluster, gui, ContextHolder.exeContext());
            } else {
                NetworkPolicyYamlVO yaml = (NetworkPolicyYamlVO) networkPolicySpec;

                // valid 하기 위행 GUI로 변환
                NetworkPolicyGuiVO gui = networkPolicyService.convertYamlToGui(cluster, yaml.getYaml());

                // valid
                networkPolicyService.checkNetworkPolicy(cluster, namespaceName, true, gui);

                return networkPolicyService.createNetworkPolicy(cluster, namespaceName, yaml.getYaml(), ContextHolder.exeContext());
            }
        } finally {
            log.debug("[END  ] addNetworkPolicy");
        }

    }

    @PostMapping("/{apiVersion}/cluster/{clusterSeq}/namespace/{namespaceName:.+}/default")
    @Operation(summary = "기본 NetworkPolicy를 추가한다", description = "클러스터의 네임스페이스에 기본 NetworkPolicy를 추가한다.")
    public K8sNetworkPolicyVO addDefaultNetworkPolicy(
            @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v1"}), required = true) @PathVariable String apiVersion,
            @Parameter(name = "clusterSeq", description = "clusterSeq", required = true) @PathVariable Integer clusterSeq,
            @Parameter(name = "namespaceName", description = "namespaceName", required = true) @PathVariable String namespaceName
    ) throws Exception {

        log.debug("[BEGIN] addDefaultNetworkPolicy");

        K8sNetworkPolicyVO result = null;

        try {
            ClusterVO cluster = clusterService.getCluster(clusterSeq);

            /**
             * cluster 상태 체크
             */
            clusterStateService.checkClusterState(cluster);

            // 기본 networkPolicy 생성
            networkPolicyService.createDefaultNetworkPolicy(cluster, namespaceName);

            String label = String.format("%s=%s", KubeConstants.META_LABELS_APP_MANAGED_BY, KubeConstants.LABELS_COCKTAIL_KEY);
            List<K8sNetworkPolicyVO> list = networkPolicyService.getNetworkPolicies(cluster, namespaceName, null, label, ContextHolder.exeContext());
            if (CollectionUtils.isNotEmpty(list)) {
                result = list.get(0);
            }
        } finally {
            log.debug("[END  ] addDefaultNetworkPolicy");
        }

        return result;
    }

    @PutMapping("/{apiVersion}/cluster/{clusterSeq}/namespace/{namespaceName:.+}/networkpolicy/{networkPolicyName:.+}")
    @Operation(summary = "지정한 NetworkPolicy 수정", description = "클러스터의 네임스페이스에 지정된 NetworkPolicy 정보를 수정한다.")
    public K8sNetworkPolicyVO updateNetworkPolicy(
            @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v1"}), required = true) @PathVariable String apiVersion,
            @Parameter(name = "clusterSeq", description = "clusterSeq", required = true) @PathVariable Integer clusterSeq,
            @Parameter(name = "namespaceName", description = "namespaceName", required = true) @PathVariable String namespaceName,
            @Parameter(name = "networkPolicyName", description = "networkPolicyName", required = true) @PathVariable String networkPolicyName,
            @Parameter(name = "networkPolicySpec", description = "수정하려는 networkPolicySpec", required = true) @RequestBody NetworkPolicyIntegrateVO networkPolicySpec
    ) throws Exception {

        log.debug("[BEGIN] updateNetworkPolicy");

        DeployType deployType;
        try {
            deployType = DeployType.valueOf(networkPolicySpec.getDeployType());
        }
        catch (IllegalArgumentException iae) {
            throw new CocktailException("DeployType invalid.", iae, ExceptionType.InvalidParameter);
        }
        catch (Exception e) {
            throw new CocktailException("DeployType invalid.", e, ExceptionType.InvalidParameter);
        }

        ClusterVO cluster = clusterService.getCluster(clusterSeq);

        /**
         * cluster 상태 체크
         */
        clusterStateService.checkClusterState(cluster);

        ApiVersionType apiVersionType = ApiVersionType.valueOf(StringUtils.upperCase(apiVersion));
        ContextHolder.exeContext().setApiVersionType(apiVersionType);

        try {
            if (deployType == DeployType.GUI) {
                NetworkPolicyGuiVO gui = (NetworkPolicyGuiVO) networkPolicySpec;
                if(!networkPolicyName.equals(gui.getName())) {
                    throw new CocktailException("Can't change the NetworkPolicy name. (NetworkPolicy name is different)", ExceptionType.K8sNetworkPolicyNameInvalid);
                }

                // valid
                networkPolicyService.checkNetworkPolicy(cluster, namespaceName, false, gui);

                return networkPolicyService.patchNetworkPolicy(cluster, gui, false, ContextHolder.exeContext());
            } else {
                NetworkPolicyYamlVO yaml = (NetworkPolicyYamlVO) networkPolicySpec;
                if(!networkPolicyName.equals(yaml.getName())) {
                    throw new CocktailException("Can't change the NetworkPolicy name. (NetworkPolicy name is different)", ExceptionType.K8sNetworkPolicyNameInvalid);
                }

                // valid 하기 위행 GUI로 변환
                NetworkPolicyGuiVO gui = networkPolicyService.convertYamlToGui(cluster, yaml.getYaml());
                if (gui == null) {
                    throw new CocktailException("Can't convert the NetworkPolicy. (It is null Gui Object to validate)", ExceptionType.K8sNetworkPolicyNotFound);
                }
                if(!networkPolicyName.equals(gui.getName())) {
                    throw new CocktailException("Can't change the NetworkPolicy name. (NetworkPolicy name is different)", ExceptionType.K8sNetworkPolicyNameInvalid);
                }

                // valid
                networkPolicyService.checkNetworkPolicy(cluster, namespaceName, false, gui);

                return networkPolicyService.patchNetworkPolicy(cluster, yaml.getYaml(), false, ContextHolder.exeContext());
            }
        } finally {
            log.debug("[END  ] updateNetworkPolicy");
        }

    }

    @GetMapping("/{apiVersion}/cluster/{clusterSeq}/namespace/{namespaceName:.+}/networkpolicy/{networkPolicyName:.+}")
    @Operation(summary = "NetworkPolicy 상세 반환", description = "클러스터의 네임스페이스안에 지정한 NetworkPolicy의 상세 정보를 응답한다.")
    public K8sNetworkPolicyVO getNetworkPolicy(
            @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v1"}), required = true) @PathVariable String apiVersion,
            @Parameter(name = "clusterSeq", description = "clusterSeq", required = true) @PathVariable Integer clusterSeq,
            @Parameter(name = "namespaceName", description = "namespaceName", required = true) @PathVariable String namespaceName,
            @Parameter(name = "networkPolicyName", description = "networkPolicyName", required = true) @PathVariable String networkPolicyName
    ) throws Exception {

        log.debug("[BEGIN] getNetworkPolicy");

        ClusterVO cluster = clusterService.getCluster(clusterSeq);

        /**
         * cluster 상태 체크
         */
        clusterStateService.checkClusterState(cluster);

        ApiVersionType apiVersionType = ApiVersionType.valueOf(StringUtils.upperCase(apiVersion));
        ContextHolder.exeContext().setApiVersionType(apiVersionType);

        K8sNetworkPolicyVO result = networkPolicyService.getNetworkPolicy(cluster, namespaceName, networkPolicyName, ContextHolder.exeContext());

        log.debug("[END  ] getNetworkPolicy");

        return result;
    }

    @GetMapping("/{apiVersion}/cluster/{clusterSeq}/namespace/{namespaceName:.+}/networkpolicy/default")
    @Operation(summary = "기본 NetworkPolicy 상세 반환", description = "클러스터의 네임스페이스안에 기본 NetworkPolicy를 찾아 상세 정보를 응답한다.")
    public K8sNetworkPolicyVO getDefaultNetworkPolicy(
            @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v1"}), required = true) @PathVariable String apiVersion,
            @Parameter(name = "clusterSeq", description = "clusterSeq", required = true) @PathVariable Integer clusterSeq,
            @Parameter(name = "namespaceName", description = "namespaceName", required = true) @PathVariable String namespaceName
    ) throws Exception {

        log.debug("[BEGIN] getDefaultNetworkPolicy");

        ClusterVO cluster = clusterService.getCluster(clusterSeq);

        /**
         * cluster 상태 체크
         */
        clusterStateService.checkClusterState(cluster);

        ApiVersionType apiVersionType = ApiVersionType.valueOf(StringUtils.upperCase(apiVersion));
        ContextHolder.exeContext().setApiVersionType(apiVersionType);

        String label = String.format("%s=%s", KubeConstants.META_LABELS_APP_MANAGED_BY, KubeConstants.LABELS_COCKTAIL_KEY);
        List<K8sNetworkPolicyVO> list = networkPolicyService.getNetworkPolicies(cluster, namespaceName, null, label, ContextHolder.exeContext());
        K8sNetworkPolicyVO result = null;
        if (CollectionUtils.isNotEmpty(list)) {
            result = list.get(0);
        }

        log.debug("[END  ] getDefaultNetworkPolicy");

        return result;
    }

    @GetMapping("/{apiVersion}/cluster/{clusterSeq}/namespace/{namespaceName:.+}/networkpolicies")
    @Operation(summary = "지정한 cluster에 속한 NetworkPolicy 목록 반환", description = "클러스터의 네임스페이스안의 전체 NetworkPolicy 목록을 응답한다.")
    public List<K8sNetworkPolicyVO> getNetworkPolicies(
            @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v1"}), required = true) @PathVariable String apiVersion,
            @Parameter(name = "clusterSeq", description = "clusterSeq", required = true) @PathVariable Integer clusterSeq,
            @Parameter(name = "namespaceName", description = "namespaceName", required = true) @PathVariable String namespaceName
    ) throws Exception {

        log.debug("[BEGIN] getNetworkPolicies");

        ClusterVO cluster = clusterService.getCluster(clusterSeq);

        /**
         * cluster 상태 체크
         */
        clusterStateService.checkClusterState(cluster);

        ApiVersionType apiVersionType = ApiVersionType.valueOf(StringUtils.upperCase(apiVersion));
        ContextHolder.exeContext().setApiVersionType(apiVersionType);


        List<K8sNetworkPolicyVO> list = networkPolicyService.getNetworkPolicies(cluster, namespaceName, null, null, ContextHolder.exeContext());


        log.debug("[END  ] getNetworkPolicies");

        return list;
    }

    @DeleteMapping("/{apiVersion}/cluster/{clusterSeq}/namespace/{namespaceName:.+}/networkpolicy/{networkPolicyName:.+}")
    @Operation(summary = "지정한 NetworkPolicy 삭제", description = "클러스터의 네임스페이스안에 지정한 NetworkPolicy를 삭제 한다.")
    public void deleteNetworkPolicy(
            @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v1"}), required = true) @PathVariable String apiVersion,
            @Parameter(name = "clusterSeq", description = "clusterSeq", required = true) @PathVariable Integer clusterSeq,
            @Parameter(name = "namespaceName", description = "namespaceName", required = true) @PathVariable String namespaceName,
            @Parameter(name = "networkPolicyName", description = "networkPolicyName", required = true) @PathVariable String networkPolicyName
    ) throws Exception {

        log.debug("[BEGIN] deleteNetworkPolicy");

        ClusterVO cluster = clusterService.getCluster(clusterSeq);

        /**
         * cluster 상태 체크
         */
        clusterStateService.checkClusterState(cluster);

        ApiVersionType apiVersionType = ApiVersionType.valueOf(StringUtils.upperCase(apiVersion));
        ContextHolder.exeContext().setApiVersionType(apiVersionType);

        networkPolicyService.deleteNetworkPolicy(cluster, namespaceName, networkPolicyName, ContextHolder.exeContext());

        log.debug("[END  ] deleteNetworkPolicy");

    }

    @GetMapping("/{apiVersion}/cluster/{clusterSeq}/namespace/{namespaceName:.+}/template")
    @Operation(summary = "NetworkPolicy Yaml Template 반환", description = "YAML형태의 Default NetworkPolicy Template을 응답한다.")
    public String getNetworkPoliciyTemplate(
            @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v1"}), required = true) @PathVariable String apiVersion,
            @Parameter(name = "clusterSeq", description = "clusterSeq", required = true) @PathVariable Integer clusterSeq,
            @Parameter(name = "namespaceName", description = "namespaceName", required = true) @PathVariable String namespaceName
    ) throws Exception {

        log.debug("[BEGIN] getNetworkPoliciyTemplate");

        ApiVersionType apiVersionType = ApiVersionType.valueOf(StringUtils.upperCase(apiVersion));
        ContextHolder.exeContext().setApiVersionType(apiVersionType);


        String template = networkPolicyService.generateNetworkPolicyTemplate(namespaceName);


        log.debug("[END  ] getNetworkPoliciyTemplate");

        return template;
    }
}
