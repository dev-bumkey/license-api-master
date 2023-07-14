package run.acloud.api.resource.controller;

import io.swagger.annotations.ApiOperation;
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
import run.acloud.api.cserver.service.ServicemapService;
import run.acloud.api.resource.constants.KubeConstants;
import run.acloud.api.resource.service.IngressSpecService;
import run.acloud.api.resource.service.K8sResourceService;
import run.acloud.api.resource.vo.IngressSpecGuiVO;
import run.acloud.api.resource.vo.IngressSpecIntegrateVO;
import run.acloud.api.resource.vo.IngressSpecYamlVO;
import run.acloud.api.resource.vo.K8sIngressVO;
import run.acloud.commons.enums.ApiVersionType;
import run.acloud.commons.vo.ResultVO;
import run.acloud.framework.context.ContextHolder;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author wschoi@acornsoft.io
 * Created on 2017. 7. 27.
 */
@Tag(name = "Kubernetes Ingress Management", description = "쿠버네티스 Ingress에 대한 관리 기능을 제공한다.")
@Slf4j
@RequestMapping(value = "/api/ingress-spec")
@RestController
@Validated
public class IngressSpecController {

    @Autowired
    private K8sResourceService k8sResourceService;

    @Autowired
    private IngressSpecService ingressSpecService;

    @Autowired
    private ClusterService clusterService;

    @Autowired
    private ClusterStateService clusterStateService;

    @Autowired
    private ServicemapService servicemapService;

    @Deprecated
    @PostMapping("/{apiVersion}/service/{serviceSeq}/servicemap/{servicemapSeq}")
    @Operation(summary = "(Old) Ingress를 추가한다", description = "Service, Appmap에 속함")
    public K8sIngressVO addIngressSpec(
            @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v2"}), required = true) @PathVariable String apiVersion,
            @Parameter(name = "serviceSeq", description = "service seq", required = true) @PathVariable Integer serviceSeq,
            @Parameter(name = "servicemapSeq", description = "servicemap seq", required = true) @PathVariable Integer servicemapSeq,
            @Parameter(name = "ingressSpec", description = "추가하려는 ingressSpec", required = true) @RequestBody IngressSpecIntegrateVO ingressSpec
    ) throws Exception {

        log.debug("[BEGIN] addIngressSpec");

        if (DeployType.valueOf(ingressSpec.getDeployType()) != DeployType.GUI) {
            throw new CocktailException("Only GUI supported.", ExceptionType.InvalidParameter);
        }

        try {
            if (DeployType.valueOf(ingressSpec.getDeployType()) == DeployType.GUI) {

                /**
                 * cluster 상태 체크
                 */
                clusterStateService.checkClusterStateByServicemap(servicemapSeq);

                ApiVersionType apiVersionType = ApiVersionType.valueOf(StringUtils.upperCase(apiVersion));
                ContextHolder.exeContext().setApiVersionType(apiVersionType);

                IngressSpecGuiVO ingressSpecGui = (IngressSpecGuiVO) ingressSpec;

                ClusterVO cluster = k8sResourceService.setupCluster(servicemapSeq);

                ingressSpecService.checkIngress(cluster, cluster.getNamespaceName(), true, ingressSpecGui);

                return ingressSpecService.createIngress(cluster, cluster.getNamespaceName(), ingressSpecGui, null, ContextHolder.exeContext());
            }
        } finally {
            log.debug("[END  ] addIngressSpec");
        }

        return null;
    }

