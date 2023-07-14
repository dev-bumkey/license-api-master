package run.acloud.api.cserver.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import run.acloud.api.cserver.service.ComponentService;
import run.acloud.api.cserver.service.WorkloadGroupService;
import run.acloud.api.cserver.vo.ComponentOrderVO;
import run.acloud.api.cserver.vo.WorkloadGroupVO;
import run.acloud.api.event.service.EventService;
import run.acloud.api.resource.vo.ComponentVO;
import run.acloud.commons.vo.ExecutingContextVO;

@Deprecated
@Tag(name = "Component", description = "컴포넌트 관련 기능을 제공한다.")
@Slf4j
@RestController
@RequestMapping(value = "/api/component")
public class ComponentController {
        
    @Autowired
    private ComponentService componentService;

    @Autowired
    private WorkloadGroupService workloadGroupService;

    @Autowired
    private EventService eventService;

    @Operation(summary = "컴포넌트 조회")
    @GetMapping("/{componentSeq}")
    public ComponentVO getComponent(@PathVariable Integer componentSeq) throws Exception {
        return this.componentService.getComponent(componentSeq);
    }

    @Operation(summary = "컴포넌트 순서 수정")
    @PutMapping("/{componentSeq}/order")
    public ComponentVO updateComponentOrder(
            @RequestHeader(name = "user-id" ) Integer userSeq,
    		@Parameter(description = "컴포넌트 번호") @PathVariable Integer componentSeq,
    		@Parameter(description = "컴포넌트순서 모델") @RequestBody @Validated ComponentOrderVO componentOrder
    		) throws Exception {
        log.debug("[BEGIN] updateComponentOrder");

        ExecutingContextVO ctx = new ExecutingContextVO();
        ctx.setUserSeq(userSeq);

        ComponentVO component = new ComponentVO();
        component.setComponentSeq(componentSeq);
        component.setWorkloadGroupSeq(componentOrder.getWorkloadGroupSeq());
        component.setSortOrder(componentOrder.getSortOrder());
        component.setUpdater(userSeq);

        componentService.updateComponentOrder(component, ctx);

        WorkloadGroupVO workloadGroup = this.workloadGroupService.getWorkloadGroup(component.getWorkloadGroupSeq());

        // 이벤트 처리
        this.eventService.getInstance().sendSerivcemapServers(workloadGroup.getServicemapSeq(), ctx);

        log.debug("[END  ] updateComponentOrder");

        return component;
    }

    @Operation(summary = "컴포넌트 수정")
    @PutMapping("/{componentSeq}")
    public ComponentVO updateComponent(@RequestHeader(name = "user-id" ) Integer userSeq,
                                       @Parameter(description = "컴포넌트 번호") @PathVariable Integer componentSeq,
                                       @RequestBody @Validated ComponentVO component) throws Exception {
        log.debug("[BEGIN] updateComponent");

        component.setComponentSeq(componentSeq);
        component.setUpdater(userSeq);

        this.componentService.updateComponent(component);

        ExecutingContextVO ctx = new ExecutingContextVO();
        ctx.setUserSeq(userSeq);
        WorkloadGroupVO workloadGroup = this.workloadGroupService.getWorkloadGroup(component.getWorkloadGroupSeq());

        // 이벤트 처리
        this.eventService.getInstance().sendSerivcemapServers(workloadGroup.getServicemapSeq(), ctx);

        log.debug("[END  ] updateComponent");

        return component;
    }
}
