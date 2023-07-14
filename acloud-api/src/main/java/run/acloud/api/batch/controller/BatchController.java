package run.acloud.api.batch.controller;


import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import run.acloud.api.batch.service.BatchService;
import run.acloud.commons.annotations.InHouse;
import run.acloud.commons.vo.ExecutingContextVO;
import run.acloud.commons.vo.ResultVO;
import run.acloud.framework.context.ContextHolder;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;

@Tag(name = "Batch", description = "batch-server에 호출되는 batch 관련 기능을 제공한다.")
@InHouse
@Slf4j
@Controller
@RequestMapping("/api/batch")
public class BatchController {

    @Autowired
    private BatchService batchService;

    @PostMapping("/account/metering")
    @ResponseBody
    public ResultVO createAccountHourMetering(HttpServletRequest request
            , @RequestHeader(name = "client-server-name" ) String clientServerName
    ) throws Exception {
        log.debug("[BEGIN] createAccountHourMetering");
        ResultVO result = new ResultVO();

        // 접근 client 체크
        checkClient(clientServerName);

        // exec context에 admin user seq 설정
        setAdminUserToContext();

        int resultCount = 0;
        resultCount = batchService.createAccountHourMetering(request);

        log.debug("[END] createAccountHourMetering");
        result.setResult(resultCount);
        return result;
    }

    @PostMapping("/account/dailycharge/{baseDate}")
    @ResponseBody
    public ResultVO createAccountBillDailyCharge(HttpServletRequest request
            , @RequestHeader(name = "client-server-name" ) String clientServerName
            , @Parameter(name = "baseDate", description = "처리기준일자, 'NONE' 값이면 하루전 날짜 기준처리", example = "2019-07-01") @PathVariable String baseDate
    ) throws Exception {
        log.debug("[BEGIN] createAccountBillDailyCharge");
        ResultVO result = new ResultVO();

        // 접근 client 체크
        checkClient(clientServerName);

        // exec context에 admin user seq 설정
        setAdminUserToContext();

        int resultCount = 0;
        resultCount = batchService.createAccountBillDailyCharge(request, baseDate);

        log.debug("[END] createAccountBillDailyCharge");
        result.setResult(resultCount);
        return result;
    }

    @PostMapping("/account/billing/{baseMonth}")
    @ResponseBody
    public ResultVO createAccountBillingData(HttpServletRequest request
            , @RequestHeader(name = "client-server-name" ) String clientServerName
            , @Parameter(name = "baseMonth", description = "처리기준월, 'NONE' 값이면 이전달 기준처리", example = "2019-07") @PathVariable String baseMonth

    ) throws Exception {
        log.debug("[BEGIN] createAccountBillingData");
        ResultVO result = new ResultVO();

        // 접근 client 체크
        checkClient(clientServerName);

        // exec context에 admin user seq 설정
        setAdminUserToContext();

        int resultCount = 0;
        resultCount = batchService.createAccountBillingData(request, baseMonth);

        log.debug("[END] createAccountBillingData");
        result.setResult(resultCount);
        return result;
    }

    /**
     * 추후 ExecuteContext 에서 vo의 createor나 updater 셋팅할 수 있게 context 에 admin user seq 셋팅
     *
     */
    private void setAdminUserToContext(){
        ExecutingContextVO ctx = new ExecutingContextVO();
        ctx.setUserSeq(1); //admin user setting
        ctx.setUserRole("ADMIN");
        ContextHolder.exeContext(ctx);
    }

    /**
     * batch-server 에서 호출된 것인지 확인한다.<br/>
     * batch-server 외 에서 호출하려면 Header 셋팅해야함
     *
     * @param clientServerName
     */
    private void checkClient(String clientServerName){
        String checkServerNmae = "batch-server";

        if(!checkServerNmae.equals(clientServerName)){
            throw new CocktailException("사용할 수 있는 권한이 없습니다.", ExceptionType.NotSupportedServerType);
        }

    }

    /**
     * Kubeconfig 만료
     * api token 만료
     *
     * @param request
     * @param clientServerName
     * @param baseDate
     * @return
     * @throws Exception
     */
    @PostMapping("/useraccount/{baseDate}/expire")
    @ResponseBody
    public int expireUserAccountForKubeconfig(HttpServletRequest request
            , @RequestHeader(name = "client-server-name" ) String clientServerName
            , @Parameter(name = "baseDate", description = "처리기준일자", example = "2020-07-01") @PathVariable String baseDate
    ) throws Exception {
        log.debug("[BEGIN] expireUserAccountForKubeconfig");

        // 접근 client 체크
        checkClient(clientServerName);

        // exec context에 admin user seq 설정
        setAdminUserToContext();

        int resultCount = batchService.expireUserAccountForKubeconfig(request, baseDate);

        // API Token 만료
        batchService.expireApiToken(request, baseDate);

        log.debug("[END] expireUserAccountForKubeconfig");

        return resultCount;
    }

    @PostMapping("/auditlog/{baseDate}/delete")
    @ResponseBody
    public int deleteCocktailAuditLog(HttpServletRequest request
            , @RequestHeader(name = "client-server-name" ) String clientServerName
            , @Parameter(name = "baseDate", description = "처리기준일자", example = "2021-07-08") @PathVariable String baseDate
    ) throws Exception {
        log.debug("[BEGIN] deleteCocktailAuditLog");

        // 접근 client 체크
        checkClient(clientServerName);

        // exec context에 admin user seq 설정
        setAdminUserToContext();

        // 기존 audit 로그( audit_logs ) 테이블 데이터 삭제
        int resultCount = batchService.deleteCocktailAuditLog(request, baseDate);

        // audit_access_logs partition drop & create
        batchService.dropAndCreateAuditAccessLogPartition(baseDate);

        // api token audit 로그( api_token_audit_log ) 테이블 데이터 삭제
        batchService.deleteApiTokenAuditLog(request, baseDate);

        log.debug("[END] deleteCocktailAuditLog");

        return resultCount;
    }
}
