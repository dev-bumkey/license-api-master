package run.acloud.api.catalog.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import run.acloud.api.catalog.utils.ChartApiGrpcClient;
import run.acloud.api.catalog.vo.ChartRequestBaseVO;
import run.acloud.api.catalog.vo.HelmInstallRequestVO;
import run.acloud.api.catalog.vo.HelmListRequestVO;
import run.acloud.api.catalog.vo.HelmStatusRequestVO;
import run.acloud.api.configuration.vo.ClusterAccessInfoVO;
import run.acloud.commons.util.JsonUtils;
import run.acloud.framework.enums.ExceptionBiz;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;
import run.acloud.framework.properties.CocktailChartProperties;
import run.acloud.protobuf.chart.Package;

import java.util.Optional;

/**
 * @author wschoi@acornsoft.io
 * Created on 2017. 7. 6.
 */
@Slf4j
@Service
public class HelmService {

    @Autowired
    private CocktailChartProperties helmServiceProperties;

    private final boolean printDebug = false;

    private Package.ClusterInfo buildClusterInfo(ClusterAccessInfoVO clusterAccessInfo) {
        Package.ClusterInfo.Builder clusterInfoBuilder = Package.ClusterInfo.newBuilder();

        Optional.ofNullable(clusterAccessInfo.getClusterId()).ifPresent(p -> clusterInfoBuilder.setClusterName(p));
        Optional.ofNullable(clusterAccessInfo.getServerAuthData()).ifPresent(p -> clusterInfoBuilder.setCertificateAuthority(p));
        Optional.ofNullable(clusterAccessInfo.getClientAuthData()).ifPresent(p -> clusterInfoBuilder.setClientCertificate(p));
        Optional.ofNullable(clusterAccessInfo.getClientKeyData()).ifPresent(p -> clusterInfoBuilder.setClientKey(p));
        Optional.ofNullable(clusterAccessInfo.getApiUrl()).ifPresent(p -> clusterInfoBuilder.setEndpoint(p));
        Optional.ofNullable(clusterAccessInfo.getUsername()).ifPresent(p -> clusterInfoBuilder.setUsername(p));
        Optional.ofNullable(clusterAccessInfo.getPassword()).ifPresent(p -> clusterInfoBuilder.setPassword(p));
        Optional.ofNullable(clusterAccessInfo.getToken()).ifPresent(p -> clusterInfoBuilder.setToken(p));

        return clusterInfoBuilder.build();
    }

    /**
     * Chart 버전 목록 조회
     * @param chartRequestBaseVO
     * @return
     * @throws Exception
     */
    public Package.ChartVersionsResponse getChartVersions(ChartRequestBaseVO chartRequestBaseVO) throws Exception {
        ChartApiGrpcClient chartApiGrpcClient = null;
        try {
            Package.ChartVersionsRequest.Builder chartVersionsRequestBuilder = Package.ChartVersionsRequest.newBuilder();
            chartVersionsRequestBuilder.setRepo(chartRequestBaseVO.getRepo());
            chartVersionsRequestBuilder.setName(chartRequestBaseVO.getName());

            Package.ChartVersionsRequest chartVersionsRequest = chartVersionsRequestBuilder.build();

            chartApiGrpcClient = new ChartApiGrpcClient(helmServiceProperties.getChartApiUrl(), helmServiceProperties.getChartGrpcPort());
            if(printDebug) {
                log.debug("###################### getChartVersions : make connection");
                log.debug("###################### getChartVersions : build message : \n{}",
                    JsonUtils.toPrettyString(chartVersionsRequest));
            }

            Package.ChartVersionsResponse chartVersionsResponse = chartApiGrpcClient.chartVersions(chartVersionsRequest);
            if(printDebug) {
                log.debug("###################### getChartVersions : response Message : \n{}",
                    JsonUtils.toGson(chartVersionsResponse));
            }

            return chartVersionsResponse;
        } catch (Exception e) {
            if(log.isDebugEnabled()) log.debug("trace log ", e);
            throw new CocktailException(ExceptionType.ChartInquireFail.getExceptionPolicy().getMessage(), this.getPackageErrorMessage(e), e, ExceptionType.ChartInquireFail, ExceptionBiz.PACKAGE_SERVER);
        } finally {
            if(chartApiGrpcClient != null) chartApiGrpcClient.shutdown();
        }
    }

