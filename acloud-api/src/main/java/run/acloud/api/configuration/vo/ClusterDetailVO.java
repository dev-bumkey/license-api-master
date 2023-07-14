package run.acloud.api.configuration.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import run.acloud.api.cserver.vo.ServicemapVO;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Schema(description = "클러스터 상세 모델")
public class ClusterDetailVO extends ClusterVO {

	/** 2019.11.11 : @hach님 요청.. Cluster의 상세 정보 조회시 Node, Namespace, storage-class, pvc 의 갯수 추가 응답 **/
	private Integer countOfNode = 0;
	private Integer countOfNamespace = 0;
	private Integer countOfStorageClass = 0;
	private Integer countOfVolume = 0;
	private Integer countOfStaticVolume = 0;
	
	@Schema(title = "Shell 권한")
	private List<UserClusterRoleIssueVO> shellRoles;

	@Schema(title = "Kubeconfig 권한")
	private List<UserClusterRoleIssueVO> kubeconfigRoles;

	@Schema(title = "Ingress Controller 접근 호스트 정보. chartType: nginx-ingress configMap의 annotation에 acornsoft.io/ingress-url 키에 저장, key : ingress controller class name, value : 호스트 주소(protocol 제외)")
	private List<Map<String, String>> ingressHosts;

	@Schema(title = "서비스맵 목록", example = "[]")
	private List<ServicemapVO> servicemaps;


}
