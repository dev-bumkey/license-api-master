package run.acloud.api.cserver.enums;

import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import run.acloud.api.code.vo.CodeVO;
import run.acloud.api.resource.enums.K8sApiVerType;
import run.acloud.commons.enums.EnumCode;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author wschoi@acornsoft.io
 * Created on 2017. 6. 30.
 */
public enum VolumePlugIn implements EnumCode {
    NFSDYNAMIC(
            "nfsDynamicVolume",
            "NFS",
            null,
            "NETWORK",
            null,
            null,
            getSupportApiVersionRange(K8sApiVerType.V1_14, K8sApiVerType.V1_21),
            false,
            false,
            "Y"
    ),
    NFSSTATIC(
            "nfsStaticVolume",
            "NFS Named",
            null,
            "NETWORK",
            getVolumePlugInParams("NFSSTATIC"),
            null,
            getSupportApiVersionRange(K8sApiVerType.V1_14, K8sApiVerType.V1_21),
            false,
            false,
            "Y"
    ),
    NFS_CSI(
            "nfsDynamicVolume",
            "NFS CSI",
            ProvisionerNames.NFS_CSI,
            "NETWORK",
            null,
            null,
            getSupportApiVersionUpto(K8sApiVerType.V1_18),
            true,
            true,
            "Y"
    ),
    GFS(
            "glusterfs",
            "Glusterfs",
            null,
            "NETWORK",
            null,
            null,
            getSupportApiVersionRange(K8sApiVerType.V1_14, K8sApiVerType.V1_21),
            false,
            false,
            "N"
    ),
    VSPHEREVOLUME(
            "vsphereVolume",
            "vSphere Volume",
            ProvisionerNames.VSPHERE_VOLUME,
            "BLOCK",
            getVolumePlugInParams("VSPHEREVOLUME"),
            null,
            getSupportApiVersionRange(K8sApiVerType.V1_14, K8sApiVerType.V1_21),
            false,
            false,
            "Y"
    ),
    VSPHEREVOLUME_CSI(
            "vsphereVolume",
            "vSphere Volume CSI",
            ProvisionerNames.VSPHERE_VOLUME_CSI,
            "BLOCK",
            null,
            null,
            getSupportApiVersionUpto(K8sApiVerType.V1_18),
            true,
            true,
            "Y"
    ),
    NCPBLOCK_CSI(
            "ncpBlock",
            "NCP Block Storage CSI",
            ProvisionerNames.NCP_BLOCK_CSI,
            "BLOCK",
            null,
            EnumSet.copyOf(Arrays.asList(VolumeBindingMode.values())),
            getSupportApiVersionUpto(K8sApiVerType.V1_18),
            true,
            true,
            "Y"
    ),
    NCPNAS_CSI(
            "ncpNas",
            "NCP NAS Volume CSI",
            ProvisionerNames.NCP_NAS_CSI,
            "BLOCK",
            null,
            EnumSet.copyOf(Arrays.asList(VolumeBindingMode.values())),
            getSupportApiVersionUpto(K8sApiVerType.V1_18),
            true,
            true,
            "Y"
    ),
    AWSEBS(
            "aWSElasticBlockStore",
            "AWS EBS",
            ProvisionerNames.AWS_EBS,
            "BLOCK",
            getVolumePlugInParams("AWSEBS"),
            EnumSet.copyOf(Arrays.asList(VolumeBindingMode.values())),
            getSupportApiVersionRange(K8sApiVerType.V1_14, K8sApiVerType.V1_21),
            false,
            false,
            "Y"
    ),
    AWSEFS(
            "aWSElasticFileSystem",
            "AWS EFS",
            null,
            "NETWORK",
            null,
            null,
            getSupportApiVersionRange(K8sApiVerType.V1_14, K8sApiVerType.V1_21),
            false,
            false,
            "Y"
    ),
    AWSEBS_CSI(
            "aWSElasticBlockStore",
            "AWS EBS CSI",
            ProvisionerNames.AWS_EBS_CSI,
            "BLOCK",
            null,
            EnumSet.copyOf(Arrays.asList(VolumeBindingMode.values())),
            getSupportApiVersionUpto(K8sApiVerType.V1_18),
            true,
            true,
            "Y"
    ),
    AWSEFS_CSI(
            "aWSElasticFileSystem",
            "AWS EFS CSI",
            ProvisionerNames.AWS_EFS_CSI,
            "BLOCK",
            null,
            null,
            getSupportApiVersionUpto(K8sApiVerType.V1_18),
            true,
            true,
            "Y"
    ),
    GCE(
            "gCEPersistentDisk",
            "Google Persistent Disk",
            ProvisionerNames.GCE_PD,
            "BLOCK",
            getVolumePlugInParams("GCE"),
            EnumSet.copyOf(Arrays.asList(VolumeBindingMode.values())),
            getSupportApiVersionRange(K8sApiVerType.V1_14, K8sApiVerType.V1_21),
            false,
            false,
            "Y"
    ),
    GCE_CSI(
            "gCEPersistentDiskCsi",
            "Compute Engine Persistent Disk CSI",
            ProvisionerNames.GCE_PD_CSI,
            "BLOCK",
            null,
            EnumSet.copyOf(Arrays.asList(VolumeBindingMode.values())),
            getSupportApiVersionUpto(K8sApiVerType.V1_18),
            true,
            true,
            "Y"
    ),
    AZUREDISK(
            "azureDisk",
            "Azure Disk",
            ProvisionerNames.AZURE_DISK,
            "BLOCK",
            getVolumePlugInParams("AZUREDISK"),
            EnumSet.copyOf(Arrays.asList(VolumeBindingMode.values())),
            getSupportApiVersionRange(K8sApiVerType.V1_14, K8sApiVerType.V1_21),
            false,
            false,
            "Y"

    ),
    AZUREFILE(
            "azureFile",
            "Azure File",
            ProvisionerNames.AZURE_FILE,
            "BLOCK",
            getVolumePlugInParams("AZUREFILE"),
            null,
            getSupportApiVersionRange(K8sApiVerType.V1_14, K8sApiVerType.V1_21),
            false,
            false,
            "Y"
    ),
    AZUREDISK_CSI(
            "azureDisk",
            "Azure Disk CSI",
            ProvisionerNames.AZURE_DISK_CSI,
            "BLOCK",
            null,
            EnumSet.copyOf(Arrays.asList(VolumeBindingMode.values())),
            getSupportApiVersionUpto(K8sApiVerType.V1_18),
            true,
            true,
            "Y"
    ),
    AZUREFILE_CSI(
            "azureFile",
            "Azure File CSI",
            ProvisionerNames.AZURE_FILE_CSI,
            "BLOCK",
            null,
            null,
            getSupportApiVersionUpto(K8sApiVerType.V1_18),
            true,
            true,
            "Y"
    )
    ;

