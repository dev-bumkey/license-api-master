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
public class K8sDeployYamlVO extends BaseVO{

    @Schema(title = "namespace")
    private String namespace;

    @Schema(title = "이름")
    private String name;

    @Schema(title = "yaml")
    private String yaml;

}