    @Deprecated
    @PutMapping("/{apiVersion}/service/{serviceSeq}/servicemap/{servicemapSeq}/ingress/{ingressName:.+}")
    @Operation(summary = "(Old) 지정한 Ingress 수정")
    public K8sIngressVO updateIngress(
            @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v2"}), required = true) @PathVariable String apiVersion,
            @Parameter(name = "serviceSeq", description = "service seq", required = true) @PathVariable Integer serviceSeq,
            @Parameter(name = "servicemapSeq", description = "servicemap seq", required = true) @PathVariable Integer servicemapSeq,
            @Parameter(name = "ingressName", description = "ingressName", required = true) @PathVariable String ingressName,
            @Parameter(name = "ingressSpec", description = "수정하려는 ingressSpec", required = true) @RequestBody IngressSpecIntegrateVO ingressSpec
    ) throws Exception {

        log.debug("[BEGIN] updateIngress");

        DeployType deployType;
        try {
            deployType = DeployType.valueOf(ingressSpec.getDeployType());
        } catch (Exception e) {
            throw new CocktailException("DeployType invalid.", e, ExceptionType.InvalidParameter);
        }

        /**
         * cluster 상태 체크
         */
        clusterStateService.checkClusterStateByServicemap(servicemapSeq);

        ApiVersionType apiVersionType = ApiVersionType.valueOf(StringUtils.upperCase(apiVersion));
        ContextHolder.exeContext().setApiVersionType(apiVersionType);

        try {
            ClusterVO cluster = k8sResourceService.setupCluster(servicemapSeq);

            if (deployType == DeployType.GUI) {
                IngressSpecGuiVO ingressSpecGui = (IngressSpecGuiVO) ingressSpec;
                if(!ingressName.equals(ingressSpecGui.getName())) {
                    throw new CocktailException("Can't change the Ingress name. (Ingress name is different)", ExceptionType.K8sIngressNameInvalid);
                }

                // valid
                ingressSpecService.checkIngress(cluster, cluster.getNamespaceName(), false, ingressSpecGui);

                return ingressSpecService.patchIngress(cluster, cluster.getNamespaceName(), ingressSpecGui, ContextHolder.exeContext());
            } else {
                IngressSpecYamlVO ingressSpecYaml = (IngressSpecYamlVO) ingressSpec;
                if(!ingressName.equals(ingressSpecYaml.getName())) {
                    throw new CocktailException("Can't change the Ingress name. (Ingress name is different)", ExceptionType.K8sIngressNameInvalid);
                }

                // valid 하기 위행 GUI로 변환
                IngressSpecGuiVO ingressSpecGui = ingressSpecService.convertIngressSpecYamlToGui(cluster, servicemapSeq, cluster.getNamespaceName(), ingressSpecYaml.getYaml());
                if(!ingressName.equals(ingressSpecGui.getName())) {
                    throw new CocktailException("Can't change the Ingress name. (Ingress name is different)", ExceptionType.K8sIngressNameInvalid);
                }
                if(!cluster.getNamespaceName().equals(ingressSpecGui.getNamespaceName())) {
                    throw new CocktailException("Can't change the Ingress namespace. (Ingress namespace is different)", ExceptionType.NamespaceNameInvalid);
                }

                // valid
                ingressSpecService.checkIngress(cluster, cluster.getNamespaceName(), false, ingressSpecGui);

                return ingressSpecService.patchIngress(cluster, cluster.getNamespaceName(), ingressSpecYaml.getYaml(), ContextHolder.exeContext());
            }
        } finally {
            log.debug("[END  ] updateIngress");
        }

    }

    @Deprecated
    @GetMapping("/{apiVersion}/service/{serviceSeq}/servicemap/{servicemapSeq}/ingress/{ingressName:.+}")
    @Operation(summary = "(Old) 지정한 Service-Servicemap에 속한 Ingress 상세 반환")
    public K8sIngressVO getIngressSpec(
            @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v2"}), required = true) @PathVariable String apiVersion,
            @Parameter(name = "serviceSeq", description = "service seq", required = true) @PathVariable Integer serviceSeq,
            @Parameter(name = "servicemapSeq", description = "servicemap seq", required = true) @PathVariable Integer servicemapSeq,
            @Parameter(name = "ingressName", description = "ingressName", required = true) @PathVariable String ingressName
    ) throws Exception {

        log.debug("[BEGIN] getIngressSpec");

        /**
         * cluster 상태 체크
         */
        clusterStateService.checkClusterStateByServicemap(servicemapSeq);

        ApiVersionType apiVersionType = ApiVersionType.valueOf(StringUtils.upperCase(apiVersion));
        ContextHolder.exeContext().setApiVersionType(apiVersionType);

        ClusterVO cluster = k8sResourceService.setupCluster(servicemapSeq);

        K8sIngressVO k8sIngress = ingressSpecService.getIngress(cluster, cluster.getNamespaceName(), ingressName, ContextHolder.exeContext());

        log.debug("[END  ] getIngressSpec");

        return k8sIngress;
    }

