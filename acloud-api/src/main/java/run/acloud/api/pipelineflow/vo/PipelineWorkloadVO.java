package run.acloud.api.pipelineflow.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import run.acloud.commons.vo.HasUseYnVO;

import java.util.List;

/**
 * @author: coolingi@acornsoft.io
 * Created on 2019. 9. 26.
 */
@Getter
@Setter
@Schema(description = "파이프라인 Workload 모델")
public class PipelineWorkloadVO extends HasUseYnVO {

    @Schema(title = "파이프라인 workload 번호", example = "1")
    private Integer pipelineWorkloadSeq;

    @Schema(title = "클러스터 번호", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer clusterSeq;

    @Schema(title = "namespace 이름", example = "test-nameapace", requiredMode = Schema.RequiredMode.REQUIRED)
    private String namespaceName;

    @Schema(title = "workload 이름", example = "mysql-server", requiredMode = Schema.RequiredMode.REQUIRED)
    private String workloadName;

    @Schema(title = "파이프라인 대상 workload 상태", example = "RUNNING")
    private String workloadStateCode;

    @Schema(title = "파이프라인 대상 workload 유형", example = "JOB_SERVER")
    private String workloadType;

    @Schema(title = "파이프라인 Container 목록", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    @Size(min = 1)
    private List<PipelineContainerVO> pipelineContainers;


}

