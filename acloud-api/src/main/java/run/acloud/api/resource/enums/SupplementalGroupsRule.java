package run.acloud.api.resource.enums;

import lombok.Getter;
import run.acloud.commons.enums.EnumCode;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public enum SupplementalGroupsRule implements EnumCode {
    RunAsAny(1),
    MayRunAs(2),
    MustRunAs(3);

    @Getter
    private int order;

    SupplementalGroupsRule(int order) {
        this.order = order;
    }

    @Override
    public String getCode() {
        return this.name();
    }

    public static SupplementalGroupsRule codeOf(String code) {
        return SupplementalGroupsRule.valueOf(code);
    }

    public static List<String> getList() {
        return Arrays.stream(SupplementalGroupsRule.values()).sorted(Comparator.comparingLong(SupplementalGroupsRule::getOrder)).map(SupplementalGroupsRule::getCode).collect(Collectors.toList());
    }

}
