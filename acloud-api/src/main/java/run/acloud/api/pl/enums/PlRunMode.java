package run.acloud.api.pl.enums;

import run.acloud.commons.enums.EnumCode;

public enum PlRunMode implements EnumCode {
	BUILD, DEPLOY;
	
	@Override
	public String getCode() {
		return this.name();
	}
	
	public static PlRunMode codeOf(String code) {
		return PlRunMode.valueOf(code);
	}
}
