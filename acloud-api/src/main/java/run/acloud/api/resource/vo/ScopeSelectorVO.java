package run.acloud.api.resource.vo;

import com.google.gson.annotations.SerializedName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import run.acloud.commons.vo.BaseVO;

import java.util.List;

/**
 * @author: wschoi@acornsoft.io
 * Created on 2017. 10. 12.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@Schema(description = "A scope selector represents the AND of the selectors represented by the scoped-resource selector requirements.")
public class ScopeSelectorVO extends BaseVO{

    public static final String SERIALIZED_NAME_MATCH_EXPRESSIONS = "matchExpressions";
    @SerializedName(SERIALIZED_NAME_MATCH_EXPRESSIONS)
    @Schema(
            name = SERIALIZED_NAME_MATCH_EXPRESSIONS,
            description =  "A list of scope selector requirements by scope of the resources."
    )
    private List<ScopedResourceSelectorRequirementVO> matchExpressions = null;

}
