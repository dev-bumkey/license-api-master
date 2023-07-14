package run.acloud.api.auth.service;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import run.acloud.api.auth.enums.LanguageType;
import run.acloud.api.auth.enums.ServiceMode;
import run.acloud.api.auth.enums.UserRole;
import run.acloud.api.auth.vo.AuthVO;
import run.acloud.api.auth.vo.UserVO;
import run.acloud.api.configuration.enums.AccountType;
import run.acloud.api.configuration.enums.GradeApplyState;
import run.acloud.api.configuration.enums.UserAuthType;
import run.acloud.api.configuration.service.AccountGradeService;
import run.acloud.api.configuration.service.AccountService;
import run.acloud.api.configuration.service.UserService;
import run.acloud.api.configuration.vo.AccountGradeVO;
import run.acloud.api.configuration.vo.AccountVO;
import run.acloud.api.configuration.vo.ServiceListVO;
import run.acloud.api.configuration.vo.ServiceVO;
import run.acloud.api.cserver.vo.ServiceRelationVO;
import run.acloud.commons.security.password.MessageDigestPasswordEncoder;
import run.acloud.commons.security.password.ShaPasswordEncoder;
import run.acloud.commons.service.LicenseService;
import run.acloud.commons.util.CryptoUtils;
import run.acloud.commons.vo.CocktailLicenseValidVO;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;
import run.acloud.framework.properties.CocktailLicenseProperties;
import run.acloud.framework.properties.CocktailServiceProperties;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Slf4j
@Service
public class LoginService {


//	@Resource(name = "cocktailSession")
//    private SqlSessionTemplate sqlSession;

    @Autowired
    private UserService userService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountGradeService accountGradeService;

    @Autowired
    private LicenseService licenseService;

    @Autowired
    private CocktailLicenseProperties licenseProperties;

    @Autowired
    private CocktailServiceProperties cocktailServiceProperties;

    public UserVO login(AuthVO auth) throws CocktailException {
        return this.login(auth, null);
    }

