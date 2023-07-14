package run.acloud.api.resource.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Maps;
import com.google.gson.annotations.SerializedName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * @author: wschoi@acornsoft.io
 * Created on 2017. 10. 12.
 */
@Getter
@Setter
@EqualsAndHashCode
@Schema(name = "PodSecurityPolicyGuiVO"
        , title = "PodSecurityPolicyGuiVO"
        , allOf = {PodSecurityPolicyIntegrateVO.class}
        , description = "GUI 배포 모델"
)
public class PodSecurityPolicyGuiVO extends PodSecurityPolicyIntegrateVO{

    @Schema(title = "name")
    private String name;

    @Schema(title = "newName", description = "변경할 이름")
    @JsonIgnore
    @Deprecated
    private String newName;

    @Schema(title = "description")
    private String description;

    @Schema(title = "displayDefault", allowableValues = {"true","false"})
    private boolean isDisplayDefault = false;

    @Schema(title = "생성시간")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private OffsetDateTime creationTimestamp;

    @Schema(title = "labels")
    private Map<String, String> labels;

    public PodSecurityPolicyGuiVO putLabelsItem(String key, String labelsItem) {
        if (this.labels == null) {
            this.labels = Maps.newHashMap();
        }
        this.labels.put(key, labelsItem);
        return this;
    }

    @Schema(title = "annotations")
    private Map<String, String> annotations;

    public PodSecurityPolicyGuiVO putAnnotationsItem(String key, String annotationsItem) {
        if (this.annotations == null) {
            this.annotations = Maps.newHashMap();
        }
        this.annotations.put(key, annotationsItem);
        return this;
    }

    public static final String SERIALIZED_NAME_ALLOW_PRIVILEGE_ESCALATION = "allowPrivilegeEscalation";
    @Schema(title = SERIALIZED_NAME_ALLOW_PRIVILEGE_ESCALATION, description = "allowPrivilegeEscalation determines if a pod can request to allow privilege escalation. If unspecified, defaults to true.")
    @SerializedName(SERIALIZED_NAME_ALLOW_PRIVILEGE_ESCALATION)
    private Boolean allowPrivilegeEscalation;

    public static final String SERIALIZED_NAME_ALLOWED_C_S_I_DRIVERS = "allowedCSIDrivers";
    @Schema(title = SERIALIZED_NAME_ALLOWED_C_S_I_DRIVERS, description = "AllowedCSIDrivers is a whitelist of inline CSI drivers that must be explicitly set to be embedded within a pod spec. An empty value indicates that any CSI driver can be used for inline ephemeral volumes. This is an alpha field, and is only honored if the API server enables the CSIInlineVolume feature gate.")
    @SerializedName(SERIALIZED_NAME_ALLOWED_C_S_I_DRIVERS)
    private List<AllowedCSIDriverVO> allowedCSIDrivers = null;

    public static final String SERIALIZED_NAME_ALLOWED_CAPABILITIES = "allowedCapabilities";
    @Schema(title = SERIALIZED_NAME_ALLOWED_CAPABILITIES, description = "allowedCapabilities is a list of capabilities that can be requested to add to the container. Capabilities in this field may be added at the pod author's discretion. You must not list a capability in both allowedCapabilities and requiredDropCapabilities.")
    @SerializedName(SERIALIZED_NAME_ALLOWED_CAPABILITIES)
    private List<String> allowedCapabilities = null;

    public static final String SERIALIZED_NAME_ALLOWED_FLEX_VOLUMES = "allowedFlexVolumes";
    @Schema(title = SERIALIZED_NAME_ALLOWED_FLEX_VOLUMES, description = "allowedFlexVolumes is a whitelist of allowed Flexvolumes.  Empty or nil indicates that all Flexvolumes may be used.  This parameter is effective only when the usage of the Flexvolumes is allowed in the 'volumes' field.")
    @SerializedName(SERIALIZED_NAME_ALLOWED_FLEX_VOLUMES)
    private List<AllowedFlexVolumeVO> allowedFlexVolumes = null;

    public static final String SERIALIZED_NAME_ALLOWED_HOST_PATHS = "allowedHostPaths";
    @Schema(title = SERIALIZED_NAME_ALLOWED_HOST_PATHS, description = "allowedHostPaths is a white list of allowed host paths. Empty indicates that all host paths may be used.")
    @SerializedName(SERIALIZED_NAME_ALLOWED_HOST_PATHS)
    private List<AllowedHostPathVO> allowedHostPaths = null;

