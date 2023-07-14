package run.acloud.api.serverless.service;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import run.acloud.api.auth.enums.LanguageType;
import run.acloud.api.auth.enums.UserGrant;
import run.acloud.api.auth.enums.UserRole;
import run.acloud.api.auth.vo.AuthVO;
import run.acloud.api.auth.vo.UserVO;
import run.acloud.api.configuration.enums.ClusterTenancy;
import run.acloud.api.configuration.enums.ServiceRegistryType;
import run.acloud.api.configuration.enums.ServiceType;
import run.acloud.api.configuration.service.AccountGradeService;
import run.acloud.api.configuration.service.AccountService;
import run.acloud.api.configuration.service.ClusterStateService;
import run.acloud.api.configuration.service.UserService;
import run.acloud.api.configuration.vo.*;
import run.acloud.api.cserver.dao.IServiceMapper;
import run.acloud.api.cserver.dao.IServicemapMapper;
import run.acloud.api.cserver.dao.IWorkloadGroupMapper;
import run.acloud.api.cserver.enums.NetworkPolicyCreationType;
import run.acloud.api.cserver.service.ServiceService;
import run.acloud.api.cserver.service.ServiceValidService;
import run.acloud.api.cserver.service.ServicemapService;
import run.acloud.api.cserver.vo.*;
import run.acloud.api.event.service.EventService;
import run.acloud.api.resource.constants.KubeConstants;
import run.acloud.api.resource.enums.SecretType;
import run.acloud.api.resource.service.NamespaceService;
import run.acloud.api.resource.service.NetworkPolicyService;
import run.acloud.api.resource.service.SecretService;
import run.acloud.api.resource.util.ResourceUtil;
import run.acloud.api.resource.vo.*;
import run.acloud.api.serverless.dao.IServerlessMapper;
import run.acloud.api.serverless.enums.ServerlessType;
import run.acloud.api.serverless.vo.*;
import run.acloud.commons.client.GatewayApiClient;
import run.acloud.commons.constants.CommonConstants;
import run.acloud.commons.enums.ApiVersionType;
import run.acloud.commons.enums.HarborRegistryProjectMemberRole;
import run.acloud.commons.service.HarborRegistryFactoryService;
import run.acloud.commons.service.IHarborRegistryService;
import run.acloud.commons.service.RegistryPropertyService;
import run.acloud.commons.util.AuthUtils;
import run.acloud.commons.util.CryptoUtils;
import run.acloud.commons.vo.*;
import run.acloud.framework.context.ContextHolder;
import run.acloud.framework.enums.ExceptionBiz;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;
import run.acloud.framework.util.ExceptionMessageUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author wschoi@acornsoft.io Created on 2017. 1. 10.
 */
@Slf4j
@Service
public class ServerlessService {
    @Resource(name = "cocktailSession")
    private SqlSessionTemplate sqlSession;

    @Autowired
    private UserService userService;

    @Autowired
    private ServicemapService servicemapService;

    @Autowired
    private NamespaceService namespaceService;

    @Autowired
    private HarborRegistryFactoryService harborRegistryFactory;

    @Autowired
    private RegistryPropertyService registry;

    @Autowired
    private SecretService secretService;

    @Autowired
    private EventService eventService;

    @Autowired
    private ClusterStateService clusterStateService;

    @Autowired
    private ServiceService serviceService;

    @Autowired
    private ServiceValidService serviceValidService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountGradeService accountGradeService;

    @Autowired
    private NetworkPolicyService networkPolicyService;

    @Autowired
    private GatewayApiClient gatewayApiClient;

    private enum Step {
        Start,
        DockerUser,
        DockerProject,
        DockerUserToProject,
        DockerUserToAnotherProject,
        Builder
    }

    /**
     * Serverless > project(namespace) 목록 조회
     *
     * @param userId
     * @param clusterId
     * @throws Exception
     */
    public List<ServerlessVO> getProjects(String userId, String clusterId, boolean useNamespace) throws Exception {

        List<ServerlessVO> projects = null;

        ExceptionMessageUtils.checkParameterRequired("userId", userId);

        IServerlessMapper serverlessDao = sqlSession.getMapper(IServerlessMapper.class);

        String workspaceName = userId;

        ServerlessWorkspaceVO serverlessWorkspace = serverlessDao.getWorkspace(null, null, workspaceName, userId);

        if (serverlessWorkspace != null) {

            serverlessWorkspace.setServerlesses(serverlessDao.getServerlesses(serverlessWorkspace.getServiceSeq(), null));

            if (CollectionUtils.isNotEmpty(serverlessWorkspace.getServerlesses())) {

                if (useNamespace) {
                    List<ClusterVO> clusters = Optional.ofNullable(serverlessWorkspace.getClusters()).orElseGet(Lists::newArrayList);

                    Map<Integer, List<ServerlessVO>> serverlessMap = Maps.newHashMap();
                    for (ServerlessVO s : serverlessWorkspace.getServerlesses()) {
                        serverlessMap.putIfAbsent(s.getClusterSeq(), Lists.newArrayList());
                        serverlessMap.get(s.getClusterSeq()).add(s);
                    }

                    projects = Lists.newArrayList();

                    for (ClusterVO cluster : clusters) {
                        if (StringUtils.isNotBlank(clusterId) && !StringUtils.equals(clusterId, cluster.getClusterId())) {
                            continue;
                        }
                        if (serverlessMap.containsKey(cluster.getClusterSeq())) {
                            List<K8sNamespaceVO> k8sNamespaces = namespaceService.getNamespacesToList(cluster, null, null, ContextHolder.exeContext());

                            if (CollectionUtils.isNotEmpty(k8sNamespaces)) {
                                Map<String, K8sNamespaceVO> k8sNamespacesMap = k8sNamespaces.stream().collect(Collectors.toMap(K8sNamespaceVO::getName, Function.identity()));

                                for (ServerlessVO s : serverlessMap.get(cluster.getClusterSeq())) {
                                    if (cluster.getClusterSeq().equals(s.getClusterSeq())) {
                                        if (k8sNamespacesMap.containsKey(s.getNamespaceName())) {
                                            s.setK8sResourceExists(Boolean.TRUE);
                                            s.setNamespaceInfo(k8sNamespacesMap.get(s.getNamespaceName()));
                                            s.setStatus(k8sNamespacesMap.get(s.getNamespaceName()).getStatus());
                                        } else {
                                            s.setK8sResourceExists(Boolean.FALSE);
                                        }
                                    }
                                    projects.add(s);
                                }
                            }
                        }
                    }
                } else {
                    projects = serverlessWorkspace.getServerlesses();
                }
            }
        }

        return projects;
    }

