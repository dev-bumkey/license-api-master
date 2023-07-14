package run.acloud.api.billing.service;


import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import run.acloud.api.billing.dao.IAccountBillMapper;
import run.acloud.api.billing.enums.BillState;
import run.acloud.api.billing.vo.AccountBillDailyChargeVO;
import run.acloud.api.billing.vo.AccountBillExchangeVO;
import run.acloud.api.billing.vo.AccountBillProductVO;
import run.acloud.api.billing.vo.AccountBillVO;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class AccountBillService {

    @Resource(name = "cocktailSession")
    private SqlSessionTemplate sqlSession;

    @Transactional(transactionManager = "transactionManager")
    public AccountBillVO addAccountBill(AccountBillVO accountBillVO){

        IAccountBillMapper billDao = sqlSession.getMapper(IAccountBillMapper.class);

        // default 값 셋팅
        if(accountBillVO.getBillState() == null){
            accountBillVO.setBillState(BillState.CREATED);
        }
        accountBillVO.setUseYn("Y");

        // accountBill 등록
        int result = billDao.addAccountBill(accountBillVO);

        // accountBillExchange 등록
        if(CollectionUtils.isNotEmpty(accountBillVO.getBillExchanges())) {
            billDao.addAccountBillExchanges(accountBillVO);
        }

        // accountBillProduct 등록
        billDao.addAccountBillProducts(accountBillVO);

        log.debug("insert result : "+result);

        return accountBillVO;
    }

    @Transactional(transactionManager = "transactionManager")
    public int addAccountBillList(List<AccountBillVO> accountBillVOList){
        IAccountBillMapper billDao = sqlSession.getMapper(IAccountBillMapper.class);
        int result = 0;

        for(AccountBillVO accountBillVO : accountBillVOList) {
            this.addAccountBill(accountBillVO);
            result++;
        }

        log.debug("insert result : "+result);
        return result;
    }

    @Transactional(transactionManager = "transactionManager")
    public AccountBillVO updateAccountBill(AccountBillVO accountBillVO){

        IAccountBillMapper billDao = sqlSession.getMapper(IAccountBillMapper.class);
        // 현재 정보 조회
        AccountBillVO currentBillVO = this.getAccountBillDetail(accountBillVO.getBillSeq());

        // 변경사항이 있을때 데이터 수정, 하위 상품들 비교는 제외
        if(!currentBillVO.equals(accountBillVO)) {
            // accountBill 수정
            billDao.updateAccountBill(accountBillVO);
        }

        // 환율 정보 수정
        List<AccountBillExchangeVO> accountBillExchanges = accountBillVO.getBillExchanges();
        List<AccountBillExchangeVO> currBillExchanges = currentBillVO.getBillExchanges();

        if(accountBillExchanges != null && !accountBillExchanges.equals(currBillExchanges)){
            billDao.removeAccountBillExchanges(accountBillVO); // accountBillExchange 삭제
            billDao.addAccountBillExchanges(accountBillVO); // accountBillExchange 등록

        } else if(accountBillExchanges == null && currBillExchanges != null){  // accountBillExchange 가 null 이거나 두 값이 같을때만 이 조건문 실행
            billDao.removeAccountBillExchanges(accountBillVO); // accountBillExchange 삭제
        }

        // 청구서 상품정보 수정
        List<AccountBillProductVO> accountBillProducts = accountBillVO.getBillProducts(); // 입력된 청구서 상품정보
        List<AccountBillProductVO> currBillProducts = currentBillVO.getBillProducts(); // 현재 저장되어 있는 청구서 상품정보

        // accountBillProduct 수정된 사항이 있을때만 수정
        if(!currBillProducts.equals(accountBillProducts)){
            billDao.removeAccountBillProducts(accountBillVO); // accountBillProduct 삭제
            billDao.addAccountBillProducts(accountBillVO); // accountBillProduct 등록
        }

        return accountBillVO;
    }

    @Transactional(transactionManager = "transactionManager")
    public int removeAccountBill(AccountBillVO accountBillVO){
        int result = 0;

        IAccountBillMapper billDao = sqlSession.getMapper(IAccountBillMapper.class);

        // accountBillProduct 삭제
        billDao.removeAccountBillProducts(accountBillVO);

        // accountBillExchange 삭제
        billDao.removeAccountBillExchanges(accountBillVO);

        // accountBill 삭제
        billDao.removeAccountBill(accountBillVO);

        return result;
    }

    public AccountBillVO getAccountBillDetail(Integer billSeq){

        IAccountBillMapper billDao = sqlSession.getMapper(IAccountBillMapper.class);

        // accountBill 조회
        AccountBillVO accountBillVO = billDao.getAccountBill(billSeq);

        // accountBillExchange 조회
        List<AccountBillExchangeVO> billExchanges = billDao.getAccountBillExchanges(billSeq);
        accountBillVO.setBillExchanges(billExchanges);  // 조회된 bill exchanges 셋팅

        // accountBillProduct 조회
        List<AccountBillProductVO> billProducts = billDao.getAccountBillProducts(billSeq);
        accountBillVO.setBillProducts(billProducts);    // 조회된 bill products 셋팅

        return accountBillVO;
    }

    /**
     * 청구서 리스트 조회하는 메서드.<br/>
     * AccountBillVO 클래스의 accountSeq, orgName, usedMonth, billState 이렇게 4개의 값을 사용함.<br/>
     * accountSeq 가 존재할 경우는 orgName는 사용하지 않고, accountSeq 없고 orgName 가 있는 경우에는 orgName로 like 검색하여 조회할 accountSeq리스트를 구한다.<br/>
     * 모든 조건이 and 조건임 <br/>
     * <br/>
     * accountSeq : 계정번호<br/>
     * organizationName : 조회 조직명<br/>
     * useMonth : 사용월<br/>
     * billState : 청구서 상태<br/>
     *
     * @author coolingi
     * @since 2019/05/28
     * @param accountBillVO
     * @return
     */
    public List<AccountBillVO> getAccountBills(AccountBillVO accountBillVO){
        List<AccountBillVO > results=null;

        IAccountBillMapper billDao = sqlSession.getMapper(IAccountBillMapper.class);

        Integer accountSeq = accountBillVO.getAccountSeq();
        String organizationName = accountBillVO.getOrganizationName();
        String usedMonth = accountBillVO.getUsedMonth();
        BillState billState = accountBillVO.getBillState();

        List<Integer> accountSeqs = null;

        // 조회 account list 셋팅
        if(accountSeq != null && accountSeq.intValue() > 0){ // 계정번호가 있을경우
            accountSeqs = new ArrayList<>();
            accountSeqs.add(accountSeq);
        }else if(StringUtils.isNotEmpty(organizationName)){ // 계정번호가 없고 조직명으로 검색했을 경우 조직명으로 검색해서 accountSeq 추출
            accountSeqs = billDao.getAccountSeqByOrganizationName(organizationName);

            // orgName가 있는데 조회되는 계정정보가 없으면 null 리턴
            if(CollectionUtils.isEmpty(accountSeqs)){
                return results;
            }
        }

        // accountBill 조회
        results = billDao.getAccountBills(accountSeqs, usedMonth, billState);

        return results;
    }

    @Transactional(transactionManager = "transactionManager")
    public void addAccountBillDailyCharge(AccountBillDailyChargeVO accountBillDailyCharge){
        IAccountBillMapper billDao = sqlSession.getMapper(IAccountBillMapper.class);
        billDao.addAccountBillDailyCharge(accountBillDailyCharge);
    }

    @Transactional(transactionManager = "transactionManager")
    public void addAccountBillDailyChargeList(List<AccountBillDailyChargeVO> accountBillDailyChargeList){
        IAccountBillMapper billDao = sqlSession.getMapper(IAccountBillMapper.class);
        billDao.addAccountBillDailyChargeList(accountBillDailyChargeList);
    }

    public List<AccountBillDailyChargeVO> getAccountBillDailyCharges(Integer accountSeq, String startDate, String endDate){
        IAccountBillMapper billDao = sqlSession.getMapper(IAccountBillMapper.class);
        return billDao.getAccountBillDailyCharges(accountSeq, startDate, endDate);
    }

    public List<AccountBillDailyChargeVO> getSumDailyCharges(Integer accountSeq, String baseDate){
        IAccountBillMapper billDao = sqlSession.getMapper(IAccountBillMapper.class);
        return billDao.getSumDailyCharges(accountSeq, baseDate);
    }


}
