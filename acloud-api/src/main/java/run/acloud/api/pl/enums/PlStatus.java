package run.acloud.api.pl.enums;

import lombok.Getter;
import run.acloud.commons.enums.EnumCode;

public enum PlStatus implements EnumCode {
	CREATED("")
	, UPDATED("")
	, WAIT("")
	, RUNNING("Start")
	, ERROR("Error")
	, DONE("Done")
	, CANCEL( "Cancel")
	, CANCELED( "Canceled")
	;

	@Getter
	private String action;

	PlStatus(String action) {
		this.action = action;
	}
	
	@Override
	public String getCode() {
		return this.name();
	}
	
	public static PlStatus codeOf(String code) {
		return PlStatus.valueOf(code);
	}
}
