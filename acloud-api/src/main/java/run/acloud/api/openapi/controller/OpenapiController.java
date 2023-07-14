package run.acloud.api.openapi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import run.acloud.api.openapi.service.OpenapiService;
import run.acloud.api.openapi.vo.*;
import run.acloud.commons.util.AuthUtils;
import run.acloud.commons.util.PagingUtils;
import run.acloud.commons.vo.ListCountVO;
import run.acloud.framework.context.ContextHolder;
import run.acloud.framework.enums.ExceptionBiz;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;

import java.util.List;

@Tag(name = "Openapi", description = "Openapi 관련 기능을 제공한다.")
@Slf4j
@RequestMapping(value = "/api/openapi")
@RestController
@Validated
public class OpenapiController {

    @Autowired
    private OpenapiService openapiService;
    

    @GetMapping(value = "/.well-known/jwks.json")
    @Operation(summary = "jwks json 조회", description = "jwks json을 조회한다.")
    public String getJsonWebKeySet() throws Exception {
        log.debug("[BEGIN] getJsonWebKeySet");

        /**
         * header 정보로 요청 사용자 권한 체크
         */
        AuthUtils.isValid(ContextHolder.exeContext(), AuthUtils.checkUserSysUserNDevOpsBlockAuthPredicate());

        String jwksJson = openapiService.getJwks();

        log.debug("[END  ] getJsonWebKeySet");

        return jwksJson;
    }

    @GetMapping(value = "/api/groups")
    @Operation(summary = "API Group 조회", description = "API Group을 조회한다.")
    public List<ApiGatewayGroupsVO> getApiGatewayGroups(
            @Parameter(name = "withApi", description = "API 정보 포함 여부", schema = @Schema(allowableValues = {"true","false"}, defaultValue = "false")) @RequestParam(value = "withApi", required = false, defaultValue = "false") boolean withApi
    ) throws Exception {
        log.debug("[BEGIN] getApiGatewayGroups");

        /**
         * header 정보로 요청 사용자 권한 체크
         */
        AuthUtils.isValid(ContextHolder.exeContext(), AuthUtils.checkUserSysUserNDevOpsBlockAuthPredicate());

        List<ApiGatewayGroupsVO> results = openapiService.getApiGatewayGroups(withApi);

        log.debug("[END  ] getApiGatewayGroups");

        return results;
    }

    @GetMapping(value = "/apis")
    @Operation(summary = "API 조회", description = "API를 조회한다.")
    public List<ApiGatewaysVO> getApiGateways() throws Exception {
        log.debug("[BEGIN] getApiGateways");

        /**
         * header 정보로 요청 사용자 권한 체크
         */
        AuthUtils.isValid(ContextHolder.exeContext(), AuthUtils.checkUserSysUserNDevOpsBlockAuthPredicate());

        List<ApiGatewaysVO> results = openapiService.getApiGateways(null);

        log.debug("[END  ] getApiGateways");

        return results;
    }

    @PostMapping(value = "/tokens")
    @Operation(summary = "API 토큰 발급", description = "API 토큰을 발급한다.")
    public void issueApiToken(
            @Parameter(name = "API 토큰 발급 생성 모델", description = "API 토큰 발급 생성 모델", required = true) @RequestBody ApiTokenIssueAddVO apiTokenIssueAdd
    ) throws Exception {
        log.debug("[BEGIN] issueApiToken");

        /**
         * header 정보로 요청 사용자 권한 체크
         */
        AuthUtils.isValid(ContextHolder.exeContext(), AuthUtils.checkUserSysUserNDevOpsBlockAuthPredicate());

        openapiService.issueApiToken(apiTokenIssueAdd);

        log.debug("[END  ] issueApiToken");
    }

