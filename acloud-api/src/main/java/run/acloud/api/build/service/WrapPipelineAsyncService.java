package run.acloud.api.build.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import run.acloud.api.build.vo.BuildRunVO;

@Slf4j
@Service
public class WrapPipelineAsyncService {

    @Autowired
    private PipelineAsyncService pipelineAsyncService;

    public void processPipelineService(BuildRunVO buildRunVO) {
        pipelineAsyncService.processPipelineService(buildRunVO);
    }
}
