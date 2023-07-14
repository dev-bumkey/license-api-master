package run.acloud.api.build.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.NotBlank;
import run.acloud.api.build.enums.DockerFileType;

@Getter
@Setter
@Schema(name = "StepCreateImageVO", title = "StepCreateImageVO", allOf = {BuildStepAddVO.class}, description = "어플리케이션 이미지 생성 모델")
public class StepCreateImageVO extends BuildStepAddVO {

	@Schema(title = "도커파일 내용", description = "dockerFileType이 CONTENTS일 때만 존재함.", requiredMode = Schema.RequiredMode.REQUIRED)
	private String dockerFile;

	@Schema(title = "레지스트리 ID", description = "e.g) projectId", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank
	private String registryId;

	@Schema(title = "레지스트리 이름", description = "e.g) projectName", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank
	@Size(max = 256)
	private String registryName;

	@Schema(title = "이미지 이름", description = "레지스트리 이름을 제외한 이미지 이름", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank
	@Size(max = 256)
	private String imageName;

	@Schema(title = "이미지 태그")
	@Size(max = 256)
	private String imageTag;

	@Schema(title = "이미지 latest 적용 여부", requiredMode = Schema.RequiredMode.REQUIRED)
	private boolean latestApply=false;


	@Schema(title = "이미지 path")
	private String image;

	@Schema(title = "레지스트리 URL", description = "e.g) Image Registry URL")
	private String registryUrl;

	@Schema(title = "레지스트리 loginId")
	private String loginId;

	@Schema(title = "레지스트리 Password")
	private String password;

	@Schema(title = "private 레지스트리 CA")
	private String privateCertificate;

	@Schema(title = "도커파일 유형", description = "dockerfile 내용을 직접 입력 or 존재하는 dockerfile의 path를 입력")
	private DockerFileType dockerFileType = DockerFileType.CONTENTS;

	@Schema(title = "도커파일 Path", description = "존재하는 dockerfile의 path를 입력")
	private String dockerFilePath;

	@Schema(title = "Image full URL", description = "Image push 위한 ImageURL, 빌드 실행시에만 생성되는 정보임.")
	private String imageUrl; // 이미지 full url

	@Schema(title = "레지스트리 번호", description = "빌드 이미지 저장할 레지스트리 번호")
	private Integer externalRegistrySeq;

	@Schema(title = "이미지 체크 레벨", description = "빌드 이미지벨 체크시 오류 여부 판단 level")
	private String severity;

	@Schema(title = "이미지 체크시 종료코드", description = "이미지 체크시 종료코드")
	private String exitCode;

}
