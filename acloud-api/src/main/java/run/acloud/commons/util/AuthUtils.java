package run.acloud.commons.util;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import run.acloud.api.auth.enums.UserGrant;
import run.acloud.api.auth.enums.UserRole;
import run.acloud.api.auth.vo.UserVO;
import run.acloud.api.configuration.service.UserService;
import run.acloud.api.cserver.vo.ServiceRelationVO;
import run.acloud.commons.vo.ExecutingContextVO;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;

import java.util.EnumSet;
import java.util.Optional;
import java.util.function.Predicate;

@Slf4j
@Component
public final class AuthUtils {

	private static UserService userService;

	@Autowired
	private UserService injectedUserService;

	private static ResourcesAccessibleChecker resourcesAccessableChecker;

	@Autowired
	private ResourcesAccessibleChecker injectedChecker;

	@PostConstruct
	public void init() {
		AuthUtils.userService = injectedUserService;
		AuthUtils.resourcesAccessableChecker = injectedChecker;
	}

	public static void checkAuth(ExecutingContextVO ctx) throws Exception{
		AuthUtils.checkAuth(ctx, null);
	}

	public static void checkAuth(ExecutingContextVO ctx, Predicate<ExecutingContextVO> p) throws Exception{
		boolean isValidReqUser = true;
		UserVO user = userService.getByUserSeq(ctx.getUserSeq());
		if(user == null){
			isValidReqUser = false;
		}else{
			if(!BooleanUtils.toBoolean(user.getUseYn()) || BooleanUtils.toBoolean(user.getInactiveYn())){
				isValidReqUser = false;
			}else{
				if(CollectionUtils.isNotEmpty(user.getRoles())){
					if(user.getRoles().contains(ctx.getUserRole())){
						if(p != null){
							isValidReqUser = p.test(ctx);
						}
						// Set User accountSeq, Account info
						if ( UserRole.valueOf(user.getUserRole()).isAccountRole() ) {
							if (user.getAccount() != null) {
								ctx.setUserTimezone(user.getUserTimezone());
								ctx.setUserAccountSeq(user.getAccount().getAccountSeq());
								ctx.setUserAccount(user.getAccount());

								// 워크스페이스 권한 처리
								if (UserRole.valueOf(ctx.getUserRole()).isDevops()) {
									if (CollectionUtils.isNotEmpty(user.getUserRelations())) {
										ctx.setUserRelations(user.getUserRelations());

										Optional<ServiceRelationVO> serviceRelationOptional = user.getUserRelations().stream().filter(r -> (r.getServiceSeq().equals(ctx.getUserServiceSeq()))).findFirst();
										if (serviceRelationOptional.isPresent()) {
											if (!StringUtils.equals(ctx.getUserGrant(), serviceRelationOptional.get().getUserGrant().getCode())) {
//												isValidReqUser = false;
											}
										} else {
//											isValidReqUser = false;
										}
									}
								}
							}else {
								isValidReqUser = false;
							}
						}
					}else{
						isValidReqUser = false;
					}
				}else{
					isValidReqUser = false;
				}
			}
		}

		AuthUtils.isValid(isValidReqUser);
	}

	public static void isValid(boolean isValid) throws Exception{
		if(!isValid){
			throw new CocktailException("Not authorized to request!!", ExceptionType.NotAuthorizedToRequest);
		}
	}

	public static void isValid(ExecutingContextVO ctx, Predicate<ExecutingContextVO> p) throws Exception{
		AuthUtils.isValid(p.test(ctx));
	}

	public static ExecutingContextVO checkUserAdminAuth(ExecutingContextVO ctx) throws Exception{

		AuthUtils.checkAuth(ctx, AuthUtils.checkUserAdminAuthPredicate());

		return ctx;
	}

