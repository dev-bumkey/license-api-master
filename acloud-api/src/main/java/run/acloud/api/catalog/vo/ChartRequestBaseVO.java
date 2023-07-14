package run.acloud.api.catalog.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@Schema(title = "Chart Request Base", description = "Chart Request Base Model")
public class ChartRequestBaseVO implements Serializable {
    private static final long serialVersionUID = -3531538150314111984L;

    @Schema(title = "repository", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("repo")
    private String repo;

    @Schema(title = "chart name")
    @JsonProperty("name")
    private String name;

    @Schema(title = "chart version")
    @JsonProperty("version")
    private String version;

    @Schema(title = "search string")
    @JsonProperty("search")
    private String search;
}
