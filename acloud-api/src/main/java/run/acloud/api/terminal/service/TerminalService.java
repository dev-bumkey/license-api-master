package run.acloud.api.terminal.service;


import com.google.common.base.Preconditions;
import io.kubernetes.client.Exec;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1ContainerStatus;
import io.kubernetes.client.openapi.models.V1Pod;
import jakarta.annotation.Resource;
import jakarta.websocket.EncodeException;
import jakarta.websocket.Session;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.mybatis.spring.SqlSessionTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;
import run.acloud.api.auth.enums.UserRole;
import run.acloud.api.auth.vo.UserVO;
import run.acloud.api.configuration.dao.IClusterMapper;
import run.acloud.api.configuration.service.UserService;
import run.acloud.api.configuration.vo.ClusterVO;
import run.acloud.api.configuration.vo.ServiceDetailVO;
import run.acloud.api.cserver.service.ServiceService;
import run.acloud.api.cserver.vo.ServiceRelationVO;
import run.acloud.api.resource.task.K8sTokenGenerator;
import run.acloud.api.resource.task.K8sWorker;
import run.acloud.api.terminal.enums.TerminalSessionStatus;
import run.acloud.api.terminal.vo.TerminalMessage;
import run.acloud.api.terminal.vo.TerminalSession;
import run.acloud.commons.provider.K8sClient;
import run.acloud.commons.util.JsonUtils;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;
import run.acloud.framework.properties.CocktailTerminalProperties;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@EnableAsync
public class TerminalService {

    public static final List<String> shellList = Arrays.asList("sh", "bash", "powershell", "cmd");
    private static ConcurrentHashMap<String, TerminalSession> sessionRegistry = new ConcurrentHashMap<String, TerminalSession>();

    private static Logger terminalLogger = LoggerFactory.getLogger("terminal.logger");

    @Resource(name = "cocktailSession")
    private SqlSessionTemplate sqlSession;

    @Autowired
    private K8sClient k8sClients;

    @Autowired
    private K8sTokenGenerator k8sTokenGenerator;

    @Autowired
    private CocktailTerminalProperties cocktailTerminalProperties;

    @Autowired
    private K8sWorker k8sWorker;

    @Autowired
    private UserService userService;

    @Autowired
    private ServiceService serviceService;

    // cluster 정보 조회
    public ClusterVO getCluster(int clusterSeq){
        IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
        ClusterVO cluster = clusterDao.getCluster(clusterSeq);
        return cluster;
    }

    public ApiClient createK8sClient(ClusterVO clusterVO){

        // token 방식 cluster일 경우 만료체크후 token update 처리
        k8sTokenGenerator.refreshClusterToken(clusterVO);
        
        // K8s Client 생성
        ApiClient k8sClient = (ApiClient) k8sClients.createNoCache(
                clusterVO.getAuthType(),
                clusterVO.getCubeType(),
                clusterVO.getApiUrl(),
                clusterVO.getApiKey(),
                clusterVO.getApiSecret(),
                clusterVO.getClientAuthData(),
                clusterVO.getClientKeyData(),
                clusterVO.getServerAuthData());

//        k8sClient.setDebugging(true);
        // setting read, write timeout, between api to k8s, 10분
        terminalLogger.debug("k8sClient.getHttpClient().getConnectTimeout() : {}", k8sClient.getHttpClient().connectTimeoutMillis());
        k8sClient.setReadTimeout(this.cocktailTerminalProperties.getTerminalConnectionTimeout() * 1000);
        k8sClient.setWriteTimeout(this.cocktailTerminalProperties.getTerminalConnectionTimeout() * 1000);

        return k8sClient;
    }

    /**
     * Terminal 연결할 container가 정상적으로 기동되어 있는지 체크 하는 메서드
     *
     * @param clusterVO
     * @param terminalSession
     * @return
     * @throws Exception
     */
    public boolean checkLivePodAndContainer(ClusterVO clusterVO, TerminalSession terminalSession) throws Exception {
        boolean checkSucc = false;
        V1Pod v1Pod = k8sWorker.getPodV1WithName(clusterVO, terminalSession.getNamespace(), terminalSession.getPodName());
        if(v1Pod != null) {
            String containerName = terminalSession.getContainerName();
            List<V1ContainerStatus> containerStatusList = v1Pod.getStatus().getContainerStatuses();
            for (V1ContainerStatus containerStatus : containerStatusList) {
                // 해당 컨테이너의 상태가 ready 일경우 true 설정
                if (containerStatus.getName().equals(containerName) && containerStatus.getReady()) {
                    checkSucc = true;
                }
            }
        }
        terminalLogger.debug("WebTerminal Container state check [{}/{}/{}] : {}",terminalSession.getNamespace(), terminalSession.getPodName(), terminalSession.getContainerName(), checkSucc);
        return checkSucc;
    }

