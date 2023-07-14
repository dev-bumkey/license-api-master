package run.acloud.commons.provider;

import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.storage.StorageOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Component
public class GcpClient {
	public Object create(String projectId, String credentialJson, String gcpServiceCode) {
		
		Object service = null;

		InputStream credentialsStream = null;
		try {
			credentialsStream = new ByteArrayInputStream(credentialJson.getBytes());
			ServiceAccountCredentials serviceAccountCredentials = ServiceAccountCredentials.fromStream(credentialsStream);
			
			if ("GCS".equals(gcpServiceCode)) {
				service = StorageOptions.newBuilder()
						.setProjectId(projectId)
						.setCredentials(serviceAccountCredentials)
						.build()
						.getService();
			}
			
		} catch (IOException e) {
			log.error("Error!", e);
		} finally {
			try {
				if (credentialsStream != null) {
					credentialsStream.close();
				}
			} catch (IOException ignore) {
				// do nothing
			}
		}
		
		return service;
	}
}
