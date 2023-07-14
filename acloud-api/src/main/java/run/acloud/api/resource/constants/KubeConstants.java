package run.acloud.api.resource.constants;

public class KubeConstants {
	public static final int CONST_PROGRESSDEADLINESECONDS = 60;
	public static final int CONST_ACTIVEDEADLINESECONDS = 60;

    public static final String DEFAULT_NAMESPACE = "default";
	public static final String DEFAULT_SERVICE_ACCOUNT = "default";
    public static final String KUBE_SYSTEM_NAMESPACE = "kube-system";
	public static final String PIPELINE_SERVER_NAME = "pipeline-server";
	public static final String COCKTAIL_ADDON_NAMESPACE = "cocktail-addon";
	public static final String ISTIO_SYSTEM_NAMESPACE = "istio-system";
	public static final String KIALI_USER_SECRET_NAME = "kiali-user";
	public static final String ADDON_NAMESPACE_KEY = "releaseNamespace";
	public static final String ADDON_MANIFEST_KEY = "manifest";

    /*
     * Addon Status
     */
    public static final String ADDON_STATUS_UNKNOWN = "unknown";
    public static final String ADDON_STATUS_DEPLOYED = "deployed";
    public static final String ADDON_STATUS_UNINSTALLED = "uninstalled";
    public static final String ADDON_STATUS_SUPERSEDED = "superseded";
    public static final String ADDON_STATUS_FAILED = "failed";
    public static final String ADDON_STATUS_UNINSTALLING = "uninstalling";
    public static final String ADDON_STATUS_PENDING_INSTALL = "pending-install";
	public static final String ADDON_STATUS_PENDING_UPGRADE = "pending-upgrade";
	public static final String ADDON_STATUS_PENDING_ROLLBACK = "pending-rollback";

    /*
	 * Monitoring 
	 */
	public static final String MONITORING_DB = "k8s";

	public static final String DEFAULT_K8S_VERSION = "1.8.6";

	/*
	 * Kubernetes Specs
	 */
	public static final String VERSION = "v1";
	public static final String VERSION_CORE_V1 = "v1";
    public static final String VERSION_APPS_V1BETA = "apps/v1beta1";
    public static final String VERSION_APPS_V1 = "apps/v1";
	public static final String VERSION_EXT = "extensions/v1beta1";
	public static final String VERSION_EXTENSIONS_V1BETA1 = "extensions/v1beta1";
	public static final String VERSION_AUTOSCALING_V1 = "autoscaling/v1";
	public static final String VERSION_AUTOSCALING_V2ALPHA1 = "autoscaling/v2alpha1";
	public static final String VERSION_AUTOSCALING_V2BETA1 = "autoscaling/v2beta1";

	/* Basic Info */
	public static final String APIVSERION = "apiVersion";
	public static final String KIND = "kind";
	public static final String NAME = "name";
	public static final String TYPE = "type";
	public static final String VALUE = "value";
    public static final String KIND_PERSISTENT_VOLUME = "PersistentVolume";
    public static final String KIND_PERSISTENT_VOLUME_CLAIM = "PersistentVolumeClaim";
    public static final String KIND_INGRESS = "Ingress";
    public static final String KIND_STORAGE_CLASS = "StorageClass";

