package run.acloud.api.configuration.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import run.acloud.api.auth.enums.UserRole;
import run.acloud.api.auth.vo.UserInactiveVO;
import run.acloud.api.auth.vo.UserOtpVO;
import run.acloud.api.auth.vo.UserVO;
import run.acloud.api.configuration.enums.AccountType;
import run.acloud.api.configuration.enums.ServiceRegistryType;
import run.acloud.api.configuration.enums.ServiceType;
import run.acloud.api.configuration.service.*;
import run.acloud.api.configuration.util.ClusterUtils;
import run.acloud.api.configuration.vo.*;
import run.acloud.api.cserver.service.ServiceService;
import run.acloud.api.cserver.service.ServiceValidService;
import run.acloud.api.cserver.service.ServicemapService;
import run.acloud.api.cserver.vo.ServicemapSummaryAdditionalVO;
import run.acloud.api.resource.service.PersistentVolumeService;
import run.acloud.api.resource.vo.K8sPersistentVolumeClaimVO;
import run.acloud.commons.annotations.InHouse;
import run.acloud.commons.enums.ApiVersionType;
import run.acloud.commons.enums.CRUDCommand;
import run.acloud.commons.util.AuthUtils;
import run.acloud.commons.util.CryptoUtils;
import run.acloud.commons.util.PagingUtils;
import run.acloud.commons.vo.ExecutingContextVO;
import run.acloud.commons.vo.ResultVO;
import run.acloud.framework.context.ContextHolder;
import run.acloud.framework.enums.ExceptionBiz;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Tag(name = "Platform", description = "플랫폼에 대한 관리 기능을 제공한다.")
@Slf4j
@RequestMapping(value = "/api/account")
@RestController
@Validated
public class AccountController {

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountGradeService accountGradeService;

    @Autowired
    private UserClusterRoleIssueService userClusterRoleIssueService;

    @Autowired
    private ServiceService serviceService;

    @Autowired
    private ServiceValidService serviceValidService;

    @Autowired
    private ServicemapService servicemapService;

    @Autowired
    private UserService userService;

    @Autowired
    private ProviderAccountService providerAccountService;

    @Autowired
    private ClusterVolumeService clusterVolumeService;

    @Autowired
    private PersistentVolumeService persistentVolumeService;

    @Autowired
    private AccountRegistryService accountRegistryService;

    @GetMapping(value = "/list")
    @Operation(summary = "플랫폼 목록", description = "플랫폼 목록 조회한다.")
    public List<AccountVO> getAccounts(@RequestHeader(name = "user-id" ) Integer userSeq,
                                       @RequestHeader(name = "user-role" ) String userRole
                                        ) throws Exception {
        List<AccountVO> accounts;
        try {
            Map<String, Object> param = new HashMap<>();

            if (StringUtils.isNotBlank(userRole)) {
                // ACCOUNT 권한은 해당 계정의 목록만 조회함.
                if(UserRole.valueOf(userRole).isUserOfSystem()) {
                    param.put("accountUserSeq", userSeq);
                    param.put("userRole", userRole);
                }
                // ACCOUNT가 아니고 ADMIN도 아닌 사용자는 사용자가 속한 Account만 조회함.
                else if(UserRole.valueOf(userRole).isDevops()) {
                    param.put("userSeq", userSeq);
                    param.put("userRole", userRole);
                }
                else {
                    // ADMIN 권한은 모든 계정 목록을 조회함.
                    log.debug("trace log : userRole={}", userRole);
                }
            }

            /** Platform Workspace 갯수는 워크스페이스 갯수에서 제외하도록 함 **/
            param.put("excludePlatform", "Y");
            accounts = accountService.getAccounts(param);
        }
        catch (CocktailException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new CocktailException("getAccounts Fail.", ex, ExceptionType.CommonInquireFail);
        }

        return accounts;
    }

    @GetMapping(value = "/{accountSeq}")
    @Operation(summary = "플랫폼 상세", description = "플랫폼 상세 조회한다.")
    public AccountVO getAccount(@PathVariable Integer accountSeq) throws Exception {
        AccountVO account;
        try {
            account = this.accountService.getAccount(accountSeq, true);

            AccountRegistryVO accountRegistry = accountRegistryService.getAccountRegistry(accountSeq);
            if(accountRegistry != null){
                account.setAccountRegistry(accountRegistry);
            }
        }
        catch (CocktailException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new CocktailException("getAccount Fail.", ex, ExceptionType.CommonInquireFail);
        }

        return account;
    }

    @GetMapping(value = "/code/{accountCode}/check")
    @Operation(summary = "플랫폼 ID 중복 체크", description = "플랫폼 ID를 중복 체크한다.")
    public Map<String, Object> checkDuplicateAccount(
            @Parameter(name = "accountCode", description = "Account Code", required = true) @PathVariable String accountCode) throws Exception {
        log.debug("[BEGIN] checkDuplicateAccount");

        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("isValid", Boolean.TRUE);
        try {
            /**
             * header 정보로 요청 사용자 권한 체크
             */
            AuthUtils.checkUserAdminAuth(ContextHolder.exeContext());

            /**
             * accountName, accountCode 중복 체크.
             */
            AccountVO account = new AccountVO();
            account.setAccountCode(accountCode);
            if (accountService.checkDuplicateAccount(account)) {
                resultMap.put("isValid", Boolean.FALSE);
                return resultMap;
            }

        } catch (CocktailException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new CocktailException("checkDuplicateAccount Fail.", ex, ExceptionType.CommonInquireFail);
        }

        log.debug("[END  ] checkDuplicateAccount");

        return resultMap;
    }

    @PostMapping(value = "")
    @Operation(summary = "플랫폼 생성", description = "플랫폼 생성한다.")
    public AccountVO addAccount(
            @Parameter(name = "account", description = "Account 모델") @RequestBody AccountVO account
    ) throws Exception {
        log.debug("[BEGIN] addAccount");
        try {
            /**
             * header 정보로 요청 사용자 권한 체크
             */
            AuthUtils.checkUserAdminAuth(ContextHolder.exeContext());

            /**
             * 입력 데이터 Validation 체크
             */
            accountService.checkAccount(account);

            // online인 일때만 필수 체크.
            if(account.getAccountType() == AccountType.CCO && account.getAccountRegistry() == null){
                throw new CocktailException("Account Registry is null", ExceptionType.InvalidParameter_Empty);
            }

            /**
             * accountName, accountCode 중복 체크.
             */
            if (accountService.checkDuplicateAccount(account)) {
                throw new CocktailException("Account Already Exists", ExceptionType.AccountAlreadyExists);
            }

            /**
             * 생성자, 수정자, 사용여부등 기본 정보 설정.
             */
            account.setUseYn("Y");

            /**
             * Account 추가
             */
            accountService.addAccountInfo(account);

        } catch (CocktailException ex) {
            throw ex;
        } catch (Exception ex) {
            if(log.isDebugEnabled()) log.debug("trace log ", ex);
            throw new CocktailException("addAccount Fail.", ex, ExceptionType.CommonCreateFail);
        }

        log.debug("[END  ] addAccount");

        return account;
    }

    @PutMapping(value = "/{accountSeq}")
    @Operation(summary = "플랫폼 수정", description = "플랫폼 수정한다.")
    public void editAccount(
            @Parameter(name = "accountSeq", description = "accountSeq", required = true) @PathVariable Integer accountSeq,
            @Parameter(name = "account", description = "Account 모델") @RequestBody AccountVO account
    ) throws Exception {
        log.debug("[BEGIN] editAccount");
        try {

            /**
             * DevOps 권한의 사용자는 수정이 불가능함.
             */
            AuthUtils.checkAuth(ContextHolder.exeContext(), AuthUtils.checkUserSysUserNDevOpsBlockAuthPredicate());

            /**
             * 입력 데이터 Validation 체크
             */
            accountService.checkAccount(account);

            /**
             * Account Sequnece 설정
             */
            account.setAccountSeq(accountSeq);

            /**
             * Account 수정
             */
            accountService.editAccount(account);

        }
        catch (CocktailException ex) {
            throw ex;
        }
        catch (Exception ex) {
            if(log.isDebugEnabled()) log.debug("trace log ", ex);
            throw new CocktailException("editAccount Fail.", ex, ExceptionType.CommonCreateFail);
        }

        log.debug("[END  ] editAccount");
    }

