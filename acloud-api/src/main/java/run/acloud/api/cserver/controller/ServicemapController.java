package run.acloud.api.cserver.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Size;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import run.acloud.api.configuration.service.ClusterService;
import run.acloud.api.configuration.service.ClusterStateService;
import run.acloud.api.configuration.vo.ClusterVO;
import run.acloud.api.cserver.service.ServerValidService;
import run.acloud.api.cserver.service.ServicemapService;
import run.acloud.api.cserver.vo.*;
import run.acloud.api.resource.service.NamespaceService;
import run.acloud.api.resource.util.ResourceUtil;
import run.acloud.api.resource.vo.K8sDeployYamlVO;
import run.acloud.commons.annotations.InHouse;
import run.acloud.commons.enums.ApiVersionType;
import run.acloud.commons.util.AuthUtils;
import run.acloud.commons.vo.ExecutingContextVO;
import run.acloud.commons.vo.ResultVO;
import run.acloud.framework.context.ContextHolder;
import run.acloud.framework.enums.ExceptionBiz;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author dy79@acornsoft.io
 * Created on 2017. 2. 1.
 */
@Slf4j
@Tag(name = "Servicemap", description = "서비스맵에 대한 관리 기능을 제공한다.")
@RestController
@Validated
@RequestMapping(value = "/api/servicemap")
public class ServicemapController {
	@Autowired
    private ServicemapService servicemapService;

    @Autowired
    private ServerValidService serverValidService;

    @Autowired
    private ClusterService clusterService;

    @Autowired
    private ClusterStateService clusterStateService;

    @Autowired
    private NamespaceService namespaceService;

    @InHouse
    @GetMapping("/summaries")
    @Operation(summary = "서비스맵 요약 정보 목록", description = "서비스맵 요약 정보 목록을 조회한다.")
    public List<ServicemapSummaryAdditionalVO> getServicemapSummaries(
            HttpServletRequest _request,
            @RequestParam List<Integer> serviceSeqs,
            @Parameter(name = "useStorage", description = "storage 정보 사용 여부", schema = @Schema(allowableValues = {"true","false"}, defaultValue = "false")) @RequestParam(value = "useStorage", required = false, defaultValue = "false") boolean useStorage,
            @Parameter(name = "useGateWay", description = "gateWay 정보 사용 여부", schema = @Schema(allowableValues = {"true","false"}, defaultValue = "false")) @RequestParam(value = "useGateWay", required = false, defaultValue = "false") boolean useGateWay,
            @Parameter(name = "useWorkload", description = "workload 정보 사용 여부", schema = @Schema(allowableValues = {"true","false"}, defaultValue = "false")) @RequestParam(value = "useWorkload", required = false, defaultValue = "false") boolean useWorkload,
            @Parameter(name = "useNamespace", description = "namespace 정보 사용 여부", schema = @Schema(allowableValues = {"true","false"}, defaultValue = "false")) @RequestParam(value = "useNamespace", required = false, defaultValue = "false") boolean useNamespace
    ) throws Exception {
    	log.debug("[BEGIN] getServicemapSummaries");

        /**
         * ADMIN이 아니면 Header로 수신되는 인증된 Workspace 값을 사용하여 조회하도록 처리.
         */
        if(!AuthUtils.isNotDevOpsUser(ContextHolder.exeContext())) {
            if(ContextHolder.exeContext().getUserServiceSeq() == null || ContextHolder.exeContext().getUserServiceSeq() < 1) {
                throw new CocktailException("Invalid Workspace!", ExceptionType.InvalidParameter);
            }
    	    serviceSeqs.clear();
            serviceSeqs.add(ContextHolder.exeContext().getUserServiceSeq());
        }

        List<ServicemapSummaryAdditionalVO> result = servicemapService.getServicemapSummaries(serviceSeqs, useStorage, useGateWay, useWorkload, useNamespace);
    	
        log.debug("[END  ] getServicemapSummaries");

        return result;
    }
    
