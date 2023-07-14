package run.acloud.api.cserver.vo;

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
		defaultImpl = ServerGuiVO.class,
		visible = true
)
@JsonSubTypes({
		@JsonSubTypes.Type(value = ServerGuiVO.class, name = DeployType.Names.GUI),
		@JsonSubTypes.Type(value = ServerYamlVO.class, name = DeployType.Names.YAML)
})
@Schema(name = "ServerIntegrateVO",
		title = "ServerIntegrateVO",
		description = "서버 배포 유형별 통합 모델",
		discriminatorProperty = "deployType",
		discriminatorMapping = {
				@DiscriminatorMapping(value = DeployType.Names.GUI, schema = ServerGuiVO.class),
				@DiscriminatorMapping(value = DeployType.Names.YAML, schema = ServerYamlVO.class)
		},
		subTypes = {ServerGuiVO.class, ServerYamlVO.class}
)
public class ServerIntegrateVO {

	@Schema(title = "deployType", allowableValues = {"GUI","YAML"}, requiredMode = Schema.RequiredMode.REQUIRED)
	private String deployType = "GUI";

}
