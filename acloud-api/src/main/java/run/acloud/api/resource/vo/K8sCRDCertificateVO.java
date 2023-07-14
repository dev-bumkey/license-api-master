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
@Schema(description = "Cert-Manager CRD Certificate 모델")
public class K8sCRDCertificateVO extends BaseVO {

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


    @Schema(title = "isCA", description = "CA 여부")
    private Boolean isCA;

    @Schema(title = "commonName", description = "CN 명, 64자 제한")
    private String commonName;

    @Schema(title = "dnsNames", description = "dnsName, SAM(subjectAltNames)")
    private List<String> dnsNames;

    @Schema(title = "uris", description = "uris, SAM(subjectAltNames)")
    private List<String> uris;

    @Schema(title = "ipAddresses", description = "ipAddresses, SAM(subjectAltNames)")
    private List<String> ipAddresses;

    @Schema(title = "emailAddresses", description = "emailAddresses, SAM(subjectAltNames)")
    private List<String> emailAddresses;

    @Schema(title = "secretName", description = "생성될 인증서 secret 명")
    private String secretName;

    @Schema(title = "secret", description = "secret 객체")
    private SecretGuiVO secret;

    @Schema(title = "issuerRef", description = "참조 발급자 객체")
    private K8sCRDIssuerRefVO issuerRef;

    @Schema(title = "issuer", description = "발급자 객체")
    private K8sCRDIssuerVO issuer;

    @Schema(title = "duration", description = "인증서 사용기간, 최소 1시간")
    private String duration;

    @Schema(title = "duration", description = "인증서 갱신기간, 최소 5분")
    private String renewBefore;

    @Schema(title = "revisionHistoryLimit", description = "revision history limit 수")
    private Integer revisionHistoryLimit;

    @Schema(title = "usages", description = "용도, CERT_USAGES enum")
    private List<String> usages;

    @Schema(title = "certificateRequests", description = "인증서 요청 정보 목록")
    private List<K8sCRDCertificateRequestVO> certificateRequests;

    @Schema(title = "status")
    private Map<String, Object> status;

    @Schema(title = "Deployment 상태")
    private List<K8sConditionVO> conditions;

    @Schema(title = "event")
    private List<K8sEventVO> events;

    @Schema(title = "배포 정보 (json)")
    private String deployment;

    @Schema(title = "배포 정보 (yaml)")
    private String deploymentYaml;
}
