package run.acloud.api.configuration.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import run.acloud.api.catalog.vo.ChartInfoBaseVO;

@Getter
@Setter
@ToString
@EqualsAndHashCode
@Schema(description = "Addon configMap Chart Repository 모델")
public class AddonInfoBaseVO extends ChartInfoBaseVO {
    private static final long serialVersionUID = 8505018958113272910L;

    @Schema(title = "installed", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    @JsonProperty("installed")
    private boolean installed = false;

    @Schema(title = "multipleInstallable")
    @JsonProperty("multipleInstallable")
    private boolean multipleInstallable = false;

    @Schema(title = "currentInstallation")
    @JsonProperty("currentInstallation")
    private Integer currentInstallation;

    @Schema(title = "maxInstallation")
    @JsonProperty("maxInstallation")
    private Integer maxInstallation;
}