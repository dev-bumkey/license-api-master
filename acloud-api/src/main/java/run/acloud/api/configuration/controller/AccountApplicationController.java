package run.acloud.api.configuration.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import run.acloud.api.configuration.service.AccountApplicationService;
import run.acloud.api.configuration.service.AccountService;
import run.acloud.api.configuration.vo.AccountApplicationListVO;
import run.acloud.api.configuration.vo.AccountApplicationStatusVO;
import run.acloud.api.configuration.vo.AccountApplicationVO;
import run.acloud.api.configuration.vo.AccountVO;
import run.acloud.commons.util.AuthUtils;
import run.acloud.commons.util.PagingUtils;
import run.acloud.framework.context.ContextHolder;
import run.acloud.framework.enums.ExceptionBiz;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;

@Tag(name = "Platform Application", description = "디지털 플랫폼 사용 신청에 대한 관리 기능을 제공한다.")
@Slf4j
@RequestMapping(value = "/api/account/applications")
@RestController
@Validated
@Deprecated
public class AccountApplicationController {

	@Autowired
	private AccountService accountService;

	@Autowired
	private AccountApplicationService accountApplicationService;

	@GetMapping(value = "")
	@Operation(summary = "플랫폼 신청 목록", description = "플랫폼 신청 목록 조회한다.")
	public AccountApplicationListVO listAccountApplications(
		@Parameter(name = "order", description = "정렬 순서", schema = @Schema(allowableValues = {"DESC","ASC"}, defaultValue = "DESC"), required = false) @RequestParam(name = "order", defaultValue = "DESC", required = true) String order,
		@Parameter(name = "orderColumn", description = "정렬기준컬럼", schema = @Schema(allowableValues = {"created","updated"}, defaultValue = "created"), required = false) @RequestParam(name = "orderColumn", defaultValue = "created", required = false) String orderColumn,
		@Parameter(name = "nextPage", description = "요청페이지", schema = @Schema(defaultValue = "1"), required = false) @RequestParam(name = "nextPage", defaultValue = "1", required = false) Integer nextPage,
		@Parameter(name = "itemPerPage", description = "페이지당 표시 갯수", schema = @Schema(defaultValue = "999999"), required = false) @RequestParam(name = "itemPerPage", defaultValue = "999999", required = false) Integer itemPerPage,
		@Parameter(name = "maxId", description = "더보기 중복 방지를 위한 데이터셋 위치 지정") @RequestParam(name = "maxId", required = false) String maxId
	) throws Exception {
		log.debug("[BEGIN] listAccountApplications");

		/**
		 * header 정보로 요청 사용자 권한 체크
		 */
		AuthUtils.checkUserAdminAuth(ContextHolder.exeContext());

		AccountApplicationListVO list;

		try {
			PagingUtils.validatePagingParams(nextPage, itemPerPage);
			PagingUtils.validatePagingParamsOrderColumn(orderColumn, "created", "updated");
			PagingUtils.validatePagingParamsOrder(order, "ASC", "DESC");

			list = accountApplicationService.getAccountApplications(order, orderColumn, nextPage, itemPerPage, maxId);
		} catch (CocktailException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new CocktailException("listAccountApplications Fail.", ex, ExceptionType.CommonInquireFail, ExceptionBiz.ACCOUNT_APPLICATION);
		}

		log.debug("[END  ] listAccountApplications");

		return list;
	}

	@PutMapping(value = "/{accountApplicationSeq}/status")
	@Operation(summary = "플랫폼 신청 상태 수정", description = "플랫폼 신청 상태를 수정 한다.")
	public AccountApplicationVO updateAccountApplicationStatus(
		@Parameter(name = "accountApplicationSeq", description = "Account Application Sequence", required = true) @PathVariable Integer accountApplicationSeq,
		@Parameter(name = "status", description = "Status", required = true) @RequestBody AccountApplicationStatusVO status
	) throws Exception {
		log.debug("[BEGIN] updateAccountApplicationStatus");

		/**
		 * header 정보로 요청 사용자 권한 체크
		 */
		AuthUtils.checkUserAdminAuth(ContextHolder.exeContext());

		AccountApplicationVO retAccountApplication = null;
		try {
			AccountApplicationVO prevAccountApplication = accountApplicationService.getDetailAccountApplicationByAdmin(accountApplicationSeq);
			if (prevAccountApplication == null) {
				throw new CocktailException("Invalid parameter", ExceptionType.AccountApplicationNotFound);
			}

			if(status != null) {
				if (!StringUtils.equalsAny(status.getStatus(), "A", "R", "C")) {
					throw new CocktailException("status Parameter is invalid.", ExceptionType.InvalidParameter);
				} else {
					int result = accountApplicationService.updateAccountApplicationStatus(accountApplicationSeq, status.getStatus(), ContextHolder.exeContext().getUserSeq());
				}
			}

			retAccountApplication = accountApplicationService.getDetailAccountApplicationByAdmin(accountApplicationSeq);
			retAccountApplication.setUserPassword(null);
			retAccountApplication.setHashSalt(null);

		} catch (CocktailException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new CocktailException("updateAccountApplications Fail.", ex, ExceptionType.CommonUpdateFail, ExceptionBiz.ACCOUNT_APPLICATION);
		}

		log.debug("[END  ] updateAccountApplicationStatus");

		return retAccountApplication;
	}

