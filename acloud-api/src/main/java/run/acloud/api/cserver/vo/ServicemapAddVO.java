package run.acloud.api.cserver.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import run.acloud.api.cserver.enums.NetworkPolicyCreationType;
import run.acloud.api.resource.vo.LimitRangeGuiVO;
import run.acloud.api.resource.vo.NetworkPolicyGuiVO;
import run.acloud.api.resource.vo.ResourceQuotaGuiVO;
import run.acloud.commons.vo.HasUseYnVO;

/**
 * @author dy79@acornsoft.io on 2017. 1. 17.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@Schema(description = "서비스맵 등록 모델")
public class ServicemapAddVO extends HasUseYnVO {

	@Schema(title="서비스맵순번", accessMode = Schema.AccessMode.READ_ONLY)
	@JsonIgnore
	private Integer servicemapSeq;

	@Schema(title="클러스터순번", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull
	private Integer clusterSeq;

	@Schema(title="네임스페이스명", requiredMode = Schema.RequiredMode.REQUIRED)
	@Valid
	@NotNull
	@Size(min = 1, max = 50)
	private String namespaceName;

	@Schema(title="서비스맵명", requiredMode = Schema.RequiredMode.REQUIRED)
	@Valid
	@NotNull
    @Size(min = 1, max = 50)
	private String servicemapName;

	@Schema(title="서비스맵그룹순번", description = "optional")
	private Integer servicemapGroupSeq;

	@Schema(title="서비스순번(워크스페이스)", description = "optional")
	private Integer serviceSeq;

	@Schema(name = "limitRange", description = "limitRange 생성용")
	private LimitRangeGuiVO limitRange;

	@Schema(name = "resourceQuota", description = "resourceQuota 생성용")
	private ResourceQuotaGuiVO resourceQuota;

	@Schema(name = "networkPolicyCreationType", description = "NetworkPolicy 생성용")
	private NetworkPolicyCreationType networkPolicyCreationType;

	@Schema(name = "networkPolicy", description = "NetworkPolicy 생성용")
	private NetworkPolicyGuiVO networkPolicy;

}