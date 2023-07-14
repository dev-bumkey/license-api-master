package run.acloud.api.build.controller;


import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import run.acloud.api.build.enums.RunState;
import run.acloud.api.build.service.PipelineAsyncService;
import run.acloud.api.build.service.PipelineBuildRunService;
import run.acloud.api.build.service.WrapPipelineAPIService;
import run.acloud.api.build.service.WrapPipelineFlowService;
import run.acloud.api.build.util.BuildUtils;
import run.acloud.api.build.vo.*;
import run.acloud.commons.util.Base64Utils;
import run.acloud.commons.util.CompressUtils;
import run.acloud.commons.util.CryptoUtils;
import run.acloud.commons.util.JsonUtils;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;
import run.acloud.framework.properties.CocktailServiceProperties;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "BuildRun", description = "빌드 실행 관련 기능을 제공한다.")
@Slf4j
@RestController
@Validated
@RequestMapping(value = "/api/build/run")
public class PipelineBuildRunController {

    @Autowired
    private PipelineBuildRunService buildRunService;

    @Autowired
    private WrapPipelineFlowService pipelineFlowService;

    @Autowired
    private WrapPipelineAPIService pipelineAPIService;

    @Autowired
    private PipelineAsyncService pipelineAsyncService;

    @Autowired
    private CocktailServiceProperties cocktailServiceProperties;

    @GetMapping(value = "")
    @Operation(summary = "빌드 이력 목록", description = "빌드 이력 목록을 반환한다.")
    public List<BuildRunVO> getBuildRuns(
            @Parameter(name = "buildSeq", description = "build 번호", required = true) @RequestParam Integer buildSeq
    ) throws Exception {
        log.debug("[BEGIN] getBuildRuns");

        List<BuildRunVO> buildRuns = buildRunService.getBuildRuns(buildSeq);

        log.debug("[END  ] getBuildRuns");
        return buildRuns;
    }

    @GetMapping(value = "/latest")
    @Operation(summary = "빌드 최근 목록", description = "빌드 성공한 최근 목록을 반환한다.")
    public List<BuildRunVO> getBuildRunsByLatest(
            @Parameter(name = "accountSeq", description = "계정 번호", required = false) @RequestParam(required = false) Integer accountSeq,
            @Parameter(name = "serviceSeq", description = "Workspace 번호", required = false) @RequestParam(required = false) Integer serviceSeq,
            @Parameter(name = "limitCount", description = "조회 건수", required = false) @RequestParam(required = false) Integer limitCount
    ) throws Exception {
        log.debug("[BEGIN] getBuildRunsByLatest");

        List<BuildRunVO> buildRuns = null;

        // accountSeq, serviceSeq, registryProjectId 모두 없으면 빈값 리턴
        if( accountSeq == null && serviceSeq == null ){
            return buildRuns;
        }

        // limit count default 셋팅, 5
        if(limitCount == null) {
            limitCount = 5;
        }

        buildRuns = buildRunService.getBuildRunsByLatest(accountSeq, serviceSeq, limitCount);

        log.debug("[END  ] getBuildRunsByLatest");
        return buildRuns;
    }

    @GetMapping(value = "/{buildRunSeq:.+}")
    @Operation(summary = "빌드 이력 상세", description = "빌드 이력 상세정보를 반환한다.")
    public BuildRunVO getBuildRun(
            @Parameter(name = "buildRunSeq", description = "빌드 실행 번호", required = true) @PathVariable Integer buildRunSeq
    ) throws Exception {
        log.debug("[BEGIN] getBuildRun");
        BuildRunVO buildRun = buildRunService.getBuildRun(buildRunSeq);

//        /** pipeline 테이블 조회하여 워크로드 맵핑된 정보일 경우, Namespace:워크로드명을 넣어줘야함. **/
//        if(buildRun != null) {
//            buildRunService.setPipelineInfoToBuildRuns(buildRun.getBuildSeq(), Arrays.asList(buildRun));
//        }

        log.debug("[END  ] getBuildRun");
        return buildRun;
    }

    @GetMapping(value = "/search")
    @Operation(summary = "image url로 빌드 이력 조회", description = "image full url로 해당하는 빌드 이력 정보를 조회 한다.")
    public BuildRunVO getBuildRunsByImageUrl(
            @Parameter(name = "accountSeq", description = "계정 번호", required = false) @RequestParam(required = false) Integer accountSeq,
            @Parameter(name = "serviceSeq", description = "Workspace 번호", required = false) @RequestParam(required = false) Integer serviceSeq,
            @Parameter(name = "buildSeq", description = "빌드 번호", required = false) @RequestParam(required = false) Integer buildSeq,
            @Parameter(name = "imageUrl", description = "image URL", required = false) @RequestParam String imageUrl
    ) throws Exception {
        log.debug("[BEGIN] getBuildRunsByImageUrl");
        BuildRunVO result = null;

        result = buildRunService.getBuildRunsByImageUrl(accountSeq, serviceSeq, buildSeq, imageUrl);

        log.debug("[END  ] getBuildRunsByImageUrl");
        return result;
    }

