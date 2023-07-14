package run.acloud.framework.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author wschoi@acornsoft.io
 * Created on 2017. 9. 21.
 */
@Data
@Component
@ConfigurationProperties(prefix = CocktailOnlineDSProperties.PREFIX)
public class CocktailOnlineDSProperties {
    public static final String PREFIX = "cocktail.online.ds";

    private String systemEmail;  // 시스템 담당자 대표 메일주소
    private String salesEmail;  // 영업 담당자 대표 메일주소

    private String mailFormPath; // 메일 양식 html 파일경로
    private String platformAdminUrl; // 플랫폼 관리자 URL

    private String platformUserUrl; // 플랫폼 사용자 URL

}
