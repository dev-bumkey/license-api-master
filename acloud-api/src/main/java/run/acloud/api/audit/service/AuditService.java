package run.acloud.api.audit.service;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.stereotype.Service;
import run.acloud.api.audit.dao.IAuditLogMapper;
import run.acloud.api.audit.vo.AuditLogListVO;
import run.acloud.api.audit.vo.AuditLogVO;
import run.acloud.api.auth.enums.UserRole;
import run.acloud.commons.util.AuthUtils;
import run.acloud.commons.util.DateTimeUtils;
import run.acloud.commons.vo.ListCountVO;
import run.acloud.framework.context.ContextHolder;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;

import java.util.*;
import java.util.regex.Pattern;

@Slf4j
@Service
public class AuditService {
	@Resource(name = "cocktailSession")
    private SqlSessionTemplate sqlSession;

    /**
     * Audit Log List 조회.
     * @param params
     * @return
     * @throws Exception
     */
    public List<AuditLogVO> getAuditLogs(Map<String, Object> params) throws Exception {
        IAuditLogMapper dao = sqlSession.getMapper(IAuditLogMapper.class);

        // get Lists
        List<AuditLogVO> auditLogs = dao.getAuditLogs(params);

        return auditLogs;
    }

    /**
     * Audit Log List 조회
     * @param order
     * @param orderColumn
     * @param nextPage
     * @param itemPerPage
     * @param searchColumn
     * @param searchKeyword
     * @return
     */
    public AuditLogListVO getAuditLogs(String order, String orderColumn, Integer nextPage, Integer itemPerPage, String searchColumn, String searchKeyword, String maxId, String newId, String startDate, String endDate) throws Exception {
        /**
         * DevOps 권한의 사용자는 접근이 불가함..
         */
        AuthUtils.checkAuth(ContextHolder.exeContext(), AuthUtils.checkUserDevOpsBlockAuthPredicate());

        AuditLogListVO auditLogList = new AuditLogListVO();
        try {
            Map<String, Object> params = new HashMap<>();

            Integer limitNextPage = (nextPage - 1) * itemPerPage; // nextPage 값을 LIMIT Query 형태에 맞도록 변환

            if(StringUtils.isBlank(startDate) || StringUtils.isBlank(endDate)) { // 날짜 없으면 Default로 현재부터 일주일 설정
                Date dEndDate = new Date();
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(dEndDate);
                calendar.add(Calendar.DATE, -7);

                Date dStartDate = calendar.getTime();
                startDate = DateTimeUtils.getUtcTimeString(dStartDate, DateTimeUtils.DEFAULT_DB_DATE_FORMAT);
                endDate = DateTimeUtils.getUtcTimeString(dEndDate, DateTimeUtils.DEFAULT_DB_DATE_FORMAT);
            }

            if(StringUtils.isNotBlank(searchColumn) && StringUtils.isNotBlank(searchKeyword)) {
                params.put(searchColumn, searchKeyword);
            }

            params.put("order", order);
            params.put("orderColumn", orderColumn);
            params.put("nextPage", limitNextPage);
            params.put("itemPerPage", itemPerPage);
            params.put("startDate", startDate);
            params.put("endDate", endDate);

            UserRole userRole = UserRole.valueOf(ContextHolder.exeContext().getUserRole());

            // Admin 사용자가 아니면 Admin Permission에 해당하는 Audit Log는 조회 불가..
            if(!userRole.isAdmin()) {
                params.put("excludeAdmin", "Y");
            }

            // System 사용자는 권한이 있는 System에 해당하는 Audit Log만 조회 가능.
            if(userRole.isUserOfSystem()) {
                params.put("systemUserSeq", ContextHolder.exeContext().getUserSeq());
            }

            ListCountVO auditLogCount = getAuditLogCountAndMaxId(params);
            Integer totalCount = auditLogCount.getCnt(); // this.getAuditLogCount(params);
            if(StringUtils.isBlank(maxId) && auditLogCount.getMaxId() != null) {
                maxId = auditLogCount.getMaxId().toString();
            }

            params.put("maxId", maxId);
            /** Params Setting Completed **/

            List<AuditLogVO> auditLogs = this.getAuditLogs(params);
//            if(!userRole.isAdmin()) {
//                for (AuditLogVO audit : auditLogs) {
//                    audit.setRequestData("Not visible by security policy.");
//                    audit.setResponseData("Not visible by security policy.");
//                }
//            }

            auditLogList.setAuditLogs(auditLogs);
            auditLogList.setTotalCount(totalCount);
            auditLogList.setMaxId(maxId);
            auditLogList.setCurrentPage(nextPage);
        }
        catch (CocktailException ce) {
            throw ce;
        }
        catch (Exception ex) {
            throw new CocktailException("Auidit Log List Inquire Failure", ex, ExceptionType.CommonInquireFail);
        }

        return auditLogList;
    }

    /**
     * Audit Log 갯수
     * @param params
     * @return
     * @throws Exception
     */
    public Integer getAuditLogCount(Map<String, Object> params) throws Exception {
        IAuditLogMapper dao = sqlSession.getMapper(IAuditLogMapper.class);

        // get Audit Log Count
        Integer auditLogCount = dao.getAuditLogCount(params);

        return auditLogCount;
    }

    /**
     * Audit Log 갯수 & audit_log_seq 기준 maxId 조회
     * @param params
     * @return
     * @throws Exception
     */
    public ListCountVO getAuditLogCountAndMaxId(Map<String, Object> params) throws Exception {
        IAuditLogMapper dao = sqlSession.getMapper(IAuditLogMapper.class);

        // get Audit Log Count
        return dao.getAuditLogCountAndMaxId(params);
    }

    /**
     * Audit Log 기준점 (MaxId 조회)
     * @param params
     * @return
     * @throws Exception
     */
    public String getAuditLogMaxId(Map<String, Object> params, String maxId) throws Exception {
        try {
            /**
             * MaxID는 여러 케이스가 될 수도 있지만 현재는 audit_log_seq를 기준으로 처리한다..
             * 따라서 Integer 타입인지 체크함..
             */
            if(StringUtils.isNotBlank(maxId)) {
                if (!Pattern.matches("^[0-9]+$", maxId)) {
                    throw new CocktailException("maxId Parameter must be of Integer.", ExceptionType.InvalidParameter);
                }
            }
            else {
                // get Audit Log MaxId
                IAuditLogMapper dao = sqlSession.getMapper(IAuditLogMapper.class);
                // params의 파라미터값에 따라 maxId가 달라질 수 있으므로 우선 Spec에 params플 넘기도록 함..
                Integer intMax = dao.getAuditLogMaxCount(params);
                if(intMax != null) {
                    maxId = intMax.toString();
                }
            }

            return maxId;
        }
        catch (CocktailException ce) {
            throw ce;
        }
        catch (Exception ex) {
            throw new CocktailException("Auidit Log MaxID Inquire Failure", ex, ExceptionType.CommonInquireFail);
        }
    }

    /**
     * Audit Log 입력
     * @param auditLog
     * @return
     * @throws Exception
     */
    public int addAuditLog(AuditLogVO auditLog) throws Exception {
        IAuditLogMapper dao = sqlSession.getMapper(IAuditLogMapper.class);

        // get Account
        int count = dao.addAuditLog(auditLog);

        return count;
    }

}
