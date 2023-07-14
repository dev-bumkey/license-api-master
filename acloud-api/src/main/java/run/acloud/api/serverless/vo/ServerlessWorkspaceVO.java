package run.acloud.api.serverless.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import run.acloud.api.auth.vo.UserVO;
import run.acloud.api.configuration.enums.ClusterTenancy;
import run.acloud.api.configuration.enums.ServiceType;
import run.acloud.api.configuration.vo.AccountVO;
import run.acloud.api.configuration.vo.ClusterVO;
import run.acloud.api.configuration.vo.ExternalRegistryVO;
import run.acloud.api.configuration.vo.ServiceRegistryVO;
import run.acloud.api.cserver.vo.ServicemapDetailResourceVO;
import run.acloud.api.cserver.vo.ServicemapGroupVO;
import run.acloud.commons.vo.HasUseYnVO;

import java.util.List;

@Getter
@Setter
@Schema(description = "서버리스 워크스페이스 모델")
public class ServerlessWorkspaceVO extends HasUseYnVO {

	@Schema(title = "워크스페이스 번호", requiredMode = Schema.RequiredMode.REQUIRED)
	private Integer serviceSeq;

	/** R4.4.0 : 추가 for Cluster Tenancy **/
	@Schema(title = "클러스터 테넌시 (HARD, SOFT)")
	private ClusterTenancy clusterTenancy;

	@Schema(title = "워크스페이스 유형 (NORMAL, PLATFORM)", allowableValues = {"NORMAL","PLATFORM"})
	private ServiceType serviceType;

	@Schema(title = "워크스페이스 이름", requiredMode = Schema.RequiredMode.REQUIRED)
	private String serviceName;

	@Schema(title = "설명", requiredMode = Schema.RequiredMode.REQUIRED)
	private String description;

	@Schema(title = "색상 코드", requiredMode = Schema.RequiredMode.REQUIRED)
	private String colorCode;

	@Schema(title = "정렬 순서", requiredMode = Schema.RequiredMode.REQUIRED)
	private int sortOrder;

	@Schema(title = "워크스페이스가 사용하는 레지스트리 사용자 id")
	@JsonIgnore
	private String registryUserId;

	@Schema(title = "워크스페이스가 사용하는 레지스트리 사용자 암호")
	@JsonIgnore
	private String registryUserPassword;

	@Schema(title = "내부 조회용")
	@JsonIgnore
	private Integer accountSeq;

	@Schema(title = "플랫폼", example = "null", accessMode = Schema.AccessMode.READ_ONLY)
	private AccountVO account;

	@Schema(title = "워크스페이스가 사용할 수 있는 사용자 목록")
	private List<UserVO> users;

	@Schema(title = "워크스페이스가 사용할 수 있는 클러스터 목록")
	private List<ClusterVO> clusters;

	@Schema(title = "워크스페이스가 사용할 수 있는 서비스맵 목록")
	private List<ServicemapDetailResourceVO> servicemaps;

	@Schema(title = "워크스페이스가 사용할 수 있는 서비스맵 그룹 목록")
	private List<ServicemapGroupVO> servicemapGroups;

	@Schema(title = "워크스페이스가 사용할 수 있는 레지스트리 프로젝트 목록")
	private List<ServiceRegistryVO> projects;

	@Schema(title = "워크스페이스가 포함된 외부 레지스트리 목록")
	private List<ExternalRegistryVO> externalRegistries;

	@Schema(title = "워크스페이스가 사용할 수 있는 서버리스 목록", example = "[]")
	private List<ServerlessVO> serverlesses;
}
