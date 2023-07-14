package run.acloud.commons.enums;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum DataType implements EnumCode {
	JSON
	,YAML
	,SNAPSHOT
	;

	@Override
	public String getCode() {
		return this.name();
	}

	public static List<String> getDataTypeListToString(){
		return Arrays.asList(DataType.values()).stream().map(s -> s.getCode()).collect(Collectors.toList());
	}

	public static List<DataType> getDataTypeList(){
		return new ArrayList(Arrays.asList(DataType.values()));
	}

}
