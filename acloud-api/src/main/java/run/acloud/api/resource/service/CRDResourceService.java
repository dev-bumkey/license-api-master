package run.acloud.api.resource.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Maps;
import io.kubernetes.client.openapi.JSON;
import io.kubernetes.client.openapi.models.*;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.joda.time.DateTime;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.error.YAMLException;
import run.acloud.api.configuration.dao.IClusterMapper;
import run.acloud.api.configuration.vo.ClusterVO;
import run.acloud.api.cserver.enums.DeployType;
import run.acloud.api.cserver.util.ServerUtils;
import run.acloud.api.k8sextended.models.V1beta1CustomResourceDefinition;
import run.acloud.api.k8sextended.models.V1beta1CustomResourceDefinitionNames;
import run.acloud.api.k8sextended.models.V1beta1CustomResourceDefinitionSpec;
import run.acloud.api.k8sextended.models.V1beta1CustomResourceDefinitionVersion;
import run.acloud.api.k8sextended.util.Yaml;
import run.acloud.api.resource.constants.KubeConstants;
import run.acloud.api.resource.enums.*;
import run.acloud.api.resource.task.K8sCRDSpecFactory;
import run.acloud.api.resource.task.K8sWorker;
import run.acloud.api.resource.vo.*;
import run.acloud.commons.util.JsonUtils;
import run.acloud.commons.util.ObjectMapperUtils;
import run.acloud.framework.enums.ExceptionBiz;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;

import java.time.OffsetDateTime;
import java.util.*;

@Slf4j
@Service
public class CRDResourceService {
    private static final Integer MAX_TAIL_COUNT = 10000;
//    private static final String COCKTAIL_REGISTRY_SECRET = "cocktail-registry-secret";

    @Resource(name = "cocktailSession")
    private SqlSessionTemplate sqlSession;

    @Autowired
    private K8sWorker k8sWorker;

    /**
     * create CustomObject
     *
     * @param clusterSeq
     * @param namespace
     * @param kind
     * @param config
     * @return
     * @throws Exception
     */
    public Map<String, Object> createCustomObject(Integer clusterSeq, String namespace, K8sApiKindType kind, Object config) throws Exception {
        IClusterMapper dao = sqlSession.getMapper(IClusterMapper.class);
        ClusterVO cluster = dao.getCluster(clusterSeq);
        return this.createCustomObject(cluster, namespace, kind, config);
    }

    /**
     * create CustomObject
     *
     * @param cluster
     * @param namespace
     * @param kind
     * @param config
     * @return
     * @throws Exception
     */
    public Map<String, Object> createCustomObject(ClusterVO cluster, String namespace, K8sApiKindType kind, Object config) throws Exception {

        if (kind == K8sApiKindType.NETWORK_ATTACHMENT_DEFINITION) {
            K8sCRDNetAttachDefGuiVO netAttachDef = (K8sCRDNetAttachDefGuiVO)config;

            Map<String, Object> v1NetAttachDef = k8sWorker.getCustomObjectV1(
                    cluster, namespace, netAttachDef.getName()
                    , K8sApiGroupType.K8S_CNI_CNCF_IO.getValue(), K8sApiType.V1.getValue(), K8sApiCRDPluralType.NETWORK_ATTACHMENT_DEFINITION.getValue());

            if (v1NetAttachDef != null) {
                throw new CocktailException("NetAttachDef already exists!!", ExceptionType.K8sNetAttachDefAlreadyExists);
            }

            Map<String, Object> valueMap = ObjectMapperUtils.getMapper().readValue(netAttachDef.getConfig(), new TypeReference<Map<String, Object>>(){});
            netAttachDef.setConfig(JsonUtils.toGson(valueMap));
            Map<String, Object> param = K8sCRDSpecFactory.buildNetworkAttachmentDefinition(netAttachDef);
            return k8sWorker.createCustomObjectV1(cluster, namespace, K8sApiGroupType.K8S_CNI_CNCF_IO.getValue(), K8sApiType.V1.getValue(), K8sApiCRDPluralType.NETWORK_ATTACHMENT_DEFINITION.getValue(), param);
        } else if (kind == K8sApiKindType.CLUSTER_ISSUER) {
            Map<String, Object> param;
            if (config != null && config instanceof Map valueMap) {
                param = valueMap;
            } else {
                throw new CocktailException("Cert-Manager ClusterIssuer object parameter is invalid!!", ExceptionType.InvalidParameter);
            }

            K8sCRDYamlVO crdYaml = new K8sCRDYamlVO();
            crdYaml.setYaml(Yaml.getSnakeYaml().dumpAsMap(param));
            return this.createCustomObject(cluster, K8sApiGroupType.CERT_MANAGER_IO.getValue(), K8sApiType.V1.getValue(), K8sApiCRDPluralType.CLUSTER_ISSUERS.getValue(), crdYaml);
        } else if (kind == K8sApiKindType.ISSUER) {
            Map<String, Object> param;
            if (config != null && config instanceof Map valueMap) {
                param = valueMap;
            } else {
                throw new CocktailException("Cert-Manager Issuer object parameter is invalid!!", ExceptionType.InvalidParameter);
            }

            K8sCRDYamlVO crdYaml = new K8sCRDYamlVO();
            crdYaml.setYaml(Yaml.getSnakeYaml().dumpAsMap(param));
            cluster.setNamespaceName(namespace);
            return this.createCustomObject(cluster, K8sApiGroupType.CERT_MANAGER_IO.getValue(), K8sApiType.V1.getValue(), K8sApiCRDPluralType.ISSUERS.getValue(), crdYaml);
        } else if (kind == K8sApiKindType.CERTIFICATE) {
            Map<String, Object> param;
            if (config != null && config instanceof Map valueMap) {
                param = valueMap;
            } else {
                throw new CocktailException("Cert-Manager Certificate object parameter is invalid!!", ExceptionType.InvalidParameter);
            }

            K8sCRDYamlVO crdYaml = new K8sCRDYamlVO();
            crdYaml.setYaml(Yaml.getSnakeYaml().dumpAsMap(param));
            cluster.setNamespaceName(namespace);
            return this.createCustomObject(cluster, K8sApiGroupType.CERT_MANAGER_IO.getValue(), K8sApiType.V1.getValue(), K8sApiCRDPluralType.CERTIFICATES.getValue(), crdYaml);
        }

        return null;
    }

    public Map<String, Object> createCustomObject(Integer servicemapSeq, K8sApiKindType kind, Object config) throws Exception {
        ClusterVO cluster = this.setupCluster(servicemapSeq);
        return this.createCustomObject(cluster, cluster.getNamespaceName(), kind, config);
    }

    /**
     * Network Attachment Definition 생성 (Invoke from Snapshot Deployment)
     * @param servicemapSeq
     * @param kind
     * @param configs
     * @throws Exception
     */
    public void createCustomObjects(Integer servicemapSeq, K8sApiKindType kind, List<?> configs) throws Exception {
        ClusterVO cluster = this.setupCluster(servicemapSeq);

        if(CollectionUtils.isNotEmpty(configs)){
            for(Object objRow : configs){

                this.createCustomObject(servicemapSeq, kind, objRow);

                Thread.sleep(100);
            }
        }
    }

    /**
     * Network Attachment Definition 생성 (Invoke from Snapshot Deployment)
     * @param servicemapSeq
     * @param kind
     * @param netAttachDef
     * @throws Exception
     */
    public void createMultipleNetworkAttachmentDefinition(Integer servicemapSeq, K8sApiKindType kind, List<K8sCRDNetAttachDefIntegrateVO> netAttachDef) throws Exception {
        ClusterVO cluster = this.setupCluster(servicemapSeq);

        if(CollectionUtils.isNotEmpty(netAttachDef)){
            for(K8sCRDNetAttachDefIntegrateVO netRow : netAttachDef){
                if(DeployType.valueOf(netRow.getDeployType()) == DeployType.GUI) {
                    K8sCRDNetAttachDefGuiVO netGui = null;
                    try {
                        netGui = (K8sCRDNetAttachDefGuiVO) netRow;
                        this.createCustomObject(servicemapSeq, kind, netGui);
                        Thread.sleep(100);
                    }
                    catch (CocktailException ce) {
                        // 실패시 Log를 남기고 다음 처리를 계속한다..
                        log.error(String.format("Network Attachment Definition Deployment Failure : createMultipleNetworkAttachmentDefinition : %s\n%s", ce.getMessage(), JsonUtils.toGson(netGui)));
                    }
                    catch (Exception ex) {
                        // 실패시 Log를 남기고 다음 처리를 계속한다..
                        log.error(String.format("Network Attachment Definition Deployment Failure : createMultipleNetworkAttachmentDefinition : %s\n%s", ex.getMessage(), JsonUtils.toGson(netGui)));
                    }
                }
                else if(DeployType.valueOf(netRow.getDeployType()) == DeployType.YAML) {
                    K8sCRDNetAttachDefYamlVO netYaml = null;
                    try {
                        netYaml = (K8sCRDNetAttachDefYamlVO) netRow;
                        Map<String, Object> netObj = Yaml.getSnakeYaml().load(netYaml.getYaml());
                        k8sWorker.createCustomObjectV1(cluster, cluster.getNamespaceName(), K8sApiGroupType.K8S_CNI_CNCF_IO.getValue(), K8sApiType.V1.getValue(), K8sApiCRDPluralType.NETWORK_ATTACHMENT_DEFINITION.getValue(), netObj);
                        Thread.sleep(100);
                    }
                    catch (CocktailException ce) {
                        // 실패시 Log를 남기고 다음 처리를 계속한다..
                        log.error(String.format("Network Attachment Definition Deployment Failure : createMultipleNetworkAttachmentDefinition : %s\n%s", ce.getMessage(), JsonUtils.toGson(netYaml)));
                    }
                    catch (Exception ex) {
                        // 실패시 Log를 남기고 다음 처리를 계속한다..
                        log.error(String.format("Network Attachment Definition Deployment Failure : createMultipleNetworkAttachmentDefinition : %s\n%s", ex.getMessage(), JsonUtils.toGson(netYaml)));
                    }
                }
                else {
                    log.error(String.format("Invalid DeployType : createMultipleNetworkAttachmentDefinition : %s", JsonUtils.toGson(netRow)));
                }
            }
        }
    }

