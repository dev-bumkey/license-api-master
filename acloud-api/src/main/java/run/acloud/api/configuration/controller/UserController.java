package run.acloud.api.configuration.controller;

import com.diffplug.common.base.Errors;
import com.google.api.client.util.Maps;
import com.google.common.collect.Lists;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import run.acloud.api.auth.enums.UserGrant;
import run.acloud.api.auth.enums.UserRole;
import run.acloud.api.auth.vo.UserInactiveVO;
import run.acloud.api.auth.vo.UserOtpVO;
import run.acloud.api.auth.vo.UserVO;
import run.acloud.api.configuration.constants.UserConstants;
import run.acloud.api.configuration.service.UserClusterRoleIssueService;
import run.acloud.api.configuration.service.UserService;
import run.acloud.api.configuration.vo.*;
import run.acloud.api.cserver.service.ServiceService;
import run.acloud.api.cserver.vo.ServiceRelationVO;
import run.acloud.commons.security.password.MessageDigestPasswordEncoder;
import run.acloud.commons.security.password.ShaPasswordEncoder;
import run.acloud.commons.util.*;
import run.acloud.commons.vo.ExecutingContextVO;
import run.acloud.commons.vo.ResultVO;
import run.acloud.framework.context.ContextHolder;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;
import run.acloud.framework.util.ExceptionMessageUtils;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Tag(name = "User", description = "칵테일 사용자에 대한 관리 기능을 제공한다.")
@Slf4j
@RequestMapping(value = "/api/user")
@RestController
@Validated
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private UserClusterRoleIssueService userClusterRoleIssueService;

    @Autowired
    private ServiceService serviceService;

    @Operation(summary = "사용자 > 사용자 목록", description = "사용자 > 사용자 목록을 조회한다.")
    @GetMapping(value = "/list")
    public List<UserVO> getUsers(
            @Parameter(name = "listType", description = "조회 유형, 'ACCOUNTUSER' - 어카운트 생성시 어카운트 사용자 추가시 'account'권한 사용자 목록, 'ACCOUNT' - 어카운트 생성/수정시 구성원 추가 목록, 'WORKSPACE' - 워크스페이스 구성원 추가 목록(추가 파라미터 필요)", schema = @Schema(allowableValues = {"ACCOUNTUSER","ACCOUNT","WORKSPACE"})) @RequestParam(required = false) String listType,
            @Parameter(name = "accountSeq", description = "'listType'이 'WORKSPACE'일 경우 필수, 사용처 - 워크스페이스 구성원 추가 목록") @RequestParam(required = false) Integer accountSeq
    ) throws Exception {

        log.debug("[BEGIN] getUsers");

        HttpServletRequest request = Utils.getCurrentRequest();

        /**
         * header 정보로 요청 사용자 권한 체크
         */
        ExecutingContextVO ctx = ContextHolder.exeContext();
        AuthUtils.isValid(ContextHolder.exeContext(), AuthUtils.checkUserSysUserNDevOpsBlockAuthPredicate());

        Map<String, Object> checkObj = Maps.newHashMap();
        checkObj.put("listType", listType);
        ctx.setParams(checkObj);
        AuthUtils.isValid(ctx, this.checkUserListAuth());

        List<UserVO> users = null;
        if(StringUtils.equalsIgnoreCase("ACCOUNTUSER", listType)){
            /**
             * account에 할당되지 않은 'ACCOUNT' ROLE의 사용자 목록 조회
             */
            users = userService.getAccountUsersForAccount(accountSeq);
        }else if(StringUtils.equalsIgnoreCase("ACCOUNT", listType)){
            /**
             * account 구성원으로 할당되지 않은 'DEVOPS' ROLE의 사용자 목록 조회
             */
            users = userService.getUsersForAccount(accountSeq);
        }else if(StringUtils.equalsIgnoreCase("WORKSPACE", listType)){
            /**
             * workspace는 account에 종속되므로 account키가 필수
             */
            if(accountSeq != null){
                /**
                 * 해당 account 구성원 중 workspace에
                 */
                UserRole userRole = UserRole.valueOf(ctx.getUserRole());
                String userGrant = null;
                if (userRole.isDevops()) {
                    if (CollectionUtils.isNotEmpty(ctx.getUserRelations())) {
                        Optional<ServiceRelationVO> serviceRelationOptional = ctx.getUserRelations().stream().filter(r -> (r.getUserGrant() == UserGrant.MANAGER)).findFirst();
                        if (serviceRelationOptional.isPresent()) {
                            userGrant = serviceRelationOptional.get().getUserGrant().getCode();
                        }
                    }
                }
                users = userService.getUsersForWorkspace(ctx.getUserRole(), userGrant, accountSeq);
            }else{
                CocktailException ce = new CocktailException("If listType is 'WORKSPACE', accountSeq is required.", ExceptionType.InvalidParameter_Empty);
                log.error(ExceptionMessageUtils.setCommonResult(request, null, ce, false));
            }
        }else{
            UserVO params = new UserVO();
            params.setUserRole(ctx.getUserRole());
            users = userService.getUsers(params);
        }

        if (CollectionUtils.isNotEmpty(users)){
            for (UserVO u : users) {
                if (u.getUserLanguage() == null) {
                    u.setUserLanguage(UserConstants.defaultLanguage());
                }
                if (StringUtils.isBlank(u.getUserTimezone())) {
                    u.setUserTimezone(u.getUserLanguage().getTimezone());
                }
                userService.setPasswordPolicyParam(u, null);
            }
        }

        log.debug("[END  ] getUsers");

        return users;
    }

    @Operation(summary = "사용자 > 사용자 정보 조회", description = "사용자 > 사용자의 상세 정보를 조회 한다.")
    @GetMapping(value = "/{userSeq}")
    public UserVO getUser(@PathVariable int userSeq) throws Exception {

        log.debug("[BEGIN] getUser");

        UserVO user = this.userService.getByUserSeq(userSeq);

        log.debug(JsonUtils.toGson(user));
        if(user != null){
            userService.setPasswordPolicyParam(user, null);
            user.setPassword(null);
            // 사용자 휴면 여부
            userService.setUserSleepParam(user);
        }

        log.debug("[END  ] getUser");

        return user;
    }

    @Operation(summary = "사용자 > 사용자 추가", description = "사용자 > 사용자를 추가 한다.")
    @PostMapping(value = "")
    public UserVO addUser(
              @RequestHeader(name="isEncrypted", defaultValue="false") boolean isEncrypted
            , @RequestBody UserVO user
    ) throws Exception {

        log.debug("[BEGIN] addUser");

        // isEncrypted가 true(암호화 되어 있다면) 복호화 하여 패스워드 다시 셋팅
        if(isEncrypted && StringUtils.isNotBlank(user.getNewPassword())){
            // 패스워드 복호화 후 다시 셋팅
            String decryptedPasswd = CryptoUtils.decryptRSA(user.getNewPassword());
            user.setNewPassword(decryptedPasswd);
        }

        /**
         * header 정보로 요청 사용자 권한 체크
         */
        AuthUtils.checkUserAdminAuth(ContextHolder.exeContext());

        // 사용자 기본 정보 체크
        userService.checkAddUser(user);

        if(CollectionUtils.isNotEmpty(user.getRoles())){
            if(UserRole.ADMIN != UserRole.valueOf(user.getRoles().get(0))){
                throw new CocktailException("A user role invalid.", ExceptionType.UserRoleInvalid);
            }
        }

        List<UserVO> results = this.userService.getUsersForCheck(user);
        if (results.size() > 0) {
            throw new CocktailException("User already exist", ExceptionType.UserAlreadyExists);
        }else{

            if (!Utils.isValidPasswordWithUserInfo(user, user.getNewPassword())){
                throw new CocktailException("Violation of password policy.", ExceptionType.UserPasswordInvalid);
            }

            this.userService.addUser(user);

            user.setPassword("");
        }

        log.debug("[END  ] addUser");

        return user;
    }

    @Operation(summary = "사용자 > 사용자 정보 수정", description = "사용자 > 사용자 정보를 수정한다.")
    @PutMapping(value = "/{userSeq}")
    public UserVO editUser(@PathVariable int userSeq,
                           @RequestBody UserVO user) throws Exception {

        log.debug("[BEGIN] editUser");

        /**
         * header 정보로 요청 사용자 권한 체크
         */
        AuthUtils.checkUserAdminAuth(ContextHolder.exeContext());

        /**
         * 사용자 수정
         */
        if(CollectionUtils.isNotEmpty(user.getRoles())){
            if(UserRole.ADMIN != UserRole.valueOf(user.getRoles().get(0))){
                throw new CocktailException("A user role invalid.", ExceptionType.UserRoleInvalid);
            }
        }

        user.setUserSeq(userSeq);
        UserVO userCurr = userService.editUserWithCheck(user);

        log.debug("[END  ] editUser");

        return userCurr;
    }

    @Operation(summary = "사용자 > 칵테일 UI 언어 변경", description = "사용자 > 칵테일 UI에서 사용하는 언어를 변경한다.")
    @PutMapping(value = "/{userSeq}/changeLanguage")
    public UserVO changeLanguage(
            @PathVariable int userSeq,
            @RequestBody UserVO user
    ) throws Exception {

        log.debug("[BEGIN] changeLanguage");

        /**
         * 사용자 언어 수정
         */
        user.setUserSeq(userSeq);
        UserVO userCurr = userService.editUserWithCheckWithConsumer(user, Errors.rethrow().wrap((UserVO u) -> {userService.editUserLanguage(u);}));

        log.debug("[END  ] changeLanguage");

        return userCurr;
    }

    @Operation(summary = "사용자 > Timezone 변경", description = "사용자 > 칵테일 UI에서 사용하는 Timezone을 변경한다.")
    @PutMapping(value = "/{userSeq}/changeTimezone")
    public UserVO changeTimezone(
            @PathVariable int userSeq,
            @RequestBody UserVO user
    ) throws Exception {

        log.debug("[BEGIN] changeTimezone");

        /**
         * 사용자 타임존 수정
         */
        user.setUserSeq(userSeq);
        UserVO userCurr = userService.editUserWithCheckWithConsumer(user, Errors.rethrow().wrap((UserVO u) -> {userService.editUserTimezone(u);}));

        log.debug("[END  ] changeTimezone");

        return userCurr;
    }

    @Operation(summary = "사용자 > 사용자 삭제", description = "사용자 > 사용자를 삭제 한다.")
    @DeleteMapping(value = "/{userSeq}")
    public void removeUser(
            @PathVariable Integer userSeq
    ) throws Exception {

        log.debug("[BEGIN] removeUser");

        /**
         * header 정보로 요청 사용자 권한 체크
         */
        AuthUtils.checkUserAdminAuth(ContextHolder.exeContext());

        /**
         * 사용자 삭제
         */
        if(userSeq.equals(ContextHolder.exeContext().getUserSeq())){
            throw new CocktailException("Do not remove a myself user!!", ExceptionType.CanNotDeleteMyself);
        }

        /**
         * 사용자 삭제
         */
        userService.removeUserWithCheck(userSeq);

        log.debug("[END  ] removeUser");
    }

    @Operation(summary = "사용자 > 비밀번호 변경", description = "사용자 > 비밀번호를 변경 한다.")
    @PutMapping(value = "/{userSeq}/changePassword")
    public ResultVO changePassword(
            @RequestHeader(name = "user-id" ) Integer _userSeq,
            @RequestHeader(name = "user-role" ) String _userRole,
            @RequestHeader(name = "isEncrypted", defaultValue="false") boolean isEncrypted,
            @PathVariable int userSeq,
            @RequestBody UserVO user) throws Exception {

        log.debug("[BEGIN] changePassword");
        user.setUserSeq(userSeq);

        Map<String, Object> errData = com.google.common.collect.Maps.newHashMap();

        // isEncrypted가 true(암호화 되어 있다면) 복호화 하여 패스워드 다시 셋팅
        if(isEncrypted){
            // new 패스워드 복호화 후 다시 셋팅
            if (StringUtils.isNotBlank(user.getNewPassword())) {
                String decryptedPasswd = CryptoUtils.decryptRSA(user.getNewPassword());
                user.setNewPassword(decryptedPasswd);
            }

            // 패스워드 복호화 후 다시 셋팅
            if (StringUtils.isNotBlank(user.getPassword())) {
                String decryptedPasswd = CryptoUtils.decryptRSA(user.getPassword());
                user.setPassword(decryptedPasswd);
            }
        }

        UserVO userCurr = this.userService.getByUserSeq(userSeq);
        if (userCurr == null) {
            throw new CocktailException("User not found", ExceptionType.UserIdNotFound);
        }

        try {
            boolean isSame = this.userService.isValidPassword(user);
            if (!isSame) {
                throw new CocktailException("Old password is invalid", ExceptionType.UserPasswordIncorect2);
            }
        } catch (CocktailException ce){
            int pwFailCount = userCurr.getLoginFailCount() + 1;
            userService.updateLoginFailCountBySeq(pwFailCount, userCurr.getUserSeq());
            if (pwFailCount > 0) {
                errData.put("pwFailCount", pwFailCount);
            }
            ce.setData(errData);
            throw ce;
        }


        if (!Utils.isValidPasswordWithUserInfo(userCurr, user.getNewPassword())){
            throw new CocktailException("Violation of password policy.", ExceptionType.UserPasswordInvalid);
        }

        // 2021-09-15, coolingi, password salt 값 적용
        MessageDigestPasswordEncoder passwordEncoder = new ShaPasswordEncoder(256);
        String encodedPassword = passwordEncoder.encodePassword(user.getNewPassword(), userCurr.getHashSalt());

        userCurr.setPassword(encodedPassword);

        userCurr.setPasswordInterval(UserConstants.INTERVAL_CHANGE_PASSWORD); // password 변경 후 90일로 만료기한 변경
        userCurr.setUpdater(_userSeq);
        this.userService.changePassword(userCurr);
        // 여기에 초기화할 거 0으로 세팅하기
        userService.updateLoginFailCountBySeq(0, user.getUserSeq());
        log.debug("[END  ] changePassword");

        return new ResultVO();
    }

    @Operation(summary = "사용자 > 비밀번호 초기화", description = "사용자 > 비밀번호를 초기 비밀번호로 Reset 한다.")
    @PostMapping(value = "/{userSeq}/resetPassword")
    public void resetPassword(
            @PathVariable int userSeq,
            @Parameter(name = "useEmail", description = "이메일로 초기화 비밀번호 전달", schema = @Schema(allowableValues = {"true","false"}, defaultValue = "true")) @RequestParam(value = "useEmail", required = false, defaultValue = "true") boolean useEmail
    ) throws Exception {

        log.debug("[BEGIN] resetPassword");

        /**
         * header 정보로 요청 사용자 권한 체크
         */
        AuthUtils.checkUserAdminAuth(ContextHolder.exeContext());

        userService.resetPasswordUserWithCheck(userSeq, useEmail);

        log.debug("[END  ] resetPassword");
    }

    @Operation(summary = "사용자 > 비밀번호 유효기간 연장", description = "사용자 > 비밀번호의 유효기간을 연장한다.")
    @PostMapping(value = "/{userSeq}/extendPassword")
    public void extendPassword(
            @RequestHeader(name = "user-id" ) Integer _userSeq,
            @RequestHeader(name = "user-role" ) String _userRole,
            @PathVariable int userSeq
    ) throws Exception {

        log.debug("[BEGIN] extendPassword");

        UserVO user = this.userService.getByUserSeq(userSeq);
        if (user == null) {
            throw new CocktailException("User not found", ExceptionType.UserIdNotFound);
        }

        user.setPasswordInterval(UserConstants.INTERVAL_EXTEND_PASSWORD); // password 변경 후 30일로 만료기한 변경
        user.setUpdater(_userSeq);
        this.userService.extendPassword(user);

        log.debug("[END  ] extendPassword");
    }

    @Operation(summary = "사용자 > 비밀번호 체크", description = "사용자 > 비밀번호룰 체크한다.")
    @PostMapping(value = "/{userSeq}/checkPassword")
    public ResultVO checkPassword(
            @RequestHeader(name = "isEncrypted", defaultValue="false") boolean isEncrypted,
            @PathVariable int userSeq,
            @RequestBody UserVO user
    ) throws Exception {

        log.debug("[BEGIN] checkPassword");

        // isEncrypted가 true(암호화 되어 있다면) 복호화 하여 패스워드 다시 셋팅
        if(isEncrypted && StringUtils.isNotBlank(user.getPassword())){
            // 패스워드 복호화 후 다시 셋팅
            String decryptedPasswd = CryptoUtils.decryptRSA(user.getPassword());
            user.setPassword(decryptedPasswd);
        }

        if(ContextHolder.exeContext().getUserSeq() != userSeq) {
            throw new CocktailException("User information mismatch", ExceptionType.InvalidParameter);
        }
        UserVO curUser = this.userService.getUser(userSeq);
        if (curUser == null) {
            throw new CocktailException("User not found", ExceptionType.UserIdNotFound);
        }
        user.setNewPassword(null);
        user.setUserSeq(userSeq);
        Map<String, Object> errData = com.google.common.collect.Maps.newHashMap();
        try {
            boolean isSame = this.userService.isValidPassword(user);
            if (!isSame) {
                throw new CocktailException("Password is invalid", ExceptionType.UserPasswordIncorect2);
            }
        } catch (CocktailException ce){
            int pwFailCount = curUser.getLoginFailCount() + 1;
            userService.updateLoginFailCountBySeq(pwFailCount, curUser.getUserSeq());
            if (pwFailCount > 0) {
                errData.put("pwFailCount", pwFailCount);
            }
            ce.setData(errData);
            throw ce;
        }

        userService.updateLoginFailCountBySeq(0, user.getUserSeq());
        log.debug("[END  ] checkPassword");


        return new ResultVO();
    }

    @Operation(summary = "사용자 > OTP QR 조회", description = "사용자 > OTP QR을 조회한다.")
    @GetMapping(value = "/{userSeq}/otp/qr")
    public UserOtpVO getUserOtpQr(
            @Parameter(name = "userSeq", description = "user seq", required = true) @PathVariable Integer userSeq

    ) throws Exception {

        log.debug("[BEGIN] getUserOtpQrOfAccount");

        /**
         * header 정보로 요청 사용자 권한 체크
         */
        AuthUtils.checkUserAdminAuth(ContextHolder.exeContext());

        UserOtpVO userOtp = userService.getUserOtpQr(userSeq);

        log.debug("[END  ] getUserOtpQrOfAccount");

        return userOtp;
    }

    @Operation(summary = "사용자 > OTP 초기화", description = "사용자 > OTP 초기화한다.")
    @PostMapping(value = "/{userSeq}/otp/reset")
    public void resetOtpUser(
            @Parameter(name = "userSeq", description = "user seq", required = true) @PathVariable int userSeq
    ) throws Exception {

        log.debug("[BEGIN] resetOtpUser");

        /**
         * header 정보로 요청 사용자 권한 체크
         */
        AuthUtils.checkUserAdminAuth(ContextHolder.exeContext());

        UserOtpVO userOtp = new UserOtpVO();
        userOtp.setUserSeq(userSeq);
        userOtp.setOtpUseYn("N");
        userService.updateUserOtpInfo(userOtp, ContextHolder.exeContext().getUserSeq());

        log.debug("[END  ] resetOtpUser");
    }

    @Operation(summary = "사용자 > 비활성 여부 초기화", description = "사용자 > 비활성 여부 초기화한다.")
    @PutMapping(value = "/{userSeq}/inactive")
    public void editUserInactiveState(
            @Parameter(name = "userSeq", description = "user seq", required = true) @PathVariable Integer userSeq,
            @Parameter(name = "userInactive", description = "수정하려는 user 비활성 여부 정보", required = true) @RequestBody UserInactiveVO userInactive
    ) throws Exception {

        log.debug("[BEGIN] editUserInactiveState");

        /**
         * header 정보로 요청 사용자 권한 체크
         */
        AuthUtils.checkUserAdminAuth(ContextHolder.exeContext());

        if (userSeq.equals(ContextHolder.exeContext().getUserSeq())) {
            throw new CocktailException("Your status cannot be modified.", ExceptionType.CanNotUpdateInactiveStateMyself);
        }

        UserVO user = userService.getByUserSeq(userSeq);
        if (user != null) {
            // 비활성여부 변경
            userService.updateUserInactiveYn(userSeq, userInactive.getInactiveYn(), ContextHolder.exeContext().getUserSeq());
        } else {
            throw new CocktailException("User not found", ExceptionType.UserIdNotFound);
        }

        log.debug("[END  ] editUserInactiveState");
    }

    @Operation(summary = "사용자 > 휴면 활성", description = "사용자 > 휴면 활성한다.")
    @PutMapping(value = "/{userSeq}/wake")
    public void editUserWakeState(
            @Parameter(name = "userSeq", description = "user seq", required = true) @PathVariable Integer userSeq
    ) throws Exception {

        log.debug("[BEGIN] editUserWakeState");

        /**
         * header 정보로 요청 사용자 권한 체크
         */
        AuthUtils.checkUserAdminAuth(ContextHolder.exeContext());

        if (userSeq.equals(ContextHolder.exeContext().getUserSeq())) {
            throw new CocktailException("Your status cannot be modified.", ExceptionType.CanNotUpdateInactiveStateMyself);
        }

        UserVO user = userService.getByUserSeq(userSeq);
        if (user != null) {
            // 휴면 활성
            userService.updateActiveTimestampBySeq(userSeq, ContextHolder.exeContext().getUserSeq());
        } else {
            throw new CocktailException("User not found", ExceptionType.UserIdNotFound);
        }

        log.debug("[END  ] editUserWakeState");
    }

    @Operation(summary = "사용자 > 사용중인 워크스페이스 설정", description = "사용자 > 사용자가 사용중인 워크스페이스를 저장한다.")
    @PutMapping(value = "/lastServiceSeq")
    public Integer setLastServiceSeq(
            @RequestHeader(name = "user-id" ) Integer _userSeq,
            @RequestHeader(name = "user-role" ) String _userRole,
            @Parameter(name = "serviceSeq", description = "서비스 번호", required = true) @RequestParam(name = "serviceSeq") Integer serviceSeq
    ) throws Exception {

        log.debug("[BEGIN] setLastServiceSeq");

        try {
            // 존재하는 사용자인지 확인.
            if (this.userService.getByUserSeq(_userSeq) == null) {
                throw new CocktailException("User not found", ExceptionType.UserIdNotFound);
            }

            List<ServiceListVO> serviceList = Lists.newArrayList();
            List<Integer> serviceSeqList = Lists.newArrayList();

            if(EnumSet.of(UserRole.ADMIN).contains(UserRole.valueOf(_userRole))){
                serviceList = serviceService.getServices(_userSeq, _userRole, "Y");
                serviceSeqList = serviceList.stream().map(s -> (s.getServiceSeq())).collect(Collectors.toList());
            }else{
                serviceList = userService.getAuthorizedServicesBySeq(_userSeq);
                serviceSeqList = serviceList.stream().map(s -> (s.getServiceSeq())).collect(Collectors.toList());
            }

            // Workspace 권한 확인. 1.권한 목록에 없거나 2.권한목록에 존재하는 Workspace로 요청하였는지.
            if(CollectionUtils.isEmpty(serviceList)){
                if(!serviceSeqList.contains(serviceSeq)){
                    throw new CocktailException("There is no authorized workspace.", ExceptionType.NoAuthorizedServices);
                }
            }

            UserVO user = new UserVO();
            user.setUserSeq(_userSeq);
            user.setLastServiceSeq(String.valueOf(serviceSeq.intValue()));

            // Update!
            int isSuccess = this.userService.updateLastServiceSeq(user);
            if (isSuccess < 1) {
                throw new CocktailException("User not found", ExceptionType.UserIdNotFound);
            }
        }
        catch (CocktailException ex) {
            throw ex;
        }
        catch (DataIntegrityViolationException ex) {
            throw new CocktailException("Service not Found", ex, ExceptionType.ServiceNotFound);
        }
        catch (Exception ex) {
            throw new CocktailException("Common SQL Update Exception", ex, ExceptionType.CommonUpdateFail);
        }

        log.debug("[END  ] setLastServiceSeq");

        return serviceSeq;
    }

    private Predicate<ExecutingContextVO> checkUserListAuth() throws Exception{

        Predicate<ExecutingContextVO> checkUserListAuth = p -> {
            // ADMIN 만 가능
            if(StringUtils.equalsAnyIgnoreCase(MapUtils.getString(p.getParams(), "listType"), "ACCOUNTUSER")){
                if(!UserRole.valueOf(p.getUserRole()).isAdmin()){
                    return false;
                }
            }else if(StringUtils.equalsAnyIgnoreCase(MapUtils.getString(p.getParams(), "listType"), "SYSTEM", "WORKSPACE")){
                if(UserRole.valueOf(p.getUserRole()).isDevops()){
                    if (CollectionUtils.isNotEmpty(p.getUserRelations())) {
                        Optional<ServiceRelationVO> serviceRelationOptional = p.getUserRelations().stream().filter(r -> (r.getUserGrant() == UserGrant.MANAGER)).findFirst();
                        if (serviceRelationOptional.isPresent()) {
                            return true;
                        }
                    }
                    return false;
                }
            }

            return true;
        };

        return checkUserListAuth;
    }

    @Operation(summary = "사용자 > 클러스터 권한 발급 이력 조회 (목록)", description = "사용자 > 클러스터 권한 (kubeconfig 다운로드) 발급 이력을 목록으로 조회한다.")
    @GetMapping(value = "/{apiVersion}/issuehistory")
    public UserClusterRoleIssueHistoryListVO getUserClusterRoleIssueHistoryList (
        @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v1"}), required = true) @PathVariable String apiVersion,
        @Parameter(name = "accountSeq", description = "accountSeq") @RequestParam(name= "accountSeq", required = false) Integer accountSeq,
        @Parameter(name = "userSeq", description = "userSeq") @RequestParam(name= "userSeq", required = false) Integer userSeq,
        @Parameter(name = "issueType", description = "발급 유형", schema = @Schema(allowableValues = {"SHELL","KUBECONFIG"})) @RequestParam(name = "issueType", required = false) String issueType,
        @Parameter(name = "clusterSeq", description = "clusterSeq") @RequestParam(name = "clusterSeq", required = false) Integer clusterSeq,
        @Parameter(name = "historyState", description = "발급 상태", schema = @Schema(allowableValues = {"GRANT","REVOKE","CHANGE","EXPIRE"})) @RequestParam(name = "historyState", required = false) String historyState,
//        @Parameter(name = "order", description = "정렬 순서", schema = @Schema(allowableValues = {"DESC","ASC"})) @RequestParam(name = "order", required = false) String order,
//        @Parameter(name = "orderColumn", description = "정렬기준컬럼", schema = @Schema(allowableValues = {"created","audit_log_seq"})) @RequestParam(name = "orderColumn", required = false) String orderColumn,
        @Parameter(name = "nextPage", description = "요청페이지", required = true) @RequestParam(name = "nextPage") Integer nextPage,
        @Parameter(name = "itemPerPage", description = "페이지당 표시 갯수", required = true) @RequestParam(name = "itemPerPage") Integer itemPerPage,
        @Parameter(name = "searchColumn", description = "검색 컬럼", schema = @Schema(allowableValues = {"ACCOUNT_NAME","USER_ID","USER_NAME","ISSUE_USER_ID","ISSUE_USER_NAME"})) @RequestParam(name = "searchColumn", required = false) String searchColumn,
        @Parameter(name = "searchKeyword", description = "검색어") @RequestParam(name = "searchKeyword", required = false) String searchKeyword,
        @Parameter(name = "startDate", description = "검색 시작 일자", example = "20200421000000") @RequestParam(name = "startDate", required = false) String startDate,
        @Parameter(name = "endDate", description = "검색 종료 일자", example = "20200428000000") @RequestParam(name = "endDate", required = false) String endDate,
        @Parameter(name = "maxId", description = "더보기 중복 방지를 위한 데이터셋 위치 지정") @RequestParam(name = "maxId", required = false) String maxId,
        @Parameter(name = "newId", description = "신규 데이터 추가를 위한 데이터셋 위치 지정") @RequestParam(name = "newId", required = false) String newId
    ) throws Exception {
        UserClusterRoleIssueHistoryListVO userClusterRoleIssueHistoryList;
        try {
            /**
             * 4.0.1 기준으로 DEVOPS 사용자는 접근 불가...
             */
            if(UserRole.valueOf(ContextHolder.exeContext().getUserRole()).isDevops()) {
                throw new CocktailException("The DEVOPS role user doesn't have Permission.", ExceptionType.NotAuthorizedToRequest);
            }
            PagingUtils.validatePagingParams(nextPage, itemPerPage);

            String orderColumn = "HISTORY_SEQ";
            String order = "DESC";

            userClusterRoleIssueHistoryList = userClusterRoleIssueService.getUserClusterRoleIssueHistories(accountSeq, userSeq, clusterSeq, issueType, historyState, order, orderColumn, nextPage, itemPerPage, searchColumn, searchKeyword, maxId, newId, startDate, endDate);
        }
        catch (CocktailException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new CocktailException("getUserClusterRoleIssueHistoryList Fail.", ex, ExceptionType.CommonInquireFail);
        }

        return userClusterRoleIssueHistoryList;
    }

    @Operation(summary = "사용자 > 클러스터 권한 발급 이력 조회", description = "사용자 > 클러스터 권한 (kubeconfig 다운로드) 발급 이력을 조회한다.")
    @GetMapping(value = "/{apiVersion}/issuehistory/accountname/{issueAccountName}/latest")
    public UserClusterRoleIssueHistoryVO getLatestUserClusterRoleIssueHistory (
        @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v1"}), required = true) @PathVariable String apiVersion,
        @Parameter(name = "issueAccountName", description = "issueAccountName") @PathVariable String issueAccountName,
        @Parameter(name = "accountSeq", description = "accountSeq") @RequestParam(name= "accountSeq", required = true) Integer accountSeq,
        @Parameter(name = "issueType", description = "발급 유형", schema = @Schema(allowableValues = {"SHELL","KUBECONFIG"})) @RequestParam(name = "issueType", required = false) String issueType,
        @Parameter(name = "clusterSeq", description = "clusterSeq") @RequestParam(name = "clusterSeq", required = false) Integer clusterSeq
    ) throws Exception {
        UserClusterRoleIssueHistoryVO userClusterRoleIssueHistory = null;
        try {
            /**
             * 4.0.1 기준으로 DEVOPS 사용자는 접근 불가...
             */
            if(UserRole.valueOf(ContextHolder.exeContext().getUserRole()).isDevops()) {
                throw new CocktailException("The DEVOPS role user doesn't have Permission.", ExceptionType.NotAuthorizedToRequest);
            }

            userClusterRoleIssueHistory = userClusterRoleIssueService.getLatestUserClusterRoleIssueHistory(accountSeq, clusterSeq, issueType, issueAccountName);
        }
        catch (CocktailException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new CocktailException("getLatestUserClusterRoleIssueHistory Fail.", ex, ExceptionType.CommonInquireFail);
        }

        return userClusterRoleIssueHistory;
    }

    @Operation(summary = "사용자 클러스터 계정 shellpath 수정(cluster-api)", description = "사용자 클러스터 계정 shellpath 수정(cluster-api)")
    @PutMapping(value = "/{apiVersion}/issue/shellpath")
    public void editUserIssueShellPathForClusterApi(
            @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v1"}), required = true) @PathVariable String apiVersion,
            @Parameter(name = "userClusterRoleIssue", description = "쉘 path 정보", required = true) @RequestBody UserClusterRoleIssueVO userClusterRoleIssue
    ) throws Exception {

        log.debug("[BEGIN] editUserIssueShellPathForClusterApi");

        userClusterRoleIssueService.editUserClusterRoleIssueForClusterApi(userClusterRoleIssue);

        log.debug("[END  ] editUserIssueShellPathForClusterApi");
    }

    @Operation(summary = "사용자 > 클러스터에 쉘 접속 이력 생성", description = "사용자 > 클러스터에 쉘 접속 이력을 생성한다.")
    @PostMapping(value = "/{apiVersion}/shellhistory")
    public UserShellConnectHistoryVO addShellConnectHistory(
            @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v1"}), required = true) @PathVariable String apiVersion,
            @Parameter(name = "userShellConnectHistory", description = "쉘 접속 이력 정보", required = true) @RequestBody UserShellConnectHistoryVO userShellConnectHistory
    ) throws Exception {

        log.debug("[BEGIN] addShellConnectHistory");
        /**
         * 4.0.1 기준으로 DEVOPS 사용자는 접근 불가...
         */
//        if(UserRole.valueOf(ContextHolder.exeContext().getUserRole()).isDevops()) {
//            throw new CocktailException("The DEVOPS role user doesn't have Permission.", ExceptionType.NotAuthorizedToRequest);
//        }
        userShellConnectHistory = userClusterRoleIssueService.addUserShellConnectHistory(userShellConnectHistory);

        log.debug("[END  ] addShellConnectHistory");
        return userShellConnectHistory;
    }

    @Operation(summary = "사용자 > 클러스터에 쉘 접속 이력 목록 생성", description = "사용자 > 클러스터에 쉘 접속 이력 목록을 생성한다.")
    @PostMapping(value = "/{apiVersion}/shellhistories")
    public List<UserShellConnectHistoryVO> addShellConnectHistories(
            @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v1"}), required = true) @PathVariable String apiVersion,
            @Parameter(name = "userShellConnectHistories", description = "쉘 접속 이력 정보 목록", required = true) @RequestBody List<UserShellConnectHistoryVO> userShellConnectHistories
    ) throws Exception {

        log.debug("[BEGIN] addShellConnectHistories");
        /**
         * 4.0.1 기준으로 DEVOPS 사용자는 접근 불가...
         */
//        if(UserRole.valueOf(ContextHolder.exeContext().getUserRole()).isDevops()) {
//            throw new CocktailException("The DEVOPS role user doesn't have Permission.", ExceptionType.NotAuthorizedToRequest);
//        }
        userShellConnectHistories = userClusterRoleIssueService.addUserShellConnectHistories(userShellConnectHistories);

        log.debug("[END  ] addShellConnectHistories");
        return userShellConnectHistories;
    }

    @Operation(summary = "사용자 > 클러스터에 쉘 접속 이력 조회", description = "사용자 > 클러스터에 쉘 접속 이력을 조회한다.")
    @GetMapping(value = "/{apiVersion}/shellhistory")
    public UserShellConnectHistoryListVO getUserShellConnectHistoryList (
        @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v1"}), required = true) @PathVariable String apiVersion,
        @Parameter(name = "accountSeq", description = "accountSeq") @RequestParam(name= "accountSeq", required = false) Integer accountSeq,
        @Parameter(name = "userSeq", description = "userSeq") @RequestParam(name= "userSeq", required = false) Integer userSeq,
        @Parameter(name = "clusterSeq", description = "clusterSeq") @RequestParam(name = "clusterSeq", required = false) Integer clusterSeq,
        @Parameter(name = "historyState", description = "발급 상태", schema = @Schema(allowableValues = {"GRANT","REVOKE","CHANGE","EXPIRE"})) @RequestParam(name = "historyState", required = false) String historyState,
//        @Parameter(name = "order", description = "정렬 순서", schema = @Schema(allowableValues = {"DESC","ASC"})) @RequestParam(name = "order", required = false) String order,
//        @Parameter(name = "orderColumn", description = "정렬기준컬럼", schema = @Schema(allowableValues = {"created","audit_log_seq"})) @RequestParam(name = "orderColumn", required = false) String orderColumn,
        @Parameter(name = "nextPage", description = "요청페이지", required = true) @RequestParam(name = "nextPage") Integer nextPage,
        @Parameter(name = "itemPerPage", description = "페이지당 표시 갯수", required = true) @RequestParam(name = "itemPerPage") Integer itemPerPage,
        @Parameter(name = "searchColumn", description = "검색 컬럼", schema = @Schema(allowableValues = {"ACCOUNT_NAME","USER_ID","USER_NAME","ISSUE_USER_ID","ISSUE_USER_NAME"})) @RequestParam(name = "searchColumn", required = false) String searchColumn,
        @Parameter(name = "searchKeyword", description = "검색어") @RequestParam(name = "searchKeyword", required = false) String searchKeyword,
        @Parameter(name = "startDate", description = "검색 시작 일자", example = "20200421000000") @RequestParam(name = "startDate", required = false) String startDate,
        @Parameter(name = "endDate", description = "검색 종료 일자", example = "20200428000000") @RequestParam(name = "endDate", required = false) String endDate,
        @Parameter(name = "maxId", description = "더보기 중복 방지를 위한 데이터셋 위치 지정") @RequestParam(name = "maxId", required = false) String maxId,
        @Parameter(name = "newId", description = "신규 데이터 추가를 위한 데이터셋 위치 지정") @RequestParam(name = "newId", required = false) String newId
    ) throws Exception {
        UserShellConnectHistoryListVO userShellConnectHistoryList;
        try {
            /**
             * 4.0.1 기준으로 DEVOPS 사용자는 접근 불가...
             */
            if(UserRole.valueOf(ContextHolder.exeContext().getUserRole()).isDevops()) {
                throw new CocktailException("The DEVOPS role user doesn't have Permission.", ExceptionType.NotAuthorizedToRequest);
            }
            if (!Pattern.matches("^[0-9]+$", nextPage.toString())) {
                throw new CocktailException("nextPage Parameter must be of Integer.", ExceptionType.InvalidParameter);
            }
            if (!Pattern.matches("^[0-9]+$", itemPerPage.toString())) {
                throw new CocktailException("itemPerPage Parameter must be of Integer.", ExceptionType.InvalidParameter);
            }

            String orderColumn = "CONNECT_SEQ";
            String order = "DESC";

            userShellConnectHistoryList = userClusterRoleIssueService.getUserShellConnectHistories(accountSeq, userSeq, clusterSeq, historyState, order, orderColumn, nextPage, itemPerPage, searchColumn, searchKeyword, maxId, newId, startDate, endDate);
        }
        catch (CocktailException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new CocktailException("getUserShellConnectHistoryList Fail.", ex, ExceptionType.CommonInquireFail);
        }

        return userShellConnectHistoryList;
    }

    @Operation(summary = "사용자 > kubeconfig 다운로드 이력 생성", description = "사용자 > kubeconfig 다운로드 이력을 생성한다.")
    @PostMapping(value = "/{apiVersion}/confighistory")
    public UserConfigDownloadHistoryVO addConfigDownloadHistory(
            @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v1"}), required = true) @PathVariable String apiVersion,
            @Parameter(name = "userConfigDownloadHistory", description = "kubeconfig 다운로드 이력 정보", required = true) @RequestBody UserConfigDownloadHistoryVO userConfigDownloadHistory
    ) throws Exception {

        log.debug("[BEGIN] addConfigDownloadHistory");
        /**
         * 4.0.1 기준으로 DEVOPS 사용자는 접근 불가...
         */
//        if(UserRole.valueOf(ContextHolder.exeContext().getUserRole()).isDevops()) {
//            throw new CocktailException("The DEVOPS role user doesn't have Permission.", ExceptionType.NotAuthorizedToRequest);
//        }
        userConfigDownloadHistory = userClusterRoleIssueService.addUserConfigDownloadHistory(userConfigDownloadHistory);

        log.debug("[END  ] addConfigDownloadHistory");
        return userConfigDownloadHistory;
    }

    @Operation(summary = "사용자 > kubeconfig 다운로드 이력 목록 생성", description = "사용자 > kubeconfig 다운로드 이력 목록을 생성한다.")
    @PostMapping(value = "/{apiVersion}/confighistories")
    public List<UserConfigDownloadHistoryVO> addConfigDownloadHistories(
            @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v1"}), required = true) @PathVariable String apiVersion,
            @Parameter(name = "userConfigDownloadHistories", description = "kubeconfig 다운로드 이력 정보 목록", required = true) @RequestBody List<UserConfigDownloadHistoryVO> userConfigDownloadHistories
    ) throws Exception {

        log.debug("[BEGIN] addConfigDownloadHistories");
        /**
         * 4.0.1 기준으로 DEVOPS 사용자는 접근 불가...
         */
//        if(UserRole.valueOf(ContextHolder.exeContext().getUserRole()).isDevops()) {
//            throw new CocktailException("The DEVOPS role user doesn't have Permission.", ExceptionType.NotAuthorizedToRequest);
//        }
        userConfigDownloadHistories = userClusterRoleIssueService.addUserConfigDownloadHistories(userConfigDownloadHistories);

        log.debug("[END  ] addConfigDownloadHistories");
        return userConfigDownloadHistories;
    }

    @Operation(summary = "사용자 > kubeconfig 다운로드 이력 조회", description = "사용자 > kubeconfig 다운로드 이력을 조회한다.")
    @GetMapping(value = "/{apiVersion}/confighistory")
    public UserConfigDownloadHistoryListVO getUserConfigDownloadHistoryList (
        @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v1"}), required = true) @PathVariable String apiVersion,
        @Parameter(name = "accountSeq", description = "accountSeq") @RequestParam(name= "accountSeq", required = false) Integer accountSeq,
        @Parameter(name = "userSeq", description = "userSeq") @RequestParam(name= "userSeq", required = false) Integer userSeq,
        @Parameter(name = "clusterSeq", description = "clusterSeq") @RequestParam(name = "clusterSeq", required = false) Integer clusterSeq,
        @Parameter(name = "historyState", description = "발급 상태", schema = @Schema(allowableValues = {"GRANT","REVOKE","CHANGE","EXPIRE"})) @RequestParam(name = "historyState", required = false) String historyState,
//        @Parameter(name = "order", description = "정렬 순서", schema = @Schema(allowableValues = {"DESC","ASC"})) @RequestParam(name = "order", required = false) String order,
//        @Parameter(name = "orderColumn", description = "정렬기준컬럼", schema = @Schema(allowableValues = {"created","audit_log_seq"})) @RequestParam(name = "orderColumn", required = false) String orderColumn,
        @Parameter(name = "nextPage", description = "요청페이지", required = true) @RequestParam(name = "nextPage") Integer nextPage,
        @Parameter(name = "itemPerPage", description = "페이지당 표시 갯수", required = true) @RequestParam(name = "itemPerPage") Integer itemPerPage,
        @Parameter(name = "searchColumn", description = "검색 컬럼", schema = @Schema(allowableValues = {"ACCOUNT_NAME","USER_ID","USER_NAME","ISSUE_USER_ID","ISSUE_USER_NAME"})) @RequestParam(name = "searchColumn", required = false) String searchColumn,
        @Parameter(name = "searchKeyword", description = "검색어") @RequestParam(name = "searchKeyword", required = false) String searchKeyword,
        @Parameter(name = "startDate", description = "검색 시작 일자", example = "20200421000000") @RequestParam(name = "startDate", required = false) String startDate,
        @Parameter(name = "endDate", description = "검색 종료 일자", example = "20200428000000") @RequestParam(name = "endDate", required = false) String endDate,
        @Parameter(name = "maxId", description = "더보기 중복 방지를 위한 데이터셋 위치 지정") @RequestParam(name = "maxId", required = false) String maxId,
        @Parameter(name = "newId", description = "신규 데이터 추가를 위한 데이터셋 위치 지정") @RequestParam(name = "newId", required = false) String newId
    ) throws Exception {
        UserConfigDownloadHistoryListVO userConfigDownloadHistoryList;
        try {
            /**
             * 4.0.1 기준으로 DEVOPS 사용자는 접근 불가...
             */
            if(UserRole.valueOf(ContextHolder.exeContext().getUserRole()).isDevops()) {
                throw new CocktailException("The DEVOPS role user doesn't have Permission.", ExceptionType.NotAuthorizedToRequest);
            }
            /**
             * 'ADMIN' 이 아니면 session의 accountSeq 정보로 셋팅
             */
            if (!UserRole.valueOf(ContextHolder.exeContext().getUserRole()).isAdmin()) {
                accountSeq = ContextHolder.exeContext().getUserAccountSeq();
            }
            if (!Pattern.matches("^[0-9]+$", nextPage.toString())) {
                throw new CocktailException("nextPage Parameter must be of Integer.", ExceptionType.InvalidParameter);
            }
            if (!Pattern.matches("^[0-9]+$", itemPerPage.toString())) {
                throw new CocktailException("itemPerPage Parameter must be of Integer.", ExceptionType.InvalidParameter);
            }

            String orderColumn = "DOWNLOAD_SEQ";
            String order = "DESC";

            userConfigDownloadHistoryList = userClusterRoleIssueService.getUserConfigDownloadHistories(accountSeq, userSeq, clusterSeq, historyState, order, orderColumn, nextPage, itemPerPage, searchColumn, searchKeyword, maxId, newId, startDate, endDate);
        }
        catch (CocktailException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new CocktailException("getUserConfigDownloadHistoryList Fail.", ex, ExceptionType.CommonInquireFail);
        }

        return userConfigDownloadHistoryList;
    }
}
