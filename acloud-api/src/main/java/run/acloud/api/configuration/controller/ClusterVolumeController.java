package run.acloud.api.configuration.controller;

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
import run.acloud.api.configuration.service.ClusterStateService;
import run.acloud.api.configuration.service.ClusterVolumeService;
import run.acloud.api.configuration.vo.ClusterVO;
import run.acloud.api.configuration.vo.ClusterVolumeVO;
import run.acloud.api.cserver.enums.ReclaimPolicy;
import run.acloud.api.cserver.enums.VolumePlugIn;
import run.acloud.api.resource.service.StorageClassService;
import run.acloud.api.resource.vo.K8sDeployYamlVO;
import run.acloud.api.resource.vo.K8sStorageClassVO;
import run.acloud.commons.annotations.InHouse;
import run.acloud.commons.enums.ApiVersionType;
import run.acloud.commons.util.AuthUtils;
import run.acloud.commons.vo.ExecutingContextVO;
import run.acloud.commons.vo.ResultVO;
import run.acloud.framework.context.ContextHolder;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;

import java.util.*;

/**
 * @author wschoi@acornsoft.io
 * Created on 2017. 7. 5.
 */
@Tag(name = "Storage", description = "스토리지에 대한 관리 기능을 제공한다.")
@Slf4j
@RequestMapping(value = "/api/cluster")
@RestController
@Validated
public class ClusterVolumeController {
    @Autowired
    private ClusterVolumeService clusterVolumeService;

    @Autowired
    private ClusterService clusterService;

    @Autowired
    private ClusterStateService clusterStateService;

    @Autowired
    private StorageClassService storageClassService;

    @GetMapping(value = "/volumeplugins/{plugin}/recalimpolicies")
    public List<ReclaimPolicy> getAvailableReclaimPolices(@Parameter(name = "plugin", description = "조회하려는 volume plugin 이름.")
                                                          @PathVariable VolumePlugIn plugin) throws Exception {
        List<ReclaimPolicy> policies = new ArrayList<>();
        policies.add(ReclaimPolicy.RETAIN);
        if (plugin != VolumePlugIn.NFSSTATIC) {
            policies.add(ReclaimPolicy.DELETE);
        }

        return policies;
    }

    /**
     * =======================================================================================================================================================================================
     * New api - R4.0.0
     * =======================================================================================================================================================================================
     */

    @GetMapping(value = "/storages")
    @Operation(summary = "스토리지 목록", description = "스토리지 목록 조회한다.")
    public List<ClusterVolumeVO> getStorages(
            @Parameter(name = "serviceSeq", description = "서비스 번호", required = false) @RequestParam(name = "serviceSeq", required = false) Integer serviceSeq,
            @Parameter(name = "storageType", description = "storageType", schema = @Schema(allowableValues = {"NETWORK","BLOCK"}, defaultValue = "")) @RequestParam(name = "storageType", required = false, defaultValue = "") String storageType,
            @Parameter(name = "type", description = "type", schema = @Schema(allowableValues = {"PERSISTENT_VOLUME","PERSISTENT_VOLUME_STATIC"}, defaultValue = "")) @RequestParam(name = "type", required = false, defaultValue = "") String type,
            @Parameter(name = "useCapacity", description = "K8S total capacity 정보 사용 여부", schema = @Schema(allowableValues = {"true","false"}, defaultValue = "false")) @RequestParam(value = "useCapacity", required = false, defaultValue = "false") boolean useCapacity,
            @Parameter(name = "useRequest", description = "K8S Claim Request 정보 사용 여부", schema = @Schema(allowableValues = {"true","false"}, defaultValue = "false")) @RequestParam(value = "useRequest", required = false, defaultValue = "false") boolean useRequest
    ) throws Exception {
        log.debug("[BEGIN] getStorages");

        /**
         * DevOps 사용자만 Header로 수신되는 인증된 Workspace 값을 사용하여 조회하도록 처리.
         */
        if(!AuthUtils.isNotDevOpsUser(ContextHolder.exeContext())) {
            serviceSeq = ContextHolder.exeContext().getUserServiceSeq();
        }

        /** SystemUser가 serviceSeq를 입력하지 않았을 경우에도 Header의 serviceSeq를 사용하도록 추가 **/
        if(AuthUtils.isSystemNSysadminUser(ContextHolder.exeContext()) && serviceSeq == null) {
            serviceSeq = ContextHolder.exeContext().getUserServiceSeq();
        }
        log.debug("############### serviceSeq : " + serviceSeq);

        if(serviceSeq == null) {
            throw new CocktailException("serviceSeq is required", ExceptionType.InvalidParameter_Empty);
        }

        List<ClusterVolumeVO> clusterVolumes = this.clusterVolumeService.getStorageVolumes(null, Collections.singletonList(serviceSeq), null, StringUtils.defaultIfBlank(storageType, null), StringUtils.defaultIfBlank(type, null), useCapacity, useRequest);

        log.debug("[END  ] getStorages");

        return clusterVolumes;
    }

