package run.acloud.api.resource.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import run.acloud.commons.vo.BaseVO;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * @author: wschoi@acornsoft.io
 * Created on 2017. 10. 12.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@Schema(description = "Cert-Manager CRD Issuer 모델")
public class K8sCRDIssuerVO extends BaseVO {

    @Schema(title = "scope", description = "배포유형 CLUSTER(ClusterIssuer), NAMESPACED(Issuer), CertIssuerScope enum 참조", allowableValues = {"CLUSTER", "NAMESPACED"})
    private String scope;

    @Schema(title = "group", description = "발급자 리소스 group, cert-manager.io/v1")
    private String group;

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

    @Schema(title = "labels")
    private Map<String, String> labels;

    @Schema(title = "annotations")
    private Map<String, String> annotations;

    @Schema(title = "생성시간")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private OffsetDateTime creationTimestamp;

    @Schema(title = "ownerReferences")
    private List<K8sOwnerReferenceVO> ownerReferences;

    @Schema(title = "issueType", description = "발급 유형", allowableValues = {"selfSigned", "ca"})
    private String issueType;

    @Schema(title = "selfSigned")
    private K8sCRDIssuerSelfSignedVO selfSigned;

    @Schema(title = "ca")
    private K8sCRDIssuerCAVO ca;

    @Schema(title = "Deployment 상태")
    private List<K8sConditionVO> conditions;

    @Schema(title = "statusAcme")
    private String statusAcme;

    @Schema(title = "event")
    private List<K8sEventVO> events;

    @Schema(title = "배포 정보 (json)")
    private String deployment;

    @Schema(title = "배포 정보 (yaml)")
    private String deploymentYaml;
}
