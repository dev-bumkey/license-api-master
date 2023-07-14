package run.acloud.api.resource.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@Schema(description = "공인인증서 인증서상세 모델")
public class PublicCertificateCertInfoVO {

	@Schema(title = "isCA")
	private boolean isCA;

	@Schema(title = "Version")
	private int version;

	@Schema(title = "Subject DN")
	private String subjectDN;

	@Schema(title = "Issuer DN")
	private String issuerDN;

	@Schema(title = "Serial Number")
	private String serialNumber;

	@Schema(title = "Not Before")
	private Date notBefore;

	@Schema(title = "Not After")
	private Date notAfter;

	@Schema(title = "Signature Algorithm")
	private String signatureAlgorithm;

}

