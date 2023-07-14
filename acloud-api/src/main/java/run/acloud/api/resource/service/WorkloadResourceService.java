package run.acloud.api.resource.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import io.kubernetes.client.custom.Quantity;
import io.kubernetes.client.custom.QuantityFormatter;
import io.kubernetes.client.openapi.JSON;
import io.kubernetes.client.openapi.models.*;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import run.acloud.api.configuration.dao.IClusterMapper;
import run.acloud.api.configuration.vo.ClusterVO;
import run.acloud.api.cserver.enums.WorkloadType;
import run.acloud.api.cserver.service.ServicemapService;
import run.acloud.api.cserver.vo.ServicemapSummaryVO;
import run.acloud.api.k8sextended.models.*;
import run.acloud.api.k8sextended.util.Yaml;
import run.acloud.api.resource.constants.KubeConstants;
import run.acloud.api.resource.enums.*;
import run.acloud.api.resource.task.K8sPatchSpecFactory;
import run.acloud.api.resource.task.K8sSpecFactory;
import run.acloud.api.resource.task.K8sWorker;
import run.acloud.api.resource.util.K8sJsonUtils;
import run.acloud.api.resource.util.K8sMapperUtils;
import run.acloud.api.resource.util.ResourceUtil;
import run.acloud.api.resource.vo.*;
import run.acloud.commons.util.JsonUtils;
import run.acloud.commons.util.Utils;
import run.acloud.commons.vo.ExecutingContextVO;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class WorkloadResourceService {
    private static final Integer MAX_TAIL_COUNT = 10000;
//    private static final String COCKTAIL_REGISTRY_SECRET = "cocktail-registry-secret";

    @Resource(name = "cocktailSession")
    private SqlSessionTemplate sqlSession;

    @Autowired
    private ServicemapService servicemapService;

    @Autowired
    private K8sWorker k8sWorker;

    @Autowired
    private K8sPatchSpecFactory k8sPatchSpecFactory;

    @Autowired
    private K8sResourceService k8sResourceService;

    @Autowired
    private CRDResourceService crdService;


    /**
     * K8S POD 정보 조회
     * (cluster > node > pod)
     * (cluster > namespace > pod)
     *
     * @param clusterSeq
     * @param nodeName
     * @param namespaceName
     * @param context
     * @return
     * @throws Exception
     */
    public List<K8sPodVO> getPods(Integer clusterSeq, String nodeName, String namespaceName, ExecutingContextVO context) throws Exception{
        return this.getPods(clusterSeq, nodeName, namespaceName, null, context);
    }

    /**
     * CLUSTER QUOTA 기준에 따른 Pod 목록 조회..
     * @param clusterSeq : Pod가 속해있는 Cluster의 Sequence
     * @param nodeName : Pod가 속해있는 Node의 이름
     * @param serviceSeq : 현재 사용중인 serviceSeq(워크스페이스 Sequence)
     * @param context : ExecutingContextVO
     * @return
     * @throws Exception
     */
    public List<K8sPodVO> getPods(Integer clusterSeq, String nodeName, Integer serviceSeq, ExecutingContextVO context) throws Exception{
        /** service_cluster의 할당 유형 조회 **/
        IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
        if (clusterSeq == null) {
            throw new CocktailException("Missing the required parameter 'clusterSeq' when calling getPods", ExceptionType.InvalidParameter);
        }

        List<ClusterVO> clusters = clusterDao.getClusters(null, serviceSeq, clusterSeq, null, null, "Y");
        if(clusters == null || clusters.size() < 1) {
            throw new CocktailException("The cluster is not assigned to a current service..", ExceptionType.InvalidParameter);
        }
        if(clusters.size() > 1) {
            throw new CocktailException("The cluster is multiple assigned to a current service..", ExceptionType.InvalidParameter);
        }

        /** Cluster Node내의 모든 Pod List 조회.. (The node can be null) */
        String labels = null;
        List<K8sPodVO> pods =  this.getPods(clusterSeq, nodeName, null, labels, context);

        /** Pods 응답 객체 생성 **/
        List<K8sPodVO> newPods = new ArrayList<>();

        /** 조회된 pods에서 service_cluster의 할당 유형에 맞는 pod만 걸러냄.. */
        if(CollectionUtils.isNotEmpty(pods)) {
            /** 클러스터 내의 전체 Appmap 연결 정보를 조회 (할당 유형 CLUSTER Type을 처리하기 위함)**/
            Map<String, ServicemapSummaryVO> servicemapInfoMap = servicemapService.getServicemapSummaryMap(clusterSeq);

            for (K8sPodVO pod : pods) {
                /** 조회한 POD의 워크스페이스 클러스터 네임스페이스등 연결 정보를 설정 **/
                if (MapUtils.isNotEmpty(servicemapInfoMap) && servicemapInfoMap.containsKey(pod.getNamespace())) {
                    pod.setServicemapInfo(servicemapInfoMap.get(pod.getNamespace()));
                }

                newPods.add(pod);
            }
        }

        log.debug("::::::::::::::::::::::::::::::::::::::::::::: {}", Optional.ofNullable(newPods).map(List::size).orElseGet(() ->0));
        return newPods;
    }

    /**
     * K8S POD 정보 조회
     * (cluster > node > pod)
     * (cluster > namespace > pod)
     *
     * @param clusterSeq
     * @param nodeName
     * @param namespaceName
     * @param label
     * @param context
     * @return
     * @throws Exception
     */
    public List<K8sPodVO> getPods(Integer clusterSeq, String nodeName, String namespaceName, String label, ExecutingContextVO context) throws Exception{
        try {
            IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
            ClusterVO cluster = clusterDao.getCluster(clusterSeq);

            return this.getPods(cluster, nodeName, namespaceName, label, context);
        } catch (Exception e) {
            throw new CocktailException("getPods fail!!", e, ExceptionType.K8sCocktailCloudInquireFail);
        }
    }

    public List<K8sPodVO> getPods(ClusterVO cluster, String nodeName, String namespaceName, String label, ExecutingContextVO context) throws Exception {
        return this.getPods(cluster, nodeName, namespaceName, label, null, context);
    }

    public List<K8sPodVO> getPods(ClusterVO cluster, String nodeName, String namespaceName, String label, Integer limit, ExecutingContextVO context) throws Exception{

        if(cluster != null){
            String field = "";
            if(StringUtils.isNotBlank(nodeName)){
                field = String.format("%s.%s=%s", KubeConstants.SPEC, KubeConstants.SPEC_NODE_NAME, nodeName);
            }else if (StringUtils.isNotBlank(namespaceName)){
                field = String.format("%s.%s=%s", KubeConstants.META, KubeConstants.META_NAMESPACE, namespaceName);
            }

            return this.getPods(cluster, field, label, limit);
        }else{
            throw new CocktailException("cluster is null.", ExceptionType.InvalidParameter);
        }
    }

    public List<K8sPodVO> getPods(ClusterVO cluster, String field, String label) throws Exception{
        return this.getPods(cluster, field, label, null);
    }

    public List<K8sPodVO> getPods(ClusterVO cluster, String field, String label, Integer limit) throws Exception{

        if(cluster != null){
            return this.convertPodDataList(cluster, field, label, limit);
        }else{
            throw new CocktailException("cluster is null.", ExceptionType.InvalidParameter);
        }
    }

    public List<K8sPodVO> getPods(ClusterVO cluster, String namespace, String field, String label, Integer limit) throws Exception{
        List<K8sPodVO> pods = new ArrayList<>();
        if(cluster != null){
            List<V1Pod> v1Pods = k8sWorker.getPodV1(cluster, namespace, null, field, label, limit);

            if(CollectionUtils.isNotEmpty(v1Pods)){
                this.genPodDataList(pods, v1Pods, null, label);
            }
        }else{
            throw new CocktailException("cluster is null.", ExceptionType.InvalidParameter);
        }
        return pods;
    }

    public List<K8sPodVO> convertPodDataList(ClusterVO cluster, String field, String label) throws Exception{
        return this.convertPodDataList(cluster, null, field, label);
    }

    public List<K8sPodVO> convertPodDataList(ClusterVO cluster, String field, String label, Integer limit) throws Exception{
        return this.convertPodDataList(cluster, null, field, label, limit);
    }

    /**
     * K8S POD 정보 조회 후 V1Pod -> K8sPodVO 변환
     * (Namespace > pod)
     *
     * @param cluster
     * @param label
     * @return
     * @throws Exception
     */
    public List<K8sPodVO> convertPodDataList(ClusterVO cluster, Integer componentSeq, String field, String label) throws Exception{
        return this.convertPodDataList(cluster, componentSeq, field, label, null);
    }

    public List<K8sPodVO> convertPodDataList(ClusterVO cluster, Integer componentSeq, String field, String label, Integer limit) throws Exception{

        List<K8sPodVO> pods = new ArrayList<>();

        try {
            HttpServletRequest request = Utils.getCurrentRequest();

            if(componentSeq != null){
                List<V1Pod> v1Pods = k8sWorker.getPodV1(cluster, cluster.getNamespaceName(), null, null, label, limit);

                if(CollectionUtils.isNotEmpty(v1Pods)){
                    this.genPodDataList(pods, v1Pods, null, label);
                }
            }else{
                List<V1Pod> v1Pods = k8sWorker.getPodV1(cluster, null, null, field, label, limit);

                if(CollectionUtils.isNotEmpty(v1Pods)){
                    this.genPodDataList(pods, v1Pods, field, label);
                }
            }
        } catch (Exception e) {
            throw new CocktailException("convertPodDataList fail!!", e, ExceptionType.K8sCocktailCloudInquireFail);
        }

        return pods;
    }

    public List<K8sPodVO> genPodDataList(List<K8sPodVO> pods, List<V1Pod> v1Pods, String field, String label) throws Exception {

        // joda.datetime Serialization
        JSON k8sJson = new JSON();

        for(V1Pod v1Pod : v1Pods){
            pods.add(this.genPodData(v1Pod, field, label, k8sJson, false));
        }

        return pods;
    }

    public K8sPodVO genPodData(V1Pod v1Pod, String field, String label, JSON k8sJson, boolean isSpec) throws Exception {

        if (k8sJson == null) {
            k8sJson = new JSON();
        }

        int restartCnt = 0;
        K8sPodVO pod = new K8sPodVO();
        // Pod 목록
        pod.setLabel(label);
        pod.setNodeName(v1Pod.getSpec().getNodeName());
        if (!isSpec) {
            pod.setNamespace(v1Pod.getMetadata().getNamespace());
            pod.setPodName(v1Pod.getMetadata().getName());
            pod.setPodStatus(v1Pod.getStatus().getPhase());
            pod.setStartTime(v1Pod.getStatus().getStartTime());
            pod.setPodDeployment(k8sJson.serialize(v1Pod));
            pod.setPodDeploymentYaml(Yaml.getSnakeYaml().dumpAsMap(v1Pod));
        }

        boolean podReadyCondition = false;

        // Pod Condition
        List<K8sConditionVO> podConditions = new ArrayList<>();
        for(V1PodCondition podConditionRow : Optional.ofNullable(v1Pod.getStatus()).map(V1PodStatus::getConditions).orElseGet(() ->Lists.newArrayList())) {
            K8sConditionVO podConditionTemp = new K8sConditionVO();
            BeanUtils.copyProperties(podConditionTemp, podConditionRow);
            if (podConditionRow.getLastProbeTime() != null) {
                podConditionTemp.setLastProbeTime(podConditionRow.getLastProbeTime());
            }
            if (podConditionRow.getLastTransitionTime() != null) {
                podConditionTemp.setLastTransitionTime(podConditionRow.getLastTransitionTime());
            }

            podConditions.add(podConditionTemp);

            // Pod Status
            if (StringUtils.equalsIgnoreCase("Ready", podConditionRow.getType())) {
                podReadyCondition = StringUtils.equalsIgnoreCase("True", podConditionRow.getStatus());
            }
        }
        pod.setConditions(podConditions);

        Map<String, V1ContainerStatus> containerStatusMap = new HashMap<>();
        List<V1ContainerStatus> initContainerStatuses = Optional.ofNullable(v1Pod.getStatus()).map(V1PodStatus::getInitContainerStatuses).orElseGet(() ->Lists.newArrayList());
        for(V1ContainerStatus containerStatus : initContainerStatuses){
            containerStatusMap.put(containerStatus.getName(), containerStatus);
        }
        for(V1ContainerStatus containerStatus : Optional.ofNullable(v1Pod.getStatus()).map(V1PodStatus::getContainerStatuses).orElseGet(() ->Lists.newArrayList())){
            containerStatusMap.put(containerStatus.getName(), containerStatus);
        }

        // Pod Status
        int totalContainers = CollectionUtils.size(v1Pod.getSpec().getContainers());
        int readyContainers = 0;
        String reason = "Pending";
        if (v1Pod.getStatus() != null) {
            reason = v1Pod.getStatus().getPhase();
            if (StringUtils.isNotBlank(v1Pod.getStatus().getReason())) {
                reason = v1Pod.getStatus().getReason();
            }

            boolean initializing = false;
            boolean hasRunning = false;
            Integer zero = Integer.valueOf(0);

            for(int i = 0, ie = initContainerStatuses.size(); i < ie; i++){

                restartCnt += initContainerStatuses.get(i).getRestartCount();

                if (initContainerStatuses.get(i).getState().getTerminated() != null && zero.equals(initContainerStatuses.get(i).getState().getTerminated().getExitCode())) {
                    continue;
                } else if (initContainerStatuses.get(i).getState().getTerminated() != null) {
                    // initialization is failed
                    if (StringUtils.isNotBlank(initContainerStatuses.get(i).getState().getTerminated().getReason())) {
                        if (!zero.equals(initContainerStatuses.get(i).getState().getTerminated().getSignal())) {
                            reason = String.format("Init:Signal:%d", initContainerStatuses.get(i).getState().getTerminated().getSignal());
                        } else {
                            reason = String.format("Init:ExitCode:%d", initContainerStatuses.get(i).getState().getTerminated().getExitCode());
                        }
                    } else {
                        reason = String.format("Init:%s", initContainerStatuses.get(i).getState().getTerminated().getReason());
                    }
                    initializing = true;
                } else if (initContainerStatuses.get(i).getState().getWaiting() != null && StringUtils.isNotBlank(initContainerStatuses.get(i).getState().getWaiting().getReason())) {
                    reason = String.format("Init:%s", initContainerStatuses.get(i).getState().getWaiting().getReason());
                    initializing = true;
                } else {
                    reason = String.format("Init:%d/%d", i, CollectionUtils.size(initContainerStatuses));
                    initializing = true;
                }
                break;
            }
            if (!initializing) {
                restartCnt = 0;

                for(V1ContainerStatus containerStatus : Optional.ofNullable(v1Pod.getStatus()).map(V1PodStatus::getContainerStatuses).orElseGet(() ->Lists.newArrayList())){
                    restartCnt += containerStatus.getRestartCount();

                    if (containerStatus.getState().getWaiting() != null && StringUtils.isNotBlank(containerStatus.getState().getWaiting().getReason())) {
                        reason = containerStatus.getState().getWaiting().getReason();
                    } else if (containerStatus.getState().getTerminated() != null && StringUtils.isNotBlank(containerStatus.getState().getTerminated().getReason())) {
                        reason = containerStatus.getState().getTerminated().getReason();
                    } else if (containerStatus.getState().getTerminated() != null && StringUtils.isBlank(containerStatus.getState().getTerminated().getReason())) {
                        if (!zero.equals(containerStatus.getState().getTerminated().getSignal())) {
                            reason = String.format("Signal:%d", containerStatus.getState().getTerminated().getSignal());
                        } else {
                            reason = String.format("ExitCode:%d", containerStatus.getState().getTerminated().getExitCode());
                        }
                    } else if (!containerStatus.getReady() && containerStatus.getState().getRunning() != null) {
                        reason = "Pending";
                    } else if (containerStatus.getReady() && containerStatus.getState().getRunning() != null) {
                        hasRunning = true;
                        readyContainers++;
                    }
                }

                // change pod status back to "Running" if there is at least one container still reporting as "Running" status
                if (StringUtils.equalsIgnoreCase("Completed", reason) && hasRunning) {
                    if (podReadyCondition) {
                        reason = "Running";
                    } else {
                        reason = "NotReady";
                    }
                }
            }

            String nodeUnreachablePodReason = "NodeLost";
            if (v1Pod.getMetadata().getDeletionTimestamp() != null && StringUtils.equalsIgnoreCase(nodeUnreachablePodReason, v1Pod.getStatus().getReason())) {
                reason = "Unknown";
            } else if (v1Pod.getMetadata().getDeletionTimestamp() != null) {
                reason = "Terminating";
            }

        }

        pod.setPodStatus(reason);

        pod.setRestartCnt(restartCnt);

        K8sPodDetailVO detail = new K8sPodDetailVO();
        // Pod Details
        detail.setNodeName(v1Pod.getSpec().getNodeName());
        detail.setRestartPolicy(v1Pod.getSpec().getRestartPolicy());
        detail.setTerminationGracePeriodSeconds(v1Pod.getSpec().getTerminationGracePeriodSeconds());
        detail.setNodeSelector(v1Pod.getSpec().getNodeSelector());
        detail.setServiceAccountName(v1Pod.getSpec().getServiceAccountName());
        detail.setHostname(v1Pod.getSpec().getHostname());
        String tolerationsJson = k8sJson.serialize(v1Pod.getSpec().getTolerations());
        detail.setTolerations(k8sJson.getGson().fromJson(tolerationsJson, new TypeToken<List<TolerationVO>>(){}.getType()));
        String affinityJson = k8sJson.serialize(v1Pod.getSpec().getAffinity());
        detail.setAffinity(k8sJson.getGson().fromJson(affinityJson, new TypeToken<AffinityVO>(){}.getType()));
        if (CollectionUtils.isNotEmpty(v1Pod.getSpec().getImagePullSecrets())) {
            detail.setImagePullSecrets(k8sJson.deserialize(k8sJson.serialize(v1Pod.getSpec().getImagePullSecrets()), new TypeToken<List<LocalObjectReferenceVO>>(){}.getType()));
        }

        if (!isSpec) {
            detail.setPodName(v1Pod.getMetadata().getName());
            detail.setNamespace(v1Pod.getMetadata().getNamespace());
            detail.setLabels(v1Pod.getMetadata().getLabels());
            detail.setAnnotations(v1Pod.getMetadata().getAnnotations());
            detail.setPodStatus(v1Pod.getStatus().getPhase());
            detail.setCreationTime(v1Pod.getMetadata().getCreationTimestamp());
            detail.setStartTime(v1Pod.getStatus().getStartTime());
            detail.setQosClass(v1Pod.getStatus().getQosClass());
            // Pod Network
            detail.setPodIP(v1Pod.getStatus().getPodIP());
            // ownerReference
            if (CollectionUtils.isNotEmpty(v1Pod.getMetadata().getOwnerReferences())) {
                detail.setOwnerReferences(ResourceUtil.setOwnerReference(v1Pod.getMetadata().getOwnerReferences()));
            }
        }

        // Pod Container
        List<K8sContainerVO> initContainers = new ArrayList<>();
        if(CollectionUtils.isNotEmpty(v1Pod.getSpec().getInitContainers())){
            for(V1Container v1ContainerRow : v1Pod.getSpec().getInitContainers()){
                initContainers.add(this.setContainer(v1ContainerRow, containerStatusMap, k8sJson));
            }
        }
        detail.setInitContainers(initContainers);
        // Pod Container
        List<K8sContainerVO> containers = new ArrayList<>();
        if(CollectionUtils.isNotEmpty(v1Pod.getSpec().getContainers())){
            for(V1Container v1ContainerRow : v1Pod.getSpec().getContainers()){
                containers.add(this.setContainer(v1ContainerRow, containerStatusMap, k8sJson));
            }
        }
        detail.setContainers(containers);

        pod.setDetail(detail);

        return pod;
    }

    private K8sContainerVO setContainer(V1Container v1Container, Map<String, V1ContainerStatus> containerStatusMap, JSON k8sJson) {
        K8sContainerVO container = new K8sContainerVO();
        container.setName(v1Container.getName());
        container.setImage(v1Container.getImage());
        container.setCommands(v1Container.getCommand());
        container.setArgs(v1Container.getArgs());
        container.setImagePullPolicy(v1Container.getImagePullPolicy());

        // env
        if(CollectionUtils.isNotEmpty(v1Container.getEnv())){
            container.setEnvironmentVariables(new HashMap<>());
            for(V1EnvVar envVar : v1Container.getEnv()){
                if(envVar.getValueFrom() != null){
                    if(envVar.getValueFrom().getConfigMapKeyRef() != null){
                        container.getEnvironmentVariables().put(envVar.getName(), k8sJson.serialize(envVar.getValueFrom().getConfigMapKeyRef()));
                    }else if(envVar.getValueFrom().getSecretKeyRef() != null){
                        container.getEnvironmentVariables().put(envVar.getName(), k8sJson.serialize(envVar.getValueFrom().getSecretKeyRef()));
                    }else if(envVar.getValueFrom().getFieldRef() != null){
                        container.getEnvironmentVariables().put(envVar.getName(), k8sJson.serialize(envVar.getValueFrom().getFieldRef()));
                    }else if(envVar.getValueFrom().getResourceFieldRef() != null){
                        container.getEnvironmentVariables().put(envVar.getName(), k8sJson.serialize(envVar.getValueFrom().getResourceFieldRef()));
                    }
                }else {
                    container.getEnvironmentVariables().put(envVar.getName(), envVar.getValue());
                }
            }
        }

        // resources
        if (v1Container.getResources() != null) {
            container.setResources(new HashMap<>());
            String requestKey = "request";
            String limitKey = "limit";
            if (MapUtils.isNotEmpty(v1Container.getResources().getRequests())) {
                for (Map.Entry<String, Quantity> request : v1Container.getResources().getRequests().entrySet()) {
                    if (MapUtils.getObject(container.getResources(), request.getKey(), null) == null) {
                        container.getResources().put(request.getKey(), new HashMap<>());
                    }
                    container.getResources().get(request.getKey()).put(requestKey, request.getValue().toSuffixedString());

                }
            }
            if (MapUtils.isNotEmpty(v1Container.getResources().getLimits())) {
                for (Map.Entry<String, Quantity> limit : v1Container.getResources().getLimits().entrySet()) {
                    if (MapUtils.getObject(container.getResources(), limit.getKey(), null) == null) {
                        container.getResources().put(limit.getKey(), new HashMap<>());
                    }
                    container.getResources().get(limit.getKey()).put(limitKey, limit.getValue().toSuffixedString());
                }
            }
        }

        // volume mount
        if (CollectionUtils.isNotEmpty(v1Container.getVolumeMounts())) {
            container.setVolumeMounts(new HashMap<>());
            for (V1VolumeMount volumeMount : v1Container.getVolumeMounts()) {
                container.getVolumeMounts().put(volumeMount.getName(), k8sJson.serialize(volumeMount));
            }
        }

        if (MapUtils.getObject(containerStatusMap, v1Container.getName(), null) != null) {
            V1ContainerStatus containerStatus = containerStatusMap.get(v1Container.getName());
            container.setContainerID(containerStatus.getContainerID());
            container.setImageID(containerStatus.getImageID());
            container.setRestartCount(containerStatus.getRestartCount() != null ? containerStatus.getRestartCount().intValue() : 0);
            container.setReady(containerStatus.getReady());
            if (containerStatus.getLastState() != null) {
                container.setLastState(k8sJson.serialize(containerStatus.getLastState()));
            }
            if (containerStatus.getState() != null) {
                container.setState(k8sJson.serialize(containerStatus.getState()));
                if (containerStatus.getState().getRunning() != null) {
                    container.setStartTime(containerStatus.getState().getRunning().getStartedAt());
                } else if (containerStatus.getState().getTerminated() != null) {
                    container.setStartTime(containerStatus.getState().getTerminated().getStartedAt());
                }
            }
        }

        return container;
    }

    public void deleteCollectionNamespacedPod(ClusterVO cluster, String namespace, String fieldSelector, String labelSelector, ExecutingContextVO context) throws Exception {
        if(cluster != null){
            V1Status v1Status = k8sWorker.deleteCollectionNamespacedPodV1(cluster, namespace, fieldSelector, labelSelector);
        }else{
            throw new CocktailException("cluster is null.", ExceptionType.InvalidParameter);
        }
    }

    public void deleteNamespacedPod(Integer clusterSeq, String namespace, String name, Integer gracePeriodSeconds, boolean force) throws Exception {
        if(clusterSeq != null){
            IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);

            ClusterVO cluster = clusterDao.getCluster(clusterSeq);

            this.deleteNamespacedPod(cluster, namespace, name, gracePeriodSeconds, force);
        }else{
            throw new CocktailException("clusterSeq is null.", ExceptionType.InvalidParameter);
        }
    }

    public void deleteNamespacedPod(ClusterVO cluster, String namespace, String name, Integer gracePeriodSeconds, boolean force) throws Exception {
        if(cluster != null){
            V1Pod result = k8sWorker.deleteNamespacedPodV1(cluster, namespace, name, gracePeriodSeconds, force);
        }else{
            throw new CocktailException("cluster is null.", ExceptionType.InvalidParameter);
        }
    }

    public K8sPodTemplateSpecVO setPodTemplateSpec(V1PodTemplateSpec v1PodTemplateSpec, JSON k8sJson) throws Exception {
        if (k8sJson == null) {
            k8sJson = new JSON();
        }
        K8sPodTemplateSpecVO podTemplateSpec = new K8sPodTemplateSpecVO();
        podTemplateSpec.setLabels(Optional.ofNullable(v1PodTemplateSpec).map(V1PodTemplateSpec::getMetadata).map(V1ObjectMeta::getLabels).orElseGet(() ->null));
        podTemplateSpec.setAnnotations(Optional.ofNullable(v1PodTemplateSpec).map(V1PodTemplateSpec::getMetadata).map(V1ObjectMeta::getAnnotations).orElseGet(() ->null));
        podTemplateSpec.setSpec(this.genPodData(new V1Pod().spec(Optional.ofNullable(v1PodTemplateSpec).map(V1PodTemplateSpec::getSpec).orElseGet(() ->new V1PodSpec())), null, null, k8sJson, true));

        return podTemplateSpec;
    }

    /**
     * Pod > Container Log 조회
     *
     * @param clusterSeq
     * @param namespaceName
     * @param podName
     * @param type
     * @param count
     * @param sinceSeconds
     * @return
     * @throws Exception
     */
    public List<PodLogVO> getContainerLogsInPod(Integer clusterSeq, String namespaceName, String podName, String type, String count, String sinceSeconds) throws Exception {
        try {
            if(clusterSeq != null){
                IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);

                ClusterVO cluster = clusterDao.getCluster(clusterSeq);

                if(cluster != null){
                    switch (type) {
                        case "tail": {
                            Integer tailCount = Integer.parseInt(count);
                            if (tailCount > MAX_TAIL_COUNT) {
                                throw new CocktailException(String.format("Tail count exceeded limit: %d", count),
                                        ExceptionType.CubeLogCountInvalid);
                            }
                            return k8sWorker.getContainerLogsInPodV1(cluster, namespaceName, podName, null, null, null, null, false, null, tailCount, false);
                        }

                        case "since": {
                            return k8sWorker.getContainerLogsInPodV1(cluster, namespaceName, podName, null, null, null, null, false, Integer.parseInt(sinceSeconds), null, false);
                        }

                        default: {
                            throw new CocktailException(String.format("Unknown log reading type: %s", type), ExceptionType.CubeLogTypeUnknown);
                        }
                    }
                }else{
                    throw new CocktailException("cluster is null.", ExceptionType.InvalidParameter);
                }
            }else{
                throw new CocktailException("clusterSeq is null.", ExceptionType.InvalidParameter);
            }
        }
        catch (CocktailException ce) {
            throw ce;
        }
        catch (Exception e) {
                throw new CocktailException("getContainerLogsInPod fail!!", e, ExceptionType.K8sCocktailCloudInquireFail);
        }
    }

    public boolean deleteDeployment(ClusterVO cluster, ServerDetailVO serverParam, ExecutingContextVO context) throws Exception {
        String name = ResourceUtil.getUniqueName(serverParam.getComponent().getComponentName());
        log.info("Converted ServerDetailVO INFO: {}", JsonUtils.toGson(serverParam));

        return this.deleteDeployment(cluster, name, context);
    }

    public boolean deleteDeployment(ClusterVO cluster, String name, ExecutingContextVO context) throws Exception {
        K8sApiVerKindType apiVerKindType = ResourceUtil.getApiVerKindType(cluster, K8sApiKindType.DEPLOYMENT);

        if (apiVerKindType != null) {
            if (apiVerKindType.getGroupType() == K8sApiGroupType.APPS && apiVerKindType.getApiType() == K8sApiType.V1) {

                /**
                 * Server Delete
                 */
                log.debug("Server to delete: {}", name);
                // Get Deployment
                V1Deployment v1Deployment = k8sWorker.getDeploymentV1(cluster, cluster.getNamespaceName(), name);

                // Deployment가 존재시 Delete Deployment
                if (v1Deployment != null) {
                    log.debug("### Deployment to delete: {}", v1Deployment.getMetadata().getName());

                    V1Status resultObj = k8sWorker.deleteDeploymentV1(cluster, cluster.getNamespaceName(), name);

                    log.debug("### Deployment to delete status: {}", resultObj);

                }

            } else {
                log.error("fail deleteServer!! ( invalid K8sAPIType : k8sApiType[{}] ) cluster:[{}], name: [{}]", K8sApiType.values(), JsonUtils.toGson(cluster), name);
                /**
                 * Server Delete
                 */
                log.debug("Server to delete: {}", name);
                // Get Deployment
                V1Deployment v1Deployment = k8sWorker.getDeploymentV1(cluster, cluster.getNamespaceName(), name);

                // Deployment가 존재시 Delete Deployment
                if (v1Deployment != null) {
                    log.debug("### Deployment to delete: {}", v1Deployment.getMetadata().getName());

                    V1Status resultObj = k8sWorker.deleteDeploymentV1(cluster, cluster.getNamespaceName(), name);

                    log.debug("### Deployment to delete status: {}", resultObj);

                }
            }
        }

        return true;
    }

    /**
     * Deployment 생성
     *
     * @param cluster
     * @param server
     * @param context
     * @return
     * @throws Exception
     */
    public K8sDeploymentVO createDeployment(ClusterVO cluster, ServerDetailVO server, ExecutingContextVO context) throws Exception {

        // Convert to JsonObject
        log.debug("Converted Server creation request: {}", JsonUtils.toGson(server));

        K8sDeploymentVO k8sDeployment = null;
        K8sApiVerKindType apiVerKindType = ResourceUtil.getApiVerKindType(cluster, K8sApiKindType.DEPLOYMENT);

        if (apiVerKindType != null) {
            if (apiVerKindType.getGroupType() == K8sApiGroupType.APPS && apiVerKindType.getApiType() == K8sApiType.V1) {
                // Create Deployment
                V1Deployment v1Deployment = K8sSpecFactory.buildDeploymentV1(server, cluster.getNamespaceName());
                v1Deployment.getMetadata().putAnnotationsItem(KubeConstants.ANNOTATION_COCKTAIL_DEPLOY_DATETIME, Utils.getNowDateTime());

                // 클러스터 조회하여 istio-injection 어노테이션 확인하기 위해 현재 네임스페이스 정보 획득
                final V1Namespace v1Namespace = k8sWorker.getNamespaceV1(cluster, cluster.getNamespaceName());
                if (v1Namespace != null && this.isServiceMeshMemberRollCustomResourceDefinitions(cluster)) {
                    this.addPodTemplateAnnotations(v1Namespace, this.getV1ObjectMetaOfWorkload().apply(v1Deployment));
                }

                log.debug("########## V1Deployment JSON request: {}", JsonUtils.toGson(v1Deployment));
                v1Deployment = k8sWorker.createDeploymentV1(cluster, cluster.getNamespaceName(), v1Deployment, false);
                log.debug("########## V1Deployment JSON response: {}", JsonUtils.toGson(v1Deployment));
                Thread.sleep(200);

                k8sDeployment = this.getDeployment(cluster, cluster.getNamespaceName(), server.getComponent().getComponentName(), context);
            }
        }

        return k8sDeployment;
    }

    private boolean isServiceMeshMemberRollCustomResourceDefinitions(ClusterVO cluster) {

        try {
            List<?> crds = crdService.readCustomResourceDefinitionRaw(cluster, "servicemeshmemberrolls.maistra.io");
            if (CollectionUtils.isNotEmpty(crds)) {
                return true;
            }
        }
        catch(Exception e) {
            log.error("#### fail servicemeshmemberrolls.maistra.io", e);
        }
        return false;

    }

    private void addPodTemplateAnnotations(V1Namespace namespace, V1ObjectMeta objectMeta) throws Exception {

        Map<String, String> labels = Optional.ofNullable(namespace)
                .map(V1Namespace::getMetadata)
                .map(V1ObjectMeta::getLabels).orElseGet(() -> Maps.newHashMap());

        if (labels.containsKey(KubeConstants.LABELS_ISTIO_INJECTION_KEY)) {
            String istioInjection = labels.get(KubeConstants.LABELS_ISTIO_INJECTION_KEY);
            if (StringUtils.isNotBlank(istioInjection)) {
                if("enabled".equalsIgnoreCase(istioInjection)) {
                    objectMeta.putAnnotationsItem("sidecar.istio.io/inject", "true");
                }
                else {
//                        annotations.remove("sidecar.istio.io/inject");
                    objectMeta.putAnnotationsItem("sidecar.istio.io/inject", "false");
                }
            }
        }
    }

    private Function<Object, V1ObjectMeta> getV1ObjectMetaOfWorkload() throws Exception {

        Function<Object, V1ObjectMeta> objectMeta = w -> {
            if (w != null) {
                if (w instanceof V1Deployment) {
                    return ((V1Deployment)w).getSpec().getTemplate().getMetadata();
                }
                else if (w instanceof V1StatefulSet) {
                    return ((V1StatefulSet)w).getSpec().getTemplate().getMetadata();
                }
                else if (w instanceof V1DaemonSet) {
                    return ((V1DaemonSet)w).getSpec().getTemplate().getMetadata();
                }
                else if (w instanceof V1Job) {
                    return ((V1Job)w).getSpec().getTemplate().getMetadata();
                }
                else if (w instanceof V1beta1CronJob) {
                    return ((V1beta1CronJob)w).getSpec().getJobTemplate().getMetadata();
                }
                else if (w instanceof V1CronJob) {
                    return ((V1CronJob)w).getSpec().getJobTemplate().getMetadata();
                } else {
                    return new V1ObjectMeta();
                }
            } else {
                return new V1ObjectMeta();
            }
        };

        return objectMeta;
    }

    /**
     * Deployment 수정
     *
     * @param cluster
     * @param server
     * @param context
     * @return
     * @throws Exception
     */
    public K8sDeploymentVO updateDeployment(ClusterVO cluster, ServerDetailVO server, ExecutingContextVO context) throws Exception {

        // Convert to JsonObject
        log.debug("Converted Server creation request: {}", JsonUtils.toGson(server));

        K8sDeploymentVO k8sDeployment = null;
        K8sApiVerKindType apiVerKindType = ResourceUtil.getApiVerKindType(cluster, K8sApiKindType.DEPLOYMENT);

        if (apiVerKindType != null) {
            if (apiVerKindType.getGroupType() == K8sApiGroupType.APPS && apiVerKindType.getApiType() == K8sApiType.V1) {

                V1Deployment updatedDeployment = K8sSpecFactory.buildDeploymentV1(server, cluster.getNamespaceName());

                WorkloadType workloadType = WorkloadType.valueOf(server.getServer().getWorkloadType());
                if (workloadType == WorkloadType.REPLICA_SERVER) {
                    // replace
                    k8sWorker.replaceDeploymentV1(cluster, cluster.getNamespaceName(), server.getComponent().getComponentName(), updatedDeployment);
                } else if (workloadType == WorkloadType.SINGLE_SERVER) {
                    // 현재 deployment 조회
                    V1Deployment currentDeployment = k8sWorker.getDeploymentV1(cluster, cluster.getNamespaceName(), server.getComponent().getComponentName());

                    // patchJson 으로 변경
                    List<JsonObject> patchBody = k8sPatchSpecFactory.buildPatch(currentDeployment, updatedDeployment);
                    log.debug("########## Deployment patchBody JSON: {}", JsonUtils.toGson(patchBody));
                    // patch
                    k8sWorker.patchDeploymentV1(cluster, cluster.getNamespaceName(), server.getComponent().getComponentName(), patchBody, false);
                }

                Thread.sleep(100);

                k8sDeployment = this.getDeployment(cluster, cluster.getNamespaceName(), server.getComponent().getComponentName(), context);
            }
        }

        return k8sDeployment;
    }

    /**
     * Deployment 수정
     *
     * @param cluster
     * @param namespace
     * @param updatedDeployment
     * @param hasHPA
     * @param context
     * @throws Exception
     */
    public void patchDeploymentV1(ClusterVO cluster, String namespace, V1Deployment updatedDeployment, boolean hasHPA, ExecutingContextVO context) throws Exception {

        // 현재 deployment 조회
        V1Deployment currentDeployment = k8sWorker.getDeploymentV1(cluster, namespace, updatedDeployment.getMetadata().getName());
        if (hasHPA && currentDeployment != null && currentDeployment.getSpec() != null) {
            currentDeployment.getSpec().setReplicas(null);
        }
        if (hasHPA && updatedDeployment != null && updatedDeployment.getSpec() != null) {
            updatedDeployment.getSpec().setReplicas(null);
        }
        currentDeployment.setStatus(null);

        // patchJson 으로 변경
        List<JsonObject> patchBody = k8sPatchSpecFactory.buildPatch(currentDeployment, updatedDeployment);
        log.debug("########## Deployment patchBody JSON: {}", JsonUtils.toGson(patchBody));
        // patch
        k8sWorker.patchDeploymentV1(cluster, namespace, updatedDeployment.getMetadata().getName(), patchBody, false);
    }

    /**
     * Deployment 수정
     *
     * @param cluster
     * @param namespace
     * @param currentDeployment
     * @param updatedDeployment
     * @param dryRun
     * @param hasHPA
     * @param context
     * @throws Exception
     */
    public void patchDeploymentV1(ClusterVO cluster, String namespace, V1Deployment currentDeployment, V1Deployment updatedDeployment, boolean dryRun, boolean hasHPA, ExecutingContextVO context) throws Exception {

        // 현재 deployment 조회
        if (hasHPA && currentDeployment != null && currentDeployment.getSpec() != null) {
            currentDeployment.getSpec().setReplicas(null);
        }
        if (hasHPA && updatedDeployment != null && updatedDeployment.getSpec() != null) {
            updatedDeployment.getSpec().setReplicas(null);
        }
        currentDeployment.setStatus(null);
        updatedDeployment.setStatus(null);

        // patchJson 으로 변경
        List<JsonObject> patchBody = k8sPatchSpecFactory.buildPatch(currentDeployment, updatedDeployment);
        log.debug("########## Deployment patchBody JSON: {}", JsonUtils.toGson(patchBody));
        // patch
        k8sWorker.patchDeploymentV1(cluster, namespace, updatedDeployment.getMetadata().getName(), patchBody, dryRun);
    }

    /**
     * Deployment 수정
     *
     * @param cluster
     * @param updatedDeployment
     * @param context
     * @return
     * @throws Exception
     */
    public void replacehDeploymentV1(ClusterVO cluster, String namespace, V1Deployment updatedDeployment, ExecutingContextVO context) throws Exception {
        k8sWorker.replaceDeploymentV1(cluster, namespace, updatedDeployment.getMetadata().getName(), updatedDeployment);
    }

    /**
     * Patch Deployment Scale
     *
     * @param cluster
     * @param namespace
     * @param workloadName
     * @param scale
     * @param context
     * @throws Exception
     */
    public void patchDeploymentScale(ClusterVO cluster, String namespace, String workloadName, int scale, boolean dryRun, ExecutingContextVO context) throws Exception{

        K8sApiVerKindType apiVerKindType = ResourceUtil.getApiVerKindType(cluster, K8sApiKindType.DEPLOYMENT);

        if (apiVerKindType != null) {
            if (apiVerKindType.getGroupType() == K8sApiGroupType.APPS && apiVerKindType.getApiType() == K8sApiType.V1) {
                List<JsonObject> patchBody = k8sPatchSpecFactory.buildPatchDeploymentScaleV1(scale);
                V1Scale v1Scale = k8sWorker.patchDeploymentScaleV1(cluster, namespace, ResourceUtil.getUniqueName(workloadName), patchBody, dryRun);
                Thread.sleep(100);

            } else {
                List<JsonObject> patchBody = k8sPatchSpecFactory.buildPatchDeploymentScaleV1(scale);
                V1Scale v1Scale = k8sWorker.patchDeploymentScaleV1(cluster, namespace, ResourceUtil.getUniqueName(workloadName), patchBody, dryRun);
                Thread.sleep(100);
            }
        }
    }

    /**
     * K8S Deployment 정보 조회
     *
     * @param cluster
     * @param namespace
     * @param fieldSelector
     * @param labelSelector
     * @param context
     * @return
     * @throws Exception
     */
    public List<K8sDeploymentVO> getDeployments(ClusterVO cluster, String namespace, String fieldSelector, String labelSelector, ExecutingContextVO context) throws Exception{

        try {
            if(cluster != null){

                List<K8sDeploymentVO> k8sDeployments = this.convertDeploymentDataList(cluster, namespace, fieldSelector, labelSelector, context);

                if(CollectionUtils.isNotEmpty(k8sDeployments)){
                    return k8sDeployments;
                }else{
                    return new ArrayList<>();
                }
            }else{
                throw new CocktailException("cluster is null.", ExceptionType.InvalidParameter);
            }
        } catch (Exception e) {
            if(log.isDebugEnabled()) log.debug("trace log ", e);
            throw new CocktailException("getDeployments fail!!", e, ExceptionType.K8sCocktailCloudInquireFail);
        }
    }

    /**
     * K8S Deployment 정보 조회
     *
     * @param clusterSeq
     * @param namespace
     * @param name
     * @param context
     * @return
     * @throws Exception
     */
    public K8sDeploymentVO getDeployment(Integer clusterSeq, String namespace, String name, ExecutingContextVO context) throws Exception {
        try {
            if (clusterSeq != null) {
                IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);

                ClusterVO cluster = clusterDao.getCluster(clusterSeq);

                if (cluster != null) {
                    return this.getDeployment(cluster, namespace, name, context);
                }
                else {
                    throw new CocktailException("cluster not found.", ExceptionType.InvalidParameter);
                }
            }
            else {
                throw new CocktailException("cluster not found.", ExceptionType.InvalidParameter);
            }
        } catch (CocktailException e) {
            throw new CocktailException("getDeployment fail!!", e, ExceptionType.K8sCocktailCloudInquireFail);
        }
    }

    /**
     * K8S Deployment 정보 조회
     *
     * @param cluster
     * @param namespace
     * @param name
     * @param context
     * @return
     * @throws Exception
     */
    public K8sDeploymentVO getDeployment(ClusterVO cluster, String namespace, String name, ExecutingContextVO context) throws Exception{

        try {
            if(cluster != null && StringUtils.isNotBlank(namespace) && StringUtils.isNotBlank(name)){
                String field = String.format("%s.%s=%s", KubeConstants.META, KubeConstants.NAME, name);

                // Parameter로 입력된 Namespace 정보를 사용하도록 처리 변경함. : 2019.06.17 : Redion
                // List<K8sDeploymentVO> k8sDeployments = this.convertDeploymentDataList(cluster, cluster.getNamespaceName(), field, null, context);
                List<K8sDeploymentVO> k8sDeployments = this.convertDeploymentDataList(cluster, namespace, field, null, context);

                if(CollectionUtils.isNotEmpty(k8sDeployments)){
                    return k8sDeployments.get(0);
                }else{
                    return null;
                }
            }else{
                throw new CocktailException("cluster/namespace/name is null.", ExceptionType.InvalidParameter);
            }
        } catch (CocktailException e) {
            throw new CocktailException("getDeployment fail!!", e, ExceptionType.K8sCocktailCloudInquireFail);
        }
    }

    /**
     * K8S Deployment 정보 조회
     *
     * @param clusterSeq
     * @param namespaceName
     * @return
     * @throws Exception
     */
    public List<K8sDeploymentVO> getDeploymentsByCluster(Integer clusterSeq, String namespaceName, ExecutingContextVO context) throws Exception{

        try {
            if(clusterSeq != null){
                IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);

                ClusterVO cluster = clusterDao.getCluster(clusterSeq);

                if(cluster != null){
                    return this.getDeployments(cluster, namespaceName, null, null, context);
                }else{
                    throw new CocktailException("cluster is null.", ExceptionType.InvalidParameter);
                }
            }else{
                throw new CocktailException("componentSeq is null.", ExceptionType.InvalidParameter);
            }
        } catch (Exception e) {
            throw new CocktailException("getDeploymentsByCluster fail!!", e, ExceptionType.K8sCocktailCloudInquireFail);
        }
    }

    /**
     * K8S Deployment 정보 조회 후,
     *
     * - V1BETA1
     * AppsV1beta1Deployment -> K8sDeploymentVO 변환
     *
     * - V1BETA2
     * V1beta2Deployment -> K8sDeploymentVO 변환
     *
     * - V1
     * V1Deployment -> K8sDeploymentVO 변환
     *
     * @param cluster
     * @param label
     * @return
     * @throws Exception
     */
    public List<K8sDeploymentVO> convertDeploymentDataList(ClusterVO cluster, String namespace, String field, String label, ExecutingContextVO context) throws Exception {

        List<K8sDeploymentVO> deployments = new ArrayList<>();

        if (cluster != null) {
            K8sApiVerKindType apiVerKindType = ResourceUtil.getApiVerKindType(cluster, K8sApiKindType.DEPLOYMENT);

            if (apiVerKindType != null) {
                K8sDeploymentVO deployment = null;
                Map<String, Integer> statusMap = null;
                List<K8sReplicaSetVO> replicaSets = null;
                Integer readyReplicas = 0;

                ObjectMapper mapper = K8sMapperUtils.getMapper();

                // joda.datetime Serialization
                JSON k8sJson = new JSON();

                if (apiVerKindType.getGroupType() == K8sApiGroupType.APPS && apiVerKindType.getApiType() == K8sApiType.V1BETA1) {
                    // Deployment 조회
                    List<AppsV1beta1Deployment> v1beta1Deployments = k8sWorker.getDeploymentsV1beta1(cluster, namespace, field, label);
                    // ReplicaSet 조회
                    List<V1beta1ReplicaSet> v1beta1ReplicaSets = k8sWorker.getReplicaSetsV1beta1(cluster, namespace, null, null);

                    if (CollectionUtils.isNotEmpty(v1beta1Deployments)) {

                        // horizontalPodAutoscaler 조회
                        Map<String, K8sHorizontalPodAutoscalerVO> horizontalPodAutoscalerMap = this.convertHorizontalPodAutoscalerDataMap(cluster, namespace, field, label);

                        // ReplicaSet to Map by ownerReference
                        Map<String, List<V1beta1ReplicaSet>> replicaSetMap = new HashMap<>();
                        if (CollectionUtils.isNotEmpty(v1beta1ReplicaSets)) {
                            String ownerName = "";
                            for (V1beta1ReplicaSet replicaSetRow : v1beta1ReplicaSets) {
                                ownerName = "";
                                if (CollectionUtils.isNotEmpty(replicaSetRow.getMetadata().getOwnerReferences())) {
                                    for (V1OwnerReference ownerReferenceRow : replicaSetRow.getMetadata().getOwnerReferences()) {
                                        ownerName = ownerReferenceRow.getName();
                                        break;
                                    }
                                }
                                if (StringUtils.isBlank(ownerName)) {
                                    ownerName = replicaSetRow.getMetadata().getName();
                                }
                                if (StringUtils.isNotBlank(ownerName) && !replicaSetMap.containsKey(ownerName)) {
                                    replicaSetMap.put(ownerName, new ArrayList<>());
                                }

                                if (replicaSetMap.get(ownerName) != null) {
                                    replicaSetMap.get(ownerName).add(replicaSetRow);
                                }
                            }
                        }

                        for (AppsV1beta1Deployment deploymentRow : v1beta1Deployments) {
                            deployment = new K8sDeploymentVO();
                            statusMap = new HashMap<>();
                            readyReplicas = deploymentRow.getStatus().getReadyReplicas() != null ? deploymentRow.getStatus().getReadyReplicas() : 0;

                            // Deployment 목록
                            deployment.setLabel(label);
                            deployment.setNamespace(deploymentRow.getMetadata().getNamespace());
                            deployment.setName(deploymentRow.getMetadata().getName());
                            deployment.setLabels(deploymentRow.getMetadata().getLabels());
                            deployment.setReadyPodCnt(readyReplicas);
                            deployment.setDesiredPodCnt(deploymentRow.getSpec().getReplicas());
                            deployment.setCreationTimestamp(deploymentRow.getMetadata().getCreationTimestamp());
                            deployment.setImages(deploymentRow.getSpec().getTemplate().getSpec().getContainers().stream().map(c -> c.getImage()).collect(Collectors.toList()));
                            deployment.setDeployment(k8sJson.serialize(deploymentRow));
                            deployment.setDeploymentYaml(Yaml.getSnakeYaml().dumpAsMap(deploymentRow));
                            if (deploymentRow.getSpec() != null && deploymentRow.getSpec().getSelector() != null) {
                                deployment.setMatchLabels(deploymentRow.getSpec().getSelector().getMatchLabels());
                            }

                            // Deployment Detail
                            K8sDeploymentDetailVO deploymentDetail = new K8sDeploymentDetailVO();
                            deploymentDetail.setName(deploymentRow.getMetadata().getName());
                            deploymentDetail.setNamespace(deploymentRow.getMetadata().getNamespace());
                            deploymentDetail.setLabels(deploymentRow.getMetadata().getLabels());
                            deploymentDetail.setAnnotations(deploymentRow.getMetadata().getAnnotations());
                            deploymentDetail.setCreationTime(deploymentRow.getMetadata().getCreationTimestamp());
                            String selectorJson = k8sJson.serialize(deploymentRow.getSpec().getSelector());
                            deploymentDetail.setSelector(k8sJson.getGson().fromJson(selectorJson, K8sLabelSelectorVO.class));
                            deploymentDetail.setStrategy(deploymentRow.getSpec().getStrategy().getType());
                            deploymentDetail.setMinReadySeconds(deploymentRow.getSpec().getMinReadySeconds() != null ? deploymentRow.getSpec().getMinReadySeconds().intValue() : 0);
                            deploymentDetail.setRevisionHistoryLimit(deploymentRow.getSpec().getRevisionHistoryLimit() != null ? deploymentRow.getSpec().getRevisionHistoryLimit().toString() : "Not set");
                            deploymentDetail.setRollingUpdate(deploymentRow.getSpec().getStrategy().getRollingUpdate() != null ? mapper.readValue(k8sJson.serialize(deploymentRow.getSpec().getStrategy().getRollingUpdate()), new TypeReference<Map<String, Object>>() {}) : new HashMap<>());
                            statusMap.put("updated", deploymentRow.getStatus().getUpdatedReplicas() != null ? deploymentRow.getStatus().getUpdatedReplicas() : 0);
                            statusMap.put("total", deploymentRow.getStatus().getReplicas() != null ? deploymentRow.getStatus().getReplicas() : 0);
                            statusMap.put("available", deploymentRow.getStatus().getAvailableReplicas() != null ? deploymentRow.getStatus().getAvailableReplicas() : 0);
                            statusMap.put("unavailable", deploymentRow.getStatus().getUnavailableReplicas() != null ? deploymentRow.getStatus().getUnavailableReplicas() : 0);
                            deploymentDetail.setStatus(statusMap);
                            // podTemplate
                            deploymentDetail.setPodTemplate(this.setPodTemplateSpec(deploymentRow.getSpec().getTemplate(), k8sJson));
                            deploymentDetail.setReplicas(deploymentRow.getSpec().getReplicas());
                            deployment.setDetail(deploymentDetail);

                            // New Replica Set, Old Replica Sets
                            if (CollectionUtils.isNotEmpty(v1beta1ReplicaSets) && MapUtils.isNotEmpty(replicaSetMap)) {
                                if (MapUtils.getObject(replicaSetMap, deploymentRow.getMetadata().getName()) != null) {
                                    String currRevision = MapUtils.getString(deploymentRow.getMetadata().getAnnotations(), KubeConstants.META_ANNOTATIONS_DEPLOYMENT_REVISION, "");
                                    if (StringUtils.isNotBlank(currRevision)) {
                                        String prevRevision = StringUtils.isNotBlank(currRevision) ? String.valueOf(Integer.parseInt(currRevision) - 1) : "0";
                                        for (V1beta1ReplicaSet replicaSetRow : MapUtils.getObject(replicaSetMap, deploymentRow.getMetadata().getName())) {
                                            if (StringUtils.equals(currRevision, replicaSetRow.getMetadata().getAnnotations().get(KubeConstants.META_ANNOTATIONS_DEPLOYMENT_REVISION))) {
                                                replicaSets = new ArrayList<>();
                                                replicaSets.add(this.genReplicaSetV1beta1(replicaSetRow, cluster, label, k8sJson));
                                                deployment.setNewReplicaSets(replicaSets);
                                            } else if (StringUtils.equals(prevRevision, replicaSetRow.getMetadata().getAnnotations().get(KubeConstants.META_ANNOTATIONS_DEPLOYMENT_REVISION))) {
                                                if (replicaSetRow.getSpec().getReplicas().intValue() > 0) {
                                                    replicaSets = new ArrayList<>();
                                                    replicaSets.add(this.genReplicaSetV1beta1(replicaSetRow, cluster, label, k8sJson));
                                                    deployment.setOldReplicaSets(replicaSets);
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Horizontal Pod Autoscalers
                            if (MapUtils.isNotEmpty(horizontalPodAutoscalerMap)) {
                                K8sHorizontalPodAutoscalerVO horizontalPodAutoscaler = horizontalPodAutoscalerMap.get(deploymentRow.getMetadata().getName());
                                if (horizontalPodAutoscaler != null) {
                                    deployment.setHorizontalPodAutoscalers(Lists.newArrayList(horizontalPodAutoscaler));
                                }
                            }

                            deployments.add(deployment);
                        }
                    }
                } else if (apiVerKindType.getGroupType() == K8sApiGroupType.APPS && apiVerKindType.getApiType() == K8sApiType.V1BETA2) {
                    // Deployment 조회
                    List<V1beta2Deployment> v1beta2Deployments = k8sWorker.getDeploymentsV1beta2(cluster, namespace, field, label);
                    // ReplicaSet 조회
                    List<V1beta2ReplicaSet> v1beta2ReplicaSets = k8sWorker.getReplicaSetsV1beta2(cluster, namespace, null, null);

                    if (CollectionUtils.isNotEmpty(v1beta2Deployments)) {

                        // horizontalPodAutoscaler 조회
                        Map<String, K8sHorizontalPodAutoscalerVO> horizontalPodAutoscalerMap = this.convertHorizontalPodAutoscalerDataMap(cluster, namespace, field, label);

                        // ReplicaSet to Map by ownerReference
                        Map<String, List<V1beta2ReplicaSet>> replicaSetMap = new HashMap<>();
                        if (CollectionUtils.isNotEmpty(v1beta2ReplicaSets)) {
                            String ownerName = "";
                            for (V1beta2ReplicaSet replicaSetRow : v1beta2ReplicaSets) {
                                ownerName = "";
                                if (CollectionUtils.isNotEmpty(replicaSetRow.getMetadata().getOwnerReferences())) {
                                    for (V1OwnerReference ownerReferenceRow : replicaSetRow.getMetadata().getOwnerReferences()) {
                                        ownerName = ownerReferenceRow.getName();
                                        break;
                                    }
                                }
                                if (StringUtils.isBlank(ownerName)) {
                                    ownerName = replicaSetRow.getMetadata().getName();
                                }
                                if (StringUtils.isNotBlank(ownerName) && !replicaSetMap.containsKey(ownerName)) {
                                    replicaSetMap.put(ownerName, new ArrayList<>());
                                }

                                if (replicaSetMap.get(ownerName) != null) {
                                    replicaSetMap.get(ownerName).add(replicaSetRow);
                                }
                            }
                        }

                        for (V1beta2Deployment deploymentRow : v1beta2Deployments) {
                            deployment = new K8sDeploymentVO();
                            statusMap = new HashMap<>();
                            readyReplicas = deploymentRow.getStatus().getReadyReplicas() != null ? deploymentRow.getStatus().getReadyReplicas() : 0;

                            // Deployment 목록
                            deployment.setLabel(label);
                            deployment.setNamespace(deploymentRow.getMetadata().getNamespace());
                            deployment.setName(deploymentRow.getMetadata().getName());
                            deployment.setLabels(deploymentRow.getMetadata().getLabels());
                            deployment.setReadyPodCnt(readyReplicas);
                            deployment.setDesiredPodCnt(deploymentRow.getSpec().getReplicas());
                            deployment.setCreationTimestamp(deploymentRow.getMetadata().getCreationTimestamp());
                            deployment.setImages(deploymentRow.getSpec().getTemplate().getSpec().getContainers().stream().map(c -> c.getImage()).collect(Collectors.toList()));
                            deployment.setDeployment(k8sJson.serialize(deploymentRow));
                            deployment.setDeploymentYaml(Yaml.getSnakeYaml().dumpAsMap(deploymentRow));
                            if (deploymentRow.getSpec() != null && deploymentRow.getSpec().getSelector() != null) {
                                deployment.setMatchLabels(deploymentRow.getSpec().getSelector().getMatchLabels());
                            }

                            // Deployment Detail
                            K8sDeploymentDetailVO deploymentDetail = new K8sDeploymentDetailVO();
                            deploymentDetail.setName(deploymentRow.getMetadata().getName());
                            deploymentDetail.setNamespace(deploymentRow.getMetadata().getNamespace());
                            deploymentDetail.setLabels(deploymentRow.getMetadata().getLabels());
                            deploymentDetail.setAnnotations(deploymentRow.getMetadata().getAnnotations());
                            deploymentDetail.setCreationTime(deploymentRow.getMetadata().getCreationTimestamp());
                            String selectorJson = k8sJson.serialize(deploymentRow.getSpec().getSelector());
                            deploymentDetail.setSelector(k8sJson.getGson().fromJson(selectorJson, K8sLabelSelectorVO.class));
                            deploymentDetail.setStrategy(deploymentRow.getSpec().getStrategy().getType());
                            deploymentDetail.setMinReadySeconds(deploymentRow.getSpec().getMinReadySeconds() != null ? deploymentRow.getSpec().getMinReadySeconds().intValue() : 0);
                            deploymentDetail.setRevisionHistoryLimit(deploymentRow.getSpec().getRevisionHistoryLimit() != null ? deploymentRow.getSpec().getRevisionHistoryLimit().toString() : "Not set");
                            deploymentDetail.setRollingUpdate(deploymentRow.getSpec().getStrategy().getRollingUpdate() != null ? mapper.readValue(k8sJson.serialize(deploymentRow.getSpec().getStrategy().getRollingUpdate()), new TypeReference<Map<String, Object>>() {}) : new HashMap<>());
                            statusMap.put("updated", deploymentRow.getStatus().getUpdatedReplicas() != null ? deploymentRow.getStatus().getUpdatedReplicas() : 0);
                            statusMap.put("total", deploymentRow.getStatus().getReplicas() != null ? deploymentRow.getStatus().getReplicas() : 0);
                            statusMap.put("available", deploymentRow.getStatus().getAvailableReplicas() != null ? deploymentRow.getStatus().getAvailableReplicas() : 0);
                            statusMap.put("unavailable", deploymentRow.getStatus().getUnavailableReplicas() != null ? deploymentRow.getStatus().getUnavailableReplicas() : 0);
                            deploymentDetail.setStatus(statusMap);
                            // podTemplate
                            deploymentDetail.setPodTemplate(this.setPodTemplateSpec(deploymentRow.getSpec().getTemplate(), k8sJson));
                            deploymentDetail.setReplicas(deploymentRow.getSpec().getReplicas());
                            deployment.setDetail(deploymentDetail);

                            // New Replica Set, Old Replica Sets
                            if (CollectionUtils.isNotEmpty(v1beta2ReplicaSets) && MapUtils.isNotEmpty(replicaSetMap)) {
                                if (MapUtils.getObject(replicaSetMap, deploymentRow.getMetadata().getName()) != null) {
                                    String currRevision = MapUtils.getString(deploymentRow.getMetadata().getAnnotations(), KubeConstants.META_ANNOTATIONS_DEPLOYMENT_REVISION, "");
                                    if (StringUtils.isNotBlank(currRevision)) {
                                        String prevRevision = StringUtils.isNotBlank(currRevision) ? String.valueOf(Integer.parseInt(currRevision) - 1) : "0";
                                        for (V1beta2ReplicaSet replicaSetRow : MapUtils.getObject(replicaSetMap, deploymentRow.getMetadata().getName())) {
                                            if (StringUtils.equals(currRevision, replicaSetRow.getMetadata().getAnnotations().get(KubeConstants.META_ANNOTATIONS_DEPLOYMENT_REVISION))) {
                                                replicaSets = new ArrayList<>();
                                                replicaSets.add(this.genReplicaSetV1beta2(replicaSetRow, cluster, label, k8sJson));
                                                deployment.setNewReplicaSets(replicaSets);
                                            } else if (StringUtils.equals(prevRevision, replicaSetRow.getMetadata().getAnnotations().get(KubeConstants.META_ANNOTATIONS_DEPLOYMENT_REVISION))) {
                                                if (replicaSetRow.getSpec().getReplicas().intValue() > 0) {
                                                    replicaSets = new ArrayList<>();
                                                    replicaSets.add(this.genReplicaSetV1beta2(replicaSetRow, cluster, label, k8sJson));
                                                    deployment.setOldReplicaSets(replicaSets);
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Horizontal Pod Autoscalers
                            if (MapUtils.isNotEmpty(horizontalPodAutoscalerMap)) {
                                K8sHorizontalPodAutoscalerVO horizontalPodAutoscaler = horizontalPodAutoscalerMap.get(deploymentRow.getMetadata().getName());
                                if (horizontalPodAutoscaler != null) {
                                    deployment.setHorizontalPodAutoscalers(Lists.newArrayList(horizontalPodAutoscaler));
                                }
                            }

                            deployments.add(deployment);
                        }
                    }
                }
                // K8sApiType.V1 == apiType
                else {
                    // Deployment 조회
                    List<V1Deployment> v1Deployments = k8sWorker.getDeploymentsV1(cluster, namespace, field, label);
                    // ReplicaSet 조회
                    List<V1ReplicaSet> v1ReplicaSets = k8sWorker.getReplicaSetsV1(cluster, namespace, null, null);

                    if (CollectionUtils.isNotEmpty(v1Deployments)) {
                        // horizontalPodAutoscaler 조회 (2020.01.06 : Namespace 전체 조회하여 Mapping 하도록 변경)
                        Map<String, K8sHorizontalPodAutoscalerVO> horizontalPodAutoscalerMap = this.convertHorizontalPodAutoscalerDataMap(cluster, namespace, null, null);
                        //                Map<String, K8sHorizontalPodAutoscalerVO> horizontalPodAutoscalerMap = this.convertHorizontalPodAutoscalerDataMap(cluster, namespace, field, label);

                        // ReplicaSet to Map by ownerReference
                        Map<String, List<V1ReplicaSet>> replicaSetMap = new HashMap<>();
                        if (CollectionUtils.isNotEmpty(v1ReplicaSets)) {
                            String ownerName = "";
                            for (V1ReplicaSet replicaSetRow : v1ReplicaSets) {
                                ownerName = "";
                                if (CollectionUtils.isNotEmpty(replicaSetRow.getMetadata().getOwnerReferences())) {
                                    for (V1OwnerReference ownerReferenceRow : replicaSetRow.getMetadata().getOwnerReferences()) {
                                        ownerName = ownerReferenceRow.getName();
                                        break;
                                    }
                                }
                                if (StringUtils.isBlank(ownerName)) {
                                    ownerName = replicaSetRow.getMetadata().getName();
                                }
                                if (StringUtils.isNotBlank(ownerName) && !replicaSetMap.containsKey(ownerName)) {
                                    replicaSetMap.put(ownerName, new ArrayList<>());
                                }

                                if (replicaSetMap.get(ownerName) != null) {
                                    replicaSetMap.get(ownerName).add(replicaSetRow);
                                }
                            }
                        }

                        for (V1Deployment deploymentRow : v1Deployments) {
                            deployment = new K8sDeploymentVO();
                            statusMap = new HashMap<>();
                            readyReplicas = deploymentRow.getStatus().getReadyReplicas() != null ? deploymentRow.getStatus().getReadyReplicas() : 0;

                            // Deployment 목록
                            deployment.setLabel(label);
                            deployment.setNamespace(deploymentRow.getMetadata().getNamespace());
                            deployment.setName(deploymentRow.getMetadata().getName());
                            deployment.setLabels(deploymentRow.getMetadata().getLabels());
                            deployment.setReadyPodCnt(readyReplicas);
                            deployment.setDesiredPodCnt(deploymentRow.getSpec().getReplicas());
                            deployment.setCreationTimestamp(deploymentRow.getMetadata().getCreationTimestamp());
                            deployment.setImages(deploymentRow.getSpec().getTemplate().getSpec().getContainers().stream().map(c -> c.getImage()).collect(Collectors.toList()));
                            deployment.setDeployment(k8sJson.serialize(deploymentRow));
                            deployment.setDeploymentYaml(Yaml.getSnakeYaml().dumpAsMap(deploymentRow));
                            if (deploymentRow.getSpec() != null && deploymentRow.getSpec().getSelector() != null) {
                                deployment.setMatchLabels(deploymentRow.getSpec().getSelector().getMatchLabels());
                            }

                            // Deployment Detail
                            K8sDeploymentDetailVO deploymentDetail = new K8sDeploymentDetailVO();
                            deploymentDetail.setName(deploymentRow.getMetadata().getName());
                            deploymentDetail.setNamespace(deploymentRow.getMetadata().getNamespace());
                            deploymentDetail.setLabels(deploymentRow.getMetadata().getLabels());
                            deploymentDetail.setAnnotations(deploymentRow.getMetadata().getAnnotations());
                            deploymentDetail.setCreationTime(deploymentRow.getMetadata().getCreationTimestamp());
                            String selectorJson = k8sJson.serialize(deploymentRow.getSpec().getSelector());
                            deploymentDetail.setSelector(k8sJson.getGson().fromJson(selectorJson, K8sLabelSelectorVO.class));
                            deploymentDetail.setStrategy(deploymentRow.getSpec().getStrategy().getType());
                            deploymentDetail.setMinReadySeconds(deploymentRow.getSpec().getMinReadySeconds() != null ? deploymentRow.getSpec().getMinReadySeconds().intValue() : 0);
                            deploymentDetail.setRevisionHistoryLimit(deploymentRow.getSpec().getRevisionHistoryLimit() != null ? deploymentRow.getSpec().getRevisionHistoryLimit().toString() : "Not set");
                            deploymentDetail.setRollingUpdate(deploymentRow.getSpec().getStrategy().getRollingUpdate() != null ? mapper.readValue(k8sJson.serialize(deploymentRow.getSpec().getStrategy().getRollingUpdate()), new TypeReference<Map<String, Object>>() {}) : new HashMap<>());
                            statusMap.put("updated", deploymentRow.getStatus().getUpdatedReplicas() != null ? deploymentRow.getStatus().getUpdatedReplicas() : 0);
                            statusMap.put("total", deploymentRow.getStatus().getReplicas() != null ? deploymentRow.getStatus().getReplicas() : 0);
                            statusMap.put("available", deploymentRow.getStatus().getAvailableReplicas() != null ? deploymentRow.getStatus().getAvailableReplicas() : 0);
                            statusMap.put("unavailable", deploymentRow.getStatus().getUnavailableReplicas() != null ? deploymentRow.getStatus().getUnavailableReplicas() : 0);
                            deploymentDetail.setStatus(statusMap);
                            // podTemplate
                            deploymentDetail.setPodTemplate(this.setPodTemplateSpec(deploymentRow.getSpec().getTemplate(), k8sJson));
                            deploymentDetail.setReplicas(deploymentRow.getSpec().getReplicas());
                            deployment.setDetail(deploymentDetail);

                            // New Replica Set, Old Replica Sets
                            if (CollectionUtils.isNotEmpty(v1ReplicaSets) && MapUtils.isNotEmpty(replicaSetMap)) {
                                if (MapUtils.getObject(replicaSetMap, deploymentRow.getMetadata().getName()) != null) {
                                    String currRevision = MapUtils.getString(deploymentRow.getMetadata().getAnnotations(), KubeConstants.META_ANNOTATIONS_DEPLOYMENT_REVISION, "");
                                    if (StringUtils.isNotBlank(currRevision)) {
                                        String prevRevision = StringUtils.isNotBlank(currRevision) ? String.valueOf(Integer.parseInt(currRevision) - 1) : "0";
                                        for (V1ReplicaSet replicaSetRow : MapUtils.getObject(replicaSetMap, deploymentRow.getMetadata().getName())) {
                                            if (StringUtils.equals(currRevision, replicaSetRow.getMetadata().getAnnotations().get(KubeConstants.META_ANNOTATIONS_DEPLOYMENT_REVISION))) {
                                                replicaSets = new ArrayList<>();
                                                replicaSets.add(this.genReplicaSetV1(replicaSetRow, cluster, label, k8sJson));
                                                deployment.setNewReplicaSets(replicaSets);
                                            } else if (StringUtils.equals(prevRevision, replicaSetRow.getMetadata().getAnnotations().get(KubeConstants.META_ANNOTATIONS_DEPLOYMENT_REVISION))) {
                                                if (replicaSetRow.getSpec().getReplicas().intValue() > 0) {
                                                    replicaSets = new ArrayList<>();
                                                    replicaSets.add(this.genReplicaSetV1(replicaSetRow, cluster, label, k8sJson));
                                                    deployment.setOldReplicaSets(replicaSets);
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Deployment Condition
                            List<K8sConditionVO> conditions = new ArrayList<>();
                            if (CollectionUtils.isNotEmpty(deploymentRow.getStatus().getConditions())) {
                                for (V1DeploymentCondition conditionRow : deploymentRow.getStatus().getConditions()) {
                                    K8sConditionVO conditionTemp = new K8sConditionVO();
                                    BeanUtils.copyProperties(conditionTemp, conditionRow);
                                    if (conditionRow.getLastUpdateTime() != null) {
                                        conditionTemp.setLastUpdateTime(conditionRow.getLastUpdateTime());
                                    }
                                    if (conditionRow.getLastTransitionTime() != null) {
                                        conditionTemp.setLastTransitionTime(conditionRow.getLastTransitionTime());
                                    }

                                    conditions.add(conditionTemp);
                                }
                            }
                            deployment.setConditions(conditions);

                            // Horizontal Pod Autoscalers
                            if (MapUtils.isNotEmpty(horizontalPodAutoscalerMap)) {
                                //                        // 2020.01.06 : HPA의 TargetRef 정보로 매칭하도록 변경..
                                //                        K8sHorizontalPodAutoscalerVO horizontalPodAutoscaler = horizontalPodAutoscalerMap.get(deploymentRow.getMetadata().getName());
                                //                        if(horizontalPodAutoscaler != null){
                                //                            deployment.setHorizontalPodAutoscalers(Lists.newArrayList(horizontalPodAutoscaler));
                                //                        }
                                List<K8sHorizontalPodAutoscalerVO> hpaList = new ArrayList<>();
                                for (String key : horizontalPodAutoscalerMap.keySet()) {
                                    K8sHorizontalPodAutoscalerVO hpa = horizontalPodAutoscalerMap.get(key);
                                    if (hpa != null && hpa.getScaleTargetRef() != null && StringUtils.isNotBlank(hpa.getScaleTargetRef().getKind()) && StringUtils.isNotBlank(hpa.getScaleTargetRef().getName())) {
                                        // 매칭되는 Autoscaler를 찾으면 break; => 여러개일 수 있으므로 List에 추가..
                                        if (K8sApiKindType.DEPLOYMENT.getValue().equals(hpa.getScaleTargetRef().getKind()) && deployment.getName().equals(hpa.getScaleTargetRef().getName())) {
                                            hpaList.add(hpa);
                                        }
                                    }
                                }
                                deployment.setHorizontalPodAutoscalers(hpaList);
                            }

                            deployments.add(deployment);
                        }
                    }
                }
            } else {
                log.warn("convertDeploymentDataList cluster {} version {} not supported.", cluster.getClusterName(), cluster.getK8sVersion());
            }
        } else {
            log.warn("convertDeploymentDataList - cluster is null.");
        }


        return deployments;
    }

    /**
     * StatefulSet 생성
     *
     * @param cluster
     * @param server
     * @param context
     * @return
     * @throws Exception
     */
    public K8sStatefulSetVO createStatefulSet(ClusterVO cluster, ServerDetailVO server, ExecutingContextVO context) throws Exception {

        // Convert to JsonObject
        log.debug("Converted Server creation request: {}", JsonUtils.toGson(server));

        K8sStatefulSetVO k8sStatefulSet = null;
        K8sApiVerKindType apiVerKindType = ResourceUtil.getApiVerKindType(cluster, K8sApiKindType.STATEFUL_SET);

        if (apiVerKindType != null) {
            if (apiVerKindType.getGroupType() == K8sApiGroupType.APPS && apiVerKindType.getApiType() == K8sApiType.V1) {
                // Create StatefulSet
                V1StatefulSet v1StatefulSet = K8sSpecFactory.buildStatefulSetV1(server, cluster.getNamespaceName());
                v1StatefulSet.getMetadata().putAnnotationsItem(KubeConstants.ANNOTATION_COCKTAIL_DEPLOY_DATETIME, Utils.getNowDateTime());

                final V1Namespace v1Namespace = k8sWorker.getNamespaceV1(cluster, cluster.getNamespaceName());
                if (v1Namespace != null && this.isServiceMeshMemberRollCustomResourceDefinitions(cluster)) {
                    this.addPodTemplateAnnotations(v1Namespace , this.getV1ObjectMetaOfWorkload().apply(v1StatefulSet));
                }

                log.debug("########## V1StatefulSet JSON request: {}", JsonUtils.toGson(v1StatefulSet));
                v1StatefulSet = k8sWorker.createStatefulSetV1(cluster, cluster.getNamespaceName(), v1StatefulSet, false);
                log.debug("########## V1StatefulSet JSON response: {}", JsonUtils.toGson(v1StatefulSet));
                Thread.sleep(200);

                k8sStatefulSet = this.getStatefulSet(cluster, cluster.getNamespaceName(), server.getComponent().getComponentName(), context);
            }
        }

        return k8sStatefulSet;
    }

    /**
     * StatefulSet 수정
     *
     * @param cluster
     * @param server
     * @param context
     * @return
     * @throws Exception
     */
    public K8sStatefulSetVO updateStatefulSet(ClusterVO cluster, ServerDetailVO server, ExecutingContextVO context) throws Exception {

        // Convert to JsonObject
        log.debug("Converted Server creation request: {}", JsonUtils.toGson(server));

        K8sStatefulSetVO k8sStatefulSet = null;
        K8sApiVerKindType apiVerKindType = ResourceUtil.getApiVerKindType(cluster, K8sApiKindType.STATEFUL_SET);

        if (apiVerKindType != null) {
            if (apiVerKindType.getGroupType() == K8sApiGroupType.APPS && apiVerKindType.getApiType() == K8sApiType.V1) {

                V1StatefulSet updatedStatefulSet = K8sSpecFactory.buildStatefulSetV1(server, cluster.getNamespaceName());

                // 현재 StatefulSet 조회
                V1StatefulSet currentStatefulSet = k8sWorker.getStatefulSetV1(cluster, cluster.getNamespaceName(), server.getComponent().getComponentName());

                // patchJson 으로 변경
                List<JsonObject> patchBody = k8sPatchSpecFactory.buildPatch(currentStatefulSet, updatedStatefulSet);
                log.debug("########## StatefulSet patchBody JSON: {}", JsonUtils.toGson(patchBody));
                // patch
                k8sWorker.patchStatefulSetV1(cluster, cluster.getNamespaceName(), server.getComponent().getComponentName(), patchBody, false);

                Thread.sleep(100);

                k8sStatefulSet = this.getStatefulSet(cluster, cluster.getNamespaceName(), server.getComponent().getComponentName(), context);
            }
        }

        return k8sStatefulSet;
    }

    /**
     * StatefulSet 수정
     *
     * @param cluster
     * @param namespace
     * @param updatedStatefulSet
     * @param hasHPA
     * @param context
     * @throws Exception
     */
    public void patchStatefulSetV1(ClusterVO cluster, String namespace, V1StatefulSet updatedStatefulSet, boolean hasHPA, ExecutingContextVO context) throws Exception {

        // 현재 StatefulSet 조회
        V1StatefulSet currentStatefulSet = k8sWorker.getStatefulSetV1(cluster, namespace, updatedStatefulSet.getMetadata().getName());
        if (hasHPA && currentStatefulSet != null && currentStatefulSet.getSpec() != null) {
            currentStatefulSet.getSpec().setReplicas(null);
        }
        if (hasHPA && updatedStatefulSet != null && updatedStatefulSet.getSpec() != null) {
            updatedStatefulSet.getSpec().setReplicas(null);
        }
        currentStatefulSet.setStatus(null);
        if (CollectionUtils.isNotEmpty(currentStatefulSet.getSpec().getVolumeClaimTemplates())) {
            for (V1PersistentVolumeClaim pvcTemp : currentStatefulSet.getSpec().getVolumeClaimTemplates()) {
                pvcTemp.setStatus(null);
                pvcTemp.getSpec().setVolumeMode(null);
            }
        }

        // patchJson 으로 변경
        List<JsonObject> patchBody = k8sPatchSpecFactory.buildPatch(currentStatefulSet, updatedStatefulSet);
        log.debug("########## StatefulSet patchBody JSON: {}", JsonUtils.toGson(patchBody));
        // patch
        k8sWorker.patchStatefulSetV1(cluster, namespace, updatedStatefulSet.getMetadata().getName(), patchBody, false);
    }

    /**
     * StatefulSet 수정
     *
     * @param cluster
     * @param updatedStatefulSet
     * @param context
     * @throws Exception
     */
    public void patchStatefulSetV1(ClusterVO cluster, String namespace, V1StatefulSet currentStatefulSet, V1StatefulSet updatedStatefulSet, boolean dryRun, boolean hasHPA, ExecutingContextVO context) throws Exception {

        // 현재 StatefulSet 조회
        if (hasHPA && currentStatefulSet != null && currentStatefulSet.getSpec() != null) {
            currentStatefulSet.getSpec().setReplicas(null);
        }
        currentStatefulSet.setStatus(null);
        if (CollectionUtils.isNotEmpty(currentStatefulSet.getSpec().getVolumeClaimTemplates())) {
            for (V1PersistentVolumeClaim pvcTemp : currentStatefulSet.getSpec().getVolumeClaimTemplates()) {
                pvcTemp.setStatus(null);
                pvcTemp.getSpec().setVolumeMode(null);
            }
        }
        updatedStatefulSet.setStatus(null);
        if (hasHPA && updatedStatefulSet != null && updatedStatefulSet.getSpec() != null) {
            updatedStatefulSet.getSpec().setReplicas(null);
        }
        if (CollectionUtils.isNotEmpty(updatedStatefulSet.getSpec().getVolumeClaimTemplates())) {
            for (V1PersistentVolumeClaim pvcTemp : updatedStatefulSet.getSpec().getVolumeClaimTemplates()) {
                pvcTemp.setStatus(null);
                pvcTemp.getSpec().setVolumeMode(null);
            }
        }

        // patchJson 으로 변경
        List<JsonObject> patchBody = k8sPatchSpecFactory.buildPatch(currentStatefulSet, updatedStatefulSet);
        log.debug("########## StatefulSet patchBody JSON: {}", JsonUtils.toGson(patchBody));
        // patch
        k8sWorker.patchStatefulSetV1(cluster, namespace, updatedStatefulSet.getMetadata().getName(), patchBody, dryRun);
    }

    public void replaceStatefulSetV1(ClusterVO cluster, String namespace, V1StatefulSet updatedStatefulSet, ExecutingContextVO context) throws Exception {
        k8sWorker.replaceStatefulSetV1(cluster, namespace, updatedStatefulSet.getMetadata().getName(), updatedStatefulSet);
    }

    /**
     * StatefulSet 삭제
     *
     * @param cluster
     * @param serverParam
     * @param context
     * @return
     * @throws Exception
     */
    public boolean deleteStatefulSet(ClusterVO cluster, ServerDetailVO serverParam, ExecutingContextVO context) throws Exception {

        String name = ResourceUtil.getUniqueName(serverParam.getComponent().getComponentName());
        log.info("Converted ServerDetailVO INFO: {}", JsonUtils.toGson(serverParam));

        return this.deleteStatefulSet(cluster, name, context);
    }

    public boolean deleteStatefulSet(ClusterVO cluster, String name, ExecutingContextVO context) throws Exception {

        K8sApiVerKindType apiVerKindType = ResourceUtil.getApiVerKindType(cluster, K8sApiKindType.STATEFUL_SET);

        if (apiVerKindType != null) {
            if (apiVerKindType.getGroupType() == K8sApiGroupType.APPS && apiVerKindType.getApiType() == K8sApiType.V1) {

                /**
                 * StatefulSet Delete
                 */
                log.debug("StatefulSet to delete: {}", name);
                // Get StatefulSet
                V1StatefulSet v1StatefulSet = k8sWorker.getStatefulSetV1(cluster, cluster.getNamespaceName(), name);

                // StatefulSet가 존재시 scale을 0으로 변경 후, Delete StatefulSet
                if (v1StatefulSet != null) {
                    // scale = 0 로 변경
                    List<JsonObject> patchBody = k8sPatchSpecFactory.buildPatchStatefulSetScaleV1(0);
                    V1Scale v1Scale = k8sWorker.patchStatefulSetScaleV1(cluster, cluster.getNamespaceName(), ResourceUtil.getUniqueName(name), patchBody, false);
                    Thread.sleep(500);

                    log.debug("### StatefulSet to delete: {}", v1StatefulSet.getMetadata().getName());
                    V1Status resultObj = k8sWorker.deleteStatefulSetV1(cluster, cluster.getNamespaceName(), name);
                    log.debug("### StatefulSet to delete status: {}", resultObj);
                }
            } else {
                log.error("fail deleteServer!! ( invalid K8sAPIType : k8sApiType[{}] ) cluster:[{}], name: [{}]", K8sApiType.values(), JsonUtils.toGson(cluster), name);

                /**
                 * StatefulSet Delete
                 */
                log.debug("StatefulSet to delete: {}", name);
                // Get StatefulSet
                V1StatefulSet v1StatefulSet = k8sWorker.getStatefulSetV1(cluster, cluster.getNamespaceName(), name);

                // StatefulSet가 존재시 scale을 0으로 변경 후, Delete StatefulSet
                if (v1StatefulSet != null) {
                    // scale = 0 로 변경
                    List<JsonObject> patchBody = k8sPatchSpecFactory.buildPatchStatefulSetScaleV1(0);
                    V1Scale v1Scale = k8sWorker.patchStatefulSetScaleV1(cluster, cluster.getNamespaceName(), ResourceUtil.getUniqueName(name), patchBody, false);
                    Thread.sleep(500);

                    log.debug("### StatefulSet to delete: {}", v1StatefulSet.getMetadata().getName());
                    V1Status resultObj = k8sWorker.deleteStatefulSetV1(cluster, cluster.getNamespaceName(), name);
                    log.debug("### StatefulSet to delete status: {}", resultObj);
                }
            }
        }

        return true;
    }

    /**
     *
     * Patch StatefulSet Scale
     * @param cluster
     * @param namespace
     * @param name
     * @param scale
     * @param context
     * @throws Exception
     */
    public void patchStatefulSetScale(ClusterVO cluster, String namespace, String name, int scale, boolean dryRun, ExecutingContextVO context) throws Exception{

        K8sApiVerKindType apiVerKindType = ResourceUtil.getApiVerKindType(cluster, K8sApiKindType.STATEFUL_SET);

        if (apiVerKindType != null) {
            if (apiVerKindType.getGroupType() == K8sApiGroupType.APPS && apiVerKindType.getApiType() == K8sApiType.V1) {
                List<JsonObject> patchBody = k8sPatchSpecFactory.buildPatchStatefulSetScaleV1(scale);
                V1Scale v1Scale = k8sWorker.patchStatefulSetScaleV1(cluster, namespace, ResourceUtil.getUniqueName(name), patchBody, dryRun);
                Thread.sleep(100);

            }
        }
    }

    /**
     * K8S StatefulSet 정보 조회
     *
     * @param cluster
     * @param namespace
     * @param fieldSelector
     * @param labelSelector
     * @param context
     * @return
     * @throws Exception
     */
    public List<K8sStatefulSetVO> getStatefulSets(ClusterVO cluster, String namespace, String fieldSelector, String labelSelector, ExecutingContextVO context) throws Exception{

        try {
            if(cluster != null && StringUtils.isNotBlank(namespace)){

                List<K8sStatefulSetVO> k8sStatefulSets = this.convertStatefulSetDataList(cluster, namespace, fieldSelector, labelSelector, context);

                if(CollectionUtils.isNotEmpty(k8sStatefulSets)){
                    return k8sStatefulSets;
                }else{
                    return new ArrayList<>();
                }
            }else{
                throw new CocktailException("cluster/namespace/name is null.", ExceptionType.InvalidParameter);
            }
        } catch (Exception e) {
            throw new CocktailException("getStatefulSets fail!!", e, ExceptionType.K8sCocktailCloudInquireFail);
        }
    }

    /**
     * K8S StatefulSet 정보 조회
     *
     * @param clusterSeq
     * @param namespace
     * @param name
     * @param context
     * @return
     * @throws Exception
     */
    public K8sStatefulSetVO getStatefulSet(Integer clusterSeq, String namespace, String name, ExecutingContextVO context) throws Exception {
        try {
            if (clusterSeq != null) {
                IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);

                ClusterVO cluster = clusterDao.getCluster(clusterSeq);

                if (cluster != null) {
                    return this.getStatefulSet(cluster, namespace, name, context);
                }
                else {
                    throw new CocktailException("cluster not found.", ExceptionType.InvalidParameter);
                }
            }
            else {
                throw new CocktailException("cluster not found.", ExceptionType.InvalidParameter);
            }
        } catch (CocktailException e) {
            throw new CocktailException("getStatefulSet fail!!", e, ExceptionType.K8sCocktailCloudInquireFail);
        }
    }

    /**
     * K8S StatefulSet 정보 조회
     *
     * @param cluster
     * @param namespace
     * @param name
     * @param context
     * @return
     * @throws Exception
     */
    public K8sStatefulSetVO getStatefulSet(ClusterVO cluster, String namespace, String name, ExecutingContextVO context) throws Exception{

        try {
            if(cluster != null && StringUtils.isNotBlank(namespace) && StringUtils.isNotBlank(name)){
                String field = String.format("%s.%s=%s", KubeConstants.META, KubeConstants.NAME, name);

                List<K8sStatefulSetVO> k8sStatefulSets = this.getStatefulSets(cluster, namespace, field, null, context);

                if(CollectionUtils.isNotEmpty(k8sStatefulSets)){
                    return k8sStatefulSets.get(0);
                }else{
                    return null;
                }
            }else{
                throw new CocktailException("cluster/namespace/name is null.", ExceptionType.InvalidParameter);
            }
        } catch (CocktailException e) {
            throw new CocktailException("getStatefulSet fail!!", e, ExceptionType.K8sCocktailCloudInquireFail);
        }
    }

    /**
     * K8S StatefulSet 정보 조회
     *
     * @param clusterSeq
     * @param namespaceName
     * @return
     * @throws Exception
     */
    public List<K8sStatefulSetVO> getStatefulSetsByCluster(Integer clusterSeq, String namespaceName, ExecutingContextVO context) throws Exception{

        try {
            if(clusterSeq != null){
                IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);

                ClusterVO cluster = clusterDao.getCluster(clusterSeq);

                if(cluster != null){
                    return this.getStatefulSets(cluster, namespaceName, null, null, context);
                }else{
                    throw new CocktailException("cluster is null.", ExceptionType.InvalidParameter);
                }
            }else{
                throw new CocktailException("componentSeq is null.", ExceptionType.InvalidParameter);
            }
        } catch (Exception e) {
            throw new CocktailException("getStatefulSetsByCluster fail!!", e, ExceptionType.K8sCocktailCloudInquireFail);
        }
    }

    /**
     * K8S StatefulSet 정보 조회 후,
     *
     * - V1BETA1
     * V1beta1StatefulSet -> K8sStatefulSetVO 변환
     *
     * - V1BETA2
     * V1beta2StatefulSet -> K8sStatefulSetVO 변환
     *
     * - V1
     * V1StatefulSet -> K8sStatefulSetVO 변환
     *
     * @param cluster
     * @param label
     * @return
     * @throws Exception
     */
    public List<K8sStatefulSetVO> convertStatefulSetDataList(ClusterVO cluster, String namespace, String field, String label, ExecutingContextVO context) throws Exception {

        List<K8sStatefulSetVO> statefulSets = new ArrayList<>();

        if (cluster != null) {
            K8sApiVerKindType apiVerKindType = ResourceUtil.getApiVerKindType(cluster, K8sApiKindType.STATEFUL_SET);

            if (apiVerKindType != null) {
                K8sStatefulSetVO statefulSet = null;
                Map<String, Integer> statusMap = null;
                List<K8sReplicaSetVO> replicaSets = null;
                K8sReplicaSetVO replicaSet = null;
                Integer readyReplicas = 0;

                ObjectMapper mapper = K8sMapperUtils.getMapper();

                // joda.datetime Serialization
                JSON k8sJson = new JSON();

                if(apiVerKindType.getGroupType() == K8sApiGroupType.APPS && apiVerKindType.getApiType() == K8sApiType.V1BETA1){
                    // StatefulSet 조회
                    List<V1beta1StatefulSet> v1beta1StatefulSets = k8sWorker.getStatefulSetsV1beta1(cluster, namespace, field, label);

                    if(CollectionUtils.isNotEmpty(v1beta1StatefulSets)){
                        // horizontalPodAutoscaler 조회
                        Map<String, K8sHorizontalPodAutoscalerVO> horizontalPodAutoscalerMap = this.convertHorizontalPodAutoscalerDataMap(cluster, namespace, field, label);

                        for(V1beta1StatefulSet statefulSetRow : v1beta1StatefulSets){
                            statefulSet = new K8sStatefulSetVO();
                            statusMap = new HashMap<>();
                            readyReplicas = statefulSetRow.getStatus().getReadyReplicas() != null ? statefulSetRow.getStatus().getReadyReplicas() : 0;

                            // StatefulSet 목록
                            statefulSet.setLabel(label);
                            statefulSet.setNamespace(statefulSetRow.getMetadata().getNamespace());
                            statefulSet.setName(statefulSetRow.getMetadata().getName());
                            statefulSet.setLabels(statefulSetRow.getMetadata().getLabels());
                            statefulSet.setReadyPodCnt(readyReplicas);
                            statefulSet.setDesiredPodCnt(statefulSetRow.getSpec().getReplicas());
                            statefulSet.setCreationTimestamp(statefulSetRow.getMetadata().getCreationTimestamp());
                            statefulSet.setImages(statefulSetRow.getSpec().getTemplate().getSpec().getContainers().stream().map(c -> c.getImage()).collect(Collectors.toList()));
                            statefulSet.setDeployment(k8sJson.serialize(statefulSetRow));
                            statefulSet.setDeploymentYaml(Yaml.getSnakeYaml().dumpAsMap(statefulSetRow));

                            // StatefulSet Detail
                            K8sStatefulSetDetailVO statefulSetDetail = new K8sStatefulSetDetailVO();
                            statefulSetDetail.setName(statefulSetRow.getMetadata().getName());
                            statefulSetDetail.setNamespace(statefulSetRow.getMetadata().getNamespace());
                            statefulSetDetail.setLabels(statefulSetRow.getMetadata().getLabels());
                            statefulSetDetail.setAnnotations(statefulSetRow.getMetadata().getAnnotations());
                            statefulSetDetail.setCreationTime(statefulSetRow.getMetadata().getCreationTimestamp());
                            String selectorJson = k8sJson.serialize(statefulSetRow.getSpec().getSelector());
                            statefulSetDetail.setSelector(k8sJson.getGson().fromJson(selectorJson, K8sLabelSelectorVO.class));
                            statefulSetDetail.setStrategy(statefulSetRow.getSpec().getUpdateStrategy().getType());
                            statefulSetDetail.setPodManagementPolicy(statefulSetRow.getSpec().getPodManagementPolicy());
                            statefulSetDetail.setRevisionHistoryLimit(statefulSetRow.getSpec().getRevisionHistoryLimit() != null ? statefulSetRow.getSpec().getRevisionHistoryLimit().toString() : "Not set");
                            statefulSetDetail.setRollingUpdate(statefulSetRow.getSpec().getUpdateStrategy().getRollingUpdate() != null ? mapper.readValue(k8sJson.serialize(statefulSetRow.getSpec().getUpdateStrategy().getRollingUpdate()), new TypeReference<Map<String, Object>>(){}) : new HashMap<>());
                            statusMap.put("collisionCount", statefulSetRow.getStatus().getCollisionCount() != null ? statefulSetRow.getStatus().getCollisionCount() : 0);
                            statusMap.put("current", statefulSetRow.getStatus().getCurrentReplicas() != null ? statefulSetRow.getStatus().getCurrentReplicas() : 0);
                            statusMap.put("ready", statefulSetRow.getStatus().getReadyReplicas() != null ? statefulSetRow.getStatus().getReadyReplicas() : 0);
                            statusMap.put("replicas", statefulSetRow.getStatus().getReplicas() != null ? statefulSetRow.getStatus().getReplicas() : 0);
                            statusMap.put("updated", statefulSetRow.getStatus().getUpdatedReplicas() != null ? statefulSetRow.getStatus().getUpdatedReplicas() : 0);
                            statefulSetDetail.setStatus(statusMap);
                            List<K8sPersistentVolumeClaimVO> volumeClaimTemplates = new ArrayList<>();
                            if (CollectionUtils.isNotEmpty(statefulSetRow.getSpec().getVolumeClaimTemplates())) {
                                for (V1PersistentVolumeClaim volumeClaimRow : statefulSetRow.getSpec().getVolumeClaimTemplates()) {
                                    K8sPersistentVolumeClaimVO persistentVolumeClaim = new K8sPersistentVolumeClaimVO();
                                    persistentVolumeClaim.setName(volumeClaimRow.getMetadata().getName());
                                    persistentVolumeClaim.setStorageClassName(volumeClaimRow.getSpec().getStorageClassName());
                                    persistentVolumeClaim.setCapacity(k8sWorker.convertReasourceMap(volumeClaimRow.getSpec().getResources().getRequests()));
                                    persistentVolumeClaim.setAccessModes(volumeClaimRow.getSpec().getAccessModes());
                                    volumeClaimTemplates.add(persistentVolumeClaim);
                                }
                            }
                            statefulSetDetail.setVolumeClaimTemplates(volumeClaimTemplates);

                            // podTemplate
                            statefulSetDetail.setPodTemplate(this.setPodTemplateSpec(statefulSetRow.getSpec().getTemplate(), k8sJson));
                            statefulSetDetail.setReplicas(statefulSetRow.getSpec().getReplicas());

                            statefulSet.setDetail(statefulSetDetail);

                            // Horizontal Pod Autoscalers
                            if(MapUtils.isNotEmpty(horizontalPodAutoscalerMap)){
                                K8sHorizontalPodAutoscalerVO horizontalPodAutoscaler = horizontalPodAutoscalerMap.get(statefulSetRow.getMetadata().getName());
                                if(horizontalPodAutoscaler != null){
                                    statefulSet.setHorizontalPodAutoscalers(Lists.newArrayList(horizontalPodAutoscaler));
                                }
                            }

                            statefulSets.add(statefulSet);
                        }
                    }
                }else if(apiVerKindType.getGroupType() == K8sApiGroupType.APPS && apiVerKindType.getApiType() == K8sApiType.V1BETA2){
                    // StatefulSet 조회
                    List<V1beta2StatefulSet> v1beta2StatefulSets = k8sWorker.getStatefulSetsV1beta2(cluster, namespace, field, label);

                    if(CollectionUtils.isNotEmpty(v1beta2StatefulSets)){

                        // horizontalPodAutoscaler 조회
                        Map<String, K8sHorizontalPodAutoscalerVO> horizontalPodAutoscalerMap = this.convertHorizontalPodAutoscalerDataMap(cluster, namespace, field, label);

                        for(V1beta2StatefulSet statefulSetRow : v1beta2StatefulSets){
                            statefulSet = new K8sStatefulSetVO();
                            statusMap = new HashMap<>();
                            readyReplicas = statefulSetRow.getStatus().getReadyReplicas() != null ? statefulSetRow.getStatus().getReadyReplicas() : 0;

                            // StatefulSet 목록
                            statefulSet.setLabel(label);
                            statefulSet.setNamespace(statefulSetRow.getMetadata().getNamespace());
                            statefulSet.setName(statefulSetRow.getMetadata().getName());
                            statefulSet.setLabels(statefulSetRow.getMetadata().getLabels());
                            statefulSet.setReadyPodCnt(readyReplicas);
                            statefulSet.setDesiredPodCnt(statefulSetRow.getSpec().getReplicas());
                            statefulSet.setCreationTimestamp(statefulSetRow.getMetadata().getCreationTimestamp());
                            statefulSet.setImages(statefulSetRow.getSpec().getTemplate().getSpec().getContainers().stream().map(c -> c.getImage()).collect(Collectors.toList()));
                            statefulSet.setDeployment(k8sJson.serialize(statefulSetRow));
                            statefulSet.setDeploymentYaml(Yaml.getSnakeYaml().dumpAsMap(statefulSetRow));

                            // StatefulSet Detail
                            K8sStatefulSetDetailVO statefulSetDetail = new K8sStatefulSetDetailVO();
                            statefulSetDetail.setName(statefulSetRow.getMetadata().getName());
                            statefulSetDetail.setNamespace(statefulSetRow.getMetadata().getNamespace());
                            statefulSetDetail.setLabels(statefulSetRow.getMetadata().getLabels());
                            statefulSetDetail.setAnnotations(statefulSetRow.getMetadata().getAnnotations());
                            statefulSetDetail.setCreationTime(statefulSetRow.getMetadata().getCreationTimestamp());
                            String selectorJson = k8sJson.serialize(statefulSetRow.getSpec().getSelector());
                            statefulSetDetail.setSelector(k8sJson.getGson().fromJson(selectorJson, K8sLabelSelectorVO.class));
                            statefulSetDetail.setStrategy(statefulSetRow.getSpec().getUpdateStrategy().getType());
                            statefulSetDetail.setPodManagementPolicy(statefulSetRow.getSpec().getPodManagementPolicy());
                            statefulSetDetail.setRevisionHistoryLimit(statefulSetRow.getSpec().getRevisionHistoryLimit() != null ? statefulSetRow.getSpec().getRevisionHistoryLimit().toString() : "Not set");
                            statefulSetDetail.setRollingUpdate(statefulSetRow.getSpec().getUpdateStrategy().getRollingUpdate() != null ? mapper.readValue(k8sJson.serialize(statefulSetRow.getSpec().getUpdateStrategy().getRollingUpdate()), new TypeReference<Map<String, Object>>(){}) : new HashMap<>());
                            statusMap.put("collisionCount", statefulSetRow.getStatus().getCollisionCount() != null ? statefulSetRow.getStatus().getCollisionCount() : 0);
                            statusMap.put("current", statefulSetRow.getStatus().getCurrentReplicas() != null ? statefulSetRow.getStatus().getCurrentReplicas() : 0);
                            statusMap.put("ready", statefulSetRow.getStatus().getReadyReplicas() != null ? statefulSetRow.getStatus().getReadyReplicas() : 0);
                            statusMap.put("replicas", statefulSetRow.getStatus().getReplicas() != null ? statefulSetRow.getStatus().getReplicas() : 0);
                            statusMap.put("updated", statefulSetRow.getStatus().getUpdatedReplicas() != null ? statefulSetRow.getStatus().getUpdatedReplicas() : 0);
                            statefulSetDetail.setStatus(statusMap);
                            List<K8sPersistentVolumeClaimVO> volumeClaimTemplates = new ArrayList<>();
                            if (CollectionUtils.isNotEmpty(statefulSetRow.getSpec().getVolumeClaimTemplates())) {
                                for (V1PersistentVolumeClaim volumeClaimRow : statefulSetRow.getSpec().getVolumeClaimTemplates()) {
                                    K8sPersistentVolumeClaimVO persistentVolumeClaim = new K8sPersistentVolumeClaimVO();
                                    persistentVolumeClaim.setName(volumeClaimRow.getMetadata().getName());
                                    persistentVolumeClaim.setStorageClassName(volumeClaimRow.getSpec().getStorageClassName());
                                    persistentVolumeClaim.setCapacity(k8sWorker.convertReasourceMap(volumeClaimRow.getSpec().getResources().getRequests()));
                                    persistentVolumeClaim.setAccessModes(volumeClaimRow.getSpec().getAccessModes());
                                    volumeClaimTemplates.add(persistentVolumeClaim);
                                }
                            }
                            statefulSetDetail.setVolumeClaimTemplates(volumeClaimTemplates);

                            // podTemplate
                            statefulSetDetail.setPodTemplate(this.setPodTemplateSpec(statefulSetRow.getSpec().getTemplate(), k8sJson));
                            statefulSetDetail.setReplicas(statefulSetRow.getSpec().getReplicas());

                            statefulSet.setDetail(statefulSetDetail);

                            // Horizontal Pod Autoscalers
                            if(MapUtils.isNotEmpty(horizontalPodAutoscalerMap)){
                                K8sHorizontalPodAutoscalerVO horizontalPodAutoscaler = horizontalPodAutoscalerMap.get(statefulSetRow.getMetadata().getName());
                                if(horizontalPodAutoscaler != null){
                                    statefulSet.setHorizontalPodAutoscalers(Lists.newArrayList(horizontalPodAutoscaler));
                                }
                            }

                            statefulSets.add(statefulSet);
                        }
                    }
                }else if(apiVerKindType.getGroupType() == K8sApiGroupType.APPS && apiVerKindType.getApiType() == K8sApiType.V1){
                    // StatefulSet 조회
                    List<V1StatefulSet> v1StatefulSets = k8sWorker.getStatefulSetsV1(cluster, namespace, field, label);

                    if(CollectionUtils.isNotEmpty(v1StatefulSets)){
                        // horizontalPodAutoscaler 조회 (2020.01.06 : Namespace 전체 조회하여 Mapping 하도록 변경)
                        Map<String, K8sHorizontalPodAutoscalerVO> horizontalPodAutoscalerMap = this.convertHorizontalPodAutoscalerDataMap(cluster, namespace, null, null);
//                Map<String, K8sHorizontalPodAutoscalerVO> horizontalPodAutoscalerMap = this.convertHorizontalPodAutoscalerDataMap(cluster, namespace, field, label);

                        for(V1StatefulSet statefulSetRow : v1StatefulSets){
                            statefulSet = new K8sStatefulSetVO();
                            statusMap = new HashMap<>();
                            readyReplicas = statefulSetRow.getStatus().getReadyReplicas() != null ? statefulSetRow.getStatus().getReadyReplicas() : 0;

                            // StatefulSet 목록
                            statefulSet.setLabel(label);
                            statefulSet.setNamespace(statefulSetRow.getMetadata().getNamespace());
                            statefulSet.setName(statefulSetRow.getMetadata().getName());
                            statefulSet.setLabels(statefulSetRow.getMetadata().getLabels());
                            statefulSet.setReadyPodCnt(readyReplicas);
                            statefulSet.setDesiredPodCnt(statefulSetRow.getSpec().getReplicas());
                            statefulSet.setCreationTimestamp(statefulSetRow.getMetadata().getCreationTimestamp());
                            statefulSet.setImages(statefulSetRow.getSpec().getTemplate().getSpec().getContainers().stream().map(c -> c.getImage()).collect(Collectors.toList()));
                            statefulSet.setDeployment(k8sJson.serialize(statefulSetRow));
                            statefulSet.setDeploymentYaml(Yaml.getSnakeYaml().dumpAsMap(statefulSetRow));

                            // StatefulSet Detail
                            K8sStatefulSetDetailVO statefulSetDetail = new K8sStatefulSetDetailVO();
                            statefulSetDetail.setName(statefulSetRow.getMetadata().getName());
                            statefulSetDetail.setNamespace(statefulSetRow.getMetadata().getNamespace());
                            statefulSetDetail.setLabels(statefulSetRow.getMetadata().getLabels());
                            statefulSetDetail.setAnnotations(statefulSetRow.getMetadata().getAnnotations());
                            statefulSetDetail.setCreationTime(statefulSetRow.getMetadata().getCreationTimestamp());
                            String selectorJson = k8sJson.serialize(statefulSetRow.getSpec().getSelector());
                            statefulSetDetail.setSelector(k8sJson.getGson().fromJson(selectorJson, K8sLabelSelectorVO.class));
                            statefulSetDetail.setStrategy(statefulSetRow.getSpec().getUpdateStrategy().getType());
                            statefulSetDetail.setPodManagementPolicy(statefulSetRow.getSpec().getPodManagementPolicy());
                            statefulSetDetail.setRevisionHistoryLimit(statefulSetRow.getSpec().getRevisionHistoryLimit() != null ? statefulSetRow.getSpec().getRevisionHistoryLimit().toString() : "Not set");
                            statefulSetDetail.setRollingUpdate(statefulSetRow.getSpec().getUpdateStrategy().getRollingUpdate() != null ? mapper.readValue(k8sJson.serialize(statefulSetRow.getSpec().getUpdateStrategy().getRollingUpdate()), new TypeReference<Map<String, Object>>(){}) : new HashMap<>());
                            statusMap.put("collisionCount", statefulSetRow.getStatus().getCollisionCount() != null ? statefulSetRow.getStatus().getCollisionCount() : 0);
                            statusMap.put("current", statefulSetRow.getStatus().getCurrentReplicas() != null ? statefulSetRow.getStatus().getCurrentReplicas() : 0);
                            statusMap.put("ready", statefulSetRow.getStatus().getReadyReplicas() != null ? statefulSetRow.getStatus().getReadyReplicas() : 0);
                            statusMap.put("replicas", statefulSetRow.getStatus().getReplicas() != null ? statefulSetRow.getStatus().getReplicas() : 0);
                            statusMap.put("updated", statefulSetRow.getStatus().getUpdatedReplicas() != null ? statefulSetRow.getStatus().getUpdatedReplicas() : 0);
                            statefulSetDetail.setStatus(statusMap);
                            List<K8sPersistentVolumeClaimVO> volumeClaimTemplates = new ArrayList<>();
                            if (CollectionUtils.isNotEmpty(statefulSetRow.getSpec().getVolumeClaimTemplates())) {
                                for (V1PersistentVolumeClaim volumeClaimRow : statefulSetRow.getSpec().getVolumeClaimTemplates()) {
                                    K8sPersistentVolumeClaimVO persistentVolumeClaim = new K8sPersistentVolumeClaimVO();
                                    persistentVolumeClaim.setName(volumeClaimRow.getMetadata().getName());
                                    persistentVolumeClaim.setStorageClassName(volumeClaimRow.getSpec().getStorageClassName());
                                    persistentVolumeClaim.setCapacity(k8sWorker.convertReasourceMap(volumeClaimRow.getSpec().getResources().getRequests()));
                                    persistentVolumeClaim.setAccessModes(volumeClaimRow.getSpec().getAccessModes());
                                    volumeClaimTemplates.add(persistentVolumeClaim);
                                }
                            }
                            statefulSetDetail.setVolumeClaimTemplates(volumeClaimTemplates);

                            // podTemplate
                            statefulSetDetail.setPodTemplate(this.setPodTemplateSpec(statefulSetRow.getSpec().getTemplate(), k8sJson));
                            statefulSetDetail.setReplicas(statefulSetRow.getSpec().getReplicas());

                            statefulSet.setDetail(statefulSetDetail);

                            // Horizontal Pod Autoscalers
                            if(MapUtils.isNotEmpty(horizontalPodAutoscalerMap)){
//                        // 2020.01.06 : HPA의 TargetRef 정보로 매칭하도록 변경..
//                        K8sHorizontalPodAutoscalerVO horizontalPodAutoscaler = horizontalPodAutoscalerMap.get(deploymentRow.getMetadata().getName());
//                        if(horizontalPodAutoscaler != null){
//                            deployment.setHorizontalPodAutoscalers(Lists.newArrayList(horizontalPodAutoscaler));
//                        }
                                List<K8sHorizontalPodAutoscalerVO> hpaList = new ArrayList<>();
                                for(String key : horizontalPodAutoscalerMap.keySet()) {
                                    K8sHorizontalPodAutoscalerVO hpa = horizontalPodAutoscalerMap.get(key);
                                    if(hpa != null && hpa.getScaleTargetRef() != null && StringUtils.isNotBlank(hpa.getScaleTargetRef().getKind()) && StringUtils.isNotBlank(hpa.getScaleTargetRef().getName())) {
                                        // 매칭되는 Autoscaler를 찾으면 break; => 여러개일 수 있으므로 List에 추가..
                                        if(K8sApiKindType.STATEFUL_SET.getValue().equals(hpa.getScaleTargetRef().getKind()) && statefulSet.getName().equals(hpa.getScaleTargetRef().getName())) {
                                            hpaList.add(hpa);
                                        }
                                    }
                                }
                                statefulSet.setHorizontalPodAutoscalers(hpaList);
                            }

                            statefulSets.add(statefulSet);
                        }
                    }
                }
            } else {
                log.warn("convertStatefulSetDataList cluster {} version {} not supported.", cluster.getClusterName(), cluster.getK8sVersion());
            }
        } else {
            log.warn("convertStatefulSetDataList - cluster is null.");
        }

        return statefulSets;
    }

    /**
     * DaemonSet 생성
     *
     * @param cluster
     * @param server
     * @param context
     * @return
     * @throws Exception
     */
    public K8sDaemonSetVO createDaemonSet(ClusterVO cluster, ServerDetailVO server, ExecutingContextVO context) throws Exception {

        // Convert to JsonObject
        log.debug("Converted Server creation request: {}", JsonUtils.toGson(server));

        K8sDaemonSetVO k8sDaemonSet = null;
        K8sApiVerKindType apiVerKindType = ResourceUtil.getApiVerKindType(cluster, K8sApiKindType.DAEMON_SET);

        if (apiVerKindType != null) {
            if (apiVerKindType.getGroupType() == K8sApiGroupType.APPS && apiVerKindType.getApiType() == K8sApiType.V1) {
                // Create DaemonSet
                V1DaemonSet v1DaemonSet = K8sSpecFactory.buildDaemonSetV1(server, cluster.getNamespaceName());
                v1DaemonSet.getMetadata().putAnnotationsItem(KubeConstants.ANNOTATION_COCKTAIL_DEPLOY_DATETIME, Utils.getNowDateTime());

                final V1Namespace v1Namespace = k8sWorker.getNamespaceV1(cluster, cluster.getNamespaceName());
                if (v1Namespace != null && this.isServiceMeshMemberRollCustomResourceDefinitions(cluster)) {
                    this.addPodTemplateAnnotations(v1Namespace, this.getV1ObjectMetaOfWorkload().apply(v1DaemonSet));
                }

                log.debug("########## V1DaemonSet JSON request: {}", JsonUtils.toGson(v1DaemonSet));
                v1DaemonSet = k8sWorker.createDaemonSetV1(cluster, cluster.getNamespaceName(), v1DaemonSet, false);
                log.debug("########## V1DaemonSet JSON response: {}", JsonUtils.toGson(v1DaemonSet));
                Thread.sleep(200);

                k8sDaemonSet = this.getDaemonSet(cluster, cluster.getNamespaceName(), server.getComponent().getComponentName(), context);
            }
        }

        return k8sDaemonSet;
    }

    /**
     * DaemonSet 수정
     *
     * @param cluster
     * @param server
     * @param context
     * @return
     * @throws Exception
     */
    public K8sDaemonSetVO updateDaemonSet(ClusterVO cluster, ServerDetailVO server, ExecutingContextVO context) throws Exception {

        // Convert to JsonObject
        log.debug("Converted Server creation request: {}", JsonUtils.toGson(server));

        K8sDaemonSetVO k8sDaemonSet = null;
        K8sApiVerKindType apiVerKindType = ResourceUtil.getApiVerKindType(cluster, K8sApiKindType.DAEMON_SET);

        if (apiVerKindType != null) {
            if (apiVerKindType.getGroupType() == K8sApiGroupType.APPS && apiVerKindType.getApiType() == K8sApiType.V1) {

                V1DaemonSet updatedDaemonSet = K8sSpecFactory.buildDaemonSetV1(server, cluster.getNamespaceName());

                // 현재 DaemonSet 조회
                V1DaemonSet currentDaemonSet = k8sWorker.getDaemonSetV1(cluster, cluster.getNamespaceName(), server.getComponent().getComponentName());

                // patchJson 으로 변경
                List<JsonObject> patchBody = k8sPatchSpecFactory.buildPatch(currentDaemonSet, updatedDaemonSet);
                log.debug("########## DaemonSet patchBody JSON: {}", JsonUtils.toGson(patchBody));
                // patch
                k8sWorker.patchDaemonSetV1(cluster, cluster.getNamespaceName(), server.getComponent().getComponentName(), patchBody, false);

                Thread.sleep(100);

                k8sDaemonSet = this.getDaemonSet(cluster, cluster.getNamespaceName(), server.getComponent().getComponentName(), context);
            }
        }

        return k8sDaemonSet;
    }

    /**
     * DaemonSet 수정
     *
     * @param cluster
     * @param updatedDaemonSet
     * @param context
     * @return
     * @throws Exception
     */
    public void patchDaemonSetV1(ClusterVO cluster, String namespace, V1DaemonSet updatedDaemonSet, ExecutingContextVO context) throws Exception {

        // 현재 DaemonSet 조회
        V1DaemonSet currentDaemonSet = k8sWorker.getDaemonSetV1(cluster, namespace, updatedDaemonSet.getMetadata().getName());
        currentDaemonSet.setStatus(null);

        // patchJson 으로 변경
        List<JsonObject> patchBody = k8sPatchSpecFactory.buildPatch(currentDaemonSet, updatedDaemonSet);
        log.debug("########## DaemonSet patchBody JSON: {}", JsonUtils.toGson(patchBody));
        // patch
        k8sWorker.patchDaemonSetV1(cluster, namespace, updatedDaemonSet.getMetadata().getName(), patchBody, false);

    }

    /**
     * DaemonSet 수정
     *
     * @param cluster
     * @param namespace
     * @param currentDaemonSet
     * @param updatedDaemonSet
     * @param dryRun
     * @param context
     * @throws Exception
     */
    public void patchDaemonSetV1(ClusterVO cluster, String namespace, V1DaemonSet currentDaemonSet, V1DaemonSet updatedDaemonSet, boolean dryRun, ExecutingContextVO context) throws Exception {

        // 현재 DaemonSet 조회
        currentDaemonSet.setStatus(null);
        updatedDaemonSet.setStatus(null);

        // patchJson 으로 변경
        List<JsonObject> patchBody = k8sPatchSpecFactory.buildPatch(currentDaemonSet, updatedDaemonSet);
        log.debug("########## DaemonSet patchBody JSON: {}", JsonUtils.toGson(patchBody));
        // patch
        k8sWorker.patchDaemonSetV1(cluster, namespace, updatedDaemonSet.getMetadata().getName(), patchBody, dryRun);

    }

    public void replaceDaemonSetV1(ClusterVO cluster, String namespace, V1DaemonSet updatedDaemonSet, ExecutingContextVO context) throws Exception {
        k8sWorker.replaceDaemonSetV1(cluster, namespace, updatedDaemonSet.getMetadata().getName(), updatedDaemonSet);

    }

    public void deleteDaemonSet(ClusterVO cluster, ServerDetailVO serverParam, ExecutingContextVO context) throws Exception {
        String name = ResourceUtil.getUniqueName(serverParam.getComponent().getComponentName());
        log.info("Converted ServerDetailVO INFO: {}", JsonUtils.toGson(serverParam));

        this.deleteDaemonSet(cluster, name, context);
    }

    public void deleteDaemonSet(ClusterVO cluster, String name, ExecutingContextVO context) throws Exception {
        K8sApiVerKindType apiVerKindType = ResourceUtil.getApiVerKindType(cluster, K8sApiKindType.DAEMON_SET);

        if (apiVerKindType != null) {
            if (apiVerKindType.getGroupType() == K8sApiGroupType.APPS && apiVerKindType.getApiType() == K8sApiType.V1) {

                /**
                 * DaemonSet Delete
                 */
                log.debug("DaemonSet to delete: {}", name);
                // Get DaemonSet
                V1DaemonSet v1DaemonSet = k8sWorker.getDaemonSetV1(cluster, cluster.getNamespaceName(), name);

                // DaemonSet가 존재시 Delete DaemonSet
                if (v1DaemonSet != null) {
                    log.debug("### DaemonSet to delete: {}", v1DaemonSet.getMetadata().getName());

                    V1Status resultObj = k8sWorker.deleteDaemonSetV1(cluster, cluster.getNamespaceName(), name);

                    log.debug("### DaemonSet to delete status: {}", resultObj);
                }
            } else {
                log.error("fail deleteServer!! ( invalid K8sAPIType : k8sApiType[{}] ) cluster:[{}], name: [{}]", K8sApiType.values(), JsonUtils.toGson(cluster), name);
                /**
                 * DaemonSet Delete
                 */
                log.debug("DaemonSet to delete: {}", name);
                // Get DaemonSet
                V1DaemonSet v1DaemonSet = k8sWorker.getDaemonSetV1(cluster, cluster.getNamespaceName(), name);

                // DaemonSet가 존재시 Delete DaemonSet
                if (v1DaemonSet != null) {
                    log.debug("### DaemonSet to delete: {}", v1DaemonSet.getMetadata().getName());

                    V1Status resultObj = k8sWorker.deleteDaemonSetV1(cluster, cluster.getNamespaceName(), name);

                    log.debug("### DaemonSet to delete status: {}", resultObj);
                }
            }
        }

    }

    /**
     * K8S DaemonSet 정보 조회
     *
     * @param cluster
     * @param namespace
     * @param fieldSelector
     * @param labelSelector
     * @param context
     * @return
     * @throws Exception
     */
    public List<K8sDaemonSetVO> getDaemonSets(ClusterVO cluster, String namespace, String fieldSelector, String labelSelector, ExecutingContextVO context) throws Exception{

        try {
            if(cluster != null && StringUtils.isNotBlank(namespace)){

                List<K8sDaemonSetVO> k8sDaemonSets = this.convertDaemonSetDataList(cluster, namespace, fieldSelector, labelSelector, context);

                if(CollectionUtils.isNotEmpty(k8sDaemonSets)){
                    return k8sDaemonSets;
                }else{
                    return new ArrayList<>();
                }
            }else{
                throw new CocktailException("cluster/namespace/name is null.", ExceptionType.InvalidParameter);
            }
        } catch (Exception e) {
            throw new CocktailException("getDaemonSets fail!!", e, ExceptionType.K8sCocktailCloudInquireFail);
        }
    }

    /**
     * K8S DaemonSet 정보 조회
     *
     * @param clusterSeq
     * @param namespace
     * @param name
     * @param context
     * @return
     * @throws Exception
     */
    public K8sDaemonSetVO getDaemonSet(Integer clusterSeq, String namespace, String name, ExecutingContextVO context) throws Exception {
        try {
            if (clusterSeq != null) {
                IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);

                ClusterVO cluster = clusterDao.getCluster(clusterSeq);

                if (cluster != null) {
                    return this.getDaemonSet(cluster, namespace, name, context);
                }
                else {
                    throw new CocktailException("cluster not found.", ExceptionType.InvalidParameter);
                }
            }
            else {
                throw new CocktailException("cluster not found.", ExceptionType.InvalidParameter);
            }
        } catch (CocktailException e) {
            throw new CocktailException("getDaemonSet fail!!", e, ExceptionType.K8sCocktailCloudInquireFail);
        }
    }

    /**
     * K8S DaemonSet 정보 조회
     *
     * @param cluster
     * @param namespace
     * @param name
     * @param context
     * @return
     * @throws Exception
     */
    public K8sDaemonSetVO getDaemonSet(ClusterVO cluster, String namespace, String name, ExecutingContextVO context) throws Exception{

        try {
            if(cluster != null && StringUtils.isNotBlank(namespace) && StringUtils.isNotBlank(name)){
                String field = String.format("%s.%s=%s", KubeConstants.META, KubeConstants.NAME, name);

                List<K8sDaemonSetVO> k8sDaemonSets = this.getDaemonSets(cluster, namespace, field, null, context);

                if(CollectionUtils.isNotEmpty(k8sDaemonSets)){
                    return k8sDaemonSets.get(0);
                }else{
                    return null;
                }
            }else{
                throw new CocktailException("cluster/namespace/name is null.", ExceptionType.InvalidParameter);
            }
        } catch (CocktailException e) {
            throw new CocktailException("getDaemonSet fail!!", e, ExceptionType.K8sCocktailCloudInquireFail);
        }
    }

    /**
     * K8S DaemonSet 정보 조회
     *
     * @param clusterSeq
     * @param namespaceName
     * @return
     * @throws Exception
     */
    public List<K8sDaemonSetVO> getDaemonSetsByCluster(Integer clusterSeq, String namespaceName, ExecutingContextVO context) throws Exception{

        try {
            if(clusterSeq != null){
                IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);

                ClusterVO cluster = clusterDao.getCluster(clusterSeq);

                if(cluster != null){
                    return this.getDaemonSets(cluster, namespaceName, null, null, context);
                }else{
                    throw new CocktailException("cluster is null.", ExceptionType.InvalidParameter);
                }
            }else{
                throw new CocktailException("componentSeq is null.", ExceptionType.InvalidParameter);
            }
        } catch (Exception e) {
            throw new CocktailException("getDaemonSetsByCluster fail!!", e, ExceptionType.K8sCocktailCloudInquireFail);
        }
    }

    /**
     * K8S DaemonSet 정보 조회 후,
     *
     * - V1BETA1
     * V1beta1DaemonSet -> K8sDaemonSetVO 변환
     *
     * - V1BETA2
     * V1beta2DaemonSet -> K8sDaemonSetVO 변환
     *
     * - V1
     * V1DaemonSet -> K8sDaemonSetVO 변환
     *
     * @param cluster
     * @param label
     * @return
     * @throws Exception
     */
    public List<K8sDaemonSetVO> convertDaemonSetDataList(ClusterVO cluster, String namespace, String field, String label, ExecutingContextVO context) throws Exception {

        List<K8sDaemonSetVO> daemonSets = new ArrayList<>();

        if (cluster != null) {
            K8sApiVerKindType apiVerKindType = ResourceUtil.getApiVerKindType(cluster, K8sApiKindType.DAEMON_SET);

            if (apiVerKindType != null) {
                K8sDaemonSetVO daemonSet = null;
                Map<String, Integer> statusMap = null;
                Integer readyPodCnt = 0;

                ObjectMapper mapper = K8sMapperUtils.getMapper();

                // joda.datetime Serialization
                JSON k8sJson = new JSON();

                if(apiVerKindType.getGroupType() == K8sApiGroupType.EXTENSIONS && apiVerKindType.getApiType() == K8sApiType.V1BETA1){
                    // DaemonSet 조회
                    List<V1beta1DaemonSet> v1beta1DaemonSets = k8sWorker.getDaemonSetsV1beta1(cluster, namespace, field, label);

                    if(CollectionUtils.isNotEmpty(v1beta1DaemonSets)){

                        for(V1beta1DaemonSet daemonSetRow : v1beta1DaemonSets){
                            daemonSet = new K8sDaemonSetVO();
                            statusMap = new HashMap<>();
                            readyPodCnt = daemonSetRow.getStatus().getNumberReady() != null ? daemonSetRow.getStatus().getNumberReady() : 0;

                            // DaemonSet 목록
                            daemonSet.setLabel(label);
                            daemonSet.setNamespace(daemonSetRow.getMetadata().getNamespace());
                            daemonSet.setName(daemonSetRow.getMetadata().getName());
                            daemonSet.setLabels(daemonSetRow.getMetadata().getLabels());
                            daemonSet.setReadyPodCnt(readyPodCnt);
                            daemonSet.setDesiredPodCnt(daemonSetRow.getStatus().getDesiredNumberScheduled());
                            daemonSet.setCreationTimestamp(daemonSetRow.getMetadata().getCreationTimestamp());
                            daemonSet.setImages(daemonSetRow.getSpec().getTemplate().getSpec().getContainers().stream().map(c -> c.getImage()).collect(Collectors.toList()));
                            daemonSet.setDeployment(k8sJson.serialize(daemonSetRow));
                            daemonSet.setDeploymentYaml(Yaml.getSnakeYaml().dumpAsMap(daemonSetRow));

                            // DaemonSet Detail
                            K8sDaemonSetDetailVO detail = new K8sDaemonSetDetailVO();
                            detail.setName(daemonSetRow.getMetadata().getName());
                            detail.setNamespace(daemonSetRow.getMetadata().getNamespace());
                            detail.setLabels(daemonSetRow.getMetadata().getLabels());
                            detail.setAnnotations(daemonSetRow.getMetadata().getAnnotations());
                            detail.setCreationTime(daemonSetRow.getMetadata().getCreationTimestamp());
                            String selectorJson = k8sJson.serialize(daemonSetRow.getSpec().getSelector());
                            detail.setSelector(k8sJson.getGson().fromJson(selectorJson, K8sLabelSelectorVO.class));
                            detail.setStrategy(daemonSetRow.getSpec().getUpdateStrategy().getType());
                            detail.setMinReadySeconds(daemonSetRow.getSpec().getMinReadySeconds() != null ? daemonSetRow.getSpec().getMinReadySeconds().intValue() : 0);
                            detail.setRevisionHistoryLimit(daemonSetRow.getSpec().getRevisionHistoryLimit() != null ? daemonSetRow.getSpec().getRevisionHistoryLimit().toString() : "Not set");
                            detail.setRollingUpdate(daemonSetRow.getSpec().getUpdateStrategy().getRollingUpdate() != null ? mapper.readValue(k8sJson.serialize(daemonSetRow.getSpec().getUpdateStrategy().getRollingUpdate()), new TypeReference<Map<String, Object>>(){}) : new HashMap<>());
                            statusMap.put("current", daemonSetRow.getStatus().getCurrentNumberScheduled() != null ? daemonSetRow.getStatus().getCurrentNumberScheduled() : 0);
                            statusMap.put("desired", daemonSetRow.getStatus().getDesiredNumberScheduled() != null ? daemonSetRow.getStatus().getDesiredNumberScheduled() : 0);
                            statusMap.put("ready", daemonSetRow.getStatus().getNumberReady() != null ? daemonSetRow.getStatus().getNumberReady() : 0);
                            statusMap.put("up-to-date", daemonSetRow.getStatus().getUpdatedNumberScheduled() != null ? daemonSetRow.getStatus().getUpdatedNumberScheduled() : 0);
                            statusMap.put("available", daemonSetRow.getStatus().getNumberAvailable() != null ? daemonSetRow.getStatus().getNumberAvailable() : 0);
                            detail.setStatus(statusMap);
                            // podTemplate
                            detail.setPodTemplate(this.setPodTemplateSpec(daemonSetRow.getSpec().getTemplate(), k8sJson));
                            daemonSet.setDetail(detail);


                            daemonSets.add(daemonSet);
                        }
                    }
                }else if(apiVerKindType.getGroupType() == K8sApiGroupType.APPS && apiVerKindType.getApiType() == K8sApiType.V1BETA2){
                    // DaemonSet 조회
                    List<V1beta2DaemonSet> v1beta2DaemonSets = k8sWorker.getDaemonSetsV1beta2(cluster, namespace, field, label);

                    if(CollectionUtils.isNotEmpty(v1beta2DaemonSets)){

                        for(V1beta2DaemonSet daemonSetRow : v1beta2DaemonSets){
                            daemonSet = new K8sDaemonSetVO();
                            statusMap = new HashMap<>();
                            readyPodCnt = daemonSetRow.getStatus().getNumberReady() != null ? daemonSetRow.getStatus().getNumberReady() : 0;

                            // DaemonSet 목록
                            daemonSet.setLabel(label);
                            daemonSet.setNamespace(daemonSetRow.getMetadata().getNamespace());
                            daemonSet.setName(daemonSetRow.getMetadata().getName());
                            daemonSet.setLabels(daemonSetRow.getMetadata().getLabels());
                            daemonSet.setReadyPodCnt(readyPodCnt);
                            daemonSet.setDesiredPodCnt(daemonSetRow.getStatus().getDesiredNumberScheduled());
                            daemonSet.setCreationTimestamp(daemonSetRow.getMetadata().getCreationTimestamp());
                            daemonSet.setImages(daemonSetRow.getSpec().getTemplate().getSpec().getContainers().stream().map(c -> c.getImage()).collect(Collectors.toList()));
                            daemonSet.setDeployment(k8sJson.serialize(daemonSetRow));
                            daemonSet.setDeploymentYaml(Yaml.getSnakeYaml().dumpAsMap(daemonSetRow));

                            // DaemonSet Detail
                            K8sDaemonSetDetailVO detail = new K8sDaemonSetDetailVO();
                            detail.setName(daemonSetRow.getMetadata().getName());
                            detail.setNamespace(daemonSetRow.getMetadata().getNamespace());
                            detail.setLabels(daemonSetRow.getMetadata().getLabels());
                            detail.setAnnotations(daemonSetRow.getMetadata().getAnnotations());
                            detail.setCreationTime(daemonSetRow.getMetadata().getCreationTimestamp());
                            String selectorJson = k8sJson.serialize(daemonSetRow.getSpec().getSelector());
                            detail.setSelector(k8sJson.getGson().fromJson(selectorJson, K8sLabelSelectorVO.class));
                            detail.setStrategy(daemonSetRow.getSpec().getUpdateStrategy().getType());
                            detail.setMinReadySeconds(daemonSetRow.getSpec().getMinReadySeconds() != null ? daemonSetRow.getSpec().getMinReadySeconds().intValue() : 0);
                            detail.setRevisionHistoryLimit(daemonSetRow.getSpec().getRevisionHistoryLimit() != null ? daemonSetRow.getSpec().getRevisionHistoryLimit().toString() : "Not set");
                            detail.setRollingUpdate(daemonSetRow.getSpec().getUpdateStrategy().getRollingUpdate() != null ? mapper.readValue(k8sJson.serialize(daemonSetRow.getSpec().getUpdateStrategy().getRollingUpdate()), new TypeReference<Map<String, Object>>(){}) : new HashMap<>());
                            statusMap.put("current", daemonSetRow.getStatus().getCurrentNumberScheduled() != null ? daemonSetRow.getStatus().getCurrentNumberScheduled() : 0);
                            statusMap.put("desired", daemonSetRow.getStatus().getDesiredNumberScheduled() != null ? daemonSetRow.getStatus().getDesiredNumberScheduled() : 0);
                            statusMap.put("ready", daemonSetRow.getStatus().getNumberReady() != null ? daemonSetRow.getStatus().getNumberReady() : 0);
                            statusMap.put("up-to-date", daemonSetRow.getStatus().getUpdatedNumberScheduled() != null ? daemonSetRow.getStatus().getUpdatedNumberScheduled() : 0);
                            statusMap.put("available", daemonSetRow.getStatus().getNumberAvailable() != null ? daemonSetRow.getStatus().getNumberAvailable() : 0);
                            detail.setStatus(statusMap);
                            // podTemplate
                            detail.setPodTemplate(this.setPodTemplateSpec(daemonSetRow.getSpec().getTemplate(), k8sJson));
                            daemonSet.setDetail(detail);


                            daemonSets.add(daemonSet);
                        }
                    }
                }else if(apiVerKindType.getGroupType() == K8sApiGroupType.APPS && apiVerKindType.getApiType() == K8sApiType.V1){
                    // DaemonSet 조회
                    List<V1DaemonSet> v1DaemonSets = k8sWorker.getDaemonSetsV1(cluster, namespace, field, label);

                    if(CollectionUtils.isNotEmpty(v1DaemonSets)){

                        for(V1DaemonSet daemonSetRow : v1DaemonSets){
                            daemonSet = new K8sDaemonSetVO();
                            statusMap = new HashMap<>();
                            readyPodCnt = daemonSetRow.getStatus().getNumberReady() != null ? daemonSetRow.getStatus().getNumberReady() : 0;

                            // DaemonSet 목록
                            daemonSet.setLabel(label);
                            daemonSet.setNamespace(daemonSetRow.getMetadata().getNamespace());
                            daemonSet.setName(daemonSetRow.getMetadata().getName());
                            daemonSet.setLabels(daemonSetRow.getMetadata().getLabels());
                            daemonSet.setReadyPodCnt(readyPodCnt);
                            daemonSet.setDesiredPodCnt(daemonSetRow.getStatus().getDesiredNumberScheduled());
                            daemonSet.setCreationTimestamp(daemonSetRow.getMetadata().getCreationTimestamp());
                            daemonSet.setImages(daemonSetRow.getSpec().getTemplate().getSpec().getContainers().stream().map(c -> c.getImage()).collect(Collectors.toList()));
                            daemonSet.setDeployment(k8sJson.serialize(daemonSetRow));
                            daemonSet.setDeploymentYaml(Yaml.getSnakeYaml().dumpAsMap(daemonSetRow));

                            // DaemonSet Detail
                            K8sDaemonSetDetailVO detail = new K8sDaemonSetDetailVO();
                            detail.setName(daemonSetRow.getMetadata().getName());
                            detail.setNamespace(daemonSetRow.getMetadata().getNamespace());
                            detail.setLabels(daemonSetRow.getMetadata().getLabels());
                            detail.setAnnotations(daemonSetRow.getMetadata().getAnnotations());
                            detail.setCreationTime(daemonSetRow.getMetadata().getCreationTimestamp());
                            String selectorJson = k8sJson.serialize(daemonSetRow.getSpec().getSelector());
                            detail.setSelector(k8sJson.getGson().fromJson(selectorJson, K8sLabelSelectorVO.class));
                            detail.setStrategy(daemonSetRow.getSpec().getUpdateStrategy().getType());
                            detail.setMinReadySeconds(daemonSetRow.getSpec().getMinReadySeconds() != null ? daemonSetRow.getSpec().getMinReadySeconds().intValue() : 0);
                            detail.setRevisionHistoryLimit(daemonSetRow.getSpec().getRevisionHistoryLimit() != null ? daemonSetRow.getSpec().getRevisionHistoryLimit().toString() : "Not set");
                            detail.setRollingUpdate(daemonSetRow.getSpec().getUpdateStrategy().getRollingUpdate() != null ? mapper.readValue(k8sJson.serialize(daemonSetRow.getSpec().getUpdateStrategy().getRollingUpdate()), new TypeReference<Map<String, Object>>(){}) : new HashMap<>());
                            statusMap.put("current", daemonSetRow.getStatus().getCurrentNumberScheduled() != null ? daemonSetRow.getStatus().getCurrentNumberScheduled() : 0);
                            statusMap.put("desired", daemonSetRow.getStatus().getDesiredNumberScheduled() != null ? daemonSetRow.getStatus().getDesiredNumberScheduled() : 0);
                            statusMap.put("ready", daemonSetRow.getStatus().getNumberReady() != null ? daemonSetRow.getStatus().getNumberReady() : 0);
                            statusMap.put("up-to-date", daemonSetRow.getStatus().getUpdatedNumberScheduled() != null ? daemonSetRow.getStatus().getUpdatedNumberScheduled() : 0);
                            statusMap.put("available", daemonSetRow.getStatus().getNumberAvailable() != null ? daemonSetRow.getStatus().getNumberAvailable() : 0);
                            detail.setStatus(statusMap);
                            // podTemplate
                            detail.setPodTemplate(this.setPodTemplateSpec(daemonSetRow.getSpec().getTemplate(), k8sJson));
                            daemonSet.setDetail(detail);


                            daemonSets.add(daemonSet);
                        }
                    }
                }
            } else {
                log.warn("convertDaemonSetDataList cluster {} version {} not supported.", cluster.getClusterName(), cluster.getK8sVersion());
            }
        } else {
            log.warn("convertDaemonSetDataList - cluster is null.");
        }

        return daemonSets;
    }

    /**
     * Job 생성
     *
     * @param cluster
     * @param server
     * @param context
     * @return
     * @throws Exception
     */
    public K8sJobVO createJob(ClusterVO cluster, ServerDetailVO server, ExecutingContextVO context) throws Exception {

        // Convert to JsonObject
        log.debug("Converted Server creation request: {}", JsonUtils.toGson(server));

        K8sJobVO k8sJob = null;
        K8sApiVerKindType apiVerKindType = ResourceUtil.getApiVerKindType(cluster, K8sApiKindType.JOB);

        if (apiVerKindType != null) {
            if (apiVerKindType.getGroupType() == K8sApiGroupType.BATCH && apiVerKindType.getApiType() == K8sApiType.V1) {
                // Create Job
                V1Job v1Job = K8sSpecFactory.buildJobV1(server, cluster.getNamespaceName());
                v1Job.getMetadata().putAnnotationsItem(KubeConstants.ANNOTATION_COCKTAIL_DEPLOY_DATETIME, Utils.getNowDateTime());

                final V1Namespace v1Namespace = k8sWorker.getNamespaceV1(cluster, cluster.getNamespaceName());
                if (v1Namespace != null && this.isServiceMeshMemberRollCustomResourceDefinitions(cluster)) {
                    this.addPodTemplateAnnotations(v1Namespace, this.getV1ObjectMetaOfWorkload().apply(v1Job));
                }

                log.debug("########## V1Job JSON request: {}", JsonUtils.toGson(v1Job));
                v1Job = k8sWorker.createJobV1(cluster, cluster.getNamespaceName(), v1Job, false);
                log.debug("########## V1Job JSON response: {}", JsonUtils.toGson(v1Job));
                Thread.sleep(200);

                k8sJob = this.getJob(cluster, cluster.getNamespaceName(), server.getComponent().getComponentName(), context);
            }
        }

        return k8sJob;
    }

    /**
     * Job 수정
     *
     * @param cluster
     * @param server
     * @param context
     * @return
     * @throws Exception
     */
    public K8sJobVO updateJob(ClusterVO cluster, ServerDetailVO server, ExecutingContextVO context) throws Exception {

        // Convert to JsonObject
        log.debug("Converted Server creation request: {}", JsonUtils.toGson(server));

        K8sJobVO k8sJob = null;
        K8sApiVerKindType apiVerKindType = ResourceUtil.getApiVerKindType(cluster, K8sApiKindType.JOB);

        if (apiVerKindType != null) {
            if (apiVerKindType.getGroupType() == K8sApiGroupType.BATCH && apiVerKindType.getApiType() == K8sApiType.V1) {

                V1Job updatedJob = K8sSpecFactory.buildJobV1(server, cluster.getNamespaceName());

                // 현재 Job 조회
                V1Job currentJob = k8sWorker.getJobV1(cluster, cluster.getNamespaceName(), server.getComponent().getComponentName());

                // patchJson 으로 변경
                List<JsonObject> patchBody = k8sPatchSpecFactory.buildPatch(currentJob, updatedJob);
                log.debug("########## Job patchBody JSON: {}", JsonUtils.toGson(patchBody));
                // patch
                k8sWorker.patchJobV1(cluster, cluster.getNamespaceName(), server.getComponent().getComponentName(), patchBody, false);

                Thread.sleep(100);

                k8sJob = this.getJob(cluster, cluster.getNamespaceName(), server.getComponent().getComponentName(), context);
            }
        }

        return k8sJob;
    }

    /**
     * Job 수정
     *
     * @param cluster
     * @param updatedJob
     * @param context
     * @return
     * @throws Exception
     */
    public void patchJobV1(ClusterVO cluster, String namespace, V1Job updatedJob, ExecutingContextVO context) throws Exception {

        // 현재 Job 조회
        V1Job currentJob = k8sWorker.getJobV1(cluster, namespace, updatedJob.getMetadata().getName());

        // patchJson 으로 변경
        List<JsonObject> patchBody = k8sPatchSpecFactory.buildPatch(currentJob, updatedJob);
        log.debug("########## Job patchBody JSON: {}", JsonUtils.toGson(patchBody));
        // patch
        k8sWorker.patchJobV1(cluster, namespace, updatedJob.getMetadata().getName(), patchBody, false);

    }

    public void replaceJobV1(ClusterVO cluster, String namespace, V1Job updatedJob, ExecutingContextVO context) throws Exception {
        k8sWorker.replaceJobV1(cluster, namespace, updatedJob.getMetadata().getName(), updatedJob);

    }

    public boolean deleteJob(ClusterVO cluster, ServerDetailVO serverParam, ExecutingContextVO context) throws Exception {
        String name = ResourceUtil.getUniqueName(serverParam.getComponent().getComponentName());
        log.info("Converted ServerDetailVO INFO: {}", JsonUtils.toGson(serverParam));

        return this.deleteJob(cluster, name, context);
    }

    public boolean deleteJob(ClusterVO cluster, String name, ExecutingContextVO context) throws Exception {
        K8sApiVerKindType apiVerKindType = ResourceUtil.getApiVerKindType(cluster, K8sApiKindType.JOB);

        if (apiVerKindType != null) {
            if (apiVerKindType.getGroupType() == K8sApiGroupType.BATCH && apiVerKindType.getApiType() == K8sApiType.V1) {

                /**
                 * Job Delete
                 */
                log.debug("Job to delete: {}", name);
                // Get Job
                V1Job v1Job = k8sWorker.getJobV1(cluster, cluster.getNamespaceName(), name);

                // Job가 존재시 Delete Job
                if (v1Job != null) {
                    log.debug("### Job to delete: {}", v1Job.getMetadata().getName());

                    V1Status resultObj = k8sWorker.deleteJobV1(cluster, cluster.getNamespaceName(), name);

                    log.debug("### Job to delete status: {}", resultObj);
                }
            } else {
                log.error("fail deleteServer!! ( invalid K8sAPIType : k8sApiType[{}] ) cluster:[{}], name: [{}]", K8sApiType.values(), JsonUtils.toGson(cluster), name);
            }
        }

        return true;
    }

    public void deleteJobV1(ClusterVO cluster, String namespace, String name, ExecutingContextVO context) throws Exception {
        V1Job v1Job = k8sWorker.getJobV1(cluster, namespace, name);

        // Job가 존재시 Delete Job
        if (v1Job != null) {
            log.debug("### Job to delete: {}", v1Job.getMetadata().getName());

            V1Status resultObj = k8sWorker.deleteJobV1(cluster, namespace, name);

            log.debug("### Job to delete status: {}", resultObj);
        }
    }

    /**
     * K8S Job 정보 조회
     *
     * @param cluster
     * @param namespace
     * @param fieldSelector
     * @param labelSelector
     * @param context
     * @return
     * @throws Exception
     */
    public List<K8sJobVO> getJobs(ClusterVO cluster, String namespace, String fieldSelector, String labelSelector, ExecutingContextVO context) throws Exception{

        try {
            if(cluster != null && StringUtils.isNotBlank(namespace)){

                List<K8sJobVO> results = this.convertJobDataList(cluster, namespace, fieldSelector, labelSelector, context);

                if(CollectionUtils.isNotEmpty(results)){
                    return results;
                }else{
                    return new ArrayList<>();
                }
            }else{
                throw new CocktailException("cluster/namespace/name is null.", ExceptionType.InvalidParameter);
            }
        } catch (Exception e) {
            throw new CocktailException("getJobs fail!!", e, ExceptionType.K8sCocktailCloudInquireFail);
        }
    }

    /**
     * K8S Job 정보 조회
     *
     * @param clusterSeq
     * @param namespace
     * @param name
     * @param context
     * @return
     * @throws Exception
     */
    public K8sJobVO getJob(Integer clusterSeq, String namespace, String name, ExecutingContextVO context) throws Exception {
        try {
            if (clusterSeq != null) {
                IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);

                ClusterVO cluster = clusterDao.getCluster(clusterSeq);

                if (cluster != null) {
                    return this.getJob(cluster, namespace, name, context);
                }
                else {
                    throw new CocktailException("cluster not found.", ExceptionType.InvalidParameter);
                }
            }
            else {
                throw new CocktailException("cluster not found.", ExceptionType.InvalidParameter);
            }
        } catch (CocktailException e) {
            throw new CocktailException("getJob fail!!", e, ExceptionType.K8sCocktailCloudInquireFail);
        }
    }

    /**
     * K8S Job 정보 조회
     *
     * @param cluster
     * @param namespace
     * @param name
     * @param context
     * @return
     * @throws Exception
     */
    public K8sJobVO getJob(ClusterVO cluster, String namespace, String name, ExecutingContextVO context) throws Exception{

        try {
            if(cluster != null && StringUtils.isNotBlank(namespace) && StringUtils.isNotBlank(name)){
                String field = String.format("%s.%s=%s", KubeConstants.META, KubeConstants.NAME, name);

                List<K8sJobVO> results = this.getJobs(cluster, namespace, field, null, context);

                if(CollectionUtils.isNotEmpty(results)){
                    return results.get(0);
                }else{
                    return null;
                }
            }else{
                throw new CocktailException("cluster/namespace/name is null.", ExceptionType.InvalidParameter);
            }
        } catch (CocktailException e) {
            throw new CocktailException("getJob fail!!", e, ExceptionType.K8sCocktailCloudInquireFail);
        }
    }

    /**
     * K8S Job 정보 조회
     *
     * @param clusterSeq
     * @param namespaceName
     * @return
     * @throws Exception
     */
    public List<K8sJobVO> getJobsByCluster(Integer clusterSeq, String namespaceName, ExecutingContextVO context) throws Exception{

        try {
            if(clusterSeq != null){
                IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);

                ClusterVO cluster = clusterDao.getCluster(clusterSeq);

                if(cluster != null){
                    return this.getJobs(cluster, namespaceName, null, null, context);
                }else{
                    throw new CocktailException("cluster is null.", ExceptionType.InvalidParameter);
                }
            }else{
                throw new CocktailException("componentSeq is null.", ExceptionType.InvalidParameter);
            }
        } catch (Exception e) {
            throw new CocktailException("getJobsByCluster fail!!", e, ExceptionType.K8sCocktailCloudInquireFail);
        }
    }

    /**
     * K8S Job 정보 조회 후,
     *
     * - V1
     * V1Job -> K8sJobVO 변환
     *
     * @param cluster
     * @param label
     * @return
     * @throws Exception
     */
    public List<K8sJobVO> convertJobDataList(ClusterVO cluster, String namespace, String field, String label, ExecutingContextVO context) throws Exception {

        List<K8sJobVO> jobs = new ArrayList<>();

        if (cluster != null) {
            K8sApiVerKindType apiVerKindType = ResourceUtil.getApiVerKindType(cluster, K8sApiKindType.JOB);

            if (apiVerKindType != null) {
                K8sJobVO job = null;
                Map<String, Integer> statusMap = null;
                Integer completionPodCnt = 0;

                // joda.datetime Serialization
                JSON k8sJson = new JSON();

                if(apiVerKindType.getGroupType() == K8sApiGroupType.BATCH && apiVerKindType.getApiType() == K8sApiType.V1){
                    // DaemonSet 조회
                    List<V1Job> v1Jobs = k8sWorker.getJobsV1(cluster, namespace, field, label);

                    if(CollectionUtils.isNotEmpty(v1Jobs)){

                        for(V1Job jobRow : v1Jobs){
                            job = new K8sJobVO();
                            statusMap = new HashMap<>();
                            completionPodCnt = jobRow.getStatus().getSucceeded() != null ? jobRow.getStatus().getSucceeded() : 0;

                            // Job 목록
                            job.setLabel(label);
                            job.setNamespace(jobRow.getMetadata().getNamespace());
                            job.setName(jobRow.getMetadata().getName());
                            job.setLabels(jobRow.getMetadata().getLabels());
                            job.setCompletionPodCnt(completionPodCnt);
                            job.setDesiredPodCnt(jobRow.getSpec().getCompletions());
                            job.setCreationTimestamp(jobRow.getMetadata().getCreationTimestamp());
                            job.setImages(jobRow.getSpec().getTemplate().getSpec().getContainers().stream().map(c -> c.getImage()).collect(Collectors.toList()));
                            job.setDeployment(k8sJson.serialize(jobRow));
                            job.setDeploymentYaml(Yaml.getSnakeYaml().dumpAsMap(jobRow));

                            // Job Detail
                            K8sJobDetailVO detail = new K8sJobDetailVO();
                            detail.setName(jobRow.getMetadata().getName());
                            detail.setNamespace(jobRow.getMetadata().getNamespace());
                            detail.setLabels(jobRow.getMetadata().getLabels());
                            detail.setAnnotations(jobRow.getMetadata().getAnnotations());
                            detail.setCreationTime(jobRow.getMetadata().getCreationTimestamp());
                            String selectorJson = k8sJson.serialize(jobRow.getSpec().getSelector());
                            detail.setSelector(k8sJson.getGson().fromJson(selectorJson, K8sLabelSelectorVO.class));
                            detail.setActiveDeadlineSeconds(jobRow.getSpec().getActiveDeadlineSeconds());
                            detail.setBackoffLimit(jobRow.getSpec().getBackoffLimit());
                            detail.setCompletions(jobRow.getSpec().getCompletions());
                            detail.setParallelism(jobRow.getSpec().getParallelism());
                            detail.setTtlSecondsAfterFinished(jobRow.getSpec().getTtlSecondsAfterFinished());

                            // ownerReference
                            if (CollectionUtils.isNotEmpty(jobRow.getMetadata().getOwnerReferences())) {
                                detail.setOwnerReferences(ResourceUtil.setOwnerReference(jobRow.getMetadata().getOwnerReferences()));
                            }

                            statusMap.put("Running", jobRow.getStatus().getActive() != null ? jobRow.getStatus().getActive() : 0);
                            statusMap.put("Succeeded", jobRow.getStatus().getSucceeded() != null ? jobRow.getStatus().getSucceeded() : 0);
                            statusMap.put("Failed", jobRow.getStatus().getFailed() != null ? jobRow.getStatus().getFailed() : 0);
                            detail.setStatus(statusMap);
                            // podTemplate
                            detail.setPodTemplate(this.setPodTemplateSpec(jobRow.getSpec().getTemplate(), k8sJson));
                            job.setDetail(detail);


                            jobs.add(job);
                        }
                    }
                }
            } else {
                log.warn("convertJobDataList cluster {} version {} not supported.", cluster.getClusterName(), cluster.getK8sVersion());
            }
        } else {
            log.warn("convertJobDataList - cluster is null.");
        }


        return jobs;
    }

    /**
     * CronJob 생성
     *
     * @param cluster
     * @param server
     * @param context
     * @return
     * @throws Exception
     */
    public K8sCronJobVO createCronJob(ClusterVO cluster, ServerDetailVO server, ExecutingContextVO context) throws Exception {

        // Convert to JsonObject
        log.debug("Converted Server creation request: {}", JsonUtils.toGson(server));

        K8sCronJobVO k8sCronJob = null;
        K8sApiVerKindType apiVerKindType = ResourceUtil.getApiVerKindType(cluster, K8sApiKindType.CRON_JOB);

        if (apiVerKindType != null) {
            if(apiVerKindType.getGroupType() == K8sApiGroupType.BATCH && apiVerKindType.getApiType() == K8sApiType.V1BETA1){
                // Create CronJob
                V1beta1CronJob cronJob = K8sSpecFactory.buildCronJobV1beta1(server, cluster.getNamespaceName());
                cronJob.getMetadata().putAnnotationsItem(KubeConstants.ANNOTATION_COCKTAIL_DEPLOY_DATETIME, Utils.getNowDateTime());

                final V1Namespace v1Namespace = k8sWorker.getNamespaceV1(cluster, cluster.getNamespaceName());
                if(v1Namespace != null && this.isServiceMeshMemberRollCustomResourceDefinitions(cluster)) {
                    this.addPodTemplateAnnotations(v1Namespace, this.getV1ObjectMetaOfWorkload().apply(cronJob));
                }
                log.debug("########## V1beta1CronJob JSON request: {}", K8sJsonUtils.getJson().serialize(cronJob));
                cronJob = k8sWorker.createCronJobV1beta1(cluster, cluster.getNamespaceName(), cronJob, false);
                log.debug("########## V1beta1CronJob JSON response: {}", K8sJsonUtils.getJson().serialize(cronJob));
                Thread.sleep(200);

                k8sCronJob = this.getCronJob(cluster, cluster.getNamespaceName(), server.getComponent().getComponentName(), context);
            } else {
                // Create CronJob
                V1CronJob cronJob = K8sSpecFactory.buildCronJobV1(server, cluster.getNamespaceName());
                cronJob.getMetadata().putAnnotationsItem(KubeConstants.ANNOTATION_COCKTAIL_DEPLOY_DATETIME, Utils.getNowDateTime());

                final V1Namespace v1Namespace = k8sWorker.getNamespaceV1(cluster, cluster.getNamespaceName());
                if(v1Namespace != null && this.isServiceMeshMemberRollCustomResourceDefinitions(cluster)) {
                    this.addPodTemplateAnnotations(v1Namespace, this.getV1ObjectMetaOfWorkload().apply(cronJob));
                }
                log.debug("########## V1CronJob JSON request: {}", K8sJsonUtils.getJson().serialize(cronJob));
                cronJob = k8sWorker.createCronJobV1(cluster, cluster.getNamespaceName(), cronJob, false);
                log.debug("########## V1beta1CronJob JSON response: {}", K8sJsonUtils.getJson().serialize(cronJob));
                Thread.sleep(200);

                k8sCronJob = this.getCronJob(cluster, cluster.getNamespaceName(), server.getComponent().getComponentName(), context);
            }
        } else {
            throw new CocktailException(String.format("Cube Cluster version[%s] is not support.", cluster.getK8sVersion()), ExceptionType.K8sNotSupported);
        }


        return k8sCronJob;
    }

    /**
     * CronJob 수정
     *
     * @param cluster
     * @param server
     * @param context
     * @return
     * @throws Exception
     */
    public K8sCronJobVO updateCronJob(ClusterVO cluster, ServerDetailVO server, ExecutingContextVO context) throws Exception {

        // Convert to JsonObject
        log.debug("Converted Server creation request: {}", JsonUtils.toGson(server));

        K8sCronJobVO k8sCronJob = null;
        K8sApiVerKindType apiVerKindType = ResourceUtil.getApiVerKindType(cluster, K8sApiKindType.CRON_JOB);

        if (apiVerKindType != null) {
            if(apiVerKindType.getGroupType() == K8sApiGroupType.BATCH && apiVerKindType.getApiType() == K8sApiType.V1BETA1){

                V1beta1CronJob updatedCronJob = K8sSpecFactory.buildCronJobV1beta1(server, cluster.getNamespaceName());

                // 현재 CronJob 조회
                V1beta1CronJob currentCronJob = k8sWorker.getCronJobV1beta1(cluster, cluster.getNamespaceName(), server.getComponent().getComponentName());

                // patchJson 으로 변경
                List<JsonObject> patchBody = k8sPatchSpecFactory.buildPatch(currentCronJob, updatedCronJob);
                log.debug("########## CronJob patchBody JSON: {}", JsonUtils.toGson(patchBody));
                // patch
                k8sWorker.patchCronJobV1beta1(cluster, cluster.getNamespaceName(), server.getComponent().getComponentName(), patchBody, false);

                Thread.sleep(100);

                k8sCronJob = this.getCronJob(cluster, cluster.getNamespaceName(), server.getComponent().getComponentName(), context);
            } else {

                V1CronJob updatedCronJob = K8sSpecFactory.buildCronJobV1(server, cluster.getNamespaceName());

                // 현재 CronJob 조회
                V1CronJob currentCronJob = k8sWorker.getCronJobV1(cluster, cluster.getNamespaceName(), server.getComponent().getComponentName());

                // patchJson 으로 변경
                List<JsonObject> patchBody = k8sPatchSpecFactory.buildPatch(currentCronJob, updatedCronJob);
                log.debug("########## CronJob patchBody JSON: {}", JsonUtils.toGson(patchBody));
                // patch
                k8sWorker.patchCronJobV1(cluster, cluster.getNamespaceName(), server.getComponent().getComponentName(), patchBody, false);

                Thread.sleep(100);

                k8sCronJob = this.getCronJob(cluster, cluster.getNamespaceName(), server.getComponent().getComponentName(), context);
            }
        } else {
            throw new CocktailException(String.format("Cube Cluster version[%s] is not support.", cluster.getK8sVersion()), ExceptionType.K8sNotSupported);
        }

        return k8sCronJob;
    }

    /**
     * CronJob V1beta1 수정
     *
     * @param cluster
     * @param updatedCronJob
     * @param context
     * @return
     * @throws Exception
     */
    public void patchCronJobV1beta1(ClusterVO cluster, String namespace, V1beta1CronJob updatedCronJob, ExecutingContextVO context) throws Exception {

        // 현재 CronJob 조회
        V1beta1CronJob currentCronJob = k8sWorker.getCronJobV1beta1(cluster, namespace, updatedCronJob.getMetadata().getName());

        // patchJson 으로 변경
        List<JsonObject> patchBody = k8sPatchSpecFactory.buildPatch(currentCronJob, updatedCronJob);
        log.debug("########## CronJob patchBody JSON: {}", JsonUtils.toGson(patchBody));
        // patch
        k8sWorker.patchCronJobV1beta1(cluster, namespace, updatedCronJob.getMetadata().getName(), patchBody, false);

    }

    /**
     * CronJob V1beta1 수정
     *
     * @param cluster
     * @param updatedCronJob
     * @param context
     * @return
     * @throws Exception
     */
    public void patchCronJobV1beta1(ClusterVO cluster, String namespace, V1beta1CronJob currentCronJob, V1beta1CronJob updatedCronJob, boolean dryRun, ExecutingContextVO context) throws Exception {

        // 현재 CronJob 조회
        currentCronJob.setStatus(null);
        updatedCronJob.setStatus(null);

        // patchJson 으로 변경
        List<JsonObject> patchBody = k8sPatchSpecFactory.buildPatch(currentCronJob, updatedCronJob);
        log.debug("########## CronJob patchBody JSON: {}", JsonUtils.toGson(patchBody));
        // patch
        k8sWorker.patchCronJobV1beta1(cluster, namespace, updatedCronJob.getMetadata().getName(), patchBody, dryRun);

    }

    public void replaceCronJobV1beta1(ClusterVO cluster, String namespace, V1beta1CronJob updatedCronJob, ExecutingContextVO context) throws Exception {
        // patch
        k8sWorker.replaceCronJobV1beta1(cluster, namespace, updatedCronJob.getMetadata().getName(), updatedCronJob);

    }

    /**
     * CronJob V1 수정
     *
     * @param cluster
     * @param updatedCronJob
     * @param context
     * @return
     * @throws Exception
     */
    public void patchCronJobV1(ClusterVO cluster, String namespace, V1CronJob updatedCronJob, ExecutingContextVO context) throws Exception {

        // 현재 CronJob 조회
        V1CronJob currentCronJob = k8sWorker.getCronJobV1(cluster, namespace, updatedCronJob.getMetadata().getName());

        // patchJson 으로 변경
        List<JsonObject> patchBody = k8sPatchSpecFactory.buildPatch(currentCronJob, updatedCronJob);
        log.debug("########## CronJob patchBody JSON: {}", JsonUtils.toGson(patchBody));
        // patch
        k8sWorker.patchCronJobV1(cluster, namespace, updatedCronJob.getMetadata().getName(), patchBody, false);

    }

    /**
     * CronJob V1 수정
     *
     * @param cluster
     * @param updatedCronJob
     * @param context
     * @return
     * @throws Exception
     */
    public void patchCronJobV1(ClusterVO cluster, String namespace, V1CronJob currentCronJob, V1CronJob updatedCronJob, boolean dryRun, ExecutingContextVO context) throws Exception {

        // 현재 CronJob 조회
        currentCronJob.setStatus(null);
        updatedCronJob.setStatus(null);

        // patchJson 으로 변경
        List<JsonObject> patchBody = k8sPatchSpecFactory.buildPatch(currentCronJob, updatedCronJob);
        log.debug("########## CronJob patchBody JSON: {}", JsonUtils.toGson(patchBody));
        // patch
        k8sWorker.patchCronJobV1(cluster, namespace, updatedCronJob.getMetadata().getName(), patchBody, dryRun);

    }

    public void replaceCronJobV1(ClusterVO cluster, String namespace, V1CronJob updatedCronJob, ExecutingContextVO context) throws Exception {
        // patch
        k8sWorker.replaceCronJobV1(cluster, namespace, updatedCronJob.getMetadata().getName(), updatedCronJob);

    }

    /**
     * CronJob 삭제
     *
     * @param cluster
     * @param serverParam
     * @param context
     * @return
     * @throws Exception
     */
    public boolean deleteCronJob(ClusterVO cluster, ServerDetailVO serverParam, ExecutingContextVO context) throws Exception {
        String name = ResourceUtil.getUniqueName(serverParam.getComponent().getComponentName());
        log.info("Converted ServerDetailVO INFO: {}", JsonUtils.toGson(serverParam));

        return this.deleteCronJob(cluster, name, context);
    }

    public boolean deleteCronJob(ClusterVO cluster, String name, ExecutingContextVO context) throws Exception {

        K8sApiVerKindType apiVerKindType = ResourceUtil.getApiVerKindType(cluster, K8sApiKindType.CRON_JOB);

        log.debug("CronJob to delete: {}", name);

        if (apiVerKindType != null) {
            if(apiVerKindType.getGroupType() == K8sApiGroupType.BATCH && apiVerKindType.getApiType() == K8sApiType.V1BETA1){

                /**
                 * Delete CronJob
                 */
                // Get CronJob
                V1beta1CronJob v1beta1CronJob = k8sWorker.getCronJobV1beta1(cluster, cluster.getNamespaceName(), name);

                // CronJob가 존재시 Delete CronJob
                if (v1beta1CronJob != null) {

                    V1Status result = k8sWorker.deleteCronJobV1beta1(cluster, cluster.getNamespaceName(), name);

                    log.debug("### CronJob to delete status: {}", K8sJsonUtils.getJson().serialize(result));
                }
            }
            else {

                /**
                 * Delete CronJob
                 */
                // Get CronJob
                V1CronJob v1CronJob = k8sWorker.getCronJobV1(cluster, cluster.getNamespaceName(), name);

                // CronJob가 존재시 Delete CronJob
                if (v1CronJob != null) {

                    V1Status result = k8sWorker.deleteCronJobV1(cluster, cluster.getNamespaceName(), name);

                    log.debug("### CronJob to delete status: {}", K8sJsonUtils.getJson().serialize(result));
                }
            }
        } else {
            throw new CocktailException(String.format("Cube Cluster version[%s] is not support.", cluster.getK8sVersion()), ExceptionType.K8sNotSupported);
        }

        return true;
    }

    /**
     * K8S CronJob 정보 조회
     *
     * @param cluster
     * @param namespace
     * @param fieldSelector
     * @param labelSelector
     * @param context
     * @return
     * @throws Exception
     */
    public List<K8sCronJobVO> getCronJobs(ClusterVO cluster, String namespace, String fieldSelector, String labelSelector, ExecutingContextVO context) throws Exception{

        try {
            if(cluster != null && StringUtils.isNotBlank(namespace)){

                List<K8sCronJobVO> results = this.convertCronJobDataList(cluster, namespace, fieldSelector, labelSelector, context);

                if(CollectionUtils.isNotEmpty(results)){
                    return results;
                }else{
                    return new ArrayList<>();
                }
            }else{
                throw new CocktailException("cluster/namespace/name is null.", ExceptionType.InvalidParameter);
            }
        } catch (Exception e) {
            throw new CocktailException("getCronJobs fail!!", e, ExceptionType.K8sCocktailCloudInquireFail);
        }
    }

    /**
     * K8S CronJob 정보 조회
     *
     * @param clusterSeq
     * @param namespace
     * @param name
     * @param context
     * @return
     * @throws Exception
     */
    public K8sCronJobVO getCronJob(Integer clusterSeq, String namespace, String name, ExecutingContextVO context) throws Exception {
        try {
            if (clusterSeq != null) {
                IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);

                ClusterVO cluster = clusterDao.getCluster(clusterSeq);

                if (cluster != null) {
                    return this.getCronJob(cluster, namespace, name, context);
                }
                else {
                    throw new CocktailException("cluster not found.", ExceptionType.InvalidParameter);
                }
            }
            else {
                throw new CocktailException("cluster not found.", ExceptionType.InvalidParameter);
            }
        } catch (CocktailException e) {
            throw new CocktailException("getCronJob fail!!", e, ExceptionType.K8sCocktailCloudInquireFail);
        }
    }

    /**
     * K8S CronJob 정보 조회
     *
     * @param cluster
     * @param namespace
     * @param name
     * @param context
     * @return
     * @throws Exception
     */
    public K8sCronJobVO getCronJob(ClusterVO cluster, String namespace, String name, ExecutingContextVO context) throws Exception{

        try {
            if(cluster != null && StringUtils.isNotBlank(namespace) && StringUtils.isNotBlank(name)){
                String field = String.format("%s.%s=%s", KubeConstants.META, KubeConstants.NAME, name);

                List<K8sCronJobVO> results = this.getCronJobs(cluster, namespace, field, null, context);

                if(CollectionUtils.isNotEmpty(results)){
                    return results.get(0);
                }else{
                    return null;
                }
            }else{
                throw new CocktailException("cluster/namespace/name is null.", ExceptionType.InvalidParameter);
            }
        } catch (CocktailException e) {
            throw new CocktailException("getCronJob fail!!", e, ExceptionType.K8sCocktailCloudInquireFail);
        }
    }

    /**
     * K8S CronJob 정보 조회
     *
     * @param clusterSeq
     * @param namespaceName
     * @return
     * @throws Exception
     */
    public List<K8sCronJobVO> getCronJobsByCluster(Integer clusterSeq, String namespaceName, ExecutingContextVO context) throws Exception{

        try {
            if(clusterSeq != null){
                IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);

                ClusterVO cluster = clusterDao.getCluster(clusterSeq);

                if(cluster != null){
                    return this.getCronJobs(cluster, namespaceName, null, null, context);
                }else{
                    throw new CocktailException("cluster is null.", ExceptionType.InvalidParameter);
                }
            }else{
                throw new CocktailException("componentSeq is null.", ExceptionType.InvalidParameter);
            }
        } catch (Exception e) {
            throw new CocktailException("getCronJobsByCluster fail!!", e, ExceptionType.K8sCocktailCloudInquireFail);
        }
    }

    /**
     * K8S CronJob 정보 조회 후,
     *
     * - V1BETA1
     * V1beta1CronJob -> K8sCronJobVO 변환
     *
     * @param cluster
     * @param label
     * @return
     * @throws Exception
     */
    public List<K8sCronJobVO> convertCronJobDataList(ClusterVO cluster, String namespace, String field, String label, ExecutingContextVO context) throws Exception {

        List<K8sCronJobVO> cronJobs = new ArrayList<>();

        if (cluster != null) {
            K8sApiVerKindType apiVerKindType = ResourceUtil.getApiVerKindType(cluster, K8sApiKindType.CRON_JOB);

            if (apiVerKindType != null) {
                K8sCronJobVO cronJob = null;
                Integer activeJobCnt = 0;

                // joda.datetime Serialization
                JSON k8sJson = new JSON();

                if (apiVerKindType.getGroupType() == K8sApiGroupType.BATCH && apiVerKindType.getApiType() == K8sApiType.V1BETA1) {
                    // CronJob 조회
                    List<V1beta1CronJob> v1beta1CronJobs = k8sWorker.getCronJobsV1beta1(cluster, namespace, field, label);

                    if (CollectionUtils.isNotEmpty(v1beta1CronJobs)) {

                        for (V1beta1CronJob cronJobRow : v1beta1CronJobs) {
                            cronJob = new K8sCronJobVO();
                            activeJobCnt = CollectionUtils.size(cronJobRow.getStatus().getActive());

                            // CronJob 목록
                            cronJob.setLabel(label);
                            cronJob.setNamespace(cronJobRow.getMetadata().getNamespace());
                            cronJob.setName(cronJobRow.getMetadata().getName());
                            cronJob.setLabels(cronJobRow.getMetadata().getLabels());
                            cronJob.setSchedule(cronJobRow.getSpec().getSchedule());
                            cronJob.setSuspend(cronJobRow.getSpec().getSuspend());
                            cronJob.setActiveJobCnt(activeJobCnt);
                            cronJob.setLastScheduleTime(cronJobRow.getStatus().getLastScheduleTime());
                            cronJob.setCreationTimestamp(cronJobRow.getMetadata().getCreationTimestamp());
                            cronJob.setImages(cronJobRow.getSpec().getJobTemplate().getSpec().getTemplate().getSpec().getContainers().stream().map(c -> c.getImage()).collect(Collectors.toList()));
                            cronJob.setDeployment(k8sJson.serialize(cronJobRow));
                            cronJob.setDeploymentYaml(Yaml.getSnakeYaml().dumpAsMap(cronJobRow));

                            // CronJob Detail
                            K8sCronJobDetailVO detail = new K8sCronJobDetailVO();
                            detail.setName(cronJobRow.getMetadata().getName());
                            detail.setNamespace(cronJobRow.getMetadata().getNamespace());
                            detail.setLabels(cronJobRow.getMetadata().getLabels());
                            detail.setAnnotations(cronJobRow.getMetadata().getAnnotations());
                            detail.setCreationTime(cronJobRow.getMetadata().getCreationTimestamp());
                            detail.setConcurrencyPolicy(cronJobRow.getSpec().getConcurrencyPolicy());
                            detail.setSchedule(cronJobRow.getSpec().getSchedule());
                            detail.setStartingDeadlineSeconds(cronJobRow.getSpec().getStartingDeadlineSeconds());
                            detail.setSuccessfulJobsHistoryLimit(cronJobRow.getSpec().getSuccessfulJobsHistoryLimit());
                            detail.setFailedJobsHistoryLimit(cronJobRow.getSpec().getFailedJobsHistoryLimit());
                            detail.setSuspend(cronJobRow.getSpec().getSuspend());
                            detail.setActiveDeadlineSeconds(cronJobRow.getSpec().getJobTemplate().getSpec().getActiveDeadlineSeconds());
                            detail.setBackoffLimit(cronJobRow.getSpec().getJobTemplate().getSpec().getBackoffLimit());
                            detail.setCompletions(cronJobRow.getSpec().getJobTemplate().getSpec().getCompletions());
                            detail.setParallelism(cronJobRow.getSpec().getJobTemplate().getSpec().getParallelism());
                            detail.setTtlSecondsAfterFinished(cronJobRow.getSpec().getJobTemplate().getSpec().getTtlSecondsAfterFinished());
                            detail.setLastScheduleTime(cronJobRow.getStatus().getLastScheduleTime());
                            detail.setActiveJobs(Lists.newArrayList());
                            if (CollectionUtils.isNotEmpty(cronJobRow.getStatus().getActive())) {
                                for (V1ObjectReference orRow : cronJobRow.getStatus().getActive()) {
                                    detail.getActiveJobs().addAll(this.convertJobDataList(cluster, orRow.getNamespace(), String.format("%s.%s=%s", KubeConstants.META, KubeConstants.NAME, orRow.getName()), null, context));
                                }
                            }
                            // podTemplate
                            detail.setPodTemplate(this.setPodTemplateSpec(cronJobRow.getSpec().getJobTemplate().getSpec().getTemplate(), k8sJson));
                            cronJob.setDetail(detail);


                            cronJobs.add(cronJob);
                        }
                    }
                } else {
                    // CronJob 조회
                    List<V1CronJob> v1CronJobs = k8sWorker.getCronJobsV1(cluster, namespace, field, label);

                    if (CollectionUtils.isNotEmpty(v1CronJobs)) {

                        for (V1CronJob cronJobRow : v1CronJobs) {
                            cronJob = new K8sCronJobVO();
                            activeJobCnt = CollectionUtils.size(cronJobRow.getStatus().getActive());

                            // CronJob 목록
                            cronJob.setLabel(label);
                            cronJob.setNamespace(cronJobRow.getMetadata().getNamespace());
                            cronJob.setName(cronJobRow.getMetadata().getName());
                            cronJob.setLabels(cronJobRow.getMetadata().getLabels());
                            cronJob.setSchedule(cronJobRow.getSpec().getSchedule());
                            cronJob.setSuspend(cronJobRow.getSpec().getSuspend());
                            cronJob.setActiveJobCnt(activeJobCnt);
                            cronJob.setLastScheduleTime(cronJobRow.getStatus().getLastScheduleTime());
                            cronJob.setCreationTimestamp(cronJobRow.getMetadata().getCreationTimestamp());
                            cronJob.setImages(cronJobRow.getSpec().getJobTemplate().getSpec().getTemplate().getSpec().getContainers().stream().map(c -> c.getImage()).collect(Collectors.toList()));
                            cronJob.setDeployment(k8sJson.serialize(cronJobRow));
                            cronJob.setDeploymentYaml(Yaml.getSnakeYaml().dumpAsMap(cronJobRow));

                            // CronJob Detail
                            K8sCronJobDetailVO detail = new K8sCronJobDetailVO();
                            detail.setName(cronJobRow.getMetadata().getName());
                            detail.setNamespace(cronJobRow.getMetadata().getNamespace());
                            detail.setLabels(cronJobRow.getMetadata().getLabels());
                            detail.setAnnotations(cronJobRow.getMetadata().getAnnotations());
                            detail.setCreationTime(cronJobRow.getMetadata().getCreationTimestamp());
                            detail.setConcurrencyPolicy(cronJobRow.getSpec().getConcurrencyPolicy());
                            detail.setSchedule(cronJobRow.getSpec().getSchedule());
                            detail.setStartingDeadlineSeconds(cronJobRow.getSpec().getStartingDeadlineSeconds());
                            detail.setSuccessfulJobsHistoryLimit(cronJobRow.getSpec().getSuccessfulJobsHistoryLimit());
                            detail.setFailedJobsHistoryLimit(cronJobRow.getSpec().getFailedJobsHistoryLimit());
                            detail.setSuspend(cronJobRow.getSpec().getSuspend());
                            detail.setActiveDeadlineSeconds(cronJobRow.getSpec().getJobTemplate().getSpec().getActiveDeadlineSeconds());
                            detail.setBackoffLimit(cronJobRow.getSpec().getJobTemplate().getSpec().getBackoffLimit());
                            detail.setCompletions(cronJobRow.getSpec().getJobTemplate().getSpec().getCompletions());
                            detail.setParallelism(cronJobRow.getSpec().getJobTemplate().getSpec().getParallelism());
                            detail.setTtlSecondsAfterFinished(cronJobRow.getSpec().getJobTemplate().getSpec().getTtlSecondsAfterFinished());
                            detail.setLastScheduleTime(cronJobRow.getStatus().getLastScheduleTime());
                            detail.setActiveJobs(Lists.newArrayList());
                            if (CollectionUtils.isNotEmpty(cronJobRow.getStatus().getActive())) {
                                for (V1ObjectReference orRow : cronJobRow.getStatus().getActive()) {
                                    detail.getActiveJobs().addAll(this.convertJobDataList(cluster, orRow.getNamespace(), String.format("%s.%s=%s", KubeConstants.META, KubeConstants.NAME, orRow.getName()), null, context));
                                }
                            }
                            // podTemplate
                            detail.setPodTemplate(this.setPodTemplateSpec(cronJobRow.getSpec().getJobTemplate().getSpec().getTemplate(), k8sJson));
                            cronJob.setDetail(detail);


                            cronJobs.add(cronJob);
                        }
                    }
                }
            } else {
                log.warn("convertCronJobDataList cluster {} version {} not supported.", cluster.getClusterName(), cluster.getK8sVersion());
            }
        } else {
            log.warn("convertCronJobDataList - cluster is null.");
        }


        return cronJobs;
    }

    /**
     * K8S Horizontal Pod Autoscalers 정보 조회 후 V1HorizontalPodAutoscaler -> K8sHorizontalPodAutoscalerVO 변환
     *
     * @param clusterSeq
     * @param namespace
     * @param field
     * @param label
     * @return
     * @throws Exception
     */
    public List<K8sHorizontalPodAutoscalerVO> convertHorizontalPodAutoscalerDataList(Integer clusterSeq, String namespace, String field, String label) throws Exception {
        try {
            if (clusterSeq != null) {
                IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
                ClusterVO cluster = clusterDao.getCluster(clusterSeq);
                if (cluster != null) {
                    return this.convertHorizontalPodAutoscalerDataList(cluster, namespace, field, label);
                }
                else {
                    throw new CocktailException("cluster not found.", ExceptionType.InvalidParameter);
                }
            }
            else {
                throw new CocktailException("cluster not found.", ExceptionType.InvalidParameter);
            }
        } catch (CocktailException e) {
            throw new CocktailException("convertHorizontalPodAutoscalerDataList fail!!", e, ExceptionType.K8sCocktailCloudInquireFail);
        }
    }

    /**
     * K8S Horizontal Pod Autoscalers 정보 조회 후 V1HorizontalPodAutoscaler -> K8sHorizontalPodAutoscalerVO 변환
     *
     * @param cluster
     * @param namespace
     * @param field
     * @param label
     * @return
     * @throws Exception
     */
    public List<K8sHorizontalPodAutoscalerVO> convertHorizontalPodAutoscalerDataList(ClusterVO cluster, String namespace, String field, String label) throws Exception {

        List<K8sHorizontalPodAutoscalerVO> hpas = new ArrayList<>();

        try {
            this.genHorizontalPodAutoscalerData(hpas, null, cluster, namespace, field, label);
        } catch (Exception e) {
            throw new CocktailException("convertHorizontalPodAutoscalerDataList fail!!", e, ExceptionType.K8sCocktailCloudInquireFail);
        }

        return hpas;
    }

    /**
     * K8S Horizontal Pod Autoscalers 정보 조회 후 V1HorizontalPodAutoscaler -> K8sHorizontalPodAutoscalerVO 변환
     *
     * @param cluster
     * @param label
     * @return
     * @throws Exception
     */
    public Map<String, K8sHorizontalPodAutoscalerVO> convertHorizontalPodAutoscalerDataMap(ClusterVO cluster, String namespace, String field, String label) throws Exception {

        Map<String, K8sHorizontalPodAutoscalerVO> hpaMap = new HashMap<>();

        try {
            this.genHorizontalPodAutoscalerData(null, hpaMap, cluster, namespace, field, label);
        } catch (Exception e) {
            if(log.isDebugEnabled()) log.debug("trace log ", e);
            throw new CocktailException("convertHorizontalPodAutoscalerDataMap fail!!", e, ExceptionType.K8sCocktailCloudInquireFail);
        }

        return hpaMap;
    }

    private void genHorizontalPodAutoscalerData(List<K8sHorizontalPodAutoscalerVO> hpas, Map<String, K8sHorizontalPodAutoscalerVO> hpaMap, ClusterVO cluster, String namespace, String field, String label) throws Exception{

        // joda.datetime Serialization
        JSON k8sJson = new JSON();

        ObjectMapper mapper = K8sMapperUtils.getMapper();

        if (cluster != null) {
            K8sApiVerKindType apiVerKindType = ResourceUtil.getApiVerKindType(cluster, K8sApiKindType.HORIZONTAL_POD_AUTOSCALER);

            if (apiVerKindType != null) {
                if(apiVerKindType.getGroupType() == K8sApiGroupType.AUTOSCALING && apiVerKindType.getApiType() == K8sApiType.V1){
                    List<V1HorizontalPodAutoscaler> v1HorizontalPodAutoscalers = k8sWorker.getHorizontalPodAutoscalersV1(cluster, namespace, field, label);

                    if(CollectionUtils.isNotEmpty(v1HorizontalPodAutoscalers)){

                        K8sHorizontalPodAutoscalerVO horizontalPodAutoscaler = null;

                        for(V1HorizontalPodAutoscaler v1HorizontalPodAutoscalerRow : v1HorizontalPodAutoscalers){
                            horizontalPodAutoscaler = new K8sHorizontalPodAutoscalerVO();

                            // Horizontal Pod Autoscalers 목록
                            horizontalPodAutoscaler.setLabel(label);
                            horizontalPodAutoscaler.setNamespace(v1HorizontalPodAutoscalerRow.getMetadata().getNamespace());
                            horizontalPodAutoscaler.setName(v1HorizontalPodAutoscalerRow.getMetadata().getName());
                            horizontalPodAutoscaler.setTargetCPUUtilization(v1HorizontalPodAutoscalerRow.getSpec().getTargetCPUUtilizationPercentage());
                            horizontalPodAutoscaler.setCurrentCPUUtilization(v1HorizontalPodAutoscalerRow.getStatus().getCurrentCPUUtilizationPercentage());
                            horizontalPodAutoscaler.setMinReplicas(v1HorizontalPodAutoscalerRow.getSpec().getMinReplicas());
                            horizontalPodAutoscaler.setMaxReplicas(v1HorizontalPodAutoscalerRow.getSpec().getMaxReplicas());
                            horizontalPodAutoscaler.setStartTime(v1HorizontalPodAutoscalerRow.getMetadata().getCreationTimestamp());
                            horizontalPodAutoscaler.setDeployment(k8sJson.serialize(v1HorizontalPodAutoscalerRow));
                            horizontalPodAutoscaler.setDeploymentYaml(Yaml.getSnakeYaml().dumpAsMap(v1HorizontalPodAutoscalerRow));

                            // Horizontal Pod Autoscalers Detail
                            K8sHorizontalPodAutoscalerDetailVO horizontalPodAutoscalerDetail = new K8sHorizontalPodAutoscalerDetailVO();
                            horizontalPodAutoscalerDetail.setName(v1HorizontalPodAutoscalerRow.getMetadata().getName());
                            horizontalPodAutoscalerDetail.setNamespace(v1HorizontalPodAutoscalerRow.getMetadata().getNamespace());
                            horizontalPodAutoscalerDetail.setLabels(v1HorizontalPodAutoscalerRow.getMetadata().getLabels());
                            horizontalPodAutoscalerDetail.setAnnotations(v1HorizontalPodAutoscalerRow.getMetadata().getAnnotations());
                            horizontalPodAutoscalerDetail.setCreationTime(v1HorizontalPodAutoscalerRow.getMetadata().getCreationTimestamp());
                            horizontalPodAutoscalerDetail.setTarget(String.format("%s: %s", v1HorizontalPodAutoscalerRow.getSpec().getScaleTargetRef().getKind(), v1HorizontalPodAutoscalerRow.getSpec().getScaleTargetRef().getName()));
                            horizontalPodAutoscalerDetail.setMinReplicas(v1HorizontalPodAutoscalerRow.getSpec().getMinReplicas());
                            horizontalPodAutoscalerDetail.setMaxReplicas(v1HorizontalPodAutoscalerRow.getSpec().getMaxReplicas());
                            horizontalPodAutoscalerDetail.setTargetCPUUtilization(v1HorizontalPodAutoscalerRow.getSpec().getTargetCPUUtilizationPercentage());
                            horizontalPodAutoscalerDetail.setCurrentReplicas(v1HorizontalPodAutoscalerRow.getStatus().getCurrentReplicas());
                            horizontalPodAutoscalerDetail.setDesiredReplicas(v1HorizontalPodAutoscalerRow.getStatus().getDesiredReplicas());
                            horizontalPodAutoscalerDetail.setCurrentCPUUtilization(v1HorizontalPodAutoscalerRow.getStatus().getCurrentCPUUtilizationPercentage());
                            horizontalPodAutoscalerDetail.setLastScaleTime(v1HorizontalPodAutoscalerRow.getStatus().getLastScaleTime());
                            horizontalPodAutoscaler.setDetail(horizontalPodAutoscalerDetail);

                            // Horizontal Pod Autoscalers Target Set
                            String scaleTargetRefJson = JsonUtils.toGson(v1HorizontalPodAutoscalerRow.getSpec().getScaleTargetRef());
                            K8sCrossVersionObjectReferenceVO scaleTargetRef = JsonUtils.fromGson(scaleTargetRefJson, K8sCrossVersionObjectReferenceVO.class);
                            horizontalPodAutoscaler.setScaleTargetRef(scaleTargetRef);

                            if(hpas != null){
                                hpas.add(horizontalPodAutoscaler);
                            }
                            if(hpaMap != null){
                                hpaMap.put(v1HorizontalPodAutoscalerRow.getMetadata().getName(), horizontalPodAutoscaler);
                            }
                        }
                    }
                }
                else if(apiVerKindType.getGroupType() == K8sApiGroupType.AUTOSCALING && apiVerKindType.getApiType() == K8sApiType.V2BETA1){
                    List<V2beta1HorizontalPodAutoscaler> v2beta1HorizontalPodAutoscalers = k8sWorker.getHorizontalPodAutoscalersV2beta1(cluster, namespace, field, label);

                    if(CollectionUtils.isNotEmpty(v2beta1HorizontalPodAutoscalers)){

                        K8sHorizontalPodAutoscalerVO horizontalPodAutoscaler = null;

                        for(V2beta1HorizontalPodAutoscaler v2beta1HorizontalPodAutoscalerRow : v2beta1HorizontalPodAutoscalers){
                            horizontalPodAutoscaler = new K8sHorizontalPodAutoscalerVO();

                            // Horizontal Pod Autoscalers 목록
                            horizontalPodAutoscaler.setLabel(label);
                            horizontalPodAutoscaler.setNamespace(v2beta1HorizontalPodAutoscalerRow.getMetadata().getNamespace());
                            horizontalPodAutoscaler.setName(v2beta1HorizontalPodAutoscalerRow.getMetadata().getName());
                            horizontalPodAutoscaler.setMinReplicas(v2beta1HorizontalPodAutoscalerRow.getSpec().getMinReplicas());
                            horizontalPodAutoscaler.setMaxReplicas(v2beta1HorizontalPodAutoscalerRow.getSpec().getMaxReplicas());
                            List<K8sHorizontalPodAutoscalerMetricVO> metrics = new ArrayList<>();
                            if(CollectionUtils.isNotEmpty(v2beta1HorizontalPodAutoscalerRow.getSpec().getMetrics())){
                                List<V2beta1MetricStatus> currentMetrics = new ArrayList<>();
                                if(v2beta1HorizontalPodAutoscalerRow.getStatus() != null && CollectionUtils.isNotEmpty(v2beta1HorizontalPodAutoscalerRow.getStatus().getCurrentMetrics())){
                                    currentMetrics.addAll(v2beta1HorizontalPodAutoscalerRow.getStatus().getCurrentMetrics());
                                }
                                for (V2beta1MetricSpec metricSpecRow : v2beta1HorizontalPodAutoscalerRow.getSpec().getMetrics()){
                                    if(StringUtils.equalsIgnoreCase(MetricType.Names.Resource, metricSpecRow.getType())){
                                        K8sHorizontalPodAutoscalerMetricResourceVO metricResource = new K8sHorizontalPodAutoscalerMetricResourceVO();
                                        metricResource.setMetricType(metricSpecRow.getType());
                                        metricResource.setName(metricSpecRow.getResource().getName());
                                        if (metricSpecRow.getResource().getTargetAverageUtilization() != null) {
                                            metricResource.setTargetType(MetricTargetType.Utilization.getCode());
                                        }
                                        metricResource.setTargetAverageUtilization(metricSpecRow.getResource().getTargetAverageUtilization());
                                        if (metricSpecRow.getResource().getTargetAverageValue() != null) {
                                            metricResource.setTargetType(MetricTargetType.AverageValue.getCode());
                                        }
                                        metricResource.setTargetAverageValue(metricSpecRow.getResource().getTargetAverageValue() != null ? metricSpecRow.getResource().getTargetAverageValue().toSuffixedString() : null);
                                        for(V2beta1MetricStatus currentMetricRow : currentMetrics){
                                            if(StringUtils.equalsIgnoreCase(currentMetricRow.getType(), MetricType.Names.Resource)
                                                    && StringUtils.equalsIgnoreCase(currentMetricRow.getResource().getName(), metricSpecRow.getResource().getName())){
                                                metricResource.setCurrentAverageUtilization(currentMetricRow.getResource().getCurrentAverageUtilization());
                                                metricResource.setCurrentAverageValue(StringUtils.removeEnd(currentMetricRow.getResource().getCurrentAverageValue().toSuffixedString(), "%"));
                                                break;
                                            }
                                        }
                                        metrics.add(metricResource);
                                    }else if(StringUtils.equalsIgnoreCase(MetricType.Names.Pods, metricSpecRow.getType())){
                                        K8sHorizontalPodAutoscalerMetricPodsVO metricPods = new K8sHorizontalPodAutoscalerMetricPodsVO();
                                        metricPods.setMetricType(metricSpecRow.getType());
                                        metricPods.setMetricName(metricSpecRow.getPods().getMetricName());
                                        if (metricSpecRow.getPods().getTargetAverageValue() != null) {
                                            metricPods.setTargetType(MetricTargetType.AverageValue.getCode());
                                        }
                                        metricPods.setTargetAverageValue(String.valueOf(metricSpecRow.getPods().getTargetAverageValue().getNumber().intValue()));
                                        for(V2beta1MetricStatus currentMetricRow : currentMetrics){
                                            if(StringUtils.equalsIgnoreCase(currentMetricRow.getType(), MetricType.Names.Pods)
                                                    && StringUtils.equalsIgnoreCase(currentMetricRow.getPods().getMetricName(), metricSpecRow.getPods().getMetricName())){
                                                metricPods.setCurrentAverageValue(String.valueOf(currentMetricRow.getPods().getCurrentAverageValue().getNumber().intValue()));
                                                break;
                                            }
                                        }
                                        metrics.add(metricPods);
                                    }else if(StringUtils.equalsIgnoreCase(MetricType.Names.Object, metricSpecRow.getType())){
                                        K8sHorizontalPodAutoscalerMetricObjectVO metricObject = new K8sHorizontalPodAutoscalerMetricObjectVO();
                                        metricObject.setMetricType(metricSpecRow.getType());
                                        metricObject.setMetricName(metricSpecRow.getObject().getMetricName());
                                        metricObject.setDescribedObject(mapper.readValue(k8sJson.serialize(metricSpecRow.getObject().getTarget()), new TypeReference<Map<String, String>>(){}));
                                        if (metricSpecRow.getObject().getTargetValue() != null) {
                                            metricObject.setTargetType(MetricTargetType.Value.getCode());
                                        }
                                        metricObject.setTargetValue(metricSpecRow.getObject().getTargetValue().toSuffixedString());
                                        for(V2beta1MetricStatus currentMetricRow : currentMetrics){
                                            if(StringUtils.equalsIgnoreCase(currentMetricRow.getType(), MetricType.Names.Object)
                                                    && StringUtils.equalsIgnoreCase(currentMetricRow.getPods().getMetricName(), metricSpecRow.getPods().getMetricName())){
                                                metricObject.setCurrentValue(currentMetricRow.getObject().getCurrentValue().toSuffixedString());
                                                break;
                                            }
                                        }
                                        metrics.add(metricObject);
                                    }
                                }
                            }
                            horizontalPodAutoscaler.setMetrics(metrics);
                            horizontalPodAutoscaler.setStartTime(v2beta1HorizontalPodAutoscalerRow.getMetadata().getCreationTimestamp());
                            horizontalPodAutoscaler.setDeployment(k8sJson.serialize(v2beta1HorizontalPodAutoscalerRow));
                            horizontalPodAutoscaler.setDeploymentYaml(Yaml.getSnakeYaml().dumpAsMap(v2beta1HorizontalPodAutoscalerRow));

                            // Horizontal Pod Autoscalers Detail
                            K8sHorizontalPodAutoscalerDetailVO horizontalPodAutoscalerDetail = new K8sHorizontalPodAutoscalerDetailVO();
                            horizontalPodAutoscalerDetail.setName(v2beta1HorizontalPodAutoscalerRow.getMetadata().getName());
                            horizontalPodAutoscalerDetail.setNamespace(v2beta1HorizontalPodAutoscalerRow.getMetadata().getNamespace());
                            horizontalPodAutoscalerDetail.setLabels(v2beta1HorizontalPodAutoscalerRow.getMetadata().getLabels());
                            horizontalPodAutoscalerDetail.setAnnotations(v2beta1HorizontalPodAutoscalerRow.getMetadata().getAnnotations());
                            horizontalPodAutoscalerDetail.setCreationTime(v2beta1HorizontalPodAutoscalerRow.getMetadata().getCreationTimestamp());
                            horizontalPodAutoscalerDetail.setTarget(String.format("%s: %s", v2beta1HorizontalPodAutoscalerRow.getSpec().getScaleTargetRef().getKind(), v2beta1HorizontalPodAutoscalerRow.getSpec().getScaleTargetRef().getName()));
                            horizontalPodAutoscalerDetail.setMinReplicas(v2beta1HorizontalPodAutoscalerRow.getSpec().getMinReplicas());
                            horizontalPodAutoscalerDetail.setMaxReplicas(v2beta1HorizontalPodAutoscalerRow.getSpec().getMaxReplicas());
                            horizontalPodAutoscalerDetail.setCurrentReplicas(v2beta1HorizontalPodAutoscalerRow.getStatus().getCurrentReplicas());
                            horizontalPodAutoscalerDetail.setDesiredReplicas(v2beta1HorizontalPodAutoscalerRow.getStatus().getDesiredReplicas());
                            horizontalPodAutoscalerDetail.setLastScaleTime(v2beta1HorizontalPodAutoscalerRow.getStatus().getLastScaleTime());
                            horizontalPodAutoscalerDetail.setMetrics(metrics);
                            horizontalPodAutoscaler.setDetail(horizontalPodAutoscalerDetail);

                            // Horizontal Pod Autoscalers Target Set
                            String scaleTargetRefJson = JsonUtils.toGson(v2beta1HorizontalPodAutoscalerRow.getSpec().getScaleTargetRef());
                            K8sCrossVersionObjectReferenceVO scaleTargetRef = JsonUtils.fromGson(scaleTargetRefJson, K8sCrossVersionObjectReferenceVO.class);
                            horizontalPodAutoscaler.setScaleTargetRef(scaleTargetRef);

                            // Horizontal Pod Autoscalers Condition
                            List<K8sHorizontalPodAutoscalerConditionVO> conditions = new ArrayList<>();
                            if(CollectionUtils.isNotEmpty(v2beta1HorizontalPodAutoscalerRow.getStatus().getConditions())){
                                for(V2beta1HorizontalPodAutoscalerCondition hpaConditionRow : v2beta1HorizontalPodAutoscalerRow.getStatus().getConditions()){
                                    K8sHorizontalPodAutoscalerConditionVO conditionTemp = new K8sHorizontalPodAutoscalerConditionVO();
                                    BeanUtils.copyProperties(conditionTemp, hpaConditionRow);
                                    if(hpaConditionRow.getLastTransitionTime() != null){
                                        conditionTemp.setLastTransitionTime(hpaConditionRow.getLastTransitionTime());
                                    }

                                    conditions.add(conditionTemp);
                                }
                            }
                            horizontalPodAutoscaler.setConditions(conditions);

                            if(hpas != null){
                                hpas.add(horizontalPodAutoscaler);
                            }
                            if(hpaMap != null){
                                hpaMap.put(v2beta1HorizontalPodAutoscalerRow.getMetadata().getName(), horizontalPodAutoscaler);
                            }
                        }
                    }
                }
                else if(apiVerKindType.getGroupType() == K8sApiGroupType.AUTOSCALING && apiVerKindType.getApiType() == K8sApiType.V2BETA2){

                    List<V2beta2HorizontalPodAutoscaler> v2beta2HorizontalPodAutoscalers = k8sWorker.getHorizontalPodAutoscalersV2beta2(cluster, namespace, field, label);

                    if(CollectionUtils.isNotEmpty(v2beta2HorizontalPodAutoscalers)){

                        QuantityFormatter quantityFormatter = new QuantityFormatter();
                        K8sHorizontalPodAutoscalerVO horizontalPodAutoscaler = null;

                        for(V2beta2HorizontalPodAutoscaler v2beta2HorizontalPodAutoscalerRow : v2beta2HorizontalPodAutoscalers){
                            horizontalPodAutoscaler = new K8sHorizontalPodAutoscalerVO();

                            // Horizontal Pod Autoscalers 목록
                            horizontalPodAutoscaler.setLabel(label);
                            horizontalPodAutoscaler.setNamespace(v2beta2HorizontalPodAutoscalerRow.getMetadata().getNamespace());
                            horizontalPodAutoscaler.setName(v2beta2HorizontalPodAutoscalerRow.getMetadata().getName());
                            horizontalPodAutoscaler.setMinReplicas(v2beta2HorizontalPodAutoscalerRow.getSpec().getMinReplicas());
                            horizontalPodAutoscaler.setMaxReplicas(v2beta2HorizontalPodAutoscalerRow.getSpec().getMaxReplicas());
                            List<K8sHorizontalPodAutoscalerMetricVO> metrics = new ArrayList<>();
                            if(CollectionUtils.isNotEmpty(v2beta2HorizontalPodAutoscalerRow.getSpec().getMetrics())){
                                List<V2beta2MetricStatus> currentMetrics = new ArrayList<>();
                                if(v2beta2HorizontalPodAutoscalerRow.getStatus() != null && CollectionUtils.isNotEmpty(v2beta2HorizontalPodAutoscalerRow.getStatus().getCurrentMetrics())){
                                    currentMetrics.addAll(v2beta2HorizontalPodAutoscalerRow.getStatus().getCurrentMetrics());
                                }
                                for (V2beta2MetricSpec metricSpecRow : v2beta2HorizontalPodAutoscalerRow.getSpec().getMetrics()){
                                    if(StringUtils.equalsIgnoreCase(MetricType.Names.Resource, metricSpecRow.getType())){
                                        K8sHorizontalPodAutoscalerMetricResourceVO metricResource = new K8sHorizontalPodAutoscalerMetricResourceVO();
                                        metricResource.setMetricType(metricSpecRow.getType());
                                        metricResource.setName(metricSpecRow.getResource().getName());
                                        metricResource.setTargetType(metricSpecRow.getResource().getTarget().getType());

                                        // set target value
                                        if (StringUtils.equalsIgnoreCase(metricSpecRow.getResource().getTarget().getType(), MetricTargetType.AverageValue.getCode())) {
                                            metricResource.setTargetAverageValue(metricSpecRow.getResource().getTarget().getAverageValue() != null ? metricSpecRow.getResource().getTarget().getAverageValue().toSuffixedString() : null);
                                        } else if (StringUtils.equalsIgnoreCase(metricSpecRow.getResource().getTarget().getType(), MetricTargetType.Value.getCode())) {
                                            metricResource.setTargetValue(metricSpecRow.getResource().getTarget().getValue() != null ? metricSpecRow.getResource().getTarget().getValue().toSuffixedString() : null);
                                        } else {
                                            metricResource.setTargetAverageUtilization(metricSpecRow.getResource().getTarget().getAverageUtilization());
                                        }

                                        // set current value
                                        for(V2beta2MetricStatus currentMetricRow : currentMetrics){
                                            if(StringUtils.equalsIgnoreCase(currentMetricRow.getType(), MetricType.Names.Resource)
                                                    && StringUtils.equalsIgnoreCase(currentMetricRow.getResource().getName(), metricSpecRow.getResource().getName())){
                                                metricResource.setCurrentAverageUtilization(Optional.ofNullable(currentMetricRow.getResource()).map(V2beta2ResourceMetricStatus::getCurrent).map(V2beta2MetricValueStatus::getAverageUtilization).orElseGet(() ->0));
                                                metricResource.setCurrentAverageValue(Optional.ofNullable(currentMetricRow.getResource()).map(V2beta2ResourceMetricStatus::getCurrent).map(V2beta2MetricValueStatus::getAverageValue).orElseGet(() ->new Quantity("0")).toSuffixedString());
                                                metricResource.setCurrentValue(Optional.ofNullable(currentMetricRow.getResource()).map(V2beta2ResourceMetricStatus::getCurrent).map(V2beta2MetricValueStatus::getValue).orElseGet(() ->new Quantity("0")).toSuffixedString());
                                                break;
                                            }
                                        }
                                        metrics.add(metricResource);
                                    }else if(StringUtils.equalsIgnoreCase(MetricType.Names.Pods, metricSpecRow.getType())){
                                        K8sHorizontalPodAutoscalerMetricPodsVO metricPods = new K8sHorizontalPodAutoscalerMetricPodsVO();
                                        metricPods.setMetricType(metricSpecRow.getType());
                                        metricPods.setMetricName(metricSpecRow.getPods().getMetric().getName());
                                        metricPods.setTargetType(metricSpecRow.getPods().getTarget().getType());

                                        String selectorJson = k8sJson.serialize(metricSpecRow.getPods().getMetric().getSelector());
                                        metricPods.setSelector(k8sJson.getGson().fromJson(selectorJson, K8sLabelSelectorVO.class));

                                        // set target value
                                        if (StringUtils.equalsIgnoreCase(metricSpecRow.getPods().getTarget().getType(), MetricTargetType.AverageValue.getCode())) {
                                            metricPods.setTargetAverageValue(metricSpecRow.getPods().getTarget().getAverageValue() != null ? metricSpecRow.getPods().getTarget().getAverageValue().toSuffixedString() : null);
                                        } else if (StringUtils.equalsIgnoreCase(metricSpecRow.getPods().getTarget().getType(), MetricTargetType.Value.getCode())) {
                                            metricPods.setTargetValue(metricSpecRow.getPods().getTarget().getValue() != null ? metricSpecRow.getPods().getTarget().getValue().toSuffixedString() : null);
                                        } else {
                                            metricPods.setTargetAverageUtilization(metricSpecRow.getPods().getTarget().getAverageUtilization());
                                        }

                                        // set current value
                                        for(V2beta2MetricStatus currentMetricRow : currentMetrics){
                                            if(StringUtils.equalsIgnoreCase(currentMetricRow.getType(), MetricType.Names.Pods)
                                                    && StringUtils.equalsIgnoreCase(currentMetricRow.getPods().getMetric().getName(), metricSpecRow.getPods().getMetric().getName())){
                                                metricPods.setCurrentAverageUtilization(currentMetricRow.getPods().getCurrent().getAverageUtilization());
                                                metricPods.setCurrentAverageValue(currentMetricRow.getPods().getCurrent().getAverageValue().toSuffixedString());
                                                metricPods.setCurrentValue(currentMetricRow.getPods().getCurrent().getValue().toSuffixedString());
                                                break;
                                            }
                                        }
                                        metrics.add(metricPods);
                                    }else if(StringUtils.equalsIgnoreCase(MetricType.Names.Object, metricSpecRow.getType())){
                                        K8sHorizontalPodAutoscalerMetricObjectVO metricObject = new K8sHorizontalPodAutoscalerMetricObjectVO();
                                        metricObject.setMetricType(metricSpecRow.getType());
                                        metricObject.setMetricName(metricSpecRow.getObject().getMetric().getName());
                                        metricObject.setTargetType(metricSpecRow.getObject().getTarget().getType());

                                        // spec.object.metric.selector
                                        String selectorJson = k8sJson.serialize(metricSpecRow.getObject().getMetric().getSelector());
                                        metricObject.setSelector(k8sJson.getGson().fromJson(selectorJson, K8sLabelSelectorVO.class));

                                        // spec.object.describedObject
                                        metricObject.setDescribedObject(mapper.readValue(k8sJson.serialize(metricSpecRow.getObject().getDescribedObject()), new TypeReference<Map<String, String>>(){}));

                                        // set target value
                                        if (StringUtils.equalsIgnoreCase(metricSpecRow.getObject().getTarget().getType(), MetricTargetType.AverageValue.getCode())) {
                                            metricObject.setTargetAverageValue(metricSpecRow.getObject().getTarget().getAverageValue() != null ? metricSpecRow.getObject().getTarget().getAverageValue().toSuffixedString() : null);
                                        } else if (StringUtils.equalsIgnoreCase(metricSpecRow.getObject().getTarget().getType(), MetricTargetType.Value.getCode())) {
                                            metricObject.setTargetValue(metricSpecRow.getObject().getTarget().getValue() != null ? metricSpecRow.getObject().getTarget().getValue().toSuffixedString() : null);
                                        } else {
                                            metricObject.setTargetAverageUtilization(metricSpecRow.getObject().getTarget().getAverageUtilization());
                                        }

                                        // set current value
                                        for(V2beta2MetricStatus currentMetricRow : currentMetrics){
                                            if(StringUtils.equalsIgnoreCase(currentMetricRow.getType(), MetricType.Names.Object)
                                                    && StringUtils.equalsIgnoreCase(currentMetricRow.getObject().getMetric().getName(), metricSpecRow.getObject().getMetric().getName())){
                                                metricObject.setCurrentAverageUtilization(currentMetricRow.getObject().getCurrent().getAverageUtilization());
                                                metricObject.setCurrentAverageValue(currentMetricRow.getObject().getCurrent().getAverageValue().toSuffixedString());
                                                metricObject.setCurrentValue(currentMetricRow.getObject().getCurrent().getValue().toSuffixedString());
                                                break;
                                            }
                                        }
                                        metrics.add(metricObject);
                                    }else if(StringUtils.equalsIgnoreCase(MetricType.Names.External, metricSpecRow.getType())){
                                        K8sHorizontalPodAutoscalerMetricExternalVO metricExternal = new K8sHorizontalPodAutoscalerMetricExternalVO();
                                        metricExternal.setMetricType(metricSpecRow.getType());
                                        metricExternal.setMetricName(metricSpecRow.getExternal().getMetric().getName());
                                        metricExternal.setTargetType(metricSpecRow.getExternal().getTarget().getType());

                                        // spec.external.metric.selector
                                        String selectorJson = k8sJson.serialize(metricSpecRow.getExternal().getMetric().getSelector());
                                        metricExternal.setSelector(k8sJson.getGson().fromJson(selectorJson, K8sLabelSelectorVO.class));

                                        // set target value
                                        if (StringUtils.equalsIgnoreCase(metricSpecRow.getExternal().getTarget().getType(), MetricTargetType.AverageValue.getCode())) {
                                            metricExternal.setTargetAverageValue(metricSpecRow.getExternal().getTarget().getAverageValue() != null ? metricSpecRow.getExternal().getTarget().getAverageValue().toSuffixedString() : null);
                                        } else if (StringUtils.equalsIgnoreCase(metricSpecRow.getExternal().getTarget().getType(), MetricTargetType.Value.getCode())) {
                                            metricExternal.setTargetValue(metricSpecRow.getExternal().getTarget().getValue() != null ? metricSpecRow.getExternal().getTarget().getValue().toSuffixedString() : null);
                                        } else {
                                            metricExternal.setTargetAverageUtilization(metricSpecRow.getExternal().getTarget().getAverageUtilization());
                                        }

                                        // set current value
                                        for(V2beta2MetricStatus currentMetricRow : currentMetrics){
                                            if(StringUtils.equalsIgnoreCase(currentMetricRow.getType(), MetricType.Names.External)
                                                    && StringUtils.equalsIgnoreCase(currentMetricRow.getExternal().getMetric().getName(), metricSpecRow.getExternal().getMetric().getName())){
                                                metricExternal.setCurrentAverageUtilization(currentMetricRow.getExternal().getCurrent().getAverageUtilization());
                                                metricExternal.setCurrentAverageValue(currentMetricRow.getExternal().getCurrent().getAverageValue().toSuffixedString());
                                                metricExternal.setCurrentValue(currentMetricRow.getExternal().getCurrent().getValue().toSuffixedString());
                                                break;
                                            }
                                        }
                                        metrics.add(metricExternal);
                                    }else if(StringUtils.equalsIgnoreCase(MetricType.Names.ContainerResource, metricSpecRow.getType())){
                                        K8sHorizontalPodAutoscalerMetricContainerVO metricContainerResource = new K8sHorizontalPodAutoscalerMetricContainerVO();
                                        metricContainerResource.setMetricType(metricSpecRow.getType());
                                        metricContainerResource.setName(metricSpecRow.getContainerResource().getName());
                                        metricContainerResource.setContainer(metricSpecRow.getContainerResource().getContainer());
                                        metricContainerResource.setTargetType(metricSpecRow.getContainerResource().getTarget().getType());

                                        // set target value
                                        if (StringUtils.equalsIgnoreCase(metricSpecRow.getContainerResource().getTarget().getType(), MetricTargetType.AverageValue.getCode())) {
                                            metricContainerResource.setTargetAverageValue(metricSpecRow.getContainerResource().getTarget().getAverageValue() != null ? metricSpecRow.getContainerResource().getTarget().getAverageValue().toSuffixedString() : null);
                                        } else if (StringUtils.equalsIgnoreCase(metricSpecRow.getContainerResource().getTarget().getType(), MetricTargetType.Value.getCode())) {
                                            metricContainerResource.setTargetValue(metricSpecRow.getContainerResource().getTarget().getValue() != null ? metricSpecRow.getContainerResource().getTarget().getValue().toSuffixedString() : null);
                                        } else {
                                            metricContainerResource.setTargetAverageUtilization(metricSpecRow.getContainerResource().getTarget().getAverageUtilization());
                                        }

                                        // set current value
                                        for(V2beta2MetricStatus currentMetricRow : currentMetrics){
                                            if(StringUtils.equalsIgnoreCase(currentMetricRow.getType(), MetricType.Names.ContainerResource)
                                                    && StringUtils.equalsIgnoreCase(currentMetricRow.getContainerResource().getName(), metricSpecRow.getContainerResource().getName())){
                                                metricContainerResource.setCurrentAverageUtilization(Optional.ofNullable(currentMetricRow.getContainerResource()).map(V2beta2ContainerResourceMetricStatus::getCurrent).map(V2beta2MetricValueStatus::getAverageUtilization).orElseGet(() ->0));
                                                metricContainerResource.setCurrentAverageValue(Optional.ofNullable(currentMetricRow.getContainerResource()).map(V2beta2ContainerResourceMetricStatus::getCurrent).map(V2beta2MetricValueStatus::getAverageValue).orElseGet(() ->new Quantity("0")).toSuffixedString());
                                                metricContainerResource.setCurrentValue(Optional.ofNullable(currentMetricRow.getContainerResource()).map(V2beta2ContainerResourceMetricStatus::getCurrent).map(V2beta2MetricValueStatus::getValue).orElseGet(() ->new Quantity("0")).toSuffixedString());
                                                break;
                                            }
                                        }
                                        metrics.add(metricContainerResource);
                                    }
                                }
                            }
                            horizontalPodAutoscaler.setMetrics(metrics);
                            horizontalPodAutoscaler.setStartTime(v2beta2HorizontalPodAutoscalerRow.getMetadata().getCreationTimestamp());
                            horizontalPodAutoscaler.setDeployment(k8sJson.serialize(v2beta2HorizontalPodAutoscalerRow));
                            horizontalPodAutoscaler.setDeploymentYaml(Yaml.getSnakeYaml().dumpAsMap(v2beta2HorizontalPodAutoscalerRow));

                            // Horizontal Pod Autoscalers Detail
                            K8sHorizontalPodAutoscalerDetailVO horizontalPodAutoscalerDetail = new K8sHorizontalPodAutoscalerDetailVO();
                            horizontalPodAutoscalerDetail.setName(v2beta2HorizontalPodAutoscalerRow.getMetadata().getName());
                            horizontalPodAutoscalerDetail.setNamespace(v2beta2HorizontalPodAutoscalerRow.getMetadata().getNamespace());
                            horizontalPodAutoscalerDetail.setLabels(v2beta2HorizontalPodAutoscalerRow.getMetadata().getLabels());
                            horizontalPodAutoscalerDetail.setAnnotations(v2beta2HorizontalPodAutoscalerRow.getMetadata().getAnnotations());
                            horizontalPodAutoscalerDetail.setCreationTime(v2beta2HorizontalPodAutoscalerRow.getMetadata().getCreationTimestamp());
                            horizontalPodAutoscalerDetail.setTarget(String.format("%s: %s", v2beta2HorizontalPodAutoscalerRow.getSpec().getScaleTargetRef().getKind(), v2beta2HorizontalPodAutoscalerRow.getSpec().getScaleTargetRef().getName()));
                            horizontalPodAutoscalerDetail.setMinReplicas(v2beta2HorizontalPodAutoscalerRow.getSpec().getMinReplicas());
                            horizontalPodAutoscalerDetail.setMaxReplicas(v2beta2HorizontalPodAutoscalerRow.getSpec().getMaxReplicas());
                            horizontalPodAutoscalerDetail.setCurrentReplicas(v2beta2HorizontalPodAutoscalerRow.getStatus().getCurrentReplicas());
                            horizontalPodAutoscalerDetail.setDesiredReplicas(v2beta2HorizontalPodAutoscalerRow.getStatus().getDesiredReplicas());
                            horizontalPodAutoscalerDetail.setLastScaleTime(v2beta2HorizontalPodAutoscalerRow.getStatus().getLastScaleTime());
                            horizontalPodAutoscalerDetail.setMetrics(metrics);
                            if (v2beta2HorizontalPodAutoscalerRow.getSpec().getBehavior() != null) {
                                if (v2beta2HorizontalPodAutoscalerRow.getSpec().getBehavior().getScaleDown() != null) {
                                    String scaleDownJson = JsonUtils.toGson(v2beta2HorizontalPodAutoscalerRow.getSpec().getBehavior().getScaleDown());
                                    K8sHorizontalPodAutoscalerScalingRulesVO scaleDown = JsonUtils.fromGson(scaleDownJson, K8sHorizontalPodAutoscalerScalingRulesVO.class);
                                    horizontalPodAutoscalerDetail.setScaleDown(scaleDown);
                                }
                                if (v2beta2HorizontalPodAutoscalerRow.getSpec().getBehavior().getScaleUp() != null) {
                                    String scaleUpJson = JsonUtils.toGson(v2beta2HorizontalPodAutoscalerRow.getSpec().getBehavior().getScaleUp());
                                    K8sHorizontalPodAutoscalerScalingRulesVO scaleUp = JsonUtils.fromGson(scaleUpJson, K8sHorizontalPodAutoscalerScalingRulesVO.class);
                                    horizontalPodAutoscalerDetail.setScaleUp(scaleUp);
                                }
                            }
                            horizontalPodAutoscaler.setDetail(horizontalPodAutoscalerDetail);

                            // Horizontal Pod Autoscalers Target Set
                            String scaleTargetRefJson = JsonUtils.toGson(v2beta2HorizontalPodAutoscalerRow.getSpec().getScaleTargetRef());
                            K8sCrossVersionObjectReferenceVO scaleTargetRef = JsonUtils.fromGson(scaleTargetRefJson, K8sCrossVersionObjectReferenceVO.class);
                            horizontalPodAutoscaler.setScaleTargetRef(scaleTargetRef);

                            // Horizontal Pod Autoscalers Condition
                            List<K8sHorizontalPodAutoscalerConditionVO> conditions = new ArrayList<>();
                            if(CollectionUtils.isNotEmpty(v2beta2HorizontalPodAutoscalerRow.getStatus().getConditions())){
                                for(V2beta2HorizontalPodAutoscalerCondition hpaConditionRow : v2beta2HorizontalPodAutoscalerRow.getStatus().getConditions()){
                                    K8sHorizontalPodAutoscalerConditionVO conditionTemp = new K8sHorizontalPodAutoscalerConditionVO();
                                    BeanUtils.copyProperties(conditionTemp, hpaConditionRow);
                                    if(hpaConditionRow.getLastTransitionTime() != null){
                                        conditionTemp.setLastTransitionTime(hpaConditionRow.getLastTransitionTime());
                                    }

                                    conditions.add(conditionTemp);
                                }
                            }
                            horizontalPodAutoscaler.setConditions(conditions);

                            if(hpas != null){
                                hpas.add(horizontalPodAutoscaler);
                            }
                            if(hpaMap != null){
                                hpaMap.put(v2beta2HorizontalPodAutoscalerRow.getMetadata().getName(), horizontalPodAutoscaler);
                            }
                        }
                    }
                }
                else if(apiVerKindType.getGroupType() == K8sApiGroupType.AUTOSCALING && apiVerKindType.getApiType() == K8sApiType.V2){

                    List<V2HorizontalPodAutoscaler> v2HorizontalPodAutoscalers = k8sWorker.getHorizontalPodAutoscalersV2(cluster, namespace, field, label);

                    if(CollectionUtils.isNotEmpty(v2HorizontalPodAutoscalers)){

                        QuantityFormatter quantityFormatter = new QuantityFormatter();
                        K8sHorizontalPodAutoscalerVO horizontalPodAutoscaler = null;

                        for(V2HorizontalPodAutoscaler v2HorizontalPodAutoscalerRow : v2HorizontalPodAutoscalers){
                            horizontalPodAutoscaler = new K8sHorizontalPodAutoscalerVO();

                            // Horizontal Pod Autoscalers 목록
                            horizontalPodAutoscaler.setLabel(label);
                            horizontalPodAutoscaler.setNamespace(v2HorizontalPodAutoscalerRow.getMetadata().getNamespace());
                            horizontalPodAutoscaler.setName(v2HorizontalPodAutoscalerRow.getMetadata().getName());
                            horizontalPodAutoscaler.setMinReplicas(v2HorizontalPodAutoscalerRow.getSpec().getMinReplicas());
                            horizontalPodAutoscaler.setMaxReplicas(v2HorizontalPodAutoscalerRow.getSpec().getMaxReplicas());
                            List<K8sHorizontalPodAutoscalerMetricVO> metrics = new ArrayList<>();
                            if(CollectionUtils.isNotEmpty(v2HorizontalPodAutoscalerRow.getSpec().getMetrics())){
                                List<V2MetricStatus> currentMetrics = new ArrayList<>();
                                if(v2HorizontalPodAutoscalerRow.getStatus() != null && CollectionUtils.isNotEmpty(v2HorizontalPodAutoscalerRow.getStatus().getCurrentMetrics())){
                                    currentMetrics.addAll(v2HorizontalPodAutoscalerRow.getStatus().getCurrentMetrics());
                                }
                                for (V2MetricSpec metricSpecRow : v2HorizontalPodAutoscalerRow.getSpec().getMetrics()){
                                    if(StringUtils.equalsIgnoreCase(MetricType.Names.Resource, metricSpecRow.getType())){
                                        K8sHorizontalPodAutoscalerMetricResourceVO metricResource = new K8sHorizontalPodAutoscalerMetricResourceVO();
                                        metricResource.setMetricType(metricSpecRow.getType());
                                        metricResource.setName(metricSpecRow.getResource().getName());
                                        metricResource.setTargetType(metricSpecRow.getResource().getTarget().getType());

                                        // set target value
                                        if (StringUtils.equalsIgnoreCase(metricSpecRow.getResource().getTarget().getType(), MetricTargetType.AverageValue.getCode())) {
                                            metricResource.setTargetAverageValue(metricSpecRow.getResource().getTarget().getAverageValue() != null ? metricSpecRow.getResource().getTarget().getAverageValue().toSuffixedString() : null);
                                        } else if (StringUtils.equalsIgnoreCase(metricSpecRow.getResource().getTarget().getType(), MetricTargetType.Value.getCode())) {
                                            metricResource.setTargetValue(metricSpecRow.getResource().getTarget().getValue() != null ? metricSpecRow.getResource().getTarget().getValue().toSuffixedString() : null);
                                        } else {
                                            metricResource.setTargetAverageUtilization(metricSpecRow.getResource().getTarget().getAverageUtilization());
                                        }

                                        // set current value
                                        for(V2MetricStatus currentMetricRow : currentMetrics){
                                            if(StringUtils.equalsIgnoreCase(currentMetricRow.getType(), MetricType.Names.Resource)
                                                    && StringUtils.equalsIgnoreCase(currentMetricRow.getResource().getName(), metricSpecRow.getResource().getName())){
                                                metricResource.setCurrentAverageUtilization(Optional.ofNullable(currentMetricRow.getResource()).map(V2ResourceMetricStatus::getCurrent).map(V2MetricValueStatus::getAverageUtilization).orElseGet(() ->0));
                                                metricResource.setCurrentAverageValue(Optional.ofNullable(currentMetricRow.getResource()).map(V2ResourceMetricStatus::getCurrent).map(V2MetricValueStatus::getAverageValue).orElseGet(() ->new Quantity("0")).toSuffixedString());
                                                metricResource.setCurrentValue(Optional.ofNullable(currentMetricRow.getResource()).map(V2ResourceMetricStatus::getCurrent).map(V2MetricValueStatus::getValue).orElseGet(() ->new Quantity("0")).toSuffixedString());
                                                break;
                                            }
                                        }
                                        metrics.add(metricResource);
                                    }else if(StringUtils.equalsIgnoreCase(MetricType.Names.Pods, metricSpecRow.getType())){
                                        K8sHorizontalPodAutoscalerMetricPodsVO metricPods = new K8sHorizontalPodAutoscalerMetricPodsVO();
                                        metricPods.setMetricType(metricSpecRow.getType());
                                        metricPods.setMetricName(metricSpecRow.getPods().getMetric().getName());
                                        metricPods.setTargetType(metricSpecRow.getPods().getTarget().getType());

                                        String selectorJson = k8sJson.serialize(metricSpecRow.getPods().getMetric().getSelector());
                                        metricPods.setSelector(k8sJson.getGson().fromJson(selectorJson, K8sLabelSelectorVO.class));

                                        // set target value
                                        if (StringUtils.equalsIgnoreCase(metricSpecRow.getPods().getTarget().getType(), MetricTargetType.AverageValue.getCode())) {
                                            metricPods.setTargetAverageValue(metricSpecRow.getPods().getTarget().getAverageValue() != null ? metricSpecRow.getPods().getTarget().getAverageValue().toSuffixedString() : null);
                                        } else if (StringUtils.equalsIgnoreCase(metricSpecRow.getPods().getTarget().getType(), MetricTargetType.Value.getCode())) {
                                            metricPods.setTargetValue(metricSpecRow.getPods().getTarget().getValue() != null ? metricSpecRow.getPods().getTarget().getValue().toSuffixedString() : null);
                                        } else {
                                            metricPods.setTargetAverageUtilization(metricSpecRow.getPods().getTarget().getAverageUtilization());
                                        }

                                        // set current value
                                        for(V2MetricStatus currentMetricRow : currentMetrics){
                                            if(StringUtils.equalsIgnoreCase(currentMetricRow.getType(), MetricType.Names.Pods)
                                                    && StringUtils.equalsIgnoreCase(currentMetricRow.getPods().getMetric().getName(), metricSpecRow.getPods().getMetric().getName())){
                                                metricPods.setCurrentAverageUtilization(currentMetricRow.getPods().getCurrent().getAverageUtilization());
                                                metricPods.setCurrentAverageValue(currentMetricRow.getPods().getCurrent().getAverageValue().toSuffixedString());
                                                metricPods.setCurrentValue(currentMetricRow.getPods().getCurrent().getValue().toSuffixedString());
                                                break;
                                            }
                                        }
                                        metrics.add(metricPods);
                                    }else if(StringUtils.equalsIgnoreCase(MetricType.Names.Object, metricSpecRow.getType())){
                                        K8sHorizontalPodAutoscalerMetricObjectVO metricObject = new K8sHorizontalPodAutoscalerMetricObjectVO();
                                        metricObject.setMetricType(metricSpecRow.getType());
                                        metricObject.setMetricName(metricSpecRow.getObject().getMetric().getName());
                                        metricObject.setTargetType(metricSpecRow.getObject().getTarget().getType());

                                        // spec.object.metric.selector
                                        String selectorJson = k8sJson.serialize(metricSpecRow.getObject().getMetric().getSelector());
                                        metricObject.setSelector(k8sJson.getGson().fromJson(selectorJson, K8sLabelSelectorVO.class));

                                        // spec.object.describedObject
                                        metricObject.setDescribedObject(mapper.readValue(k8sJson.serialize(metricSpecRow.getObject().getDescribedObject()), new TypeReference<Map<String, String>>(){}));

                                        // set target value
                                        if (StringUtils.equalsIgnoreCase(metricSpecRow.getObject().getTarget().getType(), MetricTargetType.AverageValue.getCode())) {
                                            metricObject.setTargetAverageValue(metricSpecRow.getObject().getTarget().getAverageValue() != null ? metricSpecRow.getObject().getTarget().getAverageValue().toSuffixedString() : null);
                                        } else if (StringUtils.equalsIgnoreCase(metricSpecRow.getObject().getTarget().getType(), MetricTargetType.Value.getCode())) {
                                            metricObject.setTargetValue(metricSpecRow.getObject().getTarget().getValue() != null ? metricSpecRow.getObject().getTarget().getValue().toSuffixedString() : null);
                                        } else {
                                            metricObject.setTargetAverageUtilization(metricSpecRow.getObject().getTarget().getAverageUtilization());
                                        }

                                        // set current value
                                        for(V2MetricStatus currentMetricRow : currentMetrics){
                                            if(StringUtils.equalsIgnoreCase(currentMetricRow.getType(), MetricType.Names.Object)
                                                    && StringUtils.equalsIgnoreCase(currentMetricRow.getObject().getMetric().getName(), metricSpecRow.getObject().getMetric().getName())){
                                                metricObject.setCurrentAverageUtilization(currentMetricRow.getObject().getCurrent().getAverageUtilization());
                                                metricObject.setCurrentAverageValue(currentMetricRow.getObject().getCurrent().getAverageValue().toSuffixedString());
                                                metricObject.setCurrentValue(currentMetricRow.getObject().getCurrent().getValue().toSuffixedString());
                                                break;
                                            }
                                        }
                                        metrics.add(metricObject);
                                    }else if(StringUtils.equalsIgnoreCase(MetricType.Names.External, metricSpecRow.getType())){
                                        K8sHorizontalPodAutoscalerMetricExternalVO metricExternal = new K8sHorizontalPodAutoscalerMetricExternalVO();
                                        metricExternal.setMetricType(metricSpecRow.getType());
                                        metricExternal.setMetricName(metricSpecRow.getExternal().getMetric().getName());
                                        metricExternal.setTargetType(metricSpecRow.getExternal().getTarget().getType());

                                        // spec.external.metric.selector
                                        String selectorJson = k8sJson.serialize(metricSpecRow.getExternal().getMetric().getSelector());
                                        metricExternal.setSelector(k8sJson.getGson().fromJson(selectorJson, K8sLabelSelectorVO.class));

                                        // set target value
                                        if (StringUtils.equalsIgnoreCase(metricSpecRow.getExternal().getTarget().getType(), MetricTargetType.AverageValue.getCode())) {
                                            metricExternal.setTargetAverageValue(metricSpecRow.getExternal().getTarget().getAverageValue() != null ? metricSpecRow.getExternal().getTarget().getAverageValue().toSuffixedString() : null);
                                        } else if (StringUtils.equalsIgnoreCase(metricSpecRow.getExternal().getTarget().getType(), MetricTargetType.Value.getCode())) {
                                            metricExternal.setTargetValue(metricSpecRow.getExternal().getTarget().getValue() != null ? metricSpecRow.getExternal().getTarget().getValue().toSuffixedString() : null);
                                        } else {
                                            metricExternal.setTargetAverageUtilization(metricSpecRow.getExternal().getTarget().getAverageUtilization());
                                        }

                                        // set current value
                                        for(V2MetricStatus currentMetricRow : currentMetrics){
                                            if(StringUtils.equalsIgnoreCase(currentMetricRow.getType(), MetricType.Names.External)
                                                    && StringUtils.equalsIgnoreCase(currentMetricRow.getExternal().getMetric().getName(), metricSpecRow.getExternal().getMetric().getName())){
                                                metricExternal.setCurrentAverageUtilization(currentMetricRow.getExternal().getCurrent().getAverageUtilization());
                                                metricExternal.setCurrentAverageValue(currentMetricRow.getExternal().getCurrent().getAverageValue().toSuffixedString());
                                                metricExternal.setCurrentValue(currentMetricRow.getExternal().getCurrent().getValue().toSuffixedString());
                                                break;
                                            }
                                        }
                                        metrics.add(metricExternal);
                                    }else if(StringUtils.equalsIgnoreCase(MetricType.Names.ContainerResource, metricSpecRow.getType())){
                                        K8sHorizontalPodAutoscalerMetricContainerVO metricContainerResource = new K8sHorizontalPodAutoscalerMetricContainerVO();
                                        metricContainerResource.setMetricType(metricSpecRow.getType());
                                        metricContainerResource.setName(metricSpecRow.getContainerResource().getName());
                                        metricContainerResource.setContainer(metricSpecRow.getContainerResource().getContainer());
                                        metricContainerResource.setTargetType(metricSpecRow.getContainerResource().getTarget().getType());

                                        // set target value
                                        if (StringUtils.equalsIgnoreCase(metricSpecRow.getContainerResource().getTarget().getType(), MetricTargetType.AverageValue.getCode())) {
                                            metricContainerResource.setTargetAverageValue(metricSpecRow.getContainerResource().getTarget().getAverageValue() != null ? metricSpecRow.getContainerResource().getTarget().getAverageValue().toSuffixedString() : null);
                                        } else if (StringUtils.equalsIgnoreCase(metricSpecRow.getContainerResource().getTarget().getType(), MetricTargetType.Value.getCode())) {
                                            metricContainerResource.setTargetValue(metricSpecRow.getContainerResource().getTarget().getValue() != null ? metricSpecRow.getContainerResource().getTarget().getValue().toSuffixedString() : null);
                                        } else {
                                            metricContainerResource.setTargetAverageUtilization(metricSpecRow.getContainerResource().getTarget().getAverageUtilization());
                                        }

                                        // set current value
                                        for(V2MetricStatus currentMetricRow : currentMetrics){
                                            if(StringUtils.equalsIgnoreCase(currentMetricRow.getType(), MetricType.Names.ContainerResource)
                                                    && StringUtils.equalsIgnoreCase(currentMetricRow.getContainerResource().getName(), metricSpecRow.getContainerResource().getName())){
                                                metricContainerResource.setCurrentAverageUtilization(Optional.ofNullable(currentMetricRow.getContainerResource()).map(V2ContainerResourceMetricStatus::getCurrent).map(V2MetricValueStatus::getAverageUtilization).orElseGet(() ->0));
                                                metricContainerResource.setCurrentAverageValue(Optional.ofNullable(currentMetricRow.getContainerResource()).map(V2ContainerResourceMetricStatus::getCurrent).map(V2MetricValueStatus::getAverageValue).orElseGet(() ->new Quantity("0")).toSuffixedString());
                                                metricContainerResource.setCurrentValue(Optional.ofNullable(currentMetricRow.getContainerResource()).map(V2ContainerResourceMetricStatus::getCurrent).map(V2MetricValueStatus::getValue).orElseGet(() ->new Quantity("0")).toSuffixedString());
                                                break;
                                            }
                                        }
                                        metrics.add(metricContainerResource);
                                    }
                                }
                            }
                            horizontalPodAutoscaler.setMetrics(metrics);
                            horizontalPodAutoscaler.setStartTime(v2HorizontalPodAutoscalerRow.getMetadata().getCreationTimestamp());
                            horizontalPodAutoscaler.setDeployment(k8sJson.serialize(v2HorizontalPodAutoscalerRow));
                            horizontalPodAutoscaler.setDeploymentYaml(Yaml.getSnakeYaml().dumpAsMap(v2HorizontalPodAutoscalerRow));

                            // Horizontal Pod Autoscalers Detail
                            K8sHorizontalPodAutoscalerDetailVO horizontalPodAutoscalerDetail = new K8sHorizontalPodAutoscalerDetailVO();
                            horizontalPodAutoscalerDetail.setName(v2HorizontalPodAutoscalerRow.getMetadata().getName());
                            horizontalPodAutoscalerDetail.setNamespace(v2HorizontalPodAutoscalerRow.getMetadata().getNamespace());
                            horizontalPodAutoscalerDetail.setLabels(v2HorizontalPodAutoscalerRow.getMetadata().getLabels());
                            horizontalPodAutoscalerDetail.setAnnotations(v2HorizontalPodAutoscalerRow.getMetadata().getAnnotations());
                            horizontalPodAutoscalerDetail.setCreationTime(v2HorizontalPodAutoscalerRow.getMetadata().getCreationTimestamp());
                            horizontalPodAutoscalerDetail.setTarget(String.format("%s: %s", v2HorizontalPodAutoscalerRow.getSpec().getScaleTargetRef().getKind(), v2HorizontalPodAutoscalerRow.getSpec().getScaleTargetRef().getName()));
                            horizontalPodAutoscalerDetail.setMinReplicas(v2HorizontalPodAutoscalerRow.getSpec().getMinReplicas());
                            horizontalPodAutoscalerDetail.setMaxReplicas(v2HorizontalPodAutoscalerRow.getSpec().getMaxReplicas());
                            horizontalPodAutoscalerDetail.setCurrentReplicas(v2HorizontalPodAutoscalerRow.getStatus().getCurrentReplicas());
                            horizontalPodAutoscalerDetail.setDesiredReplicas(v2HorizontalPodAutoscalerRow.getStatus().getDesiredReplicas());
                            horizontalPodAutoscalerDetail.setLastScaleTime(v2HorizontalPodAutoscalerRow.getStatus().getLastScaleTime());
                            horizontalPodAutoscalerDetail.setMetrics(metrics);
                            if (v2HorizontalPodAutoscalerRow.getSpec().getBehavior() != null) {
                                if (v2HorizontalPodAutoscalerRow.getSpec().getBehavior().getScaleDown() != null) {
                                    String scaleDownJson = JsonUtils.toGson(v2HorizontalPodAutoscalerRow.getSpec().getBehavior().getScaleDown());
                                    K8sHorizontalPodAutoscalerScalingRulesVO scaleDown = JsonUtils.fromGson(scaleDownJson, K8sHorizontalPodAutoscalerScalingRulesVO.class);
                                    horizontalPodAutoscalerDetail.setScaleDown(scaleDown);
                                }
                                if (v2HorizontalPodAutoscalerRow.getSpec().getBehavior().getScaleUp() != null) {
                                    String scaleUpJson = JsonUtils.toGson(v2HorizontalPodAutoscalerRow.getSpec().getBehavior().getScaleUp());
                                    K8sHorizontalPodAutoscalerScalingRulesVO scaleUp = JsonUtils.fromGson(scaleUpJson, K8sHorizontalPodAutoscalerScalingRulesVO.class);
                                    horizontalPodAutoscalerDetail.setScaleUp(scaleUp);
                                }
                            }
                            horizontalPodAutoscaler.setDetail(horizontalPodAutoscalerDetail);

                            // Horizontal Pod Autoscalers Target Set
                            String scaleTargetRefJson = JsonUtils.toGson(v2HorizontalPodAutoscalerRow.getSpec().getScaleTargetRef());
                            K8sCrossVersionObjectReferenceVO scaleTargetRef = JsonUtils.fromGson(scaleTargetRefJson, K8sCrossVersionObjectReferenceVO.class);
                            horizontalPodAutoscaler.setScaleTargetRef(scaleTargetRef);

                            // Horizontal Pod Autoscalers Condition
                            List<K8sHorizontalPodAutoscalerConditionVO> conditions = new ArrayList<>();
                            if(CollectionUtils.isNotEmpty(v2HorizontalPodAutoscalerRow.getStatus().getConditions())){
                                for(V2HorizontalPodAutoscalerCondition hpaConditionRow : v2HorizontalPodAutoscalerRow.getStatus().getConditions()){
                                    K8sHorizontalPodAutoscalerConditionVO conditionTemp = new K8sHorizontalPodAutoscalerConditionVO();
                                    BeanUtils.copyProperties(conditionTemp, hpaConditionRow);
                                    if(hpaConditionRow.getLastTransitionTime() != null){
                                        conditionTemp.setLastTransitionTime(hpaConditionRow.getLastTransitionTime());
                                    }

                                    conditions.add(conditionTemp);
                                }
                            }
                            horizontalPodAutoscaler.setConditions(conditions);

                            if(hpas != null){
                                hpas.add(horizontalPodAutoscaler);
                            }
                            if(hpaMap != null){
                                hpaMap.put(v2HorizontalPodAutoscalerRow.getMetadata().getName(), horizontalPodAutoscaler);
                            }
                        }
                    }
                }
            } else {
                log.warn("genHorizontalPodAutoscalerData cluster {} version {} not supported.", cluster.getClusterName(), cluster.getK8sVersion());
            }
        } else {
            log.warn("genHorizontalPodAutoscalerData - cluster is null.");
        }

    }

    /**
     * K8S Horizontal Pod Autoscalers 생성
     *
     * @param cluster
     * @param namespace
     * @param component
     * @param hpa
     * @param name
     * @param k8sApiVerKindType
     * @return
     * @throws Exception
     */
    public boolean createHorizontalPodAutoscaler(ClusterVO cluster, String namespace, ComponentVO component, HpaGuiVO hpa, String name, K8sApiVerKindType k8sApiVerKindType) throws Exception {
        /**
         * 현재(Cocktail v2.5.4기준) HPA에 V1과 V2beta1 Spec이 공존할 수 있으므로 HPA 객체의 정보를 기준으로 V1인지 V2beta1인지 판단하여 분기 처리
         * 2018.08.09 redion
         */
        if(this.isAutoscaling(hpa)) {
//            K8sApiType k8sApiType = this.getHpaApiType(hpa);

            K8sApiVerKindType hpaType = ResourceUtil.getApiVerKindType(cluster, K8sApiKindType.HORIZONTAL_POD_AUTOSCALER);

            if (hpaType != null) {
                if(K8sApiType.V2 == hpaType.getApiType()) {
                    V2HorizontalPodAutoscaler objV2HorizontalPodAutoscaler = k8sWorker.getHorizontalPodAutoscalerV2(cluster, namespace, name);

                    if (objV2HorizontalPodAutoscaler != null) {
                        k8sWorker.deleteHorizontalPodAutoscalerV2WithName(cluster, namespace, name);
                        Thread.sleep(100);
                    }

                    objV2HorizontalPodAutoscaler = K8sSpecFactory.buildHpaV2(hpa, namespace, component.getComponentName(), name, k8sApiVerKindType);
                    V2HorizontalPodAutoscaler rtnObj = k8sWorker.createHorizontalPodAutoscalerV2(cluster, cluster.getNamespaceName(), objV2HorizontalPodAutoscaler, false);

                    if (rtnObj == null) {
                        return false;
                    }
                }
                else if(K8sApiType.V2BETA2 == hpaType.getApiType()) {
                    V2beta2HorizontalPodAutoscaler objV2beta2HorizontalPodAutoscaler = k8sWorker.getHorizontalPodAutoscalerV2beta2(cluster, namespace, name);

                    if (objV2beta2HorizontalPodAutoscaler != null) {
                        k8sWorker.deleteHorizontalPodAutoscalerV2beta2WithName(cluster, namespace, name);
                        Thread.sleep(100);
                    }

                    objV2beta2HorizontalPodAutoscaler = K8sSpecFactory.buildHpaV2beta2(hpa, namespace, component.getComponentName(), name, k8sApiVerKindType);
                    V2beta2HorizontalPodAutoscaler rtnObj = k8sWorker.createHorizontalPodAutoscalerV2beta2(cluster, cluster.getNamespaceName(), objV2beta2HorizontalPodAutoscaler, false);

                    if (rtnObj == null) {
                        return false;
                    }
                }
                else if(K8sApiType.V2BETA1 == hpaType.getApiType()) {
                    V2beta1HorizontalPodAutoscaler objV2beta1HorizontalPodAutoscaler = k8sWorker.getHorizontalPodAutoscalerV2beta1(cluster, namespace, name);

                    if (objV2beta1HorizontalPodAutoscaler != null) {
                        k8sWorker.deleteHorizontalPodAutoscalerV2beta1WithName(cluster, namespace, name);
                        Thread.sleep(100);
                    }

                    objV2beta1HorizontalPodAutoscaler = K8sSpecFactory.buildHpaV2beta1(hpa, namespace, component.getComponentName(), name, k8sApiVerKindType);
                    V2beta1HorizontalPodAutoscaler rtnObj = k8sWorker.createHorizontalPodAutoscalerV2beta1(cluster, namespace, objV2beta1HorizontalPodAutoscaler, false);

                    if (rtnObj == null) {
                        return false;
                    }
                }
                else if (K8sApiType.V1 == hpaType.getApiType()) {
                    V1HorizontalPodAutoscaler objV1HorizontalPodAutoscaler = k8sWorker.getHorizontalPodAutoscalerV1(cluster, namespace, name);

                    if (objV1HorizontalPodAutoscaler != null) {
                        k8sWorker.deleteHorizontalPodAutoscalerV1WithName(cluster, namespace, name);
                        Thread.sleep(100);
                    }

                    objV1HorizontalPodAutoscaler = K8sSpecFactory.buildHpaV1(hpa, namespace, component.getComponentName(), name, k8sApiVerKindType);
                    V1HorizontalPodAutoscaler rtnObj = k8sWorker.createHorizontalPodAutoscalerV1(cluster, namespace, objV1HorizontalPodAutoscaler, false);

                    if (rtnObj == null) {
                        return false;
                    }
                }
            } else {
                V2HorizontalPodAutoscaler objV2HorizontalPodAutoscaler = k8sWorker.getHorizontalPodAutoscalerV2(cluster, namespace, name);

                if (objV2HorizontalPodAutoscaler != null) {
                    k8sWorker.deleteHorizontalPodAutoscalerV2WithName(cluster, namespace, name);
                    Thread.sleep(100);
                }

                objV2HorizontalPodAutoscaler = K8sSpecFactory.buildHpaV2(hpa, namespace, component.getComponentName(), name, k8sApiVerKindType);
                V2HorizontalPodAutoscaler rtnObj = k8sWorker.createHorizontalPodAutoscalerV2(cluster, cluster.getNamespaceName(), objV2HorizontalPodAutoscaler, false);

                if (rtnObj == null) {
                    return false;
                }
            }

        }
        else { // Autoscaling을 할 수 없는 케이스중 None Autoscaling의 경우 오류가 아니므로 정상케이스로 분기
            boolean useExtensionMetric = (hpa.getMetrics() != null && hpa.getMetrics().size() > 0); // V2Beta1 Spec 인지 확인
            if (StringUtils.isBlank(hpa.getType()) && !useExtensionMetric) {
                // HPA Type에 값이 없고 ExtensionMetric도 없음 => Autoscaling 하지 않는 케이스
                // 오류는 아니지만 CreateHorizontalPodAutoscaler 호출전에 판단되어서 이곳으로 들어오면 안되는 케이스임
                // info 로그 남기고 정상으로 처리함
                log.info(String.format("Case Without Autoscaling : %s", name));
                return true;
            } else {
                // Extension Spec은 존재하는데 hpatype이 Metric이 아닌 경우
                // hpatype은 Metric이나 Extension Spec이 존재하지 않는 경우
                // 그 외 오류 케이스.
                log.info(String.format("HorizontalPodAutoscaler Request Error : Type = %s, MetricSize = %s", hpa.getType() != null ? hpa.getType() : "", hpa.getMetrics() != null ? hpa.getMetrics().size() : "0"));
                return false;
            }
        }

        return true;
    }

    /**
     * HorizontalPodAutoscaler 수정
     *
     * @param cluster
     * @param server
     * @param context
     * @return
     * @throws Exception
     */
//    public boolean updateHorizontalPodAutoscaler(ClusterVO cluster, ServerDetailVO server, ExecutingContextVO context) throws Exception {
//
//        // Convert to JsonObject
//        log.debug("Converted Server creation request: {}", JsonUtils.toGson(server));
//
//        K8sHorizontalPodAutoscalerVO k8sHorizontalPodAutoscaler = null;
//        try {
//            K8sApiVerKindType apiVerKindType = context.getWorkloadVersionSetMap().get(K8sApiKindType.HORIZONTAL_POD_AUTOSCALER);
//
//            if(apiVerKindType.getGroupType() == K8sApiGroupType.AUTOSCALING && apiVerKindType.getApiType() == K8sApiType.V2BETA2){
//
//                V2beta2HorizontalPodAutoscaler updatedHpa = K8sSpecFactory.buildHpaV2beta2(server.getServer().getHpa(), cluster.getNamespaceName(), server.getComponent().getComponentId(), server.getComponent().getComponentName());
//
//                // 현재 Hpa 조회
//                V2beta2HorizontalPodAutoscaler currentHpa = k8sWorker.getHorizontalPodAutoscalerV2beta2(cluster, cluster.getNamespaceName(), server.getComponent().getComponentName());
//
//                // patchJson 으로 변경
//                List<JsonObject> patchBody = k8sPatchSpecFactory.buildPatch(currentHpa, updatedHpa);
//                log.debug("########## HpaV2beta2 patchBody JSON: {}", JsonUtils.toGson(patchBody));
//                // patch
//                V2beta2HorizontalPodAutoscaler result = k8sWorker.patchHorizontalPodAutoscaleV2beta2(cluster, cluster.getNamespaceName(), server.getComponent().getComponentName(), patchBody);
//
//                Thread.sleep(100);
//
//                if (result == null) {
//                    return false;
//                }
//            }
//        } catch (Exception e) {
//            throw new CocktailException("updateHorizontalPodAutoscaler fail!!", e, ExceptionType.K8sCocktailCloudUpdateFail);
//        }
//
//        return true;
//    }

    /**
     * get HorizontalPodAutoscalers V1
     * @param cluster
     * @param namespace
     * @param fieldSelector
     * @param labelSelector
     * @return
     * @throws Exception
     */
    public List<V1HorizontalPodAutoscaler> getHorizontalPodAutoscalersV1(ClusterVO cluster, String namespace, String fieldSelector, String labelSelector) throws Exception{
        if(cluster == null){
            throw new CocktailException("cluster is null.", ExceptionType.ClusterNotFound);
        }
        try {
            return k8sWorker.getHorizontalPodAutoscalersV1(cluster, namespace, fieldSelector, labelSelector);
        } catch (Exception e) {
            throw new CocktailException("getHorizontalPodAutoscalersV1 fail!!", e, ExceptionType.K8sCocktailCloudInquireFail);
        }
    }

    /**
     * get HorizontalPodAutoscalers V2beta1
     * @param cluster
     * @param namespace
     * @param fieldSelector
     * @param labelSelector
     * @return
     * @throws Exception
     */
    public List<V2beta1HorizontalPodAutoscaler> getHorizontalPodAutoscalersV2beta1(ClusterVO cluster, String namespace, String fieldSelector, String labelSelector) throws Exception {
        if(cluster == null){
            throw new CocktailException("cluster is null.", ExceptionType.ClusterNotFound);
        }
        try {
            return k8sWorker.getHorizontalPodAutoscalersV2beta1(cluster, namespace, fieldSelector, labelSelector);
        } catch (Exception e) {
            throw new CocktailException("getHorizontalPodAutoscalersV2beta1 fail!!", e, ExceptionType.K8sCocktailCloudInquireFail);
        }
    }

    /**
     * get HorizontalPodAutoscalers V2beta2
     * @param cluster
     * @param namespace
     * @param fieldSelector
     * @param labelSelector
     * @return
     * @throws Exception
     */
    public List<V2beta2HorizontalPodAutoscaler> getHorizontalPodAutoscalersV2beta2(ClusterVO cluster, String namespace, String fieldSelector, String labelSelector) throws Exception{
        if(cluster == null){
            throw new CocktailException("cluster is null.", ExceptionType.ClusterNotFound);
        }
        try {
            return k8sWorker.getHorizontalPodAutoscalersV2beta2(cluster, namespace, fieldSelector, labelSelector);
        } catch (Exception e) {
            throw new CocktailException("getHorizontalPodAutoscalersV2beta2 fail!!", e, ExceptionType.K8sCocktailCloudInquireFail);
        }
    }

    /**
     * get HorizontalPodAutoscalers V2
     * @param cluster
     * @param namespace
     * @param fieldSelector
     * @param labelSelector
     * @return
     * @throws Exception
     */
    public List<V2HorizontalPodAutoscaler> getHorizontalPodAutoscalersV2(ClusterVO cluster, String namespace, String fieldSelector, String labelSelector) throws Exception{
        if(cluster == null){
            throw new CocktailException("cluster is null.", ExceptionType.ClusterNotFound);
        }
        try {
            return k8sWorker.getHorizontalPodAutoscalersV2(cluster, namespace, fieldSelector, labelSelector);
        } catch (Exception e) {
            throw new CocktailException("getHorizontalPodAutoscalersV2 fail!!", e, ExceptionType.K8sCocktailCloudInquireFail);
        }
    }

    /**
     * HorizontalPodAutoscaler V2 수정
     *
     * @param cluster
     * @param updatedHpa
     * @param context
     * @return
     * @throws Exception
     */
    public boolean replaceHorizontalPodAutoscaleV2(ClusterVO cluster, String namespace, V2HorizontalPodAutoscaler updatedHpa, ExecutingContextVO context) throws Exception {

        // patch
        V2HorizontalPodAutoscaler result = k8sWorker.replaceHorizontalPodAutoscaleV2(cluster, namespace, updatedHpa.getMetadata().getName(), updatedHpa, false);

        Thread.sleep(100);

        if (result == null) {
            return false;
        }

        return true;
    }

    /**
     * HorizontalPodAutoscaler V2 수정
     *
     * @param cluster
     * @param updatedHpa
     * @param context
     * @return
     * @throws Exception
     */
    public boolean patchHorizontalPodAutoscalerV2(ClusterVO cluster, String namespace, V2HorizontalPodAutoscaler updatedHpa, ExecutingContextVO context) throws Exception {

        // 현재 Hpa 조회
        V2HorizontalPodAutoscaler currentHpa = k8sWorker.getHorizontalPodAutoscalerV2(cluster, namespace, updatedHpa.getMetadata().getName());
        currentHpa.setStatus(null);

        // patchJson 으로 변경
        List<JsonObject> patchBody = k8sPatchSpecFactory.buildPatch(currentHpa, updatedHpa);
        log.debug("########## HpaV2 patchBody JSON: {}", JsonUtils.toGson(patchBody));
        // patch
        V2HorizontalPodAutoscaler result = k8sWorker.patchHorizontalPodAutoscaleV2(cluster, namespace, updatedHpa.getMetadata().getName(), patchBody, false);

        Thread.sleep(100);

        if (result == null) {
            return false;
        }

        return true;
    }

    /**
     * HorizontalPodAutoscaler V2 수정
     *
     * @param cluster
     * @param namespace
     * @param currentHpa
     * @param updatedHpa
     * @param dryRun
     * @param context
     * @return
     * @throws Exception
     */
    public boolean patchHorizontalPodAutoscalerV2(ClusterVO cluster, String namespace, V2HorizontalPodAutoscaler currentHpa, V2HorizontalPodAutoscaler updatedHpa, boolean dryRun, ExecutingContextVO context) throws Exception {

        // set null
        currentHpa.setStatus(null);
        updatedHpa.setStatus(null);

        // patchJson 으로 변경
        List<JsonObject> patchBody = k8sPatchSpecFactory.buildPatch(currentHpa, updatedHpa);
        log.debug("########## HpaV2 patchBody JSON: {}", JsonUtils.toGson(patchBody));
        // patch
        V2HorizontalPodAutoscaler result = k8sWorker.patchHorizontalPodAutoscaleV2(cluster, namespace, updatedHpa.getMetadata().getName(), patchBody, dryRun);

        if (result == null) {
            return false;
        }

        return true;
    }

    /**
     * HorizontalPodAutoscaler V2beta2 수정
     *
     * @param cluster
     * @param updatedHpa
     * @param context
     * @return
     * @throws Exception
     */
    public boolean replaceHorizontalPodAutoscaleV2beta2(ClusterVO cluster, String namespace, V2beta2HorizontalPodAutoscaler updatedHpa, ExecutingContextVO context) throws Exception {

        // patch
        V2beta2HorizontalPodAutoscaler result = k8sWorker.replaceHorizontalPodAutoscaleV2beta2(cluster, namespace, updatedHpa.getMetadata().getName(), updatedHpa, false);

        Thread.sleep(100);

        if (result == null) {
            return false;
        }

        return true;
    }

    /**
     * HorizontalPodAutoscaler V2beta2 수정
     *
     * @param cluster
     * @param updatedHpa
     * @param context
     * @return
     * @throws Exception
     */
    public boolean patchHorizontalPodAutoscalerV2beta2(ClusterVO cluster, String namespace, V2beta2HorizontalPodAutoscaler updatedHpa, ExecutingContextVO context) throws Exception {

        // 현재 Hpa 조회
        V2beta2HorizontalPodAutoscaler currentHpa = k8sWorker.getHorizontalPodAutoscalerV2beta2(cluster, namespace, updatedHpa.getMetadata().getName());
        currentHpa.setStatus(null);

        // patchJson 으로 변경
        List<JsonObject> patchBody = k8sPatchSpecFactory.buildPatch(currentHpa, updatedHpa);
        log.debug("########## HpaV2beta2 patchBody JSON: {}", JsonUtils.toGson(patchBody));
        // patch
        V2beta2HorizontalPodAutoscaler result = k8sWorker.patchHorizontalPodAutoscaleV2beta2(cluster, namespace, updatedHpa.getMetadata().getName(), patchBody, false);

        Thread.sleep(100);

        if (result == null) {
            return false;
        }

        return true;
    }

    /**
     * HorizontalPodAutoscaler V2beta2 수정
     *
     * @param cluster
     * @param namespace
     * @param currentHpa
     * @param updatedHpa
     * @param dryRun
     * @param context
     * @return
     * @throws Exception
     */
    public boolean patchHorizontalPodAutoscalerV2beta2(ClusterVO cluster, String namespace, V2beta2HorizontalPodAutoscaler currentHpa, V2beta2HorizontalPodAutoscaler updatedHpa, boolean dryRun, ExecutingContextVO context) throws Exception {

        // set null
        currentHpa.setStatus(null);
        updatedHpa.setStatus(null);

        // patchJson 으로 변경
        List<JsonObject> patchBody = k8sPatchSpecFactory.buildPatch(currentHpa, updatedHpa);
        log.debug("########## HpaV2beta2 patchBody JSON: {}", JsonUtils.toGson(patchBody));
        // patch
        V2beta2HorizontalPodAutoscaler result = k8sWorker.patchHorizontalPodAutoscaleV2beta2(cluster, namespace, updatedHpa.getMetadata().getName(), patchBody, dryRun);

        if (result == null) {
            return false;
        }

        return true;
    }


    /**
     * HorizontalPodAutoscaler V2beta1 수정
     *
     * @param cluster
     * @param updatedHpa
     * @param context
     * @return
     * @throws Exception
     */
    public boolean replaceHorizontalPodAutoscaleV2beta1(ClusterVO cluster, String namespace, V2beta1HorizontalPodAutoscaler updatedHpa, ExecutingContextVO context) throws Exception {

        // patch
        V2beta1HorizontalPodAutoscaler result = k8sWorker.replaceHorizontalPodAutoscaleV2beta1(cluster, namespace, updatedHpa.getMetadata().getName(), updatedHpa, false);

        Thread.sleep(100);

        if (result == null) {
            return false;
        }

        return true;
    }

    /**
     * HorizontalPodAutoscaler V2beta1 수정
     *
     * @param cluster
     * @param updatedHpa
     * @param context
     * @return
     * @throws Exception
     */
    public boolean patchHorizontalPodAutoscalerV2beta1(ClusterVO cluster, String namespace, V2beta1HorizontalPodAutoscaler updatedHpa, ExecutingContextVO context) throws Exception {

        // 현재 Hpa 조회
        V2beta1HorizontalPodAutoscaler currentHpa = k8sWorker.getHorizontalPodAutoscalerV2beta1(cluster, namespace, updatedHpa.getMetadata().getName());
        currentHpa.setStatus(null);

        // patchJson 으로 변경
        List<JsonObject> patchBody = k8sPatchSpecFactory.buildPatch(currentHpa, updatedHpa);
        log.debug("########## HpaV2beta1 patchBody JSON: {}", JsonUtils.toGson(patchBody));
        // patch
        V2beta1HorizontalPodAutoscaler result = k8sWorker.patchHorizontalPodAutoscaleV2beta1(cluster, namespace, updatedHpa.getMetadata().getName(), patchBody, false);

        Thread.sleep(100);

        if (result == null) {
            return false;
        }

        return true;
    }

    /**
     * HorizontalPodAutoscaler V2beta1 수정
     *
     * @param cluster
     * @param namespace
     * @param currentHpa
     * @param updatedHpa
     * @param dryRun
     * @param context
     * @return
     * @throws Exception
     */
    public boolean patchHorizontalPodAutoscalerV2beta1(ClusterVO cluster, String namespace, V2beta1HorizontalPodAutoscaler currentHpa, V2beta1HorizontalPodAutoscaler updatedHpa, boolean dryRun, ExecutingContextVO context) throws Exception {

        // set null
        currentHpa.setStatus(null);
        updatedHpa.setStatus(null);

        // patchJson 으로 변경
        List<JsonObject> patchBody = k8sPatchSpecFactory.buildPatch(currentHpa, updatedHpa);
        log.debug("########## HpaV2beta1 patchBody JSON: {}", JsonUtils.toGson(patchBody));
        // patch
        V2beta1HorizontalPodAutoscaler result = k8sWorker.patchHorizontalPodAutoscaleV2beta1(cluster, namespace, updatedHpa.getMetadata().getName(), patchBody, dryRun);

        if (result == null) {
            return false;
        }

        return true;
    }

    /**
     * HorizontalPodAutoscaler V1 수정
     *
     * @param cluster
     * @param updatedHpa
     * @param context
     * @return
     * @throws Exception
     */
    public boolean patchHorizontalPodAutoscalerV1(ClusterVO cluster, String namespace, V1HorizontalPodAutoscaler updatedHpa, ExecutingContextVO context) throws Exception {

        // 현재 Hpa 조회
        V1HorizontalPodAutoscaler currentHpa = k8sWorker.getHorizontalPodAutoscalerV1(cluster, namespace, updatedHpa.getMetadata().getName());
        currentHpa.setStatus(null);

        // patchJson 으로 변경
        List<JsonObject> patchBody = k8sPatchSpecFactory.buildPatch(currentHpa, updatedHpa);
        log.debug("########## HpaV1 patchBody JSON: {}", JsonUtils.toGson(patchBody));
        // patch
        V1HorizontalPodAutoscaler result = k8sWorker.patchHorizontalPodAutoscaleV1(cluster, namespace, updatedHpa.getMetadata().getName(), patchBody, false);

        Thread.sleep(100);

        if (result == null) {
            return false;
        }

        return true;
    }

    /**
     * HorizontalPodAutoscaler V1 수정
     *
     * @param cluster
     * @param updatedHpa
     * @param context
     * @return
     * @throws Exception
     */
    public boolean patchHorizontalPodAutoscalerV1(ClusterVO cluster, String namespace, V1HorizontalPodAutoscaler currentHpa, V1HorizontalPodAutoscaler updatedHpa, boolean dryRun, ExecutingContextVO context) throws Exception {

        // set null
        currentHpa.setStatus(null);
        updatedHpa.setStatus(null);

        // patchJson 으로 변경
        List<JsonObject> patchBody = k8sPatchSpecFactory.buildPatch(currentHpa, updatedHpa);
        log.debug("########## HpaV1 patchBody JSON: {}", JsonUtils.toGson(patchBody));
        // patch
        V1HorizontalPodAutoscaler result = k8sWorker.patchHorizontalPodAutoscaleV1(cluster, namespace, updatedHpa.getMetadata().getName(), patchBody, dryRun);

        if (result == null) {
            return false;
        }

        return true;
    }

    /**
     * K8S Horizontal Pod Autoscalers 삭제 (hpa 정보를 활용하여 K8sApiType 확인 후 해당 Type으로  삭제)
     * @param cluster
     * @param namespace
     * @param name
     * @param hpa
     * @throws Exception
     */
    public void deleteHorizontalPodAutoscaler(ClusterVO cluster, String namespace, String name, HpaGuiVO hpa) throws Exception {

//        K8sApiType k8sApiType = this.getHpaApiType(hpa);
        K8sApiVerKindType hpaType = ResourceUtil.getApiVerKindType(cluster, K8sApiKindType.HORIZONTAL_POD_AUTOSCALER);

        if (hpaType != null) {
            if(K8sApiType.V2 == hpaType.getApiType()) {
                V2HorizontalPodAutoscaler currHpa = k8sWorker.getHorizontalPodAutoscalerV2(cluster, namespace, name);
                if (currHpa != null) {
                    k8sWorker.deleteHorizontalPodAutoscalerV2WithName(cluster, namespace, name);
                }
            }
            else if(K8sApiType.V2BETA2 == hpaType.getApiType()) {
                V2beta2HorizontalPodAutoscaler currHpa = k8sWorker.getHorizontalPodAutoscalerV2beta2(cluster, namespace, name);
                if (currHpa != null) {
                    k8sWorker.deleteHorizontalPodAutoscalerV2beta2WithName(cluster, namespace, name);
                }
            }
            else if (K8sApiType.V2BETA1 == hpaType.getApiType()) {
                V2beta1HorizontalPodAutoscaler currHpa = k8sWorker.getHorizontalPodAutoscalerV2beta1(cluster, namespace, name);
                if (currHpa != null) {
                    k8sWorker.deleteHorizontalPodAutoscalerV2beta1WithName(cluster, namespace, name);
                }
            }
            else if (K8sApiType.V1 == hpaType.getApiType()) {
                V1HorizontalPodAutoscaler currHpa = k8sWorker.getHorizontalPodAutoscalerV1(cluster, namespace, name);
                if (currHpa != null) {
                    k8sWorker.deleteHorizontalPodAutoscalerV1WithName(cluster, namespace, name);
                }
            }
        } else {
            V2HorizontalPodAutoscaler currHpa = k8sWorker.getHorizontalPodAutoscalerV2(cluster, namespace, name);
            if (currHpa != null) {
                k8sWorker.deleteHorizontalPodAutoscalerV2WithName(cluster, namespace, name);
            }
        }

    }

    /**
     * K8S Horizontal Pod Autoscalers 삭제
     *
     * @param cluster
     * @param namespace
     * @param name
     * @return
     * @throws Exception
     */
    public void deleteHorizontalPodAutoscaler(ClusterVO cluster, String namespace, String name) throws Exception {

        /**
         * 2018.08.09 기준 : 하위 호환성을 지원하므로 삭제는 모두 최신 스펙인 V2beta1 Spec 으로 지워도 될 것 같음. (삭제의 경우 V2Beta1과 V1 Spec 동일)
         * 현재 Hpa 정보를 확인할 수 없어 어느 버전으로 삭제하여야 할지 모를 때 사용하고 있음.
         */
        V2HorizontalPodAutoscaler v2HorizontalPodAutoscaler = k8sWorker.getHorizontalPodAutoscalerV2(cluster, namespace, name);
        if (v2HorizontalPodAutoscaler != null) {
            k8sWorker.deleteHorizontalPodAutoscalerV2WithName(cluster, namespace, name);
        }
    }

    /**
     * Spec 내에 Autoscaling을 위한 정보가 올바른지 (Autoscaling 여부) 조회
     * @param hpa
     * @return
     */
    public boolean isAutoscaling(HpaGuiVO hpa) {
        boolean useExtensionMetric = (hpa.getMetrics() != null && hpa.getMetrics().size() > 0);

        if(useExtensionMetric) { // V2 // V2beta1 // V2beta2 Spec
            return true;
        }
        else if ((StringUtils.isNotBlank(hpa.getType()) && KubeConstants.HPA_TYPE_CPU.equalsIgnoreCase(hpa.getType()))){ //V1 Spec
            return true;
        }
        else { // Error
            return false;
        }
    }

//    /**
//     * Horizontal Pod Autoscalers에서 사용할 K8sApiType 조회
//     * @param hpa
//     * @return
//     */
//    public K8sApiType getHpaApiType(HorizontalPodAutoscalerVO hpa) {
//        /*
//        boolean useExtensionMetric = (hpa.getMetrics() != null && hpa.getMetrics().size() > 0); // V2Beta1 Spec 인지 확인
//
//        if((StringUtils.isNotBlank(hpa.getType()) && KubeConstants.HPA_TYPE_METRIC.equalsIgnoreCase(hpa.getType())) && useExtensionMetric) {
//            return K8sApiType.V2BETA1;
//        }
//        else if ((StringUtils.isNotBlank(hpa.getType()) && KubeConstants.HPA_TYPE_CPU.equalsIgnoreCase(hpa.getType()))) { // HPA Type= CPU type이면 V1 Spec으로 판단하고 처리
//            return K8sApiType.V1;
//        }
//        else if(StringUtils.isBlank(hpa.getType()) && !useExtensionMetric) {
//            // Autoscaling하지 않는 타입 (V1의 CPU Spec이 아닌 케이스는 모두 V2Beta1으로 처리)
//            return K8sApiType.V2BETA1;
//        }
//        else {
//            // V1의 CPU Spec이 아닌 케이스는 모두 V2Beta1으로 처리
//            return K8sApiType.V2BETA1;
//        }
//        */
//
//        // V1 Spec이 아니면 모두 상위 버전인 V2Beta1 Spec으로 처리함
//        if ((StringUtils.isNotBlank(hpa.getType()) && KubeConstants.HPA_TYPE_CPU.equalsIgnoreCase(hpa.getType()))) { // HPA Type= CPU type이면 V1 Spec으로 판단하고 처리
//            return K8sApiType.V1;
//        }
//
//        return K8sApiType.V2BETA2;
//    }


    /**
     * K8S ReplicaSet 정보 조회
     *
     * @param clusterSeq
     * @param namespace
     * @param name
     * @param context
     * @return
     * @throws Exception
     */
    public List<K8sReplicaSetVO> getReplicaSets(Integer clusterSeq, String namespace, String name, ExecutingContextVO context) throws Exception {
        try {
            if (clusterSeq != null) {
                IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
                ClusterVO cluster = clusterDao.getCluster(clusterSeq);

                K8sDeploymentVO deployment;
                if (cluster != null) {
                    deployment = this.getDeployment(cluster, namespace, name, context);
                }
                else {
                    throw new CocktailException("cluster not found.", ExceptionType.InvalidParameter);
                }

                String labels = null;
                if (deployment != null) {
                    labels = ResourceUtil.getLabelFilterOfSelector(deployment.getDetail().getSelector());
                }

                return convertReplicaSetDataList(cluster, namespace, null, labels);
            }
            else {
                throw new CocktailException("cluster not found.", ExceptionType.InvalidParameter);
            }
        } catch (CocktailException e) {
            throw new CocktailException("getDeployment fail!!", e, ExceptionType.K8sCocktailCloudInquireFail);
        }
    }

    /**
     * K8S ReplicaSet 정보 조회
     *
     * @param clusterSeq
     * @param namespaceName
     * @return
     * @throws Exception
     */
    public List<K8sReplicaSetVO> getReplicaSetsByCluster(Integer clusterSeq, String namespaceName, ExecutingContextVO context) throws Exception{

        try {
            if(clusterSeq != null){
                IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);

                ClusterVO cluster = clusterDao.getCluster(clusterSeq);

                if(cluster != null){
                    return this.convertReplicaSetDataList(cluster, namespaceName, null, null);
                }else{
                    throw new CocktailException("cluster is null.", ExceptionType.InvalidParameter);
                }
            }else{
                throw new CocktailException("clusterSeq is null.", ExceptionType.InvalidParameter);
            }
        } catch (Exception e) {
            throw new CocktailException("getReplicaSetsByCluster fail!!", e, ExceptionType.K8sCocktailCloudInquireFail);
        }
    }

    /**
     * K8S ReplicaSet 정보 조회 후,
     *
     * - V1BETA1
     * V1beta1ReplicaSet -> K8sReplicaSetVO 변환
     *
     * - V1BETA2
     * V1beta2ReplicaSet -> K8sReplicaSetVO 변환
     *
     * - V1
     * V1ReplicaSet -> K8sReplicaSetVO 변환
     *
     * @param cluster
     * @param label
     * @return
     * @throws Exception
     */
    public List<K8sReplicaSetVO> convertReplicaSetDataList(ClusterVO cluster, String namespace, String field, String label) throws Exception {

        List<K8sReplicaSetVO> replicaSets = new ArrayList<>();

        if (cluster != null) {
            K8sApiVerKindType apiVerKindType = ResourceUtil.getApiVerKindType(cluster, K8sApiKindType.REPLICA_SET);

            if (apiVerKindType != null) {
                // joda.datetime Serialization
                JSON k8sJson = new JSON();

                if(apiVerKindType.getGroupType() == K8sApiGroupType.EXTENSIONS && apiVerKindType.getApiType() == K8sApiType.V1BETA1){
                    // ReplicaSet 조회
                    List<V1beta1ReplicaSet> v1beta1ReplicaSets = k8sWorker.getReplicaSetsV1beta1(cluster, namespace, field, label);

                    // Replica Set
                    if(CollectionUtils.isNotEmpty(v1beta1ReplicaSets)){
                        for(V1beta1ReplicaSet v1beta1ReplicaSetRow : v1beta1ReplicaSets){
                            replicaSets.add(this.genReplicaSetV1beta1(v1beta1ReplicaSetRow, cluster, label, k8sJson));
                        }
                    }
                }else if(apiVerKindType.getGroupType() == K8sApiGroupType.APPS && apiVerKindType.getApiType() == K8sApiType.V1BETA2){
                    // ReplicaSet 조회
                    List<V1beta2ReplicaSet> v1beta2ReplicaSets = k8sWorker.getReplicaSetsV1beta2(cluster, namespace, field, label);

                    // Replica Set
                    if(CollectionUtils.isNotEmpty(v1beta2ReplicaSets)){
                        for(V1beta2ReplicaSet v1beta2ReplicaSetRow : v1beta2ReplicaSets){
                            replicaSets.add(this.genReplicaSetV1beta2(v1beta2ReplicaSetRow, cluster, label, k8sJson));
                        }
                    }
                }
                // K8sApiType.V1 == apiType
                else {
                    // ReplicaSet 조회
                    List<V1ReplicaSet> v1ReplicaSets = k8sWorker.getReplicaSetsV1(cluster, namespace, field, label);

                    // Replica Set
                    if(CollectionUtils.isNotEmpty(v1ReplicaSets)){
                        for(V1ReplicaSet replicaSetRow : v1ReplicaSets){
                            replicaSets.add(this.genReplicaSetV1(replicaSetRow, cluster, label, k8sJson));
                        }
                    }
                }
            } else {
                log.warn("convertReplicaSetDataList cluster {} version {} not supported.", cluster.getClusterName(), cluster.getK8sVersion());
            }
        } else {
            log.warn("convertReplicaSetDataList - cluster is null.");
        }

        return replicaSets;
    }

    public K8sReplicaSetVO genReplicaSetV1beta1(V1beta1ReplicaSet replicaSet, ClusterVO cluster, String label, JSON k8sJson) throws Exception{
        if (k8sJson == null) {
            k8sJson = new JSON();
        }
        Map<String, Integer> podsMap = new HashMap<>();
        Map<String, Integer> statusMap = new HashMap<>();
        K8sReplicaSetVO k8sReplicaSet = new K8sReplicaSetVO();
        k8sReplicaSet.setLabel(label);
        k8sReplicaSet.setNamespace(replicaSet.getMetadata().getNamespace());
        k8sReplicaSet.setName(replicaSet.getMetadata().getName());
        k8sReplicaSet.setLabels(replicaSet.getMetadata().getLabels());
        k8sReplicaSet.setReadyPodCnt(replicaSet.getStatus().getReadyReplicas() != null ? replicaSet.getStatus().getReadyReplicas() : 0);
        k8sReplicaSet.setDesiredPodCnt(replicaSet.getStatus().getReplicas() != null ? replicaSet.getStatus().getReplicas() : 0);
        k8sReplicaSet.setCreationTimestamp(replicaSet.getMetadata().getCreationTimestamp());
        k8sReplicaSet.setImages(replicaSet.getSpec().getTemplate().getSpec().getContainers().stream().map(c -> c.getImage()).collect(Collectors.toList()));
        k8sReplicaSet.setDeployment(k8sJson.serialize(replicaSet));
        k8sReplicaSet.setDeploymentYaml(Yaml.getSnakeYaml().dumpAsMap(replicaSet));

        K8sReplicaSetDetailVO replicaSetDetail = new K8sReplicaSetDetailVO();
        replicaSetDetail.setName(replicaSet.getMetadata().getName());
        replicaSetDetail.setNamespace(replicaSet.getMetadata().getNamespace());
        replicaSetDetail.setLabels(replicaSet.getMetadata().getLabels());
        replicaSetDetail.setAnnotations(replicaSet.getMetadata().getAnnotations());
        replicaSetDetail.setCreationTime(replicaSet.getMetadata().getCreationTimestamp());
        String selectorJson = k8sJson.serialize(replicaSet.getSpec().getSelector());
        replicaSetDetail.setSelector(k8sJson.getGson().fromJson(selectorJson, K8sLabelSelectorVO.class));
        replicaSetDetail.setImages(replicaSet.getSpec().getTemplate().getSpec().getContainers().stream().map(c -> c.getImage()).collect(Collectors.toList()));
        // ownerReference
        if (CollectionUtils.isNotEmpty(replicaSet.getMetadata().getOwnerReferences())) {
            replicaSetDetail.setOwnerReferences(ResourceUtil.setOwnerReference(replicaSet.getMetadata().getOwnerReferences()));
        }
        if(replicaSet.getStatus() != null){
            if(replicaSet.getSpec().getReplicas().intValue() == 0){
                podsMap.put("running", 0);
            }else{
                // 정상 상태
                String podTemplateHashLabel = String.format("%s=%s", KubeConstants.LABELS_POD_TEMPLATE_HASH_KEY, replicaSet.getMetadata().getLabels().get(KubeConstants.LABELS_POD_TEMPLATE_HASH_KEY));
//                if (StringUtils.isNotBlank(label)) {
//                    podTemplateHashLabel = String.format("%s,%s", label, podTemplateHashLabel);
//                }
                List<V1Pod> v1Pods = k8sWorker.getPodV1(cluster, replicaSet.getMetadata().getNamespace(), null, null, podTemplateHashLabel);
                int running = 0;
                int pending = 0;
                int succeeded = 0;
                int failed = 0;

                if(CollectionUtils.isNotEmpty(v1Pods)){
                    for(V1Pod v1Pod : v1Pods){
                        if(StringUtils.equalsIgnoreCase("running", v1Pod.getStatus().getPhase())){
                            running++;
                        }else if(StringUtils.equalsIgnoreCase("pending", v1Pod.getStatus().getPhase())){
                            pending++;
                        }else if(StringUtils.equalsIgnoreCase("succeeded", v1Pod.getStatus().getPhase())){
                            succeeded++;
                        }else if(StringUtils.equalsIgnoreCase("failed", v1Pod.getStatus().getPhase())){
                            failed++;
                        }
                    }

                    if(v1Pods.size() == running){
                        podsMap.put("running", running);
                    }else{
                        podsMap.put("created", replicaSet.getStatus().getReplicas());
                        podsMap.put("desired", replicaSet.getSpec().getReplicas());

                        if(running > 0){
                            statusMap.put("running", running);
                        }
                        if(pending > 0){
                            statusMap.put("pending", pending);
                        }
                        if(succeeded > 0){
                            statusMap.put("succeeded", succeeded);
                        }
                        if(failed > 0){
                            statusMap.put("failed", failed);
                        }
                    }
                }
            }
        }
        replicaSetDetail.setPods(podsMap);
        replicaSetDetail.setPodsStatus(statusMap);

        // podTemplate
        replicaSetDetail.setPodTemplate(this.setPodTemplateSpec(replicaSet.getSpec().getTemplate(), k8sJson));

        k8sReplicaSet.setDetail(replicaSetDetail);

        return k8sReplicaSet;
    }

    public K8sReplicaSetVO genReplicaSetV1beta2(V1beta2ReplicaSet replicaSet, ClusterVO cluster, String label, JSON k8sJson) throws Exception{
        if (k8sJson == null) {
            k8sJson = new JSON();
        }
        Map<String, Integer> podsMap = new HashMap<>();
        Map<String, Integer> statusMap = new HashMap<>();
        K8sReplicaSetVO k8sReplicaSet = new K8sReplicaSetVO();
        k8sReplicaSet.setLabel(label);
        k8sReplicaSet.setNamespace(replicaSet.getMetadata().getNamespace());
        k8sReplicaSet.setName(replicaSet.getMetadata().getName());
        k8sReplicaSet.setLabels(replicaSet.getMetadata().getLabels());
        k8sReplicaSet.setReadyPodCnt(replicaSet.getStatus().getReadyReplicas() != null ? replicaSet.getStatus().getReadyReplicas() : 0);
        k8sReplicaSet.setDesiredPodCnt(replicaSet.getStatus().getReplicas() != null ? replicaSet.getStatus().getReplicas() : 0);
        k8sReplicaSet.setCreationTimestamp(replicaSet.getMetadata().getCreationTimestamp());
        k8sReplicaSet.setImages(replicaSet.getSpec().getTemplate().getSpec().getContainers().stream().map(c -> c.getImage()).collect(Collectors.toList()));
        k8sReplicaSet.setDeployment(k8sJson.serialize(replicaSet));
        k8sReplicaSet.setDeploymentYaml(Yaml.getSnakeYaml().dumpAsMap(replicaSet));

        K8sReplicaSetDetailVO replicaSetDetail = new K8sReplicaSetDetailVO();
        replicaSetDetail.setName(replicaSet.getMetadata().getName());
        replicaSetDetail.setNamespace(replicaSet.getMetadata().getNamespace());
        replicaSetDetail.setLabels(replicaSet.getMetadata().getLabels());
        replicaSetDetail.setAnnotations(replicaSet.getMetadata().getAnnotations());
        replicaSetDetail.setCreationTime(replicaSet.getMetadata().getCreationTimestamp());
        String selectorJson = k8sJson.serialize(replicaSet.getSpec().getSelector());
        replicaSetDetail.setSelector(k8sJson.getGson().fromJson(selectorJson, K8sLabelSelectorVO.class));
        replicaSetDetail.setImages(replicaSet.getSpec().getTemplate().getSpec().getContainers().stream().map(c -> c.getImage()).collect(Collectors.toList()));
        // ownerReference
        if (CollectionUtils.isNotEmpty(replicaSet.getMetadata().getOwnerReferences())) {
            replicaSetDetail.setOwnerReferences(ResourceUtil.setOwnerReference(replicaSet.getMetadata().getOwnerReferences()));
        }
        if(replicaSet.getStatus() != null){
            if(replicaSet.getSpec().getReplicas().intValue() == 0){
                podsMap.put("running", 0);
            }else{
                // 정상 상태
                String podTemplateHashLabel = String.format("%s=%s", KubeConstants.LABELS_POD_TEMPLATE_HASH_KEY, replicaSet.getMetadata().getLabels().get(KubeConstants.LABELS_POD_TEMPLATE_HASH_KEY));
//                if (StringUtils.isNotBlank(label)) {
//                    podTemplateHashLabel = String.format("%s,%s", label, podTemplateHashLabel);
//                }
                List<V1Pod> v1Pods = k8sWorker.getPodV1(cluster, replicaSet.getMetadata().getNamespace(), null, null, podTemplateHashLabel);
                int running = 0;
                int pending = 0;
                int succeeded = 0;
                int failed = 0;

                if(CollectionUtils.isNotEmpty(v1Pods)){
                    for(V1Pod v1Pod : v1Pods){
                        if(StringUtils.equalsIgnoreCase("running", v1Pod.getStatus().getPhase())){
                            running++;
                        }else if(StringUtils.equalsIgnoreCase("pending", v1Pod.getStatus().getPhase())){
                            pending++;
                        }else if(StringUtils.equalsIgnoreCase("succeeded", v1Pod.getStatus().getPhase())){
                            succeeded++;
                        }else if(StringUtils.equalsIgnoreCase("failed", v1Pod.getStatus().getPhase())){
                            failed++;
                        }
                    }

                    if(v1Pods.size() == running){
                        podsMap.put("running", running);
                    }else{
                        podsMap.put("created", replicaSet.getStatus().getReplicas());
                        podsMap.put("desired", replicaSet.getSpec().getReplicas());

                        if(running > 0){
                            statusMap.put("running", running);
                        }
                        if(pending > 0){
                            statusMap.put("pending", pending);
                        }
                        if(succeeded > 0){
                            statusMap.put("succeeded", succeeded);
                        }
                        if(failed > 0){
                            statusMap.put("failed", failed);
                        }
                    }
                }
            }
        }
        replicaSetDetail.setPods(podsMap);
        replicaSetDetail.setPodsStatus(statusMap);

        // podTemplate
        replicaSetDetail.setPodTemplate(this.setPodTemplateSpec(replicaSet.getSpec().getTemplate(), k8sJson));

        k8sReplicaSet.setDetail(replicaSetDetail);

        return k8sReplicaSet;
    }

    public K8sReplicaSetVO genReplicaSetV1(V1ReplicaSet replicaSet, ClusterVO cluster, String label, JSON k8sJson) throws Exception{
        if (k8sJson == null) {
            k8sJson = new JSON();
        }
        Map<String, Integer> podsMap = new HashMap<>();
        Map<String, Integer> statusMap = new HashMap<>();
        K8sReplicaSetVO k8sReplicaSet = new K8sReplicaSetVO();
        k8sReplicaSet.setLabel(label);
        k8sReplicaSet.setNamespace(replicaSet.getMetadata().getNamespace());
        k8sReplicaSet.setName(replicaSet.getMetadata().getName());
        k8sReplicaSet.setLabels(replicaSet.getMetadata().getLabels());
        k8sReplicaSet.setReadyPodCnt(replicaSet.getStatus().getReadyReplicas() != null ? replicaSet.getStatus().getReadyReplicas() : 0);
        k8sReplicaSet.setDesiredPodCnt(replicaSet.getStatus().getReplicas() != null ? replicaSet.getStatus().getReplicas() : 0);
        k8sReplicaSet.setCreationTimestamp(replicaSet.getMetadata().getCreationTimestamp());
        k8sReplicaSet.setImages(replicaSet.getSpec().getTemplate().getSpec().getContainers().stream().map(c -> c.getImage()).collect(Collectors.toList()));
        k8sReplicaSet.setDeployment(k8sJson.serialize(replicaSet));
        k8sReplicaSet.setDeploymentYaml(Yaml.getSnakeYaml().dumpAsMap(replicaSet));

        K8sReplicaSetDetailVO replicaSetDetail = new K8sReplicaSetDetailVO();
        replicaSetDetail.setName(replicaSet.getMetadata().getName());
        replicaSetDetail.setNamespace(replicaSet.getMetadata().getNamespace());
        replicaSetDetail.setLabels(replicaSet.getMetadata().getLabels());
        replicaSetDetail.setAnnotations(replicaSet.getMetadata().getAnnotations());
        replicaSetDetail.setCreationTime(replicaSet.getMetadata().getCreationTimestamp());
        String selectorJson = k8sJson.serialize(replicaSet.getSpec().getSelector());
        replicaSetDetail.setSelector(k8sJson.getGson().fromJson(selectorJson, K8sLabelSelectorVO.class));
        replicaSetDetail.setImages(replicaSet.getSpec().getTemplate().getSpec().getContainers().stream().map(c -> c.getImage()).collect(Collectors.toList()));
        // ownerReference
        if (CollectionUtils.isNotEmpty(replicaSet.getMetadata().getOwnerReferences())) {
            replicaSetDetail.setOwnerReferences(ResourceUtil.setOwnerReference(replicaSet.getMetadata().getOwnerReferences()));
        }
        if(replicaSet.getStatus() != null){
            if(replicaSet.getSpec().getReplicas().intValue() == 0){
                podsMap.put("running", 0);
            }else{
                // 정상 상태
                String podTemplateHashLabel = String.format("%s=%s", KubeConstants.LABELS_POD_TEMPLATE_HASH_KEY, replicaSet.getMetadata().getLabels().get(KubeConstants.LABELS_POD_TEMPLATE_HASH_KEY));
//                if (StringUtils.isNotBlank(label)) {
//                    podTemplateHashLabel = String.format("%s,%s", label, podTemplateHashLabel);
//                }
                List<V1Pod> v1Pods = k8sWorker.getPodV1(cluster, replicaSet.getMetadata().getNamespace(), null, null, podTemplateHashLabel);
                int running = 0;
                int pending = 0;
                int succeeded = 0;
                int failed = 0;

                if(CollectionUtils.isNotEmpty(v1Pods)){
                    for(V1Pod v1Pod : v1Pods){
                        if(StringUtils.equalsIgnoreCase("running", v1Pod.getStatus().getPhase())){
                            running++;
                        }else if(StringUtils.equalsIgnoreCase("pending", v1Pod.getStatus().getPhase())){
                            pending++;
                        }else if(StringUtils.equalsIgnoreCase("succeeded", v1Pod.getStatus().getPhase())){
                            succeeded++;
                        }else if(StringUtils.equalsIgnoreCase("failed", v1Pod.getStatus().getPhase())){
                            failed++;
                        }
                    }

                    if(v1Pods.size() == running){
                        podsMap.put("running", running);
                    }else{
                        podsMap.put("created", replicaSet.getStatus().getReplicas());
                        podsMap.put("desired", replicaSet.getSpec().getReplicas());

                        if(running > 0){
                            statusMap.put("running", running);
                        }
                        if(pending > 0){
                            statusMap.put("pending", pending);
                        }
                        if(succeeded > 0){
                            statusMap.put("succeeded", succeeded);
                        }
                        if(failed > 0){
                            statusMap.put("failed", failed);
                        }
                    }
                }
            }
        }
        replicaSetDetail.setPods(podsMap);
        replicaSetDetail.setPodsStatus(statusMap);

        // podTemplate
        replicaSetDetail.setPodTemplate(this.setPodTemplateSpec(replicaSet.getSpec().getTemplate(), k8sJson));

        k8sReplicaSet.setDetail(replicaSetDetail);

        return k8sReplicaSet;
    }

    /**
     *
     * @param cluster
     * @param namespace
     * @param fieldSelector
     * @param labelSelector
     * @param useDeployments
     * @param useStatefulSets
     * @param useDaemonSets
     * @param useJobs
     * @param useCronJobs
     * @param useServices
     * @param useHpas
     * @return Map<String, Map<String, Object>> - Map<K8sApiKindType.getValue(), Map<ResourceName, Object>>
     * @throws Exception
     */
    public Map<String, Map<String, ? extends Object>> getWorkloadResource(
            ClusterVO cluster, String namespace, String fieldSelector, String labelSelector,
            Boolean useDeployments,
            Boolean useStatefulSets,
            Boolean useDaemonSets,
            Boolean useJobs,
            Boolean useCronJobs,
            Boolean useServices,
            Boolean useHpas
    ) throws Exception {
        return this.getWorkloadResource(cluster, null, namespace, fieldSelector, labelSelector,
                useDeployments, useStatefulSets, useDaemonSets, useJobs, useCronJobs, useServices, useHpas);
    }

    public Map<String, Map<String, ? extends Object>> getWorkloadResource(
            ClusterVO cluster, Integer serviceSeq, String namespace, String fieldSelector, String labelSelector,
            Boolean useDeployments,
            Boolean useStatefulSets,
            Boolean useDaemonSets,
            Boolean useJobs,
            Boolean useCronJobs,
            Boolean useServices,
            Boolean useHpas
    ) throws Exception {
        return this.getWorkloadResource(cluster, serviceSeq, namespace, fieldSelector, labelSelector,
                useDeployments, useStatefulSets, useDaemonSets, useJobs, useCronJobs, useServices, useHpas, Boolean.FALSE);
    }

    public Map<String, Map<String, ? extends Object>> getWorkloadResource(
            ClusterVO cluster, Integer serviceSeq, String namespace, String fieldSelector, String labelSelector,
            Boolean useDeployments,
            Boolean useStatefulSets,
            Boolean useDaemonSets,
            Boolean useJobs,
            Boolean useCronJobs,
            Boolean useServices,
            Boolean useHpas,
            Boolean usePods
    ) throws Exception {
        Map<String, Map<String, ? extends Object>> k8sResourceMap = Maps.newHashMap();

        // 각 resource 별 flag 값으로 조회여부 셋팅 - null 이면 조회하지 않음
        List<V1Deployment> deployments = BooleanUtils.toBoolean(useDeployments) ? Lists.newArrayList() : null;
        List<V1StatefulSet> statefulSets = BooleanUtils.toBoolean(useStatefulSets) ? Lists.newArrayList() : null;
        List<V1DaemonSet> daemonSets = BooleanUtils.toBoolean(useDaemonSets) ? Lists.newArrayList() : null;
        List<V1Job> jobs = BooleanUtils.toBoolean(useJobs) ? Lists.newArrayList() : null;
        List<V1CronJob> cronJobsV1 = null;
        List<V1beta1CronJob> cronJobsV1beta1 = null;
        List<V1Service> services = BooleanUtils.toBoolean(useServices) ? Lists.newArrayList() : null;
        List<V2HorizontalPodAutoscaler> hpasV2 = null;
        List<V2beta2HorizontalPodAutoscaler> hpasV2beta2 = null;
        List<V2beta1HorizontalPodAutoscaler> hpasV2beta1 = null;
        List<V1Pod> pods = BooleanUtils.toBoolean(usePods) ? Lists.newArrayList() : null;

        // k8s resource 조회
        // - 대상 resource의 List 객체가 null 이면 조회하지 않음

        // hpa
        K8sApiVerKindType hpaApiVerKindType = ResourceUtil.getApiVerKindType(cluster, K8sApiKindType.HORIZONTAL_POD_AUTOSCALER);
        if (hpaApiVerKindType != null) {
            if (hpaApiVerKindType.getApiType() == K8sApiType.V2BETA1) {
                hpasV2beta1 = BooleanUtils.toBoolean(useHpas) ? Lists.newArrayList() : null;
            } else if (hpaApiVerKindType.getApiType() == K8sApiType.V2BETA2) {
                hpasV2beta2 = BooleanUtils.toBoolean(useHpas) ? Lists.newArrayList() : null;
            } else if (hpaApiVerKindType.getApiType() == K8sApiType.V2) {
                hpasV2 = BooleanUtils.toBoolean(useHpas) ? Lists.newArrayList() : null;
            }
        }
        // cron job
        K8sApiVerKindType cronJobApiVerKindType = ResourceUtil.getApiVerKindType(cluster, K8sApiKindType.CRON_JOB);
        if (cronJobApiVerKindType != null) {
            if (cronJobApiVerKindType.getApiType() == K8sApiType.V1BETA1) {
                cronJobsV1beta1 = BooleanUtils.toBoolean(useCronJobs) ? Lists.newArrayList() : null;
            } else {
                cronJobsV1 = BooleanUtils.toBoolean(useCronJobs) ? Lists.newArrayList() : null;
            }
        }


        this.getWorkloadResourceWithHpa(
                cluster, namespace, fieldSelector, labelSelector,
                BooleanUtils.toBoolean(useDeployments) ? deployments : null,
                null,
                statefulSets,
                daemonSets,
                jobs,
                cronJobsV1,
                cronJobsV1beta1,
                pods,
                services,
                hpasV2,
                hpasV2beta2,
                hpasV2beta1
        );

        Set<String> appmapNamespaces = k8sResourceService.getServicemapNamespacesByService(serviceSeq);

        // 조회된 결과로 list -> map으로 변경 후, 각 resource 명으로 k8sResourceMap에 셋팅
        if (BooleanUtils.toBoolean(useDeployments) && CollectionUtils.isNotEmpty(deployments)) {
            Map<String, V1Deployment> deploymentMap = Maps.newHashMap();
            if (serviceSeq != null) {
                this.convertDeploymentToMap(deployments, deploymentMap, serviceSeq, appmapNamespaces);
            } else {
                this.convertDeploymentToMap(deployments, deploymentMap);
            }
            k8sResourceMap.put(K8sApiKindType.DEPLOYMENT.getValue(), deploymentMap);
        }
        if (BooleanUtils.toBoolean(useStatefulSets) && CollectionUtils.isNotEmpty(statefulSets)) {
            Map<String, V1StatefulSet> statefulSetMap = Maps.newHashMap();
            if (serviceSeq != null) {
                this.convertStatefulSetToMap(statefulSets, statefulSetMap, serviceSeq, appmapNamespaces);
            } else {
                this.convertStatefulSetToMap(statefulSets, statefulSetMap);
            }
            k8sResourceMap.put(K8sApiKindType.STATEFUL_SET.getValue(), statefulSetMap);
        }
        if (BooleanUtils.toBoolean(useDaemonSets) && CollectionUtils.isNotEmpty(daemonSets)) {
            Map<String, V1DaemonSet> daemonSetMap = Maps.newHashMap();
            if (serviceSeq != null) {
                this.convertDaemonSetToMap(daemonSets, daemonSetMap, serviceSeq, appmapNamespaces);
            } else {
                this.convertDaemonSetToMap(daemonSets, daemonSetMap);
            }
            k8sResourceMap.put(K8sApiKindType.DAEMON_SET.getValue(), daemonSetMap);
        }
        if (BooleanUtils.toBoolean(useJobs) && CollectionUtils.isNotEmpty(jobs)) {
            Map<String, V1Job> jobMap = Maps.newHashMap();
            if (serviceSeq != null) {
                this.convertJobToMap(jobs, jobMap, serviceSeq, appmapNamespaces);
            } else {
                this.convertJobToMap(jobs, jobMap);
            }
            k8sResourceMap.put(K8sApiKindType.JOB.getValue(), jobMap);
        }
        if (BooleanUtils.toBoolean(useCronJobs)) {
            if (CollectionUtils.isNotEmpty(cronJobsV1beta1)) {
                Map<String, V1beta1CronJob> cronJobMap = Maps.newHashMap();
                if (serviceSeq != null) {
                    this.convertCronJobToMapV1beta1(cronJobsV1beta1, cronJobMap, serviceSeq, appmapNamespaces);
                } else {
                    this.convertCronJobToMapV1beta1(cronJobsV1beta1, cronJobMap);
                }
                k8sResourceMap.put(K8sApiKindType.CRON_JOB.getValue(), cronJobMap);
            } else if (CollectionUtils.isNotEmpty(cronJobsV1)) {
                Map<String, V1CronJob> cronJobMap = Maps.newHashMap();
                if (serviceSeq != null) {
                    this.convertCronJobToMapV1(cronJobsV1, cronJobMap, serviceSeq, appmapNamespaces);
                } else {
                    this.convertCronJobToMapV1(cronJobsV1, cronJobMap);
                }
                k8sResourceMap.put(K8sApiKindType.CRON_JOB.getValue(), cronJobMap);
            }
        }
        if (BooleanUtils.toBoolean(useServices) && CollectionUtils.isNotEmpty(services)) {
            Map<String, V1Service> serviceMap = Maps.newHashMap();
            this.convertServiceToMap(services, serviceMap);
            k8sResourceMap.put(K8sApiKindType.SERVICE.getValue(), serviceMap);
        }
        if (BooleanUtils.toBoolean(useHpas)) {
            if (CollectionUtils.isNotEmpty(hpasV2)) {
                Map<String, V2HorizontalPodAutoscaler> hpaMap = Maps.newHashMap();
                this.convertHpaToMapV2(hpasV2, hpaMap);
                k8sResourceMap.put(K8sApiKindType.HORIZONTAL_POD_AUTOSCALER.getValue(), hpaMap);
            } else if (CollectionUtils.isNotEmpty(hpasV2beta2)) {
                Map<String, V2beta2HorizontalPodAutoscaler> hpaMap = Maps.newHashMap();
                this.convertHpaToMapV2beta2(hpasV2beta2, hpaMap);
                k8sResourceMap.put(K8sApiKindType.HORIZONTAL_POD_AUTOSCALER.getValue(), hpaMap);
            } else if (CollectionUtils.isNotEmpty(hpasV2beta1)) {
                Map<String, V2beta1HorizontalPodAutoscaler> hpaMap = Maps.newHashMap();
                this.convertHpaToMapV2beta1(hpasV2beta1, hpaMap);
                k8sResourceMap.put(K8sApiKindType.HORIZONTAL_POD_AUTOSCALER.getValue(), hpaMap);
            }
        }
        if (BooleanUtils.toBoolean(usePods) && CollectionUtils.isNotEmpty(pods)) {
            Map<String, V1Pod> podMap = Maps.newHashMap();
            if (serviceSeq != null) {
                this.convertPodToMap(pods, podMap, serviceSeq, appmapNamespaces);
            } else {
                this.convertPodToMap(pods, podMap);
            }
            k8sResourceMap.put(K8sApiKindType.POD.getValue(), podMap);
        }

        return k8sResourceMap;
    }

    public void getWorkloadResource(
            ClusterVO cluster, String namespace, String fieldSelector, String labelSelector,
            List<V1Deployment> deployments,
            List<V1ReplicaSet> replicaSets,
            List<V1StatefulSet> statefulSets,
            List<V1DaemonSet> daemonSets,
            List<V1Job> jobs,
            List<V1CronJob> cronJobsV1,
            List<V1beta1CronJob> cronJobsV1beta1,
            List<V1Pod> pods,
            List<V1Service> services
    ) throws Exception {
        // Deployment
        if (deployments != null) {
            deployments.addAll(k8sWorker.getDeploymentsV1(cluster, namespace, fieldSelector, labelSelector));
        }
        // replicaSets
        if (replicaSets != null) {
            replicaSets.addAll(k8sWorker.getReplicaSetsV1(cluster, namespace, null, null));
        }
        // StatefulSet
        if (statefulSets != null) {
            statefulSets.addAll(k8sWorker.getStatefulSetsV1(cluster, namespace, fieldSelector, labelSelector));
        }
        // DaemonSet
        if (daemonSets != null) {
            daemonSets.addAll(k8sWorker.getDaemonSetsV1(cluster, namespace, fieldSelector, labelSelector));
        }
        // Job
        if (jobs != null) {
            jobs.addAll(k8sWorker.getJobsV1(cluster, namespace, fieldSelector, labelSelector));
        }
        // CronJob
        K8sApiVerKindType apiVerKindType = ResourceUtil.getApiVerKindType(cluster, K8sApiKindType.CRON_JOB);
        if (apiVerKindType != null) {
            if (apiVerKindType.getApiType() == K8sApiType.V1 && cronJobsV1 != null) {
                cronJobsV1.addAll(k8sWorker.getCronJobsV1(cluster, namespace, fieldSelector, labelSelector));
            }
            if (apiVerKindType.getApiType() == K8sApiType.V1BETA1 && cronJobsV1beta1 != null) {
                cronJobsV1beta1.addAll(k8sWorker.getCronJobsV1beta1(cluster, namespace, fieldSelector, labelSelector));
            }
        }
        // Pod
        if (pods != null) {
            if (StringUtils.isBlank(namespace)) {
                pods.addAll(k8sWorker.getPodV1AllNamespace(cluster, null, null));
            } else {
                pods.addAll(k8sWorker.getPodV1WithLabel(cluster, namespace, null, null));
            }
        }
        // Service
        if (services != null) {
            services.addAll(k8sWorker.getServicesV1(cluster, namespace, null, null));
        }
    }

    public void getWorkloadResourceWithHpa(
            ClusterVO cluster, String namespace, String fieldSelector, String labelSelector,
            List<V1Deployment> deployments,
            List<V1ReplicaSet> replicaSets,
            List<V1StatefulSet> statefulSets,
            List<V1DaemonSet> daemonSets,
            List<V1Job> jobs,
            List<V1CronJob> cronJobsV1,
            List<V1beta1CronJob> cronJobsV1beta1,
            List<V1Pod> pods,
            List<V1Service> services,
            List<V2HorizontalPodAutoscaler> hpasV2,
            List<V2beta2HorizontalPodAutoscaler> hpasV2beta2,
            List<V2beta1HorizontalPodAutoscaler> hpasV2beta1
    ) throws Exception {
        this.getWorkloadResource(
                cluster, namespace, fieldSelector, labelSelector,
                deployments,
                replicaSets,
                statefulSets,
                daemonSets,
                jobs,
                cronJobsV1,
                cronJobsV1beta1,
                pods,
                services
        );

        // hpas
        K8sApiVerKindType apiVerKindType = ResourceUtil.getApiVerKindType(cluster, K8sApiKindType.HORIZONTAL_POD_AUTOSCALER);
        if (apiVerKindType != null) {
            if (apiVerKindType.getApiType() == K8sApiType.V2 && hpasV2 != null) {
                hpasV2.addAll(k8sWorker.getHorizontalPodAutoscalersV2(cluster, namespace, null, null));
            }
            if (apiVerKindType.getApiType() == K8sApiType.V2BETA2 && hpasV2beta2 != null) {
                hpasV2beta2.addAll(k8sWorker.getHorizontalPodAutoscalersV2beta2(cluster, namespace, null, null));
            }
            if (apiVerKindType.getApiType() == K8sApiType.V2BETA1 && hpasV2beta1 != null) {
                hpasV2beta1.addAll(k8sWorker.getHorizontalPodAutoscalersV2beta1(cluster, namespace, null, null));
            }
        }

    }

    public Map<String, V1Deployment> convertDeploymentToMap(List<V1Deployment> deployments, Map<String, V1Deployment> deploymentMap){
        return this.convertDeploymentToMap(deployments, deploymentMap, null, null);
    }

    public Map<String, V1Deployment> convertDeploymentToMap(List<V1Deployment> deployments, Map<String, V1Deployment> deploymentMap, Integer serviceSeq, Set<String> appmapNamespaces){
        if(CollectionUtils.isNotEmpty(deployments)){
            for(V1Deployment deploymentRow : deployments){
                if (serviceSeq != null) {
                    if ( !(CollectionUtils.isNotEmpty(appmapNamespaces) && appmapNamespaces.contains(deploymentRow.getMetadata().getNamespace())) ) {
                        continue;
                    }
                }

                deploymentMap.put(deploymentRow.getMetadata().getName(), deploymentRow);
            }
        }

        return deploymentMap;
    }

    public Map<String, V1StatefulSet> convertStatefulSetToMap(List<V1StatefulSet> statefulSets, Map<String, V1StatefulSet> statefulSetMap){
        return this.convertStatefulSetToMap(statefulSets, statefulSetMap, null, null);
    }

    public Map<String, V1StatefulSet> convertStatefulSetToMap(List<V1StatefulSet> statefulSets, Map<String, V1StatefulSet> statefulSetMap, Integer serviceSeq, Set<String> appmapNamespaces){
        if(CollectionUtils.isNotEmpty(statefulSets)){
            for(V1StatefulSet statefulSetRow : statefulSets){
                if (serviceSeq != null) {
                    if ( !(CollectionUtils.isNotEmpty(appmapNamespaces) && appmapNamespaces.contains(statefulSetRow.getMetadata().getNamespace())) ) {
                        continue;
                    }
                }

                statefulSetMap.put(statefulSetRow.getMetadata().getName(), statefulSetRow);
            }
        }

        return statefulSetMap;
    }

    public Map<String, V1DaemonSet> convertDaemonSetToMap(List<V1DaemonSet> daemonSets, Map<String, V1DaemonSet> daemonSetMap){
        return this.convertDaemonSetToMap(daemonSets, daemonSetMap, null, null);
    }

    public Map<String, V1DaemonSet> convertDaemonSetToMap(List<V1DaemonSet> daemonSets, Map<String, V1DaemonSet> daemonSetMap, Integer serviceSeq, Set<String> appmapNamespaces){
        if(CollectionUtils.isNotEmpty(daemonSets)){
            for(V1DaemonSet daemonSetRow : daemonSets){
                if (serviceSeq != null) {
                    if ( !(CollectionUtils.isNotEmpty(appmapNamespaces) && appmapNamespaces.contains(daemonSetRow.getMetadata().getNamespace())) ) {
                        continue;
                    }
                }

                daemonSetMap.put(daemonSetRow.getMetadata().getName(), daemonSetRow);
            }
        }

        return daemonSetMap;
    }

    public Map<String, V1Job> convertJobToMap(List<V1Job> Jobs, Map<String, V1Job> jobMap){
        return this.convertJobToMap(Jobs, jobMap, null, null);
    }

    public Map<String, V1Job> convertJobToMap(List<V1Job> Jobs, Map<String, V1Job> jobMap, Integer serviceSeq, Set<String> appmapNamespaces){
        if(CollectionUtils.isNotEmpty(Jobs)){
            for(V1Job jobRow : Jobs){
                if (serviceSeq != null) {
                    if ( !(CollectionUtils.isNotEmpty(appmapNamespaces) && appmapNamespaces.contains(jobRow.getMetadata().getNamespace())) ) {
                        continue;
                    }
                }

                jobMap.put(jobRow.getMetadata().getName(), jobRow);
            }
        }

        return jobMap;
    }

    public Map<String, V1beta1CronJob> convertCronJobToMapV1beta1(List<V1beta1CronJob> cronJobs, Map<String, V1beta1CronJob> cronJobMap){
        return this.convertCronJobToMapV1beta1(cronJobs, cronJobMap, null, null);
    }

    public Map<String, V1beta1CronJob> convertCronJobToMapV1beta1(List<V1beta1CronJob> cronJobs, Map<String, V1beta1CronJob> cronJobMap, Integer serviceSeq, Set<String> appmapNamespaces){
        if(CollectionUtils.isNotEmpty(cronJobs)){
            for(V1beta1CronJob cronJobRow : cronJobs){
                if (serviceSeq != null) {
                    if ( !(CollectionUtils.isNotEmpty(appmapNamespaces) && appmapNamespaces.contains(cronJobRow.getMetadata().getNamespace())) ) {
                        continue;
                    }
                }

                cronJobMap.put(cronJobRow.getMetadata().getName(), cronJobRow);
            }
        }

        return cronJobMap;
    }

    public Map<String, V1CronJob> convertCronJobToMapV1(List<V1CronJob> cronJobs, Map<String, V1CronJob> cronJobMap){
        return this.convertCronJobToMapV1(cronJobs, cronJobMap, null, null);
    }

    public Map<String, V1CronJob> convertCronJobToMapV1(List<V1CronJob> cronJobs, Map<String, V1CronJob> cronJobMap, Integer serviceSeq, Set<String> appmapNamespaces){
        if(CollectionUtils.isNotEmpty(cronJobs)){
            for(V1CronJob cronJobRow : cronJobs){
                if (serviceSeq != null) {
                    if ( !(CollectionUtils.isNotEmpty(appmapNamespaces) && appmapNamespaces.contains(cronJobRow.getMetadata().getNamespace())) ) {
                        continue;
                    }
                }

                cronJobMap.put(cronJobRow.getMetadata().getName(), cronJobRow);
            }
        }

        return cronJobMap;
    }

    public Map<String, V1Pod> convertPodToMap(List<V1Pod> pods, Map<String, V1Pod> podMap){
        return this.convertPodToMap(pods, podMap, null, null);
    }

    public Map<String, V1Pod> convertPodToMap(List<V1Pod> pods, Map<String, V1Pod> podMap, Integer serviceSeq, Set<String> appmapNamespaces){
        if(CollectionUtils.isNotEmpty(pods)){
            for(V1Pod podRow : pods){
                if (serviceSeq != null) {
                    if ( !(CollectionUtils.isNotEmpty(appmapNamespaces) && appmapNamespaces.contains(podRow.getMetadata().getNamespace())) ) {
                        continue;
                    }
                }

                podMap.put(podRow.getMetadata().getName(), podRow);
            }
        }

        return podMap;
    }

    public Map<String, V1Service> convertServiceToMap(List<V1Service> services, Map<String, V1Service> serviceMap){
        if(CollectionUtils.isNotEmpty(services)){
            for(V1Service serviceRow : services){
                serviceMap.put(serviceRow.getMetadata().getName(), serviceRow);
            }
        }

        return serviceMap;
    }

    public Map<String, V2HorizontalPodAutoscaler> convertHpaToMapV2(List<V2HorizontalPodAutoscaler> hpas, Map<String, V2HorizontalPodAutoscaler> hpaMap){
        if(CollectionUtils.isNotEmpty(hpas)){
            for(V2HorizontalPodAutoscaler hpaRow : hpas){
//                hpaMap.put(hpaRow.getMetadata().getName(), hpaRow);
                hpaMap.put(hpaRow.getSpec().getScaleTargetRef().getName(), hpaRow);
            }
        }

        return hpaMap;
    }

    public Map<String, V2beta2HorizontalPodAutoscaler> convertHpaToMapV2beta2(List<V2beta2HorizontalPodAutoscaler> hpas, Map<String, V2beta2HorizontalPodAutoscaler> hpaMap){
        if(CollectionUtils.isNotEmpty(hpas)){
            for(V2beta2HorizontalPodAutoscaler hpaRow : hpas){
//                hpaMap.put(hpaRow.getMetadata().getName(), hpaRow);
                hpaMap.put(hpaRow.getSpec().getScaleTargetRef().getName(), hpaRow);
            }
        }

        return hpaMap;
    }

    public Map<String, V2beta1HorizontalPodAutoscaler> convertHpaToMapV2beta1(List<V2beta1HorizontalPodAutoscaler> hpas, Map<String, V2beta1HorizontalPodAutoscaler> hpaMap){
        if(CollectionUtils.isNotEmpty(hpas)){
            for(V2beta1HorizontalPodAutoscaler hpaRow : hpas){
//                hpaMap.put(hpaRow.getMetadata().getName(), hpaRow);
                hpaMap.put(hpaRow.getSpec().getScaleTargetRef().getName(), hpaRow);
            }
        }

        return hpaMap;
    }

    /**
     * Workload rollout 처리 (Job 제외)
     *
     * @param workloadType
     * @param cluster
     * @param namespace
     * @param name
     * @return
     * @throws Exception
     */
    public boolean rolloutRestartWorkload(WorkloadType workloadType, ClusterVO cluster, String namespace, String name) throws Exception {
        boolean result = false;
        if (workloadType.isPossibleRollout()) {
            switch (workloadType) {
                case REPLICA_SERVER:
                    result = this.rolloutRestartDeployment(cluster, namespace, name);
                    break;
                case STATEFUL_SET_SERVER:
                    result = this.rolloutRestartStatefulSet(cluster, namespace, name);
                    break;
                case DAEMON_SET_SERVER:
                    result = this.rolloutRestartDaemonSet(cluster, namespace, name);
                    break;
                case CRON_JOB_SERVER:
                    result = this.rolloutRestartCronJob(cluster, namespace, name);
                    break;
            }
        } else {
            throw new CocktailException(String.format("The %s does not support restart", workloadType.getCode()), ExceptionType.K8sNotSupported);
        }

        return result;
    }

    public boolean rolloutRestartDeployment(ClusterVO cluster, String namespace, String name) throws Exception {
        return this.rolloutRestartDeployment(cluster, namespace, name, true);
    }

    /**
     * Deployment rollout
     * @param cluster
     * @param namespace
     * @param name
     * @throws Exception
     */
    public boolean rolloutRestartDeployment(ClusterVO cluster, String namespace, String name, boolean isThrow) throws Exception {
        try {
            /** get Deployment **/
            V1Deployment iig = k8sWorker.getDeploymentV1(cluster, namespace, name);
            if(iig == null) {
                if(isThrow) {
                    throw new CocktailException("rollout failure : could not found workload : " + name, ExceptionType.ServerNotFound);
                }
                else {
                    log.warn("rollout failure : could not found workload : " + name);
                    return false;
                }
            }

            /** make patch body **/
            List<JsonObject> patchBody = k8sPatchSpecFactory.buildPatchRestartedAtAnno(iig.getSpec().getTemplate().getMetadata().getAnnotations(), "/spec/template/metadata/annotations");
            patchBody.addAll(k8sPatchSpecFactory.buildPatchDeployDatetimeAnno(iig.getMetadata().getAnnotations(), "/metadata/annotations"));

            /** patch deployment **/
            if (CollectionUtils.isNotEmpty(patchBody)) {
                k8sWorker.patchDeploymentV1(cluster, namespace, name, patchBody, false);
            }
        }
        catch (CocktailException ce) {
            if(log.isDebugEnabled()) log.debug("trace log ", ce);
            if(isThrow) {
                throw ce;
            }
            else {
                log.warn("rollout failure : " + name, ce);
                return false;
            }
        }
        catch (Exception ex) {
            if(log.isDebugEnabled()) log.debug("trace log ", ex);
            if(isThrow) {
                throw new CocktailException("rollout failure : " + name, ex, ExceptionType.K8sCocktailCloudUpdateFail);
            }
            else {
                log.warn("rollout failure : " + name, ex);
                return false;
            }
        }

        return true;
    }

    public boolean rolloutRestartStatefulSet(ClusterVO cluster, String namespace, String name) throws Exception {
        return this.rolloutRestartStatefulSet(cluster, namespace, name, true);
    }

    /**
     * StatefulSet rollout
     * @param cluster
     * @param namespace
     * @param name
     * @throws Exception
     */
    public boolean rolloutRestartStatefulSet(ClusterVO cluster, String namespace, String name, boolean isThrow) throws Exception {
        try {
            /** get StatefulSet **/
            V1StatefulSet iig = k8sWorker.getStatefulSetV1(cluster, namespace, name);
            if(iig == null) {
                if(isThrow) {
                    throw new CocktailException("rollout failure : could not found workload : " + name, ExceptionType.ServerNotFound);
                }
                else {
                    log.warn("rollout failure : could not found workload : " + name);
                    return false;
                }
            }

            /** make patch body **/
            List<JsonObject> patchBody = k8sPatchSpecFactory.buildPatchRestartedAtAnno(iig.getSpec().getTemplate().getMetadata().getAnnotations(), "/spec/template/metadata/annotations");
            patchBody.addAll(k8sPatchSpecFactory.buildPatchDeployDatetimeAnno(iig.getMetadata().getAnnotations(), "/metadata/annotations"));

            /** patch StatefulSet **/
            if (CollectionUtils.isNotEmpty(patchBody)) {
                k8sWorker.patchStatefulSetV1(cluster, namespace, name, patchBody, false);
            }
        }
        catch (CocktailException ce) {
            if(log.isDebugEnabled()) log.debug("trace log ", ce);
            if(isThrow) {
                throw ce;
            }
            else {
                log.warn("rollout failure : " + name, ce);
                return false;
            }
        }
        catch (Exception ex) {
            if(log.isDebugEnabled()) log.debug("trace log ", ex);
            if(isThrow) {
                throw new CocktailException("rollout failure : " + name, ex, ExceptionType.K8sCocktailCloudUpdateFail);
            }
            else {
                log.warn("rollout failure : " + name, ex);
                return false;
            }
        }

        return true;
    }

    public boolean rolloutRestartDaemonSet(ClusterVO cluster, String namespace, String name) throws Exception {
        return this.rolloutRestartDaemonSet(cluster, namespace, name, true);
    }

    /**
     * DaemonSet rollout
     * @param cluster
     * @param namespace
     * @param name
     * @throws Exception
     */
    public boolean rolloutRestartDaemonSet(ClusterVO cluster, String namespace, String name, boolean isThrow) throws Exception {
        try {
            /** get DaemonSet **/
            V1DaemonSet iig = k8sWorker.getDaemonSetV1(cluster, namespace, name);
            if(iig == null) {
                if(isThrow) {
                    throw new CocktailException("rollout failure : could not found workload : " + name, ExceptionType.ServerNotFound);
                }
                else {
                    log.warn("rollout failure : could not found workload : " + name);
                    return false;
                }
            }

            /** make patch body **/
            List<JsonObject> patchBody = k8sPatchSpecFactory.buildPatchRestartedAtAnno(iig.getSpec().getTemplate().getMetadata().getAnnotations(), "/spec/template/metadata/annotations");
            patchBody.addAll(k8sPatchSpecFactory.buildPatchDeployDatetimeAnno(iig.getMetadata().getAnnotations(), "/metadata/annotations"));

            /** patch DaemonSet **/
            if (CollectionUtils.isNotEmpty(patchBody)) {
                k8sWorker.patchDaemonSetV1(cluster, namespace, name, patchBody, false);
            }
        }
        catch (CocktailException ce) {
            if(log.isDebugEnabled()) log.debug("trace log ", ce);
            if(isThrow) {
                throw ce;
            }
            else {
                log.warn("rollout failure : " + name, ce);
                return false;
            }
        }
        catch (Exception ex) {
            if(log.isDebugEnabled()) log.debug("trace log ", ex);
            if(isThrow) {
                throw new CocktailException("rollout failure : " + name, ex, ExceptionType.K8sCocktailCloudUpdateFail);
            }
            else {
                log.warn("rollout failure : " + name, ex);
                return false;
            }
        }

        return true;
    }

    public boolean rolloutRestartCronJob(ClusterVO cluster, String namespace, String name) throws Exception {
        return this.rolloutRestartCronJob(cluster, namespace, name, true);
    }

    /**
     * CronJob rollout
     * @param cluster
     * @param namespace
     * @param name
     * @throws Exception
     */
    public boolean rolloutRestartCronJob(ClusterVO cluster, String namespace, String name, boolean isThrow) throws Exception {
        try {
            K8sApiVerKindType apiVerKindType = ResourceUtil.getApiVerKindType(cluster, K8sApiKindType.CRON_JOB);

            if (apiVerKindType != null) {
                if(apiVerKindType.getGroupType() == K8sApiGroupType.BATCH && apiVerKindType.getApiType() == K8sApiType.V1BETA1){
                    /** get CronJob **/
                    V1beta1CronJob iig = k8sWorker.getCronJobV1beta1(cluster, namespace, name);
                    if(iig == null) {
                        if(isThrow) {
                            throw new CocktailException("rollout failure : could not found workload : " + name, ExceptionType.ServerNotFound);
                        }
                        else {
                            log.warn("rollout failure : could not found workload : " + name);
                            return false;
                        }
                    }

                    /** make patch body **/
                    List<JsonObject> patchBody = k8sPatchSpecFactory.buildPatchRestartedAtAnno(iig.getSpec().getJobTemplate().getSpec().getTemplate().getMetadata().getAnnotations(), "/spec/jobTemplate/spec/template/metadata/annotations");
                    patchBody.addAll(k8sPatchSpecFactory.buildPatchDeployDatetimeAnno(iig.getMetadata().getAnnotations(), "/metadata/annotations"));

                    /** patch CronJob **/
                    if (CollectionUtils.isNotEmpty(patchBody)) {
                        k8sWorker.patchCronJobV1beta1(cluster, namespace, name, patchBody, false);
                    }
                } else {
                    /** get CronJob **/
                    V1CronJob iig = k8sWorker.getCronJobV1(cluster, namespace, name);
                    if(iig == null) {
                        if(isThrow) {
                            throw new CocktailException("rollout failure : could not found workload : " + name, ExceptionType.ServerNotFound);
                        }
                        else {
                            log.warn("rollout failure : could not found workload : " + name);
                            return false;
                        }
                    }

                    /** make patch body **/
                    List<JsonObject> patchBody = k8sPatchSpecFactory.buildPatchRestartedAtAnno(iig.getSpec().getJobTemplate().getSpec().getTemplate().getMetadata().getAnnotations(), "/spec/jobTemplate/spec/template/metadata/annotations");
                    patchBody.addAll(k8sPatchSpecFactory.buildPatchDeployDatetimeAnno(iig.getMetadata().getAnnotations(), "/metadata/annotations"));

                    /** patch CronJob **/
                    if (CollectionUtils.isNotEmpty(patchBody)) {
                        k8sWorker.patchCronJobV1(cluster, namespace, name, patchBody, false);
                    }
                }
            } else {
                throw new CocktailException(String.format("Cube Cluster version[%s] is not support.", cluster.getK8sVersion()), ExceptionType.K8sNotSupported);
            }

        }
        catch (CocktailException ce) {
            if(log.isDebugEnabled()) log.debug("trace log ", ce);
            if(isThrow) {
                throw ce;
            }
            else {
                log.warn("rollout failure : " + name, ce);
                return false;
            }
        }
        catch (Exception ex) {
            if(log.isDebugEnabled()) log.debug("trace log ", ex);
            if(isThrow) {
                throw new CocktailException("rollout failure : " + name, ex, ExceptionType.K8sCocktailCloudUpdateFail);
            }
            else {
                log.warn("rollout failure : " + name, ex);
                return false;
            }
        }

        return true;
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
