package run.acloud.api.build.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import run.acloud.api.build.enums.StepType;
import run.acloud.commons.vo.HasUseYnVO;

@Getter
@Setter
@Schema(name="BuildStepVO", description = "빌드단계 모델")
public class BuildStepVO extends HasUseYnVO {

    @Schema(title = "빌드 단계 번호")
    private Integer buildStepSeq;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    @JsonIgnore
    private Integer buildSeq;

    @Schema(title = "단계 종류")
    @NotNull
    private StepType stepType;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    @JsonIgnore
    private String stepConfig;

    @Schema(title = "단계 순서")
    @NotNull
    @Min(value = 0)
    private Integer stepOrder;

    @Schema(title = "빌드 단계 설정", oneOf = {StepInitVO.class, StepCodeDownVO.class, StepUserTaskVO.class, StepFtpVO.class, StepHttpVO.class, StepShellVO.class, StepCreateImageVO.class, StepCancelVO.class})
    @NotNull
    private BuildStepAddVO buildStepConfig;

    @Schema(title = "빌드 단계 사용 여부")
    private boolean useFlag;

    @Schema(title = "빌드 단계 실행 번호", description = "빌드 이력 정보로 부터 생성될때만 존재함.")
    private Integer buildStepRunSeq;

    @Schema(title = "빌드 실행 번호", description = "빌드 이력 정보로 부터 생성될때만 존재함.")
    private Integer buildRunSeq;

}

