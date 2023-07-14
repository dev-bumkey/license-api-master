package run.acloud.api.pl.dao;

import org.apache.ibatis.annotations.Param;
import run.acloud.api.pl.vo.*;
import run.acloud.commons.vo.ListCountVO;

import java.util.List;

public interface IPlMapper {
    // 조회
    List<PlMasterListVO> getPlList(@Param("accountSeq") Integer accountSeq, @Param("serviceSeq") Integer serviceSeq);
    PlMasterVO getPlDetail(@Param("plSeq") Integer plSeq);
    List<PlMasterVO> getPlMasterByNameAndWorkspace(@Param("name") String name, @Param("serviceSeq") Integer serviceSeq);
    PlResBuildVO getPlResBuild(@Param("plSeq") Integer plSeq, @Param("plResBuildSeq") Integer plResBuildSeq);
    PlResDeployVO getPlResDeploy(
            @Param("plSeq") Integer plSeq,
            @Param("plResDeploySeq") Integer plResDeploySeq
    );
    PlMasterVO getPlMaster(@Param("plSeq") Integer plSeq);
    int getPlResDeployWorkloadCount(@Param("plSeq") Integer plSeq);
    List<PlRunVO> getPlRunList(PlRunListSearchVO params);
    ListCountVO getPlRunListCount(PlRunListSearchVO params);
    List<PlRunVO> getPlVerList(@Param("plSeq") Integer plSeq);
    PlMasterVO existPlVersion(@Param("plSeq") Integer plSeq, @Param("ver") String ver);
    PlRunVO getPlRunDetail(@Param("plRunSeq") Integer plRunSeq, @Param("runYn") String runYn);
    PlRunVO getPlRun(@Param("plRunSeq") Integer plRunSeq);
    PlRunBuildVO getPlRunBuildDetail(@Param("plSeq") Integer plSeq, @Param("plRunSeq") Integer plRunSeq, @Param("plRunBuildSeq") Integer plRunBuildSeq);
    Integer getAccountSeq(@Param("plSeq") Integer plSeq);
    int getPlRunCount(@Param("plSeq") Integer plSeq);
    PlRunDeployVO getPlRunDeploy(
            @Param("plRunSeq") Integer plRunSeq,
            @Param("plRunDeploySeq") Integer plRunDeploySeq
    );


    // 등록
    int insertPlMaster(PlMasterVO plMaster);
    int insertPlResDeploy(PlResDeployVO plResDeploy);
    int insertPlResDeploysFromRunDeploy(@Param("plRunSeq") Integer plRunSeq);
    int insertPlResBuild(PlResBuildVO plResBuild);
    int insertPlResBuildsFromRunBuild(@Param("plRunSeq") Integer plRunSeq);
    int insertPlResBuildDeployMapping(PlResBuildDeployMappingVO plResBuildDeployMapping);
    int insertPlResBuildDeployMappingFromRun(@Param("plRunSeq") Integer plRunSeq);
    int insertPlRun(PlRunVO plRun);
    int insertPlRunBuild(PlRunBuildVO plRunBuild);
    int insertPlRunBuildWithRes(
            @Param("plSeq") Integer plSeq,
            @Param("plRunSeq") Integer plRunSeq,
            @Param("runStatus") String runStatus,
            @Param("runYn") String runYn,
            @Param("creator") Integer creator
    );
    int insertPlRunDeploy(PlRunDeployVO plRunDeploy);
    int insertPlRunDeployWithRes(
            @Param("plSeq") Integer plSeq,
            @Param("plRunSeq") Integer plRunSeq,
            @Param("runStatus") String runStatus,
            @Param("runYn") String runYn,
            @Param("creator") Integer creator
    );
    int insertPlRunBuildDeployMappingWithRes(@Param("plRunSeq") Integer plRunSeq);


