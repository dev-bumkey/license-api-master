package run.acloud.api.resource.enums;

import lombok.Getter;
import run.acloud.api.resource.constants.KubeConstants;
import run.acloud.commons.enums.EnumCode;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum TolerationOperators implements EnumCode {

	EXISTS(KubeConstants.TOLERATION_OPERATOR_EXISTS),
	EQUAL(KubeConstants.TOLERATION_OPERATOR_EQUAL)
	;

	@Getter
	private String value;

	TolerationOperators(String value){
		this.value = value;
	}

	public static List<String> getValueList(){
		return Arrays.asList(TolerationOperators.values()).stream().map(s -> s.getValue()).collect(Collectors.toList());
	}

	@Override
	public String getCode() {
		return this.name();
	}
}
