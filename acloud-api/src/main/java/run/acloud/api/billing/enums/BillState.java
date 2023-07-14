package run.acloud.api.billing.enums;

import run.acloud.commons.enums.EnumCode;

public enum BillState implements EnumCode {
    CREATED,
    APPLY;

    @Override
    public String getCode() {
        return this.name();
    }
}
