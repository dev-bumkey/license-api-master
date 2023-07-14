package run.acloud.api.resource.vo;

import com.google.gson.annotations.SerializedName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import run.acloud.commons.vo.BaseVO;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author: wschoi@acornsoft.io
 * Created on 2017. 10. 12.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@Schema(description = "A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.")
public class K8sLabelSelectorVO extends BaseVO {

    @SerializedName("matchExpressions")
    @Schema(title = "matchExpressions")
    private List<K8sLabelSelectorRequirementVO> matchExpressions = null;

    public K8sLabelSelectorVO matchExpressions(List<K8sLabelSelectorRequirementVO> matchExpressions) {

        this.matchExpressions = matchExpressions;
        return this;
    }

    public K8sLabelSelectorVO addMatchExpressionsItem(K8sLabelSelectorRequirementVO matchExpressionsItem) {
        if (this.matchExpressions == null) {
            this.matchExpressions = new ArrayList<>();
        }
        this.matchExpressions.add(matchExpressionsItem);
        return this;
    }

    @SerializedName("matchLabels")
    @Schema(title = "matchLabels")
    private Map<String, String> matchLabels = null;

    public K8sLabelSelectorVO matchLabels(Map<String, String> matchLabels) {

        this.matchLabels = matchLabels;
        return this;
    }

    public K8sLabelSelectorVO putMatchLabelsItem(String key, String matchLabelsItem) {
        if (this.matchLabels == null) {
            this.matchLabels = new HashMap<>();
        }
        this.matchLabels.put(key, matchLabelsItem);
        return this;
    }

}
