package run.acloud.api.build.controller;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Validator;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import run.acloud.api.auth.vo.UserVO;
import run.acloud.api.build.enums.StepType;
import run.acloud.api.build.service.*;
import run.acloud.api.build.util.BuildUtils;
import run.acloud.api.build.vo.*;
import run.acloud.api.configuration.service.UserService;
import run.acloud.api.pipelineflow.vo.PipelineCountVO;
import run.acloud.api.resource.util.ResourceUtil;
import run.acloud.commons.util.*;
import run.acloud.commons.vo.ResultVO;
import run.acloud.framework.context.ContextHolder;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Tag(name = "Build", description = "빌드 관련 기능을 제공한다.")
@Slf4j
@RestController
@Validated
@RequestMapping(value = "/api/build")
public class PipelineBuildController {

    @Autowired
    private PipelineBuildService buildService;

    @Autowired
    private PipelineBuildRunService buildRunService;

    @Autowired
    private PipelineBuildValidationService buildValidationService;

    @Autowired
    @Qualifier(value = "pipelineBuildAddValidator")
    private Validator buildAddValidator;

    @Autowired
    private UserService userService;

    @Autowired
    private WrapPipelineFlowService pipelineFlowService;

    @Autowired
    private PipelineAsyncService pipelineAsyncService;

    @GetMapping(value = "/count/{accountSeq}")
    @Operation(summary = "빌드 작업 갯수", description = "빌드 작업 갯수을 반환한다.")
    public SystemBuildCountVO getSystemBuildCount(
            @Parameter(name = "accountSeq", description = "계정 번호", required = true) @PathVariable(name = "accountSeq") Integer accountSeq
    ) throws Exception {
        log.debug("[BEGIN] getSystemBuildCount");

        log.debug("############### accountSeq : " + accountSeq);

        SystemBuildCountVO buildCount = buildService.getSystemBuildCount(accountSeq);

        log.debug("[END  ] getSystemBuildCount");
        return buildCount;
    }


    @GetMapping(value = "")
    @Operation(summary = "빌드 목록", description = "빌드 목록을 반환한다.")
    public List<BuildVO> getBuildList(
            @Parameter(name = "accountSeq", description = "계정 번호", required = false) @RequestParam(required = false) Integer accountSeq,
            @Parameter(name = "serviceSeq", description = "Workspace 번호", required = false) @RequestParam(required = false) Integer serviceSeq
    ) throws Exception {
        log.debug("[BEGIN] getBuildList");
        List<BuildVO> result = new ArrayList<>();

        // accountSeq, serviceSeq, registryProjectId 모두 없으면 빈값 리턴
        if( accountSeq == null && serviceSeq == null ){
            return result;
        }

        result = buildService.getBuildList(accountSeq, serviceSeq);

        // pipeline 조회하여 워크로드 맵핑된 정보일 경우, Build와 맵핑된 파이프라인 갯수(워크로드 갯수) 셋팅해야함.
        List<PipelineCountVO> pipelineCounts = pipelineFlowService.getPipelineContainerCountByBuild(null, accountSeq);
        if(CollectionUtils.isNotEmpty(result) && CollectionUtils.isNotEmpty(pipelineCounts)){
            Map<Integer, Integer> countMap = pipelineCounts.stream().collect(Collectors.toMap(PipelineCountVO::getBuildSeq, PipelineCountVO::getCnt));
            Integer cnt = 0;
            for(BuildVO build : result){
                cnt = countMap.get(build.getBuildSeq());
                if(cnt != null){
                    build.setPipelineCount(cnt);
                }
            }
        }

        log.debug("[END  ] getBuildList");
        return result;
    }

