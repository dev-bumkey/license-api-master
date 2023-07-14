package run.acloud.api.build.service;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import run.acloud.api.build.constant.BuildConstants;
import run.acloud.api.build.dao.IBuildMapper;
import run.acloud.api.build.enums.RepositoryType;
import run.acloud.api.build.enums.StepType;
import run.acloud.api.build.event.PipelineBuildEventService;
import run.acloud.api.build.util.BuildUtils;
import run.acloud.api.build.vo.*;
import run.acloud.api.configuration.dao.IAccountMapper;
import run.acloud.api.configuration.enums.GradeApplyState;
import run.acloud.api.configuration.vo.AccountGradeVO;
import run.acloud.api.cserver.dao.IServiceMapper;
import run.acloud.commons.service.RegistryPropertyService;
import run.acloud.commons.util.CryptoUtils;
import run.acloud.commons.util.JsonUtils;
import run.acloud.commons.util.ObjectMapperUtils;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PipelineBuildService {

    @Resource(name = "cocktailSession")
    private SqlSessionTemplate sqlSession;

    @Autowired
    private PipelineBuildEventService eventService;

    @Autowired
    private RegistryPropertyService registryProperties;

    /**
     * R3.5.0 에서 builder 스키마의 build 테이블 정보를 acloud 스키마의 build 테이블로 이관.
     * Pipeline 데이터 migration.
     *
     * 내부적으로 프로시져를 호출함.
     * @update 2021-10-15 메서드 동작안하도록 주석처리, 해당 프로시져 생성 안하도록 cmdb에서 스크립트 삭제
     */
    public void callMigrationBuild(){
//        IBuildMapper buildDao = sqlSession.getMapper(IBuildMapper.class);
//        buildDao.callMigrationBuild();
    }

    public SystemBuildCountVO getSystemBuildCount(Integer accountSeq){
        IBuildMapper buildDao = sqlSession.getMapper(IBuildMapper.class);
        return buildDao.getSystemBuildCount(accountSeq);
    }

    public List<BuildVO> getBuildList(Integer accountSeq, Integer serviceSeq){
        IBuildMapper buildDao = sqlSession.getMapper(IBuildMapper.class);
        return buildDao.getBuildList(accountSeq, serviceSeq, null, null, registryProperties.getUrl());
    }

    public List<BuildVO> getBuildList(Integer accountSeq, List<Integer> registryProjectIds){
        IBuildMapper buildDao = sqlSession.getMapper(IBuildMapper.class);
        return buildDao.getBuildList(accountSeq, null, registryProjectIds, null, registryProperties.getUrl());
    }

    public List<BuildVO> getBuildNames(Integer accountSeq, Integer serviceSeq){
        IBuildMapper buildDao = sqlSession.getMapper(IBuildMapper.class);
        return buildDao.getBuildNames(accountSeq, serviceSeq);
    }

    public BuildImageVO getBuildImages(Integer buildSeq){
        return this.getBuildImages(buildSeq, null, null);
    }

    public BuildImageVO getBuildImages(Integer buildSeq, String orderColumn, String order){
        IBuildMapper buildDao = sqlSession.getMapper(IBuildMapper.class);

        BuildImageVO buildImage = buildDao.getBuildImages(buildSeq);

        if(buildImage != null && CollectionUtils.isNotEmpty(buildImage.getTags())){
            // digest가 없다면 제거
            buildImage.getTags().removeIf(t -> (StringUtils.isBlank(t.getImageDigest())));

            // 각 유형별로 정렬처리
            if (StringUtils.isNotBlank(orderColumn) && StringUtils.isNotBlank(order)) {
                Comparator<BuildImageInfoVO> buildImageByComparator = null;

                if (StringUtils.equalsAnyIgnoreCase(orderColumn, "END_TIME")) {
                    buildImageByComparator = Comparator.comparing(BuildImageInfoVO::getEndTime);
                } else if (StringUtils.equalsAnyIgnoreCase(orderColumn, "TAG")) {
                    buildImageByComparator = Comparator.comparing(BuildImageInfoVO::getTag);
                } else if (StringUtils.equalsAnyIgnoreCase(orderColumn, "IMAGE_SIZE")) {
                    buildImageByComparator = Comparator.comparing(BuildImageInfoVO::getImageSize);
                }

                if (buildImageByComparator != null) {
                    if (StringUtils.equalsAnyIgnoreCase(order, "DESC")) {
                        buildImageByComparator = buildImageByComparator.reversed();
                    }

                    // sort
                    buildImage.getTags().sort(buildImageByComparator);
                }
            }
        }

        return buildImage;
    }

    public BuildVO getBuild(Integer buildSeq) throws Exception {

        return this.getBuild(buildSeq, true);
    }

    public BuildVO getBuild(Integer buildSeq, boolean withoutTLS) throws Exception {
        IBuildMapper buildDao = sqlSession.getMapper(IBuildMapper.class);
        BuildVO build = buildDao.getBuild(buildSeq);

        // INIT Step이 없을때는 InitStep 를 추가
        this.addInitBuildStep(build);

        // build server TLS 정보 제거
        if(withoutTLS) {
            build.setBuildServerCacrt(null);
            build.setBuildServerClientCert(null);
            build.setBuildServerClientKey(null);
        }

        build = this.convertBuildStepConfig(build);

        // step을 stepOrder로 정렬
        build.setBuildSteps(build.getBuildSteps().stream().sorted(Comparator.comparing(BuildStepVO::getStepOrder)).collect(Collectors.toList()));

        return build;
    }

    public BuildVO getBuildWithoutUseYn(Integer buildSeq) throws Exception {
        IBuildMapper buildDao = sqlSession.getMapper(IBuildMapper.class);
        BuildVO build = buildDao.getBuildWithoutUseYn(buildSeq);

        // INIT Step이 없을때는 InitStep 를 추가
        this.addInitBuildStep(build);

        return build;
    }

    public BuildVO convertBuildStepConfig(BuildVO build) throws IOException {

        if(build != null && CollectionUtils.isNotEmpty(build.getBuildSteps())) {
            // 이미지 정보 셋팅
            ObjectMapper mapper = ObjectMapperUtils.getMapper();
            for (BuildStepVO buildStepVO : build.getBuildSteps()) {
                buildStepVO.setBuildStepConfig(mapper.readValue(buildStepVO.getStepConfig(), new TypeReference<BuildStepAddVO>(){}));
                buildStepVO.setStepConfig(null);

                // 조회시 password 값은 null로 셋팅함
                if (buildStepVO.getStepType() == StepType.CODE_DOWN) {
                    StepCodeDownVO downVO = (StepCodeDownVO) buildStepVO.getBuildStepConfig();
                    downVO.setPassword(null);
                    // 코드 저장 디렉토리에 데이터가 없으면(기존 데이터일 경우 "repo" 셋팅, 신규나 수정건은 DB에 없을수가 없음)
                    if (StringUtils.isEmpty(downVO.getCodeSaveDir())) {
                        downVO.setCodeSaveDir("repo");
                    }
                } else if (buildStepVO.getStepType() == StepType.FTP) {
                    StepFtpVO taskFtp = (StepFtpVO) buildStepVO.getBuildStepConfig();
                    taskFtp.setPassword(null);
                } else if (buildStepVO.getStepType() == StepType.HTTP) {
                    StepHttpVO taskHttp = (StepHttpVO) buildStepVO.getBuildStepConfig();
                    taskHttp.setPassword(null);
                } else if (buildStepVO.getStepType() == StepType.CREATE_IMAGE) {
                    StepCreateImageVO buildAndPush = (StepCreateImageVO) buildStepVO.getBuildStepConfig();
                    // StepTitle이 존재 하지 않을때에는 "Build & Push" 로 타이틀 셋팅
                    if (StringUtils.isEmpty(buildAndPush.getStepTitle())){
                        buildAndPush.setStepTitle(BuildConstants.CREATE_IMAGE_STEP_DEFAULT_TITLE);
                    }
                }
            }
        }

        return build;
    }


    /**
     * account 계정의 build 갯수 체크 하여 build 등록 가능한지 체크.<br/>
     * account_grade 정보의 total_build_count 값과 시스템에 등록된 빌드갯수와 비교.<br/>
     * 빌드 실행할 수 없을시 Exception 발생
     *
     * @param accountSeq
     * @throws Exception
     */
    public void checkPossibleCreateBuild(Integer accountSeq) throws Exception {
        boolean isPossibleCreateBuild = true;

        int configTotalBuildCnt = 0;
        int currentTotalBuildCnt = 0;

        // 현재 시스템 등급의 총 빌드수 조회
        IAccountMapper accountDao = sqlSession.getMapper(IAccountMapper.class);
        AccountGradeVO accountGradeVO = accountDao.getAccountGrade(null, accountSeq, GradeApplyState.APPLY);
        if(accountGradeVO != null) configTotalBuildCnt = accountGradeVO.getTotalBuildCnt();

        // 현재 시스템의 등록된 빌드수 조회
        SystemBuildCountVO buildCountVO = this.getSystemBuildCount(accountSeq);
        if(buildCountVO != null){
            currentTotalBuildCnt = buildCountVO.getBuildCount();
        }

        // gradePlan 정보가 등록되어 있을때만 체크, 현재 account에 등록된 build count조회 하여 비교.
        if (configTotalBuildCnt > 0 && (currentTotalBuildCnt >= configTotalBuildCnt)){
            isPossibleCreateBuild = false;
        }

        if(!isPossibleCreateBuild){
            throw new CocktailException("Your Account Has Exceeded the Maximum Number of Allowed total build count.", ExceptionType.ExceededMaximumBuildInAccount);
        }
    }

    @Transactional(transactionManager = "transactionManager")
    public BuildVO addBuild(BuildAddVO buildAdd, String callbackUrl) throws Exception {
        BuildVO build = null;

        IBuildMapper buildDao = sqlSession.getMapper(IBuildMapper.class);
        IServiceMapper dao = sqlSession.getMapper(IServiceMapper.class);

        // build server 정보 존재 & TLS 정보 존재시, TLS 정보 암호화
        if( buildAdd.getBuildServerHost() != null && "Y".equals(buildAdd.getBuildServerTlsVerify()) ){
            buildAdd.setBuildServerCacrt( CryptoUtils.encryptAES(buildAdd.getBuildServerCacrt()) );
            buildAdd.setBuildServerClientCert( CryptoUtils.encryptAES(buildAdd.getBuildServerClientCert()) );
            buildAdd.setBuildServerClientKey( CryptoUtils.encryptAES(buildAdd.getBuildServerClientKey()) );
        }

        // build 생성
        buildDao.addBuild(buildAdd);

        /**
         * 빌드 단계 등록
         */
        // build step 생성
        List<BuildStepVO> buildStepVOList = buildAdd.getBuildSteps();

        //INIT 단계 추가, R3.5.0 에서 INIT 단계는 추가하지 않음
        // BuildStepVO initStepVO = createInitStepVO();
        // buildStepVOList.add(0,initStepVO); // 첫번째에 추가

        for (BuildStepVO buildStepVO : buildStepVOList) {
            // 사용 안함이면 skip
            if (!buildStepVO.isUseFlag()) {
                continue;
            }

            // Password 암호화 및 URL replacing
            if (buildStepVO.getStepType() == StepType.CODE_DOWN) {
                StepCodeDownVO codeDownVO = (StepCodeDownVO) buildStepVO.getBuildStepConfig();

                // Common, Private 에 따른 ID/PW 설정
                if (StringUtils.equals("COMMON", codeDownVO.getCommonType())) { // COMMON
                    codeDownVO.setUserId(null);
                    codeDownVO.setPassword(null);
                } else if (StringUtils.equalsIgnoreCase("PRIVATE", codeDownVO.getCommonType())) { //PRIVATE 일때 Password 암호화
                    if (StringUtils.isNotBlank(codeDownVO.getPassword())) {
                        codeDownVO.setPassword(CryptoUtils.encryptAES(codeDownVO.getPassword()));
                    }
                }

                // http, https, ftp 프로토콜이 url에 존재한다면 삭제
                // code 저장경로가 없다면 셋팅
                if (codeDownVO.getRepositoryType() == RepositoryType.GIT) {
                    String url = codeDownVO.getRepositoryUrl();
                    if (StringUtils.isNotBlank(url) && Pattern.matches("^(?i)(https?|ftp)://.*$", url)) {
                        url = StringUtils.replacePattern(url, "^(?i)(https?|ftp)://", "");
                        codeDownVO.setRepositoryUrl(url);
                    }

                    // code 저장경로 데이터가 없을 경우엔 git repositoryURL에서 추출
                    if ( StringUtils.isEmpty(codeDownVO.getCodeSaveDir()) ) {
                        url = codeDownVO.getRepositoryUrl();
                        String gitName = StringUtils.substringAfterLast(url, "/");
                        String codeSaveDir = gitName.replaceAll("[.](git|GIT)", "");
                        codeDownVO.setCodeSaveDir(codeSaveDir);
                    }
                }
            } else if (buildStepVO.getStepType() == StepType.FTP) {
                StepFtpVO taskFtp = (StepFtpVO) buildStepVO.getBuildStepConfig();

                // password가 존재 하면 암호화 처리
                if( StringUtils.isNotBlank(taskFtp.getPassword()) ){
                    taskFtp.setPassword(CryptoUtils.encryptAES(taskFtp.getPassword()));
                }
            } else if (buildStepVO.getStepType() == StepType.HTTP) {
                StepHttpVO taskHttp = (StepHttpVO) buildStepVO.getBuildStepConfig();

                // password가 존재 하면 암호화 처리
                if( StringUtils.isNotBlank(taskHttp.getPassword()) ){
                    taskHttp.setPassword(CryptoUtils.encryptAES(taskHttp.getPassword()));
                }
            }

            // 공통처리 부분
            buildStepVO.setBuildSeq(buildAdd.getBuildSeq());
            buildStepVO.setStepConfig(JsonUtils.toGson(buildStepVO.getBuildStepConfig()));

            // 빌드 단계 등록, build_step table insert
            buildDao.addBuildStep(buildStepVO);
        }

        build = this.getBuild(buildAdd.getBuildSeq());

        // front로 task 등록 이벤트 전송
        if(StringUtils.isNotBlank(callbackUrl)){
            List<Integer> serviceSeqs = dao.getServiceSeqsOfProject(build.getAccountSeq(), build.getRegistryProjectId() );
            eventService.sendBuildState(callbackUrl, build.getAccountSeq(), serviceSeqs, build.getBuildSeq(), build);
        }

        return build;
    }

    @Transactional(transactionManager = "transactionManager")
    public BuildVO editBuild(BuildAddVO buildAdd) throws Exception {
        IBuildMapper buildDao = sqlSession.getMapper(IBuildMapper.class);

        // build server TLS 정보 셋팅
        BuildVO buildVO = this.getBuild(buildAdd.getBuildSeq(), false);
        BuildUtils.setBuildServerTLSFromBuildToBuildAdd(buildAdd, buildVO);

        // 빌드 수정
        buildDao.editBuild(buildAdd);

        // 빌드 스텝 수정
        BuildVO build = buildDao.getBuild(buildAdd.getBuildSeq()); // 이전 빌드 정보 조회

        ObjectMapper mapper = ObjectMapperUtils.getMapper();
        List<BuildStepVO> buildSteps = buildAdd.getBuildSteps();

        for(BuildStepVO buildStep: buildSteps) {
            BuildStepVO buildStepTarget = new BuildStepVO();
            BeanUtils.copyProperties(buildStep, buildStepTarget);

            // 이전 등록된 step 정보 추출
            Optional<BuildStepVO> prevBuildStepVOOpt= build.getBuildSteps().stream().filter(bs -> ( bs.getStepType() == buildStepTarget.getStepType() && bs.getBuildStepSeq().equals(buildStepTarget.getBuildStepSeq())) ).findFirst();

            /**
             * 생략된 단계는 skip
             */
            if(!buildStep.isUseFlag() && (buildStep.getBuildStepSeq() == null || (buildStep.getBuildStepSeq() != null && buildStep.getBuildStepSeq().intValue() < 1))) {
                continue;
            }

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

                if(prevBuildStepVOOpt.isPresent()){
                    if(StringUtils.isNotBlank(prevBuildStepVOOpt.get().getStepConfig())){
                        StepCodeDownVO prevCodeDownVO = mapper.readValue(prevBuildStepVOOpt.get().getStepConfig(), new TypeReference<StepCodeDownVO>(){});

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

                }else if(prevBuildStepVOOpt.isPresent()){ // 기존 데이터가 존재할 경우
                    // 입력값은 null 이고 기존 패스워드가 존재 하면 기존 값으로 대체 한다.
                    StepFtpVO prevFtpStepVO = mapper.readValue(prevBuildStepVOOpt.get().getStepConfig(), new TypeReference<StepFtpVO>(){});
                    if( StringUtils.isNotBlank(prevFtpStepVO.getPassword()) ){
                        ftpStepVO.setPassword(prevFtpStepVO.getPassword());
                    }
                }

            } else if (buildStep.getStepType() == StepType.HTTP) {
                StepHttpVO httpStepVO = (StepHttpVO) buildStep.getBuildStepConfig();

                // password가 존재 하면 암호화 처리
                if( StringUtils.isNotBlank(httpStepVO.getPassword()) ){
                    httpStepVO.setPassword(CryptoUtils.encryptAES(httpStepVO.getPassword()));

                }else if(prevBuildStepVOOpt.isPresent()){ // 기존 데이터가 존재할 경우
                    // 입력값은 null 이고 기존 패스워드가 존재 하면 기존 값으로 대체 한다.
                    StepHttpVO prevHttpStepVO = mapper.readValue(prevBuildStepVOOpt.get().getStepConfig(), new TypeReference<StepHttpVO>(){});
                    if( StringUtils.isNotBlank(prevHttpStepVO.getPassword()) ){
                        httpStepVO.setPassword(prevHttpStepVO.getPassword());
                    }
                }
            }

            buildStep.setBuildSeq(buildAdd.getBuildSeq());
            buildStep.setStepConfig(JsonUtils.toGson(buildStep.getBuildStepConfig()));
            buildStep.setUseYn(buildStep.isUseFlag() ? "Y" : "N");

            buildDao.addBuildStep(buildStep);  // Query에서 Insert와 Update를 판단하도록 처리하여 editBuildStep을 사용하지 않음.

        }
        // 수정된 Build 내용을 다시 조회
        build = buildDao.getBuild(buildAdd.getBuildSeq());

        return build;
    }

    /**
     * 새로운 buildNo 조회 메서드
     * build의 buildNo 를 하나 증가시킨뒤 해당값을 select하여 리턴한다.
     *
     * @param buildSeq
     * @return
     */
    @Transactional(transactionManager = "transactionManager")
    public Integer getNextBuildNo(Integer buildSeq){
        IBuildMapper buildDao = sqlSession.getMapper(IBuildMapper.class);

        BuildVO buildVO = new BuildVO();
        buildVO.setBuildSeq(buildSeq);

        buildDao.getNextBuildNo(buildVO);
        Integer nextBuildNo = buildVO.getBuildNo();
        return nextBuildNo;
    }

    /**
     * 빌드 정보를 조회해서 INIT Step 이 없는 경우는 INIT Step 을 추가한다.<br/>
     *
     * @param buildSeq
     */
    public void addInitBuildStep(Integer buildSeq) {
        IBuildMapper buildDao = sqlSession.getMapper(IBuildMapper.class);
        BuildVO build = buildDao.getBuild(buildSeq);  // 빌드 실행정보 조회

        this.addInitBuildStep(build);

    }

    public void addInitBuildStep(BuildVO build) {
        if (build != null && CollectionUtils.isNotEmpty(build.getBuildSteps())) {
            Optional<BuildStepVO> buildStepVOOptional = build.getBuildSteps().stream().filter(bsr -> (bsr.getStepType() == StepType.INIT)).findFirst();

            // INIT Step이 없을때는 InitStep 를 추가 한다.
            if (!buildStepVOOptional.isPresent()) {
                IBuildMapper buildDao = sqlSession.getMapper(IBuildMapper.class);

                //INIT 단계 추가
                BuildStepVO initStep = this.createInitStep();
                initStep.setBuildSeq(build.getBuildSeq());
                initStep.setStepConfig(JsonUtils.toGson(initStep.getBuildStepConfig()));

                // 빌드 단계 등록, build_step table insert
                buildDao.addBuildStep(initStep);
                build.getBuildSteps().add(initStep);
            }
        }
    }

    private BuildStepVO createInitStep() {

        //INIT 단계 추가
        BuildStepVO initStep = new BuildStepVO();
        initStep.setStepType(StepType.INIT);
        initStep.setStepOrder(0);
        initStep.setUseFlag(true);

        // ADD step VO set
        BuildStepAddVO add = new BuildStepAddVO();
        add.setStepType(StepType.INIT.getCode());
        add.setStepTitle("init");
        add.setStepOrder(0);

        initStep.setBuildStepConfig(add);

        return initStep;
    }

}
