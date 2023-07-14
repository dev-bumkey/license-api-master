package run.acloud.commons.controller;

import io.swagger.annotations.ApiOperation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import run.acloud.commons.service.CocktailMigrationService;
import run.acloud.commons.util.AuthUtils;
import run.acloud.framework.context.ContextHolder;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * version mig api
 *
 *
 * @author: coolingi@acornsoft.io
 * Created on 2018. 04. 16.
 */

@Tag(name = "migration", description = "version mig api")
@Slf4j
@RestController
@Validated
@RequestMapping(value = "/api/mig")
public class ServiceMigController {

    final static String TOKEN_STRING = "migrationCocktail@20180625"; // For Migration Data

    @Autowired
    private CocktailMigrationService cocktailMigrationService;

//    @Operation(summary = "Decrypt Data")
//    @PostMapping("/test/decrypt")
//    public void printDecryptedData(
//        @Parameter(name = "dataMap", required = true) @Valid @RequestBody Map<String, String> dataMap
//    ) throws Exception {
//        log.debug("[BEGIN] printDecryptedData");
//        log.debug("==========================");
//        log.debug(CryptoUtils.decryptAES(dataMap.get("data")));
//        log.debug("==========================");
//        log.debug("[END  ] printDecryptedData");
//    }

    @Operation(summary = "version migration api")
    @PostMapping(value = "/{version:.+}")
    public Map<String, String> migrationOfVersion(
            @Parameter(description = "version", required = true) @PathVariable String version,
            @Parameter(name = "token", required = true) @Valid @RequestBody Map<String, String> token
    ) throws Exception {
        log.debug("[BEGIN] migrationOfVersion");

        String decodedToken = new String(Base64.getDecoder().decode(token.get("tokenKey")));
        if(!StringUtils.equals(TOKEN_STRING, decodedToken)){
            throw new CocktailException("Invalid Token.", ExceptionType.InvalidParameter);
        }

        Map<String, String> resultMap = new HashMap<>();

        cocktailMigrationService.migrationOfCocktail(version);

        log.debug("[END  ] migrationOfVersion");

        return resultMap;
    }

//    @Operation(summary = "version migration api")
//    @PostMapping(value = "/{version}/reverse")
    public Map<String, String> reverseMigrationOfVersion(
            @Parameter(description = "version", required = true) @PathVariable String version,
            @Parameter(name = "token", required = true) @Valid @RequestBody Map<String, String> token
    ) throws Exception {
        log.debug("[BEGIN] reverseMigrationOfVersion");

        String decodedToken = new String(Base64.getDecoder().decode(token.get("tokenKey")));
        if(!StringUtils.equals(TOKEN_STRING, decodedToken)){
            throw new CocktailException("Invalid Token.", ExceptionType.InvalidParameter);
        }

        Map<String, String> resultMap = new HashMap<>();

        // cocktailMigrationService.reverseMigrationOfCocktail(version);

        log.debug("[END  ] reverseMigrationOfVersion");

        return resultMap;
    }

    @Operation(summary = "Workload Group 정보를 각각 Workload의 Annotation으로 마이그레이션", description = "Workload Group 정보를 각각 Workload의 Annotation으로 마이그레이션")
    @PostMapping("/z/do/not/touch/mig/groupMigration")
    public String migWorkloadGroupToAnnotation(
        @Parameter(name = "clusterSeq", description = "Cluster Sequence") @RequestParam(value = "clusterSeq", required = false) Integer clusterSeq,
        @Parameter(name = "appmapSeq", description = "Appmap Sequence") @RequestParam(value = "appmapSeq", required = false) Integer appmapSeq,
        @Parameter(name = "accessKey", description = "이중인증키 (짱)", required = true) @RequestParam(value = "accessKey") String accessKey) throws Exception
    {
        log.debug("[BEGIN] migWorkloadGroupToAnnotation");

        if(!AuthUtils.isAdminUser(ContextHolder.exeContext())) {
            throw new CocktailException("Admin role required.", ExceptionType.NotAuthorizedToRequest);
        }

        if(!"cocktailWkd1!".equals(accessKey)) {
            throw new CocktailException("Invalid Access Key.", ExceptionType.NotAuthorizedToRequest);
        }

        ContextHolder.get().put("clusterSeq", clusterSeq);
        ContextHolder.get().put("appmapSeq", appmapSeq);

//        JsonUtils.toPrettyString(cocktailMigrationService.migrationWorkloadGroupToAnnotation());
        return null;
    }

    @Operation(summary = "Snapshot 정보를 YAML 형태로 Migration (Workload안의 Service는 별도로 분리해냄)", description = "Snapshot 정보를 YAML 형태로 Migration (Workload안의 Service는 별도로 분리해냄)")
    @PostMapping("/z/do/not/touch/mig/snapshotMigration")
    public String migServicesInSnapshot(
        @Parameter(name = "accessKey", description = "이중인증키 (짱)", required = true) @RequestParam(value = "accessKey") String accessKey) throws Exception
    {
        log.debug("[BEGIN] migServicesInSnapshot");

        if(!AuthUtils.isAdminUser(ContextHolder.exeContext())) {
            throw new CocktailException("Admin role required.", ExceptionType.NotAuthorizedToRequest);
        }

        if(!"cocktailWkd1!".equals(accessKey)) {
            throw new CocktailException("Invalid Access Key.", ExceptionType.NotAuthorizedToRequest);
        }

//        return JsonUtils.toPrettyString(cocktailMigrationService.migrationServicesInSnapshot());
        return null;
    }

    @Operation(summary = "For Test Only!!!!! : Snapshot 정보를 YAML 형태로 Migration : (Workload안의 Service는 별도로 분리해냄)", description = "For Test Only!!!!! : Snapshot 정보를 YAML 형태로 Migration (Workload안의 Service는 별도로 분리해냄)")
    @PostMapping("/z/do/not/touch/mig/snapshotMigrationTest")
    public String migServicesInSnapshotTest(
        @Parameter(name = "accessKey", description = "이중인증키 (짱)", required = true) @RequestParam(value = "accessKey") String accessKey) throws Exception
    {
        log.debug("[BEGIN] migServicesInSnapshotTest");

        if(!AuthUtils.isAdminUser(ContextHolder.exeContext())) {
            throw new CocktailException("Admin role required.", ExceptionType.NotAuthorizedToRequest);
        }

        if(!"cocktailWkd1!".equals(accessKey)) {
            throw new CocktailException("Invalid Access Key.", ExceptionType.NotAuthorizedToRequest);
        }

//        return JsonUtils.toPrettyString(cocktailMigrationService.migrationServicesInSnapshotTest());
        return null;
    }

    @Operation(summary = "audit_logs 데이터를 audit_access_logs 테이블로 마이그레이션 한다.")
    @PostMapping("/audit/log")
    public void migrationAuditLogsToAuditAccessLog(
            @Parameter(name = "accessKey", description = "이중인증키 (짱)", required = true) @RequestParam(value = "accessKey") String accessKey
    ) throws Exception
    {
        log.debug("[BEGIN] migrationAuditLogsToAuditAccessLog");

        if(!AuthUtils.isAdminUser(ContextHolder.exeContext())) {
            throw new CocktailException("Admin role required.", ExceptionType.NotAuthorizedToRequest);
        }

        if(!"cocktailWkd1!".equals(accessKey)) {
            throw new CocktailException("Invalid Access Key.", ExceptionType.NotAuthorizedToRequest);
        }

        cocktailMigrationService.migrationAuditLogsToAuditAccessLog();

        log.debug("[END] migrationAuditLogsToAuditAccessLog");
    }


}