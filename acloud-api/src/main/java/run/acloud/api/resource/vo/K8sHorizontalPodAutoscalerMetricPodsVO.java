package run.acloud.api.resource.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(name = "K8sHorizontalPodAutoscalerMetricPodsVO", title = "K8sHorizontalPodAutoscalerMetricPodsVO", allOf = {K8sHorizontalPodAutoscalerMetricVO.class}, description = "PodsMetricSource indicates how to scale on a metric describing each pod in the current scale target (for example, transactions-processed-per-second). The values will be averaged together before being compared to the target value.")
public class K8sHorizontalPodAutoscalerMetricPodsVO extends K8sHorizontalPodAutoscalerMetricVO {

	@Schema(title = "metricName", description = "metricName is the name of the metric in question.")
	private String metricName;

	@Schema(title = "selector", description = "selector is the string-encoded form of a standard kubernetes label selector for the given metric When set, it is passed as an additional parameter to the metrics server for more specific metrics scoping. When unset, just the metricName will be used to gather metrics.")
	private K8sLabelSelectorVO selector;
}