	/* Metadata */
	public static final String META = "metadata";
	public static final String META_NAMESPACE = "namespace";
	public static final String META_LABELS = "labels";
	public static final String META_LABELS_APP_NAME = "app.kubernetes.io/name";
	public static final String META_LABELS_APP_INSTANCE = "app.kubernetes.io/instance";
	public static final String META_LABELS_APP_VERSION = "app.kubernetes.io/version";
	public static final String META_LABELS_APP_COMPONENT = "app.kubernetes.io/component";
	public static final String META_LABELS_APP_PART_OF = "app.kubernetes.io/part-of";
	public static final String META_LABELS_APP_MANAGED_BY = "app.kubernetes.io/managed-by";
	public static final String META_LABELS_META_NAME = "kubernetes.io/metadata.name";
	public static final String META_ANNOTATIONS = "annotations";
	public static final String META_ANNOTATIONS_DESIRED_REPLICAS = "deployment.kubernetes.io/desired-replicas";
	public static final String META_ANNOTATIONS_DEPLOYMENT_REVISION = "deployment.kubernetes.io/revision";
	public static final String META_ANNOTATIONS_DEPRECATED_DAEMONSET_TEMPLATE_GENERATION = "deprecated.daemonset.template.generation";
	public static final String META_ANNOTATIONS_LAST_APPLIED_CONFIGURATION = "kubectl.kubernetes.io/last-applied-configuration";
	public static final String META_ANNOTATIONS_INGRESSCLASS = "kubernetes.io/ingress.class";
	public static final String META_ANNOTATIONS_INGRESSCLASS_NGINX_DEFAULT_VALUE = "nginx";
	public static final String META_ANNOTATIONS_INGRESSSSLREDIRECT = "nginx.ingress.kubernetes.io/ssl-redirect";
    public static final String META_ANNOTATIONS_REWRITE_TARGET = "nginx.ingress.kubernetes.io/rewrite-target";
	public static final String META_ANNOTATIONS_AWS_INTERNAL_LOADBALANCER_KEY = "service.beta.kubernetes.io/aws-load-balancer-internal";
	public static final String META_ANNOTATIONS_AWS_INTERNAL_LOADBALANCER_VALUE = "0.0.0.0/0";
	public static final String META_ANNOTATIONS_AWS_INTERNAL_LOADBALANCER_VALUE_AFTER_1_15 = "true";
	public static final String META_ANNOTATIONS_GCP_INTERNAL_LOADBALANCER_KEY = "cloud.google.com/load-balancer-type";
	public static final String META_ANNOTATIONS_GCP_INTERNAL_LOADBALANCER_VALUE = "Internal";
	public static final String META_ANNOTATIONS_AZR_INTERNAL_LOADBALANCER_KEY = "service.beta.kubernetes.io/azure-load-balancer-internal";
	public static final String META_ANNOTATIONS_AZR_INTERNAL_LOADBALANCER_VALUE = "true";
	public static final String META_ANNOTATIONS_NCP_INTERNAL_LOADBALANCER_KEY = "service.beta.kubernetes.io/ncloud-load-balancer-internal";
	public static final String META_ANNOTATIONS_NCP_INTERNAL_LOADBALANCER_VALUE = "true";
	public static final String META_ANNOTATIONS_BETA_STORAGE_CLASS_IS_DEFAULT = "storageclass.beta.kubernetes.io/is-default-class";
	public static final String META_ANNOTATIONS_RELEASE_STORAGE_CLASS_IS_DEFAULT = "storageclass.kubernetes.io/is-default-class";
	public static final String META_ANNOTATIONS_CNI_RESOURCE_NAME = "k8s.v1.cni.cncf.io/resourceName";
	public static final String META_ANNOTATIONS_CNI_NETWORKS = "k8s.v1.cni.cncf.io/networks";
	public static final String META_ANNOTATIONS_RESTARTED_AT = "kubectl.kubernetes.io/restartedAt";
	public static final String META_ANNOTATIONS_DESCRIPTION = "kubernetes.io/description";


	/* Spec */
	public static final String SPEC = "spec";
	public static final String SPEC_REPLICAS = "replicas";
    public static final String SPEC_STRATEGY = "strategy";
	public static final String SPEC_STRATEGY_RECREATE = "Recreate";
	public static final String SPEC_STRATEGY_ROLLINGUPDATE = "RollingUpdate";
	public static final String SPEC_ROLLINGUPDATE = "rollingUpdate";
	public static final String SPEC_MAXUNAVAILABLE = "maxUnavailable";
	public static final String SPEC_MAXSURGE = "maxSurge";
	public static final String SPEC_REVISIONHISTORYLIMIT = "revisionHistoryLimit";
	public static final String SPEC_PROGRESSDEADLINESECONDS  = "progressDeadlineSeconds";
	public static final String SPEC_ACTIVEDEADLINESECONDS  = "activeDeadlineSeconds";
    public static final String SPEC_IMAGE_SECRET = "imagePullSecrets";
    public static final String SPEC_SERVICE_NAME = "serviceName";
    public static final String SPEC_CLUSTER_IP = "clusterIP";
	public static final String SPEC_TERMINATION_SECONDS = "terminationGracePeriodSeconds";
	public static final String SPEC_NODE_NAME = "nodeName";
	public static final String SPEC_TYPE = "type";
	public static final String SPEC_TYPE_VALUE_CLUSTER_IP = "ClusterIP";
	public static final String SPEC_TYPE_VALUE_LOADBALANCER = "LoadBalancer";
	public static final String SPEC_TYPE_VALUE_NODE_PORT = "NodePort";
	public static final String SPEC_TYPE_VALUE_EXTERNAL_NAME = "ExternalName";

