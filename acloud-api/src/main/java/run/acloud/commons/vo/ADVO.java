package run.acloud.commons.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Schema(title="AD Model", description="AD 인증 정보")
@Setter
@Getter
public class ADVO {

    @Schema(name = "User Id")
    private String userId;

    @Schema(name = "User Password")
    private String password;

    @Schema(name = "OTP")
    private String otp;
}
