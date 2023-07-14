package run.acloud.api.resource.enums;

import lombok.Getter;
import run.acloud.commons.enums.EnumCode;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public enum RunAsGroupRule implements EnumCode {
    RunAsAny(1),
    MayRunAs(2),
    MustRunAs(3);

    @Getter
    private int order;

    RunAsGroupRule(int order) {
        this.order = order;
    }

    @Override
    public String getCode() {
        return this.name();
    }

    public static RunAsGroupRule codeOf(String code) {
        return RunAsGroupRule.valueOf(code);
    }

    public static List<String> getList() {
        return Arrays.stream(RunAsGroupRule.values()).sorted(Comparator.comparingLong(RunAsGroupRule::getOrder)).map(RunAsGroupRule::getCode).collect(Collectors.toList());
    }

}
