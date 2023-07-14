package run.acloud.api.configuration.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import run.acloud.api.resource.vo.K8sIngressVO;
import run.acloud.api.resource.vo.K8sServiceVO;
import run.acloud.commons.vo.HasUseYnVO;

import java.util.List;

@Getter
@Setter
@Schema(description = "클러스터 addon service 모델")
public class ClusterAddonServiceVO extends HasUseYnVO {

	@Schema(title = "Service 목록")
	private List<K8sServiceVO> services;

	@Schema(title = "Ingress 목록")
	private List<K8sIngressVO> ingresses;

}

