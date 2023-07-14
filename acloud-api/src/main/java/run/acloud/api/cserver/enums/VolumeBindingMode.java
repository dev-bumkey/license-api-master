package run.acloud.api.cserver.enums;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import run.acloud.api.resource.enums.K8sApiVerType;
import run.acloud.commons.enums.EnumCode;

import java.util.Arrays;
import java.util.EnumSet;

/**
 * @author wschoi@acornsoft.io
 * Created on 2017. 6. 21.
 */
public enum VolumeBindingMode implements EnumCode {
    IMMEDIATE("Immediate", K8sApiVerType.getSupportApiVersionUpto(K8sApiVerType.V1_9)),
    WAIT_FOR_FIRST_CONSUMER("WaitForFirstConsumer", K8sApiVerType.getSupportApiVersionUpto(K8sApiVerType.V1_12))
    ;

    @Getter
    private String value;

    @Getter
    private EnumSet<K8sApiVerType> supportedVersion;

    VolumeBindingMode(String value, EnumSet<K8sApiVerType> supportedVersion) {
        this.value = value;
        this.supportedVersion = supportedVersion;
    }

    public boolean supported(K8sApiVerType k8sApiVerType) {
        return this.getSupportedVersion().contains(k8sApiVerType);
    }

    public static VolumeBindingMode getCodeByValue(String value){
        return Arrays.stream(VolumeBindingMode.values()).filter(vbm -> (StringUtils.contains(vbm.getValue(), value))).findFirst().orElseGet(() ->null);
    }

    @Override
    public String getCode() {
        return this.name();
    }
}
