package run.acloud.api.build.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.NotBlank;

import java.util.List;

@Getter
@Setter
@Schema(name = "StepUserTaskVO",title = "StepUserTaskVO", allOf = {BuildStepAddVO.class}, description = "Build 사용자 작업 모델")
public class StepUserTaskVO extends BuildStepAddVO {

	@Schema(title = "빌드 커멘드", description = "['mvn','clean','install']")
	private List<String> cmd;

	@Schema(title = "작업 디렉토리")
	private String workingDir;

	@Schema(title = "이미지 이름", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank
	private String imageName;

	@Schema(title = "이미지 태그", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank
	private String imageTag;

	@Schema(title = "도커 볼륨 마운트 목록")
	private List<UserTaskVolumeMount> dockerVolumeMountVOList;

	@Schema(title = "도커 환경 변수 목록", description = "dockerEnvList key=asdf, value=asdf", required = false)
	private List<UserTaskEnv> envs;

}
