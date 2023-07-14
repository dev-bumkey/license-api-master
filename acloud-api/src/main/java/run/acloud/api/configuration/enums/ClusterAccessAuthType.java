package run.acloud.api.configuration.enums;

import run.acloud.commons.enums.EnumCode;

public enum ClusterAccessAuthType implements EnumCode {
	SECRET,
	ACCESS_KEY;

	@Override
	public String getCode() {
		return this.name();
	}
}
