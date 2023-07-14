package run.acloud.api.resource.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * @author wschoi@acornsoft.io
 * Created on 2017. 7. 26.
 */
@Getter
@Setter
public class K8sNetworkPolicyVO extends NetworkPolicyGuiVO {

    @Schema(description = "배포 정보")
    private String deployment;

    @Schema(description = "배포 정보 (yaml)")
    private String deploymentYaml;

}
