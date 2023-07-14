package run.acloud.api.serverless.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import run.acloud.commons.vo.HasCreatorVO;

@Getter
@Setter
@Schema(description = "서비리스 Function 생성 모델")
public class FunctionAddVO extends HasCreatorVO {
	
	@Schema(title = "사용자 ID", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull
	private String userId;

	@Schema(title = "클러스터 ID", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull
	private String clusterId;

	@Schema(title = "프로젝트 이름", description = "서비스맵, 네임스페이스명과 동일한 자원명", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull
	private String projectName;

	@Schema(title = "Function명", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull
	private String functionName;

}