    @DeleteMapping(value = "/{accountSeq}")
    @Operation(summary = "플랫폼 삭제", description = "플랫폼 삭제한다.")
    public AccountVO removeAccount(
            @Parameter(name = "accountSeq", description = "account seq", required = true)@PathVariable Integer accountSeq,
            @Parameter(name = "cascade", description = "종속된 정보(DB) 모두 삭제", schema = @Schema(allowableValues = {"true","false"}, defaultValue = "false")) @RequestParam(value = "cascade", required = false, defaultValue = "false") boolean cascade
    ) throws Exception {

        log.debug("[BEGIN] removeAccount");
        AccountVO returnVO;
        try {
            /**
             * DevOps 권한의 사용자는 수정이 불가능함.
             */
            AuthUtils.checkAuth(ContextHolder.exeContext(), AuthUtils.checkUserSysUserNDevOpsBlockAuthPredicate());
            returnVO = accountService.getAccountSimple(accountSeq);
            accountService.removeAccount(accountSeq, cascade);

        }
        catch (CocktailException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new CocktailException("removeAccount Fail.", ex, ExceptionType.CommonDeleteFail);
        }

        log.debug("[END  ] removeAccount");
        return returnVO;
    }

    @InHouse
    @GetMapping(value = "/{accountSeq}/servicemaps/summaries")
    @Operation(summary = "플랫폼 요약 정보 조회", description = "플랫폼 요약 정보를 조회한다.")
    public List<ServicemapSummaryAdditionalVO> getServicemapSummariesOfAccount(
            @Parameter(name = "accountSeq", description = "account seq", required = true) @PathVariable Integer accountSeq,
            @Parameter(name = "useStorage", description = "storage 정보 사용 여부", schema = @Schema(allowableValues = {"true","false"}, defaultValue = "false")) @RequestParam(value = "useStorage", required = false, defaultValue = "false") boolean useStorage,
            @Parameter(name = "useGateWay", description = "gateWay 정보 사용 여부", schema = @Schema(allowableValues = {"true","false"}, defaultValue = "false")) @RequestParam(value = "useGateWay", required = false, defaultValue = "false") boolean useGateWay,
            @Parameter(name = "useWorkload", description = "workload 정보 사용 여부", schema = @Schema(allowableValues = {"true","false"}, defaultValue = "false")) @RequestParam(value = "useWorkload", required = false, defaultValue = "false") boolean useWorkload,
            @Parameter(name = "useNamespace", description = "namespace 정보 사용 여부", schema = @Schema(allowableValues = {"true","false"}, defaultValue = "false")) @RequestParam(value = "useNamespace", required = false, defaultValue = "false") boolean useNamespace
    ) throws Exception {
        List<ServicemapSummaryAdditionalVO> result = new ArrayList<>();
        try {
            /**
             * DevOps 권한의 사용자는 수정이 불가능함.
             */
//            AuthUtils.checkAuth(ContextHolder.exeContext(), AuthUtils.checkUserDevOpsBlockAuthPredicate());

            result = servicemapService.getServicemapSummaries(accountSeq, useStorage, useGateWay, useWorkload, useNamespace);
        }
        catch (CocktailException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new CocktailException("getServicemapSummariesOfAccount Fail.", ex, ExceptionType.CommonInquireFail);
        }

        return result;
    }

    @GetMapping(value = "/{accountSeq}/cluster/role/issue")
    @Operation(summary = "사용자 클러스터 권한 발급 조회", description = "사용자 클러스터 권한 발급 조회한다.")
    public UserClusterRoleIssueListVO getUserClusterRoleIssueList (
            @Parameter(name = "accountSeq", description = "account seq", required = true) @PathVariable Integer accountSeq,
            @Parameter(name = "issueType", description = "발급 유형", schema = @Schema(allowableValues = {"SHELL","KUBECONFIG"}), required = true) @RequestParam(name = "issueType") String issueType,
            @Parameter(name = "clusterSeq", description = "clusterSeq") @RequestParam(name = "clusterSeq", required = false) Integer clusterSeq,
//        @Parameter(name = "order", description = "정렬 순서", schema = @Schema(allowableValues = {"DESC","ASC"})) @RequestParam(name = "order", required = false) String order,
//        @Parameter(name = "orderColumn", description = "정렬기준컬럼", schema = @Schema(allowableValues = {"created","audit_log_seq"})) @RequestParam(name = "orderColumn", required = false) String orderColumn,
            @Parameter(name = "nextPage", description = "요청페이지", required = true) @RequestParam(name = "nextPage") Integer nextPage,
            @Parameter(name = "itemPerPage", description = "페이지당 표시 갯수", required = true) @RequestParam(name = "itemPerPage") Integer itemPerPage,
            @Parameter(name = "searchColumn", description = "검색 컬럼", schema = @Schema(allowableValues = {"ACCOUNT_NAME","USER_ID","USER_NAME","ISSUE_USER_ID","ISSUE_USER_NAME"})) @RequestParam(name = "searchColumn", required = false) String searchColumn,
            @Parameter(name = "searchKeyword", description = "검색어") @RequestParam(name = "searchKeyword", required = false) String searchKeyword,
            @Parameter(name = "startDate", description = "검색 시작 일자", example = "20200421000000") @RequestParam(name = "startDate", required = false) String startDate,
            @Parameter(name = "endDate", description = "검색 종료 일자", example = "20201228000000") @RequestParam(name = "endDate", required = false) String endDate
    ) throws Exception {
        UserClusterRoleIssueListVO userClusterRoleIssueList;
        try {
            /**
             * 4.0.1 기준으로 DEVOPS 사용자는 접근 불가...
             */
            if(UserRole.valueOf(ContextHolder.exeContext().getUserRole()).isDevops()) {
                throw new CocktailException("The DEVOPS role user doesn't have Permission.", ExceptionType.NotAuthorizedToRequest);
            }
            PagingUtils.validatePagingParams(nextPage, itemPerPage);

            String orderColumn = "UPDATED";
            String order = "DESC";

            userClusterRoleIssueList = userClusterRoleIssueService.getUserClusterRoleIssueList(accountSeq, null, clusterSeq, issueType, order, orderColumn, nextPage, itemPerPage, searchColumn, searchKeyword, startDate, endDate);
        }
        catch (CocktailException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new CocktailException("getUserClusterRoleIssueList Fail.", ex, ExceptionType.CommonInquireFail);
        }

        return userClusterRoleIssueList;
    }

    @InHouse
    @GetMapping(value = "/{accountSeq}/cluster/role/issue/history/excel")
    @Operation(summary = "사용자 클러스터 권한 발급 이력 엑셀 다운로드", description = "사용자 클러스터 권한 발급 이력 엑셀 다운로드")
    public void downloadExcelUserClusterRoleIssueHistories(
            @Parameter(name = "accountSeq", description = "account seq", required = true) @PathVariable Integer accountSeq,
            @Parameter(name = "issueType", description = "발급 유형", schema = @Schema(allowableValues = {"SHELL","KUBECONFIG"}), required = true) @RequestParam(name = "issueType") String issueType,
            @Parameter(name = "clusterSeq", description = "clusterSeq") @RequestParam(name = "clusterSeq", required = false) Integer clusterSeq,
            @Parameter(name = "historyState", description = "발급 상태", schema = @Schema(allowableValues = {"GRANT","REVOKE","CHANGE","EXPIRE"})) @RequestParam(name = "historyState", required = false) String historyState,
            @Parameter(name = "searchColumn", description = "검색 컬럼", schema = @Schema(allowableValues = {"ACCOUNT_NAME","USER_ID","USER_NAME","ISSUE_USER_ID","ISSUE_USER_NAME"})) @RequestParam(name = "searchColumn", required = false) String searchColumn,
            @Parameter(name = "searchKeyword", description = "검색어") @RequestParam(name = "searchKeyword", required = false) String searchKeyword,
            @Parameter(name = "startDate", description = "검색 시작 일자", example = "20200421000000") @RequestParam(name = "startDate", required = false) String startDate,
            @Parameter(name = "endDate", description = "검색 종료 일자", example = "20201228000000") @RequestParam(name = "endDate", required = false) String endDate,
            HttpServletResponse response
    ) throws Exception {

        log.debug("[BEGIN] downloadExcelUserClusterRoleIssueHistories");

        /**
         * header 정보로 요청 사용자 권한 체크
         */
        AuthUtils.isValid(ContextHolder.exeContext(), AuthUtils.checkUserSysUserNDevOpsBlockAuthPredicate());

        UserClusterRoleIssueSearchVO params = userClusterRoleIssueService.setUserClusterRoleCommonParams(accountSeq, null, clusterSeq, issueType, historyState, searchColumn, searchKeyword, startDate, endDate, false);
        params.setPaging(PagingUtils.setPagingParams("HISTORY_SEQ", "DESC", null, null, null, null));

        userClusterRoleIssueService.downloadExcelUserClusterRoleIssueHistories(response, params);

        log.debug("[END  ] downloadExcelUserClusterRoleIssueHistories");

    }

