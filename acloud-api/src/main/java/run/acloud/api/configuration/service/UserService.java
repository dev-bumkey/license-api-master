package run.acloud.api.configuration.service;

import com.microsoft.aad.adal4j.AuthenticationContext;
import com.microsoft.aad.adal4j.AuthenticationResult;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import run.acloud.api.auth.dao.IUserMapper;
import run.acloud.api.auth.enums.ServiceMode;
import run.acloud.api.auth.enums.UserRole;
import run.acloud.api.auth.vo.UserExternalVO;
import run.acloud.api.auth.vo.UserOtpVO;
import run.acloud.api.auth.vo.UserVO;
import run.acloud.api.code.service.CodeService;
import run.acloud.api.code.vo.CodeVO;
import run.acloud.api.configuration.constants.UserConstants;
import run.acloud.api.configuration.dao.IAccountMapper;
import run.acloud.api.configuration.enums.UserAuthType;
import run.acloud.api.configuration.vo.AccountVO;
import run.acloud.api.configuration.vo.ServiceListVO;
import run.acloud.commons.security.password.MessageDigestPasswordEncoder;
import run.acloud.commons.security.password.ShaPasswordEncoder;
import run.acloud.commons.service.EmailService;
import run.acloud.commons.util.CryptoUtils;
import run.acloud.commons.util.DateTimeUtils;
import run.acloud.commons.util.Utils;
import run.acloud.framework.context.ContextHolder;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;
import run.acloud.framework.properties.CocktailServiceProperties;
import run.acloud.framework.util.ExceptionMessageUtils;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;

@Slf4j
@Service
public class UserService {
    @Resource(name = "cocktailSession")
    private SqlSessionTemplate sqlSession;

    @Autowired
    private CodeService codeService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private CocktailServiceProperties cocktailServiceProperties;

    private final MessageDigestPasswordEncoder passwordEncoder = new ShaPasswordEncoder(256);


    public UserVO getUserById(String userId, String userRole, String accountId) throws Exception {
        IUserMapper mapper = this.sqlSession.getMapper(IUserMapper.class);

        return mapper.selectByUserId(userId, userRole, accountId);
    }

    public UserVO getByUserSeq(Integer userSeq) throws Exception {
        IUserMapper mapper = this.sqlSession.getMapper(IUserMapper.class);
        return mapper.selectByUserSeq(userSeq, "Y");
    }

    public UserVO getByUserSeq(Integer userSeq, String useYn) throws Exception {
        IUserMapper mapper = this.sqlSession.getMapper(IUserMapper.class);
        return mapper.selectByUserSeq(userSeq, Utils.getUseYn(useYn));
    }

    public List<UserVO> getUsersByUserSeqs(List<Integer> userSeqs) throws Exception {
        IUserMapper mapper = this.sqlSession.getMapper(IUserMapper.class);
        return mapper.selectByUserSeqs(userSeqs);
    }

    public UserVO getUser(Integer userSeq) throws Exception {
        IUserMapper mapper = this.sqlSession.getMapper(IUserMapper.class);
        return mapper.getUser(userSeq);
    }

    public List<UserVO> getUsers(UserVO params) throws Exception {
        IUserMapper mapper = this.sqlSession.getMapper(IUserMapper.class);
        return mapper.getUsers(params);
    }

    public List<UserVO> getUsersForCheck(UserVO params) throws Exception {
        IUserMapper mapper = this.sqlSession.getMapper(IUserMapper.class);
        return mapper.getUsersForCheck(params);
    }

    public List<UserExternalVO> getUsersForExternal(Integer userSeq, String userId, String userRole, Integer accountSeq, String accountCode) throws Exception {
        IUserMapper mapper = this.sqlSession.getMapper(IUserMapper.class);
        return mapper.getUsersForExternal(userSeq, userId, userRole, accountSeq, accountCode);
    }

    public List<UserVO> getUsersOfAccount(Integer accountSeq) throws Exception {
        IUserMapper mapper = this.sqlSession.getMapper(IUserMapper.class);
        return mapper.getUsersOfAccount(accountSeq);
    }

    public List<UserVO> getUsersOfAccountWithPwdPolicy(Integer accountSeq) throws Exception {
        List<UserVO> users = this.getUsersOfAccount(accountSeq);

        // Set passwordPolicy
        if (CollectionUtils.isNotEmpty(users)) {
            IAccountMapper dao = sqlSession.getMapper(IAccountMapper.class);

            // get Account
            AccountVO account = dao.getAccount(accountSeq);

            if (account != null) {
                IUserMapper userDao = this.sqlSession.getMapper(IUserMapper.class);
                Date todayDate = userDao.getCurrentDate();

                for (UserVO userRow : users) {
                    if (account.getUserAuthType() == UserAuthType.PLAIN) {
                        this.setPasswordPolicyParam(userRow, passwordEncoder, todayDate);
                    } else if (account.getUserAuthType() == UserAuthType.AAD) {
                        userRow.setPasswordChangeRequired(false);
                        userRow.setPasswordPeriodExpired(false);
                    }
                }
            }
        }

        return users;
    }

