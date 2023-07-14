package run.acloud.api.configuration.enums;

import lombok.Getter;
import run.acloud.commons.enums.EnumCode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum ComparisonOperator implements EnumCode {
	equalTo("equalTo")
	, unequalTo("unequalTo")
	, lessThan("lessThan")
	, lessThanOrEqualTo("lessThanOrEqualTo")
	, greaterThan("greaterThan")
	, greaterThanOrEqualTo("greaterThanOrEqualTo")
	;

	@Getter
	private String value;

	ComparisonOperator(String value) {
		this.value = value;
	}

	@Override
	public String getCode() {
		return this.name();
	}

	/**
	 * AddonDynamicValueType value의 목록 응답
	 * @return
	 */
	public static List<String> getComparisonOperatorValues(){
		return Arrays.asList(ComparisonOperator.values()).stream().map(s -> s.getValue()).collect(Collectors.toList());
	}

	/**
	 * AddonDynamicValueType 목록 응답
	 * @return
	 */
	public static List<ComparisonOperator> getComparisonOperatorList(){
		return new ArrayList(Arrays.asList(ComparisonOperator.values()));
	}

}