    public static class ProvisionerNames{
        public static final String AWS_EBS = "kubernetes.io/aws-ebs";
        public static final String AWS_EBS_CSI = "ebs.csi.aws.com";
        public static final String AWS_EFS_CSI = "efs.csi.aws.com";
        public static final String GCE_PD = "kubernetes.io/gce-pd";
        public static final String GCE_PD_CSI = "pd.csi.storage.gke.io";
        public static final String AZURE_DISK = "kubernetes.io/azure-disk";
        public static final String AZURE_FILE = "kubernetes.io/azure-file";
        public static final String AZURE_DISK_CSI = "disk.csi.azure.com";
        public static final String AZURE_FILE_CSI = "file.csi.azure.com";
        public static final String VSPHERE_VOLUME = "kubernetes.io/vsphere-volume";
        public static final String VSPHERE_VOLUME_CSI = "csi.vsphere.vmware.com";
        public static final String NCP_BLOCK_CSI = "blk.csi.ncloud.com";
        public static final String NCP_NAS_CSI = "nas.csi.ncloud.com";
        public static final String NFS_CSI = "nfs.csi.k8s.io";
    }

    @Getter
    private String value;

    @Getter
    private String description;

    @Getter
    private String provisionerName;

    private String storageType;

    @Getter
    private EnumSet<VolumePlugInParams> volumePlugInParams;

