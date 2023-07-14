package run.acloud.api.pipelineflow.service;

import io.grpc.StatusRuntimeException;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import run.acloud.api.build.enums.RunState;
import run.acloud.api.build.vo.BuildRunVO;
import run.acloud.api.build.vo.BuildStepRunVO;
import run.acloud.api.build.vo.StepCancelVO;
import run.acloud.api.configuration.service.ExternalRegistryService;
import run.acloud.api.configuration.vo.ExternalRegistryDetailVO;
import run.acloud.api.configuration.vo.ServiceDetailVO;
import run.acloud.api.cserver.dao.IServiceMapper;
import run.acloud.api.pipelineflow.constants.PipelineFlowConstant;
import run.acloud.api.pipelineflow.enums.PipelineRunState;
import run.acloud.api.pipelineflow.util.PipelineAPIServerClient;
import run.acloud.api.pipelineflow.util.PipelineTypeConverter;
import run.acloud.api.pipelineflow.vo.PipelineCommandVO;
import run.acloud.api.pipelineflow.vo.PipelineContainerVO;
import run.acloud.api.resource.util.ResourceUtil;
import run.acloud.commons.service.RegistryPropertyService;
import run.acloud.commons.util.CryptoUtils;
import run.acloud.framework.context.ContextHolder;
import run.acloud.framework.properties.CocktailBuilderProperties;
import run.acloud.protobuf.pipeline.PipelineAPIServiceProto;

import java.io.IOException;

@Slf4j
@Service
public class PipelineAPIService {

    @Resource(name = "cocktailSession")
    private SqlSessionTemplate sqlSession;

    private WrappedBuildService buildService;

    @Autowired
    public void setBuildService(WrappedBuildService buildService) {
        this.buildService = buildService;
    }

    private Environment environment;
    @Autowired
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    private CocktailBuilderProperties cocktailBuilderProperties;
    @Autowired
    public void setCocktailBuilderProperties(CocktailBuilderProperties cocktailBuilderProperties) {
        this.cocktailBuilderProperties = cocktailBuilderProperties;
    }

    private ExternalRegistryService externalRegistryService;
    @Autowired
    public void setExternalRegistryService(ExternalRegistryService externalRegistryService) {
        this.externalRegistryService = externalRegistryService;
    }

    private RegistryPropertyService registryPropertyService;
    @Autowired
    public void setRegistryPropertyService(RegistryPropertyService registryPropertyService) {
        this.registryPropertyService = registryPropertyService;
    }

    public boolean checkPipelineServer() {
        try {
            PipelineAPIServerClient pipelineApiClient = new PipelineAPIServerClient(cocktailBuilderProperties.getBuildApiUrl(), cocktailBuilderProperties.getBuildApiPort());
            pipelineApiClient.shutdown();
        } catch (InterruptedException ire) {
            log.error("BuildCall check Fail.", ire);
            return false;
        }
        return true;
    }

    /**
     * 파이프라인에서 빌드 & 배포 처리 하는 메서드
     *
     * @param pipelineContainerVO
     * @return
     */
    public PipelineContainerVO runPipeline(PipelineContainerVO pipelineContainerVO){

        PipelineAPIServerClient buildApiClient = null;
        PipelineRunState runState;

        PipelineAPIServiceProto.Pipeline pipelineRequest = null;
        PipelineAPIServiceProto.Pipeline response = null;
        try {
            BuildRunVO buildRun = buildService.getBuildRunWithPasswd(pipelineContainerVO.getBuildRunSeq());

            // commandVO 생성
            PipelineCommandVO pipelineCommand = makeCommandForBuildAndDeploy(buildRun);

            // pipeline 연동 request 생성
            pipelineRequest = PipelineTypeConverter.convertToPipeline(pipelineCommand);

            // pipelineServer 호출
            buildApiClient = new PipelineAPIServerClient(cocktailBuilderProperties.getBuildApiUrl(), cocktailBuilderProperties.getBuildApiPort());
            response = buildApiClient.runPipeline(pipelineRequest);

            // response 처리
            PipelineAPIServiceProto.PipelineStatus status = response.getStatus();

            // build-api 서버에서 init 이 무쟈게 빠르면(이미 빌드 서버가 생성되어 있는 경우) Running 상태가 아닌, Succeeded 상태가 올 수 있음
            // 2022.02.23, Succeeded 상태 추가, coolingi
            if(status.getPhase() == PipelineAPIServiceProto.TaskPhase.Running || status.getPhase() == PipelineAPIServiceProto.TaskPhase.Succeeded){
                runState = PipelineRunState.RUNNING;
            }else{
                runState = PipelineRunState.ERROR;
            }

            log.debug("BuildCall : \n request : {},\n response : {}",pipelineRequest, response);
        }
        catch (StatusRuntimeException | IOException ioe){
            log.error("BuildCall Fail : \n request : {},\n response : {}",pipelineRequest, response, ioe);
            runState = PipelineRunState.ERROR;
        } finally {
            if (buildApiClient != null) {
                try {
                    buildApiClient.shutdown();
                } catch (InterruptedException e) {
                    log.error("fail runPipeline shutdown()!!", e);
                }
            }
        }

        pipelineContainerVO.setBuildState(runState);

        return pipelineContainerVO;
    }

