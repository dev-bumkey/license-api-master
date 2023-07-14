package run.acloud.api.resource.service;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.JsonObject;
import io.kubernetes.client.openapi.JSON;
import io.kubernetes.client.openapi.models.*;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.jettison.json.JSONObject;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import run.acloud.api.configuration.dao.IClusterMapper;
import run.acloud.api.configuration.service.ExternalRegistryService;
import run.acloud.api.configuration.vo.ClusterVO;
import run.acloud.api.configuration.vo.ExternalRegistryDetailVO;
import run.acloud.api.configuration.vo.ServiceRegistryUserVO;
import run.acloud.api.cserver.dao.IServiceMapper;
import run.acloud.api.cserver.dao.IServicemapMapper;
import run.acloud.api.cserver.enums.DeployType;
import run.acloud.api.cserver.util.ServerUtils;
import run.acloud.api.k8sextended.models.V1beta1CronJob;
import run.acloud.api.k8sextended.models.V1beta1CronJobSpec;
import run.acloud.api.k8sextended.models.V1beta1JobTemplateSpec;
import run.acloud.api.k8sextended.util.Yaml;
import run.acloud.api.resource.constants.KubeConstants;
import run.acloud.api.resource.enums.*;
import run.acloud.api.resource.task.K8sPatchSpecFactory;
import run.acloud.api.resource.task.K8sSpecFactory;
import run.acloud.api.resource.task.K8sWorker;
import run.acloud.api.resource.util.ResourceUtil;
import run.acloud.api.resource.vo.*;
import run.acloud.commons.util.Base64Utils;
import run.acloud.commons.util.CryptoUtils;
import run.acloud.commons.util.JsonUtils;
import run.acloud.commons.util.Utils;
import run.acloud.commons.vo.ExecutingContextVO;
import run.acloud.framework.context.ContextHolder;
import run.acloud.framework.enums.ExceptionBiz;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SecretService {

    @Resource(name = "cocktailSession")
    private SqlSessionTemplate sqlSession;

    @Autowired
    private K8sWorker k8sWorker;

    @Autowired
    private RBACResourceService rbacResourceService;

    @Autowired
    private ExternalRegistryService externalRegistryService;

    @Autowired
    private CertManagerService certManagerService;

    @Autowired
    private K8sPatchSpecFactory k8sPatchSpecFactory;

    /**
     * Secret 생성
     *
     * @param servicemapSeq
     * @param secret
     * @return
     * @throws Exception
     */
    public SecretGuiVO createSecret(Integer servicemapSeq, SecretGuiVO secret) throws Exception {
        return this.createSecret(servicemapSeq, secret, null);
    }

    public SecretGuiVO createDockerRegistrySecret(Integer servicemapSeq, DockerRegistrySecretVO secretParam) throws Exception {
        ClusterVO cluster = this.setupCluster(servicemapSeq);
        return this.createDockerRegistrySecret(cluster, cluster.getNamespaceName(), secretParam);
    }

    public SecretGuiVO createDockerRegistrySecret(Integer clusterSeq, String namespaceName, DockerRegistrySecretVO secretParam) throws Exception {
        ClusterVO cluster = this.setupCluster(clusterSeq, namespaceName);
        return this.createDockerRegistrySecret(cluster, cluster.getNamespaceName(), secretParam);
    }

    /**
     * docketRegistry secret 생성
     *
     * @param cluster - cluster.getNamespaceName 이 반드시 포함되어야함.
     * @param secretParam
     * @return
     * @throws Exception
     */
    public SecretGuiVO createDockerRegistrySecret(ClusterVO cluster, String namespaceName, DockerRegistrySecretVO secretParam) throws Exception {
        try {
            secretParam.setData(this.makeDockerRegistrySecretData(secretParam)); // replace
            this.createSecret(cluster, namespaceName, secretParam);
        } catch (Exception e) {
            boolean isSpecial = false;
            if(e instanceof CocktailException){
                if(((CocktailException) e).getType() == ExceptionType.K8sSecretCreationFail){
                    isSpecial = true;
                }
            }

            if (isSpecial){
                throw e;
            }else {
                throw new CocktailException("createDockerRegistrySecret fail!!", e, ExceptionType.K8sCocktailCloudCreateFail);
            }
        }
        return secretParam;
    }

    public Map<String, String> makeDockerRegistrySecretData(DockerRegistrySecretVO secret) {
        JsonObject joCon = new JsonObject();
        joCon.addProperty("username", secret.getUserName());
        joCon.addProperty("password", secret.getPassword());
        if (StringUtils.isNotBlank(secret.getEmail())) {
            joCon.addProperty("email", secret.getEmail());
        }
        byte[] bytes = String.format("%s:%s", secret.getUserName(), secret.getPassword()).getBytes();
        joCon.addProperty("auth", Base64.getEncoder().encodeToString(bytes));

        JsonObject joData = new JsonObject();
        joData.add(ResourceUtil.getRegistryUrl(secret.getServerUrl()), joCon);

        JsonObject rootData = new JsonObject();
        rootData.add("auths", joData);

        Map<String, String> data = new HashMap<>();
//        data.put(".dockercfg", Base64.getEncoder().encodeToString(joData.toString().getBytes()));
        data.put(KubeConstants.SECRET_TYPE_DOCKERCONFIGJSON_KEY, rootData.toString());

        return data;
    }

    public void setSecretData(SecretGuiVO secret, Integer clusterSeq, String namespace) throws Exception {
        IServiceMapper svcDao = sqlSession.getMapper(IServiceMapper.class);
        List<ServiceRegistryUserVO> services = svcDao.getRegistryUserByNamespace(clusterSeq, namespace);

        if (CollectionUtils.isNotEmpty(services)) {
            this.setSecretData(secret, services.stream().map(ServiceRegistryUserVO::getServiceSeq).collect(Collectors.toList()));
        }
    }

    /**
     * 등록된 외부 레지스트리 정보를 통한 DockerConfigJson 셋팅
     *
     * @param secret
     * @param serviceSeq
     * @throws Exception
     */
    public void setSecretData(SecretGuiVO secret, Integer serviceSeq) throws Exception {
        this.setSecretData(secret, Collections.singletonList(serviceSeq));
    }

    public void setSecretData(SecretGuiVO secret, List<Integer> serviceSeqs) throws Exception {
        if (secret != null && CollectionUtils.isNotEmpty(serviceSeqs)) {
            switch (secret.getType()) {
                case DockerRegistry -> {
                    if (BooleanUtils.toBoolean(secret.getExternalRegistryYn())) {
                        // 외부 레지스트리 정보 조회
                        ExternalRegistryDetailVO externalRegistryDetail = externalRegistryService.getExternalRegistry(secret.getExternalRegistrySeq(), null);
                        if (externalRegistryDetail != null) {
                            if (CollectionUtils.isEmpty(externalRegistryDetail.getServices())
                                    || (
                                    CollectionUtils.isNotEmpty(externalRegistryDetail.getServices())
                                            && !externalRegistryDetail.getServices().stream().filter(s -> (serviceSeqs.contains(s.getServiceSeq()))).findFirst().isPresent()
                            )
                            ) {
                                throw new CocktailException("External Registry Info is invalid.", ExceptionType.InvalidParameter, ExceptionBiz.SECRET);
                            }

                            DockerRegistrySecretVO dockerRegistrySecret = new DockerRegistrySecretVO();
                            dockerRegistrySecret.setUserName(CryptoUtils.decryptAES(externalRegistryDetail.getAccessId()));
                            dockerRegistrySecret.setPassword(CryptoUtils.decryptAES(externalRegistryDetail.getAccessSecret()));
                            dockerRegistrySecret.setServerUrl(externalRegistryDetail.getEndpointUrl());

                            // make dockerconfigjson
                            Map<String, String> secretData = this.makeDockerRegistrySecretData(dockerRegistrySecret);

                            if (MapUtils.isEmpty(secret.getData())) {
                                secret.setData(secretData);
                            } else {
                                secret.getData().putAll(secretData);
                            }
                        } else {
                            throw new CocktailException("If set 'Use External Registry', External Registry Seq required.", ExceptionType.InvalidParameter, ExceptionBiz.SECRET);
                        }
                    } else {
                        secret.setExternalRegistrySeq(null);
                    }
                }
                case Tls -> {
                    if (BooleanUtils.toBoolean(secret.getPublicCertificateYn())) {
                        PublicCertificateDetailVO detail = certManagerService.getCertPublicCertificate(null, secret.getPublicCertificateSeq(), true, true, true);

                        if (detail != null) {
                            Map<String, String> secretData = Maps.newHashMap();
                            if (StringUtils.isNotBlank(detail.getServerAuth())) {
                                secretData.put("ca.crt", detail.getServerAuth());
                            }
                            if (StringUtils.isNotBlank(detail.getClientAuth())) {
                                secretData.put("tls.crt", detail.getClientAuth());
                            }
                            if (StringUtils.isNotBlank(detail.getClientKey())) {
                                secretData.put("tls.key", detail.getClientKey());
                            }

                            if (MapUtils.isEmpty(secret.getData())) {
                                secret.setData(secretData);
                            } else {
                                secret.getData().putAll(secretData);
                            }
                        } else {
                            throw new CocktailException("If set 'Use Public Certificate', Public certtificate Seq required.", ExceptionType.InvalidParameter, ExceptionBiz.SECRET);
                        }
                    } else {
                        secret.setPublicCertificateSeq(null);
                    }
                }
            }
        }
    }

    /**
     * Secret 생성
     *
     * @param servicemapSeq
     * @param secret
     * @param cluster
     * @return
     * @throws Exception
     */
    public SecretGuiVO createSecret(Integer servicemapSeq, SecretGuiVO secret, ClusterVO cluster) throws Exception {
        if(cluster == null){
            cluster = this.setupCluster(servicemapSeq);
        }
        return this.createSecret(cluster.getClusterSeq(), cluster.getNamespaceName(), secret, cluster);
    }

    /**
     * Secret 생성 (Invoke From Snapshot Deployment)
     *
     * @param servicemapSeq
     * @param secrets
     * @return
     * @throws Exception
     */
    public void createMultipleSecret(Integer servicemapSeq, List<SecretIntegrateVO> secrets) throws Exception {
        ClusterVO cluster = this.setupCluster(servicemapSeq);

        if(CollectionUtils.isNotEmpty(secrets)){
            for(SecretIntegrateVO secret : secrets){
                if(DeployType.valueOf(secret.getDeployType()) == DeployType.GUI) {
                    SecretGuiVO secretGui = null;
                    try {
                        secretGui = (SecretGuiVO) secret;
                        this.createSecret(servicemapSeq, secretGui, cluster);
                        Thread.sleep(100);
                    }
                    catch (Exception ex) {
                        // 실패시 Log를 남기고 다음 처리를 계속한다..
                        log.error(String.format("Secret Deployment Failure : createMultipleSecret : %s\n%s", ex.getMessage(), JsonUtils.toGson(secretGui)));
                    }
                }
                else if(DeployType.valueOf(secret.getDeployType()) == DeployType.YAML) {
                    SecretYamlVO secretYaml = null;
                    try {
                        secretYaml = (SecretYamlVO) secret;
                        V1Secret v1Secret = ServerUtils.unmarshalYaml(secretYaml.getYaml(), K8sApiKindType.SECRET);
                        k8sWorker.createSecretV1(cluster, cluster.getNamespaceName(), v1Secret, false);
                        Thread.sleep(100);
                    }
                    catch (Exception ex) {
                        // 실패시 Log를 남기고 다음 처리를 계속한다..
                        log.error(String.format("Secret Deployment Failure : createMultipleSecret : %s\n%s", ex.getMessage(), JsonUtils.toGson(secretYaml)));
                    }
                }
                else {
                    log.error(String.format("Invalid Secret DeployType : createMultipleSecret : %s", JsonUtils.toGson(secret)));
                }
            }
        }
    }

    /**
     * Secret 생성 (Invoke From Snapshot Deployment)
     *
     * @param servicemapSeq
     * @param secrets
     * @return
     * @throws Exception
     */
    public void createSecrets(Integer servicemapSeq, List<SecretGuiVO> secrets) throws Exception {
        ClusterVO cluster = this.setupCluster(servicemapSeq);

        if(CollectionUtils.isNotEmpty(secrets)){
            for(SecretGuiVO secret : secrets){

                this.createSecret(servicemapSeq, secret, cluster);

                Thread.sleep(100);
            }
        }
    }

    /**
     * Secret 생성
     * @param clusterSeq
     * @param namespaceName
     * @param secret
     * @param cluster
     * @return
     * @throws Exception
     */
    public SecretGuiVO createSecret(Integer clusterSeq, String namespaceName, SecretGuiVO secret, ClusterVO cluster) throws Exception {
        if(cluster == null) {
            cluster = this.setupCluster(clusterSeq, namespaceName);
        }

        return this.createSecret(cluster,  cluster.getNamespaceName(), secret);
    }

    /**
     * Secret 생성
     * @param cluster
     * @param namespaceName
     * @param secret
     * @return
     * @throws Exception
     */
    public SecretGuiVO createSecret(ClusterVO cluster, String namespaceName, SecretGuiVO secret) throws Exception {

        V1Secret v1Secret = k8sWorker.getSecretV1(cluster, namespaceName, secret.getName());
        if(v1Secret != null){
            throw new CocktailException("Secret already exists!!", ExceptionType.SecretNameAlreadyExists);
        }

        V1Secret secretParam = K8sSpecFactory.buildSecretV1(secret);

        k8sWorker.createSecretV1(cluster, namespaceName, secretParam, false);

        return secret;
    }

    /**
     * Secret 정보 조회
     *
     * @param servicemapSeq
     * @param secretName
     * @return
     * @throws Exception
     */
    public SecretGuiVO getSecret(Integer servicemapSeq, String secretName) throws Exception {

        ClusterVO cluster = this.setupCluster(servicemapSeq);

        return this.getSecret(cluster, cluster.getNamespaceName(), secretName);
    }

    /**
     * Secret 정보 조회
     *
     * @param clusterSeq
     * @param namespaceName
     * @param secretName
     * @return
     * @throws Exception
     */
    public SecretGuiVO getSecret(Integer clusterSeq, String namespaceName, String secretName) throws Exception {
        IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
        ClusterVO cluster = clusterDao.getCluster(clusterSeq);

        return this.getSecret(cluster, namespaceName, secretName);
    }

    /**
     * Secret 정보 조회
     *
     * @param cluster
     * @param namespaceName
     * @param secretName
     * @return
     * @throws Exception
     */
    public SecretGuiVO getSecret(ClusterVO cluster, String namespaceName, String secretName) throws Exception {
        V1Secret secret;
        try {
            secret = k8sWorker.getSecretV1(cluster, namespaceName, secretName);
        } catch (Exception e) {
            throw new CocktailException("getSecret fail!!", e, ExceptionType.K8sCocktailCloudInquireFail);
        }

        return this.convertSecretData(secret);
    }

    /**
     * Secret 정보 조회
     *
     * @param cluster
     * @param namespaceName
     * @param secretName
     * @return
     * @throws Exception
     */
    public SecretGuiVO getSecret(ClusterVO cluster, String namespaceName, String secretName, boolean includeDataValue) throws Exception {
        V1Secret secret;
        try {
            secret = k8sWorker.getSecretV1(cluster, namespaceName, secretName);
        } catch (Exception e) {
            throw new CocktailException("getSecret fail!!", e, ExceptionType.K8sCocktailCloudInquireFail);
        }

        return this.convertSecretData(secret, includeDataValue);
    }

    /**
     * Secret 정보 목록 조회
     *
     * @param servicemapSeq
     * @return
     * @throws Exception
     */
    public List<SecretGuiVO> getSecrets(Integer servicemapSeq, boolean useDefaultFilter) throws Exception {

        return this.getSecrets(servicemapSeq, null, null, useDefaultFilter);
    }

    /**
     * Secret 정보 목록 조회
     *
     * @param servicemapSeq
     * @return
     * @throws Exception
     */
    public List<SecretGuiVO> getSecrets(Integer servicemapSeq, String field, String label, boolean useDefaultFilter) throws Exception {

        ClusterVO cluster = this.setupCluster(servicemapSeq);

        return this.getSecrets(cluster, cluster.getNamespaceName(), field, label, useDefaultFilter);
    }

    /**
     * Secret 정보 목록 조회
     *
     * @param cluster
     * @param field
     * @param label
     * @return
     * @throws Exception
     */
    public List<SecretGuiVO> getSecrets(ClusterVO cluster, String field, String label, boolean useDefaultFilter) throws Exception {
        return this.getSecrets(cluster, cluster.getNamespaceName(), field, label, useDefaultFilter);
    }

    /**
     * Secret 정보 목록 조회
     *
     * @param clusterId
     * @param namespaceName
     * @param field
     * @param label
     * @param useDefaultFilter
     * @return
     * @throws Exception
     */
    public List<SecretGuiVO> getSecrets(String clusterId, String namespaceName, String field, String label, boolean useDefaultFilter) throws Exception {
        IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
        ClusterVO cluster = clusterDao.getClusterByClusterId(clusterId, "Y");

        return this.getSecrets(cluster, namespaceName, field, label, useDefaultFilter);
    }

    /**
     * Secret 정보 목록 조회
     *
     * @param clusterSeq
     * @param namespaceName
     * @param field
     * @param label
     * @param useDefaultFilter
     * @return
     * @throws Exception
     */
    public List<SecretGuiVO> getSecrets(Integer clusterSeq, String namespaceName, String field, String label, boolean useDefaultFilter) throws Exception {
        IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
        ClusterVO cluster = clusterDao.getCluster(clusterSeq);

        return this.getSecrets(cluster, namespaceName, field, label, useDefaultFilter);
    }

    /**
     * Secret 정보 목록 조회
     *
     * @param cluster
     * @param namespaceName
     * @param field
     * @param label
     * @param useDefaultFilter
     * @return
     * @throws Exception
     */
    public List<SecretGuiVO> getSecrets(ClusterVO cluster, String namespaceName, String field, String label, boolean useDefaultFilter) throws Exception {
        return this.getSecrets(cluster, namespaceName, field, label, useDefaultFilter, false);
    }

    /**
     * Secret 정보 목록 조회
     *
     * @param cluster
     * @param namespaceName
     * @param field
     * @param label
     * @param useDefaultFilter
     * @param includeDataValue
     * @return
     * @throws Exception
     */
    public List<SecretGuiVO> getSecrets(ClusterVO cluster, String namespaceName, String field, String label, boolean useDefaultFilter, boolean includeDataValue) throws Exception {

        List<V1Secret> secrets;
        try {
            secrets = k8sWorker.getSecretsV1(cluster, namespaceName, field, label);
            if(useDefaultFilter){
                secrets = filterDefaultSecrets(cluster, namespaceName, secrets);
            }
        } catch (Exception e) {
            throw new CocktailException("getSecrets fail!!", e, ExceptionType.K8sCocktailCloudInquireFail);
        }

        return this.convertSecretDataList(secrets, includeDataValue);
    }

    /**
     * Secret 정보 목록 조회
     *
     * @param cluster
     * @param namespaceName
     * @param field
     * @param label
     * @param useDefaultFilter
     * @return
     * @throws Exception
     */
    public List<V1Secret> getSecretsV1(ClusterVO cluster, String namespaceName, String field, String label, boolean useDefaultFilter) throws Exception {

        List<V1Secret> secrets;
        try {
            secrets = k8sWorker.getSecretsV1(cluster, namespaceName, field, label);
            if(useDefaultFilter){
                secrets = filterDefaultSecrets(cluster, namespaceName, secrets);
            }
        } catch (Exception e) {
            throw new CocktailException("getSecretsV1 fail!!", e, ExceptionType.K8sCocktailCloudInquireFail);
        }

        return secrets;
    }

    /**
     * Secret 정보 조회
     * @param cluster
     * @param namespace
     * @param name
     * @return
     * @throws Exception
     */
    public V1Secret getSecretV1(ClusterVO cluster, String namespace, String name) throws Exception {

        V1Secret secret;
        try {
            secret = k8sWorker.getSecretV1(cluster, namespace, name);
        } catch (Exception e) {
            throw new CocktailException("getSecretV1 fail!!", e, ExceptionType.K8sCocktailCloudInquireFail);
        }

        return secret;
    }

    /**
     * 입력된 시크릿 리스트에서 k8s default로 생성되는 SA(ServiceAccount)의 secret과
     * cocktail 에서 image pull을 위해 생성한 workspace의 registry user 계정 secret을 제외 처리 한다.
     *
     * @param cluster
     * @param namespaceName
     * @param secrets
     * @return
     */
    public List<V1Secret> filterDefaultSecrets(ClusterVO cluster, String namespaceName, List<V1Secret> secrets) throws Exception {

        IServicemapMapper servicemapDao = sqlSession.getMapper(IServicemapMapper.class);
        IServiceMapper serviceDao = sqlSession.getMapper(IServiceMapper.class);
        IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);

        // 필터링할 이름 저장 리스트
        List<String> filterNames = new ArrayList<>();

        // namespace의 default SA 정보 조회 후 secret이름 추출
        String field = String.format("%s.%s=%s", KubeConstants.META, KubeConstants.NAME, "default");
        List<K8sServiceAccountVO> saList = rbacResourceService.getServiceAccounts(cluster, namespaceName, field, null);
        if(CollectionUtils.isNotEmpty(saList)){
            for(K8sServiceAccountVO sa : saList){
                if (CollectionUtils.isNotEmpty(sa.getSecrets())) {
                    filterNames.addAll(sa.getSecrets());
                }
            }
        }

        // service(workspace)의 image repository 사용자 계정 조회
        ClusterVO clusterByNamespace = clusterDao.getClusterByNamespace(cluster.getClusterSeq(), namespaceName);
        if(clusterByNamespace != null) {
            // account의 service(workspace)의 image repository 전제 사용자 계정 조회하여 필터처리 되도록 수정
            // hjchoi.20200625
            if (clusterByNamespace != null && clusterByNamespace.getAccount() != null) {
                if (StringUtils.isNotBlank(clusterByNamespace.getAccount().getRegistryDownloadUserId())) {
                    filterNames.add(CryptoUtils.decryptAES(clusterByNamespace.getAccount().getRegistryDownloadUserId()));
                }
                List<String> registryUserIds = serviceDao.getRegistryUserIds(); // 전체 플랫폼 워크스페이스... (해당 네밍스페이스가 서비스맵만 해제되었다 다른 플랫폼에 기존 네임스페이스 불러오기로 올라올수도 있다...)
                if (CollectionUtils.isNotEmpty(registryUserIds)) {
                    for (String registryUserId : registryUserIds) {
                        filterNames.add(CryptoUtils.decryptAES(registryUserId));
                    }
                }
            }
        }

//        // secret 리스트에서 위 두가지 해당하는 secret 제거
//        if(CollectionUtils.isNotEmpty(secrets) && CollectionUtils.isNotEmpty(filterNames)){
//            secrets = secrets.stream().filter(secret -> !filterNames.contains(secret.getMetadata().getName())).collect(Collectors.toList());
//        }
//
//        /**
//         * 관리용 Secret 제외 :2020.02.18 by Redion.
//         * 1. 칵테일 Package 매니저에서 관리용도로 사용하는 secret : cocktail.io/key-and-cert
//         * 2. Helm Chart가 Revision 관리를 위해 사용하는 secret : helm.sh/release.v1
//         * 3. istio addon이 관리를 위해 사용하는 secret : istio.io/key-and-cert
//         *
//         */
//        List<String> filter = new ArrayList<>();
//        filter.add("cocktail.io/key-and-cert");
//        filter.add("helm.sh/release.v1");
//        filter.add("istio.io/key-and-cert");
//        secrets = secrets.stream().filter(secret -> !filter.contains(secret.getType())).collect(Collectors.toList());

        // secret 리스트에서 위 두가지 해당하는 secret 제거
        if (CollectionUtils.isNotEmpty(secrets)) {
            /**
             * 관리용 Secret 제외 :2020.02.18 by Redion.
             * 1. 칵테일 Package 매니저에서 관리용도로 사용하는 secret : cocktail.io/key-and-cert
             * 2. Helm Chart가 Revision 관리를 위해 사용하는 secret : helm.sh/release.v1
             * 3. istio addon이 관리를 위해 사용하는 secret : istio.io/key-and-cert
             *
             * * TODO : 일단 아닌 것 같은건 빼도록 하고, 명확한 정책 정해지면 리팩토링 진행하자..
             */
            List<String> filter = new ArrayList<>();
            filter.add("cocktail.io/key-and-cert");
            filter.add("helm.sh/release.v1");
            filter.add("istio.io/key-and-cert");

            secrets.removeIf(secret -> (
                    (CollectionUtils.isNotEmpty(filterNames) && filterNames.contains(secret.getMetadata().getName()))
                        || filter.contains(secret.getType())
                    ));
        }

        return secrets;
    }

    /**
     * K8S Secret 정보 조회 후 List<V1Secret> -> List<SecretGuiVO> 변환
     *
     * @param secrets
     * @return
     */
    private List<SecretGuiVO> convertSecretDataList(List<V1Secret> secrets) throws Exception {
        return this.convertSecretDataList(secrets, false);
    }

    private List<SecretGuiVO> convertSecretDataList(List<V1Secret> secrets, boolean includeDataValue) throws Exception {
        List<SecretGuiVO> secretGuiVOS = new ArrayList<>();
        if(CollectionUtils.isNotEmpty(secrets)){
            for(V1Secret v1SecretRow : secrets){
                /** R3.5 : Secret Data List 구성시 Cocktail에서 Support하는 Type에 대해서만 조회하도록 함
                 * TODO : 향 후 전체 Secret 조회가 필요할 경우 기획안을 정해서 손 봐야 함.. */
                Set<String> supportedSecretType = SecretType.getSupportedSecretTypesValue();
                if (v1SecretRow.getMetadata().getLabels() != null && v1SecretRow.getMetadata().getLabels().containsKey(KubeConstants.LABELS_SECRET)) {
                    if(supportedSecretType.contains(v1SecretRow.getType())) {
                        secretGuiVOS.add(this.convertSecretData(v1SecretRow, includeDataValue));
                    }
                }
                else if(supportedSecretType.contains(v1SecretRow.getType())) {
                    secretGuiVOS.add(this.convertSecretData(v1SecretRow, includeDataValue));
                }
            }
        }

        return secretGuiVOS;
    }

    /**
     * K8S Secret 정보 조회 후 V1Secret -> SecretGuiVO 변환
     *
     * @param secret
     * @return
     */
    public SecretGuiVO convertSecretData(V1Secret secret) throws Exception{
        return this.convertSecretData(secret, null, false);
    }
    public SecretGuiVO convertSecretData(V1Secret secret, boolean includeDataValue) throws Exception{
        return this.convertSecretData(secret, null, includeDataValue);
    }
    public SecretGuiVO convertSecretData(V1Secret secret, SecretYamlVO secretYaml, boolean includeDataValue) throws Exception{
        SecretGuiVO sv = new SecretGuiVO();

        if(secret != null){
            if(secretYaml != null) {
                if (!StringUtils.equals(secretYaml.getName(), secret.getMetadata().getName())) {
                    // Yaml에 입력된 Name과 path에 입력한 Name 정보가 틀리면 오류..
                    throw new CocktailException("Secret name is different.2", ExceptionType.K8sSecretNameInvalid);
                }
            }
            if (secret.getMetadata().getLabels() != null && secret.getMetadata().getLabels().containsKey(KubeConstants.LABELS_SECRET)) {
                sv.setType(SecretType.codeOf(secret.getMetadata().getLabels().get(KubeConstants.LABELS_SECRET)));
            } else {
                // Type이 "Opaque" 일 때만 Generic 으로 처리.
                if(StringUtils.equalsIgnoreCase(secret.getType(), SecretType.Generic.getValue())) {
                    sv.setType(SecretType.Generic);
                }
                else {
                    Optional<SecretType> secretType = SecretType.secretTypeOf(secret.getType());
                    if(secretType.isPresent()) {
                        sv.setType(secretType.get());
                    }
                    else {
                        sv.setType(SecretType.Generic);
                        log.error(String.format("Invalid Secret Type : [namespace : %s / secret name : %s / secret type : %s]",
                                secret.getMetadata().getNamespace(), secret.getMetadata().getName(), secret.getType()));
                    }
                }
            }
            sv.setName(secret.getMetadata().getName());
            sv.setNamespace(secret.getMetadata().getNamespace());
            sv.setCreationTimestamp(secret.getMetadata().getCreationTimestamp());
            // description
            sv.setDescription(ResourceUtil.decodeDescription(Optional.ofNullable(secret.getMetadata()).map(V1ObjectMeta::getAnnotations).orElseGet(() ->Maps.newHashMap())));

            Map<String, String> dataMap = new HashMap<>();
            if(MapUtils.isNotEmpty(secret.getData())){
                for(Map.Entry<String, byte[]> dataEntry : secret.getData().entrySet()){
                    if(includeDataValue) {
                        dataMap.put(dataEntry.getKey(), new String(Base64.getEncoder().encode(dataEntry.getValue())));
                    }
                    else {
                        dataMap.put(dataEntry.getKey(), "");
                    }
                }
            }

            sv.setData(dataMap);
            /** set labels **/
            if(Optional.ofNullable(secret).map(V1Secret::getMetadata).map(V1ObjectMeta::getLabels).orElseGet(() ->null) != null) {
                sv.setLabels(secret.getMetadata().getLabels());
            }
            /** set Annotations **/
            if(secret.getMetadata() != null && secret.getMetadata().getAnnotations() != null) {
                sv.setAnnotations(secret.getMetadata().getAnnotations());
            }

            JSON k8sJson = new JSON();
            sv.setDeployment(k8sJson.serialize(secret));
            sv.setDeploymentYaml(Yaml.getSnakeYaml().dumpAsMap(secret));
        }else {
            return null;
        }

        return sv;
    }

    /**
     * Convert Yaml to Secret Object
     * @param servicemapSeq
     * @param secretYaml
     * @return
     * @throws Exception
     */
    public SecretGuiVO convertYamlToSecret(Integer servicemapSeq, SecretYamlVO secretYaml) throws Exception {
        return convertYamlToSecret(servicemapSeq, null, secretYaml);
    }

    /**
     * Convert Yaml to Secret Object
     * @param servicemapSeq
     * @param cluster
     * @param secretYaml
     * @return
     * @throws Exception
     */
    public SecretGuiVO convertYamlToSecret(Integer servicemapSeq, ClusterVO cluster, SecretYamlVO secretYaml) throws Exception {
        return this.convertYamlToSecret(servicemapSeq, cluster, secretYaml, false);
    }

    /**
     * Convert Yaml to Secret Object
     * @param servicemapSeq
     * @param cluster
     * @param secretYaml
     * @return
     * @throws Exception
     */
    public SecretGuiVO convertYamlToSecret(Integer servicemapSeq, ClusterVO cluster, SecretYamlVO secretYaml, boolean checkOnlyYaml) throws Exception {
        if(cluster == null || StringUtils.isBlank(cluster.getNamespaceName())) {
            if(servicemapSeq != null) {
                cluster = this.setupCluster(servicemapSeq);
            }
            else {
                if(!checkOnlyYaml) {
                    throw new CocktailException("convertYamlToSecret fail!! (there is no key to check)", ExceptionType.InvalidParameter_Empty);
                }
            }
        }
        // Valid check 하기 위행 GUI로 변환
        V1Secret v1Secret = ServerUtils.unmarshalYaml(secretYaml.getYaml());
        if(v1Secret == null) {
            throw new CocktailException("Can not found Secret spec (Invalid YAML)", ExceptionType.K8sSecretNotFound);
        }
        SecretGuiVO secret = this.convertSecretData(v1Secret, secretYaml, true);

        return secret;
    }
    /**
     * Patch Secret With Yaml
     * @param servicemapSeq
     * @param secretGui
     * @param secretYaml
     * @return
     * @throws Exception
     */
    public SecretGuiVO patchSecretWithYaml(Integer servicemapSeq, SecretGuiVO secretGui, SecretYamlVO secretYaml) throws Exception{
        return patchSecretWithYaml(servicemapSeq, null, secretGui, secretYaml);
    }

    /**
     * Patch Secret With Yaml
     * @param servicemapSeq
     * @param cluster
     * @param secretGui
     * @param secretYaml
     * @return
     * @throws Exception
     */
    public SecretGuiVO patchSecretWithYaml(Integer servicemapSeq, ClusterVO cluster, SecretGuiVO secretGui, SecretYamlVO secretYaml) throws Exception{
        try {
            if(cluster == null || StringUtils.isBlank(cluster.getNamespaceName())) {
                if(servicemapSeq != null) {
                    cluster = this.setupCluster(servicemapSeq);
                }
                else {
                    throw new CocktailException("convertYamlToSecret fail!! (there is no key to check)", ExceptionType.InvalidParameter_Empty);
                }
            }

            if(!cluster.getNamespaceName().equals(secretGui.getNamespace())) {
                throw new CocktailException("Can't change the namespace. (namespace is different)", ExceptionType.NamespaceNameInvalid);
            }
            if(!secretYaml.getName().equals(secretGui.getName())) { // Controller에서 URI path Param과 secretYaml Param이 다른지는 이미 비교함..
                throw new CocktailException("Can't change the name. (name is different)", ExceptionType.K8sSecretNameInvalid);
            }

            if (!this.isUsedSecret(servicemapSeq, cluster, secretGui, false, true, true)) {
                throw new CocktailException("Secret is used.", ExceptionType.SecretUsed);
            }

            Map<String, Object> secretObjMap = ServerUtils.getK8sYamlToMap(secretYaml.getYaml());

            log.debug(JsonUtils.toPrettyString(secretObjMap));
            K8sApiKindType apiKindType = ServerUtils.getK8sKindInMap(secretObjMap);
            K8sApiGroupType apiGroupType = ServerUtils.getK8sGroupInMap(secretObjMap);
            K8sApiType apiVerType = ServerUtils.getK8sVersionInMap(secretObjMap);

            if (apiKindType == K8sApiKindType.SECRET) {
                if (apiVerType == K8sApiType.V1) {
                    V1Secret updatedSecret = Yaml.loadAs(secretYaml.getYaml(), V1Secret.class);
                    return this.patchSecretV1WithYaml(cluster, cluster.getNamespaceName(), updatedSecret, ContextHolder.exeContext());
                }
            }
        } catch (Exception e) {
            boolean isSpecial = false;
            if(e instanceof CocktailException){
                if(((CocktailException) e).getType() == ExceptionType.SecretUsed){
                    isSpecial = true;
                }
            }
            if (isSpecial){
                throw e;
            }else {
                if(log.isDebugEnabled()) log.debug("trace log ", e);
                throw new CocktailException("patchSecret fail!!", e, ExceptionType.K8sCocktailCloudUpdateFail);
            }
        }

        return null;
    }

    public SecretGuiVO patchSecretV1WithYaml(ClusterVO cluster, String namespace, V1Secret updatedSecret, ExecutingContextVO context) throws Exception {
//        Jackson Json value에 byte[] 값이 있을 경우 VALUE_EMBEDDED_OBJECT Token유형으로 저장되는데 해당 Token 유형을 JsonDiff에서 지원하지 않아 Diff 불가..
//        따라서 Secret Yaml 배포는 JsonDiff로 변경점을 확인하지 않고 입력받은 내용을 그대로 Replace로 배포..

//        // 현재 Secret 조회
//        V1Secret currentSecret = k8sWorker.getSecretV1(cluster, cluster.getNamespaceName(), updatedSecret.getMetadata().getName());
//        // patchJson 으로 변경
//        List<JsonObject> patchBody = k8sPatchSpecFactory.buildPatch(currentSecret, updatedSecret);
//        log.debug("########## Secret patchBody JSON: {}", JsonUtils.toGson(patchBody));
//
//        // patch
//        V1Secret secret = k8sWorker.patchSecretV1(cluster, namespace, updatedSecret.getMetadata().getName(), patchBody);

        // replace
        V1Secret secret = k8sWorker.replaceSecretV1(cluster, namespace, updatedSecret.getMetadata().getName(), updatedSecret, false);
        Thread.sleep(100);

        return this.convertSecretData(secret);
    }

    public Object patchSecretV1WithYaml(ClusterVO cluster, String namespace, String name, V1Secret updatedSecret, ExecutingContextVO context) throws Exception {
//        Jackson Json value에 byte[] 값이 있을 경우 VALUE_EMBEDDED_OBJECT Token유형으로 저장되는데 해당 Token 유형을 JsonDiff에서 지원하지 않아 Diff 불가..
//        따라서 Secret Yaml 배포는 JsonDiff로 변경점을 확인하지 않고 입력받은 내용을 그대로 Replace로 배포..

//        // 현재 Secret 조회
//        V1Secret currentSecret = k8sWorker.getSecretV1(cluster, cluster.getNamespaceName(), updatedSecret.getMetadata().getName());
//        // patchJson 으로 변경
//        List<JsonObject> patchBody = k8sPatchSpecFactory.buildPatch(currentSecret, updatedSecret);
//        log.debug("########## Secret patchBody JSON: {}", JsonUtils.toGson(patchBody));
//
//        // patch
//        V1Secret secret = k8sWorker.patchSecretV1(cluster, namespace, updatedSecret.getMetadata().getName(), patchBody);

        // replace
        return k8sWorker.replaceSecretV1(cluster, namespace, name, updatedSecret, false);
    }

    /**
     * Patch Secret
     *
     * @param servicemapSeq
     * @param secretParam
     * @return
     * @throws Exception
     */
    public SecretGuiVO patchSecret(Integer servicemapSeq, SecretGuiVO secretParam) throws Exception{
        return patchSecret(servicemapSeq, null, secretParam);
    }

    /**
     * Patch Secret
     * @param servicemapSeq
     * @param cluster
     * @param secretParam
     * @return
     * @throws Exception
     */
    public SecretGuiVO patchSecret(Integer servicemapSeq, ClusterVO cluster, SecretGuiVO secretParam) throws Exception{
        SecretGuiVO secretGuiVO;

        if (secretParam != null) {
            if(cluster == null || (cluster != null && StringUtils.isBlank(cluster.getNamespaceName()))) {
                if(servicemapSeq != null) {
                    cluster = this.setupCluster(servicemapSeq);
                }
                else {
                    throw new CocktailException("patchConfigMap fail!! (there is no key to check)", ExceptionType.InvalidParameter_Empty);
                }
            }

            // KISA 보안 null 체크추가
            if (cluster == null){
                throw new CocktailException("cluster is null.", ExceptionType.InvalidParameter_Empty);
            }

            if(!this.isUsedSecret(servicemapSeq, cluster, secretParam, false, true, true)){
                throw new CocktailException("Secret is used.", ExceptionType.SecretUsed);
            }

            /** R3.5 : 2019.10.18 : 기존 Secret을 조회하여 Reserved Label, Annotation을 유지할 수 있도록 처리 : buildPatchSecretV1().makePatchMap() **/
            V1Secret asisSecret = k8sWorker.getSecretV1(cluster, cluster.getNamespaceName(), secretParam.getName()); // Get
            List<JsonObject> patchBody = k8sPatchSpecFactory.buildPatchSecretV1(secretParam, asisSecret); // Set
            V1Secret secret = k8sWorker.patchSecretV1(cluster, cluster.getNamespaceName(), secretParam.getName(), patchBody, false); // Go

            secretGuiVO = this.convertSecretData(secret);
        } else {
            throw new CocktailException("patchConfigMap fail!! (there is no request info to check)", ExceptionType.InvalidParameter_Empty);
        }

        return secretGuiVO;
    }

    public SecretGuiVO patchSecret(ClusterVO cluster, String namespace, SecretGuiVO secretParam) throws Exception{
        SecretGuiVO secretGuiVO;

        if (secretParam != null) {

            // KISA 보안 null 체크추가
            if (cluster == null){
                throw new CocktailException("cluster is null.", ExceptionType.InvalidParameter_Empty);
            }

            /** R3.5 : 2019.10.18 : 기존 Secret을 조회하여 Reserved Label, Annotation을 유지할 수 있도록 처리 : buildPatchSecretV1().makePatchMap() **/
            V1Secret asisSecret = k8sWorker.getSecretV1(cluster, namespace, secretParam.getName()); // Get
            List<JsonObject> patchBody = k8sPatchSpecFactory.buildPatchSecretV1(secretParam, asisSecret); // Set
            V1Secret secret = k8sWorker.patchSecretV1(cluster, namespace, secretParam.getName(), patchBody, false); // Go

            secretGuiVO = this.convertSecretData(secret);
        } else {
            throw new CocktailException("patchConfigMap fail!! (there is no request info to check)", ExceptionType.InvalidParameter_Empty);
        }

        return secretGuiVO;
    }

    public void deleteSecret(Integer servicemapSeq, String secretName) throws Exception {
        ClusterVO cluster = this.setupCluster(servicemapSeq);
        this.deleteSecret(cluster, secretName);
    }

    public void deleteSecret(Integer clusterSeq, String namespaceName, String secretName) throws Exception {
        ClusterVO cluster = this.setupCluster(clusterSeq, namespaceName);
        this.deleteSecret(cluster, secretName);
    }

    public void deleteSecret(ClusterVO cluster, String secretName) throws Exception {
        this.deleteSecret(cluster, cluster.getNamespaceName(), secretName, true);
    }

    /**
     * Secret 삭제
     *
     * @param cluster
     * @param namespaceName
     * @param secretName
     * @param checkUsed
     * @throws Exception
     */
    public void deleteSecret(ClusterVO cluster, String namespaceName, String secretName, boolean checkUsed) throws Exception {
        V1Secret secret = k8sWorker.getSecretV1(cluster, namespaceName, secretName);
        SecretGuiVO secretGuiVO = this.convertSecretData(secret);

        if(checkUsed && !this.isUsedSecret(cluster, namespaceName, secretGuiVO, true, true, false)){
            throw new CocktailException("Secret is used.", ExceptionType.SecretUsed);
        }

        k8sWorker.deleteSecretV1(cluster, namespaceName, secretName);

    }

    /**
     * Secret 사용유무 체크
     *
     * @param servicemapSeq
     * @param secretParam
     * @param checkVolume - Volume에서 Secret 사용유무
     * @param checkEnv - 환경변수에서 Secret 사용유무
     * @param checkEnvKey - 환경변수에서 Secret Key 사용유무
     * @return
     * @throws Exception
     */
    public boolean isUsedSecret(Integer servicemapSeq, SecretGuiVO secretParam, boolean checkVolume, boolean checkEnv, boolean checkEnvKey) throws Exception {
        ClusterVO cluster = this.setupCluster(servicemapSeq);
        return isUsedSecret(servicemapSeq, cluster, secretParam, checkVolume, checkEnv, checkEnvKey);
    }

    /**
     * Secret 사용유무 체크
     *
     * @param clusterSeq
     * @param namespaceName
     * @param secretParam
     * @param checkVolume
     * @param checkEnv
     * @param checkEnvKey
     * @return
     * @throws Exception
     */
    public boolean isUsedSecret(Integer clusterSeq, String namespaceName, SecretGuiVO secretParam, boolean checkVolume, boolean checkEnv, boolean checkEnvKey) throws Exception {
        ClusterVO cluster = this.setupCluster(clusterSeq, namespaceName);
        return isUsedSecret(null, cluster, secretParam, checkVolume, checkEnv, checkEnvKey);
    }

    /**
     * Secret 사용유무 체크
     * @param servicemapSeq
     * @param cluster
     * @param secretParam
     * @param checkVolume - Volume에서 Secret 사용유무
     * @param checkEnv - 환경변수에서 Secret 사용유무
     * @param checkEnvKey - 환경변수에서 Secret Key 사용유무
     * @return
     * @throws Exception
     */
    public boolean isUsedSecret(Integer servicemapSeq, ClusterVO cluster, SecretGuiVO secretParam, boolean checkVolume, boolean checkEnv, boolean checkEnvKey) throws Exception{
        boolean isNotUsed = true;
        try {
            if(cluster == null || (cluster != null && StringUtils.isBlank(cluster.getNamespaceName()))) {
                if(servicemapSeq != null) {
                    cluster = this.setupCluster(servicemapSeq);
                }
                else {
                    throw new CocktailException("isUsedConfigMap fail!! (there is no key to check)", ExceptionType.InvalidParameter_Empty);
                }
            }
            V1Secret secret = k8sWorker.getSecretV1(cluster, cluster.getNamespaceName(), secretParam.getName());

            if(secret != null){
                Map<String, JsonPatchOp> patchOp = new HashMap<>();
                Map<String, JsonPatchOp> labelPatchOp = new HashMap<>();
                Map<String, JsonPatchOp> annotationPatchOp = new HashMap<>();
                Map<String, JsonPatchOp> patchDescOp = new HashMap<>();
                Map<String, String> removeKeyMap = new HashMap<>();

                /**
                 * 20220510, hjchoi
                 * - value가 빈값이 일시 추가를 안하던것을 가능하도록 빈값체크 제거
                 */
                if(MapUtils.isNotEmpty(secret.getData()) && MapUtils.isNotEmpty(secretParam.getData())){
                    for(Map.Entry<String, String> dataEntry : secretParam.getData().entrySet()){
                        if(secret.getData().containsKey(dataEntry.getKey())){
                            if(StringUtils.isNotBlank(dataEntry.getValue())){
                                patchOp.put(dataEntry.getKey(), JsonPatchOp.REPLACE);
                            }
                        }else{
                            patchOp.put(dataEntry.getKey(), JsonPatchOp.ADD);
                        }
                    }

                    for(Map.Entry<String, byte[]> dataEntry : secret.getData().entrySet()){
                        if(!secretParam.getData().containsKey(dataEntry.getKey())){
                            patchOp.put(dataEntry.getKey(), JsonPatchOp.REMOVE);
                            removeKeyMap.put(dataEntry.getKey(), JsonPatchOp.REMOVE.getValue());
                        }
                    }
                }

                // 키 삭제시 서버에서 사용중인지 체크
                if(checkEnvKey){
                    if(MapUtils.isNotEmpty(removeKeyMap)){
                        SecretGuiVO secretRemove = new SecretGuiVO();
                        BeanUtils.copyProperties(secretRemove, secretParam);
                        secretRemove.setData(removeKeyMap);

                        // check DB
//                        if(serviceSeq != null && appmapSeq != null) {
//                            isNotUsed = this.isUsedSecret(serviceSeq, appmapSeq, secretRemove, checkVolume, checkEnv, checkEnvKey, null);
//                        }
                        // check k8s
                        isNotUsed = this.isUsedSecret(cluster, cluster.getNamespaceName(), secretRemove, checkVolume, checkEnv, checkEnvKey);
                    }
                }else{
                    // check DB
//                    if(serviceSeq != null && appmapSeq != null) {
//                        isNotUsed = this.isUsedSecret(serviceSeq, appmapSeq, secretParam, checkVolume, checkEnv, checkEnvKey, null);
//                    }
                    // check k8s
                    isNotUsed = this.isUsedSecret(cluster, cluster.getNamespaceName(), secretParam, checkVolume, checkEnv, checkEnvKey);
                }
                secretParam.setPatchOp(patchOp);

                /** R3.5 : 2019.10.18 : Add Check Labels **/
                labelPatchOp = k8sPatchSpecFactory.makePatchOp(secret.getMetadata().getLabels(), secretParam.getLabels());
                secretParam.setLabelPatchOp(labelPatchOp);

                /** R3.5 : 2019.10.18 : Add Check Annotations **/
                annotationPatchOp = k8sPatchSpecFactory.makePatchOp(secret.getMetadata().getAnnotations(), secretParam.getAnnotations());
                secretParam.setAnnotationPatchOp(annotationPatchOp);

                // description // R3.5 : 2019.10.18 : 중복 소스 리팩토링.
                patchDescOp = k8sPatchSpecFactory.makePatchOpForDescription(secret.getMetadata().getAnnotations(), secretParam.getDescription());
                secretParam.setPatchDescOp(patchDescOp);

            }else{
                throw new CocktailException(String.format("Secret not found: %s", secretParam.getName()), ExceptionType.K8sSecretNotFound);
            }
        } catch (Exception e) {
            boolean isSpecial = false;
            if(e instanceof CocktailException){
                if(((CocktailException) e).getType() == ExceptionType.K8sSecretNotFound){
                    isSpecial = true;
                }
            }

            if (isSpecial){
                throw e;
            }else {
                if(log.isDebugEnabled()) log.debug("trace log ", e);
                throw new CocktailException("isUsedSecret fail!!", e, ExceptionType.K8sCocktailCloudInquireFail);
            }
        }

        return isNotUsed;
    }

    /**
     * Secret 사용유무 체크
     *
     * @param cluster
     * @param namespace
     * @param secret
     * @param checkVolume - Volume에서 Secret 사용유무
     * @param checkEnv - 환경변수에서 Secret 사용유무
     * @param checkEnvKey - 환경변수에서 Secret Key 사용유무
     * @return
     * @throws Exception
     */
    public boolean isUsedSecret(ClusterVO cluster, String namespace, SecretGuiVO secret, boolean checkVolume, boolean checkEnv, boolean checkEnvKey) throws Exception{
        boolean isNotUsed = true;

        if (secret == null) {
            return false;
        }

        List<V1Deployment> deployments = k8sWorker.getDeploymentsV1(cluster, namespace, null, null);
        for (V1Deployment deploymentRow : Optional.ofNullable(deployments).orElseGet(() ->Lists.newArrayList())) {
            V1PodTemplateSpec v1PodTemplateSpec =
                    Optional.ofNullable(deploymentRow).map(V1Deployment::getSpec).map(V1DeploymentSpec::getTemplate).orElseGet(() ->null);
            if (checkVolume && !this.isUsedSecretOfVolume(v1PodTemplateSpec, secret)) { // volume 체크
                isNotUsed = false; break;
            }
            if (checkEnv && !this.isUsedSecretOfContainer(v1PodTemplateSpec, secret, checkEnvKey)) { // env 체크 Container && initContainer
                isNotUsed = false; break;
            }
        }
        if(!isNotUsed) return isNotUsed;

        List<V1StatefulSet> statefulSets = k8sWorker.getStatefulSetsV1(cluster, namespace, null, null);
        for (V1StatefulSet statefulSetRow : Optional.ofNullable(statefulSets).orElseGet(() ->Lists.newArrayList())) {
            V1PodTemplateSpec v1PodTemplateSpec =
                    Optional.ofNullable(statefulSetRow).map(V1StatefulSet::getSpec).map(V1StatefulSetSpec::getTemplate).orElseGet(() ->null);
            if (checkVolume && !this.isUsedSecretOfVolume(v1PodTemplateSpec, secret)) { // volume 체크
                isNotUsed = false; break;
            }
            if (checkEnv && !this.isUsedSecretOfContainer(v1PodTemplateSpec, secret, checkEnvKey)) { // env 체크 Container && initContainer
                isNotUsed = false; break;
            }
        }
        if(!isNotUsed) return isNotUsed;

        List<V1DaemonSet> daemonSets = k8sWorker.getDaemonSetsV1(cluster, namespace, null, null);
        for (V1DaemonSet daemonSetRow : Optional.ofNullable(daemonSets).orElseGet(() ->Lists.newArrayList())) {
            V1PodTemplateSpec v1PodTemplateSpec =
                    Optional.ofNullable(daemonSetRow).map(V1DaemonSet::getSpec).map(V1DaemonSetSpec::getTemplate).orElseGet(() ->null);
            if (checkVolume && !this.isUsedSecretOfVolume(v1PodTemplateSpec, secret)) { // volume 체크
                isNotUsed = false; break;
            }
            if (checkEnv && !this.isUsedSecretOfContainer(v1PodTemplateSpec, secret, checkEnvKey)) { // env 체크 Container && initContainer
                isNotUsed = false; break;
            }
        }
        if(!isNotUsed) return isNotUsed;

        List<V1Job> jobs = k8sWorker.getJobsV1(cluster, namespace, null, null);
        for (V1Job jobRow : Optional.ofNullable(jobs).orElseGet(() ->Lists.newArrayList())) {
            V1PodTemplateSpec v1PodTemplateSpec =
                    Optional.ofNullable(jobRow).map(V1Job::getSpec).map(V1JobSpec::getTemplate).orElseGet(() ->null);
            if (checkVolume && !this.isUsedSecretOfVolume(v1PodTemplateSpec, secret)) { // volume 체크
                isNotUsed = false; break;
            }
            if (checkEnv && !this.isUsedSecretOfContainer(v1PodTemplateSpec, secret, checkEnvKey)) { // env 체크 Container && initContainer
                isNotUsed = false; break;
            }
        }
        if(!isNotUsed) return isNotUsed;

        List<V1beta1CronJob> cronJobs = k8sWorker.getCronJobsV1beta1(cluster, namespace, null, null);
        for (V1beta1CronJob cronJobRow : Optional.ofNullable(cronJobs).orElseGet(() ->Lists.newArrayList())) {
            V1PodTemplateSpec v1PodTemplateSpec =
                    Optional.ofNullable(cronJobRow).map(V1beta1CronJob::getSpec)
                            .map(V1beta1CronJobSpec::getJobTemplate)
                            .map(V1beta1JobTemplateSpec::getSpec)
                            .map(V1JobSpec::getTemplate)
                            .orElseGet(() ->null);
            if (checkVolume && !this.isUsedSecretOfVolume(v1PodTemplateSpec, secret)) { // volume 체크
                isNotUsed = false; break;
            }
            if (checkEnv && !this.isUsedSecretOfContainer(v1PodTemplateSpec, secret, checkEnvKey)) { // env 체크 Container && initContainer
                isNotUsed = false; break;
            }
        }

        return isNotUsed;
    }

