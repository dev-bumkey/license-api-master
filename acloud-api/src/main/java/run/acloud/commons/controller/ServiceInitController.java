package run.acloud.commons.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import run.acloud.commons.service.CocktailInitService;
import run.acloud.commons.vo.CocktailInitVO;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Service Initial data api
 *
 *
 * @author: coolingi@acornsoft.io
 * Created on 2018. 04. 16.
 */

@Tag(name = "Service Initial data", description = "Service Initial data api")
@Slf4j
@RestController
@Validated
@RequestMapping(value = "/api/init")
public class ServiceInitController {

    final static String TOKEN_STRING = "InitialDataCocktail@20180920"; // For Migration Data

    @Autowired
    private CocktailInitService cocktailInitService;

    @Operation(summary = "Service Initial Data api")
    @PostMapping(value = "/data/{version:.+}")
    public Map<String, String> initDataOfVersion(
            @Parameter(description = "version", required = true) @PathVariable String version,
            @Parameter(name = "cocktailInit", required = true) @Validated @RequestBody CocktailInitVO cocktailInit
    ) throws Exception {
        log.debug("[BEGIN] initDataOfVersion");

        if(!StringUtils.equals(TOKEN_STRING, new String(Base64.getDecoder().decode(cocktailInit.getToken())))){
            throw new CocktailException("Invalid Token.", ExceptionType.InvalidParameter);
        }

        Map<String, String> resultMap = new HashMap<>();

        cocktailInitService.initOfCocktail(version, cocktailInit);

        log.debug("[END  ] initDataOfVersion");

        return resultMap;
    }
}