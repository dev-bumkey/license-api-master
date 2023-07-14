package run.acloud.api.cserver.enums;

import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import run.acloud.api.resource.enums.K8sApiVerType;
import run.acloud.commons.enums.EnumCode;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author wschoi@acornsoft.io
 * Created on 2017. 6. 30.
 */
public enum VolumePlugInParams implements EnumCode {
    NFSSTATIC_SERVER("NFSSTATIC", "server", true, null, null, K8sApiVerType.getSupportApiVersionUpto(K8sApiVerType.V1_9), true, ""),
    NFSSTATIC_PATH("NFSSTATIC", "path", true, null, null, K8sApiVerType.getSupportApiVersionUpto(K8sApiVerType.V1_9), true, ""),

    VSPHEREVOLUME_DISKFORMAT("VSPHEREVOLUME", "diskformat", true, StringUtils.split("thin|zeroedthick|eagerzeroedthick", "|"), "thin", K8sApiVerType.getSupportApiVersionUpto(K8sApiVerType.V1_9), true, ""),
    VSPHEREVOLUME_FSTYPE("VSPHEREVOLUME", "fstype", false, null, "ext4", K8sApiVerType.getSupportApiVersionUpto(K8sApiVerType.V1_9), true, "Filesystem type to mount. Must be a filesystem type supported by the host operating system. Ex. \"ext4\", \"xfs\", \"ntfs\". Implicitly inferred to be \"ext4\" if unspecified."),
    VSPHEREVOLUME_DATASTORE("VSPHEREVOLUME", "datastore", false, null, null, K8sApiVerType.getSupportApiVersionUpto(K8sApiVerType.V1_9), true, ""),
    VSPHEREVOLUME_STORAGEPOLICYNAME("VSPHEREVOLUME", "storagePolicyName", false, null, null, K8sApiVerType.getSupportApiVersionUpto(K8sApiVerType.V1_9), true, ""),
    VSPHEREVOLUME_CACHERESERVATION("VSPHEREVOLUME", "cacheReservation", false, null, null, K8sApiVerType.getSupportApiVersionUpto(K8sApiVerType.V1_9), true, ""),
    VSPHEREVOLUME_DISKSTRIPES("VSPHEREVOLUME", "diskStripes", false, null, null, K8sApiVerType.getSupportApiVersionUpto(K8sApiVerType.V1_9), true, ""),
    VSPHEREVOLUME_FORCEPROVISIONING("VSPHEREVOLUME", "forceProvisioning", false, null, null, K8sApiVerType.getSupportApiVersionUpto(K8sApiVerType.V1_9), true, ""),
    VSPHEREVOLUME_HOSTFAILURESTOTOLERATE("VSPHEREVOLUME", "hostFailuresToTolerate", false, null, null, K8sApiVerType.getSupportApiVersionUpto(K8sApiVerType.V1_9), true, ""),
    VSPHEREVOLUME_IOPSLIMIT("VSPHEREVOLUME", "iopsLimit", false, null, null, K8sApiVerType.getSupportApiVersionUpto(K8sApiVerType.V1_9), true, ""),
    VSPHEREVOLUME_OBJECTSPACERESERVATION("VSPHEREVOLUME", "objectSpaceReservation", false, null, null, K8sApiVerType.getSupportApiVersionUpto(K8sApiVerType.V1_9), true, ""),

