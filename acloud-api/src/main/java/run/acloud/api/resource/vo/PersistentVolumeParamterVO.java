package run.acloud.api.resource.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * @author wschoi@acornsoft.io
 * Created on 2017. 6. 20.
 */
public class PersistentVolumeParamterVO {
    @Getter
    @Setter
    @Schema(description = "Persistent Volume 일련 번호", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer volumeSeq;

    @Getter
    @Setter
    @Schema(description = "Parameter 이름", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Getter
    @Setter
    @Schema(description = "Parameter 값")
    private String value;
}
