package run.acloud.api.build.enums;

import run.acloud.commons.enums.EnumCode;

public enum RunType implements EnumCode {
	BUILD,
	CANCEL,
	REMOVE;

	@Override
	public String getCode() {
		return this.name();
	}
}
