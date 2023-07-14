package run.acloud.framework.enums;

import lombok.Getter;

public enum ExceptionSystem {

	COCKTAIL_API("C"),
	COCKTAIL_DB("D"),
	REGISTRY("R"),
	MONITORING_API("M"),
	METERING_API("A"),
	CLUSTER_API("T"),
	AWS_API("W"),
	K8S("K"),
	PACKAGE("H"),
	MESSAGING_SERVER("Q"),
	AUTH_SERVER("U"),
	GATEWAY_SERVER("G"),
	MAIL_SERVER("E")

	;

	@Getter
	private String systemCode;

	ExceptionSystem(String systemCode) {
		this.systemCode = systemCode;
	}

	public String getCode() {
		return this.name();
	}
}
