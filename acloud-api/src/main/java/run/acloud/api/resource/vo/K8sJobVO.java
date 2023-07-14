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
@Schema(description = "Job 모델")
public class K8sJobVO extends BaseVO {

    @Schema(title = "라벨")
    private String label;

    @Schema(title = "네임스페이스")
    private String namespace;

    @Schema(title = "Job 명")
    private String name;

    @Schema(title = "라벨")
    private Map<String, String> labels;

    @Schema(title = "완료된 Pod 수")
    private int completionPodCnt;

    @Schema(title = "제공할 Pod 수")
    private int desiredPodCnt;

    @Schema(title = "생성시간")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private OffsetDateTime creationTimestamp;

    @Schema(title = "이미지 목록")
    private List<String> images;

    @Schema(title = "Job 배포 정보")
    private String deployment;

    @Schema(title = "Job 배포 정보 (yaml)")
    private String deploymentYaml;

    @Schema(title = "Job 상세")
    private K8sJobDetailVO detail;

    @Schema(title = "event")
    private List<K8sEventVO> events;

}
