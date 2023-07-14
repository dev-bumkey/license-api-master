package run.acloud.api.cserver.enums;

import lombok.Getter;
import run.acloud.commons.enums.EnumCode;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public enum NetworkPolicyCreationType implements EnumCode {
	INGRESS_BLOCK_EGRESS_ALLOW(true),
	INGRESS_ALLOW_EGRESS_BLOCK(false),
	INGRESS_ALLOW_EGRESS_ALLOW(false),
	INGRESS_BLOCK_EGRESS_BLOCK(false)
	;

	@Getter
	private boolean isDefault;

	NetworkPolicyCreationType(boolean isDefault) {
		this.isDefault = isDefault;
	}

	public Map<String, Object> toMap(){
		Map<String, Object> toMap = new HashMap<>();
		toMap.put("code", this.getCode());
		toMap.put("isDefault", this.isDefault());

		return toMap;
	}

	public static List<Map<String, Object>> getList() {
		return Arrays.stream(NetworkPolicyCreationType.values()).map(NetworkPolicyCreationType::toMap).collect(Collectors.toList());
	}

	@Override
	public String getCode() {
		return this.name();
	}
}
