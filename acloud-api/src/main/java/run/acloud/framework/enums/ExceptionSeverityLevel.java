package run.acloud.framework.enums;

import lombok.Getter;

public enum ExceptionSeverityLevel {

	WARNING("1"),
	ERROR("2"),
	FATAL("3")

	;

	@Getter
	private String severityLevel;

	ExceptionSeverityLevel(String severityLevel) {
		this.severityLevel = severityLevel;
	}

	public String getCode() {
		return this.name();
	}
}
