package run.acloud.api.resource.vo;

import com.google.gson.annotations.SerializedName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import run.acloud.api.resource.enums.HpaScalingPolicyType;

import java.io.Serializable;

@Schema(description = "HPA behavior scaling policy 모델")
@Getter
@Setter
public class HpaScalingPolicyVO implements Serializable {

    private static final long serialVersionUID = 2317813778912712522L;

    @Schema(title = "periodSeconds")
    @SerializedName("periodSeconds")
    private Integer periodSeconds;

    @Schema(title = "type", allowableValues = {HpaScalingPolicyType.Names.Percent, HpaScalingPolicyType.Names.Pods})
    @SerializedName("type")
    private String type;

    @Schema(title = "value", description = "0 초과 (It must be greater than zero)")
    @SerializedName("value")
    private Integer value;
}
