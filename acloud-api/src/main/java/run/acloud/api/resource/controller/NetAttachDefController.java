package run.acloud.api.resource.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Lists;
import io.swagger.annotations.ApiOperation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import run.acloud.api.configuration.constants.AddonConstants;
import run.acloud.api.configuration.service.AddonService;
import run.acloud.api.configuration.service.ClusterStateService;
import run.acloud.api.configuration.vo.ClusterVO;
import run.acloud.api.cserver.enums.DeployType;
import run.acloud.api.cserver.service.ComponentService;
import run.acloud.api.k8sextended.util.Yaml;
import run.acloud.api.resource.constants.KubeConstants;
import run.acloud.api.resource.enums.K8sApiCniType;
import run.acloud.api.resource.enums.K8sApiKindType;
import run.acloud.api.resource.service.CRDResourceService;
import run.acloud.api.resource.service.K8sResourceService;
import run.acloud.api.resource.service.WorkloadResourceService;
import run.acloud.api.resource.vo.K8sCRDNetAttachDefGuiVO;
import run.acloud.api.resource.vo.K8sCRDNetAttachDefIntegrateVO;
import run.acloud.api.resource.vo.K8sCRDNetAttachDefYamlVO;
import run.acloud.api.resource.vo.K8sPodVO;
import run.acloud.commons.annotations.InHouse;
import run.acloud.commons.enums.ApiVersionType;
import run.acloud.commons.util.ObjectMapperUtils;
import run.acloud.commons.vo.ExecutingContextVO;
import run.acloud.framework.context.ContextHolder;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author wschoi@acornsoft.io
 * Created on 2017. 7. 27.
 */
@InHouse
@Tag(name = "Kubernetes CRD NetworkAttachmentDefinition Management", description = "쿠버네티스 CRD NetworkAttachmentDefinition에 대한 관리 기능을 제공한다.")
@Slf4j
@RequestMapping(value = "/api/netdef")
@RestController
@Validated
public class NetAttachDefController {
    @Autowired
    private AddonService addonService;

    @Autowired
    private ComponentService componentService;

    @Autowired
    private K8sResourceService k8sResourceService;

    @Autowired
    private CRDResourceService crdResourceService;

    @Autowired
    private WorkloadResourceService workloadResourceService;

    @Autowired
    private ClusterStateService clusterStateService;


    @PostMapping("/{apiVersion}/service/{serviceSeq}/servicemap/{servicemapSeq}")
    @Operation(summary = "NetworkAttachmentDefinition을 추가한다", description = "Service, Servicemap에 속함")
    public K8sCRDNetAttachDefGuiVO addNetAttachDef(
            @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v1"}), required = true) @PathVariable String apiVersion,
            @Parameter(name = "serviceSeq", description = "serviceSeq", required = true) @PathVariable Integer serviceSeq,
            @Parameter(name = "servicemapSeq", description = "servicemapSeq", required = true) @PathVariable Integer servicemapSeq,
            @Parameter(name = "netAttachDef", description = "추가하려는 netAttachDef") @RequestBody K8sCRDNetAttachDefIntegrateVO netAttachDefParam
    ) throws Exception {

        log.debug("[BEGIN] addNetAttachDef");

        K8sCRDNetAttachDefGuiVO netAttachDef;
        if (DeployType.valueOf(netAttachDefParam.getDeployType()) != DeployType.GUI) {
            throw new CocktailException("Only GUI supported.", ExceptionType.InvalidParameter);
        }
        else {
            netAttachDef = (K8sCRDNetAttachDefGuiVO)netAttachDefParam;
        }

        /** cluster 상태 체크 */
        clusterStateService.checkClusterStateByServicemap(servicemapSeq);
        ClusterVO cluster = crdResourceService.setupCluster(servicemapSeq);

        return this.addNetAttachDefProcessor(cluster, cluster.getNamespaceName(), netAttachDef);
    }

