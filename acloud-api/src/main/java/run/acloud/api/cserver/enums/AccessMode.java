package run.acloud.api.cserver.enums;

import org.apache.commons.lang3.StringUtils;
import run.acloud.commons.enums.EnumCode;

import java.util.Arrays;
import java.util.Optional;

/**
 * @author wschoi@acornsoft.io
 * Created on 2017. 6. 30.
 */
public enum AccessMode implements EnumCode {
    RWO("ReadWriteOnce"),
    ROX("ReadOnlyMany"),
    RWX("ReadWriteMany");

    private String value;

    AccessMode(String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }

    @Override
    public String getCode() {
        return this.name();
    }

    public PersistentVolumeType getPersistentVolumeType() {
        return this == AccessMode.RWO ? PersistentVolumeType.SINGLE : PersistentVolumeType.SHARED;
    }

    public static AccessMode codeOf(String code) {
        return AccessMode.valueOf(code);
    }

    public static AccessMode getAccessMode(String value) {
        Optional<AccessMode> accessModeOptional = Arrays.asList(AccessMode.values()).stream().filter(a -> (StringUtils.isNotBlank(value) && StringUtils.equals(a.getValue(), value))).findFirst();

        if (accessModeOptional.isPresent()) {
            return accessModeOptional.get();
        }

        return null;
    }
}
