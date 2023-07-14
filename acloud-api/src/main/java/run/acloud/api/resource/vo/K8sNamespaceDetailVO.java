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
@Schema(description = "Namespace 상세 모델")
public class K8sNamespaceDetailVO extends BaseVO {

    @Schema(title = "네임스페이스")
    private String name;

    @Schema(title = "라벨")
    private Map<String, String> labels;

    @Schema(title = "주석")
    private Map<String, String> annotations;

    @Schema(title = "생성시간")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private OffsetDateTime creationTime;

    @Schema(title = "status")
    private String status;
}