	public static Predicate<ExecutingContextVO> checkUserAdminAuthPredicate() throws Exception{

		Predicate<ExecutingContextVO> checkUserAuth = p -> {
			// ADMIN 만 가능
			UserRole reqUserRole = UserRole.valueOf(p.getUserRole());

			if(!reqUserRole.isAdmin()){
				return false;
			}

			return true;
		};

		return checkUserAuth;
	}

	public static Predicate<ExecutingContextVO> checkUserDevOpsBlockAuthPredicate() throws Exception{

		Predicate<ExecutingContextVO> checkUserAuth = p -> {
			// DEVOPS 만 불가능
			UserRole reqUserRole = UserRole.valueOf(p.getUserRole());

			if(reqUserRole.isDevops()){
				return false;
			}

			return true;
		};

		return checkUserAuth;
	}

	public static Predicate<ExecutingContextVO> checkUserDevOpsExcludeManagerBlockAuthPredicate(Integer serviceSeq) throws Exception{

		Predicate<ExecutingContextVO> checkUserAuth = p -> {
			// DEVOPS 권한 사용자 중 MANAGER가 아닌 사용자 불가능
			UserRole reqUserRole = UserRole.valueOf(p.getUserRole());

			if(reqUserRole.isDevops()){
				if (CollectionUtils.isNotEmpty(p.getUserRelations())) {
					Optional<ServiceRelationVO> serviceRelationOptional = p.getUserRelations().stream().filter(r -> (r.getServiceSeq().equals(serviceSeq))).findFirst();
					if (serviceRelationOptional.isPresent()) {
						if (serviceRelationOptional.get().getUserGrant() == UserGrant.MANAGER) {
							return true;
						}
					}

					return false;
				}
			}

			return true;
		};

		return checkUserAuth;
	}

	public static Predicate<ExecutingContextVO> checkUserSysUserNDevOpsBlockAuthPredicate() throws Exception{

		Predicate<ExecutingContextVO> checkUserAuth = p -> {
			// SYSUSER, SYSDEMO, DEVOPS 만 불가능
			UserRole reqUserRole = UserRole.valueOf(p.getUserRole());

			if(reqUserRole.isDevopsNSysuser()){
				return false;
			}

			return true;
		};

		return checkUserAuth;
	}

	public static Predicate<ExecutingContextVO> checkUserSystemAuthPredicate() throws Exception{

		Predicate<ExecutingContextVO> checkUserAuth = p -> {
			// SYSTEM 만 가능
			UserRole reqUserRole = UserRole.valueOf(p.getUserRole());

			if(reqUserRole.isSystem()){
				return false;
			}

			return true;
		};

		return checkUserAuth;
	}

	public static Predicate<ExecutingContextVO> checkUserSystemNSysuserAuthPredicate() throws Exception{

		Predicate<ExecutingContextVO> checkUserAuth = p -> {
			// SYSTEM, SYSUSER 만 가능
			UserRole reqUserRole = UserRole.valueOf(p.getUserRole());

			if(!reqUserRole.isUserOfSystem()){
				return false;
			}

			return true;
		};

		return checkUserAuth;
	}

	public static Predicate<ExecutingContextVO> checkUserRoleAuthPredicate(EnumSet<UserRole> userRoles) throws Exception{

		Predicate<ExecutingContextVO> checkUserAuth = p -> {

			UserRole reqUserRole = UserRole.valueOf(p.getUserRole());

			if (userRoles != null) {
				if (!userRoles.contains(reqUserRole)) {
					return false;
				}
			} else {
				return false;
			}

			return true;
		};

		return checkUserAuth;
	}


	/**
	 * ADMIN 사용자인지 판단. (ExecutingContextVO는 AuthHandlerInterceptor.java에서 Set)
	 *
	 * 향후 ADMIN 판단 기준이 어떻게 바뀔지 모르므로
	 * Interceptor에서 공통 처리하여 기준 데이터를 Request에 설정하면 해당 값을 이용하여 판단하도록 함.
	 * @param request
	 * @return
	 */
	public static boolean isAdminUser(HttpServletRequest request) throws Exception {
		ExecutingContextVO ctx = (ExecutingContextVO)request.getAttribute("ctx");

		return AuthUtils.isAdminUser(ctx);

	}

