package run.acloud.api.resource.vo;

import lombok.Getter;
import lombok.Setter;
import run.acloud.api.cserver.enums.VolumeType;
import run.acloud.commons.vo.BaseVO;

import java.util.List;

/**
 * @author dy79@acornsoft.io on 2017. 1. 17.
 */
@Getter
@Setter
public class ServerDetailParamForPVVO extends BaseVO {
	
	private Integer serviceSeq;

	private Integer appmapSeq;

	private String appmapName;

	private String namespaceName;

	private Integer jobSeq;
	
	private Integer taskSeq;

	private Integer componentSeq;

	private String workloadType;

	private String workloadVersion;

	private String serverName;

	private String stateCode;

	private Integer clusterVolumeSeq;

	private String clusterVolumeName;

	private VolumeType clusterVolumeType;

	private List<ContainerVolumeVO> containerVolumes;

}