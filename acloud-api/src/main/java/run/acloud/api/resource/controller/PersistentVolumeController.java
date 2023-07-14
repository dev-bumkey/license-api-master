package run.acloud.api.resource.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import run.acloud.api.configuration.service.ClusterService;
import run.acloud.api.configuration.vo.ClusterVO;
import run.acloud.api.cserver.enums.DeployType;
import run.acloud.api.resource.service.PersistentVolumeService;
import run.acloud.api.resource.vo.K8sPersistentVolumeClaimVO;
import run.acloud.api.resource.vo.PersistentVolumeClaimGuiVO;
import run.acloud.api.resource.vo.PersistentVolumeClaimIntegrateVO;
import run.acloud.api.resource.vo.PersistentVolumeClaimYamlVO;
import run.acloud.commons.enums.ApiVersionType;
import run.acloud.commons.vo.ExecutingContextVO;
import run.acloud.framework.context.ContextHolder;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author wschoi@acornsoft.io
 * Created on 2017. 6. 20.
 */
@Tag(name = "Kubernetes Persistent Volume Management", description = "쿠버네티스 Persistent Volume에 대한 관리 기능을 제공한다.")
@Slf4j
@RequestMapping(value = "/api/volume")
@RestController
@Validated
public class PersistentVolumeController {
    @Autowired
    private PersistentVolumeService persistentVolumeService;

    @Autowired
    private ClusterService clusterService;

    @Deprecated
    @PostMapping("/v2/service/{serviceSeq}/servicemap/{servicemapSeq}/persistentvolumeClaim")
    @Operation(summary = "PersistentVolumeClaim 생성", description = "워크스페이스의 서비스맵 안의 PersistentVolumeClaim을 생성한다.")
    public void addPersistentVolumeClaimeV2(
            @Parameter(name = "serviceSeq", description = "service sequence") @PathVariable Integer serviceSeq,
            @Parameter(name = "servicemapSeq", description = "servicemap sequence") @PathVariable Integer servicemapSeq,
            @Parameter(name = "persistentVolumeClaim", description = "추가하려는 persistentVolumeClaim") @RequestBody PersistentVolumeClaimIntegrateVO persistentVolumeClaim
    ) throws Exception {
        log.debug("[BEGIN] addPersistentVolumeClaimeV2");

        ContextHolder.exeContext().setApiVersionType(ApiVersionType.V2);

        if (persistentVolumeClaim != null) {
           if (DeployType.valueOf(persistentVolumeClaim.getDeployType()) == DeployType.GUI) {
               persistentVolumeService.createPersistentVolumeClaim(servicemapSeq, (PersistentVolumeClaimGuiVO) persistentVolumeClaim, ContextHolder.exeContext());
           } else {
               throw new CocktailException("Only GUI supported.", ExceptionType.InvalidParameter);
           }
        } else {
           throw new CocktailException("persistentVolumeClaim parameter is null.", ExceptionType.InvalidParameter);
        }

        log.debug("[END  ] addPersistentVolumeClaimeV2");

    }

    @Deprecated
    @PutMapping("/v2/service/{serviceSeq}/servicemap/{servicemapSeq}/persistentvolumeClaim/{persistentVolumeClaimName:.+}")
    @Operation(summary = "PersistentVolumeClaim 수정", description = "워크스페이스의 서비스맵 안의 PersistentVolumeClaim을 수정한다.")
    public void updatePersistentVolumeClaimeV2(
            @Parameter(name = "serviceSeq", description = "service sequence", required = true) @PathVariable Integer serviceSeq,
            @Parameter(name = "servicemapSeq", description = "servicemap sequence", required = true) @PathVariable Integer servicemapSeq,
            @Parameter(name = "persistentVolumeClaimName", description = "수정하려는 PersistentVolumeClaim 명") @PathVariable String persistentVolumeClaimName,
            @Parameter(name = "persistentVolumeClaim", description = "수정하려는 persistentVolumeClaim") @RequestBody PersistentVolumeClaimIntegrateVO persistentVolumeClaim
    ) throws Exception {
        log.debug("[BEGIN] updatePersistentVolumeClaimeV2");

        ContextHolder.exeContext().setApiVersionType(ApiVersionType.V2);

        if (persistentVolumeClaim != null) {
            if (DeployType.valueOf(persistentVolumeClaim.getDeployType()) == DeployType.GUI) {
                persistentVolumeService.updatePersistentVolumeClaim(servicemapSeq, persistentVolumeClaimName, (PersistentVolumeClaimGuiVO) persistentVolumeClaim, ContextHolder.exeContext());
            } else {
                ClusterVO cluster = persistentVolumeService.setupCluster(servicemapSeq);
                if (cluster != null) {
                    PersistentVolumeClaimYamlVO persistentVolumeClaimYaml = (PersistentVolumeClaimYamlVO) persistentVolumeClaim;
                    if (StringUtils.equals(cluster.getNamespaceName(), persistentVolumeClaimYaml.getNamespace()) && StringUtils.equals(persistentVolumeClaimName, persistentVolumeClaimYaml.getName())) {
                        persistentVolumeService.updatePersistentVolumeClaimByYaml(cluster, persistentVolumeClaimYaml, ContextHolder.exeContext());
                    } else {
                        throw new CocktailException("namespace and name parameter is invalid.", ExceptionType.InvalidParameter);
                    }
                }
            }
        } else {
            throw new CocktailException("persistentVolumeClaim parameter is null.", ExceptionType.InvalidParameter);
        }

        log.debug("[END  ] updatePersistentVolumeClaimeV2");

    }

