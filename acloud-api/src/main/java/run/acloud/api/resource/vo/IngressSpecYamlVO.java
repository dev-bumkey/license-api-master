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
@EqualsAndHashCode(callSuper = false)
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "deployType",
        visible = true
)
@Schema(name = "IngressSpecYamlVO"
        , title = "IngressSpecYamlVO"
        , allOf = {IngressSpecIntegrateVO.class}
        , description = "Yaml 배포 모델"
)
public class IngressSpecYamlVO extends IngressSpecIntegrateVO{

    @Schema(title = "appmapSeq")
    @Deprecated
    private Integer appmapSeq;

    @Schema(title = "clusterSeq")
    private Integer clusterSeq;

    @Schema(title = "namespaceName")
    private String namespaceName;

    @Schema(title = "Ingress 명")
    private String name;

    @Schema(title = "배포 yaml 문자열")
    @NotBlank
    private String yaml;

}
