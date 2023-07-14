package run.acloud.api.event.constants;

public class EventConstants {
	public static final String TYPE_SERVICE_SUMMARIES = "@@service/FETCH_SERVICE_SUCCESS";
	public static final String TYPE_APPMAP_GROUP = "@@service/FETCH_APPMAP_GROUP_SUCCESS";
//	public static final String TYPE_APPMAP_SUMMARIES = "@@appmap/FETCH_APPMAP_SUCCESS";
	public static final String TYPE_APPMAP_SERVER = "@@appmap/FETCH_SERVER_SUCCESS";
	public static final String TYPE_JOB_SUCCESS = "@@job/FETCH_JOB_SUCCESS";
	public static final String TYPE_JOB_DONE = "@@job/FETCH_JOB_DONE";
	public static final String TYPE_PIPELINE_STATE = "@@pipeline/FETCH_PIPELINE_JOB";
	public static final String TYPE_CLUSTER_STATE = "@@cluster/RELOAD_CLUSTER";
}