    @GetMapping(value = "/log/{buildRunSeq:.+}")
    @Operation(summary = "빌드 실행 로그", description = "빌드 실행 로그를 반환한다.")
    public BuildRunLogVO getBuildAllLog(
            @Parameter(name = "buildRunSeq", description = "빌드 실행 번호", required = true) @PathVariable Integer buildRunSeq
    ) throws Exception {
        log.debug("[BEGIN] getBuildAllLog");
        BuildRunLogVO result = null;

        result = buildRunService.getBuildAllLog(buildRunSeq);

        log.debug("[END  ] getBuildAllLog");
        return result;
    }

    @GetMapping(value = "/log/step/{buildStepRunSeq:.+}")
    @Operation(summary = "빌드 STEP 실행 로그", description = "빌드 실행 로그를 반환한다.")
    public BuildRunLogVO getBuildLog(
            @Parameter(name = "buildStepRunSeq", description = "빌드 STEP 실행 번호", required = true) @PathVariable Integer buildStepRunSeq
    ) throws Exception {
        log.debug("[BEGIN] getBuildLog");

        BuildRunLogVO result = buildRunService.getBuildLog(buildStepRunSeq);

        log.debug("[END  ] getBuildLog");
        return result;
    }

    @Operation(summary = "빌드 실행", description = "빌드 실행하기, 빌드화면에서 빌드실행시 호출됨.")
    @PostMapping(value = "/{buildSeq:.+}")
    public BuildRunVO runBuildByBuild(
            @Parameter(name = "buildSeq", description = "빌드번호", required = true) @PathVariable Integer buildSeq,
            @Parameter(name = "callbackUrl", description = "작업완료시 callback URL", required = true) @RequestParam String callbackUrl,
            @Parameter(name = "tagName", description = "버전 태그") @RequestParam(required = false) String tagName,
            @Parameter(name = "description", description = "빌드 실행 설명") @RequestParam(required = false) String description
    ) throws Exception {
        log.debug("[BEGIN] runBuildByBuild");

        // pipeline 서버 체크
        pipelineAPIService.checkPipelineServer();

        // 빌드실행 정보 생성
        BuildRunVO buildRun = buildRunService.createBuildRun( buildSeq,  callbackUrl, tagName, description, 0);

        // call pipeline server
        buildRun = pipelineAPIService.runBuild(buildRun);

        // update buildRun state
        buildRunService.updateStateAndSendEvent(buildRun);

        log.debug("[END  ] runBuildByBuild");
        return buildRun;
    }

    @Operation(summary = "빌드 이력에서 빌드 실행", description = "빌드 이력에서 빌드 실행하기, 빌드 이력에서 빌드 상세보기 화면에서 재실행 눌렀을때만 호츨됨.")
    @PostMapping(value = "/{buildSeq:.+}/{buildRunSeq:.+}")
    public BuildRunVO runBuildByBuildRun(
            @Parameter(name = "buildSeq", description = "빌드번호", required = true) @PathVariable Integer buildSeq,
            @Parameter(name = "buildRunSeq", description = "빌드실행번호", required = true) @PathVariable Integer buildRunSeq,
            @Parameter(name = "callbackUrl", description = "작업완료시 callback URL", required = true) @RequestParam String callbackUrl,
            @Parameter(name = "tagName", description = "버전 태그") @RequestParam(required = false) String tagName,
            @Parameter(name = "description", description = "빌드 실행 설명") @RequestParam(required = false) String description,
            @Parameter(name = "buildAdd", required = true, description = "빌드 수정 모델") @Validated @RequestBody BuildAddVO buildAdd,
            BindingResult result
    ) throws Exception {
        log.debug("[BEGIN] runBuildByBuildRun");

        // pipeline 서버 체크
        pipelineAPIService.checkPipelineServer();

        // 빌드이력 정보로 실행 정보 생성
        BuildRunVO buildRun = buildRunService.createBuildRunByBuildRunModify(buildSeq, buildRunSeq, callbackUrl, tagName, description, 0, buildAdd, result);

        // call pipeline server
        buildRun = pipelineAPIService.runBuild(buildRun);

        // update buildRun state
        buildRunService.updateStateAndSendEvent(buildRun);

        log.debug("[END  ] runBuildByBuildRun");

        return buildRun;
    }