    /**
     * Serverless > project(namespace) 상세 조회
     *
     * @param userId
     * @param clusterId
     * @param projectName
     * @return
     * @throws Exception
     */
    public ServerlessVO getProject(String userId, String clusterId, String projectName, boolean useNamespace) throws Exception {

        ServerlessVO serverless = null;

        ExceptionMessageUtils.checkParameterRequired("userId", userId);
        ExceptionMessageUtils.checkParameterRequired("clusterId", clusterId);
        ExceptionMessageUtils.checkParameterRequired("projectName", projectName);
        projectName = StringUtils.trim(projectName);

        IServerlessMapper serverlessDao = sqlSession.getMapper(IServerlessMapper.class);

        ClusterVO cluster = serverlessDao.getClusterByClusterId(clusterId);

        if (cluster != null && cluster.getAccount() != null) {

            String serviceName = userId;
            ServerlessWorkspaceVO serverlessWorkspace = serverlessDao.getWorkspace(cluster.getAccount().getAccountSeq(), null, serviceName, userId);

            if (serverlessWorkspace != null) {

                serverlessWorkspace.setServerlesses(serverlessDao.getServerlesses(serverlessWorkspace.getServiceSeq(), null));

                if (CollectionUtils.isNotEmpty(serverlessWorkspace.getServerlesses())) {

                    String finalProjectName = projectName;
                    Optional<ServerlessVO> serverlessOptional = serverlessWorkspace.getServerlesses().stream()
                            .filter(s -> (StringUtils.equals(clusterId, s.getClusterId()) && StringUtils.equals(finalProjectName, s.getProjectName())))
                            .findFirst();

                    if (serverlessOptional.isPresent()) {
                        serverless = serverlessOptional.get();

                        if (useNamespace) {
                            K8sNamespaceVO k8sNamespace = namespaceService.getNamespace(cluster, projectName, ContextHolder.exeContext());

                            if (k8sNamespace != null) {
                                serverless.setK8sResourceExists(Boolean.TRUE);
                                serverless.setNamespaceInfo(k8sNamespace);
                                serverless.setStatus(k8sNamespace.getStatus());
                            } else {
                                serverless.setK8sResourceExists(Boolean.FALSE);
                            }
                        }
                    }
                }
            }
        } else {
            throw new CocktailException("Cluster is null", ExceptionType.InvalidParameter_Empty);
        }

        return serverless;
    }

    /**
     * Serverless > project(namespace) 삭제
     *
     * @param userId
     * @param clusterId
     * @param projectName
     * @return
     * @throws Exception
     */
    @Transactional(transactionManager = "transactionManager")
    public void removeProject(String userId, String clusterId, String projectName) throws Exception {

        ServerlessWorkspaceVO serverlessWorkspace = null;

        ExceptionMessageUtils.checkParameterRequired("userId", userId);
        ExceptionMessageUtils.checkParameterRequired("clusterId", clusterId);
        ExceptionMessageUtils.checkParameterRequired("projectName", projectName);
        projectName = StringUtils.trim(projectName);

        IServerlessMapper serverlessDao = sqlSession.getMapper(IServerlessMapper.class);
        IServiceMapper serviceDao = sqlSession.getMapper(IServiceMapper.class);

        ClusterVO cluster = serverlessDao.getClusterByClusterId(clusterId);

        if (cluster != null && cluster.getAccount() != null) {
            /**
             * cluster 상태 체크
             */
            clusterStateService.checkClusterState(cluster);

            String serviceName = userId;
            serverlessWorkspace = serverlessDao.getWorkspace(cluster.getAccount().getAccountSeq(), null, serviceName, userId);

            boolean serverlessNotExists = false;
            ServerlessVO serverless = null;
            if (serverlessWorkspace != null) {

                serverlessWorkspace.setServerlesses(serverlessDao.getServerlesses(serverlessWorkspace.getServiceSeq(), null));

                if (CollectionUtils.isNotEmpty(serverlessWorkspace.getServerlesses())) {
                    String finalProjectName = projectName;
                    Optional<ServerlessVO> serverlessOptional = serverlessWorkspace.getServerlesses().stream()
                            .filter(s -> (StringUtils.equals(clusterId, s.getClusterId()) && StringUtils.equals(finalProjectName, s.getProjectName())))
                            .findFirst();

                    if (serverlessOptional.isPresent()) {
                        serverless = serverlessOptional.get();
                        cluster.setNamespaceName(projectName);
                    } else {
                        serverlessNotExists = true;
                    }
                }
            } else {
                serverlessNotExists = true;
            }

            if (serverlessNotExists) {
                throw new CocktailException("Project not found.", ExceptionType.ResourceNotFound);
            } else {
                Integer servicemapSeq = serverless.getServicemapSeq();

                IServicemapMapper servicemapDao = sqlSession.getMapper(IServicemapMapper.class);
                ServicemapVO servicemap = servicemapDao.getServicemap(servicemapSeq, null);

                String namespaceNotFoundMsg = "";

                try {
                    String fieldSelector = String.format("metadata.name=%s", cluster.getNamespaceName());
                    List<K8sNamespaceVO> namespaces = namespaceService.getNamespacesToList(cluster.getClusterSeq(), fieldSelector, null, ContextHolder.exeContext());

                    if(CollectionUtils.isEmpty(namespaces)){
                        namespaceNotFoundMsg = String.format("REMOVE_SERVICEMAP ##### - Namespace not found - [%s]!!", cluster.getNamespaceName());
                        log.debug("{} {} - cluster: [{}, {}], servicemap: {}, namespace: {}, updater: [{}, {}]", "###############################", namespaceNotFoundMsg, cluster.getClusterSeq(), cluster.getClusterId(), servicemapSeq, cluster.getNamespaceName(), ContextHolder.exeContext().getUserSeq(), ContextHolder.exeContext().getUserRole());
                        throw new CocktailException(namespaceNotFoundMsg, ExceptionType.K8sNamespaceNotFound);
                    }else{
                        namespaceService.deleteNamespace(cluster, cluster.getNamespaceName(), ContextHolder.exeContext());
                    }
                } catch (Exception e) {
                    if (e instanceof CocktailException) {
                        if(((CocktailException) e).getType() != ExceptionType.K8sNamespaceNotFound){
                            throw e;
                        }
                    } else {
                        throw e;
                    }
                }

                int result = 0;
                String baseLogFormat = String.format("############################### REMOVE_SERVICEMAP ##### - cluster: [%d, %s], servicemap: %d, namespace: %s, updater: [%d, %s]"
                        , Optional.ofNullable(cluster).map(ClusterVO::getClusterSeq).orElseGet(() ->0)
                        , Optional.ofNullable(cluster).map(ClusterVO::getClusterId).orElseGet(() ->"")
                        , servicemapSeq
                        , Optional.ofNullable(cluster).map(ClusterVO::getNamespaceName).orElseGet(() ->"")
                        , ContextHolder.exeContext().getUserSeq()
                        , ContextHolder.exeContext().getUserRole());

                result = servicemapDao.removeComponentsByServicemap(servicemapSeq, ContextHolder.exeContext().getUserSeq());
                log.info("{}, {}: {}", baseLogFormat, "removeComponents", result);

                result = servicemapDao.removeWorkloadGroupsByServicemap(servicemapSeq, ContextHolder.exeContext().getUserSeq());
                log.info("{}, {}: {}", baseLogFormat, "removeWorkloadGroups", result);

                result = servicemapDao.removePipelineRunByServicemap(servicemapSeq, ContextHolder.exeContext().getUserSeq());
                log.info("{}, {}: {}", baseLogFormat, "removePipelineRun", result);

                result = servicemapDao.removePipelineContainerByServicemap(servicemapSeq, ContextHolder.exeContext().getUserSeq());
                log.info("{}, {}: {}", baseLogFormat, "removePipelineContainer", result);

                result = servicemapDao.removePipelineWorkloadByServicemap(servicemapSeq, ContextHolder.exeContext().getUserSeq());
                log.info("{}, {}: {}", baseLogFormat, "removePipelineWorkload", result);


                result = serverlessDao.removeServerlessInfoByProject(servicemapSeq, ContextHolder.exeContext().getUserSeq());
                log.info("{}, {}: {}", baseLogFormat, "removeServerlessInfoByProject", result);

                result = serverlessDao.removeServerless(servicemapSeq, ContextHolder.exeContext().getUserSeq());
                log.info("{}, {}: {}", baseLogFormat, "removeServerless", result);

                // remove 서비스맵
                servicemap.setUpdater(ContextHolder.exeContext().getUserSeq());
                servicemapDao.removeServicemap(servicemap);

                // 맵핑 삭제 및 서비스맵그룹 sort 초기화 처리
                if (CollectionUtils.isNotEmpty(servicemap.getServicemapMappings())) {
                    for (ServicemapMappingVO smmRow : servicemap.getServicemapMappings()) {
                        if (smmRow.getServicemapGroup() != null) {
                            // servicemapgroup, servicemap 맵핑 삭제
                            servicemapDao.deleteServicemapgroupServicemapMapping(smmRow.getServicemapGroup().getServicemapGroupSeq(), servicemapSeq);

                            // 서비스맵그룹 sort 초기화 처리
                            servicemapDao.updateServicemapInitSortOrder(smmRow.getServicemapGroup().getServicemapGroupSeq());
                        }

                        // service, servicemap 맵핑 제거
                        servicemapDao.deleteServiceServicemapMapping(smmRow.getServiceSeq(), servicemapSeq);

                        try {
                            // 서비스 이벤트 처리
                            this.eventService.getInstance().sendServices(smmRow.getServiceSeq(), servicemapSeq, ContextHolder.exeContext());
                        } catch (Exception e) {
                            log.error("Error! on removeServicemap, event sendServices", e);
                        }
                    }
                }

            }
        } else {
            throw new CocktailException("Cluster is null.", ExceptionType.InvalidParameter_Empty);
        }
    }


