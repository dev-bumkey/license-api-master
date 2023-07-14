package run.acloud.api.pipelineflow.util;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import run.acloud.api.build.enums.DockerFileType;
import run.acloud.api.build.enums.RunState;
import run.acloud.api.build.enums.StepState;
import run.acloud.api.build.enums.StepType;
import run.acloud.api.build.vo.*;
import run.acloud.api.pipelineflow.vo.PipelineCommandVO;
import run.acloud.protobuf.pipeline.PipelineAPIServiceProto;

import java.util.List;

public class PipelineTypeConverter {

    private static final String CREATE_IMAGE_STEP_DEFAULT_TITLE = "Build & Push";

    /**
     * Pipeline Server에 실행 요청을 위한 request 생성 메서드.
     * BuildCommandVO => request 로 만드는 메서드
     *
     * @param pipelineCommandVO
     * @return Pipeline 객체
     */
    public static PipelineAPIServiceProto.Pipeline convertToPipeline(PipelineCommandVO pipelineCommandVO) {

        // Pipeline 생성
        PipelineAPIServiceProto.Pipeline.Builder pipelineBuilder = PipelineAPIServiceProto.Pipeline.newBuilder();

        // Namespace 설정
        pipelineBuilder.setPipelineNamespace(pipelineCommandVO.getPipelineNamespace());

        PipelineAPIServiceProto.PipelineSpec.Builder specBuilder = PipelineAPIServiceProto.PipelineSpec.newBuilder();
        // callback URL 셋팅
        specBuilder.setCallbackUrl(pipelineCommandVO.getCallback());
        specBuilder.setPipelineSeq(pipelineCommandVO.getPipelineSeq());

        // buildServerHost & build server TLS 설정
        specBuilder.setBuildServerHost( StringUtils.defaultString(pipelineCommandVO.getBuildServerHost()) );
        specBuilder.setBuildServerTlsVerify( pipelineCommandVO.isBuildServerTlsVerify() );
        specBuilder.setBuildServerCacrt( StringUtils.defaultString(pipelineCommandVO.getBuildServerCacrt()) );
        specBuilder.setBuildServerClientCert( StringUtils.defaultString(pipelineCommandVO.getBuildServerClientCert()) );
        specBuilder.setBuildServerClientKey( StringUtils.defaultString(pipelineCommandVO.getBuildServerClientKey()) );

        // logServer 정보 생성
        PipelineAPIServiceProto.LogServer.Builder logServerBuilder = PipelineAPIServiceProto.LogServer.newBuilder();
        logServerBuilder.setClusterId(pipelineCommandVO.getLogClusterId());
        logServerBuilder.setPass(pipelineCommandVO.getLogPass());
        logServerBuilder.setUser(pipelineCommandVO.getLogUser());
        logServerBuilder.setUrl(pipelineCommandVO.getLogServerUrl());
        specBuilder.setLogServer(logServerBuilder);

        // stage 정보 설정, task 설정
        PipelineAPIServiceProto.Stage.Builder stageBuilder = PipelineAPIServiceProto.Stage.newBuilder();

        // set Task list
        List<BuildStepRunVO> buildList = pipelineCommandVO.getBuildList();

        if(CollectionUtils.isNotEmpty(buildList)) {
            for (BuildStepRunVO stepRun : buildList) {
                // create image 일때는 registryID, registryPW 셋팅
                if(stepRun.getStepType() == StepType.CREATE_IMAGE){
                    StepCreateImageVO createImageStepRun = (StepCreateImageVO)stepRun.getBuildStepConfig();
                    createImageStepRun.setLoginId(pipelineCommandVO.getRegistryUserId());
                    createImageStepRun.setPassword(pipelineCommandVO.getRegistryUserPassword());
                    // certificate 셋팅
                    if(pipelineCommandVO.getPrivateCertificate() != null) {
                        createImageStepRun.setPrivateCertificate(pipelineCommandVO.getPrivateCertificate());
                    }
                }
                addBuildTask(stageBuilder, stepRun);
            }
        }

        // deploy Task 셋팅, PIPELINE 이면서 배포정보가 셋팅 되었을때만 처리
        if(StringUtils.equals("PIPELINE", pipelineCommandVO.getFromType())
                && (pipelineCommandVO.getClusterSeq() != null && pipelineCommandVO.getClusterSeq().intValue() > 0)
                && StringUtils.isNotEmpty(pipelineCommandVO.getDeployContent())
        ){
            addDeployTask(stageBuilder, pipelineCommandVO);
        }

        // spec 에 stage 추가
        specBuilder.addStages(stageBuilder);

        // pipeline에 spec 추가
        pipelineBuilder.setSpec(specBuilder);

        return pipelineBuilder.build();
    }