    /**
     * 빌드에서 호출하는 메서드.
     * 빌드만 처리 하는 메서드임.
     *
     * @param buildRun
     * @return
     * @throws Exception
     */
    public BuildRunVO runBuild(BuildRunVO buildRun) {

        PipelineAPIServerClient buildApiClient = null;
        RunState runState;

        PipelineAPIServiceProto.Pipeline pipelineRequest = null;
        PipelineAPIServiceProto.Pipeline response = null;
        try {
            // commandVO 생성
            PipelineCommandVO pipelineCommand = makeCommandForBuildAndDeploy(buildRun);

            // pipeline 연동 request 생성
            pipelineRequest = PipelineTypeConverter.convertToPipeline(pipelineCommand);

            // pipelineServer 호출
            buildApiClient = new PipelineAPIServerClient(cocktailBuilderProperties.getBuildApiUrl(), cocktailBuilderProperties.getBuildApiPort());
            response = buildApiClient.runPipeline(pipelineRequest);

            // response 데이터 익호 확인 필요
            PipelineAPIServiceProto.PipelineStatus status = response.getStatus();
            // build-api 서버에서 init 이 무쟈게 빠르면(이미 빌드 서버가 생성되어 있는 경우) Running 상태가 아닌, Succeeded 상태가 올 수 있음
            // 2022.02.23, Succeeded 상태 추가, coolingi
            if(status.getPhase() == PipelineAPIServiceProto.TaskPhase.Running || status.getPhase() == PipelineAPIServiceProto.TaskPhase.Succeeded){
                runState = RunState.RUNNING;
            }else{
                runState = RunState.ERROR;
            }

            log.debug("BuildCall : \n request : {},\n response : {}",pipelineRequest, response);
        }
        catch (StatusRuntimeException | IOException ioe) {
            log.error("BuildCall Fail : \n request : {},\n response : {}",pipelineRequest, response, ioe);
            runState = RunState.ERROR;
        } finally {
            if (buildApiClient != null) {
                try {
                    buildApiClient.shutdown();
                } catch (InterruptedException ire) {
                    log.error("BuildCall Close Fail : \n request : {},\n response : {}",pipelineRequest, response, ire);
                }
            }
        }

        // 상태 설정
        buildRun.setRunState(runState);

        return buildRun;
    }

