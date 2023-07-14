package run.acloud.api.configuration.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import run.acloud.commons.vo.BaseVO;

/**
 * @author wschoi@acornsoft.io
 * Created on 2017. 7. 7.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@Schema(description = "서비스 클러스터 모델")
public class ServiceClusterVO extends BaseVO {

    @Schema(description = "서비스 번호")
    private Integer serviceSeq;

    @Schema(description = "클러스터 번호")
    private Integer clusterSeq;
}
