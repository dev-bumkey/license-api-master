package run.acloud.api.cserver.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import com.jayway.jsonpath.PathNotFoundException;
import com.jayway.jsonpath.TypeRef;
import io.kubernetes.client.openapi.JSON;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.codehaus.jettison.json.JSONObject;
import org.json.JSONArray;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.scanner.ScannerException;
import run.acloud.api.k8sextended.util.Yaml;
import run.acloud.api.resource.constants.KubeConstants;
import run.acloud.api.resource.enums.K8sApiGroupType;
import run.acloud.api.resource.enums.K8sApiKindType;
import run.acloud.api.resource.enums.K8sApiType;
import run.acloud.commons.util.JsonPathUtils;
import run.acloud.commons.util.ObjectMapperUtils;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

@Slf4j
@Component
public final class ServerUtils {

	public static List<Object> getYamlObjects(String yamlStr) throws Exception {
		List<Object> objs = new ArrayList<>();

		if (StringUtils.isNotBlank(yamlStr)) {
			try {
				objs = Yaml.loadAll(yamlStr);
			}
			catch (ScannerException se) {
				throw new CocktailException("Yaml is invalid.", se, ExceptionType.InvalidYamlData, se.getMessage());
			}
			catch (IOException ie) {
				String errMsg = "";
				if (StringUtils.startsWith(ie.getMessage(), "Unknown ")) {
					if (StringUtils.indexOf(ie.getMessage(), ":") > 1) {
						errMsg = StringUtils.substringBefore(ie.getMessage(), ":");
					}
				}
				throw new CocktailException("Yaml is invalid.", ie, ExceptionType.InvalidYamlData, errMsg);
			}
			catch (Exception e) {
				String errMsg = "";
				Throwable rootCause = ExceptionUtils.getRootCause(e);
				if (rootCause != null) {
					errMsg = StringUtils.substringBefore(rootCause.getMessage(), "on class:");
				}
				if (StringUtils.isBlank(errMsg)) {
					for(Throwable throwableRow : ExceptionUtils.getThrowableList(e)){
						errMsg = throwableRow.getMessage();
						break;
					}
				}
				if(log.isDebugEnabled()) {
					log.error(ExceptionUtils.getStackTrace(e));
				}
				throw new CocktailException("Yaml is invalid.", e, ExceptionType.InvalidYamlData, errMsg);
			}
		} else {
			throw new CocktailException("Yaml is empty.", ExceptionType.InvalidYamlData);
		}

		return objs;
	}

	public static List<Object> getYamlObjectsWithSnakeYaml(String yamlStr) {
		List<Object> objs = new ArrayList<>();

		if (StringUtils.isNotBlank(yamlStr)) {
			try {
				Iterable<Object> iterable = Yaml.getSnakeYaml(null).loadAll(yamlStr);
				for (Object object : iterable) {
					if (object != null) {
						objs.add(object);
					}
				}
			}
			catch (ScannerException se) {
				throw new CocktailException("Yaml is invalid.", se, ExceptionType.InvalidYamlData, se.getMessage());
			}
			catch (Exception e) {
				String errMsg = "";
				Throwable rootCause = ExceptionUtils.getRootCause(e);
				if (rootCause != null) {
					errMsg = StringUtils.substringBefore(rootCause.getMessage(), "on class:");
				}
				if (StringUtils.isBlank(errMsg)) {
					for(Throwable throwableRow : ExceptionUtils.getThrowableList(e)){
						errMsg = throwableRow.getMessage();
						break;
					}
				}
				if(log.isDebugEnabled()) {
					log.error(ExceptionUtils.getStackTrace(e));
				}
				throw new CocktailException("Yaml is invalid.", e, ExceptionType.InvalidYamlData, errMsg);
			}
		} else {
			throw new CocktailException("Yaml is empty.", ExceptionType.InvalidYamlData);
		}

		return objs;
	}

	public static String marshalYaml(Object object) throws Exception{
		JsonNode objNode = ObjectMapperUtils.getPatchMapper().valueToTree(object);
		String objStr = ObjectMapperUtils.getPatchMapper().writeValueAsString(objNode);
		Map<String, Object> objMap = ObjectMapperUtils.getPatchMapper().readValue(objStr, new TypeReference<Map<String, Object>>(){});

		return Yaml.getSnakeYaml(null).dumpAsMap(objMap);
	}

