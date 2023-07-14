package run.acloud.api.cserver.enums;

import run.acloud.commons.enums.EnumCode;


public enum DeployType implements EnumCode {
	GUI,
	YAML
	;

	public static class Names{
		public static final String GUI = "GUI";
		public static final String YAML = "YAML";
	}

	@Override
	public String getCode() {
		return this.name();
	}
}
