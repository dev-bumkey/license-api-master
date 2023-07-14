package run.acloud.api.cserver.controller;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import run.acloud.api.configuration.service.ClusterService;
import run.acloud.api.configuration.service.ClusterStateService;
import run.acloud.api.configuration.vo.ClusterVO;
import run.acloud.api.cserver.service.ServerService;
import run.acloud.api.cserver.service.ServerStateService;
import run.acloud.api.cserver.vo.ServerStateVO;
import run.acloud.api.cserver.vo.SpecFileDeployVO;
import run.acloud.api.cserver.vo.WorkloadVO;
import run.acloud.api.resource.constants.KubeConstants;
import run.acloud.api.resource.enums.K8sApiKindType;
import run.acloud.api.resource.enums.TaintEffects;
import run.acloud.api.resource.service.*;
import run.acloud.api.resource.vo.*;
import run.acloud.commons.enums.ApiVersionType;
import run.acloud.commons.vo.ExecutingContextVO;
import run.acloud.framework.context.ContextHolder;
import run.acloud.framework.enums.ExceptionBiz;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;
import run.acloud.framework.util.ExceptionMessageUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author wschoi@acornsoft.io
 * Created on 2017. 6. 1.
 */
@Tag(name = "Cube Resource", description = "Cube Server에 대한 Kubernetes 정보(Description)를 제공한다")
@Slf4j
@RestController
@RequestMapping(value = "/api/cube")
public class CubeController {

    @Autowired
    private ClusterService clusterService;

    @Autowired
    private ClusterStateService clusterStateService;

    @Autowired
    private K8sResourceService k8sResourceService;

    @Autowired
    private ServerService serverService;

    @Autowired
    private ServerStateService serverStateService;

    @Autowired
    private PersistentVolumeService persistentVolumeService;

    @Autowired
    private StorageClassService storageClassService;

    @Autowired
    private IngressSpecService ingressSpecService;

    @Autowired
    private ServiceSpecService serviceSpecService;

    @Autowired
    private NamespaceService namespaceService;

    @Autowired
    private WorkloadResourceService workloadResourceService;

    @DeleteMapping("/cluster/{clusterSeq}/namespace/{namespaceName}/pod/{podName}")
    @Operation(summary = "Pod 삭제", description = "지정한 Pod를 삭제한다.")
    public void deletePod(
            @Parameter(name = "clusterSeq", description = "cluster sequence", required = true) @PathVariable Integer clusterSeq,
            @Parameter(name = "namespaceName", description = "namespaceName", required = true) @PathVariable String namespaceName,
            @Parameter(name = "podName", description = "podName", required = true) @PathVariable String podName,
            @Parameter(name = "gracePeriodSeconds", description = "The duration in seconds before the object should be deleted.(unit: second)") @RequestParam(required = false) Integer gracePeriodSeconds,
            @Parameter(name = "force", description = "delete immediately", schema = @Schema(allowableValues = {"true","false"}, defaultValue = "false")) @RequestParam(required = false, defaultValue = "false") boolean force
    ) throws Exception {
        log.debug("[BEGIN] deletePod");

        workloadResourceService.deleteNamespacedPod(clusterSeq, namespaceName, podName, gracePeriodSeconds, force);

        log.debug("[END  ] deletePod");

    }

    @GetMapping("/v2/cluster/{clusterSeq}/namespace/{namespaceName}/ingresses")
    @Operation(summary = "Namespace > Ingress 목록", description = "Namespace > Ingress 목록을 반환한다.")
    public List<K8sIngressVO> getIngressInNamespace(
        @Parameter(name = "clusterSeq", description = "cluster sequence") @PathVariable Integer clusterSeq,
        @Parameter(name = "namespaceName", description = "namespaceName") @PathVariable String namespaceName
    ) throws Exception {
        log.debug("[BEGIN] getIngressInNamespace");

        ExecutingContextVO ctx = ContextHolder.exeContext();
        ctx.setApiVersionType(ApiVersionType.V2);

        List<K8sIngressVO> ingresses = ingressSpecService.getIngresses(clusterSeq, namespaceName, ctx);

        log.debug("[END  ] getIngressInNamespace");

        return ingresses;
    }

    @GetMapping("/v2/cluster/{clusterSeq}/namespace/{namespaceName}/services")
    @Operation(summary = "Namespace > Service 목록", description = "Namespace > Service 목록을 반환한다.")
    public List<K8sServiceVO> getServicesInNamespace(
        @Parameter(name = "clusterSeq", description = "cluster sequence") @PathVariable Integer clusterSeq,
        @Parameter(name = "namespaceName", description = "namespaceName") @PathVariable String namespaceName
    ) throws Exception {
        log.debug("[BEGIN] getServicesInNamespace");

        ExecutingContextVO ctx = ContextHolder.exeContext();
        ctx.setApiVersionType(ApiVersionType.V2);

        List<K8sServiceVO> services = serviceSpecService.getServices(clusterSeq, namespaceName, ctx);

        log.debug("[END  ] getServicesInNamespace");

        return services;
    }

    @GetMapping("/v2/cluster/{clusterSeq}/namespace/{namespaceName}/persistentvolumeclaims")
    @Operation(summary = "Namespace > PersistentVolumeClaim 목록", description = "Namespace > PersistentVolumeClaim 목록을 반환한다.")
    public List<K8sPersistentVolumeClaimVO> getPersistentVolumeClaimsInNamespace(
        @Parameter(name = "clusterSeq", description = "cluster sequence") @PathVariable Integer clusterSeq,
        @Parameter(name = "namespaceName", description = "namespaceName") @PathVariable String namespaceName
    ) throws Exception {
        log.debug("[BEGIN] getPersistentVolumeClaimsInNamespace");

        ExecutingContextVO ctx = ContextHolder.exeContext();
        ctx.setApiVersionType(ApiVersionType.V2);

        List<K8sPersistentVolumeClaimVO> persistentVolumeClaims = persistentVolumeService.getPersistentVolumeClaims(clusterSeq, namespaceName, ctx);

        log.debug("[END  ] getPersistentVolumeClaimsInNamespace");

        return persistentVolumeClaims;
    }

    @GetMapping("/v2/cluster/{clusterSeq}/namespace/{namespaceName}/persistentvolumeclaim/{pvcName:.+}")
    @Operation(summary = "Namespace > PersistentVolumeClaim 정보", description = "Namespace > PersistentVolumeClaim 정보를 반환한다.")
    public K8sPersistentVolumeClaimVO getPersistentVolumeClaimInNamespace(
        @Parameter(name = "clusterSeq", description = "cluster sequence") @PathVariable Integer clusterSeq,
        @Parameter(name = "namespaceName", description = "namespaceName") @PathVariable String namespaceName,
        @Parameter(name = "pvcName", description = "pvcName") @PathVariable String pvcName
    ) throws Exception {
        log.debug("[BEGIN] getPersistentVolumeClaimInNamespace");

        if (StringUtils.isBlank(pvcName)) {
            throw new CocktailException("pvcName is required.", ExceptionType.InvalidParameter);
        }

        ExecutingContextVO ctx = ContextHolder.exeContext();
        ctx.setApiVersionType(ApiVersionType.V2);

        K8sPersistentVolumeClaimVO persistentVolumeClaim = persistentVolumeService.getPersistentVolumeClaimDetail(clusterSeq, namespaceName, pvcName, ctx);

        log.debug("[END  ] getPersistentVolumeClaimInNamespace");

        return persistentVolumeClaim;

    }

    @GetMapping("/v2/cluster/{clusterSeq}/persistentvolumes/{persistentVolumeName:.+}")
    @Operation(summary = "PersistentVolume 상세", description = "PersistentVolume 상세를 반환한다.")
    public K8sPersistentVolumeVO getPersistentVolumeByCluster(
        @Parameter(name = "clusterSeq", description = "cluster sequence") @PathVariable Integer clusterSeq,
        @Parameter(name = "persistentVolumeName", description = "조회하려는 PersistentVolume 명") @PathVariable String persistentVolumeName
    ) throws Exception {
        log.debug("[BEGIN] getPersistentVolumeByCluster");

        ExecutingContextVO ctx = ContextHolder.exeContext();
        ctx.setApiVersionType(ApiVersionType.V2);

        ClusterVO cluster = clusterService.getCluster(clusterSeq);
        String field = String.format("%s.%s=%s", KubeConstants.META, KubeConstants.NAME, persistentVolumeName);
        Map<String, K8sPersistentVolumeVO> persistentVolumesMap = persistentVolumeService.convertPersistentVolumeDataMap(cluster, null, field, null);

        K8sPersistentVolumeVO persistentVolume = null;
        if(MapUtils.isNotEmpty(persistentVolumesMap)) {
            persistentVolume = persistentVolumesMap.get(persistentVolumeName);
        }

        if(persistentVolume == null) {
            throw new CocktailException("PV Not Found", ExceptionType.K8sVolumeNotFound);
        }

        log.debug("[END  ] getPersistentVolumeByCluster");

        return persistentVolume;
    }

