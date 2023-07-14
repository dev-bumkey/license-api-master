package run.acloud.api.configuration.enums;

import lombok.Getter;
import run.acloud.commons.enums.EnumCode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum AddonDynamicValueType implements EnumCode {
	SYSTEM_RESOURCE_PREFIX("#{System.ResourcePrefix}")
	, SYSTEM_CLUSTER_ID("#{System.Cluster.ClusterId}")
	, SYSTEM_CLUSTER_SEQ("#{System.Cluster.ClusterSeq}")
	, PACKAGE_CHART_NAME("#{Package.ChartName}")
	, PACKAGE_CHART_VERSION("#{Package.ChartVersion}")
	, PACKAGE_APP_VERSION("#{Package.AppVersion}")
	, PACKAGE_RELEASE_NAME("#{Package.ReleaseName}")
	, PACKAGE_FIXED_RELEASE_NAME("#{Package.FixedReleaseName}")

	, PACKAGE_AGENT_RELEASE_NAME("#{Package.AgentReleaseName}")
	, SYSTEM_CLUSTER_TYPE("#{System.Cluster.ClusterType}")
	, SYSTEM_PROMETHEUS_URL("#{System.PrometheusUrl}")
	, SYSTEM_ALERT_MANAGER_URL("#{System.AlertManager.url}")
	, SYSTEM_COLLECTOR_SERVER_URL("#{System.CollectorServerUrl}")
	, SYSTEM_MONITOR_API_URL("#{System.MonitorApiUrl}")
	, SYSTEM_BASE64_MONITORING_SECRET("#{System.Base64MonitoringSecret}")
	, SYSTEM_BASE64_CLUSTER_ID("#{System.Base64ClusterId}")
	, SYSTEM_BASE64_CLUSTER_SEQ("#{System.Base64ClusterSeq}")
	, SYSTEM_IMAGE_BASE_URL("#{System.ImageBaseUrl}")
	, SYSTEM_CHART_REPO_URL("#{System.ChartRepoUrl}")
	, SYSTEM_CHART_REPO_PROJECT_NAME("#{System.ChartRepoProjectName}")
	, SYSTEM_CHART_REPO_USER("#{System.ChartRepoUser}")
	, SYSTEM_CHART_REPO_PASSWORD("#{System.ChartRepoPassword}")
	, SYSTEM_CHART_REPO_CERT("#{System.ChartRepoCert}")
	, SYSTEM_IMAGE_PULL_POLICY_ALWAYS("#{System.ImagePullPolicyAlways}")

	, ISTIO_KIALI_TLS_CRT("#{Istio.Kiali.Tls.Crt}")
	, ISTIO_KIALI_TLS_KEY("#{Istio.Kiali.Tls.Key}")
	, ISTIO_KIALI_TLS_CA_CRT("#{Istio.Kiali.Tls.Ca.Crt}")
	, ISTIO_KIALI_TLS_CA_PUBLIC("#{Istio.Kiali.Tls.Ca.Public}")
	, ISTIO_KIALI_TLS_CA_PRIVATE("#{Istio.Kiali.Tls.Ca.Private}")
	, ISTIO_KIALI_TLS_ADDRESS_LIST("#{Istio.Kiali.Tls.Address.List}")
	;

	@Getter
	private String value;

	AddonDynamicValueType(String value) {
		this.value = value;
	}

	@Override
	public String getCode() {
		return this.name();
	}

	/**
	 * AddonDynamicValueType value의 목록 응답
	 * @return
	 */
	public static List<String> getAddonDynamicValueTypeValues(){
		return Arrays.asList(AddonDynamicValueType.values()).stream().map(s -> s.getValue()).collect(Collectors.toList());
	}

	/**
	 * AddonDynamicValueType 목록 응답
	 * @return
	 */
	public static List<AddonDynamicValueType> getAddonDynamicValueTypeList(){
		return new ArrayList(Arrays.asList(AddonDynamicValueType.values()));
	}

}
