package run.acloud.api.configuration.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import run.acloud.api.configuration.enums.ClusterTenancy;
import run.acloud.api.configuration.enums.ServiceType;
import run.acloud.commons.vo.HasUseYnVO;

import java.util.List;

@Getter
@Setter
@Schema(description = "서비스생성 모델")
public class ServiceAddVO extends HasUseYnVO {
	
	@Schema(accessMode = Schema.AccessMode.READ_ONLY)
	private Integer serviceSeq;

	@Schema(title = "서비스 이름", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull
	private String serviceName;
	
	@Schema(title = "설명", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull
	private String description;

	@Schema(title = "색상 코드", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull
	private String colorCode;

    @Schema(title = "서비스가 사용하는 레지스트리 사용자 id(사용자가 입력하지 않는다)", accessMode = Schema.AccessMode.READ_ONLY)
	@JsonIgnore
    private String registryUserId;

    @Schema(title = "서비스가 사용하는 레지스트리 사용자 암호(사용자가 입력하지 않는다)", accessMode = Schema.AccessMode.READ_ONLY)
	@JsonIgnore
	private String registryUserPassword;

	@Schema(title = "서비스를 사용할 수 있는 사용자번호 목록")
	private List<Integer> userSeqs;
	
	@Schema(title = "서비스가 사용할 수 있는 클러스터번호 목록")
	private List<Integer> clusterSeqs;

	@Schema(title = "서비스가 사용할 수 있는 클러스터 맵핑 목록")
	private List<ServiceClusterVO> serviceClusters;

    @Schema(title = "서비스가 사용할 수 있는 레지스트리 프로젝트 아이디 목록")
	private List<Integer> projectIds;

    @Schema(title = "서비스가 사용할 수 있는 레지스트리 프로젝트 목록")
	private List<ServiceRegistryVO> projects;

    @Schema(title = "서비스가 사용할 수 있는 레지스트리 프로젝트 이름")
	private String projectName;

	@Schema(title = "클러스터 테넌시 (HARD, SOFT)", accessMode = Schema.AccessMode.READ_ONLY)
	@JsonIgnore
	private ClusterTenancy clusterTenancy = ClusterTenancy.SOFT;

	@Schema(title = "서비스 유형 (NORMAL, PLATFORM)")
	private ServiceType serviceType;

//	@Schema(title = "사용 여부 : 생성 시 자동으로 'Y'로 설정됨.")
//	protected String useYn;

	@Schema(title = "Account")
	@NotNull
	protected Integer accountSeq;

	@Schema(title = "워크스페이스가 포함된 외부 레지스트리 목록")
	private List<ExternalRegistryVO> externalRegistries;

}
