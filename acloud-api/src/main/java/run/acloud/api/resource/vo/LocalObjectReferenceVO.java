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
@Schema(description = "LocalObjectReference contains enough information to let you locate the referenced object inside the same namespace.")
public class LocalObjectReferenceVO extends BaseVO {

    public static final String SERIALIZED_NAME_NAME = "name";

    @SerializedName(SERIALIZED_NAME_NAME)
    @Schema(
            name = SERIALIZED_NAME_NAME,
            description =  "Name of the referent. More info: https://kubernetes.io/docs/concepts/overview/working-with-objects/names/#names"
    )
    private String name;

    public LocalObjectReferenceVO name(String name) {

        this.name = name;
        return this;
    }
}
