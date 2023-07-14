package run.acloud.api.billing.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import run.acloud.api.billing.enums.CurrencyCode;
import run.acloud.commons.vo.HasUseYnVO;

import java.math.BigDecimal;

@Getter
@Setter
@EqualsAndHashCode
public class AccountBillExchangeVO extends HasUseYnVO {

    @Schema(title = "청구서 환율 번호")
    private Integer billExchangeSeq;

    @Schema(title = "청구서 번호", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer billSeq;

    @Schema(title = "적용 환율 통화", requiredMode = Schema.RequiredMode.REQUIRED)
    private CurrencyCode exchangeCurrency;

    @Schema(title = "적용 환율 금액", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal exchangeAmt;

}
