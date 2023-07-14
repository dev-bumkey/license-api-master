package run.acloud.api.resource.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.google.gson.annotations.SerializedName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * @author wschoi@acornsoft.io
 * Created on 2017. 7. 26.
 */
@Getter
@Setter
@Schema(name = "ClusterRoleGuiVO", title = "ClusterRoleGuiVO", description = "Cluster Role GUI 모델", allOf = {ClusterRoleIntegrateVO.class})
public class ClusterRoleGuiVO extends ClusterRoleIntegrateVO {

    @Schema(description = "Cluster Role 이름", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(title = "생성시간")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private OffsetDateTime creationTimestamp;

    @Schema(title = "ConfigMap 설명")
    private String description;

    @Schema(description = "label")
    private Map<String, String> labels;

    @Schema(description = "annotations")
    private Map<String, String> annotations;

    public static final String SERIALIZED_NAME_AGGREGATION_RULE = "aggregationRule";
    @SerializedName(SERIALIZED_NAME_AGGREGATION_RULE)
    @Schema(
            name = SERIALIZED_NAME_AGGREGATION_RULE,
            description =  "aggregationRule"
    )
    private AggregationRuleVO aggregationRule;

    public static final String SERIALIZED_NAME_RULES = "rules";
    @SerializedName(SERIALIZED_NAME_RULES)
    @Schema(
            name = SERIALIZED_NAME_RULES,
            description =  "Rules holds all the PolicyRules for this ClusterRole"
    )
    private List<PolicyRuleVO> rules = null;

}