    @Deprecated
    @GetMapping("/v2/service/{serviceSeq}/servicemap/{servicemapSeq}/persistentvolumeClaim/{persistentVolumeClaimName:.+}")
    @Operation(summary = "PersistentVolumeClaim 중복 체크", description = "워크스페이스의 서비스맵안의 PersistentVolumeClaim 중복을 체크한다.")
    public Map<String, Boolean> checkDuplicatePersistentVolumeClaimeV2(
            @Parameter(name = "serviceSeq", description = "service sequence") @PathVariable Integer serviceSeq,
            @Parameter(name = "servicemapSeq", description = "servicemap sequence") @PathVariable Integer servicemapSeq,
            @Parameter(name = "persistentVolumeClaimName", description = "조회하려는 PersistentVolumeClaim 명") @PathVariable String persistentVolumeClaimName
    ) throws Exception {
        log.debug("[BEGIN] checkDuplicatePersistentVolumeClaimeV2");

        ExecutingContextVO ctx = new ExecutingContextVO();
        ctx.setApiVersionType(ApiVersionType.V2);

        boolean isExists = persistentVolumeService.checkDuplicatePersistentVolume(servicemapSeq, persistentVolumeClaimName, ctx);

        Map<String, Boolean> resultMap = new HashMap<>();
        resultMap.put("isExists", isExists);

        log.debug("[END  ] checkDuplicatePersistentVolumeClaimeV2");

        return resultMap;
    }

    @Deprecated
    @GetMapping("/v2/service/{serviceSeq}/servicemap/{servicemapSeq}/persistentvolumeClaims")
    @Operation(summary = "PersistentVolumeClaim 목록", description = "워크스페이스의 서비스맵 안의 PersistentVolumeClaim 목록을 반환한다.")
    public List<K8sPersistentVolumeClaimVO> getPersistentVolumeClaimasInAppmapV2(
            @Parameter(name = "serviceSeq", description = "service sequence") @PathVariable Integer serviceSeq,
            @Parameter(name = "servicemapSeq", description = "servicemap sequence") @PathVariable Integer servicemapSeq
    ) throws Exception {
        log.debug("[BEGIN] getPersistentVolumeClaimasInAppmapV2");

        ExecutingContextVO ctx = new ExecutingContextVO();
        ctx.setApiVersionType(ApiVersionType.V2);

        List<K8sPersistentVolumeClaimVO> persistentVolumeClaims = persistentVolumeService.getPersistentVolumeClaimsInServicemap(servicemapSeq, null, null, ctx);

        log.debug("[END  ] getPersistentVolumeClaimasInAppmapV2");

        return persistentVolumeClaims;
    }

