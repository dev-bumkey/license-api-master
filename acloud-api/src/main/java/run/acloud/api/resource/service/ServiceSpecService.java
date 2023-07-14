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
import org.apache.commons.lang3.StringUtils;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import run.acloud.api.configuration.dao.IClusterMapper;
import run.acloud.api.configuration.vo.ClusterVO;
import run.acloud.api.cserver.enums.DeployType;
import run.acloud.api.cserver.util.ServerUtils;
import run.acloud.api.k8sextended.util.Yaml;
import run.acloud.api.resource.constants.KubeConstants;
import run.acloud.api.resource.enums.K8sApiKindType;
import run.acloud.api.resource.enums.K8sApiType;
import run.acloud.api.resource.task.K8sPatchSpecFactory;
import run.acloud.api.resource.task.K8sSpecFactory;
import run.acloud.api.resource.task.K8sWorker;
import run.acloud.api.resource.util.K8sMapperUtils;
import run.acloud.api.resource.util.ResourceUtil;
import run.acloud.api.resource.vo.*;
import run.acloud.commons.util.JsonUtils;
import run.acloud.commons.vo.ExecutingContextVO;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;

import java.util.*;

@Slf4j
@Service
public class ServiceSpecService {
    @Resource(name = "cocktailSession")
    private SqlSessionTemplate sqlSession;

    @Autowired
    private K8sWorker k8sWorker;

    @Autowired
    private IngressSpecService ingressSpecService;

    @Autowired
    private WorkloadResourceService workloadResourceService;

    @Autowired
    private K8sPatchSpecFactory k8sPatchSpecFactory;

    /**
     * K8s Service 생성 (Invoke From Snapshot Deployment)
     *
     * @param cluster
     * @param services
     * @param context
     * @throws Exception
     */
    public void createMultipleService(ClusterVO cluster, List<ServiceSpecIntegrateVO> services, ExecutingContextVO context) throws Exception {
        if(CollectionUtils.isNotEmpty(services)){
            for(ServiceSpecIntegrateVO service : services){
                if(DeployType.valueOf(service.getDeployType()) == DeployType.GUI) {
                    ServiceSpecGuiVO serviceSpecGui = null;
                    try {
                        serviceSpecGui = (ServiceSpecGuiVO) service;
                        this.createService(cluster, cluster.getNamespaceName(), serviceSpecGui, context);
                        Thread.sleep(100);
                    }
                    catch (Exception ex) {
                        // 실패시 Log를 남기고 다음 처리를 계속한다..
                        log.error(String.format("Service Deployment Failure : createMultipleService : %s\n%s", ex.getMessage(), JsonUtils.toGson(serviceSpecGui)));
                    }
                }
                else if(DeployType.valueOf(service.getDeployType()) == DeployType.YAML) {
                    ServiceSpecYamlVO serviceSpecYaml = null;
                    try {
                        serviceSpecYaml = (ServiceSpecYamlVO) service;
                        V1Service v1Service = ServerUtils.unmarshalYaml(serviceSpecYaml.getYaml(), K8sApiKindType.SERVICE);
                        k8sWorker.createServiceV1(cluster, cluster.getNamespaceName(), v1Service, false);
                        Thread.sleep(100);
                    }
                    catch (Exception ex) {
                        // 실패시 Log를 남기고 다음 처리를 계속한다..
                        log.error(String.format("Service Deployment Failure : createMultipleService : %s\n%s", ex.getMessage(), JsonUtils.toGson(serviceSpecYaml)));
                    }
                }
                else {
                    log.error(String.format("Invalid DeployType : createMultipleService : %s", JsonUtils.toGson(service)));
                }
            }
        }
    }

    public V1Service createServiceV1(ClusterVO cluster, String namespace, V1Service service, ExecutingContextVO context) throws Exception {
        if(cluster == null) {
            throw new CocktailException("cluster is null.", ExceptionType.InvalidParameter);
        }

        V1Service v1Service = k8sWorker.getServiceV1(cluster, namespace, service.getMetadata().getName());
        if(v1Service != null){
            throw new CocktailException("Service already exists!!", ExceptionType.ServiceNameAlreadyExists);
        }

        return k8sWorker.createServiceV1(cluster, namespace, service, false);
    }
    /**
     * K8s Service 생성
     * @param cluster
     * @param namespaceName
     * @param serviceSpec
     * @param context
     * @return
     * @throws Exception
     */
    public K8sServiceVO createService(ClusterVO cluster, String namespaceName, ServiceSpecGuiVO serviceSpec, ExecutingContextVO context) throws Exception {
        return createService(cluster, namespaceName, serviceSpec, null, false, context);
    }