    @GetMapping(value = "/names")
    @Operation(summary = "빌드명 목록", description = "빌드명 목록을 반환한다.")
    public List<BuildVO> getBuildNames(
            @Parameter(name = "accountSeq", description = "계정 번호", required = false) @RequestParam(required = false) Integer accountSeq,
            @Parameter(name = "serviceSeq", description = "Workspace 번호", required = false) @RequestParam(required = false) Integer serviceSeq
    ) throws Exception {
        log.debug("[BEGIN] getBuildNames");
        List<BuildVO> result = new ArrayList<>();

        // accountSeq, serviceSeq, registryProjectId 모두 없으면 빈값 리턴
        if( accountSeq == null && serviceSeq == null){
            return result;
        }

        result = buildService.getBuildNames(accountSeq, serviceSeq);

        log.debug("[END  ] getBuildNames");
        return result;
    }

    @GetMapping(value = "/{buildSeq:.+}/images")
    @Operation(summary = "빌드의 이미지 tag list 조회", description = "이미지 tag list 조회")
    public BuildImageVO getBuildImages(
            @Parameter(name = "buildSeq", description = "빌드 번호", required = true) @PathVariable Integer buildSeq,
            @Parameter(name = "order", description = "정렬 순서", schema = @Schema(allowableValues = {"DESC","ASC"}, defaultValue = "DESC"), required = false) @RequestParam(name = "order", defaultValue = "DESC", required = false) String order,
            @Parameter(name = "orderColumn", description = "정렬기준컬럼", schema = @Schema(allowableValues = {"TAG","IMAGE_SIZE","END_TIME"}, defaultValue = "END_TIME"), required = false) @RequestParam(name = "orderColumn", defaultValue = "END_TIME", required = false) String orderColumn
    ) throws Exception {
        log.debug("[BEGIN] getBuildImages");
        BuildVO build = null;
        BuildImageVO buildImage = null;

        build = buildService.getBuild(buildSeq);

        // ADMIN이 아닐때, 빌드의 시스템과 조회하는 사람의 시스템이 다를경우 null 리턴, 해당 시스템의 사용자만 빌드 조회 가능
        if(!"ADMIN".equals(ContextHolder.exeContext().getUserRole())) {
            UserVO user = userService.getByUserSeq(ContextHolder.exeContext().getUserSeq());
            if (!user.getAccount().getAccountSeq().equals(build.getAccountSeq())) {
                return null;
            }
        }

        buildImage = buildService.getBuildImages(buildSeq, orderColumn, order);

        /** // horbor 조회해 실제 존재 하는것만 노출함. 일단은 연동 제외
        if(buildImage != null && CollectionUtils.isNotEmpty(buildImage.getTags())){

            ImageRepositoryTagVO repositoryTag;
            List<BuildImageInfoVO> imageTagListToDelete = new ArrayList<>();
            for(BuildImageInfoVO buildImageInfoVO : buildImage.getTags()){
                try {
                    repositoryTag = registryService.getImageTagInfo(buildImage.getRegistryName(), buildImage.getImageName(), buildImageInfoVO.getTag());
                    // Harbor에 존재하지 않을때 삭제 대상에 추가
                    if(repositoryTag.getTagName() == null){
                        imageTagListToDelete.add(buildImageInfoVO);
                    }
                }catch(Exception e){
                    log.error("이미지 조회 오류 : {}", buildImageInfoVO);
                }
            }

            // 제외 대상건 삭제
            if(CollectionUtils.isNotEmpty(imageTagListToDelete)){
                buildImage.getTags().removeAll(imageTagListToDelete);
            }
        }
        */

        log.debug("[END  ] getBuildImages");
        return buildImage;
    }

