package run.acloud.api.catalog.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import run.acloud.api.catalog.enums.TemplateShareType;
import run.acloud.api.catalog.enums.TemplateType;

@Getter
@Setter
@Schema(description = "템플릿 모델 목록")
public class TemplateListVO {

	@Schema(title = "템플릿 번호")
	private Integer templateSeq;

	@Schema(title = "템플릿 유형")
	private TemplateType templateType;

	@Schema(title = "템플릿 서비스 공유 유형", description = "SYSTEM_SHARE,WORKSPACE_SHARE", allowableValues = {"SYSTEM_SHARE","WORKSPACE_SHARE"}, example = "WORKSPACE_SHARE")
	private TemplateShareType templateShareType = TemplateShareType.WORKSPACE_SHARE;

	@Schema(title = "템플릿 이름")
	private String templateName;

	@Schema(title = "템플릿 버전 번호")
	private Integer templateVersionSeq;

	@Schema(title = "템플릿 요약")
	private String summary;

	@Schema(title = "버전")
	private String version;

	@Schema(title = "템플릿 서버 갯수")
	private Integer templateDeploymentCount;

	@Schema(title = "저장 할 대상 계정 번호")
	private Integer accountSeq;

	@Schema(title = "저장 할 대상 서비스 번호")
	private Integer serviceSeq;

}
