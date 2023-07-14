package run.acloud.api.log.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import run.acloud.api.catalog.enums.LaunchType;
import run.acloud.api.catalog.service.PackageCommonService;
import run.acloud.api.catalog.service.PackageK8sService;
import run.acloud.api.catalog.vo.HelmInstallRequestVO;
import run.acloud.api.catalog.vo.HelmReleaseBaseVO;
import run.acloud.api.catalog.vo.K8sObjectMapVO;
import run.acloud.api.log.vo.LogAgentVO;
import run.acloud.api.resource.enums.K8sApiKindType;
import run.acloud.framework.context.ContextHolder;
import run.acloud.framework.properties.CocktailLogAgentProperties;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class LogAgentDeployService {
    private final CocktailLogAgentProperties cocktailLogAgentProperties;
    private final PackageCommonService packageCommonService;
    private final PackageK8sService packageK8sService;

    public LogAgentDeployService(CocktailLogAgentProperties cocktailLogAgentProperties, PackageCommonService packageCommonService, PackageK8sService packageK8sService) {
        this.cocktailLogAgentProperties = cocktailLogAgentProperties;
        this.packageCommonService = packageCommonService;
        this.packageK8sService = packageK8sService;
    }

    public void installLogAgent(LogAgentVO logAgent) throws Exception {
        // helmInstallRequest 객체에 로그 에이전트 차트 정보를 주입하고 차트 배포 작업
        HelmInstallRequestVO helmInstallRequest = getHelmInstallRequest(logAgent);
        HelmReleaseBaseVO result = packageCommonService.installPackage(logAgent.getClusterSeq(), logAgent.getNamespace(), helmInstallRequest, ContextHolder.exeContext());
        // 배포 작업 후 컨트롤러 이름을 조회해서 logAgent에 설정한다.
        List<K8sObjectMapVO> list = packageK8sService.getK8sObjectList(result.getManifest(), null);
        Optional<K8sObjectMapVO> k8sObject = list.stream().filter(k8sObjectMapVO -> k8sObjectMapVO.getK8sApiKindType() == K8sApiKindType.DAEMON_SET).findFirst();
        k8sObject.ifPresent(k8sObjectMapVO -> logAgent.setControllerName(k8sObjectMapVO.getName()));
    }

    public void updateLogAgent(LogAgentVO logAgent) throws Exception {
        HelmInstallRequestVO helmInstallRequest = getHelmInstallRequest(logAgent);
        packageCommonService.upgradePackage(logAgent.getClusterSeq(), logAgent.getNamespace(), helmInstallRequest.getReleaseName(), helmInstallRequest);
    }

    public void deleteLogAgent(LogAgentVO logAgent) throws Exception {
        packageCommonService.unInstallPackage(logAgent.getClusterSeq(), logAgent.getNamespace(), logAgent.getAgentName());
    }

    private HelmInstallRequestVO getHelmInstallRequest(LogAgentVO logAgent) {
        HelmInstallRequestVO helmInstallRequest = new HelmInstallRequestVO();
        helmInstallRequest.setRepo(cocktailLogAgentProperties.getLogAgentChartRepo());
        helmInstallRequest.setChartName(cocktailLogAgentProperties.getLogAgentChartName());
        helmInstallRequest.setNamespace(logAgent.getNamespace());
        helmInstallRequest.setReleaseName(logAgent.getAgentName());
        helmInstallRequest.setValues(logAgent.getDeployConfig());
        helmInstallRequest.setLaunchType(LaunchType.ADD.getType());
        helmInstallRequest.setVersion(cocktailLogAgentProperties.getLogAgentChartVersion());
        return helmInstallRequest;
    }
}