    /**
     * Chart 정보 조회
     * @param chartRequestBaseVO
     * @return
     * @throws Exception
     */
    public Package.ChartVersionDetails getChartInfo(ChartRequestBaseVO chartRequestBaseVO) throws Exception {
        ChartApiGrpcClient chartApiGrpcClient = null;
        try {
            Package.GetChartVersionRequest.Builder getChartVersionRequestBuilder = Package.GetChartVersionRequest.newBuilder();
            getChartVersionRequestBuilder.setRepo(chartRequestBaseVO.getRepo());
            getChartVersionRequestBuilder.setName(chartRequestBaseVO.getName());
            getChartVersionRequestBuilder.setVersion(chartRequestBaseVO.getVersion());

            Package.GetChartVersionRequest getChartVersionRequest = getChartVersionRequestBuilder.build();

            chartApiGrpcClient = new ChartApiGrpcClient(helmServiceProperties.getChartApiUrl(), helmServiceProperties.getChartGrpcPort());
            if(printDebug) {
                log.debug("###################### getChartInfo : make connection");
                log.debug("###################### getChartInfo : build message : \n{}",
                    JsonUtils.toPrettyString(getChartVersionRequest));
            }

            Package.ChartVersionDetails chartVersionDetails = chartApiGrpcClient.chartVersion(getChartVersionRequest);
            if(printDebug) {
                log.debug("###################### getChartInfo : response Message : \n{}",
                    JsonUtils.toGson(chartVersionDetails));
            }

            return chartVersionDetails;
        } catch (Exception e) {
            if(log.isDebugEnabled()) log.debug("trace log ", e);
            throw new CocktailException(ExceptionType.ChartInquireFail.getExceptionPolicy().getMessage(), this.getPackageErrorMessage(e), e, ExceptionType.ChartInquireFail, ExceptionBiz.PACKAGE_SERVER);
        } finally {
            if(chartApiGrpcClient != null) chartApiGrpcClient.shutdown();
        }
    }

    /**
     * Helm List 조회
     * @param helmListRequestVO
     * @return
     * @throws Exception
     */
    public Package.HelmListResponse getPackages(HelmListRequestVO helmListRequestVO) throws Exception {
        ChartApiGrpcClient chartApiGrpcClient = null;
        try {
            Package.HelmListRequest.Builder helmListRequestBuilder = Package.HelmListRequest.newBuilder();
            helmListRequestBuilder.setClusterInfo(this.buildClusterInfo(helmListRequestVO.getClusterAccessInfo()));
            Optional.ofNullable(helmListRequestVO.getNamespace()).ifPresent(namespace -> helmListRequestBuilder.setNamespace(namespace));
            Optional.ofNullable(helmListRequestVO.getFilter()).ifPresent(filter -> helmListRequestBuilder.setFilter(filter));

            Package.HelmListRequest helmListRequest = helmListRequestBuilder.build();

            chartApiGrpcClient = new ChartApiGrpcClient(helmServiceProperties.getChartApiUrl(), helmServiceProperties.getChartGrpcPort());
            if(printDebug) {
                log.debug("###################### getPackages : make connection");
                log.debug("###################### getPackages : build message : \n{}",
                    JsonUtils.toPrettyString(helmListRequest));
            }

            Package.HelmListResponse helmListResponse = chartApiGrpcClient.helmList(helmListRequest);
            if(printDebug) {
                log.debug("###################### getPackages : response Message : \n{}",
                    JsonUtils.toGson(helmListResponse));
            }

            return helmListResponse;
        } catch (Exception e) {
            if(log.isDebugEnabled()) log.debug("trace log ", e);
            throw new CocktailException(ExceptionType.PackageListInquireFail.getExceptionPolicy().getMessage(), this.getPackageErrorMessage(e), e, ExceptionType.PackageListInquireFail, ExceptionBiz.PACKAGE_SERVER);
        } finally {
            if(chartApiGrpcClient != null) chartApiGrpcClient.shutdown();
        }
    }