	/**
	 * ADMIN 사용자인지 판단. (ExecutingContextVO는 AuthHandlerInterceptor.java에서 Set)
	 * @param ctx
	 * @return
	 */
	public static boolean isAdminUser(ExecutingContextVO ctx) throws Exception {
		return AuthUtils.checkUserAdminAuthPredicate().test(ctx);
	}

	/**
	 * SYSTEM 사용자인지 판단. (ExecutingContextVO는 AuthHandlerInterceptor.java에서 Set)
	 * @param ctx
	 * @return
	 */
	public static boolean isSystemUser(ExecutingContextVO ctx) throws Exception {
		return AuthUtils.checkUserSystemAuthPredicate().test(ctx);
	}

	/**
	 * SYSUSER, SYSTEM 사용자인지 판단. (ExecutingContextVO는 AuthHandlerInterceptor.java에서 Set)
	 * @param ctx
	 * @return
	 */
	public static boolean isSystemNSysadminUser(ExecutingContextVO ctx) throws Exception {
		return AuthUtils.checkUserSystemNSysuserAuthPredicate().test(ctx);
	}

	public static boolean isNotDevOpsUser(ExecutingContextVO ctx) throws Exception {
		return AuthUtils.checkUserDevOpsBlockAuthPredicate().test(ctx);
	}

	public static boolean isNotSysuserNDevOpsUser(ExecutingContextVO ctx) throws Exception {
		return AuthUtils.checkUserSysUserNDevOpsBlockAuthPredicate().test(ctx);
	}

	public static ExecutingContextVO getContext(HttpServletRequest request) {
		return (ExecutingContextVO)request.getAttribute("ctx");
	}

	/**
	 * 요청 Resource에 대한 접근 권한 확인
	 * @param request
	 * @throws Exception
	 */
	public static void isResourceAccessible(HttpServletRequest request) throws Exception {
		AuthUtils.isResourceAccessible(request, null, null);
	}

	/**
	 * 요청 Resource에 대한 접근 권한 확인
	 * @param request
	 * @param response
	 * @param handler
	 * @throws Exception
	 */
	public static void isResourceAccessible(HttpServletRequest request, HttpServletResponse response, Object handler) throws CocktailException {
		resourcesAccessableChecker.checkPermission(request, response, handler);
	}

	/**
	 * 칵테일 관리자 체크
	 *
	 * @return
	 * @throws Exception
	 */
	public static Predicate<UserVO> checkUserAdminRolePredicate() {

		Predicate<UserVO> checkUserAuth = p -> {
			// ADMIN 만 가능
			UserRole reqUserRole = UserRole.valueOf(p.getUserRole());

			if(!reqUserRole.isAdmin()){
				return false;
			}

			return true;
		};

		return checkUserAuth;
	}

	/**
	 * 플랫폼 관리자 체크
	 *
	 * @return
	 * @throws Exception
	 */
	public static Predicate<UserVO> checkUserSystemRolePredicate() {

		Predicate<UserVO> checkUserAuth = p -> {
			// ADMIN 만 가능
			UserRole reqUserRole = UserRole.valueOf(p.getUserRole());

			if(!reqUserRole.isSystem()){
				return false;
			}

			return true;
		};

		return checkUserAuth;
	}

	/**
	 * 플랫폼 사용자 체크
	 *
	 * @return
	 * @throws Exception
	 */
	public static Predicate<UserVO> checkUserDevopsRolePredicate() {

		Predicate<UserVO> checkUserAuth = p -> {
			// ADMIN 만 가능
			UserRole reqUserRole = UserRole.valueOf(p.getUserRole());

			if(!reqUserRole.isDevops()){
				return false;
			}

			return true;
		};

		return checkUserAuth;
	}
}
