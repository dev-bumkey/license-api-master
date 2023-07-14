package run.acloud.api.billing.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import run.acloud.api.billing.enums.BillState;
import run.acloud.api.billing.enums.CurrencyCode;
import run.acloud.commons.vo.HasUseYnVO;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@EqualsAndHashCode
public class AccountBillVO extends HasUseYnVO {
    @Schema(title = "청구서 번호")
    private Integer billSeq;

    @Schema(title = "계정 번호", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer accountSeq;

    @Schema(title = "계정명")
    private String accountName;

    @Schema(title = "계정 조직명")
    private String organizationName;

    @Schema(title = "사용시작일, YYYY/MM/DD", requiredMode = Schema.RequiredMode.REQUIRED)
    private String usedStartDate;

    @Schema(title = "사용종료일, YYYY/MM/DD", requiredMode = Schema.RequiredMode.REQUIRED)
    private String usedEndDate;

    @Schema(title = "사용월, YYYYMM", requiredMode = Schema.RequiredMode.REQUIRED)
    private String usedMonth;

    @Schema(title = "청구일자, YYYY/MM/DD", requiredMode = Schema.RequiredMode.REQUIRED)
    private String billDate;

    @Schema(title = "납부기한, YYYY/MM/DD", requiredMode = Schema.RequiredMode.REQUIRED)
    private String dueDate;

    @Schema(title = "적용통화", requiredMode = Schema.RequiredMode.REQUIRED)
    private CurrencyCode currency;

    @Schema(title = "청구금액", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal billAmt;

    @Schema(title = "부가세", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal additionalTax;

    @Schema(title = "부가세율", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer additionalTaxRate;

    @Schema(title = "최종 청구금액", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal finalBillAmt;

    @Schema(title = "청구서상태")
    private BillState billState = BillState.CREATED;

    @Schema(title = "적용환율들")
    private List<AccountBillExchangeVO> billExchanges;

    @Schema(title = "청구상품들", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<AccountBillProductVO> billProducts;


    //search property
    @Schema(title = "조회 account sequences", accessMode = Schema.AccessMode.READ_ONLY)
    private List<Integer> accountSeqs;

}
