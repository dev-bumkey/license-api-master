package run.acloud.api.monitoring.vo;

import lombok.Getter;
import lombok.Setter;
import run.acloud.api.configuration.vo.ClusterVO;

import java.util.List;

/**
 * @author: wschoi@bettertomorrow.com
 * Created on 2018. 3. 25.
 * 클러스터 모니터링 정보를 구하는테 필요한 정보를 모아서 반환할 때 사용한다.
 */
@Getter
@Setter
public class ClusterMonitoringVO {
    private int clusterSeq;
    private int groupSeq;
    private String namespaceName;
    private String componentName;
    private List<String> podNames;
    private List<ClusterVO> clusters;
    private List<ServerPodVO> serverPods;
}
