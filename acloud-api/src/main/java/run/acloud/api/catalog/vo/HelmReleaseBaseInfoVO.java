package run.acloud.api.catalog.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@Schema(title = "Helm Release Base", description = "Helm Release Base Model")
public class HelmReleaseBaseInfoVO implements Serializable {
    private static final long serialVersionUID = 7714898996373034199L;

    @Schema(title = "firstDeployed")
    private String firstDeployed;

    @Schema(title = "lastDeployed")
    private String lastDeployed;

    @Schema(title = "deleted")
    private String deleted;

    @Schema(title = "description")
    private String description;

    @Schema(title = "status")
    private String status;

    @Schema(title = "icon")
    @JsonProperty("icon")
    private String icon;

    @Schema(title = "notes")
    @JsonProperty("notes")
    private String notes;

    @Schema(title = "readme")
    @JsonProperty("readme")
    private String readme;

}