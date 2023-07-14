package run.acloud.api.terminal.endpoint;


import io.kubernetes.client.Exec;
import io.kubernetes.client.openapi.ApiClient;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import run.acloud.api.configuration.vo.ClusterVO;
import run.acloud.api.terminal.coders.TerminalMessageDecoder;
import run.acloud.api.terminal.coders.TerminalMessageEncoder;
import run.acloud.api.terminal.enums.TerminalSessionStatus;
import run.acloud.api.terminal.service.TerminalService;
import run.acloud.api.terminal.vo.TerminalMessage;
import run.acloud.api.terminal.vo.TerminalSession;
import run.acloud.framework.configuration.TerminalWebSocketConfig;
import run.acloud.framework.exception.CocktailException;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@ServerEndpoint(value = "/terminal/{clusterSeq}/{namespace}/{podName}/{containerName}"
        , encoders = {TerminalMessageEncoder.class}
        , decoders = {TerminalMessageDecoder.class}
        , configurator = TerminalWebSocketConfig.class)
public class TerminalEndPoint {

    private static Logger terminalLogger = LoggerFactory.getLogger("terminal.logger");

    @Autowired
    private TerminalService terminalService;

    @OnOpen
    public void onOpen(Session session
            , @PathParam("clusterSeq") final int clusterSeq
            , @PathParam("namespace") final String namespace
            , @PathParam("podName") final String podName
            , @PathParam("containerName") final String containerName
    ) throws Exception {
        terminalLogger.debug("OnOpen");

        Map<String, List<String>> requestHeaders = TerminalWebSocketConfig.getHeaderMap().get(String.valueOf(Thread.currentThread().getId()));

        // Header 와 PathParam 을 이용한 권한 체크
        if(requestHeaders != null) {
            try {
                terminalService.checkUserPermission(clusterSeq, namespace, requestHeaders);
            } catch (CocktailException ce) {
                // k8s connection 실패시 실패 메시지 전달
                terminalService.writeMsgToClient(session, new TerminalMessage("toast", "Connection failed!. [ Not authorized to request ]"));
                terminalService.closeClientSession(session);

                return;
            }finally {
                // 체크 후 해당 헤더값 삭제
                TerminalWebSocketConfig.getHeaderMap().remove(String.valueOf(Thread.currentThread().getId()));
            }
        }

        // session 존재 여부체크
        TerminalSession sessionVO = this.terminalService.getSessionRegistry().get(session.getId());

        // 저장된 세션이 없을때만 다시 생성
        if(sessionVO == null) {
            // Connect 불가능 하면 메시지 전송하고 connetion close, 커넥션 갯수 체크
            if(!terminalService.availableConnectCount()){
                terminalService.writeMsgToClient(session, new TerminalMessage("toast", "Connections exceeded") );
                terminalService.closeClientSession(session);

            } else {

                try {
                    // K8S WebSocket 연결 및 Client Websocket 세션 저장
                    connectK8sServer(session, clusterSeq, namespace, podName, containerName);

                    // 정상적으로 등록이 되었을때 연결되었다고 메시지 전송
                    terminalService.writeMsgToClient(session, new TerminalMessage("toast", "Connection opened"));

                } catch (IOException | EncodeException e) {
                    terminalLogger.error("Connection failed to K8s", e);

                    // k8s connection 실패시 실패 메시지 전달
                    terminalService.writeMsgToClient(session, new TerminalMessage("toast", "Connection failed!"));
                    terminalService.closeClientSession(session);

                    // connection close 하도록 exception throw
                    throw e;

                } catch (Exception e) {
                    terminalLogger.error("Connection failed to K8s", e);

                    // k8s connection 실패시 실패 메시지 전달
                    terminalService.writeMsgToClient(session, new TerminalMessage("toast", "Connection failed!"));
                    terminalService.closeClientSession(session);

                    // connection close 하도록 exception throw
                    throw e;
                }

            }
        }

    }

    @OnMessage
    public void onMessage(Session session, TerminalMessage message) {

        try {

            if(message.getOp().equals("resize")) {
                // resize stream을 통해 k8s로 메시지 전달
                terminalService.writerResizeMsgToK8s(session.getId(), message);

            }else if(message.getOp().equals("stdin")) {
                // k8s로 메시지 전송
                terminalService.writerMsgToK8s(session.getId(), message.getData());

            }

        } catch (IOException e) {
            terminalLogger.error("OnMessage Method Exception occured.", e);
            terminalService.closeSession(session.getId());
        }

    }

