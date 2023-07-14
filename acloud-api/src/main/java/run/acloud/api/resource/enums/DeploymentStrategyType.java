package run.acloud.api.resource.enums;

import run.acloud.commons.enums.EnumCode;

public enum DeploymentStrategyType implements EnumCode {
    Recreate, RollingUpdate;

    @Override
    public String getCode() {
        return this.name();
    }

    public static DeploymentStrategyType codeOf(String code) {
        return DeploymentStrategyType.valueOf(code);
    }
}
