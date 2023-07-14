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
		defaultImpl = K8sCRDCertificateGuiVO.class,
		visible = true
)
@JsonSubTypes({
		@JsonSubTypes.Type(value = K8sCRDCertificateGuiVO.class, name = DeployType.Names.GUI),
		@JsonSubTypes.Type(value = K8sCRDCertificateYamlVO.class, name = DeployType.Names.YAML)
})
@Schema(title = "K8sCRDIssuerIntegrateVO",
		description = "인증서 관리 인증서 배포 유형별 통합 모델",
		discriminatorProperty = "deployType",
		discriminatorMapping = {
				@DiscriminatorMapping(value = DeployType.Names.GUI, schema = K8sCRDCertificateGuiVO.class),
				@DiscriminatorMapping(value = DeployType.Names.YAML, schema = K8sCRDCertificateYamlVO.class)
		},
		subTypes = {K8sCRDCertificateGuiVO.class, K8sCRDCertificateYamlVO.class}
)
public class K8sCRDCertificateIntegrateVO {
	@Schema(title = "deployType", allowableValues = {"GUI","YAML"}, requiredMode = Schema.RequiredMode.REQUIRED)
	private String deployType;
}