    /**
     * <pre>
     * Serverless > project(namespace) 생성
     * - 워크스페이스가 없다면 생성(워크스페이스명은 userId로 함)
     * - user도 없다면 생성
     * - projectName은 namespace명으로 함
     * </pre>
     *
     * @param projectAdd
     * @throws Exception
     */
    @Transactional(transactionManager = "transactionManager")
    public ServerlessWorkspaceVO addProjectWithWorkspace(ProjectAddVO projectAdd) throws Exception {

        ServerlessWorkspaceVO serverlessWorkspace = null;

        ExceptionMessageUtils.checkParameter("userId", projectAdd.getUserId(), 50, true);
        ExceptionMessageUtils.checkParameter("clusterId", projectAdd.getClusterId(), 50, true);
        ExceptionMessageUtils.checkParameter("projectName", projectAdd.getProjectName(), 50, true);

        IServerlessMapper serverlessDao = sqlSession.getMapper(IServerlessMapper.class);
        IServiceMapper serviceDao = sqlSession.getMapper(IServiceMapper.class);

        ClusterVO cluster = serverlessDao.getClusterByClusterId(projectAdd.getClusterId());

        if (cluster != null && cluster.getAccount() != null) {
            /**
             * cluster 상태 체크
             */
            clusterStateService.checkClusterState(cluster);

            /**
             * project(namespace) 중복 및 validation 체크
             */
            if (StringUtils.isNotBlank(projectAdd.getProjectName())) {
                // validation 체크
                if (!ResourceUtil.validNamespaceName(projectAdd.getProjectName())) {
                    throw new CocktailException("Invalid projectName!! (a lowercase RFC 1123 label must consist of lower case alphanumeric characters or '-', and must start and end with an alphanumeric character (e.g. 'my-name',  or '123-abc', regex used for validation is '[a-z0-9]([-a-z0-9]*[a-z0-9])?'))", ExceptionType.NamespaceNameInvalid);
                }

                // 중복 체크
                String fieldSelector = String.format("metadata.name=%s", projectAdd.getProjectName());
                List<K8sNamespaceVO> namespaces = namespaceService.getNamespacesToList(cluster.getClusterSeq(), fieldSelector, null, ContextHolder.exeContext());

                if (CollectionUtils.isNotEmpty(namespaces)) {
                    throw new CocktailException(String.format("Already exists project - [%s]!!", projectAdd.getProjectName()), ExceptionType.NamespaceAlreadyExists);
                }
            }

            // 워크스페이스명은 userId로 함
            if (StringUtils.isBlank(projectAdd.getServiceName())) {
                projectAdd.setServiceName(projectAdd.getUserId());
            }

            // 워크스페이스 조회
            List<ServerlessWorkspaceVO> workspaces = serverlessDao.getWorkspaces(cluster.getAccount().getAccountSeq(), projectAdd.getServiceName(), null);

            // 워크스페이스가 없다면 생성
            // 사용자도 생성
            if (CollectionUtils.isEmpty(workspaces)) {

                // 워크스페이스 생성
                ServiceAddVO serviceAdd = new ServiceAddVO();
                serviceAdd.setAccountSeq(cluster.getAccount().getAccountSeq());
                serviceAdd.setServiceName(projectAdd.getServiceName());
                serviceAdd.setDescription("For serverless workspace");
                serviceAdd.setColorCode("red");
                serviceAdd.setServiceType(ServiceType.NORMAL);
                serviceAdd.setClusterTenancy(ClusterTenancy.SOFT);

                // 워크스페이스 생성
                this.addWorkspace(ApiVersionType.V2.getType().toLowerCase(), serviceAdd);

                // 클러스터 추가
                ServiceClusterVO serviceCluster = new ServiceClusterVO();
                serviceCluster.setServiceSeq(serviceAdd.getServiceSeq());
                serviceCluster.setClusterSeq(cluster.getClusterSeq());
                serviceDao.addClustersOfServiceV2(Collections.singletonList(serviceCluster), ContextHolder.exeContext().getUserSeq());

                // 서용자 생성
                AuthVO auth = new AuthVO();
                auth.setAccountId(cluster.getAccount().getAccountCode());
                auth.setUsername(projectAdd.getUserId());
                auth.setRole(UserRole.DEVOPS.getCode());
                UserVO user = this.addUser(auth, cluster.getAccount().getAccountSeq());

                if (user != null) {
                    // 사용자 추가
                    ServiceUserVO serviceUser = new ServiceUserVO();
                    serviceUser.setUserSeq(user.getUserSeq());
                    serviceUser.setUserGrant(UserGrant.USER);
                    serviceDao.addUsersOfService(serviceAdd.getServiceSeq(), Collections.singletonList(serviceUser), ContextHolder.exeContext().getUserSeq());
                }

                serverlessWorkspace = serverlessDao.getWorkspace(null, serviceAdd.getServiceSeq(), null, null);

            } else {
                // 프로젝트(서비스맵) 삭제시 워크스페이스에 해당 클러스터로 할당된 서비스맵이 없다면 연결정보도 삭제하므로
                // 프로젝트 생성시 클러스터 할당 정보를 다시 체크하여 연결할 수 있도록 함.
                boolean needCluster = false;
                // 할당된 클러스터가 없거나
                if (CollectionUtils.isEmpty(workspaces.get(0).getClusters())) {
                    needCluster = true;
                } else {
                    // 할당된 클러스터 중 요청 클러스터가 없을 시
                    if (workspaces.get(0).getClusters().stream().noneMatch(c -> (cluster.getClusterSeq().equals(c.getClusterSeq())))) {
                        needCluster = true;
                    }
                }
                if (needCluster) {
                    // 클러스터 추가
                    ServiceClusterVO serviceCluster = new ServiceClusterVO();
                    serviceCluster.setServiceSeq(workspaces.get(0).getServiceSeq());
                    serviceCluster.setClusterSeq(cluster.getClusterSeq());
                    serviceDao.addClustersOfServiceV2(Collections.singletonList(serviceCluster), ContextHolder.exeContext().getUserSeq());
                }

                // 워크스페이스 이름은 플랫폼안에서 유니크하여 단일값에 바로 셋팅
                serverlessWorkspace = workspaces.get(0);
            }


            // 프로젝트(서비스맵, 네임스페이스)를 생성하고
            // 워크스페이스에 추가 처리
            ServicemapAddVO servicemapAdd = new ServicemapAddVO();
            servicemapAdd.setServiceSeq(serverlessWorkspace.getServiceSeq());
            servicemapAdd.setServicemapGroupSeq(Optional.ofNullable(serverlessWorkspace.getServicemapGroups()).orElseGet(() ->Lists.newArrayList()).get(0).getServicemapGroupSeq());
            servicemapAdd.setNamespaceName(projectAdd.getProjectName());
            servicemapAdd.setServicemapName(projectAdd.getProjectName());
            servicemapAdd.setClusterSeq(cluster.getClusterSeq());
            servicemapAdd.setNetworkPolicyCreationType(NetworkPolicyCreationType.INGRESS_BLOCK_EGRESS_ALLOW);

            ServicemapVO servicemap = this.addProject(servicemapAdd, cluster, serverlessWorkspace, projectAdd, ContextHolder.exeContext());

            // 서비리스 정보 틍록
            ServerlessVO serverless = new ServerlessVO();
            serverless.setServicemapSeq(servicemap.getServicemapSeq());
            serverless.setProjectName(projectAdd.getProjectName());

            serverlessDao.addServerless(serverless);

            serverlessWorkspace = serverlessDao.getWorkspace(cluster.getAccount().getAccountSeq(), null, projectAdd.getServiceName(), projectAdd.getUserId());

        } else {
            throw new CocktailException("Cluster is null", ExceptionType.InvalidParameter_Empty);
        }

        if (serverlessWorkspace != null) {
            serverlessWorkspace.setServerlesses(serverlessDao.getServerlesses(serverlessWorkspace.getServiceSeq(), null));
        }

        return serverlessWorkspace;
    }

