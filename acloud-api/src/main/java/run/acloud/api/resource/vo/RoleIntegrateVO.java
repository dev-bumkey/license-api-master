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
		defaultImpl = RoleGuiVO.class,
		visible = true
)
@JsonSubTypes({
		@JsonSubTypes.Type(value = RoleGuiVO.class, name = DeployType.Names.GUI),
		@JsonSubTypes.Type(value = RoleYamlVO.class, name = DeployType.Names.YAML)
})
@Schema(title = "RoleIntegrateVO",
		description = "롤 스펙 배포 유형별 통합 모델",
		discriminatorProperty = "deployType",
		discriminatorMapping = {
				@DiscriminatorMapping(value = DeployType.Names.GUI, schema = RoleGuiVO.class),
				@DiscriminatorMapping(value = DeployType.Names.YAML, schema = RoleYamlVO.class)
		},
		subTypes = {RoleGuiVO.class, RoleYamlVO.class}
)
public class RoleIntegrateVO {
	@Schema(title = "deployType", allowableValues = {"GUI","YAML"}, requiredMode = Schema.RequiredMode.REQUIRED)
	private String deployType = "GUI";
}