    public UserVO getUserOfAccount(Integer accountSeq, Integer userSeq) throws Exception {
        IUserMapper mapper = this.sqlSession.getMapper(IUserMapper.class);
        return mapper.getUserOfAccount(accountSeq, userSeq);
    }

    public UserVO getUserOfAccountWithPwdPolicy(Integer accountSeq, Integer userSeq) throws Exception {
        UserVO user = this.getUserOfAccount(accountSeq, userSeq);

        if (user != null) {
            IAccountMapper dao = sqlSession.getMapper(IAccountMapper.class);

            // get Account
            AccountVO account = dao.getAccount(accountSeq);

            if (account != null) {

                if (account.getUserAuthType() == UserAuthType.PLAIN) {
                    this.setPasswordPolicyParam(user, passwordEncoder);
                } else if (account.getUserAuthType() == UserAuthType.AAD) {
                    user.setPasswordChangeRequired(false);
                    user.setPasswordPeriodExpired(false);
                }
            }
        }

        return user;
    }

    public List<UserVO> getAccountUsersForAccount(Integer accountSeq) throws Exception {
        IUserMapper mapper = this.sqlSession.getMapper(IUserMapper.class);
        return mapper.getAccountUsersForAccount(accountSeq);
    }

    public List<UserVO> getUsersForAccount(Integer accountSeq) throws Exception {
        IUserMapper mapper = this.sqlSession.getMapper(IUserMapper.class);
        return mapper.getUsersForAccount(accountSeq);
    }

    public List<UserVO> getUsersForWorkspace(String userRole, String userGrant, Integer accountSeq) throws Exception {
        IUserMapper mapper = this.sqlSession.getMapper(IUserMapper.class);
        return mapper.getUsersForWorkspace(userRole, userGrant, accountSeq);
    }

    @Transactional(transactionManager = "transactionManager")
    public void addUser(UserVO params) throws Exception {
        IUserMapper mapper = this.sqlSession.getMapper(IUserMapper.class);
        mapper.addUser(params);
        mapper.addUserRoles(params);
    }

    @Transactional(transactionManager = "transactionManager")
    public void addUserExternal(UserVO params) throws Exception {
        IUserMapper userDao = this.sqlSession.getMapper(IUserMapper.class);
        IAccountMapper accountDao = sqlSession.getMapper(IAccountMapper.class);
        userDao.addUser(params);
        userDao.addUserRole(params.getUserSeq(), params.getUserRole());
        accountDao.addUserOfAccount(params.getAccountSeq(), Collections.singletonList(params.getUserSeq()), 1);
    }

    @Transactional(transactionManager = "transactionManager")
    public void editUser(UserVO params) throws Exception {
        IUserMapper mapper = this.sqlSession.getMapper(IUserMapper.class);
        mapper.editUser(params);
        mapper.removeUserRole(params);
        mapper.addUserRoles(params);
    }

    @Transactional(transactionManager = "transactionManager")
    public void editUserLanguage(UserVO params) throws Exception {
        IUserMapper mapper = this.sqlSession.getMapper(IUserMapper.class);
        mapper.editUserLanguage(params);
    }

    @Transactional(transactionManager = "transactionManager")
    public void editUserTimezone(UserVO params) throws Exception {
        IUserMapper mapper = this.sqlSession.getMapper(IUserMapper.class);
        mapper.editUserTimezone(params);
    }

    @Transactional(transactionManager = "transactionManager")
    public void editUserFromAccount(UserVO params) throws Exception {
        IUserMapper mapper = this.sqlSession.getMapper(IUserMapper.class);
        mapper.editUser(params);
    }

    @Transactional(transactionManager = "transactionManager")
    public UserVO editUserWithCheck(UserVO user) throws Exception {
        UserVO userCurr = this.getByUserSeq(user.getUserSeq());
        if (userCurr == null) {
            throw new CocktailException("User not found", ExceptionType.UserIdNotFound);
        }else {

            this.checkEditUser(userCurr, user);

            this.editUser(userCurr);

            // 사용자 수정시 비밀번호 초기화 하지 않도록 수정
            // 2021.06.14, hjchoi
//            if (BooleanUtils.toBoolean(user.getInitPasswordYn())) {
//                this.resetPasswordUser(user);
//            }

            userCurr = this.getByUserSeq(user.getUserSeq());
            userCurr.setPassword(null);
        }

        return userCurr;
    }

