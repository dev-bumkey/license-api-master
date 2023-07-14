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
public class NetworkPolicyIngressRuleVO extends BaseVO{

    public static final String SERIALIZED_NAME_FROM = "from";
    @SerializedName(SERIALIZED_NAME_FROM)
    @Schema(
            name = SERIALIZED_NAME_FROM,
            description =  "List of sources which should be able to access the pods selected for this rule. Items in this list are combined using a logical OR operation. If this field is empty or missing, this rule matches all sources (traffic not restricted by source). If this field is present and contains at least one item, this rule allows traffic only if the traffic matches at least one item in the from list."
    )
    private List<NetworkPolicyPeerVO> from = null;

    public NetworkPolicyIngressRuleVO addFromItem(NetworkPolicyPeerVO fromItem) {
        if (this.from == null) {
            this.from = Lists.newArrayList();
        }
        this.from.add(fromItem);
        return this;
    }

    public static final String SERIALIZED_NAME_PORTS = "ports";
    @SerializedName(SERIALIZED_NAME_PORTS)
    @Schema(
            name = SERIALIZED_NAME_PORTS,
            description =  "List of ports which should be made accessible on the pods selected for this rule. Each item in this list is combined using a logical OR. If this field is empty or missing, this rule matches all ports (traffic not restricted by port). If this field is present and contains at least one item, then this rule allows traffic only if the traffic matches at least one port in the list."
    )
    private List<NetworkPolicyPortVO> ports = null;

    public NetworkPolicyIngressRuleVO addPortsItem(NetworkPolicyPortVO portsItem) {
        if (this.ports == null) {
            this.ports = Lists.newArrayList();
        }
        this.ports.add(portsItem);
        return this;
    }
}
