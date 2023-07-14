package run.acloud.api.resource.vo;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * @author wschoi@acornsoft.io
 * Created on 2017. 7. 26.
 */
@Getter
@Setter
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "deployType",
        visible = true
)
@Schema(name = "ResourceQuotaYamlVO",
        title = "ResourceQuotaYamlVO",
        description = "ResourceQuota 스펙 배포 YAML 모델",
        allOf = {ResourceQuotaIntegrateVO.class}
)
public class ResourceQuotaYamlVO extends ResourceQuotaIntegrateVO implements Serializable {

    private static final long serialVersionUID = -5827134125564834012L;

    @Schema(description = "Appmap 일련 번호", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer appmapSeq;

    @Schema(description = "ResourceQuota 이름", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(description = "namespace", requiredMode = Schema.RequiredMode.REQUIRED)
    private String namespace;

    @Schema(title = "default", allowableValues = {"true","false"})
    private boolean isDefault = false;

    @Schema(description = "yaml")
    private String yaml;

}
