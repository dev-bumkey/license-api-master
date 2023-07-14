package run.acloud.api.resource.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import run.acloud.commons.vo.BaseVO;

import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Setter
public class K8sServiceAccountVO extends BaseVO{

    @Schema(title = "네임스페이스")
    private String namespace;

    @Schema(title = "Service Account 명")
    private String serviceAccountName;

    @Schema(title = "생성시간")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private OffsetDateTime creationTimestamp;

    @Schema(title = "Secret 명")
    private List<String> secrets;

}
