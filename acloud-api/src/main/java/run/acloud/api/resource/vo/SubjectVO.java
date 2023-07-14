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
@Schema(description = "Subject contains a reference to the object or user identities a role binding applies to.  This can either hold a direct API object reference, or a value for non-objects such as user and group names.")
public class SubjectVO extends BaseVO{

    public static final String SERIALIZED_NAME_API_GROUP = "apiGroup";
    @SerializedName(SERIALIZED_NAME_API_GROUP)
    @Schema(
            name = SERIALIZED_NAME_API_GROUP,
            description =  "APIGroup holds the API group of the referenced subject. Defaults to ' for ServiceAccount subjects. Defaults to 'rbac.authorization.k8s.io' for User and Group subjects."
    )
    private String apiGroup;

    public static final String SERIALIZED_NAME_KIND = "kind";
    @SerializedName(SERIALIZED_NAME_KIND)
    @Schema(
            name = SERIALIZED_NAME_KIND,
            description =  "Kind of object being referenced. Values defined by this API group are 'User', Group', and 'ServiceAccount'. If the Authorizer does not recognized the kind value, the Authorizer should report an error."
    )
    private String kind;

    public static final String SERIALIZED_NAME_NAME = "name";
    @SerializedName(SERIALIZED_NAME_NAME)
    @Schema(
            name = SERIALIZED_NAME_NAME,
            description =  "Name of the object being referenced."
    )
    private String name;

    public static final String SERIALIZED_NAME_NAMESPACE = "namespace";
    @SerializedName(SERIALIZED_NAME_NAMESPACE)
    @Schema(
            name = SERIALIZED_NAME_NAMESPACE,
            description =  "Namespace of the referenced object.  If the object kind is non-namespace, such as 'User' or 'Group', and this value is not empty the Authorizer should report an error."
    )
    private String namespace;

}