    // 수정
    int updatePlMasterForRunSeqAndVersion(@Param("plSeq") Integer plSeq, @Param("plRunSeq") Integer plRunSeq, @Param("ver") String ver);
    int updatePlMasterName(@Param("plSeq") Integer plSeq, @Param("name") String name, @Param("updater") Integer updater);
    int updatePlResDeploy(PlResDeployVO plResDeploy);
    int updatePlResDeploysToSameDate(@Param("plSeq") Integer plSeq);
    int updatePlResBuild(PlResBuildVO plResBuild);
    int updatePlResBuildsForRunYn(@Param("plSeq") Integer plSeq, @Param("runYn") String runYn);
    int updatePlResBuildsToSameDate(@Param("plSeq") Integer plSeq);
    int updatePlResDeployWorkloadRunOrderForDel(
            @Param("plSeq") Integer plSeq,
            @Param("delRunOrder") Integer delRunOrder,
            @Param("updater") Integer updater
    );
    int updatePlResDeployWorkloadRunOrder(
            @Param("plSeq") Integer plSeq,
            @Param("plResDeploySeq") Integer plResDeploySeq,
            @Param("updateRunOrder") Integer updateRunOrder,
            @Param("runOrder") Integer runOrder,
            @Param("updater") Integer updater
    );
    int updatePlResDeployRunYn(
            @Param("plSeq") Integer plSeq,
            @Param("plResDeploySeq") Integer plResDeploySeq,
            @Param("runYn") String runYn,
            @Param("updater") Integer updater
    );
    int updatePlResBuildRunOrderForDel(
            @Param("plSeq") Integer plSeq,
            @Param("delRunOrder") Integer delRunOrder,
            @Param("updater") Integer updater
    );
    int updatePlResBuildRunOrder(
            @Param("plSeq") Integer plSeq,
            @Param("plResBuildSeq") Integer plResBuildSeq,
            @Param("updateRunOrder") Integer updateRunOrder,
            @Param("runOrder") Integer runOrder,
            @Param("updater") Integer updater
    );
    int updatePlResBuildRunYn(
            @Param("plSeq") Integer plSeq,
            @Param("plResBuildSeq") Integer plResBuildSeq,
            @Param("runYn") String runYn,
            @Param("updater") Integer updater
    );
    int updatePlRunStatus(
            @Param("runStatus") String runStatus,
            @Param("plRunSeq") Integer plRunSeq
    );
    int updatePlRunBuildForBuildRunSeq(
            @Param("plRunBuildSeq") Integer plRunBuildSeq,
            @Param("buildRunSeq") Integer buildRunSeq
    );
    int updatePlRunBuildStatus(
            @Param("plRunBuildSeq") Integer plRunBuildSeq,
            @Param("runStatus") String runStatus
    );
    int updatePlRunBuildForbuildCont(
            @Param("plRunBuildSeq") Integer plRunBuildSeq,
            @Param("buildCont") String buildCont
    );
    int updatePlRunBuildForBuildTagAndImgUrl(
            @Param("plRunBuildSeq") Integer plRunBuildSeq,
            @Param("buildTag") String buildTag,
            @Param("imgUrl") String imgUrl
    );
    int updatePlRunBuildLog(
            @Param("plRunBuildSeq") Integer plRunBuildSeq,
            @Param("runLog") String runLog
    );
    int updatePlResBuildsFromRunBuilds(
            @Param("plSeq") Integer plSeq,
            @Param("plRunSeq") Integer plRunSeq
    );
    int updatePlRunDeployStatus(
            @Param("runStatus") String runStatus,
            @Param("runLog") String runLog,
            @Param("plRunDeploySeq") Integer plRunDeploySeq
    );
    int updatePlRunDeployLog(
            @Param("runLog") String runLog,
            @Param("plRunDeploySeq") Integer plRunDeploySeq
    );
    int updatePlRunDeployContents(
            @Param("resCont") String resCont,
            @Param("plRunDeploySeq") Integer plRunDeploySeq
    );
    int updatePlResDeploysFromRunDeploys(
            @Param("plSeq") Integer plSeq,
            @Param("plRunSeq") Integer plRunSeq
    );


    // 삭제
    int deletePlMaster(@Param("plSeq") Integer plSeq, @Param("updater") Integer updater);
    int deletePlResBuild(@Param("plResBuildSeq") Integer plResBuildSeq);
    /**
     * pl_seq 에 해당하는 pl_res_build 정보 삭제<br/>.
     * pl_res_build_deploy_mapping 테이블도 cascade 되어 있어 같이 삭제된다.
     *
     * @param plSeq
     * @return
     */
    int deletePlResBuilds(@Param("plSeq") Integer plSeq);
    int deletePlResDeploy(@Param("plResDeploySeq") Integer plResDeploySeq);
    int deletePlResDeploys(@Param("plSeq") Integer plSeq);
    int deletePlResBuildDeployMapping(@Param("plResBuildSeq") Integer plResBuildSeq, @Param("plResDeploySeq") Integer plResDeploySeq);


}