    @Transactional(transactionManager = "transactionManager")
    public UserVO editUserWithCheckWithConsumer(UserVO user, Consumer<UserVO> throwingConsumer) throws Exception {
        UserVO userCurr = this.getByUserSeq(user.getUserSeq());
        if (userCurr == null) {
            throw new CocktailException("User not found", ExceptionType.UserIdNotFound);
        }else {

            this.checkEditUser(userCurr, user);

            if (throwingConsumer != null) {
                throwingConsumer.accept(userCurr);
            }

            userCurr = this.getByUserSeq(user.getUserSeq());
            userCurr.setPassword(null);
        }

        return userCurr;
    }

    public void removeUser(UserVO params) throws Exception {
        IUserMapper mapper = this.sqlSession.getMapper(IUserMapper.class);
        mapper.removeUser(params);
    }

    public void deleteUser(Integer userSeq) throws Exception {
        IUserMapper mapper = this.sqlSession.getMapper(IUserMapper.class);
        mapper.deleteUserRole(userSeq);
        mapper.deleteUser(userSeq);
    }

    @Transactional(transactionManager = "transactionManager")
    public void removeUserWithCheck(Integer userSeq) throws Exception {
        UserVO user = this.getByUserSeq(userSeq);
        if (user == null) {
            throw new CocktailException("User not found", ExceptionType.UserIdNotFound);
        }else{
            if (userSeq == 1){
                throw new CocktailException("Root Admin User do not edit", ExceptionType.UserRootAdminDontAction);
            }

            // SYSTEM ROLE 사용자가 Account에 종속되어 있는지 체크
            // 이외의 사용자는 Account, Service에 종속되어 있는지 체크
            if(!this.canRemoveUser(user)){
                throw new CocktailException("Can not delete. because a user have a account.", ExceptionType.UserHaveAccount);
            }

            user.setUpdater(ContextHolder.exeContext().getUserSeq());
            this.removeUser(user);
        }
    }

    // 2021-09-15, coolingi, 패스워드 체크 부분을 DB 처리가 아닌 로직 처리로 변경.
    public boolean isValidPassword(UserVO params) throws Exception {
        UserVO currUser = this.getUser(params.getUserSeq());

        boolean valid = false;

        // 1. 입력된 PW가 기존 PW와 같은지
        valid = passwordEncoder.isPasswordValid(currUser.getPassword(), params.getPassword(), currUser.getHashSalt());

        // 2. 입력된 new PW가 기존 PW와 다른지 체크
        if(StringUtils.isNotBlank(params.getNewPassword())){
            valid = (valid && !passwordEncoder.isPasswordValid(currUser.getPassword(), params.getNewPassword(), currUser.getHashSalt()));
        }

        return valid;
    }

    public void changeOnlyPassword(UserVO params) throws Exception {
        IUserMapper mapper = this.sqlSession.getMapper(IUserMapper.class);
        mapper.changeOnlyPassword(params);
    }

    public void changePassword(UserVO params) throws Exception {
        IUserMapper mapper = this.sqlSession.getMapper(IUserMapper.class);
        mapper.changePassword(params);
    }

    @Transactional(transactionManager = "transactionManager")
    public void resetPasswordUser(UserVO user, boolean useEmail) throws Exception {
        IUserMapper dao = this.sqlSession.getMapper(IUserMapper.class);
        useEmail = useEmail && emailService.availableEmail();

        // 2021-09-14, coolingi, salt에 따른 패스워드 셋팅
        String password;
        if (useEmail) {
            password = RandomStringUtils.randomAlphanumeric(20);
        } else {
            password = UserConstants.INIT_USER_PASSWORD;
        }

        // salt 값 생성 및 패스워드 생성
        String hashSalt = CryptoUtils.generateSalt();

        user.setHashSalt(hashSalt);
        user.setPassword(passwordEncoder.encodePassword(password, hashSalt));
        user.setPasswordInterval(UserConstants.INTERVAL_CHANGE_PASSWORD); // password 변경 후 90일로 만료기한 변경
        user.setUpdater(ContextHolder.exeContext().getUserSeq());
        dao.resetPassword(user);

        if (useEmail) {
            // send email
            emailService.sendMail(user.getUserId(), "Reset Password", String.format("Temp Password : %s", password), true);
        }
    }

    @Transactional(transactionManager = "transactionManager")
    public void resetPasswordUserWithCheck(Integer userSeq, boolean useEmail) throws Exception {
        UserVO user = this.getByUserSeq(userSeq);
        if (user == null) {
            throw new CocktailException("User not found", ExceptionType.UserIdNotFound);
        }else{
            this.resetPasswordUser(user, useEmail);
        }
    }

