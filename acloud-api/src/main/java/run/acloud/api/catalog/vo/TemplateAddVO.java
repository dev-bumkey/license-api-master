package run.acloud.api.catalog.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.NotBlank;
import run.acloud.api.catalog.enums.TemplateDeploymentType;
import run.acloud.api.catalog.enums.TemplateShareType;
import run.acloud.api.catalog.enums.TemplateType;
import run.acloud.commons.vo.HasUseYnVO;

@Getter
@Setter
@Schema(description = "템플릿생성 모델")
public class TemplateAddVO extends HasUseYnVO {
	
	@Schema(title = "템플릿 신규 생성 여부", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull
	private boolean isNew;
	
	@Schema(title = "템플릿 번호", description = "신규 생성 시 필요 없음.")
	private Integer templateSeq;

	@Schema(title = "템플릿 이름", description = "기존 템플릿에 버전을 추가하는 경우 필요 없음.")
	private String templateName;

	@Schema(title = "템플릿 유형", description = "PACKAGE,BUILD_PACK,SERVICE", allowableValues = {"PACKAGE","BUILD_PACK","SERVICE"}, example = "SERVICE")
	private TemplateType templateType = TemplateType.SERVICE;

	@Schema(title = "템플릿 서비스 공유 유형", description = "SYSTEM_SHARE,WORKSPACE_SHARE", allowableValues = {"SYSTEM_SHARE","WORKSPACE_SHARE"}, example = "WORKSPACE_SHARE")
	private TemplateShareType templateShareType = TemplateShareType.WORKSPACE_SHARE;

	@Schema(title = "템플릿 버전", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank
	private String version;
	
	@Schema(title = "템플릿 요약", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank
	private String summary;

	@Schema(title = "템플릿 설명", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank
	private String description;

	@Schema(title = "저장 할 대상 서비스맵 번호", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull
	@Min(value = 1)
	private Integer servicemapSeq;

	@Schema(title = "계정 번호")
	private Integer accountSeq;

	@Schema(title = "서비스 번호")
	private Integer serviceSeq;

	@Schema(accessMode = Schema.AccessMode.READ_ONLY)
	private Integer templateVersionSeq;

	@Schema(accessMode = Schema.AccessMode.READ_ONLY)
	private TemplateDeploymentType templateDeploymentType;

	@Schema(accessMode = Schema.AccessMode.READ_ONLY)
	private String templateTypeToString = this.templateType.getCode();

	@Schema(title = "Import Secret Data")
	private boolean isIsd;

	public String getTemplateTypeToString(){
		if (this.templateType != null) {
			return this.templateType.getCode();
		}

		return null;
	}

	@Schema(accessMode = Schema.AccessMode.READ_ONLY)
	private String templateShareTypeToString = this.templateShareType.getCode();

	public String getTemplateShareTypeToString(){
		if (this.templateShareType != null) {
			return this.templateShareType.getCode();
		}

		return null;
	}
}
