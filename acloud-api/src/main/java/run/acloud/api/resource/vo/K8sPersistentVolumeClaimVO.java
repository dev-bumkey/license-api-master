package run.acloud.api.resource.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import run.acloud.api.configuration.vo.ClusterVolumeVO;
import run.acloud.api.cserver.vo.ServicemapSummaryVO;
import run.acloud.commons.vo.BaseVO;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * @author: wschoi@acornsoft.io
 * Created on 2017. 10. 12.
 */
@Getter
@Setter
@Schema(description = "PersistentVolumeClaim 모델")
public class K8sPersistentVolumeClaimVO extends BaseVO{

    @Schema(title = "라벨")
    private String label;

    @Schema(title = "네임스페이스")
    private String namespace;

    @Schema(title = "PVC 명")
    private String name;

    @Schema(title = "PVC 상태")
    private String status;

    @Schema(title = "PV 명")
    private String volumeName;

    @Schema(title = "용량")
    private Map<String, String> capacity;

    @Schema(title = "용량")
    @JsonIgnore
    private long capacityByte;

    @Schema(title = "Access Modes")
    private List<String> accessModes;

    @Schema(title = "Storage Class 명")
    @JsonProperty(defaultValue = "-")
    private String storageClassName;

    @Schema(title = "생성시간")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private OffsetDateTime creationTimestamp;

    @Schema(title = "PVC 배포 정보")
    private String deployment;

    @Schema(title = "PVC 배포 정보 (yaml)")
    private String deploymentYaml;

    @Schema(title = "PVC 상세")
    private K8sPersistentVolumeClaimDetailVO detail;

    @Schema(title = "PersistentVolume")
    private K8sPersistentVolumeVO persistentVolume;

    @Schema(title = "Cluster Volume (storage)")
    private ClusterVolumeVO clusterVolume;

    @Schema(title = "Server Parameter")
    private List<ServerDetailParamForPVVO> serverParams;

    @Schema(title = "Persistent Volume의 유형.", allowableValues = {"SINGLE","SHARED"})
    private String persistentVolumeType;

    @Schema(title = "Storage class 정보")
    private K8sStorageClassVO storageClass;

    @Schema(title = "클러스터순번")
    private Integer clusterSeq;

    @Schema(title = "클러스터이름")
    private String clusterName;

    @Schema(title = "서비스맵 정보")
    private ServicemapSummaryVO servicemapInfo;
}
