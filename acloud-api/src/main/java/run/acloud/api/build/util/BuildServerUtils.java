/**
 * Cocktail APM, Acornsoft Inc.
 */
package run.acloud.api.build.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.google.common.collect.Maps;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import io.kubernetes.client.openapi.JSON;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import run.acloud.api.build.constant.BuildServerConstants;
import run.acloud.api.build.vo.BuildServerAddVO;
import run.acloud.api.build.vo.BuildServerVO;
import run.acloud.commons.util.ObjectMapperUtils;

import java.util.List;
import java.util.Map;

public final class BuildServerUtils {

	public static final String LABELS_VALUE_PIPELINE_SERVER = "pipeline-server";
	public static final String SECRET_DATA_KEY_VALUES = "values";
	public static final String SECRET_NAME_SUFFIX = "-config";

	public static String buildServerVOToDeployConfigYAML(BuildServerAddVO vo, String clusterId, String buildQueueUser, String buildQueuePassword, Map<String, Object> natsTLS) throws Exception{

		/**
		 *
		 * extraHosts: []
		 * natsTLS:
		 *   enabled: true
		 *   caCert: ''
		 *   tlsCert: ''
		 *   tlsKey: ''
		 * secret:
		 *   TARGET: Agent
		 *   CLUSTER_ID: thingcluster
		 *   NATS_USERNAME: xxxxx
		 *   NATS_PASSWORD: xxxxx
		 *   APPS_EVENT_SERVER: ''
		 * resources:
		 *   dind:
		 *     requests:
		 *       cpu: 100m
		 *       memory: 300Mi
		 *     limits:
		 *       cpu: 200m
		 *       memory: 400Mi
		 * nodeSelector: {}
		 * tolerations: []
		 * affinity: {}
		 * persistence:
		 *   enabled: false
		 *   workspace:
		 *     existingClaim: ''
		 *     storageClass: ''
		 *     accessModes:
		 *     - ReadWriteMany
		 *     size: 8Gi
		 *     annotations: {}
		 * natstls:
		 *   enabled: true
		 * insecureRegistries: []
		 *
		 */
		JSON k8sJson = new JSON();
		Configuration conf = Configuration.defaultConfiguration().addOptions(Option.DEFAULT_PATH_LEAF_TO_NULL);
		DocumentContext documentContext = JsonPath.using(conf).parse(BuildServerConstants.BUILD_SERVER_DEPLOY_CONFIG);

		documentContext.set("secret.CLUSTER_ID", clusterId);
		documentContext.set("secret.NATS_USERNAME", buildQueueUser);
		documentContext.set("secret.NATS_PASSWORD", buildQueuePassword);
		documentContext.set("secret.APPS_EVENT_SERVER", vo.getAppsEventServer());

		if (MapUtils.isNotEmpty(natsTLS)) {
			documentContext.set("natsTLS.enabled", natsTLS.get("enabled"));
			documentContext.set("natsTLS.caCert", natsTLS.get("caCert"));
			documentContext.set("natsTLS.tlsCert", natsTLS.get("tlsCert"));
			documentContext.set("natsTLS.tlsKey", natsTLS.get("tlsKey"));
		}

		if(vo.getCpuRequest() != null){
			documentContext.set("resources.dind.requests.cpu", vo.getCpuRequest()+BuildServerConstants.BUILD_SERVER_RESOURCE_CPU);
		}
		if(vo.getCpuLimit() != null){
			documentContext.set("resources.dind.limits.cpu", vo.getCpuLimit()+BuildServerConstants.BUILD_SERVER_RESOURCE_CPU);
		}
		if(vo.getMemoryRequest() != null){
			documentContext.set("resources.dind.requests.memory", vo.getMemoryRequest()+BuildServerConstants.BUILD_SERVER_RESOURCE_MEMORY);
		}
		if(vo.getMemoryLimit() != null){
			documentContext.set("resources.dind.limits.memory", vo.getMemoryLimit()+BuildServerConstants.BUILD_SERVER_RESOURCE_MEMORY);
		}

		if (StringUtils.isNotBlank(vo.getNodeSelector())) {
			documentContext.set("nodeSelector", getYamlValue(vo.getNodeSelector(), new TypeReference<Map<String, String>>(){}, k8sJson));
		}
		if (StringUtils.isNotBlank(vo.getTolerations())) {
			documentContext.set("tolerations", getYamlValue(vo.getTolerations(), new TypeReference<List<Map<String, Object>>>(){}, k8sJson));
		}
		if (StringUtils.isNotBlank(vo.getAffinity())) {
			documentContext.set("affinity", getYamlValue(vo.getAffinity(), new TypeReference<Map<String, Object>>(){}, k8sJson));

		}
		if (BooleanUtils.toBoolean(vo.getPersistenceEnabled())) {
			documentContext.set("persistence.enabled", true);
			if (StringUtils.isNotBlank(vo.getPvcName())) {
				documentContext.set("persistence.workspace.existingClaim", vo.getPvcName());
			} else {
				if (StringUtils.isNotBlank(vo.getStorageClass())) {
					documentContext.set("persistence.workspace.storageClass", vo.getStorageClass());
				}
				if (StringUtils.isNotBlank(vo.getPvcSize())) {
					documentContext.set("persistence.workspace.size", vo.getPvcSize());
				}
			}
		} else {
			documentContext.set("persistence.enabled", false);
		}
		if (CollectionUtils.isNotEmpty(vo.getInsecureRegistries())) {
			documentContext.set("insecureRegistries", vo.getInsecureRegistries());
		}

		Map<String, Object> valueMap = ObjectMapperUtils.getMapper().readValue(documentContext.jsonString(), new TypeReference<Map<String, Object>>(){});
		DumperOptions options = new DumperOptions();
		options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
		Yaml yaml = new Yaml(options);

		return yaml.dumpAsMap(valueMap);
	}

