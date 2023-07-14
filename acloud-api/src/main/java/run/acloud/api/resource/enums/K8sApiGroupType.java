package run.acloud.api.resource.enums;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import run.acloud.commons.enums.EnumCode;

import java.util.Arrays;

public enum K8sApiGroupType implements EnumCode {

	CORE("core"),
	BATCH("batch"),
	APPS("apps"),
	EXTENSIONS("extensions"),
	STORAGE("storage.k8s.io"),
	AUTOSCALING("autoscaling"),
	NETWORKING("networking.k8s.io"),
	METRICS("metrics.k8s.io"),
	API_EXTENSIONS("apiextensions.k8s.io"),
	AUTHORIZATION("authorization.k8s.io"),
	K8S_CNI_CNCF_IO("k8s.cni.cncf.io"),
	RBAC_AUTHORIZATION("rbac.authorization.k8s.io"),
	POLICY("policy"),
	CERT_MANAGER_IO("cert-manager.io"),
	ACME_CERT_MANAGER_IO("acme.cert-manager.io")
	;

	@Getter
	private String value;

	K8sApiGroupType(String value){
		this.value = value;
	}

	@Override
	public String getCode() {
		return this.name();
	}

	public static K8sApiGroupType findApiGroupByValue(String value) {
		return Arrays.stream(K8sApiGroupType.values()).filter(ag -> (StringUtils.equalsIgnoreCase(value, ag.getValue()))).findFirst().orElseGet(() ->null);
	}


}
