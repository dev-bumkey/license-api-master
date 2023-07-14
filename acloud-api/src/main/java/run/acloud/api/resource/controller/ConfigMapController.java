package run.acloud.api.resource.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import run.acloud.api.configuration.service.ClusterStateService;
import run.acloud.api.configuration.vo.ClusterVO;
import run.acloud.api.cserver.enums.DeployType;
import run.acloud.api.resource.constants.KubeConstants;
import run.acloud.api.resource.service.ConfigMapService;
import run.acloud.api.resource.service.K8sResourceService;
import run.acloud.api.resource.util.ResourceUtil;
import run.acloud.api.resource.vo.ConfigMapGuiVO;
import run.acloud.api.resource.vo.ConfigMapIntegrateVO;
import run.acloud.api.resource.vo.ConfigMapYamlVO;
import run.acloud.commons.annotations.InHouse;
import run.acloud.commons.enums.ApiVersionType;
import run.acloud.commons.vo.ExecutingContextVO;
import run.acloud.commons.vo.ResultVO;
import run.acloud.framework.context.ContextHolder;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author wschoi@acornsoft.io
 * Created on 2017. 7. 27.
 */
@Tag(name = "Kubernetes ConfigMap Management", description = "쿠버네티스 ConfigMap에 대한 관리 기능을 제공한다.")
@Slf4j
@RequestMapping(value = "/api/configmap")
@RestController
@Validated
public class ConfigMapController {

    @Autowired
    private K8sResourceService k8sResourceService;

    @Autowired
    private ClusterStateService clusterStateService;

    @Autowired
    private ConfigMapService configMapService;

    @InHouse
    @PostMapping("/v2/service/{serviceSeq}/servicemap/{servicemapSeq}")
    @Operation(summary = "ConfigMap 생성", description = "ConfigMap 생성한다.")
    public ConfigMapGuiVO addConfigMapV2(
        @Parameter(name = "serviceSeq", description = "service seq") @PathVariable Integer serviceSeq,
        @Parameter(name = "servicemapSeq", description = "servicemap seq") @PathVariable Integer servicemapSeq,
        @Parameter(name = "configMap", description = "추가하려는 ConfigMap") @RequestBody ConfigMapIntegrateVO configMapParam
    ) throws Exception {

        log.debug("[BEGIN] addConfigMapV2");

        ConfigMapGuiVO configMap;
        if (DeployType.valueOf(configMapParam.getDeployType()) != DeployType.GUI) {
            throw new CocktailException("Only GUI supported.", ExceptionType.InvalidParameter);
        }
        else {
            configMap = (ConfigMapGuiVO)configMapParam;
        }

        /**
         * cluster 상태 체크
         */
        clusterStateService.checkClusterStateByServicemap(servicemapSeq);

        if (StringUtils.isBlank(configMap.getName()) || !configMap.getName().matches(KubeConstants.RULE_RESOURCE_NAME)) {
            throw new CocktailException("ConfigMap name is invalid", ExceptionType.K8sConfigMapNameInvalid);
        }

        ConfigMapGuiVO existConfigMap = configMapService.getConfigMap(servicemapSeq, configMap.getName());
        if(existConfigMap != null){
            throw new CocktailException("ConfigMap already exists!!", ExceptionType.K8sConfigMapAlreadyExists);
        }

        if(StringUtils.length(configMap.getDescription()) > 50){
            throw new CocktailException("ConfigMap description more than 50 characters.", ExceptionType.K8sConfigMapDescInvalid_MaxLengthLimit);
        }
        configMapService.validConfigMapData(configMap);

        configMap = configMapService.createConfigMap(servicemapSeq, configMap);

        log.debug("[END  ] addConfigMapV2");

        return configMap;
    }

