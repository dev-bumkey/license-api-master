package run.acloud.api.audit.service;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.stereotype.Service;
import run.acloud.api.audit.dao.IAuditAccessLogMapper;
import run.acloud.api.audit.vo.AuditAccessLogListVO;
import run.acloud.api.audit.vo.AuditAccessLogVO;
import run.acloud.api.auth.dao.IUserMapper;
import run.acloud.api.auth.enums.UserRole;
import run.acloud.commons.util.AuthUtils;
import run.acloud.commons.util.DateTimeUtils;
import run.acloud.commons.vo.ListCountVO;
import run.acloud.framework.context.ContextHolder;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;

import java.sql.SQLException;
import java.util.*;

@Slf4j
@Service
public class AuditAccessService {
	@Resource(name = "cocktailSession")
    private SqlSessionTemplate sqlSession;

    /**
     * Audit Log List 조회.
     * @param params
     * @return
     * @throws Exception
     */
    public List<AuditAccessLogVO> getAuditAccessLogs(Map<String, Object> params) throws Exception {
        IAuditAccessLogMapper dao = sqlSession.getMapper(IAuditAccessLogMapper.class);

        // get Lists
        List<AuditAccessLogVO> auditAccessLogs = dao.getAuditAccessLogs(params);

        return auditAccessLogs;
    }

    /**
     * Audit Access Log List 조회
     * @param order
     * @param orderColumn
     * @param nextPage
     * @param itemPerPage
     * @param searchColumn
     * @param searchKeyword
     * @return
     */
    public AuditAccessLogListVO getAuditAccessLogs(String order, String orderColumn, Integer nextPage, Integer itemPerPage, String searchColumn, String searchKeyword, String maxId, String newId, String startDate, String endDate) throws Exception {
        /**
         * DevOps 권한의 사용자는 접근이 불가함..
         */
        AuthUtils.checkAuth(ContextHolder.exeContext(), AuthUtils.checkUserDevOpsBlockAuthPredicate());

        AuditAccessLogListVO auditLogList = new AuditAccessLogListVO();
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
            // system 사용자인데 맵핑정보 없으면 빈값 리턴
            if(userRole.isUserOfSystem()) {
                IUserMapper userMapper = sqlSession.getMapper(IUserMapper.class);
                List<Integer> accountSeqs = userMapper.getAccountOfAccountUser(ContextHolder.exeContext().getUserSeq());
                if (accountSeqs != null){
                    params.put("accountSeq", accountSeqs.get(0));
                } else {
                    return auditLogList;
                }
            }

            ListCountVO auditLogCount = getAuditAccessLogCountAndMaxId(params);
            Integer totalCount = auditLogCount.getCnt(); // this.getAuditAccessLogCount(params);
            if(StringUtils.isBlank(maxId) && auditLogCount.getMaxId() != null) {
                maxId = auditLogCount.getMaxId().toString();
            }

            params.put("maxId", maxId);
            /** Params Setting Completed **/

            List<AuditAccessLogVO> auditAccessLogs = this.getAuditAccessLogs(params);

            auditLogList.setAuditAccessLogs(auditAccessLogs);
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
     * Audit Log 갯수 & audit_log_seq 기준 maxId 조회
     * @param params
     * @return
     * @throws Exception
     */
    public ListCountVO getAuditAccessLogCountAndMaxId(Map<String, Object> params) throws Exception {
        IAuditAccessLogMapper dao = sqlSession.getMapper(IAuditAccessLogMapper.class);

        // get Audit Log Count
        return dao.getAuditAccessLogCountAndMaxId(params);
    }

    /**
     * Audit Log 입력
     * @param auditAccessLog
     * @return
     * @throws Exception
     */
    public int addAuditAccessLog(AuditAccessLogVO auditAccessLog) throws Exception {
        IAuditAccessLogMapper dao = sqlSession.getMapper(IAuditAccessLogMapper.class);

        // get Account
        int count = dao.addAuditAccessLog(auditAccessLog);

        return count;
    }

    /**
     * audit_access_logs table 존재여부 확인
     *
     * @return
     * @throws Exception
     */
    public boolean existAuditAccessLogsTable() {
        IAuditAccessLogMapper dao = sqlSession.getMapper(IAuditAccessLogMapper.class);

        boolean existTable = false;
        try {
            existTable = dao.existAuditAccessLogsTable();
        } catch (SQLException e) {
            log.error("error occured check exists audit access logs table.", e);
        }

        return existTable;
    }

}