    @Deprecated
    @GetMapping("/v2/cluster/{clusterSeq}/persistentvolumeClaims")
    @Operation(summary = "PersistentVolumeClaim 목록", description = "클러스터안의 PersistentVolumeClaim 목록을 반환한다.")
    public List<K8sPersistentVolumeClaimVO> getPersistentVolumeClaimasInClusterV2(
            @Parameter(name = "clusterSeq", description = "clusterSeq", required = true) @PathVariable Integer clusterSeq,
            @Parameter(name = "serviceSeq", description = "serviceSeq") @RequestParam(required = false) Integer serviceSeq,
            @Parameter(name = "acloudOnly", description = "Acloud에서 관리되는 컨피그맵만 조회 여부", schema = @Schema(allowableValues = {"true","false"}, defaultValue = "false")) @RequestParam(value = "acloudOnly", defaultValue = "false", required = false) Boolean acloudOnly
    ) throws Exception {
        log.debug("[BEGIN] getPersistentVolumeClaimasInClusterV2");

        ExecutingContextVO ctx = new ExecutingContextVO();
        ctx.setApiVersionType(ApiVersionType.V2);

        List<K8sPersistentVolumeClaimVO> persistentVolumeClaims = persistentVolumeService.getPersistentVolumeClaimsInCluster(clusterSeq, serviceSeq, null, null, acloudOnly, ctx);

        log.debug("[END  ] getPersistentVolumeClaimasInClusterV2");

        return persistentVolumeClaims;
    }

    @Deprecated
    @DeleteMapping("/v2/service/{serviceSeq}/servicemap/{servicemapSeq}/persistentvolumeClaim/{persistentVolumeClaimName:.+}")
    @Operation(summary = "PersistentVolumeClaim 삭제", description = "워크스페이스의 서비스맵안에 지정한 PersistentVolumeClaim을 삭제한다.")
    public void deletePersistentVolumeClaimeV2(
            @Parameter(name = "serviceSeq", description = "service sequence") @PathVariable Integer serviceSeq,
            @Parameter(name = "servicemapSeq", description = "servicemap sequence") @PathVariable Integer servicemapSeq,
            @Parameter(name = "persistentVolumeClaimName", description = "조회하려는 PersistentVolumeClaim 명") @PathVariable String persistentVolumeClaimName,
            @Parameter(name = "checkMount", description = "마운트되어 있는 지 체크하여 삭제 금지", schema = @Schema(allowableValues = {"true","false"}, defaultValue = "true")) @RequestParam(value = "checkMount", required = false, defaultValue = "true") boolean checkMount
    ) throws Exception {
        log.debug("[BEGIN] deletePersistentVolumeClaimeV2");

        ExecutingContextVO ctx = new ExecutingContextVO();
        ctx.setApiVersionType(ApiVersionType.V2);

        persistentVolumeService.deletePersistentVolumeClaimsInCluster(servicemapSeq, persistentVolumeClaimName, checkMount, ctx);

        log.debug("[END  ] deletePersistentVolumeClaimeV2");

    }


    /**
     * ======================================================================================================================================================================================================
     */

    @PostMapping("/v2/cluster/{clusterSeq}/namespace/{namespaceName}/persistentvolumeclaim")
    @Operation(summary = "PersistentVolumeClaim 생성", description = "클러스터의 네임스페이스안에 PersistentVolumeClaim 생성한다.")
    public void addPersistentVolumeClaimeV2(
            @Parameter(name = "clusterSeq", description = "cluster sequence", required = true) @PathVariable Integer clusterSeq,
            @Parameter(name = "namespaceName", description = "namespaceName", required = true) @PathVariable String namespaceName,
            @Parameter(name = "persistentVolumeClaim", description = "수정하려는 persistentVolumeClaim", required = true) @RequestBody PersistentVolumeClaimIntegrateVO persistentVolumeClaim
    ) throws Exception {
        log.debug("[BEGIN] addPersistentVolumeClaime");

        ContextHolder.exeContext().setApiVersionType(ApiVersionType.V2);

        if (persistentVolumeClaim != null) {
            if (DeployType.valueOf(persistentVolumeClaim.getDeployType()) == DeployType.GUI) {
                ClusterVO cluster = clusterService.getCluster(clusterSeq);
                persistentVolumeService.createPersistentVolumeClaim(cluster, namespaceName, (PersistentVolumeClaimGuiVO) persistentVolumeClaim, ContextHolder.exeContext());
            } else {
                throw new CocktailException("Only GUI supported.", ExceptionType.InvalidParameter);
            }
        } else {
            throw new CocktailException("persistentVolumeClaim parameter is null.", ExceptionType.InvalidParameter);
        }

        log.debug("[END  ] addPersistentVolumeClaime");

    }


