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
import run.acloud.api.resource.enums.SecretType;
import run.acloud.api.resource.service.K8sResourceService;
import run.acloud.api.resource.service.SecretService;
import run.acloud.api.resource.util.ResourceUtil;
import run.acloud.api.resource.vo.SecretGuiVO;
import run.acloud.api.resource.vo.SecretIntegrateVO;
import run.acloud.api.resource.vo.SecretYamlVO;
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
 * Created on 2017. 8. 30.
 */
@Tag(name = "Kubernetes Secret Management", description = "쿠버네티스 Secret 관리 기능을 제공한다.")
@Slf4j
@RequestMapping(value = "/api/secret")
@RestController
@Validated
public class SecretController {

    @Autowired
    private K8sResourceService k8sResourceService;

    @Autowired
    private ClusterStateService clusterStateService;

    @Autowired
    private SecretService secretService;

    @PostMapping("/service/{serviceSeq}/servicemap/{servicemapSeq}")
    @Operation(summary = "Secret을 추가한다", description = "워크스페이스의 서비스맵에 Secret을 추가한다.")
    public SecretGuiVO addSecret(
        @Parameter(name = "serviceSeq", description = "service seq") @PathVariable Integer serviceSeq,
        @Parameter(name = "servicemapSeq", description = "servicemap seq") @PathVariable Integer servicemapSeq,
        @Parameter(name = "secret", description = "추가하려는 secret") @RequestBody SecretIntegrateVO secretParam) throws Exception {
        log.debug("[BEGIN] addSecret");

        SecretGuiVO secret;
        if (DeployType.valueOf(secretParam.getDeployType()) != DeployType.GUI) {
            throw new CocktailException("Only GUI supported.", ExceptionType.InvalidParameter);
        }
        else {
            secret = (SecretGuiVO)secretParam;
        }

        /**cluster 상태 체크 */
        clusterStateService.checkClusterStateByServicemap(servicemapSeq);

        if (StringUtils.isBlank(secret.getName()) || !secret.getName().matches(KubeConstants.RULE_RESOURCE_NAME)) {
            throw new CocktailException("Secret name is invalid", ExceptionType.K8sSecretNameInvalid);
        }

        SecretGuiVO existSecret = secretService.getSecret(servicemapSeq, secret.getName());
        if(existSecret != null){
            throw new CocktailException("Secret already exists!!", ExceptionType.SecretNameAlreadyExists);
        }

        secretService.setSecretData(secret, serviceSeq);

        secretService.checkSecretValidation(secret);

        secret = secretService.createSecret(servicemapSeq, secret);

        if(secret != null){
            for(Map.Entry<String, String> dataEntry : secret.getData().entrySet()){
                dataEntry.setValue("");
            }
        }
        log.debug("[END  ] addSecret");

        return secret;
    }

    @PostMapping("/{apiVersion}/cluster/{clusterSeq}/namespace/{namespaceName}")
    @Operation(summary = "Secret을 추가한다", description = "클러스터의 네임스페이스에 Secret을 추가한다.")
    public SecretGuiVO addSecretWithCluster(
        @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v2"})) @PathVariable String apiVersion,
        @Parameter(name = "clusterSeq", description = "cluster sequence", required = true) @PathVariable Integer clusterSeq,
        @Parameter(name = "namespaceName", description = "조회하려는 Namespace 명", required = true) @PathVariable String namespaceName,
        @Parameter(name = "secret", description = "추가하려는 secret") @RequestBody SecretIntegrateVO secretParam) throws Exception {
        log.debug("[BEGIN] addSecretWithCluster");

        SecretGuiVO secret;
        if (DeployType.valueOf(secretParam.getDeployType()) != DeployType.GUI) {
            throw new CocktailException("Only GUI supported.", ExceptionType.InvalidParameter);
        }
        else {
            secret = (SecretGuiVO)secretParam;
        }

        /**cluster 상태 체크 */
        clusterStateService.checkClusterState(clusterSeq);

        if (StringUtils.isBlank(secret.getName()) || !secret.getName().matches(KubeConstants.RULE_RESOURCE_NAME)) {
            throw new CocktailException("Secret name is invalid", ExceptionType.K8sSecretNameInvalid);
        }

        SecretGuiVO existSecret = secretService.getSecret(clusterSeq, namespaceName, secret.getName());
        if(existSecret != null){
            throw new CocktailException("Secret already exists!!", ExceptionType.SecretNameAlreadyExists);
        }

        secretService.setSecretData(secret, clusterSeq, namespaceName);

        secretService.checkSecretValidation(secret);

        secret = secretService.createSecret(clusterSeq, namespaceName, secret, null);

        if(secret != null){
            for(Map.Entry<String, String> dataEntry : secret.getData().entrySet()){
                dataEntry.setValue("");
            }
        }
        log.debug("[END  ] addSecretWithCluster");

        return secret;
    }

