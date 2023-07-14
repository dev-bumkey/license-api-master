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
@Schema(description = "AggregationRule describes how to locate ClusterRoles to aggregate into the ClusterRole")
public class AggregationRuleVO extends BaseVO{

    public static final String SERIALIZED_NAME_CLUSTER_ROLE_SELECTORS = "clusterRoleSelectors";
    @SerializedName(SERIALIZED_NAME_CLUSTER_ROLE_SELECTORS)
    @Schema(
            name = SERIALIZED_NAME_CLUSTER_ROLE_SELECTORS,
            description =  "ClusterRoleSelectors holds a list of selectors which will be used to find ClusterRoles and create the rules. If any of the selectors match, then the ClusterRole's permissions will be added"
    )
    private List<K8sLabelSelectorVO> clusterRoleSelectors = null;
}
