package run.acloud.api.configuration.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import run.acloud.api.configuration.enums.GateWayNameType;
import run.acloud.commons.vo.BaseVO;

@Getter
@Setter
@Schema(description = "클러스터 게이트웨이 모델")
public class ClusterGateWayVO extends BaseVO {

	@Schema(title = "클러스터 번호")
	private Integer clusterSeq;

	@Schema(title = "게이트웨이 유형")
	private GateWayNameType gateWayType;

	@Schema(title = "게이트웨이 명")
	private String gateWayName;

	@Schema(title = "게이트웨이 설명")
	private String gateWayDesc;

	@Schema(title = "게이트웨이 상태")
	private String gateWayState;

	@Schema(title = "게이트웨이 Count")
	private long gateWayCount = 0L;
}
