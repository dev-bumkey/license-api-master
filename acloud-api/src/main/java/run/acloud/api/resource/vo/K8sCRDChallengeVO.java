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
@Schema(description = "Cert-Manager CRD Challenge 모델")
public class K8sCRDChallengeVO extends BaseVO {

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

    @Schema(title = "status")
    private Map<String, Object> status;

    @Schema(title = "event")
    private List<K8sEventVO> events;

    @Schema(title = "배포 정보 (json)")
    private String deployment;

    @Schema(title = "배포 정보 (yaml)")
    private String deploymentYaml;
}