    /**
     * 사용자 추가
     *
     * @param auth
     * @return
     * @throws Exception
     */
    @Transactional(transactionManager = "transactionManager")
    public UserVO addUser(AuthVO auth, Integer accountSeq) throws Exception {

        UserVO checkUser = new UserVO();
        checkUser.setUserId(auth.getUsername());
        checkUser.setUserRole(auth.getRole());
        AccountVO account = new AccountVO();
        account.setAccountCode(auth.getAccountId());
        List<UserVO> authUsers = userService.getUsersForCheck(checkUser);

        UserVO user = null;
        boolean addUser = false;

        if (CollectionUtils.isEmpty(authUsers)) {
            addUser = true;
        } else {
            if (StringUtils.equalsIgnoreCase(UserRole.DEVOPS.getCode(), auth.getRole())) {
                Optional<UserVO> authUserOptional = authUsers.stream()
                        .filter(au -> StringUtils.equalsIgnoreCase(au.getUserId(), auth.getUsername()) && StringUtils.equalsIgnoreCase(au.getAccount().getAccountCode(), auth.getAccountId()))
                        .findFirst();

                if (authUserOptional.isPresent()) {
                    authUsers = new ArrayList<>();
                    authUsers.add(authUserOptional.get());
                }
            }

            // 조회한 사용자의 권한으로 사용자 조회
            user = userService.getUserById(auth.getUsername(), authUsers.get(0).getRoles().get(0), auth.getAccountId());
            if (user == null) {
                addUser = true;
            } else {
                for(String roles : user.getRoles()) {
                    user.setUserRole(roles);
                    break;
                }

                UserRole userRole = UserRole.valueOf(user.getUserRole());

                // 사용자가 등록되어 있다면 권한을 체크
                // 'DEVOPS'가 아니라면 오류 처리
                if (!userRole.isDevops()) {
                    throw new CocktailException("A user with Invalid role already exists. (User only)", ExceptionType.UserRoleInvalid);
                }
            }
        }

        if (addUser) {
            user = new UserVO();
            user.setUserId(checkUser.getUserId());
            user.setUserName(checkUser.getUserId());
            user.setDescription("Serverless user");
            user.setUserLanguage(LanguageType.ko);
            user.setUserTimezone(LanguageType.ko.getTimezone());
            user.setPasswordInterval(99999);
            user.setRoles(Collections.singletonList(UserRole.DEVOPS.getCode()));

            userService.addUser(user);


            // 플랫폼에 사용자 추가
            accountService.addUserOfAccountMapping(accountSeq, Collections.singletonList(user.getUserSeq()), ContextHolder.exeContext().getUserSeq());
        }

        return user;
    }

