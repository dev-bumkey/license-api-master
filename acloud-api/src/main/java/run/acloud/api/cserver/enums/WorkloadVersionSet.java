package run.acloud.api.cserver.enums;

import lombok.Getter;
import run.acloud.api.resource.enums.K8sApiKindType;
import run.acloud.api.resource.enums.K8sApiVerKindType;
import run.acloud.api.resource.enums.K8sApiVerType;
import run.acloud.commons.enums.EnumCode;

import java.util.*;
import java.util.stream.Collectors;

public enum WorkloadVersionSet implements EnumCode {

	SINGLE_V1(WorkloadType.SINGLE_SERVER, WorkloadVersion.V1, WorkloadVersionSet.getClusterVersionSupport(WorkloadVersion.V1), WorkloadVersionSet.getWorkloadSetMap(WorkloadVersion.V1)),
	REPLICA_V1(WorkloadType.REPLICA_SERVER, WorkloadVersion.V1, WorkloadVersionSet.getClusterVersionSupport(WorkloadVersion.V1), WorkloadVersionSet.getWorkloadSetMap(WorkloadVersion.V1)),
	STATEFUL_SET_V1(WorkloadType.STATEFUL_SET_SERVER, WorkloadVersion.V1, WorkloadVersionSet.getClusterVersionSupport(WorkloadVersion.V1), WorkloadVersionSet.getWorkloadSetMap(WorkloadVersion.V1)),
	DAEMON_SET_V1(WorkloadType.DAEMON_SET_SERVER, WorkloadVersion.V1, WorkloadVersionSet.getClusterVersionSupport(WorkloadVersion.V1), WorkloadVersionSet.getWorkloadSetMap(WorkloadVersion.V1)),
	JOB_V1(WorkloadType.JOB_SERVER, WorkloadVersion.V1, WorkloadVersionSet.getClusterVersionSupport(WorkloadVersion.V1), WorkloadVersionSet.getWorkloadSetMap(WorkloadVersion.V1)),
	CRON_JOB_V1(WorkloadType.CRON_JOB_SERVER, WorkloadVersion.V1, WorkloadVersionSet.getClusterVersionSupport(WorkloadVersion.V1), WorkloadVersionSet.getWorkloadSetMap(WorkloadVersion.V1)),
//	PACKAGE_V1(WorkloadType.PACKAGE_SERVER, WorkloadVersion.V1, WorkloadVersionSet.getClusterVersionSupport(WorkloadVersion.V1), WorkloadVersionSet.getWorkloadSetMap(WorkloadVersion.V1))

	;

	private static Map<K8sApiKindType, K8sApiVerKindType> getWorkloadSetMap(WorkloadVersion workloadVersion){
		Map<K8sApiKindType, K8sApiVerKindType> enumSetMap = new HashMap<>();

		if (workloadVersion == WorkloadVersion.V1){
			enumSetMap.put(K8sApiKindType.DEPLOYMENT, K8sApiVerKindType.DEPLOYMENT_V1_14);
			enumSetMap.put(K8sApiKindType.STATEFUL_SET, K8sApiVerKindType.STATEFUL_SET_V1_14);
			enumSetMap.put(K8sApiKindType.DAEMON_SET, K8sApiVerKindType.DAEMON_SET_V1_14);
			enumSetMap.put(K8sApiKindType.JOB, K8sApiVerKindType.JOB_V1_14);
			enumSetMap.put(K8sApiKindType.CRON_JOB, K8sApiVerKindType.CRON_JOB_V1_14);
			enumSetMap.put(K8sApiKindType.SERVICE, K8sApiVerKindType.SERVICE_V1_14);
			enumSetMap.put(K8sApiKindType.INGRESS, K8sApiVerKindType.INGRESS_V1_14);
			enumSetMap.put(K8sApiKindType.PERSISTENT_VOLUME_CLAIM, K8sApiVerKindType.PERSISTENT_VOLUME_CLAIM_V1_14);
			enumSetMap.put(K8sApiKindType.PERSISTENT_VOLUME, K8sApiVerKindType.PERSISTENT_VOLUME_V1_14);
			enumSetMap.put(K8sApiKindType.HORIZONTAL_POD_AUTOSCALER, K8sApiVerKindType.HORIZONTAL_POD_AUTOSCALER_V1_14);
		}

		return enumSetMap;
	}

	private static EnumSet<K8sApiVerType> getClusterVersionSupport(WorkloadVersion workloadVersion){

		if(workloadVersion == WorkloadVersion.V1){
			return K8sApiVerType.getSupportApiVersionUpto(K8sApiVerType.V1_14);
		}

		return null;
	}

	@Getter
	private WorkloadType workloadType;

	@Getter
	private WorkloadVersion workloadVersion;

	@Getter
	private EnumSet<K8sApiVerType> clusterVersion;

	@Getter
	private Map<K8sApiKindType, K8sApiVerKindType> apiVerKindEnumSetMap;

	WorkloadVersionSet(WorkloadType workloadType, WorkloadVersion workloadVersion, EnumSet<K8sApiVerType> clusterVersion, Map<K8sApiKindType, K8sApiVerKindType> apiVerKindEnumSetMap) {
		this.workloadType = workloadType;
		this.workloadVersion = workloadVersion;
		this.clusterVersion = clusterVersion;
		this.apiVerKindEnumSetMap = apiVerKindEnumSetMap;
	}

	@Override
	public String getCode() {
		return this.name();
	}

	public static WorkloadVersionSet getSupport(WorkloadType workloadType, WorkloadVersion workloadVersion){
		return Arrays.stream(WorkloadVersionSet.values()).filter(vk -> (vk.getWorkloadType() == workloadType && vk.getWorkloadVersion() == workloadVersion))
				.findFirst()
				.orElseGet(() ->null);
	}

	public static List<WorkloadVersionSet> getWorkloadVersionSetByType(WorkloadType workloadType){
		return Arrays.stream(WorkloadVersionSet.values()).filter(vk -> (vk.getWorkloadType() == workloadType)).collect(Collectors.toList());
	}

}
