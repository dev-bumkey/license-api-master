package run.acloud.api.resource.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * @author wschoi@acornsoft.io
 * Created on 2017. 7. 26.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class K8sRoleVO extends RoleGuiVO {

    @Schema(description = "배포 정보")
    private String deployment;

    @Schema(description = "배포 정보 (yaml)")
    private String deploymentYaml;

}
