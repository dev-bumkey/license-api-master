package run.acloud.api.build.dao;


import org.apache.ibatis.annotations.Param;
import run.acloud.api.build.enums.RunState;
import run.acloud.api.build.enums.StepState;
import run.acloud.api.build.vo.BuildRunLogVO;
import run.acloud.api.build.vo.BuildRunVO;
import run.acloud.api.build.vo.BuildStepRunVO;
import run.acloud.api.build.vo.BuildVO;

import java.util.List;

public interface IBuildRunMapper {
    int addBuildRun(BuildRunVO buildRunVO);
    int addBuildStepRunsByBuildStep(BuildRunVO buildRunVO);
    int addBuildStepRunsByHistory(BuildRunVO buildRunVO);
    int addBuildStepRun(BuildStepRunVO buildStepRunVO);
    int removeBuildRun(BuildRunVO buildRunVO);
    int removeBuildRunByBuild(BuildVO buildVO);
    int removeBuildStepRun(BuildRunVO buildRunVO);
    int removeBuildStepRunByBuild(BuildVO buildVO);

    List<BuildRunVO> getBuildRuns(@Param("buildSeq") Integer buildSeq);
    List<BuildRunVO> getBuildRunsByBuildRunSeqs(@Param("buildRunSeqs") List<Integer> buildRunSeqs);
    List<BuildRunVO> getBuildRunsByLatest(@Param("accountSeq") Integer accountSeq, @Param("serviceSeq") Integer serviceSeq, @Param("limitCount") Integer limitCount);
    List<BuildRunVO> getBuildRunsByExistsImage(@Param("buildSeq") Integer buildSeq);
    List<BuildRunVO> getBuildRunsBySameDigest(@Param("buildSeq") Integer buildSeq, @Param("imageDigest") String imageDigest);
    List<BuildRunVO> getBuildRunsBySameTagName(@Param("buildSeq") Integer buildSeq, @Param("tagName") String tagName);
    List<BuildRunVO> getOtherBuildRunsBySameImageUrl(@Param("buildSeq") Integer buildSeq, @Param("buildRunSeqs") List<Integer> buildRunSeqs);

    BuildRunVO getBuildRun(@Param("buildRunSeq") Integer buildRunSeq);
    BuildRunVO getBuildRunWithUseYn(@Param("buildRunSeq") Integer buildRunSeq, @Param("useYn") String useYn);
    BuildRunVO getBuildRunByImageUrl( @Param("accountSeq") Integer accountSeq, @Param("serviceSeq") Integer serviceSeq, @Param("buildSeq") Integer buildSeq, @Param("imageUrl") String imageUrl );
    List<BuildStepRunVO> getBuildStepRuns(@Param("buildRunSeq") Integer buildRunSeq);

    BuildStepRunVO getBuildStepRun(Integer buildStepRunSeq);
    String getBuildStepRunConfig(Integer buildStepRunSeq);

    BuildRunLogVO getBuildAllLog(Integer buildRunSeq);
    BuildRunLogVO getBuildLog(@Param("buildStepRunSeq") Integer buildStepRunSeq);
    int updateBuildStepRunLog(@Param("buildStepRunSeq") Integer buildStepRunSeq, @Param("log") String log);
    int updateBuildStepRunLogId(@Param("buildStepRunSeq") Integer buildStepRunSeq, @Param("logId") String logId);
    int updateBuildStepConfig(@Param("buildStepRunSeq") Integer buildStepRunSeq, @Param("stepConfig") String stepConfig);

    int updateBuildRunState(@Param("buildRunSeq") Integer buildRunSeq, @Param("runState") RunState runState);
    int updateBuildStepRunState(@Param("buildStepRunSeq") Integer buildStepRunSeq, @Param("stepState") StepState stepState);
    int updateBuildStepRunStateWhenErrorOnRunning(@Param("buildRunSeq") Integer buildRunSeq);
    int updateImageBuildResult(@Param("buildRunSeq") Integer buildRunSeq, @Param("imageUrl") String imageUrl, @Param("imageSize") Long imageSize, @Param("imageDigest") String imageDigest);

    int getRunningBuildCount( @Param("accountSeq") Integer accountSeq, @Param("buildSeq") Integer buildSeq, @Param("pipelineSeq") Integer pipelineSeq);

}
