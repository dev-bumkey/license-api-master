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
@Schema(description = "RoleRef contains information that points to the role being used")
public class RoleRefVO extends BaseVO{

    public static final String SERIALIZED_NAME_API_GROUP = "apiGroup";
    @SerializedName(SERIALIZED_NAME_API_GROUP)
    @Schema(
            name = SERIALIZED_NAME_API_GROUP,
            description =  "APIGroup is the group for the resource being referenced"
    )
    private String apiGroup;

    public static final String SERIALIZED_NAME_KIND = "kind";
    @SerializedName(SERIALIZED_NAME_KIND)
    @Schema(
            name = SERIALIZED_NAME_KIND,
            description =  "Kind is the type of resource being referenced"
    )
    private String kind;

    public static final String SERIALIZED_NAME_NAME = "name";
    @SerializedName(SERIALIZED_NAME_NAME)
    @Schema(
            name = SERIALIZED_NAME_NAME,
            description =  "Name is the name of resource being referenced"
    )
    private String name;
}