    @Deprecated
    @GetMapping("/{apiVersion}/service/{serviceSeq}/servicemap/{servicemapSeq}/ingresses")
    @Operation(summary = "(Old) 지정한 Service-Servicemap에 속한 Ingress 목록 반환")
    public List<K8sIngressVO> getIngressSpecs(
            @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v2"}), required = true) @PathVariable String apiVersion,
            @Parameter(name = "serviceSeq", description = "service seq", required = true) @PathVariable Integer serviceSeq,
            @Parameter(name = "servicemapSeq", description = "servicemap seq", required = true) @PathVariable Integer servicemapSeq,
            @Parameter(name = "acloudOnly", description = "Acloud에서 관리되는 인그레스만 조회 여부", schema = @Schema(allowableValues = {"true","false"}, defaultValue = "false")) @RequestParam(value = "acloudOnly", defaultValue = "false", required = false) Boolean acloudOnly
    ) throws Exception {

        log.debug("[BEGIN] getIngressSpecs");

        /**
         * cluster 상태 체크
         */
        clusterStateService.checkClusterStateByServicemap(servicemapSeq);

        ApiVersionType apiVersionType = ApiVersionType.valueOf(StringUtils.upperCase(apiVersion));
        ContextHolder.exeContext().setApiVersionType(apiVersionType);

        ClusterVO cluster = k8sResourceService.setupCluster(servicemapSeq);

        String field = String.format("%s.%s=%s", KubeConstants.META, KubeConstants.META_NAMESPACE, cluster.getNamespaceName());
        /**
         * Acloud에서 관리되는 서비스만 조회 or 전체 조회 판단. : 2019.06.11
         */
        String label = null;
        if(acloudOnly) {
            label = KubeConstants.LABELS_COCKTAIL_KEY;
        }
        List<K8sIngressVO> cocktailK8sIngresses = ingressSpecService.getIngresses(cluster, field, label, ContextHolder.exeContext());

        List<K8sIngressVO> k8sIngresses = new ArrayList<>();
        if(acloudOnly) {
            // TacloudOnly 불가능.. Cocktail Label 사라짐..
            // k8sIngresses = cocktailK8sIngresses.stream().filter(svc -> ("Y".equals(svc.getIsManagedInCocktail()))).collect(Collectors.toList());
            k8sIngresses.addAll(cocktailK8sIngresses);
        }
        else {
            k8sIngresses.addAll(cocktailK8sIngresses);
        }


        log.debug("[END  ] getIngressSpecs");

        return k8sIngresses;
    }

    @Deprecated
    @DeleteMapping("/{apiVersion}/service/{serviceSeq}/servicemap/{servicemapSeq}/ingress/{ingressName:.+}")
    @Operation(summary = "(Old) 지정한 Ingress 삭제")
    public void deleteIngress(
            @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v2"}), required = true) @PathVariable String apiVersion,
            @Parameter(name = "serviceSeq", description = "service seq", required = true) @PathVariable Integer serviceSeq,
            @Parameter(name = "servicemapSeq", description = "servicemap seq", required = true) @PathVariable Integer servicemapSeq,
            @Parameter(name = "ingressName", description = "ingressName", required = true) @PathVariable String ingressName
    ) throws Exception {

        log.debug("[BEGIN] deleteIngress");

        /**
         * cluster 상태 체크
         */
        clusterStateService.checkClusterStateByServicemap(servicemapSeq);

        ResultVO r = new ResultVO();

        ClusterVO cluster = k8sResourceService.setupCluster(servicemapSeq);

        ApiVersionType apiVersionType = ApiVersionType.valueOf(StringUtils.upperCase(apiVersion));
        ContextHolder.exeContext().setApiVersionType(apiVersionType);

        ingressSpecService.deleteIngress(cluster, cluster.getNamespaceName(), ingressName, ContextHolder.exeContext());

        log.debug("[END  ] deleteIngress");

    }


