package run.acloud.api.auth.enums;

import lombok.Getter;
import run.acloud.commons.enums.EnumCode;

/**
 * @author: wschoi@acornsoft.io
 * Created on 2017. 10. 24.
 */
public enum ServiceMode implements EnumCode {
    PRD("Production", true, true),
    HYDCARD("Hyundai Card", false, false),
    SECURITY_ONLINE("Security Online", true, true)
    ;

    @Getter
    private String value;

    @Getter
    private boolean addUser;

    @Getter
    private boolean addGrade;

    ServiceMode(String value, boolean addUser, boolean addGrade) {
        this.value = value;
        this.addUser = addUser;
        this.addGrade = addGrade;
    }

    @Override
    public String getCode() {
        return this.name();
    }
}
