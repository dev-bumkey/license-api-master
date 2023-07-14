package run.acloud.api.configuration.enums;

import lombok.Getter;
import run.acloud.commons.enums.EnumCode;

public enum IssueBindingType implements EnumCode {
	CLUSTER("cluster"),
	NAMESPACE("namespace");

	@Getter
	private String type;

	IssueBindingType(String type) {
		this.type = type;
	}

	@Override
	public String getCode() {
		return this.name();
	}
}
