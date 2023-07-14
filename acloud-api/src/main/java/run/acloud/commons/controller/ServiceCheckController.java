package run.acloud.commons.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import run.acloud.api.code.service.CodeService;
import run.acloud.api.code.vo.CodeVO;
import run.acloud.api.openapi.service.OpenapiService;
import run.acloud.commons.service.LicenseService;

import java.util.HashMap;
import java.util.Map;

/**
 * Check Readiness of API & Builder
 *
 *
 * @author: coolingi@acornsoft.io
 * Created on 2018. 04. 16.
 */

@Tag(name = "check", description = "Readiness , Liveness")
@Slf4j
@RestController
@Validated
@RequestMapping(value = "/api/check")
public class ServiceCheckController {

    private static Logger serviceCheckLogger = LoggerFactory.getLogger("serviceCheck.logger");

    @Autowired
    private CodeService codeService;

    @Autowired
    private LicenseService licenseService;

    @Autowired
    private OpenapiService openapiService;

    @Operation(summary = "Startup 체크")
    @GetMapping(value = "/startup")
    public Map<String, String> startup(HttpServletResponse response) throws Exception {
        serviceCheckLogger.debug("[BEGIN] startup");

        Map<String, String> resultMap = new HashMap<>();

        int errorCnt = 0;
        String errMsg = "";

        // Check API Database
        CodeVO code = codeService.getCode("PROVIDER", "AWS");
        if (code == null) {
            errMsg += "API Database Server Check Fail, ";
            errorCnt += 1;
        } else {
            errMsg += "API Database Server Check OK, ";
        }

        resultMap.put("resultMsg", errMsg);

        if(errorCnt > 0 ){
            serviceCheckLogger.error(String.format("\nError Cnt %d , Msg : %s",errorCnt,errMsg));
//            throw new CocktailException(errMsg);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } else {
            // trial license 생성
            licenseService.issueTrialLicense(null, false, false, true);

            // generate jwks
            openapiService.generateJwk(false, false, true);

            response.setStatus(HttpServletResponse.SC_OK);
        }

        serviceCheckLogger.debug("[END  ] startup");
        return resultMap;
    }

    @Operation(summary = "Readiness 체크")
    @GetMapping(value = "/readiness")
    public Map<String, String> readiness(HttpServletResponse response) throws Exception {
        serviceCheckLogger.debug("[BEGIN] Readiness");

        Map<String, String> resultMap = new HashMap<>();

        int errorCnt = 0;
        String errMsg = "";

        // Check API Database
        CodeVO code = codeService.getCode("PROVIDER", "AWS");
        if (code == null) {
            errMsg += "API Database Server Check Fail, ";
            errorCnt += 1;
        } else {
            errMsg += "API Database Server Check OK, ";
        }

        resultMap.put("resultMsg", errMsg);

        if(errorCnt > 0 ){
            serviceCheckLogger.error(String.format("\nError Cnt %d , Msg : %s",errorCnt,errMsg));
//            throw new CocktailException(errMsg);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } else {
            // trial license 생성
            licenseService.issueTrialLicense(null, false, false, true);

            // generate jwks
            openapiService.generateJwk(false, false, true);

            response.setStatus(HttpServletResponse.SC_OK);
        }

        serviceCheckLogger.debug("[END  ] Readiness");
        return resultMap;
    }

    @Operation(summary = "Liveness 체크")
    @GetMapping(value = "/liveness")
    public Map<String, String> liveness() throws Exception{
        serviceCheckLogger.debug("[BEGIN] Liveness");
        Map<String, String> resultMap = new HashMap<>();
        resultMap.put("resultMsg", "OK");
        serviceCheckLogger.debug("[END  ] Liveness");
        return resultMap;
    }
}