    /**
     * Serverless 워크스페이스 추가
     *
     * @param apiVersion
     * @param serviceAdd
     * @return
     * @throws Exception
     */
    @Transactional(transactionManager = "transactionManager")
    public ServiceAddVO addWorkspace(String apiVersion, ServiceAddVO serviceAdd) throws Exception {
        if (serviceAdd.getServiceType() == null) {
            serviceAdd.setServiceType(ServiceType.NORMAL); // default NORMAL
        }
        if (serviceAdd.getClusterTenancy() == null) {
            serviceAdd.setClusterTenancy(ClusterTenancy.SOFT);
        }

        /**
         * header 정보로 요청 사용자 권한 체크, DevOps는 권한 없음
         */
        AuthUtils.checkAuth(ContextHolder.exeContext(), AuthUtils.checkUserDevOpsBlockAuthPredicate());

        // check parameter
        serviceValidService.checkParameter(serviceAdd);

        // check workspace name
        serviceValidService.checkWorkspaceNameUsed(serviceAdd);

        AccountVO account = null;
        if (serviceAdd.getAccountSeq() != null) {
            boolean isValidAccount = true;
            account = accountService.getAccount(serviceAdd.getAccountSeq());

            if (account == null) {
                isValidAccount = false;
            }

            if (!isValidAccount) {
                throw new CocktailException("account seq is invalid.", ExceptionType.InvalidParameter, ExceptionMessageUtils.setParameterDataInvalid("accountSeq", serviceAdd.getAccountSeq()));
            }

            // account 에 설정된 workspace 갯수를 초과 하는지 체크
            boolean isPossibleRegisterWorkspace = accountGradeService.isPossibleRegisterWorkspace(serviceAdd.getAccountSeq());
            if (!isPossibleRegisterWorkspace) { // 등록 불가능하면 Exception 발생
                String errMsg = "Account Has Exceeded the Maximum Number of Allowed Workspaces.";
                throw new CocktailException(errMsg, ExceptionType.ExceededMaximumWorkspaceInAccount, ExceptionMessageUtils.setParameterData("accountSeq", serviceAdd.getAccountSeq(), errMsg));
            }

        } else {
            throw new CocktailException("account seq is null.", ExceptionType.InvalidParameter_Empty, ExceptionMessageUtils.setParameterDataEmpty("accountSeq", serviceAdd.getAccountSeq()));
        }

        /** Harbor Registry 사용자 정보 구성 **/
        HarborUserReqVO registryReqUser = new HarborUserReqVO();
        // 생성되는 워크스페이스 (서비스)에 생성한 사용자 정보 Setting => DB Insert시에는 암호화 처리해서 넣음.
        serviceAdd.setRegistryUserId(ResourceUtil.makeRegistryUserId());
        serviceAdd.setRegistryUserPassword(ResourceUtil.makeRegistryUserPassword());
        // 해당 워크스페이스의 registry 사용자 등록 정보
        registryReqUser.setUsername(serviceAdd.getRegistryUserId());
        registryReqUser.setPassword(serviceAdd.getRegistryUserPassword());
        registryReqUser.setRealname("default devops user");

        HarborUserRespVO registryUser = new HarborUserRespVO();

        /**
         * AccountType not in (CubeEngine) 일 경우에만 registry 기능 지원
         */
        if (!account.getAccountType().isCubeEngine()) {
            // harbor api client
            IHarborRegistryService harborRegistryService = harborRegistryFactory.getService(serviceAdd.getAccountSeq());

            // 해당 account에 registry의 type이 'SERVICE'인 registry를 조회 (SERVICE는 해당 service에서 생성한 것)
            List<ServiceDetailVO> services = Optional.ofNullable(serviceService.getServiceOfAccount(serviceAdd.getAccountSeq(), ServiceRegistryType.SERVICE.getCode())).orElseGet(() ->Lists.newArrayList());
            // projectId 별로 Map에 셋팅
            Map<Integer, ServiceRegistryVO> anotherServiceProjectMap =
                    Optional.ofNullable(
                            services.stream()
                                    .filter(s -> (CollectionUtils.isNotEmpty(s.getProjects())))
                                    .flatMap(s -> (s.getProjects().stream()))
                                    .collect(Collectors.toMap(ServiceRegistryVO::getProjectId, Function.identity())))
                    .orElseGet(() ->Maps.newHashMap());

            /**
             * 01. Registry 유효성 체크.
             */
            if (CollectionUtils.isNotEmpty(serviceAdd.getProjects())) {

                for (ServiceRegistryVO srRow : serviceAdd.getProjects()) {
                    // 'SHARE' Type은 실제 account에 속한 'SERVICE' type의 registry 정보와 맞는 지 체크
                    boolean isValid = true;
                    if (!anotherServiceProjectMap.containsKey(srRow.getProjectId())) {
                        isValid = false;
                    } else {
                        if (MapUtils.getObject(anotherServiceProjectMap, srRow.getProjectId(), null) != null) {
                            if (!StringUtils.equals(anotherServiceProjectMap.get(srRow.getProjectId()).getProjectName(), srRow.getProjectName())) {
                                isValid = false;
                            }
                        } else {
                            isValid = false;
                        }
                    }

                    if (!isValid) {
                        throw new CocktailException(String.format("The registry(%d : %s) to share does not exist on the system", srRow.getProjectId(), srRow.getProjectName()), ExceptionType.RegistryToShareDoesNotExistOnTheSystem);
                    }
                }
            }

            Step step = Step.Start;

            /**
             * 02. Registry 등록 시작
             */
            try {
                /** 02.01 harbor에 사용자 등록**/
                registryUser = harborRegistryService.addUser(registryReqUser);
                step = Step.DockerUser;

                if (registryUser != null) {
                    HarborProjectMemberVO projectMember = new HarborProjectMemberVO();
                    projectMember.setEntityName(registryUser.getUsername());
                    // DEVELOPER(pull, push) 권한 부여
                    projectMember.setRoleId(HarborRegistryProjectMemberRole.DEVELOPER.getValue());

                    /** 02.02 Harbor에 Project생성(if SERVICE type) / 위에서 생성한 사용자를 Project Member로 등록 **/
                    List<Integer> shareProjectIds = new ArrayList<>();
                    // 'SERVICE' type의 registry는 생성하고 위에서 생성한 service registry 사용자를 할당함.
                    // 'SHARE' type의 registry는 위에서 생성한 service registry 사용자만 할당함.
                    if (CollectionUtils.isNotEmpty(serviceAdd.getProjects())) {
                        serviceAdd.setProjectIds(new ArrayList<>());
                        HarborProjectReqVO projectReq;
                        for (ServiceRegistryVO srRow : serviceAdd.getProjects()) {
                            if (srRow.getProjectType() == ServiceRegistryType.SERVICE) {
                                projectReq = new HarborProjectReqVO();
                                projectReq.setPublic(false);
                                projectReq.setProjectName(srRow.getProjectName());
                                RegistryProjectVO addProject = harborRegistryService.addProject(projectReq);

                                if (addProject != null) { // 등록 성공
                                    serviceAdd.getProjectIds().add(addProject.getProjectId());
                                    srRow.setProjectId(addProject.getProjectId());

                                    // add member ( role : developer )
                                    harborRegistryService.addMemberToProject(addProject.getProjectId(), projectMember, false);
                                }
                            } else {
                                shareProjectIds.add(srRow.getProjectId());
                                // add member ( role : developer )
                                harborRegistryService.addMemberToProject(srRow.getProjectId(), projectMember, false);
                            }
                        }
                    }

                    step = Step.DockerProject;

                    /** 02.03 위에서 생성한 사용사를 account 안의 다른 service registry에 멤버(GUEST)로 사용자 등록 처리  (ImagePull은 어디서든 가능하도록 처리..) **/
                    List<ServiceDetailVO> servicesOfAccount = serviceService.getServiceOfAccount(serviceAdd.getAccountSeq(), ServiceRegistryType.SERVICE.getCode());
                    if (CollectionUtils.isNotEmpty(servicesOfAccount)) {
                        projectMember.setRoleId(HarborRegistryProjectMemberRole.GUEST.getValue());

                        HarborProjectMemberVO anotherProjectMember = new HarborProjectMemberVO();
                        anotherProjectMember.setEntityName(registryUser.getUsername());
                        anotherProjectMember.setRoleId(HarborRegistryProjectMemberRole.GUEST.getValue());

                        for (ServiceDetailVO serviceRow : servicesOfAccount) {
                            if (CollectionUtils.isNotEmpty(serviceRow.getProjects())) {
                                for (ServiceRegistryVO srRow : serviceRow.getProjects()) {
                                    if (!shareProjectIds.contains(srRow.getProjectId())) {
                                        // add member ( role : guest )
                                        harborRegistryService.addMemberToProject(srRow.getProjectId(), projectMember, false);
                                    }
                                }
                            }
                            if (CollectionUtils.isNotEmpty(serviceAdd.getProjectIds())) {
                                anotherProjectMember.setEntityName(CryptoUtils.decryptAES(serviceRow.getRegistryUserId()));
                                for (Integer projectIdRow : serviceAdd.getProjectIds()) {
                                    // add member ( role : guest )
                                    harborRegistryService.addMemberToProject(projectIdRow, anotherProjectMember, false);
                                }
                            }
                        }
                    }
                    step = Step.DockerUserToAnotherProject;

                    serviceAdd.setCreator(ContextHolder.exeContext().getUserSeq());

                    // service 등록
                    serviceService.addService(serviceAdd, apiVersion);
                } else {
                    throw new CocktailException("add registry user fail!!", ExceptionType.RegistryAddUserFail);
                }

            } catch (Exception eo) { // roll back as possible
                log.error("fail addService!!", eo);
                try {
                    switch (step) {
                        case DockerUserToAnotherProject:
                        case DockerUserToProject:
                        case DockerProject:
                            if (CollectionUtils.isNotEmpty(serviceAdd.getProjects())) {
                                for (ServiceRegistryVO srRow : serviceAdd.getProjects()) {
                                    if (srRow.getProjectType() == ServiceRegistryType.SERVICE && srRow.getProjectId() != null) {
                                        harborRegistryService.deleteProject(srRow.getProjectId());
                                    }
                                }
                            }
                        case DockerUser:
                            if (registryUser != null && registryUser.getUserId() != null) {
                                harborRegistryService.deleteUser(registryUser.getUserId());
                            }
                    }
                } catch (Exception eo2) {
                    log.error("fail rollback addService!!", eo2);
                }

                throw eo;
            }
        } else {
            if (CollectionUtils.isNotEmpty(serviceAdd.getProjects())) {
                CocktailException ce = new CocktailException(String.format("Registry is not supported.[%s]", account.getAccountType().getCode()), ExceptionType.InvalidParameter, serviceAdd.getProjects());
                log.warn("Registry is not supported.", ce);
                serviceAdd.setProjects(null);
            }
            // harbor api client
            IHarborRegistryService harborRegistryService = harborRegistryFactory.getService(serviceAdd.getAccountSeq());

            Step step = Step.Start;

            try {
                /** CubeEngine -> Cocktail 서비스로 Upgrade가 가능하므로, 사용자는 미리 생성 해 둔다 **/
                // harbor에 사용자 등록
                registryUser = harborRegistryService.addUser(registryReqUser);
                step = Step.DockerUser;

                serviceAdd.setCreator(ContextHolder.exeContext().getUserSeq());

                // service 등록
                serviceAdd = serviceService.addService(serviceAdd, apiVersion);
            } catch (Exception eo) { // roll back as possible
                log.error("fail addService!!", eo);
                try {
                    if (step == Step.DockerUser) {
                        if (registryUser != null && registryUser.getUserId() != null) {
                            harborRegistryService.deleteUser(registryUser.getUserId());
                        }
                    }
                } catch (Exception eo2) {
                    log.error("fail rollback addService!!", eo2);
                }

                throw eo;
            }
        }

        return serviceAdd;
    }

