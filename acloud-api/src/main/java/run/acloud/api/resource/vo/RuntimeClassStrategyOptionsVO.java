package run.acloud.api.resource.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * @author: wschoi@acornsoft.io
 * Created on 2017. 10. 12.
 */
@Getter
@Setter
@EqualsAndHashCode
public class RuntimeClassStrategyOptionsVO {
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, title = "allowedRuntimeClassNames", description = "allowedRuntimeClassNames is a whitelist of RuntimeClass names that may be specified on a pod. A value of '*' means that any RuntimeClass name is allowed, and must be the only item in the list. An empty list requires the RuntimeClassName field to be unset.")
    private List<String> allowedRuntimeClassNames = null;

    @Schema(title = "defaultRuntimeClassName", description = "defaultRuntimeClassName is the default RuntimeClassName to set on the pod. The default MUST be allowed by the allowedRuntimeClassNames list. A value of nil does not mutate the Pod.")
    private String defaultRuntimeClassName;
}
