package run.acloud.api.resource.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import run.acloud.api.catalog.vo.ChartVO;
import run.acloud.api.monitoring.vo.ServerMonitoringVO;
import run.acloud.api.resource.enums.ConcurrencyPolicy;
import run.acloud.api.resource.enums.ResourceType;
import run.acloud.api.resource.enums.RestartPolicyType;
import run.acloud.api.resource.util.ResourceUtil;
import run.acloud.commons.vo.HasUpdaterVO;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class ServerVO extends HasUpdaterVO {
	
	private Integer componentSeq;

	private String workloadType;

	private String workloadVersion;
	
	private Integer computeTotal;
	
	private int activeCount;
	
	private String keypairName;

	private DeploymentStrategyVO strategy;

	private StatefulSetStrategyVO statefulSetStrategy;

	private DaemonSetStrategyVO daemonSetStrategy;

	private HpaGuiVO hpa;

	private RestartPolicyType restartPolicy;

	@Schema(title = "initContainers", example = "[]")
	private List<ContainerVO> initContainers;

	@Schema(title = "containers", example = "[]")
	private List<ContainerVO> containers;

	// cm db에 저장하지 않는다
	private List<ServiceSpecGuiVO> services;

	// cm db에 저장하지 않는다
	private List<PersistentVolumeClaimGuiVO> persistentVolumeClaims;

    // cm db에 저장하지 않는다
	@JsonIgnore
	private String imageSecret;
	
	private List<ServerUrlVO> serverUrls;

	// Optional duration in seconds the pod needs to terminate gracefully. May be decreased in delete request. Value must be non-negative integer. The value zero indicates delete immediately. If this value is nil, the default grace period will be used instead. The grace period is the duration in seconds after the processes running in the pod are sent a termination signal and the time when the processes are forcibly halted with a kill signal. Set this value longer than the expected cleanup time for your process. Defaults to 30 seconds.
	private Long terminationGracePeriodSeconds = 30L;

    // ServiceAccountName is the name of the ServiceAccount to use to run this pod. More info: https://kubernetes.io/docs/tasks/configure-pod-container/configure-service-account/
    private String serviceAccountName;

    private String nodeSelectorKey;

    private String nodeSelectorValue;

	private Map<String, String> nodeSelector;

    private Integer minReadySeconds = 0;

    // WorkloadType.PACKAGE_SERVER일 경우에 사용.
	@JsonIgnore
	@Deprecated
	private ChartVO chart;

    private String hostname;

    // StatefulSet
    // podManagementPolicy controls how pods are created during initial scale up, when replacing pods on nodes, or when scaling down. The default policy is `OrderedReady`, where pods are created in increasing order (pod-0, then pod-1, etc) and the controller will wait until each pod is ready before continuing. When scaling down, the pods are removed in the opposite order. The alternative policy is `Parallel` which will create pods in parallel to match the desired scale without waiting, and on scale down will delete all pods at once.
    private String podManagementPolicy;
    private String serviceName;

	// Job
    private Integer parallelism;
    private Long activeDeadlineSeconds;
    private Integer backoffLimit;
    private Integer ttlSecondsAfterFinished;

    // CronJob
    // Specifies how to treat concurrent executions of a Job. Valid values are: - "Allow" (default): allows CronJobs to run concurrently; - "Forbid": forbids concurrent runs, skipping next run if previous run hasn't finished yet; - "Replace": cancels currently running job and replaces it with a new one
    private ConcurrencyPolicy concurrencyPolicy;
    private String schedule; //The schedule in Cron format, see https://en.wikipedia.org/wiki/Cron.
    private Long startingDeadlineSeconds;
    private Integer successfulJobsHistoryLimit = 3;
    private Integer failedJobsHistoryLimit = 1;
    private Boolean suspend = false;

    private Map<String, String> labels;
    private Map<String, String> annotations;

    // Multi nic 지원을 위한 annotation(k8s.v1.cni.cncf.io/networks) value
	// [{"name": "sriov-net1"}]
    private List<Map<String, String>> podNetworks;

    private List<TolerationVO> tolerations;

	private AffinityVO affinity;

    private List<LocalObjectReferenceVO> imagePullSecrets;

	@JsonIgnore
	@Deprecated
	private Integer serverInstanceTypeSeq;

	@JsonIgnore
	@Deprecated
	private Integer loadbalancerSecurityGroupSeq;

	@JsonIgnore
	@Deprecated
	private Integer computeSecurityGroupSeq;

	@JsonIgnore
	@Deprecated
	private Integer computeImageSeq;

	@JsonIgnore
	@Deprecated
	private String loadbalancerResourceId;

	@JsonIgnore
	@Deprecated
	private Boolean externalFlag;

	@JsonIgnore
	@Deprecated
	private String loadbalancerDns;

	@JsonIgnore
	@Deprecated
	private String applicationVersion;

	@JsonIgnore
	@Deprecated
	private String serverType;

	@JsonIgnore
	@Deprecated
	public String getComponentId() {
		return ResourceUtil.makeCocktailId(ResourceType.SERVER, componentSeq);
	}

	@JsonIgnore
	@Deprecated
	private String deploymentContent;

	@JsonIgnore
    @Deprecated
    private Integer deploymentSeq;

	@JsonIgnore
	@Deprecated
	private Boolean stickySession = false;

	// timeoutSeconds specifies the seconds of ClientIP type session sticky time. The value must be >0 && <=86400(for 1 day) if ServiceAffinity == \"ClientIP\". Default value is 10800(for 3 hours).
	@JsonIgnore
	@Deprecated
	private Integer stickySessionTimeoutSeconds;

	@JsonIgnore
	@Deprecated
	public Integer getStickySessionTimeoutSeconds(){
		if(this.getStickySession() == null
				|| (this.getStickySession() != null && !this.getStickySession().booleanValue())){
			return null;
		}

		return this.stickySessionTimeoutSeconds;
	}

	@JsonIgnore
	@Deprecated
	private ServerMonitoringVO serverMonitoring;

}