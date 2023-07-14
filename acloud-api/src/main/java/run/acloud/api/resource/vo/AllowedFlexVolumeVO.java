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
public class AllowedFlexVolumeVO {
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, title = "driver", description = "driver is the name of the Flexvolume driver.")
    private String driver;
}
