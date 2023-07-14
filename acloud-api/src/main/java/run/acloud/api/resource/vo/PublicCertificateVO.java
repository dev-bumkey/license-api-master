package run.acloud.api.resource.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import run.acloud.commons.vo.HasUpserterVO;

@Getter
@Setter
@Schema(description = "공인인증서 모델")
public class PublicCertificateVO extends HasUpserterVO {

	@Schema(title = "플랫폼 번호", requiredMode = Schema.RequiredMode.REQUIRED)
	private Integer accountSeq;

	@Schema(title = "공인인증서 번호")
	private Integer publicCertificateSeq;

	@Schema(title = "공인인증서 이름", requiredMode = Schema.RequiredMode.REQUIRED)
	private String publicCertificateName;

	@Schema(title = "공인인증서 설명", requiredMode = Schema.RequiredMode.REQUIRED)
	private String description;

	@Schema(title = "ca.crt 인증서", description = "내부 조회용")
	@JsonIgnore
	private String serverAuthData;

	@Schema(title = "ca.crt 인증서", description = "CA 인증서, X509 인증서, PEM 형식")
	private String serverAuth;

	@Schema(title = "tls.crt 인증서", description = "내부 조회용")
	@JsonIgnore
	private String clientAuthData;

	@Schema(title = "tls.crt 인증서", description = "X509 인증서, PEM 형식")
	private String clientAuth;

    @Schema(title = "tls.key 개인키", description = "내부 조회용")
	@JsonIgnore
	private String clientKeyData;

    @Schema(title = "tls.key 개인키", description = "private key, PEM 형식")
	private String clientKey;

}

