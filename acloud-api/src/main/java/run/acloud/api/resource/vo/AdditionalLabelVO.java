package run.acloud.api.resource.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import run.acloud.commons.vo.BaseVO;

import java.util.Map;

/**
 * @author: wschoi@acornsoft.io
 * Created on 2017. 10. 12.
 */
@Getter
@Setter
@Schema(description = "Additional Label Response 객체")
public class AdditionalLabelVO extends BaseVO{
    @Schema(title = "Request Type")
    private String kind;

    @Schema(title = "Label List")
    private Map<String, String> label;
}