    /**
     * Pipeline Server에 서버 중지 요청을 위한 request 생성 메서드.
     * BuildCommandVO => request 로 만드는 메서드
     *
     * @param pipelineCommandVO
     * @return PipelineDeleteRequest 객체
     */
    public static PipelineAPIServiceProto.PipelineStopRequest convertToPipelineStopRequest(PipelineCommandVO pipelineCommandVO) {
        // Pipeline 생성
        PipelineAPIServiceProto.PipelineStopRequest.Builder pipelineStopRequestBuilder = PipelineAPIServiceProto.PipelineStopRequest.newBuilder();
        pipelineStopRequestBuilder.setPipelineNamespace(pipelineCommandVO.getPipelineNamespace());
        pipelineStopRequestBuilder.setPipelineSeq(pipelineCommandVO.getPipelineSeq());

        return pipelineStopRequestBuilder.build();
    }

    public static PipelineAPIServiceProto.PipelineTerminateRequest convertToPipelineTerminateRequest(PipelineCommandVO pipelineCommandVO) {
        // Pipeline 생성
        PipelineAPIServiceProto.PipelineTerminateRequest.Builder terminateRequestBuilder = PipelineAPIServiceProto.PipelineTerminateRequest.newBuilder();
        terminateRequestBuilder.setPipelineNamespace(pipelineCommandVO.getPipelineNamespace());
        terminateRequestBuilder.setPipelineSeq(pipelineCommandVO.getPipelineSeq());

        return terminateRequestBuilder.build();
    }

    public static PipelineCommandVO convertToPipelineCommandVO(PipelineAPIServiceProto.Pipeline pipeline) {
        return convertToPipelineCommandVO(pipeline, null);
    }
    /**
     * Pipeline Server 에서 받은 응답(Pipeline)으로 PipelineCommandVO 객체 생성하는 메서드
     *
     * @param pipeline
     * @return
     */
    public static PipelineCommandVO convertToPipelineCommandVO(PipelineAPIServiceProto.Pipeline pipeline, String fromType) {
        // response 정보 조회
        Integer pipelineSeq = pipeline.getSpec().getPipelineSeq();
        if(pipelineSeq == null) pipelineSeq = 0; // PipelineSeq 가 null 이면 0 으로 셋팅

        // pipelineCommand 셋팅
        PipelineCommandVO pipelineCommandVO = new PipelineCommandVO();
        pipelineCommandVO.setPipelineSeq(pipelineSeq);
        pipelineCommandVO.setFromType("BUILD");

        if ("PL".equals(fromType)) {
            pipelineCommandVO.setFromType("PL");
        } else if(pipelineSeq != null && pipelineSeq > 0){
            pipelineCommandVO.setFromType("PIPELINE");
        }

        pipelineCommandVO.setBuildServerHost(pipeline.getSpec().getBuildServerHost());

        PipelineAPIServiceProto.PipelineStatus status = pipeline.getStatus();
        PipelineAPIServiceProto.TaskType taskType = status.getCurrTaskType();
        if(taskType != null) {
            pipelineCommandVO.setStepType(convertToStepType(taskType));
        }
        pipelineCommandVO.setStepState(convetToStepState(status));

        Integer taskSeq = status.getCurrTaskSeq();
        pipelineCommandVO.setBuildStepRunSeq(taskSeq);

        // finishAt 있을때만 최종 빌드 상태 셋팅
        String finishAt = status.getFinishedAt();
        if(StringUtils.isNotEmpty(finishAt)) {
            if (pipelineCommandVO.getStepState() == StepState.ERROR) {
                pipelineCommandVO.setRunState(RunState.ERROR);
            } else if (pipelineCommandVO.getStepState() == StepState.DONE) {
                pipelineCommandVO.setRunState(RunState.DONE);
            }
        }

        return pipelineCommandVO;
    }