    /**
     * K8S Service 생성 ( 변경 spec )
     *
     * @param cluster
     * @param namespaceName
     * @param serviceSpec
     * @param appName
     * @param context
     * @return
     * @throws Exception
     */
    public K8sServiceVO createService(ClusterVO cluster, String namespaceName, ServiceSpecGuiVO serviceSpec, String appName, boolean isWorkload, ExecutingContextVO context) throws Exception {

        // 서비스 존재시 기존 로직 삭제, 워크로드에서 서비스 생성시 async 처리 때문에 들어갔다고함.
        V1Service v1Service = k8sWorker.getServiceV1(cluster, namespaceName, serviceSpec.getName());
        if (v1Service != null) {
            // 서비스 삭제
            k8sWorker.deleteServiceV1WithName(cluster, namespaceName, v1Service.getMetadata().getName());
            Thread.sleep(500);
        }

        // 서비스 생성
        v1Service = K8sSpecFactory.buildServiceV1(serviceSpec, namespaceName, appName, isWorkload, cluster);
        v1Service = k8sWorker.createServiceV1(cluster, cluster.getNamespaceName(), v1Service, false);
        Thread.sleep(100); // 느린곳에서는 아래 조회로직이 제대로 조회 못할 수 있어서 sleep 했다고함.

        return this.getService(cluster, cluster.getNamespaceName(), v1Service.getMetadata().getName(), context);
    }

    /**
     * K8s Service Patch GUI
     * @param cluster
     * @param namespaceName
     * @param serviceSpec
     * @param context
     * @return
     * @throws Exception
     */
    public K8sServiceVO patchService(ClusterVO cluster, String namespaceName, ServiceSpecGuiVO serviceSpec, ExecutingContextVO context) throws Exception {
        return patchService(cluster, namespaceName, serviceSpec, null, false, context, null);
    }

    /**
     * K8s Service Patch GUI
     * @param cluster
     * @param namespaceName
     * @param serviceSpec
     * @param appName
     * @param context
     * @return
     * @throws Exception
     */
    public K8sServiceVO patchService(ClusterVO cluster, String namespaceName, ServiceSpecGuiVO serviceSpec, String appName, boolean isWorkload, ExecutingContextVO context) throws Exception {
        return patchService(cluster, namespaceName, serviceSpec, appName, isWorkload, context, null);
    }

    /**
     * K8S Service Patch GUI ( 변경 spec )
     *
     * @param cluster cluster
     * @param namespaceName namespaceName
     * @param serviceSpec serviceSpec
     * @param appName appName
     * @param isWorkload isWorkload
     * @param context context
     * @param paramService paramService
     * @return
     * @throws Exception
     */
    public K8sServiceVO patchService(ClusterVO cluster, String namespaceName, ServiceSpecGuiVO serviceSpec, String appName, boolean isWorkload, ExecutingContextVO context, K8sServiceVO paramService) throws Exception {

        K8sServiceVO k8sService = null;

        // set port name
        for(ServicePortVO p : serviceSpec.getServicePorts()){
            if(StringUtils.isBlank(p.getName())){
                p.setName(ResourceUtil.makePortName());
            }
        }

        V1Service currentService = k8sWorker.getServiceV1(cluster, namespaceName, serviceSpec.getName());
        currentService.setStatus(null);

        /** patchData를 만들기 전 Reserved Annotations and Labels 가 삭제되었을 경우 다시 넣어주어 제거할 수 없도록 함.. */
        k8sPatchSpecFactory.saveReservedLabelsAndAnnotations(currentService.getMetadata().getLabels(), serviceSpec.getLabels());

        V1Service updatedService = K8sSpecFactory.buildServiceV1(serviceSpec, namespaceName, appName, isWorkload, cluster);
        updatedService.getSpec().setClusterIP(currentService.getSpec().getClusterIP());
        updatedService.getSpec().setExternalTrafficPolicy(currentService.getSpec().getExternalTrafficPolicy());

        List<JsonObject> patchBody = k8sPatchSpecFactory.buildPatch(currentService, updatedService);

        updatedService = k8sWorker.patchServiceV1(cluster, namespaceName, updatedService.getMetadata().getName(), patchBody, false);
        Thread.sleep(100);

        return this.getService(cluster, namespaceName, updatedService.getMetadata().getName(), context);
    }

    public void patchServiceV1(ClusterVO cluster, String namespaceName, V1Service updatedService, ExecutingContextVO context) throws Exception {

        V1Service currentService = k8sWorker.getServiceV1(cluster, namespaceName, updatedService.getMetadata().getName());
        currentService.setStatus(null);

        List<JsonObject> patchBody = k8sPatchSpecFactory.buildPatch(currentService, updatedService);

        k8sWorker.patchServiceV1(cluster, cluster.getNamespaceName(), updatedService.getMetadata().getName(), patchBody, false);
        Thread.sleep(100);
    }

    public void patchServiceV1(ClusterVO cluster, String namespaceName, V1Service currentService, V1Service updatedService, boolean dryRun, ExecutingContextVO context) throws Exception {

        currentService.setStatus(null);
        updatedService.setStatus(null);

        updatedService.getSpec().setClusterIP(currentService.getSpec().getClusterIP());
        updatedService.getSpec().setExternalTrafficPolicy(currentService.getSpec().getExternalTrafficPolicy());

        List<JsonObject> patchBody = k8sPatchSpecFactory.buildPatch(currentService, updatedService);

        k8sWorker.patchServiceV1(cluster, namespaceName, updatedService.getMetadata().getName(), patchBody, dryRun);
    }


