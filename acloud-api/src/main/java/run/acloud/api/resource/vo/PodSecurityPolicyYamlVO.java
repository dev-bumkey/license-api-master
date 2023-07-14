package run.acloud.api.resource.vo;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.NotBlank;

/**
 * @author: wschoi@acornsoft.io
 * Created on 2017. 10. 12.
 */
@Getter
@Setter
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "deployType",
    visible = true
)
@EqualsAndHashCode
@Schema(name = "PodSecurityPolicyYamlVO"
        , title = "PodSecurityPolicyYamlVO"
        , allOf = {PodSecurityPolicyIntegrateVO.class}
        , description = "YAML 배포 모델"
)
public class PodSecurityPolicyYamlVO extends PodSecurityPolicyIntegrateVO{

    @Schema(title = "name")
    private String name;

    @Schema(title = "default", allowableValues = {"true","false"})
    private boolean isDefault = false;

    @Schema(title = "배포 yaml 문자열")
    @NotBlank
    private String yaml;

}
