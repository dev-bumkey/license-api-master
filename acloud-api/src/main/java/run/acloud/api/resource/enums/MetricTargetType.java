package run.acloud.api.resource.enums;

import lombok.Getter;
import org.apache.commons.lang3.BooleanUtils;
import run.acloud.commons.enums.EnumCode;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum MetricTargetType implements EnumCode {
    AverageUtilization("N"), Utilization("Y"), Value("N"), AverageValue("Y");

    @Getter
    private String displayYn;

    public static class Names{
        public static final String AverageUtilization = "AverageUtilization";
        public static final String Utilization = "Utilization";
        public static final String Value = "Value";
        public static final String AverageValue = "AverageValue";
    }

    MetricTargetType(String displayYn) {
        this.displayYn = displayYn;
    }

    @Override
    public String getCode() {
        return this.name();
    }

    public static MetricTargetType codeOf(String code) {
        return MetricTargetType.valueOf(code);
    }

    public static List<String> getList() {
        return Arrays.stream(MetricTargetType.values()).filter(mt -> (BooleanUtils.toBoolean(mt.displayYn))).map(MetricTargetType::getCode).collect(Collectors.toList());
    }

}
