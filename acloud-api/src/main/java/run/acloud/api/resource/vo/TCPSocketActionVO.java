package run.acloud.api.resource.vo;

import com.google.gson.annotations.SerializedName;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Setter
@Getter
@EqualsAndHashCode
public class TCPSocketActionVO implements Serializable {
    @SerializedName("host")
    private String host;
    @SerializedName("port")
    private String port;
}
