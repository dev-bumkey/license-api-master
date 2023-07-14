package run.acloud.api.resource.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import run.acloud.api.cserver.enums.*;
import run.acloud.commons.vo.HasUseYnVO;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * @author wschoi@acornsoft.io
 * Created on 2017. 6. 20.
 */
@Getter
@Setter
public class PersistentVolumeVO extends HasUseYnVO implements Serializable {

    @Schema(description = "Persistent Volume의 유형.", allowableValues = {"SINGLE","SHARED"})
    private PersistentVolumeType persistentVolumeType;

    @Schema(description = "Volume의 유형.", allowableValues = {"PERSISTENT_VOLUME_LINKED"})
    private VolumeType volumeType = VolumeType.PERSISTENT_VOLUME_LINKED;

    @Schema(description = "Persistent Volume 일련 번호")
    @Deprecated
    private Integer volumeSeq;

    @Schema(description = "Storage Volume 이름")
    private String storageVolumeName;

    @Schema(description = "Persistent Volume이 속한 cluster 일련 번호", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer clusterSeq;

    @Schema(description = "Persistent Volume 이름")
    private String name;

    @Schema(description = "라벨")
    private Map<String, String> labels;

    @Schema(description = "어노테이션")
    private Map<String, String> annotations;

    @Schema(description = "Persistent Volume 용량. Giga byte 단위", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer capacity;

    @Schema(description = "Acess Mode - RWO(Read Write Once), ROX(Read Only Many), RWX(Read Write Many)", requiredMode = Schema.RequiredMode.REQUIRED,
            allowableValues = {"RWO", "ROX", "RWX"})
    private AccessMode accessMode;

    @Schema(description = "Reclaim Policy", requiredMode = Schema.RequiredMode.REQUIRED,
            allowableValues = {"RECYCLE", "RETAIN", "DELETE"})
    private ReclaimPolicy reclaimPolicy;

    @Schema(description = "Storage class 일련번호")
    // 실제로는 Cluster Volume Seq
    private Integer storageClassSeq;

    @Schema(description = "Storage class Name")
    private String storageClassName;

    @Schema(description = "Persistent Volume Plugin 이름.", requiredMode = Schema.RequiredMode.REQUIRED)
    private VolumePlugIn plugin;

    @Schema(description = "Persistent Volume이 Persistent Volume Claim을 통해 현재 사용되고 있는지를 나타낸다.")
    private String boundYn;

    @Schema(description = "Persistent Volume의 상태.")
    private VolumePhase phase;

    @Schema(description = "Persistent Volume에 대한 설명.")
    private String description;

    @Schema(description = "Plugin parameter")
    private List<PersistentVolumeParamterVO> parameters;
}
