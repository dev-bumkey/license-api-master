package run.acloud.commons.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Schema(title="Gateway Token Model")
@Setter
@Getter
public class GatewayTokenVO {

    @Schema(title = "token")
    private String access_token;

    @Schema(title = "토큰이 발급된 시간 (issued at)")
    private Long iat;
}