    AWSEBS_TYPE("AWSEBS", "type", true, StringUtils.split("io1|gp2|sc1|st1", "|"), "gp2", K8sApiVerType.getSupportApiVersionUpto(K8sApiVerType.V1_9), true, "io1, gp2, sc1, st1. See AWS docs for details. Default: gp2."),
    AWSEBS_ZONE("AWSEBS", "zone", false, null, null, K8sApiVerType.getSupportApiVersionUpto(K8sApiVerType.V1_9), false, "AWS zone. If neither zone nor zones is specified, volumes are generally round-robin-ed across all active zones where Kubernetes cluster has a node. zone and zones parameters must not be used at the same time."),
    AWSEBS_ZONES("AWSEBS", "zones", false, null, null, K8sApiVerType.getSupportApiVersionUpto(K8sApiVerType.V1_9), false, "A comma separated list of AWS zone(s). If neither zone nor zones is specified, volumes are generally round-robin-ed across all active zones where Kubernetes cluster has a node. zone and zones parameters must not be used at the same time."),
    AWSEBS_IOPSPERGB("AWSEBS", "iopsPerGB", false, null, null, K8sApiVerType.getSupportApiVersionUpto(K8sApiVerType.V1_9), false, "only for io1 volumes. I/O operations per second per GiB. AWS volume plugin multiplies this with size of requested volume to compute IOPS of the volume and caps it at 20 000 IOPS (maximum supported by AWS, see AWS docs. A string is expected here, i.e. \"10\", not 10."),
    AWSEBS_ENCRYPTED("AWSEBS", "encrypted", false, StringUtils.split("true|false", "|"), "false", K8sApiVerType.getSupportApiVersionUpto(K8sApiVerType.V1_9), false, "denotes whether the EBS volume should be encrypted or not. Valid values are \"true\" or \"false\". A string is expected here, i.e. \"true\", not true."),
    AWSEBS_KMSKEYID("AWSEBS", "kmsKeyId", false, null, null, K8sApiVerType.getSupportApiVersionUpto(K8sApiVerType.V1_9), false, "optional. The full Amazon Resource Name of the key to use when encrypting the volume. If none is supplied but encrypted is true, a key is generated by AWS. See AWS docs for valid ARN value."),
    AWSEBS_FSTYPE("AWSEBS", "fsType", false, null, "ext4", K8sApiVerType.getSupportApiVersionUpto(K8sApiVerType.V1_11), false, "fsType that is supported by kubernetes. Default: \"ext4\"."),

    GCE_TYPE("GCE", "type", true, StringUtils.split("pd-standard|pd-ssd", "|"), "pd-standard", K8sApiVerType.getSupportApiVersionUpto(K8sApiVerType.V1_9), true, "pd-standard or pd-ssd. Default: pd-standard"),
    GCE_ZONE("GCE", "zone", false, null, null, K8sApiVerType.getSupportApiVersionUpto(K8sApiVerType.V1_9), false, "GCE zone. If neither zone nor zones is specified, volumes are generally round-robin-ed across all active zones where Kubernetes cluster has a node. zone and zones parameters must not be used at the same time."),
    GCE_ZONES("GCE", "zones", false, null, null, K8sApiVerType.getSupportApiVersionUpto(K8sApiVerType.V1_9), false, "A comma separated list of GCE zone(s). If neither zone nor zones is specified, volumes are generally round-robin-ed across all active zones where Kubernetes cluster has a node. zone and zones parameters must not be used at the same time."),
    GCE_REPLICATION_TYPE("GCE", "replication-type", false, StringUtils.split("none|regional-pd", "|"), "none", K8sApiVerType.getSupportApiVersionUpto(K8sApiVerType.V1_10), false, "none or regional-pd. Default: none."),

//    AZUREDISK_SKUNAME("AZUREDISK", "skuName", false, null, null, K8sApiVerType.getSupportApiVersionUpto(K8sApiVerType.V1_9), true, "Azure storage account Sku tier. Default is empty."),
//    AZUREDISK_LOCATION("AZUREDISK", "location", false, null, null, K8sApiVerType.getSupportApiVersionUpto(K8sApiVerType.V1_9), true, "Azure storage account location. Default is empty."),
//    AZUREDISK_STORAGEACCOUNT("AZUREDISK", "storageAccount", true, null, null, K8sApiVerType.getSupportApiVersionUpto(K8sApiVerType.V1_9), true, "Azure storage account name. If a storage account is provided, it must reside in the same resource group as the cluster, and location is ignored. If a storage account is not provided, a new storage account will be created in the same resource group as the cluster.")
    AZUREDISK_STORAGEACCOUNTTYPE("AZUREDISK", "storageaccounttype", false, StringUtils.split("Standard_LRS|Premium_LRS", "|"), "Standard_LRS", K8sApiVerType.getSupportApiVersionUpto(K8sApiVerType.V1_9), true, "Azure storage account Sku tier. Default is empty."),
    AZUREDISK_KIND("AZUREDISK", "kind", true, StringUtils.split("Shared|Dedicated|Managed", "|"), "Managed", K8sApiVerType.getSupportApiVersionUpto(K8sApiVerType.V1_9), true, "Possible values are shared (default), dedicated, and managed. When kind is shared, all unmanaged disks are created in a few shared storage accounts in the same resource group as the cluster. When kind is dedicated, a new dedicated storage account will be created for the new unmanaged disk in the same resource group as the cluster. When kind is managed, all managed disks are created in the same resource group as the cluster."),

