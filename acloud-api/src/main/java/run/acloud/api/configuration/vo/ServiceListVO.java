package run.acloud.api.configuration.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import run.acloud.api.cserver.vo.ServiceUserVO;
import run.acloud.api.cserver.vo.ServicemapDetailResourceVO;
import run.acloud.api.cserver.vo.ServicemapGroupVO;

import java.util.List;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Schema(description = "워크스페이스 목록 모델")
public class ServiceListVO extends ServiceVO {

	@Schema(title = "플랫폼", example = "null", accessMode = Schema.AccessMode.READ_ONLY)
	private AccountVO account;

	@Schema(title = "워크스페이스를 사용할 수 있는 사용자번호 목록")
	private List<Integer> userSeqs;

	@Schema(title = "워크스페이스가 포함된 사용자 워크스페이스 권한 목록")
	private List<ServiceUserVO> serviceUsers;

	@Schema(title = "워크스페이스가 사용할 수 있는 클러스터 목록")
	private List<ClusterVO> clusters;

	@Schema(title = "워크스페이스가 사용할 수 있는 서비스맵 목록")
	private List<ServicemapDetailResourceVO> servicemaps;

	@Schema(title = "워크스페이스의 서비스맵 Group 목록")
	private List<ServicemapGroupVO> servicemapGroups;

    @Schema(title = "워크스페이스가 사용할 수 있는 레지스트리 프로젝트 목록")
	private List<ServiceRegistryVO> projects;

	@Schema(title = "워크스페이스가 포함된 외부 레지스트리 목록")
	private List<ExternalRegistryVO> externalRegistries;

}
