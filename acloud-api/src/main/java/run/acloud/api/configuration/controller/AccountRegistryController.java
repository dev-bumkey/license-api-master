package run.acloud.api.configuration.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import run.acloud.api.configuration.service.AccountRegistryService;
import run.acloud.api.configuration.vo.AccountRegistryVO;
import run.acloud.commons.util.AuthUtils;
import run.acloud.framework.context.ContextHolder;
import run.acloud.framework.enums.ExceptionBiz;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;
import run.acloud.framework.util.ExceptionMessageUtils;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "Platform Registry", description = "플랫폼 이미지 레지스트리 관련 기능을 제공한다.")
@Slf4j
@RequestMapping(value = "/api/accountregistry")
@RestController
@Validated
public class AccountRegistryController {

    @Autowired
    private AccountRegistryService accountRegistryService;
    

    @PostMapping(value = "/connection/check")
    @Operation(summary = "플랫폼 레지스트리 접속 체크", description = "플랫폼 레지스트리 접속 체크한다.")
    public Map<String, Object> checkConnectionRegistry(
            @Parameter(name = "플랫폼 레지스트리 모델", description = "플랫폼 레지스트리 모델", required = true) @RequestBody AccountRegistryVO accountRegistry
    ) throws Exception {
        log.debug("[BEGIN] checkConnectionRegistry");

        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("isValid", Boolean.TRUE);
        try {
            /**
             * header 정보로 요청 사용자 권한 체크
             */
            AuthUtils.checkAuth(ContextHolder.exeContext(), AuthUtils.checkUserDevOpsBlockAuthPredicate());

            ExceptionMessageUtils.checkParameterRequired("provider", accountRegistry.getProvider());
            accountRegistryService.checkParamterUrlValidation(accountRegistry.getRegistryUrl());
            ExceptionMessageUtils.checkParameterRequired("accessId", accountRegistry.getAccessId());
            ExceptionMessageUtils.checkParameterRequired("accessSecret", accountRegistry.getAccessSecret());
            ExceptionMessageUtils.checkParameterRequired("insecureYn", accountRegistry.getInsecureYn());
            accountRegistryService.setParameter(accountRegistry);

            // 체크
            accountRegistryService.getConnectionStatus(accountRegistry, false);

            if (!BooleanUtils.toBoolean(accountRegistry.getStatus())) {
                resultMap.put("isValid", Boolean.FALSE);
            }

        } catch (CocktailException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new CocktailException("checkConnectionRegistry Fail.", ex, ExceptionType.CommonInquireFail, ExceptionBiz.ACCOUNT_REGISTRY);
        }

        log.debug("[END  ] checkConnectionRegistry");

        return resultMap;
    }

    @Operation(summary = "플랫폼 레지스트리 상세", description = "플랫폼 레지스트리의 상세 정보를 응답한다.")
    @GetMapping(value = "/{accountSeq}")
    public AccountRegistryVO getAccountRegistry(
            @Parameter(name = "accountSeq", description = "플랫폼 번호", required = true) @PathVariable int accountSeq
    ) throws Exception {

        log.debug("[BEGIN] getAccountRegistry");

        try {
            /**
             * header 정보로 요청 사용자 권한 체크
             */
            AuthUtils.checkAuth(ContextHolder.exeContext(), AuthUtils.checkUserDevOpsBlockAuthPredicate());

            AccountRegistryVO result = accountRegistryService.getAccountRegistry(accountSeq);

            return result;
        } finally {
            log.debug("[END  ] getAccountRegistry");
        }
    }

