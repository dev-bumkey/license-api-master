package run.acloud.api.catalog.service;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.error.YAMLException;
import run.acloud.api.catalog.dao.IPackageMapper;
import run.acloud.api.catalog.vo.HelmInstallRequestVO;
import run.acloud.api.catalog.vo.HelmReleaseBaseVO;
import run.acloud.api.catalog.vo.HelmStatusRequestVO;
import run.acloud.api.catalog.vo.PackageDeployHistoryVO;
import run.acloud.api.k8sextended.util.Yaml;
import run.acloud.commons.util.JsonUtils;
import run.acloud.commons.vo.ExecutingContextVO;
import run.acloud.framework.exception.CocktailException;
import run.acloud.framework.util.ExceptionMessageUtils;
import run.acloud.protobuf.chart.Package;

import java.util.HashMap;
import java.util.Map;

/**
 * @author gun@acornsoft.io
 * Created on 2019. 1. 29.
 */
@Slf4j
@Service
public class PackageHistoryService {

    @Resource(name = "cocktailSession")
    private SqlSessionTemplate sqlSession;

    /**
     * Add Package Deploy History (Rollback)
     * @param command
     * @param executionResult
     * @param helmStatusRequest
     * @param helmReleaseBase
     * @throws Exception
     */
    protected void addPackageRollbackHistory(String command, String executionResult, Integer clusterSeq, HelmStatusRequestVO helmStatusRequest, HelmReleaseBaseVO helmReleaseBase) throws Exception {
        try {
            IPackageMapper packageDao = sqlSession.getMapper(IPackageMapper.class);

            if(packageDao == null) {
                log.error(" packageDao in addPackageRollbackHistory method is null ");
                return;
            }

            PackageDeployHistoryVO packageDeployHistory = new PackageDeployHistoryVO();
            packageDeployHistory.setClusterSeq(clusterSeq);
            packageDeployHistory.setNamespaceName(helmStatusRequest.getNamespace());
            packageDeployHistory.setReleaseName(helmStatusRequest.getReleaseName());
            packageDeployHistory.setChartName(helmReleaseBase.getChartName());
            packageDeployHistory.setChartVersion(helmReleaseBase.getChartVersion());
            packageDeployHistory.setRevision(helmReleaseBase.getRevision());
            packageDeployHistory.setPackageManifest(helmReleaseBase.getManifest());
            packageDeployHistory.setCommand(command);
            packageDeployHistory.setExecutionResult(executionResult);

            packageDeployHistory.setChartValues(helmReleaseBase.getValues());

            /** Package Status 조회에서 이미 values.yaml을 조회 하지만,
             * Status에서 조회된 데이터는 현재 Status의 values.yaml이므로 올바른 데이터가 아님. (최신 Revision으로 조회..)
             * rollback 요청된 Revision에 대해 새로 조회하여 처리가 필요..
             */
            PackageDeployHistoryVO history = this.getPackageDeployHistory(packageDeployHistory.getClusterSeq(),
                    packageDeployHistory.getNamespaceName(),
                    packageDeployHistory.getReleaseName(),
                    packageDeployHistory.getChartName(),
                    packageDeployHistory.getChartVersion(),
                    helmStatusRequest.getRevision(), // Revision을 rollback 요청한 Revision으로 설정
                    packageDao);

            if(history != null) {
                // 이력 데이터가 존재하면 이력의 values Yaml 파일을 이용하여 이력 적재
                if(log.isDebugEnabled()) log.debug("==================\n" + JsonUtils.toPrettyString(history) + "\n====================");
                packageDeployHistory.setChartValues(history.getChartValues());
                packageDeployHistory.setRepository(history.getRepository());
                // Rollback 완료된 후 values.yaml로 응답 데이터도 다시 설정..
                helmReleaseBase.setValues(history.getChartValues());
            }
            else {
                // 이력 데이터가 존재하지 않으면, Package 배포 정보의 데이터를 Yaml로 컨버트 하여 이력 적재
                Map<String, Object> jsonMap = JsonUtils.fromGson(helmReleaseBase.getChart(), HashMap.class);
                String yamlStr = Yaml.getSnakeYaml().dump(jsonMap.get("values"));
                log.debug("========================== YAML ===========================\n" + yamlStr);
                packageDeployHistory.setChartValues(yamlStr);
                // Rollback 완료된 후 values.yaml로 응답 데이터도 다시 설정..
                helmReleaseBase.setValues(yamlStr);
            }

            packageDao.addPackageDeployHistory(packageDeployHistory);

        } catch (YAMLException ye) {
            log.debug("Can't add Package Rollback History : {} : {}\nRequest-------------- \n{}\nYAMLException-------------- \n{}\nExceptionCause-------------- \n{}\nStackTrace-------------- \n{}"
                    , command, executionResult, JsonUtils.toPrettyString(helmStatusRequest), ye.getMessage(), ExceptionMessageUtils.getExceptionCause(ye), ExceptionMessageUtils.getStackTrace(ye));
        } catch (Exception ex) {
            log.debug("Can't add Package Rollback History : {} : {}\nRequest-------------- \n{}\nException-------------- \n{}: {}\nExceptionCause-------------- \n{}\nStackTrace-------------- \n{}"
                    , command, executionResult, JsonUtils.toPrettyString(helmStatusRequest), ExceptionMessageUtils.getException(ex), ex.getMessage(), ExceptionMessageUtils.getExceptionCause(ex), ExceptionMessageUtils.getStackTrace(ex));
        }
    }

