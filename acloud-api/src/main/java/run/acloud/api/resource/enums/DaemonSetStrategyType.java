package run.acloud.api.resource.enums;

import run.acloud.commons.enums.EnumCode;

public enum DaemonSetStrategyType implements EnumCode {
    RollingUpdate, OnDelete;

    @Override
    public String getCode() {
        return this.name();
    }

    public static DaemonSetStrategyType codeOf(String code) {
        return DaemonSetStrategyType.valueOf(code);
    }
}
