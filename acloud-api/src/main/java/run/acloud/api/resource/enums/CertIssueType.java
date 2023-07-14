package run.acloud.api.resource.enums;

import lombok.Getter;
import run.acloud.api.code.vo.CodeVO;
import run.acloud.commons.enums.EnumCode;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public enum CertIssueType implements EnumCode {
    selfSigned("Self-Signed", 1),
    ca("CA", 2),
    acme("ACME", 3),
    vault("Vault", 4),
    venafi("Venafi", 5)
    ;

    @Getter
    private String value;

    @Getter
    private int order;

    CertIssueType(String value, int order) {
        this.value = value;
        this.order = order;
    }

    @Override
    public String getCode() {
        return this.name();
    }

    public static CertIssueType codeOf(String code) {
        return CertIssueType.valueOf(code);
    }

    public static List<String> getList() {
        return Arrays.stream(CertIssueType.values()).map(CertIssueType::getCode).collect(Collectors.toList());
    }

    public static List<CodeVO> getCodeList() {
        return Arrays.stream(CertIssueType.values())
                .sorted(Comparator.comparingLong(CertIssueType::getOrder))
                .map(vp -> {
                    CodeVO code = new CodeVO();
                    code.setGroupId("CERT_ISSUE_TYPE");
                    code.setCode(vp.getCode());
                    code.setValue(vp.getValue());
                    return code;
                })
                .collect(Collectors.toList());
    }
}
