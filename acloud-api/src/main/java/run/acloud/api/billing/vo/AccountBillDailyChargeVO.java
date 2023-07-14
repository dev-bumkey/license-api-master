package run.acloud.api.billing.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import run.acloud.commons.vo.HasUseYnVO;

import java.math.BigDecimal;

@Getter
@Setter
@Schema(description = "계정별 일별 과금 모델")
public class AccountBillDailyChargeVO extends HasUseYnVO {

    @Schema(title = "일별과금시퀀스")
    private Integer dailyChargeSeq;

    @Schema(title = "계정 번호", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer accountSeq;

    @Schema(title = "과금기준일자, timezone 상관없이 입력되는 값으로 그대로 들어감.", requiredMode = Schema.RequiredMode.REQUIRED)
    private String chargeBaseDate;

    @Schema(title = "추가 동시빌드 금액", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal addParallBuildAmt = new BigDecimal("0.00");

    @Schema(title = "추가 빌드 금액", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal addBuildAmt = new BigDecimal("0.00");

    @Schema(title = "추가 워크스페이스 금액", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal addWorkspaceAmt = new BigDecimal("0.00");

    @Schema(title = "Core 금액", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal coreAmt = new BigDecimal("0.00");

    @Schema(title = "Node 금액", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal nodeAmt = new BigDecimal("0.00");

    @Schema(title = "기타 금액", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal etcAmt = new BigDecimal("0.00");

    // 내부에서 사용하는 변수
    private String usedStartDate; // 과금시작일
    private String usedEndDate;   // 과금종료일
}