    /**
     * <pre>
     * Serverless > project(namespace) 생성
     * - 서비스맵
     * - k8s namespace
     * - k8s networkPolicy
     * - serverless project 정보
     * </pre>
     *
     * @param servicemapAdd
     * @param cluster
     * @param serverlessWorkspace
     * @param projectAdd
     * @param context
     * @return
     * @throws Exception
     */
    @Transactional(transactionManager = "transactionManager")
    public ServicemapVO addProject(ServicemapAddVO servicemapAdd, ClusterVO cluster, ServerlessWorkspaceVO serverlessWorkspace, ProjectAddVO projectAdd, ExecutingContextVO context) throws Exception {

        // check validation
        ExceptionMessageUtils.checkParameterRequired("clusterSeq", servicemapAdd.getClusterSeq());
        ExceptionMessageUtils.checkParameter("namespaceName", servicemapAdd.getNamespaceName(), 50, true);
        ExceptionMessageUtils.checkParameter("servicemapName", servicemapAdd.getServicemapName(), 50, true);

        IServicemapMapper servicemapDao = sqlSession.getMapper(IServicemapMapper.class);
        IWorkloadGroupMapper workloadGroupDao = sqlSession.getMapper(IWorkloadGroupMapper.class);

        try {
            String fieldSelector = String.format("metadata.name=%s", servicemapAdd.getNamespaceName());
            List<K8sNamespaceVO> namespaces = namespaceService.getNamespacesToList(cluster.getClusterSeq(), fieldSelector, null, context);

            if(CollectionUtils.isNotEmpty(namespaces)){
                throw new CocktailException(String.format("Already exists Namespace - [%s]!!", servicemapAdd.getNamespaceName()), ExceptionType.NamespaceAlreadyExists);
            }
            log.debug("namespace: {}", servicemapAdd.getNamespaceName());
            Map<String, String> labels = Maps.newHashMap();
            // istio-injection: enabled
//            labels.put(KubeConstants.LABELS_ISTIO_INJECTION_KEY, KubeConstants.LABELS_ISTIO_INJECTION_VALUE_ENABLED);
            labels.put(KubeConstants.LABELS_ISTIO_INJECTION_KEY, KubeConstants.LABELS_ISTIO_INJECTION_VALUE_DISABLED);
            Map<String, String> annotations = Maps.newHashMap();
            // 사용자 아이디 이메일 경우 오류 발생 (일단 제거)
            annotations.put(KubeConstants.LABELS_ACORNSOFT_SERVERLESS_USERID, projectAdd.getUserId());
            namespaceService.createNamespace(cluster, servicemapAdd.getNamespaceName(), labels, annotations, context);

            /**
             * Nemespace > NetworkPolicy 생성 (dryRun)
             */
            if (ResourceUtil.isSupportedDryRun(cluster.getK8sVersion())) {
                this.createNetworkPolicy(cluster, servicemapAdd.getNamespaceName(), true, context);
            }


            // create servicemap
            servicemapAdd.setCreator(context.getUserSeq());
            servicemapAdd.setUseYn("Y");
            servicemapDao.addServicemap(servicemapAdd);

            if (servicemapAdd.getServiceSeq() != null) {
                // service(workspace), servicemap Mapping
                ServicemapMappingVO smm = new ServicemapMappingVO();
                smm.setServiceSeq(servicemapAdd.getServiceSeq());
                smm.setServicemapSeq(servicemapAdd.getServicemapSeq());
                smm.setCreator(context.getUserSeq());
                servicemapDao.addServiceServicemapMapping(smm);

                if (servicemapAdd.getServicemapGroupSeq() != null) {
                    // servicemapgroup, service Mapping
                    ServicemapGroupMappingVO smgm = new ServicemapGroupMappingVO();
                    smgm.setServicemapGroupSeq(servicemapAdd.getServicemapGroupSeq());
                    smgm.setServicemapSeq(servicemapAdd.getServicemapSeq());
                    smgm.setCreator(context.getUserSeq());
                    servicemapDao.addServicemapgroupServicemapMapping(smgm);
                }
            }

            /**
             * imagePullSecret 생성 (플랫폼 레지스트리 pull 사용자로 생성)
             */
            // 레지스트리 pull user 생성 및 존재하는 레지스트리에 pull user member 추가
            accountService.createAccountRegistryPullUser(cluster.getAccount());

            String registryPullUserId = CryptoUtils.decryptAES(cluster.getAccount().getRegistryDownloadUserId());
            String registryPullUserPassword = CryptoUtils.decryptAES(cluster.getAccount().getRegistryDownloadUserPassword());

            if (secretService.getSecret(cluster, servicemapAdd.getNamespaceName(), registryPullUserId) == null) {

                DockerRegistrySecretVO secret = new DockerRegistrySecretVO();
                secret.setMakeCocktail(false);
                secret.setType(SecretType.DockerRegistry);
                secret.setName(registryPullUserId);
                secret.setUserName(registryPullUserId);
                secret.setPassword(registryPullUserPassword);
                secret.setEmail(String.format("%s@%s", registryPullUserId, CommonConstants.DEFAULT_USER_DOMAIN));
                secret.setServerUrl(this.registry.getUrl());
                secretService.createDockerRegistrySecret(cluster, servicemapAdd.getNamespaceName(), secret);
            }

            WorkloadGroupAddVO workloadGroupAdd = new WorkloadGroupAddVO();
            workloadGroupAdd.setServicemapSeq(servicemapAdd.getServicemapSeq());
            workloadGroupAdd.setWorkloadGroupName(CommonConstants.DEFAULT_GROUP_NAME);
            workloadGroupAdd.setColumnCount(1);
            workloadGroupAdd.setSortOrder(1);
            workloadGroupAdd.setUseYn("Y");
            workloadGroupAdd.setCreator(context.getUserSeq());

            workloadGroupDao.addWorkloadGroup(workloadGroupAdd);

            /**
             * Nemespace > NetworkPolicy 생성
             */
            this.createNetworkPolicy(cluster, servicemapAdd.getNamespaceName(), false, context);

            if (servicemapAdd.getServiceSeq() != null) {
                try {
                    this.eventService.getInstance().sendServices(servicemapAdd.getServiceSeq(), servicemapAdd.getServicemapSeq(), context);
                } catch (Exception e) {
                    log.error("Error! event sendServices on addAppmap", e);
                }
            }
        } catch (Exception e) {
            boolean isExist = false;
            if (e instanceof CocktailException) {
                if (((CocktailException) e).getType() == ExceptionType.NamespaceAlreadyExists) {
                    isExist = true;
                }
            }

            if (!isExist) {
                // rollback
                namespaceService.deleteNamespace(cluster, servicemapAdd.getNamespaceName(), context);
            }

            throw e;
        }

        return servicemapDao.getServicemap(servicemapAdd.getServicemapSeq(), servicemapAdd.getServiceSeq());

    }

