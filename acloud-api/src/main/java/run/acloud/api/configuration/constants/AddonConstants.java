package run.acloud.api.configuration.constants;

public class AddonConstants {

    public static final String ADDON_LIST_CONFIGMAP_NAME = "addon-list";

    public static final String CHART_NAME_MULTI_NIC = "multi-nic";
    public static final String CHART_NAME_SR_IOV = "sr-iov";
    public static final String CHART_NAME_GPU = "gpu";
    public static final String CHART_NAME_ADDON_MANAGER = "addon-manager";

    public static final String CHART_REPO_URL = "CHART_REPO_URL";
    public static final String CHART_REPO_PROJECT_NAME = "CHART_REPO_PROJECT_NAME";
    public static final String CHART_REPO_USER = "CHART_REPO_USER";
    public static final String CHART_REPO_PASSWORD = "CHART_REPO_PASSWORD";

    public static final String ADDON_CUSTOM_VALUE_PREFIX = "#{";
    public static final String ADDON_CUSTOM_VALUE_SUFFIX = "}";
    public static final String ADDON_VALUE_GLOBAL_DYNAMIC_CLUSTERID = "#{global.dynamic.clusterid}";
    // monitoring addon의 value.yaml 파일 설정시.. EKS/GKE/AKS일때만 포함되어야 하는 값..
    // 해당 필드 값이 true일 경우에만 환경변수의 "MONITORING_CM_KAAS_ONLY_DATA_SET" 값을 읽어서 설정함...
    public static final String MONITORING_CM_KAAS_ONLY_DATA = "kubelet.serviceMonitor.cAdvisorRelabelings.sourceLabels";

    public static final String ADDON_CONFIG_COMMON = "common";
    public static final String ADDON_CONFIG_COMMON_MAX_INSTALLATION = "maxInstallation";
    public static final String ADDON_CONFIG_COMMON_INSTALLATION_STEP = "installationStep";
    public static final String ADDON_CONFIG_COMMON_INSTALLATION_TYPE_CONFIGMAP = "configmap";
    public static final String ADDON_CONFIG_COMMON_INSTALLATION_TYPE_CHART = "chart";
    public static final String ADDON_CONFIG_COMMON_FIXED_RELEASE_NAME = "fixedReleaseName";
    public static final String ADDON_CONFIG_CONFIGMAP = "configmap";
    public static final String ADDON_CONFIG_CONFIGMAP_DATA = "data";
    public static final String ADDON_CONFIG_CONFIGMAP_LABELS = "labels";
    public static final String ADDON_CONFIG_DEPENDENCY = "dependency";
    public static final String ADDON_CONFIG_DEPENDENCY_PREINSTALLATION = "preinstallation";
    public static final String ADDON_CONFIG_DEPENDENCY_POSTINSTALLATION = "postinstallation";

    public static final String ADDON_VALUE_RELEASE = "release";
    public static final String ADDON_VALUE_RELEASE_NAME = "releaseName";
    public static final String ADDON_VALUE_SETUP = "setup";
    public static final String ADDON_VALUE_DETAIL = "detail";
    public static final String ADDON_VALUE_SPECIFIC = "specific";
    public static final String ADDON_VALUE_ENABLECONDATIONS = "enableConditions";
    public static final String ADDON_VALUE_VALIDATIONS = "validations";
    public static final String ADDON_VALUE_TITLE = "title";
    public static final String ADDON_VALUE_INDENT_CHAR = " ";

    public static final String ADDON_DATA_REGISTERED_COCKTAILS = "registeredCocktails";
}
