package run.acloud.api.pipelineflow.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "파이프라인 빌드 갯수 모델")
public class PipelineCountVO {
    private Integer buildSeq;
    private Integer cnt;
}
