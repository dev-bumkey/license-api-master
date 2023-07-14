package run.acloud.api.resource.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import run.acloud.api.resource.enums.JsonPatchOp;
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
public class K8sNodeDetailVO extends BaseVO{

    @Schema(title = "Node 명")
    private String name;

    @Schema(title = "라벨")
    private Map<String, String> labels;

    @Schema(title = "어노테이션")
    private Map<String, String> annotations;

    @Schema(title = "생성 시간")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private OffsetDateTime creationTime;

    @Schema(title = "Addresses", description = "List of addresses reachable to the node. Queried from cloud provider, if available. More info: https://kubernetes.io/docs/concepts/nodes/node/#addresses")
    private Map<String, String> addresses;

    @Schema(title = "configSource", description = "If specified, the source to get node configuration from The DynamicKubeletConfig feature gate must be enabled for the Kubelet to use this field")
    private String configSource;

    @Schema(title = "podCIDR", description = "PodCIDR represents the pod IP range assigned to the node.")
    private String podCIDR;

    @Schema(title = "providerID", description = "ID of the node assigned by the cloud provider in the format: <ProviderName>://<ProviderSpecificNodeID>")
    private String providerID;

    @Schema(title = "taints list")
    private List<K8sTaintVO> taints;

    @Schema(title = "taints(json string list)", description = "If specified, the node's taints.")
    private String taintsJson;

    @Schema(title = "unschedulable", description = "Unschedulable controls node schedulability of new pods. By default, node is schedulable. More info: https://kubernetes.io/docs/concepts/nodes/node/#manual-node-administration")
    private Boolean unschedulable = false;

    @Schema(title = "machineID")
    private String machineID;

    @Schema(title = "systemUUID")
    private String systemUUID;

    @Schema(title = "bootID")
    private String bootID;

    @Schema(title = "kernelVersion")
    private String kernelVersion;

    @Schema(title = "osImage")
    private String osImage;

    @Schema(title = "containerRuntimeVersion")
    private String containerRuntimeVersion;

    @Schema(title = "kubeletVersion")
    private String kubeletVersion;

    @Schema(title = "kubeProxyVersion")
    private String kubeProxyVersion;

    @Schema(title = "operatingSystem")
    private String operatingSystem;

    @Schema(title = "architecture")
    private String architecture;


    /**
     * R3.5.0 : 2019.10.15
     */
    @JsonIgnore
    private Map<String, JsonPatchOp> labelPatchOp;

    @JsonIgnore
    private Map<String, JsonPatchOp> annotationPatchOp;

    @JsonIgnore
    private Map<String, JsonPatchOp> taintsPatchOp;


}