    public void extendPassword(UserVO params) throws Exception {
        IUserMapper mapper = this.sqlSession.getMapper(IUserMapper.class);
        mapper.extendPassword(params);
    }

    public void updateLoginTimestamp(String userName) throws Exception {
        IUserMapper mapper = this.sqlSession.getMapper(IUserMapper.class);
        mapper.updateLoginTimestamp(userName);
    }

    public void updateLoginTimestampBySeq(Integer userSeq) throws Exception {
        IUserMapper mapper = this.sqlSession.getMapper(IUserMapper.class);
        mapper.updateLoginTimestampBySeq(userSeq);
    }

    public void updateActiveTimestampBySeq(Integer userSeq, Integer updater) throws Exception {
        IUserMapper mapper = this.sqlSession.getMapper(IUserMapper.class);
        mapper.updateActiveTimestampBySeq(userSeq, updater);
    }

    public void updateLoginFailCountBySeq(int loginFailCount, Integer userSeq) throws Exception {
        IUserMapper mapper = this.sqlSession.getMapper(IUserMapper.class);
        mapper.updateLoginFailCountBySeq(loginFailCount, userSeq);
    }

    public List<ServiceListVO> getAuthorizedServicesBySeq(Integer userSeq) throws Exception {
        IUserMapper mapper = this.sqlSession.getMapper(IUserMapper.class);

        return mapper.getAuthorizedServicesBySeq(userSeq);
    }

    public String getLastServiceBySeq(Integer userSeq) throws Exception {
        IUserMapper mapper = this.sqlSession.getMapper(IUserMapper.class);

        return mapper.getLastServiceBySeq(userSeq);
    }

    public int updateLastServiceSeq(UserVO user) throws Exception {
        IUserMapper mapper = this.sqlSession.getMapper(IUserMapper.class);

        return mapper.updateLastServiceSeq(user);
    }

    public void setPasswordPolicyParam(UserVO user, MessageDigestPasswordEncoder passwordEncoder) throws Exception {
        this.setPasswordPolicyParam(user, passwordEncoder, null);
    }

    public void setPasswordPolicyParam(UserVO user, MessageDigestPasswordEncoder passwordEncoder, Date todayDate) throws Exception {
        if(user != null){
            if(passwordEncoder == null){
                passwordEncoder = new ShaPasswordEncoder(256);
            }
            // 사용자 비밀번호는 랜덤 문자열로 사용자 이메일로 전송하는 방식으로 변경하여 기존 초기화 비밀번호는 admin만 사용함.
            // 2021.06.14, hjchoi

            // 2021-09-14, coolingi, init passwd 값 생성 로직 변경
            String initAdminPassword = passwordEncoder.encodePassword(UserConstants.INIT_ADMIN_PASSWORD, user.getHashSalt());
            String initUserPassword = passwordEncoder.encodePassword(UserConstants.INIT_USER_PASSWORD, user.getHashSalt());

            if (StringUtils.equalsAny(user.getPassword(), new String[]{initAdminPassword, initUserPassword})){
                user.setPasswordChangeRequired(true);
            } else if (BooleanUtils.toBoolean(user.getResetPasswordYn())) {
                user.setPasswordChangeRequired(true);
            } else {
                user.setPasswordChangeRequired(false);
            }
            if (todayDate == null) {
                IUserMapper userDao = this.sqlSession.getMapper(IUserMapper.class);
                todayDate = userDao.getCurrentDate();
            }
            user.setPasswordExpirationBeginTime(DateTimeUtils.initMinTime(user.getPasswordExpirationBeginTime()));
            user.setPasswordExpirationEndTime(DateTimeUtils.initMaxTime(user.getPasswordExpirationEndTime()));
            if( todayDate.getTime() > user.getPasswordExpirationBeginTime().getTime()
                    && todayDate.getTime() < user.getPasswordExpirationEndTime().getTime() ){
                user.setPasswordPeriodExpired(false);
            } else {
                user.setPasswordPeriodExpired(true);
            }
        }
    }

    public void setUserSleepParam(UserVO user) throws Exception {
        if(user != null){
            if (user.getSleepPeriod() > UserConstants.INTERVAL_INACTIVE) {
                user.setSleepYn("Y");
            } else {
                user.setSleepYn("N");
            }
        }
    }

    /**
     * Role에 해당하는 User 목록 조회 (Admin 목록 조회하기 위해 생성)
     * @param roleCode
     * @return
     * @throws Exception
     */
    public List<Integer> selectSeqByUserRole(String roleCode) throws Exception {
        IUserMapper mapper = this.sqlSession.getMapper(IUserMapper.class);

        return mapper.selectSeqByUserRole(roleCode);
    }

