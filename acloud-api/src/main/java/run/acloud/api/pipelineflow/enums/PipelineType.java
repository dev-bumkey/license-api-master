package run.acloud.api.pipelineflow.enums;

import lombok.Getter;
import run.acloud.commons.enums.EnumCode;

public enum PipelineType implements EnumCode {
    BUILD_DEPLOY("BUILD_DEPLOY"),
	PUBLIC_DEPLOY("PUBLIC_DEPLOY");

	@Getter
    private String type;

    PipelineType(String type){
    	this.type = type;
	}

	@Override
	public String getCode() {
		return this.name();
	}
}
