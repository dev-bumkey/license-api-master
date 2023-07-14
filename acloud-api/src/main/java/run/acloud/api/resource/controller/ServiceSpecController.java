package run.acloud.api.resource.controller;

import com.google.common.collect.Lists;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import run.acloud.api.configuration.service.ClusterService;
import run.acloud.api.configuration.service.ClusterStateService;
import run.acloud.api.configuration.vo.ClusterVO;
import run.acloud.api.cserver.enums.DeployType;
import run.acloud.api.cserver.enums.PortType;
import run.acloud.api.cserver.service.ServerConversionService;
import run.acloud.api.cserver.service.ServerStateService;
import run.acloud.api.cserver.service.ServerValidService;
import run.acloud.api.cserver.service.ServicemapService;
import run.acloud.api.cserver.vo.ServerStateVO;
import run.acloud.api.resource.constants.KubeConstants;
import run.acloud.api.resource.enums.K8sApiKindType;
import run.acloud.api.resource.enums.K8sApiVerType;
import run.acloud.api.resource.service.K8sResourceService;
import run.acloud.api.resource.service.ServiceSpecService;
import run.acloud.api.resource.vo.*;
import run.acloud.commons.annotations.InHouse;
import run.acloud.commons.enums.ApiVersionType;
import run.acloud.framework.context.ContextHolder;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author wschoi@acornsoft.io
 * Created on 2017. 7. 27.
 */
@Tag(name = "Kubernetes Service Management", description = "쿠버네티스 Service에 대한 관리 기능을 제공한다.")
@Slf4j
@RequestMapping(value = "/api/service-spec")
@RestController
@Validated
public class ServiceSpecController {

    @Autowired
    private K8sResourceService k8sResourceService;

    @Autowired
    private ServiceSpecService serviceSpecService;

    @Autowired
    private ServerValidService serverValidService;

    @Autowired
    private ServerStateService serverStateService;

    @Autowired
    private ClusterService clusterService;

    @Autowired
    private ClusterStateService clusterStateService;

    @Autowired
    private ServicemapService servicemapService;

    @Autowired
    private ServerConversionService serverConversionService;

    @Deprecated
    @PostMapping("/{apiVersion}/service/{serviceSeq}/servicemap/{servicemapSeq}")
    @Operation(summary = "K8s Service 추가", description = "지정한 워크스페이스와 서비스맵 안에 K8s Service를 생성한다.")
    public K8sServiceVO addServiceSpec(
        @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v2"}), required = true) @PathVariable String apiVersion,
        @Parameter(name = "serviceSeq", description = "service seq", required = true) @PathVariable Integer serviceSeq,
        @Parameter(name = "servicemapSeq", description = "servicemapSeq", required = true) @PathVariable Integer servicemapSeq,
        @Parameter(name = "serviceSpec", description = "추가하려는 serviceSpec", required = true) @RequestBody ServiceSpecIntegrateVO serviceSpecParam
    ) throws Exception {

        log.debug("[BEGIN] addServiceSpec");

        ServiceSpecGuiVO serviceSpec;
        if (DeployType.valueOf(serviceSpecParam.getDeployType()) != DeployType.GUI) {
            throw new CocktailException("Only GUI supported.", ExceptionType.InvalidParameter);
        }
        else {
            serviceSpec = (ServiceSpecGuiVO)serviceSpecParam;
        }

        /**
         * cluster 상태 체크
         */
        clusterStateService.checkClusterStateByServicemap(servicemapSeq);

        ClusterVO cluster = serviceSpecService.setupCluster(servicemapSeq);
        K8sServiceVO k8sService;

        if (StringUtils.isBlank(serviceSpec.getName()) || !serviceSpec.getName().matches(KubeConstants.RULE_RESOURCE_NAME)) {
            throw new CocktailException("Service name is invalid", ExceptionType.K8sServiceNameInvalid);
        }
        k8sService = serviceSpecService.getService(cluster, cluster.getNamespaceName(), serviceSpec.getName(), ContextHolder.exeContext());
        if(k8sService != null){
            throw new CocktailException("Service already exists!!", ExceptionType.ServiceNameAlreadyExists);
        }
//        List<ComponentVO> components = componentService.getComponentsInAppmapByName(serviceSpec.getName(), appmapSeq);
//        if(CollectionUtils.isNotEmpty(components)){
//            throw new CocktailException("Reserved Service Name!!", ExceptionType.ReservedServiceName);
//        }
        if(StringUtils.isBlank(serviceSpec.getServiceType()) || PortType.findPortName(serviceSpec.getServiceType()) == null) {
            throw new CocktailException("ServiceType is invalid!", ExceptionType.InvalidInputData);
        }
        // validateHostPort
        Set<Integer> nodePorts = serviceSpecService.getUsingNodePortOfCluster(cluster, null, null, ContextHolder.exeContext()); // 해당 cluster의 k8s - service node port 조회
        List<ServiceSpecGuiVO> serviceSpecs = new ArrayList<>();
        serviceSpecs.add(serviceSpec);
        serverValidService.validateHostPort(serviceSpecs, nodePorts, cluster);

        if (!K8sApiVerType.isK8sVerSupported(cluster.getK8sVersion(), K8sApiVerType.V1_19)) {
            // SCTP 프로토콜 사용여부 및 클러스터 지원여부 체크
            boolean useStcp = serviceSpec.getServicePorts().stream().anyMatch(sp -> "SCTP".equalsIgnoreCase(sp.getProtocol()));

            if (useStcp) {
                Map<String, Boolean> featureGates = k8sResourceService.getFeatureGates(cluster);
                if (MapUtils.isEmpty(featureGates)) {
                    throw new CocktailException("SCTP Protocol Not Supported.", ExceptionType.ProtocolNotSupported);
                } else {
                    if (!MapUtils.getBooleanValue(featureGates, "SCTPSupport", false)) {
                        throw new CocktailException("SCTP Protocol Not Supported.", ExceptionType.ProtocolNotSupported);
                    }
                }
            }
        }

        ApiVersionType apiVersionType = ApiVersionType.valueOf(StringUtils.upperCase(apiVersion));
        ContextHolder.exeContext().setApiVersionType(apiVersionType);

        k8sService = serviceSpecService.createService(cluster, cluster.getNamespaceName(), serviceSpec, ContextHolder.exeContext());

        log.debug("[END  ] addServiceSpec");

        return k8sService;
    }


