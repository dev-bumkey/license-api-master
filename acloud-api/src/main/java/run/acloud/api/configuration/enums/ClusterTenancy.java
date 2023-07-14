package run.acloud.api.configuration.enums;

import run.acloud.commons.enums.EnumCode;

public enum ClusterTenancy implements EnumCode {
	HARD,
	SOFT;

	public static class Names{
		public static final String HARD = "HARD";
		public static final String SOFT = "SOFT";
	}

	@Override
	public String getCode() {
		return this.name();
	}
}