    @PostMapping("/{apiVersion}/cluster/{clusterSeq}/namespace/{namespaceName}")
    @Operation(summary = "ConfigMap 생성", description = "ConfigMap 생성한다.")
    public ConfigMapGuiVO addConfigMapWithCluster(
        @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v2"})) @PathVariable String apiVersion,
        @Parameter(name = "clusterSeq", description = "cluster sequence", required = true) @PathVariable Integer clusterSeq,
        @Parameter(name = "namespaceName", description = "조회하려는 Namespace 명", required = true) @PathVariable String namespaceName,
        @Parameter(name = "configMap", description = "추가하려는 ConfigMap") @RequestBody ConfigMapIntegrateVO configMapParam
    ) throws Exception {

        log.debug("[BEGIN] Cube.addConfigMap");

        ConfigMapGuiVO configMap;
        if (DeployType.valueOf(configMapParam.getDeployType()) != DeployType.GUI) {
            throw new CocktailException("Only GUI supported.", ExceptionType.InvalidParameter);
        }
        else {
            configMap = (ConfigMapGuiVO)configMapParam;
        }

        /**cluster 상태 체크 */
        clusterStateService.checkClusterState(clusterSeq);

        if (StringUtils.isBlank(configMap.getName()) || !configMap.getName().matches(KubeConstants.RULE_RESOURCE_NAME)) {
            throw new CocktailException("ConfigMap name is invalid", ExceptionType.K8sConfigMapNameInvalid);
        }

        ConfigMapGuiVO existConfigMap = configMapService.getConfigMap(clusterSeq, namespaceName, configMap.getName());
        if(existConfigMap != null){
            throw new CocktailException("ConfigMap already exists!!", ExceptionType.K8sConfigMapAlreadyExists);
        }

        if(StringUtils.length(configMap.getDescription()) > 50){
            throw new CocktailException("ConfigMap description more than 50 characters.", ExceptionType.K8sConfigMapDescInvalid_MaxLengthLimit);
        }
        configMapService.validConfigMapData(configMap);

        configMap = configMapService.createConfigMap(clusterSeq, namespaceName, configMap, null);

        log.debug("[END  ] Cube.addConfigMap");

        return configMap;
    }

    @InHouse
    @GetMapping("/v2/service/{serviceSeq}/servicemap/{servicemapSeq}/configmap/{configMapName:.+}")
    @Operation(summary = "ConfigMap 상세", description = "ConfigMap 상세 조회한다.")
    public ConfigMapGuiVO getConfigMapV2(
            @Parameter(name = "serviceSeq", description = "service seq") @PathVariable Integer serviceSeq,
            @Parameter(name = "servicemapSeq", description = "servicemap seq") @PathVariable Integer servicemapSeq,
            @Parameter(name = "configMapName", description = "configMap name") @PathVariable String configMapName) throws Exception {
        log.debug("[BEGIN] getConfigMapV2");

        /**
         * cluster 상태 체크
         */
        clusterStateService.checkClusterStateByServicemap(servicemapSeq);

        ConfigMapGuiVO configMap = configMapService.getConfigMap(servicemapSeq, configMapName);

        log.debug("[END  ] getConfigMapV2");
        return configMap;
    }

    @GetMapping("/{apiVersion}/cluster/{clusterSeq}/namespace/{namespaceName}/configmap/{configMapName:.+}")
    @Operation(summary = "ConfigMap 상세", description = "ConfigMap 상세 조회한다.")
    public ConfigMapGuiVO getConfigMapWithCluster(
        @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v2"})) @PathVariable String apiVersion,
        @Parameter(name = "clusterSeq", description = "cluster sequence", required = true) @PathVariable Integer clusterSeq,
        @Parameter(name = "namespaceName", description = "조회하려는 Namespace 명", required = true) @PathVariable String namespaceName,
        @Parameter(name = "configMapName", description = "조회하려는 ConfigMap의 이름", required = true) @PathVariable String configMapName
    ) throws Exception {
        log.debug("[BEGIN] getConfigMap");

        if (StringUtils.isBlank(configMapName)) {
            throw new CocktailException("configMapName is required.", ExceptionType.InvalidParameter);
        }

        ExecutingContextVO ctx = ContextHolder.exeContext();
        ctx.setApiVersionType(ApiVersionType.V2);

        ConfigMapGuiVO configMap = configMapService.getConfigMap(clusterSeq, namespaceName, configMapName);

        if(configMap == null) {
            throw new CocktailException("ConfigMap Not Found", ExceptionType.K8sConfigMapNotFound);
        }

        log.debug("[END  ] getConfigMap");

        return configMap;
    }

