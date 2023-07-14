package run.acloud.api.pipelineflow.controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import run.acloud.api.build.enums.RunState;
import run.acloud.api.build.service.WrapPipelineAsyncService;
import run.acloud.api.build.service.WrapPipelineFlowService;
import run.acloud.api.build.vo.BuildAddVO;
import run.acloud.api.build.vo.BuildRunVO;
import run.acloud.api.build.vo.BuildStepVO;
import run.acloud.api.cserver.service.ServerService;
import run.acloud.api.cserver.service.ServerValidService;
import run.acloud.api.pipelineflow.enums.PipelineRunState;
import run.acloud.api.pipelineflow.enums.PipelineType;
import run.acloud.api.pipelineflow.service.PipelineAPIService;
import run.acloud.api.pipelineflow.service.PipelineFlowService;
import run.acloud.api.pipelineflow.service.PipelineFlowValidationService;
import run.acloud.api.pipelineflow.service.WrappedBuildService;
import run.acloud.api.pipelineflow.vo.PipelineContainerVO;
import run.acloud.api.pipelineflow.vo.PipelineWorkloadVO;
import run.acloud.api.resource.vo.ComponentVO;
import run.acloud.commons.enums.ApiVersionType;
import run.acloud.commons.vo.ExecutingContextVO;
import run.acloud.commons.vo.ResultVO;
import run.acloud.framework.context.ContextHolder;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;

import java.util.*;

@Tag(name = "PipelineFlow", description = "파이프라인 flow 관련 기능을 제공한다.")
@Slf4j
@RestController
@Validated
@RequestMapping(value = "/api/pipelineflow")
public class PipelineFlowController {

	@Autowired
    private PipelineFlowService pipelineFlowService;

	@Autowired
    private WrapPipelineFlowService wrapPipelineFlowService;

	@Autowired
	private WrappedBuildService buildService;

	@Autowired
	private WrapPipelineAsyncService pipelineAsyncService;

	@Autowired
	private ServerService serverService;

	@Autowired
	private ServerValidService serverValidService;

	@Autowired
	private PipelineAPIService pipelineAPIService;

	@Autowired
	private PipelineFlowValidationService pipelineFlowValidationService;

	@GetMapping(value = "/servicemap/{servicemapSeq}")
	@Operation(summary = "파이프라인 목록(appmap)", description = "파이프라인 목록을 반환한다.")
	public List<PipelineWorkloadVO> getPipelineWorkloadsByServicemap(
			@Parameter(name = "user-id", description = "사용자 번호", required = true) @RequestHeader(name = "user-id" ) Integer _userSeq,
			@Parameter(name = "user-role", description = "사용자 권한", required = true) @RequestHeader(name = "user-role" ) String _userRole,
			@Parameter(name = "servicemapSeq", description = "servicemap 번호", required = true) @PathVariable Integer servicemapSeq
	) throws Exception {
		log.debug("[BEGIN] getPipelineWorkloadsByServicemap");

		ExecutingContextVO ctx = new ExecutingContextVO();
		BeanUtils.copyProperties(ContextHolder.exeContext(), ctx);
		ctx.setPipelineYn("Y");

		List<PipelineWorkloadVO> result = pipelineFlowService.getPipelineWorkloadsByServicemap(servicemapSeq, ctx);

		log.debug("[END  ] getPipelineWorkloadsByServicemap");

		return result;
	}

