package run.acloud.api.resource.enums;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import run.acloud.commons.enums.EnumCode;

import java.util.Arrays;
import java.util.EnumSet;

public enum K8sApiKindType implements EnumCode {

	POD("Pod")
	,DEPLOYMENT("Deployment")
	,REPLICA_SET("ReplicaSet")
	,SERVICE("Service")
	,ENDPOINTS("Endpoints")
	,NAMESPACE("Namespace")
	,INGRESS("Ingress")
	,INGRESS_CLASS("IngressClass")
	,CONFIG_MAP("ConfigMap")
	,SECRET("Secret")
	,PERSISTENT_VOLUME_CLAIM("PersistentVolumeClaim")
	,PERSISTENT_VOLUME("PersistentVolume")
	,HORIZONTAL_POD_AUTOSCALER("HorizontalPodAutoscaler")
	,STATEFUL_SET("StatefulSet")
	,DAEMON_SET("DaemonSet")
	,JOB("Job")
	,CRON_JOB("CronJob")
	,STORAGE_CLASS("StorageClass")
	,NETWORK_ATTACHMENT_DEFINITION("NetworkAttachmentDefinition")
	,SERVICE_ACCOUNT("ServiceAccount")
	,CLUSTER_ROLE("ClusterRole")
	,CLUSTER_ROLE_BINDING("ClusterRoleBinding")
	,ROLE("Role")
	,ROLE_BINDING("RoleBinding")
	,CUSTOM_RESOURCE_DEFINITION("CustomResourceDefinition")
	,CUSTOM_OBJECT("CustomObject")
	,USER("User")
	,POD_SECURITY_POLICY("PodSecurityPolicy")
	,GROUP("Group")
	,LIMIT_RANGE("LimitRange")
	,RESOURCE_QUOTA("ResourceQuota")
	,NETWORK_POLICY("NetworkPolicy")
	,CLUSTER_ISSUER("ClusterIssuer")
	,ISSUER("Issuer")
	,CERTIFICATE("Certificate")
	,CERTIFICATE_REQUEST("CertificateRequest")
	,ORDER("Order")
	,CHALLENGE("Challenge")
	,UNSUPPORTED_RESOURCE("UnsupportedResource")
	;

	@Getter
	private String value;

	K8sApiKindType(String value){
		this.value = value;
	}

	public boolean isWorkload(){
		return K8sApiKindType.getWorkloadEnumSet().contains(this);
	}

	public static EnumSet<K8sApiKindType> getWorkloadEnumSet() {
		return EnumSet.of(DEPLOYMENT, STATEFUL_SET, DAEMON_SET, JOB, CRON_JOB);
	}

	public static K8sApiKindType findKindTypeByValue(String kind) {
		return Arrays.stream(K8sApiKindType.values()).filter(kindType -> (StringUtils.equalsIgnoreCase(kind, kindType.getValue()))).findFirst().orElseGet(() ->null);
	}

	@Override
	public String getCode() {
		return this.name();
	}
}
