package run.acloud.api.resource.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import run.acloud.commons.vo.BaseVO;

@Getter
@Setter
@Schema(description = "컨테이너에서 port의 사용 설정")
public class ContainerPortVO extends BaseVO {
	
	private static final long serialVersionUID = -2660728215536741957L;

    @Schema(name = "containerPort", requiredMode = Schema.RequiredMode.REQUIRED, title = "Number of port to expose on the pod's IP address. This must be a valid port number, 0 < x < 65536.")
    @Min(1)
    @Max(65535)
    private Integer containerPort;

    @Schema(name = "hostIP", title = "What host IP to bind the external port to.")
    private String hostIP;

    @Schema(name = "hostPort", title = "Number of port to expose on the host. If specified, this must be a valid port number, 0 < x < 65536. If HostNetwork is specified, this must match ContainerPort. Most containers do not need this.")
	private Integer hostPort;

    @Schema(name = "name", title = "If specified, this must be an IANA_SVC_NAME and unique within the pod. Each named port in a pod must have a unique name. Name for the port that can be referred to by services.")
    private String name;

    @Schema(name = "protocol", title = "Protocol for port. Must be UDP, TCP, or SCTP. Defaults to TCP.")
    private String protocol;

    @Deprecated
    private String alias;

    @Deprecated
    private String portType;

    @Deprecated
    private Boolean ingressFlag;

    @Deprecated
    private String ingressPath;

    @Deprecated
    private Boolean appointNodePortFlag;

    @Deprecated
    private Integer nodePort;

    @Deprecated
    private String endpoint;

    @Deprecated
    private String clusterIp;

    @Deprecated
    private String containerPortRange;

    @Deprecated
    private String hostPortRange;

    @Deprecated
    private Integer containerPortSeq;

    @Deprecated
    private Integer containerSeq;

    @Deprecated
    private String serviceName;

}
