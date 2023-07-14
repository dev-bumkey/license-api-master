package run.acloud.api.pipelineflow.enums;

import run.acloud.commons.enums.EnumCode;

public enum PipelineContainerAction implements EnumCode {
    IMAGE_CHANGE, VIEW, BUILD, BUILD_CANCEL, LOG, ROLLBACK;

	@Override
	public String getCode() {
		return this.name();
	}
}
