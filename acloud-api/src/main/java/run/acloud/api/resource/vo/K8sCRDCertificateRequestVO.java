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
@Schema(description = "Cert-Manager CRD Certificate Request 모델")
public class K8sCRDCertificateRequestVO extends BaseVO {

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


    @Schema(title = "issuerRef", description = "참조 발급자 객체")
    private K8sCRDIssuerRefVO issuerRef;

    @Schema(title = "duration", description = "요청된 인증서 기간")
    private String duration;

    @Schema(title = "extra", description = "인증서 요청을 생성한 사용자 추가 속성")
    private Map<String, List<String>> extra;

    @Schema(title = "groups", description = "인증서 요청을 생성한 사용자 그룹")
    private List<String> groups;

    @Schema(title = "isCA", description = "CA 여부")
    private Boolean isCA;

    @Schema(title = "request", description = "PEM-encoded x509 certificate 요청")
    private String request;

    @Schema(title = "uid", description = "인증서 요청을 생성한 사용자 uid")
    private String uid;

    @Schema(title = "usages", description = "용도, CERT_USAGES enum")
    private List<String> usages;

    @Schema(title = "uris", description = "uris, SAM(subjectAltNames)")
    private List<String> uris;

    @Schema(title = "username", description = "인증서 요청을 생성한 사용자 이름")
    private String username;

    @Schema(title = "ready", description = "conditions.type=Ready")
    private String ready;

    @Schema(title = "Approved", description = "conditions.type=Approved")
    private String approved;

    @Schema(title = "Denied", description = "conditions.type=Denied")
    private String denied;

    @Schema(title = "order", description = "Order 정보")
    private K8sCRDOrderVO order;

    @Schema(title = "Deployment 상태")
    private List<K8sConditionVO> conditions;

    @Schema(title = "event")
    private List<K8sEventVO> events;

    @Schema(title = "배포 정보 (json)")
    private String deployment;

    @Schema(title = "배포 정보 (yaml)")
    private String deploymentYaml;
}
