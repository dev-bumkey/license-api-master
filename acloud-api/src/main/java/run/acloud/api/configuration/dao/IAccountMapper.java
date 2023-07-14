package run.acloud.api.configuration.dao;

import org.apache.ibatis.annotations.Param;
import run.acloud.api.auth.vo.UserVO;
import run.acloud.api.configuration.enums.GradeApplyState;
import run.acloud.api.configuration.vo.*;

import java.util.List;
import java.util.Map;

public interface IAccountMapper {

    List<AccountVO> getAccounts(Map<String, Object> params) throws Exception;

    AccountVO getAccount(@Param("accountSeq") Integer accountSeq) throws Exception;

    AccountVO getAccountByService(@Param("serviceSeq") Integer serviceSeq);

    AccountVO getAccountByUser(
        @Param("loginUserSeq") Integer loginUserSeq,
        @Param("loginUserRole") String loginUserRole
    ) throws Exception;

    AccountVO getAccountSimple(@Param("accountSeq") Integer accountSeq) throws Exception;

    AccountVO getAccountSimpleByCode(
        @Param("accountCode") String accountCode,
        @Param("useYn") String useYn
    );

    AccountVO getAccountDetailInfo(@Param("accountSeq") Integer accountSeq) throws Exception;

    AccountVO getAccountInfoForRef(@Param("accountSeq") Integer accountSeq) throws Exception;
    List<UserVO> getAccountSystemUsersForRef(@Param("accountSeq") Integer accountSeq) throws Exception;
    List<UserVO> getAccountUsersForRef(@Param("accountSeq") Integer accountSeq) throws Exception;
    List<ProviderAccountVO> getProviderAccountsOfAccountForRef(@Param("accountSeq") Integer accountSeq) throws Exception;

    int addAccount(AccountVO account) throws Exception;

    int addProviderOfAccount(
            @Param("accountSeq") Integer accountSeq,
            @Param("providerAccountSeqs") List<Integer> providerAccountSeqs,
            @Param("creator") Integer creator) throws Exception;

    int addSystemUserOfAccount(
            @Param("accountSeq") Integer accountSeq,
            @Param("accountUserSeqs") List<Integer> accountUserSeqs,
            @Param("creator") Integer creator) throws Exception;

    int deleteUserOfAccount(
            @Param("userSeq") Integer userSeq) throws Exception;

    int addUserOfAccount(
            @Param("accountSeq") Integer accountSeq,
            @Param("userSeqs") List<Integer> userSeqs,
            @Param("creator") Integer creator) throws Exception;

    int editAccount(AccountVO account) throws Exception;
    int editAccountRegistryPullUser(AccountVO account);
    int editAccountInfoForAD(AccountVO account);

    int removeAccount(AccountVO account) throws Exception;
    int deleteAccount(@Param("accountSeq") Integer accountSeq) throws Exception;

    int removeProviderOfAccount(
            @Param("accountSeq") Integer accountSeq,
            @Param("providerSeqs") List<Integer> providerSeqs) throws Exception;

    int removeAccountProvider(Integer accountSeq);
    int deleteProviderAccount(@Param("providerAccountSeq") Integer providerAccountSeq);

    int removeUserOfAccount(
            @Param("accountSeq") Integer accountSeq,
            @Param("userSeqs") List<Integer> userSeqs) throws Exception;

    int removeAccountUser(Integer accountSeq) throws Exception;

    int removeSystemUserOfAccount(
            @Param("accountSeq") Integer accountSeq,
            @Param("accountUserSeqs") List<Integer> accountUserSeqs) throws Exception;

    int removeAccountSystemUser(Integer accountSeq) throws Exception;


    int getAccountCountByName(@Param("accountName") String accountName) throws Exception;
    int getAccountCountByCode(@Param("accountCode") String accountCode) throws Exception;

    int getProviderAccountCountByAccount(Integer accountSeq) throws Exception;


    // AccountGrade Querys
    int addAccountGrade(AccountGradeVO accountGrade) throws Exception;
    int removeAccountGrade(AccountGradeVO accountGrade) throws Exception;

    AccountGradeVO getAccountGrade( @Param("accountGradeSeq") Integer accountGradeSeq, @Param("accountSeq") Integer accountSeq, @Param("applyState") GradeApplyState applyState ) throws Exception;
    List<AccountGradeVO> getAccountGrades( @Param("accountSeq") Integer accountSeq, @Param("applyState") GradeApplyState applyState, @Param("useYn") String useYn ) throws Exception;
    List<AccountGradeVO> getAccountGradesByAccounts( @Param("accountSeqs") List<Integer> accountSeqs) throws Exception;
    AccountGradeVO getLastAccountGrade(@Param("accountSeq") Integer accountSeq) throws Exception;

    // Account metering
    int addAccountHourMetering(AccountMeteringVO accountMetering) throws Exception;
    int removeAccountHourMetering(AccountMeteringVO accountMetering) throws Exception;
    int deleteAccountHourMetering(@Param("accountSeq") Integer accountSeq) throws Exception;
    int deleteAccountBillDailyCharge(@Param("accountSeq") Integer accountSeq) throws Exception;
    int deleteAccountBillPrd(@Param("accountSeq") Integer accountSeq) throws Exception;
    int deleteAccountBillExcharge(@Param("accountSeq") Integer accountSeq) throws Exception;
    int deleteAccountBill(@Param("accountSeq") Integer accountSeq) throws Exception;
    int deleteAccountGrade(@Param("accountSeq") Integer accountSeq) throws Exception;

    List<AccountMeteringVO> getAccountHourMeterings(@Param("accountSeq") Integer accountSeq,
                                                    @Param("searchStartDt") String searchStartDt,
                                                    @Param("searchEndDt") String searchEndDt,
                                                    @Param("baseDate") String baseDate,
                                                    @Param("startCount") String startCount,
                                                    @Param("pageCount") String pageCount,
                                                    @Param("regionTimeZone") String regionTimeZone) throws Exception;

    List<Integer> getAccountSeqListInHourMeteringByBaseDate(@Param("baseDate") String baseDate, @Param("regionTimeZone") String regionTimeZone) throws Exception;
    List<AccountServiceVO> getAccountService() throws Exception;

    int getAccessibleResourcesCountWithoutWorkspace(Map<String, Object> params);
    int getAccessibleUserCountWithoutWorkspace(Map<String, Object> params);

    /** R4.1.1 : 2020.03.13 **/
    List<ClusterProviderVO> getClustersProviderOfAccount(@Param("accountSeq") Integer accountSeq,
                                                @Param("accountUseType") String accountUseType) throws Exception;

    List<ClusterVO> getLinkedClustersOfAccount(@Param("accountSeq") Integer accountSeq,
                                                       @Param("accountUseType") String accountUseType) throws Exception;

    int updateClustersCloudProviderAccount(@Param("clusterSeqs") List<Integer> clusterSeqs,
                                           @Param("providerAccountSeq") Integer providerAccountSeq,
                                           @Param("updater") Integer updater) throws Exception;

    ClusterVO getClusterSummary(
        @Param("clusterSeq") Integer clusterSeq
    );
}