    @GetMapping(value = "/{buildSeq:.+}")
    @Operation(summary = "빌드 상세", description = "빌드 상세정보를 반환한다.")
    public BuildVO getBuild(
            @Parameter(name = "buildSeq", description = "빌드 번호", required = true) @PathVariable Integer buildSeq
    ) throws Exception {
        log.debug("[BEGIN] getBuild");
        BuildVO build = null;

        build = buildService.getBuild(buildSeq);

        // ADMIN이 아닐때, 빌드의 시스템과 조회하는 사람의 시스템이 다를경우 null 리턴, 해당 시스템의 사용자만 빌드 조회 가능
        if(!"ADMIN".equals(ContextHolder.exeContext().getUserRole())) {
            UserVO user = userService.getByUserSeq(ContextHolder.exeContext().getUserSeq());
            if (!user.getAccount().getAccountSeq().equals(build.getAccountSeq())) {
                return null;
            }
        }

        log.debug("[END  ] getBuild");
        return build;
    }

    @PostMapping(value = "")
    @Operation(summary = "빌드 생성", description = "새로운 빌드를 생성한다.")
    public BuildVO addBuild(
            @Parameter(name = "buildAdd", required = true) @Validated @RequestBody BuildAddVO buildAdd,
            @Parameter(name = "callbackUrl", description = "작업완료시 callback URL", required = true) @RequestParam String callbackUrl,
            BindingResult result
    ) throws Exception {

        log.debug("[BEGIN] addBuild");

        // check build 갯수 체크
        buildService.checkPossibleCreateBuild(buildAdd.getAccountSeq());

        // 빌드명 없이 이미지명만 들어왔을 경우 validation을 위해 이미지명을 빌드명으로 넣어줌
        if( StringUtils.isEmpty(buildAdd.getBuildName()) && StringUtils.isNotEmpty(buildAdd.getImageName()) ){
            buildAdd.setBuildName(buildAdd.getImageName());
        }

        // check
        buildValidationService.checkBuildByAdd(buildAddValidator, buildAdd, result);
        buildValidationService.checkBuildServerTLSInfo(buildAdd, null, null);

        // 생성
        BuildVO build = null;
        if (StringUtils.equals(buildAdd.getEditType(), "N")) {
            if("Y".equals(buildAdd.getAutotagUseYn())){
                buildAdd.setTagName(buildAdd.getAutotagPrefix());
            }
            build = buildService.addBuild(buildAdd, callbackUrl);
        }else{
            throw new CocktailException("editType을 확인해세요.", ExceptionType.InvalidParameter);
        }

        log.debug("[END  ] addBuild");

        return build;
    }

    @PutMapping(value = "/{buildSeq:.+}")
    @Operation(summary = "빌드 수정", description = "빌드를 수정한다.")
    public ResultVO editBuild(
            @Parameter(name = "buildSeq", description = "빌드 번호", required = true) @PathVariable Integer buildSeq,
            @Parameter(name = "buildAdd", required = true, description = "빌드 수정 모델") @Validated @RequestBody BuildAddVO buildAdd,
            BindingResult result
    ) throws Exception {

        log.debug("[BEGIN] editBuild");

        // check
        buildAdd.setBuildSeq(buildSeq);
        buildValidationService.checkBuildByAdd(buildAddValidator, buildAdd, result);
        buildValidationService.checkBuildServerTLSInfo(buildAdd, buildService.getBuild(buildSeq), null);

        // 수정
        if (StringUtils.equals(buildAdd.getEditType(), "U")) {
            buildService.editBuild(buildAdd);
        }else{
            throw new CocktailException("editType을 확인해세요.", ExceptionType.InvalidParameter);
        }

        log.debug("[END  ] editBuild");

        return new ResultVO();
    }

