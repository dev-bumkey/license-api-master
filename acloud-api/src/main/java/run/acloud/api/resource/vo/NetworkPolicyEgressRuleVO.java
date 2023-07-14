package run.acloud.api.resource.vo;

import com.google.common.collect.Lists;
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
@Schema(description = "NetworkPolicyPeer describes a peer to allow traffic from. Only certain combinations of fields are allowed")
public class NetworkPolicyEgressRuleVO extends BaseVO{

    public static final String SERIALIZED_NAME_PORTS = "ports";
    @SerializedName(SERIALIZED_NAME_PORTS)
    @Schema(
            name = SERIALIZED_NAME_PORTS,
            description =  "List of destination ports for outgoing traffic. Each item in this list is combined using a logical OR. If this field is empty or missing, this rule matches all ports (traffic not restricted by port). If this field is present and contains at least one item, then this rule allows traffic only if the traffic matches at least one port in the list."
    )
    private List<NetworkPolicyPortVO> ports = null;

    public NetworkPolicyEgressRuleVO addPortsItem(NetworkPolicyPortVO portsItem) {
        if (this.ports == null) {
            this.ports = Lists.newArrayList();
        }
        this.ports.add(portsItem);
        return this;
    }

    public static final String SERIALIZED_NAME_TO = "to";
    @SerializedName(SERIALIZED_NAME_TO)
    @Schema(
            name = SERIALIZED_NAME_TO,
            description =  "List of destinations for outgoing traffic of pods selected for this rule. Items in this list are combined using a logical OR operation. If this field is empty or missing, this rule matches all destinations (traffic not restricted by destination). If this field is present and contains at least one item, this rule allows traffic only if the traffic matches at least one item in the to list."
    )
    private List<NetworkPolicyPeerVO> to = null;

    public NetworkPolicyEgressRuleVO addToItem(NetworkPolicyPeerVO toItem) {
        if (this.to == null) {
            this.to = Lists.newArrayList();
        }
        this.to.add(toItem);
        return this;
    }
}
