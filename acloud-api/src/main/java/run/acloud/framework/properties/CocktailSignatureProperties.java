package run.acloud.framework.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Created by gun@acornsoft.io on 2019. 1. 29..
 */
@Data
@Component
@ConfigurationProperties(prefix = CocktailSignatureProperties.PREFIX)
public class CocktailSignatureProperties {
    public static final String PREFIX = "cocktail.signature";

    private String sigSecret;
    private String sigHmacAlg;

}