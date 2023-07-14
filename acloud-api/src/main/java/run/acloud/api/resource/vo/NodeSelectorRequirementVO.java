package run.acloud.api.resource.vo;

import com.google.gson.annotations.SerializedName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import run.acloud.api.resource.constants.KubeConstants;
import run.acloud.commons.vo.BaseVO;

import java.util.List;

/**
 * @author: wschoi@acornsoft.io
 * Created on 2017. 10. 12.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@Schema(description = "A node selector requirement is a selector that contains values, a key, and an operator that relates the key and values.")
public class NodeSelectorRequirementVO extends BaseVO{

    public static final String SERIALIZED_NAME_KEY = "key";
    @SerializedName(SERIALIZED_NAME_KEY)
    @Schema(
            requiredMode = Schema.RequiredMode.REQUIRED,
            name = SERIALIZED_NAME_KEY,
            description =  "The label key that the selector applies to."
    )
    private String key;

    public static final String SERIALIZED_NAME_OPERATOR = "operator";
    @SerializedName(SERIALIZED_NAME_OPERATOR)
    @Schema(
            requiredMode = Schema.RequiredMode.REQUIRED,
            name = SERIALIZED_NAME_OPERATOR,
            allowableValues = {KubeConstants.NODE_AFFINITY_OPERATOR_IN, KubeConstants.NODE_AFFINITY_OPERATOR_NOT_IN, KubeConstants.NODE_AFFINITY_OPERATOR_EXISTS, KubeConstants.NODE_AFFINITY_OPERATOR_DOES_NOT_EXIST, KubeConstants.NODE_AFFINITY_OPERATOR_GT, KubeConstants.NODE_AFFINITY_OPERATOR_LT},
            description =  "Represents a key's relationship to a set of values. Valid operators are In, NotIn, Exists, DoesNotExist. Gt, and Lt."
    )
    private String operator;

    public static final String SERIALIZED_NAME_VALUES = "values";
    @SerializedName(SERIALIZED_NAME_VALUES)
    @Schema(
            name = SERIALIZED_NAME_VALUES,
            description =  "An array of string values. If the operator is In or NotIn, the values array must be non-empty. If the operator is Exists or DoesNotExist, the values array must be empty. If the operator is Gt or Lt, the values array must have a single element, which will be interpreted as an integer. This array is replaced during a strategic merge patch."
    )
    private List<String> values = null;
}
