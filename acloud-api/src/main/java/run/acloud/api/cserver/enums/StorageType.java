package run.acloud.api.cserver.enums;

import lombok.Getter;
import run.acloud.commons.enums.EnumCode;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/**
 * @author wschoi@acornsoft.io
 * Created on 2017. 6. 21.
 */
public enum StorageType implements EnumCode {
    BLOCK("Block", StorageType.getVolumePlugIn("BLOCK")),
    NETWORK("Network", StorageType.getVolumePlugIn("NETWORK"))
    ;

    @Getter
    private String value;

    @Getter
    private EnumSet<VolumePlugIn> volumePlugIns;

    StorageType(String value, EnumSet<VolumePlugIn> volumePlugIns) {
        this.value = value;
        this.volumePlugIns = volumePlugIns;
    }

    public Map<String, Object> toMap(){
        Map<String, Object> volumePlugInMap = new HashMap<>();
        volumePlugInMap.put("code", this.getCode());
        volumePlugInMap.put("value", this.getValue());

        return volumePlugInMap;
    }

    public static EnumSet<VolumePlugIn> getVolumePlugIn(String storageType) {
        return VolumePlugIn.getVolumePlugInByType(storageType);
    }

    @Override
    public String getCode() {
        return this.name();
    }
}
