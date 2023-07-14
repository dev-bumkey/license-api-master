package run.acloud.api.build.enums;

import run.acloud.commons.enums.EnumCode;

public enum BuildEventAction implements EnumCode {
	
	BUILD_STATE, BUILD_RUN_STATE;

	@Override
	public String getCode() {
		return this.name();
	}

	public static BuildEventAction codeOf(String code) {
		return BuildEventAction.valueOf(code);
	}
}
