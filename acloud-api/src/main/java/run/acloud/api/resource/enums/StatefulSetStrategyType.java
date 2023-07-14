package run.acloud.api.resource.enums;

import run.acloud.commons.enums.EnumCode;

public enum StatefulSetStrategyType implements EnumCode {
    RollingUpdate, OnDelete;

    @Override
    public String getCode() {
        return this.name();
    }

    public static StatefulSetStrategyType codeOf(String code) {
        return StatefulSetStrategyType.valueOf(code);
    }
}
