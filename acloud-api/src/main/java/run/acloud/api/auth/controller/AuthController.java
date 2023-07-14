package run.acloud.api.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import run.acloud.api.auth.service.LoginService;
import run.acloud.api.auth.vo.AuthVO;
import run.acloud.api.auth.vo.UserVO;
import run.acloud.api.configuration.service.AccountApplicationService;
import run.acloud.api.configuration.service.UserService;
import run.acloud.commons.annotations.InHouse;
import run.acloud.commons.client.ADApiClient;
import run.acloud.commons.enums.LicenseExpirePeriodType;
import run.acloud.commons.enums.LicenseType;
import run.acloud.commons.service.LicenseService;
import run.acloud.commons.util.AuthUtils;
import run.acloud.commons.util.CryptoUtils;
import run.acloud.commons.vo.ADVO;
import run.acloud.commons.vo.CocktailLicenseVO;
import run.acloud.commons.vo.ResultVO;
import run.acloud.framework.context.ContextHolder;
import run.acloud.framework.properties.CocktailServiceProperties;

import java.io.IOException;

/**
 * @author wschoi@acornsoft.io
 * Created on 2017. 3. 8.
 */
@Tag(name = "Auth", description = "인증에 대한 관리 기능을 제공한다.")
@Slf4j
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private LoginService loginService;

    @Autowired
    private LicenseService licenseService;

    @Autowired
    private ADApiClient adApiClient;

    @Autowired
    private CocktailServiceProperties cocktailServiceProperties;

    @Autowired
    private AccountApplicationService accountApplicationService;


    @PostMapping("/login")
    @Operation(summary = "사용자 로그인", description = "사용자 로그인")
    public UserVO login(
            @RequestHeader(name="isEncrypted", defaultValue="false") boolean isEncrypted,
            @Validated @RequestBody AuthVO auth, HttpSession session) throws Exception
    {
        log.debug("[BEGIN] login");

        // isEncrypted가 true(암호화 되어 있다면) 복호화 하여 패스워드 다시 셋팅
        if(isEncrypted && StringUtils.isNotBlank(auth.getPassword())){
            // 패스워드 복호화 후 다시 셋팅
            String decryptedPasswd = CryptoUtils.decryptRSA(auth.getPassword());
            auth.setPassword(decryptedPasswd);
        }

        /**
         * 로그인 유형
         * - ADMIN(ADMIN) - ADMIN 권한자
         * - 멤버(DEVOPS) - SYSTEM, DEVOPS 권한자
         *
         * 로그인 유형이 '멤버'으로 접속하면 다시 조회하여 사용자 권한에 따라 로그인 처리
         */
        UserVO user = loginService.login(auth);
        if (user != null) {
            user.setToken(session.getId());
        }

        log.debug("[END  ] login");

        return user;
    }

    /**
     * 칵테일 관리자용 로그인
     *
     * @param auth
     * @param session
     * @return
     * @throws Exception
     */
    @PostMapping("/admin/login")
    @Operation(summary = "칵테일 어드민 로그인", description = "칵테일 어드민 로그인")
    public UserVO loginCocktailAdmin(
             @RequestHeader(name="isEncrypted", defaultValue="false") boolean isEncrypted,
             @Validated @RequestBody AuthVO auth,
             HttpSession session) throws Exception
    {
        log.debug("[BEGIN] loginCocktailAdmin");

        // isEncrypted가 true(암호화 되어 있다면) 복호화 하여 패스워드 다시 셋팅
        if(isEncrypted && StringUtils.isNotBlank(auth.getPassword())){
            // 패스워드 복호화 후 다시 셋팅
            String decryptedPasswd = CryptoUtils.decryptRSA(auth.getPassword());
            auth.setPassword(decryptedPasswd);
        }

        UserVO user = loginService.login(auth, AuthUtils.checkUserAdminRolePredicate());
        if (user != null) {
            user.setToken(session.getId());
        }

        log.debug("[END  ] loginCocktailAdmin");

        return user;
    }

    /**
     * 플랫폼 관리자용 로그인
     *
     * @param auth
     * @param session
     * @return
     * @throws Exception
     */
    @PostMapping("/platform/admin/login")
    @Operation(summary = "플랫폼 어드민 로그인", description = "플랫폼 어드민 로그인")
    public UserVO loginPlatformAdmin(
            @RequestHeader(name="isEncrypted", defaultValue="false") boolean isEncrypted,
            @Validated @RequestBody AuthVO auth,
            HttpSession session) throws Exception
    {
        log.debug("[BEGIN] loginPlatformAdmin");

        UserVO user = null;

        // isEncrypted가 true(암호화 되어 있다면) 복호화 하여 패스워드 다시 셋팅
        if(isEncrypted && StringUtils.isNotBlank(auth.getPassword())){
            // 패스워드 복호화 후 다시 셋팅
            String decryptedPasswd = CryptoUtils.decryptRSA(auth.getPassword());
            auth.setPassword(decryptedPasswd);
        }

        // Admin 사용자가 아니고 디지털 서비스일 경우 신청서 조회하여 존재시 로그인 처리, 2022/02/18 주석처리 - 2022/02/17일에 신청서 관련 오프라인 처리로 결정
//        if ( !UserRole.ADMIN.getCode().equals(auth.getRole())
//                && ServiceMode.valueOf(cocktailServiceProperties.getMode()) == ServiceMode.SECURITY_ONLINE
//        ) {
//            UserVO applicationLoginUser = accountApplicationService.login(auth);
//
//            // 신청서로 로그인인 경우 리턴
//            if(applicationLoginUser != null){
//                user = applicationLoginUser;
//            }
//        }

        // 신청서 로그인 실패시에만 처리
        if (user == null) {
            user = loginService.login(auth, AuthUtils.checkUserSystemRolePredicate());
            if (user != null) {
                user.setToken(session.getId());
            }
        }

        log.debug("[END  ] loginPlatformAdmin");

        return user;
    }

    /**
     * 플랫폼 사용자용 로그인
     *
     * @param auth
     * @param session
     * @return
     * @throws Exception
     */
    @PostMapping("/platform/user/login")
    @Operation(summary = "플랫폼 사용자 로그인", description = "플랫폼 사용자 로그인")
    public UserVO loginPlatformUser(
            @RequestHeader(name="isEncrypted", defaultValue="false") boolean isEncrypted,
            @Validated @RequestBody AuthVO auth,
            HttpSession session) throws Exception
    {
        log.debug("[BEGIN] loginPlatformUser");

        // isEncrypted가 true(암호화 되어 있다면) 복호화 하여 패스워드 다시 셋팅
        if(isEncrypted && StringUtils.isNotBlank(auth.getPassword())){
            // 패스워드 복호화 후 다시 셋팅
            String decryptedPasswd = CryptoUtils.decryptRSA(auth.getPassword());
            auth.setPassword(decryptedPasswd);
        }

        UserVO user = loginService.login(auth, AuthUtils.checkUserDevopsRolePredicate());
        if (user != null) {
            user.setToken(session.getId());
        }

        log.debug("[END  ] loginPlatformUser");

        return user;
    }


    @PostMapping("/logout")
    public UserVO logout() throws Exception {
        log.debug("[BEGIN] logout");

        UserVO user = userService.getByUserSeq(ContextHolder.exeContext().getUserSeq());

        log.debug("[END  ] logout");

        return user;
    }

    @InHouse
    @PostMapping("/ad/login")
    public ResultVO adLogin(@Validated @RequestBody ADVO auth) throws Exception {
        return adApiClient.authenticateAD(auth);
    }

    @InHouse
    @GetMapping("/publickey")
    @Operation(summary = "공개키 조회 ", description = "화면에서 데이터 암호화에 사용할 공개키 정보조회. public key encoded 된 값의 base64 문자열 값임.")
    public String getPublicKey()
    {
        log.debug("[BEGIN] getPublicKey");
        String publicKey = CryptoUtils.getStringRSAPublicKey();
        log.debug("[END  ] getPublicKey");

        return publicKey;
    }

    @InHouse
    @GetMapping("/license")
    @Operation(summary = "License 조회 ")
    public CocktailLicenseVO getLicense(
            @Parameter(name = "showLicenseKey", description = "LicenseKey 조회 여부", schema = @Schema(allowableValues = {"true","false"}, defaultValue = "false")) @RequestParam(value = "showLicenseKey", required = false, defaultValue = "false") boolean showLicenseKey
    ) throws Exception {
        log.debug("[BEGIN] getLicense");

        CocktailLicenseVO licenseVO = null;
        try {
            /**
             * header 정보로 요청 사용자 권한 체크
             */
            AuthUtils.checkUserAdminAuth(ContextHolder.exeContext());

            licenseVO = licenseService.getLicense();
            if (licenseVO != null && !showLicenseKey) {
                licenseVO.setLicenseKey(null);
            }
        } catch (Exception e) {
            throw e;
        } finally {
            log.debug("[END  ] getLicense");
        }

        return licenseVO;
    }

    @InHouse
    @PostMapping("/license/trial/issue")
    @Operation(summary = "Trial License 발급 ")
    public void issueTrialLicense() throws Exception {
        log.debug("[BEGIN] issueTrialLicense");

        try {
            /**
             * header 정보로 요청 사용자 권한 체크
             */
            AuthUtils.checkUserAdminAuth(ContextHolder.exeContext());

            licenseService.issueTrialLicense(null, true, false, true);
        } catch (Exception e) {
            throw e;
        } finally {
            log.debug("[END  ] issueTrialLicense");
        }

    }

    @InHouse
    @PutMapping("/license/trial/reissue")
    @Operation(summary = "Trial License 재발급(연장)")
    public void reissueTrialLicense(
            @Parameter(name = "expirePeriodDays", description = "만료 기간에 더해지는 라이센스 만료 기간") @RequestParam(name = "expirePeriodDays", required = false) Integer expirePeriodDays
    ) throws Exception {
        log.debug("[BEGIN] reissueTrialLicense");

        try {
            /**
             * header 정보로 요청 사용자 권한 체크
             */
            AuthUtils.checkUserAdminAuth(ContextHolder.exeContext());

            licenseService.issueTrialLicense(expirePeriodDays, true, true, true);
        } catch (Exception e) {
            throw e;
        } finally {
            log.debug("[END  ] reissueTrialLicense");
        }

    }

    @InHouse
    @PostMapping("/license/issue")
    @Operation(summary = "License 발급 ")
    public String issueLicense(
            @Parameter(name = "expirePeriodType", description = "만료 기간 유형(expiryDate 설정시 무시됨)", schema = @Schema(allowableValues = {LicenseExpirePeriodType.Names.YEAR, LicenseExpirePeriodType.Names.MONTH, LicenseExpirePeriodType.Names.DAY})) @RequestParam(name = "expirePeriodType", required = false) LicenseExpirePeriodType expirePeriodType,
            @Parameter(name = "expirePeriodValue", description = "만료 기간 유형으로 더해지는 라이센스 만료 기간(expiryDate 설정시 무시됨)") @RequestParam(name = "expirePeriodValue", required = false) Long expirePeriodValue,
            @Parameter(name = "purpose", description = "발급용도", schema = @Schema(defaultValue = "demo")) @RequestParam(name = "purpose", required = false) String purpose,
            @Parameter(name = "issuer", description = "발급 회사", schema = @Schema(defaultValue = "issuer")) @RequestParam(name = "issuer", required = false) String issuer,
            @Parameter(name = "company", description = "고객 회사", schema = @Schema(defaultValue = "customer")) @RequestParam(name = "company", required = false) String company,
            @Parameter(name = "capacity", description = "용량(ie. 3)", schema = @Schema(defaultValue = "0")) @RequestParam(name = "capacity", required = false) Integer capacity,
            @Parameter(name = "issueDate", description = "발급일(ie. yyyyMMdd), 값이 없을 시 현재일로 처리") @RequestParam(name = "issueDate", required = false) String issueDate,
            @Parameter(name = "expiryDate", description = "만료일(ie. yyyyMMdd), 값이 없을 시 현재일로부터 1년으로 처리") @RequestParam(name = "expiryDate", required = false) String expiryDate
    ) throws Exception {
        log.debug("[BEGIN] issueLicense");

        String encodedLicense = null;
        try {
            /**
             * header 정보로 요청 사용자 권한 체크
             */
            AuthUtils.checkUserAdminAuth(ContextHolder.exeContext());

            if (StringUtils.isNotBlank(expiryDate)) {
                encodedLicense = licenseService.issueLicense(LicenseType.FULL, purpose, issuer, company, capacity, null, issueDate, expiryDate);
            } else {
                encodedLicense = licenseService.issueLicense(expirePeriodType, expirePeriodValue, LicenseType.FULL, purpose, issuer, company, capacity, null, issueDate);
            }


        } catch (Exception e) {
            throw e;
        } finally {
            log.debug("[END  ] issueLicense");
        }

        return encodedLicense;
    }

    @InHouse
    @PostMapping("/license/issue/download")
    @Operation(summary = "License 발급 ")
    public void issueLicenseFileDownload(
            @Parameter(name = "expirePeriodType", description = "만료 기간 유형(expiryDate 설정시 무시됨)", schema = @Schema(allowableValues = {LicenseExpirePeriodType.Names.YEAR, LicenseExpirePeriodType.Names.MONTH, LicenseExpirePeriodType.Names.DAY})) @RequestParam(name = "expirePeriodType", required = false) LicenseExpirePeriodType expirePeriodType,
            @Parameter(name = "expirePeriodValue", description = "만료 기간 유형으로 더해지는 라이센스 만료 기간(expiryDate 설정시 무시됨)") @RequestParam(name = "expirePeriodValue", required = false) Long expirePeriodValue,
            @Parameter(name = "purpose", description = "발급용도", schema = @Schema(defaultValue = "demo")) @RequestParam(name = "purpose", required = false) String purpose,
            @Parameter(name = "issuer", description = "발급 회사", schema = @Schema(defaultValue = "issuer")) @RequestParam(name = "issuer", required = false) String issuer,
            @Parameter(name = "company", description = "고객 회사", schema = @Schema(defaultValue = "customer")) @RequestParam(name = "company", required = false) String company,
            @Parameter(name = "capacity", description = "용량(ie. 3)") @RequestParam(name = "capacity", required = false) Integer capacity,
            @Parameter(name = "issueDate", description = "발급일(ie. yyyyMMdd), 값이 없을 시 현재일로 처리") @RequestParam(name = "issueDate", required = false) String issueDate,
            @Parameter(name = "expiryDate", description = "만료일(ie. yyyyMMdd), 값이 없을 시 현재일로부터 1년으로 처리") @RequestParam(name = "expiryDate", required = false) String expiryDate,
            HttpServletResponse response
    ) throws Exception {
        log.debug("[BEGIN] issueLicenseFileDownload");

        String encodedLicense = null;
        try {
            /**
             * header 정보로 요청 사용자 권한 체크
             */
            AuthUtils.checkUserAdminAuth(ContextHolder.exeContext());

            if (StringUtils.isNotBlank(expiryDate)) {
                encodedLicense = licenseService.issueLicense(LicenseType.FULL, purpose, issuer, company, capacity, null, issueDate, expiryDate);
            } else {
                encodedLicense = licenseService.issueLicense(expirePeriodType, expirePeriodValue, LicenseType.FULL, purpose, issuer, company, capacity, null, issueDate);
            }
            if (StringUtils.isNotBlank(encodedLicense)) {
                byte[] licenseBytes = encodedLicense.getBytes("UTF-8");
                try (ServletOutputStream sos = response.getOutputStream()) {
                    response.setStatus(HttpServletResponse.SC_OK);
                    response.setContentType("text/html; UTF-8");
                    response.setHeader("Accept-Ranges", "bytes");
                    response.setHeader("Content-Disposition", String.format("attachment; filename=%s; filename*=UTF-8''%s", "COCKTAIL_LICENSE.txt", "COCKTAIL_LICENSE.txt"));
                    response.setHeader("Content-Transfer-Encoding", "binary");
                    response.setContentLength(licenseBytes.length);

                    sos.write(licenseBytes);
                    sos.flush();
                    response.flushBuffer();
                } catch (IOException ie) {
                    throw ie;
                } catch (Exception e) {
                    throw e;
                }
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            }
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            throw e;
        } finally {
            log.debug("[END  ] issueLicenseFileDownload");
        }
    }
}
