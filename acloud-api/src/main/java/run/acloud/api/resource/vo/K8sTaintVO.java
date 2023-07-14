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
public class K8sTaintVO extends BaseVO{
    @Schema(title = "Effect")
    private String effect = null;

    @Schema(title = "Key")
    private String key = null;

    @Schema(title = "Value")
    private String value = null;
}