    /**
     * K8s Service Patch Yaml
     * @param cluster
     * @param namespaceName
     * @param serviceSpecYaml
     * @param context
     * @return
     * @throws Exception
     */
    public K8sServiceVO patchServiceWithYaml(ClusterVO cluster, String namespaceName, ServiceSpecGuiVO serviceSpec, ServiceSpecYamlVO serviceSpecYaml, ExecutingContextVO context) throws Exception {
        K8sServiceVO k8sService = null;

        // set port name
        if(serviceSpec.getServicePorts() != null && serviceSpec.getServicePorts().size() > 1) {
            for (ServicePortVO p : serviceSpec.getServicePorts()) {
                if (StringUtils.isBlank(p.getName())) {
                    throw new CocktailException("Service port name required on multiple ports", ExceptionType.InvalidInputData);
                }
            }
        }

        V1Service currentService = k8sWorker.getServiceV1(cluster, namespaceName, serviceSpec.getName());
        currentService.setStatus(null);

        Map<String, Object> serviceYamlMap = ServerUtils.getK8sYamlToMap(serviceSpecYaml.getYaml());

        K8sApiKindType apiKindType = ServerUtils.getK8sKindInMap(serviceYamlMap);
        K8sApiType apiVerType = ServerUtils.getK8sVersionInMap(serviceYamlMap);

        V1Service updatedService = null;
        if (apiKindType == K8sApiKindType.SERVICE) {
            if (apiVerType == K8sApiType.V1) {
                updatedService = Yaml.loadAs(serviceSpecYaml.getYaml(), V1Service.class);
                updatedService.getSpec().setClusterIP(currentService.getSpec().getClusterIP());
                updatedService.getSpec().setExternalTrafficPolicy(currentService.getSpec().getExternalTrafficPolicy());

                /** patchData를 만들기 전 Reserved Annotations and Labels 가 삭제되었을 경우 다시 넣어주어 제거할 수 없도록 함.. */
                k8sPatchSpecFactory.saveReservedLabelsAndAnnotations(currentService.getMetadata().getLabels(), updatedService.getMetadata().getLabels());

                List<JsonObject> patchBody = k8sPatchSpecFactory.buildPatch(currentService, updatedService);
                updatedService = k8sWorker.patchServiceV1(cluster, namespaceName, updatedService.getMetadata().getName(), patchBody, false);
            }
        }

        Thread.sleep(100);

        if(updatedService != null) {
            k8sService = this.getService(cluster, namespaceName, updatedService.getMetadata().getName(), context);
        }

        return k8sService;
    }

    /**
     * K8S Service 삭제
     *
     * @param cluster
     * @param label
     * @param context
     * @return
     * @throws Exception
     */
    public void deleteService(ClusterVO cluster, String name, String label, ExecutingContextVO context) throws Exception {

        // 서비스 삭제
        if(StringUtils.isNotBlank(name)){
            V1Service currentService = k8sWorker.getServiceV1(cluster, cluster.getNamespaceName(), name);
            if (currentService != null) {
                k8sWorker.deleteServiceV1WithName(cluster, cluster.getNamespaceName(), name);
            }
        }else if(StringUtils.isNotBlank(label)){
            List<V1Service> currentServices = k8sWorker.getServicesV1(cluster, cluster.getNamespaceName(), null, label);
            if (CollectionUtils.isNotEmpty(currentServices)) {
                k8sWorker.deleteServiceV1WithLabel(cluster, cluster.getNamespaceName(), label);
            }
        }

        Thread.sleep(500);

    }

    /**
     * K8S Service 삭제
     *
     * @param cluster
     * @param label
     * @param context
     * @return
     * @throws Exception
     */
    public boolean deleteServiceAndConfirm(ClusterVO cluster, String name, String label, ExecutingContextVO context) throws Exception {

        boolean bRet = false;
        List<V1Service> services = k8sWorker.getServicesV1(cluster, cluster.getNamespaceName(), null, label);
        if(CollectionUtils.isNotEmpty(services)) {
            if (StringUtils.isNotBlank(name)) {
                k8sWorker.deleteServiceV1WithName(cluster, cluster.getNamespaceName(), name);
            }
            else if (StringUtils.isNotBlank(label)) {
                k8sWorker.deleteServiceV1WithLabel(cluster, cluster.getNamespaceName(), label);
            }
            else {
                log.error("fail deleteService!! ( Insufficient parameters: lable or name is required. ) cluster:[{}], name: [{}]", JsonUtils.toGson(cluster), name);
                return false;
            }
            Thread.sleep(500);

            for (int i = 1, ie = 60; i <= ie; i++) {
                services = k8sWorker.getServicesV1(cluster, cluster.getNamespaceName(), null, label);
                if (CollectionUtils.isNotEmpty(services)) {
                    Thread.sleep(1000 * i);
                }
                else {
                    bRet = true;
                    break;
                }
            }
        }

        if(!bRet) {
            log.error("fail deleteService!! ( Could not Found Services ) cluster:[{}], name: [{}]", JsonUtils.toGson(cluster), name);
        }

        return bRet;
    }