    @PutMapping(value = "/tokens/{apiTokenIssueSeq}")
    @Operation(summary = "API 토큰 정보 수정", description = "API 토큰 정보를 수정한다.")
    public void editApiTokenIssue(
            @Parameter(name = "apiTokenIssueSeq", description = "API 토큰 발급 순번", required = true) @PathVariable Integer apiTokenIssueSeq,
            @Parameter(name = "API 토큰 발급 수정 모델", description = "API 토큰 발급 수정 모델", required = true) @RequestBody ApiTokenIssueEditVO apiTokenIssueEdit
    ) throws Exception {
        log.debug("[BEGIN] editApiTokenIssue");

        /**
         * header 정보로 요청 사용자 권한 체크
         */
        AuthUtils.isValid(ContextHolder.exeContext(), AuthUtils.checkUserSysUserNDevOpsBlockAuthPredicate());

        if (apiTokenIssueEdit != null) {
            apiTokenIssueEdit.setApiTokenIssueSeq(apiTokenIssueSeq);
        }

        openapiService.editApiTokenIssue(apiTokenIssueEdit);

        log.debug("[END  ] editApiTokenIssue");
    }

    @DeleteMapping(value = "/tokens/{apiTokenIssueSeq}")
    @Operation(summary = "API 토큰 회수", description = "API 토큰을 회수한다.")
    public ApiTokenVO revokeApiTokenIssue(
            @Parameter(name = "apiTokenIssueSeq", description = "API 토큰 발급 순번", required = true) @PathVariable Integer apiTokenIssueSeq,
            @Parameter(name = "accountSeq", description = "플랫폼번호", required = true) @RequestParam Integer accountSeq
    ) throws Exception {
        log.debug("[BEGIN] revokeApiTokenIssue");

        ApiTokenVO result = null;
        /**
         * header 정보로 요청 사용자 권한 체크
         */
        AuthUtils.isValid(ContextHolder.exeContext(), AuthUtils.checkUserSysUserNDevOpsBlockAuthPredicate());

        ApiTokenIssueDetailVO detail = openapiService.revokeApiTokenIssue(apiTokenIssueSeq, accountSeq);
        if (detail != null) {
            result = new ApiTokenVO();
            result.setApiTokenIssueSeq(detail.getApiTokenIssueSeq());
            result.setApiTokenName(detail.getApiTokenName());
        }

        log.debug("[END  ] revokeApiTokenIssue");

        return result;
    }

    @GetMapping(value = "/tokens")
    @Operation(summary = "API 토큰 발급 목록 조회", description = "API 토큰 발급 목록을 조회한다.")
    public ApiTokenIssueListVO getApiTokenIssueList (
            @Parameter(name = "accountSeq", description = "플랫폼번호", required = true) @RequestParam Integer accountSeq,
            @Parameter(name = "withApi", description = "API 정보 포함 여부", schema = @Schema(allowableValues = {"true","false"}, defaultValue = "false")) @RequestParam(value = "withApi", required = false, defaultValue = "false") boolean withApi,
            @Parameter(name = "nextPage", description = "요청페이지", required = true) @RequestParam(name = "nextPage") Integer nextPage,
            @Parameter(name = "itemPerPage", description = "페이지당 표시 갯수", required = true) @RequestParam(name = "itemPerPage") Integer itemPerPage,
            @Parameter(name = "searchColumn", description = "검색 컬럼", schema = @Schema(allowableValues = {"TOKEN_NAME","ISSUE_USER_ID","ISSUE_USER_NAME"})) @RequestParam(name = "searchColumn", required = false) String searchColumn,
            @Parameter(name = "searchKeyword", description = "검색어") @RequestParam(name = "searchKeyword", required = false) String searchKeyword
    ) throws Exception {
        log.debug("[BEGIN] getApiTokenIssueList");
        ApiTokenIssueListVO list;
        try {
            /**
             * header 정보로 요청 사용자 권한 체크
             */
            AuthUtils.isValid(ContextHolder.exeContext(), AuthUtils.checkUserSysUserNDevOpsBlockAuthPredicate());

            PagingUtils.validatePagingParams(nextPage, itemPerPage);

            list = openapiService.getApiTokenIssueList(accountSeq, withApi, nextPage, itemPerPage, searchColumn, searchKeyword, null, null);
        }
        catch (CocktailException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new CocktailException("getApitokenIssueList Fail.", ex, ExceptionType.CommonInquireFail, ExceptionBiz.OPENAPI);
        }
        log.debug("[END  ] getApiTokenIssueList");
        return list;
    }

