package run.acloud.api.resource.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import run.acloud.commons.vo.BaseVO;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@Schema(description = "볼륨에서 key, path mapping")
public class ContainerVolumeKeyToPathVO extends BaseVO {

    @Schema(title = "Optional: mode bits to use on this file, must be a value between 0 and 0777. If not specified, the volume defaultMode will be used. This might be in conflict with other options that affect the file mode, like fsGroup, and the result can be other mode bits set.")
    private Integer mode;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, title = "The key to project.")
    private String key;

    @Schema(name = "path", requiredMode = Schema.RequiredMode.REQUIRED, title = "The relative path of the file to map the key to. May not be an absolute path. May not contain the path element '..'. May not start with the string '..'.")
    private String path;

}
