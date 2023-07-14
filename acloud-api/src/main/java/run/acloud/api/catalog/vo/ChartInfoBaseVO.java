package run.acloud.api.catalog.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@Schema(title = "Chart Request Base", description = "Chart Request Base Model")
public class ChartInfoBaseVO implements Serializable {
    private static final long serialVersionUID = -5071589825415367204L;

    @Schema(title = "name")
    private String name;

    @Schema(title = "version")
    private String version;

    @Schema(title = "appVersion")
    private String appVersion;

    @Schema(title = "created")
    private String created;

    @Schema(title = "updated")
    private String updated;

    @Schema(title = "icon")
    private String icon;

    @Schema(title = "home")
    private String home;

    @Schema(title = "digest")
    private String digest;

    @Schema(title = "readme")
    private String readme;

    @Schema(title = "values")
    private String values;

    @Schema(title = "description")
    private String description;

    @Schema(title = "repo")
    private String repo;

    @Schema(title = "addonToml")
    private String addonToml;

    @Schema(title = "configToml")
    private String configToml;

    @Schema(title = "defaultValues")
    private String defaultValueYaml;

    @Schema(title = "addonYaml")
    private String addonYaml;


}
