package run.acloud.api.configuration.dao;

import org.apache.ibatis.annotations.Param;
import run.acloud.api.configuration.vo.ClusterAccessAuthVO;

import java.util.List;
import java.util.Map;

public interface IClusterAccessAuthMapper {

    List<ClusterAccessAuthVO> getClusterAccessAuthList(Map<String, Object> params) throws Exception;

    ClusterAccessAuthVO getClusterAccessAuth(@Param("clusterAuthSeq") Integer clusterAuthSeq) throws Exception;

    int addClusterAccessAuth(ClusterAccessAuthVO clusterAccessAuth) throws Exception;

    int editClusterAccessAuth(ClusterAccessAuthVO clusterAccessAuth) throws Exception;

    int removeClusterAccessAuth(@Param("clusterAuthSeq") Integer clusterAuthSeq) throws Exception;
}
