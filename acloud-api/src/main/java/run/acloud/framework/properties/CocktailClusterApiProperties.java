package run.acloud.framework.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Created by choewonseog on 2017. 1. 25..
 */
@Data
@Component
@ConfigurationProperties(prefix = CocktailClusterApiProperties.PREFIX)
public class CocktailClusterApiProperties {
    public static final String PREFIX = "cocktail.cluster";

    private String clusterApiHost;
    private String collectorApiHost;
}
