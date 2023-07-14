package run.acloud.api.cserver.service;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.JSON;
import io.kubernetes.client.openapi.models.*;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import run.acloud.api.configuration.dao.IClusterMapper;
import run.acloud.api.configuration.vo.ClusterVO;
import run.acloud.api.cserver.dao.IServiceMapper;
import run.acloud.api.cserver.enums.StateCode;
import run.acloud.api.cserver.enums.WorkloadType;
import run.acloud.api.cserver.util.ServerUtils;
import run.acloud.api.cserver.vo.ServerGuiVO;
import run.acloud.api.cserver.vo.ServerYamlVO;
import run.acloud.api.k8sextended.models.V1beta1CronJob;
import run.acloud.api.k8sextended.models.V2beta1HorizontalPodAutoscaler;
import run.acloud.api.pipelineflow.service.PipelineFlowService;
import run.acloud.api.resource.constants.KubeConstants;
import run.acloud.api.resource.dao.IComponentMapper;
import run.acloud.api.resource.enums.*;
import run.acloud.api.resource.service.K8sResourceService;
import run.acloud.api.resource.service.ServiceSpecService;
import run.acloud.api.resource.service.WorkloadResourceService;
import run.acloud.api.resource.task.K8sSpecFactory;
import run.acloud.api.resource.task.K8sWorker;
import run.acloud.api.resource.util.ResourceUtil;
import run.acloud.api.resource.vo.*;
import run.acloud.commons.constants.CommonConstants;
import run.acloud.commons.util.CryptoUtils;
import run.acloud.commons.util.JsonUtils;
import run.acloud.commons.util.Utils;
import run.acloud.commons.vo.ExecutingContextVO;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;

import java.util.*;

@Slf4j
@Service
public class ServerProcessService {

    private enum KubeResourceCreationStatus {
        DeploymentCreated,
        DaemonSetCreated,
        StatefulSetCreated,
        JobCreated,
        CronJobCreated,
        ServiceCreated,
        PersistentVolumeClaimCreated,
        IngressCreated,
        HorizontalPodAutoscalerCreated
    }

    @Resource(name = "cocktailSession")
    private SqlSessionTemplate sqlSession;

    @Autowired
    private ServerConversionService serverConversionService;

    @Autowired
    private ComponentService componentService;

    @Autowired
    private K8sWorker k8sWorker;

    @Autowired
    private PipelineFlowService pipelineFlowService;

    @Autowired
    private K8sResourceService k8sResourceService;

    @Autowired
    private ServiceSpecService serviceSpecService;

    @Autowired
    private WorkloadResourceService workloadResourceService;


    /**
     * #################################################################################################################################################################################################
     * #################################################################################################################################################################################################
     * start - server
     * #################################################################################################################################################################################################
     * #################################################################################################################################################################################################
     */


