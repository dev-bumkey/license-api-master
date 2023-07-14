package run.acloud.api.configuration.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import run.acloud.api.resource.vo.K8sPersistentVolumeClaimVO;
import run.acloud.api.resource.vo.K8sPodVO;
import run.acloud.commons.vo.HasUseYnVO;

import java.util.List;

@Getter
@Setter
@Schema(description = "클러스터 addon 모델")
public class ClusterAddonVO extends HasUseYnVO {

	@Schema(title = "Controller 정보", description = "Deployment, DaemonSet, StatefulSet, CronJob, Job")
	private ClusterAddonControllerVO controller;

	@Schema(title = "Pod 정보")
	private List<K8sPodVO> pods;

	@Schema(title = "Service 정보", description = "Service, Ingress")
	private ClusterAddonServiceVO service;

	@Schema(title = "Config 정보", description = "ConfigMap, Secret")
	private ClusterAddonConfigVO config;

	@Schema(title = "Volume 정보")
	private List<K8sPersistentVolumeClaimVO> volumes;

}