    /**
     * ======================================================================================================================================================================================================================================================
     */


    @PostMapping("/{apiVersion}/cluster/{clusterSeq}/namespace/{namespaceName}/ingress")
    @Operation(summary = "Ingress를 추가한다", description = "클러스터의 네임스페이스 안에 Ingress를 생성한다.")
    public K8sIngressVO addIngressSpec(
            @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v3"}), required = true) @PathVariable String apiVersion,
            @Parameter(name = "clusterSeq", description = "clusterSeq", required = true) @PathVariable Integer clusterSeq,
            @Parameter(name = "namespaceName", description = "namespaceName", required = true) @PathVariable String namespaceName,
            @Parameter(name = "ingressSpec", description = "추가하려는 ingressSpec", required = true) @RequestBody IngressSpecIntegrateVO ingressSpec
    ) throws Exception {

        log.debug("[BEGIN] addIngressSpec");

        if (DeployType.valueOf(ingressSpec.getDeployType()) != DeployType.GUI) {
            throw new CocktailException("Only GUI supported.", ExceptionType.InvalidParameter);
        }

        try {
            if (DeployType.valueOf(ingressSpec.getDeployType()) == DeployType.GUI) {

                ClusterVO cluster = clusterService.getCluster(clusterSeq);
                cluster.setNamespaceName(namespaceName);

                /**
                 * cluster 상태 체크
                 */
                clusterStateService.checkClusterState(cluster);

                ApiVersionType apiVersionType = ApiVersionType.valueOf(StringUtils.upperCase(apiVersion));
                ContextHolder.exeContext().setApiVersionType(apiVersionType);

                IngressSpecGuiVO ingressSpecGui = (IngressSpecGuiVO) ingressSpec;

                ingressSpecService.checkIngress(cluster, cluster.getNamespaceName(), true, ingressSpecGui);

                return ingressSpecService.createIngress(cluster, cluster.getNamespaceName(), ingressSpecGui, null, ContextHolder.exeContext());
            }
        } finally {
            log.debug("[END  ] addIngressSpec");
        }

        return null;
    }

