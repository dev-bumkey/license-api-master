package run.acloud.api.catalog.service;

import com.google.common.collect.Lists;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import run.acloud.api.catalog.utils.PackageUtils;
import run.acloud.api.catalog.vo.HelmListRequestVO;
import run.acloud.api.catalog.vo.HelmReleaseBaseVO;
import run.acloud.api.configuration.dao.IClusterMapper;
import run.acloud.api.configuration.vo.ClusterAccessInfoVO;
import run.acloud.api.configuration.vo.ClusterVO;
import run.acloud.api.cserver.dao.IServiceMapper;
import run.acloud.api.cserver.dao.IServicemapMapper;
import run.acloud.api.cserver.vo.ServiceRelationVO;
import run.acloud.api.cserver.vo.ServicemapSummaryVO;
import run.acloud.api.resource.task.K8sTokenGenerator;
import run.acloud.commons.util.CryptoUtils;
import run.acloud.framework.enums.ExceptionBiz;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;
import run.acloud.protobuf.chart.Package;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author gun@acornsoft.io
 * Created on 2019. 1. 29.
 */
@Slf4j
@Service
public class PackageInfoService {

    @Resource(name = "cocktailSession")
    private SqlSessionTemplate sqlSession;

    @Autowired
    private K8sTokenGenerator k8sTokenGenerator;

    @Autowired
    private HelmService helmService;

    /**
     * Package List 조회
     * @param clusterSeq
     * @param namespaceName
     * @param filter
     * @return
     * @throws Exception
     */
    public List<HelmReleaseBaseVO> getPackages(Integer clusterSeq, String namespaceName, String filter) throws Exception {
        try {
            IServicemapMapper servicemapDao = sqlSession.getMapper(IServicemapMapper.class);

            HelmListRequestVO helmListRequest = new HelmListRequestVO();
            Optional.ofNullable(namespaceName).ifPresent(str -> helmListRequest.setNamespace(str));
            Optional.ofNullable(filter).ifPresent(str -> helmListRequest.setFilter(str));
            helmListRequest.setClusterAccessInfo(this.getClusterAccessInfo(clusterSeq));

            Package.HelmListResponse response = helmService.getPackages(helmListRequest);

            /** 클러스터 내의 전체 Appmap 연결 정보를 조회 **/
            List<ServicemapSummaryVO> servicemapSummaries = servicemapDao.getServicemapSummaries(null, clusterSeq, null, null);
            Map<String, ServicemapSummaryVO> servicemapSummariesMap = Optional.ofNullable(servicemapSummaries).orElseGet(() ->Lists.newArrayList())
                    .stream().collect(Collectors.toMap(ServicemapSummaryVO::getNamespaceName, Function.identity()));

            List<HelmReleaseBaseVO> helmReleaseList = new ArrayList<>();
            for (Package.Release release : response.getReleasesList()) {
                HelmReleaseBaseVO helmRelease = PackageUtils.convertRelease(release);
                if (MapUtils.isNotEmpty(servicemapSummariesMap) && servicemapSummariesMap.containsKey(helmRelease.getNamespace())) {
                    helmRelease = PackageUtils.fillRelation(helmRelease, servicemapSummariesMap.get(helmRelease.getNamespace()));
                }

                // chart, manifest는 list에서 응답하지 않음.
                helmRelease.setChart(null);
                helmRelease.setManifest(null);
                helmReleaseList.add(helmRelease);
            }

            return helmReleaseList;
        }
        catch (CocktailException ce) {
            if(log.isDebugEnabled()) log.debug("trace log ", ce);
            throw ce; // CocktailException이 발생하면 발생한 예외 처리 케이스를 그대로 사용
        }
        catch (Exception ex) {
            if(log.isDebugEnabled()) log.debug("trace log ", ex);
            throw new CocktailException(ExceptionType.PackageListInquireFail.getExceptionPolicy().getMessage(), ex, ExceptionType.PackageListInquireFail, ExceptionBiz.PACKAGE_SERVER);
        }
    }

