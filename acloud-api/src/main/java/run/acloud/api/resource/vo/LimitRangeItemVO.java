package run.acloud.api.resource.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Maps;
import com.google.gson.annotations.SerializedName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import run.acloud.api.resource.constants.KubeConstants;
import run.acloud.commons.vo.BaseVO;

import java.util.Map;

/**
 * @author: wschoi@acornsoft.io
 * Created on 2017. 10. 12.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@Schema(description = "LimitRangeItem defines a min/max usage limit for any resource that matches on kind.")
public class LimitRangeItemVO extends BaseVO{

    public static final String SERIALIZED_NAME_DEFAULT = "default";
    @SerializedName(SERIALIZED_NAME_DEFAULT)
    @JsonProperty(SERIALIZED_NAME_DEFAULT)
    @Schema(
            name = SERIALIZED_NAME_DEFAULT,
            allowableValues = {KubeConstants.RESOURCES_CPU, KubeConstants.RESOURCES_MEMORY, KubeConstants.RESOURCES_GPU, KubeConstants.RESOURCES_STORAGE},
            description =  "Default resource requirement limit value by resource name if resource limit is omitted."
    )
    private Map<String, String> _default = null;

    public LimitRangeItemVO putDefaultItem(String key, String _defaultItem) {
        if (this._default == null) {
            this._default = Maps.newHashMap();
        }
        this._default.put(key, _defaultItem);
        return this;
    }

    public static final String SERIALIZED_NAME_DEFAULT_REQUEST = "defaultRequest";
    @SerializedName(SERIALIZED_NAME_DEFAULT_REQUEST)
    @Schema(
            name = SERIALIZED_NAME_DEFAULT_REQUEST,
            allowableValues = {KubeConstants.RESOURCES_CPU, KubeConstants.RESOURCES_MEMORY, KubeConstants.RESOURCES_GPU, KubeConstants.RESOURCES_STORAGE},
            description =  "DefaultRequest is the default resource requirement request value by resource name if resource request is omitted."
    )
    private Map<String, String> defaultRequest = null;

    public LimitRangeItemVO putDefaultRequestItem(String key, String defaultRequestItem) {
        if (this.defaultRequest == null) {
            this.defaultRequest = Maps.newHashMap();
        }
        this.defaultRequest.put(key, defaultRequestItem);
        return this;
    }

    public static final String SERIALIZED_NAME_MAX = "max";
    @SerializedName(SERIALIZED_NAME_MAX)
    @Schema(
            name = SERIALIZED_NAME_MAX,
            allowableValues = {KubeConstants.RESOURCES_CPU, KubeConstants.RESOURCES_MEMORY, KubeConstants.RESOURCES_GPU, KubeConstants.RESOURCES_STORAGE},
            description =  "Max usage constraints on this kind by resource name."
    )
    private Map<String, String> max = null;

    public LimitRangeItemVO putMaxItem(String key, String maxItem) {
        if (this.max == null) {
            this.max = Maps.newHashMap();
        }
        this.max.put(key, maxItem);
        return this;
    }

    public static final String SERIALIZED_NAME_MAX_LIMIT_REQUEST_RATIO = "maxLimitRequestRatio";
    @SerializedName(SERIALIZED_NAME_MAX_LIMIT_REQUEST_RATIO)
    @Schema(
            name = SERIALIZED_NAME_MAX_LIMIT_REQUEST_RATIO,
            allowableValues = {KubeConstants.RESOURCES_CPU, KubeConstants.RESOURCES_MEMORY, KubeConstants.RESOURCES_GPU, KubeConstants.RESOURCES_STORAGE},
            description =  "MaxLimitRequestRatio if specified, the named resource must have a request and limit that are both non-zero where limit divided by request is less than or equal to the enumerated value; this represents the max burst for the named resource."
    )
    private Map<String, String> maxLimitRequestRatio = null;

    public LimitRangeItemVO putMaxLimitRequestRatioItem(String key, String maxLimitRequestRatioItem) {
        if (this.maxLimitRequestRatio == null) {
            this.maxLimitRequestRatio = Maps.newHashMap();
        }
        this.maxLimitRequestRatio.put(key, maxLimitRequestRatioItem);
        return this;
    }

    public static final String SERIALIZED_NAME_MIN = "min";
    @SerializedName(SERIALIZED_NAME_MIN)
    @Schema(
            name = SERIALIZED_NAME_MIN,
            allowableValues = {KubeConstants.RESOURCES_CPU, KubeConstants.RESOURCES_MEMORY, KubeConstants.RESOURCES_GPU, KubeConstants.RESOURCES_STORAGE},
            description =  "Min usage constraints on this kind by resource name."
    )
    private Map<String, String> min = null;

    public LimitRangeItemVO putMinItem(String key, String minItem) {
        if (this.min == null) {
            this.min = Maps.newHashMap();
        }
        this.min.put(key, minItem);
        return this;
    }

    public static final String SERIALIZED_NAME_TYPE = "type";
    @SerializedName(SERIALIZED_NAME_TYPE)
    @Schema(
            name = SERIALIZED_NAME_TYPE,
            allowableValues = {"Pod","Container","PersistentVolumeClaim"},
            description =  "Type of resource that this limit applies to."
    )
    private String type;




}
