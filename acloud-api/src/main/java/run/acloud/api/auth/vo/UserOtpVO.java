package run.acloud.api.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserOtpVO {

	@Schema(title = "사용자 일련번호", requiredMode = Schema.RequiredMode.REQUIRED)
	private Integer userSeq;

    @Schema(title = "OTP 사용 여부", allowableValues = {"Y","N"})
    private String otpUseYn;

    @Schema(title = "OTP QR")
    private String otpQr;

    @Schema(title = "OTP Secret")
    private String otpSecret;

    @Schema(title = "OTP Url")
    private String otpUrl;

}
