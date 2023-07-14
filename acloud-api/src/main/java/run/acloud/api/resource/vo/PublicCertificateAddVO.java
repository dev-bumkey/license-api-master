package run.acloud.api.resource.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import run.acloud.commons.vo.HasUpserterVO;

@Getter
@Setter
@Schema(description = "공인인증서 생성 모델")
public class PublicCertificateAddVO extends HasUpserterVO {

	@Schema(title = "플랫폼 번호", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull
	private Integer accountSeq;

	@Schema(title = "공인인증서 번호")
	private Integer publicCertificateSeq;

	@Schema(title = "공인인증서 이름", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull
	private String publicCertificateName;

	@Schema(title = "공인인증서 설명", requiredMode = Schema.RequiredMode.REQUIRED)
	private String description;

	@Schema(title = "ca.crt 인증서", description = "CA 인증서, X509 인증서, PEM 형식 또는 PEM 형식을 base64 인코딩한 문자열")
	private String serverAuthData;

	@Schema(title = "tls.crt 인증서", description = "X509 인증서, PEM 형식 또는 PEM 형식을 base64 인코딩한 문자열")
	private String clientAuthData;

    @Schema(title = "tls.key 개인키", description = "private key, PEM 형식 또는 PEM 형식을 base64 인코딩한 문자열")
	private String clientKeyData;

}