    /**
     * Helm 패키지 Status 조회
     * @param helmStatusRequestVO
     * @return
     * @throws Exception
     */
    public Package.HelmStatusResponse getPackageStatus(HelmStatusRequestVO helmStatusRequestVO) throws Exception {
        ChartApiGrpcClient chartApiGrpcClient = null;
        try {
            Package.HelmStatusRequest.Builder helmStatusRequestBuilder = Package.HelmStatusRequest.newBuilder();
            helmStatusRequestBuilder.setClusterInfo(this.buildClusterInfo(helmStatusRequestVO.getClusterAccessInfo()));
            Optional.ofNullable(helmStatusRequestVO.getNamespace()).ifPresent(namespace -> helmStatusRequestBuilder.setNamespace(namespace));
            Optional.ofNullable(helmStatusRequestVO.getReleaseName()).ifPresent(releaseName -> helmStatusRequestBuilder.setReleaseName(releaseName));

            Package.HelmStatusRequest helmStatusRequest = helmStatusRequestBuilder.build();

            chartApiGrpcClient = new ChartApiGrpcClient(helmServiceProperties.getChartApiUrl(), helmServiceProperties.getChartGrpcPort());
            if(printDebug) {
                log.debug("###################### getPackageStatus : make connection");
                log.debug("###################### getPackageStatus : build message : \n{}",
                    JsonUtils.toPrettyString(helmStatusRequest));
            }

            Package.HelmStatusResponse helmStatusResponse = chartApiGrpcClient.helmStatus(helmStatusRequest);
            if(printDebug) {
                log.debug("###################### getPackageStatus : response Message : \n{}",
                    JsonUtils.toGson(helmStatusResponse));
            }

            return helmStatusResponse;
        } catch (Exception e) {
            if(log.isDebugEnabled()) log.debug("trace log ", e);
            throw new CocktailException(ExceptionType.PackageStatusInquireFail.getExceptionPolicy().getMessage(), this.getPackageErrorMessage(e), e, ExceptionType.PackageStatusInquireFail, ExceptionBiz.PACKAGE_SERVER);
        } finally {
            if(chartApiGrpcClient != null) chartApiGrpcClient.shutdown();
        }
    }

    /**
     * Helm 패키지 Revision 목록 조회
     * @param helmStatusRequestVO
     * @return
     * @throws Exception
     */
    public Package.HelmHistoryResponse getPackageRevisions(HelmStatusRequestVO helmStatusRequestVO) throws Exception {
        ChartApiGrpcClient chartApiGrpcClient = null;
        try {
            Package.HelmHistoryRequest.Builder helmHistoryRequestBuilder = Package.HelmHistoryRequest.newBuilder();
            helmHistoryRequestBuilder.setClusterInfo(this.buildClusterInfo(helmStatusRequestVO.getClusterAccessInfo()));
            Optional.ofNullable(helmStatusRequestVO.getNamespace()).ifPresent(namespace -> helmHistoryRequestBuilder.setNamespace(namespace));
            Optional.ofNullable(helmStatusRequestVO.getReleaseName()).ifPresent(releaseName -> helmHistoryRequestBuilder.setReleaseName(releaseName));

            Package.HelmHistoryRequest helmHistoryRequest = helmHistoryRequestBuilder.build();

            chartApiGrpcClient = new ChartApiGrpcClient(helmServiceProperties.getChartApiUrl(), helmServiceProperties.getChartGrpcPort());
            if(printDebug) {
                log.debug("###################### getPackageRevisions : make connection");
                log.debug("###################### getPackageRevisions : build message : \n{}",
                    JsonUtils.toPrettyString(helmHistoryRequest));
            }

            Package.HelmHistoryResponse helmHistoryResponse = chartApiGrpcClient.helmHistory(helmHistoryRequest);
            if(printDebug) {
                log.debug("###################### getPackageRevisions : response Message : \n{}",
                    JsonUtils.toGson(helmHistoryResponse));
            }

            return helmHistoryResponse;
        } catch (Exception e) {
            if(log.isDebugEnabled()) log.debug("trace log ", e);
            throw new CocktailException(ExceptionType.PackageHistoryInquireFail.getExceptionPolicy().getMessage(), this.getPackageErrorMessage(e), e, ExceptionType.PackageHistoryInquireFail, ExceptionBiz.PACKAGE_SERVER);
        } finally {
            if(chartApiGrpcClient != null) chartApiGrpcClient.shutdown();
        }
    }

