package run.acloud.api.configuration.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import run.acloud.api.resource.vo.K8sNodeVO;
import run.acloud.commons.vo.BaseVO;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@Schema(description = "클러스터 노드 풀 모델")
public class ClusterNodePoolVO extends BaseVO {

	@Schema(title = "클러스터 번호")
	private Integer clusterSeq;

	@Schema(title = "노드 풀 명")
	private String nodePoolName;

	@Schema(title = "노드 풀 설명")
	private String nodePoolDesc;

	@Schema(title = "클러스터 상태")
	private String clusterState;

	@Schema(title = "ready Node Count")
	private long readyNodeCount = 0L;

	@Schema(title = "desired Node Count")
	private long desiredNodeCount = 0L;

	@Schema(title = "gpu resource 명")
	private String gpuResourceName;

	@Schema(title = "gpu 요청")
	private int gpuRequests = 0;

	@Schema(title = "gpu 용량")
	private int gpuCapacity = 0;

	@Schema(title = "cpu 요청")
	private long cpuRequests = 0L;

	@Schema(title = "cpu 용량")
	private long cpuCapacity = 0L;

	@Schema(title = "memory 요청")
	private long memoryRequests = 0L;

	@Schema(title = "memory 용량")
	private long memoryCapacity = 0L;

	@Schema(title = "resources", description = "resource별 용량")
	private Map<String, ClusterNodePoolResourceVO> resources;

	@Schema(title = "노드 목록")
	private List<K8sNodeVO> nodes;
}