/* 2019.11.26 : 전체 워크로드에서 체크가 필요 & Kubernetes 1.14 Spec 기준으로 작성을 위해 주석 처리
    public boolean isUsedSecret(ClusterVO cluster, String namespace, SecretGuiVO secret, boolean checkVolume, boolean checkEnv, boolean checkEnvKey) throws Exception{
        boolean isNotUsed = true;

        if(secret != null){
            K8sApiType apiType = k8sWorker.getApiType(cluster, K8sApiKindType.DEPLOYMENT);

            if(K8sApiType.V1BETA1 == apiType) {
                // Deployment 조회
                List<AppsV1beta1Deployment> v1beta1Deployments = k8sWorker.getDeploymentsV1beta1(cluster, namespace, null, null);

                if(CollectionUtils.isNotEmpty(v1beta1Deployments)){
                    for(AppsV1beta1Deployment deploymentRow : v1beta1Deployments){
                        if (checkVolume) {
                            // volume 체크
                            if (CollectionUtils.isNotEmpty(deploymentRow.getSpec().getTemplate().getSpec().getVolumes())) {
                                if (!this.isUsedSecretOfVolume(deploymentRow.getSpec().getTemplate().getSpec().getVolumes(), secret)) {
                                    isNotUsed = false;
                                    break;
                                }
                            }
                        }

                        if (checkEnv){
                            // env 체크
                            if(CollectionUtils.isNotEmpty(deploymentRow.getSpec().getTemplate().getSpec().getContainers())){
                                if(!this.isUsedSecretOfContainer(deploymentRow.getSpec().getTemplate().getSpec().getContainers(), secret, checkEnvKey)){
                                    isNotUsed = false;
                                    break;
                                }
                            }
                        }

                    }
                }
            }else if(K8sApiType.V1BETA2 == apiType) {
                // Deployment 조회
                List<V1beta2Deployment> v1beta2Deployments = k8sWorker.getDeploymentsV1beta2(cluster, namespace, null, null);

                if(CollectionUtils.isNotEmpty(v1beta2Deployments)){
                    for(V1beta2Deployment deploymentRow : v1beta2Deployments){
                        if (checkVolume) {
                            // volume 체크
                            if (CollectionUtils.isNotEmpty(deploymentRow.getSpec().getTemplate().getSpec().getVolumes())) {
                                if (!this.isUsedSecretOfVolume(deploymentRow.getSpec().getTemplate().getSpec().getVolumes(), secret)) {
                                    isNotUsed = false;
                                    break;
                                }
                            }
                        }

                        if (checkEnv){
                            // env 체크
                            if(CollectionUtils.isNotEmpty(deploymentRow.getSpec().getTemplate().getSpec().getContainers())){
                                if(!this.isUsedSecretOfContainer(deploymentRow.getSpec().getTemplate().getSpec().getContainers(), secret, checkEnvKey)){
                                    isNotUsed = false;
                                    break;
                                }
                            }
                        }

                    }
                }
            }else if(K8sApiType.V1 == apiType) {
                // Deployment 조회
                List<V1Deployment> v1Deployments = k8sWorker.getDeploymentsV1(cluster, namespace, null, null);

                if(CollectionUtils.isNotEmpty(v1Deployments)){
                    for(V1Deployment deploymentRow : v1Deployments){
                        if (checkVolume) {
                            // volume 체크
                            if (CollectionUtils.isNotEmpty(deploymentRow.getSpec().getTemplate().getSpec().getVolumes())) {
                                if (!this.isUsedSecretOfVolume(deploymentRow.getSpec().getTemplate().getSpec().getVolumes(), secret)) {
                                    isNotUsed = false;
                                    break;
                                }
                            }
                        }

                        if (checkEnv){
                            // env 체크
                            if(CollectionUtils.isNotEmpty(deploymentRow.getSpec().getTemplate().getSpec().getContainers())){
                                if(!this.isUsedSecretOfContainer(deploymentRow.getSpec().getTemplate().getSpec().getContainers(), secret, checkEnvKey)){
                                    isNotUsed = false;
                                    break;
                                }
                            }
                        }

                    }
                }
            }
        }else{
            isNotUsed = false;
        }


        return isNotUsed;
    }
*/

    /**
     * 볼륨에서 secret 사용 유무 (false가 사용중이다. 헷갈리지 말자;; )
     * @param v1PodTemplateSpec
     * @param secret
     * @return
     * @throws Exception
     */
    private boolean isUsedSecretOfVolume(V1PodTemplateSpec v1PodTemplateSpec, SecretGuiVO secret) throws Exception{
        if(v1PodTemplateSpec == null || secret == null) return true;

        List<V1Volume> v1Volumes =
                Optional.of(v1PodTemplateSpec).map(V1PodTemplateSpec::getSpec).map(V1PodSpec::getVolumes).orElseGet(() ->Lists.newArrayList());

        for (V1Volume volumeRow : v1Volumes) {
            if (volumeRow.getSecret() != null && StringUtils.equals(volumeRow.getSecret().getSecretName(), secret.getName())) {
                return false;
            }
        }

        return true;
    }

    /**
     * 볼륨에서 secret 사용 유무 (false가 사용중이다. 헷갈리지 말자;; )
     * @param v1Volumes
     * @param secret
     * @return
     * @throws Exception
     */
    private boolean isUsedSecretOfVolume(List<V1Volume> v1Volumes, SecretGuiVO secret) throws Exception{
        for(V1Volume volumeRow : v1Volumes){
            if(volumeRow.getSecret() != null && StringUtils.equals(volumeRow.getSecret().getSecretName(), secret.getName())){
                return false;
            }
        }

        return true;
    }

    /**
     * 환경변수 중 해당 secret key 사용 유무 (false가 사용중이다 헷갈리지 말자;; )
     * @param v1PodTemplateSpec
     * @param secret
     * @param checkEnvKey
     * @return
     * @throws Exception
     */
    private boolean isUsedSecretOfContainer(V1PodTemplateSpec v1PodTemplateSpec, SecretGuiVO secret, boolean checkEnvKey) throws Exception{
        if(v1PodTemplateSpec == null || secret == null) return true;

        List<V1Container> v1Containers = Optional.of(v1PodTemplateSpec)
                .map(V1PodTemplateSpec::getSpec).map(V1PodSpec::getContainers).orElseGet(() ->Lists.newArrayList());

        List<V1Container> v1InitContainers = Optional.of(v1PodTemplateSpec)
                .map(V1PodTemplateSpec::getSpec).map(V1PodSpec::getInitContainers).orElseGet(() ->null);

        // container + initcontainer Merge...
        Optional.ofNullable(v1InitContainers).ifPresent(v1Containers::addAll);

        for (V1Container containerRow : Optional.of(v1Containers).orElseGet(() ->Lists.newArrayList())) {
            for (V1EnvVar envVarRow : Optional.ofNullable(containerRow.getEnv()).orElseGet(() ->Lists.newArrayList())) {
                V1SecretKeySelector keySelector = Optional.ofNullable(envVarRow)
                        .map(V1EnvVar::getValueFrom)
                        .map(V1EnvVarSource::getSecretKeyRef)
                        .orElseGet(() ->null);
                if (Optional.ofNullable(keySelector).map(V1SecretKeySelector::getName).orElseGet(() ->"").equals(secret.getName())) {
                    if (checkEnvKey && Optional.of(secret).map(SecretGuiVO::getData).orElseGet(() ->Maps.newHashMap()).containsKey(keySelector.getKey())) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    /**
     * 환경변수 중 해당 secret key 사용 유무 (false가 사용중이다 헷갈리지 말자;; )
     * @param v1Containers
     * @param secret
     * @param checkEnvKey
     * @return
     * @throws Exception
     */
    private boolean isUsedSecretOfContainer(List<V1Container> v1Containers, SecretGuiVO secret, boolean checkEnvKey) throws Exception{
        for(V1Container containerRow : v1Containers){
            if(CollectionUtils.isNotEmpty(containerRow.getEnv())){
                for (V1EnvVar envVarRow : containerRow.getEnv()){
                    if(envVarRow.getValueFrom() != null && envVarRow.getValueFrom().getSecretKeyRef() != null){
                        if(StringUtils.equals(envVarRow.getValueFrom().getSecretKeyRef().getName(), secret.getName())){
                            if(checkEnvKey){
                                // 환경변수 중 해당 secret key 사용 유무
                                if(secret.getData().containsKey(envVarRow.getValueFrom().getSecretKeyRef().getKey())){
                                    return false;
                                }
                            }else {
                                return false;
                            }
                        }
                    }
                }
            }
        }

        return true;
    }

    /**
     * Secret Validation Check.
     * @throws Exception
     */
    public void checkSecretValidation(SecretGuiVO secretGui) throws Exception {
        this.checkSecretValidation(secretGui, true);
    }

    /**
     * Secret Validation Check.
     * @param secretGui
     * @throws Exception
     */
    public void checkSecretValidation(SecretGuiVO secretGui, boolean forceCheckDockerValue) throws Exception {
        if (StringUtils.length(secretGui.getDescription()) > 50) {
            throw new CocktailException("Secret description more than 50 characters.", ExceptionType.K8sSecretDescInvalid_MaxLengthLimit);
        }
        if (MapUtils.isEmpty(secretGui.getData())){
            throw new CocktailException("Secret data is invalid", ExceptionType.SecretDataInvalid);
        }

        boolean isValidKey = false;
        for (Map.Entry<String, String> dataEntry : secretGui.getData().entrySet()) {
            // checkData == false == 수정일때는 data가 없으면 "변경없음" 으로 처리함..
            // value는 체크하지 않고 key만 체크하도록 수정 hjchoi.20200107
            if (StringUtils.isBlank(dataEntry.getKey())) {
                throw new CocktailException("Secret data(key) is invalid", ExceptionType.SecretDataInvalid);
            }
            if (!dataEntry.getKey().matches(KubeConstants.RULE_SECRET_NAME)) {
                throw new CocktailException("Secret data(key) is invalid", ExceptionType.SecretDataInvalid);
            }

            if (secretGui.getType() == SecretType.DockerRegistry) {
                if (SecretType.DockerRegistry.getKeys().contains(dataEntry.getKey())) {
                    isValidKey = true;
                    try {
                        String dataValue = dataEntry.getValue();
                        if(forceCheckDockerValue || StringUtils.isNotBlank(dataValue)) {
                            boolean isBase64 = Utils.isBase64Encoded(dataValue);
                            if (isBase64) {
                                dataValue = new String(Base64Utils.decodeFromString(dataValue), "UTF-8");
                            }
                            JSONObject dockerConfigJson = new JSONObject(dataValue);
                            if (dockerConfigJson.getJSONObject("auths") == null) {
                                throw new CocktailException("Secret Data format is invalid. (dockerconfigjson)", ExceptionType.SecretDataInvalid);
                            }
                        }
                    }
                    catch (Exception e) {
                        throw new CocktailException("Secret Data format is invalid. (dockerconfigjson)", ExceptionType.SecretDataInvalid);
                    }
                }
            }
        }

        if(secretGui.getType() == SecretType.DockerRegistry) {
            if (!isValidKey) {
                throw new CocktailException("If SecretType is DockerRegistry then dockerconfigjson field is required", ExceptionType.SecretDataInvalid);
            }
        }
    }

    public ClusterVO setupCluster(Integer servicemapSeq) throws Exception {
        IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
        ClusterVO cluster = clusterDao.getClusterByServicemap(servicemapSeq);
        return cluster;
    }

    public ClusterVO setupCluster(Integer clusterSeq, String namespaceName) throws Exception {
        IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
        ClusterVO cluster = clusterDao.getCluster(clusterSeq);
        cluster.setNamespaceName(namespaceName);

        return cluster;
    }
}
