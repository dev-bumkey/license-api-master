package run.acloud.api.catalog.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import run.acloud.commons.vo.HasUseYnVO;

import java.util.List;

@Getter
@Setter
@Schema(description = "템플릿 유효성 요청 모델")
public class TemplateValidRequestVO extends HasUseYnVO {

	@Schema(title = "서비스 번호", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull
	@Min(value = 1)
	private Integer serviceSeq;

	@Schema(title = "클러스터 번호", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull
	@Min(value = 1)
	private Integer clusterSeq;
	
	@Schema(title = "서비스맵 번호", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull
	@Min(value = 0)
	private Integer servicemapSeq;

	@Schema(title = "탬플릿배치 목록", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull
	@Size(min = 1)
	private List<TemplateDeploymentVO> templateDeployments;

}
