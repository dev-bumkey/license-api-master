package run.acloud.api.build.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import run.acloud.api.auth.enums.UserRole;
import run.acloud.api.auth.vo.UserVO;
import run.acloud.api.build.service.BuildServerService;
import run.acloud.api.build.vo.BuildServerAddVO;
import run.acloud.api.build.vo.BuildServerVO;
import run.acloud.api.configuration.service.UserService;
import run.acloud.commons.util.AuthUtils;
import run.acloud.commons.vo.ResultVO;
import run.acloud.framework.context.ContextHolder;

import java.util.ArrayList;
import java.util.List;

@Tag(name = "Build Server", description = "빌드 서버 관련 기능을 제공한다.")
@Slf4j
@RestController
@Validated
@RequestMapping(value = "/api/build/server")
public class BuildServerController {

    @Autowired
    private BuildServerService buildServerService;

    @Autowired
    private UserService userService;

    @GetMapping(value = "")
    @Operation(summary = "빌드 서버 목록", description = "빌드 서버 목록을 반환한다.")
    public List<BuildServerVO> getBuildServerList(
            @Parameter(name = "accountSeq", description = "계정 번호", required = false) @RequestParam(required = false) Integer accountSeq,
            @Parameter(name = "serviceSeq", description = "워크스페이스 번호", required = false) @RequestParam(required = false) Integer serviceSeq,
            @Parameter(name = "topicName", description = "topicName", required = false) @RequestParam(required = false) String topicName
    ) throws Exception {
        log.debug("[BEGIN] getBuildServerList");

        /**
         * header 정보로 요청 사용자 권한 체크
         */
        AuthUtils.isValid(ContextHolder.exeContext(), AuthUtils.checkUserSysUserNDevOpsBlockAuthPredicate());

        List<BuildServerVO> result = new ArrayList<>();

        // accountSeq, serviceSeq 모두 없으면 빈값 리턴
        if( accountSeq == null && serviceSeq == null){
            return result;
        }
        if(!UserRole.valueOf(ContextHolder.exeContext().getUserRole()).isAdmin()) {
            UserVO user = userService.getByUserSeq(ContextHolder.exeContext().getUserSeq());
            if (!user.getAccount().getAccountSeq().equals(accountSeq)) {
                return null;
            }
        }

        if (serviceSeq != null && serviceSeq > 0) {
            result = buildServerService.getBuildServerListByWorkspace(serviceSeq, topicName);
        } else {
            result = buildServerService.getBuildServerList(accountSeq, topicName);
        }

        log.debug("[END  ] getBuildServerList");
        return result;
    }

    @GetMapping(value = "/{buildServerSeq:.+}")
    @Operation(summary = "빌드 서버 상세", description = "빌드 서버 상세정보를 반환한다.")
    public BuildServerVO getBuildServer(
            @Parameter(name = "buildServerSeq", description = "빌드 서버 번호", required = true) @PathVariable Integer buildServerSeq,
            @Parameter(name = "accountSeq", description = "계정 번호", required = false) @RequestParam(required = false) Integer accountSeq
    ) throws Exception {
        log.debug("[BEGIN] getBuildServer");

        /**
         * header 정보로 요청 사용자 권한 체크
         */
        AuthUtils.isValid(ContextHolder.exeContext(), AuthUtils.checkUserSysUserNDevOpsBlockAuthPredicate());

        BuildServerVO buildServer = buildServerService.getBuildServer(buildServerSeq, true);

        if (buildServer != null) {
            // ADMIN이 아닐때, 빌드의 시스템과 조회하는 사람의 시스템이 다를경우 null 리턴, 해당 시스템의 사용자만 빌드 조회 가능
            if (!UserRole.valueOf(ContextHolder.exeContext().getUserRole()).isAdmin()) {
                if (!buildServer.getAccountSeq().equals(ContextHolder.exeContext().getUserAccountSeq())) {
                    return null;
                }
            }
        }

        log.debug("[END  ] getBuildServer");
        return buildServer;
    }

    @PostMapping(value = "")
    @Operation(summary = "빌드 생성", description = "새로운 빌드를 생성한다.")
    public BuildServerVO addBuildServer(
            @Parameter(name = "buildServerAdd", required = true) @Validated @RequestBody BuildServerAddVO buildServerAdd,
            BindingResult result
    ) throws Exception {

        log.debug("[BEGIN] addBuildServer");

        /**
         * header 정보로 요청 사용자 권한 체크
         */
        AuthUtils.isValid(ContextHolder.exeContext(), AuthUtils.checkUserSysUserNDevOpsBlockAuthPredicate());

        // 생성
        BuildServerVO build = buildServerService.addBuildServer(buildServerAdd);

        log.debug("[END  ] addBuildServer");

        return build;
    }

    @PutMapping(value = "/{buildServerSeq:.+}")
    @Operation(summary = "빌드 서버 수정", description = "빌드 서버를 수정한다.")
    public ResultVO editBuildServer(
            @Parameter(name = "buildServerSeq", description = "빌드 서버 번호", required = true) @PathVariable Integer buildServerSeq,
            @Parameter(name = "buildServerAdd", required = true, description = "빌드 서버 수정 모델") @Validated @RequestBody BuildServerAddVO buildServerAdd,
            BindingResult result
    ) throws Exception {

        log.debug("[BEGIN] editBuildServer");

        // set buildServerSeq
        buildServerAdd.setBuildServerSeq(buildServerSeq);

        // 수정
        buildServerService.editBuildServer(buildServerAdd);

        log.debug("[END  ] editBuildServer");

        return new ResultVO();
    }

    @DeleteMapping(value = "/{buildServerSeq:.+}")
    @Operation(summary = "빌드 서버 삭제", description = "빌드 서버를 삭제한다.")
    public BuildServerVO removeBuildServer(
            @Parameter(name = "buildServerSeq", description = "빌드 서버 번호", required = true) @PathVariable Integer buildServerSeq
    ) throws Exception {
        log.debug("[BEGIN] removeBuildServer");

        BuildServerVO result = buildServerService.removeBuildServer(buildServerSeq);

        log.debug("[END  ] removeBuildServer");

        return result;
    }

}
