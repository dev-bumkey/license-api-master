package run.acloud.api.resource.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * @author: wschoi@acornsoft.io
 * Created on 2017. 10. 12.
 */
@Getter
@Setter
@EqualsAndHashCode
public class K8sPodSecurityPolicyVO extends PodSecurityPolicyGuiVO{


    @Schema(title = "배포 정보")
    private String deployment;

    @Schema(title = "배포 정보 (yaml)")
    private String deploymentYaml;

    @Schema(title = "RBAC 정보")
    private IntegrateRBACVO RBAC;
}
