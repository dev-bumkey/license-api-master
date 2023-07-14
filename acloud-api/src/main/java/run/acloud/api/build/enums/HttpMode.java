package run.acloud.api.build.enums;

import run.acloud.commons.enums.EnumCode;

public enum HttpMode implements EnumCode {
	GET,
	HEAD,
	POST,
	PUT,
	DELETE,
	OPTIONS,
	PATCH;
	
	@Override
	public String getCode() {
		return this.name();
	}
	
	public static HttpMode codeOf(String code) {
		return HttpMode.valueOf(code);
	}
}
