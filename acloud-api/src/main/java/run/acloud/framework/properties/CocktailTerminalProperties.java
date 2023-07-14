package run.acloud.framework.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = CocktailTerminalProperties.PREFIX)
public class CocktailTerminalProperties {
    public static final String PREFIX = "cocktail.terminal";
    private int terminalMaxConnection;
    private int terminalConnectionTimeout;
}
