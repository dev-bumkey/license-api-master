package run.acloud.api.resource.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import run.acloud.commons.vo.BaseVO;

/**
 * @author: wschoi@acornsoft.io
 * Created on 2017. 10. 12.
 */
@Getter
@Setter
@Schema(description = "ObjectReference 모델")
public class K8sObjectReferenceVO extends BaseVO {

    @Schema(title = "apiVersion")
    private String apiVersion = null;
    @Schema(title = "fieldPath")
    private String fieldPath = null;
    @Schema(title = "kind")
    private String kind = null;
    @Schema(title = "name")
    private String name = null;
    @Schema(title = "namespace")
    private String namespace = null;
    @Schema(title = "resourceVersion")
    private String resourceVersion = null;
    @Schema(title = "uid")
    private String uid = null;
}
