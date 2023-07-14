package run.acloud.commons.provider;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.util.ClientBuilder;
import io.kubernetes.client.util.credentials.AccessTokenAuthentication;
import io.kubernetes.client.util.credentials.ClientCertificateAuthentication;
import io.kubernetes.client.util.credentials.UsernamePasswordAuthentication;
import lombok.extern.slf4j.Slf4j;
import okio.ByteString;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import run.acloud.api.configuration.util.ClusterUtils;
import run.acloud.api.resource.enums.AuthType;
import run.acloud.api.resource.enums.CubeType;
import run.acloud.commons.util.CryptoUtils;
import run.acloud.framework.properties.CocktailServiceProperties;

import java.io.IOException;
import java.io.Serializable;
import java.util.Optional;

@Slf4j
@Component
public class K8sClient implements Serializable {

	private static final long serialVersionUID = 6580265318597086211L;

	@Autowired
	private CocktailServiceProperties cocktailServiceProperties;

	@Cacheable(value="K8sClient")
	public Object create(AuthType authType, CubeType cubeType, String accessUrl, String accountId, String accountPassword,
                         String clientAuthData, String clientKeyData, String serverAuthData) {
		log.debug("[C-MISS] Kubernetes-Official-Client. within cacheable.");

		ApiClient client = (ApiClient)this.createBody(authType, cubeType, accessUrl, accountId, accountPassword, clientAuthData, clientKeyData, serverAuthData);

		log.debug("Kubernetes-Official-Client created. within cacheable.");
		return client;
	}

	public Object createNoCache(AuthType authType, CubeType cubeType, String accessUrl, String accountId, String accountPassword,
						 String clientAuthData, String clientKeyData, String serverAuthData) {
		log.debug("[C-MISS] Kubernetes-Official-Client. without cacheable.");

		ApiClient client = (ApiClient)this.createBody(authType, cubeType, accessUrl, accountId, accountPassword, clientAuthData, clientKeyData, serverAuthData);

		log.debug("Kubernetes-Official-Client created. without cacheable.");
		return client;
	}

	public Object createBody(AuthType authType, CubeType cubeType, String accessUrl, String accountId, String accountPassword,
						 String clientAuthData, String clientKeyData, String serverAuthData) {
		ApiClient client = new ApiClient();

		try {

			if (authType == AuthType.TOKEN) {

				accountPassword = CryptoUtils.decryptAES(accountPassword);
				String accessToken = ClusterUtils.getAccessToken(cubeType, accountPassword);

				client = new ClientBuilder()
						.setBasePath(StringUtils.removeEnd(accessUrl, "/"))
						.setVerifyingSsl(true)
						.setAuthentication(new AccessTokenAuthentication(accessToken))
						.setCertificateAuthority(this.getByte(Optional.ofNullable(CryptoUtils.decryptAES(serverAuthData)).orElseGet(() ->"")))
						.build();

			} else if (authType == AuthType.CERT) {

				client = new ClientBuilder()
						.setBasePath(StringUtils.removeEnd(accessUrl, "/"))
						.setVerifyingSsl(true)
						.setAuthentication(new ClientCertificateAuthentication(this.getByte(Optional.ofNullable(CryptoUtils.decryptAES(clientAuthData)).orElseGet(() ->"")), this.getByte(Optional.ofNullable(CryptoUtils.decryptAES(clientKeyData)).orElseGet(() ->""))))
						.setCertificateAuthority(this.getByte(Optional.ofNullable(CryptoUtils.decryptAES(serverAuthData)).orElseGet(() ->"")))
						.build();

			} else { // plain

				client = new ClientBuilder()
						.setBasePath(StringUtils.removeEnd(accessUrl, "/"))
						.setAuthentication(new UsernamePasswordAuthentication(accountId, CryptoUtils.decryptAES(accountPassword)))
						.build();
			}

			client.setConnectTimeout(cocktailServiceProperties.getK8sclientConnectTimeout());

			if(cocktailServiceProperties.isK8sclientDebugging()){
				client.setDebugging(true);
			}

		} catch (IOException e) {
			log.error("Kubernetes-Official-Client error.", e);
		}

		return client;
	}

	private byte[] getByte(String data){
		byte[] bytes = null;
		ByteString decoded = ByteString.decodeBase64(data);
		if (decoded != null) {
			bytes = decoded.toByteArray();
		} else {
			bytes = data.getBytes();
		}

		return bytes;
	}


}
