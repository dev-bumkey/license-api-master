package run.acloud.api.build.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import run.acloud.api.build.enums.ShellType;

@Getter
@Setter
@Schema(name = "StepShellVO", title = "StepShellVO", allOf = {BuildStepAddVO.class}, description = "Build Shell Script 작업 모델")
public class StepShellVO extends BuildStepAddVO {

    @Schema(title = "shell type", allowableValues = {"COMMAND","SCRIPT"}, requiredMode = Schema.RequiredMode.REQUIRED)
    @Valid
    @NotNull
    private ShellType shellType;

    @Schema(title = "shell script commands", requiredMode = Schema.RequiredMode.REQUIRED)
    @Valid
    @NotNull
    private String command;

}