    /**
     * Helm 패키지 Install
     * @param helmInstallRequestVO
     * @return
     * @throws Exception
     */
    public Package.HelmInstallResponse installPackage(HelmInstallRequestVO helmInstallRequestVO) throws Exception {
        ChartApiGrpcClient chartApiGrpcClient = null;
        try {
            Package.HelmInstallRequest.Builder helmInstallRequestBuilder = Package.HelmInstallRequest.newBuilder();
            helmInstallRequestBuilder.setClusterInfo(this.buildClusterInfo(helmInstallRequestVO.getClusterAccessInfo()));
            Optional.ofNullable(helmInstallRequestVO.getRepo()).ifPresent(p -> helmInstallRequestBuilder.setRepo(p));
            Optional.ofNullable(helmInstallRequestVO.getChartName()).ifPresent(p -> helmInstallRequestBuilder.setName(p));
            Optional.ofNullable(helmInstallRequestVO.getVersion()).ifPresent(p -> helmInstallRequestBuilder.setVersion(p));
            Optional.ofNullable(helmInstallRequestVO.getNamespace()).ifPresent(p -> helmInstallRequestBuilder.setNamespace(p));
            Optional.ofNullable(helmInstallRequestVO.getReleaseName()).ifPresent(p -> helmInstallRequestBuilder.setReleaseName(p));
            Optional.ofNullable(helmInstallRequestVO.getValues()).ifPresent(p -> helmInstallRequestBuilder.setValues(p));

            Package.HelmInstallRequest helmInstallRequest = helmInstallRequestBuilder.build();

            chartApiGrpcClient = new ChartApiGrpcClient(helmServiceProperties.getChartApiUrl(), helmServiceProperties.getChartGrpcPort());
            if(printDebug) {
                log.debug("###################### installPackage : make connection");
                log.debug("###################### installPackage : build message : \n{}",
                    JsonUtils.toPrettyString(helmInstallRequest));
            }

            Package.HelmInstallResponse helmInstallResponse = chartApiGrpcClient.helmInstall(helmInstallRequest);
            if(printDebug) {
                log.debug("###################### installPackage : response Message : \n{}",
                    JsonUtils.toGson(helmInstallResponse));
            }

            return helmInstallResponse;
        } catch (Exception e) {
            if(log.isDebugEnabled()) log.debug("trace log ", e);
            throw new CocktailException(ExceptionType.PackageInstallFail.getExceptionPolicy().getMessage(), this.getPackageErrorMessage(e), e, ExceptionType.PackageInstallFail, ExceptionBiz.PACKAGE_SERVER);
        } finally {
            if(chartApiGrpcClient != null) chartApiGrpcClient.shutdown();
        }
    }

