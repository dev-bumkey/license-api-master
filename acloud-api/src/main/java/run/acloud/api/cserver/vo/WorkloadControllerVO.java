package run.acloud.api.cserver.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import run.acloud.api.resource.vo.*;
import run.acloud.commons.vo.BaseVO;

import java.util.List;

@Getter
@Setter
@Schema(description = "워크로드 controller 모델")
public class WorkloadControllerVO extends BaseVO {

	@Schema(title = "Deployment")
	private K8sDeploymentVO deployment;

	@Schema(title = "Deployment에 대한 ReplicaSet 목록")
	private List<K8sReplicaSetVO> replicaSets;

	@Schema(title = "DaemonSet")
	private K8sDaemonSetVO daemonSet;

	@Schema(title = "StatefulSet")
	private K8sStatefulSetVO statefulSet;

	@Schema(title = "CronJob")
	private K8sCronJobVO cronJob;

	@Schema(title = "CronJob에 대한 Job 목록")
	private List<K8sJobVO> jobs;

	@Schema(title = "Job")
	private K8sJobVO job;

	@Schema(title = "WorkloadType")
	private String workloadType;
}