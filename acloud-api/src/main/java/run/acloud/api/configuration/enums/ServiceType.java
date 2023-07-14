package run.acloud.api.configuration.enums;

import run.acloud.commons.enums.EnumCode;

public enum ServiceType implements EnumCode {
	NORMAL,
	PLATFORM;

	public static class Names{
		public static final String NORMAL = "NORMAL";
		public static final String PLATFORM = "PLATFORM";
	}

	@Override
	public String getCode() {
		return this.name();
	}
}
