package run.acloud.framework.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Created by gun@acornsoft.io on 2019. 1. 30.
 */
@Data
@Component
@ConfigurationProperties(prefix = CocktailChartProperties.PREFIX)
public class CocktailChartProperties {
    public static final String PREFIX = "cocktail.chart";

    private String chartApiUrl;
    private int chartGrpcPort;
    private int chartHttpPort;

}