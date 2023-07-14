package run.acloud.api.build.enums;

import run.acloud.commons.enums.EnumCode;

public enum StepState implements EnumCode {
	WAIT , RUNNING , ERROR , DONE ;
	
	@Override
	public String getCode() {
		return this.name();
	}
	
	public static StepState codeOf(String code) {
		return StepState.valueOf(code);
	}
}
