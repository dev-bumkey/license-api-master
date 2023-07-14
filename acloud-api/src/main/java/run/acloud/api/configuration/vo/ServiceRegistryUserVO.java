package run.acloud.api.configuration.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Schema(description = "워크스페이스 레지스트리 사용자 모델")
public class ServiceRegistryUserVO extends ServiceVO {

    @Schema(title = "워크스페이스가 사용할 수 있는 레지스트리 프로젝트 목록", example = "[]")
	private List<ServiceRegistryVO> projects;

	@Schema(title = "워크스페이스가 포함된 외부 레지스트리 목록")
	private List<ExternalRegistryVO> externalRegistries;

}