    @Getter
    private EnumSet<VolumeBindingMode> volumeBindingModes;

    @Getter
    private EnumSet<K8sApiVerType> supportApiVer;

    @Getter
    private boolean addParamEnabled;

    @Getter
    private boolean addMountOptionEnabled;

    @Getter
    private String useYn;

    VolumePlugIn(
              String value
            , String description
            , String provisionerName
            , String storageType
            , EnumSet<VolumePlugInParams> volumePlugInParams
            , EnumSet<VolumeBindingMode> volumeBindingModes
            , EnumSet<K8sApiVerType> supportApiVer
            , boolean addParamEnabled
            , boolean addMountOptionEnabled
            , String useYn
    ) {
        this.value = value;
        this.description = description;
        this.provisionerName = provisionerName;
        this.storageType = storageType;
        this.volumePlugInParams = volumePlugInParams;
        this.volumeBindingModes = volumeBindingModes;
        this.supportApiVer = supportApiVer;
        this.addParamEnabled = addParamEnabled;
        this.addMountOptionEnabled = addMountOptionEnabled;
        this.useYn = useYn;
    }

    public List<Map<String, Object>> getVolumePlugInParamsToList(){
        if(CollectionUtils.isNotEmpty(this.volumePlugInParams)){
            List<Map<String, Object>> volumePlugInParams = new ArrayList<>();
            for(VolumePlugInParams volumePlugInParamsRow : this.volumePlugInParams) {
                volumePlugInParams.add(volumePlugInParamsRow.toMap());
            }
            return volumePlugInParams;
        }

        return null;
    }

    public List<Map<String, String>> getVolumeBindingModeToList(){
        List<Map<String, String>> volumeBindingModes = new ArrayList<>();
        if(CollectionUtils.isNotEmpty(this.volumeBindingModes)){
            for (VolumeBindingMode volumeBindingModeRow : this.volumeBindingModes){
                Map<String, String> volumeBindingModeMap = new HashMap<>();
                volumeBindingModeMap.put("code", volumeBindingModeRow.getCode());
                volumeBindingModeMap.put("value", volumeBindingModeRow.getValue());
                volumeBindingModes.add(volumeBindingModeMap);
            }
        }

        return volumeBindingModes;
    }

    public static EnumSet<VolumePlugInParams> getVolumePlugInParams(String volumePlugIn) {
        return VolumePlugInParams.getParamsByPlugIn(volumePlugIn);
    }

    public static EnumSet<VolumePlugIn> getVolumePlugInByType(String storageMainType) {
        EnumSet<VolumePlugIn> plugInsEnumSet = EnumSet.noneOf(VolumePlugIn.class);
        if (StringUtils.isNotBlank(storageMainType)) {
            for (VolumePlugIn volumePlugInRow : VolumePlugIn.values()) {
                if (StringUtils.equals(volumePlugInRow.getCode(), storageMainType)) {
                    plugInsEnumSet.add(volumePlugInRow);
                }
            }
        }

        return plugInsEnumSet;
    }

    public static EnumSet<K8sApiVerType> getSupportApiVersionUpto(K8sApiVerType baseVersion) {
        return K8sApiVerType.getSupportApiVersionUpto(baseVersion);
    }

    public static EnumSet<K8sApiVerType> getSupportApiVersionRange(K8sApiVerType fromVersion, K8sApiVerType toVersion) {
        return K8sApiVerType.getSupportApiVersionRange(fromVersion, toVersion);
    }