    @GetMapping("/v2/cluster/{clusterSeq}/storageclasses/{storageClassName:.+}")
    @Operation(summary = "StorageClass 상세", description = "StorageClass 상세를 반환한다.")
    public K8sStorageClassVO getStorageClassByCluster(
        @Parameter(name = "clusterSeq", description = "cluster sequence") @PathVariable Integer clusterSeq,
        @Parameter(name = "storageClassName", description = "조회하려는 StorageClass 명") @PathVariable String storageClassName
    ) throws Exception {
        log.debug("[BEGIN] getStorageClassByCluster");

        ExecutingContextVO ctx = ContextHolder.exeContext();
        ctx.setApiVersionType(ApiVersionType.V2);

        K8sStorageClassVO storageClass = storageClassService.getStorageClass(clusterSeq, storageClassName, ctx);

        log.debug("[END  ] getStorageClassByCluster");

        return storageClass;
    }

    @GetMapping("/v2/cluster/{clusterSeq}/namespace/{namespaceName}/deployments")
    @Operation(summary = "Namespace > Deployment 목록", description = "Namespace > Deployment 목록을 반환한다.")
    public List<K8sDeploymentVO> getDeploymentsInNamespace(
        @Parameter(name = "clusterSeq", description = "cluster sequence") @PathVariable Integer clusterSeq,
        @Parameter(name = "namespaceName", description = "namespaceName") @PathVariable String namespaceName
    ) throws Exception {
        log.debug("[BEGIN] getDeploymentsInNamespace");

        ExecutingContextVO ctx = ContextHolder.exeContext();
        ctx.setApiVersionType(ApiVersionType.V2);

        List<K8sDeploymentVO> deployments = workloadResourceService.getDeploymentsByCluster(clusterSeq, namespaceName, ctx);

        log.debug("[END  ] getDeploymentsInNamespace");

        return deployments;
    }

    @GetMapping("/v2/cluster/{clusterSeq}/namespace/{namespaceName}/statefulsets")
    @Operation(summary = "Namespace > StatefulSet 목록", description = "Namespace > StatefulSet 목록을 반환한다.")
    public List<K8sStatefulSetVO> getStatefulSetsInNamespace(
        @Parameter(name = "clusterSeq", description = "cluster sequence") @PathVariable Integer clusterSeq,
        @Parameter(name = "namespaceName", description = "namespaceName") @PathVariable String namespaceName
    ) throws Exception {
        log.debug("[BEGIN] getStatefulSetsInNamespace");

        ExecutingContextVO ctx = ContextHolder.exeContext();
        ctx.setApiVersionType(ApiVersionType.V2);

        List<K8sStatefulSetVO> statefulSets = workloadResourceService.getStatefulSetsByCluster(clusterSeq, namespaceName, ctx);

        log.debug("[END  ] getStatefulSetsInNamespace");

        return statefulSets;
    }

    @GetMapping("/v2/cluster/{clusterSeq}/namespace/{namespaceName}/daemonsets")
    @Operation(summary = "Namespace > DaemonSet 목록", description = "Namespace > DaemonSet 목록을 반환한다.")
    public List<K8sDaemonSetVO> getDaemonSetsInNamespace(
        @Parameter(name = "clusterSeq", description = "cluster sequence") @PathVariable Integer clusterSeq,
        @Parameter(name = "namespaceName", description = "namespaceName") @PathVariable String namespaceName
    ) throws Exception {
        log.debug("[BEGIN] getDaemonSetsInNamespace");

        ExecutingContextVO ctx = ContextHolder.exeContext();
        ctx.setApiVersionType(ApiVersionType.V2);

        List<K8sDaemonSetVO> daemonSets = workloadResourceService.getDaemonSetsByCluster(clusterSeq, namespaceName, ctx);

        log.debug("[END  ] getDaemonSetsInNamespace");

        return daemonSets;
    }

    @GetMapping("/v2/cluster/{clusterSeq}/namespace/{namespaceName}/jobs")
    @Operation(summary = "Namespace > Job 목록", description = "Namespace > Job 목록을 반환한다.")
    public List<K8sJobVO> getJobsInNamespace(
        @Parameter(name = "clusterSeq", description = "cluster sequence") @PathVariable Integer clusterSeq,
        @Parameter(name = "namespaceName", description = "namespaceName") @PathVariable String namespaceName
    ) throws Exception {
        log.debug("[BEGIN] getJobsInNamespace");

        ExecutingContextVO ctx = ContextHolder.exeContext();
        ctx.setApiVersionType(ApiVersionType.V2);

        List<K8sJobVO> jobs = workloadResourceService.getJobsByCluster(clusterSeq, namespaceName, ctx);

        log.debug("[END  ] getJobsInNamespace");

        return jobs;
    }

    @GetMapping("/v2/cluster/{clusterSeq}/namespace/{namespaceName}/cronjobs")
    @Operation(summary = "Namespace > CronJob 목록", description = "Namespace > CronJob 목록을 반환한다.")
    public List<K8sCronJobVO> getCronJobsInNamespace(
        @Parameter(name = "clusterSeq", description = "cluster sequence") @PathVariable Integer clusterSeq,
        @Parameter(name = "namespaceName", description = "namespaceName") @PathVariable String namespaceName
    ) throws Exception {
        log.debug("[BEGIN] getCronJobsInNamespace");

        ExecutingContextVO ctx = ContextHolder.exeContext();
        ctx.setApiVersionType(ApiVersionType.V2);

        List<K8sCronJobVO> cronJobs = workloadResourceService.getCronJobsByCluster(clusterSeq, namespaceName, ctx);

        log.debug("[END  ] getCronJobsInNamespace");

        return cronJobs;
    }

    @GetMapping("/v2/cluster/{clusterSeq}/namespace/{namespaceName}/replicasets")
    @Operation(summary = "Namespace > ReplicaSet 목록", description = "Namespace > ReplicaSet 목록을 반환한다.")
    public List<K8sReplicaSetVO> getReplicaSetsInNamespace(
        @Parameter(name = "clusterSeq", description = "cluster sequence") @PathVariable Integer clusterSeq,
        @Parameter(name = "namespaceName", description = "namespaceName") @PathVariable String namespaceName
    ) throws Exception {
        log.debug("[BEGIN] getReplicaSetsInNamespace");

        ExecutingContextVO ctx = ContextHolder.exeContext();
        ctx.setApiVersionType(ApiVersionType.V2);

        List<K8sReplicaSetVO> replicaSets = workloadResourceService.getReplicaSetsByCluster(clusterSeq, namespaceName, ctx);

        log.debug("[END  ] getReplicaSetsInNamespace");

        return replicaSets;
    }

    @GetMapping("/v2/cluster/{clusterSeq}/nodes")
    @Operation(summary = "Node 목록", description = "Node 목록을 반환한다.")
    public List<K8sNodeVO> getNodes(
            @Parameter(name = "clusterSeq", description = "cluster sequence") @PathVariable Integer clusterSeq,
            @Parameter(name = "usePod", description = "Pod 정보 사용 여부", schema = @Schema(allowableValues = {"true","false"}, defaultValue = "false"))
                @RequestParam(value = "usePod", required = false, defaultValue = "false") boolean usePod,
            @Parameter(name = "gpuOnly", description = "Gpu 노드만 조회", schema = @Schema(allowableValues = {"true","false"}, defaultValue = "false"))
                @RequestParam(value = "gpuOnly", required = false, defaultValue = "false") boolean gpuOnly
    ) throws Exception {
        List<K8sNodeVO> nodes;
        try {
            log.debug("[BEGIN] getNodes");

            /**
             * cluster 상태 체크
             */
            clusterStateService.checkClusterState(clusterSeq);

            ExecutingContextVO ctx = ContextHolder.exeContext();
            ctx.setApiVersionType(ApiVersionType.V2);

            nodes = k8sResourceService.getNodes(clusterSeq, BooleanUtils.toBooleanDefaultIfNull(usePod, false), BooleanUtils.toBooleanDefaultIfNull(gpuOnly, false), ctx);

            log.debug("[END  ] getNodes");
        }
        catch (DataAccessException de) {
            throw de; // DB 처리중 Runtime Exception이 발생하면 처리를 공통 ExceptionHandler 로 던지고 공통 ExceptionHandler에서 처리
        }
        catch (CocktailException ce) {
            throw ce; // CocktailException이 발생하면 발생한 예외 처리 케이스를 그대로 사용
        }
        catch (Exception e) {
            throw new CocktailException(ExceptionType.CommonInquireFail.getExceptionPolicy().getMessage(), e, ExceptionType.CommonInquireFail, ExceptionBiz.NODE);
        }

        return nodes;
    }

