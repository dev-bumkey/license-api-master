package run.acloud.api.resource.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.joda.time.DateTime;

import java.util.Map;

/**
 * @author: wschoi@acornsoft.io
 * Created on 2017. 10. 12.
 */
@Getter
@Setter
@Schema(name = "K8sCRDNetAttachDefGuiVO"
        , title = "K8sCRDNetAttachDefGuiVO"
        , allOf = {K8sCRDNetAttachDefIntegrateVO.class}
        , description = "GUI 배포 모델"
)
public class K8sCRDNetAttachDefGuiVO extends K8sCRDNetAttachDefIntegrateVO{

    @Schema(title = "name")
    private String name;

    @Schema(title = "namespace")
    private String namespace;

    @Schema(title = "생성시간")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private DateTime creationTimestamp;

    @Schema(title = "type")
    private String type;

    @Schema(title = "labels")
    private Map<String, String> labels;

    @Schema(title = "annotations")
    private Map<String, String> annotations;

    @Schema(title = "config")
    private String config;

    @Schema(title = "배포 정보")
    private String deployment;

    @Schema(title = "배포 정보 (yaml)")
    private String deploymentYaml;
}
