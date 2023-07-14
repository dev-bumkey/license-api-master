package run.acloud.api.build.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(name = "StepCancelVO", title = "StepCancelVO", allOf = {BuildStepAddVO.class}, description = "Build 취소 모델")
public class StepCancelVO extends BuildStepAddVO {
    private Integer refBuildRunSeq;
}