    public static final String SERIALIZED_NAME_ALLOWED_PROC_MOUNT_TYPES = "allowedProcMountTypes";
    @Schema(title = SERIALIZED_NAME_ALLOWED_PROC_MOUNT_TYPES, description = "AllowedProcMountTypes is a whitelist of allowed ProcMountTypes. Empty or nil indicates that only the DefaultProcMountType may be used. This requires the ProcMountType feature flag to be enabled.")
    @SerializedName(SERIALIZED_NAME_ALLOWED_PROC_MOUNT_TYPES)
    private List<String> allowedProcMountTypes = null;

    public static final String SERIALIZED_NAME_ALLOWED_UNSAFE_SYSCTLS = "allowedUnsafeSysctls";
    @Schema(title = SERIALIZED_NAME_ALLOWED_UNSAFE_SYSCTLS, description = "allowedUnsafeSysctls is a list of explicitly allowed unsafe sysctls, defaults to none. Each entry is either a plain sysctl name or ends in '*' in which case it is considered as a prefix of allowed sysctls. Single * means all unsafe sysctls are allowed. Kubelet has to whitelist all allowed unsafe sysctls explicitly to avoid rejection.  Examples: e.g. 'foo/_*' allows 'foo/bar', 'foo/baz', etc. e.g. 'foo.*' allows 'foo.bar', 'foo.baz', etc.")
    @SerializedName(SERIALIZED_NAME_ALLOWED_UNSAFE_SYSCTLS)
    private List<String> allowedUnsafeSysctls = null;

    public static final String SERIALIZED_NAME_DEFAULT_ADD_CAPABILITIES = "defaultAddCapabilities";
    @Schema(title = SERIALIZED_NAME_DEFAULT_ADD_CAPABILITIES, description = "defaultAddCapabilities is the default set of capabilities that will be added to the container unless the pod spec specifically drops the capability.  You may not list a capability in both defaultAddCapabilities and requiredDropCapabilities. Capabilities added here are implicitly allowed, and need not be included in the allowedCapabilities list.")
    @SerializedName(SERIALIZED_NAME_DEFAULT_ADD_CAPABILITIES)
    private List<String> defaultAddCapabilities = null;

    public static final String SERIALIZED_NAME_DEFAULT_ALLOW_PRIVILEGE_ESCALATION = "defaultAllowPrivilegeEscalation";
    @Schema(title = SERIALIZED_NAME_DEFAULT_ALLOW_PRIVILEGE_ESCALATION, description = "defaultAllowPrivilegeEscalation controls the default setting for whether a process can gain more privileges than its parent process.")
    @SerializedName(SERIALIZED_NAME_DEFAULT_ALLOW_PRIVILEGE_ESCALATION)
    private Boolean defaultAllowPrivilegeEscalation;

    public static final String SERIALIZED_NAME_FORBIDDEN_SYSCTLS = "forbiddenSysctls";
    @Schema(title = SERIALIZED_NAME_FORBIDDEN_SYSCTLS, description = "forbiddenSysctls is a list of explicitly forbidden sysctls, defaults to none. Each entry is either a plain sysctl name or ends in '*' in which case it is considered as a prefix of forbidden sysctls. Single * means all sysctls are forbidden.  Examples: e.g. 'foo/_*' forbids 'foo/bar', 'foo/baz', etc. e.g. 'foo.*' forbids 'foo.bar', 'foo.baz', etc.")
    @SerializedName(SERIALIZED_NAME_FORBIDDEN_SYSCTLS)
    private List<String> forbiddenSysctls = null;

