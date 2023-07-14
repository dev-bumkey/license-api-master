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
@Schema(description = "템플릿배치 편집 모델")
public class TemplateEditVO extends HasUseYnVO {

	@Schema(title = "템플릿 번호", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull
	@Min(value = 1)
	private Integer templateSeq;

	@Schema(title = "템플릿버전 번호", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull
	@Min(value = 1)
	private Integer templateVersionSeq;

	@Schema(title = "요약", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank
	private String summary;

	@Schema(title = "설명", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank
	private String description;

	@Schema(title = "탬플릿배치 목록", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull
	@Size(min = 1)
	private List<TemplateDeploymentVO> templateDeployments;

}
