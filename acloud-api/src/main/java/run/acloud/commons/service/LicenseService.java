package run.acloud.commons.service;

import javax0.license3j.Feature;
import javax0.license3j.License;
import javax0.license3j.crypto.LicenseKeyPair;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import run.acloud.api.code.service.CodeService;
import run.acloud.api.code.vo.CodeVO;
import run.acloud.commons.enums.LicenseExpirePeriodType;
import run.acloud.commons.enums.LicenseKeyItem;
import run.acloud.commons.enums.LicenseType;
import run.acloud.commons.util.Base64Utils;
import run.acloud.commons.util.CryptoUtils;
import run.acloud.commons.util.DateTimeUtils;
import run.acloud.commons.vo.CocktailLicenseVO;
import run.acloud.commons.vo.CocktailLicenseValidVO;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;
import run.acloud.framework.properties.CocktailLicenseProperties;
import run.acloud.framework.properties.CocktailServiceProperties;
import run.acloud.framework.util.ExceptionMessageUtils;

import java.lang.reflect.Modifier;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.Optional;
import java.util.TimeZone;

@Slf4j
@Service
public class LicenseService {

    @Autowired
    private CodeService codeService;

    @Autowired
    private CocktailServiceProperties cocktailServiceProperties;

    @Autowired
    private CocktailLicenseProperties licenseProperties;

    private static final String DEFAULT_DATE_FORMAT = "yyyyMMdd";
    private static final String DEFAULT_DISPLAY_DATE_FORMAT = "yyyy-MM-dd";

    private static final String publicKey = "PzjT/fQgdQyiNv5JqMMuN62wp7/KwC9vuQ5gw2ANCgNtuXOkx8MerwKEqqtLt09lBgYCgTiD2o2wt0mK8xej7ACr1jxKtx2I48IVd7zKBAdE353R9tffaXGaOJ5jDhDJZhSYG1Uvb0tEx5/DFFk4C3vjjqHb7okNnF9IkMTUNXaj3qUooiblhLvc5XhxhqL+VYgjkRjzh9wwRscVwi8Falx0fLFen+5D4+VcG/ak/WhjXNlKs42BC/u+s4KBAbtyoWormuoC16x4EvyblLQY2aoIsjLBM9vmOinWSqop4XY3a1cVMGZ3PjDVPKwqSAQ1GzJihcLw1rtScil6nyorHHCQd50bDU+lejsoP9iBHWcwp3WX";
    private static final String privateKey = "4ma/TZPcRtDPHFJ/+vfWybHSHiqmQjxApG35rOa9PGh8+kdgnVhyxlsr0cDhOVLEz3UqaNiCdpUEF2avjUaXrnDCPEcbB8De4gJNflJXTRBGSJc0VNir2Nz/cvOgEleRcbxYYSJtKLkwxKE6Wcke7UcnB2MvTcBXYVsO1RPDvPR49Fd90C47R14vXVqIN+N14qLgwTEQ1nCzlBnBnfLNrT1lc4Qq3vngGu+U/YfUcwlttXrRndWmvK+s94mxMMWpGWdB7skUZ5W5hfntcRshxY8k/NWRkhC1Wznm5lJlLrLLGDb6eCmjPFDJzFHVM4V/8wEuD2g983yRe9yoGUAqRjhG2+PH/QwGH/uhXd4Q7mbL2KKyCykIF141DJNIaj86l2LnXg0NIPgOSqsgAiaVz4ikPUeZBBd7LlJyJcf8NK1rZw6xsYp+2HZhJ9pfMtChVeMquPfaL7r+XX42lKpaOutTJiEiB5836DyHHTajOdQWhLCPr7ZpXaC3S9FV0Tasp+Y3GwBUj/na9OLUJKs9GJ+Sc15w85cPURrA+EaNV4ToeJXe+szT9rs3VnpVDS/E9Rpk3VpqbS5X3kaDytFNiOqPBbG5uZjoCEMuQe5X6rCaruR7xdJ5BjxE92/YaY0s/QUp1yj2Fa5prDi9PrKSBadFiq90j3XWKMO3rHLNkDXvP/G4imcdVFlT9RUtutQW+eSdhg0p/MwYIy5kd6bUZtaAnKBLY+gr5b/v4ws841Jlof1qAnacQgaY8pVgCgaap9ISOSWE8TUGlOubd7Ov+mToswqxeq7D3NZo+FTQM7ErDpLXBvAzqsWTVfY9vwNTe9T+Q/tCthEUYZjOxCgxZbtY1w3mfP+jihDckjnUNGDV+dHTqhDZz89FAzMH7MJ2Lp+97kh63IBXRGPQWJRkWWWtphOBEv/gDafRVrd+QGNnnnUvlom9NAiEsLYUmHnZMHw4obWa94IDMwpVjVi91tzEQaNyYAaKyo++q2Hgk3EufWHn//46X0jgGKbbjWuvrNlqfkYEm4VgrxqUgZOezTdbgEM276+Hxm4oQsS20OewF1Guq5wlv8vawYv0J/AwHS/f5OU7wcUMmN9X/0ifO2Oi7txXkfdre4FWoK2Qv4tMGIr2xBc2Kh2bOMLeHao3FDkjsXdbXaF4JL+mjHK37Y7Ybaoh/YxsB6/aY+lnJZ+N74vz";
    /**
     * License 유효성 체크
     *
     * @return 라이센스 유효성 체크 결과
     * @throws Exception
     */
    public CocktailLicenseValidVO checkLicense() throws Exception {
        String encodedLicense = null;
        if (StringUtils.isNotBlank(licenseProperties.getLicenseKey())) {
            encodedLicense = licenseProperties.getLicenseKey();

        } else {
            CodeVO codeKey = this.getCodeForTrialLicense();
            if (codeKey != null) {
                encodedLicense = codeKey.getDescription();
            } else {
                return null;
            }
        }

        return this.checkLicense(encodedLicense);
    }

