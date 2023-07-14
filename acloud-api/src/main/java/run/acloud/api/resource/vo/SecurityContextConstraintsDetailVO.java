package run.acloud.api.resource.vo;

import com.google.gson.annotations.SerializedName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * @author: coolingi@acornsoft.io
 * Created on 2022. 11. 09.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@Schema(description = "SecurityContextConstraintsDetailVO is scc detail infomation.")
public class SecurityContextConstraintsDetailVO extends SecurityContextConstraintsNameVO{

    @SerializedName("describeData")
    @Schema( name = "describeData", title = "describeData" )
    private String describeData;
}
