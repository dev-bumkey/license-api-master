package run.acloud.api.cserver.enums;

import run.acloud.commons.enums.EnumCode;

/**
 * @author wschoi@acornsoft.io
 * Created on 2017. 6. 21.
 */
public enum VolumeType implements EnumCode {
    None,
    HOST_PATH,
    EMPTY_DIR,
    CONFIG_MAP,
    SECRET,
    PERSISTENT_VOLUME_STATIC, // static persistent volume
    PERSISTENT_VOLUME_LINKED,
    PERSISTENT_VOLUME; // dynamic persistent volume configuration

//    public static VolumeType fromString(String value) {
//        try {
//            return VolumeType.valueOf(value);
//        } catch (Exception e) {
//            return VolumeType.None;
//        }
//    }

    @Override
    public String getCode() {
        return this.name();
    }
}
