package run.acloud.api.resource.vo;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.NotBlank;

import java.io.Serializable;

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
@Schema(name = "SecretYamlVO",
		title = "SecretYamlVO",
		description = "Secret YAML 모델",
		allOf = {SecretIntegrateVO.class}
)
public class SecretYamlVO extends SecretIntegrateVO implements Serializable {
	private static final long serialVersionUID = 7150994310792494465L;

	@Schema(title = "namespace")
	private String namespace;

	@Schema(title = "Secret 명")
	private String name;

//  YAML TYPE에서 설명은 Annotation의 설명과 중복되어 문제 발생 소지가 있음..
//	@Schema(title = "Secret 설명")
//	private String description;

	@Schema(title = "배포 yaml 문자열")
	@NotBlank
	private String yaml;

}
