package run.acloud.api.auth.dao;

import org.apache.ibatis.annotations.Param;
import run.acloud.api.auth.vo.AuthVO;
import run.acloud.api.auth.vo.UserExternalVO;
import run.acloud.api.auth.vo.UserOtpVO;
import run.acloud.api.auth.vo.UserVO;
import run.acloud.api.configuration.vo.*;
import run.acloud.commons.vo.ListCountVO;

import java.util.Date;
import java.util.List;

public interface IUserMapper {

    UserVO selectByUserSeq(@Param("userSeq") Integer userSeq,
                           @Param("useYn") String useYn
    );

    List<UserVO> selectByUserSeqs(@Param("userSeqs") List<Integer> userSeqs);

	UserVO selectByUserId(
			@Param("userId") String userId,
			@Param("userRole") String userRole,
			@Param("accountId") String accountId
	);

	UserVO getUser(
		@Param("userSeq") Integer userSeq
	);

	AuthVO getUserByIdAuth(@Param("userId") String userId);

	List<String> readRoles(@Param("userId") String userId);

	Integer updateLoginTimestamp(@Param("userId") String userId);
	Integer updateLoginTimestampBySeq(@Param("userSeq") Integer userSeq);
	Integer updateActiveTimestampBySeq(@Param("userSeq") Integer userSeq, @Param("updater") Integer updater);
	Integer updateLoginFailCountBySeq(@Param("loginFailCount") Integer loginFailCount, @Param("userSeq") Integer userSeq);

    List<UserVO> getUsers(UserVO params);
    List<UserVO> getUsersForCheck(UserVO params);
    List<UserExternalVO> getUsersForExternal(
			@Param("userSeq") Integer userSeq,
			@Param("userId") String userId,
			@Param("userRole") String userRole,
			@Param("accountSeq") Integer accountSeq,
			@Param("accountCode") String accountCode
	);
    List<UserVO> getUsersOfAccount(
			@Param("accountSeq") Integer accountSeq
	);
    UserVO getUserOfAccount(
			@Param("accountSeq") Integer accountSeq,
			@Param("userSeq") Integer userSeq
	);

	/**
	 * 사용자 아이디로 사용자 정보 조회 : Keycloak에서 사용을 위해 해시된 패스워드도 응답함에 유의 할것.
	 * @param accountSeq
	 * @param userId
	 * @return
	 */
	UserVO getUserOfAccountById(
		@Param("accountSeq") Integer accountSeq,
		@Param("userId") String userId
	);

	Integer addUser(UserVO params);
    List<UserVO> getAccountUsersForAccount(
			@Param("accountSeq") Integer accountSeq
	);

	Integer editUser(UserVO params);
    List<UserVO> getUsersForAccount(
			@Param("accountSeq") Integer accountSeq
	);

	Integer editUserLanguage(UserVO params) throws Exception;
    List<UserVO> getUsersForWorkspace(
			@Param("userRole") String userRole,
			@Param("userGrant") String userGrant,
			@Param("accountSeq") Integer accountSeq
	);

	Integer editUserTimezone(UserVO params) throws Exception;

	Integer removeUser(UserVO params);
	Integer deleteUser(
			@Param("userSeq") Integer userSeq
	);

	Integer removeUsers(
			@Param("userSeqs") List<Integer> userSeqs,
			@Param("updater") Integer updater
	);

	boolean checkPassword(UserVO params);

	Integer changeOnlyPassword(UserVO params);

	Integer changePassword(UserVO params);

	Integer resetPassword(UserVO params);

	Integer extendPassword(UserVO params);

	List<String> getUserRoles(UserVO params);

	Integer addUserRoles(UserVO params);

	Integer addUserRole(
			@Param("userSeq") Integer userSeq,
			@Param("roleCode") String roleCode
	);

	Integer removeUserRole(UserVO params);
	Integer deleteUserRole(
			@Param("userSeq") Integer userSeq
	);

	String getLastServiceBySeq(Integer userId);

	Integer updateLastServiceSeq(UserVO user);

	List<ServiceListVO> getAuthorizedServicesBySeq(
			@Param("userSeq") Integer userSeq
	);

	List<UserVO> getUserSeqByRole(
			@Param("userRole") String userRole
	);

	Date getCurrentDate();

	List<Integer> getAccountOfAccountUser(@Param("accountUserSeq") Integer accountUserSeq);