    @PostMapping("/{apiVersion}")
    @Operation(summary = "서비스맵 생성", description = "서비스맵 생성한다.")
    public ServicemapVO addServicemapV2(
            @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v2"})) @PathVariable String apiVersion,
            @Parameter(name = "servicemapAdd", description = "servicemap 생성 모델") @Validated @RequestBody ServicemapAddVO servicemapAdd
    ) throws Exception {

    	log.debug("[BEGIN] addServicemapV2");

        /**
         * header 정보로 요청 사용자 권한 체크, DevOps는 MANAGER만 가능
         */
        if(AuthUtils.isNotDevOpsUser(ContextHolder.exeContext())) {
            AuthUtils.checkAuth(ContextHolder.exeContext(), AuthUtils.checkUserDevOpsBlockAuthPredicate());
        }


        /**
         * cluster 상태 체크
         */
        clusterStateService.checkClusterState(servicemapAdd.getClusterSeq());

        if(StringUtils.isNotBlank(servicemapAdd.getNamespaceName())){
            if(!ResourceUtil.validNamespaceName(servicemapAdd.getNamespaceName())){
                throw new CocktailException("Invalid namespaceName!!", ExceptionType.NamespaceNameInvalid);
            }
        }

        ServicemapVO servicemap = servicemapService.addServicemap(servicemapAdd, ContextHolder.exeContext());

        log.debug("[END  ] addServicemapV2");

        return servicemap;
    }

    @PutMapping("/{apiVersion}/{servicemapSeq}")
    @Operation(summary = "서비스맵 수정", description = "서비스맵 수정한다.")
    public ServicemapModVO updateServicemap(
            @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v2"})) @PathVariable String apiVersion,
            @Parameter(name = "servicemapSeq", description = "servicemapSeq") @PathVariable Integer servicemapSeq,
            @Parameter(name = "servicemap", description = "서비스맵 수정 모델") @RequestBody ServicemapModVO servicemap
    ) throws Exception {

        log.debug("[BEGIN] updateServicemap");

        /**
         * header 정보로 요청 사용자 권한 체크, DevOps는 MANAGER만 가능
         */
        if(AuthUtils.isNotDevOpsUser(ContextHolder.exeContext())) {
            AuthUtils.checkAuth(ContextHolder.exeContext(), AuthUtils.checkUserDevOpsBlockAuthPredicate());
        } else {
            servicemap.setServiceSeq(ContextHolder.exeContext().getUserServiceSeq());
            AuthUtils.checkAuth(ContextHolder.exeContext(), AuthUtils.checkUserDevOpsExcludeManagerBlockAuthPredicate(servicemap.getServiceSeq()));
        }

        /**
         * cluster 상태 체크
         */
        clusterStateService.checkClusterStateByServicemap(servicemapSeq);

        servicemapService.updateServicemap(servicemap, ContextHolder.exeContext());

        log.debug("[END  ] updateServicemap");

        return servicemap;
    }


    @PutMapping(value = "/v2/cluster/{clusterSeq}/namespace/{namespaceName:.+}")
    @Operation(summary = "Cluster > namespace 정보 yaml 수정", description = "Cluster > namespace 정보 yaml로 수정한다.")
    public void udpateNamespaceByYaml(
            @Parameter(name = "clusterSeq", description = "Cluster 일련번호", required = true) @PathVariable Integer clusterSeq,
            @Parameter(name = "namespaceName", description = "namespaceName", required = true) @PathVariable String namespaceName,
            @Parameter(description = "deployYaml", required = true) @RequestBody K8sDeployYamlVO deployYaml
    ) throws Exception {

        log.debug("[BEGIN] udpateNamespaceByYaml");

        /**
         * header 정보로 요청 사용자 권한 체크, DevOps는 MANAGER만 가능
         */
        if(AuthUtils.isNotDevOpsUser(ContextHolder.exeContext())) {
            AuthUtils.checkAuth(ContextHolder.exeContext(), AuthUtils.checkUserDevOpsBlockAuthPredicate());
        } else {
            if(ContextHolder.exeContext().getUserServiceSeq() == null || ContextHolder.exeContext().getUserServiceSeq() < 1) {
                throw new CocktailException("Invalid Workspace!", ExceptionType.InvalidParameter);
            }
            boolean isAccessbled = false;
            ServicemapVO servicemap = servicemapService.getServicemapByClusterAndName(clusterSeq, namespaceName);
            if (servicemap != null && CollectionUtils.isNotEmpty(servicemap.getServicemapMappings())) {
                Optional<ServicemapMappingVO> mappingOptional = servicemap.getServicemapMappings().stream().filter(m -> (m.getServiceSeq().equals(ContextHolder.exeContext().getUserServiceSeq()))).findFirst();
                if (mappingOptional.isPresent()) {
                    AuthUtils.checkAuth(ContextHolder.exeContext(), AuthUtils.checkUserDevOpsExcludeManagerBlockAuthPredicate(ContextHolder.exeContext().getUserServiceSeq()));
                    isAccessbled = true;
                }
            }

            if (!isAccessbled) {
                throw new CocktailException("This call does not have permission for the user.", ExceptionType.NotAuthorizedToResource);
            }

        }

        servicemapService.udpateNamespaceOfServicemapByYaml(clusterSeq, namespaceName, deployYaml);

        log.debug("[END  ] udpateNamespaceByYaml");

    }

//    @PutMapping("/{apiVersion}/cluster/{clusterSeq}/namespace/{namespaceName}")
//    public AppmapVO updateAppmap(
//            @Parameter(name = "clusterSeq", description = "cluster sequence", required = true) @PathVariable Integer clusterSeq,
//            @Parameter(name = "namespaceName", description = "namespaceName", required = true) @PathVariable String namespaceName,
//            @Parameter(name = "appmap", description = "appmap 모델", required = true) @RequestBody AppmapVO appmap
//    ) throws Exception {
//
//        log.debug("[BEGIN] updateAppmap");
//
//        /**
//         * cluster 상태 체크
//         */
//        clusterStateService.checkClusterStateByAppmap(appmapSeq);
//
//        appmapService.updateAppmap(appmap, ContextHolder.exeContext());
//
//        log.debug("[END  ] updateAppmap");
//
//        return appmap;
//    }