    @PutMapping("/v2/cluster/{clusterSeq}/namespace/{namespaceName}/persistentvolumeclaim/{persistentVolumeClaimName:.+}")
    @Operation(summary = "PersistentVolumeClaim 수정", description = "클러스터의 네임스페이스안에 PersistentVolumeClaim로 수정한다.")
    public void updatePersistentVolumeClaimeV2(
            @Parameter(name = "clusterSeq", description = "cluster sequence", required = true) @PathVariable Integer clusterSeq,
            @Parameter(name = "namespaceName", description = "namespaceName", required = true) @PathVariable String namespaceName,
            @Parameter(name = "persistentVolumeClaimName", description = "수정하려는 PersistentVolumeClaim 명", required = true) @PathVariable String persistentVolumeClaimName,
            @Parameter(name = "persistentVolumeClaim", description = "수정하려는 persistentVolumeClaim", required = true) @RequestBody PersistentVolumeClaimIntegrateVO persistentVolumeClaim
    ) throws Exception {
        log.debug("[BEGIN] updatePersistentVolumeClaim");

        ContextHolder.exeContext().setApiVersionType(ApiVersionType.V2);

        if (persistentVolumeClaim != null) {
            ClusterVO cluster = clusterService.getCluster(clusterSeq);

            if (DeployType.valueOf(persistentVolumeClaim.getDeployType()) == DeployType.GUI) {
                persistentVolumeService.updatePersistentVolumeClaim(cluster, namespaceName, persistentVolumeClaimName, (PersistentVolumeClaimGuiVO) persistentVolumeClaim, ContextHolder.exeContext());
            } else {
                if (cluster != null) {
                    PersistentVolumeClaimYamlVO persistentVolumeClaimYaml = (PersistentVolumeClaimYamlVO) persistentVolumeClaim;
                    if (StringUtils.equals(namespaceName, persistentVolumeClaimYaml.getNamespace()) && StringUtils.equals(persistentVolumeClaimName, persistentVolumeClaimYaml.getName())) {
                        persistentVolumeService.updatePersistentVolumeClaimByYaml(cluster, persistentVolumeClaimYaml, ContextHolder.exeContext());
                    } else {
                        throw new CocktailException("namespace and name parameter is invalid.", ExceptionType.InvalidParameter);
                    }
                }
            }
        } else {
            throw new CocktailException("persistentVolumeClaim parameter is null.", ExceptionType.InvalidParameter);
        }

        log.debug("[END  ] updatePersistentVolumeClaim");

    }

    @GetMapping("/v2/cluster/{clusterSeq}/namespace/{namespaceName}/persistentvolumeClaim/{persistentVolumeClaimName:.+}/exist")
    @Operation(summary = "PersistentVolumeClaim 중복 체크", description = "클러스터의 네임스페이스안의 PersistentVolumeClaim 이름 중복 여부를 응답한다.")
    public Map<String, Boolean> checkDuplicatePersistentVolumeClaime(
            @Parameter(name = "clusterSeq", description = "cluster sequence", required = true) @PathVariable Integer clusterSeq,
            @Parameter(name = "namespaceName", description = "namespaceName", required = true) @PathVariable String namespaceName,
            @Parameter(name = "persistentVolumeClaimName", description = "조회하려는 PersistentVolumeClaim 명", required = true) @PathVariable String persistentVolumeClaimName
    ) throws Exception {
        log.debug("[BEGIN] checkDuplicatePersistentVolumeClaime");

        ExecutingContextVO ctx = new ExecutingContextVO();
        ctx.setApiVersionType(ApiVersionType.V2);

        ClusterVO cluster = clusterService.getCluster(clusterSeq);
        boolean isExists = persistentVolumeService.checkDuplicatePersistentVolume(cluster, namespaceName, persistentVolumeClaimName, ctx);

        Map<String, Boolean> resultMap = new HashMap<>();
        resultMap.put("isExists", isExists);

        log.debug("[END  ] checkDuplicatePersistentVolumeClaime");

        return resultMap;
    }

    @GetMapping("/v2/cluster/{clusterSeq}/namespace/{namespaceName}/persistentvolumeClaims")
    @Operation(summary = "PersistentVolumeClaim 목록", description = "클러스터의 네임스페이스안에 존재하는 전체 PersistentVolumeClaim 목록을 반환한다.")
    public List<K8sPersistentVolumeClaimVO> getPersistentVolumeClaimasInAppmap(
            @Parameter(name = "clusterSeq", description = "cluster sequence", required = true) @PathVariable Integer clusterSeq,
            @Parameter(name = "namespaceName", description = "namespaceName", required = true) @PathVariable String namespaceName
    ) throws Exception {
        log.debug("[BEGIN] getPersistentVolumeClaimasInAppmap");

        ExecutingContextVO ctx = new ExecutingContextVO();
        ctx.setApiVersionType(ApiVersionType.V2);

        List<K8sPersistentVolumeClaimVO> persistentVolumeClaims = persistentVolumeService.getPersistentVolumeClaimsInServicemap(clusterService.getCluster(clusterSeq), namespaceName, null, null, ctx);

        log.debug("[END  ] getPersistentVolumeClaimasInAppmap");

        return persistentVolumeClaims;
    }

