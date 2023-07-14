package run.acloud.api.resource.enums;

import run.acloud.commons.enums.EnumCode;

public enum PathType implements EnumCode {
    ImplementationSpecific, Exact, Prefix;

    public static class Names{
        public static final String ImplementationSpecific = "ImplementationSpecific";
        public static final String Exact = "Exact";
        public static final String Prefix = "Prefix";
    }

    @Override
    public String getCode() {
        return this.name();
    }

    public static PathType codeOf(String code) {
        return PathType.valueOf(code);
    }

}
