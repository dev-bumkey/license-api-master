package run.acloud.api.pipelineflow.service;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;
import run.acloud.api.build.service.PipelineBuildRunService;
import run.acloud.api.build.vo.BuildAddVO;
import run.acloud.api.build.vo.BuildRunVO;

import java.io.IOException;

/**
 * Build 쪽 Service 사용애 대해 처리하는 서비스 객체.
 * 추후 빌드의 분리가 되는것을 감안하여 loose coupling 되도록 pipeline-build 간의 layer 역확을 하는 Service 임
 *
 */
@Slf4j
@Service
public class WrappedBuildService {

    @Autowired
    private PipelineBuildRunService buildRunService;

    public BuildRunVO getBuildRunsByImageUrl(Integer buildSeq, String imageUrl){
        return buildRunService.getBuildRunsByImageUrl(null, null, buildSeq, imageUrl );
    }

    public BuildRunVO getBuildRunsByImageUrl(Integer buildSeq, Integer serviceSeq, String imageUrl) {
        return buildRunService.getBuildRunsByImageUrl(null, serviceSeq, buildSeq, imageUrl);
    }

    public void checkPossibleRunBuildBySystem(Integer accountSeq) throws Exception {
        buildRunService.checkPossibleRunBuildBySystem(accountSeq);
    }

    public void checkPossibleRunBuildByBuild(Integer buildSeq, Integer pipelineWorkloadSeq) throws Exception {
        buildRunService.checkPossibleRunBuildByBuild(buildSeq, pipelineWorkloadSeq);
    }

    public BuildRunVO createRunBuildByBuildRun(Integer buildSeq, Integer buildRunSeq, String callbackUrl, String tagName, String description, Integer pipelineSeq) throws Exception {
        return buildRunService.createBuildRunByBuildRun( buildSeq, buildRunSeq, callbackUrl, tagName, description, pipelineSeq);
    }

    public BuildRunVO createBuildRunByBuildRunModify(Integer buildSeq, Integer buildRunSeq, String callbackUrl, String tagName, String description, Integer pipelineSeq, BuildAddVO buildAdd, BindingResult result) throws Exception {
        return buildRunService.createBuildRunByBuildRunModify(buildSeq, buildRunSeq, callbackUrl, tagName, description, pipelineSeq, buildAdd, result);
    }

    public BuildRunVO createBuildRunByBuildCancel(Integer buildRunSeq, String callbackUrl) throws Exception {
        return buildRunService.createBuildRunByBuildCancel(buildRunSeq, callbackUrl);
    }

    public BuildRunVO getBuildRun(Integer buildRunSeq) throws IOException {
        return buildRunService.getBuildRun(buildRunSeq);
    }

    public BuildRunVO getBuildRun(Integer buildRunSeq, String useYn, boolean withConvert) throws IOException {
        return buildRunService.getBuildRun(buildRunSeq, useYn, withConvert);
    }

    public BuildRunVO getBuildRunWithPasswd(Integer buildRunSeq) throws IOException {
        return buildRunService.getBuildRunWithPasswd(buildRunSeq);
    }

    public BuildRunVO convertBuildStepRunConfig(BuildRunVO buildRun, boolean withoutPasswd) throws IOException {
        return buildRunService.convertBuildStepRunConfig(buildRun, withoutPasswd);
    }

    public BuildAddVO convertFromBuildRunToBuildAdd(BuildRunVO buildRun){
        return buildRunService.convertFromBuildRunToBuildAdd(buildRun);
    }

    public void updateStateAndSendEvent(BuildRunVO buildRun){
        buildRunService.updateStateAndSendEvent(buildRun);
    }

    public void handleBuildResult(Integer buildRunSeq, String pipelineResultJson) throws Exception {
        buildRunService.handleBuildResult(buildRunSeq, pipelineResultJson);
    }

}