    /**
     * DeployService.cubeDeploy에서 호출
     * @param serverParam
     * @return
     * @throws Exception
     */
    public ServerGuiVO addServerProcess(ServerGuiVO serverParam, ExecutingContextVO context) throws Exception {
        log.debug("addServer from ServerProcessService.addServerProcess");
        log.info(String.format("[STEP2] Start Add Server (addServerProcess) : %s - %s - %s", serverParam.getComponent().getClusterSeq(), serverParam.getComponent().getNamespaceName(), serverParam.getComponent().getComponentName()));

        IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
        IServiceMapper serviceDao = sqlSession.getMapper(IServiceMapper.class);
        IComponentMapper componentDao = sqlSession.getMapper(IComponentMapper.class);

        ClusterVO cluster = clusterDao.getCluster(serverParam.getComponent().getClusterSeq());
        cluster.setNamespaceName(serverParam.getComponent().getNamespaceName());

        // set image secret
        // 2022.03.21 hjchoi
        // 이제 이미지 pull 사용자는 워크스페이스가 아닌 플랫폼에 등록된 사용자로 셋팅
        serverParam.getServer().setImageSecret(cluster.getAccount().getRegistryDownloadUserId());

        /** 워크로드 배포시 워크로드에 Group 정보 Annotation 추가 : 2020.02.04 : redion **/
        if(serverParam.getComponent().getWorkloadGroupSeq() != null && serverParam.getComponent().getWorkloadGroupSeq() > 0) {
            Map<String, String> annotations = Optional.ofNullable(serverParam.getServer()).map(ServerVO::getAnnotations).orElseGet(() ->Maps.newHashMap());
            annotations.put(KubeConstants.ANNOTATION_COCKTAIL_GROUP_NO, serverParam.getComponent().getWorkloadGroupSeq().toString());
            serverParam.getServer().setAnnotations(annotations);
        }

        ServerDetailVO serverDetail = serverConversionService.convertToServerDetail(serverParam);
        serverDetail.setUpdater(context.getUserSeq());
        ServerVO server = serverDetail.getServer();
        List<ContainerVO> allContainers = new ArrayList<>();
        ResourceUtil.mergeContainer(allContainers, server.getInitContainers(), server.getContainers());

        String hpaName = "";
        if (serverParam.getServer() != null
            && serverParam.getServer().getHpa() != null
            && CollectionUtils.isNotEmpty(serverParam.getServer().getHpa().getMetrics())) {
            hpaName = serverParam.getServer().getHpa().getName();
        }

//        componentService.updateComponentState(context, serverParam, StateCode.DEPLOYING, componentDao);
        Set<KubeResourceCreationStatus> creationStatus = new HashSet<>();

        StringBuffer yamlStr = new StringBuffer();

        try {
            allContainers.forEach(item -> {
                item.setImagePullPolicy(ImagePullPolicyType.Always.getCode());
                if (StringUtils.indexOf(item.getFullImageName(), ":") < 1) {
                    item.setFullImageName(String.format("%s:latest", item.getFullImageName()));
                }
            });

            // last updated time 셋팅
            if (MapUtils.isEmpty(serverDetail.getServer().getAnnotations())) {
                serverDetail.getServer().setAnnotations(Maps.newHashMap());
            }
            serverDetail.getServer().getAnnotations().put(KubeConstants.ANNOTATION_COCKTAIL_DEPLOY_DATETIME, Utils.getNowDateTime());

            log.info(String.format("[STEP3] Start Workload Deployment [%s] (addServerProcess) : %s - %s - %s", serverParam.getServer().getWorkloadType(), serverParam.getComponent().getClusterSeq(), serverParam.getComponent().getNamespaceName(), serverParam.getComponent().getComponentName()));

            boolean hasHPA = serverParam.getServer() != null
                    && serverParam.getServer().getHpa() != null
                    && CollectionUtils.isNotEmpty(serverParam.getServer().getHpa().getMetrics());

            /**
             * Deployment
             */
            if(WorkloadType.valueOf(serverParam.getServer().getWorkloadType()) == WorkloadType.SINGLE_SERVER
                    || WorkloadType.valueOf(serverParam.getServer().getWorkloadType()) == WorkloadType.REPLICA_SERVER){
                try {

                    /**
                     * Error 시 yaml 저장용
                     */
                    K8sApiVerKindType deploymentType = ResourceUtil.getApiVerKindType(cluster, K8sApiKindType.DEPLOYMENT);
                    if (deploymentType != null) {
                        if (deploymentType.getGroupType() == K8sApiGroupType.APPS && deploymentType.getApiType() == K8sApiType.V1) {
                            V1Deployment v1Deployment = K8sSpecFactory.buildDeploymentV1(serverDetail, cluster.getNamespaceName());
                            yamlStr.append(ServerUtils.marshalYaml(v1Deployment));
                        }
                    }
                    this.setHpaYamlLog(yamlStr, cluster, serverParam, hpaName, deploymentType); // HPA 로그 추가

                    // Creates a server
                    // 이전에 pod가 생성되기를 기다리던 코드는 빠짐. Deployment만 실행.
                    creationStatus.add(KubeResourceCreationStatus.DeploymentCreated);
                    log.info(String.format("[STEP3][1] Before Creating Workload [%s] (addServerProcess) : %s - %s - %s", serverParam.getServer().getWorkloadType(), serverParam.getComponent().getClusterSeq(), serverParam.getComponent().getNamespaceName(), serverParam.getComponent().getComponentName()));
                    if (hasHPA) {
                        serverDetail.getServer().setComputeTotal(null);
                    }
                    K8sDeploymentVO k8sDeployment = workloadResourceService.createDeployment(cluster, serverDetail, context);
                    if (k8sDeployment == null) {
                        log.info(String.format("[STEP3][1] Workload Creation Failed [%s] (addServerProcess) : %s - %s - %s\n%s\n-----\n%s", serverParam.getServer().getWorkloadType(), serverParam.getComponent().getClusterSeq(), serverParam.getComponent().getNamespaceName(), serverParam.getComponent().getComponentName(), JsonUtils.toGson(serverDetail), yamlStr));
                        throw new CocktailException("Can't create Deployment", ExceptionType.K8sDeploymentCreationFail);
                    }

                    log.info(String.format("[STEP3][2] Before Creating Autoscaler [%s] (addServerProcess) : %s - %s - %s", serverParam.getServer().getWorkloadType(), serverParam.getComponent().getClusterSeq(), serverParam.getComponent().getNamespaceName(), serverParam.getComponent().getComponentName()));
                    if (hasHPA) {
                        // Create HorizontalPodAutoscaler
                        creationStatus.add(KubeResourceCreationStatus.HorizontalPodAutoscalerCreated);

                        // Cocktail v2.5.4 : K8s 1.9 + Metric Server 기능 지원 개발 진행 중 HPA 동작 처리를 기존 Fabric8 -> K8s 공식 클라이언트로 변경 처리함
                        boolean bIsSuccess = this.workloadResourceService.createHorizontalPodAutoscaler(cluster, cluster.getNamespaceName(), serverParam.getComponent(), serverParam.getServer().getHpa(), hpaName, deploymentType);
                        if (!bIsSuccess) {
                            log.info(String.format("[STEP3][2] Autoscaler Creation Failed [%s] (addServerProcess) : %s - %s - %s\n%s\n-----\n%s", serverParam.getServer().getWorkloadType(), serverParam.getComponent().getClusterSeq(), serverParam.getComponent().getNamespaceName(), serverParam.getComponent().getComponentName(), JsonUtils.toGson(serverParam.getServer().getHpa()), yamlStr));
                            throw new CocktailException("Can't create HorizontalPodAutoscaler", ExceptionType.K8sHorizontalPodAutoscalerCreationFail);
                        }
                    }

                    log.info(String.format("CocktailServer created - name: %s", serverParam.getComponent().getComponentName()));

//                    // K8S Service 생성 결과로 Cocktail Server 정보를 수정한다.
//                    server.setUpdater(context.getUserSeq());
//                    componentDao.updateServer(server);

//                    componentService.updateComponentState(context, serverParam, StateCode.RUNNING, componentDao);
                } catch (Exception e) {
                    log.error("K8S cocktail server creation fail: " + e.getMessage());
                    log.error(ExceptionUtils.getStackTrace(e));
                    if (creationStatus.contains(KubeResourceCreationStatus.DeploymentCreated)) {
                        workloadResourceService.deleteDeployment(cluster, serverDetail, context);
                    }
                    if (creationStatus.contains(KubeResourceCreationStatus.HorizontalPodAutoscalerCreated)) {
                        // Cocktail v2.5.4 : K8s 1.9 + Metric Server 기능 지원 개발 진행 중 HPA 동작 처리를 기존 Fabric8 -> K8s 공식 클라이언트로 변경 처리함
                        if(serverParam != null && serverParam.getServer() != null && serverParam.getServer().getHpa() != null) {
                            this.workloadResourceService.deleteHorizontalPodAutoscaler(cluster, cluster.getNamespaceName(), hpaName, serverParam.getServer().getHpa());
                        } else {
                            this.workloadResourceService.deleteHorizontalPodAutoscaler(cluster, cluster.getNamespaceName(), hpaName, null);
                        }
                    }
//                    String errMsg = this.getApiErrorMessage(e);
//                    componentService.updateComponentState(context, serverParam.getComponent(), StateCode.ERROR, errMsg, componentDao);
//                    componentDao.updateComponentManifestByNamespace(serverParam.getComponent().getClusterSeq(), serverParam.getComponent().getNamespaceName(), serverParam.getComponent().getComponentName(), yamlStr.toString(), null);

                    throw e;
                }
            }
            /**
             * DaemonSet
             */
            else if(WorkloadType.valueOf(serverParam.getServer().getWorkloadType()) == WorkloadType.DAEMON_SET_SERVER){
                try {

                    /**
                     * Error 시 yaml 저장용
                     */
                    K8sApiVerKindType daemonSetType = ResourceUtil.getApiVerKindType(cluster, K8sApiKindType.DAEMON_SET);
                    if (daemonSetType != null) {
                        if (daemonSetType.getGroupType() == K8sApiGroupType.APPS && daemonSetType.getApiType() == K8sApiType.V1) {
                            V1DaemonSet v1DaemonSet = K8sSpecFactory.buildDaemonSetV1(serverDetail, cluster.getNamespaceName());
                            yamlStr.append(ServerUtils.marshalYaml(v1DaemonSet));
                        }
                    }


                    // Creates a server
                    // 이전에 pod가 생성되기를 기다리던 코드는 빠짐. DaemonSet만 실행.
                    creationStatus.add(KubeResourceCreationStatus.DaemonSetCreated);
                    log.info(String.format("[STEP3][1] Before Creating Workload [%s] (addServerProcess) : %s - %s - %s", serverParam.getServer().getWorkloadType(), serverParam.getComponent().getClusterSeq(), serverParam.getComponent().getNamespaceName(), serverParam.getComponent().getComponentName()));
                    K8sDaemonSetVO k8sDaemonSet = workloadResourceService.createDaemonSet(cluster, serverDetail, context);
                    if (k8sDaemonSet == null) {
                        log.info(String.format("[STEP3][1] Workload Creation Failed [%s] (addServerProcess) : %s - %s - %s\n%s\n-----\n%s", serverParam.getServer().getWorkloadType(), serverParam.getComponent().getClusterSeq(), serverParam.getComponent().getNamespaceName(), serverParam.getComponent().getComponentName(), JsonUtils.toGson(serverDetail), yamlStr));
                        throw new CocktailException("Can't create DaemonSet", ExceptionType.K8sDaemonSetCreationFail);
                    }

                    log.info(String.format("CocktailServer created - name: %s", serverParam.getComponent().getComponentName()));

//                    componentService.updateComponentState(context, serverParam.getComponent(), StateCode.RUNNING, componentDao);
                } catch (Exception e) {
                    log.error("K8S cocktail server creation fail: " + e.getMessage());
                    log.error(ExceptionUtils.getStackTrace(e));
                    if (creationStatus.contains(KubeResourceCreationStatus.DaemonSetCreated)) {
                        workloadResourceService.deleteDaemonSet(cluster, serverDetail, context);
                    }
//                    String errMsg = this.getApiErrorMessage(e);
//                    componentService.updateComponentState(context, serverParam.getComponent(), StateCode.ERROR, errMsg, componentDao);
//                    componentDao.updateComponentManifestByNamespace(serverParam.getComponent().getClusterSeq(), serverParam.getComponent().getNamespaceName(), serverParam.getComponent().getComponentName(), yamlStr.toString(), null);

                    throw e;
                }
            }
            /**
             * Job
             */
            else if(WorkloadType.valueOf(serverParam.getServer().getWorkloadType()) == WorkloadType.JOB_SERVER){
                try {

                    /**
                     * Error 시 yaml 저장용
                     */
                    K8sApiVerKindType jobType = ResourceUtil.getApiVerKindType(cluster, K8sApiKindType.JOB);
                    if (jobType != null) {
                        if (jobType.getGroupType() == K8sApiGroupType.BATCH && jobType.getApiType() == K8sApiType.V1) {
                            V1Job v1Job = K8sSpecFactory.buildJobV1(serverDetail, cluster.getNamespaceName());
                            yamlStr.append(ServerUtils.marshalYaml(v1Job));
                        }
                    }

//                    componentDao.updateComponentManifestByNamespace(serverParam.getComponent().getClusterSeq(), serverParam.getComponent().getNamespaceName(), serverParam.getComponent().getComponentName(), yamlStr.toString(), null);

                    // Creates a server
                    creationStatus.add(KubeResourceCreationStatus.JobCreated);
                    log.info(String.format("[STEP3][1] Before Creating Workload [%s] (addServerProcess) : %s - %s - %s", serverParam.getServer().getWorkloadType(), serverParam.getComponent().getClusterSeq(), serverParam.getComponent().getNamespaceName(), serverParam.getComponent().getComponentName()));
                    K8sJobVO k8sJob = workloadResourceService.createJob(cluster, serverDetail, context);
                    if (k8sJob == null) {
                        log.info(String.format("[STEP3][1] Workload Creation Failed [%s] (addServerProcess) : %s - %s - %s\n%s\n-----\n%s", serverParam.getServer().getWorkloadType(), serverParam.getComponent().getClusterSeq(), serverParam.getComponent().getNamespaceName(), serverParam.getComponent().getComponentName(), JsonUtils.toGson(serverDetail), yamlStr));
                        throw new CocktailException("Can't create Job", ExceptionType.K8sJobCreationFail);
                    }

                    log.info(String.format("CocktailServer created - name: %s", serverParam.getComponent().getComponentName()));

//                    componentService.updateComponentState(context, serverParam.getComponent(), StateCode.RUNNING, componentDao);
                } catch (Exception e) {
                    log.error("K8S cocktail server creation fail: " + e.getMessage());
                    log.error(ExceptionUtils.getStackTrace(e));
                    if (creationStatus.contains(KubeResourceCreationStatus.JobCreated)) {
                        workloadResourceService.deleteJob(cluster, serverDetail, context);
                    }
//                    String errMsg = this.getApiErrorMessage(e);
//                    componentService.updateComponentState(context, serverParam.getComponent(), StateCode.ERROR, errMsg, componentDao);
//                    componentDao.updateComponentManifestByNamespace(serverParam.getComponent().getClusterSeq(), serverParam.getComponent().getNamespaceName(), serverParam.getComponent().getComponentName(), yamlStr.toString(), null);

                    throw e;
                }
            }
            /**
             * CronJob
             */
            else if(WorkloadType.valueOf(serverParam.getServer().getWorkloadType()) == WorkloadType.CRON_JOB_SERVER){
                try {
                    /**
                     * Error 시 yaml 저장용
                     */
                    K8sApiVerKindType cronJobType = ResourceUtil.getApiVerKindType(cluster, K8sApiKindType.CRON_JOB);
                    if (cronJobType != null) {
                        if (cronJobType.getGroupType() == K8sApiGroupType.BATCH && cronJobType.getApiType() == K8sApiType.V1BETA1) {
                            V1beta1CronJob cronJob = K8sSpecFactory.buildCronJobV1beta1(serverDetail, cluster.getNamespaceName());
                            yamlStr.append(ServerUtils.marshalYaml(cronJob));
                        } else if (cronJobType.getGroupType() == K8sApiGroupType.BATCH && cronJobType.getApiType() == K8sApiType.V1) {
                            V1CronJob cronJob = K8sSpecFactory.buildCronJobV1(serverDetail, cluster.getNamespaceName());
                            yamlStr.append(ServerUtils.marshalYaml(cronJob));
                        }
                    }

//                    componentDao.updateComponentManifestByNamespace(serverParam.getComponent().getClusterSeq(), serverParam.getComponent().getNamespaceName(), serverParam.getComponent().getComponentName(), yamlStr.toString(), null);

                    // Creates a server
                    creationStatus.add(KubeResourceCreationStatus.CronJobCreated);
                    log.info(String.format("[STEP3][1] Before Creating Workload [%s] (addServerProcess) : %s - %s - %s", serverParam.getServer().getWorkloadType(), serverParam.getComponent().getClusterSeq(), serverParam.getComponent().getNamespaceName(), serverParam.getComponent().getComponentName()));
                    K8sCronJobVO k8sCronJob = workloadResourceService.createCronJob(cluster, serverDetail, context);
                    if (k8sCronJob == null) {
                        log.info(String.format("[STEP3][1] Workload Creation Failed [%s] (addServerProcess) : %s - %s - %s\n%s\n-----\n%s", serverParam.getServer().getWorkloadType(), serverParam.getComponent().getClusterSeq(), serverParam.getComponent().getNamespaceName(), serverParam.getComponent().getComponentName(), JsonUtils.toGson(serverDetail), yamlStr));
                        throw new CocktailException("Can't create CronJob", ExceptionType.K8sCronJobCreationFail);
                    }

                    log.info(String.format("CocktailServer created - name: %s", serverParam.getComponent().getComponentName()));

//                    componentService.updateComponentState(context, serverParam.getComponent(), StateCode.RUNNING, componentDao);

                } catch (Exception e) {
                    log.error("K8S cocktail server creation fail: " + e.getMessage());
                    log.error(ExceptionUtils.getStackTrace(e));
                    if (creationStatus.contains(KubeResourceCreationStatus.CronJobCreated)) {
                        workloadResourceService.deleteCronJob(cluster, serverDetail, context);
                    }

//                    String errMsg = this.getApiErrorMessage(e);
//                    componentService.updateComponentState(context, serverParam.getComponent(), StateCode.ERROR, errMsg, componentDao);
//                    componentDao.updateComponentManifestByNamespace(serverParam.getComponent().getClusterSeq(), serverParam.getComponent().getNamespaceName(), serverParam.getComponent().getComponentName(), yamlStr.toString(), null);

                    throw e;
                }
            }
            /**
             * StatefulSet
             */
            else if(WorkloadType.valueOf(serverParam.getServer().getWorkloadType()) == WorkloadType.STATEFUL_SET_SERVER){
                try {

                    /**
                     * Error 시 yaml 저장용
                     */
                    K8sApiVerKindType statefulSetType = ResourceUtil.getApiVerKindType(cluster, K8sApiKindType.STATEFUL_SET);
                    if (statefulSetType != null) {
                        if (statefulSetType.getGroupType() == K8sApiGroupType.APPS && statefulSetType.getApiType() == K8sApiType.V1) {
                            V1StatefulSet v1StatefulSet = K8sSpecFactory.buildStatefulSetV1(serverDetail, cluster.getNamespaceName());
                            yamlStr.append(ServerUtils.marshalYaml(v1StatefulSet));
                        }
                    }
                    this.setHpaYamlLog(yamlStr, cluster, serverParam, hpaName, statefulSetType); // HPA 로그 추가
//                    componentDao.updateComponentManifestByNamespace(serverParam.getComponent().getClusterSeq(), serverParam.getComponent().getNamespaceName(), serverParam.getComponent().getComponentName(), yamlStr.toString(), null);

                    // Creates a server
                    creationStatus.add(KubeResourceCreationStatus.StatefulSetCreated);
                    log.info(String.format("[STEP3][1] Before Creating Workload [%s] (addServerProcess) : %s - %s - %s", serverParam.getServer().getWorkloadType(), serverParam.getComponent().getClusterSeq(), serverParam.getComponent().getNamespaceName(), serverParam.getComponent().getComponentName()));
                    if (hasHPA) {
                        serverDetail.getServer().setComputeTotal(null);
                    }
                    K8sStatefulSetVO k8sStatefulSet = workloadResourceService.createStatefulSet(cluster, serverDetail, context);
                    if (k8sStatefulSet == null) {
                        log.info(String.format("[STEP3][1] Workload Creation Failed [%s] (addServerProcess) : %s - %s - %s\n%s\n-----\n%s", serverParam.getServer().getWorkloadType(), serverParam.getComponent().getClusterSeq(), serverParam.getComponent().getNamespaceName(), serverParam.getComponent().getComponentName(), JsonUtils.toGson(serverDetail), yamlStr));
                        throw new CocktailException("Can't create StatefulSet", ExceptionType.K8sStatefulSetCreationFail);
                    }

                    log.info(String.format("[STEP3][2] Before Creating Autoscaler [%s] (addServerProcess) : %s - %s - %s", serverParam.getServer().getWorkloadType(), serverParam.getComponent().getClusterSeq(), serverParam.getComponent().getNamespaceName(), serverParam.getComponent().getComponentName()));
                    if (serverParam.getServer() != null
                        && serverParam.getServer().getHpa() != null
                        && CollectionUtils.isNotEmpty(serverParam.getServer().getHpa().getMetrics())) {
                        // Create HorizontalPodAutoscaler
                        creationStatus.add(KubeResourceCreationStatus.HorizontalPodAutoscalerCreated);

                        // Cocktail v2.5.4 : K8s 1.9 + Metric Server 기능 지원 개발 진행 중 HPA 동작 처리를 기존 Fabric8 -> K8s 공식 클라이언트로 변경 처리함
                        boolean bIsSuccess = this.workloadResourceService.createHorizontalPodAutoscaler(cluster, cluster.getNamespaceName(), serverParam.getComponent(), serverParam.getServer().getHpa(), hpaName, statefulSetType);
                        if (!bIsSuccess) {
                            log.info(String.format("[STEP3][2] Autoscaler Creation Failed [%s] (addServerProcess) : %s - %s - %s\n%s\n-----\n%s", serverParam.getServer().getWorkloadType(), serverParam.getComponent().getClusterSeq(), serverParam.getComponent().getNamespaceName(), serverParam.getComponent().getComponentName(), JsonUtils.toGson(serverParam.getServer().getHpa()), yamlStr));
                            throw new CocktailException("Can't create HorizontalPodAutoscaler", ExceptionType.K8sHorizontalPodAutoscalerCreationFail);
                        }
                    }

                    log.info(String.format("CocktailServer created - name: %s", serverParam.getComponent().getComponentName()));

//                    componentService.updateComponentState(context, serverParam.getComponent(), StateCode.RUNNING, componentDao);

                } catch (Exception e) {
                    log.error("K8S cocktail server creation fail: " + e.getMessage());
                    log.error(ExceptionUtils.getStackTrace(e));
                    if (creationStatus.contains(KubeResourceCreationStatus.StatefulSetCreated)) {
                        workloadResourceService.deleteStatefulSet(cluster, serverDetail, context);
                    }
                    if (creationStatus.contains(KubeResourceCreationStatus.HorizontalPodAutoscalerCreated)) {
                        // Cocktail v2.5.4 : K8s 1.9 + Metric Server 기능 지원 개발 진행 중 HPA 동작 처리를 기존 Fabric8 -> K8s 공식 클라이언트로 변경 처리함
                        if(serverParam != null && serverParam.getServer() != null && serverParam.getServer().getHpa() != null) {
                            this.workloadResourceService.deleteHorizontalPodAutoscaler(cluster, cluster.getNamespaceName(), hpaName, serverParam.getServer().getHpa());
                        } else {
                            this.workloadResourceService.deleteHorizontalPodAutoscaler(cluster, cluster.getNamespaceName(), hpaName, null);
                        }
                    }

//                    String errMsg = this.getApiErrorMessage(e);
//                    componentService.updateComponentState(context, serverParam.getComponent(), StateCode.ERROR, errMsg, componentDao);
//                    componentDao.updateComponentManifestByNamespace(serverParam.getComponent().getClusterSeq(), serverParam.getComponent().getNamespaceName(), serverParam.getComponent().getComponentName(), yamlStr.toString(), null);

                    throw e;
                }
            }

            /**
             * AccountType in (Cocktail, Apps) 일 경우에만 pipeline를 생성하도록 함
             */
            if (context != null && context.getUserAccount() != null && !context.getUserAccount().getAccountType().isCubeEngine()) {
                /**
                 * pipeline 생성
                 */
                try {
                    log.info(String.format("[STEP4] Start Pipeline Deployment [%s] (addServerProcess) : %s - %s - %s", serverParam.getServer().getWorkloadType(), serverParam.getComponent().getClusterSeq(), serverParam.getComponent().getNamespaceName(), serverParam.getComponent().getComponentName()));
                    pipelineFlowService.mergePipeline(serverParam, context.getUserServiceSeq(), context);
                } catch (Exception e) {
                    log.info(String.format("[STEP4] Pipeline Deployment Failed [%s] (addServerProcess) : %s - %s - %s\n%s", serverParam.getServer().getWorkloadType(), serverParam.getComponent().getClusterSeq(), serverParam.getComponent().getNamespaceName(), serverParam.getComponent().getComponentName(), JsonUtils.toGson(serverParam)));
//                componentService.updateComponentState(context, serverParam.getComponent(), StateCode.ERROR, e.getMessage(), componentDao);
                    throw new CocktailException("fail add pipeline!!", e, ExceptionType.InternalError);
                }
            }

        } catch (Exception e) {
//            componentDao.updateComponentManifestByNamespace(cluster.getClusterSeq(), cluster.getNamespaceName(), uniqueName, yamlStr.toString(), context.getUserSeq());
            throw e;
        }

        return serverParam;
    }


