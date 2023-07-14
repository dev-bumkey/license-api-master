package run.acloud.api.cserver.enums;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import run.acloud.commons.enums.EnumCode;

import java.util.Arrays;

/**
 * @author wschoi@acornsoft.io
 * Created on 2017. 6. 30.
 */
public enum ReclaimPolicy implements EnumCode {
    RECYCLE("Recycle"),
    RETAIN("Retain"),
    DELETE("Delete");

    @Getter
    private String value;

    ReclaimPolicy(String value) {
        this.value = value;
    }

    public static ReclaimPolicy getReclaimPolicyOfValue(String value){
        return Arrays.stream(ReclaimPolicy.values()).filter(rp -> (StringUtils.equalsIgnoreCase(value, rp.value))).findFirst().orElseGet(() ->null);
    }

    @Override
    public String getCode() {
        return this.name();
    }
}