    // 빌드 취소
    public BuildRunVO stopBuild(BuildRunVO buildRun){
        // pipelineServer 호출
        PipelineAPIServerClient buildApiClient = null;
        RunState runState;

        PipelineAPIServiceProto.Pipeline stopRequest = null;
        PipelineAPIServiceProto.Pipeline response = null;

        try {

            // 빌드 중지를 위한 commandVO 생성, 실제 실행되고 있는 빌드의 정보를 조회해 PipelineCommandVO 를 생성한다.
            PipelineCommandVO pipelineCommand = makeCommandForStop(buildRun);

            // pipeline 연동 request 생성
            stopRequest = PipelineTypeConverter.convertToPipeline(pipelineCommand);

            // pipelineServer 호출
            buildApiClient = new PipelineAPIServerClient(cocktailBuilderProperties.getBuildApiUrl(), cocktailBuilderProperties.getBuildApiPort());
            response = buildApiClient.stopBuild(stopRequest);

            // response 처리
            PipelineAPIServiceProto.PipelineStatus status = response.getStatus();
            if(status.getPhase() == PipelineAPIServiceProto.TaskPhase.Succeeded){
                runState = RunState.DONE;
            }else{
                runState = RunState.ERROR;
            }

            log.debug("BuildCall : \n request : {},\n response : {}",stopRequest, response);
        }
        catch (StatusRuntimeException | IOException ioe) {
            log.error("BuildCall Fail : \n request : {},\n response : {}", stopRequest, response, ioe);
            runState = RunState.ERROR;
        } finally {
            if (buildApiClient != null) {
                try {
                    buildApiClient.shutdown();
                } catch (InterruptedException e) {
                    log.error("fail stopBuild shutdown()!!", e);
                }
            }
        }

        // 상태 설정
        buildRun.setRunState(runState);

        return buildRun;
    }

    // 빌드 instance 삭제
    public BuildRunVO terminateBuild(BuildRunVO buildRun){
        // pipelineServer 호출
        PipelineAPIServerClient buildApiClient = null;
        RunState runState;

        PipelineCommandVO pipelineCommand = null;
        PipelineAPIServiceProto.PipelineTerminateResponse response = null;
        try {

            // 빌드 삭제를 위한 commandVO 생성
            pipelineCommand = makeCommandForBuildRemove(buildRun);

            buildApiClient = new PipelineAPIServerClient(cocktailBuilderProperties.getBuildApiUrl(), cocktailBuilderProperties.getBuildApiPort());
            response = callTerminateBuild(buildApiClient, pipelineCommand);

            // response 처리
            PipelineAPIServiceProto.TaskPhase phase = response.getPhase();
            if(phase == PipelineAPIServiceProto.TaskPhase.Succeeded){
                runState = RunState.DONE;
            }else{
                runState = RunState.ERROR;
            }

            log.debug("BuildCall : \n command : {},\n response : {}",pipelineCommand, response);
        }
        catch (StatusRuntimeException sre) {
            log.error("BuildCall Fail : \n command : {},\n response : {}", pipelineCommand, response, sre);
            runState = RunState.ERROR;
        }finally {
            if (buildApiClient != null) {
                try {
                    buildApiClient.shutdown();
                } catch (InterruptedException e) {
                    log.error("fail terminateBuild shutdown()!!", e);
                }
            }
        }

        // 상태 설정
        buildRun.setRunState(runState);

        return buildRun;
    }

    // pipeline build instance 삭제
    public PipelineRunState terminatePipelineBuild(PipelineContainerVO container){
        // pipelineServer 호출
        PipelineAPIServerClient buildApiClient = null;
        PipelineRunState runState;

        PipelineCommandVO pipelineCommand = null;
        PipelineAPIServiceProto.PipelineTerminateResponse response = null;

        try {

            // 빌드 삭제를 위한 commandVO 생성
            pipelineCommand = this.makeCommandForPipelineRemove(container);

            buildApiClient = new PipelineAPIServerClient(cocktailBuilderProperties.getBuildApiUrl(), cocktailBuilderProperties.getBuildApiPort());
            response = callTerminateBuild(buildApiClient, pipelineCommand);

            // response 처리
            PipelineAPIServiceProto.TaskPhase phase = response.getPhase();
            if(phase == PipelineAPIServiceProto.TaskPhase.Succeeded){
                runState = PipelineRunState.DONE;
            }else{
                runState = PipelineRunState.ERROR;
            }

        }
        catch (StatusRuntimeException sre) {
            log.error("BuildCall Fail : \n command : {},\n response : {}", pipelineCommand, response, sre);
            runState = PipelineRunState.ERROR;
        }
        finally {
            if (buildApiClient != null) {
                try {
                    buildApiClient.shutdown();
                } catch (InterruptedException e) {
                    log.error("fail terminatePipelineBuild shutdown()!!", e);
                }
            }
        }

        return runState;
    }

