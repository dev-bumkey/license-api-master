package run.acloud.api.resource.enums;

import lombok.Getter;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import run.acloud.api.resource.constants.KubeConstants;
import run.acloud.commons.enums.EnumCode;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author wschoi@acornsoft.io
 * Created on 2017. 8. 29.
 */
public enum SecretType implements EnumCode {
    Generic(KubeConstants.SECRET_TYPE_OPAQUE, "Generic", "Y", "Y", null),
    DockerRegistry(KubeConstants.SECRET_TYPE_DOCKERCONFIGJSON, "DockerRegistry", "Y", "Y", Arrays.asList(StringUtils.split(".dockerconfigjson", "|"))),
    Tls(KubeConstants.SECRET_TYPE_TLS, "TLS", "Y", "Y", Arrays.asList(StringUtils.split("tls.crt|tls.key", "|"))),
    BasicAuth(KubeConstants.SECRET_TYPE_BASIC_AUTH, "Basic Auth", "N", "Y", Arrays.asList(StringUtils.split("username|password", "|"))),
    SshAuth(KubeConstants.SECRET_TYPE_SSH_AUTH, "SSH Auth", "N", "Y", Arrays.asList(StringUtils.split("ssh-privatekey", "|")))
    ;


    @Getter
    String value;

    @Getter
    String displayName;

    @Getter
    String showFlag;

    @Getter
    String useFlag;

    @Getter
    List<String> keys;

    SecretType(String value, String displayName, String showFlag, String useFlag, List<String> keys){
        this.value = value;
        this.displayName = displayName;
        this.showFlag = showFlag;
        this.useFlag = useFlag;
        this.keys = keys;
    }

    public Map<String, Object> toMap(){
        Map<String, Object> secretTypesMap = new HashMap<>();
        secretTypesMap.put("code", this.getCode());
        secretTypesMap.put("displayName", this.getDisplayName());
//        secretTypesMap.put("value", this.getValue());
//        secretTypesMap.put("showFlag", this.getShowFlag());
//        secretTypesMap.put("useFlag", this.getUseFlag());
        secretTypesMap.put("keys", this.getKeys());

        return secretTypesMap;
    }

    @Override
    public String getCode() {
        return this.name();
    }

    public static SecretType codeOf(String code) {
        return SecretType.valueOf(code);
    }

    public static List<Map<String, Object>> getSupportedSecretTypes() {
        return Arrays.asList(SecretType.values()).stream().filter(st -> (BooleanUtils.toBoolean(st.getShowFlag()) && BooleanUtils.toBoolean(st.getUseFlag()))).map(st -> st.toMap()).collect(Collectors.toList());
    }

    public static Set<String> getSupportedSecretTypesValue() {
        return Arrays.asList(SecretType.values()).stream().map(st -> st.getValue()).collect(Collectors.toSet());
    }

    public static Optional<SecretType> secretTypeOf(String secretType) {
        return Arrays.asList(SecretType.values()).stream().filter(st -> st.getValue().equalsIgnoreCase(secretType)).findFirst();
    }
}
