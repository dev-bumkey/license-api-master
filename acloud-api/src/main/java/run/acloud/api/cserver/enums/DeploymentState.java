package run.acloud.api.cserver.enums;

import lombok.Getter;
import lombok.Setter;
import run.acloud.commons.enums.ApiVersionType;
import run.acloud.commons.enums.EnumCode;
import run.acloud.commons.util.Utils;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public enum DeploymentState implements EnumCode {
	ERROR, CREATED, RECREATED, DEPLOYED, EDITED, WILL_TERMINATE, TERMINATED, DEPLOYING;

	@Override
	public String getCode() {
		return this.name();
	}

	@Setter
	@Getter
	private ApiVersionType apiVersionType = ApiVersionType.V1;

	private final static EnumSet<DeploymentState> CAN_EDIT = EnumSet.of(
			ERROR, CREATED, RECREATED, DEPLOYED, EDITED, WILL_TERMINATE);

	private final static EnumSet<DeploymentState> CAN_EDIT_V2 = EnumSet.of(
			ERROR, CREATED, RECREATED, DEPLOYED, EDITED, WILL_TERMINATE, TERMINATED);

	private final static EnumSet<DeploymentState> CAN_TERMINATE = EnumSet.of(
			ERROR, DEPLOYED, EDITED, WILL_TERMINATE);

	private final static EnumSet<DeploymentState> CAN_TERMINATE_V2 = EnumSet.of(
			ERROR, DEPLOYED, EDITED, WILL_TERMINATE, TERMINATED);

	private final static EnumSet<DeploymentState> CAN_RECREATE = EnumSet.of(
			TERMINATED);

//	private final static EnumSet<DeploymentState> CAN_REMOVE = EnumSet.of(
//			CREATED, TERMINATED);

	private final static EnumSet<DeploymentState> CAN_REMOVE = EnumSet.of(
			CREATED, RECREATED, TERMINATED);

	private final static EnumSet<DeploymentState> CAN_RESTORE = EnumSet.of(
			EDITED, WILL_TERMINATE);

	public boolean canDo(DeployAction action) {
		boolean valid = false;

		switch (action) {
			case EDIT:
				if(!Utils.isApiVersionV1(this.getApiVersionType())){
					valid = CAN_EDIT_V2.contains(this);
				}else{
					valid = CAN_EDIT.contains(this);
				}
				break;
			case TERMINATE:
//				if(Utils.isApiVersionV2(this.getApiVersionType())){
//					valid = CAN_TERMINATE_V2.contains(this);
//				}else{
//					valid = CAN_TERMINATE.contains(this);
//				}
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

	public boolean isFixedState() {
		return this == ERROR || this == DEPLOYED;
	}

	public List<DeployAction> possibleActions() {
		List<DeployAction> actions = new ArrayList<>();

		for (DeployAction action : DeployAction.values()) {
			if (this.canDo(action)) {
				actions.add(action);
			}
		}

		return actions;
	}
}
