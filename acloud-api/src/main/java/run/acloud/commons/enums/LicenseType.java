package run.acloud.commons.enums;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum LicenseType implements EnumCode {
	TRIAL
	,FULL
	;

	public static class Names{
		public static final String TRIAL = "TRIAL";
		public static final String FULL = "FULL";
	}

	@Override
	public String getCode() {
		return this.name();
	}

	public static List<String> getList(){
		return Arrays.asList(LicenseType.values()).stream().map(s -> s.getCode()).collect(Collectors.toList());
	}

}