    @InHouse
    @GetMapping(value = "/{accountSeq}/cluster/role/issue/excel")
    @Operation(summary = "사용자 클러스터 권한 발급 엑셀 다운로드", description = "사용자 클러스터 권한 발급 엑셀 다운로드")
    public void downloadExcelUserClusterRoleIssues (
            @Parameter(name = "accountSeq", description = "account seq", required = true) @PathVariable Integer accountSeq,
            @Parameter(name = "issueType", description = "발급 유형", schema = @Schema(allowableValues = {"SHELL","KUBECONFIG"}), required = true) @RequestParam(name = "issueType") String issueType,
            @Parameter(name = "clusterSeq", description = "clusterSeq") @RequestParam(name = "clusterSeq", required = false) Integer clusterSeq,
            @Parameter(name = "searchColumn", description = "검색 컬럼", schema = @Schema(allowableValues = {"ACCOUNT_NAME","USER_ID","USER_NAME","ISSUE_USER_ID","ISSUE_USER_NAME"})) @RequestParam(name = "searchColumn", required = false) String searchColumn,
            @Parameter(name = "searchKeyword", description = "검색어") @RequestParam(name = "searchKeyword", required = false) String searchKeyword,
            @Parameter(name = "startDate", description = "검색 시작 일자", example = "20200421000000") @RequestParam(name = "startDate", required = false) String startDate,
            @Parameter(name = "endDate", description = "검색 종료 일자", example = "20201228000000") @RequestParam(name = "endDate", required = false) String endDate,
            HttpServletResponse response
    ) throws Exception {
        log.debug("[BEGIN] downloadExcelUserClusterRoleIssues");

        /**
         * header 정보로 요청 사용자 권한 체크
         */
        AuthUtils.isValid(ContextHolder.exeContext(), AuthUtils.checkUserSysUserNDevOpsBlockAuthPredicate());

        UserClusterRoleIssueSearchVO params = userClusterRoleIssueService.setUserClusterRoleCommonParams(accountSeq, null, clusterSeq, issueType, null, searchColumn, searchKeyword, startDate, endDate, false);
        params.setPaging(PagingUtils.setPagingParams("UPDATED", "DESC", null, null, null, null));

        userClusterRoleIssueService.downloadExcelUserClusterRoleIssues(response, params);

        log.debug("[END  ] downloadExcelUserClusterRoleIssues");
    }

    @InHouse
    @GetMapping(value = "/{accountSeq}/cluster/shell/connect/history/excel")
    @Operation(summary = "사용자 클러스터 쉘 접속 이력 엑셀 다운로드", description = "사용자 클러스터 쉘 접속 이력 엑셀 다운로드")
    public void downloadExcelUserShellConnectHistories(
            @Parameter(name = "accountSeq", description = "account seq", required = true) @PathVariable Integer accountSeq,
            @Parameter(name = "clusterSeq", description = "clusterSeq") @RequestParam(name = "clusterSeq", required = false) Integer clusterSeq,
            @Parameter(name = "historyState", description = "발급 상태", schema = @Schema(allowableValues = {"GRANT","REVOKE","CHANGE"})) @RequestParam(name = "historyState", required = false) String historyState,
            @Parameter(name = "searchColumn", description = "검색 컬럼", schema = @Schema(allowableValues = {"ACCOUNT_NAME","USER_ID","USER_NAME","ISSUE_USER_ID","ISSUE_USER_NAME"})) @RequestParam(name = "searchColumn", required = false) String searchColumn,
            @Parameter(name = "searchKeyword", description = "검색어") @RequestParam(name = "searchKeyword", required = false) String searchKeyword,
            @Parameter(name = "startDate", description = "검색 시작 일자", example = "20200421000000") @RequestParam(name = "startDate", required = false) String startDate,
            @Parameter(name = "endDate", description = "검색 종료 일자", example = "20200428000000") @RequestParam(name = "endDate", required = false) String endDate,
            HttpServletResponse response
    ) throws Exception {

        log.debug("[BEGIN] downloadExcelUserShellConnectHistories");

        /**
         * header 정보로 요청 사용자 권한 체크
         */
        AuthUtils.isValid(ContextHolder.exeContext(), AuthUtils.checkUserSysUserNDevOpsBlockAuthPredicate());

        UserClusterRoleIssueSearchVO params = userClusterRoleIssueService.setUserClusterRoleCommonParams(accountSeq, null, clusterSeq, null, historyState, searchColumn, searchKeyword, startDate, endDate, false);
        params.setPaging(PagingUtils.setPagingParams("CONNECT_SEQ", "DESC", null, null, null, null));

        userClusterRoleIssueService.downloadExcelUserShellConnectHistories(response, params);

        log.debug("[END  ] downloadExcelUserShellConnectHistories");

    }

    @InHouse
    @GetMapping(value = "/{accountSeq}/cluster/config/download/history/excel")
    @Operation(summary = "사용자 클러스터 kubeconfig 다운로드 이력 엑셀 다운로드", description = "사용자 클러스터 kubeconfig 다운로드 이력 엑셀 다운로드")
    public void downloadExcelUserConfigDownloadHistories(
            @Parameter(name = "accountSeq", description = "account seq", required = true) @PathVariable Integer accountSeq,
            @Parameter(name = "clusterSeq", description = "clusterSeq") @RequestParam(name = "clusterSeq", required = false) Integer clusterSeq,
            @Parameter(name = "historyState", description = "발급 상태", schema = @Schema(allowableValues = {"GRANT","REVOKE","CHANGE","EXPIRE"})) @RequestParam(name = "historyState", required = false) String historyState,
            @Parameter(name = "searchColumn", description = "검색 컬럼", schema = @Schema(allowableValues = {"ACCOUNT_NAME","USER_ID","USER_NAME","ISSUE_USER_ID","ISSUE_USER_NAME"})) @RequestParam(name = "searchColumn", required = false) String searchColumn,
            @Parameter(name = "searchKeyword", description = "검색어") @RequestParam(name = "searchKeyword", required = false) String searchKeyword,
            @Parameter(name = "startDate", description = "검색 시작 일자", example = "20200421000000") @RequestParam(name = "startDate", required = false) String startDate,
            @Parameter(name = "endDate", description = "검색 종료 일자", example = "20200428000000") @RequestParam(name = "endDate", required = false) String endDate,
            HttpServletResponse response
    ) throws Exception {

        log.debug("[BEGIN] downloadExcelUserConfigDownloadHistories");

        /**
         * header 정보로 요청 사용자 권한 체크
         */
        AuthUtils.isValid(ContextHolder.exeContext(), AuthUtils.checkUserSysUserNDevOpsBlockAuthPredicate());

        UserClusterRoleIssueSearchVO params = userClusterRoleIssueService.setUserClusterRoleCommonParams(accountSeq, null, clusterSeq, null, historyState, searchColumn, searchKeyword, startDate, endDate, false);
        params.setPaging(PagingUtils.setPagingParams("DOWNLOAD_SEQ", "DESC", null, null, null, null));

        userClusterRoleIssueService.downloadExcelUserConfigDownloadHistories(response, params);

        log.debug("[END  ] downloadExcelUserConfigDownloadHistories");

    }

