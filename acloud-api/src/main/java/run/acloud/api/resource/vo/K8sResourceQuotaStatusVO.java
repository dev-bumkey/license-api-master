package run.acloud.api.resource.vo;

import com.google.common.collect.Maps;
import com.google.gson.annotations.SerializedName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * @author: wschoi@acornsoft.io
 * Created on 2017. 10. 12.
 */
@Getter
@Setter
@EqualsAndHashCode
public class K8sResourceQuotaStatusVO {

    public static final String SERIALIZED_NAME_HARD = "hard";
    @SerializedName(SERIALIZED_NAME_HARD)
    @Schema(
            name = SERIALIZED_NAME_HARD,
            title =  "Hard is the set of enforced hard limits for each named resource. More info: https://kubernetes.io/docs/concepts/policy/resource-quotas/"
    )
    private Map<String, String> hard = null;

    public K8sResourceQuotaStatusVO putHardItem(String key, String hardItem) {
        if (this.hard == null) {
            this.hard = Maps.newHashMap();
        }
        this.hard.put(key, hardItem);
        return this;
    }

    public static final String SERIALIZED_NAME_USED = "used";
    @SerializedName(SERIALIZED_NAME_USED)
    @Schema(
            name = SERIALIZED_NAME_USED,
            title =  "Used is the current observed total usage of the resource in the namespace."
    )
    private Map<String, String> used = null;

    public K8sResourceQuotaStatusVO putUsedItem(String key, String usedItem) {
        if (this.used == null) {
            this.used = Maps.newHashMap();
        }
        this.used.put(key, usedItem);
        return this;
    }
}