    @PostMapping("/{apiVersion}/cluster/{clusterSeq}/namespace/{namespaceName}")
    @Operation(summary = "NetworkAttachmentDefinition을 추가한다", description = "Service, Appmap에 속함")
    public K8sCRDNetAttachDefGuiVO addNetAttachDefWithCluster(
        @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v1"}), required = true) @PathVariable String apiVersion,
        @Parameter(name = "clusterSeq", description = "cluster sequence", required = true) @PathVariable Integer clusterSeq,
        @Parameter(name = "namespaceName", description = "조회하려는 Namespace 명", required = true) @PathVariable String namespaceName,
        @Parameter(name = "netAttachDef", description = "추가하려는 netAttachDef") @RequestBody K8sCRDNetAttachDefIntegrateVO netAttachDefParam
    ) throws Exception {

        log.debug("[BEGIN] addNetAttachDef");

        K8sCRDNetAttachDefGuiVO netAttachDef;
        if (DeployType.valueOf(netAttachDefParam.getDeployType()) != DeployType.GUI) {
            throw new CocktailException("Only GUI supported.", ExceptionType.InvalidParameter);
        }
        else {
            netAttachDef = (K8sCRDNetAttachDefGuiVO)netAttachDefParam;
        }

        /** cluster 상태 체크 */
        clusterStateService.checkClusterState(clusterSeq);
        ClusterVO cluster = crdResourceService.setupCluster(clusterSeq, namespaceName);

        return this.addNetAttachDefProcessor(cluster, namespaceName, netAttachDef);
    }

    /**
     * addNetAttachDefProcessor
     * @param cluster
     * @param namespaceName
     * @param netAttachDef
     * @return
     * @throws Exception
     */
    private K8sCRDNetAttachDefGuiVO addNetAttachDefProcessor(ClusterVO cluster,
                                                             String namespaceName,
                                                             K8sCRDNetAttachDefGuiVO netAttachDef) throws Exception
    {
        K8sCRDNetAttachDefGuiVO netAttachDefRet = new K8sCRDNetAttachDefGuiVO();

        if (StringUtils.isBlank(netAttachDef.getName()) || !netAttachDef.getName().matches(KubeConstants.RULE_RESOURCE_NAME)) {
            throw new CocktailException("NetworkAttachmentDefinition name is invalid", ExceptionType.K8sNetAttachDefNameInvalid);
        } else {
            String labels = String.format("%s in (%s,%s)", KubeConstants.LABELS_ADDON_CHART_KEY, AddonConstants.CHART_NAME_MULTI_NIC, AddonConstants.CHART_NAME_SR_IOV);
            List<String> addonNames = addonService.getAddonNames(cluster, labels);
            if (CollectionUtils.isNotEmpty(addonNames)) {
                if (!addonNames.contains(AddonConstants.CHART_NAME_MULTI_NIC)) {
                    throw new CocktailException("NetworkAttachmentDefinition is not supported(Multus).", ExceptionType.MultiNicNotSupported);
                }
                // TODO : 임시 주석
//                if (K8sApiCniType.valueOf(netAttachDef.getType()) == K8sApiCniType.SRIOV) {
//                    if (!addonNames.contains(AddonConstants.CHART_NAME_SR_IOV)) {
//                        throw new CocktailException("NetworkAttachmentDefinition is not supported(sriov).", ExceptionType.SriovNotSupported);
//                    }
//                } else {
//                    throw new CocktailException("NetworkAttachmentDefinition is not type supported.", ExceptionType.K8sNotSupported);
//                }
            } else {
                throw new CocktailException("NetworkAttachmentDefinition is not supported.", ExceptionType.K8sNotSupported);
            }

            netAttachDef.setNamespace(namespaceName);
            Map<String, Object> result = crdResourceService.getCustomObject(cluster, namespaceName, netAttachDef.getName(), K8sApiKindType.NETWORK_ATTACHMENT_DEFINITION);
            if(result != null){
                throw new CocktailException("NetworkAttachmentDefinition already exists!!", ExceptionType.K8sNetAttachDefAlreadyExists);
            }

            crdResourceService.validNetAttachDefConfig(netAttachDef);
        }

        Map<String, Object> result = crdResourceService.createCustomObject(cluster, namespaceName, K8sApiKindType.NETWORK_ATTACHMENT_DEFINITION, netAttachDef);
        if (MapUtils.isNotEmpty(result)) {
            crdResourceService.convertNetAttachDef(result, netAttachDefRet);
        }

        log.debug("[END  ] addNetAttachDef");

        return netAttachDefRet;    }