    @InHouse
    @PutMapping(value = "/{accountSeq}/target/user/{targetUserSeq}/cluster/role/issue/move")
    @Operation(summary = "사용자 클러스터 권한 이관", description = "사용자 클러스터 권한 이관")
    public void moveUserClusterRoleIssues (
            @Parameter(name = "accountSeq", description = "account seq", required = true) @PathVariable Integer accountSeq,
            @Parameter(name = "targetUserSeq", description = "targetUserSeq", required = true) @PathVariable Integer targetUserSeq,
            @Parameter(name = "sourceUsers", description = "추가하려는 사용자 클러스터 권한 정보") @RequestBody List<UserVO> sourceUsers
    ) throws Exception {
        log.debug("[BEGIN] moveUserClusterRoleIssues");

        /**
         * header 정보로 요청 사용자 권한 체크
         */
        AuthUtils.isValid(ContextHolder.exeContext(), AuthUtils.checkUserSysUserNDevOpsBlockAuthPredicate());

        userClusterRoleIssueService.moveUserClusterRoleIssues(targetUserSeq, sourceUsers, ContextHolder.exeContext().getUserSeq());

        log.debug("[END  ] moveUserClusterRoleIssues");
    }


    @PostMapping(value = "/{accountSeq}/user")
    @Operation(summary = "플랫폼 사용자 생성", description = "플랫폼 사용자를 생성한다.")
    public UserVO addUserOfAccount(
            @RequestHeader(name = "isEncrypted", defaultValue="false") boolean isEncrypted,
            @Parameter(name = "accountSeq", description = "account seq", required = true) @PathVariable Integer accountSeq,
            @Parameter(name = "user", description = "추가하려는 user 정보") @RequestBody UserVO user
    ) throws Exception {

        log.debug("[BEGIN] addUserOfAccount");

        /**
         * header 정보로 요청 사용자 권한 체크
         */
        AuthUtils.isValid(ContextHolder.exeContext(), AuthUtils.checkUserSysUserNDevOpsBlockAuthPredicate());

        // isEncrypted가 true(암호화 되어 있다면) 복호화 하여 패스워드 다시 셋팅
        if(isEncrypted && StringUtils.isNotBlank(user.getNewPassword())){
            // 패스워드 복호화 후 다시 셋팅
            String decryptedPasswd = CryptoUtils.decryptRSA(user.getNewPassword());
            user.setNewPassword(decryptedPasswd);
        }

        // 사용자 기본 정보 체크
        userService.checkAddUser(user);

        AccountVO account = accountService.getAccount(accountSeq, true);

        user.setAccount(account);
        List<UserVO> results = userService.getUsersForCheck(user);
        if (results.size() > 0) {
            throw new CocktailException("User already exist", ExceptionType.UserAlreadyExists);
        }else{
            if(account != null){
                user.setUserLanguage(account.getBaseLanguage());
                user.setUserTimezone(user.getUserLanguage().getTimezone());
                // 사용자 등록
                accountService.addUser(accountSeq, user);

            }else{
                throw new CocktailException("Account is not Found.", ExceptionType.AccountNotFound);
            }

            user.setPassword("");
        }

        log.debug("[END  ] addUserOfAccount");

        return user;
    }

    @GetMapping(value = "/{accountSeq}/users")
    @Operation(summary = "플랫폼 사용자 목록", description = "플랫폼 사용자 목록 조회한다.")
    public List<UserVO> getUsersOfAccount(
            @Parameter(name = "accountSeq", description = "account seq", required = true) @PathVariable Integer accountSeq
    ) throws Exception {

        log.debug("[BEGIN] getUsersOfAccount");

        /**
         * header 정보로 요청 사용자 권한 체크
         */
        AuthUtils.isValid(ContextHolder.exeContext(), AuthUtils.checkUserDevOpsBlockAuthPredicate());

        List<UserVO> users = userService.getUsersOfAccountWithPwdPolicy(accountSeq);

        log.debug("[END  ] getUsersOfAccount");

        return users;
    }


    @GetMapping(value = "/{accountSeq}/user/{userSeq}")
    @Operation(summary = "플랫폼 사용자 상세", description = "플랫폼 사용자 상세 조회한다.")
    public UserVO getUserOfAccount(
            @Parameter(name = "accountSeq", description = "account seq", required = true) @PathVariable Integer accountSeq,
            @Parameter(name = "userSeq", description = "user seq", required = true) @PathVariable Integer userSeq,
            @Parameter(name = "includeNamespace", description = "클러스터내의 전체 Namespace목록을 포함한다", schema = @Schema(allowableValues = {"true","false"}, defaultValue = "false")) @RequestParam(value = "includeNamespace", defaultValue = "false", required = false) Boolean includeNamespace
    ) throws Exception {

        log.debug("[BEGIN] getUserOfAccount");

        /**
         * header 정보로 요청 사용자 권한 체크
         */
        AuthUtils.isValid(ContextHolder.exeContext(), AuthUtils.checkUserDevOpsBlockAuthPredicate());

        UserVO user = accountService.getUser(accountSeq, userSeq, includeNamespace);

        log.debug("[END  ] getUserOfAccount");

        return user;
    }

    /**
     * KeyCloak에서 사용하기 위해 해시된 패스워드도 응답하는 API임.
     * 유의하여 사용할것.
     *
     * @param accountSeq
     * @param encUserId
     * @param includeNamespace
     * @return
     * @throws Exception
     */
    @InHouse
    @GetMapping(value = "/{accountSeq}/user/id/{encUserId}")
    @Operation(summary = "플랫폼 사용자 상세(For keycloak)", description = "플랫폼 사용자 상세 조회한다.(For keycloak)")
    public UserVO getUserOfAccountById(
        @Parameter(name = "accountSeq", description = "account seq", required = true) @PathVariable Integer accountSeq,
        @Parameter(name = "encUserId", description = "encUserId", required = true) @PathVariable String encUserId,
        @Parameter(name = "includeNamespace", description = "클러스터내의 전체 Namespace목록을 포함한다", schema = @Schema(allowableValues = {"true","false"}, defaultValue = "false")) @RequestParam(value = "includeNamespace", defaultValue = "false", required = false) Boolean includeNamespace
    ) throws Exception {
        log.debug("[BEGIN] getUserOfAccountById");

        String userId = new String(Base64.getDecoder().decode(encUserId), StandardCharsets.UTF_8);

        /**
         * header 정보로 요청 사용자 권한 체크
         */
        AuthUtils.isValid(ContextHolder.exeContext(), AuthUtils.checkUserDevOpsBlockAuthPredicate());

        UserVO user = accountService.getUserById(accountSeq, userId, includeNamespace);

        log.debug("[END  ] getUserOfAccountById");

        return user;
    }

    @PutMapping(value = "/{accountSeq}/user/{userSeq}")
    @Operation(summary = "플랫폼 사용자 수정", description = "플랫폼 사용자 수정한다.")
    public UserVO editUserOfAccount(
            @Parameter(name = "accountSeq", description = "account seq") @PathVariable Integer accountSeq,
            @Parameter(name = "userSeq", description = "user seq") @PathVariable Integer userSeq,
            @Parameter(name = "user", description = "추가하려는 user 정보") @RequestBody UserVO user
    ) throws Exception {

        log.debug("[BEGIN] editUserOfAccount");

        /**
         * header 정보로 요청 사용자 권한 체크
         */
        AuthUtils.isValid(ContextHolder.exeContext(), AuthUtils.checkUserDevOpsBlockAuthPredicate());

        /**
         * 사용자 수정
         */
        user.setUserSeq(userSeq);
        UserVO userCurr = accountService.editUser(accountSeq, user);

        log.debug("[END  ] editUserOfAccount");

        return userCurr;
    }

    @PutMapping(value = "/{accountSeq}/user/{userSeq}/grant")
    @Operation(summary = "플랫폼 사용자 권한 변경", description = "플랫폼 사용자 권한을 변경한다.")
    public UserVO editUserGrantOfAccount(
        @Parameter(name = "accountSeq", description = "account seq") @PathVariable Integer accountSeq,
        @Parameter(name = "userSeq", description = "user seq") @PathVariable Integer userSeq,
        @Parameter(name = "user", description = "수정하려는 user 정보") @RequestBody UserVO user
    ) throws Exception {

        log.debug("[BEGIN] editUserGrantOfAccount");

        /**
         * header 정보로 요청 사용자 권한 체크
         */
        AuthUtils.isValid(ContextHolder.exeContext(), AuthUtils.checkUserDevOpsBlockAuthPredicate());

        /**
         * 사용자 수정
         */
        user.setUserSeq(userSeq);
        UserVO userCurr = accountService.editUserGrant(accountSeq, user);

        log.debug("[END  ] editUserOfAccount");

        return userCurr;
    }