    @PostMapping(value = "/account/{accountSeq}")
    @Operation(summary = "플랫폼 레지스트리 등록", description = "플랫폼 레지스트리를 등록한다.")
    public AccountRegistryVO addAccountRegistry(
            @Parameter(name = "accountSeq", description = "플랫폼 번호", required = true) @PathVariable Integer accountSeq,
            @Parameter(name = "플랫폼 레지스트리 모델", description = "플랫폼 레지스트리 모델", required = true) @RequestBody AccountRegistryVO accountRegistry
    ) throws Exception {
        log.debug("[BEGIN] addAccountRegistry");

        try {
            /**
             * header 정보로 요청 사용자 권한 체크
             */
            AuthUtils.checkAuth(ContextHolder.exeContext(), AuthUtils.checkUserDevOpsBlockAuthPredicate());

            accountRegistry.setAccountSeq(accountSeq);
            AccountRegistryVO result = accountRegistryService.addAccountRegistry(accountRegistry);


            return result;
        } catch (DataAccessException de) {
            throw de; // DB 처리중 Runtime Exception이 발생하면 처리를 공통 ExceptionHandler 로 던지고 공통 ExceptionHandler에서 처리
        } catch (CocktailException ce) {
            throw ce; // CocktailException이 발생하면 발생한 예외 처리 케이스를 그대로 사용
        } catch (Exception e) {
            throw new CocktailException(e.getMessage(), e, ExceptionType.CommonCreateFail, ExceptionBiz.ACCOUNT_REGISTRY);
        } finally {
            log.debug("[END  ] addAccountRegistry");
        }
    }

    @PutMapping(value = "/{accountRegistrySeq}")
    @Operation(summary = "플랫폼 레지스트리 수정", description = "플랫폼 레지스트리를 수정한다.")
    public AccountRegistryVO editAccountRegistry(
            @Parameter(name = "accountRegistrySeq", description = "플랫폼 레지스트리 번호", required = true) @PathVariable int accountRegistrySeq,
            @Parameter(name = "플랫폼 레지스트리 모델", description = "플랫폼 레지스트리 모델", required = true) @RequestBody AccountRegistryVO accountRegistry
    ) throws Exception {
        log.debug("[BEGIN] editAccountRegistry");

        try {
            /**
             * header 정보로 요청 사용자 권한 체크
             */
            AuthUtils.checkAuth(ContextHolder.exeContext(), AuthUtils.checkUserDevOpsBlockAuthPredicate());

            accountRegistry.setAccountRegistrySeq(accountRegistrySeq);
            AccountRegistryVO result = accountRegistryService.editAccountRegistry(accountRegistry);

            // 인증정보 제거
            accountRegistryService.clearCertInfo(result);

            return result;
        } catch (DataAccessException de) {
            throw de; // DB 처리중 Runtime Exception이 발생하면 처리를 공통 ExceptionHandler 로 던지고 공통 ExceptionHandler에서 처리
        } catch (CocktailException ce) {
            throw ce; // CocktailException이 발생하면 발생한 예외 처리 케이스를 그대로 사용
        } catch (Exception e) {
            throw new CocktailException(e.getMessage(), e, ExceptionType.CommonUpdateFail, ExceptionBiz.ACCOUNT_REGISTRY);
        } finally {
            log.debug("[END  ] editAccountRegistry");
        }
    }

    @DeleteMapping(value = "/{accountRegistrySeq}")
    @Operation(summary = "플랫폼 레지스트리 삭제", description = "플랫폼 레지스트리를 삭제한다.")
    public AccountRegistryVO deleteAccountRegistry(
            @Parameter(name = "accountRegistrySeq", description = "플랫폼 레지스트리 번호", required = true) @PathVariable int accountRegistrySeq
    ) throws Exception {
        log.debug("[BEGIN] deleteAccountRegistry");

        try {
            /**
             * header 정보로 요청 사용자 권한 체크
             */
            AuthUtils.checkAuth(ContextHolder.exeContext(), AuthUtils.checkUserDevOpsBlockAuthPredicate());

            AccountRegistryVO result = accountRegistryService.deleteAccountRegistry(accountRegistrySeq);

            // 인증정보 제거
            accountRegistryService.clearCertInfo(result);

            return result;
        } catch (DataAccessException de) {
            throw de; // DB 처리중 Runtime Exception이 발생하면 처리를 공통 ExceptionHandler 로 던지고 공통 ExceptionHandler에서 처리
        } catch (CocktailException ce) {
            throw ce; // CocktailException이 발생하면 발생한 예외 처리 케이스를 그대로 사용
        } catch (Exception e) {
            throw new CocktailException(e.getMessage(), e, ExceptionType.CommonDeleteFail, ExceptionBiz.ACCOUNT_REGISTRY);
        } finally {
            log.debug("[END  ] deleteAccountRegistry");
        }

    }

}