    private PipelineAPIServiceProto.PipelineTerminateResponse callTerminateBuild(PipelineAPIServerClient buildApiClient, PipelineCommandVO pipelineCommand){
        // pipeline 연동 request 생성
        PipelineAPIServiceProto.PipelineTerminateRequest terminateRequest = PipelineTypeConverter.convertToPipelineTerminateRequest(pipelineCommand);

        return buildApiClient.removeBuild(terminateRequest);
    }

    /**
     * Build 와 Deploy 를 위한 pipelineCommand 생성
     *
     * @param buildRunVO
     * @return
     * @throws Exception
     */
    private PipelineCommandVO makeCommandForBuildAndDeploy(BuildRunVO buildRunVO) throws IOException {
        PipelineCommandVO pipelineCommandVO = new PipelineCommandVO();
        BuildRunVO buildRun = buildService.getBuildRunWithPasswd(buildRunVO.getBuildRunSeq());

        // pipeline seq 가 존재하면 파이프라인에서 실행한 빌드임.
        if(buildRun.getPipelineSeq() != null && buildRun.getPipelineSeq() > 0){
            pipelineCommandVO.setFromType("PIPELINE");
        } else if (ContextHolder.exeContext().getPlSeq() != null && ContextHolder.exeContext().getPlSeq() > 0){
            pipelineCommandVO.setFromType("PL");
        } else {
            pipelineCommandVO.setFromType("BUILD");
        }

        // build server & tls info setting
        pipelineCommandVO.setBuildServerHost(buildRun.getBuildServerHost());
        pipelineCommandVO.setBuildServerTlsVerify(StringUtils.equals("Y",buildRun.getBuildServerTlsVerify()));
        pipelineCommandVO.setBuildServerCacrt(buildRun.getBuildServerCacrt());
        pipelineCommandVO.setBuildServerClientCert(buildRun.getBuildServerClientCert());
        pipelineCommandVO.setBuildServerClientKey(buildRun.getBuildServerClientKey());

        pipelineCommandVO.setBuildSeq(buildRun.getBuildSeq());
        pipelineCommandVO.setBuildRunSeq(buildRun.getBuildRunSeq());
        pipelineCommandVO.setPipelineSeq(buildRun.getPipelineSeq());
        pipelineCommandVO.setBuildList(buildRun.getBuildStepRuns());

        this.setPipelineNamespace(pipelineCommandVO);
        this.setCallbackUrl(pipelineCommandVO);
        this.setLogServerInfo(pipelineCommandVO);

        // registry ID/PW 셋팅
        this.setRegistryCredential(pipelineCommandVO);

        return pipelineCommandVO;
    }

    private PipelineCommandVO makeCommandForStop(BuildRunVO cancelBuildRun) throws IOException {
        PipelineCommandVO cancelPipelineCommandVO = new PipelineCommandVO();

        // 실제 중지하려고 하는 정보 추출
        BuildStepRunVO buildStepRun = cancelBuildRun.getBuildStepRuns().get(0);
        StepCancelVO cancelVO = (StepCancelVO)buildStepRun.getBuildStepConfig();
        Integer cancelBuildRunSeq = cancelVO.getRefBuildRunSeq();

        // 실제 취소하려는 BuildRun 조회
        BuildRunVO buildRun = buildService.getBuildRun(cancelBuildRunSeq);

        // pipeline seq 가 존재하면 파이프라인에서 실행한 빌드임.
        if(buildRun.getPipelineSeq() != null && buildRun.getPipelineSeq() > 0){
            cancelPipelineCommandVO.setFromType("PIPELINE");
        } else if (ContextHolder.exeContext().getPlSeq() != null && ContextHolder.exeContext().getPlSeq() > 0){
            cancelPipelineCommandVO.setFromType("PL");
        } else {
            cancelPipelineCommandVO.setFromType("BUILD");
        }

        // build server & tls info setting
        cancelPipelineCommandVO.setBuildServerHost(buildRun.getBuildServerHost());
        cancelPipelineCommandVO.setBuildServerTlsVerify(StringUtils.equals("Y",buildRun.getBuildServerTlsVerify()));
        cancelPipelineCommandVO.setBuildServerCacrt(buildRun.getBuildServerCacrt());
        cancelPipelineCommandVO.setBuildServerClientCert(buildRun.getBuildServerClientCert());
        cancelPipelineCommandVO.setBuildServerClientKey(buildRun.getBuildServerClientKey());

        cancelPipelineCommandVO.setBuildSeq(buildRun.getBuildSeq());
        cancelPipelineCommandVO.setBuildRunSeq(buildRun.getBuildRunSeq());
        cancelPipelineCommandVO.setPipelineSeq(buildRun.getPipelineSeq());
        cancelPipelineCommandVO.setBuildList(buildRun.getBuildStepRuns());

        this.setPipelineNamespace(cancelPipelineCommandVO);
        this.setLogServerInfo(cancelPipelineCommandVO);

        return cancelPipelineCommandVO;
    }