    /**
     * Custom Object 생성 (Invoke from Snapshot Deployment)
     *
     * @param servicemapSeq
     * @param customObjects
     * @throws Exception
     */
    public void createMultipleCustomObject(Integer servicemapSeq, List<CommonYamlVO> customObjects) throws Exception {
        ClusterVO cluster = this.setupCluster(servicemapSeq);

        if(CollectionUtils.isNotEmpty(customObjects)){
            for(CommonYamlVO customObj : customObjects){
                if(DeployType.valueOf(customObj.getDeployType()) == DeployType.GUI) {
                    log.error(String.format("DeployType.GUI not yet supported : createMultipleCustomObject : %s", JsonUtils.toGson(customObj)));
                }
                else if(DeployType.valueOf(customObj.getDeployType()) == DeployType.YAML) {
                    try {
                        Map<String, Object> customObjMap = Yaml.getSnakeYaml().load(customObj.getYaml());
                        k8sWorker.createCustomObjectV1(cluster, cluster.getNamespaceName(), customObj.getCustomObjectGroup(), customObj.getCustomObjectVersion(), customObj.getCustomObjectPlural(), customObjMap);
                        Thread.sleep(100);
                    }
                    catch (CocktailException ce) {
                        // 실패시 Log를 남기고 다음 처리를 계속한다..
                        log.error(String.format("Network Attachment Definition Deployment Failure : createMultipleCustomObject : %s\n%s", ce.getMessage(), JsonUtils.toGson(customObj)));
                    }
                    catch (Exception ex) {
                        // 실패시 Log를 남기고 다음 처리를 계속한다..
                        log.error(String.format("Network Attachment Definition Deployment Failure : createMultipleCustomObject : %s\n%s", ex.getMessage(), JsonUtils.toGson(customObj)));
                    }
                }
                else {
                    log.error(String.format("Invalid DeployType : createMultipleCustomObject : %s", JsonUtils.toGson(customObj)));
                }
            }
        }
    }

    public Map<String, Object> patchCustomObject(ClusterVO cluster, String namespace, K8sApiKindType kind, Object config) throws Exception{

        if (kind == K8sApiKindType.NETWORK_ATTACHMENT_DEFINITION) {
            K8sCRDNetAttachDefGuiVO netAttachDef = (K8sCRDNetAttachDefGuiVO)config;

            Map<String, Object> v1NetAttachDef = k8sWorker.getCustomObjectV1(
                    cluster, namespace, netAttachDef.getName()
                    , K8sApiGroupType.K8S_CNI_CNCF_IO.getValue(), K8sApiType.V1.getValue(), K8sApiCRDPluralType.NETWORK_ATTACHMENT_DEFINITION.getValue());

            if (v1NetAttachDef == null) {
                throw new CocktailException("NetAttachDef not found!!", ExceptionType.K8sNetAttachDefNotFound);
            }

            Map<String, Object> valueMap = ObjectMapperUtils.getMapper().readValue(netAttachDef.getConfig(), new TypeReference<Map<String, Object>>(){});
            netAttachDef.setConfig(JsonUtils.toGson(valueMap));
            Map<String, Object> param = K8sCRDSpecFactory.buildNetworkAttachmentDefinition(netAttachDef);
            return k8sWorker.patchCustomObjectV1(cluster, namespace, netAttachDef.getName(), K8sApiGroupType.K8S_CNI_CNCF_IO.getValue(), K8sApiType.V1.getValue(), K8sApiCRDPluralType.NETWORK_ATTACHMENT_DEFINITION.getValue(), param);
        }

        return null;
    }

    public Map<String, Object> patchCustomObject(Integer servicemapSeq, K8sApiKindType kind, Object config) throws Exception {
        ClusterVO cluster = this.setupCluster(servicemapSeq);
        return this.patchCustomObject(cluster, cluster.getNamespaceName(), kind, config);
    }

    public Map<String, Object> replaceCustomObjectWithYaml(Integer servicemapSeq, K8sApiKindType kind, Object config, Object configYaml) throws Exception {
        ClusterVO cluster = this.setupCluster(servicemapSeq);
        return this.replaceCustomObjectWithYaml(cluster, cluster.getNamespaceName(), kind, config, configYaml);
    }

    public Map<String, Object> replaceCustomObjectWithYaml(ClusterVO cluster, String namespace, K8sApiKindType kind, Object config, Object configYaml) throws Exception{
        if (kind == K8sApiKindType.NETWORK_ATTACHMENT_DEFINITION) {
            K8sCRDNetAttachDefGuiVO netAttachDefGui = (K8sCRDNetAttachDefGuiVO)config;
            K8sCRDNetAttachDefYamlVO netAttachDefYaml = (K8sCRDNetAttachDefYamlVO)configYaml;

            if(!cluster.getNamespaceName().equals(netAttachDefGui.getNamespace())) {
                throw new CocktailException("Can't change the namespace. (namespace is different)", ExceptionType.K8sNetAttachDefNameInvalid);
            }

            Map<String, Object> v1NetAttachDef = k8sWorker.getCustomObjectV1(
                    cluster, namespace, netAttachDefYaml.getName()
                    , K8sApiGroupType.K8S_CNI_CNCF_IO.getValue(), K8sApiType.V1.getValue(), K8sApiCRDPluralType.NETWORK_ATTACHMENT_DEFINITION.getValue());

            if (v1NetAttachDef == null || v1NetAttachDef.get(KubeConstants.META) == null) {
                throw new CocktailException("NetAttachDef not found!!", ExceptionType.K8sNetAttachDefNotFound);
            }

            Map<String, Object> param = Yaml.getSnakeYaml().load(netAttachDefYaml.getYaml());
            Map<String, Object> metadata = (Map<String, Object>)param.get(KubeConstants.META);
            if(metadata == null) {
                throw new CocktailException("Invalid NetAttachDef Data!!", ExceptionType.K8sNetAttachDefDataInvalid);
            }
            metadata.put("resourceVersion", ((Map<String, Object>)v1NetAttachDef.get(KubeConstants.META)).get("resourceVersion"));

            return k8sWorker.replaceCustomObjectV1(cluster, namespace, netAttachDefYaml.getName(), K8sApiGroupType.K8S_CNI_CNCF_IO.getValue(), K8sApiType.V1.getValue(), K8sApiCRDPluralType.NETWORK_ATTACHMENT_DEFINITION.getValue(), param);
//            return k8sWorker.patchCustomObjectV1(cluster, namespace, netAttachDefYaml.getName(), K8sApiGroupType.K8S_CNI_CNCF_IO.getValue(), K8sApiType.V1.getValue(), K8sApiCRDPluralType.NETWORK_ATTACHMENT_DEFINITION.getValue(), param);
        }

        return null;
    }

    /**
     * Replace CustomObject
     *
     * @param cluster
     * @param namespace
     * @param kind
     * @param config
     * @return
     * @throws Exception
     */
    public Map<String, Object> replaceCustomObject(ClusterVO cluster, String namespace, K8sApiKindType kind, Object config) throws Exception{

        if (kind == K8sApiKindType.NETWORK_ATTACHMENT_DEFINITION) {
            K8sCRDNetAttachDefGuiVO netAttachDef = (K8sCRDNetAttachDefGuiVO)config;

            Map<String, Object> v1NetAttachDef = k8sWorker.getCustomObjectV1(
                    cluster, namespace, netAttachDef.getName()
                    , K8sApiGroupType.K8S_CNI_CNCF_IO.getValue(), K8sApiType.V1.getValue(), K8sApiCRDPluralType.NETWORK_ATTACHMENT_DEFINITION.getValue());

            if (v1NetAttachDef == null || v1NetAttachDef.get(KubeConstants.META) == null) {
                throw new CocktailException("NetAttachDef not found!!", ExceptionType.K8sNetAttachDefNotFound);
            }

            Map<String, Object> valueMap = ObjectMapperUtils.getMapper().readValue(netAttachDef.getConfig(), new TypeReference<Map<String, Object>>(){});
            netAttachDef.setConfig(JsonUtils.toGson(valueMap));
            Map<String, Object> param = K8sCRDSpecFactory.buildNetworkAttachmentDefinition(netAttachDef);

            V1ObjectMeta metadata = (V1ObjectMeta)param.get(KubeConstants.META);
            if(metadata == null) {
                throw new CocktailException("Invalid NetAttachDef Data!!", ExceptionType.K8sNetAttachDefDataInvalid);
            }

            Object objResourceVersion = Optional.ofNullable(((Map<String, Object>)v1NetAttachDef.get(KubeConstants.META)).get("resourceVersion")).orElseGet(() ->null);
            if(objResourceVersion != null) {
                String resourceVersion = objResourceVersion.toString();
                if (StringUtils.isNotBlank(resourceVersion)) {
                    metadata.setResourceVersion(resourceVersion);
                }
            }

            return k8sWorker.replaceCustomObjectV1(cluster, namespace, netAttachDef.getName(), K8sApiGroupType.K8S_CNI_CNCF_IO.getValue(), K8sApiType.V1.getValue(), K8sApiCRDPluralType.NETWORK_ATTACHMENT_DEFINITION.getValue(), param);
        }

        return null;
    }

    public Map<String, Object> replaceCustomObject(Integer servicemapSeq, K8sApiKindType kind, Object config) throws Exception {
        ClusterVO cluster = this.setupCluster(servicemapSeq);
        return this.replaceCustomObject(cluster, cluster.getNamespaceName(), kind, config);
    }

    public Map<String, Object> replaceCustomObject(Integer clusterSeq, String namespace, String name, K8sApiKindType kind, K8sCRDYamlVO yaml) throws Exception {
        IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
        ClusterVO cluster = clusterDao.getCluster(clusterSeq);
        return this.replaceCustomObject(cluster, namespace, name, kind, yaml);
    }

