package run.acloud.api.catalog.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import run.acloud.api.configuration.vo.ClusterAccessInfoVO;

import java.io.Serializable;

@Getter
@Setter
@Schema(title = "Helm Request Base", description = "Helm Request Base Model")
public class HelmRequestBaseVO implements Serializable {
    private static final long serialVersionUID = 5812331553668493715L;

    @Schema(title = "ClusterAccessInfoVO", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("cluster_info")
    private ClusterAccessInfoVO clusterAccessInfo;

    @Schema(title = "namespace", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("namespace")
    private String namespace;
}
