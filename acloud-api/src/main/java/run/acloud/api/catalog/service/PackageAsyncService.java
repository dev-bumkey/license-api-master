package run.acloud.api.catalog.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import run.acloud.api.catalog.enums.LaunchType;
import run.acloud.api.catalog.vo.HelmInstallRequestVO;
import run.acloud.api.catalog.vo.HelmReleaseBaseVO;
import run.acloud.commons.constants.CommonConstants;
import run.acloud.commons.util.JsonUtils;
import run.acloud.commons.vo.ExecutingContextVO;
import run.acloud.framework.exception.CocktailException;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class PackageAsyncService {

    @Autowired
    private PackageCommonService packageCommonService;

    @Async
    public List<HelmReleaseBaseVO> installMultiplePackage(Integer clusterSeq, Integer serviceSeq, String servicemapName, String namespaceName, ExecutingContextVO context, List<HelmInstallRequestVO> helmInstallRequestList) throws Exception {
        /**
         * 워크로드 처리가 끝나면 시작할 수 있도록 대기함...
         * 워크로드 / 패키지 동시 처리시 Context 공유로 문제가 발생하여 순차 처리 될 수 있도록 함...
         **/
        int sleepTime = 5000;
        int maxNum = 60; // 5분이 지나도 처리가 완료되지 않으면 통과 하도록 함.. (5초x60번)
        for(int i = 1; i < maxNum; i++) {
            Boolean isFinished = MapUtils.getBoolean(context.getParams(), CommonConstants.AUDIT_WORKLOAD_PROCESSING_FINISHED, Boolean.FALSE);
            if(isFinished.booleanValue()) {
                log.debug("Starting installMultiplePackage Processing ...");
                break;
            }
            log.debug("@@@ Waiting for Workload Installation to finish processing ... ");
            Thread.sleep(sleepTime);
        }

        List<HelmReleaseBaseVO> helmReleaseBaseList = new ArrayList<>();
        for (HelmInstallRequestVO helmInstallRequest : helmInstallRequestList) {
            try {
                helmInstallRequest.setLaunchType(LaunchType.ADD.getType());
                helmInstallRequest.setServicemapName(servicemapName);
                helmInstallRequest.setServiceSeq(serviceSeq);
                HelmReleaseBaseVO helmReleaseBase = packageCommonService.installPackage(clusterSeq, namespaceName, helmInstallRequest, context);
                helmReleaseBaseList.add(helmReleaseBase);
            }
            catch (CocktailException ce) {
                // 실패시 Log를 남기고 다음 처리를 계속한다..
                log.error(String.format("Package Install Failure : deployTemplate : %s\n%s", ce.getMessage(), JsonUtils.toGson(helmInstallRequest)));
                continue;
            }
            catch(Exception ex) {
                // 실패시 Log를 남기고 다음 처리를 계속한다..
                log.error(String.format("Package Install Failure : deployTemplate : %s\n%s", ex.getMessage(), JsonUtils.toGson(helmInstallRequest)));
                continue;
            }
        }

        return helmReleaseBaseList;
    }
}
