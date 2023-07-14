package run.acloud.api.resource.vo;

import lombok.Getter;
import lombok.Setter;
import run.acloud.commons.vo.BaseVO;

import java.io.Serializable;

@Getter
@Setter
public class ContainerResourcesVO extends BaseVO implements Serializable {
	// gpu 사용여부, 체크박스
	private Boolean useGpu = false;

	private ResourceVO requests;
	
	private ResourceVO limits;
}
