package run.acloud.api.resource.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(name = "K8sHorizontalPodAutoscalerMetricContainerVO", title = "K8sHorizontalPodAutoscalerMetricContainerVO", allOf = {K8sHorizontalPodAutoscalerMetricVO.class}, description = "ContainerResourceMetricSource indicates how to scale on a resource metric known to Kubernetes, as specified in requests and limits, describing each pod in the current scale target (e.g. CPU or memory).  The values will be averaged together before being compared to the target.  Such metrics are built in to Kubernetes, and have special scaling options on top of those available to normal per-pod metrics using the 'pods' source.  Only one 'target' type should be set.")
public class K8sHorizontalPodAutoscalerMetricContainerVO extends K8sHorizontalPodAutoscalerMetricVO {

	@Schema(title = "container", description = "container is the name of the container in the pods of the scaling target.")
	private String container;

	@Schema(title = "name", description = "name is the name of the resource in question.")
	private String name;
}