    public UserVO login(AuthVO auth, Predicate<UserVO> p) throws CocktailException {

        /**
         * 로그인 유형
         * - ADMIN(ADMIN) - ADMIN 권한자
         * - 멤버(DEVOPS) - SYSTEM, DEVOPS 권한자
         *
         * 로그인 유형이 '멤버'으로 접속하면 다시 조회하여 사용자 권한에 따라 로그인 처리
         */
        UserVO user = null;
        int loginFailCount = 0; // 로그인 실패 건수

        try {
            /**
             *
             * 사용자 ID에 의한 사용자가 없을 경우
             * 로그인 실패시 실패 횟수를 카운트하지 않고
             * 실패 이력만 등록
             *
             */
            try {
                user = this.setUser(auth);

                // 권한 체크
                if(p != null){
                    if (!p.test(user)) {
                        throw new CocktailException("Not authorized!!", ExceptionType.NotAuthorized);
                    }
                }
            } catch (Exception e) {
                throw e;
            }

            /**
             *
             * 사용자 ID에 의한 사용자가 있을 경우
             *
             */
            if (user != null) {
                // errData
                Map<String, Object> errData = Maps.newHashMap();

                try {
                    /**
                     * 클로벌로 사용을 위해 로그인 실패 건수를 미리 계산하여 셋팅
                     */
                    loginFailCount = user.getLoginFailCount() + 1;

                    if(UserRole.DEVOPS == UserRole.valueOf(auth.getRole())) {
                        if (BooleanUtils.toBoolean(licenseProperties.isLicenseEnable())) {
                            // check validation license
                            user.setLicenseValid(licenseService.checkLicense());
                            if (user.getLicenseValid() != null) {
                                // Login 처리 수행
                                this.checkLicenseByLogin(user.getLicenseValid(), errData);
                            }
                            // License 체크 값이 없다면 License가 없으므로 생성하고 다시 validation 체크 수행
                            else {
                                // Trial License 생성
                                licenseService.issueTrialLicense(null, false, false, false);
                                // check validation license
                                user.setLicenseValid(licenseService.checkLicense());
                                // Login 처리 수행
                                this.checkLicenseByLogin(user.getLicenseValid(), errData);
                            }
                        }

                        // check validation => ACCOUNT_CODE
                        if(!(user.getAccount() != null && StringUtils.equals(auth.getAccountId(), user.getAccount().getAccountCode()))){
                            throw new CocktailException("Account Code invalid.", ExceptionType.AccountIdInvalid);
                        }

                        /**
                         * account grade 만료일 & 미납 체크, DEVOPS 일때만 체크
                         * 2019.05.30, coolingi
                         *
                         * Online Service의 경우에만 만료일이 설정되므로 온라인일 경우에만 만료일 체크하도록 수정
                         * 2021.01.06 hjchoi
                         */
                        if (user.getAccount().getAccountType() != null && user.getAccount().getAccountType().isOnline()) {
                            // grade 정보 조회
                            AccountGradeVO accountGradeVO = new AccountGradeVO();
                            accountGradeVO.setAccountSeq(user.getAccount().getAccountSeq());
                            accountGradeVO = accountGradeService.getAccountGrade(accountGradeVO);

                            if (accountGradeVO != null) {
                                // 1. 만료일 체크, 만료일 정보가 있을때만 체크한다.
                                String endDate = accountGradeVO.getApplyEndDate();
                                if (StringUtils.isNotEmpty(endDate)) {
                                    // region time zone에 맞는 날짜 설정 후 체크
                                    String regionTimeZone = cocktailServiceProperties.getRegionTimeZone(); // get regionTimeZone
                                    DateTimeFormatter dueDateFmt = DateTimeFormat.forPattern("yyyy-MM-dd");
                                    DateTime dueDate = dueDateFmt.withZone(DateTimeZone.forID(regionTimeZone)).parseDateTime(endDate);

                                    // 만료일이 현재시간 보다 작으면 Exception 처리
                                    if (dueDate.plusDays(1).isBeforeNow()) {
                                        throw new CocktailException("플랫폼의 사용기간이 만료 되었습니다.", ExceptionType.AccountHasExpired);
                                    }
                                }

                                // 2. 요금미납여부 체크, exception 처리를 하지 않고 상태만 셋팅하여 넘겨준다.
                                GradeApplyState gradeState = accountGradeVO.getApplyState();
                                if (gradeState == GradeApplyState.UNPAID) {
                                    //accountUnpaid
                                    user.setAccountUnpaid(true); // 플랫폼의 사용요금이 미납 되었습니다.
                                }

                                // 3. accountGrade 셋팅
                                if (user.getAccount() != null) {
                                    user.getAccount().setAccountGrade(accountGradeVO);
                                }

                            }
                        }
                    }

                    /** 인증방식별 체크 시작, 2019-07-29, coolingi, 수정 *************************************/
                    MessageDigestPasswordEncoder passwordEncoder = new ShaPasswordEncoder(256);
                    // Role이 ADMIN 이거나 ID/PW 인증방식일 경우
                    if(UserRole.ADMIN == UserRole.valueOf(auth.getRole()) || EnumSet.of(UserAuthType.PLAIN).contains(user.getAccount().getUserAuthType())) {

                        //2021-09-14, coolingi, password encoding 방식 변경, 패스워드 체크 방식 변경
                        if ( !passwordEncoder.isPasswordValid(user.getPassword(), auth.getPassword(), user.getHashSalt()) ) {
                            throw new CocktailException("User password incorrect", ExceptionType.UserPasswordIncorect);
                        }

                        // ID/PW 방식으로 인증 성공 했을때, Password 정책 체크
                        userService.setPasswordPolicyParam(user, passwordEncoder);

                        // 2021-09-15, coolingi, 로그인 성공시 salt 값 없으면, salt 값 존재하는 값으로 password 변경 및 DB update
                        if(StringUtils.isBlank(user.getHashSalt())){
                            String hashSalt = CryptoUtils.generateSalt();

                            user.setHashSalt(hashSalt);
                            user.setPassword(passwordEncoder.encodePassword(auth.getPassword(), hashSalt));

                            userService.changeOnlyPassword(user);
                        }
                    }

                    // Azure Active Directory 인증방식일 경우, ADD 연동, Password 정책 체크 안한다.
                    else if(user.getAccount().getUserAuthType() == UserAuthType.AAD) {
                        boolean authFlag = userService.authenticateUserOfAzureAD(user.getAccount().getAppId(), auth.getUsername(), auth.getPassword());
                        if (!authFlag) {
                            throw new CocktailException("User password incorrect", ExceptionType.UserPasswordIncorect);
                        }

                        // Azure Active Directory 인증방식일 경우, 패스워드 정책 적용안함
                        user.setPasswordChangeRequired(false);
                        user.setPasswordPeriodExpired(false);
                    }
                    // SSO 인증방식일 경우
                    else if(user.getAccount().getUserAuthType() == UserAuthType.AD) {
                        /**
                         * AD 인증관련 로직 추가
                         */
                        /****************************************************************************************/


                        /****************************************************************************************/
                    }
                    /** 인증방식별 체크 끝. *************************************/

                    /*
                     * OTP 로그인 처리
                     * 1. 로그인 시도
                     * 2. 사용자의 OTP 사용여부 확인
                     * 3. OTP 사용시에는 opt사용여부, userSeq, passwordChangeRequired, passwordPeriodExpired만 셋팅하여 응답
                     * 4. OTP 인증 후 UI에서 다시 로그인 요청시에 사용자 정보 셋팅하여 응답
                     * 5. OTP 인증 성공/실패 여부에 따라 로그인 접근이력 등록
                     */
                    boolean setUserInfo = true;
                    if (BooleanUtils.toBoolean(user.getOtpUseYn())) {
                        if (StringUtils.isBlank(auth.getCertified())) {
                            setUserInfo = false;
                        } else {
                            // OTP 인증 실패
                            if (!StringUtils.equalsIgnoreCase(auth.getCertified(), "S")) {
                                throw new CocktailException("User OTP authentication failed", ExceptionType.UserAuthenticationFailed);
                            }
                        }
                    }

                    /**
                     * 2022.03.15 hjchoi
                     * 플랫폼별 레지스트리 pull 전용 사용자 추가
                     * 정책 변경으로 별도 마이그래이션 처리없이 로그인시 마이그래이션 처리하도록 함.
                     */
                    if (user.getAccount() != null
                            && StringUtils.isBlank(user.getAccount().getRegistryDownloadUserId())
                    ) {
                        // 레지스트리 pull user 생성 및 존재하는 레지스트리에 pull user member 추가
                        accountService.createAccountRegistryPullUser(user.getAccount());
                    }

                    if (setUserInfo) {
                        /*
                         * 1. 권한 셋팅
                         * 2. 워크스페이스 셋팅
                         */
                        user = this.setUserWorkspace(user);

                        // APP 계정 유형에서 ADMIN 권한자는 defaultLanguage로 셋팅하도록 함.
                        if (StringUtils.equalsIgnoreCase(cocktailServiceProperties.getType(), AccountType.APP.getCode())) {
                            if (UserRole.ADMIN == UserRole.valueOf(user.getUserRole())
                                    && !StringUtils.equals(cocktailServiceProperties.getDefaultLanguage(), user.getUserLanguage().getCode())) {
                                user.setUserLanguage(LanguageType.valueOf(cocktailServiceProperties.getDefaultLanguage()));
                                user.setUpdater(user.getUserSeq());
                                userService.editUserLanguage(user);
                            }
                        }

                        // update last login datetime
                        userService.updateLoginTimestampBySeq(user.getUserSeq());


                        user.setPassword(null);
                        user.setRoles(null); // 사용자가 가지고 있는 Roles에 대한 목록인듯 Null 셋팅해도 문제 없어 보임

                        /**
                         * 로그인 성공시 초기화 처리
                         */
                        userService.updateLoginFailCountBySeq(0, user.getUserSeq());

                    } else {
                        UserVO otpUser = new UserVO();
                        otpUser.setUserSeq(user.getUserSeq());
                        otpUser.setOtpUseYn(user.getOtpUseYn());
                        otpUser.setPasswordChangeRequired(user.isPasswordChangeRequired());
                        otpUser.setPasswordPeriodExpired(user.isPasswordPeriodExpired());

                        return otpUser;
                    }


                } catch (CocktailException ce) {
                    if (!EnumSet.of(
                              ExceptionType.AccountHasExpired
                            , ExceptionType.InvalidLicense
                            , ExceptionType.LicenseIsExpired).contains(ce.getType())
                    ) {
                        /**
                         * 로그인 실패 횟수 처리
                         */
                        userService.updateLoginFailCountBySeq(loginFailCount, user.getUserSeq());
                        if (loginFailCount > 0) {
                            errData.put("loginFailCount", loginFailCount);
                        }
                        ce.setData(errData);
                    }
                    throw ce;
                }
            }

        } catch (CocktailException ce) {
            throw ce;
        } catch (Exception e) {
            throw new CocktailException(e.getMessage(), e, ExceptionType.InternalError);
        }

        return user;
    }