    /**
     * K8S Service 정보 조회
     * @param clusterSeq
     * @param namespace
     * @param name
     * @param context
     * @return
     * @throws Exception
     */
    public List<K8sServiceVO> getServicesInDeployment(Integer clusterSeq, String namespace, String name, ExecutingContextVO context) throws Exception{
        try {
            if (clusterSeq == null) {
                throw new CocktailException("cluster not found.", ExceptionType.InvalidParameter);
            }

            IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
            ClusterVO cluster = clusterDao.getCluster(clusterSeq);

            K8sDeploymentVO deployment;
            if (cluster != null) {
                deployment = workloadResourceService.getDeployment(cluster, namespace, name, context);
            }
            else {
                throw new CocktailException("cluster not found.", ExceptionType.InvalidParameter);
            }

            // Namespace 안의 Service 목록 조회.
            List<K8sServiceVO> services = this.convertServiceDataList(cluster, namespace, null, null, context);
            List<K8sServiceVO> matchedServices = new ArrayList<>();

            // Deployment의 matchLabels와 Service의 Selector가 같은 경우 해당 Deployment의 서비스로 판단...
            // podTemplate의 label로 비교로직 변경, 2019/12/13, coolingi
            for(K8sServiceVO serviceRow : services) {

                if(serviceRow.getDetail() != null
                        && serviceRow.getDetail().getLabelSelector() != null
                        && deployment != null
                        && deployment.getDetail() != null
                        && deployment.getDetail().getPodTemplate() != null
                ) {
                    boolean isMatched = true;
                    Map<String, String> podLabels = deployment.getDetail().getPodTemplate().getLabels();
                    Map<String, String> seletorLabels = serviceRow.getDetail().getLabelSelector();

                    if(MapUtils.isNotEmpty(podLabels) && MapUtils.isNotEmpty(seletorLabels)){
                        isMatched = ServerUtils.containMaps(podLabels, seletorLabels);
                    }

                    if (isMatched) {
                        matchedServices.add(serviceRow);
                    }

                }
            }

            // Namespace 안의 Ingress 목록 조회
            List<K8sIngressVO> ingresses = ingressSpecService.convertIngressDataList(cluster, namespace, null, null, context);
            // Service에 매칭되는 Ingress를 찾아서 Setting
            for(K8sServiceVO matchedService : matchedServices) {
                for (K8sIngressVO ingressRow : ingresses) {
                    for (IngressRuleVO ingressRuleRow : ingressRow.getIngressSpec().getIngressRules()) {
                        for (IngressHttpPathVO ingressHttpPathRow : ingressRuleRow.getIngressHttpPaths()) {
                            if (ingressHttpPathRow.getServiceName().equals(matchedService.getServiceName())) {
                                matchedService.setMatchedIngress(ingressRow);
                                break;
                            }
                        }
                        if (matchedService.getMatchedIngress() != null) break;
                    }
                    if (matchedService.getMatchedIngress() != null) break;
                }
            }

            return matchedServices;
        } catch (CocktailException e) {
            throw new CocktailException("getServices fail!!", e, ExceptionType.K8sCocktailCloudInquireFail);
        }
    }

    /**
     * K8S Service 정보 조회
     * (cluster > namespace > pod)
     *
     * @param clusterSeq
     * @param namespaceName
     * @param context
     * @return
     * @throws Exception
     */
    public List<K8sServiceVO> getServices(Integer clusterSeq, String namespaceName, ExecutingContextVO context) throws Exception{

        try {
            if(StringUtils.isNotBlank(namespaceName)){
                IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
                ClusterVO cluster = clusterDao.getCluster(clusterSeq);

                if(cluster != null){
                    return this.convertServiceDataList(cluster, namespaceName, null, null, context);
                }else{
                    throw new CocktailException("cluster is null.", ExceptionType.InvalidParameter);
                }
            }else{
                throw new CocktailException(String.format("%s is null.", (StringUtils.isNotBlank(namespaceName) ? "namespaceName" : "Unknown")), ExceptionType.InvalidParameter);
            }
        } catch (Exception e) {
            throw new CocktailException("getServices fail!!", e, ExceptionType.K8sCocktailCloudInquireFail);
        }
    }

    /**
     * K8S Service 정보 조회
     * (cluster > namespace > pod)
     *
     * @param clusterSeq
     * @param namespace
     * @param field
     * @param label
     * @param context
     * @return
     * @throws Exception
     */
    public List<K8sServiceVO> getServices(Integer clusterSeq, String namespace, String field, String label, ExecutingContextVO context) throws Exception{

        IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
        ClusterVO cluster = clusterDao.getCluster(clusterSeq);

        if(cluster != null){
            return this.convertServiceDataList(cluster, namespace, field, label, context);
        }else{
            throw new CocktailException("cluster is null.", ExceptionType.InvalidParameter);
        }
    }

    /**
     * K8S Service 정보 조회
     * (cluster > namespace > pod)
     *
     * @param cluster
     * @param namespace
     * @param field
     * @param label
     * @param context
     * @return
     * @throws Exception
     */
    public List<K8sServiceVO> getServices(ClusterVO cluster, String namespace, String field, String label, ExecutingContextVO context) throws Exception{

        if(cluster != null){
            return this.convertServiceDataList(cluster, namespace, field, label, true, context);
        }else{
            throw new CocktailException("cluster is null.", ExceptionType.InvalidParameter);
        }
    }
    public List<K8sServiceVO> getServices(ClusterVO cluster, String namespace, String field, String label, boolean useEndpoint, ExecutingContextVO context) throws Exception{

        if(cluster != null){
            return this.convertServiceDataList(cluster, namespace, field, label, useEndpoint, context);
        }else{
            throw new CocktailException("cluster is null.", ExceptionType.InvalidParameter);
        }
    }

