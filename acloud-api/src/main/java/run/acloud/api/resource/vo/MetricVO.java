package run.acloud.api.resource.vo;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import run.acloud.api.resource.enums.MetricTargetType;
import run.acloud.api.resource.enums.MetricType;
import run.acloud.commons.vo.BaseVO;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class MetricVO extends BaseVO {

    MetricType type;
    MetricTargetType targetType = MetricTargetType.Utilization;
    String targetValue;
    String targetAverageValue;
    Integer targetAverageUtilization;
    K8sLabelSelectorVO metricLabelSelector;

    // Object
    String objectMetricName;
    String objectTargetApiVerion;
    String objectTargetKind;
    String objectTargetName;

    // Pods
    String podsMetricName;

    // Resource
    String resourceName;

    // External
    String externalMetricName;

    // ContainerResource
    String containerResourceName;
    String containerName;

    Integer resourceTargetAverageUtilization;
}
