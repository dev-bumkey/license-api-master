package run.acloud.commons.enums;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum AccessibleResources implements EnumCode {
	ACCOUNT("accountSeq", false)
	,SERVICE("serviceSeq", true)
	,SERVICES("serviceSeqs", true)
	,SERVICEMAP("servicemapSeq", false)
	,NAMESPACE("namespaceName", false)
	,CLUSTER("clusterSeq", false)
	,CLUSTERID("clusterId", false)
//	,VOLUMES("volumeSeq", false)
//	,GROUP("groupSeq", true)
//	,COMPONENT("componentSeq", true)
//	,JOB("jobSeq", true)
//	,TASK("taskSeq", true)
//	,BUILD_JOB("build_jobSeq", true)
//	,BUILD_TASK("build_taskSeq", true)
	,USER("userSeq", false)
	,TEMPLATE("templateSeq", false)
	,PIPELINE_WORKLOAD("pipelineWorkloadSeq", true)
	,PIPELINE_CONTAINER("pipelineContainerSeq", true)
	,BUILD("buildSeq", false)
	,BUILD_RUN("buildRunSeq", false)
	,BUILD_STEP_RUN("buildStepRunSeq", false)
	;

	private String resourceKey;
	private boolean mustHaveService;

	AccessibleResources(String resourceKey, boolean mustHaveService) {
		this.resourceKey = resourceKey;
		this.mustHaveService = mustHaveService;
	}

	public String getResourceKey() {
		return this.resourceKey;
	}

	public boolean getMustHaveService() {
		return this.mustHaveService;
	}

	public static List<String> getResourceKeyList(){
		return Arrays.asList(AccessibleResources.values()).stream().map(s -> s.getResourceKey()).collect(Collectors.toList());
	}

	@Override
	public String getCode() {
		return this.name();
	}
}