    @InHouse
    @GetMapping("/v2/service/{serviceSeq}/servicemap/{servicemapSeq}/configmaps")
    @Operation(summary = "ConfigMap 목록", description = "ConfigMap 목록 조회한다.")
    public List<ConfigMapGuiVO> getConfigMapsV2(
            @Parameter(name = "serviceSeq", description = "service seq") @PathVariable Integer serviceSeq,
            @Parameter(name = "servicemapSeq", description = "servicemap seq") @PathVariable Integer servicemapSeq,
            @Parameter(name = "acloudOnly", description = "Acloud에서 관리되는 컨피그맵만 조회 여부", schema = @Schema(allowableValues = {"true","false"}, defaultValue = "false")) @RequestParam(value = "acloudOnly", defaultValue = "false", required = false) Boolean acloudOnly
    ) throws Exception {

        log.debug("[BEGIN] getConfigMapsV2");

        /**
         * cluster 상태 체크
         */
        clusterStateService.checkClusterStateByServicemap(servicemapSeq);

        List<ConfigMapGuiVO> configMaps;

        // ConfigMap Default를 false로 설정, Cocktail에서 설정하지 않은 ConfigMap까지 조회하도록 함.
        if(acloudOnly) {
            String label = String.format("%s=%s", KubeConstants.LABELS_COCKTAIL_KEY, ResourceUtil.getResourcePrefix());
            configMaps = configMapService.getConfigMaps(servicemapSeq, null, label);
        }
        else {
            configMaps = configMapService.getConfigMaps(servicemapSeq, null, null);
        }

        log.debug("[END  ] getConfigMapsV2");

        return configMaps;
    }

    @GetMapping("/{apiVersion}/cluster/{clusterSeq}/namespace/{namespaceName}/configmaps")
    @Operation(summary = "ConfigMap 목록", description = "ConfigMap 목록 조회한다.")
    public List<ConfigMapGuiVO> getConfigMapsWithCluster(
            @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v2"})) @PathVariable String apiVersion,
            @Parameter(name = "clusterSeq", description = "cluster sequence", required = true) @PathVariable Integer clusterSeq,
            @Parameter(name = "namespaceName", description = "조회하려는 Namespace 명", required = true) @PathVariable String namespaceName,
            @Parameter(name = "acloudOnly", description = "Acloud에서 관리되는 컨피그맵만 조회 여부", schema = @Schema(allowableValues = {"true","false"}, defaultValue = "false")) @RequestParam(value = "acloudOnly", defaultValue = "false", required = false) Boolean acloudOnly
    ) throws Exception {

        log.debug("[BEGIN] getConfigMapsWithCluster");

        /**
         * cluster 상태 체크
         */
        clusterStateService.checkClusterState(clusterSeq);

        List<ConfigMapGuiVO> configMaps;

        // ConfigMap Default를 false로 설정, Cocktail에서 설정하지 않은 ConfigMap까지 조회하도록 함.
        if(acloudOnly) {
            String label = String.format("%s=%s", KubeConstants.LABELS_COCKTAIL_KEY, ResourceUtil.getResourcePrefix());
            configMaps = configMapService.getConfigMaps(clusterSeq, namespaceName, null, label);
        }
        else {
            configMaps = configMapService.getConfigMaps(clusterSeq, namespaceName, null, null);
        }

        log.debug("[END  ] getConfigMapsWithCluster");

        return configMaps;
    }

