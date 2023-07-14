package run.acloud.api.cserver.enums;

import run.acloud.commons.enums.EnumCode;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum SystemNamespaces implements EnumCode {
	COCKTAIL_SYSTEM("cocktail-system"),
	COCKTAIL_ADDON("cocktail-addon"),
	COCKTAIL_CONTROLLER("cocktail-controller"),
	ACLOUD_SYSTEM("acloud-system"),
	ACLOUD_ADDON("acloud-addon"),
	ACLOUD_CONTROLLER("acloud-controller"),
	KUBE_PUBLIC("kube-public"),
	KUBE_SYSTEM("kube-system");

	private String namespace;

	SystemNamespaces(String namespace) {
		this.namespace = namespace;
	}

	public String getNamespace() {
		return this.namespace;
	}

	public static List<String> getSystemNamespaces(){
		return Arrays.asList(SystemNamespaces.values()).stream().map(s -> s.getNamespace()).collect(Collectors.toList());
	}

	@Override
	public String getCode() {
		return this.name();
	}
}