    // StepType에 따른 task 추가
    private static void addBuildTask(PipelineAPIServiceProto.Stage.Builder stageBuilder, BuildStepRunVO buildStepRun){

        // StepType에 따른 처리
        switch (buildStepRun.getStepType()){
            case INIT:
                stageBuilder.addTasks(convertToInit(buildStepRun));
                break;
            case CODE_DOWN:
                stageBuilder.addTasks(convertToCodeDown(buildStepRun));
                break;
            case USER_TASK:
                stageBuilder.addTasks(convertToUserTask(buildStepRun));
                break;
            case FTP:
                stageBuilder.addTasks(convertToFtp(buildStepRun));
                break;
            case HTTP:
                stageBuilder.addTasks(convertToHttp(buildStepRun));
                break;
            case SHELL:
                stageBuilder.addTasks(convertToShell(buildStepRun));
                break;
            case CREATE_IMAGE: // create Image는 Build 와 Push 로 두 개의 task로 분리되어 처리 된다.
                stageBuilder.addTasks(convertToBuild(buildStepRun));
                stageBuilder.addTasks(convertToPush(buildStepRun));
                break;
        }
    }

    // create & add a deploy task
    private static void addDeployTask(PipelineAPIServiceProto.Stage.Builder stageBuilder, PipelineCommandVO pipelineCommandVO){
        // task 정보 생성
        PipelineAPIServiceProto.Task.Builder taskBuilder = PipelineAPIServiceProto.Task.newBuilder();
        taskBuilder.setTaskSeq(pipelineCommandVO.getPipelineRunSeq());
        taskBuilder.setTaskType(PipelineAPIServiceProto.TaskType.DEPLOY);

        // deploy 정보 생성
        PipelineAPIServiceProto.Deploy.Builder deployBuilder = PipelineAPIServiceProto.Deploy.newBuilder();
        deployBuilder.setName("Deploy");
        PipelineAPIServiceProto.ClusterInfo.Builder clusterInfoBuilder = PipelineAPIServiceProto.ClusterInfo.newBuilder();
        clusterInfoBuilder.setClusterSeq(pipelineCommandVO.getClusterSeq().toString());
        clusterInfoBuilder.setUrl(pipelineCommandVO.getClusterUrl());
        deployBuilder.setClusterRef(clusterInfoBuilder);
        deployBuilder.setManifestBytes(ByteString.copyFromUtf8(pipelineCommandVO.getDeployContent()));

        // task에 deploy 정보 설정
        taskBuilder.setDeploy(deployBuilder);
        stageBuilder.addTasks(taskBuilder.build());
    }

    // setting taskType & taskSeq
    private static PipelineAPIServiceProto.Task.Builder makeCommonTask(BuildStepRunVO stepRun, PipelineAPIServiceProto.TaskType taskType){
        PipelineAPIServiceProto.Task.Builder taskBuilder = PipelineAPIServiceProto.Task.newBuilder();
        taskBuilder.setTaskType(taskType);
        taskBuilder.setTaskSeq(stepRun.getBuildStepRunSeq());
        taskBuilder.setLogId(stepRun.getLogId());

        return taskBuilder;
    }

    // convert from id/pw to Credential
    private static PipelineAPIServiceProto.Credential makeCredential(String username, String password){
        PipelineAPIServiceProto.Credential.Builder credentialBuilder = PipelineAPIServiceProto.Credential.newBuilder();
        credentialBuilder.setUsername(username);
        credentialBuilder.setPasswordBytes(ByteString.copyFromUtf8(password));

        return credentialBuilder.build();
    }

