package run.acloud.api.build.dao;

import org.apache.ibatis.annotations.Param;
import run.acloud.api.build.vo.*;

import java.util.List;

public interface IBuildMapper {

    int addBuild(BuildAddVO buildAdd);
    int editBuild(BuildAddVO buildAdd);
    int removeBuild(BuildVO build);

    int addBuildStep(BuildStepVO buildStep);
    int editBuildStep(BuildStepVO buildStep);
    int removeBuildSteps(BuildVO build);

    int updateBuildNo (@Param("buildSeq") Integer buildSeq, @Param("buildNo") Integer buildNo);
    int getNextBuildNo (BuildVO build);

    SystemBuildCountVO getSystemBuildCount( @Param("accountSeq") Integer accountSeq );
    List<BuildVO> getBuildList( @Param("accountSeq") Integer accountSeq, @Param("serviceSeq") Integer serviceSeq, @Param("registryProjectIds") List<Integer> registryProjectIds, @Param("externalRegistrySeqs") List<Integer> externalRegistrySeqs, @Param("registryUrl") String registryUrl);
    List<BuildVO> getBuildNames( @Param("accountSeq") Integer accountSeq, @Param("serviceSeq") Integer serviceSeq);
    BuildImageVO getBuildImages( @Param("buildSeq") Integer buildSeq );
    BuildVO getBuild(@Param("buildSeq") Integer buildSeq);
    BuildVO getBuildWithoutUseYn(@Param("buildSeq") Integer buildSeq);

    int checkImageName(
            @Param("registryName") String registryName,
            @Param("imageName") String imageName,
            @Param("registryProjectId") Integer registryProjectId,
            @Param("externalRegistrySeq") Integer externalRegistrySeq,
            @Param("buildSeq") Integer buildSeq
    );

    int deleteBuildStepRunByAccount(@Param("accountSeq") Integer accountSeq);
    int deleteBuildRunByAccount(@Param("accountSeq") Integer accountSeq);
    int deleteBuildStepByAccount(@Param("accountSeq") Integer accountSeq);
    int deleteBuildByAccount(@Param("accountSeq") Integer accountSeq);
}
