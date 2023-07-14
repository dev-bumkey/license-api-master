package run.acloud.api.resource.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "공인인증서 상세 모델")
public class PublicCertificateDetailVO extends PublicCertificateVO {

	@Schema(title = "ca.crt 인증서 상세 정보")
	private PublicCertificateCertInfoVO serverAuthDetail;

	@Schema(title = "tls.crt 인증서 상세 정보")
	private PublicCertificateCertInfoVO clientAuthDetail;
}