    @GetMapping("/{apiVersion}/service/{serviceSeq}/servicemap/{servicemapSeq}/netdef/{netAttachDefName:.+}")
    @Operation(summary = "지정한 NetworkAttachmentDefinition 반환")
    public K8sCRDNetAttachDefGuiVO getNetAttachDef(
            @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v1"}), required = true) @PathVariable String apiVersion,
            @Parameter(name = "serviceSeq", description = "serviceSeq", required = true) @PathVariable Integer serviceSeq,
            @Parameter(name = "servicemapSeq", description = "servicemapSeq", required = true) @PathVariable Integer servicemapSeq,
            @Parameter(name = "netAttachDefName", description = "netAttachDefName", required = true) @PathVariable String netAttachDefName
    ) throws Exception {

        log.debug("[BEGIN] getNetAttachDef");

        /**
         * cluster 상태 체크
         */
        clusterStateService.checkClusterStateByServicemap(servicemapSeq);

        K8sCRDNetAttachDefGuiVO netAttachDef = new K8sCRDNetAttachDefGuiVO();
        Map<String, Object> result = crdResourceService.getCustomObject(servicemapSeq, netAttachDefName, K8sApiKindType.NETWORK_ATTACHMENT_DEFINITION);

        if (MapUtils.isNotEmpty(result)) {
            crdResourceService.convertNetAttachDef(result, netAttachDef);
        }

        log.debug("[END  ] getNetAttachDef");

        return netAttachDef;
    }

    @GetMapping("/{apiVersion}/cluster/{clusterSeq}/namespace/{namespaceName}/netdef/{netAttachDefName:.+}")
    @Operation(summary = "Namespace > 네트워크 상세 조회", description = "Namespace > NetworkAttachmentDefinition 상세정보를 반환한다.")
    public K8sCRDNetAttachDefGuiVO getNetAttachDefWithCluster(
        @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v2"}), required = true) @PathVariable String apiVersion,
        @Parameter(name = "clusterSeq", description = "cluster sequence", required = true) @PathVariable Integer clusterSeq,
        @Parameter(name = "namespaceName", description = "조회하려는 Namespace 명", required = true) @PathVariable String namespaceName,
        @Parameter(name = "netAttachDefName", description = "조회하려는 netAttachDefName의 이름", required = true) @PathVariable String netAttachDefName
    ) throws Exception {
        log.debug("[BEGIN] getNetAttachDef");

        if (StringUtils.isBlank(netAttachDefName)) {
            throw new CocktailException("netAttachDefName is required.", ExceptionType.InvalidParameter);
        }

        ExecutingContextVO ctx = ContextHolder.exeContext();
        ctx.setApiVersionType(ApiVersionType.V2);

        K8sCRDNetAttachDefGuiVO netAttachDef = new K8sCRDNetAttachDefGuiVO();
        ClusterVO cluster = crdResourceService.setupCluster(clusterSeq, namespaceName);
        Map<String, Object> result = crdResourceService.getCustomObject(cluster, namespaceName, netAttachDefName, K8sApiKindType.NETWORK_ATTACHMENT_DEFINITION);

        if (MapUtils.isNotEmpty(result)) {
            crdResourceService.convertNetAttachDef(result, netAttachDef);
        }
        else {
            throw new CocktailException("NetAttachDef Not Found", ExceptionType.K8sNetAttachDefNotFound);
        }

        log.debug("[END  ] getNetAttachDef");

        return netAttachDef;
    }


    @GetMapping("/cni")
    @Operation(summary = "CNI 유형 정보 반환")
    public List<Map<String, Object>> getCniType() throws Exception {
        return K8sApiCniType.toAllList();
    }

