package run.acloud.api.resource.vo;

import lombok.Getter;
import lombok.Setter;
import run.acloud.commons.vo.HasStateVO;

import java.util.List;

@Getter
@Setter
public class ComponentDetailsVO extends HasStateVO {
	private Integer clusterSeq;

	private List<Integer> serviceSeqs;

	private Integer servicemapSeq;

	private String servicemapName;

	private String namespaceName;

	private List<ComponentVO> components;

	private List<ServerVO> servers;
}
