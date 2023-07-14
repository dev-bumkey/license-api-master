package run.acloud.api.internal.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import run.acloud.api.configuration.service.AccountApplicationService;
import run.acloud.api.configuration.service.AccountService;
import run.acloud.api.configuration.vo.AccountApplicationVO;
import run.acloud.api.configuration.vo.AccountVO;
import run.acloud.commons.util.CryptoUtils;
import run.acloud.framework.enums.ExceptionBiz;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "Internal Platform Application", description = "내부호출용 디지털 플랫폼 사용 신청 관련 기능을 제공한다.")
@Slf4j
@RequestMapping(value = "/internal/account/applications")
@RestController
@Validated
@Deprecated
public class InternalAccountApplicationController {

	@Autowired
	private AccountApplicationService accountApplicationService;

	@Autowired
	private AccountService accountService;

	@PostMapping(value = "")
	@Operation(summary = "플랫폼 신청", description = "플랫폼 신청 한다.")
	public AccountApplicationVO addAccountApplications(
		@RequestHeader(name = "isEncrypted", defaultValue="false") boolean isEncrypted,
		@Parameter(name = "accountApplication", description = "Account Application", required = true) @RequestBody AccountApplicationVO accountApplication
	) throws Exception {
		log.debug("[BEGIN] addAccountApplications");

		AccountApplicationVO retAccountApplication;
		try {
			if (accountApplication == null) {
				throw new CocktailException("Invalid parameter", ExceptionType.InvalidParameter_Empty);
			}

			String accountCode = accountApplication.getAccountCode();
			String userEmail = accountApplication.getUserEmail();

			AccountApplicationVO prevAccountApplication = accountApplicationService.getDetailAccountApplicationByUser(accountCode, userEmail);
			if (prevAccountApplication != null || accountApplicationService.checkDuplicateAccountCode(accountCode)) {
				throw new CocktailException("Already Exists.", ExceptionType.AccountApplicationAlreadyExists);
			}

			if(isEncrypted && StringUtils.isNotBlank(accountApplication.getUserPassword())){
				// 패스워드 복호화 후 다시 셋팅
				String decryptedPasswd = CryptoUtils.decryptRSA(accountApplication.getUserPassword());
				accountApplication.setUserPassword(decryptedPasswd);
			}

			// 신청서 정보 등록, 내부적으로 validation 체크 존재
			accountApplicationService.insertAccountApplication(accountApplication);

			// 리턴할 신청 데이터 조회
			retAccountApplication = this.detailAccountApplications(accountCode, userEmail);
			// 패스워드 정보 삭제
			retAccountApplication.setUserPassword(null);
			retAccountApplication.setHashSalt(null);

		} catch (CocktailException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new CocktailException("addAccountApplications Fail.", ex, ExceptionType.CommonCreateFail, ExceptionBiz.ACCOUNT_APPLICATION);
		}

		log.debug("[END  ] addAccountApplications");

		return retAccountApplication;
	}

	@GetMapping(value = "/{accountCode}/{userEmail:.+}")
	@Operation(summary = "플랫폼 신청 상세", description = "플랫폼 신청 목록 조회한다.")
	public AccountApplicationVO detailAccountApplications(
		@Parameter(name = "accountCode", description = "Account Code", required = true) @PathVariable String accountCode,
		@Parameter(name = "userEmail", description = "User Email", required = true) @PathVariable String userEmail
	) throws Exception {
		log.debug("[BEGIN] detailAccountApplications");

		AccountApplicationVO accountApplication = null;
		try {
			accountApplication = accountApplicationService.getDetailAccountApplicationByUser(accountCode, userEmail);
			if (accountApplication == null) {
				throw new CocktailException("Account application not found.", ExceptionType.AccountApplicationNotFound);
			}

			// 패스워드 정보 삭제
			accountApplication.setUserPassword(null);
			accountApplication.setHashSalt(null);

		} catch (CocktailException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new CocktailException("detailAccountApplications Fail.", ex, ExceptionType.CommonInquireFail, ExceptionBiz.ACCOUNT_APPLICATION);
		}

		log.debug("[END  ] detailAccountApplications");

		return accountApplication;
	}

	@GetMapping(value = "/{accountCode}/check")
	@Operation(summary = "플랫폼 ID 중복 체크", description = "플랫폼 ID를 중복 체크한다.")
	public Map<String, Object> checkDuplicateAccountCodeApplication(
		@Parameter(name = "accountCode", description = "Account Code", required = true) @PathVariable String accountCode
	) throws Exception {
		log.debug("[BEGIN] checkDuplicateAccountApplications");

		Map<String, Object> resultMap = new HashMap<>();
		resultMap.put("isValid", Boolean.TRUE);
		try {
			// 신청현황 체크
			if(accountApplicationService.checkDuplicateAccountCode(accountCode)) {
				resultMap.put("isValid", Boolean.FALSE);
			}
			// 플렛폼 체크
			else {
				AccountVO account = new AccountVO();
				account.setAccountCode(accountCode);
				if (accountService.checkDuplicateAccount(account)) {
					resultMap.put("isValid", Boolean.FALSE);
				}
			}
		} catch (CocktailException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new CocktailException("checkDuplicateAccountCodeApplication Fail.", ex, ExceptionType.CommonInquireFail, ExceptionBiz.ACCOUNT_APPLICATION);
		}

		log.debug("[END  ] checkDuplicateAccountApplications");

		return resultMap;
	}

}
