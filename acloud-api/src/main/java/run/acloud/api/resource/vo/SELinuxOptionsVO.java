package run.acloud.api.resource.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import run.acloud.commons.vo.BaseVO;

import java.io.Serializable;

@Getter
@Setter
@EqualsAndHashCode
public class SELinuxOptionsVO extends BaseVO implements Serializable {

	@Schema(title = "level", description = "Level is SELinux level label that applies to the container.")
	private String level;

	@Schema(title = "role", description = "Role is a SELinux role label that applies to the container.")
	private String role;

	@Schema(title = "type", description = "Type is a SELinux type label that applies to the container.")
	private String type;

	@Schema(title = "user", description = "User is a SELinux user label that applies to the container.")
	private String user;
}
