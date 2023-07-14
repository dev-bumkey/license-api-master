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
@Schema(description = "Affinity is a group of affinity scheduling rules.")
public class AffinityVO extends BaseVO{

    public static final String SERIALIZED_NAME_NODE_AFFINITY = "nodeAffinity";
    @SerializedName(SERIALIZED_NAME_NODE_AFFINITY)
    @Schema(
            name = SERIALIZED_NAME_NODE_AFFINITY,
            description =  "Node affinity is a group of node affinity scheduling rules."
    )
    private NodeAffinityVO nodeAffinity;

    public static final String SERIALIZED_NAME_POD_AFFINITY = "podAffinity";
    @SerializedName(SERIALIZED_NAME_POD_AFFINITY)
    @Schema(
            name = SERIALIZED_NAME_POD_AFFINITY,
            description =  "Node affinity is a group of node affinity scheduling rules."
    )
    private PodAffinityVO podAffinity;

    public static final String SERIALIZED_NAME_POD_ANTI_AFFINITY = "podAntiAffinity";
    @SerializedName(SERIALIZED_NAME_POD_ANTI_AFFINITY)
    @Schema(
            name = SERIALIZED_NAME_POD_ANTI_AFFINITY,
            description =  "Pod anti affinity is a group of inter pod anti affinity scheduling rules."
    )
    private PodAntiAffinityVO podAntiAffinity;

}
