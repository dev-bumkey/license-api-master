package run.acloud.api.resource.enums;

import run.acloud.commons.enums.EnumCode;

public enum MetricType  implements EnumCode {
    Object, Pods, Resource, External, ContainerResource;

    public static class Names{
        public static final String Object = "Object";
        public static final String Pods = "Pods";
        public static final String Resource = "Resource";
        public static final String External = "External";
        public static final String ContainerResource = "ContainerResource";
    }

    @Override
    public String getCode() {
        return this.name();
    }

    public static MetricType codeOf(String code) {
        return MetricType.valueOf(code);
    }

}
