package run.acloud.api.resource.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.NotBlank;
import run.acloud.api.resource.enums.K8sApiKindType;

import java.io.Serializable;

@Getter
@Setter
@Schema(title = "CommonYamlVO", description = "공통 Yaml 배포 모델")
public class CommonYamlVO implements Serializable{
    private static final long serialVersionUID = -7447390551952274076L;

    @Schema(title = "deployType", allowableValues = {"GUI","YAML"})
    private String deployType;

    @Schema(title = "네임스페이스명")
	private String namespace;

    @Schema(title = "이름")
    private String name;

    @Schema(title = "배포 yaml 문자열")
    @NotBlank
    private String yaml;

    @Schema(title = "K8s API 유형")
    private K8sApiKindType k8sApiKindType;

    // internal use only
    @JsonIgnore
    @Schema(title = "Custom Object의 Group")
    private String customObjectGroup;

    // internal use only
    @JsonIgnore
    @Schema(title = "Custom Object의 Version")
    private String customObjectVersion;

    // internal use only
    @JsonIgnore
    @Schema(title = "Custom Object의 Plural")
    private String customObjectPlural;

    // internal use only
    @JsonIgnore
    @Schema(title = "Custom Object의 Kind")
    private String customObjectKind;
}
