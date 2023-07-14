package run.acloud.framework.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Created by gun@acornsoft.io on 2019. 1. 30.
 */
@Data
@Component
@ConfigurationProperties(prefix = CocktailEfkProperties.PREFIX)
public class CocktailEfkProperties {
    public static final String PREFIX = "cocktail.efk";

    private String kibanaUrl;
}