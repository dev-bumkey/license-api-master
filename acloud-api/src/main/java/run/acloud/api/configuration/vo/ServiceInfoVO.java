package run.acloud.api.configuration.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import run.acloud.api.resource.vo.ComponentVO;

import java.util.List;

@Getter
@Setter
@Schema(description = "서비스 정보 모델")
public class ServiceInfoVO {
	
	@Schema(title = "서비스 번호")
	private Integer serviceSeq;

	@Schema(title = "서비스 이름")
	private String serviceName;
	
	@Schema(title = "앱맵 번호")
	private Integer appmapSeq;

	@Schema(title = "앱맵 이름")
	private String appmapName;

    @Schema(title = "서버 정보")
	private List<ComponentVO> components;
}