    @GetMapping("/service/{serviceSeq}/servicemap/{servicemapSeq}/secret/{secretName:.+}")
    @Operation(summary = "Secret 조회", description = "워크스페이스 - 서비스맵의 시크릿을 조회한다.")
    public SecretGuiVO getSecret(
            @Parameter(name = "serviceSeq", description = "service seq") @PathVariable Integer serviceSeq,
            @Parameter(name = "servicemapSeq", description = "servicemap seq") @PathVariable Integer servicemapSeq,
            @Parameter(name = "secretName", description = "secret name") @PathVariable String secretName) throws Exception {
        log.debug("[BEGIN] getSecret");

        /**
         * cluster 상태 체크
         */
        clusterStateService.checkClusterStateByServicemap(servicemapSeq);

        SecretGuiVO secret = secretService.getSecret(servicemapSeq, secretName);
        log.debug("[END  ] getSecret");
        return secret;
    }

    @GetMapping("/{apiVersion}/cluster/{clusterSeq}/namespace/{namespaceName}/secret/{secretName:.+}")
    @Operation(summary = "Secret 조회", description = "클러스터 - 네임스페이스 안의 시크릿을 조회한다.")
    public SecretGuiVO getSecretWithCluster(
        @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v2"})) @PathVariable String apiVersion,
        @Parameter(name = "clusterSeq", description = "cluster sequence", required = true) @PathVariable Integer clusterSeq,
        @Parameter(name = "namespaceName", description = "조회하려는 Namespace 명", required = true) @PathVariable String namespaceName,
        @Parameter(name = "secretName", description = "조회하려는 Secret의 이름", required = true) @PathVariable String secretName
    ) throws Exception {
        log.debug("[BEGIN] getSecret");

        if (StringUtils.isBlank(secretName)) {
            throw new CocktailException("secretName is required.", ExceptionType.InvalidParameter);
        }

        ExecutingContextVO ctx = ContextHolder.exeContext();
        ctx.setApiVersionType(ApiVersionType.V2);

        SecretGuiVO secret = secretService.getSecret(clusterSeq, namespaceName, secretName);

        if(secret == null) {
            throw new CocktailException("Secret Not Found", ExceptionType.K8sSecretNotFound);
        }

        log.debug("[END  ] getSecret");

        return secret;
    }