    public Process createK8sExec(TerminalSession terminalSession, String shell) throws IOException, ApiException {

        if(!shellList.contains(shell)){
            return null;
        }

//        TerminalExec exec = new TerminalExec(terminalSession.getK8sClient());
        Exec exec = new Exec(terminalSession.getK8sClient());
        final Process proc = exec.exec(
                terminalSession.getNamespace(),
                terminalSession.getPodName(),
                new String[] {shell},  // shell 타입을 정의한다. bash or sh 중 선택가능하나 bash 일경우는 오류가 나서 sh 로 설정해 놨음
                terminalSession.getContainerName(),
                true,
                true);
        return proc;
    }

    // Websocket 커넥션 max 갯수 체크해서 접속 가능 여부 리턴
    public boolean availableConnectCount(){
        return cocktailTerminalProperties.getTerminalMaxConnection() > sessionRegistry.size();
    }

    public ConcurrentHashMap<String, TerminalSession> getSessionRegistry(){
        return sessionRegistry;
    }

    // WebSocket Session을 등록한다.
    public void registSession(TerminalSession sessionVO){
        // setting max Idel timeout, between web to api, 5분
        sessionVO.getSession().setMaxIdleTimeout(this.cocktailTerminalProperties.getTerminalConnectionTimeout()*1000);
        sessionRegistry.put(sessionVO.getSession().getId(), sessionVO);
    }

    public void writerMsgToK8s(String sessionId, String msg) throws IOException {
        Writer k8sWriter = sessionRegistry.get(sessionId).getK8sWriter();
        k8sWriter.write(msg);
        k8sWriter.flush();
    }

    /**
     * resize stream에 resize할 메시지 전송하는 메서드
     * 메시내용은 변경할 width, height 값을 json 형태로 전송해야함
     *
     * @param sessionId
     * @param msg
     * @throws IOException
     */
    public void writerResizeMsgToK8s(String sessionId, TerminalMessage msg) throws IOException {
        Map sizeMap = new HashMap<String,Integer>();
        sizeMap.put("width", msg.getCols());
        sizeMap.put("height",msg.getRows());

        Writer resizeWriterWriter = sessionRegistry.get(sessionId).getResizeWriter();
        resizeWriterWriter.write(JsonUtils.toGson(sizeMap));
        resizeWriterWriter.flush();
    }

