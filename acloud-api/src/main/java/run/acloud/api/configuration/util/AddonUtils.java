package run.acloud.api.configuration.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Maps;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;
import run.acloud.api.configuration.enums.AddonKeyItem;
import run.acloud.api.configuration.vo.AddonConfigMapVO;
import run.acloud.api.resource.constants.KubeConstants;
import run.acloud.api.resource.enums.JsonPatchOp;
import run.acloud.commons.util.JsonUtils;
import run.acloud.commons.util.ObjectMapperUtils;

import java.text.SimpleDateFormat;
import java.util.*;

@Slf4j
@Component
public final class AddonUtils {

	public static String genAddonConfigMapYaml(AddonConfigMapVO addonConfigMap) throws Exception{
		String valueStr = JsonUtils.toGson(addonConfigMap);
		Map<String, Object> valueMap = ObjectMapperUtils.getMapper().readValue(valueStr, new TypeReference<Map<String, Object>>(){});

		DumperOptions options = new DumperOptions();
		options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
		Yaml yaml = new Yaml(options);

		return yaml.dumpAsMap(valueMap);
	}

	public static String genAddonConfigMapYaml(Map<String, Object> valueMap) throws Exception{
		DumperOptions options = new DumperOptions();
		options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);

		return new Yaml(options).dumpAsMap(valueMap);
	}

	public static AddonConfigMapVO getAddonConfigMapYaml(String addonConfigMap) throws Exception{

		DumperOptions options = new DumperOptions();
		options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
		Yaml yaml = new Yaml(options);

		return yaml.loadAs(addonConfigMap, AddonConfigMapVO.class);
	}


	public static String getAddonPrometheusUrl(String addonSuffix) throws Exception{
		String url = "http://prometheus";

		return String.format("%s-%s", url, addonSuffix);
	}

	public static String getAddonAlertmanagerUrl(String addonSuffix) throws Exception{
		String url = "http://alertmanager";

		return String.format("%s-%s", url, addonSuffix);
	}

    public static String getAutoAddonPrometheusUrl(String addonPrefix) throws Exception{
        return String.format("http://%s-prometheus-oper-prometheus:9090", addonPrefix);
    }

    public static String getAutoAddonAlertmanagerUrl(String addonPrefix) throws Exception{
        return String.format("http://%s-prometheus-oper-alertmanager:9093", addonPrefix);
    }

    public static String getAutoAddonMonitoringServiceName(String addonPrefix) throws Exception {
		return String.format("%s-prometheus-oper-kubelet", addonPrefix);
    }

    public static Map<String, String> newAddonDataMap(String valueYaml, String namespace) throws Exception {
		Map<String, String> dataMap = Maps.newHashMap();
		dataMap.put(AddonKeyItem.VALUE_YAML.getValue(), valueYaml);
		dataMap.put(AddonKeyItem.VERSION.getValue(), "");
//		dataMap.put(AddonKeyItem.AUTO_UPDATE.getValue(), "Y"); // addon auto update를 무조건 Y로 설정하지 않도록 함... 2020.12.03
		dataMap.put(AddonKeyItem.USE_YN.getValue(), "Y");
		dataMap.put(AddonKeyItem.RELEASE_NAMESPACE.getValue(), namespace);

		return dataMap;
	}

	public static boolean isValidYaml(String yamlStr) throws Exception{

		try {
			DumperOptions options = new DumperOptions();
			options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
			Yaml yaml = new Yaml(options);

			yaml.load(yamlStr);

			return true;
		}
		catch (YAMLException ye) {
			log.warn("is not valid yaml!!", ye);
			return false;
		}
		catch (Exception e) {
			log.warn("is not valid yaml!!", e);
			return false;
		}

	}

	public static String addonLabel() {
		return String.format("%s,%s=agent,%s=addon", KubeConstants.LABELS_HELM_CHART_KEY, KubeConstants.LABELS_ADDON_AGENT_KEY, KubeConstants.LABELS_ADDON_CHART_KEY);
	}

	public static void setAddonUpdateAt(List<JsonObject> patchBody) throws Exception {
//		Map<String, Object> patchMap = new HashMap<>();
//		patchMap.put("op", JsonPatchOp.REPLACE.getValue());
//		patchMap.put("path", String.format("/metadata/labels/%s", KubeConstants.LABELS_ADDON_UPDATE_AT));
//		patchMap.put("value", String.valueOf(LocalDateTime.now(ZoneId.of("GMT")).toInstant(ZoneOffset.UTC).toEpochMilli()*0.001));
//		patchBody.add((JsonUtils.fromGson(JsonUtils.toGson(patchMap), JsonElement.class)).getAsJsonObject());
		Date current = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		String dateStr = sdf.format(current);

		Map<String, Object> patchMap = new HashMap<>();
		patchMap.put("op", JsonPatchOp.REPLACE.getValue());
		patchMap.put("path", String.format("/data/%s", KubeConstants.LABELS_ADDON_UPDATE_AT));
		patchMap.put("value", dateStr);
		patchBody.add((JsonUtils.fromGson(JsonUtils.toGson(patchMap), JsonElement.class)).getAsJsonObject());

	}

	public static void setAddonStatus(List<JsonObject> patchBody, String status) throws Exception {
		Map<String, Object> patchMap = new HashMap<>();
		patchMap.put("op", JsonPatchOp.REPLACE.getValue());
		patchMap.put("path", String.format("/metadata/labels/%s", KubeConstants.LABELS_ADDON_STATUS));
		patchMap.put("value", status);
		patchBody.add((JsonUtils.fromGson(JsonUtils.toGson(patchMap), JsonElement.class)).getAsJsonObject());
	}

	public static void setAddonAppVersion(List<JsonObject> patchBody, String appVersion) throws Exception {
		Map<String, Object> patchMap = new HashMap<>();
		patchMap.put("op", JsonPatchOp.REPLACE.getValue());
		patchMap.put("path", String.format("/data/%s", AddonKeyItem.APP_VERSION.getValue()));
		patchMap.put("value", appVersion);
		patchBody.add((JsonUtils.fromGson(JsonUtils.toGson(patchMap), JsonElement.class)).getAsJsonObject());
	}

}