    public List<V1Service> getServicesV1(ClusterVO cluster, String namespace, String field, String label, ExecutingContextVO context) throws Exception{
        if(cluster == null){
            throw new CocktailException("cluster is null.", ExceptionType.ClusterNotFound);
        }
        List<V1Service> services;
        try {
            services = k8sWorker.getServicesV1(cluster, namespace, field, label);
        } catch (Exception e) {
            throw new CocktailException("getServicesV1 fail!!", e, ExceptionType.K8sCocktailCloudInquireFail);
        }

        return services;
    }

    public V1Service getServiceV1(ClusterVO cluster, String namespace, String name, ExecutingContextVO context) throws Exception {
        if(cluster == null){
            throw new CocktailException("cluster is null.", ExceptionType.ClusterNotFound);
        }
        try {
            return k8sWorker.getServiceV1(cluster, namespace, name);
        } catch (Exception e) {
            throw new CocktailException("getServiceV1 fail!!", e, ExceptionType.K8sCocktailCloudInquireFail);
        }
    }

    /**
     * K8S Service 정보 조회
     *
     * @param cluster
     * @param namespace
     * @param name
     * @param context
     * @return
     * @throws Exception
     */
    public K8sServiceVO getService(ClusterVO cluster, String namespace, String name, ExecutingContextVO context) throws Exception{

        if(cluster != null && StringUtils.isNotBlank(namespace) && StringUtils.isNotBlank(name)){
            String field = String.format("%s.%s=%s", KubeConstants.META, KubeConstants.NAME, name);
            List<K8sServiceVO> k8sServices = this.convertServiceDataList(cluster, namespace, field, null, context);

            if(CollectionUtils.isNotEmpty(k8sServices)){
                return k8sServices.get(0);
            }else{
                return null;
            }
        }else{
            throw new CocktailException("cluster/namespace/name is null.", ExceptionType.InvalidParameter);
        }
    }

    /**
     * K8S Service 정보 조회 후 V1Service, V1Endpoints -> K8sServiceVO 변환
     *
     * @param cluster
     * @param label
     * @return
     * @throws Exception
     */
    public List<K8sServiceVO> convertServiceDataList(ClusterVO cluster, String namespaceName, String field, String label, ExecutingContextVO context) throws Exception{

        return this.convertServiceDataList(cluster, namespaceName, field, label, true, context);
    }

    public List<K8sServiceVO> convertServiceDataList(ClusterVO cluster, String namespaceName, String field, String label, boolean useEndpoint, ExecutingContextVO context) throws Exception{

        List<K8sServiceVO> services = new ArrayList<>();

        try {
            // get service
            List<V1Service> v1Services = k8sWorker.getServicesV1(cluster, namespaceName, field, label);
            // get endpoint
            List<V1Endpoints> v1Endpoints = new ArrayList<>();
            if (useEndpoint){
                v1Endpoints = k8sWorker.getEndpointV1(cluster, namespaceName, field, label);
            }

            this.genServiceDataList(cluster, services, v1Services, v1Endpoints);
        } catch (Exception e) {
            throw new CocktailException("convertServiceDataList fail!!", e, ExceptionType.K8sCocktailCloudInquireFail);
        }

        return services;
    }

    /**
     * V1Service -> K8sServiceVO
     * @param cluster
     * @param service
     * @param v1Service
     * @param v1Endpoints
     * @return
     * @throws Exception
     */
    public K8sServiceVO genServiceData(ClusterVO cluster, K8sServiceVO service, V1Service v1Service, List<V1Endpoints> v1Endpoints) throws Exception{
        return this.genServiceData(cluster, service, v1Service, v1Endpoints, null);
    }

    public K8sServiceVO genServiceData(ClusterVO cluster, K8sServiceVO service, V1Service v1Service, List<V1Endpoints> v1Endpoints, JSON k8sJson) throws Exception {

        Map<String, Map<String, V1Endpoints>> v1EndpointsMap = this.convertEndpointsListToMap(v1Endpoints);

        return this.genServiceData(cluster, service, v1Service, v1EndpointsMap, k8sJson);
    }

