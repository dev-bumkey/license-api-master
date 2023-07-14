package run.acloud.api.resource.enums;

import lombok.Getter;
import run.acloud.commons.enums.EnumCode;

public enum K8sRoleResourceType implements EnumCode {

	  NODES("nodes")
	, BINDINGS("bindings")
	, ROLEBINDINGS("rolebindings")
	, ROLES("roles")
	, EVENTS("events")
	, LOCALSUBJECTACCESSREVIEWS("localsubjectaccessreviews")
	, LIMITRANGES("limitranges")
	, RESOURCEQUOTAS("resourcequotas")
	, RESOURCEQUOTAS_STATUS("resourcequotas/status")
	, NAMESPACES("namespaces")
	, NAMESPACES_STATUS("namespaces/status")
	, SERVICEACCOUNTS("serviceaccounts")
	, NETWORKPOLICIES("networkpolicies")
	, CONTROLLERREVISIONS("controllerrevisions")
	, PODDISRUPTIONBUDGETS("poddisruptionbudgets")
	, PODDISRUPTIONBUDGETS_STATUS("poddisruptionbudgets/status")
	, REPLICATIONCONTROLLERS("replicationcontrollers")
	, REPLICATIONCONTROLLERS_SCALE("replicationcontrollers/scale")
	, REPLICATIONCONTROLLERS_STATUS("replicationcontrollers/status")
	, DAEMONSETS("daemonsets")
	, DAEMONSETS_STATUS("daemonsets/status")
	, DEPLOYMENTS("deployments")
	, DEPLOYMENTS_ROLLBACK("deployments/rollback")
	, DEPLOYMENTS_SCALE("deployments/scale")
	, DEPLOYMENTS_STATUS("deployments/status")
	, REPLICASETS("replicasets")
	, REPLICASETS_SCALE("replicasets/scale")
	, REPLICASETS_STATUS("replicasets/status")
	, STATEFULSETS("statefulsets")
	, STATEFULSETS_SCALE("statefulsets/scale")
	, STATEFULSETS_STATUS("statefulsets/status")
	, CRONJOBS("cronjobs")
	, CRONJOBS_STATUS("cronjobs/status")
	, JOBS("jobs")
	, JOBS_STATUS("jobs/status")
	, PODS("pods")
	, PODS_ATTACH("pods/attach")
	, PODS_EXEC("pods/exec")
	, PODS_PORTFORWARD("pods/portforward")
	, PODS_PROXY("pods/proxy")
	, PODS_LOG("pods/log")
	, PODS_STATUS("pods/status")
	, PERSISTENTVOLUMECLAIMS("persistentvolumeclaims")
	, PERSISTENTVOLUMECLAIMS_STATUS("persistentvolumeclaims/status")
	, HORIZONTALPODAUTOSCALERS("horizontalpodautoscalers")
	, HORIZONTALPODAUTOSCALERS_STATUS("horizontalpodautoscalers/status")
	, SECRETS("secrets")
	, CONFIGMAPS("configmaps")
	, SERVICES("services")
	, SERVICES_PROXY("services/proxy")
	, SERVICES_STATUS("services/status")
	, ENDPOINTS("endpoints")
	, INGRESSES("ingresses")
	, INGRESSES_STATUS("ingresses/status")
	;

	@Getter
	private String value;

	K8sRoleResourceType(String value){
		this.value = value;
	}

	@Override
	public String getCode() {
		return this.name();
	}
}
