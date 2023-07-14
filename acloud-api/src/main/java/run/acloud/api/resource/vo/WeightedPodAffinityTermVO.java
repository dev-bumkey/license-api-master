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
@Schema(description = "The weights of all of the matched WeightedPodAffinityTerm fields are added per-node to find the most preferred node(s)")
public class WeightedPodAffinityTermVO extends BaseVO{

    public static final String SERIALIZED_NAME_POD_AFFINITY_TERM = "podAffinityTerm";
    @SerializedName(SERIALIZED_NAME_POD_AFFINITY_TERM)
    @Schema(
            requiredMode = Schema.RequiredMode.REQUIRED,
            name = SERIALIZED_NAME_POD_AFFINITY_TERM,
            description =  "Defines a set of pods (namely those matching the labelSelector relative to the given namespace(s)) that this pod should be co-located (affinity) or not co-located (anti-affinity) with, where co-located is defined as running on a node whose value of the label with key <topologyKey> matches that of any node on which a pod of the set of pods is running"
    )
    private PodAffinityTermVO podAffinityTerm;

    public static final String SERIALIZED_NAME_WEIGHT = "weight";
    @SerializedName(SERIALIZED_NAME_WEIGHT)
    @Schema(
            requiredMode = Schema.RequiredMode.REQUIRED,
            name = SERIALIZED_NAME_WEIGHT,
            description =  "weight associated with matching the corresponding podAffinityTerm, in the range 1-100."
    )
    private Integer weight;

}
