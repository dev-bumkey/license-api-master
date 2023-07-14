package run.acloud.api.terminal.vo;


import io.kubernetes.client.openapi.ApiClient;
import jakarta.websocket.Session;
import lombok.Getter;
import lombok.Setter;
import run.acloud.api.terminal.enums.TerminalSessionStatus;

import java.io.Writer;

@Getter
@Setter
public class TerminalSession {
    private String createTime;
    // 접속 K8s POD or Container 정보
    private int clusterSeq;
    private String namespace;
    private String podName;
    private String containerName;

    // K8s Client
    private ApiClient k8sClient;

    // K8s Exec Process
    private Process k8sProcess;

    // K8s Exec Writer
    private Writer k8sWriter;

    // client Web socket 세션정보
    private Session session;

    // Session Message Status
    private TerminalSessionStatus status;

    // resize Writer
    private Writer resizeWriter;

    @Override
    public String toString() {
        return "\n\tTerminalSessionVO {" +
                "\n\t  createTime=[" + createTime +"]"+
                "\n\t, clusterSeq=" + clusterSeq +
                "\n\t, namespace='" + namespace + '\'' +
                "\n\t, podName='" + podName + '\'' +
                "\n\t, containerName='" + containerName + '\'' +
                "\n\t, k8sProcess=" + k8sProcess +
                "\n\t, session.getId()=" + session.getId() +
                "\n\t, session.isOpen()=" + session.isOpen() +
                "\n\t, status=" + status +
                "\n\t}\n";
    }
}