	List<Integer> getServiceOfUser(@Param("userSeq") Integer userSeq);

	List<Integer> getAccountOfUser(@Param("userSeq") Integer userSeq);

	List<Integer> selectSeqByUserRole(@Param("roleCode") String roleCode);

	List<UserVO> selectUsersByAccount(
			@Param("accountSeq") Integer accountSeq,
			@Param("useYn") String useYn
	);

	List<UserVO> selectSystemUsersByAccount(
		@Param("accountSeq") Integer accountSeq,
		@Param("useYn") String useYn
	);

	Integer rebirthUser(UserVO user);

	Integer updateUsersLastService(
			@Param("lastServiceSeq") Integer lastServiceSeq,
			@Param("userSeqs") List<Integer> userSeqs,
			@Param("updater") Integer updater
	);

	Integer updateDefaultUsersLastService(
		@Param("lastServiceSeq") Integer lastServiceSeq,
		@Param("userSeqs") List<Integer> userSeqs,
		@Param("updater") Integer updater
	);

	Integer addUserClusterRoleIssue(UserClusterRoleIssueVO userClusterRoleIssue);
	Integer addUserClusterRoleIssues(
			@Param("issues") List<UserClusterRoleIssueVO> userClusterRoleIssues
	);
	Integer addUserClusterRoleIssueBindings(
            @Param("userSeq") Integer userSeq,
			@Param("issueBindings") List<UserClusterRoleIssueBindingVO> userClusterRoleIssueBindings,
			@Param("creator") Integer creator
	);
	Integer moveUserClusterRoleIssueBindings(
			@Param("targetUserSeq") Integer targetUserSeq,
			@Param("userSeq") Integer userSeq,
			@Param("clusterSeq") Integer clusterSeq,
			@Param("issueType") String issueType,
			@Param("issueAccountName") String issueAccountName,
			@Param("updater") Integer updater
	);
	Integer deleteUserClusterRoleIssueBinding(
			@Param("userSeq") Integer userSeq,
			@Param("clusterSeq") Integer clusterSeq,
			@Param("issueType") String issueType,
			@Param("issueAccountName") String issueAccountName
	);
	List<UserClusterRoleIssueVO> getUserClusterRoleIssuesForAdd(
			@Param("accountSeq") Integer accountSeq,
			@Param("userSeq") Integer userSeq,
			@Param("issueType") String issueType,
			@Param("issues") List<UserClusterRoleIssueVO> userClusterRoleIssues,
			@Param("creator") Integer creator
	);
	UserClusterRoleIssueVO getUserClusterRoleIssue(
			@Param("userSeq") Integer userSeq,
			@Param("clusterSeq") Integer clusterSeq,
			@Param("issueType") String issueType,
			@Param("issueAccountName") String issueAccountName,
			@Param("userTimezone") String userTimezone
	);

	List<UserClusterRoleIssueVO> getUserClusterRoleIssues(
			@Param("accountSeq") Integer accountSeq,
			@Param("clusterSeq") Integer clusterSeq,
			@Param("clusterSeqs") List<Integer> clusterSeqs,
			@Param("userSeq") Integer userSeq,
			@Param("issueType") String issueType,
			@Param("expirationDatetime") String expirationDatetime,
			@Param("userTimezone") String userTimezone,
			@Param("isUserBased") Boolean isUserBased
	);
	List<UserClusterRoleIssueVO> getUserClusterRoleIssueList(UserClusterRoleIssueSearchVO params);
	ListCountVO getUserClusterRoleIssueCount(UserClusterRoleIssueSearchVO params);
	List<UserClusterRoleIssueVO> getUserClusterRoleIssuesForExcel(UserClusterRoleIssueSearchVO params) throws Exception;