    @GetMapping("/service/{serviceSeq}/servicemap/{servicemapSeq}/secrets")
    @Operation(summary = "Secret 목록 조회", description = "워크스페이스 - 서비스맵 안의 시크릿 목록을 조회한다.")
    public List<SecretGuiVO> getSecrets(
            @Parameter(name = "serviceSeq", description = "service seq") @PathVariable Integer serviceSeq,
            @Parameter(name = "servicemapSeq", description = "servicemap seq") @PathVariable Integer servicemapSeq,
            @Parameter(name = "acloudOnly", description = "Acloud에서 관리되는 시크릿만 조회 여부", schema = @Schema(allowableValues = {"true","false"}, defaultValue = "false")) @RequestParam(value = "acloudOnly", defaultValue = "false", required = false) Boolean acloudOnly
    ) throws Exception {

        log.debug("[BEGIN] getSecrets");

        /**
         * cluster 상태 체크
         */
        clusterStateService.checkClusterStateByServicemap(servicemapSeq);

        List<SecretGuiVO> secrets;
        if(acloudOnly) {
            String label = String.format("%s=%s", KubeConstants.LABELS_COCKTAIL_KEY, ResourceUtil.getResourcePrefix());
            secrets = secretService.getSecrets(servicemapSeq, null, label, false);
        }
        else {
            secrets = secretService.getSecrets(servicemapSeq, null, null, true);
        }

        log.debug("[END  ] getSecrets");

        return secrets;
    }

    @GetMapping("/{apiVersion}/cluster/{clusterSeq}/namespace/{namespaceName}/secrets")
    @Operation(summary = "Secret 목록 조회", description = "클러스터 - 네임스페이스 안의 시크릿 목록을 조회한다.")
    public List<SecretGuiVO> getSecretsWithCluster(
            @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v2"})) @PathVariable String apiVersion,
            @Parameter(name = "clusterSeq", description = "cluster sequence", required = true) @PathVariable Integer clusterSeq,
            @Parameter(name = "namespaceName", description = "조회하려는 Namespace 명", required = true) @PathVariable String namespaceName,
            @Parameter(name = "acloudOnly", description = "Acloud에서 관리되는 시크릿만 조회 여부", schema = @Schema(allowableValues = {"true","false"}, defaultValue = "false")) @RequestParam(value = "acloudOnly", defaultValue = "false", required = false) Boolean acloudOnly
    ) throws Exception {

        log.debug("[BEGIN] getSecretsWithCluster");

        /**
         * cluster 상태 체크
         */
        clusterStateService.checkClusterState(clusterSeq);

        List<SecretGuiVO> secrets;
        if(acloudOnly) {
            String label = String.format("%s=%s", KubeConstants.LABELS_COCKTAIL_KEY, ResourceUtil.getResourcePrefix());
            secrets = secretService.getSecrets(clusterSeq, namespaceName, null, label, false);
        }
        else {
            secrets = secretService.getSecrets(clusterSeq, namespaceName, null, null, true);
        }

        log.debug("[END  ] getSecretsWithCluster");

        return secrets;
    }

    @GetMapping("/{apiVersion}/cluster/id/{clusterId}/namespace/{namespaceName}/secrets")
    @Operation(summary = "Secret 목록 조회", description = "클러스터(ClusterId기준) - 네임스페이스 안의 시크릿 목록을 조회한다.")
    public List<SecretGuiVO> getSecretsWithClusterById(
            @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v2"})) @PathVariable String apiVersion,
            @Parameter(name = "clusterId", description = "clusterId", required = true) @PathVariable String clusterId,
            @Parameter(name = "namespaceName", description = "조회하려는 Namespace 명", required = true) @PathVariable String namespaceName,
            @Parameter(name = "acloudOnly", description = "Acloud에서 관리되는 시크릿만 조회 여부", schema = @Schema(allowableValues = {"true","false"}, defaultValue = "false")) @RequestParam(value = "acloudOnly", defaultValue = "false", required = false) Boolean acloudOnly
    ) throws Exception {

        log.debug("[BEGIN] getSecretsWithClusterById");

        /**
         * cluster 상태 체크
         */
        clusterStateService.checkClusterState(clusterId);

        List<SecretGuiVO> secrets;
        if(acloudOnly) {
            String label = String.format("%s=%s", KubeConstants.LABELS_COCKTAIL_KEY, ResourceUtil.getResourcePrefix());
            secrets = secretService.getSecrets(clusterId, namespaceName, null, label, false);
        }
        else {
            secrets = secretService.getSecrets(clusterId, namespaceName, null, null, true);
        }

        log.debug("[END  ] getSecretsWithClusterById");

        return secrets;
    }

