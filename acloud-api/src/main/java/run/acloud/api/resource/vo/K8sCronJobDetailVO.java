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
@Schema(description = "CronJob 상세 모델")
public class K8sCronJobDetailVO extends BaseVO{

    @Schema(title = "CronJob 명")
    private String name;

    @Schema(title = "네임스페이스")
    private String namespace;

    @Schema(title = "라벨")
    private Map<String, String> labels;

    @Schema(title = "어노테이션")
    private Map<String, String> annotations;

    @Schema(title = "생성 시간")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private OffsetDateTime creationTime;

    @Schema(title = "concurrencyPolicy")
    private String concurrencyPolicy;

    @Schema(title = "schedule")
    private String schedule;

    @Schema(title = "startingDeadlineSeconds")
    private Long startingDeadlineSeconds;

    @Schema(title = "successfulJobsHistoryLimit")
    private Integer successfulJobsHistoryLimit;

    @Schema(title = "failedJobsHistoryLimit")
    private Integer failedJobsHistoryLimit;

    @Schema(title = "suspend")
    private Boolean suspend;

    @Schema(title = "activeDeadlineSeconds")
    private Long activeDeadlineSeconds;

    @Schema(title = "backoffLimit")
    private Integer backoffLimit;

    @Schema(title = "completions")
    private Integer completions;

    @Schema(title = "parallelism")
    private Integer parallelism;

    @Schema(title = "ttlSecondsAfterFinished")
    private Integer ttlSecondsAfterFinished;

    @Schema(title = "마지막 schedule 시간")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private OffsetDateTime lastScheduleTime;

    @Schema(title = "activeJob 목록")
    private List<K8sJobVO> activeJobs;

    @Schema(title = "podTemplate")
    private K8sPodTemplateSpecVO podTemplate;
}
