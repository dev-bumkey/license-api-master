package run.acloud.api.resource.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.NotBlank;
import run.acloud.api.resource.enums.ResourceType;
import run.acloud.api.resource.util.ResourceUtil;
import run.acloud.commons.vo.BaseVO;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@Schema(title = "컨테이너 모델")
public class ContainerVO extends BaseVO implements Serializable {
	
	private static final long serialVersionUID = 109269869967592043L;

	private Integer containerSeq;

	@NotBlank
	@Pattern(
			regexp = "^[a-z0-9]([-a-z0-9]*[a-z0-9])?$"
	)
	@Size(
			max = 63
	)
	private String containerName;
	
	private Integer componentSeq;

	private Integer projectId;

	private Integer buildTaskSeq;
	private Integer buildSeq;

	@Deprecated
	private String imageName;

	@NotBlank
	private String fullImageName;

	@Deprecated
	private String imageTag;

	@Schema(title = "image pull policy", allowableValues = {"Always","Never","IfNotPresent"})
	private String imagePullPolicy;
	
	private String description;

	@Deprecated
	private String command;

	@Deprecated
	private String arguments;

	private List<String> cmds;
	private List<String> args;

	private String privateRegistryYn = "N";

	private String initContainerYn = "N";
	
	private List<ContainerPortVO> containerPorts;

	@Deprecated
	private List<ContainerPortVO> servicePorts;

	@Valid
	private List<ContainerEnvVarVO> containerEnvVars;

	@Schema(title = "containerVolumes", example = "[]")
	private List<ContainerVolumeVO> containerVolumes;

    @Valid
    @Schema(description = "volumeMounts", example = "[]")
	private List<VolumeMountVO> volumeMounts;
	
	private SecurityContextVO securityContext;

	private ContainerResourcesVO resources;

	private ContainerProbeVO livenessProbe;

	private ContainerProbeVO readinessProbe;

	private ContainerLifecycleVO lifecycle;

	public String getContainerCocktailId() {
		return ResourceUtil.makeCocktailId(ResourceType.CONTAINER, containerSeq);
	}
}
