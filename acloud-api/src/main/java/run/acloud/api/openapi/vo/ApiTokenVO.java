package run.acloud.api.openapi.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@Schema(description = "API 토큰 모델")
public class ApiTokenVO {
    @Schema(title = "API 토큰 발급 순번")
    private Integer apiTokenIssueSeq;

    @Schema(title = "API 토큰 이름")
    private String apiTokenName;

    @Schema(title = "토큰")
    private String token;
}
