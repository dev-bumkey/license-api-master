package run.acloud.api.resource.vo;

import com.google.gson.annotations.SerializedName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import run.acloud.commons.vo.BaseVO;

import java.util.List;

/**
 * @author: wschoi@acornsoft.io
 * Created on 2017. 10. 12.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class IngressTLSVO extends BaseVO {

    @Schema(title = "Hosts are a list of hosts included in the TLS certificate. The values in this list must match the name/s used in the tlsSecret. Defaults to the wildcard host setting for the loadbalancer controller fulfilling this Ingress, if left unspecified.")
    @SerializedName("hosts")
    private List<String> hosts;

    @Schema(title = "SecretName is the name of the secret used to terminate SSL traffic on 443. Field is left optional to allow SSL routing based on SNI hostname alone. If the SNI host in a listener conflicts with the Host header field used by an IngressRule, the SNI host is used for termination and value of the Host header is used for routing.")
    @SerializedName("secretName")
    private String secretName;
}
