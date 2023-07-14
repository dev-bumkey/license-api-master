package run.acloud.api.cserver.service;

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
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.mybatis.spring.SqlSessionTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import run.acloud.api.configuration.dao.IClusterMapper;
import run.acloud.api.configuration.service.ClusterStateService;
import run.acloud.api.configuration.util.ClusterUtils;
import run.acloud.api.configuration.vo.ClusterVO;
import run.acloud.api.cserver.dao.IServicemapMapper;
import run.acloud.api.cserver.dao.IWorkloadGroupMapper;
import run.acloud.api.cserver.enums.PortType;
import run.acloud.api.cserver.enums.StateCode;
import run.acloud.api.cserver.enums.WorkloadType;
import run.acloud.api.cserver.enums.WorkloadVersion;
import run.acloud.api.cserver.util.ServerUtils;
import run.acloud.api.cserver.vo.ServerStateVO;
import run.acloud.api.cserver.vo.ServicemapSummaryVO;
import run.acloud.api.cserver.vo.WorkloadGroupVO;
import run.acloud.api.k8sextended.models.V1beta1CronJob;
import run.acloud.api.k8sextended.models.V1beta1CronJobSpec;
import run.acloud.api.k8sextended.models.V1beta1CronJobStatus;
import run.acloud.api.k8sextended.models.V1beta1JobTemplateSpec;
import run.acloud.api.resource.constants.KubeConstants;
import run.acloud.api.resource.dao.IComponentMapper;
import run.acloud.api.resource.enums.*;
import run.acloud.api.resource.service.K8sResourceService;
import run.acloud.api.resource.service.WorkloadResourceService;
import run.acloud.api.resource.task.K8sWorker;
import run.acloud.api.resource.util.ResourceUtil;
import run.acloud.api.resource.vo.*;
import run.acloud.commons.util.JsonUtils;
import run.acloud.commons.util.Utils;
import run.acloud.commons.vo.ExecutingContextVO;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ServerStateService {

    private class StateAndCount {
        StateCode state;
        int count = 0;
    }

    @Resource(name = "cocktailSession")
    private SqlSessionTemplate sqlSession;

	@Autowired
	private ComponentService componentService;

    @Autowired
    private WorkloadResourceService workloadResourceService;

    @Autowired
    private K8sResourceService k8sResourceService;

    @Autowired
    private K8sWorker k8sWorker;

    @Autowired
    private ClusterStateService clusterStateService;

    @Autowired
    private ServerConversionService serverConversionService;

    private static Logger stateLogger = LoggerFactory.getLogger("server.state.logger");


    public ServerStateVO getWorkloadsStateInNamespace(Integer clusterSeq, String namespace, String workloadName, boolean canActualizeState, ExecutingContextVO context) throws Exception {
        IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
        ClusterVO cluster = clusterDao.getCluster(clusterSeq);
        cluster.setNamespaceName(namespace);
        return this.getWorkloadsStateInNamespace(cluster, namespace, workloadName, canActualizeState, context);
    }

    public ServerStateVO getWorkloadsStateInNamespace(String clusterId, String namespace, String workloadName, boolean canActualizeState, ExecutingContextVO context) throws Exception {
        IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
        ClusterVO cluster = clusterDao.getClusterByClusterId(clusterId, "Y");
        cluster.setNamespaceName(namespace);
        return this.getWorkloadsStateInNamespace(cluster, namespace, workloadName, canActualizeState, context);
    }

    public ServerStateVO getWorkloadsStateInNamespace(ClusterVO cluster, String namespace, String workloadName, boolean canActualizeState, ExecutingContextVO context) throws Exception {
        return this.getWorkloadsStateInNamespace(cluster, namespace, workloadName, canActualizeState, false, context);
    }

    public ServerStateVO getWorkloadsStateInNamespace(ClusterVO cluster, String namespace, String workloadName, boolean canActualizeState, boolean useExistingWorkload, ExecutingContextVO context) throws Exception {
        return this.getWorkloadsStateInNamespace(cluster, namespace, workloadName, canActualizeState, useExistingWorkload, false, context);
    }

    public ServerStateVO getWorkloadsStateInNamespace(ClusterVO cluster, String namespace, String workloadName, boolean canActualizeState, boolean useExistingWorkload, boolean usePodEventInfo, ExecutingContextVO context) throws Exception {
        /** 응답 객체 생성 **/
        ServerStateVO serverStates = new ServerStateVO();

        /** Component 객체 생성 **/
        List<ComponentVO> components = new ArrayList<>();

        /** cluster 상태 체크 **/
        clusterStateService.checkClusterState(cluster);

        /** Group 조회 **/
        IWorkloadGroupMapper workloadGroupDao = sqlSession.getMapper(IWorkloadGroupMapper.class);
        List<WorkloadGroupVO> workloadGroups = workloadGroupDao.getWorkloadGroupsOfNamespace(cluster.getClusterSeq(), namespace);
        Map<Integer, WorkloadGroupVO> workloadGroupMap = Optional.ofNullable(workloadGroups).orElseGet(() ->Lists.newArrayList()).stream().collect(Collectors.toMap(WorkloadGroupVO::getWorkloadGroupSeq, Function.identity()));

        /** Appmap 내의 Component 상세 정보 및 Actualize State 조회 **/
        ComponentDetailsVO componentDetails = this.getComponentDetails(cluster, namespace, workloadName, canActualizeState, true, usePodEventInfo, context);

        if (componentDetails != null) {
            components.addAll(componentDetails.getComponents());

            if (useExistingWorkload) {
                components.removeIf(c -> (BooleanUtils.isFalse(c.getIsK8sResourceExist())));
            }

            for (ComponentVO component : components) {
                List<ServerUrlVO> serverUrls = new ArrayList<>();
                component.setServerUrls(serverUrls);
                // 그룹 목록에 워크로드의 그룹이 없다면 null 처리
                if (MapUtils.isNotEmpty(workloadGroupMap) && MapUtils.getObject(workloadGroupMap, component.getWorkloadGroupSeq(), null) == null) {
                    component.setWorkloadGroupSeq(null);
                }

                if (CollectionUtils.isNotEmpty(component.getServices())) {
                    for (ServiceSpecGuiVO serviceSpecRow : component.getServices()) {
                        /**
                         * 3.5.0 : 2019.09.26 : 전체 서비스에 대해서 url 구성
                         * Headless 서비스는 제외
                         **/
                        if (!BooleanUtils.toBoolean(serviceSpecRow.getHeadlessFlag())) {
                            for (ServicePortVO servicePortRow : serviceSpecRow.getServicePorts()) {
                                ServerUrlVO serverUrl = new ServerUrlVO();
                                switch (PortType.valueOf(serviceSpecRow.getServiceType())) {
                                    case CLUSTER_IP:
                                        if (StringUtils.isNotBlank(serviceSpecRow.getClusterIp()) && servicePortRow.getPort() != null) {
                                            serverUrl.setUrl(String.format("%s:%s", serviceSpecRow.getClusterIp(), servicePortRow.getPort()));
                                        }
                                        else {
                                            serverUrl.setUrl("");
                                        }
                                        break;
                                    case NODE_PORT:
                                        if (StringUtils.isNotBlank(cluster.getNodePortUrl())) {
                                            serverUrl.setUrl(String.format("%s:%s", cluster.getNodePortUrl(), servicePortRow.getNodePort()));
                                        }
                                        else {
                                            serverUrl.setUrl("");
                                        }
                                        break;
                                    case LOADBALANCER:
                                        if (StringUtils.isNotBlank(serviceSpecRow.getLoadBalancer())) {
                                            serverUrl.setUrl(String.format("%s:%s", serviceSpecRow.getLoadBalancer(), servicePortRow.getPort()));
                                        }
                                        else {
                                            serverUrl.setUrl("");
                                        }
                                        break;
                                    default:
                                        serverUrl.setUrl("");
                                        break;
                                }

                                serverUrl.setServiceType(serviceSpecRow.getServiceType());
                                serverUrl.setAlias(servicePortRow.getName());
                                serverUrls.add(serverUrl);
                            }
                        }
                    }
                }
            }
        }

        ClusterUtils.setNullClusterInfo(cluster);

        serverStates.setClusterSeq(cluster.getClusterSeq());
        serverStates.setNamespaceName(namespace);
//        serverStates.setCluster(cluster);
        serverStates.setClusters(Arrays.asList(cluster));
		serverStates.setWorkloadGroups(workloadGroups);
        serverStates.setComponents(components);

        return serverStates;
    }

    /**
     * Component의 상세 정보 조회.
     *
     * @param cluster
     * @param namespace
     * @param workloadName
     * @param canActualizeState
     * @param context
     * @return
     * @throws Exception
     */
    public ComponentDetailsVO getComponentDetails(ClusterVO cluster, String namespace, String workloadName, boolean canActualizeState, ExecutingContextVO context) throws Exception {
        return getComponentDetails(cluster, namespace, workloadName, canActualizeState, true, false, context);
    }

    /**
     * Component의 상세 정보 조회.
     *
     * @param cluster
     * @param namespace
     * @param workloadName
     * @param canActualizeState
     * @param useAdditionalInfo
     * @param context
     * @return
     * @throws Exception
     */
	public ComponentDetailsVO getComponentDetails(ClusterVO cluster, String namespace, String workloadName, boolean canActualizeState, boolean useAdditionalInfo, ExecutingContextVO context) throws Exception {
        return getComponentDetails(cluster, namespace, workloadName, canActualizeState, true, false, context);
    }

    /**
     * Component의 상세 정보 조회.
     *
     * @param cluster
     * @param namespace
     * @param workloadName
     * @param canActualizeState
     * @param useAdditionalInfo
     * @param usePodEventInfo - canActualizeState = true 일 경우에만 동작
     * @param context
     * @return
     * @throws Exception
     */
	public ComponentDetailsVO getComponentDetails(ClusterVO cluster, String namespace, String workloadName, boolean canActualizeState, boolean useAdditionalInfo, boolean usePodEventInfo, ExecutingContextVO context) throws Exception {

        IComponentMapper compoDao = sqlSession.getMapper(IComponentMapper.class);
        IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
        IServicemapMapper servicemapDao = sqlSession.getMapper(IServicemapMapper.class);

        cluster.setNamespaceName(namespace);

        ComponentFilterVO compoFilter = new ComponentFilterVO();
        compoFilter.setClusterSeq(cluster.getClusterSeq());
        compoFilter.setNamespaceName(namespace);
        compoFilter.setComponentName(workloadName);

        /** Cluster 전체 데이터 조회인가? **/
        boolean getAllComponentsInCluster = (StringUtils.isBlank(namespace) && StringUtils.isBlank(workloadName) && cluster != null);

        // cluster & appmap 별 리스트임
        List<ComponentDetailsVO> componentDetailsList = compoDao.getServerDetails(compoFilter);

        /** Component 전체 목록 보관 **/
        ComponentDetailsVO componentDetails = new ComponentDetailsVO();
        componentDetails.setComponents(Lists.newArrayList());

        List<ComponentVO> componentVOList = Lists.newArrayList();

        if (CollectionUtils.isNotEmpty(componentDetailsList)) {
            /** 3.5.0 : 2019.09.27 :
             * AS-IS : appmapSeq를 Input으로 넣고 appmapSeq에 해당하는 Row가 1건이라 가정하고 처리함
             * componentDetails = componentDetailsList.get(0);
             * TO-BE : Cluster 전체에서 조회할 경우 여러건이 조회될 수 있고, 실제 입력한 appmapSeq와 동일한 Row를 할당하는 것이 정확할 것으로 생각되어 개선..
             * TO-BE : namespace 동일여부 체크하던 부분은 data 조회시 검색조건으로 포함되어 있어, namespace 데이터만 나옴, 필터링 할 필요가 없음, 필터링 로직 제거, coolingi 2019/12/13
             */
            for(ComponentDetailsVO c : componentDetailsList) {
                // 클러스터 전체 조회일 경우 조회된 모든 Component를 입력..
//                componentDetails.getComponents().addAll(c.getComponents());
                componentVOList.addAll(c.getComponents());
            }
        }

        String labelSelector = null;
        String fieldSelector = null;
        if (StringUtils.isNotBlank(workloadName)) {
            fieldSelector = String.format("%s.%s=%s", KubeConstants.META, KubeConstants.NAME, workloadName);
        }

        // map<namespace, ServicemapSummaryVO>
        Map<String, ServicemapSummaryVO> servicemapNamespaceMap = Maps.newHashMap();
        if (cluster != null) {
            if (StringUtils.isNotBlank(namespace)) {
                servicemapNamespaceMap.put(namespace, servicemapDao.getServicemapSummary(cluster.getClusterSeq(), namespace, null));
            } else {
                List<ServicemapSummaryVO> servicemapInfos = servicemapDao.getServicemapSummaries(null, cluster.getClusterSeq(), null, null);
                servicemapNamespaceMap.putAll(Optional.ofNullable(servicemapInfos).orElseGet(() ->Lists.newArrayList()).stream().collect(Collectors.toMap(ServicemapSummaryVO::getNamespaceName, Function.identity())));
                Optional.ofNullable(servicemapNamespaceMap).orElseGet(() ->Maps.newHashMap());
            }
        }

        List<V1Deployment> deployments = new ArrayList<>();
        List<V1StatefulSet> statefulSets = new ArrayList<>();
        List<V1DaemonSet> daemonSets = new ArrayList<>();
        List<V1Job> tempJobs = new ArrayList<>();
        List<V1Job> jobs = new ArrayList<>();
        List<V1beta1CronJob> cronJobsV1beta1 = new ArrayList<>();
        List<V1CronJob> cronJobsV1 = new ArrayList<>();
        List<V1Pod> pods = canActualizeState ? new ArrayList<>() : null;
        List<V1ReplicaSet> replicaSets = canActualizeState ? new ArrayList<>() : null;

        Map<String, V1Deployment> deploymentMap = new HashMap<>();
        Map<String, V1StatefulSet> statefulSetMap = new HashMap<>();
        Map<String, V1DaemonSet> daemonSetMap = new HashMap<>();
        Map<String, V1Job> jobMap = new HashMap<>();
        Map<String, Map<String, V1Job>> jobMapInCronJob = new HashMap<>(); // Map<ownerName, Map<jobName, job>>
        Map<String, V1beta1CronJob> cronJobMapV1beta1 = new HashMap<>();
        Map<String, V1CronJob> cronJobMapV1 = new HashMap<>();
        Map<String, List<V1Pod>> tempPodMap = new HashMap<>();
        Map<String, List<V1Pod>> podMap = new HashMap<>();
        Map<String, List<V1ReplicaSet>> replicaSetMap = new HashMap<>();
        // Map<Kind, Map<name, List<K8sEventVO>>>
        Map<String, Map<String, List<K8sEventVO>>> eventMap = new HashMap<>();


        workloadResourceService.getWorkloadResource(
                cluster, namespace, fieldSelector, labelSelector,
                deployments, replicaSets, statefulSets, daemonSets, tempJobs, cronJobsV1, cronJobsV1beta1, pods, null
        );
        // workload 단일 조회이고 cronJob 일 경우 상기 getWorkloadResource 메소드 조회시 fieldSelector에서 제외되어 별도로 조회하여 jobs 셋팅
        if (StringUtils.isNotBlank(workloadName) && (CollectionUtils.isNotEmpty(cronJobsV1beta1) || CollectionUtils.isNotEmpty(cronJobsV1))) {
            tempJobs.addAll(k8sWorker.getJobsV1(cluster, namespace, null, null));
        }
        if (canActualizeState) {
            /** Pod 리스트를 맵으로 변환 : Key는 Namespace+owner ReplicaSet Name**/
            this.getPodToMap(pods, tempPodMap);

            /** ReplicaSet 리스트를 맵으로 변환 : Key는 Namespace+owner Deployment Name**/
            this.getReplicaSetToMap(replicaSets, replicaSetMap);

            /** Event **/
            if (usePodEventInfo) {
                List<K8sEventVO> events = k8sResourceService.convertEventDataList(cluster, namespace, String.format("%s=%s", "involvedObject.kind", K8sApiKindType.POD.getValue()),null);

                if (CollectionUtils.isNotEmpty(events)) {
                    for (K8sEventVO eventRow : events) {
                        if (!eventMap.containsKey(eventRow.getKind())) {
                            eventMap.put(eventRow.getKind(), Maps.newHashMap());
                        }
                        if (!eventMap.get(eventRow.getKind()).containsKey(eventRow.getName())) {
                            eventMap.get(eventRow.getKind()).put(eventRow.getName(), Lists.newArrayList());
                        }

                        eventMap.get(eventRow.getKind()).get(eventRow.getName()).add(eventRow);
                    }
                }
            }
        }

        /** Deployment를 맵으로 변환 **/
        // 1. Deployment에 포함된 Pod 목록을 분류..
        if (canActualizeState) {
            for (V1Deployment deploymentRow : deployments) {
                // 1-1. Current Row Deployment가 가지고 있는 ReplicaSet 목록을 조회
                String uniqueDeploymentNameInCluster = this.makeClusterUniqueName(deploymentRow.getMetadata().getNamespace(), deploymentRow.getMetadata().getName());
                List<V1ReplicaSet> replicaSetInCurrentDeployment = replicaSetMap.get(uniqueDeploymentNameInCluster);
                if(CollectionUtils.isNotEmpty(replicaSetInCurrentDeployment)) {
                    // 1-2. 해당 ReplicaSet 목록을 ownerRefrence로 가지고 있는 Pod 목록을 조회...
                    for(V1ReplicaSet replicaSetRow : replicaSetInCurrentDeployment) {
                        String uniqueReplicaSetNameInCluster = this.makeClusterUniqueName(replicaSetRow.getMetadata().getNamespace(), replicaSetRow.getMetadata().getName());
                        if (MapUtils.getObject(tempPodMap, uniqueReplicaSetNameInCluster, null) != null) {
                            // 1-3. 해당 Pod 목록을 Current Deployment의 이름으로 Map에 저장..
                            if (MapUtils.getObject(podMap, uniqueDeploymentNameInCluster, null) == null) {
                                podMap.put(uniqueDeploymentNameInCluster, Lists.newArrayList());
                            }
                            podMap.get(uniqueDeploymentNameInCluster).addAll(MapUtils.getObject(tempPodMap, uniqueReplicaSetNameInCluster, Lists.newArrayList()));
                        }
                    }
                }
            }
        }

        // 2. Deployment를 맵으로 변환 : Key는 Namespace+Deployment 이름
        this.getDeploymentToMap(deployments, deploymentMap);

        /** StatefulSet을 맵으로 변환 **/
        if (canActualizeState) {
            // 1. StatefulSet에 포함된 Pod 목록을 분류..
            for (V1StatefulSet statefulSetRow : statefulSets) {
                String uniqueStatefulSetNameInCluster = this.makeClusterUniqueName(statefulSetRow.getMetadata().getNamespace(), statefulSetRow.getMetadata().getName());
                if (MapUtils.getObject(tempPodMap, uniqueStatefulSetNameInCluster, null) != null) {
                    podMap.put(uniqueStatefulSetNameInCluster, MapUtils.getObject(tempPodMap, uniqueStatefulSetNameInCluster, null));
                }
            }
        }

        // 2. StatefulSet을 맵으로 변환 : Key는 Namespace+Statefulset 이름
        this.getStatefulSetToMap(statefulSets, statefulSetMap);

        /** DaemonSet을 맵으로 변환 **/
        if (canActualizeState) {
            // 1. DaemonSet에 포함된 Pod 목록을 분류..
            for (V1DaemonSet daemonSetRow : daemonSets) {
                String uniqueDaemonSetNameInCluster = this.makeClusterUniqueName(daemonSetRow.getMetadata().getNamespace(), daemonSetRow.getMetadata().getName());
                if (MapUtils.getObject(tempPodMap, uniqueDaemonSetNameInCluster, null) != null) {
                    podMap.put(uniqueDaemonSetNameInCluster, MapUtils.getObject(tempPodMap, uniqueDaemonSetNameInCluster, null));
                }
            }
        }

        // 2. DaemonSet을 맵으로 변환 : Key는 Namespace+DaemonSet 이름
        this.getDaemonSetToMap(daemonSets, daemonSetMap);

        /** Job을 맵으로 변환 **/
        // 1. Job목록 중 CronJob에 해당하는 내용을 따로 분류..
        this.getJobToMapListWithFilteringJobWorkload(tempJobs, jobs, jobMapInCronJob); // jobListMapInCronJob : CronJob에 포함된 Map 판단용 / jobs 안에는 순수 Job만 입력됨..
        // Job에 포함된 Pod 목록을 분류..
        if (canActualizeState) {
            for (V1Job jobRow : jobs) {
                String uniqueJobNameInCluster = this.makeClusterUniqueName(jobRow.getMetadata().getNamespace(), jobRow.getMetadata().getName());
                if (MapUtils.getObject(tempPodMap, uniqueJobNameInCluster, null) != null) {
                    podMap.put(uniqueJobNameInCluster, MapUtils.getObject(tempPodMap, uniqueJobNameInCluster, null));
                }
            }
        }

        // 2. Job을 맵으로 변환 : Key는 Namespace+Job 이름
        this.getJobToMap(jobs, jobMap); // jobMap : 실제 Job 워크로드

        /** CronJob을 맵으로 변환 **/
        K8sApiVerKindType cronType = ResourceUtil.getApiVerKindType(cluster, K8sApiKindType.CRON_JOB);

        if (cronType != null) {

            if (cronType.getGroupType() == K8sApiGroupType.BATCH && cronType.getApiType() == K8sApiType.V1BETA1) {
                if (canActualizeState) {
                    // 1. CronJob에 포함된 Pod 목록을 분류..
                    for (V1beta1CronJob cronJobRow : cronJobsV1beta1) {
                        // 1-1. Current Row CronJob에 해당하는 Job목록을 조회..
                        String uniqueCronJobNameInCluster = this.makeClusterUniqueName(cronJobRow.getMetadata().getNamespace(), cronJobRow.getMetadata().getName());
                        Map<String, V1Job> jobInCurrentCronJob = jobMapInCronJob.get(uniqueCronJobNameInCluster);
                        if (MapUtils.isNotEmpty(jobInCurrentCronJob)) {
                            // 1-2. 해당 Jobs 목록을 ownerRefrence로 가지고 있는 Pod 목록을 조회...
                            for (Map.Entry<String, V1Job> jobRow : jobInCurrentCronJob.entrySet()) {
                                String uniqueJobNameInCluster = this.makeClusterUniqueName(jobRow.getValue().getMetadata().getNamespace(), jobRow.getValue().getMetadata().getName());
                                if (MapUtils.getObject(tempPodMap, uniqueJobNameInCluster, null) != null) {
                                    // 1-3. 해당 Pod 목록을 Current CronJob 이름으로 Map에 저장..
                                    if (MapUtils.getObject(podMap, uniqueCronJobNameInCluster, null) == null) {
                                        podMap.put(uniqueCronJobNameInCluster, Lists.newArrayList());
                                    }
                                    podMap.get(uniqueCronJobNameInCluster).addAll(MapUtils.getObject(tempPodMap, uniqueJobNameInCluster, Lists.newArrayList()));
                                }
                            }
                        }
                    }
                }

                // 2. CronJob을 맵으로 변환 : Key는 Namespace+CronJob 이름
                this.getCronJobV1beta1ToMap(cronJobsV1beta1, cronJobMapV1beta1);

            } else if (cronType.getGroupType() == K8sApiGroupType.BATCH && cronType.getApiType() == K8sApiType.V1) {
                if (canActualizeState) {
                    // 1. CronJob에 포함된 Pod 목록을 분류..
                    for (V1CronJob cronJobRow : cronJobsV1) {
                        // 1-1. Current Row CronJob에 해당하는 Job목록을 조회..
                        String uniqueCronJobNameInCluster = this.makeClusterUniqueName(cronJobRow.getMetadata().getNamespace(), cronJobRow.getMetadata().getName());
                        Map<String, V1Job> jobInCurrentCronJob = jobMapInCronJob.get(uniqueCronJobNameInCluster);
                        if (MapUtils.isNotEmpty(jobInCurrentCronJob)) {
                            // 1-2. 해당 Jobs 목록을 ownerRefrence로 가지고 있는 Pod 목록을 조회...
                            for (Map.Entry<String, V1Job> jobRow : jobInCurrentCronJob.entrySet()) {
                                String uniqueJobNameInCluster = this.makeClusterUniqueName(jobRow.getValue().getMetadata().getNamespace(), jobRow.getValue().getMetadata().getName());
                                if (MapUtils.getObject(tempPodMap, uniqueJobNameInCluster, null) != null) {
                                    // 1-3. 해당 Pod 목록을 Current CronJob 이름으로 Map에 저장..
                                    if (MapUtils.getObject(podMap, uniqueCronJobNameInCluster, null) == null) {
                                        podMap.put(uniqueCronJobNameInCluster, Lists.newArrayList());
                                    }
                                    podMap.get(uniqueCronJobNameInCluster).addAll(MapUtils.getObject(tempPodMap, uniqueJobNameInCluster, Lists.newArrayList()));
                                }
                            }
                        }
                    }
                }

                // 2. CronJob을 맵으로 변환 : Key는 Namespace+CronJob 이름
                this.getCronJobV1ToMap(cronJobsV1, cronJobMapV1);
            }
        }


        // Cocktail 라벨 제거 모든 서비스를 대상으로 조회하도록 변경
        List<V1Service> services = k8sWorker.getServicesV1(cluster, cluster.getNamespaceName(), null, null);

        /**
         * 3.5.0 : 2019.09.27 : Cluster 전체 조회일 경우 Cocktail에서 관리되지 않는 리소스도 목록에 추가..
         * 4.0.0 : 2019.12.04 : Cocktail에서 관리되지 않는 리소스를 포함 모든 리소스를 조회할 수 있도록 수정
         */
        String resourcePrefixSource = ResourceUtil.getResourcePrefix();

        //  4.0.0 - 2019.12.04 : 무조건 k8s rosource로 처리하도록 함.
        /** Deployment List중 Cocktail에서 관리되지 않는 리소스를 찾아 Component에 삽입 **/
        Set<String> componentNames = Sets.newHashSet();
        for(V1Deployment deployment : deployments) {
            try {
                String findKey = this.makeClusterUniqueName(deployment.getMetadata().getNamespace(), deployment.getMetadata().getName());
                componentNames.add(findKey);

                ComponentVO compo = this.addComponentDefault(cluster, deployment.getMetadata().getNamespace(), WorkloadType.REPLICA_SERVER, resourcePrefixSource, servicemapNamespaceMap.get(deployment.getMetadata().getNamespace()), deployment);
                if (canActualizeState) {
                    this.actualizeDeploymentState(cluster, compo, deploymentMap, Collections.emptyMap(), podMap, services, eventMap.get(K8sApiKindType.POD.getValue()), useAdditionalInfo, compoDao);
                }
                componentDetails.getComponents().add(compo);
            }
            catch (Exception ex) {
                if(log.isDebugEnabled()) {
                    log.error(ExceptionUtils.getStackTrace(ex));
                }
                log.warn(String.format("An error occurred while adding a component in cluster. : workloadType(%s), namespace(%s), name(%s)",
                    WorkloadType.REPLICA_SERVER.getCode(), deployment.getMetadata().getNamespace(), deployment.getMetadata().getName()));
            }
        }

        /** StatefulSet List중 Cocktail에서 관리되지 않는 리소스를 찾아 Component에 삽입 **/
        for(V1StatefulSet statefulSet : statefulSets) {
            try {
                String findKey = this.makeClusterUniqueName(statefulSet.getMetadata().getNamespace(), statefulSet.getMetadata().getName());
                componentNames.add(findKey);

                ComponentVO compo = this.addComponentDefault(cluster, statefulSet.getMetadata().getNamespace(), WorkloadType.STATEFUL_SET_SERVER, resourcePrefixSource, servicemapNamespaceMap.get(statefulSet.getMetadata().getNamespace()), statefulSet);
                if (canActualizeState) {
                    this.actualizeDeploymentState(cluster, compo, statefulSetMap, Collections.emptyMap(), podMap, services, eventMap.get(K8sApiKindType.POD.getValue()), useAdditionalInfo, compoDao);
                }
                componentDetails.getComponents().add(compo);
            }
            catch (Exception ex) {
                if(log.isDebugEnabled()) {
                    log.error(ExceptionUtils.getStackTrace(ex));
                }
                log.warn(String.format("An error occurred while adding a component in cluster. : workloadType(%s), namespace(%s), name(%s)",
                    WorkloadType.STATEFUL_SET_SERVER.getCode(), statefulSet.getMetadata().getNamespace(), statefulSet.getMetadata().getName()));
            }
        }

        /** DaemonSet List중 Cocktail에서 관리되지 않는 리소스를 찾아 Component에 삽입 **/
        for(V1DaemonSet daemonSet : daemonSets) {
            try {
                String findKey = this.makeClusterUniqueName(daemonSet.getMetadata().getNamespace(), daemonSet.getMetadata().getName());
                componentNames.add(findKey);

                ComponentVO compo = this.addComponentDefault(cluster, daemonSet.getMetadata().getNamespace(), WorkloadType.DAEMON_SET_SERVER, resourcePrefixSource, servicemapNamespaceMap.get(daemonSet.getMetadata().getNamespace()), daemonSet);
                if (canActualizeState) {
                    this.actualizeDeploymentState(cluster, compo, daemonSetMap, Collections.emptyMap(), podMap, services, eventMap.get(K8sApiKindType.POD.getValue()), useAdditionalInfo, compoDao);
                }
                componentDetails.getComponents().add(compo);
            }
            catch (Exception ex) {
                if(log.isDebugEnabled()) {
                    log.error(ExceptionUtils.getStackTrace(ex));
                }
                log.warn(String.format("An error occurred while adding a component in cluster. : workloadType(%s), namespace(%s), name(%s)",
                    WorkloadType.DAEMON_SET_SERVER.getCode(), daemonSet.getMetadata().getNamespace(), daemonSet.getMetadata().getName()), ex);
            }

        }

        /** Job List중 Cocktail에서 관리되지 않는 리소스를 찾아 Component에 삽입 **/
        for(V1Job job : jobs) {
            try {
                String findKey = this.makeClusterUniqueName(job.getMetadata().getNamespace(), job.getMetadata().getName());
                componentNames.add(findKey);

                ComponentVO compo = this.addComponentDefault(cluster, job.getMetadata().getNamespace(), WorkloadType.JOB_SERVER, resourcePrefixSource, servicemapNamespaceMap.get(job.getMetadata().getNamespace()), job);
                if (canActualizeState) {
                    this.actualizeDeploymentState(cluster, compo, jobMap, Collections.emptyMap(), podMap, services, eventMap.get(K8sApiKindType.POD.getValue()), useAdditionalInfo, compoDao);
                }
                componentDetails.getComponents().add(compo);
            }
            catch (Exception ex) {
                if(log.isDebugEnabled()) {
                    log.error(ExceptionUtils.getStackTrace(ex));
                }
                log.warn(String.format("An error occurred while adding a component in cluster. : workloadType(%s), namespace(%s), name(%s)",
                    WorkloadType.JOB_SERVER.getCode(), job.getMetadata().getNamespace(), job.getMetadata().getName()), ex);
            }
        }

        /** CronJob List중 Cocktail에서 관리되지 않는 리소스를 찾아 Component에 삽입 **/
        if (CollectionUtils.isNotEmpty(cronJobsV1beta1)) {
            for (V1beta1CronJob cronJob : cronJobsV1beta1) {
                try {
                    String findKey = this.makeClusterUniqueName(cronJob.getMetadata().getNamespace(), cronJob.getMetadata().getName());
                    componentNames.add(findKey);

                    ComponentVO compo = this.addComponentDefault(cluster, cronJob.getMetadata().getNamespace(), WorkloadType.CRON_JOB_SERVER, resourcePrefixSource, servicemapNamespaceMap.get(cronJob.getMetadata().getNamespace()), cronJob);
                    if (canActualizeState) {
                        this.actualizeDeploymentState(cluster, compo, cronJobMapV1beta1, jobMapInCronJob, podMap, services, eventMap.get(K8sApiKindType.POD.getValue()), useAdditionalInfo, compoDao);
                    }
                    componentDetails.getComponents().add(compo);
                } catch (Exception ex) {
                    if (log.isDebugEnabled()) {
                        log.error(ExceptionUtils.getStackTrace(ex));
                    }
                    log.warn(String.format("An error occurred while adding a component in cluster. : workloadType(%s), namespace(%s), name(%s)",
                            WorkloadType.CRON_JOB_SERVER.getCode(), cronJob.getMetadata().getNamespace(), cronJob.getMetadata().getName()), ex);
                }
            }
        } else if (CollectionUtils.isNotEmpty(cronJobsV1)) {
            for (V1CronJob cronJob : cronJobsV1) {
                try {
                    String findKey = this.makeClusterUniqueName(cronJob.getMetadata().getNamespace(), cronJob.getMetadata().getName());
                    componentNames.add(findKey);

                    ComponentVO compo = this.addComponentDefault(cluster, cronJob.getMetadata().getNamespace(), WorkloadType.CRON_JOB_SERVER, resourcePrefixSource, servicemapNamespaceMap.get(cronJob.getMetadata().getNamespace()), cronJob);
                    if (canActualizeState) {
                        this.actualizeDeploymentState(cluster, compo, cronJobMapV1, jobMapInCronJob, podMap, services, eventMap.get(K8sApiKindType.POD.getValue()), useAdditionalInfo, compoDao);
                    }
                    componentDetails.getComponents().add(compo);
                } catch (Exception ex) {
                    if (log.isDebugEnabled()) {
                        log.error(ExceptionUtils.getStackTrace(ex));
                    }
                    log.warn(String.format("An error occurred while adding a component in cluster. : workloadType(%s), namespace(%s), name(%s)",
                            WorkloadType.CRON_JOB_SERVER.getCode(), cronJob.getMetadata().getNamespace(), cronJob.getMetadata().getName()), ex);
                }
            }
        }

        /** yaml 데이터 기반의 각 컴포넌트의 template labels 설정 & names 셋팅
         *  2019/12/13 추가 & 기존 names 설정 수정, coolingi
         *  기존 name 설정은 name으로만 조회해서 cluster 전체 조회시에는 중복될 수 있음
         **/
        if (CollectionUtils.isNotEmpty(componentVOList)) {
            for (ComponentVO component : componentVOList) {
                String uniqueComponentNameInCluster = this.makeClusterUniqueName(component.getNamespaceName(), component.getComponentName());
//                componentNames.add(uniqueComponentNameInCluster);
                if (componentNames.contains(uniqueComponentNameInCluster)) { // 기존 목록에 존재하면 Skip...
                    continue;
                }
                component.setUpdater(context.getUserSeq());

                if (canActualizeState) {
                    // yaml 데이터 존재시 service match 위한 template labels 추출
                    if(component.getWorkloadManifest() != null){
//                    Map<String, String> templateLabels = this.getTemplateLabels(component.getWorkloadManifest()); // 2020.02.04 Group 정보 설정을 위해 Annotation도 추가 조회.. //redion
                        Map<String, Map<String, String>> labelsAndAnnotations = this.getAllLabelsAndAnnotations(component.getWorkloadManifest());
                        if(MapUtils.isNotEmpty(labelsAndAnnotations)) { // 설정한 값들은 아래 actualizeDeploymentState에서 사용..
                            component.setTemplateLabels(labelsAndAnnotations.get("templateLabels"));
                            component.setAnnotations(labelsAndAnnotations.get(KubeConstants.META_ANNOTATIONS));

                            if (MapUtils.isNotEmpty(component.getAnnotations())) {
                                String cocktailDeployDateTime = MapUtils.getString(component.getAnnotations(), KubeConstants.ANNOTATION_COCKTAIL_DEPLOY_DATETIME, null);
                                if (cocktailDeployDateTime != null) {
                                    component.setCreationTimestamp(Utils.toOffsetDateTime(cocktailDeployDateTime));
                                }
                            }
                        }
                    }

                    // actualizeDeploymentState 메서드내에서 하는일이 많다.
                    // k8s조회, 상태셋팅, DB 상태 update, component에 서비스 셋팅
                    WorkloadType workloadType = WorkloadType.valueOf(component.getWorkloadType());
                    K8sApiVerKindType apiVerKindType = ResourceUtil.getApiVerKindType(cluster, workloadType.getK8sApiKindType());

                    if (apiVerKindType != null) {
                        switch (workloadType) {
                            case SINGLE_SERVER:
                            case REPLICA_SERVER:
                                this.actualizeDeploymentState(cluster, component, deploymentMap, Collections.emptyMap(), podMap, services, eventMap.get(K8sApiKindType.POD.getValue()), useAdditionalInfo, compoDao);
                                break;
                            case STATEFUL_SET_SERVER:
                                this.actualizeDeploymentState(cluster, component, statefulSetMap, Collections.emptyMap(), podMap, services, eventMap.get(K8sApiKindType.POD.getValue()), useAdditionalInfo, compoDao);
                                break;
                            case DAEMON_SET_SERVER:
                                this.actualizeDeploymentState(cluster, component, daemonSetMap, Collections.emptyMap(), podMap, services, eventMap.get(K8sApiKindType.POD.getValue()), useAdditionalInfo, compoDao);
                                break;
                            case JOB_SERVER:
                                this.actualizeDeploymentState(cluster, component, jobMap, Collections.emptyMap(), podMap, services, eventMap.get(K8sApiKindType.POD.getValue()), useAdditionalInfo, compoDao);
                                break;
                            case CRON_JOB_SERVER:
                                if (apiVerKindType.getGroupType() == K8sApiGroupType.BATCH && apiVerKindType.getApiType() == K8sApiType.V1BETA1) {
                                    this.actualizeDeploymentState(cluster, component, cronJobMapV1beta1, jobMapInCronJob, podMap, services, eventMap.get(K8sApiKindType.POD.getValue()), useAdditionalInfo, compoDao);
                                } else if (apiVerKindType.getGroupType() == K8sApiGroupType.BATCH && apiVerKindType.getApiType() == K8sApiType.V1) {
                                    this.actualizeDeploymentState(cluster, component, cronJobMapV1, jobMapInCronJob, podMap, services, eventMap.get(K8sApiKindType.POD.getValue()), useAdditionalInfo, compoDao);
                                }
                                break;
                        }
                    }
                }

                if (StateCode.valueOf(component.getStateCode()) == StateCode.STOPPED && StringUtils.isBlank(component.getWorkloadManifest())) {
                    continue;
                } else {
                    component.setWorkloadManifest(null); // workloadManifest는 Stop된 워크로드의 상태 확인용도로 사용하고 응답 데이터에는 포함하지 않음..
                    componentDetails.getComponents().add(component);
                }
            }
        }

        return componentDetails;
    }

    protected ComponentVO addComponentDefault(ClusterVO cluster, String namespace, WorkloadType workloadType, String resourcePrefixSource, ServicemapSummaryVO servicemapInfo, Object obj) throws Exception {
	    return this.addComponentDefault(cluster, namespace, workloadType, resourcePrefixSource, servicemapInfo, obj, null);
    }

    protected ComponentVO addComponentDefault(ClusterVO cluster, String namespace, WorkloadType workloadType, String resourcePrefixSource, ServicemapSummaryVO servicemapInfo, Object obj, ComponentVO component) throws Exception {
	    if(component == null) {
            component = new ComponentVO();
            component.setComponentSeq(null);
            component.setWorkloadType(workloadType.getCode());
            component.setWorkloadVersion(WorkloadVersion.V1.getCode());
            component.setComponentType(ComponentType.CSERVER.getCode());
        } else {
            if (WorkloadType.valueOf(component.getWorkloadType()) != workloadType) {
                component.setWorkloadType(workloadType.getCode());
            }
        }
        component.setClusterSeq(cluster.getClusterSeq());
        component.setClusterName(cluster.getClusterName());
        component.setNamespaceName(namespace);
        if (servicemapInfo != null) {
            component.setServicemapInfo(servicemapInfo);
        }

        Map<String, String> labelMap = null;
        boolean isBase64 = false;
        String cocktailDeployDateTime = null;
	    switch (workloadType) {
            case SINGLE_SERVER:
            case REPLICA_SERVER:
                K8sApiVerKindType deploymentType = ResourceUtil.getApiVerKindType(cluster, K8sApiKindType.DEPLOYMENT);

                // Deployment
                if (deploymentType != null) {
                    if (deploymentType.getGroupType() == K8sApiGroupType.APPS && deploymentType.getApiType() == K8sApiType.V1) {
                        component.setComponentName(((V1Deployment)obj).getMetadata().getName());
                        component.setNamespaceName(((V1Deployment)obj).getMetadata().getNamespace());
                        component.setDescription(serverConversionService.getDescription(((V1Deployment)obj).getMetadata().getAnnotations()));
                        cocktailDeployDateTime = MapUtils.getString(((V1Deployment)obj).getMetadata().getAnnotations(), KubeConstants.ANNOTATION_COCKTAIL_DEPLOY_DATETIME, null);
                        if (cocktailDeployDateTime != null) {
                            component.setCreationTimestamp(Utils.toOffsetDateTime(cocktailDeployDateTime));
                        } else {
                            component.setCreationTimestamp(((V1Deployment) obj).getMetadata().getCreationTimestamp());
                        }
                        labelMap = Optional.ofNullable(((V1Deployment) obj).getMetadata())
                                .map(V1ObjectMeta::getLabels)
                                .orElseGet(() ->null);
                    }
                }

                break;
            case STATEFUL_SET_SERVER:
                K8sApiVerKindType statefulSetType = ResourceUtil.getApiVerKindType(cluster, K8sApiKindType.STATEFUL_SET);

                // StatefulSet
                if (statefulSetType != null) {
                    if (statefulSetType.getGroupType() == K8sApiGroupType.APPS && statefulSetType.getApiType() == K8sApiType.V1) {
                        component.setComponentName(((V1StatefulSet)obj).getMetadata().getName());
                        component.setNamespaceName(((V1StatefulSet)obj).getMetadata().getNamespace());
                        component.setDescription(serverConversionService.getDescription(((V1StatefulSet)obj).getMetadata().getAnnotations()));
                        cocktailDeployDateTime = MapUtils.getString(((V1StatefulSet)obj).getMetadata().getAnnotations(), KubeConstants.ANNOTATION_COCKTAIL_DEPLOY_DATETIME, null);
                        if (cocktailDeployDateTime != null) {
                            component.setCreationTimestamp(Utils.toOffsetDateTime(cocktailDeployDateTime));
                        } else {
                            component.setCreationTimestamp(((V1StatefulSet) obj).getMetadata().getCreationTimestamp());
                        }
                        labelMap = Optional.ofNullable(((V1StatefulSet) obj).getMetadata())
                                .map(V1ObjectMeta::getLabels)
                                .orElseGet(() ->null);
                    }
                }

                break;
            case DAEMON_SET_SERVER:
                K8sApiVerKindType daemonSetType = ResourceUtil.getApiVerKindType(cluster, K8sApiKindType.DAEMON_SET);

                if (daemonSetType != null) {
                    if (daemonSetType.getGroupType() == K8sApiGroupType.APPS && daemonSetType.getApiType() == K8sApiType.V1) {
                        component.setComponentName(((V1DaemonSet)obj).getMetadata().getName());
                        component.setNamespaceName(((V1DaemonSet)obj).getMetadata().getNamespace());
                        component.setDescription(serverConversionService.getDescription(((V1DaemonSet)obj).getMetadata().getAnnotations()));
                        cocktailDeployDateTime = MapUtils.getString(((V1DaemonSet)obj).getMetadata().getAnnotations(), KubeConstants.ANNOTATION_COCKTAIL_DEPLOY_DATETIME, null);
                        if (cocktailDeployDateTime != null) {
                            component.setCreationTimestamp(Utils.toOffsetDateTime(cocktailDeployDateTime));
                        } else {
                            component.setCreationTimestamp(((V1DaemonSet) obj).getMetadata().getCreationTimestamp());
                        }
                        labelMap = Optional.ofNullable(((V1DaemonSet) obj).getMetadata())
                                .map(V1ObjectMeta::getLabels)
                                .orElseGet(() ->null);
                    }
                }

                break;
            case JOB_SERVER:
                K8sApiVerKindType jobType = ResourceUtil.getApiVerKindType(cluster, K8sApiKindType.JOB);

                // Job
                if (jobType != null) {
                    if (jobType.getGroupType() == K8sApiGroupType.BATCH && jobType.getApiType() == K8sApiType.V1) {
                        component.setComponentName(((V1Job)obj).getMetadata().getName());
                        component.setNamespaceName(((V1Job)obj).getMetadata().getNamespace());
                        component.setDescription(serverConversionService.getDescription(((V1Job)obj).getMetadata().getAnnotations()));
                        cocktailDeployDateTime = MapUtils.getString(((V1Job)obj).getMetadata().getAnnotations(), KubeConstants.ANNOTATION_COCKTAIL_DEPLOY_DATETIME, null);
                        if (cocktailDeployDateTime != null) {
                            component.setCreationTimestamp(Utils.toOffsetDateTime(cocktailDeployDateTime));
                        } else {
                            component.setCreationTimestamp(((V1Job) obj).getMetadata().getCreationTimestamp());
                        }
                        labelMap = Optional.ofNullable(((V1Job) obj).getMetadata())
                                .map(V1ObjectMeta::getLabels)
                                .orElseGet(() ->null);
                    }
                }

                break;
            case CRON_JOB_SERVER:
                K8sApiVerKindType cronType = ResourceUtil.getApiVerKindType(cluster, K8sApiKindType.CRON_JOB);

                if (cronType != null) {
                    if (cronType.getGroupType() == K8sApiGroupType.BATCH && cronType.getApiType() == K8sApiType.V1BETA1) {
                        component.setComponentName(((V1beta1CronJob)obj).getMetadata().getName());
                        component.setNamespaceName(((V1beta1CronJob)obj).getMetadata().getNamespace());
                        component.setDescription(serverConversionService.getDescription(((V1beta1CronJob)obj).getMetadata().getAnnotations()));
                        cocktailDeployDateTime = MapUtils.getString(((V1beta1CronJob)obj).getMetadata().getAnnotations(), KubeConstants.ANNOTATION_COCKTAIL_DEPLOY_DATETIME, null);
                        if (cocktailDeployDateTime != null) {
                            component.setCreationTimestamp(Utils.toOffsetDateTime(cocktailDeployDateTime));
                        } else {
                            component.setCreationTimestamp(((V1beta1CronJob) obj).getMetadata().getCreationTimestamp());
                        }
                        labelMap = Optional.ofNullable(((V1beta1CronJob) obj).getMetadata())
                                .map(V1ObjectMeta::getLabels)
                                .orElseGet(() ->null);
                    } else if (cronType.getGroupType() == K8sApiGroupType.BATCH && cronType.getApiType() == K8sApiType.V1) {
                        component.setComponentName(((V1CronJob)obj).getMetadata().getName());
                        component.setNamespaceName(((V1CronJob)obj).getMetadata().getNamespace());
                        component.setDescription(serverConversionService.getDescription(((V1CronJob)obj).getMetadata().getAnnotations()));
                        cocktailDeployDateTime = MapUtils.getString(((V1CronJob)obj).getMetadata().getAnnotations(), KubeConstants.ANNOTATION_COCKTAIL_DEPLOY_DATETIME, null);
                        if (cocktailDeployDateTime != null) {
                            component.setCreationTimestamp(Utils.toOffsetDateTime(cocktailDeployDateTime));
                        } else {
                            component.setCreationTimestamp(((V1CronJob) obj).getMetadata().getCreationTimestamp());
                        }
                        labelMap = Optional.ofNullable(((V1CronJob) obj).getMetadata())
                                .map(V1ObjectMeta::getLabels)
                                .orElseGet(() ->null);
                    }
                }

                break;
        }

        return component;
    }

//    public void actualizeDeploymentState(ComponentVO component,
//                                            Map<String, V1Deployment> deploymentMap,
//                                            Map<String, V1StatefulSet> statefulSetMap,
//                                            Map<String, V1DaemonSet> daemonSetMap,
//                                            Map<String, V1Job> jobMap,
//                                            Map<String, V1beta1CronJob> cronJobMap,
//                                            Map<String, Map<String, V1Job>> jobMapInCronJob,
//                                            Map<String, List<V1Pod>> podMap,
//                                            List<V1Service> services,
//                                            boolean useAdditionalInfo,
//                                            IComponentMapper compoDao) throws Exception {
//	    this.actualizeDeploymentState(component,
//                                        deploymentMap,
//                                        statefulSetMap,
//                                        daemonSetMap,
//                                        jobMap,
//                                        cronJobMap,
//                                        jobMapInCronJob,
//                                        podMap,
//                                        services,
//                                        null,
//                                        useAdditionalInfo,
//                                        compoDao);
//    }
//
//    /**
//     * component 와 서비스 매칭, cluster 조회해 현재 상태 셋팅 및 update
//     *
//     * @param component
//     * @param deploymentMap
//     * @param statefulSetMap
//     * @param daemonSetMap
//     * @param jobMap
//     * @param cronJobMap
//     * @param jobMapInCronJob
//     * @param podMap
//     * @param services
//     * @param podEventMap
//     * @param useAdditionalInfo
//     * @param compoDao
//     * @throws Exception
//     */
//    public void actualizeDeploymentState(ComponentVO component,
//                                              Map<String, V1Deployment> deploymentMap,
//                                              Map<String, V1StatefulSet> statefulSetMap,
//                                              Map<String, V1DaemonSet> daemonSetMap,
//                                              Map<String, V1Job> jobMap,
//                                              Map<String, V1beta1CronJob> cronJobMap,
//                                              Map<String, Map<String, V1Job>> jobMapInCronJob,
//                                              Map<String, List<V1Pod>> podMap,
//                                              List<V1Service> services,
//                                              Map<String, List<K8sEventVO>> podEventMap,
//                                              boolean useAdditionalInfo,
//                                            IComponentMapper compoDao) throws Exception {
//        Integer componentSeq = component.getComponentSeq();
//        /** 3.5.0 :2019.09.27 : Cluster 전체에서 조회시 중복 제거를 위해 key로 사용되는 Name을 "namespace+name" 으로 변경 **/
//        String name = this.makeClusterUniqueName(component.getNamespaceName(), component.getComponentName());
//        boolean isManaged = (componentSeq != null);
//
//        boolean k8sResourceExists = false;
//
//        /** 3.5.0 : 2019.09.27 : Cocktail에서 관리되지 않는 경우 currState를 배포정보로부터 수집하고, 알 수 없는 경우 UNKNOWN **/
//        StateCode currState = StateCode.STOPPED;
//
//        switch (WorkloadType.valueOf(component.getWorkloadType())) {
//            case SINGLE_SERVER:
//            case REPLICA_SERVER:
//                if (MapUtils.getObject(deploymentMap, name, null) != null) {
//                    currState = this.getDeploymentStateV2(deploymentMap.get(name).getStatus());
//                }
//                break;
//            case STATEFUL_SET_SERVER:
//                if (MapUtils.getObject(statefulSetMap, name, null) != null) {
//                    currState = this.getStatefulSetState(statefulSetMap.get(name).getStatus());
//                }
//                break;
//        }
//
//        StateCode newState = currState;
//        String detail = "";
//        stateLogger.debug("Component state check: name - {}, current state - {}", name, currState);
//
//        WorkloadType workloadType = WorkloadType.valueOf(component.getWorkloadType());
//        switch (workloadType) {
//            case SINGLE_SERVER:
//            case REPLICA_SERVER: {
//                if (deploymentMap.containsKey(name)) {
//                    k8sResourceExists = true;
//                    V1DeploymentStatus status = deploymentMap.get(name).getStatus();
//                    if (status == null || status.getConditions() == null || status.getConditions().size() == 0) { // 상태 파악 불가. 현재 상태 유지
//                        stateLogger.debug("Deployment status or conditions is empty");
//                        component.setActiveCount(0);
//                    }else {
//                        if (currState != StateCode.STOPPING) {
//                            component.setComputeTotal(Optional.ofNullable(deploymentMap.get(name))
//                                .map(V1Deployment::getSpec)
//                                .map(V1DeploymentSpec::getReplicas)
//                                .orElseGet(() ->0));
//
//                            StateCode deploymentState = this.getDeploymentStateV2(status);
//                            stateLogger.debug("Deployment state: {}", deploymentState);
//                            stateLogger.debug("Label: {}", name);
//
//                            StateAndCount sc = this.getPodStateV2(podMap.get(name));
//                            stateLogger.debug("Deployment [{}] - Pod state: {}, count: {}", name, sc.state, sc.count);
//                            component.setActiveCount(sc.count);
//                            stateLogger.debug("Deployment [{}] - Active count: {}", name, component.getActiveCount());
//
//                            if (deploymentState == StateCode.RUNNING &&
//                                sc.state == StateCode.RUNNING) {
//                                newState = StateCode.RUNNING;
//                            } else if (deploymentState != StateCode.RUNNING) {
//                                newState = deploymentState;
//                            } else {
//                                newState = sc.state;
//                            }
//                        }
//                    }
//                    /** K8s에서 조회한 CreationTimeStamp를 사용 **/
//                    /** hjchoi.20200318 addComponentDefault에서 셋팅함 **/
////                    component.setCreationTimestamp(Optional.ofNullable(deploymentMap.get(name))
////                        .map(V1Deployment::getMetadata)
////                        .map(V1ObjectMeta::getCreationTimestamp)
////                        .orElseGet(() ->null));
//
//                    /** Group 정보를 Annotation으로부터 조회하여 설정 **/
//                    Map<String, String> annotationsForGroup = Optional.ofNullable(deploymentMap.get(name))
//                        .map(V1Deployment::getMetadata)
//                        .map(V1ObjectMeta::getAnnotations)
//                        .orElseGet(() ->Maps.newHashMap());
//                    Integer groupSeq = ServerUtils.getInteger(annotationsForGroup.get(KubeConstants.ANNOTATION_COCKTAIL_GROUP_NO));
//                    component.setGroupSeq(groupSeq);
//                }
//                break;
//            }
//            case STATEFUL_SET_SERVER: {
//                if (statefulSetMap.containsKey(name)) {
//                    k8sResourceExists = true;
//                    V1StatefulSetStatus status = statefulSetMap.get(name).getStatus();
//                    if (status == null) { // 상태 파악 불가. 현재 상태 유지
//                        stateLogger.debug("StatefulSet status");
//                        component.setActiveCount(0);
//                    } else {
//                        if (currState != StateCode.STOPPING) {
//                            component.setComputeTotal(Optional.ofNullable(statefulSetMap.get(name))
//                                .map(V1StatefulSet::getSpec)
//                                .map(V1StatefulSetSpec::getReplicas)
//                                .orElseGet(() ->0));
//
//                            StateCode statefulSetState = this.getStatefulSetState(status);
//                            stateLogger.debug("StatefulSet state: {}", statefulSetState);
//                            stateLogger.debug("Label: {}", name);
//
//                            StateAndCount sc = this.getPodStateV2(podMap.get(name));
//                            stateLogger.debug("StatefulSet [{}] - Pod state: {}, count: {}", name, sc.state, sc.count);
//                            component.setActiveCount(sc.count);
//                            stateLogger.debug("StatefulSet [{}] - Active count: {}", name, component.getActiveCount());
//
//                            if (statefulSetState == StateCode.RUNNING &&
//                                sc.state == StateCode.RUNNING) {
//                                newState = StateCode.RUNNING;
//                            } else if (statefulSetState != StateCode.RUNNING) {
//                                newState = statefulSetState;
//                            } else {
//                                newState = sc.state;
//                            }
//                        }
//                    }
//                    /** K8s에서 조회한 CreationTimeStamp를 사용 **/
//                    /** hjchoi.20200318 addComponentDefault에서 셋팅함 **/
////                    component.setCreationTimestamp(Optional.ofNullable(statefulSetMap.get(name))
////                        .map(V1StatefulSet::getMetadata)
////                        .map(V1ObjectMeta::getCreationTimestamp)
////                        .orElseGet(() ->null));
//
//                    /** Group 정보를 Annotation으로부터 조회하여 설정 **/
//                    Map<String, String> annotationsForGroup = Optional.ofNullable(statefulSetMap.get(name))
//                        .map(V1StatefulSet::getMetadata)
//                        .map(V1ObjectMeta::getAnnotations)
//                        .orElseGet(() ->Maps.newHashMap());
//                    Integer groupSeq = ServerUtils.getInteger(annotationsForGroup.get(KubeConstants.ANNOTATION_COCKTAIL_GROUP_NO));
//                    component.setGroupSeq(groupSeq);
//                }
//                break;
//            }
//            case DAEMON_SET_SERVER: {
//                if (daemonSetMap.containsKey(name)) {
//                    k8sResourceExists = true;
//                    V1DaemonSetStatus status = daemonSetMap.get(name).getStatus();
//                    if (status == null) { // 상태 파악 불가. 현재 상태 유지
//                        stateLogger.debug("DaemonSet status");
//                        component.setActiveCount(0);
//                    }else {
//                        if (currState != StateCode.STOPPING) {
//                            component.setComputeTotal(status.getDesiredNumberScheduled());
//
//                            StateAndCount sc = this.getPodStateV2(podMap.get(name));
//                            stateLogger.debug("DaemonSet [{}] - Pod state: {}, count: {}", name, sc.state, sc.count);
//                            component.setActiveCount(sc.count);
//                            stateLogger.debug("DaemonSet [{}] - Active count: {}", name, component.getActiveCount());
//
//                            if (sc.state == StateCode.RUNNING) {
//                                newState = StateCode.RUNNING;
//                            } else {
//                                newState = sc.state;
//                            }
//                        }
//                    }
//                    /** K8s에서 조회한 CreationTimeStamp를 사용 **/
//                    /** hjchoi.20200318 addComponentDefault에서 셋팅함 **/
////                    component.setCreationTimestamp(Optional.ofNullable(daemonSetMap.get(name))
////                        .map(V1DaemonSet::getMetadata)
////                        .map(V1ObjectMeta::getCreationTimestamp)
////                        .orElseGet(() ->null));
//
//                    /** Group 정보를 Annotation으로부터 조회하여 설정 **/
//                    Map<String, String> annotationsForGroup = Optional.ofNullable(daemonSetMap.get(name))
//                        .map(V1DaemonSet::getMetadata)
//                        .map(V1ObjectMeta::getAnnotations)
//                        .orElseGet(() ->Maps.newHashMap());
//                    Integer groupSeq = ServerUtils.getInteger(annotationsForGroup.get(KubeConstants.ANNOTATION_COCKTAIL_GROUP_NO));
//                    component.setGroupSeq(groupSeq);
//                }
//                break;
//            }
//            case JOB_SERVER: {
//                if (jobMap.containsKey(name)) {
//                    k8sResourceExists = true;
//                    V1JobStatus status = jobMap.get(name).getStatus();
//                    if (status == null) { // 상태 파악 불가. 현재 상태 유지
//                        stateLogger.debug("Job status");
//                        component.setActiveCount(0);
//                    }else {
//                        if (currState != StateCode.STOPPING) {
//                            component.setComputeTotal(Optional.ofNullable(jobMap.get(name))
//                                .map(V1Job::getSpec)
//                                .map(V1JobSpec::getCompletions)
//                                .orElseGet(() ->0));
//                            StateCode jobState = this.getJobState(status);
//                            stateLogger.debug("Job state: {}", jobState);
//                            stateLogger.debug("Label: {}", name);
//
//                            int s = status.getSucceeded() != null ? status.getSucceeded() : 0;
//                            int f = status.getFailed() != null ? status.getFailed() : 0;
//                            int a = status.getActive() != null ? status.getActive() : 0;
//
//                            component.setActiveCount(s + f + a);
//                            stateLogger.debug("Job [{}] - Active count: {}", name, component.getActiveCount());
//
//                            newState = jobState;
//                        }
//                    }
//                    /** K8s에서 조회한 CreationTimeStamp를 사용 **/
//                    /** hjchoi.20200318 addComponentDefault에서 셋팅함 **/
////                    component.setCreationTimestamp(Optional.ofNullable(jobMap.get(name))
////                        .map(V1Job::getMetadata)
////                        .map(V1ObjectMeta::getCreationTimestamp)
////                        .orElseGet(() ->null));
//
//                    /** Group 정보를 Annotation으로부터 조회하여 설정 **/
//                    Map<String, String> annotationsForGroup = Optional.ofNullable(jobMap.get(name))
//                        .map(V1Job::getMetadata)
//                        .map(V1ObjectMeta::getAnnotations)
//                        .orElseGet(() ->Maps.newHashMap());
//                    Integer groupSeq = ServerUtils.getInteger(annotationsForGroup.get(KubeConstants.ANNOTATION_COCKTAIL_GROUP_NO));
//                    component.setGroupSeq(groupSeq);
//                }
//                break;
//            }
//            case CRON_JOB_SERVER: {
//                if (cronJobMap.containsKey(name)) {
//                    k8sResourceExists = true;
//                    V1beta1CronJobStatus status = cronJobMap.get(name).getStatus();
//                    if (status == null) { // 상태 파악 불가. 현재 상태 유지
//                        stateLogger.debug("CronJob status");
//                        component.setActiveCount(0);
//                    }else {
//                        if (currState != StateCode.STOPPING) {
//                            component.setComputeTotal(Optional.ofNullable(cronJobMap.get(name))
//                                .map(V1beta1CronJob::getSpec)
//                                .map(V1beta1CronJobSpec::getJobTemplate)
//                                .map(V1beta1JobTemplateSpec::getSpec)
//                                .map(V1JobSpec::getCompletions)
//                                .orElseGet(() ->0));
//                            StateCode jobStateCode;
//                            if (CollectionUtils.isNotEmpty(status.getActive())
//                                && CollectionUtils.size(status.getActive()) > 0) {
//                                jobStateCode = StateCode.RUNNING;
//
//                                if(MapUtils.isNotEmpty(jobMapInCronJob)) {
//                                    for (V1ObjectReference activeRow : status.getActive()) {
//                                        log.debug("###############" + JsonUtils.toGson(activeRow));
//                                        // String jobName = this.makeClusterUniqueName(cronJobMap.get(name).getMetadata().getNamespace(), activeRow.getName());
//                                        // jobMapInCronJob -> Map<ownerName, Map<jobName, job>>
//                                        Map<String, V1Job> jobsInCronJob = jobMapInCronJob.get(name);
//                                        V1JobStatus jobStatus = null;
//                                        if(MapUtils.isNotEmpty(jobsInCronJob)) {
//                                            jobStatus = Optional.ofNullable(jobsInCronJob.get(activeRow.getName())).map(V1Job::getStatus).orElseGet(() ->null);
//                                        }
//                                        jobStateCode = this.getJobState(jobStatus);
//                                        stateLogger.debug("Job state: {}", jobStateCode);
//                                        stateLogger.debug("Label: {}", name);
//
//                                        int s = jobStatus == null ? 0 : (jobStatus.getSucceeded() != null ? jobStatus.getSucceeded() : 0);
//                                        int f = jobStatus == null ? 0 : (jobStatus.getFailed() != null ? jobStatus.getFailed() : 0);
//                                        int a = jobStatus == null ? 0 : (jobStatus.getActive() != null ? jobStatus.getActive() : 0);
//
//                                        component.setActiveCount(s + f + a);
//                                        stateLogger.debug("Job [{}] - Active count: {}", name, component.getActiveCount());
//                                    }
//                                }
//                            } else {
//                                jobStateCode = StateCode.READY;
//                            }
//
//                            newState = jobStateCode;
//
//                        }
//                    }
//
//                    /** Group 정보를 Annotation으로부터 조회하여 설정 **/
//                    Map<String, String> annotationsForGroup = Optional.ofNullable(cronJobMap.get(name))
//                        .map(V1beta1CronJob::getMetadata)
//                        .map(V1ObjectMeta::getAnnotations)
//                        .orElseGet(() ->Maps.newHashMap());
//                    Integer groupSeq = ServerUtils.getInteger(annotationsForGroup.get(KubeConstants.ANNOTATION_COCKTAIL_GROUP_NO));
//                    component.setGroupSeq(groupSeq);
//                }
//                break;
//            }
//            default:
//                break;
//        }
//
//        component.setIsK8sResourceExist(k8sResourceExists);
//        if (!k8sResourceExists) { // Component 정보는 있는데 대응하는 K8S workload가 없는 경우
////            stateLogger.warn("Component '{}' dose not have K8S workload", componentSeq);
//            stateLogger.debug(" > current state: {}", currState);
//            if (currState == StateCode.STOPPED) {
//                component.setActiveCount(0);
//
//                // 중지된 workload라도 templateLabels 값이 있으면 서비스 매칭 시켜준다. 2019/12/13, coolingi
//                Map<String, String> podLabels = component.getTemplateLabels();
//                if(podLabels != null && services != null && services.size() > 0) {
//                    List<ServiceSpecGuiVO> serviceSpecs = this.getCouplingServices(services, component);
//                    if (CollectionUtils.isNotEmpty(serviceSpecs)) {
//                        component.setServices(serviceSpecs);
//                    }
//                }
//
//                // 중지된 workload라도 저장된 Manifest 정보에 Annotation 정보가 있으면 groupSeq를 설정. 2020/02/04, redion
//                Map<String, String> annotationsForGroup = Optional.ofNullable(component.getAnnotations()).orElseGet(() ->Maps.newHashMap());
//                Integer groupSeq = ServerUtils.getInteger(annotationsForGroup.get(KubeConstants.ANNOTATION_COCKTAIL_GROUP_NO));
//                component.setGroupSeq(groupSeq);
//
//                return;
//            }
//            if (currState == StateCode.CREATING
//                    || currState == StateCode.UPDATING
//                    || currState == StateCode.DEPLOYING
//                    || currState == StateCode.READY
//                    || currState == StateCode.RUNNING_PREPARE
//                    || currState == StateCode.RUNNING
//                    || currState == StateCode.COMPLETED
//                    || currState == StateCode.FAILED
//            ) {
//                if(isManaged) {
//                    componentService.updateComponentState(component, StateCode.PENDING, "Server creating", compoDao);
//                }
//                newState = StateCode.PENDING;
//            } else if (currState == StateCode.DELETING
//                    || currState == StateCode.STOPPING) { // stopping -> workload 없음 -> stopped
//            } else if (currState != StateCode.PENDING
//                    && currState != StateCode.DELETED
//                    && currState != StateCode.ERROR) {
//                if(isManaged) {
//                    componentService.updateComponentState(component, StateCode.ERROR, "Component has not K8S workload", compoDao);
//                }
//                newState = StateCode.ERROR;
//            } else {
//                if(currState == StateCode.PENDING){
//                    if(isManaged) {
//                        componentService.updateComponentState(component, StateCode.STOPPED, "Server stop", compoDao);
//                    }
//                    newState = StateCode.STOPPED;
//                }
//            }
//            stateLogger.debug(" > new state: {}", newState);
//        }
//
//        if (useAdditionalInfo && k8sResourceExists && EnumSet.of(StateCode.RUNNING, StateCode.RUNNING_PREPARE, StateCode.READY, StateCode.FAILED, StateCode.COMPLETED).contains(newState)) {
//            Map<String, String> podLabels = new HashMap<>();
//            List<V1Container> initContainers = null;
//            List<V1Container> containers = null;
//            // 기존 selector의 matchlabels => podtemplate.metadata.labels로 변경, 2019/12/13, coolingi
//            // 실제 matchLabel과 pod label은 다를수 있음.
//            switch (workloadType) {
//                case SINGLE_SERVER:
//                case REPLICA_SERVER:
//                    V1Deployment currentDeployment = deploymentMap.get(name);
//                    containers = Optional.ofNullable(currentDeployment)
//                        .map(V1Deployment::getSpec)
//                        .map(V1DeploymentSpec::getTemplate)
//                        .map(V1PodTemplateSpec::getSpec)
//                        .map(V1PodSpec::getContainers)
//                        .orElseGet(() ->null);
//                    initContainers = Optional.ofNullable(currentDeployment)
//                        .map(V1Deployment::getSpec)
//                        .map(V1DeploymentSpec::getTemplate)
//                        .map(V1PodTemplateSpec::getSpec)
//                        .map(V1PodSpec::getInitContainers)
//                        .orElseGet(() ->null);
//                    podLabels = Optional.ofNullable(currentDeployment)
//                            .map(V1Deployment::getSpec)
//                            .map(V1DeploymentSpec::getTemplate)
//                            .map(V1PodTemplateSpec::getMetadata)
//                            .map(V1ObjectMeta::getLabels)
//                            .orElseGet(() ->null);
//                    break;
//                case STATEFUL_SET_SERVER:
//                    V1StatefulSet currentStatefulSet = statefulSetMap.get(name);
//                    containers = Optional.ofNullable(currentStatefulSet)
//                        .map(V1StatefulSet::getSpec)
//                        .map(V1StatefulSetSpec::getTemplate)
//                        .map(V1PodTemplateSpec::getSpec)
//                        .map(V1PodSpec::getContainers)
//                        .orElseGet(() ->null);
//                    initContainers = Optional.ofNullable(currentStatefulSet)
//                        .map(V1StatefulSet::getSpec)
//                        .map(V1StatefulSetSpec::getTemplate)
//                        .map(V1PodTemplateSpec::getSpec)
//                        .map(V1PodSpec::getInitContainers)
//                        .orElseGet(() ->null);
//                    podLabels = Optional.ofNullable(currentStatefulSet)
//                            .map(V1StatefulSet::getSpec)
//                            .map(V1StatefulSetSpec::getTemplate)
//                            .map(V1PodTemplateSpec::getMetadata)
//                            .map(V1ObjectMeta::getLabels)
//                            .orElseGet(() ->null);
//                    break;
//                case DAEMON_SET_SERVER:
//                    V1DaemonSet currentDaemonSet = daemonSetMap.get(name);
//                    containers = Optional.ofNullable(currentDaemonSet)
//                        .map(V1DaemonSet::getSpec)
//                        .map(V1DaemonSetSpec::getTemplate)
//                        .map(V1PodTemplateSpec::getSpec)
//                        .map(V1PodSpec::getContainers)
//                        .orElseGet(() ->null);
//                    initContainers = Optional.ofNullable(currentDaemonSet)
//                        .map(V1DaemonSet::getSpec)
//                        .map(V1DaemonSetSpec::getTemplate)
//                        .map(V1PodTemplateSpec::getSpec)
//                        .map(V1PodSpec::getInitContainers)
//                        .orElseGet(() ->null);
//                    podLabels = Optional.ofNullable(currentDaemonSet)
//                            .map(V1DaemonSet::getSpec)
//                            .map(V1DaemonSetSpec::getTemplate)
//                            .map(V1PodTemplateSpec::getMetadata)
//                            .map(V1ObjectMeta::getLabels)
//                            .orElseGet(() ->null);
//                    break;
//                case JOB_SERVER:
//                    V1Job currentJob = jobMap.get(name);
//                    containers = Optional.ofNullable(currentJob)
//                        .map(V1Job::getSpec)
//                        .map(V1JobSpec::getTemplate)
//                        .map(V1PodTemplateSpec::getSpec)
//                        .map(V1PodSpec::getContainers)
//                        .orElseGet(() ->null);
//                    initContainers = Optional.ofNullable(currentJob)
//                        .map(V1Job::getSpec)
//                        .map(V1JobSpec::getTemplate)
//                        .map(V1PodTemplateSpec::getSpec)
//                        .map(V1PodSpec::getInitContainers)
//                        .orElseGet(() ->null);
//                    podLabels = Optional.ofNullable(currentJob)
//                            .map(V1Job::getSpec)
//                            .map(V1JobSpec::getTemplate)
//                            .map(V1PodTemplateSpec::getMetadata)
//                            .map(V1ObjectMeta::getLabels)
//                            .orElseGet(() ->null);
//                    break;
//                case CRON_JOB_SERVER:
//                    V1beta1CronJob currentCronJob = cronJobMap.get(name);
//                    containers = Optional.ofNullable(currentCronJob)
//                        .map(V1beta1CronJob::getSpec)
//                        .map(V1beta1CronJobSpec::getJobTemplate)
//                        .map(V1beta1JobTemplateSpec::getSpec)
//                        .map(V1JobSpec::getTemplate)
//                        .map(V1PodTemplateSpec::getSpec)
//                        .map(V1PodSpec::getContainers)
//                        .orElseGet(() ->null);
//                    initContainers = Optional.ofNullable(currentCronJob)
//                        .map(V1beta1CronJob::getSpec)
//                        .map(V1beta1CronJobSpec::getJobTemplate)
//                        .map(V1beta1JobTemplateSpec::getSpec)
//                        .map(V1JobSpec::getTemplate)
//                        .map(V1PodTemplateSpec::getSpec)
//                        .map(V1PodSpec::getInitContainers)
//                        .orElseGet(() ->null);
//                    podLabels = Optional.ofNullable(currentCronJob)
//                            .map(V1beta1CronJob::getSpec)
//                            .map(V1beta1CronJobSpec::getJobTemplate)
//                            .map(V1beta1JobTemplateSpec::getSpec)
//                            .map(V1JobSpec::getTemplate)
//                            .map(V1PodTemplateSpec::getMetadata)
//                            .map(V1ObjectMeta::getLabels)
//                            .orElseGet(() ->null);
//                    break;
//            }
//
//            /** Container 리스트 구성 **/
//            component.setContainers(this.buildContainerList(containers, initContainers));
//
//            /** 실행되는 워크로드의 pod labels 셋팅, 2019/12/13, coolingi **/
//            if(podLabels != null) component.setTemplateLabels(podLabels);
//
//            /** Service 리스트 구성 **/
//            if(podLabels != null && services != null && services.size() > 0) {
//                List<ServiceSpecGuiVO> serviceSpecs = this.getCouplingServices(services, component);
//                if (serviceSpecs != null && serviceSpecs.size() >= 1) {
//                    component.setServices(serviceSpecs);
//                }
//            }
//
//            /**
//             * 3.5.0 : QoS 설정
//             * Pod가 여러개라도 하나의 Pod에서만 내용을 확인.
//             **/
//            List<V1Pod> pods = podMap.get(name);
//            if(pods != null && pods.size() > 0) {
//                component.setQos(Optional.ofNullable(pods.get(0))
//                    .map(V1Pod::getStatus)
//                    .map(V1PodStatus::getQosClass)
//                    .orElseGet(() ->null));
//
//                /**
//                 * 3.5.0 : Pod 목록 설정 : 2019.10.16 : ServiceSpec 상세에서 Pod목록이 필요..
//                 */
//                List<K8sPodVO> k8sPods = new ArrayList<>();
//                if(CollectionUtils.isNotEmpty(pods)) {
//                    JSON k8sJson = new JSON();
//                    for (V1Pod v1Pod : pods) {
//                        K8sPodVO k8sPod = workloadResourceService.genPodData(v1Pod, null, null, k8sJson, false);
//                        if (MapUtils.isNotEmpty(podEventMap)) {
//                            k8sPod.setEvents(podEventMap.get(k8sPod.getPodName()));
//                        }
//                        k8sPods.add(k8sPod);
//                    }
////                    k8sPods = workloadResourceService.genPodDataList(k8sPods, pods, null, null);
//                }
//
//                component.setPods(k8sPods);
//            }
//
//        }
//
//        // 3.5.0 : 2019.09.27 : Managed되는 컴포넌트일때만 update하도록 조건 추가.
////        if (isManaged && k8sResourceExists && currState != newState) {
////            componentService.updateComponentState(component, newState, detail, compoDao);
////        }
//
//        // 3.5.0 : 2019.09.27 : Cocktail에서 관리되지 않는 워크로드는 DB에 Default 상태가 없으로 상태값을 입력 해 주어야 함.
//        component.setStateCode(newState.getCode());
//        component.setStateDetail(detail);
//    }


    /**
     * component 와 서비스 매칭, cluster 조회해 현재 상태 셋팅 및 update
     *
     * @param cluster
     * @param component
     * @param workloadMap
     * @param jobMapInCronJob
     * @param podMap
     * @param services
     * @param podEventMap
     * @param useAdditionalInfo
     * @param compoDao
     * @throws Exception
     */
    public void actualizeDeploymentState(
            ClusterVO cluster,
            ComponentVO component,
            Map<String, ?> workloadMap,
            Map<String, Map<String, V1Job>> jobMapInCronJob,
            Map<String, List<V1Pod>> podMap,
            List<V1Service> services,
            Map<String, List<K8sEventVO>> podEventMap,
            boolean useAdditionalInfo,
            IComponentMapper compoDao
    ) throws Exception {
        if (cluster != null && component != null) {
            if (compoDao == null) {
                compoDao = sqlSession.getMapper(IComponentMapper.class);
            }

            Integer componentSeq = component.getComponentSeq();
            /** 3.5.0 :2019.09.27 : Cluster 전체에서 조회시 중복 제거를 위해 key로 사용되는 Name을 "namespace+name" 으로 변경 **/
            String name = this.makeClusterUniqueName(component.getNamespaceName(), component.getComponentName());
            boolean isManaged = (componentSeq != null);

            boolean k8sResourceExists = false;

            /** 3.5.0 : 2019.09.27 : Cocktail에서 관리되지 않는 경우 currState를 배포정보로부터 수집하고, 알 수 없는 경우 UNKNOWN **/
            StateCode currState = StateCode.STOPPED;

            WorkloadType workloadType = WorkloadType.valueOf(component.getWorkloadType());
            K8sApiVerKindType apiVerKindType = ResourceUtil.getApiVerKindType(cluster, workloadType.getK8sApiKindType());

            if (apiVerKindType != null && MapUtils.isNotEmpty(workloadMap)) {

                V1Deployment currDeployment = null;
                V1StatefulSet currStatefulSet = null;
                V1DaemonSet currDaemonSet = null;
                V1Job currJob = null;
                V1beta1CronJob currCronJobV1beta1 = null;
                V1CronJob currCronJobV1 = null;

                switch (workloadType) {
                    case SINGLE_SERVER:
                    case REPLICA_SERVER:
                        if (MapUtils.getObject(workloadMap, name, null) != null) {
                            if (apiVerKindType.getGroupType() == K8sApiGroupType.APPS && apiVerKindType.getApiType() == K8sApiType.V1) {
                                currDeployment = (V1Deployment)workloadMap.get(name);
                                currState = this.getDeploymentStateV2(currDeployment.getStatus());
                            }
                        }
                        break;
                    case STATEFUL_SET_SERVER:
                        if (MapUtils.getObject(workloadMap, name, null) != null) {
                            if (apiVerKindType.getGroupType() == K8sApiGroupType.APPS && apiVerKindType.getApiType() == K8sApiType.V1) {
                                currStatefulSet = (V1StatefulSet)workloadMap.get(name);
                                currState = this.getStatefulSetState(currStatefulSet.getStatus());
                            }
                        }
                        break;
                    case DAEMON_SET_SERVER:
                        if (MapUtils.getObject(workloadMap, name, null) != null) {
                            if (apiVerKindType.getGroupType() == K8sApiGroupType.APPS && apiVerKindType.getApiType() == K8sApiType.V1) {
                                currDaemonSet = (V1DaemonSet)workloadMap.get(name);
                            }
                        }
                        break;
                    case JOB_SERVER:
                        if (MapUtils.getObject(workloadMap, name, null) != null) {
                            if (apiVerKindType.getGroupType() == K8sApiGroupType.BATCH && apiVerKindType.getApiType() == K8sApiType.V1) {
                                currJob = (V1Job)workloadMap.get(name);
                            }
                        }
                        break;
                    case CRON_JOB_SERVER:
                        if (MapUtils.getObject(workloadMap, name, null) != null) {
                            if (apiVerKindType.getGroupType() == K8sApiGroupType.BATCH && apiVerKindType.getApiType() == K8sApiType.V1BETA1) {
                                currCronJobV1beta1 = (V1beta1CronJob)workloadMap.get(name);
                            } else if (apiVerKindType.getGroupType() == K8sApiGroupType.BATCH && apiVerKindType.getApiType() == K8sApiType.V1) {
                                currCronJobV1 = (V1CronJob)workloadMap.get(name);
                            }
                        }
                        break;
                }

                StateCode newState = currState;
                String detail = "";
                stateLogger.debug("Component state check: name - {}, current state - {}", name, currState);

                switch (workloadType) {
                    case SINGLE_SERVER:
                    case REPLICA_SERVER: {
                        if (currDeployment != null) {
                            k8sResourceExists = true;
                            V1DeploymentStatus status = currDeployment.getStatus();
                            if (status == null || status.getConditions() == null || status.getConditions().size() == 0) { // 상태 파악 불가. 현재 상태 유지
                                stateLogger.debug("Deployment status or conditions is empty");
                                component.setActiveCount(0);
                            } else {
                                if (currState != StateCode.STOPPING) {
                                    component.setComputeTotal(Optional.ofNullable(currDeployment)
                                            .map(V1Deployment::getSpec)
                                            .map(V1DeploymentSpec::getReplicas)
                                            .orElseGet(() ->0));

                                    StateCode deploymentState = this.getDeploymentStateV2(status);
                                    stateLogger.debug("Deployment state: {}", deploymentState);
                                    stateLogger.debug("Label: {}", name);

                                    StateAndCount sc = this.getPodStateV2(podMap.get(name));
                                    stateLogger.debug("Deployment [{}] - Pod state: {}, count: {}", name, sc.state, sc.count);
                                    component.setActiveCount(sc.count);
                                    stateLogger.debug("Deployment [{}] - Active count: {}", name, component.getActiveCount());

                                    if (deploymentState == StateCode.RUNNING &&
                                            sc.state == StateCode.RUNNING) {
                                        newState = StateCode.RUNNING;
                                    } else if (deploymentState != StateCode.RUNNING) {
                                        newState = deploymentState;
                                    } else {
                                        newState = sc.state;
                                    }
                                }
                            }
                            /** K8s에서 조회한 CreationTimeStamp를 사용 **/
                            /** hjchoi.20200318 addComponentDefault에서 셋팅함 **/
//                    component.setCreationTimestamp(Optional.ofNullable(deploymentMap.get(name))
//                        .map(V1Deployment::getMetadata)
//                        .map(V1ObjectMeta::getCreationTimestamp)
//                        .orElseGet(() ->null));

                            /** Group 정보를 Annotation으로부터 조회하여 설정 **/
                            Map<String, String> annotationsForGroup = Optional.ofNullable(currDeployment)
                                    .map(V1Deployment::getMetadata)
                                    .map(V1ObjectMeta::getAnnotations)
                                    .orElseGet(() ->Maps.newHashMap());
                            Integer workloadGroupSeq = ServerUtils.getInteger(annotationsForGroup.get(KubeConstants.ANNOTATION_COCKTAIL_GROUP_NO));
                            component.setWorkloadGroupSeq(workloadGroupSeq);
                        }
                        break;
                    }
                    case STATEFUL_SET_SERVER: {
                        if (currStatefulSet != null) {
                            k8sResourceExists = true;
                            V1StatefulSetStatus status = currStatefulSet.getStatus();
                            if (status == null) { // 상태 파악 불가. 현재 상태 유지
                                stateLogger.debug("StatefulSet status");
                                component.setActiveCount(0);
                            } else {
                                if (currState != StateCode.STOPPING) {
                                    component.setComputeTotal(Optional.ofNullable(currStatefulSet)
                                            .map(V1StatefulSet::getSpec)
                                            .map(V1StatefulSetSpec::getReplicas)
                                            .orElseGet(() ->0));

                                    StateCode statefulSetState = this.getStatefulSetState(status);
                                    stateLogger.debug("StatefulSet state: {}", statefulSetState);
                                    stateLogger.debug("Label: {}", name);

                                    StateAndCount sc = this.getPodStateV2(podMap.get(name));
                                    stateLogger.debug("StatefulSet [{}] - Pod state: {}, count: {}", name, sc.state, sc.count);
                                    component.setActiveCount(sc.count);
                                    stateLogger.debug("StatefulSet [{}] - Active count: {}", name, component.getActiveCount());

                                    if (statefulSetState == StateCode.RUNNING &&
                                            sc.state == StateCode.RUNNING) {
                                        newState = StateCode.RUNNING;
                                    } else if (statefulSetState != StateCode.RUNNING) {
                                        newState = statefulSetState;
                                    } else {
                                        newState = sc.state;
                                    }
                                }
                            }
                            /** K8s에서 조회한 CreationTimeStamp를 사용 **/
                            /** hjchoi.20200318 addComponentDefault에서 셋팅함 **/
//                    component.setCreationTimestamp(Optional.ofNullable(statefulSetMap.get(name))
//                        .map(V1StatefulSet::getMetadata)
//                        .map(V1ObjectMeta::getCreationTimestamp)
//                        .orElseGet(() ->null));

                            /** Group 정보를 Annotation으로부터 조회하여 설정 **/
                            Map<String, String> annotationsForGroup = Optional.ofNullable(currStatefulSet)
                                    .map(V1StatefulSet::getMetadata)
                                    .map(V1ObjectMeta::getAnnotations)
                                    .orElseGet(() ->Maps.newHashMap());
                            Integer workloadGroupSeq = ServerUtils.getInteger(annotationsForGroup.get(KubeConstants.ANNOTATION_COCKTAIL_GROUP_NO));
                            component.setWorkloadGroupSeq(workloadGroupSeq);
                        }
                        break;
                    }
                    case DAEMON_SET_SERVER: {
                        if (currDaemonSet != null) {
                            k8sResourceExists = true;
                            V1DaemonSetStatus status = currDaemonSet.getStatus();
                            if (status == null) { // 상태 파악 불가. 현재 상태 유지
                                stateLogger.debug("DaemonSet status");
                                component.setActiveCount(0);
                            } else {
                                if (currState != StateCode.STOPPING) {
                                    component.setComputeTotal(status.getDesiredNumberScheduled());

                                    StateAndCount sc = this.getPodStateV2(podMap.get(name));
                                    stateLogger.debug("DaemonSet [{}] - Pod state: {}, count: {}", name, sc.state, sc.count);
                                    component.setActiveCount(sc.count);
                                    stateLogger.debug("DaemonSet [{}] - Active count: {}", name, component.getActiveCount());

                                    if (sc.state == StateCode.RUNNING) {
                                        newState = StateCode.RUNNING;
                                    } else {
                                        newState = sc.state;
                                    }
                                }
                            }
                            /** K8s에서 조회한 CreationTimeStamp를 사용 **/
                            /** hjchoi.20200318 addComponentDefault에서 셋팅함 **/
//                    component.setCreationTimestamp(Optional.ofNullable(daemonSetMap.get(name))
//                        .map(V1DaemonSet::getMetadata)
//                        .map(V1ObjectMeta::getCreationTimestamp)
//                        .orElseGet(() ->null));

                            /** Group 정보를 Annotation으로부터 조회하여 설정 **/
                            Map<String, String> annotationsForGroup = Optional.ofNullable(currDaemonSet)
                                    .map(V1DaemonSet::getMetadata)
                                    .map(V1ObjectMeta::getAnnotations)
                                    .orElseGet(() ->Maps.newHashMap());
                            Integer workloadGroupSeq = ServerUtils.getInteger(annotationsForGroup.get(KubeConstants.ANNOTATION_COCKTAIL_GROUP_NO));
                            component.setWorkloadGroupSeq(workloadGroupSeq);
                        }
                        break;
                    }
                    case JOB_SERVER: {
                        if (currJob != null) {
                            k8sResourceExists = true;
                            V1JobStatus status = currJob.getStatus();
                            if (status == null) { // 상태 파악 불가. 현재 상태 유지
                                stateLogger.debug("Job status");
                                component.setActiveCount(0);
                            } else {
                                if (currState != StateCode.STOPPING) {
                                    component.setComputeTotal(Optional.ofNullable(currJob)
                                            .map(V1Job::getSpec)
                                            .map(V1JobSpec::getCompletions)
                                            .orElseGet(() ->0));
                                    StateCode jobState = this.getJobState(status);
                                    stateLogger.debug("Job state: {}", jobState);
                                    stateLogger.debug("Label: {}", name);

                                    int s = status.getSucceeded() != null ? status.getSucceeded() : 0;
                                    int f = status.getFailed() != null ? status.getFailed() : 0;
                                    int a = status.getActive() != null ? status.getActive() : 0;

                                    component.setActiveCount(s + f + a);
                                    stateLogger.debug("Job [{}] - Active count: {}", name, component.getActiveCount());

                                    newState = jobState;
                                }
                            }
                            /** K8s에서 조회한 CreationTimeStamp를 사용 **/
                            /** hjchoi.20200318 addComponentDefault에서 셋팅함 **/
//                    component.setCreationTimestamp(Optional.ofNullable(jobMap.get(name))
//                        .map(V1Job::getMetadata)
//                        .map(V1ObjectMeta::getCreationTimestamp)
//                        .orElseGet(() ->null));

                            /** Group 정보를 Annotation으로부터 조회하여 설정 **/
                            Map<String, String> annotationsForGroup = Optional.ofNullable(currJob)
                                    .map(V1Job::getMetadata)
                                    .map(V1ObjectMeta::getAnnotations)
                                    .orElseGet(() ->Maps.newHashMap());
                            Integer workloadGroupSeq = ServerUtils.getInteger(annotationsForGroup.get(KubeConstants.ANNOTATION_COCKTAIL_GROUP_NO));
                            component.setWorkloadGroupSeq(workloadGroupSeq);
                        }
                        break;
                    }
                    case CRON_JOB_SERVER: {
                        if (currCronJobV1beta1 != null) {
                            k8sResourceExists = true;
                            V1beta1CronJobStatus status = currCronJobV1beta1.getStatus();
                            if (status == null) { // 상태 파악 불가. 현재 상태 유지
                                stateLogger.debug("CronJob status");
                                component.setActiveCount(0);
                            } else {
                                if (currState != StateCode.STOPPING) {
                                    component.setComputeTotal(Optional.ofNullable(currCronJobV1beta1)
                                            .map(V1beta1CronJob::getSpec)
                                            .map(V1beta1CronJobSpec::getJobTemplate)
                                            .map(V1beta1JobTemplateSpec::getSpec)
                                            .map(V1JobSpec::getCompletions)
                                            .orElseGet(() ->0));
                                    StateCode jobStateCode;
                                    if (CollectionUtils.isNotEmpty(status.getActive())
                                            && CollectionUtils.size(status.getActive()) > 0) {
                                        jobStateCode = StateCode.RUNNING;

                                        if (MapUtils.isNotEmpty(jobMapInCronJob)) {
                                            for (V1ObjectReference activeRow : status.getActive()) {
                                                log.debug("###############" + JsonUtils.toGson(activeRow));
                                                // String jobName = this.makeClusterUniqueName(cronJobMap.get(name).getMetadata().getNamespace(), activeRow.getName());
                                                // jobMapInCronJob -> Map<ownerName, Map<jobName, job>>
                                                Map<String, V1Job> jobsInCronJob = jobMapInCronJob.get(name);
                                                V1JobStatus jobStatus = null;
                                                if (MapUtils.isNotEmpty(jobsInCronJob)) {
                                                    jobStatus = Optional.ofNullable(jobsInCronJob.get(activeRow.getName())).map(V1Job::getStatus).orElseGet(() ->null);
                                                }
                                                jobStateCode = this.getJobState(jobStatus);
                                                stateLogger.debug("Job state: {}", jobStateCode);
                                                stateLogger.debug("Label: {}", name);

                                                int s = jobStatus == null ? 0 : (jobStatus.getSucceeded() != null ? jobStatus.getSucceeded() : 0);
                                                int f = jobStatus == null ? 0 : (jobStatus.getFailed() != null ? jobStatus.getFailed() : 0);
                                                int a = jobStatus == null ? 0 : (jobStatus.getActive() != null ? jobStatus.getActive() : 0);

                                                component.setActiveCount(s + f + a);
                                                stateLogger.debug("Job [{}] - Active count: {}", name, component.getActiveCount());
                                            }
                                        }
                                    } else {
                                        jobStateCode = StateCode.READY;
                                    }

                                    newState = jobStateCode;

                                }
                            }

                            /** Group 정보를 Annotation으로부터 조회하여 설정 **/
                            Map<String, String> annotationsForGroup = Optional.ofNullable(currCronJobV1beta1)
                                    .map(V1beta1CronJob::getMetadata)
                                    .map(V1ObjectMeta::getAnnotations)
                                    .orElseGet(() ->Maps.newHashMap());
                            Integer workloadGroupSeq = ServerUtils.getInteger(annotationsForGroup.get(KubeConstants.ANNOTATION_COCKTAIL_GROUP_NO));
                            component.setWorkloadGroupSeq(workloadGroupSeq);
                        } else if (currCronJobV1 != null) {
                            k8sResourceExists = true;
                            V1CronJobStatus status = currCronJobV1.getStatus();
                            if (status == null) { // 상태 파악 불가. 현재 상태 유지
                                stateLogger.debug("CronJob status");
                                component.setActiveCount(0);
                            } else {
                                if (currState != StateCode.STOPPING) {
                                    component.setComputeTotal(Optional.ofNullable(currCronJobV1)
                                            .map(V1CronJob::getSpec)
                                            .map(V1CronJobSpec::getJobTemplate)
                                            .map(V1JobTemplateSpec::getSpec)
                                            .map(V1JobSpec::getCompletions)
                                            .orElseGet(() ->0));
                                    StateCode jobStateCode;
                                    if (CollectionUtils.isNotEmpty(status.getActive())
                                            && CollectionUtils.size(status.getActive()) > 0) {
                                        jobStateCode = StateCode.RUNNING;

                                        if (MapUtils.isNotEmpty(jobMapInCronJob)) {
                                            for (V1ObjectReference activeRow : status.getActive()) {
                                                log.debug("###############" + JsonUtils.toGson(activeRow));
                                                // String jobName = this.makeClusterUniqueName(cronJobMap.get(name).getMetadata().getNamespace(), activeRow.getName());
                                                // jobMapInCronJob -> Map<ownerName, Map<jobName, job>>
                                                Map<String, V1Job> jobsInCronJob = jobMapInCronJob.get(name);
                                                V1JobStatus jobStatus = null;
                                                if (MapUtils.isNotEmpty(jobsInCronJob)) {
                                                    jobStatus = Optional.ofNullable(jobsInCronJob.get(activeRow.getName())).map(V1Job::getStatus).orElseGet(() ->null);
                                                }
                                                jobStateCode = this.getJobState(jobStatus);
                                                stateLogger.debug("Job state: {}", jobStateCode);
                                                stateLogger.debug("Label: {}", name);

                                                int s = jobStatus == null ? 0 : (jobStatus.getSucceeded() != null ? jobStatus.getSucceeded() : 0);
                                                int f = jobStatus == null ? 0 : (jobStatus.getFailed() != null ? jobStatus.getFailed() : 0);
                                                int a = jobStatus == null ? 0 : (jobStatus.getActive() != null ? jobStatus.getActive() : 0);

                                                component.setActiveCount(s + f + a);
                                                stateLogger.debug("Job [{}] - Active count: {}", name, component.getActiveCount());
                                            }
                                        }
                                    } else {
                                        jobStateCode = StateCode.READY;
                                    }

                                    newState = jobStateCode;

                                }
                            }

                            /** Group 정보를 Annotation으로부터 조회하여 설정 **/
                            Map<String, String> annotationsForGroup = Optional.ofNullable(currCronJobV1)
                                    .map(V1CronJob::getMetadata)
                                    .map(V1ObjectMeta::getAnnotations)
                                    .orElseGet(() ->Maps.newHashMap());
                            Integer workloadGroupSeq = ServerUtils.getInteger(annotationsForGroup.get(KubeConstants.ANNOTATION_COCKTAIL_GROUP_NO));
                            component.setWorkloadGroupSeq(workloadGroupSeq);
                        }
                        break;
                    }
                    default:
                        break;
                }

                component.setIsK8sResourceExist(k8sResourceExists);
                if (!k8sResourceExists) { // Component 정보는 있는데 대응하는 K8S workload가 없는 경우
//            stateLogger.warn("Component '{}' dose not have K8S workload", componentSeq);
                    stateLogger.debug(" > current state: {}", currState);
                    if (currState == StateCode.STOPPED) {
                        component.setActiveCount(0);

                        // 중지된 workload라도 templateLabels 값이 있으면 서비스 매칭 시켜준다. 2019/12/13, coolingi
                        Map<String, String> podLabels = component.getTemplateLabels();
                        if (podLabels != null && services != null && services.size() > 0) {
                            List<ServiceSpecGuiVO> serviceSpecs = this.getCouplingServices(services, component);
                            if (CollectionUtils.isNotEmpty(serviceSpecs)) {
                                component.setServices(serviceSpecs);
                            }
                        }

                        // 중지된 workload라도 저장된 Manifest 정보에 Annotation 정보가 있으면 groupSeq를 설정. 2020/02/04, redion
                        Map<String, String> annotationsForGroup = Optional.ofNullable(component.getAnnotations()).orElseGet(() ->Maps.newHashMap());
                        Integer workloadGroupSeq = ServerUtils.getInteger(annotationsForGroup.get(KubeConstants.ANNOTATION_COCKTAIL_GROUP_NO));
                        if (workloadGroupSeq != null) {
                            component.setWorkloadGroupSeq(workloadGroupSeq);
                        }

                        return;
                    }
                    if (currState == StateCode.CREATING
                            || currState == StateCode.UPDATING
                            || currState == StateCode.DEPLOYING
                            || currState == StateCode.READY
                            || currState == StateCode.RUNNING_PREPARE
                            || currState == StateCode.RUNNING
                            || currState == StateCode.COMPLETED
                            || currState == StateCode.FAILED
                    ) {
                        if (isManaged) {
                            componentService.updateComponentState(component, StateCode.PENDING, "Server creating", compoDao);
                        }
                        newState = StateCode.PENDING;
                    } else if (currState == StateCode.DELETING
                            || currState == StateCode.STOPPING) { // stopping -> workload 없음 -> stopped
                    } else if (currState != StateCode.PENDING
                            && currState != StateCode.DELETED
                            && currState != StateCode.ERROR) {
                        if (isManaged) {
                            componentService.updateComponentState(component, StateCode.ERROR, "Component has not K8S workload", compoDao);
                        }
                        newState = StateCode.ERROR;
                    } else {
                        if (currState == StateCode.PENDING) {
                            if (isManaged) {
                                componentService.updateComponentState(component, StateCode.STOPPED, "Server stop", compoDao);
                            }
                            newState = StateCode.STOPPED;
                        }
                    }
                    stateLogger.debug(" > new state: {}", newState);
                }

                if (useAdditionalInfo && k8sResourceExists && EnumSet.of(StateCode.RUNNING, StateCode.RUNNING_PREPARE, StateCode.READY, StateCode.FAILED, StateCode.COMPLETED).contains(newState)) {
                    Map<String, String> podLabels = new HashMap<>();
                    List<V1Container> initContainers = null;
                    List<V1Container> containers = null;
                    // 기존 selector의 matchlabels => podtemplate.metadata.labels로 변경, 2019/12/13, coolingi
                    // 실제 matchLabel과 pod label은 다를수 있음.
                    switch (workloadType) {
                        case SINGLE_SERVER:
                        case REPLICA_SERVER:
                            if (currDeployment != null) {
                                containers = Optional.ofNullable(currDeployment)
                                        .map(V1Deployment::getSpec)
                                        .map(V1DeploymentSpec::getTemplate)
                                        .map(V1PodTemplateSpec::getSpec)
                                        .map(V1PodSpec::getContainers)
                                        .orElseGet(() ->null);
                                initContainers = Optional.ofNullable(currDeployment)
                                        .map(V1Deployment::getSpec)
                                        .map(V1DeploymentSpec::getTemplate)
                                        .map(V1PodTemplateSpec::getSpec)
                                        .map(V1PodSpec::getInitContainers)
                                        .orElseGet(() ->null);
                                podLabels = Optional.ofNullable(currDeployment)
                                        .map(V1Deployment::getSpec)
                                        .map(V1DeploymentSpec::getTemplate)
                                        .map(V1PodTemplateSpec::getMetadata)
                                        .map(V1ObjectMeta::getLabels)
                                        .orElseGet(() ->null);
                            }
                            break;
                        case STATEFUL_SET_SERVER:
                            if (currStatefulSet != null) {
                                containers = Optional.ofNullable(currStatefulSet)
                                        .map(V1StatefulSet::getSpec)
                                        .map(V1StatefulSetSpec::getTemplate)
                                        .map(V1PodTemplateSpec::getSpec)
                                        .map(V1PodSpec::getContainers)
                                        .orElseGet(() ->null);
                                initContainers = Optional.ofNullable(currStatefulSet)
                                        .map(V1StatefulSet::getSpec)
                                        .map(V1StatefulSetSpec::getTemplate)
                                        .map(V1PodTemplateSpec::getSpec)
                                        .map(V1PodSpec::getInitContainers)
                                        .orElseGet(() ->null);
                                podLabels = Optional.ofNullable(currStatefulSet)
                                        .map(V1StatefulSet::getSpec)
                                        .map(V1StatefulSetSpec::getTemplate)
                                        .map(V1PodTemplateSpec::getMetadata)
                                        .map(V1ObjectMeta::getLabels)
                                        .orElseGet(() ->null);
                            }
                            break;
                        case DAEMON_SET_SERVER:
                            if (currDaemonSet != null) {
                                containers = Optional.ofNullable(currDaemonSet)
                                        .map(V1DaemonSet::getSpec)
                                        .map(V1DaemonSetSpec::getTemplate)
                                        .map(V1PodTemplateSpec::getSpec)
                                        .map(V1PodSpec::getContainers)
                                        .orElseGet(() ->null);
                                initContainers = Optional.ofNullable(currDaemonSet)
                                        .map(V1DaemonSet::getSpec)
                                        .map(V1DaemonSetSpec::getTemplate)
                                        .map(V1PodTemplateSpec::getSpec)
                                        .map(V1PodSpec::getInitContainers)
                                        .orElseGet(() ->null);
                                podLabels = Optional.ofNullable(currDaemonSet)
                                        .map(V1DaemonSet::getSpec)
                                        .map(V1DaemonSetSpec::getTemplate)
                                        .map(V1PodTemplateSpec::getMetadata)
                                        .map(V1ObjectMeta::getLabels)
                                        .orElseGet(() ->null);
                            }
                            break;
                        case JOB_SERVER:
                            if (currJob != null) {
                                containers = Optional.ofNullable(currJob)
                                        .map(V1Job::getSpec)
                                        .map(V1JobSpec::getTemplate)
                                        .map(V1PodTemplateSpec::getSpec)
                                        .map(V1PodSpec::getContainers)
                                        .orElseGet(() ->null);
                                initContainers = Optional.ofNullable(currJob)
                                        .map(V1Job::getSpec)
                                        .map(V1JobSpec::getTemplate)
                                        .map(V1PodTemplateSpec::getSpec)
                                        .map(V1PodSpec::getInitContainers)
                                        .orElseGet(() ->null);
                                podLabels = Optional.ofNullable(currJob)
                                        .map(V1Job::getSpec)
                                        .map(V1JobSpec::getTemplate)
                                        .map(V1PodTemplateSpec::getMetadata)
                                        .map(V1ObjectMeta::getLabels)
                                        .orElseGet(() ->null);
                            }
                            break;
                        case CRON_JOB_SERVER:
                            if (currCronJobV1beta1 != null) {
                                containers = Optional.ofNullable(currCronJobV1beta1)
                                        .map(V1beta1CronJob::getSpec)
                                        .map(V1beta1CronJobSpec::getJobTemplate)
                                        .map(V1beta1JobTemplateSpec::getSpec)
                                        .map(V1JobSpec::getTemplate)
                                        .map(V1PodTemplateSpec::getSpec)
                                        .map(V1PodSpec::getContainers)
                                        .orElseGet(() ->null);
                                initContainers = Optional.ofNullable(currCronJobV1beta1)
                                        .map(V1beta1CronJob::getSpec)
                                        .map(V1beta1CronJobSpec::getJobTemplate)
                                        .map(V1beta1JobTemplateSpec::getSpec)
                                        .map(V1JobSpec::getTemplate)
                                        .map(V1PodTemplateSpec::getSpec)
                                        .map(V1PodSpec::getInitContainers)
                                        .orElseGet(() ->null);
                                podLabels = Optional.ofNullable(currCronJobV1beta1)
                                        .map(V1beta1CronJob::getSpec)
                                        .map(V1beta1CronJobSpec::getJobTemplate)
                                        .map(V1beta1JobTemplateSpec::getSpec)
                                        .map(V1JobSpec::getTemplate)
                                        .map(V1PodTemplateSpec::getMetadata)
                                        .map(V1ObjectMeta::getLabels)
                                        .orElseGet(() ->null);
                            } else if (currCronJobV1 != null) {
                                containers = Optional.ofNullable(currCronJobV1)
                                        .map(V1CronJob::getSpec)
                                        .map(V1CronJobSpec::getJobTemplate)
                                        .map(V1JobTemplateSpec::getSpec)
                                        .map(V1JobSpec::getTemplate)
                                        .map(V1PodTemplateSpec::getSpec)
                                        .map(V1PodSpec::getContainers)
                                        .orElseGet(() ->null);
                                initContainers = Optional.ofNullable(currCronJobV1)
                                        .map(V1CronJob::getSpec)
                                        .map(V1CronJobSpec::getJobTemplate)
                                        .map(V1JobTemplateSpec::getSpec)
                                        .map(V1JobSpec::getTemplate)
                                        .map(V1PodTemplateSpec::getSpec)
                                        .map(V1PodSpec::getInitContainers)
                                        .orElseGet(() ->null);
                                podLabels = Optional.ofNullable(currCronJobV1)
                                        .map(V1CronJob::getSpec)
                                        .map(V1CronJobSpec::getJobTemplate)
                                        .map(V1JobTemplateSpec::getSpec)
                                        .map(V1JobSpec::getTemplate)
                                        .map(V1PodTemplateSpec::getMetadata)
                                        .map(V1ObjectMeta::getLabels)
                                        .orElseGet(() ->null);
                            }
                            break;
                    }

                    /** Container 리스트 구성 **/
                    component.setContainers(this.buildContainerList(containers, initContainers));

                    /** 실행되는 워크로드의 pod labels 셋팅, 2019/12/13, coolingi **/
                    if (podLabels != null) component.setTemplateLabels(podLabels);

                    /** Service 리스트 구성 **/
                    if (podLabels != null && services != null && services.size() > 0) {
                        List<ServiceSpecGuiVO> serviceSpecs = this.getCouplingServices(services, component);
                        if (serviceSpecs != null && serviceSpecs.size() >= 1) {
                            component.setServices(serviceSpecs);
                        }
                    }

                    /**
                     * 3.5.0 : QoS 설정
                     * Pod가 여러개라도 하나의 Pod에서만 내용을 확인.
                     **/
                    List<V1Pod> pods = podMap.get(name);
                    if (pods != null && pods.size() > 0) {
                        component.setQos(Optional.ofNullable(pods.get(0))
                                .map(V1Pod::getStatus)
                                .map(V1PodStatus::getQosClass)
                                .orElseGet(() ->null));

                        /**
                         * 3.5.0 : Pod 목록 설정 : 2019.10.16 : ServiceSpec 상세에서 Pod목록이 필요..
                         */
                        List<K8sPodVO> k8sPods = new ArrayList<>();
                        if (CollectionUtils.isNotEmpty(pods)) {
                            JSON k8sJson = new JSON();
                            for (V1Pod v1Pod : pods) {
                                K8sPodVO k8sPod = workloadResourceService.genPodData(v1Pod, null, null, k8sJson, false);
                                if (MapUtils.isNotEmpty(podEventMap)) {
                                    k8sPod.setEvents(podEventMap.get(k8sPod.getPodName()));
                                }
                                k8sPods.add(k8sPod);
                            }
                        }

                        component.setPods(k8sPods);
                    }

                }


                // 3.5.0 : 2019.09.27 : Cocktail에서 관리되지 않는 워크로드는 DB에 Default 상태가 없으로 상태값을 입력 해 주어야 함.
                component.setStateCode(newState.getCode());
                component.setStateDetail(detail);
            }
        }

    }

    /**
     * component와 연결된 Service List를 조회.. (Component와 연결된 전체 서비스 조회)
     *
     * @param services
     * @param component
     * @return
     */
    private List<ServiceSpecGuiVO> getCouplingServices(List<V1Service> services, ComponentVO component) throws Exception {
        try {

            if (MapUtils.isNotEmpty(component.getTemplateLabels()) && CollectionUtils.isNotEmpty(services)) {
                List<ServiceSpecGuiVO> serviceSpecs = new ArrayList<>();
                for (V1Service service : services) {
                    Map<String, String> labelSelector = Optional.ofNullable(service)
                        .map(V1Service::getSpec)
                        .map(V1ServiceSpec::getSelector)
                        .orElseGet(() ->null);

                    if (labelSelector == null) continue;

                    /**
                     * Service의 LabelSelector에 해당하는 Key:Value가 Workload의 PodTemplate에 정의된 Labels를 포함하는지 확인.
                     */
                    boolean isFound = ServerUtils.containMaps(component.getTemplateLabels(), labelSelector);
                    // 현재 워크로드에 연결된 서비스면 입력.
                    if(isFound) {
                        ServiceSpecGuiVO serviceSpec = this.buildServiceSpec(service, component);
                        serviceSpecs.add(serviceSpec);
                    }
                }

                return serviceSpecs;
            }
        }
        catch (Exception ex) {
            log.warn("getCouplingServices Error : " + component.getComponentName());
        }

        return null;
    }

//    /**
//     * component와 연결된 Service List를 조회.. (Component에서 생성한 서비스만 조회)
//     * @param serviceMap
//     * @param component
//     * @return
//     */
//    private List<ServiceSpecGuiVO> getCouplingServices(Map<String, List<V1Service>> serviceMap, ComponentVO component) throws Exception {
//        try {
//            // k8s service 조회
//            List<V1Service> v1Services = serviceMap.get(component.getComponentName());
//            if (CollectionUtils.isEmpty(v1Services)) {
//                v1Services = serviceMap.get(String.format("%s-%s", component.getComponentName(), PortType.CLUSTER_IP.getCode().replace('_', '-').toLowerCase()));
//            }
//
//            if (CollectionUtils.isNotEmpty(v1Services)) {
//                List<ServiceSpecGuiVO> serviceSpecs = new ArrayList<>();
//                if (CollectionUtils.isNotEmpty(v1Services)) {
//                    int i = 1;
//                    for (V1Service v1ServiceRow : v1Services) {
//                        // ClusterIP Type 만 변환 (기존 로직 유지함.. ClusterIP Type만 처리하도록..)
//                        if (StringUtils.equals(v1ServiceRow.getSpec().getType(), KubeServiceTypes.ClusterIP.name())) {
//                            ServiceSpecGuiVO serviceSpec = this.buildServiceSpec(v1ServiceRow, component);
//                            serviceSpecs.add(serviceSpec);
//                        }
//                    }
//                }
//
//                return serviceSpecs;
//            }
//        }
//        catch (Exception ex) {
//            log.error("getCouplingServices Error : " + component.getComponentName());
//        }
//
//        return null;
//    }

    /**
     * V1Service to ServiceSpecGuiVO with ComponentVO
     * @param v1Service
     * @param component
     * @return
     */
    private ServiceSpecGuiVO buildServiceSpec(V1Service v1Service, ComponentVO component) {
        ServiceSpecGuiVO serviceSpec = new ServiceSpecGuiVO();
        List<ServicePortVO> servicePorts = new ArrayList<>();
        serviceSpec.setComponentSeq(component.getComponentSeq());
        serviceSpec.setName(v1Service.getMetadata().getName());
        serviceSpec.setNamespaceName(v1Service.getMetadata().getNamespace());
        serviceSpec.setServiceType(PortType.findPortType(v1Service.getSpec().getType()).getCode());
        serviceSpec.setClusterIp(v1Service.getSpec().getClusterIP());
        String isHeadless = v1Service.getSpec().getClusterIP();
        if(isHeadless != null && isHeadless.equals("None")) {
            serviceSpec.setHeadlessFlag(Boolean.TRUE);
            serviceSpec.setServiceType(PortType.HEADLESS.getCode());
        }
        else {
            serviceSpec.setHeadlessFlag(Boolean.FALSE);
        }
        /** TODO: Cloud Vendor마다 Internal Load Balancer를 확인하는 Annotation이 다름... 본 로직에서는 해당 내용의 중요도가 높지 않으니 우선 기존과 동일하게 FALSE로 설정. **/
        serviceSpec.setInternalLBFlag(Boolean.FALSE);
        if (StringUtils.equalsIgnoreCase(v1Service.getSpec().getSessionAffinity(), "ClientIP")) {
            serviceSpec.setStickySessionFlag(Boolean.TRUE);
            serviceSpec.setStickySessionTimeoutSeconds(v1Service.getSpec().getSessionAffinityConfig().getClientIP().getTimeoutSeconds());
        }
        else {
            serviceSpec.setStickySessionFlag(Boolean.FALSE);
            serviceSpec.setStickySessionTimeoutSeconds(10800);
        }

        // LoadBalancer 정보 셋팅
        if (StringUtils.equalsIgnoreCase(v1Service.getSpec().getType(), PortType.LOADBALANCER.getType())) {
            if (CollectionUtils.isNotEmpty(v1Service.getStatus().getLoadBalancer().getIngress())) {
                String ip = v1Service.getStatus().getLoadBalancer().getIngress().get(0).getIp();
                String hostname = v1Service.getStatus().getLoadBalancer().getIngress().get(0).getHostname();

                serviceSpec.setLoadBalancer(StringUtils.isEmpty(ip)? hostname : ip);
            }
        } else if (StringUtils.equalsIgnoreCase(v1Service.getSpec().getType(), PortType.EXTERNAL_NAME.getType())) {
            serviceSpec.setExternalName(v1Service.getSpec().getExternalName());
        }

        if (CollectionUtils.isNotEmpty(v1Service.getSpec().getPorts())) {
            for (V1ServicePort v1ServicePortRow : v1Service.getSpec().getPorts()) {
                ServicePortVO servicePort = new ServicePortVO();
                servicePort.setName(v1ServicePortRow.getName());
                servicePort.setPort(v1ServicePortRow.getPort() != null ? v1ServicePortRow.getPort().intValue() : null);
                servicePort.setProtocol(v1ServicePortRow.getProtocol());
                if(v1ServicePortRow.getTargetPort() != null) {
                    servicePort.setTargetPort(v1ServicePortRow.getTargetPort().toString());
                }

                if (StringUtils.equalsIgnoreCase(v1Service.getSpec().getType(), PortType.NODE_PORT.getType())) {
                    servicePort.setNodePort(v1ServicePortRow.getNodePort());
                }
                servicePorts.add(servicePort);
            }
            serviceSpec.setServicePorts(servicePorts);
        }

        return serviceSpec;
    }

    /**
     * Container List를 구성..
     * @param containers
     * @param initContainers
     * @return
     */
    private List<ContainerVO> buildContainerList(List<V1Container> containers, List<V1Container> initContainers) {
        List<ContainerVO> containerList = new ArrayList<>();
        if (containers != null && containers.size() > 0) {
            for (V1Container container : containers) {
                ContainerVO containerVO = new ContainerVO();
                containerVO.setFullImageName(container.getImage());
                containerVO.setContainerName(container.getName());
                containerVO.setInitContainerYn("N");
                containerList.add(containerVO);
            }
        }
        if (initContainers != null && initContainers.size() > 0) {
            for (V1Container container : initContainers) {
                ContainerVO containerVO = new ContainerVO();
                containerVO.setFullImageName(container.getImage());
                containerVO.setContainerName(container.getName());
                containerVO.setInitContainerYn("Y");
                containerList.add(containerVO);
            }
        }
        return containerList;
    }

    private StateCode getDeploymentStateV2(V1DeploymentStatus status) {
        if(status != null){
            if(CollectionUtils.isNotEmpty(status.getConditions())){
                Map<String, String> conditionMap = new HashMap<>();

                for(V1DeploymentCondition conditionRow : status.getConditions()){
                    conditionMap.put(conditionRow.getType(), conditionRow.getStatus());
                }

                if (MapUtils.isNotEmpty(conditionMap)){
                    // Condition - Type != Failure 가 아니면 정상 상태 체크
                    if(conditionMap.get(KubeConstants.DEPLOYMENT_CONDITION_FAILURE) == null){
                        StateCode state = StateCode.RUNNING_PREPARE;

                        if(conditionMap.get(KubeConstants.DEPLOYMENT_CONDITION_PROGRESSING) != null
                                && !StringUtils.equalsIgnoreCase("True", conditionMap.get(KubeConstants.DEPLOYMENT_CONDITION_PROGRESSING))){
                            return state;
                        }

                        if(conditionMap.get(KubeConstants.DEPLOYMENT_CONDITION_AVAILABLE) != null){
                            if(BooleanUtils.toBoolean(conditionMap.get(KubeConstants.DEPLOYMENT_CONDITION_AVAILABLE))){
                                if(status.getReadyReplicas() != null && status.getReadyReplicas().intValue() > 0){
                                    return StateCode.RUNNING;
                                }
                            }
                        }

                        return state;
                    }
                }
            }
        }

        return StateCode.RUNNING_WARNING;
    }

    private StateCode getStatefulSetState(V1StatefulSetStatus status) {
        if(status != null){

            if (status.getReplicas() != null) {
                if (status.getReadyReplicas() != null) {
                    if (status.getReplicas().equals(status.getReadyReplicas())) {
                        return StateCode.RUNNING;
                    } else {
                        return StateCode.RUNNING_PREPARE;
                    }
                } else {
                    return StateCode.RUNNING_PREPARE;
                }
            }
        }

        return StateCode.RUNNING_WARNING;
    }

    private StateCode getJobState(V1JobStatus status) {
        if(status != null){
            // Condition 은 작업이 끝나고 난 뒤에 생성됨
            if(CollectionUtils.isNotEmpty(status.getConditions())){
                Map<String, String> conditionMap = new HashMap<>();

                for(V1JobCondition conditionRow : status.getConditions()){
                    conditionMap.put(conditionRow.getType(), conditionRow.getStatus());
                }

                if (MapUtils.isNotEmpty(conditionMap)){

                    if(conditionMap.get(KubeConstants.JOB_CONDITION_COMPLETE) == null && conditionMap.get(KubeConstants.JOB_CONDITION_FAILED) == null){
                        return StateCode.RUNNING;
                    } else {
                        if(conditionMap.get(KubeConstants.JOB_CONDITION_FAILED) != null
                                && BooleanUtils.toBoolean(conditionMap.get(KubeConstants.JOB_CONDITION_FAILED))){
                            return StateCode.FAILED;
                        }

                        if(conditionMap.get(KubeConstants.JOB_CONDITION_COMPLETE) != null
                                && BooleanUtils.toBoolean(conditionMap.get(KubeConstants.JOB_CONDITION_COMPLETE))){
                            return StateCode.COMPLETED;
                        }
                    }
                }
            }
            // Condition이 없을 경우, Active, Succeeded, Failed 수치를 가지고 판단
            else {
                if (status.getActive() == null && status.getSucceeded() == null && status.getFailed() == null) {
                    return StateCode.RUNNING_PREPARE;
                } else {
                    return StateCode.RUNNING;
                }
            }
        }

        return StateCode.RUNNING_WARNING;
    }

    protected Map<String, V1Deployment> getDeploymentToMap(List<V1Deployment> deployments, Map<String, V1Deployment> deploymentMap){
        if(CollectionUtils.isNotEmpty(deployments)){
            for(V1Deployment deploymentRow : deployments){
                /** 3.5.0 :2019.09.27 : Cluster 전체에서 조회시 중복 제거를 위해 key로 사용되는 Name을 "namespace+name" 으로 변경 **/
                String name = this.makeClusterUniqueName(deploymentRow.getMetadata().getNamespace(), deploymentRow.getMetadata().getName());
                deploymentMap.put(name, deploymentRow);
            }
        }

        return deploymentMap;
    }

    protected Map<String, V1StatefulSet> getStatefulSetToMap(List<V1StatefulSet> statefulSets, Map<String, V1StatefulSet> statefulSetMap){
        if(CollectionUtils.isNotEmpty(statefulSets)){
            for(V1StatefulSet statefulSetRow : statefulSets){
                /** 3.5.0 :2019.09.27 : Cluster 전체에서 조회시 중복 제거를 위해 key로 사용되는 Name을 "namespace+name" 으로 변경 **/
                String name = this.makeClusterUniqueName(statefulSetRow.getMetadata().getNamespace(), statefulSetRow.getMetadata().getName());
                statefulSetMap.put(name, statefulSetRow);
            }
        }

        return statefulSetMap;
    }

    protected Map<String, V1DaemonSet> getDaemonSetToMap(List<V1DaemonSet> daemonSets, Map<String, V1DaemonSet> daemonSetMap){
        if(CollectionUtils.isNotEmpty(daemonSets)){
            for(V1DaemonSet daemonSetRow : daemonSets){
                /** 3.5.0 :2019.09.27 : Cluster 전체에서 조회시 중복 제거를 위해 key로 사용되는 Name을 "namespace+name" 으로 변경 **/
                String name = this.makeClusterUniqueName(daemonSetRow.getMetadata().getNamespace(), daemonSetRow.getMetadata().getName());
                daemonSetMap.put(name, daemonSetRow);
            }
        }

        return daemonSetMap;
    }

    protected Map<String, V1Job> getJobToMap(List<V1Job> allJobs, Map<String, V1Job> jobMap){
        return this.getJobToMap(allJobs, null, jobMap);
    }
    protected Map<String, V1Job> getJobToMap(List<V1Job> allJobs, List<V1Job> jobs, Map<String, V1Job> jobMap){
        if(CollectionUtils.isNotEmpty(allJobs)){
            String ownerName = "";
            for(V1Job jobRow : allJobs){
                ownerName = "";
                if (CollectionUtils.isNotEmpty(jobRow.getMetadata().getOwnerReferences())) {
                    for (V1OwnerReference ownerReferenceRow : jobRow.getMetadata().getOwnerReferences()) {
                        ownerName = this.makeClusterUniqueName(jobRow.getMetadata().getNamespace(), ownerReferenceRow.getName());
                        break;
                    }
                }
                if (StringUtils.isBlank(ownerName)) {
                    ownerName = this.makeClusterUniqueName(jobRow.getMetadata().getNamespace(), jobRow.getMetadata().getName());
                    if(jobs != null) {
                        jobs.add(jobRow);
                    }
                }
                if(StringUtils.isNotBlank(ownerName)){
                    jobMap.put(ownerName, jobRow);
                }
            }
        }

        return jobMap;
    }

    protected void getJobToMapListWithFilteringJobWorkload(List<V1Job> allJobs, List<V1Job> jobs, Map<String, Map<String, V1Job>> jobMap){
        if(CollectionUtils.isNotEmpty(allJobs)){
            String ownerName = "";
            for(V1Job jobRow : allJobs){
                ownerName = "";
                if (CollectionUtils.isNotEmpty(jobRow.getMetadata().getOwnerReferences())) {
                    for (V1OwnerReference ownerReferenceRow : jobRow.getMetadata().getOwnerReferences()) {
                        ownerName = this.makeClusterUniqueName(jobRow.getMetadata().getNamespace(), ownerReferenceRow.getName());
                        break;
                    }
                }
                // owner가 없는 job 셋팅
                if (StringUtils.isBlank(ownerName)) {
                    if(jobs != null) {
                        jobs.add(jobRow);
                    }
                }
                if(StringUtils.isNotBlank(ownerName) && !jobMap.containsKey(ownerName)){
                    jobMap.put(ownerName, Maps.newHashMap());
                }
                if(jobMap.get(ownerName) != null){
                    jobMap.get(ownerName).put(jobRow.getMetadata().getName(), jobRow);
                }
            }
        }

    }

    protected Map<String, V1beta1CronJob> getCronJobV1beta1ToMap(List<V1beta1CronJob> cronJobs, Map<String, V1beta1CronJob> cronJobMap){
        if(CollectionUtils.isNotEmpty(cronJobs)){
            for(V1beta1CronJob cronJobRow : cronJobs){
                /** 3.5.0 :2019.09.27 : Cluster 전체에서 조회시 중복 제거를 위해 key로 사용되는 Name을 "namespace+name" 으로 변경 **/
                String name = this.makeClusterUniqueName(cronJobRow.getMetadata().getNamespace(), cronJobRow.getMetadata().getName());
                cronJobMap.put(name, cronJobRow);
            }
        }

        return cronJobMap;
    }

    protected Map<String, V1CronJob> getCronJobV1ToMap(List<V1CronJob> cronJobs, Map<String, V1CronJob> cronJobMap){
        if(CollectionUtils.isNotEmpty(cronJobs)){
            for(V1CronJob cronJobRow : cronJobs){
                /** 3.5.0 :2019.09.27 : Cluster 전체에서 조회시 중복 제거를 위해 key로 사용되는 Name을 "namespace+name" 으로 변경 **/
                String name = this.makeClusterUniqueName(cronJobRow.getMetadata().getNamespace(), cronJobRow.getMetadata().getName());
                cronJobMap.put(name, cronJobRow);
            }
        }

        return cronJobMap;
    }

    protected String makeClusterUniqueName(String namespace, String name) {
        // 더 좋은 아이디어 생기면 그때 교체.. (ComponentVO에서 확인 가능한 정보로만 구성 가능)
        return String.format("%s-%s", namespace, name);

    }

    protected Map<String, List<V1Pod>> getPodToMap(List<V1Pod> pods, Map<String, List<V1Pod>> podMap){
        if(CollectionUtils.isNotEmpty(pods)){
            String ownerName = "";
            for(V1Pod podRow : pods){
                ownerName = "";
                if (CollectionUtils.isNotEmpty(podRow.getMetadata().getOwnerReferences())) {
                    for (V1OwnerReference ownerReferenceRow : podRow.getMetadata().getOwnerReferences()) {
                        ownerName = this.makeClusterUniqueName(podRow.getMetadata().getNamespace(), ownerReferenceRow.getName());
                        break;
                    }
                }
                if (StringUtils.isBlank(ownerName)) {
                    /** 3.5.0 :2019.09.27 : Cluster 전체에서 조회시 중복 제거를 위해 key로 사용되는 Name을 "namespace+deploymentName" 으로 변경
                     *  Cocktail에서 관리되지 않는 일부 Pod들중 "app" label이 없는 경우 pod의 이름으로 설정.. (맵에 들어가긴 하지만 이후 사용되지는 않음)
                     * **/
//                    String name = Optional.ofNullable(podRow).map(V1Pod::getMetadata).map(V1ObjectMeta::getLabels).orElseGet(() ->new HashMap<>()).get(KubeConstants.LABELS_KEY);
//                    if(StringUtils.isBlank(name)) {
//                        name = podRow.getMetadata().getName();
//                    }
//                    ownerName = this.makeClusterUniqueName(podRow.getMetadata().getNamespace(), name);

                    // app 라벨이 실제 리소스명이 아니므로 owner가 없을 시 그냥 pod명을 셋팅
                    ownerName = this.makeClusterUniqueName(podRow.getMetadata().getNamespace(), podRow.getMetadata().getName());
                }
                if(StringUtils.isNotBlank(ownerName) && !podMap.containsKey(ownerName)){
                    podMap.put(ownerName, new ArrayList<>());
                }
                if(podMap.get(ownerName) != null){
                    podMap.get(ownerName).add(podRow);
                }
            }
        }

        return podMap;
    }

    public Map<String, List<V1ReplicaSet>> getReplicaSetToMap(List<V1ReplicaSet> replicaSets, Map<String, List<V1ReplicaSet>> replicaSetMap){
        if(CollectionUtils.isNotEmpty(replicaSets)){
            String ownerName = "";
            for(V1ReplicaSet replicaSetRow : replicaSets){
                ownerName = "";
                if (CollectionUtils.isNotEmpty(replicaSetRow.getMetadata().getOwnerReferences())) {
                    for (V1OwnerReference ownerReferenceRow : replicaSetRow.getMetadata().getOwnerReferences()) {
                        ownerName = this.makeClusterUniqueName(replicaSetRow.getMetadata().getNamespace(), ownerReferenceRow.getName());
                        break;
                    }
                }
                if (StringUtils.isBlank(ownerName)) {
                    ownerName = replicaSetRow.getMetadata().getName();
                    ownerName = this.makeClusterUniqueName(replicaSetRow.getMetadata().getNamespace(), ownerName);
                }

                if(StringUtils.isNotBlank(ownerName) && !replicaSetMap.containsKey(ownerName)){
                    replicaSetMap.put(ownerName, new ArrayList<>());
                }

                if(replicaSetMap.get(ownerName) != null){
                    replicaSetMap.get(ownerName).add(replicaSetRow);
                }
            }
        }

        return replicaSetMap;
    }


//    protected Map<String, List<V1Service>> getServiceToMap(List<V1Service> services, Map<String, List<V1Service>> serviceMap){
//        return getServiceToMap(services, serviceMap, null);
//    }
//
//    protected Map<String, List<V1Service>> getServiceToMap(List<V1Service> services, Map<String, List<V1Service>> serviceMap, WorkloadType workloadType){
//        if(CollectionUtils.isNotEmpty(services)){
//            String deploymentName;
//            for(V1Service serviceRow : services){
//                deploymentName = Optional.ofNullable(serviceRow).map(V1Service::getMetadata).map(V1ObjectMeta::getLabels).orElseGet(() ->new HashMap<>()).get(KubeConstants.LABELS_KEY);
//
//                if(!serviceMap.containsKey(deploymentName)){
//                    serviceMap.put(deploymentName, new ArrayList<>());
//                }
//
//                if(serviceMap.get(deploymentName) != null){
//                    serviceMap.get(deploymentName).add(serviceRow);
//                }
//            }
//        }
//
//        return serviceMap;
//    }
//
//    protected Map<String, List<V1Endpoints>> getEndpointToMap(List<V1Endpoints> endpoints, Map<String, List<V1Endpoints>> endpointMap, WorkloadType workloadType){
//        if(CollectionUtils.isNotEmpty(endpoints)){
//            String deploymentName;
//            for(V1Endpoints endpointRow : endpoints){
//                deploymentName = endpointRow.getMetadata().getLabels().get(KubeConstants.LABELS_KEY);
//                if(!endpointMap.containsKey(deploymentName)){
//                    endpointMap.put(deploymentName, new ArrayList<>());
//                }
//
//                if(endpointMap.get(deploymentName) != null){
//                    endpointMap.get(deploymentName).add(endpointRow);
//                }
//            }
//        }
//
//        return endpointMap;
//    }

    private StateAndCount getPodStateV2(List<V1Pod> pods) {
        StateAndCount sc = new StateAndCount();
        if(CollectionUtils.isNotEmpty(pods)){
            stateLogger.debug("Total Pod count: {}", pods.size());
        }else{
            sc.count = 0;
            sc.state = StateCode.RUNNING_PREPARE;
            return sc;
        }

        sc.state = StateCode.RUNNING;
        for (V1Pod p : pods) {
            if (p.getStatus() == null || p.getStatus().getConditions() == null || p.getStatus().getConditions().size() == 0) {
                continue;
            }

            stateLogger.debug("Pod phase: {}/{} - {}", p.getMetadata().getOwnerReferences().get(0).getName(), p.getMetadata().getName(), p.getStatus().getPhase());
            if (p.getStatus().getPhase().equals("Pending")) {
                if (sc.state == StateCode.RUNNING) {
                    sc.state = StateCode.RUNNING_PREPARE;
                }
            }

            // TODO: Running phase가 아닌 다른 phase에 대한 처리는 어떻게 할 것인가?

            if(CollectionUtils.isNotEmpty(p.getStatus().getConditions())){
                for(V1PodCondition pc : p.getStatus().getConditions()){
                    stateLogger.debug("Pod state: {}/{} - {}: {}", p.getMetadata().getOwnerReferences().get(0).getName(), p.getMetadata().getName(), pc.getType(), pc.getStatus());
                    if(StringUtils.equalsIgnoreCase("Ready", pc.getType())){
                        if(StringUtils.equalsIgnoreCase("True", pc.getStatus())){
                            sc.count++;
                        }else{
                            if (sc.state == StateCode.RUNNING) {
                                sc.state = StateCode.RUNNING_PREPARE;
                            }
                        }
                        break;
                    }
                }
            }else{
                sc.state = StateCode.RUNNING_PREPARE;
            }

            // InitContainerStatuses
            if(CollectionUtils.isNotEmpty(p.getStatus().getInitContainerStatuses())){
                for (V1ContainerStatus status : p.getStatus().getInitContainerStatuses()) {
                    if (!status.getReady()) {
                        if (sc.state == StateCode.RUNNING) {
                            sc.state = StateCode.RUNNING_PREPARE;
                        }
                        break;
                    } else {
                        stateLogger.debug(" is ready: {}", status.getReady());
                    }
                }
            }
            // ContainerStatuses
            if(CollectionUtils.isNotEmpty(p.getStatus().getContainerStatuses())){
                for (V1ContainerStatus status : p.getStatus().getContainerStatuses()) {
                    if (!status.getReady()) {
                        if (sc.state == StateCode.RUNNING) {
                            sc.state = StateCode.RUNNING_PREPARE;
                        }
                        break;
                    } else {
                        stateLogger.debug(" is ready: {}", status.getReady());
                    }
                }
            }else{
                sc.state = StateCode.RUNNING_PREPARE;
            }
        }

        return sc;
    }

    /**
     * yaml을 Object로 변경하여 template 의 label을 추출한다.
     *
     * @param yaml
     * @return
     */
    private Map<String, String> getTemplateLabels(String yaml){
        V1Deployment currDeployment = null;
        V1DaemonSet currDaemonSet = null;
        V1Job currJob = null;
        V1beta1CronJob currCronJobV1beta1 = null;
        V1CronJob currCronJobV1 = null;
        V1StatefulSet currStatefulSet = null;

        Map<String,String> templateLabels = Maps.newHashMap();

        if (StringUtils.isNotBlank(yaml)) {

            try {
                List<Object> objs = ServerUtils.getYamlObjects(yaml);

                if (CollectionUtils.isNotEmpty(objs)) {
                    JSON k8sJson = new JSON();

                    for (Object obj : objs) {
                        Map<String, Object> k8sObjectToMap = ServerUtils.getK8sObjectToMap(obj, k8sJson);
                        K8sApiKindType kind = ServerUtils.getK8sKindInMap(k8sObjectToMap);
                        K8sApiGroupType apiGroupType = ServerUtils.getK8sGroupInMap(k8sObjectToMap);
                        K8sApiType apiType = ServerUtils.getK8sVersionInMap(k8sObjectToMap);

                        switch (kind) {
                            case DEPLOYMENT:
                                currDeployment = (V1Deployment) obj;
                                templateLabels = currDeployment.getSpec().getTemplate().getMetadata().getLabels();
                                break;
                            case DAEMON_SET:
                                currDaemonSet = (V1DaemonSet) obj;
                                templateLabels = currDaemonSet.getSpec().getTemplate().getMetadata().getLabels();
                                break;
                            case JOB:
                                currJob = (V1Job) obj;
                                templateLabels = currJob.getSpec().getTemplate().getMetadata().getLabels();
                                break;
                            case CRON_JOB:
                                if (apiGroupType == K8sApiGroupType.BATCH && apiType == K8sApiType.V1BETA1) {
                                    currCronJobV1beta1 = (V1beta1CronJob) obj;
                                    templateLabels = currCronJobV1beta1.getSpec().getJobTemplate().getMetadata().getLabels();
                                } else if (apiGroupType == K8sApiGroupType.BATCH && apiType == K8sApiType.V1) {
                                    currCronJobV1 = (V1CronJob) obj;
                                    templateLabels = currCronJobV1.getSpec().getJobTemplate().getMetadata().getLabels();
                                }
                                break;
                            case STATEFUL_SET:
                                currStatefulSet = (V1StatefulSet) obj;
                                templateLabels = currStatefulSet.getSpec().getTemplate().getMetadata().getLabels();
                                break;
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Fail to extract a template labels of yaml!!", e);
            }
        }

        return templateLabels;
    }

    /**
     * yaml을 Object로 변경하여 template 의 label을 추출한다.
     *
     * @param yaml
     * @return
     */
    private Map<String, Map<String, String>> getAllLabelsAndAnnotations(String yaml){
        V1Deployment currDeployment = null;
        V1DaemonSet currDaemonSet = null;
        V1Job currJob = null;
        V1beta1CronJob currCronJobV1beta1 = null;
        V1CronJob currCronJobV1 = null;
        V1StatefulSet currStatefulSet = null;

        Map<String,String> labels = Maps.newHashMap();
        Map<String,String> annotations = Maps.newHashMap();
        Map<String,String> templateLabels = Maps.newHashMap();

        if (StringUtils.isNotBlank(yaml)) {

            try {
                List<Object> objs = ServerUtils.getYamlObjects(yaml);

                if (CollectionUtils.isNotEmpty(objs)) {
                    JSON k8sJson = new JSON();

                    for (Object obj : objs) {
                        Map<String, Object> k8sObjectToMap = ServerUtils.getK8sObjectToMap(obj, k8sJson);
                        K8sApiKindType kind = ServerUtils.getK8sKindInMap(k8sObjectToMap);
                        K8sApiGroupType apiGroupType = ServerUtils.getK8sGroupInMap(k8sObjectToMap);
                        K8sApiType apiType = ServerUtils.getK8sVersionInMap(k8sObjectToMap);

                        switch (kind) {
                            case DEPLOYMENT:
                                currDeployment = (V1Deployment) obj;
                                labels = Optional.ofNullable(currDeployment).map(V1Deployment::getMetadata).map(V1ObjectMeta::getLabels).orElseGet(() ->Maps.newHashMap());
                                annotations = Optional.ofNullable(currDeployment).map(V1Deployment::getMetadata).map(V1ObjectMeta::getAnnotations).orElseGet(() ->Maps.newHashMap());
                                templateLabels = Optional.ofNullable(currDeployment).map(V1Deployment::getSpec).map(V1DeploymentSpec::getTemplate).map(V1PodTemplateSpec::getMetadata).map(V1ObjectMeta::getAnnotations).orElseGet(() ->Maps.newHashMap());
                                break;
                            case DAEMON_SET:
                                currDaemonSet = (V1DaemonSet) obj;
                                labels = Optional.ofNullable(currDaemonSet).map(V1DaemonSet::getMetadata).map(V1ObjectMeta::getLabels).orElseGet(() ->Maps.newHashMap());
                                annotations = Optional.ofNullable(currDaemonSet).map(V1DaemonSet::getMetadata).map(V1ObjectMeta::getAnnotations).orElseGet(() ->Maps.newHashMap());
                                templateLabels = Optional.ofNullable(currDaemonSet).map(V1DaemonSet::getSpec).map(V1DaemonSetSpec::getTemplate).map(V1PodTemplateSpec::getMetadata).map(V1ObjectMeta::getAnnotations).orElseGet(() ->Maps.newHashMap());
                                break;
                            case JOB:
                                currJob = (V1Job) obj;
                                labels = Optional.ofNullable(currJob).map(V1Job::getMetadata).map(V1ObjectMeta::getLabels).orElseGet(() ->Maps.newHashMap());
                                annotations = Optional.ofNullable(currJob).map(V1Job::getMetadata).map(V1ObjectMeta::getAnnotations).orElseGet(() ->Maps.newHashMap());
                                templateLabels = Optional.ofNullable(currJob).map(V1Job::getSpec).map(V1JobSpec::getTemplate).map(V1PodTemplateSpec::getMetadata).map(V1ObjectMeta::getAnnotations).orElseGet(() ->Maps.newHashMap());
                                break;
                            case CRON_JOB:
                                if (apiGroupType == K8sApiGroupType.BATCH && apiType == K8sApiType.V1BETA1) {
                                    currCronJobV1beta1 = (V1beta1CronJob) obj;
                                    labels = Optional.ofNullable(currCronJobV1beta1).map(V1beta1CronJob::getMetadata).map(V1ObjectMeta::getLabels).orElseGet(() -> Maps.newHashMap());
                                    annotations = Optional.ofNullable(currCronJobV1beta1).map(V1beta1CronJob::getMetadata).map(V1ObjectMeta::getAnnotations).orElseGet(() -> Maps.newHashMap());
                                    templateLabels = Optional.ofNullable(currCronJobV1beta1).map(V1beta1CronJob::getSpec).map(V1beta1CronJobSpec::getJobTemplate).map(V1beta1JobTemplateSpec::getMetadata).map(V1ObjectMeta::getAnnotations).orElseGet(() -> Maps.newHashMap());
                                } else if (apiGroupType == K8sApiGroupType.BATCH && apiType == K8sApiType.V1) {
                                    currCronJobV1 = (V1CronJob) obj;
                                    labels = Optional.ofNullable(currCronJobV1).map(V1CronJob::getMetadata).map(V1ObjectMeta::getLabels).orElseGet(() -> Maps.newHashMap());
                                    annotations = Optional.ofNullable(currCronJobV1).map(V1CronJob::getMetadata).map(V1ObjectMeta::getAnnotations).orElseGet(() -> Maps.newHashMap());
                                    templateLabels = Optional.ofNullable(currCronJobV1).map(V1CronJob::getSpec).map(V1CronJobSpec::getJobTemplate).map(V1JobTemplateSpec::getMetadata).map(V1ObjectMeta::getAnnotations).orElseGet(() -> Maps.newHashMap());
                                }
                                break;
                            case STATEFUL_SET:
                                currStatefulSet = (V1StatefulSet) obj;
                                labels = Optional.ofNullable(currStatefulSet).map(V1StatefulSet::getMetadata).map(V1ObjectMeta::getLabels).orElseGet(() ->Maps.newHashMap());
                                annotations = Optional.ofNullable(currStatefulSet).map(V1StatefulSet::getMetadata).map(V1ObjectMeta::getAnnotations).orElseGet(() ->Maps.newHashMap());
                                templateLabels = Optional.ofNullable(currStatefulSet).map(V1StatefulSet::getSpec).map(V1StatefulSetSpec::getTemplate).map(V1PodTemplateSpec::getMetadata).map(V1ObjectMeta::getAnnotations).orElseGet(() ->Maps.newHashMap());
                                break;
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Fail to extract a template labels of yaml!!", e);
            }
        }

        Map<String, Map<String, String>> response = new HashMap<>();
        response.put(KubeConstants.META_LABELS, labels);
        response.put(KubeConstants.META_ANNOTATIONS, annotations);
        response.put("templateLabels", templateLabels);

        return response;
    }

    private Integer getInteger(String param) throws Exception {
        if(StringUtils.isBlank(param)) {
            return null;
        }
        Integer val = null;
        if (Pattern.matches("^[0-9]+$", param)) {
            val = Integer.valueOf(param);
        }
        return val;
    }

}
