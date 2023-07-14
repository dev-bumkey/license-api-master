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
@Schema(description = "Job 상세 모델")
public class K8sJobDetailVO extends BaseVO{

    @Schema(title = "Job 명")
    private String name;

    @Schema(title = "네임스페이스")
    private String namespace;

    @Schema(title = "라벨")
    private Map<String, String> labels;

    @Schema(title = "어노테이션")
    private Map<String, String> annotations;

    @Schema(title = "ownerReferences")
    private List<K8sOwnerReferenceVO> ownerReferences;

    @Schema(title = "생성 시간")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private OffsetDateTime creationTime;

    @Schema(title = "Selector")
    private K8sLabelSelectorVO selector;

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

    @Schema(title = "status", description = "(0 Running / 1 Succeeded / 0 Failed), Current status of a job. More info: https://git.k8s.io/community/contributors/devel/api-conventions.md#spec-and-status")
    private Map<String, Integer> status;

    @Schema(title = "podTemplate")
    private K8sPodTemplateSpecVO podTemplate;
}