    // make a Init Task
    private static PipelineAPIServiceProto.Task convertToInit(BuildStepRunVO stepRun){
        BuildStepAddVO initStepRun = stepRun.getBuildStepConfig();
        PipelineAPIServiceProto.Task.Builder taskBuilder = makeCommonTask(stepRun, PipelineAPIServiceProto.TaskType.INIT);

        PipelineAPIServiceProto.Init.Builder initBuilder = PipelineAPIServiceProto.Init.newBuilder();
        String title = initStepRun.getStepTitle();
        if(StringUtils.isBlank(title)){
            title = "init";
        }
        initBuilder.setName(title);

        taskBuilder.setInit(initBuilder.build());

        return taskBuilder.build();
    }

    // make a CodeDown Task
    private static PipelineAPIServiceProto.Task convertToCodeDown(BuildStepRunVO stepRun){
        StepCodeDownVO codeDownStepRun = (StepCodeDownVO)stepRun.getBuildStepConfig();
        PipelineAPIServiceProto.Task.Builder taskBuilder = makeCommonTask(stepRun, PipelineAPIServiceProto.TaskType.CODE_DOWN);

        PipelineAPIServiceProto.CodeDown.Builder codeDownBuilder = PipelineAPIServiceProto.CodeDown.newBuilder();
        String title = codeDownStepRun.getStepTitle();
        if(StringUtils.isBlank(title)){
            title = "code-down";
        }
        codeDownBuilder.setName(title);
        codeDownBuilder.setBranchName(codeDownStepRun.getBranchName());
        codeDownBuilder.setCodeSaveDir(codeDownStepRun.getCodeSaveDir());
        String repoUrl = codeDownStepRun.getProtocolType() + "://" +codeDownStepRun.getRepositoryUrl();
        codeDownBuilder.setRepo(repoUrl);
        // PRIVATE 이면서 ID/PW 가 존재할 때만 Credential 셋팅
        if(StringUtils.equals(codeDownStepRun.getCommonType(),"PRIVATE") && StringUtils.isNotEmpty(codeDownStepRun.getUserId()) && StringUtils.isNotEmpty(codeDownStepRun.getPassword())) {
            codeDownBuilder.setCredential(makeCredential(codeDownStepRun.getUserId(), codeDownStepRun.getPassword()));
        }
        // 추가로직, 20230530, coolingi 화면에서는 인증서 스킵(무시) 여부, 실제 서버에서는 인증서 검증 여부
        // 서로 사용하는 값이 반대이기 때문에 build 서버에서 내부적으로 false -> true로, true -> false로 변경해서 사용한다.
        codeDownBuilder.setHttpSslVerify(codeDownStepRun.isHttpSslVerifySkip());

        taskBuilder.setCodeDown(codeDownBuilder.build());

        return taskBuilder.build();
    }

    // make user task
    public static PipelineAPIServiceProto.Task convertToUserTask(BuildStepRunVO stepRun){
        StepUserTaskVO userTaskStepRun = (StepUserTaskVO)stepRun.getBuildStepConfig();
        PipelineAPIServiceProto.Task.Builder taskBuilder = makeCommonTask(stepRun, PipelineAPIServiceProto.TaskType.USER_TASK);

        PipelineAPIServiceProto.UserTask.Builder userTaskBuilder = PipelineAPIServiceProto.UserTask.newBuilder();
        String title = userTaskStepRun.getStepTitle();
        if(StringUtils.isBlank(title)){
            title = "user-task";
        }
        userTaskBuilder.setName(title);
        userTaskBuilder.setImage(userTaskStepRun.getImageName() +":"+userTaskStepRun.getImageTag());
        userTaskBuilder.setWorkingDir(userTaskStepRun.getWorkingDir());

        // cmd 셋팅
        if (CollectionUtils.isNotEmpty(userTaskStepRun.getCmd())) {
            for (String cmd : userTaskStepRun.getCmd()) {
                userTaskBuilder.addCmds(cmd);
            }
        }

        // volume mount 셋팅
        if(CollectionUtils.isNotEmpty(userTaskStepRun.getDockerVolumeMountVOList())){
            for(UserTaskVolumeMount vm : userTaskStepRun.getDockerVolumeMountVOList()){
                PipelineAPIServiceProto.Map.Builder mapBuilder = PipelineAPIServiceProto.Map.newBuilder();
                mapBuilder.setKey(vm.getHostPath());
                mapBuilder.setValue(vm.getContainerPath());
                userTaskBuilder.addMounts(mapBuilder.build());
            }
        }

        // envList 셋팅
        if (CollectionUtils.isNotEmpty(userTaskStepRun.getEnvs())) {
            for(UserTaskEnv env : userTaskStepRun.getEnvs()){
                PipelineAPIServiceProto.Map.Builder mapBuilder = PipelineAPIServiceProto.Map.newBuilder();
                mapBuilder.setKey(env.getKey());
                mapBuilder.setValue(env.getValue());
                userTaskBuilder.addEnvs(mapBuilder.build());
            }
        }

        // UserTask 셋팅
        taskBuilder.setUserTask(userTaskBuilder.build());

        return taskBuilder.build();
    }

