package run.acloud.api.configuration.service;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import run.acloud.api.configuration.dao.IAccountMapper;
import run.acloud.api.configuration.enums.GradeApplyState;
import run.acloud.api.configuration.enums.ServiceType;
import run.acloud.api.configuration.vo.AccountGradeVO;
import run.acloud.api.configuration.vo.AccountMeteringVO;
import run.acloud.api.configuration.vo.AccountVO;
import run.acloud.api.configuration.vo.ServiceDetailVO;
import run.acloud.api.cserver.dao.IServiceMapper;
import run.acloud.commons.util.Utils;
import run.acloud.framework.context.ContextHolder;
import run.acloud.framework.properties.CocktailServiceProperties;

import java.util.List;

@Slf4j
@Service
public class AccountGradeService {
	@Resource(name = "cocktailSession")
    private SqlSessionTemplate sqlSession;

    @Autowired
    private CocktailServiceProperties cocktailServiceProperties;



    /**
     * 계정 등급정보 등록<br/>
     * <br/>
     * 파라메터 등급정보와 DB등급 정보와 비교하여 파라메터가 변경되었으면 파라메터 값을 사용하고,<br/>
     * 변경되지 않았으면 조회된 DB등급정보로 계정 등급정보 설정.<br/>
     *
     * @author coolingi
     * @since 2019-04-25
     * @param account
     * @return
     * @throws Exception
     */
    public void addAccountGrade(AccountVO account) throws Exception {
        IAccountMapper dao = sqlSession.getMapper(IAccountMapper.class);

        AccountGradeVO accountGradeVO = account.getAccountGrade(); // Grade Plan 정보 조회

        // account 정보
        if (accountGradeVO != null){
            // default 값 셋팅
            accountGradeVO.setAccountSeq(account.getAccountSeq());
            accountGradeVO.setUseYn("Y");
            accountGradeVO.setApplyState(GradeApplyState.APPLY);

            // 시작일 없을시 시작 날짜 셋팅
            if(accountGradeVO.getApplyStartDate() == null) {
                DateTime jodaTime = new DateTime();
                String today = jodaTime.toString("yyyy-MM-dd");
                accountGradeVO.setApplyStartDate(today);
            }

            // 만료일이 null이 아닌 빈값이면 null로 변경 처리
            if (StringUtils.isBlank(accountGradeVO.getApplyEndDate())) {
                accountGradeVO.setApplyEndDate(null);
            }

            dao.addAccountGrade(accountGradeVO);
        }

    }

    /**
     * 계정 등급정보 수정<br/>
     * <br/>
     * 계정 grade plan 정보 수정.<br/>
     * 화면에서는 GradeSeq 값만 셋팅함.<br/>
     * case 가 3가지<br/>
     *    1. 기존값 없고 새로등록 했을경우 : 등록 처리<br/>
     *    2. 기존값 있는데, 등록 안했을경우 : 삭제 처리<br/>
     *    3. 기존값 있는데, 다른 등급으로 수정 했을 경우 OR 값을 바껏을 경우 : 기존값 삭제 & 등록 처리<br/>
     *<br/>
     *
     * @author coolingi
     * @since 2019-05-29
     * @param account
     * @return
     * @throws Exception
     */
    public void editAccountGrade(AccountVO account) throws Exception {
        IAccountMapper dao = sqlSession.getMapper(IAccountMapper.class);

        AccountGradeVO editAccountGradeVO = account.getAccountGrade();
        if(editAccountGradeVO != null){
            editAccountGradeVO.setAccountSeq(account.getAccountSeq());
        }
        // 이전 적용상태나 미납상태의 Grade Plan 정보 조회
        AccountGradeVO prevAccountGradeVO = dao.getAccountGrade(null, account.getAccountSeq(), GradeApplyState.APPLY);
        if(prevAccountGradeVO == null){
            prevAccountGradeVO = dao.getAccountGrade(null, account.getAccountSeq(), GradeApplyState.UNPAID);
        }

        if (prevAccountGradeVO == null && editAccountGradeVO != null){          // 1. 기존값 없고 새로등록 했을경우 : 등록 처리
            addAccountGrade(account);

        } else if ( prevAccountGradeVO != null && editAccountGradeVO == null ){ // 2. 기존값 있는데, 등록 안했을경우 : 삭제 처리, 이런 경우는 없을듯
            removeAccountGrade(prevAccountGradeVO.getAccountGradeSeq());

        } else if ( prevAccountGradeVO != null && editAccountGradeVO != null ){ // 3. 기존값 있는데, 다른 등급으로 수정 했을 경우 : 기존값 삭제 & 등록 처리
            // 기존 등록된 gradeSeq 와 새로 등록된 gradeSeq가 다를 경우에만 다시 삭제 후 등록
            if( !prevAccountGradeVO.getGradeSeq().equals(editAccountGradeVO.getGradeSeq())  // 등급을 바꿨을 경우
                    || !editAccountGradeVO.equals(prevAccountGradeVO)                       // 값을 바꿨을 경우
            ){
                // 기존정보 삭제
                removeAccountGrade(prevAccountGradeVO.getAccountGradeSeq());

                // 기본값 셋팅 필요
                if(editAccountGradeVO.getUseYn() == null) editAccountGradeVO.setUseYn("Y"); // 사용유무
                if(editAccountGradeVO.getApplyStartDate() == null) {    //
                    // 시작일 없을시 수정전 데이터의 시작 날짜 셋팅
                    editAccountGradeVO.setApplyStartDate(prevAccountGradeVO.getApplyStartDate());
                }
                // 만료일이 null이 아닌 빈값이면 null로 변경 처리
                if (StringUtils.isBlank(editAccountGradeVO.getApplyEndDate())) {
                    editAccountGradeVO.setApplyEndDate(null);
                }

                // 수정된 정보 새로 등록
                dao.addAccountGrade(editAccountGradeVO);
            }
        }
    }