    @DeleteMapping(value = "/{accountSeq}/user/{userSeq}")
    @Operation(summary = "플랫폼 사용자 삭제", description = "플랫폼 사용자 삭제한다.")
    public UserVO removeUserOfAccount(
            @Parameter(name = "accountSeq", description = "account seq") @PathVariable Integer accountSeq,
            @PathVariable int userSeq
    ) throws Exception {

        log.debug("[BEGIN] removeUserOfAccount");

        /**
         * header 정보로 요청 사용자 권한 체크
         */
        AuthUtils.isValid(ContextHolder.exeContext(), AuthUtils.checkUserDevOpsBlockAuthPredicate());

        /**
         * 사용자 삭제
         */
        UserVO user = accountService.removeUser(accountSeq, userSeq, false);

        log.debug("[END  ] removeUserOfAccount");

        user.setShellRoles(null);
        user.setKubeconfigRoles(null);
        user.setClusters(null);
        user.setUserRelations(null);
        return user;
    }

    @PostMapping(value = "/{accountSeq}/user/{userSeq}/resetPassword")
    @Operation(summary = "플랫폼 사용자 비밀번호 초기화", description = "플랫폼 사용자 비밀번호 초기화한다.")
    public void resetPasswordUserOfAccount(
            @Parameter(name = "accountSeq", description = "account seq", required = true) @PathVariable Integer accountSeq,
            @Parameter(name = "userSeq", description = "user seq", required = true) @PathVariable int userSeq,
            @Parameter(name = "useEmail", description = "이메일로 초기화 비밀번호 전달", schema = @Schema(allowableValues = {"true","false"}, defaultValue = "false")) @RequestParam(value = "useEmail", required = false, defaultValue = "false") boolean useEmail
    ) throws Exception {

        log.debug("[BEGIN] resetPasswordUserOfAccount");

        /**
         * header 정보로 요청 사용자 권한 체크
         */
        AuthUtils.isValid(ContextHolder.exeContext(), AuthUtils.checkUserSysUserNDevOpsBlockAuthPredicate());

        userService.resetPasswordUserWithCheck(userSeq, useEmail);

        log.debug("[END  ] resetPasswordUserOfAccount");
    }

//    @PutMapping(value = "/{accountSeq}/user/{userSeq}/otp")
//    public void editUserOtpInfoOfAccount(
//            @Parameter(name = "accountSeq", description = "account seq", required = true) @PathVariable Integer accountSeq,
//            @Parameter(name = "userSeq", description = "user seq", required = true) @PathVariable Integer userSeq,
//            @Parameter(name = "userOtp", description = "수정하려는 user otp 정보") @RequestBody UserOtpVO userOtp
//
//    ) throws Exception {
//
//        log.debug("[BEGIN] editUserOtpInfoOfAccount");
//
//        // 요청자의 플랫폼과 요청 파라미터의 플랫폼이 같은 지 체크
//        if (!accountSeq.equals(ContextHolder.exeContext().getUserAccountSeq())) {
//            throw new CocktailException("Check Authorization Error : Invalid accountSeq.", ExceptionType.NotAuthorizedToResource);
//        }
//
//        if (!userSeq.equals(ContextHolder.exeContext().getUserSeq())) {
//            throw new CocktailException("You can only edit yourself", ExceptionType.NotAuthorizedToRequest);
//        } else {
//            userOtp.setUserSeq(userSeq);
//        }
//
//        userService.updateUserOtpInfo(userOtp, ContextHolder.exeContext().getUserSeq());
//
//        log.debug("[END  ] editUserOtpInfoOfAccount");
//    }

    @GetMapping(value = "/{accountSeq}/user/{userSeq}/otp/qr")
    @Operation(summary = "플랫폼 사용자 otp qr 정보 조회", description = "플랫폼 사용자의 OTP QR 정보를 조회한다.")
    public UserOtpVO getUserOtpQrOfAccount(
            @Parameter(name = "accountSeq", description = "account seq", required = true) @PathVariable Integer accountSeq,
            @Parameter(name = "userSeq", description = "user seq", required = true) @PathVariable Integer userSeq

    ) throws Exception {

        log.debug("[BEGIN] getUserOtpQrOfAccount");

        // 요청자의 플랫폼과 요청 파라미터의 플랫폼이 같은 지 체크
        if (!accountSeq.equals(ContextHolder.exeContext().getUserAccountSeq())) {
            throw new CocktailException("Check Authorization Error : Invalid accountSeq.", ExceptionType.NotAuthorizedToResource);
        }

        UserOtpVO userOtp = userService.getUserOtpQr(userSeq);

        log.debug("[END  ] getUserOtpQrOfAccount");

        return userOtp;
    }

    @PostMapping(value = "/{accountSeq}/user/{userSeq}/otp/reset")
    @Operation(summary = "플랫폼 사용자 OTP 초기화", description = "플랫폼 사용자 OTP 초기화한다.")
    public void resetOtpUserOfAccount(
            @Parameter(name = "accountSeq", description = "account seq", required = true) @PathVariable Integer accountSeq,
            @Parameter(name = "userSeq", description = "user seq", required = true) @PathVariable int userSeq
    ) throws Exception {

        log.debug("[BEGIN] resetOtpUserOfAccount");

        /**
         * header 정보로 요청 사용자 권한 체크
         */
        AuthUtils.isValid(ContextHolder.exeContext(), AuthUtils.checkUserSysUserNDevOpsBlockAuthPredicate());

        // 요청자의 플랫폼과 요청 파라미터의 플랫폼이 같은 지 체크
        if (!accountSeq.equals(ContextHolder.exeContext().getUserAccountSeq())) {
            throw new CocktailException("Check Authorization Error : Invalid accountSeq.", ExceptionType.NotAuthorizedToResource);
        }

        UserOtpVO userOtp = new UserOtpVO();
        userOtp.setUserSeq(userSeq);
        userOtp.setOtpUseYn("N");
        userService.updateUserOtpInfo(userOtp, ContextHolder.exeContext().getUserSeq());

        log.debug("[END  ] resetOtpUserOfAccount");
    }

    @PutMapping(value = "/{accountSeq}/user/{userSeq}/inactive")
    @Operation(summary = "플랫폼 사용자 비활성 여부 변경", description = "플랫폼 사용자 비활성 여부를 변경한다.")
    public void editUserInactiveStateOfAccount(
            @Parameter(name = "accountSeq", description = "account seq", required = true) @PathVariable Integer accountSeq,
            @Parameter(name = "userSeq", description = "user seq", required = true) @PathVariable Integer userSeq,
            @Parameter(name = "userInactive", description = "수정하려는 user 비활성 여부 정보", required = true) @RequestBody UserInactiveVO userInactive
    ) throws Exception {

        log.debug("[BEGIN] editUserInactiveStateOfAccount");

        /**
         * header 정보로 요청 사용자 권한 체크
         */
        AuthUtils.isValid(ContextHolder.exeContext(), AuthUtils.checkUserSysUserNDevOpsBlockAuthPredicate());

        if (userSeq.equals(ContextHolder.exeContext().getUserSeq())) {
            throw new CocktailException("Your status cannot be modified.", ExceptionType.CanNotUpdateInactiveStateMyself);
        }

        // 요청자의 플랫폼과 요청 파라미터의 플랫폼이 같은 지 체크
        if (!accountSeq.equals(ContextHolder.exeContext().getUserAccountSeq())) {
            throw new CocktailException("Check Authorization Error : Invalid accountSeq.", ExceptionType.NotAuthorizedToResource);
        }

        UserVO user = userService.getByUserSeq(userSeq);
        if (user != null) {
            // 요청 파라미터의 플랫폼과 요청한 사용자의 플랫펌이 같은 지 체크
            if (!accountSeq.equals(user.getAccount().getAccountSeq())) {
                throw new CocktailException("Check Authorization Error : Invalid accountSeq.", ExceptionType.NotAuthorizedToResource);
            }

            // 비활성여부 변경
            userService.updateUserInactiveYn(userSeq, userInactive.getInactiveYn(), ContextHolder.exeContext().getUserSeq());
        } else {
            throw new CocktailException("User not found", ExceptionType.UserIdNotFound);
        }

        log.debug("[END  ] editUserInactiveStateOfAccount");
    }

