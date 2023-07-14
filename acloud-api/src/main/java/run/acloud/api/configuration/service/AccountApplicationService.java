package run.acloud.api.configuration.service;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import run.acloud.api.auth.dao.IUserMapper;
import run.acloud.api.auth.vo.AuthVO;
import run.acloud.api.auth.vo.UserVO;
import run.acloud.api.configuration.constants.AccountApplicationConstants;
import run.acloud.api.configuration.constants.UserConstants;
import run.acloud.api.configuration.dao.IAccountApplicationMapper;
import run.acloud.api.configuration.dao.IAccountMapper;
import run.acloud.api.configuration.dao.IClusterMapper;
import run.acloud.api.configuration.vo.*;
import run.acloud.commons.security.password.MessageDigestPasswordEncoder;
import run.acloud.commons.security.password.ShaPasswordEncoder;
import run.acloud.commons.service.EmailService;
import run.acloud.commons.util.CryptoUtils;
import run.acloud.commons.util.PagingUtils;
import run.acloud.commons.util.Utils;
import run.acloud.commons.vo.ListCountVO;
import run.acloud.commons.vo.PagingVO;
import run.acloud.framework.context.ContextHolder;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;
import run.acloud.framework.properties.CocktailOnlineDSProperties;
import run.acloud.framework.properties.CocktailServiceProperties;
import run.acloud.framework.util.ExceptionMessageUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@Deprecated
public class AccountApplicationService {

	@Resource(name = "cocktailSession")
	private SqlSessionTemplate sqlSession;

	@Autowired
	private EmailService emailService;

	@Autowired
	private CocktailOnlineDSProperties cocktailOnlineDSProperties;

	@Autowired
	private CocktailServiceProperties cocktailServiceProperties;

	private final MessageDigestPasswordEncoder passwordEncoder = new ShaPasswordEncoder(256);

	@Transactional(transactionManager = "transactionManager")
	public int insertAccountApplication(AccountApplicationVO accountApplication) throws Exception {
		IAccountApplicationMapper mapper = sqlSession.getMapper(IAccountApplicationMapper.class);
		if(accountApplication == null) {
			throw new CocktailException("Invalid parameter", ExceptionType.InvalidParameter);
		}

		/** 값 validation **/
		// 필수값 체크
		this.checkAccountApplication(accountApplication);

		// 패스워드 체크
		if (!Utils.isValidPasswordWithEmail(accountApplication.getUserEmail(), accountApplication.getUserPassword())){
			throw new CocktailException("Violation of password policy.", ExceptionType.UserPasswordInvalid);
		}

		// salt 값 생성 및 패스워드 생성
		String hashSalt = CryptoUtils.generateSalt();
		accountApplication.setHashSalt(hashSalt);
		accountApplication.setUserPassword(passwordEncoder.encodePassword(accountApplication.getUserPassword(), hashSalt));

		int result = mapper.insertAccountApplication(accountApplication);

		if (result > 0) {
			// 신청정보 메일 전송
			AccountApplicationVO detail = mapper.getDetailByAdmin(accountApplication.getAccountApplicationSeq(), cocktailServiceProperties.getRegionTimeZone());
			this.sendAccountApplicationMail(
					detail
					, String.format("Cocktail Cloud 신청 정보(%s)", detail.getAccountCode())
					, AccountApplicationConstants.DSONLINE_APPLICATION_MAILFORM_FILENAME
			);
		}

		return result;
	}

	@Transactional(transactionManager = "transactionManager")
	public int updateAccountApplicationStatus(Integer accountApplicationSeq, String status, Integer updater) throws Exception {
		IAccountApplicationMapper mapper = sqlSession.getMapper(IAccountApplicationMapper.class);

		if(accountApplicationSeq == null || status == null) {
			throw new CocktailException("Invalid parameter", ExceptionType.InvalidParameter);
		}

		if(!this.isValidStatus(status)) {
			throw new CocktailException("Invalid parameter", ExceptionType.InvalidParameter);
		}

		// 구축완료 상태 변경일 때는 정상적으로 구축 완료 되었는지 체크
		if (StringUtils.equalsIgnoreCase(status, "C")){
			this.validateConstructionComplete(accountApplicationSeq);
		}

		// 신청서 상태 변경
		int result = mapper.updateAccountApplicationStatus(accountApplicationSeq, status, updater);

		// 사용자 password 변경 및 메일 발송
		if (result > 0 && StringUtils.equalsIgnoreCase(status, "C")) {
			// 사용자 패스워드를 신청서의 패스워드로 변경
			this.updateUserPasswordByApplication(accountApplicationSeq);
			// 구축 완료 메일 발송
			this.sendConstructionCompletedMail(accountApplicationSeq);
		}

		return result;
	}

