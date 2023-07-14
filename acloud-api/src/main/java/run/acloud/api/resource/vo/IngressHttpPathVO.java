package run.acloud.api.resource.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import run.acloud.commons.vo.BaseVO;

/**
 * @author: wschoi@acornsoft.io
 * Created on 2017. 10. 12.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class IngressHttpPathVO extends BaseVO {

    @Schema(title = "path")
    private String path;

    @Schema(title = "pathType")
    private String pathType;

    @Schema(title = "serviceName")
    private String serviceName;

    @Schema(title = "servicePortIsInteger", description = "servicePort가 Integer 인지 String 인지 여부")
    private boolean servicePortIsInteger = true;

    @Schema(title = "servicePort")
    private String servicePort;
}
