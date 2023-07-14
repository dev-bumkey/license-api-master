package run.acloud.api.resource.vo;

import com.google.gson.annotations.SerializedName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import run.acloud.commons.vo.BaseVO;

import java.io.Serializable;

@Getter
@Setter
@Schema(description = "container > securityContext model")
public class SecurityContextVO extends BaseVO implements Serializable {

	@Schema(name = "allowPrivilegeEscalation", description = "AllowPrivilegeEscalation controls whether a process can gain more privileges than its parent process. This bool directly controls if the no_new_privs flag will be set on the container process. AllowPrivilegeEscalation is true always when the container is: 1) run as Privileged 2) has CAP_SYS_ADMIN")
	@SerializedName("allowPrivilegeEscalation")
	private Boolean allowPrivilegeEscalation;

	@Schema(name = "capabilities", description = "Adds and removes POSIX capabilities from running containers.")
	@SerializedName("capabilities")
	private CapabilitiesVO capabilities;

	@Schema(name = "privileged", description = "Run container in privileged mode. Processes in privileged containers are essentially equivalent to root on the host. Defaults to false.")
	@SerializedName("privileged")
	private Boolean privileged;

	@Schema(name = "procMount", description = "procMount denotes the type of proc mount to use for the containers. The default is DefaultProcMount which uses the container runtime defaults for readonly paths and masked paths. This requires the ProcMountType feature flag to be enabled.")
	@SerializedName("procMount")
	private String procMount;

	@Schema(name = "readOnlyRootFilesystem", description = "Whether this container has a read-only root filesystem. Default is false.")
	@SerializedName("readOnlyRootFilesystem")
	private Boolean readOnlyRootFilesystem;

	@Schema(name = "runAsGroup", description = "The GID to run the entrypoint of the container process. Uses runtime default if unset. May also be set in PodSecurityContext.  If set in both SecurityContext and PodSecurityContext, the value specified in SecurityContext takes precedence.")
	@SerializedName("runAsGroup")
	private Long runAsGroup;

	@Schema(name = "runAsNonRoot", description = "Indicates that the container must run as a non-root user. If true, the Kubelet will validate the image at runtime to ensure that it does not run as UID 0 (root) and fail to start the container if it does. If unset or false, no such validation will be performed. May also be set in PodSecurityContext.  If set in both SecurityContext and PodSecurityContext, the value specified in SecurityContext takes precedence.")
	@SerializedName("runAsNonRoot")
	private Boolean runAsNonRoot;

	@Schema(name = "runAsUser", description = "The UID to run the entrypoint of the container process. Defaults to user specified in image metadata if unspecified. May also be set in PodSecurityContext.  If set in both SecurityContext and PodSecurityContext, the value specified in SecurityContext takes precedence.")
	@SerializedName("runAsUser")
	private Long runAsUser;

	@Schema(name = "seLinuxOptions", description = "SELinuxOptions are the labels to be applied to the container")
	@SerializedName("seLinuxOptions")
	private SELinuxOptionsVO seLinuxOptions;

	@Schema(name = "windowsOptions", description = "WindowsSecurityContextOptions contain Windows-specific options and credentials.")
	@SerializedName("windowsOptions")
	private WindowsOptionsVO windowsOptions;
}
