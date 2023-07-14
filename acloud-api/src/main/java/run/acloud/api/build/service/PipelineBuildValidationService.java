package run.acloud.api.build.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.util.Lists;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.validation.Validator;
import run.acloud.api.auth.enums.UserRole;
import run.acloud.api.build.dao.IBuildMapper;
import run.acloud.api.build.dao.IBuildServerMapper;
import run.acloud.api.build.enums.RepositoryType;
import run.acloud.api.build.enums.StepType;
import run.acloud.api.build.util.BuildUtils;
import run.acloud.api.build.vo.*;
import run.acloud.api.configuration.dao.IAccountMapper;
import run.acloud.api.configuration.vo.AccountVO;
import run.acloud.api.cserver.enums.StateCode;
import run.acloud.api.cserver.service.ServerStateService;
import run.acloud.api.cserver.vo.ServerStateVO;
import run.acloud.api.resource.vo.ComponentVO;
import run.acloud.commons.service.HarborRegistryFactoryService;
import run.acloud.commons.service.IHarborRegistryService;
import run.acloud.commons.util.JsonUtils;
import run.acloud.commons.util.ObjectMapperUtils;
import run.acloud.framework.context.ContextHolder;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PipelineBuildValidationService {

    @Resource(name = "cocktailSession")
    private SqlSessionTemplate sqlSession;

    @Autowired
    private HarborRegistryFactoryService harborRegistryFactory;

    @Autowired
    private ServerStateService serverStateService;


    public void checkBuildByAdd(Validator buildAddValidator, BuildAddVO buildAdd) throws Exception {
        this.checkBuildByAdd(buildAddValidator, buildAdd, null);
    }

    /**
     * build server 정보 체크, 빌드 생성시만 체크
     *
     * @param buildAdd
     * @throws Exception
     */
    public void checkBuildServerTLSInfo(run.acloud.api.build.vo.BuildAddVO buildAdd, BuildVO prevBuildVO, BuildRunVO prevBuildRunVO) throws Exception {
        // 빌드 서버 TLS를 처름으로 사용 한다고 했을때, 값이 없으면 오류
        if ("Y".equals(buildAdd.getBuildServerTlsVerify()) && buildAdd.getBuildServerHost() != null){
            if( "N".equals(buildAdd.getEditType())
                    || ( prevBuildVO != null && !"Y".equals(prevBuildVO.getBuildServerTlsVerify()) )
                    || ( prevBuildRunVO != null && !"Y".equals(prevBuildRunVO.getBuildServerTlsVerify()) )
            ){
                if ( buildAdd.getBuildServerCacrt() == null || buildAdd.getBuildServerClientCert() == null || buildAdd.getBuildServerClientKey() == null){
                    String errMsg = "Invalid build server TLS infos.";
                    throw new CocktailException(errMsg, ExceptionType.InvalidParameter, errMsg);
                }

            }
        }

    }

    public void checkUseInternalBuildServer(Integer accountSeq, String buildServerHost) throws Exception {
        AccountVO account = null;
        if (accountSeq != null && accountSeq > 0) {
            IAccountMapper aDao = sqlSession.getMapper(IAccountMapper.class);
            account = aDao.getAccount(accountSeq);
        } else {
            account = ContextHolder.exeContext().getUserAccount();
        }

        this.checkUseInternalBuildServer(account, buildServerHost);
    }

    /**
     * 내부 빌드서버 사용여부 체크
     *
     * @param account
     * @param buildServerHost
     * @throws Exception
     */
    public void checkUseInternalBuildServer(AccountVO account, String buildServerHost) throws Exception {
        if (account != null) {
            if (account.getAccountConfig() != null) {
                // 내부 빌드서버 사용여부 = N 이고 Host 선택값이 없다면 오류
                if ( !BooleanUtils.toBoolean(account.getAccountConfig().getInternalBuildServerUseYn())
                        && StringUtils.isBlank(buildServerHost) ) {
                    String errMsg = "Please select a build server.";
                    throw new CocktailException(errMsg, ExceptionType.InvalidParameter, errMsg);
                }
            }
        } else {
            String errMsg = "Platform not found.";
            throw new CocktailException(errMsg, ExceptionType.InvalidParameter, errMsg);
        }
    }

    /**
     * 빌드 서버 상태 체크
     *
     * @param accountSeq
     * @param buildServerHost
     * @throws Exception
     */
    public void chekcBuildServerStatus(Integer accountSeq, String buildServerHost) throws Exception {
        if (StringUtils.isNotBlank(buildServerHost) && StringUtils.startsWith(buildServerHost, "topic:")) {

            String buildServerName = StringUtils.split(buildServerHost, ":")[1];
            Integer serviceSeq = null;

            if (!UserRole.valueOf(ContextHolder.exeContext().getUserRole()).isAdmin()) {
                serviceSeq = ContextHolder.exeContext().getUserServiceSeq();// workspace seq
            }

            List<BuildServerVO> buildServers;

            IBuildServerMapper bsDao = sqlSession.getMapper(IBuildServerMapper.class);

            if (serviceSeq != null && serviceSeq > 0){
                buildServers = bsDao.getBuildServerListForRef(serviceSeq, buildServerName);
            } else {
                buildServers = bsDao.getBuildServerList(accountSeq, buildServerName);
            }

            if (CollectionUtils.isNotEmpty(buildServers)) {
                BuildServerVO buildServer = buildServers.get(0);
                ServerStateVO serverStates = serverStateService.getWorkloadsStateInNamespace(buildServer.getClusterSeq(), buildServer.getNamespace(), buildServer.getControllerName(), true, ContextHolder.exeContext());
                boolean isValid = true;
                if (serverStates != null) {
                    String state = Optional.ofNullable(serverStates.getComponents()).orElseGet(Lists::newArrayList)
                                        .stream()
                                        .filter(c -> (
                                                buildServer.getClusterSeq().equals(c.getClusterSeq())
                                                && StringUtils.equals(buildServer.getNamespace(), c.getNamespaceName())
                                                && StringUtils.equals(buildServer.getControllerName(), c.getComponentName())
                                        ))
                                        .map(ComponentVO::getStateCode)
                                        .findFirst().orElseGet(() -> null);

                    if (!StringUtils.equalsIgnoreCase(state, StateCode.RUNNING.getCode())) {
                        isValid = false;
                    }
                } else {
                    isValid = false;
                }

                if (!isValid) {
                    String errMsg = String.format("The build server status is undeployable. - [%s]", buildServer.getBuildServerName());
                    throw new CocktailException(errMsg, ExceptionType.ResourceNotFound, errMsg);
                }
            } else {
                String errMsg = "The currently deployed build server does not exist.";
                throw new CocktailException(errMsg, ExceptionType.ResourceNotFound, errMsg);
            }
        }
    }

    /**
     * Build 생성/수정 시 체크 수행
     *
     * @param buildAddValidator
     * @param buildAdd
     * @param result
     * @throws Exception
     */
    public void checkBuildByAdd(Validator buildAddValidator, BuildAddVO buildAdd, BindingResult result) throws Exception {
        if (result == null) {
            result = new BeanPropertyBindingResult(buildAdd, "BuildAddVO");
        }

        /**
         * buildAdd 파라미터 validation 체크
         */
        buildAddValidator.validate(buildAdd, result);

        /**
         * validation error 확인
         */
        this.getValidatorErrors(result);

        /**
         * build tag check
         */
        this.checkTag(buildAdd);

        /**
         * build names check
         */
        this.checkNames(buildAdd);


        /**
         * Down Step validation 체크
         */
        this.checkCodeDownStep(buildAdd);

        /**
         * Create Image Dockerfile validation
         * 2019.10.14 추가
         */
        this.checkImageDockerfile(buildAdd);

        /**
         * Create Image Step Count validation
         * 2019.03.11 추가
         */
        this.checkImageStepCount(buildAdd);

        /**
         * 레지스트리, 이미지, 태그 중복 체크
         * cf) 수정시 현재 등록된 것(buildSeq)을 제외하고 체크
         */
        this.checkImageName(buildAdd);
    }

    /**
     * validation error 확인
     *
     * @param result
     * @throws Exception
     */
    public void getValidatorErrors(BindingResult result) throws Exception{
        if(result.hasErrors()){
            List<String> errorList = new ArrayList<>();
            Map<String, Object> errorRow;
            ObjectMapper mapper = ObjectMapperUtils.getMapper();
            for (ObjectError error : result.getAllErrors()) {
                errorRow = mapper.readValue(JsonUtils.toGson(error), new TypeReference<Map<String, Object>>(){});
                errorRow.remove("bindingFailure");
                errorRow.remove("objectName");
                errorRow.remove("codes");
                errorList.add(JsonUtils.toGson(errorRow));
            }

            throw new CocktailException(JsonUtils.toGson(errorList), ExceptionType.InvalidParameter);
        }
    }

    // build 명, tag 명, registry 명을 체크한다. 소문자 & 숫자 & -._ 만 가능
    public void checkNames(run.acloud.api.build.vo.BuildAddVO buildAdd) throws CocktailException {
        if ( buildAdd.getImageName() != null && !BuildUtils.isValidImageName(buildAdd.getImageName()) ){
            throw new CocktailException("Invalid ImageName.", ExceptionType.InvalidParameter);
        }
        if ( buildAdd.getRegistryName() != null && !BuildUtils.isValidName(buildAdd.getRegistryName()) ){
            throw new CocktailException("Invalid RegistryName.", ExceptionType.InvalidParameter);
        }
        if ( buildAdd.getTagName() != null && !BuildUtils.isValidName(buildAdd.getTagName()) ){
            throw new CocktailException("Invalid TagName.", ExceptionType.InvalidParameter);
        }
    }

    /**
     * tag 유형에 따른 validation 체
     *
     * @param buildAdd
     * @throws Exception
     */
    private void checkTag(run.acloud.api.build.vo.BuildAddVO buildAdd) throws Exception {
        // 자동태그 인데 필수 값이 없거나 자동태그 아닌데 tagName이 없으 오류면
        if( "Y".equals(buildAdd.getAutotagUseYn()) ){
            if ( buildAdd.getAutotagPrefix() == null || buildAdd.getAutotagSeqType() == null){
                throw new CocktailException("Invalid Autotag infos.", ExceptionType.InvalidParameter);
            }
        }else{
            if ( buildAdd.getTagName() == null ){
                throw new CocktailException("Invalid TagName.", ExceptionType.InvalidParameter);
            }
        }
    }

    private void checkCodeDownStep(run.acloud.api.build.vo.BuildAddVO buildAdd) throws Exception{

        // 단건 체크로직에서 여러건 체크로직으로 변경
        List<BuildStepVO> stepList = buildAdd.getBuildSteps().stream().filter(bs -> (bs.getStepType() == StepType.CODE_DOWN)).collect(Collectors.toList());

        for(BuildStepVO stepVO : stepList){
            if(stepVO.isUseFlag()){
                StepCodeDownVO codeDownVO = (StepCodeDownVO)stepVO.getBuildStepConfig();

                if(!this.checkUsernameByRepo(codeDownVO)){
                    throw new CocktailException("Invalid UserId.", ExceptionType.InvalidParameter);
                }
            }
        }

    }

    private boolean checkUsernameByRepo(StepCodeDownVO codeDownVO) throws Exception{
        boolean result = true;

        if(StringUtils.equalsIgnoreCase("PRIVATE", codeDownVO.getCommonType())) {
            if (codeDownVO.getRepositoryType() == RepositoryType.GIT) {
                String userName = codeDownVO.getUserId();
                if (StringUtils.isNotBlank(userName)) {
                    if (StringUtils.length(userName) == 1) {
                        if (!Pattern.matches("^[_\\w]", userName)) {
                            result = false;
                        }
                    } else if (StringUtils.length(userName) == 2) {
                        if (!Pattern.matches("^[_.\\w][-_\\w]$", userName)) {
                            result = false;
                        }
                    } else {
                        if (!Pattern.matches("^(?:[_.\\w])(?:[-_.\\w])*(?:[-_\\w])$", userName)) {
                            result = false;
                        }
                    }

                } else {
                    result = false;
                }
            }
        }

        return result;
    }

    private void checkImageDockerfile(BuildAddVO buildAdd) throws Exception{

        // 단건 체크로직에서 여러건 체크로직으로 변경
        List<BuildStepVO> stepList = buildAdd.getBuildSteps().stream().filter(bs -> (bs.getStepType() == StepType.CREATE_IMAGE)).collect(Collectors.toList());

        for(BuildStepVO stepVO : stepList){
            if(stepVO.isUseFlag()){
                StepCreateImageVO createImageVO = (StepCreateImageVO)stepVO.getBuildStepConfig();

                if(StringUtils.isEmpty(createImageVO.getDockerFile()) && StringUtils.isEmpty(createImageVO.getDockerFilePath())){
                    throw new CocktailException("Invalid dockerFile or dockerFilePath.", ExceptionType.InvalidParameter);
                }
            }
        }

    }

    private void checkImageStepCount(BuildAddVO buildAdd) throws Exception{

        long imageCount = buildAdd.getBuildSteps().stream().filter(bs -> (bs.getStepType() == StepType.CREATE_IMAGE && bs.isUseFlag())).collect(Collectors.counting());

        if(imageCount != 1){
            throw new CocktailException("Invalid Create Image Step Count.", ExceptionType.BuildAddCreateImageStepFail);
        }

    }

    public void checkImageName(BuildAddVO buildAdd) throws Exception{
        Optional<BuildStepVO> createImageStepVOOptional = buildAdd.getBuildSteps().stream().filter(bs -> (bs.getStepType() == run.acloud.api.build.enums.StepType.CREATE_IMAGE)).findFirst();
        if(createImageStepVOOptional.isPresent()){
            // harbor api client
            IHarborRegistryService harborRegistryService = harborRegistryFactory.getService();

            StepCreateImageVO createImageVO = (StepCreateImageVO)createImageStepVOOptional.get().getBuildStepConfig();
            /**
             * Harbor 에서 태그를 제외한 이미지명 체크 ( cocktail-common/test-image )
             */
            boolean isExist = false;
            // 내부 일때만 체크
            if(StringUtils.equals(buildAdd.getEditType(), "N") && buildAdd.getRegistryProjectId() > 0){
                if(harborRegistryService.isRegistryImagesCheck(createImageVO.getRegistryName(), createImageVO.getImageName())){
                    isExist = true;
                }
            }else if(StringUtils.equals(buildAdd.getEditType(), "U") && buildAdd.getRegistryProjectId() > 0){
                // 수정되는 정보와 기존 빌드 정보의 이미지 저장 정보 다를 경우, 변경할 수 있는지 registry 체크
                BuildVO currentBuild = this.getBuild(buildAdd.getBuildSeq());
                if(currentBuild != null){
                    if(!StringUtils.equals(currentBuild.getRegistryName(), createImageVO.getRegistryName()) || !StringUtils.equals(currentBuild.getImageName(), createImageVO.getImageName())){
                        if(harborRegistryService.isRegistryImagesCheck(createImageVO.getRegistryName(), createImageVO.getImageName())){
                            isExist = true;
                        }
                    }
                }
            }

            if(isExist){
                throw new CocktailException(String.format("이미 레지스트리에 등록된 이미지명입니다. 다른 이미지명을 입력하시기 바랍니다.(레지스트리: %s, 이미지: %s)", createImageVO.getRegistryName(), createImageVO.getImageName()), ExceptionType.IsExistsRegistryImageName);
            }

            //TODO 2021-05-10 registryProjectId 와 externalRegistrySeq 를 조건 추가 필요, createImageVO.getImageTag() 는 조건에서 제외 필요
            /**
             * 현재 사용 중인 DB이 등록된 태그(빌드번호 제외)를 포함한 이미지명 체크
             * R3.5.0 에서는 이력으로 빌드 실행시 체크하다 오류 발생됨.
             * 이력을 통해 빌드 실행 생성될때는 BuildStepVO 에 BuildStepRunSeq 존재함.
             * 이력을 통해 빌드 실행 생성될때는 체크 안하도록 buildStepVOOptional.get().getBuildStepRunSeq() == null 조건 추가
             */
            if(createImageStepVOOptional.get().getBuildStepRunSeq() == null && this.checkImageName(createImageVO.getRegistryName(), createImageVO.getImageName(), buildAdd.getRegistryProjectId(), buildAdd.getExternalRegistrySeq(), buildAdd.getBuildSeq()) > 0){
                throw new CocktailException(String.format("이미 등록된 이미지입니다.(레지스트리: %s, 이미지: %s, RegistryProjectId: %s, , ExternalRegistrySeq: %s 중복)", createImageVO.getRegistryName(), createImageVO.getImageName(), buildAdd.getRegistryProjectId(), buildAdd.getExternalRegistrySeq()), ExceptionType.IsExistsRegistryImageName);
            }
        }
    }

    private BuildVO getBuild(Integer buildSeq) throws Exception {
        IBuildMapper buildDao = sqlSession.getMapper(IBuildMapper.class);
        return buildDao.getBuild(buildSeq);
    }

    public int checkImageName(String registryName, String imageName, Integer registryProjectId, Integer externalRegistrySeq, Integer buildSeq) throws Exception {
        IBuildMapper buildDao = sqlSession.getMapper(IBuildMapper.class);
        if (registryProjectId == 0){
            registryProjectId = null;
        }
        if (externalRegistrySeq != null && externalRegistrySeq == 0){
            externalRegistrySeq = null;
        }
        return buildDao.checkImageName(registryName, imageName, registryProjectId, externalRegistrySeq, buildSeq);
    }
}
