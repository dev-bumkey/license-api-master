package run.acloud.api.resource.vo;

import com.google.gson.annotations.SerializedName;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Setter
@Getter
@EqualsAndHashCode
public class ContainerLifecycleHandlerVO implements Serializable {
    @SerializedName("exec")
    private ExecActionVO exec;
    @SerializedName("httpGet")
    private HTTPGetActionVO httpGet;

    //TCP hooks not yet supported at 1.13
    @SerializedName("tcpSocket")
    private TCPSocketActionVO tcpSocket;
}
