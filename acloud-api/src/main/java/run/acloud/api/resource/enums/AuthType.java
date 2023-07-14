package run.acloud.api.resource.enums;

import run.acloud.commons.enums.EnumCode;

public enum AuthType implements EnumCode {
	TOKEN, CERT, PLAIN;

	public static AuthType codeOf(String name) {
		return AuthType.valueOf(name);
	}

	@Override
	public String getCode() {
		return this.name();
	}
}
