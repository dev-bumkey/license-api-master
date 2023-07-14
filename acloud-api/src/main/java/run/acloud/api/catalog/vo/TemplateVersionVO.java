package run.acloud.api.catalog.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import run.acloud.commons.vo.HasUseYnVO;

@Getter
@Setter
@Schema(description = "템플릿버전 모델")
public class TemplateVersionVO extends HasUseYnVO {
	
	@Schema(title = "템플릿 버전 번호", requiredMode = Schema.RequiredMode.REQUIRED)
	private Integer templateVersionSeq;

	@Schema(title = "템플릿 버전", requiredMode = Schema.RequiredMode.REQUIRED)
	private String version;
	
}
