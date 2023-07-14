package run.acloud.api.resource.enums;

import run.acloud.commons.enums.EnumCode;

import java.util.Arrays;

public enum DynamicLabelType implements EnumCode {
    PVC,
    SERVICE;

    public static DynamicLabelType findDynamicLabelType(String findVal){
        return Arrays.stream(DynamicLabelType.values()).filter(vk -> (vk.toString().equals(findVal)))
            .findFirst()
            .orElseGet(() ->null);
    }

    @Override
    public String getCode() {
        return this.name();
    }
}