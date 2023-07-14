package run.acloud.api.audit.dao;

import org.apache.ibatis.annotations.Param;
import run.acloud.api.audit.vo.AuditAccessLogVO;
import run.acloud.commons.vo.ListCountVO;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public interface IAuditAccessLogMapper {

    List<AuditAccessLogVO> getAuditAccessLogs(Map<String, Object> params) throws Exception;

    Integer getAuditAccessLogCount(Map<String, Object> params) throws Exception;

    ListCountVO getAuditAccessLogCountAndMaxId(Map<String, Object> params) throws Exception;

    Integer getAuditAccessLogMaxCount(Map<String, Object> params) throws Exception;

    int addAuditAccessLog(AuditAccessLogVO auditLog) throws Exception;

    boolean existAuditAccessLogsTable() throws SQLException;
    boolean existAuditAccessLogsPartition(@Param("partitionName") String partitionName);

    int getMaxSeq();

    int migrationAuditLogsToAuditAccessLog();
    int dropPartitionAuditAccessLogForBatch(@Param("dropPartitionName") String dropPartitionName);
    int addPartitionToAuditAccessLogForBatch(@Param("partitionName") String partitionName, @Param("partitionConditionDate") String partitionConditionDate);

}
