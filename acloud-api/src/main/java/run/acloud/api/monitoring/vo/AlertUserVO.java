package run.acloud.api.monitoring.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.NotBlank;
import run.acloud.commons.vo.HasUseYnVO;

@Getter
@Setter
@EqualsAndHashCode
@Schema(description = "Alert 사용자 모델")
public class AlertUserVO extends HasUseYnVO {

	@Schema(title = "사용자 일련번호", requiredMode = Schema.RequiredMode.REQUIRED)
	private Integer alertUserSeq;

	@Schema(title = "계정번호", requiredMode = Schema.RequiredMode.REQUIRED)
	private Integer accountSeq;

	@Schema(title = "사용자 ID(email 형식)")
	private String userId;

	@Schema(title = "사용자 이름", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank
	private String userName;

	@Schema(title = "사용자 직책")
	private String userJob;

	@Schema(title = "전화번호")
	private String phoneNumber;

	@Schema(title = "카카오아이디")
	private String kakaoId;

	@Schema(title = "이메일")
	private String email;

	@Schema(title = "설명")
	private String description;
}
