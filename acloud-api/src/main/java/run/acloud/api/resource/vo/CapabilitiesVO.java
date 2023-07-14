package run.acloud.api.resource.vo;

import lombok.Getter;
import lombok.Setter;
import run.acloud.commons.vo.BaseVO;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
public class CapabilitiesVO extends BaseVO implements Serializable {
	
	private List<String> drop;

	private List<String> add;
}
