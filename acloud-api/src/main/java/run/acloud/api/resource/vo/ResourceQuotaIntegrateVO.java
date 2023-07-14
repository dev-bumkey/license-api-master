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
		defaultImpl = ResourceQuotaGuiVO.class,
		visible = true
)
@JsonSubTypes({
		@JsonSubTypes.Type(value = ResourceQuotaGuiVO.class, name = DeployType.Names.GUI),
		@JsonSubTypes.Type(value = ResourceQuotaYamlVO.class, name = DeployType.Names.YAML)
})
@Schema(name = "ResourceQuotaIntegrateVO",
		title = "ResourceQuotaIntegrateVO",
		description = "ResourceQuota 스펙 배포 유형별 통합 모델",
		discriminatorProperty = "deployType",
		discriminatorMapping = {
				@DiscriminatorMapping(value = DeployType.Names.GUI, schema = ResourceQuotaGuiVO.class),
				@DiscriminatorMapping(value = DeployType.Names.YAML, schema = ResourceQuotaYamlVO.class)
		},
		subTypes = {ResourceQuotaGuiVO.class, ResourceQuotaYamlVO.class}
)
public class ResourceQuotaIntegrateVO {

	@Schema(title = "deployType", allowableValues = {"GUI","YAML"}, requiredMode = Schema.RequiredMode.REQUIRED)
	private String deployType = "GUI";
}