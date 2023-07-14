package run.acloud.api.configuration.enums;

import run.acloud.commons.enums.EnumCode;

public enum ServiceRegistryType implements EnumCode {
	SERVICE,
	SHARE;

	@Override
	public String getCode() {
		return this.name();
	}
}
