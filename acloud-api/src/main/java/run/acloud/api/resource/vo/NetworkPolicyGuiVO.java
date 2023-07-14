package run.acloud.api.resource.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.annotations.SerializedName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * @author wschoi@acornsoft.io
 * Created on 2017. 7. 26.
 */
@Getter
@Setter
@EqualsAndHashCode
@Schema(name = "NetworkPolicyGuiVO", title = "NetworkPolicyGuiVO", allOf = {NetworkPolicyIntegrateVO.class}, description = "GUI 배포 모델")
public class NetworkPolicyGuiVO extends NetworkPolicyIntegrateVO implements Serializable {

    private static final long serialVersionUID = 5947075239496626047L;

    @Schema(description = "서비스맵 일련 번호", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer servicemapSeq;

    @Schema(description = "NetworkPolicy 이름", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(description = "namespace", requiredMode = Schema.RequiredMode.REQUIRED)
    private String namespace;

    @Schema(title = "NetworkPolicy 설명")
    private String description;

    @Schema(title = "default", allowableValues = {"true","false"})
    private boolean isDefault = false;

    @Schema(title = "생성시간")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private OffsetDateTime creationTimestamp;

    @Schema(description = "label")
    private Map<String, String> labels;

    public NetworkPolicyGuiVO putLabelsItem(String key, String labelsItem) {
        if (this.labels == null) {
            this.labels = Maps.newHashMap();
        }
        this.labels.put(key, labelsItem);
        return this;
    }

    @Schema(description = "annotations")
    private Map<String, String> annotations;

    public NetworkPolicyGuiVO putAnnotationsItem(String key, String annotationsItem) {
        if (this.annotations == null) {
            this.annotations = Maps.newHashMap();
        }
        this.annotations.put(key, annotationsItem);
        return this;
    }

    public static final String SERIALIZED_NAME_EGRESS = "egress";
    @SerializedName(SERIALIZED_NAME_EGRESS)
    @Schema(
            name = SERIALIZED_NAME_EGRESS,
            description =  "List of egress rules to be applied to the selected pods. Outgoing traffic is allowed if there are no NetworkPolicies selecting the pod (and cluster policy otherwise allows the traffic), OR if the traffic matches at least one egress rule across all of the NetworkPolicy objects whose podSelector matches the pod. If this field is empty then this NetworkPolicy limits all outgoing traffic (and serves solely to ensure that the pods it selects are isolated by default). This field is beta-level in 1.8"
    )
    private List<NetworkPolicyEgressRuleVO> egress = null;

    public NetworkPolicyGuiVO addEgressItem(NetworkPolicyEgressRuleVO egressItem) {
        if (this.egress == null) {
            this.egress = Lists.newArrayList();
        }
        this.egress.add(egressItem);
        return this;
    }

    public static final String SERIALIZED_NAME_INGRESS = "ingress";
    @SerializedName(SERIALIZED_NAME_INGRESS)
    @Schema(
            name = SERIALIZED_NAME_INGRESS,
            description =  "List of ingress rules to be applied to the selected pods. Traffic is allowed to a pod if there are no NetworkPolicies selecting the pod (and cluster policy otherwise allows the traffic), OR if the traffic source is the pod's local node, OR if the traffic matches at least one ingress rule across all of the NetworkPolicy objects whose podSelector matches the pod. If this field is empty then this NetworkPolicy does not allow any traffic (and serves solely to ensure that the pods it selects are isolated by default)"
    )
    private List<NetworkPolicyIngressRuleVO> ingress = null;

    public NetworkPolicyGuiVO addIngressItem(NetworkPolicyIngressRuleVO ingressItem) {
        if (this.ingress == null) {
            this.ingress = Lists.newArrayList();
        }
        this.ingress.add(ingressItem);
        return this;
    }

    public static final String SERIALIZED_NAME_POD_SELECTOR = "podSelector";
    @SerializedName(SERIALIZED_NAME_POD_SELECTOR)
    @Schema(
            name = SERIALIZED_NAME_POD_SELECTOR,
            description =  ""
    )
    private K8sLabelSelectorVO podSelector;

    public static final String SERIALIZED_NAME_POLICY_TYPES = "policyTypes";
    @SerializedName(SERIALIZED_NAME_POLICY_TYPES)
    @Schema(
            name = SERIALIZED_NAME_POLICY_TYPES,
            allowableValues = {"Ingress","Egress"},
            description =  "List of rule types that the NetworkPolicy relates to. Valid options are 'Ingress', 'Egress', or 'Ingress,Egress'. If this field is not specified, it will default based on the existence of Ingress or Egress rules; policies that contain an Egress section are assumed to affect Egress, and all policies (whether or not they contain an Ingress section) are assumed to affect Ingress. If you want to write an egress-only policy, you must explicitly specify policyTypes [ 'Egress' ]. Likewise, if you want to write a policy that specifies that no egress is allowed, you must specify a policyTypes value that include 'Egress' (since such a policy would not include an Egress section and would otherwise default to just [ 'Ingress' ]). This field is beta-level in 1.8"
    )
    private List<String> policyTypes = null;

    public NetworkPolicyGuiVO addPolicyTypesItem(String policyTypesItem) {
        if (this.policyTypes == null) {
            this.policyTypes = Lists.newArrayList();
        }
        this.policyTypes.add(policyTypesItem);
        return this;
    }

}
