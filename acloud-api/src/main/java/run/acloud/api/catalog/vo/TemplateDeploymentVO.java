package run.acloud.api.catalog.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import run.acloud.api.catalog.enums.TemplateDeploymentType;
import run.acloud.commons.vo.HasUseYnVO;

@Getter
@Setter
@Schema(description = "템플릿배치 모델")
public class TemplateDeploymentVO extends HasUseYnVO {
	
	@Schema(title = "템플릿배치 번호", requiredMode = Schema.RequiredMode.REQUIRED)
	private Integer templateDeploymentSeq;

	@Schema(title = "템플릿배치 유형", requiredMode = Schema.RequiredMode.REQUIRED)
	private TemplateDeploymentType templateDeploymentType;

	@Schema(title = "템플릿내용 GUI", requiredMode = Schema.RequiredMode.REQUIRED, description = "서버 배포 설정 GUI 모델 Json")
	private String templateContent;

	@Schema(title = "템플릿내용 JSON", requiredMode = Schema.RequiredMode.REQUIRED, description = "서버 배포 설정 JSON")
	private String templateContentJson;

	@Schema(title = "템플릿내용 YAML", requiredMode = Schema.RequiredMode.REQUIRED, description = "서버 배포 설정 YAML")
	private String templateContentYaml;

	@Schema(title = "워크로드그룹명", requiredMode = Schema.RequiredMode.REQUIRED)
	private String workloadGroupName;
	
	@Schema(title = "정렬순서", requiredMode = Schema.RequiredMode.REQUIRED)
	private int sortOrder;
	
	@Schema(title = "실행순서", requiredMode = Schema.RequiredMode.REQUIRED)
	private int runOrder;

	@Schema(title = "deployType", allowableValues = {"GUI","YAML"}, requiredMode = Schema.RequiredMode.REQUIRED)
	private String deployType;

	@JsonIgnore
	private Integer templateVersionSeq;
	
}
