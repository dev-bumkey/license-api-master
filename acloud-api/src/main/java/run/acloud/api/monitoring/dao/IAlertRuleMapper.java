package run.acloud.api.monitoring.dao;

import org.apache.ibatis.annotations.Param;
import run.acloud.api.monitoring.vo.AlertRuleClusterMappingVO;
import run.acloud.api.monitoring.vo.AlertRuleVO;
import run.acloud.commons.vo.ListCountVO;

import java.util.List;
import java.util.Map;

public interface IAlertRuleMapper {

    List<AlertRuleVO> getAlertRules(Map<String, Object> params) throws Exception;

    List<AlertRuleVO> getAlertRulesForExcel(Map<String, Object> params) throws Exception;

    AlertRuleVO getAlertRule(@Param("alertRuleSeq") Integer alertRuleSeq) throws Exception;

    Integer getAlertRuleCount(Map<String, Object> params) throws Exception;

    ListCountVO getAlertRuleCountAndMaxId(Map<String, Object> params) throws Exception;

    List<Integer> getUserSeqsOfAlertRule(Integer alertRuleSeq);

    Integer getAlertRuleMaxId(Map<String, Object> params) throws Exception;

    int addAlertRuleUserMappingByUser(
        @Param("userSeq") Integer userSeq,
        @Param("alertRuleSeqs") List<Integer> alertRuleSeqs,
        @Param("creator") Integer creator) throws Exception;

    int addAlertRuleUserMappingByAlertRule(
        @Param("alertRuleSeq") Integer alertRuleSeq,
        @Param("userSeqs") List<Integer> userSeqs,
        @Param("creator") Integer creator) throws Exception;

    int deleteAlertRuleUserMappingByUser(
        @Param("userSeq") Integer userSeq,
        @Param("alertRuleSeqs") List<Integer> alertRuleSeqs) throws Exception;

    int deleteAlertRuleUserMappingByAlertRule(
        @Param("alertRuleSeq") Integer alertRuleSeq,
        @Param("userSeqs") List<Integer> userSeqs) throws Exception;

    List<Integer> getClusterSeqsOfAlertRule(Integer alertRuleSeq);

    int addAlertRuleClusterMappingByAlertRule(
            @Param("alertRuleSeq") Integer alertRuleSeq,
            @Param("clusterSeqs") List<Integer> clusterSeqs,
            @Param("creator") Integer creator) throws Exception;

    int deleteAlertRuleClusterMappingByAlertRule(
            @Param("alertRuleSeq") Integer alertRuleSeq,
            @Param("clusterSeqs") List<Integer> clusterSeqs) throws Exception;

    List<AlertRuleClusterMappingVO> getClustersByAlertRule(
            @Param("alertRuleSeq") Integer alertRuleSeq
    ) throws Exception;

    int addAlertRule(AlertRuleVO alertRule) throws Exception;
    int updateAlertRule(AlertRuleVO alertRule) throws Exception;
    int removeAlertRule(AlertRuleVO alertRule) throws Exception;
}
