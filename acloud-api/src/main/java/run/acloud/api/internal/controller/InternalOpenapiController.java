package run.acloud.api.internal.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import run.acloud.api.openapi.service.OpenapiService;
import run.acloud.api.openapi.vo.*;
import run.acloud.commons.annotations.InHouse;

import java.util.List;

/**
 * @author dy79@acornsoft.io
 * Created on 2017. 3. 13.
 */
@Tag(name = "Internal Openapi", description = "내부호출용 Openapi 관련 기능을 제공한다.")
@InHouse
@Slf4j
@RestController
@RequestMapping(value = "/internal/openapi")
public class InternalOpenapiController {

    @Autowired
    private OpenapiService openapiService;


    @GetMapping(value = "/.well-known/jwks.json")
    @Operation(summary = "jwks json 조회", description = "jwks json을 조회한다.")
    public String getJsonWebKeySet() throws Exception {
        log.debug("[BEGIN] getJsonWebKeySet");

        String jwksJson = openapiService.getJwks();

        log.debug("[END  ] getJsonWebKeySet");

        return jwksJson;
    }

    @GetMapping(value = "/api/groups")
    @Operation(summary = "API Group 조회", description = "API Group을 조회한다.")
    public List<ApiGatewayGroupsVO> getApiGatewayGroups(
            @Parameter(name = "withApi", description = "API 정보 포함 여부", schema = @Schema(allowableValues = {"true","false"}, defaultValue = "false")) @RequestParam(value = "withApi", required = false, defaultValue = "false") boolean withApi
    ) throws Exception {
        log.debug("[BEGIN] getApiGatewayGroups");

        List<ApiGatewayGroupsVO> results = openapiService.getApiGatewayGroups(withApi);

        log.debug("[END  ] getApiGatewayGroups");

        return results;
    }

    @GetMapping(value = "/apis")
    @Operation(summary = "API 조회", description = "API를 조회한다.")
    public List<ApiGatewaysVO> getApiGateways() throws Exception {
        log.debug("[BEGIN] getApiGateways");

        List<ApiGatewaysVO> results = openapiService.getApiGateways(null);

        log.debug("[END  ] getApiGateways");

        return results;
    }

    @GetMapping(value = "/tokens/detail")
    @Operation(summary = "API 토큰 발급 상세 조회", description = "API 토큰 발급 상세 정보를 조회한다.")
    public ApiTokenIssueDetailVO getApiTokenIssueByToken(
            @Parameter(name = "accountSeq", description = "플랫폼번호", required = true) @RequestParam Integer accountSeq,
            @Parameter(name = "token", description = "조회할 token", required = true) @RequestParam String token
    ) throws Exception {
        log.debug("[BEGIN] getApiTokenIssueByToken");

        ApiTokenIssueDetailVO result = openapiService.getApiTokenIssueByToken(accountSeq, token);
        if (result != null) {
            if (StringUtils.isBlank(result.getExpirationDatetime())) {
                result.setExpirationDatetime("9999-12-31");
            }
        }

        log.debug("[END  ] getApiTokenIssueByToken");

        return result;
    }

    @GetMapping(value = "/tokens/relations")
    @Operation(summary = "API 토큰 발급 관계 조회", description = "API 토큰 발급 관계 정보를 조회한다.")
    public List<ApiTokenIssueRelationVO> getApiTokenIssuesRelation(
            @Parameter(name = "accountSeq", description = "플랫폼번호") @RequestParam(required = false) Integer accountSeq
    ) throws Exception {
        log.debug("[BEGIN] getApiTokenIssuesRelation");

        List<ApiTokenIssueRelationVO> result = openapiService.getApiTokenIssuesRelation(accountSeq);

        log.debug("[END  ] getApiTokenIssuesRelation");

        return result;
    }

    @PostMapping(value = "/tokens/audits")
    @Operation(summary = "API 토큰 감사 로그 등록", description = "API 토큰을 사용한 감사 로그를 등록한다.")
    public void addApiTokenAuditLog(
            @Parameter(name = "apiTokenAuditLogAdd", description = "API 토큰 감사 로그 생성 모델", required = true) @RequestBody ApiTokenAuditLogAddVO apiTokenAuditLogAdd
    ) throws Exception {
        log.debug("[BEGIN] addApiTokenAuditLog");

        openapiService.addApiTokenAuditLog(apiTokenAuditLogAdd);

        log.debug("[END  ] addApiTokenAuditLog");

    }

    @PutMapping(value = "/tokens/{apiTokenIssueSeq}/request/counts")
    @Operation(summary = "API 토큰 호출 건수 업데이트", description = "API 토큰 호출 건수를 업데이트한다.")
    public ApiTokenRequestCountVO editRequestCountByToken(
            @Parameter(name = "apiTokenIssueSeq", description = "API 토큰 발급 순번", required = true) @PathVariable Integer apiTokenIssueSeq,
            @Parameter(name = "accountSeq", description = "플랫폼번호", required = true) @RequestParam Integer accountSeq
    ) throws Exception {
        log.debug("[BEGIN] editRequestCountByToken");

        ApiTokenRequestCountVO result = openapiService.editApiTokenRequestCount(apiTokenIssueSeq, accountSeq);

        log.debug("[END  ] editRequestCountByToken");

        return result;
    }
}
