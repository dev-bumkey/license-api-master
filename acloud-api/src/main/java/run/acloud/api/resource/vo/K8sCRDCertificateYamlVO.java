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
@Schema(name = "K8sCRDCertificateYamlVO"
        , title = "K8sCRDCertificateYamlVO"
        , allOf = {K8sCRDCertificateIntegrateVO.class}
        , description = "YAML 배포 모델"
)
public class K8sCRDCertificateYamlVO extends K8sCRDCertificateIntegrateVO{

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

    @Schema(title = "배포 yaml 문자열")
    @NotBlank
    private String yaml;

}