    /**
     * License 유효성 체크
     *
     * @param encodedLicense 인코딩된 라이센스
     * @return 라이센스 유효성 체크 결과
     * @throws Exception
     */
    public CocktailLicenseValidVO checkLicense(String encodedLicense) throws Exception {

        CocktailLicenseValidVO valid = new CocktailLicenseValidVO();
        valid.setValid(false);
        valid.setReminder(false);
        valid.setExpired(true);
        valid.setTimeZone(cocktailServiceProperties.getRegionTimeZone());

        if (StringUtils.isNotBlank(encodedLicense)) {
            try {
                // parse license
                License license = License.Create.from(Base64Utils.decodeFromString(encodedLicense));

                if (license.isOK(Base64Utils.decodeFromString(CryptoUtils.decryptDefaultAES(publicKey)))) {
                    // set license type ( default: FULL )
                    valid.setType(this.getFeatureToString(license, LicenseKeyItem.TYPE));

                    LicenseType licenseType = LicenseType.valueOf(valid.getType());

                    // signature is valid
                    valid.setValid(true);

                    // check valid expire date
                    if (license.get(LicenseKeyItem.EXPIRY_DATE.getValue()) != null) {
                        String expiryDate = DateTimeUtils.getTimeString(license.get(LicenseKeyItem.EXPIRY_DATE.getValue()).getDate(), cocktailServiceProperties.getRegionTimeZone(), DEFAULT_DISPLAY_DATE_FORMAT);
                        valid.setExpireDate(expiryDate);
                        if (license.isExpired()) {
                            this.setMessage(valid, String.format("License expired. (expiration date : %s)", valid.getExpireDate()));
                        } else {
                            valid.setExpired(false);

                            // License 만료일 reminder 처리
                            // 정식 License는 30일전
                            int checkExpirePeriod = 30;
                            // Trial License는 기본 만료일 / 3 일 전으로 체크
                            // 만약, 30일보다 크다면 30일로 고정
                            if (licenseType == LicenseType.TRIAL) {
                                if (licenseProperties.getInitExpirePeriodDays() > 3) {
                                    checkExpirePeriod = Math.floorDiv(licenseProperties.getInitExpirePeriodDays(), 3);
                                    if (checkExpirePeriod > 30) {
                                        checkExpirePeriod = 30;
                                    }
                                } else {
                                    checkExpirePeriod = 1;
                                }
                            }

                            Date reminderDate = DateTimeUtils.addDate(new Date(), 0, 0, checkExpirePeriod);
                            if (reminderDate.getTime() > license.get(LicenseKeyItem.EXPIRY_DATE.getValue()).getDate().getTime()) {
                                valid.setReminder(true);
                            }
                        }
                    } else {
                        // 만료일이 셋팅되지 않은 license는 invalid 처리
                        valid.setValid(false);
                        this.setMessage(valid, "The license expiration date is not set.");
                    }
                } else {
                    this.setMessage(valid, "Invalid License!!");
                }
            } catch (Exception e) {
                throw new CocktailException("An error occurred while checking the license.", e, ExceptionType.InvalidLicense);
            }
        } else {
            this.setMessage(valid, "License is empty!!");
        }

        return valid;
    }