    @GetMapping("/{apiVersion}/cluster/{clusterSeq}/cni")
    @Operation(summary = "CNI 유형 정보 반환")
    public List<Map<String, Object>> getCniTypeInCluster(
            @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v1"}), required = true) @PathVariable String apiVersion,
            @Parameter(name = "clusterSeq", description = "clusterSeq", required = true) @PathVariable Integer clusterSeq
    ) throws Exception {
        List<Map<String, Object>> cnis = Lists.newArrayList();
        List<Map<String, Object>> allCnis = K8sApiCniType.toAllList();
        if (CollectionUtils.isNotEmpty(allCnis)) {
            String labels = String.format("%s in (%s)", KubeConstants.LABELS_ADDON_CHART_KEY, AddonConstants.CHART_NAME_SR_IOV);
            List<String> addonNames = addonService.getAddonNames(clusterSeq, labels);

            if (CollectionUtils.isNotEmpty(addonNames)) {
                for (Map<String, Object> cniRow : allCnis) {
                    if (addonNames.contains(cniRow.get("chartName"))) {
                        cnis.add(cniRow);
                    }
                }
            }
        }

        return cnis;
    }

    @GetMapping("/{apiVersion}/service/{serviceSeq}/servicemap/{servicemapSeq}/netdefs")
    @Operation(summary = "지정한 Service-Appmap에 속한 NetworkAttachmentDefinition 목록 반환")
    public List<K8sCRDNetAttachDefGuiVO> getNetAttachDefs(
            @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v1"}), required = true) @PathVariable String apiVersion,
            @Parameter(name = "serviceSeq", description = "serviceSeq", required = true) @PathVariable Integer serviceSeq,
            @Parameter(name = "servicemapSeq", description = "servicemapSeq", required = true) @PathVariable Integer servicemapSeq
    ) throws Exception {

        log.debug("[BEGIN] getNetAttachDefs");

        /**
         * cluster 상태 체크
         */
        clusterStateService.checkClusterStateByServicemap(servicemapSeq);

        List<K8sCRDNetAttachDefGuiVO> netAttachDefs = new ArrayList<>();

        List<Map<String, Object>> results = crdResourceService.getCustomObjects(servicemapSeq, K8sApiKindType.NETWORK_ATTACHMENT_DEFINITION, null);
        if (CollectionUtils.isNotEmpty(results)) {
            for (Map<String, Object> resultRow : results) {
                K8sCRDNetAttachDefGuiVO netAttachDef = new K8sCRDNetAttachDefGuiVO();
                crdResourceService.convertNetAttachDef(resultRow, netAttachDef);
                netAttachDefs.add(netAttachDef);
            }
        }

        log.debug("[END  ] getNetAttachDefs");

        return netAttachDefs;
    }

    @GetMapping("/{apiVersion}/cluster/{clusterSeq}/namespace/{namespaceName}/netdefs")
    @Operation(summary = "지정한 Service-Appmap에 속한 NetworkAttachmentDefinition 목록 반환")
    public List<K8sCRDNetAttachDefGuiVO> getNetAttachDefsWithCluster(
            @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v2"}), required = true) @PathVariable String apiVersion,
            @Parameter(name = "clusterSeq", description = "cluster sequence", required = true) @PathVariable Integer clusterSeq,
            @Parameter(name = "namespaceName", description = "조회하려는 Namespace 명", required = true) @PathVariable String namespaceName
    ) throws Exception {

        log.debug("[BEGIN] getNetAttachDefsWithCluster");

        /**
         * cluster 상태 체크
         */
        clusterStateService.checkClusterState(clusterSeq);

        List<K8sCRDNetAttachDefGuiVO> netAttachDefs = new ArrayList<>();

        List<Map<String, Object>> results = crdResourceService.getCustomObjects(clusterSeq, namespaceName, K8sApiKindType.NETWORK_ATTACHMENT_DEFINITION, null);
        if (CollectionUtils.isNotEmpty(results)) {
            for (Map<String, Object> resultRow : results) {
                K8sCRDNetAttachDefGuiVO netAttachDef = new K8sCRDNetAttachDefGuiVO();
                crdResourceService.convertNetAttachDef(resultRow, netAttachDef);
                netAttachDefs.add(netAttachDef);
            }
        }

        log.debug("[END  ] getNetAttachDefsWithCluster");

        return netAttachDefs;
    }

