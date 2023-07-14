package run.acloud.api.resource.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.hibernate.validator.constraints.NotBlank;

@Data
public class K8sCRDYamlVO {

	@Schema(title = "배포 YAML 문자열")
	@NotBlank
	private String yaml;

}
