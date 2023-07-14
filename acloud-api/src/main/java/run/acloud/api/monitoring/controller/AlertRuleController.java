package run.acloud.api.monitoring.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import run.acloud.api.monitoring.service.AlertRuleService;
import run.acloud.api.monitoring.service.AlertUserService;
import run.acloud.api.monitoring.vo.AlertRuleListVO;
import run.acloud.api.monitoring.vo.AlertRuleVO;
import run.acloud.api.monitoring.vo.AlertUserListVO;
import run.acloud.api.monitoring.vo.AlertUserVO;
import run.acloud.commons.annotations.InHouse;
import run.acloud.commons.util.AuthUtils;
import run.acloud.commons.util.PagingUtils;
import run.acloud.commons.vo.ResultVO;
import run.acloud.framework.context.ContextHolder;
import run.acloud.framework.enums.ExceptionBiz;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;

import java.util.List;

@Tag(name = "Alert Rule", description = "Alert Rule에 대한 관리 기능을 제공한다.")
@Slf4j
@RestController
@RequestMapping(value = "/api/alert")
public class AlertRuleController {

    @Autowired
    private AlertRuleService alertRuleService;

    @Autowired
    private AlertUserService alertUserService;

    @GetMapping(value = "/{apiVersion}/list")
    @Operation(summary = "Alert Rule 목록", description = "Alert Rule 목록 조회한다.")
    public AlertRuleListVO getAlertRules(
        @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v1"}), required = true) @PathVariable String apiVersion,
        @Parameter(name = "alertState", description = "상태", schema = @Schema(allowableValues = {"warning","critical"})) @RequestParam(name = "alertState", required = false) String alertState,
        @Parameter(name = "order", description = "정렬 순서", schema = @Schema(allowableValues = {"DESC","ASC"}, defaultValue = "ASC"), required = true) @RequestParam(name = "order", defaultValue = "ASC") String order,
        @Parameter(name = "orderColumn", description = "정렬기준컬럼", schema = @Schema(allowableValues = {"created","audit_log_seq","alert_name"}, defaultValue = "alert_name")) @RequestParam(name = "orderColumn", defaultValue = "alert_name", required = false) String orderColumn,
        @Parameter(name = "nextPage", description = "요청페이지", required = true) @RequestParam(name = "nextPage") Integer nextPage,
        @Parameter(name = "itemPerPage", description = "페이지당 표시 갯수", required = true) @RequestParam(name = "itemPerPage") Integer itemPerPage,
        @Parameter(name = "searchColumn", schema = @Schema(allowableValues = {"NAME_OR_DESC","ALERT_NAME", "DESCRIPTION"}, defaultValue = "NAME_OR_DESC"), description = "검색 컬럼") @RequestParam(name = "searchColumn", required = false, defaultValue = "NAME_OR_DESC") String searchColumn,
        @Parameter(name = "searchKeyword", description = "검색어") @RequestParam(name = "searchKeyword", required = false) String searchKeyword,
        @Parameter(name = "maxId", description = "더보기 중복 방지를 위한 데이터셋 위치 지정") @RequestParam(name = "maxId", required = false) String maxId,
        @Parameter(name = "newId", description = "신규 데이터 추가를 위한 데이터셋 위치 지정") @RequestParam(name = "newId", required = false) String newId
    ) throws Exception {
        log.debug("[BEGIN] getAlertRules");

        AlertRuleListVO alertRuleList;
        try {
            PagingUtils.validatePagingParams(nextPage, itemPerPage);
            PagingUtils.validatePagingParamsOrderColumn(orderColumn, "created", "audit_log_seq", "alert_name");
            PagingUtils.validatePagingParamsOrder(order, "ASC", "DESC");

//            // order column 고정 (alert규칙번호) : 추후 확장..
//            if("DESC".equalsIgnoreCase(order)) {
//                orderColumn = "created";  // DESC일 경우 Descending Index를 태움.
//            }
//            else {
//                orderColumn = "alert_rule_seq"; // ASC일 경우 PK Index 태움.
//            }

            alertRuleList = alertRuleService.getAlertRules(alertState, order, orderColumn, nextPage, itemPerPage, searchColumn, searchKeyword, maxId, newId);
        }
        catch (CocktailException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new CocktailException("getAlertRules Fail.", ex, ExceptionType.CommonInquireFail);
        }

        log.debug("[END  ] getAlertRules");
        return alertRuleList;
    }