    @GetMapping("/v2/taint/effects")
    @Operation(summary = "Taint에 유효한 Effect 목록 조회", description = "Taint에 유효한 Effect 목록 조회")
    public List<String> getTaintEffects() throws Exception {
        try {
            return TaintEffects.getTaintEffectList();
        }
        catch (Exception e) {
            throw new CocktailException(ExceptionType.CommonInquireFail.getExceptionPolicy().getMessage(), e, ExceptionType.CommonInquireFail, ExceptionBiz.NODE);
        }
    }

    @GetMapping("/v2/cluster/{clusterSeq}/node/{nodeName:.+}")
    @Operation(summary = "Node 정보 조회", description = "Node 상세 정보를 반환.")
    public K8sNodeVO getNode(
        @Parameter(name = "clusterSeq", description = "cluster sequence") @PathVariable Integer clusterSeq,
        @Parameter(name = "nodeName", description = "nodeName") @PathVariable String nodeName,
        @Parameter(name = "usePod", description = "Pod 정보 사용 여부", schema = @Schema(allowableValues = {"true","false"}, defaultValue = "false"))
            @RequestParam(value = "usePod", required = false, defaultValue = "false") boolean usePod
    ) throws Exception {
        K8sNodeVO node;
        try {
            log.debug("[BEGIN] getNode");

            /**
             * cluster 상태 체크
             */
            clusterStateService.checkClusterState(clusterSeq);

            ExecutingContextVO ctx = ContextHolder.exeContext();
            ctx.setApiVersionType(ApiVersionType.V2);

            node = k8sResourceService.getNode(clusterSeq, nodeName, BooleanUtils.toBooleanDefaultIfNull(usePod, false), ctx);

            log.debug("[END  ] getNode");
        }
        catch (DataAccessException de) {
            throw de; // DB 처리중 Runtime Exception이 발생하면 처리를 공통 ExceptionHandler 로 던지고 공통 ExceptionHandler에서 처리
        }
        catch (CocktailException ce) {
            throw ce; // CocktailException이 발생하면 발생한 예외 처리 케이스를 그대로 사용
        }
        catch (Exception e) {
            throw new CocktailException(ExceptionType.CommonInquireFail.getExceptionPolicy().getMessage(), e, ExceptionType.CommonInquireFail, ExceptionBiz.NODE);
        }

        return node;
    }

    @PutMapping("/v2/cluster/{clusterSeq}/node/{nodeName:.+}")
    @Operation(summary = "Node 설정", description = "Node의 Label, Annotation, Taint 정보 설정 API")
    public K8sNodeDetailVO patchNode(
        @Parameter(name = "clusterSeq", description = "cluster sequence") @PathVariable Integer clusterSeq,
        @Parameter(name = "nodeName", description = "nodeName") @PathVariable String nodeName,
        @Parameter(name = "node", description = "수정하려는 Node정보") @RequestBody K8sNodeDetailVO node) throws Exception {
        try {
            log.debug("[BEGIN] patchNode");

            /**
             * cluster 상태 체크
             */
            clusterStateService.checkClusterState(clusterSeq);

            ExecutingContextVO ctx = ContextHolder.exeContext();
            ctx.setApiVersionType(ApiVersionType.V2);

            node = k8sResourceService.patchNode(clusterSeq, nodeName, node);

            log.debug("[END  ] patchNode");
        }
        catch (DataAccessException de) {
            throw de; // DB 처리중 Runtime Exception이 발생하면 처리를 공통 ExceptionHandler 로 던지고 공통 ExceptionHandler에서 처리
        }
        catch (CocktailException ce) {
            throw ce; // CocktailException이 발생하면 발생한 예외 처리 케이스를 그대로 사용
        }
        catch (Exception e) {
            throw new CocktailException(ExceptionType.CommonInquireFail.getExceptionPolicy().getMessage(), e, ExceptionType.CommonInquireFail, ExceptionBiz.NODE);
        }

        return node;
    }

    @GetMapping("/v2/cluster/{clusterSeq}/node/{nodeName1}/event")
    @Operation(summary = "Node Evnet 목록", description = "Node Event 목록을 반환한다.")
    public List<K8sEventVO> getNodeEvents(
        @Parameter(name = "clusterSeq", description = "cluster sequence") @PathVariable Integer clusterSeq,
        @Parameter(name = "nodeName1", description = "node name") @PathVariable String nodeName1
    ) throws Exception {
        log.debug("[BEGIN] getNodeEvents");

        ExecutingContextVO ctx = ContextHolder.exeContext();
        ctx.setApiVersionType(ApiVersionType.V2);

        String fieldSelector = String.format("%s=%s,%s=%s", "involvedObject.name", nodeName1, "involvedObject.kind", "Node");
        List<K8sEventVO> replicaSetEvents = k8sResourceService.getEventByCluster(clusterSeq, null, fieldSelector, null, ctx);

        log.debug("[END  ] getNodeEvents");

        return replicaSetEvents;
    }

    @GetMapping("/v2/cluster/{clusterSeq}/node/{nodeName1}/pods")
    @Operation(summary = "Node > pod 목록", description = "Node > pod 목록을 반환한다.")
    public List<K8sPodVO> getPodsInNode(
        @Parameter(name = "clusterSeq", description = "cluster sequence") @PathVariable Integer clusterSeq,
        @Parameter(name = "nodeName1", description = "node name") @PathVariable String nodeName1
    ) throws Exception {
        log.debug("[BEGIN] getPodsInNode");

        ExecutingContextVO ctx = ContextHolder.exeContext();
        ctx.setApiVersionType(ApiVersionType.V2);

        Integer serviceSeq = null;
        List<K8sPodVO> pods = workloadResourceService.getPods(clusterSeq, nodeName1, serviceSeq, ctx);

        log.debug("[END  ] getPodsInNode");

        return pods;
    }

    @GetMapping("/v2/cluster/{clusterSeq}/namespaces")
    @Operation(summary = "Namespace 목록", description = "Namespace 목록을 반환한다.")
    public List<K8sNamespaceVO> getNamespaces(
        @Parameter(name = "clusterSeq", description = "cluster sequence") @PathVariable Integer clusterSeq,
        @Parameter(name = "includeSystem", description = "System Namespace 포함 여부", schema = @Schema(allowableValues = {"true","false"}, defaultValue = "true")) @RequestParam(value = "includeSystem", defaultValue = "true", required = false) Boolean includeSystem,
        @Parameter(name = "includeManaged", description = "Acloud에서 관리되는 네입스페이스 포함 여부", schema = @Schema(allowableValues = {"true","false"}, defaultValue = "true")) @RequestParam(value = "includeManaged", defaultValue = "true", required = false) Boolean includeManaged,
        @Parameter(name = "useOnlyName", description = "namespace name만 사용", schema = @Schema(allowableValues = {"true","false"}, defaultValue = "false")) @RequestParam(value = "useOnlyName", defaultValue = "false", required = false) Boolean useOnlyName
    ) throws Exception {
        log.debug("[BEGIN] getNamespaces");

        ExecutingContextVO ctx = ContextHolder.exeContext();
        ctx.setApiVersionType(ApiVersionType.V2);

        List<K8sNamespaceVO> namespaces = namespaceService.getNamespaces(clusterSeq, null, null, BooleanUtils.toBooleanDefaultIfNull(includeSystem, true), BooleanUtils.toBooleanDefaultIfNull(includeManaged, true), BooleanUtils.toBooleanDefaultIfNull(useOnlyName, false), ctx);

        log.debug("[END  ] getNamespaces");

        if (CollectionUtils.isNotEmpty(namespaces)) {
            return namespaces;
        }
        else {
            return null;
        }
    }

    @GetMapping("/v2/cluster/{clusterSeq}/namespacenames")
    @Operation(summary = "Namespace 목록", description = "Namespace 이름 목록을 반환한다.")
    public List<String> getNamespaceNames(
        @Parameter(name = "clusterSeq", description = "cluster sequence") @PathVariable Integer clusterSeq
    ) throws Exception {
        log.debug("[BEGIN] getNamespaceNames");

        ExecutingContextVO ctx = ContextHolder.exeContext();
        ctx.setApiVersionType(ApiVersionType.V2);

        List<K8sNamespaceVO> namespaces = namespaceService.getNamespaces(clusterSeq, null, null, true, true, false, ctx);

        log.debug("[END  ] getNamespaceNames");

        if (CollectionUtils.isNotEmpty(namespaces)) {
            return namespaces.stream().map(ns -> (ns.getName())).collect(Collectors.toList());
        }
        else {
            return null;
        }
    }