    @PutMapping(value = "/{accountSeq}/user/{userSeq}/wake")
    @Operation(summary = "플랫폼 휴면 사용자 활성", description = "플랫폼 휴면 사용자 활성한다.")
    public void editUserWakeStateOfAccount(
            @Parameter(name = "accountSeq", description = "account seq", required = true) @PathVariable Integer accountSeq,
            @Parameter(name = "userSeq", description = "user seq", required = true) @PathVariable Integer userSeq
    ) throws Exception {

        log.debug("[BEGIN] editUserWakeStateOfAccount");

        /**
         * header 정보로 요청 사용자 권한 체크
         */
        AuthUtils.isValid(ContextHolder.exeContext(), AuthUtils.checkUserSysUserNDevOpsBlockAuthPredicate());

        if (userSeq.equals(ContextHolder.exeContext().getUserSeq())) {
            throw new CocktailException("Your status cannot be modified.", ExceptionType.CanNotUpdateInactiveStateMyself);
        }

        // 요청자의 플랫폼과 요청 파라미터의 플랫폼이 같은 지 체크
        if (!accountSeq.equals(ContextHolder.exeContext().getUserAccountSeq())) {
            throw new CocktailException("Check Authorization Error : Invalid accountSeq.", ExceptionType.NotAuthorizedToResource);
        }

        UserVO user = userService.getByUserSeq(userSeq);
        if (user != null) {
            // 요청 파라미터의 플랫폼과 요청한 사용자의 플랫펌이 같은 지 체크
            if (!accountSeq.equals(user.getAccount().getAccountSeq())) {
                throw new CocktailException("Check Authorization Error : Invalid accountSeq.", ExceptionType.NotAuthorizedToResource);
            }

            // 휴면 활성
            userService.updateActiveTimestampBySeq(userSeq, ContextHolder.exeContext().getUserSeq());
        } else {
            throw new CocktailException("User not found", ExceptionType.UserIdNotFound);
        }

        log.debug("[END  ] editUserWakeStateOfAccount");
    }

    @InHouse
    @PostMapping(value = "/{accountSeq}/provideraccount")
    @Operation(summary = "클러스터 계정 등록", description = "클러스터 계정을 등록한다.")
    public ProviderAccountVO addProviderAccountOfAccount(
            @Parameter(name = "accountSeq", description = "account seq") @PathVariable Integer accountSeq,
            @Parameter(name = "providerAccount", description = "추가하려는 providerAccount 정보") @RequestBody ProviderAccountVO providerAccount
    ) throws Exception {

        log.debug("[BEGIN] addProviderAccountOfAccount");

        providerAccount.setAccountSeq(accountSeq);
        providerAccount.setUseYn("Y");

        /**
         * add provider_account
         */
        providerAccountService.addProviderAccount(providerAccount);

        /**
         * add account & provider_account mapping
         */
        accountService.addProviderOfAccount(providerAccount.getAccountSeq(), Collections.singletonList(providerAccount.getProviderAccountSeq()), providerAccount.getCreator());

        providerAccount = providerAccountService.getProviderAccount(providerAccount.getProviderAccountSeq());
        if (providerAccount != null) {
            providerAccount.setApiAccountId("");
            providerAccount.setApiAccountPassword("");
            providerAccount.setCredential(null);
        }

        log.debug("[END  ] addProviderAccountOfAccount");

        return providerAccount;
    }

    @InHouse
    @PutMapping(value = "/{accountSeq}/provideraccount/{providerAccountSeq}")
    @Operation(summary = "클러스터 계정 수정", description = "클러스터 계정을 수정한다.")
    public ProviderAccountVO editProviderAccountOfAccount(
            @Parameter(name = "accountSeq", description = "account seq") @PathVariable int accountSeq,
            @Parameter(name = "providerAccountSeq", description = "provider account seq") @PathVariable int providerAccountSeq,
            @Parameter(name = "providerAccount", description = "providerAccount 정보") @RequestBody ProviderAccountVO providerAccount
    ) throws Exception {

        log.debug("[BEGIN] editProviderAccountOfAccount");

        providerAccount.setAccountSeq(accountSeq);

        providerAccountService.checkProviderAccountValidation(providerAccount);

        providerAccountService.editProviderAccount(providerAccount);

        ProviderAccountVO pvCurr = providerAccountService.getProviderAccount(providerAccountSeq);
        pvCurr.setApiAccountId("");
        pvCurr.setApiAccountPassword("");
        pvCurr.setCredential("");

        log.debug("[END  ] editProviderAccountOfAccount");

        return pvCurr;
    }

    @InHouse
    @DeleteMapping(value = "/{accountSeq}/provideraccount/{providerAccountSeq}")
    @Operation(summary = "클러스터 계정 삭제", description = "클러스터 계정을 삭제한다.")
    public void removeProviderAccountOfAccount(
            @RequestHeader(name = "user-id" ) Integer userSeq,
            @Parameter(name = "accountSeq", description = "account seq") @PathVariable int accountSeq,
            @Parameter(name = "providerAccountSeq", description = "provider account seq") @PathVariable int providerAccountSeq
    ) throws Exception {

        log.debug("[BEGIN] removeProviderAccountOfAccount");

        ProviderAccountVO pvCurr = providerAccountService.getProviderAccount(providerAccountSeq);
        if (pvCurr == null) {
            throw new CocktailException("Provider not found", ExceptionType.ProviderNotFound);
        }else{
            pvCurr.setAccountSeq(accountSeq);
        }
        // METERING 유형만 체크
        List<String> clusters = providerAccountService.getClusterUsingProviderAccount(providerAccountSeq, pvCurr.getAccountUseType());
        if (CollectionUtils.isNotEmpty(clusters)) {
            throw new CocktailException(String.format("Provider account used by cluster(s): %s", clusters),
                    ExceptionType.ProviderUsedByCluster);
        }

        pvCurr.setProviderAccountSeq(providerAccountSeq);
        pvCurr.setUpdater(userSeq);
        providerAccountService.removeProviderAccount(pvCurr);

        log.debug("[END  ] removeProviderAccountOfAccount");

    }

    @InHouse
    @GetMapping(value = "/{accountSeq}/volumes")
    @Operation(summary = "플랫폼 > 스토리지 목록", description = "플랫폼 > 스토리지 목록 조회한다.")
    public List<ClusterVolumeVO> getClusterVolumesOfAccount(
            @Parameter(name = "accountSeq", description = "계정 번호", required = true) @PathVariable(name = "accountSeq") Integer accountSeq,
            @Parameter(name = "storageType", description = "storageType", schema = @Schema(allowableValues = {"NETWORK","BLOCK"})) @RequestParam(name = "storageType", required = false) String storageType,
            @Parameter(name = "type", description = "type", schema = @Schema(allowableValues = {"PERSISTENT_VOLUME","PERSISTENT_VOLUME_STATIC"})) @RequestParam(name = "type", required = false) String type,
            @Parameter(name = "useCapacity", description = "K8S total capacity 정보 사용 여부", schema = @Schema(allowableValues = {"true","false"}, defaultValue = "false")) @RequestParam(value = "useCapacity", required = false, defaultValue = "false") boolean useCapacity,
            @Parameter(name = "useRequest", description = "K8S Claim Request 정보 사용 여부", schema = @Schema(allowableValues = {"true","false"}, defaultValue = "false")) @RequestParam(value = "useRequest", required = false, defaultValue = "false") boolean useRequest
    ) throws Exception {
        log.debug("[BEGIN] getClusterVolumes");

        /**
         * SYSTEM 권한자만 조회 가능
         */
        AuthUtils.isValid(AuthUtils.isSystemNSysadminUser(ContextHolder.exeContext()));

        List<ClusterVolumeVO> clusterVolumes = clusterVolumeService.getStorageVolumes(accountSeq, null, null, StringUtils.defaultIfBlank(storageType, null), StringUtils.defaultIfBlank(type, null), useCapacity, useRequest);

        log.debug("[END  ] getClusterVolumes");

        return clusterVolumes;
    }