    @Operation(summary = "빌드 실행 취소", description = "실행중인 빌드를 취소한다.")
    @PutMapping(value = "/cancel/{buildRunSeq}")
    public BuildRunVO cancelBuildRun(
            @Parameter(name = "buildRunSeq", description = "취소할 빌드실행번호", required = true) @PathVariable Integer buildRunSeq,
            @Parameter(name = "callbackUrl", description = "작업완료시 callback URL", required = true) @RequestParam String callbackUrl
    ) throws Exception {
        log.debug("[BEGIN] createBuildRunByBuildCancel");

        // pipeline 서버 체크
        pipelineAPIService.checkPipelineServer();

        // 빌드할 대상조회
        BuildRunVO buildRunVO = buildRunService.getBuildRun(buildRunSeq);

        if(buildRunVO.getRunState() != RunState.RUNNING){
            throw new CocktailException(String.format("Build is not running! [buildSeq : %s, buildRunSeq : %s]", buildRunVO.getBuildSeq(), buildRunVO.getBuildRunSeq()), ExceptionType.InvalidState);
        }

        callbackUrl = BuildUtils.addParamToCallbackURL(callbackUrl); // callbackUrl에 파라메터 추가
        BuildRunVO cancelBuildRun = buildRunService.createBuildRunByBuildCancel(buildRunSeq, callbackUrl); // 빌드 취소용 BuildRun 생성 및 요청

        pipelineAsyncService.processPipelineService(cancelBuildRun);

        log.debug("[END  ] createBuildRunByBuildCancel");

        return cancelBuildRun;
    }

    @Operation(summary = "빌드 실행 삭제시 같이 삭제될 대상 조회")
    @GetMapping(value = "/{buildRunSeq}/otherremoveruns")
    public List<BuildRunVO> getOtherRemoveRuns(
            @Parameter(name = "buildRunSeq", description = "빌드실행번호", required = true) @PathVariable Integer buildRunSeq
    ) throws Exception {
        log.debug("[BEGIN] getOtherRemoveRuns");

        List<BuildRunVO> otherRemoveRuns = buildRunService.getOtherRemoveRunsFromDB(buildRunSeq);

        log.debug("[END  ] getOtherRemoveRuns");

        return otherRemoveRuns;
    }

    @Operation(summary = "빌드 실행 삭제", description = "빌드 실행 내역을 삭제한다.")
    @DeleteMapping(value = "/{buildRunSeq}")
    public BuildRunVO removeBuildRun(
            @Parameter(name = "buildRunSeq", description = "빌드실행번호", required = true) @PathVariable Integer buildRunSeq
    ) throws Exception {
        log.debug("[BEGIN] removeBuildRun");

        // buildRun 정보 조회
        BuildRunVO buildRunVO = buildRunService.getBuildRun(buildRunSeq);

        if(buildRunVO.getRunState() == RunState.RUNNING){
            throw new CocktailException("실행중인 빌드는 삭제할 수 없습니다.", ExceptionType.BuildHistoryOfRunningTaskDeleteFail);
        }

        // 파이프라인에서 사용하는 빌드면 삭제 불가
        if( pipelineFlowService.checkPipelineUsingBuild(null, buildRunSeq,null)
                || ( buildRunVO.getImageSize() > 0 && pipelineFlowService.checkPipelineUsingBuild(null, null, buildRunVO.getImageUrl()) )
        ){
            throw new CocktailException("파이프라인에서 사용중인 빌드는 삭제할 수 없습니다.", ExceptionType.DeleteFailBuildHistoryOfUsingInPipeline);
        }

        // 같이 삭제될 대상 조회
        List<BuildRunVO> otherRemoveRuns = buildRunService.getOtherRemoveRunsFromDB(buildRunVO.getBuildRunSeq());
        otherRemoveRuns.add(buildRunVO);

        // DB 정보 삭제
        Integer removeRunSeq = buildRunService.removeBuildRun(otherRemoveRuns);

        // registry tag image 삭제, imageURL 이 존재하고 내부 레지스트리인 경우만 삭제
        List<BuildRunVO> existImageList = otherRemoveRuns.stream().filter(vo -> StringUtils.isNotEmpty(vo.getImageUrl()) && (vo.getExternalRegistrySeq() == null || vo.getExternalRegistrySeq() == 0) ).collect(Collectors.toList());
        if(CollectionUtils.isNotEmpty(existImageList)) {
            buildRunService.removeRegistryTagImages(existImageList);
        }

        log.debug("[END  ] removeBuildRun");

        return buildRunVO;
    }

