package run.acloud.api.resource.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
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
@Schema(description = "SecurityContextConstraints is a resource for openshift archetecture.")
public class SecurityContextConstraintsVO extends SecurityContextConstraintsNameVO{

    @SerializedName("priv")
    @JsonProperty("priv")
    @Schema( name = "priv", title = "Privileged" )
    private String priv;

    @SerializedName("caps")
    @JsonProperty("caps")
    @Schema( name = "caps", title = "Capabilities" )
    private String caps;

    @SerializedName("selinux")
    @JsonProperty("selinux")
    @Schema( name = "selinux", title = "SELinux Context Strategy" )
    private String selinux;

    @SerializedName("runasuser")
    @JsonProperty("runasuser")
    @Schema( name = "runasuser", title = "Run as User" )
    private String runasuser;

    @SerializedName("fsgroup")
    @JsonProperty("fsgroup")
    @Schema( name = "fsgroup", title = "FSGroup Strategy" )
    private String fsgroup;

    @SerializedName("supgroup")
    @JsonProperty("supgroup")
    @Schema( name = "supgroup", title = "Supplemental Groups Strategy" )
    private String supgroup;

    @SerializedName("priority")
    @JsonProperty("priority")
    @Schema( name = "priority", title = "Priority" )
    private String priority;

    @SerializedName("readonlyrootfs")
    @JsonProperty("readonlyrootfs")
    @Schema( name = "readonlyrootfs", title = "Readonly root FS" )
    private boolean readonlyrootfs;

    @SerializedName("volumes")
    @JsonProperty("volumes")
    @Schema( name = "volumes", title = "Volumes" )
    private String volumes;

}
