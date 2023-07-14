package run.acloud.api.resource.enums;

import run.acloud.commons.enums.EnumCode;

public enum ImagePullPolicyType implements EnumCode {
    Always, Never, IfNotPresent;

    @Override
    public String getCode() {
        return this.name();
    }

    public static ImagePullPolicyType codeOf(String code) {
        return ImagePullPolicyType.valueOf(code);
    }
}
