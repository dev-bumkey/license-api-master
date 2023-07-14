package run.acloud.framework.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Created by gun@acornsoft.io on 2019. 1. 30.
 */
@Data
@Component
@ConfigurationProperties(prefix = CocktailEmailProperties.PREFIX)
public class CocktailEmailProperties {
    public static final String PREFIX = "cocktail.email";

    private String mailSmtpStarttlsEnable;
    private String mailSmtpHost;
    private String mailSmtpAuth;
    private String mailSmtpPort;
    private int mailSmtpConnectTimeout;
    private String mailSmtpId;
    private String mailSmtpLocalhost;
    private String mailTransportProtocol;
    private String mailDebug;
    private String mailSmtpPw;
    private String mailSmtpFromName;
}