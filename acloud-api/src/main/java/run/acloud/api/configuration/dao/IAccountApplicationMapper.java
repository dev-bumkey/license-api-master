package run.acloud.api.configuration.dao;

import org.apache.ibatis.annotations.Param;
import run.acloud.api.configuration.vo.AccountApplicationSearchVO;
import run.acloud.api.configuration.vo.AccountApplicationVO;
import run.acloud.commons.vo.ListCountVO;

import java.util.List;

@Deprecated
public interface IAccountApplicationMapper {

    // 신청 페이지
    // 등록
    int insertAccountApplication(AccountApplicationVO accountApplication);

    // 신청 페이지 -> accountCode, userEmail 사용
    AccountApplicationVO getDetailByUser(
        @Param("accountCode") String accountCode,
        @Param("userEmail") String userEmail,
        @Param("defaultTimezone") String defaultTimezone
    );

    Integer getAccountCodeCount(
        @Param("accountCode") String accountCode
    );

    // 관리 페이지
    // 조회
    List<AccountApplicationVO> getAccountApplications(
        AccountApplicationSearchVO params
    );

    ListCountVO getAccountApplicationCountAndMaxId(
        AccountApplicationSearchVO params
    );

    // 관리 페이지 -> accountApplicationSeq 사용
    AccountApplicationVO getDetailByAdmin(
        @Param("accountApplicationSeq") Integer accountApplicationSeq,
        @Param("defaultTimezone") String defaultTimezone
    );

    // 상태수정
    int updateAccountApplicationStatus(
        @Param("accountApplicationSeq") Integer accountApplicationSeq,
        @Param("status") String status,
        @Param("updater") Integer updater
    );

    // 삭제
    int deleteAccountApplication(
        @Param("accountApplicationSeq") Integer accountApplicationSeq
    );

}