    @PutMapping("/{apiVersion}/cluster/{clusterSeq}/namespace/{namespaceName}/ingress/{ingressName:.+}")
    @Operation(summary = "지정한 Ingress 수정", description = "지정한 Ingress 정보를 수정한다.")
    public K8sIngressVO updateIngress(
            @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v3"}), required = true) @PathVariable String apiVersion,
            @Parameter(name = "clusterSeq", description = "clusterSeq", required = true) @PathVariable Integer clusterSeq,
            @Parameter(name = "namespaceName", description = "namespaceName", required = true) @PathVariable String namespaceName,
            @Parameter(name = "ingressName", description = "ingressName", required = true) @PathVariable String ingressName,
            @Parameter(name = "ingressSpec", description = "수정하려는 ingressSpec", required = true) @RequestBody IngressSpecIntegrateVO ingressSpec
    ) throws Exception {

        log.debug("[BEGIN] updateIngress");

        DeployType deployType;
        try {
            deployType = DeployType.valueOf(ingressSpec.getDeployType());
        } catch (Exception e) {
            throw new CocktailException("DeployType invalid.", e, ExceptionType.InvalidParameter);
        }

        ClusterVO cluster = clusterService.getCluster(clusterSeq);
        cluster.setNamespaceName(namespaceName);

        /**
         * cluster 상태 체크
         */
        clusterStateService.checkClusterState(cluster);

        ApiVersionType apiVersionType = ApiVersionType.valueOf(StringUtils.upperCase(apiVersion));
        ContextHolder.exeContext().setApiVersionType(apiVersionType);

        try {
            if (deployType == DeployType.GUI) {
                IngressSpecGuiVO ingressSpecGui = (IngressSpecGuiVO) ingressSpec;
                if(!ingressName.equals(ingressSpecGui.getName())) {
                    throw new CocktailException("Can't change the Ingress name. (Ingress name is different)", ExceptionType.K8sIngressNameInvalid);
                }

                // valid
                ingressSpecService.checkIngress(cluster, cluster.getNamespaceName(), false, ingressSpecGui);

                return ingressSpecService.patchIngress(cluster, cluster.getNamespaceName(), ingressSpecGui, ContextHolder.exeContext());
            } else {
                IngressSpecYamlVO ingressSpecYaml = (IngressSpecYamlVO) ingressSpec;
                if(!ingressName.equals(ingressSpecYaml.getName())) {
                    throw new CocktailException("Can't change the Ingress name. (Ingress name is different)", ExceptionType.K8sIngressNameInvalid);
                }

                // valid 하기 위행 GUI로 변환
                IngressSpecGuiVO ingressSpecGui = ingressSpecService.convertIngressSpecYamlToGui(cluster, cluster.getNamespaceName(), ingressSpecYaml.getYaml());
                if(!ingressName.equals(ingressSpecGui.getName())) {
                    throw new CocktailException("Can't change the Ingress name. (Ingress name is different)", ExceptionType.K8sIngressNameInvalid);
                }
                if(!cluster.getNamespaceName().equals(ingressSpecGui.getNamespaceName())) {
                    throw new CocktailException("Can't change the Ingress namespace. (Ingress namespace is different)", ExceptionType.NamespaceNameInvalid);
                }

                // valid
                ingressSpecService.checkIngress(cluster, cluster.getNamespaceName(), false, ingressSpecGui);

                return ingressSpecService.patchIngress(cluster, cluster.getNamespaceName(), ingressSpecYaml.getYaml(), ContextHolder.exeContext());
            }
        } finally {
            log.debug("[END  ] updateIngress");
        }

    }

    @GetMapping("/{apiVersion}/cluster/{clusterSeq}/namespace/{namespaceName}/ingress/{ingressName:.+}")
    @Operation(summary = "지정한 Ingress 상세 정보 응답", description = "클러스터의 네임스페이스안에 지정 Ingress의 상세 정보를 응답한다.")
    public K8sIngressVO getIngressSpec(
            @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v3"}), required = true) @PathVariable String apiVersion,
            @Parameter(name = "clusterSeq", description = "clusterSeq", required = true) @PathVariable Integer clusterSeq,
            @Parameter(name = "namespaceName", description = "namespaceName", required = true) @PathVariable String namespaceName,
            @Parameter(name = "ingressName", description = "ingressName", required = true) @PathVariable String ingressName
    ) throws Exception {

        log.debug("[BEGIN] getIngressSpec");

        ClusterVO cluster = clusterService.getCluster(clusterSeq);
        cluster.setNamespaceName(namespaceName);

        /**
         * cluster 상태 체크
         */
        clusterStateService.checkClusterState(cluster);

        ApiVersionType apiVersionType = ApiVersionType.valueOf(StringUtils.upperCase(apiVersion));
        ContextHolder.exeContext().setApiVersionType(apiVersionType);

        K8sIngressVO k8sIngress = ingressSpecService.getIngress(cluster, cluster.getNamespaceName(), ingressName, ContextHolder.exeContext());

        log.debug("[END  ] getIngressSpec");

        return k8sIngress;
    }