	private void checkAccountApplication(AccountApplicationVO accountApplication) throws CocktailException {
		ExceptionMessageUtils.checkParameter("prdType", accountApplication.getPrdType(), 50, true);
		ExceptionMessageUtils.checkParameter("accountCode", accountApplication.getAccountCode(), 50, true);
		ExceptionMessageUtils.checkParameter("userEmail", accountApplication.getUserEmail(), 100, true);
		ExceptionMessageUtils.checkParameter("userPassword", accountApplication.getUserPassword(), 24, true);
		ExceptionMessageUtils.checkParameter("userName", accountApplication.getUserName(), 50, true);
		ExceptionMessageUtils.checkParameter("customerName", accountApplication.getCustomerName(), 50, true);
		ExceptionMessageUtils.checkParameter("customerAddress", accountApplication.getCustomerAddress(), 300, true);
		ExceptionMessageUtils.checkParameter("agreePersonalInfoYn", accountApplication.getAgreePersonalInfoYn(), 1, true);
		ExceptionMessageUtils.checkParameter("agreeMarketingYn", accountApplication.getAgreeMarketingYn(), 1, true);
		ExceptionMessageUtils.checkParameter("status", accountApplication.getStatus(), 1, false);
	}

	private boolean isValidStatus(String status) {
		if("A".equalsIgnoreCase(status) || "R".equalsIgnoreCase(status) || "C".equalsIgnoreCase(status)) {
			return true;
		}
		return false;
	}

	@Transactional(transactionManager = "transactionManager")
	public Integer deleteAccountApplication(Integer accountApplicationSeq) throws CocktailException {
		IAccountApplicationMapper mapper = sqlSession.getMapper(IAccountApplicationMapper.class);
		int result = mapper.deleteAccountApplication(accountApplicationSeq);
		return result;
	}

	public UserVO login(AuthVO auth){
		UserVO user = null;

		// auth.getAccountId()와 auth.getUsername() 가 존재 할때만 login 처리함. admin 일때는 AccountId 없음.
		if (StringUtils.isNotEmpty(auth.getAccountId()) && StringUtils.isNotEmpty(auth.getUsername())) {
			AccountApplicationVO applicationVO = this.getDetailAccountApplicationByUser(auth.getAccountId(), auth.getUsername());

			// 완료 안된 신청서 존재하고, 사용자 ID, PW 동일하면 로그인 완료 처리.
			if ( applicationVO != null && !"C".equalsIgnoreCase(applicationVO.getStatus()) ) {

				String encodedPasswd = passwordEncoder.encodePassword(auth.getPassword(), applicationVO.getHashSalt());

				// 사용자 아이디 패스워드가 동일할 경우
				if ( StringUtils.equals(auth.getUsername(), applicationVO.getUserEmail())
						&& StringUtils.equals(encodedPasswd, applicationVO.getUserPassword())
				){
					user = new UserVO();
					user.setUserId(auth.getUsername());
					user.setApplicationLogin(true);
					user.setApplicationStatus(applicationVO.getStatus());
				}

			}
		}
		return user;
	}

	public AccountApplicationListVO getAccountApplications(String order, String orderColumn, Integer nextPage, Integer itemPerPage, String maxId) throws CocktailException {
		IAccountApplicationMapper mapper = sqlSession.getMapper(IAccountApplicationMapper.class);
		AccountApplicationListVO list = new AccountApplicationListVO();
		try {
			AccountApplicationSearchVO params = new AccountApplicationSearchVO();
			ListCountVO listCount = mapper.getAccountApplicationCountAndMaxId(params);
			PagingVO paging = PagingUtils.setPagingParams(orderColumn, order, nextPage, itemPerPage, maxId, listCount);
			params.setPaging(paging);

			List<AccountApplicationVO> accountApplications = mapper.getAccountApplications(params);

			list.setItems(accountApplications);
			list.setTotalCount(params.getPaging().getListCount().getCnt());
			list.setMaxId(params.getPaging().getMaxId());
			list.setCurrentPage(nextPage);
		}
		catch (CocktailException ce) {
			throw ce;
		}
		catch (Exception ex) {
			throw new CocktailException("Account Application List Inquire Failure", ex, ExceptionType.CommonInquireFail);
		}

		return list;
	}