    public static final String SPEC_HEADLESS_POSTFIX = "headless";

	/* Template */
	public static final String TEMPLATE = "template";
	
	/* Labels */
	public static final String LABELS_KEY = "app";
	public static final String LABELS_COCKTAIL_KEY = "cocktail";
	public static final String LABELS_COCKTAIL_WORKLOAD_NAME = "cocktail-workload-name";
	public static final String LABELS_COCKTAIL_WORKLOAD_CONTROLLER = "cocktail-workload-controller";
	public static final String LABELS_COCKTAIL_SERVICE_TYPE = "cocktail-service-type";
	public static final String LABELS_POD_TEMPLATE_HASH_KEY = "pod-template-hash";
    public static final String LABELS_SECRET = "secret-type";
    public static final String LABELS_CLUSTERID = "clusterid";
    public static final String LABELS_VALUE_STORAGE = "storage";
    public static final String LABELS_ACORNSOFT_STORAGE_CLASS_VOLUME_TYPE = "acornsoft.io/volume-type";
    public static final String LABELS_ACORNSOFT_STORAGE_CLASS_STORAGE_TYPE = "acornsoft.io/type";
	public static final String LABELS_ACORNSOFT_STORAGE_CLASS_PROVISIONER_TYPE = "acornsoft.io/provisioner-type";
	public static final String LABELS_ACORNSOFT_STORAGE_CLASS_TOTAL_CAPACITY = "acornsoft.io/total-capacity";
	public static final String LABELS_PACKAGE_RELEASE_KEY = "release";
	public static final String LABELS_WORKLOAD_TYPE = "workload-type";
	public static final String LABELS_WORKLOAD_VERSION = "workload-version";
	public static final String LABELS_HELM_CHART_KEY = "release-id";
	public static final String LABELS_HELM_CHART_VALUE_INGRESS = "nginx-ingress";
	public static final String LABELS_ADDON_AGENT_KEY = "agent";
	public static final String LABELS_ADDON_CHART_KEY = "chart";
	public static final String LABELS_ADDON_CHART_VALUE_COCKTAIL = "cocktail";
	public static final String LABELS_ADDON_INSTANCE_KEY = "app.kubernetes.io/instance";
	public static final String LABELS_ADDON_NAME_KEY = "app.kubernetes.io/name";
	public static final String LABELS_ADDON_UPDATE_AT = "update_at";
	public static final String LABELS_ADDON_STATUS = "status";
    public static final String LABELS_CRD_NET_ATTACH_DEF = "acornsoft.io/cni-type";
    public static final String LABELS_ISTIO_INJECTION_KEY = "istio-injection";
    public static final String LABELS_ISTIO_INJECTION_VALUE_ENABLED = "enabled";
    public static final String LABELS_ISTIO_INJECTION_VALUE_DISABLED = "disabled";
	public static final String LABELS_ACORNSOFT_CLUSTER_ROLE = "acornsoft.io/cluster-role";
	public static final String LABELS_ACORNSOFT_INGRESS_URL = "acornsoft.io/ingress-url";
	public static final String LABELS_ACORNSOFT_SYSTEM_RESOURCE = "acornsoft.io/system-resource";
	public static final String LABELS_ACORNSOFT_PSP_RESOURCE = "acornsoft.io/psp-resource";
	public static final String LABELS_ACORNSOFT_PSP_DISPLAY_DEFALUT = "acornsoft.io/psp-display-default";
	// Serverless labels
	public static final String LABELS_ACORNSOFT_SERVERLESS_TYPE = "serverless.acornsoft.io/type";
	public static final String LABELS_ACORNSOFT_SERVERLESS_USERID = "serverless.acornsoft.io/userid";


