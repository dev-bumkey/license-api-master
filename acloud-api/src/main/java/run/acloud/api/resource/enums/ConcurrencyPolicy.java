package run.acloud.api.resource.enums;

import run.acloud.commons.enums.EnumCode;

public enum ConcurrencyPolicy implements EnumCode {
    Allow, Forbid, Replace;

    @Override
    public String getCode() {
        return this.name();
    }

    public static ConcurrencyPolicy codeOf(String code) {
        return ConcurrencyPolicy.valueOf(code);
    }
}