    @GetMapping(value = "/tokens/download")
    @Operation(summary = "API 토큰 발급 목록 엑셀 다운로드", description = "API 토큰 발급 목록을 엑셀 다운로드한다.")
    public void downloadExcelApiTokenIssue (
            @Parameter(name = "accountSeq", description = "플랫폼번호", required = true) @RequestParam Integer accountSeq,
            @Parameter(name = "withApi", description = "API 정보 포함 여부", schema = @Schema(allowableValues = {"true","false"}, defaultValue = "true")) @RequestParam(value = "withApi", required = false, defaultValue = "true") boolean withApi,
            @Parameter(name = "searchColumn", description = "검색 컬럼", schema = @Schema(allowableValues = {"TOKEN_NAME","ISSUE_USER_ID","ISSUE_USER_NAME"})) @RequestParam(name = "searchColumn", required = false) String searchColumn,
            @Parameter(name = "searchKeyword", description = "검색어") @RequestParam(name = "searchKeyword", required = false) String searchKeyword,
            HttpServletResponse response
    ) throws Exception {
        log.debug("[BEGIN] downloadExcelApiTokenIssue");
        ApiTokenIssueListVO list;
        try {
            /**
             * header 정보로 요청 사용자 권한 체크
             */
            AuthUtils.isValid(ContextHolder.exeContext(), AuthUtils.checkUserSysUserNDevOpsBlockAuthPredicate());

            ApiTokenIssueSearchVO params = openapiService.setApiTokenIssueCommonParams(accountSeq, withApi, null, null, searchColumn, searchKeyword, null, null);
            openapiService.downloadExcelApiTokenIssues(response, params);
        }
        catch (CocktailException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new CocktailException("downloadExcelApiTokenIssue Fail.", ex, ExceptionType.CommonInquireFail, ExceptionBiz.OPENAPI);
        }
        log.debug("[END  ] downloadExcelApiTokenIssue");
    }

    @GetMapping(value = "/tokens/{apiTokenIssueSeq}")
    @Operation(summary = "API 토큰 발급 상세 조회", description = "API 토큰 발급 상세 정보를 조회한다.")
    public ApiTokenIssueDetailVO getApiTokenIssue (
            @Parameter(name = "apiTokenIssueSeq", description = "API 토큰 발급 순번", required = true) @PathVariable Integer apiTokenIssueSeq,
            @Parameter(name = "accountSeq", description = "플랫폼번호", required = true) @RequestParam Integer accountSeq,
            @Parameter(name = "withToken", description = "토큰 정보 포함 여부", schema = @Schema(allowableValues = {"true","false"}, defaultValue = "false")) @RequestParam(value = "withToken", required = false, defaultValue = "false") boolean withToken
    ) throws Exception {
        log.debug("[BEGIN] getApiTokenIssue");
        ApiTokenIssueDetailVO detail;
        try {
            /**
             * header 정보로 요청 사용자 권한 체크
             */
            AuthUtils.isValid(ContextHolder.exeContext(), AuthUtils.checkUserSysUserNDevOpsBlockAuthPredicate());

            detail = openapiService.getApiTokenIssue(accountSeq, apiTokenIssueSeq, withToken);
        }
        catch (CocktailException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new CocktailException("getApiTokenIssue Fail.", ex, ExceptionType.CommonInquireFail, ExceptionBiz.OPENAPI);
        }
        log.debug("[END  ] getApiTokenIssue");

        return detail;
    }

