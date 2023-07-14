package run.acloud.api.cserver.enums;

import run.acloud.commons.enums.EnumCode;

public enum DeployAction implements EnumCode {
	CREATE, EDIT, TERMINATE, RECREATE, REMOVE, RESTORE;

	@Override
	public String getCode() {
		return this.name();
	}
}
