package run.acloud.api.configuration.enums;

import lombok.Getter;
import run.acloud.commons.enums.EnumCode;

public enum AddonKeyItem implements EnumCode {
	VALUE_YAML("value.yaml"),
	VALUE_YAML_PREV("value.yaml-prev"),
	RELEASE_NAMESPACE("releaseNamespace"),
	AUTO_UPDATE("autoUpdate"),
	VERSION("version"),
	VERSION_PREV("version-prev"),
	APP_VERSION("appVersion"),
	USE_YN("useYn"),
	ADDON_TOML("addon.toml"),
	ADDON_TOML_PREV("addon.toml-prev"),
	ADDON_YAML("addon.yaml"),
	ADDON_YAML_PREV("addon.yaml-prev"),
	KIALI_CA_CRT("tlsCaCrt"),
	KIALI_CA_PUBLIC("tlsCaPublic"),
	KIALI_CA_PRVATE("tlsCaPrivate"),
	KIALI_ADDRESS_LIST("tlsAddressList"),
	KIALI_URL("kiali_url"),
	KIALI_USER("kiali_user"),
	KIALI_PASSWORD("kiali_password"),
	MANIFEST("manifest"),
	REPOSITORY("repository")
	;

	@Getter
	private String value;

	AddonKeyItem(String value) {
		this.value = value;
	}

	@Override
	public String getCode() {
		return this.name();
	}
}