    @GetMapping("/{apiVersion}/{servicemapSeq}")
    @Operation(summary = "서비스맵 상세", description = "서비스맵 상세 조회한다.")
    public ServicemapVO getServicemap(
            @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v2"})) @PathVariable String apiVersion,
            @Parameter(name = "servicemapSeq", description = "servicemapSeq") @PathVariable Integer servicemapSeq
    ) throws Exception {
        log.debug("[BEGIN] getServicemap");

        /**
         * cluster 상태 체크
         */
        clusterStateService.checkClusterStateByServicemap(servicemapSeq);

        ServicemapVO servicemap = servicemapService.getServicemapDetail(servicemapSeq, null, ContextHolder.exeContext());

        log.debug("[END  ] getServicemap");

        return servicemap;
    }

    @DeleteMapping("/{servicemapSeq}")
    @Operation(summary = "서비스맵 삭제", description = "서비스맵 삭제한다.")
    public ResultVO removeServicemap(
            @Parameter(name = "servicemapSeq", description = "servicemapSeq", required = true) @PathVariable Integer servicemapSeq,
            @Parameter(name = "cascade", description = "true : servicemap에 관련된 설정 정보(칵테일 설정 정보(DB)와 실제 k8s namespace까지 삭제), false : servicemap에 관련된 설정 정보(칵테일 설정 정보(DB)만 삭제", schema = @Schema(allowableValues = {"true","false"}, defaultValue = "false")) @RequestParam(value = "cascade", required = false, defaultValue = "false") boolean cascade
    ) throws Exception {
    	log.debug("[BEGIN] removeServicemap");

        /**
         * cluster 상태 체크
         */
        if (!cascade) {
            clusterStateService.checkClusterStateByServicemap(servicemapSeq);
        }

        /**
         * header 정보로 요청 사용자 권한 체크, DevOps는 MANAGER만 가능
         */
        if(AuthUtils.isNotDevOpsUser(ContextHolder.exeContext())) {
            AuthUtils.checkAuth(ContextHolder.exeContext(), AuthUtils.checkUserDevOpsBlockAuthPredicate());
        }

        servicemapService.removeServicemap(servicemapSeq, cascade, ContextHolder.exeContext());

        log.debug("[END  ] removeServicemap");

        return new ResultVO();
    }

    @PutMapping("/{servicemapSeq}/rename")
    @Operation(summary = "서비스맵 이름변경", description = "서비스맵의 이름을 변경한다.")
    public ResultVO renameServicemap(
            @RequestHeader(name = "user-id" ) Integer userSeq,
            @PathVariable Integer servicemapSeq,
            @Size(min = 1, max = 50) @RequestParam(name = "name") String name) throws Exception {
    	log.debug("[BEGIN] renameServicemap");

    	if (StringUtils.isBlank(name) || name.length() > 50) {
    	    throw new CocktailException("Appmap name is invalid", ExceptionType.AppmapNameInvalid);
        }
        ExecutingContextVO ctx = new ExecutingContextVO();
        ctx.setUserSeq(userSeq);

        servicemapService.renameServicemap(servicemapSeq, name, ctx);

        log.debug("[END  ] renameServicemap");
        
        return new ResultVO();
    }

