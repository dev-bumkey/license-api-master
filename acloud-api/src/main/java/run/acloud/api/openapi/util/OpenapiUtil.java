package run.acloud.api.openapi.util;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.jose4j.jwk.JsonWebKey;
import org.jose4j.jwk.RsaJsonWebKey;
import org.jose4j.jwk.RsaJwkGenerator;
import org.jose4j.jwk.Use;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import run.acloud.commons.security.password.MessageDigestPasswordEncoder;
import run.acloud.commons.security.password.ShaPasswordEncoder;
import run.acloud.commons.util.CryptoUtils;
import run.acloud.framework.properties.CocktailServiceProperties;

@Slf4j
@Component
public final class OpenapiUtil {

	private static CocktailServiceProperties cocktailServiceProperties;

	@Autowired
	private CocktailServiceProperties injectedCocktailServiceProperties;

	@PostConstruct
	public void init() {
		OpenapiUtil.cocktailServiceProperties = injectedCocktailServiceProperties;
	}

	public static String generateJwk() throws Exception {
		MessageDigestPasswordEncoder passwordEncoder = new ShaPasswordEncoder(256);
		RsaJsonWebKey rsaJsonWebKey = RsaJwkGenerator.generateJwk(2048);
		rsaJsonWebKey.setKeyId(passwordEncoder.encodePassword("cocktail-api", CryptoUtils.generateSalt()));
		rsaJsonWebKey.setAlgorithm(AlgorithmIdentifiers.RSA_USING_SHA256);
		rsaJsonWebKey.setUse(Use.SIGNATURE);
//		log.debug("jwk : {}", rsaJsonWebKey.toJson());
//		log.debug("jwk pri : {}", Base64.encodeBase64String(rsaJsonWebKey.getRsaPrivateKey().getEncoded()));
//		log.debug("jwk pub : {}", Base64.encodeBase64String(rsaJsonWebKey.getRsaPublicKey().getEncoded()));

		return rsaJsonWebKey.toJson(JsonWebKey.OutputControlLevel.INCLUDE_PRIVATE);
	}

}