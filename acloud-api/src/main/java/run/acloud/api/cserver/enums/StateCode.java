package run.acloud.api.cserver.enums;

import run.acloud.commons.enums.EnumCode;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public enum StateCode implements EnumCode {
	//	AVAILABLE,
//    NOT_AVAILABLE,
    INSUFFICENT_RESOURCE,
    CREATING,
    DEPLOYING,
    INITIALIZING,
    RUNNING,
    RUNNING_PREPARE,
    RUNNING_WARNING,
    STOPPING, // terminating
    STOPPED,  // terminated
    DELETING,
    DELETED,
	COMPLETED,
	FAILED,
	READY,
//    IN_SCALING,
//    OUT_SCALING,
//    IN_OUT_SCALING,
    UPDATING,
    PENDING,
    UNKNOWN,
    IGNORE,
    ERROR;

	// 개발이 완료되면 DELETED 상태는 삭제
	private final static EnumSet<StateCode> CAN_DELETE_SERVER_SET = EnumSet.of(
            RUNNING, RUNNING_PREPARE, RUNNING_WARNING, DEPLOYING, STOPPED, ERROR, PENDING, COMPLETED, FAILED);

//	private final static EnumSet<StateCode> CAN_START_SERVER_SET = EnumSet.of(STOPPED, ERROR);

//	private final static EnumSet<StateCode> CAN_STOP_SERVER_SET = EnumSet.of(AVAILABLE, NOT_AVAILABLE, ERROR);

	private final static EnumSet<StateCode> CAN_SCALE_INOUT_SERVER_SET = EnumSet.of(RUNNING, ERROR);

	private final static EnumSet<StateCode> CAN_DEPLOY_SERVER_SET = EnumSet.of(RUNNING, RUNNING_PREPARE, RUNNING_WARNING,
            UPDATING, ERROR);

	private final static EnumSet<StateCode> CAN_DEPLOY_PACKAGE_SERVER_SET = EnumSet.of(RUNNING, RUNNING_PREPARE, RUNNING_WARNING,
		UPDATING, ERROR, PENDING, STOPPED);

	public String getCode() {
		return this.name();
	}

	public static StateCode codeOf(String code) {
		return StateCode.valueOf(code);
	}

	public boolean canDoServerAction(StateCode stateCode) {
		boolean result = false;

		switch (stateCode) {
			case DELETING:
				result = CAN_DELETE_SERVER_SET.contains(this);
				break;
//		case STARTING:
//			result = CAN_START_SERVER_SET.contains(this);
//			break;
//		case STOPPING:
//			result = CAN_STOP_SERVER_SET.contains(this);
//			break;
//		case IN_OUT_SCALING:
//			result = CAN_SCALE_INOUT_SERVER_SET.contains(this);
//			break;
//		case UP_DOWN_SCALING:
//			result = CAN_SCALE_UPDOWN_SERVER_SET.contains(this);
//			break;
			case DEPLOYING:
				result = CAN_DEPLOY_SERVER_SET.contains(this);
				break;
			default:
				break;
		}

		return result;
	}


	public boolean canDoPackageServerAction(StateCode stateCode) {
		boolean result = false;

		switch (stateCode) {
			case DELETING:
				result = CAN_DELETE_SERVER_SET.contains(this);
				break;
			case DEPLOYING:
				result = CAN_DEPLOY_PACKAGE_SERVER_SET.contains(this);
				break;
			default:
				break;
		}

		return result;
	}


	private final static EnumSet<StateCode> CAN_EDIT = EnumSet.of(RUNNING, RUNNING_PREPARE, RUNNING_WARNING, ERROR);
	private final static EnumSet<StateCode> CAN_TERMINATE = EnumSet.of(RUNNING, RUNNING_PREPARE, RUNNING_WARNING, CREATING, UPDATING, DEPLOYING, ERROR, PENDING, COMPLETED, READY, FAILED, UNKNOWN);
	private final static EnumSet<StateCode> CAN_RECREATE = EnumSet.of(STOPPED);
	private final static EnumSet<StateCode> CAN_REMOVE = EnumSet.of(STOPPED);
	private final static EnumSet<StateCode> CAN_RESTORE = EnumSet.of(RUNNING, RUNNING_PREPARE, RUNNING_WARNING);

	public boolean canDoDeployAction(DeployAction action) {
		boolean valid = false;

		switch (action) {
			case EDIT:
				valid = CAN_EDIT.contains(this);
				break;
			case TERMINATE:
				valid = CAN_TERMINATE.contains(this);
				break;
			case RECREATE:
				valid = CAN_RECREATE.contains(this);
				break;
			case REMOVE:
				valid = CAN_REMOVE.contains(this);
				break;
			case RESTORE:
				valid = CAN_RESTORE.contains(this);
				break;
			default:
				break;
		}

		return valid;
	}

	public List<DeployAction> possibleActions() {
		List<DeployAction> actions = new ArrayList<>();

		for (DeployAction action : DeployAction.values()) {
			if (this.canDoDeployAction(action)) {
				actions.add(action);
			}
		}

		return actions;
	}
}
