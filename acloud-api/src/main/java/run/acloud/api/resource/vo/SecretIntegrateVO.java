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
		defaultImpl = SecretGuiVO.class,
		visible = true
)
@JsonSubTypes({
	@JsonSubTypes.Type(value = SecretGuiVO.class, name = DeployType.Names.GUI),
	@JsonSubTypes.Type(value = SecretYamlVO.class, name = DeployType.Names.YAML)
})
@Schema(title = "SecretIntegrateVO",
		description = "시크릿 배포 유형별 통합 모델",
		discriminatorProperty = "deployType",
		discriminatorMapping = {
				@DiscriminatorMapping(value = DeployType.Names.GUI, schema = SecretGuiVO.class),
				@DiscriminatorMapping(value = DeployType.Names.YAML, schema = SecretYamlVO.class)
		},
		subTypes = {SecretGuiVO.class, SecretYamlVO.class}
)
public class SecretIntegrateVO {
	@Schema(title = "deployType", allowableValues = {"GUI","YAML"}, requiredMode = Schema.RequiredMode.REQUIRED)
	private String deployType = "GUI";
}