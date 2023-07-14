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
public class FSGroupStrategyOptionsVO {
    @Schema(title = "ranges", description = "ranges are the allowed ranges of fs groups.  If you would like to force a single fs group then supply a single range with the same start and end. Required for MustRunAs.")
    private List<IDRangeVO> ranges = null;

    @Schema(title = "rule", description = "rule is the strategy that will dictate what FSGroup is used in the SecurityContext.")
    private String rule;

    public FSGroupStrategyOptionsVO() {
    }

    public FSGroupStrategyOptionsVO(String rule) {
        this.rule = rule;
    }
}
