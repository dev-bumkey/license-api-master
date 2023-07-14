package run.acloud.api.openapi.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "API 토큰 감사 로그 생성 모델")
public class ApiTokenAuditLogAddVO {

    @Schema(title = "API 토큰 감사 로그 순번", hidden = true)
    @JsonIgnore
    private Integer apiTokenAuditLogSeq;

    @Schema(title = "API 순번")
    private Integer apiSeq;

    @Schema(title = "URL", requiredMode = Schema.RequiredMode.REQUIRED)
    private String url;

    @Schema(title = "HTTP 메소드", requiredMode = Schema.RequiredMode.REQUIRED)
    private String httpMethod;

    @Schema(title = "클라이언트 IP", requiredMode = Schema.RequiredMode.REQUIRED)
    private String clientIp;

    @Schema(title = "Referer")
    private String referer;

    @Schema(title = "User Agent")
    private String userAgent;

    @Schema(title = "API 토큰 발급 순번")
    private Integer apiTokenIssueSeq;

    @Schema(title = "플랫폼 순번")
    private Integer accountSeq;

    @Schema(title = "처리 시간")
    private double processingTime;

    @Schema(title = "결과", allowableValues = {"SUCCESS", "FAILURE"}, requiredMode = Schema.RequiredMode.REQUIRED)
    private String result;

    @Schema(title = "요청", requiredMode = Schema.RequiredMode.REQUIRED)
    private String request;

    @Schema(title = "응답", requiredMode = Schema.RequiredMode.REQUIRED)
    private String response;



}