    @GetMapping("/v2/cluster/{clusterSeq}/namespace/{namespaceName:.+}")
    @Operation(summary = "Namespace 상세", description = "Namespace를 반환한다.")
    public K8sNamespaceVO getNamespace(
        @Parameter(name = "clusterSeq", description = "cluster sequence") @PathVariable Integer clusterSeq,
        @Parameter(name = "namespaceName", description = "namespaceName") @PathVariable String namespaceName
    ) throws Exception {
        log.debug("[BEGIN] getNamespace");

        ExecutingContextVO ctx = ContextHolder.exeContext();
        ctx.setApiVersionType(ApiVersionType.V2);

        String fieldSelector = String.format("%s.%s=%s", KubeConstants.META, KubeConstants.NAME, namespaceName);
        List<K8sNamespaceVO> namespaces = namespaceService.getNamespaces(clusterSeq, fieldSelector, null, true, true, ctx);

        log.debug("[END  ] getNamespace");

        if (CollectionUtils.isNotEmpty(namespaces)) {
            return namespaces.get(0);
        }
        else {
            return null;
        }
    }

    @PutMapping("/v2/cluster/{clusterSeq}/namespace/{namespaceName:.+}")
    @Operation(summary = "Namespace 수정", description = "Namespace를 수정한다.")
    public K8sNamespaceVO patchNamespace(
        @Parameter(name = "clusterSeq", description = "cluster sequence") @PathVariable Integer clusterSeq,
        @Parameter(name = "namespaceName", description = "namespaceName") @PathVariable String namespaceName,
        @Parameter(name = "namespace", description = "namespace 모델") @RequestBody K8sNamespaceVO namespace
    ) throws Exception {
        log.debug("[BEGIN] patchNamespace");

        ExecutingContextVO ctx = ContextHolder.exeContext();
        ctx.setApiVersionType(ApiVersionType.V2);

        K8sNamespaceVO result = namespaceService.patchNamespace(clusterSeq, namespace.getName(), namespace.getLabels(), namespace.getAnnotations(), ctx);

        log.debug("[END  ] patchNamespace");

        return result;
    }

    @GetMapping("/v2/cluster/{clusterSeq}/namespace/{namespaceName}/pods")
    @Operation(summary = "Namespace > pod 목록", description = "Namespace > pod 목록을 반환한다.")
    public List<K8sPodVO> getPodsInNamespace(
        @Parameter(name = "clusterSeq", description = "cluster sequence") @PathVariable Integer clusterSeq,
        @Parameter(name = "namespaceName", description = "namespaceName") @PathVariable String namespaceName,
        @Parameter(name = "podTemplateHash", description = "Pod의 Template Hash Label 값") @RequestParam(value = "podTemplateHash", required = false) String podTemplateHash
    ) throws Exception {
        log.debug("[BEGIN] getPodsInNamespace");

        ExecutingContextVO ctx = ContextHolder.exeContext();
        ctx.setApiVersionType(ApiVersionType.V2);

        List<K8sPodVO> pods;
        if (StringUtils.isNotBlank(podTemplateHash)) {
            String labels = String.format("%s=%s", KubeConstants.LABELS_POD_TEMPLATE_HASH_KEY, podTemplateHash);
            pods = workloadResourceService.getPods(clusterSeq, null, namespaceName, labels, ctx);
        }
        else {
            pods = workloadResourceService.getPods(clusterSeq, null, namespaceName, ctx);
        }

        log.debug("[END  ] getPodsInNamespace");

        return pods;
    }

    @GetMapping("/v2/cluster/{clusterSeq}/namespace/{namespaceName}/event")
    @Operation(summary = "Namespace Evnet 목록", description = "Namespace Event 목록을 반환한다.")
    public List<K8sEventVO> getNamespaceEvents(
        @Parameter(name = "clusterSeq", description = "cluster sequence") @PathVariable Integer clusterSeq,
        @Parameter(name = "namespaceName", description = "조회하려는 Namespace 명") @PathVariable String namespaceName
    ) throws Exception {
        log.debug("[BEGIN] getNamespaceEvents");

        ExecutingContextVO ctx = ContextHolder.exeContext();
        ctx.setApiVersionType(ApiVersionType.V2);

        List<K8sEventVO> namespaceEvents = k8sResourceService.getEventByCluster(clusterSeq, namespaceName, null, null, ctx);

        log.debug("[END  ] getNamespaceEvents");

        return namespaceEvents;
    }

    @GetMapping("/v2/cluster/{clusterSeq}/namespace/{namespaceName}/{involvedObjectName}/{involvedObjectKind}/event")
    @Operation(summary = "Namespace > involvedObject Evnet 목록", description = "Namespace > involvedObject Event 목록을 반환한다.")
    public List<K8sEventVO> getNamespaceEventsByInvolvedObject(
        @Parameter(name = "clusterSeq", description = "cluster sequence") @PathVariable Integer clusterSeq,
        @Parameter(name = "namespaceName", description = "조회하려는 Namespace 명") @PathVariable String namespaceName,
        @Parameter(name = "involvedObjectName", description = "조회하려는 involvedObject 명 ex) pod명, service명 ...") @PathVariable String involvedObjectName,
        @Parameter(name = "involvedObjectKind", description = "조회하려는 involvedObject 종류 ex) pod, service, deployment ...") @PathVariable String involvedObjectKind
    ) throws Exception {
        log.debug("[BEGIN] getNamespaceEventsByInvolvedObject");

        ExecutingContextVO ctx = ContextHolder.exeContext();
        ctx.setApiVersionType(ApiVersionType.V2);

        String fieldSelector = String.format("%s=%s,%s=%s", "involvedObject.name", involvedObjectName, "involvedObject.kind", involvedObjectKind);
        List<K8sEventVO> namespaceEvents = k8sResourceService.getEventByCluster(clusterSeq, namespaceName, fieldSelector, null, ctx);

        log.debug("[END  ] getNamespaceEventsByInvolvedObject");

        return namespaceEvents;
    }

    @GetMapping("/v2/cluster/{clusterSeq}/namespace/{namespaceName}/{involvedObjectName}/event")
    @Operation(summary = "Namespace > involvedObject Name별 Evnet 목록", description = "Namespace > involvedObject Name에 해당하는 Event 목록을 반환한다.")
    public List<K8sEventVO> getNamespaceEventsByInvolvedObjectName(
        @Parameter(name = "clusterSeq", description = "cluster sequence") @PathVariable Integer clusterSeq,
        @Parameter(name = "namespaceName", description = "조회하려는 Namespace 명") @PathVariable String namespaceName,
        @Parameter(name = "involvedObjectName", description = "조회하려는 involvedObject 명 ex) pod명, service명 ...") @PathVariable String involvedObjectName
    ) throws Exception {
        log.debug("[BEGIN] getNamespaceEventsByInvolvedObjectName");

        ExecutingContextVO ctx = ContextHolder.exeContext();
        ctx.setApiVersionType(ApiVersionType.V2);

        String fieldSelector = String.format("%s=%s", "involvedObject.name", involvedObjectName);
        List<K8sEventVO> namespaceEvents = k8sResourceService.getEventByCluster(clusterSeq, namespaceName, fieldSelector, null, ctx);

        log.debug("[END  ] getNamespaceEventsByInvolvedObjectName");

        return namespaceEvents;
    }

    @GetMapping("/v2/cluster/{clusterSeq}/namespace/{namespaceName}/pod/{podName}/container/log")
    @Operation(summary = "Container log", description = "Component의 container log(pod 수 x pod의 container 수)를 반환한다. 'since'의 경우 전달된 시간 값을 그대로 사용한다.")
    public List<PodLogVO> getContainerLogsInPod(
        @Parameter(name = "clusterSeq", description = "cluster sequence") @PathVariable Integer clusterSeq,
        @Parameter(name = "namespaceName", description = "조회하려는 Namespace 명") @PathVariable String namespaceName,
        @Parameter(name = "podName", description = "조회하려는 Pod 명") @PathVariable String podName,
        @Parameter(name = "type", description = "로그를 조회하는 방식", schema = @Schema(allowableValues = {"tail","since"})) @RequestParam(value = "type") String type,
        @Parameter(name = "count", description = "type이 tail인 경우 읽을 행 수(최대 10,000).", example = "5000", schema = @Schema(defaultValue = "1000")) @RequestParam(value = "count", required = false) String count,
        @Parameter(name = "sinceSeconds", description = "초단위, value 이전부터의 로그를 반환한다", example = "3600", schema = @Schema(defaultValue = "3600")) @RequestParam(value = "sinceSeconds", required = false) String sinceSeconds) throws Exception {
        log.debug("[BEGIN] getContainerLogsInPod");

        List<PodLogVO> logs = new ArrayList<>();
        try {
            if (StringUtils.equalsAny(type, new String[]{"tail", "since"})) {
                if (type.compareTo("tail") == 0) {
                    if (StringUtils.isBlank(count)) {
                        count = "1000";
                    }
                }
                else if (type.compareTo("since") == 0) {
                    if (StringUtils.isBlank(sinceSeconds)) {
                        sinceSeconds = "3600";
                    }
                }
                logs.addAll(workloadResourceService.getContainerLogsInPod(clusterSeq, namespaceName, podName, type, count, sinceSeconds));
            }
            else {
                throw new CocktailException(String.format("Unknown type: %s", type), ExceptionType.CubeLogTypeUnknown);
            }
        }
        catch (Exception eo) {
            if (eo instanceof CocktailException) {
                log.error("fail getContainerLogsInPod!!", eo);
                PodLogVO log = new PodLogVO();
                log.setPodName("no-name");
                log.setContainerName("no-name");
                log.setLog("<Pod not exists>");
                logs.add(log);
                return logs;
            }
            else {
                throw eo;
            }
        }
        log.debug("[END  ] getContainerLogsInPod");

        return logs;
    }

