package run.acloud.api.configuration.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import run.acloud.commons.vo.BaseVO;

import java.util.List;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@Schema(description = "사용자 클러스터 권한 발급 이력 모델")
public class UserClusterRoleIssueHistoryVO extends BaseVO {
	
	@Schema(name = "이력 번호")
//	@JsonIgnore
	private Integer historySeq;

	@Schema(name = "사용자 번호", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull
	private Integer userSeq;

	@Schema(name = "사용자 아이디")
	private String userId;

	@Schema(name = "사용자 이름")
	private String userName;

	@Schema(name = "클러스터 번호", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull
	private Integer clusterSeq;

	@Schema(name = "클러스터 ID")
	private String clusterId;

	@Schema(name = "클러스터 명")
	private String clusterName;

	@Schema(name = "발급유형", requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = {"SHELL","KUBECONFIG"})
	@NotNull
	private String issueType;

	@Schema(name = "바인딩유형", allowableValues = {"CLUSTER","NAMESPACE"})
	@NotNull
	private String bindingType;

	@Schema(name = "만료일시", description = "yyyy-MM-dd")
	@JsonFormat(pattern = "yyyy-MM-dd")
	private String expirationDatetime;

	@Schema(name = "발급권한")
	private String issueRole;

	@Schema(name = "발급된 serviceAccount(shell), userAccount(kubeconfig) 명")
	private String issueAccountName;

	@Schema(name = "발급 사용자 번호", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull
	private Integer issueUserSeq;

	@Schema(name = "발급 사용자 아이디")
	private String issueUserId;

	@Schema(name = "발급 사용자 이름")
	private String issueUserName;

	@Schema(name = "이력상태", requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = {"GRANT","CHANGE","REVOKE","EXPIRED"})
	private String historyState;

	@Schema(name = "이력일시", description = "발급일시와 값을 그대로 셋팅")
	private String historyDatetime;

	@Schema(name = "이력상태", description = "로직상 이슈가 있을 시 정보 입력")
	private String historyMessage;

	@Schema(name = "바인딩유형이 'NAMESPACE'일 경우 바인딩 구성 정보")
	private List<UserClusterRoleIssueBindingVO> bindings;

	@Schema(name = "binding된 namespace", description = "조회정보")
	private String bindingNamespace;

	@Schema(name = "binding된 발급권한", description = "조회정보")
	private String bindingIssueRole;
}
