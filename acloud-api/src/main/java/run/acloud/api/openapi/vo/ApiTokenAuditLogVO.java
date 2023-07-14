package run.acloud.api.openapi.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "API 토큰 감사 로그 모델")
public class ApiTokenAuditLogVO {

    @Schema(title = "API 토큰 감사 로그 순번")
    private Integer apiTokenAuditLogSeq;

    @Schema(title = "로그 일시")
    private String logDatetime;

    @Schema(title = "API 순번")
    private Integer apiSeq;

    @Schema(title = "API 이름")
    private String apiName;

    @Schema(title = "URL")
    private String url;

    @Schema(title = "HTTP 메소드")
    private String httpMethod;

    @Schema(title = "클라이언트 IP")
    private String clientIp;

    @Schema(title = "Referer")
    private String referer;

    @Schema(title = "User Agent")
    private String userAgent;

    @Schema(title = "API 토큰 발급 순번")
    private Integer apiTokenIssueSeq;

    @Schema(title = "API 토큰 이름")
    private String apiTokenName;

    @Schema(title = "플랫폼 순번")
    private Integer accountSeq;

    @Schema(title = "플랫폼 이름")
    private String accountName;

    @Schema(title = "처리 시간")
    private double processingTime;

    @Schema(title = "결과")
    private String result;

    @Schema(title = "요청")
    private String request;

    @Schema(title = "응답")
    private String response;



}
