package run.acloud.api.cserver.vo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import run.acloud.api.configuration.vo.ServiceVO;
import run.acloud.api.resource.enums.RestartPolicyType;
import run.acloud.api.resource.vo.*;

import java.util.List;

/**
 * Created by dy79@acornsoft.io on 2017. 2. 15.
 */
@Getter
@Setter
public class ServerDeployVO  {
	@NotNull
	@Size(min = 1)
	String applicationVersion;

	List<ContainerVO> initContainers;
	
	@NotNull
	@Size(min = 1)
	@Valid
	List<ContainerVO> containers;

	@Null
	@Size(min = 1)
    HpaGuiVO hpa;

	@Null
	@Size(min = 1)
    DeploymentStrategyVO strategy;

	@Null
	@Size(min = 1)
	RestartPolicyType restartPolicy;

	ServerVO server;

	// internal user
    ServiceVO service;

    // internal user
    private List<ContainerVolumeVO> volumes;

	private List<PersistentVolumeClaimGuiVO> volumeTemplates;

    private List<ServiceSpecGuiVO> services;

    private String previousTemplate;
}
