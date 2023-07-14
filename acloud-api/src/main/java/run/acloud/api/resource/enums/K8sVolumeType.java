package run.acloud.api.resource.enums;

import lombok.Getter;
import run.acloud.commons.enums.EnumCode;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author wschoi@acornsoft.io
 * Created on 2017. 6. 21.
 */
public enum K8sVolumeType implements EnumCode {
    AWS_ELASTIC_BLOCK_STORE("awsElasticBlockStore",  false),
    AZURE_DISK("azureDisk",  false),
    AZURE_FILE("azureFile",  false),
    CEPHFS("cephFS",  false),
    CINDER("cinder",  false),
    CONFIG_MAP("configMap",  true),
    CSI("csi",  false),
    DOWNWARD_API("downwardAPI",  true),
    EMPTY_DIR("emptyDir",  true),
    FC("fc",  false),
    FLEX_VOLUME("flexVolume",  false),
    FLOCKER("flocker",  false),
    GCE_PERSISTENT_DISK("gcePersistentDisk",  false),
    GLUSTERFS("glusterfs",  false),
    HOST_PATH("hostPath",  false),
    ISCSI("iscsi",  false),
//    LOCAL("local",  false),
    NFS("nfs",  false),
    PERSISTENT_VOLUME_CLAIM("persistentVolumeClaim",  true),
    PHOTON_PERSISTENT_DISK("photonPersistentDisk",  false),
    PORTWORX_VOLUME("portworxVolume",  false),
    PROJECTED("projected",  true),
    QUOBYTE("quobyte",  false),
    RBD("rbd",  false),
    SCALE_IO("scaleIO",  false),
    SECRET("secret",  true),
    STORAGEOS("storageos",  false),
    VSPHERE_VOLUME("vsphereVolume",  false);

    @Getter
    private String value;

    @Getter
    private boolean isMinimumSet;

    K8sVolumeType(String value, boolean isMinimumSet) {
        this.value = value;
        this.isMinimumSet = isMinimumSet;
    }

    public Map<String, Object> toMap(){
        Map<String, Object> toMap = new HashMap<>();
        toMap.put("value", this.getValue());
        toMap.put("isMinimumSet", this.isMinimumSet());

        return toMap;
    }

    public static List<Map<String, Object>> getList() {
        return Arrays.stream(K8sVolumeType.values()).map(K8sVolumeType::toMap).collect(Collectors.toList());
    }

    @Override
    public String getCode() {
        return this.name();
    }
}
