package run.acloud.api.build.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import run.acloud.api.build.vo.BuildRunVO;
import run.acloud.api.pipelineflow.service.PipelineAPIService;

@Slf4j
@Service
public class WrapPipelineAPIService {
    @Autowired
    private PipelineAPIService pipelineAPIService;

    public boolean checkPipelineServer() throws Exception {
        return pipelineAPIService.checkPipelineServer();
    }

    public BuildRunVO runBuild(BuildRunVO buildRun) throws Exception {
        return pipelineAPIService.runBuild(buildRun);
    }

    public BuildRunVO stopBuild(BuildRunVO buildRun){
        return pipelineAPIService.stopBuild(buildRun);
    }

    public BuildRunVO removeBuild(BuildRunVO buildRun){
        return pipelineAPIService.terminateBuild(buildRun);
    }


}