    private static PipelineAPIServiceProto.Task convertToFtp(BuildStepRunVO stepRun){
        StepFtpVO ftpStepRun = (StepFtpVO)stepRun.getBuildStepConfig();
        PipelineAPIServiceProto.Task.Builder taskBuilder = makeCommonTask(stepRun, PipelineAPIServiceProto.TaskType.FTP_TASK);

        PipelineAPIServiceProto.Ftp.Builder ftpBuilder = PipelineAPIServiceProto.Ftp.newBuilder();
        String title = ftpStepRun.getStepTitle();
        if(StringUtils.isBlank(title)){
            title = "ftp-task";
        }
        ftpBuilder.setName(title);
        ftpBuilder.setFtpExecType(PipelineAPIServiceProto.FtpExecType.valueOf(ftpStepRun.getFtpExecType().getCode()));
        ftpBuilder.setFtpType(PipelineAPIServiceProto.FtpType.valueOf(ftpStepRun.getFtpType().getCode()));
//        String ftpHost = ftpStepRun.getFtpType().getCode().toLowerCase() + "://" + ftpStepRun.getUrl();
//        ftpBuilder.setHost(ftpHost);
        ftpBuilder.setHost(ftpStepRun.getUrl());
        ftpBuilder.setRemoteDirectory(ftpStepRun.getRemoteDirectory());
        ftpBuilder.setSourceFiles(ftpStepRun.getSourceFiles());

        if( StringUtils.isNotEmpty(ftpStepRun.getUsername()) && StringUtils.isNotEmpty(ftpStepRun.getPassword()) ) {
            ftpBuilder.setCredential(makeCredential(ftpStepRun.getUsername(), ftpStepRun.getPassword()));
        }

        taskBuilder.setFtp(ftpBuilder.build());

        return taskBuilder.build();
    }

