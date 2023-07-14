package run.acloud.api.configuration.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import run.acloud.api.resource.vo.*;
import run.acloud.commons.vo.HasUseYnVO;

import java.util.List;

@Getter
@Setter
@Schema(description = "클러스터 addon controller 모델")
public class ClusterAddonControllerVO extends HasUseYnVO {

	@Schema(title = "Deployment 목록")
	private List<K8sDeploymentVO> deployments;

	@Schema(title = "ReplicaSet 목록")
	private List<K8sReplicaSetVO> replicaSets;

	@Schema(title = "DaemonSet 목록")
	private List<K8sDaemonSetVO> daemonSets;

	@Schema(title = "StatefulSet 목록")
	private List<K8sStatefulSetVO> statefulSets;

	@Schema(title = "CronJob 목록")
	private List<K8sCronJobVO> cronJobs;

	@Schema(title = "Job 목록")
	private List<K8sJobVO> jobs;

}

