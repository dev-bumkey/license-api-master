package run.acloud.api.cserver.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import run.acloud.api.configuration.vo.ServiceVO;
import run.acloud.api.resource.vo.*;

import java.util.List;

/**
 * Created by dy79@acornsoft.io on 2017. 1. 19.
 */
@Getter
@Setter
@Schema(name = "ServerGuiVO",
		title = "ServerGuiVO",
		description = "서버 배포 GUI 모델",
		allOf = {ServerIntegrateVO.class}
)
public class ServerGuiVO extends ServerIntegrateVO{
	@NotNull
	@Valid
	ServerVO server;

	@Schema(title = "initContainers", example = "[]")
	List<ContainerVO> initContainers;

	@Size(min = 1)
	@Valid
	@Schema(title = "containers", example = "[]")
	List<ContainerVO> containers;

    @Valid
	List<ContainerVolumeVO> volumes;

	List<PersistentVolumeClaimGuiVO> volumeTemplates;

	@Deprecated
	List<ServiceSpecGuiVO> services;

	@Valid
	@NotNull
	@Schema(title = "component", example = "null", accessMode = Schema.AccessMode.READ_ONLY)
	ComponentVO component;

	// internal use only
	@JsonIgnore
	private ServiceVO service;

	// internal use only
	@JsonIgnore
	private String deploymentYaml;

	Integer creator;
}
