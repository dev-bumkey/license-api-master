package run.acloud.api.configuration.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import run.acloud.commons.vo.BaseVO;

@Getter
@Setter
@Schema(description = "Ingress Host Info 모델")
public class IngressHostInfoVO extends BaseVO {

	@Schema(title = "클러스터 번호")
	private Integer clusterSeq;

	@Schema(title = "클러스터 명")
	private String clusterName;

	@Schema(title = "클러스터 ID")
	private String clusterId;

	@Schema(title = "servicemapSeq")
	private Integer servicemapSeq;

	@Schema(title = "servicemap 명")
	private String servicemapName;

	@Schema(title = "namespace 명")
	private String namaespaceName;

	@Schema(title = "host 명")
	private String hostName;

	@Schema(title = "Ingress 명")
	private String ingressName;
}
