package run.acloud.api.resource.enums;

import run.acloud.commons.enums.EnumCode;

public enum ProbeType implements EnumCode {
    EXEC, TCPSOCKET, HTTPGET;

    @Override
    public String getCode() {
        return this.name();
    }

    public static ProbeType codeOf(String code) {
        return ProbeType.valueOf(code);
    }
}