    // 빌드 삭제를 위한 PipelineCommandVO 생성
    private PipelineCommandVO makeCommandForBuildRemove(BuildRunVO deleteBuildRun){
        PipelineCommandVO deletePipelineCommandVO = new PipelineCommandVO();

        deletePipelineCommandVO.setFromType("BUILD");
        deletePipelineCommandVO.setBuildSeq(deleteBuildRun.getBuildSeq());
        deletePipelineCommandVO.setBuildRunSeq(deleteBuildRun.getBuildRunSeq());
        deletePipelineCommandVO.setPipelineSeq(deleteBuildRun.getPipelineSeq());

        this.setPipelineNamespace(deletePipelineCommandVO);

        return deletePipelineCommandVO;
    }

    private PipelineCommandVO makeCommandForPipelineRemove(PipelineContainerVO deleteContainer){
        PipelineCommandVO deletePipelineCommandVO = new PipelineCommandVO();

        deletePipelineCommandVO.setFromType("PIPELINE");
        deletePipelineCommandVO.setBuildSeq(deleteContainer.getBuildSeq());
        deletePipelineCommandVO.setPipelineSeq(deleteContainer.getPipelineContainerSeq());

        this.setPipelineNamespace(deletePipelineCommandVO);

        return deletePipelineCommandVO;
    }

    // 서버구분위한 namespace 생성 및 설정
    private void setPipelineNamespace(PipelineCommandVO pipelineCommandVO){
        String resourcePrefix = ResourceUtil.getResourcePrefix();

        // pipeline 서버에서 서버 생성의 기준이 되는 namespace, cocktail 별 unique key(resourcePrefix)-buildSeq-PipelineContainerSeq()
        String pipelineNamespace = String.format("%s-%d-%d", resourcePrefix, pipelineCommandVO.getBuildSeq(), pipelineCommandVO.getPipelineSeq());
        pipelineCommandVO.setPipelineNamespace(pipelineNamespace);
    }

    // callback url 설정
    private void setCallbackUrl(PipelineCommandVO pipelineCommandVO){
        String serverURL = "api-server";
        String serverPort = environment.getProperty("local.server.port");

        String callbackUrl;
        if(StringUtils.equals("PIPELINE", pipelineCommandVO.getFromType())){
            String callbackStr = String.format(PipelineFlowConstant.PIPELINE_RESULT_RECEIVE_URL, serverURL, serverPort);
            callbackUrl = callbackStr.replaceAll("[{]pipelineContainerSeq[}]",pipelineCommandVO.getPipelineSeq().toString());

        }else if(StringUtils.equals("PL", pipelineCommandVO.getFromType())){

            String callbackStr = String.format(PipelineFlowConstant.PL_RESULT_RECEIVE_URL, serverURL, serverPort);

            Integer plSeq = ContextHolder.exeContext().getPlSeq();
            Integer plRunSeq = ContextHolder.exeContext().getPlRunSeq();
            Integer plRunBuildSeq = ContextHolder.exeContext().getPlRunBuildSeq();

            callbackStr = callbackStr.replaceAll("[{]plSeq[}]", plSeq.toString());
            callbackStr = callbackStr.replaceAll("[{]plRunSeq[}]",plRunSeq.toString());
            callbackUrl = callbackStr.replaceAll("[{]plRunBuildSeq[}]",plRunBuildSeq.toString());

        }else {
            String callbackStr = String.format(PipelineFlowConstant.BUILD_RESULT_RECEIVE_URL, serverURL, serverPort);
            callbackUrl = callbackStr.replaceAll("[{]buildRunSeq[}]",pipelineCommandVO.getBuildRunSeq().toString());
        }

        pipelineCommandVO.setCallback(callbackUrl);
    }

