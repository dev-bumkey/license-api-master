package run.acloud.api.external.controller;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import run.acloud.api.external.service.KeycloakService;
import run.acloud.api.external.vo.KeycloakUserListVO;
import run.acloud.api.external.vo.KeycloakUserVO;
import run.acloud.commons.annotations.InHouse;
import run.acloud.commons.util.PagingUtils;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * KeyCloak Interface API
 */
@InHouse
@Slf4j
@RequestMapping(value = "/api/keycloak")
@RestController
@Validated
public class KeycloakController {

    @Autowired
    private KeycloakService keycloakService;

    @GetMapping(value = "/{apiVersion}/users")
    public KeycloakUserListVO getKeycloakUsers(
        @RequestHeader(name = "keycloak-signature" ) String keySig,
        @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v1"}, defaultValue = "v1"), required = true) @PathVariable String apiVersion,
        @Parameter(name = "order", description = "정렬 순서", schema = @Schema(allowableValues = {"DESC","ASC"}, defaultValue = "ASC"), required = true) @RequestParam(name = "order", defaultValue = "ASC") String order,
        @Parameter(name = "orderColumn", description = "정렬기준컬럼", schema = @Schema(allowableValues = {"created","audit_log_seq"})) @RequestParam(name = "orderColumn", required = false) String orderColumn,
        @Parameter(name = "nextPage", description = "요청페이지", required = true, schema = @Schema(defaultValue = "1")) @RequestParam(name = "nextPage") Integer nextPage,
        @Parameter(name = "itemPerPage", description = "페이지당 표시 갯수", required = true, schema = @Schema(defaultValue = "10000")) @RequestParam(name = "itemPerPage") Integer itemPerPage,
        @Parameter(name = "searchColumn", description = "검색 컬럼") @RequestParam(name = "searchColumn", required = false) String searchColumn,
        @Parameter(name = "searchKeyword", description = "검색어") @RequestParam(name = "searchKeyword", required = false) String searchKeyword,
        @Parameter(name = "startDate", description = "검색 시작 일자") @RequestParam(name = "startDate", required = false) String startDate,
        @Parameter(name = "endDate", description = "검색 종료 일자") @RequestParam(name = "endDate", required = false) String endDate,
        @Parameter(name = "maxId", description = "더보기 중복 방지를 위한 데이터셋 위치 지정") @RequestParam(name = "maxId", required = false) String maxId,
        @Parameter(name = "newId", description = "신규 데이터 추가를 위한 데이터셋 위치 지정") @RequestParam(name = "newId", required = false) String newId
    ) throws CocktailException {
        log.debug("[BEGIN] getKeycloakUsers");
        KeycloakUserListVO keycloakUserList;
        try {

            if(!StringUtils.equals(keySig, getSHA256("keycloakSignature"))) {
                throw new CocktailException("getKeycloakUsers Fail : Invalid Signature ", ExceptionType.NotAuthorizedToRequest);
            }

            PagingUtils.validatePagingParams(nextPage, itemPerPage);
            PagingUtils.validatePagingParamsOrder(order, "ASC", "DESC");

            // order column 고정 (시간 역순) : 추후 확장..
            if("DESC".equalsIgnoreCase(order)) {
                orderColumn = "created";
            }
            else {
                orderColumn = "user_name";
            }

            keycloakUserList = keycloakService.getKeycloakUsers(order, orderColumn, nextPage, itemPerPage, searchColumn, searchKeyword, maxId, newId, startDate, endDate);

        } catch (CocktailException ce) {
            throw ce;
        } catch (Exception ex) {
            throw new CocktailException("getKeycloakUsers Fail.", ex, ExceptionType.CommonInquireFail);
        }

        log.debug("[END  ] getKeycloakUsers");

        return keycloakUserList;
    }

    /**
     * KeyCloak에서 사용하기 위해 해시된 패스워드도 응답하는 API임.
     * 유의하여 사용할것.
     *
     * @param encUserId
     * @return
     * @throws Exception
     */
    @GetMapping(value = "/{apiVersion}/user/id/{encUserId}")
    public KeycloakUserVO getKeycloakUser(
        @RequestHeader(name = "keycloak-signature" ) String keySig,
        @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v1"}, defaultValue = "v1"), required = true) @PathVariable String apiVersion,
        @Parameter(name = "encUserId", description = "encUserId", required = true) @PathVariable String encUserId
    ) throws Exception {
        log.debug("[BEGIN] getKeycloakUser");
        log.debug(getSHA256(encUserId));
        log.debug(getSHA256("keycloakSignature"));
        if(!StringUtils.equals(keySig, getSHA256("keycloakSignature"))) {
            throw new CocktailException("getKeycloakUsers Fail : Invalid Signature ", ExceptionType.NotAuthorizedToRequest);
        }

        String decUserId = new String(Base64.getDecoder().decode(encUserId), "UTF-8");
//        int p = decUserId.indexOf(".");
//        String accountCode = decUserId.substring(0, p);
//        String userId = decUserId.substring(p+1);
//
//
//        KeycloakUserVO keycloakUser = keycloakService.getKeycloakUser(accountCode, userId);
        KeycloakUserVO keycloakUser = keycloakService.getKeycloakUser(null, decUserId, "ADMIN");

        log.debug("[END  ] getKeycloakUser");

        return keycloakUser;
    }

    /**
     * KeyCloak에서 사용하기 위해 해시된 패스워드도 응답하는 API임.
     * 유의하여 사용할것.
     *
     * @param keySig
     * @param apiVersion
     * @param userId
     * @param roleCode
     * @return
     */
    @GetMapping(value="/{apiVersion}/auth/user/id/{userId:.+}")
    public KeycloakUserVO getKeycloakUserForAuth(
            @RequestHeader(name = "keycloak-signature" ) String keySig,
            @Parameter(name = "apiVersion", description = "apiVersion", schema = @Schema(allowableValues = {"v1"}, defaultValue = "v1"), required = true) @PathVariable String apiVersion,
            @Parameter(name = "userId", description = "userId", required = true) @PathVariable String userId,
            @Parameter(name = "roleCode", description = "roleCode") @RequestParam(required = false) String roleCode
    )throws Exception{
        log.debug("[BEGIN] getKeycloakUserForAuth");
        log.debug(getSHA256("keycloakSignature"));
        if(!StringUtils.equals(keySig, getSHA256("keycloakSignature"))) {
            throw new CocktailException("getKeycloakUsers Fail : Invalid Signature ", ExceptionType.NotAuthorizedToRequest);
        }

        KeycloakUserVO keycloakUser = keycloakService.getKeycloakUser(null, userId, roleCode);

        log.debug("[END  ] getKeycloakUserForAuth");

        return keycloakUser;
    }

    /**
     * SHA-256 해싱.
     * @param input
     * @return
     */
    private String getSHA256(String input){
        String toReturn = null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.reset();
            digest.update(input.getBytes("utf8"));
            toReturn = String.format("%064x", new BigInteger(1, digest.digest()));
        }
        catch (NoSuchAlgorithmException nsae) {
            CocktailException ce = new CocktailException(nsae.getMessage(), nsae, ExceptionType.CryptoFail);
            log.error(ce.getMessage(), ce);
        }
        catch (UnsupportedEncodingException uee) {
            CocktailException ce = new CocktailException(uee.getMessage(), uee, ExceptionType.CryptoFail);
            log.error(ce.getMessage(), ce);
        }
        catch (Exception e) {
            CocktailException ce = new CocktailException(e.getMessage(), e, ExceptionType.CryptoFail);
            log.error(ce.getMessage(), ce);
        }

        return toReturn;
    }
}