    /**
     * Add Package Deploy History (Install, Upgrade)
     * @param command
     * @param executionResult
     * @param helmInstallRequest
     * @param release
     * @throws Exception
     */
    protected void addPackageDeployHistory(String command, String executionResult, HelmInstallRequestVO helmInstallRequest, Package.Release release) throws Exception {
        this.addPackageDeployHistory(command, executionResult, helmInstallRequest, release, null);
    }

    /**
     * Add Package Deploy History (Install, Upgrade)
     * @param command
     * @param executionResult
     * @param helmInstallRequest
     * @param release
     */
    protected void addPackageDeployHistory(String command, String executionResult, HelmInstallRequestVO helmInstallRequest, Package.Release release, ExecutingContextVO context) throws Exception {
        try {
            PackageDeployHistoryVO packageDeployHistory = new PackageDeployHistoryVO();

            packageDeployHistory.setClusterSeq(helmInstallRequest.getClusterAccessInfo().getClusterSeq());
            packageDeployHistory.setNamespaceName(helmInstallRequest.getNamespace());
            packageDeployHistory.setReleaseName(helmInstallRequest.getReleaseName());
            packageDeployHistory.setChartName(helmInstallRequest.getChartName());
            packageDeployHistory.setChartVersion(helmInstallRequest.getVersion());
            packageDeployHistory.setChartValues(helmInstallRequest.getValues());
            packageDeployHistory.setRepository(helmInstallRequest.getRepo());
            if(release != null) {
                packageDeployHistory.setRevision(release.getVersion());
                packageDeployHistory.setPackageManifest(release.getManifest());
            }
            if(context != null) {
                packageDeployHistory.setCreator(context.getUserSeq());
            }
            packageDeployHistory.setCommand(command);
            packageDeployHistory.setExecutionResult(executionResult);

            IPackageMapper packageDao = sqlSession.getMapper(IPackageMapper.class);
            packageDao.addPackageDeployHistory(packageDeployHistory);

        } catch (CocktailException ce) {
            log.debug("Can't add Package DeployHistory : {} : {}\nRequest-------------- \n{}\nCocktailException-------------- \n{}\nExceptionCause-------------- \n{}\nStackTrace-------------- \n{}"
                    , command, executionResult, JsonUtils.toPrettyString(helmInstallRequest), ce.getMessage(), ExceptionMessageUtils.getExceptionCause(ce), ExceptionMessageUtils.getStackTrace(ce));
        } catch (Exception ex) {
            log.debug("Can't add Package DeployHistory : {} : {}\nRequest-------------- \n{}\nException-------------- \n{}: {}\nExceptionCause-------------- \n{}\nStackTrace-------------- \n{}"
                    , command, executionResult, JsonUtils.toPrettyString(helmInstallRequest), ExceptionMessageUtils.getException(ex), ex.getMessage(), ExceptionMessageUtils.getExceptionCause(ex), ExceptionMessageUtils.getStackTrace(ex));
        }
    }

    /**
     * get Package Deploy History.
     * @param clusterSeq
     * @param namespaceName
     * @param releaseName
     * @param chartName
     * @param chartVersion
     * @param revision
     * @param packageDao
     * @return
     */
    protected PackageDeployHistoryVO getPackageDeployHistory(Integer clusterSeq,
                                                           String namespaceName,
                                                           String releaseName,
                                                           String chartName,
                                                           String chartVersion,
                                                           String revision,
                                                           IPackageMapper packageDao) throws Exception
    {
        if(packageDao == null) {
            packageDao = sqlSession.getMapper(IPackageMapper.class);
        }

        /** Input Parameter Setting **/
        Map<String, Object> params = new HashMap<>();
        params.put("clusterSeq", clusterSeq);
        params.put("namespaceName", namespaceName);
        params.put("releaseName", releaseName);
        params.put("chartName", chartName);
        params.put("chartVersion", chartVersion);
        params.put("revision", revision);

        /** Get! **/
        return packageDao.getPackageDeployHistory(params);
    }
}