    @GetMapping(value = "/{clusterSeq}/storages")
    @Operation(summary = "클러스터 > 스토리지 목록", description = "클러스터 > 스토리지 목록 조회한다.")
    public List<ClusterVolumeVO> getStoragesOfCluster(
            @Parameter(name = "clusterSeq", description = "조회하려는 스토리지이 속한 클러스터 일련번호", required = true) @PathVariable Integer clusterSeq,
            @Parameter(name = "serviceSeq", description = "서비스 번호", required = false) @RequestParam(name = "serviceSeq", required = false) Integer serviceSeq,
            @Parameter(name = "storageType", description = "storageType", schema = @Schema(allowableValues = {"NETWORK","BLOCK"}, defaultValue = "")) @RequestParam(name = "storageType", required = false, defaultValue = "") String storageType,
            @Parameter(name = "type", description = "type", schema = @Schema(allowableValues = {"PERSISTENT_VOLUME","PERSISTENT_VOLUME_STATIC"}, defaultValue = "")) @RequestParam(name = "type", required = false, defaultValue = "") String type,
            @Parameter(name = "useCapacity", description = "K8S total capacity 정보 사용 여부", schema = @Schema(allowableValues = {"true","false"}, defaultValue = "false")) @RequestParam(value = "useCapacity", required = false, defaultValue = "false") boolean useCapacity,
            @Parameter(name = "useRequest", description = "K8S Claim Request 정보 사용 여부", schema = @Schema(allowableValues = {"true","false"}, defaultValue = "false")) @RequestParam(value = "useRequest", required = false, defaultValue = "false") boolean useRequest
    ) throws Exception {

        log.debug("[BEGIN] getStoragesOfCluster");

        List<Integer> serviceSeqs = null;
        if (serviceSeq != null) {
            serviceSeqs = Collections.singletonList(serviceSeq);
        }
        List<ClusterVolumeVO> clusterVolumes = this.clusterVolumeService.getStorageVolumes(null, serviceSeqs, clusterSeq, StringUtils.defaultIfBlank(storageType, null), StringUtils.defaultIfBlank(type, null), useCapacity, useRequest);

        log.debug("[END  ] getStoragesOfCluster");

        return clusterVolumes;
    }

    @GetMapping(value = "/{clusterSeq}/storage/{storageName:.+}")
    @Operation(summary = "스토리지 상세", description = "스토리지 상세 조회한다.")
    public ClusterVolumeVO getStorageVolume(
            @Parameter(name = "clusterSeq", description = "조회하려는 Cluster volume이 속한 클러스터 일련번호", required = true) @PathVariable Integer clusterSeq,
            @Parameter(name = "storageName", description = "조회하려는 Storage 명", required = true) @PathVariable String storageName
    ) throws Exception {

        log.debug("[BEGIN] getStorageVolume");

        ClusterVolumeVO clusterVolume = clusterVolumeService.getStorageVolume(clusterSeq, storageName);

        log.debug("[END  ] getStorageVolume");

        return clusterVolume;
    }

    @PostMapping("/{clusterSeq}/storage")
    @Operation(summary = "스토리지 생성", description = "스토리지 생성한다.")
    public ClusterVolumeVO addStorageVolume(
            @RequestHeader(name = "user-id" ) Integer userSeq,
            @Parameter(name = "clusterSeq", description = "조회하려는 Cluster volume이 속한 클러스터 일련번호", required = true) @PathVariable Integer clusterSeq,
            @Parameter(description = "Cluster volume", required = true) @RequestBody ClusterVolumeVO volume
    ) throws Exception {

        log.debug("[BEGIN] addStorageVolume");

        volume.setCreator(userSeq);
        volume.setUpdater(userSeq);

        this.clusterVolumeService.addStorageVolume(volume, true);

        log.debug("[END  ] addStorageVolume");

        return volume;
    }

    @PutMapping(value = "/{clusterSeq}/storage/{storageName:.+}")
    @Operation(summary = "스토리지 수정", description = "스토리지 수정한다.")
    public void udpateStorageVolume(
            @Parameter(name = "clusterSeq", description = "조회하려는 Cluster volume이 속한 클러스터 일련번호", required = true) @PathVariable Integer clusterSeq,
            @Parameter(name = "storageName", description = "조회하려는 Storage 명", required = true) @PathVariable String storageName,
            @Parameter(description = "Cluster volume", required = true) @RequestBody ClusterVolumeVO volume
    ) throws Exception {

        log.debug("[BEGIN] udpateStorageVolume");

        ClusterVolumeVO cv = clusterVolumeService.getStorageVolume(clusterSeq, storageName);
        if (cv == null) {
            throw new CocktailException(String.format("ClusterVolume not found: %d, %s", clusterSeq, storageName),
                    ExceptionType.ClusterVolumeNotFound);
        }

        clusterVolumeService.checkRequest(volume, false);

        clusterVolumeService.updateStorageVolume(volume);

        log.debug("[END  ] udpateStorageVolume");
    }

