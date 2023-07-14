package run.acloud.api.configuration.util;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import run.acloud.api.configuration.vo.ClusterAddVO;
import run.acloud.api.configuration.vo.ClusterVO;
import run.acloud.api.configuration.vo.OAuthTokenVO;
import run.acloud.api.resource.enums.CubeType;
import run.acloud.api.resource.util.K8sMapperUtils;
import run.acloud.commons.util.JsonUtils;

import java.io.IOException;
import java.util.EnumSet;
import java.util.Map;

@Slf4j
@Component
public final class ClusterUtils {

	public static void setNullClusterInfo(ClusterVO cluster){
		if(cluster != null){
			cluster.setApiSecret(null);
			cluster.setServerAuthData(null);
			cluster.setClientAuthData(null);
			cluster.setClientKeyData(null);
		}
	}

	public static void setNullClusterInfo(ClusterAddVO clusterAdd){
		if(clusterAdd != null){
			clusterAdd.setApiSecret(null);
			clusterAdd.setServerAuthData(null);
			clusterAdd.setClientAuthData(null);
			clusterAdd.setClientKeyData(null);
		}
	}

	public static String getAccessToken(CubeType cubeType, String authJson) throws IOException {
		String accessToken = "";
		if(EnumSet.of(CubeType.EKS, CubeType.NCPKS).contains(cubeType)){
			Map<String, Object> authMap = K8sMapperUtils.getMapper().readValue(authJson, new TypeReference<Map<String, Object>>(){});
			accessToken = (String)((Map<String, Object>)authMap.get("status")).get("token");
		}else if(EnumSet.of(CubeType.GKE, CubeType.AKS).contains(cubeType)){
			OAuthTokenVO authToken = JsonUtils.fromGson(authJson, OAuthTokenVO.class);
			accessToken = authToken.getAccessToken();
		}
		return accessToken;
	}
}