    /**
     * get Service Relation
     * @param clusterSeq
     * @param namespaceName
     * @return
     * @throws Exception
     */
    protected ServiceRelationVO getServiceRelation(Integer clusterSeq, String namespaceName) throws Exception {
        Integer servicemapSeq = null;
        if(clusterSeq != null && StringUtils.isNotBlank(namespaceName)) {
            IServicemapMapper servicemapDao = sqlSession.getMapper(IServicemapMapper.class);
            servicemapSeq = servicemapDao.getServicemapSeqByNamespace(clusterSeq, namespaceName);
        }

        Map<String, Object> params = new HashMap<>();
        if(servicemapSeq != null) {
            params.put("servicemapSeq", servicemapSeq);
        }
        else if (clusterSeq != null) {
            params.put("clusterSeq", clusterSeq);
        }

        if (MapUtils.isEmpty(params)) {
            return null;
        }

        IServiceMapper serviceDao = sqlSession.getMapper(IServiceMapper.class);
        return serviceDao.getServiceRelation(params);
    }

    /**
     * 서비스 연결정보를 조회하여 HelmReleaseBaseVO에 설정..
     * @param helmRelease
     * @param clusterSeq
     * @param namespaceName
     * @return
     * @throws Exception
     */
    protected HelmReleaseBaseVO fillRelation(HelmReleaseBaseVO helmRelease, Integer clusterSeq, String namespaceName) throws Exception {
        if(clusterSeq == null && StringUtils.isBlank(namespaceName)) {
            return helmRelease;
        }
        IServicemapMapper servicemapDao = sqlSession.getMapper(IServicemapMapper.class);
        ServicemapSummaryVO servicemapSummary = servicemapDao.getServicemapSummary(clusterSeq, namespaceName, null);

        return PackageUtils.fillRelation(helmRelease, servicemapSummary);
    }

    /**
     * Cluster Access 정보 조회
     * @param clusterSeq
     * @return
     */
    protected ClusterAccessInfoVO getClusterAccessInfo(Integer clusterSeq) throws Exception {
        try {
            IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
            ClusterVO cluster = clusterDao.getCluster(clusterSeq);

            return this.getClusterAccessInfo(cluster);
        }
        catch (Exception ex) {
            throw new CocktailException("Unable to parse cluster access information", ExceptionType.InvalidClusterCertification);
        }
    }

    /**
     * Cluster Access 정보 조회
     * @param cluster
     * @return
     */
    protected ClusterAccessInfoVO getClusterAccessInfo(ClusterVO cluster) throws Exception {
        try {
            /** Cluster 인증이 Token 유형일 경우 만료기간을 판단하여 만료되었다면 토큰 재발급하고 이후 처리 진행 **/
            if(k8sTokenGenerator.refreshClusterToken(cluster)) {
                IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
                cluster = clusterDao.getCluster(cluster.getClusterSeq());
            }

            ClusterAccessInfoVO clusterAccessInfoVO = new ClusterAccessInfoVO();
            clusterAccessInfoVO.setClusterSeq(cluster.getClusterSeq());
            clusterAccessInfoVO.setClusterId(cluster.getClusterId());
            clusterAccessInfoVO.setApiKey(cluster.getApiKey());
            clusterAccessInfoVO.setApiUrl(cluster.getApiUrl());
            clusterAccessInfoVO.setAuthType(cluster.getAuthType());
            clusterAccessInfoVO.setCubeType(cluster.getCubeType());

            if (StringUtils.isNotBlank(cluster.getApiSecret())) {
                clusterAccessInfoVO.setApiSecret(CryptoUtils.decryptAES(cluster.getApiSecret()));
            }
            if (StringUtils.isNotBlank(cluster.getServerAuthData())) {
                clusterAccessInfoVO.setServerAuthData(CryptoUtils.decryptAES(cluster.getServerAuthData()));
            }
            if (StringUtils.isNotBlank(cluster.getClientAuthData())) {
                clusterAccessInfoVO.setClientAuthData(CryptoUtils.decryptAES(cluster.getClientAuthData()));
            }
            if (StringUtils.isNotBlank(cluster.getClientKeyData())) {
                clusterAccessInfoVO.setClientKeyData(CryptoUtils.decryptAES(cluster.getClientKeyData()));
            }

            return clusterAccessInfoVO;
        }
        catch (Exception ex) {
            throw new CocktailException("Unable to parse cluster access information", ExceptionType.InvalidClusterCertification);
        }
    }

    public ClusterVO setupCluster(Integer servicemapSeq) throws Exception {
        IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
        ClusterVO cluster = clusterDao.getClusterByServicemap(servicemapSeq);
        return cluster;
    }

    public ClusterVO setupCluster(Integer clusterSeq, String namespaceName) throws Exception {
        IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
        ClusterVO cluster = clusterDao.getCluster(clusterSeq);
        cluster.setNamespaceName(namespaceName);

        return cluster;
    }
}