    @InHouse
    @GetMapping("/{servicemapSeq}/servername/{serverName}/check")
    @Operation(summary = "Server 이름 사용 여부 확인 Namespace에서 Unique 여부", description = "추가하려는 Server의 이름이 해당 서비스맵에서 이미 사용하고 있는 것인지 검사한다(삭제된 서버의 이름은 사용할 수 있다).")
    public ResultVO isServerNameUsed(
            @PathVariable Integer servicemapSeq,
            @PathVariable String serverName
    ) throws Exception {
        ResultVO r = new ResultVO();
        try {
            r.putKeyValue("exists", serverValidService.checkServerNameIfExistsByServicemapSeq(servicemapSeq, serverName, true, false, null));
        }
        catch (DataAccessException de) {
            throw de; // DB 처리중 Runtime Exception이 발생하면 처리를 공통 ExceptionHandler 로 던지고 공통 ExceptionHandler에서 처리
        }
        catch (CocktailException ce) {
            throw ce; // CocktailException이 발생하면 발생한 예외 처리 케이스를 그대로 사용
        }
        catch (Exception e) {
            throw new CocktailException("Error during data query.", e, ExceptionType.CommonInquireFail, ExceptionBiz.SERVER);
        }
        return r;
    }

    @InHouse
    @GetMapping("/{apiVersion}/cluster/{clusterSeq}/namespace/{namespaceName}/hpa/{hpaName}/check")
    @Operation(summary = "HPA 이름 사용 여부 확인 Namespace에서 Unique 여부", description = "추가하려는 HPA의 이름이 해당 서비스맵에서 이미 사용하고 있는 것인지 검사한다.")
    public ResultVO isHpaNameUsed(
            @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v1"}), required = true) @PathVariable String apiVersion,
            @Parameter(description = "clusterSeq", required = true) @PathVariable Integer clusterSeq,
            @Parameter(description = "namespaceName", required = true) @PathVariable String namespaceName,
            @Parameter(description = "hpaName", required = true) @PathVariable String hpaName
    ) throws Exception {
        ResultVO r = new ResultVO();
        try {
            r.putKeyValue("exists", serverValidService.checkHpaNameIfExists(clusterSeq, namespaceName, hpaName));
        }
        catch (DataAccessException de) {
            throw de; // DB 처리중 Runtime Exception이 발생하면 처리를 공통 ExceptionHandler 로 던지고 공통 ExceptionHandler에서 처리
        }
        catch (CocktailException ce) {
            throw ce; // CocktailException이 발생하면 발생한 예외 처리 케이스를 그대로 사용
        }
        catch (Exception e) {
            throw new CocktailException("Error during data query.", e, ExceptionType.CommonInquireFail, ExceptionBiz.SERVER);
        }
        return r;
    }

    @InHouse
    @PutMapping("/{servicemapSeq}/order")
    @Operation(summary = "서비스맵 순서 수정", description = "서비스맵 순서 수정한다.")
    public ServicemapOrderVO updateServicemapOrder(
            @Parameter(description = "servicemap 번호") @PathVariable Integer servicemapSeq,
            @Parameter(description = "servicemap순서 모델") @RequestBody @Validated ServicemapOrderVO servicemapOrder
    ) throws Exception {
        log.debug("[BEGIN] updateServicemapOrder");

        servicemapService.updateServicemapOrder(servicemapOrder);

        log.debug("[END  ] updateServicemapOrder");

        return servicemapOrder;
    }