    /**
     * Account에 속해있는 User SEQ 목록 조회.
     * @param accountSeq
     * @return
     * @throws Exception
     */
    public List<UserVO> selectUsersByAccount(Integer accountSeq, String useYn) throws Exception {
        IUserMapper mapper = this.sqlSession.getMapper(IUserMapper.class);

        return mapper.selectUsersByAccount(accountSeq, Utils.getUseYn(useYn));
    }

    /**
     * Account에 속해있는 System User SEQ 목록 조회.
     * @param accountSeq
     * @return
     * @throws Exception
     */
    public List<UserVO> selectSystemUsersByAccount(Integer accountSeq, String useYn) throws Exception {
        IUserMapper mapper = this.sqlSession.getMapper(IUserMapper.class);

        return mapper.selectSystemUsersByAccount(accountSeq, Utils.getUseYn(useYn));
    }

    /**
     * 사용자 부활.
     * @param user
     * @return
     * @throws Exception
     */
    public Integer rebirthUser(UserVO user) throws Exception {
        IUserMapper mapper = this.sqlSession.getMapper(IUserMapper.class);
        return mapper.rebirthUser(user);
    }

    /**
     * 사용자를 삭제할 수 있는 지 여부
     *
     * @param user
     * @return
     * @throws Exception
     */
    public boolean canRemoveUser(UserVO user) throws Exception{
        if(CollectionUtils.isNotEmpty(user.getRoles())){
            IUserMapper userDao = this.sqlSession.getMapper(IUserMapper.class);
            UserRole userRole = UserRole.valueOf(user.getRoles().get(0));

            // 자기 자신은 삭제 불가
            if(user.getUserSeq().equals(ContextHolder.exeContext().getUserSeq())) {
                return false;
            }

            // account 안에서 'SYSTEM' 권한자는 적어도 한 명이 있어야 함.
            if(userRole.isSystem()){
                UserVO param = new UserVO();
                param.setUserRole(userRole.getCode());
                param.setAccount(user.getAccount());
                List<UserVO> users = userDao.getUsers(param);
                if(CollectionUtils.isNotEmpty(users) && users.size() <= 1){
                    return false;
                }
            }else{
//                if(UserRole.ADMIN != userRole){
//                    List<Integer> serviceSeqs = userDao.getServiceOfUser(user.getUserSeq());
//                    if(CollectionUtils.isNotEmpty(serviceSeqs)){
//                        return false;
//                    }
//                    List<Integer> accountSeqs = userDao.getAccountOfUser(user.getUserSeq());
//                    if(CollectionUtils.isNotEmpty(accountSeqs)){
//                        return false;
//                    }
//                }
            }
        }

        return true;
    }

    /**
     * 사용자를 수정할 수 있는 지 여부
     *
     * @param user
     * @throws Exception
     */
    public boolean canEditUser(UserVO user) throws Exception{
        UserRole userRole = UserRole.valueOf(user.getRoles().get(0));
        UserRole requestUserRole = UserRole.valueOf(ContextHolder.exeContext().getUserRole());

        if (requestUserRole.isSystem() && userRole.isAdmin()) {
            throw new CocktailException("do not have permission : can't change it with higher privileges.", ExceptionType.NotAuthorizedToRequest, "do not have permission : can't change it with higher privileges.");
        }
        if (requestUserRole.isSysuser() && (userRole.isAdmin() || userRole.isSystem())) {
            throw new CocktailException("do not have permission : can't change it with higher privileges.", ExceptionType.NotAuthorizedToRequest, "do not have permission : can't change it with higher privileges.");
        }
        if (requestUserRole.isDevops() && (userRole.isAdmin() || userRole.isSystem() || userRole.isSysuser())) {
            throw new CocktailException("do not have permission : can't change it with higher privileges.", ExceptionType.NotAuthorizedToRequest, "do not have permission : can't change it with higher privileges.");
        }

        return true;

//        if(userRole.isSystemNSysuser()){
//            List<Integer> accountSeqs = userDao.getAccountOfAccountUser(user.getUserSeq());
//            if(CollectionUtils.isNotEmpty(accountSeqs)){
//                throw new CocktailException("Can not edit. because a user have a system account.", ExceptionType.UserHaveAccount);
//            }
//        }
//        List<Integer> serviceSeqs = userDao.getServiceOfUser(user.getUserSeq());
//        if(CollectionUtils.isNotEmpty(serviceSeqs)){
//            throw new CocktailException("Can not edit. because a user have a workspace.", ExceptionType.UserHaveWorkspace);
//        }
//        List<Integer> accountSeqs = userDao.getAccountOfUser(user.getUserSeq());
//        if(CollectionUtils.isNotEmpty(accountSeqs)){
//            throw new CocktailException("Can not edit. because a user have a system account.", ExceptionType.UserHaveAccount);
//        }
    }