    @GetMapping("/{accountSeq}/persistentvolumeClaims")
    @Operation(summary = "플랫폼 > PersistentVolumeClaim 목록", description = "플랫 > PersistentVolumeClaim 목록을 조회한다.")
    public List<K8sPersistentVolumeClaimVO> getPersistentVolumeClaimasInAccount(
            @Parameter(name = "accountSeq", description = "accountSeq", required = true) @PathVariable Integer accountSeq,
            @Parameter(name = "serviceSeq", description = "서비스 번호") @RequestParam(name = "serviceSeq", required = false) Integer serviceSeq,
            @Parameter(name = "acloudOnly", description = "Acloud에서 관리되는 리소스만 조회 여부", schema = @Schema(allowableValues = {"true","false"}, defaultValue = "false")) @RequestParam(value = "acloudOnly", defaultValue = "false", required = false) Boolean acloudOnly
    ) throws Exception {
        log.debug("[BEGIN] getPersistentVolumeClaimasInAccount");

        ExecutingContextVO ctx = new ExecutingContextVO();
        ctx.setApiVersionType(ApiVersionType.V2);

        List<K8sPersistentVolumeClaimVO> persistentVolumeClaims = persistentVolumeService.getPersistentVolumeClaimsInAccount(accountSeq, serviceSeq, null, null, acloudOnly, ctx);

        log.debug("[END  ] getPersistentVolumeClaimasInAccount");

        return persistentVolumeClaims;
    }

    @InHouse
    @GetMapping(value = "/{accountSeq}/check/possibleResisterWorkspace")
    @Operation(summary = "플랫폼에 workspace 등록 가능한지 체크", description = "check possible to register workspace.")
    public Map<String, Boolean> isPossibleRegisterWorkspace(
            @Parameter(name = "accountSeq", description = "계정 번호", required = true) @PathVariable(name = "accountSeq") Integer accountSeq
    ) throws Exception {

        log.debug("[BEGIN] isPossibleRegisterWorkspace");

        boolean isPossibleRegisterWorkspace = accountGradeService.isPossibleRegisterWorkspace(accountSeq);

        Map<String, Boolean> resultMap = new HashMap<>();
        resultMap.put("isPossibleRegisterWorkspace", isPossibleRegisterWorkspace);

        log.debug("[END  ] isPossibleRegisterWorkspace");

        return resultMap;
    }

    @GetMapping(value = "/{accountSeq}/registries")
    @Operation(summary = "플랫폼 > registry 목록", description = "플랫폼 > registry 목록을 조회한다.")
    public List<ServiceRegistryVO> getServiceRegistriesOfAccount(
            @Parameter(name = "accountSeq", description = "계정 번호", required = true) @PathVariable(name = "accountSeq") Integer accountSeq,
            @Parameter(name = "serviceSeq", description = "서비스 번호") @RequestParam(name = "serviceSeq", required = false) Integer serviceSeq
    ) throws Exception {
        log.debug("[BEGIN] getServiceRegistriesOfAccount");

        List<ServiceRegistryVO> serviceRegistries;
        if(serviceSeq != null) {
            serviceRegistries = serviceService.getServiceRegistryOfAccount(accountSeq, serviceSeq, null, null, null);
        }
        else {
            serviceRegistries = serviceService.getServiceRegistryOfAccount(accountSeq, ServiceRegistryType.SERVICE.getCode(), null, ServiceType.PLATFORM.getCode());
        }

        log.debug("[END  ] getServiceRegistriesOfAccount");

        return serviceRegistries;
    }

    @GetMapping(value = "/{accountSeq}/registry/{projectId}")
    @Operation(summary = "플랫폼 > registry 상세", description = "플랫폼 > registry 상세 조회한다.")
    public ServiceRegistryVO getServiceRegistryOfAccount(
        @Parameter(name = "accountSeq", description = "계정 번호", required = true) @PathVariable(name = "accountSeq") Integer accountSeq,
        @Parameter(name = "projectId", description = "프로젝트 ID (번호)", required = true) @PathVariable(name = "projectId") Integer projectId
    ) throws Exception {
        log.debug("[BEGIN] getServiceRegistryOfAccount");

        /** Registry 상세 정보 조회 **/
        List<ServiceRegistryVO> serviceRegistries = serviceService.getServiceRegistryOfAccount(accountSeq, ServiceRegistryType.SERVICE.getCode(), projectId, ServiceType.PLATFORM.getCode());

        if(CollectionUtils.isEmpty(serviceRegistries)) {
            throw new CocktailException("Could not found registry.", ExceptionType.RegistryProjectNotFound, projectId.toString());
        }

        log.debug("[END  ] getServiceRegistryOfAccount");

        ServiceRegistryVO serviceRegistry = serviceRegistries.get(0);

        /** Registry를 Share하여 사용중인 서비스(워크스페이스) 목록 조회 하여 셋팅 **/
        List<ServiceCountVO> services = serviceService.getServicesInRegistry(projectId, "Y");
        serviceRegistry.setServices(services);

        return serviceRegistry;
    }

    @PostMapping(value = "/{accountSeq}/registry")
    @Operation(summary = "플랫폼 >  Registry 추가", description = "플랫폼 >  Registry를 추가한다.")
    public ServiceRegistryVO addServiceRegistryOfAccount(
        @Parameter(name = "accountSeq", description = "계정 번호", required = true) @PathVariable(name = "accountSeq") Integer accountSeq,
        @Parameter(description = "서비스레지스트리 모델", required = true) @RequestBody ServiceRegistryVO serviceRegistry
    ) throws Exception {
        log.debug("[BEGIN] addServiceRegistryOfAccount");

        // Get Platform 워크스페이스
        ServiceVO platformService = serviceService.getPlatformService(accountSeq, null);
        serviceRegistry.setServiceSeq(platformService.getServiceSeq());


        serviceRegistry = accountService.serviceRegistryManager(accountSeq, serviceRegistry, CRUDCommand.C);

        log.debug("[END  ] addServiceRegistryOfAccount");

        return serviceRegistry;
    }

    @PutMapping(value = "/{accountSeq}/registry/{projectId}")
    @Operation(summary = "플랫폼 > Registry 수정", description = "플랫폼 > Registry를 수정한다.")
    public ServiceRegistryVO updateServiceRegistryOfAccount(
        @Parameter(name = "accountSeq", description = "계정 번호", required = true) @PathVariable(name = "accountSeq") Integer accountSeq,
        @Parameter(name = "projectId", description = "프로젝트 ID (번호)", required = true) @PathVariable(name = "projectId") Integer projectId,
        @Parameter(description = "서비스레지스트리 모델", required = true) @RequestBody ServiceRegistryVO serviceRegistry
    ) throws Exception {
        log.debug("[BEGIN] updateServiceRegistryOfAccount");

        if(projectId == null || serviceRegistry.getProjectId() == null) {
            throw new CocktailException("Project Id is null", ExceptionType.InvalidParameter_Empty, "ProjectId is null");
        }
        if(!projectId.equals(serviceRegistry.getProjectId())) {
            throw new CocktailException("Project Id is different", ExceptionType.InvalidParameter_Empty, "ProjectId is different");
        }
        if(serviceRegistry.getServiceSeq() == null) {
            // Get Platform 서비스
            ServiceVO platformService = serviceService.getPlatformService(accountSeq, null);
            serviceRegistry.setServiceSeq(platformService.getServiceSeq());
        }

        serviceRegistry = accountService.serviceRegistryManager(accountSeq, serviceRegistry, CRUDCommand.U);

        log.debug("[END  ] updateServiceRegistryOfAccount");

        return serviceRegistry;
    }

    @DeleteMapping(value = "/{accountSeq}/registry/{projectId}")
    @Operation(summary = "플랫폼 > Registry 삭제", description = "플랫폼 > Registry를 삭제한다.")
    public ServiceRegistryVO removeServiceRegistryOfAccount(
        @Parameter(name = "accountSeq", description = "계정 번호", required = true) @PathVariable(name = "accountSeq") Integer accountSeq,
        @Parameter(name = "projectId", description = "프로젝트 ID (번호)", required = true) @PathVariable(name = "projectId") Integer projectId
    ) throws Exception {
        log.debug("[BEGIN] removeServiceRegistryOfAccount");

        // Get Platform 서비스
        ServiceVO platformService = serviceService.getPlatformService(accountSeq, null);
        ServiceRegistryVO serviceRegistry = serviceService.getServiceRegistry(platformService.getServiceSeq(), ServiceRegistryType.SERVICE.getCode(), projectId, null);

        if(serviceRegistry == null) {
            throw new CocktailException("Could not found registry.", ExceptionType.RegistryProjectNotFound, projectId.toString());
        }

        serviceRegistry = accountService.serviceRegistryManager(accountSeq, serviceRegistry, CRUDCommand.D);

        log.debug("[END  ] removeServiceRegistryOfAccount");

        return serviceRegistry;
    }

