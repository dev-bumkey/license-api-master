package run.acloud.api.catalog.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@Schema(title = "Helm Release Info", description = "Helm Release Info Model")
public class HelmReleaseInfoVO implements Serializable {
    private static final long serialVersionUID = -4286213986977293543L;

    @Schema(title = "revision")
    @JsonProperty("revision")
    private String revision;

    @Schema(title = "updated")
    @JsonProperty("updated")
    private String updated;

    @Schema(title = "status")
    @JsonProperty("status")
    private String status;

    @Schema(title = "chart")
    @JsonProperty("chart")
    private String chart;

    @Schema(title = "appVersion")
    private String appVersion;

    @Schema(title = "description")
    @JsonProperty("description")
    private String description;
}