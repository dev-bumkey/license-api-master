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
public class K8sPodDetailVO extends BaseVO{

    @Schema(title = "Pod 명")
    private String podName;

    @Schema(title = "네임스페이스")
    private String namespace;

    @Schema(title = "라벨")
    private Map<String, String> labels;

    @Schema(title = "어노테이션")
    private Map<String, String> annotations;

    @Schema(title = "ownerReferences")
    private List<K8sOwnerReferenceVO> ownerReferences;

    @Schema(title = "생성 시간")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private OffsetDateTime creationTime;

    @Schema(title = "시작 시간")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private OffsetDateTime startTime;

    @Schema(title = "Pod 상태")
    private String podStatus;

    @Schema(title = "QosClass")
    private String qosClass;

    @Schema(title = "Node 명")
    private String nodeName;

    @Schema(title = "restartPolicy")
    private String restartPolicy;

    @Schema(title = "종료시 유예 시간")
    private Long terminationGracePeriodSeconds;

    @Schema(title = "nodeSelector")
    private Map<String, String> nodeSelector;

    @Schema(title = "serviceAccountName")
    private String serviceAccountName;

    @Schema(title = "hostname")
    private String hostname;

    @Schema(title = "imagePullSecrets")
    private List<LocalObjectReferenceVO> imagePullSecrets;

    @Schema(title = "tolerations")
    private List<TolerationVO> tolerations;

    @Schema(title = "affinity")
    private AffinityVO affinity;

    @Schema(title = "Pod IP")
    private String podIP;

    @Schema(title = "init Containers")
    private List<K8sContainerVO> initContainers;

    @Schema(title = "Containers")
    private List<K8sContainerVO> containers;
}
