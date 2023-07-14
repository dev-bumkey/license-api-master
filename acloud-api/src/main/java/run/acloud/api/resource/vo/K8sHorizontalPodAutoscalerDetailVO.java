package run.acloud.api.resource.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import run.acloud.commons.vo.BaseVO;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * @author: wschoi@acornsoft.io
 * Created on 2017. 10. 12.
 */
@Getter
@Setter
@Schema(description = "Horizontal Pod Autoscaler 상세 모델")
public class K8sHorizontalPodAutoscalerDetailVO extends BaseVO{

    @Schema(title = "Horizontal Pod Autoscaler 명")
    private String name;

    @Schema(title = "네임스페이스")
    private String namespace;

    @Schema(title = "라벨")
    private Map<String, String> labels;

    @Schema(title = "어노테이션")
    private Map<String, String> annotations;

    @Schema(title = "생성 시간")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private OffsetDateTime creationTime;

    @Schema(title = "scaled target", description = "reference to scaled resource; horizontal pod autoscaler will learn the current resource consumption and will set the desired number of pods by using its Scale subresource.")
    private String target;

    @Schema(title = "min Replicas", description = "lower limit for the number of pods that can be set by the autoscaler, default 1.")
    private Integer minReplicas;

    @Schema(title = "max Replicas", description = "upper limit for the number of pods that can be set by the autoscaler; cannot be smaller than MinReplicas.")
    private Integer maxReplicas;

    @Schema(title = "target CPU Utilization Percentage", description = "target average CPU utilization (represented as a percentage of requested CPU) over all the pods; if not specified the default autoscaling policy will be used.")
    @Deprecated
    private Integer targetCPUUtilization;

    @Schema(title = "current Replicas", description = "current number of replicas of pods managed by this autoscaler.")
    private Integer currentReplicas;

    @Schema(title = "desired Replicas", description = "desired number of replicas of pods managed by this autoscaler.")
    private Integer desiredReplicas;

    @Schema(title = "current CPU Utilization Percentage", description = "current average CPU utilization over all pods, represented as a percentage of requested CPU, e.g. 70 means that an average pod is using now 70% of its requested CPU.")
    @Deprecated
    private Integer currentCPUUtilization;

    @Schema(title = "마지막 scale 시간", description = "last time the HorizontalPodAutoscaler scaled the number of pods; used by the autoscaler to control how often the number of pods is changed.")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private OffsetDateTime lastScaleTime;

    @Schema(title = "metrics", description = "metrics contains the specifications for which to use to calculate the desired replica count (the maximum replica count across all metrics will be used).  The desired replica count is calculated multiplying the ratio between the target value and the current value by the current number of pods.  Ergo, metrics used must decrease as the pod count is increased, and vice-versa.  See the individual metric source types for more information about how each type of metric must respond.")
    private List<K8sHorizontalPodAutoscalerMetricVO> metrics;

    @Schema(title = "scaleDown", description = "HorizontalPodAutoscalerBehavior configures the scaling behavior of the target in both Up and Down directions (scaleUp and scaleDown fields respectively).")
    private K8sHorizontalPodAutoscalerScalingRulesVO scaleDown;

    @Schema(title = "scaleUp", description = "HorizontalPodAutoscalerBehavior configures the scaling behavior of the target in both Up and Down directions (scaleUp and scaleDown fields respectively).")
    private K8sHorizontalPodAutoscalerScalingRulesVO scaleUp;
}
