package run.acloud.api.monitoring.dao;

import org.apache.ibatis.annotations.Param;
import run.acloud.api.monitoring.vo.AlertUserSearchVO;
import run.acloud.api.monitoring.vo.AlertUserVO;
import run.acloud.commons.vo.ListCountVO;

import java.util.List;

public interface IAlertUserMapper {

    List<AlertUserVO> getAlertUsers(AlertUserSearchVO params) throws Exception;

    AlertUserVO getAlertUser(@Param("alertUserSeq") Integer alertUserSeq) throws Exception;

    Integer getAlertUserCount(AlertUserSearchVO params) throws Exception;

    ListCountVO getAlertUserCountAndMaxId(AlertUserSearchVO params) throws Exception;

    Integer getAlertUserMaxId(AlertUserSearchVO params) throws Exception;

    int addAlertUser(AlertUserVO alertUser) throws Exception;
    int updateAlertUser(AlertUserVO alertUser) throws Exception;
    int removeAlertUser(AlertUserVO alertUser) throws Exception;


    List<AlertUserVO> getAlertUsersOfRule(
            @Param("accountSeq") Integer accountSeq,
            @Param("clusterSeq") Integer clusterSeq,
            @Param("clusterId") String clusterId,
            @Param("alertRuleSeq") Integer alertRuleSeq,
            @Param("alertRuleId") String alertRuleId
    ) throws Exception;

}