	// Dynamic Labeling (From Controller)
	public static final String LABELS_CLUSTERSEQ = "clusterseq";
	public static final String LABELS_DYNAMIC_KEY = "@DynamicLabel";
	public static final String LABELS_DYNAMIC_VALUE = "LABEL_TYPE_CALLAPI";
	public static final String LABELS_DYNAMIC_LABEL_KIND = "dynamic-label-kind";
	public static final String LABELS_DYNAMIC_LABEL_VALUE = "dynamic-label-value";
	public static final String LABELS_DYNAMIC_LABEL_ACCESS_MODES = "dynamic-label-accessmodes";

	/* Containers */
	public static final String CONTAINERS = "containers";
    public static final String INIT_CONTAINERS = "initContainers";
	
	/* Container - sub item */
	public static final String CONTAINER_IMAGE = "image";
	public static final String CONTAINER_COMMAND = "command";
	public static final String CONTAINER_ARGS = "args";
	
	/* Container - Envs */
	public static final String CONTAINER_ENVS = "env";
	public static final String CONTAINER_ENVS_PROVISIONER_NAME = "PROVISIONER_NAME";

	/* Container - Ports */
	public static final String PORTS = "ports";
	
	/* Container - Port */
	public static final String PORT_PORT = "port";
	public static final String PORT_HOSTPORT = "hostPort";
	public static final String PORT_CONTAINERPORT = "containerPort";
	public static final String PORT_TARGETPORT = "targetPort";
	
	/* Container - Volumes */
	public static final String CONTAINER_VOLUME_MOUNTS = "volumeMounts";
	public static final String CONTAINER_MOUNTPATH = "mountPath";
	public static final String CONTAINER_SUBPATH = "subPath";

	/* Volumes */
	public static final String VOLUMES = "volumes";
	public static final String VOLUMES_HOSTPATH = "hostPath";
	public static final String VOLUMES_PATH = "path";
	public static final String VOLUMES_EMPTYDIR = "emptyDir";
    public static final String VOLUMES_CONFIG_MAP = "configMap";
	public static final String VOLUMES_MEDIUM = "medium";			
	public static final String VOLUMES_CLAILM = "persistentVolumeClaim";
    public static final String VOLUMES_CLAILM_NAME = "claimName";
    public static final String VOLUMES_ACCESS_MODES = "accessModes";
    public static final String VOLUMES_NAME = "volumeName";
    public static final String VOLUMES_CAPACITY = "capacity";
    public static final String VOLUMES_STORAGE = "storage";
    public static final String VOLUMES_RECLAIM_POLICY = "persistentVolumeReclaimPolicy";
    public static final String VOLUMES_CLASS_NAME = "storageClassName";
    public static final String VOLUMES_CLASS_NAME_BETA = "volume.beta.kubernetes.io/storage-class";
    public static final String VOLUMES_PLUGINS_NFS = "nfs";
    public static final String VOLUMES_PROVISONER = "provisioner";
    public static final String VOLUMES_PARAMETERS = "parameters";
//  public static final String VOLUMES_AWS_EBS = "kubernetes.io/aws-ebs";
//	public static final String VOLUMES_AWS_EBS_CSI = "ebs.csi.aws.com";
//	public static final String VOLUMES_AWS_EFS_CSI = "efs.csi.aws.com";
//	public static final String VOLUMES_GCE_PD = "kubernetes.io/gce-pd";
//	public static final String VOLUMES_GCE_PD_CSI = "pd.csi.storage.gke.io";
//	public static final String VOLUMES_AZURE_DISK = "kubernetes.io/azure-disk";
//	public static final String VOLUMES_AZURE_FILE = "kubernetes.io/azure-file";
//	public static final String VOLUMES_AZURE_DISK_CSI = "disk.csi.azure.com";
//	public static final String VOLUMES_AZURE_FILE_CSI = "file.csi.azure.com";
//	public static final String VOLUMES_VSPHERE_VOLUME = "kubernetes.io/vsphere-volume";
//	public static final String VOLUMES_VSPHERE_VOLUME_CSI = "csi.vsphere.vmware.com";
//	public static final String VOLUMES_NFS = "cocktail.io/cocktail-nfs";
//	public static final String VOLUMES_NFS_CSI = "nfs.csi.k8s.io";
    public static final String VOLUMES_CLAILM_TEMPLATES = "volumeClaimTemplates";
    public static final String VOLUMES_READ_ONLY = "readOnly";