    @DeleteMapping(value = "/{buildSeq:.+}")
    @Operation(summary = "빌드 삭제", description = "빌드를 삭제한다.")
    public BuildRunVO removeBuild(
            @Parameter(name = "buildSeq", description = "빌드 번호", required = true) @PathVariable Integer buildSeq,
            @Parameter(name = "callbackUrl", description = "작업완료시 callback URL", required = true) @RequestParam String callbackUrl
    ) throws Exception {
        log.debug("[BEGIN] removeBuild");

        BuildRunVO removeBuildRun = null;

        // 실행되고 있는 빌드가 있는경우는 삭제 못하게 함.
        int runningCount = buildRunService.getRunningBuildCount(null, buildSeq, null);
        if(runningCount > 0){
            throw new CocktailException(String.format("Build is running! [%s]", buildSeq), ExceptionType.InvalidState);
        }

        // pipeline에서 빌드를 사용하고 있는지 확인
        boolean used = pipelineFlowService.checkPipelineUsingBuild(buildSeq, null, null);
        if(used){
            throw new CocktailException("파이프라인에서 사용중인 빌드는 삭제할 수 없습니다.", ExceptionType.DeleteFailBuildHistoryOfUsingInPipeline);
        }

        // callbackUrl에 파라메터 추가
        callbackUrl = BuildUtils.addParamToCallbackURL(callbackUrl);

        removeBuildRun = buildRunService.createBuildRunByBuildRemove(buildSeq, callbackUrl);

        /** Asycn 처리 method call **/
        pipelineAsyncService.processPipelineService(removeBuildRun);

        log.debug("[END  ] removeBuild");

        return removeBuildRun;
    }

