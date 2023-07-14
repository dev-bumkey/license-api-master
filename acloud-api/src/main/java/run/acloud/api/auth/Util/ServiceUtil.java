package run.acloud.api.auth.Util;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import run.acloud.api.auth.enums.ServiceMode;
import run.acloud.framework.properties.CocktailServiceProperties;

import java.util.Optional;

@Slf4j
@Component
public final class ServiceUtil {

	private static CocktailServiceProperties cocktailServiceProperties;

	@Autowired
	private CocktailServiceProperties injectedCocktailServiceProperties;

	@PostConstruct
	public void init() {
		ServiceUtil.cocktailServiceProperties = injectedCocktailServiceProperties;
	}

	public static ServiceMode getServiceMode() {
		if (cocktailServiceProperties != null) {
			return ServiceMode.valueOf(Optional.ofNullable(cocktailServiceProperties.getMode()).orElseGet(() ->"PRD"));
		} else {
			return ServiceMode.PRD;
		}
	}

}