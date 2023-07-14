package run.acloud.api.resource.enums;

import run.acloud.commons.enums.EnumCode;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum LimitRangeType implements EnumCode {
	Container,
	Pod,
	PersistentVolumeClaim;

	public static List<String> getList() {
		return Arrays.stream(LimitRangeType.values()).map(LimitRangeType::getCode).collect(Collectors.toList());
	}

	@Override
	public String getCode() {
		return this.name();
	}
}
