package run.acloud.framework.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Created by choewonseog on 2017. 1. 25..
 */
@Data
@Component
@ConfigurationProperties(prefix = CocktailCryptoProperties.PREFIX)
public class CocktailCryptoProperties {
    public static final String PREFIX = "cocktail.crypto";

    private String aesKey;
    private String defaultAesKey;

    private String rsaPublicKey;
    private String rsaPrivateKey;

}