    @PostMapping(value = "/import", consumes = { "multipart/form-data" })
    @Operation(summary = "빌드 import", description = "빌드를 import 한다.")
    @ResponseBody
    public void buildFileImport(
            @Parameter(name = "accountSeq", description = "계정번호", required = true) @RequestParam(required = false) Integer accountSeq,
            @Parameter(name = "registryId", description = "이미지 저장소 아이디", required = true) @RequestParam(required = false) Integer registryId,
            @Parameter(name = "registryName", description = "이미지 저장소 이", required = true) @RequestParam(required = false) String registryName,
            @Parameter(name = "imageName", description = "image Name", required = true) @RequestParam(required = false) String imageName,
            @Parameter(name = "tagName", description = "tag Name", required = true) @RequestParam(required = false) String tagName,
            @Parameter(description = "빌드 export file", required = true) @RequestPart("dataFile") MultipartFile multipartFile,
            HttpServletResponse response
    ) throws Exception {

        log.debug("[BEGIN] buildFileImport");

        // check build 갯수 체크
        buildService.checkPossibleCreateBuild(accountSeq);

        if(!multipartFile.isEmpty()){
            // 확장자 체크
            if(!FilenameUtils.isExtension(multipartFile.getOriginalFilename(), "zip")){
                throw new CocktailException("Invalid File extension.", ExceptionType.BuildImportFileInvalid_Extension);
            }
            // mime type 체크
            Tika defaultTika = new Tika();
            if(!StringUtils.equalsIgnoreCase("application/zip", defaultTika.detect(multipartFile.getBytes()))){
                throw new CocktailException("Invalid File mime type.", ExceptionType.BuildImportFileInvalid_MimeType);
            }

            File f = null;
            try {
                f = CompressUtils.unzipFile(multipartFile);
                BuildAddVO buildAdd = null;

                if (f != null) {
                    log.debug("build import filename: zip - {}, json - {}", multipartFile.getOriginalFilename(), f.getName());

                    String buildStr = new String(Base64Utils.decode(Files.readAllBytes(Paths.get(f.getPath()))), Charset.forName("UTF-8").name());
                    String exportApiVersion = ResourceUtil.getMatchVersion(f.getName());

                    // BuildAdd 등록 모델 셋팅
                    buildAdd = new BuildAddVO();
                    buildAdd.setEditType("N");
                    buildAdd.setAccountSeq(accountSeq);
                    buildAdd.setBuildName(imageName);
                    buildAdd.setRegistryProjectId(registryId);
                    buildAdd.setRegistryName(registryName);
                    buildAdd.setImageName(imageName);
                    buildAdd.setTagName(tagName);
                    buildAdd.setBuildSteps(Lists.newArrayList());

                    if (StringUtils.startsWith(exportApiVersion, "4.")) {
                        BuildAddVO buildAddV4 = JsonUtils.fromGson(buildStr, BuildAddVO.class);
                        log.debug("buildFileImport.dataFile v4 : {}", JsonUtils.toGson(buildAddV4));

                        ObjectMapper mapper = ObjectMapperUtils.getMapper();
                        for (BuildStepVO buildStep : buildAddV4.getBuildSteps()) {
                            buildStep.setBuildSeq(null);
                            buildStep.setBuildRunSeq(null);
                            buildStep.setBuildStepSeq(null);
                            buildStep.setBuildStepRunSeq(null);

                            if (StringUtils.isNotBlank(buildStep.getStepConfig())) {
                                buildStep.setBuildStepConfig(mapper.readValue(buildStep.getStepConfig(), new TypeReference<BuildStepAddVO>(){}));

                                if (StepType.CODE_DOWN == buildStep.getStepType()) {
                                    StepCodeDownVO downVO = (StepCodeDownVO) buildStep.getBuildStepConfig();
                                    downVO.setPassword(CryptoUtils.decryptDefaultAES(downVO.getPassword()));
                                }
                                else if (buildStep.getStepType() == StepType.FTP) {
                                    StepFtpVO taskFtp = (StepFtpVO) buildStep.getBuildStepConfig();
                                    taskFtp.setPassword(CryptoUtils.decryptDefaultAES(taskFtp.getPassword()));
                                }
                                else if (buildStep.getStepType() == StepType.HTTP) {
                                    StepHttpVO taskHttp = (StepHttpVO) buildStep.getBuildStepConfig();
                                    taskHttp.setPassword(CryptoUtils.decryptDefaultAES(taskHttp.getPassword()));
                                }
                                else if (buildStep.getStepType() == StepType.CREATE_IMAGE) {
                                    StepCreateImageVO createImageVO = (StepCreateImageVO) buildStep.getBuildStepConfig();
                                    createImageVO.setPassword(CryptoUtils.decryptDefaultAES(createImageVO.getPassword()));
                                    createImageVO.setImageName(imageName);
                                    createImageVO.setImageTag(tagName);
                                    createImageVO.setRegistryId(String.valueOf(registryId));
                                    createImageVO.setRegistryName(registryName);
                                }
                            }
                        }

                        buildAdd.setBuildSteps(buildAddV4.getBuildSteps());
                    }
                    else {
                        throw new CocktailException("This build file is not supported for import in that version.", ExceptionType.BuildFileNotSupported);
                    }

                    // check
//                  buildValidationService.checkBuildByAdd(buildAddValidator, buildAdd);

                    // 빌드명 없이 이미지명만 들어왔을 경우 validation을 위해 이미지명을 빌드명으로 넣어줌
                    if( StringUtils.isEmpty(buildAdd.getBuildName()) && StringUtils.isNotEmpty(buildAdd.getImageName()) ){
                        buildAdd.setBuildName(buildAdd.getImageName());
                    }

                    // build 등록
//                  BuildVO build = buildService.addBuild(buildAdd, callbackUrl);
                }


                // json으로 response 셋팅
                ServletOutputStream sos = response.getOutputStream();
                try {
                    response.setStatus(HttpServletResponse.SC_OK);
                    response.setContentType("application/json");

                    ResultVO result = new ResultVO();
                    if (buildAdd != null) {
                        result.setResult(buildAdd);
                    }

                    sos.write(JsonUtils.toGson(result).getBytes());
                    sos.flush();
                    response.flushBuffer();
                } catch (IOException e){
                    throw e;
                } finally {
                    sos.close();
                }
            } catch (CocktailException e) {
                throw e;
            } catch (Exception e) {
                throw new CocktailException("Build Import fail.", e, ExceptionType.BuildImportFail);
            }
        }else{
            throw new CocktailException("File empty.", ExceptionType.BuildImportFileInvalid);
        }

        log.debug("[END  ] buildFileImport");
    }
}
