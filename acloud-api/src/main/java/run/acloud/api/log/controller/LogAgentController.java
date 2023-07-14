package run.acloud.api.log.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import run.acloud.api.log.service.LogAgentService;
import run.acloud.api.log.vo.LogAgentVO;
import run.acloud.api.log.vo.LogAgentViewVO;
import run.acloud.commons.util.AuthUtils;
import run.acloud.framework.context.ContextHolder;

import java.util.List;

@Tag(name = "Log Agent", description = "Log Agent에 대한 관리 기능을 제공한다.")
@Slf4j
@RestController
@RequestMapping(value = "/api/log")
public class LogAgentController {
    private final LogAgentService logAgentService;

    public LogAgentController(LogAgentService logAgentService) {
        this.logAgentService = logAgentService;
    }

    @GetMapping(value = "/agent")
    @Operation(summary = "Log Agent 목록 조회", description = "Log Agent 목록을 조회한다.")
    public List<LogAgentVO> getLogAgentList(
            @Parameter(name = "accountSeq", description = "계정 번호", required = false) @RequestParam(required = false) Integer accountSeq
    ) throws Exception {

        log.debug("[BEGIN] getLogAgentList");

        AuthUtils.isValid(ContextHolder.exeContext(), AuthUtils.checkUserSysUserNDevOpsBlockAuthPredicate());
        List<LogAgentVO> result = logAgentService.getLogAgentList();

        log.debug("[END  ] getLogAgentList");

        return result;
    }

    @PostMapping(value = "/agent")
    @Operation(summary = "Log Agent 등록", description = "Log Agent를 등록한다.")
    public void addLogAgent(@Parameter(name = "logAgentAdd", required = true) @Validated @RequestBody LogAgentViewVO logAgentView) throws Exception {

        log.debug("[BEGIN] addLogAgent");

        AuthUtils.isValid(ContextHolder.exeContext(), AuthUtils.checkUserSysUserNDevOpsBlockAuthPredicate());
        logAgentService.addLogAgent(logAgentView);

        log.debug("[END  ] addLogAgent");
    }

    @GetMapping(value = "/agent/{agentSeq}")
    @Operation(summary = "Log Agent 상세", description = "Log Agent 상세정보를 조회한다.")
    public LogAgentViewVO getLogAgent(
            @Parameter(name = "accountSeq", description = "계정 번호", required = false) @RequestParam(required = false) Integer accountSeq,
            @Parameter(name = "agentSeq", required = true) @PathVariable Integer agentSeq
    ) throws Exception {
        log.debug("[BEGIN] getLogAgent");

        AuthUtils.isValid(ContextHolder.exeContext(), AuthUtils.checkUserSysUserNDevOpsBlockAuthPredicate());
        LogAgentViewVO result = logAgentService.getLogAgentView(agentSeq);

        log.debug("[END  ] getLogAgent");

        return result;
    }

    @PutMapping(value = "/agent/{agentSeq}")
    @Operation(summary = "Log Agent 수정", description = "Log Agent 정보를 수정한다.")
    public void editLogAgent(
            @Parameter(name = "agentSeq", required = true) @PathVariable Integer agentSeq,
            @Validated @RequestBody LogAgentViewVO logAgentView
    ) throws Exception {
        log.debug("[BEGIN] editLogAgent");

        AuthUtils.isValid(ContextHolder.exeContext(), AuthUtils.checkUserSysUserNDevOpsBlockAuthPredicate());
        logAgentService.editLogAgent(agentSeq, logAgentView);

        log.debug("[END  ] editLogAgent");
    }

    @DeleteMapping(value = "/agent/{agentSeq}")
    @Operation(summary = "Log Agent 삭제", description = "Log Agent를 삭제한다.")
    public LogAgentVO removeLogAgent(
            @Parameter(name = "accountSeq", description = "계정 번호", required = false) @RequestParam(required = false) Integer accountSeq,
            @Parameter(name = "agentSeq", required = true) @PathVariable Integer agentSeq
    ) throws Exception {
        log.debug("[BEGIN] removeLogAgent");

        AuthUtils.isValid(ContextHolder.exeContext(), AuthUtils.checkUserSysUserNDevOpsBlockAuthPredicate());
        LogAgentVO result = logAgentService.removeLogAgent(agentSeq);

        log.debug("[END  ] removeLogAgent");

        return result;
    }

    @GetMapping(value = "/token")
    @Operation(summary = "Log Agent 토큰 목록 조회", description = "Log Agent 토큰 목록을 조회한다.")
    public List<String> getTokenList(
            @Parameter(name = "accountSeq", description = "계정 번호", required = false) @RequestParam(required = false) Integer accountSeq
    ) throws Exception {
        log.debug("[BEGIN] getTokenList");

        AuthUtils.isValid(ContextHolder.exeContext(), AuthUtils.checkUserSysUserNDevOpsBlockAuthPredicate());
        List<String> result = logAgentService.getTokenList();

        log.debug("[END  ] getTokenList");

        return result;
    }

    @GetMapping(value = "/application")
    @Operation(summary = "어플리케이션 목록 조회", description = "어플리케이션 목록을 조회한다.")
    public List<String> getApplicationList(
            @Parameter(name = "accountSeq", description = "계정 번호", required = false) @RequestParam(required = false) Integer accountSeq
    ) throws Exception {
        log.debug("[BEGIN] getApplicationList");

        AuthUtils.isValid(ContextHolder.exeContext(), AuthUtils.checkUserSysUserNDevOpsBlockAuthPredicate());
        List<String> result = logAgentService.getApplicationList();

        log.debug("[END  ] getApplicationList");

        return result;
    }
}