	@PostMapping(value = "/run/{pipelineContainerSeq}")
	@Operation(summary = "파이프라인 build & deploy 작업 실행", description = "빌드 연결된 파이프라인 에서만 호출 가능하다.")
	public PipelineContainerVO runPipelineContainerByBuildRun(
			@Parameter(name = "user-id", description = "사용자 번호", required = true) @RequestHeader(name = "user-id" ) Integer _userSeq,
			@Parameter(name = "user-role", description = "사용자 권한", required = true) @RequestHeader(name = "user-role" ) String _userRole,
			@Parameter(name = "tagName", description = "버전 태그") @RequestParam(required = false) String tagName,
			@Parameter(name = "description", description = "빌드 실행 설명") @RequestParam(required = false) String description,
			@Parameter(name = "pipelineContainerSeq", description = "pipeline container 번호", required = true) @PathVariable Integer pipelineContainerSeq
	) throws Exception {
		log.debug("[BEGIN] runPipelineContainerByBuildRun");

		ExecutingContextVO ctx = new ExecutingContextVO();
		BeanUtils.copyProperties(ContextHolder.exeContext(), ctx);
		ctx.setPipelineYn("Y");

		String callbackUrl = pipelineFlowService.genPipelineCallbackUrl(ctx); // callbackurl 생성

		// pipeline validation check
		PipelineContainerVO pipelineContainer = pipelineFlowService.checkBuildRunPipelineValidation(pipelineContainerSeq);

		//빌드 실행 정보 생성 및 리턴
		BuildRunVO buildRun = buildService.createRunBuildByBuildRun( pipelineContainer.getBuildSeq(), pipelineContainer.getBuildRunSeq(), callbackUrl, tagName, description, pipelineContainer.getPipelineContainerSeq());

		// 파이프라인 실행 정보 생성 및 pipeline Container 정보 수정
		pipelineContainer = pipelineFlowService.editPipelinesByRun(pipelineContainerSeq, buildRun, ctx);

		// 파이프라인 실행
		pipelineContainer = pipelineAPIService.runPipeline(pipelineContainer);

		// Pipeline 상태 update
		pipelineFlowService.updatePipelineBuildState(pipelineContainer);

		// DB update(build) and event send
		buildRun.setRunState(RunState.valueOf(pipelineContainer.getBuildState().getCode())); // pipeline run 상태와 build run 상태가 Name 동일
		buildService.updateStateAndSendEvent(buildRun);

		// return을 위한 pipeline 조회
		pipelineContainer = pipelineFlowService.getPipelineContainer(pipelineContainer.getPipelineContainerSeq());

		log.debug("[END  ] runPipelineContainerByBuildRun");

		return pipelineContainer;
	}

	@Operation(summary = "파이프라인 취소 작업 실행", description = "빌드 실행중일 때만 가능하다.")
	@PutMapping(value = "/cancel/{pipelineContainerSeq}")
	public PipelineContainerVO cancelPipelineContainer(
			@Parameter(name = "user-id", description = "사용자 번호", required = true) @RequestHeader(name = "user-id" ) Integer _userSeq,
			@Parameter(name = "user-role", description = "사용자 권한", required = true) @RequestHeader(name = "user-role" ) String _userRole,
			@Parameter(name = "pipelineContainerSeq", description = "pipeline container 번호", required = true) @PathVariable Integer pipelineContainerSeq,
			@Parameter(name = "callbackUrl", description = "작업완료시 callback URL", required = true) @RequestParam String callbackUrl
	) throws Exception {
		log.debug("[BEGIN] cancelPipelineContainer");

		ExecutingContextVO ctx = new ExecutingContextVO();
		BeanUtils.copyProperties(ContextHolder.exeContext(), ctx);
		ctx.setPipelineYn("Y");

		// callback url 존재 하지 않을 경우에, callback URL 생성
		if (callbackUrl == null) {
			callbackUrl = pipelineFlowService.genPipelineCallbackUrl(ctx);
		}

		// pipeline 취소 validation check
		PipelineContainerVO pipelineContainer = pipelineFlowService.checkCancelPipelineValidation(pipelineContainerSeq);

		// 빌드 실행정보 조회 및 체크
		BuildRunVO buildRun = buildService.getBuildRun(pipelineContainer.getBuildRunSeq());

		// 정상적인 경우는 모두 이 부분이 처리 된다.
		if (buildRun.getRunState() == RunState.RUNNING){
			// 빌드 취소 생성
			BuildRunVO cancleBuildRun = buildService.createBuildRunByBuildCancel(buildRun.getBuildRunSeq(), callbackUrl);
			// 빌드 취소 호출
			pipelineAsyncService.processPipelineService(cancleBuildRun);

		} else if( EnumSet.of(RunState.ERROR, RunState.DONE).contains(buildRun.getRunState()) && pipelineContainer.getBuildState() == PipelineRunState.RUNNING){
			// Pipeline 상태 update
			pipelineContainer.setBuildState(PipelineRunState.ERROR);
			pipelineFlowService.updatePipelineBuildState(pipelineContainer);

			pipelineContainer.setDeployState(PipelineRunState.DONE);
			pipelineFlowService.updatePipelineDeployState(pipelineContainer);
		}

		// return을 위한 pipeline 조회
		pipelineContainer = pipelineFlowService.getPipelineContainer(pipelineContainerSeq);

		log.debug("[END  ] cancelPipelineContainer");

		return pipelineContainer;
	}

