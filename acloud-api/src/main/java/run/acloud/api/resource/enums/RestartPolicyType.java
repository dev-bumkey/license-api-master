package run.acloud.api.resource.enums;

import run.acloud.commons.enums.EnumCode;

public enum RestartPolicyType implements EnumCode {
    Always, OnFailure, Never;

    @Override
    public String getCode() {
        return this.name();
    }

    public static RestartPolicyType codeOf(String code) {
        return RestartPolicyType.valueOf(code);
    }
}
