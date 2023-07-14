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
public class SupplementalGroupsStrategyOptionsVO {
    @Schema(title = "ranges", description = "ranges are the allowed ranges of supplemental groups.  If you would like to force a single supplemental group then supply a single range with the same start and end. Required for MustRunAs.")
    private List<IDRangeVO> ranges = null;

    @Schema(title = "rule", description = "rule is the strategy that will dictate what supplemental groups is used in the SecurityContext.")
    private String rule;

    public SupplementalGroupsStrategyOptionsVO() {
    }

    public SupplementalGroupsStrategyOptionsVO(String rule) {
        this.rule = rule;
    }
}