    @GetMapping("/v2/cluster/id/{clusterId}/namespace/{namespaceName}/persistentvolumeClaims")
    @Operation(summary = "PersistentVolumeClaim 목록", description = "클러스터의(Cluster ID기준) 네임스페이스안에 존재하는 전체 PersistentVolumeClaim 목록을 반환한다.")
    public List<K8sPersistentVolumeClaimVO> getPersistentVolumeClaimasInAppmapById(
            @Parameter(name = "clusterId", description = "clusterId", required = true) @PathVariable String clusterId,
            @Parameter(name = "namespaceName", description = "namespaceName", required = true) @PathVariable String namespaceName
    ) throws Exception {
        log.debug("[BEGIN] getPersistentVolumeClaimasInAppmapById");

        ExecutingContextVO ctx = new ExecutingContextVO();
        ctx.setApiVersionType(ApiVersionType.V2);

        List<K8sPersistentVolumeClaimVO> persistentVolumeClaims = persistentVolumeService.getPersistentVolumeClaimsInServicemap(clusterService.getClusterByClusterId(clusterId), namespaceName, null, null, ctx);

        log.debug("[END  ] getPersistentVolumeClaimasInAppmapById");

        return persistentVolumeClaims;
    }

    @GetMapping("/{apiVersion}/cluster/{clusterSeq}/storageVolumes")
    @Operation(summary = "PersistentVolumeClaim 목록", description = "클러스터안의 전체 PersistentVolumeClaim 목록을 반환한다.")
    public List<K8sPersistentVolumeClaimVO> getStorageVolumesInCluster(
            @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v2"}), required = true) @PathVariable String apiVersion,
            @Parameter(name = "clusterSeq", description = "클러스터 번호", required = true) @PathVariable Integer clusterSeq
    ) throws Exception {
        log.debug("[BEGIN] getStorageVolumesInCluster");

        ApiVersionType apiVersionType = ApiVersionType.valueOf(StringUtils.upperCase(apiVersion));
        ContextHolder.exeContext().setApiVersionType(apiVersionType);

        List<K8sPersistentVolumeClaimVO> pvcs = new ArrayList<>();

        /**
         * 클러스터내의 전체 스토리지 볼륨 조회
         */
        ClusterVO cluster = clusterService.getClusterInfoWithStateCheck(clusterSeq, null, null);
        if (cluster != null) {
            pvcs = persistentVolumeService.getStorageVolumesInCluster(cluster, null, null, ContextHolder.exeContext());
        }

        log.debug("[END  ] getStorageVolumesInCluster");

        return pvcs;
    }

    @DeleteMapping("/v2/cluster/{clusterSeq}/namespace/{namespaceName}/persistentvolumeClaim/{persistentVolumeClaimName:.+}")
    @Operation(summary = "PersistentVolumeClaim 삭제", description = "클러스터의 네임스페이스안에 지정된 PersistentVolumeClaim을 삭제한다.")
    public void deletePersistentVolumeClaimeV2(
            @Parameter(name = "clusterSeq", description = "cluster sequence", required = true) @PathVariable Integer clusterSeq,
            @Parameter(name = "namespaceName", description = "namespaceName", required = true) @PathVariable String namespaceName,
            @Parameter(name = "persistentVolumeClaimName", description = "조회하려는 PersistentVolumeClaim 명", required = true) @PathVariable String persistentVolumeClaimName,
            @Parameter(name = "checkMount", description = "마운트되어 있는 지 체크하여 삭제 금지", schema = @Schema(allowableValues = {"true","false"}, defaultValue = "true")) @RequestParam(value = "checkMount", required = false, defaultValue = "true") boolean checkMount
    ) throws Exception {
        log.debug("[BEGIN] deletePersistentVolumeClaime");

        ExecutingContextVO ctx = new ExecutingContextVO();
        ctx.setApiVersionType(ApiVersionType.V2);

        ClusterVO cluster = clusterService.getCluster(clusterSeq);
        persistentVolumeService.deletePersistentVolumeClaimsInCluster(cluster, namespaceName, persistentVolumeClaimName, checkMount, ctx);

        log.debug("[END  ] deletePersistentVolumeClaime");

    }
}
