package run.acloud.api.openapi.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.math.BigInteger;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@Schema(description = "API 토큰 호출 갯수 모델")
public class ApiTokenRequestCountVO {

    @Schema(title = "API 토큰 발급 순번")
    private Integer apiTokenIssueSeq;

    @Schema(title = "플랫폼 순번")
    private Integer accountSeq;

    @Schema(title = "현재 호출 건수")
    private BigInteger currRequestCount;

    @Schema(title = "총 호출 건수")
    private BigInteger totalRequestCount;

}
