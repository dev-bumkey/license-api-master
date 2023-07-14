package run.acloud.api.pipelineflow.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import run.acloud.api.build.service.PipelineBuildValidationService;
import run.acloud.api.configuration.vo.AccountVO;

@Slf4j
@Service
public class PipelineFlowValidationService {

    @Autowired
    private PipelineBuildValidationService pipelineBuildValidationService;

    public void checkUseInternalBuildServer(Integer accountSeq, String buildServerHost) throws Exception {
        pipelineBuildValidationService.checkUseInternalBuildServer(accountSeq, buildServerHost);
    }

    /**
     * 내부 빌드서버 사용여부 체크
     *
     * @param account
     * @param buildServerHost
     * @throws Exception
     */
    public void checkUseInternalBuildServer(AccountVO account, String buildServerHost) throws Exception {
        pipelineBuildValidationService.checkUseInternalBuildServer(account, buildServerHost);
    }

    /**
     * 빌드 서버 상태 체크
     *
     * @param accountSeq
     * @param buildServerHost
     * @throws Exception
     */
    public void chekcBuildServerStatus(Integer accountSeq, String buildServerHost) throws Exception {
        pipelineBuildValidationService.chekcBuildServerStatus(accountSeq, buildServerHost);
    }

}
