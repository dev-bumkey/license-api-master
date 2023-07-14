package run.acloud.api.log.dao;

import run.acloud.api.log.vo.LogAgentAccountMappingVO;
import run.acloud.api.log.vo.LogAgentVO;

import java.util.List;

public interface ILogAgentMapper {
    List<LogAgentVO> getLogAgentList();
    int addLogAgent(LogAgentVO logAgent);
    int addLogAgentAccountMapping(LogAgentAccountMappingVO logAgentAccountMapping);
    LogAgentVO getLogAgent(Integer logAgentSeq);
    int editLogAgent(LogAgentVO logAgent);
    int removeLogAgent(Integer agentSeq);
    List<String> getTokenList();
    List<String> getApplicationList();
}
