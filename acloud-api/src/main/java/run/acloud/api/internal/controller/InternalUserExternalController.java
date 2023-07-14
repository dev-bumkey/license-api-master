package run.acloud.api.internal.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import run.acloud.api.auth.enums.LanguageType;
import run.acloud.api.auth.enums.UserRole;
import run.acloud.api.auth.vo.UserExternalVO;
import run.acloud.api.auth.vo.UserVO;
import run.acloud.api.configuration.service.AccountService;
import run.acloud.api.configuration.service.UserService;
import run.acloud.api.configuration.vo.AccountVO;
import run.acloud.commons.annotations.InHouse;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;
import run.acloud.framework.util.ExceptionMessageUtils;

import java.util.List;

/**
 * @author dy79@acornsoft.io
 * Created on 2017. 3. 13.
 */
@Tag(name = "Internal External User", description = "내부호출용 외부사용자 관련 기능을 제공한다.")
@InHouse
@Slf4j
@RestController
@RequestMapping(value = "/internal/user")
public class InternalUserExternalController {

    @Autowired
    private UserService userService;

    @Autowired
    private AccountService accountService;


    /**
     * 사용자 등록
     * - 사용자 암호를 넣지 않는 환경에서 사용자 등록이 필요할 경우 사용
     * - 권한은 사용자(DEVOPS)로 무조건 등록 (칵테일 관리자 화면애서 변경)
     * - 기준이 되는 account에 사용자로 셋팅
     *
     * @param userExternal
     * @return
     * @throws Exception
     */
    @Operation(summary = "사용자 추가(외부호출)", description = "사용자 > 사용자를 추가 한다.(외부호출)")
    @PostMapping(value = "/")
    public UserExternalVO addUserExternal(
            @Parameter(name = "외부 사용자 모델", description = "외부 사용자 모델", required = true) @RequestBody UserExternalVO userExternal
    ) throws Exception {

        log.debug("[BEGIN] addUserExternal");


        // 사용자 기본 정보 체크
        ExceptionMessageUtils.checkParameter("userId", userExternal.getUserId(), 50, true);
        ExceptionMessageUtils.checkParameter("userName", userExternal.getUserName(), 50, true);

        // 권한은 사용자(DEVOPS)로 무조건 등록
        UserVO user = new UserVO();
        user.setUserId(userExternal.getUserId());
        user.setUserName(userExternal.getUserName());
        user.setUserRole(UserRole.DEVOPS.getCode());
        user.setUserLanguage(LanguageType.ko);
        user.setUserTimezone(LanguageType.ko.getTimezone());
        user.setPasswordInterval(9999);
        if (userExternal.getAccountSeq() == null) {
            user.setAccountSeq(1); // Default : account_seq = 1
        } else {
            user.setAccountSeq(userExternal.getAccountSeq());
        }
        user.setAccount(new AccountVO());
        user.getAccount().setAccountSeq(user.getAccountSeq());
        user.setCreator(1);
        user.setUpdater(1);


        List<UserVO> results = userService.getUsersForCheck(user);
        if (results.size() > 0) {
            throw new CocktailException("User already exist", ExceptionType.UserAlreadyExists);
        }else{
            userService.addUserExternal(user);

        }

        log.debug("[END  ] addUserExternal");

        return userExternal;
    }

    /**
     * 사용자 삭제
     * - 사용자 암호를 넣지 않는 환경에서 사용자 등록이 필요할 경우 사용
     * - 기준이 되는 account에 사용자 맵핑까지 삭제
     *
     * @param userExternal
     * @return
     * @throws Exception
     */
    @Operation(summary = "사용자 삭제(외부호출)", description = "사용자 > 사용자를 삭제 한다.(외부호출)")
    @DeleteMapping(value = "/")
    public void removeUserExternal(
            @Parameter(name = "외부 사용자 모델", description = "외부 사용자 모델", required = true) @RequestBody UserExternalVO userExternal
    ) throws Exception {

        log.debug("[BEGIN] removeUserExternal");

        // 사용자 기본 정보 체크
        ExceptionMessageUtils.checkParameterRequired("userId", userExternal.getUserId());

        // 권한은 사용자(DEVOPS)로 무조건 등록
        UserVO user = new UserVO();
        user.setUserId(userExternal.getUserId());
        user.setUserRole(UserRole.DEVOPS.getCode());
        if (userExternal.getAccountSeq() == null) {
            user.setAccountSeq(1); // Default : account_seq = 1
        } else {
            user.setAccountSeq(userExternal.getAccountSeq());
        }
        user.setAccount(new AccountVO());
        user.getAccount().setAccountSeq(user.getAccountSeq());


        List<UserVO> results = this.userService.getUsersForCheck(user);
        if (results.size() > 0) {
            accountService.removeUserExternal(user.getAccountSeq(), results.get(0).getUserSeq());
        }

        log.debug("[END  ] removeUserExternal");
    }


    @GetMapping(value = "/")
    @Operation(summary = "사용자 목록(외부 호출)", description = "사용자 목록을 조회한다.(외부 호출)")
    public List<UserExternalVO> getUsersExternal(
            @Parameter(name = "userSeq", description = "userSeq", required = false) @RequestParam(required = false) Integer userSeq,
            @Parameter(name = "userId", description = "userId", required = false) @RequestParam(required = false) String userId,
            @Parameter(name = "userRole", description = "userRole", required = false) @RequestParam(defaultValue = "SYSTEM", required = false) String userRole,
            @Parameter(name = "accountSeq", description = "accountSeq", required = false) @RequestParam(defaultValue = "1", required = false) Integer accountSeq,
            @Parameter(name = "accountCode", description = "accountCode", required = false) @RequestParam(required = false) String accountCode
    ) throws Exception {

        log.debug("[BEGIN] getUsersExternal");

        List<UserExternalVO> users = userService.getUsersForExternal(userSeq, userId, userRole, accountSeq, accountCode);

        log.debug("[END  ] getUsersExternal");

        return users;
    }
}
