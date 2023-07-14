package run.acloud.api.serverless.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import run.acloud.commons.vo.BaseVO;

@Getter
@Setter
@Schema(description = "서비리스 프로젝트 생성 모델")
public class GatewayCertificateVO extends BaseVO {
	
	@Schema(title = "게이트웨이 인증서 순번", accessMode = Schema.AccessMode.READ_ONLY)
	@JsonIgnore
	private Integer gatewayCertificateSeq;

	@Schema(title = "인증서", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull
	private String certificate;

	@Schema(accessMode = Schema.AccessMode.READ_ONLY)
	@JsonIgnore
	private String created;

}
