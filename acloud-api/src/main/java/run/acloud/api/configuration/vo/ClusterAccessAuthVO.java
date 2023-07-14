package run.acloud.api.configuration.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import run.acloud.commons.vo.HasUseYnVO;


@Getter
@Setter
@Schema(description = "클러스터 접근 인가 모델")
public class ClusterAccessAuthVO extends HasUseYnVO {

    @Schema(title = "클러스터 접근 인가 번호")
    private Integer clusterAuthSeq;

    @Schema(title = "클러스터 번호")
    private Integer clusterSeq;

    @Schema(title = "인가 유형 (SECRET / ACCESS_TOKEN)")
    private String authType;

    @Schema(title = "오너 유형 (CONTROLLER / MONITORING")
    private String ownerType;

    @Schema(title = "인가 키")
    private String authKey;

    @Schema(title = "만료 일자")
    private String expired;

}
