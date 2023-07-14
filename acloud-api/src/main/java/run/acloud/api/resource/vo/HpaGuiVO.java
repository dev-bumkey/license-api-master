package run.acloud.api.resource.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import run.acloud.api.cserver.enums.WorkloadType;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
public class HpaGuiVO implements Serializable {
    private static final long serialVersionUID = -4760154629009102795L;

    /** 2020.10.15 : HPA 생성시 이름을 지정할 수 있도록 함 **/
    @Schema(title = "이름")
    private String name;

    @Schema(title = "네임스페이스")
    private String namespace;

    @Deprecated
    @Schema(title = "리소스 유형(기존 hps v1 spec)")
    private String type;

    @Deprecated
    @Schema(title = "대상 CPU 퍼센트 값(기존 hps v1 spec)")
    private Integer targetCPUUtilizationPercentage;

    @Schema(title = "워크로드 유형", allowableValues = {WorkloadType.Names.REPLICA_SERVER, WorkloadType.Names.STATEFUL_SET_SERVER})
    private WorkloadType workloadType;

    @Schema(title = "워크로드 이름")
    private String workloadName;

    @Schema(title = "최소 replica")
    private Integer minReplicas;

    @Schema(title = "최대 replica")
    private Integer maxReplicas;

    @Schema(title = "대상 메트릭")
    private List<MetricVO> metrics;

    @Schema(title = "scale down 규칙")
    private HpaScalingRulesVO scaleDown;

    @Schema(title = "scale up 규칙")
    private HpaScalingRulesVO scaleUp;

}
