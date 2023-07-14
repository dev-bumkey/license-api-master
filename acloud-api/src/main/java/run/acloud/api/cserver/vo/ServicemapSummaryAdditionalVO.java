package run.acloud.api.cserver.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import run.acloud.api.configuration.vo.IngressHostInfoVO;

import java.util.List;

/**
 * @author dy79@acornsoft.io on 2017. 1. 17.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Schema(description = "서비스맵 요약 추가 정보 모델")
public class ServicemapSummaryAdditionalVO extends ServicemapSummaryVO {

	@Schema(title = "워크로드 수 (in namespace)")
	private int serverCount = 0;

	@Schema(title = "게이트웨이(LB, Nodeport, Ingress) 수 (in namespace)")
	private int gateWayCount = 0;

	@Schema(title = "로드밸랜서 수 (in namespace)")
	private int loadBalancerCount = 0;

	@Schema(title = "노드포트 수 (in namespace)")
	private int nodePortCount = 0;

	@Schema(title = "클러스터IP 수 (in namespace)")
	private int clusterIpCount = 0;

	@Schema(title = "인그레스 수 (in namespace)")
	private int ingressCount = 0;

	@Schema(title = "볼륨요청량 (in namespace)")
	private int volumeRequestCapacity = 0;

	@Schema(title = "인그레스Host정보 (in namespace)")
	private List<IngressHostInfoVO> ingressHostInfos;
}
