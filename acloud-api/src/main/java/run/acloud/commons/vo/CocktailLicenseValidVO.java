package run.acloud.commons.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import run.acloud.commons.enums.LicenseType;

@Schema(title="칵테일 license valid Model")
@Setter
@Getter
public class CocktailLicenseValidVO {

    @Schema(name = "License 유효성 여부", allowableValues = {"true","false"})
    private boolean isValid = false;

    @Schema(name = "License 만료 reminder 여부", allowableValues = {"true","false"}, description = "isValid 값이 true일 경우에만 유효, isReminder 값이 true일 경우 expireDate를 보여주고 칵테일 사용은 할 수 있음.")
    private boolean isReminder = false;

    @Schema(name = "License 만료 여부", allowableValues = {"true","false"}, description = "isValid 값이 true일 경우에만 유효, isExpired 값이 true일 경우 expireDate를 보여주고 칵테일 사용을 막음.")
    private boolean isExpired = true;

    @Schema(name = "timezone이 적용된 만료일", example = "yyyy-MM-dd")
    private String expireDate;

    @Schema(name = "칵테일 설치 기준 timezone", example = "Asia/Seoul")
    private String timeZone;

    @Schema(name = "Message")
    private String message;

    @Schema(name = "License 유형", allowableValues = {LicenseType.Names.TRIAL, LicenseType.Names.FULL})
    private String type = LicenseType.Names.FULL;
}
