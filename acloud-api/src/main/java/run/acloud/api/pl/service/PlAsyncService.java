package run.acloud.api.pl.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import run.acloud.api.pl.enums.PlRunMode;
import run.acloud.commons.vo.ExecutingContextVO;


@Slf4j
@Service
public class PlAsyncService {

    @Autowired
    private PlRunBuildService runBuildService;

    @Autowired
    private PlRunDeployService runDeployService;

    @Async
    public void runPl(PlRunMode runMode, Integer plRunSeq, ExecutingContextVO ctx){
        switch(runMode){
            case BUILD:
                runBuildService.runPlBuild(plRunSeq, ctx);
                break;
            case DEPLOY:
                runDeployService.runPlDeploy(plRunSeq, null, ctx);
                break;
            default:
                log.debug("Unknown Run Mode : "+runMode);
        }
    }

}
