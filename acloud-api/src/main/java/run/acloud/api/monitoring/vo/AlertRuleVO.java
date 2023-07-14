package run.acloud.api.monitoring.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@Schema(description = "Alert Rule 모델")
public class AlertRuleVO implements Serializable {
    private static final long serialVersionUID = 7037935285679706255L;

    @Schema(title = "Alert 규칙 번호")
    private Integer alertRuleSeq;

    @Schema(title = "Alert 규칙 아이디")
    private String alertRuleId;

    @Schema(title = "Alert 그룹")
    private String alertGroup;

    @Schema(title = "Alert 이름")
    private String alertName;

    @Schema(title = "Alert 상태")
    private String alertState;

    @Schema(title = "Alert 메시지")
    private String alertMessage;

    @Schema(title = "지속 시간")
    private String duration;

    @Schema(title = "설명")
    private String description;

    @Schema(title = "표현식")
    private String expression;

    @Schema(title = "사용여부")
    private String useYn;

    @Schema(title = "생성일")
    private String created;

    @Schema(title = "생성자")
    private Integer creator;

    @Schema(title = "수정일")
    private String updated;

    @Schema(title = "수정자")
    private Integer updater;

    @Schema(title = "Alert 클러스터 목록")
    private List<AlertRuleClusterMappingVO> alertClusters;

    @Schema(title = "Alert 수신인 목록")
    private List<AlertUserVO> alertReceivers;

    @JsonIgnore
    private Integer accountSeq;

    @JsonIgnore
    private Integer clusterSeq;

    @JsonIgnore
    private Integer clusterId;
}