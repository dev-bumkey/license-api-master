package run.acloud.api.resource.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

/**
 * @author: wschoi@acornsoft.io
 * Created on 2017. 10. 12.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@Schema(name = "IngressSpecGuiVO"
        , title = "IngressSpecGuiVO"
        , allOf = {IngressSpecIntegrateVO.class}
        , description = "GUI 배포 모델"
)
public class IngressSpecGuiVO extends IngressSpecIntegrateVO{

    @Schema(title = "servicemapSeq")
    private Integer servicemapSeq;

    @Schema(title = "clusterSeq")
    private Integer clusterSeq;

    @Schema(title = "namespaceName")
    private String namespaceName;

    @Schema(title = "Ingress 명")
    private String name;

    @Schema(title = "Ingress Controller 명")
    private String ingressControllerName;

    @Schema(title = "SSL Redirect 사용여부", allowableValues = {"false","true"})
    private Boolean useSslRedirect;

    @Schema(title = "Ingress Rules")
    @JsonProperty("rules")
    private List<IngressRuleVO> ingressRules;

    @Schema(title = "Ingress TLS")
    @JsonProperty("tls")
    @SerializedName("tls")
    private List<IngressTLSVO> ingressTLSs;

    @Schema(title = "labels")
    private Map<String, String> labels;

    @Schema(title = "annotations")
    private Map<String, String> annotations;

}
