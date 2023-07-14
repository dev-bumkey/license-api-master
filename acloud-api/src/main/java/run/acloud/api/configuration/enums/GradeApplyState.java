package run.acloud.api.configuration.enums;

import run.acloud.commons.enums.EnumCode;

public enum GradeApplyState implements EnumCode {
	APPLY,
	UNPAID,
	DELETE;

	@Override
	public String getCode() {
		return this.name();
	}
}
