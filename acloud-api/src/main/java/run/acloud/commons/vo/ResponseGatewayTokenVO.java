package run.acloud.commons.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Schema(description ="Gateway Token 요청 Model by serverless")
@Setter
@Getter
public class ResponseGatewayTokenVO<T> {

    @Schema(description = "처리 상태. ok | error", allowableValues = {"ok","error"})
    private String status = "ok";

    @Schema(description = "상세 결과")
    private T result;
}
