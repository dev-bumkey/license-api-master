package run.acloud.api.resource.enums;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import run.acloud.commons.enums.EnumCode;

import java.util.Arrays;

public enum K8sApiType implements EnumCode {

	V1("v1"),
	V1ALPHA1("v1alpha1"),
	V1BETA1("v1beta1"),
	V1BETA2("v1beta2"),
	V2("v2"),
	V2ALPHA1("v2alpha1"),
	V2BETA1("v2beta1"),
	V2BETA2("v2beta2");

	@Getter
	private String value;

	K8sApiType(String value){
		this.value = value;
	}

	@Override
	public String getCode() {
		return this.name();
	}

	public static K8sApiType findApiTypeByValue(String value) {
		return Arrays.stream(K8sApiType.values()).filter(at -> (StringUtils.equalsIgnoreCase(value, at.getValue()))).findFirst().orElseGet(() ->null);
	}
}
