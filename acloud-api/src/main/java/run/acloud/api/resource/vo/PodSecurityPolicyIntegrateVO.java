package run.acloud.api.resource.vo;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
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
		defaultImpl = PodSecurityPolicyGuiVO.class,
		visible = true
)
@JsonSubTypes({
		@JsonSubTypes.Type(value = PodSecurityPolicyGuiVO.class, name = DeployType.Names.GUI),
		@JsonSubTypes.Type(value = PodSecurityPolicyYamlVO.class, name = DeployType.Names.YAML)
})
@Schema(title = "PodSecurityPolicyIntegrateVO",
		description = "PodSecurityPolicy 통합 모델",
		discriminatorProperty = "deployType",
		discriminatorMapping = {
				@DiscriminatorMapping(value = DeployType.Names.GUI, schema = PodSecurityPolicyGuiVO.class),
				@DiscriminatorMapping(value = DeployType.Names.YAML, schema = PodSecurityPolicyYamlVO.class)
		},
		subTypes = {PodSecurityPolicyGuiVO.class, PodSecurityPolicyYamlVO.class}
)
@EqualsAndHashCode
public class PodSecurityPolicyIntegrateVO {
	@Schema(title = "deployType", allowableValues = {"GUI","YAML"}, requiredMode = Schema.RequiredMode.REQUIRED)
	private String deployType;
}
