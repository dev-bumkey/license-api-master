package run.acloud.api.resource.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
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
@Schema(description = "PersistentVolumeClaim 상세 모델")
public class K8sPersistentVolumeClaimDetailVO extends BaseVO {

    @Schema(title = "네임스페이스")
    private String namespace;

    @Schema(title = "PVC 명")
    private String name;

    @Schema(title = "라벨")
    private Map<String, String> labels;

    @Schema(title = "어노테이션")
    private Map<String, String> annotations;

    @Schema(title = "생성 시간")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private OffsetDateTime creationTime;

    @Schema(title = "Selector")
    private K8sLabelSelectorVO selector;

    @Schema(title = "PVC 상태")
    private String status;

    @Schema(title = "PV 명")
    private String volumeName;

    @Schema(title = "Access Modes")
    private List<String> accessModes;

    @Schema(title = "Storage Class 명")
    @JsonProperty(defaultValue = "-")
    private String storageClassName;

    @Schema(title = "용량")
    private Map<String, String> capacity;

    @Schema(title = "이벤트")
    private List<K8sEventVO> events;
}