    @Deprecated
    @Operation(summary = "Server 이름 사용 여부 확인 Cluster에서 Unique 여부", description = "추가하려는 Server의 이름이 cluster에서 이미 사용하고 있는 것인지 검사한다 (삭제된 서버의 이름은 사용할 수 있다).")
    @GetMapping("/{servicemapSeq}/servername/{serverName}/checkwithincluster")
    public ResultVO isServerNameUsedInCluster(@PathVariable Integer servicemapSeq, @PathVariable String serverName) throws Exception {
        ResultVO r = new ResultVO();
        try {
            // 서버명 중복 체크시 Pod의 Label을 기준으로 체크하는데 K8s Label의 Max Length가 63글자여서 에러 발생. => 예외 처리함. => UI에서는 63글자 넘어가면 호출하지 않는 것으로 처리..
            if (StringUtils.isBlank(serverName) || serverName.length() > 63) {
                throw new CocktailException("Package Workload Name is null or empty or more than 20 characters", ExceptionType.InvalidParameter);
            }

            r.putKeyValue("exists", serverValidService.checkServerNameIfExistsByServicemapSeq(servicemapSeq, serverName, true, false, null));
        }
        catch (DataAccessException de) {
            throw de; // DB 처리중 Runtime Exception이 발생하면 처리를 공통 ExceptionHandler 로 던지고 공통 ExceptionHandler에서 처리
        }
        catch (CocktailException ce) {
            throw ce; // CocktailException이 발생하면 발생한 예외 처리 케이스를 그대로 사용
        }
        catch (Exception e) {
            throw new CocktailException("Error during data query.", e, ExceptionType.CommonInquireFail, ExceptionBiz.SERVER);
        }
        return r;
    }

    @PostMapping("/v2/existnamespace")
    @Operation(summary = "존재하는 Namespace를 서비스맵으로 생성", description = "존재하는 Namespace를 서비스맵으로 생성한다.")
    public ServicemapAddVO addExistNamespace(
        @RequestHeader(name = "user-id" ) Integer userSeq,
        @Parameter(name = "servicemapAdd", description = "servicemap 생성 모델") @Validated @RequestBody ServicemapAddVO servicemapAdd
    ) throws Exception {
        log.debug("[BEGIN] addExistNamespace");

        /**
         * header 정보로 요청 사용자 권한 체크, DevOps는 MANAGER만 가능
         */
        if(AuthUtils.isNotDevOpsUser(ContextHolder.exeContext())) {
            AuthUtils.checkAuth(ContextHolder.exeContext(), AuthUtils.checkUserDevOpsBlockAuthPredicate());
        } else {
            AuthUtils.checkAuth(ContextHolder.exeContext(), AuthUtils.checkUserDevOpsExcludeManagerBlockAuthPredicate(servicemapAdd.getServiceSeq()));
        }

        /**
         * cluster 상태 체크
         */
        clusterStateService.checkClusterState(servicemapAdd.getClusterSeq());

        ExecutingContextVO ctx = new ExecutingContextVO();
        ctx.setUserSeq(userSeq);
        ctx.setApiVersionType(ApiVersionType.V2);

        servicemapService.addExistServicemap(servicemapAdd, null, ctx);

        log.debug("[END  ] addExistNamespace");

        return servicemapAdd;
    }

    @GetMapping("/v2/{servicemapSeq}/workloads")
    @Operation(summary = "서비스맵 > 워크로드 상태 목록", description = "서비스맵 > 워크로드 상태 목록 조회한다.")
    public ServerStateVO getWorkloadsInNamespace(
        @PathVariable Integer servicemapSeq,
        @Parameter(name = "useExistingWorkload", description = "클러스터에 실제 리소스가 존재하는 경우만 응답", schema = @Schema(allowableValues = {"true","false"}, defaultValue = "false")) @RequestParam(value = "useExistingWorkload", required = false, defaultValue = "false") boolean useExistingWorkload,
        @Parameter(name = "usePodEventInfo", description = "Pod event 정보 사용여부", schema = @Schema(allowableValues = {"true","false"}, defaultValue = "false")) @RequestParam(value = "usePodEventInfo", required = false, defaultValue = "false") boolean usePodEventInfo
    ) throws Exception {
        log.debug("[BEGIN] getWorkloadsInNamespace");

        /**
         * cluster 상태 체크
         */
        clusterStateService.checkClusterStateByServicemap(servicemapSeq);

        ExecutingContextVO ctx = new ExecutingContextVO();
        ctx.setUserSeq(ContextHolder.exeContext().getUserSeq());
        ctx.setApiVersionType(ApiVersionType.V2);

        ServerStateVO serverStates = servicemapService.getWorkloadsInNamespace(servicemapSeq, null, true, useExistingWorkload, usePodEventInfo, ctx);

        log.debug("[END  ] getWorkloadsInNamespace");

        return serverStates;
    }

