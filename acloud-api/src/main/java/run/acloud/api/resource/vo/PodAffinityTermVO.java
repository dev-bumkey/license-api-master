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
@Schema(description = "Defines a set of pods (namely those matching the labelSelector relative to the given namespace(s)) that this pod should be co-located (affinity) or not co-located (anti-affinity) with, where co-located is defined as running on a node whose value of the label with key <topologyKey> matches that of any node on which a pod of the set of pods is running")
public class PodAffinityTermVO extends BaseVO{

    public static final String SERIALIZED_NAME_LABEL_SELECTOR = "labelSelector";
    @SerializedName(SERIALIZED_NAME_LABEL_SELECTOR)
    @Schema(
            name = SERIALIZED_NAME_LABEL_SELECTOR,
            description =  "A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects."
    )
    private K8sLabelSelectorVO labelSelector;

    public static final String SERIALIZED_NAME_NAMESPACES = "namespaces";
    @SerializedName(SERIALIZED_NAME_NAMESPACES)
    @Schema(
            name = SERIALIZED_NAME_NAMESPACES,
            description =  "namespaces specifies which namespaces the labelSelector applies to (matches against); null or empty list means 'this pod's namespace'"
    )
    private List<String> namespaces = null;

    public static final String SERIALIZED_NAME_TOPOLOGY_KEY = "topologyKey";
    @SerializedName(SERIALIZED_NAME_TOPOLOGY_KEY)
    @Schema(
            requiredMode = Schema.RequiredMode.REQUIRED,
            name = SERIALIZED_NAME_TOPOLOGY_KEY,
            description =  "This pod should be co-located (affinity) or not co-located (anti-affinity) with the pods matching the labelSelector in the specified namespaces, where co-located is defined as running on a node whose value of the label with key topologyKey matches that of any node on which any of the selected pods is running. Empty topologyKey is not allowed."
    )
    private String topologyKey;

}
