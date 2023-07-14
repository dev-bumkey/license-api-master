package run.acloud.commons.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import run.acloud.commons.enums.LicenseType;

@Schema(title="칵테일 license Model")
@Setter
@Getter
public class CocktailLicenseVO {

    @Schema(name = "License 유효성 여부", allowableValues = {"true","false"})
    private boolean isValid;

    @Schema(name = "License 유형", allowableValues = {LicenseType.Names.TRIAL, LicenseType.Names.FULL})
    private String type;

    @Schema(name = "용도(ie. demo, enterprise, gcloud)")
    private String purpose;

    @Schema(name = "발급회사")
    private String issuer;

    @Schema(name = "고객회사")
    private String company;

    @Schema(name = "용량")
    private String capacity;

    @Schema(name = "region")
    private String region;

    @Schema(name = "발급일", example = "yyyy-MM-dd")
    private String issueDate;

    @Schema(name = "License 만료 여부", allowableValues = {"true","false"})
    private boolean isExpired;

    @Schema(name = "만료일", example = "yyyy-MM-dd")
    private String expireDate;

    @Schema(name = "칵테일 설치 기준 timezone", example = "Asia/Seoul")
    private String timeZone;

    @Schema(name = "licenseKey")
    private String licenseKey;
}
