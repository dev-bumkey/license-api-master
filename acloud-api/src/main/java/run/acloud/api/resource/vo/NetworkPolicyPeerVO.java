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
@Schema(description = "NetworkPolicyPeer describes a peer to allow traffic from. Only certain combinations of fields are allowed")
public class NetworkPolicyPeerVO extends BaseVO{

    public static final String SERIALIZED_NAME_IP_BLOCK = "ipBlock";
    @SerializedName(SERIALIZED_NAME_IP_BLOCK)
    @Schema(
            name = SERIALIZED_NAME_IP_BLOCK,
            description =  "IPBlock describes a particular CIDR (Ex. '192.168.1.1/24') that is allowed to the pods matched by a NetworkPolicySpec's podSelector. The except entry describes CIDRs that should not be included within this rule."
    )
    private IPBlockVO ipBlock;

    public NetworkPolicyPeerVO ipBlock(IPBlockVO ipBlock) {

        this.ipBlock = ipBlock;
        return this;
    }

    public static final String SERIALIZED_NAME_NAMESPACE_SELECTOR = "namespaceSelector";
    @SerializedName(SERIALIZED_NAME_NAMESPACE_SELECTOR)
    @Schema(
            name = SERIALIZED_NAME_NAMESPACE_SELECTOR,
            description =  "A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects."
    )
    private K8sLabelSelectorVO namespaceSelector;

    public NetworkPolicyPeerVO namespaceSelector(K8sLabelSelectorVO namespaceSelector) {

        this.namespaceSelector = namespaceSelector;
        return this;
    }

    public static final String SERIALIZED_NAME_POD_SELECTOR = "podSelector";
    @SerializedName(SERIALIZED_NAME_POD_SELECTOR)
    @Schema(
            name = SERIALIZED_NAME_POD_SELECTOR,
            description =  "A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects."
    )
    private K8sLabelSelectorVO podSelector;

    public NetworkPolicyPeerVO podSelector(K8sLabelSelectorVO podSelector) {

        this.podSelector = podSelector;
        return this;
    }

}
