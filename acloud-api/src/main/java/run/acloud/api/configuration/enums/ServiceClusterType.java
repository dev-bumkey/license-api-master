package run.acloud.api.configuration.enums;

import run.acloud.commons.enums.EnumCode;

public enum ServiceClusterType implements EnumCode {
	QUOTA,
	CLUSTER;

	@Override
	public String getCode() {
		return this.name();
	}
}
