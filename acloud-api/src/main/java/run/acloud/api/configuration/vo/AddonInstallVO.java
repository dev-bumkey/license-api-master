package run.acloud.api.configuration.vo;

import io.swagger.v3.oas.annotations.media.Schema;
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
public class AddonInstallVO extends ChartInfoBaseVO {
    private static final long serialVersionUID = -608956543985445334L;

    @Schema(title = "releaseName")
    private String releaseName;
}