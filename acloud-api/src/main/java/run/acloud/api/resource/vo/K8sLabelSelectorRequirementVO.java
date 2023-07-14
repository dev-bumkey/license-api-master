package run.acloud.api.resource.vo;

import com.google.gson.annotations.SerializedName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import run.acloud.commons.vo.BaseVO;

import java.util.ArrayList;
import java.util.List;

/**
 * @author: wschoi@acornsoft.io
 * Created on 2017. 10. 12.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class K8sLabelSelectorRequirementVO extends BaseVO {

    @SerializedName("key")
    @Schema(title = "key", description = "key is the label key that the selector applies to.")
    private String key = null;

    public K8sLabelSelectorRequirementVO key(String key) {

        this.key = key;
        return this;
    }

    @SerializedName("operator")
    @Schema(title = "operator", allowableValues = {"In","NotIn","Exists","DoesNotExist"}, description = "operator represents a key's relationship to a set of values. Valid operators are In, NotIn, Exists and DoesNotExist.")
    private String operator = null;

    public K8sLabelSelectorRequirementVO operator(String operator) {

        this.operator = operator;
        return this;
    }

    @SerializedName("values")
    @Schema(title = "values", description = " values is an array of string values. If the operator is In or NotIn, the values array must be non-empty. If the operator is Exists or DoesNotExist, the values array must be empty. This array is replaced during a strategic merge patch.")
    private List<String> values = null;

    public K8sLabelSelectorRequirementVO values(List<String> values) {

        this.values = values;
        return this;
    }

    public K8sLabelSelectorRequirementVO addValuesItem(String valuesItem) {
        if (this.values == null) {
            this.values = new ArrayList<>();
        }
        this.values.add(valuesItem);
        return this;
    }
}
