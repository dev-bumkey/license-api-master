package run.acloud.api.catalog.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(title = "ChartModel", description = "Chart Data Model")
@Deprecated // old chart model...
public class ChartVO {

@Schema(title = "ChartName", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(title = "ChartVersion", requiredMode = Schema.RequiredMode.REQUIRED)
    private String version;

    @Schema(title = "ChartDataFormat", requiredMode = Schema.RequiredMode.REQUIRED)
    private String dataFormat;

    @Schema(title = "ChartData", requiredMode = Schema.RequiredMode.REQUIRED)
    private String data;
}
