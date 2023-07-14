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
@Schema(description = "Deployment 상세 모델")
public class K8sDeploymentDetailVO extends BaseVO{

    @Schema(title = "Deployment 명")
    private String name;

    @Schema(title = "네임스페이스")
    private String namespace;

    @Schema(title = "라벨")
    private Map<String, String> labels;

    @Schema(title = "어노테이션")
    private Map<String, String> annotations;

    @Schema(title = "생성 시간")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private OffsetDateTime creationTime;

    @Schema(title = "Selector")
    private K8sLabelSelectorVO selector;

    @Schema(title = "strategy")
    private String strategy;

    @Schema(title = "min Ready Seconds", description = "Minimum number of seconds for which a newly created pod should be ready without any of its container crashing, for it to be considered available. Defaults to 0 (pod will be considered available as soon as it is ready)")
    private int minReadySeconds = 0;

    @Schema(title = "revision History Limit", description = "The number of old ReplicaSets to retain to allow rollback. This is a pointer to distinguish between explicit zero and not specified. Defaults to 2.")
    private String revisionHistoryLimit = "Not Set";

    @Schema(title = "rollingUpdate", description = "Spec to control the desired behavior of rolling update.")
    private Map<String, Object> rollingUpdate;

    @Schema(title = "status", description = "Most recently observed status of the Deployment.")
    private Map<String, Integer> status;

    @Schema(title = "New ReplicaSet 목록")
    private List<K8sReplicaSetVO> newReplicaSets;

    @Schema(title = "Old ReplicaSet 목록")
    private List<K8sReplicaSetVO> oldReplicaSets;

    @Schema(title = "replicas")
    private Integer replicas;

    @Schema(title = "podTemplate")
    private K8sPodTemplateSpecVO podTemplate;
}
