package run.acloud.api.resource.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * @author wschoi@acornsoft.io
 * Created on 2017. 7. 5.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@Schema(description = "컨테이너에서 volume의 사용 설정")
public class VolumeMountVO implements Serializable {
    @Schema(title = "사용할 Volume 이름", description = "spec.container.volumeMount.name에 사용", requiredMode = Schema.RequiredMode.REQUIRED)
    private String volumeName;

    @Schema(title = "컨테이너의 volume mount 경로", requiredMode = Schema.RequiredMode.REQUIRED)
    private String containerPath;

    @Schema(title = "Persistent Volume을 사용하는 경우, 하위 경로 지정")
    private String subPath;

    @Schema(title = "사용할 Volume 이름", description = "기본값은 'N'")
    private String readOnlyYn = "N";

    @Schema(title = "mountPropagation", allowableValues = {"None","HostToContainer","Bidirectional"})
    private String mountPropagation;

    @Schema(title = "subPathExpr")
    private String subPathExpr;

    // 사용자가 서버를 생성할 때 지정한 볼륨의 subPath. catalog에서 사용
    private String userSubPath;
}
