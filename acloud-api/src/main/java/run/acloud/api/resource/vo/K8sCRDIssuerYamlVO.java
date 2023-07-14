package run.acloud.api.resource.vo;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(name = "K8sCRDIssuerYamlVO"
        , title = "K8sCRDIssuerYamlVO"
        , allOf = {K8sCRDIssuerIntegrateVO.class}
        , description = "YAML 배포 모델"
)
public class K8sCRDIssuerYamlVO extends K8sCRDIssuerIntegrateVO{

    @Schema(title = "scope", description = "배포유형", allowableValues = {"CLUSTER", "NAMESPACED"})
    private String scope;

    @Schema(title = "clusterSeq")
    private Integer clusterSeq;

    @Schema(title = "clusterId")
    private String clusterId;

    @Schema(title = "clusterName")
    private String clusterName;

    @Schema(title = "namespace")
    private String namespace;

    @Schema(title = "name")
    private String name;

    @Schema(title = "issueType", description = "발급 유형", allowableValues = {"selfSigned", "ca", "acme", "vault", "venafi"})
    private String issueType;

    @Schema(title = "배포 yaml 문자열")
    @NotBlank
    private String yaml;

}
