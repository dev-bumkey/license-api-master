package run.acloud.api.resource.vo;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * @author: wschoi@acornsoft.io
 * Created on 2017. 10. 12.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@Schema(name = "ServiceSpecGuiVO",
        title = "ServiceSpecGuiVO",
        description = "서비스 스펙 배포 GUI 모델",
        allOf = {ServiceSpecIntegrateVO.class}
)
public class ServiceSpecGuiVO extends ServiceSpecIntegrateVO implements Serializable {
    private static final long serialVersionUID = -4764957134069757655L;

    @Schema(title = "appmapSeq")
    private Integer appmapSeq;

    @Schema(title = "componentSeq")
    private Integer componentSeq;

    @Schema(title = "워크로드 명")
    private String workloadName;

    @Schema(title = "Service 명")
    private String name;

    @Schema(title = "service 유형", allowableValues = {"CLUSTER_IP","NODE_PORT","LOADBALANCER","HEADLESS"})
    private String serviceType;

    @Schema(title = "할당된 Cluster ip", description = "Service deploy결과")
    private String clusterIp;

    @Schema(title = "EXTERNAL_NAME serviceType 일 경우 셋팅", description = "Service deploy결과")
    private String externalName;

    @Schema(title = "Internal load balancer 여부", allowableValues = {"false","true"})
    private Boolean internalLBFlag = Boolean.FALSE;

    @Schema(title = "headless 여부", allowableValues = {"false","true"})
    private Boolean headlessFlag = Boolean.FALSE;

    @Schema(title = "stickySession 사용 여부", allowableValues = {"false","true"})
    private Boolean stickySessionFlag = Boolean.FALSE;

    @Schema(title = "stickySession 사용 여부", description = "timeoutSeconds specifies the seconds of ClientIP type session sticky time. The value must be >0 && <=86400(for 1 day) if ServiceAffinity == 'ClientIP'. Default value is 10800(for 3 hours).")
    private Integer stickySessionTimeoutSeconds;

    public Integer getStickySessionTimeoutSeconds(){
        if(this.getStickySessionFlag() == null
                || (this.getStickySessionFlag() != null && !this.getStickySessionFlag().booleanValue())){
            return null;
        }

        return this.stickySessionTimeoutSeconds;
    }

    @Schema(title = "service ports")
    @JsonProperty("ports")
    @SerializedName(value="ports", alternate = {"servicePorts"})
    @JsonAlias({"servicePorts", "ports"})
    private List<ServicePortVO> servicePorts;

    @Schema(title = "workload Controller", description = "Package Type의 경우 Workload 안에 WorkloadSet이 한종류가 아니므로 어떤 Controller인지 구분이 필요.")
    private String workloadController;

    @Schema(title = "selector", description = "Package Type의 경우 Workload 안에 WorloadSet이 한종류가 아니므로 지정이 필요함..")
    private Map<String, String> labelSelector;

    @Schema(title = "labels")
    private Map<String, String> labels;

    @Schema(title = "annotations")
    private Map<String, String> annotations;

    @Schema(title = "loadBalancer")
    private String loadBalancer;

    @Schema(title = "namespaceName")
    private String namespaceName;


}
