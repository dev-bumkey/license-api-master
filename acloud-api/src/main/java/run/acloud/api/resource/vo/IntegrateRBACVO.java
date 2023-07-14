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
@Schema(title = "IntegrateRBACVO",
        description = "롤, 클러스터 롤, 롤 바인딩, 클러스터 롤 바인딩 통합 모델")
public class IntegrateRBACVO {

    @Schema(title = "cluster role 정보 목록")
    private List<K8sClusterRoleVO> clusterRoles;

    @Schema(title = "role 정보 목록")
    private List<K8sRoleVO> roles;

    @Schema(title = "cluster role Binding 정보 목록")
    private List<K8sClusterRoleBindingVO> clusterRoleBindings;

    @Schema(title = "role Binding 정보 목록")
    private List<K8sRoleBindingVO> roleBindings;
}