    /* Selector */
	public static final String SELECTOR = "selector";
	
	/* ContextSecurity */
	public static final String SECURITYCONTEXT = "securityContext";
	public static final String SECURITYCONTEXT_RUNASNONROOT = "runAsNonRoot";
	public static final String SECURITYCONTEXT_RUNASUSER = "runAsUser";
	public static final String SECURITYCONTEXT_SELINUXOPTIONS = "seLinuxOptions";
	public static final String SECURITYCONTEXT_CAPABILITIES = "capabilities";
	public static final String SECURITYCONTEXT_PRIVILEGED = "privileged";
	public static final String SECURITYCONTEXT_READONLYROOTFILESYSTEM = "readOnlyRootFilesystem";

	/* RBAC */
	public static final String RBAC_GROUP_SYSTEM_AUTHENTICATED = "system:authenticated";

	/* SE Linux Options */
	public static final String SELINUXOPTIONS_LEVEL = "level";
	public static final String SELINUXOPTIONS_ROLE = "role";
	public static final String SELINUXOPTIONS_USER = "user";

	/* Capabilities */
	public static final String CAPABILITIES_ADD = "add";
	public static final String CAPABILITIES_DROP = "drop";

	/* Resources */
	public static final String RESOURCES = "resources";
	public static final String RESOURCES_LIMIT = "limits";
	public static final String RESOURCES_REQUESTS = "requests";
	public static final String RESOURCES_CPU = "cpu";
	public static final String RESOURCES_MEMORY = "memory";
	public static final String RESOURCES_GPU = "nvidia.com/gpu";
    public static final String RESOURCES_STORAGE = "storage";
    public static final String RESOURCES_PODS = "storage";
    public static final String RESOURCES_EPHEMERAL_STORAGE = "ephemeral-storage";

	/* Rules */
	public static final String RULES = "rules";
	public static final String RULES_HTTP = "http";
	public static final String HTTP_PATHS = "paths";
	public static final String PATHS_PATH = "path";
	public static final String PATHS_BACKEND = "backend";
	public static final String BACKEND_SERVICENAME = "serviceName";
	public static final String BACKEND_SERVICEPORT = "servicePort";

    public static final String DATA = "data";
    public static final String DATA_USERNAME = "username";
    public static final String DATA_PASSWORD = "password";

    /* Liveness Probe */
    public static final String LIVENESSPROBE = "livenessProbe";
    /* Readness Probe */
	public static final String READINESSPROBE = "readinessProbe";
	/* Probe */
	public static final String PROBE_INITIALDELAY_SECONDS = "initialDelaySeconds";
	public static final String PROBE_PERIOD_SECONDS = "periodSeconds";
	public static final String PROBE_TIMEOUT_SECONDS = "timeoutSeconds";
	public static final String PROBE_SUCCESS_THRESHOLD = "successThreshold";
	public static final String PROBE_FAILURE_THRESHOLD = "failureThreshold";

	public static final String PROBE_EXEC = "exec";
	public static final String PROBE_HTTPGET = "httpGet";
	public static final String PROBE_TCPSOCKET = "tcpSocket";

	public static final String PROBE_SCHEME = "scheme";
	public static final String PROBE_HOST = "host";
	public static final String PROBE_PORT = "port";
	public static final String PROBE_PATH = "path";
	public static final String PROBE_HTTPHEADERS = "httpHeaders";

	/* Taint */
	public static final String TAINT_EFFECT_NO_SCHEDULE = "NoSchedule";
	public static final String TAINT_EFFECT_PREFER_NO_SCHEDULE = "PreferNoSchedule";
	public static final String TAINT_EFFECT_NO_EXECUTE = "NoExecute";

