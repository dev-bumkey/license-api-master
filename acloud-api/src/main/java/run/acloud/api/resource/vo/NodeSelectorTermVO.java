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
@Schema(description = "A null or empty node selector term matches no objects. The requirements of them are ANDed. The TopologySelectorTerm type implements a subset of the NodeSelectorTerm.")
public class NodeSelectorTermVO extends BaseVO{

    public static final String SERIALIZED_NAME_MATCH_EXPRESSIONS = "matchExpressions";
    @SerializedName(SERIALIZED_NAME_MATCH_EXPRESSIONS)
    @Schema(
            name = SERIALIZED_NAME_MATCH_EXPRESSIONS,
            description =  "A list of node selector requirements by node's labels."
    )
    private List<NodeSelectorRequirementVO> matchExpressions = null;

    public static final String SERIALIZED_NAME_MATCH_FIELDS = "matchFields";
    @SerializedName(SERIALIZED_NAME_MATCH_FIELDS)
    @Schema(
            name = SERIALIZED_NAME_MATCH_FIELDS,
            description =  "A list of node selector requirements by node's fields."
    )
    private List<NodeSelectorRequirementVO> matchFields = null;
}
