package run.acloud.api.resource.vo;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(name = "K8sCRDCertificateGuiVO"
        , title = "K8sCRDCertificateGuiVO"
        , allOf = {K8sCRDCertificateIntegrateVO.class}
        , description = "GUI 배포 모델"
)
public class K8sCRDCertificateGuiVO extends K8sCRDCertificateIntegrateVO{

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

    @Schema(title = "issuerRef", description = "참조 발급자 객체")
    private K8sCRDIssuerRefVO issuerRef;

    @Schema(title = "duration", description = "인증서 사용기간, 최소 1시간")
    private String duration;

    @Schema(title = "duration", description = "인증서 갱신기간, 최소 5분")
    private String renewBefore;

    @Schema(title = "revisionHistoryLimit", description = "revision history limit 수")
    private Integer revisionHistoryLimit;

    @Schema(title = "usages", description = "용도, CERT_USAGES enum")
    private List<String> usages;

}