	/**
	 * yamlStr에 여러 Object가 존재할때 원하는 Object 유형을 가져와야 하는 경우 사용..
	 * @param yamlStr
	 * @param k8sApiKindType
	 * @param <T>
	 * @return
	 * @throws Exception
	 */
	public static <T> T unmarshalYaml(String yamlStr, K8sApiKindType k8sApiKindType) throws Exception {
		return unmarshalYaml(yamlStr, k8sApiKindType, null);
	}

	/**
	 * yamlStr 에 1개의 Object만 존재할때 사용..
	 * (ClassCastException 발생 할 수 있음..)
	 *
	 * @param yamlStr
	 * @param <T>
	 * @return
	 * @throws Exception
	 */
	public static <T> T unmarshalYaml(String yamlStr) throws Exception {
		return unmarshalYaml(yamlStr, null, null);
	}

	/**
	 * yamlStr 에 1개의 Object만 존재할때 사용..
	 * (ClassCastException 발생 할 수 있음..)
	 *
	 * @param yamlStr
	 * @param k8sJson
	 * @param <T>
	 * @return
	 * @throws Exception
	 */
	public static <T> T unmarshalYaml(String yamlStr, JSON k8sJson) throws Exception {
		return unmarshalYaml(yamlStr, null, k8sJson);
	}

	/**
	 * YAML String에서 원하는 K8s Object Type을 찾아서 해당 Object Type으로 Return..
	 * - K8sApiKindType 파라미터가 null 이면 리스트의 첫번째 Object를 응답..
	 * - K8sApiKindType 파라미터가 있을 경우 해당 Type의 첫번째 Object를 응답..
	 *
	 * @param yamlStr
	 * @param k8sJson
	 * @param <T>
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public static <T> T unmarshalYaml(String yamlStr, K8sApiKindType k8sApiKindType, JSON k8sJson) throws Exception {

		if (k8sJson == null) {
			k8sJson = new JSON();
		}

		List<Object> objs = ServerUtils.getYamlObjects(yamlStr);

		T convertObj = null;
		if (CollectionUtils.isNotEmpty(objs)) {
			for (Object obj : objs) {
				K8sApiKindType kind = ServerUtils.getK8sKindInObject(obj, k8sJson);
				if(k8sApiKindType == null) {
					if (K8sApiKindType.findKindTypeByValue(kind.getValue()) != null) {
						convertObj = (T) obj;
						break;
					}
				}
				else {
					if(kind == k8sApiKindType) {
						convertObj = (T) obj;
						break;
					}
				}
				log.debug(io.kubernetes.client.util.Yaml.dump(obj));
			}
		}

		return convertObj;
	}

	/**
	 * k8s object to map
	 *
	 * @param k8sObj
	 * @param k8sJson
	 * @return
	 * @throws Exception
	 */
	public static Map<String, Object> getK8sObjectToMap(Object k8sObj, JSON k8sJson) throws Exception {

		if (k8sJson == null) {
			k8sJson = new JSON();
		}

		String valueStr = k8sJson.serialize(k8sObj);

		return getK8sJsonToMap(valueStr);
	}

	/**
	 * k8s Json to map
	 * @param valueStr
	 * @return
	 * @throws Exception
	 */
	public static Map<String, Object> getK8sJsonToMap(String valueStr) throws Exception {
		Map<String, Object> valueMap = ObjectMapperUtils.getMapper().readValue(valueStr, new TypeReference<Map<String, Object>>(){});
		log.debug("apiVersion : {}, kind : {}", Optional.ofNullable(valueMap.get(KubeConstants.APIVSERION)).orElseGet(() ->""), Optional.ofNullable(valueMap.get(KubeConstants.KIND)).orElseGet(() ->""));
		return valueMap;
	}

