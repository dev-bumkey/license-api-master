package run.acloud.api.monitoring.controller;

import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import run.acloud.api.configuration.enums.ClusterState;
import run.acloud.api.configuration.service.ClusterService;
import run.acloud.api.configuration.vo.ClusterVO;
import run.acloud.api.monitoring.vo.ClusterMonitoringVO;
import run.acloud.api.resource.task.K8sTokenGenerator;
import run.acloud.commons.util.CryptoUtils;
import run.acloud.commons.vo.ExecutingContextVO;
import run.acloud.commons.vo.ResultVO;
import run.acloud.framework.context.ContextHolder;

import java.util.List;

@Slf4j
@RestController
@RequestMapping(value = "/api/monitoring")
public class MonitoringController {

    @Autowired
    private ClusterService clusterService;

    @Autowired
    private K8sTokenGenerator k8sTokenGenerator;

    @Operation(summary = "모니터링 API가 사용하는 기본 정보 반환.")
    @GetMapping(value = "/v2/info")
    public ResultVO getMonitoringInfo(
            @RequestParam(name = "includeCluster", required = false, defaultValue = "false") boolean includeCluster,
            @RequestParam(name = "includeStopped", required = false, defaultValue = "false") boolean includeStopped
    ) throws Exception {
        ResultVO result = new ResultVO();

        ClusterMonitoringVO info = new ClusterMonitoringVO();
        // Admin 기본사용자 셋팅
        ExecutingContextVO context = new ExecutingContextVO();
        context.setUserSeq(1);
        ContextHolder.exeContext(context);

        if (includeCluster) {
            List<ClusterVO> clusters = clusterService.getClusters();
            clusters.removeIf(p -> (p.getUseYn().equals("N") ||
                    (!includeStopped && !StringUtils.equalsIgnoreCase(p.getClusterState(), ClusterState.RUNNING.getCode()))));
            for (ClusterVO cluster : clusters) {
                cluster.setProviderAccount(null);

                k8sTokenGenerator.refreshClusterToken(cluster);
                cluster.setApiSecret(CryptoUtils.decryptAES(cluster.getApiSecret()));
                cluster.setClientAuthData(CryptoUtils.decryptAES(cluster.getClientAuthData()));
                cluster.setClientKeyData(CryptoUtils.decryptAES(cluster.getClientKeyData()));
                cluster.setServerAuthData(CryptoUtils.decryptAES(cluster.getServerAuthData()));
            }
            info.setClusters(clusters);
        }

        result.setResult(info);

        return result;
    }
}
