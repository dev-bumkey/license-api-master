package run.acloud.api.resource.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.kubernetes.client.custom.Quantity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import run.acloud.api.configuration.vo.ClusterNodePoolResourceVO;
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
@Schema(description = "Node 모델")
public class K8sNodeVO extends BaseVO{

    @Schema(title = "Node 명")
    private String name;

    @Schema(title = "라벨")
    private Map<String, String> labels;

    @Schema(title = "상태")
    private String ready;

    @Schema(title = "gpu 용량")
    private Integer allocatedGpu;

    @Schema(title = "gpu 용량")
    private int gpuCapacity = 0;

    @Schema(title = "gpu 요청")
    private int gpuRequests = 0;

    @Schema(title = "gpu 제한")
    private int gpuLimits;

    @Schema(title = "cpu 용량")
    private long allocatedCpu;

    @Schema(title = "cpu 용량")
    private long cpuCapacity;

    @Schema(title = "cpu 요청")
    private long cpuRequests;

    @Schema(title = "cpu 제한")
    private long cpuLimits;

    @Schema(title = "cpu 사용량")
    private double cpuUsage;

    @Schema(title = "memory 용량")
    private long memoryCapacity;

    @Schema(title = "memory 용량")
    private long allocatedMemory;

    @Schema(title = "ephemeral-storage 용량")
    private long ephemeralStorageCapacity;

    @Schema(title = "ephemeral-storage 용량")
    private long allocatedEphemeralStorage;

    @Schema(title = "memory 요청")
    private long memoryRequests;

    @Schema(title = "memory 제한")
    private long memoryLimits;

    @Schema(title = "memory 사용량")
    private long memoryUsage;

    @Schema(title = "pod 용량")
    private String podCapacity;

    @Schema(title = "pod 수")
    private String allocatedPods;

    @Schema(title = "pod 수")
    private long allocationPods;

    @Schema(title = "allocatable")
    private Map<String, Quantity> allocatable;

    @Schema(title = "capacity")
    private Map<String, Quantity> capacity;

    @Schema(title = "resources", description = "resource별 용량")
    private Map<String, ClusterNodePoolResourceVO> resources;

    @Schema(title = "생성시간")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private OffsetDateTime creationTimestamp;

    @Schema(title = "Node 배포 정보")
    private String deployment;

    @Schema(title = "Node 배포 정보 (yaml)")
    private String deploymentYaml;

    @Schema(title = "Node 상세")
    private K8sNodeDetailVO detail;

    @Schema(title = "Node 상태")
    private List<K8sNodeConditionVO> conditions;
}
