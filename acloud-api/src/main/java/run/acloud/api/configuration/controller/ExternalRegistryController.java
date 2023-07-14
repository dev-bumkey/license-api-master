package run.acloud.api.configuration.controller;

import com.google.common.collect.Lists;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import run.acloud.api.configuration.enums.ServiceType;
import run.acloud.api.configuration.service.ExternalRegistryService;
import run.acloud.api.configuration.vo.ExternalRegistryAddVO;
import run.acloud.api.configuration.vo.ExternalRegistryDetailVO;
import run.acloud.api.configuration.vo.ExternalRegistryVO;
import run.acloud.commons.util.AuthUtils;
import run.acloud.framework.context.ContextHolder;
import run.acloud.framework.enums.ExceptionBiz;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;
import run.acloud.framework.util.ExceptionMessageUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Tag(name = "External Registry", description = "외부 이미지 레지스트리 관련 기능을 제공한다.")
@Slf4j
@RequestMapping(value = "/api/externalregistry")
@RestController
@Validated
public class ExternalRegistryController {

    @Autowired
    private ExternalRegistryService externalRegistryService;

    @Operation(summary = "외부 레지스트리 목록 조회", description = "외부 레지스트리의 목록을 조회한다.")
    @GetMapping(value = "/list")
    public List<ExternalRegistryVO> getExternalRegistries(
            @Parameter(description = "계정 번호", required = true) @RequestParam Integer accountSeq,
            @Parameter(description = "워크스페이스 번호") @RequestParam(required = false) Integer serviceSeq,
            @Parameter(description = "서비스맵 번호") @RequestParam(required = false) Integer servicemapSeq,
            @Parameter(description = "접속상태 확인 여부", schema = @Schema(allowableValues = {"true","false"}, defaultValue = "false")) @RequestParam(value = "useStatus", required = false, defaultValue = "false") boolean useStatus
     ) throws Exception {
        log.debug("[BEGIN] getExternalRegistries");

        try {
            /**
             * header 정보로 요청 사용자 권한 체크
             */
//            AuthUtils.checkAuth(ContextHolder.exeContext(), AuthUtils.checkUserDevOpsBlockAuthPredicate());

            List<ExternalRegistryVO> results = externalRegistryService.getExternalRegistriesWithStatus(accountSeq, serviceSeq, servicemapSeq, null, null, null, useStatus);
            for (ExternalRegistryVO item : results) {
                // 인증정보 제거
                externalRegistryService.clearCertInfo(item);
            }
            return results;
        } finally {
            log.debug("[END  ] getExternalRegistries");
        }
    }


    @GetMapping(value = "/name/{name}/duplicate/check")
    @Operation(summary = "외부 레지스트리 이름 중복 체크", description = "외부 레지스트리 이름을 중복 체크한다.")
    public Map<String, Object> checkDuplicateName(
            @Parameter(description = "외부 레지스트리 이름", required = true) @PathVariable String name,
            @Parameter(description = "계정 번호", required = true) @RequestParam Integer accountSeq,
            @Parameter(description = "수정하려는 외부 레지스트리 번호") @RequestParam(required = false) Integer externalRegistrySeq
    ) throws Exception {
        log.debug("[BEGIN] checkDuplicateName");

        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("isValid", Boolean.TRUE);
        try {
            /**
             * header 정보로 요청 사용자 권한 체크
             */
            AuthUtils.checkAuth(ContextHolder.exeContext(), AuthUtils.checkUserDevOpsBlockAuthPredicate());

            ExceptionMessageUtils.checkParameterRequired("name", name);
            ExceptionMessageUtils.checkParameterRequired("accountSeq", accountSeq);

            List<ExternalRegistryVO> externalRegistrys = Optional.ofNullable(externalRegistryService.getExternalRegistriesWithStatus(accountSeq, null, null,  name, null, null, false)).orElseGet(() ->Lists.newArrayList());

            // 수정시 자기자신 제외 처리
            if (externalRegistrySeq != null && externalRegistrySeq.intValue() > 0) {
                externalRegistrys.removeIf(er -> (er.getExternalRegistrySeq().equals(externalRegistrySeq)));
            }

            if (externalRegistrys.size() > 0) {
                resultMap.put("isValid", Boolean.FALSE);
                return resultMap;
            }

        } catch (CocktailException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new CocktailException("checkDuplicateName Fail.", ex, ExceptionType.CommonInquireFail, ExceptionBiz.EXTERNAL_REGISTRY);
        }

        log.debug("[END  ] checkDuplicateName");

        return resultMap;
    }