    @GetMapping("/v2/cluster/{clusterSeq}/namespace/{namespaceName}/deployment/{deploymentName:.+}")
    @Operation(summary = "Deployment 조회", description = "Deployment 정보를 반환한다.")
    public K8sDeploymentVO getDeployment(
        @Parameter(name = "clusterSeq", description = "cluster sequence") @PathVariable Integer clusterSeq,
        @Parameter(name = "namespaceName", description = "조회하려는 Namespace 명") @PathVariable String namespaceName,
        @Parameter(name = "deploymentName", description = "조회하려는 Deployment의 이름") @PathVariable String deploymentName
    ) throws Exception {
        log.debug("[BEGIN] getDeployment");

        if (StringUtils.isBlank(deploymentName)) {
            throw new CocktailException("deploymentName is required.", ExceptionType.InvalidParameter);
        }

        ExecutingContextVO ctx = ContextHolder.exeContext();
        ctx.setApiVersionType(ApiVersionType.V2);

        K8sDeploymentVO deployment = workloadResourceService.getDeployment(clusterSeq, namespaceName, deploymentName, ctx);

        log.debug("[END  ] getDeployment");

        return deployment;
    }

    @GetMapping("/v2/cluster/{clusterSeq}/namespace/{namespaceName}/deployment/{deploymentName}/replicasets")
    @Operation(summary = "ReplicaSet 목록", description = "ReplicaSet 목록을 반환한다.")
    public List<K8sReplicaSetVO> getReplicaSetsInDeployment(
        @Parameter(name = "clusterSeq", description = "cluster sequence") @PathVariable Integer clusterSeq,
        @Parameter(name = "namespaceName", description = "조회하려는 Namespace 명") @PathVariable String namespaceName,
        @Parameter(name = "deploymentName", description = "조회하려는 Deployment의 이름") @PathVariable String deploymentName
    ) throws Exception {
        log.debug("[BEGIN] getReplicaSetsInDeployment");

        ExecutingContextVO ctx = ContextHolder.exeContext();
        ctx.setApiVersionType(ApiVersionType.V2);

        List<K8sReplicaSetVO> replicaSets = workloadResourceService.getReplicaSets(clusterSeq, namespaceName, deploymentName, ctx);

        log.debug("[END  ] getReplicaSetsInDeployment");

        return replicaSets;
    }

    @GetMapping("/v2/cluster/{clusterSeq}/namespace/{namespaceName}/deployment/{deploymentName}/services")
    @Operation(summary = "Service 목록", description = "Service 목록을 반환한다.")
    public List<K8sServiceVO> getServicesInDeployment(
        @Parameter(name = "clusterSeq", description = "cluster sequence") @PathVariable Integer clusterSeq,
        @Parameter(name = "namespaceName", description = "조회하려는 Namespace 명") @PathVariable String namespaceName,
        @Parameter(name = "deploymentName", description = "조회하려는 Deployment의 이름") @PathVariable String deploymentName
    ) throws Exception {
        log.debug("[BEGIN] getServicesInDeployment");

        ExecutingContextVO ctx = ContextHolder.exeContext();
        ctx.setApiVersionType(ApiVersionType.V2);

        List<K8sServiceVO> services = serviceSpecService.getServicesInDeployment(clusterSeq, namespaceName, deploymentName, ctx);

        log.debug("[END  ] getServicesInDeployment");

        return services;
    }

    @GetMapping("/v2/cluster/{clusterSeq}/namespace/{namespaceName}/deployment/{deploymentName}/pods")
    @Operation(summary = "Namespace > pod 목록", description = "Deployments > pod 목록을 반환한다.")
    public List<K8sPodVO> getPodsInDeployments(
        @Parameter(name = "clusterSeq", description = "cluster sequence") @PathVariable Integer clusterSeq,
        @Parameter(name = "namespaceName", description = "조회하려는 Namespace 명") @PathVariable String namespaceName,
        @Parameter(name = "deploymentName", description = "조회하려는 Deployment의 이름") @PathVariable String deploymentName
    ) throws Exception {
        log.debug("[BEGIN] getPodsInDeployments");

        ExecutingContextVO ctx = ContextHolder.exeContext();
        ctx.setApiVersionType(ApiVersionType.V2);

//        List<K8sReplicaSetVO> replicaSets = workloadResourceService.getReplicaSets(clusterSeq, namespaceName, deploymentName, ctx);
//        long current = 0;
//        K8sReplicaSetVO currentReplica = null;
//        for (K8sReplicaSetVO replica : replicaSets) {
//            if (current == 0) {
//                current = replica.getCreationTimestamp().getMillis();
//                currentReplica = replica;
//            }
//            if (current <= replica.getCreationTimestamp().getMillis()) {
//                current = replica.getCreationTimestamp().getMillis();
//                currentReplica = replica;
//            }
//        }
//
//        log.debug("========= currentReplica : " + currentReplica.getName());
//        String labels = KubeConstants.LABELS_POD_TEMPLATE_HASH_KEY + "=";
//        if (currentReplica.getLabels() != null) {
//            labels = labels + currentReplica.getLabels().get(KubeConstants.LABELS_POD_TEMPLATE_HASH_KEY);
//        }

        K8sDeploymentVO k8sDeployment = workloadResourceService.getDeployment(clusterSeq, namespaceName, deploymentName, ctx);
        String labels = String.format("%s=%s", KubeConstants.LABELS_KEY, deploymentName);
        if (k8sDeployment != null) {
            List<String> hashKeys = Lists.newArrayList();
            if (CollectionUtils.isNotEmpty(k8sDeployment.getNewReplicaSets())) {
                hashKeys.add(k8sDeployment.getNewReplicaSets().get(0).getLabels().get(KubeConstants.LABELS_POD_TEMPLATE_HASH_KEY));
            }
            if (CollectionUtils.isNotEmpty(k8sDeployment.getOldReplicaSets())) {
                hashKeys.add(k8sDeployment.getOldReplicaSets().get(0).getLabels().get(KubeConstants.LABELS_POD_TEMPLATE_HASH_KEY));
            }

            if (CollectionUtils.isNotEmpty(hashKeys)) {
                labels = String.format("%s in (%s)", KubeConstants.LABELS_POD_TEMPLATE_HASH_KEY, Joiner.on(",").join(hashKeys));
            }
        }

        List<K8sPodVO> pods = workloadResourceService.getPods(clusterSeq, null, namespaceName, labels, ctx);

        log.debug("[END  ] getPodsInDeployments");

        return pods;
    }

