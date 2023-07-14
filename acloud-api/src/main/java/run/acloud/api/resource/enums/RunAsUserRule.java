package run.acloud.api.resource.enums;

import lombok.Getter;
import run.acloud.commons.enums.EnumCode;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public enum RunAsUserRule implements EnumCode {
    RunAsAny(1),
    MustRunAsNonRoot(2),
    MustRunAs(3);

    @Getter
    private int order;

    RunAsUserRule(int order) {
        this.order = order;
    }

    @Override
    public String getCode() {
        return this.name();
    }

    public static RunAsUserRule codeOf(String code) {
        return RunAsUserRule.valueOf(code);
    }

    public static List<String> getList() {
        return Arrays.stream(RunAsUserRule.values()).sorted(Comparator.comparingLong(RunAsUserRule::getOrder)).map(RunAsUserRule::getCode).collect(Collectors.toList());
    }

}
