package run.acloud.api.configuration.enums;

import lombok.Getter;
import run.acloud.commons.enums.EnumCode;

public enum HistoryState implements EnumCode {
	GRANT("Grant"),
	CHANGE("Change"),
	REVOKE("Revoke"),
	EXPIRED("Expired")
	;

	@Getter
	private String state;

	HistoryState(String state) {
		this.state = state;
	}

	@Override
	public String getCode() {
		return this.name();
	}
}
