package run.acloud.api.configuration.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import run.acloud.api.configuration.enums.AccountType;
import run.acloud.commons.vo.HasUseYnVO;

import java.io.Serializable;

@Getter
@Setter
@Schema(description = "등급 계획 모델")
public class GradePlanVO extends HasUseYnVO implements Serializable {

    @Schema(title = "등급 번호")
    private Integer gradeSeq;

    @Schema(title = "계정 타입", requiredMode = Schema.RequiredMode.REQUIRED)
    private AccountType accountType;

    @Schema(title = "등급 이름", requiredMode = Schema.RequiredMode.REQUIRED)
    private String gradeName;

    @Schema(title = "등급 설명")
    private String gradeDesc;

    @Schema(title = "동시 빌드 수", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer parallelBuildCnt;

    @Schema(title = "총 빌드 갯수", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer totalBuildCnt;

    @Schema(title = "워크스페이스 수", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer workspaceCnt;

    @Schema(title = "Core 수", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer coreCnt;

    @Schema(title = "로그 제공 여부, default = N")
    private String logEnableYn = "N";

    @Schema(title = "정렬순서", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer sortOrder;

    @Schema(title = "수정 가능 여부, default값 'N' ")
    private String editableYn = "N";

}
