package run.acloud.api.resource.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import run.acloud.api.cserver.enums.AccessMode;
import run.acloud.api.cserver.enums.PersistentVolumeType;
import run.acloud.api.cserver.enums.ReclaimPolicy;
import run.acloud.api.cserver.enums.VolumeType;

import java.util.List;
import java.util.Map;

/**
 * @author wschoi@acornsoft.io
 * Created on 2017. 6. 20.
 */
@Getter
@Setter
@Schema(name = "PersistentVolumeClaimGuiVO"
        , title = "PersistentVolumeClaimGuiVO"
        , allOf = {PersistentVolumeClaimIntegrateVO.class}
        , description = "GUI 배포 모델"
)
public class PersistentVolumeClaimGuiVO extends PersistentVolumeClaimIntegrateVO {

    @Schema(description = "Persistent Volume의 유형.", allowableValues = {"SINGLE","SHARED"}, requiredMode = Schema.RequiredMode.REQUIRED)
    private PersistentVolumeType persistentVolumeType;

    @Schema(description = "Volume의 유형.", allowableValues = {"PERSISTENT_VOLUME_LINKED"}, requiredMode = Schema.RequiredMode.REQUIRED)
    private VolumeType volumeType = VolumeType.PERSISTENT_VOLUME_LINKED;

    @Schema(description = "Persistent Volume Claim 일련 번호")
    private Integer claimSeq;

    @Schema(description = "Persistent Volume 일련 번호")
    @Deprecated
    private Integer volumeSeq;

    @Schema(description = "Storage Volume 이름")
    private String storageVolumeName;

    @Schema(description = "Persistent Volume Claim 이름", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(description = "라벨")
    private Map<String, String> labels;

    @Schema(description = "어노테이션")
    private Map<String, String> annotations;

    @Schema(description = "Persistent Volume 이름")
    private String volumeName;

    @Schema(description = "Persistent Volume 용량. Giga byte 단위", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer capacity;

    @Schema(description = "Acess Mode - ROW(Read Write Once), ROX(Read Only Many), RWX(Read Write Many)", requiredMode = Schema.RequiredMode.REQUIRED,
            allowableValues = {"RWO", "ROX", "RWX"})
    private AccessMode accessMode;

    @Schema(description = "Reclaim Policy", allowableValues = {"RECYCLE", "RETAIN", "DELETE"})
    private ReclaimPolicy reclaimPolicy;

    @Schema(description = "Persistent Volume Claim에 대한 설명.")
    private String description;

    @Schema(description = "Storage class 이름.")
    private String storageClassName;

    private Integer appmapSeq;

    @Schema(description = "Plugin parameter")
    private List<PersistentVolumeParamterVO> parameters;

}
