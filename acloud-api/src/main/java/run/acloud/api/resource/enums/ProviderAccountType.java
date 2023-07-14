package run.acloud.api.resource.enums;

import run.acloud.commons.enums.EnumCode;

public enum ProviderAccountType implements EnumCode {
	METERING, USER, ACCESS_KEY;
	
	public static ProviderAccountType codeOf(String code) {
		return ProviderAccountType.valueOf(code);
	}

	public String getCode() {
		return this.name();
	}
}
