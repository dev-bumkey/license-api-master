package run.acloud.api.resource.enums;

import run.acloud.api.code.vo.CodeVO;
import run.acloud.commons.enums.EnumCode;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum HpaScalingPolicyType implements EnumCode {
    Percent, Pods;

    public static class Names{
        public static final String Percent = "Percent";
        public static final String Pods = "Pods";
    }

    public static List<CodeVO> getCodeList() {
        return Arrays.stream(HpaScalingPolicyType.values())
                .map(vp -> {
                    CodeVO code = new CodeVO();
                    code.setGroupId("HPA_SCALING_POLICY_TYPE");
                    code.setCode(vp.getCode());
                    code.setValue(vp.getCode());
                    return code;
                })
                .collect(Collectors.toList());
    }

    @Override
    public String getCode() {
        return this.name();
    }

    public static HpaScalingPolicyType codeOf(String code) {
        return HpaScalingPolicyType.valueOf(code);
    }

}
