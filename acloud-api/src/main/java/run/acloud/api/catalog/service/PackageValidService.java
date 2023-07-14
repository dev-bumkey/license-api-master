package run.acloud.api.catalog.service;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import run.acloud.api.catalog.vo.HelmInstallRequestVO;
import run.acloud.api.catalog.vo.HelmReleaseBaseVO;
import run.acloud.api.configuration.dao.IClusterMapper;
import run.acloud.api.configuration.vo.ClusterVO;
import run.acloud.api.cserver.service.ServerValidService;
import run.acloud.api.k8sextended.util.Yaml;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author gun@acornsoft.io
 * Created on 2019. 1. 29.
 */
@Slf4j
@Service
public class PackageValidService {

    @Resource(name = "cocktailSession")
    private SqlSessionTemplate sqlSession;

    @Autowired
    private ServerValidService serverValidService;

    @Autowired
    private PackageInfoService packageInfoService;



    /**
     * Release Name & Server Name으로 모두 사용중인지 체크.
     * @param clusterSeq
     * @param namespaceName
     * @param checkName
     * @param isCheckServerName
     * @return
     * @throws Exception
     */
    public boolean isUsingReleaseName(Integer clusterSeq, String namespaceName, String checkName, boolean isCheckServerName) throws Exception {
        IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
        ClusterVO cluster = clusterDao.getCluster(clusterSeq);
        cluster.setNamespaceName(namespaceName);

        return this.isUsingReleaseName(cluster, namespaceName, checkName, isCheckServerName);
    }

    /**
     * Release Name & Server Name으로 모두 사용중인지 체크.
     * @param cluster
     * @param namespaceName
     * @param checkName
     * @return
     * @throws Exception
     */
    public boolean isUsingReleaseName(ClusterVO cluster, String namespaceName, String checkName, boolean isCheckServerName) throws Exception {
        List<HelmReleaseBaseVO> helmReleaseList = packageInfoService.getPackages(cluster.getClusterSeq(), namespaceName, null);

        long count = helmReleaseList.stream().filter(rb -> StringUtils.equals(rb.getName(), checkName)).collect(Collectors.counting());
        if(count > 0) {
            return true;
        }

        if(isCheckServerName) {
            return serverValidService.checkServerNameIfExists(cluster, namespaceName, checkName, false, null);
        }

        return false;
    }


    /**
     * Package Install & Upgrade Validation Check
     * @param helmInstallRequest
     * @param namespaceName
     * @param packageName
     * @throws Exception
     */
    public void packageInstallValidation(HelmInstallRequestVO helmInstallRequest, String namespaceName, String packageName) throws Exception {
        if(StringUtils.isNotBlank(namespaceName)) {
            if (!StringUtils.equals(namespaceName, helmInstallRequest.getNamespace())) {
                throw new CocktailException("namespace is different ", ExceptionType.InvalidInputData);
            }
        }
        if(StringUtils.isNotBlank(packageName)) {
            if(!StringUtils.equals(packageName, helmInstallRequest.getReleaseName())) {
                throw new CocktailException("packageName is different ", ExceptionType.InvalidInputData);
            }
        }
        if(StringUtils.isBlank(helmInstallRequest.getChartName())) {
            throw new CocktailException("ChartName is null ", ExceptionType.InvalidInputData);
        }
        if(StringUtils.isBlank(helmInstallRequest.getReleaseName())) {
            throw new CocktailException("ReleaseName is null ", ExceptionType.InvalidInputData);
        }
        if(StringUtils.isBlank(helmInstallRequest.getRepo())) {
            throw new CocktailException("Repository is null ", ExceptionType.InvalidInputData);
        }
//        if(StringUtils.isBlank(helmInstallRequest.getValues())) {
//            throw new CocktailException("Values is null ", ExceptionType.InvalidInputData);
//        }
        if(StringUtils.isBlank(helmInstallRequest.getVersion())) {
            throw new CocktailException("Version is null ", ExceptionType.InvalidInputData);
        }
        if(this.isInvalidYamlFormat(helmInstallRequest.getValues())) {
            throw new CocktailException("Yaml is invalid.", ExceptionType.InvalidYamlData);
        }
    }


    /**
     * 변환 가능한 Yaml 인지 (Yaml Validation) 체크.
     * @param yamlString
     * @return
     * @throws Exception
     */
    public boolean isInvalidYamlFormat(String yamlString) throws Exception {
        if(StringUtils.isBlank(yamlString)) {
            // Blank는 허용...
            return false;
        }
        try {
            Iterable<Object> iterable = Yaml.getSnakeYaml().loadAll(yamlString);
            List<Object> objs = new ArrayList();
            Iterator iterator = iterable.iterator();
            while (iterator.hasNext()) {
                Object object = iterator.next();
                if (object != null) {
                    objs.add(object);
                }
            }
        }
        catch (Exception ex) {
            return true;
        }

        return false;
    }
}
