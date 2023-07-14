package run.acloud.framework.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Created by choewonseog on 2017. 1. 25..
 */
@Data
@Component
@ConfigurationProperties(prefix = CocktailRegistryProperties.PREFIX)
public class CocktailRegistryProperties {
    public static final String PREFIX = "cocktail.registry";

    private String url;
    private String id;
    private String password;
    private String insecureYn;
    private String privateCertificateUseYn;
    private String privateCertificate;
}
