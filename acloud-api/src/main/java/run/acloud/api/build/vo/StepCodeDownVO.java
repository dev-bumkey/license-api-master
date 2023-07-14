package run.acloud.api.build.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.NotBlank;
import run.acloud.api.build.enums.RepositoryType;

@Getter
@Setter
@Schema(name = "StepCodeDownVO", title = "StepCodeDownVO", allOf = {BuildStepAddVO.class}, description = "코드 다운 모델")
public class StepCodeDownVO extends BuildStepAddVO {

	@Schema(title = "레파지토리 종류", requiredMode = Schema.RequiredMode.REQUIRED)
	@Valid
	@NotNull
	private RepositoryType repositoryType;

	@Schema(title = "레파지토리 URL", requiredMode = Schema.RequiredMode.REQUIRED)
	@Valid
	@NotBlank
	private String repositoryUrl;

	@Schema(title = "Common저장소 사용여부", allowableValues = {"COMMON","PRIVATE"}, description = "체크박스 선택시 [COMMON] 저장소 체크 , 미 선택시 [PRIVATE]", requiredMode = Schema.RequiredMode.REQUIRED)
	@Valid
	@NotBlank
	private String commonType;

	@Schema(title = "프로토콜 사용", allowableValues = {"http","https"}, description = "[http,https] 라디오 버튼 프로프톨 Type 선택", requiredMode = Schema.RequiredMode.REQUIRED)
	@Valid
	@NotBlank
	private String protocolType;

	@Schema(title = "사용자 ID")
	private String userId;

	@Schema(title = "사용자 비밀번호")
	private String password;

	@Schema(title = "브랜치 이름", requiredMode = Schema.RequiredMode.REQUIRED)
	@Valid
	@NotBlank
	private String branchName;

	@Schema(title = "코드 저장 디렉토리")
	private String codeSaveDir;

	@Schema(title = "인증서 검증 skip")
	private boolean httpSslVerifySkip = false;

}
