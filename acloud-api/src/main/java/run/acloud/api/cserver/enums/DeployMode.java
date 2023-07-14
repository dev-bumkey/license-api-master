package run.acloud.api.cserver.enums;

import run.acloud.commons.enums.EnumCode;


public enum DeployMode implements EnumCode {
	SYNC,
	ASYNC
	;

	public static class Names{
		public static final String SYNC = "SYNC";
		public static final String ASYNC = "ASYNC";
	}

	@Override
	public String getCode() {
		return this.name();
	}
}
