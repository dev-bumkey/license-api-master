package run.acloud.api.pipelineflow.enums;

import run.acloud.commons.enums.EnumCode;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public enum PipelineContainerState implements EnumCode {
    CREATED, BUILDING, BUILD_ERROR, DEPLOYING, DEPLOYED, ERROR;

	@Override
	public String getCode() {
		return this.name();
	}

	private final static EnumSet<PipelineContainerState> CAN_VIEW = EnumSet.of(
			CREATED, ERROR, DEPLOYED, BUILD_ERROR);

	private final static EnumSet<PipelineContainerState> CAN_BUILD = EnumSet.of(
			CREATED, ERROR, DEPLOYED);

	private final static EnumSet<PipelineContainerState> CAN_BUILD_CANCEL = EnumSet.of(
			BUILDING);

	private final static EnumSet<PipelineContainerState> CAN_LOG = EnumSet.of(
			BUILDING);

	private final static EnumSet<PipelineContainerState> CAN_IMAGE_CHANGE = EnumSet.of(
			CREATED, ERROR, DEPLOYED);

	private final static EnumSet<PipelineContainerState> CAN_ROLLBACK = EnumSet.of(
			BUILD_ERROR);

	public boolean canDo(PipelineContainerAction action) {
		boolean valid = false;

		switch (action) {
			case VIEW:
				valid = CAN_VIEW.contains(this);
				break;
			case BUILD:
				valid = CAN_BUILD.contains(this);
				break;
			case BUILD_CANCEL:
				valid = CAN_BUILD_CANCEL.contains(this);
				break;
			case LOG:
				valid = CAN_LOG.contains(this);
				break;
			case IMAGE_CHANGE:
				valid = CAN_IMAGE_CHANGE.contains(this);
				break;
			case ROLLBACK:
				valid = CAN_ROLLBACK.contains(this);
				break;
			default:
				break;
		}

		return valid;
	}

	public List<PipelineContainerAction> possibleActions(PipelineType pipelineType) {
		List<PipelineContainerAction> actions = new ArrayList<>();

		// public deploy일 경우는 이미지 변경만 가능
		if(pipelineType == PipelineType.PUBLIC_DEPLOY) {
			actions.add(PipelineContainerAction.IMAGE_CHANGE);
		} else {
			for (PipelineContainerAction action : PipelineContainerAction.values()) {
				if (this.canDo(action)) {
					actions.add(action);
				}
			}
		}

		return actions;
	}

	public static PipelineContainerState getState(PipelineType pipelineType, PipelineRunState buildState, PipelineRunState deployState){
		PipelineContainerState containerState = PipelineContainerState.CREATED;

		// 두상태 모두 create 일 경우는 최초 생성된 pipeline 이고, 이미 배포된 상태임
		if(buildState == PipelineRunState.CREATED && deployState == PipelineRunState.CREATED) {
			containerState = PipelineContainerState.DEPLOYED;
		}else if(pipelineType == PipelineType.PUBLIC_DEPLOY || (pipelineType == PipelineType.BUILD_DEPLOY && buildState == PipelineRunState.DONE)){
			switch (deployState){
				case RUNNING:
				case WAIT:
					containerState = PipelineContainerState.DEPLOYING;
					break;
				case DONE:
					containerState = PipelineContainerState.DEPLOYED;
					break;
				case ERROR:
					containerState = PipelineContainerState.ERROR;
					break;
			}
		}else if(pipelineType == PipelineType.BUILD_DEPLOY){
			switch (buildState){
				case RUNNING:
				case WAIT:
					containerState = PipelineContainerState.BUILDING;
					break;
				case ERROR:
					containerState = PipelineContainerState.BUILD_ERROR;
					break;
			}
		}
		return containerState;
	}
}
