package run.acloud.api.catalog.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import run.acloud.api.resource.vo.*;

import java.util.List;

@Getter
@Setter
@Schema(description = "Helm Package Resources 모델")
public class HelmResourcesVO extends HelmReleaseBaseVO {
	private static final long serialVersionUID = -1383668035824729297L;

	@Schema(title = "Controller 정보", description = "Deployment, DaemonSet, StatefulSet, CronJob, Job")
	private K8sControllersVO controllers;

	@Schema(title = "Pod 정보")
	private List<K8sPodVO> pods;

	@Schema(title = "Service 목록")
	private List<K8sServiceVO> services;

	@Schema(title = "Ingress 목록")
	private List<K8sIngressVO> ingresses;

	@Schema(title = "ConfigMap 목록")
	private List<ConfigMapGuiVO> configMaps;

	@Schema(title = "Secret 목록")
	private List<SecretGuiVO> secrets;

	@Schema(title = "Volume 정보")
	private List<K8sPersistentVolumeClaimVO> volumes;

}

