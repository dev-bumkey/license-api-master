package run.acloud.api.cserver.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import run.acloud.api.configuration.vo.ClusterVO;
import run.acloud.api.resource.vo.ComponentVO;
import run.acloud.commons.vo.HasStateVO;

import java.util.List;

@Getter
@Setter
public class ServerStateVO extends HasStateVO {

	private Integer clusterSeq;

	private String namespaceName;

	@Schema(title = "components", example = "[]")
	private List<ComponentVO> components;

	private List<WorkloadGroupVO> workloadGroups;

	@Schema(title = "cluster", example = "null", accessMode = Schema.AccessMode.READ_ONLY)
	private ClusterVO cluster;

	@Schema(title = "clusters", example = "[]")
	private List<ClusterVO> clusters;
}
