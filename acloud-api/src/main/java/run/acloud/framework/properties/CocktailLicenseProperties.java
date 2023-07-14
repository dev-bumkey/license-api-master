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
@ConfigurationProperties(prefix = CocktailLicenseProperties.PREFIX)
public class CocktailLicenseProperties {
    public static final String PREFIX = "cocktail.license";

    private boolean licenseEnable;

    private int initExpirePeriodDays;

    private String licenseKey;
}
