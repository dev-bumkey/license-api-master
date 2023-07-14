package run.acloud.api.resource.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import run.acloud.api.cserver.vo.ServicemapSummaryVO;

import java.util.List;

/**
 * @author: wschoi@acornsoft.io
 * Created on 2017. 10. 12.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Schema(description = "k8s Service 정보 모델")
public class K8sServiceInfoVO extends K8sServiceVO{

    @Schema(title = "서비스맵 정보")
    private ServicemapSummaryVO servicemapInfo;

    private List<ComponentVO> workloads;
}