    @GetMapping("/v2/cluster/{clusterSeq}/namespace/{namespaceName}/hpas")
    @Operation(summary = "Namespace > hpa 목록", description = "Deployments > hpa 목록을 반환한다.")
    public List<K8sHorizontalPodAutoscalerVO> getHorizontalPodAutoscalers(
        @Parameter(name = "clusterSeq", description = "cluster sequence") @PathVariable Integer clusterSeq,
        @Parameter(name = "namespaceName", description = "조회하려는 Namespace 명") @PathVariable String namespaceName,
        @Parameter(name = "controllerKind", description = "scaleTargetRef Kind", schema = @Schema(allowableValues = {"Deployment","Job","StatefulSet"})) @RequestParam(value = "controllerKind", required = false) String controllerKind,
        @Parameter(name = "controllerName", description = "scaleTargetRef Name") @RequestParam(value = "controllerName", required = false) String controllerName
    ) throws Exception {
        log.debug("[BEGIN] getHorizontalPodAutoscalers");

        ExecutingContextVO ctx = ContextHolder.exeContext();
        ctx.setApiVersionType(ApiVersionType.V2);

        List<K8sHorizontalPodAutoscalerVO> hpas = workloadResourceService.convertHorizontalPodAutoscalerDataList(clusterSeq, namespaceName, null, null);

        List<K8sHorizontalPodAutoscalerVO> matchedHpas = new ArrayList<>();
        // Kind, Name 모두 입력되지 않았을 경우 전체 응답.
        if (StringUtils.isBlank(controllerKind) && StringUtils.isBlank(controllerName)) {
            return hpas;
        }

        if (CollectionUtils.isNotEmpty(hpas)) {
            // Filter 적용..
            for (K8sHorizontalPodAutoscalerVO hpa : hpas) {
                if (hpa != null && hpa.getScaleTargetRef() != null) {
                    // Kind, Name 모두 입력되었을 경우
                    if (StringUtils.isNotBlank(controllerKind) && StringUtils.isNotBlank(controllerName)) {
                        // Kind, Name 모두 매치될 경우에만 add
                        if (StringUtils.equalsIgnoreCase(controllerKind, hpa.getScaleTargetRef().getKind()) && StringUtils.equalsIgnoreCase(controllerName, hpa.getScaleTargetRef().getName())) {
                            matchedHpas.add(hpa);
                        }
                    }
                    // else = Kind, Name 중 하나만 입력되었을 경우는 : 둘중 하나만 같아도 add
                    else if ((controllerKind != null && controllerKind.equalsIgnoreCase(hpa.getScaleTargetRef().getKind())) ||
                            (controllerName != null && controllerName.equalsIgnoreCase(hpa.getScaleTargetRef().getName()))) {
                        matchedHpas.add(hpa);
                    }
                }
            }
        }

        log.debug("[END  ] getHorizontalPodAutoscalers");

        return matchedHpas;
    }

    @GetMapping("/v2/cluster/{clusterSeq}/namespace/{namespaceName}/service/{serviceName:.+}")
    @Operation(summary = "Service 조회", description = "Service 정보 조회")
    public K8sServiceInfoVO getService(
        @Parameter(name = "clusterSeq", description = "cluster sequence") @PathVariable Integer clusterSeq,
        @Parameter(name = "namespaceName", description = "조회하려는 Namespace 명") @PathVariable String namespaceName,
        @Parameter(name = "serviceName", description = "조회하려는 service의 이름") @PathVariable String serviceName
    ) throws Exception {
        log.debug("[BEGIN] getService");

        if (StringUtils.isBlank(serviceName)) {
            throw new CocktailException("serviceName is required.", ExceptionType.InvalidParameter);
        }

        ExecutingContextVO ctx = ContextHolder.exeContext();
        ctx.setApiVersionType(ApiVersionType.V2);

        ClusterVO cluster = clusterService.getCluster(clusterSeq);
        K8sServiceVO k8sServiceSpec = serviceSpecService.getService(cluster, namespaceName, serviceName, ctx);

        if(k8sServiceSpec == null) {
            throw new CocktailException("ServiceSpec Not Found", ExceptionType.K8sServiceNotFound);
        }

        // K8sServiceVO -> K8sServiceInfoVO
        K8sServiceInfoVO k8sServiceInfo = new K8sServiceInfoVO();
        BeanUtils.copyProperties(k8sServiceSpec, k8sServiceInfo);

        /** Namespace안의 전체 이벤트를 조회하
         * 아래에서 서비스와 Pod의 이벤트 정보를 응답 **/
        List<K8sEventVO> namespaceEvents = k8sResourceService.getEventByCluster(cluster.getClusterSeq(), cluster.getNamespaceName(), null, null, ContextHolder.exeContext());

        /** 연결된 워크로드를 찾아서 매칭...
         * 연결된 워크로드를 찾으려면 전체 워크로드를 조회하여 찾아야 하므로 기존 워크로드 상태 조회 로직 활용 함.. **/
        ServerStateVO serverStates = serverStateService.getWorkloadsStateInNamespace(cluster, namespaceName, null, true, ContextHolder.exeContext());
        if (k8sServiceInfo != null && serverStates != null && CollectionUtils.isNotEmpty(serverStates.getComponents())) {

            List<ComponentVO> components = new ArrayList<>();

            for (ComponentVO component : serverStates.getComponents()) {
                if (CollectionUtils.isNotEmpty(component.getServices())) {
                    for (ServiceSpecGuiVO service : component.getServices()) {

                        if ( StringUtils.equals(service.getNamespaceName(), k8sServiceInfo.getNamespace()) && StringUtils.equals(service.getName(), k8sServiceInfo.getServiceName()) ){
                            /** Pod에 매칭되는 이벤트 찾아서 응답 **/
                            if (CollectionUtils.isNotEmpty(component.getPods())) {
                                for (K8sPodVO pod : component.getPods()) {
                                    List<K8sEventVO> podEvents = namespaceEvents.stream()
                                            .filter(ev -> (ev.getInvolvedObject().getKind().equals(K8sApiKindType.POD.getValue()) && ev.getInvolvedObject().getName().equals(pod.getPodName())))
                                            .collect(Collectors.toList());
                                    pod.setEvents(podEvents);
                                }
                            }
                            component.setServices(null);
                            component.setServerUrls(null);
                            components.add(component);
                            break;
                        }
                    }
                }
            }

            k8sServiceInfo.setWorkloads(components);
        }

        /** 서비스에 매칭되는 이벤트 찾아서 응답 **/
        List<K8sEventVO> serviceEvents = namespaceEvents.stream()
            .filter(ev -> (ev.getInvolvedObject().getKind().equals(K8sApiKindType.SERVICE.getValue()) && ev.getInvolvedObject().getName().equals(k8sServiceInfo.getServiceName())))
            .collect(Collectors.toList());
        k8sServiceInfo.setEvents(serviceEvents);

        log.debug("[END  ] getService");

        return k8sServiceInfo;
    }

    @GetMapping("/v2/cluster/{clusterSeq}/namespace/{namespaceName}/ingress/{ingressName:.+}")
    @Operation(summary = "Ingress 조회", description = "Ingress 정보 조회")
    public K8sIngressVO getIngress(
        @Parameter(name = "clusterSeq", description = "cluster sequence") @PathVariable Integer clusterSeq,
        @Parameter(name = "namespaceName", description = "조회하려는 Namespace 명") @PathVariable String namespaceName,
        @Parameter(name = "ingressName", description = "조회하려는 Ingress 의 이름") @PathVariable String ingressName
    ) throws Exception {
        log.debug("[BEGIN] getIngress");

        if (StringUtils.isBlank(ingressName)) {
            throw new CocktailException("ingressName is required.", ExceptionType.InvalidParameter);
        }

        ExecutingContextVO ctx = ContextHolder.exeContext();
        ctx.setApiVersionType(ApiVersionType.V2);

        ClusterVO cluster = clusterService.getCluster(clusterSeq);
        K8sIngressVO ingress = ingressSpecService.getIngress(cluster, namespaceName, ingressName, ctx);

        if(ingress == null) {
            throw new CocktailException("Ingress Not Found", ExceptionType.K8sIngressNotFound);
        }

        log.debug("[END  ] getIngress");

        return ingress;
    }

    @GetMapping("/v2/cluster/{clusterSeq}/namespace/{namespaceName}/daemonset/{daemonSetName:.+}")
    @Operation(summary = "DaemonSet 조회", description = "DaemonSet 정보를 반환한다.")
    public K8sDaemonSetVO getDaemonSet(
        @Parameter(name = "clusterSeq", description = "cluster sequence") @PathVariable Integer clusterSeq,
        @Parameter(name = "namespaceName", description = "조회하려는 Namespace 명") @PathVariable String namespaceName,
        @Parameter(name = "daemonSetName", description = "조회하려는 daemonSet의 이름") @PathVariable String daemonSetName
    ) throws Exception {
        log.debug("[BEGIN] getDaemonSet");

        if (StringUtils.isBlank(daemonSetName)) {
            throw new CocktailException("daemonSetName is required.", ExceptionType.InvalidParameter);
        }

        ExecutingContextVO ctx = ContextHolder.exeContext();
        ctx.setApiVersionType(ApiVersionType.V2);

        K8sDaemonSetVO daemonSet = workloadResourceService.getDaemonSet(clusterSeq, namespaceName, daemonSetName, ctx);

        log.debug("[END  ] getDaemonSet");

        return daemonSet;
    }

    @GetMapping("/v2/cluster/{clusterSeq}/namespace/{namespaceName}/cronjob/{cronJobName:.+}")
    @Operation(summary = "CronJob 조회", description = "CronJob 정보를 반환한다.")
    public K8sCronJobVO getCronJob(
        @Parameter(name = "clusterSeq", description = "cluster sequence") @PathVariable Integer clusterSeq,
        @Parameter(name = "namespaceName", description = "조회하려는 Namespace 명") @PathVariable String namespaceName,
        @Parameter(name = "cronJobName", description = "조회하려는 CronJob의 이름") @PathVariable String cronJobName
    ) throws Exception {
        log.debug("[BEGIN] getCronJob");

        ExceptionMessageUtils.checkParameterRequired("cronJob Name", cronJobName);

        ExecutingContextVO ctx = ContextHolder.exeContext();
        ctx.setApiVersionType(ApiVersionType.V2);

        K8sCronJobVO result = workloadResourceService.getCronJob(clusterSeq, namespaceName, cronJobName, ctx);

        log.debug("[END  ] getCronJob");

        return result;
    }

