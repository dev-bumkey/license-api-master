package run.acloud.api.resource.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@Schema(name = "K8sHorizontalPodAutoscalerMetricObjectVO", title = "K8sHorizontalPodAutoscalerMetricObjectVO", allOf = {K8sHorizontalPodAutoscalerMetricVO.class}, description = "ObjectMetricSource indicates how to scale on a metric describing a kubernetes object (for example, hits-per-second on an Ingress object).")
public class K8sHorizontalPodAutoscalerMetricObjectVO extends K8sHorizontalPodAutoscalerMetricVO {

	@Schema(title = "metricName", description = "metricName is the name of the metric in question.")
	private String metricName;

	@Schema(title = "describedObject, e.g) CrossVersionObjectReference", description = "target is the described Kubernetes object.")
	private Map<String, String> describedObject;

	@Schema(title = "selector", description = "selector is the string-encoded form of a standard kubernetes label selector for the given metric When set, it is passed as an additional parameter to the metrics server for more specific metrics scoping. When unset, just the metricName will be used to gather metrics.")
	private K8sLabelSelectorVO selector;

}