    @GetMapping("/{apiVersion}/cluster/{clusterSeq}/namespace/{namespaceName}/ingresses")
    @Operation(summary = "Ingress 목록 반환", description = "클러스터의 네임스페이스안의 모든 Ingress 목록을 응답한다.")
    public List<K8sIngressVO> getIngressSpecs(
            @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v2"}), required = true) @PathVariable String apiVersion,
            @Parameter(name = "clusterSeq", description = "clusterSeq", required = true) @PathVariable Integer clusterSeq,
            @Parameter(name = "namespaceName", description = "namespaceName", required = true) @PathVariable String namespaceName,
            @Parameter(name = "acloudOnly", description = "Acloud에서 관리되는 인그레스만 조회 여부", schema = @Schema(allowableValues = {"true","false"}, defaultValue = "false")) @RequestParam(value = "acloudOnly", defaultValue = "false", required = false) Boolean acloudOnly
    ) throws Exception {

        log.debug("[BEGIN] getIngressSpecs");

        ClusterVO cluster = clusterService.getCluster(clusterSeq);
        cluster.setNamespaceName(namespaceName);

        /**
         * cluster 상태 체크
         */
        clusterStateService.checkClusterState(cluster);

        ApiVersionType apiVersionType = ApiVersionType.valueOf(StringUtils.upperCase(apiVersion));
        ContextHolder.exeContext().setApiVersionType(apiVersionType);


        String field = String.format("%s.%s=%s", KubeConstants.META, KubeConstants.META_NAMESPACE, cluster.getNamespaceName());
        /**
         * Acloud에서 관리되는 서비스만 조회 or 전체 조회 판단. : 2019.06.11
         */
        String label = null;
        if(acloudOnly) {
            label = KubeConstants.LABELS_COCKTAIL_KEY;
        }
        List<K8sIngressVO> cocktailK8sIngresses = ingressSpecService.getIngresses(cluster, field, label, ContextHolder.exeContext());

        List<K8sIngressVO> k8sIngresses = new ArrayList<>();
        if(acloudOnly) {
            // AcloudOnly 불가능.. Cocktail Label 사라짐..
            // k8sIngresses = cocktailK8sIngresses.stream().filter(svc -> ("Y".equals(svc.getIsManagedInCocktail()))).collect(Collectors.toList());
            k8sIngresses.addAll(cocktailK8sIngresses);
        }
        else {
            k8sIngresses.addAll(cocktailK8sIngresses);
        }


        log.debug("[END  ] getIngressSpecs");

        return k8sIngresses;
    }

    @DeleteMapping("/{apiVersion}/cluster/{clusterSeq}/namespace/{namespaceName}/ingress/{ingressName:.+}")
    @Operation(summary = "지정한 Ingress 삭제", description = "클러스터의 네임스페이스안에 지정한 Ingress를 삭제 한다.")
    public void deleteIngress(
            @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v2"}), required = true) @PathVariable String apiVersion,
            @Parameter(name = "clusterSeq", description = "clusterSeq", required = true) @PathVariable Integer clusterSeq,
            @Parameter(name = "namespaceName", description = "namespaceName", required = true) @PathVariable String namespaceName,
            @Parameter(name = "ingressName", description = "ingressName", required = true) @PathVariable String ingressName
    ) throws Exception {

        log.debug("[BEGIN] deleteIngress");

        ClusterVO cluster = clusterService.getCluster(clusterSeq);
        cluster.setNamespaceName(namespaceName);

        /**
         * cluster 상태 체크
         */
        clusterStateService.checkClusterState(cluster);

        ResultVO r = new ResultVO();

        ApiVersionType apiVersionType = ApiVersionType.valueOf(StringUtils.upperCase(apiVersion));
        ContextHolder.exeContext().setApiVersionType(apiVersionType);

        ingressSpecService.deleteIngress(cluster, cluster.getNamespaceName(), ingressName, ContextHolder.exeContext());

        log.debug("[END  ] deleteIngress");

    }