    @PutMapping("/service/{serviceSeq}/servicemap/{servicemapSeq}/secret/{secretName:.+}")
    @Operation(summary = "Secret 수정", description = "워크스페이스 - 서비스맵안에 지정한 시크릿을 수정한다.")
    public SecretGuiVO updateSecret(
            @Parameter(name = "serviceSeq", description = "service seq") @PathVariable Integer serviceSeq,
            @Parameter(name = "servicemapSeq", description = "servicemap seq") @PathVariable Integer servicemapSeq,
            @Parameter(name = "secretName", description = "secret name") @PathVariable String secretName,
            @Parameter(name = "secret", description = "수정하려는 secret") @RequestBody SecretIntegrateVO secretParam) throws Exception {

        log.debug("[BEGIN] updateSecret");
        /**
         * cluster 상태 체크
         */
        clusterStateService.checkClusterStateByServicemap(servicemapSeq);

        if (StringUtils.isBlank(secretName) || !secretName.matches(KubeConstants.RULE_RESOURCE_NAME)) {
            throw new CocktailException("Secret name is invalid", ExceptionType.K8sSecretNameInvalid);
        }

        SecretGuiVO secret;
        if(DeployType.valueOf(secretParam.getDeployType()) == DeployType.GUI) {
            SecretGuiVO secretGui = (SecretGuiVO) secretParam;
            if(!secretName.equals(secretGui.getName())) {
                throw new CocktailException("Can't change the name. (name is different)", ExceptionType.K8sSecretNameInvalid);
            }

            secretService.checkSecretValidation(secretGui, false);
            secret = secretService.patchSecret(servicemapSeq, secretGui);
        }
        else {
            SecretYamlVO secretYaml = (SecretYamlVO) secretParam;

            if(!secretName.equals(secretYaml.getName())) {
                throw new CocktailException("Can't change the name. (name is different)", ExceptionType.K8sSecretNameInvalid);
            }
            // Valid check 하기 위행 GUI로 변환
            SecretGuiVO secretGui = secretService.convertYamlToSecret(servicemapSeq, secretYaml);
            secretService.checkSecretValidation(secretGui);

            secret = secretService.patchSecretWithYaml(servicemapSeq, secretGui, secretYaml);
        }

        if(secret != null){
            for(Map.Entry<String, String> dataEntry : secret.getData().entrySet()){
                dataEntry.setValue("");
            }
        }

        log.debug("[END  ] updateSecret");

        return secret;
    }

    @PutMapping("/{apiVersion}/cluster/{clusterSeq}/namespace/{namespaceName}/secret/{secretName:.+}")
    @Operation(summary = "Secret 수정", description = "클러스터 - 네임스페이안에 지정한 시크릿을 수정한다.")
    public SecretGuiVO updateSecretWithCluster(
        @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v2"})) @PathVariable String apiVersion,
        @Parameter(name = "clusterSeq", description = "cluster sequence", required = true) @PathVariable Integer clusterSeq,
        @Parameter(name = "namespaceName", description = "조회하려는 Namespace 명", required = true) @PathVariable String namespaceName,
        @Parameter(name = "secretName", description = "secret name") @PathVariable String secretName,
        @Parameter(name = "secret", description = "수정하려는 secret") @RequestBody SecretIntegrateVO secretParam) throws Exception {

        log.debug("[BEGIN] updateSecretWithCluster");

        /** cluster 상태 체크 */
        clusterStateService.checkClusterState(clusterSeq);
        ClusterVO cluster = k8sResourceService.setupCluster(clusterSeq, namespaceName);

        if (StringUtils.isBlank(secretName) || !secretName.matches(KubeConstants.RULE_RESOURCE_NAME)) {
            throw new CocktailException("Secret name is invalid", ExceptionType.K8sSecretNameInvalid);
        }

        SecretGuiVO secret;
        if(DeployType.valueOf(secretParam.getDeployType()) == DeployType.GUI) {
            SecretGuiVO secretGui = (SecretGuiVO) secretParam;
            if(!secretName.equals(secretGui.getName())) {
                throw new CocktailException("Can't change the name. (name is different)", ExceptionType.K8sSecretNameInvalid);
            }

            secretService.checkSecretValidation(secretGui, false);
            secret = secretService.patchSecret(null, cluster, secretGui);
        }
        else {
            SecretYamlVO secretYaml = (SecretYamlVO) secretParam;
            if(!secretName.equals(secretYaml.getName())) {
                throw new CocktailException("Can't change the name. (name is different)", ExceptionType.K8sSecretNameInvalid);
            }
            if(!namespaceName.equals(secretYaml.getNamespace())) {
                throw new CocktailException("Can't change the namespace. (namespace is different)", ExceptionType.NamespaceNameInvalid);
            }

            // Valid check 하기 위행 GUI로 변환
            SecretGuiVO secretGui = secretService.convertYamlToSecret(null, cluster, secretYaml);
            secretService.checkSecretValidation(secretGui);

            secret = secretService.patchSecretWithYaml(null, cluster, secretGui, secretYaml);
        }

        if(secret != null){
            for(Map.Entry<String, String> dataEntry : secret.getData().entrySet()){
                dataEntry.setValue("");
            }
        }

        log.debug("[END  ] updateSecretWithCluster");

        return secret;
    }

