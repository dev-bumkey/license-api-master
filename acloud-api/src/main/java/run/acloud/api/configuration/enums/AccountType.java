package run.acloud.api.configuration.enums;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import run.acloud.commons.enums.EnumCode;

import java.util.EnumSet;

@Schema(name = "AccountType", description = "AccountType")
public enum AccountType implements EnumCode {
	@Schema(name = "CCO", description = "Cocktail Cloud Online")
	CCO("Cocktail Cloud Online"),
	@Schema(name = "ENT", description = "Enterprise")
	ENT("Enterprise"),
	@Schema(name = "OEM", description = "OEM")
	OEM("OEM"),
	@Schema(name = "APP", description = "Apps")
	APP("Apps"),
	@Schema(name = "CBE", description = "Cube Engine")
	CBE("Cube Engine");

	@Getter
	private String type;

	AccountType(String type) {
		this.type = type;
	}

	public boolean isCubeEngine() {
		return EnumSet.of(CBE).contains(this);
	}

	public boolean isCocktail() {
		return EnumSet.of(CCO, ENT, OEM).contains(this);
	}

	public boolean isApps() {
		return EnumSet.of(APP).contains(this);
	}

	public boolean isOnline() {
		return EnumSet.of(CCO, APP).contains(this);
	}

	@Override
	public String getCode() {
		return this.name();
	}
}
