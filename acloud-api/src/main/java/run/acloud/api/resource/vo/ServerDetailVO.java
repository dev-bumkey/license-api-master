package run.acloud.api.resource.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import run.acloud.api.configuration.vo.ServiceVO;
import run.acloud.api.cserver.vo.WorkloadGroupVO;
import run.acloud.commons.vo.HasUpdaterVO;

import java.util.List;

/**
 * Created by dy79@acornsoft.io on 2017. 1. 19.
 */
@Getter
@Setter
public class ServerDetailVO extends HasUpdaterVO {
	
	private Integer componentSeq;
	
	private WorkloadGroupVO workloadGroup;

	@Schema(title = "component", example = "null", accessMode = Schema.AccessMode.READ_ONLY)
	private ComponentVO component;

	@Schema(title = "volumes", example = "[]")
	private List<ContainerVolumeVO> volumes;

	@Schema(title = "volumeTemplates", example = "[]")
	private List<PersistentVolumeClaimGuiVO> volumeTemplates;

	@Schema(title = "services", example = "[]")
	private List<ServiceSpecGuiVO> services;

	@Schema(title = "server", example = "null", accessMode = Schema.AccessMode.READ_ONLY)
	private ServerVO server;

	// internal use only
	@Schema(title = "service", example = "null", accessMode = Schema.AccessMode.READ_ONLY)
	private ServiceVO service;
}
