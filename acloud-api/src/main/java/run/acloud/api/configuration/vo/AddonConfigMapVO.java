package run.acloud.api.configuration.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import run.acloud.commons.vo.BaseVO;

@Getter
@Setter
@ToString
@EqualsAndHashCode
@Schema(description = "Addon configMap 모델")
public class AddonConfigMapVO extends BaseVO {

    /**
     * common
     */
    private String cluster_type;
    private String base64_cluster_seq;

    /**
     * controller
     */
    private String cluster_id;
    private String shake_url;
    private String shake_grpc_port;
    private String base64_controller_secret;

    /**
     * monitoring
     */
    private String collector_server_url;
    private String monitor_api_url;
    private String base64_monitoring_secret;
    private String base64_cluster_id;
    private String prometheus_url;
    private String alertmanager_url;
    private AddonConfigMapImageVO image;

    /**
     * addon-manager
     */
    private String imagePullPolicy;
    private AddonConfigMapChartRepoVO env;
    private String addonManagerTag;
}