	public AccountApplicationVO getDetailAccountApplicationByUser(String accountCode, String userEmail) throws CocktailException {
		IAccountApplicationMapper mapper = sqlSession.getMapper(IAccountApplicationMapper.class);

		ExceptionMessageUtils.checkParameter("accountCode", accountCode, 50, true);
		ExceptionMessageUtils.checkParameter("userEmail", userEmail, 100, true);

		return mapper.getDetailByUser(accountCode, userEmail, null);
	}

	public AccountApplicationVO getDetailAccountApplicationByAdmin(Integer accountApplicationSeq) throws CocktailException {
		IAccountApplicationMapper mapper = sqlSession.getMapper(IAccountApplicationMapper.class);

		if(accountApplicationSeq == null) {
			throw new CocktailException("Invalid parameter", ExceptionType.InvalidParameter);
		}

		return mapper.getDetailByAdmin(accountApplicationSeq, null);
	}

	public boolean checkDuplicateAccountCode(String accountCode) throws CocktailException {
		IAccountApplicationMapper mapper = sqlSession.getMapper(IAccountApplicationMapper.class);
		return mapper.getAccountCodeCount(accountCode) > 0;
	}

	public void sendAccountApplicationMail(AccountApplicationVO accountApplication, String title, String mailFormFileName) throws Exception {
		if (accountApplication != null) {
			// 제외할 필드
			List<String> ignoreFields = Lists.newArrayList();
			ignoreFields.add("accountApplicationSeq");
			ignoreFields.add("updater");

			// create mail form
			String mailForm = emailService.genMailFormWithFile(
					String.format("%s/%s", StringUtils.removeEnd(cocktailOnlineDSProperties.getMailFormPath(), "/"), mailFormFileName)
					, accountApplication
					, ignoreFields
			);

			// send mail
			emailService.sendMail(
					Arrays.asList(cocktailOnlineDSProperties.getSystemEmail(), cocktailOnlineDSProperties.getSalesEmail())
					, title
					, mailForm
					, false
			);
		}
	}

	@Transactional(transactionManager = "transactionManager")
	public void validateConstructionComplete(Integer accountApplicationSeq) throws Exception {
		IAccountApplicationMapper applicationMapper = sqlSession.getMapper(IAccountApplicationMapper.class);
		IAccountMapper accountMapper = sqlSession.getMapper(IAccountMapper.class);
		IClusterMapper clusterMapper = sqlSession.getMapper(IClusterMapper.class);
		IUserMapper userMapper = this.sqlSession.getMapper(IUserMapper.class);

		AccountApplicationVO accountApplication = applicationMapper.getDetailByAdmin(accountApplicationSeq, cocktailServiceProperties.getRegionTimeZone());

		/**
		 * 완료시 고객에게 구축완료 메일 전송
		 * - 사용자 비밀번호 초기화 처리
		 */
		if (accountApplication != null) {
			String errMsg = "Unable to complete platform application.";

			/**
			 * 구축완료 메일 전송전 확인 사항
			 * - 플렛폼
			 * - 클러스터
			 * - 사용자
			 */
			// 플랫폼 생성확인
			AccountVO account = accountMapper.getAccountSimpleByCode(accountApplication.getAccountCode(), "Y");
			if (account == null) {
				throw new CocktailException(errMsg, ExceptionType.ResourceNotFound, String.format("Platform '%s' does not exist.", accountApplication.getAccountCode()));
			}

			// 클러스터 생성확인
			List<ClusterVO> clusters = clusterMapper.getClusters(account.getAccountSeq(), null, null, null, null, "Y");
			if (CollectionUtils.isEmpty(clusters)) {
				throw new CocktailException(errMsg, ExceptionType.ResourceNotFound, String.format("Cluster on platform '%s' does not exist.", accountApplication.getAccountCode()));
			}

			// 사용자 생성확인
			List<UserVO> users = userMapper.getUsersOfAccount(account.getAccountSeq());
			if (CollectionUtils.isEmpty(users)) {
				throw new CocktailException(errMsg, ExceptionType.ResourceNotFound, String.format("User on platform '%s' does not exist.", accountApplication.getAccountCode()));
			}

			// 신청서 email의 사용자가 존재하는지 확인
			Optional<UserVO> optUser = users.stream().filter(vo -> StringUtils.equals(vo.getUserId(), accountApplication.getUserEmail()) && vo.getRoles().contains("SYSTEM")).findFirst();
			if (!optUser.isPresent()){
				throw new CocktailException(errMsg, ExceptionType.ResourceNotFound, String.format("User on platform '%s' does not exist.", accountApplication.getAccountCode()));
			}

		}
	}