    @GetMapping("/v2/cluster/{clusterSeq}/namespace/{namespaceName}/job/{jobName:.+}")
    @Operation(summary = "Job 조회", description = "Job 정보를 반환한다.")
    public K8sJobVO getJob(
        @Parameter(name = "clusterSeq", description = "cluster sequence") @PathVariable Integer clusterSeq,
        @Parameter(name = "namespaceName", description = "조회하려는 Namespace 명") @PathVariable String namespaceName,
        @Parameter(name = "jobName", description = "조회하려는 Job의 이름") @PathVariable String jobName
    ) throws Exception {
        log.debug("[BEGIN] getJob");

        ExceptionMessageUtils.checkParameterRequired("job Name", jobName);

        ExecutingContextVO ctx = ContextHolder.exeContext();
        ctx.setApiVersionType(ApiVersionType.V2);

        K8sJobVO result = workloadResourceService.getJob(clusterSeq, namespaceName, jobName, ctx);

        log.debug("[END  ] getJob");

        return result;
    }

    @GetMapping("/v2/cluster/{clusterSeq}/namespace/{namespaceName}/statefulset/{statefulSetName:.+}")
    @Operation(summary = "StatefulSet 조회", description = "StatefulSet 정보를 반환한다.")
    public K8sStatefulSetVO getStatefulSet(
        @Parameter(name = "clusterSeq", description = "cluster sequence") @PathVariable Integer clusterSeq,
        @Parameter(name = "namespaceName", description = "조회하려는 Namespace 명") @PathVariable String namespaceName,
        @Parameter(name = "statefulSetName", description = "조회하려는 StatefulSet의 이름") @PathVariable String statefulSetName
    ) throws Exception {
        log.debug("[BEGIN] getStatefulSet");

        ExceptionMessageUtils.checkParameterRequired("statefulSet Name", statefulSetName);

        ExecutingContextVO ctx = ContextHolder.exeContext();
        ctx.setApiVersionType(ApiVersionType.V2);

        K8sStatefulSetVO result = workloadResourceService.getStatefulSet(clusterSeq, namespaceName, statefulSetName, ctx);

        log.debug("[END  ] getStatefulSet");

        return result;
    }

    @GetMapping("/cluster/{clusterSeq}/namespace/{namespaceName}/workload/{workloadName:.+}")
    @Operation(summary = "Namespace > workload Resource 조회", description = "Namespace > workload Resource 정보를 반환한다.")
    public WorkloadVO getWorkloadResource(
        @Parameter(name = "clusterSeq", description = "cluster sequence", required = true) @PathVariable Integer clusterSeq,
        @Parameter(name = "namespaceName", description = "조회하려는 Namespace 명", required = true) @PathVariable String namespaceName,
        @Parameter(name = "workloadName", description = "조회하려는 workload의 이름", required = true) @PathVariable String workloadName,
        @Parameter(name = "acloudOnly", description = "Acloud에서 관리되는 리소스만 조회 여부", schema = @Schema(allowableValues = {"true","false"}, defaultValue = "false")) @RequestParam(value = "acloudOnly", defaultValue = "false", required = false) Boolean acloudOnly
    ) throws Exception {
        log.debug("[BEGIN] getWorkloadResource");

        if (StringUtils.isBlank(workloadName)) {
            throw new CocktailException("workloadName is required.", ExceptionType.InvalidParameter);
        }

        WorkloadVO workload = serverService.getWorkloadResource(clusterSeq, namespaceName, workloadName, acloudOnly);

        log.debug("[END  ] getWorkloadResource");

        return workload;
    }