    /**
     * 계정 등급정보 삭제
     *
     * @author coolingi
     * @since 2019-04-22
     * @param accountGradeSeq
     * @return
     * @throws Exception
     */
    public int removeAccountGrade(Integer accountGradeSeq) throws Exception {
        int cnt = 0;
        IAccountMapper dao = sqlSession.getMapper(IAccountMapper.class);

        if(accountGradeSeq != null){
            AccountGradeVO accountGradeVO = new AccountGradeVO();
            accountGradeVO.setAccountGradeSeq(accountGradeSeq);
            // Grade Plan 정보 삭제
            cnt = dao.removeAccountGrade(accountGradeVO);
        }
        return cnt;
    }

    /**
     * 플랫폼 청구서 관련 삭제
     *
     * @param accountSeq
     * @return
     * @throws Exception
     */
    public int deleteAccountBillInfo(Integer accountSeq) throws Exception {
        int result = 0;
        IAccountMapper dao = sqlSession.getMapper(IAccountMapper.class);

        if(accountSeq != null){
            String baseLogFormat = String.format("############################### DELETE_ACCOUNT_BILL ##### - account: [%d], updater: [%d, %s]", accountSeq, ContextHolder.exeContext().getUserSeq(), ContextHolder.exeContext().getUserRole());

            result = dao.deleteAccountHourMetering(accountSeq);
            log.info("{}, {}: {}", baseLogFormat, "deleteAccountHourMetering", result);

            result = dao.deleteAccountBillDailyCharge(accountSeq);
            log.info("{}, {}: {}", baseLogFormat, "deleteAccountBillDailyCharge", result);

            result = dao.deleteAccountBillPrd(accountSeq);
            log.info("{}, {}: {}", baseLogFormat, "deleteAccountBillPrd", result);

            result = dao.deleteAccountBillExcharge(accountSeq);
            log.info("{}, {}: {}", baseLogFormat, "deleteAccountBillExcharge", result);

            result = dao.deleteAccountBill(accountSeq);
            log.info("{}, {}: {}", baseLogFormat, "deleteAccountBill", result);

            result = dao.deleteAccountGrade(accountSeq);
            log.info("{}, {}: {}", baseLogFormat, "deleteAccountGrade", result);
        }
        return result;
    }

    /**
     * AccountGrade 조회.<br/>
     * <br/>
     * accountGradeSeq, accountSeq, applyState 세개의 값만 사용함.
     *
     * @param accountGrade
     * @return
     * @throws Exception
     */
    public AccountGradeVO getAccountGrade(AccountGradeVO accountGrade) throws Exception {
        IAccountMapper dao = sqlSession.getMapper(IAccountMapper.class);

        // get One
        AccountGradeVO accountGradeVO = dao.getAccountGrade(accountGrade.getAccountGradeSeq(), accountGrade.getAccountSeq(), accountGrade.getApplyState());
        return accountGradeVO;
    }

    /**
     * AccountGradeList 조회.
     *
     * @param accountGrade
     * @return
     * @throws Exception
     */
    public List<AccountGradeVO> getAccountGrades(AccountGradeVO accountGrade) throws Exception {
        IAccountMapper dao = sqlSession.getMapper(IAccountMapper.class);

        return this.getAccountGrades(accountGrade, "Y");
    }