    @GetMapping("/{apiVersion}/cluster/id/{clusterId}/namespace/{namespaceName}/netdefs")
    @Operation(summary = "지정한 Service-Appmap에 속한 NetworkAttachmentDefinition 목록 반환")
    public List<K8sCRDNetAttachDefGuiVO> getNetAttachDefsWithClusterById(
            @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v2"}), required = true) @PathVariable String apiVersion,
            @Parameter(name = "clusterId", description = "clusterId", required = true) @PathVariable String clusterId,
            @Parameter(name = "namespaceName", description = "조회하려는 Namespace 명", required = true) @PathVariable String namespaceName
    ) throws Exception {

        log.debug("[BEGIN] getNetAttachDefsWithClusterById");

        /**
         * cluster 상태 체크
         */
        clusterStateService.checkClusterState(clusterId);

        List<K8sCRDNetAttachDefGuiVO> netAttachDefs = new ArrayList<>();

        List<Map<String, Object>> results = crdResourceService.getCustomObjects(clusterId, namespaceName, K8sApiKindType.NETWORK_ATTACHMENT_DEFINITION, null);
        if (CollectionUtils.isNotEmpty(results)) {
            for (Map<String, Object> resultRow : results) {
                K8sCRDNetAttachDefGuiVO netAttachDef = new K8sCRDNetAttachDefGuiVO();
                crdResourceService.convertNetAttachDef(resultRow, netAttachDef);
                netAttachDefs.add(netAttachDef);
            }
        }

        log.debug("[END  ] getNetAttachDefsWithClusterById");

        return netAttachDefs;
    }

    @PutMapping("/{apiVersion}/service/{serviceSeq}/servicemap/{servicemapSeq}/netdef/{netAttachDefName:.+}")
    @Operation(summary = "지정한 NetworkAttachmentDefinition 수정(data 수정)")
    public K8sCRDNetAttachDefGuiVO updateNetAttachDef(
            @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v1"}), required = true) @PathVariable String apiVersion,
            @Parameter(name = "serviceSeq", description = "serviceSeq", required = true) @PathVariable Integer serviceSeq,
            @Parameter(name = "servicemapSeq", description = "servicemapSeq", required = true) @PathVariable Integer servicemapSeq,
            @Parameter(name = "netAttachDefName", description = "netAttachDefName", required = true) @PathVariable String netAttachDefName,
            @Parameter(name = "netAttachDef", description = "수정하려는 netAttachDef") @RequestBody K8sCRDNetAttachDefIntegrateVO netAttachDefParam
    ) throws Exception {

        log.debug("[BEGIN] updateNetAttachDef");

        /** cluster 상태 체크 */
        clusterStateService.checkClusterStateByServicemap(servicemapSeq);
        ClusterVO cluster = k8sResourceService.setupCluster(servicemapSeq);

        return updateNetAttachDefProcessor(cluster, cluster.getNamespaceName(), netAttachDefName, netAttachDefParam);

    }

    @PutMapping("/{apiVersion}/cluster/{clusterSeq}/namespace/{namespaceName}/netdef/{netAttachDefName:.+}")
    @Operation(summary = "지정한 NetworkAttachmentDefinition 수정(data 수정)")
    public K8sCRDNetAttachDefGuiVO updateNetAttachDefWithCluster(
        @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v1"}), required = true) @PathVariable String apiVersion,
        @Parameter(name = "clusterSeq", description = "cluster sequence", required = true) @PathVariable Integer clusterSeq,
        @Parameter(name = "namespaceName", description = "조회하려는 Namespace 명", required = true) @PathVariable String namespaceName,
        @Parameter(name = "netAttachDefName", description = "netAttachDefName", required = true) @PathVariable String netAttachDefName,
        @Parameter(name = "netAttachDef", description = "수정하려는 netAttachDef") @RequestBody K8sCRDNetAttachDefIntegrateVO netAttachDefParam
    ) throws Exception {

        log.debug("[BEGIN] updateNetAttachDef");

        /** cluster 상태 체크 */
        clusterStateService.checkClusterState(clusterSeq);
        ClusterVO cluster = k8sResourceService.setupCluster(clusterSeq, namespaceName);

        return updateNetAttachDefProcessor(cluster, namespaceName, netAttachDefName, netAttachDefParam);
    }

