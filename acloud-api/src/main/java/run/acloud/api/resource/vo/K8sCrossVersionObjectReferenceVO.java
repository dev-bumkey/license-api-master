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
@Schema(description = "CrossVersionObjectReference contains enough information to let you identify the referred resource.")
public class K8sCrossVersionObjectReferenceVO extends BaseVO {

    public static final String SERIALIZED_NAME_API_VERSION = "apiVersion";
    @Schema(title = "API version of the referent")
    @SerializedName(SERIALIZED_NAME_API_VERSION)
    private String apiVersion;

    public static final String SERIALIZED_NAME_KIND = "kind";
    @Schema(title = "Kind of the referent; More info: https://git.k8s.io/community/contributors/devel/sig-architecture/api-conventions.md#types-kinds")
    @SerializedName(SERIALIZED_NAME_KIND)
    private String kind;

    public static final String SERIALIZED_NAME_NAME = "name";
    @Schema(title = "Name of the referent; More info: http://kubernetes.io/docs/user-guide/identifiers#names")
    @SerializedName(SERIALIZED_NAME_NAME)
    private String name;
}
