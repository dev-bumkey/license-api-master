package run.acloud.api.resource.vo;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.NotBlank;

/**
 * Created by dy79@acornsoft.io on 2017. 1. 19.
 */
@Getter
@Setter
@JsonTypeInfo(
	use = JsonTypeInfo.Id.NAME,
	include = JsonTypeInfo.As.PROPERTY,
	property = "deployType",
	visible = true
)
@Schema(name = "ClusterRoleYamlVO", title = "ClusterRoleYamlVO", allOf = {ClusterRoleIntegrateVO.class}, description = "ClusterRole Yaml 배포 모델")
public class ClusterRoleYamlVO extends ClusterRoleIntegrateVO {

	@Schema(title = "ClusterRole 명")
	@NotBlank
	private String name;

	@Schema(title = "배포 yaml 문자열")
	@NotBlank
	private String yaml;

}
