package run.acloud.api.cserver.service;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import io.kubernetes.client.openapi.JSON;
import io.kubernetes.client.openapi.models.*;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import run.acloud.api.configuration.constants.AddonConstants;
import run.acloud.api.configuration.dao.IClusterMapper;
import run.acloud.api.configuration.service.AddonService;
import run.acloud.api.configuration.vo.ClusterVO;
import run.acloud.api.cserver.dao.IServicemapMapper;
import run.acloud.api.cserver.enums.*;
import run.acloud.api.cserver.util.ServerUtils;
import run.acloud.api.cserver.vo.ServerGuiVO;
import run.acloud.api.cserver.vo.ServicemapVO;
import run.acloud.api.k8sextended.models.V1beta1CronJob;
import run.acloud.api.k8sextended.models.V2beta1HorizontalPodAutoscaler;
import run.acloud.api.pipelineflow.service.PipelineFlowService;
import run.acloud.api.resource.constants.KubeConstants;
import run.acloud.api.resource.dao.IComponentMapper;
import run.acloud.api.resource.enums.*;
import run.acloud.api.resource.service.CRDResourceService;
import run.acloud.api.resource.service.K8sResourceService;
import run.acloud.api.resource.service.PersistentVolumeService;
import run.acloud.api.resource.service.WorkloadResourceService;
import run.acloud.api.resource.task.K8sWorker;
import run.acloud.api.resource.util.ResourceUtil;
import run.acloud.api.resource.vo.*;
import run.acloud.commons.vo.ExecutingContextVO;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ServerValidService {

    @Resource(name = "cocktailSession")
    private SqlSessionTemplate sqlSession;

    @Autowired
    private K8sResourceService k8sResourceService;

    @Autowired
    private CRDResourceService crdResourceService;

    @Autowired
    private K8sWorker k8sWorker;

    @Autowired
    private AddonService addonService;

    @Autowired
    private PipelineFlowService pipelineFlowService;

    @Autowired
    private ServerService serverService;

    @Autowired
    private PersistentVolumeService persistentVolumeService;

    @Autowired
    private WorkloadResourceService workloadResourceService;

    /**
     * #################################################################################################################################################################################################
     * #################################################################################################################################################################################################
     * start - validation
     * #################################################################################################################################################################################################
     * #################################################################################################################################################################################################
     */


    /**
     * Redion.Package : Package Type Workload일 경우 Cluster 전체에서 중복 체크 하기 위해 Method Overloading 처리..
     * (WorkloadType Parameter를 추가하여 Package Type일 경우 처리를 분기할 수 있도록 함..)
     * @param servicemapSeq
     * @param serverName
     * @param isAdd
     * @param isThrow
     * @return
     * @throws Exception
     */
    public boolean checkServerNameIfExists(Integer servicemapSeq, String serverName, boolean isAdd, boolean isThrow) throws Exception {
        return this.checkServerNameIfExistsByServicemapSeq(servicemapSeq, serverName, isAdd, isThrow, null);
    }

    /**
     * 서버명 중복 체크. (Cluster Seq, ServerName을 기준으로 Cluster 내에서 Unique한지 판단.)
     * @param serverName
     * @param clusterSeq
     * @param isAdd
     * @param isThrow
     * @return
     * @throws Exception
     */
    @Deprecated
    public boolean checkServerNameIfExistsInCluster(String serverName, Integer clusterSeq, boolean isAdd, boolean isThrow) throws Exception {
        IComponentMapper componentDao = this.sqlSession.getMapper(IComponentMapper.class);
        IClusterMapper clusterDao = this.sqlSession.getMapper(IClusterMapper.class);

        List<ComponentVO> components = componentDao.getComponentListInClusterByName(serverName, clusterSeq);

        ClusterVO cluster = clusterDao.getCluster(clusterSeq);

        return this.checkServerNameIfExists(serverName, components, cluster, isAdd, isThrow, null);
    }

    /**
     * 서버명 중복 체크
     *
     * @param servicemapSeq
     * @param serverName
     * @param isAdd - true : 생성, false : 수정
     * @param isThrow
     * @throws Exception
     */

    public boolean checkServerNameIfExists(Integer servicemapSeq, String serverName, boolean isAdd, boolean isThrow, WorkloadType workloadType) throws Exception {
        IComponentMapper componentDao = this.sqlSession.getMapper(IComponentMapper.class);
        IClusterMapper clusterDao = this.sqlSession.getMapper(IClusterMapper.class);

        List<ComponentVO> components;
        components = componentDao.getComponentsInServicemapByName(serverName, servicemapSeq);
        ClusterVO cluster = clusterDao.getClusterByServicemap(servicemapSeq);

        return this.checkServerNameIfExists(serverName, components, cluster, isAdd, isThrow, workloadType);
    }

    public boolean checkServerNameIfExistsByServicemapSeq(Integer servicemapSeq, String serverName, boolean isAdd, boolean isThrow, WorkloadType workloadType) throws Exception {
        IComponentMapper componentDao = this.sqlSession.getMapper(IComponentMapper.class);
        IClusterMapper clusterDao = this.sqlSession.getMapper(IClusterMapper.class);

        List<ComponentVO> components;
        components = componentDao.getComponentsInServicemapByName(serverName, servicemapSeq);
        ClusterVO cluster = clusterDao.getClusterByServicemap(servicemapSeq);

        return this.checkServerNameIfExists(serverName, components, cluster, isAdd, isThrow, workloadType);
    }

    /**
     * 서버명 중복 체크
     *
     * @param appmapSeq
     * @param serverName
     * @param components - 해당 appmap안의 serverName으로 조회한 components
     * @param isAdd
     * @param isThrow
     * @return
     * @throws Exception
     */
    public boolean checkServerNameIfExists(Integer appmapSeq, String serverName, List<ComponentVO> components, boolean isAdd, boolean isThrow, WorkloadType workloadType) throws Exception {
        IClusterMapper clusterDao = this.sqlSession.getMapper(IClusterMapper.class);

        ClusterVO cluster = clusterDao.getClusterByServicemap(appmapSeq);

        return this.checkServerNameIfExists(serverName, components, cluster, isAdd, isThrow, workloadType);
    }

    private boolean checkServerNameIfExists(String serverName, List<ComponentVO> components, ClusterVO cluster, boolean isAdd, boolean isThrow, WorkloadType workloadType) throws Exception{
        boolean isUsed = true;

        if(isAdd){
            if(CollectionUtils.isNotEmpty(components) && components.size() > 0){
                log.debug("isAdd : {} - Check component name", isAdd);
            }else{
                isUsed = false;
            }
        }else{
            if(CollectionUtils.isNotEmpty(components)){
                if(components.size() > 1){
                    log.debug("redeployV2 - Check component name");
                }else {
                    if(components.size() == 1){
                        int stoppedCnt = 0;
                        int errorCnt = 0;
                        for(ComponentVO componentRow : components){
                            if(StringUtils.equals(componentRow.getStateCode(), StateCode.STOPPED.name())){
                                stoppedCnt++;
                            }
                            if(StringUtils.equals(componentRow.getStateCode(), StateCode.ERROR.name())){
                                errorCnt++;
                            }
                        }
                        if(stoppedCnt > 0 || errorCnt > 0){
                            isUsed = false;
                        }
                    }else{
                        isUsed = false;
                    }
                }
            }else{
                isUsed = false;
            }
        }

        if(!isUsed && isAdd){
            if (cluster != null) {
                isUsed = this.checkServerNameIfExists(cluster, cluster.getNamespaceName(), serverName, isThrow, workloadType);
            }
        }

        if(isUsed && isThrow){
            throw new CocktailException(String.format("Component name already used: %s", serverName), ExceptionType.ServerNameAlreadyExists);
        }

        return isUsed;
    }

    public boolean checkServerNameIfExists(ClusterVO cluster, String namespace, String serverName, boolean isThrow, WorkloadType workloadType) throws Exception {

        boolean isUsed = false;

        if (cluster != null){
            // Service Name 중복은 Namespace 내에서만 처리해도 됨..
            // Package로 만들어지는 Service 이름이 YAML 안에 적용되어 있어 패키지 type일 경우. 중복의 여지가 남아있게 됨..
            // 이는 Chart를 제작(?) 할때 Service 이름을 생성하는 규칙에 Workload 이름이 포함되도록 하여 회피하도록 하여야함.
            if(!isUsed) {
                // 원크로드 Service를 삭제하고 서비스를 생성하지 않음
//                Set<String> k8sServiceNameSet = new HashSet<>();
//                List<K8sServiceVO> k8sServices = k8sResourceService.getServices(cluster, namespace, null, null, ContextHolder.exeContext());
//                if (CollectionUtils.isNotEmpty(k8sServices)) {
//                    for (K8sServiceVO k8sServiceRow : k8sServices) {
//                        k8sServiceNameSet.add(k8sServiceRow.getServiceName());
//                    }
//
//                    isUsed = k8sServiceNameSet.contains(serverName);
//                }
                if (!isUsed) {
                    for (int i = 0, ie = 5; i < ie; i++) {
                        if (!isUsed) {
                            if ( i == 0) {
                                V1Deployment v1Deployment = k8sWorker.getDeploymentV1(cluster, namespace, serverName);
                                if (v1Deployment != null){
                                    isUsed = true;
                                    break;
                                }
                            } else if ( i == 1) {
                                V1Job v1Job = k8sWorker.getJobV1(cluster, namespace, serverName);
                                if (v1Job != null){
                                    isUsed = true;
                                    break;
                                }
                            } else if ( i == 2) {
                                V1beta1CronJob v1beta1CronJob = k8sWorker.getCronJobV1beta1(cluster, namespace, serverName);
                                if (v1beta1CronJob != null){
                                    isUsed = true;
                                    break;
                                }
                            } else if ( i == 3) {
                                V1StatefulSet v1StatefulSet = k8sWorker.getStatefulSetV1(cluster, namespace, serverName);
                                if (v1StatefulSet != null){
                                    isUsed = true;
                                    break;
                                }
                            } else if ( i == 4) {
                                V1DaemonSet v1DaemonSet = k8sWorker.getDaemonSetV1(cluster, namespace, serverName);
                                if (v1DaemonSet != null){
                                    isUsed = true;
                                    break;
                                }
                            }
                        }
                    }

//                        String podLabel = String.format("%s=%s", KubeConstants.LABELS_KEY, serverName);
//                        List<K8sPodVO> k8sPods = k8sResourceService.getPods(cluster.getClusterSeq(), null, cluster.getNamespaceName(), podLabel, ContextHolder.exeContext());
//                        if (CollectionUtils.isNotEmpty(k8sPods)){
//                            isUsed = true;
//                        }
                }

            }
        }

        if(isUsed && isThrow){
            throw new CocktailException(String.format("Component name already used: %s", serverName),
                    ExceptionType.ServerNameAlreadyExists);
        }

        return isUsed;
    }

    /**
     * workload가 존재하는지 체크
     *
     * @param clusterSeq
     * @param namespaceName
     * @param workloadName
     * @param includeStoppedWorkload - 중지된 서버도 포함하여 체크여부 (존재하지 않는 것으로 판단)
     * @param isThrow
     * @return
     * @throws Exception
     */
    public boolean checkServerIfExists(Integer clusterSeq, String namespaceName, String workloadName, boolean includeStoppedWorkload, boolean isThrow) throws Exception {
        ComponentVO component = serverService.getComponent(clusterSeq, namespaceName, workloadName);
        boolean isExistsWorkload = true;
        if(component == null){
            isExistsWorkload = false;
        } else {
            if (includeStoppedWorkload && StateCode.valueOf(component.getStateCode()) == StateCode.STOPPED) {
                isExistsWorkload = false;
            }
        }
        if (!isExistsWorkload && isThrow) {
            throw new CocktailException("Does not exists workload!!!", ExceptionType.ServerNotFound);
        }

        return isExistsWorkload;
    }

    /**
     * 컨테이너명 중복 체크
     *
     * @param containers
     * @throws Exception
     */
    public void checkContainerNameIfExists(List<ContainerVO> initContainers, List<ContainerVO> containers) throws Exception{
        if(CollectionUtils.isNotEmpty(initContainers) || CollectionUtils.isNotEmpty(containers)){
            Set<String> containerNames = new HashSet<>();

            List<ContainerVO> allContainers = new ArrayList<>();
            ResourceUtil.mergeContainer(allContainers, initContainers, containers);

            for(ContainerVO containerRow : allContainers){
                if(containerNames.contains(containerRow.getContainerName())){
                    throw new CocktailException(String.format("Container name duplicated: %s", containerRow.getContainerName()),
                            ExceptionType.ContainerNameAlreadyExists);
                }else{
                    containerNames.add(containerRow.getContainerName());
                }
            }
        }
    }


    /**
     * Host Port 유효성 체크
     *
     * @param serverParam 서버 파라미터
     * @param nodePorts k8s service - node port
     * @param cluster cluster info
     * @return
     * @throws Exception
     */
    public ServerGuiVO validateHostPort(ServerGuiVO serverParam, Set<Integer> nodePorts, ClusterVO cluster) throws Exception {

        this.validateHostPort(serverParam.getServices(), nodePorts, cluster);

        return serverParam;
    }

    public void validateHostPort(List<ServiceSpecGuiVO> serviceSpecs, Set<Integer> nodePorts, ClusterVO cluster) throws Exception {
        Map<PortType, Set<Integer>> portsByTypes = new HashMap<>();
        Set<Integer> appointNodePorts = new HashSet<>();
        int appointNodePortCnt = 0;

        if(CollectionUtils.isNotEmpty(serviceSpecs)){
            for (ServiceSpecGuiVO serviceSpecRow : serviceSpecs){
                // sticky session : timeout 값 체크
                if(serviceSpecRow.getStickySessionFlag() != null && serviceSpecRow.getStickySessionFlag().booleanValue()){
                    if(serviceSpecRow.getStickySessionTimeoutSeconds() == null){
                        serviceSpecRow.setStickySessionTimeoutSeconds(10800);
                    }else{
                        if(!(serviceSpecRow.getStickySessionTimeoutSeconds().intValue() > 0 && serviceSpecRow.getStickySessionTimeoutSeconds().intValue() <= 86400)){
                            throw new CocktailException("Sticky Session timeout is out of range.", ExceptionType.ServerStickySessionTimeoutOutOfRange);
                        }
                    }
                }else{
                    serviceSpecRow.setStickySessionTimeoutSeconds(null);
                }

                PortType portType = PortType.valueOf(serviceSpecRow.getServiceType());

                if(CollectionUtils.isNotEmpty(serviceSpecRow.getServicePorts())) {
                    for(ServicePortVO p : serviceSpecRow.getServicePorts()){
                        if(StringUtils.isBlank(p.getName())){
                            p.setName(ResourceUtil.makePortName());
                        }
                        /** 2020.01.10 : Redion : targetPort가 규격을 준수하는지 체크 : 상세 내용은 호출 메서드 참고. **/
                        ResourceUtil.isValidPortRule(p.getTargetPort());
                    }

                    if (portType == PortType.NODE_PORT) {
                        appointNodePorts = new HashSet<>();
                        appointNodePortCnt = 0;
                        for (ServicePortVO servicePortRow : serviceSpecRow.getServicePorts()) {
                            if (servicePortRow.getNodePort() != null) {
                                if (cluster.getNodePortRange() != null) {
                                    String[] nodePortRange = StringUtils.split(cluster.getNodePortRange(), "-");
                                    int startNodePort = Integer.parseInt(nodePortRange[0]);
                                    int endNodePort = Integer.parseInt(nodePortRange[1]);
                                    if (startNodePort > servicePortRow.getNodePort().intValue() || endNodePort < servicePortRow.getNodePort().intValue()) {
                                        throw new CocktailException("Node port out of range.", ExceptionType.NodePortOutOfRange);
                                    }
                                    else {
                                        appointNodePorts.add(servicePortRow.getNodePort());
                                        appointNodePortCnt++;
                                    }
                                }
                            }
                        }

                        // 지정 노드포트 검사 후, 오류처리
                        if (appointNodePortCnt != 0 && !appointNodePorts.isEmpty()) {
                            boolean isDuplicated = false;

                            if (appointNodePortCnt != appointNodePorts.size()) {
                                isDuplicated = true;
                            }
                            else {

                                /**
                                 * k8s 서비스 조회 후, 중복체크
                                 */
                                if (!isDuplicated) {
                                    isDuplicated = this.checkDuplicatedNodePortOfCluster(nodePorts, appointNodePorts);
                                }
                            }

                            if (isDuplicated) {
                                throw new CocktailException("Node port is duplicated.", ExceptionType.NodePortDuplicated, "Node port is duplicated.");
                            }
                        }
                    }
                }
                else {
                    throw new CocktailException("Service port is required.", ExceptionType.InvalidParameter_Empty, "Service port is required.");
                }
            }
        }

    }

    /**
     * 노드포트 사용여부 체크
     *
     * @param nodePorts - 사용중인 노드포트
     * @param containerPorts - 체크할 포트
     * @return
     * @throws Exception
     */
    public boolean checkDuplicatedNodePortOfCluster(Set<Integer> nodePorts, Set<Integer> containerPorts) throws Exception{

        if(CollectionUtils.isNotEmpty(nodePorts)){
            Set<Integer> a;
            Set<Integer> b;
            if (nodePorts.size() <= containerPorts.size()) {
                a = nodePorts;
                b = containerPorts;
            } else {
                a = containerPorts;
                b = nodePorts;
            }
            for (Integer e : a) {
                if (b.contains(e)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * addon 으로 feature 지원 여부 판단
     */
    public void checkAddonPluginSupported(ServerGuiVO serverAdd, ClusterVO cluster) throws Exception {
        /**
         * addon 으로 feature 지원 여부 판단
         */
        String labels = String.format("%s in (%s,%s,%s)", KubeConstants.LABELS_ADDON_CHART_KEY, AddonConstants.CHART_NAME_MULTI_NIC, AddonConstants.CHART_NAME_SR_IOV, AddonConstants.CHART_NAME_GPU);
        List<String> addonNames = addonService.getAddonNames(cluster, labels);
        if (CollectionUtils.isNotEmpty(addonNames)) {

            if (serverAdd != null) {
                // Multus
                if (CollectionUtils.isNotEmpty(serverAdd.getServer().getPodNetworks())) {
                    if (!addonNames.contains(AddonConstants.CHART_NAME_MULTI_NIC)) {
                        throw new CocktailException("Multi-nic is not supported.", ExceptionType.MultiNicNotSupported);
                    }
//                    if (serverAdd.getServer().getAnnotations().containsKey(KubeConstants.META_ANNOTATIONS_CNI_NETWORKS)) {
//                        if (!addonNames.contains("")) {
//                            throw new CocktailException("Sriov is not supported.", ExceptionType.SriovNotSupported);
//                        }
//                    }
                }
                // GPU
                boolean useGpu = Optional.ofNullable(serverAdd.getContainers()).orElseGet(() ->Lists.newArrayList()).stream().anyMatch( c -> Boolean.TRUE.equals(c.getResources().getUseGpu()) );
                useGpu = useGpu ? useGpu : Optional.ofNullable(serverAdd.getInitContainers()).orElseGet(() ->Lists.newArrayList()).stream().anyMatch( c -> Boolean.TRUE.equals(c.getResources().getUseGpu()) );
                if (useGpu) {
                    if (!addonNames.contains(AddonConstants.CHART_NAME_GPU)) {
                        throw new CocktailException("GPU is not supported.", ExceptionType.GpuNotSupported);
                    }
                }

                // SCTP
                if (CollectionUtils.isNotEmpty(serverAdd.getServices()) && CollectionUtils.isNotEmpty(serverAdd.getServices().get(0).getServicePorts())) {
                    Set<String> protocolSet = serverAdd.getServices().get(0).getServicePorts().stream().map(sp -> (sp.getProtocol())).collect(Collectors.toSet());
                    if (protocolSet.contains("SCTP")) {
                        if (!K8sApiVerType.isK8sVerSupported(cluster.getK8sVersion(), K8sApiVerType.V1_19)) {
                            Map<String, Boolean> featureGates = k8sResourceService.getFeatureGates(cluster);
                            if(MapUtils.isEmpty(featureGates)) {
                                throw new CocktailException("SCTP is not supported.", ExceptionType.SctpNotSupported);
                            }else{
                                if(!MapUtils.getBooleanValue(featureGates, "SCTPSupport", false)){
                                    throw new CocktailException("SCTP is not supported.", ExceptionType.SctpNotSupported);
                                }
                            }
                        }
                    }
                }
            }

        }
    }

    /**
     * Multi-nic 관련 resource 수 셋팅
     *
     * @param serverParam
     * @param cluster
     * @throws Exception
     */
    public void setMultiNic(ServerGuiVO serverParam, ClusterVO cluster) throws Exception {

        if (serverParam != null && serverParam.getServer() != null && CollectionUtils.isNotEmpty(serverParam.getServer().getPodNetworks()) && cluster != null) {
            // annotation (k8s.v1.cni.cncf.io/networks) 에 설정된 이름을 기반으로 net-attach-def의 resource name (k8s.v1.cni.cncf.io/resourceName: intel.com/intel_sriov) 별 갯수를 구함.
            // e.g) k8s.v1.cni.cncf.io/networks: '[{"name": "sriov-net1"}]' -> k8s.v1.cni.cncf.io/resourceName: intel.com/intel_sriov
            List<String> podNetworkNames =  serverParam.getServer().getPodNetworks().stream().map(pn -> (pn.get("name"))).collect(Collectors.toList());
            Map<String, List<String>> resourceNetworkMap = new HashMap<>(); // resource name 별 net-attach-def name 목록
            List<Map<String, Object>> results = crdResourceService.getCustomObjects(cluster, cluster.getNamespaceName(), K8sApiKindType.NETWORK_ATTACHMENT_DEFINITION, null);
            if (CollectionUtils.isNotEmpty(results)) {
                for (Map<String, Object> resultRow : results) {
                    K8sCRDNetAttachDefGuiVO netAttachDef = new K8sCRDNetAttachDefGuiVO();
                    crdResourceService.convertNetAttachDef(resultRow, netAttachDef);

                    if (podNetworkNames.contains(netAttachDef.getName())) {
                        if ( MapUtils.getObject(resourceNetworkMap, netAttachDef.getAnnotations().get(KubeConstants.META_ANNOTATIONS_CNI_RESOURCE_NAME), null) == null ) {
                            resourceNetworkMap.put(netAttachDef.getAnnotations().get(KubeConstants.META_ANNOTATIONS_CNI_RESOURCE_NAME), Lists.newArrayList());
                        }
                        resourceNetworkMap.get(netAttachDef.getAnnotations().get(KubeConstants.META_ANNOTATIONS_CNI_RESOURCE_NAME)).add(netAttachDef.getName());
                    }
                }

                // Network resource name 별로 갯수를 container > resource에 셋팅
                if (MapUtils.isNotEmpty(resourceNetworkMap)) {
                    Map<String, String> resourceNetwork = Maps.newHashMap();
                    for (Map.Entry<String, List<String>> resourceRow : resourceNetworkMap.entrySet()) {
                        resourceNetwork.put(resourceRow.getKey(), String.valueOf(resourceRow.getValue().size()));
                    }
                    if (CollectionUtils.isNotEmpty(serverParam.getInitContainers())) {
                        for (ContainerVO cRow : serverParam.getInitContainers()) {
                            if(Optional.ofNullable(cRow).map(ContainerVO::getResources).map(ContainerResourcesVO::getRequests).orElseGet(() ->null) != null) {
                                cRow.getResources().getRequests().setNetwork(resourceNetwork);
                            }
                            if(Optional.ofNullable(cRow).map(ContainerVO::getResources).map(ContainerResourcesVO::getLimits).orElseGet(() ->null) != null) {
                                cRow.getResources().getLimits().setNetwork(resourceNetwork);
                            }
                        }
                    }
                    if (CollectionUtils.isNotEmpty(serverParam.getContainers())) {
                        for (ContainerVO cRow : serverParam.getContainers()) {
                            if(Optional.ofNullable(cRow).map(ContainerVO::getResources).map(ContainerResourcesVO::getRequests).orElseGet(() ->null) != null) {
                                cRow.getResources().getRequests().setNetwork(resourceNetwork);
                            }
                            if(Optional.ofNullable(cRow).map(ContainerVO::getResources).map(ContainerResourcesVO::getLimits).orElseGet(() ->null) != null) {
                                cRow.getResources().getLimits().setNetwork(resourceNetwork);
                            }
                        }
                    }
                }
            }
        } else {
            if (CollectionUtils.isNotEmpty(serverParam.getInitContainers())) {
                for (ContainerVO cRow : serverParam.getInitContainers()) {
                    if(Optional.ofNullable(cRow).map(ContainerVO::getResources).map(ContainerResourcesVO::getRequests).orElseGet(() ->null) != null) {
                        cRow.getResources().getRequests().setNetwork(null);
                    }
                    if(Optional.ofNullable(cRow).map(ContainerVO::getResources).map(ContainerResourcesVO::getLimits).orElseGet(() ->null) != null) {
                        cRow.getResources().getLimits().setNetwork(null);
                    }
                }
            }
            if (CollectionUtils.isNotEmpty(serverParam.getContainers())) {
                for (ContainerVO cRow : serverParam.getContainers()) {
                    if(Optional.ofNullable(cRow).map(ContainerVO::getResources).map(ContainerResourcesVO::getRequests).orElseGet(() ->null) != null) {
                        cRow.getResources().getRequests().setNetwork(null);
                    }
                    if(Optional.ofNullable(cRow).map(ContainerVO::getResources).map(ContainerResourcesVO::getLimits).orElseGet(() ->null) != null) {
                        cRow.getResources().getLimits().setNetwork(null);
                    }
                }
            }
        }

    }

    public void processVolumeRequest(ClusterVO cluster, Integer servicemapSeq, ServerGuiVO serverParam, boolean isDeployment, ExecutingContextVO context) throws Exception {
        IServicemapMapper servicemapDao = sqlSession.getMapper(IServicemapMapper.class);
        ServicemapVO servicemap = servicemapDao.getServicemap(servicemapSeq, null);

        this.processVolumeRequest(cluster, servicemap.getNamespaceName(), serverParam, isDeployment, context);
    }

    /**
     * PV 생성 및 PVC Name 생성
     * Validation 체크
     *
     * @param cluster
     * @param serverParam
     * @param isDeployment - 배포정보 단계에서의 작업이면 true, k8s단계이면 false
     * @throws Exception
     */
    public void processVolumeRequest(ClusterVO cluster, String namespace, ServerGuiVO serverParam, boolean isDeployment, ExecutingContextVO context) throws Exception {
        // check volume
        List<String> volumeNames = new ArrayList<>();
        List<String> volumeTemplateNames = new ArrayList<>();
        List<ContainerVolumeVO> volumes = serverParam.getVolumes();
        if (volumes != null) {
            String volumeName;

            // JsonPath config
            Configuration conf = Configuration.defaultConfiguration().addOptions(Option.DEFAULT_PATH_LEAF_TO_NULL);

            List<K8sPersistentVolumeClaimVO> k8sPersistentVolumeClaims = null;
            List<K8sDeploymentVO> k8sDeployments = null;
            if(StringUtils.isNotBlank(namespace)){
                // namespace > pvc 조회
                cluster.setNamespaceName(namespace);

                String labelSelector = String.format("%s,%s=%s", KubeConstants.LABELS_COCKTAIL_KEY, KubeConstants.CUSTOM_VOLUME_TYPE, VolumeType.PERSISTENT_VOLUME_LINKED.getCode());
                k8sPersistentVolumeClaims = persistentVolumeService.getPersistentVolumeClaims(cluster, cluster.getNamespaceName(), null, labelSelector, context);

                // namespace > deployment 조회
                String fieldSelector = null;
                if(serverParam.getComponent().getComponentSeq() != null) {
                    fieldSelector = String.format("%s.%s!=%s", KubeConstants.META, KubeConstants.NAME, serverParam.getComponent().getComponentName());
                }
                k8sDeployments = workloadResourceService.getDeployments(cluster, cluster.getNamespaceName(), fieldSelector, null, context);
            }

            for (ContainerVolumeVO containerVolume : volumes) {
                if (StringUtils.isEmpty(containerVolume.getVolumeName())) {
                    throw new CocktailException("Volume name is null or empty", ExceptionType.ServerVolumeConfigInvalid_VolumeNameIsNull, serverParam);
                }

                containerVolume.setVolumeName(ResourceUtil.getUniqueName(containerVolume.getVolumeName()));
                if (volumeNames.contains(containerVolume.getVolumeName())) {
                    throw new CocktailException(String.format("Volume name already exists: %s", containerVolume.getVolumeName()), ExceptionType.ServerVolumeAlreadyExists, serverParam);
                } else {
                    volumeNames.add(containerVolume.getVolumeName());
                }

                VolumeType volumeType = containerVolume.getVolumeType();
                if(volumeType == VolumeType.PERSISTENT_VOLUME_LINKED){
                    if(StringUtils.isBlank(containerVolume.getPersistentVolumeClaimName())){
                        throw new CocktailException("PersistentVolumeClaimName is null or empty", ExceptionType.ServerVolumeConfigInvalid_LinkedPersistentVolumeClaimNameIsNull);
                    }else{
                        /**
                         * 해당 appmap(namespace)에서 생성된 pvc를 조회하여
                         * 접근모드가 RWO인 pvc를 다른 deployment에서 사용 중인지 체크
                         */
                        if(CollectionUtils.isNotEmpty(k8sPersistentVolumeClaims)){
                            for (K8sPersistentVolumeClaimVO k8sPersistentVolumeClaimRow : k8sPersistentVolumeClaims){
                                // pvc 명 비교하여 같고,
                                if(StringUtils.equals(k8sPersistentVolumeClaimRow.getName(), containerVolume.getPersistentVolumeClaimName())){
                                    // accessMode가 RWO라면,
                                    if(StringUtils.equalsIgnoreCase("ReadWriteOnce", k8sPersistentVolumeClaimRow.getDetail().getAccessModes().get(0))){
                                        if(WorkloadType.valueOf(serverParam.getServer().getWorkloadType()) == WorkloadType.REPLICA_SERVER
                                            && serverParam.getServer().getStrategy().getType() == DeploymentStrategyType.Recreate){
                                            // 다른 deployment에서 사용 중인지 체크 (k8s)
                                            if(CollectionUtils.isNotEmpty(k8sDeployments)){
                                                for(K8sDeploymentVO k8sDeploymentRow : k8sDeployments){
                                                    List<Map<String, Object>> deploymentVolumes = JsonPath.using(conf).parse(k8sDeploymentRow.getDeployment()).read("$.spec.template.spec.volumes", List.class);

                                                    if(CollectionUtils.isNotEmpty(deploymentVolumes)){
                                                        for(Map<String, Object> volumeRow : deploymentVolumes){
                                                            if(volumeRow.get("persistentVolumeClaim") != null){
                                                                if(StringUtils.equals(containerVolume.getPersistentVolumeClaimName(), ((Map<String, String>)volumeRow.get("persistentVolumeClaim")).get("claimName"))){
                                                                    if(StringUtils.equals(cluster.getNamespaceName(), k8sDeploymentRow.getNamespace())
                                                                            && !StringUtils.equals(serverParam.getComponent().getComponentName(), k8sDeploymentRow.getName())){
                                                                        throw new CocktailException("PersistentVolumeClaim is used!!", ExceptionType.K8sVolumeClaimIsUsingMount);
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }else{
                                            throw new CocktailException("PersistentVolumeClaim is not support, only Recreate!!", ExceptionType.NotSupportedServerType);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        List<ContainerVO> allContainers = new ArrayList<>();
        ResourceUtil.mergeContainer(allContainers, serverParam.getServer().getInitContainers(), serverParam.getServer().getContainers());

        if (CollectionUtils.isEmpty(allContainers)) {
            log.warn("Container not exists.");
            return;
        }
        if (WorkloadType.valueOf(serverParam.getServer().getWorkloadType()) == WorkloadType.STATEFUL_SET_SERVER
                && CollectionUtils.isNotEmpty(serverParam.getVolumeTemplates())) {
            for (PersistentVolumeClaimGuiVO pvcRow : serverParam.getVolumeTemplates()) {
                if (volumeTemplateNames.contains(pvcRow.getName())) {
                    throw new CocktailException(String.format("Volume name already exists: %s", pvcRow.getName()), ExceptionType.ServerVolumeAlreadyExists, serverParam);
                } else {
                    volumeTemplateNames.add(pvcRow.getName());
                }
            }
        }
        for (ContainerVO c : allContainers) {
            List<VolumeMountVO> mounts = c.getVolumeMounts();
            if (mounts == null) {
                continue;
            }

            for (VolumeMountVO m : mounts) {
                m.setVolumeName(ResourceUtil.getUniqueName(m.getVolumeName()));

                // SubPathExpr 클러스터 버전 체크
                if (StringUtils.isNotBlank(m.getSubPathExpr())) {
                    if (!K8sApiVerType.isK8sVerSupported(cluster.getK8sVersion(), K8sApiVerType.V1_15)) {
                        throw new CocktailException("SubPathExpr specification is not supported by the current cluster version.", ExceptionType.K8sNotSupported, "SubPathExpr specification is not supported by the current cluster version");
                    }
                }

                if (StringUtils.isEmpty(m.getVolumeName())) {
                    throw new CocktailException("Volume name(in container) is null or empty", ExceptionType.ServerVolumeConfigInvalid_VolumeNameIsNullInMount, serverParam);
                } else {
                    boolean isExists = false;

                    if (!volumeNames.contains(m.getVolumeName())) {
                        // StatefulSet 은 volumeTemplate의 name으로 mount volumeName을 설정하므로 volumeTemplate도 체크
                        if (WorkloadType.valueOf(serverParam.getServer().getWorkloadType()) == WorkloadType.STATEFUL_SET_SERVER) {
                            if (!volumeTemplateNames.contains(m.getVolumeName())) {
                                isExists = true;
                            }
                        } else {
                            isExists = true;
                        }
                    }

                    if (isExists) {
                        throw new CocktailException(String.format("Volume not exists: %s", m.getVolumeName()), ExceptionType.ServerVolumeConfigInvalid_VolumeNameIsNotExistsInMount, serverParam);
                    }
                }
            }
        }
    }

    /**
     * check api version
     *
     * @param workloadType
     * @param workloadVersion
     * @param clusterSeq
     * @param ctx
     * @throws Exception
     */
    public void checkServerApiVersion(String workloadType, String workloadVersion, Integer clusterSeq, ExecutingContextVO ctx) throws Exception {
        IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
        ClusterVO cluster = clusterDao.getCluster(clusterSeq);

        this.checkServerApiVersion(workloadType, workloadVersion, cluster, ctx);
    }

    /**
     * check api version
     *
     * @param workloadType
     * @param workloadVersion
     * @param cluster
     * @param ctx
     * @throws Exception
     */
    public void checkServerApiVersion(String workloadType, String workloadVersion, ClusterVO cluster, ExecutingContextVO ctx) throws Exception {

        if (StringUtils.isNotBlank(workloadType) && StringUtils.isNotBlank(workloadVersion)) {
            WorkloadVersionSet workloadVersionSet = WorkloadVersionSet.getSupport(WorkloadType.valueOf(workloadType), WorkloadVersion.valueOf(workloadVersion));

            if(workloadVersionSet != null){
                ctx.setWorkloadVersionSetMap(workloadVersionSet.getApiVerKindEnumSetMap());

                boolean isSupport = true;

                EnumSet<K8sApiVerType> k8sApiVerTypes = workloadVersionSet.getClusterVersion();
                List<String> k8sApiVerTypeStrs = k8sApiVerTypes.isEmpty() ? new ArrayList<>() : k8sApiVerTypes.stream().map(K8sApiVerType::getVersion).collect(Collectors.toList());
                if(CollectionUtils.isNotEmpty(k8sApiVerTypeStrs)){

                    if (!ResourceUtil.getK8sSupported(cluster.getK8sVersion(), k8sApiVerTypeStrs)) {
                        isSupport = false;
                    }
                }else{
                    isSupport = false;
                }

                if(!isSupport){
                    throw new CocktailException("This cluster is not support!!", ExceptionType.K8sNotSupported);
                }
            }else{
                throw new CocktailException("WorkloadType or WorkloadVersion is null!!", ExceptionType.InvalidParameter);
            }
        } else {
            throw new CocktailException("WorkloadType or WorkloadVersion is null!!", ExceptionType.InvalidParameter);
        }

    }

    public boolean checkWorkloadYaml(String namespace, WorkloadType workloadType, String workloadName, String yaml, JSON k8sJson) throws Exception {
        boolean isValid = true;
        if (k8sJson == null) {
            k8sJson = new JSON();
        }

        List<Object> objs = ServerUtils.getYamlObjects(yaml);

        if (CollectionUtils.isNotEmpty(objs)) {
            if (objs.size() > 2) {
                return false;
            }

            // kind 별로 map에 셋팅
            Map<K8sApiKindType, List<Object>> k8sApiKindMap = Maps.newHashMap();
            for (Object obj : objs) {
                K8sApiKindType kind = ServerUtils.getK8sKindInObject(obj, k8sJson);
                if (MapUtils.getObject(k8sApiKindMap, kind, null) == null) {
                    k8sApiKindMap.put(kind, Lists.newArrayList());
                }
                k8sApiKindMap.get(kind).add(obj);
                log.debug(run.acloud.api.k8sextended.util.Yaml.dump(obj));
            }

            K8sApiKindType k8sApiKindType = null;
            switch (workloadType) {
                case SINGLE_SERVER:
                case REPLICA_SERVER:
                    k8sApiKindType = K8sApiKindType.DEPLOYMENT;
                    log.info("### namespace : {}, workloadType : {}, name : {}, k8sApiKindType : {} - yaml count : deployment - {}, hpa - {}"
                            , namespace, workloadType, workloadName, k8sApiKindType
                            , Optional.ofNullable(k8sApiKindMap.get(k8sApiKindType)).orElseGet(() ->Lists.newArrayList()).size()
                            , Optional.ofNullable(k8sApiKindMap.get(K8sApiKindType.HORIZONTAL_POD_AUTOSCALER)).orElseGet(() ->Lists.newArrayList()).size()
                    );
                    break;
                case STATEFUL_SET_SERVER:
                    k8sApiKindType = K8sApiKindType.STATEFUL_SET;
                    log.info("### namespace : {}, workloadType : {}, name : {}, k8sApiKindType : {} - yaml count : statefulSet - {}"
                            , namespace, workloadType, workloadName, k8sApiKindType
                            , Optional.ofNullable(k8sApiKindMap.get(k8sApiKindType)).orElseGet(() ->Lists.newArrayList()).size()
                    );
                    break;
                case DAEMON_SET_SERVER:
                    k8sApiKindType = K8sApiKindType.DAEMON_SET;
                    log.info("### namespace : {}, workloadType : {}, name : {}, k8sApiKindType : {} - yaml count : daemonSet - {}"
                            , namespace, workloadType, workloadName, k8sApiKindType
                            , Optional.ofNullable(k8sApiKindMap.get(k8sApiKindType)).orElseGet(() ->Lists.newArrayList()).size()
                    );
                    break;
                case CRON_JOB_SERVER:
                    k8sApiKindType = K8sApiKindType.CRON_JOB;
                    log.info("### namespace : {}, workloadType : {}, name : {}, k8sApiKindType : {} - yaml count : cronJob - {}"
                            , namespace, workloadType, workloadName, k8sApiKindType
                            , Optional.ofNullable(k8sApiKindMap.get(k8sApiKindType)).orElseGet(() ->Lists.newArrayList()).size()
                    );
                    break;
                case JOB_SERVER:
                    k8sApiKindType = K8sApiKindType.JOB;
                    log.info("### namespace : {}, workloadType : {}, name : {}, k8sApiKindType : {} - yaml count : job - {}"
                            , namespace, workloadType, workloadName, k8sApiKindType
                            , Optional.ofNullable(k8sApiKindMap.get(k8sApiKindType)).orElseGet(() ->Lists.newArrayList()).size()
                    );
                    break;
            }

            if (k8sApiKindType != null) {
                if (MapUtils.isNotEmpty(k8sApiKindMap)) {
                    if (k8sApiKindType == K8sApiKindType.DEPLOYMENT && k8sApiKindMap.containsKey(K8sApiKindType.DEPLOYMENT)) {
                        if (k8sApiKindMap.containsKey(K8sApiKindType.HORIZONTAL_POD_AUTOSCALER)) {
                            if (k8sApiKindMap.size() == 2) {
                                if (MapUtils.getObject(k8sApiKindMap, K8sApiKindType.HORIZONTAL_POD_AUTOSCALER, null) != null) {
                                    if (k8sApiKindMap.get(K8sApiKindType.HORIZONTAL_POD_AUTOSCALER).size() != 1) {
                                        return false;
                                    }
                                } else {
                                    return false;
                                }
                            } else {
                                return false;
                            }
                        } else {
                            if (k8sApiKindMap.size() != 1) {
                                return false;
                            }
                        }

                        if (MapUtils.getObject(k8sApiKindMap, K8sApiKindType.DEPLOYMENT, null) != null) {
                            if (k8sApiKindMap.get(K8sApiKindType.DEPLOYMENT).size() != 1) {
                                return false;
                            } else {
                                if (!this.checkMetadata(namespace, workloadName, k8sApiKindMap.get(K8sApiKindType.DEPLOYMENT).get(0), k8sJson)) {
                                    return false;
                                }
                            }
                        } else {
                            return false;
                        }
                    } else {

                        if (k8sApiKindType == K8sApiKindType.STATEFUL_SET && k8sApiKindMap.containsKey(K8sApiKindType.STATEFUL_SET)) {
                            if (MapUtils.getObject(k8sApiKindMap, K8sApiKindType.STATEFUL_SET, null) != null) {
                                if (k8sApiKindMap.get(K8sApiKindType.STATEFUL_SET).size() != 1) {
                                    return false;
                                } else {
                                    Map<String, Object> k8sObjectMap = ServerUtils.getK8sObjectToMap(k8sApiKindMap.get(K8sApiKindType.STATEFUL_SET).get(0), k8sJson);
                                    if (!this.checkMetadata(namespace, workloadName, k8sObjectMap)) {
                                        return false;
                                    }
                                }
                            } else {
                                return false;
                            }

                        } else if (k8sApiKindType == K8sApiKindType.DAEMON_SET && k8sApiKindMap.containsKey(K8sApiKindType.DAEMON_SET)) {
                            if (MapUtils.getObject(k8sApiKindMap, K8sApiKindType.DAEMON_SET, null) != null) {
                                if (k8sApiKindMap.get(K8sApiKindType.DAEMON_SET).size() != 1) {
                                    return false;
                                } else {
                                    if (!this.checkMetadata(namespace, workloadName, k8sApiKindMap.get(K8sApiKindType.DAEMON_SET).get(0), k8sJson)) {
                                        return false;
                                    }
                                }
                            } else {
                                return false;
                            }
                        } else if (k8sApiKindType == K8sApiKindType.CRON_JOB && k8sApiKindMap.containsKey(K8sApiKindType.CRON_JOB)) {
                            if (MapUtils.getObject(k8sApiKindMap, K8sApiKindType.CRON_JOB, null) != null) {
                                if (k8sApiKindMap.get(K8sApiKindType.CRON_JOB).size() != 1) {
                                    return false;
                                } else {
                                    if (!this.checkMetadata(namespace, workloadName, k8sApiKindMap.get(K8sApiKindType.CRON_JOB).get(0), k8sJson)) {
                                        return false;
                                    }
                                }
                            } else {
                                return false;
                            }
                        } else if (k8sApiKindType == K8sApiKindType.JOB && k8sApiKindMap.containsKey(K8sApiKindType.JOB)) {
                            if (MapUtils.getObject(k8sApiKindMap, K8sApiKindType.JOB, null) != null) {
                                if (k8sApiKindMap.get(K8sApiKindType.JOB).size() != 1) {
                                    return false;
                                } else {
                                    if (!this.checkMetadata(namespace, workloadName, k8sApiKindMap.get(K8sApiKindType.JOB).get(0), k8sJson)) {
                                        return false;
                                    }
                                }
                            } else {
                                return false;
                            }
                        } else {
                            return false;
                        }
                    }

                }
            } else {
                return false;
            }
        }

        return isValid;
    }

    /**
     * check metadata(namespace, name)
     *
     * @param namespace
     * @param name
     * @param k8sObj
     * @param k8sJson
     * @return
     * @throws Exception
     */
    public boolean checkMetadata(String namespace, String name, Object k8sObj, JSON k8sJson) throws Exception {
        Map<String, Object> k8sObjectMap = ServerUtils.getK8sObjectToMap(k8sObj, k8sJson);
        return this.checkMetadata(namespace, name, k8sObjectMap);
    }
    public boolean checkMetadata(String namespace, String name, Map<String, Object> k8sObjectMap) throws Exception {
        if (MapUtils.isNotEmpty(k8sObjectMap) && MapUtils.getObject(k8sObjectMap, KubeConstants.META, null) != null) {
            Object k8sMetadataObj = k8sObjectMap.get(KubeConstants.META);
            if (k8sMetadataObj instanceof Map) {
                Map<String, String> metadata = (Map<String, String>)k8sMetadataObj;
                if (!StringUtils.equals(metadata.get(KubeConstants.META_NAMESPACE), namespace) || !StringUtils.equals(metadata.get(KubeConstants.NAME), name)) {
                    return false;
                }
            } else if (k8sMetadataObj instanceof V1ObjectMeta) {
                V1ObjectMeta metadata = (V1ObjectMeta)k8sMetadataObj;
                if (!StringUtils.equals(metadata.getNamespace(), namespace) || !StringUtils.equals(metadata.getName(), name)) {
                    return false;
                }
            } else {
                return false;
            }
        } else {
            return false;
        }

        return true;
    }

    public boolean checkHeadlessServiceForStatefulSet(String namespace, String serviceName, Map<K8sApiKindType, List<Object>> k8sApiKindMap, JSON k8sJson) throws Exception {
        if (k8sApiKindMap.containsKey(K8sApiKindType.SERVICE)) {
            if (k8sApiKindMap.size() != 2) {
                return false;
            }
            if (MapUtils.getObject(k8sApiKindMap, K8sApiKindType.SERVICE, null) != null) {
                if (k8sApiKindMap.get(K8sApiKindType.SERVICE).size() != 1) {
                    return false;
                } else {
                    Map<String, Object> k8sObjectMap = ServerUtils.getK8sObjectToMap(k8sApiKindMap.get(K8sApiKindType.SERVICE).get(0), k8sJson);
                    if (!this.checkMetadata(namespace, serviceName, k8sObjectMap, k8sJson)) {
                        return false;
                    }
                    if (MapUtils.isNotEmpty(k8sObjectMap) && MapUtils.getObject(k8sObjectMap, KubeConstants.SPEC, null) != null) {
                        Object k8sSpecObj = k8sObjectMap.get(KubeConstants.SPEC);
                        if (k8sSpecObj instanceof Map) {
                            Map<String, String> serviceSpec = (Map<String, String>)k8sSpecObj;
                            if (!StringUtils.equals("None", serviceSpec.get("clusterIP"))) {
                                return false;
                            }
                        } else if (k8sSpecObj instanceof V1ServiceSpec) {
                            V1ServiceSpec serviceSpec = (V1ServiceSpec)k8sSpecObj;
                            if (!StringUtils.equals("None", serviceSpec.getClusterIP())) {
                                return false;
                            }
                        } else {
                            return false;
                        }
                    } else {
                        return false;
                    }
                }
            } else {
                return false;
            }
        } else {
            return false;
        }

        return true;
    }

    /**
     * Pipeline이 Running중인지 여부 체크..
     * @param component
     * @throws Exception
     */
    public void checkPipelineOnRunning(ComponentVO component) throws Exception {
        if(pipelineFlowService.checkPipelineOnRunning(component)) {
            throw new CocktailException("Pipeline is running!!", ExceptionType.PipelineRunning);
        }

    }

    /**
     * TODO : 이후 진행
     *
     * @param affinity
     * @throws Exception
     */
    public void checkAffinity(AffinityVO affinity) throws Exception {
        if (affinity != null) {
            if (affinity.getNodeAffinity() != null) {
                if (CollectionUtils.isNotEmpty(affinity.getNodeAffinity().getPreferredDuringSchedulingIgnoredDuringExecution())) {
                    for (PreferredSchedulingTermVO termRow : affinity.getNodeAffinity().getPreferredDuringSchedulingIgnoredDuringExecution()) {
                        if (termRow.getPreference() != null || termRow.getWeight() != null) {

                        } else {

                        }
                    }
                }
            }
            if (affinity.getPodAffinity() != null) {

            }
            if (affinity.getPodAntiAffinity() != null) {

            }
        }
    }

    public boolean checkHpaNameIfExists(Integer clusterSeq, String namespaceName, String hpaName) throws Exception {
        IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
        ClusterVO cluster = clusterDao.getCluster(clusterSeq);

        return this.checkHpaNameIfExists(cluster, namespaceName, hpaName);
    }

    /**
     * Horizontal Pod Autoscaler 이름 중복 체크
     * @param cluster
     * @param namespaceName
     * @param hpaName
     * @return
     * @throws Exception
     */
    public boolean checkHpaNameIfExists(ClusterVO cluster, String namespaceName, String hpaName) throws Exception {
        boolean isUsed = false;

        if (cluster != null) {
            K8sApiVerKindType apiVerKindType = ResourceUtil.getApiVerKindType(cluster, K8sApiKindType.HORIZONTAL_POD_AUTOSCALER);

            if (apiVerKindType != null) {
                if(apiVerKindType.getGroupType() == K8sApiGroupType.AUTOSCALING && apiVerKindType.getApiType() == K8sApiType.V1){
                    V1HorizontalPodAutoscaler v1HorizontalPodAutoscaler = k8sWorker.getHorizontalPodAutoscalerV1(cluster, namespaceName, hpaName);
                    if(v1HorizontalPodAutoscaler != null) {
                        isUsed = true;
                    }
                }
                else if(apiVerKindType.getGroupType() == K8sApiGroupType.AUTOSCALING && apiVerKindType.getApiType() == K8sApiType.V2BETA1) {
                    V2beta1HorizontalPodAutoscaler v2beta1HorizontalPodAutoscaler = k8sWorker.getHorizontalPodAutoscalerV2beta1(cluster, namespaceName, hpaName);
                    if(v2beta1HorizontalPodAutoscaler != null) {
                        isUsed = true;
                    }
                }
                else if(apiVerKindType.getGroupType() == K8sApiGroupType.AUTOSCALING && apiVerKindType.getApiType() == K8sApiType.V2BETA2) {
                    V2beta2HorizontalPodAutoscaler v2beta2HorizontalPodAutoscaler = k8sWorker.getHorizontalPodAutoscalerV2beta2(cluster, namespaceName, hpaName);
                    if(v2beta2HorizontalPodAutoscaler != null) {
                        isUsed = true;
                    }
                }
                else if(apiVerKindType.getGroupType() == K8sApiGroupType.AUTOSCALING && apiVerKindType.getApiType() == K8sApiType.V2) {
                    V2HorizontalPodAutoscaler v2HorizontalPodAutoscaler = k8sWorker.getHorizontalPodAutoscalerV2(cluster, namespaceName, hpaName);
                    if(v2HorizontalPodAutoscaler != null) {
                        isUsed = true;
                    }
                }
            }

        }

        return isUsed;
    }

    /**
     * #################################################################################################################################################################################################
     * #################################################################################################################################################################################################
     * end - validation
     * #################################################################################################################################################################################################
     * #################################################################################################################################################################################################
     */
}