    @Operation(summary = "이미지 태그 삭제", description = "이미지 태그을 삭제한다.")
    @DeleteMapping(value = "/{buildSeq:.+}/tag")
    public Integer removeImageTag(
            @Parameter(name = "buildSeq", description = "빌드실행번호", required = true) @PathVariable Integer buildSeq,
            @Parameter(name = "buildRunSeqs", description = "삭제할 이미지 태그의 빌드실행 번호들, 빌드목록 > 이미지 화면에서 체크한 태그의 buildRunSeq 리스트", required = true) @RequestParam List<Integer> buildRunSeqs
    ) throws Exception {
        log.debug("[BEGIN] removeImageTag");

        // 빌드 실행 정보 조회
        List<BuildRunVO> buildRunsToRemove = buildRunService.getBuildRunsByBuildRunSeqs(buildRunSeqs);

        // 이미지 URL 동일한 삭제할 다른 빌드 실행정보 조회
        List<BuildRunVO> otherBuildRunsToRemove = buildRunService.getOtherBuildRunsBySameImageUrl(buildSeq, buildRunSeqs);

        List<BuildRunVO> totalBuildRunsToDelete = new ArrayList<>();
        totalBuildRunsToDelete.addAll(buildRunsToRemove);
        totalBuildRunsToDelete.addAll(otherBuildRunsToRemove);

        if(CollectionUtils.isNotEmpty(totalBuildRunsToDelete)){
            for(BuildRunVO buildRun : totalBuildRunsToDelete){
                if(buildRun.getRunState() == RunState.RUNNING){
                    throw new CocktailException("실행중인 빌드는 삭제할 수 없습니다.", ExceptionType.BuildHistoryOfRunningTaskDeleteFail);
                }
            }
        }

        if(CollectionUtils.isNotEmpty(buildRunsToRemove)){
            for(BuildRunVO buildRun : buildRunsToRemove){
                // 파이프라인에서 사용하는 빌드면 삭제 불가
                if( pipelineFlowService.checkPipelineUsingBuild(null, buildRun.getBuildRunSeq(),null)
                        || ( buildRun.getImageSize() > 0 && pipelineFlowService.checkPipelineUsingBuild(null, null, buildRun.getImageUrl()) )
                ){
                    throw new CocktailException("워크로드에서 사용중인 빌드는 삭제할 수 없습니다.", ExceptionType.DeleteFailBuildHistoryOfUsingInPipeline);
                }
            }
        }

        // DB 정보 삭제
        Integer removeRunSeq = buildRunService.removeBuildRun(totalBuildRunsToDelete);
        log.debug("======== Image Tag list deleted.[{}]", removeRunSeq);

        // registry tag image 삭제, imageURL 이 존재하고 내부 레지스트리인 경우만 삭제
        List<BuildRunVO> existImageList = buildRunsToRemove.stream().filter(vo -> StringUtils.isNotEmpty(vo.getImageUrl()) && (vo.getExternalRegistrySeq() == null || vo.getExternalRegistrySeq() == 0) ).collect(Collectors.toList());
        if(CollectionUtils.isNotEmpty(existImageList)) {
            buildRunService.removeRegistryTagImages(existImageList);
        }

        log.debug("[END  ] removeImageTag");

        return removeRunSeq;
    }

    // build-api 서버로 부터 build 처리 결과를 받아 update 하는 메서드
    @Operation(summary = "빌드 실행 결과", description = "빌드 실행의 특정 단계 결과를 받는다. Pipeline 서버로 부터 받는 정보.")
    @PostMapping(value = "/result/{buildRunSeq}")
    public void handelBuildResult(
            @Parameter(name = "buildRunSeq", description = "빌드 실행 번호", required = true) @PathVariable Integer buildRunSeq,
            HttpServletRequest request
    ) throws Exception {

        log.debug("[START  ] handleBuildResult");

        // Build API로 부터 받은 처리결과 Json을 JsonObject로 변경
        JsonObject jsonObject = new Gson().fromJson(request.getReader(), JsonObject.class);
        String pipelineResult = jsonObject.toString();

        log.debug("The received data from pipeline : \n"+pipelineResult);

        // Build 결과처리
        buildRunService.handleBuildResult(buildRunSeq, pipelineResult);

        log.debug("[END  ] handleBuildResult");
    }


