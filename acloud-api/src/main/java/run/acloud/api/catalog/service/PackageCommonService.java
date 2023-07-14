package run.acloud.api.catalog.service;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import run.acloud.api.catalog.enums.LaunchType;
import run.acloud.api.catalog.enums.PackageCommandType;
import run.acloud.api.catalog.utils.PackageUtils;
import run.acloud.api.catalog.vo.*;
import run.acloud.api.configuration.dao.IClusterMapper;
import run.acloud.api.configuration.vo.ClusterVO;
import run.acloud.api.cserver.service.ServicemapService;
import run.acloud.api.cserver.util.ServerUtils;
import run.acloud.api.cserver.vo.ServicemapAddVO;
import run.acloud.api.resource.util.ResourceUtil;
import run.acloud.commons.enums.ExecutionResultCode;
import run.acloud.commons.vo.ExecutingContextVO;
import run.acloud.framework.context.ContextHolder;
import run.acloud.framework.enums.ExceptionBiz;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;
import run.acloud.protobuf.chart.Package;

/**
 * @author gun@acornsoft.io
 * Created on 2019. 1. 29.
 */
@Slf4j
@Service
public class PackageCommonService {

    @Resource(name = "cocktailSession")
    private SqlSessionTemplate sqlSession;

    @Autowired
    private HelmService helmService;

    @Autowired
    private PackageInfoService packageInfoService;

    @Autowired
    private PackageValidService packageValidService;

    @Autowired
    private PackageHistoryService packageHistoryService;

    @Autowired
    private ServicemapService servicemapService;

    /**
     * Latest Chart 버전 정보 조회
     * @param repository
     * @param name
     * @return
     * @throws Exception
     */
    public ChartInfoBaseVO getLastestChartVersion(String repository, String name) throws Exception {
        try {

            ChartRequestBaseVO chartRequestBase = new ChartRequestBaseVO();
            chartRequestBase.setRepo(repository);
            chartRequestBase.setName(name);

            Package.ChartVersionsResponse chartVersionsResponse =  helmService.getChartVersions(chartRequestBase);

            if(chartVersionsResponse != null && CollectionUtils.isNotEmpty(chartVersionsResponse.getResultList())) {
                return PackageUtils.convertChartVersion(chartVersionsResponse.getResult(0));
            }
            else {
                return new ChartInfoBaseVO();
            }
        }
        catch (CocktailException ce) {
            if(log.isDebugEnabled()) log.debug("trace log ", ce);
            throw ce; // CocktailException이 발생하면 발생한 예외 처리 케이스를 그대로 사용
        }
        catch (Exception ex) {
            if(log.isDebugEnabled()) log.debug("trace log ", ex);
            throw new CocktailException(ExceptionType.ChartInquireFail.getExceptionPolicy().getMessage(), ex, ExceptionType.ChartInquireFail, ExceptionBiz.PACKAGE_SERVER);
        }
    }


    /**
     * Chart 상세 정보 조회
     * @param repository
     * @param name
     * @param version
     * @return
     * @throws Exception
     */
    public ChartInfoBaseVO getChart(String repository, String name, String version) throws Exception {
        try {

            ChartRequestBaseVO chartRequestBase = new ChartRequestBaseVO();
            chartRequestBase.setRepo(repository);
            chartRequestBase.setName(name);
            chartRequestBase.setVersion(version);

            Package.ChartVersionDetails chartVersionDetails =  helmService.getChartInfo(chartRequestBase);

            return PackageUtils.convertChart(chartVersionDetails);
        }
        catch (CocktailException ce) {
            if(log.isDebugEnabled()) log.debug("trace log ", ce);
            throw ce; // CocktailException이 발생하면 발생한 예외 처리 케이스를 그대로 사용
        }
        catch (Exception ex) {
            if(log.isDebugEnabled()) log.debug("trace log ", ex);
            throw new CocktailException(ExceptionType.ChartInquireFail.getExceptionPolicy().getMessage(), ex, ExceptionType.ChartInquireFail, ExceptionBiz.PACKAGE_SERVER);
        }
    }


    /**
     * UnInstall Package
     * @param clusterSeq
     * @param namespaceName
     * @param releaseName
     * @return
     * @throws Exception
     */
    public HelmReleaseBaseVO unInstallPackage(Integer clusterSeq, String namespaceName, String releaseName) throws Exception {
        try {
            HelmStatusRequestVO helmStatusRequest = new HelmStatusRequestVO();
            helmStatusRequest.setNamespace(namespaceName);
            helmStatusRequest.setReleaseName(releaseName);
            helmStatusRequest.setClusterAccessInfo(packageInfoService.getClusterAccessInfo(clusterSeq));

            Package.HelmUninstallResponse response =  helmService.unInstallPackage(helmStatusRequest);

            HelmReleaseBaseVO helmRelease = PackageUtils.convertRelease(response.getRelease());
            return packageInfoService.fillRelation(helmRelease, clusterSeq, namespaceName);
        }
        catch (CocktailException ce) {
            if(log.isDebugEnabled()) log.debug("trace log ", ce);
            throw ce; // CocktailException이 발생하면 발생한 예외 처리 케이스를 그대로 사용
        }
        catch (Exception ex) {
            if(log.isDebugEnabled()) log.debug("trace log ", ex);
            throw new CocktailException(ExceptionType.PackageUninstallFail.getExceptionPolicy().getMessage(), ex, ExceptionType.PackageUninstallFail, ExceptionBiz.PACKAGE_SERVER);
        }
    }


