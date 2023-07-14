package run.acloud.api.catalog.enums;

import run.acloud.commons.enums.EnumCode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum TemplateShareType implements EnumCode {
	SYSTEM_SHARE,
	WORKSPACE_SHARE;

	@Override
	public String getCode() {
		return this.name();
	}

	public static List<String> getTemplateShareTypeListToString(){
		return Arrays.asList(TemplateShareType.values()).stream().map(s -> s.getCode()).collect(Collectors.toList());
	}

	public static List<TemplateShareType> getTemplateShareTypeList(){
		return new ArrayList(Arrays.asList(TemplateShareType.values()));
	}

}
