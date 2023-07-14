package run.acloud.api.cserver.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import run.acloud.api.resource.vo.K8sLimitRangeVO;
import run.acloud.api.resource.vo.K8sNetworkPolicyVO;
import run.acloud.api.resource.vo.K8sResourceQuotaVO;

import java.util.List;

/**
 * @author dy79@acornsoft.io on 2017. 1. 17.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Schema(title = "서비스맵 상세 리소스 모델", description = "사용여부에 따라 limitRange, resourceQuota, networkPolicy 추가 조회")
public class ServicemapDetailResourceVO extends ServicemapDetailVO {

	@Schema(title = "limitRange 목록")
	private List<K8sLimitRangeVO> limitRanges;

	@Schema(title = "resourceQuota 목록")
	private List<K8sResourceQuotaVO> resourceQuotas;

	@Schema(title = "NetworkPolicy 목록")
	private List<K8sNetworkPolicyVO> networkPolicies;

}