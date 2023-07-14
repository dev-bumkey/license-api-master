package run.acloud.api.resource.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.google.gson.annotations.SerializedName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * @author wschoi@acornsoft.io
 * Created on 2017. 7. 26.
 */
@Getter
@Setter
@Schema(name = "ClusterRoleBindingGuiVO",
        title = "ClusterRoleBindingGuiVO",
        description = "Cluster RoleBinding GUI 모델",
        implementation = ClusterRoleBindingIntegrateVO.class
)
public class ClusterRoleBindingGuiVO extends ClusterRoleBindingIntegrateVO {

    @Schema(description = "Cluster RoleBinding 이름", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(title = "생성시간")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private OffsetDateTime creationTimestamp;

    @Schema(title = "ConfigMap 설명")
    private String description;

    @Schema(description = "label")
    private Map<String, String> labels;

    @Schema(description = "annotations")
    private Map<String, String> annotations;

    public static final String SERIALIZED_NAME_ROLE_REF = "roleRef";
    @SerializedName(SERIALIZED_NAME_ROLE_REF)
    @Schema(
            name = SERIALIZED_NAME_ROLE_REF,
            description =  "roleRef"
    )
    private RoleRefVO roleRef;

    public static final String SERIALIZED_NAME_SUBJECTS = "subjects";
    @SerializedName(SERIALIZED_NAME_SUBJECTS)
    @Schema(
            name = SERIALIZED_NAME_SUBJECTS,
            description =  "Subjects holds references to the objects the role applies to."
    )
    private List<SubjectVO> subjects = null;

}