    @InHouse
    @GetMapping(value = "/{apiVersion}/list/excel")
    @Operation(summary = "Alert Rule 엑셀 다운로드", description = "Alert Rule 엑셀 다운로드")
    public void downloadExcelAlertRules(
        @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v1"}), required = true) @PathVariable String apiVersion,
        @Parameter(name = "alertState", description = "상태", schema = @Schema(allowableValues = {"warning","critical"})) @RequestParam(name = "alertState", required = false) String alertState,
        @Parameter(name = "searchColumn", schema = @Schema(allowableValues = {"NAME_OR_DESC", "ALERT_NAME", "DESCRIPTION"}, defaultValue = "NAME_OR_DESC"), description = "검색 컬럼") @RequestParam(name = "searchColumn", required = false, defaultValue = "NAME_OR_DESC") String searchColumn,
        @Parameter(name = "searchKeyword", description = "검색어") @RequestParam(name = "searchKeyword", required = false) String searchKeyword,
        HttpServletResponse response
    ) throws Exception {
        log.debug("[BEGIN] downloadExcelAlertRules");

        alertRuleService.downloadExcelAlertRules(response, alertState, searchColumn, searchKeyword);

        log.debug("[END  ] downloadExcelAlertRules");
    }

    @GetMapping(value = "/{apiVersion}/{alertRuleSeq}")
    @Operation(summary = "Alert Rule 상세", description = "Alert Rule 상세 조회한다.")
    public AlertRuleVO getAlertRule(
        @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v1"}), required = true) @PathVariable String apiVersion,
        @Parameter(description = "Alert Rule 번호", required = true) @PathVariable Integer alertRuleSeq
    ) throws Exception {
        log.debug("[BEGIN] getAlertRule");

        AlertRuleVO alertRule;
        try {
            alertRule = alertRuleService.getAlertRule(alertRuleSeq);
        }
        catch (CocktailException ex) {
            throw ex;
        }
        catch (Exception ex) {
            if(log.isDebugEnabled()) log.debug("trace log ", ex);
            throw new CocktailException("getAlertRule Fail.", ex, ExceptionType.CommonInquireFail);
        }

        log.debug("[END  ] getAlertRule");
        return alertRule;
    }

    @PostMapping(value = "/{apiVersion}/rule")
    @Operation(summary = "Alert Rule 생성", description = "Alert Rule 생성한다.")
    public AlertRuleVO addAlertRule(
        @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v1"}), required = true) @PathVariable String apiVersion,
        @RequestBody @Validated AlertRuleVO rule
    ) throws Exception {

        log.debug("[BEGIN] addAlertRule");
        AlertRuleVO result;

        /**
         * DevOps 권한의 사용자는 접근이 불가함..
         */
        AuthUtils.checkAuth(ContextHolder.exeContext(), AuthUtils.checkUserDevOpsBlockAuthPredicate());

        rule.setCreator(ContextHolder.exeContext().getUserSeq());
        rule.setUpdater(ContextHolder.exeContext().getUserSeq());

        result = alertRuleService.addAlertRule(rule);

        log.debug("[END  ] addAlertRule");

        return result;
    }

    @PutMapping(value = "/{apiVersion}/rule/{alertRuleSeq}")
    @Operation(summary = "Alert Rule 수정", description = "Alert Rule 수정한다.")
    public AlertRuleVO editAlertRule(
        @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v1"}), required = true) @PathVariable String apiVersion,
        @Parameter(description = "alertRuleSeq", required = true) @PathVariable Integer alertRuleSeq,
        @RequestBody @Validated AlertRuleVO rule
    ) throws Exception {

        log.debug("[BEGIN] editAlertRule");
        AlertRuleVO result;

        /**
         * DevOps 권한의 사용자는 접근이 불가함..
         */
        AuthUtils.checkAuth(ContextHolder.exeContext(), AuthUtils.checkUserDevOpsBlockAuthPredicate());

        AlertRuleVO currRule = alertRuleService.getAlertRule(alertRuleSeq);
        if (currRule == null) {
            throw new CocktailException("Alert Rule not found", ExceptionType.UserIdNotFound);
        }else {
            rule.setAlertRuleSeq(alertRuleSeq);
            rule.setUpdater(ContextHolder.exeContext().getUserSeq());
            result = alertRuleService.updateAlertRule(rule);
        }

        log.debug("[END  ] editAlertRule");

        return result;
    }

