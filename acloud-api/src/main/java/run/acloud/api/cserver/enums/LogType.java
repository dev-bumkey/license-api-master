package run.acloud.api.cserver.enums;

import run.acloud.commons.enums.EnumCode;

public enum LogType implements EnumCode {
	START, RUNNING, FINISHED, ERROR
	;

	@Override
	public String getCode() {
		return this.name();
	}
	
	public static LogType codeOf(String code) {
		return LogType.valueOf(code);
	}
}
