package run.acloud.api.resource.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import run.acloud.commons.vo.BaseVO;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * @author: wschoi@acornsoft.io
 * Created on 2017. 10. 12.
 */
@Getter
@Setter
public class K8sServiceDetailVO extends BaseVO{

    @Schema(title = "네임스페이스")
    private String namespace;

    @Schema(title = "Service 명")
    private String serviceName;

    @Schema(title = "라벨")
    private Map<String, String> labels;

    @Schema(title = "annotations")
    private Map<String, String> annotations;

    @Schema(title = "라벨")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private OffsetDateTime creationTimestamp;

    @Schema(title = "Label Selector")
    private Map<String, String> labelSelector;

    @Schema(title = "Service Type")
    private String type;

    @Schema(title = "Session Affinity")
    private String sessionAffinity;

    @Schema(title = "Session Affinity Config (Json string)")
    private String sessionAffinityConfig;

    @Schema(title = "Cluster IP")
    private String clusterIP;

    @Schema(title = "내부 endpoint")
    private List<String> internalEndpoints;

    @Schema(title = "외부 endpoint")
    private List<String> externalEndpoints;

}