	@DeleteMapping(value = "/{accountApplicationSeq}")
	@Operation(summary = "플랫폼 신청 내역 삭제", description = "플랫폼 신청 내역을 삭제 한다.")
	public AccountApplicationVO deleteAccountApplications(
		@Parameter(name = "accountApplicationSeq", description = "Account Application Sequence", required = true) @PathVariable Integer accountApplicationSeq
	) throws Exception {
		log.debug("[BEGIN] deleteAccountApplications");

		/**
		 * header 정보로 요청 사용자 권한 체크
		 */
		AuthUtils.checkUserAdminAuth(ContextHolder.exeContext());

		AccountApplicationVO result = null;

		try {
			AccountApplicationVO prevAccountApplication = accountApplicationService.getDetailAccountApplicationByAdmin(accountApplicationSeq);
			if (prevAccountApplication == null) {
				throw new CocktailException("Invalid parameter", ExceptionType.AccountApplicationNotFound);
			}

			String accountCode = prevAccountApplication.getAccountCode();
			String status = prevAccountApplication.getStatus();

			AccountVO account = accountService.getAccountSimpleByCode(accountCode, "Y");
			if("A".equalsIgnoreCase(status) || ("R".equalsIgnoreCase(status) && account == null)) {
				Integer deletedCount = accountApplicationService.deleteAccountApplication(accountApplicationSeq);
				if (deletedCount != null && deletedCount > 0) {
					result = prevAccountApplication;
					result.setUserPassword(null);
					result.setHashSalt(null);
				}
			} else {
				throw new CocktailException("platform's registered cannot be deleted.", ExceptionType.AccountApplicationCannotDeleteRegistAccount);
			}
		} catch (CocktailException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new CocktailException("deleteAccountApplications Fail.", ex, ExceptionType.CommonDeleteFail, ExceptionBiz.ACCOUNT_APPLICATION);
		}

		log.debug("[END  ] deleteAccountApplications");

		return result;
	}

	@GetMapping(value = "/{accountApplicationSeq}")
	@Operation(summary = "플랫폼 신청 상세", description = "플랫폼 신청 상세 내용을 조회한다.")
	public AccountApplicationVO detailAccountApplications(
		@Parameter(name = "accountApplicationSeq", description = "Account Application Sequence", required = true) @PathVariable Integer accountApplicationSeq
	) throws Exception {
		log.debug("[BEGIN] detailAccountApplications");

		/**
		 * header 정보로 요청 사용자 권한 체크
		 */
		AuthUtils.checkUserAdminAuth(ContextHolder.exeContext());

		AccountApplicationVO accountApplication;
		try {
			accountApplication = accountApplicationService.getDetailAccountApplicationByAdmin(accountApplicationSeq);
			accountApplication.setUserPassword(null);
			accountApplication.setHashSalt(null);
		} catch (CocktailException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new CocktailException("detailAccountApplications Fail.", ex, ExceptionType.CommonInquireFail);
		}

		log.debug("[END  ] detailAccountApplications");

		return accountApplication;
	}


	@PostMapping(value = "/{accountApplicationSeq}/send/completed/mail")
	@Operation(summary = "플랫폼 구축 완료 메일 전송", description = "플랫폼 구축 완료 메일 전송 한다.")
	public void sendConstructionCompletedMail(
			@Parameter(name = "accountApplicationSeq", description = "Account Application Sequence", required = true) @PathVariable Integer accountApplicationSeq
	) throws Exception {
		log.debug("[BEGIN] sendConstructionCompletedMail");

		/**
		 * header 정보로 요청 사용자 권한 체크
		 */
		AuthUtils.checkUserAdminAuth(ContextHolder.exeContext());

		AccountApplicationVO prevAccountApplication = accountApplicationService.getDetailAccountApplicationByAdmin(accountApplicationSeq);
		if (prevAccountApplication == null) {
			throw new CocktailException("Invalid parameter", ExceptionType.AccountApplicationNotFound);
		} else {
			if (StringUtils.equalsIgnoreCase(prevAccountApplication.getStatus(), "C")) {
				accountApplicationService.sendConstructionCompletedMail(accountApplicationSeq);
			} else {
				throw new CocktailException("status Parameter is invalid.", ExceptionType.InvalidParameter);
			}
		}


		log.debug("[END  ] sendConstructionCompletedMail");
	}

}
