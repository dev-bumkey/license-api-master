package run.acloud.api.resource.enums;

import run.acloud.commons.enums.EnumCode;


public enum TargetPortType implements EnumCode {
	NAME,
	NUMBER
	;

	public static class Names{
		public static final String NAME = "NAME";
		public static final String NUMBER = "NUMBER";
	}

	@Override
	public String getCode() {
		return this.name();
	}
}
