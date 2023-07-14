package run.acloud.api.billing.dao;

import org.apache.ibatis.annotations.Param;
import run.acloud.api.billing.enums.BillState;
import run.acloud.api.billing.vo.AccountBillDailyChargeVO;
import run.acloud.api.billing.vo.AccountBillExchangeVO;
import run.acloud.api.billing.vo.AccountBillProductVO;
import run.acloud.api.billing.vo.AccountBillVO;

import java.util.List;

public interface IAccountBillMapper {

    // for bill
    int addAccountBill(AccountBillVO accountBill);
    int updateAccountBill(AccountBillVO accountBill);
    int removeAccountBill(AccountBillVO accountBill);
    AccountBillVO getAccountBill(Integer billSeq);
    List<AccountBillVO> getAccountBills(@Param("accountSeqs") List<Integer> accountSeqs,@Param("usedMonth") String usedMonth,@Param("billState") BillState billState);
    List<Integer> getAccountSeqByOrganizationName(String orgName);

    // service and product infos for bill
    void addAccountBillProducts(AccountBillVO accountBill);
    void removeAccountBillProducts(AccountBillVO accountBill);
    List<AccountBillProductVO> getAccountBillProducts(Integer billSeq);

    // exchange infos for bill
    void addAccountBillExchanges(AccountBillVO accountBill);
    void removeAccountBillExchanges(AccountBillVO accountBill);
    List<AccountBillExchangeVO> getAccountBillExchanges(Integer billSeq);

    // 월별 청구서 생성을 위한 일별 과금 summary 테이블
    void addAccountBillDailyCharge(AccountBillDailyChargeVO accountBillDailyCharge);
    void addAccountBillDailyChargeList(List<AccountBillDailyChargeVO> accountBillDailyChargeList);
    List<AccountBillDailyChargeVO> getAccountBillDailyCharges(@Param("accountSeq") Integer accountSeq, @Param("startDate") String startDate, @Param("endDate") String endDate);
    List<AccountBillDailyChargeVO> getSumDailyCharges(@Param("accountSeq") Integer accountSeq, @Param("baseDate") String baseDate);

}