    public K8sServiceVO genServiceData(ClusterVO cluster, K8sServiceVO service, V1Service v1Service, Map<String, Map<String, V1Endpoints>> v1EndpointsMap, JSON k8sJson) throws Exception {
        if(v1Service == null) {
            return null;
        }
        if(service == null) {
            service = new K8sServiceVO();
        }
        if (k8sJson == null) {
            k8sJson = new JSON();
        }

        String sessionAffinityConfigVal = "";
        sessionAffinityConfigVal = k8sJson.serialize(v1Service.getSpec().getSessionAffinityConfig());

        service.setNamespace(v1Service.getMetadata().getNamespace());
        service.setServiceName(v1Service.getMetadata().getName());
        service.setLabels(v1Service.getMetadata().getLabels());
        service.setClusterIP(v1Service.getSpec().getClusterIP());
        service.setSessionAffinity(v1Service.getSpec().getSessionAffinity());
        service.setSessionAffinityTimeoutSeconds(StringUtils.equalsIgnoreCase("null", sessionAffinityConfigVal) ? null : v1Service.getSpec().getSessionAffinityConfig().getClientIP().getTimeoutSeconds());
        if(cluster != null) {
            if (StringUtils.equalsIgnoreCase(KubeConstants.SPEC_TYPE_VALUE_LOADBALANCER, v1Service.getSpec().getType())) {
                if (cluster.getProviderAccount().getProviderCode().canInternalLB()) {
                    switch (cluster.getProviderAccount().getProviderCode()) {
                        case AWS:
                            if (v1Service.getMetadata().getAnnotations() != null
                                    && StringUtils.equalsAny(v1Service.getMetadata().getAnnotations().get(KubeConstants.META_ANNOTATIONS_AWS_INTERNAL_LOADBALANCER_KEY), KubeConstants.META_ANNOTATIONS_AWS_INTERNAL_LOADBALANCER_VALUE, KubeConstants.META_ANNOTATIONS_AWS_INTERNAL_LOADBALANCER_VALUE_AFTER_1_15)) {
                                service.setInternalLBFlag(true);
                            }
                            break;
                        case GCP:
                            if (v1Service.getMetadata().getAnnotations() != null
                                    && StringUtils.equals(v1Service.getMetadata().getAnnotations().get(KubeConstants.META_ANNOTATIONS_GCP_INTERNAL_LOADBALANCER_KEY), KubeConstants.META_ANNOTATIONS_GCP_INTERNAL_LOADBALANCER_VALUE)) {
                                service.setInternalLBFlag(true);
                            }
                            break;
                        case AZR:
                            if (v1Service.getMetadata().getAnnotations() != null
                                    && StringUtils.equals(v1Service.getMetadata().getAnnotations().get(KubeConstants.META_ANNOTATIONS_AZR_INTERNAL_LOADBALANCER_KEY), KubeConstants.META_ANNOTATIONS_AZR_INTERNAL_LOADBALANCER_VALUE)) {
                                service.setInternalLBFlag(true);
                            }
                            break;
                        case NCP:
                            if (v1Service.getMetadata().getAnnotations() != null
                                    && StringUtils.equals(v1Service.getMetadata().getAnnotations().get(KubeConstants.META_ANNOTATIONS_NCP_INTERNAL_LOADBALANCER_KEY), KubeConstants.META_ANNOTATIONS_NCP_INTERNAL_LOADBALANCER_VALUE)) {
                                service.setInternalLBFlag(true);
                            }
                            break;
                        default:
                            service.setInternalLBFlag(false);
                            break;
                    }
                }
            }
        }

        this.genServicePortData(cluster, service, v1Service, null);

        service.setCreationTimestamp(v1Service.getMetadata().getCreationTimestamp());
        service.setServiceDeployment(k8sJson.serialize(v1Service));
        service.setServiceDeploymentYaml(Yaml.getSnakeYaml().dumpAsMap(v1Service));

        // Service Details
        K8sServiceDetailVO serviceDetail = new K8sServiceDetailVO();
        serviceDetail.setServiceName(v1Service.getMetadata().getName());
        serviceDetail.setNamespace(v1Service.getMetadata().getNamespace());
        serviceDetail.setLabels(v1Service.getMetadata().getLabels());
        serviceDetail.setAnnotations(v1Service.getMetadata().getAnnotations());
        serviceDetail.setCreationTimestamp(v1Service.getMetadata().getCreationTimestamp());
        serviceDetail.setLabelSelector(v1Service.getSpec().getSelector());
        serviceDetail.setType(v1Service.getSpec().getType());
        serviceDetail.setSessionAffinity(v1Service.getSpec().getSessionAffinity());
        serviceDetail.setSessionAffinityConfig(StringUtils.equalsIgnoreCase("null", sessionAffinityConfigVal) ? null : sessionAffinityConfigVal);
        serviceDetail.setClusterIP(v1Service.getSpec().getClusterIP());
        serviceDetail.setInternalEndpoints(service.getInternalEndpoints());
        serviceDetail.setExternalEndpoints(service.getExternalEndpoints());
        service.setDetail(serviceDetail);

        // Endpoint
        if (MapUtils.isNotEmpty(v1EndpointsMap)) {
            List<K8sEndpointVO> endpoints = new ArrayList<>();
            if (v1EndpointsMap.containsKey(v1Service.getMetadata().getNamespace())
                    && v1EndpointsMap.get(v1Service.getMetadata().getNamespace()).containsKey(v1Service.getMetadata().getName())) {
                V1Endpoints v1EndpointsRow = v1EndpointsMap.get(v1Service.getMetadata().getNamespace()).get(v1Service.getMetadata().getName());
                if(CollectionUtils.isNotEmpty(v1EndpointsRow.getSubsets())){
                    K8sEndpointVO endpoint;
                    for(V1EndpointSubset endpointSubsetRow : v1EndpointsRow.getSubsets()){
                        if(CollectionUtils.isNotEmpty(endpointSubsetRow.getAddresses())){
                            for(V1EndpointAddress endpointAddressRow : endpointSubsetRow.getAddresses()){
                                endpoint = new K8sEndpointVO();
                                endpoint.setHost(endpointAddressRow.getIp() != null ? endpointAddressRow.getIp() : endpointAddressRow.getHostname());
                                endpoint.setReady(true);
                                if(CollectionUtils.isNotEmpty(endpointSubsetRow.getPorts())){
                                    endpoint.setPorts(K8sMapperUtils.getMapper().readValue(K8sMapperUtils.getMapper().writeValueAsString(endpointSubsetRow.getPorts()), new TypeReference<List<Map<String, String>>>(){}));
                                }
                                endpoints.add(endpoint);
                            }

                        }else if(CollectionUtils.isNotEmpty(endpointSubsetRow.getNotReadyAddresses())){
                            for(V1EndpointAddress endpointAddressRow : endpointSubsetRow.getNotReadyAddresses()){
                                endpoint = new K8sEndpointVO();
                                endpoint.setHost(endpointAddressRow.getIp() != null ? endpointAddressRow.getIp() : endpointAddressRow.getHostname());
                                endpoint.setReady(false);
                                if(CollectionUtils.isNotEmpty(endpointSubsetRow.getPorts())){
                                    endpoint.setPorts(K8sMapperUtils.getMapper().readValue(K8sMapperUtils.getMapper().writeValueAsString(endpointSubsetRow.getPorts()), new TypeReference<List<Map<String, String>>>(){}));
                                }
                                endpoints.add(endpoint);
                            }
                        }
                    }
                }
            }

            service.setEndpoints(endpoints);
        }

        return service;
    }

