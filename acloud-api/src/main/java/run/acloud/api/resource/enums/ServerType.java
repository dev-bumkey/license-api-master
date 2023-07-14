package run.acloud.api.resource.enums;

import run.acloud.commons.enums.EnumCode;

public enum ServerType implements EnumCode {
	SINGLE
	,MULTI
	,STATEFUL_SET
	,DAEMON_SET
	,JOB
	,CRON_JOB
//	,PACKAGE
	;
	
	@Override
	public String getCode() {
	    return this.name();
	}
	
	public static ServerType codeOf(String code) {
	    return ServerType.valueOf(code);
	}
}