	/**
	 * 신청서 정보에 해당하는 플랫폼사용자(관리자)의 password를 신청서에서 사용하던 패스워드로 update 한다.
	 *
	 * @param accountApplicationSeq
	 * @throws Exception
	 */
	@Transactional(transactionManager = "transactionManager")
	public void updateUserPasswordByApplication(Integer accountApplicationSeq) throws Exception {
		IAccountApplicationMapper applicationMapper = sqlSession.getMapper(IAccountApplicationMapper.class);
		IAccountMapper accountMapper = sqlSession.getMapper(IAccountMapper.class);
		IUserMapper userMapper = this.sqlSession.getMapper(IUserMapper.class);

		AccountApplicationVO accountApplication = applicationMapper.getDetailByAdmin(accountApplicationSeq, cocktailServiceProperties.getRegionTimeZone());

		if ( accountApplication != null) {

			// 플랫폼 조회
			AccountVO account = accountMapper.getAccountSimpleByCode(accountApplication.getAccountCode(), "Y");

			// 사용자 조회
			List<UserVO> users = userMapper.getUsersOfAccount(account.getAccountSeq());
			Optional<UserVO> optUser = users.stream().filter(vo -> StringUtils.equals(vo.getUserId(), accountApplication.getUserEmail()) && vo.getRoles().contains("SYSTEM")).findFirst();
			if (optUser.isPresent()){
				UserVO user = optUser.get();

				// 사용자 비밀번호 신청서 사용자의 비밀번호로 변경, salt & password 모두 update
				user.setHashSalt(accountApplication.getHashSalt());
				user.setPassword(accountApplication.getUserPassword());
				user.setPasswordInterval(UserConstants.INTERVAL_CHANGE_PASSWORD); // password 변경 후 90일로 만료기한 변경
				user.setUpdater(ContextHolder.exeContext().getUserSeq());
				userMapper.changePassword(user);
			}
		}
	}

	@Transactional(transactionManager = "transactionManager")
	public void sendConstructionCompletedMail(Integer accountApplicationSeq) throws Exception {
		IAccountApplicationMapper mapper = sqlSession.getMapper(IAccountApplicationMapper.class);

		AccountApplicationVO accountApplication = mapper.getDetailByAdmin(accountApplicationSeq, cocktailServiceProperties.getRegionTimeZone());

		/**
		 * 고객에게 구축완료 메일 전송, 신청서의 상태가 완료 상태인 경우만 메일 발송
		 */
		if (accountApplication != null && "C".equalsIgnoreCase(accountApplication.getStatus())) {

			// 구축 완료 메일 전송 -> 고객
			String mailForm = emailService.getMailFormFile(String.format("%s/%s", StringUtils.removeEnd(cocktailOnlineDSProperties.getMailFormPath(), "/"), AccountApplicationConstants.DSONLINE_CONSTRUCTION_COMPLETED_MAILFORM_FILENAME));
			Map<String, String> mailValueMap = Maps.newHashMap();
			mailValueMap.put("cocktailCloudPlatformAdminURL", cocktailOnlineDSProperties.getPlatformAdminUrl());
			mailValueMap.put("cocktailCloudPlatformUserURL", cocktailOnlineDSProperties.getPlatformUserUrl());
			mailValueMap.put("accountCode", accountApplication.getAccountCode());
			mailValueMap.put("platformAdminUserName", accountApplication.getUserEmail());
			mailValueMap.put("platformAdminUserInitPassword", "서비스 신청시의 패스워드");

			for (Map.Entry<String, String> entry : mailValueMap.entrySet()) {
				mailForm = emailService.replaceMailForm(mailForm, entry.getKey(), entry.getValue());
			}

			emailService.sendMail(
					accountApplication.getUserEmail()
					, "Cocktail Cloud 구축 완료 안내"
					, mailForm
					, false
			);

			// 구축 완료 메일 전송 -> 시스템, 영업
			this.sendAccountApplicationMail(
					accountApplication
					, String.format("Cocktail Cloud 구축 완료(%s)", accountApplication.getAccountCode())
					, AccountApplicationConstants.DSONLINE_APPLICATION_COMPLETED_MAILFORM_FILENAME
			);

		}
	}
}