    public Map<String, Object> replaceCustomObject(ClusterVO cluster, String namespace, String name, K8sApiKindType kind, K8sCRDYamlVO yaml) throws Exception {
        if (kind == K8sApiKindType.CLUSTER_ISSUER) {
            return this.replaceCustomObject(cluster, name, K8sApiGroupType.CERT_MANAGER_IO.getValue(), K8sApiType.V1.getValue(), K8sApiCRDPluralType.CLUSTER_ISSUERS.getValue(), yaml);
        } else if (kind == K8sApiKindType.ISSUER) {
            cluster.setNamespaceName(namespace);
            return this.replaceCustomObject(cluster, name, K8sApiGroupType.CERT_MANAGER_IO.getValue(), K8sApiType.V1.getValue(), K8sApiCRDPluralType.ISSUERS.getValue(), yaml);
        } else if (kind == K8sApiKindType.CERTIFICATE) {
            cluster.setNamespaceName(namespace);
            return this.replaceCustomObject(cluster, name, K8sApiGroupType.CERT_MANAGER_IO.getValue(), K8sApiType.V1.getValue(), K8sApiCRDPluralType.CERTIFICATES.getValue(), yaml);
        }

        return null;
    }

    /**
     * CustomObject 상세
     *
     * @param cluster
     * @param namespaceName
     * @param name
     * @param kind
     * @return
     * @throws Exception
     */
    public Map<String, Object> getCustomObject(ClusterVO cluster, String namespaceName, String name, K8sApiKindType kind) throws Exception {
        try {
            if (kind == K8sApiKindType.NETWORK_ATTACHMENT_DEFINITION) {
                return k8sWorker.getCustomObjectV1(
                        cluster, namespaceName, name
                        , K8sApiGroupType.K8S_CNI_CNCF_IO.getValue(), K8sApiType.V1.getValue(), K8sApiCRDPluralType.NETWORK_ATTACHMENT_DEFINITION.getValue());
            } else if (kind == K8sApiKindType.CLUSTER_ISSUER) {
                return k8sWorker.getCustomObjectV1(
                        cluster, namespaceName, name
                        , K8sApiGroupType.CERT_MANAGER_IO.getValue(), K8sApiType.V1.getValue(), K8sApiCRDPluralType.CLUSTER_ISSUERS.getValue());
            } else if (kind == K8sApiKindType.ISSUER) {
                return k8sWorker.getCustomObjectV1(
                        cluster, namespaceName, name
                        , K8sApiGroupType.CERT_MANAGER_IO.getValue(), K8sApiType.V1.getValue(), K8sApiCRDPluralType.ISSUERS.getValue());
            } else if (kind == K8sApiKindType.CERTIFICATE) {
                return k8sWorker.getCustomObjectV1(
                        cluster, namespaceName, name
                        , K8sApiGroupType.CERT_MANAGER_IO.getValue(), K8sApiType.V1.getValue(), K8sApiCRDPluralType.CERTIFICATES.getValue());
            } else if (kind == K8sApiKindType.CERTIFICATE_REQUEST) {
                return k8sWorker.getCustomObjectV1(
                        cluster, namespaceName, name
                        , K8sApiGroupType.CERT_MANAGER_IO.getValue(), K8sApiType.V1.getValue(), K8sApiCRDPluralType.CERTIFICATE_REQUESTS.getValue());
            } else if (kind == K8sApiKindType.ORDER) {
                return k8sWorker.getCustomObjectV1(
                        cluster, namespaceName, name
                        , K8sApiGroupType.ACME_CERT_MANAGER_IO.getValue(), K8sApiType.V1.getValue(), K8sApiCRDPluralType.ORDERS.getValue());
            } else if (kind == K8sApiKindType.CHALLENGE) {
                return k8sWorker.getCustomObjectV1(
                        cluster, namespaceName, name
                        , K8sApiGroupType.ACME_CERT_MANAGER_IO.getValue(), K8sApiType.V1.getValue(), K8sApiCRDPluralType.CHALLENGES.getValue());
            }
        }
        catch (CocktailException ce) {
            throw ce;
        }
        catch (Exception e) {
            throw new CocktailException("getCustomObject fail!!", e, ExceptionType.K8sCocktailCloudInquireFail);
        }
        return null;
    }

    public Map<String, Object> getCustomObject(Integer servicemapSeq, String name, K8sApiKindType kind) throws Exception {
        ClusterVO cluster = this.setupCluster(servicemapSeq);
        return this.getCustomObject(cluster, cluster.getNamespaceName(), name, kind);
    }

    /**
     * CustomObject 목록
     *
     * @param cluster
     * @param namespaceName
     * @param kind
     * @param label
     * @return
     * @throws Exception
     */
    public List<Map<String, Object>> getCustomObjects(ClusterVO cluster, String namespaceName, K8sApiKindType kind, String label) throws Exception {
        try {
            if (kind == K8sApiKindType.NETWORK_ATTACHMENT_DEFINITION) {
                return k8sWorker.getCustomObjectsV1(
                        cluster, namespaceName
                        , K8sApiGroupType.K8S_CNI_CNCF_IO.getValue(), K8sApiType.V1.getValue(), K8sApiCRDPluralType.NETWORK_ATTACHMENT_DEFINITION.getValue(), label);
            } else if (kind == K8sApiKindType.CLUSTER_ISSUER) {
                return k8sWorker.getCustomObjectsV1(
                        cluster, namespaceName
                        , K8sApiGroupType.CERT_MANAGER_IO.getValue(), K8sApiType.V1.getValue(), K8sApiCRDPluralType.CLUSTER_ISSUERS.getValue(), label);
            } else if (kind == K8sApiKindType.ISSUER) {
                return k8sWorker.getCustomObjectsV1(
                        cluster, namespaceName
                        , K8sApiGroupType.CERT_MANAGER_IO.getValue(), K8sApiType.V1.getValue(), K8sApiCRDPluralType.ISSUERS.getValue(), label);
            } else if (kind == K8sApiKindType.CERTIFICATE) {
                return k8sWorker.getCustomObjectsV1(
                        cluster, namespaceName
                        , K8sApiGroupType.CERT_MANAGER_IO.getValue(), K8sApiType.V1.getValue(), K8sApiCRDPluralType.CERTIFICATES.getValue(), label);
            } else if (kind == K8sApiKindType.CERTIFICATE_REQUEST) {
                return k8sWorker.getCustomObjectsV1(
                        cluster, namespaceName
                        , K8sApiGroupType.CERT_MANAGER_IO.getValue(), K8sApiType.V1.getValue(), K8sApiCRDPluralType.CERTIFICATE_REQUESTS.getValue(), label);
            } else if (kind == K8sApiKindType.ORDER) {
                return k8sWorker.getCustomObjectsV1(
                        cluster, namespaceName
                        , K8sApiGroupType.ACME_CERT_MANAGER_IO.getValue(), K8sApiType.V1.getValue(), K8sApiCRDPluralType.ORDERS.getValue(), label);
            } else if (kind == K8sApiKindType.CHALLENGE) {
                return k8sWorker.getCustomObjectsV1(
                        cluster, namespaceName
                        , K8sApiGroupType.ACME_CERT_MANAGER_IO.getValue(), K8sApiType.V1.getValue(), K8sApiCRDPluralType.CHALLENGES.getValue(), label);
            }
        }
        catch (CocktailException ce) {
            throw ce;
        }
        catch (Exception e) {
            throw new CocktailException("getCustomObjects fail!!", e, ExceptionType.K8sCocktailCloudInquireFail);
        }
        return null;
    }

    public List<Map<String, Object>> getCustomObjects(Integer servicemapSeq, K8sApiKindType kind, String label) throws Exception {
        ClusterVO cluster = this.setupCluster(servicemapSeq);
        return this.getCustomObjects(cluster, cluster.getNamespaceName(), kind, label);
    }

    public List<Map<String, Object>> getCustomObjects(Integer clusterSeq, String namespaceName, K8sApiKindType kind, String label) throws Exception {
        IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
        ClusterVO cluster = clusterDao.getCluster(clusterSeq);

        return this.getCustomObjects(cluster, namespaceName, kind, label);
    }

    public List<Map<String, Object>> getCustomObjects(String clusterId, String namespaceName, K8sApiKindType kind, String label) throws Exception {
        IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
        ClusterVO cluster = clusterDao.getClusterByClusterId(clusterId, "Y");

        return this.getCustomObjects(cluster, namespaceName, kind, label);
    }

    /**
     * CustomObject 목록 (범용...)
     *
     * @param cluster
     * @param namespaceName
     * @param group
     * @param version
     * @param plural
     * @param label
     * @return
     * @throws Exception
     */
    public List<Map<String, Object>> getCustomObjects(ClusterVO cluster, String namespaceName, String group, String version, String plural, String label) throws Exception {
        try {
            return k8sWorker.getCustomObjectsV1(cluster, namespaceName, group, version, plural, label);
        }
        catch (CocktailException ce) {
            throw ce;
        }
        catch (Exception e) {
            throw new CocktailException("getCustomObjects fail!!", e, ExceptionType.K8sCocktailCloudInquireFail);
        }
    }


    /**
     * delete CustomObject
     *
     * @param cluster
     * @param namespaceName
     * @param kind
     * @return
     * @throws Exception
     */
    public void deleteCustomObject(ClusterVO cluster, String namespaceName, String name, K8sApiKindType kind) throws Exception{

        if (kind == K8sApiKindType.NETWORK_ATTACHMENT_DEFINITION) {

            Map<String, Object> v1NetAttachDef = k8sWorker.getCustomObjectV1(
                    cluster, namespaceName, name
                    , K8sApiGroupType.K8S_CNI_CNCF_IO.getValue(), K8sApiType.V1.getValue(), K8sApiCRDPluralType.NETWORK_ATTACHMENT_DEFINITION.getValue());

            if (v1NetAttachDef != null) {
                k8sWorker.deleteCustomObjectV1(
                        cluster, namespaceName, name
                        , K8sApiGroupType.K8S_CNI_CNCF_IO.getValue(), K8sApiType.V1.getValue(), K8sApiCRDPluralType.NETWORK_ATTACHMENT_DEFINITION.getValue());
            }
        } else if (kind == K8sApiKindType.CLUSTER_ISSUER) {

            Map<String, Object> result = k8sWorker.getCustomObjectV1(
                    cluster, null, name
                    , K8sApiGroupType.CERT_MANAGER_IO.getValue(), K8sApiType.V1.getValue(), K8sApiCRDPluralType.CLUSTER_ISSUERS.getValue());

            if (result != null) {
                k8sWorker.deleteCustomObjectV1(
                        cluster, null, name
                        , K8sApiGroupType.CERT_MANAGER_IO.getValue(), K8sApiType.V1.getValue(), K8sApiCRDPluralType.CLUSTER_ISSUERS.getValue());
            }
        } else if (kind == K8sApiKindType.ISSUER) {

            Map<String, Object> result = k8sWorker.getCustomObjectV1(
                    cluster, namespaceName, name
                    , K8sApiGroupType.CERT_MANAGER_IO.getValue(), K8sApiType.V1.getValue(), K8sApiCRDPluralType.ISSUERS.getValue());

            if (result != null) {
                k8sWorker.deleteCustomObjectV1(
                        cluster, namespaceName, name
                        , K8sApiGroupType.CERT_MANAGER_IO.getValue(), K8sApiType.V1.getValue(), K8sApiCRDPluralType.ISSUERS.getValue());
            }
        } else if (kind == K8sApiKindType.CERTIFICATE) {

            Map<String, Object> result = k8sWorker.getCustomObjectV1(
                    cluster, namespaceName, name
                    , K8sApiGroupType.CERT_MANAGER_IO.getValue(), K8sApiType.V1.getValue(), K8sApiCRDPluralType.CERTIFICATES.getValue());

            if (result != null) {
                k8sWorker.deleteCustomObjectV1(
                        cluster, namespaceName, name
                        , K8sApiGroupType.CERT_MANAGER_IO.getValue(), K8sApiType.V1.getValue(), K8sApiCRDPluralType.CERTIFICATES.getValue());
            }
        }

    }

