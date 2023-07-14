package run.acloud.framework.enums;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.AntPathMatcher;

import java.util.Arrays;

public enum ExceptionBiz {

	UNKNOWN("unknown", ""),
	SERVERLESS("serverless", "/api/serverless/**"),
	SERVICES("services", ""),
	SERVICEMAP_GROUP("servicemapGroup", "/api/appmapgroup/**|/api/servicemapgroup/**"),
	SERVICEMAP("servicemap", "/api/appmap/**|/api/servicemap/**"),
	SERVER("server", "/api/server/**"),
	WORKLOAD_GROUP("workloadGroup", "/api/group/**|/api/workloadgroup/**"),
	MONITORING("monitoring", ""),
	PIPELINE("pipeline", "/api/pipelineflow/**"),
	PIPELINE_SET("pipelineSet", "/api/pl/**"),
	CONFIG("config", "/api/configmap/**"),
	SECRET("secret", "/api/secret/**"),
	BUILD("build", "/api/build/**"),
	LOG("log", "/api/log/**"),
	CATALOG("catalog", "/api/template/**"),
	CLUSTER("cluster", ""),
	NODE("node", ""),
	APPLICATION("application", ""),
	ACCOUNT_APPLICATION("accountApplication", "/api/account/applications/**"),
	VOLUME("volume", "/api/**/volumes/**"),
	ALARM("alarm", ""),
	METERING("metering", "/api/metering/**"),
	REGISTRATION("registration", "/api/cluster/**"),
	USER("user", "/api/user/**"),
	PROVIDER("provider", "/api/provideraccount/**"),
	SERVICE("service", "/api/service/**"),
	SERVICE_SPEC("serviceSpec", "/api/service-spec/**"),
	INGRESS_SPEC("ingressSpec", "/api/ingress-spec/**"),
	PACKAGE_SERVER("server", "/api/package/**"),
	POD_SECURITY_POLICY("podSecurityPolicy", "/api/psp/**"),
	NETWORK_POLICY("networkPolicy", "/api/np/**"),
	LIMIT_RANGE("podSecurityPolicy", "/api/lr/**"),
	RESOURCE_QUOTA("podSecurityPolicy", "/api/rq/**"),
	BILLING("billing", "/api/billing/**"),
	EXTERNAL_REGISTRY("externalRegistry", "/api/externalregistry/**"),
	ALERT("alert", "/api/alert/**"),
	ACCOUNT_REGISTRY("accountRegistry", "/api/accountregistry/**"),
	OPENAPI("openapi", "/api/openapi/**"),
	CUSTOM_RESOURCE_DEFINITION("customObjectDefinition", "/api/crds/**")
	;

	@Getter
	private String bizCode;

	@Getter
	private String bizUrl;

	ExceptionBiz(String bizCode, String bizUrl) {
		this.bizCode = bizCode;
		this.bizUrl = bizUrl;
	}

	public static ExceptionBiz getExceptionBizByUrl(String bizUrl){
		AntPathMatcher matcher = new AntPathMatcher();
		return Arrays.stream(ExceptionBiz.values()).filter(b -> {
			if(StringUtils.isNotBlank(b.getBizUrl())){
				String[] bizUrls = StringUtils.split(b.getBizUrl(), "|");
				for(String url : bizUrls){
					if (matcher.match(url, bizUrl)) {
						return true;
					}
				}
			}
			return false;
		}).findFirst().orElseGet(() ->ExceptionBiz.UNKNOWN);
	}

	public String getCode() {
		return this.name();
	}
}
