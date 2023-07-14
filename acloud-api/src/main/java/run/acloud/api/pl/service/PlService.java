package run.acloud.api.pl.service;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import io.kubernetes.client.openapi.JSON;
import io.kubernetes.client.openapi.models.*;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Validator;
import run.acloud.api.auth.enums.UserGrant;
import run.acloud.api.auth.enums.UserRole;
import run.acloud.api.auth.vo.UserVO;
import run.acloud.api.build.enums.*;
import run.acloud.api.build.service.PipelineBuildValidationService;
import run.acloud.api.build.service.WrapPipelineAsyncService;
import run.acloud.api.build.vo.*;
import run.acloud.api.configuration.dao.IAccountMapper;
import run.acloud.api.configuration.dao.IClusterMapper;
import run.acloud.api.configuration.service.AddonCommonService;
import run.acloud.api.configuration.service.ClusterVolumeService;
import run.acloud.api.configuration.service.UserService;
import run.acloud.api.configuration.vo.*;
import run.acloud.api.cserver.dao.IServiceMapper;
import run.acloud.api.cserver.dao.IServicemapMapper;
import run.acloud.api.cserver.enums.DeployType;
import run.acloud.api.cserver.enums.PortType;
import run.acloud.api.cserver.enums.WorkloadType;
import run.acloud.api.cserver.enums.WorkloadVersion;
import run.acloud.api.cserver.service.ServerConversionService;
import run.acloud.api.cserver.service.ServerValidService;
import run.acloud.api.cserver.service.ServiceService;
import run.acloud.api.cserver.util.ServerUtils;
import run.acloud.api.cserver.vo.*;
import run.acloud.api.k8sextended.models.*;
import run.acloud.api.pipelineflow.service.WrappedBuildService;
import run.acloud.api.pipelineflow.util.PipelineTypeConverter;
import run.acloud.api.pipelineflow.vo.PipelineCommandVO;
import run.acloud.api.pl.dao.IPlMapper;
import run.acloud.api.pl.enums.PlResType;
import run.acloud.api.pl.enums.PlRunMode;
import run.acloud.api.pl.enums.PlRunType;
import run.acloud.api.pl.enums.PlStatus;
import run.acloud.api.pl.vo.*;
import run.acloud.api.resource.constants.KubeConstants;
import run.acloud.api.resource.enums.K8sApiGroupType;
import run.acloud.api.resource.enums.K8sApiKindType;
import run.acloud.api.resource.enums.K8sApiType;
import run.acloud.api.resource.enums.K8sApiVerType;
import run.acloud.api.resource.service.*;
import run.acloud.api.resource.task.K8sSpecFactory;
import run.acloud.api.resource.util.ResourceUtil;
import run.acloud.api.resource.vo.*;
import run.acloud.commons.util.Base64Utils;
import run.acloud.commons.util.CryptoUtils;
import run.acloud.commons.util.JsonUtils;
import run.acloud.commons.util.PagingUtils;
import run.acloud.commons.vo.ExecutingContextVO;
import run.acloud.commons.vo.ListCountVO;
import run.acloud.commons.vo.PagingVO;
import run.acloud.framework.context.ContextHolder;
import run.acloud.framework.enums.ExceptionBiz;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;
import run.acloud.framework.properties.CocktailServiceProperties;
import run.acloud.framework.util.ExceptionMessageUtils;
import run.acloud.protobuf.pipeline.PipelineAPIServiceProto;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PlService {

    @Resource(name = "cocktailSession")
    private SqlSessionTemplate sqlSession;

    @Autowired
    private PlAsyncService plAsyncService;

    @Autowired
    private WorkloadResourceService workloadResourceService;

    @Autowired
    private AddonCommonService addonCommonService;

    @Autowired
    private StorageClassService storageClassService;

    @Autowired
    private K8sResourceService k8sResourceService;

    @Autowired
    private ServiceSpecService serviceSpecService;

    @Autowired
    private ConfigMapService configMapService;

    @Autowired
    private SecretService secretService;

    @Autowired
    private PersistentVolumeService persistentVolumeService;

    @Autowired
    private ClusterVolumeService clusterVolumeService;

    @Autowired
    private IngressSpecService ingressSpecService;

    @Autowired
    private ServerConversionService serverConversionService;

    @Autowired
    private ServerValidService serverValidService;

    @Autowired
    private WrappedBuildService buildService;

    @Autowired
    private WrapPipelineAsyncService pipelineAsyncService;

    @Autowired
    private PipelineBuildValidationService buildValidationService;

    @Autowired
    @Qualifier(value = "pipelineBuildAddValidator")
    private Validator buildAddValidator;

    @Autowired
    private PlRunBuildService plRunBuildService;

    @Autowired
    private UserService userService;

    @Autowired
    private PlEventService plEventService;

    @Autowired
    private CocktailServiceProperties cocktailServiceProperties;

    @Autowired
    private ServiceService serviceService;

    /**
     * 파이프라인 목록 조회
     *
     * @param accountSeq
     * @param serviceSeq
     * @return
     * @throws Exception
     */
    public List<PlMasterListVO> getPlList(Integer accountSeq, Integer serviceSeq) throws Exception {
        IPlMapper dao = sqlSession.getMapper(IPlMapper.class);
        List<PlMasterListVO> list = dao.getPlList(accountSeq, serviceSeq);

        // status 재설정
        for(PlMasterListVO vo : list){
            if ((vo.getStatus() == PlStatus.DONE || (vo.getStatus() != PlStatus.RUNNING && vo.getStatus() != PlStatus.ERROR && vo.getStatus() != PlStatus.CREATED && vo.getStatus() != PlStatus.CANCELED) )
                    && (vo.getReleaseVer() != null && !vo.getVer().equals(vo.getReleaseVer()))) {
                vo.setStatus(PlStatus.UPDATED);
            }
        }

        return list;
    }


    /**
     * 파이프라인 상세 조회
     *
     * @param plSeq
     * @return
     * @throws Exception
     */
    public PlMasterVO getPlDetail(Integer plSeq) throws Exception {
        return this.getPlDetail(plSeq, false);
    }

    /**
     * 파이프라인 상세 조회
     *
     * @param plSeq
     * @param includeDetailType - 특정 배포 리소스의 상세 유형 정보 포함 여부
     * @return
     * @throws Exception
     */
    public PlMasterVO getPlDetail(Integer plSeq, boolean includeDetailType) throws Exception {
        IPlMapper dao = sqlSession.getMapper(IPlMapper.class);

        PlMasterVO detail = dao.getPlDetail(plSeq);

        if (detail != null ) {
            // Pl 상태 체크, 기존 배포된 버전이 존재하는데 현재 버전이 다르면 UPDATED 상테 셋팅
            // RUNNING 상태는 제외 처리 - 2021.03.30 hjchoi
            if ((detail.getStatus() == PlStatus.DONE || (detail.getStatus() != PlStatus.RUNNING && detail.getStatus() != PlStatus.ERROR && detail.getStatus() != PlStatus.CANCELED) )
                    && (detail.getReleaseVer() != null && !detail.getVer().equals(detail.getReleaseVer()))) {
                detail.setStatus(PlStatus.UPDATED);
            }

            // build 실행정보 string -> vo 형태로 변경
            List<PlResBuildVO> builds = detail.getPlResBuilds();

            if(builds != null && builds.size() > 0) {
                for (PlResBuildVO build : builds) {
                    // 복호화 및 build step VO 변경
                    BuildRunVO buildRunVO = this.getBuildRunFromBuildCont(build.getBuildCont());
                    if( buildRunVO != null) {
                        buildRunVO = buildService.convertBuildStepRunConfig(buildRunVO, false);
                    }

                    build.setBuildConfig(buildRunVO);
                    build.setBuildCont(null);
                }
            }

            // 파이프라인 상태 - Released
            boolean isReleased = (detail.getStatus() == PlStatus.DONE && (detail.getReleaseVer() != null && detail.getVer().equals(detail.getReleaseVer())));

            if (CollectionUtils.isNotEmpty(detail.getPlResDeploys()) && (includeDetailType || isReleased)) {
                IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
                ClusterVO cluster = clusterDao.getCluster(detail.getClusterSeq());
                WorkloadVersion workloadVersion = WorkloadVersion.V1;

                for (PlResDeployVO deploy : detail.getPlResDeploys()) {
                    switch (deploy.getResType()) {
                        case SVC:
                            if (includeDetailType) {
                                // 서비스 유형 셋팅
                                K8sServiceVO k8sService = this.convertServiceYamlToK8sObj(deploy.getResCont());
                                deploy.setResDetailType(k8sService.getDetail().getType());
                            }
                            break;
                        case REPLICA_SERVER:
                        case STATEFUL_SET_SERVER:
                        case DAEMON_SET_SERVER:
                        case CRON_JOB_SERVER:
                        case JOB_SERVER:
                            // 파이프라인 상태 - Released일 경우 추가 정보 셋팅
                            if (isReleased) {
                                WorkloadType workloadType = WorkloadType.valueOf(deploy.getResType().getCode());
                                List<Object> objs = ServerUtils.getYamlObjects(deploy.getResCont());
                                ServerGuiVO serverGui = serverConversionService.convertYamlToGui(cluster, deploy.getNamespace(), workloadType.getCode(), workloadVersion.getCode(), null, null, objs);
                                deploy.setWorkloadConfig(serverGui);
                            }
                            break;
                    }
                }
            }
        }

        return detail;
    }

    /**
     * 파이프라인 빌드 정보 상세, 패스워드 없이 조
     *
     * @param plSeq
     * @param plResBuildSeq
     * @return
     * @throws Exception
     */
    public PlResBuildVO getPlResBuild(Integer plSeq, Integer plResBuildSeq) throws Exception {
        return getPlResBuild(plSeq,  plResBuildSeq, true);
    }

    public PlResBuildVO getPlResBuild(Integer plSeq, Integer plResBuildSeq, boolean withoutPasswd) throws Exception {
        IPlMapper dao = sqlSession.getMapper(IPlMapper.class);
        PlResBuildVO buildVO = dao.getPlResBuild(plSeq, plResBuildSeq);

        if (buildVO != null) {
            BuildRunVO buildConfig = plRunBuildService.convertBuildRunStringToDecryptedBuildRunVO(buildVO.getBuildCont(), withoutPasswd);

            buildVO.setBuildConfig(buildConfig);
            buildVO.setBuildCont(null);
        } else {
            // throw exception
            this.throwPipelineResNotFound("build");
        }

        return buildVO;
    }

    /**
     * 파이프라인 생성
     *
     * @param servicemapSeq
     * @param plMaster
     * @throws Exception
     */
    public PlMasterVO addPipeline(Integer servicemapSeq, PlMasterVO plMaster) throws Exception {

        // userSeq, serviceSeq 추출, pipeline은 항상 serviceSeq 값이 존재한다.
        ExecutingContextVO ctx = ContextHolder.exeContext();
        Integer serviceSeq = ctx.getUserServiceSeq();

        /** 권한 검증 및 데이터 체크 시작 **/

        // appmap 조회 및 체크
        ServiceDetailVO service;
        try {
            service = (ServiceDetailVO)serviceService.getService(serviceSeq);
        } catch (Exception e) {
            log.error("Service not found.[{}]", serviceSeq, e);
            throw new CocktailException("Not authorized to request!!", ExceptionType.NotAuthorizedToRequest);
        }

        Optional<ServicemapDetailResourceVO> servicemapOptional = service.getServicemaps().stream().filter(s -> s.getServicemapSeq().equals(servicemapSeq)).findFirst();
        if (!servicemapOptional.isPresent()){
            throw new CocktailException("Not authorized to request!!", ExceptionType.NotAuthorizedToRequest);
        }

        // 파이프라인 이름 중복 체크
        IPlMapper plDao = sqlSession.getMapper(IPlMapper.class);
        List<PlMasterVO> plList = plDao.getPlMasterByNameAndWorkspace(plMaster.getName(), serviceSeq);

        if (CollectionUtils.isNotEmpty(plList)){
            throw new CocktailException("The Pipeline's Name is used already.", ExceptionType.CommonCreateFail, ExceptionBiz.PIPELINE_SET);
        }
        /** 권한 검증 및 데이터 체크 끝 **/

        // 클러스터 정보 조회
        IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);

        ClusterVO clusterVO = clusterDao.getClusterByServicemap(servicemapSeq);
        if (clusterVO == null) {
            throw new CocktailException("Has not Cluster.", ExceptionType.PipelineCreationFail_ParameterInvalid);
        }

        // namespace 조회
        String namespaceName = clusterVO.getNamespaceName();

        return this.addPipeline(clusterVO, namespaceName, plMaster);
    }

    /**
     * 파이프라인 생성
     *
     * @param clusterSeq
     * @param namespaceName
     * @param plMaster
     * @throws Exception
     */
    @Transactional(transactionManager = "transactionManager")
    public PlMasterVO addPipeline(Integer clusterSeq, String namespaceName, PlMasterVO plMaster) throws Exception {
        IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
        ClusterVO cluster = clusterDao.getCluster(clusterSeq);

        return this.addPipeline(cluster, namespaceName, plMaster);
    }

    /**
     * 파이프라인 생성
     *
     * @param cluster
     * @param namespaceName
     * @param plMaster
     * @throws Exception
     */
    @Transactional(transactionManager = "transactionManager")
    public PlMasterVO addPipeline(ClusterVO cluster, String namespaceName, PlMasterVO plMaster) throws Exception {
        if (cluster != null && StringUtils.isNotBlank(namespaceName) && plMaster != null) {
            if (StringUtils.isBlank(plMaster.getName())) {
                throw new CocktailException("Pipeline name is required.", ExceptionType.InvalidParameter_Empty, "Pipeline name is required.");
            }
            if (StringUtils.isBlank(plMaster.getVer())) {
                throw new CocktailException("Pipeline version is required.", ExceptionType.InvalidParameter_Empty, "Pipeline version is required.");
            }

            IPlMapper plDao = sqlSession.getMapper(IPlMapper.class);

            plMaster.setClusterSeq(cluster.getClusterSeq());
            plMaster.setNamespace(namespaceName);

            // pl_master 등록
            plDao.insertPlMaster(plMaster);

        }

        return plMaster;
    }

    @Transactional(transactionManager = "transactionManager")
    public void addPlResDeploies(Integer plSeq, List<PlResDeployVO> plResDeploys) throws Exception {
        IPlMapper plDao = sqlSession.getMapper(IPlMapper.class);
        IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);

        PlMasterVO plMaster = plDao.getPlMaster(plSeq);
        ClusterVO cluster = clusterDao.getCluster(plMaster.getClusterSeq());

        this.addPlResDeploies(cluster, plMaster.getNamespace(), plSeq, plResDeploys, null);
    }

    @Transactional(transactionManager = "transactionManager")
    public void addPlResDeploies(ClusterVO cluster, String namespace, Integer plSeq, List<PlResDeployVO> plResDeploys) throws Exception {
        this.addPlResDeploies(cluster, namespace, plSeq, plResDeploys, null);
    }

    /**
     * 파이프라인 다건의 배포 리소스 생성
     *
     * @param cluster
     * @param namespace
     * @param plSeq
     * @param plResDeploys - resType, resName, runOrder(워크로드일 경우)
     * @throws Exception
     */
    @Transactional(transactionManager = "transactionManager")
    public void addPlResDeploies(ClusterVO cluster, String namespace, Integer plSeq, List<PlResDeployVO> plResDeploys, IPlMapper plDao) throws Exception {
        if (cluster != null && StringUtils.isNotBlank(namespace) && plSeq != null) {
            if (CollectionUtils.isNotEmpty(plResDeploys)) {
                if (plDao == null) {
                    plDao = sqlSession.getMapper(IPlMapper.class);
                }
                IServiceMapper serviceDao = sqlSession.getMapper(IServiceMapper.class);

                // 워크스페이스에 할당된 registry project ID
                List<ServiceRegistryUserVO> registryUsers = serviceDao.getRegistryUserByNamespace(cluster.getClusterSeq(), namespace);
                List<Integer> projectIds = Lists.newArrayList();
                List<Integer> externalRegistryIds = Lists.newArrayList();
                if (CollectionUtils.isNotEmpty(registryUsers)) {
                    for (ServiceRegistryUserVO service : registryUsers) {
                        projectIds.addAll(Optional.ofNullable(service.getProjects()).orElseGet(() ->Lists.newArrayList()).stream().map(ServiceRegistryVO::getProjectId).collect(Collectors.toList()));
                        externalRegistryIds.addAll(Optional.ofNullable(service.getExternalRegistries()).orElseGet(() ->Lists.newArrayList()).stream().map(ExternalRegistryVO::getExternalRegistrySeq).collect(Collectors.toList()));
                    }
                }

                // 기존 리소스 유형별로 Map으로 셋팅
                PlMasterVO detail = plDao.getPlDetail(plSeq);
                Map<PlResType, Set<String>> currResTypeListMap = Maps.newHashMap();
                if (detail != null && CollectionUtils.isNotEmpty(detail.getPlResDeploys())) {
                    for (PlResDeployVO res : detail.getPlResDeploys()) {
                        if (MapUtils.getObject(currResTypeListMap, res.getResType(), null) == null) {
                            currResTypeListMap.put(res.getResType(), Sets.newHashSet());
                        }

                        currResTypeListMap.get(res.getResType()).add(res.getResName());
                    }
                }

                // 리소스 유형별로 Map으로 셋팅
                Map<PlResType, List<PlResDeployVO>> resTypeListMap = Maps.newHashMap();
                for (PlResDeployVO res : plResDeploys) {
                    // 중복 리소스 체크
                    if (MapUtils.isNotEmpty(currResTypeListMap)) {
                        if (currResTypeListMap.containsKey(res.getResType())) {
                            if (currResTypeListMap.get(res.getResType()).contains(res.getResName())) {
                                String errMsg = "This resource is already registered.";
                                throw new CocktailException(errMsg, ExceptionType.InvalidParameter, String.format("%s - [%s: %s]", errMsg, res.getResType().getValue(), res.getResName()));
                            }
                        }
                    }
                    resTypeListMap.putIfAbsent(res.getResType(), Lists.newArrayList());
                    resTypeListMap.get(res.getResType()).add(res);
                }

                for (Map.Entry<PlResType, List<PlResDeployVO>> resTypeListEntry : resTypeListMap.entrySet()) {
                    if (resTypeListEntry.getKey() == PlResType.REPLICA_SERVER) {
                        // k8s 조회
                        List<K8sDeploymentVO> k8sResources = workloadResourceService.getDeployments(cluster, namespace, null, null, ContextHolder.exeContext());
                        Map<String, K8sDeploymentVO> k8sMap = Optional.ofNullable(k8sResources).orElseGet(() ->Lists.newArrayList()).stream().collect(Collectors.toMap(K8sDeploymentVO::getName, Function.identity()));
                        StringBuffer yamlStr;
                        for (PlResDeployVO resDeploy : resTypeListEntry.getValue()) {
                            K8sDeploymentVO currK8sRes = k8sMap.get(resDeploy.getResName());
                            if (currK8sRes != null) {
                                resDeploy.setPlSeq(plSeq);

                                boolean hasHPA = CollectionUtils.isNotEmpty(currK8sRes.getHorizontalPodAutoscalers());

                                /**
                                 * yaml 변환 작업
                                 */
                                yamlStr = new StringBuffer();

                                /** deployment **/
                                // convert from yaml to obj
                                List<Object> obj = ServerUtils.getYamlObjects(currK8sRes.getDeploymentYaml());

                                // get group // version
                                Map<String, Object> k8sYamlToMap = ServerUtils.getK8sYamlToMap(currK8sRes.getDeploymentYaml());
                                K8sApiGroupType k8sGroup = ServerUtils.getK8sGroupInMap(k8sYamlToMap);
                                K8sApiType k8sVer = ServerUtils.getK8sVersionInMap(k8sYamlToMap);

                                // status 항목 제거
                                if (k8sGroup == K8sApiGroupType.APPS && k8sVer == K8sApiType.V1BETA1) {
                                    AppsV1beta1Deployment k8sRes = (AppsV1beta1Deployment)Optional.ofNullable(obj).orElseGet(() ->Lists.newArrayList()).get(0);
                                    if (hasHPA) {
                                        k8sRes.getSpec().setReplicas(null);
                                    }
                                    k8sRes.setStatus(null);
                                    yamlStr.append(ServerUtils.marshalYaml(k8sRes));
                                } else if (k8sGroup == K8sApiGroupType.APPS && k8sVer == K8sApiType.V1BETA2) {
                                    V1beta2Deployment k8sRes = (V1beta2Deployment)Optional.ofNullable(obj).orElseGet(() ->Lists.newArrayList()).get(0);
                                    if (hasHPA) {
                                        k8sRes.getSpec().setReplicas(null);
                                    }
                                    k8sRes.setStatus(null);
                                    yamlStr.append(ServerUtils.marshalYaml(k8sRes));
                                } else {
                                    V1Deployment k8sRes = (V1Deployment)Optional.ofNullable(obj).orElseGet(() ->Lists.newArrayList()).get(0);
                                    if (hasHPA) {
                                        k8sRes.getSpec().setReplicas(null);
                                    }
                                    k8sRes.setStatus(null);
                                    yamlStr.append(ServerUtils.marshalYaml(k8sRes));
                                }
                                /** Hpa **/
                                if (hasHPA) {
                                    this.setHpaYaml(yamlStr, currK8sRes.getHorizontalPodAutoscalers().get(0));
                                }
                                resDeploy.setResCont(yamlStr.toString());

                                // pl_resource 등록
                                plDao.insertPlResDeploy(resDeploy);

                                // 빌드정보 셋팅
                                this.addResBuildWithMapping(resDeploy, projectIds, externalRegistryIds, currK8sRes.getDetail().getPodTemplate().getSpec().getDetail().getInitContainers(), currK8sRes.getDetail().getPodTemplate().getSpec().getDetail().getContainers(), plDao);
                            }
                        }
                    } else if (resTypeListEntry.getKey() == PlResType.STATEFUL_SET_SERVER) {
                        // k8s 조회
                        List<K8sStatefulSetVO> k8sResources = workloadResourceService.getStatefulSets(cluster, namespace, null, null, ContextHolder.exeContext());
                        Map<String, K8sStatefulSetVO> k8sMap = Optional.ofNullable(k8sResources).orElseGet(() ->Lists.newArrayList()).stream().collect(Collectors.toMap(K8sStatefulSetVO::getName, Function.identity()));
                        StringBuffer yamlStr;
                        for (PlResDeployVO resDeploy : resTypeListEntry.getValue()) {
                            K8sStatefulSetVO currK8sRes = k8sMap.get(resDeploy.getResName());
                            if (currK8sRes != null) {
                                resDeploy.setPlSeq(plSeq);

                                boolean hasHPA = CollectionUtils.isNotEmpty(currK8sRes.getHorizontalPodAutoscalers());

                                /**
                                 * yaml 변환 작업
                                 */
                                yamlStr = new StringBuffer();

                                /** statefulSet **/
                                // convert from yaml to obj
                                List<Object> obj = ServerUtils.getYamlObjects(currK8sRes.getDeploymentYaml());

                                // get group // version
                                Map<String, Object> k8sYamlToMap = ServerUtils.getK8sYamlToMap(currK8sRes.getDeploymentYaml());
                                K8sApiGroupType k8sGroup = ServerUtils.getK8sGroupInMap(k8sYamlToMap);
                                K8sApiType k8sVer = ServerUtils.getK8sVersionInMap(k8sYamlToMap);

                                // status 항목 제거
                                if (k8sGroup == K8sApiGroupType.APPS && k8sVer == K8sApiType.V1BETA1) {
                                    V1beta1StatefulSet k8sRes = (V1beta1StatefulSet)Optional.ofNullable(obj).orElseGet(() ->Lists.newArrayList()).get(0);
                                    if (hasHPA) {
                                        k8sRes.getSpec().setReplicas(null);
                                    }
                                    k8sRes.setStatus(null);
                                    yamlStr.append(ServerUtils.marshalYaml(k8sRes));
                                } else if (k8sGroup == K8sApiGroupType.APPS && k8sVer == K8sApiType.V1BETA2) {
                                    V1beta2StatefulSet k8sRes = (V1beta2StatefulSet)Optional.ofNullable(obj).orElseGet(() ->Lists.newArrayList()).get(0);
                                    if (hasHPA) {
                                        k8sRes.getSpec().setReplicas(null);
                                    }
                                    k8sRes.setStatus(null);
                                    yamlStr.append(ServerUtils.marshalYaml(k8sRes));
                                } else {
                                    V1StatefulSet k8sRes = (V1StatefulSet)Optional.ofNullable(obj).orElseGet(() ->Lists.newArrayList()).get(0);
                                    if (hasHPA) {
                                        k8sRes.getSpec().setReplicas(null);
                                    }
                                    k8sRes.setStatus(null);
                                    yamlStr.append(ServerUtils.marshalYaml(k8sRes));
                                }
                                /** Hpa **/
                                if (hasHPA) {
                                    this.setHpaYaml(yamlStr, currK8sRes.getHorizontalPodAutoscalers().get(0));
                                }
                                resDeploy.setResCont(yamlStr.toString());

                                // pl_resource 등록
                                plDao.insertPlResDeploy(resDeploy);

                                // 빌드정보 셋팅
                                this.addResBuildWithMapping(resDeploy, projectIds, externalRegistryIds, currK8sRes.getDetail().getPodTemplate().getSpec().getDetail().getInitContainers(), currK8sRes.getDetail().getPodTemplate().getSpec().getDetail().getContainers(), plDao);
                            }
                        }
                    } else if (resTypeListEntry.getKey() == PlResType.DAEMON_SET_SERVER) {
                        // k8s 조회
                        List<K8sDaemonSetVO> k8sResources = workloadResourceService.getDaemonSets(cluster, namespace, null, null, ContextHolder.exeContext());
                        Map<String, K8sDaemonSetVO> k8sMap = Optional.ofNullable(k8sResources).orElseGet(() ->Lists.newArrayList()).stream().collect(Collectors.toMap(K8sDaemonSetVO::getName, Function.identity()));
                        StringBuffer yamlStr;
                        for (PlResDeployVO resDeploy : resTypeListEntry.getValue()) {
                            K8sDaemonSetVO currK8sRes = k8sMap.get(resDeploy.getResName());
                            if (currK8sRes != null) {
                                resDeploy.setPlSeq(plSeq);

                                /**
                                 * yaml 변환 작업
                                 */
                                yamlStr = new StringBuffer();

                                // convert from yaml to obj
                                List<Object> obj = ServerUtils.getYamlObjects(currK8sRes.getDeploymentYaml());

                                // get group // version
                                Map<String, Object> k8sYamlToMap = ServerUtils.getK8sYamlToMap(currK8sRes.getDeploymentYaml());
                                K8sApiGroupType k8sGroup = ServerUtils.getK8sGroupInMap(k8sYamlToMap);
                                K8sApiType k8sVer = ServerUtils.getK8sVersionInMap(k8sYamlToMap);

                                // status 항목 제거
                                if (k8sGroup == K8sApiGroupType.EXTENSIONS && k8sVer == K8sApiType.V1BETA1) {
                                    V1beta1DaemonSet k8sRes = (V1beta1DaemonSet)Optional.ofNullable(obj).orElseGet(() ->Lists.newArrayList()).get(0);
                                    k8sRes.setStatus(null);
                                    yamlStr.append(ServerUtils.marshalYaml(k8sRes));
                                } else if (k8sGroup == K8sApiGroupType.APPS && k8sVer == K8sApiType.V1BETA2) {
                                    V1beta2DaemonSet k8sRes = (V1beta2DaemonSet)Optional.ofNullable(obj).orElseGet(() ->Lists.newArrayList()).get(0);
                                    k8sRes.setStatus(null);
                                    yamlStr.append(ServerUtils.marshalYaml(k8sRes));
                                } else {
                                    V1DaemonSet k8sRes = (V1DaemonSet)Optional.ofNullable(obj).orElseGet(() ->Lists.newArrayList()).get(0);
                                    k8sRes.setStatus(null);
                                    yamlStr.append(ServerUtils.marshalYaml(k8sRes));
                                }
                                resDeploy.setResCont(yamlStr.toString());

                                // pl_resource 등록
                                plDao.insertPlResDeploy(resDeploy);

                                // 빌드정보 셋팅
                                this.addResBuildWithMapping(resDeploy, projectIds, externalRegistryIds, currK8sRes.getDetail().getPodTemplate().getSpec().getDetail().getInitContainers(), currK8sRes.getDetail().getPodTemplate().getSpec().getDetail().getContainers(), plDao);
                            }
                        }
                    } else if (resTypeListEntry.getKey() == PlResType.JOB_SERVER) {
                        // k8s 조회
                        List<K8sJobVO> k8sResources = workloadResourceService.getJobs(cluster, namespace, null, null, ContextHolder.exeContext());
                        Map<String, K8sJobVO> k8sMap = Optional.ofNullable(k8sResources).orElseGet(() ->Lists.newArrayList()).stream().collect(Collectors.toMap(K8sJobVO::getName, Function.identity()));
                        for (PlResDeployVO resDeploy : resTypeListEntry.getValue()) {
                            K8sJobVO currK8sRes = k8sMap.get(resDeploy.getResName());
                            if (currK8sRes != null) {
                                resDeploy.setPlSeq(plSeq);

                                /**
                                 * yaml 변환 작업
                                 */
                                StringBuffer yamlStr = new StringBuffer();

                                // convert from yaml to obj
                                List<Object> obj = ServerUtils.getYamlObjects(currK8sRes.getDeploymentYaml());

                                // get group // version
                                Map<String, Object> k8sYamlToMap = ServerUtils.getK8sYamlToMap(currK8sRes.getDeploymentYaml());
                                K8sApiGroupType k8sGroup = ServerUtils.getK8sGroupInMap(k8sYamlToMap);
                                K8sApiType k8sVer = ServerUtils.getK8sVersionInMap(k8sYamlToMap);

                                // status 항목 제거
                                if (k8sGroup == K8sApiGroupType.BATCH && k8sVer == K8sApiType.V1) {
                                    V1Job k8sRes = (V1Job)Optional.ofNullable(obj).orElseGet(() ->Lists.newArrayList()).get(0);
                                    k8sRes.setStatus(null);
                                    yamlStr.append(ServerUtils.marshalYaml(k8sRes));
                                }
                                resDeploy.setResCont(yamlStr.toString());

                                // pl_resource 등록
                                plDao.insertPlResDeploy(resDeploy);

                                // 빌드정보 셋팅
                                this.addResBuildWithMapping(resDeploy, projectIds, externalRegistryIds, currK8sRes.getDetail().getPodTemplate().getSpec().getDetail().getInitContainers(), currK8sRes.getDetail().getPodTemplate().getSpec().getDetail().getContainers(), plDao);
                            }
                        }
                    } else if (resTypeListEntry.getKey() == PlResType.CRON_JOB_SERVER) {
                        // k8s 조회
                        List<K8sCronJobVO> k8sResources = workloadResourceService.getCronJobs(cluster, namespace, null, null, ContextHolder.exeContext());
                        Map<String, K8sCronJobVO> k8sMap = Optional.ofNullable(k8sResources).orElseGet(() ->Lists.newArrayList()).stream().collect(Collectors.toMap(K8sCronJobVO::getName, Function.identity()));
                        for (PlResDeployVO resDeploy : resTypeListEntry.getValue()) {
                            K8sCronJobVO currK8sRes = k8sMap.get(resDeploy.getResName());
                            if (currK8sRes != null) {
                                resDeploy.setPlSeq(plSeq);

                                /**
                                 * yaml 변환 작업
                                 */
                                StringBuffer yamlStr = new StringBuffer();

                                // convert from yaml to obj
                                List<Object> obj = ServerUtils.getYamlObjects(currK8sRes.getDeploymentYaml());

                                // get group // version
                                Map<String, Object> k8sYamlToMap = ServerUtils.getK8sYamlToMap(currK8sRes.getDeploymentYaml());
                                K8sApiGroupType k8sGroup = ServerUtils.getK8sGroupInMap(k8sYamlToMap);
                                K8sApiType k8sVer = ServerUtils.getK8sVersionInMap(k8sYamlToMap);

                                // status 항목 제거
                                if (k8sGroup == K8sApiGroupType.BATCH && k8sVer == K8sApiType.V1BETA1) {
                                    V1beta1CronJob k8sRes = (V1beta1CronJob)Optional.ofNullable(obj).orElseGet(() ->Lists.newArrayList()).get(0);
                                    k8sRes.setStatus(null);
                                    yamlStr.append(ServerUtils.marshalYaml(k8sRes));
                                } else if (k8sGroup == K8sApiGroupType.BATCH && k8sVer == K8sApiType.V1) {
                                    V1CronJob k8sRes = (V1CronJob)Optional.ofNullable(obj).orElseGet(() ->Lists.newArrayList()).get(0);
                                    k8sRes.setStatus(null);
                                    yamlStr.append(ServerUtils.marshalYaml(k8sRes));
                                }
                                resDeploy.setResCont(yamlStr.toString());

                                // pl_resource 등록
                                plDao.insertPlResDeploy(resDeploy);

                                // 빌드정보 셋팅
                                this.addResBuildWithMapping(resDeploy, projectIds, externalRegistryIds, currK8sRes.getDetail().getPodTemplate().getSpec().getDetail().getInitContainers(), currK8sRes.getDetail().getPodTemplate().getSpec().getDetail().getContainers(), plDao);
                            }
                        }
                    } else if (resTypeListEntry.getKey() == PlResType.SVC) {
                        // k8s 조회
                        List<K8sServiceVO> k8sResources = serviceSpecService.getServices(cluster, namespace, null, null, ContextHolder.exeContext());
                        Map<String, K8sServiceVO> k8sMap = Optional.ofNullable(k8sResources).orElseGet(() ->Lists.newArrayList()).stream().collect(Collectors.toMap(K8sServiceVO::getServiceName, Function.identity()));
                        for (PlResDeployVO resDeploy : resTypeListEntry.getValue()) {
                            K8sServiceVO currK8sRes = k8sMap.get(resDeploy.getResName());
                            if (currK8sRes != null) {
                                resDeploy.setPlSeq(plSeq);

                                /**
                                 * yaml 변환 작업
                                 */
                                StringBuffer yamlStr = new StringBuffer();

                                // convert from yaml to obj
                                List<Object> obj = ServerUtils.getYamlObjects(currK8sRes.getServiceDeploymentYaml());

                                // status 항목 제거
                                V1Service k8sRes = (V1Service)Optional.ofNullable(obj).orElseGet(() ->Lists.newArrayList()).get(0);
                                k8sRes.setStatus(null);
                                yamlStr.append(ServerUtils.marshalYaml(k8sRes));
                                resDeploy.setResCont(yamlStr.toString());

                                // pl_resource 등록
                                plDao.insertPlResDeploy(resDeploy);
                            }
                        }
                    } else if (resTypeListEntry.getKey() == PlResType.CM) {
                        // k8s 조회
                        List<ConfigMapGuiVO> k8sResources = configMapService.getConfigMaps(cluster, namespace, null, null);
                        Map<String, ConfigMapGuiVO> k8sMap = Optional.ofNullable(k8sResources).orElseGet(() ->Lists.newArrayList()).stream().collect(Collectors.toMap(ConfigMapGuiVO::getName, Function.identity()));
                        for (PlResDeployVO resDeploy : resTypeListEntry.getValue()) {
                            ConfigMapGuiVO currK8sRes = k8sMap.get(resDeploy.getResName());
                            if (currK8sRes != null) {
                                resDeploy.setPlSeq(plSeq);

                                // convert to yaml
                                List<Object> obj = ServerUtils.getYamlObjects(currK8sRes.getDeploymentYaml());
                                resDeploy.setResCont(ServerUtils.marshalYaml(Optional.ofNullable(obj).orElseGet(() ->Lists.newArrayList()).get(0)));

                                // pl_resource 등록
                                plDao.insertPlResDeploy(resDeploy);
                            }
                        }
                    } else if (resTypeListEntry.getKey() == PlResType.SC) {
                        // k8s 조회
                        List<SecretGuiVO> k8sResources = secretService.getSecrets(cluster, namespace, null, null, false);
                        Map<String, SecretGuiVO> k8sMap = Optional.ofNullable(k8sResources).orElseGet(() ->Lists.newArrayList()).stream().collect(Collectors.toMap(SecretGuiVO::getName, Function.identity()));
                        for (PlResDeployVO resDeploy : resTypeListEntry.getValue()) {
                            SecretGuiVO currK8sRes = k8sMap.get(resDeploy.getResName());
                            if (currK8sRes != null) {
                                resDeploy.setPlSeq(plSeq);

                                // convert to yaml
                                List<Object> obj = ServerUtils.getYamlObjects(currK8sRes.getDeploymentYaml());
                                StringBuffer yamlStr = new StringBuffer();
                                V1Secret k8sRes = (V1Secret)Optional.ofNullable(obj).orElseGet(() ->Lists.newArrayList()).get(0);
                                // Yaml 암호화
                                yamlStr.append(ServerUtils.marshalYaml(k8sRes));
                                resDeploy.setResCont(CryptoUtils.encryptAES(yamlStr.toString()));

                                // pl_resource 등록
                                plDao.insertPlResDeploy(resDeploy);
                            }
                        }
                    } else if (resTypeListEntry.getKey() == PlResType.PVC) {
                        // k8s 조회
                        List<K8sPersistentVolumeClaimVO> k8sResources = persistentVolumeService.getPersistentVolumeClaims(cluster, namespace, null, null, ContextHolder.exeContext());
                        Map<String, K8sPersistentVolumeClaimVO> k8sMap = Optional.ofNullable(k8sResources).orElseGet(() ->Lists.newArrayList()).stream().collect(Collectors.toMap(K8sPersistentVolumeClaimVO::getName, Function.identity()));
                        Map<String, K8sPersistentVolumeVO> k8sPvMap = persistentVolumeService.getPersistentVolumesMap(cluster, namespace, null, null, ContextHolder.exeContext());
                        for (PlResDeployVO resDeploy : resTypeListEntry.getValue()) {
                            K8sPersistentVolumeClaimVO currK8sRes = k8sMap.get(resDeploy.getResName());
                            if (currK8sRes != null) {
                                resDeploy.setPlSeq(plSeq);

                                /**
                                 * yaml 변환 작업
                                 */
                                StringBuffer yamlStr = new StringBuffer();

                                // static PVC일 경우 PV도 yaml 저장
                                if (StringUtils.isBlank(currK8sRes.getStorageClassName())) {
                                    K8sPersistentVolumeVO currK8sPvRes = k8sPvMap.get(currK8sRes.getVolumeName());
                                    if (currK8sPvRes != null) {
                                        // convert from yaml to obj
                                        List<Object> pvObj = ServerUtils.getYamlObjects(currK8sPvRes.getDeploymentYaml());
                                        V1PersistentVolume k8sRes = (V1PersistentVolume)Optional.ofNullable(pvObj).orElseGet(() ->Lists.newArrayList()).get(0);
                                        k8sRes.setStatus(null);
                                        yamlStr.append(ServerUtils.marshalYaml(k8sRes));
                                        yamlStr.append("---\n");
                                    }
                                }
                                // convert from yaml to obj
                                List<Object> obj = ServerUtils.getYamlObjects(currK8sRes.getDeploymentYaml());
                                V1PersistentVolumeClaim k8sRes = (V1PersistentVolumeClaim)Optional.ofNullable(obj).orElseGet(() ->Lists.newArrayList()).get(0);
                                k8sRes.setStatus(null);
                                yamlStr.append(ServerUtils.marshalYaml(k8sRes));
                                resDeploy.setResCont(yamlStr.toString());
                                resDeploy.setRunYn("N");

                                // pl_resource 등록
                                plDao.insertPlResDeploy(resDeploy);
                            }
                        }
                    } else if (resTypeListEntry.getKey() == PlResType.IG) {
                        // k8s 조회
                        List<K8sIngressVO> k8sResources = ingressSpecService.getIngresses(cluster, namespace, ContextHolder.exeContext());
                        Map<String, K8sIngressVO> k8sMap = Optional.ofNullable(k8sResources).orElseGet(() ->Lists.newArrayList()).stream().collect(Collectors.toMap(K8sIngressVO::getName, Function.identity()));
                        for (PlResDeployVO resDeploy : resTypeListEntry.getValue()) {
                            K8sIngressVO currK8sRes = k8sMap.get(resDeploy.getResName());
                            if (currK8sRes != null) {
                                resDeploy.setPlSeq(plSeq);

                                /**
                                 * yaml 변환 작업
                                 */
                                StringBuffer yamlStr = new StringBuffer();

                                // convert from yaml to obj
                                List<Object> obj = ServerUtils.getYamlObjects(currK8sRes.getDeploymentYaml());

                                // get group // version
                                Map<String, Object> k8sYamlToMap = ServerUtils.getK8sYamlToMap(currK8sRes.getDeploymentYaml());
                                K8sApiGroupType k8sGroup = ServerUtils.getK8sGroupInMap(k8sYamlToMap);
                                K8sApiType k8sVer = ServerUtils.getK8sVersionInMap(k8sYamlToMap);

                                // status 항목 제거
                                if (k8sGroup == K8sApiGroupType.NETWORKING && k8sVer == K8sApiType.V1) {
                                    V1Ingress k8sRes = (V1Ingress)Optional.ofNullable(obj).orElseGet(() ->Lists.newArrayList()).get(0);
                                    k8sRes.setStatus(null);
                                    yamlStr.append(ServerUtils.marshalYaml(k8sRes));
                                } else if (k8sGroup == K8sApiGroupType.NETWORKING && k8sVer == K8sApiType.V1BETA1) {
                                    NetworkingV1beta1Ingress k8sRes = (NetworkingV1beta1Ingress)Optional.ofNullable(obj).orElseGet(() ->Lists.newArrayList()).get(0);
                                    k8sRes.setStatus(null);
                                    yamlStr.append(ServerUtils.marshalYaml(k8sRes));
                                } else {
                                    ExtensionsV1beta1Ingress k8sRes = (ExtensionsV1beta1Ingress)Optional.ofNullable(obj).orElseGet(() ->Lists.newArrayList()).get(0);
                                    k8sRes.setStatus(null);
                                    yamlStr.append(ServerUtils.marshalYaml(k8sRes));
                                }
                                resDeploy.setResCont(yamlStr.toString());

                                // pl_resource 등록
                                plDao.insertPlResDeploy(resDeploy);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 빌드 정보들 등록
     *
     * @param plSeq
     * @param addResBuilds 빌드 정보들
     * @throws Exception
     */
    @Transactional(transactionManager = "transactionManager")
    public void addPlResBuilds(Integer plSeq, List<PlResBuildVO> addResBuilds) throws Exception {
        IPlMapper dao = sqlSession.getMapper(IPlMapper.class);

        BuildRunVO buildRun = null;
        if (addResBuilds != null && addResBuilds.size() > 0) {
            for (PlResBuildVO addResBuild : addResBuilds) {

                // step 정보 convert 하지 않는 DB 정보만 조회 한다.
                buildRun = buildService.getBuildRun(addResBuild.getBuildRunSeq(), "Y", false);

                if(buildRun != null) {
                    PlResBuildVO plResBuild = new PlResBuildVO();
                    plResBuild.setPlSeq(plSeq);
                    plResBuild.setBuildSeq(addResBuild.getBuildSeq());
                    plResBuild.setBuildRunSeq(addResBuild.getBuildRunSeq());
                    plResBuild.setRunOrder(addResBuild.getRunOrder());
                    plResBuild.setRunYn("N");
                    plResBuild.setImgUrl(buildRun.getImageUrl());
                    plResBuild.setBuildTag(buildRun.getTagName());
                    plResBuild.setBuildCont(JsonUtils.toGson(buildRun));

                    // 빌드 리소스 정보 등록
                    dao.insertPlResBuild(plResBuild);
                }
            }
        }
    }

    /**
     * 파이프라인 워크로드 리소스의 컨테이너별 빌드 정보 생성
     *
     * @param resDeploy
     * @param projectIds
     * @param initContainers
     * @param containers
     * @param plDao
     * @throws Exception
     */
    @Transactional(transactionManager = "transactionManager")
    public void addResBuildWithMapping(PlResDeployVO resDeploy, List<Integer> projectIds, List<Integer> externalRegistryIds, List<K8sContainerVO> initContainers, List<K8sContainerVO> containers, IPlMapper plDao) throws Exception {

        if (plDao == null) {
            plDao = sqlSession.getMapper(IPlMapper.class);
        }

        // 기존 빌드정보 조회
        PlMasterVO detail = plDao.getPlDetail(resDeploy.getPlSeq());
        List<PlResBuildVO> builds = Optional.ofNullable(detail.getPlResBuilds()).orElseGet(() ->Lists.newArrayList());
        Map<Integer, PlResBuildVO> buildMap = Maps.newHashMap();
        for (PlResBuildVO prb : builds) {
            buildMap.putIfAbsent(prb.getBuildRunSeq(), prb);
        }
        // 신규 빌드정보 처리용
        Map<Integer, PlResBuildVO> newBuildMap = Maps.newHashMap();
        Map<Integer, List<PlResBuildDeployMappingVO>> newBuildMappingMap = Maps.newHashMap();

        List<K8sContainerVO> allContainers = Lists.newArrayList();
        ResourceUtil.mergeK8sContainer(allContainers, initContainers, containers);

        for (K8sContainerVO k8sContainer : allContainers) {
            if (k8sContainer != null) {
                BuildRunVO buildRun = buildService.getBuildRunsByImageUrl(null, k8sContainer.getImage());

                // registry private 여부
                boolean isPrivate = false;
                if( buildRun != null){
                    // 해당 이미지의 registry가 워크스페이스에 속해 있는 지 확인하여 있다면 build와 연결 시킴
                    // 내부 레지스트리
                    if (CollectionUtils.isNotEmpty(projectIds)) {
                        if (buildRun.getRegistryProjectId() != null && projectIds.contains(buildRun.getRegistryProjectId())) {
                            isPrivate = true;
                        }
                    }
                    // 외부 레지스트리
                    if (!isPrivate && CollectionUtils.isNotEmpty(externalRegistryIds)) {
                        if (buildRun.getExternalRegistrySeq() != null && externalRegistryIds.contains(buildRun.getExternalRegistrySeq())) {
                            isPrivate = true;
                        }
                    }
                }

                if (isPrivate) {
                    // buildRunSeq 기준으로 판단
                    // 빌드정보가 기존에 등록되어 있지 않은 경우
                    if (!buildMap.containsKey(buildRun.getBuildRunSeq())) {
                        // step 정보 convert 하지 않는 DB 정보만 조회 한다.
                        buildRun = buildService.getBuildRun(buildRun.getBuildRunSeq(), "Y", false);

                        if (!newBuildMap.containsKey(buildRun.getBuildRunSeq())) {
                            // 새로운 빌드정보 셋팅
                            PlResBuildVO plResBuild = new PlResBuildVO();
                            plResBuild.setPlSeq(resDeploy.getPlSeq());
                            plResBuild.setImgUrl(k8sContainer.getImage());
                            plResBuild.setBuildSeq(buildRun.getBuildSeq());
                            plResBuild.setBuildRunSeq(buildRun.getBuildRunSeq());
                            plResBuild.setBuildCont(JsonUtils.toGson(buildRun));
                            plResBuild.setBuildTag(buildRun.getTagName());
                            plResBuild.setRunYn("N");

                            newBuildMap.put(buildRun.getBuildRunSeq(), plResBuild);
                        }

                        // 새로운 맵핑정보 셋팅
                        PlResBuildDeployMappingVO mapping = new PlResBuildDeployMappingVO();
                        mapping.setPlResDeploySeq(resDeploy.getPlResDeploySeq());
                        mapping.setResType(resDeploy.getResType().getCode());
                        mapping.setResName(resDeploy.getResName());
                        mapping.setContainerName(k8sContainer.getName());

                        if (MapUtils.getObject(newBuildMappingMap, buildRun.getBuildRunSeq(), null) == null) {
                            newBuildMappingMap.put(buildRun.getBuildRunSeq(), Lists.newArrayList());
                        }
                        newBuildMappingMap.get(buildRun.getBuildRunSeq()).add(mapping);
                    }
                    // 빌드정보가 등록되어 있다면 해당 빌드정보로 맵핑처리
                    else {
                        PlResBuildDeployMappingVO mapping = new PlResBuildDeployMappingVO();
                        mapping.setPlResBuildSeq(buildMap.get(buildRun.getBuildRunSeq()).getPlResBuildSeq());
                        mapping.setPlResDeploySeq(resDeploy.getPlResDeploySeq());
                        mapping.setResType(resDeploy.getResType().getCode());
                        mapping.setResName(resDeploy.getResName());
                        mapping.setContainerName(k8sContainer.getName());

                        // 빌드 배포 맵핑 정보 등록
                        plDao.insertPlResBuildDeployMapping(mapping);
                    }

                }
            }
        }

        // 신규 빌드 정보 처리
        if (MapUtils.isNotEmpty(newBuildMap)) {
            for (Map.Entry<Integer, PlResBuildVO> buildEntry : newBuildMap.entrySet()) {
                // 빌드 리소스 정보 등록
                plDao.insertPlResBuild(buildEntry.getValue());

                if (newBuildMappingMap.containsKey(buildEntry.getKey())) {
                    List<PlResBuildDeployMappingVO> mappings = newBuildMappingMap.get(buildEntry.getKey());
                    for (PlResBuildDeployMappingVO mappingRow : mappings) {
                        mappingRow.setPlResBuildSeq(buildEntry.getValue().getPlResBuildSeq());

                        // 빌드 배포 맵핑 정보 등록
                        plDao.insertPlResBuildDeployMapping(mappingRow);
                    }
                }
            }
        }
    }

    /**
     * set HorizontalPodAutoscaler yaml
     *
     * @param yamlStr
     * @param k8sHorizontalPodAutoscaler
     * @throws Exception
     */
    private void setHpaYaml(StringBuffer yamlStr, K8sHorizontalPodAutoscalerVO k8sHorizontalPodAutoscaler) throws Exception {
        // convert from yaml to obj
        List<Object> hpsObj = ServerUtils.getYamlObjects(k8sHorizontalPodAutoscaler.getDeploymentYaml());
        // get group // version
        Map<String, Object> k8sHpaYamlToMap = ServerUtils.getK8sYamlToMap(k8sHorizontalPodAutoscaler.getDeploymentYaml());
        K8sApiGroupType k8sHpaGroup = ServerUtils.getK8sGroupInMap(k8sHpaYamlToMap);
        K8sApiType k8sHpaVer = ServerUtils.getK8sVersionInMap(k8sHpaYamlToMap);

        // status 항목 제거
        if (k8sHpaGroup == K8sApiGroupType.AUTOSCALING && k8sHpaVer == K8sApiType.V1) {
            V1HorizontalPodAutoscaler k8sRes = (V1HorizontalPodAutoscaler)Optional.ofNullable(hpsObj).orElseGet(() ->Lists.newArrayList()).get(0);
            k8sRes.setStatus(null);
            yamlStr.append("---\n");
            yamlStr.append(ServerUtils.marshalYaml(k8sRes));
        } else if (k8sHpaGroup == K8sApiGroupType.AUTOSCALING && k8sHpaVer == K8sApiType.V2BETA1) {
            V2beta1HorizontalPodAutoscaler k8sRes = (V2beta1HorizontalPodAutoscaler)Optional.ofNullable(hpsObj).orElseGet(() ->Lists.newArrayList()).get(0);
            k8sRes.setStatus(null);
            yamlStr.append("---\n");
            yamlStr.append(ServerUtils.marshalYaml(k8sRes));
        } else if (k8sHpaGroup == K8sApiGroupType.AUTOSCALING && k8sHpaVer == K8sApiType.V2BETA2) {
            V2beta2HorizontalPodAutoscaler k8sRes = (V2beta2HorizontalPodAutoscaler)Optional.ofNullable(hpsObj).orElseGet(() ->Lists.newArrayList()).get(0);
            k8sRes.setStatus(null);
            yamlStr.append("---\n");
            yamlStr.append(ServerUtils.marshalYaml(k8sRes));
        } else if (k8sHpaGroup == K8sApiGroupType.AUTOSCALING && k8sHpaVer == K8sApiType.V2) {
            V2HorizontalPodAutoscaler k8sRes = (V2HorizontalPodAutoscaler)Optional.ofNullable(hpsObj).orElseGet(() ->Lists.newArrayList()).get(0);
            k8sRes.setStatus(null);
            yamlStr.append("---\n");
            yamlStr.append(ServerUtils.marshalYaml(k8sRes));
        }
    }

    /**
     * 파이프라인 워크로드 리소스 상세 조회
     *
     * @param plSeq
     * @param plResDeploySeq
     * @param deployType
     * @return
     * @throws Exception
     */
    public ServerIntegrateVO getWorkloadDeployResourceDetail(Integer plSeq, Integer plResDeploySeq, String deployType) throws Exception {

        IPlMapper plDao = sqlSession.getMapper(IPlMapper.class);
        IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);

        PlResDeployVO plResource = plDao.getPlResDeploy(plSeq, plResDeploySeq);
        PlMasterVO plMaster = plDao.getPlMaster(plSeq);
        ClusterVO cluster = clusterDao.getCluster(plMaster.getClusterSeq());

        ServerIntegrateVO serverIntegrate = new ServerIntegrateVO();
        serverIntegrate.setDeployType(deployType);

        if (plResource != null) {
            WorkloadType workloadType = WorkloadType.valueOf(plResource.getResType().getCode());
            List<Object> objs = ServerUtils.getYamlObjects(plResource.getResCont());

            WorkloadVersion workloadVersion = WorkloadVersion.V1;

            // annotation에 설정된 설명을 워크로드별로 파싱하여 가져옴.
            String workloadDesc = null;
            Map<String, String> annotations = Maps.newHashMap();
            for (Object obj : objs) {
                Map<String, Object> k8sObjectToMap = ServerUtils.getK8sObjectToMap(obj, null);
                K8sApiKindType kind = ServerUtils.getK8sKindInMap(k8sObjectToMap);
                K8sApiGroupType apiGroupType = ServerUtils.getK8sGroupInMap(k8sObjectToMap);
                K8sApiType apiType = ServerUtils.getK8sVersionInMap(k8sObjectToMap);

                switch (kind) {
                    case DEPLOYMENT:
                        if (apiGroupType == K8sApiGroupType.APPS && apiType == K8sApiType.V1) {
                            V1Deployment deployment = (V1Deployment) obj;
                            annotations = Optional.ofNullable(deployment).map(V1Deployment::getMetadata).map(V1ObjectMeta::getAnnotations).orElseGet(() ->Maps.newHashMap());
                        }
                        break;
                    case STATEFUL_SET:
                        if (apiGroupType == K8sApiGroupType.APPS && apiType == K8sApiType.V1) {
                            V1StatefulSet statefulSet = (V1StatefulSet) obj;
                            annotations = Optional.ofNullable(statefulSet).map(V1StatefulSet::getMetadata).map(V1ObjectMeta::getAnnotations).orElseGet(() ->Maps.newHashMap());
                        }
                        break;
                    case DAEMON_SET:
                        if (apiGroupType == K8sApiGroupType.APPS && apiType == K8sApiType.V1) {
                            V1DaemonSet daemonSet = (V1DaemonSet) obj;
                            annotations = Optional.ofNullable(daemonSet).map(V1DaemonSet::getMetadata).map(V1ObjectMeta::getAnnotations).orElseGet(() ->Maps.newHashMap());
                        }
                        break;
                    case JOB:
                        if (apiGroupType == K8sApiGroupType.BATCH && apiType == K8sApiType.V1) {
                            V1Job job = (V1Job) obj;
                            annotations = Optional.ofNullable(job).map(V1Job::getMetadata).map(V1ObjectMeta::getAnnotations).orElseGet(() ->Maps.newHashMap());
                        }
                        break;
                    case CRON_JOB:
                        if (apiGroupType == K8sApiGroupType.BATCH && apiType == K8sApiType.V1BETA1) {
                            V1beta1CronJob cronJob = (V1beta1CronJob) obj;
                            annotations = Optional.ofNullable(cronJob).map(V1beta1CronJob::getMetadata).map(V1ObjectMeta::getAnnotations).orElseGet(() ->Maps.newHashMap());
                        } else if (apiGroupType == K8sApiGroupType.BATCH && apiType == K8sApiType.V1) {
                            V1CronJob cronJob = (V1CronJob) obj;
                            annotations = Optional.ofNullable(cronJob).map(V1CronJob::getMetadata).map(V1ObjectMeta::getAnnotations).orElseGet(() ->Maps.newHashMap());
                        }
                        break;
                }

                if (MapUtils.isNotEmpty(annotations)) {
                    workloadDesc = new String(Base64Utils.decodeFromString(MapUtils.getString(annotations, KubeConstants.ANNOTATION_COCKTAIL_USER_DESCRIPTION, "")), StandardCharsets.UTF_8);
                    break;
                }
            }

            if (DeployType.valueOf(deployType) == DeployType.GUI) {
                ServerGuiVO serverGui = serverConversionService.convertYamlToGui(cluster, plResource.getNamespace(), workloadType.getCode(), workloadVersion.getCode(), workloadDesc, null, objs);

                // set buildSeq
                if (CollectionUtils.isNotEmpty(plResource.getPlResBuilds())) {
                    // Map<containerName, buildSeq>
                    Map<String, Integer> buildInfoVOMap = Maps.newHashMap();
                    for (PlResBuildVO resBuild : plResource.getPlResBuilds()) {
                        if (CollectionUtils.isNotEmpty(resBuild.getBuildDeployMapping())) {
                            for (PlResBuildDeployMappingVO mapping : resBuild.getBuildDeployMapping()) {
                                buildInfoVOMap.put(mapping.getContainerName(), resBuild.getBuildSeq());
                            }
                        }
                    }

                    List<ContainerVO> allContainers = Lists.newArrayList();
                    ResourceUtil.mergeContainer(allContainers, serverGui.getInitContainers(), serverGui.getContainers());

                    for (ContainerVO cRow : allContainers) {
                        cRow.setBuildSeq(buildInfoVOMap.get(cRow.getContainerName()));
                    }
                }

                serverIntegrate = serverGui;
            } else {
                ServerYamlVO serverYaml = new ServerYamlVO();
                serverYaml.setDeployType(DeployType.YAML.getCode());
                serverYaml.setWorkloadType(workloadType.getCode());
                serverYaml.setWorkloadVersion(workloadVersion.getCode());
                serverYaml.setClusterSeq(cluster.getClusterSeq());
                serverYaml.setNamespaceName(plResource.getNamespace());
                serverYaml.setWorkloadName(plResource.getResName());
                serverYaml.setDescription(workloadDesc);
                serverYaml.setYaml(plResource.getResCont());

                serverIntegrate = serverYaml;
            }
        } else {
            // throw exception
            this.throwPipelineResNotFound("workload");
        }

        return serverIntegrate;
    }

    /**
     * 파이프라인 컨피그맵 리소스 상세 조회
     *
     * @param plSeq
     * @param plResDeploySeq
     * @return
     * @throws Exception
     */
    public ConfigMapGuiVO getConfigMapDeployResourceDetail(Integer plSeq, Integer plResDeploySeq) throws Exception {
        IPlMapper plDao = sqlSession.getMapper(IPlMapper.class);

        PlResDeployVO plResource = plDao.getPlResDeploy(plSeq, plResDeploySeq);

        ConfigMapGuiVO gui = new ConfigMapGuiVO();
        gui.setDeployType(DeployType.GUI.getCode());

        if (plResource != null && plResource.getResType() == PlResType.CM) {
            List<Object> obj = ServerUtils.getYamlObjects(plResource.getResCont());
            V1ConfigMap k8sRes = (V1ConfigMap)Optional.ofNullable(obj).orElseGet(() ->Lists.newArrayList()).get(0);
            gui = configMapService.convertConfigMapData(k8sRes);
        } else {
            // throw exception
            this.throwPipelineResNotFound(PlResType.CM.getValue());
        }

        return gui;
    }

    /**
     * 파이프라인 시크릿 리소스 상세 조회
     *
     * @param plSeq
     * @param plResDeploySeq
     * @return
     * @throws Exception
     */
    public SecretGuiVO getSecretDeployResourceDetail(Integer plSeq, Integer plResDeploySeq) throws Exception {
        IPlMapper plDao = sqlSession.getMapper(IPlMapper.class);

        PlResDeployVO plResource = plDao.getPlResDeploy(plSeq, plResDeploySeq);

        SecretGuiVO gui = new SecretGuiVO();
        gui.setDeployType(DeployType.GUI.getCode());

        if (plResource != null && plResource.getResType() == PlResType.SC) {
            // Yaml 복호화
            List<Object> obj = ServerUtils.getYamlObjects(CryptoUtils.decryptAES(plResource.getResCont()));
            V1Secret k8sRes = (V1Secret)Optional.ofNullable(obj).orElseGet(() ->Lists.newArrayList()).get(0);
            gui = secretService.convertSecretData(k8sRes);
        } else {
            // throw exception
            this.throwPipelineResNotFound(PlResType.SC.getValue());
        }

        return gui;
    }

    /**
     * 파이프라인 서비스 리소스 상세 조회
     *
     * @param plSeq
     * @param plResDeploySeq
     * @return
     * @throws Exception
     */
    public K8sServiceVO getServiceDeployResourceDetail(Integer plSeq, Integer plResDeploySeq) throws Exception {
        IPlMapper plDao = sqlSession.getMapper(IPlMapper.class);

        PlResDeployVO plResource = plDao.getPlResDeploy(plSeq, plResDeploySeq);

        K8sServiceVO k8sService = null;

        if (plResource != null && plResource.getResType() == PlResType.SVC) {
            k8sService = this.convertServiceYamlToK8sObj(plResource.getResCont());
        } else {
            // throw exception
            this.throwPipelineResNotFound(PlResType.SVC.getValue());
        }

        return k8sService;
    }

    public K8sServiceVO convertServiceYamlToK8sObj(String resCont) throws Exception {
        K8sServiceVO k8sService = null;

        if (StringUtils.isNotBlank(resCont)) {
            List<Object> obj = ServerUtils.getYamlObjects(resCont);
            V1Service k8sRes = (V1Service)Optional.ofNullable(obj).orElseGet(() ->Lists.newArrayList()).get(0);
            return serviceSpecService.genServiceData(null, null, k8sRes, null);
        }

        return k8sService;
    }

    /**
     * 파이프라인 인그레스 리소스 상세 조회
     *
     * @param plSeq
     * @param plResDeploySeq
     * @return
     * @throws Exception
     */
    public K8sIngressVO getIngressDeployResourceDetail(Integer plSeq, Integer plResDeploySeq) throws Exception {
        IPlMapper plDao = sqlSession.getMapper(IPlMapper.class);

        PlResDeployVO plResource = plDao.getPlResDeploy(plSeq, plResDeploySeq);

        K8sIngressVO k8sIngress = new K8sIngressVO();

        if (plResource != null && plResource.getResType() == PlResType.IG) {
            List<Object> obj = ServerUtils.getYamlObjects(plResource.getResCont());

            // get group // version
            Map<String, Object> k8sYamlToMap = ServerUtils.getK8sYamlToMap(plResource.getResCont());
            K8sApiGroupType k8sGroup = ServerUtils.getK8sGroupInMap(k8sYamlToMap);
            K8sApiType k8sVer = ServerUtils.getK8sVersionInMap(k8sYamlToMap);

            // status 항목 제거
            if (k8sGroup == K8sApiGroupType.NETWORKING && k8sVer == K8sApiType.V1) {
                V1Ingress k8sRes = (V1Ingress)Optional.ofNullable(obj).orElseGet(() ->Lists.newArrayList()).get(0);
                k8sIngress = ingressSpecService.convertIngressData(new K8sIngressVO(), k8sRes, new JSON());
            } else if (k8sGroup == K8sApiGroupType.NETWORKING && k8sVer == K8sApiType.V1BETA1) {
                NetworkingV1beta1Ingress k8sRes = (NetworkingV1beta1Ingress)Optional.ofNullable(obj).orElseGet(() ->Lists.newArrayList()).get(0);
                k8sIngress = ingressSpecService.convertIngressData(new K8sIngressVO(), k8sRes, new JSON());
            }
        } else {
            // throw exception
            this.throwPipelineResNotFound(PlResType.IG.getValue());
        }

        return k8sIngress;
    }

    /**
     * 파이프라인 pvc 리소스 상세 조회
     *
     * @param plSeq
     * @param plResDeploySeq
     * @return
     * @throws Exception
     */
    public K8sPersistentVolumeClaimVO getPvcDeployResourceDetail(Integer plSeq, Integer plResDeploySeq) throws Exception {
        IPlMapper plDao = sqlSession.getMapper(IPlMapper.class);
        IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);

        PlResDeployVO plResource = plDao.getPlResDeploy(plSeq, plResDeploySeq);
        PlMasterVO plMaster = plDao.getPlMaster(plSeq);
        ClusterVO cluster = clusterDao.getCluster(plMaster.getClusterSeq());
        List<ClusterVolumeVO> clusterVolumes = clusterVolumeService.getStorageVolumes(null, null, cluster.getClusterSeq(), null, null, false, false);

        K8sPersistentVolumeClaimVO k8sPersistentVolumeClaim = new K8sPersistentVolumeClaimVO();
        Map<String, K8sPersistentVolumeVO> persistentVolumesMap = new HashMap<>();
        JSON k8sJson = new JSON();

        if (plResource != null && plResource.getResType() == PlResType.PVC) {
            List<Object> objs = ServerUtils.getYamlObjects(plResource.getResCont());

            for (Object obj : objs) {
                // get group // version // kind
                Map<String, Object> k8sObjectToMap = ServerUtils.getK8sObjectToMap(obj, k8sJson);
                K8sApiKindType k8sKind = ServerUtils.getK8sKindInMap(k8sObjectToMap);

                if (k8sKind == K8sApiKindType.PERSISTENT_VOLUME) {
                    V1PersistentVolume k8sRes = (V1PersistentVolume)obj;
                    persistentVolumeService.genPersistentVolumesData(null, persistentVolumesMap, Collections.singletonList(k8sRes), plMaster.getNamespace());
                } else {
                    V1PersistentVolumeClaim k8sRes = (V1PersistentVolumeClaim)obj;
                    List<K8sPersistentVolumeClaimVO> pvcs = new ArrayList<>();
                    persistentVolumeService.genPersistentVolumeClaimData(cluster, pvcs, null, Collections.singletonList(k8sRes));
                    k8sPersistentVolumeClaim = pvcs.get(0);
                }
            }

            persistentVolumeService.setPersistentVolumeClaims(
                    cluster,
                    k8sPersistentVolumeClaim, persistentVolumesMap,
                    null, null,
                    clusterVolumes,
                    null, null, null
            );
        } else {
            // throw exception
            this.throwPipelineResNotFound(PlResType.PVC.getValue());
        }

        return k8sPersistentVolumeClaim;
    }

    /**
     * K8s로 부터 파이프라인 리소스 배포 현황 데이터를 조회.
     * @return
     */
    public CurrentDeployVO getCurrentResources(Integer plSeq) throws Exception {


        PlMasterVO plMaster = getPlDetail(plSeq);
        CurrentDeployVO currentResources = new CurrentDeployVO();

        // 기초 데이터 셋팅
        currentResources.setPlSeq(plSeq);
        currentResources.setClusterSeq(plMaster.getClusterSeq());
        currentResources.setName(plMaster.getName());
        currentResources.setNamespace(plMaster.getNamespace());

        IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
        ClusterVO cluster = clusterDao.getCluster(currentResources.getClusterSeq());
        if (cluster == null) {
            throw new CocktailException("Fail to get cluster information", ExceptionType.ClusterNotFound);
        } else {
            cluster.setNamespaceName(currentResources.getNamespace());
        }


        /**
         * Define the Response Objects.
         */
        K8sControllersVO k8sControllers = new K8sControllersVO();
        List<K8sServiceVO> services = new ArrayList<>();
        List<K8sIngressVO> ingresses = new ArrayList<>();
        List<ConfigMapGuiVO> configMaps = new ArrayList<>();
        List<SecretGuiVO> secrets = new ArrayList<>();
        List<K8sPersistentVolumeClaimVO> persistentVolumeClaims = new ArrayList<>();
        Map<String, K8sPersistentVolumeVO> persistentVolumesMap = null;
        Map<String, K8sStorageClassVO> storageClassMap = null;

        List<K8sEventVO> events;
        List<K8sPodVO> pods = new ArrayList<>();
        List<K8sPodVO> allPods;
        Map<String, List<K8sPodVO>> podMap = new HashMap<>();

        String field = null;
        String label = null;

        /** Event List 조회 **/
        events = Optional.ofNullable(k8sResourceService.getEventByCluster(cluster, plMaster.getNamespace(), field, label, ContextHolder.exeContext())).orElseGet(() ->new ArrayList<>());
        Map<String, Map<String, List<K8sEventVO>>> eventMap = new HashMap<>();
        for (K8sEventVO eventRow : events) {
            if (!eventMap.containsKey(eventRow.getKind())) {
                eventMap.put(eventRow.getKind(), Maps.newHashMap());
            }
            if (!eventMap.get(eventRow.getKind()).containsKey(eventRow.getName())) {
                eventMap.get(eventRow.getKind()).put(eventRow.getName(), Lists.newArrayList());
            }
            eventMap.get(eventRow.getKind()).get(eventRow.getName()).add(eventRow);
        }

        /** Pod List 조회 **/
        allPods = workloadResourceService.getPods(cluster, null, currentResources.getNamespace(), null, ContextHolder.exeContext());
        addonCommonService.getPodToMap(allPods, podMap, MapUtils.getObject(eventMap, K8sApiKindType.POD.getValue(), Maps.newHashMap()));


        // k8s 조회할 리소스 리스트
        List<PlResDeployVO> resources = Optional.ofNullable(plMaster.getPlResDeploys()).orElseGet(() ->Lists.newArrayList());

        Map<PlResType, Map<String, Object>> nsOjbects = new HashMap<PlResType, Map<String, Object>>();

        /** 존재하는 리소스 타입의 리스트 추출 **/
        for (PlResDeployVO res : resources) {
            switch (res.getResType()) {

                case REPLICA_SERVER: {
                    if (!nsOjbects.containsKey(res.getResType())) {
                        /** 1. Get Deployments. **/
                        List<K8sDeploymentVO> k8sDeployments = workloadResourceService.getDeployments(cluster, currentResources.getNamespace(), field, label, ContextHolder.exeContext());
                        nsOjbects.put(res.getResType(), k8sDeployments.stream().collect(Collectors.toMap(K8sDeploymentVO::getName, Function.identity())));
                    }
                    break;
                }
                case STATEFUL_SET_SERVER: {
                    if (!nsOjbects.containsKey(res.getResType())) {
                        /** 1. Get StatefulSets **/
                        List<K8sStatefulSetVO> k8sStatefulSets = workloadResourceService.getStatefulSets(cluster, currentResources.getNamespace(), field, label, ContextHolder.exeContext());
                        nsOjbects.put(res.getResType(), k8sStatefulSets.stream().collect(Collectors.toMap(K8sStatefulSetVO::getName, Function.identity())));
                    }
                    break;
                }
                case DAEMON_SET_SERVER: {
                    if (!nsOjbects.containsKey(res.getResType())) {
                        /** 1. Get DaemonSets **/
                        List<K8sDaemonSetVO> k8sDaemonSets = workloadResourceService.getDaemonSets(cluster, currentResources.getNamespace(), field, label, ContextHolder.exeContext());
                        nsOjbects.put(res.getResType(), k8sDaemonSets.stream().collect(Collectors.toMap(K8sDaemonSetVO::getName, Function.identity())));
                    }
                    break;
                }
                case CRON_JOB_SERVER: {
                    if (!nsOjbects.containsKey(res.getResType())) {
                        /** 1. Get CronJobs **/
                        List<K8sCronJobVO> k8sCronJobs = workloadResourceService.getCronJobs(cluster, currentResources.getNamespace(), field, label, ContextHolder.exeContext());
                        nsOjbects.put(res.getResType(), k8sCronJobs.stream().collect(Collectors.toMap(K8sCronJobVO::getName, Function.identity())));
                    }
                    break;
                }
                case JOB_SERVER: {
                    if (!nsOjbects.containsKey(res.getResType())) {
                        /** 1. Get Jobs **/
                        List<K8sJobVO> k8sJobs = workloadResourceService.getJobs(cluster, currentResources.getNamespace(), field, label, ContextHolder.exeContext());
                        nsOjbects.put(res.getResType(), k8sJobs.stream().collect(Collectors.toMap(K8sJobVO::getName, Function.identity())));
                    }
                    break;
                }
                case SVC: {
                    if (!nsOjbects.containsKey(res.getResType())) {
                        /** 1. Get Services **/
                        List<K8sServiceVO> svcList = serviceSpecService.getServices(cluster, currentResources.getNamespace(), field, label, ContextHolder.exeContext());
                        nsOjbects.put(res.getResType(), svcList.stream().collect(Collectors.toMap(K8sServiceVO::getServiceName, Function.identity())));
                    }
                    break;
                }
                case IG: {
                    if (!nsOjbects.containsKey(res.getResType())) {
                        /** 1. Get Ingresses **/
                        List<K8sIngressVO> ingressList = ingressSpecService.getIngresses(cluster, currentResources.getNamespace(), ContextHolder.exeContext());
                        nsOjbects.put(res.getResType(), ingressList.stream().collect(Collectors.toMap(K8sIngressVO::getName, Function.identity())));
                    }
                    break;
                }
                case CM: {
                    if (!nsOjbects.containsKey(res.getResType())) {
                        /** 1. Get ConfigMaps **/
                        List<ConfigMapGuiVO> configMapList = configMapService.getConfigMaps(cluster, currentResources.getNamespace(), field, label);
                        nsOjbects.put(res.getResType(), configMapList.stream().collect(Collectors.toMap(ConfigMapGuiVO::getName, Function.identity())));
                    }
                    break;
                }
                case SC: {
                    if (!nsOjbects.containsKey(res.getResType())) {
                        /** 1. Get Secrets **/
                        List<SecretGuiVO> secretList = secretService.getSecrets(cluster, currentResources.getNamespace(), field, label, true);
                        nsOjbects.put(res.getResType(), secretList.stream().collect(Collectors.toMap(SecretGuiVO::getName, Function.identity())));
                    }
                    break;
                }
                case PVC: {
                    if (!nsOjbects.containsKey(res.getResType())) {
                        List<K8sPersistentVolumeClaimVO> pvcList = this.genPersistentVolumeClaims(cluster, currentResources.getNamespace(), field, label, persistentVolumesMap, storageClassMap);
                        nsOjbects.put(res.getResType(), pvcList.stream().collect(Collectors.toMap(K8sPersistentVolumeClaimVO::getName, Function.identity())));
                    }
                    break;
                }
                default: {
                    /** TODO: Support 되지 않는 Type은 Error Logging 처리하여 향후 지원하도록 한다. **/
                    if(log.isDebugEnabled()) {
                        log.warn("Unsupported Resources : " + res.getResType());
                    }
                    break;
                }
            } // end of switch(k8sObj.getK8sApiKindType()) {
        }

        /**
         * 추출할 리소스 응답 객체 생성
         * **/
        for (PlResDeployVO res : resources) {
            /* Resource List Logging.................... */
            log.debug("====================================================\n" +
                    res.getResType() + " : " +
                    res.getResName() + " : " +
                    plMaster.getNamespace() + "\n----------------------------------------------\n");

//            field = String.format("%s.%s=%s", KubeConstants.META, KubeConstants.NAME, res.getResName());

            // 리소스에 해당하는 k8s 조회정보가 없으면 skip
            if ( !nsOjbects.containsKey(res.getResType()) || !nsOjbects.get(res.getResType()).containsKey(res.getResName()) ) {
                continue;
            }

            switch (res.getResType()) {

                case REPLICA_SERVER: {

                    /** 1. Get Deployments. **/
                    // List<K8sDeploymentVO> k8sDeployments = workloadResourceService.getDeployments(cluster, currentResources.getNamespace(), field, label, ContextHolder.exeContext());
                    K8sDeploymentVO k8sDeployment = (K8sDeploymentVO) nsOjbects.get(res.getResType()).get(res.getResName());
                    List<K8sDeploymentVO> k8sDeployments = new ArrayList<K8sDeploymentVO>(Collections.singletonList(k8sDeployment));

                    /** 2. Get ReplicaSets in Deployments **/
                    Map<String, List<K8sReplicaSetVO>> replicaSetMapInDeployments = new HashMap<>();
                    List<K8sReplicaSetVO> replicaSetsInDeployments = new ArrayList<>();
                    for (K8sDeploymentVO k8sDeploymentRow : Optional.ofNullable(k8sDeployments).orElseGet(() ->Lists.newArrayList())) {
                        Optional.ofNullable(k8sDeploymentRow).map(K8sDeploymentVO::getOldReplicaSets).ifPresent(replicaSetsInDeployments::addAll);
                        Optional.ofNullable(k8sDeploymentRow).map(K8sDeploymentVO::getNewReplicaSets).ifPresent(replicaSetsInDeployments::addAll);
                    }
                    addonCommonService.getReplicaSetToMap(replicaSetsInDeployments, replicaSetMapInDeployments, MapUtils.getObject(eventMap, K8sApiKindType.REPLICA_SET.getValue(), Maps.newHashMap()));

                    /** 3. Set replicaSet owner pod **/
                    for (K8sDeploymentVO k8sDeploymentRow : Optional.ofNullable(k8sDeployments).orElseGet(() ->Lists.newArrayList())) {
                        Optional.ofNullable(k8sDeploymentRow.getNewReplicaSets()).ifPresent(replicas -> addonCommonService.addPod(pods, podMap, replicas.get(0).getName()));
                        Optional.ofNullable(k8sDeploymentRow.getOldReplicaSets()).ifPresent(replicas -> addonCommonService.addPod(pods, podMap, replicas.get(0).getName()));
                        // Set replicaSet
                        k8sControllers.addAllReplicaSetsItem(replicaSetMapInDeployments.get(k8sDeploymentRow.getName()));
                    }
                    /** 4. Set Deployments. **/
                    k8sControllers.addAllDeploymentsItem(k8sDeployments);

                    break;
                }
                case STATEFUL_SET_SERVER: {
                    /** 1. Get StatefulSets **/
                    // List<K8sStatefulSetVO> k8sStatefulSets = workloadResourceService.getStatefulSets(cluster, currentResources.getNamespace(), field, label, ContextHolder.exeContext());
                    K8sStatefulSetVO k8sStatefulSet = (K8sStatefulSetVO) nsOjbects.get(res.getResType()).get(res.getResName());
                    List<K8sStatefulSetVO> k8sStatefulSets = new ArrayList<K8sStatefulSetVO>(Collections.singletonList(k8sStatefulSet));

                    /** 2. Set Events and Add Pods**/
                    for (K8sStatefulSetVO k8sStatefulSetRow : Optional.ofNullable(k8sStatefulSets).orElseGet(() ->Lists.newArrayList())) {
                        k8sStatefulSetRow.setEvents(MapUtils.getObject(eventMap, K8sApiKindType.STATEFUL_SET.getValue(), Maps.newHashMap()).get(k8sStatefulSetRow.getName()));
                        addonCommonService.addPod(pods, podMap, k8sStatefulSetRow.getName());
                    }
                    /** 3. Set StatefulSets. **/
                    k8sControllers.addAllStatefulSetsItem(k8sStatefulSets);

                    /** 4. volumeClaimTemplates -> PVC에 추가 **/
                    if (MapUtils.getObject(nsOjbects, PlResType.PVC, null) != null) {
                        for(K8sStatefulSetVO statefulSet : Optional.ofNullable(k8sStatefulSets).orElseGet(() ->Lists.newArrayList())) {
                            for (K8sPersistentVolumeClaimVO vctRow : Optional.ofNullable(statefulSet).map(K8sStatefulSetVO::getDetail).map(K8sStatefulSetDetailVO::getVolumeClaimTemplates).orElseGet(() ->Lists.newArrayList())) {
                                String pvcName = String.format("%s-%s-", vctRow.getName(), statefulSet.getName());
                                for (int idx = 0, idxe = statefulSet.getDesiredPodCnt(); idx < idxe; idx++) {
                                    // [VolumeClaimTemplate명]-[statefulSet명]-[idx] = PVC 이름 => 조회하여 PVC 목록에 넣어줌..
//                                    String volumeTemplateFieldSelector = String.format("%s.%s=%s", KubeConstants.META, KubeConstants.NAME, String.format("%s%d", pvcName, idx));
//                                List<K8sPersistentVolumeClaimVO> pvcList = this.genPersistentVolumeClaims(cluster, currentResources.getNamespace(), volumeTemplateFieldSelector, null, persistentVolumesMap, storageClassMap);
                                    String pvcNameIdx = String.format("%s%d", pvcName, idx);
                                    if (nsOjbects.get(PlResType.PVC).containsKey(pvcNameIdx)) {
                                        persistentVolumeClaims.add((K8sPersistentVolumeClaimVO)(nsOjbects.get(PlResType.PVC)).get(pvcNameIdx));
                                    }
                                }
                            }
                        }
                    }

                    break;
                }
                case DAEMON_SET_SERVER: {
                    /** 1. Get DaemonSets **/
                    // List<K8sDaemonSetVO> k8sDaemonSets = workloadResourceService.getDaemonSets(cluster, currentResources.getNamespace(), field, label, ContextHolder.exeContext());
                    K8sDaemonSetVO k8sDaemonSet = (K8sDaemonSetVO) nsOjbects.get(res.getResType()).get(res.getResName());
                    List<K8sDaemonSetVO> k8sDaemonSets = new ArrayList<K8sDaemonSetVO>(Collections.singletonList(k8sDaemonSet));

                    /** 2. Set Events and Add Pods**/
                    for (K8sDaemonSetVO k8sDaemonSetRow : Optional.ofNullable(k8sDaemonSets).orElseGet(() ->Lists.newArrayList())) {
                        k8sDaemonSetRow.setEvents(MapUtils.getObject(eventMap, K8sApiKindType.DAEMON_SET.getValue(), Maps.newHashMap()).get(k8sDaemonSetRow.getName()));
                        addonCommonService.addPod(pods, podMap, k8sDaemonSetRow.getName());
                    }
                    /** 3. Set DaemonSets. **/
                    k8sControllers.addAllDaemonSetsItem(k8sDaemonSets);
                    break;
                }
                case CRON_JOB_SERVER: {
                    /** 1. Get CronJobs **/
                    // List<K8sCronJobVO> k8sCronJobs = workloadResourceService.getCronJobs(cluster, currentResources.getNamespace(), field, label, ContextHolder.exeContext());
                    K8sCronJobVO k8sCronJob = (K8sCronJobVO) nsOjbects.get(res.getResType()).get(res.getResName());
                    List<K8sCronJobVO> k8sCronJobs = new ArrayList<K8sCronJobVO>(Collections.singletonList(k8sCronJob));

                    /** 2. Set Jobs From CronJobs **/
                    List<K8sJobVO> currentJobs = new ArrayList<>();
                    for (K8sCronJobVO k8sCronJobRow : Optional.ofNullable(k8sCronJobs).orElseGet(() ->Lists.newArrayList())) {
                        // Set CronJob Events
                        k8sCronJobRow.setEvents(MapUtils.getObject(eventMap, K8sApiKindType.CRON_JOB.getValue(), Maps.newHashMap()).get(k8sCronJobRow.getName()));
                        List<K8sJobVO> jobsInCronJob = Optional.ofNullable(k8sCronJobRow).map(K8sCronJobVO::getDetail).map(K8sCronJobDetailVO::getActiveJobs).orElseGet(() ->Lists.newArrayList());
                        for (K8sJobVO k8sJobRow : jobsInCronJob) {
                            // Set Jobs Events in CronJob
                            k8sJobRow.setEvents(MapUtils.getObject(eventMap, K8sApiKindType.JOB.getValue(), Maps.newHashMap()).get(k8sJobRow.getName()));
                        }
                        // Set Jobs From Cronjob
                        k8sControllers.addAllJobsItem(jobsInCronJob);

                        currentJobs.addAll(jobsInCronJob);
                    }

                    /** 3. Add Pods **/
                    for (K8sJobVO k8sJobRow : Optional.ofNullable(currentJobs).orElseGet(() ->Lists.newArrayList())) {
                        addonCommonService.addPod(pods, podMap, k8sJobRow.getName());
                    }

                    /** 4. Set CronJobs. **/
                    k8sControllers.addAllCronJobsItem(k8sCronJobs);
                    break;
                }
                case JOB_SERVER: {
                    /** 1. Get Jobs **/
                    K8sJobVO k8sJob = (K8sJobVO) nsOjbects.get(res.getResType()).get(res.getResName());
                    List<K8sJobVO> k8sJobs = new ArrayList<K8sJobVO>(Collections.singletonList(k8sJob));

                    /** 2. Set Events and Add Pods**/
                    for (K8sJobVO k8sJobRow : Optional.ofNullable(k8sJobs).orElseGet(() ->Lists.newArrayList())) {
                        k8sJobRow.setEvents(MapUtils.getObject(eventMap, K8sApiKindType.JOB.getValue(), Maps.newHashMap()).get(k8sJobRow.getName()));
                        addonCommonService.addPod(pods, podMap, k8sJobRow.getName());
                    }
                    /** 3. Set Jobs **/
                    k8sControllers.addAllJobsItem(k8sJobs);
                    break;
                }
                case SVC: {
                    /** 1. Get Services **/
                    K8sServiceVO svc = (K8sServiceVO) nsOjbects.get(res.getResType()).get(res.getResName());
                    services.add(svc);

                    break;
                }
                case IG: {
                    /** 1. Get Ingresses **/
                    K8sIngressVO ingress = (K8sIngressVO) nsOjbects.get(res.getResType()).get(res.getResName());
                    ingresses.add(ingress);

                    break;
                }
                case CM: {
                    /** 1. Get ConfigMaps **/
                    ConfigMapGuiVO configMap = (ConfigMapGuiVO) nsOjbects.get(res.getResType()).get(res.getResName());
                    configMaps.add(configMap);

                    break;
                }
                case SC: {
                    /** 1. Get Secrets **/
                    SecretGuiVO secret = (SecretGuiVO) nsOjbects.get(res.getResType()).get(res.getResName());
                    secrets.add(secret);
                    break;
                }
                case PVC: {
                    K8sPersistentVolumeClaimVO pvc = (K8sPersistentVolumeClaimVO) nsOjbects.get(res.getResType()).get(res.getResName());
                    if (!persistentVolumeClaims.contains(pvc)) {
                        persistentVolumeClaims.add(pvc);
                    }
                    break;
                }
                default: {
                    /** TODO: Support 되지 않는 Type은 Error Logging 처리하여 향후 지원하도록 한다. **/
                    if(log.isDebugEnabled()) {
                        log.warn("Unsupported Resources : " + res.getResType());
                    }
                    break;
                }
            } // end of switch (res.getResType())  {
        } // end of for (PlResDeployVO res : Optional.ofNullable(resources).orElseGet(() ->Lists.newArrayList())) {

        /**
         * 조회된 리소스별 셋팅
         */
        currentResources.setControllers(k8sControllers);
        currentResources.setConfigMaps(configMaps);
        currentResources.setIngresses(ingresses);
        currentResources.setPods(pods);
        currentResources.setSecrets(secrets);
        currentResources.setServices(services);
        currentResources.setVolumes(persistentVolumeClaims);

        /**
         * Response!!
         */
        return currentResources;

    }

    /**
     * Generate Persistent Volume Claim List
     * @param cluster
     * @param namespace
     * @param field
     * @param label
     * @param persistentVolumesMap
     * @param storageClassMap
     * @return
     * @throws Exception
     */
    private List<K8sPersistentVolumeClaimVO> genPersistentVolumeClaims(ClusterVO cluster, String namespace, String field, String label,
                                                                       Map<String, K8sPersistentVolumeVO> persistentVolumesMap,
                                                                       Map<String, K8sStorageClassVO> storageClassMap) throws Exception
    {
        /** 1. Get Persistent Volume Claims **/
        List<K8sPersistentVolumeClaimVO> pvcList = persistentVolumeService.getPersistentVolumeClaims(cluster, namespace, field, label, ContextHolder.exeContext());
        /** 1-1. Get Persistent Volume **/
        if (MapUtils.isEmpty(persistentVolumesMap)) {
            persistentVolumesMap = persistentVolumeService.getPersistentVolumesMap(cluster, namespace, null, null, ContextHolder.exeContext());
        }
        /** 1-2. Get StorageClass **/
        if (MapUtils.isEmpty(storageClassMap)) {
            storageClassMap = storageClassService.convertStorageClassDataMap(cluster, null, null, ContextHolder.exeContext());
        }

        /** 2. Set PersistentVolume And StorageClass **/
        for (K8sPersistentVolumeClaimVO k8sPersistentVolumeClaimRow : Optional.ofNullable(pvcList).orElseGet(() ->Lists.newArrayList())) {
            // Set PersistentVolume
            k8sPersistentVolumeClaimRow.setPersistentVolume(persistentVolumesMap.get(k8sPersistentVolumeClaimRow.getVolumeName()));
            // Set StorageClass
            k8sPersistentVolumeClaimRow.setStorageClass(storageClassMap.get(k8sPersistentVolumeClaimRow.getStorageClassName()));
        }

        return pvcList;
    }

    /**
     * 파이프라인 워크로드 리소스 수정
     *
     * @param plSeq
     * @param plResDeploySeq
     * @param serverIntegrate
     * @param context
     * @return
     * @throws Exception
     */
    @Transactional(transactionManager = "transactionManager")
    public void editWorkloadDeployResource(Integer plSeq, Integer plResDeploySeq, ServerIntegrateVO serverIntegrate, ExecutingContextVO context) throws Exception {

        IPlMapper plDao = sqlSession.getMapper(IPlMapper.class);
        IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
        IServiceMapper serviceDao = this.sqlSession.getMapper(IServiceMapper.class);

        PlResDeployVO plResource = plDao.getPlResDeploy(plSeq, plResDeploySeq);
        PlMasterVO plDetail = plDao.getPlDetail(plSeq);

        if (plResource != null && plResource.getResType().isWorkload()) {
            ClusterVO cluster = clusterDao.getCluster(plDetail.getClusterSeq());
            cluster.setNamespaceName(plDetail.getNamespace());

            List<ServiceRegistryUserVO> registryUsers = serviceDao.getRegistryUserByNamespace(cluster.getClusterSeq(), plDetail.getNamespace());
            List<Integer> projectIds = Lists.newArrayList();
            List<Integer> externalRegistryIds = Lists.newArrayList();
            if (CollectionUtils.isNotEmpty(registryUsers)) {
                for (ServiceRegistryUserVO service : registryUsers) {
                    projectIds.addAll(Optional.ofNullable(service.getProjects()).orElseGet(() ->Lists.newArrayList()).stream().map(ServiceRegistryVO::getProjectId).collect(Collectors.toList()));
                    externalRegistryIds.addAll(Optional.ofNullable(service.getExternalRegistries()).orElseGet(() ->Lists.newArrayList()).stream().map(ExternalRegistryVO::getExternalRegistrySeq).collect(Collectors.toList()));
                }
            }

            DeployType deployType = DeployType.valueOf(serverIntegrate.getDeployType());
            String yamlStr = "";
            switch (deployType) {
                case GUI:
                    ServerGuiVO updatedServerGui = (ServerGuiVO)serverIntegrate;

                    if (!StringUtils.equals(plResource.getResType().getCode(), updatedServerGui.getServer().getWorkloadType())) {
                        throw new CocktailException("Invalid resource type.", ExceptionType.InvalidParameter);
                    }
                    if (!StringUtils.equals(plResource.getResName(), updatedServerGui.getComponent().getComponentName())) {
                        throw new CocktailException("Invalid resource name.", ExceptionType.InvalidParameter);
                    }

                    // set image secret
                    // 2022.03.21 hjchoi
                    // 이제 이미지 pull 사용자는 워크스페이스가 아닌 플랫폼에 등록된 사용자로 셋팅
                    if (cluster.getAccount() != null) {
                        updatedServerGui.getServer().setImageSecret(cluster.getAccount().getRegistryDownloadUserId());
                    }

                    // set manifest
                    ServerYamlVO currServerYaml = new ServerYamlVO();
                    currServerYaml.setDeployType(DeployType.YAML.getCode());
                    currServerYaml.setWorkloadType(updatedServerGui.getServer().getWorkloadType());
                    currServerYaml.setWorkloadVersion(updatedServerGui.getServer().getWorkloadVersion());
                    currServerYaml.setClusterSeq(cluster.getClusterSeq());
                    currServerYaml.setNamespaceName(cluster.getNamespaceName());
                    currServerYaml.setWorkloadName(updatedServerGui.getComponent().getComponentName());
                    currServerYaml.setDescription(updatedServerGui.getComponent().getDescription());
                    currServerYaml.setYaml(plResource.getResCont());
                    yamlStr = serverConversionService.mergeWorkload(cluster, cluster.getNamespaceName(), updatedServerGui, currServerYaml);

                    break;
                case YAML:
                    ServerYamlVO updatedServerYaml = (ServerYamlVO)serverIntegrate;

                    if (!StringUtils.equals(plResource.getResType().getCode(), updatedServerYaml.getWorkloadType())) {
                        throw new CocktailException("Invalid resource type.", ExceptionType.InvalidParameter);
                    }
                    if (!StringUtils.equals(plResource.getResName(), updatedServerYaml.getWorkloadName())) {
                        throw new CocktailException("Invalid resource name.", ExceptionType.InvalidParameter);
                    }

                    /**
                     * yaml validation
                     */
                    if (!serverValidService.checkWorkloadYaml(updatedServerYaml.getNamespaceName(), WorkloadType.valueOf(updatedServerYaml.getWorkloadType()), updatedServerYaml.getWorkloadName(), updatedServerYaml.getYaml(), new JSON())) {
                        throw new CocktailException("Yaml is invalid.", ExceptionType.InvalidYamlData);
                    }

                    // set manifest
                    List<Object> objs = ServerUtils.getYamlObjects(updatedServerYaml.getYaml());
                    if (CollectionUtils.isNotEmpty(objs)) {
                        yamlStr = updatedServerYaml.getYaml();
                    }

                    break;
            }

            // update plResource
            plResource.setResCont(yamlStr);
            plResource.setUpdater(ContextHolder.exeContext().getUserSeq());
            plDao.updatePlResDeploy(plResource);

            /** 빌드정보 맵핑정보 셋팅 **/
            // 초기화
            plDao.deletePlResBuildDeployMapping(null, plResDeploySeq);
            // 신규 맵핑
            WorkloadType workloadType = WorkloadType.valueOf(plResource.getResType().getCode());
            ServerGuiVO serverGui = serverConversionService.convertYamlToGui(cluster, plResource.getNamespace(), workloadType.getCode(), WorkloadVersion.V1.getCode(), null, null, yamlStr);
            List<K8sContainerVO> initContainers = Lists.newArrayList();
            for (ContainerVO container : Optional.ofNullable(serverGui.getInitContainers()).orElseGet(() ->Lists.newArrayList())) {
                K8sContainerVO k8sContainer = new K8sContainerVO();
                k8sContainer.setName(container.getContainerName());
                k8sContainer.setImage(container.getFullImageName());
                initContainers.add(k8sContainer);
            }
            List<K8sContainerVO> containers = Lists.newArrayList();
            for (ContainerVO container : Optional.ofNullable(serverGui.getContainers()).orElseGet(() ->Lists.newArrayList())) {
                K8sContainerVO k8sContainer = new K8sContainerVO();
                k8sContainer.setName(container.getContainerName());
                k8sContainer.setImage(container.getFullImageName());
                containers.add(k8sContainer);
            }
            this.addResBuildWithMapping(plResource, projectIds, externalRegistryIds, initContainers, containers, plDao);

        }

    }

    /**
     * 파이프라인 컨피그맵 리소스 수정
     *
     * @param plSeq
     * @param plResDeploySeq
     * @param configMapName
     * @param configMapIntegrate
     * @param context
     * @return
     * @throws Exception
     */
    @Transactional(transactionManager = "transactionManager")
    public void editConfigMapDeployResource(Integer plSeq, Integer plResDeploySeq, String deployType, String configMapName, ConfigMapIntegrateVO configMapIntegrate, ExecutingContextVO context) throws Exception {
        IPlMapper plDao = sqlSession.getMapper(IPlMapper.class);

        PlResDeployVO plResource = plDao.getPlResDeploy(plSeq, plResDeploySeq);
        PlMasterVO plMaster = plDao.getPlMaster(plSeq);

        if (plMaster != null && plResource != null && plResource.getResType() == PlResType.CM) {

            // Yaml 복호화
            V1ConfigMap savedK8sRes = ServerUtils.unmarshalYaml(plResource.getResCont());
            ConfigMapGuiVO savedConfigMap = configMapService.convertConfigMapData(savedK8sRes);

            if (savedConfigMap != null) {
                // 저장된 정보와 이름과 네임스페이스 체크
                if(!StringUtils.equals(configMapName, savedConfigMap.getName())) {
                    throw new CocktailException("Can't change the name. (name is different)", ExceptionType.K8sConfigMapNameInvalid);
                }
                if(!StringUtils.equals(plMaster.getNamespace(), savedConfigMap.getNamespace())) {
                    throw new CocktailException("Can't change the namespace. (namespace is different)", ExceptionType.NamespaceNameInvalid);
                }

                String yamlStr = null;
                if (configMapIntegrate != null) {
                    if (DeployType.valueOf(deployType) == DeployType.GUI) {
                        ConfigMapGuiVO configMapGui = (ConfigMapGuiVO) configMapIntegrate;

                        if (configMapGui != null) {
                            configMapGui.setNamespace(plMaster.getNamespace());

                            configMapService.checkNameAndDescription(configMapName, configMapGui.getName(), configMapGui.getDescription());
                            configMapService.validConfigMapData(configMapGui);

                            V1ConfigMap k8sRes = K8sSpecFactory.buildConfigMapV1(configMapGui);

                            yamlStr = ServerUtils.marshalYaml(k8sRes);
                        }

                    }
                    else {
                        ConfigMapYamlVO configMapYaml = (ConfigMapYamlVO) configMapIntegrate;

                        if (configMapYaml != null) {
                            configMapService.checkNameAndDescription(configMapName, configMapYaml.getName(), "");

                            // Valid check 하기 위행 GUI로 변환
                            ConfigMapGuiVO configMapGui = configMapService.convertYamlToConfigMap(configMapYaml);
                            if(!plMaster.getNamespace().equals(configMapGui.getNamespace())) {
                                throw new CocktailException("Can't change the namespace. (namespace is different)", ExceptionType.NamespaceNameInvalid);
                            }
                            if(!configMapName.equals(configMapGui.getName())) { // Controller에서 URI path Param과 configMapYaml Param이 다른지는 이미 비교함..
                                throw new CocktailException("Can't change the name. (name is different)", ExceptionType.K8sConfigMapNameInvalid);
                            }

                            // set manifest
                            List<Object> objs = ServerUtils.getYamlObjects(configMapYaml.getYaml());
                            if (CollectionUtils.isNotEmpty(objs)) {
                                yamlStr = configMapYaml.getYaml();
                            }
                        }
                    }

                    // update plResource
                    plResource.setResCont(yamlStr);
                    plResource.setUpdater(ContextHolder.exeContext().getUserSeq());
                    plDao.updatePlResDeploy(plResource);
                } else {
                    throw new CocktailException("Secret resource is invalid request info.", ExceptionType.InvalidParameter);
                }

            } else {
                throw new CocktailException("ConfigMap resource is invalid content.", ExceptionType.InvalidInputData);
            }

        }

    }

    /**
     * 파이프라인 시크릿 리소스 수정
     *
     * @param plSeq
     * @param plResDeploySeq
     * @param secretName
     * @param secretIntegrate
     * @param context
     * @return
     * @throws Exception
     */
    @Transactional(transactionManager = "transactionManager")
    public void editSecretDeployResource(Integer plSeq, Integer plResDeploySeq, String deployType, String secretName, SecretIntegrateVO secretIntegrate, ExecutingContextVO context) throws Exception {
        IPlMapper plDao = sqlSession.getMapper(IPlMapper.class);

        PlResDeployVO plResource = plDao.getPlResDeploy(plSeq, plResDeploySeq);
        PlMasterVO plMaster = plDao.getPlMaster(plSeq);

        if (plMaster != null && plResource != null && plResource.getResType() == PlResType.SC) {

            // Yaml 복호화
            V1Secret savedK8sRes = ServerUtils.unmarshalYaml(CryptoUtils.decryptAES(plResource.getResCont()));
            SecretGuiVO savedSecret = secretService.convertSecretData(savedK8sRes, true);

            if (savedSecret != null) {
                // 저장된 정보와 이름과 네임스페이스 체크
                if(!StringUtils.equals(secretName, savedSecret.getName())) {
                    throw new CocktailException("Can't change the name. (name is different)", ExceptionType.K8sSecretNameInvalid);
                }
                if(!StringUtils.equals(plMaster.getNamespace(), savedSecret.getNamespace())) {
                    throw new CocktailException("Can't change the namespace. (namespace is different)", ExceptionType.NamespaceNameInvalid);
                }

                String yamlStr = null;
                if (secretIntegrate != null) {
                    if (DeployType.valueOf(deployType) == DeployType.GUI) {
                        SecretGuiVO secretGui = (SecretGuiVO) secretIntegrate;

                        if (secretGui != null) {
                            if(!secretName.equals(secretGui.getName())) {
                                throw new CocktailException("Can't change the name. (name is different)", ExceptionType.K8sSecretNameInvalid);
                            }
                            if(!plMaster.getNamespace().equals(secretGui.getNamespace())) {
                                throw new CocktailException("Can't change the namespace. (namespace is different)", ExceptionType.NamespaceNameInvalid);
                            }

                            secretService.checkSecretValidation(secretGui, false);

                            if(MapUtils.isNotEmpty(savedSecret.getData()) && MapUtils.isNotEmpty(secretGui.getData())){
                                // 저자왼 시크릿과 키를 비교하여 Data 셋팅
                                // 수정시 수정한 값만 넘어옴
                                for(Map.Entry<String, String> dataEntry : secretGui.getData().entrySet()){
                                    // 키가 존재할 경우
                                    if(savedSecret.getData().containsKey(dataEntry.getKey())){
                                        // // 수정 값이 존재하지 않는다면 기존 값으로 세팅
                                        if(StringUtils.isBlank(dataEntry.getValue())){
                                            dataEntry.setValue(new String(Base64Utils.decodeFromString(savedSecret.getData().get(dataEntry.getKey())), StandardCharsets.UTF_8));
                                        }
                                    }
                                }

                                // 키가 존재하지 않느다면 삭제 처리
                                for(Map.Entry<String, String> dataEntry : savedSecret.getData().entrySet()){
                                    if(!secretGui.getData().containsKey(dataEntry.getKey())){
                                        secretGui.getData().remove(dataEntry.getKey());
                                    }
                                }

                            }

                            V1Secret k8sRes = K8sSpecFactory.buildSecretV1(secretGui);

                            yamlStr = ServerUtils.marshalYaml(k8sRes);
                        }
                    }
                    else {
                        SecretYamlVO secretYaml = (SecretYamlVO) secretIntegrate;

                        if (secretYaml != null) {
                            // Valid check 하기 위행 GUI로 변환
                            SecretGuiVO secretGui = secretService.convertSecretData(savedK8sRes, secretYaml, true);

                            if(!secretName.equals(secretGui.getName())) {
                                throw new CocktailException("Can't change the name. (name is different)", ExceptionType.K8sSecretNameInvalid);
                            }
                            if(!plMaster.getNamespace().equals(secretGui.getNamespace())) {
                                throw new CocktailException("Can't change the namespace. (namespace is different)", ExceptionType.NamespaceNameInvalid);
                            }
                            secretService.checkSecretValidation(secretGui);

                            // set manifest
                            List<Object> objs = ServerUtils.getYamlObjects(secretYaml.getYaml());
                            if (CollectionUtils.isNotEmpty(objs)) {
                                yamlStr = secretYaml.getYaml();
                            }
                        }
                    }

                    // update plResource
                    plResource.setResCont(CryptoUtils.encryptAES(yamlStr));
                    plResource.setUpdater(ContextHolder.exeContext().getUserSeq());
                    plDao.updatePlResDeploy(plResource);
                } else {
                    throw new CocktailException("Secret resource is invalid request info.", ExceptionType.InvalidParameter);
                }

            } else {
                throw new CocktailException("Secret resource is invalid content.", ExceptionType.InvalidInputData);
            }

        }

    }

    /**
     * 파이프라인 서비스 리소스 수정
     *
     * @param plSeq
     * @param plResDeploySeq
     * @param serviceName
     * @param serviceSpecParam
     * @param context
     * @return
     * @throws Exception
     */
    @Transactional(transactionManager = "transactionManager")
    public void editServiceDeployResource(Integer plSeq, Integer plResDeploySeq, String deployType, String serviceName, ServiceSpecIntegrateVO serviceSpecParam, ExecutingContextVO context) throws Exception {
        IPlMapper plDao = sqlSession.getMapper(IPlMapper.class);
        IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);

        PlResDeployVO plResource = plDao.getPlResDeploy(plSeq, plResDeploySeq);
        PlMasterVO plMaster = plDao.getPlMaster(plSeq);

        if (plResource != null && plResource.getResType() == PlResType.SVC) {
            ClusterVO cluster = clusterDao.getCluster(plMaster.getClusterSeq());
            cluster.setNamespaceName(plMaster.getNamespace());
            String yamlStr = null;

            if (DeployType.valueOf(deployType) == DeployType.GUI) {
                ServiceSpecGuiVO serviceSpec = (ServiceSpecGuiVO) serviceSpecParam;
                if(!serviceName.equals(serviceSpec.getName())) {
                    throw new CocktailException("Can't change the Service name. (Service name is different)", ExceptionType.K8sServiceNameInvalid);
                }
                if (StringUtils.isBlank(serviceSpec.getName()) || !serviceSpec.getName().matches(KubeConstants.RULE_RESOURCE_NAME)) {
                    throw new CocktailException("Service name is invalid", ExceptionType.K8sServiceNameInvalid);
                }
                if(StringUtils.isBlank(serviceSpec.getServiceType()) || PortType.findPortName(serviceSpec.getServiceType()) == null) {
                    throw new CocktailException("ServiceType is invalid!", ExceptionType.InvalidInputData);
                }
                // validateHostPort
                String field = String.format("%s.%s!=%s", KubeConstants.META, KubeConstants.NAME, serviceName);
                Set<Integer> nodePorts = serviceSpecService.getUsingNodePortOfCluster(cluster, field, null, ContextHolder.exeContext()); // 해당 cluster의 k8s - service node port 조회
                List<ServiceSpecGuiVO> serviceSpecs = new ArrayList<>();
                serviceSpecs.add(serviceSpec);
                serverValidService.validateHostPort(serviceSpecs, nodePorts, cluster);

                V1Service updatedService = K8sSpecFactory.buildServiceV1(serviceSpec, plMaster.getNamespace(), null, false, cluster);

                yamlStr = ServerUtils.marshalYaml(updatedService);
            }
            else {
                ServiceSpecYamlVO serviceSpecYaml = (ServiceSpecYamlVO) serviceSpecParam;
                if(!serviceName.equals(serviceSpecYaml.getName())) {
                    throw new CocktailException("Can't change the Service name. (Service name is different)", ExceptionType.K8sServiceNameInvalid);
                }

                if (StringUtils.isBlank(serviceSpecYaml.getName()) || !serviceSpecYaml.getName().matches(KubeConstants.RULE_RESOURCE_NAME)) {
                    throw new CocktailException("Service name is invalid", ExceptionType.K8sServiceNameInvalid);
                }

                // validateHostPort
                String field = String.format("%s.%s!=%s", KubeConstants.META, KubeConstants.NAME, serviceName);
                Set<Integer> nodePorts = serviceSpecService.getUsingNodePortOfCluster(cluster, field, null, ContextHolder.exeContext()); // 해당 cluster의 k8s - service node port 조회
                List<ServiceSpecGuiVO> serviceSpecs = new ArrayList<>();
                ServiceSpecGuiVO newServiceSpec = serverConversionService.convertYamlToServiceSpec(cluster, serviceSpecYaml.getYaml());
                serviceSpecs.add(newServiceSpec);
                serverValidService.validateHostPort(serviceSpecs, nodePorts, cluster);

                if(!serviceName.equals(newServiceSpec.getName())) {
                    throw new CocktailException("Can't change the Service name. (Service name is different)", ExceptionType.K8sServiceNameInvalid);
                }
                if(!cluster.getNamespaceName().equals(newServiceSpec.getNamespaceName())) {
                    throw new CocktailException("Can't change the Service namespace. (Service namespace is different)", ExceptionType.NamespaceNameInvalid);
                }

                yamlStr = serviceSpecYaml.getYaml();

            }

            // update plResource
            plResource.setResCont(yamlStr);
            plResource.setUpdater(ContextHolder.exeContext().getUserSeq());
            plDao.updatePlResDeploy(plResource);
        }

    }

    /**
     * 파이프라인 인그레스 리소스 수정
     *
     * @param plSeq
     * @param plResDeploySeq
     * @param ingressName
     * @param ingressSpecParam
     * @param context
     * @return
     * @throws Exception
     */
    @Transactional(transactionManager = "transactionManager")
    public void editIngressDeployResource(Integer plSeq, Integer plResDeploySeq, String deployType, String ingressName, IngressSpecIntegrateVO ingressSpecParam, ExecutingContextVO context) throws Exception {
        IPlMapper plDao = sqlSession.getMapper(IPlMapper.class);
        IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);

        PlResDeployVO plResource = plDao.getPlResDeploy(plSeq, plResDeploySeq);
        PlMasterVO plMaster = plDao.getPlMaster(plSeq);

        if (plResource != null && plResource.getResType() == PlResType.IG) {
            ClusterVO cluster = clusterDao.getCluster(plMaster.getClusterSeq());
            cluster.setNamespaceName(plMaster.getNamespace());
            String yamlStr = null;

            Map<String, Object> configMapObjMap = ServerUtils.getK8sYamlToMap(plResource.getResCont());

//            K8sApiKindType apiKindType = ServerUtils.getK8sKindInMap(configMapObjMap);
//            K8sApiGroupType apiGroupType = ServerUtils.getK8sGroupInMap(configMapObjMap);
//            K8sApiType apiVerType = ServerUtils.getK8sVersionInMap(configMapObjMap);

            if (DeployType.valueOf(deployType) == DeployType.GUI) {
                IngressSpecGuiVO ingressSpecGui = (IngressSpecGuiVO) ingressSpecParam;
                if(!ingressName.equals(ingressSpecGui.getName())) {
                    throw new CocktailException("Can't change the Ingress name. (Ingress name is different)", ExceptionType.K8sIngressNameInvalid);
                }

                // valid
                ingressSpecService.checkIngress(cluster, cluster.getNamespaceName(), false, ingressSpecGui);

                // Igress 버전별로 머지하여 yaml 생성
                ExtensionsV1beta1Ingress currExtensionsIngress = null;
                NetworkingV1beta1Ingress currNetworkingV1beta1Ingress = null;
                V1Ingress currNetworkingV1Ingress = null;

                if (K8sApiVerType.isK8sVerSupported(cluster.getK8sVersion(), K8sApiVerType.V1_19)) {
                    currNetworkingV1Ingress = ServerUtils.unmarshalYaml(plResource.getResCont());
                } else if (K8sApiVerType.isK8sVerSupported(cluster.getK8sVersion(), K8sApiVerType.V1_14)) {
                    currNetworkingV1beta1Ingress = ServerUtils.unmarshalYaml(plResource.getResCont());
                } else {
                    currExtensionsIngress = ServerUtils.unmarshalYaml(plResource.getResCont());
                }

                if (currNetworkingV1Ingress != null) {
                    currNetworkingV1Ingress.setStatus(null);

                    // 수정할 ingress 병합
                    V1Ingress updatedIngress = ingressSpecService.mergeIngressSpec(cluster, plMaster.getNamespace(), ingressSpecGui, currNetworkingV1Ingress);

                    yamlStr = ServerUtils.marshalYaml(updatedIngress);

                } else if (currNetworkingV1beta1Ingress != null) {
                    currNetworkingV1beta1Ingress.setStatus(null);

                    // 수정할 ingress 병합
                    NetworkingV1beta1Ingress updatedIngress = ingressSpecService.mergeIngressSpec(cluster, plMaster.getNamespace(), ingressSpecGui, currNetworkingV1beta1Ingress);

                    yamlStr = ServerUtils.marshalYaml(updatedIngress);

                }
            }
            else {
                IngressSpecYamlVO ingressSpecYaml = (IngressSpecYamlVO) ingressSpecParam;
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


                yamlStr = ingressSpecYaml.getYaml();

            }

            // update plResource
            plResource.setResCont(yamlStr);
            plResource.setUpdater(ContextHolder.exeContext().getUserSeq());
            plDao.updatePlResDeploy(plResource);
        }

    }


    /**
     * 워크로드의 빌드 정상세 정보 수정
     *
     * @param plSeq
     * @param plResBuildSeq
     * @param plResBuild
     * @param ctx
     * @return
     * @throws Exception
     */
    @Transactional(transactionManager = "transactionManager")
    public PlResBuildVO editPlResBuild(Integer plSeq, Integer plResBuildSeq, PlResBuildVO plResBuild, ExecutingContextVO ctx) throws Exception {
        // 이전 정보 조회
        IPlMapper dao = sqlSession.getMapper(IPlMapper.class);
        PlResBuildVO prevBuildInfo = this.getPlResBuild(plSeq, plResBuildSeq, false);

        if (prevBuildInfo == null){
            throw new CocktailException("Not Fount BuildInfo.", ExceptionType.InvalidParameter, ExceptionBiz.PIPELINE_SET);
        }

        // plSeq 셋팅, 데이터상으로는 필요는 없으나 혹시 모르니...
        plResBuild.setPlSeq(plSeq);


        // 빌드 정보 validation check
        BuildAddVO buildAdd = buildService.convertFromBuildRunToBuildAdd(plResBuild.getBuildConfig());
        buildValidationService.checkBuildByAdd(buildAddValidator, buildAdd);

        // build server TLS Info 체크
        BuildRunVO prevBuildRun = prevBuildInfo.getBuildConfig();
        // prevBuildRun = buildService.getBuildRun(prevBuildRun.getBuildRunSeq(), null, false);
        buildValidationService.checkBuildServerTLSInfo(buildAdd, null, prevBuildRun);


        BuildRunVO buildRunVO = plResBuild.getBuildConfig();
        // build server TLS 값 암호화 설정
        if ( "Y".equals(buildRunVO.getBuildServerTlsVerify()) ){
            if(buildRunVO.getBuildServerCacrt() != null){
                buildRunVO.setBuildServerCacrt( CryptoUtils.encryptAES(buildRunVO.getBuildServerCacrt()) );
            } else if (prevBuildRun.getBuildServerCacrt() != null){
                buildRunVO.setBuildServerCacrt( CryptoUtils.encryptAES(prevBuildRun.getBuildServerCacrt()) );
            }

            if(buildRunVO.getBuildServerClientCert() != null){
                buildRunVO.setBuildServerClientCert( CryptoUtils.encryptAES(buildRunVO.getBuildServerClientCert()) );
            } else if (prevBuildRun.getBuildServerClientCert() != null){
                buildRunVO.setBuildServerClientCert( CryptoUtils.encryptAES(prevBuildRun.getBuildServerClientCert()) );
            }

            if(buildRunVO.getBuildServerClientKey() != null){
                buildRunVO.setBuildServerClientKey( CryptoUtils.encryptAES(buildRunVO.getBuildServerClientKey()) );
            }else if (prevBuildRun.getBuildServerClientKey() != null){
                buildRunVO.setBuildServerClientKey( CryptoUtils.encryptAES(prevBuildRun.getBuildServerClientKey()) );
            }
        }

        // buildRunStep 정보 json 변경 및 plBuildInfo의 buildCont 설정, 암호화 필드 설정
        List<BuildStepRunVO> prevBsrList = prevBuildRun.getBuildStepRuns();
        List<BuildStepRunVO> bsrList = buildRunVO.getBuildStepRuns();
        List<BuildStepRunVO> finalBsrList = new ArrayList<>();

        // 현재 step 정보의 build_step_seq max 값 조회
        int maxBuildStepSeq = bsrList.stream().map(BuildStepRunVO::getBuildStepSeq).max(Comparator.comparingInt(Integer::intValue)).orElseGet(() ->0);

        for (BuildStepRunVO bsr : bsrList){

            // 사용 안하는 step skip
            if(!bsr.isUseFlag()) {
                continue;
            }

            // build_step_seq가 없는 건이면 발급
            if (bsr.getBuildStepSeq().intValue() == 0){
                bsr.setBuildStepSeq(++maxBuildStepSeq); // buildStepSeq 셋팅
            }

            // 이전 등록된 step 정보 추출, BuildStepRunSeq 기준으로 먼저 찾고 없으면 BuildStepSeq 기준으로 찾는다.
            // 기존 실행되었던 build_step_run 에는 build_step
            Optional<BuildStepRunVO> prevBuildStepRunVOOpt = Optional.empty();
            if (bsr.getBuildStepRunSeq() != null && bsr.getBuildStepRunSeq() > 0) {
                prevBuildStepRunVOOpt = prevBsrList.stream().filter(bs -> (bs.getStepType() == bsr.getStepType() && bs.getBuildStepRunSeq().equals(bsr.getBuildStepRunSeq()))).findFirst();
            } else {
                prevBuildStepRunVOOpt = prevBsrList.stream().filter(bs -> (bs.getStepType() == bsr.getStepType() && bs.getBuildStepSeq().equals(bsr.getBuildStepSeq()))).findFirst();
            }

            if( bsr.getStepType() == StepType.CODE_DOWN ){
                StepCodeDownVO codeDownVO = (StepCodeDownVO)bsr.getBuildStepConfig();

                // http, https, ftp 프로토콜이 url에 존재한다면 삭제
                if(codeDownVO.getRepositoryType() == RepositoryType.GIT){
                    String url = codeDownVO.getRepositoryUrl();
                    if(StringUtils.isNotBlank(url) && Pattern.matches("^(?i)(https?|ftp)://.*$", url)){
                        codeDownVO.setRepositoryUrl( StringUtils.replacePattern(url, "^(?i)(https?|ftp)://", "") );
                    }

                    // code 저장경로 데이터가 없을 경우엔 git repositoryURL에서 추출
                    if ( StringUtils.isEmpty(codeDownVO.getCodeSaveDir()) ) {
                        String gitName = StringUtils.substringAfterLast(codeDownVO.getRepositoryUrl(), "/");
                        String codeSaveDir = gitName.replaceAll("[.](git|GIT)", "");
                        codeDownVO.setCodeSaveDir(codeSaveDir);
                    }
                }

                String commonType = codeDownVO.getCommonType();

                if(StringUtils.equals("COMMON", commonType)){
                    codeDownVO.setUserId(null);
                    codeDownVO.setPassword(null);

                } else if(StringUtils.equalsIgnoreCase("PRIVATE", commonType)){

                    if(StringUtils.isNotBlank(codeDownVO.getPassword())){
                        codeDownVO.setPassword(CryptoUtils.encryptAES(codeDownVO.getPassword()));

                    }else if (prevBuildStepRunVOOpt.isPresent()){
                        StepCodeDownVO prevCodeDownVO = (StepCodeDownVO) prevBuildStepRunVOOpt.get().getBuildStepConfig();
                        codeDownVO.setPassword(CryptoUtils.encryptAES(prevCodeDownVO.getPassword()));
                    }
                }

            } else if ( bsr.getStepType() == StepType.FTP ){
                StepFtpVO ftpStepVO = (StepFtpVO) bsr.getBuildStepConfig();

                // password가 존재 하면 암호화 처리
                if( StringUtils.isNotBlank(ftpStepVO.getPassword()) ){
                    ftpStepVO.setPassword(CryptoUtils.encryptAES(ftpStepVO.getPassword()));

                }else if(prevBuildStepRunVOOpt.isPresent()){ // 기존 데이터가 존재할 경우
                    // 입력값은 null 이고 기존 패스워드가 존재 하면 기존 값으로 대체 한다.
                    StepFtpVO prevFtpStepVO = (StepFtpVO)prevBuildStepRunVOOpt.get().getBuildStepConfig();
                    if( StringUtils.isNotBlank(prevFtpStepVO.getPassword()) ){
                        ftpStepVO.setPassword(CryptoUtils.encryptAES(prevFtpStepVO.getPassword()));
                    }
                }

            } else if ( bsr.getStepType() == StepType.HTTP ){
                StepHttpVO httpStepVO = (StepHttpVO) bsr.getBuildStepConfig();

                // password가 존재 하면 암호화 처리
                if( StringUtils.isNotBlank(httpStepVO.getPassword()) ){
                    httpStepVO.setPassword(CryptoUtils.encryptAES(httpStepVO.getPassword()));

                }else if(prevBuildStepRunVOOpt.isPresent()){ // 기존 데이터가 존재할 경우
                    // 입력값은 null 이고 기존 패스워드가 존재 하면 기존 값으로 대체 한다.
                    StepHttpVO prevHttpStepVO = (StepHttpVO) prevBuildStepRunVOOpt.get().getBuildStepConfig();
                    if( StringUtils.isNotBlank(prevHttpStepVO.getPassword()) ){
                        httpStepVO.setPassword(CryptoUtils.encryptAES(prevHttpStepVO.getPassword()));
                    }
                }
            }

            bsr.setStepConfig(JsonUtils.toGson(bsr.getBuildStepConfig()));
            bsr.setBuildStepConfig(null);

            // 사용하는 step만 담는다.
            finalBsrList.add(bsr);
        }

        // 최종 정리된 build step list를 담는다.
        buildRunVO.setBuildStepRuns(finalBsrList);

        plResBuild.setBuildCont(JsonUtils.toGson(buildRunVO)); // buildCont 설정
        plResBuild.setBuildConfig(null);

        // 빌드 정보 수정
        dao.updatePlResBuild(plResBuild);

        return plResBuild;
    }

    /**
     * pl_res_build 의 run_yn 값을 일괄 변경한다.
     *
     * @param plSeq
     * @param runYn
     */
    public void editPlResBuildsForRunYn(Integer plSeq, String runYn) {
        // 이전 정보 조회
        IPlMapper dao = sqlSession.getMapper(IPlMapper.class);
        dao.updatePlResBuildsForRunYn(plSeq, runYn);

    }

    /**
     * 파이프라인 마스터 이름을 업데이트 한다.
     *
     * @param plSeq
     * @param ctx
     * @return
     * @throws Exception
     */
    @Transactional(transactionManager = "transactionManager")
    public void editPlMasterName(Integer plSeq, String name, ExecutingContextVO ctx) throws Exception {

        IPlMapper dao = sqlSession.getMapper(IPlMapper.class);

        // 이전 정보 조회
        PlMasterVO plMaster = dao.getPlMaster(plSeq);

        if (plMaster == null){
            throw new CocktailException("수정할 수 있는 리소스가 존재 하지 않습니다.", ExceptionType.InvalidState, ExceptionBiz.PIPELINE_SET);
        }

        // check parameter
        ExceptionMessageUtils.checkParameterRequired("pipeline name", name);

        // update
        dao.updatePlMasterName(plSeq, name, ctx.getUserSeq());

    }

    /**
     * 파이프라인 마스터 삭제, 실제적으로는 use_yn 값만 'N'로 업데이트 한다.
     *
     * @param plSeq
     * @param ctx
     * @return
     * @throws Exception
     */
    @Transactional(transactionManager = "transactionManager")
    public PlMasterVO deletePlMaster(Integer plSeq, ExecutingContextVO ctx) throws Exception {
        // 이전 정보 조회
        PlMasterVO plMaster = this.getPlDetail(plSeq);

        if (plMaster == null){
            throw new CocktailException("삭제할 수 있는 리소스가 존재 하지 않습니다.", ExceptionType.InvalidState, ExceptionBiz.PIPELINE_SET);
        }

        IPlMapper dao = sqlSession.getMapper(IPlMapper.class);
        int deleteCnt = dao.deletePlMaster(plSeq, ctx.getUserSeq());
        log.debug(String.format("plSeq:[%s], delete count:[%s]", plSeq, deleteCnt));

        return plMaster;
    }

    /**
     * 빌드 리소스 삭제
     *
     * @param plSeq
     * @param plResBuildSeq
     * @param ctx
     * @return
     * @throws Exception
     */
    @Transactional(transactionManager = "transactionManager")
    public String deletePlResBuild(Integer plSeq, Integer plResBuildSeq, ExecutingContextVO ctx) throws Exception {
        // 이전 정보 조회
        IPlMapper dao = sqlSession.getMapper(IPlMapper.class);
        PlResBuildVO build = dao.getPlResBuild(plSeq, plResBuildSeq);

        String buildName = null;

        // 데이터 존재할 때만 삭제
        int deleteCnt = 0;
        if (build != null){
            /** 플랫폼 audit log 리소스명 입력 용도 로직 **/
            BuildRunVO buildRunVO = this.getBuildRunFromBuildCont(build.getBuildCont());
            if( buildRunVO != null) {
                buildName = buildRunVO.getBuildName();
            }
            /** 플랫폼 audit log 리소스명 입력 용도 로직 **/

            dao.deletePlResBuildDeployMapping(plResBuildSeq, null); // 맵핑정보 삭제
            dao.updatePlResBuildRunOrderForDel(plSeq, build.getRunOrder(), ctx.getUserSeq());
            dao.deletePlResBuild(plResBuildSeq);
        }

        return buildName;
    }

    /**
     * 배포 리소스 삭제
     *
     * @param plSeq
     * @param plResDeploySeq
     * @param ctx
     * @return
     * @throws Exception
     */
    @Transactional(transactionManager = "transactionManager")
    public PlResDeployVO deletePlResDeploy(Integer plSeq, Integer plResDeploySeq, ExecutingContextVO ctx) throws Exception {
        // 이전 정보 조회
        IPlMapper dao = sqlSession.getMapper(IPlMapper.class);
        PlResDeployVO plResource = dao.getPlResDeploy(plSeq, plResDeploySeq);

        // 데이터 존재할 때만 삭제
        int deleteCnt = 0;
        if (plResource != null){
            // 워크로드일 경우
            // - 빌드 맵핑정보 삭제
            // - 삭제한 워크로드의 run_order를 기준으로 나머지 run_order를 감소시켜줌.
            if (plResource.getResType().isWorkload()) {
                dao.deletePlResBuildDeployMapping(null, plResDeploySeq);
                dao.updatePlResDeployWorkloadRunOrderForDel(plSeq, plResource.getRunOrder(), ctx.getUserSeq());
            }
            deleteCnt = dao.deletePlResDeploy(plResDeploySeq);
        }

        return plResource;
    }

    /**
     * 파이프라인 배포 리소스 추가
     *
     * @param plSeq
     * @param plResDeploy
     * @throws Exception
     */
    @Transactional(transactionManager = "transactionManager")
    public void addPlResDeploy(Integer plSeq, PlResDeployVO plResDeploy) throws Exception {

        IPlMapper plDao = sqlSession.getMapper(IPlMapper.class);
        IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);

        PlMasterVO plMaster = plDao.getPlMaster(plSeq);
        ClusterVO cluster = clusterDao.getCluster(plMaster.getClusterSeq());

        // add deploy resource
        this.addPlResDeploies(cluster, plMaster.getNamespace(), plSeq, Collections.singletonList(plResDeploy));
    }

    /**
     * 파이프라인 워크로드 배포 리소스 실행순서 변경
     * (실행 순서 변경은 1 단위 위아래로만 가능)
     *
     * @param plSeq
     * @param plResDeploySeq
     * @param updateRunOrder
     * @param ctx
     * @throws Exception
     */
    @Transactional(transactionManager = "transactionManager")
    public PlResDeployVO editPlResDeployWorkloadRunOrder(Integer plSeq, Integer plResDeploySeq, Integer updateRunOrder, ExecutingContextVO ctx) throws Exception {

        // 이전 정보 조회
        IPlMapper dao = sqlSession.getMapper(IPlMapper.class);
        int plResDeployWorkloadCnt = dao.getPlResDeployWorkloadCount(plSeq);
        PlResDeployVO plResource = dao.getPlResDeploy(plSeq, plResDeploySeq);

        if (plResource != null
                && plResource.getResType() != null && plResource.getResType().isWorkload() // 워크로드 유형이고
                && updateRunOrder != null && updateRunOrder.intValue() > 0 && updateRunOrder.intValue() <= plResDeployWorkloadCnt // 변경할 실행순서가 0보다 크고 워크로드 전체 수보다는 작거나 같고
        ) {
            int runOrder = plResource.getRunOrder();
            int absVal = Math.abs(updateRunOrder.intValue() - runOrder);

            // 1 단위 일 경우에만 순서변경 가능
            if (absVal == 1) {
                // 변경되는 워크로드와 뒤바뀌는 워크로드의 순서를 변경
                dao.updatePlResDeployWorkloadRunOrder(plSeq, null, runOrder, updateRunOrder, ctx.getUserSeq());
                // 변경되는 워크로드의 순서 변경
                dao.updatePlResDeployWorkloadRunOrder(plSeq, plResDeploySeq, updateRunOrder, null, ctx.getUserSeq());
            }
        }

        return plResource;
    }

    @Transactional(transactionManager = "transactionManager")
    public PlResDeployVO editPlResDeployRunYn(Integer plSeq, Integer plResDeploySeq, String runYn, ExecutingContextVO ctx) throws Exception {
        // 이전 정보 조회
        IPlMapper dao = sqlSession.getMapper(IPlMapper.class);
        PlResDeployVO resDeploy = dao.getPlResDeploy(plSeq, plResDeploySeq);

        if (resDeploy != null && runYn != null) {
            // run_yn 업데이트
            dao.updatePlResDeployRunYn(plSeq, plResDeploySeq, runYn, ctx.getUserSeq());
        }

        return resDeploy;
    }

    @Transactional(transactionManager = "transactionManager")
    public String editPlResBuildRunOrder(Integer plSeq, Integer plResBuildSeq, Integer updateRunOrder, ExecutingContextVO ctx) throws Exception {

        // 이전 정보 조회
        IPlMapper dao = sqlSession.getMapper(IPlMapper.class);
        PlResBuildVO resBuild = dao.getPlResBuild(plSeq, plResBuildSeq);

        String buildName = null;

        if (resBuild != null
                && updateRunOrder != null
                && updateRunOrder > 0
        ) {
            /** 플랫폼 audit log 리소스명 입력 용도 로직 **/
            BuildRunVO buildRunVO = this.getBuildRunFromBuildCont(resBuild.getBuildCont());
            if( buildRunVO != null) {
                buildName = buildRunVO.getBuildName();
            }
            /** 플랫폼 audit log 리소스명 입력 용도 로직 **/

            int absVal = Math.abs(updateRunOrder - resBuild.getRunOrder());

            // 1 단위 일 경우에만 순서변경 가능
            if (absVal == 1) {
                // 변경되는 빌드와 뒤바뀌는 빌드의 순서를 변경
                dao.updatePlResBuildRunOrder(plSeq, null, resBuild.getRunOrder(), updateRunOrder, ctx.getUserSeq());
                // 변경되는 빌드의 순서 변경
                dao.updatePlResBuildRunOrder(plSeq, plResBuildSeq, updateRunOrder, null, ctx.getUserSeq());
            }
        }

        return buildName;
    }

    @Transactional(transactionManager = "transactionManager")
    public String editPlResBuildRunYn(Integer plSeq, Integer plResBuildSeq, String runYn, ExecutingContextVO ctx) throws Exception {
        // 이전 정보 조회
        IPlMapper dao = sqlSession.getMapper(IPlMapper.class);
        PlResBuildVO resBuild = dao.getPlResBuild(plSeq, plResBuildSeq);

        String buildName = null;

        if (resBuild != null && runYn != null) {
            /** 플랫폼 audit log 리소스명 입력 용도 로직 **/
            BuildRunVO buildRunVO = this.getBuildRunFromBuildCont(resBuild.getBuildCont());
            if( buildRunVO != null) {
                buildName = buildRunVO.getBuildName();
            }
            /** 플랫폼 audit log 리소스명 입력 용도 로직 **/

            // run_yn 업데이트
            dao.updatePlResBuildRunYn(plSeq, plResBuildSeq, runYn, ctx.getUserSeq());
        }

        return buildName;
    }

    /**
     * 파이프라인 실행 가능여부, 동시빌드수 체크, 첫번째 빌드 수행 가능여부 등을 체크함.
     *
     * @param plSeq
     * @throws Exception
     */
    public void checkRunValidation(Integer plSeq) throws Exception {
        /***************** Validation Check Start *****************/

        IPlMapper plDao = sqlSession.getMapper(IPlMapper.class);
        IAccountMapper aDao = sqlSession.getMapper(IAccountMapper.class);

        PlMasterVO plDetail = plDao.getPlDetail(plSeq);

        // validation, pipeline 정보가 없으면 Exception
        if(plDetail == null){
            throw new CocktailException("실행할 수 없는 pipeline 상태 입니다.", ExceptionType.InvalidState, ExceptionBiz.PIPELINE_SET);
        }

        int runCnt = plDao.getPlRunCount(plDetail.getPlSeq());

        // 해당 파이프라인이 현재 실행 중이면 Exception 처리
        if(runCnt > 0){
            throw new CocktailException("실행할 수 없는 pipeline 상태 입니다.", ExceptionType.InvalidState, ExceptionBiz.PIPELINE_SET);
        }

        // 빌드 해야할 건이 있는지 체크
        boolean existsBuildToRun = false;
        if(CollectionUtils.isNotEmpty(plDetail.getPlResBuilds())){
            existsBuildToRun = (plDetail.getPlResBuilds().stream().anyMatch(build -> BooleanUtils.toBoolean(build.getRunYn())));
        }

        // 배포 해야할 건이 있는지 체크
        boolean existsDeployToRun = false;
        if(CollectionUtils.isNotEmpty(plDetail.getPlResDeploys())){
            existsDeployToRun = (plDetail.getPlResDeploys().stream().anyMatch(deploy -> BooleanUtils.toBoolean(deploy.getRunYn())));
        }

        // 실행 해야할 건이 존재 하지 않은 경우 오류 리턴
        if(!existsBuildToRun && !existsDeployToRun){
            throw new CocktailException("실행할 수 있는 리소스가 존재 하지 않습니다.", ExceptionType.InvalidState, ExceptionBiz.PIPELINE_SET);
        }

        // 빌드건이 있을경우, 동시빌드 & 첫번째 빌드 가능한지 체크
        if (existsBuildToRun){
            Integer accountSeq = plDao.getAccountSeq(plDetail.getPlSeq());
            // 빌드 가능한지 체크, 불가능 하면 Exception 발생
            buildService.checkPossibleRunBuildBySystem(accountSeq);

            List<PlResBuildVO> buildRuns = plDetail.getPlResBuilds().stream().filter(build -> build.getRunYn().equals("Y")).sorted(Comparator.comparingInt(PlResBuildVO::getRunOrder)).collect(Collectors.toList());
            AccountVO account = aDao.getAccount(accountSeq);
            Set<String> buildServerHostSet = Sets.newHashSet();
            for (PlResBuildVO br : buildRuns) {
                BuildRunVO buildConfig = plRunBuildService.convertBuildRunStringToDecryptedBuildRunVO(br.getBuildCont(), true);
                if (buildConfig != null) {
                    // 내부 빌드서버 사용여부 체크
                    // BuildServerHost 값이 없을때만 체크
                    if (StringUtils.isBlank(buildConfig.getBuildServerHost())) {
                        buildValidationService.checkUseInternalBuildServer(account, buildConfig.getBuildServerHost());
                    } else {
                        buildServerHostSet.add(buildConfig.getBuildServerHost());
                    }
                }
            }

            // 외부 빌드서버 상태 및 워크스페이스 리소스 체크
            for (String host : buildServerHostSet) {
                buildValidationService.chekcBuildServerStatus(accountSeq, host);
            }

            // 첫번째 빌드 정보 가져오기
            PlResBuildVO firstBuild = plDetail.getPlResBuilds().stream().filter(build -> build.getRunYn().equals("Y")).min(Comparator.comparingInt(PlResBuildVO::getRunOrder)).get();

            // 현재 실행중인 빌드가 있는지 체크, 불가능 하면 Exception 발생
            buildService.checkPossibleRunBuildByBuild(firstBuild.getBuildSeq(), 0);
        }

        /***************** Validation Check End *****************/
    }

    /**
     * 파이프라인 취소 가능여부 체크, PL 실행중 & 빌드실행중 일때만 취소 가능, 취소 대상 PlRunBuild 리턴
     *
     * @param plSeq
     * @param plRunSeq
     *
     * @throws Exception
     */
    public PlRunBuildVO checkCancelValidation(Integer plSeq, Integer plRunSeq) throws Exception {
        /***************** Cancel Validation Check Start *****************/

        IPlMapper plDao = sqlSession.getMapper(IPlMapper.class);

        PlRunVO plRunVO = plDao.getPlRunDetail(plRunSeq, "Y");
        Optional<PlRunBuildVO> optionalPlRunBuildVO = plRunVO.getPlRunBuilds().stream()
                .filter(vo -> BooleanUtils.toBoolean(vo.getRunYn()))
                .sorted(Comparator.comparing( PlRunBuildVO::getRunOrder ))
                .filter(vo -> vo.getRunStatus() == PlStatus.RUNNING).findAny();

        // Pl 이 실행중이 아니거나 실행중인 Build 가 없을 경우에는 취소할 수 없는 상태임
        if (plRunVO.getRunStatus() != PlStatus.RUNNING || !optionalPlRunBuildVO.isPresent()) {
            throw new CocktailException("취소를 실행할 수 없는 pipeline 상태 입니다.", ExceptionType.InvalidState, ExceptionBiz.PIPELINE_SET);
        }

        PlRunBuildVO plRunBuildVO = optionalPlRunBuildVO.get();

        // 빌드 취소할 대상조회, plRunBuildVO.getBuildRunSeq() 가 NULL 일경우는 buildRunVO 가 null 임
        BuildRunVO buildRunVO = buildService.getBuildRun(plRunBuildVO.getBuildRunSeq());

        if(buildRunVO == null || buildRunVO.getRunState() != RunState.RUNNING){
            throw new CocktailException(String.format("Build is not running! [buildSeq : %s, buildRunSeq : %s]", buildRunVO.getBuildSeq(), buildRunVO.getBuildRunSeq()), ExceptionType.InvalidState, ExceptionBiz.PIPELINE_SET);
        }

        // 빌드가 취소 가능한 상태인지 체크
        if(!buildRunVO.getPossibleActions().contains(BuildAction.CANCEL)){
            throw new CocktailException(String.format("The build is not in a cancelable state. [buildSeq : %s, buildRunSeq : %s]", buildRunVO.getBuildSeq(), buildRunVO.getBuildRunSeq()), ExceptionType.InvalidState, ExceptionBiz.PIPELINE_SET);
        }

        return plRunBuildVO;

        /***************** Cancel Validation Check End *****************/
    }


    /**
     * Pl 상태 CANCEL로 변경 및 현재 빌드되고 있는건 취소 요청
     *
     * @param plRunSeq 취소할 Pl실행 번호
     * @param runBuildVO 취소할 빌드 정보
     * @return 상태 변경된 PlRun상세 정보
     * @throws Exception
     */
    public PlRunVO cancelPlRun(Integer plRunSeq, PlRunBuildVO runBuildVO) throws Exception {
        IPlMapper plDao = sqlSession.getMapper(IPlMapper.class);

        PlRunVO plRunVO = null;

        /** 빌드 취소 정보 생성 */
        // 빌드 취소할 대상조회
        BuildRunVO buildRunVO = buildService.getBuildRun(runBuildVO.getBuildRunSeq());

        // callback URL은 기존 정보 조회
        String callbackUrl = buildRunVO.getCallbackUrl();
        BuildRunVO cancelBuildRun = buildService.createBuildRunByBuildCancel(runBuildVO.getBuildRunSeq(), callbackUrl); // 빌드 취소용 BuildRun 생성

        /** PL 상태 변경 **/
        // plRun상태 Cancel 으로 변경
        plDao.updatePlRunStatus(PlStatus.CANCELED.getCode(), plRunSeq);

        // 대상 PL 실행 빌드 정보 상태 변경
        plDao.updatePlRunBuildStatus(runBuildVO.getPlRunBuildSeq(), PlStatus.ERROR.getCode());

        /** 빌드 취소 호출 */
        // BuildAPI에 빌드 실행 취소 호출
        pipelineAsyncService.processPipelineService(cancelBuildRun);

        plRunVO = this.getPlRunDetail(plRunSeq, null);

        // Pl 이벤트 발송
        plEventService.sendPlRunState(plRunVO.getCallbackUrl(), plRunVO.getPlSeq(), plRunVO.getPlRunSeq(), plRunVO.getRunStatus());


        return plRunVO;
    }

    /**
     * 파이프라인 실행 하는 메서드
     *
     * @param plRunVO
     * @return
     */
    public PlRunVO runPl(PlRunVO plRunVO) throws Exception {
        IPlMapper plDao = sqlSession.getMapper(IPlMapper.class);

        plRunVO = this.getPlRunDetail(plRunVO.getPlRunSeq(), "Y");

        if (plRunVO != null) {

            // 빌드 해야할 건이 있을경우 빌드 체크
            boolean existsBuildToRun = false;
            if( CollectionUtils.isNotEmpty(plRunVO.getPlRunBuilds()) ){
                existsBuildToRun = (plRunVO.getPlRunBuilds().size() > 0);
            }

            // 빌드건이 존재하면 빌드요청 후 끝남
            if(existsBuildToRun){
                plAsyncService.runPl(PlRunMode.BUILD, plRunVO.getPlRunSeq(), ContextHolder.exeContext());

            } else {
                plAsyncService.runPl(PlRunMode.DEPLOY, plRunVO.getPlRunSeq(), ContextHolder.exeContext());

            }

            /** 중요, pl_run 정보로 pl 정보 update, created & updated 값을 동일하게 설정 한다. **/
            plDao.updatePlResBuildsToSameDate(plRunVO.getPlSeq());
            plDao.updatePlResDeploysToSameDate(plRunVO.getPlSeq());


            plRunVO = this.getPlRunDetail(plRunVO.getPlRunSeq(), null);

        }

        return plRunVO;
    }

    @Transactional(transactionManager = "transactionManager")
    public PlRunVO createPlRun(PlMasterVO detail, String runNote, String callbackUrl){
        return this.createPlRun(detail, runNote, callbackUrl, null);
    }

    /**
     * 파이프라인 실행을 위한 run 정보 생성
     *
     * @param detail
     * @param runNote
     * @param callbackUrl
     * @param runtype - 외부 실행용 (현대카드)
     * @return
     */
    @Transactional(transactionManager = "transactionManager")
    public PlRunVO createPlRun(PlMasterVO detail, String runNote, String callbackUrl, String runtype){
        IPlMapper plDao = sqlSession.getMapper(IPlMapper.class);

        if (StringUtils.isBlank(runtype)) {
            runtype = PlRunType.Names.LATEST;
        }

        // 실행 유형 (외부 실행시)
        PlRunType plRunType = PlRunType.valueOf(runtype);

        boolean isExistBuild = CollectionUtils.isNotEmpty(detail.getPlResBuilds());
        boolean isExistDeploy = CollectionUtils.isNotEmpty(detail.getPlResDeploys());

        String buildRunYn = null;
        if (isExistBuild) {
            if (EnumSet.of(PlRunType.ALL, PlRunType.BUILD).contains(plRunType)) {
                buildRunYn = "Y";
            } else if (EnumSet.of(PlRunType.DEPLOY).contains(plRunType)) {
                buildRunYn = "N";
            }
        }

        String deployRunYn = null;
        if (isExistDeploy) {
            if (EnumSet.of(PlRunType.ALL, PlRunType.DEPLOY).contains(plRunType)) {
                deployRunYn = "Y";
            } else if (EnumSet.of(PlRunType.BUILD).contains(plRunType)) {
                deployRunYn = "N";
            } else {
                /**
                 * 2021.07.08, hjchoi - 현대카드 모드일 경우 워크스페이스 권한이 DEV는 배포 금지 처리
                 * 2022.04.21, hjchoi - 위 권한 처리를 기본 기능으로 처리하도록 변경
                 */
                if (UserRole.valueOf(ContextHolder.exeContext().getUserRole()) == UserRole.DEVOPS) {
                    UserGrant userGrant = this.getUserGrant(detail);
                    if (userGrant != null) {
                        if (EnumSet.of(UserGrant.DEVOPS, UserGrant.DEV, UserGrant.VIEWER).contains(userGrant)) {
                            deployRunYn = "N";
                        }
                    } else {
                        deployRunYn = "N";
                    }
                }

            }
        }

        PlRunVO plRunVO = new PlRunVO();

        // pl_run 생성createPlRun
        plRunVO.setPlSeq(detail.getPlSeq());
        plRunVO.setRunNote(runNote);
        plRunVO.setVer(detail.getVer());
        plRunVO.setRunStatus(PlStatus.RUNNING);
        plRunVO.setCallbackUrl(callbackUrl);
        plDao.insertPlRun(plRunVO);

        // pl_run_build 생성
        if (isExistBuild) {
            plDao.insertPlRunBuildWithRes(detail.getPlSeq(), plRunVO.getPlRunSeq(), PlStatus.WAIT.getCode(), buildRunYn, ContextHolder.exeContext().getUserSeq());
        }

        // pl_run_deploy 생성
        if (isExistDeploy) {
            plDao.insertPlRunDeployWithRes(detail.getPlSeq(), plRunVO.getPlRunSeq(), PlStatus.WAIT.getCode(), deployRunYn, ContextHolder.exeContext().getUserSeq());
        }
        // pl_run_build_deploy_mapping 생성
        plDao.insertPlRunBuildDeployMappingWithRes(plRunVO.getPlRunSeq());

        return plRunVO;
    }

    /**
     * 요청 사용자의 워크스페이스 권한 조회
     *
     * @param detail
     * @return
     */
    public UserGrant getUserGrant(PlMasterVO detail) {
        if (UserRole.valueOf(ContextHolder.exeContext().getUserRole()) == UserRole.DEVOPS) {
            if (CollectionUtils.isNotEmpty(ContextHolder.exeContext().getUserRelations())) {
                IServicemapMapper servicemapDao = sqlSession.getMapper(IServicemapMapper.class);
                ServicemapVO servicemap = servicemapDao.getServicemapByClusterAndName(detail.getClusterSeq(), detail.getNamespace());
                if (servicemap != null) {
                    Map<Integer, ServicemapMappingVO> servicemapMappingMap = Optional.ofNullable(servicemap.getServicemapMappings()).orElseGet(() ->Lists.newArrayList())
                            .stream().collect(Collectors.toMap(ServicemapMappingVO::getServiceSeq, Function.identity()));
                    for (ServiceRelationVO srRow : ContextHolder.exeContext().getUserRelations()) {
                        if (servicemap.getClusterSeq().equals(srRow.getClusterSeq())) {
                            if (MapUtils.isNotEmpty(servicemapMappingMap) && servicemapMappingMap.containsKey(srRow.getServiceSeq())) {
                                return srRow.getUserGrant();
                            }
                        }
                    }
                }
            }
        }

        return null;
    }

    /**
     * Pl 실행후 pl_master 테이블에 pl_run_seq 를 update
     *
     * @param plSeq
     * @param plRunSeq
     */
    public void updatePlMasterForPlRunSeqAndVersion(Integer plSeq, Integer plRunSeq, String ver){
        IPlMapper plDao = sqlSession.getMapper(IPlMapper.class);
        plDao.updatePlMasterForRunSeqAndVersion(plSeq, plRunSeq, ver);
    }

    /**
     * 파이프라인에 해당하는 실행 이력 리스트 조회
     *
     * @param params
     * @return
     * @throws Exception
     */
    public List<PlRunVO> getPlRunList(PlRunListSearchVO params) throws Exception {
        IPlMapper plDao = sqlSession.getMapper(IPlMapper.class);
        return plDao.getPlRunList(params);
    }

    /**
     * 파이프라인에 해당하는 실행 이력 리스트 조회
     *
     * @param plSeq
     * @return
     * @throws Exception
     */
    public PlRunListVO getPlRunList(Integer plSeq, List<String> exceptRunningStatus, String order, String orderColumn, Integer nextPage, Integer itemPerPage, String searchColumn, String searchKeyword, String startDate, String endDate) throws Exception {
        PlRunListVO plRunList = new PlRunListVO();

        PlRunListSearchVO params = new PlRunListSearchVO();
        params.setPlSeq(plSeq);
        params.setExceptRunningStatus(exceptRunningStatus);
        ListCountVO listCount = this.getPlRunListCount(params);
        PagingVO paging = PagingUtils.setPagingParams(orderColumn, order, nextPage, itemPerPage, null, listCount);
        params.setPaging(paging);

        List<PlRunVO> plRuns = this.getPlRunList(params);

        plRunList.setItems(plRuns);
        plRunList.setTotalCount(params.getPaging().getListCount().getCnt());
        plRunList.setCurrentPage(nextPage);

        return plRunList;
    }

    /**
     * pl_run 갯수 조회
     * @param params
     * @return
     * @throws Exception
     */
    public ListCountVO getPlRunListCount(PlRunListSearchVO params) throws Exception {
        IPlMapper plDao = sqlSession.getMapper(IPlMapper.class);
        return plDao.getPlRunListCount(params);
    }

    /**
     * 파이프라인에 해당하는 버전 리스트 조회
     *
     * @param plSeq
     * @return
     * @throws Exception
     */
    public List<PlRunVO> getPlVerList(Integer plSeq) throws Exception {
        IPlMapper plDao = sqlSession.getMapper(IPlMapper.class);
        return plDao.getPlVerList(plSeq);
    }


    /**
     * pl_run 상세정보 조회
     *
     * @param plRunSeq
     * @param runYn 실행여부 값 조회 조건
     * @return
     * @throws Exception
     */
    public PlRunVO getPlRunDetail(Integer plRunSeq, String runYn) throws Exception {
        return this.getPlRunDetail(plRunSeq, runYn, false);
    }

    /**
     * 파이프라인 실행된 빌드 정보 상세 조회
     *
     * @param plRunSeq
     * @param plRunSeq
     * @param plRunBuildSeq
     * @return
     */

    public PlRunBuildVO getPlRunBuildDetail(Integer plSeq, Integer plRunSeq, Integer plRunBuildSeq) throws Exception {
        IPlMapper plDao = sqlSession.getMapper(IPlMapper.class);
        PlRunBuildVO plRunBuild = plDao.getPlRunBuildDetail(plSeq, plRunSeq, plRunBuildSeq);

        if(plRunBuild != null) {

            this.setBuildConfig(plRunBuild);

        }

        return plRunBuild;
    }

    /**
     * pl_run_ 상세정보 조회
     *
     * @param plRunSeq
     * @param runYn 실행여부 값 조회 조건
     * @param includeDetailType - 특정 배포 리소스의 상세 유형 정보 포함 여부
     * @return
     * @throws Exception
     */
    public PlRunVO getPlRunDetail(Integer plRunSeq, String runYn, boolean includeDetailType) throws Exception {
        IPlMapper plDao = sqlSession.getMapper(IPlMapper.class);
        PlRunVO runDetail = plDao.getPlRunDetail(plRunSeq, runYn);

        // build 실행정보 string -> vo 형태로 변경
        List<PlRunBuildVO> runBuilds = runDetail.getPlRunBuilds();

        if(runBuilds != null && runBuilds.size() > 0) {

            for (PlRunBuildVO build : runBuilds) {

                this.setBuildConfig(build);
            }
        }

        boolean isReleased = (runDetail.getRunStatus() == PlStatus.DONE);

        if (CollectionUtils.isNotEmpty(runDetail.getPlRunDeploys()) && (includeDetailType || isReleased)) {
            IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
            PlMasterVO plMaster = plDao.getPlMaster(runDetail.getPlSeq());
            ClusterVO cluster = clusterDao.getCluster(plMaster.getClusterSeq());
            WorkloadVersion workloadVersion = WorkloadVersion.V1;

            for (PlRunDeployVO deploy : runDetail.getPlRunDeploys()) {
                switch (PlResType.valueOf(deploy.getResType())) {
                    case SVC:
                        if (includeDetailType) {
                            // 서비스 유형 셋팅
                            K8sServiceVO k8sService = this.convertServiceYamlToK8sObj(deploy.getResCont());
                            deploy.setResDetailType(k8sService.getDetail().getType());
                        }
                        break;
                    case REPLICA_SERVER:
                    case STATEFUL_SET_SERVER:
                    case DAEMON_SET_SERVER:
                    case CRON_JOB_SERVER:
                    case JOB_SERVER:
                        // 파이프라인 상태 - Released일 경우 추가 정보 셋팅
                        if (isReleased) {
                            WorkloadType workloadType = WorkloadType.valueOf(deploy.getResType());
                            List<Object> objs = ServerUtils.getYamlObjects(deploy.getResCont());
                            ServerGuiVO serverGui = serverConversionService.convertYamlToGui(cluster, plMaster.getNamespace(), workloadType.getCode(), workloadVersion.getCode(), null, null, objs);
                            deploy.setWorkloadConfig(serverGui);
                        }
                        break;
                }
            }
        }

        return runDetail;
    }

    public void setBuildConfig(PlRunBuildVO plRunBuild) throws Exception {
        BuildRunVO buildRunVO = null;
        boolean hasBuildRun = true;

        // buildRunSeq가 존재 하는건 (빌드된건)은 buildRun 정보 조회하여 셋팅
        if (BooleanUtils.toBoolean(plRunBuild.getRunYn()) && plRunBuild.getBuildRunSeq() != null){
            BuildRunVO tmpBuildRun = buildService.getBuildRun(plRunBuild.getBuildRunSeq(), "Y", false);

            if (tmpBuildRun != null) {
                buildRunVO = buildService.convertBuildStepRunConfig(tmpBuildRun, false);
            } else {
                hasBuildRun = false;
            }

        }

        // 실행한 buildRun이 존재하지 않는다면 plBuildRun으로 셋팅
        plRunBuild.setHasBuildRun(hasBuildRun); // 실행한 buildRun이 존재여부
        if (buildRunVO == null) {
            buildRunVO = plRunBuildService.convertBuildRunStringToDecryptedBuildRunVO(plRunBuild.getBuildCont());
        }

        plRunBuild.setBuildConfig(buildRunVO);
        plRunBuild.setBuildCont(null);
    }


    /**
     * PlRun 내의 각 실행한 build들과 deploy들의 로그를 취합해 문자열로 리턴한다.
     *
     * @param plRunSeq
     * @return
     * @throws Exception
     */
    public String getPlRunLog(Integer plRunSeq) throws Exception {
        IPlMapper plDao = sqlSession.getMapper(IPlMapper.class);
        // 실행한 build와 deploy 건만 조회
        PlRunVO runDetail = plDao.getPlRunDetail(plRunSeq, "Y");

        StringBuffer stringBuffer = new StringBuffer();

        // 빌드 로그 취합
        List<PlRunBuildVO> runBuilds = runDetail.getPlRunBuilds();
        if (CollectionUtils.isNotEmpty(runBuilds)){
            runBuilds.stream().sorted(Comparator.comparing(PlRunBuildVO::getRunOrder)).forEachOrdered(vo -> { if (vo.getRunLog() != null) stringBuffer.append(vo.getRunLog()); } );
        }

        // 배포 그 취합
        List<PlRunDeployVO> runDeploys = runDetail.getPlRunDeploys();
        if (CollectionUtils.isNotEmpty(runDeploys)){
            runDeploys.stream().sorted(Comparator.comparing(PlRunDeployVO::getRunOrder)).forEachOrdered(vo -> { if (vo.getRunLog() != null) stringBuffer.append(vo.getRunLog()); } );
        }

        return stringBuffer.toString();
    }

    /**
     *
     * 현재 executing context 에 pl_run 정보 기반의 userSeq, userRole, userServiceSeq 값을 셋팅.<br/>
     *
     * @param plRunSeq
     *
     */
    public void setUserInfosToExcuteContext(Integer plRunSeq){

        IPlMapper plDao = sqlSession.getMapper(IPlMapper.class);
        IServiceMapper serviceDao = sqlSession.getMapper(IServiceMapper.class);

        PlRunVO plRunVO = plDao.getPlRunDetail(plRunSeq, null);
        PlMasterVO plDetail = plDao.getPlDetail(plRunVO.getPlSeq());


        if (plRunVO != null){

            Integer userSeq = plRunVO.getCreator();
            String userRole = null;
            Integer userWorkspaceSeq = null;

            try {
                // 사용자 권한 조회
                UserVO userVO = userService.getByUserSeq(userSeq);
                userRole = userVO.getUserRole();

//                // user-workspace 조회
//                ServiceVO registryUser = serviceDao.getRegistryUserByNamespace(plDetail.getClusterSeq(), plDetail.getNamespace());
//                userWorkspaceSeq = registryUser.getServiceSeq();

                // executing context 셋팅
                ContextHolder.exeContext().setUserSeq(userSeq);
                ContextHolder.exeContext().setUserRole(userRole);
//                ContextHolder.exeContext().setUserServiceSeq(userWorkspaceSeq);

            } catch (Exception e) {
                log.error("trace log ", e);
            }

        }

    }

    /**
     * Builder 서버로 부터 PL 에 해당하는 Build 실행시 각 step 에 대한 처리 결과 응답 처리하는 메서드.<br/>
     * build에 대한 처리이니 기본적으로 buildservice 쪽으로 넘겨서 build 관련 테이블들은 처리하고,<br/>
     * Pl 에러나 완료시에 PL관련 처리를 진행한다.<br/><br/>
     * 현재 빌드건 완료시 다음 빌드건이 존재 하면 다음 빌드건을 처리하고, 빌드건 없이 배포건이 존재할 경우 배포 처리를 호출하며,<br/>
     * 다음 빌드/배포 건 처리시에는 async로 처리한다.
     *
     * @param plSeq
     * @param plRunSeq
     * @param plRunBuildSeq
     * @param pipelineResult Build Server 로 부터 받은 응답 JSON 데이터
     * @param ctx
     * @throws Exception
     */
    public void handleBuildResult(Integer plSeq, Integer plRunSeq, Integer plRunBuildSeq, String pipelineResult, ExecutingContextVO ctx) throws Exception {
        IPlMapper plDao = sqlSession.getMapper(IPlMapper.class);

        // pipeline response 생성
        PipelineAPIServiceProto.Pipeline pipeline = PipelineTypeConverter.convertVO(pipelineResult, PipelineAPIServiceProto.Pipeline.newBuilder()).build();

        // pipeline command 생성
        PipelineCommandVO pipelineCommandVO = PipelineTypeConverter.convertToPipelineCommandVO(pipeline, "PL");
        pipelineCommandVO.setPlSeq(plSeq);
        pipelineCommandVO.setPlRunSeq(plRunSeq);
        pipelineCommandVO.setPlRunBuildSeq(plRunBuildSeq);

        // 실행 대상 PlRun 정보 조회
        PlRunVO plRunVO = plDao.getPlRunDetail(plRunSeq, "Y");

        // 현재 PlRunStatus
        PlStatus currPlRunStatus = plRunVO.getRunStatus();

        // 현재 빌드 정보 조회 및 비교조건 값 셋팅
        PlRunBuildVO plRunBuildVO = plRunVO.getPlRunBuilds().stream().filter(vo -> (vo.getPlRunBuildSeq().compareTo(plRunBuildSeq) == 0)).findFirst().orElseGet(() ->new PlRunBuildVO());
        pipelineCommandVO.setBuildRunSeq(plRunBuildVO.getBuildRunSeq()); // buildRunSeq 셋팅

        // build 테이블 관련해서는 handleBuildResult 메서드에서 처리됨. buildRun 상태 & 각 스텝 상태 & 로그처리 등...
        buildService.handleBuildResult(plRunBuildVO.getBuildRunSeq(), pipelineResult);

        // build step 이 시작될 때마다 Pl 이벤트 전송
        if (pipelineCommandVO.getRunState() == null && pipelineCommandVO.getStepState() == StepState.RUNNING){
            // Pl 이벤트 발송
            plEventService.sendPlRunState(plRunVO.getCallbackUrl(), plRunVO.getPlSeq(), plRunVO.getPlRunSeq(), plRunVO.getRunStatus(), this.makeEventResVO(pipelineCommandVO));

        } else if (pipelineCommandVO.getRunState() == RunState.ERROR || pipelineCommandVO.getRunState() == RunState.DONE){ // 빌드 종료(ERROR 이나 DONE 상태)일때 공통 처리 부분

            // buildRun contents 엽데이트
            BuildRunVO tmpBuildRun = buildService.getBuildRun(plRunBuildVO.getBuildRunSeq(), "Y", false);
            String buildCont = JsonUtils.toGson(tmpBuildRun);
            plDao.updatePlRunBuildForbuildCont(plRunBuildVO.getPlRunBuildSeq(), buildCont);

            if(pipelineCommandVO.getRunState() == RunState.ERROR) { // 빌드가 ERROR 이거나 완료 되었을때만 처리된다.
                plRunBuildVO.setRunStatus(PlStatus.ERROR);

                // PlRunBuild 상태 & log 업데이트
                plDao.updatePlRunBuildStatus(plRunBuildSeq, PlStatus.ERROR.getCode());
                plRunBuildService.updatePlRunBuildLog(plRunBuildVO);

                /** PlRun 상태 업데이트, 빌드 오류로 인해 파이프라인 실행도 종료 처리.
                 *  PlRun 상태가 CANCELED 아니면  ERROR 로 Update */
                if (currPlRunStatus != PlStatus.CANCELED ) {
                    currPlRunStatus = PlStatus.ERROR;
                }

                plDao.updatePlRunStatus(currPlRunStatus.getCode(), plRunSeq);
                plRunVO.setRunStatus(currPlRunStatus);

                // run 리소스 정보로 work 리소스 update
                this.applyFromPlRunToPlMaster(plRunVO.getPlSeq(), plRunVO.getPlRunSeq());

            } else if(pipelineCommandVO.getRunState() == RunState.DONE) { // 빌드가 완료 되었을때만 처리된다.

                plRunBuildVO.setRunStatus(PlStatus.DONE);

                // 실행할 다음 build 존재 여부 & 실행할 배포 존재 여부 설정
                boolean existsNextBuild = plRunVO.getPlRunBuilds().stream().anyMatch(vo -> vo.getRunOrder() > plRunBuildVO.getRunOrder());
                boolean existsDeploys = CollectionUtils.isNotEmpty(plRunVO.getPlRunDeploys());

                // PlRunBuild 상태 & imgUrl & log 업데이트
                plDao.updatePlRunBuildStatus( plRunBuildSeq, PlStatus.DONE.getCode());
                plDao.updatePlRunBuildForBuildTagAndImgUrl(plRunBuildSeq, tmpBuildRun.getTagName(), tmpBuildRun.getImageUrl());
                plRunBuildService.updatePlRunBuildLog(plRunBuildVO);


                /** PlRun 상태가 CANCEL 이거나, 실행할 다음 build 존재 하지 않고, 배포할 건도 없으면, PlRun 종료 처리 **/
                if ( currPlRunStatus == PlStatus.CANCELED || (!existsNextBuild && !existsDeploys) ){

                    /** PlRun 상태가 CANCELED 아니면 DONE 로 Update */
                    if (currPlRunStatus != PlStatus.CANCELED) {
                        currPlRunStatus = PlStatus.DONE;
                    }

                    // PlRun 상태 업데이트
                    plDao.updatePlRunStatus(currPlRunStatus.getCode(), plRunSeq);
                    plRunVO.setRunStatus(currPlRunStatus);

                    // run 리소스 정보로 work 리소스 update
                    this.applyFromPlRunToPlMaster(plRunVO.getPlSeq(), plRunVO.getPlRunSeq());

                } else if (existsNextBuild){
                    // 다음 빌드 작업이 존재 할 경우, 다음 빌드 작업 호출
                    plAsyncService.runPl(PlRunMode.BUILD, plRunSeq, ctx); // Async 처리

                } else if (existsDeploys){
                    // build 작업이 완료 되었을때는 Deploy 리소스 배포 처리
                    plAsyncService.runPl(PlRunMode.DEPLOY, plRunSeq, ctx); // Async 처리
                }
            }

            // Pl 이벤트 발송
            plEventService.sendPlRunState(plRunVO.getCallbackUrl(), plRunVO.getPlSeq(), plRunVO.getPlRunSeq(), plRunVO.getRunStatus(), this.makeEventResVO(pipelineCommandVO));
        }

    }

    private PlEventResVO makeEventResVO(PipelineCommandVO pipelineCommandVO){
        Integer buildStepRunSeq = pipelineCommandVO.getBuildStepRunSeq();

        PlEventResVO eventResVO = new PlEventResVO();
        eventResVO.setResType("BUILD");
        eventResVO.setBuildStepType(pipelineCommandVO.getStepType());
        eventResVO.setBuildStepState(pipelineCommandVO.getStepState());

        // buildName 와 step title 셋팅
        try {
            BuildRunVO buildRunVO = buildService.getBuildRun(pipelineCommandVO.getBuildRunSeq());
            eventResVO.setResName(buildRunVO.getBuildName());
            eventResVO.setResState(buildRunVO.getRunState().getCode());

            Optional<BuildStepRunVO> optStepRunVO = buildRunVO.getBuildStepRuns().stream().filter(vo -> vo.getBuildStepRunSeq().equals(buildStepRunSeq)).findAny();
            if(optStepRunVO.isPresent()) {
                BuildStepRunVO stepRunVO = optStepRunVO.get();
                eventResVO.setBuildStepTitle(stepRunVO.getBuildStepConfig().getStepTitle());
            }

        } catch (IOException e) {
            log.error("빌드실행정보 조회 오류", e);
        }

        return eventResVO;
    }

    /**
     * 파이프라인에 특정 버전정보가 존재 하는지 체크하는 메서드, 존재할 경우 파이프라인 마스터 정보를 리턴한다.
     *
     * @param plSeq
     * @param version
     * @return
     * @throws Exception
     */
    public PlMasterVO existPlVersion(Integer plSeq, String version) throws Exception {
        IPlMapper plDao = sqlSession.getMapper(IPlMapper.class);
        return plDao.existPlVersion(plSeq, version);
    }

    /**
     * 파이프라인 롤백 실행 테이블 정보로 작업테이블을 delete&insert 하는 메서드
     *
     * @param plSeq
     * @param plRunSeq
     * @param version
     * @throws Exception
     */
    @Transactional(transactionManager = "transactionManager")
    public void rollbackFromPlRunToPlMaster(Integer plSeq, Integer plRunSeq, String version) throws Exception {
        IPlMapper plDao = sqlSession.getMapper(IPlMapper.class);

        // run 정보 조회
        PlRunVO runVO = plDao.getPlRunDetail(plRunSeq, null);

        // res_build 삭제 (mapping 도 같이 삭제, because of cascade)
        plDao.deletePlResBuilds(plSeq);

        // res_deploy 삭제
        plDao.deletePlResDeploys(plSeq);

        // res_build insert
        plDao.insertPlResBuildsFromRunBuild(plRunSeq);

        // res_deploy insert
        plDao.insertPlResDeploysFromRunDeploy(plRunSeq);

        // res_mapping insert, res_build& res_deploy && run_build & run_deploy & pl_run_build_deploy_mapping 참조하여 pl_res_build_deploy_mapping 정보 테이블 생성 쿼리 작성해줘야함.
        plDao.insertPlResBuildDeployMappingFromRun(plRunSeq);

        // version 파라메터가 있으면 파라메터 사용
        String ver = runVO.getVer();
        if (version != null) {
            ver = version;
        }

        // pl_master update
        plDao.updatePlMasterForRunSeqAndVersion(plSeq, plRunSeq, ver);

    }

    /**
     * 파이프라인 실행 완료후 실행 테이블 정보로 작업테이블을 update 하는 메서드
     *
     * @param plSeq
     * @param plRunSeq
     * @throws Exception
     */
    @Transactional(transactionManager = "transactionManager")
    public void applyFromPlRunToPlMaster(Integer plSeq, Integer plRunSeq) throws Exception {
        IPlMapper plDao = sqlSession.getMapper(IPlMapper.class);

        // run 정보 조회
        PlRunVO runVO = plDao.getPlRunDetail(plRunSeq, null);

        if (runVO != null) {

            // 실행한 build, deploy 리소스로 work 리소스테이블을 update 한다.
            plDao.updatePlResBuildsFromRunBuilds(plSeq, plRunSeq);

            // 사실 deploy update는 필요할까 싶다.. ㅡㅡ;;;;;;
            plDao.updatePlResDeploysFromRunDeploys(plSeq, plRunSeq);

            // version 파라메터가 있으면 파라메터 사용
            String ver = runVO.getVer();

            // pl_master update
            plDao.updatePlMasterForRunSeqAndVersion(plSeq, plRunSeq, ver);

        }

    }

    private BuildRunVO getBuildRunFromBuildCont(String buildCont) throws Exception {
        BuildRunVO buildRun = null;

        if (StringUtils.isNotEmpty(buildCont)) {
            try {
                buildRun = JsonUtils.fromGson(buildCont, BuildRunVO.class);
            }catch (Exception e){
                log.debug(e.getMessage(), e);
            }
        }

        return buildRun;
    }

    private void throwPipelineResNotFound(String resourceTypeName) throws Exception {
        String notFoundMsg = String.format("The pipeline %s resource could not be found.", resourceTypeName);
        throw new CocktailException(notFoundMsg, ExceptionType.CommonNotFound, ExceptionBiz.PIPELINE_SET, notFoundMsg);
    }


    /**
     * plversion 으로 사용할 DateTime 형식의 version 정보 생성
     *
     * @return
     */
    public String newPlversion(){
        ZonedDateTime zdt = ZonedDateTime.now(ZoneId.of(cocktailServiceProperties.getRegionTimeZone()));
        String dateTimeVersion = String.format("release-%tY%<tm%<td-%<tH%<tM%<tS", zdt);

        return dateTimeVersion;
    }
}