    /**
     * NetAttachDefProcessor
     * @param cluster
     * @param namespaceName
     * @param netAttachDefName
     * @param netAttachDefParam
     * @return
     * @throws Exception
     */
    private K8sCRDNetAttachDefGuiVO updateNetAttachDefProcessor(ClusterVO cluster,
                                                                String namespaceName,
                                                                String netAttachDefName,
                                                                K8sCRDNetAttachDefIntegrateVO netAttachDefParam) throws Exception
    {
        K8sCRDNetAttachDefGuiVO netAttachDef = new K8sCRDNetAttachDefGuiVO();
        if (DeployType.valueOf(netAttachDefParam.getDeployType()) == DeployType.GUI) {
            K8sCRDNetAttachDefGuiVO netAttachDefGui = (K8sCRDNetAttachDefGuiVO)netAttachDefParam;

            if (StringUtils.isBlank(netAttachDefGui.getName())) {
                throw new CocktailException("Network name is empty", ExceptionType.K8sNetAttachDefNameInvalid);
            }

            crdResourceService.validNetAttachDefConfig(netAttachDefGui);

            Map<String, Object> result = crdResourceService.replaceCustomObject(cluster, namespaceName, K8sApiKindType.NETWORK_ATTACHMENT_DEFINITION, netAttachDefGui);
            if (MapUtils.isNotEmpty(result)) {
                crdResourceService.convertNetAttachDef(result, netAttachDef);
            }
        }
        else {
            K8sCRDNetAttachDefYamlVO netAttachDefYaml = (K8sCRDNetAttachDefYamlVO)netAttachDefParam;

            if (StringUtils.isBlank(netAttachDefYaml.getName())) {
                throw new CocktailException("Network name is empty", ExceptionType.K8sNetAttachDefNameInvalid);
            }

            // validation Check를 위해 Gui모델로 변환..
            K8sCRDNetAttachDefGuiVO netAttachDefGui = new K8sCRDNetAttachDefGuiVO();

            Map<String, Object> netAttachDefMap = Yaml.getSnakeYaml().load(netAttachDefYaml.getYaml());
            if (MapUtils.isNotEmpty(netAttachDefMap)) {
                crdResourceService.convertNetAttachDef(netAttachDefMap, netAttachDefGui);

            }
            if(!netAttachDefName.equals(netAttachDefGui.getName())) {
                throw new CocktailException("Can't change the network name. (Network name is different)", ExceptionType.K8sNetAttachDefNameInvalid);
            }
            crdResourceService.validNetAttachDefConfig(netAttachDefGui);

            Map<String, Object> result = crdResourceService.replaceCustomObjectWithYaml(cluster, namespaceName, K8sApiKindType.NETWORK_ATTACHMENT_DEFINITION, netAttachDefGui, netAttachDefYaml);
            if (MapUtils.isNotEmpty(result)) {
                crdResourceService.convertNetAttachDef(result, netAttachDef);
            }
        }

        log.debug("[END  ] updateNetAttachDef");

        return netAttachDef;
    }
    @DeleteMapping("/{apiVersion}/service/{serviceSeq}/servicemap/{servicemapSeq}/netdef/{netAttachDefName:.+}")
    @Operation(summary = "지정한 NetworkAttachmentDefinition 삭제")
    public void deleteNetAttachDef(
            @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v1","v2"}), required = true) @PathVariable String apiVersion,
            @Parameter(name = "serviceSeq", description = "serviceSeq", required = true) @PathVariable Integer serviceSeq,
            @Parameter(name = "servicemapSeq", description = "servicemapSeq", required = true) @PathVariable Integer servicemapSeq,
            @Parameter(name = "netAttachDefName", description = "netAttachDefName", required = true) @PathVariable String netAttachDefName
    ) throws Exception {

        log.debug("[BEGIN] deleteNetAttachDef");

        /**
         * cluster 상태 체크
         */
        clusterStateService.checkClusterStateByServicemap(servicemapSeq);

        crdResourceService.deleteCustomObject(servicemapSeq, netAttachDefName, K8sApiKindType.NETWORK_ATTACHMENT_DEFINITION);

        log.debug("[END  ] deleteNetAttachDef");

    }