	/* Toleration */
	public static final String TOLERATION_OPERATOR_EXISTS = "Exists";
	public static final String TOLERATION_OPERATOR_EQUAL = "Equal";
	public static final String TOLERATION_EFFECT_NO_SCHEDULE = "NoSchedule";
	public static final String TOLERATION_EFFECT_PREFER_NO_SCHEDULE = "PreferNoSchedule";
	public static final String TOLERATION_EFFECT_NO_EXECUTE = "NoExecute";

	/* Node Affinity */
	public static final String NODE_AFFINITY_OPERATOR_IN = "In";
	public static final String NODE_AFFINITY_OPERATOR_NOT_IN = "NotIn";
	public static final String NODE_AFFINITY_OPERATOR_EXISTS = "Exists";
	public static final String NODE_AFFINITY_OPERATOR_DOES_NOT_EXIST = "DoesNotExist";
	public static final String NODE_AFFINITY_OPERATOR_GT = "Gt";
	public static final String NODE_AFFINITY_OPERATOR_LT = "Lt";

	/* Horizontal TotalPod Scaling v1 */
	public static final String HPA_TYPE_CPU = "cpu";
	public static final String HPA_TYPE_METRIC = "metric";
	public static final String HPA_TYPE_NONE = "none";
	public static final String HPA_MAX_REPLICAS = "maxReplicas";
	public static final String HPA_MIN_REPLICAS = "minReplicas";
	public static final String HPA_SCALE_TARGET_REF = "scaleTargetRef";
	public static final String HPA_TARGET_CPU_UTILIZATION_PERCENTAGE = "targetCPUUtilizationPercentage";

	/* Horizontal TotalPod Scaling v2alpha1 */
	public static final String HPA_METRICS = "metrics";
	public static final String HPA_METRIC_NAME = "metricName";
	public static final String HPA_TARGET_VALUE = "targetValue";
	public static final String HPA_TARGET_AVERAGE_VALUE = "targetAverageValue";
	public static final String HPA_TARGET_AVERAGE_UTILIZATION = "targetAverageUtilization";

	/* Restart Policy */
	public static final String RESTART_POLICY = "restartPolicy";

	/* Secret */
    public static final String SECRET_TYPE_OPAQUE = "Opaque";
    public static final String SECRET_TYPE_DOCKERCFG = "kubernetes.io/dockercfg";
    public static final String SECRET_TYPE_DOCKERCONFIGJSON = "kubernetes.io/dockerconfigjson";
    public static final String SECRET_TYPE_DOCKERCONFIGJSON_KEY = ".dockerconfigjson";
    public static final String SECRET_TYPE_SERVICE_ACCOUNT_TOKEN = "kubernetes.io/service-account-token";
    public static final String SECRET_TYPE_BASIC_AUTH = "kubernetes.io/basic-auth";
    public static final String SECRET_TYPE_SSH_AUTH = "kubernetes.io/ssh-auth";
    public static final String SECRET_TYPE_TLS = "kubernetes.io/tls";
    public static final String SECRET_TYPE_BOOTSTRAP_TOKEN = "bootstrap.kubernetes.io/token";

    /* Regular expression */
	public static final String RULE_RESOURCE_NAME = "[a-z0-9]([-a-z0-9]*[a-z0-9])?(\\.[a-z0-9]([-a-z0-9]*[a-z0-9])?)*";
    public static final String RULE_CONFIGMAP_NAME = "[-._a-zA-Z0-9]+";
    public static final String RULE_SERVICE_NAME = "[a-z]([-a-z0-9]*[a-z0-9])?";
    public static final String RULE_SECRET_NAME = "[-._a-zA-Z0-9]+";
    public static final String RULE_LABEL_KEY = "([A-Za-z0-9][-A-Za-z0-9_.]*)?[A-Za-z0-9]";
//    public static final String RULE_LABEL_VALUE = "([A-Za-z0-9][-A-Za-z0-9_.]*)?[A-Za-z0-9]";
    public static final String RULE_LABEL_VALUE = "(([A-Za-z0-9][-A-Za-z0-9_.]*)?[A-Za-z0-9])?";

