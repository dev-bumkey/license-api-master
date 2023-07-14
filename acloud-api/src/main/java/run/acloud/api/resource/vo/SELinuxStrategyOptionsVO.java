package run.acloud.api.resource.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * @author: wschoi@acornsoft.io
 * Created on 2017. 10. 12.
 */
@Getter
@Setter
@EqualsAndHashCode
public class SELinuxStrategyOptionsVO {
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, title = "allowedRuntimeClassNames", description = "rule is the strategy that will dictate the allowable labels that may be set.")
    private String rule;

    @Schema(title = "seLinuxOptions", description = "SELinuxOptions are the labels to be applied to the container")
    private SELinuxOptionsVO seLinuxOptions;

    public SELinuxStrategyOptionsVO() {
    }

    public SELinuxStrategyOptionsVO(String rule) {
        this.rule = rule;
    }
}
