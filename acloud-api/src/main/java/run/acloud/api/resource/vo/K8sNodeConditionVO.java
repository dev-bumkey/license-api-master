package run.acloud.api.resource.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import run.acloud.commons.vo.BaseVO;

import java.time.OffsetDateTime;

/**
 * @author: wschoi@acornsoft.io
 * Created on 2017. 10. 12.
 */
@Getter
@Setter
public class K8sNodeConditionVO extends BaseVO{

    @Schema(title = "마지막 측정 시간")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private OffsetDateTime lastHeartbeatTime = null;

    @Schema(title = "마지막 전송 시간")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private OffsetDateTime lastTransitionTime = null;

    @Schema(title = "메세지")
    private String message = null;

    @Schema(title = "이유")
    private String reason = null;

    @Schema(title = "상태")
    private String status = null;

    @Schema(title = "타입")
    private String type = null;
}