    /* Annotation */
    public static final String ANNOTATION_COCKTAIL_USER_DESCRIPTION = "cocktail-user-description";
    public static final String ANNOTATION_COCKTAIL_DEPLOY_DATETIME = "cocktail-deploy-datetime";
	public static final String ANNOTATION_COCKTAIL_GROUP_NO = "acornsoft.io/workload-group-no";
//	public static final String OLD_ANNOTATION_COCKTAIL_GROUP_NO = "cocktailcloud.io/workload-group-no";

    /* Deployment Conditions */
    public static final String DEPLOYMENT_CONDITION_AVAILABLE = "Available";
    public static final String DEPLOYMENT_CONDITION_PROGRESSING = "Progressing";
    public static final String DEPLOYMENT_CONDITION_FAILURE = "Failure";

    /* Job Conditions */
    public static final String JOB_CONDITION_COMPLETE = "Complete";
    public static final String JOB_CONDITION_FAILED = "Failed";

    /* TotalPod Status */
    public static final String POD_STATUS_RUNNING = "Running";
    public static final String POD_STATUS_WAITING = "Waiting";
    public static final String POD_STATUS_SUCCEEDED = "Succeeded";
    public static final String POD_STATUS_FAILED = "Failed";


    /* Persistent Volume label */
	public static final String CUSTOM_VOLUME_STORAGE = "VOLUME_STORAGE";
	public static final String CUSTOM_VOLUME_TYPE = "VOLUME_TYPE";
	public static final String CUSTOM_PERSISTENT_VOLUME_TYPE = "PERSISTENT_VOLUME_TYPE";

	/* Ingress */
	public static final String CUSTOM_INGRESS_HTTP_NON_HOST = "COCKTAIL_NON_HOST";

	/* Static Storaage ConfigMap dtat key */
	public static final String CUSTOM_STATIC_STORAGE_CONFIGMAP_PREFIX_NAME = "static-storage";
	public static final String CUSTOM_STATIC_STORAGE_CONFIGMAP_DATA_KEY_RECLAIM_POLICY = "reclaim_policy";
	public static final String CUSTOM_STATIC_STORAGE_CONFIGMAP_DATA_KEY_SERVER = "server";
	public static final String CUSTOM_STATIC_STORAGE_CONFIGMAP_DATA_KEY_PATH = "path";

	/* For AWS EKS */
	public static final String AWS_CONFIGMAP_NAME_AWS_AUTH = "aws-auth";
	public static final String AWS_CONFIGMAP_KEY_MAP_USERS = "mapUsers";

	/* Pod Security Policy */
	public static final String POD_SECURITY_POLICY_RESOURCES = "podsecuritypolicies";
	public static final String POD_SECURITY_POLICY_SHORT_NAME = "psp";
	public static final String POD_SECURITY_POLICY_ANNOTATIONS_ALLOWED_PROFILE_NAMES = "seccomp.security.alpha.kubernetes.io/allowedProfileNames";
	public static final String POD_SECURITY_POLICY_DEFAULT_NAME = "default-psp";
	public static final String POD_SECURITY_POLICY_CLUSTER_ROLE_DEFAULT_NAME = "cocktail:podsecuritypolicy:privileged";
	public static final String POD_SECURITY_POLICY_CLUSTER_ROLE_BINDING_DEFAULT_NAME = "cocktail:podsecuritypolicy:authenticated";
	public static final String POD_SECURITY_POLICY_EKS_DEFAULT_NAME = "eks.privileged";
	public static final String POD_SECURITY_POLICY_EKS_CLUSTER_ROLE_DEFAULT_NAME = "eks:podsecuritypolicy:privileged";
	public static final String POD_SECURITY_POLICY_EKS_CLUSTER_ROLE_BINDING_DEFAULT_NAME = "eks:podsecuritypolicy:authenticated";

	/* Network Policy */
	public static final String NETWORK_POLICY_DEFAULT_NAME = "default-network-policy";

	/* Limit Range */
	public static final String LIMIT_RANGE_DEFAULT_NAME = "default-limit-range";

	/* Resource Quota */
	public static final String RESOURCE_QUOTA_DEFAULT_NAME = "default-resource-quota";
}
