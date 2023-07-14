package run.acloud.api.configuration.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import run.acloud.api.cserver.enums.*;
import run.acloud.api.resource.vo.K8sStorageClassVO;
import run.acloud.commons.vo.HasUseYnVO;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * @author wschoi@acornsoft.io
 * Created on 2017. 7. 5.
 *
 * 1. NFS 설정
 * 2. AWS EBS, Google Persistent Disk, Azuer Disk 등에 대해서는 Storage class를 생성하기 위한 정보로 사용한다
 * 3. NFS Static Volume 정보 제공.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class ClusterVolumeVO extends HasUseYnVO implements Serializable {
    @Schema(description = "Volume 일련 번호")
    @Deprecated
    private Integer volumeSeq;

    @Schema(description = "Volume이 속한 cluster 일련 번호", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer clusterSeq;

    @Schema(description = "Volume 이름", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(description = "라벨")
    private Map<String, String> labels;

    @Schema(description = "어노테이션")
    private Map<String, String> annotations;

    @Schema(description = "Storage Type", requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = {"BLOCK","NETWORK"})
    private StorageType storageType;

    public void setStorageType(StorageType storageType) {
        if (storageType == null) {
            this.storageType = this.getStorageType();
        } else {
            this.storageType = storageType;
        }
    }

    public StorageType getStorageType() {
        if (this.storageType == null && this.plugin != null) {
            return this.plugin.getStorageType();
        }
        return this.storageType;
    }

    @Schema(description = "설정 형식", requiredMode = Schema.RequiredMode.REQUIRED)
    private VolumeType type;

    @Schema(description = "기본 스토리지 여부", allowableValues = {"Y","N"})
    private String baseStorageYn;

    @Schema(description = "총 Storage 용량. Giga byte 단위. NFS Plugin")
    private Integer totalCapacity;

    @Schema(description = "총 Request Storage 용량. Giga byte 단위.")
    private Integer totalRequest;

    @Schema(description = "총 PVC 수")
    private Integer totalVolumeCount;

    @Schema(description = "Volume 용량. Giga byte 단위. NFS외의 볼륨에서는 한 번에 생성할 수 있는 볼륨의 최대 크기")
    private Integer capacity;

    @Schema(description = "Reclaim Policy", requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = {"RECYCLE", "RETAIN", "DELETE"})
    private ReclaimPolicy reclaimPolicy;

    @Schema(description = "Persistent Volume Plugin 이름.", requiredMode = Schema.RequiredMode.REQUIRED)
    private VolumePlugIn plugin;

    @Schema(description = "Provisioner 이름.")
    private String provisionerName;

    @Schema(description = "volumeBindingMode.", allowableValues = {"IMMEDIATE","WAIT_FOR_FIRST_CONSUMER"})
    private VolumeBindingMode volumeBindingMode = VolumeBindingMode.IMMEDIATE;

    @Schema(description = "Persistent Volume ReadWriteOnce 지원여부.")
    private String readWriteOnceYn;

    @Schema(description = "Persistent Volume ReadOnlyMany 지원여부.")
    private String readOnlyManyYn;

    @Schema(description = "Persistent Volume ReadWriteMany 지원여부.")
    private String readWriteManyYn;

    @Schema(description = "볼륨 상태. static volume만 사용")
    private VolumePhase phase;

    @Schema(description = "Volume에 대한 설명.")
    private String description;

    @Schema(description = "Cluster Volume Parameter. 볼륨 설정만 사용")
    private List<ClusterVolumeParamterVO> parameters;

    @Schema(description = "Cluster Volume MountOptions. 볼륨 설정만 사용")
    private List<String> mountOptions;

    @Schema(description = "Storage class 정보")
    private K8sStorageClassVO storageClass;

    @Schema(description = "cluster 정보")
    @JsonIgnore
    private ClusterVO cluster;
}
