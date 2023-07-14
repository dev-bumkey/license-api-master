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
@Schema(description = "IPBlock describes a particular CIDR (Ex. 192.168.1.1/24) that is allowed to the pods matched by a NetworkPolicySpec's podSelector. The except entry describes CIDRs that should not be included within this rule.")
public class IPBlockVO extends BaseVO{

    public static final String SERIALIZED_NAME_CIDR = "cidr";
    @SerializedName(SERIALIZED_NAME_CIDR)
    @Schema(
            requiredMode = Schema.RequiredMode.REQUIRED,
            name = SERIALIZED_NAME_CIDR,
            description =  "CIDR is a string representing the IP Block Valid examples are 192.168.1.1/24"
    )
    private String cidr;

    public static final String SERIALIZED_NAME_EXCEPT = "except";
    @SerializedName(SERIALIZED_NAME_EXCEPT)
    @Schema(
            name = SERIALIZED_NAME_EXCEPT,
            description =  "Except is a slice of CIDRs that should not be included within an IP Block Valid examples are 192.168.1.1/24 Except values will be rejected if they are outside the CIDR range"
    )
    private List<String> except = null;

}
