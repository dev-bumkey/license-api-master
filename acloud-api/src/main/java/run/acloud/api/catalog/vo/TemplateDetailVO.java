package run.acloud.api.catalog.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import run.acloud.api.catalog.enums.TemplateShareType;
import run.acloud.api.catalog.enums.TemplateType;
import run.acloud.commons.vo.HasUseYnVO;

import java.util.List;

@Getter
@Setter
@Schema(description = "템플릿 상세정보 모델")
public class TemplateDetailVO extends HasUseYnVO {
	
	@Schema(title = "템플릿 번호", requiredMode = Schema.RequiredMode.REQUIRED)
	private Integer templateSeq;

	@Schema(title = "템플릿 유형", description = "COCKTAIL, SERVICE")
	private TemplateType templateType;

	@Schema(title = "템플릿 서비스 공유 유형", description = "SYSTEM_SHARE,WORKSPACE_SHARE", allowableValues = {"SYSTEM_SHARE","WORKSPACE_SHARE"}, example = "WORKSPACE_SHARE")
	private TemplateShareType templateShareType = TemplateShareType.WORKSPACE_SHARE;

	@Schema(title = "템플릿 이름", requiredMode = Schema.RequiredMode.REQUIRED)
	private String templateName;
	
	@Schema(title = "템플릿버전 번호", requiredMode = Schema.RequiredMode.REQUIRED)
	private Integer templateVersionSeq;
	
	@Schema(title = "버전", requiredMode = Schema.RequiredMode.REQUIRED)
	private String version;
	
	@Schema(title = "요약", requiredMode = Schema.RequiredMode.REQUIRED)
	private String summary;

	@Schema(title = "설명", requiredMode = Schema.RequiredMode.REQUIRED)
	private String description;

	@Schema(title = "템플릿버전 목록", requiredMode = Schema.RequiredMode.REQUIRED)
	private List<TemplateVersionVO> templateVersions;
	
	@Schema(title = "템플릿배치 목록", requiredMode = Schema.RequiredMode.REQUIRED)
	private List<TemplateDeploymentVO> templateDeployments;
}
