package run.acloud.api.resource.enums;

import lombok.Getter;
import run.acloud.api.code.vo.CodeVO;
import run.acloud.commons.enums.EnumCode;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum CertIssuerScope implements EnumCode {
    CLUSTER("Cluster", K8sApiKindType.CLUSTER_ISSUER),
    NAMESPACED("Namespaced", K8sApiKindType.ISSUER)
    ;

    @Getter
    private String value;

    @Getter
    private K8sApiKindType kind;

    CertIssuerScope(String value, K8sApiKindType kind) {
        this.value = value;
        this.kind = kind;
    }

    @Override
    public String getCode() {
        return this.name();
    }

    public static CertIssuerScope codeOf(String code) {
        return CertIssuerScope.valueOf(code);
    }

    public static CertIssuerScope kindOf(K8sApiKindType kind) {
        return Arrays.stream(CertIssuerScope.values()).filter(s -> (s.getKind() == kind)).findFirst().orElseGet(() ->null);
    }

    public static List<String> getList() {
        return Arrays.stream(CertIssuerScope.values()).map(CertIssuerScope::getCode).collect(Collectors.toList());
    }

    public static List<CodeVO> getCodeList() {
        return Arrays.stream(CertIssuerScope.values())
                .map(vp -> {
                    CodeVO code = new CodeVO();
                    code.setGroupId("CERT_ISSUER_SCOPE");
                    code.setCode(vp.getCode());
                    code.setValue(vp.getValue());
                    return code;
                })
                .collect(Collectors.toList());
    }
}
