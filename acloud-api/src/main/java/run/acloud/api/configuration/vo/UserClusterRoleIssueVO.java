package run.acloud.api.configuration.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import run.acloud.api.resource.enums.ProviderCode;
import run.acloud.api.resource.enums.ProviderRegionCode;
import run.acloud.commons.vo.HasUseYnVO;

import java.util.List;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@Schema(description = "사용자 클러스터 권한 발급 모델")
public class UserClusterRoleIssueVO extends HasUseYnVO {
	
	@Schema(name = "사용자 번호", requiredMode = Schema.RequiredMode.REQUIRED)
	private Integer userSeq;

	@Schema(name = "사용자 명")
	private String userName;

	@Schema(name = "사용자 ID")
	private String userId;

	@Schema(name = "사용자 권한")
	private String userRole;

	@Schema(name = "클러스터 번호", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull
	private Integer clusterSeq;

    @Schema(name = "클러스터 ID", title = "조회정보")
    private String clusterId;

    @Schema(name = "클러스터 명", title = "조회정보")
    private String clusterName;

	@Schema(name = "클러스터 상태", description = "클러스터 상태")
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

	@Schema(name = "발급유형", allowableValues = {"SHELL","KUBECONFIG"})
	@NotNull
	private String issueType;

	@Schema(name = "바인딩유형", allowableValues = {"CLUSTER","NAMESPACE"})
	@NotNull
	private String bindingType;

	@Schema(name = "발급일시")
	private String issueDatetime;

	@Schema(name = "만료일시", description = "yyyy-MM-dd")
	@JsonFormat(pattern = "yyyy-MM-dd")
	private String expirationDatetime;

	@Schema(name = "발급권한")
	private String issueRole;

	@Schema(name = "발급된 serviceAccount(shell), userAccount(kubeconfig) 명")
	private String issueAccountName;

	@Schema(name = "발급 쉘 경로")
	private String issueShellPath;

	@Schema(name = "Issue 환경 정보")
	@JsonIgnore
	private String issueConfig;

	@Schema(name = "바인딩 구성 정보")
	@JsonIgnore
	private String bindingConfig;

	@Schema(name = "바인딩유형이 'NAMESPACE'일 경우 바인딩 구성 정보")
	private List<UserClusterRoleIssueBindingVO> bindings;

	@Schema(name = "발급자 ID", description = "조회정보")
	private String creatorId;

	@Schema(name = "발급자 이름", description = "조회정보")
	private String creatorName;

	@Schema(name = "발급(수정)자 ID", description = "조회정보")
	private String updaterId;

	@Schema(name = "발급(수정)자 이름", description = "조회정보")
	private String updaterName;

	@Schema(name = "클러스터", description = "조회정보")
	private ClusterVO cluster;

	@Schema(name = "accountSeq", description = "조회정보")
	private Integer accountSeq;

	@Schema(name = "binding된 namespace", description = "조회정보")
	private String bindingNamespace;

	@Schema(name = "binding된 발급권한", description = "조회정보")
	private String bindingIssueRole;

	@Schema(name = "발급이력", description = "등록/수정/삭제시 필요")
	private UserClusterRoleIssueHistoryVO history;

	@Schema(name = "이전 바인딩유형", description = "바인딩 유형이 변경시 cluster-api 셋팅용", allowableValues = {"CLUSTER","NAMESPACE"})
	private String preBindingType;

}