    /**
     * 권한에 따른 사용자 정보 조회
     *
     * @param auth
     * @return
     * @throws Exception
     */
    public UserVO setUser(AuthVO auth) throws Exception {
        UserVO checkUser = new UserVO();
        checkUser.setUserId(auth.getUsername());
        checkUser.setUserRole(auth.getRole());
        AccountVO account = new AccountVO();
        account.setAccountCode(auth.getAccountId());
        List<UserVO> authUsers = userService.getUsersForCheck(checkUser);
        UserVO user = null;

        if(CollectionUtils.isEmpty(authUsers)){
            throw new CocktailException("User id not found", ExceptionType.UserIdNotFound);
        }else{
            if(StringUtils.equalsIgnoreCase(UserRole.DEVOPS.getCode(), auth.getRole())){
                Optional<UserVO> authUserOptional = authUsers.stream()
                        .filter(au -> StringUtils.equalsIgnoreCase(au.getUserId(), auth.getUsername()) && StringUtils.equalsIgnoreCase(au.getAccount().getAccountCode(), auth.getAccountId()))
                        .findFirst();

                if(authUserOptional.isPresent()){
                    authUsers = new ArrayList<>();
                    authUsers.add(authUserOptional.get());
                }
            }

            // 조회한 사용자의 권한으로 사용자 조회
            user = this.userService.getUserById(auth.getUsername(), authUsers.get(0).getRoles().get(0), auth.getAccountId());
            if (user == null) {
                throw new CocktailException("User id not found", ExceptionType.UserIdNotFound);
            } else {
                if(!BooleanUtils.toBoolean(user.getUseYn())) {
                    throw new CocktailException("Removed user", ExceptionType.InactivatedUser);
                }
                if(BooleanUtils.toBoolean(user.getInactiveYn())) {
                    throw new CocktailException("Inactivated user", ExceptionType.InactivatedUser);
                }
                /*
                 * 클라우드보안인증 요건으로 휴면사용자 처리
                 * 디지털서비스는 필수이고, 환경변수를 통해 기능을 enabled 처리 가능
                 */
                if(cocktailServiceProperties.isUserSleepEnabled() || ServiceMode.valueOf(cocktailServiceProperties.getMode()) == ServiceMode.SECURITY_ONLINE) {
                    // 사용자 휴면 여부
                    userService.setUserSleepParam(user);
                    if(BooleanUtils.toBoolean(user.getSleepYn())) {
                        throw new CocktailException("Sleep user", ExceptionType.InactivatedUser);
                    }
                }
                /**
                 * 2018.10.31. 멀티 테넌트 관련 수정
                 * User의 Role은 앞으로 하나여야함. 혹시라도 여러개의 Role을 가지고 있다면 어떻게 처리할 것인지? => 일단 첫번째 Role을 부여하도록 함
                 */
                for(String roles : user.getRoles()) {
                    user.setUserRole(roles);
                    break;
                }
            }
        }

        return user;
    }

