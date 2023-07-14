package run.acloud.api.build.dao;

import org.apache.ibatis.annotations.Param;
import run.acloud.api.build.vo.BuildServerVO;

import java.util.List;

public interface IBuildServerMapper {

    int addBuildServer(BuildServerVO buildServer);

    int addBuildServerMapping(BuildServerVO buildServerVO);

    int editBuildServer(BuildServerVO buildServer);
    int removeBuildServer(BuildServerVO buildServer);

    List<BuildServerVO> getBuildServerList(
            @Param("accountSeq") Integer accountSeq,
            @Param("topicName") String topicName
    );

    List<BuildServerVO> getBuildServerListForRef(
            @Param("serviceSeq") Integer serviceSeq,
            @Param("topicName") String topicName
    );

    BuildServerVO getBuildServer(@Param("buildServerSeq") Integer buildServerSeq);


}
