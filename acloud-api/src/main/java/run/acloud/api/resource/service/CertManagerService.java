package run.acloud.api.resource.service;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.reflect.TypeToken;
import io.kubernetes.client.openapi.JSON;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1OwnerReference;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import run.acloud.api.configuration.dao.IClusterMapper;
import run.acloud.api.configuration.service.ClusterStateService;
import run.acloud.api.configuration.vo.ClusterVO;
import run.acloud.api.cserver.enums.DeployType;
import run.acloud.api.cserver.util.ServerUtils;
import run.acloud.api.k8sextended.util.Yaml;
import run.acloud.api.resource.constants.KubeConstants;
import run.acloud.api.resource.dao.ICertManagerMapper;
import run.acloud.api.resource.enums.*;
import run.acloud.api.resource.task.K8sCRDSpecFactory;
import run.acloud.api.resource.util.ResourceUtil;
import run.acloud.api.resource.vo.*;
import run.acloud.commons.util.Base64Utils;
import run.acloud.commons.util.CryptoUtils;
import run.acloud.framework.context.ContextHolder;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;
import run.acloud.framework.util.ExceptionMessageUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CertManagerService {
    @Resource(name = "cocktailSession")
    private SqlSessionTemplate sqlSession;

    @Autowired
    private CRDResourceService crdResourceService;

    @Autowired
    private K8sResourceService k8sResourceService;

    @Autowired
    private SecretService secretService;

    @Autowired
    private WorkloadResourceService workloadResourceService;

    @Autowired
    private ClusterStateService clusterStateService;

    /**
     * add Cert-Private Issuer
     *
     * @param issuer
     * @return
     * @throws Exception
     */
    public Map<String, Object> addCertPrivateIssuer(K8sCRDIssuerIntegrateVO issuer) throws Exception {

        IClusterMapper cDao = sqlSession.getMapper(IClusterMapper.class);

        // GUI 모델
        K8sCRDIssuerGuiVO issuerGui;
        // YAML 모델
        K8sCRDIssuerYamlVO issuerYaml;
        // 최종 CRD 모델
        Map<String, Object> crdMap = null;
        // Issuer Scope
        CertIssuerScope scope;
        ClusterVO cluster;
        String namespace = null;
        Map<String, Object> result = null;

        if (DeployType.valueOf(issuer.getDeployType()) == DeployType.GUI) {
            issuerGui = (K8sCRDIssuerGuiVO)issuer;

            // check issuer scope
            ExceptionMessageUtils.checkParameterRequired("scope", issuerGui.getScope());
            if (!StringUtils.equalsAny(issuerGui.getScope(), CertIssuerScope.CLUSTER.getCode(), CertIssuerScope.NAMESPACED.getCode())) {
                String errMsg = "Invalid scope parameter!!";
                throw new CocktailException(errMsg, ExceptionType.InvalidParameter, errMsg);
            } else {
                scope = CertIssuerScope.valueOf(issuerGui.getScope());
            }

            // check clusterSeq
            ExceptionMessageUtils.checkParameterRequired("clusterSeq", issuerGui.getClusterSeq());
            cluster = cDao.getCluster(issuerGui.getClusterSeq());
            clusterStateService.checkClusterState(cluster);

            // check cert-manager 설치 여부
            this.checkInstalledCertManagerExists(cluster);

            // set namespace
            if (scope == CertIssuerScope.NAMESPACED) {
                ExceptionMessageUtils.checkParameterRequired("namespace", issuerGui.getNamespace());
                namespace = issuerGui.getNamespace();
            }

            // check name
            ExceptionMessageUtils.checkParameter("name", issuerGui.getName(), 256, true);
            if (!issuerGui.getName().matches(KubeConstants.RULE_RESOURCE_NAME)) {
                throw new CocktailException("Issuer name is invalid", ExceptionType.InvalidParameter, ResourceUtil.getInvalidNameMsg("Issuer name is invalid"));
            }

            // issueType에 따른 spec 체크
            // GUI는 selfSigned, ca만 지원
            ExceptionMessageUtils.checkParameterRequired("IssueType", issuerGui.getIssueType());
            if (!EnumSet.of(CertIssueType.selfSigned, CertIssueType.ca).contains(CertIssueType.valueOf(issuerGui.getIssueType()))) {
                String errMsg = "Only selfSigned, ca GUI supported.";
                throw new CocktailException(errMsg, ExceptionType.InvalidParameter, errMsg);
            } else {
                switch (CertIssueType.valueOf(issuerGui.getIssueType())) {
                    case selfSigned -> {
                        issuerGui.setCa(null);
                    }
                    case ca -> {
                        issuerGui.setSelfSigned(null);
                        if (issuerGui.getCa() != null) {
                            ExceptionMessageUtils.checkParameterRequired("secretName", issuerGui.getCa().getSecretName());
                        } else {
                            String errMsg = "Invalid ca parameter!!";
                            throw new CocktailException(errMsg, ExceptionType.InvalidParameter, errMsg);
                        }
                    }
                }

                crdMap = K8sCRDSpecFactory.buildIssuer(issuerGui);
            }
        }
        else {
            issuerYaml = (K8sCRDIssuerYamlVO)issuer;

            // check issuer scope
            ExceptionMessageUtils.checkParameterRequired("scope", issuerYaml.getScope());
            if (!StringUtils.equalsAny(issuerYaml.getScope(), CertIssuerScope.CLUSTER.getCode(), CertIssuerScope.NAMESPACED.getCode())) {
                String errMsg = "Invalid scope parameter!!";
                throw new CocktailException(errMsg, ExceptionType.InvalidParameter, errMsg);
            } else {
                scope = CertIssuerScope.valueOf(issuerYaml.getScope());
            }

            // check clusterSeq
            ExceptionMessageUtils.checkParameterRequired("clusterSeq", issuerYaml.getClusterSeq());
            cluster = cDao.getCluster(issuerYaml.getClusterSeq());
            clusterStateService.checkClusterState(cluster);

            // check cert-manager 설치 여부
            this.checkInstalledCertManagerExists(cluster);

            // set namespace
            if (scope == CertIssuerScope.NAMESPACED) {
                ExceptionMessageUtils.checkParameterRequired("namespace", issuerYaml.getNamespace());
                namespace = issuerYaml.getNamespace();
            }

            // issueType에 따른 spec 체크
            ExceptionMessageUtils.checkParameterRequired("IssueType", issuerYaml.getIssueType());
            if (!EnumSet.of(CertIssueType.selfSigned, CertIssueType.ca, CertIssueType.acme, CertIssueType.vault, CertIssueType.venafi).contains(CertIssueType.valueOf(issuerYaml.getIssueType()))) {
                String errMsg = "IssueType Not supported.";
                throw new CocktailException(errMsg, ExceptionType.InvalidParameter, errMsg);
            } else {
                boolean isValidYaml = true;
                JSON k8sJson = new JSON();

                if (StringUtils.isNotBlank(issuerYaml.getYaml())) {
                    CertIssueType issueType = CertIssueType.valueOf(issuerYaml.getIssueType());
                    // Yaml to Map
                    Map<String, Object> objMap = Yaml.getSnakeYaml().load(issuerYaml.getYaml());
                    if (MapUtils.isNotEmpty(objMap)) {
                        // objectMeta 객체
                        V1ObjectMeta objectMeta = ServerUtils.getK8sObjectMetaInMap(objMap, k8sJson);
                        // spec 객체
                        Object specObj = MapUtils.getObject(objMap, KubeConstants.SPEC, null);
                        if (objectMeta != null && specObj != null) {

                            // check yaml meta
                            objectMeta.setNamespace(namespace);
                            this.checkCRDYamlCommonMetaInfo(true, null, namespace, scope.getKind(), K8sApiGroupType.CERT_MANAGER_IO, K8sApiType.V1, objectMeta, objMap);

                            Map<String, Object> specMap = (Map<String, Object>)specObj;
                            // 하나만 존재하여야 함
                            if (specMap.keySet().size() == 1) {
                                if (!specMap.containsKey(issueType.getCode())) {
                                    isValidYaml = false;
                                }
                            } else {
                                isValidYaml = false;
                            }
                        } else {
                            isValidYaml = false;
                        }
                    } else {
                        isValidYaml = false;
                    }
                } else {
                    String errMsg = "Issuer yaml is empty!!";
                    throw new CocktailException(errMsg, ExceptionType.InvalidParameter_Empty, errMsg);
                }

                if (!isValidYaml) {
                    String errMsg = "Invalid yaml parameter!!";
                    throw new CocktailException(errMsg, ExceptionType.InvalidParameter, errMsg);
                } else {
                    crdMap = K8sCRDSpecFactory.buildIssuer(issuerYaml);
                }
            }
        }

        if (MapUtils.isNotEmpty(crdMap)) {

            result = crdResourceService.createCustomObject(cluster, namespace, scope.getKind(), crdMap);
        }

        return result;
    }

    /**
     * edit Cert-Private Issuer
     *
     * @param clusterSeq
     * @param namespaceName
     * @param issuerName
     * @param issuer
     * @return
     * @throws Exception
     */
    public Map<String, Object> editCertPrivateIssuer(Integer clusterSeq, String namespaceName, String issuerName, K8sCRDIssuerIntegrateVO issuer) throws Exception {
        // GUI 모델
        K8sCRDIssuerGuiVO issuerGui;
        // YAML 모델
        K8sCRDIssuerYamlVO issuerYaml;
        // 최종 CRD 모델
        Map<String, Object> crdMap = null;
        // Issuer Scope
        CertIssuerScope scope;
        Map<String, Object> result = null;

        IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
        ClusterVO cluster = null;

        if (DeployType.valueOf(issuer.getDeployType()) == DeployType.GUI) {
            issuerGui = (K8sCRDIssuerGuiVO)issuer;

            // check issuer scope
            ExceptionMessageUtils.checkParameterRequired("scope", issuerGui.getScope());
            if (!StringUtils.equalsAny(issuerGui.getScope(), CertIssuerScope.CLUSTER.getCode(), CertIssuerScope.NAMESPACED.getCode())) {
                String errMsg = "Invalid scope parameter!!";
                throw new CocktailException(errMsg, ExceptionType.InvalidParameter, errMsg);
            } else {
                scope = CertIssuerScope.valueOf(issuerGui.getScope());
            }

            // check clusterSeq
            ExceptionMessageUtils.checkParameterRequired("clusterSeq", issuerGui.getClusterSeq());
            if (!issuerGui.getClusterSeq().equals(clusterSeq)) {
                String errMsg = "Invalid clusterSeq parameter!!";
                throw new CocktailException(errMsg, ExceptionType.InvalidParameter, errMsg);
            }
            cluster = clusterDao.getCluster(clusterSeq);
            clusterStateService.checkClusterState(cluster);

            // set namespace
            if (scope == CertIssuerScope.NAMESPACED) {
                ExceptionMessageUtils.checkParameterRequired("namespace", issuerGui.getNamespace());
                if (!StringUtils.equals(namespaceName, issuerGui.getNamespace())) {
                    String errMsg = "Invalid namespace parameter!!";
                    throw new CocktailException(errMsg, ExceptionType.InvalidParameter, errMsg);
                }
            }

            // check name
            ExceptionMessageUtils.checkParameter("name", issuerGui.getName(), 256, true);
            if (!issuerGui.getName().matches(KubeConstants.RULE_RESOURCE_NAME)) {
                throw new CocktailException("Issuer name is invalid", ExceptionType.InvalidParameter, ResourceUtil.getInvalidNameMsg("Issuer name is invalid"));
            } else {
                if (!StringUtils.equals(issuerName, issuerGui.getName())) {
                    String errMsg = "Invalid name parameter!!";
                    throw new CocktailException(errMsg, ExceptionType.InvalidParameter, errMsg);
                }
            }

            // issueType에 따른 spec 체크
            // GUI는 selfSigned, ca만 지원
            ExceptionMessageUtils.checkParameterRequired("IssueType", issuerGui.getIssueType());
            if (!EnumSet.of(CertIssueType.selfSigned, CertIssueType.ca).contains(CertIssueType.valueOf(issuerGui.getIssueType()))) {
                String errMsg = "Only selfSigned, ca GUI supported.";
                throw new CocktailException(errMsg, ExceptionType.InvalidParameter, errMsg);
            } else {
                switch (CertIssueType.valueOf(issuerGui.getIssueType())) {
                    case selfSigned -> {
                        issuerGui.setCa(null);
                    }
                    case ca -> {
                        issuerGui.setSelfSigned(null);
                        if (issuerGui.getCa() != null) {
                            ExceptionMessageUtils.checkParameterRequired("secretName", issuerGui.getCa().getSecretName());
                        } else {
                            String errMsg = "Invalid ca parameter!!";
                            throw new CocktailException(errMsg, ExceptionType.InvalidParameter, errMsg);
                        }
                    }
                }

                crdMap = K8sCRDSpecFactory.buildIssuer(issuerGui);
            }
        }
        else {
            issuerYaml = (K8sCRDIssuerYamlVO)issuer;

            // check issuer scope
            ExceptionMessageUtils.checkParameterRequired("scope", issuerYaml.getScope());
            if (!StringUtils.equalsAny(issuerYaml.getScope(), CertIssuerScope.CLUSTER.getCode(), CertIssuerScope.NAMESPACED.getCode())) {
                String errMsg = "Invalid scope parameter!!";
                throw new CocktailException(errMsg, ExceptionType.InvalidParameter, errMsg);
            } else {
                scope = CertIssuerScope.valueOf(issuerYaml.getScope());
            }

            // check clusterSeq
            ExceptionMessageUtils.checkParameterRequired("clusterSeq", issuerYaml.getClusterSeq());
            if (!issuerYaml.getClusterSeq().equals(clusterSeq)) {
                String errMsg = "Invalid clusterSeq parameter!!";
                throw new CocktailException(errMsg, ExceptionType.InvalidParameter, errMsg);
            }
            cluster = clusterDao.getCluster(clusterSeq);
            clusterStateService.checkClusterState(cluster);

            // set namespace
            if (scope == CertIssuerScope.NAMESPACED) {
                ExceptionMessageUtils.checkParameterRequired("namespace", issuerYaml.getNamespace());
                if (!StringUtils.equals(namespaceName, issuerYaml.getNamespace())) {
                    String errMsg = "Invalid namespace parameter!!";
                    throw new CocktailException(errMsg, ExceptionType.InvalidParameter, errMsg);
                }
            }

            // check name
            ExceptionMessageUtils.checkParameter("name", issuerYaml.getName(), 256, true);
            if (!issuerYaml.getName().matches(KubeConstants.RULE_RESOURCE_NAME)) {
                throw new CocktailException("Issuer name is invalid", ExceptionType.InvalidParameter, ResourceUtil.getInvalidNameMsg("Issuer name is invalid"));
            } else {
                if (!StringUtils.equals(issuerName, issuerYaml.getName())) {
                    String errMsg = "Invalid name parameter!!";
                    throw new CocktailException(errMsg, ExceptionType.InvalidParameter, errMsg);
                }
            }

            // issueType에 따른 spec 체크
            ExceptionMessageUtils.checkParameterRequired("IssueType", issuerYaml.getIssueType());
            if (!EnumSet.of(CertIssueType.selfSigned, CertIssueType.ca, CertIssueType.acme, CertIssueType.vault, CertIssueType.venafi).contains(CertIssueType.valueOf(issuerYaml.getIssueType()))) {
                throw new CocktailException("Not supported.", ExceptionType.InvalidParameter);
            } else {
                boolean isValidYaml = true;
                JSON k8sJson = new JSON();
                if (StringUtils.isNotBlank(issuerYaml.getYaml())) {
                    CertIssueType issueType = CertIssueType.valueOf(issuerYaml.getIssueType());
                    // Yaml to Map
                    Map<String, Object> objMap = Yaml.getSnakeYaml().load(issuerYaml.getYaml());

                    if (MapUtils.isNotEmpty(objMap)) {
                        // objectMeta 객체
                        V1ObjectMeta objectMeta = ServerUtils.getK8sObjectMetaInMap(objMap, k8sJson);
                        // spec 객체
                        Object specObj = MapUtils.getObject(objMap, KubeConstants.SPEC, null);

                        if (objectMeta != null && specObj != null) {

                            // check yaml meta
                            this.checkCRDYamlCommonMetaInfo(false, issuerName, namespaceName, scope.getKind(), K8sApiGroupType.CERT_MANAGER_IO, K8sApiType.V1, objectMeta, objMap);

                            Map<String, Object> specMap = (Map<String, Object>)specObj;
                            // 하나만 존재하여야 함
                            if (specMap.keySet().size() == 1) {
                                if (!specMap.containsKey(issueType.getCode())) {
                                    isValidYaml = false;
                                }
                            } else {
                                isValidYaml = false;
                            }
                        } else {
                            isValidYaml = false;
                        }
                    } else {
                        isValidYaml = false;
                    }
                } else {
                    String errMsg = "yaml is empty!!";
                    throw new CocktailException(errMsg, ExceptionType.InvalidParameter_Empty, errMsg);
                }

                if (!isValidYaml) {
                    String errMsg = "Invalid yaml parameter!!";
                    throw new CocktailException(errMsg, ExceptionType.InvalidParameter, errMsg);
                } else {
                    crdMap = K8sCRDSpecFactory.buildIssuer(issuerYaml);
                }
            }
        }

        if (MapUtils.isNotEmpty(crdMap)) {
            K8sCRDYamlVO crdYaml = new K8sCRDYamlVO();
            crdYaml.setYaml(Yaml.getSnakeYaml().dumpAsMap(crdMap));
            result = crdResourceService.replaceCustomObject(cluster, namespaceName, issuerName, scope.getKind(), crdYaml);
        }

        return result;

    }

    public void deleteCertPrivateIssuer(CertIssuerScope scope, Integer clusterSeq, String namespaceName, String issuerName) throws Exception {
        IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
        ClusterVO cluster = clusterDao.getCluster(clusterSeq);
        this.deleteCertPrivateIssuer(scope, cluster, namespaceName, issuerName);
    }

    /**
     * delete Cert-Private Issuer
     *
     * @param scope
     * @param cluster
     * @param namespaceName
     * @param issuerName
     * @throws Exception
     */
    public void deleteCertPrivateIssuer(CertIssuerScope scope, ClusterVO cluster, String namespaceName, String issuerName) throws Exception {
        if (scope != null && cluster != null) {
            /** 클러스터 상태 체크 **/
            clusterStateService.checkClusterState(cluster);

            // TODO: issuerRef 에서 사용시 삭제 제한

            switch (scope) {
                case CLUSTER -> {
                    crdResourceService.deleteCustomObject(cluster, null, issuerName, scope.getKind());
                }
                case NAMESPACED -> {
                    crdResourceService.deleteCustomObject(cluster, namespaceName, issuerName, scope.getKind());
                }

            }
        }
    }

    /**
     * Cert-Private Issuer 목록 조회
     *
     * @param accountSeq
     * @param clusterSeq
     * @param withEvent - 이벤트 정보 조회 여부
     * @return
     * @throws Exception
     */
    public List<K8sCRDIssuerVO> getCertPrivateIssuers(Integer accountSeq, Integer clusterSeq, boolean withEvent) throws Exception {
        IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
        List<ClusterVO> clusters = clusterDao.getClusters(accountSeq, null, clusterSeq, null, null, "Y");

        List<K8sCRDIssuerVO> k8sCRDIssuers = Lists.newArrayList();

        if (CollectionUtils.isNotEmpty(clusters)) {
            // Map<clusterSeq, Map<K8sApiKindType, List<Map<String, Object>>>>
            Map<Integer, Map<K8sApiKindType, List<Map<String, Object>>>> issuerMap = Maps.newHashMap();
            // Map<clusterSeq, Map<kind, Map<namespace, Map<name, List<K8sEventVO>>>>>
            Map<Integer, Map<String, Map<String, Map<String, List<K8sEventVO>>>>> eventMap = Maps.newHashMap();
            Map<Integer, ClusterVO> clusterMap = Maps.newHashMap();

            for (ClusterVO cluster : clusters) {
                if (clusterStateService.isClusterRunning(cluster)) {
                    clusterMap.put(cluster.getClusterSeq(), cluster);
                    issuerMap.put(cluster.getClusterSeq(), Maps.newHashMap());

                    // ClusterIssuer
                    issuerMap.get(cluster.getClusterSeq()).put(K8sApiKindType.CLUSTER_ISSUER, Lists.newArrayList());
                    List<Map<String, Object>> clusterIssuers = crdResourceService.getCustomObjects(cluster, null, K8sApiKindType.CLUSTER_ISSUER, null);
                    if (CollectionUtils.isNotEmpty(clusterIssuers)) {
                        issuerMap.get(cluster.getClusterSeq()).get(K8sApiKindType.CLUSTER_ISSUER).addAll(clusterIssuers);
                    }

                    // Issuer
                    issuerMap.get(cluster.getClusterSeq()).put(K8sApiKindType.ISSUER, Lists.newArrayList());
                    List<Map<String, Object>> issuers = crdResourceService.getCustomObjects(cluster, null, K8sApiKindType.ISSUER, null);
                    if (CollectionUtils.isNotEmpty(issuers)) {
                        issuerMap.get(cluster.getClusterSeq()).get(K8sApiKindType.ISSUER).addAll(issuers);
                    }

                    // Event
                    if (withEvent) {
                        List<K8sEventVO> events = k8sResourceService.convertEventDataList(cluster, null, null, null);
                        if (CollectionUtils.isNotEmpty(events)) {
                            eventMap.put(cluster.getClusterSeq(), Maps.newHashMap());

                            for (K8sEventVO eventRow : events) {
                                if (!eventMap.get(cluster.getClusterSeq()).containsKey(eventRow.getKind())) {
                                    eventMap.get(cluster.getClusterSeq()).put(eventRow.getKind(), Maps.newHashMap());
                                }
                                if (!eventMap.get(cluster.getClusterSeq()).get(eventRow.getKind()).containsKey(eventRow.getNamespace())) {
                                    eventMap.get(cluster.getClusterSeq()).get(eventRow.getKind()).put(eventRow.getNamespace(), Maps.newHashMap());
                                }
                                if (!eventMap.get(cluster.getClusterSeq()).get(eventRow.getKind()).get(eventRow.getNamespace()).containsKey(eventRow.getName())) {
                                    eventMap.get(cluster.getClusterSeq()).get(eventRow.getKind()).get(eventRow.getNamespace()).put(eventRow.getName(), Lists.newArrayList());
                                }

                                eventMap.get(cluster.getClusterSeq()).get(eventRow.getKind()).get(eventRow.getNamespace()).get(eventRow.getName()).add(eventRow);
                            }
                        }
                    }
                }
            }

            // convert Map to Model
            if (MapUtils.isNotEmpty(issuerMap)) {
                JSON k8sJson = new JSON();
                // cluster Loop
                for (Map.Entry<Integer, Map<K8sApiKindType, List<Map<String, Object>>>> clusterEntry : issuerMap.entrySet()) {

                    ClusterVO cluster = clusterMap.get(clusterEntry.getKey());

                    // issuer type Loop
                    for (Map.Entry<K8sApiKindType, List<Map<String, Object>>> kindEntry : clusterEntry.getValue().entrySet()) {

                        // Issuer scope : CLUSTER(ClusterIssuer), NAMESPACED(Issuer)
                        CertIssuerScope scope = CertIssuerScope.kindOf(kindEntry.getKey());

                        // issuer Loop
                        for (Map<String, Object> issuerItem : kindEntry.getValue()) {

                            // objectMeta 객체
                            V1ObjectMeta objectMeta = ServerUtils.getK8sObjectMetaInMap(issuerItem, k8sJson);

                            // gen issuer model
                            K8sCRDIssuerVO issuer = this.genCertPrivateCRDIssuer(scope, cluster, issuerItem, objectMeta, k8sJson);

                            // event
                            if (withEvent) {
                                if (eventMap.containsKey(clusterEntry.getKey())) {
                                    issuer.setEvents(
                                            Optional.of(eventMap)
                                                    .map(e -> e.get(clusterEntry.getKey()))
                                                    .map(e -> e.get((String)issuerItem.get(KubeConstants.KIND)))
                                                    .map(e -> e.get(objectMeta.getNamespace()))
                                                    .map(e -> e.get(objectMeta.getName()))
                                                    .orElseGet(() -> null)
                                    );
                                }
                            }


                            k8sCRDIssuers.add(issuer);
                        }
                    }
                }
            }
        }

        return k8sCRDIssuers;
    }

    public K8sCRDIssuerVO getCertPrivateIssuer(CertIssuerScope scope, Integer clusterSeq, String namespaceName, String issuerName) throws Exception {
        IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
        ClusterVO cluster = clusterDao.getCluster(clusterSeq);
        return this.getCertPrivateIssuer(scope, cluster, namespaceName, issuerName);
    }

    /**
     * Cert-Private Issuer 배포정보 상세 조회
     *
     * @param scope
     * @param cluster
     * @param namespaceName
     * @param issuerName
     * @return
     * @throws Exception
     */
    public K8sCRDIssuerVO getCertPrivateIssuer(CertIssuerScope scope, ClusterVO cluster, String namespaceName, String issuerName) throws Exception {
        if (scope != null && cluster != null) {

            /** 클러스터 상태 체크 **/
            clusterStateService.checkClusterState(cluster);

            Map<String, Object> issuerMap = Maps.newHashMap();
            switch (scope) {
                case CLUSTER -> {
                    issuerMap = crdResourceService.getCustomObject(cluster, null, issuerName, K8sApiKindType.CLUSTER_ISSUER);
                }
                case NAMESPACED -> {
                    issuerMap = crdResourceService.getCustomObject(cluster, namespaceName, issuerName, K8sApiKindType.ISSUER);
                }
            }

            if (MapUtils.isNotEmpty(issuerMap)) {
                // gen issuer model
                K8sCRDIssuerVO issuer = this.genCertPrivateCRDIssuer(scope, cluster, issuerMap, null, null);

                // set Event (현재 클러스터 scope evnet를 조회하는 k8s api는 없음)
                if (scope == CertIssuerScope.NAMESPACED) {
                    String fieldSelector = String.format("%s=%s,%s=%s,%s=%s", "involvedObject.namespace", namespaceName, "involvedObject.name", issuerName, "involvedObject.kind", K8sApiKindType.ISSUER.getValue());
                    List<K8sEventVO> events = k8sResourceService.getEventByCluster(cluster.getClusterSeq(), namespaceName, fieldSelector, null, ContextHolder.exeContext());

                    issuer.setEvents(events);
                }

                return issuer;
            }
        }

        return null;
    }

    public K8sCRDIssuerIntegrateVO getCertPrivateIssuerConfig(DeployType deployType, CertIssuerScope scope, Integer clusterSeq, String namespaceName, String issuerName) throws Exception {
        IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
        ClusterVO cluster = clusterDao.getCluster(clusterSeq);
        return this.getCertPrivateIssuerConfig(deployType, scope, cluster, namespaceName, issuerName);
    }

    /**
     * Cert-Private Issuer 설정정보 상세 조회
     *
     * @param deployType
     * @param scope
     * @param cluster
     * @param namespaceName
     * @param issuerName
     * @return
     * @throws Exception
     */
    public K8sCRDIssuerIntegrateVO getCertPrivateIssuerConfig(DeployType deployType, CertIssuerScope scope, ClusterVO cluster, String namespaceName, String issuerName) throws Exception {
        K8sCRDIssuerIntegrateVO issuerIntegrate = new K8sCRDIssuerIntegrateVO();
        issuerIntegrate.setDeployType(deployType.getCode());
        if (cluster != null) {
            /** 클러스터 상태 체크 **/
            clusterStateService.checkClusterState(cluster);

            Map<String, Object> issuerMap = Maps.newHashMap();
            switch (scope) {
                case CLUSTER -> {
                    issuerMap = crdResourceService.getCustomObject(cluster, null, issuerName, K8sApiKindType.CLUSTER_ISSUER);
                }
                case NAMESPACED -> {
                    issuerMap = crdResourceService.getCustomObject(cluster, namespaceName, issuerName, K8sApiKindType.ISSUER);
                }
            }

            if (MapUtils.isNotEmpty(issuerMap)) {

                JSON k8sJson = new JSON();

                // objectMeta 객체
                V1ObjectMeta objectMeta = ServerUtils.getK8sObjectMetaInMap(issuerMap, k8sJson);
                // spec 객체
                Object specObj = MapUtils.getObject(issuerMap, KubeConstants.SPEC, Maps.newHashMap());
                Map<String, Object> specMap = (Map<String, Object>)specObj;

                if (deployType == DeployType.GUI) {
                    K8sCRDIssuerGuiVO issuerGui = new K8sCRDIssuerGuiVO();
                    issuerGui.setDeployType(DeployType.GUI.getCode());
                    issuerGui.setScope(scope.getCode());
                    issuerGui.setClusterSeq(cluster.getClusterSeq());
                    issuerGui.setClusterId(cluster.getClusterId());
                    issuerGui.setClusterName(cluster.getClusterName());
                    issuerGui.setNamespace(objectMeta.getNamespace());
                    issuerGui.setName(objectMeta.getName());
                    issuerGui.setLabels(objectMeta.getLabels());
                    issuerGui.setAnnotations(objectMeta.getAnnotations());
                    // issueType only one
                    for (String key : specMap.keySet()) {
                        issuerGui.setIssueType(CertIssueType.valueOf(key).getCode());
                        break;
                    }
                    switch (CertIssueType.valueOf(issuerGui.getIssueType())) {
                        case selfSigned -> {
                            String strJson = k8sJson.serialize(specMap.get(issuerGui.getIssueType()));
                            issuerGui.setSelfSigned(k8sJson.getGson().fromJson(strJson, K8sCRDIssuerSelfSignedVO.class));
                        }
                        case ca -> {
                            String strJson = k8sJson.serialize(specMap.get(issuerGui.getIssueType()));
                            issuerGui.setCa(k8sJson.getGson().fromJson(strJson, K8sCRDIssuerCAVO.class));
                        }
                    }

                    issuerIntegrate = issuerGui;
                } else {
                    K8sCRDIssuerYamlVO issuerYaml = new K8sCRDIssuerYamlVO();
                    issuerYaml.setDeployType(DeployType.YAML.getCode());
                    issuerYaml.setScope(scope.getCode());
                    issuerYaml.setClusterSeq(cluster.getClusterSeq());
                    issuerYaml.setClusterId(cluster.getClusterId());
                    issuerYaml.setClusterName(cluster.getClusterName());
                    issuerYaml.setNamespace(objectMeta.getNamespace());
                    issuerYaml.setName(objectMeta.getName());
                    // issueType only one
                    for (String key : specMap.keySet()) {
                        issuerYaml.setIssueType(CertIssueType.valueOf(key).getCode());
                        break;
                    }

                    issuerYaml.setYaml(Yaml.getSnakeYaml().dumpAsMap(issuerMap));
                    // ManagedFields, status 제거
                    issuerYaml.setYaml(Yaml.getSnakeYaml().dumpAsMap(K8sCRDSpecFactory.buildIssuer(issuerYaml)));

                    issuerIntegrate = issuerYaml;
                }
            } else {
                issuerIntegrate = null;
            }
        } else {
            issuerIntegrate = null;
        }

        return issuerIntegrate;
    }

    /**
     * Cert-Private Issuer 모델 생성
     *
     * @param scope
     * @param cluster
     * @param issuerMap
     * @param objectMeta
     * @param k8sJson
     * @return
     * @throws Exception
     */
    public K8sCRDIssuerVO genCertPrivateCRDIssuer(CertIssuerScope scope, ClusterVO cluster, Map<String, Object> issuerMap, V1ObjectMeta objectMeta, JSON k8sJson) throws Exception {
        if (scope != null && cluster != null && MapUtils.isNotEmpty(issuerMap)) {
            if (k8sJson == null) {
                k8sJson = new JSON();
            }

            // objectMeta 객체
            if (objectMeta == null) {
                objectMeta = ServerUtils.getK8sObjectMetaInMap(issuerMap, k8sJson);
            }
            // spec 객체
            Object specObj = MapUtils.getObject(issuerMap, KubeConstants.SPEC, Maps.newHashMap());
            Map<String, Object> specMap = (Map<String, Object>)specObj;
            // status 객체
            Object statusObj = MapUtils.getObject(issuerMap, "status", Maps.newHashMap());
            Map<String, Object> statusMap = (Map<String, Object>)statusObj;

            K8sCRDIssuerVO issuer = new K8sCRDIssuerVO();
            issuer.setScope(scope.getCode());
            issuer.setGroup((String)issuerMap.get(KubeConstants.APIVSERION));
            issuer.setClusterSeq(cluster.getClusterSeq());
            issuer.setClusterId(cluster.getClusterId());
            issuer.setClusterName(cluster.getClusterName());
            issuer.setNamespace(objectMeta.getNamespace());
            issuer.setName(objectMeta.getName());
            issuer.setLabels(objectMeta.getLabels());
            issuer.setAnnotations(objectMeta.getAnnotations());
            issuer.setCreationTimestamp(objectMeta.getCreationTimestamp());
            issuer.setOwnerReferences(ResourceUtil.setOwnerReference(objectMeta.getOwnerReferences()));
            // issueType only one
            for (String key : specMap.keySet()) {
                issuer.setIssueType(CertIssueType.valueOf(key).getCode());
                break;
            }
            switch (CertIssueType.valueOf(issuer.getIssueType())) {
                case selfSigned -> {
                    String strJson = k8sJson.serialize(specMap.get(issuer.getIssueType()));
                    issuer.setSelfSigned(k8sJson.getGson().fromJson(strJson, K8sCRDIssuerSelfSignedVO.class));
                }
                case ca -> {
                    String strJson = k8sJson.serialize(specMap.get(issuer.getIssueType()));
                    issuer.setCa(k8sJson.getGson().fromJson(strJson, K8sCRDIssuerCAVO.class));
                }
            }
            /** status **/
            // conditions
            if (statusMap.containsKey("conditions")) {
                String strJson = k8sJson.serialize(statusMap.get("conditions"));
                issuer.setConditions(k8sJson.getGson().fromJson(strJson, new TypeToken<List<K8sConditionVO>>(){}.getType()));
            }
            // acme
            if (statusMap.containsKey("acme")) {
                issuer.setStatusAcme(k8sJson.serialize(statusMap.get("acme")));
            }

            issuer.setDeployment(k8sJson.serialize(issuerMap));
            issuer.setDeploymentYaml(Yaml.getSnakeYaml().dumpAsMap(issuerMap));

            return issuer;
        }

        return null;
    }

    /**
     * Issuer Yaml 배포를 위한 기본 양식 생성
     *
     * @return
     * @throws Exception
     */
    public Map<String, Map<String, Object>> genCertPrivateIssuerDefaultYamlTemplate() throws Exception {
        Map<String, Map<String, Object>> template = Maps.newHashMap();

        /** CertIssuerScope.CLUSTER - ClusterIssuer **/
        template.put(CertIssuerScope.CLUSTER.getCode(), Maps.newLinkedHashMap());

        // apiversion, kind
        Map<String, Object> clusterIssuerMeta = Maps.newLinkedHashMap();
        clusterIssuerMeta.put(KubeConstants.APIVSERION, String.format("%s/%s", K8sApiGroupType.CERT_MANAGER_IO.getValue(), K8sApiType.V1.getValue()));
        clusterIssuerMeta.put(KubeConstants.KIND, CertIssuerScope.CLUSTER.getKind().getValue());

        // objectMeta
        V1ObjectMeta objectMetaCluster = new V1ObjectMeta();
        objectMetaCluster.setName("");
        clusterIssuerMeta.put(KubeConstants.META, objectMetaCluster);

        // selfSigned
        template.get(CertIssuerScope.CLUSTER.getCode()).put(CertIssueType.selfSigned.getCode(), this.genCertPrivateDefaultYamlTemplateSelfSigned(clusterIssuerMeta));

        // ca
        template.get(CertIssuerScope.CLUSTER.getCode()).put(CertIssueType.ca.getCode(), this.genCertPrivateDefaultYamlTemplateCa(clusterIssuerMeta));

        // acme
        template.get(CertIssuerScope.CLUSTER.getCode()).put(CertIssueType.acme.getCode(), this.genCertPrivateDefaultYamlTemplateAcme(clusterIssuerMeta));

        // vault
        template.get(CertIssuerScope.CLUSTER.getCode()).put(CertIssueType.vault.getCode(), this.genCertPrivateDefaultYamlTemplateVault(clusterIssuerMeta));

        // venafi
        template.get(CertIssuerScope.CLUSTER.getCode()).put(CertIssueType.venafi.getCode(), this.genCertPrivateDefaultYamlTemplateVenafi(clusterIssuerMeta));

        /** CertIssuerScope.NAMESPACED Issuer **/
        template.put(CertIssuerScope.NAMESPACED.getCode(), Maps.newHashMap());

        // apiversion, kind
        Map<String, Object> issuerMeta = Maps.newLinkedHashMap();
        issuerMeta.put(KubeConstants.APIVSERION, String.format("%s/%s", K8sApiGroupType.CERT_MANAGER_IO.getValue(), K8sApiType.V1.getValue()));
        issuerMeta.put(KubeConstants.KIND, CertIssuerScope.NAMESPACED.getKind().getValue());

        // objectMeta
        V1ObjectMeta objectMetaNamespace = new V1ObjectMeta();
        objectMetaNamespace.setName("");
        issuerMeta.put(KubeConstants.META, objectMetaNamespace);


        // selfSigned
        template.get(CertIssuerScope.NAMESPACED.getCode()).put(CertIssueType.selfSigned.getCode(), this.genCertPrivateDefaultYamlTemplateSelfSigned(issuerMeta));

        // ca
        template.get(CertIssuerScope.NAMESPACED.getCode()).put(CertIssueType.ca.getCode(), this.genCertPrivateDefaultYamlTemplateCa(issuerMeta));

        // acme
        template.get(CertIssuerScope.NAMESPACED.getCode()).put(CertIssueType.acme.getCode(), this.genCertPrivateDefaultYamlTemplateAcme(issuerMeta));

        // vault
        template.get(CertIssuerScope.NAMESPACED.getCode()).put(CertIssueType.vault.getCode(), this.genCertPrivateDefaultYamlTemplateVault(issuerMeta));

        // venafi
        template.get(CertIssuerScope.NAMESPACED.getCode()).put(CertIssueType.venafi.getCode(), this.genCertPrivateDefaultYamlTemplateVenafi(issuerMeta));

        return template;
    }

    private String genCertPrivateDefaultYamlTemplateSelfSigned(Map<String, Object> issuerMeta) throws Exception {
        /**
         * spec:
         *   selfSigned: {}
         */
        // selfSigned
        Map<String, Object> issuerSpec = Maps.newLinkedHashMap();
        issuerSpec.putAll(issuerMeta);
        Map<String, Object> issuerSelfSigned = Maps.newLinkedHashMap();
        issuerSelfSigned.put(CertIssueType.selfSigned.getCode(), Maps.newHashMap());
        issuerSpec.put(KubeConstants.SPEC, issuerSelfSigned);

        return Yaml.getSnakeYaml().dumpAsMap(issuerSpec);
    }

    private String genCertPrivateDefaultYamlTemplateCa(Map<String, Object> issuerMeta) throws Exception {
        /**
         * spec:
         *   ca:
         *     secretName: ca-key-pair
         */
        // ca
        Map<String, Object> issuerSpec = Maps.newLinkedHashMap();
        issuerSpec.putAll(issuerMeta);
        Map<String, Object> issuerCa = Maps.newLinkedHashMap();
        Map<String, Object> issuerCa1 = Maps.newLinkedHashMap();
        issuerCa1.put("secretName", "");
        issuerCa.put(CertIssueType.ca.getCode(), issuerCa1);
        issuerSpec.put(KubeConstants.SPEC, issuerCa);

        return Yaml.getSnakeYaml().dumpAsMap(issuerSpec);
    }

    private String genCertPrivateDefaultYamlTemplateAcme(Map<String, Object> issuerMeta) throws Exception {
        /**
         * spec:
         *   acme:
         *     email: my@example.com
         *     privateKeySecretRef:
         *       name: letsencrypt-staging
         *     server: https://acme-staging-v02.api.letsencrypt.org/directory
         *     solvers:
         *     - http01:
         *         ingress:
         *           ingressClassName: nginx
         */
        // acme
        Map<String, Object> issuerSpec = Maps.newLinkedHashMap();
        issuerSpec.putAll(issuerMeta);
        Map<String, Object> issuerAcme = Maps.newLinkedHashMap();
        Map<String, Object> issuerAcme1 = Maps.newLinkedHashMap();
        Map<String, Object> issuerAcme2_1 = Maps.newLinkedHashMap();
        Map<String, Object> issuerAcme2_2 = Maps.newLinkedHashMap();
        Map<String, Object> issuerAcme3 = Maps.newLinkedHashMap();
        Map<String, Object> issuerAcme4 = Maps.newLinkedHashMap();
        issuerAcme1.put("email", "my@example.com");
        issuerAcme2_1.put("name", "letsencrypt-staging");
        issuerAcme1.put("privateKeySecretRef", issuerAcme2_1);
        issuerAcme1.put("server", "https://acme-staging-v02.api.letsencrypt.org/directory");
        issuerAcme4.put("ingressClassName", "nginx");
        issuerAcme3.put("ingress", issuerAcme4);
        issuerAcme2_2.put("http01", issuerAcme3);
        issuerAcme1.put("solvers", issuerAcme2_2);
        issuerAcme.put(CertIssueType.acme.getCode(), issuerAcme1);
        issuerSpec.put(KubeConstants.SPEC, issuerAcme);

        return Yaml.getSnakeYaml().dumpAsMap(issuerSpec);
    }

    private String genCertPrivateDefaultYamlTemplateVault(Map<String, Object> issuerMeta) throws Exception {
        /**
         * spec:
         *   vault:
         *     path: pki_int/sign/example-dot-com
         *     server: https://vault.local
         *     caBundle: <base64 encoded caBundle PEM file>
         *     auth:
         *       appRole:
         *         path: approle
         *         roleId: "291b9d21-8ff5-..."
         *         secretRef:
         *           name: cert-manager-vault-approle
         *           key: secretId
         */
        // vault
        Map<String, Object> issuerSpec = Maps.newLinkedHashMap();
        issuerSpec.putAll(issuerMeta);
        Map<String, Object> issuerVault = Maps.newLinkedHashMap();
        Map<String, Object> issuerVault1 = Maps.newLinkedHashMap();
        Map<String, Object> issuerVault2 = Maps.newLinkedHashMap();
        Map<String, Object> issuerVault3 = Maps.newLinkedHashMap();
        Map<String, Object> issuerVault4 = Maps.newLinkedHashMap();
        issuerVault1.put("path", "pki_int/sign/example-dot-com");
        issuerVault1.put("server", "https://vault.local");
        issuerVault1.put("caBundle", "<base64 encoded caBundle PEM file>");
        issuerVault4.put("name", "cert-manager-vault-approle");
        issuerVault4.put("key", "secretId");
        issuerVault3.put("path", "approle");
        issuerVault3.put("roleId", "291b9d21-8ff5-...");
        issuerVault3.put("secretRef", issuerVault4);
        issuerVault2.put("appRole", issuerVault3);
        issuerVault1.put("auth", issuerVault2);
        issuerVault.put(CertIssueType.vault.getCode(), issuerVault1);
        issuerSpec.put(KubeConstants.SPEC, issuerVault);

        return Yaml.getSnakeYaml().dumpAsMap(issuerSpec);
    }

    private String genCertPrivateDefaultYamlTemplateVenafi(Map<String, Object> issuerMeta) throws Exception {
        /**
         * spec:
         *   venafi:
         *     zone: "My Application\My CIT" # Set this to <Application Name>\<Issuing Template Alias>
         *     cloud:
         *       apiTokenSecretRef:
         *         name: vaas-secret
         *         key: apikey
         */
        // venafi
        Map<String, Object> issuerSpec = Maps.newLinkedHashMap();
        issuerSpec.putAll(issuerMeta);
        Map<String, Object> issuerVenafi = Maps.newLinkedHashMap();
        Map<String, Object> issuerVenafi1 = Maps.newLinkedHashMap();
        Map<String, Object> issuerVenafi2 = Maps.newLinkedHashMap();
        Map<String, Object> issuerVenafi3 = Maps.newLinkedHashMap();
        issuerVenafi1.put("zone", "Set this to <Application Name>\\<Issuing Template Alias>");
        issuerVenafi3.put("name", "vaas-secret");
        issuerVenafi3.put("key", "apikey");
        issuerVenafi2.put("apiTokenSecretRef", issuerVenafi3);
        issuerVenafi1.put("cloud", issuerVenafi2);
        issuerVenafi.put(CertIssueType.venafi.getCode(), issuerVenafi1);
        issuerSpec.put(KubeConstants.SPEC, issuerVenafi);

        return Yaml.getSnakeYaml().dumpAsMap(issuerSpec);
    }


    /**
     * add Cert-Private Certificate
     *
     * @param certificate
     * @return
     * @throws Exception
     */
    public Map<String, Object> addCertPrivateCertificate(K8sCRDCertificateIntegrateVO certificate) throws Exception {

        // GUI 모델
        K8sCRDCertificateGuiVO certGui;
        // YAML 모델
        K8sCRDCertificateYamlVO certYaml;
        // 최종 CRD 모델
        Map<String, Object> crdMap = null;
        String namespace = null;
        Map<String, Object> result = null;

        IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
        ClusterVO cluster = null;

        if (DeployType.valueOf(certificate.getDeployType()) == DeployType.GUI) {
            certGui = (K8sCRDCertificateGuiVO)certificate;

            // check clusterSeq
            ExceptionMessageUtils.checkParameterRequired("clusterSeq", certGui.getClusterSeq());
            cluster = clusterDao.getCluster(certGui.getClusterSeq());
            clusterStateService.checkClusterState(cluster);

            // check cert-manager 설치 여부
            this.checkInstalledCertManagerExists(cluster);

            // valid
            this.validCertPrivateCertificateGUI(cluster, certGui.getNamespace(), certGui.getName(), certGui);

            // set namespace
            namespace = certGui.getNamespace();

            crdMap = K8sCRDSpecFactory.buildCertificate(certGui);
        }
        else {
            certYaml = (K8sCRDCertificateYamlVO)certificate;

            // check clusterSeq
            ExceptionMessageUtils.checkParameterRequired("clusterSeq", certYaml.getClusterSeq());
            cluster = clusterDao.getCluster(certYaml.getClusterSeq());
            clusterStateService.checkClusterState(cluster);

            // check cert-manager 설치 여부
            this.checkInstalledCertManagerExists(cluster);

            // set namespace
            ExceptionMessageUtils.checkParameterRequired("namespace", certYaml.getNamespace());
            namespace = certYaml.getNamespace();

            // spec 체크
            boolean isValidYaml = true;
            JSON k8sJson = new JSON();

            if (StringUtils.isNotBlank(certYaml.getYaml())) {
                // Yaml to Map
                Map<String, Object> objMap = Yaml.getSnakeYaml().load(certYaml.getYaml());
                if (MapUtils.isNotEmpty(objMap)) {
                    // objectMeta 객체
                    V1ObjectMeta objectMeta = ServerUtils.getK8sObjectMetaInMap(objMap, k8sJson);
                    // spec 객체
                    Object specObj = MapUtils.getObject(objMap, KubeConstants.SPEC, null);
                    if (objectMeta != null && specObj != null) {

                        // check yaml meta
                        objectMeta.setNamespace(namespace);
                        this.checkCRDYamlCommonMetaInfo(false, null, namespace, K8sApiKindType.CERTIFICATE, K8sApiGroupType.CERT_MANAGER_IO, K8sApiType.V1, objectMeta, objMap);

                        Map<String, Object> specMap = (Map<String, Object>)specObj;

                        certGui = this.convertCertPrivateCRDCertificateMapToGui(cluster, objMap, objectMeta, specMap, k8sJson);

                        // valid
                        this.validCertPrivateCertificateGUI(cluster, namespace, objectMeta.getName(), certGui);

                    } else {
                        isValidYaml = false;
                    }
                } else {
                    isValidYaml = false;
                }
            } else {
                String errMsg = "Issuer yaml is empty!!";
                throw new CocktailException(errMsg, ExceptionType.InvalidParameter_Empty, errMsg);
            }

            if (!isValidYaml) {
                String errMsg = "Invalid yaml parameter!!";
                throw new CocktailException(errMsg, ExceptionType.InvalidParameter, errMsg);
            } else {
                crdMap = K8sCRDSpecFactory.buildCertificate(certYaml);
            }
        }

        if (MapUtils.isNotEmpty(crdMap)) {

            result = crdResourceService.createCustomObject(cluster, namespace, K8sApiKindType.CERTIFICATE, crdMap);
        }

        return result;
    }

    /**
     * edit Cert-Private Certificate
     *
     * @param clusterSeq
     * @param namespaceName
     * @param certificateName
     * @param certificate
     * @return
     * @throws Exception
     */
    public Map<String, Object> editCertPrivateCertificate(Integer clusterSeq, String namespaceName, String certificateName, K8sCRDCertificateIntegrateVO certificate) throws Exception {
        // GUI 모델
        K8sCRDCertificateGuiVO certGui;
        // YAML 모델
        K8sCRDCertificateYamlVO certYaml;
        // 최종 CRD 모델
        Map<String, Object> crdMap = null;
        Map<String, Object> result = null;

        IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
        ClusterVO cluster = null;

        JSON k8sJson = new JSON();

        if (DeployType.valueOf(certificate.getDeployType()) == DeployType.GUI) {
            certGui = (K8sCRDCertificateGuiVO)certificate;

            // check clusterSeq
            ExceptionMessageUtils.checkParameterRequired("clusterSeq", certGui.getClusterSeq());
            if (!certGui.getClusterSeq().equals(clusterSeq)) {
                String errMsg = "Invalid clusterSeq parameter!!";
                throw new CocktailException(errMsg, ExceptionType.InvalidParameter, errMsg);
            }
            cluster = clusterDao.getCluster(clusterSeq);
            clusterStateService.checkClusterState(cluster);

            // valid
            this.validCertPrivateCertificateGUI(cluster, namespaceName, certificateName, certGui);

            // GUI 항목을 현재 Certificate spec과 Merge
            Map<String, Object> currCertMap = crdResourceService.getCustomObject(cluster, namespaceName, certificateName, K8sApiKindType.CERTIFICATE);
            if (MapUtils.isNotEmpty(currCertMap)) {

                /** current **/
                // current할  objectMeta 객체
                V1ObjectMeta currObjectMeta = ServerUtils.getK8sObjectMetaInMap(currCertMap, k8sJson);
                // current할 spec 객체
                Object currSpecObj = MapUtils.getObject(currCertMap, KubeConstants.SPEC, null);
                Map<String, Object> currSpecMap = (Map<String, Object>)currSpecObj;

                /** update **/
                Map<String, Object> uptCertMap = K8sCRDSpecFactory.buildCertificate(certGui);

                // update할  objectMeta 객체
                V1ObjectMeta uptObjectMeta = ServerUtils.getK8sObjectMetaInMap(uptCertMap, k8sJson);
                // update할 spec 객체
                Object uptSpecObj = MapUtils.getObject(uptCertMap, KubeConstants.SPEC, null);
                Map<String, Object> uptSpecMap = (Map<String, Object>)uptSpecObj;

                // Merge current, update
                currObjectMeta.setLabels(uptObjectMeta.getLabels());
                currObjectMeta.setAnnotations(uptObjectMeta.getAnnotations());
                currObjectMeta.setManagedFields(null);
                for (CertCertificateGUIItem item : CertCertificateGUIItem.values()) {
                    currSpecMap.put(item.getCode(), uptSpecMap.get(item.getCode()));
                }

                currCertMap.put(KubeConstants.META, currObjectMeta);
                currCertMap.put(KubeConstants.SPEC, currSpecMap);
                currCertMap.remove(KubeConstants.LABELS_ADDON_STATUS);

            } else {
                String errMsg = String.format("Certificate [%s] not found]!!", certificateName);
                throw new CocktailException(errMsg, ExceptionType.ResourceNotFound, errMsg);
            }

            crdMap = currCertMap;

        } else {
            certYaml = (K8sCRDCertificateYamlVO)certificate;

            // check clusterSeq
            ExceptionMessageUtils.checkParameterRequired("clusterSeq", certYaml.getClusterSeq());
            if (!certYaml.getClusterSeq().equals(clusterSeq)) {
                String errMsg = "Invalid clusterSeq parameter!!";
                throw new CocktailException(errMsg, ExceptionType.InvalidParameter, errMsg);
            }
            cluster = clusterDao.getCluster(clusterSeq);
            clusterStateService.checkClusterState(cluster);

            // set namespace
            ExceptionMessageUtils.checkParameterRequired("namespace", certYaml.getNamespace());
            if (!StringUtils.equals(namespaceName, certYaml.getNamespace())) {
                String errMsg = "Invalid namespace parameter!!";
                throw new CocktailException(errMsg, ExceptionType.InvalidParameter, errMsg);
            }

            // check name
            ExceptionMessageUtils.checkParameter("name", certYaml.getName(), 256, true);
            if (!certYaml.getName().matches(KubeConstants.RULE_RESOURCE_NAME)) {
                throw new CocktailException("Certificate name is invalid", ExceptionType.InvalidParameter, ResourceUtil.getInvalidNameMsg("Certificate name is invalid"));
            } else {
                if (!StringUtils.equals(certificateName, certYaml.getName())) {
                    String errMsg = "Invalid name parameter!!";
                    throw new CocktailException(errMsg, ExceptionType.InvalidParameter, errMsg);
                }
            }

            boolean isValidYaml = true;
            if (StringUtils.isNotBlank(certYaml.getYaml())) {
                // Yaml to Map
                Map<String, Object> objMap = Yaml.getSnakeYaml().load(certYaml.getYaml());

                if (MapUtils.isNotEmpty(objMap)) {
                    // objectMeta 객체
                    V1ObjectMeta objectMeta = ServerUtils.getK8sObjectMetaInMap(objMap, k8sJson);
                    // spec 객체
                    Object specObj = MapUtils.getObject(objMap, KubeConstants.SPEC, null);

                    if (objectMeta != null && specObj != null) {

                        // check yaml meta
                        this.checkCRDYamlCommonMetaInfo(false, certificateName, namespaceName, K8sApiKindType.CERTIFICATE, K8sApiGroupType.CERT_MANAGER_IO, K8sApiType.V1, objectMeta, objMap);

                        Map<String, Object> specMap = (Map<String, Object>)specObj;

                        certGui = this.convertCertPrivateCRDCertificateMapToGui(cluster, objMap, objectMeta, specMap, k8sJson);

                        // valid
                        this.validCertPrivateCertificateGUI(cluster, namespaceName, certificateName, certGui);

                        crdMap = K8sCRDSpecFactory.buildCertificate(certYaml);
                    } else {
                        isValidYaml = false;
                    }
                } else {
                    isValidYaml = false;
                }
            } else {
                String errMsg = "yaml is empty!!";
                throw new CocktailException("yaml is empty!!", ExceptionType.InvalidParameter_Empty, errMsg);
            }

            if (!isValidYaml) {
                String errMsg = "Invalid yaml parameter!!";
                throw new CocktailException(errMsg, ExceptionType.InvalidParameter, errMsg);
            } else {
                crdMap = K8sCRDSpecFactory.buildCertificate(certYaml);
            }
        }

        if (MapUtils.isNotEmpty(crdMap)) {
            K8sCRDYamlVO crdYaml = new K8sCRDYamlVO();
            crdYaml.setYaml(Yaml.getSnakeYaml().dumpAsMap(crdMap));
            result = crdResourceService.replaceCustomObject(cluster, namespaceName, certificateName, K8sApiKindType.CERTIFICATE, crdYaml);
        }

        return result;
    }

    public void validCertPrivateCertificateGUI(ClusterVO cluster, String namespaceName, String certificateName, K8sCRDCertificateGuiVO certGui) throws Exception {
        // set namespace
        ExceptionMessageUtils.checkParameterRequired("namespace", certGui.getNamespace());
        if (!StringUtils.equals(namespaceName, certGui.getNamespace())) {
            String errMsg = "Invalid namespace parameter!!";
            throw new CocktailException(errMsg, ExceptionType.InvalidParameter, errMsg);
        }

        // check name
        ExceptionMessageUtils.checkParameter("name", certGui.getName(), 256, true);
        if (!certGui.getName().matches(KubeConstants.RULE_RESOURCE_NAME)) {
            throw new CocktailException("Certificate name is invalid", ExceptionType.InvalidParameter, ResourceUtil.getInvalidNameMsg("Certificate name is invalid"));
        } else {
            if (!StringUtils.equals(certificateName, certGui.getName())) {
                String errMsg = "Invalid name parameter!!";
                throw new CocktailException(errMsg, ExceptionType.InvalidParameter, errMsg);
            }
        }

        if (StringUtils.isNotBlank(certGui.getCommonName())
                || CollectionUtils.isNotEmpty(certGui.getDnsNames())
                || CollectionUtils.isNotEmpty(certGui.getUris())
                || CollectionUtils.isNotEmpty(certGui.getIpAddresses())
                || CollectionUtils.isNotEmpty(certGui.getEmailAddresses())
        ) {

        } else {
            String errMsg = "Invalid value. at least one of commonName, dnsNames, uris, ipAddresses, or emailAddresses must be set.";
            throw new CocktailException(errMsg, ExceptionType.InvalidParameter, errMsg);
        }

        // check secretName
        ExceptionMessageUtils.checkParameter("secretName", certGui.getSecretName(), 256, true);
        if (!certGui.getSecretName().matches(KubeConstants.RULE_RESOURCE_NAME)) {
            throw new CocktailException("Certificate secretName is invalid", ExceptionType.InvalidParameter, ResourceUtil.getInvalidNameMsg("Certificate secretName is invalid"));
        }

        // check issuerRef
        if (certGui.getIssuerRef() != null) {
            ExceptionMessageUtils.checkParameter("name", certGui.getIssuerRef().getName(), 256, true);
            if (!certGui.getIssuerRef().getName().matches(KubeConstants.RULE_RESOURCE_NAME)) {
                throw new CocktailException("Certificate issuerRef.name is invalid", ExceptionType.InvalidParameter, ResourceUtil.getInvalidNameMsg("Certificate issuerRef.name is invalid"));
            }
            if (cluster != null) {
                // group = cert-manager.io 인데, kind가 다른 issuer인지 체크
                // kind in ClusterIssuer, Issuer 인데, group이 cert-manager.io가 맞는지 체크
                if ((
                        StringUtils.equals(certGui.getIssuerRef().getGroup(), K8sApiGroupType.CERT_MANAGER_IO.getValue())
                            && StringUtils.isNotBlank(certGui.getIssuerRef().getKind())
                            && !StringUtils.equalsAny(certGui.getIssuerRef().getKind(), K8sApiKindType.CLUSTER_ISSUER.getValue(), K8sApiKindType.ISSUER.getValue()) )
                        || (
                                StringUtils.equalsAny(certGui.getIssuerRef().getKind(), K8sApiKindType.CLUSTER_ISSUER.getValue(), K8sApiKindType.ISSUER.getValue())
                                    && StringUtils.isNotBlank(certGui.getIssuerRef().getGroup())
                                    && !StringUtils.equals(certGui.getIssuerRef().getGroup(), K8sApiGroupType.CERT_MANAGER_IO.getValue()) )
                ) {
                    String errMsg = String.format("Invalid issuer.[group: %s, kind: %s, name: %s]", certGui.getIssuerRef().getGroup(), certGui.getIssuerRef().getKind(), certGui.getIssuerRef().getName());
                    throw new CocktailException(errMsg, ExceptionType.InvalidParameter, errMsg);
                }

                // 실제로 issuer가 있는 지 체크
                if ( (StringUtils.isBlank(certGui.getIssuerRef().getGroup())
                        || StringUtils.startsWith(certGui.getIssuerRef().getGroup(), K8sApiGroupType.CERT_MANAGER_IO.getValue()))
                        && (StringUtils.isBlank(certGui.getIssuerRef().getKind())
                            || StringUtils.equalsAny(certGui.getIssuerRef().getKind(), K8sApiKindType.CLUSTER_ISSUER.getValue(), K8sApiKindType.ISSUER.getValue()))
                ) {
                    String kind = certGui.getIssuerRef().getKind();
                    if (StringUtils.isBlank(kind)) {
                        kind = K8sApiKindType.ISSUER.getValue();
                    }
                    K8sApiKindType kindType = K8sApiKindType.findKindTypeByValue(kind);
                    if (this.getCertPrivateIssuer(CertIssuerScope.kindOf(kindType), cluster, namespaceName, certGui.getIssuerRef().getName()) == null) {
                        String errMsg = "Issuer not found.";
                        throw new CocktailException(errMsg, ExceptionType.ResourceNotFound, errMsg);
                    }
                }
            }
        } else {
            String errMsg = "Certificate issuerRef is empty!!";
            throw new CocktailException(errMsg, ExceptionType.InvalidParameter_Empty, errMsg);
        }

        // check usages
        if (CollectionUtils.isNotEmpty(certGui.getUsages())) {
            for (String usage : certGui.getUsages()) {
                if (CertUsages.fromCode(usage) == null) {
                    String errMsg = String.format("Unexpected usage value [%s].", usage);
                    throw new CocktailException(errMsg, ExceptionType.InvalidParameter, errMsg);
                }
            }
        }

        // check duration
        if (StringUtils.isNotBlank(certGui.getDuration())) {
            if (!this.validDuration(certGui.getDuration())) {
                String errMsg = String.format("Invalid duration[%s] format!!", certGui.getDuration());
                throw new CocktailException(errMsg, ExceptionType.InvalidParameter, errMsg);
            }
        }

        // check RenewBefore
        if (StringUtils.isNotBlank(certGui.getRenewBefore())) {
            if (!this.validDuration(certGui.getRenewBefore())) {
                String errMsg = String.format("Invalid renewBefore[%s] format!!", certGui.getRenewBefore());
                throw new CocktailException(errMsg, ExceptionType.InvalidParameter, errMsg);
            }
        }
    }

    private boolean validDuration(String durationString) {
        try {
            Pattern pattern = Pattern.compile("(\\d+)(\\D+)");
            Matcher matcher = pattern.matcher(durationString);

            int valid = 0;

            while (matcher.find()) {
//                int amount = Integer.parseInt(matcher.group(1));
                String unit = matcher.group(2);

                switch (unit) {
                    case "ns":
                    case "us":
                    case "ms":
                    case "s":
                    case "m":
                    case "h":
                        valid += 1;
                        break;
                    default:
                        valid *= 0;
                        break;
                }
            }

            return BooleanUtils.toBoolean(valid);
        } catch (NumberFormatException e) {
            log.warn(String.format("Invalid duration[%s] format!!", durationString), e);
            return false;
        }
    }

    public void deleteCertPrivateCertificate(Integer clusterSeq, String namespaceName, String certificateName) throws Exception {
        IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
        ClusterVO cluster = clusterDao.getCluster(clusterSeq);
        this.deleteCertPrivateCertificate(cluster, namespaceName, certificateName);
    }

    /**
     * delete Cert-Private Certificate
     *
     * @param cluster
     * @param namespaceName
     * @param certificateName
     * @throws Exception
     */
    public void deleteCertPrivateCertificate(ClusterVO cluster, String namespaceName, String certificateName) throws Exception {
        if (cluster != null) {
            /** 클러스터 상태 체크 **/
            clusterStateService.checkClusterState(cluster);

            // TODO: issuerRef 에서 사용시 삭제 제한

            crdResourceService.deleteCustomObject(cluster, namespaceName, certificateName, K8sApiKindType.CERTIFICATE);
        }
    }

    /**
     * Cert-Private Certificate 목록 조회
     *
     * @param accountSeq
     * @param clusterSeq
     * @param withEvent - 이벤트 정보 조회 여부
     * @return
     * @throws Exception
     */
    public List<K8sCRDCertificateVO> getCertPrivateCertificates(Integer accountSeq, Integer clusterSeq, boolean withEvent) throws Exception {
        IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
        List<ClusterVO> clusters = clusterDao.getClusters(accountSeq, null, clusterSeq, null, null, "Y");

        List<K8sCRDCertificateVO> k8sCRDCertificates = Lists.newArrayList();

        if (CollectionUtils.isNotEmpty(clusters)) {
            // Map<clusterSeq, Map<K8sApiKindType, List<Map<String, Object>>>>
            Map<Integer, Map<K8sApiKindType, List<Map<String, Object>>>> certMap = Maps.newHashMap();
            // Map<clusterSeq, Map<kind, Map<namespace, Map<name, List<K8sEventVO>>>>>
            Map<Integer, Map<String, Map<String, Map<String, List<K8sEventVO>>>>> eventMap = Maps.newHashMap();
            Map<Integer, ClusterVO> clusterMap = Maps.newHashMap();

            for (ClusterVO cluster : clusters) {
                if (clusterStateService.isClusterRunning(cluster)) {
                    clusterMap.put(cluster.getClusterSeq(), cluster);
                    certMap.put(cluster.getClusterSeq(), Maps.newHashMap());

                    // Certificate
                    certMap.get(cluster.getClusterSeq()).put(K8sApiKindType.CERTIFICATE, Lists.newArrayList());
                    List<Map<String, Object>> certificates = crdResourceService.getCustomObjects(cluster, null, K8sApiKindType.CERTIFICATE, null);
                    if (CollectionUtils.isNotEmpty(certificates)) {
                        certMap.get(cluster.getClusterSeq()).get(K8sApiKindType.CERTIFICATE).addAll(certificates);
                    }

                    // Event
                    if (withEvent) {
                        List<K8sEventVO> events = k8sResourceService.convertEventDataList(cluster, null, null, null);
                        if (CollectionUtils.isNotEmpty(events)) {
                            eventMap.put(cluster.getClusterSeq(), Maps.newHashMap());

                            for (K8sEventVO eventRow : events) {
                                if (!eventMap.get(cluster.getClusterSeq()).containsKey(eventRow.getKind())) {
                                    eventMap.get(cluster.getClusterSeq()).put(eventRow.getKind(), Maps.newHashMap());
                                }
                                if (!eventMap.get(cluster.getClusterSeq()).get(eventRow.getKind()).containsKey(eventRow.getNamespace())) {
                                    eventMap.get(cluster.getClusterSeq()).get(eventRow.getKind()).put(eventRow.getNamespace(), Maps.newHashMap());
                                }
                                if (!eventMap.get(cluster.getClusterSeq()).get(eventRow.getKind()).get(eventRow.getNamespace()).containsKey(eventRow.getName())) {
                                    eventMap.get(cluster.getClusterSeq()).get(eventRow.getKind()).get(eventRow.getNamespace()).put(eventRow.getName(), Lists.newArrayList());
                                }

                                eventMap.get(cluster.getClusterSeq()).get(eventRow.getKind()).get(eventRow.getNamespace()).get(eventRow.getName()).add(eventRow);
                            }
                        }
                    }
                }
            }

            // convert Map to Model
            if (MapUtils.isNotEmpty(certMap)) {
                JSON k8sJson = new JSON();
                // cluster Loop
                for (Map.Entry<Integer, Map<K8sApiKindType, List<Map<String, Object>>>> clusterEntry : certMap.entrySet()) {

                    ClusterVO cluster = clusterMap.get(clusterEntry.getKey());

                    // kind type Loop
                    for (Map.Entry<K8sApiKindType, List<Map<String, Object>>> kindEntry : clusterEntry.getValue().entrySet()) {

                        // item Loop
                        for (Map<String, Object> item : kindEntry.getValue()) {

                            // objectMeta 객체
                            V1ObjectMeta objectMeta = ServerUtils.getK8sObjectMetaInMap(item, k8sJson);

                            // gen certificate model
                            K8sCRDCertificateVO cert = this.genCertPrivateCRDCertificate(cluster, item, objectMeta, k8sJson);

                            // event
                            if (withEvent) {
                                if (eventMap.containsKey(clusterEntry.getKey())) {
                                    cert.setEvents(
                                            Optional.of(eventMap)
                                                    .map(e -> e.get(clusterEntry.getKey()))
                                                    .map(e -> e.get((String)item.get(KubeConstants.KIND)))
                                                    .map(e -> e.get(objectMeta.getNamespace()))
                                                    .map(e -> e.get(objectMeta.getName()))
                                                    .orElseGet(() -> null)
                                    );
                                }
                            }


                            k8sCRDCertificates.add(cert);
                        }
                    }
                }
            }
        }

        return k8sCRDCertificates;
    }

    public K8sCRDCertificateVO getCertPrivateCertificate(Integer clusterSeq, String namespaceName, String certificateName, boolean withCR) throws Exception {
        IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
        ClusterVO cluster = clusterDao.getCluster(clusterSeq);
        return this.getCertPrivateCertificate(cluster, namespaceName, certificateName, withCR);
    }

    /**
     * Cert-Private Certificate 상세 조회 - 배포정보
     *
     * @param cluster
     * @param namespaceName
     * @param certificateName
     * @param withCR
     * @return
     * @throws Exception
     */
    public K8sCRDCertificateVO getCertPrivateCertificate(ClusterVO cluster, String namespaceName, String certificateName, boolean withCR) throws Exception {

        if (cluster != null) {

            /** 클러스터 상태 체크 **/
            clusterStateService.checkClusterState(cluster);

            Map<K8sApiKindType, List<Map<String, Object>>> certReqMap = Maps.newHashMap();
            // Map<kind, Map<namespace, Map<name, List<K8sEventVO>>>>
            Map<String, Map<String, Map<String, List<K8sEventVO>>>> eventMap = Maps.newHashMap();

            // Certificate
            Map<String, Object> certMap = crdResourceService.getCustomObject(cluster, namespaceName, certificateName, K8sApiKindType.CERTIFICATE);

            if (MapUtils.isNotEmpty(certMap)) {
                JSON k8sJson = new JSON();

                // Event 조회
                List<K8sEventVO> events = k8sResourceService.convertEventDataList(cluster, namespaceName, null, null);
                if (CollectionUtils.isNotEmpty(events)) {

                    for (K8sEventVO eventRow : events) {
                        if (!eventMap.containsKey(eventRow.getKind())) {
                            eventMap.put(eventRow.getKind(), Maps.newHashMap());
                        }
                        if (!eventMap.get(eventRow.getKind()).containsKey(eventRow.getNamespace())) {
                            eventMap.get(eventRow.getKind()).put(eventRow.getNamespace(), Maps.newHashMap());
                        }
                        if (!eventMap.get(eventRow.getKind()).get(eventRow.getNamespace()).containsKey(eventRow.getName())) {
                            eventMap.get(eventRow.getKind()).get(eventRow.getNamespace()).put(eventRow.getName(), Lists.newArrayList());
                        }

                        eventMap.get(eventRow.getKind()).get(eventRow.getNamespace()).get(eventRow.getName()).add(eventRow);
                    }
                }


                /** cert 셋팅 **/
                // objectMeta 객체
                V1ObjectMeta objectMeta = ServerUtils.getK8sObjectMetaInMap(certMap, k8sJson);

                // gen certificate model
                K8sCRDCertificateVO cert = this.genCertPrivateCRDCertificate(cluster, certMap, objectMeta, k8sJson);

                // event
                cert.setEvents(
                        Optional.of(eventMap)
                                .map(e -> e.get((String)certMap.get(KubeConstants.KIND)))
                                .map(e -> e.get(objectMeta.getNamespace()))
                                .map(e -> e.get(objectMeta.getName()))
                                .orElseGet(() -> null)
                );

                // issuer 셋팅
                if (cert.getIssuerRef() != null) {
                    // 참조 issuer가 cert-manager일 경우
                    if ( (StringUtils.isBlank(cert.getIssuerRef().getGroup())
                            || StringUtils.equals(cert.getIssuerRef().getGroup(), K8sApiGroupType.CERT_MANAGER_IO.getValue()))
                                && (StringUtils.isBlank(cert.getIssuerRef().getKind())
                                    || StringUtils.equalsAny(cert.getIssuerRef().getKind(), K8sApiKindType.CLUSTER_ISSUER.getValue(), K8sApiKindType.ISSUER.getValue()))
                    ) {
                        String kind = cert.getIssuerRef().getKind();
                        if (StringUtils.isBlank(kind)) {
                            kind = K8sApiKindType.ISSUER.getValue();
                        }
                        K8sApiKindType kindType = K8sApiKindType.findKindTypeByValue(kind);
                        cert.setIssuer(this.getCertPrivateIssuer(CertIssuerScope.kindOf(kindType), cluster, namespaceName, cert.getIssuerRef().getName()));
                    }
                }

                // 생성된 secret 정보 셋팅
                if (StringUtils.isNotBlank(cert.getSecretName())) {
                    SecretGuiVO secretGui = secretService.getSecret(cluster, namespaceName, cert.getSecretName(), true);
                    if (secretGui != null) {
                        if (MapUtils.isNotEmpty(secretGui.getData())) {
                            for (Map.Entry<String, String> entry : secretGui.getData().entrySet()) {
                                entry.setValue(new String(Base64Utils.decodeFromString(entry.getValue()), StandardCharsets.UTF_8));
                            }
                        }
                        cert.setSecret(secretGui);
                    }
                }

                if (withCR) {
                    List<Map<String, Object>> certReqList = crdResourceService.getCustomObjects(cluster, namespaceName, K8sApiKindType.CERTIFICATE_REQUEST, null);

                    if (CollectionUtils.isNotEmpty(certReqList)) {
                        // Map<APIVSERION, Map<KIND, Map<NAME, Map<String, Object>>>>
                        Map<String, Map<String, Map<String, Map<String, Object>>>> orderMap = Maps.newHashMap();
                        // Map<APIVSERION, Map<KIND, Map<NAME, Map<String, Object>>>>
                        Map<String, Map<String, Map<String, Map<String, Object>>>> challengeMap = Maps.newHashMap();

                        // issuer의 issue 유형이 ACME일 경우 order, challenge 조회
                        if (cert.getIssuer() != null && CertIssueType.acme == CertIssueType.valueOf(cert.getIssuer().getIssueType())) {
                            List<Map<String, Object>> orderList = crdResourceService.getCustomObjects(cluster, namespaceName, K8sApiKindType.ORDER, null);
                            List<Map<String, Object>> challengeList = crdResourceService.getCustomObjects(cluster, namespaceName, K8sApiKindType.CHALLENGE, null);

                            // orderList to orderMap
                            if (CollectionUtils.isNotEmpty(orderList)) {
                                for (Map<String, Object> orderItem : orderList) {
                                    V1ObjectMeta orderItemMeta = ServerUtils.getK8sObjectMetaInMap(orderItem, k8sJson);
                                    if (orderItemMeta != null && CollectionUtils.isNotEmpty(orderItemMeta.getOwnerReferences())) {
                                        V1OwnerReference ownerRef = orderItemMeta.getOwnerReferences().get(0);
                                        if (!orderMap.containsKey(ownerRef.getApiVersion())) {
                                            orderMap.put(ownerRef.getApiVersion(), Maps.newHashMap());
                                        }
                                        if (!orderMap.get(ownerRef.getApiVersion()).containsKey(ownerRef.getKind())) {
                                            orderMap.get(ownerRef.getApiVersion()).put(ownerRef.getKind(), Maps.newHashMap());
                                        }
                                        orderMap.get(ownerRef.getApiVersion()).get(ownerRef.getKind()).put(ownerRef.getName(), orderItem);
                                    }
                                }
                            }

                            // challengeList to challengeMap
                            if (CollectionUtils.isNotEmpty(challengeList)) {
                                for (Map<String, Object> challengeItem : challengeList) {
                                    V1ObjectMeta challengeItemMeta = ServerUtils.getK8sObjectMetaInMap(challengeItem, k8sJson);
                                    if (challengeItemMeta != null && CollectionUtils.isNotEmpty(challengeItemMeta.getOwnerReferences())) {
                                        V1OwnerReference ownerRef = challengeItemMeta.getOwnerReferences().get(0);
                                        if (!challengeMap.containsKey(ownerRef.getApiVersion())) {
                                            challengeMap.put(ownerRef.getApiVersion(), Maps.newHashMap());
                                        }
                                        if (!challengeMap.get(ownerRef.getApiVersion()).containsKey(ownerRef.getKind())) {
                                            challengeMap.get(ownerRef.getApiVersion()).put(ownerRef.getKind(), Maps.newHashMap());
                                        }
                                        challengeMap.get(ownerRef.getApiVersion()).get(ownerRef.getKind()).put(ownerRef.getName(), challengeItem);
                                    }
                                }
                            }
                        }

                        cert.setCertificateRequests(Lists.newArrayList());
                        for (Map<String, Object> certReqItem : certReqList) {
                            V1ObjectMeta certReqItemMeta = ServerUtils.getK8sObjectMetaInMap(certReqItem, k8sJson);
                            if (certReqItemMeta != null && CollectionUtils.isNotEmpty(certReqItemMeta.getOwnerReferences())) {
                                V1OwnerReference ownerRef = certReqItemMeta.getOwnerReferences().get(0);
                                if (
                                        StringUtils.equals(ownerRef.getApiVersion(), (String)certMap.get(KubeConstants.APIVSERION))
                                            && StringUtils.equals(ownerRef.getKind(), (String)certMap.get(KubeConstants.KIND))
                                            && StringUtils.equals(ownerRef.getName(), objectMeta.getName())
                                ) {
                                    // spec 객체
                                    Object certReqSpecObj = MapUtils.getObject(certReqItem, KubeConstants.SPEC, Maps.newHashMap());
                                    Map<String, Object> certReqSpecMap = (Map<String, Object>)certReqSpecObj;
                                    // status 객체
                                    Object certReqStatusObj = MapUtils.getObject(certReqItem, "status", Maps.newHashMap());
                                    Map<String, Object> certReqStatusMap = (Map<String, Object>)certReqStatusObj;

                                    K8sCRDCertificateRequestVO certReq = new K8sCRDCertificateRequestVO();
                                    certReq.setClusterSeq(cluster.getClusterSeq());
                                    certReq.setClusterId(cluster.getClusterId());
                                    certReq.setClusterName(cluster.getClusterName());
                                    certReq.setNamespace(certReqItemMeta.getNamespace());
                                    certReq.setName(certReqItemMeta.getName());
                                    certReq.setLabels(certReqItemMeta.getLabels());
                                    certReq.setAnnotations(certReqItemMeta.getAnnotations());
                                    certReq.setCreationTimestamp(certReqItemMeta.getCreationTimestamp());
                                    certReq.setOwnerReferences(ResourceUtil.setOwnerReference(certReqItemMeta.getOwnerReferences()));

                                    if (certReqSpecMap.containsKey(CertCertificateGUIItem.issuerRef.getCode())) {
                                        String strJson = k8sJson.serialize(certReqSpecMap.get(CertCertificateGUIItem.issuerRef.getCode()));
                                        certReq.setIssuerRef(k8sJson.getGson().fromJson(strJson, K8sCRDIssuerRefVO.class));
                                    }
                                    certReq.setDuration(MapUtils.getString(certReqSpecMap, CertCertificateGUIItem.duration.getCode(), null));
                                    if (certReqSpecMap.containsKey("extra")) {
                                        String strJson = k8sJson.serialize(certReqSpecMap.get("extra"));
                                        certReq.setExtra(k8sJson.getGson().fromJson(strJson, new TypeToken<Map<String, List<String>>>(){}.getType()));
                                    }
                                    if (certReqSpecMap.containsKey("groups")) {
                                        String strJson = k8sJson.serialize(certReqSpecMap.get("groups"));
                                        certReq.setGroups(k8sJson.getGson().fromJson(strJson, new TypeToken<List<String>>(){}.getType()));
                                    }
                                    certReq.setIsCA(MapUtils.getBoolean(certReqSpecMap, CertCertificateGUIItem.isCA.getCode(), null));
                                    certReq.setRequest(MapUtils.getString(certReqSpecMap, "request", null));
                                    certReq.setUid(MapUtils.getString(certReqSpecMap, "uid", null));
                                    if (certReqSpecMap.containsKey(CertCertificateGUIItem.usages.getCode())) {
                                        String strJson = k8sJson.serialize(certReqSpecMap.get(CertCertificateGUIItem.usages.getCode()));
                                        certReq.setUsages(k8sJson.getGson().fromJson(strJson, new TypeToken<List<String>>(){}.getType()));
                                    }
                                    if (certReqSpecMap.containsKey(CertCertificateGUIItem.uris.getCode())) {
                                        String strJson = k8sJson.serialize(certReqSpecMap.get(CertCertificateGUIItem.uris.getCode()));
                                        certReq.setUris(k8sJson.getGson().fromJson(strJson, new TypeToken<List<String>>(){}.getType()));
                                    }
                                    certReq.setUsername(MapUtils.getString(certReqSpecMap, "username", null));

                                    /** status **/
                                    // conditions
                                    if (certReqStatusMap.containsKey("conditions")) {
                                        String strJson = k8sJson.serialize(certReqStatusMap.get("conditions"));
                                        certReq.setConditions(k8sJson.getGson().fromJson(strJson, new TypeToken<List<K8sConditionVO>>(){}.getType()));

                                        for (K8sConditionVO c : certReq.getConditions()) {
                                            if (StringUtils.equalsIgnoreCase(c.getType(), "Ready")) {
                                                certReq.setReady(c.getStatus());
                                            } else if (StringUtils.equalsIgnoreCase(c.getType(), "Approved")) {
                                                certReq.setApproved(c.getStatus());
                                            } else if (StringUtils.equalsIgnoreCase(c.getType(), "Denied")) {
                                                certReq.setDenied(c.getStatus());
                                            }
                                        }
                                    }
                                    certReq.setDeployment(k8sJson.serialize(certReqItem));
                                    certReq.setDeploymentYaml(Yaml.getSnakeYaml().dumpAsMap(certReqItem));

                                    certReqStatusMap.remove("conditions");


                                    /** Event **/
                                    certReq.setEvents(
                                            Optional.of(eventMap)
                                                    .map(e -> e.get((String)certReqItem.get(KubeConstants.KIND)))
                                                    .map(e -> e.get(certReqItemMeta.getNamespace()))
                                                    .map(e -> e.get(certReqItemMeta.getName()))
                                                    .orElseGet(() -> null)
                                    );

                                    /** Order **/
                                    if (MapUtils.isNotEmpty(orderMap)) {
                                        Map<String, Object> currOrderMap =
                                                Optional.of(orderMap)
                                                        .map(o -> o.get((String)certReqItem.get(KubeConstants.APIVSERION)))
                                                        .map(o -> o.get((String)certReqItem.get(KubeConstants.KIND)))
                                                        .map(o -> o.get(certReqItemMeta.getName()))
                                                        .orElseGet(() -> null);

                                        if (MapUtils.isNotEmpty(currOrderMap)) {
                                            V1ObjectMeta currOrderMeta = ServerUtils.getK8sObjectMetaInMap(currOrderMap, k8sJson);
                                            // spec 객체
                                            Object currOrderSpecObj = MapUtils.getObject(currOrderMap, KubeConstants.SPEC, Maps.newHashMap());
                                            Map<String, Object> currOrderSpecMap = (Map<String, Object>) currOrderSpecObj;
                                            // status 객체
                                            Object currOrderStatusObj = MapUtils.getObject(currOrderMap, "status", Maps.newHashMap());
                                            Map<String, Object> currOrderStatusMap = (Map<String, Object>) currOrderStatusObj;

                                            K8sCRDOrderVO order = new K8sCRDOrderVO();
                                            order.setClusterSeq(cluster.getClusterSeq());
                                            order.setClusterId(cluster.getClusterId());
                                            order.setClusterName(cluster.getClusterName());
                                            order.setNamespace(currOrderMeta.getNamespace());
                                            order.setName(currOrderMeta.getName());
                                            order.setLabels(currOrderMeta.getLabels());
                                            order.setAnnotations(currOrderMeta.getAnnotations());
                                            order.setCreationTimestamp(currOrderMeta.getCreationTimestamp());
                                            order.setOwnerReferences(ResourceUtil.setOwnerReference(currOrderMeta.getOwnerReferences()));
                                            if (currOrderSpecMap.containsKey(CertCertificateGUIItem.issuerRef.getCode())) {
                                                String strJson = k8sJson.serialize(currOrderSpecMap.get(CertCertificateGUIItem.issuerRef.getCode()));
                                                order.setIssuerRef(k8sJson.getGson().fromJson(strJson, K8sCRDIssuerRefVO.class));
                                            }
                                            order.setStatus(currOrderStatusMap);
                                            order.setDeployment(k8sJson.serialize(currOrderMap));
                                            order.setDeploymentYaml(Yaml.getSnakeYaml().dumpAsMap(currOrderMap));
                                            order.setEvents(
                                                    Optional.of(eventMap)
                                                            .map(e -> e.get((String) currOrderMap.get(KubeConstants.KIND)))
                                                            .map(e -> e.get(currOrderMeta.getNamespace()))
                                                            .map(e -> e.get(currOrderMeta.getName()))
                                                            .orElseGet(() -> null)
                                            );

                                            /** Challenge **/
                                            if (MapUtils.isNotEmpty(challengeMap)) {
                                                Map<String, Object> currChallengeMap =
                                                        Optional.of(challengeMap)
                                                                .map(c -> c.get((String) currOrderMap.get(KubeConstants.APIVSERION)))
                                                                .map(c -> c.get((String) currOrderMap.get(KubeConstants.KIND)))
                                                                .map(c -> c.get(currOrderMeta.getName()))
                                                                .orElseGet(() -> null);

                                                if (MapUtils.isNotEmpty(currChallengeMap)) {
                                                    V1ObjectMeta currChallengeMeta = ServerUtils.getK8sObjectMetaInMap(currChallengeMap, k8sJson);
                                                    // spec 객체
                                                    Object currChallengeSpecObj = MapUtils.getObject(currChallengeMap, KubeConstants.SPEC, Maps.newHashMap());
                                                    Map<String, Object> currChallengeSpecMap = (Map<String, Object>) currChallengeSpecObj;
                                                    // status 객체
                                                    Object currChallengeStatusObj = MapUtils.getObject(currChallengeMap, "status", Maps.newHashMap());
                                                    Map<String, Object> currChallengeStatusMap = (Map<String, Object>) currChallengeStatusObj;

                                                    K8sCRDChallengeVO challenge = new K8sCRDChallengeVO();
                                                    challenge.setClusterSeq(cluster.getClusterSeq());
                                                    challenge.setClusterId(cluster.getClusterId());
                                                    challenge.setClusterName(cluster.getClusterName());
                                                    challenge.setNamespace(currChallengeMeta.getNamespace());
                                                    challenge.setName(currChallengeMeta.getName());
                                                    challenge.setLabels(currChallengeMeta.getLabels());
                                                    challenge.setAnnotations(currChallengeMeta.getAnnotations());
                                                    challenge.setCreationTimestamp(currChallengeMeta.getCreationTimestamp());
                                                    challenge.setOwnerReferences(ResourceUtil.setOwnerReference(currChallengeMeta.getOwnerReferences()));
                                                    if (currChallengeSpecMap.containsKey(CertCertificateGUIItem.issuerRef.getCode())) {
                                                        String strJson = k8sJson.serialize(currChallengeSpecMap.get(CertCertificateGUIItem.issuerRef.getCode()));
                                                        challenge.setIssuerRef(k8sJson.getGson().fromJson(strJson, K8sCRDIssuerRefVO.class));
                                                    }
                                                    challenge.setStatus(currChallengeStatusMap);
                                                    challenge.setDeployment(k8sJson.serialize(currChallengeMap));
                                                    challenge.setDeploymentYaml(Yaml.getSnakeYaml().dumpAsMap(currChallengeMap));
                                                    challenge.setEvents(
                                                            Optional.of(eventMap)
                                                                    .map(e -> e.get((String) currChallengeMap.get(KubeConstants.KIND)))
                                                                    .map(e -> e.get(currChallengeMeta.getNamespace()))
                                                                    .map(e -> e.get(currChallengeMeta.getName()))
                                                                    .orElseGet(() -> null)
                                                    );

                                                    order.setChallenge(challenge);

                                                }
                                            }
                                            certReq.setOrder(order);
                                        }
                                    }

                                    cert.getCertificateRequests().add(certReq);
                                }
                            }
                        }
                    }
                }

                return cert;
            }

        }


        return null;
    }

    public K8sCRDCertificateIntegrateVO getCertPrivateCertificateConfig(DeployType deployType, Integer clusterSeq, String namespaceName, String certificateName) throws Exception {
        IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
        ClusterVO cluster = clusterDao.getCluster(clusterSeq);
        return this.getCertPrivateCertificateConfig(deployType, cluster, namespaceName, certificateName);
    }

    /**
     * Cert-Private Certificate 설정정보 상세 조회
     *
     * @param deployType
     * @param cluster
     * @param namespaceName
     * @param certificateName
     * @return
     * @throws Exception
     */
    public K8sCRDCertificateIntegrateVO getCertPrivateCertificateConfig(DeployType deployType, ClusterVO cluster, String namespaceName, String certificateName) throws Exception {
        K8sCRDCertificateIntegrateVO certificateIntegrate = new K8sCRDCertificateIntegrateVO();
        certificateIntegrate.setDeployType(deployType.getCode());
        if (cluster != null) {
            /** 클러스터 상태 체크 **/
            clusterStateService.checkClusterState(cluster);

            Map<String, Object> certMap = crdResourceService.getCustomObject(cluster, namespaceName, certificateName, K8sApiKindType.CERTIFICATE);

            if (MapUtils.isNotEmpty(certMap)) {

                JSON k8sJson = new JSON();

                // objectMeta 객체
                V1ObjectMeta objectMeta = ServerUtils.getK8sObjectMetaInMap(certMap, k8sJson);
                // spec 객체
                Object specObj = MapUtils.getObject(certMap, KubeConstants.SPEC, Maps.newHashMap());
                Map<String, Object> specMap = (Map<String, Object>)specObj;

                if (deployType == DeployType.GUI) {
                    // convert to GUI
                    certificateIntegrate = this.convertCertPrivateCRDCertificateMapToGui(cluster, certMap, objectMeta, specMap, k8sJson);
                } else {
                    K8sCRDCertificateYamlVO certYaml = new K8sCRDCertificateYamlVO();
                    certYaml.setDeployType(DeployType.YAML.getCode());
                    certYaml.setClusterSeq(cluster.getClusterSeq());
                    certYaml.setClusterId(cluster.getClusterId());
                    certYaml.setClusterName(cluster.getClusterName());
                    certYaml.setNamespace(objectMeta.getNamespace());
                    certYaml.setName(objectMeta.getName());

                    certYaml.setYaml(Yaml.getSnakeYaml().dumpAsMap(certMap));
                    // ManagedFields, status 제거
                    certYaml.setYaml(Yaml.getSnakeYaml().dumpAsMap(K8sCRDSpecFactory.buildCertificate(certYaml)));

                    certificateIntegrate = certYaml;
                }
            } else {
                certificateIntegrate = null;
            }
        } else {
            certificateIntegrate = null;
        }

        return certificateIntegrate;
    }

    public K8sCRDCertificateGuiVO convertCertPrivateCRDCertificateMapToGui(ClusterVO cluster, Map<String, Object> certMap, V1ObjectMeta objectMeta, Map<String, Object> specMap, JSON k8sJson) throws Exception {

        if (k8sJson == null) {
            k8sJson = new JSON();
        }

        // objectMeta 객체
        if (objectMeta == null) {
            objectMeta = ServerUtils.getK8sObjectMetaInMap(certMap, k8sJson);
        }
        // spec 객체
        if (MapUtils.isEmpty(specMap)) {
            Object specObj = MapUtils.getObject(certMap, KubeConstants.SPEC, Maps.newHashMap());
            specMap = (Map<String, Object>) specObj;
        }

        K8sCRDCertificateGuiVO certGui = new K8sCRDCertificateGuiVO();
        certGui.setDeployType(DeployType.GUI.getCode());
        certGui.setClusterSeq(cluster.getClusterSeq());
        certGui.setClusterId(cluster.getClusterId());
        certGui.setClusterName(cluster.getClusterName());
        certGui.setNamespace(objectMeta.getNamespace());
        certGui.setName(objectMeta.getName());
        certGui.setLabels(objectMeta.getLabels());
        certGui.setAnnotations(objectMeta.getAnnotations());
        certGui.setIsCA(MapUtils.getBoolean(specMap, CertCertificateGUIItem.isCA.getCode(), null));
        certGui.setCommonName(MapUtils.getString(specMap, CertCertificateGUIItem.commonName.getCode(), null));
        if (specMap.containsKey(CertCertificateGUIItem.dnsNames.getCode())) {
            String strJson = k8sJson.serialize(specMap.get(CertCertificateGUIItem.dnsNames.getCode()));
            certGui.setDnsNames(k8sJson.getGson().fromJson(strJson, new TypeToken<List<String>>(){}.getType()));
        }
        if (specMap.containsKey(CertCertificateGUIItem.uris.getCode())) {
            String strJson = k8sJson.serialize(specMap.get(CertCertificateGUIItem.uris.getCode()));
            certGui.setUris(k8sJson.getGson().fromJson(strJson, new TypeToken<List<String>>(){}.getType()));
        }
        if (specMap.containsKey(CertCertificateGUIItem.ipAddresses.getCode())) {
            String strJson = k8sJson.serialize(specMap.get(CertCertificateGUIItem.ipAddresses.getCode()));
            certGui.setIpAddresses(k8sJson.getGson().fromJson(strJson, new TypeToken<List<String>>(){}.getType()));
        }
        if (specMap.containsKey(CertCertificateGUIItem.emailAddresses.getCode())) {
            String strJson = k8sJson.serialize(specMap.get(CertCertificateGUIItem.emailAddresses.getCode()));
            certGui.setEmailAddresses(k8sJson.getGson().fromJson(strJson, new TypeToken<List<String>>(){}.getType()));
        }
        certGui.setSecretName(MapUtils.getString(specMap, CertCertificateGUIItem.secretName.getCode(), null));
        if (specMap.containsKey(CertCertificateGUIItem.issuerRef.getCode())) {
            String strJson = k8sJson.serialize(specMap.get(CertCertificateGUIItem.issuerRef.getCode()));
            certGui.setIssuerRef(k8sJson.getGson().fromJson(strJson, K8sCRDIssuerRefVO.class));
        }
        certGui.setDuration(MapUtils.getString(specMap, CertCertificateGUIItem.duration.getCode(), null));
        certGui.setRenewBefore(MapUtils.getString(specMap, CertCertificateGUIItem.renewBefore.getCode(), null));
        certGui.setRevisionHistoryLimit(MapUtils.getInteger(specMap, CertCertificateGUIItem.revisionHistoryLimit.getCode(), null));
        if (specMap.containsKey(CertCertificateGUIItem.usages.getCode())) {
            String strJson = k8sJson.serialize(specMap.get(CertCertificateGUIItem.usages.getCode()));
            certGui.setUsages(k8sJson.getGson().fromJson(strJson, new TypeToken<List<String>>(){}.getType()));
            if (CollectionUtils.isNotEmpty(certGui.getUsages())) {
                List<String> usages = Lists.newArrayList();
                // Map<Value, Code>
                Map<String, String> usageMap = Arrays.stream(CertUsages.values()).collect(Collectors.toMap(CertUsages::getValue, CertUsages::getCode));
                certGui.getUsages().stream().filter(u -> (usageMap.containsKey(u))).forEach(u -> usages.add(usageMap.get(u)));
                certGui.setUsages(usages);
            }
        }

        return certGui;
    }

    /**
     * Cert-Private Certificate 모델 생성
     *
     * @param cluster
     * @param certMap
     * @param objectMeta
     * @param k8sJson
     * @return
     * @throws Exception
     */
    public K8sCRDCertificateVO genCertPrivateCRDCertificate(ClusterVO cluster, Map<String, Object> certMap, V1ObjectMeta objectMeta, JSON k8sJson) throws Exception {
        if (cluster != null && MapUtils.isNotEmpty(certMap)) {
            if (k8sJson == null) {
                k8sJson = new JSON();
            }

            // objectMeta 객체
            if (objectMeta == null) {
                objectMeta = ServerUtils.getK8sObjectMetaInMap(certMap, k8sJson);
            }
            // spec 객체
            Object specObj = MapUtils.getObject(certMap, KubeConstants.SPEC, Maps.newHashMap());
            Map<String, Object> specMap = (Map<String, Object>)specObj;
            // status 객체
            Object statusObj = MapUtils.getObject(certMap, "status", Maps.newHashMap());
            Map<String, Object> statusMap = (Map<String, Object>)statusObj;

            K8sCRDCertificateVO cert = new K8sCRDCertificateVO();
            cert.setClusterSeq(cluster.getClusterSeq());
            cert.setClusterId(cluster.getClusterId());
            cert.setClusterName(cluster.getClusterName());
            cert.setNamespace(objectMeta.getNamespace());
            cert.setName(objectMeta.getName());
            cert.setLabels(objectMeta.getLabels());
            cert.setAnnotations(objectMeta.getAnnotations());
            cert.setCreationTimestamp(objectMeta.getCreationTimestamp());
            cert.setOwnerReferences(ResourceUtil.setOwnerReference(objectMeta.getOwnerReferences()));

            cert.setIsCA(MapUtils.getBoolean(specMap, CertCertificateGUIItem.isCA.getCode(), null));
            cert.setCommonName(MapUtils.getString(specMap, CertCertificateGUIItem.commonName.getCode(), null));
            if (specMap.containsKey(CertCertificateGUIItem.dnsNames.getCode())) {
                String strJson = k8sJson.serialize(specMap.get(CertCertificateGUIItem.dnsNames.getCode()));
                cert.setDnsNames(k8sJson.getGson().fromJson(strJson, new TypeToken<List<String>>(){}.getType()));
            }
            if (specMap.containsKey(CertCertificateGUIItem.uris.getCode())) {
                String strJson = k8sJson.serialize(specMap.get(CertCertificateGUIItem.uris.getCode()));
                cert.setUris(k8sJson.getGson().fromJson(strJson, new TypeToken<List<String>>(){}.getType()));
            }
            if (specMap.containsKey(CertCertificateGUIItem.ipAddresses.getCode())) {
                String strJson = k8sJson.serialize(specMap.get(CertCertificateGUIItem.ipAddresses.getCode()));
                cert.setIpAddresses(k8sJson.getGson().fromJson(strJson, new TypeToken<List<String>>(){}.getType()));
            }
            if (specMap.containsKey(CertCertificateGUIItem.emailAddresses.getCode())) {
                String strJson = k8sJson.serialize(specMap.get(CertCertificateGUIItem.emailAddresses.getCode()));
                cert.setEmailAddresses(k8sJson.getGson().fromJson(strJson, new TypeToken<List<String>>(){}.getType()));
            }
            cert.setSecretName(MapUtils.getString(specMap, CertCertificateGUIItem.secretName.getCode(), null));
            if (specMap.containsKey(CertCertificateGUIItem.issuerRef.getCode())) {
                String strJson = k8sJson.serialize(specMap.get(CertCertificateGUIItem.issuerRef.getCode()));
                cert.setIssuerRef(k8sJson.getGson().fromJson(strJson, K8sCRDIssuerRefVO.class));
            }
            cert.setDuration(MapUtils.getString(specMap, CertCertificateGUIItem.duration.getCode(), null));
            cert.setRenewBefore(MapUtils.getString(specMap, CertCertificateGUIItem.renewBefore.getCode(), null));
            cert.setRevisionHistoryLimit(MapUtils.getInteger(specMap, CertCertificateGUIItem.revisionHistoryLimit.getCode(), null));
            if (specMap.containsKey(CertCertificateGUIItem.usages.getCode())) {
                String strJson = k8sJson.serialize(specMap.get(CertCertificateGUIItem.usages.getCode()));
                cert.setUsages(k8sJson.getGson().fromJson(strJson, new TypeToken<List<String>>(){}.getType()));
            }

            /** status **/
            // conditions
            if (statusMap.containsKey("conditions")) {
                String strJson = k8sJson.serialize(statusMap.get("conditions"));
                cert.setConditions(k8sJson.getGson().fromJson(strJson, new TypeToken<List<K8sConditionVO>>(){}.getType()));
            }

            cert.setDeployment(k8sJson.serialize(certMap));
            cert.setDeploymentYaml(Yaml.getSnakeYaml().dumpAsMap(certMap));

            statusMap.remove("conditions");
            cert.setStatus(statusMap);

            return cert;
        }

        return null;
    }

    /**
     * Certificate Yaml 배포를 위한 기본 양식 생성
     *
     * @return
     * @throws Exception
     */
    public String genCertPrivateCertificateDefaultYamlTemplate() throws Exception {

        // apiversion, kind
        Map<String, Object> meta = Maps.newLinkedHashMap();
        meta.put(KubeConstants.APIVSERION, String.format("%s/%s", K8sApiGroupType.CERT_MANAGER_IO.getValue(), K8sApiType.V1.getValue()));
        meta.put(KubeConstants.KIND, K8sApiKindType.CERTIFICATE.getValue());

        // objectMeta
        V1ObjectMeta objectMeta = new V1ObjectMeta();
        objectMeta.setName("");
        meta.put(KubeConstants.META, objectMeta);

        /**
         * spec:
         *   isCA: false
         *   commonName: my-selfsigned-ca
         *   secretName: my-secret
         *   duration: 8736h # 1 year
         *   renewBefore: 240h # 10 days
         *   issuerRef:
         *     kind: ClusterIssuer
         *     group: cert-manager.io
         *     name: my-issuer
         *   revisionHistoryLimit: 10
         */
        Map<String, Object> spec = Maps.newLinkedHashMap();
        spec.putAll(meta);
        Map<String, Object> spec1 = Maps.newLinkedHashMap();
        Map<String, Object> spec2 = Maps.newLinkedHashMap();
        spec1.put(CertCertificateGUIItem.isCA.getCode(), "false");
        spec1.put(CertCertificateGUIItem.commonName.getCode(), "my-selfsigned-ca");
        spec1.put(CertCertificateGUIItem.secretName.getCode(), "my-secret");
        spec1.put(CertCertificateGUIItem.duration.getCode(), "8736h");
        spec1.put(CertCertificateGUIItem.renewBefore.getCode(), "240h");
        spec2.put(KubeConstants.KIND, K8sApiKindType.CLUSTER_ISSUER.getValue());
        spec2.put("group", K8sApiGroupType.CERT_MANAGER_IO.getValue());
        spec2.put(KubeConstants.NAME, "my-issuer");
        spec1.put(CertCertificateGUIItem.issuerRef.getCode(), spec2);
        spec1.put(CertCertificateGUIItem.revisionHistoryLimit.getCode(), "10");
        spec.put(KubeConstants.SPEC, spec1);

        return Yaml.getSnakeYaml().dumpAsMap(spec);
    }

    /**
     * check yaml meta
     *
     * @param isAdd
     * @param name - isAdd=false 일 경우 셋팅
     * @param namespace
     * @param kindType
     * @param groupType
     * @param apiType
     * @param objectMeta
     * @param objMap
     * @throws Exception
     */
    private void checkCRDYamlCommonMetaInfo(boolean isAdd, String name, String namespace, K8sApiKindType kindType, K8sApiGroupType groupType, K8sApiType apiType, V1ObjectMeta objectMeta, Map<String, Object> objMap) throws Exception {

        if (objectMeta != null) {
            ExceptionMessageUtils.checkParameter("name", objectMeta.getName(), 256, true);
            if (!objectMeta.getName().matches(KubeConstants.RULE_RESOURCE_NAME)) {
                throw new CocktailException("Issuer name is invalid", ExceptionType.InvalidParameter, ResourceUtil.getInvalidNameMsg("name is invalid"));
            }

            if (!isAdd) {
                if (!StringUtils.equals(name, objectMeta.getName())) {
                    String errMsg = "Invalid name parameter!!";
                    throw new CocktailException(errMsg, ExceptionType.InvalidParameter, errMsg);
                }
            }

            if (!StringUtils.equals(namespace, objectMeta.getNamespace())) {
                String errMsg = "Invalid namespace parameter!!";
                throw new CocktailException(errMsg, ExceptionType.InvalidParameter, errMsg);
            }
        }

        if (kindType != null) {
            if (MapUtils.getObject(objMap, KubeConstants.KIND, null) == null
                    || (
                        MapUtils.getObject(objMap, KubeConstants.KIND, null) != null
                            && !StringUtils.equals(kindType.getValue(), (String) objMap.get(KubeConstants.KIND))
                    )
            ) {
                String errMsg = "Invalid kind parameter!!";
                throw new CocktailException(errMsg, ExceptionType.InvalidParameter, errMsg);
            }
        }

        if (groupType != null && apiType != null) {
            if (MapUtils.getObject(objMap, KubeConstants.APIVSERION, null) == null
                    || (
                        MapUtils.getObject(objMap, KubeConstants.APIVSERION, null) != null
                            && !StringUtils.equals(
                                String.format("%s/%s", groupType.getValue(), apiType.getValue())
                                , (String) objMap.get(KubeConstants.APIVSERION))
                    )
            ) {
                String errMsg = "Invalid apiVersion parameter!!";
                throw new CocktailException(errMsg, ExceptionType.InvalidParameter, errMsg);
            }
        }
    }

    /**
     * add Cert-Public Certificate
     *
     * @param certAdd
     * @throws Exception
     */
    @Transactional(transactionManager = "transactionManager")
    public void addCertPublicCertificate(PublicCertificateAddVO certAdd) throws Exception {
        ICertManagerMapper certDao = sqlSession.getMapper(ICertManagerMapper.class);

        if (certAdd != null) {
            ExceptionMessageUtils.checkParameterRequired("accountSeq", certAdd.getAccountSeq());
            ExceptionMessageUtils.checkParameter("publicCertificateName", certAdd.getPublicCertificateName(), 50, true);
            ExceptionMessageUtils.checkParameter("description", certAdd.getDescription(), 256, false);
            ExceptionMessageUtils.checkParameterRequired("tls.crt", certAdd.getClientAuthData());
            ExceptionMessageUtils.checkParameterRequired("tls.key", certAdd.getClientKeyData());

            // ca.crt
            if (StringUtils.isNotBlank(certAdd.getServerAuthData())) {
                certAdd.setServerAuthData(this.validCertPublicCaCrtWithEncrypt(certAdd.getServerAuthData()));
            }
            // tls.crt
            if (StringUtils.isNotBlank(certAdd.getClientAuthData())) {
                certAdd.setClientAuthData(this.validCertPublicTlsCrtWithEncrypt(certAdd.getClientAuthData()));
            }
            // tls.key
            if (StringUtils.isNotBlank(certAdd.getClientKeyData())) {
                certAdd.setClientKeyData(this.validCertPublicTlsKeyWithEncrypt(certAdd.getClientKeyData()));
            }

            // 등록
            int result = certDao.insertPublicCertificate(certAdd);
            if (result > 0) {
                certDao.insertPublicCertificateAccountMapping(certAdd.getAccountSeq(), certAdd.getPublicCertificateSeq(), ContextHolder.exeContext().getUserSeq());
            }
        } else {
            String errMsg = "parameter is empty!!";
            throw new CocktailException(errMsg, ExceptionType.InvalidParameter_Empty, errMsg);
        }
    }

    /**
     * edit Cert-Public Certificate
     *
     * @param certAdd
     * @throws Exception
     */
    @Transactional(transactionManager = "transactionManager")
    public void editCertPublicCertificate(PublicCertificateAddVO certAdd) throws Exception {
        ICertManagerMapper certDao = sqlSession.getMapper(ICertManagerMapper.class);

        if (certAdd != null) {
            ExceptionMessageUtils.checkParameterRequired("accountSeq", certAdd.getAccountSeq());
            ExceptionMessageUtils.checkParameterRequired("publicCertificateSeq", certAdd.getPublicCertificateSeq());
            ExceptionMessageUtils.checkParameter("publicCertificateName", certAdd.getPublicCertificateName(), 50, true);
            ExceptionMessageUtils.checkParameter("description", certAdd.getDescription(), 256, false);

            PublicCertificateDetailVO publicCertificate = certDao.getPublicCertificate(certAdd.getAccountSeq(), certAdd.getPublicCertificateSeq());
            if (publicCertificate != null) {
                // ca.crt
                if (StringUtils.isNotBlank(certAdd.getServerAuthData())) {
                    certAdd.setServerAuthData(this.validCertPublicCaCrtWithEncrypt(certAdd.getServerAuthData()));
                } else {
                    certAdd.setServerAuthData(publicCertificate.getServerAuthData());
                }
                // tls.crt
                if (StringUtils.isNotBlank(certAdd.getClientAuthData())) {
                    certAdd.setClientAuthData(this.validCertPublicTlsCrtWithEncrypt(certAdd.getClientAuthData()));
                } else {
                    certAdd.setClientAuthData(publicCertificate.getClientAuthData());
                }
                // tls.key
                if (StringUtils.isNotBlank(certAdd.getClientKeyData())) {
                    certAdd.setClientKeyData(this.validCertPublicTlsKeyWithEncrypt(certAdd.getClientKeyData()));
                } else {
                    certAdd.setClientKeyData(publicCertificate.getClientKeyData());
                }

                certDao.updatePublicCertificate(certAdd);
            } else {
                String errMsg = "public certificate not found!!";
                throw new CocktailException(errMsg, ExceptionType.ResourceNotFound, errMsg);
            }

        } else {
            String errMsg = "parameter is empty!!";
            throw new CocktailException(errMsg, ExceptionType.InvalidParameter_Empty, errMsg);
        }
    }

    /**
     * Cert-Public Certificate 목록 조회
     *
     * @param accountSeq
     * @return
     * @throws Exception
     */
    public List<PublicCertificateVO> getCertPublicCertificates(Integer accountSeq) throws Exception {
        ICertManagerMapper certDao = sqlSession.getMapper(ICertManagerMapper.class);
        return certDao.getPublicCertificates(accountSeq);
    }

    /**
     * Cert-Public Certificate 상세 조회
     *
     * @param accountSeq
     * @param publicCertificateSeq
     * @param showCaCrt
     * @param showTlsCrt
     * @param showTlsKey
     * @return
     * @throws Exception
     */
    public PublicCertificateDetailVO getCertPublicCertificate(Integer accountSeq, Integer publicCertificateSeq, boolean showCaCrt, boolean showTlsCrt, boolean showTlsKey) throws Exception {
        ICertManagerMapper certDao = sqlSession.getMapper(ICertManagerMapper.class);
        PublicCertificateDetailVO publicCertificate = certDao.getPublicCertificate(accountSeq, publicCertificateSeq);
        if (publicCertificate != null) {
            if (StringUtils.isNotBlank(publicCertificate.getServerAuthData())) {
                // crypto.decrypt -> base64.decode 변환
                // PEM 파일
                String decodedStr = this.decodeCert(publicCertificate.getServerAuthData());
                if (showCaCrt) {
                    publicCertificate.setServerAuth(decodedStr);
                }

                // 인증서 상세정보
                publicCertificate.setServerAuthDetail(this.setPublicCertificateCertInfo(decodedStr));
            }
            if (StringUtils.isNotBlank(publicCertificate.getClientAuthData())) {
                // crypto.decrypt -> base64.decode 변환
                // PEM 파일
                String decodedStr = this.decodeCert(publicCertificate.getClientAuthData());
                if (showTlsCrt) {
                    publicCertificate.setClientAuth(decodedStr);
                }

                // 인증서 상세정보
                publicCertificate.setClientAuthDetail(this.setPublicCertificateCertInfo(decodedStr));
            }
            if (StringUtils.isNotBlank(publicCertificate.getClientKeyData())) {
                // crypto.decrypt -> base64.decode 변환
                // PEM 파일
                String decodedStr = this.decodeCert(publicCertificate.getClientKeyData());
                if (showTlsKey) {
                    publicCertificate.setClientKey(decodedStr);
                }
            }
        }
        return publicCertificate;
    }

    /**
     * delete Cert-Public Certificate
     *
     * @param accountSeq
     * @param publicCertificateSeq
     * @return
     * @throws Exception
     */
    @Transactional(transactionManager = "transactionManager")
    public PublicCertificateDetailVO deleteCertPublicCertificate(Integer accountSeq, Integer publicCertificateSeq) throws Exception {
        ICertManagerMapper certDao = sqlSession.getMapper(ICertManagerMapper.class);
        PublicCertificateDetailVO publicCertificate = certDao.getPublicCertificate(accountSeq, publicCertificateSeq);
        if (publicCertificate != null) {
            // 삭제
            certDao.deletePublicCertificateAccountMapping(accountSeq, publicCertificateSeq);
            certDao.deletePublicCertificate(publicCertificateSeq);

            // 초기화
            publicCertificate.setDescription(null);
            publicCertificate.setServerAuthData(null);
            publicCertificate.setClientAuthData(null);
            publicCertificate.setClientKeyData(null);
        }

        return publicCertificate;
    }

    /**
     * 인증서 상세 정보 셋팅
     *
     * @param decodedStr - PEM 파일
     * @return
     * @throws Exception
     */
    private PublicCertificateCertInfoVO setPublicCertificateCertInfo(String decodedStr) throws Exception {
        if (StringUtils.isNotBlank(decodedStr)) {
            // 인증서
            X509Certificate certificate = this.getX509Certificate(decodedStr);

            PublicCertificateCertInfoVO info = new PublicCertificateCertInfoVO();
            info.setCA(certificate.getBasicConstraints() != -1);
            info.setVersion(certificate.getVersion());
            info.setSubjectDN(Optional.ofNullable(certificate.getSubjectX500Principal()).map(s -> s.toString()).orElseGet(null));
            info.setIssuerDN(Optional.ofNullable(certificate.getIssuerX500Principal()).map(s -> s.toString()).orElseGet(null));
            info.setSerialNumber(Optional.ofNullable(certificate.getSerialNumber()).map(s -> s.toString()).orElseGet(null));
            info.setNotBefore(certificate.getNotBefore());
            info.setNotAfter(certificate.getNotAfter());
            info.setSignatureAlgorithm(certificate.getSigAlgName());

            return info;
        }

        return null;
    }

    /**
     * 인증서 PEM파일 -> X509Certificate 변환
     *
     * @param decodedStr
     * @return
     * @throws Exception
     */
    private X509Certificate getX509Certificate(String decodedStr) throws Exception {
        if (StringUtils.isNotBlank(decodedStr)) {
            byte[] bytes = decodedStr.getBytes(StandardCharsets.UTF_8);
            InputStream inputStream = new ByteArrayInputStream(bytes);

            CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
            X509Certificate certificate = (X509Certificate) certFactory.generateCertificate(inputStream);

            return certificate;
        }

        return null;
    }

    /**
     * ca.crt 유효성 체크 및 암호화
     *
     * @param str - PEM 형식 또는 PEM 형식을 base64 인코딩한 문자열
     * @return
     * @throws Exception
     */
    private String validCertPublicCaCrtWithEncrypt(String str) throws Exception {
        String encryptStr = null;
        if (StringUtils.isNotBlank(str)) {
            String caCrt = StringUtils.trim(str);
            boolean isBase64 = this.isBase64(caCrt);
            String encodedStr = caCrt;
            String decodedStr = caCrt;
            if (isBase64) {
                decodedStr = new String(Base64Utils.decodeFromString(caCrt), StandardCharsets.UTF_8);
            } else {
                encodedStr = Base64Utils.encodeToString(caCrt.getBytes(StandardCharsets.UTF_8));
            }

            if (StringUtils.startsWith(decodedStr, "-----BEGIN CERTIFICATE-----")) {

                X509Certificate certificate = this.getX509Certificate(decodedStr);

                // CA 여부 확인
                boolean isCA = certificate.getBasicConstraints() != -1;

                if (isCA) {
                    encryptStr = CryptoUtils.encryptAES(encodedStr);
                } else {
                    String errMsg = "Invalid ca.crt parameter!!";
                    throw new CocktailException(errMsg, ExceptionType.InvalidParameter, String.format("%s - A CA certificate is required.", errMsg));
                }
            } else {
                String errMsg = "Invalid ca.crt parameter!!";
                throw new CocktailException(errMsg, ExceptionType.InvalidParameter, String.format("%s - Only PEM(Public Key Encapsulation Format) Supported. ", errMsg));
            }
        }

        return encryptStr;
    }

    /**
     * tls.crt 유효성 체크 및 암호화
     *
     * @param str - PEM 형식 또는 PEM 형식을 base64 인코딩한 문자열
     * @return
     * @throws Exception
     */
    private String validCertPublicTlsCrtWithEncrypt(String str) throws Exception {
        String encryptStr = null;
        if (StringUtils.isNotBlank(str)) {
            String tlsCrt = StringUtils.trim(str);
            boolean isBase64 = this.isBase64(tlsCrt);
            String encodedStr = tlsCrt;
            String decodedStr = tlsCrt;
            if (isBase64) {
                decodedStr = new String(Base64Utils.decodeFromString(tlsCrt), StandardCharsets.UTF_8);
            } else {
                encodedStr = Base64Utils.encodeToString(tlsCrt.getBytes(StandardCharsets.UTF_8));
            }

            if (StringUtils.startsWith(decodedStr, "-----BEGIN CERTIFICATE-----")) {

                try {
                    X509Certificate certificate = this.getX509Certificate(decodedStr);
                } catch (Exception e) {
                    String errMsg = "Invalid tls.crt format (x509 certificate)!!";
                    throw new CocktailException(errMsg, ExceptionType.InvalidParameter, errMsg);
                }

                encryptStr = CryptoUtils.encryptAES(encodedStr);
            } else {
                String errMsg = "Invalid tls.crt parameter!!";
                throw new CocktailException(errMsg, ExceptionType.InvalidParameter, String.format("%s - Only PEM(Public Key Encapsulation Format) Supported. ", errMsg));
            }
        }

        return encryptStr;
    }

    /**
     * tls.key 유효성 체크 및 암호화
     *
     * @param str - PEM 형식 또는 PEM 형식을 base64 인코딩한 문자열
     * @return
     * @throws Exception
     */
    private String validCertPublicTlsKeyWithEncrypt(String str) throws Exception {
        String encryptStr = null;
        if (StringUtils.isNotBlank(str)) {
            String tlsKey = StringUtils.trim(str);
            boolean isBase64 = this.isBase64(tlsKey);
            String encodedStr = tlsKey;
            String decodedStr = tlsKey;
            if (isBase64) {
                decodedStr = new String(Base64Utils.decodeFromString(tlsKey), StandardCharsets.UTF_8);
            } else {
                encodedStr = Base64Utils.encodeToString(tlsKey.getBytes(StandardCharsets.UTF_8));
            }

            if (StringUtils.startsWithAny(decodedStr
                    , "-----BEGIN PRIVATE KEY-----"
                    , "-----BEGIN RSA PRIVATE KEY-----"
                    , "-----BEGIN EC PRIVATE KEY-----")
            ) {
                encryptStr = CryptoUtils.encryptAES(encodedStr);
            } else {
                String errMsg = "Invalid tls.key parameter!!";
                throw new CocktailException(errMsg, ExceptionType.InvalidParameter, String.format("%s - Only PEM(Public Key Encapsulation Format) Supported. ", errMsg));
            }
        }

        return encryptStr;
    }

    /**
     * Base64 문자열 encode -> decode
     *
     * @param encodedStr - encoded base64 string
     * @return
     */
    private boolean isBase64(String encodedStr) {
        if (StringUtils.isNotBlank(encodedStr)) {
            try {
                return Base64.getDecoder().decode(encodedStr.getBytes(StandardCharsets.UTF_8)) != null;
            } catch (Exception e) {
                return false;
            }
        }

        return false;
    }

    /**
     * 복호화 > base64 decode 처리
     *
     * @param encodedStr
     * @return
     * @throws Exception
     */
    private String decodeCert(String encodedStr) throws Exception {
        return new String(Base64Utils.decodeFromString(CryptoUtils.decryptAES(encodedStr)), StandardCharsets.UTF_8);
    }

    private void checkInstalledCertManagerExists(ClusterVO cluster) throws Exception {
        String label = String.format("%s=%s,%s=%s,%s=%s"
                            , KubeConstants.META_LABELS_APP_NAME, "cert-manager"
                            , KubeConstants.META_LABELS_APP_INSTANCE, "cert-manager"
                            , KubeConstants.META_LABELS_APP_COMPONENT, "controller"
        );
        if (CollectionUtils.isEmpty(workloadResourceService.getPods(cluster, null, label))) {
            String errMsg = String.format("cert-manager is not installed on the cluster[%s].", cluster.getClusterId());
            throw new CocktailException(errMsg, ExceptionType.InvalidParameter, errMsg);
        }
    }
}