    /**
     * 실제 존재하는 권한인지 - 사용자 권한 체크
     *
     * @param user
     * @throws Exception
     */
    public void checkUserRoleParameter(UserVO user) throws Exception{
        if(user.getRoles().size() > 1){
            throw new CocktailException("A user can not have more than one role.", ExceptionType.UserRoleExceed);
        }else{
            CodeVO code = codeService.getCodeByEnum("USER_ROLE", user.getRoles().get(0));
            if(code == null){
                throw new CocktailException("A user role invalid.", ExceptionType.UserRoleInvalid);
            }else{
                user.setUserRole(user.getRoles().get(0));
            }
        }
    }

    public void checkAddUser(UserVO user) throws Exception{
        ExceptionMessageUtils.checkParameter("userId", user.getUserId(), 50, true);
        ExceptionMessageUtils.checkParameter("password", user.getNewPassword(), 24, false);
        ExceptionMessageUtils.checkParameter("userName", user.getUserName(), 50, true);
        ExceptionMessageUtils.checkParameter("description", user.getDescription(), 300, false);

        if(CollectionUtils.isNotEmpty(user.getRoles())){
            this.checkUserRoleParameter(user);
        }else{
            throw new CocktailException("User role is empty.", ExceptionType.InvalidParameter_Empty);
        }

        if (StringUtils.isNotBlank(user.getNewPassword())) {
            // 비밀번호 정책 체크
            if (Utils.isValidPasswordWithUserInfo(user, user.getNewPassword())) {
                //2021-09-15, coolingi, get salt, get hash value
                String hashSalt = CryptoUtils.generateSalt();
                String encodedPasswd = this.passwordEncoder.encodePassword(user.getNewPassword(), hashSalt);

                user.setHashSalt(hashSalt);
                user.setPassword(encodedPasswd);
            } else {
                throw new CocktailException("Violation of password policy.", ExceptionType.UserPasswordInvalid);
            }
        } else {
            //2021-09-15, coolingi, get salt, get hash value
            String hashSalt = CryptoUtils.generateSalt();
            String encodedPasswd = this.passwordEncoder.encodePassword(UserConstants.INIT_USER_PASSWORD, hashSalt);

            user.setHashSalt(hashSalt);
            user.setPassword(encodedPasswd);
        }

        if (user.getUserLanguage() == null) {
            user.setUserLanguage(UserConstants.defaultLanguage());
        }
        if (StringUtils.isBlank(user.getUserTimezone())) {
            user.setUserTimezone(user.getUserLanguage().getTimezone());
        }

        if (ServiceMode.valueOf(cocktailServiceProperties.getMode()) == ServiceMode.SECURITY_ONLINE) {
            user.setResetPasswordYn("Y");
        }

        user.setPasswordInterval(UserConstants.INTERVAL_CHANGE_PASSWORD);
        user.setUseYn("Y");

        if (StringUtils.isNotBlank(user.getUserJob())) {
            ExceptionMessageUtils.checkParameter("userJob", user.getUserJob(), 100, false);
            user.setUserJob(StringUtils.trim(user.getUserJob()));
        } else {
            user.setUserJob(null);
        }
        if (StringUtils.isNotBlank(user.getUserDepartment())) {
            ExceptionMessageUtils.checkParameter("userDepartment", user.getUserDepartment(), 256, false);
            user.setUserDepartment(StringUtils.trim(user.getUserDepartment()));
        } else {
            user.setUserDepartment(null);
        }
        if (StringUtils.isNotBlank(user.getPhoneNumber())) {
            ExceptionMessageUtils.checkParameter("phoneNumber", user.getPhoneNumber(), 100, false);
            user.setPhoneNumber(StringUtils.trim(user.getPhoneNumber()));
        } else {
            user.setPhoneNumber(null);
        }
        if (StringUtils.isNotBlank(user.getKakaoId())) {
            ExceptionMessageUtils.checkParameter("kakaoId", user.getKakaoId(), 100, false);
            user.setKakaoId(StringUtils.trim(user.getKakaoId()));
        } else {
            user.setKakaoId(null);
        }
        if (StringUtils.isNotBlank(user.getEmail())) {
            ExceptionMessageUtils.checkParameter("email", user.getEmail(), 100, false);
            user.setEmail(StringUtils.trim(user.getEmail()));
            if (!Utils.isValidEmail(user.getEmail())) {
                String errMsg = "A email invalid.";
                throw new CocktailException(errMsg, ExceptionType.InvalidParameter, errMsg);
            }
        } else {
            user.setEmail(null);
        }

    }