    private Map<String, Map<String, V1Endpoints>> convertEndpointsListToMap(List<V1Endpoints> v1Endpoints) throws Exception{
        Map<String, Map<String, V1Endpoints>> v1EndpointsMap = Maps.newHashMap();
        v1Endpoints = Optional.ofNullable(v1Endpoints).orElseGet(() ->Lists.newArrayList());
        for (V1Endpoints item : v1Endpoints) {
            if (MapUtils.getObject(v1EndpointsMap, item.getMetadata().getNamespace(), null) == null) {
                v1EndpointsMap.put(item.getMetadata().getNamespace(), Maps.newHashMap());
            }

            v1EndpointsMap.get(item.getMetadata().getNamespace()).put(item.getMetadata().getName(), item);
        }
        return v1EndpointsMap;
    }

    /**
     * K8S Service 정보 조회 후 V1Service, V1Endpoints -> K8sServiceVO 변환
     *
     * @param services
     * @param v1Services
     * @param v1Endpoints
     * @return
     * @throws Exception
     */
    private List<K8sServiceVO> genServiceDataList(ClusterVO cluster, List<K8sServiceVO> services, List<V1Service> v1Services, List<V1Endpoints> v1Endpoints) throws Exception{

        if(CollectionUtils.isNotEmpty(v1Services)){

            JSON k8sJson = new JSON();

            Map<String, Map<String, V1Endpoints>> v1EndpointsMap = this.convertEndpointsListToMap(v1Endpoints);

            for(V1Service v1Service : v1Services){
                services.add(this.genServiceData(cluster, null, v1Service, v1EndpointsMap, k8sJson));
            }
        }

        return services;
    }