    @GetMapping(value = "/tokens/{apiTokenIssueSeq}/token")
    @Operation(summary = "API 토큰 조회", description = "API 토큰을 조회한다.")
    public ApiTokenVO getApiToken (
            @Parameter(name = "apiTokenIssueSeq", description = "API 토큰 발급 순번", required = true) @PathVariable Integer apiTokenIssueSeq,
            @Parameter(name = "accountSeq", description = "플랫폼번호", required = true) @RequestParam Integer accountSeq

    ) throws Exception {
        log.debug("[BEGIN] getApiToken");
        ApiTokenVO result = null;
        try {
            /**
             * header 정보로 요청 사용자 권한 체크
             */
            AuthUtils.isValid(ContextHolder.exeContext(), AuthUtils.checkUserSysUserNDevOpsBlockAuthPredicate());

            ApiTokenIssueDetailVO detail = openapiService.getApiTokenIssue(accountSeq, apiTokenIssueSeq, true);
            if (detail != null) {
                result = new ApiTokenVO();
                String token = detail.getToken();
                result.setApiTokenIssueSeq(detail.getApiTokenIssueSeq());
                result.setApiTokenName(detail.getApiTokenName());
                result.setToken(token);
            }
        }
        catch (CocktailException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new CocktailException("getApiToken Fail.", ex, ExceptionType.CommonInquireFail, ExceptionBiz.OPENAPI);
        }
        log.debug("[END  ] getApiToken");

        return result;
    }

    @GetMapping(value = "/tokens/history")
    @Operation(summary = "API 토큰 발급 이력 목록 조회", description = "API 토큰 발급 이력 목록을 조회한다.")
    public ApiTokenIssueHistoryListVO getApiTokenIssueHistoryList (
            @Parameter(name = "accountSeq", description = "플랫폼번호", required = true) @RequestParam Integer accountSeq,
            @Parameter(name = "withApi", description = "API 정보 포함 여부", schema = @Schema(allowableValues = {"true","false"}, defaultValue = "true")) @RequestParam(value = "withApi", required = false, defaultValue = "true") boolean withApi,
            @Parameter(name = "nextPage", description = "요청페이지", required = true) @RequestParam(name = "nextPage") Integer nextPage,
            @Parameter(name = "itemPerPage", description = "페이지당 표시 갯수", required = true) @RequestParam(name = "itemPerPage") Integer itemPerPage,
            @Parameter(name = "searchColumn", description = "검색 컬럼", schema = @Schema(allowableValues = {"TOKEN_NAME","ISSUE_USER_ID","ISSUE_USER_NAME"})) @RequestParam(name = "searchColumn", required = false) String searchColumn,
            @Parameter(name = "searchKeyword", description = "검색어") @RequestParam(name = "searchKeyword", required = false) String searchKeyword,
            @Parameter(name = "historyState", description = "발급 상태", schema = @Schema(allowableValues = {"GRANT","REVOKE","CHANGE","EXPIRE"})) @RequestParam(name = "historyState", required = false) String historyState,
            @Parameter(name = "startDate", description = "검색 시작 일자", example = "20230421000000") @RequestParam(name = "startDate", required = false) String startDate,
            @Parameter(name = "endDate", description = "검색 종료 일자", example = "20230428000000") @RequestParam(name = "endDate", required = false) String endDate
    ) throws Exception {
        log.debug("[BEGIN] getApiTokenIssueHistoryList");
        ApiTokenIssueHistoryListVO list;
        try {
            /**
             * header 정보로 요청 사용자 권한 체크
             */
            AuthUtils.isValid(ContextHolder.exeContext(), AuthUtils.checkUserSysUserNDevOpsBlockAuthPredicate());

            PagingUtils.validatePagingParams(nextPage, itemPerPage);

            list = openapiService.getApiTokenIssueHistoryList(accountSeq, withApi, historyState, nextPage, itemPerPage, searchColumn, searchKeyword, startDate, endDate);
        }
        catch (CocktailException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new CocktailException("getApiTokenIssueHistoryList Fail.", ex, ExceptionType.CommonInquireFail, ExceptionBiz.OPENAPI);
        }
        log.debug("[END  ] getApiTokenIssueHistoryList");
        return list;
    }