    @GetMapping(value = "/duplicate/check")
    @Operation(summary = "외부 레지스트리 url+이름 중복 체크", description = "외부 레지스트리 url+이름을 중복 체크한다.")
    public Map<String, Object> checkDuplicateRegistry(
            @Parameter(description = "계정 번호", required = true) @RequestParam Integer accountSeq,
            @Parameter(description = "외부 레지스트리 - endpoint Url", required = true) @RequestParam String endpointUrl,
            @Parameter(description = "외부 레지스트리 - registry Name", required = true) @RequestParam String registryName
    ) throws Exception {
        log.debug("[BEGIN] checkDuplicateRegistry");

        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("isValid", Boolean.TRUE);
        try {
            /**
             * header 정보로 요청 사용자 권한 체크
             */
            AuthUtils.checkAuth(ContextHolder.exeContext(), AuthUtils.checkUserDevOpsBlockAuthPredicate());

            ExceptionMessageUtils.checkParameterRequired("accountSeq", accountSeq);
            ExceptionMessageUtils.checkParameterRequired("endpointUrl", endpointUrl);
            ExceptionMessageUtils.checkParameterRequired("registryName", registryName);

            if (Optional.ofNullable(externalRegistryService.getExternalRegistriesWithStatus(accountSeq, null, null, null, endpointUrl, registryName, false)).orElseGet(() ->Lists.newArrayList()).size() > 0) {
                resultMap.put("isValid", Boolean.FALSE);
                return resultMap;
            }

        } catch (CocktailException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new CocktailException("checkDuplicateRegistry Fail.", ex, ExceptionType.CommonInquireFail, ExceptionBiz.EXTERNAL_REGISTRY);
        }

        log.debug("[END  ] checkDuplicateRegistry");

        return resultMap;
    }

    @PostMapping(value = "/connection/check")
    @Operation(summary = "외부 레지스트리 접속 체크", description = "외부 레지스트리 접속 체크한다.")
    public Map<String, Object> checkConnectionRegistry(
            @Parameter(name = "외부 레지스트리 모델", description = "외부 레지스트리 모델", required = true) @RequestBody ExternalRegistryAddVO externalRegistry
    ) throws Exception {
        log.debug("[BEGIN] checkConnectionRegistry");

        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("isValid", Boolean.TRUE);
        try {
            /**
             * header 정보로 요청 사용자 권한 체크
             */
            AuthUtils.checkAuth(ContextHolder.exeContext(), AuthUtils.checkUserDevOpsBlockAuthPredicate());

            ExceptionMessageUtils.checkParameterRequired("accountSeq", externalRegistry.getAccountSeq());
            ExceptionMessageUtils.checkParameterRequired("provider", externalRegistry.getProvider());
            externalRegistryService.checkParamterUrlValidation(externalRegistry.getEndpointUrl());
            ExceptionMessageUtils.checkParameterRequired("accessId", externalRegistry.getAccessId());
            ExceptionMessageUtils.checkParameterRequired("accessSecret", externalRegistry.getAccessSecret());
            ExceptionMessageUtils.checkParameterRequired("insecure", externalRegistry.getInsecureYn());
            externalRegistryService.setParameter(externalRegistry);

            // 체크
            externalRegistryService.getConnectionStatus(externalRegistry, false);

            if (!BooleanUtils.toBoolean(externalRegistry.getStatus())) {
                resultMap.put("isValid", Boolean.FALSE);
            }

        } catch (CocktailException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new CocktailException("checkConnectionRegistry Fail.", ex, ExceptionType.CommonInquireFail, ExceptionBiz.EXTERNAL_REGISTRY);
        }

        log.debug("[END  ] checkConnectionRegistry");

        return resultMap;
    }

    @Operation(summary = "외부 레지스트리 상세", description = "외부 레지스트리의 상세 정보를 응답한다.")
    @GetMapping(value = "/{externalRegistrySeq}")
    public ExternalRegistryDetailVO getExternalRegistry(
            @Parameter(name = "externalRegistrySeq", description = "외부 레지스트리 번호", required = true) @PathVariable int externalRegistrySeq
    ) throws Exception {

        log.debug("[BEGIN] getExternalRegistry");

        try {
            /**
             * header 정보로 요청 사용자 권한 체크
             */
            AuthUtils.checkAuth(ContextHolder.exeContext(), AuthUtils.checkUserDevOpsBlockAuthPredicate());

            ExternalRegistryDetailVO result = externalRegistryService.getExternalRegistry(externalRegistrySeq, ServiceType.NORMAL.getCode());

            // 인증정보 제거
            externalRegistryService.clearCertInfo(result);

            return result;
        } finally {
            log.debug("[END  ] getExternalRegistry");
        }
    }

