package run.acloud.api.pl.enums;

import lombok.Getter;
import run.acloud.commons.enums.EnumCode;

import java.util.*;
import java.util.stream.Collectors;

public enum PlResType implements EnumCode {

	  REPLICA_SERVER("Deployment")
	, STATEFUL_SET_SERVER("StatefulSet")
	, DAEMON_SET_SERVER("DaemonSet")
	, JOB_SERVER("Job")
	, CRON_JOB_SERVER("CronJob")
	, SVC("Service")
	, CM("ConfigMap")
	, SC("Secret")
	, PVC("PersistentVolumeClaim")
	, IG("Ingress")
	, BUILD("Build")
	;

	public static class Names{
		public static final String REPLICA_SERVER = "REPLICA_SERVER";
		public static final String STATEFUL_SET_SERVER = "STATEFUL_SET_SERVER";
		public static final String DAEMON_SET_SERVER = "DAEMON_SET_SERVER";
		public static final String JOB_SERVER = "JOB_SERVER";
		public static final String CRON_JOB_SERVER = "CRON_JOB_SERVER";
		public static final String SVC = "SVC";
		public static final String CM = "CM";
		public static final String SC = "SC";
		public static final String PVC = "PVC";
		public static final String IG = "IG";
		public static final String BUILD = "BUILD";
	}

	@Getter
	private String value;

	PlResType(String value) {
		this.value = value;
	}

	public Map<String, Object> toMap(){
		Map<String, Object> toMap = new HashMap<>();
		toMap.put("code", this.getCode());
		toMap.put("value", this.getValue());

		return toMap;
	}

	public static List<Map<String, Object>> getList() {
		return Arrays.stream(PlResType.values()).map(PlResType::toMap).collect(Collectors.toList());
	}

	public boolean isWorkload(){
		return EnumSet.of(REPLICA_SERVER, STATEFUL_SET_SERVER, DAEMON_SET_SERVER, JOB_SERVER, CRON_JOB_SERVER).contains(this);
	}

	@Override
	public String getCode() {
		return this.name();
	}
}
