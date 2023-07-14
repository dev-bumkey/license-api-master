package run.acloud.api.resource.vo;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import run.acloud.api.resource.enums.MetricTargetType;
import run.acloud.api.resource.enums.MetricType;

/**
 * @author: wschoi@acornsoft.io
 * Created on 2017. 10. 12.
 */
@Getter
@Setter
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "metricType",
        visible = true
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = K8sHorizontalPodAutoscalerMetricObjectVO.class   , name = MetricType.Names.Object            ),
        @JsonSubTypes.Type(value = K8sHorizontalPodAutoscalerMetricPodsVO.class     , name = MetricType.Names.Pods              ),
        @JsonSubTypes.Type(value = K8sHorizontalPodAutoscalerMetricResourceVO.class , name = MetricType.Names.Resource          ),
        @JsonSubTypes.Type(value = K8sHorizontalPodAutoscalerMetricExternalVO.class , name = MetricType.Names.External          ),
        @JsonSubTypes.Type(value = K8sHorizontalPodAutoscalerMetricContainerVO.class, name = MetricType.Names.ContainerResource )
})
@Schema(title = "K8sHorizontalPodAutoscalerMetricVO",
        description = "Horizontal Pod Autoscaler 모델",
        discriminatorProperty = "metricType",
        discriminatorMapping = {
                @DiscriminatorMapping(value = MetricType.Names.Object            , schema = K8sHorizontalPodAutoscalerMetricObjectVO.class   ),
                @DiscriminatorMapping(value = MetricType.Names.Pods              , schema = K8sHorizontalPodAutoscalerMetricPodsVO.class     ),
                @DiscriminatorMapping(value = MetricType.Names.Resource          , schema = K8sHorizontalPodAutoscalerMetricResourceVO.class ),
                @DiscriminatorMapping(value = MetricType.Names.External          , schema = K8sHorizontalPodAutoscalerMetricExternalVO.class ),
                @DiscriminatorMapping(value = MetricType.Names.ContainerResource , schema = K8sHorizontalPodAutoscalerMetricContainerVO.class)
        },
        subTypes = {K8sHorizontalPodAutoscalerMetricObjectVO.class, K8sHorizontalPodAutoscalerMetricPodsVO.class, K8sHorizontalPodAutoscalerMetricResourceVO.class, K8sHorizontalPodAutoscalerMetricExternalVO.class, K8sHorizontalPodAutoscalerMetricContainerVO.class}
)
public class K8sHorizontalPodAutoscalerMetricVO{

    @Schema(title = "type is the type of metric source", allowableValues = {MetricType.Names.Object, MetricType.Names.Pods, MetricType.Names.Resource, MetricType.Names.External}, description = "type is the type of metric source.  It should be one of Object, Pods or Resource, each mapping to a matching field in the object.", requiredMode = Schema.RequiredMode.REQUIRED)
    private String metricType;

    @Schema(title = "type represents whether the metric type is Utilization, Value, or AverageValue", allowableValues = {MetricTargetType.Names.Utilization, MetricTargetType.Names.Value, MetricTargetType.Names.AverageValue}, description = "type represents whether the metric type is Utilization, Value, or AverageValue", requiredMode = Schema.RequiredMode.REQUIRED)
    private String targetType;

    @Schema(title = "targetAverageUtilization", description = "averageUtilization is the target value of the average of the resource metric across all relevant pods, represented as a percentage of the requested value of the resource for the pods. Currently only valid for Resource metric source type")
    private Integer targetAverageUtilization;

    @Schema(title = "targetAverageValue", description = "averageValue is the target value of the average of the metric across all relevant pods (as a quantity)")
    private String targetAverageValue;

    @Schema(title = "targetValue", description = "value is the target value of the metric (as a quantity).")
    private String targetValue;

    @Schema(title = "currentAverageUtilization", description = "currentAverageUtilization is the current value of the average of the resource metric across all relevant pods, represented as a percentage of the requested value of the resource for the pods.")
    private Integer currentAverageUtilization;

    @Schema(title = "currentAverageValue", description = "averageValue is the current value of the average of the metric across all relevant pods (as a quantity)")
    private String currentAverageValue;

    @Schema(title = "currentValue", description = "value is the current value of the metric (as a quantity).")
    private String currentValue;
}
