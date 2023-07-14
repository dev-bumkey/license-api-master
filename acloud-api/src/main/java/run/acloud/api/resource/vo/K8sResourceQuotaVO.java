package run.acloud.api.resource.vo;

import com.google.gson.annotations.SerializedName;
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
@EqualsAndHashCode(callSuper = true)
@Schema(description = "ResourceQuotaStatus defines the enforced hard limits and observed use.")
public class K8sResourceQuotaVO extends ResourceQuotaGuiVO{

    @Schema(title = "배포 정보")
    private String deployment;

    @Schema(title = "배포 정보 (yaml)")
    private String deploymentYaml;

    public static final String SERIALIZED_NAME_STATUS = "status";
    @SerializedName(SERIALIZED_NAME_STATUS)
    @Schema(
            name = SERIALIZED_NAME_STATUS,
            description =  "ResourceQuotaStatus defines the enforced hard limits and observed use."
    )
    private K8sResourceQuotaStatusVO status;
}
