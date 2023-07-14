package run.acloud.api.event.enums;

import lombok.Getter;

public enum ClientAction {
	
	GET_SERVICE_SUMMARIES("@@service/FETCH_SERVICESUMMARY_SUCCESS", "/topic/services"),
	GET_APPMAP_SUMMARIES("@@appmap/FETCH_APPMAP_SUCCESS", "/topic/services"),
	GET_APPMAP_SERVERS("@@appmap/FETCH_SERVER_SUCCESS", "/topic/services/appmap/%d/map"),
	GET_APPMAP_DEPLOY_JOB("@@job/FETCH_JOB_SUCCESS", "/topic/services/appmap/%d/job"),
	GET_APPMAP_BUILD_LOG("@@build/FETCH_BUILDTASKLOG_SUCCESS", "/topic/task/build/%d/log"),
	GET_APPMAP_BUILD_RECEIVE("@@build/APPEND_BUILDTASKLOG_RECEIVE", "/topic/task/build/%d/log"),
	GET_APPMAP_SERVERS_BUILDLIST("@@build/FETCH_BUILDLIST_SUCCESS", "/topic/services/%d/buildList"),
	GET_APPMAP_SERVERS_BUILDEDIT("@@build/FETCH_BUILD_SUCCESS", "/topic/task/%d/buildEdit")
	;


	@Getter
	private String actionType;
	
	@Getter
	private String destinationFormat;

	ClientAction(String actionType, String destinationFormat) {
		this.actionType = actionType;
		this.destinationFormat = destinationFormat;
	}

	public String getDestination(Object... args) {
		return String.format(this.destinationFormat, args);
	}
}