    @InHouse
    @GetMapping("/{apiVersion}/cluster/id/{clusterId}/namespace/{namespaceName}/configmaps")
    @Operation(summary = "ConfigMap 목록(ID)", description = "ConfigMap 목록 조회한다.(ID)")
    public List<ConfigMapGuiVO> getConfigMapsWithClusterById(
            @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v2"})) @PathVariable String apiVersion,
            @Parameter(name = "clusterId", description = "clusterId", required = true) @PathVariable String clusterId,
            @Parameter(name = "namespaceName", description = "조회하려는 Namespace 명", required = true) @PathVariable String namespaceName,
            @Parameter(name = "acloudOnly", description = "Acloud에서 관리되는 컨피그맵만 조회 여부", schema = @Schema(allowableValues = {"true","false"}, defaultValue = "false")) @RequestParam(value = "acloudOnly", defaultValue = "false", required = false) Boolean acloudOnly
    ) throws Exception {

        log.debug("[BEGIN] getConfigMapsWithClusterById");

        /**
         * cluster 상태 체크
         */
        clusterStateService.checkClusterState(clusterId);

        List<ConfigMapGuiVO> configMaps;

        // ConfigMap Default를 false로 설정, Cocktail에서 설정하지 않은 ConfigMap까지 조회하도록 함.
        if(acloudOnly) {
            String label = String.format("%s=%s", KubeConstants.LABELS_COCKTAIL_KEY, ResourceUtil.getResourcePrefix());
            configMaps = configMapService.getConfigMaps(clusterId, namespaceName, null, label);
        }
        else {
            configMaps = configMapService.getConfigMaps(clusterId, namespaceName, null, null);
        }

        log.debug("[END  ] getConfigMapsWithClusterById");

        return configMaps;
    }

    @PostMapping("{apiVersion}/cluster/{clusterSeq}/namespace/{namespaceName}/configmap/{configMapName}/used")
    @Operation(summary = "ConfigMap 수정시 사용유무 체크", description = "ConfigMap 수정시 사용유무 체크한다.")
    public Map<String, Boolean> isUsedConfigMap(
            @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v2"})) @PathVariable String apiVersion,
            @Parameter(name = "clusterSeq", description = "cluster sequence", required = true) @PathVariable Integer clusterSeq,
            @Parameter(name = "namespaceName", description = "조회하려는 Namespace 명", required = true) @PathVariable String namespaceName,
            @Parameter(name = "configMapName", description = "ConfigMap name") @PathVariable String configMapName,
            @Parameter(name = "checkVolume", description = "volume 체크", schema = @Schema(allowableValues = {"true","false"}), required = true) @RequestParam boolean checkVolume,
            @Parameter(name = "checkEnv", description = "환경변수 체크", schema = @Schema(allowableValues = {"true","false"}), required = true) @RequestParam boolean checkEnv,
            @Parameter(name = "checkEnvKey", description = "환경변수 key 체크", schema = @Schema(allowableValues = {"true","false"}), required = true) @RequestParam boolean checkEnvKey,
            @Parameter(name = "configMap", description = "검사하려는 ConfigMap") @RequestBody ConfigMapGuiVO configMap) throws Exception{

        log.debug("[BEGIN] isUsedConfigMap");

        /**
         * cluster 상태 체크
         */
        clusterStateService.checkClusterState(clusterSeq);

        if(!checkEnv){
            if(checkEnvKey){
                throw new CocktailException("checkEnv > checkEnvKey is invalid", ExceptionType.InvalidParameter);
            }
        }
        boolean isUsed = configMapService.isUsedConfigMap(clusterSeq, namespaceName, configMap, checkVolume, checkEnv, checkEnvKey);

        Map<String, Boolean> resultMap = new HashMap<>();
        resultMap.put("isUsed", isUsed);

        log.debug("[END  ] isUsedConfigMap");

        return resultMap;
    }