    /**
     * License 조회
     *
     * @return License 정보
     */
    public CocktailLicenseVO getLicense() throws Exception {
        CocktailLicenseVO licenseVO = null;
        String encodedLicense = null;
        // 정식 License
        if (StringUtils.isNotBlank(licenseProperties.getLicenseKey())) {
            encodedLicense = licenseProperties.getLicenseKey();
        }
        // Trial License
        else {
            CodeVO codeKey = this.getCodeForTrialLicense();
            if (codeKey != null) {
                encodedLicense = codeKey.getDescription();
            }
        }

        if (StringUtils.isNotBlank(encodedLicense)) {
            try {
                // parse license
                License license = License.Create.from(Base64Utils.decodeFromString(encodedLicense));

                if (license != null) {
                    licenseVO = new CocktailLicenseVO();
                    licenseVO.setValid(license.isOK(Base64Utils.decodeFromString(CryptoUtils.decryptDefaultAES(publicKey))));
                    licenseVO.setType(this.getFeatureToString(license, LicenseKeyItem.TYPE));
                    licenseVO.setPurpose(this.getFeatureToString(license, LicenseKeyItem.PURPOSE));
                    licenseVO.setIssuer(this.getFeatureToString(license, LicenseKeyItem.ISSUER));
                    licenseVO.setCompany(this.getFeatureToString(license, LicenseKeyItem.COMPANY));
                    licenseVO.setCapacity(this.getFeatureToString(license, LicenseKeyItem.CAPACITY));
                    licenseVO.setRegion(this.getFeatureToString(license, LicenseKeyItem.REGION));
                    licenseVO.setIssueDate(this.getFeatureToString(license, LicenseKeyItem.ISSUE_DATE));
                    licenseVO.setExpired(license.isExpired());
                    licenseVO.setExpireDate(this.getFeatureToString(license, LicenseKeyItem.EXPIRY_DATE));
                    licenseVO.setTimeZone(cocktailServiceProperties.getRegionTimeZone());
                    licenseVO.setLicenseKey(encodedLicense);
                }
            } catch (Exception e) {
                throw new CocktailException("An error occurred while inquiring the license.", e, ExceptionType.InvalidLicense);
            }
        }

        return licenseVO;
    }

