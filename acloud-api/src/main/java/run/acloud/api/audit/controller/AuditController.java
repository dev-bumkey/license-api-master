package run.acloud.api.audit.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import run.acloud.api.audit.service.AuditAccessService;
import run.acloud.api.audit.service.AuditService;
import run.acloud.api.audit.vo.AuditAccessLogListVO;
import run.acloud.api.audit.vo.AuditLogListVO;
import run.acloud.commons.util.PagingUtils;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;

/**
 * 칵테일 감사 로그에 대한 관리 기능을 제공한다.
 */
@Tag(name = "Audit Log", description = "칵테일 감사로그 관련 기능을 제공한다.")
@Slf4j
@RequestMapping(value = "/api/audit")
@RestController
@Validated
public class AuditController {

    @Autowired
    private AuditService auditService;

    @Autowired
    private AuditAccessService auditAccessService;

    @GetMapping(value = "/{apiVersion}/list")
    @Operation(summary = "칵테일 감사 로그 목록", description = "칵테일 감사 로그 목록 조회한다.")
    public AuditLogListVO getAuditLogs(
        @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = { "v3" }), required = true) @PathVariable String apiVersion,
        @Parameter(name = "order", description = "정렬 순서", schema = @Schema(allowableValues = { "DESC", "ASC" }, defaultValue = "ASC"), required = true) @RequestParam(name = "order", defaultValue = "ASC") String order,
        @Parameter(name = "orderColumn", description = "정렬기준컬럼", schema = @Schema(allowableValues = { "created", "audit_log_seq" })) @RequestParam(name = "orderColumn", required = false) String orderColumn,
        @Parameter(name = "nextPage", description = "요청페이지", required = true) @RequestParam(name = "nextPage") Integer nextPage,
        @Parameter(name = "itemPerPage", description = "페이지당 표시 갯수", required = true) @RequestParam(name = "itemPerPage") Integer itemPerPage,
        @Parameter(name = "searchColumn", description = "검색 컬럼") @RequestParam(name = "searchColumn", required = false) String searchColumn,
        @Parameter(name = "searchKeyword", description = "검색어") @RequestParam(name = "searchKeyword", required = false) String searchKeyword,
        @Parameter(name = "startDate", description = "검색 시작 일자") @RequestParam(name = "startDate", required = false) String startDate,
        @Parameter(name = "endDate", description = "검색 종료 일자") @RequestParam(name = "endDate", required = false) String endDate,
        @Parameter(name = "maxId", description = "더보기 중복 방지를 위한 데이터셋 위치 지정") @RequestParam(name = "maxId", required = false) String maxId,
        @Parameter(name = "newId", description = "신규 데이터 추가를 위한 데이터셋 위치 지정") @RequestParam(name = "newId", required = false) String newId
    ) throws CocktailException {

        AuditLogListVO auditLogList;

        try {

            this.checkPageAndOrder(nextPage, itemPerPage, order);

            // order column 고정 (시간 역순) : 추후 확장..
            String localOrderColumn;
            if("DESC".equalsIgnoreCase(order)) {
                localOrderColumn = "created";  // DESC일 경우 Descending Index를 태움.
            }
            else {
                localOrderColumn = "audit_log_seq"; // ASC일 경우 PK Index 태움.
            }

            auditLogList = auditService.getAuditLogs(order, localOrderColumn, nextPage, itemPerPage, searchColumn, searchKeyword, maxId, newId, startDate, endDate);
        }
        catch (CocktailException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new CocktailException("getAuditLogs Fail.", ex, ExceptionType.CommonInquireFail);
        }

        return auditLogList;
    }

    @GetMapping(value = "/access/{apiVersion}/list")
    @Operation(summary = "칵테일 감사 접근 로그 목록", description = "칵테일 감사 접근 로그 목록 조회한다.")
    public AuditAccessLogListVO getAuditAccessLogs(
            @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = { "v3" }), required = true) @PathVariable String apiVersion,
            @Parameter(name = "order", description = "정렬 순서", schema = @Schema(allowableValues = { "DESC", "ASC" }, defaultValue = "ASC"), required = true) @RequestParam(name = "order", defaultValue = "ASC") String order,
            @Parameter(name = "orderColumn", description = "정렬기준컬럼", schema = @Schema(allowableValues = { "log_dt, log_seq" })) @RequestParam(name = "orderColumn", required = false) String orderColumn,
            @Parameter(name = "nextPage", description = "요청페이지", required = true) @RequestParam(name = "nextPage") Integer nextPage,
            @Parameter(name = "itemPerPage", description = "페이지당 표시 갯수", required = true) @RequestParam(name = "itemPerPage") Integer itemPerPage,
            @Parameter(name = "searchColumn", description = "검색 컬럼") @RequestParam(name = "searchColumn", required = false) String searchColumn,
            @Parameter(name = "searchKeyword", description = "검색어") @RequestParam(name = "searchKeyword", required = false) String searchKeyword,
            @Parameter(name = "startDate", description = "검색 시작 일자") @RequestParam(name = "startDate", required = false) String startDate,
            @Parameter(name = "endDate", description = "검색 종료 일자") @RequestParam(name = "endDate", required = false) String endDate,
            @Parameter(name = "maxId", description = "더보기 중복 방지를 위한 데이터셋 위치 지정") @RequestParam(name = "maxId", required = false) String maxId,
            @Parameter(name = "newId", description = "신규 데이터 추가를 위한 데이터셋 위치 지정") @RequestParam(name = "newId", required = false) String newId
    ) throws CocktailException {
        AuditAccessLogListVO auditAccessLogList;

        try {

            this.checkPageAndOrder(nextPage, itemPerPage, order);

            // order column 고정 (시간 역순) : 추후 확장..
            String localOrderColumn;
            if("DESC".equalsIgnoreCase(order)) {
                localOrderColumn = "log_dt";  // DESC일 경우 Descending Index를 태움.
            }
            else {
                localOrderColumn = "log_seq"; // ASC일 경우 PK Index 태움.
            }

            auditAccessLogList = auditAccessService.getAuditAccessLogs(order, localOrderColumn, nextPage, itemPerPage, searchColumn, searchKeyword, maxId, newId, startDate, endDate);
        }
        catch (CocktailException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new CocktailException("getAuditAccessLogs Fail.", ex, ExceptionType.CommonInquireFail);
        }

        return auditAccessLogList;
    }

    private void checkPageAndOrder(Integer nextPage, Integer itemPerPage, String order) throws CocktailException{

        PagingUtils.validatePagingParams(nextPage, itemPerPage);
        PagingUtils.validatePagingParamsOrder(order, "ASC", "DESC");

    }

}
