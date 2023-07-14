package run.acloud.api.serverless.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import run.acloud.api.resource.vo.K8sNamespaceVO;
import run.acloud.commons.vo.HasUseYnVO;

import java.util.List;

@Getter
@Setter
@Schema(description = "서비리스 모델")
public class ServerlessVO extends HasUseYnVO {
	
	@Schema(title = "서버리스 순번")
	private Integer serverlessSeq;

	@Schema(title = "서비스맵 순번", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull
	private Integer servicemapSeq;

	@Schema(title = "프로젝트 이름", description = "서비스맵, 네임스페이스명과 동일한 자원명", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull
	private String projectName;

	@Schema(title = "클러스터ID")
	private String clusterId;

	@Schema(title = "클러스터 순번")
	private Integer clusterSeq;

	@Schema(title = "네임스페이스 이름")
	private String namespaceName;

	@Schema(title = "네임스페이스 k8s 정보")
	@JsonIgnore
	private K8sNamespaceVO namespaceInfo;

	@Schema(title = "네임스페이스 상태", allowableValues = {"Active","Terminating"})
	private String status;

	@Schema(title = "useNamespace = true시 셋팅, k8s 존재 여부")
	private Boolean k8sResourceExists;

	@Schema(title = "서버리스 프로젝트 생성일")
	private String created;

	@Schema(title = "서비리스 정보 목록")
	private List<ServerlessInfoVO> serverlessInfos;

}
