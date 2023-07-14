package run.acloud.api.configuration.enums;

import lombok.Getter;
import run.acloud.commons.enums.EnumCode;

public enum ClusterState implements EnumCode {
	RUNNING("Running"),
	STOPPED("Stopped"),
	CREATING("Creating"),
	DELETING("Deleting"),
	UPGRADING("Upgrading"),
	RESIZING("Resizing"),
	DELETED("Deleted"),
	ERROR_CREATE("Error_Create"),
	ERROR_DELETE("Error_Delete"),
	ERROR_UPGRADE("Error_Upgrade"),
	ERROR_RESIZE("Error_Resize"),
	ERROR_UNKNOWN("Error_Unknown"),
	DONE("Done");

	@Getter
	private String state;

	ClusterState(String state) {
		this.state = state;
	}

	@Override
	public String getCode() {
		return this.name();
	}
}
