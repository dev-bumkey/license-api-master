package run.acloud.api.resource.vo;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Map;

@Getter
@Setter
public class ResourceVO implements Serializable {
	
	private Double cpu;

	private Double memory;

	private Double gpu;

	private Map<String, String> network;
}
