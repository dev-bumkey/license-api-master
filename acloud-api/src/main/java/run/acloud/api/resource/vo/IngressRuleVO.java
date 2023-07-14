package run.acloud.api.resource.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import run.acloud.commons.vo.BaseVO;

import java.util.ArrayList;
import java.util.List;

/**
 * @author: wschoi@acornsoft.io
 * Created on 2017. 10. 12.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class IngressRuleVO extends BaseVO {

    @Schema(title = "hostName")
    @JsonProperty("host")
    @SerializedName("host")
    private String hostName;

    @Schema(title = "Ingress http path")
    @JsonProperty("paths")
    @SerializedName("paths")
    private List<IngressHttpPathVO> ingressHttpPaths = new ArrayList<IngressHttpPathVO>();
}
