package run.acloud.api.pipelineflow.constants;

public class PipelineFlowConstant {
    public static String PIPELINE_CALLBACK_URL = "http://%s:%s/api/pipelineflow/listener?user-id=%d&user-role=%s&user-workspace=%d";
    public static String BUILD_RESULT_RECEIVE_URL = "http://%s:%s/api/build/run/result/{buildRunSeq}";
    public static String PIPELINE_RESULT_RECEIVE_URL = "http://%s:%s/api/pipelineflow/run/result/{pipelineContainerSeq}";
    public static String CLUSTER_INFO_GET_URL = "http://%s:%s//api/cluster/pipeline/{clusterSeq}/{token}";
    public static String PL_RESULT_RECEIVE_URL = "http://%s:%s/api/pl/{plSeq}/run/{plRunSeq}/build/{plRunBuildSeq}/result";

}

