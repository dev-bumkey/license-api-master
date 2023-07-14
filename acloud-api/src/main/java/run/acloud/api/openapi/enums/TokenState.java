package run.acloud.api.openapi.enums;

import lombok.Getter;
import run.acloud.commons.enums.EnumCode;

public enum TokenState implements EnumCode {
	GRANT("Grant"),
	CHANGE("Change"),
	REVOKE("Revoke"),
	EXPIRED("Expired")
	;

	@Getter
	private String state;

	TokenState(String state) {
		this.state = state;
	}

	@Override
	public String getCode() {
		return this.name();
	}
}
