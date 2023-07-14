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
@Schema(name = "ClusterRoleBindingYamlVO",
		title = "ClusterRoleBindingYamlVO",
		description = "Cluster RoleBinding YAML 모델",
		implementation = ClusterRoleBindingIntegrateVO.class
)
public class ClusterRoleBindingYamlVO extends ClusterRoleBindingIntegrateVO {

	@Schema(title = "ClusterRoleBinding 명")
	@NotBlank
	private String name;

	@Schema(title = "배포 yaml 문자열")
	@NotBlank
	private String yaml;

}
