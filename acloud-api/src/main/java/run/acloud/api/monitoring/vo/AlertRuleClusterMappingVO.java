package run.acloud.api.monitoring.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import run.acloud.api.resource.enums.ProviderCode;
import run.acloud.api.resource.enums.ProviderRegionCode;
import run.acloud.commons.vo.BaseVO;

@Getter
@Setter
@Schema(description = "Alert Rule 클러스터 모델")
public class AlertRuleClusterMappingVO extends BaseVO {

    @Schema(title = "클러스터 번호")
    private Integer clusterSeq;

    @Schema(title = "Alert 규칙 번호")
    private Integer alertRuleSeq;

    @Schema(name = "클러스터 ID", title = "조회정보")
    private String clusterId;

    @Schema(name = "클러스터 명", title = "조회정보")
    private String clusterName;

    @Schema(name = "클러스터 상태", title = "클러스터 상태")
    private String clusterState;

    @Schema(name = "클러스터 region 코드", title = "조회정보")
    private String regionCode;

    @Schema(name = "클러스터 region 명", title = "조회정보")
    private String regionName;

    public String getRegionName() {
        if (StringUtils.isNotBlank(regionCode) && ProviderCode.exists(providerCode) && CollectionUtils.isNotEmpty(ProviderCode.valueOf(providerCode).getRegionCodes())) {
            regionName = ProviderCode.valueOf(providerCode).getRegionCodes().stream().filter(rc -> (StringUtils.equalsIgnoreCase(rc.getRegionCode(), regionCode))).map(ProviderRegionCode::getRegionName).findFirst().orElseGet(() ->null);
        } else {
            regionName = null;
        }
        return regionName;
    }

    @Schema(name = "클러스터 provider 코드", title = "조회정보")
    private String providerCode;

    @Schema(name = "클러스터 provider 명", title = "조회정보")
    private String providerCodeName;

    public String getProviderCodeName() {
        if (StringUtils.isNotBlank(providerCode) && ProviderCode.exists(providerCode)) {
            providerCodeName = ProviderCode.valueOf(providerCode).getDescription();
        } else {
            providerCodeName = null;
        }
        return providerCodeName;
    }
}