    public boolean haveTotalCapacity(){
        return EnumSet.of(NFSDYNAMIC, NFSSTATIC, GFS, AWSEFS, NFS_CSI).contains(this);
    }

    public boolean haveProvisioner(){
        return EnumSet.of(AWSEBS, GCE, AZUREDISK, AZUREFILE, VSPHEREVOLUME
                        , AWSEBS_CSI, AWSEFS_CSI, GCE_CSI, AZUREDISK_CSI, AZUREFILE_CSI, VSPHEREVOLUME_CSI, NCPBLOCK_CSI, NCPNAS_CSI, NFS_CSI).contains(this);
    }

    public boolean canUpdatePVC(){
        return EnumSet.of(GCE, AWSEBS, AZUREDISK, AZUREFILE, NFSSTATIC, VSPHEREVOLUME
                        , GCE_CSI, AWSEBS_CSI, AZUREDISK_CSI, AZUREFILE_CSI, VSPHEREVOLUME_CSI, NCPBLOCK_CSI, NCPNAS_CSI).contains(this);
    }

    // PVC 용량 변경시 PV도 변경해야 하는 Plug-In Type
    public boolean canUpdatePV(){
        return EnumSet.of(NFSSTATIC).contains(this);
    }

    // StorageClass 에 allowExpandVolume : true 설정 필요한 plug-in 타입
    public boolean canVolumeExpansion(){
        return EnumSet.of(GCE, AWSEBS, AZUREDISK, AZUREFILE, VSPHEREVOLUME
                        , GCE_CSI, AWSEBS_CSI, AZUREDISK_CSI, AZUREFILE_CSI, VSPHEREVOLUME_CSI, NCPBLOCK_CSI, NCPNAS_CSI).contains(this);
    }

    public boolean canBindingMode(){
        return EnumSet.of(GCE, AWSEBS, AZUREDISK
                        , GCE_CSI, AWSEBS_CSI, AZUREDISK_CSI, NCPBLOCK_CSI, NCPNAS_CSI).contains(this);
    }

    public boolean canReadWriteMany() {
        return EnumSet.of(NFSDYNAMIC, NFSSTATIC, GFS, AZUREFILE, AWSEFS
                        , NFS_CSI, AZUREFILE_CSI, AWSEFS_CSI, NCPNAS_CSI).contains(this);
    }

    public boolean canReadOnlyMany() {
        return EnumSet.of(NFSDYNAMIC, NFSSTATIC, GFS, AZUREFILE, AWSEFS
                        , NFS_CSI, AZUREFILE_CSI, AWSEFS_CSI, NCPNAS_CSI).contains(this);
    }

    public boolean isCSI() {
        return EnumSet.of(NFS_CSI, AWSEBS_CSI, AWSEFS_CSI, GCE_CSI, AZUREDISK_CSI, AZUREFILE_CSI, VSPHEREVOLUME_CSI, NCPBLOCK_CSI, NCPNAS_CSI).contains(this);
    }

    public StorageType getStorageType() {
        return StorageType.valueOf(this.storageType);
    }

    /**
     * codes 테이블 대체 - GroupId("VOLUME_PLUGIN")
     *
     * @return
     */
    public static List<CodeVO> getVolumePlugInCodeList() {
        return Arrays.stream(VolumePlugIn.values())
                .filter(vp -> (BooleanUtils.toBoolean(vp.getUseYn())))
                .map(vp -> {
                                CodeVO code = new CodeVO();
                                code.setGroupId("VOLUME_PLUGIN");
                                code.setCode(vp.getCode());
                                code.setValue(vp.getValue());
                                code.setDescription(vp.getDescription());
                                return code;
                            })
                .collect(Collectors.toList());
    }

    @Override
    public String getCode() {
        return this.name();
    }
}
