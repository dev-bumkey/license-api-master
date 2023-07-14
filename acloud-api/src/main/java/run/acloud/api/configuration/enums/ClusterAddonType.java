package run.acloud.api.configuration.enums;

import run.acloud.commons.enums.EnumCode;

import java.util.Arrays;

public enum ClusterAddonType implements EnumCode {
	CONTROLLER,
	MONITORING;

	public static ClusterAddonType findClusterAddonType(String findVal){
		return Arrays.stream(ClusterAddonType.values()).filter(vk -> (vk.toString().equals(findVal)))
			.findFirst()
			.orElseGet(() ->null);
	}

	@Override
	public String getCode() {
		return this.name();
	}
}