    @PostMapping(value = "/account/{accountSeq}")
    @Operation(summary = "외부 레지스트리 등록", description = "외부 레지스트리를 등록한다.")
    public ExternalRegistryDetailVO addExternalRegistry(
            @Parameter(name = "accountSeq", description = "플랫폼 번호", required = true) @PathVariable Integer accountSeq,
            @Parameter(name = "외부 레지스트리 모델", description = "외부 레지스트리 모델", required = true) @RequestBody ExternalRegistryAddVO externalRegistry
    ) throws Exception {
        log.debug("[BEGIN] addExternalRegistry");

        try {
            /**
             * header 정보로 요청 사용자 권한 체크
             */
            AuthUtils.checkAuth(ContextHolder.exeContext(), AuthUtils.checkUserDevOpsBlockAuthPredicate());

            externalRegistry.setAccountSeq(accountSeq);
            ExternalRegistryDetailVO result = externalRegistryService.addExternalRegistry(externalRegistry);

            // 인증정보 제거
            externalRegistryService.clearCertInfo(result);

            return result;
        } catch (DataAccessException de) {
            throw de; // DB 처리중 Runtime Exception이 발생하면 처리를 공통 ExceptionHandler 로 던지고 공통 ExceptionHandler에서 처리
        } catch (CocktailException ce) {
            throw ce; // CocktailException이 발생하면 발생한 예외 처리 케이스를 그대로 사용
        } catch (Exception e) {
            throw new CocktailException(e.getMessage(), e, ExceptionType.CommonCreateFail, ExceptionBiz.EXTERNAL_REGISTRY);
        } finally {
            log.debug("[END  ] addExternalRegistry");
        }
    }

    @PutMapping(value = "/{externalRegistrySeq}")
    @Operation(summary = "외부 레지스트리 수정", description = "외부 레지스트리를 수정한다.")
    public ExternalRegistryDetailVO editExternalRegistry(
            @Parameter(name = "externalRegistrySeq", description = "외부 레지스트리 번호", required = true) @PathVariable int externalRegistrySeq,
            @Parameter(name = "외부 레지스트리 모델", description = "외부 레지스트리 모델", required = true) @RequestBody ExternalRegistryAddVO externalRegistry
    ) throws Exception {
        log.debug("[BEGIN] editExternalRegistry");

        try {
            /**
             * header 정보로 요청 사용자 권한 체크
             */
            AuthUtils.checkAuth(ContextHolder.exeContext(), AuthUtils.checkUserDevOpsBlockAuthPredicate());

            externalRegistry.setExternalRegistrySeq(externalRegistrySeq);
            ExternalRegistryDetailVO result = externalRegistryService.editExternalRegistry(externalRegistry);

            // 인증정보 제거
            externalRegistryService.clearCertInfo(result);

            return result;
        } catch (DataAccessException de) {
            throw de; // DB 처리중 Runtime Exception이 발생하면 처리를 공통 ExceptionHandler 로 던지고 공통 ExceptionHandler에서 처리
        } catch (CocktailException ce) {
            throw ce; // CocktailException이 발생하면 발생한 예외 처리 케이스를 그대로 사용
        } catch (Exception e) {
            throw new CocktailException(e.getMessage(), e, ExceptionType.CommonUpdateFail, ExceptionBiz.EXTERNAL_REGISTRY);
        } finally {
            log.debug("[END  ] editExternalRegistry");
        }
    }

    @DeleteMapping(value = "/{externalRegistrySeq}")
    @Operation(summary = "외부 레지스트리 삭제", description = "외부 레지스트리를 삭제한다.")
    public ExternalRegistryDetailVO deleteExternalRegistry(
            @Parameter(name = "externalRegistrySeq", description = "외부 레지스트리 번호", required = true) @PathVariable int externalRegistrySeq,
            @Parameter(name = "cascade", description = "종속된 정보(DB) 모두 삭제", schema = @Schema(allowableValues = {"true","false"}, defaultValue = "false")) @RequestParam(value = "cascade", required = false, defaultValue = "false") boolean cascade
    ) throws Exception {
        log.debug("[BEGIN] deleteExternalRegistry");

        try {
            /**
             * header 정보로 요청 사용자 권한 체크
             */
            AuthUtils.checkAuth(ContextHolder.exeContext(), AuthUtils.checkUserDevOpsBlockAuthPredicate());

            ExternalRegistryDetailVO result = externalRegistryService.deleteExternalRegistry(externalRegistrySeq, cascade);

            // 인증정보 제거
            externalRegistryService.clearCertInfo(result);

            return result;
        } catch (DataAccessException de) {
            throw de; // DB 처리중 Runtime Exception이 발생하면 처리를 공통 ExceptionHandler 로 던지고 공통 ExceptionHandler에서 처리
        } catch (CocktailException ce) {
            throw ce; // CocktailException이 발생하면 발생한 예외 처리 케이스를 그대로 사용
        } catch (Exception e) {
            throw new CocktailException(e.getMessage(), e, ExceptionType.CommonDeleteFail, ExceptionBiz.EXTERNAL_REGISTRY);
        } finally {
            log.debug("[END  ] deleteExternalRegistry");
        }

    }

}
