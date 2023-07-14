package run.acloud.api.pl.service;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
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
import org.springframework.stereotype.Service;
import run.acloud.api.build.enums.RunState;
import run.acloud.api.build.vo.BuildRunVO;
import run.acloud.api.configuration.dao.IClusterMapper;
import run.acloud.api.configuration.service.ClusterStateService;
import run.acloud.api.configuration.service.ClusterVolumeService;
import run.acloud.api.configuration.vo.ClusterVO;
import run.acloud.api.configuration.vo.ClusterVolumeVO;
import run.acloud.api.cserver.dao.IServiceMapper;
import run.acloud.api.cserver.enums.DeployType;
import run.acloud.api.cserver.enums.WorkloadType;
import run.acloud.api.cserver.enums.WorkloadVersion;
import run.acloud.api.cserver.service.ServerConversionService;
import run.acloud.api.cserver.util.ServerUtils;
import run.acloud.api.cserver.vo.ServerGuiVO;
import run.acloud.api.cserver.vo.ServerIntegrateVO;
import run.acloud.api.cserver.vo.ServerYamlVO;
import run.acloud.api.k8sextended.models.*;
import run.acloud.api.pipelineflow.service.PipelineFlowService;
import run.acloud.api.pipelineflow.service.WrappedBuildService;
import run.acloud.api.pl.dao.IPlMapper;
import run.acloud.api.pl.enums.PlResType;
import run.acloud.api.pl.enums.PlStatus;
import run.acloud.api.pl.vo.*;
import run.acloud.api.resource.constants.KubeConstants;
import run.acloud.api.resource.enums.*;
import run.acloud.api.resource.service.*;
import run.acloud.api.resource.task.K8sWorker;
import run.acloud.api.resource.util.K8sJsonUtils;
import run.acloud.api.resource.util.ResourceUtil;
import run.acloud.api.resource.vo.*;
import run.acloud.commons.util.CryptoUtils;
import run.acloud.commons.util.Utils;
import run.acloud.commons.vo.ExecutingContextVO;
import run.acloud.framework.context.ContextHolder;
import run.acloud.framework.enums.ExceptionBiz;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;
import run.acloud.framework.util.ExceptionMessageUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PlRunDeployService {

    @Resource(name = "cocktailSession")
    private SqlSessionTemplate sqlSession;

    @Autowired
    private WorkloadResourceService workloadResourceService;

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
    private ClusterStateService clusterStateService;

    @Autowired
    private K8sWorker k8sWorker;

    @Autowired
    private PlEventService plEventService;

    @Autowired
    private PlService plService;

    @Autowired
    private WrappedBuildService buildService;

    @Autowired
    private PipelineFlowService pipelineFlowService;

    public void runPlDeploy(Integer plRunSeq, String callbackUrl, ExecutingContextVO context) {

        IPlMapper plDao = sqlSession.getMapper(IPlMapper.class);
        IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
        IServiceMapper serviceDao = sqlSession.getMapper(IServiceMapper.class);

        PlRunVO plRunVO = plDao.getPlRunDetail(plRunSeq, null);

        JSON k8sJson = new JSON();

        CocktailException ce;

        try {
            if (plRunVO != null && CollectionUtils.isNotEmpty(plRunVO.getPlRunDeploys())) {

                PlMasterVO plDetail = plDao.getPlDetail(plRunVO.getPlSeq());
                ClusterVO cluster = clusterDao.getCluster(plDetail.getClusterSeq());
                cluster.setNamespaceName(plDetail.getNamespace());

                // dry-run 지원여부 체크
                if (!ResourceUtil.isSupportedDryRun(cluster.getK8sVersion())) {
                    ce = new CocktailException("Cluster does not support dryrun", ExceptionType.K8sNotSupported, ExceptionBiz.PIPELINE_SET);
                    throw ce;
                }

                //TODO 빌드 && 파이프라인 실행 상태 확인

                // 리소스 유형별로 Map으로 셋팅
                Map<PlResType, Map<String, PlRunDeployVO>> resTypeListMap = Maps.newHashMap();
                List<PlRunDeployVO> workloads = Lists.newArrayList();
                for (PlRunDeployVO res : plRunVO.getPlRunDeploys()) {
                    if (BooleanUtils.toBoolean(res.getRunYn())) {
                        PlResType plResType = PlResType.valueOf(res.getResType());
                        resTypeListMap.computeIfAbsent(plResType, (value) -> Maps.newHashMap());
                        resTypeListMap.get(plResType).put(res.getResName(), res);

                        // runOrder 순으로 배포를 위해 workload만 별로의 배열로 셋팅
                        if (plResType.isWorkload()) {
                            workloads.add(res);
                        }
                    }
                }

                // runOrder로 정렬
                workloads = workloads.stream().sorted(Comparator.comparing(PlRunDeployVO::getRunOrder)).collect(Collectors.toList());

                // 워크스페이스에 할당된 registry project ID
                String decryptedRegistryUser = CryptoUtils.decryptAES(cluster.getAccount().getRegistryDownloadUserId());

                // Map<ResType, Map<ResName, K8sObject>>
                Map<String, Map<String, ?>> currResMap = Maps.newHashMap();

                // check cluster state
                clusterStateService.checkClusterState(cluster);

                /**
                 * 현재 대상 k8s resource 정보 조회
                 */
                // configMap
                if (MapUtils.getObject(resTypeListMap, PlResType.CM, null) != null) {
                    PlResType plResType = PlResType.CM;
                    List<ConfigMapGuiVO> k8sResources = configMapService.getConfigMaps(cluster, cluster.getNamespaceName(), null, null);
                    currResMap.put(plResType.getValue(), k8sResources.stream().filter(c -> resTypeListMap.get(plResType).containsKey(c.getName())).collect(Collectors.toMap(ConfigMapGuiVO::getName, Function.identity())));
                }
                // secret
                if (MapUtils.getObject(resTypeListMap, PlResType.SC, null) != null) {
                    PlResType plResType = PlResType.SC;
                    List<SecretGuiVO> k8sResources = secretService.getSecrets(cluster, cluster.getNamespaceName(), null, null, false);
                    currResMap.put(plResType.getValue(), k8sResources.stream().filter(c -> resTypeListMap.get(plResType).containsKey(c.getName())).collect(Collectors.toMap(SecretGuiVO::getName, Function.identity())));
                }
                // persistent volume claim
//                if (MapUtils.getObject(resTypeListMap, PlResType.PVC, null) != null) {
//                        PlResType plResType = PlResType.PVC;
//                        currResMap.computeIfAbsent(plResType.getValue(), (value) -> Maps.newHashMap());
//                        List<SecretGuiVO> currK8sRes = secretService.getSecrets(cluster, cluster.getNamespaceName(), null, null, false);
//                        currResMap.get(plResType.getValue()).putAll(currK8sRes.stream().filter(c -> resTypeListMap.get(plResType).containsKey(c.getName())).collect(Collectors.toMap(SecretGuiVO::getName, Function.identity())));
//                }
                // workload
                if (CollectionUtils.isNotEmpty(workloads)) {

                    // deployment
                    if (MapUtils.getObject(resTypeListMap, PlResType.REPLICA_SERVER, null) != null) {
                        PlResType plResType = PlResType.REPLICA_SERVER;
                        List<K8sDeploymentVO> k8sResources = workloadResourceService.getDeployments(cluster, cluster.getNamespaceName(), null, null, ContextHolder.exeContext());
                        currResMap.put(plResType.getValue(), k8sResources.stream().filter(c -> resTypeListMap.get(plResType).containsKey(c.getName())).collect(Collectors.toMap(K8sDeploymentVO::getName, Function.identity())));
                    }
                    // statefulSet
                    if (MapUtils.getObject(resTypeListMap, PlResType.STATEFUL_SET_SERVER, null) != null) {
                        PlResType plResType = PlResType.STATEFUL_SET_SERVER;
                        List<K8sStatefulSetVO> k8sResources = workloadResourceService.getStatefulSets(cluster, cluster.getNamespaceName(), null, null, ContextHolder.exeContext());
                        currResMap.put(plResType.getValue(), k8sResources.stream().filter(c -> resTypeListMap.get(plResType).containsKey(c.getName())).collect(Collectors.toMap(K8sStatefulSetVO::getName, Function.identity())));
                    }
                    // daemonSet
                    if (MapUtils.getObject(resTypeListMap, PlResType.DAEMON_SET_SERVER, null) != null) {
                        PlResType plResType = PlResType.DAEMON_SET_SERVER;
                        List<K8sDaemonSetVO> k8sResources = workloadResourceService.getDaemonSets(cluster, cluster.getNamespaceName(), null, null, ContextHolder.exeContext());
                        currResMap.put(plResType.getValue(), k8sResources.stream().filter(c -> resTypeListMap.get(plResType).containsKey(c.getName())).collect(Collectors.toMap(K8sDaemonSetVO::getName, Function.identity())));
                    }
                    // job
                    if (MapUtils.getObject(resTypeListMap, PlResType.JOB_SERVER, null) != null) {
                        PlResType plResType = PlResType.JOB_SERVER;
                        List<K8sJobVO> k8sResources = workloadResourceService.getJobs(cluster, cluster.getNamespaceName(), null, null, ContextHolder.exeContext());
                        currResMap.put(plResType.getValue(), k8sResources.stream().filter(c -> resTypeListMap.get(plResType).containsKey(c.getName())).collect(Collectors.toMap(K8sJobVO::getName, Function.identity())));
                    }
                    // cronJob
                    if (MapUtils.getObject(resTypeListMap, PlResType.CRON_JOB_SERVER, null) != null) {
                        PlResType plResType = PlResType.CRON_JOB_SERVER;
                        List<K8sCronJobVO> k8sResources = workloadResourceService.getCronJobs(cluster, cluster.getNamespaceName(), null, null, ContextHolder.exeContext());
                        currResMap.put(plResType.getValue(), k8sResources.stream().filter(c -> resTypeListMap.get(plResType).containsKey(c.getName())).collect(Collectors.toMap(K8sCronJobVO::getName, Function.identity())));
                    }
                    // hpa - 워크로드가 없는 경우에 대비하여 별도 조회
                    if (MapUtils.getObject(resTypeListMap, PlResType.REPLICA_SERVER, null) != null || MapUtils.getObject(resTypeListMap, PlResType.STATEFUL_SET_SERVER, null) != null) {
                        currResMap.put(K8sApiKindType.HORIZONTAL_POD_AUTOSCALER.getValue(), workloadResourceService.convertHorizontalPodAutoscalerDataMap(cluster, cluster.getNamespaceName(), null, null));
                    }
                }
                // service
                if (MapUtils.getObject(resTypeListMap, PlResType.SVC, null) != null) {
                    PlResType plResType = PlResType.SVC;
                    List<K8sServiceVO> k8sResources = serviceSpecService.getServices(cluster, cluster.getNamespaceName(), null, null, ContextHolder.exeContext());
                    currResMap.put(plResType.getValue(), k8sResources.stream().filter(c -> resTypeListMap.get(plResType).containsKey(c.getServiceName())).collect(Collectors.toMap(K8sServiceVO::getServiceName, Function.identity())));
                }
                // ingress
                if (MapUtils.getObject(resTypeListMap, PlResType.IG, null) != null) {
                    PlResType plResType = PlResType.IG;
                    List<K8sIngressVO> k8sResources = ingressSpecService.getIngresses(cluster, cluster.getNamespaceName(), ContextHolder.exeContext());
                    currResMap.put(plResType.getValue(), k8sResources.stream().filter(c -> resTypeListMap.get(plResType).containsKey(c.getName())).collect(Collectors.toMap(K8sIngressVO::getName, Function.identity())));
                }

                /**
                 * 빌드가 연결된 워크로드일 경우 이미지 변경하여 yaml 업데이트 처리
                 */
                this.updateWorkloadContentsChangeBuildImage(workloads, decryptedRegistryUser, k8sJson, plDao);

                /**
                 * dry-run
                 */
                this.runDeployProcess(cluster, cluster.getNamespaceName(), context.getUserServiceSeq(), plRunVO, resTypeListMap, workloads, currResMap, decryptedRegistryUser, true, k8sJson, plDao, context);

                /**
                 * run
                 */
                this.runDeployProcess(cluster, cluster.getNamespaceName(), context.getUserServiceSeq(), plRunVO, resTypeListMap, workloads, currResMap, decryptedRegistryUser, false, k8sJson, plDao, context);

                // end
                plDao.updatePlRunStatus(PlStatus.DONE.getCode(), plRunSeq);

                // res 테이블 반영
                plService.applyFromPlRunToPlMaster(plRunVO.getPlSeq(), plRunSeq);

                // send event
                plEventService.sendPlRunState(plRunVO.getCallbackUrl(), plRunVO.getPlSeq(), plRunSeq, PlStatus.DONE);
            }

        } catch (Exception e) {
            if(e instanceof CocktailException){
                ce = (CocktailException) e;
            } else {
                ce = new CocktailException(e.getMessage(), e, ExceptionType.CommonUpdateFail, ExceptionBiz.PIPELINE_SET);
            }
//            e.printStackTrace();
            log.error(ExceptionMessageUtils.setCommonResult(null, null, ce, false), ce);

            plDao.updatePlRunStatus(PlStatus.ERROR.getCode(), plRunSeq);

            // send event
            plEventService.sendPlRunState(plRunVO.getCallbackUrl(), plRunVO.getPlSeq(), plRunSeq, PlStatus.ERROR);
        }
    }

    /**
     * 빌드가 연결된 워크로드일 경우 이미지 변경하여 yaml 업데이트 처리
     *
     * @param workloads
     * @param registryUser
     * @param k8sJson
     * @param plDao
     * @throws Exception
     */
    public void updateWorkloadContentsChangeBuildImage(List<PlRunDeployVO> workloads, String registryUser, JSON k8sJson, IPlMapper plDao) throws Exception {

        if (CollectionUtils.isNotEmpty(workloads)) {

            StringBuilder yamlStr;

            for (PlRunDeployVO runDeploy : workloads) {
                if (CollectionUtils.isNotEmpty(runDeploy.getPlRunBuilds())) {

                    yamlStr = new StringBuilder();

                    PlResType plResType = PlResType.valueOf(runDeploy.getResType());
                    List<Object> objs = ServerUtils.getYamlObjects(runDeploy.getResCont());

                    if (plResType == PlResType.REPLICA_SERVER) {

                        // 변경된 yaml을 객체로 변환
                        V1Deployment uptDeployment = null;
                        V2HorizontalPodAutoscaler uptHpaV2 = null;
                        V2beta2HorizontalPodAutoscaler uptHpaV2beta2 = null;
                        V2beta1HorizontalPodAutoscaler uptHpaV2beta1 = null;
                        V1HorizontalPodAutoscaler uptHpaV1 = null;

                        if (CollectionUtils.isNotEmpty(objs)) {
                            for (Object obj : objs) {
                                if (obj != null) {
                                    Map<String, Object> k8sObjectToMap = ServerUtils.getK8sObjectToMap(obj, k8sJson);
                                    K8sApiKindType kind = ServerUtils.getK8sKindInMap(k8sObjectToMap);
                                    K8sApiGroupType apiGroupType = ServerUtils.getK8sGroupInMap(k8sObjectToMap);
                                    K8sApiType apiType = ServerUtils.getK8sVersionInMap(k8sObjectToMap);
                                    V1ObjectMeta objectMeta = ServerUtils.getK8sObjectMetaInMap(k8sObjectToMap, k8sJson);

                                    switch (kind) {
                                        case DEPLOYMENT:
                                            if (objectMeta != null && StringUtils.equals(objectMeta.getName(), runDeploy.getResName())) {
                                                if (apiGroupType == K8sApiGroupType.APPS && apiType == K8sApiType.V1) {
                                                    uptDeployment = (V1Deployment) obj;
                                                }
                                            }
                                            break;
                                        case HORIZONTAL_POD_AUTOSCALER:
                                            if (K8sApiType.V2 == apiType) {
                                                if (StringUtils.equals(Optional.ofNullable(((V2HorizontalPodAutoscaler) obj).getSpec()).map(V2HorizontalPodAutoscalerSpec::getScaleTargetRef).map(V2CrossVersionObjectReference::getName).orElseGet(() ->"")
                                                        , runDeploy.getResName())) {
                                                    uptHpaV2 = (V2HorizontalPodAutoscaler) obj;
                                                }
                                            } else if (K8sApiType.V2BETA2 == apiType) {
                                                if (StringUtils.equals(Optional.ofNullable(((V2beta2HorizontalPodAutoscaler) obj).getSpec()).map(V2beta2HorizontalPodAutoscalerSpec::getScaleTargetRef).map(V2beta2CrossVersionObjectReference::getName).orElseGet(() ->"")
                                                        , runDeploy.getResName())) {
                                                    uptHpaV2beta2 = (V2beta2HorizontalPodAutoscaler) obj;
                                                }
                                            } else if (K8sApiType.V2BETA1 == apiType) {
                                                if (StringUtils.equals(Optional.ofNullable(((V2beta1HorizontalPodAutoscaler) obj).getSpec()).map(V2beta1HorizontalPodAutoscalerSpec::getScaleTargetRef).map(V2beta1CrossVersionObjectReference::getName).orElseGet(() ->"")
                                                        , runDeploy.getResName())) {
                                                    uptHpaV2beta1 = (V2beta1HorizontalPodAutoscaler) obj;
                                                }
                                            } else {
                                                if (StringUtils.equals(Optional.ofNullable(((V1HorizontalPodAutoscaler) obj).getSpec()).map(V1HorizontalPodAutoscalerSpec::getScaleTargetRef).map(V1CrossVersionObjectReference::getName).orElseGet(() ->"")
                                                        , runDeploy.getResName())) {
                                                    uptHpaV1 = (V1HorizontalPodAutoscaler) obj;
                                                }
                                            }
                                            break;
                                    }
                                }
                            }

                            if (uptDeployment != null) {
                                if (uptDeployment.getMetadata() != null && uptDeployment.getSpec() != null) {
                                    // set container Image 및 pull secret
                                    this.setContainerImage(uptDeployment.getSpec().getTemplate(), runDeploy.getPlRunBuilds(), registryUser);

                                    // update yaml
                                    uptDeployment.getMetadata().putAnnotationsItem(KubeConstants.ANNOTATION_COCKTAIL_DEPLOY_DATETIME, Utils.getNowDateTime());
                                    yamlStr.append(ServerUtils.marshalYaml(uptDeployment));
                                }
                            }

                            if (uptHpaV2 != null) {
                                yamlStr.append("---\n").append(ServerUtils.marshalYaml(uptHpaV2));
                            } else if (uptHpaV2beta2 != null) {
                                yamlStr.append("---\n").append(ServerUtils.marshalYaml(uptHpaV2beta2));
                            } else if (uptHpaV2beta1 != null) {
                                yamlStr.append("---\n").append(ServerUtils.marshalYaml(uptHpaV2beta1));
                            } else if (uptHpaV1 != null) {
                                yamlStr.append("---\n").append(ServerUtils.marshalYaml(uptHpaV1));
                            }
                            plDao.updatePlRunDeployContents(yamlStr.toString(), runDeploy.getPlRunDeploySeq());
                            runDeploy.setResCont(yamlStr.toString());
                        }
                    } else if (plResType == PlResType.STATEFUL_SET_SERVER) {

                        // 변경된 yaml을 객체로 변환
                        V1StatefulSet uptStatefulSet = null;
                        V2HorizontalPodAutoscaler uptHpaV2 = null;
                        V2beta2HorizontalPodAutoscaler uptHpaV2beta2 = null;
                        V2beta1HorizontalPodAutoscaler uptHpaV2beta1 = null;
                        V1HorizontalPodAutoscaler uptHpaV1 = null;

                        if (CollectionUtils.isNotEmpty(objs)) {
                            for (Object obj : objs) {
                                if (obj != null) {
                                    Map<String, Object> k8sObjectToMap = ServerUtils.getK8sObjectToMap(obj, k8sJson);
                                    K8sApiKindType kind = ServerUtils.getK8sKindInMap(k8sObjectToMap);
                                    K8sApiGroupType apiGroupType = ServerUtils.getK8sGroupInMap(k8sObjectToMap);
                                    K8sApiType apiType = ServerUtils.getK8sVersionInMap(k8sObjectToMap);
                                    V1ObjectMeta objectMeta = ServerUtils.getK8sObjectMetaInMap(k8sObjectToMap, k8sJson);

                                    switch (kind) {
                                        case STATEFUL_SET:
                                            if (objectMeta != null && StringUtils.equals(objectMeta.getName(), runDeploy.getResName())) {
                                                if (apiGroupType == K8sApiGroupType.APPS && apiType == K8sApiType.V1) {
                                                    uptStatefulSet = (V1StatefulSet) obj;
                                                }
                                            }
                                            break;
                                        case HORIZONTAL_POD_AUTOSCALER:
                                            if (K8sApiType.V2 == apiType) {
                                                if (StringUtils.equals(Optional.ofNullable(((V2HorizontalPodAutoscaler) obj).getSpec()).map(V2HorizontalPodAutoscalerSpec::getScaleTargetRef).map(V2CrossVersionObjectReference::getName).orElseGet(() ->"")
                                                        , runDeploy.getResName())) {
                                                    uptHpaV2 = (V2HorizontalPodAutoscaler) obj;
                                                }
                                            } else if (K8sApiType.V2BETA2 == apiType) {
                                                if (StringUtils.equals(Optional.ofNullable(((V2beta2HorizontalPodAutoscaler) obj).getSpec()).map(V2beta2HorizontalPodAutoscalerSpec::getScaleTargetRef).map(V2beta2CrossVersionObjectReference::getName).orElseGet(() ->"")
                                                        , runDeploy.getResName())) {
                                                    uptHpaV2beta2 = (V2beta2HorizontalPodAutoscaler) obj;
                                                }
                                            } else if (K8sApiType.V2BETA1 == apiType) {
                                                if (StringUtils.equals(Optional.ofNullable(((V2beta1HorizontalPodAutoscaler) obj).getSpec()).map(V2beta1HorizontalPodAutoscalerSpec::getScaleTargetRef).map(V2beta1CrossVersionObjectReference::getName).orElseGet(() ->"")
                                                        , runDeploy.getResName())) {
                                                    uptHpaV2beta1 = (V2beta1HorizontalPodAutoscaler) obj;
                                                }
                                            } else {
                                                if (StringUtils.equals(Optional.ofNullable(((V1HorizontalPodAutoscaler) obj).getSpec()).map(V1HorizontalPodAutoscalerSpec::getScaleTargetRef).map(V1CrossVersionObjectReference::getName).orElseGet(() ->"")
                                                        , runDeploy.getResName())) {
                                                    uptHpaV1 = (V1HorizontalPodAutoscaler) obj;
                                                }
                                            }
                                            break;
                                    }
                                }
                            }

                            if (uptStatefulSet != null) {
                                if (uptStatefulSet.getMetadata() != null && uptStatefulSet.getSpec() != null) {
                                    // set container Image 및 pull secret
                                    this.setContainerImage(uptStatefulSet.getSpec().getTemplate(), runDeploy.getPlRunBuilds(), registryUser);

                                    // update yaml
                                    uptStatefulSet.getMetadata().putAnnotationsItem(KubeConstants.ANNOTATION_COCKTAIL_DEPLOY_DATETIME, Utils.getNowDateTime());
                                    yamlStr.append(ServerUtils.marshalYaml(uptStatefulSet));
                                }
                            }

                            if (uptHpaV2 != null) {
                                yamlStr.append("---\n").append(ServerUtils.marshalYaml(uptHpaV2));
                            } else if (uptHpaV2beta2 != null) {
                                yamlStr.append("---\n").append(ServerUtils.marshalYaml(uptHpaV2beta2));
                            } else if (uptHpaV2beta1 != null) {
                                yamlStr.append("---\n").append(ServerUtils.marshalYaml(uptHpaV2beta1));
                            } else if (uptHpaV1 != null) {
                                yamlStr.append("---\n").append(ServerUtils.marshalYaml(uptHpaV1));
                            }

                            plDao.updatePlRunDeployContents(yamlStr.toString(), runDeploy.getPlRunDeploySeq());
                            runDeploy.setResCont(yamlStr.toString());

                        }

                    } else if (plResType == PlResType.DAEMON_SET_SERVER) {

                        // 변경된 yaml을 객체로 변환
                        V1DaemonSet uptDaemonSet = null;

                        if (CollectionUtils.isNotEmpty(objs)) {
                            for (Object obj : objs) {
                                if (obj != null) {
                                    Map<String, Object> k8sObjectToMap = ServerUtils.getK8sObjectToMap(obj, k8sJson);
                                    K8sApiKindType kind = ServerUtils.getK8sKindInMap(k8sObjectToMap);
                                    K8sApiGroupType apiGroupType = ServerUtils.getK8sGroupInMap(k8sObjectToMap);
                                    K8sApiType apiType = ServerUtils.getK8sVersionInMap(k8sObjectToMap);
                                    V1ObjectMeta objectMeta = ServerUtils.getK8sObjectMetaInMap(k8sObjectToMap, k8sJson);

                                    if (kind == K8sApiKindType.DAEMON_SET) {
                                        if (objectMeta != null && StringUtils.equals(objectMeta.getName(), runDeploy.getResName())) {
                                            if (apiGroupType == K8sApiGroupType.APPS && apiType == K8sApiType.V1) {
                                                uptDaemonSet = (V1DaemonSet) obj;
                                            }
                                        }
                                    }
                                }
                            }

                            if (uptDaemonSet != null) {
                                if (uptDaemonSet.getMetadata() != null && uptDaemonSet.getSpec() != null) {
                                    // set container Image 및 pull secret
                                    this.setContainerImage(uptDaemonSet.getSpec().getTemplate(), runDeploy.getPlRunBuilds(), registryUser);

                                    // update yaml
                                    uptDaemonSet.getMetadata().putAnnotationsItem(KubeConstants.ANNOTATION_COCKTAIL_DEPLOY_DATETIME, Utils.getNowDateTime());
                                    yamlStr.append(ServerUtils.marshalYaml(uptDaemonSet));
                                }
                            }

                            plDao.updatePlRunDeployContents(yamlStr.toString(), runDeploy.getPlRunDeploySeq());
                            runDeploy.setResCont(yamlStr.toString());
                        }

                    } else if (plResType == PlResType.JOB_SERVER) {

                        // 변경된 yaml을 객체로 변환
                        V1Job uptJob = null;

                        if (CollectionUtils.isNotEmpty(objs)) {
                            for (Object obj : objs) {
                                if (obj != null) {
                                    Map<String, Object> k8sObjectToMap = ServerUtils.getK8sObjectToMap(obj, k8sJson);
                                    K8sApiKindType kind = ServerUtils.getK8sKindInMap(k8sObjectToMap);
                                    K8sApiGroupType apiGroupType = ServerUtils.getK8sGroupInMap(k8sObjectToMap);
                                    K8sApiType apiType = ServerUtils.getK8sVersionInMap(k8sObjectToMap);
                                    V1ObjectMeta objectMeta = ServerUtils.getK8sObjectMetaInMap(k8sObjectToMap, k8sJson);

                                    if (kind == K8sApiKindType.JOB) {
                                        if (objectMeta != null && StringUtils.equals(objectMeta.getName(), runDeploy.getResName())) {
                                            if (apiGroupType == K8sApiGroupType.BATCH && apiType == K8sApiType.V1) {
                                                uptJob = (V1Job) obj;
                                            }
                                        }
                                    }
                                }
                            }

                            if (uptJob != null) {
                                if (uptJob.getMetadata() != null && uptJob.getSpec() != null) {
                                    // set container Image 및 pull secret
                                    this.setContainerImage(uptJob.getSpec().getTemplate(), runDeploy.getPlRunBuilds(), registryUser);

                                    // update yaml
                                    uptJob.getMetadata().putAnnotationsItem(KubeConstants.ANNOTATION_COCKTAIL_DEPLOY_DATETIME, Utils.getNowDateTime());
                                    yamlStr.append(ServerUtils.marshalYaml(uptJob));
                                }
                            }

                            plDao.updatePlRunDeployContents(yamlStr.toString(), runDeploy.getPlRunDeploySeq());
                            runDeploy.setResCont(yamlStr.toString());
                        }

                    } else if (plResType == PlResType.CRON_JOB_SERVER) {

                        // 변경된 yaml을 객체로 변환
                        V1beta1CronJob uptCronJobV1beta1 = null;
                        V1CronJob uptCronJobV1 = null;

                        if (CollectionUtils.isNotEmpty(objs)) {
                            for (Object obj : objs) {
                                if (obj != null) {
                                    Map<String, Object> k8sObjectToMap = ServerUtils.getK8sObjectToMap(obj, k8sJson);
                                    K8sApiKindType kind = ServerUtils.getK8sKindInMap(k8sObjectToMap);
                                    K8sApiGroupType apiGroupType = ServerUtils.getK8sGroupInMap(k8sObjectToMap);
                                    K8sApiType apiType = ServerUtils.getK8sVersionInMap(k8sObjectToMap);
                                    V1ObjectMeta objectMeta = ServerUtils.getK8sObjectMetaInMap(k8sObjectToMap, k8sJson);

                                    if (kind == K8sApiKindType.CRON_JOB) {
                                        if (objectMeta != null && StringUtils.equals(objectMeta.getName(), runDeploy.getResName())) {
                                            if (apiGroupType == K8sApiGroupType.BATCH && apiType == K8sApiType.V1BETA1) {
                                                uptCronJobV1beta1 = (V1beta1CronJob) obj;
                                            } else if (apiGroupType == K8sApiGroupType.BATCH && apiType == K8sApiType.V1) {
                                                uptCronJobV1 = (V1CronJob) obj;
                                            }
                                        }
                                    }
                                }
                            }

                            if (uptCronJobV1beta1 != null) {
                                if (uptCronJobV1beta1.getMetadata() != null && uptCronJobV1beta1.getSpec() != null) {
                                    // set container Image 및 pull secret
                                    this.setContainerImage(Optional.ofNullable(uptCronJobV1beta1.getSpec()).map(V1beta1CronJobSpec::getJobTemplate).map(V1beta1JobTemplateSpec::getSpec).map(V1JobSpec::getTemplate).orElseGet(() ->null), runDeploy.getPlRunBuilds(), registryUser);

                                    // update yaml
                                    uptCronJobV1beta1.getMetadata().putAnnotationsItem(KubeConstants.ANNOTATION_COCKTAIL_DEPLOY_DATETIME, Utils.getNowDateTime());
                                    yamlStr.append(ServerUtils.marshalYaml(uptCronJobV1beta1));
                                }
                            } else if (uptCronJobV1 != null) {
                                if (uptCronJobV1.getMetadata() != null && uptCronJobV1.getSpec() != null) {
                                    // set container Image 및 pull secret
                                    this.setContainerImage(Optional.ofNullable(uptCronJobV1.getSpec()).map(V1CronJobSpec::getJobTemplate).map(V1JobTemplateSpec::getSpec).map(V1JobSpec::getTemplate).orElseGet(() ->null), runDeploy.getPlRunBuilds(), registryUser);

                                    // update yaml
                                    uptCronJobV1.getMetadata().putAnnotationsItem(KubeConstants.ANNOTATION_COCKTAIL_DEPLOY_DATETIME, Utils.getNowDateTime());
                                    yamlStr.append(ServerUtils.marshalYaml(uptCronJobV1));
                                }
                            }

                            plDao.updatePlRunDeployContents(yamlStr.toString(), runDeploy.getPlRunDeploySeq());
                            runDeploy.setResCont(yamlStr.toString());
                        }

                    }
                }
            }
        }
    }

    /**
     * run Pipeline deploy
     *
     * @param cluster
     * @param namespace
     * @param serviceSeq
     * @param plRunVO
     * @param resTypeListMap
     * @param workloads
     * @param currResMap
     * @param registryUser
     * @param dryRun
     * @param k8sJson
     * @param plDao
     * @param context
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public void runDeployProcess(
            ClusterVO cluster
            , String namespace
            , Integer serviceSeq
            , PlRunVO plRunVO
            , Map<PlResType, Map<String, PlRunDeployVO>> resTypeListMap
            , List<PlRunDeployVO> workloads
            , Map<String, Map<String, ?>> currResMap
            , String registryUser
            , boolean dryRun
            , JSON k8sJson
            , IPlMapper plDao
            , ExecutingContextVO context
    ) throws Exception {

        if (plDao == null) {
            plDao = sqlSession.getMapper(IPlMapper.class);
        }

        // configMap
        if (MapUtils.getObject(resTypeListMap, PlResType.CM, null) != null) {
            PlResType plResType = PlResType.CM;

            Map<String, ConfigMapGuiVO> configMapMap = (Map<String, ConfigMapGuiVO>) Optional.ofNullable(currResMap.get(plResType.getValue())).orElseGet(() ->Maps.newHashMap());

            for (Map.Entry<String, PlRunDeployVO> entry : resTypeListMap.get(plResType).entrySet()) {

                try {
                    // begin
                    if (!dryRun) {
                        this.updatePlRunDeployStatusNEvent(PlResType.valueOf(entry.getValue().getResType()), entry.getValue().getResName(), PlStatus.RUNNING, null, plRunVO.getCallbackUrl(), plRunVO.getPlSeq(), plRunVO.getPlRunSeq(), entry.getValue().getPlRunDeploySeq(), plDao);
                    }

                    List<Object> uptObjs = ServerUtils.getYamlObjects(entry.getValue().getResCont());

                    if (MapUtils.getObject(configMapMap, entry.getKey(), null) != null) {
                        // deploy
                        List<Object> currObjs = ServerUtils.getYamlObjects(configMapMap.get(entry.getKey()).getDeploymentYaml());

                        // patch
                        configMapService.patchConfigMapV1WithYaml(cluster, namespace, entry.getKey(), currObjs.get(0), uptObjs.get(0), dryRun, context);
                    } else {

                        k8sWorker.createConfigMapV1(cluster, namespace, (V1ConfigMap) uptObjs.get(0), dryRun);
                    }

                    // end
                    if (!dryRun) {
                        this.updatePlRunDeployStatusNEvent(PlResType.valueOf(entry.getValue().getResType()), entry.getValue().getResName(), entry.getValue().getRunStatus(), null, plRunVO.getCallbackUrl(), plRunVO.getPlSeq(), plRunVO.getPlRunSeq(), entry.getValue().getPlRunDeploySeq(), plDao);
                    }
                } catch (Exception e) {
                    entry.getValue().setRunStatus(PlStatus.ERROR);
                    this.updatePlRunDeployStatusNEvent(PlResType.valueOf(entry.getValue().getResType()), entry.getValue().getResName(), entry.getValue().getRunStatus(), e.getMessage(), plRunVO.getCallbackUrl(), plRunVO.getPlSeq(), plRunVO.getPlRunSeq(), entry.getValue().getPlRunDeploySeq(), plDao);
                    throw e;
                }

            }
        }
        // secret
        if (MapUtils.getObject(resTypeListMap, PlResType.SC, null) != null) {
            PlResType plResType = PlResType.SC;

            for (Map.Entry<String, PlRunDeployVO> entry : resTypeListMap.get(plResType).entrySet()) {
                try {
                    // begin
                    if (!dryRun) {
                        this.updatePlRunDeployStatusNEvent(PlResType.valueOf(entry.getValue().getResType()), entry.getValue().getResName(), PlStatus.RUNNING, null, plRunVO.getCallbackUrl(), plRunVO.getPlSeq(), plRunVO.getPlRunSeq(), entry.getValue().getPlRunDeploySeq(), plDao);
                    }

                    // deploy, secret 일때는 복호화 처리한다.
                    String resContents = CryptoUtils.decryptAES(entry.getValue().getResCont());
                    List<Object> uptObjs = ServerUtils.getYamlObjects(resContents);

                    // patch
                    secretService.patchSecretV1WithYaml(cluster, namespace, entry.getKey(), (V1Secret)uptObjs.get(0), context);

                    // end
                    if (!dryRun) {
                        entry.getValue().setRunStatus(PlStatus.DONE);
                        this.updatePlRunDeployStatusNEvent(PlResType.valueOf(entry.getValue().getResType()), entry.getValue().getResName(), entry.getValue().getRunStatus(), null, plRunVO.getCallbackUrl(), plRunVO.getPlSeq(), plRunVO.getPlRunSeq(), entry.getValue().getPlRunDeploySeq(), plDao);
                    }
                } catch (Exception e) {
                    entry.getValue().setRunStatus(PlStatus.ERROR);
                    this.updatePlRunDeployStatusNEvent(PlResType.valueOf(entry.getValue().getResType()), entry.getValue().getResName(), PlStatus.ERROR, e.getMessage(), plRunVO.getCallbackUrl(), plRunVO.getPlSeq(), plRunVO.getPlRunSeq(), entry.getValue().getPlRunDeploySeq(), plDao);
                    throw e;
                }
            }
        }
        // persistent volume claim
//        if (MapUtils.getObject(resTypeListMap, PlResType.PVC, null) != null) {
//                PlResType plResType = PlResType.PVC;
//        }


        if (CollectionUtils.isNotEmpty(workloads)) {

            for (PlRunDeployVO runDeploy : workloads) {
                PlResType plResType = PlResType.valueOf(runDeploy.getResType());
                List<Object> objs = ServerUtils.getYamlObjects(runDeploy.getResCont());


                if (plResType == PlResType.REPLICA_SERVER) {

                    this.runDeployment(cluster, cluster.getNamespaceName(), serviceSeq
                            , plRunVO, runDeploy, objs
                            , (Map<String, K8sDeploymentVO>) Optional.ofNullable(currResMap.get(plResType.getValue())).orElseGet(() ->Maps.newHashMap())
                            , (Map<String, K8sHorizontalPodAutoscalerVO>) Optional.ofNullable(currResMap.get(K8sApiKindType.HORIZONTAL_POD_AUTOSCALER.getValue())).orElseGet(() ->Maps.newHashMap())
                            , registryUser, dryRun, k8sJson, plDao, context);


                } else if (plResType == PlResType.STATEFUL_SET_SERVER) {

                    this.runStatefulSet(cluster, cluster.getNamespaceName(), serviceSeq
                            , plRunVO, runDeploy, objs
                            , (Map<String, K8sStatefulSetVO>) Optional.ofNullable(currResMap.get(plResType.getValue())).orElseGet(() ->Maps.newHashMap())
                            , (Map<String, K8sHorizontalPodAutoscalerVO>) Optional.ofNullable(currResMap.get(K8sApiKindType.HORIZONTAL_POD_AUTOSCALER.getValue())).orElseGet(() ->Maps.newHashMap())
                            , registryUser, dryRun, k8sJson, plDao, context);

                } else if (plResType == PlResType.DAEMON_SET_SERVER) {

                    this.runDaemonSet(cluster, cluster.getNamespaceName(), serviceSeq
                            , plRunVO, runDeploy, objs
                            , (Map<String, K8sDaemonSetVO>) Optional.ofNullable(currResMap.get(plResType.getValue())).orElseGet(() ->Maps.newHashMap())
                            , registryUser, dryRun, k8sJson, plDao, context);

                } else if (plResType == PlResType.JOB_SERVER) {

                    this.runJob(cluster, cluster.getNamespaceName(), serviceSeq
                            , plRunVO, runDeploy, objs
                            , (Map<String, K8sJobVO>) Optional.ofNullable(currResMap.get(plResType.getValue())).orElseGet(() ->Maps.newHashMap())
                            , registryUser, dryRun, k8sJson, plDao, context);

                } else if (plResType == PlResType.CRON_JOB_SERVER) {

                    this.runCronJob(cluster, cluster.getNamespaceName(), serviceSeq
                            , plRunVO, runDeploy, objs
                            , (Map<String, K8sCronJobVO>) Optional.ofNullable(currResMap.get(plResType.getValue())).orElseGet(() ->Maps.newHashMap())
                            , registryUser, dryRun, k8sJson, plDao, context);
                }
            }
        }


        // service
        if (MapUtils.getObject(resTypeListMap, PlResType.SVC, null) != null) {
            PlResType plResType = PlResType.SVC;

            Map<String, K8sServiceVO> serviceMap = (Map<String, K8sServiceVO>) Optional.ofNullable(currResMap.get(plResType.getValue())).orElseGet(() ->Maps.newHashMap());

            for (Map.Entry<String, PlRunDeployVO> entry : resTypeListMap.get(plResType).entrySet()) {
                try {
                    // begin
                    if (!dryRun) {
                        this.updatePlRunDeployStatusNEvent(PlResType.valueOf(entry.getValue().getResType()), entry.getValue().getResName(), PlStatus.RUNNING, null, plRunVO.getCallbackUrl(), plRunVO.getPlSeq(), plRunVO.getPlRunSeq(), entry.getValue().getPlRunDeploySeq(), plDao);
                    }

                    List<Object> uptObjs = ServerUtils.getYamlObjects(entry.getValue().getResCont());

                    if (MapUtils.getObject(serviceMap, entry.getKey(), null) != null) {
                        // deploy
                        List<Object> currObjs = ServerUtils.getYamlObjects(serviceMap.get(entry.getKey()).getServiceDeploymentYaml());

                        // patch
                        serviceSpecService.patchServiceV1(cluster, namespace, (V1Service) currObjs.get(0), (V1Service) uptObjs.get(0), dryRun, context);
                    } else {
                        k8sWorker.createServiceV1(cluster, namespace, (V1Service) uptObjs.get(0), dryRun);
                    }

                    // end
                    if (!dryRun) {
                        this.updatePlRunDeployStatusNEvent(PlResType.valueOf(entry.getValue().getResType()), entry.getValue().getResName(), PlStatus.DONE, null, plRunVO.getCallbackUrl(), plRunVO.getPlSeq(), plRunVO.getPlRunSeq(), entry.getValue().getPlRunDeploySeq(), plDao);
                    }
                } catch (Exception e) {
                    this.updatePlRunDeployStatusNEvent(PlResType.valueOf(entry.getValue().getResType()), entry.getValue().getResName(), PlStatus.ERROR, e.getMessage(), plRunVO.getCallbackUrl(), plRunVO.getPlSeq(), plRunVO.getPlRunSeq(), entry.getValue().getPlRunDeploySeq(), plDao);
                    throw e;
                }
            }
        }
        // ingress
        if (MapUtils.getObject(resTypeListMap, PlResType.IG, null) != null) {
            PlResType plResType = PlResType.IG;

            Map<String, K8sIngressVO> ingressMap = (Map<String, K8sIngressVO>) Optional.ofNullable(currResMap.get(plResType.getValue())).orElseGet(() ->Maps.newHashMap());

            for (Map.Entry<String, PlRunDeployVO> entry : resTypeListMap.get(plResType).entrySet()) {
                try {
                    // begin
                    if (!dryRun) {
                        this.updatePlRunDeployStatusNEvent(PlResType.valueOf(entry.getValue().getResType()), entry.getValue().getResName(), PlStatus.RUNNING, null, plRunVO.getCallbackUrl(), plRunVO.getPlSeq(), plRunVO.getPlRunSeq(), entry.getValue().getPlRunDeploySeq(), plDao);
                    }

                    List<Object> uptObjs = ServerUtils.getYamlObjects(entry.getValue().getResCont());

                    if (K8sApiVerType.isK8sVerSupported(cluster.getK8sVersion(), K8sApiVerType.V1_19)) {
                        if (MapUtils.getObject(ingressMap, entry.getKey(), null) != null) {
                            // deploy
                            List<Object> currObjs = ServerUtils.getYamlObjects(ingressMap.get(entry.getKey()).getDeploymentYaml());

                            // patch
                            ingressSpecService.patchIngress(cluster, namespace, (V1Ingress) currObjs.get(0) , (V1Ingress) uptObjs.get(0), dryRun, context);
                        } else {
                            k8sWorker.createIngressNetworkingV1(cluster, namespace, (V1Ingress) uptObjs.get(0), dryRun);
                        }
                    } else if (K8sApiVerType.isK8sVerSupported(cluster.getK8sVersion(), K8sApiVerType.V1_14)) {
                        if (MapUtils.getObject(ingressMap, entry.getKey(), null) != null) {
                            // deploy
                            List<Object> currObjs = ServerUtils.getYamlObjects(ingressMap.get(entry.getKey()).getDeploymentYaml());

                            // patch
                            ingressSpecService.patchIngress(cluster, namespace, (NetworkingV1beta1Ingress) currObjs.get(0) , (NetworkingV1beta1Ingress) uptObjs.get(0), dryRun, context);
                        } else {
                            k8sWorker.createIngressNetworkingV1beat1(cluster, namespace, (NetworkingV1beta1Ingress) uptObjs.get(0), dryRun);
                        }
                    }

                    // end
                    if (!dryRun) {
                        this.updatePlRunDeployStatusNEvent(PlResType.valueOf(entry.getValue().getResType()), entry.getValue().getResName(), PlStatus.DONE, null, plRunVO.getCallbackUrl(), plRunVO.getPlSeq(), plRunVO.getPlRunSeq(), entry.getValue().getPlRunDeploySeq(), plDao);
                    }
                } catch (Exception e) {
                    this.updatePlRunDeployStatusNEvent(PlResType.valueOf(entry.getValue().getResType()), entry.getValue().getResName(), PlStatus.ERROR, e.getMessage(), plRunVO.getCallbackUrl(), plRunVO.getPlSeq(), plRunVO.getPlRunSeq(), entry.getValue().getPlRunDeploySeq(), plDao);
                    throw e;
                }
            }
        }
    }

    /**
     * run deployment, hpa
     * 리소스가 존재한다면 'patch', 없다면 'create'
     *
     * @param cluster
     * @param namespace
     * @param serviceSeq
     * @param plRunVO
     * @param runDeploy
     * @param uptObjs
     * @param deploymentMap
     * @param hpaMap
     * @param registryUser
     * @param dryRun
     * @param k8sJson
     * @param plDao
     * @param context
     * @throws Exception
     */
    public void runDeployment(ClusterVO cluster, String namespace, Integer serviceSeq, PlRunVO plRunVO, PlRunDeployVO runDeploy, List<Object> uptObjs, Map<String, K8sDeploymentVO> deploymentMap, Map<String, K8sHorizontalPodAutoscalerVO> hpaMap, String registryUser, boolean dryRun, JSON k8sJson, IPlMapper plDao, ExecutingContextVO context) throws Exception {

        if (plDao == null) {
            plDao = sqlSession.getMapper(IPlMapper.class);
        }

        // 변경된 yaml을 객체로 변환
        V1Deployment uptDeployment = null;
        V2HorizontalPodAutoscaler uptHpaV2 = null;
        V2beta2HorizontalPodAutoscaler uptHpaV2beta2 = null;
        V2beta1HorizontalPodAutoscaler uptHpaV2beta1 = null;
        V1HorizontalPodAutoscaler uptHpaV1 = null;
        boolean hasHPA = false;

        String desc = null;

        if (CollectionUtils.isNotEmpty(uptObjs)) {
            try {

                for (Object obj : uptObjs) {
                    if (obj != null) {
                        Map<String, Object> k8sObjectToMap = ServerUtils.getK8sObjectToMap(obj, k8sJson);
                        K8sApiKindType kind = ServerUtils.getK8sKindInMap(k8sObjectToMap);
                        K8sApiGroupType apiGroupType = ServerUtils.getK8sGroupInMap(k8sObjectToMap);
                        K8sApiType apiType = ServerUtils.getK8sVersionInMap(k8sObjectToMap);
                        V1ObjectMeta objectMeta = ServerUtils.getK8sObjectMetaInMap(k8sObjectToMap, k8sJson);

                        switch (kind) {
                            case DEPLOYMENT:
                                if (objectMeta != null && StringUtils.equals(objectMeta.getName(), runDeploy.getResName())) {
                                    if (apiGroupType == K8sApiGroupType.APPS && apiType == K8sApiType.V1) {
                                        uptDeployment = (V1Deployment) obj;
                                        desc = serverConversionService.getDescription(objectMeta.getAnnotations());
                                    }
                                }
                                break;
                            case HORIZONTAL_POD_AUTOSCALER:
                                if (K8sApiType.V2 == apiType) {
                                    if (StringUtils.equals(Optional.ofNullable(((V2HorizontalPodAutoscaler) obj).getSpec()).map(V2HorizontalPodAutoscalerSpec::getScaleTargetRef).map(V2CrossVersionObjectReference::getName).orElseGet(() ->"")
                                            , runDeploy.getResName())) {
                                        uptHpaV2 = (V2HorizontalPodAutoscaler) obj;
                                    }
                                } else if (K8sApiType.V2BETA2 == apiType) {
                                    if (StringUtils.equals(Optional.ofNullable(((V2beta2HorizontalPodAutoscaler) obj).getSpec()).map(V2beta2HorizontalPodAutoscalerSpec::getScaleTargetRef).map(V2beta2CrossVersionObjectReference::getName).orElseGet(() ->"")
                                            , runDeploy.getResName())) {
                                        uptHpaV2beta2 = (V2beta2HorizontalPodAutoscaler) obj;
                                    }
                                } else if (K8sApiType.V2BETA1 == apiType) {
                                    if (StringUtils.equals(Optional.ofNullable(((V2beta1HorizontalPodAutoscaler) obj).getSpec()).map(V2beta1HorizontalPodAutoscalerSpec::getScaleTargetRef).map(V2beta1CrossVersionObjectReference::getName).orElseGet(() ->"")
                                            , runDeploy.getResName())) {
                                        uptHpaV2beta1 = (V2beta1HorizontalPodAutoscaler) obj;
                                    }
                                } else {
                                    if (StringUtils.equals(Optional.ofNullable(((V1HorizontalPodAutoscaler) obj).getSpec()).map(V1HorizontalPodAutoscalerSpec::getScaleTargetRef).map(V1CrossVersionObjectReference::getName).orElseGet(() ->"")
                                            , runDeploy.getResName())) {
                                        uptHpaV1 = (V1HorizontalPodAutoscaler) obj;
                                    }
                                }
                                break;
                        }
                    }
                }

                hasHPA = uptHpaV2 != null || uptHpaV2beta2 != null || uptHpaV2beta1 != null || uptHpaV1 != null;

                if (uptDeployment != null) {
                    if (uptDeployment.getMetadata() != null && uptDeployment.getSpec() != null) {
                        // begin
                        if (!dryRun) {
                            this.updatePlRunDeployStatusNEvent(PlResType.valueOf(runDeploy.getResType()), runDeploy.getResName(), PlStatus.RUNNING, null, plRunVO.getCallbackUrl(), plRunVO.getPlSeq(), plRunVO.getPlRunSeq(), runDeploy.getPlRunDeploySeq(), plDao);
                        }

                        // 추가
//                    serverValidService.checkServerApiVersion(WorkloadType.REPLICA_SERVER.getCode(), WorkloadVersion.V1.getCode(), cluster, context);

                        // 2022.09.22 hjchoi - 아래 이슈로 HPA가 존재한다면 null 셋팅
                        // HPA가 활성화되어 있으면, 디플로이먼트, 스테이트풀셋 모두 또는 둘 중 하나의 매니페스트에서 spec.replicas의 값을 삭제하는 것이 바람직하다
                        // https://kubernetes.io/ko/docs/tasks/run-application/horizontal-pod-autoscale/#%EB%94%94%ED%94%8C%EB%A1%9C%EC%9D%B4%EB%A8%BC%ED%8A%B8%EC%99%80-%EC%8A%A4%ED%85%8C%EC%9D%B4%ED%8A%B8%ED%92%80%EC%85%8B%EC%9D%84-horizontal-autoscaling%EC%9C%BC%EB%A1%9C-%EC%A0%84%ED%99%98%ED%95%98%EA%B8%B0
                        if (hasHPA) {
                            uptDeployment.getSpec().setReplicas(null);
                        }

                        // 기존 값이 있다면 patch
                        if (deploymentMap.containsKey(runDeploy.getResName())) {
                            if (!hasHPA) {
                                // deployment ScaleInOut
                                workloadResourceService.patchDeploymentScale(cluster, namespace, uptDeployment.getMetadata().getName(), Optional.ofNullable(uptDeployment.getSpec().getReplicas()).orElseGet(() -> 0), dryRun, context);
                            }
                            // patch deployment
                            workloadResourceService.patchDeploymentV1(cluster, namespace, ServerUtils.unmarshalYaml(deploymentMap.get(runDeploy.getResName()).getDeploymentYaml(), K8sApiKindType.DEPLOYMENT, k8sJson), uptDeployment, dryRun, hasHPA, context);
                        }
                        // 없다면 create
                        else {
                            // create deployment
                            k8sWorker.createDeploymentV1(cluster, namespace, uptDeployment, dryRun);
                        }
                    }
                }
            } catch (Exception e) {
                this.updatePlRunDeployStatusNEvent(PlResType.valueOf(runDeploy.getResType()), runDeploy.getResName(), PlStatus.ERROR, e.getMessage(), plRunVO.getCallbackUrl(), plRunVO.getPlSeq(), plRunVO.getPlRunSeq(), runDeploy.getPlRunDeploySeq(), plDao);
                throw e;
            }

            Map<String, K8sHorizontalPodAutoscalerVO> currHpaMap;

            if (hasHPA) {
                try {
                    // 워크로드가 존재한다면 워크로드에서 조회한 hpa 셋팅
                    if (deploymentMap.containsKey(runDeploy.getResName()) && CollectionUtils.isNotEmpty(deploymentMap.get(runDeploy.getResName()).getHorizontalPodAutoscalers())) {
                        currHpaMap = deploymentMap.get(runDeploy.getResName()).getHorizontalPodAutoscalers().stream().collect(Collectors.toMap(K8sHorizontalPodAutoscalerVO::getName, Function.identity()));
                    }
                    // 없다면 별도로 조회한 hpa 셋팅
                    else {
                        currHpaMap = hpaMap;
                    }

                    // run hpa
                    this.runHorizontalPodAutoscaler(cluster, namespace, plRunVO, runDeploy.getPlRunDeploySeq(), currHpaMap, uptHpaV2, uptHpaV2beta2, uptHpaV2beta1, uptHpaV1, dryRun, k8sJson, plDao, context);

                } catch (Exception e) {
                    this.updatePlRunDeployStatusNEvent(PlResType.valueOf(runDeploy.getResType()), runDeploy.getResName(), PlStatus.ERROR, null, plRunVO.getCallbackUrl(), plRunVO.getPlSeq(), plRunVO.getPlRunSeq(), runDeploy.getPlRunDeploySeq(), plDao);
                    throw e;
                }
            } else {

                if (uptDeployment != null && uptDeployment.getMetadata() != null) {
                    K8sApiVerKindType hpaType = ResourceUtil.getApiVerKindType(cluster, K8sApiKindType.HORIZONTAL_POD_AUTOSCALER);
                    List<K8sHorizontalPodAutoscalerVO> currentHpas = Lists.newArrayList();
                    if (EnumSet.of(K8sApiType.V2, K8sApiType.V2BETA2, K8sApiType.V2BETA1, K8sApiType.V1).contains(hpaType.getApiType())) {
                        currentHpas = workloadResourceService.convertHorizontalPodAutoscalerDataList(cluster, namespace, null, null);
                    }

                    if (CollectionUtils.isNotEmpty(currentHpas)) {
                        String kind = uptDeployment.getKind();
                        String name = uptDeployment.getMetadata().getName();
                        for (K8sHorizontalPodAutoscalerVO hpa : currentHpas) {
                            if (hpa.getScaleTargetRef() != null
                                    && StringUtils.equals(hpa.getScaleTargetRef().getKind(), kind)
                                    && StringUtils.equals(hpa.getScaleTargetRef().getName(), name)
                            ) {
                                workloadResourceService.deleteHorizontalPodAutoscaler(cluster, namespace, hpa.getName(), null);
                                break;
                            }
                        }
                    }
                }

                // end
                if (!dryRun) {
                    this.updatePlRunDeployStatusNEvent(PlResType.valueOf(runDeploy.getResType()), runDeploy.getResName(), PlStatus.DONE, null, plRunVO.getCallbackUrl(), plRunVO.getPlSeq(), plRunVO.getPlRunSeq(), runDeploy.getPlRunDeploySeq(), plDao);
                }
            }

            /**
             * old pipeline 수정
             */
            if (!dryRun) {
                this.mergeOldPipeline(cluster, cluster.getNamespaceName(), serviceSeq, WorkloadType.REPLICA_SERVER.getCode(), WorkloadVersion.V1.getCode(), desc, uptObjs, context);
            }
        }

    }

    /**
     * run statefulSet, hpa
     * 리소스가 존재한다면 'patch', 없다면 'create'
     *
     * @param cluster
     * @param namespace
     * @param serviceSeq
     * @param plRunVO
     * @param runDeploy
     * @param uptObjs
     * @param statefulSetMap
     * @param hpaMap
     * @param registryUser
     * @param dryRun
     * @param k8sJson
     * @param plDao
     * @param context
     * @throws Exception
     */
    public void runStatefulSet(ClusterVO cluster, String namespace, Integer serviceSeq, PlRunVO plRunVO, PlRunDeployVO runDeploy, List<Object> uptObjs, Map<String, K8sStatefulSetVO> statefulSetMap, Map<String, K8sHorizontalPodAutoscalerVO> hpaMap, String registryUser, boolean dryRun, JSON k8sJson, IPlMapper plDao, ExecutingContextVO context) throws Exception {

        if (plDao == null) {
            plDao = sqlSession.getMapper(IPlMapper.class);
        }

        // 변경된 yaml을 객체로 변환
        V1StatefulSet uptStatefulSet = null;
        V2HorizontalPodAutoscaler uptHpaV2 = null;
        V2beta2HorizontalPodAutoscaler uptHpaV2beta2 = null;
        V2beta1HorizontalPodAutoscaler uptHpaV2beta1 = null;
        V1HorizontalPodAutoscaler uptHpaV1 = null;
        boolean hasHPA = false;

        String desc = null;

        if (CollectionUtils.isNotEmpty(uptObjs)) {
            try {
                for (Object obj : uptObjs) {
                    Map<String, Object> k8sObjectToMap = ServerUtils.getK8sObjectToMap(obj, k8sJson);
                    K8sApiKindType kind = ServerUtils.getK8sKindInMap(k8sObjectToMap);
                    K8sApiGroupType apiGroupType = ServerUtils.getK8sGroupInMap(k8sObjectToMap);
                    K8sApiType apiType = ServerUtils.getK8sVersionInMap(k8sObjectToMap);
                    V1ObjectMeta objectMeta = ServerUtils.getK8sObjectMetaInMap(k8sObjectToMap, k8sJson);

                    switch (kind) {
                        case STATEFUL_SET:
                            if (objectMeta != null && StringUtils.equals(objectMeta.getName(), runDeploy.getResName())) {
                                if (apiGroupType == K8sApiGroupType.APPS && apiType == K8sApiType.V1) {
                                    uptStatefulSet = (V1StatefulSet) obj;
                                    desc = serverConversionService.getDescription(objectMeta.getAnnotations());
                                }
                            }
                            break;
                        case HORIZONTAL_POD_AUTOSCALER:
                            if (K8sApiType.V2 == apiType) {
                                if (StringUtils.equals(Optional.ofNullable(((V2HorizontalPodAutoscaler) obj).getSpec()).map(V2HorizontalPodAutoscalerSpec::getScaleTargetRef).map(V2CrossVersionObjectReference::getName).orElseGet(() ->"")
                                        , runDeploy.getResName())) {
                                    uptHpaV2 = (V2HorizontalPodAutoscaler) obj;
                                }
                            } else if (K8sApiType.V2BETA2 == apiType) {
                                if (StringUtils.equals(Optional.ofNullable(((V2beta2HorizontalPodAutoscaler) obj).getSpec()).map(V2beta2HorizontalPodAutoscalerSpec::getScaleTargetRef).map(V2beta2CrossVersionObjectReference::getName).orElseGet(() ->"")
                                        , runDeploy.getResName())) {
                                    uptHpaV2beta2 = (V2beta2HorizontalPodAutoscaler) obj;
                                }
                            } else if (K8sApiType.V2BETA1 == apiType) {
                                if (StringUtils.equals(Optional.ofNullable(((V2beta1HorizontalPodAutoscaler) obj).getSpec()).map(V2beta1HorizontalPodAutoscalerSpec::getScaleTargetRef).map(V2beta1CrossVersionObjectReference::getName).orElseGet(() ->"")
                                        , runDeploy.getResName())) {
                                    uptHpaV2beta1 = (V2beta1HorizontalPodAutoscaler) obj;
                                }
                            } else {
                                if (StringUtils.equals(Optional.ofNullable(((V1HorizontalPodAutoscaler) obj).getSpec()).map(V1HorizontalPodAutoscalerSpec::getScaleTargetRef).map(V1CrossVersionObjectReference::getName).orElseGet(() ->"")
                                        , runDeploy.getResName())) {
                                    uptHpaV1 = (V1HorizontalPodAutoscaler) obj;
                                }
                            }
                            break;
                    }
                }

                hasHPA = uptHpaV2 != null || uptHpaV2beta2 != null || uptHpaV2beta1 != null || uptHpaV1 != null;

                if (uptStatefulSet != null) {
                    if (uptStatefulSet.getMetadata() != null && uptStatefulSet.getSpec() != null) {
                        // begin
                        if (!dryRun) {
                            this.updatePlRunDeployStatusNEvent(PlResType.valueOf(runDeploy.getResType()), runDeploy.getResName(), PlStatus.RUNNING, null, plRunVO.getCallbackUrl(), plRunVO.getPlSeq(), plRunVO.getPlRunSeq(), runDeploy.getPlRunDeploySeq(), plDao);
                        }

                        // 추가
//                    serverValidService.checkServerApiVersion(WorkloadType.STATEFUL_SET_SERVER.getCode(), WorkloadVersion.V1.getCode(), cluster, context);

                        // 2022.09.22 hjchoi - 아래 이슈로 HPA가 존재한다면 null 셋팅
                        // HPA가 활성화되어 있으면, 디플로이먼트, 스테이트풀셋 모두 또는 둘 중 하나의 매니페스트에서 spec.replicas의 값을 삭제하는 것이 바람직하다
                        // https://kubernetes.io/ko/docs/tasks/run-application/horizontal-pod-autoscale/#%EB%94%94%ED%94%8C%EB%A1%9C%EC%9D%B4%EB%A8%BC%ED%8A%B8%EC%99%80-%EC%8A%A4%ED%85%8C%EC%9D%B4%ED%8A%B8%ED%92%80%EC%85%8B%EC%9D%84-horizontal-autoscaling%EC%9C%BC%EB%A1%9C-%EC%A0%84%ED%99%98%ED%95%98%EA%B8%B0
                        if (hasHPA) {
                            uptStatefulSet.getSpec().setReplicas(null);
                        }

                        // 기존 값이 있다면 patch
                        if (statefulSetMap.containsKey(runDeploy.getResName())) {
                            if (!hasHPA) {
                                // statefulSet ScaleInOut
                                workloadResourceService.patchStatefulSetScale(cluster, namespace, uptStatefulSet.getMetadata().getName(), Optional.ofNullable(uptStatefulSet.getSpec().getReplicas()).orElseGet(() -> 0), dryRun, context);
                            }

                            // patch statefulSet
                            workloadResourceService.patchStatefulSetV1(cluster, namespace, ServerUtils.unmarshalYaml(statefulSetMap.get(runDeploy.getResName()).getDeploymentYaml(), K8sApiKindType.STATEFUL_SET, k8sJson), uptStatefulSet, dryRun, hasHPA, context);
                        }
                        // 없다면 create
                        else {
                            // create statefulSet
                            k8sWorker.createStatefulSetV1(cluster, namespace, uptStatefulSet, dryRun);
                        }
                    }
                }
            } catch (Exception e) {
                this.updatePlRunDeployStatusNEvent(PlResType.valueOf(runDeploy.getResType()), runDeploy.getResName(), PlStatus.ERROR, e.getMessage(), plRunVO.getCallbackUrl(), plRunVO.getPlSeq(), plRunVO.getPlRunSeq(), runDeploy.getPlRunDeploySeq(), plDao);
                throw e;
            }


            Map<String, K8sHorizontalPodAutoscalerVO> currHpaMap;
            if (uptHpaV2 != null || uptHpaV2beta2 != null || uptHpaV2beta1 != null || uptHpaV1 != null) {
                try {
                    // 워크로드가 존재한다면 워크로드에서 조회한 hpa 셋팅
                    if (statefulSetMap.containsKey(runDeploy.getResName()) && CollectionUtils.isNotEmpty(statefulSetMap.get(runDeploy.getResName()).getHorizontalPodAutoscalers())) {
                        currHpaMap = statefulSetMap.get(runDeploy.getResName()).getHorizontalPodAutoscalers().stream().collect(Collectors.toMap(K8sHorizontalPodAutoscalerVO::getName, Function.identity()));
                    }
                    // 없다면 별도로 조회한 hpa 셋팅
                    else {
                        currHpaMap = hpaMap;
                    }

                    // run hpa
                    this.runHorizontalPodAutoscaler(cluster, namespace, plRunVO, runDeploy.getPlRunDeploySeq(), currHpaMap, uptHpaV2, uptHpaV2beta2, uptHpaV2beta1, uptHpaV1, dryRun, k8sJson, plDao, context);

                } catch (Exception e) {
                    this.updatePlRunDeployStatusNEvent(PlResType.valueOf(runDeploy.getResType()), runDeploy.getResName(), PlStatus.ERROR, e.getMessage(), plRunVO.getCallbackUrl(), plRunVO.getPlSeq(), plRunVO.getPlRunSeq(), runDeploy.getPlRunDeploySeq(), plDao);
                    throw e;
                }
            } else {

                if (uptStatefulSet != null && uptStatefulSet.getMetadata() != null) {
                    K8sApiVerKindType hpaType = ResourceUtil.getApiVerKindType(cluster, K8sApiKindType.HORIZONTAL_POD_AUTOSCALER);
                    List<K8sHorizontalPodAutoscalerVO> currentHpas = Lists.newArrayList();
                    if (EnumSet.of(K8sApiType.V2, K8sApiType.V2BETA2, K8sApiType.V2BETA1, K8sApiType.V1).contains(hpaType.getApiType())) {
                        currentHpas = workloadResourceService.convertHorizontalPodAutoscalerDataList(cluster, namespace, null, null);
                    }

                    if (CollectionUtils.isNotEmpty(currentHpas)) {
                        String kind = uptStatefulSet.getKind();
                        String name = uptStatefulSet.getMetadata().getName();
                        for (K8sHorizontalPodAutoscalerVO hpa : currentHpas) {
                            if (hpa.getScaleTargetRef() != null
                                    && StringUtils.equals(hpa.getScaleTargetRef().getKind(), kind)
                                    && StringUtils.equals(hpa.getScaleTargetRef().getName(), name)
                            ) {
                                workloadResourceService.deleteHorizontalPodAutoscaler(cluster, namespace, hpa.getName(), null);
                                break;
                            }
                        }
                    }
                }

                // end
                if (!dryRun) {
                    this.updatePlRunDeployStatusNEvent(PlResType.valueOf(runDeploy.getResType()), runDeploy.getResName(), PlStatus.DONE, null, plRunVO.getCallbackUrl(), plRunVO.getPlSeq(), plRunVO.getPlRunSeq(), runDeploy.getPlRunDeploySeq(), plDao);
                }
            }

            /**
             * old pipeline 수정
             */
            if (!dryRun) {
                this.mergeOldPipeline(cluster, cluster.getNamespaceName(), serviceSeq, WorkloadType.STATEFUL_SET_SERVER.getCode(), WorkloadVersion.V1.getCode(), desc, uptObjs, context);
            }
        }
    }

    /**
     * run daemonSet
     * 리소스가 존재한다면 'patch', 없다면 'create'
     *
     * @param cluster
     * @param namespace
     * @param serviceSeq
     * @param plRunVO
     * @param runDeploy
     * @param uptObjs
     * @param daemonSetMap
     * @param registryUser
     * @param dryRun
     * @param k8sJson
     * @param plDao
     * @param context
     * @throws Exception
     */
    public void runDaemonSet(ClusterVO cluster, String namespace, Integer serviceSeq, PlRunVO plRunVO, PlRunDeployVO runDeploy, List<Object> uptObjs, Map<String, K8sDaemonSetVO> daemonSetMap, String registryUser, boolean dryRun, JSON k8sJson, IPlMapper plDao, ExecutingContextVO context) throws Exception {

        if (plDao == null) {
            plDao = sqlSession.getMapper(IPlMapper.class);
        }

        // 변경된 yaml을 객체로 변환
        V1DaemonSet uptDaemonSet = null;

        String desc = null;

        if (CollectionUtils.isNotEmpty(uptObjs)) {
            try {
                for (Object obj : uptObjs) {
                    Map<String, Object> k8sObjectToMap = ServerUtils.getK8sObjectToMap(obj, k8sJson);
                    K8sApiKindType kind = ServerUtils.getK8sKindInMap(k8sObjectToMap);
                    K8sApiGroupType apiGroupType = ServerUtils.getK8sGroupInMap(k8sObjectToMap);
                    K8sApiType apiType = ServerUtils.getK8sVersionInMap(k8sObjectToMap);
                    V1ObjectMeta objectMeta = ServerUtils.getK8sObjectMetaInMap(k8sObjectToMap, k8sJson);

                    if (kind == K8sApiKindType.DAEMON_SET) {
                        if (objectMeta != null && StringUtils.equals(objectMeta.getName(), runDeploy.getResName())) {
                            if (apiGroupType == K8sApiGroupType.APPS && apiType == K8sApiType.V1) {
                                uptDaemonSet = (V1DaemonSet) obj;
                                desc = serverConversionService.getDescription(objectMeta.getAnnotations());
                            }
                        }
                    }
                }

                if (uptDaemonSet != null) {
                    if (uptDaemonSet.getMetadata() != null && uptDaemonSet.getSpec() != null) {
                        // begin
                        if (!dryRun) {
                            this.updatePlRunDeployStatusNEvent(PlResType.valueOf(runDeploy.getResType()), runDeploy.getResName(), PlStatus.RUNNING, null, plRunVO.getCallbackUrl(), plRunVO.getPlSeq(), plRunVO.getPlRunSeq(), runDeploy.getPlRunDeploySeq(), plDao);
                        }

                        // set container Image 및 pull secret
                        if (CollectionUtils.isNotEmpty(runDeploy.getPlRunBuilds())) {
                            this.setContainerImage(uptDaemonSet.getSpec().getTemplate(), runDeploy.getPlRunBuilds(), registryUser);
                        }

                        uptDaemonSet.getMetadata().putAnnotationsItem(KubeConstants.ANNOTATION_COCKTAIL_DEPLOY_DATETIME, Utils.getNowDateTime());

                        // 추가
//                    serverValidService.checkServerApiVersion(WorkloadType.DAEMON_SET_SERVER.getCode(), WorkloadVersion.V1.getCode(), cluster, context);

                        // 기존 값이 있다면 patch
                        if (daemonSetMap.containsKey(runDeploy.getResName())) {
                            workloadResourceService.patchDaemonSetV1(cluster, cluster.getNamespaceName(), ServerUtils.unmarshalYaml(daemonSetMap.get(runDeploy.getResName()).getDeploymentYaml(), K8sApiKindType.DAEMON_SET, k8sJson), uptDaemonSet, dryRun, context);
                        }
                        // 없다면 create
                        else {
                            k8sWorker.createDaemonSetV1(cluster, namespace, uptDaemonSet, dryRun);
                        }

                        // end
                        if (!dryRun) {
                            this.updatePlRunDeployStatusNEvent(PlResType.valueOf(runDeploy.getResType()), runDeploy.getResName(), PlStatus.DONE, null, plRunVO.getCallbackUrl(), plRunVO.getPlSeq(), plRunVO.getPlRunSeq(), runDeploy.getPlRunDeploySeq(), plDao);
                        }
                    }
                }
            } catch (Exception e) {
                this.updatePlRunDeployStatusNEvent(PlResType.valueOf(runDeploy.getResType()), runDeploy.getResName(), PlStatus.ERROR, e.getMessage(), plRunVO.getCallbackUrl(), plRunVO.getPlSeq(), plRunVO.getPlRunSeq(), runDeploy.getPlRunDeploySeq(), plDao);
                throw e;
            }

            /**
             * old pipeline 수정
             */
            if (!dryRun) {
                this.mergeOldPipeline(cluster, cluster.getNamespaceName(), serviceSeq, WorkloadType.DAEMON_SET_SERVER.getCode(), WorkloadVersion.V1.getCode(), desc, uptObjs, context);
            }
        }
    }

    /**
     * run job
     * 리소스가 존재한다면 'delete' 후, 'create', 없다면 그냥 'create'
     *
     * @param cluster
     * @param namespace
     * @param serviceSeq
     * @param plRunVO
     * @param runDeploy
     * @param uptObjs
     * @param jobMap
     * @param registryUser
     * @param dryRun
     * @param k8sJson
     * @param plDao
     * @param context
     * @throws Exception
     */
    public void runJob(ClusterVO cluster, String namespace, Integer serviceSeq, PlRunVO plRunVO, PlRunDeployVO runDeploy, List<Object> uptObjs, Map<String, K8sJobVO> jobMap, String registryUser, boolean dryRun, JSON k8sJson, IPlMapper plDao, ExecutingContextVO context) throws Exception {

        if (plDao == null) {
            plDao = sqlSession.getMapper(IPlMapper.class);
        }

        // 변경된 yaml을 객체로 변환
        V1Job uptJob = null;

        String desc = null;

        if (CollectionUtils.isNotEmpty(uptObjs)) {
            try {
                for (Object obj : uptObjs) {
                    Map<String, Object> k8sObjectToMap = ServerUtils.getK8sObjectToMap(obj, k8sJson);
                    K8sApiKindType kind = ServerUtils.getK8sKindInMap(k8sObjectToMap);
                    K8sApiGroupType apiGroupType = ServerUtils.getK8sGroupInMap(k8sObjectToMap);
                    K8sApiType apiType = ServerUtils.getK8sVersionInMap(k8sObjectToMap);
                    V1ObjectMeta objectMeta = ServerUtils.getK8sObjectMetaInMap(k8sObjectToMap, k8sJson);

                    if (kind == K8sApiKindType.JOB) {
                        if (objectMeta != null && StringUtils.equals(objectMeta.getName(), runDeploy.getResName())) {
                            if (apiGroupType == K8sApiGroupType.BATCH && apiType == K8sApiType.V1) {
                                uptJob = (V1Job) obj;
                                desc = serverConversionService.getDescription(objectMeta.getAnnotations());
                            }
                        }
                    }
                }

                if (uptJob != null) {
                    if (uptJob.getMetadata() != null && uptJob.getSpec() != null) {
                        // begin
                        if (!dryRun) {
                            this.updatePlRunDeployStatusNEvent(PlResType.valueOf(runDeploy.getResType()), runDeploy.getResName(), PlStatus.RUNNING, null, plRunVO.getCallbackUrl(), plRunVO.getPlSeq(), plRunVO.getPlRunSeq(), runDeploy.getPlRunDeploySeq(), plDao);
                        }

                        // set container Image 및 pull secret
                        if (CollectionUtils.isNotEmpty(runDeploy.getPlRunBuilds())) {
                            this.setContainerImage(uptJob.getSpec().getTemplate(), runDeploy.getPlRunBuilds(), registryUser);
                        }

                        uptJob.getMetadata().putAnnotationsItem(KubeConstants.ANNOTATION_COCKTAIL_DEPLOY_DATETIME, Utils.getNowDateTime());

                        // 추가
//                    serverValidService.checkServerApiVersion(WorkloadType.JOB_SERVER.getCode(), WorkloadVersion.V1.getCode(), cluster, context);

                        // 기존 값이 있다면 delete 후, create
                        if (jobMap.containsKey(runDeploy.getResName())) {
                            // delete
                            workloadResourceService.deleteJobV1(cluster, namespace, uptJob.getMetadata().getName(), context);

                            // delete 확인
                            K8sJobVO k8sJob = null;
                            for (int n = 10, ne = 0; n > ne; n--) {
                                Thread.sleep(1000);

                                k8sJob = workloadResourceService.getJob(cluster, namespace, uptJob.getMetadata().getName(), context);
                                if (k8sJob == null) {
                                    break;
                                }
                            }

                            // create
                            if (k8sJob == null) {
                                k8sWorker.createJobV1(cluster, namespace, uptJob, dryRun);
                            }

                        }
                        // 없다면 create
                        else {
                            k8sWorker.createJobV1(cluster, namespace, uptJob, dryRun);
                        }

                        // end
                        if (!dryRun) {
                            this.updatePlRunDeployStatusNEvent(PlResType.valueOf(runDeploy.getResType()), runDeploy.getResName(), PlStatus.DONE, null, plRunVO.getCallbackUrl(), plRunVO.getPlSeq(), plRunVO.getPlRunSeq(), runDeploy.getPlRunDeploySeq(), plDao);
                        }
                    }
                }
            } catch (Exception e) {
                this.updatePlRunDeployStatusNEvent(PlResType.valueOf(runDeploy.getResType()), runDeploy.getResName(), PlStatus.ERROR, e.getMessage(), plRunVO.getCallbackUrl(), plRunVO.getPlSeq(), plRunVO.getPlRunSeq(), runDeploy.getPlRunDeploySeq(), plDao);
                throw e;
            }

            /**
             * old pipeline 수정
             */
            if (!dryRun) {
                this.mergeOldPipeline(cluster, cluster.getNamespaceName(), serviceSeq, WorkloadType.JOB_SERVER.getCode(), WorkloadVersion.V1.getCode(), desc, uptObjs, context);
            }

        }
    }

    /**
     * run cronJob
     * 리소스가 존재한다면 'patch', 없다면 'create'
     *
     * @param cluster
     * @param namespace
     * @param serviceSeq
     * @param plRunVO
     * @param runDeploy
     * @param uptObjs
     * @param cronJobMap
     * @param registryUser
     * @param dryRun
     * @param k8sJson
     * @param plDao
     * @param context
     * @throws Exception
     */
    public void runCronJob(ClusterVO cluster, String namespace, Integer serviceSeq, PlRunVO plRunVO, PlRunDeployVO runDeploy, List<Object> uptObjs, Map<String, K8sCronJobVO> cronJobMap, String registryUser, boolean dryRun, JSON k8sJson, IPlMapper plDao, ExecutingContextVO context) throws Exception {

        if (plDao == null) {
            plDao = sqlSession.getMapper(IPlMapper.class);
        }

        // 변경된 yaml을 객체로 변환
        V1beta1CronJob uptCronJobV1beta1 = null;
        V1CronJob uptCronJobV1 = null;

        String desc = null;

        if (CollectionUtils.isNotEmpty(uptObjs)) {
            try {
                for (Object obj : uptObjs) {
                    Map<String, Object> k8sObjectToMap = ServerUtils.getK8sObjectToMap(obj, k8sJson);
                    K8sApiKindType kind = ServerUtils.getK8sKindInMap(k8sObjectToMap);
                    K8sApiGroupType apiGroupType = ServerUtils.getK8sGroupInMap(k8sObjectToMap);
                    K8sApiType apiType = ServerUtils.getK8sVersionInMap(k8sObjectToMap);
                    V1ObjectMeta objectMeta = ServerUtils.getK8sObjectMetaInMap(k8sObjectToMap, k8sJson);

                    if (kind == K8sApiKindType.CRON_JOB) {
                        if (objectMeta != null && StringUtils.equals(objectMeta.getName(), runDeploy.getResName())) {
                            if (apiGroupType == K8sApiGroupType.BATCH && apiType == K8sApiType.V1BETA1) {
                                uptCronJobV1beta1 = (V1beta1CronJob) obj;
                                desc = serverConversionService.getDescription(objectMeta.getAnnotations());
                            } else if (apiGroupType == K8sApiGroupType.BATCH && apiType == K8sApiType.V1) {
                                uptCronJobV1 = (V1CronJob) obj;
                                desc = serverConversionService.getDescription(objectMeta.getAnnotations());
                            }
                        }
                    }
                }

                if (uptCronJobV1beta1 != null) {
                    if (uptCronJobV1beta1.getMetadata() != null && uptCronJobV1beta1.getSpec() != null) {
                        // begin
                        if (!dryRun) {
                            this.updatePlRunDeployStatusNEvent(PlResType.valueOf(runDeploy.getResType()), runDeploy.getResName(), PlStatus.RUNNING, null, plRunVO.getCallbackUrl(), plRunVO.getPlSeq(), plRunVO.getPlRunSeq(), runDeploy.getPlRunDeploySeq(), plDao);
                        }

                        // set container Image 및 pull secret
                        if (CollectionUtils.isNotEmpty(runDeploy.getPlRunBuilds())) {
                            this.setContainerImage(Optional.ofNullable(uptCronJobV1beta1.getSpec()).map(V1beta1CronJobSpec::getJobTemplate).map(V1beta1JobTemplateSpec::getSpec).map(V1JobSpec::getTemplate).orElseGet(() ->null), runDeploy.getPlRunBuilds(), registryUser);
                        }

                        uptCronJobV1beta1.getMetadata().putAnnotationsItem(KubeConstants.ANNOTATION_COCKTAIL_DEPLOY_DATETIME, Utils.getNowDateTime());

                        // 추가
//                    serverValidService.checkServerApiVersion(WorkloadType.CRON_JOB_SERVER.getCode(), WorkloadVersion.V1.getCode(), cluster, context);

                        // 기존 값이 있다면 patch
                        if (cronJobMap.containsKey(runDeploy.getResName())) {
                            workloadResourceService.patchCronJobV1beta1(cluster, cluster.getNamespaceName(), ServerUtils.unmarshalYaml(cronJobMap.get(runDeploy.getResName()).getDeploymentYaml(), K8sApiKindType.CRON_JOB, k8sJson), uptCronJobV1beta1, dryRun, context);
                        }
                        // 없다면 create
                        else {
                            k8sWorker.createCronJobV1beta1(cluster, namespace, uptCronJobV1beta1, dryRun);
                        }

                        // end
                        if (!dryRun) {
                            this.updatePlRunDeployStatusNEvent(PlResType.valueOf(runDeploy.getResType()), runDeploy.getResName(), PlStatus.DONE, null, plRunVO.getCallbackUrl(), plRunVO.getPlSeq(), plRunVO.getPlRunSeq(), runDeploy.getPlRunDeploySeq(), plDao);
                        }
                    }
                } else if (uptCronJobV1 != null) {
                    if (uptCronJobV1.getMetadata() != null && uptCronJobV1.getSpec() != null) {
                        // begin
                        if (!dryRun) {
                            this.updatePlRunDeployStatusNEvent(PlResType.valueOf(runDeploy.getResType()), runDeploy.getResName(), PlStatus.RUNNING, null, plRunVO.getCallbackUrl(), plRunVO.getPlSeq(), plRunVO.getPlRunSeq(), runDeploy.getPlRunDeploySeq(), plDao);
                        }

                        // set container Image 및 pull secret
                        if (CollectionUtils.isNotEmpty(runDeploy.getPlRunBuilds())) {
                            this.setContainerImage(Optional.ofNullable(uptCronJobV1.getSpec()).map(V1CronJobSpec::getJobTemplate).map(V1JobTemplateSpec::getSpec).map(V1JobSpec::getTemplate).orElseGet(() ->null), runDeploy.getPlRunBuilds(), registryUser);
                        }

                        uptCronJobV1.getMetadata().putAnnotationsItem(KubeConstants.ANNOTATION_COCKTAIL_DEPLOY_DATETIME, Utils.getNowDateTime());

                        // 추가
//                    serverValidService.checkServerApiVersion(WorkloadType.CRON_JOB_SERVER.getCode(), WorkloadVersion.V1.getCode(), cluster, context);

                        // 기존 값이 있다면 patch
                        if (cronJobMap.containsKey(runDeploy.getResName())) {
                            workloadResourceService.patchCronJobV1(cluster, cluster.getNamespaceName(), ServerUtils.unmarshalYaml(cronJobMap.get(runDeploy.getResName()).getDeploymentYaml(), K8sApiKindType.CRON_JOB, k8sJson), uptCronJobV1, dryRun, context);
                        }
                        // 없다면 create
                        else {
                            k8sWorker.createCronJobV1(cluster, namespace, uptCronJobV1, dryRun);
                        }

                        // end
                        if (!dryRun) {
                            this.updatePlRunDeployStatusNEvent(PlResType.valueOf(runDeploy.getResType()), runDeploy.getResName(), PlStatus.DONE, null, plRunVO.getCallbackUrl(), plRunVO.getPlSeq(), plRunVO.getPlRunSeq(), runDeploy.getPlRunDeploySeq(), plDao);
                        }
                    }
                }
            } catch (Exception e) {
                this.updatePlRunDeployStatusNEvent(PlResType.valueOf(runDeploy.getResType()), runDeploy.getResName(), PlStatus.ERROR, e.getMessage(), plRunVO.getCallbackUrl(), plRunVO.getPlSeq(), plRunVO.getPlRunSeq(), runDeploy.getPlRunDeploySeq(), plDao);
                throw e;
            }

            /**
             * old pipeline 수정
             */
            if (!dryRun) {
                this.mergeOldPipeline(cluster, cluster.getNamespaceName(), serviceSeq, WorkloadType.CRON_JOB_SERVER.getCode(), WorkloadVersion.V1.getCode(), desc, uptObjs, context);
            }

        }
    }

    /**
     * run HorizontalPodAutoscaler V2beta2
     *
     * @param cluster
     * @param namespace
     * @param plRunVO
     * @param plRunDeploySeq
     * @param currHpaMap
     * @param uptHpaV2beta2
     * @param uptHpaV2beta1
     * @param uptHpaV1
     * @param dryRun
     * @param k8sJson
     * @param plDao
     * @param context
     * @throws Exception
     */
    public void runHorizontalPodAutoscaler(ClusterVO cluster, String namespace, PlRunVO plRunVO, Integer plRunDeploySeq, Map<String, K8sHorizontalPodAutoscalerVO> currHpaMap, V2HorizontalPodAutoscaler uptHpaV2, V2beta2HorizontalPodAutoscaler uptHpaV2beta2, V2beta1HorizontalPodAutoscaler uptHpaV2beta1, V1HorizontalPodAutoscaler uptHpaV1, boolean dryRun, JSON k8sJson, IPlMapper plDao, ExecutingContextVO context) throws Exception {

        if (plDao == null) {
            plDao = sqlSession.getMapper(IPlMapper.class);
        }

        String hpaName = null;
        try {
            if (uptHpaV2 != null) {
                if (uptHpaV2.getMetadata() != null) {
                    hpaName = uptHpaV2.getMetadata().getName();

                    // begin
                    if (!dryRun) {
                        this.updatePlRunDeployHpaStatusNEvent(uptHpaV2.getMetadata().getName(), PlStatus.RUNNING, null, plRunVO.getCallbackUrl(), plRunVO.getPlSeq(), plRunVO.getPlRunSeq(), plRunDeploySeq, plDao);
                    }

                    if (currHpaMap.containsKey(uptHpaV2.getMetadata().getName())) {
                        V2HorizontalPodAutoscaler currHpaV2 = ServerUtils.unmarshalYaml(currHpaMap.get(uptHpaV2.getMetadata().getName()).getDeploymentYaml(), k8sJson);
                        workloadResourceService.patchHorizontalPodAutoscalerV2(cluster, namespace, currHpaV2, uptHpaV2, dryRun, context);
                    } else {
                        k8sWorker.createHorizontalPodAutoscalerV2(cluster, namespace, uptHpaV2, dryRun);
                    }

                    // end
                    if (!dryRun) {
                        this.updatePlRunDeployHpaStatusNEvent(uptHpaV2.getMetadata().getName(), PlStatus.DONE, null, plRunVO.getCallbackUrl(), plRunVO.getPlSeq(), plRunVO.getPlRunSeq(), plRunDeploySeq, plDao);
                    }
                }
            } else if (uptHpaV2beta2 != null) {
                if (uptHpaV2beta2.getMetadata() != null) {
                    hpaName = uptHpaV2beta2.getMetadata().getName();

                    // begin
                    if (!dryRun) {
                        this.updatePlRunDeployHpaStatusNEvent(uptHpaV2beta2.getMetadata().getName(), PlStatus.RUNNING, null, plRunVO.getCallbackUrl(), plRunVO.getPlSeq(), plRunVO.getPlRunSeq(), plRunDeploySeq, plDao);
                    }

                    if (currHpaMap.containsKey(uptHpaV2beta2.getMetadata().getName())) {
                        V2beta2HorizontalPodAutoscaler currHpaV2beta2 = ServerUtils.unmarshalYaml(currHpaMap.get(uptHpaV2beta2.getMetadata().getName()).getDeploymentYaml(), k8sJson);
                        workloadResourceService.patchHorizontalPodAutoscalerV2beta2(cluster, namespace, currHpaV2beta2, uptHpaV2beta2, dryRun, context);
                    } else {
                        k8sWorker.createHorizontalPodAutoscalerV2beta2(cluster, namespace, uptHpaV2beta2, dryRun);
                    }

                    // end
                    if (!dryRun) {
                        this.updatePlRunDeployHpaStatusNEvent(uptHpaV2beta2.getMetadata().getName(), PlStatus.DONE, null, plRunVO.getCallbackUrl(), plRunVO.getPlSeq(), plRunVO.getPlRunSeq(), plRunDeploySeq, plDao);
                    }
                }
            } else if (uptHpaV2beta1 != null) {
                if (uptHpaV2beta1.getMetadata() != null) {
                    hpaName = uptHpaV2beta1.getMetadata().getName();

                    // begin
                    if (!dryRun) {
                        this.updatePlRunDeployHpaStatusNEvent(uptHpaV2beta1.getMetadata().getName(), PlStatus.RUNNING, null, plRunVO.getCallbackUrl(), plRunVO.getPlSeq(), plRunVO.getPlRunSeq(), plRunDeploySeq, plDao);
                    }

                    if (currHpaMap.containsKey(uptHpaV2beta1.getMetadata().getName())) {
                        V2beta1HorizontalPodAutoscaler currHpaV2beta1 = ServerUtils.unmarshalYaml(currHpaMap.get(uptHpaV2beta1.getMetadata().getName()).getDeploymentYaml(), k8sJson);
                        workloadResourceService.patchHorizontalPodAutoscalerV2beta1(cluster, namespace, currHpaV2beta1, uptHpaV2beta1, dryRun, context);
                    } else {
                        k8sWorker.createHorizontalPodAutoscalerV2beta1(cluster, namespace, uptHpaV2beta1, dryRun);
                    }

                    // end
                    if (!dryRun) {
                        this.updatePlRunDeployHpaStatusNEvent(uptHpaV2beta1.getMetadata().getName(), PlStatus.DONE, null, plRunVO.getCallbackUrl(), plRunVO.getPlSeq(), plRunVO.getPlRunSeq(), plRunDeploySeq, plDao);
                    }
                }
            } else if (uptHpaV1 != null) {
                if (uptHpaV1.getMetadata() != null) {
                    hpaName = uptHpaV1.getMetadata().getName();

                    // begin
                    if (!dryRun) {
                        this.updatePlRunDeployHpaStatusNEvent(uptHpaV1.getMetadata().getName(), PlStatus.RUNNING, null, plRunVO.getCallbackUrl(), plRunVO.getPlSeq(), plRunVO.getPlRunSeq(), plRunDeploySeq, plDao);
                    }

                    if (currHpaMap.containsKey(uptHpaV1.getMetadata().getName())) {
                        V1HorizontalPodAutoscaler currHpaV1 = ServerUtils.unmarshalYaml(currHpaMap.get(uptHpaV1.getMetadata().getName()).getDeploymentYaml(), k8sJson);
                        workloadResourceService.patchHorizontalPodAutoscalerV1(cluster, namespace, currHpaV1, uptHpaV1, dryRun, context);
                    } else {
                        k8sWorker.createHorizontalPodAutoscalerV1(cluster, namespace, uptHpaV1, dryRun);
                    }

                    // end
                    if (!dryRun) {
                        this.updatePlRunDeployHpaStatusNEvent(uptHpaV1.getMetadata().getName(), PlStatus.DONE, null, plRunVO.getCallbackUrl(), plRunVO.getPlSeq(), plRunVO.getPlRunSeq(), plRunDeploySeq, plDao);
                    }
                }
            }
        } catch (Exception e) {
            this.updatePlRunDeployHpaStatusNEvent(hpaName, PlStatus.ERROR, e.getMessage(), plRunVO.getCallbackUrl(), plRunVO.getPlSeq(), plRunVO.getPlRunSeq(), plRunDeploySeq, plDao);
            throw e;
        }
    }

    public void setContainerImage(V1PodTemplateSpec v1PodTemplateSpec, List<PlRunBuildVO> runBuilds, String registryUser) throws Exception {

        // 내부 registry url
        String registryUrl = ResourceUtil.getRegistryUrl();
        boolean usePrivate = false;
        List<V1Container> allContainers = new ArrayList<>();

        if (v1PodTemplateSpec != null && v1PodTemplateSpec.getSpec() != null) {
            if (CollectionUtils.isNotEmpty(v1PodTemplateSpec.getSpec().getInitContainers())) {
                allContainers.addAll(v1PodTemplateSpec.getSpec().getInitContainers());
            }
            if (CollectionUtils.isNotEmpty(v1PodTemplateSpec.getSpec().getContainers())) {
                allContainers.addAll(v1PodTemplateSpec.getSpec().getContainers());
            }

            for (V1Container c : allContainers) {
                // 내부 레지스트리 사용 여부 체크
                if (StringUtils.startsWith(c.getImage(), registryUrl)) {
                    if (!usePrivate) {
                        usePrivate = true;
                    }
                }
                // 빌드가 실행되고 맵핑된 이미지가 있다면 변경 처리함
                // 2021.04.28, hjchoi - 로직 변경 - 빌드가 실행되었다면 DONE 이고 실행되지 않았던 경우 이미지명을 비교하여 상이할 경우 셋팅
                Optional<PlRunBuildVO> runBuildOptional = runBuilds.stream()
                        .filter(rb -> {
                            if (!BooleanUtils.toBoolean(rb.getRunYn()) || rb.getRunStatus() == PlStatus.DONE) {
                                if (CollectionUtils.isNotEmpty(rb.getRunBuildDeployMapping())) {
                                    for (PlRunBuildDeployMappingVO runMapping : rb.getRunBuildDeployMapping()) {
                                        if (StringUtils.equals(c.getName(), runMapping.getContainerName())) {
                                            return true;
                                        }
                                    }
                                }
                            }

                            return false;
                        })
                        .findFirst();

                // 2021.04.30, hjchoi - 로직 변경
                // 파이프라인 빌드 실행 정보에 따라 image url 셋팅
                // - buildRunSeq가 존재할 경우 => imgUrl 값을 셋팅
                // - buildRunSeq가 존재하지 않을 경우 => buildPrevRunSeq로 BuildRun 정보를 조회하여 imageUrl 값을 셋팅
                if (runBuildOptional.isPresent()) {
                    boolean isSameImgName = false;
                    if (runBuildOptional.get().getBuildRunSeq() != null) {
                        if (StringUtils.equals(c.getImage(), runBuildOptional.get().getImgUrl())) {
                            isSameImgName = true;
                        }
                        c.setImage(runBuildOptional.get().getImgUrl());
                    } else {
                        BuildRunVO buildRun = buildService.getBuildRun(runBuildOptional.get().getBuildPrevRunSeq(), "Y", false);
                        if (buildRun != null && buildRun.getRunState() == RunState.DONE && StringUtils.isNotBlank(buildRun.getImageUrl())) {
                            if (StringUtils.equals(c.getImage(), buildRun.getImageUrl())) {
                                isSameImgName = true;
                            }
                            c.setImage(buildRun.getImageUrl());
                        }
                    }

                    // 빌드한 이미지의 이미지명:태그명이 기존과 동일하다면 재배포 및 이미지를 다시 pull하지 않기 때문에 restartedAt annotation 추가과 pullPolicy(Always)를 변경하여 줌.
                    if (isSameImgName) {
                        // 재배포를 위해 'kubectl.kubernetes.io/restartedAt' annotation 추가
                        Optional.ofNullable(v1PodTemplateSpec.getMetadata()).orElseGet(() ->new V1ObjectMeta()).putAnnotationsItem(KubeConstants.META_ANNOTATIONS_RESTARTED_AT, Utils.getNowDateTime(Utils.DEFAULT_DATE_TIME_ZONE_FORMAT));
                        // Always로 변경
                        c.setImagePullPolicy("Always");
                    }
                }
            }

            // 내부 레지스트리 사용자 셋팅
            if (usePrivate) {
                if (StringUtils.isNotBlank(registryUser)) {
                    V1LocalObjectReference ref = new V1LocalObjectReference();
                    ref.setName(registryUser);
                    if (CollectionUtils.isNotEmpty(v1PodTemplateSpec.getSpec().getImagePullSecrets())) {
                        if (!v1PodTemplateSpec.getSpec().getImagePullSecrets().contains(ref)) {
                            v1PodTemplateSpec.getSpec().addImagePullSecretsItem(ref);
                        }
                    } else {
                        v1PodTemplateSpec.getSpec().addImagePullSecretsItem(ref);
                    }
                }
            }
        }
    }

    /**
     * 이미지 업데이트((구)파이프라인))의 이미지정보 업데이트
     *
     * @param cluster
     * @param namespace
     * @param serviceSeq
     * @param workloadType
     * @param workloadVersion
     * @param workloadDesc
     * @param objs
     * @param context
     * @throws Exception
     */
    public void mergeOldPipeline(ClusterVO cluster, String namespace, Integer serviceSeq, String workloadType, String workloadVersion, String workloadDesc, List<Object> objs, ExecutingContextVO context) {
        try {
            ServerGuiVO serverGui = serverConversionService.convertYamlToGui(cluster, namespace, workloadType, workloadVersion, workloadDesc, null, objs);
            serverGui.getComponent().setClusterSeq(cluster.getClusterSeq());
            serverGui.getComponent().setNamespaceName(namespace);
            pipelineFlowService.mergePipeline(serverGui, serviceSeq, context);
        } catch (Exception e) {
            CocktailException ce = new CocktailException("fail merge pipeline!!", e, ExceptionType.InternalError);
            log.error("fail merge pipeline!!", ce);
        }
    }

    public void updatePlRunDeployStatusNEvent(PlResType resType, String resName, PlStatus runStatus, String runLog, String callbackUrl, Integer plSeq, Integer plRunSeq, Integer plRunDeploySeq, IPlMapper plDao) {
        if (plDao == null) {
            plDao = sqlSession.getMapper(IPlMapper.class);
        }

        // plRun 상태 - ERROR가 아니면 RUNNING
        PlStatus plRunStatus = PlStatus.RUNNING;

        StringBuilder logSb = new StringBuilder();
        logSb.append("\n##### ")
                .append(runStatus.getAction()) // Start, Error, Done
                .append(String.format("Deploy %s > %s #####", resType.getValue(), resName)); // resType resName

        if (runStatus == PlStatus.ERROR) {

            plRunStatus = runStatus;

            if (StringUtils.isNotBlank(runLog)) {
                logSb.append("\n    - cause : ").append(runLog);
            }
        }

        plDao.updatePlRunDeployStatus(runStatus.getCode(), logSb.toString(), plRunDeploySeq);

        // event
        PlEventResVO eventRes = new PlEventResVO();
        eventRes.setResType(resType.getValue());
        eventRes.setResName(resName);
        eventRes.setResState(runStatus.getCode());
        plEventService.sendPlRunState(callbackUrl, plSeq, plRunSeq, plRunStatus, eventRes);
    }

    public void updatePlRunDeployHpaStatusNEvent(String resName, PlStatus runStatus, String runLog, String callbackUrl, Integer plSeq, Integer plRunSeq, Integer plRunDeploySeq, IPlMapper plDao) {
        if (plDao == null) {
            plDao = sqlSession.getMapper(IPlMapper.class);
        }

        // plRun 상태 - ERROR가 아니면 RUNNING
        PlStatus plRunStatus = PlStatus.RUNNING;

        StringBuilder logSb = new StringBuilder();
        logSb.append(String.format("\n    - %s Deploy %s > %s ", runStatus.getAction(), K8sApiKindType.HORIZONTAL_POD_AUTOSCALER.getValue(), resName)); // Start, Error, Done, resType resName

        if (runStatus == PlStatus.ERROR) {

            plRunStatus = runStatus;

            if (StringUtils.isNotBlank(runLog)) {
                logSb.append("\n        - cause : ").append(runLog);
            }
        }

        plDao.updatePlRunDeployStatus(runStatus.getCode(), logSb.toString(), plRunDeploySeq);

        // event
        PlEventResVO eventRes = new PlEventResVO();
        eventRes.setResType(K8sApiKindType.HORIZONTAL_POD_AUTOSCALER.getValue());
        eventRes.setResName(resName);
        eventRes.setResState(runStatus.getCode());
        plEventService.sendPlRunState(callbackUrl, plSeq, plRunSeq, plRunStatus, eventRes);
    }

    /**
     * 파이프라인 워크로드 실행 리소스 상세 조회
     *
     * @param plSeq
     * @param plRunSeq
     * @param plRunDeploySeq
     * @param deployType
     * @return
     * @throws Exception
     */
    public ServerIntegrateVO getWorkloadRunDeployResourceDetail(Integer plSeq, Integer plRunSeq, Integer plRunDeploySeq, String deployType) throws Exception {

        IPlMapper plDao = sqlSession.getMapper(IPlMapper.class);
        IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);

        PlRunDeployVO plResource = plDao.getPlRunDeploy(plRunSeq, plRunDeploySeq);
        PlMasterVO plMaster = plDao.getPlMaster(plSeq);
        ClusterVO cluster = clusterDao.getCluster(plMaster.getClusterSeq());

        ServerIntegrateVO serverIntegrate = new ServerIntegrateVO();
        serverIntegrate.setDeployType(deployType);

        if (plResource != null) {
            WorkloadType workloadType = WorkloadType.valueOf(plResource.getResType());
            List<Object> objs = ServerUtils.getYamlObjects(plResource.getResCont());

            WorkloadVersion workloadVersion = WorkloadVersion.V1;

            // annotation에 설정된 설명을 워크로드별로 파싱하여 가져옴.
            String workloadDesc = null;
            for (Object obj : objs) {
                Map<String, Object> k8sObjectToMap = ServerUtils.getK8sObjectToMap(obj, K8sJsonUtils.getJson());
                K8sApiKindType kind = ServerUtils.getK8sKindInMap(k8sObjectToMap);

                switch (kind) {
                    case DEPLOYMENT:
                    case STATEFUL_SET:
                    case DAEMON_SET:
                    case JOB:
                    case CRON_JOB:
                        V1ObjectMeta objectMeta = ServerUtils.getK8sObjectMetaInMap(k8sObjectToMap, K8sJsonUtils.getJson());
                        if (objectMeta != null) {
                            workloadDesc = serverConversionService.getDescription(objectMeta.getAnnotations());
                        }
                        break;
                }
            }

            if (DeployType.valueOf(deployType) == DeployType.GUI) {
                ServerGuiVO serverGui = serverConversionService.convertYamlToGui(cluster, plMaster.getNamespace(), workloadType.getCode(), workloadVersion.getCode(), workloadDesc, null, objs);

                // set buildSeq
                if (CollectionUtils.isNotEmpty(plResource.getPlRunBuilds())) {
                    // Map<containerName, buildSeq>
                    Map<String, Integer> buildInfoVOMap = Maps.newHashMap();
                    for (PlRunBuildVO resBuild : plResource.getPlRunBuilds()) {
                        if (CollectionUtils.isNotEmpty(resBuild.getRunBuildDeployMapping())) {
                            for (PlRunBuildDeployMappingVO mapping : resBuild.getRunBuildDeployMapping()) {
                                buildInfoVOMap.put(mapping.getContainerName(), resBuild.getBuildSeq());
                            }
                        }
                    }

                    List<ContainerVO> allContainers = Lists.newArrayList();
                    ResourceUtil.mergeContainer(allContainers, serverGui.getInitContainers(), serverGui.getContainers());

                    for (ContainerVO cRow : allContainers) {
                        if (MapUtils.getObject(buildInfoVOMap, cRow.getContainerName(), null) != null) {
                            cRow.setBuildSeq(buildInfoVOMap.get(cRow.getContainerName()));
                        }
                    }
                }

                serverIntegrate = serverGui;
            } else {
                ServerYamlVO serverYaml = new ServerYamlVO();
                serverYaml.setDeployType(DeployType.YAML.getCode());
                serverYaml.setWorkloadType(workloadType.getCode());
                serverYaml.setWorkloadVersion(workloadVersion.getCode());
                serverYaml.setClusterSeq(cluster.getClusterSeq());
                serverYaml.setNamespaceName(plMaster.getNamespace());
                serverYaml.setWorkloadName(plResource.getResName());
                serverYaml.setDescription(workloadDesc);
                serverYaml.setYaml(plResource.getResCont());

                serverIntegrate = serverYaml;
            }
        }

        return serverIntegrate;
    }

    /**
     * 파이프라인 컨피그맵 실행 리소스 상세 조회
     *
     * @param plSeq
     * @param plRunSeq
     * @param plRunDeploySeq
     * @return
     * @throws Exception
     */
    public ConfigMapGuiVO getConfigMapRunDeployResourceDetail(Integer plSeq, Integer plRunSeq, Integer plRunDeploySeq) throws Exception {
        IPlMapper plDao = sqlSession.getMapper(IPlMapper.class);

        PlRunDeployVO plResource = plDao.getPlRunDeploy(plRunSeq, plRunDeploySeq);

        ConfigMapGuiVO gui = new ConfigMapGuiVO();
        gui.setDeployType(DeployType.GUI.getCode());

        if (plResource != null && PlResType.valueOf(plResource.getResType()) == PlResType.CM) {
            List<Object> obj = ServerUtils.getYamlObjects(plResource.getResCont());
            V1ConfigMap k8sRes = (V1ConfigMap)Optional.ofNullable(obj).orElseGet(() ->Lists.newArrayList()).get(0);
            gui = configMapService.convertConfigMapData(k8sRes);
        }

        return gui;
    }

    /**
     * 파이프라인 시크릿 실행 리소스 상세 조회
     *
     * @param plSeq
     * @param plRunSeq
     * @param plRunDeploySeq
     * @return
     * @throws Exception
     */
    public SecretGuiVO getSecretRunDeployResourceDetail(Integer plSeq, Integer plRunSeq, Integer plRunDeploySeq) throws Exception {
        IPlMapper plDao = sqlSession.getMapper(IPlMapper.class);

        PlRunDeployVO plResource = plDao.getPlRunDeploy(plRunSeq, plRunDeploySeq);

        SecretGuiVO gui = new SecretGuiVO();
        gui.setDeployType(DeployType.GUI.getCode());

        if (plResource != null && PlResType.valueOf(plResource.getResType()) == PlResType.SC) {
            // Yaml 복호화
            List<Object> obj = ServerUtils.getYamlObjects(CryptoUtils.decryptAES(plResource.getResCont()));
            V1Secret k8sRes = (V1Secret)Optional.ofNullable(obj).orElseGet(() ->Lists.newArrayList()).get(0);
            gui = secretService.convertSecretData(k8sRes);
        }

        return gui;
    }

    /**
     * 파이프라인 서비스 실행 리소스 상세 조회
     *
     * @param plSeq
     * @param plRunSeq
     * @param plRunDeploySeq
     * @return
     * @throws Exception
     */
    public K8sServiceVO getServiceRunDeployResourceDetail(Integer plSeq, Integer plRunSeq, Integer plRunDeploySeq) throws Exception {
        IPlMapper plDao = sqlSession.getMapper(IPlMapper.class);

        PlRunDeployVO plResource = plDao.getPlRunDeploy(plRunSeq, plRunDeploySeq);

        K8sServiceVO k8sService = null;

        if (plResource != null && PlResType.valueOf(plResource.getResType()) == PlResType.SVC) {
            k8sService = plService.convertServiceYamlToK8sObj(plResource.getResCont());
        }

        return k8sService;
    }

    /**
     * 파이프라인 인그레스 실행 리소스 상세 조회
     *
     * @param plSeq
     * @param plRunSeq
     * @param plRunDeploySeq
     * @return
     * @throws Exception
     */
    public K8sIngressVO getIngressRunDeployResourceDetail(Integer plSeq, Integer plRunSeq, Integer plRunDeploySeq) throws Exception {
        IPlMapper plDao = sqlSession.getMapper(IPlMapper.class);

        PlRunDeployVO plResource = plDao.getPlRunDeploy(plRunSeq, plRunDeploySeq);

        K8sIngressVO k8sIngress = new K8sIngressVO();

        if (plResource != null && PlResType.valueOf(plResource.getResType()) == PlResType.IG) {
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
        }

        return k8sIngress;
    }

    /**
     * 파이프라인 pvc 실행 리소스 상세 조회
     *
     * @param plSeq
     * @param plRunSeq
     * @param plRunDeploySeq
     * @return
     * @throws Exception
     */
    public K8sPersistentVolumeClaimVO getPvcRunDeployResourceDetail(Integer plSeq, Integer plRunSeq, Integer plRunDeploySeq) throws Exception {
        IPlMapper plDao = sqlSession.getMapper(IPlMapper.class);
        IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);

        PlRunDeployVO plResource = plDao.getPlRunDeploy(plRunSeq, plRunDeploySeq);
        PlMasterVO plMaster = plDao.getPlMaster(plSeq);
        ClusterVO cluster = clusterDao.getCluster(plMaster.getClusterSeq());
        List<ClusterVolumeVO> clusterVolumes = clusterVolumeService.getStorageVolumes(null, null, cluster.getClusterSeq(), null, null, false, false);

        K8sPersistentVolumeClaimVO k8sPersistentVolumeClaim = new K8sPersistentVolumeClaimVO();
        Map<String, K8sPersistentVolumeVO> persistentVolumesMap = new HashMap<>();
        JSON k8sJson = new JSON();

        if (plResource != null && PlResType.valueOf(plResource.getResType()) == PlResType.PVC) {
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
        }

        return k8sPersistentVolumeClaim;
    }
}

































