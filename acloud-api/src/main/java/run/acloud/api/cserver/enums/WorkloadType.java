package run.acloud.api.cserver.enums;

import lombok.Getter;
import org.apache.commons.lang3.BooleanUtils;
import run.acloud.api.resource.enums.K8sApiKindType;
import run.acloud.commons.enums.EnumCode;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

public enum WorkloadType implements EnumCode {

	LEGACY_SERVER(K8sApiKindType.DEPLOYMENT, "N", 1)
	,SINGLE_SERVER(K8sApiKindType.DEPLOYMENT, "N", 2)
	,REPLICA_SERVER(K8sApiKindType.DEPLOYMENT, "Y", 3)
	,STATEFUL_SET_SERVER(K8sApiKindType.STATEFUL_SET, "Y", 4)
	,DAEMON_SET_SERVER(K8sApiKindType.DAEMON_SET, "Y", 5)
	,JOB_SERVER(K8sApiKindType.JOB, "Y", 6)
	,CRON_JOB_SERVER(K8sApiKindType.CRON_JOB, "Y", 7)
//	,PACKAGE_SERVER("Y", 8)
	;

	public static class Names{
		public static final String LEGACY_SERVER = "LEGACY_SERVER";
		public static final String SINGLE_SERVER = "SINGLE_SERVER";
		public static final String REPLICA_SERVER = "REPLICA_SERVER";
		public static final String STATEFUL_SET_SERVER = "STATEFUL_SET_SERVER";
		public static final String DAEMON_SET_SERVER = "DAEMON_SET_SERVER";
		public static final String JOB_SERVER = "JOB_SERVER";
		public static final String CRON_JOB_SERVER = "CRON_JOB_SERVER";
//		public static final String PACKAGE_SERVER = "PACKAGE_SERVER";
	}

	@Getter
	private K8sApiKindType k8sApiKindType;

	@Getter
	private String support;

	@Getter
	private Integer order;

	WorkloadType(K8sApiKindType k8sApiKindType, String support, Integer order) {
		this.k8sApiKindType = k8sApiKindType;
		this.support = support;
		this.order = order;
	}

	public boolean isPossibleAutoscaling() {
		return EnumSet.of(WorkloadType.SINGLE_SERVER, WorkloadType.REPLICA_SERVER, WorkloadType.STATEFUL_SET_SERVER).contains(this);
	}

	public boolean isPossibleRollout() {
		return EnumSet.of(WorkloadType.SINGLE_SERVER, WorkloadType.REPLICA_SERVER, WorkloadType.STATEFUL_SET_SERVER, WorkloadType.DAEMON_SET_SERVER, WorkloadType.CRON_JOB_SERVER).contains(this);
	}

	public static List<WorkloadType> getSupoortWorkloadTypes(){
		return Arrays.asList(WorkloadType.values()).stream().filter(wt -> (BooleanUtils.toBoolean(wt.getSupport()))).collect(Collectors.toList());
	}

	@Override
	public String getCode() {
		return this.name();
	}
}
