package run.acloud.api.resource.enums;

import lombok.Getter;
import run.acloud.commons.enums.EnumCode;

public enum JsonPatchOp implements EnumCode {

	ADD("add"),
	ADD_ANNO("add"),
	REPLACE("replace"),
	REMOVE("remove")
	;

	@Getter
	private String value;

	JsonPatchOp(String value){
		this.value = value;
	}

	@Override
	public String getCode() {
		return this.name();
	}
}
