package run.acloud.commons.enums;

/**
 * @author wschoi@acornsoft.io
 * Created on 2017. 6. 30.
 */
public enum HarborRegistryProjectMemberRole implements EnumCode {
    PROJECT_ADMIN(1),
    DEVELOPER(2),
    GUEST(3);

    private int value;

    HarborRegistryProjectMemberRole(int value) {
        this.value = value;
    }

    public int getValue() {
        return this.value;
    }

    public String getValueToString() {
        return String.valueOf(this.value);
    }

    @Override
    public String getCode() {
        return this.name();
    }

    public static HarborRegistryProjectMemberRole codeOf(String code) {
        return HarborRegistryProjectMemberRole.valueOf(code);
    }
}