    @GetMapping("/{apiVersion}/cluster/{clusterSeq}/ingresses")
    @Operation(summary = "클러스터안의 전체 Ingress 목록 반환", description = "클러스터안의 전체 Ingress 목록을 응답한다.")
    public List<K8sIngressVO> getIngressSpecsInCluster(
            @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v2"}), required = true) @PathVariable String apiVersion,
            @Parameter(name = "clusterSeq", description = "clusterSeq", required = true) @PathVariable Integer clusterSeq,
            @Parameter(name = "serviceSeq", description = "serviceSeq") @RequestParam(name = "serviceSeq", required = false) Integer serviceSeq,
            @Parameter(name = "acloudOnly", description = "Acloud에서 관리되는 인그레스만 조회 여부", schema = @Schema(allowableValues = {"true","false"}, defaultValue = "false")) @RequestParam(value = "acloudOnly", defaultValue = "false", required = false) Boolean acloudOnly
    ) throws Exception {

        log.debug("[BEGIN] getIngressSpecs");

        /**
         * cluster 상태 체크
         */
        clusterStateService.checkClusterState(clusterSeq);

        ApiVersionType apiVersionType = ApiVersionType.valueOf(StringUtils.upperCase(apiVersion));
        ContextHolder.exeContext().setApiVersionType(apiVersionType);

        ClusterVO cluster = clusterService.getCluster(clusterSeq);
        Set<String> namespaces = new HashSet<>();
        if (serviceSeq != null) {
            namespaces = servicemapService.getNamespaceNamesByServiceInCluster(serviceSeq, clusterSeq);
        }

        /**
         * Acloud에서 관리되는 서비스만 조회 or 전체 조회 판단. : 2019.06.11
         */
        String label = null;
        if(acloudOnly) {
            label = KubeConstants.LABELS_COCKTAIL_KEY;
        }
        List<K8sIngressVO> k8sIngresses = new ArrayList<>();
        List<K8sIngressVO> k8sIngressesTmp = ingressSpecService.getIngresses(cluster, null, label, ContextHolder.exeContext());
        for (K8sIngressVO k8sIngressRow : k8sIngressesTmp) {
            if (serviceSeq != null) {
                if (CollectionUtils.isNotEmpty(namespaces) && namespaces.contains(k8sIngressRow.getNamespace())) {
                    k8sIngresses.add(k8sIngressRow);
                }
            } else {
                k8sIngresses.add(k8sIngressRow);
            }
        }

        log.debug("[END  ] getIngressSpecs");

        return k8sIngresses;
    }

    @GetMapping("/{apiVersion}/cluster/{clusterSeq}/ingressclassnames")
    @Operation(summary = "Ingress Class 목록 응답", description = "지정한 클러스터에 속한 IngressClass명(Nginx Ingress Controller) 목록을 응답한다.")
    public List<String> getIngressClassNamesInCluster(
            @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v2"}), required = true) @PathVariable String apiVersion,
            @Parameter(name = "clusterSeq", description = "clusterSeq", required = true) @PathVariable Integer clusterSeq,
            @Parameter(name = "withDeployIngressController", description = "배포된 nginx-ingress-controller의 정보를 조회하여 ingressClass 정보 사용유무", schema = @Schema(allowableValues = {"true","false"}, defaultValue = "false")) @RequestParam(value = "withDeployIngressController", defaultValue = "false", required = false) Boolean withDeployIngressController,
            @Parameter(name = "allNamespace", description = "all namespace 조회유무", schema = @Schema(allowableValues = {"true","false"}, defaultValue = "false")) @RequestParam(value = "allNamespace", defaultValue = "false", required = false) Boolean allNamespace
    ) throws Exception {

        log.debug("[BEGIN] getIngressClassNamesInCluster");

        /**
         * cluster 상태 체크
         */
        clusterStateService.checkClusterState(clusterSeq);

        ApiVersionType apiVersionType = ApiVersionType.valueOf(StringUtils.upperCase(apiVersion));
        ContextHolder.exeContext().setApiVersionType(apiVersionType);

        ClusterVO cluster = clusterService.getCluster(clusterSeq);
        List<String> result = ingressSpecService.getIngressClassNames(cluster, withDeployIngressController, allNamespace);

        log.debug("[END  ] getIngressClassNamesInCluster");

        return result;
    }
}