    private static PipelineAPIServiceProto.Task convertToHttp(BuildStepRunVO stepRun){
        StepHttpVO httpStepRun = (StepHttpVO)stepRun.getBuildStepConfig();
        PipelineAPIServiceProto.Task.Builder taskBuilder = makeCommonTask(stepRun, PipelineAPIServiceProto.TaskType.HTTP_REQUEST);

        PipelineAPIServiceProto.HttpRequest.Builder httpRequestBuilder = PipelineAPIServiceProto.HttpRequest.newBuilder();
        String title = httpStepRun.getStepTitle();
        if(StringUtils.isBlank(title)){
            title = "http-task";
        }
        httpRequestBuilder.setName(title);
        httpRequestBuilder.setIgnoreSslErrors(httpStepRun.isIgnoreSslErrors());
        httpRequestBuilder.setHttpMode(httpStepRun.getHttpMode().getCode());
        httpRequestBuilder.setUrl(httpStepRun.getUrl());
        if(StringUtils.isNotEmpty(httpStepRun.getRequestBody())) {
            httpRequestBuilder.setRequestBodyBytes(ByteString.copyFromUtf8(httpStepRun.getRequestBody()));
        }


        httpRequestBuilder.setConsoleLogResponseBody(httpStepRun.isConsoleLogResponseBody());
        httpRequestBuilder.setValidResponseCodes(StringUtils.defaultString(httpStepRun.getValidResponseCodes()));

        httpRequestBuilder.setValidResponseContent(StringUtils.defaultString(httpStepRun.getValidResponseContent()));
        httpRequestBuilder.setOutputFile(StringUtils.defaultString(httpStepRun.getOutputFile()));
        httpRequestBuilder.setTimeout(httpStepRun.getTimeout());

        // ID/PW 셋팅
        if( StringUtils.isNotEmpty(httpStepRun.getUsername()) && StringUtils.isNotEmpty(httpStepRun.getPassword()) ) {
            httpRequestBuilder.setCredential(makeCredential(httpStepRun.getUsername(), httpStepRun.getPassword()));
        }

        // HTTP Header 셋팅
        if(CollectionUtils.isNotEmpty(httpStepRun.getCustomHeaders())){
            for(HttpHeader header: httpStepRun.getCustomHeaders()){
                PipelineAPIServiceProto.CustomHeader.Builder customHeaderBuilder = PipelineAPIServiceProto.CustomHeader.newBuilder();
                customHeaderBuilder.setName(header.getName());
                customHeaderBuilder.setValueBytes(ByteString.copyFromUtf8(header.getValue()));
                httpRequestBuilder.addCustomHeaders(customHeaderBuilder);
            }
        }
        // task에 셋팅
        taskBuilder.setHttpRequest(httpRequestBuilder.build());

        return taskBuilder.build();
    }

    private static PipelineAPIServiceProto.Task convertToShell(BuildStepRunVO stepRun) {
        StepShellVO shellStepRun = (StepShellVO)stepRun.getBuildStepConfig();
        PipelineAPIServiceProto.Task.Builder taskBuilder = makeCommonTask(stepRun, PipelineAPIServiceProto.TaskType.SHELL);

        PipelineAPIServiceProto.Shell.Builder shellBuilder = PipelineAPIServiceProto.Shell.newBuilder();
        String title = shellStepRun.getStepTitle();
        if(StringUtils.isBlank(title)){
            title = "shell-task";
        }
        shellBuilder.setName(title);
        shellBuilder.setCommandBytes(ByteString.copyFromUtf8(shellStepRun.getCommand()));

        taskBuilder.setShell(shellBuilder.build());

        return taskBuilder.build();
    }

    // make a Build
    public static PipelineAPIServiceProto.Task convertToBuild(BuildStepRunVO stepRun){
        StepCreateImageVO createImageStepRun = (StepCreateImageVO)stepRun.getBuildStepConfig();
        PipelineAPIServiceProto.Task.Builder taskBuilder = makeCommonTask(stepRun, PipelineAPIServiceProto.TaskType.BUILD);

        PipelineAPIServiceProto.Build.Builder buildBuilder = PipelineAPIServiceProto.Build.newBuilder();

        // set title
        String title = CREATE_IMAGE_STEP_DEFAULT_TITLE;
        if(StringUtils.isNotEmpty(createImageStepRun.getStepTitle())){
            title = createImageStepRun.getStepTitle();
        }
        buildBuilder.setName(title);

        if(DockerFileType.FILE_PATH == createImageStepRun.getDockerFileType()){
            buildBuilder.setFile(createImageStepRun.getDockerFilePath());
        } else {
            buildBuilder.setDockerfileBytes(ByteString.copyFromUtf8(createImageStepRun.getDockerFile()));
        }
        buildBuilder.setImage(createImageStepRun.getImageUrl());

        //TODO 추가 정보 입력, 이미지 체크 level 및 종료코드 설정
        String severity = StringUtils.defaultString(createImageStepRun.getSeverity());
        String exitCode = StringUtils.defaultString(createImageStepRun.getExitCode(), "0");

        buildBuilder.setSeverity(severity);
        buildBuilder.setExitCode(exitCode);

        // task에 셋팅
        taskBuilder.setBuild(buildBuilder.build());

        return taskBuilder.build();
    }

