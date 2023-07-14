package run.acloud.api.build.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.NotBlank;

import java.io.Serializable;


@Getter
@Setter
@Schema(description = "도커 볼륨 마운트 모델")
public class UserTaskVolumeMount implements Serializable {

    @Schema(description = "컨테이너 경로", required = true)
    @NotBlank
    private String containerPath;

    @Schema(description = "호스트 경로", required = true)
    @NotBlank
    private String hostPath;

}