    /**
     * <pre>
     *     Nemespace > NetworkPolicy 생성
     *     - Egress All Allow
     *     - Ingress Partial Allow
     *       - 'knative-serving' namespace
     * </pre>
     *
     * @param cluster
     * @param namespaceName
     * @param dryRun
     * @param context
     * @throws Exception
     */
    public void createNetworkPolicy(ClusterVO cluster, String namespaceName, boolean dryRun, ExecutingContextVO context) throws Exception {
        try {
            boolean isExist = false;

            // NetworkPolicy spec 생성
            NetworkPolicyGuiVO gui = new NetworkPolicyGuiVO();
            gui.setName(KubeConstants.NETWORK_POLICY_DEFAULT_NAME);
            gui.setNamespace(namespaceName);
            gui.setDefault(true);

            gui.addPolicyTypesItem("Ingress");
            gui.addPolicyTypesItem("Egress");

            // Egress All Allow
            gui.addEgressItem(new NetworkPolicyEgressRuleVO());
            // Ingress Partial Allow
            NetworkPolicyIngressRuleVO ingress = new NetworkPolicyIngressRuleVO();

            NetworkPolicyPeerVO fromBaas = new NetworkPolicyPeerVO();
            K8sLabelSelectorVO namespaceSelectorBaas = new K8sLabelSelectorVO();
            ingress.addFromItem(
                    fromBaas.namespaceSelector(
                            namespaceSelectorBaas
                                    .putMatchLabelsItem(KubeConstants.META_LABELS_META_NAME, "knative-serving")));

            NetworkPolicyPeerVO fromFaas = new NetworkPolicyPeerVO();
            K8sLabelSelectorVO namespaceSelectorFaas = new K8sLabelSelectorVO();
            ingress.addFromItem(
                    fromFaas.namespaceSelector(
                            namespaceSelectorFaas
                                    .putMatchLabelsItem(KubeConstants.META_LABELS_META_NAME, "fission")));

            gui.addIngressItem(ingress);

            try {
                // check
                networkPolicyService.checkNetworkPolicy(cluster, namespaceName, true, gui);
            } catch (Exception e) {
                if ( e instanceof CocktailException ) {
                    if (((CocktailException) e).getType() == ExceptionType.NetworkPolicyNameAlreadyExists) {
                        isExist = true;
                    }
                }

                if (!isExist) {
                    throw e;
                }
            }

            if (!isExist) {
                networkPolicyService.createNetworkPolicy(cluster, gui, dryRun, context);
            }

        } catch (Exception e) {
            throw e;
        }
    }

    /**
     * <pre>
     * Project > Function 저장
     * - token을 발급하여 저장
     * - 이미 존재한다면 재발급 처리
     * </pre>
     *
     * @param serverlessType
     * @param functionAdd
     * @return
     * @throws Exception
     */
    @Transactional(transactionManager = "transactionManager")
    public ServerlessInfoVO mergeFunction(ServerlessType serverlessType, FunctionAddVO functionAdd) throws Exception {

        ExceptionMessageUtils.checkParameterRequired("userId", functionAdd.getUserId());
        ExceptionMessageUtils.checkParameterRequired("clusterId", functionAdd.getClusterId());
        ExceptionMessageUtils.checkParameterRequired("projectName", functionAdd.getProjectName());
        ExceptionMessageUtils.checkParameter("functionName", functionAdd.getFunctionName(), 256, true);

        ServerlessInfoVO serverlessInfo = null;

        IServerlessMapper serverlessDao = sqlSession.getMapper(IServerlessMapper.class);

        // project 조회
        ServerlessVO serverless = this.getProject(functionAdd.getUserId(), functionAdd.getClusterId(), functionAdd.getProjectName(), false);

        if (serverless != null) {

            // Function 정보가 이미 존재하는 지 확인
            if (CollectionUtils.isNotEmpty(serverless.getServerlessInfos())) {
                Optional<ServerlessInfoVO> serverlessInfoOptional = serverless.getServerlessInfos().stream()
                        .filter(sli -> (serverlessType == sli.getServerlessType() && StringUtils.equals(functionAdd.getFunctionName(), sli.getFunctionName())))
                        .findFirst();
                if (serverlessInfoOptional.isPresent()) {
                    serverlessInfo = serverlessInfoOptional.get();
                }
            }

            // issue token
            String token = this.issueToken(serverlessType, functionAdd.getUserId(), functionAdd.getClusterId(), functionAdd.getProjectName(), functionAdd.getFunctionName());

            // Function 정보가 이미 존재한다면 token만 update (reissue)
            if (serverlessInfo != null) {
                serverlessInfo.setToken(token);
                serverlessDao.updateFunctionToken(serverlessInfo.getServerlessInfoSeq(), serverlessInfo.getToken(), ContextHolder.exeContext().getUserSeq());
            }
            // 없다면 신규 등록
            else {
                serverlessInfo = new ServerlessInfoVO();
                serverlessInfo.setServerlessSeq(serverless.getServerlessSeq());
                serverlessInfo.setServerlessType(serverlessType);
                serverlessInfo.setFunctionName(functionAdd.getFunctionName());
                serverlessInfo.setToken(token);

                serverlessDao.addServerlessInfo(serverlessInfo);
            }

        } else {
            throw new CocktailException("Project not found.", ExceptionType.ResourceNotFound);
        }

        return serverlessInfo;
    }