    private void setHpaYamlLog(StringBuffer yamlStr, ClusterVO cluster, ServerGuiVO serverParam, String hpaName, K8sApiVerKindType k8sApiVerKindType) throws Exception {
        if (serverParam.getServer() != null
            && serverParam.getServer().getHpa() != null
            && CollectionUtils.isNotEmpty(serverParam.getServer().getHpa().getMetrics())) {
            K8sApiVerKindType hpaType = ResourceUtil.getApiVerKindType(cluster, K8sApiKindType.HORIZONTAL_POD_AUTOSCALER);

            if (hpaType != null) {
                if (hpaType.getApiType() == K8sApiType.V2) {
                    V2HorizontalPodAutoscaler v2HorizontalPodAutoscaler = K8sSpecFactory.buildHpaV2(serverParam.getServer().getHpa(), cluster.getNamespaceName(), serverParam.getComponent().getComponentName(), hpaName, k8sApiVerKindType);
                    yamlStr.append("---\n");
                    yamlStr.append(ServerUtils.marshalYaml(v2HorizontalPodAutoscaler));
                } else if (hpaType.getApiType() == K8sApiType.V2BETA2) {
                    V2beta2HorizontalPodAutoscaler v2beta2HorizontalPodAutoscaler = K8sSpecFactory.buildHpaV2beta2(serverParam.getServer().getHpa(), cluster.getNamespaceName(), serverParam.getComponent().getComponentName(), hpaName, k8sApiVerKindType);
                    yamlStr.append("---\n");
                    yamlStr.append(ServerUtils.marshalYaml(v2beta2HorizontalPodAutoscaler));
                } else if (hpaType.getApiType() == K8sApiType.V2BETA1) {
                    V2beta1HorizontalPodAutoscaler v2beta1HorizontalPodAutoscaler = K8sSpecFactory.buildHpaV2beta1(serverParam.getServer().getHpa(), cluster.getNamespaceName(), serverParam.getComponent().getComponentName(), hpaName, k8sApiVerKindType);
                    yamlStr.append("---\n");
                    yamlStr.append(ServerUtils.marshalYaml(v2beta1HorizontalPodAutoscaler));
                }
            }
        }
    }

    /**
     * 서비 수정
     *
     * @param serverYaml
     * @param context
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public ServerYamlVO udpateServerProcess(ServerYamlVO serverYaml, ExecutingContextVO context) throws Exception {
        log.info(String.format("[STEP2] Start Update Server (udpateServerProcess) : %s - %s - %s", serverYaml.getClusterSeq(), serverYaml.getNamespaceName(), context.getParams().get(CommonConstants.UPDATE_TARGET_WORKLOAD)));
        JSON k8sJson = new JSON();

        IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
        IServiceMapper serviceDao = sqlSession.getMapper(IServiceMapper.class);
        IComponentMapper componentDao = this.sqlSession.getMapper(IComponentMapper.class);

        ClusterVO cluster = clusterDao.getCluster(serverYaml.getClusterSeq());
        cluster.setNamespaceName(serverYaml.getNamespaceName());
        WorkloadType workloadType = WorkloadType.valueOf(serverYaml.getWorkloadType());

        ServerDetailVO serverDetail = null;
        ServerGuiVO serverGui = null;


        Set<ServerProcessService.KubeResourceCreationStatus> creationStatus = new HashSet<>();

        try {
            log.info(String.format("[STEP3] Start Workload Patch [%s] (udpateServerProcess) : %s - %s - %s", serverYaml.getWorkloadType(), serverYaml.getClusterSeq(), serverYaml.getNamespaceName(), context.getParams().get(CommonConstants.UPDATE_TARGET_WORKLOAD)));
            V1Deployment deployment = null;
            V2HorizontalPodAutoscaler hpaV2 = null;
            V2beta2HorizontalPodAutoscaler hpaV2beta2 = null;
            V2beta1HorizontalPodAutoscaler hpaV2beta1 = null;
            V1HorizontalPodAutoscaler hpaV1 = null;
            V1DaemonSet daemonSet = null;
            V1Job job = null;
            V1beta1CronJob cronJobV1beta1 = null;
            V1CronJob cronJobV1 = null;
            V1StatefulSet statefulSet = null;

            List<Object> objs = ServerUtils.getYamlObjects(serverYaml.getYaml());

            if (CollectionUtils.isNotEmpty(objs)) {
                for (Object obj : objs) {
                    Map<String, Object> k8sObjectToMap = ServerUtils.getK8sObjectToMap(obj, k8sJson);
                    K8sApiKindType kind = ServerUtils.getK8sKindInMap(k8sObjectToMap);
                    K8sApiGroupType apiGroupType = ServerUtils.getK8sGroupInMap(k8sObjectToMap);
                    K8sApiType apiType = ServerUtils.getK8sVersionInMap(k8sObjectToMap);

                    switch (kind) {
                        case DEPLOYMENT:
                            if (apiGroupType == K8sApiGroupType.APPS && apiType == K8sApiType.V1) {
                                if (StringUtils.equals(((V1Deployment) obj).getMetadata().getName(), serverYaml.getWorkloadName())) {
                                    deployment = (V1Deployment) obj;
                                }
                            }
                            break;
                        case HORIZONTAL_POD_AUTOSCALER:
                            if (K8sApiType.V2 == apiType) {
                                if ( StringUtils.equals(((V2HorizontalPodAutoscaler) obj).getSpec().getScaleTargetRef().getName(), serverYaml.getWorkloadName()) ) {
                                    hpaV2 = (V2HorizontalPodAutoscaler) obj;
                                }
                            } else if (K8sApiType.V2BETA2 == apiType) {
                                if ( StringUtils.equals(((V2beta2HorizontalPodAutoscaler) obj).getSpec().getScaleTargetRef().getName(), serverYaml.getWorkloadName()) ) {
                                    hpaV2beta2 = (V2beta2HorizontalPodAutoscaler) obj;
                                }
                            } else if (K8sApiType.V2BETA1 == apiType) {
                                if ( StringUtils.equals(((V2beta1HorizontalPodAutoscaler) obj).getSpec().getScaleTargetRef().getName(), serverYaml.getWorkloadName()) ) {
                                    hpaV2beta1 = (V2beta1HorizontalPodAutoscaler) obj;
                                }
                            } else {
                                if ( StringUtils.equals(((V1HorizontalPodAutoscaler) obj).getSpec().getScaleTargetRef().getName(), serverYaml.getWorkloadName()) ) {
                                    hpaV1 = (V1HorizontalPodAutoscaler) obj;
                                }
                            }
                            break;
                        case DAEMON_SET:
                            if (apiGroupType == K8sApiGroupType.APPS && apiType == K8sApiType.V1) {
                                if (StringUtils.equals(((V1DaemonSet) obj).getMetadata().getName(), serverYaml.getWorkloadName())) {
                                    daemonSet = (V1DaemonSet) obj;
                                }
                            }
                            break;
                        case JOB:
                            if (apiGroupType == K8sApiGroupType.BATCH && apiType == K8sApiType.V1) {
                                if (StringUtils.equals(((V1Job) obj).getMetadata().getName(), serverYaml.getWorkloadName())) {
                                    job = (V1Job) obj;
                                }
                            }
                            break;
                        case CRON_JOB:
                            if (apiGroupType == K8sApiGroupType.BATCH && apiType == K8sApiType.V1BETA1) {
                                if ( StringUtils.equals(((V1beta1CronJob) obj).getMetadata().getName(), serverYaml.getWorkloadName()) ) {
                                    cronJobV1beta1 = (V1beta1CronJob) obj;
                                }
                            } else if (apiGroupType == K8sApiGroupType.BATCH && apiType == K8sApiType.V1) {
                                if ( StringUtils.equals(((V1CronJob) obj).getMetadata().getName(), serverYaml.getWorkloadName()) ) {
                                    cronJobV1 = (V1CronJob) obj;
                                }
                            }
                            break;
                        case STATEFUL_SET:
                            if (apiGroupType == K8sApiGroupType.APPS && apiType == K8sApiType.V1) {
                                if (StringUtils.equals(((V1StatefulSet) obj).getMetadata().getName(), serverYaml.getWorkloadName())) {
                                    statefulSet = (V1StatefulSet) obj;
                                }
                            }
                            break;
                    }
                }
            }

            String dateStr = Utils.getNowDateTime();
            boolean hasHPA = hpaV2 != null || hpaV2beta2 != null ||hpaV2beta1 != null ||hpaV1 != null;

            /**
             * Deployment
             */
            if(workloadType == WorkloadType.SINGLE_SERVER
                    || workloadType == WorkloadType.REPLICA_SERVER) {

                if (deployment != null) {
                    // 2022.09.22 hjchoi - 아래 이슈로 HPA가 존재한다면 null 셋팅
                    // HPA가 활성화되어 있으면, 디플로이먼트, 스테이트풀셋 모두 또는 둘 중 하나의 매니페스트에서 spec.replicas의 값을 삭제하는 것이 바람직하다
                    // https://kubernetes.io/ko/docs/tasks/run-application/horizontal-pod-autoscale/#%EB%94%94%ED%94%8C%EB%A1%9C%EC%9D%B4%EB%A8%BC%ED%8A%B8%EC%99%80-%EC%8A%A4%ED%85%8C%EC%9D%B4%ED%8A%B8%ED%92%80%EC%85%8B%EC%9D%84-horizontal-autoscaling%EC%9C%BC%EB%A1%9C-%EC%A0%84%ED%99%98%ED%95%98%EA%B8%B0
                    if(workloadType.isPossibleAutoscaling()) {
                        if (hasHPA) {
                            deployment.getSpec().setReplicas(null);
                        }
                    }

                    if (!hasHPA) {
                        // deployment ScaleInOut
                        workloadResourceService.patchDeploymentScale(cluster, cluster.getNamespaceName(), deployment.getMetadata().getName(), deployment.getSpec().getReplicas(), false, context);
                    }

                    // patch deployment
                    if (cluster.getAccount() != null && StringUtils.isNotBlank(cluster.getAccount().getRegistryDownloadUserId())) {
                        this.setImagePullSecret(deployment.getSpec().getTemplate(), CryptoUtils.decryptAES(cluster.getAccount().getRegistryDownloadUserId()));
                    }
                    deployment.getMetadata().putAnnotationsItem(KubeConstants.ANNOTATION_COCKTAIL_DEPLOY_DATETIME, dateStr);
                    workloadResourceService.patchDeploymentV1(cluster, cluster.getNamespaceName(), deployment, hasHPA, context);

                }
            }
            /**
             * DaemonSet
             */
            else if(workloadType == WorkloadType.DAEMON_SET_SERVER) {

                if (daemonSet != null) {

                    // patch daemonSet
                    if (cluster.getAccount() != null && StringUtils.isNotBlank(cluster.getAccount().getRegistryDownloadUserId())) {
                        this.setImagePullSecret(daemonSet.getSpec().getTemplate(), CryptoUtils.decryptAES(cluster.getAccount().getRegistryDownloadUserId()));
                    }
                    daemonSet.getMetadata().putAnnotationsItem(KubeConstants.ANNOTATION_COCKTAIL_DEPLOY_DATETIME, dateStr);
                    workloadResourceService.patchDaemonSetV1(cluster, cluster.getNamespaceName(), daemonSet, context);
                }

            }
            /**
             * Job
             */
            else if(workloadType == WorkloadType.JOB_SERVER) {

                if (job != null) {

//                    // delete job
//                    k8sResourceService.deleteJobV1(cluster, cluster.getNamespaceName(), job.getMetadata().getName(), context);
//                    Thread.sleep(1000);
//
//                    // create job
//                    if (registryUser != null && StringUtils.isNotBlank(registryUser.getRegistryUserId())) {
//                        this.setImagePullSecret(job.getSpec().getTemplate(), CryptoUtils.decryptAES(registryUser.getRegistryUserId()));
//                    }
//                    k8sWorker.createJobV1(cluster, cluster.getNamespaceName(), job);

                    // patch job
                    if (cluster.getAccount() != null && StringUtils.isNotBlank(cluster.getAccount().getRegistryDownloadUserId())) {
                        this.setImagePullSecret(job.getSpec().getTemplate(), CryptoUtils.decryptAES(cluster.getAccount().getRegistryDownloadUserId()));
                    }
                    job.getMetadata().putAnnotationsItem(KubeConstants.ANNOTATION_COCKTAIL_DEPLOY_DATETIME, dateStr);
                    workloadResourceService.patchJobV1(cluster, cluster.getNamespaceName(), job, context);
                }

            }
            /**
             * CronJob
             */
            else if(workloadType == WorkloadType.CRON_JOB_SERVER) {

                if (cronJobV1beta1 != null) {
                    // patch cronJob
                    if (cluster.getAccount() != null && StringUtils.isNotBlank(cluster.getAccount().getRegistryDownloadUserId())) {
                        this.setImagePullSecret(cronJobV1beta1.getSpec().getJobTemplate().getSpec().getTemplate(), CryptoUtils.decryptAES(cluster.getAccount().getRegistryDownloadUserId()));
                    }
                    cronJobV1beta1.getMetadata().putAnnotationsItem(KubeConstants.ANNOTATION_COCKTAIL_DEPLOY_DATETIME, dateStr);
                    workloadResourceService.patchCronJobV1beta1(cluster, cluster.getNamespaceName(), cronJobV1beta1, context);
                } else if (cronJobV1 != null) {
                    // patch cronJob
                    if (cluster.getAccount() != null && StringUtils.isNotBlank(cluster.getAccount().getRegistryDownloadUserId())) {
                        this.setImagePullSecret(cronJobV1.getSpec().getJobTemplate().getSpec().getTemplate(), CryptoUtils.decryptAES(cluster.getAccount().getRegistryDownloadUserId()));
                    }
                    cronJobV1.getMetadata().putAnnotationsItem(KubeConstants.ANNOTATION_COCKTAIL_DEPLOY_DATETIME, dateStr);
                    workloadResourceService.patchCronJobV1(cluster, cluster.getNamespaceName(), cronJobV1, context);
                }

            }
            /**
             * StatefulSet
             */
            else if(workloadType == WorkloadType.STATEFUL_SET_SERVER) {

                if (statefulSet != null) {
                    // 2022.09.22 hjchoi - 아래 이슈로 HPA가 존재한다면 null 셋팅
                    // HPA가 활성화되어 있으면, 디플로이먼트, 스테이트풀셋 모두 또는 둘 중 하나의 매니페스트에서 spec.replicas의 값을 삭제하는 것이 바람직하다
                    // https://kubernetes.io/ko/docs/tasks/run-application/horizontal-pod-autoscale/#%EB%94%94%ED%94%8C%EB%A1%9C%EC%9D%B4%EB%A8%BC%ED%8A%B8%EC%99%80-%EC%8A%A4%ED%85%8C%EC%9D%B4%ED%8A%B8%ED%92%80%EC%85%8B%EC%9D%84-horizontal-autoscaling%EC%9C%BC%EB%A1%9C-%EC%A0%84%ED%99%98%ED%95%98%EA%B8%B0
                    if(workloadType.isPossibleAutoscaling()) {
                        if (hasHPA) {
                            statefulSet.getSpec().setReplicas(null);
                        }
                    }

                    if (!hasHPA) {
                        // statefulSet ScaleInOut
                        workloadResourceService.patchStatefulSetScale(cluster, cluster.getNamespaceName(), statefulSet.getMetadata().getName(), statefulSet.getSpec().getReplicas(), false, context);
                    }

                    // patch statefulSet
                    if (cluster.getAccount() != null && StringUtils.isNotBlank(cluster.getAccount().getRegistryDownloadUserId())) {
                        this.setImagePullSecret(statefulSet.getSpec().getTemplate(), CryptoUtils.decryptAES(cluster.getAccount().getRegistryDownloadUserId()));
                    }
                    statefulSet.getMetadata().putAnnotationsItem(KubeConstants.ANNOTATION_COCKTAIL_DEPLOY_DATETIME, dateStr);
                    workloadResourceService.patchStatefulSetV1(cluster, cluster.getNamespaceName(), statefulSet, hasHPA, context);
//                    k8sResourceService.replaceStatefulSetV1(cluster, cluster.getNamespaceName(), statefulSet, context);

                }
            }

