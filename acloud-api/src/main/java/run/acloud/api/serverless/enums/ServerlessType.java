package run.acloud.api.serverless.enums;

import run.acloud.commons.enums.EnumCode;


public enum ServerlessType implements EnumCode {
	BAAS,
	FAAS
	;

	public static class Names{
		public static final String BAAS = "BAAS";
		public static final String FAAS = "FAAS";
	}

	@Override
	public String getCode() {
		return this.name();
	}
}
