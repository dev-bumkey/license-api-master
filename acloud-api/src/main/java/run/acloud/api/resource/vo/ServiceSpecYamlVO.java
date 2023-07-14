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
@Schema(name = "ServiceSpecYamlVO",
		title = "ServiceSpecYamlVO",
		description = "서비스 스펙 배포 Yaml 모델",
		allOf = {ServiceSpecIntegrateVO.class}
)
public class ServiceSpecYamlVO extends ServiceSpecIntegrateVO implements Serializable {
	private static final long serialVersionUID = -4110496901682356818L;

	@Schema(title = "servicemapSeq")
	private Integer servicemapSeq;

	@Schema(title = "네임스페이스명")
	private String namespaceName;

//	@Schema(title = "service 유형", allowableValues = {"CLUSTER_IP","NODE_PORT","LOADBALANCER","HEADLESS"})
//	private String serviceType;
//
	@Schema(title = "Service 명")
	private String name;

	@Schema(title = "배포 yaml 문자열")
	@NotBlank
	private String yaml;

}
