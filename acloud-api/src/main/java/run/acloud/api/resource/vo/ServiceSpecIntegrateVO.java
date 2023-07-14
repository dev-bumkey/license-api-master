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
		defaultImpl = ServiceSpecGuiVO.class,
		visible = true
)
@JsonSubTypes({
		@JsonSubTypes.Type(value = ServiceSpecGuiVO.class, name = DeployType.Names.GUI),
		@JsonSubTypes.Type(value = ServiceSpecYamlVO.class, name = DeployType.Names.YAML)
})
@Schema(name = "ServiceSpecIntegrateVO",
		title = "ServiceSpecIntegrateVO",
		description = "서비스 스펙 배포 유형별 통합 모델",
		discriminatorProperty = "deployType",
		discriminatorMapping = {
				@DiscriminatorMapping(value = DeployType.Names.GUI, schema = ServiceSpecGuiVO.class),
				@DiscriminatorMapping(value = DeployType.Names.YAML, schema = ServiceSpecYamlVO.class)
		},
		subTypes = {ServiceSpecGuiVO.class, ServiceSpecYamlVO.class}
)
public class ServiceSpecIntegrateVO {
	@Schema(title = "deployType", allowableValues = {"GUI","YAML"}, requiredMode = Schema.RequiredMode.REQUIRED)
	private String deployType = "GUI";
}
