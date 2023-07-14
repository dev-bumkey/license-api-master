package run.acloud.api.build.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(name = "StepInitVO", title = "StepInitVO", allOf = {BuildStepAddVO.class}, description = "Build INIT 모델")
public class StepInitVO extends BuildStepAddVO {
}
