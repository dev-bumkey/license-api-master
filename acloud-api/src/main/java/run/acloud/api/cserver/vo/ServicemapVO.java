package run.acloud.api.cserver.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import run.acloud.commons.vo.HasUseYnVO;

import java.util.List;

/**
 * @author dy79@acornsoft.io on 2017. 1. 17.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@Schema(description = "서비스맵 모델")
public class ServicemapVO extends HasUseYnVO {

	@Schema(title="서비스맵순번")
	private Integer servicemapSeq;

	@Schema(title="클러스터순번")
	private Integer clusterSeq;

	@Schema(title = "클러스터ID")
	private String clusterId;

	@Schema(title = "클러스터명")
	private String clusterName;

	@Schema(title="네임스페이스명")
	private String namespaceName;

	@Schema(title="서비스맵명")
	private String servicemapName;

	@Schema(title="워크로드그룹 목록")
	private List<WorkloadGroupVO> workloadGroups;

	@Schema(title="서비스맵맵핑 목록")
	private List<ServicemapMappingVO> servicemapMappings;
}