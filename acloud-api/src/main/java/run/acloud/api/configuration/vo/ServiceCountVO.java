package run.acloud.api.configuration.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Schema(description = "워크스페이스 리소스 카운트 모델")
public class ServiceCountVO extends ServiceVO {

	@Schema(title = "클러스터 수")
	private Integer clusterCount;
	@Schema(title = "서비스맵 수")
	private Integer servicemapCount;
}
