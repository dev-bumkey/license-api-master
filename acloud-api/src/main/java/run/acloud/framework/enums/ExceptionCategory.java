package run.acloud.framework.enums;

import lombok.Getter;

public enum ExceptionCategory {

	COMMON("CM"),
	SYSTEM("SY"),
	USER("US"),
	ACCOUNT_APPLICATION("AA"),
	ACCOUNT("AT"),
	PROVIDER_ACCOUNT("AC"),
	CLUSTER("CT"),
	SERVICE("SV"),
	SERVICEMAP_GROUP("AG"),
	SERVICEMAP("AP"),
	WORKLOAD_GROUP("GP"),
	SERVER("SR"),
	CLUSTER_VOLUME("CV"),
	BUILD("BD"),
	CONFIG_MAP("CF"),
	SECRET("ST"),
	NET_ATTACH_DEF("NA"),
	CATALOG("CL"),
	PIPELINE("PL"),
	PIPELINE_SET("PT"),
	REGISTRY("RG"),
	EXTERNAL_REGISTRY("ER"),
	SERVICE_SPEC("SS"),
	INGRESS_SPEC("IS"),
	PACKAGE("PK"),
	BILLING("BL"),
	CLUSTER_ROLE("CR"),
	CLUSTER_ROLE_BINDING("CB"),
	ROLE("RL"),
	ROLE_BINDING("RB"),
	SERVICE_ACCOUNT("SA"),
	POD_SECURITY_POLICY("PS"),
	LIMIT_RANGE("LR"),
	RESOURCE_QUOTA("RQ"),
	NETWORK_POLICY("NP"),
	CUSTOM_OBJECT("CO"),
	ALERT("AL")
	;

	@Getter
	private String categoryCode;

	ExceptionCategory(String categoryCode) {
		this.categoryCode = categoryCode;
	}

	public String getCode() {
		return this.name();
	}
}
