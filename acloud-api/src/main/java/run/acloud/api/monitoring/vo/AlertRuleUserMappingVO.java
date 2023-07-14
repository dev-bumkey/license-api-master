package run.acloud.api.monitoring.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@Schema(description = "Alert Rule 사용자 모델")
public class AlertRuleUserMappingVO implements Serializable {
    private static final long serialVersionUID = -4157274634504343443L;

    @Schema(title = "사용자 번호")
    private Integer userSeq;

    @Schema(title = "Alert 규칙 번호")
    private Integer alertRuleSeq;
}