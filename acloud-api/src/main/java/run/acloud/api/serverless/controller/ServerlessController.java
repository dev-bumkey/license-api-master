package run.acloud.api.serverless.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import run.acloud.api.serverless.enums.ServerlessType;
import run.acloud.api.serverless.service.ServerlessService;
import run.acloud.api.serverless.vo.*;
import run.acloud.commons.annotations.InHouse;
import run.acloud.commons.util.CryptoUtils;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;

import java.util.List;

/**
 * @author wschoi@acornsoft.io on 2017. 1. 10.
 */
@InHouse
@Tag(name = "Serverless", description = "Serverless 관련 기능을 제공한다.")
@Slf4j
@RestController
@Validated
@RequestMapping(value = "/api/serverless")
public class ServerlessController {

    @Autowired
    private ServerlessService serverlessService;

    @PostMapping(value = "/project")
    @Operation(summary = "프로젝트 생성", description = "프로젝트를 생성한다.")
    public ServerlessWorkspaceVO addProject(
            @Parameter(description = "서비리스 프로젝트 생성 모델", required = true) @RequestBody @Validated ProjectAddVO project
    ) throws Exception {
        log.debug("[BEGIN] addProject");


        ServerlessWorkspaceVO serverlessWorkspace = serverlessService.addProjectWithWorkspace(project);

        if (serverlessWorkspace != null) {
            serverlessWorkspace.setAccount(null);
            serverlessWorkspace.setClusters(null);
            serverlessWorkspace.setServicemapGroups(null);
            serverlessWorkspace.setServicemaps(null);
            serverlessWorkspace.setUsers(null);
            serverlessWorkspace.setProjects(null);
            serverlessWorkspace.setExternalRegistries(null);
        }

        log.debug("[END  ] addProject");

        return serverlessWorkspace;
    }

    @GetMapping(value = "/projects")
    @Operation(summary = "프로젝트 목록 조회", description = "프로젝트를 목록을 조회한다.")
    public List<ServerlessVO> getProjects(
            @Parameter(name = "userId", description = "사용자 ID", required = true) @RequestParam(value = "userId") String userId,
            @Parameter(name = "clusterId", description = "클러스터 ID") @RequestParam(value = "clusterId", required = false) String clusterId,
            @Parameter(name = "useNamespace", description = "Namespace 정보 사용 여부", schema = @Schema(allowableValues = {"true","false"}, defaultValue = "false")) @RequestParam(value = "useNamespace", required = false, defaultValue = "false") boolean useNamespace
    ) throws Exception {
        log.debug("[BEGIN] getProjects");

        List<ServerlessVO> result = serverlessService.getProjects(userId, clusterId, useNamespace);

        log.debug("[END  ] getProjects");

        return result;
    }

    @GetMapping(value = "/project/{projectName:.+}")
    @Operation(summary = "프로젝트 상세 조회", description = "프로젝트를 상세 조회한다.")
    public ServerlessVO getProject(
            @Parameter(name = "projectName", description = "Project Name", required = true) @PathVariable String projectName,
            @Parameter(name = "userId", description = "사용자 ID", required = true) @RequestParam(value = "userId") String userId,
            @Parameter(name = "clusterId", description = "클러스터 ID", required = true) @RequestParam(value = "clusterId") String clusterId,
            @Parameter(name = "useNamespace", description = "Namespace 정보 사용 여부", schema = @Schema(allowableValues = {"true","false"}, defaultValue = "false")) @RequestParam(value = "useNamespace", required = false, defaultValue = "false") boolean useNamespace
    ) throws Exception {
        log.debug("[BEGIN] getProject");

        ServerlessVO result = serverlessService.getProject(userId, clusterId, projectName, useNamespace);

        log.debug("[END  ] getProject");

        return result;
    }

    @DeleteMapping(value = "/project/{projectName:.+}")
    @Operation(summary = "프로젝트 삭제", description = "프로젝트를 삭제한다.")
    public void deleteProject(
            @Parameter(name = "projectName", description = "Project Name", required = true) @PathVariable String projectName,
            @Parameter(name = "userId", description = "사용자 ID", required = true) @RequestParam(value = "userId") String userId,
            @Parameter(name = "clusterId", description = "클러스터 ID", required = true) @RequestParam(value = "clusterId") String clusterId
    ) throws Exception {
        log.debug("[BEGIN] deleteProject");


        serverlessService.removeProject(userId, clusterId, projectName);

        log.debug("[END  ] deleteProject");
    }

//    @PostMapping(value = "/project/{projectName}/function/token")
//    @Operation(summary = "Function token 발급", description = "Function token을 발급한다.")
//    public String issueFunctionToken(
//            @Parameter(name = "projectName", description = "Project Name", required = true) @PathVariable String projectName,
//            @Parameter(description = "서비리스 Function 생성 모델", required = true) @RequestBody @Validated FunctionAddVO function
//    ) throws Exception {
//        log.debug("[BEGIN] issueFunctionToken");
//
//        if (!StringUtils.equals(projectName, function.getProjectName())) {
//            throw new CocktailException("projectName are different from function Parameter.", ExceptionType.InvalidParameter);
//        }
//
//        ServerlessInfoVO serverlessInfo = serverlessService.addFunction(ServerlessType.BAAS, function);
//        String token = null;
//        if (serverlessInfo != null) {
//            token = CryptoUtils.decryptAES(serverlessInfo.getToken());
//        }
//
//        log.debug("[END  ] issueFunctionToken");
//
//        return token;
//    }

