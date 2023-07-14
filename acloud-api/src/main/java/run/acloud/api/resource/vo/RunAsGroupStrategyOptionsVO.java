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
public class RunAsGroupStrategyOptionsVO {
    @Schema(title = "ranges", description = "ranges are the allowed ranges of gids that may be used. If you would like to force a single gid then supply a single range with the same start and end. Required for MustRunAs.")
    private List<IDRangeVO> ranges = null;

    @Schema(title = "rule", description = "rule is the strategy that will dictate the allowable RunAsGroup values that may be set.")
    private String rule;

    public RunAsGroupStrategyOptionsVO(String rule) {
        this.rule = rule;
    }
}
