package run.acloud.api.internal.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import run.acloud.api.auth.vo.UserOtpVO;
import run.acloud.api.configuration.service.UserService;
import run.acloud.commons.annotations.InHouse;
import run.acloud.commons.security.password.MessageDigestPasswordEncoder;
import run.acloud.commons.security.password.ShaPasswordEncoder;
import run.acloud.framework.context.ContextHolder;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;

/**
 * @author dy79@acornsoft.io
 * Created on 2017. 3. 13.
 */
@Tag(name = "Internal OTP", description = "내부호출용 OTP 관련 기능을 제공한다.")
@InHouse
@Slf4j
@RestController
@RequestMapping(value = "/internal/otp")
public class InternalOtpController {

    @Autowired
    private UserService userService;

    @GetMapping("/user/{userSeq}/secret")
    @Operation(summary = "사용자 OTP Secret 조회", description = "사용자 OTP Secret 조회한다.")
    public UserOtpVO getOtpSecret(
            @RequestHeader(name = "otp-signature" ) String keySig,
            @Parameter(name = "userSeq", description = "userSeq", required = true) @PathVariable Integer userSeq
    ) throws Exception {
    	log.debug("[BEGIN] getOtpSecret");

        UserOtpVO userOtp = null;

        MessageDigestPasswordEncoder passwordEncoder = new ShaPasswordEncoder(256);
        if(!StringUtils.equals(keySig, passwordEncoder.encodePassword("CocktailOtpSignature", null))) {
            throw new CocktailException("getOtpSecret Fail : Invalid Signature ", ExceptionType.NotAuthorizedToRequest);
        }

        userOtp = userService.getUserOtpInfo(userSeq);

        if (userOtp != null) {
            userOtp.setOtpQr(null);
            userOtp.setOtpUrl(null);
        }

        log.debug("[END  ] getOtpSecret");

        return userOtp;
    }

    @PutMapping(value = "/user/{userSeq}")
    @Operation(summary = "사용자 OTP 정보 변경", description = "사용자 OTP 정보 변경한다.")
    public void editUserOtpInfo(
            @Parameter(name = "userSeq", description = "user seq", required = true) @PathVariable Integer userSeq,
            @Parameter(name = "userOtp", description = "수정하려는 user otp 정보") @RequestBody UserOtpVO userOtp

    ) throws Exception {

        log.debug("[BEGIN] editUserOtpInfo");

        userOtp.setUserSeq(userSeq);
        userService.updateUserOtpInfo(userOtp, ContextHolder.exeContext().getUserSeq());

        log.debug("[END  ] editUserOtpInfo");
    }
}
