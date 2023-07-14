package run.acloud.api.resource.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import run.acloud.api.configuration.vo.ClusterVolumeVO;
import run.acloud.commons.vo.BaseVO;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * @author: wschoi@acornsoft.io
 * Created on 2017. 10. 12.
 */
@Getter
@Setter
@Schema(description = "PersistentVolume 모델")
public class K8sPersistentVolumeVO extends BaseVO {

    @Schema(title = "PV 명")
    private String name;

    @Schema(title = "용량")
    private String capacity;

    @Schema(title = "Access Modes")
    private List<String> accessModes;

    @Schema(title = "Reclaim policy 명")
    private String persistentVolumeReclaimPolicy;

    @Schema(title = "PV 상태")
    private String status;

    @Schema(title = "PVC 명(namespace 포함)")
    private String claimNameWithNamespace;

    @Schema(title = "PVC 명")
    private String claimName;

    @Schema(title = "Storage Class 명")
    @JsonProperty(defaultValue = "-")
    private String storageClassName;

    @Schema(title = "Status Reason")
    @JsonProperty(defaultValue = "-")
    private String statusReason = null;

    @Schema(title = "생성시간")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private OffsetDateTime creationTimestamp;

    @Schema(title = "배포정보")
    private String deployment;

    @Schema(title = "배포정보 (yaml)")
    private String deploymentYaml;

    @Schema(title = "상세")
    private K8sPersistentVolumeDetailVO detail;

    @Schema(title = "PersistentVolumeClaim")
    private K8sPersistentVolumeClaimVO claim;

    @Schema(title = "Cluster Volume (storage)")
    private ClusterVolumeVO clusterVolume;

    @Schema(title = "Server Parameter")
    private List<ServerDetailParamForPVVO> serverParams;

}
