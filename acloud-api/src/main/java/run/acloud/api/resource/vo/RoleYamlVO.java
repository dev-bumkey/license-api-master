package run.acloud.api.resource.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.NotBlank;

/**
 * Created by dy79@acornsoft.io on 2017. 1. 19.
 */
@Getter
@Setter
@Schema(name = "RoleYamlVO", title = "RoleYamlVO", description = "Role Yaml 모델", allOf = {RoleIntegrateVO.class})
public class RoleYamlVO extends RoleIntegrateVO {

	@Schema(title = "Role 명")
	@NotBlank
	private String name;

	@Schema(title = "배포 yaml 문자열")
	@NotBlank
	private String yaml;

}
