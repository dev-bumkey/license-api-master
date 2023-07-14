package run.acloud.api.cserver.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import run.acloud.api.cserver.service.WorkloadGroupService;
import run.acloud.api.cserver.vo.WorkloadGroupAddVO;
import run.acloud.api.cserver.vo.WorkloadGroupVO;
import run.acloud.commons.util.JsonUtils;
import run.acloud.commons.vo.ResultVO;

/**
 * @author dy79@acornsoft.io
 * Created on 2017. 2. 1.
 */
@Tag(name = "Workload Group", description = "서비스맵의 워크로드를 그룹으로 관리할 수 있는 기능을 제공한다.")
@Slf4j
@RestController
@RequestMapping(value = "/api/workloadgroup")
public class WorkloadGroupController {
	@Autowired
	private WorkloadGroupService workloadGroupService;

	@PostMapping("")
	@Operation(summary = "워크로드 그룹 생성", description = "서비스맵안에 워크로드 그룹을 생성한다.")
	public WorkloadGroupVO addWorkloadGroup(
			@Parameter(name = "workloadGroupAdd", description = "워크로드 그룹 생성 모델", required = true) @RequestBody @Validated WorkloadGroupAddVO workloadGroupAdd
	) throws Exception {
    	log.debug("[BEGIN] addWorkloadGroup");

    	log.debug("[PARAM] {}", JsonUtils.toPrettyString(workloadGroupAdd));
		workloadGroupService.addWorkloadGroup(workloadGroupAdd);

		WorkloadGroupVO result = workloadGroupService.getWorkloadGroup(workloadGroupAdd.getWorkloadGroupSeq());

        log.debug("[END  ] addWorkloadGroup");

        return result;
    }

	@Operation(summary = "워크로드 그룹 삭제", description = "서비스맵 안의 워크로드 그룹을 제거한다.")
	@DeleteMapping("/{workloadGroupSeq}")
	public void removeWorkloadGroup(
			@Parameter(description = "워크로드그룹순번", required = true) @PathVariable Integer workloadGroupSeq
	) throws Exception {
		log.debug("[BEGIN] removeWorkloadGroup");

		workloadGroupService.removeWorkloadGroup(workloadGroupSeq);

        log.debug("[END  ] removeWorkloadGroup");
	}

	@Operation(summary = "워크로드 그룹 수정", description = "서비스맵안에 워크로드 그룹 정보를 수정한다.")
	@PutMapping("/{workloadGroupSeq}")
	public void updateWorkloadGroup(
			@Parameter(description = "워크로드그룹순번", required = true) @PathVariable Integer workloadGroupSeq,
			@Parameter(name = "workloadGroup", description = "워크로드 그룹 모델", required = true) @RequestBody WorkloadGroupVO workloadGroup
	) throws Exception {
		log.debug("[BEGIN] updateWorkloadGroup");

		workloadGroup.setWorkloadGroupSeq(workloadGroupSeq);
		workloadGroupService.updateWorkloadGroup(workloadGroup);
    	
        log.debug("[END  ] updateWorkloadGroup");
	}

    @Operation(summary = "워크로드 그룹의 k8s namespace 이름을 반환한다.", description="워크로드 그룹이 속해있는 클러스터의 Namespace를 응답한다.")
    @GetMapping("/{workloadGroupSeq}/namespace")
    public ResultVO getNamespaceNameOfWorkloadGroup(
			@Parameter(description = "워크로드그룹순번", required = true) @PathVariable Integer workloadGroupSeq
	) throws Exception {
        ResultVO r = new ResultVO();
        r.setResult(workloadGroupService.getNamespaceOfWorkloadGroup(workloadGroupSeq));
        return r;
    }
}
