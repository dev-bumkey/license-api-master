package run.acloud.api.resource.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import run.acloud.api.cserver.vo.ServicemapSummaryVO;
import run.acloud.commons.vo.BaseVO;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * @author: wschoi@acornsoft.io
 * Created on 2017. 10. 12.
 */
@Getter
@Setter
@Schema(description = "Namespace 모델")
public class K8sNamespaceVO extends BaseVO{

    @Schema(title = "네임스페이스")
    private String name;

    @Schema(title = "라벨")
    private Map<String, String> labels;

    @Schema(title = "주석")
    private Map<String, String> annotations;

    @Schema(title = "status")
    private String status;

    @Schema(title = "생성시간")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private OffsetDateTime creationTimestamp;

    @Schema(title = "cpu 사용량")
    private double cpuUsage;

    @Schema(title = "cpu 요청")
    private double cpuRequests;

    @Schema(title = "cpu 제한")
    private double cpuLimits;

    @Schema(title = "memory 사용량")
    private long memoryUsage;

    @Schema(title = "memory 요청")
    private long memoryRequests;

    @Schema(title = "memory 제한")
    private long memoryLimits;

    @Schema(title = "namespace 배포 정보")
    private String deployment;

    @Schema(title = "namespace 배포 정보 (yaml)")
    private String deploymentYaml;

    @Schema(title = "Namespace 상세")
    private K8sNamespaceDetailVO detail;

    @Schema(title = "클러스터순번")
    private Integer clusterSeq;

    @Schema(title = "클러스터이름")
    private String clusterName;

    @Schema(title = "서비스맵 정보")
    private ServicemapSummaryVO servicemapInfo;
}
