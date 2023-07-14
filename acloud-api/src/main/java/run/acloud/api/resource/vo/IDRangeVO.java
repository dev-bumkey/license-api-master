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
public class IDRangeVO {
    @Schema(title = "max", description = "max is the end of the range, inclusive.")
    private Long max;

    @Schema(title = "min", description = "min is the start of the range, inclusive.")
    private Long min;
}
