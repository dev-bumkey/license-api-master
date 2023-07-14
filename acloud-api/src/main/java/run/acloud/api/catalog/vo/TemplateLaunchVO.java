package run.acloud.api.catalog.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.NotBlank;
import run.acloud.commons.vo.HasUseYnVO;

import java.util.List;

@Getter
@Setter
@Schema(description = "템플릿배치 실행 모델")
public class TemplateLaunchVO extends HasUseYnVO {

	@Schema(title = "실행 종류", description = "N : 신규 servicemap을 생성, A : 기존 servicemap에 추가", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank
	private String launchType;

	@Schema(title = "서비스 순번", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull
	@Min(value = 1)
	private Integer serviceSeq;

	@Schema(title = "클러스터 순번", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull
	@Min(value = 1)
	private Integer clusterSeq;

	@Schema(title = "서비스맵 순번")
	private Integer servicemapSeq;

	@Schema(title = "서비스맵 그룹 순번")
	private Integer servicemapGroupSeq;

	@Schema(title = "서비스맵 이름", requiredMode = Schema.RequiredMode.REQUIRED)
	private String servicemapName;

	@Schema(title = "Namespace 이름", requiredMode = Schema.RequiredMode.REQUIRED)
	private String namespaceName;

	@Schema(title = "탬플릿배치 목록", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull
	@Size(min = 1)
	private List<TemplateDeploymentVO> templateDeployments;

}
