package run.acloud.api.configuration.service;

import com.google.common.collect.Lists;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import run.acloud.api.auth.dao.IUserMapper;
import run.acloud.api.auth.enums.ServiceMode;
import run.acloud.api.auth.enums.UserGrant;
import run.acloud.api.auth.enums.UserRole;
import run.acloud.api.auth.vo.UserVO;
import run.acloud.api.configuration.dao.IAccountApplicationMapper;
import run.acloud.api.configuration.dao.IAccountMapper;
import run.acloud.api.configuration.dao.IClusterMapper;
import run.acloud.api.configuration.enums.HistoryState;
import run.acloud.api.configuration.enums.IssueType;
import run.acloud.api.configuration.enums.ServiceRegistryType;
import run.acloud.api.configuration.enums.ServiceType;
import run.acloud.api.configuration.vo.*;
import run.acloud.api.cserver.dao.IServiceMapper;
import run.acloud.api.cserver.service.ServiceService;
import run.acloud.api.cserver.service.ServiceValidService;
import run.acloud.api.cserver.vo.ServiceUserVO;
import run.acloud.api.resource.enums.CubeType;
import run.acloud.api.resource.enums.ProviderAccountType;
import run.acloud.api.resource.enums.ProviderCode;
import run.acloud.api.resource.service.NamespaceService;
import run.acloud.api.resource.util.ResourceUtil;
import run.acloud.api.resource.vo.K8sNamespaceVO;
import run.acloud.commons.client.ClusterApiClient;
import run.acloud.commons.enums.CRUDCommand;
import run.acloud.commons.enums.HarborRegistryProjectMemberRole;
import run.acloud.commons.service.HarborRegistryFactoryService;
import run.acloud.commons.service.IHarborRegistryService;
import run.acloud.commons.util.AuthUtils;
import run.acloud.commons.util.CryptoUtils;
import run.acloud.commons.util.JsonUtils;
import run.acloud.commons.vo.HarborProjectMemberVO;
import run.acloud.commons.vo.HarborUserReqVO;
import run.acloud.commons.vo.HarborUserRespVO;
import run.acloud.framework.context.ContextHolder;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;
import run.acloud.framework.properties.CocktailServiceProperties;
import run.acloud.framework.util.ExceptionMessageUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AccountService {
	@Resource(name = "cocktailSession")
    private SqlSessionTemplate sqlSession;

    @Autowired
    private UserService userService;

    @Autowired
    private ProviderAccountService providerAccountService;

    @Autowired
    private ClusterApiClient clusterApiClient;

    @Autowired
    private ServiceService serviceService;

    @Autowired
    private ServiceValidService serviceValidService;

    @Autowired
    private AccountGradeService accountGradeService;

    @Autowired
    private UserClusterRoleIssueService userClusterRoleIssueService;

    @Autowired
    private NamespaceService namespaceService;

    @Autowired
    private ClusterStateService clusterStateService;

    @Autowired
    private ClusterService clusterService;

    @Autowired
    private CocktailServiceProperties cocktailServiceProperties;

    @Autowired
    private GradePlanService gradePlanService;

    @Autowired
    private AccountRegistryService accountRegistryService;

    @Autowired
    private HarborRegistryFactoryService harborRegistryFactory;


    /**
     * Account List 조회.
     * @param params
     * @return
     * @throws Exception
     */
    public List<AccountVO> getAccounts(Map<String, Object> params) throws Exception {
        IAccountMapper dao = sqlSession.getMapper(IAccountMapper.class);

        // get Lists
        List<AccountVO> accounts = dao.getAccounts(params);


        // account의 grade 정보 조회 및 셋팅
        if(CollectionUtils.isNotEmpty(accounts)) {
            List<Integer> accountSeqs = accounts.stream().map(AccountVO::getAccountSeq).collect(Collectors.toList());

            List<AccountGradeVO> accountGradeVOList = accountGradeService.getAccountGradesByAccounts(accountSeqs); // grade 정보 조회

            if(CollectionUtils.isNotEmpty(accountGradeVOList)) { // accountGrade가 존재할 때만 설정
                // 검색을 속도를 위해 hashmap에 셋팅
                Map<Integer, AccountGradeVO> accountGradeVOHashMap = accountGradeVOList.stream().collect(Collectors.toMap(AccountGradeVO::getAccountSeq, Function.identity()));

                // accountGrade 정보 셋팅
                AccountGradeVO tmpAccountGradeVO = null;
                for (AccountVO accountVO : accounts) {
                    tmpAccountGradeVO = accountGradeVOHashMap.get(accountVO.getAccountSeq());
                    if (tmpAccountGradeVO != null) {
                        accountVO.setAccountGrade(tmpAccountGradeVO);
                    }
                }
            }
        }

        return accounts;
    }

    /**
     * Account 정보 조회.
     * @param accountSeq
     * @return
     * @throws Exception
     */
    public AccountVO getAccount(Integer accountSeq) throws Exception {
        return this.getAccount(accountSeq, false);
    }

    public void checkAccount(AccountVO account) throws Exception{
        if (StringUtils.isBlank(account.getAccountName()) ||
                account.getAccountName().length() > 50) {
            throw new CocktailException("Account name is null or empty or more than 50 characters", ExceptionType.AccountNameIsNull);
        }
        ExceptionMessageUtils.checkParameter("accountCode", account.getAccountCode(), 50, true);
        ExceptionMessageUtils.checkParameter("description", account.getDescription(), 300, false);
        ExceptionMessageUtils.checkParameter("organizationName", account.getOrganizationName(), 50, true);
        ExceptionMessageUtils.checkParameter("customerName", account.getCustomerName(), 50, true);
        ExceptionMessageUtils.checkParameter("customerEmail", account.getCustomerEmail(), 50, true);
        ExceptionMessageUtils.checkParameter("customerAddress", account.getCustomerAddress(), 300, true);
        ExceptionMessageUtils.checkParameter("licenceKey", account.getLicenseKey(), 500, false);

        if(account.getAccountType().isOnline()) {
            if(account.getAccountGrade() != null) {
                if(account.getAccountGrade().getGradeSeq()  == null || account.getAccountGrade().getGradeSeq() < 1) {
                    throw new CocktailException("Grade Sequence is null or empty", ExceptionType.InvalidParameter_Empty);
                }
            }
            else {
                throw new CocktailException("Grade Information is null or empty", ExceptionType.InvalidParameter_Empty);
            }
        }

    }

    /**
     * Account 상세 정보 조회.
     * @param accountSeq
     * @return
     * @throws Exception
     */
    public AccountVO getAccount(Integer accountSeq, boolean includeDetail) throws Exception {
        IAccountMapper dao = sqlSession.getMapper(IAccountMapper.class);

        // get Account
        AccountVO account = dao.getAccount(accountSeq);
        if(!includeDetail || account == null) {
            return account;
        }

        AccountVO accountInfo = dao.getAccountDetailInfo(accountSeq);
        if(accountInfo == null) {
            return accountInfo;
        }

        /** provider account가 GCP이고 USER Type이면 ProjectId에 apiAccountID를 넣어줌 **/
        // UI와 DB 필드 매핑은 다음과 같으니 혼동하면 안됨.
        // 결제계정ID(Billing Account ID) = account_group_id
        // 프로젝트ID(Project ID) = api_account_id
        accountInfo.setLogoImage(account.getLogoImage());
        accountInfo.setUseYn(null);
        List<ProviderAccountVO> providerAccounts = accountInfo.getProviderAccounts();
        if (CollectionUtils.isNotEmpty(providerAccounts)) {
            for (ProviderAccountVO providerAccountRow : providerAccounts) {
                if (providerAccountRow.getProviderCode() == ProviderCode.GCP && providerAccountRow.getAccountUseType() == ProviderAccountType.USER) {
                    providerAccountRow.setProjectId(providerAccountRow.getApiAccountId());
                }
            }
        }

        /** account 적용된 grade plan 정보 조회 및 셋팅 **/
        AccountGradeVO paramAccountGrade = new AccountGradeVO();
        paramAccountGrade.setAccountSeq(accountSeq); //account seq & use_yn Y 인 1건 조회
        AccountGradeVO accountGradeVO = accountGradeService.getAccountGrade(paramAccountGrade);
        if(accountGradeVO != null){
            accountInfo.setAccountGrade(accountGradeVO);
        }

        /** Account내의 클러스터들중 Provider의 AccessKey가 연결된 클러스터 목록 조회 및 셋팅 **/
        IAccountMapper accountDao = sqlSession.getMapper(IAccountMapper.class);
        List<ClusterVO> currentClusters = accountDao.getLinkedClustersOfAccount(account.getAccountSeq(), ProviderAccountType.ACCESS_KEY.getCode());
        accountInfo.setClusters(currentClusters);

        List<ClusterProviderVO> clusterProviders = new ArrayList<>();
        for(ClusterVO cluster : currentClusters) {
            ClusterProviderVO clusterProvider = new ClusterProviderVO();
            clusterProvider.setAccountSeq(account.getAccountSeq());
            clusterProvider.setClusterSeq(cluster.getClusterSeq());
            clusterProvider.setProviderAccountSeq(cluster.getCloudProviderAccountSeq());
            clusterProviders.add(clusterProvider);
        }
        accountInfo.setClusterProviders(clusterProviders);

        /** Account내의 서비스 레지스트리 목록 조회 및 셋팅 **/
        // 플랫폼 워크스페이스의 서비스레지스트리를 조회하여 셋팅
        List<ServiceRegistryVO> serviceRegistries = serviceService.getServiceRegistryOfAccount(accountSeq, ServiceRegistryType.SERVICE.getCode(), null, ServiceType.PLATFORM.getCode());
        accountInfo.setProjects(serviceRegistries);

        return accountInfo;
    }

//    /**
//     * Account 생성.
//     * @param account
//     * @throws Exception
//     */
//    @Transactional(transactionManager = "transactionManager")
//    public void addAccount(AccountVO account) throws Exception {
//        IAccountMapper dao = sqlSession.getMapper(IAccountMapper.class);
//
//        /**
//         * 프로바이더 테이블에 먼저 데이터를 입력
//         */
//        List<ProviderAccountVO> providerAccountVOList = account.getProviderAccounts();
//        List<Integer> providerSeqList = new ArrayList<>();
//
//        if(providerAccountVOList != null && providerAccountVOList.size() >= 1) {
//            for(ProviderAccountVO providerAccount : providerAccountVOList) {
//                // 기본 정보 설정
//                providerAccount.setCreator(account.getCreator());
//                providerAccount.setUpdater(account.getUpdater());
//                providerAccount.setUseYn(account.getUseYn());
//
//                this.addProviderAccount(providerAccount, providerSeqList);
//            }
//        }
//
//        /**
//         * 계정 테이블에 데이터 입력
//         */
//        dao.addAccount(account);
//
//        /**
//         * 계정 프로바이더계정 매핑 테이블에 데이터 입력
//         */
//        // 입력된 Provider 정보가 있으면 매핑 입력
//        if(providerSeqList != null && providerSeqList.size() >= 1) {
//            dao.addProviderOfAccount(account.getAccountSeq(), providerSeqList, account.getCreator());
//        }
//
//        /**
//         * 사용자 매핑 처리.
//         */
//        List<Integer> addUserList = new ArrayList<>();
//
//        // 계정에 매핑될 사용자를 목록에 추가
//        if(account.getUserSeqs() != null && account.getUserSeqs().size() >= 1) {
//            addUserList.addAll(account.getUserSeqs());
//        }
//
////        // Admin Role을 가진 사용자를 모두 조회하여 매핑 목록에 추가 : Admin은 매핑하지 않음.
////        List<Integer> adminUsers = this.userService.selectSeqByUserRole(UserRole.ADMIN.getCode());
////        if(adminUsers != null && adminUsers.size() >= 1) {
////            // 중복된 사용자에 대한 확인을 위해 addAll을 사용하지 않고, 중복 체크 후 add 하도록 처리함
////            for(Integer adminUser : adminUsers) {
////                if(!addUserList.contains(adminUser)) {
////                    addUserList.add(adminUser);
////                }
////            }
////        }
//
//        // 사용자 매핑 처리
//        if(addUserList != null && addUserList.size() >= 1) {
//            dao.addUserOfAccount(account.getAccountSeq(), addUserList, account.getCreator());
//        }
//
//    }

//    /**
//     * Provider Account를 등록하고, 매핑을 위해 Sequence를 List로 저장.
//     * @param providerAccount
//     * @param addList
//     * @throws Exception
//     */
//    public void addProviderAccount(ProviderAccountVO providerAccount, List<Integer> addList) throws Exception {
//
//        // Provider Account 추가
//        this.providerAccountService.addProviderAccountProcess(providerAccount);
//
//        // Account와 매핑을 위해 추가한 Provider Account를 List에 저장.
//        addList.add(Integer.valueOf(providerAccount.getProviderAccountSeq()));
//    }

//    /**
//     * Account 생성.
//     *
//     * @param account
//     * @throws Exception
//     */
//    @Transactional(transactionManager = "transactionManager")
//    public void addAccount(AccountVO account) throws Exception {
//        IAccountMapper dao = sqlSession.getMapper(IAccountMapper.class);
//
//        /**
//         * 계정 테이블에 데이터 입력
//         */
//        dao.addAccount(account);
//
//        /**
//         * ProviderAccount 생성
//         */
//        this.addProviderAccounts(account);
//
//        /**
//         * 계정 grade plan 정보 등록, 2019-04-22, coolingi
//         * account 로직을 addAccountGrade 메서드 안으로 이동, 2019-05-29, coolingi
//         */
//        this.addAccountGrade(account);
//    }

    /**
     * Account 정보/관리자 생성.</br>
     * Account Registry 등록 추가, 2021-11-23, coolingi
     *
     * @param account
     * @throws Exception
     */
    @Transactional(transactionManager = "transactionManager")
    public void addAccountInfo(AccountVO account) throws Exception {

        IAccountMapper dao = sqlSession.getMapper(IAccountMapper.class);

        if (ServiceMode.valueOf(cocktailServiceProperties.getMode()).isAddUser()) {
            /**
             * add account user
             */
            // 사용자 기본 정보 체크
            userService.checkAddUser(account.getAccountUser());

            account.getAccountUser().setUserLanguage(account.getBaseLanguage());
            account.getAccountUser().setUserTimezone(account.getBaseLanguage().getTimezone());
            userService.addUser(account.getAccountUser());

            account.setAccountUserSeq(account.getAccountUser().getUserSeq());
            account.getAccountUser().setPassword(null);
            account.getAccountUser().setNewPassword(null);
        }

        /**
         * 계정 테이블에 데이터 입력
         */
        dao.addAccount(account);

        if (ServiceMode.valueOf(cocktailServiceProperties.getMode()).isAddUser()) {
            /**
             * Account System 사용자 Mapping 등록
             */
            this.addSystemUserOfAccountMapping(account.getAccountSeq(), account.getAccountUser().getUserSeq(), ContextHolder.exeContext().getUserSeq());
        }

        if (ServiceMode.valueOf(cocktailServiceProperties.getMode()).isAddGrade()) {

            /**
             * 계정 grade plan 정보 등록, 2019-04-22, coolingi
             * account 로직을 addAccountGrade 메서드 안으로 이동, 2019-05-29, coolingi
             */
            accountGradeService.addAccountGrade(account);
        }

        /**
         * Account Registry 등록, 2021-11-23
         */
        if (account.getAccountRegistry() != null){
            AccountRegistryVO accountRegistry = account.getAccountRegistry();
            accountRegistry.setAccountSeq(account.getAccountSeq());
            accountRegistryService.addAccountRegistry(accountRegistry);
        }

        /**
         * 플랫폼 워크스페이스 생성
         */
        serviceService.createPlatformWorkspace(account.getAccountSeq());

        /**
         * 플랫폼 레지스트리 pull 전용 사용자 생성
         */
        this.createAccountRegistryPullUser(account, dao);

        /**
         * [Single Tenancy : 2021.01.26]
         * 싱글 테넌시 플랫폼일 경우 기본 워크스페이스를 구성 함.
         */
        if (gradePlanService.isSingleTenancy(account.getAccountGrade())) {
            ServiceAddVO service = serviceService.createSingleTenancyWorkspace(account.getAccountSeq(), account.getAccountName(), account.getDescription());

            // 생성한 사용자의 lastServiceSeq를 기본 워크스페이스로 구성
            IUserMapper userDao = sqlSession.getMapper(IUserMapper.class);
            userDao.updateDefaultUsersLastService(service.getServiceSeq(), Collections.singletonList(account.getAccountUserSeq()), ContextHolder.exeContext().getUserSeq());
        }

    }


    @Transactional(transactionManager = "transactionManager")
    public void createAccountRegistryPullUser(Integer accountSeq) throws Exception {
        IAccountMapper dao = sqlSession.getMapper(IAccountMapper.class);
        AccountVO account = dao.getAccount(accountSeq);
        this.createAccountRegistryPullUser(account, dao);
    }

    @Transactional(transactionManager = "transactionManager")
    public void createAccountRegistryPullUser(AccountVO account) throws Exception {
        this.createAccountRegistryPullUser(account, null);
    }

    @Transactional(transactionManager = "transactionManager")
    public void createAccountRegistryPullUser(AccountVO account, IAccountMapper dao) throws Exception {
        if (dao == null) {
            dao = sqlSession.getMapper(IAccountMapper.class);
        }

        if (account != null && StringUtils.isBlank(account.getRegistryDownloadUserId())) {
            /**
             * 2022.03.15 hjchoi
             * 플랫폼 레지스트리 pull 전용 사용자 생성
             * - 변경사유 : 기존에도 pull은 모든 레지스트리에 접근이 가능했음
             * - 플랫폼별로 pull 전용 사용자를 생성하여 워크로드에서 imagePullSecret에서 공통으로 사용하도록 변경
             */
            // 해당 워크스페이스의 registry 사용자 등록 정보
            HarborUserReqVO registryUser = new HarborUserReqVO();
            registryUser.setUsername(ResourceUtil.makeRegistryPullUserId(account.getAccountCode()));
            registryUser.setPassword(ResourceUtil.makeRegistryUserPassword());
            registryUser.setRealname(account.getAccountName());
            registryUser.setComment("account pull user");

            // get Harbor API Client
            IHarborRegistryService harborRegistryService = harborRegistryFactory.getService(account.getAccountSeq());

            if (harborRegistryService.getUser(registryUser.getUsername()) == null) {
                // registry 사용자 추가
                harborRegistryService.addUser(registryUser);
            }

            account.setRegistryDownloadUserId(CryptoUtils.encryptAES(registryUser.getUsername()));
            account.setRegistryDownloadUserPassword(CryptoUtils.encryptAES(registryUser.getPassword()));

            dao.editAccountRegistryPullUser(account);

            // 플랫폼에 존재하는 레지스트리(project)에 pull user를 member로 추가
            this.addAccountPullUserToRegistryProjectMember(account.getAccountSeq());
        }
    }

    /**
     * 플랫폼에 존재하는 레지스트리(project)에 pull user를 member로 추가
     */
    public void addAccountPullUserToRegistryProjectMember(Integer accountSeq) throws Exception {
        if (accountSeq != null) {
            IAccountMapper accountDao = sqlSession.getMapper(IAccountMapper.class);
            AccountVO account = accountDao.getAccount(accountSeq);

            if (account != null && StringUtils.isNotBlank(account.getRegistryDownloadUserId())) {
                // 플랫폼에 존재하는 레지스트리 조회
                List<ServiceRegistryVO> registries = serviceService.getServiceRegistryOfAccount(accountSeq, ServiceRegistryType.SERVICE.getCode(), null);

                if (CollectionUtils.isNotEmpty(registries)) {
                    IHarborRegistryService harborRegistryService = harborRegistryFactory.getService(accountSeq);

                    // pull user 조회 (harbor)
                    HarborUserRespVO registryUser = harborRegistryService.getUser(CryptoUtils.decryptAES(account.getRegistryDownloadUserId()));

                    if (registryUser != null) {
                        HarborProjectMemberVO projectMember = new HarborProjectMemberVO();
                        projectMember.setEntityName(registryUser.getUsername());

                        // add member ( role : guest )
                        for (ServiceRegistryVO srRow : registries) {
                            projectMember.setRoleId(HarborRegistryProjectMemberRole.GUEST.getValue());
                            harborRegistryService.addMemberToProject(srRow.getProjectId(), projectMember, true);
                        }
                    }
                }
            }
        }
    }

    @Transactional(transactionManager = "transactionManager")
    public void addUser(Integer accountSeq, UserVO user) throws Exception {
//        IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
        // 사용자 등록
        this.userService.addUser(user);

        UserRole userRole = UserRole.valueOf(user.getRoles().get(0));
        if (userRole.isDevops()) {
            // DEVOPS 사용자 && 계정 mapping 등록
            this.addUserOfAccountMapping(accountSeq, user.getUserSeq(), ContextHolder.exeContext().getUserSeq());
        } else if (userRole.isUserOfSystem()) {
            // SYSTEM 사용자 && 계정 mapping 등록
            this.addSystemUserOfAccountMapping(accountSeq, user.getUserSeq(), ContextHolder.exeContext().getUserSeq());
        }

        /**
         * Single Tenancy 플랫폼이면 사용자를 Single Tenancy 워크스페이스에 즉시 등록.
         */
        this.addSingleTenancyUser(gradePlanService.isSingleTenancy(user.getAccount().getAccountGrade()), accountSeq, user, userRole);

        // 생성시 클러스터 계정 정보가 없음
//        List<ClusterVO> clusters = clusterDao.getClusterCondition(null, accountSeq, null);
//        List<Integer> userClusterSeqs = Optional.ofNullable(clusters).orElseGet(() ->Lists.newArrayList()).stream().map(ClusterVO::getClusterSeq).collect(Collectors.toList());
//
//        // 사용자 클러스터 체크
//        userClusterRoleIssueService.checkUserClusterForIssueRole(userClusterSeqs, user);
//        // kubeconfig 만료일 체크
//        userClusterRoleIssueService.checkUserClusterIssueRoleForExpirationDate(null, user);
//
//        // 클러스터 Shell, 권한 등록
//        user.setShellRoles(userClusterRoleIssueService.addUserClusterRoleIssues(accountSeq, user.getUserSeq(), user.getUserId(), IssueType.SHELL, user.getShellRoles(), ContextHolder.exeContext().getUserSeq()));
//        // cluster-api 권한 추가 요청
//        clusterApiClient.manageClusterRole(ContextHolder.exeContext().getUserSeq().toString(), ContextHolder.exeContext().getUserRole(), accountSeq, null, IssueType.SHELL, user.getShellRoles(), null, null);
//
//        // 클러스터 kubeconfig 권한 등록
//        user.setKubeconfigRoles(userClusterRoleIssueService.addUserClusterRoleIssues(accountSeq, user.getUserSeq(), user.getUserId(), IssueType.KUBECONFIG, user.getKubeconfigRoles(), ContextHolder.exeContext().getUserSeq()));
//        // cluster-api 권한 추가 요청
//        clusterApiClient.manageClusterRole(ContextHolder.exeContext().getUserSeq().toString(), ContextHolder.exeContext().getUserRole(), accountSeq, null, IssueType.KUBECONFIG, user.getKubeconfigRoles(), null, null);
    }

    @Transactional(transactionManager = "transactionManager")
    public UserVO editUser(Integer accountSeq, UserVO user) throws Exception {
        UserVO userCurr = userService.getUserOfAccountWithPwdPolicy(accountSeq, user.getUserSeq());
        if (userCurr == null) {
            throw new CocktailException("User not found", ExceptionType.UserIdNotFound);
        }else {
            UserRole currUserRole = UserRole.valueOf(userCurr.getRoles().get(0));
            userService.checkEditUser(userCurr, user); // DEVOPS, SYSUSER 권한의 RequestUser가 대상 사용자를 상위 권한으로 변경 요청 하는 경우 여기서 판단하여 throw 함.

            IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
            List<ClusterVO> clusters = clusterDao.getClusters(accountSeq, null, null, null, null, "Y");
            Map<Integer, ClusterVO> userClusterMap = Optional.ofNullable(clusters).orElseGet(Lists::newArrayList).stream().collect(Collectors.toMap(ClusterVO::getClusterSeq, Function.identity()));

            userService.editUser(userCurr);

            // 사용자 수정시 비밀번호 초기화 하지 않도록 수정
            // 2021.06.14, hjchoi
//            if (BooleanUtils.toBoolean(user.getInitPasswordYn())) {
//                userService.resetPasswordUser(user);
//            }

            /** Request사용자가 ADMIN과 SYSTEM 계정 사용자일때만 클러스터 계정 정보에 대한 처리가 가능함 : 그외 권한자가 요청시 무시**/
            UserRole requestUserRole = UserRole.valueOf(ContextHolder.exeContext().getUserRole());
            if(requestUserRole.isAdminNSystem()) {
                // 사용자 클러스터 체크
                userClusterRoleIssueService.checkUserClusterForIssueRole(userClusterMap, user);
                // kubeconfig 만료일 체크
                userClusterRoleIssueService.checkUserClusterIssueRoleForExpirationDate(userCurr, user);
                // 클러스터 Shell, 권한 수정
                userClusterRoleIssueService.editUserClusterRoleIssues(accountSeq, user.getUserSeq(), userCurr.getUserId(), IssueType.SHELL, userCurr.getShellRoles(), user.getShellRoles(), ContextHolder.exeContext().getUserSeq());
                // 클러스터 kubeconfig 권한 수정
                userClusterRoleIssueService.editUserClusterRoleIssues(accountSeq, user.getUserSeq(), userCurr.getUserId(), IssueType.KUBECONFIG, userCurr.getKubeconfigRoles(), user.getKubeconfigRoles(), ContextHolder.exeContext().getUserSeq());
            }

            UserRole userRole = UserRole.valueOf(user.getRoles().get(0));

            IServiceMapper serviceDao = sqlSession.getMapper(IServiceMapper.class);
            IAccountMapper accountDao = sqlSession.getMapper(IAccountMapper.class);

            /** DevOps -> System 사용자로 변경시 추가 코드 **/
            if (currUserRole.isDevops() && userRole.isUserOfSystem()) {
                // service_user 테이블 연결 삭제 / account_user_mapping 테이블 연결 삭제
                serviceDao.deleteUserOfService(user.getUserSeq());
                accountDao.deleteUserOfAccount(user.getUserSeq());

                // account_system_user_mapping 테이블 연결 추가
                accountDao.addSystemUserOfAccount(accountSeq, Collections.singletonList(user.getUserSeq()), ContextHolder.exeContext().getUserSeq());
            }
            /** System 사용자 -> DevOps 사용자로 변경시 추가 코드 **/
            else if (currUserRole.isUserOfSystem() && userRole.isDevops()) {
                // account_system_user_mapping 테이블 삭제
                accountDao.removeSystemUserOfAccount(accountSeq, Collections.singletonList(user.getUserSeq()));
                // account_user_mapping 테이블 연결 추가
                accountDao.addUserOfAccount(accountSeq, Collections.singletonList(user.getUserSeq()), ContextHolder.exeContext().getUserSeq());

                //TODO : 클러스터 계정 관리를 칵테일 계정과 분리하게 되면 사용자 정보 수정시에 클러스터 계정을 삭제하진 않는다.
//                /** 클러스터 Shell / 클러스터 kubeconfig 권한 제거 (System -> Devops로 강등되는 순간 어떤 클러스터에도 권한이 없게 되므로 제거 함) **/
//                // 클러스터 Shell, 권한 삭제
//                user.setShellRoles(userClusterRoleIssueService.removeUserClusterRoleIssues(accountSeq, user.getUserSeq(), IssueType.SHELL, user.getShellRoles(), HistoryState.REVOKE, ContextHolder.exeContext().getUserSeq()));
//                // cluster-api 권한 삭제 요청
//                clusterApiClient.manageClusterRole(ContextHolder.exeContext().getUserSeq().toString(), ContextHolder.exeContext().getUserRole(), accountSeq, user.getUserSeq(), IssueType.SHELL, null, null, user.getShellRoles());
//
//                // 클러스터 kubeconfig 권한 삭제
//                user.setKubeconfigRoles(userClusterRoleIssueService.removeUserClusterRoleIssues(accountSeq, user.getUserSeq(), IssueType.KUBECONFIG, user.getKubeconfigRoles(), HistoryState.REVOKE, ContextHolder.exeContext().getUserSeq()));
//                // cluster-api 권한 삭제 요청
//                clusterApiClient.manageClusterRole(ContextHolder.exeContext().getUserSeq().toString(), ContextHolder.exeContext().getUserRole(), accountSeq, user.getUserSeq(), IssueType.KUBECONFIG, null, null, user.getKubeconfigRoles());

                /**
                 * is Single Tenancy : System 사용자 -> DevOps 사용자로 변경시 Single Tenancy 워크스페이스에 입력 해 줌.
                 */
                AccountGradeVO paramAccountGrade = new AccountGradeVO();
                paramAccountGrade.setAccountSeq(accountSeq);
                AccountGradeVO accountGrade = accountGradeService.getAccountGrade(paramAccountGrade);
                this.addSingleTenancyUser(gradePlanService.isSingleTenancy(accountGrade), accountSeq, user, userRole);
            }
            /** DevOps -> DevOps 사용자로 변경 (워크스페이스 권한 수정) **/
            else if (currUserRole.isDevops() && userRole.isDevops()) {
                AccountGradeVO paramAccountGrade = new AccountGradeVO();
                paramAccountGrade.setAccountSeq(accountSeq);
                AccountGradeVO accountGrade = accountGradeService.getAccountGrade(paramAccountGrade);
                if(gradePlanService.isSingleTenancy(accountGrade)) {
                    // 워크스페이스 권한에 변경이 있을때만 처리한다.
                    if (!StringUtils.equals(userCurr.getUserGrant(), user.getUserGrant())) {
                        ServiceVO singleTenancyService = serviceDao.getServiceByType(accountSeq, null, ServiceType.NORMAL.getCode(), "Y");
                        // 워크스페이스 권한 수정.
                        serviceDao.updateUsersOfService(
                            singleTenancyService.getServiceSeq()
                            , user.getUserSeq()
                            , user.getUserGrant()
                            , ContextHolder.exeContext().getUserSeq());
                    }
                }
            }

        }

        return userService.getByUserSeq(user.getUserSeq());
    }

    public void addSingleTenancyUser(boolean isSingleTenancy, Integer accountSeq, UserVO user, UserRole userRole) throws Exception {
        if(isSingleTenancy) {
            /** DevOps 사용자일 경우에만 Single Tenancy 워크스페이스에 등록 해 주어야 함 (System 사용자는 원래 등록 없이 Access 가능) **/
            IServiceMapper serviceDao = sqlSession.getMapper(IServiceMapper.class);
            IUserMapper userDao = sqlSession.getMapper(IUserMapper.class);
            ServiceVO singleTenancyService = serviceDao.getServiceByType(accountSeq, null, ServiceType.NORMAL.getCode(), "Y");
            if(userRole.isDevops()) {
                if(StringUtils.isBlank(user.getUserGrant())) {
                    // DevOps 사용자가 UserGrant가 없으면 오류
                    throw new CocktailException("UserGrant is missing", ExceptionType.InvalidParameter_Empty);
                }

                int count = serviceDao.getServicesCountByType(accountSeq, null, ServiceType.NORMAL.getCode());
                if(count != 1) {
                    // Single Tenancy가 워크스페이스를 여러개 가지고 있거나 없다면 오류 -> 아래에서 어느 워크스페이스에 연결해야 할지 판단 어려움. 사전 체크.
                    throw new CocktailException("Invalid Account - Invalid Single Tenancy Platform", ExceptionType.InvalidState);
                }

                // 추가
                ServiceUserVO serviceUser = new ServiceUserVO();
                serviceUser.setServiceSeq(singleTenancyService.getServiceSeq());
                serviceUser.setUserSeq(user.getUserSeq());
                serviceUser.setUserGrant(UserGrant.valueOf(user.getUserGrant()));
                serviceDao.addUsersOfService(singleTenancyService.getServiceSeq(), Collections.singletonList(serviceUser), ContextHolder.exeContext().getUserSeq());
            }

            // update 마지막 작업한 workspace (Single Tenancy 워크스페이스로... 모든 사용자 공통)
            userDao.updateDefaultUsersLastService(singleTenancyService.getServiceSeq(), Collections.singletonList(user.getUserSeq()), ContextHolder.exeContext().getUserSeq());
        }
    }

    @Transactional(transactionManager = "transactionManager")
    public UserVO editUserGrant(Integer accountSeq, UserVO user) throws Exception {
        UserVO userCurr = userService.getUserOfAccountWithPwdPolicy(accountSeq, user.getUserSeq());
        if (userCurr == null) {
            throw new CocktailException("User not found", ExceptionType.UserIdNotFound);
        }else {
            UserRole currUserRole = UserRole.valueOf(userCurr.getRoles().get(0));
            userService.checkEditUser(userCurr, user);
            userService.editUser(userCurr);

            // 사용자 수정시 비밀번호 초기화 하지 않도록 수정
            // 2021.06.14, hjchoi
//            if (BooleanUtils.toBoolean(user.getInitPasswordYn())) {
//                userService.resetPasswordUser(user);
//            }

            UserRole userRole = UserRole.valueOf(user.getRoles().get(0));
            /** DevOps -> System 사용자로 변경시 추가 코드 **/
            if (currUserRole.isDevops() && userRole.isUserOfSystem()) {
                IServiceMapper serviceDao = sqlSession.getMapper(IServiceMapper.class);
                IAccountMapper accountDao = sqlSession.getMapper(IAccountMapper.class);
                // service_user 테이블 연결 삭제 / account_user_mapping 테이블 연결 삭제
                serviceDao.deleteUserOfService(user.getUserSeq());
                accountDao.deleteUserOfAccount(user.getUserSeq());

                // account_system_user_mapping 테이블 연결 추가
                accountDao.addSystemUserOfAccount(accountSeq, Collections.singletonList(user.getUserSeq()), ContextHolder.exeContext().getUserSeq());
            }
            /** System 사용자 -> DevOps 사용자로 변경시 추가 코드 **/
            else if (currUserRole.isUserOfSystem() && userRole.isDevops()) {
                IAccountMapper accountDao = sqlSession.getMapper(IAccountMapper.class);
                // account_system_user_mapping 테이블 삭제
                accountDao.removeSystemUserOfAccount(accountSeq, Collections.singletonList(user.getUserSeq()));
                // account_user_mapping 테이블 연결 추가
                accountDao.addUserOfAccount(accountSeq, Collections.singletonList(user.getUserSeq()), ContextHolder.exeContext().getUserSeq());
            }
        }

        return userService.getByUserSeq(user.getUserSeq());
    }

    @Transactional(transactionManager = "transactionManager")
    public UserVO removeUser(Integer accountSeq, Integer userSeq, boolean force) throws Exception {
        UserVO user = userService.getByUserSeq(userSeq, null);
        if (user == null) {
            throw new CocktailException("User not found", ExceptionType.UserIdNotFound);
        }else{
            if (userSeq == 1){
                throw new CocktailException("Root Admin User do not edit", ExceptionType.UserRootAdminDontAction);
            }

            /** System > Sysuser > devops 권한에 따른 삭제 권한 체크 **/
            userService.canEditUser(user);

            // TODO: SYSTEM ROLE 사용자가 Account에 종속되어 있는지 체크
            // 이외의 사용자는 Account, Service에 종속되어 있는지 체크
            if(!userService.canRemoveUser(user) && !force){
                throw new CocktailException("Can not delete. because a user have a account.", ExceptionType.UserHaveAccount);
            } else {
                String reqUserRole = ContextHolder.exeContext().getUserRole();
                // 클러스터 Shell, 권한 삭제
                user.setShellRoles(userClusterRoleIssueService.removeUserClusterRoleIssues(accountSeq, userSeq, IssueType.SHELL, user.getShellRoles(), HistoryState.REVOKE, ContextHolder.exeContext().getUserSeq()));
                // cluster-api 권한 삭제 요청
                clusterApiClient.manageClusterRole(ContextHolder.exeContext().getUserSeq().toString(), StringUtils.equalsIgnoreCase(reqUserRole, UserRole.ADMIN.getCode()) ? UserRole.SYSTEM.getCode() : reqUserRole, accountSeq, userSeq, IssueType.SHELL, null, null, user.getShellRoles());

                // 클러스터 kubeconfig 권한 삭제
                user.setKubeconfigRoles(userClusterRoleIssueService.removeUserClusterRoleIssues(accountSeq, userSeq, IssueType.KUBECONFIG, user.getKubeconfigRoles(), HistoryState.REVOKE, ContextHolder.exeContext().getUserSeq()));
                // cluster-api 권한 삭제 요청
                clusterApiClient.manageClusterRole(ContextHolder.exeContext().getUserSeq().toString(), StringUtils.equalsIgnoreCase(reqUserRole, UserRole.ADMIN.getCode()) ? UserRole.SYSTEM.getCode() : reqUserRole, accountSeq, userSeq, IssueType.KUBECONFIG, null, null, user.getKubeconfigRoles());

                IServiceMapper serviceDao = sqlSession.getMapper(IServiceMapper.class);
                IAccountMapper accountDao = sqlSession.getMapper(IAccountMapper.class);
                // service_user 테이블 연결 삭제 / account_user_mapping 테이블 연결 삭제
                serviceDao.deleteUserOfService(userSeq);
                accountDao.deleteUserOfAccount(userSeq);
                // account_system_user_mapping 테이블 삭제
                accountDao.removeSystemUserOfAccount(accountSeq, Collections.singletonList(userSeq));

                user.setUpdater(ContextHolder.exeContext().getUserSeq());
                userService.deleteUser(userSeq);

            }
        }

        return user;
    }

    /**
     * 외부에서 사용자 삭제
     *
     * @param accountSeq
     * @param userSeq
     * @return
     * @throws Exception
     */
    @Transactional(transactionManager = "transactionManager")
    public UserVO removeUserExternal(Integer accountSeq, Integer userSeq) throws Exception {
        UserVO user = userService.getByUserSeq(userSeq, null);
        if (user != null) {

            String reqUserRole = UserRole.ADMIN.getCode();
            Integer reqUserSeq = Integer.valueOf(1);
            // 클러스터 Shell, 권한 삭제
            user.setShellRoles(userClusterRoleIssueService.removeUserClusterRoleIssues(accountSeq, userSeq, IssueType.SHELL, user.getShellRoles(), HistoryState.REVOKE, reqUserSeq));
            // cluster-api 권한 삭제 요청
            clusterApiClient.manageClusterRole(reqUserSeq.toString(), StringUtils.equalsIgnoreCase(reqUserRole, UserRole.ADMIN.getCode()) ? UserRole.SYSTEM.getCode() : reqUserRole, accountSeq, userSeq, IssueType.SHELL, null, null, user.getShellRoles());

            // 클러스터 kubeconfig 권한 삭제
            user.setKubeconfigRoles(userClusterRoleIssueService.removeUserClusterRoleIssues(accountSeq, userSeq, IssueType.KUBECONFIG, user.getKubeconfigRoles(), HistoryState.REVOKE, reqUserSeq));
            // cluster-api 권한 삭제 요청
            clusterApiClient.manageClusterRole(reqUserSeq.toString(), StringUtils.equalsIgnoreCase(reqUserRole, UserRole.ADMIN.getCode()) ? UserRole.SYSTEM.getCode() : reqUserRole, accountSeq, userSeq, IssueType.KUBECONFIG, null, null, user.getKubeconfigRoles());

            IServiceMapper serviceDao = sqlSession.getMapper(IServiceMapper.class);
            IAccountMapper accountDao = sqlSession.getMapper(IAccountMapper.class);
            // service_user 테이블 연결 삭제 / account_user_mapping 테이블 연결 삭제
            serviceDao.deleteUserOfService(userSeq);
            accountDao.deleteUserOfAccount(userSeq);
            // account_system_user_mapping 테이블 삭제
            accountDao.removeSystemUserOfAccount(accountSeq, Collections.singletonList(userSeq));

            user.setUpdater(reqUserSeq);
            userService.deleteUser(userSeq);

        }

        return user;
    }

    public UserVO getUser(Integer accountSeq, Integer userSeq, Boolean includeNamespace) throws Exception {

        UserVO user = userService.getUserOfAccountWithPwdPolicy(accountSeq, userSeq);

        if (user != null) {
            for(ClusterDetailNamespaceVO cluster : Optional.ofNullable(user.getClusters()).orElseGet(() ->Lists.newArrayList())) {
                if(includeNamespace && clusterStateService.isClusterRunning(cluster.getClusterSeq())) {
                    List<K8sNamespaceVO> namespaces = namespaceService.getNamespaces(cluster.getClusterSeq(), null, null, true, true, false, ContextHolder.exeContext());
                    if(CollectionUtils.isNotEmpty(namespaces)) {
                        cluster.setNamespaces(namespaces);
                    }
                }

//                if (CollectionUtils.isNotEmpty(user.getShellRoles())) {
//                    for (UserClusterRoleIssueVO roleIssueRow : user.getShellRoles()) {
//                        if (roleIssueRow.getClusterSeq().equals(cluster.getClusterSeq())) {
//                            roleIssueRow.setCluster(cluster);
//                        }
//                    }
//                }
//
//                if (CollectionUtils.isNotEmpty(user.getKubeconfigRoles())) {
//                    for (UserClusterRoleIssueVO roleIssueRow : user.getKubeconfigRoles()) {
//                        if (roleIssueRow.getClusterSeq().equals(cluster.getClusterSeq())) {
//                            roleIssueRow.setCluster(cluster);
//                        }
//                    }
//                }
            }

            // 사용자 휴면 여부
            userService.setUserSleepParam(user);
        }

        return user;
    }

    /**
     * 사용자 아이디로 사용자 정보 조회 : Keycloak에서 사용을 위해 해시된 패스워드도 응답함에 유의 할것
     * @param accountSeq
     * @param userId
     * @param includeNamespace
     * @return
     * @throws Exception
     */
    public UserVO getUserById(Integer accountSeq, String userId, Boolean includeNamespace) throws Exception {
        IUserMapper mapper = this.sqlSession.getMapper(IUserMapper.class);
        UserVO user = mapper.getUserOfAccountById(accountSeq, userId);

        if (user != null) {
            for(ClusterDetailNamespaceVO cluster : Optional.ofNullable(user.getClusters()).orElseGet(() ->Lists.newArrayList())) {
                if(includeNamespace && clusterStateService.isClusterRunning(cluster.getClusterSeq())) {
                    List<K8sNamespaceVO> namespaces = namespaceService.getNamespaces(cluster.getClusterSeq(), null, null, true, true, false, ContextHolder.exeContext());
                    if(CollectionUtils.isNotEmpty(namespaces)) {
                        cluster.setNamespaces(namespaces);
                    }
                }

//                if (CollectionUtils.isNotEmpty(user.getShellRoles())) {
//                    for (UserClusterRoleIssueVO roleIssueRow : user.getShellRoles()) {
//                        if (roleIssueRow.getClusterSeq().equals(cluster.getClusterSeq())) {
//                            roleIssueRow.setCluster(cluster);
//                        }
//                    }
//                }
//
//                if (CollectionUtils.isNotEmpty(user.getKubeconfigRoles())) {
//                    for (UserClusterRoleIssueVO roleIssueRow : user.getKubeconfigRoles()) {
//                        if (roleIssueRow.getClusterSeq().equals(cluster.getClusterSeq())) {
//                            roleIssueRow.setCluster(cluster);
//                        }
//                    }
//                }
            }

            // 사용자 휴면 여부
            userService.setUserSleepParam(user);
        }

        return user;
    }

    /**
     * Account 사용자 Mapping 등록
     *
     * @param accountSeq
     * @param accountUserSeqs
     * @param creator
     * @throws Exception
     */
    @Transactional(transactionManager = "transactionManager")
    public void addUserOfAccountMapping(Integer accountSeq, List<Integer> accountUserSeqs, Integer creator) throws Exception {
        IAccountMapper dao = sqlSession.getMapper(IAccountMapper.class);
        dao.addUserOfAccount(accountSeq, accountUserSeqs, creator);
    }

    @Transactional(transactionManager = "transactionManager")
    public void addUserOfAccountMapping(Integer accountSeq, Integer accountUserSeq, Integer creator) throws Exception {
        this.addUserOfAccountMapping(accountSeq, Collections.singletonList(accountUserSeq), creator);
    }

    /**
     * Account System 사용자 Mapping 등록
     *
     * @param accountSeq
     * @param accountSystemUserSeqs
     * @param creator
     * @throws Exception
     */
    @Transactional(transactionManager = "transactionManager")
    public void addSystemUserOfAccountMapping(Integer accountSeq, List<Integer> accountSystemUserSeqs, Integer creator) throws Exception {
        IAccountMapper dao = sqlSession.getMapper(IAccountMapper.class);
        dao.addSystemUserOfAccount(accountSeq, accountSystemUserSeqs, creator);
    }

    @Transactional(transactionManager = "transactionManager")
    public void addSystemUserOfAccountMapping(Integer accountSeq, Integer accountSystemUserSeq, Integer creator) throws Exception {
        this.addSystemUserOfAccountMapping(accountSeq, Collections.singletonList(accountSystemUserSeq), creator);
    }

    /**
     * ProviderAccount 생성
     *
     * @param account
     * @throws Exception
     */
    @Transactional(transactionManager = "transactionManager")
    public void addProviderAccounts(AccountVO account) throws Exception {
        if (CollectionUtils.isNotEmpty(account.getProviderAccounts())) {
            /**
             * 프로바이더 테이블에 먼저 데이터를 입력
             */
            List<Integer> providerSeqList = new ArrayList<>();
            if (CollectionUtils.isNotEmpty(account.getProviderAccounts())) {
                for (ProviderAccountVO providerAccountRow : account.getProviderAccounts()) {
                    this.addProviderAccount(providerAccountRow, providerSeqList);
                }
            }

            /**
             * 계정 프로바이더계정 매핑 테이블에 데이터 입력
             */
            this.addProviderOfAccount(account.getAccountSeq(), providerSeqList, ContextHolder.exeContext().getUserSeq());
        }
    }

    /**
     * ProviderAccount 생성
     *
     * @param providerAccount
     * @param newProviderSeqList
     * @throws Exception
     */
    @Transactional(transactionManager = "transactionManager")
    public void addProviderAccount(ProviderAccountVO providerAccount, List<Integer> newProviderSeqList) throws Exception {
        providerAccountService.addProviderAccountProcess(providerAccount);
        newProviderSeqList.add(providerAccount.getProviderAccountSeq());
    }

    /**
     * Account & ProviderAccount Mapping 생성
     *
     * @param accountSeq
     * @param providerSeqs
     * @param creator
     * @throws Exception
     */
    @Transactional(transactionManager = "transactionManager")
    public void addProviderOfAccount(Integer accountSeq, List<Integer> providerSeqs, Integer creator) throws Exception {
        /**
         * 계정 프로바이더계정 매핑 테이블에 데이터 입력
         */
        if(CollectionUtils.isNotEmpty(providerSeqs)) {
            IAccountMapper dao = sqlSession.getMapper(IAccountMapper.class);
            dao.addProviderOfAccount(accountSeq, providerSeqs, creator);
        }
    }

    /**
     * ProviderAccount 삭제
     *
     * @param providerAccount
     * @param updater
     * @throws Exception
     */
    @Transactional(transactionManager = "transactionManager")
    public void removeProviderAccount(ProviderAccountVO providerAccount, List<Integer> removeProviderSeqList, boolean cascade, Integer updater) throws Exception {
        List<String> clusters = providerAccountService.getClusterUsingProviderAccount(providerAccount.getProviderAccountSeq(), providerAccount.getAccountUseType());
        if (CollectionUtils.isNotEmpty(clusters) && !cascade) {
            throw new CocktailException(String.format("Provider account used by cluster(s): %s", clusters),
                    ExceptionType.ProviderUsedByCluster);
        } else {
            // providerAccount 삭제
            providerAccount.setUseYn("N");
            providerAccount.setUpdater(updater);
            if (cascade) {
                providerAccountService.deleteProviderAccount(providerAccount);
            } else {
                providerAccountService.removeProviderAccountFromAccount(providerAccount);
            }
            removeProviderSeqList.add(providerAccount.getProviderAccountSeq());
        }
    }

    /**
     * Account 수정.
     * @param account
     * @throws Exception
     */
    @Transactional(transactionManager = "transactionManager")
    public void editAccount(AccountVO account) throws Exception {
        IAccountMapper accountDao = sqlSession.getMapper(IAccountMapper.class);

        /**
         * header 정보로 요청 사용자 권한 체크
         */
        AuthUtils.isValid(ContextHolder.exeContext(), AuthUtils.checkUserSysUserNDevOpsBlockAuthPredicate());

        /**
         * Single Tenancy
         * Multi Tenancy -> Single Tenancy로의 Downgrade는 불가능.
         * Previous Account Grade가 Single Tenancy가 아닌데 Single Tenancy로 설정하려고 하면 오류
         */
        AccountGradeVO prevAccountGrade = accountDao.getAccountGrade(null, account.getAccountSeq(), null);
        if(account.getAccountGrade() != null &&
            gradePlanService.isSingleTenancy(account.getAccountGrade().getGradeSeq()) &&
            !gradePlanService.isSingleTenancy(prevAccountGrade)) {
            throw new CocktailException("Could not downgrade to single tenancy plan.", ExceptionType.CouldNotDownGradeToSingleTenancy);
        }

        /** ======================================================
         * 1. Registry 등록 / 수정 / 삭제
         * - Account 관리에서 제거되고 별도 관리 체계로... : 2020.06.18
         */
//        if(account.getAccountType().isCubeEngine() && CollectionUtils.isNotEmpty(account.getProjects())) {
//            throw new CocktailException("CubeEngine Platform can't have a Registry", ExceptionType.CubeEngineAccountCanNotHaveRegistry);
//        }
//
//        /** 1.1. Registry Data Set 구성 **/
//        // 입력받은 Registry
//        List<ServiceRegistryVO> receivedServiceRegistries = account.getProjects();
//        if(CollectionUtils.isEmpty(receivedServiceRegistries)) {
//            receivedServiceRegistries = new ArrayList<>();
//        }
//        // 현재 Registry
//        List<ServiceRegistryVO> currentServiceRegistries = serviceService.getServiceRegistryOfAccount(account.getAccountSeq(), ServiceRegistryType.SERVICE.getCode(), null, ServiceType.PLATFORM.getCode());
//        if(CollectionUtils.isEmpty(currentServiceRegistries)) {
//            currentServiceRegistries = new ArrayList<>();
//        }
//        // 현재 Registry to Map
//        Map<Integer, ServiceRegistryVO> currentServiceRegistriesMap = currentServiceRegistries.stream().collect(Collectors.toMap(ServiceRegistryVO::getProjectId, Function.identity()));
//        // Get Platform 서비스
//        ServiceVO platformService = serviceService.getServiceByType(account.getAccountSeq(), null);
//        platformService.setAccount(account);
//
//        /** 1.2. 등록 / 수정 / 삭제 구분에 따른 Validation 처리 **/
//        serviceService.checkServiceRegistryCommonValidation(platformService, receivedServiceRegistries, currentServiceRegistries, currentServiceRegistriesMap);
//
//
//        /** 1.3. 입력 / 수정 / 삭제 구분에 따라 레지스트리 등록 / 수정 / 삭제 처리 **/
//        serviceService.serviceRegistriesManaging(platformService, receivedServiceRegistries);


        /** ======================================================
         * 2. AccessKey Type ProviderAccount 등록 / 수정 / 삭제
         */
        /** 2.1. ProviderAccount Data Set 구성 **/
        List<ProviderAccountVO> receivedProviderAccounts = new ArrayList<>();
        List<ProviderAccountVO> currentProviderAccounts = new ArrayList<>();
        /** 2.1.1. Type 1. Metering ProviderAccount 수정 : 2020.03.12 기준 Metering 기능 제거 예정 (일단 그대로 유지) => 2020.05.21 : Metering 기능 제거됨 => 현재 사용안함 */
        List<ProviderAccountVO> rcvMeteringProviderAccounts = Optional.ofNullable(account.getProviderAccounts()).orElseGet(() -> Lists.newArrayList()).stream().filter(pa -> (pa.getAccountUseType() == ProviderAccountType.METERING)).collect(Collectors.toList());
        if(CollectionUtils.isNotEmpty(rcvMeteringProviderAccounts)) { // Received 미터링 정보 추가
            receivedProviderAccounts.addAll(rcvMeteringProviderAccounts);
        }
        List<ProviderAccountVO> currMeteringProviderAccounts = providerAccountService.getProviderAccounts(null, ProviderAccountType.METERING.getCode(), account.getAccountSeq(), null, "Y");
        if(CollectionUtils.isNotEmpty(currMeteringProviderAccounts)) { // Current 미터링 정보 추가...
            currentProviderAccounts.addAll(currMeteringProviderAccounts);
        }

        /** 2.1.2. Type 2. AccessKey Type ProviderAccount 수정 */
        List<ProviderAccountVO> rcvAccessKeyProviderAccounts = account.getProviderAccounts().stream().filter(pa -> (pa.getAccountUseType() == ProviderAccountType.ACCESS_KEY)).collect(Collectors.toList());
        if(CollectionUtils.isNotEmpty(rcvAccessKeyProviderAccounts)) { // Received Access Key 정보 추가
            receivedProviderAccounts.addAll(rcvAccessKeyProviderAccounts);
        }
        List<ProviderAccountVO> currAccessKeyProviderAccounts = providerAccountService.getProviderAccounts(null, ProviderAccountType.ACCESS_KEY.getCode(), account.getAccountSeq(), null, "Y");
        if(CollectionUtils.isNotEmpty(currAccessKeyProviderAccounts)) { // Current Access Key 정보 추가...
            currentProviderAccounts.addAll(currAccessKeyProviderAccounts);
        }

        Map<Integer, ProviderAccountVO> currentProviderAccountsMap = currentProviderAccounts.stream().filter(pa -> (pa.getProviderAccountSeq() > 0)).collect(Collectors.toMap(ProviderAccountVO::getProviderAccountSeq, Function.identity()));

        // 신규로 등록된 Provider Account List를 보관 -> Account와 매핑시 사용.
        List<Integer> newProviderSeqList = new ArrayList<>();
        // 삭제된 Provider Account List를 보관 -> Account와 매핑 해제시 사용.
        List<Integer> removeProviderSeqList = new ArrayList<>();

        /** 2.2. 입력 / 수정 / 삭제 구분에 따른 처리 **/
        for (ProviderAccountVO receivedProviderAccountRow : receivedProviderAccounts) {
            if (receivedProviderAccountRow.getModifyType() == null) {
                receivedProviderAccountRow.setModifyType("N");
            }

            CRUDCommand modifyType = null;
            try {
                modifyType = CRUDCommand.valueOf(receivedProviderAccountRow.getModifyType());
            }
            catch (Exception ex) {
                throw new CocktailException(String.format("Invalid Use Type. [%s]", receivedProviderAccountRow.getModifyType()), ExceptionType.InvalidInputData);
            }
            switch (modifyType) {
                case C: // Create
                    if (currentProviderAccountsMap.containsKey((receivedProviderAccountRow.getProviderAccountSeq()))) {
                        throw new CocktailException("The provider account is already exists in account.", ExceptionType.ProviderAccountExistsInAccount);
                    }
                    this.addProviderAccount(receivedProviderAccountRow, newProviderSeqList);
                    break;
                case U: // Update
                    // Validation 체크
                    providerAccountService.checkProviderAccountValidation(receivedProviderAccountRow);
                    providerAccountService.setProviderAccount(receivedProviderAccountRow, true);
                    /** 변경된 Provider계정에 대한 수정 처리. **/
                    providerAccountService.editProviderAccountFromAccount(receivedProviderAccountRow);
                    break;
                case D: // Delete
                    this.removeProviderAccount(receivedProviderAccountRow, removeProviderSeqList, false, ContextHolder.exeContext().getUserSeq());
                    break;
                case N: // No Change
                    break;
                default:
                    throw new CocktailException(String.format("Invalid Use Type. [%s]", receivedProviderAccountRow.getModifyType()), ExceptionType.InvalidInputData);
            }
        }

        /** 2.3. Account와 매핑 정보 구성 **/
        // mapping 정보 생성
        if (CollectionUtils.isNotEmpty(newProviderSeqList)) {
            accountDao.addProviderOfAccount(account.getAccountSeq(), newProviderSeqList, ContextHolder.exeContext().getUserSeq());
        }
        // mapping 정보 삭제
        if (CollectionUtils.isNotEmpty(removeProviderSeqList)) {
            accountDao.removeProviderOfAccount(account.getAccountSeq(), removeProviderSeqList);
        }

        /** ======================================================
         * 3. AccessKey Type ProviderAccount와 클러스터간 연결 정보 구성
         */
        /** 3.1. DataSet 구성 **/
        List<ClusterProviderVO> receivedClusters = account.getClusterProviders();
        if (CollectionUtils.isEmpty(receivedClusters)) {
            receivedClusters = new ArrayList<>();
        }

        List<ClusterProviderVO> currentClusters = accountDao.getClustersProviderOfAccount(account.getAccountSeq(), ProviderAccountType.ACCESS_KEY.getCode());
        List<Integer> currentClusterSeqs = Optional.ofNullable(currentClusters).orElseGet(() ->Lists.newArrayList()).stream().map(ClusterProviderVO::getClusterSeq).collect(Collectors.toList());
        List<Integer> receivedClusterSeqs = receivedClusters.stream().map(ClusterProviderVO::getClusterSeq).collect(Collectors.toList());

        /** 3.2. 클러스터 삭제 / 수정 / 추가 데이터 분류 **/
        @SuppressWarnings("unchecked")
        List<Integer> deleteClusterSeqs = ListUtils.subtract(currentClusterSeqs, receivedClusterSeqs);
        @SuppressWarnings("unchecked")
        List<Integer> updateClusterSeqs = ListUtils.intersection(receivedClusterSeqs, currentClusterSeqs);
        @SuppressWarnings("unchecked")
        List<Integer> addClusterSeqs = ListUtils.subtract(receivedClusterSeqs, currentClusterSeqs);

        /** 3.3. 데이터 분류에 따른 처리 **/
        // 클러스터 삭제
        if (CollectionUtils.isNotEmpty(deleteClusterSeqs)) {
            /** 삭제 대상 클러스터중 EKS 클러스터가 있는지 확인.. **/
            List<Integer> eksClustersSeq = new ArrayList<>();
            for(Integer clusterSeqRow : deleteClusterSeqs) { // 전체 삭제대상 클러스터
                ClusterVO clusterRow = accountDao.getClusterSummary(clusterSeqRow); // 삭제대상 클러스터의 상세 정보를 찾은 후
                if(clusterRow.getCubeType() == CubeType.EKS) { // 삭제대상 클러스터가 EKS 일 경우에만
                    eksClustersSeq.add(clusterSeqRow); // 처리 대상에 포함.
                }
            }

            if(CollectionUtils.isNotEmpty(eksClustersSeq)) { /** 해당 발급 계정으로 클러스터에 발급된 사용자가 있다면 삭제 불가 (EKS type일 경우에만...) **/
                List<UserClusterRoleIssueVO> kubeconfigRoles = userClusterRoleIssueService.getUserClusterRoleIssues(null, eksClustersSeq, null, null, null, null, true);
                if (CollectionUtils.isNotEmpty(kubeconfigRoles)) {
                    List<UserVO> users = userService.getUsersByUserSeqs(new ArrayList<>(kubeconfigRoles.stream().map(UserClusterRoleIssueVO::getUserSeq).collect(Collectors.toSet())));
                    throw new CocktailException("User accounts issued to the cluster exist. Please try again after retrieving permission.", ExceptionType.IssuedUserAccountClusterExist
                        , JsonUtils.toGson(users.stream().map(UserVO::getUserId).collect(Collectors.toList())));
                }
            }
            // 2020.03.13 : ClusterService에서 accountService를 사용하고 있어 순환 참조 문제로 accountDao에 구현함..
            accountDao.updateClustersCloudProviderAccount(deleteClusterSeqs, null, ContextHolder.exeContext().getUserSeq());
        }
        // 클러스터 수정 (AccessKey)
        if (CollectionUtils.isNotEmpty(updateClusterSeqs)) {
            for (ClusterProviderVO cpRow : receivedClusters) {
                if (updateClusterSeqs.contains(cpRow.getClusterSeq())) {
                    List<Integer> updateCluster = new ArrayList<>();
                    updateCluster.add(cpRow.getClusterSeq());
                    if(!currentProviderAccountsMap.containsKey(cpRow.getProviderAccountSeq())) {
                        //Provider Account에 존재하지 않는 Key 매핑시 오류..
                        throw new CocktailException(String.format("Provider Account not found : %s ", cpRow.getProviderAccountSeq()), ExceptionType.ProviderNotFound);
                    }
                    accountDao.updateClustersCloudProviderAccount(updateCluster, cpRow.getProviderAccountSeq(), ContextHolder.exeContext().getUserSeq());
                }
            }
        }
        // 클러스터 추가
        if (CollectionUtils.isNotEmpty(addClusterSeqs)) {
            List<ClusterProviderVO> addClusters = receivedClusters.stream().filter(sc -> (addClusterSeqs.contains(sc.getClusterSeq()))).collect(Collectors.toList());
            for (ClusterProviderVO cpRow : addClusters) {
                List<Integer> addCluster = new ArrayList<>();
                addCluster.add(cpRow.getClusterSeq());
                if(!currentProviderAccountsMap.containsKey(cpRow.getProviderAccountSeq())) {
                    //Provider Account에 존재하지 않는 Key 매핑시 오류..
                    throw new CocktailException(String.format("Provider Account not found : %s ", cpRow.getProviderAccountSeq()), ExceptionType.ProviderNotFound);
                }
                accountDao.updateClustersCloudProviderAccount(addCluster, cpRow.getProviderAccountSeq(), ContextHolder.exeContext().getUserSeq());
            }
        }

        /**
         * edit Account
         */
        if (!UserRole.valueOf(ContextHolder.exeContext().getUserRole()).isAdmin()) {
            // Admin이 아니면 AccountType은 수정이 불가능함...
            account.setAccountType(null);
            // Admin이 아니면 accountConfig 수정 불가
            AccountVO curr = accountDao.getAccount(account.getAccountSeq());
            if (curr != null) {
                account.setAccountConfig(curr.getAccountConfig());
            }

        }
        account.setAccountUserSeq(null); // 수정불가
        accountDao.editAccount(account);

        /**
         * 이전 Plan이 Single Tenancy 플랜이면 워크스페이스 이름 / 설명을 플랫폼과 동일하게 설정한다.
         * (Plan 변경이 있어도 현재 Single Tenancy 플랜이면 최종 정보로 Update 해주고 변경되도록 함)
         */
        if(gradePlanService.isSingleTenancy(prevAccountGrade)) {
            // Single Tenancy 워크스페이스 조회
            ServiceVO singleWorkspace = serviceService.getNormalService(account.getAccountSeq(), null);
            // Single Tenancy 워크스페이스 Update
            if(singleWorkspace != null) { // Frontend Input 오류등으로 실제 Grade가 SingleTenancy가 아닌데 GradeName이 STP로 들어오는 경우가 있음.. 해당 케이스는 처리하지 않음...
                ServiceAddVO updateService = new ServiceAddVO();
                updateService.setServiceSeq(singleWorkspace.getServiceSeq());
                updateService.setServiceName(account.getAccountName());
                updateService.setDescription(account.getDescription());
                updateService.setUpdater(ContextHolder.exeContext().getUserSeq());
                serviceService.updateService(updateService);
            }
        }

        /**
         * 2019-04-22, coolingi, 계정 grade plan 정보 수정
         * 2019-05-29, coolingi, 로직을 editAccountGrade로 이동
         */
        if (UserRole.valueOf(ContextHolder.exeContext().getUserRole()).isAdmin()) {
            // Admin일 경우에만 위의 AccountType 수정 및 Grade 수정이 가능함...
            accountGradeService.editAccountGrade(account);
        }

        /**
         * Account Registry 등록, 2021-11-23
         */
        if (account.getAccountRegistry() != null){
            AccountRegistryVO accountRegistry = account.getAccountRegistry();

            // 기존에 등록된 registry 정보가 있을때만 수정
            AccountRegistryVO currentAccountRegistry = accountRegistryService.getAccountRegistry(accountRegistry.getAccountSeq(), accountRegistry.getAccountRegistrySeq(), false);
            if( currentAccountRegistry != null) {
                accountRegistryService.editAccountRegistry(accountRegistry);
            }
        }
    }

    /**
     * Account accountCode, userAuthType 수정.
     * @param account
     * @throws Exception
     */
    @Transactional(transactionManager = "transactionManager")
    public void editAccountInfoForAD(AccountVO account) throws Exception {
        IAccountMapper accountDao = sqlSession.getMapper(IAccountMapper.class);

        accountDao.editAccountInfoForAD(account);
    }

//    /**
//     * Account 정보 수정.
//     * @param account
//     * @return
//     * @throws Exception
//     */
//    @Transactional(transactionManager = "transactionManager")
//    public void editAccountInfo(AccountVO account) throws Exception {
//        IAccountMapper dao = sqlSession.getMapper(IAccountMapper.class);
//
//        /**
//         * edit Account
//         */
//        account.setAccountUserSeq(null); // 수정불가
//        dao.editAccount(account);
//
//        /**
//         * 2019-04-22, coolingi, 계정 grade plan 정보 수정
//         * 2019-05-29, coolingi, 로직을 editAccountGrade로 이동
//         */
//        editAccountGrade(account);
//
//        /**
//         * edit Account User Info
//         */
//        userService.editUserFromAccount(account.getAccountUser());
//    }

    /**
     * Account 삭제.
     * @param accountSeq
     * @param cascade
     * @return
     * @throws Exception
     */
    @Transactional(transactionManager = "transactionManager")
    public int removeAccount(Integer accountSeq, boolean cascade) throws Exception {
        IAccountMapper dao = sqlSession.getMapper(IAccountMapper.class);
        IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);

        int result = 0;

        // Platform Workspace는 제외하고 조회
        AccountVO accountInfo = dao.getAccountDetailInfo(accountSeq);

        if (accountInfo != null) {
            AccountGradeVO prevAccountGrade = dao.getAccountGrade(null, accountSeq, null);
            boolean isSingleTenancy = gradePlanService.isSingleTenancy(prevAccountGrade); // for Single Tenancy Check

            boolean isSecurityOnline = ServiceMode.valueOf(cocktailServiceProperties.getMode()) == ServiceMode.SECURITY_ONLINE;

            // 워크스페이스 존제시 삭제 불가
            // Single Tenancy일 경우에는 기본 워크스페이스가 있어도 함께 삭제하도록 한다. (checkCnt = 1)
            int checkCnt = isSingleTenancy ? 1 : 0;
            if (accountInfo.getServiceCount().intValue() > checkCnt) {
                if (!cascade) {
                    throw new CocktailException(String.format("workspace exists in account : %d", checkCnt), ExceptionType.AccountHaveWorkspace);
                }
            }

            // 클러스터 존재시 삭제 불가
            List<ClusterVO> clusters = clusterDao.getClusters(accountSeq, null, null, null, null, null);
            if (CollectionUtils.isNotEmpty(clusters)) {
                if (cascade) {
                    for (ClusterVO cluster : clusters) {
                        clusterService.removeCluster(cluster, true, ContextHolder.exeContext().getUserSeq());
                    }
                } else {
                    if (clusters.stream().anyMatch(c -> (BooleanUtils.toBoolean(c.getUseYn())))) {
                        throw new CocktailException("cluster exists in account.", ExceptionType.AccountHaveClusters);
                    }
                }
            }

            /**
             * ProviderAccount 삭제
             */
            List<ProviderAccountVO> currProviderAccounts = providerAccountService.getProviderAccounts(null, Arrays.asList(ProviderAccountType.ACCESS_KEY.getCode(), ProviderAccountType.METERING.getCode()), accountSeq, null, null);
            if (CollectionUtils.isNotEmpty(currProviderAccounts)) {
                List<Integer> removeProviderSeqList = new ArrayList<>();
                // ProviderAccount 삭제
                for (ProviderAccountVO currProviderAccountRow : currProviderAccounts) {
                    this.removeProviderAccount(currProviderAccountRow, removeProviderSeqList, cascade, ContextHolder.exeContext().getUserSeq());
                }
                // mapping 정보 삭제
                if (CollectionUtils.isNotEmpty(removeProviderSeqList)) {
                    dao.removeProviderOfAccount(accountSeq, removeProviderSeqList);
                }
            }

            if (ServiceMode.valueOf(cocktailServiceProperties.getMode()).isAddUser()) {
                // 현재 Account에 등록되어 있는 사용자 조회.
                List<UserVO> users = userService.selectUsersByAccount(accountSeq, null);
                // 기존에 매핑된 사용자가 있으면 삭제할 수 없음.
                // => 사용자 매핑은 끊고 삭제해도 될 것 같습니다만.. 일단 기존 로직 유지...
                if (CollectionUtils.isNotEmpty(users)) {
                    // 싱글 테넌시일때라도 사용자 연결을 끊고 플랫폼을 삭제하도록 함
                    if (cascade) {
                        for (UserVO user : users) {
                            this.removeUser(accountSeq, user.getUserSeq(), true);
                        }
                    } else {
                        throw new CocktailException("users exists in account.", ExceptionType.UsersExistsInAccount);
                    }
                }

                // SYSTEM 권한자 삭제 (2021.02.09 : System 권한을 조회하지 않고 삭제 -> 지금까지 계속 삭제되지 않고 있었음... Patch)
                List<UserVO> systemUsers = userService.selectSystemUsersByAccount(accountSeq, null);
                if (CollectionUtils.isNotEmpty(systemUsers)) {
                    for (UserVO user : systemUsers) {
                        this.removeUser(accountSeq, user.getUserSeq(), true);
                    }

                }
            }

            // account grade plan 삭제, 2019-04-22, coolingi
            AccountGradeVO accountGradeVO = new AccountGradeVO();
            accountGradeVO.setAccountSeq(accountSeq);
            List<AccountGradeVO> accountGradeList = accountGradeService.getAccountGrades(accountGradeVO, null); // 해당 계정에 사용중인건 모두 조회
            for (AccountGradeVO gradeVO : accountGradeList) {
                if (cascade || isSecurityOnline) {
                    accountGradeService.deleteAccountBillInfo(accountSeq);
                } else {
                    accountGradeService.removeAccountGrade(gradeVO.getAccountGradeSeq());
                }
            }

            /**
             * R4.4.0 : 2020.06.30 : 플랫폼 삭제시 플랫폼 워크스페이스 삭제 기능 추가
             * - 워크스페이스, 클러스터는 등록되어 있지 않은 상태.. Method 진입시 등록되어 있으면 throw 하도록 되어 있음...
             **/
            if (cascade || isSecurityOnline) {
                serviceService.deleteWorkspaceByServiceType(accountSeq, null);
            } else {
                serviceService.removePlatformWorkspace(accountSeq);
            }

            /**
             * R4.6.5 : 2021.12.03 : account_registry 삭제 추가
             * on delete cascade 옵션이 있으나 deleteAccount시 외래키 무시 하기 때문에 삭제 로직 추가
             */
            AccountRegistryVO accountRegistry = accountRegistryService.getAccountRegistry(accountSeq);
            if (accountRegistry != null) {
                accountRegistryService.deleteAccountRegistry(accountRegistry.getAccountRegistrySeq());
            }

            /**
             * R4.6.5 : 2022.01.21 : account_application 삭제 추가
             */
            if (isSecurityOnline) {
                IAccountApplicationMapper mapper = sqlSession.getMapper(IAccountApplicationMapper.class);
                AccountApplicationVO accountApplication = mapper.getDetailByUser(accountInfo.getAccountCode(), null, null);
                if (accountApplication != null) {
                    mapper.deleteAccountApplication(accountApplication.getAccountApplicationSeq());
                }
            }

            //remove Account
            if (cascade || isSecurityOnline) {
                result = dao.deleteAccount(accountSeq);
            } else {
                result = dao.removeAccount(accountInfo);
            }
        } else {
            throw new CocktailException("Account not found.", ExceptionType.AccountNotFound);
        }

        return result;
    }

    /**
     * Account 중복 체크
     * @param account
     * @return
     * @throws Exception
     */
    public boolean checkDuplicateAccount(AccountVO account) throws Exception {
        IAccountMapper dao = sqlSession.getMapper(IAccountMapper.class);

        int accountCnt = 0;
        if(StringUtils.isNotBlank(account.getAccountName())){
            accountCnt += dao.getAccountCountByName(account.getAccountName());
        }
        if(StringUtils.isNotBlank(account.getAccountCode())){
            accountCnt += dao.getAccountCountByCode(account.getAccountCode());
        }

        return accountCnt > 0;
    }


    /**
     * Account 간단 조회 (삭제된 Account도 조회)
     * @param accountSeq
     * @return
     * @throws Exception
     */
    public AccountVO getAccountSimple(Integer accountSeq) throws Exception {
        IAccountMapper dao = sqlSession.getMapper(IAccountMapper.class);
        // get Account
        return dao.getAccountSimple(accountSeq);
    }

    /**
     * Account 간단 조회 (코드로 조회, use_yn = 'Y')
     * @param accountCode
     * @return
     * @throws Exception
     */
    public AccountVO getAccountSimpleByCode(String accountCode, String useYn) throws Exception {
        IAccountMapper dao = sqlSession.getMapper(IAccountMapper.class);
        // get Account
        return dao.getAccountSimpleByCode(accountCode, useYn);
    }

    /**
     * Account 조회 (사용자 정보로.. )
     * @param loginUserSeq
     * @param loginUserRole
     * @return
     * @throws Exception
     */
    public AccountVO getAccountByUser(Integer loginUserSeq, String loginUserRole) throws Exception {
        IAccountMapper dao = sqlSession.getMapper(IAccountMapper.class);
        // get Account
        return dao.getAccountByUser(loginUserSeq, loginUserRole);
    }

    /**
     * Service Registry 생성 / 수정 / 삭제 관리 (CUD 단건 처리)
     * @param accountSeq
     * @param serviceRegistry
     * @param command
     * @throws Exception
     */
    @Transactional(transactionManager = "transactionManager")
    public ServiceRegistryVO serviceRegistryManager(Integer accountSeq, ServiceRegistryVO serviceRegistry, CRUDCommand command) throws Exception {
        serviceRegistry.setModifyType(command);
        serviceRegistry.setProjectType(ServiceRegistryType.SERVICE); // 서비스 레지스트리 신규등록/수정/삭제는 R4.4.0 부터 SERVICE Type만 유효함...

        List<ServiceRegistryVO> serviceRegistries = Collections.singletonList(serviceRegistry);
        serviceRegistries = this.serviceRegistryManager(accountSeq, serviceRegistries);

        return serviceRegistries.get(0);
    }

    /**
     * Service Registry 생성 / 수정 / 삭제 관리
     * @param accountSeq
     * @param serviceRegistries
     * @return
     * @throws Exception
     */
    public List<ServiceRegistryVO> serviceRegistryManager(Integer accountSeq, List<ServiceRegistryVO> serviceRegistries) throws Exception {
        AccountVO account = this.getAccount(accountSeq, true);

        /** Single Tenancy에서 레지스트리 추가시 체크로직 추가 **/
        if(gradePlanService.isSingleTenancy(account.getAccountGrade())) {
            int count = serviceService.getServicesCountByType(accountSeq, null, ServiceType.NORMAL.getCode());
            if(count != 1) {
                // Single Tenancy가 워크스페이스를 여러개 가지고 있거나 없다면 오류 -> 다음 처리에서 어느 워크스페이스에 연결해야 할지 판단 어려움. 사전 체크.
                throw new CocktailException("Invalid Account - Invalid Single Tenancy Platform", ExceptionType.InvalidState);
            }
        }

        /** ======================================================
         * 1. Registry 등록 / 수정 / 삭제
         */
        if(account.getAccountType().isCubeEngine() && CollectionUtils.isNotEmpty(account.getProjects())) {
            throw new CocktailException("CubeEngine Platform can't have a Registry", ExceptionType.CubeEngineAccountCanNotHaveRegistry);
        }

        /** 1.1. Registry Data Set 구성 **/
        // 입력받은 Registry
        List<ServiceRegistryVO> receivedServiceRegistries = serviceRegistries;
        if(CollectionUtils.isEmpty(receivedServiceRegistries)) {
            receivedServiceRegistries = new ArrayList<>();
        }

        // 현재 Registry
        List<ServiceRegistryVO> currentServiceRegistries = serviceService.getServiceRegistryOfAccount(account.getAccountSeq(), ServiceRegistryType.SERVICE.getCode(), null, ServiceType.PLATFORM.getCode());
        if(CollectionUtils.isEmpty(currentServiceRegistries)) {
            currentServiceRegistries = new ArrayList<>();
        }

        /**
         * 플랫폼 레지스트리 pull 전용 사용자 생성
         */
        this.createAccountRegistryPullUser(account);

        // 현재 Registry to Map
        Map<Integer, ServiceRegistryVO> currentServiceRegistriesMap = currentServiceRegistries.stream().collect(Collectors.toMap(ServiceRegistryVO::getProjectId, Function.identity()));
        // Get Platform 서비스
        ServiceDetailVO platformService = serviceService.getPlatformService(account.getAccountSeq(), null);
        platformService.setAccount(account);

        /** 1.2. 등록 / 수정 / 삭제 구분에 따른 Validation 처리 **/
        serviceValidService.checkServiceRegistryCommonValidation(accountSeq, platformService, receivedServiceRegistries, currentServiceRegistries, currentServiceRegistriesMap);

        /** 1.3. 입력 / 수정 / 삭제 구분에 따라 레지스트리 등록 / 수정 / 삭제 처리 **/
        serviceService.serviceRegistriesManaging(accountSeq, platformService, receivedServiceRegistries);

        return receivedServiceRegistries;
    }

    public List<ClusterVO> getAssignableClustersOfAccountForTenancy(Integer accountSeq, String clusterTenancy) throws Exception {
        return this.getAssignableClustersOfAccountForTenancy(accountSeq, null, clusterTenancy);
    }

    public List<ClusterVO> getAssignableClustersOfAccountForTenancy(Integer accountSeq, Integer serviceSeq, String clusterTenancy) throws Exception {
        IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
        return clusterDao.getAssignableClustersOfAccountForTenancy(accountSeq, serviceSeq, clusterTenancy);
    }


}
