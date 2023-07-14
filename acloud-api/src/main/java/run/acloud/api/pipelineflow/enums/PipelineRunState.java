package run.acloud.api.pipelineflow.enums;

import run.acloud.commons.enums.EnumCode;

public enum PipelineRunState implements EnumCode {
	CREATED, WAIT , RUNNING , ERROR , DONE;
	
	@Override
	public String getCode() {
		return this.name();
	}
	
	public static PipelineRunState codeOf(String code) {
		return PipelineRunState.valueOf(code);
	}
}
