package run.acloud.api.pl.vo;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import run.acloud.api.build.enums.StepState;
import run.acloud.api.build.enums.StepType;

@Getter
@Setter
@Schema(description = "Event 빌드 모델, ")
public class PlEventResVO {
    private String resName;
    private String resType;
    private String resState;

    private String buildStepTitle;
    private StepType buildStepType;
    private StepState buildStepState;
}