    public boolean checkEditUser(UserVO userCurr, UserVO user) throws Exception{
        boolean canEdit = true;
        if (userCurr.getUserSeq() == 1){
            boolean canRootAdminEdit = true;
            if(CollectionUtils.isNotEmpty(user.getRoles())){
                Optional<String> userRoleOptional = user.getRoles().stream().filter(r -> StringUtils.equalsIgnoreCase(UserRole.ADMIN.getCode(), r)).findFirst();
                if(!userRoleOptional.isPresent()){
                    canRootAdminEdit = false;
                }
            }

            if(!canRootAdminEdit){
                throw new CocktailException("Root Admin User do not edit", ExceptionType.UserRootAdminDontAction);
            }
        }

        if (StringUtils.isNotBlank(user.getUserName())) {
            ExceptionMessageUtils.checkParameter("userName", user.getUserName(), 50, true);
            userCurr.setUserName(StringUtils.trim(user.getUserName()));
        }

        if (StringUtils.isNotBlank(user.getDescription())) {
            ExceptionMessageUtils.checkParameter("description", user.getDescription(), 300, false);
            userCurr.setDescription(StringUtils.trim(user.getDescription()));
        } else {
            userCurr.setDescription(null);
        }

        if (user.getUserLanguage() == null) {
            userCurr.setUserLanguage(UserConstants.defaultLanguage());
        } else {
            userCurr.setUserLanguage(user.getUserLanguage());
        }

        // 사용자 timezone 값이 없다면 사용자 언어에 따른 기본 timezone으로 셋팅
        if (StringUtils.isBlank(user.getUserTimezone())) {
            userCurr.setUserTimezone(userCurr.getUserLanguage().getTimezone());
        } else {
            userCurr.setUserTimezone(user.getUserTimezone());
        }
//            if (StringUtils.isNotBlank(user.getUseYn())) {
//                userCurr.setUseYn(user.getUseYn());
//            }

        if (CollectionUtils.isNotEmpty(user.getRoles())) {
            // Service 유형이 Product가 아닌경우 처리
            if (ServiceMode.valueOf(cocktailServiceProperties.getMode()).isAddUser()) {
                this.checkUserRoleParameter(user);
            } else {
                if (UserRole.valueOf(user.getRoles().get(0)) != UserRole.APPUSER) {
                    this.checkUserRoleParameter(user);
                }
            }

            // 권한 변경 체크
            // account, workspace 에 속해 있다면 변경 불가
            if(!StringUtils.equals(user.getRoles().get(0), userCurr.getRoles().get(0))){
                canEdit = this.canEditUser(user);
            }

            userCurr.setRoles(user.getRoles());
        }

        if (StringUtils.isNotBlank(user.getUserJob())) {
            ExceptionMessageUtils.checkParameter("userJob", user.getUserJob(), 100, false);
            userCurr.setUserJob(StringUtils.trim(user.getUserJob()));
        } else {
            userCurr.setUserJob(null);
        }
        if (StringUtils.isNotBlank(user.getUserDepartment())) {
            ExceptionMessageUtils.checkParameter("userDepartment", user.getUserDepartment(), 256, false);
            userCurr.setUserDepartment(StringUtils.trim(user.getUserDepartment()));
        } else {
            userCurr.setUserDepartment(null);
        }
        if (StringUtils.isNotBlank(user.getPhoneNumber())) {
            ExceptionMessageUtils.checkParameter("phoneNumber", user.getPhoneNumber(), 100, false);
            userCurr.setPhoneNumber(StringUtils.trim(user.getPhoneNumber()));
        } else {
            userCurr.setPhoneNumber(null);
        }
        if (StringUtils.isNotBlank(user.getKakaoId())) {
            ExceptionMessageUtils.checkParameter("kakaoId", user.getKakaoId(), 100, false);
            userCurr.setKakaoId(StringUtils.trim(user.getKakaoId()));
        } else {
            userCurr.setKakaoId(null);
        }
        if (StringUtils.isNotBlank(user.getEmail())) {
            ExceptionMessageUtils.checkParameter("email", user.getEmail(), 100, false);
            userCurr.setEmail(StringUtils.trim(user.getEmail()));
            if (!Utils.isValidEmail(user.getEmail())) {
                String errMsg = "Email invalid.";
                throw new CocktailException(errMsg, ExceptionType.InvalidParameter, errMsg);
            }
        } else {
            userCurr.setEmail(null);
        }

        userCurr.setUpdater(ContextHolder.exeContext().getUserSeq());

        return canEdit;
    }


