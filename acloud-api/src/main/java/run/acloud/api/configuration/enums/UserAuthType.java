package run.acloud.api.configuration.enums;

import com.google.common.collect.Maps;
import lombok.Getter;
import run.acloud.commons.enums.EnumCode;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public enum UserAuthType implements EnumCode {
	PLAIN("PLAIN ID/PW", true),
	AAD("Azure Active Directory", false),
	AD("Active Directory", true)
	;

	@Getter
	private String value;

	@Getter
	private boolean used;

	UserAuthType(String value, boolean used) {
		this.value = value;
		this.used = used;
	}

	public Map<String, String> toMap() {
		Map<String, String> valueMap = Maps.newHashMap();
		valueMap.put("code", this.getCode());
		valueMap.put("value", this.getValue());

		return valueMap;
	}

	public static List<Map<String, String>> getValueList(){
		return Arrays.asList(UserAuthType.values()).stream().filter(s -> (s.isUsed())).map(s -> s.toMap()).collect(Collectors.toList());
	}

	@Override
	public String getCode() {
		return this.name();
	}
}
