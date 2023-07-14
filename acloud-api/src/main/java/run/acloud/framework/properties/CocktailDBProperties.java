package run.acloud.framework.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = CocktailDBProperties.PREFIX)
public class CocktailDBProperties implements IDatabaseProperties {
    public static final String PREFIX = "datasource.cmdb";

    private String driverClassName;
    private String url;
    private String userName;
    private String password;

    private int initialSize;
    private int maxActive;
    private int maxIdle;
    private int minIdle;
    private int maxWait;
    private String resourcePrefix;
}
