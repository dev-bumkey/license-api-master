package run.acloud.commons.enums;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum ExecutionResultCode implements EnumCode {
	SUCCESS
	,FAILURE
	,UNKNOWN
	;

	@Override
	public String getCode() {
		return this.name();
	}

	public static List<String> getExecutionResultCodeListToString(){
		return Arrays.asList(ExecutionResultCode.values()).stream().map(s -> s.getCode()).collect(Collectors.toList());
	}

	public static List<ExecutionResultCode> getExecutionResultCodeList(){
		return new ArrayList(Arrays.asList(ExecutionResultCode.values()));
	}

}