	@Operation(summary = "빌드 이력 상세에서 빌드 실행", description = "빌드 이력상세에서 재실행 버튼 클릭시 실행")
	@PostMapping(value = "/rerun/{pipelineContainerSeq}")
	public PipelineContainerVO runPipelineContainerByBuildRunModify(
			@Parameter(name = "pipelineContainerSeq", description = "파이프라인컨테이너번호", required = true) @PathVariable Integer pipelineContainerSeq,
			@Parameter(name = "tagName", description = "버전 태그") @RequestParam(required = false) String tagName,
			@Parameter(name = "description", description = "빌드 실행 설명") @RequestParam(required = false) String description,
			@Parameter(name = "buildAdd", required = true, description = "빌드 수정 모델") @Validated @RequestBody BuildAddVO buildAdd,
			BindingResult result
	) throws Exception {
		log.debug("[BEGIN] runPipelineContainerByBuildRunModify");

		ExecutingContextVO ctx = new ExecutingContextVO();
		BeanUtils.copyProperties(ContextHolder.exeContext(), ctx);
		ctx.setPipelineYn("Y");

		String callbackUrl = pipelineFlowService.genPipelineCallbackUrl(ctx); // callbackurl 생성

		// pipeline validation 체크
		PipelineContainerVO pipelineContainer = pipelineFlowService.checkBuildRunPipelineValidation(pipelineContainerSeq);

		// 화면에서 선택한 tag 의 BuildRunSeq 추출, tags 변수에 선택한 BuildRun의 sequence가 넘어온다.
		Integer tagBuildRunSeq = buildAdd.getTags(); // 새로운 태그생성시에만 존재하고, 이력에서 재실행일 경우는 tags가 안넘어옴.
		if(tagBuildRunSeq == null && CollectionUtils.isNotEmpty(buildAdd.getBuildSteps())){ // 그럴리는 없겟지만 tags 정보가 존재 하지 않을 경우, step 정보중에서 buildRunSeq 를 추출해 사용
		 	Optional<BuildStepVO> optBuildStepVO =buildAdd.getBuildSteps().stream().filter(stepVO -> stepVO.getBuildRunSeq() != null).findAny();
			if(optBuildStepVO.isPresent()) {
				tagBuildRunSeq = optBuildStepVO.get().getBuildRunSeq();
			}else if(pipelineContainer.getDeployBuildRunSeq() != null && pipelineContainer.getDeployBuildRunSeq() > 0){
				tagBuildRunSeq = pipelineContainer.getDeployBuildRunSeq();
			}
		}

		//빌드 실행 정보 생성 및 리턴
		BuildRunVO buildRun = buildService.createBuildRunByBuildRunModify(pipelineContainer.getBuildSeq(), tagBuildRunSeq, callbackUrl, tagName, description, pipelineContainerSeq, buildAdd, result );

		// 파이프라인 실행 정보 생성 및 pipeline Container 정보 수정
		pipelineContainer = pipelineFlowService.editPipelinesByRun(pipelineContainerSeq, buildRun, ctx);

		// 파이프라인 실행
		pipelineContainer = pipelineAPIService.runPipeline(pipelineContainer);

		// Pipeline 상태 update
		pipelineFlowService.updatePipelineBuildState(pipelineContainer);

		// DB update(build) and event send
		buildRun.setRunState(RunState.valueOf(pipelineContainer.getBuildState().getCode())); // pipeline run 상태와 build run 상태가 Name 동일
		buildService.updateStateAndSendEvent(buildRun);

		// return을 위한 pipeline 조회
		pipelineContainer = pipelineFlowService.getPipelineContainer(pipelineContainer.getPipelineContainerSeq());

		log.debug("[END  ] runPipelineContainerByBuildRunModify");

		return pipelineContainer;
	}