    @PutMapping(value = "/{clusterSeq}/storageclass/{storageClassName:.+}")
    @Operation(summary = "Cluster > StorageClass 정보 yaml 수정", description = "Cluster > StorageClass 정보 yaml로 수정한다.")
    public void udpateStroageClassByYaml(
            @Parameter(name = "clusterSeq", description = "Cluster 일련번호", required = true) @PathVariable Integer clusterSeq,
            @Parameter(name = "storageClassName", description = "storageClassName", required = true) @PathVariable String storageClassName,
            @Parameter(description = "deployYaml", required = true) @RequestBody K8sDeployYamlVO deployYaml
    ) throws Exception {

        log.debug("[BEGIN] udpateStroageClassByYaml");

        ClusterVO cluster = clusterService.getCluster(clusterSeq);

        /**
         * cluster 상태 체크
         */
        clusterStateService.checkClusterState(cluster);

        K8sStorageClassVO storageClass = storageClassService.getStorageClass(cluster, storageClassName, ContextHolder.exeContext());
        if (storageClass == null) {
            throw new CocktailException(String.format("StorageClass not found: %d", storageClassName),
                    ExceptionType.ClusterVolumeNotFound);
        } else {
            if (cluster != null) {
                if (StringUtils.equals(storageClassName, deployYaml.getName())) {
                    storageClassService.patchStorageClassByYaml(cluster, deployYaml);
                } else {
                    throw new CocktailException("name parameter is invalid.", ExceptionType.InvalidParameter);
                }
            }
        }

        log.debug("[END  ] udpateStroageClassByYaml");
    }

    @GetMapping(value = "/{clusterSeq}/storage/{storageName:.+}/deletable")
    @Operation(summary = "스토리지 삭제 여부 검사", description = "스토리지를 삭제할 수 있는 지 검사한다.")
    public ResultVO canDeleteClusterVolume(
            @Parameter(name = "clusterSeq", description = "조회하려는 Cluster volume이 속한 클러스터 일련번호", required = true) @PathVariable Integer clusterSeq,
            @Parameter(name = "storageName", description = "조회하려는 Storage 명", required = true) @PathVariable String storageName
    ) throws Exception {

        ResultVO result = new ResultVO();
        result.setResult(clusterVolumeService.canDeleteStorageVolume(clusterSeq, storageName));
        return result;
    }

    @DeleteMapping(value = "/{clusterSeq}/storage/{storageName:.+}")
    @Operation(summary = "스토리지 삭제", description = "스토리지 삭제한다.")
    public void removeStorageVolume(
            @Parameter(name = "clusterSeq", description = "조회하려는 Cluster volume이 속한 클러스터 일련번호", required = true) @PathVariable Integer clusterSeq,
            @Parameter(name = "storageName", description = "조회하려는 Storage 명", required = true) @PathVariable String storageName
    ) throws Exception {

        log.debug("[BEGIN] removeStorageVolume");

        if (!clusterVolumeService.canDeleteStorageVolume(clusterSeq, storageName)) {
            throw new CocktailException("ClusterVolume is used", ExceptionType.ClusterVolumeNotDeletableState);
        }

        this.clusterVolumeService.deleteStorageVolume(clusterSeq, storageName);

        log.debug("[END  ] removeStorageVolume");
    }

    @InHouse
    @GetMapping(value = "/{clusterSeq}/storage/{storageName:.+}/resource/{capacity}/check")
    @Operation(summary = "지정한 Cluster volume의 리소스 할당량 체크")
    public Map<String, Object> isStorageVolumeResourceAvailable(
            @Parameter(name = "clusterSeq", description = "조회하려는 Cluster volume이 속한 클러스터 일련번호", required = true) @PathVariable Integer clusterSeq,
            @Parameter(name = "storageName", description = "조회하려는 Storage 명", required = true) @PathVariable String storageName,
            @Parameter(name = "capacity", description = "요청 용량, GB") @PathVariable long capacity
    ) throws Exception {
        ExecutingContextVO ctx = new ExecutingContextVO();
        ctx.setApiVersionType(ApiVersionType.V2);

        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("isValid", Boolean.TRUE);
        ctx.setParams(resultMap);

        ClusterVolumeVO cv = clusterVolumeService.getStorageVolume(clusterSeq, storageName);
        if (cv == null) {
            throw new CocktailException(String.format("ClusterVolume not found: %d, %s", clusterSeq, storageName), ExceptionType.ClusterVolumeNotFound);
        }else{
            if(cv.getPlugin().haveTotalCapacity()){
                clusterVolumeService.checkStorageResource(cv, capacity, ctx);
            }
        }

        return ctx.getParams();
    }
}
