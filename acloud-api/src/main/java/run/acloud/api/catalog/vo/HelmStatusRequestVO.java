package run.acloud.api.catalog.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(title = "Helm Status Request", description = "Helm Status Request Model")
public class HelmStatusRequestVO extends HelmRequestBaseVO {
    private static final long serialVersionUID = -6673569474469952785L;

    @Schema(title = "releaseName")
    @JsonProperty("releaseName")
    private String releaseName;

    @Schema(title = "revision")
    @JsonProperty("revision")
    private String revision;
}