    @DeleteMapping(value = "/{apiVersion}/rule/{alertRuleSeq}")
    @Operation(summary = "Alert Rule 삭제", description = "Alert Rule 삭제한다.")
    public AlertRuleVO deleteAlertRule(
        @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v1"}), required = true) @PathVariable String apiVersion,
        @Parameter(description = "alertRuleSeq", required = true) @PathVariable Integer alertRuleSeq
    ) throws Exception {

        log.debug("[BEGIN] deleteAlertRule");
        AlertRuleVO result;

        /**
         * DevOps 권한의 사용자는 접근이 불가함..
         */
        AuthUtils.checkAuth(ContextHolder.exeContext(), AuthUtils.checkUserDevOpsBlockAuthPredicate());

        AlertRuleVO alertRule = new AlertRuleVO();
        alertRule.setAlertRuleSeq(alertRuleSeq);
        alertRule.setUpdater(ContextHolder.exeContext().getUserSeq());
        result = alertRuleService.removeAlertRule(alertRule);

        log.debug("[END  ] deleteAlertRule");

        return result;
    }

    @InHouse
    @GetMapping("/{apiVersion}/ruleid/{alertRuleId}/check")
    @Operation(summary = "Alert Rule ID 사용 여부 확인", description = "추가하려는 AlertRuleID가 이미 사용하고 있는 것인지 검사한다.")
    public ResultVO isServerNameUsed(
        @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v1"}), required = true) @PathVariable String apiVersion,
        @Parameter(description = "alertRuleId", required = true) @PathVariable String alertRuleId
    ) throws Exception {
        ResultVO r = new ResultVO();
        try {
            r.putKeyValue("exists", alertRuleService.checkRuleIdIfExists(alertRuleId));
        }
        catch (DataAccessException de) {
            throw de; // DB 처리중 Runtime Exception이 발생하면 처리를 공통 ExceptionHandler 로 던지고 공통 ExceptionHandler에서 처리
        }
        catch (CocktailException ce) {
            throw ce; // CocktailException이 발생하면 발생한 예외 처리 케이스를 그대로 사용
        }
        catch (Exception e) {
            throw new CocktailException("Error during data query.", e, ExceptionType.CommonInquireFail, ExceptionBiz.ALERT);
        }
        return r;
    }


