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
@Schema(description = "StatefulSet 상세 모델")
public class K8sStatefulSetDetailVO extends BaseVO{

    @Schema(title = "StatefulSet 명")
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

    @Schema(title = "strategy", description = "Type indicates the type of the StatefulSetUpdateStrategy. Default is RollingUpdate.")
    private String strategy;

    @Schema(title = "podManagementPolicy", description = "podManagementPolicy controls how pods are created during initial scale up, when replacing pods on nodes, or when scaling down. The default policy is `OrderedReady`, where pods are created in increasing order (pod-0, then pod-1, etc) and the controller will wait until each pod is ready before continuing. When scaling down, the pods are removed in the opposite order. The alternative policy is `Parallel` which will create pods in parallel to match the desired scale without waiting, and on scale down will delete all pods at once.")
    private String podManagementPolicy;

    @Schema(title = "headless service name", description = "serviceName is the name of the service that governs this StatefulSet. This service must exist before the StatefulSet, and is responsible for the network identity of the set. Pods get DNS/hostnames that follow the pattern: pod-specific-string.serviceName.default.svc.cluster.local where pod-specific-string is managed by the StatefulSet controller.")
    private String serviceName;

    @Schema(title = "revision History Limit", description = "revisionHistoryLimit is the maximum number of revisions that will be maintained in the StatefulSet's revision history. The revision history consists of all revisions not represented by a currently applied StatefulSetSpec version. The default value is 10.")
    private String revisionHistoryLimit = "Not Set";

    @Schema(title = "rollingUpdate", description = "RollingUpdate is used to communicate parameters when Type is RollingUpdateStatefulSetStrategyType.")
    private Map<String, Object> rollingUpdate;

    @Schema(title = "status", description = "Status is the current status of Pods in this StatefulSet. This data may be out of date by some window of time.")
    private Map<String, Integer> status;

    @Schema(title = "volumeClaimTemplates", description = "volumeClaimTemplates is a list of claims that pods are allowed to reference. The StatefulSet controller is responsible for mapping network identities to claims in a way that maintains the identity of a pod. Every claim in this list must have at least one matching (by name) volumeMount in one container in the template. A claim in this list takes precedence over any volumes in the template, with the same name.")
    private List<K8sPersistentVolumeClaimVO> volumeClaimTemplates;

    @Schema(title = "replicas")
    private Integer replicas;

    @Schema(title = "podTemplate")
    private K8sPodTemplateSpecVO podTemplate;
}
