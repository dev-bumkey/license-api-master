package run.acloud.api.build.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import run.acloud.api.build.enums.FtpExeType;
import run.acloud.api.build.enums.FtpType;

@Getter
@Setter
@Schema(name = "StepFtpVO", title = "StepFtpVO", allOf = {BuildStepAddVO.class}, description = "FTP 작업 모델")
public class StepFtpVO extends BuildStepAddVO {

    @Schema(title = "FTP 서버 url", requiredMode = Schema.RequiredMode.REQUIRED)
    @Valid
    @NotNull
    private String url;

    @Schema(title = "FTP 타입", requiredMode = Schema.RequiredMode.REQUIRED)
    @Valid
    @NotNull
    private FtpType ftpType;

    @Schema(title = "FTP 서버 사용자 ID", required = false)
    @Valid
    private String username;

    @Schema(title = "FTP 서버 사용자 Password", required = false)
    @Valid
    private String password;

    @Schema(title = "FTP 서버 사용자 Password", requiredMode = Schema.RequiredMode.REQUIRED)
    @Valid
    @NotNull
    private String sourceFiles;

    @Schema(title = "FTP 서버 사용자 Password", requiredMode = Schema.RequiredMode.REQUIRED)
    @Valid
    @NotNull
    private String remoteDirectory;

    @Schema(title = "FTP 실행 타입", requiredMode = Schema.RequiredMode.REQUIRED)
    @Valid
    @NotNull
    private FtpExeType ftpExecType;

}