    @DeleteMapping("/service/{serviceSeq}/servicemap/{servicemapSeq}/secret/{secretName:.+}")
    @Operation(summary = "Secret 삭제", description = "워크스페이스 - 서비스맵안에 지정한 시크릿을 삭제한다.")
    public ResultVO deleteSecret(
            @Parameter(name = "serviceSeq", description = "service seq") @PathVariable Integer serviceSeq,
            @Parameter(name = "servicemapSeq", description = "servicemap seq") @PathVariable Integer servicemapSeq,
            @Parameter(name = "secretName", description = "secret name") @PathVariable String secretName) throws Exception {

        log.debug("[BEGIN] deleteSecret");

        /**
         * cluster 상태 체크
         */
        clusterStateService.checkClusterStateByServicemap(servicemapSeq);

        ResultVO r = new ResultVO();
        secretService.deleteSecret(servicemapSeq, secretName);

        log.debug("[END  ] deleteSecret");

        return r;
    }

    @DeleteMapping("/cluster/{clusterSeq}/namespace/{namespaceName}/secret/{secretName:.+}")
    @Operation(summary = "Secret 삭", description = "클러스터 - 네임스페이스안에 지정한 시크릿을 삭제한다.")
    public ResultVO deleteSecretWithCluster(
        @Parameter(name = "clusterSeq", description = "cluster sequence", required = true) @PathVariable Integer clusterSeq,
        @Parameter(name = "namespaceName", description = "조회하려는 Namespace 명", required = true) @PathVariable String namespaceName,
        @Parameter(name = "secretName", description = "secret name") @PathVariable String secretName) throws Exception {

        log.debug("[BEGIN] deleteSecret");

        /** cluster 상태 체크 */
        clusterStateService.checkClusterState(clusterSeq);
        ClusterVO cluster = k8sResourceService.setupCluster(clusterSeq, namespaceName);

        ResultVO r = new ResultVO();
        secretService.deleteSecret(cluster, secretName);

        log.debug("[END  ] deleteSecret");

        return r;
    }

