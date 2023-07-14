package run.acloud.api.configuration.enums;

import lombok.Getter;
import run.acloud.commons.enums.EnumCode;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public enum IssueRole implements EnumCode {
	CLUSTER_ADMIN("cluster-admin"),
	ADMIN("admin"),
	EDIT("edit"),
	VIEW("view");

	@Getter
	private String role;

	IssueRole(String role) {
		this.role = role;
	}

    public static Map<String, String> toMap(){
        return Arrays.asList(IssueRole.values()).stream().collect(Collectors.toMap(IssueRole::getCode, IssueRole::getRole));
    }

	@Override
	public String getCode() {
		return this.name();
	}
}
