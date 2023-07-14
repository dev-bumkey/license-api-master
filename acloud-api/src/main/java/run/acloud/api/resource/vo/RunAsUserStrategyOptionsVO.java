package run.acloud.api.resource.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * @author: wschoi@acornsoft.io
 * Created on 2017. 10. 12.
 */
@Getter
@Setter
@EqualsAndHashCode
public class RunAsUserStrategyOptionsVO {
    @Schema(title = "ranges", description = "ranges are the allowed ranges of uids that may be used. If you would like to force a single uid then supply a single range with the same start and end. Required for MustRunAs.")
    private List<IDRangeVO> ranges = null;

    @Schema(title = "rule", description = "rule is the strategy that will dictate the allowable RunAsUser values that may be set.")
    private String rule;

    public RunAsUserStrategyOptionsVO() {
    }

    public RunAsUserStrategyOptionsVO(String rule) {
        this.rule = rule;
    }
}