    @PostMapping("/service/{serviceSeq}/servicemap/{servicemapSeq}/secret/{secretName}/used")
    @Operation(summary = "Secret 사용여부 확인", description = "워크스페이스 - 서비스맵안에 지정한 시크릿의 사용여부를 응답한다.")
    public Map<String, Boolean> isUsedSecret(
            @Parameter(name = "serviceSeq", description = "service seq") @PathVariable Integer serviceSeq,
            @Parameter(name = "servicemapSeq", description = "servicemap seq") @PathVariable Integer servicemapSeq,
            @Parameter(name = "secretName", description = "secret name") @PathVariable String secretName,
            @Parameter(name = "checkVolume", description = "volume 체크", schema = @Schema(allowableValues = {"true","false"}), required = true) @RequestParam boolean checkVolume,
            @Parameter(name = "checkEnv", description = "환경변수 체크", schema = @Schema(allowableValues = {"true","false"}), required = true) @RequestParam boolean checkEnv,
            @Parameter(name = "checkEnvKey", description = "환경변수 key 체크", schema = @Schema(allowableValues = {"true","false"}), required = true) @RequestParam boolean checkEnvKey,
            @Parameter(name = "secret", description = "검사하려는 secret") @RequestBody SecretGuiVO secret) throws Exception{

        log.debug("[BEGIN] isUsedSecret");

        /**
         * cluster 상태 체크
         */
        clusterStateService.checkClusterStateByServicemap(servicemapSeq);

        if(!checkEnv){
            if(checkEnvKey){
                throw new CocktailException("checkEnv > checkEnvKey is invalid", ExceptionType.InvalidParameter);
            }
        }
        boolean isUsed = secretService.isUsedSecret(servicemapSeq, secret, checkVolume, checkEnv, checkEnvKey);

        Map<String, Boolean> resultMap = new HashMap<>();
        resultMap.put("isUsed", isUsed);

        log.debug("[END  ] isUsedSecret");

        return resultMap;
    }

    @PostMapping("/cluster/{clusterSeq}/namespace/{namespaceName}/secret/{secretName}/used")
    @Operation(summary = "Secret 사용여부 확인", description = "워크스페이스 - 서비스맵안에 지정한 시크릿의 사용여부를 응답한다.")
    public Map<String, Boolean> isUsedSecret(
            @Parameter(name = "clusterSeq", description = "cluster sequence", required = true) @PathVariable Integer clusterSeq,
            @Parameter(name = "namespaceName", description = "조회하려는 Namespace 명", required = true) @PathVariable String namespaceName,
            @Parameter(name = "secretName", description = "secret name") @PathVariable String secretName,
            @Parameter(name = "checkVolume", description = "volume 체크", schema = @Schema(allowableValues = {"true","false"}), required = true) @RequestParam boolean checkVolume,
            @Parameter(name = "checkEnv", description = "환경변수 체크", schema = @Schema(allowableValues = {"true","false"}), required = true) @RequestParam boolean checkEnv,
            @Parameter(name = "checkEnvKey", description = "환경변수 key 체크", schema = @Schema(allowableValues = {"true","false"}), required = true) @RequestParam boolean checkEnvKey,
            @Parameter(name = "secret", description = "검사하려는 secret") @RequestBody SecretGuiVO secret) throws Exception{

        log.debug("[BEGIN] isUsedSecret");

        /**
         * cluster 상태 체크
         */
        clusterStateService.checkClusterState(clusterSeq);

        if(!checkEnv){
            if(checkEnvKey){
                throw new CocktailException("checkEnv > checkEnvKey is invalid", ExceptionType.InvalidParameter);
            }
        }
        boolean isUsed = secretService.isUsedSecret(clusterSeq, namespaceName, secret, checkVolume, checkEnv, checkEnvKey);

        Map<String, Boolean> resultMap = new HashMap<>();
        resultMap.put("isUsed", isUsed);

        log.debug("[END  ] isUsedSecret");

        return resultMap;
    }

    @GetMapping("/types")
    @Operation(summary = "Secret 유형 조회", description = "지원하는 Secret 유형 목록을 조회한다.")
    public List<Map<String, Object>> getSupportedSecretTypes() throws Exception {

        log.debug("[BEGIN] getSupportedSecretTypes");

        List<Map<String, Object>> secretTypes = SecretType.getSupportedSecretTypes();

        log.debug("[END  ] getSupportedSecretTypes");

        return secretTypes;
    }
}
