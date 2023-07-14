package run.acloud.framework.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author wschoi@acornsoft.io
 * Created on 2017. 9. 25.
 */
@Data
@Component
@ConfigurationProperties(prefix = CocktailUIProperties.PREFIX)
public class CocktailUIProperties {
    public static final String PREFIX = "cocktail.event";

    private String eventType;

    private String clientHost;

    private String callbackUrl;

    private String callbackBuilderUrl;
}