    /**
     * Trial license 발급
     *
     * @param expirePeriodDays (optional) 만료 기간 일수, 기본 설정값 대신 설정할 경우 셋팅
     * @param checkReadiness readiness 체크 여부
     * @param isReissue 재발급 여부
     * @param isThrow exception throw 여부
     * @throws Exception
     */
    public void issueTrialLicense(Integer expirePeriodDays, boolean checkReadiness, boolean isReissue, boolean isThrow) throws Exception {
        if (BooleanUtils.toBoolean(licenseProperties.isLicenseEnable())) {
            if (StringUtils.isBlank(licenseProperties.getLicenseKey())) {
                // readiness 체크 (database is ready?)
                int errorCnt = 0;
                if (checkReadiness) {
                    try {
                        CodeVO code = codeService.getCode("PROVIDER", "AWS");
                        if (code == null) {
                            errorCnt += 1;
                        }
                    } catch (Exception e) {
                        errorCnt += 1;
                        CocktailException ce = new CocktailException("An error occurred while checking the license.(api-server is not ready))", e, ExceptionType.CommonCreateFail);
                        if (isThrow) {
                            throw ce;
                        } else {
                            log.error(ce.getMessage(), ce);
                        }
                    }
                }

                // readiness is valid
                if (errorCnt == 0) {
                    // 발급된 trial key 존재여부 확인
                    CodeVO codeKey = this.getCodeForTrialLicense();
                    Integer expirePeriodValue = Optional.ofNullable(expirePeriodDays).orElseGet(() -> licenseProperties.getInitExpirePeriodDays());
                    if (codeKey == null) {
                        try {
                            codeKey = new CodeVO();
                            codeKey.setGroupId("LICENSE_KEY");
                            codeKey.setCode(LicenseType.Names.TRIAL);
                            codeKey.setValue(String.valueOf(expirePeriodValue));
                            codeKey.setDescription(this.issueLicense(LicenseExpirePeriodType.DAY, expirePeriodValue.longValue(), LicenseType.TRIAL, "trial", "trial", null, null, null, null));
                            int result = codeService.addCode(codeKey);
                            if (result > 0) {
                                log.info("Trial License Key Created Success.");
                            } else {
                                log.warn("Trial License Key Created Fail.");
                            }
                        } catch (Exception e) {
                            CocktailException ce = new CocktailException("An error occurred while checking the license.", e, ExceptionType.CommonCreateFail);
                            if (isThrow) {
                                throw ce;
                            } else {
                                log.error(ce.getMessage(), ce);
                            }
                        }
                    } else {
                        if (isReissue) {
                            try {
                                codeKey.setValue(String.valueOf(expirePeriodValue));
                                codeKey.setDescription(this.issueLicense(LicenseExpirePeriodType.DAY, expirePeriodValue.longValue(), LicenseType.TRIAL, "trial", "trial", null, null, null, null));
                                int result = codeService.editCodeForTrialLicense(codeKey);
                                if (result > 0) {
                                    log.info("Trial License Key Update Success.");
                                } else {
                                    log.warn("Trial License Key Update Fail.");
                                }
                            } catch (Exception e) {
                                CocktailException ce = new CocktailException("An error occurred while checking the license.", e, ExceptionType.CommonCreateFail);
                                if (isThrow) {
                                    throw ce;
                                } else {
                                    log.error(ce.getMessage(), ce);
                                }
                            }
                        }
                    }
                }
            } else {
                if (checkReadiness) {
                    log.info("A full license already exists.");
                }
            }
        }
    }

    /**
     * Licensse 발급
     *
     * @param expirePeriodType 라이센스 만료 기간 유형 (YEAR, MONTH, DAY)
     * @param expirePeriodValue 라이센스 만료 기간
     * @param licenseType 라이센스 유형(Trial, Full)
     * @param purpose 발급용도 (ie. demo, enterprise, gcloud)
     * @param issuer 라이센스 발급 회사
     * @param company 고객 회사
     * @param capacity 용량 (ie. 3, 300)
     * @param region 라이센스 발급하는 리전 (ie. KR, JP, CN)
     * @param issueDate license 발급일
     * @return - LicenseKey
     * @throws Exception
     */
    public String issueLicense(
            LicenseExpirePeriodType expirePeriodType, Long expirePeriodValue
            , LicenseType licenseType
            , String purpose, String issuer, String company, Integer capacity, String region, String issueDate
    ) throws Exception {
        this.checkDateFormat(issueDate);
        return this.issueLicense(expirePeriodType, expirePeriodValue, licenseType, purpose, issuer, company, capacity, region, issueDate, null);
    }

    /**
     * Licensse 발급
     *
     * @param licenseType 라이센스 유형(Trial, Full)
     * @param purpose 발급용도 (ie. demo, enterprise, gcloud)
     * @param issuer 라이센스 발급 회사
     * @param company 고객 회사
     * @param capacity 용량 (ie. 3, 300)
     * @param region 라이센스 발급하는 리전 (ie. KR, JP, CN)
     * @param issueDate license 발급일
     * @param expiryDate license 만료일
     * @return - LicenseKey
     * @throws Exception
     */
    public String issueLicense(
            LicenseType licenseType
            , String purpose, String issuer, String company, Integer capacity, String region, String issueDate, String expiryDate
    ) throws Exception {
        this.checkDateFormat(issueDate);
        this.checkDateFormat(expiryDate);
        return this.issueLicense(null, null, licenseType, purpose, issuer, company, capacity, region, issueDate, expiryDate);
    }

