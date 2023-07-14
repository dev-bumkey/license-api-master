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
@Schema(description = "k8s Service 모델")
public class K8sServiceVO extends BaseVO{

    @Schema(title = "네임스페이스")
    private String namespace;

    @Schema(title = "Service 명")
    private String serviceName;

    @Schema(title = "라벨")
    private Map<String, String> labels;

    @Schema(title = "Cluster IP")
    private String clusterIP;

    @Schema(title = "Internal LB Flag")
    private boolean internalLBFlag = false;

    @Schema(allowableValues = {"ClientIP","None"}, title = "Session Affinity")
    private String sessionAffinity;

    @Schema(title = "Session Affinity TimeoutSeconds")
    private Integer sessionAffinityTimeoutSeconds;

    @Schema(title = "내부 endpoint")
    private List<String> internalEndpoints;

    @Schema(title = "외부 endpoint")
    private List<String> externalEndpoints;

    @Schema(title = "생성시간")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private OffsetDateTime creationTimestamp;

    @Schema(title = "Service 배포 정보")
    private String serviceDeployment;

    @Schema(title = "Service 배포 정보(Yaml)")
    private String serviceDeploymentYaml;

    @Schema(title = "Service 상세")
    private K8sServiceDetailVO detail;

    @Schema(title = "Endpoint 목록")
    private List<K8sEndpointVO> endpoints;

    @Schema(title = "Port 목록")
    private List<K8sServicePortVO> servicePorts;

    private K8sIngressVO matchedIngress;

    private List<K8sEventVO> events;
}
