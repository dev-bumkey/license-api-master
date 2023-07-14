package run.acloud.api.build.enums;

import run.acloud.commons.enums.EnumCode;

public enum RepositoryType implements EnumCode {
	GIT;
	
	@Override
	public String getCode() {
		return this.name();
	}
	
	public static RepositoryType codeOf(String code) {
		return RepositoryType.valueOf(code);
	}
}