    /**
     * Licensse 발급
     *
     * @param expirePeriodType 라이센스 만료 기간 유형 (YEAR, MONTH, DAY)
     * @param expirePeriodValue 라이센스 만료 기간
     * @param licenseType 라이센스 유형(Trial, Full)
     * @param purpose 발급용도 (ie. demo, enterprise, gcloud)
     * @param issuer 라이센스 발급 회사
     * @param company 고객 회사
     * @param capacity 용량 (ie. 3, 300)
     * @param region 라이센스 발급하는 리전 (ie. KR, JP, CN)
     * @param issueDate license 발급일 (ie. 20220913)
     * @param expiryDate license 만료일 (ie. 20221231)
     * @return - LicenseKey
     * @throws Exception
     */
    private String issueLicense(
            LicenseExpirePeriodType expirePeriodType, Long expirePeriodValue
            , LicenseType licenseType
            , String purpose, String issuer, String company, Integer capacity, String region, String issueDate, String expiryDate
    ) throws Exception {
        String base64License = null;

        if (BooleanUtils.toBoolean(licenseProperties.isLicenseEnable())) {
            License license = License.Create.from("");

            // LicenseType
            license.add(Feature.Create.stringFeature(LicenseKeyItem.TYPE.getValue(), licenseType.getCode()));
            // purpose(용도)
            if (StringUtils.isBlank(purpose)) {
                purpose = "demo";
            }
            ExceptionMessageUtils.checkParameter("purpose", purpose, 50, false);
            license.add(Feature.Create.stringFeature(LicenseKeyItem.PURPOSE.getValue(), purpose));
            // issuer
            if (StringUtils.isBlank(issuer)) {
                issuer = "issuer";
            }
            ExceptionMessageUtils.checkParameter("issuer", issuer, 50, false);
            license.add(Feature.Create.stringFeature(LicenseKeyItem.ISSUER.getValue(), issuer));
            // company
            if (StringUtils.isBlank(company)) {
                company = "customer";
            }
            ExceptionMessageUtils.checkParameter("company", company, 50, false);
            license.add(Feature.Create.stringFeature(LicenseKeyItem.COMPANY.getValue(), company));
            // capacity
            if (capacity != null) {
                if (capacity.intValue() > 999999) {
                    throw new CocktailException("Value is overflow.", ExceptionType.InvalidParameter_Overflow, "Value is overflow. (max: 999999)");
                }
                license.add(Feature.Create.intFeature(LicenseKeyItem.CAPACITY.getValue(), capacity));
            }
            // region
            if (StringUtils.isBlank(region)) {
                region = "KR";
            }
            license.add(Feature.Create.stringFeature(LicenseKeyItem.REGION.getValue(), region));



            // issue date
            String issueDateStr = "";
            if (StringUtils.isNotBlank(issueDate)) {
                issueDateStr = issueDate;
            } else {
                ZonedDateTime issueZonedDateTime = ZonedDateTime.now(ZoneId.of(cocktailServiceProperties.getRegionTimeZone()));
                issueDateStr = issueZonedDateTime.format(java.time.format.DateTimeFormatter.ofPattern(DEFAULT_DATE_FORMAT));
            }
            try {
                DateFormat issueDf = new SimpleDateFormat(DEFAULT_DATE_FORMAT);
                issueDf.setTimeZone(TimeZone.getTimeZone("UTC"));
                Date utcDate = issueDf.parse(issueDateStr);
                license.add(Feature.Create.dateFeature(LicenseKeyItem.ISSUE_DATE.getValue(), utcDate));
            } catch (ParseException e) {
                throw new CocktailException("Invalid data format.", e, ExceptionType.InvalidParameter_DateFormat, "Invalid data format.(ie. yyyyMMdd)");
            }

            // 만료일 설정
            // 설정된 timezone으로 yyyy-MM-dd 형식의 현재 날짜를 얻음.
            String expiredDateStr = "";
            ZonedDateTime expiryZonedDateTime = ZonedDateTime.now(ZoneId.of(cocktailServiceProperties.getRegionTimeZone()));
            if (StringUtils.isNotBlank(expiryDate)) {
                expiredDateStr = expiryDate;
            } else if (expirePeriodType != null && (expirePeriodValue != null && expirePeriodValue.longValue() > 0L)) {

                switch (expirePeriodType) {
                    case YEAR:
                        expiryZonedDateTime = expiryZonedDateTime.plusYears(expirePeriodValue.longValue());
                        break;
                    case MONTH:
                        expiryZonedDateTime = expiryZonedDateTime.plusMonths(expirePeriodValue.longValue());
                        break;
                    case DAY:
                        expiryZonedDateTime = expiryZonedDateTime.plusDays(expirePeriodValue.longValue());
                        break;
                }
                expiredDateStr = expiryZonedDateTime.format(java.time.format.DateTimeFormatter.ofPattern(DEFAULT_DATE_FORMAT));
            } else {
                expiryZonedDateTime = expiryZonedDateTime.plusYears(1);
                expiredDateStr = expiryZonedDateTime.format(java.time.format.DateTimeFormatter.ofPattern(DEFAULT_DATE_FORMAT));
            }

            try {
                // Date 타입으로 만료일을 체크하므로 Date 타입으로 변경
                Calendar cal = Calendar.getInstance();
                DateFormat df = new SimpleDateFormat(DEFAULT_DATE_FORMAT);
//                df.setTimeZone(TimeZone.getTimeZone(cocktailServiceProperties.getRegionTimeZone()));
//                Date customerDate = df.parse(expiredDateStr);
//                log.debug("license date({}): {}", cocktailServiceProperties.getRegionTimeZone(), customerDate);

                // api-server system timezone 'UTC'로 변경하여 셋팅
                df.setTimeZone(TimeZone.getTimeZone("UTC"));
//                Date utcDate = df.parse(df.format(customerDate));
                Date utcDate = df.parse(expiredDateStr);
                log.debug("license date({}): {}", "UTC", utcDate);
                cal.setTime(utcDate);
                license.setExpiry(cal.getTime());
            } catch (ParseException e) {
                throw new CocktailException("Invalid data format.", e, ExceptionType.InvalidParameter_DateFormat, "Invalid data format.(ie. yyyyMMdd)");
            }

            // 발급일과 만료일 체크
            if (license.get(LicenseKeyItem.ISSUE_DATE.getValue()) != null && license.get(LicenseKeyItem.EXPIRY_DATE.getValue()) != null) {
                if (license.get(LicenseKeyItem.ISSUE_DATE.getValue()).getDate().getTime() > license.get(LicenseKeyItem.EXPIRY_DATE.getValue()).getDate().getTime()) {
                    throw new CocktailException("Invalid issue and expiry date.", ExceptionType.InvalidParameter, "Expiry date is earlier than issue date.");
                }
            }

            // set license uuid
            license.setLicenseId();
            // signature license
            license.sign(LicenseKeyPair.Create.from(Base64Utils.decodeFromString(CryptoUtils.decryptDefaultAES(privateKey)), Modifier.PRIVATE).getPair().getPrivate(), "SHA-512");
            // encode base64 license
            base64License = Base64Utils.encodeToString(license.serialized());
        }

        return base64License;
    }

