package run.acloud.api.cserver.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import run.acloud.api.configuration.vo.ClusterVO;
import run.acloud.api.configuration.vo.IngressHostInfoVO;
import run.acloud.api.configuration.vo.ServiceVO;

import java.util.List;

/**
 * @author dy79@acornsoft.io on 2017. 1. 17.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Schema(description = "서비스 요약 모델")
public class ServiceSummaryVO extends ServiceVO {

    @Schema(title = "서비스맵 수")
    private int servicemapCount = 0;

    @Schema(title = "서버(워크로드) 수")
    private int serverCount = 0;

    @Schema(title = "게이트웨이(LB, Nodeport, Ingress) 수")
    private int gateWayCount = 0;

    @Schema(title = "LB 수")
    private int loadBalancerCount = 0;

    @Schema(title = "Nodeport 수")
    private int nodePortCount = 0;

    @Schema(title = "Ingress 수")
    private int ingressCount = 0;

    @Schema(title = "ClusterIP 수")
    private int clusterIpCount = 0;

    @Schema(title = "인그레스Host정보")
    private List<IngressHostInfoVO> ingressHostInfos;

    @Schema(title = "워크스페이스가 사용할 수 있는 클러스터 목록")
    private List<ClusterVO> clusters;
}