    /**
     * Helm 패키지 Upgrade
     * @param helmInstallRequestVO
     * @return
     * @throws Exception
     */
    public Package.HelmUpgradeResponse upgradePackage(HelmInstallRequestVO helmInstallRequestVO) throws Exception {
        ChartApiGrpcClient chartApiGrpcClient = null;
        try {
            Package.HelmUpgradeRequest.Builder helmUpgradeRequestBuilder = Package.HelmUpgradeRequest.newBuilder();
            helmUpgradeRequestBuilder.setClusterInfo(this.buildClusterInfo(helmInstallRequestVO.getClusterAccessInfo()));
            Optional.ofNullable(helmInstallRequestVO.getRepo()).ifPresent(p -> helmUpgradeRequestBuilder.setRepo(p));
            Optional.ofNullable(helmInstallRequestVO.getChartName()).ifPresent(p -> helmUpgradeRequestBuilder.setName(p));
            Optional.ofNullable(helmInstallRequestVO.getVersion()).ifPresent(p -> helmUpgradeRequestBuilder.setVersion(p));
            Optional.ofNullable(helmInstallRequestVO.getNamespace()).ifPresent(p -> helmUpgradeRequestBuilder.setNamespace(p));
            Optional.ofNullable(helmInstallRequestVO.getReleaseName()).ifPresent(p -> helmUpgradeRequestBuilder.setReleaseName(p));
            Optional.ofNullable(helmInstallRequestVO.getValues()).ifPresent(p -> helmUpgradeRequestBuilder.setValues(p));

            Package.HelmUpgradeRequest helmUpgradeRequest = helmUpgradeRequestBuilder.build();

            chartApiGrpcClient = new ChartApiGrpcClient(helmServiceProperties.getChartApiUrl(), helmServiceProperties.getChartGrpcPort());
            if(printDebug) {
                log.debug("###################### upgradePackage : make connection");
                log.debug("###################### upgradePackage : build message : \n{}",
                    JsonUtils.toPrettyString(helmUpgradeRequest));
            }

            Package.HelmUpgradeResponse helmUpgradeResponse = chartApiGrpcClient.helmUpgrade(helmUpgradeRequest);
            if(printDebug) {
                log.debug("###################### upgradePackage : response Message : \n{}",
                    JsonUtils.toGson(helmUpgradeResponse));
            }

            return helmUpgradeResponse;
        } catch (Exception e) {
            if(log.isDebugEnabled()) log.debug("trace log ", e);
            throw new CocktailException(ExceptionType.PackageUpgradeFail.getExceptionPolicy().getMessage(), this.getPackageErrorMessage(e), e, ExceptionType.PackageUpgradeFail, ExceptionBiz.PACKAGE_SERVER);
        } finally {
            if(chartApiGrpcClient != null) chartApiGrpcClient.shutdown();
        }
    }

    /**
     * Helm 패키지 UnInstall
     * @param helmStatusRequestVO
     * @return
     * @throws Exception
     */
    public Package.HelmUninstallResponse unInstallPackage(HelmStatusRequestVO helmStatusRequestVO) throws Exception {
        ChartApiGrpcClient chartApiGrpcClient = null;
        try {
            Package.HelmUninstallRequest.Builder helmUninstallRequestBuilder = Package.HelmUninstallRequest.newBuilder();
            helmUninstallRequestBuilder.setClusterInfo(this.buildClusterInfo(helmStatusRequestVO.getClusterAccessInfo()));
            Optional.ofNullable(helmStatusRequestVO.getNamespace()).ifPresent(namespace -> helmUninstallRequestBuilder.setNamespace(namespace));
            Optional.ofNullable(helmStatusRequestVO.getReleaseName()).ifPresent(releaseName -> helmUninstallRequestBuilder.setReleaseName(releaseName));

            Package.HelmUninstallRequest helmUninstallRequest = helmUninstallRequestBuilder.build();

            chartApiGrpcClient = new ChartApiGrpcClient(helmServiceProperties.getChartApiUrl(), helmServiceProperties.getChartGrpcPort());
            if(printDebug) {
                log.debug("###################### unInstallPackage : make connection");
                log.debug("###################### unInstallPackage : build message : \n{}",
                    JsonUtils.toPrettyString(helmUninstallRequest));
            }

            Package.HelmUninstallResponse helmUninstallResponse = chartApiGrpcClient.helmUninstall(helmUninstallRequest);
            if(printDebug) {
                log.debug("###################### unInstallPackage : response Message : \n{}",
                    JsonUtils.toGson(helmUninstallResponse));
            }

            return helmUninstallResponse;
        } catch (Exception e) {
            if(log.isDebugEnabled()) log.debug("trace log ", e);
            throw new CocktailException(ExceptionType.PackageUninstallFail.getExceptionPolicy().getMessage(), this.getPackageErrorMessage(e), e, ExceptionType.PackageUninstallFail, ExceptionBiz.PACKAGE_SERVER);
        } finally {
            if(chartApiGrpcClient != null) chartApiGrpcClient.shutdown();
        }
    }

