package run.acloud.api.cserver.enums;

import run.acloud.commons.enums.EnumCode;

/**
 * @author wschoi@acornsoft.io
 * Created on 2017. 6. 21.
 */
public enum PersistentVolumeType implements EnumCode {
    SINGLE,
    SHARED;

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
