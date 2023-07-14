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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author wschoi@acornsoft.io
 * Created on 2017. 7. 26.
 */
@Getter
@Setter
@EqualsAndHashCode
@Schema(name = "LimitRangeGuiVO", title = "LimitRangeGuiVO", allOf = {LimitRangeIntegrateVO.class}, description = "GUI 배포 모델")
public class LimitRangeGuiVO extends LimitRangeIntegrateVO implements Serializable {

    private static final long serialVersionUID = 7872159927713997797L;

    @Schema(description = "서비스맵 일련 번호", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer servicemapSeq;

    @Schema(description = "LimitRange 이름", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(description = "namespace", requiredMode = Schema.RequiredMode.REQUIRED)
    private String namespace;

    @Schema(title = "LimitRange 설명")
    private String description;

    @Schema(title = "default", allowableValues = {"true","false"})
    private boolean isDefault = false;

    @Schema(title = "생성시간")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private OffsetDateTime creationTimestamp;

    @Schema(description = "label")
    private Map<String, String> labels;

    public LimitRangeGuiVO putLabelsItem(String key, String labelsItem) {
        if (this.labels == null) {
            this.labels = Maps.newHashMap();
        }
        this.labels.put(key, labelsItem);
        return this;
    }

    @Schema(description = "annotations")
    private Map<String, String> annotations;

    public LimitRangeGuiVO putAnnotationsItem(String key, String annotationsItem) {
        if (this.annotations == null) {
            this.annotations = Maps.newHashMap();
        }
        this.annotations.put(key, annotationsItem);
        return this;
    }

    public static final String SERIALIZED_NAME_LIMITS = "limits";
    @Schema(
            requiredMode = Schema.RequiredMode.REQUIRED,
            name = SERIALIZED_NAME_LIMITS,
            description =  "LimitRangeSpec defines a min/max usage limit for resources that match on kind."
    )
    @SerializedName(SERIALIZED_NAME_LIMITS)
    private List<LimitRangeItemVO> limits = new ArrayList<LimitRangeItemVO>();

    public LimitRangeGuiVO addLimitsItem(LimitRangeItemVO limitsItem) {
        if (this.limits == null) {
            this.limits = Lists.newArrayList();
        }
        this.limits.add(limitsItem);
        return this;
    }

}