    @GetMapping(value = "/tokens/history/download")
    @Operation(summary = "API 토큰 발급 이력 목록 엑셀 다운로드", description = "API 토큰 발급 이력 목록을 엑셀 다운로드한다.")
    public void downloadExcelApiTokenIssueHistory (
            @Parameter(name = "accountSeq", description = "플랫폼번호", required = true) @RequestParam Integer accountSeq,
            @Parameter(name = "withApi", description = "API 정보 포함 여부", schema = @Schema(allowableValues = {"true","false"}, defaultValue = "true")) @RequestParam(value = "withApi", required = false, defaultValue = "true") boolean withApi,
            @Parameter(name = "searchColumn", description = "검색 컬럼", schema = @Schema(allowableValues = {"TOKEN_NAME","ISSUE_USER_ID","ISSUE_USER_NAME"})) @RequestParam(name = "searchColumn", required = false) String searchColumn,
            @Parameter(name = "searchKeyword", description = "검색어") @RequestParam(name = "searchKeyword", required = false) String searchKeyword,
            @Parameter(name = "historyState", description = "발급 상태", schema = @Schema(allowableValues = {"GRANT","REVOKE","CHANGE","EXPIRE"})) @RequestParam(name = "historyState", required = false) String historyState,
            @Parameter(name = "startDate", description = "검색 시작 일자", example = "20230421000000") @RequestParam(name = "startDate", required = false) String startDate,
            @Parameter(name = "endDate", description = "검색 종료 일자", example = "20230428000000") @RequestParam(name = "endDate", required = false) String endDate,
            HttpServletResponse response
    ) throws Exception {
        log.debug("[BEGIN] downloadExcelApiTokenIssueHistory");
        ApiTokenIssueListVO list;
        try {
            /**
             * header 정보로 요청 사용자 권한 체크
             */
            AuthUtils.isValid(ContextHolder.exeContext(), AuthUtils.checkUserSysUserNDevOpsBlockAuthPredicate());

            ApiTokenIssueSearchVO params = openapiService.setApiTokenIssueCommonParams(accountSeq, withApi, historyState, null, searchColumn, searchKeyword, startDate, endDate);
            openapiService.downloadExcelApiTokenIssuesHistory(response, params);
        }
        catch (CocktailException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new CocktailException("downloadExcelApiTokenIssueHistory Fail.", ex, ExceptionType.CommonInquireFail, ExceptionBiz.OPENAPI);
        }
        log.debug("[END  ] downloadExcelApiTokenIssueHistory");
    }

    @GetMapping(value = "/tokens/audit")
    @Operation(summary = "API 토큰 감사 로그 목록 조회", description = "API 토큰 감사 로그 목록을 조회한다.")
    public ApiTokenAuditLogListVO getApiTokenAuditLogList (
            @Parameter(name = "accountSeq", description = "플랫폼번호", required = true) @RequestParam Integer accountSeq,
            @Parameter(name = "withTCnt", description = "페이징 총 카운트 정보 포함 여부", schema = @Schema(allowableValues = {"true","false"}, defaultValue = "false")) @RequestParam(value = "withTCnt", required = false, defaultValue = "false") boolean withTCnt,
            @Parameter(name = "nextPage", description = "요청페이지", required = true) @RequestParam(name = "nextPage") Integer nextPage,
            @Parameter(name = "itemPerPage", description = "페이지당 표시 갯수", required = true) @RequestParam(name = "itemPerPage") Integer itemPerPage,
            @Parameter(name = "resultCode", description = "처리 결과 코드", schema = @Schema(allowableValues = {"SUCCESS", "FAILURE"})) @RequestParam(name = "resultCode", required = false) String resultCode,
            @Parameter(name = "searchColumn", description = "검색 컬럼", schema = @Schema(allowableValues = {"API_NAME","TOKEN_NAME","CLIENT_IP"})) @RequestParam(name = "searchColumn", required = false) String searchColumn,
            @Parameter(name = "searchKeyword", description = "검색어") @RequestParam(name = "searchKeyword", required = false) String searchKeyword,
            @Parameter(name = "startDate", description = "검색 시작 일자", example = "20230421000000") @RequestParam(name = "startDate", required = false) String startDate,
            @Parameter(name = "endDate", description = "검색 종료 일자", example = "20230428000000") @RequestParam(name = "endDate", required = false) String endDate
    ) throws Exception {
        log.debug("[BEGIN] getApiTokenAuditLogList");
        ApiTokenAuditLogListVO list;
        try {
            /**
             * header 정보로 요청 사용자 권한 체크
             */
            AuthUtils.isValid(ContextHolder.exeContext(), AuthUtils.checkUserSysUserNDevOpsBlockAuthPredicate());

            PagingUtils.validatePagingParams(nextPage, itemPerPage);

            list = openapiService.getApiTokenAuditLogList(accountSeq, withTCnt, nextPage, itemPerPage, resultCode, searchColumn, searchKeyword, startDate, endDate);
        }
        catch (CocktailException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new CocktailException("getApiTokenAuditLogList Fail.", ex, ExceptionType.CommonInquireFail, ExceptionBiz.OPENAPI);
        }
        log.debug("[END  ] getApiTokenAuditLogList");
        return list;
    }