            /** Deployment외에 추가로 StatefulSet에서 AutoScaler 를 적용하면서 코드 이동 (AS-IS : 위 if(deploymentType일 경우에 처리...) **/
            if(workloadType.isPossibleAutoscaling()) {
                if (deployment != null || statefulSet != null) {
                    // hpa
                    if (hpaV2 != null) {
                        List<K8sHorizontalPodAutoscalerVO> currentHpas = workloadResourceService.convertHorizontalPodAutoscalerDataList(cluster, cluster.getNamespaceName(), String.format("%s.%s=%s", KubeConstants.META, KubeConstants.NAME, hpaV2.getMetadata().getName()), null);
                        if (CollectionUtils.isNotEmpty(currentHpas)) {
                            workloadResourceService.patchHorizontalPodAutoscalerV2(cluster, cluster.getNamespaceName(), hpaV2, context);
                        } else {
                            k8sWorker.createHorizontalPodAutoscalerV2(cluster, cluster.getNamespaceName(), hpaV2, false);
                        }
                    } else if (hpaV2beta2 != null) {
                        List<K8sHorizontalPodAutoscalerVO> currentHpas = workloadResourceService.convertHorizontalPodAutoscalerDataList(cluster, cluster.getNamespaceName(), String.format("%s.%s=%s", KubeConstants.META, KubeConstants.NAME, hpaV2beta2.getMetadata().getName()), null);
                        if (CollectionUtils.isNotEmpty(currentHpas)) {
                            workloadResourceService.patchHorizontalPodAutoscalerV2beta2(cluster, cluster.getNamespaceName(), hpaV2beta2, context);
                        } else {
                            k8sWorker.createHorizontalPodAutoscalerV2beta2(cluster, cluster.getNamespaceName(), hpaV2beta2, false);
                        }
                    } else if (hpaV2beta1 != null) {
                        List<K8sHorizontalPodAutoscalerVO> currentHpas = workloadResourceService.convertHorizontalPodAutoscalerDataList(cluster, cluster.getNamespaceName(), String.format("%s.%s=%s", KubeConstants.META, KubeConstants.NAME, hpaV2beta1.getMetadata().getName()), null);
                        if (CollectionUtils.isNotEmpty(currentHpas)) {
                            workloadResourceService.patchHorizontalPodAutoscalerV2beta1(cluster, cluster.getNamespaceName(), hpaV2beta1, context);
                        } else {
                            k8sWorker.createHorizontalPodAutoscalerV2beta1(cluster, cluster.getNamespaceName(), hpaV2beta1, false);
                        }
                    } else if (hpaV1 != null) {
                        List<K8sHorizontalPodAutoscalerVO> currentHpas = workloadResourceService.convertHorizontalPodAutoscalerDataList(cluster, cluster.getNamespaceName(), String.format("%s.%s=%s", KubeConstants.META, KubeConstants.NAME, hpaV1.getMetadata().getName()), null);
                        if (CollectionUtils.isNotEmpty(currentHpas)) {
                            workloadResourceService.patchHorizontalPodAutoscalerV1(cluster, cluster.getNamespaceName(), hpaV1, context);
                        } else {
                            k8sWorker.createHorizontalPodAutoscalerV1(cluster, cluster.getNamespaceName(), hpaV1, false);
                        }
                    } else {
                        K8sApiVerKindType hpaType = ResourceUtil.getApiVerKindType(cluster, K8sApiKindType.HORIZONTAL_POD_AUTOSCALER);
                        List<K8sHorizontalPodAutoscalerVO> currentHpas = Lists.newArrayList();
                        if (EnumSet.of(K8sApiType.V2, K8sApiType.V2BETA2, K8sApiType.V2BETA1, K8sApiType.V1).contains(hpaType.getApiType())) {
                            currentHpas = workloadResourceService.convertHorizontalPodAutoscalerDataList(cluster, cluster.getNamespaceName(), null, null);
                        }

                        if (CollectionUtils.isNotEmpty(currentHpas)) {
                            String kind = deployment != null ? deployment.getKind() : statefulSet.getKind();
                            String name = deployment != null ? deployment.getMetadata().getName() : statefulSet.getMetadata().getName();
                            for (K8sHorizontalPodAutoscalerVO hpa : currentHpas) {
                                if (hpa.getScaleTargetRef() != null
                                        && StringUtils.equals(hpa.getScaleTargetRef().getKind(), kind)
                                        && StringUtils.equals(hpa.getScaleTargetRef().getName(), name)
                                ) {
                                    workloadResourceService.deleteHorizontalPodAutoscaler(cluster, cluster.getNamespaceName(), hpa.getName(), null);
                                    break;
                                }
                            }
                        }
                    }
                }

            }


//            componentDao.updateComponentManifestByNamespace(serverYaml.getClusterSeq(), serverYaml.getNamespaceName(), serverYaml.getWorkloadName(), serverYaml.getYaml(), null);
//            componentService.updateComponentState(context, component, StateCode.RUNNING, componentDao);

