package run.acloud.api.cserver.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * @author dy79@acornsoft.io on 2017. 1. 17.
 */
@Getter
@Setter
public class SoftTenantAddVO {
    @Schema(title = "servicemap 번호")
    private Integer servicemapSeq;

    @Schema(title = "servicemap group 번호")
    private Integer servicemapGroupSeq;
}
