package run.acloud.api.configuration.enums;

import lombok.Getter;
import run.acloud.commons.enums.EnumCode;

public enum GateWayNameType implements EnumCode {
	INGRESS("Ingress", "Y"),
	NODE_PORT("Node Port", "Y"),
	LOAD_BALANCER("Load Balancer", "Y"),
	CLUSTER_IP("Cluster Ip", "Y")
	;

	@Getter
	private String type;
	@Getter
	private String useYn;

	GateWayNameType(String type, String useYn) {
		this.type = type;
		this.useYn = useYn;
	}

	@Override
	public String getCode() {
		return this.name();
	}
}