    @InHouse
    @PutMapping("/v2/service/{serviceSeq}/servicemap/{servicemapSeq}/configmap/{configMapName:.+}")
    @Operation(summary = "ConfigMap 수정(data 수정)", description = "ConfigMap 수정한다.(data 수정)")
    public ConfigMapGuiVO updateConfigMapV2(
            @Parameter(name = "serviceSeq", description = "service seq") @PathVariable Integer serviceSeq,
            @Parameter(name = "servicemapSeq", description = "servicemap seq") @PathVariable Integer servicemapSeq,
            @Parameter(name = "configMapName", description = "configMap name") @PathVariable String configMapName,
            @Parameter(name = "configMap", description = "수정하려는 configMap") @RequestBody ConfigMapIntegrateVO configMapParam) throws Exception {

        log.debug("[BEGIN] updateConfigMapV2");

        /**
         * cluster 상태 체크
         */
        clusterStateService.checkClusterStateByServicemap(servicemapSeq);

        ConfigMapGuiVO configMap;
        if (DeployType.valueOf(configMapParam.getDeployType()) == DeployType.GUI) {
            ConfigMapGuiVO configMapGui = (ConfigMapGuiVO) configMapParam;

            configMapService.checkNameAndDescription(configMapName, configMapGui.getName(), configMapGui.getDescription());
            configMapService.validConfigMapData(configMapGui);

            configMap = configMapService.patchConfigMap(servicemapSeq, configMapGui);
        }
        else {
            ConfigMapYamlVO configMapYaml = (ConfigMapYamlVO) configMapParam;

            configMapService.checkNameAndDescription(configMapName, configMapYaml.getName(), "");

            configMap = configMapService.patchConfigMapWithYaml(servicemapSeq, configMapYaml);

            if(configMap == null) {
                throw new CocktailException("patchConfigMap fail! (Unknown error)", ExceptionType.K8sCocktailCloudUpdateFail);
            }
        }
        log.debug("[END  ] updateConfigMapV2");

        return configMap;
    }

    @PutMapping("/{apiVersion}/cluster/{clusterSeq}/namespace/{namespaceName}/configmap/{configMapName:.+}")
    @Operation(summary = "ConfigMap 수정(data 수정)", description = "ConfigMap 수정한다.(data 수정)")
    public ConfigMapGuiVO updateConfigMapWithCluster(
        @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v2"})) @PathVariable String apiVersion,
        @Parameter(name = "clusterSeq", description = "cluster sequence", required = true) @PathVariable Integer clusterSeq,
        @Parameter(name = "namespaceName", description = "조회하려는 Namespace 명", required = true) @PathVariable String namespaceName,
        @Parameter(name = "configMapName", description = "configMap name", required = true) @PathVariable String configMapName,
        @Parameter(name = "configMap", description = "수정하려는 configMap") @RequestBody ConfigMapIntegrateVO configMapParam) throws Exception {

        log.debug("[BEGIN] updateConfigMapV2");

        /** cluster 상태 체크 */
        clusterStateService.checkClusterState(clusterSeq);
        ClusterVO cluster = k8sResourceService.setupCluster(clusterSeq, namespaceName);

        ConfigMapGuiVO configMap;

        if (DeployType.valueOf(configMapParam.getDeployType()) == DeployType.GUI) {
            ConfigMapGuiVO configMapGui = (ConfigMapGuiVO) configMapParam;

            configMapService.checkNameAndDescription(configMapName, configMapGui.getName(), configMapGui.getDescription());
            configMapService.validConfigMapData(configMapGui);

            configMap = configMapService.patchConfigMap(null, cluster, configMapGui);
        }
        else {
            ConfigMapYamlVO configMapYaml = (ConfigMapYamlVO) configMapParam;
            if(!namespaceName.equals(configMapYaml.getNamespace())) {
                throw new CocktailException("Can't change the namespace. (namespace is different)", ExceptionType.NamespaceNameInvalid);
            }

            configMapService.checkNameAndDescription(configMapName, configMapYaml.getName(), "");

            configMap = configMapService.patchConfigMapWithYaml(null, cluster, configMapYaml);

            if(configMap == null) {
                throw new CocktailException("patchConfigMap fail! (Unknown error)", ExceptionType.K8sCocktailCloudUpdateFail);
            }
        }
        log.debug("[END  ] updateConfigMapV2");

        return configMap;
    }