    @PostMapping("/v2/cluster/{clusterSeq}/replaceresource")
    @Operation(summary = "Kubernetes Spec file (json, yaml)을 이용하여 리소스 Replace")
    public SpecFileDeployVO replaceResource(
        @PathVariable("clusterSeq") Integer clusterSeq,
        @RequestBody @Validated SpecFileDeployVO deployData) throws Exception {

        /**
         * cluster 상태 체크
         */
        clusterStateService.checkClusterState(clusterSeq);

        ExecutingContextVO ctx = ContextHolder.exeContext();
        ctx.setApiVersionType(ApiVersionType.V2);

        SpecFileDeployVO specFileDeploy = k8sResourceService.replaceResourceBySpecFile(clusterSeq, deployData, ctx);

        return specFileDeploy;
    }


//    @GetMapping("/component/{componentSeq}/pod")
//    @Operation(summary = "Pod 이름 목록", description = "Cube 서버를 구성하고 있는 K8S Pod의 이름 목록을 반환한다.")
//    public List<String> getCubePodList(@Parameter(name = "componentSeq", description = "조회하려는 서버의 component sequence")
//                                       @PathVariable Integer componentSeq) throws Exception {
//        log.debug("[BEGIN] getCubePodList");
//
//        List<String> podNames = kubeResourceService.getCubePodList(componentSeq);
//
//        log.debug("[END  ] getCubePodList");
//
//        return podNames;
//    }
//
//    @GetMapping("/component/{componentSeq}/pod/{podName}/log")
//    @Operation(summary = "Pod log", notes = "Pod의 log를 지정한 방식으로 읽어 반환한다. " +
//            "'since'의 경우 전달된 시간 값을 그대로 사용한다.")
//    public ResultVO getPodLog(
//            @Parameter(name = "componentSeq", description = "조회하려는 서버의 component sequence")
//            @PathVariable Integer componentSeq,
//            @Parameter(name = "podName", description = "로그를 조회할 Pod의 이름")
//            @PathVariable String podName,
//            @Parameter(name = "type", description = "로그를 조회하는 방식", schema = @Schema(allowableValues = {"tail","since"}))
//            @RequestParam(value = "type") String type,
//            @Parameter(name = "count", description = "type이 tail인 경우 읽을 행 수(최대 10,000).",
//                    example = "time - '2006-01-02T15:04:05Z'")
//            @RequestParam(value = "count", required = false) String count,
//            @Parameter(name = "since", description = "type이 since인 경우 읽기 시작할 시간(최대 1시간 이전). " +
//                    "이 값이 없으면 1시간 이전부터의 로그를 반환한다",
//                    example = "time - '2017-01-02T15:04:05Z'")
//            @RequestParam(value = "since", required = false) String since,
//            @Parameter(name = "utc", description = "since가 local time인지 utc time인지 지정한다.",
//                    schema = @Schema(allowableValues = {"true","false"}))
//            @RequestParam(value = "utc", required = false, defaultValue = "true") String utc) throws Exception {
//        log.debug("[BEGIN] getPodLog");
//
//        ResultVO r = new ResultVO();
//        if (type.compareTo("tail") == 0) {
//            if (StringUtils.isEmpty(count)) {
//                throw new CocktailException("'count' required", ExceptionType.CubeLogCountInvalid);
//            } else {
//                r.setResult(kubeResourceService.getPodLog(componentSeq, podName, type, count));
//            }
//        } else if (type.compareTo("since") == 0) {
//            r.setResult(kubeResourceService.getPodLog(componentSeq, podName, type, since, utc));
//        } else {
//            throw new CocktailException(String.format("Unknown type: %s", type), ExceptionType.CubeLogTypeUnknown);
//        }
//
//        log.debug("[END  ] getPodLog");
//        return r;
//    }
//
//    @GetMapping("/component/{componentSeq}/container/log")
//    @Operation(summary = "Container log", notes = "Component의 container log(pod 수 x pod의 container 수)를 반환한다. " +
//            "'since'의 경우 전달된 시간 값을 그대로 사용한다.")
//    public ResultVO getContainerLogs(
//            @Parameter(name = "componentSeq", description = "조회하려는 서버의 component sequence")
//            @PathVariable Integer componentSeq,
//            @Parameter(name = "type", description = "로그를 조회하는 방식", schema = @Schema(allowableValues = {"tail","since"}))
//            @RequestParam(value = "type") String type,
//            @Parameter(name = "count", description = "type이 tail인 경우 읽을 행 수(최대 10,000).",
//                    example = "time - '2006-01-02T15:04:05Z'")
//            @RequestParam(value = "count", required = false) String count,
//            @Parameter(name = "since", description = "type이 since인 경우 읽기 시작할 시간(최대 1시간 이전). " +
//                    "이 값이 없으면 1시간 이전부터의 로그를 반환한다",
//                    example = "time - '2017-01-02T15:04:05Z'")
//            @RequestParam(value = "since", required = false) String since,
//            @Parameter(name = "utc", description = "since가 local time인지 utc time인지 지정한다.",
//                    schema = @Schema(allowableValues = {"true","false"}))
//            @RequestParam(value = "utc", required = false, defaultValue = "true") String utc) throws Exception {
//        log.debug("[BEGIN] getContainerLogs");
//
//        ResultVO r = new ResultVO();
//        try {
//            if (type.compareTo("tail") == 0) {
//                if (StringUtils.isEmpty(count)) {
//                    throw new CocktailException("'count' required", ExceptionType.CubeLogCountInvalid);
//                } else {
//                    r.setResult(kubeResourceService.getContainerLogs(componentSeq, type, count));
//                }
//            } else if (type.compareTo("since") == 0) {
////            r.setResult(kubeResourceService.getPodLog(componentSeq, podName, type, since, utc));
//            } else {
//                throw new CocktailException(String.format("Unknown type: %s", type), ExceptionType.CubeLogTypeUnknown);
//            }
//        } catch (Exception eo) {
//            if (eo instanceof CocktailException && ((CocktailException)eo).getType() == ExceptionType.K8sPodNotFound) {
//                PodLogVO log = new PodLogVO();
//                log.setPodName("no-name");
//                log.setContainerName("no-name");
//                log.setLog("<Pod not exists>");
//                List<PodLogVO> logs = new ArrayList<>();
//                logs.add(log);
//                r.setResult(logs);
//            } else {
//                throw eo;
//            }
//        }
//        log.debug("[END  ] getContainerLogs");
//        return r;
//    }
//
//    @GetMapping("/describe/component/{componentSeq}/type/{type}")
//    @Operation(summary = "Resource description", notes = "지정한 component의 resource 정보를 kubetcl describe에 준하는 " +
//            "내용으로 반환한다.")
//    public ResultVO getDescription(
//            @Parameter(name = "componentSeq", description = "조회하려는 서버의 component sequence")
//            @PathVariable Integer componentSeq,
//            @Parameter(name = "type", description = "조회하려는 리소스 형식",
//                    schema = @Schema(allowableValues = {"node","server, deployment, replicaset, pod"}))
//            @PathVariable String type,
//            @Parameter(name = "name", description = "조회하려는 리소스의 이름.")
//            @RequestParam(value = "name", required = false) String name)
//            throws Exception {
//
//        ResultVO r = new ResultVO();
//        Object desc = this.kubeResourceService.getDescription(componentSeq, type, name);
//        r.setResult(desc);
//        return r;
//    }
//
//    @GetMapping("/component/{componentSeq}/event")
//    @Operation(summary = "Get events of Server", description = "지정한 component(server)의 이벤트를 반환한다.")
//    public ResultVO getEvents(
//            @Parameter(name = "componentSeq", description = "조회하려는 서버의 component sequence")
//            @PathVariable Integer componentSeq,
//            @Parameter(name = "type", description = "조회하려는 이벤트 형식",
//                    schema = @Schema(allowableValues = {"all","deployment, replicaset, pod"}, defaultValue = "all"))
//            @RequestParam(value = "type", required = false, defaultValue = "all") String type,
//            @Parameter(name = "name", description = "조회하려는 이벤트와 관련된 리소스 이름")
//            @RequestParam(value = "name", required = false) String name
//            ) throws Exception {
//        ResultVO r = new ResultVO();
//        List<Map<String, Object>> events = this.kubeResourceService.getEvents(componentSeq, type, name);
//        r.setResult(events);
//        return r;
//    }
//
//    @GetMapping("/describe/node/cluster/{clusterSeq}/appmap/{appmapSeq}")
//    @Operation(summary = "Node description", notes = "지정한 cluster, appmap이 사용하는 node의 정보를 kubetcl describe에 " +
//            "준하는 내용으로 반환한다.")
//    public ResultVO getNodeDescription(
//            @Parameter(name = "clusterSeq", description = "조회하려는 노드가 속한 cluster sequence")
//            @PathVariable(name = "clusterSeq") Integer clusterSeq,
//            @Parameter(name = "appmapSeq", description = "조회하려는 노드를 사용하고 있는 appmap sequence")
//            @PathVariable(name = "appmapSeq") Integer appmapSeq) throws Exception {
//        ResultVO r = new ResultVO();
//        List<Map<String, Object>> desc = this.kubeResourceService.getNodeDescription(clusterSeq, appmapSeq);
//        r.setResult(desc);
//        return r;
//    }
//
//
//    @GetMapping("/describe/node/cluster/{clusterSeq}")
//    @Operation(summary = "Node description", notes = "지정한 cluster가 사용하는 node의 정보를 kubetcl describe에 " +
//            "준하는 내용으로 반환한다.")
//    public ResultVO getNodeDescription(
//            @Parameter(name = "clusterSeq", description = "조회하려는 노드가 속한 cluster sequence")
//            @PathVariable(name = "clusterSeq") Integer clusterSeq) throws Exception {
//        ResultVO r = new ResultVO();
//        List<Map<String, Object>> desc = this.kubeResourceService.getNodeDescription(clusterSeq, null);
//        r.setResult(desc);
//        return r;
//    }

//    @PostMapping("/check/cluster/{clusterSeq}")
//    @Operation(summary = "Server를 해당 cluster에 생성하려 할 때 리소스가 가용한지 검사한다")
//    public ResultVO checkDeploymentPracticabilityInCube(@PathVariable("clusterSeq") Integer clusterSeq,
//                                                  @RequestBody ServerAddVO serverParam) throws Exception {
//        ResourceState state = this.resourceService.checkResourceBeforeCreate(clusterSeq, serverParam.getContainers(),
//                serverParam.getServer().getComputeTotal());
//        this.resourceService.processResourceState(state);
//
//        return new ResultVO();
//    }
//
//    @PostMapping("/check/cluster/{clusterSeq}/server/{componentSeq}")
//    @Operation(summary = "Server를 수정할 때 클러스터의 리소스가 가용한지 검사한다")
//    public ResultVO checkDeploymentPracticabilityInComponent(@PathVariable("clusterSeq") Integer clusterSeq,
//                                                  @PathVariable("componentSeq") Integer componentSeq,
//                                                  @RequestBody ServerAddVO serverParam) throws Exception {
////        ResourceState state = this.resourceService.checkResourceBeforeUpdate(componentSeq, serverParam.getContainers(),
////                serverParam.getServer().getComputeTotal());
//        ResourceState state = this.resourceService.checkResourceBeforeCreate(clusterSeq, serverParam.getContainers(),
//                serverParam.getServer().getComputeTotal());
//        this.resourceService.processResourceState(state);
//
//        return new ResultVO();
//    }
//
//    @PostMapping("/check/cluster/{clusterSeq}/template")
//    @Operation(summary = "Template(catalog)을 해당 cluster에 적용하려 할 때 리소스가 가용한지 검사한다")
//    public ResultVO checkTemplatePracticability(@PathVariable("clusterSeq") Integer clusterSeq,
//                                                @RequestBody List<ServerAddVO> serverParams) throws Exception {
//        ResourceState state = this.resourceService.checkResourceBeforeCreate(clusterSeq, serverParams);
//        this.resourceService.processResourceState(state);
//
//        return new ResultVO();
//    }
//
//    @GetMapping("/v2/component/{componentSeq}/pod/{podName}/containers")
//    @Operation(summary = "Pod에 속한 container의 이름 목록을 반환한다.")
//    public ResultVO getContainerNamesOfPod(@Parameter(name = "componentSeq", description = "조회하려는 서버의 component sequence")
//                                           @PathVariable Integer componentSeq,
//                                           @Parameter(name = "podName", description = "조회하려는 pod의 이름")
//                                           @PathVariable String podName) throws Exception {
//        ResultVO result = new ResultVO();
//        result.setResult(this.resourceService.getContainerNamesOfPod(componentSeq, podName));
//        return result;
//    }
//
//    @GetMapping("/v2/component/{componentSeq}/resource")
//    @Operation(summary = "Server에 포함된 container의 cpu/memory limit/request를 반환한다.")
//    public ResultVO getComponentResourceQuota(@Parameter(name = "componentSeq", description = "조회하려는 서버의 component sequence")
//                                              @PathVariable Integer componentSeq) throws Exception {
//        ResultVO result = new ResultVO();
//        List<Double> quotas = this.resourceService.getResourceQuotaOfComponent(componentSeq);
//        Map<String, Double> r = new HashMap<>();
//        r.put("limit-cpu", quotas.get(0));
//        r.put("request-cpu", quotas.get(1));
//        r.put("limit-memory", quotas.get(2));
//        r.put("request-memory", quotas.get(3));
//        result.setResult(r);
//        return result;
//    }
}
