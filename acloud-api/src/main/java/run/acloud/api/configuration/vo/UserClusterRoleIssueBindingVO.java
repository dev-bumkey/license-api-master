package run.acloud.api.configuration.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import run.acloud.commons.vo.HasCreatorVO;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@Schema(description = "사용자 클러스터 권한 발급 바인딩 모델")
public class UserClusterRoleIssueBindingVO extends HasCreatorVO {

	@Schema(name = "사용자 번호")
	private Integer userSeq;

	@Schema(name = "클러스터 번호")
	private Integer clusterSeq;

	@Schema(name = "발급유형", allowableValues = {"SHELL","KUBECONFIG"})
	private String issueType;

	@Schema(name = "발급된 serviceAccount(shell), userAccount(kubeconfig) 명")
	private String issueAccountName;

	@Schema(name = "바인딩 이력 번호")
	private Integer bindingHistorySeq;

	@Schema(name = "이력 번호")
	private Integer historySeq;

	@Schema(name = "namespace")
	private String namespace;

	@Schema(name = "발급권한")
	private String issueRole;

}
