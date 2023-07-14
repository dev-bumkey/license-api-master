package run.acloud.framework.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Created by gun@acornsoft.io on 2019. 1. 30.
 */
@Data
@Component
@ConfigurationProperties(prefix = CocktailAddonProperties.PREFIX)
public class CocktailAddonProperties {
    public static final String PREFIX = "cocktail.addon";

    private String acloudRegistryUrl;
    private String monitoringCollectorUrlProxy;
    private String monitoringApiUrlProxy;
    private String controllerShakeUrlProxy;
    private String controllerShakeGrpcPortProxy;
    private String addonNamespace;
    private String monitoringAgentConfigmapPrefix; // Rename R4.2.1 : private String monitoringConfigmapPrefix;
    private String controllerConfigmapPrefix;

    /** R4.2.1 : for Addon-Manager **/
    private String addonChartRepoUrl;
    private String addonChartRepoProjectName;
    private String addonChartRepoUser;
    private String addonChartRepoPassword;
    private String addonManagerConfigmapPrefix;
    private String addonManagerChartName;
    private String addonManagerChartVersion;
    private String addonImageBaseUrl;

    /** R4.5.35 : for Istio Addon 1.9.1 **/
    private String kialiUrl;

//    private String monitoringConfigmapPrefix;
//    private String addonMonitoringNamespace;
//    private String monitoringChartName;
//    private String monitoringAgentChartName;
//
//    private String monitoringCmValues;
//    private String monitoringCmDefaultDataSet;
//    private String monitoringCmEksDataSet;
//    private String monitoringCmGkeDataSet;
//    private String monitoringCmAksDataSet;
//    private String monitoringCmKaasOnlyDataSet;
}