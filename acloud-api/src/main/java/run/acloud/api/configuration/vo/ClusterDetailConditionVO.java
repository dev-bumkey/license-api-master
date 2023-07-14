package run.acloud.api.configuration.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import run.acloud.api.cserver.vo.ServicemapDetailVO;
import run.acloud.api.resource.vo.ConfigMapGuiVO;
import run.acloud.api.resource.vo.K8sNamespaceVO;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Schema(description = "클러스터 모델")
public class ClusterDetailConditionVO extends ClusterVO {
	
	@Schema(title = "클러스터 현황")
	private ClusterConditionVO condition;

	@Schema(title = "featureGates 설정 정보")
	private Map<String, Boolean> featureGates;

	@Schema(title = "Shell 권한")
	private List<UserClusterRoleIssueVO> shellRoles;

	@Schema(title = "Kubeconfig 권한")
	private List<UserClusterRoleIssueVO> kubeconfigRoles;

	@Schema(title = "namespace 목록")
	private List<K8sNamespaceVO> namespaces;

	@Schema(title = "서비스맵 목록", example = "[]")
	private List<ServicemapDetailVO> servicemaps;

	@Schema(title = "addon 목록", example = "[]")
	private List<ConfigMapGuiVO> addons;


}
