package run.acloud.api.resource.vo;

import com.google.gson.annotations.SerializedName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import run.acloud.commons.vo.BaseVO;

/**
 * @author: wschoi@acornsoft.io
 * Created on 2017. 10. 12.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@Schema(description = "An empty preferred scheduling term matches all objects with implicit weight 0 (i.e. it's a no-op). A null preferred scheduling term matches no objects (i.e. is also a no-op).")
public class PreferredSchedulingTermVO extends BaseVO{

    public static final String SERIALIZED_NAME_PREFERENCE = "preference";
    @SerializedName(SERIALIZED_NAME_PREFERENCE)
    @Schema(
            requiredMode = Schema.RequiredMode.REQUIRED,
            name = SERIALIZED_NAME_PREFERENCE,
            description =  "Required. A null or empty node selector term matches no objects. The requirements of them are ANDed. The TopologySelectorTerm type implements a subset of the NodeSelectorTerm."
    )
    private NodeSelectorTermVO preference;

    public static final String SERIALIZED_NAME_WEIGHT = "weight";
    @SerializedName(SERIALIZED_NAME_WEIGHT)
    @Schema(
            requiredMode = Schema.RequiredMode.REQUIRED,
            name = SERIALIZED_NAME_WEIGHT,
            description =  "Weight associated with matching the corresponding nodeSelectorTerm, in the range 1-100."
    )
    private Integer weight;

}
