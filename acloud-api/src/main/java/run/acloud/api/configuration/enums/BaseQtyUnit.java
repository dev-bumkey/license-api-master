package run.acloud.api.configuration.enums;

import run.acloud.commons.enums.EnumCode;

public enum BaseQtyUnit implements EnumCode {
	EA,
	SYSTEM,
	NODE,
	CORE;

	@Override
	public String getCode() {
		return this.name();
	}
}