    @InHouse
    @Operation(summary = "ServiceRegistry 이름 사용 여부 확인", description = "추가하려는 ServiceRegistry 이름이  이미 사용하고 있는 것인지 검사한다.")
    @GetMapping("/{accountSeq}/registry/{projectName}/check")
    public ResultVO isServiceRegistryUsed(
            @Parameter(name = "accountSeq", description = "계정 번호", required = true) @PathVariable Integer accountSeq,
            @Parameter(name = "projectName", description = "Registry 이름", required = true) @PathVariable String projectName,
            @Parameter(name = "withHarbor", description = "Harbor 포함 여부, false면 DB만 체크", schema = @Schema(allowableValues = {"true","false"}, defaultValue = "false")) @RequestParam(value = "withHarbor", required = false, defaultValue = "false") boolean withHarbor
    ) throws Exception {
        ResultVO r = new ResultVO();
        try {
            r.putKeyValue("exists", serviceValidService.checkServiceRegistryUsed(accountSeq, projectName, withHarbor));
        }
        catch (DataAccessException de) {
            throw de; // DB 처리중 Runtime Exception이 발생하면 처리를 공통 ExceptionHandler 로 던지고 공통 ExceptionHandler에서 처리
        }
        catch (CocktailException ce) {
            throw ce; // CocktailException이 발생하면 발생한 예외 처리 케이스를 그대로 사용
        }
        catch (Exception e) {
            throw new CocktailException("Error during data query.", e, ExceptionType.CommonInquireFail, ExceptionBiz.SERVER);
        }
        return r;
    }

    @InHouse
    @GetMapping(value = "/{accountSeq}/tenancy/{clusterTenancy}/assignable/clusters")
    @Operation(summary = "테넌시별로 할당 가능한 클러스터 목록", description = "테넌시별로 할당 가능한  클러스터 목록을 가져온다.")
    public List<ClusterVO> getAssignableClustersOfAccountForTenancy(
            @Parameter(name = "accountSeq", description = "계정 번호", required = true) @PathVariable Integer accountSeq,
            @Parameter(name = "clusterTenancy", description = "클러스터 테넌시 유형", schema = @Schema(allowableValues = {"HARD","SOFT"}, defaultValue = "HARD"), required = true) @PathVariable String clusterTenancy,
            @Parameter(name = "serviceSeq", description = "워크스페이스 Seq") @RequestParam(name = "serviceSeq", required = false) Integer serviceSeq
    ) throws Exception {
        log.debug("[BEGIN] getAssignableClustersOfAccountForTenancy");

        List<ClusterVO> clusters = accountService.getAssignableClustersOfAccountForTenancy(accountSeq, serviceSeq, clusterTenancy);
        try {
            clusters = clusters.stream().map(c -> {
                ClusterUtils.setNullClusterInfo(c);
                return c;
            }).collect(Collectors.toList());

        }
        catch (CocktailException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new CocktailException("getAssignableClustersOfAccountForTenancy Fail.", ex, ExceptionType.CommonInquireFail);
        } finally {
            log.debug("[END  ] getAssignableClustersOfAccountForTenancy");
        }

        return clusters;
    }

    @InHouse
    @GetMapping(value = "/{apiVersion}/{accountSeq}/users/exist/clusterroleissue")
    @Operation(summary = "클러스터 접속 계정이 발급이력이 존재하는 사용자 목록 조회", description = "클러스터 접속 계정이 발급이력이 존재하는 사용자 목록 조회")
    public List<UserVO> getUsersExistClusterRoleIssue (
            @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v1"}), required = true) @PathVariable String apiVersion,
            @Parameter(name = "accountSeq", description = "accountSeq", required = true) @PathVariable Integer accountSeq
    ) throws Exception {
        List<UserVO> users;
        try {
            /**
             * 4.0.1 기준으로 DEVOPS 사용자는 접근 불가...
             */
            if(UserRole.valueOf(ContextHolder.exeContext().getUserRole()).isDevops()) {
                throw new CocktailException("The DEVOPS role user doesn't have Permission.", ExceptionType.NotAuthorizedToRequest);
            }

            users = userService.getUsersExistClusterRoleIssue(accountSeq);
        }
        catch (CocktailException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new CocktailException("getUsersExistClusterRoleIssue Fail.", ex, ExceptionType.CommonInquireFail);
        }

        return users;
    }

    @InHouse
    @GetMapping(value = "/{accountSeq}/user/{userSeq}/cluster/role/issue")
    @Operation(summary = "사용자별 접속 가능한 클러스터 목록 조회", description = "사용자별 접속 가능한 클러스터 목록 조회")
    public UserClusterRoleIssueListVO getUserAccessibleClusters (
        @Parameter(name = "accountSeq", description = "account seq", required = true) @PathVariable Integer accountSeq,
        @Parameter(name = "userSeq", description = "user seq", required = true) @PathVariable Integer userSeq,
        @Parameter(name = "issueType", description = "발급 유형", schema = @Schema(allowableValues = {"SHELL","KUBECONFIG"}), required = true) @RequestParam(name = "issueType") String issueType,
//        @Parameter(name = "order", description = "정렬 순서", schema = @Schema(allowableValues = {"DESC","ASC"})) @RequestParam(name = "order", required = false) String order,
//        @Parameter(name = "orderColumn", description = "정렬기준컬럼", schema = @Schema(allowableValues = {"created","audit_log_seq"})) @RequestParam(name = "orderColumn", required = false) String orderColumn,
        @Parameter(name = "nextPage", description = "요청페이지", required = true) @RequestParam(name = "nextPage") Integer nextPage,
        @Parameter(name = "itemPerPage", description = "페이지당 표시 갯수", required = true) @RequestParam(name = "itemPerPage") Integer itemPerPage,
        @Parameter(name = "searchColumn", description = "검색 컬럼", schema = @Schema(allowableValues = {"ACCOUNT_NAME","USER_ID","USER_NAME","ISSUE_USER_ID","ISSUE_USER_NAME"})) @RequestParam(name = "searchColumn", required = false) String searchColumn,
        @Parameter(name = "searchKeyword", description = "검색어") @RequestParam(name = "searchKeyword", required = false) String searchKeyword,
        @Parameter(name = "startDate", description = "검색 시작 일자", example = "20200421000000") @RequestParam(name = "startDate", required = false) String startDate,
        @Parameter(name = "endDate", description = "검색 종료 일자", example = "20201228000000") @RequestParam(name = "endDate", required = false) String endDate
    ) throws Exception {
        UserClusterRoleIssueListVO userClusterRoleIssueList;
        try {
            /**
             * R4.5.0 : userSeq는 로그인한 사용자의 정보와 같아야 한다.
             */
            if(userSeq != null && !ContextHolder.exeContext().getUserSeq().equals(userSeq)) {
                throw new CocktailException("userSeq is different.", ExceptionType.NotAuthorizedToRequest, "userSeq is different.");
            }
            // userSeq is not null
            if(userSeq == null) {
                throw new CocktailException("userSeq is null", ExceptionType.InvalidParameter_Empty, "userSeq is null");
            }
            // invalid parameters
            PagingUtils.validatePagingParams(nextPage, itemPerPage);

            String orderColumn = "cluster_id, ucrib.namespace";
            String order = "ASC";

            userClusterRoleIssueList = userClusterRoleIssueService.getUserClusterRoleIssueList(accountSeq, userSeq, null, issueType, order, orderColumn, nextPage, itemPerPage, searchColumn, searchKeyword, startDate, endDate);
        }
        catch (CocktailException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new CocktailException("getUserClusterRoleIssueList Fail.", ex, ExceptionType.CommonInquireFail);
        }

        return userClusterRoleIssueList;
    }


}
