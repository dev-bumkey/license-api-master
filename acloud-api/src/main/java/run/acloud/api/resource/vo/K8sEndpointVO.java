package run.acloud.api.resource.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import run.acloud.commons.vo.BaseVO;

import java.util.List;
import java.util.Map;

/**
 * @author: wschoi@acornsoft.io
 * Created on 2017. 10. 12.
 */
@Getter
@Setter
public class K8sEndpointVO extends BaseVO{

    @Schema(title = "host")
    private String host;

    @Schema(title = "ports")
    private List<Map<String, String>> ports;

    @Schema(title = "노드명")
    private String nodeName;

    @Schema(title = "준비")
    private boolean ready;

}