    /**
     * K8s Client 를 통해 연결한 Websocket 으로 부터 데이터를 읽어서 Web Client로 write 하는 메서드 이다.<br>
     * Async로 동작하며 'terminalExecutor' Executor 내의 Thread 통해 처리 된다.<br>
     * 인터럽트, Close 상태, read값이 -1(connection close일 경우), Exception 발생이 아닐 경우는 while문을 반복 하면서 데이터 처리한다.<br>
     * terminalExecutor 설정은 <b>TerminalWebSocketConfig</b> 를 참고.
     *
     * @param sessionId SessionID string of Web Client
     * @param k8sInputStream the InputStream of Container's Websocket in POD
     */
    @Async("terminalExecutor")
    public void procMsgFromK8s(String sessionId, InputStream k8sInputStream){

        ByteArrayOutputStream result = new ByteArrayOutputStream();

        Preconditions.checkNotNull(k8sInputStream);
        Preconditions.checkNotNull(result);

        int available = -1;
        int readLength = 0;
        byte[] buffer = new byte[2048];

        Process process = sessionRegistry.get(sessionId).getK8sProcess();
        int exitValue = 0;

        // Client전송을 위한 메시지 객체
        TerminalMessage stdOutMessage = new TerminalMessage();
        stdOutMessage.setOp("stdout");

        try {

            while (!Thread.currentThread().isInterrupted() &&
                    sessionRegistry.get(sessionId) != null &&
                    sessionRegistry.get(sessionId).getStatus() != TerminalSessionStatus.CLOSE) {

                // 프로세스가 끝났으면 바로 종료 할 수 있도록 looping을 멈춘다. ex) 터미널에서 exit 명령 입력시
                if(!process.isAlive()) {
                    // client 프로세스 종료 데이터 전송
                    stdOutMessage.setOp("toast");
                    stdOutMessage.setData("Process exited");
                    writeMsgToClient(sessionId, stdOutMessage);

                    terminalLogger.debug("process.isAlive() : "+process.isAlive());
                    terminalLogger.debug("process.exitValue : "+exitValue);
                    break;
                }

                available = k8sInputStream.available();

                // response 사이즈에 따라 read 방식을 변경, 키 입력이나 짧은 결과일때
                if (available > 0 && available < 100 ) {
                    readLength = k8sInputStream.read(buffer);
                    if (readLength < 0) { // -1 is connection closed.
                        throw new IOException("EOF has been reached!");
                    }

                    result.write(buffer, 0, readLength);

                    // web client에 k8s 데이터 전송
                    if (result.size() > 0) {
                        // client 데이터 전송
                        stdOutMessage.setData(result.toString(StandardCharsets.UTF_8.name()));
                        terminalLogger.debug("procMsgFromK8s[1] -> {}", JsonUtils.toGson(stdOutMessage));
                        writeMsgToClient(sessionId, stdOutMessage);
                        result.reset();
                    }
                }
                // 데이터 결과가 많을때 read while 처리
                else if (available != 0){
                    while ((readLength = k8sInputStream.read(buffer)) > 0) {

                        if (readLength < 0) { // -1 is connection closed.
                            throw new IOException("EOF has been reached!");
                        }

                        result.write(buffer, 0, readLength);

                        // web client에 k8s 데이터 전송
                        if (result.size() > 0) {
                            // client 데이터 전송
                            stdOutMessage.setData(result.toString(StandardCharsets.UTF_8.name()));
                            terminalLogger.debug("procMsgFromK8s[2] -> {}", JsonUtils.toGson(stdOutMessage));
                            writeMsgToClient(sessionId, stdOutMessage);
                            result.reset();
                        }
                        if(readLength < 1000){
                            break;
                        }
                    }
                }
                Thread.sleep(10);
            }

        } catch (EncodeException ee) {
            terminalLogger.error("Exception[SessionID :"+sessionId+"]", ee);
        } catch (InterruptedException ie) {
            terminalLogger.error("Exception[SessionID :"+sessionId+"]", ie);
        } catch (UnsupportedEncodingException uee) {
            terminalLogger.error("Exception[SessionID :"+sessionId+"]", uee);
        } catch (IOException ioe) {
            terminalLogger.error("Exception[SessionID :"+sessionId+"]", ioe);
        } finally {
            // IOException 발생시 proccess Close
            closeSession(sessionId);
        }

    }

    public void writeMsgToClient(String sessionId, TerminalMessage message) throws IOException, EncodeException {
        writeMsgToClient(sessionRegistry.get(sessionId).getSession(), message);
    }

    public void writeMsgToClient(Session session, TerminalMessage message) throws IOException, EncodeException {
        session.getBasicRemote().sendObject(message);
    }

    public void closeSession(String sessionId){
        try {
            // K8s client 연결 종료
            if(sessionRegistry.get(sessionId) != null ) {
                // K8s 연결 종료
                closeK8sSession(sessionId);
                // client 연결 종료
                closeClientSession(sessionRegistry.get(sessionId).getSession());
            }

        } catch (IOException e) {
            terminalLogger.debug("====================================Exception[SessionID :"+sessionId+"]");
            terminalLogger.error("Exception occured when call closeSession method. ", e);
        } finally {
            // session정보 삭제
            sessionRegistry.remove(sessionId);
        }
        printResistry();
    }

    // K8s web socket session close
    public void closeK8sSession(String sessionId) throws IOException {
        Process k8sProcess = sessionRegistry.get(sessionId).getK8sProcess();
        k8sProcess.destroy();

        terminalLogger.debug("====================================K8s Connection Close.");
    }

    public void closeClientSession(Session session) throws IOException {
        if(session.isOpen() ){
            session.close();
            terminalLogger.debug("====================================WebClient Connection Close.");
        }
    }

    // registry 현황 출력
    public void printResistry(){
        terminalLogger.debug("\nWebSocketSession registry Status =========================================\n"+ sessionRegistry.toString());
        terminalLogger.debug("\n==========================================================================\n");
    }

