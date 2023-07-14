package run.acloud.api.cserver.enums;

import run.acloud.commons.enums.EnumCode;

public enum WorkloadVersion implements EnumCode {

	V1;

	@Override
	public String getCode() {
		return this.name();
	}
}
