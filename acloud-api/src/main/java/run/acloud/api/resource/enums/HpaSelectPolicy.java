package run.acloud.api.resource.enums;

import run.acloud.api.code.vo.CodeVO;
import run.acloud.commons.enums.EnumCode;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum HpaSelectPolicy implements EnumCode {
    Max, Min, Disabled;

    public static class Names{
        public static final String Max = "Max";
        public static final String Min = "Min";
        public static final String Disabled = "Disabled";
    }

    public static List<CodeVO> getCodeList() {
        return Arrays.stream(HpaSelectPolicy.values())
                .map(vp -> {
                    CodeVO code = new CodeVO();
                    code.setGroupId("HPA_SELECT_POLICY");
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

    public static HpaSelectPolicy codeOf(String code) {
        return HpaSelectPolicy.valueOf(code);
    }

}