    /**
     * ID/PW로 Azure Active Directory 사용자 인증 하는 메서드.<br/>
     * ID에 해당하는 사용자에 대한 token 정보 조회하는것으로 사용자 인증처리 한다.
     *
     * @since 2019-07-29
     * @author coolingi
     * @param appId
     * @param username
     * @param password
     * @return 사용자 인증여부
     */
    public boolean authenticateUserOfAzureAD(String appId, String username, String password) {
        String AUTHORITY = "https://login.microsoftonline.com/common/v2.0/"; // 인증을 위한 URI
        String resource = "https://graph.microsoft.com/";

        boolean authResult = false;

        AuthenticationContext context = null;
        AuthenticationResult result = null;
        ExecutorService service = Executors.newFixedThreadPool(1);

        try {
            context = new AuthenticationContext(AUTHORITY, false, service);
            Future<AuthenticationResult> future = context.acquireToken(
                    resource,
                    appId,
                    username,
                    password,
                    null);
            result = future.get();
            log.debug(" result : {}", result);

            // 사용자 정보가 있으면 인증 성공으로 설정
            if(result != null && result.getUserInfo() != null){
                log.debug(" user info : TenantId=[{}], displayId=[{}], UID=[{}].", result.getUserInfo().getTenantId(), result.getUserInfo().getDisplayableId(), result.getUserInfo().getUniqueId());
                authResult = true;
            }
        } catch (Exception e) {
            log.error("Authentication of AAD user faild.", e);
            authResult = false;
        } finally {
            service.shutdown();
            if(service.isShutdown()){
                service = null;
            }
        }

        return authResult;
    }

    /**
     * k8s audit log 용 사용자 조회
     * 발급된 사용자 목록
     */
    public List<UserVO> getUsersExistClusterRoleIssue(Integer accountSeq) throws Exception {
        IUserMapper userDao = this.sqlSession.getMapper(IUserMapper.class);
        return userDao.getUsersExistClusterRoleIssue(accountSeq);
    }

    /**
     * 사용자 OTP 정보 조회
     *
     * @param userSeq
     * @return
     * @throws Exception
     */
    public UserOtpVO getUserOtpInfo(Integer userSeq) throws Exception {
        IUserMapper userDao = this.sqlSession.getMapper(IUserMapper.class);
        UserOtpVO userOtp = userDao.getUserOtpInfo(userSeq);

        if (userOtp != null) {
            if (BooleanUtils.toBoolean(userOtp.getOtpUseYn())) {
                userOtp.setOtpQr(CryptoUtils.decryptAES(userOtp.getOtpQr()));
                userOtp.setOtpSecret(CryptoUtils.decryptAES(userOtp.getOtpSecret()));
                userOtp.setOtpUrl(CryptoUtils.decryptAES(userOtp.getOtpUrl()));
            } else {
                userOtp.setOtpQr(null);
                userOtp.setOtpSecret(null);
                userOtp.setOtpUrl(null);
            }
        }

        return userOtp;
    }

    /**
     * 사용자 OTP - QR 정보 조회
     *
     * @param userSeq
     * @return
     * @throws Exception
     */
    public UserOtpVO getUserOtpQr(Integer userSeq) throws Exception {
        if (!userSeq.equals(ContextHolder.exeContext().getUserSeq())) {
            throw new CocktailException("You can only edit yourself", ExceptionType.NotAuthorizedToRequest);
        }

        UserOtpVO userOtp = this.getUserOtpInfo(userSeq);

        if (userOtp != null) {
            userOtp.setOtpSecret(null);
            userOtp.setOtpUrl(null);
        }

        return userOtp;
    }

    /**
     * 사용자 OTP 정보 수정
     *
     * @param userOtp
     * @param updater
     * @throws Exception
     */
    public void updateUserOtpInfo(UserOtpVO userOtp, Integer updater) throws Exception {
        IUserMapper userDao = this.sqlSession.getMapper(IUserMapper.class);
        if (userOtp != null) {
            if (BooleanUtils.toBoolean(userOtp.getOtpUseYn())) {
                userOtp.setOtpQr(CryptoUtils.encryptAES(userOtp.getOtpQr()));
                userOtp.setOtpSecret(CryptoUtils.encryptAES(userOtp.getOtpSecret()));
                userOtp.setOtpUrl(CryptoUtils.encryptAES(userOtp.getOtpUrl()));
            }

            userDao.updateUserOtpInfo(userOtp, updater);
        }
    }

    /**
     * 사용자 비활성여부 수정
     *
     * @param userSeq
     * @param inactiveYn
     * @param updater
     * @throws Exception
     */
    public void updateUserInactiveYn(Integer userSeq, String inactiveYn, Integer updater) throws Exception {
        IUserMapper userDao = this.sqlSession.getMapper(IUserMapper.class);
        userDao.updateUserInactiveYn(userSeq, inactiveYn, updater);
    }
}