    private void genServicePortData(ClusterVO cluster, K8sServiceVO service, V1Service v1Service, String endpointVal) throws Exception{
        if(v1Service != null && v1Service.getSpec() != null){
            if(CollectionUtils.isNotEmpty(v1Service.getSpec().getPorts())){
                List<String> internalEndpoints = new ArrayList<>();
                List<K8sServicePortVO> k8sServicePorts = new ArrayList<>();

                for(V1ServicePort servicePort : v1Service.getSpec().getPorts()){
                    endpointVal = "";
                    K8sServicePortVO k8sServicePort = new K8sServicePortVO();
                    k8sServicePort.setName(servicePort.getName());
                    k8sServicePort.setNodePort(servicePort.getNodePort() != null ? String.valueOf(servicePort.getNodePort()) : null);
                    k8sServicePort.setPort(servicePort.getPort() != null ? String.valueOf(servicePort.getPort()) : null);
                    k8sServicePort.setProtocol(servicePort.getProtocol());
                    if(servicePort.getTargetPort() != null) {
                        k8sServicePort.setTargetPort(servicePort.getTargetPort().toString());
                    }

                    if(StringUtils.equalsIgnoreCase(KubeConstants.SPEC_TYPE_VALUE_NODE_PORT, v1Service.getSpec().getType())
                            || StringUtils.equalsIgnoreCase(KubeConstants.SPEC_TYPE_VALUE_LOADBALANCER, v1Service.getSpec().getType())){
                        endpointVal = String.format("%s.%s:%s %s", v1Service.getMetadata().getName(), v1Service.getMetadata().getNamespace(), servicePort.getPort(), servicePort.getProtocol());
                        internalEndpoints.add(endpointVal);
                        endpointVal = String.format("%s.%s:%s %s", v1Service.getMetadata().getName(), v1Service.getMetadata().getNamespace(), servicePort.getNodePort(), servicePort.getProtocol());
                        internalEndpoints.add(endpointVal);

                        // Service URL 셋팅
                        if(StringUtils.equalsIgnoreCase(KubeConstants.SPEC_TYPE_VALUE_NODE_PORT, v1Service.getSpec().getType())){
                            if(cluster != null && StringUtils.isNotBlank(cluster.getNodePortUrl())) {
                                k8sServicePort.setServiceUrl(String.format("%s:%s", cluster.getNodePortUrl(), servicePort.getNodePort()));
                            }

                        }
                        if(StringUtils.equalsIgnoreCase(KubeConstants.SPEC_TYPE_VALUE_LOADBALANCER, v1Service.getSpec().getType())){
                            if(v1Service.getStatus() != null && v1Service.getStatus().getLoadBalancer() != null && CollectionUtils.isNotEmpty(v1Service.getStatus().getLoadBalancer().getIngress())){
                                String ip = v1Service.getStatus().getLoadBalancer().getIngress().get(0).getIp();
                                String hostname = v1Service.getStatus().getLoadBalancer().getIngress().get(0).getHostname();
                                k8sServicePort.setServiceUrl(String.format("%s:%s", (StringUtils.isEmpty(ip)? hostname : ip), servicePort.getPort()));
                            }
                        }
                    }else if(StringUtils.equalsIgnoreCase(KubeConstants.SPEC_TYPE_VALUE_CLUSTER_IP, v1Service.getSpec().getType())){
                        endpointVal = String.format("%s.%s:%s %s", v1Service.getMetadata().getName(), v1Service.getMetadata().getNamespace(), servicePort.getPort(), servicePort.getProtocol());
                        internalEndpoints.add(endpointVal);
                        endpointVal = String.format("%s.%s:%s %s", v1Service.getMetadata().getName(), v1Service.getMetadata().getNamespace(), "0", servicePort.getProtocol());
                        internalEndpoints.add(endpointVal);

                        // Service URL 셋팅
                        k8sServicePort.setServiceUrl(String.format("%s:%s", service.getClusterIP(), servicePort.getPort()));
                    }
                    k8sServicePorts.add(k8sServicePort);
                }

                List<String> externalEndpoints = new ArrayList<>();
                if(StringUtils.equalsIgnoreCase(KubeConstants.SPEC_TYPE_VALUE_LOADBALANCER, v1Service.getSpec().getType())){
                    if(v1Service.getStatus() != null
                            && v1Service.getStatus().getLoadBalancer() != null
                            && CollectionUtils.isNotEmpty(v1Service.getStatus().getLoadBalancer().getIngress()))
                    {
                        for(V1LoadBalancerIngress v1LoadBalancerIngress : v1Service.getStatus().getLoadBalancer().getIngress()){
                            for(V1ServicePort servicePort : v1Service.getSpec().getPorts()){
                                endpointVal = String.format("%s:%s"
                                        , StringUtils.isNotBlank(v1LoadBalancerIngress.getHostname()) ? v1LoadBalancerIngress.getHostname() : v1LoadBalancerIngress.getIp()
                                        , servicePort.getPort());
                                externalEndpoints.add(endpointVal);

                                // Service URL 셋팅
                                Optional<K8sServicePortVO> k8sServicePortOptional = k8sServicePorts.stream().filter(ksp -> (servicePort.getPort().equals(ksp.getPort()))).findFirst();
                                if(k8sServicePortOptional.isPresent()){
                                    k8sServicePortOptional.get().setServiceUrl(endpointVal);
                                }
                            }
                        }
                    }
                }

                service.setInternalEndpoints(internalEndpoints);
                service.setExternalEndpoints(externalEndpoints);
                service.setServicePorts(k8sServicePorts);
            }
        }
    }

    /**
     * 해당 클러스터의 서비스를 조회하여 사용중인 노드포트 리턴
     *
     * @param cluster
     * @param label
     * @param context
     * @return
     * @throws Exception
     */
    public Set<Integer> getUsingNodePortOfCluster(ClusterVO cluster, String field, String label, ExecutingContextVO context) throws Exception{
        List<K8sServiceVO> k8sServices = this.getServices(cluster.getClusterSeq(), null, field, label, context);
//        log.debug(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> getUsingNodePortOfCluster - label : [{}] - withoutLabel : [{}] - serviceList : {}", label, withoutLabel, JsonUtils.toGson(serviceList));
        Set<Integer> nodePorts = new HashSet<>();
        if(CollectionUtils.isNotEmpty(k8sServices)){
            for(K8sServiceVO s : k8sServices){
                if(s.getDetail() != null && StringUtils.equals(s.getDetail().getType(), "NodePort") && CollectionUtils.isNotEmpty(s.getServicePorts())){
                    for(K8sServicePortVO portRow : s.getServicePorts()){
                        nodePorts.add(Integer.valueOf(portRow.getNodePort()));
                    }
                }
            }
        }

        return nodePorts;
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
