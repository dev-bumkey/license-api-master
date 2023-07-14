package run.acloud.api.catalog.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(title = "Helm List Request", description = "Helm List Request Model")
public class HelmListRequestVO extends HelmRequestBaseVO {
    private static final long serialVersionUID = 3783446398700259896L;

    @Schema(title = "filter")
    @JsonProperty("filter")
    private String filter;
}
