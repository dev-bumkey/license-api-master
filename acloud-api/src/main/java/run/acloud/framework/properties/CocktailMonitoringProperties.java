package run.acloud.framework.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Created by choewonseog on 2017. 1. 25..
 */
@Data
@Component
@ConfigurationProperties(prefix = CocktailMonitoringProperties.PREFIX)
public class CocktailMonitoringProperties {
    public static final String PREFIX = "cocktail.monitoring";

    private String monitoringHost;
}