    @GetMapping(value = "/tokens/audit/count")
    @Operation(summary = "API 토큰 감사 로그 목록 총 카운트 조회", description = "API 토큰 감사 로그 목록 총 카운트를 조회한다.")
    public ListCountVO getApiTokenAuditLogListTotalCount (
            @Parameter(name = "accountSeq", description = "플랫폼번호", required = true) @RequestParam Integer accountSeq,
            @Parameter(name = "resultCode", description = "처리 결과 코드", schema = @Schema(allowableValues = {"SUCCESS", "FAILURE"})) @RequestParam(name = "resultCode", required = false) String resultCode,
            @Parameter(name = "searchColumn", description = "검색 컬럼", schema = @Schema(allowableValues = {"API_NAME","TOKEN_NAME","CLIENT_IP"})) @RequestParam(name = "searchColumn", required = false) String searchColumn,
            @Parameter(name = "searchKeyword", description = "검색어") @RequestParam(name = "searchKeyword", required = false) String searchKeyword,
            @Parameter(name = "startDate", description = "검색 시작 일자", example = "20230421000000") @RequestParam(name = "startDate", required = false) String startDate,
            @Parameter(name = "endDate", description = "검색 종료 일자", example = "20230428000000") @RequestParam(name = "endDate", required = false) String endDate
    ) throws Exception {
        log.debug("[BEGIN] getApiTokenAuditLogListTotalCount");
        ListCountVO result;
        try {
            /**
             * header 정보로 요청 사용자 권한 체크
             */
            AuthUtils.isValid(ContextHolder.exeContext(), AuthUtils.checkUserSysUserNDevOpsBlockAuthPredicate());

            result = openapiService.getApiTokenAuditLogListTotalCount(accountSeq, resultCode, searchColumn, searchKeyword, startDate, endDate);
        }
        catch (CocktailException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new CocktailException("getApiTokenAuditLogListTotalCount Fail.", ex, ExceptionType.CommonInquireFail, ExceptionBiz.OPENAPI);
        }
        log.debug("[END  ] getApiTokenAuditLogListTotalCount");
        return result;
    }