    public void deleteCustomObject(Integer servicemapSeq, String name, K8sApiKindType kind) throws Exception{
        ClusterVO cluster = this.setupCluster(servicemapSeq);
        this.deleteCustomObject(cluster, cluster.getNamespaceName(), name, kind);
    }

    public void convertNetAttachDef(Map<String, Object> result, K8sCRDNetAttachDefGuiVO netAttachDef) throws Exception {
        /**
         * {
         *     "apiVersion": "k8s.cni.cncf.io/v1",
         *     "kind": "NetworkAttachmentDefinition",
         *     "metadata": {
         *       "creationTimestamp": "2019-08-02T07:14:42Z",
         *       "generation": 1,
         *       "labels": {
         *         "cocktail": "ntywn2"
         *       },
         *       "name": "test",
         *       "namespace": "multus-test",
         *       "resourceVersion": "1894029",
         *       "selfLink": "/apis/k8s.cni.cncf.io/v1/namespaces/multus-test/network-attachment-definitions/test",
         *       "uid": "32fd40eb-b4f5-11e9-b589-9017acc13504"
         *     },
         *     "spec": {
         *       "config": "{\"ipam\":{\"subnet\":\"10.56.69.0/24\",\"routes\":[{\"dst\":\"0.0.0.0/0\"}],\"rangeStart\":\"101.55.69.56\",\"type\":\"host-local\",\"gateway\":\"101.56.69.1\",\"rangeEnd\":\"101.55.69.56\"},\"name\":\"sriov-net\",\"type\":\"sriov\"}"
         *     }
         *   }
         */
        Map<String, Object> metadata = (Map<String, Object>)result.get(KubeConstants.META);
        Map<String, String> labels = metadata.get(KubeConstants.META_LABELS) != null ? (Map<String, String>)metadata.get(KubeConstants.META_LABELS) : Maps.newHashMap();
        Map<String, String> annotations = metadata.get(KubeConstants.META_ANNOTATIONS) != null ? (Map<String, String>)metadata.get(KubeConstants.META_ANNOTATIONS) : Maps.newHashMap();
        Map<String, String> spec = result.get(KubeConstants.SPEC) != null ? (Map<String, String>)result.get(KubeConstants.SPEC) : Maps.newHashMap();
        netAttachDef.setName((String)metadata.get(KubeConstants.NAME));
        netAttachDef.setNamespace((String)metadata.get(KubeConstants.META_NAMESPACE));
        if(metadata.get("creationTimestamp") != null) {
            netAttachDef.setCreationTimestamp(new DateTime(metadata.get("creationTimestamp")));
        }
        netAttachDef.setType(labels.get(KubeConstants.LABELS_CRD_NET_ATTACH_DEF));
        netAttachDef.setLabels(labels);
        netAttachDef.setAnnotations(annotations);
        /**
         * 2021년 10월 29일 (금)
         * OpenShift에서 배포된 Istio에서는 SPEC 없는 NetworkAttachmentDefinition가 배포되어,
         * spec.config 참조할 때 NullPointerException 발생 -> NULL 체크 추가
         *
         * $ kubectl get net-attach-def -n default v2-0-istio-cni -o yaml
         * apiVersion: k8s.cni.cncf.io/v1
         * kind: NetworkAttachmentDefinition
         * metadata:
         *   creationTimestamp: "2022-03-08T06:42:29Z"
         *   generation: 1
         *   labels:
         *     maistra.io/member-of: istio-system
         *   name: v2-1-istio-cni
         *   namespace: test-ns
         *   resourceVersion: "53622934"
         *   uid: c54ee8b0-47fe-4654-8a97-b79d18542250
         * spec:
         *   config: ""
         *
         */
        if(Objects.nonNull(spec) && spec.containsKey("config")) {
            if (StringUtils.isNotBlank(spec.get("config"))) {
                Map<String, Object> valueMap = ObjectMapperUtils.getMapper().readValue(spec.get("config"), new TypeReference<Map<String, Object>>() {});
                netAttachDef.setConfig(JsonUtils.toPrettyString(valueMap));
            } else {
                netAttachDef.setConfig("");
            }
        }

        JSON k8sJson = new JSON();
        netAttachDef.setDeployment(k8sJson.serialize(result));
        netAttachDef.setDeploymentYaml(Yaml.getSnakeYaml().dumpAsMap(result));
    }

    public List<K8sCRDNetAttachDefGuiVO> getConvertNetAttachDefList(List<Map<String, Object>> results) throws Exception {
        List<K8sCRDNetAttachDefGuiVO> netAttachDefs = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(results)) {
            for (Map<String, Object> resultRow : results) {
                K8sCRDNetAttachDefGuiVO netAttachDef = new K8sCRDNetAttachDefGuiVO();
                this.convertNetAttachDef(resultRow, netAttachDef);
                netAttachDefs.add(netAttachDef);
            }
        }

