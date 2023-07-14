package run.acloud.api.build.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import lombok.Getter;
import lombok.Setter;
import run.acloud.api.build.enums.HttpMode;

import java.util.List;

@Getter
@Setter
@Schema(name = "StepHttpVO", title = "StepHttpVO", allOf = {BuildStepAddVO.class}, description = "HTTP 연동 작업 모델")
public class StepHttpVO extends BuildStepAddVO {

    @Schema(title = "HTTP 연동 host URL", requiredMode = Schema.RequiredMode.REQUIRED)
    @Valid
    @NotNull
    private String url;

    @Schema(title = "HTTP Method", requiredMode = Schema.RequiredMode.REQUIRED)
    @Valid
    @NotNull
    private HttpMode httpMode; // HttpMethod enum class

    @Schema(title = "HTTP Request Header list", requiredMode = Schema.RequiredMode.REQUIRED)
    @Valid
    @Null
    private List<HttpHeader> customHeaders;

    @Schema(title = "HTTP SSL 연동 에러 무시여부", requiredMode = Schema.RequiredMode.REQUIRED)
    @Valid
    @Null
    private boolean ignoreSslErrors;

    @Schema(title = "HTTP 인증 username", required = false)
    @Valid
    @Null
    private String username;

    @Schema(title = "HTTP 인증 password", required = false)
    @Valid
    @Null
    private String password;

    @Schema(title = "HTTP Request Connection timeout", requiredMode = Schema.RequiredMode.REQUIRED)
    @Valid
    @NotNull
    private int timeout;

    @Schema(title = "HTTP Response 결과 검증 HTTP Codes", required = false)
    @Valid
    @Null
    private String validResponseCodes;

    @Schema(title = "HTTP Response 결과 검증 Content", required = false)
    @Valid
    @Null
    private String validResponseContent;

    @Schema(title = "HTTP Request 요청 결과 파일 path", required = false)
    @Valid
    @Null
    private String outputFile;

    @Schema(title = "HTTP Request Response body log 출력 여부", requiredMode = Schema.RequiredMode.REQUIRED)
    @Valid
    @Null
    private boolean consoleLogResponseBody;

    @Schema(title = "HTTP Request Body", required = false)
    @Valid
    @Null
    private String requestBody;

}