    public List<AccountGradeVO> getAccountGrades(AccountGradeVO accountGrade, String useYn) throws Exception {
        IAccountMapper dao = sqlSession.getMapper(IAccountMapper.class);

        return dao.getAccountGrades(accountGrade.getAccountSeq(), accountGrade.getApplyState(), Utils.getUseYn(useYn));
    }

    /**
     * AccountGradeList 조회.<br/>
     * accountSeq List 값에 의해 조회됨. <br/>
     *
     * @param accountSeqs account sequence list
     * @return
     * @throws Exception
     */
    public List<AccountGradeVO> getAccountGradesByAccounts(List<Integer> accountSeqs) throws Exception {
        IAccountMapper dao = sqlSession.getMapper(IAccountMapper.class);

        List<AccountGradeVO> accountGrades = dao.getAccountGradesByAccounts(accountSeqs);
        return accountGrades;
    }

    /**
     * 사용유무에 상관없이 Account에 적용된 최종 등급정보 조회<br/>
     * 혹시 시스템 삭제후 청구서 정보 생성할 경우가 있을 수도 있어서 생성함.
     *
     * @param accountSeq
     * @return
     * @throws Exception
     */
    public AccountGradeVO getLastAccountGrade(Integer accountSeq) throws Exception {
        IAccountMapper dao = sqlSession.getMapper(IAccountMapper.class);
        return dao.getLastAccountGrade(accountSeq);
    }


    /**
     * 계정별 시간별 미터링 정보 테이블 데이터 저장
     *
     * @param accountMetering
     * @throws Exception
     */
    @Transactional(transactionManager = "transactionManager")
    public void addAccountHourMetering(AccountMeteringVO accountMetering) throws Exception {
        IAccountMapper dao = sqlSession.getMapper(IAccountMapper.class);
        dao.addAccountHourMetering(accountMetering);
    }


    /**
     * 계정별 시간당 미터링 정보를 검색하는 메서드
     *
     * @param accountSeq
     * @param searchStartDt
     * @param searchEndDt
     * @param baseDate
     * @param startCount
     * @param pageCount
     * @return
     * @throws Exception
     */
    public List<AccountMeteringVO> getAccountHourMeterings(Integer accountSeq, String searchStartDt, String searchEndDt, String baseDate, String startCount, String pageCount) throws Exception {
        IAccountMapper dao = sqlSession.getMapper(IAccountMapper.class);
        return dao.getAccountHourMeterings(accountSeq, searchStartDt, searchEndDt, baseDate, startCount, pageCount, cocktailServiceProperties.getRegionTimeZone());
    }

    /**
     * 시간별 metering 정보에서 기준일에 account_seq list를 구하는 메서드
     *
     * @param baseDate
     * @return
     * @throws Exception
     */
    public List<Integer> getAccountSeqListInHourMeteringByBaseDate(String baseDate) throws Exception {
        IAccountMapper dao = sqlSession.getMapper(IAccountMapper.class);
        return dao.getAccountSeqListInHourMeteringByBaseDate(baseDate, cocktailServiceProperties.getRegionTimeZone());
    }

    /**
     * 계정에 workspace 등록 가능한지 체크
     *
     * @param accountSeq
     * @return
     * @throws Exception
     */
    public boolean isPossibleRegisterWorkspace(Integer accountSeq) throws Exception {
        boolean isPossibleRegisterWorkspace = true;

        // account 의 현재 적용된 grade plan 정보 조회
        AccountGradeVO accountGradeVO = new AccountGradeVO();
        accountGradeVO.setAccountSeq(accountSeq);
        accountGradeVO.setApplyState(GradeApplyState.APPLY);

        accountGradeVO = getAccountGrade(accountGradeVO);

        // gradePlan 정보가 등록되어 있을때만 체크, 아니면 모두 true
        if (accountGradeVO != null){
            // Account의 서비스 조회
            IServiceMapper dao = sqlSession.getMapper(IServiceMapper.class);
            List<ServiceDetailVO> serviceVOList = dao.getServiceOfAccount(accountSeq, null);


            if (serviceVOList != null) {

                int maxWorkspaceCount = accountGradeVO.getWorkspaceCnt();   // 설정된 등록가능한 workspace 갯수
                long currentWorkspaceCnt = serviceVOList.stream().filter(vo -> vo.getServiceType() != ServiceType.PLATFORM).count();             // 현재 등록되어 있는 workspace 갯수

                // maxWorkspaceCount가 0보다 클때만 체크
                if ( maxWorkspaceCount > 0 && currentWorkspaceCnt >= maxWorkspaceCount ) { // 현재 갯수가 max 값 보다 크거나 같으면 등록 불가
                    isPossibleRegisterWorkspace = false;
                }
            }
        }

        return isPossibleRegisterWorkspace;
    }

}
