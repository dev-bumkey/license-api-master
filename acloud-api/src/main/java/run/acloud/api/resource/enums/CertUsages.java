package run.acloud.api.resource.enums;

import lombok.Getter;
import run.acloud.api.code.vo.CodeVO;
import run.acloud.commons.enums.EnumCode;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum CertUsages implements EnumCode {
    SIGNING("signing"),

    DIGITAL_SIGNATURE("digital signature"),

    CONTENT_COMMITMENT("content commitment"),

    KEY_ENCIPHERMENT("key encipherment"),

    KEY_AGREEMENT("key agreement"),

    DATA_ENCIPHERMENT("data encipherment"),

    CERT_SIGN("cert sign"),

    CRL_SIGN("crl sign"),

    ENCIPHER_ONLY("encipher only"),

    DECIPHER_ONLY("decipher only"),

    ANY("any"),

    SERVER_AUTH("server auth"),

    CLIENT_AUTH("client auth"),

    CODE_SIGNING("code signing"),

    EMAIL_PROTECTION("email protection"),

    S_MIME("s/mime"),

    IPSEC_END_SYSTEM("ipsec end system"),

    IPSEC_TUNNEL("ipsec tunnel"),

    IPSEC_USER("ipsec user"),

    TIMESTAMPING("timestamping"),

    OCSP_SIGNING("ocsp signing"),

    MICROSOFT_SGC("microsoft sgc"),

    NETSCAPE_SGC("netscape sgc")
    ;

    @Getter
    private String value;

    CertUsages(String value) {
        this.value = value;
    }

    @Override
    public String getCode() {
        return this.name();
    }

    public static CertUsages codeOf(String code) {
        return CertUsages.valueOf(code);
    }

    public static CertUsages fromValue(String value) {
        for (CertUsages b : CertUsages.values()) {
            if (b.getValue().equals(value)) {
                return b;
            }
        }
        return null;
    }

    public static CertUsages fromCode(String code) {
        for (CertUsages b : CertUsages.values()) {
            if (b.getCode().equals(code)) {
                return b;
            }
        }
        return null;
    }

    public static List<String> getList() {
        return Arrays.stream(CertUsages.values()).map(CertUsages::getCode).collect(Collectors.toList());
    }

    public static List<CodeVO> getCodeList() {
        return Arrays.stream(CertUsages.values())
                .map(vp -> {
                    CodeVO code = new CodeVO();
                    code.setGroupId("CERT_USAGES");
                    code.setCode(vp.getCode());
                    code.setValue(vp.getValue());
                    return code;
                })
                .collect(Collectors.toList());
    }
}
