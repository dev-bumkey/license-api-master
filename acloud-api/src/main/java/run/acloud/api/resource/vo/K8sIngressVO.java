package run.acloud.api.resource.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import run.acloud.commons.vo.BaseVO;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * @author: wschoi@acornsoft.io
 * Created on 2017. 10. 12.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@Schema(description = "k8s Ingress 모델")
public class K8sIngressVO extends BaseVO {

    @Schema(title = "네임스페이스")
    private String namespace;

    @Schema(title = "Ingress 명")
    private String name;

    @Schema(title = "endpoint 목록")
    private List<String> endpoints;

    @Schema(title = "생성시간")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private OffsetDateTime creationTimestamp;

    @Schema(title = "Ingress 배포 정보")
    private String deployment;

    @Schema(title = "Ingress 배포 정보 (yaml)")
    private String deploymentYaml;

    @Schema(title = "Ingress 상세")
    private K8sIngressDetailVO detail;

    @Schema(title = "Ingress 스펙")
    private IngressSpecGuiVO ingressSpec;
}
