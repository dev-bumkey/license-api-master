package run.acloud.api.configuration.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import run.acloud.api.configuration.enums.ClusterTenancy;
import run.acloud.api.configuration.enums.ServiceType;
import run.acloud.commons.vo.HasUseYnVO;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@Schema(description = "워크스페이스 모델")
public class ServiceVO extends HasUseYnVO {
	
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

}
