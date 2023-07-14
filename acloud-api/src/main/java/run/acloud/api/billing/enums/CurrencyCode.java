package run.acloud.api.billing.enums;

import run.acloud.commons.enums.EnumCode;

public enum CurrencyCode implements EnumCode {
    KRW,
    USD,
    JPY,
    CNY;

    @Override
    public String getCode() {
        return this.name();
    }
}