    @GetMapping("/v2/{servicemapSeq}/workload/{workloadName}")
    @Operation(summary = "서비스맵 > 워크로드 상태 조회", description = "서비스맵 > 워크로드 상태 조회한다.")
    public ServerStateVO getWorkloadState(
        @PathVariable Integer servicemapSeq,
        @PathVariable String workloadName
    ) throws Exception {
        log.debug("[BEGIN] getWorkloadState");

        /**
         * cluster 상태 체크
         */
        clusterStateService.checkClusterStateByServicemap(servicemapSeq);

        ExecutingContextVO ctx = new ExecutingContextVO();
        ctx.setUserSeq(ContextHolder.exeContext().getUserSeq());
        ctx.setApiVersionType(ApiVersionType.V2);

        ServerStateVO serverStates = servicemapService.getWorkloadsInNamespace(servicemapSeq, workloadName, ctx);

        log.debug("[END  ] getWorkloadState");

        return serverStates;
    }

    @InHouse
    @GetMapping("/{apiVersion}/cluster/{clusterSeq}/namespace/{namespaceName}/resource/exists")
    @Operation(summary = "Namespace > 기본 resource가 생성되어 있는지 여부를 반환.(LimitRange, ResourceQuota, NetworkPolicy)")
    public Map<String, Boolean> getNamespacedDefaultResourceExists(
            @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v1"}), required = true) @PathVariable String apiVersion,
            @Parameter(description = "clusterSeq", required = true) @PathVariable Integer clusterSeq,
            @Parameter(description = "namespaceName", required = true) @PathVariable String namespaceName
    ) throws Exception {
        log.debug("[BEGIN] getNamespacedDefaultResourceExists");

        ClusterVO cluster = clusterService.getCluster(clusterSeq);

        /**
         * cluster 상태 체크
         */
        clusterStateService.checkClusterState(cluster);

        Map<String, Boolean> resourceExists = servicemapService.getNamespacedDefaultResourceExists(cluster, namespaceName);

        log.debug("[END  ] getNamespacedDefaultResourceExists");

        return resourceExists;
    }

//    @Operation(summary = "앱맵의 k8s namespace 이름을 반환한다.", notes="monitoring에서 사용하는 api")
//    @GetMapping("/v2/{appmapSeq}/namespace")
//    public ResultVO getNamespaceOfAppmap(@PathVariable Integer appmapSeq) throws Exception {
//        ResultVO r = new ResultVO();
//        r.setResult(this.appmapService.getNamespaceName(appmapSeq));
//        return r;
//    }

//    @Operation(summary = "앱맵의 server(k8s deployment)들의 이름을 반환한다.", notes="monitoring에서 사용하는 api")
//    @GetMapping("/v2/{appmapSeq}/server-names")
//    public ResultVO getDeploymentNamesInAppmap(@PathVariable Integer appmapSeq) throws Exception {
//        ResultVO r = new ResultVO();
//        r.setResult(this.appmapService.getDeploymentNamesInAppmap(appmapSeq));
//        return r;
//    }

//    @Operation(summary = "앱맵 server(k8s deployment) 전체의 resource limit/request를 반환한다.", notes="monitoring에서 사용하는 api")
//    @GetMapping("/v2/{appmapSeq}/resource")
//    public ResultVO getResource(@PathVariable Integer appmapSeq) throws Exception {
//        ResultVO result = new ResultVO();
//        List<Double> quotas = this.resourceService.getResourceQuotaOfNamespace(appmapSeq);
//        Map<String, Double> r = new HashMap<>();
//        r.put("limit-cpu", quotas.get(0));
//        r.put("request-cpu", quotas.get(2));
//        r.put("limit-memory", quotas.get(1));
//        r.put("request-memory", quotas.get(3));
//        result.setResult(r);
//        return result;
//    }

//    @Operation(summary = "앱맵 (k8s namespace) 전체의 pod 이름 목록을 반환한다.", notes="monitoring에서 사용하는 api")
//    @GetMapping("/v2/{appmapSeq}/pods")
//    public ResultVO getPodListOfAppmap(@PathVariable Integer appmapSeq) throws Exception {
//        ResultVO result = new ResultVO();
//        result.setResult(this.resourceService.getPodListOfAppmap(appmapSeq));
//        return result;
//    }

//    @Operation(summary = "앱맵 (k8s namespace) 전체의 pod 이름 목록을 서버의 이름과 묶어 반환한다.", notes="monitoring에서 사용하는 api")
//    @GetMapping("/v2/{appmapSeq}/server-pods")
//    public ResultVO getServerPodsOfAppmap(@PathVariable Integer appmapSeq) throws Exception {
//        ResultVO result = new ResultVO();
//        result.setResult(this.resourceService.getServerPodsOfAppmap(appmapSeq));
//        return result;
//    }
}
