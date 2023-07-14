package run.acloud.api.resource.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Replica Set 모델")
public class K8sReplicaSetVO extends BaseVO{

    @Schema(title = "라벨")
    private String label;

    @Schema(title = "네임스페이스")
    private String namespace;

    @Schema(title = "ReplicaSet 명")
    private String name;

    @Schema(title = "라벨")
    private Map<String, String> labels;

    @Schema(title = "준비된 Pod 수")
    private int readyPodCnt;

    @Schema(title = "제공할 Pod 수")
    private int desiredPodCnt;

    @Schema(title = "생성시간")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private OffsetDateTime creationTimestamp;

    @Schema(title = "이미지 목록")
    private List<String> images;

    @Schema(title = "ReplicaSet 배포 정보")
    private String deployment;

    @Schema(title = "ReplicaSet 상세")
    private K8sReplicaSetDetailVO detail;

    @Schema(title = "Horizontal Pod Autoscaler 목록")
    private List<K8sHorizontalPodAutoscalerVO> horizontalPodAutoscalers;

    @Schema(title = "ReplicaSet 배포 정보 Yaml")
    private String deploymentYaml;

    @Schema(title = "event")
    private List<K8sEventVO> events;

}
