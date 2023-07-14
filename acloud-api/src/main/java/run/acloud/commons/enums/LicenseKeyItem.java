package run.acloud.commons.enums;

import lombok.Getter;

public enum LicenseKeyItem implements EnumCode {
	TYPE("type"),
	PURPOSE("purpose"),
	ISSUER("issuer"),
	COMPANY("company"),
	CAPACITY("capacity"),
	REGION("region"),
	ISSUE_DATE("issueDate"),
	EXPIRY_DATE("expiryDate")
	;

	@Getter
	private String value;

	LicenseKeyItem(String value) {
		this.value = value;
	}

	@Override
	public String getCode() {
		return this.name();
	}
}
