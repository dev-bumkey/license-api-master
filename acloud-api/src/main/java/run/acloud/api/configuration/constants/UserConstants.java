package run.acloud.api.configuration.constants;

import jakarta.annotation.PostConstruct;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import run.acloud.api.auth.enums.LanguageType;
import run.acloud.framework.properties.CocktailServiceProperties;

import java.util.Arrays;
import java.util.Optional;

@Component
public class UserConstants {
	private static CocktailServiceProperties cocktailServiceProperties;

	@Autowired
	private CocktailServiceProperties injectedCocktailServiceProperties;

	@PostConstruct
	public void init() {
		UserConstants.cocktailServiceProperties = injectedCocktailServiceProperties;
	}

	private UserConstants() {
	}

	public static final String INIT_ADMIN_PASSWORD = "admin0000";
	public static final String INIT_USER_PASSWORD = "Pass0000";
	public static final int INTERVAL_CHANGE_PASSWORD = 90;
	public static final int INTERVAL_EXTEND_PASSWORD = 30;
	public static final int INTERVAL_INACTIVE = 90;

	public static LanguageType defaultLanguage(){
		if(StringUtils.isNotBlank(cocktailServiceProperties.getDefaultLanguage())){
			String defaultLanguage = StringUtils.lowerCase(cocktailServiceProperties.getDefaultLanguage());

			Optional<LanguageType> languageTypeOptional = Arrays.asList(LanguageType.values()).stream().filter(lt -> (StringUtils.equals(defaultLanguage, lt.getCode()))).findFirst();
			if(languageTypeOptional.isPresent()){
				return LanguageType.valueOf(defaultLanguage);
			}
		}

		return LanguageType.en;
	}
}