    public static final String SERIALIZED_NAME_FS_GROUP = "fsGroup";
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, title = SERIALIZED_NAME_FS_GROUP, description = "FSGroupStrategyOptions defines the strategy type and options used to create the strategy.")
    @SerializedName(SERIALIZED_NAME_FS_GROUP)
    private FSGroupStrategyOptionsVO fsGroup;

    public static final String SERIALIZED_NAME_HOST_I_P_C = "hostIPC";
    @Schema(title = SERIALIZED_NAME_HOST_I_P_C, description = "hostIPC determines if the policy allows the use of HostIPC in the pod spec.")
    @SerializedName(SERIALIZED_NAME_HOST_I_P_C)
    private Boolean hostIPC;

    public static final String SERIALIZED_NAME_HOST_NETWORK = "hostNetwork";
    @Schema(title = SERIALIZED_NAME_HOST_NETWORK, description = "hostNetwork determines if the policy allows the use of HostNetwork in the pod spec.")
    @SerializedName(SERIALIZED_NAME_HOST_NETWORK)
    private Boolean hostNetwork;

    public static final String SERIALIZED_NAME_HOST_P_I_D = "hostPID";
    @Schema(title = SERIALIZED_NAME_HOST_P_I_D, description = "hostPID determines if the policy allows the use of HostPID in the pod spec.")
    @SerializedName(SERIALIZED_NAME_HOST_P_I_D)
    private Boolean hostPID;

    public static final String SERIALIZED_NAME_HOST_PORTS = "hostPorts";
    @Schema(title = SERIALIZED_NAME_HOST_PORTS, description = "hostPorts determines which host port ranges are allowed to be exposed.")
    @SerializedName(SERIALIZED_NAME_HOST_PORTS)
    private List<HostPortRangeVO> hostPorts = null;

    public static final String SERIALIZED_NAME_PRIVILEGED = "privileged";
    @Schema(title = SERIALIZED_NAME_PRIVILEGED, description = "privileged determines if a pod can request to be run as privileged.")
    @SerializedName(SERIALIZED_NAME_PRIVILEGED)
    private Boolean privileged;

    public static final String SERIALIZED_NAME_READ_ONLY_ROOT_FILESYSTEM = "readOnlyRootFilesystem";
    @Schema(title = SERIALIZED_NAME_READ_ONLY_ROOT_FILESYSTEM, description = "readOnlyRootFilesystem when set to true will force containers to run with a read only root file system.  If the container specifically requests to run with a non-read only root file system the PSP should deny the pod. If set to false the container may run with a read only root file system if it wishes but it will not be forced to.")
    @SerializedName(SERIALIZED_NAME_READ_ONLY_ROOT_FILESYSTEM)
    private Boolean readOnlyRootFilesystem;

    public static final String SERIALIZED_NAME_REQUIRED_DROP_CAPABILITIES = "requiredDropCapabilities";
    @Schema(title = SERIALIZED_NAME_REQUIRED_DROP_CAPABILITIES, description = "requiredDropCapabilities are the capabilities that will be dropped from the container.  These are required to be dropped and cannot be added.")
    @SerializedName(SERIALIZED_NAME_REQUIRED_DROP_CAPABILITIES)
    private List<String> requiredDropCapabilities = null;

    public static final String SERIALIZED_NAME_RUN_AS_GROUP = "runAsGroup";
    @Schema(title = SERIALIZED_NAME_RUN_AS_GROUP, description = "RunAsGroupStrategyOptions defines the strategy type and any options used to create the strategy.")
    @SerializedName(SERIALIZED_NAME_RUN_AS_GROUP)
    private RunAsGroupStrategyOptionsVO runAsGroup;

    public static final String SERIALIZED_NAME_RUN_AS_USER = "runAsUser";
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, title = SERIALIZED_NAME_RUN_AS_USER, description = "RunAsUserStrategyOptions defines the strategy type and any options used to create the strategy.")
    @SerializedName(SERIALIZED_NAME_RUN_AS_USER)
    private RunAsUserStrategyOptionsVO runAsUser;

    public static final String SERIALIZED_NAME_RUNTIME_CLASS = "runtimeClass";
    @Schema(title = SERIALIZED_NAME_RUNTIME_CLASS, description = "RuntimeClassStrategyOptions define the strategy that will dictate the allowable RuntimeClasses for a pod.")
    @SerializedName(SERIALIZED_NAME_RUNTIME_CLASS)
    private RuntimeClassStrategyOptionsVO runtimeClass;

    public static final String SERIALIZED_NAME_SE_LINUX = "seLinux";
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, title = SERIALIZED_NAME_SE_LINUX, description = "SELinuxStrategyOptions defines the strategy type and any options used to create the strategy.")
    @SerializedName(SERIALIZED_NAME_SE_LINUX)
    private SELinuxStrategyOptionsVO seLinux;

    public static final String SERIALIZED_NAME_SUPPLEMENTAL_GROUPS = "supplementalGroups";
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, title = SERIALIZED_NAME_SUPPLEMENTAL_GROUPS, description = "SupplementalGroupsStrategyOptions defines the strategy type and options used to create the strategy.")
    @SerializedName(SERIALIZED_NAME_SUPPLEMENTAL_GROUPS)
    private SupplementalGroupsStrategyOptionsVO supplementalGroups;

    public static final String SERIALIZED_NAME_VOLUMES = "volumes";
    @Schema(title = SERIALIZED_NAME_VOLUMES, description = "volumes is a white list of allowed volume plugins. Empty indicates that no volumes may be used. To allow all volumes you may use '*'.")
    @SerializedName(SERIALIZED_NAME_VOLUMES)
    private List<String> volumes = null;

}
