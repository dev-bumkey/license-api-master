package run.acloud.api.audit.dao;

import org.apache.ibatis.annotations.Param;
import run.acloud.api.audit.vo.AuditLogVO;
import run.acloud.commons.vo.ListCountVO;

import java.util.List;
import java.util.Map;

public interface IAuditLogMapper {

    List<AuditLogVO> getAuditLogs(Map<String, Object> params) throws Exception;

    Integer getAuditLogCount(Map<String, Object> params) throws Exception;

    ListCountVO getAuditLogCountAndMaxId(Map<String, Object> params) throws Exception;

    Integer getAuditLogMaxCount(Map<String, Object> params) throws Exception;

    int addAuditLog(AuditLogVO auditLog) throws Exception;

    int getAuditLogCountForBatch(@Param("baseDate") String baseDate) throws Exception;
    int deleteAuditLogForBatch(@Param("baseDate") String baseDate) throws Exception;
}
