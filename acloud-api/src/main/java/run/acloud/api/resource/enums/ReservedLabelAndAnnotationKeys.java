package run.acloud.api.resource.enums;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import run.acloud.api.resource.constants.KubeConstants;
import run.acloud.commons.enums.EnumCode;

import java.util.Arrays;

public enum ReservedLabelAndAnnotationKeys implements EnumCode {
	/**
	 * https://kubernetes.io/docs/concepts/overview/working-with-objects/annotations/
	 * https://kubernetes.io/ko/docs/concepts/overview/working-with-objects/labels/
	 */
	KUBERNETES_IO("kubernetes.io"),
	K8S_IO("k8s.io"),
	KUBE_SCHEDULER("kube-scheduler"),
	KUBE_CONTROLLER_MANAGER("kube-controller-manager"),
	KUBE_APISERVER("kube-apiserver"),
	KUBECTL("kubectl"),

	ACORNSOFT("acornsoft"),
	COCKTAIL("cocktail"),
	ACLOUD("acloud"),

	VOLUME_STORAGE(KubeConstants.CUSTOM_VOLUME_STORAGE),
	PERSISTENT_VOLUME_TYPE(KubeConstants.CUSTOM_PERSISTENT_VOLUME_TYPE),
	VOLUME_TYPE(KubeConstants.CUSTOM_VOLUME_TYPE)
	;

	@Getter
	private String value;

	ReservedLabelAndAnnotationKeys(String value){
		this.value = value;
	}

	public static ReservedLabelAndAnnotationKeys getKeyWhoesValueContainsKeys(String value) {
		return Arrays.stream(ReservedLabelAndAnnotationKeys.values()).filter(key -> (StringUtils.contains(value, key.getValue()))).findFirst().orElseGet(() ->null);
	}

	@Override
	public String getCode() {
		return this.name();
	}
}