    @InHouse
    @DeleteMapping("/v2/service/{serviceSeq}/servicemap/{servicemapSeq}/configmap/{configMapName:.+}")
    @Operation(summary = "ConfigMap 삭제", description = "ConfigMap 삭제한다.")
    public ResultVO deleteConfigMapV2(
            @Parameter(name = "serviceSeq", description = "service seq") @PathVariable Integer serviceSeq,
            @Parameter(name = "servicemapSeq", description = "servicemap seq") @PathVariable Integer servicemapSeq,
            @Parameter(name = "configMapName", description = "configMap name") @PathVariable String configMapName) throws Exception {

        log.debug("[BEGIN] deleteConfigMapV2");

        /**
         * cluster 상태 체크
         */
        clusterStateService.checkClusterStateByServicemap(servicemapSeq);

        ResultVO r = new ResultVO();
        configMapService.deleteConfigMap(servicemapSeq, configMapName);

        log.debug("[END  ] deleteConfigMapV2");

        return r;
    }

    @DeleteMapping("/{apiVersion}/cluster/{clusterSeq}/namespace/{namespaceName}/configmap/{configMapName:.+}")
    @Operation(summary = "ConfigMap 삭제", description = "ConfigMap 삭제한다.")
    public ResultVO deleteConfigMapWithCluster(
        @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v2"}), required = true) @PathVariable String apiVersion,
        @Parameter(name = "clusterSeq", description = "cluster sequence", required = true) @PathVariable Integer clusterSeq,
        @Parameter(name = "namespaceName", description = "조회하려는 Namespace 명", required = true) @PathVariable String namespaceName,
        @Parameter(name = "configMapName", description = "configMap name") @PathVariable String configMapName) throws Exception {

        log.debug("[BEGIN] deleteConfigMapWithCluster");

        /** cluster 상태 체크 */
        clusterStateService.checkClusterState(clusterSeq);
        ClusterVO cluster = k8sResourceService.setupCluster(clusterSeq, namespaceName);

        ResultVO r = new ResultVO();
        configMapService.deleteConfigMap(cluster, configMapName);

        log.debug("[END  ] deleteConfigMapWithCluster");

        return r;
    }

    @InHouse
    @PostMapping("/service/{serviceSeq}/servicemap/{servicemapSeq}/configmap/{configMapName}/used")
    @Operation(summary = "ConfigMap 수정시 사용유무 체크", description = "ConfigMap 수정시 사용유무 체크한다.")
    public Map<String, Boolean> isUsedConfigMap(
            @Parameter(name = "serviceSeq", description = "service seq") @PathVariable Integer serviceSeq,
            @Parameter(name = "servicemapSeq", description = "servicemap seq") @PathVariable Integer servicemapSeq,
            @Parameter(name = "configMapName", description = "ConfigMap name") @PathVariable String configMapName,
            @Parameter(name = "checkVolume", description = "volume 체크", schema = @Schema(allowableValues = {"true","false"}), required = true) @RequestParam boolean checkVolume,
            @Parameter(name = "checkEnv", description = "환경변수 체크", schema = @Schema(allowableValues = {"true","false"}), required = true) @RequestParam boolean checkEnv,
            @Parameter(name = "checkEnvKey", description = "환경변수 key 체크", schema = @Schema(allowableValues = {"true","false"}), required = true) @RequestParam boolean checkEnvKey,
            @Parameter(name = "configMap", description = "검사하려는 ConfigMap") @RequestBody ConfigMapGuiVO configMap) throws Exception{

        log.debug("[BEGIN] isUsedConfigMap");

        /**
         * cluster 상태 체크
         */
        clusterStateService.checkClusterStateByServicemap(servicemapSeq);

        if(!checkEnv){
            if(checkEnvKey){
                throw new CocktailException("checkEnv > checkEnvKey is invalid", ExceptionType.InvalidParameter);
            }
        }
        boolean isUsed = configMapService.isUsedConfigMap(servicemapSeq, configMap, checkVolume, checkEnv, checkEnvKey);

        Map<String, Boolean> resultMap = new HashMap<>();
        resultMap.put("isUsed", isUsed);

        log.debug("[END  ] isUsedConfigMap");

        return resultMap;
    }
}
