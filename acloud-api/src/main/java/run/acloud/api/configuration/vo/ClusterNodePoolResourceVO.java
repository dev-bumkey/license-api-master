package run.acloud.api.configuration.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import run.acloud.commons.vo.BaseVO;

@Getter
@Setter
@Schema(description = "클러스터 노드 풀 리소스 모델")
public class ClusterNodePoolResourceVO extends BaseVO {

	@Schema(title = "용량")
	private long allocatable = 0L;

	@Schema(title = "가용량")
	private long capacity = 0L;

	@Schema(title = "요청량")
	private long request = 0L;
}