	public static Map<String, Object> getK8sYamlToMap(String yamlStr) throws Exception {
		Object object = getYamlObjects(yamlStr).get(0);
		JsonNode objNode = ObjectMapperUtils.getPatchMapper().valueToTree(object);
		String objStr = ObjectMapperUtils.getPatchMapper().writeValueAsString(objNode);
		Map<String, Object> valueMap = ObjectMapperUtils.getMapper().readValue(objStr, new TypeReference<Map<String, Object>>(){});
		log.debug("apiVersion : {}, kind : {}", Optional.ofNullable(valueMap.get(KubeConstants.APIVSERION)).orElseGet(() ->""), Optional.ofNullable(valueMap.get(KubeConstants.KIND)).orElseGet(() ->""));
		return valueMap;
	}

	public static K8sApiKindType getK8sKindInObject(Object k8sObj, JSON k8sJson) throws Exception {
		return K8sApiKindType.findKindTypeByValue(MapUtils.getString(getK8sObjectToMap(k8sObj, k8sJson), KubeConstants.KIND));
	}

	public static K8sApiKindType getK8sKindInMap(Map<String, Object> k8sObjectMap) throws Exception {
		return K8sApiKindType.findKindTypeByValue(MapUtils.getString(k8sObjectMap, KubeConstants.KIND));
	}

	public static K8sApiGroupType getK8sGroupInObject(Object k8sObj, JSON k8sJson) throws Exception {
		return getK8sGroupInMap(getK8sObjectToMap(k8sObj, k8sJson));
	}

	public static K8sApiGroupType getK8sGroupInMap(Map<String, Object> k8sObjectMap) throws Exception {
		if (StringUtils.indexOf(MapUtils.getString(k8sObjectMap, KubeConstants.APIVSERION), "/") < 1) {
			return null;
		} else {
			String groupStr = StringUtils.upperCase(StringUtils.split(MapUtils.getString(k8sObjectMap, KubeConstants.APIVSERION), "/")[0]);
			return K8sApiGroupType.findApiGroupByValue(groupStr);
		}
	}

	public static K8sApiType getK8sVersionInObject(Object k8sObj, JSON k8sJson) throws Exception {
		return getK8sVersionInMap(getK8sObjectToMap(k8sObj, k8sJson));
	}

	public static K8sApiType getK8sVersionInMap(Map<String, Object> k8sObjectMap) throws Exception {
		if (StringUtils.indexOf(MapUtils.getString(k8sObjectMap, KubeConstants.APIVSERION), "/") < 1) {
			return K8sApiType.findApiTypeByValue(MapUtils.getString(k8sObjectMap, KubeConstants.APIVSERION));
		} else {
			String versionStr = StringUtils.upperCase(StringUtils.split(MapUtils.getString(k8sObjectMap, KubeConstants.APIVSERION), "/")[1]);
			return K8sApiType.findApiTypeByValue(versionStr);
		}
	}

	public static V1ObjectMeta getK8sObjectMetaInObject(Object k8sObj, JSON k8sJson) throws Exception {
		if (k8sJson == null) {
			k8sJson = new JSON();
		}

		Map<String, Object> k8sObjectMap = getK8sObjectToMap(k8sObj, k8sJson);
		return ServerUtils.getK8sObjectMetaInMap(k8sObjectMap, k8sJson);

	}

	@SuppressWarnings("unchecked")
	public static V1ObjectMeta getK8sObjectMetaInMap(Map<String, Object> k8sObjectMap, JSON k8sJson) {
		if (k8sJson == null) {
			k8sJson = new JSON();
		}
		if (MapUtils.isNotEmpty(k8sObjectMap) && MapUtils.getObject(k8sObjectMap, KubeConstants.META, null) != null) {
			Object k8sMetadataObj = k8sObjectMap.get(KubeConstants.META);
			if (k8sMetadataObj instanceof Map) {
				Map<String, Object> metadata = (Map<String, Object>)k8sMetadataObj;
				return k8sJson.deserialize(k8sJson.serialize(metadata), V1ObjectMeta.class);
			} else if (k8sMetadataObj instanceof V1ObjectMeta) {
				return (V1ObjectMeta)k8sMetadataObj;
			}
		}

		return null;

	}

