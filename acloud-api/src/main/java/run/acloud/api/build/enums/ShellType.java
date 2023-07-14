package run.acloud.api.build.enums;

import run.acloud.commons.enums.EnumCode;

public enum ShellType implements EnumCode {
	COMMAND, SCRIPT;

	public static class Names{
		public static final String COMMAND = "COMMAND";
		public static final String SCRIPT = "SCRIPT";
	}

	@Override
	public String getCode() {
		return this.name();
	}
	
	public static ShellType codeOf(String code) {
		return ShellType.valueOf(code);
	}
}
