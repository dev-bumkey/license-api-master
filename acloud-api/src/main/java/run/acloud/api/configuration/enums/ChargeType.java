package run.acloud.api.configuration.enums;

import run.acloud.commons.enums.EnumCode;

public enum ChargeType implements EnumCode {
	DEFAULT,
	ADD_CONCURRENT_BUILD,
	ADD_BUILD,
	ADD_WORKSPACE,
	CORE,
	NODE,
	ETC;

	@Override
	public String getCode() {
		return this.name();
	}
}
