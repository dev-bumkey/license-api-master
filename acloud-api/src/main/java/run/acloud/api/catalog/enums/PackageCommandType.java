package run.acloud.api.catalog.enums;

import run.acloud.commons.enums.EnumCode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum PackageCommandType implements EnumCode {
	INSTALL
	,UNINSTALL
	,ROLLBACK
	,UPGRADE
	;

	@Override
	public String getCode() {
		return this.name();
	}

	public static List<String> getPackageCommandTypeListToString(){
		return Arrays.asList(PackageCommandType.values()).stream().map(s -> s.getCode()).collect(Collectors.toList());
	}

	public static List<PackageCommandType> getPackageCommandTypeList(){
		return new ArrayList(Arrays.asList(PackageCommandType.values()));
	}

}
