package run.acloud.api.build.constant;

public class BuildServerConstants {

	public static final String BUILD_SERVER_RESOURCE_CPU = "m";
	public static final String BUILD_SERVER_RESOURCE_MEMORY = "Mi";

	public static final String BUILD_SERVER_DEPLOY_CONFIG = "{\"extraHosts\":[]," +
			"\"natsTLS\":{\"enabled\":true,\"caCert\":\"\",\"tlsCert\":\"\",\"tlsKey\":\"\"}," +
			"\"secret\":{\"TARGET\":\"Agent\",\"CLUSTER_ID\":\"\",\"NATS_USERNAME\":\"\",\"NATS_PASSWORD\":\"\",\"APPS_EVENT_SERVER\":\"\"}," +
			"\"resources\":{\"dind\":{\"requests\":{\"cpu\":\"100m\",\"memory\":\"256Mi\"},\"limits\":{\"cpu\":\"1000m\",\"memory\":\"2048Mi\"}}}," +
			"\"nodeSelector\":{}," +
			"\"tolerations\":[]," +
			"\"affinity\":{}," +
			"\"persistence\":{\"enabled\":false,\"workspace\":{\"existingClaim\":\"\",\"storageClass\":\"\",\"accessModes\":[\"ReadWriteMany\"],\"size\":\"8Gi\",\"annotations\":{}}}," +
			"\"insecureRegistries\": []" +
			"}";

}