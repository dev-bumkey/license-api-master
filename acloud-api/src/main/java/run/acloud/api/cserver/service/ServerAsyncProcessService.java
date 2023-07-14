package run.acloud.api.cserver.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import run.acloud.api.cserver.enums.DeployType;
import run.acloud.api.cserver.enums.DeploymentState;
import run.acloud.api.cserver.vo.ServerGuiVO;
import run.acloud.api.cserver.vo.ServerIntegrateVO;
import run.acloud.api.cserver.vo.ServerYamlVO;
import run.acloud.commons.vo.ExecutingContextVO;

@Slf4j
@Service
public class ServerAsyncProcessService {

    @Autowired
    private ServerProcessService serverProcessService;

    public void serverProcessSync(DeploymentState deploymentState, ServerIntegrateVO serverIntegrate, ExecutingContextVO context) throws Exception {
        this.executeServerProcess(deploymentState, serverIntegrate, context);
    }

    @Async
    public void serverProcessAsync(DeploymentState deploymentState, ServerIntegrateVO serverIntegrate, ExecutingContextVO context) throws Exception {
        this.executeServerProcess(deploymentState, serverIntegrate, context);
    }

    private void executeServerProcess(DeploymentState deploymentState, ServerIntegrateVO serverIntegrate, ExecutingContextVO context) throws Exception {

        switch (deploymentState) {
            case CREATED:
                switch (DeployType.valueOf(serverIntegrate.getDeployType())) {
                    case GUI:
                        serverProcessService.addServerProcess((ServerGuiVO)serverIntegrate, context);
                        break;
                    case YAML:
                        serverProcessService.redeployServerProcess((ServerYamlVO)serverIntegrate, context);
                        break;
                }

                break;
            case EDITED:
                if (BooleanUtils.toBoolean(context.getPipelineYn())) {
                    serverProcessService.replaceServerProcessForPipeline((ServerYamlVO)serverIntegrate, context);
                } else {
                    serverProcessService.udpateServerProcess((ServerYamlVO)serverIntegrate, context);
                }
                break;
            case RECREATED:
                serverProcessService.redeployServerProcess((ServerYamlVO)serverIntegrate, context);
                break;
            case TERMINATED:
                serverProcessService.terminateServerProcess((ServerYamlVO)serverIntegrate, context);
                break;
        }
    }
}
