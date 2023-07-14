package run.acloud.api.build.service;


import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Maps;
import io.kubernetes.client.custom.QuantityFormatter;
import io.kubernetes.client.openapi.JSON;
import io.kubernetes.client.openapi.models.V1Affinity;
import io.kubernetes.client.openapi.models.V1Toleration;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import run.acloud.api.auth.enums.UserRole;
import run.acloud.api.build.dao.IBuildServerMapper;
import run.acloud.api.build.util.BuildServerUtils;
import run.acloud.api.build.vo.BuildServerAddVO;
import run.acloud.api.build.vo.BuildServerVO;
import run.acloud.api.catalog.enums.LaunchType;
import run.acloud.api.catalog.service.PackageCommonService;
import run.acloud.api.catalog.service.PackageK8sService;
import run.acloud.api.catalog.vo.HelmInstallRequestVO;
import run.acloud.api.catalog.vo.HelmReleaseBaseVO;
import run.acloud.api.catalog.vo.K8sObjectMapVO;
import run.acloud.api.configuration.dao.IClusterMapper;
import run.acloud.api.configuration.service.ClusterStateService;
import run.acloud.api.configuration.service.ClusterValidService;
import run.acloud.api.configuration.vo.ClusterVO;
import run.acloud.api.resource.constants.KubeConstants;
import run.acloud.api.resource.enums.K8sApiKindType;
import run.acloud.api.resource.enums.SecretType;
import run.acloud.api.resource.service.NamespaceService;
import run.acloud.api.resource.service.SecretService;
import run.acloud.api.resource.service.WorkloadResourceService;
import run.acloud.api.resource.util.ResourceUtil;
import run.acloud.api.resource.vo.K8sDeploymentVO;
import run.acloud.api.resource.vo.K8sNamespaceVO;
import run.acloud.api.resource.vo.SecretGuiVO;
import run.acloud.commons.util.CryptoUtils;
import run.acloud.framework.context.ContextHolder;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;
import run.acloud.framework.properties.CocktailBuilderProperties;
import run.acloud.framework.util.ExceptionMessageUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class BuildServerService {

    @Resource(name = "cocktailSession")
    private SqlSessionTemplate sqlSession;

    @Autowired
    private ClusterStateService clusterStateService;

    @Autowired
    private PackageCommonService packageCommonService;

    @Autowired
    private CocktailBuilderProperties cocktailBuilderProperties;

    @Autowired
    private PackageK8sService packageK8sService;

    @Autowired
    private ClusterValidService clusterValidService;

    @Autowired
    private NamespaceService namespaceService;

    @Autowired
    private SecretService secretService;

    @Autowired
    private WorkloadResourceService workloadResourceService;


    public List<BuildServerVO> getBuildServerList(Integer accountSeq, String topicName) throws Exception {
        IBuildServerMapper buildServerDao = sqlSession.getMapper(IBuildServerMapper.class);
        List<BuildServerVO> list = buildServerDao.getBuildServerList(accountSeq, topicName);
        for(BuildServerVO vo : list ){
            BuildServerUtils.deployConfigYamlToBuildServerVO(vo);
        }
        return list;
    }

    public List<BuildServerVO> getBuildServerListByWorkspace(Integer serviceSeq, String topicName) throws Exception {
        IBuildServerMapper buildServerDao = sqlSession.getMapper(IBuildServerMapper.class);
        List<BuildServerVO> list = buildServerDao.getBuildServerListForRef(serviceSeq, topicName);
        for(BuildServerVO vo : list ){
            BuildServerUtils.deployConfigYamlToBuildServerVO(vo);
        }
        return list;
    }

    public BuildServerVO getBuildServer(Integer buildServerSeq, boolean withConvert) throws Exception {
        IBuildServerMapper buildServerDao = sqlSession.getMapper(IBuildServerMapper.class);
        BuildServerVO buildServer = buildServerDao.getBuildServer(buildServerSeq);

        if (withConvert) {
            buildServer = BuildServerUtils.deployConfigYamlToBuildServerVO(buildServer);
        }
        return buildServer;
    }


    @Transactional(transactionManager = "transactionManager")
    public BuildServerVO addBuildServer(BuildServerAddVO buildServerAdd) throws Exception {
        BuildServerVO buildServer = null;

        IBuildServerMapper buildServerDao = sqlSession.getMapper(IBuildServerMapper.class);
        IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);

        if (!UserRole.valueOf(ContextHolder.exeContext().getUserRole()).isAdmin()) {
            if (buildServerAdd.getAccountSeq() == null) {
                buildServerAdd.setAccountSeq(ContextHolder.exeContext().getUserAccountSeq());
            }
        }

        // valid
        this.validBuildServerParam(buildServerAdd, true);

        // 클러스터 상태 체크
        ClusterVO cluster = clusterDao.getCluster(buildServerAdd.getClusterSeq());
        clusterStateService.checkClusterState(cluster);

        // 네임스페이스 존재 여부 체크
        this.checkNamespaceExists(cluster, buildServerAdd.getNamespace());

        // 빌드 서버명 중복 체크
        if (CollectionUtils.isNotEmpty(
                buildServerDao.getBuildServerList(
                        buildServerAdd.getAccountSeq()
                        , String.format("%s-%s-%s", buildServerAdd.getBuildServerName(), buildServerAdd.getNamespace(), cluster.getClusterId())))
        ) {
            String errMsg = String.format("A build server with that name already exists. - [%s]!!", buildServerAdd.getBuildServerName());
            throw new CocktailException(errMsg, ExceptionType.InvalidParameter, errMsg);
        }

        // 빌드 queue URL 셋팅
        // - 대상 클러스터가 칵테일 클러스터라면 내부 URL
        if (clusterValidService.isCocktailCluster(cluster)) {
            buildServerAdd.setAppsEventServer(cocktailBuilderProperties.getBuildQueueUrl());
        }
        // - 아니면 외부 URL 세팅
        else {
            buildServerAdd.setAppsEventServer(cocktailBuilderProperties.getBuildQueueExternalUrl());
            ExceptionMessageUtils.checkParameterRequired("AppsEventServer", buildServerAdd.getAppsEventServer(), "No external event service is set up for the build server.");
        }

        // build 생성
        BuildServerVO vo = BuildServerUtils.copyBuildServerVO(buildServerAdd);

        String deployConfigYAML = BuildServerUtils.buildServerVOToDeployConfigYAML(
                                                                buildServerAdd,
                                                                cluster.getClusterId(),
                                                                cocktailBuilderProperties.getBuildQueueUser(),
                                                                cocktailBuilderProperties.getBuildQueuePasswd(),
                                                                this.buildQueue(true));

        // build queue 접속 정보 암호화
        String deployConfigYAMLWithCrypto = BuildServerUtils.buildServerVOToDeployConfigYAML(
                                                                buildServerAdd,
                                                                cluster.getClusterId(),
                                                                CryptoUtils.encryptAES(cocktailBuilderProperties.getBuildQueueUser()),
                                                                CryptoUtils.encryptAES(cocktailBuilderProperties.getBuildQueuePasswd()),
                                                                this.buildQueue(true));
        // DB 저장시 암호화된 정보로 저장
        vo.setDeployConfig(deployConfigYAMLWithCrypto);
        vo.setClusterId(cluster.getClusterId());

        HelmInstallRequestVO helmInstallRequest = new HelmInstallRequestVO();
        helmInstallRequest.setRepo(cocktailBuilderProperties.getBuildServerChartRepo());
        helmInstallRequest.setChartName(cocktailBuilderProperties.getBuildServerChartName());
        helmInstallRequest.setNamespace(buildServerAdd.getNamespace());
        helmInstallRequest.setReleaseName(buildServerAdd.getBuildServerName());
        helmInstallRequest.setValues(deployConfigYAML);
        helmInstallRequest.setLaunchType(LaunchType.ADD.getType());
        helmInstallRequest.setVersion(cocktailBuilderProperties.getBuildServerChartVersion());

        try {
            HelmReleaseBaseVO result = packageCommonService.installPackage(buildServerAdd.getClusterSeq(), buildServerAdd.getNamespace(), helmInstallRequest, ContextHolder.exeContext());

            // get ControllerName
            List<K8sObjectMapVO> list = packageK8sService.getK8sObjectList(result.getManifest(), null);
            Optional<K8sObjectMapVO> k8sObject = list.stream().filter(k8sObjectMapVO -> k8sObjectMapVO.getK8sApiKindType() == K8sApiKindType.DEPLOYMENT).findFirst();
            if(k8sObject.isPresent()){
                vo.setControllerName(k8sObject.get().getName());
            }
        } catch (CocktailException e) {
            try {
                // remove package
                packageCommonService.unInstallPackage(buildServerAdd.getClusterSeq(), buildServerAdd.getNamespace(), buildServerAdd.getBuildServerName());
            } catch (Exception ex) {
                log.warn(ex.getMessage(), ex);
            }
            throw e;
        } catch (Exception e) {
            try {
                // remove package
                packageCommonService.unInstallPackage(buildServerAdd.getClusterSeq(), buildServerAdd.getNamespace(), buildServerAdd.getBuildServerName());
            } catch (Exception ex) {
                log.warn(ex.getMessage(), ex);
            }
            throw new CocktailException(e.getMessage(), e, ExceptionType.PackageInstallFail);
        }

        // 등록
        buildServerDao.addBuildServer(vo);
        buildServerDao.addBuildServerMapping(vo);

        // secret에 values 저장
        this.setSecretDataValues(cluster, vo.getNamespace(), vo, deployConfigYAML);

        buildServer = this.getBuildServer(vo.getBuildServerSeq(), true);


        return buildServer;
    }

    @Transactional(transactionManager = "transactionManager")
    public BuildServerVO editBuildServer(BuildServerAddVO buildServerAdd) throws Exception {

        IBuildServerMapper buildServerDao = sqlSession.getMapper(IBuildServerMapper.class);
        IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);

        if (!UserRole.valueOf(ContextHolder.exeContext().getUserRole()).isAdmin()) {
            buildServerAdd.setAccountSeq(ContextHolder.exeContext().getUserAccountSeq());
        }

        // valid
        this.validBuildServerParam(buildServerAdd, false);

        // 클러스터 상태 체크
        ClusterVO cluster = clusterDao.getCluster(buildServerAdd.getClusterSeq());
        clusterStateService.checkClusterState(cluster);

        // 빌드 queue URL 셋팅
        // - 대상 클러스터가 칵테일 클러스터라면 내부 URL
        if (clusterValidService.isCocktailCluster(cluster)) {
            buildServerAdd.setAppsEventServer(cocktailBuilderProperties.getBuildQueueUrl());
        }
        // - 아니면 외부 URL 세팅
        else {
            buildServerAdd.setAppsEventServer(cocktailBuilderProperties.getBuildQueueExternalUrl());
            ExceptionMessageUtils.checkParameterRequired("AppsEventServer", buildServerAdd.getAppsEventServer(), "No external event service is set up for the build server.");
        }

        BuildServerVO vo = BuildServerUtils.copyBuildServerVO(buildServerAdd);
        String deployConfigYAML = BuildServerUtils.buildServerVOToDeployConfigYAML(
                                                        buildServerAdd,
                                                        cluster.getClusterId(),
                                                        cocktailBuilderProperties.getBuildQueueUser(),
                                                        cocktailBuilderProperties.getBuildQueuePasswd(),
                                                        this.buildQueue(true));
        // build queue 접속 정보 암호화
        String deployConfigYAMLWithCrypto = BuildServerUtils.buildServerVOToDeployConfigYAML(
                                                        buildServerAdd,
                                                        cluster.getClusterId(),
                                                        CryptoUtils.encryptAES(cocktailBuilderProperties.getBuildQueueUser()),
                                                        CryptoUtils.encryptAES(cocktailBuilderProperties.getBuildQueuePasswd()),
                                                        this.buildQueue(true));
        // DB 저장시 암호화된 정보로 저장
        vo.setDeployConfig(deployConfigYAMLWithCrypto);

        HelmInstallRequestVO helmInstallRequest = new HelmInstallRequestVO();
        helmInstallRequest.setRepo(cocktailBuilderProperties.getBuildServerChartRepo());
        helmInstallRequest.setChartName(cocktailBuilderProperties.getBuildServerChartName());
        helmInstallRequest.setNamespace(buildServerAdd.getNamespace());
        helmInstallRequest.setReleaseName(buildServerAdd.getBuildServerName());
        helmInstallRequest.setValues(deployConfigYAML);
        helmInstallRequest.setLaunchType(LaunchType.ADD.getType());
        helmInstallRequest.setVersion(cocktailBuilderProperties.getBuildServerChartVersion());
        packageCommonService.upgradePackage(buildServerAdd.getClusterSeq(), buildServerAdd.getNamespace(), helmInstallRequest.getReleaseName(), helmInstallRequest);

        // 빌드 수정
        buildServerDao.editBuildServer(vo);

        // secret에 values 저장
        this.setSecretDataValues(cluster, vo.getNamespace(), vo, deployConfigYAML);

        BuildServerVO build = buildServerDao.getBuildServer(buildServerAdd.getBuildServerSeq());

        return build;
    }

    private void validBuildServerParam(BuildServerAddVO add, boolean isAdd) throws Exception {
        if (add != null) {
            JSON k8sJson = new JSON();
            QuantityFormatter quantityFormatter = new QuantityFormatter();
            if (!isAdd) {
                ExceptionMessageUtils.checkParameterRequired("buildServerSeq", add.getBuildServerSeq());
            }
            ExceptionMessageUtils.checkParameterRequired("accountSeq", add.getAccountSeq());
            ExceptionMessageUtils.checkParameterRequired("clusterSeq", add.getClusterSeq());
            ExceptionMessageUtils.checkParameter("namespace", add.getNamespace(), 256, true);
            if (!add.getNamespace().matches(KubeConstants.RULE_RESOURCE_NAME)) {
                throw new CocktailException("namespace is invalid", ExceptionType.InvalidParameter, ResourceUtil.getInvalidNameMsg("namespace is invalid"));
            }
            ExceptionMessageUtils.checkParameter("buildServerName", add.getBuildServerName(), 50, true);
            if (!add.getBuildServerName().matches(KubeConstants.RULE_RESOURCE_NAME)) {
                throw new CocktailException("Build Server name is invalid", ExceptionType.InvalidParameter, ResourceUtil.getInvalidNameMsg("Build Server name is invalid"));
            }
            ExceptionMessageUtils.checkParameter("buildServerDesc", add.getBuildServerDesc(), 256, false);
            ExceptionMessageUtils.checkParameterRequired("cpuRequest", add.getCpuRequest());
            ExceptionMessageUtils.checkParameterRequired("cpuLimit", add.getCpuLimit());
            if (add.getCpuRequest() > add.getCpuLimit()) {
                String errMsg = "cpu request value cannot be greater than cpu limit value!!";
                throw new CocktailException(errMsg, ExceptionType.InvalidParameter, errMsg);
            }
            ExceptionMessageUtils.checkParameterRequired("memoryRequest", add.getMemoryRequest());
            ExceptionMessageUtils.checkParameterRequired("memoryLimit", add.getMemoryLimit());
            if (add.getMemoryRequest() > add.getMemoryLimit()) {
                String errMsg = "memory request value cannot be greater than memory limit value!!";
                throw new CocktailException(errMsg, ExceptionType.InvalidParameter, errMsg);
            }
            if (StringUtils.isNotBlank(add.getNodeSelector())) {
                try {
                    // spec 체크
                    BuildServerUtils.getYamlValue(add.getNodeSelector(), new TypeReference<Map<String, String>>(){}, k8sJson);
                } catch (Exception e) {
                    String errMsg = String.format("Invalid node selector!! - %s", e.getMessage());
                    throw new CocktailException(errMsg, e, ExceptionType.InvalidParameter, errMsg);
                }
            }
            if (StringUtils.isNotBlank(add.getTolerations())) {
                try {
                    // spec 체크
                    // k8s model로 체크
                    BuildServerUtils.getYamlValue(add.getTolerations(), new TypeReference<List<V1Toleration>>(){}, k8sJson);
                } catch (Exception e) {
                    String errMsg = String.format("Invalid Tolerations!! - %s", e.getMessage());
                    throw new CocktailException(errMsg, e, ExceptionType.InvalidParameter, errMsg);
                }
            }
            if (StringUtils.isNotBlank(add.getAffinity())) {
                try {
                    // spec 체크
                    // k8s model로 체크
                    BuildServerUtils.getYamlValue(add.getAffinity(), new TypeReference<V1Affinity>(){}, k8sJson);
                } catch (Exception e) {
                    String errMsg = String.format("Invalid Affinity!! - %s", e.getMessage());
                    throw new CocktailException(errMsg, e, ExceptionType.InvalidParameter, errMsg);
                }
            }
            // pvc.enabled
            // 존재하는 PVC로 하거나 PVC 생성하는 정보 체크
            if (BooleanUtils.toBoolean(add.getPersistenceEnabled())) {
                // 존재하는 PVC 명
                // add.getPvcName() == persistence.workspace.existingClaim
                if (StringUtils.isNotBlank(add.getPvcName())) {
                    // k8s naming validate 체크
                    if (!add.getPvcName().matches(KubeConstants.RULE_RESOURCE_NAME)) {
                        throw new CocktailException("pvc name is invalid", ExceptionType.InvalidParameter, ResourceUtil.getInvalidNameMsg("pvc name is invalid"));
                    }
                }
                // PVC 생성
                else {
                    if (StringUtils.isNotBlank(add.getStorageClass())) {
                        // k8s naming validate 체크
                        if (!add.getStorageClass().matches(KubeConstants.RULE_RESOURCE_NAME)) {
                            throw new CocktailException("StorageClass is invalid", ExceptionType.InvalidParameter, ResourceUtil.getInvalidNameMsg("StorageClass is invalid"));
                        }
                    } else {
                        ExceptionMessageUtils.checkParameterRequired("storageClass", add.getStorageClass());
                    }
                    if (StringUtils.isNotBlank(add.getPvcSize())) {
                        try {
                            // 단위 Quantity format 체크
                            quantityFormatter.parse(add.getPvcSize());
                        } catch (Exception e) {
                            String errMsg = String.format("Invalid pvc size!! - %s", e.getMessage());
                            throw new CocktailException(errMsg, e, ExceptionType.InvalidParameter, errMsg);
                        }
                    } else {
                        ExceptionMessageUtils.checkParameterRequired("pvcSize", add.getPvcSize());
                    }
                }
            }
        }
    }


    /**
     * Build server 삭제 처리 메서드.
     *
     * @param buildServerSeq 빌드 삭제에 대해 생성된 BuildRunVO 가 넘어옴
     */
    public BuildServerVO removeBuildServer(Integer buildServerSeq) throws Exception {
        IBuildServerMapper bsDao = sqlSession.getMapper(IBuildServerMapper.class);
        IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);

        BuildServerVO buildServer = this.getBuildServer(buildServerSeq, false);
        BuildServerVO buildServerVO = new BuildServerVO();
        buildServerVO.setBuildServerSeq(buildServerSeq);

        // 클러스터 상태 체크
        ClusterVO cluster = clusterDao.getCluster(buildServer.getClusterSeq());
        clusterStateService.checkClusterState(cluster);

        bsDao.removeBuildServer(buildServerVO);
        HelmInstallRequestVO helmInstallRequest = new HelmInstallRequestVO();
        helmInstallRequest.setRepo(cocktailBuilderProperties.getBuildServerChartRepo());
        helmInstallRequest.setChartName(cocktailBuilderProperties.getBuildServerChartName());
        helmInstallRequest.setNamespace(buildServer.getNamespace());
        helmInstallRequest.setReleaseName(buildServer.getBuildServerName());
        helmInstallRequest.setValues(buildServer.getDeployConfig());
        helmInstallRequest.setLaunchType(LaunchType.ADD.getType());
        helmInstallRequest.setVersion(cocktailBuilderProperties.getBuildServerChartVersion());
        packageCommonService.unInstallPackage(buildServer.getClusterSeq(), buildServer.getNamespace(), buildServer.getBuildServerName());

        // secret 삭제
        secretService.deleteSecret(cluster, buildServer.getNamespace(), BuildServerUtils.makeSecretName(buildServer.getControllerName()), false);

        return buildServer;
    }

    private Map<String, Object> buildQueue(boolean enabled) {
        Map<String, Object> dataMap = Maps.newHashMap();
        dataMap.put("enabled", enabled);
        if(enabled){
            String certDir = cocktailBuilderProperties.getBuildQueueClientCertDir();
            try {
                // 인증서 경로 설정
                Path certPath = Paths.get(certDir+"/tls.crt");
                Path keyPath = Paths.get(certDir+"/tls.key");
                Path caPath = Paths.get(certDir+"/ca.crt");
                String cert = Files.readString(certPath);
                String key = Files.readString(keyPath);
                String ca = Files.readString(caPath);
                dataMap.put("caCert", ca);
                dataMap.put("tlsCert", cert);
                dataMap.put("tlsKey", key);
            } catch (IOException e) {
                log.error("No search file Nats Tls", e);
            }
        }
        return dataMap;
    }

    private void checkNamespaceExists(ClusterVO cluster, String namespace) throws Exception {
        // 네임스페이스 존재 여부 체크
        String fieldSelector = String.format("metadata.name=%s", namespace);
        List<K8sNamespaceVO> namespaces = namespaceService.getNamespacesToList(cluster, fieldSelector, null, ContextHolder.exeContext());
        if(CollectionUtils.isEmpty(namespaces)){
            String errMsg = String.format("Namespace not exists  - [%s]!!", namespace);
            throw new CocktailException(errMsg, ExceptionType.ResourceNotFound, errMsg);
        }
    }

    /**
     * secret에 values 저장
     *
     * @param cluster
     * @param namespace
     * @param vo
     * @param deployConfigYAML
     * @throws Exception
     */
    private void setSecretDataValues(ClusterVO cluster, String namespace, BuildServerVO vo, String deployConfigYAML) throws Exception {
        SecretGuiVO s = new SecretGuiVO();
        s.setNamespace(namespace);
        s.setName(BuildServerUtils.makeSecretName(vo.getControllerName())); // build_server명 + '-config'
        s.setType(SecretType.Generic);
        s.putLabelsItem(KubeConstants.META_LABELS_APP_MANAGED_BY, BuildServerUtils.LABELS_VALUE_PIPELINE_SERVER);
        s.putLabelsItem(KubeConstants.META_LABELS_APP_PART_OF, vo.getControllerName());
        s.putDataItem(BuildServerUtils.SECRET_DATA_KEY_VALUES, CryptoUtils.encryptDefaultAES(deployConfigYAML));
        // 없다면 등록해줌
        if (CollectionUtils.isEmpty(secretService.getSecrets(cluster, namespace, String.format("%s.%s=%s", KubeConstants.META, KubeConstants.NAME, s.getName()), null, false))) {
            secretService.createSecret(cluster, namespace, s);
        } else {
            secretService.patchSecret(cluster, namespace, s);
        }
    }

    public void addBuildServerIfExists(ClusterVO cluster, Integer accountSeq) throws Exception {
        if (clusterStateService.isClusterRunning(cluster)) {
            // build server Deployment 조회
            List<K8sDeploymentVO> buildServers = workloadResourceService.getDeployments(cluster, null, null, String.format("%s=%s", KubeConstants.META_LABELS_APP_NAME, BuildServerUtils.LABELS_VALUE_PIPELINE_SERVER), ContextHolder.exeContext());
            if (CollectionUtils.isNotEmpty(buildServers)) {
                IBuildServerMapper buildServerDao = sqlSession.getMapper(IBuildServerMapper.class);

                Map<String, Map<String, K8sDeploymentVO>> buildServerMap = Maps.newHashMap();
                for (K8sDeploymentVO bs : buildServers) {
                    buildServerMap.putIfAbsent(bs.getNamespace(), Maps.newHashMap());
                    buildServerMap.get(bs.getNamespace()).put(bs.getName(), bs);
                }

                // build server values data secret 조회
                List<SecretGuiVO> secrets = secretService.getSecrets(cluster, null, null, String.format("%s=%s", KubeConstants.META_LABELS_APP_MANAGED_BY, BuildServerUtils.LABELS_VALUE_PIPELINE_SERVER), false, true);
                if (CollectionUtils.isNotEmpty(secrets)) {
                    for (SecretGuiVO s : secrets) {
                        if (buildServerMap.containsKey(s.getNamespace())) {
                            // secret명(build_server명 + '-config')에서 Suffix를 제거한 build server가 있는지 체크
                            if (MapUtils.getObject(buildServerMap.get(s.getNamespace()), StringUtils.removeEnd(s.getName(), BuildServerUtils.SECRET_NAME_SUFFIX), null) != null) {
                                if (StringUtils.isNotBlank(s.getData().get(BuildServerUtils.SECRET_DATA_KEY_VALUES))) {
                                    K8sDeploymentVO bs = buildServerMap.get(s.getNamespace()).get(StringUtils.removeEnd(s.getName(), BuildServerUtils.SECRET_NAME_SUFFIX));
                                    BuildServerVO vo = new BuildServerVO();
                                    vo.setBuildServerName(bs.getLabels().get(KubeConstants.META_LABELS_APP_INSTANCE));
                                    vo.setAccountSeq(accountSeq);
                                    vo.setClusterSeq(cluster.getClusterSeq());
                                    vo.setClusterId(cluster.getClusterId());
                                    vo.setNamespace(s.getNamespace());
                                    vo.setControllerName(bs.getName());
                                    vo.setDeployConfig(CryptoUtils.decryptDefaultAES(new String(Base64.getDecoder().decode(s.getData().get(BuildServerUtils.SECRET_DATA_KEY_VALUES)))));

                                    buildServerDao.addBuildServer(vo);
                                    buildServerDao.addBuildServerMapping(vo);
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
