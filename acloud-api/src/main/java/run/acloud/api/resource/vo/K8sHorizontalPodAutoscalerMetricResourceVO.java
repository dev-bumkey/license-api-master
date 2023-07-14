package run.acloud.api.resource.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(name = "K8sHorizontalPodAutoscalerMetricResourceVO", title = "K8sHorizontalPodAutoscalerMetricResourceVO", allOf = {K8sHorizontalPodAutoscalerMetricVO.class}, description = "PodsMetricSource indicates how to scale on a metric describing each pod in the current scale target (for example, transactions-processed-per-second). The values will be averaged together before being compared to the target value.")
public class K8sHorizontalPodAutoscalerMetricResourceVO extends K8sHorizontalPodAutoscalerMetricVO {

	@Schema(title = "name", description = "name is the name of the resource in question.")
	private String name;

}
