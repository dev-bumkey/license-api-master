package run.acloud.api.cserver.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.JsonObject;
import com.jayway.jsonpath.TypeRef;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import run.acloud.api.configuration.dao.IClusterMapper;
import run.acloud.api.configuration.service.AddonCommonService;
import run.acloud.api.configuration.vo.ClusterVO;
import run.acloud.api.cserver.dao.IServiceMapper;
import run.acloud.api.cserver.dao.IServicemapMapper;
import run.acloud.api.cserver.enums.*;
import run.acloud.api.cserver.util.ServerUtils;
import run.acloud.api.cserver.vo.*;
import run.acloud.api.k8sextended.models.V1beta1CronJob;
import run.acloud.api.k8sextended.models.V1beta1CronJobStatus;
import run.acloud.api.k8sextended.models.V2beta1HorizontalPodAutoscaler;
import run.acloud.api.pipelineflow.dao.IPipelineFlowMapper;
import run.acloud.api.pipelineflow.enums.PipelineType;
import run.acloud.api.pipelineflow.vo.PipelineContainerVO;
import run.acloud.api.pipelineflow.vo.PipelineWorkloadVO;
import run.acloud.api.resource.constants.KubeConstants;
import run.acloud.api.resource.dao.IComponentMapper;
import run.acloud.api.resource.enums.K8sApiGroupType;
import run.acloud.api.resource.enums.K8sApiKindType;
import run.acloud.api.resource.enums.K8sApiType;
import run.acloud.api.resource.enums.K8sApiVerKindType;
import run.acloud.api.resource.service.*;
import run.acloud.api.resource.task.K8sPatchSpecFactory;
import run.acloud.api.resource.task.K8sWorker;
import run.acloud.api.resource.util.ResourceUtil;
import run.acloud.api.resource.vo.*;
import run.acloud.commons.constants.CommonConstants;
import run.acloud.commons.util.JsonUtils;
import run.acloud.commons.vo.ExecutingContextVO;
import run.acloud.framework.context.ContextHolder;
import run.acloud.framework.enums.ExceptionBiz;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ServerService {

    @Resource(name = "cocktailSession")
    private SqlSessionTemplate sqlSession;

    @Autowired
    private ServerConversionService serverConversionService;

    @Autowired
    private ServerValidService serverValidService;

    @Autowired
    private ServerAsyncProcessService serverAsyncProcessService;

    @Autowired
    private ComponentService componentService;

    @Autowired
    private AddonCommonService addonCommonService;

    @Autowired
    private K8sResourceService k8sResourceService;

    @Autowired
    private ServiceSpecService serviceSpecService;

    @Autowired
    private SecretService secretService;

    @Autowired
    private K8sWorker k8sWorker;

    @Autowired
    private ServerStateService serverStateService;

    @Autowired
    private PersistentVolumeService persistentVolumeService;

    @Autowired
    private StorageClassService storageClassService;

    @Autowired
    private WorkloadResourceService workloadResourceService;

    @Autowired
    private K8sPatchSpecFactory k8sPatchSpecFactory;

    /**
     * #################################################################################################################################################################################################
     * #################################################################################################################################################################################################
     * start - Workload
     * #################################################################################################################################################################################################
     * #################################################################################################################################################################################################
     */

    /**
     * 서버 생성. ServerController에서 호출
     * @param serverIntegrate
     * @return
     * @throws Exception
     */
	public ServerIntegrateVO addWorkload(Integer clusterSeq, String namespace, String workloadName, ServerIntegrateVO serverIntegrate, DeployMode deployMode, ExecutingContextVO context) throws Exception {
        log.info(String.format("[STEP1] addWorkload : Start Checking Datas : %s - %s - %s", clusterSeq, namespace, workloadName));

        IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
        IServiceMapper serviceDao = this.sqlSession.getMapper(IServiceMapper.class);

        DeployType deployType = DeployType.valueOf(serverIntegrate.getDeployType());

        ClusterVO cluster = clusterDao.getClusterByNamespace(clusterSeq, namespace);
        if (cluster == null) {
            cluster = clusterDao.getCluster(clusterSeq);
            if (cluster != null) {
                cluster.setNamespaceName(namespace);
            } else {
                throw new CocktailException("Invalid cluster info!!", ExceptionType.InvalidParameter, ExceptionBiz.SERVER);
            }
        }

        if (serverIntegrate != null) {
            ServerGuiVO serverGui = null;
            ServerYamlVO serverYaml = null;
            switch (deployType) {
                case GUI:
                    serverGui = (ServerGuiVO)serverIntegrate;
                    break;
                case YAML:
                    serverYaml = (ServerYamlVO)serverIntegrate;
                    serverGui = serverConversionService.convertYamlToGui(cluster, namespace, serverYaml.getWorkloadType(), serverYaml.getWorkloadVersion(), serverYaml.getDescription(), null, serverYaml.getYaml());
                    break;
            }

            if (serverGui != null && serverGui.getServer() != null && serverGui.getComponent() != null) {
                if(StringUtils.equalsIgnoreCase("N", context.getCatalogYn())){

                    /**
                     * 서버명 규칙 체크
                     */
                    ServerUtils.checkServerNameRule(serverGui.getComponent().getComponentName());

                    /**
                     * 서버명 중복 체크
                     */
                    serverValidService.checkServerNameIfExists(cluster, cluster.getNamespaceName(), serverGui.getComponent().getComponentName(), true, null);

                    /**
                     * HPA 이름 규칙 및 중복 체크 (입력이 있을때만 체크한다)
                     */
                    if (serverGui.getServer() != null
                            && serverGui.getServer().getHpa() != null
                            && CollectionUtils.isNotEmpty(serverGui.getServer().getHpa().getMetrics())) {
                        /** HPA 규칙 체크 */
                        ServerUtils.checkHpaNameRule(serverGui.getServer().getHpa().getName());
                        /** HPA명 중복 체크 */
                        if (serverValidService.checkHpaNameIfExists(cluster.getClusterSeq(), cluster.getNamespaceName(), serverGui.getServer().getHpa().getName())) {
                            throw new CocktailException("HPA Name Already Exists", ExceptionType.HorizontalPodAutoscalerNameAlreadyExists);
                        }
                    }

                    /**
                     * Container Name 중복 체크
                     */
                    serverValidService.checkContainerNameIfExists(serverGui.getInitContainers(), serverGui.getContainers());
                    /**
                     * Node Selector 체크
                     */
                    if (!(StringUtils.isBlank(serverGui.getServer().getNodeSelectorKey()) && StringUtils.isBlank(serverGui.getServer().getNodeSelectorValue()))
                            && (StringUtils.isBlank(serverGui.getServer().getNodeSelectorKey()) || StringUtils.isBlank(serverGui.getServer().getNodeSelectorValue()))) {
                        throw new CocktailException("Node Selector is key or value empty.", ExceptionType.InvalidParameter_Empty);
                    }
                    /**
                     * addon 으로 feature 지원 여부 판단
                     */
                    serverValidService.checkAddonPluginSupported(serverGui, cluster);
                    /**
                     * multi-nic
                     */
                    serverValidService.setMultiNic(serverGui, cluster);
                    /**
                     * PV 체크
                     */
                    // TODO: DaemonSet processVolumeRequest 확인 필요
                    serverValidService.processVolumeRequest(cluster, serverGui.getComponent().getNamespaceName(), serverGui, true, context);

                    if(WorkloadType.STATEFUL_SET_SERVER == WorkloadType.valueOf(serverGui.getServer().getWorkloadType())
                            || WorkloadType.DAEMON_SET_SERVER == WorkloadType.valueOf(serverGui.getServer().getWorkloadType())) {
                        if (CollectionUtils.isNotEmpty(serverGui.getServices())) {
                            for (ServiceSpecGuiVO serviceSpecRow : serverGui.getServices()) {
                                serviceSpecRow.setHeadlessFlag(Boolean.TRUE);
                                serviceSpecRow.setServiceType(PortType.HEADLESS.getCode());
                            }
                        }
                    }
                }

                try {

                    /**
                     * image pull secret 셋팅
                     */
                    // 2022.03.21 hjchoi
                    // 이제 이미지 pull 사용자는 워크스페이스가 아닌 플랫폼에 등록된 사용자로 셋팅
                    if (cluster.getAccount() != null) {
                        serverGui.getServer().setImageSecret(cluster.getAccount().getRegistryDownloadUserId());
                    }

                    // server 배포
                    this.serverProcess(DeploymentState.CREATED, serverIntegrate, deployMode, context);

                    return serverIntegrate;

                } catch (Exception eo) {
                    if (eo instanceof CocktailException) {
                        throw eo;
                    } else {
                        log.error("Error while prepare server creation: {}", eo.getMessage());
                        log.error("Stack: {}", ExceptionUtils.getStackTrace(eo));
                        throw new CocktailException(eo.getMessage(), ExceptionType.ServerCreationFailOnPreparation);
                    }
                }
            } else {
                throw new CocktailException("Invalid workload info!!", ExceptionType.InvalidParameter, ExceptionBiz.SERVER);
            }
        } else {
            throw new CocktailException("Invalid request workload info!!", ExceptionType.InvalidParameter, ExceptionBiz.SERVER);
        }

    }

    /**
     * catalog에서 배포시 사용
     *
     * @param serverParams
     * @param context
     * @throws Exception
     */
    @Async
    public void addWorkloads(List<ServerGuiVO> serverParams, ClusterVO cluster, ExecutingContextVO context) throws Exception{
        IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);

        for(ServerGuiVO serverParam : serverParams){
            // server request sleep
            CLUSTER_LOOP:
            for(int i = 0; i < 10; i++){
                ClusterVO clusterProviderAccount = clusterDao.getClusterByNamespace(serverParam.getComponent().getClusterSeq(), serverParam.getComponent().getNamespaceName());
                if(clusterProviderAccount != null && clusterProviderAccount.getProviderAccount() != null && clusterProviderAccount.getProviderAccount().getProviderCode() != null){
                    if(clusterProviderAccount.getProviderAccount().getProviderCode().isNotCloud()){
                        Thread.sleep(2000);
                    }else{
                        Thread.sleep(4000);
                    }
                    break CLUSTER_LOOP;
                }else{
                    Thread.sleep(500);
                    continue CLUSTER_LOOP;
                }
            }

            /**
             * api version 체크
             */
            serverValidService.checkServerApiVersion(serverParam.getServer().getWorkloadType(), serverParam.getServer().getWorkloadVersion(), cluster, context);

            // request add server
            serverParam.setDeployType(DeployType.GUI.getCode());
            this.addWorkload(cluster.getClusterSeq(), serverParam.getComponent().getNamespaceName(), serverParam.getComponent().getComponentName(), serverParam, DeployMode.ASYNC, context);
        }
    }

    /**
     * 워크로드 배포 (Invoke from Snapshot Deployment)
     *
     * @param serverParams
     * @param context
     * @throws Exception
     */
    @Async
    public void addMultipleWorkload(List<ServerIntegrateVO> serverParams, ClusterVO cluster, ExecutingContextVO context) throws Exception{
        IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);

        for(ServerIntegrateVO serverParam : serverParams){
            try {
                if (DeployType.valueOf(serverParam.getDeployType()) == DeployType.GUI) {
                    ServerGuiVO serverGui = (ServerGuiVO) serverParam;
                    CLUSTER_LOOP:
                    for (int i = 0; i < 10; i++) {
                        ClusterVO clusterProviderAccount = clusterDao.getClusterByNamespace(serverGui.getComponent().getClusterSeq(), serverGui.getComponent().getNamespaceName());
                        if (clusterProviderAccount != null && clusterProviderAccount.getProviderAccount() != null && clusterProviderAccount.getProviderAccount().getProviderCode() != null) {
                            if (clusterProviderAccount.getProviderAccount().getProviderCode().isNotCloud()) {
                                Thread.sleep(2100);
                            }
                            else {
                                Thread.sleep(4100);
                            }
                            break CLUSTER_LOOP;
                        }
                        else {
                            Thread.sleep(510);
                            continue CLUSTER_LOOP;
                        }
                    }

                    /** api version 체크 **/
                    serverValidService.checkServerApiVersion(serverGui.getServer().getWorkloadType(), serverGui.getServer().getWorkloadVersion(), cluster, context);

                    /**
                     * request add server
                     */
                    serverGui.setDeployType(DeployType.GUI.getCode());
                    this.addWorkload(cluster.getClusterSeq(), serverGui.getComponent().getNamespaceName(), serverGui.getComponent().getComponentName(), serverGui, DeployMode.SYNC, context);
                }
                else if (DeployType.valueOf(serverParam.getDeployType()) == DeployType.YAML) {
                    ServerYamlVO serverYaml = (ServerYamlVO) serverParam;
                    serverYaml.setDeployType(DeployType.YAML.getCode());

                    /** api version 체크 **/
                    serverValidService.checkServerApiVersion(serverYaml.getWorkloadType(), serverYaml.getWorkloadVersion(), cluster, context);

                    /**yaml validation **/
                    if (!serverValidService.checkWorkloadYaml(cluster.getNamespaceName(), WorkloadType.valueOf(serverYaml.getWorkloadType()), serverYaml.getWorkloadName(), serverYaml.getYaml(), new JSON())) {
                        throw new CocktailException("Yaml is invalid.", ExceptionType.InvalidYamlData);
                    }

                    /** Validation 체크를 위해 GUI 모델로 Convert **/
                    ServerGuiVO serverGui = serverConversionService.convertYamlToGui(cluster, cluster.getNamespaceName(), serverYaml.getWorkloadType(), serverYaml.getWorkloadVersion(), "", null, serverYaml.getYaml());

                    /** 서버명 중복 체크 */
                    serverValidService.checkServerNameIfExists(cluster, cluster.getNamespaceName(), serverYaml.getWorkloadName(), true, null);

                    /** addon 으로 feature 지원 여부 판단 */
                    serverValidService.checkAddonPluginSupported(serverGui, cluster);

                    try {
                        serverYaml.setDeployType(DeployType.YAML.getCode());
                        serverYaml.setClusterSeq(cluster.getClusterSeq());
                        serverYaml.setNamespaceName(cluster.getNamespaceName());
                        serverYaml.setDescription("");

                        context.getParams().put(CommonConstants.UPDATE_TARGET_WORKLOAD, serverYaml.getWorkloadName()); // for Logging

                        /**
                         * request add server
                         */
                        this.serverProcess(DeploymentState.CREATED, serverYaml, DeployMode.SYNC, context);
                    }
                    catch (Exception eo) {
                        if (eo instanceof CocktailException) {
                            throw eo;
                        }
                        else {
                            log.error("Error while server creation: {}", eo.getMessage());
                            log.error("Stack:\n{}", ExceptionUtils.getStackTrace(eo));
                            throw new CocktailException(eo.getMessage(), ExceptionType.ServerCreationFailOnPreparation);
                        }
                    }
                }
                else {
                    log.error(String.format("Invalid DeployType : addMultipleWorkload : %s", JsonUtils.toGson(serverParam)));
                }
            }
            catch (Exception ex) {
                if(log.isDebugEnabled()) log.debug("trace log ", ex);
                log.error("@@@ addWorkload Failure... try next workload installation...");
            }
        }

        // 워크로드 처리가 끝났음을 표시.. => 다음 처리 Package...
        context.getParams().put(CommonConstants.AUDIT_WORKLOAD_PROCESSING_FINISHED, Boolean.TRUE);
    }

    /**
     * 서버 수정
     *
     * @param clusterSeq
     * @param namespace
     * @param serverIntegrate
     * @param deployMode
     * @param context
     * @return
     * @throws Exception
     */
    public ServerIntegrateVO updateWorkload(Integer clusterSeq, String namespace, String workloadName, ServerIntegrateVO serverIntegrate, DeployMode deployMode, ExecutingContextVO context) throws Exception {
        // 콤포넌트 정보 조회
        ComponentVO component = this.getComponent(clusterSeq, namespace, workloadName);

        return this.updateWorkload(clusterSeq, namespace, component, serverIntegrate, deployMode, context);
    }

    public ServerIntegrateVO updateWorkload(Integer clusterSeq, String namespace, ComponentVO component, ServerIntegrateVO serverIntegrate, DeployMode deployMode, ExecutingContextVO context) throws Exception {
        log.info(String.format("[STEP1] updateWorkload : Start Checking Datas : %s - %s - %s", clusterSeq, namespace, component.getComponentName()));

        IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);

        DeployType deployType = DeployType.valueOf(serverIntegrate.getDeployType());

        ClusterVO cluster = clusterDao.getClusterByNamespace(clusterSeq, namespace);
        if (cluster == null) {
            cluster = clusterDao.getCluster(clusterSeq);
            if (cluster != null) {
                cluster.setNamespaceName(namespace);
            } else {
                throw new CocktailException("Invalid cluster info!!", ExceptionType.InvalidParameter, ExceptionBiz.SERVER);
            }
        }

        if (serverIntegrate != null) {
            ServerGuiVO serverGui = null;
            ServerYamlVO serverYaml = null;
            switch (deployType) {
                case GUI:
                    serverGui = (ServerGuiVO)serverIntegrate;
                    break;
                case YAML:
                    serverYaml = (ServerYamlVO)serverIntegrate;
                    serverGui = serverConversionService.convertYamlToGui(cluster, namespace, serverYaml.getWorkloadType(), serverYaml.getWorkloadVersion(), serverYaml.getDescription(), null, serverYaml.getYaml());
                    break;
            }

            if (serverGui != null && serverGui.getServer() != null && serverGui.getComponent() != null) {
                /** 2020.10.16 : HPA 이름을 변경할 수 없도록 처리 위해 현재 워크로드에 설정된 HPA 조회
                 * (여기서도 HPA는 무조건 1개만 가져옴.. HPA가 여러개인 케이스는 전반적으로 수정이 필요)
                 * **/
                ServerIntegrateVO serverDetail4Hpa = this.getWorkloadDetailByNamespace(DeployType.GUI, cluster.getClusterSeq(), namespace, serverGui.getComponent().getComponentName());
                if(serverDetail4Hpa != null) {
                    ServerGuiVO serverGui4Hpa = (ServerGuiVO) serverDetail4Hpa;
                    String oldHpaName = Optional.ofNullable(serverGui4Hpa).map(ServerGuiVO::getServer).map(ServerVO::getHpa).map(HpaGuiVO::getName).orElseGet(() ->null);
                    String newHpaName = Optional.ofNullable(serverGui).map(ServerGuiVO::getServer).map(ServerVO::getHpa).map(HpaGuiVO::getName).orElseGet(() ->null);

                    if(StringUtils.isNotBlank(oldHpaName) && StringUtils.isNotBlank(newHpaName) && !StringUtils.equalsIgnoreCase(newHpaName, oldHpaName)) {
                        throw new CocktailException("HPA Name cannot be modified", ExceptionType.HorizontalPodAutoscalerNameMismatch);
                    }
                }

                /**
                 * image pull secret 셋팅
                 */
                // 2022.03.21 hjchoi
                // 이제 이미지 pull 사용자는 워크스페이스가 아닌 플랫폼에 등록된 사용자로 셋팅
                if (cluster.getAccount() != null) {
                    serverGui.getServer().setImageSecret(cluster.getAccount().getRegistryDownloadUserId());
                }

                /**
                 * 서버명 규칙 체크
                 */
                ServerUtils.checkServerNameRule(serverGui.getComponent().getComponentName());

                /**
                 * Node Selector 체크
                 */
                /** 2020.08.13 : NodeSelector의 Key는 Blank이면 안되지면 value는 Blank일 수 있음 From cloud.jung **/
//        if(!( StringUtils.isBlank(serverGui.getServer().getNodeSelectorKey()) && StringUtils.isBlank(serverGui.getServer().getNodeSelectorValue()) )
//            && ( StringUtils.isBlank(serverGui.getServer().getNodeSelectorKey()) || StringUtils.isBlank(serverGui.getServer().getNodeSelectorValue()) )){
//            throw new CocktailException("Node Selector is key or value empty.", ExceptionType.InvalidParameter_Empty);
//        }
                if(!( StringUtils.isBlank(serverGui.getServer().getNodeSelectorKey()) && StringUtils.isBlank(serverGui.getServer().getNodeSelectorValue()) )
                        && StringUtils.isBlank(serverGui.getServer().getNodeSelectorKey()) ) {
                    throw new CocktailException("Node Selector is key or value empty.", ExceptionType.InvalidParameter_Empty);
                }

                StateCode state = StateCode.codeOf(component.getStateCode());
                log.debug("StateCode of component(server) that is subjected to update: {}", state);
                if (state != StateCode.RUNNING && state != StateCode.RUNNING_PREPARE && state != StateCode.RUNNING_WARNING &&
                        state != StateCode.ERROR && state != StateCode.STOPPED && state != StateCode.COMPLETED && state != StateCode.FAILED
                        && state != StateCode.READY
                ) {
                    String message = String.format("server state is not suitable for update: %s", state);
                    log.warn(message);
                    throw new CocktailException(message, ExceptionType.ServerIsNotUpdatableState);
                } else {
                    /**
                     * PV 체크
                     */
                    serverValidService.processVolumeRequest(cluster, cluster.getNamespaceName(), serverGui, true, context);
                    /**
                     * addon 으로 feature 지원 여부 판단
                     */
                    serverValidService.checkAddonPluginSupported(serverGui, cluster);
                    /**
                     * multi-nic
                     */
                    serverValidService.setMultiNic(serverGui, cluster);

                }

                try {

                    String yamlStr = null;

                    switch (deployType) {
                        case GUI:
                            context.getParams().put("serverGui", serverGui);

                            // merge request gui to yaml
                            ServerYamlVO serverYamlForMerge = null;
                            ServerIntegrateVO serverIntegrateForMerge = this.getWorkloadDetail(DeployType.YAML, namespace, serverGui.getComponent().getComponentName(), component, cluster, Boolean.FALSE, null);
                            if (serverIntegrate != null) {
                                serverYamlForMerge = (ServerYamlVO) serverIntegrateForMerge;
                            }
                            yamlStr = serverConversionService.mergeWorkload(cluster, cluster.getNamespaceName(), serverGui, serverYamlForMerge);

                            serverYaml = new ServerYamlVO();
                            serverYaml.setDeployType(DeployType.YAML.getCode());
                            serverYaml.setWorkloadType(serverGui.getServer().getWorkloadType());
                            serverYaml.setWorkloadVersion(serverGui.getServer().getWorkloadVersion());
                            serverYaml.setClusterSeq(cluster.getClusterSeq());
                            serverYaml.setNamespaceName(cluster.getNamespaceName());
                            serverYaml.setWorkloadName(component.getComponentName());
                            serverYaml.setDescription(serverGui.getComponent().getDescription());
                            serverYaml.setYaml(yamlStr);

                            break;
                    }

                    context.getParams().put(CommonConstants.UPDATE_TARGET_WORKLOAD, component.getComponentName()); // for Logging
                    if( state == StateCode.STOPPED ) {
                        // 서버 생성
                        log.info(String.format("[STEP2][TYPE 2 : Create] Start updateWorkload : ServerState [%s] : %s - %s - %s", state.getCode(), clusterSeq, namespace, component.getComponentName()));
                        this.serverProcess(DeploymentState.CREATED, serverYaml, deployMode, context);
                    } else {
                        // 서버 수정
                        log.info(String.format("[STEP2][TYPE 1 : Update] Start updateWorkload : ServerState [%s] : %s - %s - %s", state.getCode(), clusterSeq, namespace, component.getComponentName()));
                        this.serverProcess(DeploymentState.EDITED, serverYaml, deployMode, context);
                    }

                    return serverIntegrate;

                } catch (Exception eo) {
                    // TODO: 에러처리 고민 필요
                    if (eo instanceof CocktailException) {
                        throw eo;
                    } else {
                        log.error("Error while prepare server update: {}", eo.getMessage());
                        log.error("Stack:\n{}", ExceptionUtils.getStackTrace(eo));
                        throw new CocktailException(eo.getMessage(), eo,  ExceptionType.ServerUpdateFailOnPreparation);
                    }
                }
            } else {
                throw new CocktailException("Invalid workload info!!", ExceptionType.InvalidParameter, ExceptionBiz.SERVER);
            }
        } else {
            throw new CocktailException("Invalid request workload info!!", ExceptionType.InvalidParameter, ExceptionBiz.SERVER);
        }

    }

    /**
     * 기존 종료된 서버를 재성생 하여 서버를 생성함
     *
     * @param clusterSeq
     * @param namespace
     * @param workloadName
     * @param deployMode
     * @param context
     * @return
     * @throws Exception
     */
    public ServerIntegrateVO redeployWorkload(Integer clusterSeq, String namespace, String workloadName, DeployMode deployMode, ExecutingContextVO context) throws Exception {
        // 콤포넌트 정보 조회
        ComponentVO component = this.getComponent(clusterSeq, namespace, workloadName);

        return this.redeployWorkload(clusterSeq, namespace, workloadName, component, deployMode, context);
    }

    public ServerIntegrateVO redeployWorkload(Integer clusterSeq, String namespace, String workloadName, ComponentVO component, DeployMode deployMode, ExecutingContextVO context) throws Exception {
        log.info(String.format("[STEP1] redeployWorkload : Start Checking Datas : %s - %s - %s", clusterSeq, namespace, workloadName));

        IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);

        ClusterVO cluster = clusterDao.getClusterByNamespace(clusterSeq, namespace);
        if (cluster == null) {
            cluster = clusterDao.getCluster(clusterSeq);
            cluster.setNamespaceName(namespace);
        }
        ServerGuiVO serverGui = null;

        if (component != null) {
            /**
             * api version 체크
             */
            serverValidService.checkServerApiVersion(component.getWorkloadType(), component.getWorkloadVersion(), cluster, context);

            /**
             * yaml validation
             */
            if (!serverValidService.checkWorkloadYaml(cluster.getNamespaceName(), WorkloadType.valueOf(component.getWorkloadType()), workloadName, component.getWorkloadManifest(), new JSON())) {
                throw new CocktailException("Yaml is invalid.", ExceptionType.InvalidYamlData);
            }

            serverGui = serverConversionService.convertYamlToGui(cluster, namespace, component.getWorkloadType(), component.getWorkloadVersion(), component.getDescription(), component, component.getWorkloadManifest());

            /**
             * 서버명 중복 체크
             */
            serverValidService.checkServerNameIfExists(cluster, cluster.getNamespaceName(), workloadName, true, null);

            /**
             * HPA 이름 규칙 및 중복 체크 (입력이 있을때만 체크한다)
             */
            if (serverGui.getServer() != null
                && serverGui.getServer().getHpa() != null
                && CollectionUtils.isNotEmpty(serverGui.getServer().getHpa().getMetrics())) {
                /** HPA 규칙 체크 */
                ServerUtils.checkHpaNameRule(serverGui.getServer().getHpa().getName());
                /** HPA명 중복 체크 */
                if (serverValidService.checkHpaNameIfExists(cluster.getClusterSeq(), cluster.getNamespaceName(), serverGui.getServer().getHpa().getName())) {
                    throw new CocktailException("HPA Name Already Exists", ExceptionType.HorizontalPodAutoscalerNameAlreadyExists);
                }
            }

            /**
             * addon 으로 feature 지원 여부 판단
             */
            serverValidService.checkAddonPluginSupported(serverGui, cluster);
        }


        try {

            if (component != null) {
                WorkloadType workloadType = WorkloadType.valueOf(component.getWorkloadType());

                /**
                 * server 생성
                 */
                ServerYamlVO serverYaml = new ServerYamlVO();
                serverYaml.setDeployType(DeployType.YAML.getCode());
                serverYaml.setWorkloadType(component.getWorkloadType());
                serverYaml.setWorkloadVersion(component.getWorkloadVersion());
                serverYaml.setClusterSeq(clusterSeq);
                serverYaml.setNamespaceName(cluster.getNamespaceName());
                serverYaml.setWorkloadName(component.getComponentName());
                serverYaml.setDescription(component.getDescription());
                serverYaml.setYaml(component.getWorkloadManifest());

                context.getParams().put(CommonConstants.UPDATE_TARGET_WORKLOAD, component.getComponentName()); // for Logging
                this.serverProcess(DeploymentState.RECREATED, serverYaml, deployMode, context);

            }
        } catch (Exception eo) {
//            try {
//                if (component != null && component.getComponentSeq() != null) {
//                    componentService.updateComponentState(context, component, StateCode.ERROR, eo.getMessage(), componentDao);
//                } else {
//                    // Component 정보 입력...
//                    component.setComponentType(ComponentType.CSERVER.name());
//                    component.setUseYn("Y");
//                    component.setWorkloadType(serverGui.getServer().getWorkloadType());
//                    component.setWorkloadVersion(serverGui.getServer().getWorkloadVersion());
//                    component.setStateCode(StateCode.ERROR.getCode());
//                    component.setCreator(context.getUserSeq());
//
//                    AppmapVO appmap = appmapDao.getAppmapByClusterAndName(clusterSeq, namespace);
//                    if (appmap != null && appmap.getAppmapSeq() != null) {
//                        component.setAppmapSeq(appmap.getAppmapSeq());
//                    } else {
//                        component.setAppmapSeq(0);
//                    }
//
//                    component = componentService.addServerComponent(component, context);
//                    componentService.updateComponentState(context, component, StateCode.ERROR, eo.getMessage(), componentDao);
//                    log.debug("New Component: {}", JsonUtils.toGson(component));
//                }
//
//            } catch (Exception e) {
//                log.error("Error while prepare server recreate : {}", e.getMessage());
//            }

            if (eo instanceof CocktailException) {
                throw eo;
            } else {
                log.error("Error while recreate server creation: {}", eo.getMessage());
                log.error("Stack:\n{}", ExceptionUtils.getStackTrace(eo));
                throw new CocktailException(eo.getMessage(), ExceptionType.ServerCreationFailOnPreparation);
            }
        }

        return serverGui;
    }

    /**
     * 서버를 종료한다. 서버의 종료는 k8s object 삭제, task(job) 종료이나 component를 삭제하지는 않는다.
     * ServerController에서 호출.
     *
     * @param clusterSeq
     * @param namespace
     * @param workloadName
     * @param deployMode
     * @param context
     * @return
     * @throws Exception
     */
    public ServerIntegrateVO terminateWorkload(Integer clusterSeq, String namespace, String workloadName, DeployMode deployMode, ExecutingContextVO context) throws Exception {
        // 콤포넌트 정보 조회
        ComponentVO component = this.getComponent(clusterSeq, namespace, workloadName);

        return this.terminateWorkload(clusterSeq, namespace, workloadName, component, deployMode, context);
    }

    public ServerIntegrateVO terminateWorkload(Integer clusterSeq, String namespace, String workloadName, ComponentVO component, DeployMode deployMode, ExecutingContextVO context) throws Exception {

        IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
        IComponentMapper componentDao = this.sqlSession.getMapper(IComponentMapper.class);

        ClusterVO cluster = clusterDao.getClusterByNamespace(clusterSeq, namespace);
        if (cluster == null) {
            cluster = clusterDao.getCluster(clusterSeq);
            cluster.setNamespaceName(namespace);
        }
//        ComponentVO component = componentDao.getComponentInAppmapByClusterAndNames(clusterSeq, namespace, workloadName);

        try {

            /**
             * api version 체크
             */
            serverValidService.checkServerApiVersion(component.getWorkloadType(), component.getWorkloadVersion(), cluster, context);

            // 종료는 어떤 상태에서도 가능하므로 action 검사는 하지 않는다.
            componentService.updateComponentState(context, component, StateCode.STOPPING, componentDao);

            /**
             * 서버 종료
             */
            ServerIntegrateVO serverIntegrate = null;
            try {
                // 현재 workload 정보 조회
                serverIntegrate = this.getWorkloadDetail(DeployType.YAML, namespace, workloadName, component, cluster, true, null);
                if (serverIntegrate != null) {
                    ServerYamlVO serverYaml = (ServerYamlVO) serverIntegrate;
                    if (serverYaml.getClusterSeq() != null || StringUtils.isNotBlank(serverYaml.getNamespaceName()) || StringUtils.isNotBlank(serverYaml.getWorkloadName()) || StringUtils.isNotBlank(serverYaml.getYaml())) {
                        // 설정 정보를 미리 저장
                        componentDao.updateComponentManifestByNamespace(serverYaml.getClusterSeq(), serverYaml.getNamespaceName(), serverYaml.getWorkloadName(), serverYaml.getYaml(), null);
                    } else {
                        // 종료는 어떤 상태에서도 가능하므로 action 검사는 하지 않는다.
                        componentService.updateComponentState(context, component, StateCode.STOPPED, componentDao);
                        return serverIntegrate;
                    }
                } else {
                    // 종료는 어떤 상태에서도 가능하므로 action 검사는 하지 않는다.
                    componentService.updateComponentState(context, component, StateCode.STOPPED, componentDao);
                    return serverIntegrate;
                }

            } catch (Exception e) {
                throw e;
            }
            // 서버 종료
            this.serverProcess(DeploymentState.TERMINATED, serverIntegrate, deployMode, context);

            return serverIntegrate;
        } catch (Exception eo) {

//            if(component != null){
//                try {
//                    componentService.updateComponentState(context, component, StateCode.ERROR, eo.getMessage(), componentDao);
//                } catch (Exception e) {
//                    log.error("Error while prepare server termination : {}", e.getMessage());
//                }
//            }

            if (eo instanceof CocktailException) {
                throw eo;
            } else {
                log.error("Error while prepare server termination: {}", eo.getMessage());
                log.error("Stack:\n{}", ExceptionUtils.getStackTrace(eo));
                throw new CocktailException(eo.getMessage(), ExceptionType.ServerTerminationFailOnPreparation);
            }
        }
    }

    /**
     * 서버를 삭제한다.
     *
     * @param clusterSeq
     * @param namespace
     * @param workloadName
     * @param context
     * @throws Exception
     */
    public void removeWorkload(Integer clusterSeq, String namespace, String workloadName, ExecutingContextVO context) throws Exception {

        IComponentMapper componentDao = this.sqlSession.getMapper(IComponentMapper.class);
        IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);

        ComponentVO component = componentDao.getComponentByClusterAndNames(clusterSeq, namespace, workloadName);

        if (component != null) {
            try {

                componentService.updateComponentState(context, component, StateCode.DELETING, componentDao);

                /**
                 * 2019.12.08 chj
                 * 기존에 서비스 생성시 워크로드를 선택하여 생성하여 의존도가 있었으나
                 * label을 선택하여 생성하도록 로직을 변경하여 워크로드가 삭제되어도 의존도가 없으므로 삭제처리하지 않음
                 */
//            /**
//             * 2019.04.02 : Redion : 워크로드 삭제시 워크로드에 연결된 서비스를 함께 지워줄 수 있도록 처리 추가.
//             * ( 참고 : Package Type 워크로드와 일반 워크로드는 매핑된 서비스를 판단하는 조건이 달라 별도로 구현함 )
//             */
//            String label = String.format("%s=%s,%s in (%s,%s,%s)",
//                    KubeConstants.LABELS_COCKTAIL_KEY, ResourceUtil.getResourcePrefix(),
//                    KubeConstants.LABELS_COCKTAIL_SERVICE_TYPE, PortType.LOADBALANCER.getCode(), PortType.NODE_PORT.getCode(), PortType.CLUSTER_IP.getCode());
//
//            // NameSpace안에서 Cocktail에서 만들어진 서비스 목록을 조회.
//            List<K8sServiceVO> k8sServices = k8sResourceService.getServices(cluster, cluster.getNamespaceName(), null, label, ContextHolder.exeContext());
//            Set<String> serviceNameSet = new HashSet<>();
//            serviceNameSet.add(component.getComponentName()); // 워크로드가 중지되었을때 삭제된 clusterIp 서비스가 있을 지 모르므로 셋팅 해줌.
//            // 서비스 목록중. labelSelector를 보고 워크로드에 매핑된 서비스인지 판별.
//            // 판단기준 : app={componentNameName} / cocktail={componentId}
//            for (K8sServiceVO k8sServiceRow : k8sServices) {
//                String selectorApp = "";
//                String selectorCocktail = "";
//                if (k8sServiceRow.getDetail() != null &&
//                        k8sServiceRow.getDetail().getLabelSelector() != null) {
//                    selectorApp = k8sServiceRow.getDetail().getLabelSelector().get(KubeConstants.LABELS_KEY);
//                    selectorCocktail = k8sServiceRow.getDetail().getLabelSelector().get(KubeConstants.LABELS_COCKTAIL_KEY);
//                }
//
//                // 워크로드에 매핑된 서비스만 골라서 삭제.
//                if (StringUtils.isNotBlank(selectorApp) && StringUtils.isNotBlank(selectorCocktail) &&
//                        selectorApp.equals(component.getComponentName()) &&
//                        selectorCocktail.equals(ResourceUtil.getUniqueName(component.getComponentId()))) {
//                    log.debug("=====================================================");
//                    log.debug("Find and delete the service : " + k8sServiceRow.getServiceName() + ":" + selectorApp + ":" + selectorCocktail);
//                    log.debug("=====================================================");
//                    // 워크로드에 매핑된 서비스 삭제.
//                    k8sResourceService.deleteService(cluster, k8sServiceRow.getServiceName(), null, context);
//                    serviceNameSet.add(k8sServiceRow.getServiceName()); // 인그레스 삭제 참조용
//                }
//            }
//            /**
//             * 2019.05.15 : chj : 워크로드 삭제시 삭제된 서비스를 참조하는 인그레스 룰 삭제
//             */
//            this.deleteIngressOfMatchedService(cluster, serviceNameSet);

//            /**
//             * 기존 종료(TERMINATED) 된 component의 파이프라인 삭제
//             */
//            pipelineService.removePipelineByComponent(componentSeq, context);

                // 삭제
                componentService.updateComponentState(context, component, StateCode.DELETED, componentDao);

            } catch (Exception eo) {
                componentService.updateComponentState(context, component, StateCode.ERROR, componentDao);

                if (eo instanceof CocktailException) {
                    throw eo;
                } else {
                    log.error("Error while prepare server deletion: {}", eo.getMessage());
                    log.error("Stack:\n{}", ExceptionUtils.getStackTrace(eo));
                    throw new CocktailException(eo.getMessage(), ExceptionType.ServerDeleteFailOnPreparation);
                }
            }
        }

    }
    /**
     * 비동기로 서버 배포
     *
     * @param deploymentState
     * @param serverIntegrate
     * @param context
     * @throws Exception
     */
    public void serverProcess(DeploymentState deploymentState, ServerIntegrateVO serverIntegrate, DeployMode deployMode, ExecutingContextVO context) throws Exception {

        switch (deployMode) {
            case ASYNC:
                serverAsyncProcessService.serverProcessAsync(deploymentState, serverIntegrate, context);
                break;
            default:
                serverAsyncProcessService.serverProcessSync(deploymentState, serverIntegrate, context);
                break;

        }
    }

    /**
     * 중지된 서버 수정
     *
     * @param clusterSeq
     * @param namespace
     * @param workloadName
     * @param serverIntegrate
     * @param context
     * @return
     * @throws Exception
     */
    @Transactional(transactionManager = "transactionManager")
    public ServerIntegrateVO editWorkloadManifest(Integer clusterSeq, String namespace, String workloadName, ServerIntegrateVO serverIntegrate, ExecutingContextVO context) throws Exception {

        IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
        IServiceMapper serviceDao = this.sqlSession.getMapper(IServiceMapper.class);
        IComponentMapper componentDao = this.sqlSession.getMapper(IComponentMapper.class);

        ClusterVO cluster = clusterDao.getClusterByNamespace(clusterSeq, namespace);
        if (cluster == null) {
            cluster = clusterDao.getCluster(clusterSeq);
            cluster.setNamespaceName(namespace);
        }
        ComponentVO component = componentDao.getComponentByClusterAndNames(clusterSeq, namespace, workloadName);

        WorkloadType workloadType = WorkloadType.valueOf(component.getWorkloadType());

        try {

            DeployType deployType = DeployType.valueOf(serverIntegrate.getDeployType());
            String yamlStr = "";
            String description = null;
            Integer workloadGroupSeq = null;
            switch (deployType) {
                case GUI:
                    ServerGuiVO updatedServerGui = (ServerGuiVO)serverIntegrate;

                    // set image secret
                    // 2022.03.21 hjchoi
                    // 이제 이미지 pull 사용자는 워크스페이스가 아닌 플랫폼에 등록된 사용자로 셋팅
                    if (cluster.getAccount() != null) {
                        updatedServerGui.getServer().setImageSecret(cluster.getAccount().getRegistryDownloadUserId());
                    }

                    // set description
                    description = updatedServerGui.getComponent().getDescription();

                    // set group
                    workloadGroupSeq = updatedServerGui.getComponent().getWorkloadGroupSeq();

                    // set manifest
                    ServerYamlVO currServerYaml = new ServerYamlVO();
                    currServerYaml.setDeployType(DeployType.YAML.getCode());
                    currServerYaml.setWorkloadType(updatedServerGui.getServer().getWorkloadType());
                    currServerYaml.setWorkloadVersion(updatedServerGui.getServer().getWorkloadVersion());
                    currServerYaml.setClusterSeq(cluster.getClusterSeq());
                    currServerYaml.setNamespaceName(cluster.getNamespaceName());
                    currServerYaml.setWorkloadName(updatedServerGui.getComponent().getComponentName());
                    currServerYaml.setDescription(updatedServerGui.getComponent().getDescription());
                    currServerYaml.setYaml(component.getWorkloadManifest());
                    yamlStr = serverConversionService.mergeWorkload(cluster, cluster.getNamespaceName(), updatedServerGui, currServerYaml);

                    break;
                case YAML:
                    ServerYamlVO updatedServerYaml = (ServerYamlVO)serverIntegrate;

                    // set description
                    description = updatedServerYaml.getDescription();

                    /**
                     * yaml validation
                     */
                    if (!serverValidService.checkWorkloadYaml(updatedServerYaml.getNamespaceName(), WorkloadType.valueOf(updatedServerYaml.getWorkloadType()), updatedServerYaml.getWorkloadName(), updatedServerYaml.getYaml(), new JSON())) {
                        throw new CocktailException("Yaml is invalid.", ExceptionType.InvalidYamlData);
                    }

                    // set manifest
                    List<Object> objs = ServerUtils.getYamlObjects(updatedServerYaml.getYaml());
                    if (CollectionUtils.isNotEmpty(objs)) {
                        V1ObjectMeta objectMeta = ServerUtils.getK8sObjectMetaInObject(objs.get(0), new JSON());
                        Map<String, String> annotations = objectMeta.getAnnotations();
                        if (MapUtils.isNotEmpty(annotations) && MapUtils.getString(annotations, KubeConstants.ANNOTATION_COCKTAIL_GROUP_NO, null) != null) {
                            if (StringUtils.isNumeric(objectMeta.getAnnotations().get(KubeConstants.ANNOTATION_COCKTAIL_GROUP_NO))) {
                                workloadGroupSeq = Integer.parseInt(objectMeta.getAnnotations().get(KubeConstants.ANNOTATION_COCKTAIL_GROUP_NO));
                            }
                        }

                        yamlStr = updatedServerYaml.getYaml();
                    }

                    break;
            }

            componentDao.updateComponentManiDescGrpByNamespace(clusterSeq, namespace, workloadName, workloadGroupSeq, yamlStr, description, ContextHolder.exeContext().getUserSeq());

//            /**
//             * pipeline 수정
//             */
//            try {
//                ServerGuiVO serverGui = serverConversionService.convertYamlToGui(cluster, component.getGroupSeq(), component.getWorkloadType(), component.getWorkloadVersion(), component.getDescription(), yamlStr);
//                serverGui.getComponent().setComponentSeq(component.getComponentSeq());
//                pipelineFlowService.mergePipeline(serverGui, context);
//            } catch (Exception e) {
//                throw new CocktailException("fail merge pipeline!!", e, ExceptionType.InternalError);
//            }

            return serverIntegrate;
        } catch (Exception eo) {
            throw eo;
        }
    }

    public ComponentVO getComponent(Integer clusterSeq, String namespace, String workloadName) throws Exception {
        ServerIntegrateVO serverIntegrate = this.getWorkloadDetailByNamespace(DeployType.GUI, clusterSeq, namespace, workloadName);
        ComponentVO component = null;
        ServerGuiVO serverGui = null;
        if (serverIntegrate != null) {
            serverGui = (ServerGuiVO) serverIntegrate;
            component = serverGui.getComponent();
        } else {
            throw new CocktailException("Workload not found.", ExceptionType.ServerNotFound);
        }

        return component;
    }

