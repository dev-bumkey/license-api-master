package run.acloud.api.configuration.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import run.acloud.commons.vo.BaseVO;

@Getter
@Setter
@ToString
@EqualsAndHashCode
@Schema(description = "Addon configMap Chart Repository 모델")
public class AddonConfigMapChartRepoVO extends BaseVO {
    private static final long serialVersionUID = -7093759644278772782L;

    private String CHART_REPO_URL;
    private String CHART_REPO_PROJECT_NAME;
    private String CHART_REPO_USER;
    private String CHART_REPO_PASSWORD;

}