package run.acloud.api.resource.vo;

import com.google.gson.annotations.SerializedName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import run.acloud.api.resource.constants.KubeConstants;
import run.acloud.commons.vo.BaseVO;

/**
 * @author: wschoi@acornsoft.io
 * Created on 2017. 10. 12.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class TolerationVO extends BaseVO{

    @SerializedName("effect")
    @Schema(
            allowableValues = {KubeConstants.TOLERATION_EFFECT_NO_SCHEDULE, KubeConstants.TOLERATION_EFFECT_PREFER_NO_SCHEDULE, KubeConstants.TOLERATION_EFFECT_NO_EXECUTE},
            description =  "Effect indicates the taint effect to match. Empty means match all taint effects. When specified, allowed values are NoSchedule, PreferNoSchedule and NoExecute."
    )
    private String effect = null;

    @SerializedName("key")
    @Schema(title = "Key is the taint key that the toleration applies to. Empty means match all taint keys. If the key is empty, operator must be Exists; this combination means to match all values and all keys.")
    private String key = null;

    @SerializedName("operator")
    @Schema(
            allowableValues = {KubeConstants.TOLERATION_OPERATOR_EXISTS, KubeConstants.TOLERATION_OPERATOR_EQUAL},
            description =  "Operator represents a key's relationship to the value. Valid operators are Exists and Equal. Defaults to Equal. Exists is equivalent to wildcard for value, so that a pod can tolerate all taints of a particular category."
    )
    private String operator = null;

    @SerializedName("tolerationSeconds")
    @Schema(title = "TolerationSeconds represents the period of time the toleration (which must be of effect NoExecute, otherwise this field is ignored) tolerates the taint. By default, it is not set, which means tolerate the taint forever (do not evict). Zero and negative values will be treated as 0 (evict immediately) by the system.")
    private Long tolerationSeconds = null;

    @SerializedName("value")
    @Schema(title = "Value is the taint value the toleration matches to. If the operator is Exists, the value should be empty, otherwise just a regular string.")
    private String value = null;
}
