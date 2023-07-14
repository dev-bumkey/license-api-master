package run.acloud.api.resource.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import run.acloud.commons.vo.BaseVO;

@Getter
@Setter
public class ServerUrlVO extends BaseVO {

    @Schema(title = "urlßßßßß")
    private String url;

    @Schema(title = "port 명")
    private String alias;

    @Schema(title = "service 유형", allowableValues = {"CLUSTER_IP","NODE_PORT","LOADBALANCER","HEADLESS"})
    private String serviceType;
}
