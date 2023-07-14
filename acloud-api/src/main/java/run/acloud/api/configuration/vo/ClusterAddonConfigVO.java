package run.acloud.api.configuration.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import run.acloud.api.resource.vo.ConfigMapGuiVO;
import run.acloud.api.resource.vo.SecretGuiVO;
import run.acloud.commons.vo.HasUseYnVO;

import java.util.List;

@Getter
@Setter
@Schema(description = "클러스터 addon config 모델")
public class ClusterAddonConfigVO extends HasUseYnVO {

	@Schema(title = "ConfigMap 목록")
	private List<ConfigMapGuiVO> configMaps;

	@Schema(title = "Secret 목록")
	private List<SecretGuiVO> secrets;

}

