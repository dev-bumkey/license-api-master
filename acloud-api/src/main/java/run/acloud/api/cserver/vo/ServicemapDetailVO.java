package run.acloud.api.cserver.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import run.acloud.api.resource.vo.ComponentVO;
import run.acloud.api.resource.vo.K8sNamespaceVO;

import java.util.List;
import java.util.Map;

/**
 * @author dy79@acornsoft.io on 2017. 1. 17.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Schema(title = "서비스맵 상세 모델")
public class ServicemapDetailVO extends ServicemapVO {

	@Schema(name = "labels", description = "k8s namespace labels")
	private Map<String, String> labels;
	@Schema(name = "annotations", description = "k8s namespace annotations")
	private Map<String, String> annotations;
	@Schema(name = "k8sNamespace", description = "k8s namespace 모델")
	private K8sNamespaceVO k8sNamespace;

	@Schema(name ="k8sResourceExists", description = "k8s 리소스 존재여부")
	private Boolean k8sResourceExists;

	@JsonIgnore
	@Schema(name = "components", example = "[]")
	private List<ComponentVO> components;


}