package run.acloud.api.cserver.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import run.acloud.api.cserver.service.ServicemapGroupService;
import run.acloud.api.cserver.vo.ServicemapGroupAddVO;
import run.acloud.api.cserver.vo.ServicemapGroupVO;
import run.acloud.commons.util.JsonUtils;
import run.acloud.framework.context.ContextHolder;

import java.util.List;

/**
 * @author hjchoi@acornsoft.io
 * Created on 2018. 11. 12.
 */
@Tag(name = "Servicemap Group", description = "서비스맵 그룹에 대한 관리 기능을 제공한다.")
@Slf4j
@RestController
@RequestMapping(value = "/api/servicemapgroup")
public class ServicemapGroupController {
	@Autowired
	private ServicemapGroupService servicemapGroupService;

    @GetMapping(value = "/service/{serviceSeq}/list")
    @Operation(summary = "워크스페이스 > 서비스맵 그룹 목록", description = "워크스페이스 > 서비스맵 그룹 목록 조회한다.")
    public List<ServicemapGroupVO> getServicemapGroupsOfService(
            @Parameter(name = "serviceSeq", description = "service seq", required = true) @PathVariable Integer serviceSeq
    ) throws Exception {
        log.debug("[BEGIN] getServicemapGroupsOfService");

        List<ServicemapGroupVO> result = servicemapGroupService.getServicemapGroupsOfService(serviceSeq);

        log.debug("[END  ] getServicemapGroupsOfService");

        return result;
    }

    @GetMapping(value = "/{servicemapGroupSeq}")
    @Operation(summary = "서비스맵 그룹 상세", description = "서비스맵 그룹 상세 조회한다.")
    public ServicemapGroupVO getServicemapGroup(
            @Parameter(name = "servicemapGroupSeq", description = "servicemapGroupSeq", required = true) @PathVariable(name = "servicemapGroupSeq") Integer servicemapGroupSeq
    ) throws Exception {
        log.debug("[BEGIN] getServicemapGroup");

        ServicemapGroupVO result = servicemapGroupService.getServicemapGroup(servicemapGroupSeq, ContextHolder.exeContext().getUserServiceSeq());

        log.debug("[END  ] getServicemapGroup");

        return result;
    }

    @PostMapping("")
    @Operation(summary = "서비스맵 그룹 생성", description = "서비스맵 그룹 생성한다.")
    public ServicemapGroupVO addServicemapGroup(
            @Parameter(name = "servicemapGroupAdd", description = "서비스맵 그룹 생성 모델", required = true) @RequestBody @Validated ServicemapGroupAddVO servicemapGroupAdd
    ) throws Exception {
    	log.debug("[BEGIN] addServicemapGroup");
    	
    	log.debug("[PARAM] {}", JsonUtils.toPrettyString(servicemapGroupAdd));
        servicemapGroupAdd.setCreator(ContextHolder.exeContext().getUserSeq());
        servicemapGroupAdd.setUpdater(ContextHolder.exeContext().getUserSeq());
        servicemapGroupService.addServicemapGroup(servicemapGroupAdd);

        ServicemapGroupVO result = servicemapGroupService.getServicemapGroup(servicemapGroupAdd.getServicemapGroupSeq());
    	
        log.debug("[END  ] addServicemapGroup");

        return result;
    }

	@DeleteMapping("/{servicemapGroupSeq}")
    @Operation(summary = "서비스맵 그룹 삭제", description = "서비스맵 그룹 삭제한다.")
    public void removeServicemapGroup(
            @Parameter(description = "서비스맵그룹순번", required = true) @PathVariable Integer servicemapGroupSeq
    ) throws Exception {
		log.debug("[BEGIN] removeServicemapGroup");

        servicemapGroupService.removeServicemapGroup(servicemapGroupSeq, ContextHolder.exeContext().getUserSeq());

        log.debug("[END  ] removeServicemapGroup");
	}

	@PutMapping("/{servicemapGroupSeq}")
    @Operation(summary = "서비스맵 그룹 수정", description = "서비스맵 그룹 수정한다.")
    public ServicemapGroupVO updateServicemapGroup(
            @Parameter(description = "서비스맵그룹순번", required = true) @PathVariable Integer servicemapGroupSeq,
            @Parameter(name = "servicemapGroup", description = "서비스맵 그룹 모델", required = true) @RequestBody ServicemapGroupVO servicemapGroup
    ) throws Exception {
		log.debug("[BEGIN] updateServicemapGroup");

        servicemapGroup.setUpdater(ContextHolder.exeContext().getUserSeq());
        servicemapGroup.setServicemapGroupSeq(servicemapGroupSeq);
        servicemapGroupService.updateServicemapGroup(servicemapGroup);

        log.debug("[END  ] updateServicemapGroup");

        return servicemapGroup;
	}
}