    @DeleteMapping("/{apiVersion}/cluster/{clusterSeq}/namespace/{namespaceName}/netdef/{netAttachDefName:.+}")
    @Operation(summary = "지정한 NetworkAttachmentDefinition 삭제")
    public void deleteNetAttachDefWithCluster(
        @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v1","v2"}), required = true) @PathVariable String apiVersion,
        @Parameter(name = "clusterSeq", description = "cluster sequence", required = true) @PathVariable Integer clusterSeq,
        @Parameter(name = "namespaceName", description = "조회하려는 Namespace 명", required = true) @PathVariable String namespaceName,
        @Parameter(name = "netAttachDefName", description = "netAttachDefName", required = true) @PathVariable String netAttachDefName
    ) throws Exception {

        log.debug("[BEGIN] deleteNetAttachDefWithCluster");

        /** cluster 상태 체크 */
        clusterStateService.checkClusterState(clusterSeq);
        ClusterVO cluster = crdResourceService.setupCluster(clusterSeq, namespaceName);

        crdResourceService.deleteCustomObject(cluster, namespaceName, netAttachDefName, K8sApiKindType.NETWORK_ATTACHMENT_DEFINITION);

        log.debug("[END  ] deleteNetAttachDefWithCluster");

    }

    @PostMapping("/{apiVersion}/service/{serviceSeq}/servicemap/{servicemapSeq}/netdef/{netAttachDefName:.+}/used")
    @Operation(summary = "지정한 Service-Servicemap에 속한 NetworkAttachmentDefinition 삭제시 사용유무 체크")
    public Map<String, Boolean> isUsedNetAttachDef(
            @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v1"}), required = true) @PathVariable String apiVersion,
            @Parameter(name = "serviceSeq", description = "service seq", required = true) @PathVariable Integer serviceSeq,
            @Parameter(name = "servicemapSeq", description = "servicemapSeq", required = true) @PathVariable Integer servicemapSeq,
            @Parameter(name = "netAttachDefName", description = "netAttachDefName", required = true) @PathVariable String netAttachDefName,
            @Parameter(name = "withK8s", description = "K8s도 포함하여 사용유무 체크", schema = @Schema(allowableValues = {"true","false"}, defaultValue = "false")) @RequestParam(value = "withK8s", required = false, defaultValue = "false") boolean withK8s
    ) throws Exception{

        log.debug("[BEGIN] isUsedNetAttachDef");

        boolean isUsed = false;

        /**
         * cluster 상태 체크
         */
        clusterStateService.checkClusterStateByServicemap(servicemapSeq);

        /**
         * K8s의 Pod Annotation에서 설정정보를 검색하여 사용유무 검색
         */
        if (!isUsed && withK8s) {
            ClusterVO cluster = crdResourceService.setupCluster(servicemapSeq);
            if (cluster != null) {
                List<K8sPodVO> pods = workloadResourceService.getPods(cluster.getClusterSeq(), null, cluster.getNamespaceName(), ContextHolder.exeContext());

                if (CollectionUtils.isNotEmpty(pods)) {
                    LOOP_POD_DETAIL:
                    for (K8sPodVO podRow : pods) {
                        if (MapUtils.isNotEmpty(podRow.getDetail().getAnnotations())) {

                        }
                        if (MapUtils.isNotEmpty(podRow.getDetail().getAnnotations())
                                && podRow.getDetail().getAnnotations().containsKey(KubeConstants.META_ANNOTATIONS_CNI_NETWORKS)) {
                            String config = podRow.getDetail().getAnnotations().get(KubeConstants.META_ANNOTATIONS_CNI_NETWORKS);
                            if (StringUtils.isNotBlank(config)) {
                                List<Map<String, String>> networks = ObjectMapperUtils.getMapper().readValue(config, new TypeReference<List<Map<String, String>>>(){});

                                for (Map<String, String> networkRow : networks) {
                                    if (StringUtils.equals(networkRow.get("name"), netAttachDefName)) {
                                        isUsed = true;
                                        break LOOP_POD_DETAIL;
                                    }
                                }
                            }
                        }
                    }

                }
            }
        }

        Map<String, Boolean> resultMap = new HashMap<>();
        resultMap.put("isUsed", isUsed);

        log.debug("[END  ] isUsedNetAttachDef");

        return resultMap;
    }

}