    /**
     * Install Package
     * @param clusterSeq
     * @param namespaceName
     * @param helmInstallRequest
     * @param context
     * @return
     * @throws Exception
     */
    public HelmReleaseBaseVO installPackage(Integer clusterSeq, String namespaceName, HelmInstallRequestVO helmInstallRequest, ExecutingContextVO context) throws Exception {
        Package.Release release;
        try {
            if (helmInstallRequest == null) {
                throw new CocktailException("helmInstallRequest is empty!!", ExceptionType.InvalidParameter);
            }

            /** Setup Cluster **/
            IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
            ClusterVO cluster = clusterDao.getCluster(clusterSeq);
            cluster.setNamespaceName(namespaceName);

            /** 규칙 체크 */
            ServerUtils.checkServerNameRule(helmInstallRequest.getReleaseName());

            /** 중복 체크 */
            if(packageValidService.isUsingReleaseName(cluster, namespaceName, helmInstallRequest.getReleaseName(), false)) {
                throw new CocktailException("release name already exists", ExceptionType.PackageNameAlreadyExists);
            }

            packageValidService.packageInstallValidation(helmInstallRequest, namespaceName, null);

            /** Install 전 신규이면 appmap 먼저 생성 **/
            if(StringUtils.equals(LaunchType.NEW.getType(), helmInstallRequest.getLaunchType())){
                // Namespace 규격 확인
                if(!ResourceUtil.validNamespaceName(helmInstallRequest.getNamespace())){
                    throw new CocktailException("Invalid namespaceName!!", ExceptionType.NamespaceNameInvalid);
                }
                // ServiceSeq 는 필수 입력.
                if(helmInstallRequest.getServiceSeq() == null || helmInstallRequest.getServiceSeq() < 1) {
                    throw new CocktailException("serviceSeq required", ExceptionType.InvalidParameter);
                }
                // ServicemapGroupSeq 는 필수 입력.
                if(helmInstallRequest.getServicemapGroupSeq() == null || helmInstallRequest.getServicemapGroupSeq() < 1) {
                    throw new CocktailException("servicemapGroupSeq required", ExceptionType.InvalidParameter);
                }
                // ServicemapName은 필수 입력.
                if(StringUtils.isBlank(helmInstallRequest.getServicemapName())){
                    throw new CocktailException("servicemapName required", ExceptionType.AppmapNameInvalid);
                }

                // Servicemap 생성을 위한 데이터 입력
                ServicemapAddVO servicemapAdd = new ServicemapAddVO();
                servicemapAdd.setClusterSeq(clusterSeq);
                servicemapAdd.setNamespaceName(helmInstallRequest.getNamespace());
                servicemapAdd.setServicemapName(helmInstallRequest.getServicemapName());
                servicemapAdd.setServicemapGroupSeq(helmInstallRequest.getServicemapGroupSeq());
                servicemapAdd.setServiceSeq(helmInstallRequest.getServiceSeq());

                // Appmap 생성
                servicemapService.addServicemap(servicemapAdd, ContextHolder.exeContext());
            }

            helmInstallRequest.setClusterAccessInfo(packageInfoService.getClusterAccessInfo(cluster));
            Package.HelmInstallResponse response =  helmService.installPackage(helmInstallRequest);

            release = response.getRelease();
            HelmReleaseBaseVO helmRelease = PackageUtils.convertRelease(release);
            helmRelease = packageInfoService.fillRelation(helmRelease, clusterSeq, namespaceName);

            // 배포 이력 적재
            packageHistoryService.addPackageDeployHistory(PackageCommandType.INSTALL.getCode(), ExecutionResultCode.SUCCESS.getCode(), helmInstallRequest, release, context);

            return helmRelease;
        }
        catch (CocktailException ce) {
            if(log.isDebugEnabled()) log.debug("trace log ", ce);
            throw ce; // CocktailException이 발생하면 발생한 예외 처리 케이스를 그대로 사용
        }
        catch (Exception ex) {
            if(log.isDebugEnabled()) log.debug("trace log ", ex);
            throw new CocktailException(ExceptionType.PackageInstallFail.getExceptionPolicy().getMessage(), ex, ExceptionType.PackageInstallFail, ExceptionBiz.PACKAGE_SERVER);
        }
    }

    /**
     * Upgrade Package
     * @param clusterSeq
     * @param namespaceName
     * @param packageName
     * @param helmUpgradeRequest
     * @return
     * @throws Exception
     */
    public HelmReleaseBaseVO upgradePackage(Integer clusterSeq, String namespaceName, String packageName, HelmInstallRequestVO helmUpgradeRequest) throws Exception {
        Package.Release release;
        try {
            packageValidService.packageInstallValidation(helmUpgradeRequest, namespaceName, null);
            helmUpgradeRequest.setClusterAccessInfo(packageInfoService.getClusterAccessInfo(clusterSeq));

            Package.HelmUpgradeResponse response =  helmService.upgradePackage(helmUpgradeRequest);

            release = response.getRelease();
            HelmReleaseBaseVO helmRelease = PackageUtils.convertRelease(release);
            helmRelease = packageInfoService.fillRelation(helmRelease, clusterSeq, namespaceName);

            // 배포 이력 적재
            packageHistoryService.addPackageDeployHistory(PackageCommandType.UPGRADE.getCode(), ExecutionResultCode.SUCCESS.getCode(), helmUpgradeRequest, release);

            return helmRelease;
        }
        catch (CocktailException ce) {
            if(log.isDebugEnabled()) log.debug("trace log ", ce);
            throw ce; // CocktailException이 발생하면 발생한 예외 처리 케이스를 그대로 사용
        }
        catch (Exception ex) {
            if(log.isDebugEnabled()) log.debug("trace log ", ex);
            throw new CocktailException(ExceptionType.PackageUpgradeFail.getExceptionPolicy().getMessage(), ex, ExceptionType.PackageUpgradeFail, ExceptionBiz.PACKAGE_SERVER);
        }
    }


}
