package run.acloud.framework.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = CocktailLogAgentProperties.PREFIX)
public class CocktailLogAgentProperties {
    public static final String PREFIX = "cocktail.log-agent";

    private String logAgentChartRepo;    // cocktail-app
    private String logAgentChartName;    // fluent-bit
    private String logAgentChartVersion; // 0.31.0
    private String logAgentLogPushUrl; // cocktail-logs-gateway.cocktail-logs
    private String logAgentLogPushPort; // 8980
}
