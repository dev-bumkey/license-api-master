package run.acloud.api.configuration.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import run.acloud.commons.vo.HasUseYnVO;

import java.io.Serializable;

@Getter
@Setter
@Schema(description = "계정 미터링 모델")
public class AccountMeteringVO extends HasUseYnVO implements Serializable {

    @Schema(title = "계정 번호", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer accountSeq;

    @Schema(title = "등급 번호", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer gradeSeq;

    @Schema(title = "동시 빌드 수", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer parallelBuildCnt;

    @Schema(title = "총 빌드 갯수", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer totalBuildCnt;

    @Schema(title = "워크스페이스 수", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer workspaceCnt;

    @Schema(title = "Core 수", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer coreCnt;

    @Schema(title = "노드 수", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer nodeCnt;

}
