package run.acloud.api.monitoring.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * @author: wschoi@bettertomorrow.com
 * Created on 2018. 4. 13.
 * 서비스 모니터링 정보를 구하는테 필요한 정보를 모아서 반환할 때 사용한다.
 */
@Schema(title = "워크스페이스에 속한 클러스터, 서비스맵 모델")
@Getter
@Setter
public class ServiceMonitoringVO {
    private Integer serviceSeq;
    private Integer servicemapSeq;
    private Integer clusterSeq;
    private String namespaceName;
    private Boolean k8sResourceExists;
    private String servicemapName;
    private String serviceName;
}