    public void checkUserPermission(int clusterSeq, String namespace, Map<String, List<String>> requestHeader) throws CocktailException {

        terminalLogger.debug("clusterSeq : {}, namespace : {}, user-id : {}, user-role : {}, user-workspace : {}, Ignore-Auth : {}"
                , clusterSeq
                , namespace
                , requestHeader.get("user-id")
                , requestHeader.get("user-role")
                , requestHeader.get("user-workspace")
                , requestHeader.get("Ignore-Auth")
        );

        // '내 클러스터 계정' 페이지의 접속 터미널을 통해 터미널 접속시 Ignore-Auth header에 true값 셋팅됨.
        // Ignore-Auth : true 일 경우는 권한체크를 하지않음.
        List<String> ignoreAuths = requestHeader.get("Ignore-Auth");
        if (ListUtils.emptyIfNull(ignoreAuths).size() > 0 && BooleanUtils.toBoolean(ignoreAuths.get(0))){
            return; // 권한체크 안함.
        }

        //request에서 사용자 정보 추출, user-id & user-role
        Integer userSeq;
        String userRole;
        Integer serviceSeq = 0;

        List<String> userIds = requestHeader.get("user-id");
        List<String> userRoles = requestHeader.get("user-role");
        List<String> userWorkspaces = requestHeader.get("user-workspace"); // admin 일 경우

        if (ListUtils.emptyIfNull(userIds).size() < 1 || ListUtils.emptyIfNull(userRoles).size() < 1 ){
            throw new CocktailException("Not authorized to request!!", ExceptionType.NotAuthorizedToRequest);
        }

        // key 추출
        userSeq = Integer.valueOf(userIds.get(0));
        userRole = userRoles.get(0);
        if (ListUtils.emptyIfNull(userWorkspaces).size() > 0 ){
            serviceSeq = Integer.valueOf(userWorkspaces.get(0));
        }

        //////////// user 정보 체크 ////////////
        UserVO userVO = null;
        try {
            userVO = userService.getByUserSeq(userSeq);
        } catch (Exception e) {
            terminalLogger.error("User not found.[{}]", userSeq, e);
            throw new CocktailException("Not authorized to request!! [get user]", ExceptionType.NotAuthorizedToRequest);
        }

        // 사용자 정보가 없거나 관련 정보가 없으면 권한 오류
        if (userVO == null || CollectionUtils.isEmpty(userVO.getUserRelations())) {
            throw new CocktailException("Not authorized to request!! [not user or relations]", ExceptionType.NotAuthorizedToRequest);
        }

        // user role 체크
        if(!StringUtils.equalsIgnoreCase(userVO.getUserRole(), userRole)){
            throw new CocktailException("Not authorized to request!! [not role]", ExceptionType.NotAuthorizedToRequest);
        }

        // 클러스터 체크
        long clusterCnt = userVO.getUserRelations().stream().filter(vo -> vo.getClusterSeq() != null && vo.getClusterSeq().intValue() == clusterSeq).count();
        if (clusterCnt < 1) {
            throw new CocktailException("Not authorized to request!! [not cluster]", ExceptionType.NotAuthorizedToRequest);
        }

        // service && namespace 체크
        if (UserRole.valueOf(userRole).isDevops()) {

            // 서비스 정보 및 권한 체크
            Integer checkServiceSeq = serviceSeq;
            Optional<ServiceRelationVO> serviceRelationOptional = userVO.getUserRelations().stream().filter(r -> (checkServiceSeq.equals(r.getServiceSeq()))).findFirst();
            // 권한오류
            if (!serviceRelationOptional.isPresent()   // 서비스 정보가 없거나
                    || serviceRelationOptional.get().getUserGrant().isViewer()   // view 권한일 경우
                    || serviceRelationOptional.get().getUserGrant().isPipeline()   // DEVOPS, DEV 권한일 경우
            ) {
                throw new CocktailException("Not authorized to request!! [not grant]", ExceptionType.NotAuthorizedToRequest);
            }

            // servicemap 조회 및 체크
            ServiceDetailVO service; // service key 로 appmap 조회
            try {
                service = serviceService.getService(serviceSeq);
            } catch (Exception e) {
                terminalLogger.error("Service not found.[{}]", serviceSeq, e);
                throw new CocktailException("Not authorized to request!! [get service]", ExceptionType.NotAuthorizedToRequest);
            }

            long servicemapCnt = service.getServicemaps().stream().filter(s -> s.getClusterSeq().equals(clusterSeq) && s.getNamespaceName().equals(namespace)).count();
            if (servicemapCnt == 0){
                throw new CocktailException("Not authorized to request!! [not namespace]", ExceptionType.NotAuthorizedToRequest);
            }

        }

    }


}