    @GetMapping(value = "/{buildRunSeq}/export")
    @Operation(summary = "빌드 Export", description = "빌드를 Export 한다.")
    public void buildFileExport(
            @Parameter(name = "buildRunSeq", description = "빌드 실행 번호", required = true) @PathVariable Integer buildRunSeq,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws Exception {

        log.debug("[BEGIN] buildFileExport");

        try {

            // build 설정정보 조회
            BuildRunVO buildRun = buildRunService.getBuildRunForExport(buildRunSeq);

            if(buildRun != null && CollectionUtils.isNotEmpty(buildRun.getBuildStepRuns())){
                // 압축할 파일명 ReleaseVersion을 포함하여 생성 - releaseVersion에 따라 import 제약을 주기 위해 추가
                String entryname = String.format("build-%s.json", cocktailServiceProperties.getReleaseVersion());

                // 등록 모델로 변경
                BuildAddVO buildAdd = new BuildAddVO();
                buildAdd.setEditType("N");
                BeanUtils.copyProperties(buildRun, buildAdd);

                // data init
                buildAdd.setBuildSeq(null);
                buildAdd.setAccountSeq(null);
                buildAdd.setCreator(null);
                buildAdd.setUpdater(null);
                List<BuildStepVO> buildSteps = Lists.newArrayList();
                for( BuildStepRunVO buildStepRun : buildRun.getBuildStepRuns() ){
                    BuildStepVO buildStep = new BuildStepVO();

                    // 조회시 password 값은 다른 암호화 하여 셋팅함
                    if (buildStepRun.getStepType() == run.acloud.api.build.enums.StepType.CODE_DOWN) {
                        StepCodeDownVO downVO = (StepCodeDownVO) buildStepRun.getBuildStepConfig();
                        downVO.setPassword(CryptoUtils.encryptDefaultAES(downVO.getPassword()));
                        buildStepRun.setStepConfig(JsonUtils.toGson(downVO));
                    }
                    else if (buildStepRun.getStepType() == run.acloud.api.build.enums.StepType.FTP) {
                        StepFtpVO taskFtp = (StepFtpVO) buildStepRun.getBuildStepConfig();
                        taskFtp.setPassword(CryptoUtils.encryptDefaultAES(taskFtp.getPassword()));
                        buildStepRun.setStepConfig(JsonUtils.toGson(taskFtp));
                    }
                    else if (buildStepRun.getStepType() == run.acloud.api.build.enums.StepType.HTTP) {
                        StepHttpVO taskHttp = (StepHttpVO) buildStepRun.getBuildStepConfig();
                        taskHttp.setPassword(CryptoUtils.encryptDefaultAES(taskHttp.getPassword()));
                        buildStepRun.setStepConfig(JsonUtils.toGson(taskHttp));
                    }
                    buildStep.setStepType(buildStepRun.getStepType());
                    buildStep.setStepConfig(buildStepRun.getStepConfig());
                    buildStep.setStepOrder(buildStepRun.getStepOrder());
                    buildStep.setUseFlag(true);
                    buildStep.setCreator(null);
                    buildStep.setUpdater(null);
                    buildSteps.add(buildStep);
                }
                buildAdd.setBuildSteps(buildSteps);


                // Zip 파일명 생성
                String buildName = StringUtils.replaceAll(buildAdd.getBuildName(), "[:\\\\/%*?:|\"<>]", "");
                String zipFileName = URLEncoder.encode(String.format("%s.zip", buildName), "UTF-8");
                // build data Base64로 인코딩하여 생성
                byte[] buildData = Base64Utils.encode(JsonUtils.toGson(buildAdd).getBytes("UTF-8"));
                byte[] zip = CompressUtils.zipFileToByte(entryname, buildData);

                // Zip 파일로 response 셋팅
                ServletOutputStream sos = response.getOutputStream();
                try {
                    response.setStatus(HttpServletResponse.SC_OK);
                    response.setContentType("application/zip; UTF-8");
                    response.setHeader("Accept-Ranges", "bytes");
                    response.setHeader("Content-Disposition", String.format("attachment; filename=%s; filename*=UTF-8''%s", zipFileName, zipFileName));
                    response.setHeader("Content-Transfer-Encoding", "binary");
                    response.setContentLength(zip.length);

                    sos.write(zip);
                    sos.flush();
                    response.flushBuffer();
                } catch (Exception e){
                    throw e;
                } finally {
                    sos.close();
                }
            }else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            }

        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            throw new CocktailException("Build Export fail.", e, ExceptionType.BuildExportFail);
        }

        log.debug("[END  ] buildFileExport");
    }

}