	@PostMapping(value = "/change/{pipelineContainerSeq}")
	@Operation(summary = "파이프라인 이미지 변경 작업 실행", description = "파이프라인 이미지 변경, PUBLIC_DEPLOY 일때만 tagName 값이 필수, BUILD_DEPLOY 일때는 BuildRunSeq 필수")
	public PipelineContainerVO runChangeImage(
			@Parameter(name = "pipelineContainerSeq", description = "pipeline container 번호", required = true) @PathVariable Integer pipelineContainerSeq,
			@Parameter(name = "tagName", description = "버전 태그") @RequestParam(required = false) String tagName,
			@Parameter(name = "buildRunSeq", description = "빌드 실행 번호") @RequestParam(required = false) Integer buildRunSeq
	) throws Exception {
		log.debug("[BEGIN] runChangeImage");

		ExecutingContextVO ctx = new ExecutingContextVO();
		BeanUtils.copyProperties(ContextHolder.exeContext(), ctx);
		ctx.setPipelineYn("Y");

		/** pipeline validation check **/
		PipelineContainerVO pipelineContainer = pipelineFlowService.getPipelineContainer(pipelineContainerSeq); // pipeline 정보조회
		PipelineWorkloadVO pipelineWorkload = pipelineFlowService.getPipelineWorkload(pipelineContainer.getPipelineWorkloadSeq());

		// component 상태 체크
		Integer clusterSeq = pipelineWorkload.getClusterSeq();
		String namespaceName = pipelineWorkload.getNamespaceName();
		String workloadName = pipelineWorkload.getWorkloadName();

		// workload 존재하는 지 체크
		if (!serverValidService.checkServerIfExists(clusterSeq, namespaceName, workloadName, true, false)) {
			throw new CocktailException("does not Exists workload!!!", ExceptionType.ServerNotFound, pipelineWorkload);
		}

		// buildSeq 체크
		if(pipelineContainer.getPipelineType() == PipelineType.BUILD_DEPLOY){
			//빌드 실행 정보 생성 및 리턴
			BuildRunVO buildRun = buildService.getBuildRun(buildRunSeq);

			// build check
			if( buildRun == null || !pipelineContainer.getBuildSeq().equals(buildRun.getBuildSeq()) ){
				throw new CocktailException("BuildSeq not Match!!!", ExceptionType.PipelineRunningFail, pipelineContainer);
			}
		}

		// 파이프라인 실행중인지 체크
		if(pipelineFlowService.checkPipelineOnRunning(pipelineWorkload)){
			throw new CocktailException("Pipeline is running!!", ExceptionType.PipelineRunning);
		}

		// 배포위한 pipelineContainer 데이터 생성
		PipelineContainerVO deployPipelineContainer = pipelineFlowService.runChangeImage(pipelineContainerSeq, buildRunSeq, tagName, ctx);

		//TODO pipeline server를 통한 deploy 배포는 추후 진행
		//pipelineContainer = pipelineAPIService.runDeployPipeline(pipelineContainer);

		// 호출전에 running 상태처리
		deployPipelineContainer.setDeployState(PipelineRunState.RUNNING);
		pipelineFlowService.updatePipelineDeployState(deployPipelineContainer);

		// workload 배포
		deployPipelineContainer = pipelineFlowService.deployByPipeline(pipelineWorkload, deployPipelineContainer, ctx);

		// 배포 오류일 경우 deploy 정보를 이전 배포정보로 원복
		if(deployPipelineContainer.getDeployState() == PipelineRunState.ERROR){
			if(deployPipelineContainer.getPipelineType() == PipelineType.BUILD_DEPLOY){
				deployPipelineContainer.setDeployBuildRunSeq(pipelineContainer.getDeployBuildRunSeq());
				deployPipelineContainer.setDeployRegistryName(pipelineContainer.getDeployRegistryName());
				deployPipelineContainer.setDeployRegistrySeq(pipelineContainer.getDeployRegistrySeq());
				deployPipelineContainer.setDeployImageName(pipelineContainer.getDeployImageName());
				deployPipelineContainer.setDeployImageTag(pipelineContainer.getDeployImageTag());
				deployPipelineContainer.setDeployImageUrl(pipelineContainer.getDeployImageUrl());

			} else if(deployPipelineContainer.getPipelineType() == PipelineType.PUBLIC_DEPLOY){
				deployPipelineContainer.setDeployImageTag(pipelineContainer.getDeployImageTag());
				deployPipelineContainer.setDeployImageUrl(pipelineContainer.getDeployImageUrl());
			}
		}

		// deploy done or Error 상태 처리
		pipelineFlowService.updatePipelineDeployState(deployPipelineContainer);

		// deploy 정보 update 처리
		pipelineFlowService.updatePipelineDeploy(deployPipelineContainer);

		log.debug("[END  ] runChangeImage");

		return deployPipelineContainer;
	}