    @GetMapping(value = "/project/{projectName}/type/{serverlessType}/function/{functionName}/token")
    @Operation(summary = "Function token 조회", description = "Function token을 조회한다.")
    public String getFunctionToken(
            @Parameter(name = "projectName", description = "Project Name", required = true) @PathVariable String projectName,
            @Parameter(name = "serverlessType", description = "serverlessType", schema = @Schema(allowableValues = {ServerlessType.Names.BAAS, ServerlessType.Names.FAAS}), required = true) @PathVariable String serverlessType,
            @Parameter(name = "functionName", description = "Function Name", required = true) @PathVariable String functionName,
            @Parameter(name = "userId", description = "사용자 ID", required = true) @RequestParam(value = "userId") String userId,
            @Parameter(name = "clusterId", description = "클러스터 ID", required = true) @RequestParam(value = "clusterId") String clusterId
    ) throws Exception {
        log.debug("[BEGIN] getFunctionToken");

        if (!StringUtils.equalsAny(serverlessType, ServerlessType.Names.BAAS, ServerlessType.Names.FAAS)) {
            throw new CocktailException("serverlessType is invalid.", ExceptionType.InvalidParameter);
        }

        ServerlessInfoVO serverlessInfo = serverlessService.getFunction(ServerlessType.valueOf(serverlessType), userId, clusterId, projectName, functionName);
        String token = null;
        if (serverlessInfo != null) {
            token = CryptoUtils.decryptAES(serverlessInfo.getToken());
        }

        log.debug("[END  ] getFunctionToken");

        return token;
    }

    @PutMapping(value = "/project/{projectName}/type/{serverlessType}/function/{functionName}/token")
    @Operation(summary = "Function token 재발급", description = "Function token을 재발급한다.")
    public String reissueFunctionToken(
            @Parameter(name = "projectName", description = "Project Name", required = true) @PathVariable String projectName,
            @Parameter(name = "serverlessType", description = "serverlessType", schema = @Schema(allowableValues = {ServerlessType.Names.BAAS, ServerlessType.Names.FAAS}), required = true) @PathVariable String serverlessType,
            @Parameter(name = "functionName", description = "Function Name", required = true) @PathVariable String functionName,
            @Parameter(description = "서비리스 Function 생성 모델", required = true) @RequestBody @Validated FunctionAddVO function
    ) throws Exception {
        log.debug("[BEGIN] reissueFunctionToken");

        if (!StringUtils.equals(projectName, function.getProjectName())) {
            throw new CocktailException("projectName are different from function Parameter.", ExceptionType.InvalidParameter);
        }

        if (!StringUtils.equalsAny(serverlessType, ServerlessType.Names.BAAS, ServerlessType.Names.FAAS)) {
            throw new CocktailException("serverlessType is invalid.", ExceptionType.InvalidParameter);
        }

        if (!StringUtils.equals(functionName, function.getFunctionName())) {
            throw new CocktailException("functionName are different from function Parameter.", ExceptionType.InvalidParameter);
        }

        ServerlessInfoVO serverlessInfo = serverlessService.mergeFunction(ServerlessType.valueOf(serverlessType), function);
        String token = null;
        if (serverlessInfo != null) {
            token = CryptoUtils.decryptAES(serverlessInfo.getToken());
        }

        log.debug("[END  ] reissueFunctionToken");

        return token;
    }

    @DeleteMapping(value = "/project/{projectName}/type/{serverlessType}/function/{functionName}/token")
    @Operation(summary = "Function token 회수", description = "Function token을 회수한다.")
    public void revokeFunctionToken(
            @Parameter(name = "projectName", description = "Project Name", required = true) @PathVariable String projectName,
            @Parameter(name = "serverlessType", description = "serverlessType", schema = @Schema(allowableValues = {ServerlessType.Names.BAAS, ServerlessType.Names.FAAS}), required = true) @PathVariable String serverlessType,
            @Parameter(name = "functionName", description = "Function Name", required = true) @PathVariable String functionName,
            @Parameter(name = "userId", description = "사용자 ID", required = true) @RequestParam(value = "userId") String userId,
            @Parameter(name = "clusterId", description = "클러스터 ID", required = true) @RequestParam(value = "clusterId") String clusterId
    ) throws Exception {
        log.debug("[BEGIN] revokeFunctionToken");

        if (!StringUtils.equalsAny(serverlessType, ServerlessType.Names.BAAS, ServerlessType.Names.FAAS)) {
            throw new CocktailException("serverlessType is invalid.", ExceptionType.InvalidParameter);
        }

        serverlessService.removeFunction(ServerlessType.valueOf(serverlessType), userId, clusterId, projectName, functionName);

        log.debug("[END  ] revokeFunctionToken");
    }
}