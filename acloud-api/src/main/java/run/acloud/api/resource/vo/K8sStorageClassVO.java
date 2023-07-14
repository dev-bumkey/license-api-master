package run.acloud.api.resource.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import run.acloud.commons.vo.BaseVO;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * @author: wschoi@acornsoft.io
 * Created on 2017. 10. 12.
 */
@Getter
@Setter
@Schema(description = "Storage Class 모델")
public class K8sStorageClassVO extends BaseVO{

    @Schema(title = "Storage Class 명")
    private String name;

    @Schema(title = "라벨")
    private Map<String, String> labels;

    @Schema(title = "provisioner", description = "Provisioner indicates the type of the provisioner.")
    private String provisioner;

    @Schema(title = "parameters", description = "Parameters holds the parameters for the provisioner that should create volumes of this storage class.")
    private Map<String, String> parameters;

    @Schema(title = "생성시간")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private OffsetDateTime creationTimestamp;

    @Schema(title = "Storage Class 배포 정보")
    private String deployment;

    @Schema(title = "Storage Class 배포 정보 (yaml)")
    private String deploymentYaml;

    @Schema(title = "Storage Class 상세")
    private K8sStorageClassDetailVO detail;
}
