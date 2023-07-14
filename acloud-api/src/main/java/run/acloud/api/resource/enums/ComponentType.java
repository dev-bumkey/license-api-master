package run.acloud.api.resource.enums;

import run.acloud.commons.enums.EnumCode;

public enum ComponentType implements EnumCode {
	CSERVER(ResourceType.SERVER)/*, RDB(ResourceType.RDB), STORAGE(ResourceType.STORAGE)*/;

	ResourceType resourceCode;
	
	ComponentType(ResourceType resourceCode) {
		this.resourceCode = resourceCode;
	}
	
	public static ComponentType codeOf(String code) {
		return ComponentType.valueOf(code);
	}
	
	@Override
	public String getCode() {
		return this.name();
	}
	
	public String getResourceCode() {
		return resourceCode.getCode();
	}
}