    @GetMapping(value = "/{apiVersion}/user/list")
    @Operation(summary = "Alert Rule 수신 사용자 목록", description = "Alert Rule 수신 사용자 목록 조회한다.")
    public AlertUserListVO getAlertUsers(
            @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v1"}), required = true) @PathVariable String apiVersion,
            @Parameter(name = "accountSeq", description = "accountSeq", required = true) @RequestParam(name = "accountSeq") Integer accountSeq,
            @Parameter(name = "order", description = "정렬 순서", schema = @Schema(allowableValues = {"DESC","ASC"}, defaultValue = "ASC"), required = true) @RequestParam(name = "order", defaultValue = "ASC") String order,
            @Parameter(name = "orderColumn", description = "정렬기준컬럼", schema = @Schema(allowableValues = {"created","updated, user_seq, user_name"}, defaultValue = "user_name")) @RequestParam(name = "orderColumn", defaultValue = "user_name", required = false) String orderColumn,
            @Parameter(name = "nextPage", description = "요청페이지", required = true) @RequestParam(name = "nextPage") Integer nextPage,
            @Parameter(name = "itemPerPage", description = "페이지당 표시 갯수", required = true) @RequestParam(name = "itemPerPage") Integer itemPerPage,
            @Parameter(name = "searchColumn", schema = @Schema(allowableValues = {"USER_NAME","PHONE_NUMBER","KAKAO_ID","EMAIL"}), description = "검색 컬럼") @RequestParam(name = "searchColumn", required = false) String searchColumn,
            @Parameter(name = "searchKeyword", description = "검색어") @RequestParam(name = "searchKeyword", required = false) String searchKeyword,
            @Parameter(name = "maxId", description = "더보기 중복 방지를 위한 데이터셋 위치 지정") @RequestParam(name = "maxId", required = false) String maxId
    ) throws Exception {
        log.debug("[BEGIN] getAlertUsers");

        AlertUserListVO list;
        try {
            PagingUtils.validatePagingParams(nextPage, itemPerPage);
            PagingUtils.validatePagingParamsOrderColumn(orderColumn, "created", "updated", "user_seq", "user_name");
            PagingUtils.validatePagingParamsOrder(order, "ASC", "DESC");

            list = alertUserService.getAlertUsers(accountSeq, searchColumn, searchKeyword, order, orderColumn, nextPage, itemPerPage, maxId);
        }
        catch (CocktailException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new CocktailException("getAlertUsers Fail.", ex, ExceptionType.CommonInquireFail);
        }

        log.debug("[END  ] getAlertUsers");
        return list;
    }

    @PostMapping(value = "/{apiVersion}/user")
    @Operation(summary = "Alert Rule 수신 사용자 생성", description = "Alert Rule 수신 사용자를 생성한다.")
    public void addAlertUser(
            @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v1"}), required = true) @PathVariable String apiVersion,
            @RequestBody @Validated AlertUserVO user
    ) throws Exception {

        log.debug("[BEGIN] addAlertUser");

        /**
         * DevOps 권한의 사용자는 접근이 불가함..
         */
        AuthUtils.checkAuth(ContextHolder.exeContext(), AuthUtils.checkUserDevOpsBlockAuthPredicate());

        alertUserService.addAlertUser(user);

        log.debug("[END  ] addAlertUser");
    }

    @PutMapping(value = "/{apiVersion}/user/{alertUserSeq}")
    @Operation(summary = "Alert Rule 수신 사용자 수정", description = "Alert Rule 수신 사용자를 수정한다.")
    public void editAlertUser(
            @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v1"}), required = true) @PathVariable String apiVersion,
            @Parameter(description = "alertUserSeq", required = true) @PathVariable Integer alertUserSeq,
            @RequestBody @Validated AlertUserVO user
    ) throws Exception {

        log.debug("[BEGIN] editAlertUser");

        /**
         * DevOps 권한의 사용자는 접근이 불가함..
         */
        AuthUtils.checkAuth(ContextHolder.exeContext(), AuthUtils.checkUserDevOpsBlockAuthPredicate());

        AlertUserVO currUser = alertUserService.getAlertUser(alertUserSeq);
        if (currUser == null) {
            throw new CocktailException("Alert User not found", ExceptionType.UserIdNotFound);
        }else {
            alertUserService.updateAlertUser(user);
        }

        log.debug("[END  ] editAlertUser");
    }

    @DeleteMapping(value = "/{apiVersion}/user/{alertUserSeq}")
    @Operation(summary = "Alert Rule 수신 사용자 삭제", description = "Alert Rule 수신 사용자를 삭제한다.")
    public void deleteAlertUser(
            @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v1"}), required = true) @PathVariable String apiVersion,
            @Parameter(description = "alertUserSeq", required = true) @PathVariable Integer alertUserSeq
    ) throws Exception {

        log.debug("[BEGIN] deleteAlertUser");

        /**
         * DevOps 권한의 사용자는 접근이 불가함..
         */
        AuthUtils.checkAuth(ContextHolder.exeContext(), AuthUtils.checkUserDevOpsBlockAuthPredicate());

        AlertUserVO alertUser = new AlertUserVO();
        alertUser.setAlertUserSeq(alertUserSeq);
        alertUser.setUpdater(ContextHolder.exeContext().getUserSeq());
        alertUserService.removeAlertUser(alertUser);

        log.debug("[END  ] deleteAlertUser");
    }