            /**
             * AccountType in (Cocktail, Apps) 일 경우에만 pipeline를 생성하도록 함
             */
            if (context != null && context.getUserAccount() != null && !context.getUserAccount().getAccountType().isCubeEngine()) {
                /**
                 * pipeline 수정
                 */
                try {
                    log.info(String.format("[STEP4] Start Pipeline Patch [%s] (udpateServerProcess) : %s - %s - %s", serverYaml.getWorkloadType(), serverYaml.getClusterSeq(), serverYaml.getNamespaceName(), context.getParams().get(CommonConstants.UPDATE_TARGET_WORKLOAD)));
                    if (MapUtils.getObject(context.getParams(), "serverGui", null) != null) {
                        serverGui = (ServerGuiVO)context.getParams().get("serverGui");
                    } else {
                        serverGui = serverConversionService.convertYamlToGui(cluster, serverYaml.getNamespaceName(), serverYaml.getWorkloadType(), serverYaml.getWorkloadVersion(), serverYaml.getDescription(), null, serverYaml.getYaml());
                    }
//                serverGui.getComponent().setComponentSeq(component.getComponentSeq());
                    serverGui.getComponent().setClusterSeq(cluster.getClusterSeq());
                    serverGui.getComponent().setNamespaceName(cluster.getNamespaceName());
                    pipelineFlowService.mergePipeline(serverGui, context.getUserServiceSeq(), context);
                } catch (Exception e) {
                    log.info(String.format("[STEP4] Pipeline Patch Failed [%s] (udpateServerProcess) : %s - %s - %s\n%s", serverYaml.getWorkloadType(), serverYaml.getClusterSeq(), serverYaml.getNamespaceName(), context.getParams().get(CommonConstants.UPDATE_TARGET_WORKLOAD), JsonUtils.toGson(serverGui)));
                    throw new CocktailException("fail merge pipeline!!", e, ExceptionType.InternalError);
                }
            }

        } catch (Exception e) {
            log.info(String.format("[STEP5] Workload Patch Failed [%s] (udpateServerProcess) : %s - %s - %s\n%s\n-----\n%s", serverYaml.getWorkloadType(), serverYaml.getClusterSeq(), serverYaml.getNamespaceName(), context.getParams().get(CommonConstants.UPDATE_TARGET_WORKLOAD), JsonUtils.toGson(serverDetail), serverYaml.getYaml()));
            log.error("Fail to rolling update cocktail '{}' server: {}", workloadType.getCode(), e.getMessage());
            log.error(ExceptionUtils.getStackTrace(e));

//            String errMsg = this.getApiErrorMessage(e);
//            componentService.updateComponentState(context, component, StateCode.ERROR, errMsg, componentDao);
//            componentDao.updateComponentManifestByNamespace(serverYaml.getClusterSeq(), serverYaml.getNamespaceName(), serverYaml.getWorkloadName(), serverYaml.getYaml(), null);

            throw e;
        }

        return serverYaml;
    }

    /**
     * 서비 수정 (replace)
     *
     * @param serverYaml
     * @param context
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public ServerYamlVO replaceServerProcessForPipeline(ServerYamlVO serverYaml, ExecutingContextVO context) throws Exception {
        log.info(String.format("[STEP2] Start Update Server for Pipeline (replaceServerProcessForPipeline) : %s - %s - %s", serverYaml.getClusterSeq(), serverYaml.getNamespaceName(), context.getParams().get(CommonConstants.UPDATE_TARGET_WORKLOAD)));

        JSON k8sJson = new JSON();

        IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
        IServiceMapper serviceDao = sqlSession.getMapper(IServiceMapper.class);
        IComponentMapper componentDao = this.sqlSession.getMapper(IComponentMapper.class);

        ClusterVO cluster = clusterDao.getCluster(serverYaml.getClusterSeq());
        cluster.setNamespaceName(serverYaml.getNamespaceName());
        WorkloadType workloadType = WorkloadType.valueOf(serverYaml.getWorkloadType());

        ServerDetailVO serverDetail = null;
        ServerGuiVO serverGui = null;


        Set<ServerProcessService.KubeResourceCreationStatus> creationStatus = new HashSet<>();

        try {
            log.info(String.format("[STEP3] Start Workload Patch [%s] (replaceServerProcessForPipeline) : %s - %s - %s", serverYaml.getWorkloadType(), serverYaml.getClusterSeq(), serverYaml.getNamespaceName(), context.getParams().get(CommonConstants.UPDATE_TARGET_WORKLOAD)));
            V1Deployment deployment = null;
//            V2beta2HorizontalPodAutoscaler hpa = null;
            V1DaemonSet daemonSet = null;
            V1Job job = null;
            V1beta1CronJob cronJobV1beta1 = null;
            V1CronJob cronJobV1 = null;
            V1StatefulSet statefulSet = null;

            List<Object> objs = ServerUtils.getYamlObjects(serverYaml.getYaml());

            if (CollectionUtils.isNotEmpty(objs)) {
                for (Object obj : objs) {
                    Map<String, Object> k8sObjectToMap = ServerUtils.getK8sObjectToMap(obj, k8sJson);
                    K8sApiKindType kind = ServerUtils.getK8sKindInMap(k8sObjectToMap);
                    K8sApiGroupType apiGroupType = ServerUtils.getK8sGroupInMap(k8sObjectToMap);
                    K8sApiType apiType = ServerUtils.getK8sVersionInMap(k8sObjectToMap);

                    switch (kind) {
                        case DEPLOYMENT:
                            if (apiGroupType == K8sApiGroupType.APPS && apiType == K8sApiType.V1) {
                                if (StringUtils.equals(((V1Deployment) obj).getMetadata().getName(), serverYaml.getWorkloadName())) {
                                    deployment = (V1Deployment) obj;
                                }
                            }
                            break;
//                        case HORIZONTAL_POD_AUTOSCALER:
//                            if ( StringUtils.equals(((V2beta2HorizontalPodAutoscaler) obj).getMetadata().getName(), serverYaml.getWorkloadName()) ) {
//                                hpa = (V2beta2HorizontalPodAutoscaler) obj;
//                            }
//                            break;
                        case DAEMON_SET:
                            if (apiGroupType == K8sApiGroupType.APPS && apiType == K8sApiType.V1) {
                                if (StringUtils.equals(((V1DaemonSet) obj).getMetadata().getName(), serverYaml.getWorkloadName())) {
                                    daemonSet = (V1DaemonSet) obj;
                                }
                            }
                            break;
                        case JOB:
                            if (apiGroupType == K8sApiGroupType.BATCH && apiType == K8sApiType.V1) {
                                if (StringUtils.equals(((V1Job) obj).getMetadata().getName(), serverYaml.getWorkloadName())) {
                                    job = (V1Job) obj;
                                }
                            }
                            break;
                        case CRON_JOB:
                            if (apiGroupType == K8sApiGroupType.BATCH && apiType == K8sApiType.V1BETA1) {
                                if ( StringUtils.equals(((V1beta1CronJob) obj).getMetadata().getName(), serverYaml.getWorkloadName()) ) {
                                    cronJobV1beta1 = (V1beta1CronJob) obj;
                                }
                            } else if (apiGroupType == K8sApiGroupType.BATCH && apiType == K8sApiType.V1) {
                                if ( StringUtils.equals(((V1CronJob) obj).getMetadata().getName(), serverYaml.getWorkloadName()) ) {
                                    cronJobV1 = (V1CronJob) obj;
                                }
                            }
                            break;
                        case STATEFUL_SET:
                            if (apiGroupType == K8sApiGroupType.APPS && apiType == K8sApiType.V1) {
                                if (StringUtils.equals(((V1StatefulSet) obj).getMetadata().getName(), serverYaml.getWorkloadName())) {
                                    statefulSet = (V1StatefulSet) obj;
                                }
                            }
                            break;
                    }
                }
            }

            String dateStr = Utils.getNowDateTime();
            String datetimeStr = Utils.getNowDateTime(Utils.DEFAULT_DATE_TIME_ZONE_FORMAT);

            /**
             * Deployment
             */
            if(workloadType == WorkloadType.SINGLE_SERVER
                    || workloadType == WorkloadType.REPLICA_SERVER) {

                if (deployment != null) {
                    // replace deployment
                    if (cluster.getAccount() != null && StringUtils.isNotBlank(cluster.getAccount().getRegistryDownloadUserId())) {
                        this.setImagePullSecret(deployment.getSpec().getTemplate(), CryptoUtils.decryptAES(cluster.getAccount().getRegistryDownloadUserId()));
                    }
                    deployment.getMetadata().putAnnotationsItem(KubeConstants.ANNOTATION_COCKTAIL_DEPLOY_DATETIME, dateStr);
                    // 기존 restart를 위한 annotation 키 대신 kubectl.kubernetes.io/restartedAt 로 변경, 기존 키는 삭제 (official로 맞추기 위함)
                    deployment.getSpec().getTemplate().getMetadata().putAnnotationsItem(KubeConstants.META_ANNOTATIONS_RESTARTED_AT, datetimeStr);
                    deployment.getSpec().getTemplate().getMetadata().getAnnotations().remove(KubeConstants.ANNOTATION_COCKTAIL_DEPLOY_DATETIME);

                    // set ImagePullPolicy = Always
                    this.setImagePullPolicy(deployment.getSpec().getTemplate().getSpec().getContainers());

                    workloadResourceService.replacehDeploymentV1(cluster, cluster.getNamespaceName(), deployment, context);
                }

            }
            /**
             * DaemonSet
             */
            else if(workloadType == WorkloadType.DAEMON_SET_SERVER) {

                if (daemonSet != null) {

                    // replace daemonSet
                    if (cluster.getAccount() != null && StringUtils.isNotBlank(cluster.getAccount().getRegistryDownloadUserId())) {
                        this.setImagePullSecret(daemonSet.getSpec().getTemplate(), CryptoUtils.decryptAES(cluster.getAccount().getRegistryDownloadUserId()));
                    }
                    daemonSet.getMetadata().putAnnotationsItem(KubeConstants.ANNOTATION_COCKTAIL_DEPLOY_DATETIME, dateStr);
                    // 기존 restart를 위한 annotation 키 대신 kubectl.kubernetes.io/restartedAt 로 변경, 기존 키는 삭제 (official로 맞추기 위함)
                    daemonSet.getSpec().getTemplate().getMetadata().putAnnotationsItem(KubeConstants.META_ANNOTATIONS_RESTARTED_AT, datetimeStr);
                    daemonSet.getSpec().getTemplate().getMetadata().getAnnotations().remove(KubeConstants.ANNOTATION_COCKTAIL_DEPLOY_DATETIME);

                    // set ImagePullPolicy = Always
                    this.setImagePullPolicy(daemonSet.getSpec().getTemplate().getSpec().getContainers());

                    workloadResourceService.replaceDaemonSetV1(cluster, cluster.getNamespaceName(), daemonSet, context);
                }

            }
            /**
             * Job
             */
            else if(workloadType == WorkloadType.JOB_SERVER) {

                if (job != null) {

//                    // delete job
//                    k8sResourceService.deleteJobV1(cluster, cluster.getNamespaceName(), job.getMetadata().getName(), context);
//                    Thread.sleep(1000);
//
//                    // create job
//                    if (registryUser != null && StringUtils.isNotBlank(registryUser.getRegistryUserId())) {
//                        this.setImagePullSecret(job.getSpec().getTemplate(), CryptoUtils.decryptAES(registryUser.getRegistryUserId()));
//                    }
//                    job.getSpec().getTemplate().getMetadata().putAnnotationsItem(KubeConstants.ANNOTATION_COCKTAIL_DEPLOY_DATETIME, dateStr);
//                    k8sWorker.createJobV1(cluster, cluster.getNamespaceName(), job);

                    if (cluster.getAccount() != null && StringUtils.isNotBlank(cluster.getAccount().getRegistryDownloadUserId())) {
                        this.setImagePullSecret(job.getSpec().getTemplate(), CryptoUtils.decryptAES(cluster.getAccount().getRegistryDownloadUserId()));
                    }
                    job.getMetadata().putAnnotationsItem(KubeConstants.ANNOTATION_COCKTAIL_DEPLOY_DATETIME, dateStr);
                    // 기존 restart를 위한 annotation 키 대신 kubectl.kubernetes.io/restartedAt 로 변경, 기존 키는 삭제 (official로 맞추기 위함)
                    job.getSpec().getTemplate().getMetadata().putAnnotationsItem(KubeConstants.META_ANNOTATIONS_RESTARTED_AT, datetimeStr);
                    job.getSpec().getTemplate().getMetadata().getAnnotations().remove(KubeConstants.ANNOTATION_COCKTAIL_DEPLOY_DATETIME);

                    // set ImagePullPolicy = Always
                    this.setImagePullPolicy(job.getSpec().getTemplate().getSpec().getContainers());

                    workloadResourceService.replaceJobV1(cluster, cluster.getNamespaceName(), job, context);
                }

            }
            /**
             * CronJob
             */
            else if(workloadType == WorkloadType.CRON_JOB_SERVER) {

                if (cronJobV1beta1 != null) {

                    // replace cronJob
                    if (cluster.getAccount() != null && StringUtils.isNotBlank(cluster.getAccount().getRegistryDownloadUserId())) {
                        this.setImagePullSecret(cronJobV1beta1.getSpec().getJobTemplate().getSpec().getTemplate(), CryptoUtils.decryptAES(cluster.getAccount().getRegistryDownloadUserId()));
                    }
                    cronJobV1beta1.getMetadata().putAnnotationsItem(KubeConstants.ANNOTATION_COCKTAIL_DEPLOY_DATETIME, dateStr);
                    // 기존 restart를 위한 annotation 키 대신 kubectl.kubernetes.io/restartedAt 로 변경, 기존 키는 삭제 (official로 맞추기 위함)
                    cronJobV1beta1.getSpec().getJobTemplate().getSpec().getTemplate().getMetadata().putAnnotationsItem(KubeConstants.META_ANNOTATIONS_RESTARTED_AT, datetimeStr);
                    cronJobV1beta1.getSpec().getJobTemplate().getSpec().getTemplate().getMetadata().getAnnotations().remove(KubeConstants.ANNOTATION_COCKTAIL_DEPLOY_DATETIME);

                    // set ImagePullPolicy = Always
                    this.setImagePullPolicy(cronJobV1beta1.getSpec().getJobTemplate().getSpec().getTemplate().getSpec().getContainers());

                    workloadResourceService.replaceCronJobV1beta1(cluster, cluster.getNamespaceName(), cronJobV1beta1, context);
                } else if (cronJobV1 != null) {

                    // replace cronJob
                    if (cluster.getAccount() != null && StringUtils.isNotBlank(cluster.getAccount().getRegistryDownloadUserId())) {
                        this.setImagePullSecret(cronJobV1.getSpec().getJobTemplate().getSpec().getTemplate(), CryptoUtils.decryptAES(cluster.getAccount().getRegistryDownloadUserId()));
                    }
                    cronJobV1.getMetadata().putAnnotationsItem(KubeConstants.ANNOTATION_COCKTAIL_DEPLOY_DATETIME, dateStr);
                    // 기존 restart를 위한 annotation 키 대신 kubectl.kubernetes.io/restartedAt 로 변경, 기존 키는 삭제 (official로 맞추기 위함)
                    cronJobV1.getSpec().getJobTemplate().getSpec().getTemplate().getMetadata().putAnnotationsItem(KubeConstants.META_ANNOTATIONS_RESTARTED_AT, datetimeStr);
                    cronJobV1.getSpec().getJobTemplate().getSpec().getTemplate().getMetadata().getAnnotations().remove(KubeConstants.ANNOTATION_COCKTAIL_DEPLOY_DATETIME);

                    // set ImagePullPolicy = Always
                    this.setImagePullPolicy(cronJobV1.getSpec().getJobTemplate().getSpec().getTemplate().getSpec().getContainers());

                    workloadResourceService.replaceCronJobV1(cluster, cluster.getNamespaceName(), cronJobV1, context);
                }

            }
            /**
             * StatefulSet
             */
            else if(workloadType == WorkloadType.STATEFUL_SET_SERVER) {

                if (statefulSet != null) {

                    // replace statefulSet
                    if (cluster.getAccount() != null && StringUtils.isNotBlank(cluster.getAccount().getRegistryDownloadUserId())) {
                        this.setImagePullSecret(statefulSet.getSpec().getTemplate(), CryptoUtils.decryptAES(cluster.getAccount().getRegistryDownloadUserId()));
                    }
                    statefulSet.getMetadata().putAnnotationsItem(KubeConstants.ANNOTATION_COCKTAIL_DEPLOY_DATETIME, dateStr);
                    // 기존 restart를 위한 annotation 키 대신 kubectl.kubernetes.io/restartedAt 로 변경, 기존 키는 삭제 (official로 맞추기 위함)
                    statefulSet.getSpec().getTemplate().getMetadata().putAnnotationsItem(KubeConstants.META_ANNOTATIONS_RESTARTED_AT, datetimeStr);
                    statefulSet.getSpec().getTemplate().getMetadata().getAnnotations().remove(KubeConstants.ANNOTATION_COCKTAIL_DEPLOY_DATETIME);

                    // set ImagePullPolicy = Always
                    this.setImagePullPolicy(statefulSet.getSpec().getTemplate().getSpec().getContainers());

                    workloadResourceService.replaceStatefulSetV1(cluster, cluster.getNamespaceName(), statefulSet, context);

                }

            }

            /**
             * AccountType in (Cocktail, Apps) 일 경우에만 pipeline를 생성하도록 함
             */
            if (context != null && context.getUserAccount() != null && !context.getUserAccount().getAccountType().isCubeEngine()) {
                /**
                 * pipeline 수정
                 */
                try {
                    log.info(String.format("[STEP4] Start Pipeline Patch [%s] (replaceServerProcessForPipeline) : %s - %s - %s", serverYaml.getWorkloadType(), serverYaml.getClusterSeq(), serverYaml.getNamespaceName(), context.getParams().get(CommonConstants.UPDATE_TARGET_WORKLOAD)));
                    if (MapUtils.getObject(context.getParams(), "serverGui", null) != null) {
                        serverGui = (ServerGuiVO)context.getParams().get("serverGui");
                    } else {
                        serverGui = serverConversionService.convertYamlToGui(cluster, cluster.getNamespaceName(), serverYaml.getWorkloadType(), serverYaml.getWorkloadVersion(), serverYaml.getDescription(), null, serverYaml.getYaml());
                    }
                    serverGui.getComponent().setClusterSeq(cluster.getClusterSeq());
                    serverGui.getComponent().setNamespaceName(cluster.getNamespaceName());
                    pipelineFlowService.mergePipeline(serverGui, context.getUserServiceSeq(), context);
                } catch (Exception e) {
                    log.info(String.format("[STEP4] Pipeline Patch Failed [%s] (udpateServerProcess) : %s - %s - %s\n%s", serverYaml.getWorkloadType(), serverYaml.getClusterSeq(), serverYaml.getNamespaceName(), context.getParams().get(CommonConstants.UPDATE_TARGET_WORKLOAD), JsonUtils.toGson(serverGui)));
                    throw new CocktailException("fail merge pipeline!!", e, ExceptionType.InternalError);
                }
            }

        } catch (Exception e) {
            log.info(String.format("[STEP5] Workload Patch Failed [%s] (udpateServerProcess) : %s - %s - %s\n%s\n-----\n%s", serverYaml.getWorkloadType(), serverYaml.getClusterSeq(), serverYaml.getNamespaceName(), context.getParams().get(CommonConstants.UPDATE_TARGET_WORKLOAD), JsonUtils.toGson(serverDetail), serverYaml.getYaml()));
            log.error("Fail to rolling update cocktail '{}' server: {}", workloadType.getCode(), e.getMessage());
            log.error(ExceptionUtils.getStackTrace(e));

//            String errMsg = this.getApiErrorMessage(e);
//            componentService.updateComponentState(context, component, StateCode.ERROR, errMsg, componentDao);

            throw e;
        }

