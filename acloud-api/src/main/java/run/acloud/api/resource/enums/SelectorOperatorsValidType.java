package run.acloud.api.resource.enums;

import lombok.Getter;
import run.acloud.commons.enums.EnumCode;

public enum SelectorOperatorsValidType implements EnumCode {

	NOT_NULL("must be non-empty"),
	NULL("must be empty"),
	SINGLE("must have a single element")
	;

	@Getter
	private String desc;

	SelectorOperatorsValidType(String desc){
		this.desc = desc;
	}

	@Override
	public String getCode() {
		return this.name();
	}
}
