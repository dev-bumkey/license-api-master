package run.acloud.api.resource.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import run.acloud.commons.vo.BaseVO;

/**
 * @author: coolingi@acornsoft.io
 * Created on 2022. 11. 09.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@Schema(description = "SecurityContextConstraintsName is only a name of SecurityContextConstraints for openshift archetecture.")
public class SecurityContextConstraintsNameVO extends BaseVO{
    @SerializedName("name")
    @JsonProperty("name")
    @Schema( name = "name", title = "scc name" )
    private String name;
}


