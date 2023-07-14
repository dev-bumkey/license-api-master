package run.acloud.api.resource.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import run.acloud.api.catalog.vo.HelmResourcesVO;
import run.acloud.api.resource.enums.JsonPatchOp;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * @author wschoi@acornsoft.io
 * Created on 2017. 7. 26.
 */
@Getter
@Setter
@Schema(name = "ConfigMapGuiVO",
        title = "ConfigMapGuiVO",
        description = "ConfigMap GUI 모델",
        allOf = {ConfigMapIntegrateVO.class}
)
public class ConfigMapGuiVO extends ConfigMapIntegrateVO implements Serializable {
    private static final long serialVersionUID = -4816701097080090432L;

    @Schema(description = "Service 순번")
    private Integer serviceSeq;

    @Schema(description = "Appmap 순번")
    private Integer servicemapSeq;

    @Schema(description = "ConfigMap 이름", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(description = "namespace")
    private String namespace;

    @Schema(title = "생성시간")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private OffsetDateTime creationTimestamp;

    @Schema(title = "ConfigMap 설명")
    private String description;

    @Schema(description = "ConfigMap 내용(이름-값 쌍)", requiredMode = Schema.RequiredMode.REQUIRED)
    private Map<String, String> data;
//    private List<NameValueDataVO> data;

    /**
     * Redion.Package : 2019.03.14 :
     * 카탈로그 저장시에 Package Type에서 자동 생성된 ConfigMap까지 모두 저장 되어 문제 발생..
     * (Package는 Configmap과 Secret을 자동으로 생성하므로 카탈로그로 저장되면 안됨..)
     * ConfigMap을 조회하여 매핑시에 Labels 정보를 함께 리턴하기 위해 추가하였으며, "release" Label이 있으면 Package에서 생성한 것으로 간주.
     */
    @Schema(description = "label")
    private Map<String, String> labels;

    @Schema(description = "annotations")
    private Map<String, String> annotations; //R3.5

    @Schema(description = "배포 정보")
    private String deployment;

    @Schema(description = "배포 정보 (yaml)")
    private String deploymentYaml;

//    @Schema(description = "addon - configMap status", allowableValues = {"DEPLOYED","DELETED"})
//    private String status;
//
//    @Schema(description = "addon - configMap version", example = "3.4.0")
//    private String version;
//
//    @Schema(description = "addon - configMap autoUpdate", allowableValues = {"true","false"})
//    private String autoUpdate;

    @JsonIgnore
    private Map<String, JsonPatchOp> patchOp;

    @JsonIgnore
    private Map<String, JsonPatchOp> labelPatchOp;

    @JsonIgnore
    private Map<String, JsonPatchOp> annotationPatchOp;

    @JsonIgnore
    private Map<String, JsonPatchOp> patchDescOp;


    /** for Addon : 2020.05.27 : 통합 대시보드 구현에 필요 from @bw.y**/

    @Schema(description = "resources", example = "null", accessMode = Schema.AccessMode.READ_ONLY)
    private HelmResourcesVO resources;

    @Schema(description = "ingresses", example = "[]")
    private List<K8sIngressVO> ingresses;
}
