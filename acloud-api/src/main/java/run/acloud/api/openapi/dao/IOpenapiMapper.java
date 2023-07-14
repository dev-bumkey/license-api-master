package run.acloud.api.openapi.dao;

import org.apache.ibatis.annotations.Param;
import run.acloud.api.openapi.vo.*;
import run.acloud.commons.vo.ListCountVO;

import java.util.List;

public interface IOpenapiMapper {

    /** 조회 **/
    List<ApiGatewayGroupsVO> getApiGatewayGroups();
    List<ApiGatewayGroupsVO> getApiGatewayGroupsWithApi();
    List<ApiGatewaysVO> getApiGateways(
            @Param("apiSeqs") List<Integer> apiSeqs
    );
    List<ApiTokenIssueVO> getApiTokenIssuesList(ApiTokenIssueSearchVO params);
    ListCountVO getApiTokenIssuesCount(ApiTokenIssueSearchVO params);
    List<ApiTokenIssueExcelVO> getApiTokenIssuesForExcel(ApiTokenIssueSearchVO params);
    List<ApiTokenIssueVO> getApiTokenIssues(
            @Param("accountSeq") Integer accountSeq,
            @Param("systemUserSeq") Integer systemUserSeq,
            @Param("userTimezone") String userTimezone,
            @Param("withApi") boolean withApi,
            @Param("expirationDatetime") String expirationDatetime
    );
    List<ApiTokenIssueRelationVO> getApiTokenIssuesRelation(
            @Param("accountSeq") Integer accountSeq
    );
    ApiTokenIssueDetailVO getApiTokenIssue(
            @Param("accountSeq") Integer accountSeq,
            @Param("apiTokenIssueSeq") Integer apiTokenIssueSeq,
            @Param("withToken") boolean withToken,
            @Param("systemUserSeq") Integer systemUserSeq,
            @Param("userTimezone") String userTimezone
    );
    List<ApiTokenIssueDetailVO> getApiTokens(
            @Param("accountSeq") Integer accountSeq
    );
    List<ApiTokenIssueVO> getApiTokenNames(
            @Param("accountSeq") Integer accountSeq,
            @Param("apiTokenName") String apiTokenName,
            @Param("excludeApiTokenIssueSeq") Integer excludeApiTokenIssueSeq
    );
    List<ApiTokenIssueHistoryVO> getApiTokenIssuesHistoryList(ApiTokenIssueSearchVO params);
    ListCountVO getApiTokenIssuesHistoryCount(ApiTokenIssueSearchVO params);
    List<ApiTokenIssueHistoryExcelVO> getApiTokenIssuesHistoryForExcel(ApiTokenIssueSearchVO params);
    List<ApiTokenAuditLogVO> getApiTokenAuditLogList(ApiTokenIssueSearchVO params);
    ListCountVO getApiTokenAuditLogCount(ApiTokenIssueSearchVO params);
    List<ApiTokenAuditLogVO> getApiTokenAuditLogForExcel(ApiTokenIssueSearchVO params);
    int getApiTokenAuditLogCountForBatch(@Param("baseDate") String baseDate);
    ApiTokenRequestCountVO getApiTokenRequestCount(
            @Param("apiTokenIssueSeq") Integer apiTokenIssueSeq,
            @Param("accountSeq") Integer accountSeq
    );

    /** 등록 **/
    int addApiTokenIssue(ApiTokenIssueAddVO apiTokenIssueAdd);
    int addApiTokenPermissionsScopes(
            @Param("apiTokenIssueSeq") Integer apiTokenIssueSeq,
            @Param("permissionsScopes") List<Integer> permissionsScopes,
            @Param("creator") Integer creator
    );
    int addApiTokenIssueHistory(ApiTokenIssueHistoryVO apiTokenIssueHistory);
    int addApiTokenPermissionsScopeHistory(ApiTokenPermissionsScopeHistoryVO apiTokenPermissionsScopeHistory);
    int addApiTokenAuditLog(ApiTokenAuditLogAddVO apiTokenAuditLogAdd);

    /** 수정 **/
    int editApiTokenIssue(ApiTokenIssueEditVO apiTokenIssueEdit);
    int editRequestCountByToken(
            @Param("apiTokenIssueSeq") Integer apiTokenIssueSeq,
            @Param("accountSeq") Integer accountSeq
    );

    /** 삭제 **/
    int deleteApiTokenIssue(
            @Param("apiTokenIssueSeq") Integer apiTokenIssueSeq,
            @Param("accountSeq") Integer accountSeq
    );
    int deleteApiTokenPermissionsScopes(
            @Param("apiTokenIssueSeq") Integer apiTokenIssueSeq
    );
    int deleteApiTokenAuditLogForBatch(@Param("baseDate") String baseDate);
}
