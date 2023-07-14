package run.acloud.api.configuration.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * @author wschoi@acornsoft.io
 * Created on 2017. 6. 20.
 */
@Getter
@Setter
public class ClusterVolumeParamterVO implements Serializable {
    @Schema(description = "Cluster Volume 일련 번호")
    @Deprecated
    private Integer volumeSeq;

    @Schema(description = "Parameter 이름", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(description = "Parameter 값")
    private String value;
}