//        componentService.updateComponentState(context, component, StateCode.RUNNING, componentDao);
//        componentDao.updateComponentManifestByNamespace(cluster.getClusterSeq(), cluster.getNamespaceName(), serverYaml.getWorkloadName(), null, null);

        return serverYaml;
    }

    /**
     * 서비 재배포
     *
     * @param serverYaml
     * @param context
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public ServerYamlVO redeployServerProcess(ServerYamlVO serverYaml, ExecutingContextVO context) throws Exception {
        log.info(String.format("[STEP2] Start Redeploy Server (redeployServerProcess) : %s - %s - %s", serverYaml.getClusterSeq(), serverYaml.getNamespaceName(), context.getParams().get(CommonConstants.UPDATE_TARGET_WORKLOAD)));

        JSON k8sJson = new JSON();

        IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
        IServiceMapper serviceDao = sqlSession.getMapper(IServiceMapper.class);
        IComponentMapper comDao = this.sqlSession.getMapper(IComponentMapper.class);

        ClusterVO cluster = clusterDao.getCluster(serverYaml.getClusterSeq());
        cluster.setNamespaceName(serverYaml.getNamespaceName());
        WorkloadType workloadType = WorkloadType.valueOf(serverYaml.getWorkloadType());

        ServerGuiVO serverGui = null;

        /**
         * AccountType in (Cocktail, Apps) 일 경우에만 pipeline를 생성하도록 함
         */
        if (context != null && context.getUserAccount() != null && !context.getUserAccount().getAccountType().isCubeEngine()) {
            /**
             * pipeline 생성
             */
            try {
                log.info(String.format("[STEP3] Start Pipeline Redeploy [%s] (redeployServerProcess) : %s - %s - %s", serverYaml.getWorkloadType(), serverYaml.getClusterSeq(), serverYaml.getNamespaceName(), context.getParams().get(CommonConstants.UPDATE_TARGET_WORKLOAD)));
                serverGui = serverConversionService.convertYamlToGui(cluster, cluster.getNamespaceName(), serverYaml.getWorkloadType(), serverYaml.getWorkloadVersion(), serverYaml.getDescription(), null, serverYaml.getYaml());
//            serverGui.getComponent().setComponentSeq(component.getComponentSeq());
                serverGui.getComponent().setClusterSeq(cluster.getClusterSeq());
                serverGui.getComponent().setNamespaceName(cluster.getNamespaceName());
                pipelineFlowService.mergePipeline(serverGui, context.getUserServiceSeq(), context);
            } catch (Exception e) {
//            componentService.updateComponentState(context, component, StateCode.ERROR, e.getMessage(), comDao);
                log.info(String.format("[STEP3] Pipeline Redeploy Failed [%s] (redeployServerProcess) : %s - %s - %s\n%s", serverYaml.getWorkloadType(), serverYaml.getClusterSeq(), serverYaml.getNamespaceName(), context.getParams().get(CommonConstants.UPDATE_TARGET_WORKLOAD), JsonUtils.toGson(serverGui)));
                throw new CocktailException("fail add pipeline!!", e, ExceptionType.InternalError);
            }
        }

        /**
         * Redeploy Workload
         */
        V1Deployment deployment = null;
        V2HorizontalPodAutoscaler hpaV2 = null;
        V2beta2HorizontalPodAutoscaler hpaV2beta2 = null;
        V2beta1HorizontalPodAutoscaler hpaV2beta1 = null;
        V1HorizontalPodAutoscaler hpaV1 = null;
        V1DaemonSet daemonSet = null;
        V1Job job = null;
        V1beta1CronJob cronJobV1beta1 = null;
        V1CronJob cronJobV1 = null;
        V1StatefulSet statefulSet = null;
        try {
            log.info(String.format("[STEP4] Start Workload Redeploy[%s] (redeployServerProcess) : %s - %s - %s", serverYaml.getWorkloadType(), serverYaml.getClusterSeq(), serverYaml.getNamespaceName(), context.getParams().get(CommonConstants.UPDATE_TARGET_WORKLOAD)));

            List<Object> objs = ServerUtils.getYamlObjects(serverYaml.getYaml());

            if (CollectionUtils.isNotEmpty(objs)) {
                for (Object obj : objs) {
                    Map<String, Object> k8sObjectToMap = ServerUtils.getK8sObjectToMap(obj, k8sJson);
                    K8sApiKindType kind = ServerUtils.getK8sKindInMap(k8sObjectToMap);
                    K8sApiGroupType apiGroupType = ServerUtils.getK8sGroupInMap(k8sObjectToMap);
                    K8sApiType apiType = ServerUtils.getK8sVersionInMap(k8sObjectToMap);

                    switch (kind) {
                        case DEPLOYMENT:
                            if (apiGroupType == K8sApiGroupType.APPS && apiType == K8sApiType.V1) {
                                if (StringUtils.equals(((V1Deployment) obj).getMetadata().getName(), serverYaml.getWorkloadName())) {
                                    deployment = (V1Deployment) obj;
                                }
                            }
                            break;
                        case HORIZONTAL_POD_AUTOSCALER:
                            if (K8sApiType.V2 == apiType) {
                                if ( StringUtils.equals(((V2HorizontalPodAutoscaler) obj).getSpec().getScaleTargetRef().getName(), serverYaml.getWorkloadName()) ) {
                                    hpaV2 = (V2HorizontalPodAutoscaler) obj;
                                }
                            } else if (K8sApiType.V2BETA2 == apiType) {
                                if ( StringUtils.equals(((V2beta2HorizontalPodAutoscaler) obj).getSpec().getScaleTargetRef().getName(), serverYaml.getWorkloadName()) ) {
                                    hpaV2beta2 = (V2beta2HorizontalPodAutoscaler) obj;
                                }
                            } else if (K8sApiType.V2BETA1 == apiType) {
                                if ( StringUtils.equals(((V2beta1HorizontalPodAutoscaler) obj).getSpec().getScaleTargetRef().getName(), serverYaml.getWorkloadName()) ) {
                                    hpaV2beta1 = (V2beta1HorizontalPodAutoscaler) obj;
                                }
                            } else {
                                if ( StringUtils.equals(((V1HorizontalPodAutoscaler) obj).getSpec().getScaleTargetRef().getName(), serverYaml.getWorkloadName()) ) {
                                    hpaV1 = (V1HorizontalPodAutoscaler) obj;
                                }
                            }
                            break;
                        case DAEMON_SET:
                            if (apiGroupType == K8sApiGroupType.APPS && apiType == K8sApiType.V1) {
                                if (StringUtils.equals(((V1DaemonSet) obj).getMetadata().getName(), serverYaml.getWorkloadName())) {
                                    daemonSet = (V1DaemonSet) obj;
                                }
                            }
                            break;
                        case JOB:
                            if (apiGroupType == K8sApiGroupType.BATCH && apiType == K8sApiType.V1) {
                                if (StringUtils.equals(((V1Job) obj).getMetadata().getName(), serverYaml.getWorkloadName())) {
                                    job = (V1Job) obj;
                                }
                            }
                            break;
                        case CRON_JOB:
                            if (apiGroupType == K8sApiGroupType.BATCH && apiType == K8sApiType.V1BETA1) {
                                if ( StringUtils.equals(((V1beta1CronJob) obj).getMetadata().getName(), serverYaml.getWorkloadName()) ) {
                                    cronJobV1beta1 = (V1beta1CronJob) obj;
                                }
                            } else if (apiGroupType == K8sApiGroupType.BATCH && apiType == K8sApiType.V1) {
                                if ( StringUtils.equals(((V1CronJob) obj).getMetadata().getName(), serverYaml.getWorkloadName()) ) {
                                    cronJobV1 = (V1CronJob) obj;
                                }
                            }
                            break;
                        case STATEFUL_SET:
                            if (apiGroupType == K8sApiGroupType.APPS && apiType == K8sApiType.V1) {
                                if (StringUtils.equals(((V1StatefulSet) obj).getMetadata().getName(), serverYaml.getWorkloadName())) {
                                    statefulSet = (V1StatefulSet) obj;
                                }
                            }
                            break;
                    }
                }
            }

            String dateStr = Utils.getNowDateTime();
            boolean hasHPA = hpaV2 != null || hpaV2beta2 != null || hpaV2beta1 != null || hpaV1 != null;

            /**
             * Deployment
             */
            if(workloadType == WorkloadType.SINGLE_SERVER
                    || workloadType == WorkloadType.REPLICA_SERVER) {

                if (deployment != null) {

                    // create deployment
                    if (cluster.getAccount() != null && StringUtils.isNotBlank(cluster.getAccount().getRegistryDownloadUserId())) {
                        this.setImagePullSecret(deployment.getSpec().getTemplate(), CryptoUtils.decryptAES(cluster.getAccount().getRegistryDownloadUserId()));
                    }
                    deployment.getMetadata().putAnnotationsItem(KubeConstants.ANNOTATION_COCKTAIL_DEPLOY_DATETIME, dateStr);

                    // 2022.09.22 hjchoi - 아래 이슈로 HPA가 존재한다면 null 셋팅
                    // HPA가 활성화되어 있으면, 디플로이먼트, 스테이트풀셋 모두 또는 둘 중 하나의 매니페스트에서 spec.replicas의 값을 삭제하는 것이 바람직하다
                    // https://kubernetes.io/ko/docs/tasks/run-application/horizontal-pod-autoscale/#%EB%94%94%ED%94%8C%EB%A1%9C%EC%9D%B4%EB%A8%BC%ED%8A%B8%EC%99%80-%EC%8A%A4%ED%85%8C%EC%9D%B4%ED%8A%B8%ED%92%80%EC%85%8B%EC%9D%84-horizontal-autoscaling%EC%9C%BC%EB%A1%9C-%EC%A0%84%ED%99%98%ED%95%98%EA%B8%B0
                    if(workloadType.isPossibleAutoscaling()) {
                        if (hasHPA) {
                            deployment.getSpec().setReplicas(null);
                        }
                    }
                    k8sWorker.createDeploymentV1(cluster, cluster.getNamespaceName(), deployment, false);
                }

            }
            /**
             * DaemonSet
             */
            else if(workloadType == WorkloadType.DAEMON_SET_SERVER) {

                if (daemonSet != null) {

                    // create daemonSet
                    if (cluster.getAccount() != null && StringUtils.isNotBlank(cluster.getAccount().getRegistryDownloadUserId())) {
                        this.setImagePullSecret(daemonSet.getSpec().getTemplate(), CryptoUtils.decryptAES(cluster.getAccount().getRegistryDownloadUserId()));
                    }
                    daemonSet.getMetadata().putAnnotationsItem(KubeConstants.ANNOTATION_COCKTAIL_DEPLOY_DATETIME, dateStr);
                    k8sWorker.createDaemonSetV1(cluster, cluster.getNamespaceName(), daemonSet, false);
                }

            }
            /**
             * Job
             */
            else if(workloadType == WorkloadType.JOB_SERVER) {

                if (job != null) {

                    // create job
                    if (cluster.getAccount() != null && StringUtils.isNotBlank(cluster.getAccount().getRegistryDownloadUserId())) {
                        this.setImagePullSecret(job.getSpec().getTemplate(), CryptoUtils.decryptAES(cluster.getAccount().getRegistryDownloadUserId()));
                    }
                    job.getMetadata().putAnnotationsItem(KubeConstants.ANNOTATION_COCKTAIL_DEPLOY_DATETIME, dateStr);
                    k8sWorker.createJobV1(cluster, cluster.getNamespaceName(), job, false);
                }

            }
            /**
             * CronJob
             */
            else if(workloadType == WorkloadType.CRON_JOB_SERVER) {

                if (cronJobV1beta1 != null) {

                    // create cronJob
                    if (cluster.getAccount() != null && StringUtils.isNotBlank(cluster.getAccount().getRegistryDownloadUserId())) {
                        this.setImagePullSecret(cronJobV1beta1.getSpec().getJobTemplate().getSpec().getTemplate(), CryptoUtils.decryptAES(cluster.getAccount().getRegistryDownloadUserId()));
                    }
                    cronJobV1beta1.getMetadata().putAnnotationsItem(KubeConstants.ANNOTATION_COCKTAIL_DEPLOY_DATETIME, dateStr);
                    k8sWorker.createCronJobV1beta1(cluster, cluster.getNamespaceName(), cronJobV1beta1, false);
                } else if (cronJobV1 != null) {

                    // create cronJob
                    if (cluster.getAccount() != null && StringUtils.isNotBlank(cluster.getAccount().getRegistryDownloadUserId())) {
                        this.setImagePullSecret(cronJobV1.getSpec().getJobTemplate().getSpec().getTemplate(), CryptoUtils.decryptAES(cluster.getAccount().getRegistryDownloadUserId()));
                    }
                    cronJobV1.getMetadata().putAnnotationsItem(KubeConstants.ANNOTATION_COCKTAIL_DEPLOY_DATETIME, dateStr);
                    k8sWorker.createCronJobV1(cluster, cluster.getNamespaceName(), cronJobV1, false);
                }

            }
            /**
             * StatefulSet
             */
            else if(workloadType == WorkloadType.STATEFUL_SET_SERVER) {

                if (statefulSet != null) {

                    // patch statefulSet
                    if (cluster.getAccount() != null && StringUtils.isNotBlank(cluster.getAccount().getRegistryDownloadUserId())) {
                        this.setImagePullSecret(statefulSet.getSpec().getTemplate(), CryptoUtils.decryptAES(cluster.getAccount().getRegistryDownloadUserId()));
                    }
                    statefulSet.getMetadata().putAnnotationsItem(KubeConstants.ANNOTATION_COCKTAIL_DEPLOY_DATETIME, dateStr);
                    // 2022.09.22 hjchoi - 아래 이슈로 HPA가 존재한다면 null 셋팅
                    // HPA가 활성화되어 있으면, 디플로이먼트, 스테이트풀셋 모두 또는 둘 중 하나의 매니페스트에서 spec.replicas의 값을 삭제하는 것이 바람직하다
                    // https://kubernetes.io/ko/docs/tasks/run-application/horizontal-pod-autoscale/#%EB%94%94%ED%94%8C%EB%A1%9C%EC%9D%B4%EB%A8%BC%ED%8A%B8%EC%99%80-%EC%8A%A4%ED%85%8C%EC%9D%B4%ED%8A%B8%ED%92%80%EC%85%8B%EC%9D%84-horizontal-autoscaling%EC%9C%BC%EB%A1%9C-%EC%A0%84%ED%99%98%ED%95%98%EA%B8%B0
                    if(workloadType.isPossibleAutoscaling()) {
                        if (hasHPA) {
                            statefulSet.getSpec().setReplicas(null);
                        }
                    }
                    k8sWorker.createStatefulSetV1(cluster, cluster.getNamespaceName(), statefulSet, false);

                }

            }

            /** Deployment외에 추가로 StatefulSet에서 AutoScaler 를 적용하면서 코드 이동 (AS-IS : 위 if(deploymentType일 경우에 처리...) **/
            if(workloadType.isPossibleAutoscaling()) {
                if (deployment != null || statefulSet != null) {
                    // hpa
                    if (hpaV2 != null) {
                        List<K8sHorizontalPodAutoscalerVO> currentHpas = workloadResourceService.convertHorizontalPodAutoscalerDataList(cluster, cluster.getNamespaceName(), String.format("%s.%s=%s", KubeConstants.META, KubeConstants.NAME, hpaV2.getMetadata().getName()), null);
                        if (CollectionUtils.isNotEmpty(currentHpas)) {
                            workloadResourceService.patchHorizontalPodAutoscalerV2(cluster, cluster.getNamespaceName(), hpaV2, context);
                        } else {
                            k8sWorker.createHorizontalPodAutoscalerV2(cluster, cluster.getNamespaceName(), hpaV2, false);
                        }
                    } else if (hpaV2beta2 != null) {
                        List<K8sHorizontalPodAutoscalerVO> currentHpas = workloadResourceService.convertHorizontalPodAutoscalerDataList(cluster, cluster.getNamespaceName(), String.format("%s.%s=%s", KubeConstants.META, KubeConstants.NAME, hpaV2beta2.getMetadata().getName()), null);
                        if (CollectionUtils.isNotEmpty(currentHpas)) {
                            workloadResourceService.patchHorizontalPodAutoscalerV2beta2(cluster, cluster.getNamespaceName(), hpaV2beta2, context);
                        } else {
                            k8sWorker.createHorizontalPodAutoscalerV2beta2(cluster, cluster.getNamespaceName(), hpaV2beta2, false);
                        }
                    } else if (hpaV2beta1 != null) {
                        List<K8sHorizontalPodAutoscalerVO> currentHpas = workloadResourceService.convertHorizontalPodAutoscalerDataList(cluster, cluster.getNamespaceName(), String.format("%s.%s=%s", KubeConstants.META, KubeConstants.NAME, hpaV2beta1.getMetadata().getName()), null);
                        if (CollectionUtils.isNotEmpty(currentHpas)) {
                            workloadResourceService.patchHorizontalPodAutoscalerV2beta1(cluster, cluster.getNamespaceName(), hpaV2beta1, context);
                        } else {
                            k8sWorker.createHorizontalPodAutoscalerV2beta1(cluster, cluster.getNamespaceName(), hpaV2beta1, false);
                        }
                    } else if (hpaV1 != null) {
                        List<K8sHorizontalPodAutoscalerVO> currentHpas = workloadResourceService.convertHorizontalPodAutoscalerDataList(cluster, cluster.getNamespaceName(), String.format("%s.%s=%s", KubeConstants.META, KubeConstants.NAME, hpaV1.getMetadata().getName()), null);
                        if (CollectionUtils.isNotEmpty(currentHpas)) {
                            workloadResourceService.patchHorizontalPodAutoscalerV1(cluster, cluster.getNamespaceName(), hpaV1, context);
                        } else {
                            k8sWorker.createHorizontalPodAutoscalerV1(cluster, cluster.getNamespaceName(), hpaV1, false);
                        }
                    }
                }
            }
        } catch (Exception e) {
            try {
                String yamlForLogging = "";
                switch (workloadType) {
                    case SINGLE_SERVER:
                    case REPLICA_SERVER: {
                        yamlForLogging = ServerUtils.marshalYaml(deployment);
                        break;
                    }
                    case CRON_JOB_SERVER: {
                        if (cronJobV1beta1 != null) {
                            yamlForLogging = ServerUtils.marshalYaml(cronJobV1beta1);
                        } else {
                            yamlForLogging = ServerUtils.marshalYaml(cronJobV1);
                        }
                        break;
                    }
                    case JOB_SERVER: {
                        yamlForLogging = ServerUtils.marshalYaml(job);
                        break;
                    }
                    case DAEMON_SET_SERVER: {
                        yamlForLogging = ServerUtils.marshalYaml(daemonSet);
                        break;
                    }
                    case STATEFUL_SET_SERVER: {
                        yamlForLogging = ServerUtils.marshalYaml(statefulSet);
                        break;
                    }
                }
                log.info(String.format("[STEP5] Workload Redeploy Failed [%s] (redeployServerProcess) : %s - %s - %s\n%s", serverYaml.getWorkloadType(), serverYaml.getClusterSeq(), serverYaml.getNamespaceName(), context.getParams().get(CommonConstants.UPDATE_TARGET_WORKLOAD), yamlForLogging));
            }
            catch (Exception ele) {
                log.error(String.format("[STEP5] Workload Redeploy Exception logging failed [%s] (redeployServerProcess) : %s - %s - %s\n%s", serverYaml.getWorkloadType(), serverYaml.getClusterSeq(), serverYaml.getNamespaceName(), context.getParams().get(CommonConstants.UPDATE_TARGET_WORKLOAD), serverGui));
            }
            log.error("Fail to redeploy cocktail '{}' server: {}", workloadType.getCode(), e.getMessage());
            log.error(ExceptionUtils.getStackTrace(e));

//            String errMsg = this.getApiErrorMessage(e);
//            componentService.updateComponentState(context, component, StateCode.ERROR, errMsg, comDao);

            throw e;
        }