    public UserVO setUserWorkspace(UserVO user) throws Exception {

        /**
         * 2018.10.31. 멀티 테넌트 관련 수정
         * 1. LastServiceSEQ와 무관하게 사용자가 가진 Workspace 권한 목록을 조회하여 로그인 가능 여부를 우선 판단.
         * 2. LastServiceSEQ와 권한있는 Workspaces 와 비교 처리 후 권한이 있을 경우에 해당 Workspace로 로그인 허용.
         * 3. LastServiceSEQ가 존재하지 않으면 사용자가 권한을 가지고 있는 워크스페이스를 조회하여 첫번째 워크스페이스로 로그인.
         *    LastServiceSEQ에 Workspace 정보 업데이트 처리.
         */
        List<ServiceListVO> serviceList = Lists.newArrayList();
        List<Integer> serviceSeqList = Lists.newArrayList();

        UserRole userRole = UserRole.valueOf(user.getUserRole());
        boolean existsWorkspace = true;

        if(userRole.isAdmin()){
            user.setLastServiceSeq(null);
            int isSuccess = userService.updateLastServiceSeq(user);
            if (isSuccess < 1) {
                log.info("사용자가 마지막으로 사용한 워크스페이스 정보를 업데이트 하였으나 성공하지 못하였습니다.");
            }
        }else{
            serviceList = userService.getAuthorizedServicesBySeq(user.getUserSeq());
            serviceSeqList = serviceList.stream().map(ServiceListVO::getServiceSeq).collect(Collectors.toList());

            if(StringUtils.isNotBlank(user.getLastServiceSeq())){
                if(CollectionUtils.isEmpty(serviceList)) {
                    user.setLastServiceSeq(null);
                    int isSuccess = this.userService.updateLastServiceSeq(user);
                    if (isSuccess < 1) {
                        log.info("사용자가 마지막으로 사용한 워크스페이스 정보를 업데이트 하였으나 성공하지 못하였습니다.");
                    }
                    // hjchoi.20200721 - 할당된 워크스페이스가 없더라도 로그인 가능하도록 해당 로직 주석처리
                    //                    if(UserRole.valueOf(userRole).isDevops()){
                    //                        throw new CocktailException("There is no authorized workspace.", ExceptionType.NoAuthorizedServices);
                    //                    }
                }else{
                    if(!serviceSeqList.contains(Integer.valueOf(user.getLastServiceSeq()))){
                        log.info("User("+user.getUserSeq()+")'s Workspace permission has been deleted. : " + user.getLastServiceSeq());
                        log.info("User("+user.getUserSeq()+")'s Newly selected Workspace SEQ = " + serviceSeqList.get(0).toString());
                        existsWorkspace = false;
                    }else{
                        UserVO finalUser = user;
                        ServiceListVO lastService = serviceList.stream().filter(s -> (s.getServiceSeq().equals(Integer.valueOf(finalUser.getLastServiceSeq())))).findFirst().orElseGet(() ->null);
                        if (lastService != null) {
                            ServiceVO loginService = new ServiceVO();
                            loginService.setServiceSeq(lastService.getServiceSeq());
                            loginService.setServiceName(lastService.getServiceName());
                            loginService.setColorCode(lastService.getColorCode());
                            user.setLastService(loginService);
                        }
                    }
                }
            }else{
                if(CollectionUtils.isEmpty(serviceList)) {
                    user.setLastServiceSeq(null);
                    // hjchoi.20200721 - 할당된 워크스페이스가 없더라도 로그인 가능하도록 해당 로직 주석처리
                    //                    if(UserRole.valueOf(userRole).isDevops()){
                    //                        throw new CocktailException("There is no authorized workspace.", ExceptionType.NoAuthorizedServices);
                    //                    }
                }else{
                    log.info("User("+user.getUserSeq()+")'s Last access workspace is missing. : " + user.getLastServiceSeq());
                    log.info("User("+user.getUserSeq()+")'s Newly selected Workspace SEQ = " + serviceSeqList.get(0).toString());
                    existsWorkspace = false;
                }
            }

            if(!existsWorkspace){
                user.setLastServiceSeq(serviceList.get(0).getServiceSeq().toString());
                ServiceVO loginService = new ServiceVO();
                loginService.setServiceSeq(serviceList.get(0).getServiceSeq());
                loginService.setServiceName(serviceList.get(0).getServiceName());
                loginService.setColorCode(serviceList.get(0).getColorCode());
                user.setLastService(loginService);
                int isSuccess = this.userService.updateLastServiceSeq(user);
                if (isSuccess < 1) {
                    log.info("사용자가 마지막으로 사용한 워크스페이스 정보를 업데이트 하였으나 성공하지 못하였습니다.");
                }
            }

            // 워크스페이스 권한 셋팅
            if(userRole.isDevops()){
                UserVO userWithSvc = userService.getByUserSeq(user.getUserSeq());
                if (userWithSvc != null && CollectionUtils.isNotEmpty(userWithSvc.getUserRelations())) {
                    UserVO finalUser = user;
                    Optional<ServiceRelationVO> serviceRegistryOptional = userWithSvc.getUserRelations().stream().filter(r -> (finalUser.getLastServiceSeq().equals(String.valueOf(r.getServiceSeq())))).findFirst();
                    serviceRegistryOptional.ifPresent(serviceRelationVO -> user.setUserGrant(serviceRelationVO.getUserGrant().getCode()));
                }
            }
        }

        // 전체 서비스 수
        user.setServiceTotalCount(CollectionUtils.size(serviceList));

        return user;

    }

    private void checkLicenseByLogin(CocktailLicenseValidVO licenseValid, Map<String, Object> errData) throws Exception {
        if (licenseValid != null) {
            if (!licenseValid.isValid()) {
                throw new CocktailException("LicenseKey invalid.", ExceptionType.InvalidLicense, licenseValid.getMessage());
            }
            if (licenseValid.isExpired()) {
                errData.put("expireDate", licenseValid.getExpireDate());
//                throw new CocktailException("LicenseKey is expired.", ExceptionType.LicenseIsExpired, String.format("Expire date : %s", licenseValid.getExpireDate()));
                throw new CocktailException("LicenseKey is expired.", ExceptionType.LicenseIsExpired, errData);
            }
        }
    }
}
