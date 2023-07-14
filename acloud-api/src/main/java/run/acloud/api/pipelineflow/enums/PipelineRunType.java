package run.acloud.api.pipelineflow.enums;

import lombok.Getter;
import run.acloud.commons.enums.EnumCode;

public enum PipelineRunType implements EnumCode {
    BUILD("BUILD"),
	DEPLOY("DEPLOY");

	@Getter
    private String type;

    PipelineRunType(String type){
    	this.type = type;
	}

	@Override
	public String getCode() {
		return this.name();
	}
}
