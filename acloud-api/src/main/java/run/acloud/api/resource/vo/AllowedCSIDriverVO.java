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
public class AllowedCSIDriverVO {
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, title = "name", description = "Name is the registered name of the CSI driver")
    private String name;
}
