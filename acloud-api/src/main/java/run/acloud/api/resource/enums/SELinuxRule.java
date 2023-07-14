package run.acloud.api.resource.enums;

import lombok.Getter;
import run.acloud.commons.enums.EnumCode;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public enum SELinuxRule implements EnumCode {
    RunAsAny(1),
    MustRunAs(2);

    @Getter
    private int order;

    SELinuxRule(int order) {
        this.order = order;
    }

    @Override
    public String getCode() {
        return this.name();
    }

    public static SELinuxRule codeOf(String code) {
        return SELinuxRule.valueOf(code);
    }

    public static List<String> getList() {
        return Arrays.stream(SELinuxRule.values()).sorted(Comparator.comparingLong(SELinuxRule::getOrder)).map(SELinuxRule::getCode).collect(Collectors.toList());
    }

}
