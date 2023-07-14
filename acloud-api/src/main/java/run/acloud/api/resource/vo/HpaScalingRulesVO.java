package run.acloud.api.resource.vo;

import com.google.gson.annotations.SerializedName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import run.acloud.api.resource.enums.HpaSelectPolicy;

import java.io.Serializable;
import java.util.List;

@Schema(description = "HPA behavior scaling rule 모델")
@Getter
@Setter
public class HpaScalingRulesVO implements Serializable {

    private static final long serialVersionUID = -8906514488382665789L;

    @Schema(
              title =  "policies"
            , description = "policies is a list of potential scaling polices which can be used during scaling. At least one policy must be specified, otherwise the HPAScalingRules will be discarded as invalid"
    )
    @SerializedName("policies")
    private List<HpaScalingPolicyVO> policies;

    @Schema(
              title =  "selectPolicy"
            , allowableValues = {HpaSelectPolicy.Names.Max, HpaSelectPolicy.Names.Min, HpaSelectPolicy.Names.Disabled}
    )
    @SerializedName("selectPolicy")
    private String selectPolicy;

    @Schema(
              title =  "stabilizationWindowSeconds"
            , description = "StabilizationWindowSeconds is the number of seconds for which past recommendations should be considered while scaling up or scaling down. StabilizationWindowSeconds must be greater than or equal to zero and less than or equal to 3600 (one hour). If not set, use the default values: - For scale up: 0 (i.e. no stabilization is done). - For scale down: 300 (i.e. the stabilization window is 300 seconds long)."
    )
    @SerializedName("stabilizationWindowSeconds")
    private Integer stabilizationWindowSeconds;
}
