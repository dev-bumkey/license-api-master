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
@Schema(description = "NetworkPolicyPort describes a port to allow traffic on")
public class NetworkPolicyPortVO extends BaseVO{

    public static final String SERIALIZED_NAME_PORT = "port";
    @SerializedName(SERIALIZED_NAME_PORT)
    @Schema(
            name = SERIALIZED_NAME_PORT,
            description =  "IntOrString is a type that can hold an int32 or a string.  When used in JSON or YAML marshalling and unmarshalling, it produces or consumes the inner type.  This allows you to have, for example, a JSON field that can accept a name or number."
    )
    private String port;

    public static final String SERIALIZED_NAME_PROTOCOL = "protocol";
    @SerializedName(SERIALIZED_NAME_PROTOCOL)
    @Schema(
            name = SERIALIZED_NAME_PROTOCOL,
            allowableValues = {"TCP","UDP","SCTP"},
            description =  "The protocol (TCP, UDP, or SCTP) which traffic must match. If not specified, this field defaults to TCP."
    )
    private String protocol;

}
