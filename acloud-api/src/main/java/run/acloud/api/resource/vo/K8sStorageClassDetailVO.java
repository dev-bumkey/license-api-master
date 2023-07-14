package run.acloud.api.resource.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
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
@Schema(description = "Storage Class 상세 모델")
public class K8sStorageClassDetailVO extends BaseVO{

    @Schema(title = "Storage Class 명")
    private String name;

    @Schema(title = "어노테이션")
    private Map<String, String> annotations;

    @Schema(title = "생성 시간")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private OffsetDateTime creationTime;

    @Schema(title = "라벨")
    private Map<String, String> labels;

    @Schema(title = "provisioner", description = "Provisioner indicates the type of the provisioner.")
    private String provisioner;

    @Schema(title = "parameters", description = "Parameters holds the parameters for the provisioner that should create volumes of this storage class.")
    private Map<String, String> parameters;

    @Schema(title = "allowVolumeExpansion", description = "AllowVolumeExpansion shows whether the storage class allow volume expand.")
    private Boolean allowVolumeExpansion;

    @Schema(title = "mountOptions", description = "Dynamically provisioned PersistentVolumes of this storage class are created with these mountOptions, e.g. ['ro', 'soft']. Not validated - mount of the PVs will simply fail if one is invalid.")
    private List<String> mountOptions;

    @Schema(title = "reclaimPolicy", description = "Dynamically provisioned PersistentVolumes of this storage class are created with this reclaimPolicy. Defaults to Delete.")
    private String reclaimPolicy;

    @Schema(title = "volumeBindingMode", description = "VolumeBindingMode indicates how PersistentVolumeClaims should be provisioned and bound.  When unset, VolumeBindingImmediate is used. This field is alpha-level and is only honored by servers that enable the VolumeScheduling feature.")
    private String volumeBindingMode;
}
