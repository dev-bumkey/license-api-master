package run.acloud.api.resource.vo;

import lombok.Getter;
import lombok.Setter;
import run.acloud.api.resource.enums.ProbeType;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
public class ContainerProbeVO implements Serializable {

    private ProbeType type;
    private Integer initialDelaySeconds;
    private Integer periodSeconds;
    private Integer timeoutSeconds;
    private Integer successThreshold;
    private Integer failureThreshold;

    /* ProbeType.EXEC */
    @Deprecated
    private String execCommand;
    private List<String> execCmds;

    /* ProbeType.TCPSOCKET */
//    private Integer tcpSocketPort; // K8s의 IntOrString Spec 지원을 위해 Type 변경 2020.01.10 Redion
    private String tcpSocketPort;

    /* ProbeType.HTTPGET */
    private String httpGetScheme;
    private String httpGetHost;
//    private Integer httpGetPort; // K8s의 IntOrString Spec 지원을 위해 Type 변경 2020.01.10 Redion
    private String httpGetPort;
    private String httpGetPath;
    private List<HTTPHeaderVO> httpGetHeaders;
}