    public void checkDateFormat(String dateString) throws Exception {
        if (StringUtils.isNotBlank(dateString)) {
            try {
                DateFormat df = new SimpleDateFormat(DEFAULT_DATE_FORMAT);
                df.parse(dateString);
            } catch (ParseException e) {
                throw new CocktailException("Invalid data format.", e, ExceptionType.InvalidParameter_DateFormat, "Invalid data format.(ie. yyyyMMdd)");
            }
        }
    }

    private String getFeatureToString(License license, LicenseKeyItem key) {
        if (license != null) {
            switch (key) {
                case TYPE:
                    if (license.get(key.getValue()) != null) {
                        return license.get(key.getValue()).valueString();
                    } else {
                        return LicenseType.Names.FULL;
                    }
                case ISSUE_DATE:
                    if (license.get(key.getValue()) != null) {
                        return DateTimeUtils.getTimeString(license.get(LicenseKeyItem.ISSUE_DATE.getValue()).getDate(), cocktailServiceProperties.getRegionTimeZone(), DEFAULT_DISPLAY_DATE_FORMAT);
                    }
                case EXPIRY_DATE:
                    if (license.get(key.getValue()) != null) {
                        return DateTimeUtils.getTimeString(license.get(LicenseKeyItem.EXPIRY_DATE.getValue()).getDate(), cocktailServiceProperties.getRegionTimeZone(), DEFAULT_DISPLAY_DATE_FORMAT);
                    }
                case PURPOSE:
                case ISSUER:
                case COMPANY:
                case CAPACITY:
                case REGION:
                    if (license.get(key.getValue()) != null) {
                        return license.get(key.getValue()).valueString();
                    }
            }
        }

        return null;
    }

    private CodeVO getCodeForTrialLicense() {
        return codeService.getCode("LICENSE_KEY", LicenseType.Names.TRIAL);
    }

    private void setMessage(CocktailLicenseValidVO valid, String msg) {
        valid.setMessage(msg);
        log.warn(valid.getMessage());
    }
}
