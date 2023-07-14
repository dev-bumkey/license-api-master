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
@Schema(name = "RoleBindingYamlVO", title = "RoleBindingYamlVO", allOf = {RoleBindingIntegrateVO.class}, description = "Yaml 배포 모델")
public class RoleBindingYamlVO extends RoleBindingIntegrateVO {

	@Schema(title = "RoleBinding 명")
	@NotBlank
	private String name;

	@Schema(title = "배포 yaml 문자열")
	@NotBlank
	private String yaml;

}
