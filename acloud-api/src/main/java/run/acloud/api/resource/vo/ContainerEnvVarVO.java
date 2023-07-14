package run.acloud.api.resource.vo;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import run.acloud.commons.vo.BaseVO;

@Getter
@Setter
public class ContainerEnvVarVO extends BaseVO {
	
	private Integer containerEnvVarSeq;
	
	private Integer containerSeq;
	
	@Size(min = 1)
	private String key;
	
	private String value;

	private String configMapName;

	private String configMapKey;

	private String secretName;

	private String secretKey;

	private K8sObjectFieldSelectorVO fieldRef;

	private K8sResourceFieldSelectorVO resourceFieldRef;

}
