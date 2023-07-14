package run.acloud.api.resource.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import run.acloud.commons.vo.BaseVO;

/**
 * @author: wschoi@acornsoft.io
 * Created on 2017. 10. 12.
 */
@Getter
@Setter
public class K8sServicePortVO extends BaseVO {

    @Schema(title = "name")
    private String name;

    @Schema(title = "nodePort")
    private String nodePort;

    @Schema(title = "port")
    private String port;

    @Schema(title = "protocol")
    private String protocol;

    @Schema(title = "targetPort")
    private Object targetPort;

    @Schema(title = "serviceUrl")
    private String serviceUrl;
}