//    public List<ComponentVO> getComponents(Integer clusterSeq, String namespace) throws Exception {
//        IComponentMapper componentDao = this.sqlSession.getMapper(IComponentMapper.class);
//        return componentDao.getComponents(clusterSeq, null, namespace);
//    }
//
//    public List<ComponentVO> getComponents(Integer clusterSeq, Integer appmapSeq) throws Exception {
//        IComponentMapper componentDao = this.sqlSession.getMapper(IComponentMapper.class);
//        return componentDao.getComponents(clusterSeq, appmapSeq, null);
//    }

    /**
     * DeployType별 workload 상세 정보 조회
     *
     * @param deployType
     * @param clusterSeq
     * @param namespace
     * @param workloadName
     * @return
     * @throws Exception
     */
    public ServerIntegrateVO getWorkloadDetailByNamespace(DeployType deployType, Integer clusterSeq, String namespace, String workloadName) throws Exception {
        IComponentMapper componentDao = this.sqlSession.getMapper(IComponentMapper.class);
        IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);

        ClusterVO cluster = clusterDao.getCluster(clusterSeq);
        ComponentVO component = componentDao.getComponentByClusterAndNames(clusterSeq, namespace, workloadName);

        if(component != null && component.getWorkloadGroupSeq() != null && component.getWorkloadGroupSeq() < 1) {
            component.setWorkloadGroupSeq(null);
        }

        return this.getWorkloadDetail(deployType, namespace, workloadName, component, cluster, true, null);
    }
    public ServerIntegrateVO getWorkloadDetailByNamespaceForCluster(DeployType deployType, Integer clusterSeq, String namespace, String workloadName) throws Exception {
        IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
        ClusterVO cluster = clusterDao.getCluster(clusterSeq);
        cluster.setNamespaceName(namespace);

        return this.getWorkloadDetail(deployType, workloadName, cluster, cluster.getNamespaceName(), null);
    }
    public ServerIntegrateVO getWorkloadDetail(DeployType deployType, String namespace, String workloadName, ComponentVO component, ClusterVO cluster, Boolean canActualizeState, Map<String, Map<String, ? extends Object>> k8sResourceMap) throws Exception {

        IComponentMapper componentDao = this.sqlSession.getMapper(IComponentMapper.class);
        IPipelineFlowMapper pipelineDao = sqlSession.getMapper(IPipelineFlowMapper.class);

        ServerIntegrateVO serverIntegrate = new ServerIntegrateVO();
        serverIntegrate.setDeployType(deployType.getCode());
        // Workload default 상태
        StateCode currState = StateCode.STOPPED;

        if (cluster != null) {
            // 워크로드가 STOPPED 이거나 오류로 인하여 배포하지 못한 상태가 아닌 다른 경우에는
            // k8s를 직접 조회하여 yaml 및 상태를 조회함.
            if (MapUtils.isEmpty(k8sResourceMap)) {
                k8sResourceMap = workloadResourceService.getWorkloadResource(cluster, namespace,
                        String.format("%s.%s=%s", KubeConstants.META, KubeConstants.NAME, workloadName), null,
                        Boolean.TRUE, Boolean.TRUE, Boolean.TRUE, Boolean.TRUE, Boolean.TRUE, Boolean.FALSE, Boolean.TRUE);
            }

            boolean k8sResourceExists = true;

            if (MapUtils.isNotEmpty(k8sResourceMap)) {
                WorkloadType workloadType = null;
                if(MapUtils.getObject(k8sResourceMap, K8sApiKindType.DEPLOYMENT.getValue(), null) != null
                    && MapUtils.getObject(k8sResourceMap.get(K8sApiKindType.DEPLOYMENT.getValue()), workloadName, null) != null) {
                    workloadType = WorkloadType.REPLICA_SERVER;
                }
                else if(MapUtils.getObject(k8sResourceMap, K8sApiKindType.STATEFUL_SET.getValue(), null) != null
                        && MapUtils.getObject(k8sResourceMap.get(K8sApiKindType.STATEFUL_SET.getValue()), workloadName, null) != null){
                    workloadType = WorkloadType.STATEFUL_SET_SERVER;
                }
                else if(MapUtils.getObject(k8sResourceMap, K8sApiKindType.DAEMON_SET.getValue(), null) != null
                        && MapUtils.getObject(k8sResourceMap.get(K8sApiKindType.DAEMON_SET.getValue()), workloadName, null) != null) {
                    workloadType = WorkloadType.DAEMON_SET_SERVER;
                }
                else if(MapUtils.getObject(k8sResourceMap, K8sApiKindType.JOB.getValue(), null) != null
                        && MapUtils.getObject(k8sResourceMap.get(K8sApiKindType.JOB.getValue()), workloadName, null) != null) {
                    workloadType = WorkloadType.JOB_SERVER;
                }
                else if(MapUtils.getObject(k8sResourceMap, K8sApiKindType.CRON_JOB.getValue(), null) != null
                        && MapUtils.getObject(k8sResourceMap.get(K8sApiKindType.CRON_JOB.getValue()), workloadName, null) != null) {
                    workloadType = WorkloadType.CRON_JOB_SERVER;
                }

                WorkloadVersion workloadVersion = WorkloadVersion.V1;
                WorkloadVersionSet workloadVersionSet = WorkloadVersionSet.getSupport(workloadType, workloadVersion);

                if(workloadVersionSet != null){
                    // Pod 조회
                    List<V1Pod> pods = new ArrayList<>();
                    Map<String, List<V1Pod>> podMap = new HashMap<>();
                    if (canActualizeState) {
                        pods.addAll(k8sWorker.getPodV1WithLabel(cluster, namespace, null, null));
                    }
                    serverStateService.getPodToMap(pods, podMap);

                    Map<K8sApiKindType, K8sApiVerKindType> workloadSetMap = workloadVersionSet.getApiVerKindEnumSetMap();
                    StringBuffer yamlStr = new StringBuffer();

                    /**
                     * Deployment
                     */
                    if (workloadType == WorkloadType.SINGLE_SERVER || workloadType == WorkloadType.REPLICA_SERVER) {
                        K8sApiVerKindType deploymentType = ResourceUtil.getApiVerKindType(cluster, K8sApiKindType.DEPLOYMENT);

                        // Deployment
                        if (deploymentType != null) {
                            if (deploymentType.getGroupType() == K8sApiGroupType.APPS && deploymentType.getApiType() == K8sApiType.V1) {

                                V1Deployment currDeployment = null;
                                if (MapUtils.getObject(k8sResourceMap.get(K8sApiKindType.DEPLOYMENT.getValue()), workloadName, null) != null) {
                                    currDeployment = (V1Deployment) (k8sResourceMap.get(K8sApiKindType.DEPLOYMENT.getValue())).get(workloadName);
                                }

                                if (currDeployment != null) {
                                    if (canActualizeState) {

                                        // ReplicaSet 조회
                                        List<V1ReplicaSet> replicaSets = new ArrayList<>();
                                        Map<String, List<V1ReplicaSet>> replicaSetMap = new HashMap<>();
                                        replicaSets.addAll(k8sWorker.getReplicaSetsV1(cluster, namespace, null, ResourceUtil.getLabelFilterOfSelector(currDeployment.getSpec().getSelector())));
                                        serverStateService.getReplicaSetToMap(replicaSets, replicaSetMap);
                                        /** Deployment에 해당하는 pod만 필터링 하기 위한 맵 정의 **/
                                        Map<String, List<V1Pod>> podMapInDeployment = new HashMap<>();

                                        /** Deployment에 해당하는 pod를 필터링 (with ReplicaSets) **/
                                        String uniqueName = serverStateService.makeClusterUniqueName(currDeployment.getMetadata().getNamespace(), currDeployment.getMetadata().getName());
                                        List<V1ReplicaSet> currReplicaSet = replicaSetMap.get(uniqueName);
                                        if(CollectionUtils.isNotEmpty(currReplicaSet)) {
                                            // 1-2. 해당 ReplicaSet 목록을 ownerRefrence로 가지고 있는 Pod 목록을 조회...
                                            for(V1ReplicaSet replicaSetRow : currReplicaSet) {
                                                String uniqueReplicaSetName = serverStateService.makeClusterUniqueName(replicaSetRow.getMetadata().getNamespace(), replicaSetRow.getMetadata().getName());
                                                if (MapUtils.getObject(podMap, uniqueReplicaSetName, null) != null) {
                                                    // 1-3. 해당 Pod 목록을 Current Deployment의 이름으로 Map에 저장..
                                                    podMapInDeployment.put(uniqueName, MapUtils.getObject(podMap, uniqueReplicaSetName, null));
                                                }
                                            }
                                        }
                                        else {
                                            podMapInDeployment.putAll(podMap);
                                        }

                                        /** Deployment에 대한 actualize State 조회 **/
                                        List<V1Deployment> deployments = new ArrayList<>();
                                        Map<String, V1Deployment> deploymentMap = new HashMap<>();
                                        deployments.add(currDeployment);
                                        serverStateService.getDeploymentToMap(deployments, deploymentMap);

                                        component = serverStateService.addComponentDefault(cluster, currDeployment.getMetadata().getNamespace(), WorkloadType.REPLICA_SERVER, null, null, currDeployment, component);
                                        serverStateService.actualizeDeploymentState(cluster, component, deploymentMap, Collections.emptyMap(), podMap, null, null, false, componentDao);
                                        currState = StringUtils.isNotBlank(component.getStateCode()) ? StateCode.valueOf(component.getStateCode()) : StateCode.STOPPED;
                                    }

                                    // generate yaml string
                                    currDeployment.setStatus(null);
                                    yamlStr.append(ServerUtils.marshalYaml(currDeployment));

                                }
                            }
                        }
                    }
                    /**
                     * StatefulSet
                     */
                    else if (workloadType == WorkloadType.STATEFUL_SET_SERVER) {
                        K8sApiVerKindType statefulSetType = ResourceUtil.getApiVerKindType(cluster, K8sApiKindType.STATEFUL_SET);

                        // StatefulSet
                        if (statefulSetType != null) {
                            if (statefulSetType.getGroupType() == K8sApiGroupType.APPS && statefulSetType.getApiType() == K8sApiType.V1) {
                                V1StatefulSet currStatefulSet = null;
                                if (MapUtils.getObject(k8sResourceMap.get(K8sApiKindType.STATEFUL_SET.getValue()), workloadName, null) != null) {
                                    currStatefulSet = (V1StatefulSet) (k8sResourceMap.get(K8sApiKindType.STATEFUL_SET.getValue())).get(workloadName);
                                }

                                if (currStatefulSet != null) {
                                    if (canActualizeState) {
                                        List<V1StatefulSet> statefulSets = new ArrayList<>();
                                        Map<String, V1StatefulSet> statefulSetMap = new HashMap<>();
                                        statefulSets.add(currStatefulSet);
                                        serverStateService.getStatefulSetToMap(statefulSets, statefulSetMap);

                                        component = serverStateService.addComponentDefault(cluster, currStatefulSet.getMetadata().getNamespace(), WorkloadType.STATEFUL_SET_SERVER, null, null, currStatefulSet, component);
                                        serverStateService.actualizeDeploymentState(cluster, component, statefulSetMap, Collections.emptyMap(), podMap, null, null, false, componentDao);
                                        currState = StringUtils.isNotBlank(component.getStateCode()) ? StateCode.valueOf(component.getStateCode()) : StateCode.STOPPED;
                                    }


                                    // generate yaml string
                                    currStatefulSet.setStatus(null);
                                    if (yamlStr != null && StringUtils.isNotBlank(yamlStr.toString())) {
                                        yamlStr.append("---\n");
                                    }
                                    yamlStr.append(ServerUtils.marshalYaml(currStatefulSet));

                                }
                            }
                        }
                    }
                    /**
                     * DaemonSet
                     */
                    else if (workloadType == WorkloadType.DAEMON_SET_SERVER) {
                        K8sApiVerKindType daemonSetType = ResourceUtil.getApiVerKindType(cluster, K8sApiKindType.DAEMON_SET);

                        // DaemonSet
                        if (daemonSetType != null) {
                            if (daemonSetType.getGroupType() == K8sApiGroupType.APPS && daemonSetType.getApiType() == K8sApiType.V1) {
                                V1DaemonSet currDaemonSet = null;
                                if (MapUtils.getObject(k8sResourceMap.get(K8sApiKindType.DAEMON_SET.getValue()), workloadName, null) != null) {
                                    currDaemonSet = (V1DaemonSet) (k8sResourceMap.get(K8sApiKindType.DAEMON_SET.getValue())).get(workloadName);
                                }

                                if (currDaemonSet != null) {
                                    if (canActualizeState) {
                                        List<V1DaemonSet> daemonSets = new ArrayList<>();
                                        Map<String, V1DaemonSet> daemonSetMap = new HashMap<>();
                                        daemonSets.add(currDaemonSet);
                                        serverStateService.getDaemonSetToMap(daemonSets, daemonSetMap);

                                        component = serverStateService.addComponentDefault(cluster, currDaemonSet.getMetadata().getNamespace(), WorkloadType.DAEMON_SET_SERVER, null, null, currDaemonSet, component);
                                        serverStateService.actualizeDeploymentState(cluster, component, daemonSetMap, Collections.emptyMap(), podMap, null, null, false, componentDao);
                                        currState = StringUtils.isNotBlank(component.getStateCode()) ? StateCode.valueOf(component.getStateCode()) : StateCode.STOPPED;
                                    }

                                    // generate yaml string
                                    currDaemonSet.setStatus(null);
                                    yamlStr.append(ServerUtils.marshalYaml(currDaemonSet));

                                }
                            }
                        }
                    }
                    /**
                     * Job
                     */
                    else if (workloadType == WorkloadType.JOB_SERVER) {
                        K8sApiVerKindType jobType = ResourceUtil.getApiVerKindType(cluster, K8sApiKindType.JOB);

                        // Job
                        if (jobType != null) {
                            if (jobType.getGroupType() == K8sApiGroupType.BATCH && jobType.getApiType() == K8sApiType.V1) {
                                V1Job currJob = null;
                                if (MapUtils.getObject(k8sResourceMap.get(K8sApiKindType.JOB.getValue()), workloadName, null) != null) {
                                    currJob = (V1Job) (k8sResourceMap.get(K8sApiKindType.JOB.getValue())).get(workloadName);
                                }

                                if (currJob != null) {
                                    if (canActualizeState) {
                                        List<V1Job> jobs = new ArrayList<>();
                                        Map<String, V1Job> jobMap = new HashMap<>();
                                        jobs.add(currJob);
                                        serverStateService.getJobToMap(jobs, jobMap);

                                        component = serverStateService.addComponentDefault(cluster, currJob.getMetadata().getNamespace(), WorkloadType.JOB_SERVER, null, null, currJob, component);
                                        serverStateService.actualizeDeploymentState(cluster, component, jobMap, Collections.emptyMap(), podMap, null, null, false, componentDao);
                                        currState = StringUtils.isNotBlank(component.getStateCode()) ? StateCode.valueOf(component.getStateCode()) : StateCode.STOPPED;
                                    }

                                    // generate yaml string
                                    currJob.setStatus(null);
                                    yamlStr.append(ServerUtils.marshalYaml(currJob));

                                }
                            }
                        }
                    }
                    /**
                     * CronJob
                     */
                    else if (workloadType == WorkloadType.CRON_JOB_SERVER) {
                        K8sApiVerKindType cronType = ResourceUtil.getApiVerKindType(cluster, K8sApiKindType.CRON_JOB);

                        // CronJob
                        if (cronType != null) {
                            if (cronType.getGroupType() == K8sApiGroupType.BATCH && cronType.getApiType() == K8sApiType.V1BETA1) {
                                V1beta1CronJob currCronJob = null;
                                if (MapUtils.getObject(k8sResourceMap.get(K8sApiKindType.CRON_JOB.getValue()), workloadName, null) != null) {
                                    currCronJob = (V1beta1CronJob) (k8sResourceMap.get(K8sApiKindType.CRON_JOB.getValue())).get(workloadName);
                                }

                                if (currCronJob != null) {
                                    if (canActualizeState) {
                                        /** 1. Job목록 중 CronJob이 생성한 Job만 따로 분류.. **/
                                        List<V1Job> jobs = new ArrayList<>();
                                        jobs.addAll(k8sWorker.getJobsV1(cluster, cluster.getNamespaceName(), null, null));
                                        Map<String, Map<String, V1Job>> jobMapInCronJob = new HashMap<>();
                                        serverStateService.getJobToMapListWithFilteringJobWorkload(jobs, null, jobMapInCronJob); // CronJob이 생성한 Job만 필터링..

                                        /** CronJob에 해당하는 pod만 필터링 하기 위한 맵 정의 **/
                                        Map<String, List<V1Pod>> podMapInCronJob = new HashMap<>();

                                        /** CronJob에 해당하는 pod를 필터링 (with Jobs) **/
                                        String uniqueName = serverStateService.makeClusterUniqueName(currCronJob.getMetadata().getNamespace(), currCronJob.getMetadata().getName());
                                        Map<String, V1Job> jobInCron = jobMapInCronJob.get(uniqueName);
                                        if(MapUtils.isNotEmpty(jobInCron)) {
                                            // 1-2. 해당 Job 목록을 ownerRefrence로 가지고 있는 Pod 목록을 조회...
                                            for(Map.Entry<String, V1Job> jobRow : jobInCron.entrySet()) {
                                                String uniqueJobName = serverStateService.makeClusterUniqueName(jobRow.getValue().getMetadata().getNamespace(), jobRow.getValue().getMetadata().getName());
                                                if (MapUtils.getObject(podMap, uniqueJobName, null) != null) {
                                                    // 1-3. 해당 Pod 목록을 Current Deployment의 이름으로 Map에 저장..
                                                    podMapInCronJob.put(uniqueName, MapUtils.getObject(podMap, uniqueJobName, null));
                                                }
                                            }
                                        }
                                        else {
                                            podMapInCronJob.putAll(podMap);
                                        }

                                        List<V1beta1CronJob> cronJobs = new ArrayList<>();
                                        Map<String, V1beta1CronJob> cronJobMap = new HashMap<>();
                                        cronJobs.add(currCronJob);
                                        serverStateService.getCronJobV1beta1ToMap(cronJobs, cronJobMap);

                                        component = serverStateService.addComponentDefault(cluster, currCronJob.getMetadata().getNamespace(), WorkloadType.CRON_JOB_SERVER, null, null, currCronJob, component);
                                        serverStateService.actualizeDeploymentState(cluster, component, cronJobMap, jobMapInCronJob, podMapInCronJob, null, null, false, componentDao);
                                        currState = StringUtils.isNotBlank(component.getStateCode()) ? StateCode.valueOf(component.getStateCode()) : StateCode.STOPPED;
                                    }

                                    // generate yaml string
                                    currCronJob.setStatus(null);
                                    yamlStr.append(ServerUtils.marshalYaml(currCronJob));

                                }
                            } else if (cronType.getGroupType() == K8sApiGroupType.BATCH && cronType.getApiType() == K8sApiType.V1) {
                                V1CronJob currCronJob = null;
                                if (MapUtils.getObject(k8sResourceMap.get(K8sApiKindType.CRON_JOB.getValue()), workloadName, null) != null) {
                                    currCronJob = (V1CronJob) (k8sResourceMap.get(K8sApiKindType.CRON_JOB.getValue())).get(workloadName);
                                }

                                if (currCronJob != null) {
                                    if (canActualizeState) {
                                        /** 1. Job목록 중 CronJob이 생성한 Job만 따로 분류.. **/
                                        List<V1Job> jobs = new ArrayList<>();
                                        jobs.addAll(k8sWorker.getJobsV1(cluster, cluster.getNamespaceName(), null, null));
                                        Map<String, Map<String, V1Job>> jobMapInCronJob = new HashMap<>();
                                        serverStateService.getJobToMapListWithFilteringJobWorkload(jobs, null, jobMapInCronJob); // CronJob이 생성한 Job만 필터링..

                                        /** CronJob에 해당하는 pod만 필터링 하기 위한 맵 정의 **/
                                        Map<String, List<V1Pod>> podMapInCronJob = new HashMap<>();

                                        /** CronJob에 해당하는 pod를 필터링 (with Jobs) **/
                                        String uniqueName = serverStateService.makeClusterUniqueName(currCronJob.getMetadata().getNamespace(), currCronJob.getMetadata().getName());
                                        Map<String, V1Job> jobInCron = jobMapInCronJob.get(uniqueName);
                                        if(MapUtils.isNotEmpty(jobInCron)) {
                                            // 1-2. 해당 Job 목록을 ownerRefrence로 가지고 있는 Pod 목록을 조회...
                                            for(Map.Entry<String, V1Job> jobRow : jobInCron.entrySet()) {
                                                String uniqueJobName = serverStateService.makeClusterUniqueName(jobRow.getValue().getMetadata().getNamespace(), jobRow.getValue().getMetadata().getName());
                                                if (MapUtils.getObject(podMap, uniqueJobName, null) != null) {
                                                    // 1-3. 해당 Pod 목록을 Current Deployment의 이름으로 Map에 저장..
                                                    podMapInCronJob.put(uniqueName, MapUtils.getObject(podMap, uniqueJobName, null));
                                                }
                                            }
                                        }
                                        else {
                                            podMapInCronJob.putAll(podMap);
                                        }

                                        List<V1CronJob> cronJobs = new ArrayList<>();
                                        Map<String, V1CronJob> cronJobMap = new HashMap<>();
                                        cronJobs.add(currCronJob);
                                        serverStateService.getCronJobV1ToMap(cronJobs, cronJobMap);

                                        component = serverStateService.addComponentDefault(cluster, currCronJob.getMetadata().getNamespace(), WorkloadType.CRON_JOB_SERVER, null, null, currCronJob, component);
                                        serverStateService.actualizeDeploymentState(cluster, component, cronJobMap, jobMapInCronJob, podMapInCronJob, null, null, false, componentDao);
                                        currState = StringUtils.isNotBlank(component.getStateCode()) ? StateCode.valueOf(component.getStateCode()) : StateCode.STOPPED;
                                    }

                                    // generate yaml string
                                    currCronJob.setStatus(null);
                                    yamlStr.append(ServerUtils.marshalYaml(currCronJob));

                                }
                            }
                        }
                    }

                    /** Deployment외에 추가로 StatefulSet에서 AutoScaler 를 적용하면서 코드 이동 (AS-IS : 위 if(deploymentType일 경우에 처리...) **/
                    if(workloadType.isPossibleAutoscaling()) {
                        K8sApiVerKindType hpaType = ResourceUtil.getApiVerKindType(cluster, K8sApiKindType.HORIZONTAL_POD_AUTOSCALER);

                        // hpa
                        if (hpaType != null && hpaType.getGroupType() == K8sApiGroupType.AUTOSCALING) {
                            if (hpaType.getApiType() == K8sApiType.V2) {
                                V2HorizontalPodAutoscaler currHpa = null;
                                if (MapUtils.getObject(k8sResourceMap.get(K8sApiKindType.HORIZONTAL_POD_AUTOSCALER.getValue()), workloadName, null) != null) {
                                    currHpa = (V2HorizontalPodAutoscaler) (k8sResourceMap.get(K8sApiKindType.HORIZONTAL_POD_AUTOSCALER.getValue())).get(workloadName);
                                }

                                // 값이 있다면 yaml로 변환
                                if (currHpa != null) {
                                    currHpa.setStatus(null);
                                    yamlStr.append("---\n").append(ServerUtils.marshalYaml(currHpa));
                                }
                            } else if (hpaType.getApiType() == K8sApiType.V2BETA2) {
                                V2beta2HorizontalPodAutoscaler currHpa = null;
                                if (MapUtils.getObject(k8sResourceMap.get(K8sApiKindType.HORIZONTAL_POD_AUTOSCALER.getValue()), workloadName, null) != null) {
                                    currHpa = (V2beta2HorizontalPodAutoscaler) (k8sResourceMap.get(K8sApiKindType.HORIZONTAL_POD_AUTOSCALER.getValue())).get(workloadName);
                                }

                                // 값이 있다면 yaml로 변환
                                if (currHpa != null) {
                                    currHpa.setStatus(null);
                                    yamlStr.append("---\n").append(ServerUtils.marshalYaml(currHpa));
                                }
                            } else {
                                V2beta1HorizontalPodAutoscaler currHpa = null;
                                if (MapUtils.getObject(k8sResourceMap.get(K8sApiKindType.HORIZONTAL_POD_AUTOSCALER.getValue()), workloadName, null) != null) {
                                    currHpa = (V2beta1HorizontalPodAutoscaler) (k8sResourceMap.get(K8sApiKindType.HORIZONTAL_POD_AUTOSCALER.getValue())).get(workloadName);
                                }

                                // 값이 있다면 yaml로 변환
                                if (currHpa != null) {
                                    currHpa.setStatus(null);
                                    yamlStr.append("---\n").append(ServerUtils.marshalYaml(currHpa));
                                }
                            }
                        }

                    }

                    log.debug("workload yaml : \n{}", yamlStr.toString());

                    // check yaml
                    List<Object> yamlObjs = ServerUtils.getYamlObjects(yamlStr.toString());

//                    componentService.updateComponentState(ContextHolder.exeContext(), component, currState, componentDao);
//                    componentDao.updateComponentManifestByNamespace(cluster.getClusterSeq(), namespace, workloadName, yamlStr.toString(), ContextHolder.exeContext().getUserSeq());

                    // 파이프라인과 연결되어 있는 지 확인하여 buildSeq 셋팅
                    PipelineWorkloadVO pipelineWorkload = pipelineDao.getPipelineWorkloadByWorkload(component.getClusterSeq(), component.getNamespaceName(), component.getComponentName());
                    List<PipelineContainerVO> pipelineContainers = Optional.ofNullable(pipelineWorkload).map(PipelineWorkloadVO::getPipelineContainers).orElseGet(() ->Lists.newArrayList());

                    if (deployType == DeployType.GUI) {
                        ServerGuiVO serverGui = serverConversionService.convertYamlToGui(cluster, namespace, component.getWorkloadType(), component.getWorkloadVersion(), component.getDescription(), component, yamlObjs);
                        serverGui.getComponent().setStateCode(currState.getCode());

                        List<ContainerVO> allContainers = Lists.newArrayList();
                        ResourceUtil.mergeContainer(allContainers, serverGui.getInitContainers(), serverGui.getContainers());

                        for (ContainerVO cRow : allContainers) {
                            Optional<PipelineContainerVO> pipelineContainerOptional = pipelineContainers.stream()
                                    .filter(pc -> (pc.getPipelineType() == PipelineType.BUILD_DEPLOY && pc.getBuildSeq() != null && StringUtils.equals(pc.getDeployImageUrl(), cRow.getFullImageName()))).findFirst();
                            if (pipelineContainerOptional.isPresent()) {
                                cRow.setBuildSeq(pipelineContainerOptional.get().getBuildSeq());
                            }
                        }

                        serverIntegrate = serverGui;
                    } else {
                        ServerYamlVO serverYaml = new ServerYamlVO();
                        serverYaml.setDeployType(DeployType.YAML.getCode());
                        serverYaml.setWorkloadType(component.getWorkloadType());
                        serverYaml.setWorkloadVersion(component.getWorkloadVersion());
                        serverYaml.setClusterSeq(cluster.getClusterSeq());
                        serverYaml.setNamespaceName(namespace);
                        serverYaml.setWorkloadName(component.getComponentName());
                        serverYaml.setDescription(component.getDescription());
                        serverYaml.setStateCode(currState.getCode());
                        serverYaml.setYaml(yamlStr.toString());

                        serverIntegrate = serverYaml;
                    }
                } else {
                    k8sResourceExists = false;
                }
            } else {
                k8sResourceExists = false;
            }

            if (!k8sResourceExists) {
                // 워크로드가 STOPPED 이거나 오류로 인하여 배포하지 못한 상태일 경우
                // DB에서 조회한 yaml로 처리
                if (component != null) {
//                    StateCode stateCode = StateCode.valueOf(component.getStateCode());
//                    if (stateCode == StateCode.STOPPING || stateCode == StateCode.STOPPED || (stateCode == StateCode.ERROR && StringUtils.isNotBlank(component.getWorkloadManifest()))) {
//
//                        // check yaml
//                        List<Object> objs = ServerUtils.getYamlObjects(component.getWorkloadManifest());
//
//                        if (deployType == DeployType.GUI) {
//                            serverIntegrate = serverConversionService.convertYamlToGui(cluster, namespace, component.getWorkloadType(), component.getWorkloadVersion(), component.getDescription(), component, objs);
//                        } else {
//                            ServerYamlVO serverYaml = new ServerYamlVO();
//                            serverYaml.setDeployType(DeployType.YAML.getCode());
//                            serverYaml.setWorkloadType(component.getWorkloadType());
//                            serverYaml.setWorkloadVersion(component.getWorkloadVersion());
//                            serverYaml.setClusterSeq(cluster.getClusterSeq());
//                            serverYaml.setNamespaceName(cluster.getNamespaceName());
//                            serverYaml.setWorkloadName(component.getComponentName());
//                            serverYaml.setDescription(component.getDescription());
//                            serverYaml.setStateCode(component.getStateCode());
//                            serverYaml.setYaml(component.getWorkloadManifest());
//
//                            serverIntegrate = serverYaml;
//                        }
//
//                        return serverIntegrate;
//                    } else {
//                        serverIntegrate = null;
//                    }


                    if (deployType == DeployType.GUI) {
                        // check yaml
                        List<Object> objs = ServerUtils.getYamlObjects(component.getWorkloadManifest());
                        serverIntegrate = serverConversionService.convertYamlToGui(cluster, namespace, component.getWorkloadType(), component.getWorkloadVersion(), component.getDescription(), component, objs);
                    } else {
                        ServerYamlVO serverYaml = new ServerYamlVO();
                        serverYaml.setDeployType(DeployType.YAML.getCode());
                        serverYaml.setWorkloadType(component.getWorkloadType());
                        serverYaml.setWorkloadVersion(component.getWorkloadVersion());
                        serverYaml.setClusterSeq(cluster.getClusterSeq());
                        serverYaml.setNamespaceName(cluster.getNamespaceName());
                        serverYaml.setWorkloadName(component.getComponentName());
                        serverYaml.setDescription(component.getDescription());
                        serverYaml.setStateCode(component.getStateCode());
                        serverYaml.setYaml(component.getWorkloadManifest());

                        serverIntegrate = serverYaml;
                    }

                    return serverIntegrate;

                } else {
                    serverIntegrate = null;
                }
            }

        } else {
            serverIntegrate = null;
        }



        return serverIntegrate;
    }

    public V1Deployment getV1Deployment(String workloadName, ClusterVO cluster) throws Exception {
        return k8sWorker.getDeploymentV1(cluster, cluster.getNamespaceName(), workloadName);
    }
    public V1StatefulSet getV1StatefulSet(String workloadName, ClusterVO cluster) throws Exception {
        return k8sWorker.getStatefulSetV1(cluster, cluster.getNamespaceName(), workloadName);
    }
    public V1DaemonSet getV1DaemonSet(String workloadName, ClusterVO cluster) throws Exception {
        return k8sWorker.getDaemonSetV1(cluster, cluster.getNamespaceName(), workloadName);
    }
    public V1Job getV1Job(String workloadName, ClusterVO cluster) throws Exception {
        return k8sWorker.getJobV1(cluster, cluster.getNamespaceName(), workloadName);
    }
    public V1beta1CronJob getV1beta1CronJob(String workloadName, ClusterVO cluster) throws Exception {
        return k8sWorker.getCronJobV1beta1(cluster, cluster.getNamespaceName(), workloadName);
    }
    public V1CronJob getV1CronJob(String workloadName, ClusterVO cluster) throws Exception {
        return k8sWorker.getCronJobV1(cluster, cluster.getNamespaceName(), workloadName);
    }

    public ServerIntegrateVO getWorkloadDetail(DeployType deployType, String workloadName, ClusterVO cluster, String namespace, Map<String, Object> k8sResourceMap) throws Exception {
        IComponentMapper componentDao = this.sqlSession.getMapper(IComponentMapper.class);
        IServicemapMapper servicemapDao = sqlSession.getMapper(IServicemapMapper.class);

        if (cluster == null) {
            throw new CocktailException("Invalid cluster Information ", ExceptionType.InvalidInputData);
        }

        ComponentVO component = componentDao.getComponentByClusterAndNames(cluster.getClusterSeq(), namespace, workloadName);

        ServerIntegrateVO serverIntegrate = new ServerIntegrateVO();
        serverIntegrate.setDeployType(deployType.getCode());

        StringBuffer yamlStr = new StringBuffer();

        K8sApiVerKindType deploymentType = ResourceUtil.getApiVerKindType(cluster, K8sApiKindType.DEPLOYMENT);
        V1Deployment currDeployment = null;
        if (deploymentType != null) {
            if (deploymentType.getGroupType() == K8sApiGroupType.APPS && deploymentType.getApiType() == K8sApiType.V1) {
                currDeployment = k8sWorker.getDeploymentV1(cluster, namespace, workloadName);
            }
        }

        K8sApiVerKindType statefulSetType = ResourceUtil.getApiVerKindType(cluster, K8sApiKindType.STATEFUL_SET);
        V1StatefulSet currStatefulSet = null;
        if (statefulSetType != null) {
            if (statefulSetType.getGroupType() == K8sApiGroupType.APPS && statefulSetType.getApiType() == K8sApiType.V1) {
                currStatefulSet = k8sWorker.getStatefulSetV1(cluster, namespace, workloadName);
            }
        }

        K8sApiVerKindType daemonSetType = ResourceUtil.getApiVerKindType(cluster, K8sApiKindType.DAEMON_SET);
        V1DaemonSet currDaemonSet = null;
        if (daemonSetType != null) {
            if (daemonSetType.getGroupType() == K8sApiGroupType.APPS && daemonSetType.getApiType() == K8sApiType.V1) {
                currDaemonSet = k8sWorker.getDaemonSetV1(cluster, namespace, workloadName);
            }
        }

        K8sApiVerKindType jobType = ResourceUtil.getApiVerKindType(cluster, K8sApiKindType.JOB);
        V1Job currJob = null;
        if (jobType != null) {
            if (jobType.getGroupType() == K8sApiGroupType.BATCH && jobType.getApiType() == K8sApiType.V1) {
                currJob = k8sWorker.getJobV1(cluster, namespace, workloadName);
            }
        }

        K8sApiVerKindType cronJobType = ResourceUtil.getApiVerKindType(cluster, K8sApiKindType.CRON_JOB);
        V1beta1CronJob currCronJobV1beta1 = null;
        V1CronJob currCronJobV1 = null;
        if (cronJobType != null) {
            if (cronJobType.getGroupType() == K8sApiGroupType.BATCH && cronJobType.getApiType() == K8sApiType.V1BETA1) {
                currCronJobV1beta1 = k8sWorker.getCronJobV1beta1(cluster, namespace, workloadName);
            } else if (cronJobType.getGroupType() == K8sApiGroupType.BATCH && cronJobType.getApiType() == K8sApiType.V1) {
                currCronJobV1 = k8sWorker.getCronJobV1(cluster, namespace, workloadName);
            }
        }
        V2HorizontalPodAutoscaler currHpaV2 = null;
        V2beta2HorizontalPodAutoscaler currHpaV2beta2 = null;
        V2beta1HorizontalPodAutoscaler currHpaV2beta1 = null;
        K8sApiVerKindType hpaType = ResourceUtil.getApiVerKindType(cluster, K8sApiKindType.HORIZONTAL_POD_AUTOSCALER);
        if (currDeployment != null || currStatefulSet != null) {
            if (hpaType != null) {
                if (hpaType.getApiType() == K8sApiType.V2) {
                    currHpaV2 = k8sWorker.getHorizontalPodAutoscalerV2(cluster, namespace, workloadName);
                } else if (hpaType.getApiType() == K8sApiType.V2BETA2) {
                    currHpaV2beta2 = k8sWorker.getHorizontalPodAutoscalerV2beta2(cluster, namespace, workloadName);
                } else if (hpaType.getApiType() == K8sApiType.V2BETA1) {
                    currHpaV2beta1 = k8sWorker.getHorizontalPodAutoscalerV2beta1(cluster, namespace, workloadName);
                }
            }
        }

        // Pod 조회
        List<V1Pod> pods = new ArrayList<>();
        Map<String, List<V1Pod>> podMap = new HashMap<>();
        pods.addAll(k8sWorker.getPodV1WithLabel(cluster, namespace, null, null));
        serverStateService.getPodToMap(pods, podMap);

        WorkloadType workloadType = null;
        if(currDeployment != null) {
            workloadType = WorkloadType.REPLICA_SERVER;
        }
        else if(currStatefulSet != null){
            workloadType = WorkloadType.STATEFUL_SET_SERVER;
        }
        else if(currDaemonSet != null) {
            workloadType = WorkloadType.DAEMON_SET_SERVER;
        }
        else if(currJob != null) {
            workloadType = WorkloadType.JOB_SERVER;
        }
        else if(currCronJobV1beta1 != null || currCronJobV1 != null) {
            workloadType = WorkloadType.CRON_JOB_SERVER;
        }
        else {
            // K8s로부터 조회된 데이터가 없고 Component 테이블에서 조회된 데이터만 존재할때....
            if (component != null) {
                StateCode stateCode = StateCode.valueOf(component.getStateCode());
                if (stateCode == StateCode.STOPPED || (stateCode == StateCode.ERROR && StringUtils.isNotBlank(component.getWorkloadManifest()))) {
                    if (deployType == DeployType.GUI) {
                        serverIntegrate = serverConversionService.convertYamlToGui(cluster, namespace, component.getWorkloadType(), component.getWorkloadVersion(), component.getDescription(), component, component.getWorkloadManifest());
                        ((ServerGuiVO) serverIntegrate).setComponent(component);
                    }
                    else {
                        ServerYamlVO serverYaml = new ServerYamlVO();
                        serverYaml.setDeployType(DeployType.YAML.getCode());
                        serverYaml.setWorkloadType(component.getWorkloadType());
                        serverYaml.setWorkloadVersion(component.getWorkloadVersion());
                        serverYaml.setClusterSeq(cluster.getClusterSeq());
                        serverYaml.setNamespaceName(cluster.getNamespaceName());
                        serverYaml.setWorkloadName(component.getComponentName());
                        serverYaml.setDescription(component.getDescription());
                        serverYaml.setYaml(component.getWorkloadManifest());

                        serverIntegrate = serverYaml;
                    }
                }

                return serverIntegrate;
            }
        }

        //WorkloadVersion Default를 설정.. 컴포넌트 정보가 있다면 재 설정
        WorkloadVersion workloadVersion = WorkloadVersion.V1;
        WorkloadVersionSet workloadVersionSet = WorkloadVersionSet.getSupport(workloadType, workloadVersion);

        // component 정보가 있으면 워크로드 타입 판단 및 워크로드 버전 Component 정보에 따라 맞춰줌..
        if(component != null) {
            WorkloadType componentWorkloadType = WorkloadType.valueOf(component.getWorkloadType());
            if(workloadType == null) {
                workloadType = componentWorkloadType;
            }
            // K8s에서 조회한 워크로드와 Component 테이블의 워크로드가 다르면 오류 처리
            else if (!workloadType.getCode().equals(componentWorkloadType.getCode())) {
                // SINGLE과 REPLICA는 서로 다르지 않은 것으로 판단한다..
                if (workloadType == WorkloadType.REPLICA_SERVER && componentWorkloadType == WorkloadType.SINGLE_SERVER) {
                    // workloadType을 component 테이블의 워크로드 타입으로 변경..
                    workloadType = componentWorkloadType;
                }
                else {
                    throw new CocktailException("Invalid Workload Information ", ExceptionType.InvalidInputData);
                }
            }
            workloadVersion = WorkloadVersion.valueOf(component.getWorkloadVersion());
            workloadVersionSet = WorkloadVersionSet.getSupport(workloadType, workloadVersion);
        }

        if(workloadType == null) {
            // 조회된 내용이 없으면 null
            return null;
        }
        /** 워크로드 기본 정보 설정.. **/
        else {
            switch (workloadType) {
                case SINGLE_SERVER:
                case REPLICA_SERVER: {
                    if (currDeployment != null) {
                        V1DeploymentStatus backupStatus = currDeployment.getStatus();
                        currDeployment.setStatus(null);
                        yamlStr.append(ServerUtils.marshalYaml(currDeployment));
                        currDeployment.setStatus(backupStatus);
                    }

                    // hpa
                    if (hpaType != null && hpaType.getGroupType() == K8sApiGroupType.AUTOSCALING) {
                        if (hpaType.getApiType() == K8sApiType.V2) {
                            if (currHpaV2 != null) {
                                currHpaV2.setStatus(null);
                                yamlStr.append("---\n").append(ServerUtils.marshalYaml(currHpaV2));
                            }
                        } else if (hpaType.getApiType() == K8sApiType.V2BETA2) {
                            if (currHpaV2beta2 != null) {
                                currHpaV2beta2.setStatus(null);
                                yamlStr.append("---\n").append(ServerUtils.marshalYaml(currHpaV2beta2));
                            }
                        } else if (hpaType.getApiType() == K8sApiType.V2BETA1) {
                            if (currHpaV2beta1 != null) {
                                currHpaV2beta1.setStatus(null);
                                yamlStr.append("---\n").append(ServerUtils.marshalYaml(currHpaV2beta1));
                            }
                        }

                    }
                    break;
                }
                case STATEFUL_SET_SERVER: {
                    if (currStatefulSet != null) {
                        V1StatefulSetStatus backupStatus = currStatefulSet.getStatus();
                        currStatefulSet.setStatus(null);
                        yamlStr.append("---\n").append(ServerUtils.marshalYaml(currStatefulSet));
                        currStatefulSet.setStatus(backupStatus);
                    }
                    break;
                }
                case DAEMON_SET_SERVER: {
                    if (currDaemonSet != null) {
                        V1DaemonSetStatus backupStatus = currDaemonSet.getStatus();
                        currDaemonSet.setStatus(null);
                        yamlStr.append(ServerUtils.marshalYaml(currDaemonSet));
                        currDaemonSet.setStatus(backupStatus);
                    }

                    break;
                }
                case JOB_SERVER: {
                    if (currJob != null) {
                        V1JobStatus backupStatus = currJob.getStatus();
                        currJob.setStatus(null);
                        yamlStr.append(ServerUtils.marshalYaml(currJob));
                        currJob.setStatus(backupStatus);
                    }

                    break;
                }
                case CRON_JOB_SERVER: {
                    if (currCronJobV1beta1 != null) {
                        V1beta1CronJobStatus backupStatus = currCronJobV1beta1.getStatus();
                        currCronJobV1beta1.setStatus(null);
                        yamlStr.append(ServerUtils.marshalYaml(currCronJobV1beta1));
                        currCronJobV1beta1.setStatus(backupStatus);
                    } else if (currCronJobV1 != null) {
                        V1CronJobStatus backupStatus = currCronJobV1.getStatus();
                        currCronJobV1.setStatus(null);
                        yamlStr.append(ServerUtils.marshalYaml(currCronJobV1));
                        currCronJobV1.setStatus(backupStatus);
                    }

                    break;
                }
            }
        }

        log.debug("workload yaml : \n{}", yamlStr.toString());

        /** 워크로드의 Actualize State 설정.. **/
        if (deployType == DeployType.GUI) {
            ServerGuiVO serverGui = serverConversionService.convertYamlToGui(cluster, namespace, workloadType.getCode(), workloadVersion.getCode(), "", null, yamlStr.toString());
            String resourcePrefixSource = ResourceUtil.getResourcePrefix();

            ServicemapSummaryVO servicemapInfo = servicemapDao.getServicemapSummary(cluster.getClusterSeq(), cluster.getNamespaceName(), null);

            if(true) {
                // Actualize Deployment State도 조회..
                ComponentVO compo = null;


                switch (workloadType) {
                    case SINGLE_SERVER:
                    case REPLICA_SERVER: {
                        /** ReplicaSet 작성.. **/
                        List<V1ReplicaSet> replicaSets = new ArrayList<>();
                        Map<String, List<V1ReplicaSet>> replicaSetMap = new HashMap<>();
                        replicaSets.addAll(k8sWorker.getReplicaSetsV1(cluster, namespace, null, ResourceUtil.getLabelFilterOfSelector(currDeployment.getSpec().getSelector())));
                        serverStateService.getReplicaSetToMap(replicaSets, replicaSetMap);
                        /** Deployment에 해당하는 pod만 필터링 하기 위한 맵 정의 **/
                        Map<String, List<V1Pod>> podMapInDeployment = new HashMap<>();

                        /** Deployment에 해당하는 pod를 필터링 (with ReplicaSets) **/
                        String uniqueName = serverStateService.makeClusterUniqueName(currDeployment.getMetadata().getNamespace(), currDeployment.getMetadata().getName());
                        List<V1ReplicaSet> currReplicaSet = replicaSetMap.get(uniqueName);
                        if(CollectionUtils.isNotEmpty(currReplicaSet)) {
                            // 1-2. 해당 ReplicaSet 목록을 ownerRefrence로 가지고 있는 Pod 목록을 조회...
                            for(V1ReplicaSet replicaSetRow : currReplicaSet) {
                                String uniqueReplicaSetName = serverStateService.makeClusterUniqueName(replicaSetRow.getMetadata().getNamespace(), replicaSetRow.getMetadata().getName());
                                if (MapUtils.getObject(podMap, uniqueReplicaSetName, null) != null) {
                                    if (MapUtils.getObject(podMapInDeployment, uniqueName, null) == null) {
                                        podMapInDeployment.put(uniqueName, Lists.newArrayList());
                                    }
                                    // 1-3. 해당 Pod 목록을 Current Deployment의 이름으로 Map에 저장..
                                    podMapInDeployment.get(uniqueName).addAll(MapUtils.getObject(podMap, uniqueReplicaSetName, Lists.newArrayList()));
                                }
                            }
                        }
                        else {
                            podMapInDeployment.putAll(podMap);
                        }

                        /** Deployment에 대한 actualize State 조회 **/
                        List<V1Deployment> deployments = new ArrayList<>();
                        Map<String, V1Deployment> deploymentMap = new HashMap<>();
                        deployments.add(currDeployment);
                        serverStateService.getDeploymentToMap(deployments, deploymentMap);

                        if(component != null) {
                            compo = serverStateService.addComponentDefault(cluster, currDeployment.getMetadata().getNamespace(), workloadType, resourcePrefixSource, servicemapInfo, currDeployment, component);
                        }
                        else {
                            compo = serverStateService.addComponentDefault(cluster, currDeployment.getMetadata().getNamespace(), workloadType, resourcePrefixSource, servicemapInfo, currDeployment);
                        }
                        serverStateService.actualizeDeploymentState(cluster, component, deploymentMap, Collections.emptyMap(), podMapInDeployment, null, null, false, componentDao);
                        break;
                    }
                    case STATEFUL_SET_SERVER: {
                        List<V1StatefulSet> statefulSets = new ArrayList<>();
                        Map<String, V1StatefulSet> statefulSetMap = new HashMap<>();
                        statefulSets.add(currStatefulSet);
                        serverStateService.getStatefulSetToMap(statefulSets, statefulSetMap);

                        if(component != null) {
                            compo = serverStateService.addComponentDefault(cluster, currStatefulSet.getMetadata().getNamespace(), workloadType, resourcePrefixSource, servicemapInfo, currStatefulSet, component);
                        }
                        else {
                            compo = serverStateService.addComponentDefault(cluster, currStatefulSet.getMetadata().getNamespace(), workloadType, resourcePrefixSource, servicemapInfo, currStatefulSet);
                        }
                        serverStateService.actualizeDeploymentState(cluster, component, statefulSetMap, Collections.emptyMap(), podMap, null, null, false, componentDao);
                        break;
                    }
                    case DAEMON_SET_SERVER: {
                        List<V1DaemonSet> daemonSets = new ArrayList<>();
                        Map<String, V1DaemonSet> daemonSetMap = new HashMap<>();
                        daemonSets.add(currDaemonSet);
                        serverStateService.getDaemonSetToMap(daemonSets, daemonSetMap);

                        if(component != null) {
                            compo = serverStateService.addComponentDefault(cluster, currDaemonSet.getMetadata().getNamespace(), workloadType, resourcePrefixSource, servicemapInfo, currDaemonSet, component);
                        }
                        else {
                            compo = serverStateService.addComponentDefault(cluster, currDaemonSet.getMetadata().getNamespace(), workloadType, resourcePrefixSource, servicemapInfo, currDaemonSet);
                        }
                        serverStateService.actualizeDeploymentState(cluster, component, daemonSetMap, Collections.emptyMap(), podMap, null, null, false, componentDao);
                        break;
                    }
                    case JOB_SERVER: {
                        List<V1Job> jobs = new ArrayList<>();
                        Map<String, V1Job> jobMap = new HashMap<>();
                        jobs.add(currJob);
                        serverStateService.getJobToMap(jobs, jobMap);

                        if(component != null) {
                            compo = serverStateService.addComponentDefault(cluster, currJob.getMetadata().getNamespace(), workloadType, resourcePrefixSource, servicemapInfo, currJob, component);
                        }
                        else {
                            compo = serverStateService.addComponentDefault(cluster, currJob.getMetadata().getNamespace(), workloadType, resourcePrefixSource, servicemapInfo, currJob);
                        }
                        serverStateService.actualizeDeploymentState(cluster, component, jobMap, Collections.emptyMap(), podMap, null, null, false, componentDao);
                        break;
                    }
                    case CRON_JOB_SERVER: {
                        /** 1. Job목록 중 CronJob이 생성한 Job만 따로 분류.. **/
                        List<V1Job> jobs = new ArrayList<>();
                        jobs.addAll(k8sWorker.getJobsV1(cluster, namespace, null, null));
                        Map<String, Map<String, V1Job>> jobMapInCronJob = new HashMap<>();
                        serverStateService.getJobToMapListWithFilteringJobWorkload(jobs, null, jobMapInCronJob); // CronJob이 생성한 Job만 필터링..

                        /** CronJob에 해당하는 pod만 필터링 하기 위한 맵 정의 **/
                        Map<String, List<V1Pod>> podMapInCronJob = new HashMap<>();

                        /** CronJob에 해당하는 pod를 필터링 (with Jobs) **/
                        if (currCronJobV1beta1 != null) {
                            String uniqueName = serverStateService.makeClusterUniqueName(currCronJobV1beta1.getMetadata().getNamespace(), currCronJobV1beta1.getMetadata().getName());
                            Map<String, V1Job> jobInCron = jobMapInCronJob.get(uniqueName);
                            if (MapUtils.isNotEmpty(jobInCron)) {
                                // 1-2. 해당 Job 목록을 ownerRefrence로 가지고 있는 Pod 목록을 조회...
                                for (Map.Entry<String, V1Job> jobRow : jobInCron.entrySet()) {
                                    String uniqueJobName = serverStateService.makeClusterUniqueName(jobRow.getValue().getMetadata().getNamespace(), jobRow.getValue().getMetadata().getName());
                                    if (MapUtils.getObject(podMap, uniqueJobName, null) != null) {
                                        if (MapUtils.getObject(podMapInCronJob, uniqueName, null) == null) {
                                            podMapInCronJob.put(uniqueName, Lists.newArrayList());
                                        }
                                        // 1-3. 해당 Pod 목록을 Current Deployment의 이름으로 Map에 저장..
                                        podMapInCronJob.get(uniqueName).addAll(MapUtils.getObject(podMap, uniqueJobName, Lists.newArrayList()));
                                    }
                                }
                            } else {
                                podMapInCronJob.putAll(podMap);
                            }

                            List<V1beta1CronJob> cronJobs = new ArrayList<>();
                            Map<String, V1beta1CronJob> cronJobMap = new HashMap<>();
                            cronJobs.add(currCronJobV1beta1);
                            serverStateService.getCronJobV1beta1ToMap(cronJobs, cronJobMap);

                            if (component != null) {
                                compo = serverStateService.addComponentDefault(cluster, currCronJobV1beta1.getMetadata().getNamespace(), workloadType, resourcePrefixSource, servicemapInfo, currCronJobV1beta1, component);
                            } else {
                                compo = serverStateService.addComponentDefault(cluster, currCronJobV1beta1.getMetadata().getNamespace(), workloadType, resourcePrefixSource, servicemapInfo, currCronJobV1beta1);
                            }
                            serverStateService.actualizeDeploymentState(cluster, compo, cronJobMap, jobMapInCronJob, podMapInCronJob, null, null, true, componentDao);
                        } else if (currCronJobV1 != null) {
                            String uniqueName = serverStateService.makeClusterUniqueName(currCronJobV1.getMetadata().getNamespace(), currCronJobV1.getMetadata().getName());
                            Map<String, V1Job> jobInCron = jobMapInCronJob.get(uniqueName);
                            if (MapUtils.isNotEmpty(jobInCron)) {
                                // 1-2. 해당 Job 목록을 ownerRefrence로 가지고 있는 Pod 목록을 조회...
                                for (Map.Entry<String, V1Job> jobRow : jobInCron.entrySet()) {
                                    String uniqueJobName = serverStateService.makeClusterUniqueName(jobRow.getValue().getMetadata().getNamespace(), jobRow.getValue().getMetadata().getName());
                                    if (MapUtils.getObject(podMap, uniqueJobName, null) != null) {
                                        if (MapUtils.getObject(podMapInCronJob, uniqueName, null) == null) {
                                            podMapInCronJob.put(uniqueName, Lists.newArrayList());
                                        }
                                        // 1-3. 해당 Pod 목록을 Current Deployment의 이름으로 Map에 저장..
                                        podMapInCronJob.get(uniqueName).addAll(MapUtils.getObject(podMap, uniqueJobName, Lists.newArrayList()));
                                    }
                                }
                            } else {
                                podMapInCronJob.putAll(podMap);
                            }

                            List<V1CronJob> cronJobs = new ArrayList<>();
                            Map<String, V1CronJob> cronJobMap = new HashMap<>();
                            cronJobs.add(currCronJobV1);
                            serverStateService.getCronJobV1ToMap(cronJobs, cronJobMap);

                            if (component != null) {
                                compo = serverStateService.addComponentDefault(cluster, currCronJobV1.getMetadata().getNamespace(), workloadType, resourcePrefixSource, servicemapInfo, currCronJobV1, component);
                            } else {
                                compo = serverStateService.addComponentDefault(cluster, currCronJobV1.getMetadata().getNamespace(), workloadType, resourcePrefixSource, servicemapInfo, currCronJobV1);
                            }
                            serverStateService.actualizeDeploymentState(cluster, compo, cronJobMap, jobMapInCronJob, podMapInCronJob, null, null, true, componentDao);
                        }
                        break;
                    }
                }

                if(compo != null) {
                    serverGui.setComponent(compo);
                    // set server actualize state from component state
                    serverGui.getServer().setActiveCount(Optional.ofNullable(compo.getActiveCount()).orElseGet(() ->0));
                    serverGui.getServer().setComputeTotal(compo.getComputeTotal());
                }
            }
            else {
                //old
                List<ContainerVO> allContainers = Lists.newArrayList();
                ResourceUtil.mergeContainer(allContainers, serverGui.getInitContainers(), serverGui.getContainers());
            }
            serverIntegrate = serverGui;
        }
        else {
            ServerYamlVO serverYaml = new ServerYamlVO();
            serverYaml.setDeployType(DeployType.YAML.getCode());
            serverYaml.setWorkloadType(workloadType.getCode());
            serverYaml.setWorkloadVersion(WorkloadVersion.V1.getCode());
            serverYaml.setClusterSeq(cluster.getClusterSeq());
            serverYaml.setNamespaceName(namespace);
            serverYaml.setWorkloadName(workloadName);
            serverYaml.setYaml(yamlStr.toString());
            serverIntegrate = serverYaml;
        }

        return serverIntegrate;
    }

    /**
     * #################################################################################################################################################################################################
     * #################################################################################################################################################################################################
     * end - Workload
     * #################################################################################################################################################################################################
     * #################################################################################################################################################################################################
     */
    public WorkloadVO getWorkloadResource(Integer clusterSeq, String namespace, String workloadName, Boolean acloudOnly) throws Exception {
        IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
        ClusterVO cluster =  clusterDao.getCluster(clusterSeq);

        return this.getWorkloadResource(cluster, namespace, workloadName, acloudOnly);
    }

    public WorkloadVO getWorkloadResource(ClusterVO cluster, String namespace, String workloadName, Boolean acloudOnly) throws Exception {

        WorkloadVO workload = new WorkloadVO();
        workload.setController(new WorkloadControllerVO());
        workload.setPods(Lists.newArrayList());
        workload.setServices(Lists.newArrayList());
        workload.setVolumeMounts(Lists.newArrayList());
        workload.setVolumes(Lists.newArrayList());

        if (cluster != null) {
            String label = String.format("%s=%s", KubeConstants.LABELS_KEY, workloadName);
            String cocktailLabel = null;
            if (BooleanUtils.toBoolean(acloudOnly)) {
                cocktailLabel = KubeConstants.LABELS_COCKTAIL_KEY;
            }

            /** Event **/
            List<K8sEventVO> events = k8sResourceService.convertEventDataList(cluster, namespace, null ,null);
            Map<String, Map<String, List<K8sEventVO>>> eventMap = new HashMap<>();
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

            /** Pod **/
            List<K8sPodVO> pods = new ArrayList<>();
            List<K8sPodVO> allPods = workloadResourceService.getPods(cluster, null, namespace, cocktailLabel, ContextHolder.exeContext());
            Map<String, List<K8sPodVO>> podMap = new HashMap<>();
            addonCommonService.getPodToMap(allPods, podMap, MapUtils.getObject(eventMap, K8sApiKindType.POD.getValue(), Maps.newHashMap()));

            Map<String, String> podLabels = Maps.newHashMap();

            /** Deployment **/
            K8sDeploymentVO deployment = workloadResourceService.getDeployment(cluster, namespace, workloadName, ContextHolder.exeContext());
            if (deployment != null) {
                podLabels = deployment.getDetail().getPodTemplate().getLabels();

                /** ReplicaSet **/
                List<K8sReplicaSetVO> replicaSets = new ArrayList<>();
                List<K8sReplicaSetVO> allRelicaSets = workloadResourceService.convertReplicaSetDataList(cluster, namespace, null, null);
                Map<String, List<K8sReplicaSetVO>> replicaSetMap = new HashMap<>();
                addonCommonService.getReplicaSetToMap(allRelicaSets, replicaSetMap, MapUtils.getObject(eventMap, K8sApiKindType.REPLICA_SET.getValue(), Maps.newHashMap()));
                // Set replicaSet
                if(replicaSetMap.get(deployment.getName()) != null) {
                    replicaSets.addAll(replicaSetMap.get(deployment.getName()));
                    /** Set replicaSet owner pod **/
                    if (CollectionUtils.isNotEmpty(replicaSets)) {
                        for (K8sReplicaSetVO replicaSetRow : replicaSets) {
                            addonCommonService.addPod(pods, podMap, replicaSetRow.getName());
                        }
                    }
                }

                /** Set Event **/
                deployment.setEvents(MapUtils.getObject(eventMap, K8sApiKindType.DEPLOYMENT.getValue(), Maps.newHashMap()).get(deployment.getName()));

                /** Set NewReplicaSet's Events **/
                for(K8sReplicaSetVO rs : deployment.getNewReplicaSets()) {
                    rs.setEvents(MapUtils.getObject(eventMap, K8sApiKindType.REPLICA_SET.getValue(), Maps.newHashMap()).get(rs.getName()));
                }

                /** Set hpa Event **/
                if (CollectionUtils.isNotEmpty(deployment.getHorizontalPodAutoscalers())) {
                    for (K8sHorizontalPodAutoscalerVO hpaRow : deployment.getHorizontalPodAutoscalers()) {
                        hpaRow.setEvents(MapUtils.getObject(eventMap, K8sApiKindType.HORIZONTAL_POD_AUTOSCALER.getValue(), Maps.newHashMap()).get(deployment.getName()));
                    }
                }
                workload.getController().setDeployment(deployment);
                workload.getController().setReplicaSets(replicaSets);

                workload.getController().setWorkloadType(WorkloadType.REPLICA_SERVER.getCode());
            }

            /** DaemonSet **/
            K8sDaemonSetVO daemonSet = workloadResourceService.getDaemonSet(cluster, namespace, workloadName, ContextHolder.exeContext());
            if (daemonSet != null) {
                podLabels = daemonSet.getDetail().getPodTemplate().getLabels();

                daemonSet.setEvents(MapUtils.getObject(eventMap, K8sApiKindType.DAEMON_SET.getValue(), Maps.newHashMap()).get(daemonSet.getName()));
                addonCommonService.addPod(pods, podMap, daemonSet.getName());

                workload.getController().setDaemonSet(daemonSet);
                workload.getController().setWorkloadType(WorkloadType.DAEMON_SET_SERVER.getCode());
            }

            /** StatefulSet **/
            K8sStatefulSetVO statefulSet = workloadResourceService.getStatefulSet(cluster, namespace, workloadName, ContextHolder.exeContext());
            if (statefulSet != null) {
                podLabels = statefulSet.getDetail().getPodTemplate().getLabels();

                statefulSet.setEvents(MapUtils.getObject(eventMap, K8sApiKindType.STATEFUL_SET.getValue(), Maps.newHashMap()).get(statefulSet.getName()));
                addonCommonService.addPod(pods, podMap, statefulSet.getName());

                /** Set hpa Event **/
                if (CollectionUtils.isNotEmpty(statefulSet.getHorizontalPodAutoscalers())) {
                    for (K8sHorizontalPodAutoscalerVO hpaRow : statefulSet.getHorizontalPodAutoscalers()) {
                        hpaRow.setEvents(MapUtils.getObject(eventMap, K8sApiKindType.HORIZONTAL_POD_AUTOSCALER.getValue(), Maps.newHashMap()).get(statefulSet.getName()));
                    }
                }
                workload.getController().setStatefulSet(statefulSet);
                workload.getController().setWorkloadType(WorkloadType.STATEFUL_SET_SERVER.getCode());
            }

            /** CronJob **/
            K8sCronJobVO cronJob = workloadResourceService.getCronJob(cluster, namespace, workloadName, ContextHolder.exeContext());
            if (cronJob != null) {
                List<K8sJobVO> jobs = new ArrayList<>();
                List<K8sJobVO> allJobs = workloadResourceService.getJobs(cluster, namespace, null, null, ContextHolder.exeContext());

                Map<String, List<K8sJobVO>> jobMap = new HashMap<>();
                // 기본 라벨로 조회되는 job은 jobs에 셋팅
                this.getJobToMap(allJobs, jobs, jobMap, MapUtils.getObject(eventMap, K8sApiKindType.JOB.getValue(), Maps.newHashMap()));

                // CronJob이 owner인 job을 jobs에 셋팅
                cronJob.setEvents(MapUtils.getObject(eventMap, K8sApiKindType.CRON_JOB.getValue(), Maps.newHashMap()).get(cronJob.getName()));
                addonCommonService.addJob(jobs, jobMap, cronJob.getName());

                // Set pod
                for (K8sJobVO k8sJobRow : jobs) {
                    if (MapUtils.isEmpty(podLabels)) {
                        podLabels = k8sJobRow.getDetail().getPodTemplate().getLabels();
                    }
                    addonCommonService.addPod(pods, podMap, k8sJobRow.getName());
                }

                workload.getController().setCronJob(cronJob);
                workload.getController().setJobs(jobs);
                workload.getController().setWorkloadType(WorkloadType.CRON_JOB_SERVER.getCode());
            }

            /** Job **/
            K8sJobVO job = workloadResourceService.getJob(cluster, namespace, workloadName, ContextHolder.exeContext());
            if (job != null) {
                podLabels = job.getDetail().getPodTemplate().getLabels();
                // Set event
                job.setEvents(MapUtils.getObject(eventMap, K8sApiKindType.JOB.getValue(), Maps.newHashMap()).get(job.getName()));
                // Set pod
                addonCommonService.addPod(pods, podMap, job.getName());

                workload.getController().setJob(job);
                workload.getController().setWorkloadType(WorkloadType.JOB_SERVER.getCode());
            }

            /** Pods **/
            workload.setPods(pods);

            /** Service **/
            List<K8sServiceVO> services = Lists.newArrayList();
            List<K8sServiceVO> allServices = serviceSpecService.getServices(cluster, namespace, null, null, ContextHolder.exeContext());
            if (CollectionUtils.isNotEmpty(allServices) && MapUtils.isNotEmpty(podLabels)) {
                for (K8sServiceVO service : allServices) {
                    if (MapUtils.isNotEmpty(service.getDetail().getLabelSelector())) {
                        if (ServerUtils.containMaps(podLabels, service.getDetail().getLabelSelector())) {
                            services.add(service);
                        }
                    }
                }

                workload.setServices(services);
            }

            if (CollectionUtils.isNotEmpty(workload.getPods())) {
                /** volumes **/
                List<K8sPersistentVolumeClaimVO> persistentVolumeClaims = Lists.newArrayList();
                // PVC of namespace 조회
                Map<String, K8sPersistentVolumeClaimVO> persistentVolumeClaimMap = persistentVolumeService.convertPersistentVolumeClaimDataMap(cluster, namespace, null, null, true);
                // spec.volumes
                List<V1Volume> volumes = Lists.newArrayList();

                if (MapUtils.isNotEmpty(persistentVolumeClaimMap)) {
                    // PV
                    Map<String, K8sPersistentVolumeVO> persistentVolumesMap = persistentVolumeService.getPersistentVolumesMap(cluster, namespace, null, null, ContextHolder.exeContext());
                    // StorageClass
                    Map<String, K8sStorageClassVO> storageClassMap = storageClassService.convertStorageClassDataMap(cluster, null, null, ContextHolder.exeContext());
                    // pvc name set - StatefulSet pvc 셋팅용
                    Set<String> pvcNameSet = new HashSet<>();

                    for (Map.Entry<String, K8sPersistentVolumeClaimVO> claimEntry : persistentVolumeClaimMap.entrySet()) {
                        // Set PersistentVolume
                        claimEntry.getValue().setPersistentVolume(persistentVolumesMap.get(claimEntry.getValue().getVolumeName()));
                        // Set StorageClass
                        claimEntry.getValue().setStorageClass(storageClassMap.get(claimEntry.getValue().getStorageClassName()));
                        // Set Pvc name
                        pvcNameSet.add(claimEntry.getKey());
                    }

                    if (workload.getController().getDeployment() != null) {
                        volumes = ServerUtils.getObjectsInWorkload(workload.getController().getDeployment().getDeployment(), "$.spec.template.spec.volumes", new TypeRef<List<V1Volume>>() {}, true);
                    } else if (workload.getController().getStatefulSet() != null) {
                        // VolumeClaimTemplate 으로 생성된 pvc 셋팅
                        if (CollectionUtils.isNotEmpty(workload.getController().getStatefulSet().getDetail().getVolumeClaimTemplates())) {
                            for (K8sPersistentVolumeClaimVO vctRow : workload.getController().getStatefulSet().getDetail().getVolumeClaimTemplates()) {
                                String pvcName = String.format("%s-%s-", vctRow.getName(), workload.getController().getStatefulSet().getName());
                                if (pvcNameSet.contains(String.format("%s%d", pvcName, 0))) {
                                    for (int idx = 0, idxe = workload.getController().getStatefulSet().getDesiredPodCnt(); idx < idxe; idx++) {
                                        // [VolumeClaimTemplate명]-[statefulSet명]-[idx] 규칙의 pvc 명과 비교
                                        if (MapUtils.getObject(persistentVolumeClaimMap, String.format("%s%d", pvcName, idx), null) != null) {
                                            // pvc 의 app 라벨이 statefulSet 명과 같다면 셋팅
//                                            if (StringUtils.equals(persistentVolumeClaimMap.get(String.format("%s%d", pvcName, idx)).getDetail().getLabels().get(KubeConstants.LABELS_KEY), workload.getController().getStatefulSet().getName())) {
//                                                persistentVolumeClaims.add(persistentVolumeClaimMap.get(String.format("%s%d", pvcName, idx)));
//                                            }
                                            // 그냥 비교하지 말고 셋팅하도록 수정 ( pvc 의 app 라벨이 statefulSet 명이 상이할 수 있음 )
                                            persistentVolumeClaims.add(persistentVolumeClaimMap.get(String.format("%s%d", pvcName, idx)));
                                        }
                                    }
                                }
                            }
                        }

                        volumes = ServerUtils.getObjectsInWorkload(workload.getController().getStatefulSet().getDeployment(), "$.spec.template.spec.volumes", new TypeRef<List<V1Volume>>() {}, true);
                    } else if (workload.getController().getDaemonSet() != null) {
                        volumes = ServerUtils.getObjectsInWorkload(workload.getController().getDaemonSet().getDeployment(), "$.spec.template.spec.volumes", new TypeRef<List<V1Volume>>() {}, true);
                    } else if (workload.getController().getJob() != null) {
                        volumes = ServerUtils.getObjectsInWorkload(workload.getController().getJob().getDeployment(), "$.spec.template.spec.volumes", new TypeRef<List<V1Volume>>() {}, true);
                    } else if (workload.getController().getCronJob() != null) {
                        volumes = ServerUtils.getObjectsInWorkload(workload.getController().getCronJob().getDeployment(), "$.spec.jobTemplate.spec.template.spec.volumes", new TypeRef<List<V1Volume>>() {}, true);
                    }

                    persistentVolumeService.setPersistentVolumeInWorkload(persistentVolumeClaims, volumes, persistentVolumeClaimMap);
                }

                if (CollectionUtils.isNotEmpty(persistentVolumeClaims)) {
                    workload.setVolumes(persistentVolumeClaims);
                }

                /** volumeMounts **/
                List<ContainerVolumeVO> volumeMounts = Lists.newArrayList();
                Set<String> VolumeClaimTemplateNameSet = new HashSet<>();
                if (workload.getController().getStatefulSet() != null) {
                    if (CollectionUtils.isNotEmpty(workload.getController().getStatefulSet().getDetail().getVolumeClaimTemplates())) {
                        VolumeClaimTemplateNameSet = workload.getController().getStatefulSet().getDetail().getVolumeClaimTemplates().stream().map(vct -> (vct.getName())).collect(Collectors.toSet());
                    }
                }
                // Secret
                List<SecretGuiVO> secrets = Optional.ofNullable(secretService.getSecrets(cluster, namespace, null, cocktailLabel, true)).orElseGet(() ->Lists.newArrayList());
                Map<String, SecretGuiVO> secretMap = Optional.ofNullable(secrets.stream().collect(Collectors.toMap(SecretGuiVO::getName, Function.identity()))).orElseGet(() ->Maps.newHashMap());

                POD_LOOP :
                for (int i = 0, ie = workload.getPods().size(); i < ie; i++) {
                    List<V1Volume> podVolumes = ServerUtils.getObjectsInWorkload(workload.getPods().get(i).getPodDeployment(), "$.spec.volumes", new TypeRef<List<V1Volume>>() {},  true);
                    if (CollectionUtils.isNotEmpty(podVolumes)) {
                        Map<String, V1Volume> v1VolumeMap = podVolumes.stream().collect(Collectors.toMap(V1Volume::getName, Function.identity()));

                        List<V1Container> allContainers = Lists.newArrayList();
                        List<V1Container> initContainers = ServerUtils.getObjectsInWorkload(workload.getPods().get(i).getPodDeployment(), "$.spec.initContainers", new TypeRef<List<V1Container>>() {}, true);
                        List<V1Container> containers = ServerUtils.getObjectsInWorkload(workload.getPods().get(i).getPodDeployment(), "$.spec.containers", new TypeRef<List<V1Container>>() {}, true);

                        if (CollectionUtils.isNotEmpty(initContainers)) {
                            allContainers.addAll(initContainers);
                        }
                        if (CollectionUtils.isNotEmpty(containers)) {
                            allContainers.addAll(containers);
                        }

                        if (CollectionUtils.isNotEmpty(allContainers)) {
                            for (V1Container container : allContainers) {
                                if (CollectionUtils.isNotEmpty(container.getVolumeMounts())) {

                                    VOLUME_MOUNT_LOOP :
                                    for (V1VolumeMount volumeMount : container.getVolumeMounts()) {
                                        if (v1VolumeMap.containsKey(volumeMount.getName())) {

                                            ContainerVolumeVO containerVolume = new ContainerVolumeVO();

                                            if (v1VolumeMap.get(volumeMount.getName()).getPersistentVolumeClaim() != null) {
                                                if (i > 0) {
                                                    // VolumeClaimTemp로 mount한 것이 아니라면 skip
                                                    if (!VolumeClaimTemplateNameSet.contains(volumeMount.getName())) {
                                                        continue VOLUME_MOUNT_LOOP;
                                                    }
                                                }

                                                containerVolume.setPodName(workload.getPods().get(i).getPodName());
                                                containerVolume.setContainerName(container.getName());
                                                containerVolume.setVolumeName(volumeMount.getName());
                                                containerVolume.setVolumeType(VolumeType.PERSISTENT_VOLUME_LINKED);
                                                containerVolume.setPersistentVolumeClaimName(v1VolumeMap.get(volumeMount.getName()).getPersistentVolumeClaim().getClaimName());
                                                containerVolume.setContainerPath(volumeMount.getMountPath());
                                                containerVolume.setSubPath(volumeMount.getSubPath());
                                                containerVolume.setSubPathExpr(volumeMount.getSubPathExpr());
                                                containerVolume.setReadOnly(volumeMount.getReadOnly());

                                                volumeMounts.add(containerVolume);

                                            } else if (
                                                i == 0
                                                || v1VolumeMap.get(volumeMount.getName()).getEmptyDir() != null
                                                || v1VolumeMap.get(volumeMount.getName()).getConfigMap() != null
                                                || v1VolumeMap.get(volumeMount.getName()).getSecret() != null
                                                || v1VolumeMap.get(volumeMount.getName()).getHostPath() != null
                                            ) {

                                                if (v1VolumeMap.get(volumeMount.getName()).getEmptyDir() != null) {
                                                    containerVolume.setVolumeType(VolumeType.EMPTY_DIR);
                                                } else if (v1VolumeMap.get(volumeMount.getName()).getConfigMap() != null) {
                                                    if (MapUtils.getObject(v1VolumeMap, volumeMount.getName(), null) != null) {
                                                        containerVolume.setVolumeType(VolumeType.CONFIG_MAP);
                                                        containerVolume.setConfigMapName(v1VolumeMap.get(volumeMount.getName()).getConfigMap().getName());
                                                    } else {
                                                        continue VOLUME_MOUNT_LOOP;
                                                    }
                                                } else if (v1VolumeMap.get(volumeMount.getName()).getSecret() != null) {
                                                    if (MapUtils.getObject(v1VolumeMap, volumeMount.getName(), null) != null) {
                                                        if (MapUtils.getObject(secretMap, v1VolumeMap.get(volumeMount.getName()).getSecret().getSecretName(), null) != null) {
                                                            containerVolume.setVolumeType(VolumeType.SECRET);
                                                            containerVolume.setSecretName(v1VolumeMap.get(volumeMount.getName()).getSecret().getSecretName());
                                                        } else {
                                                            continue VOLUME_MOUNT_LOOP;
                                                        }
                                                    } else {
                                                        continue VOLUME_MOUNT_LOOP;
                                                    }
                                                } else if (v1VolumeMap.get(volumeMount.getName()).getHostPath() != null) {
                                                    containerVolume.setVolumeType(VolumeType.HOST_PATH);
                                                }
                                                containerVolume.setPodName(workload.getPods().get(i).getPodName());
                                                containerVolume.setContainerName(container.getName());
                                                containerVolume.setVolumeName(volumeMount.getName());
                                                containerVolume.setContainerPath(volumeMount.getMountPath());
                                                containerVolume.setSubPath(volumeMount.getSubPath());
                                                containerVolume.setSubPathExpr(volumeMount.getSubPathExpr());
                                                containerVolume.setReadOnly(volumeMount.getReadOnly());

                                                volumeMounts.add(containerVolume);

                                            }
                                        }
                                    }

                                }
                            }
                        }
                    }

                    // volumeClaimeTemp로 생성한 volume에 대한 것을 위해 POD_LOOP하는 것이므로 아니면 break
                    if (workload.getController().getStatefulSet() == null) {
                        break POD_LOOP;
                    }
                }

                workload.setVolumeMounts(volumeMounts);

            }

        }

        return workload;
    }

    protected Map<String, List<K8sJobVO>> getJobToMap(List<K8sJobVO> allJobs, List<K8sJobVO> jobs, Map<String, List<K8sJobVO>> jobMap, Map<String, List<K8sEventVO>> eventMap){
        if(CollectionUtils.isNotEmpty(allJobs)){
            String ownerName = "";
            for(K8sJobVO jobRow : allJobs){
                ownerName = "";
                // Set event
                jobRow.setEvents(eventMap.get(jobRow.getName()));
                if (CollectionUtils.isNotEmpty(jobRow.getDetail().getOwnerReferences())) {
                    for (K8sOwnerReferenceVO ownerReferenceRow : jobRow.getDetail().getOwnerReferences()) {
                        ownerName = ownerReferenceRow.getName();
                        break;
                    }
                }
                if(StringUtils.isNotBlank(ownerName) && !jobMap.containsKey(ownerName)){
                    jobMap.put(ownerName, new ArrayList<>());
                }

                if(jobMap.get(ownerName) != null){
                    jobMap.get(ownerName).add(jobRow);
                }
            }
        }

        return jobMap;
    }

    /**
     * 워크로드 이동 (그룹간)
     * TODO : 이후 워크로드 정렬 처리 추가 예정..
     * @param clusterSeq
     * @param namespaceName
     * @param workloadName
     * @throws Exception
     */
    public void moveWorkload(Integer clusterSeq, String namespaceName, String workloadName, ComponentOrderVO componentOrder) throws Exception {
        IClusterMapper clusterDao = this.sqlSession.getMapper(IClusterMapper.class);

        ComponentVO component = this.getComponent(clusterSeq, namespaceName, workloadName);
        ClusterVO cluster = clusterDao.getClusterByNamespace(clusterSeq, namespaceName);

        WorkloadType workloadType = WorkloadType.valueOf(component.getWorkloadType());

//        if(StateCode.STOPPED == StateCode.valueOf(component.getStateCode())) { // 상태와 관계 없이 WorkloadManifest가 존재하면 그룹 정보를 업데이트 하도록 변경..
            if(StringUtils.isNotEmpty(component.getWorkloadManifest())) {
                try {
                    io.kubernetes.client.openapi.JSON k8sJson = new io.kubernetes.client.openapi.JSON();
                    List<Object> objs = ServerUtils.getYamlObjects(component.getWorkloadManifest());

                    if (CollectionUtils.isNotEmpty(objs)) {
                        for (Object obj : objs) {
                            Map<String, Object> k8sObjectToMap = ServerUtils.getK8sObjectToMap(obj, k8sJson);
                            K8sApiKindType kind = ServerUtils.getK8sKindInMap(k8sObjectToMap);
                            K8sApiGroupType apiGroupType = ServerUtils.getK8sGroupInMap(k8sObjectToMap);
                            K8sApiType apiType = ServerUtils.getK8sVersionInMap(k8sObjectToMap);

                            switch (kind) {
                                case DEPLOYMENT:
                                    if (apiGroupType == K8sApiGroupType.APPS && apiType == K8sApiType.V1) {
                                        V1Deployment deployment = ServerUtils.unmarshalYaml(component.getWorkloadManifest(), K8sApiKindType.DEPLOYMENT);
                                        Map<String, String> annotations = Optional.ofNullable(deployment.getMetadata()).map(V1ObjectMeta::getAnnotations).orElseGet(() ->Maps.newHashMap());
                                        if(componentOrder.getWorkloadGroupSeq() == null || componentOrder.getWorkloadGroupSeq() < 1) {
                                            annotations.remove(KubeConstants.ANNOTATION_COCKTAIL_GROUP_NO);
                                        }
                                        else {
                                            annotations.put(KubeConstants.ANNOTATION_COCKTAIL_GROUP_NO, componentOrder.getWorkloadGroupSeq().toString());
                                        }
                                        deployment.getMetadata().setAnnotations(annotations);
                                        this.updateComponentManifestAndGroupByNamespace(clusterSeq, namespaceName, componentOrder.getWorkloadGroupSeq(), workloadName, ServerUtils.marshalYaml(deployment), ContextHolder.exeContext().getUserSeq());
                                    }
                                break;
                                case DAEMON_SET:
                                    if (apiGroupType == K8sApiGroupType.APPS && apiType == K8sApiType.V1) {
                                        V1DaemonSet daemonSet = ServerUtils.unmarshalYaml(component.getWorkloadManifest(), K8sApiKindType.DAEMON_SET);
                                        Map<String, String> annotations = Optional.ofNullable(daemonSet.getMetadata()).map(V1ObjectMeta::getAnnotations).orElseGet(() ->Maps.newHashMap());
                                        if(componentOrder.getWorkloadGroupSeq() == null || componentOrder.getWorkloadGroupSeq() < 1) {
                                            annotations.remove(KubeConstants.ANNOTATION_COCKTAIL_GROUP_NO);
                                        }
                                        else {
                                            annotations.put(KubeConstants.ANNOTATION_COCKTAIL_GROUP_NO, componentOrder.getWorkloadGroupSeq().toString());
                                        }
                                        daemonSet.getMetadata().setAnnotations(annotations);
                                        this.updateComponentManifestAndGroupByNamespace(clusterSeq, namespaceName, componentOrder.getWorkloadGroupSeq(), workloadName, ServerUtils.marshalYaml(daemonSet), ContextHolder.exeContext().getUserSeq());
                                    }
                                break;
                                case JOB:
                                    if (apiGroupType == K8sApiGroupType.BATCH && apiType == K8sApiType.V1) {
                                        V1Job job = ServerUtils.unmarshalYaml(component.getWorkloadManifest(), K8sApiKindType.JOB);
                                        Map<String, String> annotations = Optional.ofNullable(job.getMetadata()).map(V1ObjectMeta::getAnnotations).orElseGet(() ->Maps.newHashMap());
                                        if(componentOrder.getWorkloadGroupSeq() == null || componentOrder.getWorkloadGroupSeq() < 1) {
                                            annotations.remove(KubeConstants.ANNOTATION_COCKTAIL_GROUP_NO);
                                        }
                                        else {
                                            annotations.put(KubeConstants.ANNOTATION_COCKTAIL_GROUP_NO, componentOrder.getWorkloadGroupSeq().toString());
                                        }
                                        job.getMetadata().setAnnotations(annotations);
                                        this.updateComponentManifestAndGroupByNamespace(clusterSeq, namespaceName, componentOrder.getWorkloadGroupSeq(), workloadName, ServerUtils.marshalYaml(job), ContextHolder.exeContext().getUserSeq());
                                    }
                                break;
                                case CRON_JOB:
                                    if (apiGroupType == K8sApiGroupType.BATCH && apiType == K8sApiType.V1BETA1) {
                                        V1beta1CronJob cronJob = ServerUtils.unmarshalYaml(component.getWorkloadManifest(), K8sApiKindType.CRON_JOB);
                                        Map<String, String> annotations = Optional.ofNullable(cronJob.getMetadata()).map(V1ObjectMeta::getAnnotations).orElseGet(() ->Maps.newHashMap());
                                        if(componentOrder.getWorkloadGroupSeq() == null || componentOrder.getWorkloadGroupSeq() < 1) {
                                            annotations.remove(KubeConstants.ANNOTATION_COCKTAIL_GROUP_NO);
                                        }
                                        else {
                                            annotations.put(KubeConstants.ANNOTATION_COCKTAIL_GROUP_NO, componentOrder.getWorkloadGroupSeq().toString());
                                        }
                                        cronJob.getMetadata().setAnnotations(annotations);
                                        this.updateComponentManifestAndGroupByNamespace(clusterSeq, namespaceName, componentOrder.getWorkloadGroupSeq(), workloadName, ServerUtils.marshalYaml(cronJob), ContextHolder.exeContext().getUserSeq());
                                    } else if (apiGroupType == K8sApiGroupType.BATCH && apiType == K8sApiType.V1) {
                                        V1CronJob cronJob = ServerUtils.unmarshalYaml(component.getWorkloadManifest(), K8sApiKindType.CRON_JOB);
                                        Map<String, String> annotations = Optional.ofNullable(cronJob.getMetadata()).map(V1ObjectMeta::getAnnotations).orElseGet(() ->Maps.newHashMap());
                                        if(componentOrder.getWorkloadGroupSeq() == null || componentOrder.getWorkloadGroupSeq() < 1) {
                                            annotations.remove(KubeConstants.ANNOTATION_COCKTAIL_GROUP_NO);
                                        }
                                        else {
                                            annotations.put(KubeConstants.ANNOTATION_COCKTAIL_GROUP_NO, componentOrder.getWorkloadGroupSeq().toString());
                                        }
                                        cronJob.getMetadata().setAnnotations(annotations);
                                        this.updateComponentManifestAndGroupByNamespace(clusterSeq, namespaceName, componentOrder.getWorkloadGroupSeq(), workloadName, ServerUtils.marshalYaml(cronJob), ContextHolder.exeContext().getUserSeq());
                                    }
                                break;
                                case STATEFUL_SET:
                                    if (apiGroupType == K8sApiGroupType.APPS && apiType == K8sApiType.V1) {
                                        V1StatefulSet statefulSet = ServerUtils.unmarshalYaml(component.getWorkloadManifest(), K8sApiKindType.STATEFUL_SET);
                                        Map<String, String> annotations = Optional.ofNullable(statefulSet.getMetadata()).map(V1ObjectMeta::getAnnotations).orElseGet(() ->Maps.newHashMap());
                                        if(componentOrder.getWorkloadGroupSeq() == null || componentOrder.getWorkloadGroupSeq() < 1) {
                                            annotations.remove(KubeConstants.ANNOTATION_COCKTAIL_GROUP_NO);
                                        }
                                        else {
                                            annotations.put(KubeConstants.ANNOTATION_COCKTAIL_GROUP_NO, componentOrder.getWorkloadGroupSeq().toString());
                                        }
                                        statefulSet.getMetadata().setAnnotations(annotations);
                                        this.updateComponentManifestAndGroupByNamespace(clusterSeq, namespaceName, componentOrder.getWorkloadGroupSeq(), workloadName, ServerUtils.marshalYaml(statefulSet), ContextHolder.exeContext().getUserSeq());
                                    }
                                break;
                            }
                        }
                    }
                }
                catch (Exception ex) {
                    if(log.isDebugEnabled()) log.debug("trace log ", ex);
                    throw new CocktailException("Workload Move Failure.", ex, ExceptionType.ServerInvalidState);
                }
            }
            else {
                throw new CocktailException("Can not move workload. because Workload Manifest is null", ExceptionType.ServerInvalidState);
            }
//        }
//        else {
            switch (workloadType) {
                case SINGLE_SERVER:
                case REPLICA_SERVER: {
                    K8sApiVerKindType deploymentType = ResourceUtil.getApiVerKindType(cluster, K8sApiKindType.DEPLOYMENT);
                    if (deploymentType != null) {
                        if (deploymentType.getGroupType() == K8sApiGroupType.APPS && deploymentType.getApiType() == K8sApiType.V1) {
                            V1Deployment currentDeployment = this.getV1Deployment(component.getComponentName(), cluster);
                            if (currentDeployment == null) {
                                log.warn("Workload Move : Can not found workload.");
                                break;
                            }
                            currentDeployment.setStatus(null);
                            currentDeployment.getMetadata().setCreationTimestamp(null);
                            V1Deployment updatedDeployment = k8sPatchSpecFactory.copyObject(currentDeployment, new TypeReference<V1Deployment>() {});

                            Map<String, String> annotations = Optional.ofNullable(updatedDeployment.getMetadata().getAnnotations()).orElseGet(() ->Maps.newHashMap());
                            if (componentOrder.getWorkloadGroupSeq() == null || componentOrder.getWorkloadGroupSeq() < 1) {
                                annotations.remove(KubeConstants.ANNOTATION_COCKTAIL_GROUP_NO);
                            }
                            else {
                                annotations.put(KubeConstants.ANNOTATION_COCKTAIL_GROUP_NO, componentOrder.getWorkloadGroupSeq().toString());
                            }
                            updatedDeployment.getMetadata().setAnnotations(annotations);
                            // patchJson 으로 변경
                            List<JsonObject> patchBody = k8sPatchSpecFactory.buildPatch(currentDeployment, updatedDeployment);
                            log.debug("########## Deployment patchBody JSON: {}", JsonUtils.toGson(patchBody));
                            k8sWorker.patchDeploymentV1(cluster, cluster.getNamespaceName(), component.getComponentName(), patchBody, false);
                        }
                    }
                    break;
                }
                case STATEFUL_SET_SERVER: {
                    K8sApiVerKindType statefulSetType = ResourceUtil.getApiVerKindType(cluster, K8sApiKindType.STATEFUL_SET);
                    if (statefulSetType != null) {
                        if (statefulSetType.getGroupType() == K8sApiGroupType.APPS && statefulSetType.getApiType() == K8sApiType.V1) {
                            V1StatefulSet currentStatefulSet = this.getV1StatefulSet(component.getComponentName(), cluster);
                            if (currentStatefulSet == null) {
                                log.warn("Workload Move : Can not found workload.");
                                break;
                            }
                            currentStatefulSet.setStatus(null);
                            currentStatefulSet.getMetadata().setCreationTimestamp(null);
                            V1StatefulSet updatedStatefulSet = k8sPatchSpecFactory.copyObject(currentStatefulSet, new TypeReference<V1StatefulSet>() {});

                            Map<String, String> annotations = Optional.ofNullable(updatedStatefulSet.getMetadata().getAnnotations()).orElseGet(() ->Maps.newHashMap());
                            if (componentOrder.getWorkloadGroupSeq() == null || componentOrder.getWorkloadGroupSeq() < 1) {
                                annotations.remove(KubeConstants.ANNOTATION_COCKTAIL_GROUP_NO);
                            }
                            else {
                                annotations.put(KubeConstants.ANNOTATION_COCKTAIL_GROUP_NO, componentOrder.getWorkloadGroupSeq().toString());
                            }
                            updatedStatefulSet.getMetadata().setAnnotations(annotations);
                            // patchJson 으로 변경
                            List<JsonObject> patchBody = k8sPatchSpecFactory.buildPatch(currentStatefulSet, updatedStatefulSet);
                            log.debug("########## StatefulSet patchBody JSON: {}", JsonUtils.toGson(patchBody));
                            k8sWorker.patchStatefulSetV1(cluster, cluster.getNamespaceName(), component.getComponentName(), patchBody, false);
                        }
                    }
                    break;
                }
                case DAEMON_SET_SERVER: {
                    K8sApiVerKindType daemonSetType = ResourceUtil.getApiVerKindType(cluster, K8sApiKindType.DAEMON_SET);
                    if (daemonSetType != null) {
                        if (daemonSetType.getGroupType() == K8sApiGroupType.APPS && daemonSetType.getApiType() == K8sApiType.V1) {
                            V1DaemonSet currentDaemonSet = this.getV1DaemonSet(component.getComponentName(), cluster);
                            if (currentDaemonSet == null) {
                                log.warn("Workload Move : Can not found workload.");
                                break;
                            }
                            currentDaemonSet.setStatus(null);
                            currentDaemonSet.getMetadata().setCreationTimestamp(null);
                            V1DaemonSet updatedDaemonSet = k8sPatchSpecFactory.copyObject(currentDaemonSet, new TypeReference<V1DaemonSet>() {});

                            Map<String, String> annotations = Optional.ofNullable(updatedDaemonSet.getMetadata().getAnnotations()).orElseGet(() ->Maps.newHashMap());
                            if (componentOrder.getWorkloadGroupSeq() == null || componentOrder.getWorkloadGroupSeq() < 1) {
                                annotations.remove(KubeConstants.ANNOTATION_COCKTAIL_GROUP_NO);
                            }
                            else {
                                annotations.put(KubeConstants.ANNOTATION_COCKTAIL_GROUP_NO, componentOrder.getWorkloadGroupSeq().toString());
                            }
                            updatedDaemonSet.getMetadata().setAnnotations(annotations);
                            // patchJson 으로 변경
                            List<JsonObject> patchBody = k8sPatchSpecFactory.buildPatch(currentDaemonSet, updatedDaemonSet);
                            log.debug("########## DaemonSet patchBody JSON: {}", JsonUtils.toGson(patchBody));
                            k8sWorker.patchDaemonSetV1(cluster, cluster.getNamespaceName(), component.getComponentName(), patchBody, false);
                        }
                    }
                    break;
                }
                case JOB_SERVER: {
                    K8sApiVerKindType jobType = ResourceUtil.getApiVerKindType(cluster, K8sApiKindType.JOB);
                    if (jobType != null) {
                        if (jobType.getGroupType() == K8sApiGroupType.BATCH && jobType.getApiType() == K8sApiType.V1) {
                            V1Job currentJob = this.getV1Job(component.getComponentName(), cluster);
                            if (currentJob == null) {
                                log.warn("Workload Move : Can not found workload.");
                                break;
                            }
                            currentJob.setStatus(null);
                            currentJob.getMetadata().setCreationTimestamp(null);
                            V1Job updatedJob = k8sPatchSpecFactory.copyObject(currentJob, new TypeReference<V1Job>() {});

                            Map<String, String> annotations = Optional.ofNullable(updatedJob.getMetadata().getAnnotations()).orElseGet(() ->Maps.newHashMap());
                            if (componentOrder.getWorkloadGroupSeq() == null || componentOrder.getWorkloadGroupSeq() < 1) {
                                annotations.remove(KubeConstants.ANNOTATION_COCKTAIL_GROUP_NO);
                            }
                            else {
                                annotations.put(KubeConstants.ANNOTATION_COCKTAIL_GROUP_NO, componentOrder.getWorkloadGroupSeq().toString());
                            }
                            updatedJob.getMetadata().setAnnotations(annotations);
                            // patchJson 으로 변경
                            List<JsonObject> patchBody = k8sPatchSpecFactory.buildPatch(currentJob, updatedJob);
                            log.debug("########## Job patchBody JSON: {}", JsonUtils.toGson(patchBody));
                            k8sWorker.patchJobV1(cluster, cluster.getNamespaceName(), component.getComponentName(), patchBody, false);
                        }
                    }
                    break;
                }
                case CRON_JOB_SERVER: {
                    K8sApiVerKindType cronJobType = ResourceUtil.getApiVerKindType(cluster, K8sApiKindType.CRON_JOB);
                    if (cronJobType != null) {
                        if (cronJobType.getGroupType() == K8sApiGroupType.BATCH && cronJobType.getApiType() == K8sApiType.V1BETA1) {
                            V1beta1CronJob currentCronJob = this.getV1beta1CronJob(component.getComponentName(), cluster);
                            if (currentCronJob == null) {
                                log.warn("Workload Move : Can not found workload.");
                                break;
                            }
                            currentCronJob.setStatus(null);
                            currentCronJob.getMetadata().setCreationTimestamp(null);
                            V1beta1CronJob updatedCronJob = k8sPatchSpecFactory.copyObject(currentCronJob, new TypeReference<V1beta1CronJob>() {});

                            Map<String, String> annotations = Optional.ofNullable(updatedCronJob.getMetadata().getAnnotations()).orElseGet(() ->Maps.newHashMap());
                            if (componentOrder.getWorkloadGroupSeq() == null || componentOrder.getWorkloadGroupSeq() < 1) {
                                annotations.remove(KubeConstants.ANNOTATION_COCKTAIL_GROUP_NO);
                            }
                            else {
                                annotations.put(KubeConstants.ANNOTATION_COCKTAIL_GROUP_NO, componentOrder.getWorkloadGroupSeq().toString());
                            }
                            updatedCronJob.getMetadata().setAnnotations(annotations);
                            // patchJson 으로 변경
                            List<JsonObject> patchBody = k8sPatchSpecFactory.buildPatch(currentCronJob, updatedCronJob);
                            log.debug("########## CronJob patchBody JSON: {}", JsonUtils.toGson(patchBody));
                            k8sWorker.patchCronJobV1beta1(cluster, cluster.getNamespaceName(), component.getComponentName(), patchBody, false);
                        } else if (cronJobType.getGroupType() == K8sApiGroupType.BATCH && cronJobType.getApiType() == K8sApiType.V1) {
                            V1CronJob currentCronJob = this.getV1CronJob(component.getComponentName(), cluster);
                            if (currentCronJob == null) {
                                log.warn("Workload Move : Can not found workload.");
                                break;
                            }
                            currentCronJob.setStatus(null);
                            currentCronJob.getMetadata().setCreationTimestamp(null);
                            V1CronJob updatedCronJob = k8sPatchSpecFactory.copyObject(currentCronJob, new TypeReference<V1CronJob>() {});

                            Map<String, String> annotations = Optional.ofNullable(updatedCronJob.getMetadata().getAnnotations()).orElseGet(() ->Maps.newHashMap());
                            if (componentOrder.getWorkloadGroupSeq() == null || componentOrder.getWorkloadGroupSeq() < 1) {
                                annotations.remove(KubeConstants.ANNOTATION_COCKTAIL_GROUP_NO);
                            }
                            else {
                                annotations.put(KubeConstants.ANNOTATION_COCKTAIL_GROUP_NO, componentOrder.getWorkloadGroupSeq().toString());
                            }
                            updatedCronJob.getMetadata().setAnnotations(annotations);
                            // patchJson 으로 변경
                            List<JsonObject> patchBody = k8sPatchSpecFactory.buildPatch(currentCronJob, updatedCronJob);
                            log.debug("########## CronJob patchBody JSON: {}", JsonUtils.toGson(patchBody));
                            k8sWorker.patchCronJobV1(cluster, cluster.getNamespaceName(), component.getComponentName(), patchBody, false);
                        }
                    }
                    break;
                }
                default: {
                    throw new CocktailException("Can not move workload. workload type mismatch", ExceptionType.ServerNotFound);
                }
            } // switch
//        }
    }

    /**
     * Component의 Manifest내용을 update
     * @param clusterSeq
     * @param namespaceName
     * @param componentName
     * @param manifest
     * @param updater
     * @return
     * @throws Exception
     */
    public int updateComponentManifestAndGroupByNamespace(Integer clusterSeq, String namespaceName, Integer groupSeq, String componentName, String manifest, Integer updater) throws Exception {
        IComponentMapper compDao = sqlSession.getMapper(IComponentMapper.class);
        return compDao.updateComponentManifestAndGroupByNamespace(clusterSeq, namespaceName, groupSeq, componentName, manifest, updater);
    }

    public List<WorkloadPodTemplateVO> getWorkloadPodTemplates(Integer clusterSeq, String namespaceName) throws Exception {
        IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
        ClusterVO cluster =  clusterDao.getCluster(clusterSeq);

        return this.getWorkloadPodTemplates(cluster, namespaceName);
    }

    /**
     * Workload의 podTemplate 목록 조회
     *
     * @param cluster
     * @param namespaceName
     * @return
     * @throws Exception
     */
    public List<WorkloadPodTemplateVO> getWorkloadPodTemplates(ClusterVO cluster, String namespaceName) throws Exception {
        Map<String, Map<String, ?>> k8sResourceMap = workloadResourceService.getWorkloadResource(cluster, namespaceName,
                null, null,
                Boolean.TRUE, Boolean.TRUE, Boolean.TRUE, Boolean.TRUE, Boolean.TRUE, Boolean.FALSE, Boolean.FALSE);

        List<WorkloadPodTemplateVO> workloadPodTemplates = Lists.newArrayList();
        JSON k8sJson = new JSON();

        if (MapUtils.isNotEmpty(k8sResourceMap)) {
            for (Map.Entry<String, Map<String, ?>> k8sResourseEntry : k8sResourceMap.entrySet()) {

                if (MapUtils.isNotEmpty(k8sResourseEntry.getValue())) {
                    for (Map.Entry<String, ?> workloadEntry : k8sResourseEntry.getValue().entrySet()) {

                        WorkloadPodTemplateVO workloadPodTemplate = new WorkloadPodTemplateVO();
                        workloadPodTemplate.setWorkloadName(workloadEntry.getKey());

                        switch (K8sApiKindType.findKindTypeByValue(k8sResourseEntry.getKey())) {
                            case DEPLOYMENT:
                                workloadPodTemplate.setWorkloadType(WorkloadType.REPLICA_SERVER.getCode());
                                K8sApiVerKindType deploymentType = ResourceUtil.getApiVerKindType(cluster, K8sApiKindType.DEPLOYMENT);
                                if (deploymentType != null) {
                                    if (deploymentType.getGroupType() == K8sApiGroupType.APPS && deploymentType.getApiType() == K8sApiType.V1) {
                                        V1Deployment v1Deployment = (V1Deployment)workloadEntry.getValue();
                                        workloadPodTemplate.setPodTemplate(workloadResourceService.setPodTemplateSpec(v1Deployment.getSpec().getTemplate(), k8sJson));
                                    }
                                }

                                break;
                            case STATEFUL_SET:
                                workloadPodTemplate.setWorkloadType(WorkloadType.STATEFUL_SET_SERVER.getCode());
                                K8sApiVerKindType statefulSetType = ResourceUtil.getApiVerKindType(cluster, K8sApiKindType.STATEFUL_SET);
                                if (statefulSetType != null) {
                                    if (statefulSetType.getGroupType() == K8sApiGroupType.APPS && statefulSetType.getApiType() == K8sApiType.V1) {
                                        V1StatefulSet v1StatefulSet = (V1StatefulSet)workloadEntry.getValue();
                                        workloadPodTemplate.setPodTemplate(workloadResourceService.setPodTemplateSpec(v1StatefulSet.getSpec().getTemplate(), k8sJson));
                                    }
                                }

                                break;
                            case DAEMON_SET:
                                workloadPodTemplate.setWorkloadType(WorkloadType.DAEMON_SET_SERVER.getCode());
                                K8sApiVerKindType daemonSetType = ResourceUtil.getApiVerKindType(cluster, K8sApiKindType.DAEMON_SET);
                                if (daemonSetType != null) {
                                    if (daemonSetType.getGroupType() == K8sApiGroupType.APPS && daemonSetType.getApiType() == K8sApiType.V1) {
                                        V1DaemonSet v1DaemonSet = (V1DaemonSet)workloadEntry.getValue();
                                        workloadPodTemplate.setPodTemplate(workloadResourceService.setPodTemplateSpec(v1DaemonSet.getSpec().getTemplate(), k8sJson));
                                    }
                                }

                                break;
                            case JOB:
                                workloadPodTemplate.setWorkloadType(WorkloadType.JOB_SERVER.getCode());
                                K8sApiVerKindType jobType = ResourceUtil.getApiVerKindType(cluster, K8sApiKindType.JOB);
                                if (jobType != null) {
                                    if (jobType.getGroupType() == K8sApiGroupType.BATCH && jobType.getApiType() == K8sApiType.V1) {
                                        V1Job v1Job = (V1Job)workloadEntry.getValue();
                                        workloadPodTemplate.setPodTemplate(workloadResourceService.setPodTemplateSpec(v1Job.getSpec().getTemplate(), k8sJson));
                                    }
                                }

                                break;
                            case CRON_JOB:
                                workloadPodTemplate.setWorkloadType(WorkloadType.CRON_JOB_SERVER.getCode());
                                K8sApiVerKindType cronJobType = ResourceUtil.getApiVerKindType(cluster, K8sApiKindType.CRON_JOB);
                                if (cronJobType != null) {
                                    if (cronJobType.getGroupType() == K8sApiGroupType.BATCH && cronJobType.getApiType() == K8sApiType.V1BETA1) {
                                        V1beta1CronJob cronJob = (V1beta1CronJob)workloadEntry.getValue();
                                        workloadPodTemplate.setPodTemplate(workloadResourceService.setPodTemplateSpec(cronJob.getSpec().getJobTemplate().getSpec().getTemplate(), k8sJson));
                                    } else if (cronJobType.getGroupType() == K8sApiGroupType.BATCH && cronJobType.getApiType() == K8sApiType.V1) {
                                        V1CronJob cronJob = (V1CronJob)workloadEntry.getValue();
                                        workloadPodTemplate.setPodTemplate(workloadResourceService.setPodTemplateSpec(cronJob.getSpec().getJobTemplate().getSpec().getTemplate(), k8sJson));
                                    }
                                }

                                break;
                        }

                        workloadPodTemplates.add(workloadPodTemplate);
                    }
                }

            }
        }

        return workloadPodTemplates;
    }
}
