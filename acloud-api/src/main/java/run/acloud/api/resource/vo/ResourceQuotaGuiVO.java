package run.acloud.api.resource.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
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
@Schema(name = "ResourceQuotaGuiVO",
        title = "ResourceQuotaGuiVO",
        description = "ResourceQuota 스펙 배포 GUI 모델",
        allOf = {ResourceQuotaIntegrateVO.class}
)
public class ResourceQuotaGuiVO extends ResourceQuotaIntegrateVO implements Serializable {

    private static final long serialVersionUID = -4261909293060482745L;

    @Schema(description = "서비스맵 일련 번호", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer servicemapSeq;

    @Schema(description = "ResourceQuota 이름", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(description = "namespace", requiredMode = Schema.RequiredMode.REQUIRED)
    private String namespace;

    @Schema(title = "ResourceQuota 설명")
    private String description;

    @Schema(title = "default", allowableValues = {"true","false"})
    private boolean isDefault = false;

    @Schema(title = "생성시간")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private OffsetDateTime creationTimestamp;

    @Schema(description = "label")
    private Map<String, String> labels;

    public ResourceQuotaGuiVO putLabelsItem(String key, String labelsItem) {
        if (this.labels == null) {
            this.labels = Maps.newHashMap();
        }
        this.labels.put(key, labelsItem);
        return this;
    }

    @Schema(description = "annotations")
    private Map<String, String> annotations;

    public ResourceQuotaGuiVO putAnnotationsItem(String key, String annotationsItem) {
        if (this.annotations == null) {
            this.annotations = Maps.newHashMap();
        }
        this.annotations.put(key, annotationsItem);
        return this;
    }

    public static final String SERIALIZED_NAME_HARD = "hard";
    @SerializedName(SERIALIZED_NAME_HARD)
    @Schema(
            name = SERIALIZED_NAME_HARD,
            description =  "hard is the set of desired hard limits for each named resource. More info: https://kubernetes.io/docs/concepts/policy/resource-quotas/"
    )
    private Map<String, String> hard = null;

    public ResourceQuotaGuiVO putHardItem(String key, String hardItem) {
        if (this.hard == null) {
            this.hard = Maps.newHashMap();
        }
        this.hard.put(key, hardItem);
        return this;
    }

    public static final String SERIALIZED_NAME_SCOPE_SELECTOR = "scopeSelector";
    @SerializedName(SERIALIZED_NAME_SCOPE_SELECTOR)
    @Schema(
            name = SERIALIZED_NAME_SCOPE_SELECTOR,
            description =  "A scope selector represents the AND of the selectors represented by the scoped-resource selector requirements."
    )
    private ScopeSelectorVO scopeSelector;

    public static final String SERIALIZED_NAME_SCOPES = "scopes";
    @SerializedName(SERIALIZED_NAME_SCOPES)
    @Schema(
            name = SERIALIZED_NAME_SCOPES,
            description =  "A collection of filters that must match each object tracked by a quota. If not specified, the quota matches all objects."
    )
    private List<String> scopes = null;

}
