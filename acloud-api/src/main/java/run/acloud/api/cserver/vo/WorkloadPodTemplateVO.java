package run.acloud.api.cserver.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import run.acloud.api.resource.vo.K8sPodTemplateSpecVO;
import run.acloud.commons.vo.BaseVO;

@Getter
@Setter
@Schema(description = "워크로드 PodTemplate 모델")
public class WorkloadPodTemplateVO extends BaseVO {

	@Schema(title = "WorkloadType")
	private String workloadType;

	@Schema(title = "workloadName")
	private String workloadName;

	@Schema(title = "podTemplate")
	private K8sPodTemplateSpecVO podTemplate;
}