//        comDao.updateComponentManifestByNamespace(serverYaml.getClusterSeq(), serverYaml.getNamespaceName(), serverYaml.getWorkloadName(), serverYaml.getYaml(), null);
//        componentService.updateComponentState(context, component, StateCode.RUNNING, comDao);

        return serverYaml;
    }

    /**
     * 서비 종료
     *
     * @param serverYaml
     * @param context
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public ServerYamlVO terminateServerProcess(ServerYamlVO serverYaml, ExecutingContextVO context) throws Exception {

        JSON k8sJson = new JSON();

        IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
        IComponentMapper comDao = this.sqlSession.getMapper(IComponentMapper.class);
//        IClusterVolumeMapper clusterVolumeDao = this.sqlSession.getMapper(IClusterVolumeMapper.class);

        ClusterVO cluster = clusterDao.getCluster(serverYaml.getClusterSeq());
        cluster.setNamespaceName(serverYaml.getNamespaceName());
        ComponentVO component = this.getComponent(serverYaml, StateCode.STOPPING, context, comDao);

//        DeployType deployType = DeployType.valueOf(serverYaml.getDeployType());
        WorkloadType workloadType = WorkloadType.valueOf(serverYaml.getWorkloadType());
//        WorkloadVersion workloadVersion = WorkloadVersion.valueOf(serverYaml.getWorkloadVersion());

        try {
            V1Deployment deployment = null;
            V2HorizontalPodAutoscaler hpaV2 = null;
            V2beta2HorizontalPodAutoscaler hpaV2beta2 = null;
            V2beta1HorizontalPodAutoscaler hpaV2beta1 = null;
            V1HorizontalPodAutoscaler hpaV1 = null;
            V1DaemonSet daemonSet = null;
            V1Job job = null;
            V1beta1CronJob cronJobV1beta1 = null;
            V1CronJob cronJobV1 = null;
            V1StatefulSet statefulSet = null;

            // 설정 정보를 미리 저장
            comDao.updateComponentManifestByNamespace(serverYaml.getClusterSeq(), serverYaml.getNamespaceName(), serverYaml.getWorkloadName(), serverYaml.getYaml(), null);

            List<Object> objs = ServerUtils.getYamlObjects(serverYaml.getYaml());

            if (CollectionUtils.isNotEmpty(objs)) {
                for (Object obj : objs) {
                    Map<String, Object> k8sObjectToMap = ServerUtils.getK8sObjectToMap(obj, k8sJson);
                    K8sApiKindType kind = ServerUtils.getK8sKindInMap(k8sObjectToMap);
                    K8sApiGroupType apiGroupType = ServerUtils.getK8sGroupInMap(k8sObjectToMap);
                    K8sApiType apiType = ServerUtils.getK8sVersionInMap(k8sObjectToMap);

                    switch (kind) {
                        case DEPLOYMENT:
                            if (apiGroupType == K8sApiGroupType.APPS && apiType == K8sApiType.V1) {
                                if (StringUtils.equals(((V1Deployment) obj).getMetadata().getName(), serverYaml.getWorkloadName())) {
                                    deployment = (V1Deployment) obj;
                                }
                            }
                            break;
                        case HORIZONTAL_POD_AUTOSCALER:
                            if (K8sApiType.V2 == apiType) {
                                if ( StringUtils.equals(((V2HorizontalPodAutoscaler) obj).getSpec().getScaleTargetRef().getName(), serverYaml.getWorkloadName()) ) {
                                    hpaV2 = (V2HorizontalPodAutoscaler) obj;
                                }
                            } else if (K8sApiType.V2BETA2 == apiType) {
                                if ( StringUtils.equals(((V2beta2HorizontalPodAutoscaler) obj).getSpec().getScaleTargetRef().getName(), serverYaml.getWorkloadName()) ) {
                                    hpaV2beta2 = (V2beta2HorizontalPodAutoscaler) obj;
                                }
                            } else if (K8sApiType.V2BETA1 == apiType) {
                                if ( StringUtils.equals(((V2beta1HorizontalPodAutoscaler) obj).getSpec().getScaleTargetRef().getName(), serverYaml.getWorkloadName()) ) {
                                    hpaV2beta1 = (V2beta1HorizontalPodAutoscaler) obj;
                                }
                            } else {
                                if ( StringUtils.equals(((V1HorizontalPodAutoscaler) obj).getSpec().getScaleTargetRef().getName(), serverYaml.getWorkloadName()) ) {
                                    hpaV1 = (V1HorizontalPodAutoscaler) obj;
                                }
                            }
                            break;
                        case DAEMON_SET:
                            if (apiGroupType == K8sApiGroupType.APPS && apiType == K8sApiType.V1) {
                                if (StringUtils.equals(((V1DaemonSet) obj).getMetadata().getName(), serverYaml.getWorkloadName())) {
                                    daemonSet = (V1DaemonSet) obj;
                                }
                            }
                            break;
                        case JOB:
                            if (apiGroupType == K8sApiGroupType.BATCH && apiType == K8sApiType.V1) {
                                if (StringUtils.equals(((V1Job) obj).getMetadata().getName(), serverYaml.getWorkloadName())) {
                                    job = (V1Job) obj;
                                }
                            }
                            break;
                        case CRON_JOB:
                            if (apiGroupType == K8sApiGroupType.BATCH && apiType == K8sApiType.V1BETA1) {
                                if ( StringUtils.equals(((V1beta1CronJob) obj).getMetadata().getName(), serverYaml.getWorkloadName()) ) {
                                    cronJobV1beta1 = (V1beta1CronJob) obj;
                                }
                            } else if (apiGroupType == K8sApiGroupType.BATCH && apiType == K8sApiType.V1) {
                                if ( StringUtils.equals(((V1CronJob) obj).getMetadata().getName(), serverYaml.getWorkloadName()) ) {
                                    cronJobV1 = (V1CronJob) obj;
                                }
                            }
                            break;
                        case STATEFUL_SET:
                            if (apiGroupType == K8sApiGroupType.APPS && apiType == K8sApiType.V1) {
                                if (StringUtils.equals(((V1StatefulSet) obj).getMetadata().getName(), serverYaml.getWorkloadName())) {
                                    statefulSet = (V1StatefulSet) obj;
                                }
                            }
                            break;
                    }
                }
            }

            /**
             * Deployment
             */
            if(workloadType == WorkloadType.SINGLE_SERVER
                    || workloadType == WorkloadType.REPLICA_SERVER) {

                if (CollectionUtils.isNotEmpty(objs)) {
                    if (deployment != null) {
                        workloadResourceService.deleteDeployment(cluster, deployment.getMetadata().getName(), context);
                    }
                }
            }
            /**
             * DaemonSet
             */
            else if(workloadType == WorkloadType.DAEMON_SET_SERVER) {
                if (daemonSet != null) {
                    workloadResourceService.deleteDaemonSet(cluster, daemonSet.getMetadata().getName(), context);
                }
            }
            /**
             * Job
             */
            else if(workloadType == WorkloadType.JOB_SERVER) {
                if (job != null) {
                    workloadResourceService.deleteJob(cluster, job.getMetadata().getName(), context);
                }
            }
            /**
             * CronJob
             */
            else if(workloadType == WorkloadType.CRON_JOB_SERVER) {
                if (cronJobV1beta1 != null) {
                    workloadResourceService.deleteCronJob(cluster, cronJobV1beta1.getMetadata().getName(), context);
                } else if (cronJobV1 != null) {
                    workloadResourceService.deleteCronJob(cluster, cronJobV1.getMetadata().getName(), context);
                }
            }
            /**
             * StatefulSet
             */
            else if(workloadType == WorkloadType.STATEFUL_SET_SERVER) {
                if (statefulSet != null) {
                    workloadResourceService.deleteStatefulSet(cluster, statefulSet.getMetadata().getName(), context);
                }
            }

            /** Deployment외에 추가로 StatefulSet에서 AutoScaler 를 적용하면서 코드 이동 (AS-IS : 위 if(deploymentType일 경우에 처리...) **/
            if(workloadType.isPossibleAutoscaling()) {
                List<K8sHorizontalPodAutoscalerVO> currentHpas = Lists.newArrayList();
                String hpaName = "";
                if (hpaV2 != null) {
                    currentHpas = workloadResourceService.convertHorizontalPodAutoscalerDataList(cluster, cluster.getNamespaceName(), String.format("%s.%s=%s", KubeConstants.META, KubeConstants.NAME, hpaV2.getMetadata().getName()), null);
                    hpaName = hpaV2.getMetadata().getName();
                } else if (hpaV2beta2 != null) {
                    currentHpas = workloadResourceService.convertHorizontalPodAutoscalerDataList(cluster, cluster.getNamespaceName(), String.format("%s.%s=%s", KubeConstants.META, KubeConstants.NAME, hpaV2beta2.getMetadata().getName()), null);
                    hpaName = hpaV2beta2.getMetadata().getName();
                } else if (hpaV2beta1 != null) {
                    currentHpas = workloadResourceService.convertHorizontalPodAutoscalerDataList(cluster, cluster.getNamespaceName(), String.format("%s.%s=%s", KubeConstants.META, KubeConstants.NAME, hpaV2beta1.getMetadata().getName()), null);
                    hpaName = hpaV2beta1.getMetadata().getName();
                } else if (hpaV1 != null) {
                    currentHpas = workloadResourceService.convertHorizontalPodAutoscalerDataList(cluster, cluster.getNamespaceName(), String.format("%s.%s=%s", KubeConstants.META, KubeConstants.NAME, hpaV1.getMetadata().getName()), null);
                    hpaName = hpaV1.getMetadata().getName();
                }

                if (CollectionUtils.isNotEmpty(currentHpas) && StringUtils.isNotBlank(hpaName)) {
                    workloadResourceService.deleteHorizontalPodAutoscaler(cluster, cluster.getNamespaceName(), hpaName, null);
                }
            }

            /**
             * AccountType in (Cocktail, Apps) 일 경우에 pipeline를 삭제하도록 함
             */
            if (context != null && context.getUserAccount() != null && !context.getUserAccount().getAccountType().isCubeEngine()) {
                /**
                 * pipeline 삭제
                 */
                try {
                    pipelineFlowService.removePipelineByComponent(component, context);
                } catch (Exception e) {
                    throw new CocktailException("fail remove pipeline!!", e, ExceptionType.InternalError);
                }
            }


        } catch (Exception e) {
            log.error("Fail to terminate cocktail '{}' server: {}", workloadType.getCode(), e.getMessage());
            log.error(ExceptionUtils.getStackTrace(e));

//            String errMsg = this.getApiErrorMessage(e);
//            componentService.updateComponentState(context, component, StateCode.ERROR, errMsg, comDao);

            throw e;
        }

        comDao.updateComponentManifestByNamespace(serverYaml.getClusterSeq(), serverYaml.getNamespaceName(), serverYaml.getWorkloadName(), serverYaml.getYaml(), null);
        componentService.updateComponentState(context, component, StateCode.STOPPED, comDao);

        return serverYaml;
    }

    public void updateService(ClusterVO cluster, String namespace, String name, V1Service updateService, ExecutingContextVO context) throws Exception {
        K8sServiceVO currentSvc = serviceSpecService.getService(cluster, namespace, name, context);
        if (updateService != null) {
            if (currentSvc != null) {
                serviceSpecService.patchServiceV1(cluster, namespace, updateService, context);
            } else {
                k8sWorker.createServiceV1(cluster, namespace, updateService, false);
            }
        } else {
            if (currentSvc != null) {
                serviceSpecService.deleteService(cluster, name, null, context);
            }
        }
    }

    /**
     * #################################################################################################################################################################################################
     * #################################################################################################################################################################################################
     * end - server
     * #################################################################################################################################################################################################
     * #################################################################################################################################################################################################
     */

