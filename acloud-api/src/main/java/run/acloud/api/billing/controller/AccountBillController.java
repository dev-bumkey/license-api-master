package run.acloud.api.billing.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import run.acloud.api.auth.vo.UserVO;
import run.acloud.api.billing.enums.BillState;
import run.acloud.api.billing.service.AccountBillService;
import run.acloud.api.billing.vo.AccountBillVO;
import run.acloud.api.configuration.service.AccountGradeService;
import run.acloud.api.configuration.service.UserService;
import run.acloud.api.configuration.vo.AccountMeteringVO;
import run.acloud.commons.vo.ResultVO;
import run.acloud.framework.context.ContextHolder;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;

import java.util.List;

@Tag(name = "billing", description = "billing 관련 기능을 제공한다.")
@Slf4j
@RestController
@Validated
@RequestMapping(value = "/api/billing")
public class AccountBillController {

    @Autowired
    private AccountBillService accountBillService;

    @Autowired
    private UserService userService;

    @Autowired
    private AccountGradeService accountGradeService;

    @PostMapping(value = "")
    @Operation(summary = "청구서 생성", description = "계정에 대한 월별 청구서를 생성한다.")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "청구서") } )
    @ResponseBody
    public AccountBillVO addAccountBill(
            @Parameter(description = "템플릿 생성 모델", required = true) @RequestBody @Validated AccountBillVO accountBillVO
    ){
        log.debug("[BEGIN] addAccountBill");

        AccountBillVO accountBill = accountBillService.addAccountBill(accountBillVO);

        log.debug("[END] addAccountBill");
        return accountBill;
    }

    @Operation(summary = "청구서 수정", description = "청구서를 수정한다.")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "청구서 수정 요청") } )
    @PutMapping(value = "/{billSeq}")
    public AccountBillVO updateAccountBill(
            @PathVariable Integer billSeq,
            @RequestBody @Validated AccountBillVO accountBillVO
    ){
        log.debug("[BEGIN] updateAccountBill");
        // validation, billSeq 와 accountBillVO의 billSeq 와 같은지 비교, 다르면 Exception
        if(!billSeq.equals(accountBillVO.getBillSeq())){
            throw new CocktailException("Billing sequence is invalid.", ExceptionType.BillingSequenceInvalid);
        }

        // 기존 데이터가 존재하는지 체크, billSeq 와 accountBillVO의 billSeq 와 같은지 비교, 다르면 Exception
        AccountBillVO currAccountBillVO = accountBillService.getAccountBillDetail(billSeq);
        if(currAccountBillVO == null) {
            throw new CocktailException("Billing data is not exist.", ExceptionType.BillingDataNotExist);
        }

        accountBillVO = accountBillService.updateAccountBill(accountBillVO);

        log.debug("[END] updateAccountBill");
        return accountBillVO;
    }

    @Operation(summary = "청구서 삭제", description = "청구서를 삭제한다.")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "결과") } )
    @DeleteMapping(value = "/{billSeq}")
    public ResultVO removeAccountBill(
            @PathVariable Integer billSeq
    ){
        log.debug("[BEGIN] removeAccountBill");
        AccountBillVO currAccountBillVO = accountBillService.getAccountBillDetail(billSeq);

        // 데이터가 존재할 때만 삭제
        if(currAccountBillVO != null){
            int result = accountBillService.removeAccountBill(currAccountBillVO);
            log.debug("delete account bill : "+result);
        }

        log.debug("[END] removeAccountBill");
        return new ResultVO();
    }

    @GetMapping(value = "/{billSeq}")
    @Operation(summary = "청구서 상세조회", description = "청구서를 상세 조회 한다.")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "청구서 상세 조회 결과") } )
    public AccountBillVO getAccountBillDetail( @Parameter(name = "billSeq", description = "청구서 번호", required = true) @PathVariable Integer billSeq ){
        log.debug("[BEGIN] getAccountBillDetail");

        AccountBillVO currAccountBillVO = accountBillService.getAccountBillDetail(billSeq);

        log.debug("[END] getAccountBillDetail");
        return currAccountBillVO;
    }

    @Operation(summary = "청구서 리스트 조회", description = "청구서 리스트 조회 한다.")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "결과") } )
    @GetMapping(value = "/search")
    public List<AccountBillVO> getAccountBills(
            @RequestParam(required = false) Integer accountSeq,
            @RequestParam(required = false) String organizationName,
            @RequestParam(required = false) String usedMonth,
            @RequestParam(required = false) BillState billState
    ) throws Exception {
        log.debug("[BEGIN] getAccountBills");

        // Check if accountSeq is user's
        checkDataOwner(accountSeq);

        AccountBillVO searchVO = new AccountBillVO();
        searchVO.setAccountSeq(accountSeq);
        searchVO.setUsedMonth(usedMonth);
        searchVO.setBillState(billState);
        searchVO.setOrganizationName(organizationName);

        List<AccountBillVO> accountBillVOList = accountBillService.getAccountBills(searchVO);

        log.debug("[BEGIN] getAccountBills");
        return accountBillVOList;
    }

    @Operation(summary = "미터링 데이터 리스트 조회", description = "미터리 데이터 리스트 조회 한다. (ADMIN만 사용가능)")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "결과") } )
    @GetMapping(value = "/metering/search")
    public List<AccountMeteringVO> getAccountHourMetering(
            @RequestParam(required = false) Integer accountSeq,
            @RequestParam(required = false) String searchStartDt,
            @RequestParam(required = false) String searchEndDt
    ) throws Exception {
        log.debug("[BEGIN] getAccountHourMetering");

        // Check if accountSeq is user's
        checkDataOwner(accountSeq);

        // 기준월에 따른 필요 날짜 계산
        DateTimeFormatter dateFmt = DateTimeFormat.forPattern("yyyy-MM-dd");
        searchEndDt = dateFmt.parseDateTime(searchEndDt).plusDays(1).toString(dateFmt);

        List<AccountMeteringVO> accountHourMeteringList = accountGradeService.getAccountHourMeterings(accountSeq, searchStartDt, searchEndDt, null, null, null);

        log.debug("[BEGIN] getAccountHourMetering");
        return accountHourMeteringList;
    }

    @Operation(summary = "계정별, 월별 미터링 데이터 리스트 조회", description = "미터리 데이터 리스트 조회 한다.")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "결과") } )
    @GetMapping(value = "/metering/account/{accountSeq}/{baseMonth}")
    public List<AccountMeteringVO> getAccountHourMetering(
            @PathVariable Integer accountSeq,
            @PathVariable String baseMonth
            ) throws Exception {
        log.debug("[BEGIN] getAccountHourMetering");

        // Check if accountSeq is user's
        checkDataOwner(accountSeq);

        // 기준월에 따른 필요 날짜 계산
        DateTimeFormatter monthFmt = DateTimeFormat.forPattern("yyyy-MM");
        DateTimeFormatter dayFmt = DateTimeFormat.forPattern("yyyy-MM-dd");

        DateTime baseMonthDateTime = monthFmt.parseDateTime(baseMonth);

        String searchStartDt = baseMonthDateTime.toString(dayFmt); // 기준일자, 월첫일
        String searchEndDt = baseMonthDateTime.toLocalDate().dayOfMonth().withMaximumValue().plusDays(1).toString(dayFmt); // 기준월 말일+1일 00:00:00

        List<AccountMeteringVO> accountHourMeteringList = accountGradeService.getAccountHourMeterings(accountSeq, searchStartDt, searchEndDt, null, null, null);

        log.debug("[BEGIN] getAccountHourMetering");
        return accountHourMeteringList;
    }

    @Operation(summary = "계정별, 해당일 미터링 데이터 리스트 조회", description = "미터리 데이터 리스트 조회 한다.")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "결과") } )
    @GetMapping(value = "/metering/account/{accountSeq}/date/{baseDate}")
    public List<AccountMeteringVO> getAccountHourMeteringByDate(
            @PathVariable Integer accountSeq,
            @PathVariable String baseDate
    ) throws Exception {
        log.debug("[BEGIN] getAccountHourMetering");

        // Check if accountSeq is user's
        checkDataOwner(accountSeq);

        // 기준월에 따른 필요 날짜 계산
        DateTimeFormatter dayFmt = DateTimeFormat.forPattern("yyyy-MM-dd");

        baseDate = dayFmt.parseDateTime(baseDate).toString(dayFmt);

        List<AccountMeteringVO> accountHourMeteringList = accountGradeService.getAccountHourMeterings(accountSeq, null, null, baseDate, null, null);

        log.debug("[BEGIN] getAccountHourMetering");
        return accountHourMeteringList;
    }

    /**
     * Check the accountSeq parameter whether accountSeq is user's.
     *
     * @param accountSeq
     * @throws Exception
     */
    private void checkDataOwner(Integer accountSeq) throws Exception {
        // ADMIN 이 아닐때 accountSeq 와 현재 로그인한 사용자의 accountSeq 가 다르면 오류
        // 다른 시스템의 정보를 볼 수 없도록 하기 위함.
        String userRole = ContextHolder.exeContext().getUserRole();
        if(!"ADMIN".equals(userRole)){
            // 사용자 정보 조회
            UserVO userVO = userService.getByUserSeq(ContextHolder.exeContext().getUserSeq());
            Integer userAccountSeq = userVO.getAccount().getAccountSeq();
            if(accountSeq == null || !accountSeq.equals(userAccountSeq)){
                throw new CocktailException("미터링 리스트 조회의 파라메터가 잘못 되었습니다.", ExceptionType.InvalidParameter);
            }
        }
    }

}