    /**
     * Helm 패키지 Rollback
     * @param helmStatusRequestVO
     * @return
     * @throws Exception
     */
    public Package.HelmRollbackResponse rollbackPackage(HelmStatusRequestVO helmStatusRequestVO) throws Exception {
        ChartApiGrpcClient chartApiGrpcClient = null;
        try {
            Package.HelmRollbackRequest.Builder helmRollbackRequestBuilder = Package.HelmRollbackRequest.newBuilder();
            helmRollbackRequestBuilder.setClusterInfo(this.buildClusterInfo(helmStatusRequestVO.getClusterAccessInfo()));
            Optional.ofNullable(helmStatusRequestVO.getNamespace()).ifPresent(namespace -> helmRollbackRequestBuilder.setNamespace(namespace));
            Optional.ofNullable(helmStatusRequestVO.getReleaseName()).ifPresent(releaseName -> helmRollbackRequestBuilder.setReleaseName(releaseName));
            Optional.ofNullable(helmStatusRequestVO.getRevision()).ifPresent(revision -> helmRollbackRequestBuilder.setRevision(revision));

            Package.HelmRollbackRequest helmRollbackRequest = helmRollbackRequestBuilder.build();

            chartApiGrpcClient = new ChartApiGrpcClient(helmServiceProperties.getChartApiUrl(), helmServiceProperties.getChartGrpcPort());
            if(printDebug) {
                log.debug("###################### rollbackPackage : make connection");
                log.debug("###################### rollbackPackage : build message : \n{}",
                    JsonUtils.toPrettyString(helmRollbackRequest));
            }

            Package.HelmRollbackResponse helmRollbackResponse = chartApiGrpcClient.helmRollback(helmRollbackRequest);
            if(printDebug) {
                log.debug("###################### rollbackPackage : response Message : \n{}",
                    JsonUtils.toGson(helmRollbackResponse));
            }

            return helmRollbackResponse;
        } catch (Exception e) {
            if(log.isDebugEnabled()) log.debug("trace log ", e);
            throw new CocktailException(ExceptionType.PackageRollbackFail.getExceptionPolicy().getMessage(), this.getPackageErrorMessage(e), e, ExceptionType.PackageRollbackFail, ExceptionBiz.PACKAGE_SERVER);
        } finally {
            if(chartApiGrpcClient != null) chartApiGrpcClient.shutdown();
        }
    }

    /**
     * Package 서버에서 발생한 Exception으로 부터 메시지 추출..
     * @param e
     * @return
     * @throws Exception
     */
    private String getPackageErrorMessage(Exception e) throws Exception {
        String separator = "[CAUSE] ";
        String message = e.getMessage();
        try {
            int startIndex = message.lastIndexOf(separator);

            if (startIndex < 0) {
                return message;
            }
            else {
                return message.substring(startIndex + separator.length());
            }
        }
        catch(Exception ex) {
            try {
                log.error("Can't Generate Error Message : " + JsonUtils.toGson(e));
            }
            catch(Exception ex2) {
                log.error("Can't Generate Error Message : ");
            }
            return "None";
        }
    }
}