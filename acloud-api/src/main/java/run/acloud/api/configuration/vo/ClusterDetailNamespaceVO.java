package run.acloud.api.configuration.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import run.acloud.api.cserver.vo.ServicemapVO;
import run.acloud.api.resource.vo.K8sNamespaceVO;

import java.util.List;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Schema(description = "클러스터 상세 모델")
public class ClusterDetailNamespaceVO extends ClusterVO {

	@Schema(title = "Shell 권한")
	private List<UserClusterRoleIssueVO> shellRoles;

	@Schema(title = "Kubeconfig 권한")
	private List<UserClusterRoleIssueVO> kubeconfigRoles;

	@Schema(title = "namespace 목록")
	private List<K8sNamespaceVO> namespaces;

	@Schema(title = "서비스맵 목록", example = "[]")
	private List<ServicemapVO> servicemaps;


}
