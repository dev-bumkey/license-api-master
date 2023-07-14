package run.acloud.api.log.dao;

import org.apache.ibatis.annotations.Param;
import run.acloud.api.configuration.vo.ClusterVO;
import run.acloud.api.log.vo.LogAgentVO;

public interface IAddonLogAgentMapper {
    Integer getAddonLogAgentSeq(@Param("agentName") String releaseName, @Param("cluster") ClusterVO cluster);
    int addAddonLogAgent(LogAgentVO logAgent);
    int removeAddonLogAgent(Integer logAgentSeq);
    String getAddonLogAgentToken(@Param("agentName") String releaseName, @Param("cluster") ClusterVO cluster);
}
