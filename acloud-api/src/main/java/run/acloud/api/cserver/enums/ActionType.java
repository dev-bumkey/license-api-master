package run.acloud.api.cserver.enums;

import run.acloud.commons.enums.EnumCode;

public enum ActionType implements EnumCode {
	ADD_CSERVER, STOP_CSERVER, START_CSERVER, REMOVE_CSERVER, DEPLOY_CSERVER, FETCH_CSERVER,
	REMOVE_ALL_CONTAINERS, REMOVE_ALL_IMAGES, CREATE_CONTAINERS, ADD_STATEFULSET, STOP_STATEFULSET, START_STATEFULSET,
    REMOVE_STATEFULSET, ADD_PACKAGE, STOP_PACKAGE, START_PACKAGE, REMOVE_PACKAGE, DEPLOY_PACKAGE, FETCH_PACKAGE
	;

	@Override
	public String getCode() {
		return this.name();
	}
	
	public static ActionType codeOf(String code) {
		return ActionType.valueOf(code);
	}
}
