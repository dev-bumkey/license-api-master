package run.acloud.commons.provider;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClientBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AWSClient {

	public AmazonIdentityManagement createIAM(String regionCode, String accessKey, String secretKey) {
		log.debug("[C-MISS] AWS-Client IAM.");

		AmazonIdentityManagement client = this.createIAMBody(regionCode, accessKey, secretKey);

		log.debug("AWS-Client IAM created.");
		return client;
	}

	public AmazonIdentityManagement createIAMBody(String regionCode, String accessKey, String secretKey) {
		AmazonIdentityManagement client = null;

		AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);

		client = AmazonIdentityManagementClientBuilder.standard()
				.withCredentials(new AWSStaticCredentialsProvider(credentials))
				.withRegion(regionCode)
				.build();

		return client;
	}

	/**
	 * 현재 동작 안됨... Region 없이 연결할수 있는 방법 확인 후 없다면 제거 예정...
	 * @param accessKey
	 * @param secretKey
	 * @return
	 */
	public AmazonIdentityManagement createIAM(String accessKey, String secretKey) {
		log.debug("[C-MISS] AWS-Client IAM.");

		AmazonIdentityManagement client = this.createIAMBody(accessKey, secretKey);

		log.debug("AWS-Client IAM created.");
		return client;
	}

	/**
	 * 현재 동작 안됨... Region 없이 연결할수 있는 방법 확인 후 없다면 제거 예정...
	 * @param accessKey
	 * @param secretKey
	 * @return
	 */
	public AmazonIdentityManagement createIAMBody(String accessKey, String secretKey) {
		AmazonIdentityManagement client = null;

		AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);

		client = AmazonIdentityManagementClientBuilder.standard()
			.withCredentials(new AWSStaticCredentialsProvider(credentials))
			.withRegion(Regions.AP_NORTHEAST_2)
			.build();

		return client;
	}


}
