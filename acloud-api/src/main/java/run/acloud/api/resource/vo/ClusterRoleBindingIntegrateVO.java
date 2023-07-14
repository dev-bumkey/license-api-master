package run.acloud.api.resource.vo;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import run.acloud.api.cserver.enums.DeployType;

/**
 * @author: hjchoi@acornsoft.io
 * Created on 2017. 4. 17.
 */
@Getter
@Setter
@JsonTypeInfo(
		use = JsonTypeInfo.Id.NAME,
		include = JsonTypeInfo.As.PROPERTY,
		property = "deployType",
		defaultImpl = ClusterRoleBindingGuiVO.class,
		visible = true
)
@JsonSubTypes({
		@JsonSubTypes.Type(value = ClusterRoleBindingGuiVO.class, name = DeployType.Names.GUI),
		@JsonSubTypes.Type(value = ClusterRoleBindingYamlVO.class, name = DeployType.Names.YAML)
})
@Schema(name = "ClusterRoleBindingIntegrateVO",
		title = "ClusterRoleBindingIntegrateVO",
		description = "클러스터 롤바인딩 스펙 배포 유형별 통합 모델",
		discriminatorProperty = "deployType",
		discriminatorMapping = {
				@DiscriminatorMapping(value = DeployType.Names.GUI, schema = ClusterRoleBindingGuiVO.class),
				@DiscriminatorMapping(value = DeployType.Names.YAML, schema = ClusterRoleBindingYamlVO.class)
		},
		subTypes = {ClusterRoleBindingGuiVO.class, ClusterRoleBindingYamlVO.class}
)
public class ClusterRoleBindingIntegrateVO {
	@Schema(title = "deployType", allowableValues = {"GUI","YAML"}, requiredMode = Schema.RequiredMode.REQUIRED)
	private String deployType = "GUI";
}