package run.acloud.api.configuration.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import run.acloud.commons.vo.BaseVO;

@Getter
@Setter
@Schema(description = "클러스터 현황 모델")
public class ClusterConditionVO extends BaseVO {

	@JsonIgnore
	private Integer clusterSeq;

	@Schema(title = "workspace 수")
	private Integer serviceCount = 0;

	@Schema(title = "servicemap 수")
	private Integer servicemapCount = 0;

	@Schema(title = "Namespace 수")
	private Integer namespaceCount = 0;

	@Schema(title = "server 수")
	private Integer serverCount = 0;

	@Schema(title = "pod 수")
	private Integer podCount = 0;

	@Schema(title = "ready Node Count")
	private long readyNodeCount = 0L;

	@Schema(title = "desired Node Count")
	private long desiredNodeCount = 0L;

	@Schema(title = "gpu Node Count")
	private long gpuNodeCount = 0L;

	@Schema(title = "전체 CPU 사용량")
	private double totalCpuUsage = 0.0D;

	@Schema(title = "전체 Memory 사용량")
	private long totalMemUsage = 0L;

	@Schema(title = "전체 Gpu 사용량")
	private int totalGpuUsage = 0;

	@Schema(title = "알람 수")
	private Integer alarmCount = 0;

	@Schema(title = "load balancer 수")
	private Integer loadBalancerCount = 0;

	@Schema(title = "볼륨 PVC 요청량")
	private Integer volumeRequestCapacity = 0;

	@Schema(title = "OpenShift 사용 여부")
	private Boolean openShiftUsage = false;

	@Schema(title = "Kiali URL")
	private String kialiUrl = "";
}
