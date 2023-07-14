package run.acloud.api.catalog.enums;

import lombok.Getter;
import run.acloud.commons.enums.EnumCode;

public enum TemplateType implements EnumCode {
    COCKTAIL("N"),
	PACKAGE("Y"),
	BUILD_PACK("N"),
	SERVICE("Y");

    @Getter
	private String useYn;

	TemplateType(String useYn) {
		this.useYn = useYn;
	}

	@Override
	public String getCode() {
		return this.name();
	}
}
