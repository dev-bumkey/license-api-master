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
public class ServicePortVO extends BaseVO {

    @Schema(title = "name")
    private String name;

    @Schema(title = "nodePort")
    private Integer nodePort;

    @Schema(title = "port")
    private Integer port;

    @Schema(allowableValues = {"TCP","UDP","SCTP"}, title = "protocol")
    private String protocol;

    @Schema(title = "targetPort")
    private String targetPort;

}
