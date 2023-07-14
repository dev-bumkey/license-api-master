package run.acloud.api.configuration.enums;

import run.acloud.commons.enums.EnumCode;

public enum BaseTerm implements EnumCode {
	YEAR,
	MONTH,
	DATE,
	HOUR;

	@Override
	public String getCode() {
		return this.name();
	}
}
