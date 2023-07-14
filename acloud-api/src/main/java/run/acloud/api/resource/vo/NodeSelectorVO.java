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
@Schema(description = "A node selector represents the union of the results of one or more label queries over a set of nodes; that is, it represents the OR of the selectors represented by the node selector terms.")
public class NodeSelectorVO extends BaseVO{

    public static final String SERIALIZED_NAME_NODE_SELECTOR_TERMS = "nodeSelectorTerms";
    @SerializedName(SERIALIZED_NAME_NODE_SELECTOR_TERMS)
    @Schema(
            requiredMode = Schema.RequiredMode.REQUIRED,
            name = SERIALIZED_NAME_NODE_SELECTOR_TERMS,
            description =  "Required. A list of node selector terms. The terms are ORed."
    )
    private List<NodeSelectorTermVO> nodeSelectorTerms = new ArrayList<NodeSelectorTermVO>();

}
