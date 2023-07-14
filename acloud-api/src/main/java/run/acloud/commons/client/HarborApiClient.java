package run.acloud.commons.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import run.acloud.commons.client.harbor.v1.HarborApiClientV1;
import run.acloud.commons.client.harbor.v2.HarborApiClientV2;
import run.acloud.commons.enums.ApiVersionType;
import run.acloud.commons.service.RegistryPropertyService;
import run.acloud.commons.util.CryptoUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class HarborApiClient implements Serializable {

	private static final long serialVersionUID = 3807571931300880356L;

	@Autowired
	private RegistryPropertyService registryProperties;

	@Cacheable(value="HarborClient")
	public Object create(String encryptUsername, String encryptPassword) {
		log.debug("[C-MISS] Harbor-Client V1. within cacheable.");

		HarborApiClientV1 client = new HarborApiClientV1();

		client.setBasePath(String.format("%s/api", registryProperties.getUrl()));
		client.setUsername(CryptoUtils.decryptAES(encryptUsername));
		client.setPassword(CryptoUtils.decryptAES(encryptPassword));
		client.getHttpClient().newBuilder().readTimeout(600, TimeUnit.SECONDS);
		client.setVerifyingSsl(false);
		if (log.isDebugEnabled()) {
			client.setDebugging(true);
		}

		log.debug("Harbor-Client V1 created. within cacheable.");

		return client;
	}

	@Cacheable(value="HarborApiClient")
	public Object create(ApiVersionType apiVer, String registryUrl, String decryptedUsername, String decryptedPassword, String privateCertificateUseYn, String privateCertificate) {

		if (apiVer == ApiVersionType.V1) {
			log.debug("[C-MISS] Harbor-Client V1. within cacheable.");

			HarborApiClientV1 client = new HarborApiClientV1();

			client.setBasePath(String.format("%s/api", registryUrl));
			client.setUsername(decryptedUsername);
			client.setPassword(decryptedPassword);
			client.getHttpClient().newBuilder().readTimeout(600, TimeUnit.SECONDS);
			client.setVerifyingSsl(false);
			if (log.isDebugEnabled()) {
				client.setDebugging(true);
			}
			// private CA 사용시 CA값 설정
			if ("Y".equals(privateCertificateUseYn) && privateCertificate != null){
				InputStream caInputStream = new ByteArrayInputStream(privateCertificate.getBytes(StandardCharsets.UTF_8));
				client.setSslCaCert(caInputStream);
			}

			log.debug("Harbor-Client V1 created. within cacheable.");

			return client;
		} else {
			log.debug("[C-MISS] Harbor-Client V2. within cacheable.");

			HarborApiClientV2 client = new HarborApiClientV2();

			client.setBasePath(String.format("%s/api/v2.0", registryUrl));
			client.setUsername(decryptedUsername);
			client.setPassword(decryptedPassword);
			client.getHttpClient().newBuilder().readTimeout(600, TimeUnit.SECONDS);
			client.setVerifyingSsl(false);
			if (log.isDebugEnabled()) {
				client.setDebugging(true);
			}
			// private CA 사용시 CA값 설정
			if ("Y".equals(privateCertificateUseYn) && privateCertificate != null){
				InputStream caInputStream = new ByteArrayInputStream(privateCertificate.getBytes(StandardCharsets.UTF_8));
				client.setSslCaCert(caInputStream);
			}

			log.debug("Harbor-Client V2 created. within cacheable.");

			return client;
		}
	}

}