    // make a push
    public static PipelineAPIServiceProto.Task convertToPush(BuildStepRunVO stepRun){
        StepCreateImageVO createImageStepRun = (StepCreateImageVO)stepRun.getBuildStepConfig();
        PipelineAPIServiceProto.Task.Builder taskBuilder = makeCommonTask(stepRun, PipelineAPIServiceProto.TaskType.PUSH);

        PipelineAPIServiceProto.Push.Builder pushBuilder = PipelineAPIServiceProto.Push.newBuilder();

        // set title
        String title = CREATE_IMAGE_STEP_DEFAULT_TITLE;
        if(StringUtils.isNotEmpty(createImageStepRun.getStepTitle())){
            title = createImageStepRun.getStepTitle();
        }
        pushBuilder.setName(title);

        pushBuilder.setImage(createImageStepRun.getImageUrl());
        pushBuilder.setName(createImageStepRun.getStepTitle());
        pushBuilder.setUrl(createImageStepRun.getRegistryUrl());

        // id/password 설정
        if( StringUtils.isNotEmpty(createImageStepRun.getLoginId()) && StringUtils.isNotEmpty(createImageStepRun.getPassword()) ) {
            pushBuilder.setCredential(makeCredential(createImageStepRun.getLoginId(), createImageStepRun.getPassword()));
        }

        // private certificate 값 셋팅
        if (createImageStepRun.getPrivateCertificate() != null){
            pushBuilder.setCaCrt(createImageStepRun.getPrivateCertificate());
        }

        taskBuilder.setPush(pushBuilder.build());

        return taskBuilder.build();
    }

    // convert type from TaskType to StepType
    public static StepType convertToStepType(PipelineAPIServiceProto.TaskType taskType){
        StepType stepType = null;

        switch (taskType){
            case INIT:
                stepType = StepType.INIT;
                break;
            case CODE_DOWN:
                stepType = StepType.CODE_DOWN;
                break;
            case USER_TASK:
                stepType = StepType.USER_TASK;
                break;
            case FTP_TASK:
                stepType = StepType.FTP;
                break;
            case HTTP_REQUEST:
                stepType = StepType.HTTP;
                break;
            case SHELL:
                stepType = StepType.SHELL;
                break;
            case BUILD:
                stepType = StepType.CREATE_IMAGE;
                break;
            case PUSH:
                stepType = StepType.CREATE_IMAGE;
                break;
            case DEPLOY:
                stepType = StepType.DEPLOY;
        }
        return stepType;
    }

    /**
     * convert type from PipelineStatus to StepState.
     *
     * CreateImage Step은 pipeline server 에서는 BUILD 와 PUSH 두 단계로 처리되기 때문에 BUILD 일때는 상태가 DONE 이어도 RUNNING로 설정.
     *
     * @param status
     * @return
     */
    public static StepState convetToStepState(PipelineAPIServiceProto.PipelineStatus status){
        StepState stepState = StepState.WAIT;
        PipelineAPIServiceProto.TaskPhase taskPhase = status.getPhase();
        PipelineAPIServiceProto.TaskType taskType = status.getCurrTaskType();
        switch (taskPhase) {
            case Running:
                stepState = StepState.RUNNING;
                break;
            case Succeeded:
                if(taskType == PipelineAPIServiceProto.TaskType.BUILD) {
                    stepState = StepState.RUNNING;
                }else {
                    stepState = StepState.DONE;
                }
                break;
            case Error:
                stepState = StepState.ERROR;
                break;
            case Failed:
                stepState = StepState.ERROR;
                break;
        }
        return stepState;
    }

    public static <T extends com.google.protobuf.Message.Builder> T convertVO(String json, T builder) throws InvalidProtocolBufferException {
        JsonFormat.parser().ignoringUnknownFields().merge(json, builder);
        return builder;
    }
}