    AZUREFILE_SKUNAME("AZUREFILE", "skuName", false, null, null, K8sApiVerType.getSupportApiVersionUpto(K8sApiVerType.V1_9), false, "Azure storage account Sku tier. Default is empty."),
    AZUREFILE_LOCATION("AZUREFILE", "location", false, null, null, K8sApiVerType.getSupportApiVersionUpto(K8sApiVerType.V1_9), false, "Azure storage account location. Default is empty."),
    AZUREFILE_STORAGEACCOUNT("AZUREFILE", "storageAccount", true, null, null, K8sApiVerType.getSupportApiVersionUpto(K8sApiVerType.V1_9), true, "Azure storage account name. Default is empty. If a storage account is not provided, all storage accounts associated with the resource group are searched to find one that matches skuName and location. If a storage account is provided, it must reside in the same resource group as the cluster, and skuName and location are ignored.")
    ;

    @Getter
    private String volumePlugIn;

    @Getter
    private String keyName;

    @Getter
    private boolean isRequired;

    @Getter
    private String[] allowableValues;

    @Getter
    private String defaultValue;

    @Getter
    private EnumSet<K8sApiVerType> supportApiVer;

    @Getter
    private boolean used;

    @Getter
    private String description;

    VolumePlugInParams(String volumePlugIn, String keyName, boolean isRequired, String[] allowableValues, String defaultValue, EnumSet<K8sApiVerType> supportApiVer, boolean used, String description) {
        this.volumePlugIn = volumePlugIn;
        this.keyName = keyName;
        this.isRequired = isRequired;
        this.allowableValues = allowableValues;
        this.defaultValue = defaultValue;
        this.supportApiVer = supportApiVer;
        this.used = used;
        this.description = description;
    }

    public List<String> getAllowableValuesToList(){
        if (ArrayUtils.isNotEmpty(this.allowableValues)){
            return Arrays.asList(this.allowableValues);
        }

        return null;
    }

    public List<String> getSupportApiVerToList(){
        if (CollectionUtils.isNotEmpty(this.supportApiVer)){
            return this.supportApiVer.stream().map(K8sApiVerType::getVersion).collect(Collectors.toList());
        }

        return null;
    }

    public Map<String, Object> toMap(){
        Map<String, Object> volumePlugInParamsMap = new HashMap<>();
        volumePlugInParamsMap.put("plugIn", this.getVolumePlugIn());
        volumePlugInParamsMap.put("paramName", this.getKeyName());
        volumePlugInParamsMap.put("isRequired", this.isRequired());
        volumePlugInParamsMap.put("allowableValues", this.getAllowableValuesToList());
        volumePlugInParamsMap.put("defaultValue", this.getDefaultValue());
        volumePlugInParamsMap.put("supportApiVer", this.getSupportApiVerToList());
        volumePlugInParamsMap.put("used", this.isUsed());
        volumePlugInParamsMap.put("description", this.getDescription());
        volumePlugInParamsMap.put("thisObj", this);

        return volumePlugInParamsMap;
    }

    public static EnumSet<VolumePlugInParams> getParamsByPlugIn(String volumePlugIn) {
        EnumSet<VolumePlugInParams> plugInParamsEnumSet = EnumSet.noneOf(VolumePlugInParams.class);
        if (StringUtils.isNotBlank(volumePlugIn)) {
            for (VolumePlugInParams volumePlugInParamsRow : VolumePlugInParams.values()) {
                if (StringUtils.startsWith(volumePlugInParamsRow.getCode(), volumePlugIn)) {
                    plugInParamsEnumSet.add(volumePlugInParamsRow);
                }
            }
        }

        return plugInParamsEnumSet;
    }

    @Override
    public String getCode() {
        return this.name();
    }
}