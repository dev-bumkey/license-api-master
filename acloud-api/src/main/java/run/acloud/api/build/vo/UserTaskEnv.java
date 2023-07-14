package run.acloud.api.build.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Build Environment 모델")
public class UserTaskEnv {

    @Schema(description = "환경변수 Key", required = false)
    private String key;

    @Schema(description = "환경변수 Value", required = false)
    private String value;
}
