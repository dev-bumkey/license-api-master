package run.acloud.api.resource.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.JsonObject;
import io.kubernetes.client.openapi.JSON;
import io.kubernetes.client.openapi.models.*;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import run.acloud.api.configuration.dao.IClusterMapper;
import run.acloud.api.configuration.service.AddonCommonService;
import run.acloud.api.configuration.vo.ClusterVO;
import run.acloud.api.cserver.util.ServerUtils;
import run.acloud.api.k8sextended.models.NetworkingV1beta1HTTPIngressPath;
import run.acloud.api.k8sextended.models.NetworkingV1beta1Ingress;
import run.acloud.api.k8sextended.models.NetworkingV1beta1IngressRule;
import run.acloud.api.k8sextended.models.NetworkingV1beta1IngressTLS;
import run.acloud.api.k8sextended.util.Yaml;
import run.acloud.api.resource.constants.KubeConstants;
import run.acloud.api.resource.enums.*;
import run.acloud.api.resource.task.K8sPatchSpecFactory;
import run.acloud.api.resource.task.K8sSpecFactory;
import run.acloud.api.resource.task.K8sWorker;
import run.acloud.api.resource.util.ResourceUtil;
import run.acloud.api.resource.vo.*;
import run.acloud.commons.util.JsonUtils;
import run.acloud.commons.vo.ExecutingContextVO;
import run.acloud.framework.context.ContextHolder;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class IngressSpecService {
    @Resource(name = "cocktailSession")
    private SqlSessionTemplate sqlSession;

    @Autowired
    private K8sWorker k8sWorker;

    @Autowired
    private WorkloadResourceService workloadResourceService;

    @Autowired
    private AddonCommonService addonCommonService;

    @Autowired
    private K8sPatchSpecFactory k8sPatchSpecFactory;


    /**
     * K8S Ingress 생성 ( 변경 spec )
     *
     * @param cluster
     * @param namespaceName
     * @param ingressSpec
     * @param componentId
     * @param context
     * @return
     * @throws Exception
     */
    public K8sIngressVO createIngress(ClusterVO cluster, String namespaceName, IngressSpecGuiVO ingressSpec, String componentId, ExecutingContextVO context) throws Exception {

        K8sIngressVO k8sIngress = null;

        K8sApiVerKindType apiVerKindType = ResourceUtil.getApiVerKindType(cluster, K8sApiKindType.INGRESS);

        // k8s 1.19 부터 추가된 Networking group의 ingress V1을 기본으로 생성함
        if (apiVerKindType.getGroupType() == K8sApiGroupType.NETWORKING && apiVerKindType.getApiType() == K8sApiType.V1) {

            V1Ingress ingress = K8sSpecFactory.buildIngressNetworkingV1(ingressSpec, namespaceName, componentId);
            if(ingress != null && ingress.getSpec() != null && StringUtils.isBlank(ingress.getSpec().getIngressClassName())) {
            	String ingressClassName = ingressSpec.getIngressControllerName();
            	ingress.getSpec().setIngressClassName(ingressClassName);
            }
            if(ingress != null && ingress.getMetadata() != null && ingress.getMetadata().getAnnotations() != null) {
                HashMap<String, String> annotations = (HashMap<String, String>)ingress.getMetadata().getAnnotations();
                if(annotations.containsKey(KubeConstants.META_ANNOTATIONS_INGRESSCLASS)) {
                    annotations.remove(KubeConstants.META_ANNOTATIONS_INGRESSCLASS);
                }
			}
            k8sWorker.createIngressNetworkingV1(cluster, namespaceName, ingress, false);
            Thread.sleep(100);

            k8sIngress = this.getIngress(cluster, namespaceName, ingressSpec.getName(), context);
        }
        // k8s 1.14 부터 추가된 Networking group의 ingress를 기본으로 생성함
        else if (apiVerKindType.getGroupType() == K8sApiGroupType.NETWORKING && apiVerKindType.getApiType() == K8sApiType.V1BETA1) {

            NetworkingV1beta1Ingress ingress = K8sSpecFactory.buildIngressNetworkingV1beta1(ingressSpec, namespaceName, componentId);
            if (K8sApiVerType.isK8sVerSupported(cluster.getK8sVersion(), K8sApiVerType.V1_18)) {
                if(ingress != null && ingress.getSpec() != null && StringUtils.isBlank(ingress.getSpec().getIngressClassName())) {
                    String ingressClassName = ingressSpec.getIngressControllerName();
                    ingress.getSpec().setIngressClassName(ingressClassName);
                }
                if(ingress != null && ingress.getMetadata() != null && ingress.getMetadata().getAnnotations() != null) {
                    HashMap<String, String> annotations = (HashMap<String, String>)ingress.getMetadata().getAnnotations();
                    if(annotations.containsKey(KubeConstants.META_ANNOTATIONS_INGRESSCLASS)) {
                        annotations.remove(KubeConstants.META_ANNOTATIONS_INGRESSCLASS);
                    }
                }
            }
            k8sWorker.createIngressNetworkingV1beat1(cluster, namespaceName, ingress, false);
            Thread.sleep(100);

            k8sIngress = this.getIngress(cluster, namespaceName, ingressSpec.getName(), context);
        }

        return k8sIngress;
    }

    public void createIngress(ClusterVO cluster, String namespace, String yamlStr, ExecutingContextVO context) throws Exception {
        Map<String, Object> ingressObjMap = ServerUtils.getK8sYamlToMap(yamlStr);

        K8sApiKindType apiKindType = ServerUtils.getK8sKindInMap(ingressObjMap);
        if (apiKindType == K8sApiKindType.INGRESS) {
            K8sApiVerKindType apiVerKindType = ResourceUtil.getApiVerKindType(cluster, apiKindType);
            if (apiVerKindType != null) {
                if (apiVerKindType.getGroupType() == K8sApiGroupType.NETWORKING && apiVerKindType.getApiType() == K8sApiType.V1) {
                    V1Ingress createIngress = Yaml.loadAs(yamlStr, V1Ingress.class);
                    createIngress.setApiVersion(String.format("%s/%s", K8sApiGroupType.NETWORKING.getValue(), K8sApiType.V1.getValue()));
                    this.createIngress(cluster, namespace, createIngress, ContextHolder.exeContext());
                } else if (apiVerKindType.getGroupType() == K8sApiGroupType.NETWORKING && apiVerKindType.getApiType() == K8sApiType.V1BETA1) {
                    NetworkingV1beta1Ingress createIngress = Yaml.loadAs(yamlStr, NetworkingV1beta1Ingress.class);
                    createIngress.setApiVersion(String.format("%s/%s", K8sApiGroupType.NETWORKING.getValue(), K8sApiType.V1BETA1.getValue()));
                    this.createIngress(cluster, namespace, createIngress, ContextHolder.exeContext());
                }
            } else {
                log.error("Invalid Ingress API not support version : cluster[{}] : createIngress : {}\n{}", cluster.getK8sVersion(), apiKindType.getCode(), yamlStr);
            }
        } else {
            log.error("Invalid API Kind Type : createIngress : {}\n{}", apiKindType.getCode(), yamlStr);
        }

    }

    /**
     * Ingress 생성
     *
     * @param cluster
     * @param namespace
     * @param createIngress
     * @param context
     * @return
     * @throws Exception
     */
    public NetworkingV1beta1Ingress createIngress(ClusterVO cluster, String namespace, NetworkingV1beta1Ingress createIngress, ExecutingContextVO context) throws Exception {
        // 현재 Ingress 조회
        NetworkingV1beta1Ingress currentIngress = k8sWorker.getIngressNetworkingV1beta1(cluster, namespace, createIngress.getMetadata().getName());
        if (currentIngress != null) {
            throw new CocktailException("Ingress already exists!!", ExceptionType.IngressNameAlreadyExists);
        }

        return k8sWorker.createIngressNetworkingV1beat1(cluster, cluster.getNamespaceName(), createIngress, false);
    }

    /**
     * Ingress 생성
     *
     * @param cluster
     * @param namespace
     * @param createIngress
     * @param context
     * @return
     * @throws Exception
     */
    public V1Ingress createIngress(ClusterVO cluster, String namespace, V1Ingress createIngress, ExecutingContextVO context) throws Exception {
        // 현재 Ingress 조회
        V1Ingress currentIngress = k8sWorker.getIngressNetworkingV1(cluster, namespace, createIngress.getMetadata().getName());
        if (currentIngress != null) {
            throw new CocktailException("Ingress already exists!!", ExceptionType.IngressNameAlreadyExists);
        }

        return k8sWorker.createIngressNetworkingV1(cluster, cluster.getNamespaceName(), createIngress, false);
    }

    /**
     * K8S Ingress Patch ( 변경 spec )
     *
     * @param cluster
     * @param namespaceName
     * @param ingressSpec
     * @param context
     * @return
     * @throws Exception
     */
    public K8sIngressVO patchIngress(ClusterVO cluster, String namespaceName, IngressSpecGuiVO ingressSpec, String componentId, ExecutingContextVO context) throws Exception {

        K8sIngressVO k8sIngress = null;

        NetworkingV1beta1Ingress currNetworkingV1beta1Ingress = null;
        V1Ingress currNetworkingV1Ingress = null;

        K8sApiVerKindType apiVerKindType = ResourceUtil.getApiVerKindType(cluster, K8sApiKindType.INGRESS);

        if (apiVerKindType != null) {
            if (apiVerKindType.getGroupType() == K8sApiGroupType.NETWORKING && apiVerKindType.getApiType() == K8sApiType.V1) {
                currNetworkingV1Ingress = k8sWorker.getIngressNetworkingV1(cluster, namespaceName, ingressSpec.getName());
            } else if (apiVerKindType.getGroupType() == K8sApiGroupType.NETWORKING && apiVerKindType.getApiType() == K8sApiType.V1BETA1) {
                currNetworkingV1beta1Ingress = k8sWorker.getIngressNetworkingV1beta1(cluster, namespaceName, ingressSpec.getName());
            }

            if (currNetworkingV1Ingress != null) {
                currNetworkingV1Ingress.setStatus(null);

                /** patchData를 만들기 전 Reserved Annotations and Labels 가 삭제되었을 경우 다시 넣어주어 제거할 수 없도록 함.. */
                k8sPatchSpecFactory.saveReservedLabelsAndAnnotations(currNetworkingV1Ingress.getMetadata().getLabels(), ingressSpec.getLabels());

                V1Ingress updatedIngress = K8sSpecFactory.buildIngressNetworkingV1(ingressSpec, namespaceName, componentId);
                List<JsonObject> patchBody = k8sPatchSpecFactory.buildPatch(currNetworkingV1Ingress, updatedIngress);

                updatedIngress = k8sWorker.patchIngressNetworkingV1(cluster, namespaceName, updatedIngress.getMetadata().getName(), patchBody, false);
                if (updatedIngress != null && updatedIngress.getSpec() != null && StringUtils.isBlank(updatedIngress.getSpec().getIngressClassName())) {
                    String ingressClassName = ingressSpec.getIngressControllerName();
                    updatedIngress.getSpec().setIngressClassName(ingressClassName);
                }
                if (updatedIngress != null && updatedIngress.getMetadata() != null && updatedIngress.getMetadata().getAnnotations() != null) {
                    HashMap<String, String> annotations = (HashMap<String, String>) updatedIngress.getMetadata().getAnnotations();
                    if (annotations.containsKey(KubeConstants.META_ANNOTATIONS_INGRESSCLASS)) {
                        annotations.remove(KubeConstants.META_ANNOTATIONS_INGRESSCLASS);
                    }
                }
                Thread.sleep(100);

                k8sIngress = this.getIngress(cluster, namespaceName, updatedIngress.getMetadata().getName(), context);
            } else if (currNetworkingV1beta1Ingress != null) {
                currNetworkingV1beta1Ingress.setStatus(null);

                /** patchData를 만들기 전 Reserved Annotations and Labels 가 삭제되었을 경우 다시 넣어주어 제거할 수 없도록 함.. */
                k8sPatchSpecFactory.saveReservedLabelsAndAnnotations(currNetworkingV1beta1Ingress.getMetadata().getLabels(), ingressSpec.getLabels());

                NetworkingV1beta1Ingress updatedIngress = K8sSpecFactory.buildIngressNetworkingV1beta1(ingressSpec, namespaceName, componentId);
                List<JsonObject> patchBody = k8sPatchSpecFactory.buildPatch(currNetworkingV1beta1Ingress, updatedIngress);

                updatedIngress = k8sWorker.patchIngressNetworkingV1beat1(cluster, namespaceName, updatedIngress.getMetadata().getName(), patchBody, false);
                if (K8sApiVerType.isK8sVerSupported(cluster.getK8sVersion(), K8sApiVerType.V1_18)) {
                    if (updatedIngress != null && updatedIngress.getSpec() != null && StringUtils.isBlank(updatedIngress.getSpec().getIngressClassName())) {
                        String ingressClassName = ingressSpec.getIngressControllerName();
                        updatedIngress.getSpec().setIngressClassName(ingressClassName);
                    }
                    if (updatedIngress != null && updatedIngress.getMetadata() != null && updatedIngress.getMetadata().getAnnotations() != null) {
                        HashMap<String, String> annotations = (HashMap<String, String>) updatedIngress.getMetadata().getAnnotations();
                        if (annotations.containsKey(KubeConstants.META_ANNOTATIONS_INGRESSCLASS)) {
                            annotations.remove(KubeConstants.META_ANNOTATIONS_INGRESSCLASS);
                        }
                    }
                }
                Thread.sleep(100);

                k8sIngress = this.getIngress(cluster, namespaceName, updatedIngress.getMetadata().getName(), context);
            }
        }

        return k8sIngress;
    }

    public K8sIngressVO patchIngress(ClusterVO cluster, String namespaceName, IngressSpecGuiVO ingressSpecGui, ExecutingContextVO context) throws Exception {

        NetworkingV1beta1Ingress currNetworkingV1beta1Ingress = null;
        V1Ingress currNetworkingV1Ingress = null;

        K8sApiVerKindType apiVerKindType = ResourceUtil.getApiVerKindType(cluster, K8sApiKindType.INGRESS);

        if (apiVerKindType != null) {
            if (apiVerKindType.getGroupType() == K8sApiGroupType.NETWORKING && apiVerKindType.getApiType() == K8sApiType.V1) {
                currNetworkingV1Ingress = k8sWorker.getIngressNetworkingV1(cluster, namespaceName, ingressSpecGui.getName());
            } else if (apiVerKindType.getGroupType() == K8sApiGroupType.NETWORKING && apiVerKindType.getApiType() == K8sApiType.V1BETA1) {
                currNetworkingV1beta1Ingress = k8sWorker.getIngressNetworkingV1beta1(cluster, namespaceName, ingressSpecGui.getName());
            }

            if (currNetworkingV1Ingress != null) {
                currNetworkingV1Ingress.setStatus(null);

                // 수정할 ingress 병합
                V1Ingress updatedIngress = this.mergeIngressSpec(cluster, namespaceName, ingressSpecGui, currNetworkingV1Ingress);
                if (updatedIngress != null && updatedIngress.getSpec() != null && StringUtils.isBlank(updatedIngress.getSpec().getIngressClassName())) {
                    String ingressClassName = ingressSpecGui.getIngressControllerName();
                    updatedIngress.getSpec().setIngressClassName(ingressClassName);
                }
                if (updatedIngress != null && updatedIngress.getMetadata() != null && updatedIngress.getMetadata().getAnnotations() != null) {
                    HashMap<String, String> annotations = (HashMap<String, String>) updatedIngress.getMetadata().getAnnotations();
                    if (annotations.containsKey(KubeConstants.META_ANNOTATIONS_INGRESSCLASS)) {
                        annotations.remove(KubeConstants.META_ANNOTATIONS_INGRESSCLASS);
                    }
                }

                // patchJson 으로 변경
                List<JsonObject> patchBody = k8sPatchSpecFactory.buildPatch(currNetworkingV1Ingress, updatedIngress);
                log.debug("########## Ingress patchBody JSON: {}", JsonUtils.toGson(patchBody));

                // patch
                k8sWorker.patchIngressNetworkingV1(cluster, namespaceName, updatedIngress.getMetadata().getName(), patchBody, false);
                Thread.sleep(100);

                return this.getIngress(cluster, namespaceName, updatedIngress.getMetadata().getName(), context);
            } else if (currNetworkingV1beta1Ingress != null) {
                currNetworkingV1beta1Ingress.setStatus(null);

                // 수정할 ingress 병합
                NetworkingV1beta1Ingress updatedIngress = this.mergeIngressSpec(cluster, namespaceName, ingressSpecGui, currNetworkingV1beta1Ingress);
                if (K8sApiVerType.isK8sVerSupported(cluster.getK8sVersion(), K8sApiVerType.V1_18)) {
                    if (updatedIngress != null && updatedIngress.getSpec() != null && StringUtils.isBlank(updatedIngress.getSpec().getIngressClassName())) {
                        String ingressClassName = ingressSpecGui.getIngressControllerName();
                        updatedIngress.getSpec().setIngressClassName(ingressClassName);
                    }
                    if (updatedIngress != null && updatedIngress.getMetadata() != null && updatedIngress.getMetadata().getAnnotations() != null) {
                        HashMap<String, String> annotations = (HashMap<String, String>) updatedIngress.getMetadata().getAnnotations();
                        if (annotations.containsKey(KubeConstants.META_ANNOTATIONS_INGRESSCLASS)) {
                            annotations.remove(KubeConstants.META_ANNOTATIONS_INGRESSCLASS);
                        }
                    }
                }

                // patchJson 으로 변경
                List<JsonObject> patchBody = k8sPatchSpecFactory.buildPatch(currNetworkingV1beta1Ingress, updatedIngress);
                log.debug("########## Ingress patchBody JSON: {}", JsonUtils.toGson(patchBody));

                // patch
                k8sWorker.patchIngressNetworkingV1beat1(cluster, namespaceName, updatedIngress.getMetadata().getName(), patchBody, false);
                Thread.sleep(100);

                return this.getIngress(cluster, namespaceName, updatedIngress.getMetadata().getName(), context);
            }
        }

        return null;
    }

    public K8sIngressVO patchIngress(ClusterVO cluster, String namespace, NetworkingV1beta1Ingress updatedIngress, ExecutingContextVO context) throws Exception {
        // 현재 Ingress 조회
        NetworkingV1beta1Ingress currentIngress = k8sWorker.getIngressNetworkingV1beta1(cluster, namespace, updatedIngress.getMetadata().getName());
        currentIngress.setStatus(null);

        // patchJson 으로 변경
        List<JsonObject> patchBody = k8sPatchSpecFactory.buildPatch(currentIngress, updatedIngress);
        log.debug("########## Ingress patchBody JSON: {}", JsonUtils.toGson(patchBody));

        // patch
        k8sWorker.patchIngressNetworkingV1beat1(cluster, namespace, updatedIngress.getMetadata().getName(), patchBody, false);
        Thread.sleep(100);

        return this.getIngress(cluster, namespace, updatedIngress.getMetadata().getName(), context);
    }

    public void patchIngress(ClusterVO cluster, String namespace, NetworkingV1beta1Ingress currentIngress, NetworkingV1beta1Ingress updatedIngress, boolean dryRun, ExecutingContextVO context) throws Exception {
        // 현재 Ingress 조회
        currentIngress.setStatus(null);
        updatedIngress.setStatus(null);

        // patchJson 으로 변경
        List<JsonObject> patchBody = k8sPatchSpecFactory.buildPatch(currentIngress, updatedIngress);
        log.debug("########## Ingress patchBody JSON: {}", JsonUtils.toGson(patchBody));

        // patch
        k8sWorker.patchIngressNetworkingV1beat1(cluster, namespace, updatedIngress.getMetadata().getName(), patchBody, dryRun);
    }

    public K8sIngressVO patchIngress(ClusterVO cluster, String namespace, V1Ingress updatedIngress, ExecutingContextVO context) throws Exception {
        // 현재 Ingress 조회
        V1Ingress currentIngress = k8sWorker.getIngressNetworkingV1(cluster, namespace, updatedIngress.getMetadata().getName());
        currentIngress.setStatus(null);

        // patchJson 으로 변경
        List<JsonObject> patchBody = k8sPatchSpecFactory.buildPatch(currentIngress, updatedIngress);
        log.debug("########## Ingress patchBody JSON: {}", JsonUtils.toGson(patchBody));

        // patch
        k8sWorker.patchIngressNetworkingV1(cluster, namespace, updatedIngress.getMetadata().getName(), patchBody, false);
        Thread.sleep(100);

        return this.getIngress(cluster, namespace, updatedIngress.getMetadata().getName(), context);
    }

    public void patchIngress(ClusterVO cluster, String namespace, V1Ingress currentIngress, V1Ingress updatedIngress, boolean dryRun, ExecutingContextVO context) throws Exception {
        // 현재 Ingress 조회
        currentIngress.setStatus(null);
        updatedIngress.setStatus(null);

        // patchJson 으로 변경
        List<JsonObject> patchBody = k8sPatchSpecFactory.buildPatch(currentIngress, updatedIngress);
        log.debug("########## Ingress patchBody JSON: {}", JsonUtils.toGson(patchBody));

        // patch
        k8sWorker.patchIngressNetworkingV1(cluster, namespace, updatedIngress.getMetadata().getName(), patchBody, dryRun);
    }

    public K8sIngressVO patchIngress(ClusterVO cluster, String namespace, String yamlStr, ExecutingContextVO context) throws Exception {
        Map<String, Object> ingressObjMap = ServerUtils.getK8sYamlToMap(yamlStr);

        K8sApiKindType apiKindType = ServerUtils.getK8sKindInMap(ingressObjMap);
//        K8sApiGroupType apiGroupType = ServerUtils.getK8sGroupInMap(ingressObjMap);
//        K8sApiType apiVerType = ServerUtils.getK8sVersionInMap(ingressObjMap);

        if (apiKindType != null && apiKindType == K8sApiKindType.INGRESS) {
            K8sApiVerKindType apiVerKindType = ResourceUtil.getApiVerKindType(cluster, apiKindType);

            if (apiVerKindType != null) {
                if (apiVerKindType.getGroupType() == K8sApiGroupType.NETWORKING && apiVerKindType.getApiType() == K8sApiType.V1) {
                    V1Ingress updatedIngress = Yaml.loadAs(yamlStr, V1Ingress.class);
                    return this.patchIngress(cluster, namespace, updatedIngress, ContextHolder.exeContext());
                } else if (apiVerKindType.getGroupType() == K8sApiGroupType.NETWORKING && apiVerKindType.getApiType() == K8sApiType.V1BETA1) {
                    NetworkingV1beta1Ingress updatedIngress = Yaml.loadAs(yamlStr, NetworkingV1beta1Ingress.class);
                    return this.patchIngress(cluster, namespace, updatedIngress, ContextHolder.exeContext());
                }
            }
        }

        return null;
    }

    /**
     * K8S Ingress 삭제
     *
     * @param cluster
     * @param namespace
     * @param name
     * @param context
     * @throws Exception
     */
    public void deleteIngress(ClusterVO cluster, String namespace, String name, ExecutingContextVO context) throws Exception {

        K8sApiVerKindType apiVerKindType = ResourceUtil.getApiVerKindType(cluster, K8sApiKindType.INGRESS);

        if (apiVerKindType != null) {
            if (apiVerKindType.getGroupType() == K8sApiGroupType.NETWORKING && apiVerKindType.getApiType() == K8sApiType.V1) {
                V1Ingress networkingIngress = k8sWorker.getIngressNetworkingV1(cluster, namespace, name);
                if (networkingIngress != null) {
                    k8sWorker.deleteIngressNetworkingV1(cluster, namespace, name);
                    Thread.sleep(500);
                }
            } else if (apiVerKindType.getGroupType() == K8sApiGroupType.NETWORKING && apiVerKindType.getApiType() == K8sApiType.V1BETA1) {
                NetworkingV1beta1Ingress networkingIngress = k8sWorker.getIngressNetworkingV1beta1(cluster, namespace, name);
                if (networkingIngress != null) {
                    k8sWorker.deleteIngressNetworkingV1beta1(cluster, namespace, name);
                    Thread.sleep(500);
                }
            }
        }

    }

    /**
     * K8S Ingress 정보 조회
     * (cluster > namespace)
     *
     * @param clusterSeq
     * @param namespaceName
     * @return
     * @throws Exception
     */
    public List<K8sIngressVO> getIngresses(Integer clusterSeq, String namespaceName, ExecutingContextVO context) throws Exception {

        try {
            IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
            ClusterVO cluster = clusterDao.getCluster(clusterSeq);

            return this.getIngresses(cluster, namespaceName, context);
        } catch (CocktailException e) {
            throw e;
        } catch (Exception e) {
            throw new CocktailException("getIngresses fail!!", e, ExceptionType.K8sCocktailCloudInquireFail);
        }
    }

    /**
     * K8S Ingress 정보 조회
     * (cluster > namespace)
     *
     * @param cluster
     * @param namespaceName
     * @return
     * @throws Exception
     */
    public List<K8sIngressVO> getIngresses(ClusterVO cluster, String namespaceName, ExecutingContextVO context) throws Exception {

        try {
            if (StringUtils.isNotBlank(namespaceName)) {
                if (cluster != null) {
                    String field = String.format("%s.%s=%s", KubeConstants.META, KubeConstants.META_NAMESPACE, namespaceName);

                    return this.getIngresses(cluster, field, null, context);
                } else {
                    throw new CocktailException("cluster is null.", ExceptionType.InvalidParameter);
                }
            } else {
                throw new CocktailException(String.format("%s is null.", (StringUtils.isNotBlank(namespaceName) ? "namespaceName" : "Unknown")), ExceptionType.InvalidParameter);
            }
        } catch (CocktailException e) {
            throw e;
        } catch (Exception e) {
            throw new CocktailException("getIngresses fail!!", e, ExceptionType.K8sCocktailCloudInquireFail);
        }
    }

    /**
     * K8S Ingress 정보 조회
     *
     * @param cluster
     * @param field
     * @param label
     * @param context
     * @return List<K8sIngressVO>
     * @throws Exception
     */
    public List<K8sIngressVO> getIngresses(ClusterVO cluster, String field, String label, ExecutingContextVO context) throws Exception {

        try {
            if (cluster != null) {
                return this.convertIngressDataList(cluster, field, label, context);
            } else {
                throw new CocktailException("cluster is null.", ExceptionType.InvalidParameter);
            }
        } catch (CocktailException e) {
            throw e;
        } catch (Exception e) {
            throw new CocktailException("getIngresses fail!!", e, ExceptionType.K8sCocktailCloudInquireFail);
        }
    }

    /**
     * K8S Ingress 정보 조회
     *
     * @param cluster
     * @param namespace
     * @param name
     * @param context
     * @return
     * @throws Exception
     */
    public K8sIngressVO getIngress(ClusterVO cluster, String namespace, String name, ExecutingContextVO context) throws Exception {

        if (cluster != null && StringUtils.isNotBlank(namespace) && StringUtils.isNotBlank(name)) {
            NetworkingV1beta1Ingress networkingV1beta1Ingress = null;
            V1Ingress networkingV1Ingress = null;

            K8sApiVerKindType apiVerKindType = ResourceUtil.getApiVerKindType(cluster, K8sApiKindType.INGRESS);

            // Parameter로 입력된 namespace를 사용하도록 변경..
            // V1beta1Ingress v1beta1Ingress = k8sWorker.getIngressExtensionsV1beta1(cluster, cluster.getNamespaceName(), name);
            if (apiVerKindType != null) {
                if (apiVerKindType.getGroupType() == K8sApiGroupType.NETWORKING && apiVerKindType.getApiType() == K8sApiType.V1) {
                    networkingV1Ingress = k8sWorker.getIngressNetworkingV1(cluster, namespace, name);
                } else if (apiVerKindType.getGroupType() == K8sApiGroupType.NETWORKING && apiVerKindType.getApiType() == K8sApiType.V1BETA1) {
                    networkingV1beta1Ingress = k8sWorker.getIngressNetworkingV1beta1(cluster, namespace, name);
                }


                if (networkingV1beta1Ingress != null) {
                    return this.convertIngressData(new K8sIngressVO(), networkingV1beta1Ingress, new JSON());
                } else if (networkingV1Ingress != null) {
                    return this.convertIngressData(new K8sIngressVO(), networkingV1Ingress, new JSON());
                } else {
                    return null;
                }
            }
        } else {
            throw new CocktailException("cluster/namespace/name is null.", ExceptionType.InvalidParameter);
        }

        return null;
    }

    /**
     * K8s Ingress 정보 조회. (K8s API Version 1.19 이상)
     *
     * @param cluster
     * @param namespace
     * @param field
     * @param label
     * @return
     * @throws Exception
     */
    public List<V1Ingress> getIngressesNetworkingV1(ClusterVO cluster, String namespace, String field, String label) throws Exception {
        if (cluster == null) {
            throw new CocktailException("cluster is null.", ExceptionType.ClusterNotFound);
        }
        try {
            return k8sWorker.getIngressesNetworkingV1(cluster, namespace, field, label);
        }
        catch (CocktailException ce) {
            throw ce;
        }
        catch (Exception e) {
            throw new CocktailException("getIngressesNetworkingV1 fail!!", e, ExceptionType.K8sCocktailCloudInquireFail);
        }
    }

    /**
     * K8s Ingress 정보 조회. (K8s API Version 1.14 이상)
     *
     * @param cluster
     * @param namespace
     * @param field
     * @param label
     * @return
     * @throws Exception
     */
    public List<NetworkingV1beta1Ingress> getIngressesNetworkingV1Beta1(ClusterVO cluster, String namespace, String field, String label) throws Exception {
        if (cluster == null) {
            throw new CocktailException("cluster is null.", ExceptionType.ClusterNotFound);
        }
        try {
            return k8sWorker.getIngressesNetworkingV1Beta1(cluster, namespace, field, label);
        }
        catch (CocktailException ce) {
            throw ce;
        }
        catch (Exception e) {
            throw new CocktailException("getIngressesNetworkingV1Beta1 fail!!", e, ExceptionType.K8sCocktailCloudInquireFail);
        }
    }

    /**
     * K8S Ingress 정보 조회 후 V1beta1Ingress -> K8sIngressVO 변환
     *
     * @param cluster
     * @param field
     * @param label
     * @return
     * @throws Exception
     */
    public List<K8sIngressVO> convertIngressDataList(ClusterVO cluster, String namespace, String field, String label, ExecutingContextVO context) throws Exception {

        List<K8sIngressVO> ingresses = new ArrayList<>();
        List<NetworkingV1beta1Ingress> networkingV1beta1Ingresses = null;
        List<V1Ingress> networkingV1Ingresses = null;

        K8sApiVerKindType apiVerKindType = ResourceUtil.getApiVerKindType(cluster, K8sApiKindType.INGRESS);

        if (apiVerKindType != null) {
            if (apiVerKindType.getGroupType() == K8sApiGroupType.NETWORKING && apiVerKindType.getApiType() == K8sApiType.V1) {
                networkingV1Ingresses = k8sWorker.getIngressesNetworkingV1(cluster, namespace, field, label);
            } else if (apiVerKindType.getGroupType() == K8sApiGroupType.NETWORKING && apiVerKindType.getApiType() == K8sApiType.V1BETA1) {
                networkingV1beta1Ingresses = k8sWorker.getIngressesNetworkingV1Beta1(cluster, namespace, field, label);
            }

            // joda.datetime Serialization
            JSON k8sJson = new JSON();

            if (CollectionUtils.isNotEmpty(networkingV1beta1Ingresses)) {

                for (NetworkingV1beta1Ingress v1beta1IngressRow : networkingV1beta1Ingresses) {
                    K8sIngressVO ingress = new K8sIngressVO();
                    this.convertIngressData(ingress, v1beta1IngressRow, k8sJson);

                    ingresses.add(ingress);

                }
            } else if (CollectionUtils.isNotEmpty(networkingV1Ingresses)) {

                for (V1Ingress v1IngressRow : networkingV1Ingresses) {
                    K8sIngressVO ingress = new K8sIngressVO();
                    this.convertIngressData(ingress, v1IngressRow, k8sJson);

                    ingresses.add(ingress);

                }
            }
        }

        return ingresses;
    }

    /**
     * K8S Ingress 정보 조회 후 V1beta1Ingress -> K8sIngressVO 변환
     *
     * @param cluster
     * @param field
     * @param label
     * @return
     * @throws Exception
     */
    public List<K8sIngressVO> convertIngressDataList(ClusterVO cluster, String field, String label, ExecutingContextVO context) throws Exception {

        List<K8sIngressVO> ingresses = new ArrayList<>();
        List<NetworkingV1beta1Ingress> networkingV1beta1Ingresses = null;
        List<V1Ingress> networkingV1Ingresses = null;

        K8sApiVerKindType apiVerKindType = ResourceUtil.getApiVerKindType(cluster, K8sApiKindType.INGRESS);

        if (apiVerKindType != null) {
            if (apiVerKindType.getGroupType() == K8sApiGroupType.NETWORKING && apiVerKindType.getApiType() == K8sApiType.V1) {
                networkingV1Ingresses = k8sWorker.getIngressesNetworkingV1(cluster, field, label);
            } else if (apiVerKindType.getGroupType() == K8sApiGroupType.NETWORKING && apiVerKindType.getApiType() == K8sApiType.V1BETA1) {
                networkingV1beta1Ingresses = k8sWorker.getIngressesNetworkingV1Beta1(cluster, field, label);
            }

            // joda.datetime Serialization
            JSON k8sJson = new JSON();

            if (CollectionUtils.isNotEmpty(networkingV1Ingresses)) {

                for (V1Ingress v1IngressRow : networkingV1Ingresses) {
                    K8sIngressVO ingress = new K8sIngressVO();
                    this.convertIngressData(ingress, v1IngressRow, k8sJson);

                    ingresses.add(ingress);

                }
            } else if (CollectionUtils.isNotEmpty(networkingV1beta1Ingresses)) {

                for (NetworkingV1beta1Ingress v1beta1IngressRow : networkingV1beta1Ingresses) {
                    K8sIngressVO ingress = new K8sIngressVO();
                    this.convertIngressData(ingress, v1beta1IngressRow, k8sJson);

                    ingresses.add(ingress);

                }

            }
        }

        return ingresses;
    }

    /**
     * K8S Ingress 정보 조회 후 NetworkingV1beta1Ingress -> K8sIngressVO 변환
     *
     * @param ingress
     * @param v1beta1Ingress
     * @throws Exception
     */
    public K8sIngressVO convertIngressData(K8sIngressVO ingress, NetworkingV1beta1Ingress v1beta1Ingress, JSON k8sJson) throws Exception {

        if (v1beta1Ingress != null) {
            if (k8sJson == null) {
                k8sJson = new JSON();
            }
            if (ingress == null) {
                ingress = new K8sIngressVO();
            }

            ingress.setName(v1beta1Ingress.getMetadata().getName());
            ingress.setNamespace(v1beta1Ingress.getMetadata().getNamespace());

            List<String> endpoints = new ArrayList<>();
            if (v1beta1Ingress.getStatus() != null
                    && v1beta1Ingress.getStatus().getLoadBalancer() != null
                    && CollectionUtils.isNotEmpty(v1beta1Ingress.getStatus().getLoadBalancer().getIngress())) {
                for (V1LoadBalancerIngress v1LoadBalancerIngressRow : v1beta1Ingress.getStatus().getLoadBalancer().getIngress()) {
                    if (StringUtils.isNotBlank(v1LoadBalancerIngressRow.getHostname()) || StringUtils.isNotBlank(v1LoadBalancerIngressRow.getIp())) {
                        endpoints.add(StringUtils.isNotBlank(v1LoadBalancerIngressRow.getHostname()) ? v1LoadBalancerIngressRow.getHostname() : v1LoadBalancerIngressRow.getIp());
                    }
                }
            }
            ingress.setEndpoints(endpoints);
            ingress.setCreationTimestamp(v1beta1Ingress.getMetadata().getCreationTimestamp());
            ingress.setDeployment(k8sJson.serialize(v1beta1Ingress));
            ingress.setDeploymentYaml(Yaml.getSnakeYaml().dumpAsMap(v1beta1Ingress));

            K8sIngressDetailVO ingressDetail = new K8sIngressDetailVO();
            ingressDetail.setName(v1beta1Ingress.getMetadata().getName());
            ingressDetail.setNamespace(v1beta1Ingress.getMetadata().getNamespace());
            ingressDetail.setLabels(v1beta1Ingress.getMetadata().getLabels());
            ingressDetail.setAnnotations(v1beta1Ingress.getMetadata().getAnnotations());
            ingressDetail.setCreationTime(v1beta1Ingress.getMetadata().getCreationTimestamp());
            ingress.setDetail(ingressDetail);

            // Ingress 정보
            IngressSpecGuiVO ingressSpec = this.convertIngressSpecData(v1beta1Ingress);
            ingress.setIngressSpec(ingressSpec);

        }

        return ingress;
    }

    /**
     * K8S Ingress 정보 조회 후 Networking V1Ingress -> K8sIngressVO 변환
     *
     * @param ingress
     * @param v1Ingress
     * @throws Exception
     */
    public K8sIngressVO convertIngressData(K8sIngressVO ingress, V1Ingress v1Ingress, JSON k8sJson) throws Exception {

        if (v1Ingress != null) {
            if (k8sJson == null) {
                k8sJson = new JSON();
            }
            if (ingress == null) {
                ingress = new K8sIngressVO();
            }

            ingress.setName(v1Ingress.getMetadata().getName());
            ingress.setNamespace(v1Ingress.getMetadata().getNamespace());

            List<String> endpoints = new ArrayList<>();
            if (v1Ingress.getStatus() != null
                    && v1Ingress.getStatus().getLoadBalancer() != null
                    && CollectionUtils.isNotEmpty(v1Ingress.getStatus().getLoadBalancer().getIngress())) {
                for (V1LoadBalancerIngress v1LoadBalancerIngressRow : v1Ingress.getStatus().getLoadBalancer().getIngress()) {
                    if (StringUtils.isNotBlank(v1LoadBalancerIngressRow.getHostname()) || StringUtils.isNotBlank(v1LoadBalancerIngressRow.getIp())) {
                        endpoints.add(StringUtils.isNotBlank(v1LoadBalancerIngressRow.getHostname()) ? v1LoadBalancerIngressRow.getHostname() : v1LoadBalancerIngressRow.getIp());
                    }
                }
            }
            ingress.setEndpoints(endpoints);
            ingress.setCreationTimestamp(v1Ingress.getMetadata().getCreationTimestamp());
            ingress.setDeployment(k8sJson.serialize(v1Ingress));
            ingress.setDeploymentYaml(Yaml.getSnakeYaml().dumpAsMap(v1Ingress));

            K8sIngressDetailVO ingressDetail = new K8sIngressDetailVO();
            ingressDetail.setName(v1Ingress.getMetadata().getName());
            ingressDetail.setNamespace(v1Ingress.getMetadata().getNamespace());
            ingressDetail.setLabels(v1Ingress.getMetadata().getLabels());
            ingressDetail.setAnnotations(v1Ingress.getMetadata().getAnnotations());
            ingressDetail.setCreationTime(v1Ingress.getMetadata().getCreationTimestamp());
            ingress.setDetail(ingressDetail);

            // Ingress 정보
            IngressSpecGuiVO ingressSpec = this.convertIngressSpecData(v1Ingress);
            ingress.setIngressSpec(ingressSpec);

        }

        return ingress;
    }

    /**
     * 해당 클러스터의 인그레스를 조회하여 사용중인 인그레스 리턴
     *
     * @param cluster
     * @param label
     * @return
     * @throws Exception
     */
    public Map<String, Map<String, Set<String>>> getUsingIngressOfCluster(ClusterVO cluster, String field, String label) throws Exception {

        // Map<class, Map<host, Set<path>>>
        Map<String, Map<String, Set<String>>> ingresses = new HashMap<>();
        String ingressControllerClass = null;

        K8sApiVerKindType apiVerKindType = ResourceUtil.getApiVerKindType(cluster, K8sApiKindType.INGRESS);
        /**
         * TODO: K8s API Spec 변경시 수정 필요
         */
        if (apiVerKindType != null) {
            if (apiVerKindType.getGroupType() == K8sApiGroupType.NETWORKING && apiVerKindType.getApiType() == K8sApiType.V1) {
                List<V1Ingress> v1Ingresses = k8sWorker.getIngressesNetworkingV1(cluster, field, label);
                if (CollectionUtils.isNotEmpty(v1Ingresses)) {
                    for (V1Ingress ingressRow : v1Ingresses) {
                        ingressControllerClass = MapUtils.getString(ingressRow.getMetadata().getAnnotations(), KubeConstants.META_ANNOTATIONS_INGRESSCLASS, KubeConstants.META_ANNOTATIONS_INGRESSCLASS_NGINX_DEFAULT_VALUE);

                        if (CollectionUtils.isNotEmpty(ingressRow.getSpec().getRules())) {
                            for (V1IngressRule ingressRuleRow : ingressRow.getSpec().getRules()) {
                                // set path
                                Set<String> ingressPaths = new HashSet<>();
                                if (ingressRuleRow.getHttp() != null && CollectionUtils.isNotEmpty(ingressRuleRow.getHttp().getPaths())) {
                                    for (V1HTTPIngressPath httpIngressPathRow : ingressRuleRow.getHttp().getPaths()) {
                                        if (StringUtils.isNotBlank(httpIngressPathRow.getPath())) {
                                            ingressPaths.add(httpIngressPathRow.getPath());
                                        }
                                    }
                                }

                                // set ingressControllerClass
                                if (MapUtils.getObject(ingresses, ingressControllerClass, null) == null) {
                                    ingresses.put(ingressControllerClass, Maps.newHashMap());
                                }

                                // set host
                                if (StringUtils.isNotBlank(ingressRuleRow.getHost())) {
                                    if (MapUtils.getObject(ingresses.get(ingressControllerClass), ingressRuleRow.getHost(), null) == null) {
                                        ingresses.get(ingressControllerClass).put(ingressRuleRow.getHost(), ingressPaths);
                                    } else {
                                        ingresses.get(ingressControllerClass).get(ingressRuleRow.getHost()).addAll(ingressPaths);
                                    }
                                } else {
                                    if (MapUtils.getObject(ingresses, KubeConstants.CUSTOM_INGRESS_HTTP_NON_HOST, null) == null) {
                                        ingresses.get(ingressControllerClass).put(KubeConstants.CUSTOM_INGRESS_HTTP_NON_HOST, ingressPaths);
                                    } else {
                                        ingresses.get(ingressControllerClass).get(KubeConstants.CUSTOM_INGRESS_HTTP_NON_HOST).addAll(ingressPaths);
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (apiVerKindType.getGroupType() == K8sApiGroupType.NETWORKING && apiVerKindType.getApiType() == K8sApiType.V1BETA1) {
                List<NetworkingV1beta1Ingress> v1beta1Ingresses = k8sWorker.getIngressesNetworkingV1Beta1(cluster, field, label);
                if (CollectionUtils.isNotEmpty(v1beta1Ingresses)) {
                    for (NetworkingV1beta1Ingress ingressRow : v1beta1Ingresses) {
                        ingressControllerClass = MapUtils.getString(ingressRow.getMetadata().getAnnotations(), KubeConstants.META_ANNOTATIONS_INGRESSCLASS, KubeConstants.META_ANNOTATIONS_INGRESSCLASS_NGINX_DEFAULT_VALUE);

                        if (CollectionUtils.isNotEmpty(ingressRow.getSpec().getRules())) {
                            for (NetworkingV1beta1IngressRule ingressRuleRow : ingressRow.getSpec().getRules()) {
                                // set path
                                Set<String> ingressPaths = new HashSet<>();
                                if (ingressRuleRow.getHttp() != null && CollectionUtils.isNotEmpty(ingressRuleRow.getHttp().getPaths())) {
                                    for (NetworkingV1beta1HTTPIngressPath httpIngressPathRow : ingressRuleRow.getHttp().getPaths()) {
                                        if (StringUtils.isNotBlank(httpIngressPathRow.getPath())) {
                                            ingressPaths.add(httpIngressPathRow.getPath());
                                        }
                                    }
                                }

                                // set ingressControllerClass
                                if (MapUtils.getObject(ingresses, ingressControllerClass, null) == null) {
                                    ingresses.put(ingressControllerClass, Maps.newHashMap());
                                }

                                // set host
                                if (StringUtils.isNotBlank(ingressRuleRow.getHost())) {
                                    if (MapUtils.getObject(ingresses.get(ingressControllerClass), ingressRuleRow.getHost(), null) == null) {
                                        ingresses.get(ingressControllerClass).put(ingressRuleRow.getHost(), ingressPaths);
                                    } else {
                                        ingresses.get(ingressControllerClass).get(ingressRuleRow.getHost()).addAll(ingressPaths);
                                    }
                                } else {
                                    if (MapUtils.getObject(ingresses, KubeConstants.CUSTOM_INGRESS_HTTP_NON_HOST, null) == null) {
                                        ingresses.get(ingressControllerClass).put(KubeConstants.CUSTOM_INGRESS_HTTP_NON_HOST, ingressPaths);
                                    } else {
                                        ingresses.get(ingressControllerClass).get(KubeConstants.CUSTOM_INGRESS_HTTP_NON_HOST).addAll(ingressPaths);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return ingresses;
    }


    public V1Ingress mergeIngressSpec(ClusterVO cluster, String namespace, IngressSpecGuiVO ingressSpecGui, V1Ingress currIngress) throws Exception {

        V1Ingress currCopyIngress = null;
        // 현재 인그레스가 생성되어 있고
        if (currIngress != null) {
            // 수정하는 인그레스의 값이 존재한다면 merge 처리
            if (ingressSpecGui != null) {
                V1Ingress updatedIngress = K8sSpecFactory.buildIngressNetworkingV1(ingressSpecGui, namespace, null);
                currCopyIngress = k8sPatchSpecFactory.copyObject(currIngress, new TypeReference<V1Ingress>() {});

                /**
                 * merge
                 */
                // metadata
                currCopyIngress.getMetadata().setAnnotations(updatedIngress.getMetadata().getAnnotations());
                k8sPatchSpecFactory.saveReservedLabelsAndAnnotations(currCopyIngress.getMetadata().getLabels(), updatedIngress.getMetadata().getLabels());
                currCopyIngress.getMetadata().setLabels(updatedIngress.getMetadata().getLabels());
                // spec
                currCopyIngress.getSpec().setRules(updatedIngress.getSpec().getRules());
                currCopyIngress.getSpec().setTls(updatedIngress.getSpec().getTls());

            }
        }
        // 현재 생성된 인그레스가 없고
        else {
            // 수정하는 인그레스의 값이 존재한다면 merge 없이 currIngress에 셋팅
            if (ingressSpecGui != null) {
                currCopyIngress = K8sSpecFactory.buildIngressNetworkingV1(ingressSpecGui, namespace, null);
            }
        }

        // merge된 값이 있다면 yaml로 변환
        if (currCopyIngress != null) {
            currCopyIngress.setStatus(null);
        }

        return currCopyIngress;
    }

    public NetworkingV1beta1Ingress mergeIngressSpec(ClusterVO cluster, String namespace, IngressSpecGuiVO ingressSpecGui, NetworkingV1beta1Ingress currIngress) throws Exception {

        NetworkingV1beta1Ingress currCopyIngress = null;
        // 현재 인그레스가 생성되어 있고
        if (currIngress != null) {
            // 수정하는 인그레스의 값이 존재한다면 merge 처리
            if (ingressSpecGui != null) {
                NetworkingV1beta1Ingress updatedIngress = K8sSpecFactory.buildIngressNetworkingV1beta1(ingressSpecGui, namespace, null);
                currCopyIngress = k8sPatchSpecFactory.copyObject(currIngress, new TypeReference<NetworkingV1beta1Ingress>() {});

                /**
                 * merge
                 */
                // metadata
                currCopyIngress.getMetadata().setAnnotations(updatedIngress.getMetadata().getAnnotations());
                k8sPatchSpecFactory.saveReservedLabelsAndAnnotations(currCopyIngress.getMetadata().getLabels(), updatedIngress.getMetadata().getLabels());
                currCopyIngress.getMetadata().setLabels(updatedIngress.getMetadata().getLabels());
                // spec
                currCopyIngress.getSpec().setRules(updatedIngress.getSpec().getRules());
                currCopyIngress.getSpec().setTls(updatedIngress.getSpec().getTls());

            }
        }
        // 현재 생성된 인그레스가 없고
        else {
            // 수정하는 인그레스의 값이 존재한다면 merge 없이 currIngress에 셋팅
            if (ingressSpecGui != null) {
                currCopyIngress = K8sSpecFactory.buildIngressNetworkingV1beta1(ingressSpecGui, namespace, null);
            }
        }

        // merge된 값이 있다면 yaml로 변환
        if (currCopyIngress != null) {
            currCopyIngress.setStatus(null);
        }

        return currCopyIngress;
    }

    public IngressSpecGuiVO convertIngressSpecYamlToGui(ClusterVO cluster, String namespace, String yamlStr) throws Exception {
        return this.convertIngressSpecYamlToGui(cluster, null, namespace, yamlStr);
    }

    public IngressSpecGuiVO convertIngressSpecYamlToGui(ClusterVO cluster, Integer servicemapSeq, String namespace, String yamlStr) throws Exception {

        if (StringUtils.isNotBlank(yamlStr)) {
            Map<String, Object> ingressObjMap = ServerUtils.getK8sYamlToMap(yamlStr);

            K8sApiKindType apiKindType = ServerUtils.getK8sKindInMap(ingressObjMap);
            K8sApiGroupType apiGroupType = ServerUtils.getK8sGroupInMap(ingressObjMap);
            K8sApiType apiVerType = ServerUtils.getK8sVersionInMap(ingressObjMap);

            if (apiKindType != null && apiKindType == K8sApiKindType.INGRESS) {
                if (apiGroupType == K8sApiGroupType.NETWORKING && apiVerType == K8sApiType.V1) {
                    V1Ingress updatedIngress = Yaml.loadAs(yamlStr, V1Ingress.class);
                    IngressSpecGuiVO ingressSpec = this.convertIngressSpecData(updatedIngress);
                    ingressSpec.setClusterSeq(cluster.getClusterSeq());
                    ingressSpec.setNamespaceName(namespace);
                    ingressSpec.setServicemapSeq(servicemapSeq);
                    return ingressSpec;
                } else if (apiGroupType == K8sApiGroupType.NETWORKING && apiVerType == K8sApiType.V1BETA1) {
                    NetworkingV1beta1Ingress updatedIngress = Yaml.loadAs(yamlStr, NetworkingV1beta1Ingress.class);
                    IngressSpecGuiVO ingressSpec = this.convertIngressSpecData(updatedIngress);
                    ingressSpec.setClusterSeq(cluster.getClusterSeq());
                    ingressSpec.setNamespaceName(namespace);
                    ingressSpec.setServicemapSeq(servicemapSeq);
                    return ingressSpec;
                }
            }
        }

        return null;
    }

    public IngressSpecGuiVO convertIngressSpecData(NetworkingV1beta1Ingress v1beta1Ingress) throws Exception {
        IngressSpecGuiVO ingressSpec = new IngressSpecGuiVO();
        ingressSpec.setName(v1beta1Ingress.getMetadata().getName());
        ingressSpec.setNamespaceName(v1beta1Ingress.getMetadata().getNamespace());
        ingressSpec.setLabels(v1beta1Ingress.getMetadata().getLabels());
        ingressSpec.setAnnotations(v1beta1Ingress.getMetadata().getAnnotations());
        ingressSpec.setUseSslRedirect(MapUtils.getBoolean(v1beta1Ingress.getMetadata().getAnnotations(), KubeConstants.META_ANNOTATIONS_INGRESSSSLREDIRECT, Boolean.FALSE));
        if (MapUtils.isNotEmpty(v1beta1Ingress.getMetadata().getAnnotations()) && v1beta1Ingress.getMetadata().getAnnotations().containsKey(KubeConstants.META_ANNOTATIONS_INGRESSCLASS)) {
            ingressSpec.setIngressControllerName(MapUtils.getString(v1beta1Ingress.getMetadata().getAnnotations(), KubeConstants.META_ANNOTATIONS_INGRESSCLASS, ""));
        }
        if (v1beta1Ingress != null && v1beta1Ingress.getSpec() != null) {
            String ingressClassName = v1beta1Ingress.getSpec().getIngressClassName();
            if(StringUtils.isNotBlank(ingressClassName)) {
                ingressSpec.setIngressControllerName(ingressClassName);
            }
        }
        List<IngressRuleVO> rules = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(v1beta1Ingress.getSpec().getRules())) {
            for (NetworkingV1beta1IngressRule v1beta1IngressRuleRow : v1beta1Ingress.getSpec().getRules()) {
                IngressRuleVO rule = new IngressRuleVO();
                rule.setHostName(v1beta1IngressRuleRow.getHost());
                if (v1beta1IngressRuleRow.getHttp() != null && CollectionUtils.isNotEmpty(v1beta1IngressRuleRow.getHttp().getPaths())) {
                    List<IngressHttpPathVO> paths = new ArrayList<>();
                    for (NetworkingV1beta1HTTPIngressPath v1beta1HTTPIngressPathRow : v1beta1IngressRuleRow.getHttp().getPaths()) {
                        IngressHttpPathVO path = new IngressHttpPathVO();
                        path.setPath(v1beta1HTTPIngressPathRow.getPath());
                        path.setPathType(v1beta1HTTPIngressPathRow.getPathType());
                        if (v1beta1HTTPIngressPathRow.getBackend() != null) {
                            path.setServiceName(v1beta1HTTPIngressPathRow.getBackend().getServiceName());
                            path.setServicePortIsInteger(v1beta1HTTPIngressPathRow.getBackend().getServicePort().isInteger());
                            if (v1beta1HTTPIngressPathRow.getBackend().getServicePort().isInteger()) {
                                path.setServicePort(String.valueOf(v1beta1HTTPIngressPathRow.getBackend().getServicePort().getIntValue()));
                            } else {
                                path.setServicePort(v1beta1HTTPIngressPathRow.getBackend().getServicePort().getStrValue());
                            }

                        }
                        paths.add(path);
                    }
                    rule.setIngressHttpPaths(paths);
                }
                rules.add(rule);
            }
        }
        List<IngressTLSVO> tlses = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(v1beta1Ingress.getSpec().getTls())) {
            for (NetworkingV1beta1IngressTLS v1beta1IngressTLSRow : v1beta1Ingress.getSpec().getTls()) {
                IngressTLSVO tls = new IngressTLSVO();
                tls.setHosts(v1beta1IngressTLSRow.getHosts());
                tls.setSecretName(v1beta1IngressTLSRow.getSecretName());

                tlses.add(tls);
            }
        }
        ingressSpec.setIngressRules(rules);
        ingressSpec.setIngressTLSs(tlses);

        return ingressSpec;
    }

    public IngressSpecGuiVO convertIngressSpecData(V1Ingress v1Ingress) throws Exception {
        IngressSpecGuiVO ingressSpec = new IngressSpecGuiVO();
        ingressSpec.setName(v1Ingress.getMetadata().getName());
        ingressSpec.setNamespaceName(v1Ingress.getMetadata().getNamespace());
        ingressSpec.setLabels(v1Ingress.getMetadata().getLabels());
        ingressSpec.setAnnotations(v1Ingress.getMetadata().getAnnotations());
        ingressSpec.setUseSslRedirect(MapUtils.getBoolean(v1Ingress.getMetadata().getAnnotations(), KubeConstants.META_ANNOTATIONS_INGRESSSSLREDIRECT, Boolean.FALSE));
        if (MapUtils.isNotEmpty(v1Ingress.getMetadata().getAnnotations()) && v1Ingress.getMetadata().getAnnotations().containsKey(KubeConstants.META_ANNOTATIONS_INGRESSCLASS)) {
            ingressSpec.setIngressControllerName(MapUtils.getString(v1Ingress.getMetadata().getAnnotations(), KubeConstants.META_ANNOTATIONS_INGRESSCLASS, ""));
        }
        if (v1Ingress != null && v1Ingress.getSpec() != null) {
            String ingressClassName = v1Ingress.getSpec().getIngressClassName();
            if(StringUtils.isNotBlank(ingressClassName)) {
                ingressSpec.setIngressControllerName(ingressClassName);
            }
        }
        List<IngressRuleVO> rules = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(v1Ingress.getSpec().getRules())) {
            for (V1IngressRule v1IngressRuleRow : v1Ingress.getSpec().getRules()) {
                IngressRuleVO rule = new IngressRuleVO();
                rule.setHostName(v1IngressRuleRow.getHost());
                if (v1IngressRuleRow.getHttp() != null && CollectionUtils.isNotEmpty(v1IngressRuleRow.getHttp().getPaths())) {
                    List<IngressHttpPathVO> paths = new ArrayList<>();
                    for (V1HTTPIngressPath v1HTTPIngressPathRow : v1IngressRuleRow.getHttp().getPaths()) {
                        IngressHttpPathVO path = new IngressHttpPathVO();
                        path.setPath(v1HTTPIngressPathRow.getPath());
                        path.setPathType(v1HTTPIngressPathRow.getPathType());
                        if (v1HTTPIngressPathRow.getBackend() != null) {
                            path.setServiceName(v1HTTPIngressPathRow.getBackend().getService().getName());

                            path.setServicePortIsInteger(v1HTTPIngressPathRow.getBackend().getService().getPort().getNumber() != null);
                            if (v1HTTPIngressPathRow.getBackend().getService().getPort().getNumber() != null) {
                                path.setServicePort(String.valueOf(v1HTTPIngressPathRow.getBackend().getService().getPort().getNumber().intValue()));
                            } else {
                                path.setServicePort(v1HTTPIngressPathRow.getBackend().getService().getPort().getName());
                            }

                        }
                        paths.add(path);
                    }
                    rule.setIngressHttpPaths(paths);
                }
                rules.add(rule);
            }
        }
        List<IngressTLSVO> tlses = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(v1Ingress.getSpec().getTls())) {
            for (V1IngressTLS v1IngressTLSRow : v1Ingress.getSpec().getTls()) {
                IngressTLSVO tls = new IngressTLSVO();
                tls.setHosts(v1IngressTLSRow.getHosts());
                tls.setSecretName(v1IngressTLSRow.getSecretName());

                tlses.add(tls);
            }
        }
        ingressSpec.setIngressRules(rules);
        ingressSpec.setIngressTLSs(tlses);

        return ingressSpec;
    }


    public void checkIngress(ClusterVO cluster, String namespaceName, boolean isAdd, IngressSpecGuiVO ingressSpecGui) throws Exception {
        K8sIngressVO k8sIngress = null;
        if (isAdd) {
            if (StringUtils.isBlank(ingressSpecGui.getName()) || !ingressSpecGui.getName().matches(KubeConstants.RULE_RESOURCE_NAME)) {
                throw new CocktailException("Ingress name is invalid", ExceptionType.K8sIngressNameInvalid, ResourceUtil.getInvalidNameMsg("Ingress name is invalid"));
            } else {
                k8sIngress = this.getIngress(cluster, namespaceName, ingressSpecGui.getName(), ContextHolder.exeContext());
                if (k8sIngress != null) {
                    throw new CocktailException("Ingress already exists!!", ExceptionType.IngressNameAlreadyExists);
                }
            }
        } else {
            if (StringUtils.isBlank(ingressSpecGui.getName()) || !ingressSpecGui.getName().matches(KubeConstants.RULE_RESOURCE_NAME)) {
                throw new CocktailException("Ingress name is invalid", ExceptionType.K8sIngressNameInvalid, ResourceUtil.getInvalidNameMsg("Ingress name is invalid"));
            } else {
                k8sIngress = this.getIngress(cluster, namespaceName, ingressSpecGui.getName(), ContextHolder.exeContext());
                if (k8sIngress == null) {
                    throw new CocktailException("Ingress not found!!", ExceptionType.K8sIngressNotFound);
                }
            }
        }

        /**
         * rule > servicePort valid 체크
         */
        this.validIngressServicePort(ingressSpecGui.getIngressRules(), true);

        /**
         * Ingress Controller Class 체크
         */
        boolean isExistClassAnno = (MapUtils.isNotEmpty(ingressSpecGui.getAnnotations()) && MapUtils.getString(ingressSpecGui.getAnnotations(), KubeConstants.META_ANNOTATIONS_INGRESSCLASS, null) != null);
        if (StringUtils.isNotBlank(ingressSpecGui.getIngressControllerName()) || isExistClassAnno) {
            if (StringUtils.isNotBlank(ingressSpecGui.getIngressControllerName()) && isExistClassAnno) {
                if (isAdd) {
                    // GUI - IngressControllerName 필드와 annotation class 설정도 함께 하였을 경우 class 명이 다르다면 오류
                    if (!StringUtils.equals(ingressSpecGui.getIngressControllerName(), MapUtils.getString(ingressSpecGui.getAnnotations(), KubeConstants.META_ANNOTATIONS_INGRESSCLASS))) {
                        throw new CocktailException("You can only edit with the ingress controller field."
                                , ExceptionType.InvalidParameter
                                , String.format("You can only edit with the ingress controller (%s) field. (annotaion : %s)"
                                , ingressSpecGui.getIngressControllerName()
                                , MapUtils.getString(ingressSpecGui.getAnnotations(), KubeConstants.META_ANNOTATIONS_INGRESSCLASS)));
                    }
                } else {
                    String oldIngressControllerName = MapUtils.getString(k8sIngress.getDetail().getAnnotations(), KubeConstants.META_ANNOTATIONS_INGRESSCLASS, null);

                    if (StringUtils.isNotBlank(ingressSpecGui.getIngressControllerName()) && isExistClassAnno) {
                        if (!StringUtils.equals(ingressSpecGui.getIngressControllerName(), MapUtils.getString(ingressSpecGui.getAnnotations(), KubeConstants.META_ANNOTATIONS_INGRESSCLASS))) {
                            if (StringUtils.isNotBlank(oldIngressControllerName)) {
                                // GUI - IngressControllerName 필드와 annotation class 설정도 함께 하였을 경우 annotation class 명이 기존 class명과 다르다면 오류
                                // GUI는 IngressControllerName 필드를 사용하도록 유도
                                if (!StringUtils.equals(MapUtils.getString(ingressSpecGui.getAnnotations(), KubeConstants.META_ANNOTATIONS_INGRESSCLASS), oldIngressControllerName)) {
                                    throw new CocktailException("You can only edit with the ingress controller field."
                                            , ExceptionType.InvalidParameter
                                            , String.format("You can only edit with the ingress controller (%s) field. (annotaion : %s)"
                                            , ingressSpecGui.getIngressControllerName()
                                            , MapUtils.getString(ingressSpecGui.getAnnotations(), KubeConstants.META_ANNOTATIONS_INGRESSCLASS)));
                                }
                            }
                        }
                    }
                }
            }

            // GUI - annotation class 설정만 하였을 경우 IngressControllerName에 셋팅하여 줌.
            if (StringUtils.isBlank(ingressSpecGui.getIngressControllerName())) {
                ingressSpecGui.setIngressControllerName(MapUtils.getString(ingressSpecGui.getAnnotations(), KubeConstants.META_ANNOTATIONS_INGRESSCLASS));
            }
        } else {
            ingressSpecGui.setIngressControllerName(KubeConstants.META_ANNOTATIONS_INGRESSCLASS_NGINX_DEFAULT_VALUE);
        }
        // nginx ingress controller class 명이 존재하는 지 체크
        if (StringUtils.isNotBlank(ingressSpecGui.getIngressControllerName())) {
            // nginx ingress controller 애드온 configMap 조회
//            List<ConfigMapGuiVO> ingressAddonConfigMaps = addonCommonService.getAddonConfigMaps(cluster, String.format("%s=%s,%s=%s"
//                    , KubeConstants.LABELS_ADDON_CHART_KEY, KubeConstants.LABELS_HELM_CHART_VALUE_INGRESS
//                    , KubeConstants.LABELS_HELM_CHART_KEY, ingressSpecGui.getIngressControllerName()));
//
//            if (CollectionUtils.isEmpty(ingressAddonConfigMaps)
//                    || (
//                    CollectionUtils.isNotEmpty(ingressAddonConfigMaps)
//                            && ingressAddonConfigMaps
//                            .stream()
//                            .filter(a -> (StringUtils.equalsIgnoreCase(Optional.ofNullable(a.getData()).orElseGet(() ->Maps.newHashMap()).get(AddonKeyItem.USE_YN.getValue()), "Y"))).count() < 1
//            )
//            ) {
//                throw new CocktailException("The ingress controller does not exist. Please check again."
//                        , ExceptionType.InvalidParameter
//                        , String.format("The ingress controller (%s) does not exist.", ingressSpecGui.getIngressControllerName()));
//            }

            List<String> ingressClassNames = Optional.ofNullable(this.getIngressClassNames(cluster, false, true)).orElseGet(() ->Lists.newArrayList());

            if (!ingressClassNames.contains(ingressSpecGui.getIngressControllerName())) {
                throw new CocktailException("The ingress controller does not exist. Please check again."
                        , ExceptionType.InvalidParameter
                        , String.format("The ingress controller (%s) does not exist.", ingressSpecGui.getIngressControllerName()));
            }
        }

        /**
         * valid Ingress duplicate Path
         */
        String field = null;
        if (!isAdd) {
            field = String.format("%s.%s!=%s", KubeConstants.META, KubeConstants.NAME, ingressSpecGui.getName());
        }
        Map<String, Map<String, Set<String>>> ingresses = this.getUsingIngressOfCluster(cluster, field, null); // 해당 cluster의 k8s - ingress 조회
        List<IngressSpecGuiVO> ingressSpecs = new ArrayList<>();
        ingressSpecs.add(ingressSpecGui);
        this.validIngressPath(ingressSpecs, ingresses, cluster);

        /**
         * ssl redirect
         */
        boolean isExistSSLRedirectAnno = (MapUtils.isNotEmpty(ingressSpecGui.getAnnotations()) && MapUtils.getString(ingressSpecGui.getAnnotations(), KubeConstants.META_ANNOTATIONS_INGRESSSSLREDIRECT, null) != null);
        if (isExistSSLRedirectAnno) {
            // check annotation value ( true, false ) 가 아니면 제거
            try {
                BooleanUtils.toBoolean(MapUtils.getString(ingressSpecGui.getAnnotations(), KubeConstants.META_ANNOTATIONS_INGRESSSSLREDIRECT), "true", "false");
            } catch (IllegalArgumentException e) {
                log.warn(String.format("Ingress SSL Redirect annotation value is invalid. (%s)", MapUtils.getString(ingressSpecGui.getAnnotations(), KubeConstants.META_ANNOTATIONS_INGRESSSSLREDIRECT, null)), e);
                ingressSpecGui.getAnnotations().remove(KubeConstants.META_ANNOTATIONS_INGRESSSSLREDIRECT);
                isExistSSLRedirectAnno = false;
            }
        }
        if (ingressSpecGui.getUseSslRedirect() != null || isExistSSLRedirectAnno) {
            if (isAdd) {
                // GUI - UseSslRedirect 필드와 annotation SSL Redirect 설정도 함께 하였을 경우 값이 다르다면 오류
                if (ingressSpecGui.getUseSslRedirect() != null && isExistSSLRedirectAnno) {
                    if (BooleanUtils.toBoolean(ingressSpecGui.getUseSslRedirect()) != BooleanUtils.toBoolean(MapUtils.getString(ingressSpecGui.getAnnotations(), KubeConstants.META_ANNOTATIONS_INGRESSSSLREDIRECT))) {
                        throw new CocktailException("You can only edit with the SSL Redirect field."
                                , ExceptionType.InvalidParameter
                                , String.format("You can only edit with the SSL Redirect (%s) field. (annotaion : %s)"
                                , ingressSpecGui.getUseSslRedirect().toString()
                                , MapUtils.getString(ingressSpecGui.getAnnotations(), KubeConstants.META_ANNOTATIONS_INGRESSSSLREDIRECT)));
                    }
                }
                // GUI - annotation class 설정만 하였을 경우 UseSslRedirect에 셋팅하여 줌.
                else if (isExistSSLRedirectAnno) {
                    ingressSpecGui.setUseSslRedirect(BooleanUtils.toBoolean(MapUtils.getString(ingressSpecGui.getAnnotations(), KubeConstants.META_ANNOTATIONS_INGRESSSSLREDIRECT), "true", "false"));
                }
            } else {
                String oldSSLRedirect = MapUtils.getString(k8sIngress.getDetail().getAnnotations(), KubeConstants.META_ANNOTATIONS_INGRESSSSLREDIRECT, null);

                if (ingressSpecGui.getUseSslRedirect() != null && isExistSSLRedirectAnno) {
                    if (BooleanUtils.toBoolean(ingressSpecGui.getUseSslRedirect()) != BooleanUtils.toBoolean(MapUtils.getString(ingressSpecGui.getAnnotations(), KubeConstants.META_ANNOTATIONS_INGRESSSSLREDIRECT))) {
                        if (StringUtils.isNotBlank(oldSSLRedirect)) {
                            // GUI - UseSslRedirect 필드와 annotation SSL Redirect 설정도 함께 하였을 경우 annotation SSL Redirect 값이 기존 SSL Redirect값과 다르다면 오류
                            // GUI는 UseSslRedirect 필드를 사용하도록 유도
                            if (BooleanUtils.toBoolean(MapUtils.getString(ingressSpecGui.getAnnotations(), KubeConstants.META_ANNOTATIONS_INGRESSSSLREDIRECT)) != BooleanUtils.toBoolean(oldSSLRedirect)) {
                                throw new CocktailException("You can only edit with the SSL Redirect field."
                                        , ExceptionType.InvalidParameter
                                        , String.format("You can only edit with the SSL Redirect (%s) field. (annotaion : %s)"
                                        , ingressSpecGui.getUseSslRedirect().toString()
                                        , MapUtils.getString(ingressSpecGui.getAnnotations(), KubeConstants.META_ANNOTATIONS_INGRESSSSLREDIRECT)));
                            }
                        }
                    }
                }
            }
        }
    }

    public boolean validIngressServicePort(List<IngressRuleVO> ingressRules, boolean isThrow) throws Exception {
        if (CollectionUtils.isNotEmpty(ingressRules)) {
            for (IngressRuleVO ingressRule : ingressRules) {

                for (IngressHttpPathVO ingressHttpPath : ingressRule.getIngressHttpPaths()) {
                    if (StringUtils.isNotBlank(ingressHttpPath.getServiceName()) && StringUtils.isNotBlank(ingressHttpPath.getServicePort())) {

                        Pair<Boolean, ExceptionType> validResult = ResourceUtil.isValidPortRule(ingressHttpPath.getServicePort(), isThrow);

                        if (validResult != null && !isThrow) {
                            if (!BooleanUtils.toBoolean(validResult.getKey())) {
                                return false;
                            }
                        }
                    }
                }
            }
        }

        return true;
    }

    /**
     * 인그레스 유효성 체크
     *
     * @param ingressSpecs
     * @param ingresses
     * @param cluster
     * @throws Exception
     */
    public void validIngressPath(List<IngressSpecGuiVO> ingressSpecs, Map<String, Map<String, Set<String>>> ingresses, ClusterVO cluster) throws Exception {

        String duplicatedHost = null;
        String duplicatedPath = null;
        if (CollectionUtils.isNotEmpty(ingressSpecs)) {
            MAIN_INGRESS_LOOP:
            for (IngressSpecGuiVO ingressSpecRow : ingressSpecs) {

                // 현재 적용할 ingress의 host (중복 체크)
                Set<String> selfDuplicateHost = new HashSet<>();
                if (CollectionUtils.isNotEmpty(ingressSpecRow.getIngressRules())) {
                    for (IngressRuleVO ingressRuleRow : ingressSpecRow.getIngressRules()) {
                        if (StringUtils.isNotBlank(ingressRuleRow.getHostName())) {
                            if (StringUtils.length(ingressRuleRow.getHostName()) > 200) {
                                throw new CocktailException(String.format("Ingress Host too long!! [%s]", ingressRuleRow.getHostName()), ExceptionType.IngressHostTooLong);
                            } else {
                                if (selfDuplicateHost.contains(ingressRuleRow.getHostName())) {
                                    duplicatedHost = ingressRuleRow.getHostName();
                                    break MAIN_INGRESS_LOOP;
                                }
                                selfDuplicateHost.add(ingressRuleRow.getHostName());
                            }
                        }

                        // 현재 적용할 ingress의 path (중복 체크)
                        Set<String> selfDuplicatePath = new HashSet<>();
                        String hostName = null;
                        if (CollectionUtils.isNotEmpty(ingressRuleRow.getIngressHttpPaths())) {
                            for (IngressHttpPathVO ingressHttpPathRow : ingressRuleRow.getIngressHttpPaths()) {
                                if (StringUtils.length(ingressHttpPathRow.getPath()) > 200) {
                                    throw new CocktailException(String.format("Ingress Path too long!! [%s]", ingressHttpPathRow.getPath()), ExceptionType.IngressPathTooLong);
                                }

                                if (StringUtils.isNotBlank(ingressRuleRow.getHostName())) {
                                    hostName = ingressRuleRow.getHostName();
                                } else {
                                    hostName = KubeConstants.CUSTOM_INGRESS_HTTP_NON_HOST;
                                }

                                if (MapUtils.isNotEmpty(ingresses)
                                        && MapUtils.getObject(ingresses, ingressSpecRow.getIngressControllerName(), null) != null
                                        && MapUtils.getObject(ingresses.get(ingressSpecRow.getIngressControllerName()), hostName) != null) {
                                    if (ingresses.get(ingressSpecRow.getIngressControllerName()).get(hostName).contains(ingressHttpPathRow.getPath())) {
                                        duplicatedPath = ingressHttpPathRow.getPath();
                                        break MAIN_INGRESS_LOOP;
                                    }
                                }

                                if (CollectionUtils.isNotEmpty(selfDuplicatePath) && selfDuplicatePath.contains(ingressHttpPathRow.getPath())) {
                                    duplicatedPath = ingressHttpPathRow.getPath();
                                    break MAIN_INGRESS_LOOP;
                                }

                                selfDuplicatePath.add(ingressHttpPathRow.getPath());
                            }
                        }
                    }
                }
            }

            if (duplicatedHost != null) {
                String msg = String.format("The '%s' Ingress Host is already in use.", duplicatedHost);
                throw new CocktailException(msg, ExceptionType.IngressHostUsed, msg);
            }
            if (duplicatedPath != null) {
                String msg = String.format("The '%s' Ingress Path is already in use.", duplicatedPath);
                throw new CocktailException(msg, ExceptionType.IngressPathUsed, msg);
            }
        }

    }

    /**
     * Nginx ingress controller - ingressClass명 조회
     *
     * @param clusterSeq
     * @param withDeployIngressController - 배포된 nginx-ingress-controller의 정보를 조회하여 ingressClass 정보 사용유무
     * @param allNamespace
     * @return
     * @throws Exception
     */
    public List<String> getIngressClassNames(Integer clusterSeq, boolean withDeployIngressController, boolean allNamespace) throws Exception {
        IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
        ClusterVO cluster = clusterDao.getCluster(clusterSeq);

        return this.getIngressClassNames(cluster, withDeployIngressController, allNamespace);
    }

    /**
     * Nginx ingress controller - ingressClass명 조회
     *
     * @param cluster
     * @param withDeployIngressController - 배포된 nginx-ingress-controller의 정보를 조회하여 ingressClass 정보 사용유무
     * @param allNamespace
     * @return
     * @throws Exception
     */
    public List<String> getIngressClassNames(ClusterVO cluster, boolean withDeployIngressController, boolean allNamespace) throws Exception {

        List<String> ingressClassNames = Lists.newArrayList();

        // get addon ConfigMap - chartType : nginx-ingress
        List<ConfigMapGuiVO> addons = addonCommonService.getAddonConfigMaps(cluster, ResourceUtil.commonAddonSearchLabel("nginx-ingress"), "Y", false, false);
        Set<String> ingressClassNameSet = Optional.ofNullable(addons).orElseGet(() ->Lists.newArrayList()).stream().map(ConfigMapGuiVO::getName).collect(Collectors.toSet());

        // configMap이 없을 시 nginx-ingress-controller deployment를 조회하여 ingressClass 명을 추출
        if (CollectionUtils.isEmpty(ingressClassNames) || withDeployIngressController) {
            String addonNamespace = allNamespace ? null : addonCommonService.getCocktailAddonNamespace();
            List<K8sDeploymentVO> deployments = workloadResourceService.getDeployments(cluster, addonNamespace, null, null, ContextHolder.exeContext());

            String ingressClassName = null;
            DEPLOYMENT_LOOP:
            for (K8sDeploymentVO d : Optional.ofNullable(deployments).orElseGet(() ->Lists.newArrayList())) {
                ingressClassName = null;
                for (K8sContainerVO c : Optional.ofNullable(d.getDetail().getPodTemplate().getSpec().getDetail().getContainers()).orElseGet(() ->Lists.newArrayList())) {

                    // nginx-ingress-controller 이미지로 생성된 container 검색
                    // 2021.05.14 chj arg의 ingress-class 옵션으로만 체크하여 클래스명 추출하도록 수정
//                    String[] imageArr = StringUtils.split(c.getImage(), ":");
//                    if (StringUtils.endsWith(imageArr[0], "/nginx-ingress-controller")) {
                    for (String arg : Optional.ofNullable(c.getArgs()).orElseGet(() ->Lists.newArrayList())) {
                        // args 에서 --ingress-class=nginx 구문을 찾아 셋팅
                        if (StringUtils.contains(arg, "--ingress-class=")) {
                            String[] argArr = StringUtils.split(arg, "=");
                            if (argArr.length == 2) {
                                ingressClassName = StringUtils.defaultIfBlank(argArr[1], KubeConstants.META_ANNOTATIONS_INGRESSCLASS_NGINX_DEFAULT_VALUE);
                            }
                            break;
                        }
                    }

                    // args 에서 --ingress-class=nginx 구문이 없다면 기본 ingressClass는 nginx이므로 셋팅해줌
                    if (StringUtils.isBlank(ingressClassName)) {
                        ingressClassName = KubeConstants.META_ANNOTATIONS_INGRESSCLASS_NGINX_DEFAULT_VALUE;
                    }

                    ingressClassNameSet.add(ingressClassName);
                    continue DEPLOYMENT_LOOP;
//                    }
                }
            }
        }

        /*
        String kind = String.format("%s/%s","networking.k8s.io","v1");
        boolean isCheck = k8sWorker.isServerSupportsVersion(cluster,kind);
        */

        // Get IngressClass Resource Name 조회하여 , 이전 Annotations 설정 방식에서 조회 한 Ingress Class 이름과 Merge 하여 중복을 제거한다.
        List<V1IngressClass> v1IngressClasss = k8sWorker.getIngressClassNetworkingV1(cluster, null, null, null);
        if (v1IngressClasss.size() > 0) {
            for (V1IngressClass v1IngressClass : v1IngressClasss) {
                ingressClassNameSet.add(v1IngressClass.getMetadata().getName());
            }
        }

        ingressClassNames.addAll(ingressClassNameSet);

        return ingressClassNames;
    }
}