	/**
	 * targetMaps 에 compareMaps 이 포함되어 있는지 확인하는 메서드
	 *
	 * @param targetMaps
	 * @param compareMaps
	 * @return
	 */
	public static<K,V> boolean containMaps(Map<K, V> targetMaps, Map<K, V> compareMaps){
		boolean contain = false;

		if(MapUtils.isNotEmpty(targetMaps) && MapUtils.isNotEmpty(compareMaps)) {
			MapDifference<K, V> diff = Maps.difference(targetMaps, compareMaps);
			MapDifference<K, V> diff2 = Maps.difference(diff.entriesInCommon(), compareMaps);

			contain = diff2.areEqual();
		}

		return contain;
	}

	/**
	 * Convert YAML to JSON
	 * @param yamlString
	 * @return
	 */
	public static String convertYamlToJson(String yamlString) throws Exception {
		Map<String,Object> map = Yaml.getSnakeYaml(null).load(yamlString);
		JSONObject jsonObject = new JSONObject(map);
		return jsonObject.toString();
	}

	/**
	 * Convert YAML List to JSON List
	 * @param yamlString
	 * @return
	 */
	public static String convertYamlListToJsonList(String yamlString) throws Exception {
		List<Object> objs = ServerUtils.getYamlObjectsWithSnakeYaml(yamlString);
		if(CollectionUtils.isEmpty(objs)) {
			return null;
		}

		if(objs.size() > 1) { // JSONArray
			JSONArray jsonArray = new JSONArray();
			for(Object obj : objs) {
				Map<String,Object> map= Yaml.getSnakeYaml(null).load(ServerUtils.marshalYaml(obj));
				JSONObject jsonObject = new JSONObject(map);
				jsonArray.put(jsonObject);
			}
			return jsonArray.toString();
		}
		else if(objs.size() == 1) { // JSONObject
			return ServerUtils.convertYamlToJson(ServerUtils.marshalYaml(objs.get(0)));
		}
		else {
			return null;
		}
	}

	public static <T> T getObjectsInWorkload(String deployment_config, String jsonPath, TypeRef<T> targetType, boolean isThrow) throws Exception {

		T objects = null;

		try {
			objects = JsonPathUtils.getInstance().parse(deployment_config).read(jsonPath, targetType);
		} catch (PathNotFoundException e) {
			log.debug("objects not found!! - {} : {}", e.getClass().getName(), e.getMessage());
		} catch (Exception e){
			if (isThrow) {
				throw e;
			}
		}

		return objects;
	}

	/**
	 * 서버명 규칙 체크
	 *
	 * @param serverName
	 * @throws Exception
	 */
	public static boolean checkServerNameRule(String serverName, boolean isThrow) throws Exception{
		return ServerUtils.checkServerNameRule(serverName, isThrow, "Server Name is invalid name rule!!");
	}
	public static boolean checkServerNameRule(String serverName, boolean isThrow, String exceptionMessage) throws Exception{
		boolean isValid = false;
		if(StringUtils.isNotBlank(serverName)){
			/**
			 * K8s Service 생성시 이름을 숫자로 시작하여 생성하면 오류가 발생합니다. 저희는 서버명을 이용하여 서비스명을 생성하므로 서버명 체크 로직에 반영
			 *
			 * a DNS-1035 label must consist of lower case alphanumeric characters or '-', start with an alphabetic character, and end with an alphanumeric character (e.g. 'my-name',  or 'abc-123', regex used for validation is '[a-z]([-a-z0-9]*[a-z0-9])?')
			 */
			isValid = serverName.matches(KubeConstants.RULE_SERVICE_NAME);
		}

		if(!isValid && isThrow){
			throw new CocktailException(exceptionMessage, ExceptionType.InvalidParameter);
		}

		return isValid;
	}

	public static boolean checkServerNameRule(String serverName) throws Exception{
		return ServerUtils.checkServerNameRule(serverName, true);
	}

	public static boolean checkHpaNameRule(String hpaName) throws Exception{
		return ServerUtils.checkServerNameRule(hpaName, true, "Horizontal Pod Autoscaler Name is invalid name rule!!");
	}

	public static Integer getInteger(String param) throws Exception {
		if(StringUtils.isBlank(param)) {
			return null;
		}
		Integer val = null;
		if (Pattern.matches("^[0-9]+$", param)) {
			val = Integer.valueOf(param);
		}
		return val;
	}
}