    @GetMapping(value = "/{apiVersion}/user/{alertUserSeq}")
    @Operation(summary = "Alert Rule 수신 사용자 상세", description = "Alert Rule 수신 사용자 상세 조회한다.")
    public AlertUserVO getAlertUser(
            @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v1"}), required = true) @PathVariable String apiVersion,
            @Parameter(description = "alertUserSeq", required = true) @PathVariable Integer alertUserSeq
    ) throws Exception {

        log.debug("[BEGIN] getAlertUser");

        /**
         * DevOps 권한의 사용자는 접근이 불가함..
         */
        AuthUtils.checkAuth(ContextHolder.exeContext(), AuthUtils.checkUserDevOpsBlockAuthPredicate());

        AlertUserVO currUser = alertUserService.getAlertUser(alertUserSeq);

        log.debug("[END  ] getAlertUser");

        return currUser;
    }

    @GetMapping(value = "/{apiVersion}/ruleid/{alertRuleId}/user")
    @Operation(summary = "Alert Rule을 수신받는 사용자 목록", description = "Alert Rule을 수신받는 사용자 목록 조회한다.")
    public List<AlertUserVO> getUsersByAlertRuleId(
        @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v1"}), required = true) @PathVariable String apiVersion,
        @Parameter(description = "Alert Rule 아이디", required = true) @PathVariable String alertRuleId,
        @Parameter(description = "Alert Rule target cluster-id", required = false) @RequestParam(required = false) String clusterId
    ) throws Exception {
        log.debug("[BEGIN] getUsersByAlertRuleId");

        List<AlertUserVO> users;
        try {
            users = alertRuleService.getUsersByAlertRuleId(alertRuleId, clusterId);
        }
        catch (CocktailException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new CocktailException("getUsersByAlertRuleId Fail.", ex, ExceptionType.CommonInquireFail);
        }

        log.debug("[END  ] getUsersByAlertRuleId");
        return users;
    }

    @PutMapping(value = "/{apiVersion}/{alertRuleSeq}/user")
    @Operation(summary = "Alert Rule에 사용자 등록/수정/삭제", description = "AlertRule을 수신할 사용자를 등록/수정/삭제한다.")
    public ResultVO updateUsersOfAlertRule(
        @Parameter(description = "Alert Rule 번호", required = true) @PathVariable Integer alertRuleSeq,
        @Parameter(description = "Alert 사용자 번호 목록", required = true) @RequestBody List<Integer> alertUserSeqs
    ) throws Exception {
        log.debug("[BEGIN] updateUsersOfAlertRule");

        /** header 정보로 요청 사용자 권한 체크 */
        AuthUtils.checkAuth(ContextHolder.exeContext(), AuthUtils.checkUserDevOpsBlockAuthPredicate());

        alertRuleService.updateUsersOfAlertRule(alertRuleSeq, alertUserSeqs, ContextHolder.exeContext().getUserSeq());

        log.debug("[END  ] updateUsersOfAlertRule");

        return new ResultVO();
    }

    @PutMapping(value = "/{apiVersion}/{alertRuleSeq}/cluster")
    @Operation(summary = "Alert Rule에 클러스터 등록/수정/삭제", description = "AlertRule을 수신할 클러스터를 등록/수정/삭제한다.")
    public ResultVO updateClustersOfAlertRule(
        @Parameter(description = "Alert Rule 번호", required = true) @PathVariable Integer alertRuleSeq,
        @Parameter(description = "Alert 클러스터 번호 목록", required = true) @RequestBody List<Integer> alertClusterSeqs
    ) throws Exception {
        log.debug("[BEGIN] updateClustersOfAlertRule");

        /** header 정보로 요청 사용자 권한 체크 */
        AuthUtils.checkAuth(ContextHolder.exeContext(), AuthUtils.checkUserDevOpsBlockAuthPredicate());

        alertRuleService.updateClustersOfAlertRule(alertRuleSeq, alertClusterSeqs, ContextHolder.exeContext().getUserSeq());

        log.debug("[END  ] updateClustersOfAlertRule");

        return new ResultVO();
    }


}