    @Deprecated
    @GetMapping("/{apiVersion}/service/{serviceSeq}/servicemap/{servicemapSeq}/services")
    @Operation(summary = "K8s Service 목록 조회", description = "지정한 워크스페이스, 서비스맵에 속한 Service 목록 반환")
    public List<K8sServiceInfoVO> getServiceSpecs(
            @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v2"}), required = true) @PathVariable String apiVersion,
            @Parameter(name = "serviceSeq", description = "service seq", required = true) @PathVariable Integer serviceSeq,
            @Parameter(name = "servicemapSeq", description = "servicemapSeq", required = true) @PathVariable Integer servicemapSeq
    ) throws Exception {

        log.debug("[BEGIN] getServiceSpecs");

        /**
         * cluster 상태 체크
         */
        clusterStateService.checkClusterStateByServicemap(servicemapSeq);

        ApiVersionType apiVersionType = ApiVersionType.valueOf(StringUtils.upperCase(apiVersion));
        ContextHolder.exeContext().setApiVersionType(apiVersionType);

        ClusterVO cluster = serviceSpecService.setupCluster(servicemapSeq);

        /** Cocktail에서 생성한 Service 전체 조회로 변경 : R3.5.0 : 2019.11.21 **/
        String label = null;
        List<K8sServiceVO> cocktailK8sServices = serviceSpecService.getServices(cluster, cluster.getNamespaceName(), null, label, ContextHolder.exeContext());

        List<K8sServiceInfoVO> k8sServices = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(cocktailK8sServices)) {
            for (K8sServiceVO k8sService : cocktailK8sServices) {
                // K8sServiceVO -> K8sServiceInfoVO
                K8sServiceInfoVO k8sServiceInfo = new K8sServiceInfoVO();
                BeanUtils.copyProperties(k8sService, k8sServiceInfo);
                k8sServices.add(k8sServiceInfo);
            }
        }

