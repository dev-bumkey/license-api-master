package run.acloud.api.resource.service;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.JsonObject;
import io.kubernetes.client.custom.Quantity;
import io.kubernetes.client.openapi.JSON;
import io.kubernetes.client.openapi.models.*;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import run.acloud.api.configuration.dao.IClusterMapper;
import run.acloud.api.configuration.vo.ClusterNodePoolResourceVO;
import run.acloud.api.configuration.vo.ClusterVO;
import run.acloud.api.cserver.dao.IServicemapMapper;
import run.acloud.api.cserver.vo.SpecFileDeployVO;
import run.acloud.api.k8sextended.util.Yaml;
import run.acloud.api.monitoring.vo.ServiceMonitoringVO;
import run.acloud.api.resource.constants.KubeConstants;
import run.acloud.api.resource.enums.JsonPatchOp;
import run.acloud.api.resource.enums.TaintEffects;
import run.acloud.api.resource.task.K8sPatchSpecFactory;
import run.acloud.api.resource.task.K8sWorker;
import run.acloud.api.resource.util.ResourceUtil;
import run.acloud.api.resource.vo.*;
import run.acloud.commons.util.JsonUtils;
import run.acloud.commons.util.Utils;
import run.acloud.commons.vo.ExecutingContextVO;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class K8sResourceService {
    private static final Integer MAX_TAIL_COUNT = 10000;
//    private static final String COCKTAIL_REGISTRY_SECRET = "cocktail-registry-secret";

    @Resource(name = "cocktailSession")
    private SqlSessionTemplate sqlSession;

    @Autowired
    private K8sWorker k8sWorker;

    @Autowired
    private K8sPatchSpecFactory k8sPatchSpecFactory;

//    private static Logger stateLogger = LoggerFactory.getLogger("server.state.logger");



    /**
     * K8S Event 정보 조회
     * (By Cluster)
     *
     * @param clusterSeq
     * @param field
     * @param label
     * @param context
     * @return
     * @throws Exception
     */
    public List<K8sEventVO> getEventByCluster(Integer clusterSeq, String namespaceName, String field, String label, ExecutingContextVO context) throws Exception {
        try {
            if(clusterSeq != null){
                IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);

                ClusterVO cluster = clusterDao.getCluster(clusterSeq);

                return this.getEventByCluster(cluster, namespaceName, field, label, context);
            }else{
                throw new CocktailException("clusterSeq is null.", ExceptionType.InvalidParameter);
            }
        } catch (Exception e) {
            if (e instanceof CocktailException) {
                if (((CocktailException) e).getType() == ExceptionType.K8sCocktailCloudInquireFail) {
                    throw e;
                }
            }

            throw new CocktailException("getEventByCluster fail!!", e, ExceptionType.K8sCocktailCloudInquireFail);
        }
    }

    /**
     * K8S Event 정보 조회
     * (By Cluster)
     *
     * @param cluster
     * @param field
     * @param label
     * @param context
     * @return
     * @throws Exception
     */
    public List<K8sEventVO> getEventByCluster(ClusterVO cluster, String namespaceName, String field, String label, ExecutingContextVO context) throws Exception {
        try {
            if(cluster != null){
                return this.convertEventDataList(cluster, namespaceName, field, label);
            }else{
                throw new CocktailException("cluster is null.", ExceptionType.InvalidParameter);
            }
        } catch (Exception e) {
            throw new CocktailException("getEventByCluster fail!!", e, ExceptionType.K8sCocktailCloudInquireFail);
        }
    }

    /**
     * K8S Event 정보 조회 후 V1Event -> K8sEventVO 변환
     *
     * @param cluster
     * @param field
     * @param label
     * @return
     * @throws Exception
     */
    public List<K8sEventVO> convertEventDataList(ClusterVO cluster, String nameSpace, String field, String label) throws Exception {
        List<K8sEventVO> events = new ArrayList<>();

        try {

            List<CoreV1Event> v1Events = k8sWorker.getEventV1(cluster, nameSpace, field, label);

            events = this.genEventDataList(v1Events, new JSON());
        } catch (Exception e) {
            throw new CocktailException("convertEventDataList fail!!", e, ExceptionType.K8sCocktailCloudInquireFail);
        }

        return events;
    }

    public  List<K8sEventVO> genEventDataList(List<CoreV1Event> v1Events, JSON k8sJson) throws Exception {

        List<K8sEventVO> events = new ArrayList<>();

        if(CollectionUtils.isNotEmpty(v1Events)){
            if (k8sJson == null) {
                k8sJson = new JSON();
            }

            K8sEventVO event = null;
            for(CoreV1Event v1Event : v1Events){
                event = this.genEventData(v1Event, k8sJson);

                events.add(event);
            }
        }

        return events;
    }

    public K8sEventVO genEventData(CoreV1Event v1Event, JSON k8sJson) throws Exception {

        if (k8sJson == null) {
            k8sJson = new JSON();
        }

        K8sEventVO event = new K8sEventVO();
        event.setMessage(v1Event.getMessage());
        event.setSource(String.format("%s %s", StringUtils.defaultString(v1Event.getSource().getComponent()), StringUtils.defaultString(v1Event.getSource().getHost())));
        event.setSubObject(StringUtils.defaultString(v1Event.getInvolvedObject().getFieldPath()));
        event.setCount(v1Event.getCount());
        event.setFirstTime(v1Event.getFirstTimestamp());
        event.setLastTime(v1Event.getLastTimestamp());
        event.setKind(StringUtils.defaultString(v1Event.getInvolvedObject().getKind()));
        event.setName(StringUtils.defaultString(v1Event.getInvolvedObject().getName()));
        event.setNamespace(StringUtils.defaultString(v1Event.getInvolvedObject().getNamespace()));
        event.setInvolvedObject(ResourceUtil.setObjectReference(v1Event.getInvolvedObject()));
        event.setRelated(ResourceUtil.setObjectReference(v1Event.getRelated()));
        event.setDeployment(k8sJson.serialize(v1Event));
        event.setDeploymentYaml(Yaml.getSnakeYaml().dumpAsMap(v1Event));

        return event;
    }


    /**
     * R3.5 : 2019.10.15 : 노드의 Label, Annotation, Taint를 수정한다.
     * @param clusterSeq
     * @param nodeName
     * @param updateNode
     * @return
     * @throws Exception
     */
    public K8sNodeDetailVO patchNode(Integer clusterSeq, String nodeName, K8sNodeDetailVO updateNode) throws Exception{
        try {
            /** Cluster Sequence가 Null이면 오류 **/
            if(clusterSeq == null) {
                throw new CocktailException("clusterSeq is null.", ExceptionType.InvalidParameter);
            }
            IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
            ClusterVO cluster = clusterDao.getCluster(clusterSeq);

            /** Cluster를 조회할 수 없으면 오류 **/
            if(cluster == null) {
                throw new CocktailException("can't find cluster", ExceptionType.ClusterNotFound);
            }

            V1Node currentNode = k8sWorker.getNodeV1(cluster, nodeName);

            if(currentNode == null) {
                throw new CocktailException("can't find node", ExceptionType.NodeNotFound);
            }

            Map<String, JsonPatchOp> labelPatchOp = new HashMap<>();
            Map<String, JsonPatchOp> annotationPatchOp = new HashMap<>();
            Map<String, JsonPatchOp> taintsPatchOp = new HashMap<>();

            /** Labels **/
            labelPatchOp = k8sPatchSpecFactory.makePatchOp(currentNode.getMetadata().getLabels(), updateNode.getLabels());
            updateNode.setLabelPatchOp(labelPatchOp);

            /** Annotations **/
            annotationPatchOp = k8sPatchSpecFactory.makePatchOp(currentNode.getMetadata().getAnnotations(), updateNode.getAnnotations());
            updateNode.setAnnotationPatchOp(annotationPatchOp);

            /** Taints **/
            if(CollectionUtils.isNotEmpty(updateNode.getTaints())) {
                for (K8sTaintVO updateTaint : updateNode.getTaints()) {
                    // Taints의 Key는 필수 입력
                    if(StringUtils.isEmpty(updateTaint.getKey())) {
                        throw new CocktailException("The key is mandatory when setting taint.", ExceptionType.KeyOfTaintIsNull);
                    }
                    // Taints의 Effect는 필수 입력
                    if(StringUtils.isEmpty(updateTaint.getEffect())) {
                        throw new CocktailException("The effect is mandatory when setting taint.", ExceptionType.EffectOfTaintIsNull);
                    }
                    // Taints의 Effect에는 정의된 값만 입력 가능.
                    if(!TaintEffects.getTaintEffectList().contains(updateTaint.getEffect())) {
                        throw new CocktailException("Invalid effect value in taints", ExceptionType.InvalidEffectOfTaint);
                    }

                    boolean isFoundKey = false;
                    int foundIndex = 0;
                    if(CollectionUtils.isNotEmpty(currentNode.getSpec().getTaints())) {
                        for (int i = 0; i < currentNode.getSpec().getTaints().size(); i++) {
                            V1Taint currentTaint = currentNode.getSpec().getTaints().get(i);
                            if (currentTaint.getKey() != null && currentTaint.getKey().equals(updateTaint.getKey())) {
                                isFoundKey = true;
                                foundIndex = i;
                                break;
                            }
                        }
                    }
                    if (isFoundKey) {
                        if (!StringUtils.equals(updateTaint.getValue(), currentNode.getSpec().getTaints().get(foundIndex).getValue()) ||
                            !StringUtils.equals(updateTaint.getEffect(), currentNode.getSpec().getTaints().get(foundIndex).getEffect())) {
                            taintsPatchOp.put(updateTaint.getKey(), JsonPatchOp.REPLACE);
                        }
                    }
                    else {
                        taintsPatchOp.put(updateTaint.getKey(), JsonPatchOp.ADD);
                    }
                }
            }
            if(CollectionUtils.isNotEmpty(currentNode.getSpec().getTaints())) {
                for(V1Taint currentV1Taint : currentNode.getSpec().getTaints()) {
                    boolean isNotFoundKey = true;
                    if(CollectionUtils.isNotEmpty(updateNode.getTaints())) {
                        for (int i = 0; i < updateNode.getTaints().size(); i++) {
                            K8sTaintVO updateV1Taint = updateNode.getTaints().get(i);
                            if (updateV1Taint.getKey() != null && StringUtils.equals(updateV1Taint.getKey(), currentV1Taint.getKey())) {
                                isNotFoundKey = false;
                                break;
                            }
                        }
                    }
                    if(isNotFoundKey) {
                        taintsPatchOp.put(currentV1Taint.getKey(), JsonPatchOp.REMOVE);
                    }
                }
            }
            updateNode.setTaintsPatchOp(taintsPatchOp);
            String logDel = "===============================================================";
            List<JsonObject> patchBody = k8sPatchSpecFactory.buildPatchNodeV1(updateNode, currentNode);
                    log.debug("{}\n{}\n{}\n{}\n{}", logDel, JsonUtils.toGson(updateNode), logDel, JsonUtils.toGson(patchBody), logDel);
            V1Node node = k8sWorker.patchNodeV1(cluster, nodeName, patchBody);
                    log.debug("{}\n{}\n{}\n{}\n{}\n{}\n{}", logDel, JsonUtils.toGson(node.getMetadata().getLabels()), logDel, JsonUtils.toGson(node.getMetadata().getAnnotations()), logDel, JsonUtils.toGson(node.getSpec().getTaints()), logDel);

        } catch (Exception e) {
            log.error(ExceptionUtils.getStackTrace(e));
            throw e;
        }

        return updateNode;
    }

    /**
     * K8S Node 정보 조회
     *
     * @param clusterSeq
     * @param context
     * @return
     * @throws Exception
     */
    public K8sNodeVO getNode(Integer clusterSeq, String nodeName, boolean usePod, ExecutingContextVO context) throws Exception{

        return this.getNode(clusterSeq, nodeName,null, usePod, context);
    }

    public K8sNodeVO getNode(Integer clusterSeq, String nodeName, Integer serviceSeq, boolean usePod, ExecutingContextVO context) throws Exception{
        try {
            if(clusterSeq != null){
                IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
                ClusterVO cluster = clusterDao.getCluster(clusterSeq);

                if(cluster != null){
                    List<K8sNodeVO> nodes = this.convertNodeDataList(cluster, serviceSeq, nodeName, usePod, false, context);
                    return Optional.ofNullable(nodes.get(0)).orElseGet(() ->null);
                }else{
                    throw new CocktailException("cluster is null.", ExceptionType.InvalidParameter);
                }
            }else{
                throw new CocktailException("clusterSeq is null.", ExceptionType.InvalidParameter);
            }
        } catch (Exception e) {
            throw new CocktailException("getNode fail!!", e, ExceptionType.K8sCocktailCloudInquireFail);
        }
    }

    /**
     * K8S Node 정보 조회
     *
     * @param clusterSeq
     * @param context
     * @return
     * @throws Exception
     */
    public List<K8sNodeVO> getNodes(Integer clusterSeq, boolean usePod, boolean gpuOnly, ExecutingContextVO context) throws Exception{

        return this.getNodes(clusterSeq, null, usePod, gpuOnly, context);
    }

    public List<K8sNodeVO> getNodes(Integer clusterSeq, Integer serviceSeq, boolean usePod, boolean gpuOnly, ExecutingContextVO context) throws Exception{

        try {
            if(clusterSeq != null){
                IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);

                ClusterVO cluster = clusterDao.getCluster(clusterSeq);

                if(cluster != null){
                    return this.convertNodeDataList(cluster, serviceSeq, usePod, gpuOnly, context);
                }else{
                    throw new CocktailException("cluster is null.", ExceptionType.InvalidParameter);
                }
            }else{
                throw new CocktailException("clusterSeq is null.", ExceptionType.InvalidParameter);
            }
        } catch (Exception e) {
            throw new CocktailException("getNodes fail!!", e, ExceptionType.K8sCocktailCloudInquireFail);
        }
    }

    /**
     * K8S Node 정보 조회 후,
     *
     * V1Node -> K8sNodeVO 변환
     *
     * @param cluster
     * @param context
     * @return
     * @throws Exception
     */
    public List<K8sNodeVO> convertNodeDataList(ClusterVO cluster, Integer serviceSeq, boolean usePod, boolean gpuOnly, ExecutingContextVO context) throws Exception{
        return this.convertNodeDataList(cluster, serviceSeq, null, usePod, gpuOnly, context);
    }
    public List<K8sNodeVO> convertNodeDataList(ClusterVO cluster, Integer serviceSeq, String nodeName, boolean usePod, boolean gpuOnly, ExecutingContextVO context) throws Exception{

        List<K8sNodeVO> nodes = new ArrayList<>();

        HttpServletRequest request = Utils.getCurrentRequest();

        try {
            if(cluster != null){
                // Node 조회
                List<V1Node> v1Nodes;
                if(StringUtils.isNotEmpty(nodeName)) {  // NodeName이 있으면 해당 Node만 조회.
                    V1Node v1Node = k8sWorker.getNodeV1(cluster, nodeName);
                    v1Nodes = new ArrayList<>();
                    v1Nodes.add(v1Node);
                }
                else {
                    String labelSelector = null;
                    if (gpuOnly) {
                        labelSelector = String.format("%s", KubeConstants.RESOURCES_GPU);
                    }
                    v1Nodes = k8sWorker.getNodesV1(cluster, null, labelSelector);
                }
                // Pod 조회
                Map<String, List<V1Pod>> v1PodMap = Maps.newHashMap();
                if(usePod){
                    List<V1Pod> v1Pods = k8sWorker.getPodV1AllNamespace(cluster, null, null);
                    if (CollectionUtils.isNotEmpty(v1Pods)) {
                        for (V1Pod v1Pod : v1Pods) {
                            if (MapUtils.getObject(v1PodMap, v1Pod.getSpec().getNodeName(), null) == null) {
                                v1PodMap.put(v1Pod.getSpec().getNodeName(), Lists.newArrayList());
                            }
                            v1PodMap.get(v1Pod.getSpec().getNodeName()).add(v1Pod);
                        }
                    }
                }

                if(CollectionUtils.isNotEmpty(v1Nodes)){
                    K8sNodeVO node;
                    Map<String, String> addressesMap;
                    long pcnt = 0L;
                    long rccnt = 0L;
                    long lccnt = 0L;
                    long rmcnt = 0L;
                    long lmcnt = 0L;
                    int  rgcnt = 0;
                    int  lgcnt = 0;

                    // joda.datetime Serialization
                    JSON k8sJson = new JSON();

                    Set<String> namespaces = this.getServicemapNamespacesByService(serviceSeq);

                    for(V1Node v1NodeRow : v1Nodes){
                        node = new K8sNodeVO();
                        pcnt = 0L;
                        rccnt = 0L;
                        lccnt = 0L;
                        rmcnt = 0L;
                        lmcnt = 0L;
                        rgcnt = 0;
                        lgcnt = 0;

                        // Node 목록
                        node.setName(v1NodeRow.getMetadata().getName());
                        node.setLabels(v1NodeRow.getMetadata().getLabels());
                        node.setCreationTimestamp(v1NodeRow.getMetadata().getCreationTimestamp());
                        node.setDeployment(k8sJson.serialize(v1NodeRow));
                        node.setDeploymentYaml(Yaml.getSnakeYaml().dumpAsMap(v1NodeRow));
                        node.setResources(Maps.newHashMap());
                        if(MapUtils.isNotEmpty(v1NodeRow.getStatus().getCapacity())){
                            node.setCapacity(v1NodeRow.getStatus().getCapacity());
                            for (Map.Entry<String, Quantity> capacityEntryRow : v1NodeRow.getStatus().getCapacity().entrySet()) {
                                switch (capacityEntryRow.getKey()) {
                                    case KubeConstants.RESOURCES_CPU:
                                        node.setCpuCapacity(k8sWorker.convertQuantityToLong(capacityEntryRow.getValue()));
                                        break;
                                    case KubeConstants.RESOURCES_MEMORY:
                                        node.setMemoryCapacity(k8sWorker.convertQuantityToLong(capacityEntryRow.getValue()));
                                        break;
                                    case "ephemeral-storage":
                                        node.setEphemeralStorageCapacity(k8sWorker.convertQuantityToLong(capacityEntryRow.getValue()));
                                        break;
                                    case KubeConstants.RESOURCES_GPU:
                                        node.setGpuCapacity(capacityEntryRow.getValue().getNumber().intValue());
                                        break;
                                    case "pods":
                                        node.setPodCapacity(String.valueOf(capacityEntryRow.getValue().getNumber().intValue()));
                                        break;
                                }

                                if (MapUtils.getObject(node.getResources(), capacityEntryRow.getKey(), null) == null) {
                                    node.getResources().put(capacityEntryRow.getKey(), new ClusterNodePoolResourceVO());
                                }

                                node.getResources().get(capacityEntryRow.getKey()).setCapacity(capacityEntryRow.getValue().getNumber().longValue());
                            }
//                            node.setCpuCapacity(k8sWorker.convertQuantityToLong(v1NodeRow.getStatus().getCapacity().get("cpu")));
//                            node.setMemoryCapacity(k8sWorker.convertQuantityToLong(v1NodeRow.getStatus().getCapacity().get("memory")));
//                            node.setGpuCapacity(v1NodeRow.getStatus().getCapacity().get(KubeConstants.RESOURCES_GPU) != null ? v1NodeRow.getStatus().getCapacity().get(KubeConstants.RESOURCES_GPU).getNumber().intValue() : 0);
//                            node.setPodCapacity(v1NodeRow.getStatus().getCapacity().get("pods") != null ? String.valueOf(v1NodeRow.getStatus().getCapacity().get("pods").getNumber().intValue()) : null);
                        }
                        if(MapUtils.isNotEmpty(v1NodeRow.getStatus().getAllocatable())){
                            node.setAllocatable(v1NodeRow.getStatus().getAllocatable());
                            for (Map.Entry<String, Quantity> allocatableEntryRow : v1NodeRow.getStatus().getAllocatable().entrySet()) {
                                switch (allocatableEntryRow.getKey()) {
                                    case KubeConstants.RESOURCES_CPU:
                                        node.setAllocatedCpu(k8sWorker.convertQuantityToLong(allocatableEntryRow.getValue()));
                                        break;
                                    case KubeConstants.RESOURCES_MEMORY:
                                        node.setAllocatedMemory(k8sWorker.convertQuantityToLong(allocatableEntryRow.getValue()));
                                        break;
                                    case "ephemeral-storage":
                                        node.setEphemeralStorageCapacity(k8sWorker.convertQuantityToLong(allocatableEntryRow.getValue()));
                                        break;
                                    case KubeConstants.RESOURCES_GPU:
                                        node.setAllocatedGpu(allocatableEntryRow.getValue().getNumber().intValue());
                                        break;
                                    case "pods":
                                        node.setAllocatedPods(String.valueOf(allocatableEntryRow.getValue().getNumber().intValue()));
                                        break;
                                }

                                if (MapUtils.getObject(node.getResources(), allocatableEntryRow.getKey(), null) == null) {
                                    node.getResources().put(allocatableEntryRow.getKey(), new ClusterNodePoolResourceVO());
                                }

                                node.getResources().get(allocatableEntryRow.getKey()).setAllocatable(allocatableEntryRow.getValue().getNumber().longValue());
                            }
//                            node.setAllocatedCpu(k8sWorker.convertQuantityToLong(v1NodeRow.getStatus().getAllocatable().get("cpu")));
//                            node.setAllocatedMemory(k8sWorker.convertQuantityToLong(v1NodeRow.getStatus().getAllocatable().get("memory")));
//                            node.setAllocatedGpu(v1NodeRow.getStatus().getAllocatable().get(KubeConstants.RESOURCES_GPU) != null ? v1NodeRow.getStatus().getAllocatable().get(KubeConstants.RESOURCES_GPU).getNumber().intValue() : 0);
//                            node.setAllocatedPods(v1NodeRow.getStatus().getAllocatable().get("pods") != null ? String.valueOf(v1NodeRow.getStatus().getAllocatable().get("pods").getNumber().intValue()) : null);
                        }

                        // k8s status - cpu, memory, pod 할당량
                        if(MapUtils.isNotEmpty(v1PodMap) && MapUtils.getObject(v1PodMap, v1NodeRow.getMetadata().getName(), null) != null){
//                            String fieldSelectorForPods = String.format("%s.%s=%s", KubeConstants.SPEC, KubeConstants.SPEC_NODE_NAME, v1NodeRow.getMetadata().getName());
//                            if(usePod){
//                                v1Pods = k8sWorker.getPodV1AllNamespace(cluster, fieldSelectorForPods, null, apiInstance);
//                            }
                            POD_LOOP:
                                for(V1Pod v1Pod : v1PodMap.get(v1NodeRow.getMetadata().getName())){
                                    if(StringUtils.equalsIgnoreCase(v1NodeRow.getMetadata().getName(), v1Pod.getSpec().getNodeName())){
                                        if (serviceSeq != null) {
                                            if (CollectionUtils.isNotEmpty(namespaces) && namespaces.contains(v1Pod.getMetadata().getNamespace())) {

                                            } else {
                                                continue POD_LOOP;
                                            }
                                        }
                                        if(StringUtils.equalsIgnoreCase("running", v1Pod.getStatus().getPhase())){
                                            for(V1Container v1Container : v1Pod.getSpec().getContainers()){
                                                if(v1Container.getResources().getRequests() != null){
    //                                                log.debug("pod Name : {}.{}", v1Pod.getMetadata().getName(), v1Container.getName());
                                                    rccnt += k8sWorker.convertQuantityToLongForCpuMilliCore(v1Container.getResources().getRequests().get(KubeConstants.RESOURCES_CPU));
                                                    rmcnt += k8sWorker.convertQuantityToLong(v1Container.getResources().getRequests().get(KubeConstants.RESOURCES_MEMORY));
                                                    if (v1Container.getResources().getRequests().get(KubeConstants.RESOURCES_GPU) != null) {
                                                        rgcnt += v1Container.getResources().getRequests().get(KubeConstants.RESOURCES_GPU).getNumber().intValue();
                                                    }
                                                }
                                                if(v1Container.getResources().getLimits() != null){
                                                    lccnt += k8sWorker.convertQuantityToLongForCpuMilliCore(v1Container.getResources().getLimits().get(KubeConstants.RESOURCES_CPU));
                                                    lmcnt += k8sWorker.convertQuantityToLong(v1Container.getResources().getLimits().get(KubeConstants.RESOURCES_MEMORY));
                                                    if (v1Container.getResources().getLimits().get(KubeConstants.RESOURCES_GPU) != null) {
                                                        lgcnt += v1Container.getResources().getLimits().get(KubeConstants.RESOURCES_GPU).getNumber().intValue();
                                                    }
                                                }
                                            }
                                        }
                                        pcnt++;
                                    }
                                }
                        }

                        node.setCpuRequests(rccnt);
                        node.setCpuLimits(lccnt);
                        node.setMemoryRequests(rmcnt);
                        node.setMemoryLimits(lmcnt);
                        node.setGpuRequests(rgcnt);
                        node.setGpuLimits(lgcnt);
                        node.setAllocationPods(pcnt);
                        if (MapUtils.getObject(node.getResources(), KubeConstants.RESOURCES_CPU, null) != null) {
                            node.getResources().get(KubeConstants.RESOURCES_CPU).setRequest(rccnt);
                        }
                        if (MapUtils.getObject(node.getResources(), KubeConstants.RESOURCES_MEMORY, null) != null) {
                            node.getResources().get(KubeConstants.RESOURCES_MEMORY).setRequest(rmcnt);
                        }
                        if (MapUtils.getObject(node.getResources(), KubeConstants.RESOURCES_GPU, null) != null) {
                            node.getResources().get(KubeConstants.RESOURCES_GPU).setRequest(rgcnt);
                        }

                        // Node Detail
                        K8sNodeDetailVO nodeDetail = new K8sNodeDetailVO();
                        nodeDetail.setName(v1NodeRow.getMetadata().getName());
                        nodeDetail.setLabels(v1NodeRow.getMetadata().getLabels());
                        nodeDetail.setAnnotations(v1NodeRow.getMetadata().getAnnotations());
                        nodeDetail.setCreationTime(v1NodeRow.getMetadata().getCreationTimestamp());
                        if(CollectionUtils.isNotEmpty(v1NodeRow.getStatus().getAddresses())){
                            addressesMap = new HashMap<>();
                            for(V1NodeAddress v1NodeAddressRow : v1NodeRow.getStatus().getAddresses()){
                                addressesMap.put(v1NodeAddressRow.getType(), v1NodeAddressRow.getAddress());
                            }
                            nodeDetail.setAddresses(addressesMap);
                        }
                        if(v1NodeRow.getSpec().getConfigSource() != null){
                            nodeDetail.setConfigSource(k8sJson.serialize(v1NodeRow.getSpec().getConfigSource()));
                        }
                        nodeDetail.setPodCIDR(v1NodeRow.getSpec().getPodCIDR());
                        nodeDetail.setProviderID(v1NodeRow.getSpec().getProviderID());
                        if(CollectionUtils.isNotEmpty(v1NodeRow.getSpec().getTaints())){
                            nodeDetail.setTaintsJson(k8sJson.serialize(v1NodeRow.getSpec().getTaints()));
                            List<K8sTaintVO> taints = new ArrayList<>();
                            for(V1Taint t : v1NodeRow.getSpec().getTaints()) {
                                K8sTaintVO taint = new K8sTaintVO();
                                taint.setEffect(t.getEffect());
                                taint.setKey(t.getKey());
                                taint.setValue(t.getValue());
                                taints.add(taint);
                            }
                            nodeDetail.setTaints(taints);
                        }
                        if(v1NodeRow.getSpec().getUnschedulable() != null){
                            nodeDetail.setUnschedulable(v1NodeRow.getSpec().getUnschedulable());
                        }
                        nodeDetail.setMachineID(v1NodeRow.getStatus().getNodeInfo().getMachineID());
                        nodeDetail.setSystemUUID(v1NodeRow.getStatus().getNodeInfo().getSystemUUID());
                        nodeDetail.setBootID(v1NodeRow.getStatus().getNodeInfo().getBootID());
                        nodeDetail.setKernelVersion(v1NodeRow.getStatus().getNodeInfo().getKernelVersion());
                        nodeDetail.setOsImage(v1NodeRow.getStatus().getNodeInfo().getOsImage());
                        nodeDetail.setContainerRuntimeVersion(v1NodeRow.getStatus().getNodeInfo().getContainerRuntimeVersion());
                        nodeDetail.setKubeletVersion(v1NodeRow.getStatus().getNodeInfo().getKubeletVersion());
                        nodeDetail.setKubeProxyVersion(v1NodeRow.getStatus().getNodeInfo().getKubeProxyVersion());
                        nodeDetail.setOperatingSystem(v1NodeRow.getStatus().getNodeInfo().getOperatingSystem());
                        nodeDetail.setArchitecture(v1NodeRow.getStatus().getNodeInfo().getArchitecture());
                        node.setDetail(nodeDetail);

                        // Node Condition
                        List<K8sNodeConditionVO> nodeConditions = new ArrayList<>();
                        if(CollectionUtils.isNotEmpty(v1NodeRow.getStatus().getConditions())){
                            K8sNodeConditionVO nodeCondition;
                            for(V1NodeCondition v1NodeConditionRow : v1NodeRow.getStatus().getConditions()){
                                nodeCondition = new K8sNodeConditionVO();
                                BeanUtils.copyProperties(nodeCondition, v1NodeConditionRow);
                                if(v1NodeConditionRow.getLastHeartbeatTime() != null){
                                    nodeCondition.setLastHeartbeatTime(v1NodeConditionRow.getLastHeartbeatTime());
                                }
                                if(v1NodeConditionRow.getLastTransitionTime() != null){
                                    nodeCondition.setLastTransitionTime(v1NodeConditionRow.getLastTransitionTime());
                                }

                                // 목록 Ready 상태 셋팅
                                if(StringUtils.equalsIgnoreCase("Ready", v1NodeConditionRow.getType())){
                                    node.setReady(v1NodeConditionRow.getStatus());
                                }

                                nodeConditions.add(nodeCondition);
                            }
                        }
                        node.setConditions(nodeConditions);

                        nodes.add(node);
                    }
                }
            }else{
                throw new CocktailException("cluster is null.", ExceptionType.InvalidParameter);
            }
        } catch (Exception e) {
            throw new CocktailException("convertNodeDataList fail!!", e, ExceptionType.K8sCocktailCloudInquireFail);
        }

        return nodes;
    }

    /**
     * Kubernetes Spec File Deploy (Json / Yaml)
     * @param clusterSeq
     * @param deployData
     * @param ctx
     * @return
     */
    public SpecFileDeployVO replaceResourceBySpecFile(Integer clusterSeq, SpecFileDeployVO deployData, ExecutingContextVO ctx) throws Exception {
        IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
        ClusterVO cluster = clusterDao.getCluster(clusterSeq);

        if (cluster == null) {
            log.error("Cluster is null");
            throw new CocktailException("Cluster is null", ExceptionType.InvalidParameter);
        }

        try {
            if(deployData.getType().equalsIgnoreCase("yaml")) {

                /**
                 * TODO Redion : 일단 동작하도록 구현 완료 후 Refactoring...................... 필요..
                 * 1. WorkloadVersionSet에 매핑되는 resourceClassName을 ENUM 객체에 매핑하여 처리.
                 *    - 지원하는 워크로드 버전셋 체크 및 Resource 유형에 따른 분기등 모두 사용할 수 있도록..
                 * 2. 기능별 컴포넌트 화..
                 **/
                // Snake YAML을 이용하여 YAML Data Loading
                org.yaml.snakeyaml.Yaml snakeYaml = new org.yaml.snakeyaml.Yaml();
                LinkedHashMap<String, Object> dataMap;
                try {
                     dataMap = snakeYaml.load(deployData.getData());
                }
                catch (Exception ex) {
                    // Loading에 실패하면 DATA Format 오류로 판단..
                    throw new CocktailException("Invalid YAML data format : " + deployData.getType(), ExceptionType.InvalidParameter);
                }

                String namespace = ((LinkedHashMap)dataMap.get(KubeConstants.META)).get(KubeConstants.META_NAMESPACE).toString();
                String name = ((LinkedHashMap)dataMap.get(KubeConstants.META)).get(KubeConstants.NAME).toString();

                // TODO Redion : Acloud에서 지원하는 Workload Version set과 다른 버전으로 요청시 예외 처리. 필요함..
//                String apiVersion = dataMap.get(KubeConstants.APIVSERION).toString();
//                String kind = dataMap.get(KubeConstants.KIND).toString();
//                Pair<String, String> workloadVersion = Pair.of(kind, apiVersion);

                // 현재 Namespace와 YAML 파일안의 Namespace 정보가 다르면 배포할 수 없도록 처리.. (Appmap간 이동 불가)
                if(!deployData.getNamespace().equalsIgnoreCase(namespace)) {
                    throw new CocktailException("namespace can't be changed.", ExceptionType.InvalidParameter);
                }

                // 현재 Name과 YAML 파일안의 Name 정보가 다르면 배포할 수 없도록 처리.. (이름은 변경할 수 없음.)
                if(!deployData.getName().equalsIgnoreCase(name)) {
                    throw new CocktailException("name can't be changed.", ExceptionType.InvalidParameter);
                }

                Object obj = Yaml.load(deployData.getData());
                String resourceClassName = obj.getClass().getSimpleName();
                switch (resourceClassName) {
                    case "V1Deployment" :   // replace
                        V1Deployment v1Deployment = (V1Deployment)obj;
                        v1Deployment.setStatus(null);
                        v1Deployment.getMetadata().setResourceVersion(null);
                        v1Deployment = k8sWorker.replaceDeploymentV1(cluster, namespace, name, v1Deployment);
                        break;
                    case "V1DaemonSet" :    // replace
                        V1DaemonSet v1DaemonSet = (V1DaemonSet)obj;
                        v1DaemonSet.setStatus(null);
                        v1DaemonSet.getMetadata().setResourceVersion(null);
                        v1DaemonSet = k8sWorker.replaceDaemonSetV1(cluster, namespace, name, v1DaemonSet);
                        break;
                    case "V1Service" :      // replace
                        V1Service v1Service = (V1Service)obj;
                        v1Service.setStatus(null);
                        v1Service.getMetadata().setResourceVersion(null);
                        v1Service = k8sWorker.replaceServiceV1(cluster, namespace, name, v1Service);
                        break;
                    case "V1ConfigMap" :    // replace
                        V1ConfigMap v1ConfigMap = (V1ConfigMap)obj;
                        v1ConfigMap.getMetadata().setResourceVersion(null);
                        v1ConfigMap = k8sWorker.replaceConfigMapV1(cluster, namespace, name, v1ConfigMap);
                        break;
                    case "V1Secret" :       // replace
                        V1Secret v1Secret = (V1Secret)obj;
                        v1Secret.getMetadata().setResourceVersion(null);
                        v1Secret = k8sWorker.replaceSecretV1(cluster, namespace, name, v1Secret, false);
                        break;
                    case "V1PersistentVolumeClaim" :
                        V1PersistentVolumeClaim v1PersistentVolumeClaim = (V1PersistentVolumeClaim)obj;
                        v1PersistentVolumeClaim.setStatus(null);
                        v1PersistentVolumeClaim.getMetadata().setResourceVersion(null);
                        v1PersistentVolumeClaim = k8sWorker.replacePersistentVolumeClaimV1(cluster, namespace, name, v1PersistentVolumeClaim);
                        break;
                    case "V1PersistentVolume" :
                        V1PersistentVolume v1PersistentVolume = (V1PersistentVolume)obj;
                        v1PersistentVolume.setStatus(null);
                        v1PersistentVolume.getMetadata().setResourceVersion(null);
                        v1PersistentVolume = k8sWorker.replacePersistentVolumeV1(cluster, name, v1PersistentVolume);
                        break;
                    default :
                        break;
                }
                Thread.sleep(100);
            }
            else if(deployData.getType().equalsIgnoreCase("json")) {
                throw new CocktailException("json type is not yet supported.", ExceptionType.InvalidParameter);
            }
            else {
                throw new CocktailException("Spec Data type mismatch : " + deployData.getType(), ExceptionType.InvalidParameter);
            }
        } catch (Exception e) {
            if (e instanceof CocktailException) {
                throw e;
            }
            else {
                throw new CocktailException("specDeploy fail!!", e, ExceptionType.K8sCocktailCloudUpdateFail);
            }
        }

        return deployData;
    }


    public Map<String, Boolean> getFeatureGates(ClusterVO cluster) throws Exception {
        String namespace = "kube-system";
        String labelSelector = "component=kube-apiserver";

        String targetContainerName = "kube-apiserver";
        String searchWord = "feature-gates=";

        Map<String, Boolean> featureGatesMap = null;
        List<V1Pod> podList = k8sWorker.getPodV1WithLabel(cluster, namespace, null, labelSelector );
        if( CollectionUtils.isNotEmpty(podList) ){
            V1Pod pod = podList.get(0);
            Optional<V1Container> containerOptional = pod.getSpec().getContainers().stream().filter(c -> targetContainerName.equalsIgnoreCase(c.getName())).findFirst();

            if( containerOptional.isPresent() ){
                V1Container container = containerOptional.get();

                if( CollectionUtils.isNotEmpty(container.getCommand()) ){
                    // featureGates 포함 명령어만 추출
                    Optional<String> optionalFeatureGate = container.getCommand().stream().filter(cmd -> cmd.indexOf(searchWord) > -1).findFirst();

                    // featureGates 존재할때만 처리
                    if(optionalFeatureGate.isPresent()){
                        featureGatesMap = new HashMap<>(); // 변수 초기화

                        String featureGateCmdStr = optionalFeatureGate.get();
                        // 명령어에서 feature-gates의 값만 추출
                        featureGateCmdStr = featureGateCmdStr.substring(featureGateCmdStr.indexOf(searchWord)+searchWord.length());
                        // feature-gates 값을 ',' 로 구분
                        List<String> cmdList = Arrays.asList(StringUtils.split(featureGateCmdStr, ","));

                        // 추출한 값을 Map에 저장
                        for(String cmd : cmdList){
                            String[] featureGateArray = StringUtils.split(cmd, "=");
                            featureGatesMap.put(featureGateArray[0].trim(), Boolean.valueOf(featureGateArray[1].trim()));
                        }
                    }
                }
            }
        }
        return featureGatesMap;
    }

    public Set<String> getServicemapNamespacesByService(Integer serviceSeq) throws Exception {
        Set<String> servicemapNamespaces = new HashSet<>();
        if (serviceSeq != null) {
            IServicemapMapper servicemapDao = sqlSession.getMapper(IServicemapMapper.class);
            List<ServiceMonitoringVO> serviceServicemaps = servicemapDao.getServicemapListOfService(serviceSeq);
            servicemapNamespaces = Optional.ofNullable(serviceServicemaps).orElseGet(() ->Lists.newArrayList()).stream().map(sa -> (sa.getNamespaceName())).collect(Collectors.toSet());
        }

        return servicemapNamespaces;
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

    public VersionInfo getClusterVersionInfo(ClusterVO cluster) throws Exception {
        return k8sWorker.getVersion(cluster);
    }

//    private Long castMonitoringValueToLong(Object valueObj) throws Exception{
//        if(valueObj != null){
//            if(valueObj instanceof Long){
//                return (Long)valueObj;
//            }else {
//                return ((Integer)valueObj).longValue();
//            }
//        }
//
//        return 0L;
//    }
//
//    private Double castMonitoringValueToDouble(Object valueObj) throws Exception{
//        if(valueObj != null){
//            if(valueObj instanceof Double){
//                return (Double)valueObj;
//            }else {
//                return ((Integer)valueObj).doubleValue();
//            }
//        }
//
//        return 0.0D;
//    }

}
