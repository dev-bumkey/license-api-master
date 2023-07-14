package run.acloud.api.resource.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * @author: wschoi@acornsoft.io
 * Created on 2017. 10. 12.
 */
@Getter
@Setter
@Schema(name = "K8sCRDIssuerGuiVO"
        , title = "K8sCRDIssuerGuiVO"
        , allOf = {K8sCRDIssuerIntegrateVO.class}
        , description = "GUI 배포 모델"
)
public class K8sCRDIssuerGuiVO extends K8sCRDIssuerIntegrateVO{

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

    @Schema(title = "issueType", description = "발급 유형", allowableValues = {"selfSigned", "ca"})
    private String issueType;

    @Schema(title = "labels")
    private Map<String, String> labels;

    @Schema(title = "annotations")
    private Map<String, String> annotations;

    @Schema(title = "selfSigned")
    private K8sCRDIssuerSelfSignedVO selfSigned;

    @Schema(title = "ca")
    private K8sCRDIssuerCAVO ca;
}
