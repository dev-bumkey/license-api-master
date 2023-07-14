package run.acloud.api.resource.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import run.acloud.commons.vo.BaseVO;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * @author: wschoi@acornsoft.io
 * Created on 2017. 10. 12.
 */
@Getter
@Setter
@Schema(description = "Container 모델")
public class K8sContainerVO extends BaseVO{

    @Schema(title = "Container 명")
    private String name;

    @Schema(title = "image")
    private String image;

    @Schema(title = "Environment variables")
    private Map<String, String> environmentVariables;

    @Schema(title = "commands")
    private List<String> commands;

    @Schema(title = "args")
    private List<String> args;

    @Schema(title = "resources")
    private Map<String, Map<String, String>> resources;

    @Schema(title = "imagePullPolicy")
    private String imagePullPolicy;

    @Schema(title = "volumeMounts")
    private Map<String, String> volumeMounts;


    @Schema(title = "containerID")
    private String containerID;

    @Schema(title = "imageID")
    private String imageID;

    @Schema(title = "restartCount")
    private int restartCount;

    @Schema(title = "ready")
    private Boolean ready;

    @Schema(title = "lastState")
    private String lastState;

    @Schema(title = "state")
    private String state;

    @Schema(title = "시작 시간")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private OffsetDateTime startTime;
}