	public static BuildServerVO deployConfigYamlToBuildServerVO(BuildServerVO buildServerVO)throws Exception{
		if (buildServerVO != null) {
			DumperOptions options = new DumperOptions();
			options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
			Yaml yaml = new Yaml(options);
			Object o = yaml.loadAs(buildServerVO.getDeployConfig(), Map.class);
			Configuration conf = Configuration.defaultConfiguration().addOptions(Option.DEFAULT_PATH_LEAF_TO_NULL);
			DocumentContext documentContext = JsonPath.using(conf).parse(o);

			buildServerVO.setCpuRequest(getResource(documentContext.read("resources.dind.requests.cpu"), BuildServerConstants.BUILD_SERVER_RESOURCE_CPU));
			buildServerVO.setCpuLimit(getResource(documentContext.read("resources.dind.limits.cpu"), BuildServerConstants.BUILD_SERVER_RESOURCE_CPU));
			buildServerVO.setMemoryRequest(getResource(documentContext.read("resources.dind.requests.memory"), BuildServerConstants.BUILD_SERVER_RESOURCE_MEMORY));
			buildServerVO.setMemoryLimit(getResource(documentContext.read("resources.dind.limits.memory"), BuildServerConstants.BUILD_SERVER_RESOURCE_MEMORY));
			// affinity
			Map<String, Object> affinityMap = Maps.newHashMap();
			affinityMap.put("affinity", documentContext.read("affinity"));
			buildServerVO.setAffinity(yaml.dumpAsMap(affinityMap));
			// tolerations
			Map<String, Object> tolerationsMap = Maps.newHashMap();
			tolerationsMap.put("tolerations", documentContext.read("tolerations"));
			buildServerVO.setTolerations(yaml.dumpAsMap(tolerationsMap));
			// nodeSelector
			Map<String, Object> nodeSelectorMap = Maps.newHashMap();
			nodeSelectorMap.put("nodeSelector", documentContext.read("nodeSelector"));
			buildServerVO.setNodeSelector(yaml.dumpAsMap(nodeSelectorMap));
			if (documentContext.read("persistence.enabled", Boolean.class) != null) {
				if (BooleanUtils.toBoolean(documentContext.read("persistence.enabled", Boolean.class))) {
					buildServerVO.setPersistenceEnabled(true);
					if (StringUtils.isNotBlank(documentContext.read("persistence.workspace.existingClaim"))) {
						buildServerVO.setPvcName(documentContext.read("persistence.workspace.existingClaim"));
					} else {
						buildServerVO.setStorageClass(documentContext.read("persistence.workspace.storageClass"));
						buildServerVO.setPvcSize(documentContext.read("persistence.workspace.size"));
					}
				} else {
					buildServerVO.setPersistenceEnabled(false);
				}
			} else {
				buildServerVO.setPersistenceEnabled(false);
			}

			if (StringUtils.isNotBlank(documentContext.read("secret.APPS_EVENT_SERVER", String.class))) {
				buildServerVO.setAppsEventServer(documentContext.read("secret.APPS_EVENT_SERVER", String.class));
			}
			if (CollectionUtils.isNotEmpty(documentContext.read("insecureRegistries", List.class))) {
				buildServerVO.setInsecureRegistries(documentContext.read("insecureRegistries", List.class));
			}
		}
		return buildServerVO;
	}

	public static Integer getResource(String resource, String separator) {
		if(StringUtils.isBlank(resource)){
			return null;
		}
		return NumberUtils.toInt(StringUtils.substringBeforeLast(resource, separator));
	}

	public static BuildServerVO copyBuildServerVO(BuildServerAddVO add) {
		BuildServerVO vo = new BuildServerVO();
		vo.setBuildServerName(add.getBuildServerName());
		vo.setBuildServerDesc(add.getBuildServerDesc());
		vo.setBuildServerSeq(add.getBuildServerSeq());
		vo.setAccountSeq(add.getAccountSeq());
		vo.setClusterSeq(add.getClusterSeq());
		vo.setNamespace(add.getNamespace());
		return vo;
	}

	public static <T> T getYamlValue(String content, TypeReference<T> valueTypeRef, JSON k8sJson) throws Exception {
		if (StringUtils.isNotBlank(content)) {
			if (k8sJson == null) {
				k8sJson = new JSON();
			}

			// spec 체크
			Object k8sObj = run.acloud.api.k8sextended.util.Yaml.getSnakeYaml().load(content);
			String valueStr = k8sJson.serialize(k8sObj);
			return ObjectMapperUtils.getMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).setSerializationInclusion(JsonInclude.Include.NON_NULL).readValue(valueStr, valueTypeRef);
		} else {
			return null;
		}
	}

	public static String makeSecretName(String buildServerName) throws Exception {
		return String.format("%s%s", buildServerName, SECRET_NAME_SUFFIX);
	}
}
