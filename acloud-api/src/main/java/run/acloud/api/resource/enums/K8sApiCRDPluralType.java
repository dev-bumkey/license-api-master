package run.acloud.api.resource.enums;

import lombok.Getter;
import run.acloud.commons.enums.EnumCode;

public enum K8sApiCRDPluralType implements EnumCode {

	  NETWORK_ATTACHMENT_DEFINITION("network-attachment-definitions")
	, CLUSTER_ISSUERS("clusterissuers")
	, ISSUERS("issuers")
	, CERTIFICATES("certificates")
	, CERTIFICATE_REQUESTS("certificaterequests")
	, ORDERS("orders")
	, CHALLENGES("challenges")
	;

	@Getter
	private String value;

	K8sApiCRDPluralType(String value){
		this.value = value;
	}

	@Override
	public String getCode() {
		return this.name();
	}
}
