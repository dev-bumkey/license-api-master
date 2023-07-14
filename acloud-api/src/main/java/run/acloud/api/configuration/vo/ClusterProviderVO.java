package run.acloud.api.configuration.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import run.acloud.commons.vo.BaseVO;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@Schema(description = "서비스 클러스터 모델")
public class ClusterProviderVO extends BaseVO {
    private static final long serialVersionUID = -6625944540634307752L;

    @Schema(description = "Account 번호")
    private Integer accountSeq;

    @Schema(description = "클러스터 번호")
    private Integer clusterSeq;

    @Schema(description = "클러스터와 매핑할 프로바이더(AccessKey) Seq")
    private Integer providerAccountSeq;
}
