package run.acloud.api.build.service;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Validator;
import run.acloud.api.build.constant.BuildConstants;
import run.acloud.api.build.dao.IBuildMapper;
import run.acloud.api.build.dao.IBuildRunMapper;
import run.acloud.api.build.enums.*;
import run.acloud.api.build.event.PipelineBuildEventService;
import run.acloud.api.build.util.BuildUtils;
import run.acloud.api.build.vo.*;
import run.acloud.api.configuration.dao.IAccountMapper;
import run.acloud.api.configuration.enums.GradeApplyState;
import run.acloud.api.configuration.service.ExternalRegistryService;
import run.acloud.api.configuration.vo.AccountGradeVO;
import run.acloud.api.configuration.vo.ExternalRegistryDetailVO;
import run.acloud.api.configuration.vo.ImageRepositoryTagVO;
import run.acloud.api.cserver.dao.IServiceMapper;
import run.acloud.api.pipelineflow.util.PipelineTypeConverter;
import run.acloud.api.pipelineflow.vo.PipelineCommandVO;
import run.acloud.api.pipelineflow.vo.PipelineRelatedInfoVO;
import run.acloud.commons.service.HarborRegistryFactoryService;
import run.acloud.commons.service.IHarborRegistryService;
import run.acloud.commons.service.RegistryPropertyService;
import run.acloud.commons.util.CryptoUtils;
import run.acloud.commons.util.JsonUtils;
import run.acloud.commons.util.ObjectMapperUtils;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;
import run.acloud.framework.properties.CocktailBuilderProperties;
import run.acloud.framework.properties.CocktailServiceProperties;
import run.acloud.protobuf.pipeline.PipelineAPIServiceProto;

