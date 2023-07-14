package run.acloud.api.resource.vo;

import com.google.gson.annotations.SerializedName;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Setter
@Getter
@EqualsAndHashCode
public class HTTPGetActionVO implements Serializable {
    @SerializedName("host")
    private String host = null;
    @SerializedName("httpHeaders")
    private List<HTTPHeaderVO> httpHeaders = null;
    @SerializedName("path")
    private String path = null;
    @SerializedName("port")
//    private Integer port = null; // K8s의 IntOrString Spec 지원을 위해 Type 변경 2020.01.10 Redion
    private String port = null;
    @SerializedName("scheme")
    private String scheme = null;
}
