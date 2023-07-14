package run.acloud.api.configuration.enums;

import lombok.Getter;
import run.acloud.commons.enums.EnumCode;

public enum IssueType implements EnumCode {
	SHELL("Shell"),
	KUBECONFIG("Kubeconfig");

	@Getter
	private String type;

	IssueType(String type) {
		this.type = type;
	}

	@Override
	public String getCode() {
		return this.name();
	}
}