import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PipelineBuildRunService {

    @Resource(name = "cocktailSession")
    private SqlSessionTemplate sqlSession;

    @Autowired
    private CocktailBuilderProperties cocktailBuilderProperties;

    @Autowired
    private RegistryPropertyService registryProperties;

    @Autowired
    private HarborRegistryFactoryService harborRegistryFactory;

    @Autowired
    private PipelineBuildEventService eventService;

    @Autowired
    private PipelineBuildRunLogService runLogService;

    @Autowired
    private PipelineBuildService buildService;

    @Autowired
    private PipelineBuildValidationService buildValidationService;

    @Autowired
    @Qualifier(value = "pipelineBuildAddValidator")
    private Validator buildAddValidator;

    @Autowired
    private WrapPipelineFlowService pipelineFlowService;

    @Autowired
    private WrapPipelineAPIService pipelineAPIService;

    @Autowired
    private ExternalRegistryService externalRegistryService;

    @Autowired
    private CocktailServiceProperties cocktailServiceProperties;


    // 빌드 이력 목록 조회
    public List<BuildRunVO> getBuildRuns(Integer buildSeq){
        IBuildRunMapper buildRunDao = sqlSession.getMapper(IBuildRunMapper.class);
        List<BuildRunVO> buildRuns = buildRunDao.getBuildRuns(buildSeq);

        // pipeline related info convert
        if(CollectionUtils.isNotEmpty(buildRuns)){
            for(BuildRunVO buildRun : buildRuns){
                // pipeline 관련설정 정보가 있으면 VO 형태로 converting and Json 정보삭제
                if(StringUtils.isNotEmpty(buildRun.getRelatedPipeline())){
                    RelatedPipelineInfoVO relatedPipelineInfo = JsonUtils.fromGson(buildRun.getRelatedPipeline(), RelatedPipelineInfoVO.class);
                    buildRun.setRelatedPipelineInfo(relatedPipelineInfo);
                    buildRun.setRelatedPipeline(null);
                }
            }

            // sort
            buildRuns.sort(Comparator.comparing(BuildRunVO::getBeginTime).reversed());
        }

        return buildRuns;
    }

    /**
     * buildRunSeq list에 의한 BuildRun 정보 조회, pipeline 관련설정 정보가 있으면 VO 형태로 설정
     *
     * @param buildRunSeqs
     * @return
     */
    public List<BuildRunVO> getBuildRunsByBuildRunSeqs(List<Integer> buildRunSeqs){
        IBuildRunMapper buildRunDao = sqlSession.getMapper(IBuildRunMapper.class);
        List<BuildRunVO> buildRuns = buildRunDao.getBuildRunsByBuildRunSeqs(buildRunSeqs);

        // pipeline related info convert
        if(CollectionUtils.isNotEmpty(buildRuns)){
            for(BuildRunVO buildRun : buildRuns){
                // pipeline 관련설정 정보가 있으면 VO 형태로 converting and Json 정보삭제
                if(StringUtils.isNotEmpty(buildRun.getRelatedPipeline())){
                    RelatedPipelineInfoVO relatedPipelineInfo = JsonUtils.fromGson(buildRun.getRelatedPipeline(), RelatedPipelineInfoVO.class);
                    buildRun.setRelatedPipelineInfo(relatedPipelineInfo);
                    buildRun.setRelatedPipeline(null);
                }
            }
        }

        return buildRuns;
    }

    public List<BuildRunVO> getBuildRunsByLatest(Integer accountSeq, Integer serviceSeq, Integer limitCount){
        IBuildRunMapper buildRunDao = sqlSession.getMapper(IBuildRunMapper.class);
        List<BuildRunVO> buildRuns = buildRunDao.getBuildRunsByLatest(accountSeq, serviceSeq, limitCount);

        return buildRuns;
    }

    public List<Integer> getProjectIdsOfService(Integer serviceSeq) {
        IServiceMapper dao = sqlSession.getMapper(IServiceMapper.class);

        return dao.getProjectIdsOfService(serviceSeq);
    }

    // 이미지 정보가 존재하는 빌드 이력 목록 조회
    public List<BuildRunVO> getBuildRunsByExistsImage(Integer buildSeq){
        IBuildRunMapper buildRunDao = sqlSession.getMapper(IBuildRunMapper.class);
        List<BuildRunVO> buildRuns = buildRunDao.getBuildRunsByExistsImage(buildSeq);

        return buildRuns;
    }

    public List<BuildRunVO> getBuildRunsBySameTagName(Integer buildSeq, String tagName){
        IBuildRunMapper buildRunDao = sqlSession.getMapper(IBuildRunMapper.class);
        List<BuildRunVO> buildRuns = buildRunDao.getBuildRunsBySameTagName(buildSeq, tagName);

        return buildRuns;
    }

    /**
     * 입력된 buildRunSeq들에 해당하는 이미지 Url이 동일한 다른 buildRun 리스트 조회.<br/>
     * 파라메터로 입력된 BuildRun 정보는 제외한다.
     *
     * @param buildRunSeqs
     * @return
     */
    public List<BuildRunVO> getOtherBuildRunsBySameImageUrl(Integer buildSeq, List<Integer> buildRunSeqs){
        IBuildRunMapper buildRunDao = sqlSession.getMapper(IBuildRunMapper.class);
        List<BuildRunVO> buildRuns = buildRunDao.getOtherBuildRunsBySameImageUrl(buildSeq, buildRunSeqs);

        return buildRuns;
    }

    /**
     * 빌드 상세정보 조회. <br/>
     * build step 에서 사용하는 password를 포함안함.
     *
     * @param buildRunSeq
     * @return
     * @throws IOException
     */
    public BuildRunVO getBuildRun(Integer buildRunSeq) throws IOException {
        IBuildRunMapper buildRunDao = sqlSession.getMapper(IBuildRunMapper.class);

        BuildRunVO buildRun = buildRunDao.getBuildRun(buildRunSeq);  // 빌드 실행정보 조회

        if(buildRun != null) {

            // INIT Step이 없을때는 InitStep 를 추가
            this.addInitBuildStepRun(buildRun);

            buildRun = convertBuildStepRunConfig(buildRun, true);

            // pipeline 관련설정 정보가 있으면 VO 형태로 converting and Json 정보삭제
            if (StringUtils.isNotBlank(buildRun.getRelatedPipeline())) {
                RelatedPipelineInfoVO relatedPipelineInfo = JsonUtils.fromGson(buildRun.getRelatedPipeline(), RelatedPipelineInfoVO.class);
                buildRun.setRelatedPipelineInfo(relatedPipelineInfo);
                buildRun.setRelatedPipeline(null);
            }

            // Build Cancel 상태 재설정
            checkBuildCancelState(buildRun);
        }
        return buildRun;
    }

    public BuildRunVO getBuildRun(Integer buildRunSeq, String useYn, boolean withConvert) throws IOException {
        IBuildRunMapper buildRunDao = sqlSession.getMapper(IBuildRunMapper.class);
        BuildRunVO buildRun = buildRunDao.getBuildRunWithUseYn(buildRunSeq, useYn);  // 빌드 실행정보 조회

        if (buildRun != null) {
            // Build Cancel 상태 재설정
            checkBuildCancelState(buildRun);

            if (withConvert) {
                buildRun = convertBuildStepRunConfig(buildRun, true);
            }
        }
        return buildRun;
    }

    private void checkBuildCancelState(BuildRunVO buildRun){
        // 실행중인데 init step 이 'DONE' 이 아닌 경우, CANCEL Action은 삭제 한다.
        if (buildRun.getRunState() == RunState.RUNNING && buildRun.getRunType() == RunType.BUILD
                && CollectionUtils.isNotEmpty(buildRun.getPossibleActions()) && buildRun.getPossibleActions().contains(BuildAction.CANCEL)){
            Optional<BuildStepRunVO> initStep = buildRun.getBuildStepRuns().stream().filter(vo -> vo.getStepType() == StepType.INIT && vo.getStepState() != StepState.DONE).findFirst();
            if(initStep.isPresent()){
                buildRun.getPossibleActions().remove(BuildAction.CANCEL);
            }
        }
    }

    public BuildRunVO getBuildRunForExport(Integer buildRunSeq) throws IOException {
        IBuildRunMapper buildRunDao = sqlSession.getMapper(IBuildRunMapper.class);
        BuildRunVO buildRun = buildRunDao.getBuildRun(buildRunSeq);  // 빌드 실행정보 조회

        if(buildRun != null && CollectionUtils.isNotEmpty(buildRun.getBuildStepRuns())) {

            // INIT Step이 없을때는 InitStep 를 추가
            this.addInitBuildStepRun(buildRun);

            // 이미지 정보 셋팅
            ObjectMapper mapper = ObjectMapperUtils.getMapper();

            for (BuildStepRunVO buildStepRun : buildRun.getBuildStepRuns()) {
                buildStepRun.setBuildStepConfig(mapper.readValue(buildStepRun.getStepConfig(), new TypeReference<BuildStepAddVO>(){}));
                buildStepRun.setUseFlag(true);
                buildStepRun.setBuildRunSeq(buildRun.getBuildRunSeq());

                // 코드 저장 디렉토리에 데이터가 없으면(기존 데이터일 경우 "repo" 셋팅, 신규나 수정건은 DB에 없을수가 없음)
                if (StepType.CODE_DOWN == buildStepRun.getStepType()) {
                    StepCodeDownVO downVO = (StepCodeDownVO) buildStepRun.getBuildStepConfig();

                    if (StringUtils.isEmpty(downVO.getCodeSaveDir())) {
                        downVO.setCodeSaveDir("repo");
                    }
                }

                if (StepType.CODE_DOWN == buildStepRun.getStepType()) {
                    StepCodeDownVO downVO = (StepCodeDownVO) buildStepRun.getBuildStepConfig();
                    downVO.setPassword(CryptoUtils.decryptAES(downVO.getPassword()));
                } else if (buildStepRun.getStepType() == StepType.FTP) {
                    StepFtpVO taskFtp = (StepFtpVO) buildStepRun.getBuildStepConfig();
                    taskFtp.setPassword(CryptoUtils.decryptAES(taskFtp.getPassword()));
                } else if (buildStepRun.getStepType() == StepType.HTTP) {
                    StepHttpVO taskHttp = (StepHttpVO) buildStepRun.getBuildStepConfig();
                    taskHttp.setPassword(CryptoUtils.decryptAES(taskHttp.getPassword()));
                } else if (buildStepRun.getStepType() == StepType.CREATE_IMAGE) {
                    StepCreateImageVO createImageVO = (StepCreateImageVO) buildStepRun.getBuildStepConfig();
                    createImageVO.setLoginId(null);
                    createImageVO.setPassword(null);
                }
            }
        }

        return buildRun;
    }

    /**
     * 빌드 상세정보 조회. <br/>
     * build step 에서 사용하는 password를 포함하여 조회
     *
     * @param buildRunSeq
     * @return
     * @throws IOException
     */
    public BuildRunVO getBuildRunWithPasswd(Integer buildRunSeq) throws IOException {
        IBuildRunMapper buildRunDao = sqlSession.getMapper(IBuildRunMapper.class);

        BuildRunVO buildRunVO = buildRunDao.getBuildRun(buildRunSeq);  // 빌드 실행정보 조회

        // INIT Step이 없을때는 InitStep 를 추가
        this.addInitBuildStepRun(buildRunVO);

        buildRunVO = convertBuildStepRunConfig(buildRunVO, false);

        return buildRunVO;
    }

    public BuildRunVO convertBuildStepRunConfig(BuildRunVO buildRun, boolean withoutPasswd) throws IOException {

        if(buildRun != null && CollectionUtils.isNotEmpty(buildRun.getBuildStepRuns())) {

            // buildserver tls 정보 복호화 셋팅
            if(withoutPasswd) {
                buildRun.setBuildServerCacrt(null);
                buildRun.setBuildServerClientCert(null);
                buildRun.setBuildServerClientKey(null);
            } else {
                buildRun.setBuildServerCacrt(CryptoUtils.decryptAES(buildRun.getBuildServerClientCert()));
                buildRun.setBuildServerClientCert(CryptoUtils.decryptAES(buildRun.getBuildServerClientCert()));
                buildRun.setBuildServerClientKey(CryptoUtils.decryptAES(buildRun.getBuildServerClientKey()));
            }

            // 이미지 정보 셋팅팅
            ObjectMapper mapper = ObjectMapperUtils.getMapper();

            for (BuildStepRunVO buildStepRun : buildRun.getBuildStepRuns()) {
                buildStepRun.setBuildStepConfig(mapper.readValue(buildStepRun.getStepConfig(), new TypeReference<BuildStepAddVO>(){}));
                buildStepRun.setStepConfig(null);
                buildStepRun.setUseFlag(true);
                buildStepRun.setBuildRunSeq(buildRun.getBuildRunSeq());

                // 코드 저장 디렉토리에 데이터가 없으면(기존 데이터일 경우 "repo" 셋팅, 신규나 수정건은 DB에 없을수가 없음)
                if (StepType.CODE_DOWN == buildStepRun.getStepType()) {
                    StepCodeDownVO downVO = (StepCodeDownVO) buildStepRun.getBuildStepConfig();

                    if (StringUtils.isEmpty(downVO.getCodeSaveDir())) {
                        downVO.setCodeSaveDir("repo");
                    }
                }

                // 조회시 password 값은 null로 셋팅함
                if(withoutPasswd) {
                    if (StepType.CODE_DOWN == buildStepRun.getStepType()) {
                        StepCodeDownVO downVO = (StepCodeDownVO) buildStepRun.getBuildStepConfig();
                        downVO.setPassword(null);
                    } else if (buildStepRun.getStepType() == StepType.FTP) {
                        StepFtpVO taskFtp = (StepFtpVO) buildStepRun.getBuildStepConfig();
                        taskFtp.setPassword(null);
                    } else if (buildStepRun.getStepType() == StepType.HTTP) {
                        StepHttpVO taskHttp = (StepHttpVO) buildStepRun.getBuildStepConfig();
                        taskHttp.setPassword(null);
                    }
                }else{
                    if (StepType.CODE_DOWN == buildStepRun.getStepType()) {
                        StepCodeDownVO downVO = (StepCodeDownVO) buildStepRun.getBuildStepConfig();
                        downVO.setPassword(CryptoUtils.decryptAES(downVO.getPassword()));
                    } else if (buildStepRun.getStepType() == StepType.FTP) {
                        StepFtpVO taskFtp = (StepFtpVO) buildStepRun.getBuildStepConfig();
                        taskFtp.setPassword(CryptoUtils.decryptAES(taskFtp.getPassword()));
                    } else if (buildStepRun.getStepType() == StepType.HTTP) {
                        StepHttpVO taskHttp = (StepHttpVO) buildStepRun.getBuildStepConfig();
                        taskHttp.setPassword(CryptoUtils.decryptAES(taskHttp.getPassword()));
                    } else if (buildStepRun.getStepType() == StepType.CREATE_IMAGE) {
                        StepCreateImageVO createImageVO = (StepCreateImageVO) buildStepRun.getBuildStepConfig();
                        createImageVO.setPassword(CryptoUtils.decryptAES(createImageVO.getPassword()));
                    }
                }
            }
        }

        return buildRun;
    }

    /**
     * pipeline 에서 현재 build run 정보를 사용하고 있는지 체크해 build run 에 파이프라인 정보 셋팅한다.
     * 하나의 build run을 여러개의 pipeline이 사용할 수 있다.
     *
     * @param buildSeq
     * @param buildRuns
     */
    public void setPipelineInfoToBuildRuns(Integer buildSeq, List<BuildRunVO> buildRuns){

        try {
            List<PipelineRelatedInfoVO> pipelineInfos = pipelineFlowService.getPipelineRelatedInfoListUsingBuild(buildSeq);

            /** buildRun 리스트와 맵핑할 수 있도록 pipelineContainerSeq 별로 Map 생성 **/
            Map<Integer, PipelineRelatedInfoVO> pipelineMap = new HashMap<>();
            if(CollectionUtils.isNotEmpty(pipelineInfos)){
                for(PipelineRelatedInfoVO pipelineInfo : pipelineInfos){
                    pipelineMap.put(pipelineInfo.getPipelineContainerSeq(), pipelineInfo);
                }
            }

            /** build 정보에 pipeline 정보 셋팅 **/
            if(CollectionUtils.isNotEmpty(buildRuns) && MapUtils.isNotEmpty(pipelineMap)){

                for(BuildRunVO buildRun : buildRuns){
                    // pipeline 에서 빌드한 정보만 셋팅
                    if(buildRun.getPipelineSeq() > 0) {
                        PipelineRelatedInfoVO pipelineInfo = pipelineMap.get(buildRun.getPipelineSeq());
                        if(pipelineInfo != null){
                            RelatedPipelineInfoVO relatedPipelineInfo = new RelatedPipelineInfoVO();
                            BeanUtils.copyProperties(pipelineInfo, relatedPipelineInfo);
                            buildRun.setRelatedPipelineInfo(relatedPipelineInfo);
                        }
                    }
                }
            }

        } catch (Exception e) {
            log.error("Setting pipeline infomation is Failed.", e);
        }

    }

    // 빌드 이력 전체 로그 보기
    public BuildRunLogVO getBuildAllLog(Integer buildRunSeq){
        IBuildRunMapper buildRunDao = sqlSession.getMapper(IBuildRunMapper.class);
        BuildRunLogVO buildRunLogVO = buildRunDao.getBuildAllLog(buildRunSeq);

        // DB log를 사용해야 하는지 체크후 flag 값 설정, DB Save 모드 일때만 DB log check
        if(cocktailBuilderProperties.isBuildLogDbSaveEnabled()){

            RunState runState = buildRunLogVO.getRunState();
            List<BuildStepRunVO> buildStepRunList = buildRunLogVO.getBuildRunLogs();

            // front에서 log를 어디서(DB or Nats) 가져와야 할지 flag 셋팅
            switch (runState){
                case DONE: //case2 상태=DONE & 마지막 build step 에 로그가 존재하면 DB log
                    BuildStepRunVO tmpDoneLog = buildStepRunList.get(buildStepRunList.size()-1);
                    if(tmpDoneLog.getLog() != null && !tmpDoneLog.getLog().equals("") ) buildRunLogVO.setDbLog(true);
                    break;
                case ERROR: // case2 상태=ERROR & 각 build step 에 로그가 존재하면 DB log, 에러는 중간에 로그가 끊겨 전체 로그가 없을 수도 있음.
                    BuildStepRunVO tmpErrorLog = buildStepRunList.stream()
                            .filter( vo -> (vo.getLog() != null && !vo.getLog().equals("")) )
                            .findAny()
                            .orElseGet(() ->null);
                    if(tmpErrorLog != null) buildRunLogVO.setDbLog(true);
                    break;
            }

        }

        return buildRunLogVO;
    }

    // 실행된 각 step 에 대한 로그 조회
    public BuildRunLogVO getBuildLog(Integer buildStepRunSeq){
        IBuildRunMapper buildRunDao = sqlSession.getMapper(IBuildRunMapper.class);
        BuildRunLogVO buildRunLogVO = buildRunDao.getBuildLog(buildStepRunSeq);

        // DB log를 사용해야 하는지 체크후 flag 값 설정, DB Save 모드 일때만 DB log check
        if(cocktailBuilderProperties.isBuildLogDbSaveEnabled()){

            RunState runState = buildRunLogVO.getRunState();
            List<BuildStepRunVO> buildStepRunList = buildRunLogVO.getBuildRunLogs();

            if(CollectionUtils.isNotEmpty(buildStepRunList)){
                // 어차피 한건이기 때문에 0 번째 값을 가져옴
                BuildStepRunVO buildStepRunVO = buildStepRunList.get(0);
                StepState stepState = buildStepRunVO.getStepState();

                // front에서 log를 어디서(DB or Nats) 가져와야 할지 flag 셋팅
                // step 상태=(DONE or ERROR) & 로그가 존재하면 DB log
                switch (stepState){
                    case DONE:
                    case ERROR:
                        if(buildStepRunVO.getLog() != null && !buildStepRunVO.getLog().equals("") ) buildRunLogVO.setDbLog(true);
                        break;
                }
            }

        }

        return buildRunLogVO;
    }

    /**
     * 최신 빌드 실행 데이터 생성 메서드.
     * 빌드 실행하기, 빌드화면이나 pipeline 화면에서 빌드실행시 호출됨.
     *
     * @param buildSeq
     * @param callbackUrl
     * @param tagName
     * @param description
     * @param pipelineSeq
     * @return
     * @throws Exception
     */
    public BuildRunVO createBuildRun(Integer buildSeq, String callbackUrl, String tagName, String description, Integer pipelineSeq) throws Exception {
        BuildRunVO buildRun = null;

        // build 정보 조회
        BuildVO build = buildService.getBuild(buildSeq);

        // 내부 빌드서버 사용여부 체크
        buildValidationService.checkUseInternalBuildServer(build.getAccountSeq(), build.getBuildServerHost());

        // 빌드서버 상태 체크
        buildValidationService.chekcBuildServerStatus(build.getAccountSeq(), build.getBuildServerHost());

        // 빌드 실행 갯수 체크
        this.checkPossibleRunBuildBySystem(build.getAccountSeq());

        // 빌드 실행 여부 체크, 빌드 실행 여부는 buildSeq와 pipelineSeq로 체크한다.
        this.checkPossibleRunBuildByBuild(buildSeq, pipelineSeq);

        // generation next build number & set to VO, tag 생성 때문에 번호 채번을 먼저함.
        build.setBuildNo( buildService.getNextBuildNo(buildSeq) );

        // 자동태그일 경우 tagName generate
        if ("Y".equals(build.getAutotagUseYn())){
            tagName = BuildUtils.generateTagNameByAutoTagInfo(build.getAutotagPrefix(), cocktailServiceProperties.getRegionTimeZone(), build.getBuildNo(), build.getAutotagSeqType());
        }

        // check tag name, 입력한 태그와 이전 태그가 다를 경우에 존재하는 태그인지 체크
        if(!"Y".equals(build.getAutotagUseYn()) && !build.getTagName().equals(tagName)){
            List<BuildRunVO> sameTagRuns = this.getBuildRunsBySameTagName(buildSeq, tagName);
            if(CollectionUtils.isNotEmpty(sameTagRuns)){
                throw new CocktailException(String.format("이미 사용된 tagName 입니다.(레지스트리: %s, 이미지: %s, 태그: %s 중복)", build.getRegistryName(), build.getImageName(), tagName), ExceptionType.IsExistsImageNameTag);
            }
        }

        // call the method to create a build run data.
        buildRun = this.addBuildRunByBuild( build, callbackUrl, tagName, description, pipelineSeq );

        return buildRun;
    }


    @Transactional(transactionManager = "transactionManager")
    protected BuildRunVO addBuildRunByBuild(BuildVO buildVO, String callbackUrl, String tagName, String description, Integer pipelineSeq) throws Exception {
        IBuildRunMapper buildRunDao = sqlSession.getMapper(IBuildRunMapper.class);

        BuildRunVO buildRun = this.createBuildRunVOForBuild(buildVO, tagName, callbackUrl, description, pipelineSeq);

        // BuildRun 등록
        buildRunDao.addBuildRun(buildRun);

        // BuildStepRun 등록
        buildRunDao.addBuildStepRunsByBuildStep(buildRun);

        // logId 셋팅 및 create image step 추가 정보 update
        this.updateLogIdAndCreateImageAddtionalInfo(buildRun);

        // 등록된 BuildRun 정보 조회
        buildRun = this.getBuildRun(buildRun.getBuildRunSeq());

        return buildRun;
    }

    /**
     *  빌드이력 수정없이 기존 빌드 이력 정보로 실행 데이터 생성 메서드.
     *  pipeline에서 빌드 싫행 버튼 클릭시 연결된 빌드 이력으로 빌드실행 데이터 생성.
     *
     * @param buildSeq
     * @param prevBuildRunSeq 참조할 기존 buildRunSeq
     * @param callbackUrl
     * @param tagName
     * @param description
     * @param pipelineSeq
     * @return
     * @throws Exception
     */
    public BuildRunVO createBuildRunByBuildRun(Integer buildSeq, Integer prevBuildRunSeq, String callbackUrl, String tagName, String description, Integer pipelineSeq) throws Exception {
        BuildRunVO buildRun = null;

        // build 정보 조회
        BuildVO build = buildService.getBuild(buildSeq);

        // generation next build number & set to VO
        build.setBuildNo( buildService.getNextBuildNo(buildSeq) );

        // 빌드 실행 갯수 체크
        this.checkPossibleRunBuildBySystem(build.getAccountSeq());

        // 빌드 실행 여부 체크, 빌드 실행 여부는 buildSeq와 pipelineSeq로 체크한다.
        this.checkPossibleRunBuildByBuild(buildSeq, pipelineSeq);

        BuildRunVO prevBuildRun = this.getBuildRun(prevBuildRunSeq, null, false);

        if (prevBuildRun != null) {

            // 자동태그일 경우 tagName generate
            if ("Y".equals(prevBuildRun.getAutotagUseYn())){
                tagName = BuildUtils.generateTagNameByAutoTagInfo(prevBuildRun.getAutotagPrefix(), cocktailServiceProperties.getRegionTimeZone(), build.getBuildNo(), prevBuildRun.getAutotagSeqType());
            }

            // build server host 정보 입력
            build.setBuildServerHost(prevBuildRun.getBuildServerHost());
            build.setBuildServerTlsVerify(prevBuildRun.getBuildServerTlsVerify());
            build.setBuildServerCacrt(prevBuildRun.getBuildServerCacrt());
            build.setBuildServerClientCert(prevBuildRun.getBuildServerClientCert());
            build.setBuildServerClientKey(prevBuildRun.getBuildServerClientKey());

            // addBuildRunByBuildRun 메서드에서 build_run 정보 생성시 쓰일 자동태그 정보를 이전 build_run 정보에서 가져와 build VO에 셋팅한다.
            build.setAutotagUseYn(prevBuildRun.getAutotagUseYn());
            build.setAutotagPrefix(prevBuildRun.getAutotagPrefix());
            build.setAutotagSeqType(prevBuildRun.getAutotagSeqType());

            // check tag name, 입력한 태그와 이전 태그가 다를 경우에 존재하는 태그인지 체크, 자동태그가 아닐때만 체크
            if(!prevBuildRun.getTagName().equals(tagName) && !"Y".equals(prevBuildRun.getAutotagUseYn())){
                List<BuildRunVO> sameTagRuns = this.getBuildRunsBySameTagName(buildSeq, tagName);
                if(CollectionUtils.isNotEmpty(sameTagRuns)){
                    String errMsg = String.format("tagName already used. (Duplicate Registry: %s, Image: %s, Tag: %s)", build.getRegistryName(), build.getImageName(), tagName);
                    throw new CocktailException(errMsg, ExceptionType.IsExistsImageNameTag, errMsg);
                }
            }
        }

        // 내부 빌드서버 사용여부 체크
        buildValidationService.checkUseInternalBuildServer(build.getAccountSeq(), build.getBuildServerHost());

        // 빌드서버 상태 체크
        buildValidationService.chekcBuildServerStatus(build.getAccountSeq(), build.getBuildServerHost());

        // call the method to create a build run data.
        buildRun = this.addBuildRunByBuildRun( build, prevBuildRunSeq, callbackUrl, tagName, description, pipelineSeq );

        return buildRun;
    }

    @Transactional(transactionManager = "transactionManager")
    protected BuildRunVO addBuildRunByBuildRun(BuildVO buildVO, Integer prevBuildRunSeq, String callbackUrl, String tagName, String description, Integer pipelineSeq) throws Exception {
        IBuildRunMapper buildRunDao = sqlSession.getMapper(IBuildRunMapper.class);

        // buildRun 정보 생성
        BuildRunVO buildRun = this.createBuildRunVOForBuild(buildVO, tagName, callbackUrl, description, pipelineSeq);

        // 이전 빌드실행번호 셋팅
        buildRun.setPrevBuildRunSeq(prevBuildRunSeq);

        // BuildRun 등록
        buildRunDao.addBuildRun(buildRun);

        // BuildStepRun 등록
        buildRunDao.addBuildStepRunsByHistory(buildRun);

        // logId 셋팅 및 create image step 추가 정보 update
        this.updateLogIdAndCreateImageAddtionalInfo(buildRun);

        // 등록된 BuildRun 정보 조회
        buildRun = this.getBuildRun(buildRun.getBuildRunSeq());

        return buildRun;
    }

    /**
     * 빌드 이력 및 수정사항을 가지고 빌드실행 데이터 생성하는 메서드.
     * 기존 빌드 실행 정보없이 빌드실행 데이터를 모두 넘겨서 처리할 수도 있다.
     *
     * @param buildSeq
     * @param buildRunSeq
     * @param callbackUrl
     * @param tagName
     * @param description
     * @param pipelineSeq
     * @param buildAdd
     * @param result
     * @return
     * @throws Exception
     */
    public BuildRunVO createBuildRunByBuildRunModify(Integer buildSeq, Integer buildRunSeq, String callbackUrl, String tagName, String description, Integer pipelineSeq, BuildAddVO buildAdd, BindingResult result) throws Exception {
        // 등록정보 check
        buildAdd.setBuildSeq(buildSeq);
        buildValidationService.checkBuildByAdd(buildAddValidator, buildAdd, result);

        // build 정보 조회
        BuildVO build = buildService.getBuild(buildSeq);

        // generation next build number & set to VO
        build.setBuildNo( buildService.getNextBuildNo(buildSeq) );

        // 동시 빌드 실행 갯수 체크
        this.checkPossibleRunBuildBySystem(build.getAccountSeq());

        // 빌드 실행 여부 체크, 빌드 실행 여부는 buildSeq와 pipelineSeq로 체크한다.
        this.checkPossibleRunBuildByBuild(buildSeq, pipelineSeq);

        BuildRunVO buildRun = this.getBuildRun(buildRunSeq); // 기존 BuildRun 조회

        // build server TLS 정보 체크
        buildValidationService.checkBuildServerTLSInfo(buildAdd, null, buildRun);

        // 자동태그일 경우 tagName generate
        if ("Y".equals(buildAdd.getAutotagUseYn())){
            tagName = BuildUtils.generateTagNameByAutoTagInfo(buildAdd.getAutotagPrefix(), cocktailServiceProperties.getRegionTimeZone(), build.getBuildNo(), buildAdd.getAutotagSeqType());
        }

        // build server TLS 셋팅
        BuildUtils.setBuildServerTLSFromBuildRunToBuildAdd(buildAdd, this.getBuildRun(buildRunSeq, null, false));

        // 서버 호스트 & 빌드서버 TLS 설정
        build.setBuildServerHost(buildAdd.getBuildServerHost());
        build.setBuildServerTlsVerify(buildAdd.getBuildServerTlsVerify());
        build.setBuildServerCacrt(buildAdd.getBuildServerCacrt());
        build.setBuildServerClientCert(buildAdd.getBuildServerClientCert());
        build.setBuildServerClientKey(buildAdd.getBuildServerClientKey());

        // 내부 빌드서버 사용여부 체크
        buildValidationService.checkUseInternalBuildServer(build.getAccountSeq(), build.getBuildServerHost());

        // 빌드서버 상태 체크
        buildValidationService.chekcBuildServerStatus(build.getAccountSeq(), build.getBuildServerHost());

        // createBuildRunVOForBuild 메서드에서 build_run 정보 생성시 쓰일 자동태그 정보를 buildAdd 정보에서 가져와 buildVO에 셋팅한다.
        build.setAutotagUseYn(buildAdd.getAutotagUseYn());
        build.setAutotagPrefix(buildAdd.getAutotagPrefix());
        build.setAutotagSeqType(buildAdd.getAutotagSeqType());

        // check tag name, 입력한 태그와 이전 태그가 다를 경우에 존재하는 태그인지 체크, 자동태그가 아닐때만 체크
        if( ( buildRun == null && !"Y".equals(buildAdd.getAutotagUseYn()) )
                || ( buildRun != null && !buildRun.getTagName().equals(tagName) && !"Y".equals(buildAdd.getAutotagUseYn()) )
        ){
            List<BuildRunVO> sameTagRuns = this.getBuildRunsBySameTagName(buildSeq, tagName);
            if(CollectionUtils.isNotEmpty(sameTagRuns)){
                throw new CocktailException(String.format("이미 사용된 tagName 입니다.(레지스트리: %s, 이미지: %s, 태그: %s 중복)", buildAdd.getRegistryName(), buildAdd.getImageName(), buildAdd.getTagName()), ExceptionType.IsExistsImageNameTag);
            }
        }

        // Create a build run data.
        buildRun = this.addBuildRunByBuildRunModify( build, buildRunSeq, buildAdd, tagName, callbackUrl, description, pipelineSeq );

        return buildRun;
    }

    /**
     * 빌드 실행 이력을 이용한 재실행인 경우, 빌드 실행 데이터 처리 로직
     *
     * @param buildVO
     * @param buildRunSeq
     * @param buildAdd
     * @param callbackUrl
     * @param description
     * @return
     */
    @Transactional(transactionManager = "transactionManager")
    protected BuildRunVO addBuildRunByBuildRunModify(BuildVO buildVO, Integer buildRunSeq, BuildAddVO buildAdd, String tagName, String callbackUrl, String description, Integer pipelineSeq) throws Exception {
        IBuildRunMapper buildRunDao = sqlSession.getMapper(IBuildRunMapper.class);

        BuildRunVO buildRun = this.createBuildRunVOForBuild(buildVO, tagName, callbackUrl, description, pipelineSeq);

        // buildRun 등록
        buildRunDao.addBuildRun(buildRun);

        ObjectMapper mapper = ObjectMapperUtils.getMapper();
        List<BuildStepVO> buildSteps = buildAdd.getBuildSteps();

        // 이전 빌드 정보 조회
        BuildRunVO prevBuildRun = buildRunDao.getBuildRun(buildRunSeq);
        List<BuildStepRunVO> prevBuildStepRuns = new ArrayList<>();
        if(prevBuildRun != null) {
            prevBuildStepRuns = prevBuildRun.getBuildStepRuns();
        }

        // BuildStepVO 데이터로 build_step_run 데이터 생성
        for(BuildStepVO buildStep: buildSteps) {

            // 사용 안하는 정보는 skip
            if(!buildStep.isUseFlag()) {
                continue;
            }

            // 이전 등록된 step run 정보 추출
            Optional<BuildStepRunVO> prevBuildStepRunVOOpt= prevBuildStepRuns.stream().filter( bs -> ( bs.getStepType() == buildStep.getStepType() && bs.getBuildStepRunSeq().equals(buildStep.getBuildStepRunSeq())) ).findFirst();

            /**
             * 빌드 단계 수정
             */
            if (buildStep.getStepType() == StepType.CODE_DOWN){
                StepCodeDownVO codeDownVO = (StepCodeDownVO)buildStep.getBuildStepConfig();

                if(StringUtils.equals("COMMON", codeDownVO.getCommonType())){
                    codeDownVO.setUserId(null);
                    codeDownVO.setPassword(null);
                }

                // http, https, ftp 프로토콜이 url에 존재한다면 삭제
                if(codeDownVO.getRepositoryType() == RepositoryType.GIT){
                    String url = codeDownVO.getRepositoryUrl();
                    if(StringUtils.isNotBlank(url) && Pattern.matches("^(?i)(https?|ftp)://.*$", url)){
                        codeDownVO.setRepositoryUrl( StringUtils.replacePattern(url, "^(?i)(https?|ftp)://", "") );
                    }

                    // code 저장경로 데이터가 없을 경우엔 git repositoryURL에서 추출
                    if ( StringUtils.isEmpty(codeDownVO.getCodeSaveDir()) ) {
                        String gitName = StringUtils.substringAfterLast(codeDownVO.getRepositoryUrl(), "/");
                        String codeSaveDir = gitName.replaceAll("[.](git|GIT)", "");
                        codeDownVO.setCodeSaveDir(codeSaveDir);
                    }
                }

                if(prevBuildStepRunVOOpt.isPresent()){
                    if(StringUtils.isNotBlank(prevBuildStepRunVOOpt.get().getStepConfig())){
                        StepCodeDownVO prevCodeDownVO = mapper.readValue(prevBuildStepRunVOOpt.get().getStepConfig(), new TypeReference<StepCodeDownVO>(){});

                        // password 없이 수정할 경우, 조회된 값을 셋팅
                        if(StringUtils.equalsIgnoreCase("PRIVATE", codeDownVO.getCommonType())){
                            if(StringUtils.isBlank(codeDownVO.getPassword())){
                                codeDownVO.setPassword(prevCodeDownVO.getPassword());
                            }else{
                                codeDownVO.setPassword(CryptoUtils.encryptAES(codeDownVO.getPassword()));
                            }
                        }
                    }
                }

            } else if (buildStep.getStepType() == StepType.FTP) {
                StepFtpVO ftpStepVO = (StepFtpVO) buildStep.getBuildStepConfig();

                // password가 존재 하면 암호화 처리
                if( StringUtils.isNotBlank(ftpStepVO.getPassword()) ){
                    ftpStepVO.setPassword(CryptoUtils.encryptAES(ftpStepVO.getPassword()));

                }else if(prevBuildStepRunVOOpt.isPresent()){ // 기존 데이터가 존재할 경우
                    // 입력값은 null 이고 기존 패스워드가 존재 하면 기존 값으로 대체 한다.
                    StepFtpVO prevFtpStepVO = mapper.readValue(prevBuildStepRunVOOpt.get().getStepConfig(), new TypeReference<StepFtpVO>(){});
                    if( StringUtils.isNotBlank(prevFtpStepVO.getPassword()) ){
                        ftpStepVO.setPassword(prevFtpStepVO.getPassword());
                    }
                }

            } else if (buildStep.getStepType() == StepType.HTTP) {
                StepHttpVO httpStepVO = (StepHttpVO) buildStep.getBuildStepConfig();

                // password가 존재 하면 암호화 처리
                if( StringUtils.isNotBlank(httpStepVO.getPassword()) ){
                    httpStepVO.setPassword(CryptoUtils.encryptAES(httpStepVO.getPassword()));

                }else if(prevBuildStepRunVOOpt.isPresent()){ // 기존 데이터가 존재할 경우
                    // 입력값은 null 이고 기존 패스워드가 존재 하면 기존 값으로 대체 한다.
                    StepHttpVO prevHttpStepVO = mapper.readValue(prevBuildStepRunVOOpt.get().getStepConfig(), new TypeReference<StepHttpVO>(){});
                    if( StringUtils.isNotBlank(prevHttpStepVO.getPassword()) ){
                        httpStepVO.setPassword(prevHttpStepVO.getPassword());
                    }
                }
            }

            // 공통 설정
            buildStep.setStepConfig(JsonUtils.toGson(buildStep.getBuildStepConfig()));
            buildStep.setUseYn(buildStep.isUseFlag() ? "Y" : "N");

            // BuildStepRunVO 데이터 생성
            BuildStepRunVO buildStepRunVO = new BuildStepRunVO();
            BeanUtils.copyProperties(buildStep, buildStepRunVO);

            // build_step_run 데이터 생성
            buildStepRunVO.setBuildRunSeq(buildRun.getBuildRunSeq());
            buildRunDao.addBuildStepRun(buildStepRunVO);

        }

        // logId 셋팅 및 create image step 추가 정보 update
        this.updateLogIdAndCreateImageAddtionalInfo(buildRun);

        // 등록된 BuildRun 내용을 다시 조회
        buildRun = this.getBuildRun(buildRun.getBuildRunSeq());

        return buildRun;
    }

    /**
     * DB 저장할 BuildRun 정보 생성 메서드
     *
     * @param buildVO
     * @param tagName
     * @param callbackUrl
     * @param description
     * @param pipelineSeq
     * @return
     */
    private BuildRunVO createBuildRunVOForBuild(BuildVO buildVO, String tagName, String callbackUrl, String description, Integer pipelineSeq) throws Exception{
        BuildRunVO buildRun = new BuildRunVO();

        buildRun.setBuildSeq(buildVO.getBuildSeq());
        buildRun.setBuildNo(buildVO.getBuildNo());
        buildRun.setRunType(RunType.BUILD);
        buildRun.setRunState(RunState.CREATED);
        buildRun.setRunDesc(description);
        buildRun.setPipelineSeq(pipelineSeq);
        buildRun.setCallbackUrl(BuildUtils.addParamToCallbackURL(callbackUrl));
        buildRun.setTagName(tagName);
        buildRun.setRegistryName(buildVO.getRegistryName());
        buildRun.setImageName(buildVO.getImageName());

        buildRun.setExternalRegistrySeq(buildVO.getExternalRegistrySeq());

        if (buildVO.getExternalRegistrySeq() != null && buildVO.getExternalRegistrySeq() > 0) {
            ExternalRegistryDetailVO externalRegistryDetailVO = externalRegistryService.getExternalRegistry(buildVO.getExternalRegistrySeq(), null);
            buildRun.setImageUrl(BuildUtils.getFullImageUrl(externalRegistryDetailVO.getEndpointUrl(), buildRun));
        }else {
            // imageUrl 생성
            buildRun.setImageUrl(BuildUtils.getFullImageUrl(registryProperties.getUrl(), buildRun));
        }

        /** BuildServerHost & TLS 정보 입력, build_run 정보기반으로 빌드시에는 이 메서드 호출전에 이전 buildRun의 정보로 buildVO의 BuildServerHost가 셋팅 되어야 한다. **/
        buildRun.setBuildServerHost(buildVO.getBuildServerHost());
        buildRun.setBuildServerTlsVerify(buildVO.getBuildServerTlsVerify());
        buildRun.setBuildServerCacrt(buildVO.getBuildServerCacrt());
        buildRun.setBuildServerClientCert(buildVO.getBuildServerClientCert());
        buildRun.setBuildServerClientKey(buildVO.getBuildServerClientKey());

        /** 자동태그 정보 입력, build_run 정보기반으로 빌드시에는 이 메서드 호출전에 이전 buildRun의 정보로 buildVO의 자동태그정보가 셋팅 되어야 한다. **/
        buildRun.setAutotagUseYn(buildVO.getAutotagUseYn());
        buildRun.setAutotagPrefix(buildVO.getAutotagPrefix());
        buildRun.setAutotagSeqType(buildVO.getAutotagSeqType());

        // pipeline seq가 존재하면 관련정보 생성
        if(pipelineSeq != null && pipelineSeq.intValue() > 0) {
            // pipeline 연관정보 조회
            PipelineRelatedInfoVO pipelineRelatedInfo = pipelineFlowService.getPipelineRelatedInfoByContainer(pipelineSeq);
            if(pipelineRelatedInfo != null) {
                RelatedPipelineInfoVO relatedPipelineInfo = new RelatedPipelineInfoVO();
                BeanUtils.copyProperties(pipelineRelatedInfo, relatedPipelineInfo);
                String relatedPipeline = JsonUtils.toGson(relatedPipelineInfo);
                buildRun.setRelatedPipeline(relatedPipeline);
            }
        }

        return buildRun;
    }

    // logId 생성 및 update & create image 추가 정보 update
    private void updateLogIdAndCreateImageAddtionalInfo(BuildRunVO buildRun) throws Exception {
        IBuildRunMapper buildRunDao = sqlSession.getMapper(IBuildRunMapper.class);

        String logId;
        List<BuildStepRunVO> buildStepRuns = buildRunDao.getBuildStepRuns(buildRun.getBuildRunSeq());
        for(BuildStepRunVO buildStepRun: buildStepRuns){
            // BuildStepRunSeq 이용한 logId 생성 및 db update
            logId = BuildUtils.getBuildLogId(buildStepRun.getStepType(), buildRun.getBuildSeq(), buildRun.getBuildRunSeq(), buildStepRun.getBuildStepRunSeq());
            buildRunDao.updateBuildStepRunLogId(buildStepRun.getBuildStepRunSeq(), logId);

            // CREATE_IMAGE 일 경우 step_config를 재설정 후 INSERT
            if(buildStepRun.getStepType() == StepType.CREATE_IMAGE){

                ObjectMapper mapper = ObjectMapperUtils.getMapper();
                StepCreateImageVO createImage = mapper.readValue(buildStepRun.getStepConfig(), new TypeReference<StepCreateImageVO>(){});

                // title 값이 없을때는 임의 Default 타이틀 셋팅
                if(StringUtils.isEmpty(createImage.getStepTitle())){
                    createImage.setStepTitle(BuildConstants.CREATE_IMAGE_STEP_DEFAULT_TITLE);
                }

                if (buildRun.getExternalRegistrySeq() != null && buildRun.getExternalRegistrySeq() > 0) {
                    ExternalRegistryDetailVO externalRegistryDetailVO = externalRegistryService.getExternalRegistry(buildRun.getExternalRegistrySeq(), null);
                    createImage.setRegistryUrl(externalRegistryDetailVO.getEndpointUrl());

                }else {
                    createImage.setRegistryUrl(registryProperties.getUrl());
                }

                createImage.setImageTag(buildRun.getTagName());
                createImage.setImageUrl(buildRun.getImageUrl());

                String createImageStepConfig = JsonUtils.toGson(createImage);
                buildRunDao.updateBuildStepConfig(buildStepRun.getBuildStepRunSeq(), createImageStepConfig);
            }
        }
    }

    @Transactional(transactionManager = "transactionManager")
    public BuildRunVO createBuildRunByBuildRemove(Integer buildSeq, String callbackUrl){

        // 빌드 삭제 위한 buildRun, buildStepRun 정보 생성
        IBuildMapper buildDao = sqlSession.getMapper(IBuildMapper.class);
        IBuildRunMapper buildRunDao = sqlSession.getMapper(IBuildRunMapper.class);

        // build 정보 조회
        BuildVO buildVO = buildDao.getBuild(buildSeq);

        // build_run 추가
        BuildRunVO removeBuildRunVO = new BuildRunVO();
        removeBuildRunVO.setBuildSeq(buildVO.getBuildSeq());
        removeBuildRunVO.setBuildNo(buildVO.getBuildNo());
        removeBuildRunVO.setBuildName(buildVO.getBuildName());
        removeBuildRunVO.setBuildDesc(buildVO.getBuildDesc());
        removeBuildRunVO.setRunType(RunType.REMOVE);
        removeBuildRunVO.setRunState(RunState.CREATED);
        removeBuildRunVO.setCallbackUrl(callbackUrl);
        removeBuildRunVO.setPipelineSeq(0);
        buildRunDao.addBuildRun(removeBuildRunVO);

        // build_step_run 추가, log 처리를 위해서 추가함
        BuildStepRunVO buildDeleteStepRun = new BuildStepRunVO();
        buildDeleteStepRun.setBuildRunSeq(removeBuildRunVO.getBuildRunSeq());
        buildDeleteStepRun.setStepType(StepType.DELETE);
        buildDeleteStepRun.setStepConfig("");
        buildDeleteStepRun.setStepState(StepState.WAIT);
        buildDeleteStepRun.setStepOrder(1);
        buildRunDao.addBuildStepRun(buildDeleteStepRun);

        // return 하기 위해 buildRun 조회
        removeBuildRunVO = buildRunDao.getBuildRun(removeBuildRunVO.getBuildRunSeq());

        return removeBuildRunVO;
    }

    /**
     * Build 삭제 처리 메서드.
     *
     * @param removeBuildRun 빌드 삭제에 대해 생성된 BuildRunVO 가 넘어옴
     */
    public void removeBuild(BuildRunVO removeBuildRun){
        IBuildRunMapper buildRunDao = sqlSession.getMapper(IBuildRunMapper.class);
        IServiceMapper serviceDao = sqlSession.getMapper(IServiceMapper.class);

        // 시작상태로 변경 및 UI로 이벤트 전송
        removeBuildRun.setRunState(RunState.RUNNING);
        this.updateStateAndSendEvent(removeBuildRun);

        try {
            // harbor api client
            IHarborRegistryService harborRegistryService = harborRegistryFactory.getService();

            // pipeline server 호출
            pipelineAPIService.removeBuild(removeBuildRun);

            /** pipeline server 응답의 실패 여부에 상관없이 데이터 삭제 처리 **/
            // registry 에서 이미지 삭제를 위한 정보 조회, 이미지 빌드된 정보가 있을 경우는 항상 존재함.
            List<BuildRunVO> buildRuns = buildRunDao.getBuildRunsByExistsImage(removeBuildRun.getBuildSeq());

            BuildVO build = this.removeBuildData(removeBuildRun.getBuildSeq()); // DB 데이터 삭제

            /** registry repository 삭제, registry 실제 삭제하다 오류가 나도 exception 로그만 찍는다. **/
            if (CollectionUtils.isNotEmpty(buildRuns)) {
                boolean deleteFlag = harborRegistryService.deleteImagesFromProjects(buildRuns.get(0).getRegistryName(), buildRuns.get(0).getImageName());
                log.debug("Deleting image in registry {}.{} :: {}", buildRuns.get(0).getRegistryName(), buildRuns.get(0).getImageName(), deleteFlag);
            }

            // 빌드의 registry를 사용하는 service 조회
            List<Integer> serviceSeqs = null;
            if (removeBuildRun.getExternalRegistrySeq() != null && removeBuildRun.getExternalRegistrySeq() > 0){
                ExternalRegistryDetailVO externalRegistryDetailVO = externalRegistryService.getExternalRegistry(removeBuildRun.getExternalRegistrySeq(), null);
                if (externalRegistryDetailVO.getServices() != null) {
                    serviceSeqs = externalRegistryDetailVO.getServices().stream().map(vo -> vo.getServiceSeq()).collect(Collectors.toList());
                }
            } else {
                serviceSeqs = serviceDao.getServiceSeqsOfProject(removeBuildRun.getAccountSeq(), removeBuildRun.getRegistryProjectId());
            }

            // event 전송
            eventService.sendBuildState(removeBuildRun.getCallbackUrl(), removeBuildRun.getAccountSeq(), serviceSeqs, removeBuildRun.getBuildSeq(), build);

        } catch(Exception e){
            removeBuildRun.setRunState(RunState.ERROR);
            updateStateAndSendEvent(removeBuildRun);
        }

    }

    /**
     * build 데이터 삭제하는 메서드
     *
     * @param buildSeq
     */
    @Transactional(transactionManager = "transactionManager")
    BuildVO removeBuildData(Integer buildSeq){
        IBuildMapper buildDao = sqlSession.getMapper(IBuildMapper.class);
        IBuildRunMapper buildRunDao = sqlSession.getMapper(IBuildRunMapper.class);

        BuildVO build = buildDao.getBuild(buildSeq);

        /**
         * 데이터 삭제 부분, build_run & build_step_run은 실제 데이터를 delete 하고, build & build_step는 use_yn을 'N'으로 update 한다.
         */
        buildRunDao.removeBuildStepRunByBuild(build); // BuildStepRun 삭제
        buildRunDao.removeBuildRunByBuild(build); // BuildRun 삭제

        buildDao.removeBuildSteps(build); // build_step 삭제
        buildDao.removeBuild(build); // build 삭제

        return build;
    }


    @Transactional(transactionManager = "transactionManager")
    public BuildRunVO createBuildRunByBuildCancel(Integer buildRunSeq, String callbackUrl) throws IOException {

        IBuildRunMapper buildRunDao = sqlSession.getMapper(IBuildRunMapper.class);

        // build 정보 조회
        BuildRunVO buildRunVO = buildRunDao.getBuildRun(buildRunSeq);

        // build_run 추가
        BuildRunVO cancelBuildRunVO = new BuildRunVO();
        cancelBuildRunVO.setBuildSeq(buildRunVO.getBuildSeq());
        cancelBuildRunVO.setBuildNo(buildRunVO.getBuildNo());
        cancelBuildRunVO.setRunType(RunType.CANCEL);
        cancelBuildRunVO.setRunState(RunState.CREATED);
        cancelBuildRunVO.setCallbackUrl(callbackUrl);
        cancelBuildRunVO.setTagName(buildRunVO.getTagName());
        cancelBuildRunVO.setImageUrl(buildRunVO.getImageUrl());

        buildRunDao.addBuildRun(cancelBuildRunVO);

        // build_step_run 추가, log 처리를 위해서 추가함
        StepCancelVO cancelVO = new StepCancelVO();
        cancelVO.setStepType(StepType.CANCEL.getCode());
        cancelVO.setRefBuildRunSeq(buildRunSeq);

        BuildStepRunVO buildCancelStepRun = new BuildStepRunVO();
        buildCancelStepRun.setBuildRunSeq(cancelBuildRunVO.getBuildRunSeq());
        buildCancelStepRun.setStepType(StepType.CANCEL);
        buildCancelStepRun.setStepConfig(JsonUtils.toGson(cancelVO)); // json 변환하여 StepConfig 셋팅
        buildCancelStepRun.setStepState(StepState.WAIT);
        buildCancelStepRun.setStepOrder(1);
        buildRunDao.addBuildStepRun(buildCancelStepRun);

        // update logId
        String logId = BuildUtils.getBuildLogId(buildCancelStepRun.getStepType(),buildRunVO.getBuildSeq(), cancelBuildRunVO.getBuildRunSeq(), buildCancelStepRun.getBuildStepRunSeq());
        buildRunDao.updateBuildStepRunLogId(buildCancelStepRun.getBuildStepRunSeq(), logId);

        buildRunVO = this.getBuildRun(cancelBuildRunVO.getBuildRunSeq());

        return buildRunVO;
    }

    /**
     * 빌드 취소 처리 하는 메서드.<br/>
     * PipelineAsyncService 에서 Async로 처리된다.
     *
     * @param cancelBuildRun
     */
    public void cancelBuildRun(BuildRunVO cancelBuildRun){
        // step run 정보 추출
        Integer buildStepRunSeq = cancelBuildRun.getBuildStepRuns().get(0).getBuildStepRunSeq();
        StepState stepState = StepState.RUNNING;

        // 시작상태로 변경 및 UI로 이벤트 전송
        this.updateBuildStepRunState(buildStepRunSeq, stepState);
        cancelBuildRun.setRunState(RunState.RUNNING);
        this.updateStateAndSendEvent(cancelBuildRun);

        // call pipeline server
        pipelineAPIService.stopBuild(cancelBuildRun);

        // set step run state
        if(cancelBuildRun.getRunState() == RunState.DONE){
            stepState = StepState.DONE;
        }else{
            stepState = StepState.ERROR;
        }

        // update buildRun state & event send
        this.updateBuildStepRunState(buildStepRunSeq, stepState);
        this.updateStateAndSendEvent(cancelBuildRun);

        // pipeline 취소 호출 완료시 원본 빌드 실행건 ERROR 로 상태 처리
        if(cancelBuildRun != null && cancelBuildRun.getRunState() == RunState.DONE){
            // 실제 중지하려고 하는 정보 추출
            if (CollectionUtils.isNotEmpty(cancelBuildRun.getBuildStepRuns())) {
                BuildStepRunVO buildStepRun = cancelBuildRun.getBuildStepRuns().get(0);
                if (buildStepRun != null) {
                    StepCancelVO cancelVO = (StepCancelVO)buildStepRun.getBuildStepConfig();

                    if (cancelVO != null) {
                        Integer originCancelBuildRunSeq = cancelVO.getRefBuildRunSeq();

                        try {
                            // 실제 취소하려는 BuildRun 조회
                            BuildRunVO originBuildRun = this.getBuildRun(originCancelBuildRunSeq);
                            if(originBuildRun != null && originBuildRun.getRunState() == RunState.RUNNING) {
                                originBuildRun.setRunState(RunState.ERROR);
                                this.updateStateAndSendEvent(originBuildRun);
                            }
                        } catch (IOException e) {
                            log.error(" original buildrun failed update state(ERROR)", e);
                        }
                    }

                }

            }
        }
    }

    @Transactional(transactionManager = "transactionManager")
    public Integer removeBuildRun(List<BuildRunVO> removeBuildRuns){
        IBuildRunMapper buildRunDao = sqlSession.getMapper(IBuildRunMapper.class);

        int removeCount = 0;
        for(BuildRunVO buildRun : removeBuildRuns){
            buildRunDao.removeBuildStepRun(buildRun); // BuildStepRun remove 처리
            removeCount += buildRunDao.removeBuildRun(buildRun); // BuildRun remove 처리
        }

        return removeCount;
    }

    /**
     * pipeline server로 부터 받은 빌드결과 처리 메서드.<br/>
     * 빌드 실행에 대해서만 호출되고 빌드 실행취소&삭제에 대해서는 호출되지 않는다.
     *
     * @param buildRunSeq
     * @param pipelineResult
     * @throws Exception
     */
    public void handleBuildResult(Integer buildRunSeq, String pipelineResult) throws Exception {

        IServiceMapper serviceDao = sqlSession.getMapper(IServiceMapper.class);

        // pipeline response 생성
        PipelineAPIServiceProto.Pipeline pipeline = PipelineTypeConverter.convertVO(pipelineResult, PipelineAPIServiceProto.Pipeline.newBuilder()).build();

        // pipeline command 생성
        PipelineCommandVO pipelineCommandVO = PipelineTypeConverter.convertToPipelineCommandVO(pipeline);

        // DB 조회
        BuildRunVO buildRun = this.getBuildRun(buildRunSeq);

        // 빌드의 registry를 사용하는 service 조회
        List<Integer> serviceSeqs = null;
        if (buildRun.getExternalRegistrySeq() != null && buildRun.getExternalRegistrySeq() > 0){
            ExternalRegistryDetailVO externalRegistryDetailVO = externalRegistryService.getExternalRegistry(buildRun.getExternalRegistrySeq(), null);
            if (externalRegistryDetailVO.getServices() != null) {
                serviceSeqs = externalRegistryDetailVO.getServices().stream().map(vo -> vo.getServiceSeq()).collect(Collectors.toList());
            }
        } else {
            serviceSeqs = serviceDao.getServiceSeqsOfProject(buildRun.getAccountSeq(), buildRun.getRegistryProjectId());
        }

        /** BuildRun, 빌드 종료 처리, finishAt 존재(빌드종료) 시에만 처리됨 **/
        // 빌드 최종 발송된 결과일 경우 처리, convertToPipelineCommandVO 에서 finishAt 존재할 때만 runState 상태 셋팅됨
        if(pipelineCommandVO.getRunState() != null){

            //  pipeline server에서 build 시작하자 마자 오류가 발행 했을때, 오류로 빌드 실행상태 update.
            if(pipelineCommandVO.getRunState() == RunState.ERROR){
                log.info("Build Error ...... ");
                buildRun.setRunState(RunState.ERROR);
                this.updateStateAndSendEvent(buildRun, serviceSeqs);
                // build-api callback 처리가 안되어 RUNNING 경우 ERROR로 변경 처리
                this.updateBuildStepRunStateWhenErrorOnRunning(buildRunSeq);

            }else if(pipelineCommandVO.getRunState() == RunState.DONE){
                log.info("Build DONE ...... ");
                buildRun.setRunState(RunState.DONE);
                this.updateStateAndSendEvent(buildRun, serviceSeqs);
            }

            return;
        }

        /** 빌드 각 스텝별 처리 **/

        // Current Build Step info
        StepState currentStepState = pipelineCommandVO.getStepState();
        StepType  currentStepType = pipelineCommandVO.getStepType();
        Integer currBuildStepRunSeq = pipelineCommandVO.getBuildStepRunSeq();

        // buildStepRun 상태 update, build done & push running 일때는 update 하지 않는다.
        if(currBuildStepRunSeq != null
                && !(pipeline.getStatus().getCurrTaskType() == PipelineAPIServiceProto.TaskType.BUILD && pipeline.getStatus().getPhase() == PipelineAPIServiceProto.TaskPhase.Succeeded)
                && !(pipeline.getStatus().getCurrTaskType() == PipelineAPIServiceProto.TaskType.PUSH && pipeline.getStatus().getPhase() == PipelineAPIServiceProto.TaskPhase.Running)
        ){
            this.updateBuildStepRunState(currBuildStepRunSeq, currentStepState);
        }

        // RUNNING 일때 Front 에 event 전송
        if(currentStepState == StepState.RUNNING) {
            // Build Step이 시작할 때마다 buildEvent 발송
            eventService.sendBuildRunState(buildRun.getCallbackUrl(), buildRun.getAccountSeq(), serviceSeqs, buildRun.getBuildSeq(), buildRun.getBuildRunSeq(), buildRun);

        } else if(currentStepState == StepState.DONE){ // DONE 처리

            // create image 일때, buildNo && step result update
            if (currentStepType == StepType.CREATE_IMAGE) {

                // digest 값을 얻기 위해 registry 이미지 정보 조회
                String[] imageInfo = StringUtils.split(buildRun.getImageUrl(), "/");

                if (imageInfo != null && imageInfo.length > 0) {

                    String imageDigest = null;
                    long imageSize = 0;

                    try {
                        // R4.7.0부터 빌드 서버에서 digest와 이미지 사이즈 값을 준다.
                        PipelineAPIServiceProto.Push push = pipeline.getSpec().getStagesList().get(0).getTasksList().stream().filter(vo -> vo.hasPush()).findAny().get().getPush();
                        log.debug("Build result : accountSeq={}, buildSeq={}, buildRunSeq={}, push={}", buildRun.getAccountSeq(), buildRun.getBuildSeq(), buildRun.getBuildRunSeq(), JsonUtils.toGson(push));

                        // 빌드에서 주는 Digest 값이 여러개 일 수 있어서, 이미지에 해당하는 건만 가져오도록 필터링함.
                        String checkImageURL = StringUtils.split(buildRun.getImageUrl(), ":")[0];
                        Optional<String> imageDigestOptional = push.getDigestList().stream().filter(s -> s.indexOf(checkImageURL) > -1).findFirst();
                        if (imageDigestOptional.isPresent()) {
                            imageDigest = imageDigestOptional.get().split("@")[1];
                        }
                        // docker-hub일 경우 docker.io가 생략되어 digest에 생성됨.
                        else {
                            String url = StringUtils.removeEnd(StringUtils.replacePattern(push.getUrl(), "^(https?):\\/\\/", ""), "/");
                            String checkImageWithoutUrl = StringUtils.removeStart(checkImageURL, String.format("%s/", url));
                            imageDigestOptional = push.getDigestList().stream().filter(s -> s.indexOf(checkImageWithoutUrl) > -1).findFirst();
                            if (imageDigestOptional.isPresent()) {
                                imageDigest = imageDigestOptional.get().split("@")[1];
                            }
                        }

                        // image size 셋팅
                        imageSize = push.getImageSize();

                    }catch (Exception ex){
                        log.error("Fail get image Infos", ex);
                    }

                    buildRun.setImageDigest(imageDigest);
                    buildRun.setImageSize(imageSize);

                    /*// R4.7.0 이전 로직, Harbor일때 registry 서버 찔러서 이미지 정보 조회해서, digest 와 사이즈 구함.
                    String registryName = imageInfo[1];
                    String imageName = StringUtils.split(imageInfo[2], ":")[0];
                    String tag = StringUtils.split(imageInfo[2], ":")[1];
                    ImageRepositoryTagVO tagVO = registryService.getImageTagInfo(registryName, imageName, tag);
                    if(tagVO.getDigest() != null) buildRun.setImageDigest(tagVO.getDigest());
                    if(tagVO.getSize() > 0) buildRun.setImageSize(tagVO.getSize());*/

                    // 이미지 결과 update
                    this.updateImageBuildResult(buildRun.getBuildRunSeq(), buildRun.getImageUrl(), buildRun.getImageSize(), buildRun.getImageDigest());
                }

            }

        } // DONE 처리 끝

        // log 저장 처리, step 처리가 끝났을때(DONE or ERROR)만 처리
        if( cocktailBuilderProperties.isBuildLogDbSaveEnabled()
                && (currentStepState == StepState.DONE || currentStepState == StepState.ERROR)
        ){
            // currBuildStepRunSeq 존재 할때만 로그 저장
            if(currBuildStepRunSeq != null) {
                runLogService.updateBuildLog(currBuildStepRunSeq);
            }
        }

    }

    public void updateStateAndSendEvent(BuildRunVO buildRun){
        IServiceMapper serviceDao = sqlSession.getMapper(IServiceMapper.class);

        List<Integer> serviceSeqs = null;
        if (buildRun.getExternalRegistrySeq() != null && buildRun.getExternalRegistrySeq() > 0){
            ExternalRegistryDetailVO externalRegistryDetailVO = null;
            try {
                externalRegistryDetailVO = externalRegistryService.getExternalRegistry(buildRun.getExternalRegistrySeq(), null);
                if (externalRegistryDetailVO.getServices() != null) {
                    serviceSeqs = externalRegistryDetailVO.getServices().stream().map(vo -> vo.getServiceSeq()).collect(Collectors.toList());
                }
            } catch (Exception e) {
                log.debug("trace log ", e);
            }

        } else {
            serviceSeqs = serviceDao.getServiceSeqsOfProject(buildRun.getAccountSeq(), buildRun.getRegistryProjectId());
        }

        updateStateAndSendEvent(buildRun, serviceSeqs);
    }

    /**
     * 빌드 상태 처리 및 이벤트 전송 메서드<br/>
     * 1) 빌드 상태 저장 <br/>
     * 2) 빌드 이벤트 전송 <br/>
     *
     * @param buildRun
     * @param serviceSeqs
     */
    private void updateStateAndSendEvent(BuildRunVO buildRun, List<Integer> serviceSeqs){
        IBuildRunMapper buildRunDao = sqlSession.getMapper(IBuildRunMapper.class);

        buildRunDao.updateBuildRunState(buildRun.getBuildRunSeq(), buildRun.getRunState()); // BuildRun 상태 update
        eventService.sendBuildRunState(buildRun.getCallbackUrl(), buildRun.getAccountSeq(), serviceSeqs, buildRun.getBuildSeq(), buildRun.getBuildRunSeq(), buildRun); // event 전송
    }

    /**
     * 해당 build_run의 마지막 build_step_run 의 build_step_run_seq 값을 구하는 메서드
     *
     * @param buildRun
     * @return
     * @throws Exception
     */
    private Integer getLastBuildStepRunSeq(BuildRunVO buildRun) throws Exception {
        // 마지막 Step 정보 추출
        BuildStepRunVO buildStepRun = buildRun.getBuildStepRuns().stream().max( Comparator.comparing(BuildStepRunVO::getStepOrder) ).get();
        Integer buildStepRunSeq = buildStepRun.getBuildStepRunSeq();
        return buildStepRunSeq;
    }

    public int updateBuildRunState(Integer buildRunSeq, RunState runState) {
        IBuildRunMapper buildRunDao = sqlSession.getMapper(IBuildRunMapper.class);
        return buildRunDao.updateBuildRunState(buildRunSeq, runState);
    }

    public int updateBuildStepRunState(Integer buildStepRunSeq, StepState stepState) {
        IBuildRunMapper buildRunDao = sqlSession.getMapper(IBuildRunMapper.class);
        return buildRunDao.updateBuildStepRunState(buildStepRunSeq, stepState);
    }

    public int updateBuildStepRunStateWhenErrorOnRunning(Integer buildRunSeq) {
        IBuildRunMapper buildRunDao = sqlSession.getMapper(IBuildRunMapper.class);
        return buildRunDao.updateBuildStepRunStateWhenErrorOnRunning(buildRunSeq);
    }

    public int updateBuildStepRunLog(Integer buildStepRunSeq, String log) {
        IBuildRunMapper buildRunDao = sqlSession.getMapper(IBuildRunMapper.class);
        return buildRunDao.updateBuildStepRunLog(buildStepRunSeq, log);
    }

    public int updateImageBuildResult(Integer buildRunSeq, String imageUrl, Long imageSize, String digest){
        IBuildRunMapper buildRunDao = sqlSession.getMapper(IBuildRunMapper.class);
        return buildRunDao.updateImageBuildResult(buildRunSeq, imageUrl, imageSize, digest);
    }

    /**
     * account 계정의 동시 build 갯수 체크 하여 build 실행 가능한지 체크.<br/>
     * account_grade 정보의 parallel_build_cnt 값과 account에 속한 실행중인 빌드갯수의 총 합과 비교.<br/>
     * 실행 불가능할 시 Exception 발생
     *
     * @param accountSeq
     * @throws Exception
     */
    public void checkPossibleRunBuildBySystem(Integer accountSeq) throws Exception {
        boolean isPossibleBuildRun = true;
        int configParallelBuildCnt = 0;

        // 현재 시스템 등급의 총 빌드수 조회
        IAccountMapper accountDao = sqlSession.getMapper(IAccountMapper.class);
        AccountGradeVO accountGradeVO = accountDao.getAccountGrade(null, accountSeq, GradeApplyState.APPLY);
        if(accountGradeVO != null) configParallelBuildCnt = accountGradeVO.getParallelBuildCnt();

        // 계정 Grade 정보에 동시빌드수가 없을 경우, 즉 Grade 설정이 안되어 있을 경우는 api서버에 설정된 동시빌드 갯수를 사용한다.
        if(configParallelBuildCnt < 1) {
            configParallelBuildCnt = cocktailBuilderProperties.getDefaultParallelBuildCnt();
        }

        // gradePlan 정보가 등록되어 있을때만 체크, 현재 account에 실행중인 build count 조회 하여 비교.
        if (configParallelBuildCnt > 0){

            // build 실행건수 조회
            int runningBuildCnt = this.getRunningBuildCount(accountSeq, null, null);

            // 현재 실행중인 build 건수가 설정된 동시빌드수 보다 크거나 같을경우 빌드실행 불가
            if (runningBuildCnt >= configParallelBuildCnt) {
                isPossibleBuildRun = false;
            }
        }

        if(!isPossibleBuildRun){
            throw new CocktailException("It Has Exceeded the Maximum Number of Allowed Parallel Build Count.", ExceptionType.ExceededMaximumParallelBuildInAccount);
        }
    }

    /**
     * build_seq & pipeline_seq 별 실행되는 빌드가 있는지 확인 <br/>
     *
     * @param buildSeq
     * @param pipelineSeq pipeline 에서 실행시 0 보다 큰 값이 셋팅, 빌드에서 실행시는 null 이나 0값임.
     * @throws Exception
     */
    public void checkPossibleRunBuildByBuild(Integer buildSeq, Integer pipelineSeq) throws Exception {

        // pipelineSeq가 null 일때는 0으로 셋팅, pipelineSeq 가 null이나 0 인 경우는 Build 화면에서 build 실행 했을때임
        if(pipelineSeq == null) pipelineSeq = 0;

        // build 실행건수 조회
        int runningBuildCnt = this.getRunningBuildCount(null, buildSeq, pipelineSeq);

        // 빌드 실행중인 경우 Exception 처리
        if(runningBuildCnt > 0){
            throw new CocktailException(String.format("Build is running! [buildSeq : %s, pipelineSeq : %s]", buildSeq, pipelineSeq), ExceptionType.InvalidState);
        }
    }

    public int getRunningBuildCount(Integer accountSeq, Integer buildSeq, Integer pipelineSeq){
        IBuildRunMapper buildDao = sqlSession.getMapper(IBuildRunMapper.class);
        int runningBuildCount = buildDao.getRunningBuildCount(accountSeq, buildSeq, pipelineSeq);
        return runningBuildCount;
    }

    public void removeRegistryTagImages(List<BuildRunVO> removeRuns){

        boolean exist = false;
        boolean del = false;

        String registryName = null;
        String imageName = null;
        String tag = null;

        // harbor api client
        IHarborRegistryService harborRegistryService = harborRegistryFactory.getService();

        for(BuildRunVO buildRun : removeRuns) {
            exist = false;
            del = false;

            // regi.acloud.run/cocktail-test-dev/k8s.io/jun-test:0.0.1.B000001
            String[] imageInfo = StringUtils.split(buildRun.getImageUrl(), "/");

            if (imageInfo != null && imageInfo.length > 0) {
                String[] imageSplitInfo = StringUtils.split(buildRun.getImageUrl(), ":");
                registryName = imageInfo[1];
                imageName = StringUtils.removeStart(imageSplitInfo[0], String.format("%s/%s/", imageInfo[0], imageInfo[1]));
                tag = imageSplitInfo[1];

                // 이미지 존재여부
                ImageRepositoryTagVO tagVO = null;
                try {
                    tagVO = harborRegistryService.getImageTagInfo(registryName, imageName, tag);
                } catch (Exception e) {
                    log.error("failed to get image tag info.");
                }

                if(tagVO != null && StringUtils.isNotEmpty(tagVO.getDigest())){
                    exist = true;
                }

                // 이미지 삭제
                if (exist) {
                    del = harborRegistryService.deleteImagesFromProjects(registryName, imageName, tag);
                }
            }

            log.debug(" removeRegistryTagImages [ tag : {}, exist : {}, delete : {} ]", buildRun.getImageUrl(), exist, del);
        }
    }

    /**
     * buildRunSeq 에 해당하는 이미지와 동일한 digest를 가지고 있는 이미지를 registry 에서 찾아서 자기 자신을 제외한 buildRun 리스트를 리턴한다.<br/>
     * 동일한 digest를 가지고 있는 이미지는 registry 에서 삭제시 모두 같이 지워진다.
     *
     * @param buildRunSeq
     * @return
     * @throws Exception
     */
    public List<BuildRunVO> getOtherRemoveRunsFromDB(Integer buildRunSeq) throws Exception{
        IBuildMapper buildDao = sqlSession.getMapper(IBuildMapper.class);
        IBuildRunMapper buildRunDao = sqlSession.getMapper(IBuildRunMapper.class);

        // 삭제할 대상 정보 조회
        BuildRunVO buildRun = buildRunDao.getBuildRun(buildRunSeq);

        List<BuildRunVO> returnHistoryList = new ArrayList<>(); // return value

        // 이미지 정보가 없으면 로직 처리 안함
        if(buildRun.getImageSize().intValue() == 0 || StringUtils.isEmpty(buildRun.getImageUrl())){
            return returnHistoryList;
        }

        // digest 동일한 buildRun 정보 조회
        List<BuildRunVO> buildRuns = buildRunDao.getBuildRunsBySameDigest(buildRun.getBuildSeq(), buildRun.getImageDigest());

        // 삭제 대상건 제외
        if(CollectionUtils.isNotEmpty(buildRuns)){
            for(BuildRunVO tmpBuildRun : buildRuns) {
                if (buildRunSeq.equals(tmpBuildRun.getBuildRunSeq())) {
                    continue;
                }
                returnHistoryList.add(tmpBuildRun);
            }
        }

        return returnHistoryList;
    }

    /**
     * imageUrl 를 이용해 build run 정보 조회
     *
     * @param accountSeq 존재하는 경우만 사용함
     * @param serviceSeq 워크스페이스에 속한 registry 조회인지 판단을 위한 param
     * @param buildSeq 빌드 번호
     * @param imageUrl 필수로 존재 해야하는 값임, full image url, regi.acornsoft.io/library/api-server:3.5.0.B11
     * @return
     */
    public BuildRunVO getBuildRunsByImageUrl(Integer accountSeq, Integer serviceSeq, Integer buildSeq, String imageUrl){
        IBuildRunMapper buildRunDao = sqlSession.getMapper(IBuildRunMapper.class);
        return buildRunDao.getBuildRunByImageUrl(accountSeq, serviceSeq, buildSeq, imageUrl);
    }

    /**
     * 빌드 정보를 조회해서 INIT Step 이 없는 경우는 INIT Step 을 추가한다.<br/>
     *
     * @param buildRunSeq
     */
    public void addInitBuildStepRun(Integer buildRunSeq) {
        IBuildRunMapper buildRunDao = sqlSession.getMapper(IBuildRunMapper.class);
        BuildRunVO buildRun = buildRunDao.getBuildRun(buildRunSeq);  // 빌드 실행정보 조회

        this.addInitBuildStepRun(buildRun);

    }

    public void addInitBuildStepRun(BuildRunVO buildRun) {
        if (buildRun != null && CollectionUtils.isNotEmpty(buildRun.getBuildStepRuns())) {
            Optional<BuildStepRunVO> buildStepRunVOOptional = buildRun.getBuildStepRuns().stream().filter(bsr -> (bsr.getStepType() == StepType.INIT)).findFirst();

            // INIT Step이 없을때는 InitStep 를 추가 한다.
            if (!buildStepRunVOOptional.isPresent()) {
                IBuildRunMapper buildRunDao = sqlSession.getMapper(IBuildRunMapper.class);

                //INIT 단계 추가
                BuildStepRunVO initStepRun = this.createInitStepRun();
                initStepRun.setBuildRunSeq(buildRun.getBuildRunSeq());
                initStepRun.setStepConfig(JsonUtils.toGson(initStepRun.getBuildStepConfig()));

                // 빌드 단계 등록, build_step_run table insert
                buildRunDao.addBuildStepRun(initStepRun);

                // update logId
                String logId = BuildUtils.getBuildLogId(initStepRun.getStepType(), buildRun.getBuildSeq(), buildRun.getBuildRunSeq(), initStepRun.getBuildStepRunSeq());
                buildRunDao.updateBuildStepRunLogId(initStepRun.getBuildStepRunSeq(), logId);

                buildRun.getBuildStepRuns().add(initStepRun);
            }
        }
    }

    private BuildStepRunVO createInitStepRun() {

        //INIT 단계 추가L
        BuildStepRunVO initStepRun = new BuildStepRunVO();
        initStepRun.setBuildStepSeq(0);
        initStepRun.setStepType(StepType.INIT);
        initStepRun.setStepOrder(0);
        initStepRun.setUseFlag(true);

        // ADD step VO set
        BuildStepAddVO add = new BuildStepAddVO();
        add.setStepType("INIT");
        add.setStepTitle("init");
        add.setStepOrder(0);

        initStepRun.setBuildStepConfig(add);

        return initStepRun;
    }


    /**
     * BuildRunVO 의 정보로 BuildAddVO 를 생성해 리턴한다.
     *
     * @param buildRun
     * @return BuildAddVO
     */
    public BuildAddVO convertFromBuildRunToBuildAdd(BuildRunVO buildRun){
        BuildAddVO buildAdd = new BuildAddVO();

        buildAdd.setEditType("U");
        buildAdd.setBuildSeq(buildRun.getBuildSeq());
        buildAdd.setBuildName(buildRun.getBuildName());
        buildAdd.setBuildDesc(buildRun.getBuildDesc());
        buildAdd.setAccountSeq(buildRun.getAccountSeq());
        buildAdd.setRegistryName(buildRun.getRegistryName());
        buildAdd.setRegistryProjectId(buildRun.getRegistryProjectId());
        buildAdd.setExternalRegistrySeq(buildRun.getExternalRegistrySeq());

        // build 서버 정보 셋팅
        buildAdd.setBuildServerHost(buildRun.getBuildServerHost());
        buildAdd.setBuildServerTlsVerify(buildRun.getBuildServerTlsVerify());
        buildAdd.setBuildServerCacrt(buildRun.getBuildServerCacrt());
        buildAdd.setBuildServerClientCert(buildRun.getBuildServerClientCert());
        buildAdd.setBuildServerClientKey(buildRun.getBuildServerClientKey());


        // 자동 태그 정보 설정
        buildAdd.setAutotagUseYn(buildRun.getAutotagUseYn());
        buildAdd.setAutotagPrefix(buildRun.getAutotagPrefix());
        buildAdd.setAutotagSeqType(buildRun.getAutotagSeqType());

        buildAdd.setTagName(buildRun.getTagName());

        buildAdd.setImageName(buildRun.getImageName()); // 파이프라인 빌드 오류 수정, 2022-12-01

        // buildStepRun 정보로 BuildStep 정보 생성
        int stepSeq = 0; // 의미 없지만 없으면 validation 에서 오류 발생되어 필요.
        List<BuildStepVO> buildsteps = new ArrayList<BuildStepVO>();
        for (BuildStepRunVO buildStepRun : buildRun.getBuildStepRuns()){
            BuildStepVO buildStep = new BuildStepVO();
            buildStep.setBuildSeq(buildAdd.getBuildSeq());
            buildStep.setBuildStepSeq(++stepSeq);
            buildStep.setBuildRunSeq(buildStepRun.getBuildRunSeq());
            buildStep.setBuildStepRunSeq(buildStepRun.getBuildStepRunSeq());
            buildStep.setStepType(buildStepRun.getStepType());
            buildStep.setBuildStepConfig(buildStepRun.getBuildStepConfig());
            buildStep.setStepOrder(buildStepRun.getStepOrder());
            buildStep.setUseFlag(buildStepRun.isUseFlag());
            buildsteps.add(buildStep);
        }
        buildAdd.setBuildSteps(buildsteps);

        return buildAdd;
    }

}

