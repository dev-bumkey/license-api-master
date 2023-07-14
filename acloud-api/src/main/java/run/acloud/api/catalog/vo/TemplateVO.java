package run.acloud.api.catalog.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import run.acloud.commons.vo.HasUseYnVO;

@Getter
@Setter
@Schema(description = "템플릿 모델")
public class TemplateVO extends HasUseYnVO {
	
	@Schema(accessMode = Schema.AccessMode.READ_ONLY)
	private Integer templateSeq;

	@NotNull
	private String templateName;

	@NotNull
	private String templateType;

	@NotNull
	private String templateShareType;

	@NotNull
	private Integer accountSeq;

	@NotNull
	private Integer serviceSeq;
}
