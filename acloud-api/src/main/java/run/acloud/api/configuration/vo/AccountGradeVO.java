package run.acloud.api.configuration.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import run.acloud.api.configuration.enums.GradeApplyState;
import run.acloud.commons.vo.HasUseYnVO;

import java.io.Serializable;

@Getter
@Setter
@EqualsAndHashCode
@Schema(description = "계정 등급 모델")
public class AccountGradeVO extends HasUseYnVO implements Serializable {

    @Schema(title = "계정 등급 번호")
    private Integer accountGradeSeq;

    @Schema(title = "계정 번호", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer accountSeq;

    @Schema(title = "등급 번호", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer gradeSeq;

    @Schema(title = "등급명")
    private String gradeName;

    @Schema(title = "계정 등급  설명")
    private String accountGradeDesc;

    @Schema(title = "동시 빌드 수")
    private Integer parallelBuildCnt;

    @Schema(title = "총 빌드 갯수")
    private Integer totalBuildCnt;

    @Schema(title = "워크스페이스 수")
    private Integer workspaceCnt;

    @Schema(title = "Core 수")
    private Integer coreCnt;

    @Schema(title = "로그 제공 여부, default = N")
    private String logEnableYn = "N";

    @Schema(title = "등급 적용 상태")
    private GradeApplyState applyState;

    @Schema(title = "등급 적용 시작일, YYYY-MM-DD")
    private String applyStartDate;

    @Schema(title = "등급 적용 종료일, YYYY-MM-DD")
    private String applyEndDate;
}
