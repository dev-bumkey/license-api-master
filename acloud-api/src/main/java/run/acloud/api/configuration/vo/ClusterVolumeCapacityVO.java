package run.acloud.api.configuration.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import run.acloud.commons.vo.BaseVO;

@Getter
@Setter
@Schema(description = "클러스터 볼륨 용량 모델")
public class ClusterVolumeCapacityVO extends BaseVO {

	@Schema(title = "클러스터 번호")
	private Integer clusterSeq;

	@Schema(title = "cluster volume 번호")
	@Deprecated
	private Integer volumeSeq;

	@Schema(title = "Storage 명")
	private String storageName;

	@Schema(title = "allocatedCapacity")
	private long allocatedCapacity = 0L;

	@Schema(title = "PVC Count")
	private long volumeCount = 0L;
}
