package run.acloud.api.cserver.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import run.acloud.api.resource.vo.ContainerVolumeVO;
import run.acloud.api.resource.vo.K8sPersistentVolumeClaimVO;
import run.acloud.api.resource.vo.K8sPodVO;
import run.acloud.api.resource.vo.K8sServiceVO;
import run.acloud.commons.vo.BaseVO;

import java.util.List;

@Getter
@Setter
@Schema(description = "워크로드 모델")
public class WorkloadVO extends BaseVO {

	@Schema(title = "Controller 정보", description = "Deployment, DaemonSet, StatefulSet, CronJob, Job")
	private WorkloadControllerVO controller;

	@Schema(title = "Pod 정보")
	private List<K8sPodVO> pods;

	@Schema(title = "Service 목록")
	private List<K8sServiceVO> services;

	@Schema(title = "Volume Mount 정보")
	private List<ContainerVolumeVO> volumeMounts;

	@Schema(title = "Volume 정보")
	private List<K8sPersistentVolumeClaimVO> volumes;

}

