package run.acloud.api.catalog.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import run.acloud.api.resource.enums.K8sApiKindType;

import java.io.Serializable;
import java.util.Map;

@Getter
@Setter
@Schema(title = "K8s Object Map", description = "K8s Object 리스트를 보관하기 위한 객체")
public class K8sObjectMapVO implements Serializable {
    private static final long serialVersionUID = -2856288531959527649L;

    @Schema(title = "k8sApiKindType")
    private K8sApiKindType k8sApiKindType;

    @Schema(title = "apiVersion")
    private String apiVersion;

    @Schema(title = "name")
    private String name;

    @Schema(title = "namespace")
    private String namespace;

    @Schema(title = "k8sObjMap")
    private Map<String, Object> k8sObjMap;

    @Schema(title = "k8sObj")
    private Object k8sObj;
}