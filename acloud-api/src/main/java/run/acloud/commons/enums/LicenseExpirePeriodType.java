package run.acloud.commons.enums;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum LicenseExpirePeriodType implements EnumCode {
	YEAR
	,MONTH
	,DAY
	;

	public static class Names{
		public static final String YEAR = "YEAR";
		public static final String MONTH = "MONTH";
		public static final String DAY = "DAY";
	}

	@Override
	public String getCode() {
		return this.name();
	}

	public static List<String> getList(){
		return Arrays.asList(LicenseExpirePeriodType.values()).stream().map(s -> s.getCode()).collect(Collectors.toList());
	}

}