    @GetMapping(value = "/tokens/audit/download")
    @Operation(summary = "API 토큰 감사 로그 목록 엑셀 다운로드", description = "API 토큰 감사 로그 목록을 엑셀 다운로드한다.")
    public void downloadExcelApiTokenAuditLog (
            @Parameter(name = "accountSeq", description = "플랫폼번호", required = true) @RequestParam Integer accountSeq,
            @Parameter(name = "resultCode", description = "처리 결과 코드", schema = @Schema(allowableValues = {"SUCCESS", "FAILURE"})) @RequestParam(name = "resultCode", required = false) String resultCode,
            @Parameter(name = "searchColumn", description = "검색 컬럼", schema = @Schema(allowableValues = {"API_NAME","TOKEN_NAME","CLIENT_IP"})) @RequestParam(name = "searchColumn", required = false) String searchColumn,
            @Parameter(name = "searchKeyword", description = "검색어") @RequestParam(name = "searchKeyword", required = false) String searchKeyword,
            @Parameter(name = "startDate", description = "검색 시작 일자", example = "20230421000000") @RequestParam(name = "startDate", required = false) String startDate,
            @Parameter(name = "endDate", description = "검색 종료 일자", example = "20230428000000") @RequestParam(name = "endDate", required = false) String endDate,
            HttpServletResponse response
    ) throws Exception {
        log.debug("[BEGIN] downloadExcelApiTokenAuditLog");
        ApiTokenIssueListVO list;
        try {
            /**
             * header 정보로 요청 사용자 권한 체크
             */
            AuthUtils.isValid(ContextHolder.exeContext(), AuthUtils.checkUserSysUserNDevOpsBlockAuthPredicate());

            ApiTokenIssueSearchVO params = openapiService.setApiTokenIssueCommonParams(accountSeq, false, null, resultCode, searchColumn, searchKeyword, startDate, endDate);
            openapiService.downloadExcelApiTokenAuditLog(response, params);
        }
        catch (CocktailException ex) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            throw ex;
        }
        catch (Exception ex) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            throw new CocktailException("downloadExcelApiTokenAuditLog Fail.", ex, ExceptionType.CommonInquireFail, ExceptionBiz.OPENAPI);
        }
        log.debug("[END  ] downloadExcelApiTokenAuditLog");
    }

    @GetMapping(value = "/tokens/audit/download/csv")
    @Operation(summary = "API 토큰 감사 로그 목록 CSV 다운로드", description = "API 토큰 감사 로그 목록을 CSV 다운로드한다.")
    public void downloadCsvApiTokenAuditLog (
            @Parameter(name = "accountSeq", description = "플랫폼번호", required = true) @RequestParam Integer accountSeq,
            @Parameter(name = "resultCode", description = "처리 결과 코드", schema = @Schema(allowableValues = {"SUCCESS", "FAILURE"})) @RequestParam(name = "resultCode", required = false) String resultCode,
            @Parameter(name = "searchColumn", description = "검색 컬럼", schema = @Schema(allowableValues = {"API_NAME","TOKEN_NAME","CLIENT_IP"})) @RequestParam(name = "searchColumn", required = false) String searchColumn,
            @Parameter(name = "searchKeyword", description = "검색어") @RequestParam(name = "searchKeyword", required = false) String searchKeyword,
            @Parameter(name = "startDate", description = "검색 시작 일자", example = "20230421000000") @RequestParam(name = "startDate", required = false) String startDate,
            @Parameter(name = "endDate", description = "검색 종료 일자", example = "20230428000000") @RequestParam(name = "endDate", required = false) String endDate,
            HttpServletResponse response
    ) throws Exception {
        log.debug("[BEGIN] downloadCsvApiTokenAuditLog");
        ApiTokenIssueListVO list;
        try {
            /**
             * header 정보로 요청 사용자 권한 체크
             */
            AuthUtils.isValid(ContextHolder.exeContext(), AuthUtils.checkUserSysUserNDevOpsBlockAuthPredicate());

            ApiTokenIssueSearchVO params = openapiService.setApiTokenIssueCommonParams(accountSeq, false, null, resultCode, searchColumn, searchKeyword, startDate, endDate);
            openapiService.downloadCsvApiTokenAuditLog(response, params);
        }
        catch (CocktailException ex) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            throw ex;
        }
        catch (Exception ex) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            throw new CocktailException("downloadCsvApiTokenAuditLog Fail.", ex, ExceptionType.CommonInquireFail, ExceptionBiz.OPENAPI);
        }
        log.debug("[END  ] downloadCsvApiTokenAuditLog");
    }
}
