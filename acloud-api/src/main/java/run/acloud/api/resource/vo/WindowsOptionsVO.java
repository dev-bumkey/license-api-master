package run.acloud.api.resource.vo;

import com.google.gson.annotations.SerializedName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import run.acloud.commons.vo.BaseVO;

import java.io.Serializable;

@Getter
@Setter
@Schema(description = "container > securityContext > windowsOptions model")
public class WindowsOptionsVO extends BaseVO implements Serializable {

	@Schema(name = "gmsaCredentialSpec", description = "GMSACredentialSpec is where the GMSA admission webhook (https://github.com/kubernetes-sigs/windows-gmsa) inlines the contents of the GMSA credential spec named by the GMSACredentialSpecName field. This field is alpha-level and is only honored by servers that enable the WindowsGMSA feature flag.")
	@SerializedName("gmsaCredentialSpec")
	private String gmsaCredentialSpec;

	@Schema(name = "gmsaCredentialSpecName", description = "GMSACredentialSpecName is the name of the GMSA credential spec to use. This field is alpha-level and is only honored by servers that enable the WindowsGMSA feature flag.")
	@SerializedName("gmsaCredentialSpecName")
	private String gmsaCredentialSpecName;

	@Schema(name = "runAsUserName", description = "The UserName in Windows to run the entrypoint of the container process. Defaults to the user specified in image metadata if unspecified. May also be set in PodSecurityContext. If set in both SecurityContext and PodSecurityContext, the value specified in SecurityContext takes precedence. This field is alpha-level and it is only honored by servers that enable the WindowsRunAsUserName feature flag.")
	@SerializedName("runAsUserName")
	private String runAsUserName;
}