//    private String composeRegistryUrl(ContainerVO container) {
//
//        String registryUrl;
//        if (container.getPrivateRegistryYn().compareToIgnoreCase("N") == 0) {
//            registryUrl = String.format("%s:%s", container.getImageName(), container.getImageTag());
//        } else {
//            String url = registryProp.getUrl();
//            if (url.startsWith("http://")) {
//                registryUrl = String.format("%s/%s:%s", registryProp.getUrl().substring("http://".length()), container.getImageName(), container.getImageTag());
//            } else if (url.startsWith("https://")) {
//                registryUrl = String.format("%s/%s:%s", registryProp.getUrl().substring("https://".length()), container.getImageName(), container.getImageTag());
//            } else {
//                registryUrl = String.format("%s/%s:%s", registryProp.getUrl(), container.getImageName(), container.getImageTag());
//            }
//        }
//        return registryUrl;
//    }

    /**
     * io.kubernetes.client.ApiException 의 오류메시지를 찾아서 저장
     *
     * @param e
     * @return
     */
    private String getApiErrorMessage(Exception e) {
        String errMsg = null;

        if (e instanceof CocktailException) {
            Throwable rootCause = ExceptionUtils.getRootCause(e);
            if (rootCause != null) {
                if (rootCause instanceof ApiException) {
                    if (((CocktailException) e).getData() != null) {
                        JSON k8sJson = new JSON();
                        errMsg = k8sJson.serialize(((CocktailException) e).getData());
                    } else {
                        errMsg = rootCause.getMessage();
                    }
                }
            }
            if (StringUtils.isBlank(errMsg)) {
                for(Throwable throwableRow : ExceptionUtils.getThrowableList(e)){
                    if (throwableRow instanceof ApiException) {
                        if (((CocktailException) e).getData() != null) {
                            JSON k8sJson = new JSON();
                            errMsg = k8sJson.serialize(((CocktailException) e).getData());
                        } else {
                            errMsg = throwableRow.getMessage();
                        }
                        break;
                    }
                }
            }

            if (StringUtils.isBlank(errMsg)) {
                errMsg = e.getMessage();
            }
        } else {
            errMsg = e.getMessage();
        }

        return errMsg;
    }

    private void setImagePullSecret(V1PodTemplateSpec v1PodTemplateSpec, String registryUser) throws Exception {

        // registry url
        String registryUrl = ResourceUtil.getRegistryUrl();

        boolean usePrivate = false;
        List<V1Container> allContainers = new ArrayList<>();
        if (allContainers == null) {
            allContainers = new ArrayList<>();
        }
        if (CollectionUtils.isNotEmpty(v1PodTemplateSpec.getSpec().getInitContainers())) {
            allContainers.addAll(v1PodTemplateSpec.getSpec().getInitContainers());
        }
        if (CollectionUtils.isNotEmpty(v1PodTemplateSpec.getSpec().getContainers())) {
            allContainers.addAll(v1PodTemplateSpec.getSpec().getContainers());
        }

        for (V1Container c : allContainers) {
            if (StringUtils.startsWith(c.getImage(), registryUrl)) {
                usePrivate = true;
                break;
            }
        }
        if (usePrivate) {
            if (StringUtils.isNotBlank(registryUser)) {
                V1LocalObjectReference ref = new V1LocalObjectReference();
                ref.setName(registryUser);
                if (CollectionUtils.isNotEmpty(v1PodTemplateSpec.getSpec().getImagePullSecrets())) {
//                Optional<V1LocalObjectReference> v1LocalObjectReferenceOptional = v1PodTemplateSpec.getSpec().getImagePullSecrets().stream().filter(v1LocalObjectReference -> (StringUtils.equals(v1LocalObjectReference.getName(), ref.getName()))).findFirst();
//                if (!v1LocalObjectReferenceOptional.isPresent()) {
//                    v1PodTemplateSpec.getSpec().addImagePullSecretsItem(ref);
//                }
                    if (!v1PodTemplateSpec.getSpec().getImagePullSecrets().contains(ref)) {
                        v1PodTemplateSpec.getSpec().addImagePullSecretsItem(ref);
                    }
                } else {
                    v1PodTemplateSpec.getSpec().addImagePullSecretsItem(ref);
                }
            }
        }
    }

    private void setImagePullPolicy(List<V1Container> containers) throws Exception {
        if (CollectionUtils.isNotEmpty(containers)) {
            for (V1Container containerRow : containers) {
                containerRow.setImagePullPolicy(ImagePullPolicyType.Always.getCode());
            }
        }
    }

    private ComponentVO getComponent(ServerYamlVO serverYaml, StateCode stateCode, ExecutingContextVO context, IComponentMapper comDao) throws Exception {
        ComponentVO component = comDao.getComponentByClusterAndNames(serverYaml.getClusterSeq(), serverYaml.getNamespaceName(), serverYaml.getWorkloadName());
        if (component == null) {
            component = new ComponentVO();
            component.setComponentType(ComponentType.CSERVER.name());
            component.setClusterSeq(serverYaml.getClusterSeq());
            component.setNamespaceName(serverYaml.getNamespaceName());
            component.setComponentName(serverYaml.getWorkloadName());
            component.setDescription(serverYaml.getDescription());
            component.setUseYn("Y");
            component.setWorkloadType(serverYaml.getWorkloadType());
            component.setWorkloadVersion(serverYaml.getWorkloadVersion());
            component.setWorkloadManifest(serverYaml.getYaml());
            component.setStateCode(stateCode.getCode());
            component.setCreator(context.getUserSeq());
        }

        return component;
    }

}
