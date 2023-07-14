package run.acloud.api.catalog.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import run.acloud.api.catalog.enums.PackageCommandType;
import run.acloud.api.catalog.utils.PackageUtils;
import run.acloud.api.catalog.vo.*;
import run.acloud.api.configuration.vo.ClusterVO;
import run.acloud.api.k8sextended.util.Yaml;
import run.acloud.commons.enums.ExecutionResultCode;
import run.acloud.commons.util.JsonUtils;
import run.acloud.framework.enums.ExceptionBiz;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;
import run.acloud.protobuf.chart.Package;

import java.util.*;

/**
 * @author gun@acornsoft.io
 * Created on 2019. 1. 29.
 */
@Slf4j
@Service
public class PackageService {

    @Autowired
    private HelmService helmService;

    @Autowired
    private PackageInfoService packageInfoService;

    @Autowired
    private PackageHistoryService packageHistoryService;

    @Autowired
    private PackageK8sService packageK8sService;


    /**
     * Print Cluster Access Info
     * @param clusterSeq
     * @throws Exception
     */
    public void printClusterAccessInfo(Integer clusterSeq) throws Exception {
        log.debug(JsonUtils.toPrettyString(packageInfoService.getClusterAccessInfo(clusterSeq)));

    }

    /**
     * Chart 버전 정보 조회
     * @param repository
     * @param name
     * @return
     * @throws Exception
     */
    public List<ChartInfoBaseVO> getChartVersions(String repository, String name) throws Exception {
        try {

            ChartRequestBaseVO chartRequestBase = new ChartRequestBaseVO();
            chartRequestBase.setRepo(repository);
            chartRequestBase.setName(name);

            Package.ChartVersionsResponse chartVersionsResponse =  helmService.getChartVersions(chartRequestBase);

            return PackageUtils.convertChartVersions(chartVersionsResponse);
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
     * Package Status(개별) 조회
     * @param clusterSeq
     * @param namespaceName
     * @param releaseName
     * @param revision
     * @return
     * @throws Exception
     */
    public HelmReleaseBaseVO getPackageStatus(Integer clusterSeq, String namespaceName, String releaseName, String revision) throws Exception {
        try {
            HelmStatusRequestVO helmStatusRequest = new HelmStatusRequestVO();
            helmStatusRequest.setNamespace(namespaceName);
            helmStatusRequest.setReleaseName(releaseName);
            helmStatusRequest.setRevision(revision);
            helmStatusRequest.setClusterAccessInfo(packageInfoService.getClusterAccessInfo(clusterSeq));

            Package.HelmStatusResponse response =  helmService.getPackageStatus(helmStatusRequest);

            HelmReleaseBaseVO helmRelease = PackageUtils.convertRelease(response.getRelease());
            helmRelease = packageInfoService.fillRelation(helmRelease, clusterSeq, namespaceName);

            PackageDeployHistoryVO history = packageHistoryService.getPackageDeployHistory(clusterSeq, namespaceName, releaseName, helmRelease.getChartName(),
                helmRelease.getChartVersion(), helmRelease.getRevision(), null);

            /** Values 정보 설정 **/
            if(history != null) {
                // 이력 데이터가 존재하면 이력의 values Yaml 파일을 이용하여 이력 적재
                if(log.isDebugEnabled()) log.debug("========================== YAML (found history) ===========================\n" + JsonUtils.toPrettyString(history));
                helmRelease.setValues(history.getChartValues());
                helmRelease.setRepo(history.getRepository());
            }
            else {
                // 이력 데이터가 존재하지 않으면, Package 배포 정보의 데이터를 Yaml로 컨버트 하여 이력 적재
                Map<String, Object> jsonMap = JsonUtils.fromGson(helmRelease.getChart(), HashMap.class);
                String yamlStr = Yaml.getSnakeYaml().dump(jsonMap.get("values"));
                if(log.isDebugEnabled()) log.debug("========================== YAML (not found history) ===========================\n" + yamlStr);
                helmRelease.setValues(yamlStr);
            }

            helmRelease.setChart(null);
            return helmRelease;
        }
        catch (CocktailException ce) {
            if(log.isDebugEnabled()) log.debug("trace log ", ce);
            throw ce; // CocktailException이 발생하면 발생한 예외 처리 케이스를 그대로 사용
        }
        catch (Exception ex) {
            if(log.isDebugEnabled()) log.debug("trace log ", ex);
            throw new CocktailException(ExceptionType.PackageStatusInquireFail.getExceptionPolicy().getMessage(), ex, ExceptionType.PackageStatusInquireFail, ExceptionBiz.PACKAGE_SERVER);
        }
    }

    /**
     * Package Revision List 조회
     * @param clusterSeq
     * @param namespaceName
     * @param releaseName
     * @return
     * @throws Exception
     */
    public List<HelmReleaseInfoVO> getPackageRevisions(Integer clusterSeq, String namespaceName, String releaseName) throws Exception {
        try {
            HelmStatusRequestVO helmStatusRequest = new HelmStatusRequestVO();
            helmStatusRequest.setNamespace(namespaceName);
            helmStatusRequest.setReleaseName(releaseName);
            helmStatusRequest.setClusterAccessInfo(packageInfoService.getClusterAccessInfo(clusterSeq));

            Package.HelmHistoryResponse response =  helmService.getPackageRevisions(helmStatusRequest);

            List<HelmReleaseInfoVO> helmReleaseInfoList = new ArrayList<>();
            for (Package.ReleaseInfo releaseInfo : response.getReleaseInfosList()) {
                HelmReleaseInfoVO helmReleaseInfo = new HelmReleaseInfoVO();
                Optional.ofNullable(releaseInfo.getRevision()).ifPresent(rl -> helmReleaseInfo.setRevision(rl));
                Optional.ofNullable(releaseInfo.getUpdated()).ifPresent(rl -> helmReleaseInfo.setUpdated(rl));
                Optional.ofNullable(releaseInfo.getChart()).ifPresent(rl -> helmReleaseInfo.setChart(rl));
                Optional.ofNullable(releaseInfo.getStatus()).ifPresent(rl -> helmReleaseInfo.setStatus(rl));
                Optional.ofNullable(releaseInfo.getAppVersion()).ifPresent(rl -> helmReleaseInfo.setAppVersion(rl));
                Optional.ofNullable(releaseInfo.getDescription()).ifPresent(rl -> helmReleaseInfo.setDescription(rl));

                helmReleaseInfoList.add(helmReleaseInfo);
            }

            try {
                helmReleaseInfoList.sort(Comparator.comparingInt(s -> Integer.parseInt(s.getRevision())));
                Collections.reverse(helmReleaseInfoList);
            }
            catch (Exception ex) {
                log.error("1. Can't Sort Revision. retry with string type. : PackageService.getPackageRevisions");
                try {
                    helmReleaseInfoList.sort(Comparator.comparing(s -> s.getRevision()));
                    Collections.reverse(helmReleaseInfoList);
                }
                catch (Exception ex2) {
                    log.error("2. Can't Sort Revision : PackageService.getPackageRevisions");
                }
            }

            return helmReleaseInfoList;
        }
        catch (CocktailException ce) {
            if(log.isDebugEnabled()) log.debug("trace log ", ce);
            throw ce; // CocktailException이 발생하면 발생한 예외 처리 케이스를 그대로 사용
        }
        catch (Exception ex) {
            if(log.isDebugEnabled()) log.debug("trace log ", ex);
            throw new CocktailException(ExceptionType.PackageHistoryInquireFail.getExceptionPolicy().getMessage(), ex, ExceptionType.PackageHistoryInquireFail, ExceptionBiz.PACKAGE_SERVER);
        }
    }

    /**
     * Rollback Package
     * @param clusterSeq
     * @param namespaceName
     * @param releaseName
     * @param revision
     * @return
     * @throws Exception
     */
    public HelmReleaseBaseVO rollbackPackage(Integer clusterSeq, String namespaceName, String releaseName, String revision) throws Exception {
        try {
            HelmStatusRequestVO helmStatusRequest = new HelmStatusRequestVO();
            helmStatusRequest.setNamespace(namespaceName);
            helmStatusRequest.setReleaseName(releaseName);
            helmStatusRequest.setRevision(revision);
            helmStatusRequest.setClusterAccessInfo(packageInfoService.getClusterAccessInfo(clusterSeq));

            Package.HelmRollbackResponse response =  helmService.rollbackPackage(helmStatusRequest);

            // Status 조회
            HelmReleaseBaseVO helmReleaseBase = this.getPackageStatus(clusterSeq, namespaceName, releaseName, null);
            if(helmReleaseBase == null) {
                if(log.isDebugEnabled()) {
                    throw new CocktailException("Fail to get package Resource", ExceptionType.PackageResourceInquireFail);
                }
                else {
                    log.error("Rollback succeeded but failed to retrieve data.\n" + JsonUtils.toPrettyString(helmStatusRequest));
                }
            } else {
                // 배포 이력 적재
                packageHistoryService.addPackageRollbackHistory(PackageCommandType.ROLLBACK.getCode(), ExecutionResultCode.SUCCESS.getCode(), clusterSeq, helmStatusRequest, helmReleaseBase);
            }

            return helmReleaseBase;
        }
        catch (CocktailException ce) {
            if(log.isDebugEnabled()) log.debug("trace log ", ce);
            throw ce; // CocktailException이 발생하면 발생한 예외 처리 케이스를 그대로 사용
        }
        catch (Exception ex) {
            if(log.isDebugEnabled()) log.debug("trace log ", ex);
            throw new CocktailException(ExceptionType.PackageRollbackFail.getExceptionPolicy().getMessage(), ex, ExceptionType.PackageRollbackFail, ExceptionBiz.PACKAGE_SERVER);
        }
    }

    /**
     * Package 상세 정보 조회 (include Resources)
     * @param clusterSeq
     * @param namespaceName
     * @param releaseName
     * @return
     * @throws Exception
     */
    public HelmResourcesVO getPackageResources(Integer clusterSeq, String namespaceName, String releaseName) throws Exception {
        try {
            /**
             * Cluster 유효성 체크
             * **/
            ClusterVO cluster = packageInfoService.setupCluster(clusterSeq, namespaceName);
            if(cluster == null) {
                throw new CocktailException("Fail to get cluster information", ExceptionType.ClusterNotFound);
            }

            /**
             * Helm Resource 조회
             * **/
            HelmReleaseBaseVO helmReleaseBase = this.getPackageStatus(clusterSeq, namespaceName, releaseName, null);
            if(helmReleaseBase == null) {
                throw new CocktailException("Fail to get package Resource", ExceptionType.PackageResourceInquireFail);
            }

            /**
             * Helm Resource 응답 객체 생성 및 기본 정보 설정
             * **/
            HelmResourcesVO helmResources = new HelmResourcesVO();
            BeanUtils.copyProperties(helmReleaseBase, helmResources);
            helmResources.setChart(null); // Chart Data 응답하지 않음.

            if(log.isDebugEnabled()) log.debug(JsonUtils.toPrettyString(helmResources));

            /**
             * 배포된 Helm Chart의 Manifest 파일을 Parsing 하여 조회할 리소스 추출
             * **/
            helmResources = packageK8sService.getHelmResourcesFromManifest(helmReleaseBase.getManifest(), clusterSeq, cluster, namespaceName, helmResources);

            /** Resources 조회중 오류 발생시 기본 정보는 다시 넣어줌.. **/
            if(helmResources == null) {
                helmResources = new HelmResourcesVO();
                BeanUtils.copyProperties(helmReleaseBase, helmResources);
                helmResources.setChart(null); // Chart Data 응답하지 않음.
            }

            return helmResources;
        }
        catch (CocktailException ce) {
            if(log.isDebugEnabled()) log.debug("trace log ", ce);
            throw ce; // CocktailException이 발생하면 발생한 예외 처리 케이스를 그대로 사용
        }
        catch (Exception ex) {
            if(log.isDebugEnabled()) log.debug("trace log ", ex);
            throw new CocktailException("Can't get Resources", ExceptionType.PackageResourceInquireFail);
        }
    }


}
