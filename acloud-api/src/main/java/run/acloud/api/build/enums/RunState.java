package run.acloud.api.build.enums;

import run.acloud.commons.enums.EnumCode;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public enum RunState implements EnumCode {
	CREATED, RUNNING, DONE, ERROR;

	@Override
	public String getCode() {
		return this.name();
	}

	private final static EnumSet<RunState> CAN_VIEW = EnumSet.of(
			CREATED, RUNNING, DONE, ERROR);

	private final static EnumSet<RunState> CAN_BUILD = EnumSet.of(
			CREATED, DONE, ERROR);

	private final static EnumSet<RunState> CAN_LOG = EnumSet.of(
			         RUNNING, DONE, ERROR);

	private final static EnumSet<RunState> CAN_REMOVE = EnumSet.of(
			CREATED, DONE, ERROR);

	private final static EnumSet<RunState> CAN_CANCEL = EnumSet.of(
			RUNNING);

	public boolean canDo(BuildAction action) {
		boolean valid = false;

		switch (action) {
			case VIEW:
				valid = CAN_VIEW.contains(this);
				break;
			case BUILD:
				valid = CAN_BUILD.contains(this);
				break;
			case LOG:
				valid = CAN_LOG.contains(this);
				break;
			case REMOVE:
				valid = CAN_REMOVE.contains(this);
				break;
			case CANCEL:
				valid = CAN_CANCEL.contains(this);
				break;
			default:
				break;
		}

		return valid;
	}

	public List<BuildAction> possibleActions() {
		List<BuildAction> actions = new ArrayList<>();

		for (BuildAction action : BuildAction.values()) {
			if (this.canDo(action)) {
				actions.add(action);
			}
		}

		return actions;
	}
}
