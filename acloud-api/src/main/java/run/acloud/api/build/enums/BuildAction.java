package run.acloud.api.build.enums;

import run.acloud.commons.enums.EnumCode;

public enum BuildAction implements EnumCode {
    VIEW, BUILD, LOG, REMOVE, CANCEL;

	@Override
	public String getCode() {
		return this.name();
	}
}