    // build log 서버 정보 셋팅
    private void setLogServerInfo(PipelineCommandVO pipelineCommandVO){
        pipelineCommandVO.setLogServerUrl(cocktailBuilderProperties.getBuildQueueUrl());
        pipelineCommandVO.setLogClusterId(cocktailBuilderProperties.getBuildQueueCid());
        pipelineCommandVO.setLogUser(cocktailBuilderProperties.getBuildQueueUser());
        pipelineCommandVO.setLogPass(cocktailBuilderProperties.getBuildQueuePasswd());
    }

    // registry push 위한 id/pw 셋팅
    private void setRegistryCredential(PipelineCommandVO pipelineCommandVO) throws IOException {
        IServiceMapper serviceDao = sqlSession.getMapper(IServiceMapper.class);

        // build 정보 조회
        BuildRunVO buildRun = buildService.getBuildRun(pipelineCommandVO.getBuildRunSeq());

        String registryUserId = null;
        String registryPasswd = null;
        String privateCertificate = null;

        if (buildRun.getRegistryProjectId() > 0) { // 내부 registry 사용시
            // registry owner serviceSeq 조회
            Integer serviceSeq = serviceDao.getServiceSeqByProjectId(buildRun.getAccountSeq(), buildRun.getRegistryProjectId());

            // service 정보 조회
            ServiceDetailVO serviceVO = serviceDao.getService(serviceSeq);

            registryUserId = serviceVO.getRegistryUserId();
            registryPasswd = serviceVO.getRegistryUserPassword();

            // privateCertificate 설정, 2021-11-26, registryPropertyService.getPrivateCertificate() 에서 복호화 까지 처리하기 때문에 별도 복호화 하지 않는다.
            if(BooleanUtils.toBoolean(registryPropertyService.getPrivateCertificateUseYn(buildRun.getAccountSeq()))){
                privateCertificate = registryPropertyService.getPrivateCertificate(buildRun.getAccountSeq());
            }

            // 복호화 처리
            if(StringUtils.isNotEmpty(registryUserId)){
                registryUserId = CryptoUtils.decryptAES(registryUserId);
            }
            if(StringUtils.isNotEmpty(registryPasswd)){
                registryPasswd = CryptoUtils.decryptAES(registryPasswd);
            }

        } else if(buildRun.getExternalRegistrySeq() > 0){ // 외부 레지스트리 사용시
            ExternalRegistryDetailVO externalRegistryDetail = externalRegistryService.getExternalRegistry(buildRun.getExternalRegistrySeq(), null);

            if (externalRegistryDetail != null) {
                registryUserId = externalRegistryDetail.getAccessId();
                registryPasswd = externalRegistryDetail.getAccessSecret();

                // privateCertificate 설정, 2021-11-26
                // privateCertificate 조회 수정, 2023-01-16, DB 상에 암호화 되어 저장되어 있기 때문에 외부 레지스트리 인증서 조회후 복호화처리 해야함.
                if (BooleanUtils.toBoolean(externalRegistryDetail.getPrivateCertificateUseYn())) {
                    privateCertificate = externalRegistryDetail.getPrivateCertificate();
                }

                // 복호화 처리
                if(StringUtils.isNotEmpty(registryUserId)){
                    registryUserId = CryptoUtils.decryptAES(registryUserId);
                }
                if(StringUtils.isNotEmpty(registryPasswd)){
                    registryPasswd = CryptoUtils.decryptAES(registryPasswd);
                }
                // 외부 레지스트리 사설 인증서 정보 복호화 처리
                if(StringUtils.isNotEmpty(privateCertificate)){
                    privateCertificate = CryptoUtils.decryptAES(privateCertificate);
                }

            }

        }

        // registry ID/PW 조회 및 셋팅, private 인증서 정보 셋팅
        pipelineCommandVO.setRegistryUserId(registryUserId);
        pipelineCommandVO.setRegistryUserPassword(registryPasswd);
        pipelineCommandVO.setPrivateCertificate(privateCertificate);

    }

}
