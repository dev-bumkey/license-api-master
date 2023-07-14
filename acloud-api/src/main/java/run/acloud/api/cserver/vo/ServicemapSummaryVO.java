package run.acloud.api.cserver.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import run.acloud.api.resource.enums.ClusterType;
import run.acloud.api.resource.enums.CubeType;
import run.acloud.api.resource.enums.ProviderCode;
import run.acloud.api.resource.enums.ProviderRegionCode;

/**
 * @author dy79@acornsoft.io on 2017. 1. 17.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Schema(description = "서비스맵 요약 모델")
public class ServicemapSummaryVO extends ServicemapVO {

	@Schema(title = "k8s 존재여부")
	private Boolean k8sResourceExists;

	@Schema(title = "클러스터 프로바이더 코드")
	private String providerCode;

	@Schema(title = "클러스터 프로바이더 명")
	private String providerName;

	public String getProviderName() {
		if (StringUtils.isNotBlank(providerCode) && ProviderCode.exists(providerCode)) {
			providerName = ProviderCode.valueOf(providerCode).getDescription();
		} else {
			providerName = null;
		}
		return providerName;
	}

	@Schema(title = "클러스터 리전 코드")
	private String regionCode;

	@Schema(title = "클러스터 리전 명")
	private String regionName;

	public String getRegionName() {
		if (StringUtils.isNotBlank(regionCode) && ProviderCode.exists(providerCode) && CollectionUtils.isNotEmpty(ProviderCode.valueOf(providerCode).getRegionCodes())) {
			regionName = ProviderCode.valueOf(providerCode).getRegionCodes().stream().filter(rc -> (StringUtils.equalsIgnoreCase(rc.getRegionCode(), regionCode))).map(ProviderRegionCode::getRegionName).findFirst().orElseGet(() ->null);
		} else {
			regionName = null;
		}
		return regionName;
	}

	@Schema(title = "클러스터 유형")
	private ClusterType clusterType;

	@Schema(title = "클러스터 설치 유형")
	private CubeType cubeType;

	@Schema(title = "클러스터 상태")
	private String clusterState;
}
