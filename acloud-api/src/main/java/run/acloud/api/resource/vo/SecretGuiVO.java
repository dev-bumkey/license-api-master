package run.acloud.api.resource.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import run.acloud.api.resource.enums.JsonPatchOp;
import run.acloud.api.resource.enums.SecretType;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * @author wschoi@acornsoft.io
 * Created on 2017. 8. 29.
 */
@Getter
@Setter
@Schema(name = "SecretGuiVO",
        title = "SecretGuiVO",
        description = "Secret GUI 모델",
        allOf = {SecretIntegrateVO.class}
)
public class SecretGuiVO extends SecretIntegrateVO implements Serializable {
    private static final long serialVersionUID = -616211148650831092L;

    @Schema(description = "name")
    private String name;

    @Schema(description = "namespace")
    private String namespace;

    @Schema(title = "생성시간")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private OffsetDateTime creationTimestamp;

    @Schema(description = "description")
    private String description;

    private boolean makeCocktail = true;

    private SecretType type = SecretType.Generic;

    private Map<String, String> data;

    public SecretGuiVO putDataItem(String key, String dataItem) {
        if (this.data == null) {
            this.data = new HashMap<>();
        }
        this.data.put(key, dataItem);
        return this;
    }

    /**
     * Redion.Package : 2019.03.14 :
     * 카탈로그 저장시에 Package Type에서 자동 생성된 ConfigMap까지 모두 저장 되어 문제 발생..
     * (Package는 Configmap과 Secret을 자동으로 생성하므로 카탈로그로 저장되면 안됨..)
     * ConfigMap을 조회하여 매핑시에 Labels 정보를 함께 리턴하기 위해 추가하였으며, "release" Label이 있으면 Package에서 생성한 것으로 간주.
     */
    @Schema(description = "label")
    private Map<String, String> labels;

    public SecretGuiVO putLabelsItem(String key, String labelsItem) {
        if (this.labels == null) {
            this.labels = new HashMap<>();
        }
        this.labels.put(key, labelsItem);
        return this;
    }

    @Schema(description = "annotations")
    private Map<String, String> annotations; //R3.5

    public SecretGuiVO putAnnotationsItem(String key, String annotationsItem) {
        if (this.annotations == null) {
            this.annotations = new HashMap<>();
        }
        this.annotations.put(key, annotationsItem);
        return this;
    }

    @Schema(description = "배포 정보")
    private String deployment;

    @Schema(description = "배포 정보 (yaml)")
    private String deploymentYaml;

    @Schema(description = "DockerRegistry 유형일 경우, 외부 레지스트리 여부")
    private String externalRegistryYn = "N";

    @Schema(description = "외부 레지스트리 번호")
    private Integer externalRegistrySeq;

    @Schema(description = "TLS 유형일 경우, 공인인증서 여부")
    private String publicCertificateYn = "N";

    @Schema(description = "공인인증서 번호")
    private Integer publicCertificateSeq;

    @JsonIgnore
    private Map<String, JsonPatchOp> patchOp;

    @JsonIgnore
    private Map<String, JsonPatchOp> labelPatchOp;

    @JsonIgnore
    private Map<String, JsonPatchOp> annotationPatchOp;

    @JsonIgnore
    private Map<String, JsonPatchOp> patchDescOp;
}
