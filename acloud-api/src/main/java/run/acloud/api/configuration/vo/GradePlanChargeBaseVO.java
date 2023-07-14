package run.acloud.api.configuration.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import run.acloud.api.billing.enums.CurrencyCode;
import run.acloud.api.configuration.enums.BaseQtyUnit;
import run.acloud.api.configuration.enums.BaseTerm;
import run.acloud.api.configuration.enums.ChargeType;
import run.acloud.commons.vo.HasUseYnVO;

import java.math.BigDecimal;

@Getter
@Setter
@Schema(description = "등급별 과금 기본정보 모델")
public class GradePlanChargeBaseVO extends HasUseYnVO {

    @Schema(title = "과금 기준 시퀀스")
    private Integer chargeBaseSeq;

    @Schema(title = "등급 시퀀스", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer gradeSeq;

    @Schema(title = "과금영역명", requiredMode = Schema.RequiredMode.REQUIRED)
    private String chargeAreaName;

    @Schema(title = "과금명", requiredMode = Schema.RequiredMode.REQUIRED)
    private String chargeName;

    @Schema(title = "과금타입", requiredMode = Schema.RequiredMode.REQUIRED)
    private ChargeType chargeType;

    @Schema(title = "기준기간", requiredMode = Schema.RequiredMode.REQUIRED)
    private BaseTerm baseTerm;

    @Schema(title = "기준수량단위")
    private BaseQtyUnit baseQtyUnit;

    @Schema(title = "기준수량", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer baseQty;

    @Schema(title = "기준가격", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal basePrice;

    @Schema(title = "시간당 기준 가격", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal basePricePerHour;

    @Schema(title = "기준통화", requiredMode = Schema.RequiredMode.REQUIRED)
    private CurrencyCode baseCurrency;

    @Schema(title = "설명")
    private String description;

    @Schema(title = "정렬순서")
    private Integer sortOrder;

}
