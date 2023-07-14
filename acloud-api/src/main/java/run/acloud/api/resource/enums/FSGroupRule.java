package run.acloud.api.resource.enums;

import lombok.Getter;
import run.acloud.commons.enums.EnumCode;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public enum FSGroupRule implements EnumCode {
    RunAsAny(1),
    MayRunAs(2),
    MustRunAs(3);

    @Getter
    private int order;

    FSGroupRule(int order) {
        this.order = order;
    }

    @Override
    public String getCode() {
        return this.name();
    }

    public static FSGroupRule codeOf(String code) {
        return FSGroupRule.valueOf(code);
    }

    public static List<String> getList() {
        return Arrays.stream(FSGroupRule.values()).sorted(Comparator.comparingLong(FSGroupRule::getOrder)).map(FSGroupRule::getCode).collect(Collectors.toList());
    }

}
