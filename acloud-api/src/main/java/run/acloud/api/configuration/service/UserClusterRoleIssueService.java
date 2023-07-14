package run.acloud.api.configuration.service;

import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.model.CreateUserResult;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Lists;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import io.kubernetes.client.openapi.models.V1ConfigMap;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.ibatis.session.ResultHandler;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import run.acloud.api.auth.dao.IUserMapper;
import run.acloud.api.auth.enums.UserRole;
import run.acloud.api.auth.vo.UserVO;
import run.acloud.api.configuration.dao.IClusterMapper;
import run.acloud.api.configuration.enums.HistoryState;
import run.acloud.api.configuration.enums.IssueBindingType;
import run.acloud.api.configuration.enums.IssueType;
import run.acloud.api.configuration.handler.UserClusterRoleIssueHistoryResultHandler;
import run.acloud.api.configuration.handler.UserClusterRoleIssueResultHandler;
import run.acloud.api.configuration.handler.UserConfigDownloadHistoryResultHandler;
import run.acloud.api.configuration.handler.UserShellConnectHistoryResultHandler;
import run.acloud.api.configuration.vo.*;
import run.acloud.api.resource.constants.KubeConstants;
import run.acloud.api.resource.enums.CubeType;
import run.acloud.api.resource.enums.JsonPatchOp;
import run.acloud.api.resource.service.ConfigMapService;
import run.acloud.api.resource.task.K8sWorker;
import run.acloud.api.resource.util.ResourceUtil;
import run.acloud.commons.client.ClusterApiClient;
import run.acloud.commons.service.AWSResourceService;
import run.acloud.commons.util.*;
import run.acloud.commons.vo.ListCountVO;
import run.acloud.commons.vo.PagingVO;
import run.acloud.framework.context.ContextHolder;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class UserClusterRoleIssueService {
	@Resource(name = "cocktailSession")
    private SqlSessionTemplate sqlSession;

    @Autowired
    private AWSResourceService awsResourceService;

    @Autowired
    private K8sWorker k8sWorker;

    @Autowired
    private ConfigMapService configMapService;

    @Autowired
    private ClusterApiClient clusterApiClient;

    @Autowired
    private ClusterStateService clusterStateService;

    @Autowired
    private ProviderAccountService providerAccountService;

    @Transactional(transactionManager = "transactionManager")
    public List<UserClusterRoleIssueVO> addUserClusterRoleIssues(Integer accountSeq, Integer userSeq, String userId, IssueType issueType, List<UserClusterRoleIssueVO> addUserClusterRoleIssues, Integer creator) throws Exception {
        if (CollectionUtils.isNotEmpty(addUserClusterRoleIssues)) {
            IUserMapper userDao = sqlSession.getMapper(IUserMapper.class);
            IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);

            // 바인딩유형이 'NAMESPACE'인 경우 bindings 값을 json serialize 하여 bindingConfig에 셋팅하여 줌.
            for (UserClusterRoleIssueVO issue : addUserClusterRoleIssues) {

                // issue별 account make & set account name
                issue.setIssueAccountName(ResourceUtil.makeIssueAccountName(userId, issueType));

                if (IssueBindingType.valueOf(issue.getBindingType()) == IssueBindingType.NAMESPACE) {
                    if (CollectionUtils.isNotEmpty(issue.getBindings())) {
                        for (UserClusterRoleIssueBindingVO bindingRow : issue.getBindings()) {
                            bindingRow.setUserSeq(userSeq);
                            bindingRow.setClusterSeq(issue.getClusterSeq());
                            bindingRow.setIssueType(issue.getIssueType());
                            bindingRow.setIssueAccountName(issue.getIssueAccountName());
                            bindingRow.setCreator(creator);
                        }

                        issue.setBindingConfig(JsonUtils.toGson(issue.getBindings()));
                    }
                }
            }
            List<UserClusterRoleIssueVO> userClusterRoleIssuesForAdd = userDao.getUserClusterRoleIssuesForAdd(accountSeq, userSeq, issueType.getCode(), addUserClusterRoleIssues, creator);

            if (CollectionUtils.isNotEmpty(userClusterRoleIssuesForAdd)) {

                // 등록
                userDao.addUserClusterRoleIssues(userClusterRoleIssuesForAdd);

                // 등록할 clusterSeq 셋팅 (클러스터 조회용)
                Set<Integer> addClusterSeqs = Sets.newHashSet();

                List<UserClusterRoleIssueBindingVO> userClusterRoleIssueBindings = Lists.newArrayList();

                // 위에서 등록한 정보를 이력으로 등록
                List<UserClusterRoleIssueHistoryVO> userClusterRoleIssueHistories = Lists.newArrayList();
                for (UserClusterRoleIssueVO issueRow : userClusterRoleIssuesForAdd) {
                    // 바인딩유형이 'NAMESPACE'인 경우 bindings 등록
                    if (IssueBindingType.valueOf(issueRow.getBindingType()) == IssueBindingType.NAMESPACE) {
                        if (StringUtils.isNotBlank(issueRow.getBindingConfig())) {
                            issueRow.setBindings(JsonUtils.fromGson(issueRow.getBindingConfig(), new TypeToken<List<UserClusterRoleIssueBindingVO>>(){}.getType()));

                            userClusterRoleIssueBindings.addAll(issueRow.getBindings());
                        }
                    }

                    UserClusterRoleIssueHistoryVO history = new UserClusterRoleIssueHistoryVO();
                    history.setUserSeq(userSeq);
                    history.setClusterSeq(issueRow.getClusterSeq());
                    history.setIssueType(issueRow.getIssueType());
                    history.setBindingType(issueRow.getBindingType());
                    history.setExpirationDatetime(issueRow.getExpirationDatetime());
                    history.setIssueRole(issueRow.getIssueRole());
                    history.setIssueAccountName(issueRow.getIssueAccountName());
                    history.setIssueUserSeq(creator);
                    history.setHistoryState(HistoryState.GRANT.getCode());
                    userClusterRoleIssueHistories.add(history);

                    // history 추가
                    userDao.addUserClusterRoleIssueHistory(history);

                    // binding history 추가
                    if (CollectionUtils.isNotEmpty(issueRow.getBindings())) {
                        userDao.addUserClusterRoleIssueBindingHistories(history.getHistorySeq(), issueRow.getBindings(), creator);
                    }

                    // cluster-api에 history 정보 전달용
                    issueRow.setHistory(history);

                    // clusterSeq 셋팅
                    addClusterSeqs.add(issueRow.getClusterSeq());
                }

                if (CollectionUtils.isNotEmpty(userClusterRoleIssueBindings)) {
                    // binding 등록
                    userDao.addUserClusterRoleIssueBindings(userSeq, userClusterRoleIssueBindings, creator);
                }

                // KUBECONFIG 유형일 경우 EKS는 IAM을 생성하여 issue_config에 셋팅
                if (issueType == IssueType.KUBECONFIG) {

                    // cluster 정보 조회
                    List<ClusterVO> clusters = clusterDao.getClusters(accountSeq, null, null, null, Lists.newArrayList(addClusterSeqs), "Y");

                    Map<Integer, ClusterVO> eksClusterMap = Optional.ofNullable(clusters).orElseGet(() ->Lists.newArrayList()).stream().filter(c -> (
                            c.getCubeType() == CubeType.EKS && c.getCloudProviderAccountSeq() != null && c.getCloudProviderAccount() != null
                    )).collect(Collectors.toMap(ClusterVO::getClusterSeq, Function.identity()));

                    if (MapUtils.isNotEmpty(eksClusterMap)) {

                        // AWS IAM permission 체크
                        for (Map.Entry<Integer, ClusterVO> c : eksClusterMap.entrySet()) {
                            if (c != null
                                    && c.getValue().getCubeType() == CubeType.EKS
                                    && c.getValue().getCloudProviderAccountSeq() != null
                                    && c.getValue().getCloudProviderAccount() != null) {
                                if (!providerAccountService.validAwsIAMPermission(c.getValue().getCloudProviderAccount())) {
                                    throw new CocktailException("AWS IAM Authentication information is not valid!!", ExceptionType.ProviderAuthenticationIsInvalid);
                                }
                            }
                        }

                        DumperOptions options = new DumperOptions();
                        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
                        Yaml yaml = new Yaml(options);

                        AmazonIdentityManagement iam = null;

                        try {
                            for (UserClusterRoleIssueVO issueRow : userClusterRoleIssuesForAdd) {
                                if (eksClusterMap.containsKey(issueRow.getClusterSeq())) {

                                    ClusterVO cluster = eksClusterMap.get(issueRow.getClusterSeq());

                                    // AWS USER 생성
                                    iam = awsResourceService.getIAMClientUsingEncryptedAuth(cluster.getRegionCode(), cluster.getCloudProviderAccount().getApiAccountPassword());
                                    CreateUserResult createUserResult = awsResourceService.createUserUsingCocktailPath(iam, issueRow.getIssueAccountName());

                                    // AWS USER 정보 issue_config에 반영
                                    if (createUserResult != null && createUserResult.getUser() != null) {
                                        // aws-auth configMap 추가
                                        List<Map<String, Object>> valueList = this.getMapUsersInAwsAuthByEKS(cluster, yaml);

                                        Map<String, Object> addUserMap = Maps.newHashMap();
                                        addUserMap.put("username", createUserResult.getUser().getUserName());
                                        addUserMap.put("userarn", createUserResult.getUser().getArn());
//                                        addUserMap.put("groups", Arrays.asList("system:masters"));
                                        valueList.add(addUserMap);

                                        this.patchMapUsersInAwsAuthByEKS(cluster, valueList, yaml);

                                        // issueConfig에 iam 정보 업데이트
                                        IssueConfigAWSVO issueConfigAWS = new IssueConfigAWSVO();
                                        issueConfigAWS.setUserName(createUserResult.getUser().getUserName());
                                        issueConfigAWS.setArn(createUserResult.getUser().getArn());
                                        issueConfigAWS.setPath(createUserResult.getUser().getPath());
                                        String encryptIssueConfigAWS = CryptoUtils.encryptAES(JsonUtils.toGson(issueConfigAWS));
                                        userDao.updateUserClusterRoleIssueConfig(userSeq, cluster.getClusterSeq(), issueType.getCode(), issueRow.getIssueAccountName(), encryptIssueConfigAWS);

                                        issueRow.setIssueConfig(encryptIssueConfigAWS);

                                    } else {
                                        // TODO: 예외 로직 추가
                                    }
                                }
                            }

                        } finally {
                            if (iam != null) {
                                iam.shutdown();
                            }
                        }
                    }
                }

            }

            return userClusterRoleIssuesForAdd;
        }

        return null;
    }

    @Transactional(transactionManager = "transactionManager")
    public void addUserClusterRoleIssuesWithClusterApi(Integer accountSeq, Integer userSeq, String userId, IssueType issueType, List<UserClusterRoleIssueVO> addUserClusterRoleIssues, Integer creator) throws Exception {
        if (CollectionUtils.isNotEmpty(addUserClusterRoleIssues)) {

            addUserClusterRoleIssues = this.addUserClusterRoleIssues(accountSeq, userSeq, userId, issueType, addUserClusterRoleIssues, creator);

            // cluster-api 추가 요청 (serviceAccount, userAccount)
            clusterApiClient.manageClusterRole(ContextHolder.exeContext().getUserSeq().toString(), ContextHolder.exeContext().getUserRole(), accountSeq, userSeq, issueType, addUserClusterRoleIssues, null, null);
        }
    }

    @Transactional(transactionManager = "transactionManager")
    public List<UserClusterRoleIssueVO> removeUserClusterRoleIssues(Integer accountSeq, Integer userSeq, IssueType issueType, List<UserClusterRoleIssueVO> removeUserClusterRoleIssues, HistoryState historyState, Integer creator) throws Exception {

        if (CollectionUtils.isNotEmpty(removeUserClusterRoleIssues)) {
            IUserMapper userDao = sqlSession.getMapper(IUserMapper.class);
            IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);

            // 삭제할 cluster 셋팅
            Set<Integer> removeClusterSeqs = removeUserClusterRoleIssues.stream().map(UserClusterRoleIssueVO::getClusterSeq).collect(Collectors.toSet());
            List<ClusterVO> clusters = clusterDao.getClusters(accountSeq, null, null, null, Lists.newArrayList(removeClusterSeqs), "Y");
            Map<Integer, ClusterVO> clusterMap = clusters.stream().collect(Collectors.toMap(ClusterVO::getClusterSeq, Function.identity()));
            // AWS IAM permission 체크
            for (ClusterVO c : clusters) {
                if (c != null
                        && c.getCubeType() == CubeType.EKS
                        && c.getCloudProviderAccountSeq() != null
                        && c.getCloudProviderAccount() != null) {
                    if (!providerAccountService.validAwsIAMPermission(c.getCloudProviderAccount())) {
                        throw new CocktailException("AWS IAM Authentication information is not valid!!", ExceptionType.ProviderAuthenticationIsInvalid);
                    }
                }
            }

            ClusterVO cluster = null;

            // Yaml config 셋팅
            DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            Yaml yaml = new Yaml(options);

            // AWS USER 삭제
            AmazonIdentityManagement iam = null;
            try {
                for (UserClusterRoleIssueVO issueRow : removeUserClusterRoleIssues) {

                    if (userSeq != null) {
                        issueRow.setUserSeq(userSeq);
                    }
                    // KUBECONFIG 유형일 경우 EKS는 IAM 삭제
                    if (issueType == IssueType.KUBECONFIG) {
                        cluster = Optional.ofNullable(clusterMap).orElseGet(() ->Maps.newHashMap()).get(issueRow.getClusterSeq());

                        if (cluster != null
                                && cluster.getCubeType() == CubeType.EKS
                                && cluster.getCloudProviderAccountSeq() != null
                                && cluster.getCloudProviderAccount() != null) {

                            UserClusterRoleIssueVO userClusterRoleIssue = userDao.getUserClusterRoleIssue(issueRow.getUserSeq(), issueRow.getClusterSeq(), issueRow.getIssueType(), issueRow.getIssueAccountName(), null);

                            if (userClusterRoleIssue != null && StringUtils.isNotBlank(userClusterRoleIssue.getIssueConfig())) {

                                // AWS USER 정보
                                String decryptedUserInfo = CryptoUtils.decryptAES(userClusterRoleIssue.getIssueConfig());
                                IssueConfigAWSVO issueConfigAWS = JsonUtils.fromGson(decryptedUserInfo, IssueConfigAWSVO.class);

                                // aws-auth configMap 정보 삭제
                                List<Map<String, Object>> valueList = this.getMapUsersInAwsAuthByEKS(cluster, yaml);
                                valueList.removeIf(v -> (StringUtils.equals((String)v.get("username"), issueConfigAWS.getUserName()) && StringUtils.equals((String)v.get("userarn"), issueConfigAWS.getArn())));
                                this.patchMapUsersInAwsAuthByEKS(cluster, valueList, yaml);

                                // AWS iam 사용자 삭제
                                iam = awsResourceService.getIAMClientUsingEncryptedAuth(cluster.getRegionCode(), cluster.getCloudProviderAccount().getApiAccountPassword());
                                awsResourceService.deleteUserWithAccessKey(iam, issueConfigAWS.getUserName());

                            }
                        }
                    }

                    // 회수시 삭제
                    issueRow.setUpdater(creator);
                    userDao.deleteUserClusterRoleIssue(issueRow);

                    if (IssueBindingType.valueOf(issueRow.getBindingType()) == IssueBindingType.NAMESPACE) {
                        userDao.deleteUserClusterRoleIssueBinding(issueRow.getUserSeq(), issueRow.getClusterSeq(), issueRow.getIssueType(), issueRow.getIssueAccountName());
                    }
                    // 회수 이력 등록
                    UserClusterRoleIssueHistoryVO history = new UserClusterRoleIssueHistoryVO();
                    history.setUserSeq(issueRow.getUserSeq());
                    history.setClusterSeq(issueRow.getClusterSeq());
                    history.setIssueType(issueRow.getIssueType());
                    history.setBindingType(issueRow.getBindingType());
                    history.setIssueRole(issueRow.getIssueRole());
                    history.setIssueAccountName(issueRow.getIssueAccountName());
                    history.setIssueUserSeq(creator);
                    history.setHistoryState(historyState.getCode());
                    history.setHistoryMessage(JsonUtils.toGson(issueRow));
                    userDao.addUserClusterRoleIssueHistory(history);

                    // binding history 등록
                    if (CollectionUtils.isNotEmpty(issueRow.getBindings())) {
                        history.setBindings(issueRow.getBindings());
                        userDao.addUserClusterRoleIssueBindingHistories(history.getHistorySeq(), issueRow.getBindings(), creator);
                    }

                    issueRow.setHistory(history);
                }
            } finally {
                if (iam != null) {
                    iam.shutdown();
                }
            }
        }

        return removeUserClusterRoleIssues;
    }

    @Transactional(transactionManager = "transactionManager")
    public void removeUserClusterRoleIssuesWithClusterApi(Integer accountSeq, Integer userSeq, IssueType issueType, List<UserClusterRoleIssueVO> removeUserClusterRoleIssues, HistoryState historyState, Integer creator) throws Exception {
        if (CollectionUtils.isNotEmpty(removeUserClusterRoleIssues)) {

            removeUserClusterRoleIssues = this.removeUserClusterRoleIssues(accountSeq, userSeq, issueType, removeUserClusterRoleIssues, historyState, creator);

            // cluster-api 권한 삭제 요청
            clusterApiClient.manageClusterRole(ContextHolder.exeContext().getUserSeq().toString(), ContextHolder.exeContext().getUserRole(), accountSeq, userSeq, issueType, null, null, removeUserClusterRoleIssues);
        }
    }

    @Transactional(transactionManager = "transactionManager")
    public void removeUserClusterRoleIssues(ClusterVO cluster, Integer accountSeq, Integer updater, String userRole) throws Exception {

        /**
         * 해당 클러스터에 shell 권한이 부여된 사용자 권한 삭제
         */
        List<UserClusterRoleIssueVO> shellRoles = this.getUserClusterRoleIssues(null, cluster.getClusterSeq(), null, IssueType.SHELL.getCode(), null, null, true);
        shellRoles = this.removeUserClusterRoleIssues(accountSeq, null, IssueType.SHELL, shellRoles, HistoryState.REVOKE, updater);
        if (clusterStateService.isClusterRunning(cluster)) {
            // cluster-api 권한 삭제 요청
            clusterApiClient.manageClusterRole(updater.toString(), StringUtils.equalsIgnoreCase(userRole, UserRole.ADMIN.getCode()) ? UserRole.SYSTEM.getCode() : userRole, accountSeq, null, IssueType.SHELL, null, null, shellRoles);
        } else {
            log.warn("Failed to reclaim cluster privileges[SHELL] because cluster is not up.");
        }

        /**
         * 해당 클러스터에 kubeconfig 권한이 부여된 사용자 권한 삭제
         */
        List<UserClusterRoleIssueVO> kubeconfigRoles = this.getUserClusterRoleIssues(null, cluster.getClusterSeq(), null, IssueType.KUBECONFIG.getCode(), null, null, true);
        kubeconfigRoles = this.removeUserClusterRoleIssues(accountSeq, null, IssueType.KUBECONFIG, kubeconfigRoles, HistoryState.REVOKE, updater);
        if (clusterStateService.isClusterRunning(cluster)) {
            // cluster-api 권한 삭제 요청
            clusterApiClient.manageClusterRole(updater.toString(), StringUtils.equalsIgnoreCase(userRole, UserRole.ADMIN.getCode()) ? UserRole.SYSTEM.getCode() : userRole, accountSeq, null, IssueType.KUBECONFIG, null, null, kubeconfigRoles);
        } else {
            log.warn("Failed to reclaim cluster privileges[KUBECONFIG] because cluster is not up.");
        }
    }

    /**
     * EKS 클러스터의 aws-auth ConfigMap을 조회하여 'mapUsers' key의 value 리턴
     *
     * @param cluster
     * @param yaml
     * @return 'mapUsers' key의 value
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getMapUsersInAwsAuthByEKS(ClusterVO cluster, Yaml yaml) throws Exception {

        if (cluster == null || (cluster != null && cluster.getCubeType() != CubeType.EKS)) {
            return Lists.newArrayList();
        }

        if (yaml == null) {
            DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            yaml = new Yaml(options);
        }

        // aws-auth ConfigMap 조회하여 'mapUsers' key의 value 리턴
        V1ConfigMap awsAuthConfigMap = k8sWorker.getConfigMapV1(cluster, KubeConstants.KUBE_SYSTEM_NAMESPACE, KubeConstants.AWS_CONFIGMAP_NAME_AWS_AUTH);
        String strMapUsers = Optional.ofNullable(awsAuthConfigMap).map(V1ConfigMap::getData).orElseGet(() ->Maps.newHashMap()).get(KubeConstants.AWS_CONFIGMAP_KEY_MAP_USERS);
        List<Map<String, Object>> valueList = Lists.newArrayList();
        if (StringUtils.isNotBlank(strMapUsers)) {
            valueList = yaml.loadAs(strMapUsers, List.class);
        }

        return valueList;
    }

    /**
     * EKS 클러스터의 aws-auth ConfigMap의 'mapUsers' key의 value 수정
     *
     * @param cluster
     * @param mapUsers
     * @param yaml
     * @throws Exception
     */
    private void patchMapUsersInAwsAuthByEKS(ClusterVO cluster, List<Map<String, Object>> mapUsers, Yaml yaml) throws Exception {
        if (yaml == null) {
            DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            yaml = new Yaml(options);
        }

        if (cluster != null && clusterStateService.isClusterRunning(cluster) && cluster.getCubeType() == CubeType.EKS) {
            String strMapUsers = JsonUtils.toGson(mapUsers);
            mapUsers = ObjectMapperUtils.getMapper().readValue(strMapUsers, new TypeReference<List<Map<String, Object>>>(){});
            strMapUsers = yaml.dump(mapUsers);

            List<JsonObject> patchBody = new ArrayList<>();
            Map<String, Object> patchMap = new HashMap<>();
            patchMap.put("op", JsonPatchOp.REPLACE.getValue());
            patchMap.put("path", String.format("/data/%s", KubeConstants.AWS_CONFIGMAP_KEY_MAP_USERS));
            patchMap.put("value", strMapUsers);
            patchBody.add((JsonUtils.fromGson(JsonUtils.toGson(patchMap), JsonElement.class)).getAsJsonObject());

            configMapService.patchConfigMap(cluster, KubeConstants.KUBE_SYSTEM_NAMESPACE, KubeConstants.AWS_CONFIGMAP_NAME_AWS_AUTH, patchBody);
        }

    }

    @Transactional(transactionManager = "transactionManager")
    public void editUserClusterRoleIssueForClusterApi(UserClusterRoleIssueVO userClusterRoleIssue) throws Exception {
        IUserMapper userDao = sqlSession.getMapper(IUserMapper.class);
        if (userClusterRoleIssue != null) {
            userDao.updateUserClusterRoleIssueForClusterApi(userClusterRoleIssue.getUserSeq(), userClusterRoleIssue.getClusterSeq(), userClusterRoleIssue.getIssueType(), userClusterRoleIssue.getIssueShellPath());
        }
    }

    @Transactional(transactionManager = "transactionManager")
    public void editUserClusterRoleIssues(Integer userSeq, UserClusterRoleIssueVO currUserClusterRoleIssue, UserClusterRoleIssueVO updateUserClusterRoleIssue, Integer creator) throws Exception {
        if (updateUserClusterRoleIssue != null) {
            IUserMapper userDao = sqlSession.getMapper(IUserMapper.class);
            IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);

//            ClusterVO cluster = clusterDao.getCluster(updateUserClusterRoleIssue.getClusterSeq());

            // 권한 변경시 수정
            userDao.updateUserClusterRoleIssue(updateUserClusterRoleIssue, creator);

            // binding 삭제
            userDao.deleteUserClusterRoleIssueBinding(updateUserClusterRoleIssue.getUserSeq(), updateUserClusterRoleIssue.getClusterSeq(), updateUserClusterRoleIssue.getIssueType(), updateUserClusterRoleIssue.getIssueAccountName());

            // binding 등록
            if (CollectionUtils.isNotEmpty(updateUserClusterRoleIssue.getBindings())) {
                userDao.addUserClusterRoleIssueBindings(userSeq, updateUserClusterRoleIssue.getBindings(), creator);
            }

            // 권한 변경(재발급) 이력 등록
            UserClusterRoleIssueHistoryVO history = new UserClusterRoleIssueHistoryVO();
            history.setUserSeq(userSeq);
            history.setClusterSeq(updateUserClusterRoleIssue.getClusterSeq());
            history.setIssueType(updateUserClusterRoleIssue.getIssueType());
            history.setBindingType(updateUserClusterRoleIssue.getBindingType());
            history.setIssueRole(updateUserClusterRoleIssue.getIssueRole());
            history.setIssueAccountName(updateUserClusterRoleIssue.getIssueAccountName());
            history.setIssueUserSeq(creator);
            history.setHistoryState(HistoryState.CHANGE.getCode());
            history.setHistoryMessage(JsonUtils.toGson(currUserClusterRoleIssue));
            userDao.addUserClusterRoleIssueHistory(history);

            // binding history 등록
            if (CollectionUtils.isNotEmpty(updateUserClusterRoleIssue.getBindings())) {
                history.setBindings(updateUserClusterRoleIssue.getBindings());
                userDao.addUserClusterRoleIssueBindingHistories(history.getHistorySeq(), updateUserClusterRoleIssue.getBindings(), creator);
            }

            updateUserClusterRoleIssue.setHistory(history);
        }
    }

    @Transactional(transactionManager = "transactionManager")
    public void editUserClusterRoleIssues(Integer accountSeq, Integer userSeq, String userId, IssueType issueType, List<UserClusterRoleIssueVO> currUserClusterRoleIssues, List<UserClusterRoleIssueVO> uptUserClusterRoleIssues, Integer creator) throws Exception {

        if (CollectionUtils.isNotEmpty(currUserClusterRoleIssues)) {

            if (CollectionUtils.isNotEmpty(uptUserClusterRoleIssues)) {

                this.initUserClusterRoleIssuesForCompare(currUserClusterRoleIssues);
                this.initUserClusterRoleIssuesForCompare(uptUserClusterRoleIssues);

                // Map<clusterSeq, Map<issueAccountName, UserClusterRoleIssueVO>>
                Map<Integer, Map<String, UserClusterRoleIssueVO>> currRoleMap = this.convertUserClusterRoleIssueMap(currUserClusterRoleIssues);

                // Map<clusterSeq, Map<issueAccountName, List<UserClusterRoleIssueVO>>>
                Map<Integer, Map<String, UserClusterRoleIssueVO>> uptRoleMap = Maps.newHashMap();
                Map<Integer, List<UserClusterRoleIssueVO>> addRoleMap = Maps.newHashMap();
                for (UserClusterRoleIssueVO row : Optional.ofNullable(uptUserClusterRoleIssues).orElseGet(() ->Lists.newArrayList())) {
                    if (!uptRoleMap.containsKey(row.getClusterSeq())) {
                        uptRoleMap.put(row.getClusterSeq(), Maps.newHashMap());
                    }

                    // account name 이 있다면 기존 값이라 생각하고 처리
                    if (StringUtils.isNotBlank(row.getIssueAccountName())) {

                        // request 값에 없는 값을 현재 값과 비교하여 셋팅
                        if (currRoleMap.containsKey(row.getClusterSeq()) && currRoleMap.get(row.getClusterSeq()).containsKey(row.getIssueAccountName())) {
                            UserClusterRoleIssueVO currRow = currRoleMap.get(row.getClusterSeq()).get(row.getIssueAccountName());
                            if (
                                    row.getClusterSeq().equals(currRow.getClusterSeq())
                                    && StringUtils.equals(row.getIssueType(), currRow.getIssueType())
                                    && StringUtils.equals(row.getIssueAccountName(), currRow.getIssueAccountName())
                            ) {
                                row.setUserSeq(currRow.getUserSeq());
                                row.setIssueConfig(currRow.getIssueConfig());

                                if (IssueBindingType.valueOf(row.getBindingType()) == IssueBindingType.NAMESPACE && CollectionUtils.isNotEmpty(row.getBindings())) {
                                    for (UserClusterRoleIssueBindingVO bindingRow : row.getBindings()) {
                                        bindingRow.setUserSeq(currRow.getUserSeq());
                                        bindingRow.setClusterSeq(currRow.getClusterSeq());
                                        bindingRow.setIssueType(currRow.getIssueType());
                                        bindingRow.setIssueAccountName(currRow.getIssueAccountName());
                                    }
                                }

                            }
                        }

                        uptRoleMap.get(row.getClusterSeq()).put(row.getIssueAccountName(), row);
                    }
                    // account name 이 없다면 신규로 처리하기 위해 addRoleMap에 별도 셋팅
                    else {
                        if (!addRoleMap.containsKey(row.getClusterSeq())) {
                            addRoleMap.put(row.getClusterSeq(), Lists.newArrayList());
                        }

                        addRoleMap.get(row.getClusterSeq()).add(row);
                    }
                }

                Set<Integer> clusterSeqSet = Sets.newHashSet();
                clusterSeqSet.addAll(currRoleMap.keySet());
                clusterSeqSet.addAll(uptRoleMap.keySet());
                List<Integer> clusterSeqs = new ArrayList<>(clusterSeqSet);

                List<UserClusterRoleIssueVO> addRoles = Lists.newArrayList();
                List<UserClusterRoleIssueVO> editRoles = Lists.newArrayList();
                List<UserClusterRoleIssueVO> removeRoles = Lists.newArrayList();

                for (Integer clusterSeq : clusterSeqs) {
                    // 현재 권한과 수정 권한을 비교
                    MapDifference<String, UserClusterRoleIssueVO> diff = Maps.difference(Optional.ofNullable(currRoleMap.get(clusterSeq)).orElseGet(() ->Maps.newHashMap()), Optional.ofNullable(uptRoleMap.get(clusterSeq)).orElseGet(() ->Maps.newHashMap()));

                    // 현재 권한에만 있다면 삭제 처리
                    if (MapUtils.isNotEmpty(diff.entriesOnlyOnLeft())) {
                        removeRoles.addAll(new ArrayList<>(diff.entriesOnlyOnLeft().values()));

                        this.removeUserClusterRoleIssues(accountSeq, userSeq, issueType, removeRoles, HistoryState.REVOKE, creator);
                    }
                    // 수정 권한에만 있다면 추가 처리
                    if (MapUtils.isNotEmpty(diff.entriesOnlyOnRight())) {
                        addRoles.addAll(this.addUserClusterRoleIssues(accountSeq, userSeq, userId, issueType, new ArrayList<>(diff.entriesOnlyOnRight().values()), creator));
                    }
                    if (addRoleMap.get(clusterSeq) != null) {
                        // request에서 account가 없던 값들
                        addRoles.addAll(this.addUserClusterRoleIssues(accountSeq, userSeq, userId, issueType, Optional.ofNullable(addRoleMap.get(clusterSeq)).orElseGet(() ->Lists.newArrayList()), creator));
                    }
                    // 현재와 수정 권한이 서로 다를 경우 발급권한만 수정함
                    if (MapUtils.isNotEmpty(diff.entriesDiffering())) {
                        Map<String, MapDifference.ValueDifference<UserClusterRoleIssueVO>> differenceMap = diff.entriesDiffering();
                        for (Map.Entry<String, MapDifference.ValueDifference<UserClusterRoleIssueVO>> valueDifferenceEntry : differenceMap.entrySet()) {
                            // 바인딩 유형이 같을 경우 기본 로직대로 처리
                            if (StringUtils.equals(valueDifferenceEntry.getValue().leftValue().getBindingType(), valueDifferenceEntry.getValue().rightValue().getBindingType())) {
                                if (IssueBindingType.valueOf(valueDifferenceEntry.getValue().rightValue().getBindingType()) == IssueBindingType.CLUSTER) {
                                    this.editUserClusterRoleIssues(userSeq, valueDifferenceEntry.getValue().leftValue(), valueDifferenceEntry.getValue().rightValue(), creator);

                                    if (!StringUtils.equals(valueDifferenceEntry.getValue().leftValue().getIssueRole(), valueDifferenceEntry.getValue().rightValue().getIssueRole())) {
                                        editRoles.add(valueDifferenceEntry.getValue().rightValue());
                                    }
                                } else {
                                    this.editUserClusterRoleIssues(userSeq, valueDifferenceEntry.getValue().leftValue(), valueDifferenceEntry.getValue().rightValue(), creator);

                                    if (!CollectionUtils.isEqualCollection(Optional.ofNullable(valueDifferenceEntry.getValue().leftValue().getBindings()).orElseGet(() ->Lists.newArrayList()), Optional.ofNullable(valueDifferenceEntry.getValue().rightValue().getBindings()).orElseGet(() ->Lists.newArrayList()))) {
                                        editRoles.add(valueDifferenceEntry.getValue().rightValue());
                                    }


                                    /**
                                     * 바인딩 유형이 'NAMESPACE'인 경우
                                     * 기존 값과 비교하여 namespace 별로 추가/수정/삭제 를 추려 cluster-api가 처리할 데이터를 생성함.
                                     */
                                    List<UserClusterRoleIssueBindingVO> addBindings = Lists.newArrayList();
                                    List<UserClusterRoleIssueBindingVO> editBindings = Lists.newArrayList();
                                    List<UserClusterRoleIssueBindingVO> removeBindings = Lists.newArrayList();

                                    Map<String, UserClusterRoleIssueBindingVO> currBindingMap = Optional.ofNullable(valueDifferenceEntry.getValue().leftValue().getBindings()).orElseGet(() ->Lists.newArrayList()).stream().collect(Collectors.toMap(UserClusterRoleIssueBindingVO::getNamespace, Function.identity()));
                                    Map<String, UserClusterRoleIssueBindingVO> uptBindingMap = Optional.ofNullable(valueDifferenceEntry.getValue().rightValue().getBindings()).orElseGet(() ->Lists.newArrayList()).stream().collect(Collectors.toMap(UserClusterRoleIssueBindingVO::getNamespace, Function.identity()));

                                    // binding 비교
                                    MapDifference<String, UserClusterRoleIssueBindingVO> bindingDiff = Maps.difference(currBindingMap, uptBindingMap);

                                    // binding 삭제
                                    if (MapUtils.isNotEmpty(bindingDiff.entriesOnlyOnLeft())) {
                                        // 제거할 binding 목록 셋팅
                                        removeBindings.addAll(bindingDiff.entriesOnlyOnLeft().values().stream().collect(Collectors.toList()));

                                        // cluster-api에 보낼 데이터 셋팅
                                        UserClusterRoleIssueVO removeRole = JsonUtils.fromGson(JsonUtils.toGson(valueDifferenceEntry.getValue().rightValue()), UserClusterRoleIssueVO.class);
                                        removeRole.setBindings(removeBindings);
                                        removeRoles.add(removeRole);
                                    }

                                    // binding 추가
                                    if (MapUtils.isNotEmpty(bindingDiff.entriesOnlyOnRight())) {
                                        // 추기할 namespace 목록 셋팅
                                        addBindings.addAll(bindingDiff.entriesOnlyOnLeft().values().stream().collect(Collectors.toList()));

                                        // cluster-api에 보낼 데이터 셋팅
                                        UserClusterRoleIssueVO addRole = JsonUtils.fromGson(JsonUtils.toGson(valueDifferenceEntry.getValue().rightValue()), UserClusterRoleIssueVO.class);
                                        addRole.setBindings(addBindings);
                                        addRoles.add(addRole);
                                    }

                                    // binding 수정
                                    if (MapUtils.isNotEmpty(bindingDiff.entriesDiffering())) {
                                        Map<String, MapDifference.ValueDifference<UserClusterRoleIssueBindingVO>> differenceBindingMap = bindingDiff.entriesDiffering();

                                        // 수정할 binding 목록 셋팅
                                        for (Map.Entry<String, MapDifference.ValueDifference<UserClusterRoleIssueBindingVO>> valueDifferenceBindingEntry : differenceBindingMap.entrySet()) {
                                            editBindings.add(valueDifferenceBindingEntry.getValue().rightValue());
                                        }

                                        // cluster-api에 보낼 데이터 셋팅
                                        UserClusterRoleIssueVO editRole = JsonUtils.fromGson(JsonUtils.toGson(valueDifferenceEntry.getValue().rightValue()), UserClusterRoleIssueVO.class);
                                        editRole.setBindings(editBindings);
                                        editRoles.add(editRole);
                                    }
                                }

                            } else {
                                valueDifferenceEntry.getValue().rightValue().setPreBindingType(valueDifferenceEntry.getValue().leftValue().getBindingType());

                                if (IssueBindingType.valueOf(valueDifferenceEntry.getValue().rightValue().getBindingType()) == IssueBindingType.NAMESPACE) {
                                    valueDifferenceEntry.getValue().rightValue().setIssueRole(null);
                                } else {
                                    valueDifferenceEntry.getValue().rightValue().setBindingConfig(null);
                                    valueDifferenceEntry.getValue().rightValue().setBindings(null);
                                }

                                // 수정
                                this.editUserClusterRoleIssues(userSeq, valueDifferenceEntry.getValue().leftValue(), valueDifferenceEntry.getValue().rightValue(), creator);

                                editRoles.add(valueDifferenceEntry.getValue().rightValue());
                            }
                        }
                    }
                }

                if (CollectionUtils.isNotEmpty(addRoles) || CollectionUtils.isNotEmpty(editRoles) || CollectionUtils.isNotEmpty(removeRoles)) {
                    // cluster-api 권한 수정/삭제 요청
                    clusterApiClient.manageClusterRole(ContextHolder.exeContext().getUserSeq().toString(), ContextHolder.exeContext().getUserRole(), accountSeq, userSeq, issueType, addRoles, editRoles, removeRoles);
                }
            } else {
                this.removeUserClusterRoleIssuesWithClusterApi(accountSeq, userSeq, issueType, currUserClusterRoleIssues, HistoryState.REVOKE, creator);
            }
        }
        // 현재 등록된 권한이 없다면 새로 추가
        else {
            this.addUserClusterRoleIssuesWithClusterApi(accountSeq, userSeq, userId, issueType, uptUserClusterRoleIssues, creator);
        }
    }

    private void initUserClusterRoleIssuesForCompare(List<UserClusterRoleIssueVO> userClusterRoleIssues) throws Exception {
        if (CollectionUtils.isNotEmpty(userClusterRoleIssues)) {
            for (UserClusterRoleIssueVO issueRow : userClusterRoleIssues) {
                issueRow.setCluster(null);
                issueRow.setCreatorId(null);
                issueRow.setCreatorName(null);
                issueRow.setCreator(null);
                issueRow.setCreated(null);
                issueRow.setUpdaterId(null);
                issueRow.setUpdaterName(null);
                issueRow.setUpdater(null);
                issueRow.setUpdated(null);
                issueRow.setIssueDatetime(null);
                issueRow.setAccountSeq(null);
                issueRow.setHistory(null);
                issueRow.setBindingConfig(null);

                if (CollectionUtils.isNotEmpty(issueRow.getBindings())) {
                    for (UserClusterRoleIssueBindingVO bindingRow : issueRow.getBindings()) {
                        bindingRow.setUserSeq(issueRow.getUserSeq());
                        bindingRow.setClusterSeq(issueRow.getClusterSeq());
                        bindingRow.setIssueType(issueRow.getIssueType());
                        bindingRow.setCreator(null);
                        bindingRow.setCreated(null);
                    }
                }
            }
        }
    }

    public Map<Integer, Map<String, UserClusterRoleIssueVO>> convertUserClusterRoleIssueMap(List<UserClusterRoleIssueVO> issues) throws Exception {
        Map<Integer, Map<String, UserClusterRoleIssueVO>> roleMap = Maps.newHashMap();
        for (UserClusterRoleIssueVO row : Optional.ofNullable(issues).orElseGet(() ->Lists.newArrayList())) {
            if (!roleMap.containsKey(row.getClusterSeq())) {
                roleMap.put(row.getClusterSeq(), Maps.newHashMap());
            }

            roleMap.get(row.getClusterSeq()).put(row.getIssueAccountName(), row);
        }

        return roleMap;
    }

    public Map<Integer, List<UserClusterRoleIssueVO>> convertUserClusterRoleIssueList(List<UserClusterRoleIssueVO> issues) throws Exception {
        Map<Integer, List<UserClusterRoleIssueVO>> roleMap = Maps.newHashMap();
        for (UserClusterRoleIssueVO row : Optional.ofNullable(issues).orElseGet(() ->Lists.newArrayList())) {
            if (!roleMap.containsKey(row.getClusterSeq())) {
                roleMap.put(row.getClusterSeq(), Lists.newArrayList());
            }

            roleMap.get(row.getClusterSeq()).add(row);
        }

        return roleMap;
    }

    public void downloadExcelUserClusterRoleIssues(HttpServletResponse response, UserClusterRoleIssueSearchVO params) throws Exception {
        try{
            // excel header 컬럼 순서대로 셋팅
            List<Pair<String, Integer>> headers = Lists.newArrayList();
            headers.add(Pair.of("User", 40));
            if (IssueType.valueOf(params.getIssueType()) == IssueType.KUBECONFIG) {
                headers.add(Pair.of("User Account", 31));
            } else {
                headers.add(Pair.of("Service Account", 31));
            }
            headers.add(Pair.of("Role Binding", 40));
            headers.add(Pair.of("Namespace", 28));
            headers.add(Pair.of("Cluster", 28));
            headers.add(Pair.of("Role", 20));
            headers.add(Pair.of("Role Type", 20));
            headers.add(Pair.of("Issuer", 40));
            headers.add(Pair.of("Creation Date", 22));
            if (IssueType.valueOf(params.getIssueType()) == IssueType.KUBECONFIG) {
                headers.add(Pair.of("Expiration Date", 22));
            }
//
//            headers.add(Pair.of("Access Account", 40));
//            headers.add(Pair.of("Cluster", 28));
//            headers.add(Pair.of("Role", 20));
//            headers.add(Pair.of("Provider", 28));
//            headers.add(Pair.of("Region", 28));
//            if (IssueType.valueOf(params.getIssueType()) == IssueType.KUBECONFIG) {
//                headers.add(Pair.of("User Account", 31));
//            } else {
//                headers.add(Pair.of("Service Account", 31));
//            }
//            headers.add(Pair.of("Issuer", 40));
//            if (IssueType.valueOf(params.getIssueType()) == IssueType.KUBECONFIG) {
//                headers.add(Pair.of("Expiration Date", 22));
//            }
//            headers.add(Pair.of("Creation Date", 22));
            // resultHandler
            UserClusterRoleIssueResultHandler<UserClusterRoleIssueVO> resultHandler = new UserClusterRoleIssueResultHandler(response, String.format("cocktail-%s-issue.xlsx", params.getIssueType()), "list", headers);

            // 조회
            this.getUserClusterRoleIssuesForExcel(params, resultHandler);
            // excel 생성 및 종료
            ExcelUtils.closeAfterWrite(resultHandler.getResponse(), resultHandler.getWorkbook());
        } catch (Exception e) {
            throw new CocktailException("An error occurred while downloading Excel.[UserClusterRoleIssue]", e, ExceptionType.ExcelDownloadFail);
        }
    }

    public void downloadExcelUserClusterRoleIssueHistories(HttpServletResponse response, UserClusterRoleIssueSearchVO params) throws Exception {
        try{
            // excel header 컬럼 순서대로 셋팅
            List<Pair<String, Integer>> headers = Lists.newArrayList();
            headers.add(Pair.of("Issue Date", 22));
            headers.add(Pair.of("Issue State", 11));
            headers.add(Pair.of("User", 40));
            if (IssueType.valueOf(params.getIssueType()) == IssueType.KUBECONFIG) {
                headers.add(Pair.of("User Account", 31));
            } else {
                headers.add(Pair.of("Service Account", 31));
            }
            headers.add(Pair.of("Role Binding", 40));
            headers.add(Pair.of("Namespace", 28));
            headers.add(Pair.of("Cluster", 28));
            headers.add(Pair.of("Role", 20));
            headers.add(Pair.of("Role Type", 20));
            headers.add(Pair.of("Issuer", 40));

//            headers.add(Pair.of("Issue Date", 22));
//            headers.add(Pair.of("Issue State", 11));
//            if (IssueType.valueOf(params.getIssueType()) == IssueType.KUBECONFIG) {
//                headers.add(Pair.of("Expiration Date", 22));
//            }
//            headers.add(Pair.of("Service Account", 31));
//            headers.add(Pair.of("Role Binding", 20));
//            headers.add(Pair.of("Access Account", 40));
//            headers.add(Pair.of("Cluster", 28));
//            headers.add(Pair.of("Issuer", 40));

            // resultHandler
            UserClusterRoleIssueHistoryResultHandler<UserClusterRoleIssueHistoryVO> resultHandler = new UserClusterRoleIssueHistoryResultHandler(response, String.format("cocktail-%s-issue-history.xlsx", params.getIssueType()), "list", headers);
            // 조회
            this.getUserClusterRoleIssueHistoriesForExcel(params, resultHandler);
            // excel 생성 및 종료
            ExcelUtils.closeAfterWrite(resultHandler.getResponse(), resultHandler.getWorkbook());
        } catch (Exception e) {
            throw new CocktailException("An error occurred while downloading Excel.[UserClusterRoleIssueHistory]", e, ExceptionType.ExcelDownloadFail);
        }
    }

    public void downloadExcelUserShellConnectHistories(HttpServletResponse response, UserClusterRoleIssueSearchVO params) throws Exception {
        try{
            // excel header 컬럼 순서대로 셋팅
            List<Pair<String, Integer>> headers = Lists.newArrayList();
            headers.add(Pair.of("Connection Date", 22));
            headers.add(Pair.of("Issue State", 11));
            headers.add(Pair.of("Connection State", 11));
            headers.add(Pair.of("Service Account", 31));
            headers.add(Pair.of("Access Account", 40));
            headers.add(Pair.of("Cluster", 28));
            headers.add(Pair.of("Issuer", 40));
            // resultHandler
            UserShellConnectHistoryResultHandler<UserShellConnectHistoryVO> resultHandler = new UserShellConnectHistoryResultHandler(response, "cocktail-shell-connect-history.xlsx", "list", headers);
            // 조회
            this.getUserShellConnectHistoriesForExcel(params, resultHandler);
            // excel 생성 및 종료
            ExcelUtils.closeAfterWrite(resultHandler.getResponse(), resultHandler.getWorkbook());
        } catch (Exception e) {
            throw new CocktailException("An error occurred while downloading Excel.[UserShellConnectHistory]", e, ExceptionType.ExcelDownloadFail);
        }
    }

    public void downloadExcelUserConfigDownloadHistories(HttpServletResponse response, UserClusterRoleIssueSearchVO params) throws Exception {
        try{
            // excel header 컬럼 순서대로 셋팅
            List<Pair<String, Integer>> headers = Lists.newArrayList();
            headers.add(Pair.of("Download Date", 22));
            headers.add(Pair.of("Issue State", 11));
            headers.add(Pair.of("Expiration Date", 22));
            headers.add(Pair.of("Download State", 11));
            headers.add(Pair.of("User Account", 31));
            headers.add(Pair.of("Access Account", 40));
            headers.add(Pair.of("Cluster", 28));
            headers.add(Pair.of("Issuer", 40));
            // resultHandler
            UserConfigDownloadHistoryResultHandler<UserConfigDownloadHistoryVO> resultHandler = new UserConfigDownloadHistoryResultHandler(response, "cocktail-config-download-history.xlsx", "list", headers);
            // 조회
            this.getUserConfigDownloadHistoriesForExcel(params, resultHandler);
            // excel 생성 및 종료
            ExcelUtils.closeAfterWrite(resultHandler.getResponse(), resultHandler.getWorkbook());
        } catch (Exception e) {
            throw new CocktailException("An error occurred while downloading Excel.[UserConfigDownloadHistory]", e, ExceptionType.ExcelDownloadFail);
        }
    }

    /**
     * user_shell_connect_history 입력
     * @param userShellConnectHistory
     * @return
     * @throws Exception
     */
    public UserShellConnectHistoryVO addUserShellConnectHistory(UserShellConnectHistoryVO userShellConnectHistory) throws Exception {
        IUserMapper dao = sqlSession.getMapper(IUserMapper.class);
        List<UserShellConnectHistoryVO> userShellConnectHistories = new ArrayList<>();
        userShellConnectHistories.add(userShellConnectHistory);

        int result = dao.addUserShellConnectHistory(userShellConnectHistories);
        return userShellConnectHistory;
    }

    /**
     * user_shell_connect_history 입력
     * @param userShellConnectHistories
     * @return
     * @throws Exception
     */
    public List<UserShellConnectHistoryVO> addUserShellConnectHistories(List<UserShellConnectHistoryVO> userShellConnectHistories) throws Exception {
        IUserMapper dao = sqlSession.getMapper(IUserMapper.class);
        int result = dao.addUserShellConnectHistory(userShellConnectHistories);
        return userShellConnectHistories;
    }

    /**
     * user_shell_connect_history_list 조회
     * @param userSeq
     * @param order
     * @param orderColumn
     * @param nextPage
     * @param itemPerPage
     * @param searchColumn
     * @param searchKeyword
     * @param maxId
     * @param newId
     * @param startDate
     * @param endDate
     * @return
     * @throws Exception
     */
    public UserShellConnectHistoryListVO getUserShellConnectHistories(Integer accountSeq, Integer userSeq, Integer clusterSeq, String historyState, String order, String orderColumn, Integer nextPage, Integer itemPerPage, String searchColumn, String searchKeyword, String maxId, String newId, String startDate, String endDate) throws Exception {
        UserShellConnectHistoryListVO userShellConnectHistoryList = new UserShellConnectHistoryListVO();
        try {
            UserClusterRoleIssueSearchVO params = this.setUserClusterRoleCommonParams(accountSeq, userSeq, clusterSeq, IssueType.SHELL.getCode(), historyState, searchColumn, searchKeyword, startDate, endDate, false);
            ListCountVO listCount = this.getUserShellConnectHistoriesCountAndMaxId(params);
            PagingVO paging = PagingUtils.setPagingParams(orderColumn, order, nextPage, itemPerPage, maxId, listCount);
            params.setPaging(paging);
            /** Params Setting Completed **/

            List<UserShellConnectHistoryVO> userShellConnectHistories = this.getUserShellConnectHistories(params);

            userShellConnectHistoryList.setHistories(userShellConnectHistories);
            userShellConnectHistoryList.setTotalCount(params.getPaging().getListCount().getCnt());
            userShellConnectHistoryList.setMaxId(params.getPaging().getMaxId());
            userShellConnectHistoryList.setCurrentPage(nextPage);
        }
        catch (Exception ex) {
            if(ex instanceof CocktailException) {
                throw ex;
            }
            throw new CocktailException("User Shell Connect History List Inquire Failure", ex, ExceptionType.CommonInquireFail);
        }

        return userShellConnectHistoryList;
    }

    /**
     * user_shell_connect_history_list 조회.
     * @param params
     * @return
     * @throws Exception
     */
    public List<UserShellConnectHistoryVO> getUserShellConnectHistories(UserClusterRoleIssueSearchVO params) throws Exception {
        IUserMapper dao = sqlSession.getMapper(IUserMapper.class);
        // get Lists
        return dao.getUserShellConnectHistories(params);
    }


    /**
     * user_shell_connect_history 갯수 & connect_seq 기준 maxId 조회
     * @param params
     * @return
     * @throws Exception
     */
    public ListCountVO getUserShellConnectHistoriesCountAndMaxId(UserClusterRoleIssueSearchVO params) throws Exception {
        IUserMapper dao = sqlSession.getMapper(IUserMapper.class);
        // get Audit Log Count
        return dao.getUserShellConnectHistoriesCountAndMaxId(params);
    }


    /**
     * user_config_download_history  입력
     * @param userConfigDownloadHistory
     * @return
     * @throws Exception
     */
    public UserConfigDownloadHistoryVO addUserConfigDownloadHistory(UserConfigDownloadHistoryVO userConfigDownloadHistory) throws Exception {
        IUserMapper dao = sqlSession.getMapper(IUserMapper.class);
        List<UserConfigDownloadHistoryVO> userConfigDownloadHistories = new ArrayList<>();
        userConfigDownloadHistories.add(userConfigDownloadHistory);

        int result = dao.addUserConfigDownloadHistory(userConfigDownloadHistories);
        return userConfigDownloadHistory;
    }

    /**
     * user_config_download_history  입력 (리스트 입력)
     * @param userConfigDownloadHistories
     * @return
     * @throws Exception
     */
    public List<UserConfigDownloadHistoryVO> addUserConfigDownloadHistories(List<UserConfigDownloadHistoryVO> userConfigDownloadHistories) throws Exception {
        IUserMapper dao = sqlSession.getMapper(IUserMapper.class);

        int result = dao.addUserConfigDownloadHistory(userConfigDownloadHistories);
        return userConfigDownloadHistories;
    }

    /**
     * user_config_download_history_list 조회
     * @param userSeq
     * @param order
     * @param orderColumn
     * @param nextPage
     * @param itemPerPage
     * @param searchColumn
     * @param searchKeyword
     * @param maxId
     * @param newId
     * @param startDate
     * @param endDate
     * @return
     * @throws Exception
     */
    public UserConfigDownloadHistoryListVO getUserConfigDownloadHistories(Integer accountSeq, Integer userSeq, Integer clusterSeq, String historyState, String order, String orderColumn, Integer nextPage, Integer itemPerPage, String searchColumn, String searchKeyword, String maxId, String newId, String startDate, String endDate) throws Exception {
        UserConfigDownloadHistoryListVO userConfigDownloadHistoryList = new UserConfigDownloadHistoryListVO();
        try {
            UserClusterRoleIssueSearchVO params = this.setUserClusterRoleCommonParams(accountSeq, userSeq, clusterSeq, IssueType.KUBECONFIG.getCode(), historyState, searchColumn, searchKeyword, startDate, endDate, false);
            ListCountVO listCount = this.getUserConfigDownloadHistoriesCountAndMaxId(params);
            PagingVO paging = PagingUtils.setPagingParams(orderColumn, order, nextPage, itemPerPage, maxId, listCount);
            params.setPaging(paging);
            /** Params Setting Completed **/

            List<UserConfigDownloadHistoryVO> userConfigDownloadHistories = this.getUserConfigDownloadHistories(params);

            userConfigDownloadHistoryList.setHistories(userConfigDownloadHistories);
            userConfigDownloadHistoryList.setTotalCount(params.getPaging().getListCount().getCnt());
            userConfigDownloadHistoryList.setMaxId(params.getPaging().getMaxId());
            userConfigDownloadHistoryList.setCurrentPage(nextPage);
        }
        catch (Exception ex) {
            if(ex instanceof CocktailException) {
                throw ex;
            }
            if(log.isDebugEnabled()) log.debug("trace log ", ex);
            throw new CocktailException("User Config Download History List Inquire Failure", ex, ExceptionType.CommonInquireFail);
        }

        return userConfigDownloadHistoryList;
    }

    /**
     * user_config_download_history_list 조회.
     * @param params
     * @return
     * @throws Exception
     */
    public List<UserConfigDownloadHistoryVO> getUserConfigDownloadHistories(UserClusterRoleIssueSearchVO params) throws Exception {
        IUserMapper dao = sqlSession.getMapper(IUserMapper.class);
        // get Lists
        return dao.getUserConfigDownloadHistories(params);
    }


    /**
     * user_config_download_history 갯수 & connect_seq 기준 maxId 조회
     * @param params
     * @return
     * @throws Exception
     */
    public ListCountVO getUserConfigDownloadHistoriesCountAndMaxId(UserClusterRoleIssueSearchVO params) throws Exception {
        IUserMapper dao = sqlSession.getMapper(IUserMapper.class);
        // get Audit Log Count
        return dao.getUserConfigDownloadHistoriesCountAndMaxId(params);
    }

    /**
     * user_cluster_role_issue_history_list 조회
     *
     * @param accountSeq
     * @param userSeq
     * @param clusterSeq
     * @param issueType
     * @param historyState
     * @param order
     * @param orderColumn
     * @param nextPage
     * @param itemPerPage
     * @param searchColumn
     * @param searchKeyword
     * @param maxId
     * @param newId
     * @param startDate
     * @param endDate
     * @return
     * @throws Exception
     */
    public UserClusterRoleIssueHistoryListVO getUserClusterRoleIssueHistories(Integer accountSeq, Integer userSeq, Integer clusterSeq, String issueType, String historyState, String order, String orderColumn, Integer nextPage, Integer itemPerPage, String searchColumn, String searchKeyword, String maxId, String newId, String startDate, String endDate) throws Exception {
        UserClusterRoleIssueHistoryListVO userClusterRoleIssueHistoryList = new UserClusterRoleIssueHistoryListVO();
        try {
            UserClusterRoleIssueSearchVO params = this.setUserClusterRoleCommonParams(accountSeq, userSeq, clusterSeq, issueType, historyState, searchColumn, searchKeyword, startDate, endDate, false);
            ListCountVO listCount = this.getUserClusterRoleIssueHistoriesCountAndMaxId(params);
            PagingVO paging = PagingUtils.setPagingParams(orderColumn, order, nextPage, itemPerPage, maxId, listCount);
            params.setPaging(paging);
            /** Params Setting Completed **/

            List<UserClusterRoleIssueHistoryVO> userClusterRoleIssueHistories = this.getUserClusterRoleIssueHistories(params);

            userClusterRoleIssueHistoryList.setHistories(userClusterRoleIssueHistories);
            userClusterRoleIssueHistoryList.setTotalCount(params.getPaging().getListCount().getCnt());
            userClusterRoleIssueHistoryList.setMaxId(params.getPaging().getMaxId());
            userClusterRoleIssueHistoryList.setCurrentPage(nextPage);
        }
        catch (Exception ex) {
            if(ex instanceof CocktailException) {
                throw ex;
            }
            if(log.isDebugEnabled()) log.debug("trace log ", ex);
            throw new CocktailException("User Cluster Role Issue History List Inquire Failure", ex, ExceptionType.CommonInquireFail);
        }

        return userClusterRoleIssueHistoryList;
    }

    /**
     * user_cluster_role_issue_history_list 조회.
     * @param params
     * @return
     * @throws Exception
     */
    public List<UserClusterRoleIssueHistoryVO> getUserClusterRoleIssueHistories(UserClusterRoleIssueSearchVO params) throws Exception {
        IUserMapper dao = sqlSession.getMapper(IUserMapper.class);
        // get Lists
        return dao.getUserClusterRoleIssueHistories(params);
    }

    /**
     * user_cluster_role_issue_history 갯수 & connect_seq 기준 maxId 조회
     * @param params
     * @return
     * @throws Exception
     */
    public ListCountVO getUserClusterRoleIssueHistoriesCountAndMaxId(UserClusterRoleIssueSearchVO params) throws Exception {
        IUserMapper dao = sqlSession.getMapper(IUserMapper.class);
        // get Audit Log Count
        return dao.getUserClusterRoleIssueHistoriesCountAndMaxId(params);
    }

    /**
     * 최근 user_cluster_role_issue_history 조회
     *
     * @param accountSeq
     * @param clusterSeq
     * @param issueType
     * @param issueAccountName
     * @return
     * @throws Exception
     */
    public UserClusterRoleIssueHistoryVO getLatestUserClusterRoleIssueHistory(Integer accountSeq, Integer clusterSeq, String issueType, String issueAccountName) throws Exception {
        UserClusterRoleIssueHistoryVO userClusterRoleIssue = null;
        try {
            IUserMapper userDao = sqlSession.getMapper(IUserMapper.class);
            UserClusterRoleIssueSearchVO params = new UserClusterRoleIssueSearchVO();
            params.setAccountSeq(accountSeq);
            params.setClusterSeq(clusterSeq);
            params.setIssueType(issueType);
            params.setSearchColumn("ACCOUNT_NAME");
            params.setSearchKeyword(issueAccountName);

            userClusterRoleIssue = userDao.getLatestUserClusterRoleIssueHistory(params);

        }
        catch (Exception ex) {
            if(ex instanceof CocktailException) {
                throw ex;
            }
            if(log.isDebugEnabled()) log.debug("trace log ", ex);
            throw new CocktailException("User Cluster Role Issue History Inquire Failure", ex, ExceptionType.CommonInquireFail);
        }

        return userClusterRoleIssue;
    }

    /**
     * user_cluster_role_issue_list 조회
     * @param accountSeq
     * @param userSeq
     * @param issueType
     * @param order
     * @param orderColumn
     * @param nextPage
     * @param itemPerPage
     * @param searchColumn
     * @param searchKeyword
     * @param startDate
     * @param endDate
     * @return
     * @throws Exception
     */
    public UserClusterRoleIssueListVO getUserClusterRoleIssueList(Integer accountSeq, Integer userSeq, Integer clusterSeq, String issueType, String order, String orderColumn, Integer nextPage, Integer itemPerPage, String searchColumn, String searchKeyword, String startDate, String endDate) throws Exception {
        UserClusterRoleIssueListVO userClusterRoleIssueList = new UserClusterRoleIssueListVO();
        try {
            UserClusterRoleIssueSearchVO params = this.setUserClusterRoleCommonParams(accountSeq, userSeq, clusterSeq, issueType, null, searchColumn, searchKeyword, startDate, endDate, false);
            ListCountVO listCount = this.getUserClusterRoleIssueCount(params);
            PagingVO paging = PagingUtils.setPagingParams(orderColumn, order, nextPage, itemPerPage, null, listCount);
            params.setPaging(paging);
            /** Params Setting Completed **/

            List<UserClusterRoleIssueVO> userClusterRoleIssues = this.getUserClusterRoleIssueList(params);

            userClusterRoleIssueList.setItems(userClusterRoleIssues);
            userClusterRoleIssueList.setTotalCount(params.getPaging().getListCount().getCnt());
            userClusterRoleIssueList.setCurrentPage(nextPage);
        }
        catch (Exception ex) {
            if(ex instanceof CocktailException) {
                throw ex;
            }
            if(log.isDebugEnabled()) log.debug("trace log ", ex);
            throw new CocktailException("User Cluster Role Issue List Inquire Failure", ex, ExceptionType.CommonInquireFail);
        }

        return userClusterRoleIssueList;
    }

    /**
     * user_cluster_role_issue_list 조회.
     * @param params
     * @return
     * @throws Exception
     */
    public List<UserClusterRoleIssueVO> getUserClusterRoleIssueList(UserClusterRoleIssueSearchVO params) throws Exception {
        IUserMapper dao = sqlSession.getMapper(IUserMapper.class);
        // get Lists
        return dao.getUserClusterRoleIssueList(params);
    }

    /**
     * user_cluster_role_issue 갯수 조회
     * @param params
     * @return
     * @throws Exception
     */
    public ListCountVO getUserClusterRoleIssueCount(UserClusterRoleIssueSearchVO params) throws Exception {
        IUserMapper dao = sqlSession.getMapper(IUserMapper.class);
        // get Audit Log Count
        return dao.getUserClusterRoleIssueCount(params);
    }

    public List<UserClusterRoleIssueVO> getUserClusterRoleIssues(Integer accountSeq, Integer clusterSeq, Integer userSeq, String issueType, String expirationDatetime, String userTimezone, boolean isUserBased) throws Exception {
        IUserMapper userDao = sqlSession.getMapper(IUserMapper.class);
        return userDao.getUserClusterRoleIssues(accountSeq, clusterSeq, null, userSeq, issueType, expirationDatetime, userTimezone, isUserBased);
    }

    public List<UserClusterRoleIssueVO> getUserClusterRoleIssues(Integer accountSeq, List<Integer> clusterSeqs, Integer userSeq, String issueType, String expirationDatetime, String userTimezone, boolean isUserBased) throws Exception {
        IUserMapper userDao = sqlSession.getMapper(IUserMapper.class);
        return userDao.getUserClusterRoleIssues(accountSeq, null, clusterSeqs, userSeq, issueType, expirationDatetime, userTimezone, isUserBased);
    }

    public UserClusterRoleIssueVO getUserClusterRoleIssue(Integer userSeq, Integer clusterSeq, String issueType, String issueAccountName) throws Exception {
        IUserMapper userDao = sqlSession.getMapper(IUserMapper.class);
        return userDao.getUserClusterRoleIssue(userSeq, clusterSeq, issueType, issueAccountName, null);
    }

    /**
     * Excel 다운로드를 위해 resultHandler 를 이용한 UserClusterRoleIssueHistories 조회
     *
     * @param params
     * @param resultHandler
     * @throws Exception
     */
    public void getUserClusterRoleIssuesForExcel(UserClusterRoleIssueSearchVO params, ResultHandler<UserClusterRoleIssueVO> resultHandler) throws Exception {
        sqlSession.select(String.format("%s.%s", IUserMapper.class.getName(), "getUserClusterRoleIssuesForExcel"), params, resultHandler);
    }

    /**
     * Excel 다운로드를 위해 resultHandler 를 이용한 UserClusterRoleIssues 조회
     *
     * @param params
     * @param resultHandler
     * @throws Exception
     */
    public void getUserClusterRoleIssueHistoriesForExcel(UserClusterRoleIssueSearchVO params, ResultHandler<UserClusterRoleIssueHistoryVO> resultHandler) throws Exception {
        sqlSession.select(String.format("%s.%s", IUserMapper.class.getName(), "getUserClusterRoleIssueHistoriesForExcel"), params, resultHandler);
    }

    /**
     * Excel 다운로드를 위해 resultHandler 를 이용한 UserShellConnectHistories 조회
     *
     * @param params
     * @param resultHandler
     * @throws Exception
     */
    public void getUserShellConnectHistoriesForExcel(UserClusterRoleIssueSearchVO params, ResultHandler<UserShellConnectHistoryVO> resultHandler) throws Exception {
        sqlSession.select(String.format("%s.%s", IUserMapper.class.getName(), "getUserShellConnectHistoriesForExcel"), params, resultHandler);
    }

    /**
     * Excel 다운로드를 위해 resultHandler 를 이용한 UserConfigDownloadHistories 조회
     *
     * @param params
     * @param resultHandler
     * @throws Exception
     */
    public void getUserConfigDownloadHistoriesForExcel(UserClusterRoleIssueSearchVO params, ResultHandler<UserConfigDownloadHistoryVO> resultHandler) throws Exception {
        sqlSession.select(String.format("%s.%s", IUserMapper.class.getName(), "getUserConfigDownloadHistoriesForExcel"), params, resultHandler);
    }

    /**
     * Set 클러스터 접속 관리 관련 params
     *
     * @param accountSeq
     * @param userSeq
     * @param issueType
     * @param historyState
     * @param searchColumn
     * @param searchKeyword
     * @param startDate
     * @param endDate
     * @return
     * @throws Exception
     */
    public UserClusterRoleIssueSearchVO setUserClusterRoleCommonParams(Integer accountSeq, Integer userSeq, Integer clusterSeq, String issueType, String historyState, String searchColumn, String searchKeyword, String startDate, String endDate, boolean isUserBased) throws Exception {
        UserClusterRoleIssueSearchVO userClusterRoleIssueSearch = new UserClusterRoleIssueSearchVO();

        // 검색 기간
//        if(StringUtils.isBlank(startDate) || StringUtils.isBlank(endDate)) { // 둘중 하나라도 날짜가 없으면..
//            if(StringUtils.isNotBlank(startDate) && StringUtils.isBlank(endDate)) {
//                // startDate만 존재하면 endDate를 최신으로 설정하여 조회함. (Query에서 DATETIME 필드의 Between 조건 사용)
//                Date dEndDate = new Date();
//                endDate = DateTimeUtils.getUtcTimeString(dEndDate, DateTimeUtils.DEFAULT_DB_DATE_FORMAT);
//            }
//            else if(StringUtils.isBlank(startDate) && StringUtils.isNotBlank(endDate)) {
//                // endDate만 존재하면 startDate를 먼 과거로 설정하여 조회함. (Query에서 DATETIME 필드의 Between 조건 사용)
//                Date dEndDate = new Date();
//                Calendar calendar = Calendar.getInstance();
//                calendar.setTime(dEndDate);
//                calendar.add(Calendar.DATE, -7);
//                Date dStartDate = calendar.getTime();
//                startDate = DateTimeUtils.getUtcTimeString(dStartDate, DateTimeUtils.DEFAULT_DB_DATE_FORMAT);
//            }
//            else {
////                Date dEndDate = new Date();
////                Calendar calendar = Calendar.getInstance();
////                calendar.setTime(dEndDate);
////                calendar.add(Calendar.DATE, -7);
////
////                Date dStartDate = calendar.getTime();
////                startDate = DateTimeUtils.getUtcTimeString(dStartDate, DateTimeUtils.DEFAULT_DB_DATE_FORMAT);
////                endDate = DateTimeUtils.getUtcTimeString(dEndDate, DateTimeUtils.DEFAULT_DB_DATE_FORMAT);
//            }
//        }

        userClusterRoleIssueSearch.setStartDate(startDate);
        userClusterRoleIssueSearch.setEndDate(endDate);

        // 검색조건 있으면 추가
        if(StringUtils.isNotBlank(searchColumn) && StringUtils.isNotBlank(searchKeyword)) {
            userClusterRoleIssueSearch.setSearchColumn(searchColumn);
            userClusterRoleIssueSearch.setSearchKeyword(searchKeyword);
        }

        // 발급유형 입력이 있으면 설정..
        if(StringUtils.isNotBlank(issueType)) {
            userClusterRoleIssueSearch.setIssueType(issueType);
        }

        // 발급상태 입력이 있으면 설정..
        if(StringUtils.isNotBlank(historyState)) {
            userClusterRoleIssueSearch.setHistoryState(historyState);
        }


        if (clusterSeq != null && clusterSeq > 0) {
            userClusterRoleIssueSearch.setClusterSeq(clusterSeq);
        }

        if (accountSeq != null && accountSeq > 0) {
            userClusterRoleIssueSearch.setAccountSeq(accountSeq);
        }

        // 사용자 정보 입력이 있으면 추가
        if(userSeq != null && userSeq >= 1) {
            userClusterRoleIssueSearch.setUserSeq(userSeq);
        }

        // 사용자 정보 timezone 셀정
        userClusterRoleIssueSearch.setUserTimezone(ContextHolder.exeContext().getUserTimezone());

        // System 사용자는 권한이 있는 System에 해당하는 이력만 조회 가능.
        if (UserRole.valueOf(ContextHolder.exeContext().getUserRole()).isUserOfSystem()) {
            userClusterRoleIssueSearch.setSystemUserSeq(ContextHolder.exeContext().getUserSeq());
        }

        if(isUserBased) {
            userClusterRoleIssueSearch.setIsUserBased(Boolean.TRUE);
        }
        else {
            userClusterRoleIssueSearch.setIsUserBased(Boolean.FALSE);
        }


        return userClusterRoleIssueSearch;
    }

    /**
     * <pre>
     * - 클러스터 사용자 권한 발급시 해당 클러스터가 사용자에게 부여되어 있는 지 체크
     * - kubeconfig, shell 기능이 지원 가능한 지 체크
     * </pre>
     *
     * @param currUserClusterMap
     * @param user
     * @throws Exception
     */
    public void checkUserClusterForIssueRole(Map<Integer, ClusterVO> currUserClusterMap, UserVO user) throws Exception {
        if (MapUtils.isEmpty(currUserClusterMap)) {
            currUserClusterMap = Maps.newHashMap();
        }
        if (CollectionUtils.isNotEmpty(user.getShellRoles()) || CollectionUtils.isNotEmpty(user.getKubeconfigRoles())) {
            // 중복제거
            Set<Integer> userShellClusterSeqSet = Optional.ofNullable(user.getShellRoles()).orElseGet(() ->Lists.newArrayList()).stream().map(UserClusterRoleIssueVO::getClusterSeq).collect(Collectors.toSet());
            Set<Integer> userKubeconfigClusterSeqSet = Optional.ofNullable(user.getKubeconfigRoles()).orElseGet(() ->Lists.newArrayList()).stream().map(UserClusterRoleIssueVO::getClusterSeq).collect(Collectors.toSet());
            // cast from set to list
            List<Integer> userShellClusterSeqs = Lists.newArrayList(userShellClusterSeqSet);
            List<Integer> userKubeconfigClusterSeqs = Lists.newArrayList(userKubeconfigClusterSeqSet);

            List<Integer> currUserClusterSeqs = Lists.newArrayList();
            for (Map.Entry<Integer, ClusterVO> entry : currUserClusterMap.entrySet()) {
                // 사용자가 할당 가능한 clusterSeq 정보 셋팅
                currUserClusterSeqs.add(entry.getKey());

                // kubeconfig, shell 기능이 지원 가능한 지 체크
                if (entry.getValue() != null) {
                    if (entry.getValue().getCubeType().isKubeconfigNotSupported()) {
                        throw new CocktailException("This cluster does not support issuing kubeconfig accounts.", ExceptionType.CommonNotSupported);
                    }
                    if (entry.getValue().getCubeType().isShellNotSupported()) {
                        throw new CocktailException("This cluster does not support issuing shell accounts.", ExceptionType.CommonNotSupported);
                    }
                }
            }

            if (CollectionUtils.isNotEmpty(ListUtils.subtract(userShellClusterSeqs, currUserClusterSeqs)) || CollectionUtils.isNotEmpty(ListUtils.subtract(userKubeconfigClusterSeqs, currUserClusterSeqs))) {
                throw new CocktailException("Cocktail Shell / Kubeconfig Access is not authorization.", ExceptionType.InvalidInputData);
            }
        }
    }

    /**
     * 클러스터 인증서 권한 발급시 만료일 체크
     *
     * @param currUser
     * @param uptUser
     * @throws Exception
     */
    public void checkUserClusterIssueRoleForExpirationDate(UserVO currUser, UserVO uptUser) throws Exception {
        // Kubeconfig 만료일 < (발급일 + 364)
        if (uptUser != null && CollectionUtils.isNotEmpty(uptUser.getKubeconfigRoles())) {

            // 현재 사용자의 kubeconfig role정보를 Map<clusterSeq, UserClusterRoleIssueVO> 셋팅
            Map<Integer, Map<String, UserClusterRoleIssueVO>> currKubeconfigRoleMap = Maps.newHashMap();
            for (UserClusterRoleIssueVO issueRow : Optional.ofNullable(Optional.ofNullable(currUser).orElseGet(() ->new UserVO()).getKubeconfigRoles()).orElseGet(() ->Lists.newArrayList())) {
                if (!currKubeconfigRoleMap.containsKey(issueRow.getClusterSeq())) {
                    currKubeconfigRoleMap.put(issueRow.getClusterSeq(), Maps.newHashMap());
                }

                currKubeconfigRoleMap.get(issueRow.getClusterSeq()).put(issueRow.getIssueAccountName(), issueRow);
            }

            // 신규일 경우 issueDate를 오늘로 설정하여 최대 만료일을 오늘포함 + 365로 설정하여 치크
            DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate toDate = LocalDate.now();
            LocalDate endDate = null; // 최대 설정가능한 만료일
            LocalDate issueDate = null; // 발급일
            LocalDate expirationDate = null; // 만료일

            boolean isAddRole = false;
            String errMsg = "";

            for (UserClusterRoleIssueVO issueRow : uptUser.getKubeconfigRoles()) {
                isAddRole = false;

                if (StringUtils.isBlank(issueRow.getExpirationDatetime())) {
                    // 만료일자가 null이면 무기한 처리
//                    throw new CocktailException("Expiration date is required.", ExceptionType.InvalidParameter_Empty, "Expiration date is required.");

                    // 만료일자가 빈값일 경우가 있을 수 있어 명시적으로 null 처리함
                    issueRow.setExpirationDatetime(null);

                    // 만료일자를 설정/미설정 간 수정 불가
                   if (StringUtils.isNotBlank(issueRow.getIssueAccountName())) {
                        UserClusterRoleIssueVO currIssue = Optional.ofNullable(currKubeconfigRoleMap.get(issueRow.getClusterSeq())).orElseGet(() ->Maps.newHashMap()).get(issueRow.getIssueAccountName());

                        if (currIssue != null && StringUtils.isNotBlank(currIssue.getExpirationDatetime())) {
                            throw new CocktailException("Expiration date cannot be modified.", ExceptionType.InvalidParameter, "Expiration date cannot be modified.");
                        }
                    }

                    continue;
                } else {
                    // 날짜 포맷 체크 및 만료일 LocalDate로 변환
                    try {
                        expirationDate = LocalDate.parse(issueRow.getExpirationDatetime());
                    } catch (Exception e) {
                        throw new CocktailException("expirationDatetime format is invalid."
                                , ExceptionType.InvalidParameter_DateFormat
                                , String.format("The format of the expiration date[%s] is invalid.", issueRow.getExpirationDatetime()));
                    }

                    // 만료 기간 체크
                    // 수정
                    if (currUser != null) {
                        // 기 부여된 권한이면 발급일과 비교
                        if (MapUtils.isNotEmpty(currKubeconfigRoleMap)
                                && MapUtils.getObject(currKubeconfigRoleMap, issueRow.getClusterSeq(), null) != null
                                && MapUtils.getObject(currKubeconfigRoleMap.get(issueRow.getClusterSeq()), issueRow.getIssueAccountName(), null) != null
                        ) {
                            String issueDatetimeFormat = "yyyy-MM-dd HH:mm:ss";
                            if (StringUtils.indexOf(MapUtils.getObject(currKubeconfigRoleMap, issueRow.getClusterSeq()).get(issueRow.getIssueAccountName()).getIssueDatetime(), ".") == 19) {
                                issueDatetimeFormat = "yyyy-MM-dd HH:mm:ss.S";
                            }
                            issueDate = LocalDateTime.parse(MapUtils.getObject(currKubeconfigRoleMap, issueRow.getClusterSeq()).get(issueRow.getIssueAccountName()).getIssueDatetime(), DateTimeFormatter.ofPattern(issueDatetimeFormat)).toLocalDate();
                            endDate = issueDate.plusDays(364); // 발급일 + 364
                            if (expirationDate.isAfter(endDate)) {
                                errMsg = "The expiration date cannot be set beyond 365 days, including the issue date.";
                                throw new CocktailException(errMsg
                                        , ExceptionType.InvalidParameter
                                        , String.format("%s Expiration date : %s", errMsg, endDate.format(dateFormat)));
                            }
                            // 만료 기간이 발급일 이전인지 체크
                            if (expirationDate.isBefore(issueDate)) {
                                errMsg = "The expiration date cannot be set before the issue date.";
                                throw new CocktailException(errMsg
                                        , ExceptionType.InvalidParameter
                                        , String.format("%s Expiration date : %s", errMsg, endDate.format(dateFormat)));
                            }
                        } else {
                            isAddRole = true;
                        }
                    }
                    // 신규
                    else {
                        isAddRole = true;
                    }

                    if (isAddRole) {
                        endDate = toDate.plusDays(364); // 현재 + 364
                        // 만료 기간이 365일(오늘포함)보다 이후인지 체크
                        if (expirationDate.isAfter(endDate)) {
                            errMsg = "The expiration date cannot be set beyond 365 days, including today.";
                            throw new CocktailException(errMsg
                                    , ExceptionType.InvalidParameter
                                    , String.format("%s Expiration date : %s", errMsg, endDate.format(dateFormat)));
                        }
                    }

                    // 만료 기간이 오늘 이전인지 체크
                    if (expirationDate.isBefore(toDate)) {
                        errMsg = "The expiration date cannot be set before today.";
                        throw new CocktailException(errMsg
                                , ExceptionType.InvalidParameter
                                , String.format("%s Expiration date : %s", errMsg, endDate.format(dateFormat)));
                    }
                }
            }
        }
    }


    @Transactional(transactionManager = "transactionManager")
    public void moveUserClusterRoleIssues(Integer targerUserSeq, List<UserVO> sourceUsers, Integer updater) throws Exception {
        if (targerUserSeq != null && CollectionUtils.isNotEmpty(sourceUsers)) {
            IUserMapper userDao = sqlSession.getMapper(IUserMapper.class);
            UserVO targetUser = userDao.getUserOfAccount(ContextHolder.exeContext().getUserAccountSeq(), targerUserSeq);

            if (targetUser != null) {
                for (UserVO sourceUser : sourceUsers) {
                    if (CollectionUtils.isNotEmpty(sourceUser.getShellRoles())) {
                        this.moveUserClusterRoleIssueProcess(targerUserSeq, sourceUser.getUserSeq(), sourceUser.getShellRoles(), updater, userDao);
                    }
                    if (CollectionUtils.isNotEmpty(sourceUser.getKubeconfigRoles())) {
                        this.moveUserClusterRoleIssueProcess(targerUserSeq, sourceUser.getUserSeq(), sourceUser.getKubeconfigRoles(), updater, userDao);
                    }
                }
            } else {
                throw new CocktailException("Invalid target user!!", ExceptionType.InvalidInputData, "Target user does not exist.");
            }
        }
    }

    public void deleteAllUserClusterRoleIssueInfoByCluster(Integer clusterSeq) throws Exception {
        int result = 0;
        IUserMapper dao = sqlSession.getMapper(IUserMapper.class);

        if(clusterSeq != null){
            String baseLogFormat = String.format("############################### DELETE_USER_CLUSTER_ROLE_ISSUE ##### - cluster: [%d], updater: [%d, %s]", clusterSeq, ContextHolder.exeContext().getUserSeq(), ContextHolder.exeContext().getUserRole());

            result = dao.deleteUserClusterRoleIssueBindingHistoryByCluster(clusterSeq);
            log.info("{}, {}: {}", baseLogFormat, "deleteUserClusterRoleIssueBindingHistoryByCluster", result);

            result = dao.deleteUserClusterRoleIssueHistoryByCluster(clusterSeq);
            log.info("{}, {}: {}", baseLogFormat, "deleteUserClusterRoleIssueHistoryByCluster", result);

            result = dao.deleteUserConfigDownloadHistoryByCluster(clusterSeq);
            log.info("{}, {}: {}", baseLogFormat, "deleteUserConfigDownloadHistoryByCluster", result);

            result = dao.deleteUserShellConnectHistoryByCluster(clusterSeq);
            log.info("{}, {}: {}", baseLogFormat, "deleteUserShellConnectHistoryByCluster", result);

            result = dao.deleteUserClusterRoleIssueBindingByCluster(clusterSeq);
            log.info("{}, {}: {}", baseLogFormat, "deleteUserClusterRoleIssueBindingByCluster", result);

            result = dao.deleteUserClusterRoleIssueByCluster(clusterSeq);
            log.info("{}, {}: {}", baseLogFormat, "deleteUserClusterRoleIssueByCluster", result);
        }
    }

    private void moveUserClusterRoleIssueProcess(Integer targerUserSeq, Integer sourceUserSeq, List<UserClusterRoleIssueVO> sourceIssues, Integer updater, IUserMapper userDao) throws Exception {
        if (CollectionUtils.isNotEmpty(sourceIssues)) {
            if (userDao == null) {
                userDao = sqlSession.getMapper(IUserMapper.class);
            }
            for (UserClusterRoleIssueVO issueRow : sourceIssues) {
                // UserClusterRoleIssue 사용자 이관
                userDao.moveUserClusterRoleIssue(targerUserSeq, sourceUserSeq, issueRow.getClusterSeq(), issueRow.getIssueType(), issueRow.getIssueAccountName(), updater);
                // UserClusterRoleIssueBinding 사용자 이관
                userDao.moveUserClusterRoleIssueBindings(targerUserSeq, sourceUserSeq, issueRow.getClusterSeq(), issueRow.getIssueType(), issueRow.getIssueAccountName(), updater);

                // Source UserClusterRoleIssueHistory 'REVOKE' 처리
                UserClusterRoleIssueHistoryVO history = new UserClusterRoleIssueHistoryVO();
                history.setUserSeq(sourceUserSeq);
                history.setClusterSeq(issueRow.getClusterSeq());
                history.setIssueType(issueRow.getIssueType());
                history.setBindingType(issueRow.getBindingType());
                history.setIssueRole(issueRow.getIssueRole());
                history.setIssueAccountName(issueRow.getIssueAccountName());
                history.setIssueUserSeq(updater);
                history.setHistoryState(HistoryState.REVOKE.getCode());

                // history 추가
                userDao.addUserClusterRoleIssueHistory(history);

                // binding history 추가
                if (CollectionUtils.isNotEmpty(issueRow.getBindings())) {
                    userDao.addUserClusterRoleIssueBindingHistories(history.getHistorySeq(), issueRow.getBindings(), updater);
                }

                // Target UserClusterRoleIssueHistory 'GRANT' 처리
                history = new UserClusterRoleIssueHistoryVO();
                history.setUserSeq(targerUserSeq);
                history.setClusterSeq(issueRow.getClusterSeq());
                history.setIssueType(issueRow.getIssueType());
                history.setBindingType(issueRow.getBindingType());
                history.setIssueRole(issueRow.getIssueRole());
                history.setIssueAccountName(issueRow.getIssueAccountName());
                history.setIssueUserSeq(updater);
                history.setHistoryState(HistoryState.GRANT.getCode());

                // history 추가
                userDao.addUserClusterRoleIssueHistory(history);

                // binding history 추가
                if (CollectionUtils.isNotEmpty(issueRow.getBindings())) {
                    userDao.addUserClusterRoleIssueBindingHistories(history.getHistorySeq(), issueRow.getBindings(), updater);
                }
            }
        }
    }
}
