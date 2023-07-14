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
public class AccountBillProductVO extends HasUseYnVO {

    @Schema(title = "청구서 상품 번호")
    private Integer billPrdSeq;

    @Schema(title = "청구서 번호")
    private Integer billSeq;

    @Schema(title = "서비스명", requiredMode = Schema.RequiredMode.REQUIRED)
    private String svcNm;

    @Schema(title = "상품명", requiredMode = Schema.RequiredMode.REQUIRED)
    private String prdNm;

    @Schema(title = "상품통화", requiredMode = Schema.RequiredMode.REQUIRED)
    private CurrencyCode prdCurrency;

    @Schema(title = "상품금액", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal prdAmt;

    @Schema(title = "할인율", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer discountRate;

    @Schema(title = "상품청구금액", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal prdBillAmt;

    @Schema(title = "비고")
    private String description;

}
