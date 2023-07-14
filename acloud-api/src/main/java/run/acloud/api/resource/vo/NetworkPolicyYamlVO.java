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
@Schema(name = "NetworkPolicyYamlVO", title = "NetworkPolicyYamlVO", allOf = {NetworkPolicyIntegrateVO.class}, description = "Yaml 배포 모델")
public class NetworkPolicyYamlVO extends NetworkPolicyIntegrateVO implements Serializable {

    private static final long serialVersionUID = 7823208283582079923L;

    @Schema(description = "Appmap 일련 번호")
    private Integer appmapSeq;

    @Schema(description = "NetworkPolicy 이름", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(description = "namespace", requiredMode = Schema.RequiredMode.REQUIRED)
    private String namespace;

    @Schema(title = "default", allowableValues = {"true","false"})
    private boolean isDefault = false;

    @Schema(description = "yaml")
    private String yaml;

}
