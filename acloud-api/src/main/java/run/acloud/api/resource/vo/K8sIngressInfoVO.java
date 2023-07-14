package run.acloud.api.resource.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import run.acloud.api.cserver.vo.ServicemapSummaryVO;

/**
 * @author: wschoi@acornsoft.io
 * Created on 2017. 10. 12.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@Schema(description = "k8s Ingress 정보 모델")
public class K8sIngressInfoVO extends K8sIngressVO {

    @Schema(title = "서비스맵 정보")
    private ServicemapSummaryVO servicemapInfo;
}