    @OnClose
    public void onClose(Session session, CloseReason reason) throws IOException {
        terminalLogger.debug("OnClose");

        // 상태 설정
        if(this.terminalService.getSessionRegistry().get(session.getId()) != null){
            terminalService.getSessionRegistry().get(session.getId()).setStatus(TerminalSessionStatus.CLOSE);
        }
        terminalService.closeSession(session.getId());
    }

    @OnError
    public void onError(Session session, Throwable t) {
        terminalLogger.error("OnError", t);

        // 상태 설정
        terminalService.getSessionRegistry().get(session.getId()).setStatus(TerminalSessionStatus.ERROR);
        terminalService.closeSession(session.getId());
    }

    // K8s Websocket 연결 및 세션정보 저장
    private void connectK8sServer(
            Session session
            , final int clusterSeq
            , final String namespace
            , final String podName
            , final String containerName) throws Exception {

        // 클러스터 정보 조회
        ClusterVO clusterVO = this.terminalService.getCluster(clusterSeq);

        // K8s Client 생성
        ApiClient k8sClient = this.terminalService.createK8sClient(clusterVO);
        // 세선 정보 생성
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd, HH:mm:ss");
        TerminalSession sessionVO = new TerminalSession();
        sessionVO.setCreateTime(sdf.format(new Date()));
        sessionVO.setSession(session);
        sessionVO.setClusterSeq(clusterSeq);
        sessionVO.setNamespace(namespace);
        sessionVO.setPodName(podName);
        sessionVO.setContainerName(containerName);
        sessionVO.setK8sClient(k8sClient);

        // 세션 등록하기전 POD 및 Container 상태 체크
        boolean avaliableConnect = this.terminalService.checkLivePodAndContainer(clusterVO, sessionVO);
        if (!avaliableConnect){
            throw new CocktailException("Not avaliable to connect K8s Container.");
        }

        // 세션 정보 등록
        this.terminalService.registSession(sessionVO);


        // shell에 맞는 k8s connect 및 process 생성
        Exec.ExecProcess proc = null;
        boolean flag = false;
        boolean alive = false;
        int exitValue = -1;

        for(String shell : TerminalService.shellList){
            sessionVO.setStatus(TerminalSessionStatus.CONNECT);

            // Exec Process 생성
            proc = (Exec.ExecProcess)this.terminalService.createK8sExec(sessionVO, shell);

            if (proc != null) {

                sessionVO.setK8sProcess(proc);

                flag = proc.waitFor(1200, TimeUnit.MILLISECONDS);
                alive = proc.isAlive();

                terminalLogger.debug("===============shell : "+shell);
                terminalLogger.debug("===============flag : "+flag);
                terminalLogger.debug("===============alive : "+alive);

                // 프로세스 정상상태 이면 loop 중지
                if(!flag && alive){
                    sessionVO.setK8sWriter( new BufferedWriter(new OutputStreamWriter(proc.getOutputStream())) );
                    // K8s 에서 결과 전송시 client 에 전송 할 수 있도록 Async로 처리 되도록 하는 메서드
                    this.terminalService.procMsgFromK8s(session.getId(), proc.getInputStream());

                    // terminal resize를 위해 outputstream을 얻어 셋팅함
                    sessionVO.setResizeWriter(new BufferedWriter(new OutputStreamWriter(proc.getResizeStream())) );

                    // 세션 상태 변경
                    sessionVO.setStatus(TerminalSessionStatus.OPEN);

                    // 세션현황 출력
                    this.terminalService.printResistry();


                    break;
                }

                // 프로세스가 비정상일 경우는 K8s 프로세스 destory
                sessionVO.setStatus(TerminalSessionStatus.CLOSE);
                exitValue = proc.exitValue();
            }

            this.terminalService.closeK8sSession(session.getId());

            terminalLogger.debug("===============exitValue : "+exitValue);
        }

        // shell 별로 설정 진행시 모두 오류가 났을 경우는 IOException
        if(!alive){
            throw new IOException("Fail to connect K8s.");
        }

    }

}
