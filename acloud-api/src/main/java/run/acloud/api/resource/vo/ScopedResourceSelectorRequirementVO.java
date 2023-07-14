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
@Schema(description = "A scoped-resource selector requirement is a selector that contains values, a scope name, and an operator that relates the scope name and values.")
public class ScopedResourceSelectorRequirementVO extends BaseVO{

    public static final String SERIALIZED_NAME_OPERATOR = "operator";
    @SerializedName(SERIALIZED_NAME_OPERATOR)
    @Schema(
            requiredMode = Schema.RequiredMode.REQUIRED,
            name = SERIALIZED_NAME_OPERATOR,
            allowableValues = {KubeConstants.NODE_AFFINITY_OPERATOR_IN, KubeConstants.NODE_AFFINITY_OPERATOR_NOT_IN, KubeConstants.NODE_AFFINITY_OPERATOR_EXISTS, KubeConstants.NODE_AFFINITY_OPERATOR_DOES_NOT_EXIST},
            description =  "Represents a scope's relationship to a set of values. Valid operators are In, NotIn, Exists, DoesNotExist."
    )
    private String operator;

    public static final String SERIALIZED_NAME_SCOPE_NAME = "scopeName";
    @SerializedName(SERIALIZED_NAME_SCOPE_NAME)
    @Schema(
            requiredMode = Schema.RequiredMode.REQUIRED,
            name = SERIALIZED_NAME_SCOPE_NAME,
            description =  "The name of the scope that the selector applies to."
    )
    private String scopeName;

    public static final String SERIALIZED_NAME_VALUES = "values";
    @SerializedName(SERIALIZED_NAME_VALUES)
    @Schema(
            name = SERIALIZED_NAME_VALUES,
            description =  "An array of string values. If the operator is In or NotIn, the values array must be non-empty. If the operator is Exists or DoesNotExist, the values array must be empty. This array is replaced during a strategic merge patch."
    )
    private List<String> values = null;

}