	@PostMapping(value = "/rollback/{pipelineContainerSeq}")
	@Operation(summary = "파이프라인 빌드 이미지 작업 rollback", description = "빌드 오류일 경우 사용자가 되돌리기 버튼으로 배포된 정보로 build image 정보를 update 한다. BUILD_DEPLOY 일 경우만 가능")
	public PipelineContainerVO rollbackBuildImage(
			@Parameter(name = "user-id", description = "사용자 번호", required = true) @RequestHeader(name = "user-id" ) Integer _userSeq,
			@Parameter(name = "user-role", description = "사용자 권한", required = true) @RequestHeader(name = "user-role" ) String _userRole,
			@Parameter(name = "pipelineContainerSeq", description = "pipeline container 번호", required = true) @PathVariable Integer pipelineContainerSeq
	) throws Exception {
		log.debug("[BEGIN] rollbackBuildImage");

		ExecutingContextVO ctx = new ExecutingContextVO();
		BeanUtils.copyProperties(ContextHolder.exeContext(), ctx);
		ctx.setPipelineYn("Y");

		PipelineContainerVO pipelineContainer = pipelineFlowService.getPipelineContainer(pipelineContainerSeq); // pipeline 정보조회

		PipelineWorkloadVO pipelineWorkload = pipelineFlowService.getPipelineWorkload(pipelineContainer.getPipelineWorkloadSeq());

		// component 상태 체크
		ComponentVO component = serverService.getComponent(pipelineWorkload.getClusterSeq(), pipelineWorkload.getNamespaceName(), pipelineWorkload.getWorkloadName());
		if(component == null){
			throw new CocktailException("does not Exists Server!!!", ExceptionType.ServerNotFound, component);
		}

		// 파이프라인 실행중인지 체크
		if(pipelineFlowService.checkPipelineOnRunning(pipelineWorkload)){
			throw new CocktailException("Pipeline is running!!", ExceptionType.PipelineRunning);
		}

		pipelineContainer = pipelineFlowService.rollbackBuildImage(pipelineContainerSeq, ctx);

		log.debug("[END  ] rollbackBuildImage");

		return pipelineContainer;
	}