	Integer removeUserClusterRoleIssue(UserClusterRoleIssueVO userClusterRoleIssue);
	Integer deleteUserClusterRoleIssue(UserClusterRoleIssueVO userClusterRoleIssue);
	Integer updateUserClusterRoleIssue(
			@Param("issue") UserClusterRoleIssueVO userClusterRoleIssue,
			@Param("creator") Integer creator
	);
	Integer updateUserClusterRoleIssueConfig(
			@Param("userSeq") Integer userSeq,
			@Param("clusterSeq") Integer clusterSeq,
			@Param("issueType") String issueType,
            @Param("issueAccountName") String issueAccountName,
			@Param("issueConfig") String issueConfig
	);
	Integer updateUserClusterRoleIssueForClusterApi(
			@Param("userSeq") Integer userSeq,
			@Param("clusterSeq") Integer clusterSeq,
			@Param("issueType") String issueType,
			@Param("issueShellPath") String issueShellPath
	);
	Integer moveUserClusterRoleIssue(
			@Param("targetUserSeq") Integer targetUserSeq,
			@Param("userSeq") Integer userSeq,
			@Param("clusterSeq") Integer clusterSeq,
			@Param("issueType") String issueType,
			@Param("issueAccountName") String issueAccountName,
			@Param("updater") Integer updater
	);
	Integer addUserClusterRoleIssueHistory(UserClusterRoleIssueHistoryVO userClusterRoleIssueHistory);
	Integer addUserClusterRoleIssueHistories(
			@Param("issues") List<UserClusterRoleIssueHistoryVO> userClusterRoleIssueHistories
	);
	Integer addUserClusterRoleIssueBindingHistories(
			@Param("historySeq") Integer historySeq,
			@Param("issueBindings") List<UserClusterRoleIssueBindingVO> userClusterRoleIssueBindings,
			@Param("creator") Integer creator
	);
	List<UserClusterRoleIssueHistoryVO> getUserClusterRoleIssueHistories(UserClusterRoleIssueSearchVO params) throws Exception;
	ListCountVO getUserClusterRoleIssueHistoriesCountAndMaxId(UserClusterRoleIssueSearchVO params) throws Exception;
    List<UserClusterRoleIssueHistoryVO> getUserClusterRoleIssueHistoriesForExcel(UserClusterRoleIssueSearchVO params) throws Exception;
    UserClusterRoleIssueHistoryVO getLatestUserClusterRoleIssueHistory(UserClusterRoleIssueSearchVO params) throws Exception;

	Integer addUserShellConnectHistory(
		@Param("shellHistories") List<UserShellConnectHistoryVO> userShellConnectHistories
	);
	List<UserShellConnectHistoryVO> getUserShellConnectHistories(UserClusterRoleIssueSearchVO params) throws Exception;
	ListCountVO getUserShellConnectHistoriesCountAndMaxId(UserClusterRoleIssueSearchVO params) throws Exception;
    List<UserShellConnectHistoryVO> getUserShellConnectHistoriesForExcel(UserClusterRoleIssueSearchVO params) throws Exception;

	Integer addUserConfigDownloadHistory(
		@Param("downloadHistories") List<UserConfigDownloadHistoryVO> userConfigDownloadHistories
	);
	List<UserConfigDownloadHistoryVO> getUserConfigDownloadHistories(UserClusterRoleIssueSearchVO params) throws Exception;
	ListCountVO getUserConfigDownloadHistoriesCountAndMaxId(UserClusterRoleIssueSearchVO params) throws Exception;
    List<UserConfigDownloadHistoryVO> getUserConfigDownloadHistoriesForExcel(UserClusterRoleIssueSearchVO params) throws Exception;

	List<UserVO> getUsersExistClusterRoleIssue(
			@Param("accountSeq") Integer accountSeq
	);

	UserOtpVO getUserOtpInfo(@Param("userSeq") Integer userSeq);
	Integer updateUserOtpInfo(
			@Param("userOtp") UserOtpVO userOtp,
			@Param("updater") Integer updater
	);

	Integer updateUserInactiveYn(
			@Param("userSeq") Integer userSeq,
			@Param("inactiveYn") String inactiveYn,
			@Param("updater") Integer updater
	);

	Integer deleteUserClusterRoleIssueBindingHistoryByCluster(@Param("clusterSeq") Integer clusterSeq);
	Integer deleteUserClusterRoleIssueHistoryByCluster(@Param("clusterSeq") Integer clusterSeq);
	Integer deleteUserConfigDownloadHistoryByCluster(@Param("clusterSeq") Integer clusterSeq);
	Integer deleteUserShellConnectHistoryByCluster(@Param("clusterSeq") Integer clusterSeq);
	Integer deleteUserClusterRoleIssueBindingByCluster(@Param("clusterSeq") Integer clusterSeq);
	Integer deleteUserClusterRoleIssueByCluster(@Param("clusterSeq") Integer clusterSeq);
}