        /** Namespace안의 서비스 이벤트를 조회하여
         * 아래에서 서비스와 Pod의 이벤트 정보를 응답 **/
        List<K8sEventVO> namespaceEvents = k8sResourceService.getEventByCluster(cluster.getClusterSeq(), cluster.getNamespaceName(), String.format("%s=%s", "involvedObject.kind", K8sApiKindType.SERVICE.getValue()), null, ContextHolder.exeContext());
        Map<String, List<K8sEventVO>> serviceEventMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(namespaceEvents)) {
            for (K8sEventVO eventRow : namespaceEvents) {
                if (StringUtils.equals(K8sApiKindType.SERVICE.getValue(), eventRow.getKind())) {
                    if (!serviceEventMap.containsKey(eventRow.getName())) {
                        serviceEventMap.put(eventRow.getName(), Lists.newArrayList());
                    }

                    serviceEventMap.get(eventRow.getName()).add(eventRow);
                }
            }
        }

        /** 연결된 워크로드를 찾아서 매칭...
         * 연결된 워크로드를 찾으려면 전체 워크로드를 조회하여 찾아야 하므로 기존 워크로드 상태 조회 로직 활용 함.. **/
        ServerStateVO serverStates = serverStateService.getWorkloadsStateInNamespace(cluster, cluster.getNamespaceName(), null, true, ContextHolder.exeContext());
        if(CollectionUtils.isNotEmpty(k8sServices) && serverStates != null && CollectionUtils.isNotEmpty(serverStates.getComponents())){
            List<ComponentVO> componentsIncludeService = serverStates.getComponents().stream().filter(c -> CollectionUtils.isNotEmpty(c.getServices())).collect(Collectors.toList());
            for (K8sServiceInfoVO k8sServiceRow : k8sServices) {
                List<ComponentVO> components = new ArrayList<>();

                for(ComponentVO component : componentsIncludeService) {

                    for (ServiceSpecGuiVO service : component.getServices()) {
                        // component의 서비스와 service리스트의 서비스가 동일하면
                        if (StringUtils.equals(service.getNamespaceName(), k8sServiceRow.getNamespace())
                                && StringUtils.equals(service.getName(), k8sServiceRow.getServiceName())
                        ) {
                            ComponentVO matchComponent = this.makeComponent(component);
                            components.add(matchComponent);
                        }
                    }

                }

                // 워크로드 정보 셋팅
                k8sServiceRow.setWorkloads(components);

                /** 서비스에 매칭되는 이벤트 찾아서 응답 **/
                k8sServiceRow.setEvents(serviceEventMap.get(k8sServiceRow.getServiceName()));
            }
        }

        log.debug("[END  ] getServiceSpecs");

        return k8sServices;
    }

    @GetMapping("/{apiVersion}/cluster/{clusterSeq}/services")
    @Operation(summary = "K8s Service 목록 조회", description = "지정한 클러스터에 속한 Service 목록 반환")
    public List<K8sServiceVO> getServiceSpecsInWorkspace(
            @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v2"}), required = true) @PathVariable String apiVersion,
            @Parameter(name = "clusterSeq", description = "clusterSeq", required = true) @PathVariable Integer clusterSeq,
            @Parameter(name = "serviceSeq", description = "serviceSeq") @RequestParam(name = "serviceSeq", required = false) Integer serviceSeq,
            @Parameter(name = "acloudOnly", description = "Acloud에서 관리되는 서비스만 조회 여부", schema = @Schema(allowableValues = {"true","false"}, defaultValue = "false")) @RequestParam(value = "acloudOnly", defaultValue = "false", required = false) Boolean acloudOnly
    ) throws Exception {

        log.debug("[BEGIN] getServiceSpecs");

        /**
         * cluster 상태 체크
         */
        clusterStateService.checkClusterState(clusterSeq);

        ApiVersionType apiVersionType = ApiVersionType.valueOf(StringUtils.upperCase(apiVersion));
        ContextHolder.exeContext().setApiVersionType(apiVersionType);

        ClusterVO cluster = clusterService.getCluster(clusterSeq);
        Set<String> namespaces = new HashSet<>();
        if (serviceSeq != null) {
            namespaces = servicemapService.getNamespaceNamesByServiceInCluster(serviceSeq, clusterSeq);
        }

        List<K8sServiceVO> k8sServices = new ArrayList<>();

        /** Cocktail에서 생성한 Service 전체 조회로 변경 : R3.5.0 : 2019.11.21 **/
        String label = null;
        if(acloudOnly) {
            label = KubeConstants.LABELS_COCKTAIL_KEY;
        }
        List<K8sServiceVO> cocktailK8sServices = serviceSpecService.getServices(cluster, null, null, label, ContextHolder.exeContext());

        List<K8sServiceVO> managedK8sServices = new ArrayList<>();
        if(acloudOnly) {
            // AcloudOnly 불가능.. Cocktail Label 사라짐..
            // managedK8sServices = cocktailK8sServices.stream().filter(svc -> ("Y".equals(svc.getIsManagedInCocktail()))).collect(Collectors.toList());
            managedK8sServices.addAll(cocktailK8sServices);
        }
        else {
            managedK8sServices.addAll(cocktailK8sServices);
        }

        for (K8sServiceVO k8sServiceRow : managedK8sServices) {
            if (serviceSeq != null) {
                if (CollectionUtils.isNotEmpty(namespaces) && namespaces.contains(k8sServiceRow.getNamespace())) {
                    k8sServices.add(k8sServiceRow);
                }
            } else {
                k8sServices.add(k8sServiceRow);
            }
        }


        log.debug("[END  ] getServiceSpecs");

        return k8sServices;
    }

    @Deprecated
    @GetMapping("/{apiVersion}/service/{serviceSeq}/servicemap/{servicemapSeq}/services/all")
    @Operation(summary = "K8s Service 목록 조회", description = "지정한 워크스페이스, 서비스맵에 속한 Service 목록 반환")
    public List<K8sServiceVO> getAllServiceSpecs(
            @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v2"}), required = true) @PathVariable String apiVersion,
            @Parameter(name = "serviceSeq", description = "service seq", required = true) @PathVariable Integer serviceSeq,
            @Parameter(name = "servicemapSeq", description = "servicemapSeq", required = true) @PathVariable Integer servicemapSeq,
            @Parameter(name = "acloudOnly", description = "Acloud에서 관리되는 서비스만 조회 여부", schema = @Schema(allowableValues = {"true","false"}, defaultValue = "false")) @RequestParam(value = "acloudOnly", defaultValue = "false", required = false) Boolean acloudOnly
    ) throws Exception {

        log.debug("[BEGIN] getAllServiceSpecs");

        /**
         * cluster 상태 체크
         */
        clusterStateService.checkClusterStateByServicemap(servicemapSeq);

        ApiVersionType apiVersionType = ApiVersionType.valueOf(StringUtils.upperCase(apiVersion));
        ContextHolder.exeContext().setApiVersionType(apiVersionType);

        ClusterVO cluster = serviceSpecService.setupCluster(servicemapSeq);

        /**
         * Acloud에서 관리되는 서비스만 조회 or 전체 조회 판단. : 2019.06.11
         */
        String label = null;
        if(acloudOnly) {
            label = KubeConstants.LABELS_COCKTAIL_KEY;
        }

        // 서비스에서 생성한 Service
        List<K8sServiceVO> k8sServices = serviceSpecService.getServices(cluster, cluster.getNamespaceName(), null, label, ContextHolder.exeContext());

        log.debug("[END  ] getAllServiceSpecs");

        return k8sServices;
    }

    @Deprecated
    @PutMapping("/{apiVersion}/service/{serviceSeq}/servicemap/{servicemapSeq}/service/{serviceName:.+}")
    @Operation(summary = "K8s Service 수정", description = "지정한 K8s Service의 정보를 수정한다.")
    public K8sServiceVO updateService(
            @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v2"}), required = true) @PathVariable String apiVersion,
            @Parameter(name = "serviceSeq", description = "service seq", required = true) @PathVariable Integer serviceSeq,
            @Parameter(name = "servicemapSeq", description = "servicemapSeq", required = true) @PathVariable Integer servicemapSeq,
            @Parameter(name = "serviceName", description = "serviceName", required = true) @PathVariable String serviceName,
            @Parameter(name = "serviceSpec", description = "수정하려는 serviceSpec", required = true) @RequestBody @Validated ServiceSpecIntegrateVO serviceSpecParam
    ) throws Exception {

        log.debug("[BEGIN] updateService");

        /**
         * cluster 상태 체크
         */
        clusterStateService.checkClusterStateByServicemap(servicemapSeq);

        ClusterVO cluster = serviceSpecService.setupCluster(servicemapSeq);
        K8sServiceVO k8sService;

        if (DeployType.valueOf(serviceSpecParam.getDeployType()) == DeployType.GUI) {
            ServiceSpecGuiVO serviceSpec = (ServiceSpecGuiVO) serviceSpecParam;
            if(!serviceName.equals(serviceSpec.getName())) {
                throw new CocktailException("Can't change the Service name. (Service name is different)", ExceptionType.K8sServiceNameInvalid);
            }
            if (StringUtils.isBlank(serviceSpec.getName()) || !serviceSpec.getName().matches(KubeConstants.RULE_RESOURCE_NAME)) {
                throw new CocktailException("Service name is invalid", ExceptionType.K8sServiceNameInvalid);
            }
            if(StringUtils.isBlank(serviceSpec.getServiceType()) || PortType.findPortName(serviceSpec.getServiceType()) == null) {
                throw new CocktailException("ServiceType is invalid!", ExceptionType.InvalidInputData);
            }
            k8sService = serviceSpecService.getService(cluster, cluster.getNamespaceName(), serviceSpec.getName(), ContextHolder.exeContext());
            if (k8sService == null) {
                throw new CocktailException("Service not found!!", ExceptionType.K8sServiceNotFound);
            }
            // validateHostPort
            String field = String.format("%s.%s!=%s", KubeConstants.META, KubeConstants.NAME, serviceName);
            Set<Integer> nodePorts = serviceSpecService.getUsingNodePortOfCluster(cluster, field, null, ContextHolder.exeContext()); // 해당 cluster의 k8s - service node port 조회
            List<ServiceSpecGuiVO> serviceSpecs = new ArrayList<>();
            serviceSpecs.add(serviceSpec);
            serverValidService.validateHostPort(serviceSpecs, nodePorts, cluster);

            ApiVersionType apiVersionType = ApiVersionType.valueOf(StringUtils.upperCase(apiVersion));
            ContextHolder.exeContext().setApiVersionType(apiVersionType);

            k8sService = serviceSpecService.patchService( cluster, cluster.getNamespaceName(), serviceSpec, ContextHolder.exeContext() );
        }
        else {
            ServiceSpecYamlVO serviceSpecYaml = (ServiceSpecYamlVO) serviceSpecParam;
            if(!serviceName.equals(serviceSpecYaml.getName())) {
                throw new CocktailException("Can't change the Service name. (Service name is different)", ExceptionType.K8sServiceNameInvalid);
            }

            if (StringUtils.isBlank(serviceSpecYaml.getName()) || !serviceSpecYaml.getName().matches(KubeConstants.RULE_RESOURCE_NAME)) {
                throw new CocktailException("Service name is invalid", ExceptionType.K8sServiceNameInvalid);
            }

            k8sService = serviceSpecService.getService(cluster, cluster.getNamespaceName(), serviceSpecYaml.getName(), ContextHolder.exeContext());
            if (k8sService == null) {
                throw new CocktailException("Service not found!!", ExceptionType.K8sServiceNotFound);
            }

            // validateHostPort
            String field = String.format("%s.%s!=%s", KubeConstants.META, KubeConstants.NAME, serviceName);
            Set<Integer> nodePorts = serviceSpecService.getUsingNodePortOfCluster(cluster, field, null, ContextHolder.exeContext()); // 해당 cluster의 k8s - service node port 조회
            List<ServiceSpecGuiVO> serviceSpecs = new ArrayList<>();
            ServiceSpecGuiVO newServiceSpec = serverConversionService.convertYamlToServiceSpec(cluster, serviceSpecYaml.getYaml());
            serviceSpecs.add(newServiceSpec);
            serverValidService.validateHostPort(serviceSpecs, nodePorts, cluster);

            if(!serviceName.equals(newServiceSpec.getName())) {
                throw new CocktailException("Can't change the Service name. (Service name is different)", ExceptionType.K8sServiceNameInvalid);
            }
            if(!cluster.getNamespaceName().equals(newServiceSpec.getNamespaceName())) {
                throw new CocktailException("Can't change the Service namespace. (Service namespace is different)", ExceptionType.NamespaceNameInvalid);
            }

            ApiVersionType apiVersionType = ApiVersionType.valueOf(StringUtils.upperCase(apiVersion));
            ContextHolder.exeContext().setApiVersionType(apiVersionType);

            /**
             * Package는 일단 고려하지 않는다..
             */
            k8sService = serviceSpecService.patchServiceWithYaml( cluster, cluster.getNamespaceName(), newServiceSpec, serviceSpecYaml, ContextHolder.exeContext() );
        }

        log.debug("[END  ] updateService");

        return k8sService;
    }

    @Deprecated
    @DeleteMapping("/{apiVersion}/service/{serviceSeq}/servicemapSeqmap/{servicemapSeq}/service/{serviceName:.+}")
    @Operation(summary = "K8s Service 삭제", description = "지정한 K8s Service를 삭제한다.")
    public void deleteService(
            @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v2"}), required = true) @PathVariable String apiVersion,
            @Parameter(name = "serviceSeq", description = "service seq", required = true) @PathVariable Integer serviceSeq,
            @Parameter(name = "servicemapSeq", description = "servicemapSeq", required = true) @PathVariable Integer servicemapSeq,
            @Parameter(name = "serviceName", description = "serviceName", required = true) @PathVariable String serviceName
    ) throws Exception {

        log.debug("[BEGIN] deleteService");

        /**
         * cluster 상태 체크
         */
        clusterStateService.checkClusterStateByServicemap(servicemapSeq);

        ClusterVO cluster = serviceSpecService.setupCluster(servicemapSeq);

        ApiVersionType apiVersionType = ApiVersionType.valueOf(StringUtils.upperCase(apiVersion));
        ContextHolder.exeContext().setApiVersionType(apiVersionType);

        K8sServiceVO k8sService = serviceSpecService.getService(cluster, cluster.getNamespaceName(), serviceName, ContextHolder.exeContext());
        if(k8sService != null){
            serviceSpecService.deleteService(cluster, serviceName, null, ContextHolder.exeContext());
        }


        log.debug("[END  ] deleteService");

    }

    /*******************************************************************************************************************************
     * [시작] R3.5.0, clusterSeq, namespace 사용 추가, coolingi, 2019/12/10
     *******************************************************************************************************************************/

    @PostMapping("/{apiVersion}/cluster/{clusterSeq}/namespace/{namespaceName}")
    @Operation(summary = "K8s Service를 추가한다", description = "지정한 클러스터 및 네임스페이스에 속한 K8s Service를 생성한다.")
    public K8sServiceVO addServiceSpec(
            @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v2"}), required = true) @PathVariable String apiVersion,
            @Parameter(name = "clusterSeq", description = "cluster seq", required = true) @PathVariable Integer clusterSeq,
            @Parameter(name = "namespaceName", description = "namespace name", required = true) @PathVariable String namespaceName,
            @Parameter(name = "serviceSpec", description = "추가하려는 serviceSpec", required = true) @RequestBody ServiceSpecIntegrateVO serviceSpecParam
    ) throws Exception {

        log.debug("[BEGIN] addServiceSpec");

        ServiceSpecGuiVO serviceSpec;
        if (DeployType.valueOf(serviceSpecParam.getDeployType()) != DeployType.GUI) {
            throw new CocktailException("Only GUI supported.", ExceptionType.InvalidParameter);
        }
        else {
            serviceSpec = (ServiceSpecGuiVO)serviceSpecParam;
        }

        /**
         * cluster 상태 체크
         */
        clusterStateService.checkClusterState(clusterSeq);

        ClusterVO cluster = serviceSpecService.setupCluster(clusterSeq, namespaceName);
        K8sServiceVO k8sService;

        if (StringUtils.isBlank(serviceSpec.getName()) || !serviceSpec.getName().matches(KubeConstants.RULE_RESOURCE_NAME)) {
            throw new CocktailException("Service name is invalid", ExceptionType.K8sServiceNameInvalid);
        }
        k8sService = serviceSpecService.getService(cluster, cluster.getNamespaceName(), serviceSpec.getName(), ContextHolder.exeContext());
        if(k8sService != null){
            throw new CocktailException("Service already exists!!", ExceptionType.ServiceNameAlreadyExists);
        }
        if(StringUtils.isBlank(serviceSpec.getServiceType()) || PortType.findPortName(serviceSpec.getServiceType()) == null) {
            throw new CocktailException("ServiceType is invalid!", ExceptionType.InvalidInputData);
        }

//        ComponentDetailsVO componentDetailsVO = serverStateService.getComponentDetails(cluster, namespaceName, serviceSpec.getName(), false, ContextHolder.exeContext());
//        if(componentDetailsVO != null && CollectionUtils.isNotEmpty(componentDetailsVO.getComponents())){
//            throw new CocktailException("Reserved Service Name!!", ExceptionType.ReservedServiceName);
//        }

        // validateHostPort
        Set<Integer> nodePorts = serviceSpecService.getUsingNodePortOfCluster(cluster, null, null, ContextHolder.exeContext()); // 해당 cluster의 k8s - service node port 조회
        List<ServiceSpecGuiVO> serviceSpecs = new ArrayList<>();
        serviceSpecs.add(serviceSpec);
        serverValidService.validateHostPort(serviceSpecs, nodePorts, cluster);

        if (!K8sApiVerType.isK8sVerSupported(cluster.getK8sVersion(), K8sApiVerType.V1_19)) {
            // SCTP 프로토콜 사용여부 및 클러스터 지원여부 체크
            boolean useStcp = serviceSpec.getServicePorts().stream().anyMatch(sp -> "SCTP".equalsIgnoreCase(sp.getProtocol()));

            if (useStcp) {
                Map<String, Boolean> featureGates = k8sResourceService.getFeatureGates(cluster);
                if (MapUtils.isEmpty(featureGates)) {
                    throw new CocktailException("SCTP Protocol Not Supported.", ExceptionType.ProtocolNotSupported);
                } else {
                    if (!MapUtils.getBooleanValue(featureGates, "SCTPSupport", false)) {
                        throw new CocktailException("SCTP Protocol Not Supported.", ExceptionType.ProtocolNotSupported);
                    }
                }
            }
        }

        ApiVersionType apiVersionType = ApiVersionType.valueOf(StringUtils.upperCase(apiVersion));
        ContextHolder.exeContext().setApiVersionType(apiVersionType);

        k8sService = serviceSpecService.createService(cluster, cluster.getNamespaceName(), serviceSpec, ContextHolder.exeContext());

        log.debug("[END  ] addServiceSpec");

        return k8sService;
    }



    @GetMapping("/{apiVersion}/cluster/{clusterSeq}/namespace/{namespaceName}/services")
    @Operation(summary = "K8s Service 목록 조회", description = "지정한 클러스터, 네임스페이스에 속한 Service 목록 반환")
    public List<K8sServiceInfoVO> getServiceSpecs(
            @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v2"}), required = true) @PathVariable String apiVersion,
            @Parameter(name = "clusterSeq", description = "cluster seq", required = true) @PathVariable Integer clusterSeq,
            @Parameter(name = "namespaceName", description = "namespace name", required = true) @PathVariable String namespaceName,
            @Deprecated @Parameter(name = "acloudOnly", description = "Acloud에서 관리되는 서비스만 조회 여부", schema = @Schema(allowableValues = {"true","false"}, defaultValue = "false")) @RequestParam(value = "acloudOnly", defaultValue = "false", required = false) Boolean acloudOnly
    ) throws Exception {

        log.debug("[BEGIN] getServiceSpecs");

        /**
         * cluster 상태 체크
         */
        clusterStateService.checkClusterState(clusterSeq);

        ApiVersionType apiVersionType = ApiVersionType.valueOf(StringUtils.upperCase(apiVersion));
        ContextHolder.exeContext().setApiVersionType(apiVersionType);

        ClusterVO cluster = serviceSpecService.setupCluster(clusterSeq, namespaceName);

        /** Cocktail에서 생성한 Service 전체 조회로 변경 : R3.5.0 : 2019.11.21 **/
        String label = null;
        List<K8sServiceVO> cocktailK8sServices = serviceSpecService.getServices(cluster, cluster.getNamespaceName(), null, label, ContextHolder.exeContext());

        List<K8sServiceInfoVO> k8sServices = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(cocktailK8sServices)) {
            for (K8sServiceVO k8sService : cocktailK8sServices) {
                // K8sServiceVO -> K8sServiceInfoVO
                K8sServiceInfoVO k8sServiceInfo = new K8sServiceInfoVO();
                BeanUtils.copyProperties(k8sService, k8sServiceInfo);
                k8sServices.add(k8sServiceInfo);
            }
        }

        /** Namespace안의 서비스 이벤트를 조회하여
         * 아래에서 서비스와 Pod의 이벤트 정보를 응답 **/
        List<K8sEventVO> namespaceEvents = k8sResourceService.getEventByCluster(cluster.getClusterSeq(), cluster.getNamespaceName(), String.format("%s=%s", "involvedObject.kind", K8sApiKindType.SERVICE.getValue()), null, ContextHolder.exeContext());
        Map<String, List<K8sEventVO>> serviceEventMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(namespaceEvents)) {
            for (K8sEventVO eventRow : namespaceEvents) {
                if (StringUtils.equals(K8sApiKindType.SERVICE.getValue(), eventRow.getKind())) {
                    if (!serviceEventMap.containsKey(eventRow.getName())) {
                        serviceEventMap.put(eventRow.getName(), Lists.newArrayList());
                    }

                    serviceEventMap.get(eventRow.getName()).add(eventRow);
                }
            }
        }

        /** 연결된 워크로드를 찾아서 매칭...
         * 연결된 워크로드를 찾으려면 전체 워크로드를 조회하여 찾아야 하므로 기존 워크로드 상태 조회 로직 활용 함.. **/
        ServerStateVO serverStates = serverStateService.getWorkloadsStateInNamespace(cluster, cluster.getNamespaceName(), null, true, ContextHolder.exeContext());
        if(CollectionUtils.isNotEmpty(k8sServices) && serverStates != null && CollectionUtils.isNotEmpty(serverStates.getComponents())){
            List<ComponentVO> componentsIncludeService = serverStates.getComponents().stream().filter(c -> CollectionUtils.isNotEmpty(c.getServices())).collect(Collectors.toList());
            for (K8sServiceInfoVO k8sServiceRow : k8sServices) {
                List<ComponentVO> components = new ArrayList<>();

                for(ComponentVO component : componentsIncludeService) {

                    for (ServiceSpecGuiVO service : component.getServices()) {
                        // component의 서비스와 service리스트의 서비스가 동일하면
                        if (StringUtils.equals(service.getNamespaceName(), k8sServiceRow.getNamespace())
                                && StringUtils.equals(service.getName(), k8sServiceRow.getServiceName())
                        ) {
                            ComponentVO matchComponent = this.makeComponent(component);
                            components.add(matchComponent);
                        }
                    }

                }

                // 워크로드 정보 셋팅
                k8sServiceRow.setWorkloads(components);

                /** 서비스에 매칭되는 이벤트 찾아서 응답 **/
                k8sServiceRow.setEvents(serviceEventMap.get(k8sServiceRow.getServiceName()));
            }
        }

        log.debug("[END  ] getServiceSpecs");

        return k8sServices;
    }

    @InHouse // 위 API (getServiceSpecs)와 중복 같아 보여 InHouse 처리함..
    @GetMapping("/{apiVersion}/cluster/{clusterSeq}/namespace/{namespaceName}/services/all")
    @Operation(summary = "K8s Service 목록 조회", description = "지정한 클러스터, 네임스페이스에 속한 Service 목록 반환")
    public List<K8sServiceVO> getAllServiceSpecs(
            @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v2"}), required = true) @PathVariable String apiVersion,
            @Parameter(name = "clusterSeq", description = "cluster seq", required = true) @PathVariable Integer clusterSeq,
            @Parameter(name = "namespaceName", description = "namespace name", required = true) @PathVariable String namespaceName,
            @Parameter(name = "acloudOnly", description = "Acloud에서 관리되는 서비스만 조회 여부", schema = @Schema(allowableValues = {"true","false"}, defaultValue = "false")) @RequestParam(value = "acloudOnly", defaultValue = "false", required = false) Boolean acloudOnly
    ) throws Exception {

        log.debug("[BEGIN] getAllServiceSpecs");

        /**
         * cluster 상태 체크
         */
        clusterStateService.checkClusterState(clusterSeq);

        ApiVersionType apiVersionType = ApiVersionType.valueOf(StringUtils.upperCase(apiVersion));
        ContextHolder.exeContext().setApiVersionType(apiVersionType);

        ClusterVO cluster = serviceSpecService.setupCluster(clusterSeq, namespaceName);

        /**
         * Acloud에서 관리되는 서비스만 조회 or 전체 조회 판단. : 2019.06.11
         */
        String label = null;
        if(acloudOnly) {
            label = KubeConstants.LABELS_COCKTAIL_KEY;
        }

        // 서비스에서 생성한 Service
        List<K8sServiceVO> k8sServices = serviceSpecService.getServices(cluster, cluster.getNamespaceName(), null, label, ContextHolder.exeContext());

        log.debug("[END  ] getAllServiceSpecs");

        return k8sServices;
    }

    @PutMapping("/{apiVersion}/cluster/{clusterSeq}/namespace/{namespaceName}/service/{serviceName:.+}")
    @Operation(summary = "K8s Service 수정", description = "지정한 K8s Service를 수정한다.")
    public K8sServiceVO updateService(
            @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v2"}), required = true) @PathVariable String apiVersion,
            @Parameter(name = "clusterSeq", description = "cluster seq", required = true) @PathVariable Integer clusterSeq,
            @Parameter(name = "namespaceName", description = "namespace name", required = true) @PathVariable String namespaceName,
            @Parameter(name = "serviceName", description = "serviceName", required = true) @PathVariable String serviceName,
            @Parameter(name = "serviceSpec", description = "수정하려는 serviceSpec", required = true) @RequestBody @Validated ServiceSpecIntegrateVO serviceSpecParam
    ) throws Exception {

        log.debug("[BEGIN] updateService");

        /**
         * cluster 상태 체크
         */
        clusterStateService.checkClusterState(clusterSeq);

        ClusterVO cluster = serviceSpecService.setupCluster(clusterSeq, namespaceName);
        K8sServiceVO k8sService;

        if (DeployType.valueOf(serviceSpecParam.getDeployType()) == DeployType.GUI) {
            ServiceSpecGuiVO serviceSpec = (ServiceSpecGuiVO) serviceSpecParam;
            if(!serviceName.equals(serviceSpec.getName())) {
                throw new CocktailException("Can't change the Service name. (Service name is different)", ExceptionType.K8sServiceNameInvalid);
            }
            if (StringUtils.isBlank(serviceSpec.getName()) || !serviceSpec.getName().matches(KubeConstants.RULE_RESOURCE_NAME)) {
                throw new CocktailException("Service name is invalid", ExceptionType.K8sServiceNameInvalid);
            }
            if(StringUtils.isBlank(serviceSpec.getServiceType()) || PortType.findPortName(serviceSpec.getServiceType()) == null) {
                throw new CocktailException("ServiceType is invalid!", ExceptionType.InvalidInputData);
            }
            k8sService = serviceSpecService.getService(cluster, cluster.getNamespaceName(), serviceSpec.getName(), ContextHolder.exeContext());
            if (k8sService == null) {
                throw new CocktailException("Service not found!!", ExceptionType.K8sServiceNotFound);
            }
            // validateHostPort
            String field = String.format("%s.%s!=%s", KubeConstants.META, KubeConstants.NAME, serviceName);
            Set<Integer> nodePorts = serviceSpecService.getUsingNodePortOfCluster(cluster, field, null, ContextHolder.exeContext()); // 해당 cluster의 k8s - service node port 조회
            List<ServiceSpecGuiVO> serviceSpecs = new ArrayList<>();
            serviceSpecs.add(serviceSpec);
            serverValidService.validateHostPort(serviceSpecs, nodePorts, cluster);

            ApiVersionType apiVersionType = ApiVersionType.valueOf(StringUtils.upperCase(apiVersion));
            ContextHolder.exeContext().setApiVersionType(apiVersionType);

            k8sService = serviceSpecService.patchService( cluster, cluster.getNamespaceName(), serviceSpec, ContextHolder.exeContext() );
        }
        else {
            ServiceSpecYamlVO serviceSpecYaml = (ServiceSpecYamlVO) serviceSpecParam;
            if(!serviceName.equals(serviceSpecYaml.getName())) {
                throw new CocktailException("Can't change the Service name. (Service name is different)", ExceptionType.K8sServiceNameInvalid);
            }

            if (StringUtils.isBlank(serviceSpecYaml.getName()) || !serviceSpecYaml.getName().matches(KubeConstants.RULE_RESOURCE_NAME)) {
                throw new CocktailException("Service name is invalid", ExceptionType.K8sServiceNameInvalid);
            }

            k8sService = serviceSpecService.getService(cluster, cluster.getNamespaceName(), serviceSpecYaml.getName(), ContextHolder.exeContext());
            if (k8sService == null) {
                throw new CocktailException("Service not found!!", ExceptionType.K8sServiceNotFound);
            }

            // validateHostPort
            String field = String.format("%s.%s!=%s", KubeConstants.META, KubeConstants.NAME, serviceName);
            Set<Integer> nodePorts = serviceSpecService.getUsingNodePortOfCluster(cluster, field, null, ContextHolder.exeContext()); // 해당 cluster의 k8s - service node port 조회
            List<ServiceSpecGuiVO> serviceSpecs = new ArrayList<>();
            ServiceSpecGuiVO newServiceSpec = serverConversionService.convertYamlToServiceSpec(cluster, serviceSpecYaml.getYaml());
            serviceSpecs.add(newServiceSpec);
            serverValidService.validateHostPort(serviceSpecs, nodePorts, cluster);

            if(!serviceName.equals(newServiceSpec.getName())) {
                throw new CocktailException("Can't change the Service name. (Service name is different)", ExceptionType.K8sServiceNameInvalid);
            }
            if(!cluster.getNamespaceName().equals(newServiceSpec.getNamespaceName())) {
                throw new CocktailException("Can't change the Service namespace. (Service namespace is different)", ExceptionType.NamespaceNameInvalid);
            }

            ApiVersionType apiVersionType = ApiVersionType.valueOf(StringUtils.upperCase(apiVersion));
            ContextHolder.exeContext().setApiVersionType(apiVersionType);

            /**
             * Package는 일단 고려하지 않는다..
             */
            k8sService = serviceSpecService.patchServiceWithYaml( cluster, cluster.getNamespaceName(), newServiceSpec, serviceSpecYaml, ContextHolder.exeContext() );
        }

        log.debug("[END  ] updateService");

        return k8sService;
    }

    @DeleteMapping("/{apiVersion}/cluster/{clusterSeq}/namespace/{namespaceName}/service/{serviceName:.+}")
    @Operation(summary = "K8s Service 삭제", description = "지정한 K8s Service를 삭한다.")
    public void deleteService(
            @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v2"}), required = true) @PathVariable String apiVersion,
            @Parameter(name = "clusterSeq", description = "cluster seq", required = true) @PathVariable Integer clusterSeq,
            @Parameter(name = "namespaceName", description = "namespace name", required = true) @PathVariable String namespaceName,
            @Parameter(name = "serviceName", description = "serviceName", required = true) @PathVariable String serviceName
    ) throws Exception {
        log.debug("[BEGIN] deleteService");

        /**
         * cluster 상태 체크제
         */
        clusterStateService.checkClusterState(clusterSeq);

        ClusterVO cluster = serviceSpecService.setupCluster(clusterSeq, namespaceName);

        ApiVersionType apiVersionType = ApiVersionType.valueOf(StringUtils.upperCase(apiVersion));
        ContextHolder.exeContext().setApiVersionType(apiVersionType);

        K8sServiceVO k8sService = serviceSpecService.getService(cluster, cluster.getNamespaceName(), serviceName, ContextHolder.exeContext());
        if(k8sService != null){
            serviceSpecService.deleteService(cluster, serviceName, null, ContextHolder.exeContext());
        }

        log.debug("[END  ] deleteService");
    }

    /*******************************************************************************************************************************
     * [끝] R3.5.0, clusterSeq, namespace 사용 추가, coolingi, 2019/12/10
     *******************************************************************************************************************************/


    /**
     * Component 복사 (필요한 내용만)
     * @param source
     * @return
     */
    private ComponentVO makeComponent(ComponentVO source) {
        ComponentVO target = new ComponentVO();
        target.setComponentSeq(source.getComponentSeq());
        target.setComponentName(source.getComponentName());
        target.setWorkloadType(source.getWorkloadType());
        target.setSortOrder(source.getSortOrder());
        target.setDryRunYn(source.getDryRunYn());
        target.setComputeTotal(source.getComputeTotal());
        target.setActiveCount(source.getActiveCount());
        target.setWorkloadVersion(source.getWorkloadVersion());

        return target;
    }
}