        return netAttachDefs;
    }

    public void validNetAttachDefConfig(K8sCRDNetAttachDefGuiVO netAttachDef) throws Exception{
        if (StringUtils.isBlank(netAttachDef.getConfig())){
            throw new CocktailException("NetworkAttachmentDefinition config is invalid", ExceptionType.K8sNetAttachDefDataInvalid);
        }else{
            JSONObject config;
            try {
                config = new JSONObject(netAttachDef.getConfig());
            }
            catch (JSONException je) {
                throw new CocktailException("NetworkAttachmentDefinition config format is invalid", je, ExceptionType.K8sNetAttachDefDataInvalid);
            }
            catch (Exception e) {
                throw new CocktailException("NetworkAttachmentDefinition config format is invalid", e, ExceptionType.K8sNetAttachDefDataInvalid);
            }

            if (StringUtils.isNotBlank(config.getString("type"))) {
                if (!K8sApiCniType.containType(config.getString("type"))) {
                    throw new CocktailException("NetworkAttachmentDefinition type is not exists!!", ExceptionType.K8sNetAttachDefDataInvalid);
                }
            } else {
                throw new CocktailException("NetworkAttachmentDefinition config is invalid", ExceptionType.K8sNetAttachDefDataInvalid);
            }
        }
    }

    public List<V1beta1CustomResourceDefinition> getCustomResourceDefinitionV1beta1(ClusterVO cluster, String fieldSelector, String labelSelector) throws Exception{
        if(cluster == null) {
            throw new CocktailException("cluster is null.", ExceptionType.InvalidParameter);
        }
        try {
            return k8sWorker.getCustomResourceDefinitionV1beta1(cluster, fieldSelector, labelSelector);
        }
        catch (CocktailException ce) {
            throw ce;
        }
        catch (Exception e) {
            throw new CocktailException("getCustomResourceDefinitionV1beta1 fail!!", e, ExceptionType.K8sCocktailCloudInquireFail);
        }
    }

    public List<V1CustomResourceDefinition> getCustomResourceDefinitionV1(ClusterVO cluster, String fieldSelector, String labelSelector) throws Exception{
        if(cluster == null) {
            throw new CocktailException("cluster is null.", ExceptionType.InvalidParameter);
        }
        try {
            return k8sWorker.getCustomResourceDefinitionV1(cluster, fieldSelector, labelSelector);
        }
        catch (CocktailException ce) {
            throw ce;
        }
        catch (Exception e) {
            throw new CocktailException("getCustomResourceDefinitionV1 fail!!", e, ExceptionType.K8sCocktailCloudInquireFail);
        }
    }

    /**
     * Created by 2021-06-11
     */

    public List<K8sCRDResultVO> getCustomResourceDefinitions(ClusterVO cluster, String fieldSelector, String labelSelector) throws Exception {
        if(cluster == null) {
            throw new CocktailException("Cluster not found.", ExceptionType.ClusterNotFound, ExceptionBiz.CUSTOM_RESOURCE_DEFINITION);
        }

        List<K8sCRDResultVO> results = new ArrayList<>();

        K8sApiType apiType = this.getApiType(cluster);
        if(apiType == K8sApiType.V1BETA1) {
            List<V1beta1CustomResourceDefinition> crdDefs = null;
            crdDefs = this.listCustomResourceDefinitionV1beta1(cluster, fieldSelector, labelSelector);
            for (V1beta1CustomResourceDefinition crdDef : crdDefs) {
                if(!this.validateCustomResourceDefinition(crdDef)) {
                    break;
                }
				this.removeCustomResourceDefinitionManagedFields(crdDef);
                results.add(this.createDefinitionVO(crdDef));
            }
            return results;
        }
        else if(apiType == K8sApiType.V1) {
            List<V1CustomResourceDefinition> crdDefs = null;
            crdDefs = this.listCustomResourceDefinitionV1(cluster, fieldSelector, labelSelector);
            for (V1CustomResourceDefinition crdDef : crdDefs) {
                if(!this.validateCustomResourceDefinition(crdDef)) {
                    break;
                }
				this.removeCustomResourceDefinitionManagedFields(crdDef);
                results.add(this.createDefinitionVO(crdDef));
            }
            return results;
        }
        else {
            throw new CocktailException("Not support API Version.", ExceptionType.CommonNotSupported, ExceptionBiz.CUSTOM_RESOURCE_DEFINITION);
        }
    }

    public List<V1beta1CustomResourceDefinition> listCustomResourceDefinitionV1beta1(ClusterVO cluster, String fieldSelector, String labelSelector) throws Exception {
        List<V1beta1CustomResourceDefinition> crdDefs = null;
        crdDefs = k8sWorker.getCustomResourceDefinitionV1beta1(cluster, fieldSelector, labelSelector);
        if(crdDefs == null || crdDefs.size() <= 0) {
            throw new CocktailException("No found Custom Resource Definition.", ExceptionType.CommonNotFound, ExceptionBiz.CUSTOM_RESOURCE_DEFINITION);
        }
        return crdDefs;

    }

    public List<V1CustomResourceDefinition> listCustomResourceDefinitionV1(ClusterVO cluster, String fieldSelector, String labelSelector) throws Exception {
        List<V1CustomResourceDefinition> crdDefs = null;
        crdDefs = k8sWorker.getCustomResourceDefinitionV1(cluster, fieldSelector, labelSelector);
        if(crdDefs == null || crdDefs.size() <= 0) {
            throw new CocktailException("No found Custom Resource Definition.", ExceptionType.CommonNotFound, ExceptionBiz.CUSTOM_RESOURCE_DEFINITION);
        }
        return crdDefs;
    }

    private V1beta1CustomResourceDefinition removeCustomResourceDefinitionManagedFields(V1beta1CustomResourceDefinition crdDef) {
        if(crdDef == null || crdDef.getMetadata() == null) {
            return crdDef;
        }
        V1ObjectMeta metadata = crdDef.getMetadata();
        if(metadata == null || metadata.getManagedFields() == null) {
            return crdDef;
        }
        List<V1ManagedFieldsEntry> managedFields = metadata.getManagedFields();
        if(managedFields == null || managedFields.size() <= 0) {
            return crdDef;
        }
        else {
            managedFields.clear();
            metadata.setManagedFields(null);
        }
        return crdDef;
    }

    private V1CustomResourceDefinition removeCustomResourceDefinitionManagedFields(V1CustomResourceDefinition crdDef) {
        if(crdDef == null || crdDef.getMetadata() == null) {
            return crdDef;
        }
        V1ObjectMeta metadata = crdDef.getMetadata();
        if(metadata == null || metadata.getManagedFields() == null) {
            return crdDef;
        }
        List<V1ManagedFieldsEntry> managedFields = metadata.getManagedFields();
        if(managedFields == null || managedFields.size() <= 0) {
            return crdDef;
        }
        else {
            managedFields.clear();
            metadata.setManagedFields(null);
        }
        return crdDef;
    }

    private K8sCRDResultVO createDefinitionVO(V1beta1CustomResourceDefinition v1beta1) {
        if(v1beta1 == null) {
            throw new CocktailException("Invalid parameter.", ExceptionType.ClusterNotFound, ExceptionBiz.CUSTOM_RESOURCE_DEFINITION);
        }

        K8sCRDResultVO result = new K8sCRDResultVO();
        String name = v1beta1.getMetadata().getName();
        name = name.substring(0, name.indexOf("."));
        result.setName(name);
        result.setGroup(v1beta1.getSpec().getGroup());
        String plural = v1beta1.getSpec().getNames().getPlural();
        String group = v1beta1.getSpec().getGroup();
        String fullName = this.combinePluralGroup(plural, group);
        result.setFullName(fullName);
        result.setScope(v1beta1.getSpec().getScope());
        for(V1beta1CustomResourceDefinitionVersion s : v1beta1.getSpec().getVersions()) {
//            Map<String, String> version = new HashMap<>();
//            version.put("name", s.getName());
//            version.put("storage", s.getStorage().toString());
//            version.put("served", s.getServed().toString());
            result.getVersions().add(JsonUtils.fromGson(JsonUtils.toGson(s), K8sCRDVersionVO.class));
            if(s.getStorage()) {
                result.setStoredVersion(s.getName());
            }
        }
        V1beta1CustomResourceDefinitionNames acceptedNameObj = v1beta1.getSpec().getNames();
        K8sCRDNamesVO crdNames = JsonUtils.fromGson(JsonUtils.toGson(acceptedNameObj), K8sCRDNamesVO.class);
//        Map<String, Object> acceptedName = new HashMap<>();
//        acceptedName.put("plural", acceptedNameObj.getPlural());
//        acceptedName.put("singular", acceptedNameObj.getSingular());
//        acceptedName.put("listkind", acceptedNameObj.getListKind());
//        acceptedName.put("shortnames", acceptedNameObj.getShortNames());
//        acceptedName.put("kind", acceptedNameObj.getKind());
        result.setAcceptedNames(crdNames);
        result.setLabels(v1beta1.getMetadata().getLabels());
        result.setAnnotations(v1beta1.getMetadata().getAnnotations());
        result.setCreationTimestamp(v1beta1.getMetadata().getCreationTimestamp());
        return result;
    }

    private K8sCRDResultVO createDefinitionVO(V1CustomResourceDefinition v1) {
        if(v1 == null) {
            throw new CocktailException("Invalid parameter.", ExceptionType.InvalidParameter, ExceptionBiz.CUSTOM_RESOURCE_DEFINITION);
        }

        K8sCRDResultVO result = new K8sCRDResultVO();
        String name = v1.getMetadata().getName();
        name = name.substring(0, name.indexOf("."));
        result.setName(name);
        result.setGroup(v1.getSpec().getGroup());
        String plural = v1.getSpec().getNames().getPlural();
        String group = v1.getSpec().getGroup();
        String fullName = this.combinePluralGroup(plural, group);
        result.setFullName(fullName);
        result.setScope(v1.getSpec().getScope());
        for(V1CustomResourceDefinitionVersion s : v1.getSpec().getVersions()) {
//            Map<String, String> version = new HashMap<>();
//            version.put("name", s.getName());
//            version.put("storage", s.getStorage().toString());
//            version.put("served", s.getServed().toString());
            result.getVersions().add(JsonUtils.fromGson(JsonUtils.toGson(s), K8sCRDVersionVO.class));
            if(s.getStorage()) {
                result.setStoredVersion(s.getName());
            }
        }
        V1CustomResourceDefinitionNames acceptedNameObj = v1.getSpec().getNames();
//        Map<String, Object> acceptedName = new HashMap<>();
//        acceptedName.put("plural", acceptedNameObj.getPlural());
//        acceptedName.put("singular", acceptedNameObj.getSingular());
//        acceptedName.put("listkind", acceptedNameObj.getListKind());
//        acceptedName.put("shortnames", acceptedNameObj.getShortNames());
//        acceptedName.put("kind", acceptedNameObj.getKind());
        result.setAcceptedNames(JsonUtils.fromGson(JsonUtils.toGson(acceptedNameObj), K8sCRDNamesVO.class));
        result.setLabels(v1.getMetadata().getLabels());
        result.setAnnotations(v1.getMetadata().getAnnotations());
        result.setCreationTimestamp(v1.getMetadata().getCreationTimestamp());
        return result;
    }

    public K8sCRDResultVO readCustomResourceDefinition(ClusterVO cluster, String name) throws Exception {
        if(cluster == null) {
            throw new CocktailException("Cluster not found.", ExceptionType.ClusterNotFound, ExceptionBiz.CUSTOM_RESOURCE_DEFINITION);
        }
        if(name == null || "".equals(name)) {
            throw new CocktailException("Invalid parameter.", ExceptionType.InvalidParameter, ExceptionBiz.CUSTOM_RESOURCE_DEFINITION);
        }

        K8sApiType apiType = this.getApiType(cluster);
        if(apiType == K8sApiType.V1BETA1) {
            V1beta1CustomResourceDefinition crdDef = null;
            crdDef = this.readCustomResourceDefinitionV1beta1(cluster, name);
            return this.createDefinitionVO(crdDef);
        } else
        if(apiType == K8sApiType.V1) {
            V1CustomResourceDefinition crdDef = null;
            crdDef = this.readCustomResourceDefinitionV1(cluster, name);
            return this.createDefinitionVO(crdDef);
        }
        else {
            throw new CocktailException("Not support API Version.", ExceptionType.CommonNotSupported, ExceptionBiz.CUSTOM_RESOURCE_DEFINITION);
        }
    }

    public V1beta1CustomResourceDefinition readCustomResourceDefinitionV1beta1(ClusterVO cluster, String name) throws Exception {
        V1beta1CustomResourceDefinition crdDef = null;
        crdDef = k8sWorker.readCustomResourceDefinitionV1beta1Status(cluster, name);
        if(crdDef == null) {
            throw new CocktailException("Not found Custom Resource Definition.", ExceptionType.CommonNotFound, ExceptionBiz.CUSTOM_RESOURCE_DEFINITION);
        }
//        if(!this.validateCustomResourceDefinition(crdDef)) {
//            throw new CocktailException("Failed validation request data (V1beta1).", ExceptionType.CommonFail, ExceptionBiz.CUSTOM_RESOURCE_DEFINITION);
//        }

        return this.removeCustomResourceDefinitionManagedFields(crdDef);
    }

    public V1CustomResourceDefinition readCustomResourceDefinitionV1(ClusterVO cluster, String name) throws Exception {
        V1CustomResourceDefinition crdDef = null;
        crdDef = k8sWorker.readCustomResourceDefinitionV1Status(cluster, name);
        if(crdDef == null) {
            throw new CocktailException("Not found Custom Resource Definition.", ExceptionType.CommonNotFound, ExceptionBiz.CUSTOM_RESOURCE_DEFINITION);
        }
//        if(!this.validateCustomResourceDefinition(crdDef)) {
//            throw new CocktailException("Failed validation request data (V1).", ExceptionType.CommonFail, ExceptionBiz.CUSTOM_RESOURCE_DEFINITION);
//        }
        return this.removeCustomResourceDefinitionManagedFields(crdDef);
    }

    public List<?> readCustomResourceDefinitionRaw(ClusterVO cluster, String name) throws Exception {
        if(cluster == null) {
            throw new CocktailException("Cluster not found.", ExceptionType.ClusterNotFound, ExceptionBiz.CUSTOM_RESOURCE_DEFINITION);
        }
        if(name == null || "".equals(name)) {
            throw new CocktailException("Invalid parameter.", ExceptionType.InvalidParameter, ExceptionBiz.CUSTOM_RESOURCE_DEFINITION);
        }

        K8sApiType apiType = this.getApiType(cluster);
        if(apiType == K8sApiType.V1BETA1) {
            List<V1beta1CustomResourceDefinition> result = new ArrayList<>();
            V1beta1CustomResourceDefinition crdDef = null;
            crdDef = this.readCustomResourceDefinitionV1beta1(cluster, name);
            if(crdDef != null) {
                this.removeCustomResourceDefinitionManagedFields(crdDef);
                result.add(crdDef);
            }
            return result;
        } else
        if(apiType == K8sApiType.V1) {
            List<V1CustomResourceDefinition> result = new ArrayList<>();
            V1CustomResourceDefinition crdDef = null;
            crdDef = this.readCustomResourceDefinitionV1(cluster, name);
            if(crdDef != null) {
                V1ObjectMeta metadata = crdDef.getMetadata();
                this.removeCustomResourceDefinitionManagedFields(crdDef);
                result.add(crdDef);
            }
            return result;
        }
        else {
            throw new CocktailException("Not support API Version.", ExceptionType.CommonNotSupported, ExceptionBiz.CUSTOM_RESOURCE_DEFINITION);
        }
    }

    public K8sCRDResultVO createCustomResourceDefinition(ClusterVO cluster, K8sCRDYamlVO yaml) throws Exception {
        if(cluster == null) {
            throw new CocktailException("Cluster not found.", ExceptionType.ClusterNotFound, ExceptionBiz.CUSTOM_RESOURCE_DEFINITION);
        }

        if(yaml == null) {
            throw new CocktailException("Invalid parameter.", ExceptionType.InvalidParameter, ExceptionBiz.CUSTOM_RESOURCE_DEFINITION);
        }

        K8sApiType apiType = this.getApiType(cluster);
        if(apiType == K8sApiType.V1BETA1) {
            V1beta1CustomResourceDefinition body = this.loadV1beta1(yaml.getYaml());
            V1beta1CustomResourceDefinition crdDef = null;
            crdDef = this.createCustomResourceDefinitionV1beta1(cluster, body, null);
            return this.createDefinitionVO(crdDef);
        } else
        if(apiType == K8sApiType.V1) {
            V1CustomResourceDefinition body = this.loadV1(yaml.getYaml());
            V1CustomResourceDefinition crdDef = null;
            crdDef = this.createCustomResourceDefinitionV1(cluster, body, null);
            return this.createDefinitionVO(crdDef);
        }
        else {
            throw new CocktailException("Not support API Version.", ExceptionType.CommonNotSupported, ExceptionBiz.CUSTOM_RESOURCE_DEFINITION);
        }
    }

    public V1beta1CustomResourceDefinition createCustomResourceDefinitionV1beta1(ClusterVO cluster, V1beta1CustomResourceDefinition body, String fieldManager) throws Exception {
        V1ObjectMeta metadata = body.getMetadata();
        String name = metadata.getName();
        V1beta1CustomResourceDefinition v1beta1Prev = k8sWorker.readCustomResourceDefinitionV1beta1(cluster, name);
        // v1beta1Prev 값이 null이 아닐경우, 기존에 같은 이름으로 생성된 Custom Resource Definition이 존재한다는 의미
        if(v1beta1Prev != null &&
            (v1beta1Prev.getApiVersion() != null &&
                v1beta1Prev.getMetadata() != null &&
                v1beta1Prev.getSpec() != null)) {
            throw new CocktailException("Already exists Custom Resoure Definition.", ExceptionType.CustomObjectNameAlreadyExists, ExceptionBiz.CUSTOM_RESOURCE_DEFINITION);
        }

        V1beta1CustomResourceDefinition crdDef = null;
        crdDef = k8sWorker.createCustomResourceDefinitionV1beta1(cluster, body, fieldManager);
        return this.removeCustomResourceDefinitionManagedFields(crdDef);
    }

    public V1CustomResourceDefinition createCustomResourceDefinitionV1(ClusterVO cluster, V1CustomResourceDefinition body, String fieldManager) throws Exception {
        V1ObjectMeta metadata = body.getMetadata();
        String name = metadata.getName();
        V1CustomResourceDefinition v1Prev = k8sWorker.readCustomResourceDefinitionV1(cluster, name);
        // v1Prev 값이 null이 아닐경우, 기존에 같은 이름으로 생성된 Custom Resource Definition이 존재한다는 의미
        if(v1Prev != null &&
            (v1Prev.getApiVersion() != null &&
                v1Prev.getMetadata() != null &&
                v1Prev.getSpec() != null)) {
            throw new CocktailException("Already exists Custom Resoure Definition.", ExceptionType.CustomObjectNameAlreadyExists, ExceptionBiz.CUSTOM_RESOURCE_DEFINITION);
        }

        V1CustomResourceDefinition crdDef = null;
        crdDef = k8sWorker.createCustomResourceDefinitionV1(cluster, body, fieldManager);
        return this.removeCustomResourceDefinitionManagedFields(crdDef);
    }

    public Object deleteCustomResourceDefinition(ClusterVO cluster, String name) throws Exception {
        if(cluster == null) {
            throw new CocktailException("Cluster not found.", ExceptionType.ClusterNotFound, ExceptionBiz.CUSTOM_RESOURCE_DEFINITION);
        }
        if(name == null || "".equals(name)) {
            throw new CocktailException("Invalid parameter.", ExceptionType.InvalidParameter, ExceptionBiz.CUSTOM_RESOURCE_DEFINITION);
        }

        K8sApiType apiType = this.getApiType(cluster);
        if(apiType == K8sApiType.V1BETA1) {
            return this.deleteCustomResourceDefinitionV1beta1(cluster, name);
        } else
        if(apiType == K8sApiType.V1) {
            return this.deleteCustomResourceDefinitionV1(cluster, name);
        }
        else {
            throw new CocktailException("Not support API Version.", ExceptionType.CommonNotSupported, ExceptionBiz.CUSTOM_RESOURCE_DEFINITION);
        }
    }

    private Object deleteCustomResourceDefinitionV1beta1(ClusterVO cluster, String name) throws Exception {
        V1beta1CustomResourceDefinition crdDef = null;
        crdDef = k8sWorker.readCustomResourceDefinitionV1beta1(cluster, name);
        if(crdDef != null) {
//            throw new CocktailException("Not found Custom Resoure Definition.", ExceptionType.CommonNotFound, ExceptionBiz.CUSTOM_RESOURCE_DEFINITION);
			return k8sWorker.deleteCustomResourceDefinitionV1beta1(cluster, name);
        }

		return null;
    }

    private Object deleteCustomResourceDefinitionV1(ClusterVO cluster, String name) throws Exception {
        V1CustomResourceDefinition crdDef = null;
        crdDef = k8sWorker.readCustomResourceDefinitionV1(cluster, name);
        if(crdDef != null) {
//            throw new CocktailException("Not found Custom Resoure Definition.", ExceptionType.CommonNotFound, ExceptionBiz.CUSTOM_RESOURCE_DEFINITION);
			return k8sWorker.deleteCustomResourceDefinitionV1(cluster, name);
        }

		return null;
    }

    public List<Map<String, Object>> genTemplate(ClusterVO cluster, String namespace, String name, String version) throws Exception {
        if(cluster == null) {
            throw new CocktailException("Cluster not found.", ExceptionType.ClusterNotFound, ExceptionBiz.CUSTOM_RESOURCE_DEFINITION);
        }

        if(name == null || "".equals(name)) {
            throw new CocktailException("Invalid parameter.", ExceptionType.InvalidParameter, ExceptionBiz.CUSTOM_RESOURCE_DEFINITION);
        }

        if(version == null || "".equals(version)) {
            throw new CocktailException("Invalid parameter.", ExceptionType.InvalidParameter, ExceptionBiz.CUSTOM_RESOURCE_DEFINITION);
        }

        K8sApiType apiType = this.getApiType(cluster);
        if(apiType == K8sApiType.V1BETA1) {
            return this.genTemplateV1beta1(cluster, namespace, name, version);
        }
        if(apiType == K8sApiType.V1) {
            return this.genTemplateV1(cluster, namespace, name, version);
        }
        else {
            throw new CocktailException("Not support API Version.", ExceptionType.CommonNotSupported, ExceptionBiz.CUSTOM_RESOURCE_DEFINITION);
        }
    }

    public List<Map<String, Object>> genTemplateV1beta1(ClusterVO cluster, String namespace, String name, String version) throws Exception {
        V1beta1CustomResourceDefinition crdDef = k8sWorker.readCustomResourceDefinitionV1beta1(cluster, name);
        if(crdDef == null ||
            crdDef.getApiVersion() == null ||
            crdDef.getMetadata() == null ||
            crdDef.getSpec() == null) {
            throw new CocktailException("Not found Custom Resoure Definition.", ExceptionType.CommonNotFound, ExceptionBiz.CUSTOM_RESOURCE_DEFINITION);
        }

        V1ObjectMeta defMeta = crdDef.getMetadata();
        V1beta1CustomResourceDefinitionSpec defSpec = crdDef.getSpec();

        if(defSpec == null) {
            throw new CocktailException("Not found Spec. (required)", ExceptionType.CommonNotFound, ExceptionBiz.CUSTOM_RESOURCE_DEFINITION);
        }

        if(defSpec.getVersions() == null || defSpec.getVersions().size() <= 0) {
            throw new CocktailException("Not found Versions. (required)", ExceptionType.CommonNotFound, ExceptionBiz.CUSTOM_RESOURCE_DEFINITION);
        }

        // 버전 리스트가 Kubernetes 버전 정렬 방식에 따라 정렬이 되어 있다고 가정을 하고 작성.
        // 수정이 필요할 수 있음.
        V1beta1CustomResourceDefinitionVersion lastVersion = null;
        for(V1beta1CustomResourceDefinitionVersion tmp : defSpec.getVersions()) {
            if(tmp.getName().equalsIgnoreCase(version)) {
                lastVersion = null;
                break;
            }
            else if(tmp.getStorage() || tmp.getServed()) {
                lastVersion = tmp;
            }
        }

        if(lastVersion != null) {
            version = lastVersion.getName();
        }

        List<Map<String, Object>> tmpl = new ArrayList<>();

        if(defSpec.getGroup() == null) {
            return tmpl;
        }

        Map<String, Object> apiVersion = new HashMap<>();
        apiVersion.put("apiVersion", defSpec.getGroup() + "/" + version);
        tmpl.add(apiVersion);

        if(defSpec.getNames() == null) {
            return tmpl;
        }

        Map<String, Object> kind = new HashMap<>();
        kind.put("kind", defSpec.getNames().getKind());
        tmpl.add(kind);

        Map<String, Object> metadata = new HashMap<>();

        Map<String, Object> metaCont = new HashMap<>();
        if(!StringUtils.isBlank(namespace)) {
            metaCont.put("namespace", namespace);
        }
        metaCont.put("name", "");
        metadata.put("metadata", metaCont);

        tmpl.add(metadata);

        Map<String, Object> spec = new HashMap<>();

        spec.put("spec", "");

        tmpl.add(spec);

        return tmpl;
    }

    public List<Map<String, Object>> genTemplateV1(ClusterVO cluster, String namespace, String name, String version) throws Exception {
        V1CustomResourceDefinition crdDef = k8sWorker.readCustomResourceDefinitionV1(cluster, name);
        if(crdDef == null ||
            crdDef.getApiVersion() == null ||
            crdDef.getMetadata() == null ||
            crdDef.getSpec() == null) {
            throw new CocktailException("Not found Custom Resoure Definition.", ExceptionType.CommonNotFound, ExceptionBiz.CUSTOM_RESOURCE_DEFINITION);
        }

        V1ObjectMeta defMeta = crdDef.getMetadata();
        V1CustomResourceDefinitionSpec defSpec = crdDef.getSpec();

        if(defSpec == null) {
            throw new CocktailException("Not found Spec. (required)", ExceptionType.CommonNotFound, ExceptionBiz.CUSTOM_RESOURCE_DEFINITION);
        }

        if(defSpec.getVersions() == null || defSpec.getVersions().size() <= 0) {
            throw new CocktailException("Not found Versions. (required)", ExceptionType.CommonNotFound, ExceptionBiz.CUSTOM_RESOURCE_DEFINITION);
        }

        // 버전 리스트가 Kubernetes 버전 정렬 방식에 따라 정렬이 되어 있다고 가정을 하고 작성.
        // 수정이 필요할 수 있음.
        V1CustomResourceDefinitionVersion lastVersion = null;
        for(V1CustomResourceDefinitionVersion tmp : defSpec.getVersions()) {
            if(tmp.getName().equalsIgnoreCase(version)) {
                lastVersion = null;
                break;
            }
            else if(tmp.getStorage() || tmp.getServed()) {
                lastVersion = tmp;
            }
        }

        if(lastVersion != null) {
            version = lastVersion.getName();
        }

        List<Map<String, Object>> tmpl = new ArrayList<>();

        if(defSpec.getGroup() == null) {
            return tmpl;
        }

        Map<String, Object> apiVersion = new HashMap<>();
        apiVersion.put("apiVersion", defSpec.getGroup() + "/" + version);
        tmpl.add(apiVersion);

        if(defSpec.getNames() == null) {
            return tmpl;
        }

        Map<String, Object> kind = new HashMap<>();
        kind.put("kind", defSpec.getNames().getKind());
        tmpl.add(kind);

        Map<String, Object> metadata = new HashMap<>();

        Map<String, Object> metaCont = new HashMap<>();
        if(!StringUtils.isBlank(namespace)) {
            metaCont.put("namespace", namespace);
        }
        metaCont.put("name", "");
        metadata.put("metadata", metaCont);

        tmpl.add(metadata);

        Map<String, Object> spec = new HashMap<>();

        spec.put("spec", "");

        tmpl.add(spec);

        return tmpl;
    }

    public Map<String, Object> genTemplateCommon(String name, String version, String kind) throws Exception {
        Map<String, Object> templateObj = new HashMap<>();
        templateObj.put("apiVersion", String.format("%s/%s", name, version));
        templateObj.put("kind", kind);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("name", "");
        templateObj.put("metadata", metadata);

        templateObj.put("spec", "");

        return templateObj;
    }

    public Map<String, Object> createCustomObject(ClusterVO cluster, String group, String version, String plural, K8sCRDYamlVO yaml) throws Exception {
        if(cluster == null) {
            throw new CocktailException("Cluster not found.", ExceptionType.ClusterNotFound, ExceptionBiz.CUSTOM_RESOURCE_DEFINITION);
        }

        if(StringUtils.isNotBlank(cluster.getNamespaceName())) {
            this.requireNamespace(cluster, cluster.getNamespaceName());
        }

        Map<String, Object> body = this.loadMap(yaml.getYaml());
        if(body == null || body.size() <= 0) {
            throw new CocktailException("Invalid parameter.", ExceptionType.InvalidYamlData, ExceptionBiz.CUSTOM_RESOURCE_DEFINITION);
        }

        Map<String, Object> metadata = (Map<String, Object>) body.get("metadata");
        if(metadata == null || metadata.size() <= 0) {
            throw new CocktailException("Not found Metadata. (required)", ExceptionType.K8sCustomObjectNameInvalid, ExceptionBiz.CUSTOM_RESOURCE_DEFINITION);
       }

       if(metadata.get("namespace") != null) {
            String namespace = metadata.get("namespace").toString();
            if(StringUtils.isNotBlank(namespace)) {
                this.requireNamespace(cluster, namespace);
            }
       }

       if(metadata.get("name") == null)  {
           throw new CocktailException("Invalid Custom Object name.", ExceptionType.K8sCustomObjectNameInvalid, ExceptionBiz.CUSTOM_RESOURCE_DEFINITION);
       }
       String name = metadata.get("name").toString();
       if("".equals(name))  {
           throw new CocktailException("Invalid Custom Object name.", ExceptionType.K8sCustomObjectNameInvalid, ExceptionBiz.CUSTOM_RESOURCE_DEFINITION);
       }

        Map<String, Object> customObject = k8sWorker.getCustomObjectV1Call(cluster, cluster.getNamespaceName(), name, group, version, plural);
        // customObject가 null이 아닐경우, 기존에 같은 이름으로 생성된 Custom Object가 존재한다는 의미
        if(customObject != null) {
            throw new CocktailException("Already exists Custom Object.", ExceptionType.CustomObjectNameAlreadyExists, ExceptionBiz.CUSTOM_RESOURCE_DEFINITION);
        }

        // 요청 본문에 ManagedFields 값이 있을 경우 삭제.
        this.removeManagedFields(body);

        Map<String, Object> result = null;
        result = k8sWorker.createCustomObjectV1Call(cluster, cluster.getNamespaceName(), group, version, plural, body);
        return this.removeManagedFields(result);
    }

    private void requireNamespace(ClusterVO cluster, String namespace) throws Exception {
        if(k8sWorker.getNamespaceV1(cluster, namespace) == null) {
            throw new CocktailException("Not found namespace", ExceptionType.K8sNamespaceNotFound, ExceptionBiz.CUSTOM_RESOURCE_DEFINITION);
        }
    }

    public K8sCustomObjectVO createCustomObjectVO(Map<String, Object> customObject) {
        if(customObject == null) {
            throw new CocktailException("Not found Custom Object.", ExceptionType.CommonNotFound, ExceptionBiz.CUSTOM_RESOURCE_DEFINITION);
        }

        Map<String, Object> metadata = (Map<String, Object>)customObject.get("metadata");
        Map<String, Object> spec = (Map<String, Object>)customObject.get("spec");

        if(metadata == null || metadata.isEmpty()) {
            throw new CocktailException("Not found Metadata. (required)", ExceptionType.CommonNotFound, ExceptionBiz.CUSTOM_RESOURCE_DEFINITION);
        }

        K8sCustomObjectVO result = new K8sCustomObjectVO();

        String name = metadata.get("name").toString();
        if(name != null && !"".equals(name)) {
            result.setName(name);
        }

        Map<String, String> annotations = (Map<String, String>) metadata.get("annotations");
        if(annotations != null && !annotations.isEmpty()) {
            result.setAnnotations(annotations);
        } else {
            result.setAnnotations(null);
        }

        if(metadata.get("namespace") != null) {
            String namespace = metadata.get("namespace").toString();
            if(namespace != null && !"".equals(namespace)) {
                result.setNamespace(namespace);
            }
        }

        Map<String, String> labels = (Map<String, String>) metadata.get("labels");
        if(labels != null && !labels.isEmpty()) {
            result.setLabels(labels);
        } else {
            result.setLabels(null);
        }

        if(metadata.get("creationTimestamp") != null) {
            String tmpTimestamp = metadata.get("creationTimestamp").toString();
            OffsetDateTime creationDatetime = OffsetDateTime.parse(tmpTimestamp);
            if(creationDatetime != null && !"".equals(creationDatetime.toString())) {
                result.setCreationTimestamp(creationDatetime);
            }
        }

        if(spec != null) {
            result.setSpec(spec);
        } else {
            result.setSpec(null);
        }

        return result;
    }

    public List<Map<String, Object>> getCustomObjectsByMultiple(ClusterVO cluster, List<Map<String, Object>> objects) throws Exception {
        List<Map<String, Object>> results = new ArrayList<>();

        for(Map<String, Object> spec : objects) {
            List<String> versions = (List<String>)spec.get("versions");
            if(versions == null || versions.size() <= 0) {
                throw new CocktailException("Not found Version. (required)", ExceptionType.CommonNotFound, ExceptionBiz.CUSTOM_RESOURCE_DEFINITION);
            }

            String name = spec.get("name").toString();
            String group = spec.get("group").toString();
            String plural = spec.get("plural").toString();
            String kind = spec.get("kind").toString();

            Map<String, Object> result = new HashMap<>();
            result.put("name", name);
            result.put("group", group);
            result.put("plural", plural);
            result.put("kind", kind);
            List<Map<String, Object>> items = new ArrayList<>();
            result.put("items", items);

            for(String version : versions) {
                List<Map<String, Object>> customObjects = null;
                customObjects = this.getCustomObjects(cluster, group, version, plural, null);
                if (customObjects == null || customObjects.size() <= 0) {
                    continue;
                }

                Map<String, Object> tmpItem = new HashMap<>();
                tmpItem.put("version", version);
                List<K8sCustomObjectVO> subItems = new ArrayList<>();
                tmpItem.put("items", subItems);

                for(Map<String, Object> customObject : customObjects) {
                    K8sCustomObjectVO tmpCustomObject = this.createCustomObjectVO(customObject);
                    if(tmpCustomObject == null) {
                        continue;
                    }
                    tmpCustomObject.setYaml(this.dumpAsMap(customObject));
                    subItems.add(tmpCustomObject);
                }

                items.add(tmpItem);
            }
        }

        return results;
    }

    public List<Map<String, Object>> getCustomObjectsBySingle(ClusterVO cluster, List<Map<String, Object>> objects) throws Exception {
        List<Map<String, Object>> results = new ArrayList<>();

        for(Map<String, Object> spec : objects) {
            if(spec.get("storedVersion") == null) {
                throw new CocktailException("Not found Version. (required)", ExceptionType.CommonFail, ExceptionBiz.CUSTOM_RESOURCE_DEFINITION);
            }
            String storedVersion = spec.get("storedVersion").toString();
            if("".equals(storedVersion)) {
                throw new CocktailException("Not found Version. (required)", ExceptionType.CommonFail, ExceptionBiz.CUSTOM_RESOURCE_DEFINITION);
            }

            String name = spec.get("name").toString();
            String group = spec.get("group").toString();
            String plural = spec.get("plural").toString();
            String kind = spec.get("kind").toString();

            Map<String, Object> result = new HashMap<>();
            result.put("name", name);
            result.put("group", group);
            result.put("plural", plural);
            result.put("kind", kind);
            List<K8sCustomObjectVO> items = new ArrayList<>();
            result.put("items", items);

            result.put("version", storedVersion);

            List<Map<String, Object>> customObjects = null;
            customObjects = this.getCustomObjects(cluster, group, storedVersion, plural, null);
            if (customObjects == null) {
                continue;
            }

            for(Map<String, Object> customObject : customObjects) {
                K8sCustomObjectVO tmpCustomObject = this.createCustomObjectVO(customObject);
                if(tmpCustomObject == null) {
                    continue;
                }
                tmpCustomObject.setYaml(this.dumpAsMap(customObject));
                items.add(tmpCustomObject);
            }

            results.add(result);
        }

        return results;
    }

    public List<Map<String, Object>> getCustomObjects(ClusterVO cluster, String group, String version, String plural, String label) throws Exception {
        List<Map<String, Object>> results = k8sWorker.getCustomObjectsV1Call(cluster, cluster.getNamespaceName(), group, version, plural, label);
        for(Map<String, Object> result : results) {
            this.removeManagedFields(result);
        }
        return results;
    }

    public Map<String, Object> getCustomObject(ClusterVO cluster, String name, String group, String version, String plural) throws Exception {
        Map<String, Object> result = k8sWorker.getCustomObjectV1Call(cluster, cluster.getNamespaceName(), name, group, version, plural);
        return this.removeManagedFields(result);
    }

    public Map<String, Object> removeManagedFields(Map<String, Object> result) {
        if(result == null) {
            return null;
        }
        Map<String, Object> metadata = (Map<String, Object>) result.get("metadata");
        if(metadata == null || metadata.size() <= 0) {
            return result;
        }
        List<Map<String, Object>> managedFields = null;
        managedFields = (List<Map<String, Object>>)metadata.get("managedFields");
        if(managedFields == null || managedFields.size() <= 0) {
            return result;
        } else {
            managedFields.clear();
            metadata.remove("managedFields");
        }
        return result;
    }

    public Map<String, Object> replaceCustomObject(ClusterVO cluster, String name, String group, String version, String plural, K8sCRDYamlVO yaml) throws Exception {
        if(cluster == null) {
            throw new CocktailException("Cluster not found.", ExceptionType.ClusterNotFound, ExceptionBiz.CUSTOM_RESOURCE_DEFINITION);
        }

        if(StringUtils.isNotBlank(cluster.getNamespaceName())) {
            this.requireNamespace(cluster, cluster.getNamespaceName());
        }

        Map<String, Object> body = this.loadMap(yaml.getYaml());
        if(body == null || body.size() <= 0) {
            throw new CocktailException("Invalid parameter.", ExceptionType.InvalidYamlData, ExceptionBiz.CUSTOM_RESOURCE_DEFINITION);
        }

        Map<String, Object> prev = this.getCustomObject(cluster, name, group, version, plural);
        if (prev == null || prev.size() <= 0) {
            throw new CocktailException("Not found Custom Object.", ExceptionType.InvalidParameter, ExceptionBiz.CUSTOM_RESOURCE_DEFINITION);
        }

		Map<String, Object> prevMeta = (Map<String, Object>) prev.get("metadata");
		if (prevMeta == null || prevMeta.size() <= 0) {
			throw new CocktailException("Not found applied Metadata. (required)", ExceptionType.InvalidParameter, ExceptionBiz.CUSTOM_RESOURCE_DEFINITION);
		}

		Map<String, Object> bodyMeta = (Map<String, Object>) body.get("metadata");
		if (bodyMeta == null || bodyMeta.size() <= 0) {
			throw new CocktailException("Not found requested Metadata. (required)", ExceptionType.InvalidParameter, ExceptionBiz.CUSTOM_RESOURCE_DEFINITION);
		}

        bodyMeta.put("resourceVersion", prevMeta.get("resourceVersion").toString());

        if(bodyMeta.get("namespace") != null) {
            String bodyNamespace = bodyMeta.get("namespace").toString();
            if(StringUtils.isNotBlank(bodyNamespace)) {
                this.requireNamespace(cluster, bodyNamespace);
            }
        }

		// 요청 본문에 Managed Fields 값이 있을 경우 삭제.
		this.removeManagedFields(body);

		Map<String, Object> result = null;
		result = k8sWorker.replaceCustomObjectV1Call(cluster, cluster.getNamespaceName(), name, group, version, plural, body);
		return this.removeManagedFields(result);
    }

    public Map<String, Object> deleteCustomObject(ClusterVO cluster, String name, String group, String version, String plural) throws Exception {
        Map<String, Object> customObject = k8sWorker.getCustomObjectV1Call(cluster, cluster.getNamespaceName(), name, group, version, plural);
        if(customObject != null) {
//            throw new CocktailException("Not found Custom Object.", ExceptionType.CommonNotFound, ExceptionBiz.CUSTOM_RESOURCE_DEFINITION);
			Map<String, Object> result = null;
			result = k8sWorker.deleteCustomObjectV1Call(cluster, cluster.getNamespaceName(), name, group, version, plural);
			return this.removeManagedFields(result);
        }

        return null;
    }

    private String combinePluralGroup(String plural, String group) {
        return plural + "." + group;
    }

    private V1beta1CustomResourceDefinition loadV1beta1(String body) throws Exception {
        try {
            Object object = ServerUtils.getYamlObjects(body).get(0);
            JsonNode objNode = ObjectMapperUtils.getPatchMapper().valueToTree(object);
            String objStr = ObjectMapperUtils.getPatchMapper().writeValueAsString(objNode);
            V1beta1CustomResourceDefinition customResourceDef = null;
            customResourceDef = ObjectMapperUtils.getMapper().readValue(objStr, new TypeReference<V1beta1CustomResourceDefinition>(){});
            if (customResourceDef != null) {
                return customResourceDef;
            }
        }
        catch (CocktailException ce) {
            throw ce;
        }
        catch(Exception e) {
            throw new CocktailException("Invalid YAML format.", ExceptionType.InvalidYamlData, ExceptionBiz.CUSTOM_RESOURCE_DEFINITION);
        }
        return null;
    }

    private V1CustomResourceDefinition loadV1(String body) throws Exception {
        try {
            Object object = ServerUtils.getYamlObjects(body).get(0);
            JsonNode objNode = ObjectMapperUtils.getPatchMapper().valueToTree(object);
            String objStr = ObjectMapperUtils.getPatchMapper().writeValueAsString(objNode);
            V1CustomResourceDefinition customResourceDef = null;
            customResourceDef = ObjectMapperUtils.getMapper().readValue(objStr, new TypeReference<V1CustomResourceDefinition>(){});
            if (customResourceDef != null) {
                return customResourceDef;
            }
        } catch (CocktailException ce) {
            throw ce;
        } catch(Exception e) {
            throw new CocktailException("Invalid YAML format.", ExceptionType.InvalidYamlData, ExceptionBiz.CUSTOM_RESOURCE_DEFINITION);
        }
        return null;
    }

    private Map<String, Object> loadMap(String body) {
        Map<String, Object> result = null;
        try {
            result = run.acloud.api.k8sextended.util.Yaml.getSnakeYaml().load(body);
            if(result != null && result.size() > 0) {
                if(result.get("apiVersion") == null ||
                    result.get("kind") == null ||
                    result.get("metadata") == null) {
                    return null;
                }
            }
        } catch (YAMLException ye) {
            log.warn(ye.getMessage());
        } catch(Exception e) {
            log.warn(e.getMessage());
        }
        return result;
    }

	public <T> String dumpAsMap(T defObj) {
		return Yaml.getSnakeYaml().dumpAsMap(defObj);
	}

    private K8sApiType getApiType(ClusterVO cluster) {
        String version = cluster.getK8sVersion();
        int endIndex = version.lastIndexOf('.');
        K8sApiVerType verType = K8sApiVerType.getApiVerType(version.substring(0, endIndex));
        K8sApiKindType kindType = K8sApiKindType.CUSTOM_RESOURCE_DEFINITION;
        K8sApiVerKindType verKindType = K8sApiVerKindType.getApiType(kindType, verType);
        return verKindType.getApiType();
    }

    private boolean validateCustomResourceDefinition(V1beta1CustomResourceDefinition def) {
        return (def.getApiVersion() != null ||
            def.getKind() != null ||
            def.getSpec() != null ||
            def.getMetadata() != null ||
            def.getStatus() != null) ? true : false;
    }

    private boolean validateCustomResourceDefinition(V1CustomResourceDefinition def) {
        return (def.getApiVersion() != null ||
            def.getKind() != null ||
            def.getSpec() != null ||
            def.getMetadata() != null ||
            def.getStatus() != null) ? true : false;
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