	@PostMapping(value = "/run/result/{pipelineContainerSeq}")
	@Operation(summary = "파이프라인 실행 결과 처리")
	public void handleResult(
			@Parameter(name = "pipelineContainerSeq", description = "pipeline container 번호", required = true) @PathVariable Integer pipelineContainerSeq,
			HttpServletRequest request
	) throws Exception {
		log.debug("[BEGIN] handleResult");

		ExecutingContextVO ctx = new ExecutingContextVO();
		BeanUtils.copyProperties(ContextHolder.exeContext(), ctx);
		ctx.setPipelineYn("Y");

		JsonObject jsonObject = new Gson().fromJson(request.getReader(), JsonObject.class);
		String pipelineResult = jsonObject.toString();

		log.debug("the call data from pipeline server : \n {}", jsonObject);

		// pipeline handle 처리
		pipelineFlowService.handleResult(pipelineContainerSeq, pipelineResult, ctx);

		log.debug("[END  ] handleResult");

	}

	@GetMapping(value = "/build/{buildSeq}/check")
	@Operation(summary = "파이프라인에서 해당 빌드를 사용하는지 체크")
	public ResultVO checkPipelineUsingBuildTask(
			@Parameter(name = "user-id", description = "사용자 번호", required = true) @RequestHeader(name = "user-id" ) Integer _userSeq,
			@Parameter(name = "user-role", description = "사용자 권한", required = true) @RequestHeader(name = "user-role" ) String _userRole,
			@Parameter(name = "buildSeq", description = "빌드 번호") @PathVariable Integer buildSeq
	) throws Exception {
		log.debug("[BEGIN] checkPipelineUsingBuildTask");

		ExecutingContextVO ctx = new ExecutingContextVO();
		BeanUtils.copyProperties(ContextHolder.exeContext(), ctx);
		ctx.setPipelineYn("Y");

		boolean isUsed = wrapPipelineFlowService.checkPipelineUsingBuild(buildSeq, null, null);
		Map<String, Boolean> resultMap = new HashMap<>();
		resultMap.put("isUsed", isUsed);

		ResultVO result = new ResultVO();
		result.setResult(resultMap);

		log.debug("[END  ] checkPipelineUsingBuildTask");

		return result;
	}

	/**
	 * build run 에 해당하는 이미지가 Pipeline에서 사용되는지 체크.<br/>
	 * build history 삭제에서 사용된다.
	 *
	 *  @param buildRunSeq
	 * @return
	 * @throws Exception
	 */
	@GetMapping(value = "/buildrun/{buildRunSeq}/check")
	@Operation(summary = "파이프라인에서 해당 빌드를 사용하는지 체크")
	public ResultVO checkPipelineUsingBuildRun(
			@Parameter(name = "user-id", description = "사용자 번호", required = true) @RequestHeader(name = "user-id" ) Integer _userSeq,
			@Parameter(name = "user-role", description = "사용자 권한", required = true) @RequestHeader(name = "user-role" ) String _userRole,
			@Parameter(name = "buildRunSeq", description = "빌드 실행 번호") @PathVariable Integer buildRunSeq
	) throws Exception {
		log.debug("[BEGIN] checkPipelineUsingBuildRun");

		ExecutingContextVO ctx = new ExecutingContextVO();
		BeanUtils.copyProperties(ContextHolder.exeContext(), ctx);
		ctx.setApiVersionType(ApiVersionType.V2);
		ctx.setPipelineYn("Y");

		boolean isUsed = wrapPipelineFlowService.checkPipelineUsingBuild(null, buildRunSeq, null);
		Map<String, Boolean> resultMap = new HashMap<>();
		resultMap.put("isUsed", isUsed);

		ResultVO result = new ResultVO();
		result.setResult(resultMap);

		log.debug("[END  ] checkPipelineUsingBuildRun");

		return result;
	}

}