    /**
     * Project > Function 상세 조회
     *
     * @param serverlessType
     * @param userId
     * @param clusterId
     * @param projectName
     * @param functionName
     * @return
     * @throws Exception
     */
    public ServerlessInfoVO getFunction(ServerlessType serverlessType, String userId, String clusterId, String projectName, String functionName) throws Exception {

        ServerlessInfoVO serverlessInfo = null;

        ExceptionMessageUtils.checkParameterRequired("userId", userId);
        ExceptionMessageUtils.checkParameterRequired("clusterId", clusterId);
        ExceptionMessageUtils.checkParameterRequired("projectName", projectName);
        projectName = StringUtils.trim(projectName);
        ExceptionMessageUtils.checkParameterRequired("functionName", functionName);
        functionName = StringUtils.trim(functionName);

        ServerlessVO serverless = this.getProject(userId, clusterId, projectName, false);

        if (serverless != null) {
            if (CollectionUtils.isNotEmpty(serverless.getServerlessInfos())) {
                String finalFunctionName = functionName;
                Optional<ServerlessInfoVO> serverlessInfoOptional = serverless.getServerlessInfos().stream()
                        .filter(sli -> (serverlessType == sli.getServerlessType() && StringUtils.equals(finalFunctionName, sli.getFunctionName())))
                        .findFirst();
                if (serverlessInfoOptional.isPresent()) {
                    serverlessInfo = serverlessInfoOptional.get();
                }
            }
        } else {
            throw new CocktailException("Project not found.", ExceptionType.ResourceNotFound);
        }

        return serverlessInfo;
    }

    /**
     * Project > Function 삭제
     *
     * @param serverlessType
     * @param userId
     * @param clusterId
     * @param projectName
     * @param functionName
     * @throws Exception
     */
    @Transactional(transactionManager = "transactionManager")
    public void removeFunction(ServerlessType serverlessType, String userId, String clusterId, String projectName, String functionName) throws Exception {

        ExceptionMessageUtils.checkParameterRequired("userId", userId);
        ExceptionMessageUtils.checkParameterRequired("clusterId", clusterId);
        ExceptionMessageUtils.checkParameterRequired("projectName", projectName);
        projectName = StringUtils.trim(projectName);
        ExceptionMessageUtils.checkParameterRequired("functionName", functionName);
        functionName = StringUtils.trim(functionName);

        IServerlessMapper serverlessDao = sqlSession.getMapper(IServerlessMapper.class);

        ServerlessVO serverless = this.getProject(userId, clusterId, projectName, false);

        if (serverless != null) {
            if (CollectionUtils.isNotEmpty(serverless.getServerlessInfos())) {
                String finalFunctionName = functionName;
                Optional<ServerlessInfoVO> serverlessInfoOptional = serverless.getServerlessInfos().stream()
                        .filter(sli -> (serverlessType == sli.getServerlessType() && StringUtils.equals(finalFunctionName, sli.getFunctionName())))
                        .findFirst();
                if (serverlessInfoOptional.isPresent()) {

                    serverlessDao.deleteServerlessInfo(serverlessInfoOptional.get().getServerlessInfoSeq());

                } else {
                    throw new CocktailException("Function not found.", ExceptionType.ResourceNotFound);
                }
            }
        } else {
            throw new CocktailException("Project not found.", ExceptionType.ResourceNotFound);
        }
    }

    /**
     * Function Token 발급 요청
     *
     * @param serverlessType
     * @param userId
     * @param functionName
     * @return
     * @throws Exception
     */
    private String issueToken(ServerlessType serverlessType, String userId, String clusterId, String projectName, String functionName) throws Exception {
        String token = null;

        try {
            RequestGatewayTokenVO requestToken = new RequestGatewayTokenVO();
            requestToken.setServerlessType(serverlessType.getCode());
            requestToken.setUserId(userId);
            requestToken.setClusterId(clusterId);
            requestToken.setProjectName(projectName);
            requestToken.setFunctionName(functionName);

            // issue token
            ResponseGatewayTokenVO<GatewayTokenVO> result = gatewayApiClient.requestBaasToken(requestToken);

            // BAAS token만 사용하도록 수정
//            if (ServerlessType.BAAS == serverlessType) {
//                result = gatewayApiClient.requestBaasToken(requestToken);
//            } else {
//                result = gatewayApiClient.requestFaasToken(requestToken);
//            }

            if (result != null) {
                if (StringUtils.equalsIgnoreCase("ok", result.getStatus()) && result.getResult() != null) {
                    token = CryptoUtils.encryptAES(result.getResult().getAccess_token());
                } else {
                    throw new CocktailException("Issue token fail!! - result fail.", ExceptionType.ExternalApiFail_GatewayApi, ExceptionBiz.SERVERLESS);
                }
            } else {
                throw new CocktailException("Issue token fail!! - no response.", ExceptionType.ExternalApiFail_GatewayApi, ExceptionBiz.SERVERLESS);
            }
        } catch (CocktailException e) {
            throw new CocktailException("Issue token fail!!", e, ExceptionType.ExternalApiFail_GatewayApi, ExceptionBiz.SERVERLESS);
        }

        return token;
    }


    /**
     * Gateway server 인증서 저장
     *
     * @param gatewayCertificate
     * @throws Exception
     */
    public void addGatewayCertificate(GatewayCertificateVO gatewayCertificate) throws Exception {
        IServerlessMapper serverlessDao = sqlSession.getMapper(IServerlessMapper.class);
        if (gatewayCertificate != null && StringUtils.isNotBlank(gatewayCertificate.getCertificate())) {
            gatewayCertificate.setCertificate(CryptoUtils.encryptAES(gatewayCertificate.getCertificate()));
            serverlessDao.addGatewayCertificate(gatewayCertificate);
        } else {
            throw new CocktailException("Certificate is null.", ExceptionType.InvalidParameter_Empty);
        }
    }

    /**
     * Gateway server 인증서 목록 조회
     *
     * @return
     * @throws Exception
     */
    public List<String> getGatewayCertificates() throws Exception {

        List<String> certificates = Lists.newArrayList();

        IServerlessMapper serverlessDao = sqlSession.getMapper(IServerlessMapper.class);
        List<GatewayCertificateVO> getGatewayCertificates = serverlessDao.getGatewayCertificates();

        if (CollectionUtils.isNotEmpty(getGatewayCertificates)) {
            for (GatewayCertificateVO g : getGatewayCertificates) {
                certificates.add(CryptoUtils.decryptAES(g.getCertificate()));
            }
        }

        return certificates;
    }


}
