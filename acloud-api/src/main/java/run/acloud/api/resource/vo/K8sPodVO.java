package run.acloud.api.resource.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import run.acloud.api.cserver.vo.ServicemapSummaryVO;
import run.acloud.commons.vo.BaseVO;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * @author: wschoi@acornsoft.io
 * Created on 2017. 10. 12.
 */
@Getter
@Setter
@Schema(description = "Pod 모델")
public class K8sPodVO extends BaseVO {

    @Schema(title = "라벨")
    private String label;

    @Schema(title = "네임스페이스")
    private String namespace;

    @Schema(title = "Pod 명")
    private String podName;

    @Schema(title = "Node 명")
    private String nodeName;

    @Schema(title = "Pod 상태")
    private String podStatus;

    @Schema(title = "재시작 횟수")
    private int restartCnt;

    @Schema(title = "시작 시간")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private OffsetDateTime startTime;

    @Schema(title = "CPU 사용량", hidden = true)
    @JsonIgnore
    private float cpuUsage;

    @Schema(title = "Memory 사용량", hidden = true)
    @JsonIgnore
    private float memUsage;

    @Schema(title = "Pod 배포 정보")
    private String podDeployment;

    @Schema(title = "Pod 상세")
    private K8sPodDetailVO detail;

    @Schema(title = "Pod 상태")
    private List<K8sConditionVO> conditions;

    @Schema(title = "Pod 배포 정보 (YAML)")
    private String podDeploymentYaml;

    @Schema(title = "Pod event")
    private List<K8sEventVO> events;

    @Schema(title = "클러스터순번")
    private Integer clusterSeq;

    @Schema(title = "클러스터이름")
    private String clusterName;

    @Schema(title = "서비스맵 정보")
    private ServicemapSummaryVO servicemapInfo;
}
