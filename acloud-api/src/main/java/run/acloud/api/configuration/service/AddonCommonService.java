package run.acloud.api.configuration.service;

import com.google.common.collect.Maps;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.moandjiezana.toml.Toml;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Factory;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import run.acloud.api.catalog.service.PackageK8sService;
import run.acloud.api.catalog.vo.HelmResourcesVO;
import run.acloud.api.configuration.constants.AddonConstants;
import run.acloud.api.configuration.dao.IClusterMapper;
import run.acloud.api.configuration.enums.AddonDynamicValueType;
import run.acloud.api.configuration.enums.AddonKeyItem;
import run.acloud.api.configuration.enums.ComparisonOperator;
import run.acloud.api.configuration.util.AddonUtils;
import run.acloud.api.configuration.vo.AddonInstallVO;
import run.acloud.api.configuration.vo.ClusterVO;
import run.acloud.api.k8sextended.util.Yaml;
import run.acloud.api.log.service.LogAgentService;
import run.acloud.api.resource.constants.KubeConstants;
import run.acloud.api.resource.enums.JsonPatchOp;
import run.acloud.api.resource.service.*;
import run.acloud.api.resource.util.ResourceUtil;
import run.acloud.api.resource.vo.*;
import run.acloud.commons.enums.CRUDCommand;
import run.acloud.commons.util.*;
import run.acloud.commons.vo.CertificateVO;
import run.acloud.framework.context.ContextHolder;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;
import run.acloud.framework.properties.CocktailAddonProperties;

import javax.security.auth.x500.X500PrivateCredential;
import java.util.*;

@Service
@Slf4j
public class AddonCommonService {

	@Resource(name = "cocktailSession")
	private SqlSessionTemplate sqlSession;

	@Autowired
	private K8sResourceService k8sResourceService;

	@Autowired
	private CocktailAddonProperties cocktailAddonProperties;

	@Autowired
	private IngressSpecService ingressSpecService;

	@Autowired
	private ServiceSpecService serviceSpecService;

	@Autowired
	private ConfigMapService configMapService;

	@Autowired
	private SecretService secretService;

	@Autowired
	private PackageK8sService packageK8sService;

	@Autowired
	private LogAgentService logAgentService;

	public Map<String, List<K8sPodVO>> getPodToMap(List<K8sPodVO> pods, Map<String, List<K8sPodVO>> podMap, Map<String, List<K8sEventVO>> eventMap){
		if(CollectionUtils.isNotEmpty(pods)){
			String ownerName = "";
			for(K8sPodVO podRow : pods){
				ownerName = "";
                if (CollectionUtils.isNotEmpty(podRow.getDetail().getOwnerReferences())) {
                    for (K8sOwnerReferenceVO ownerReferenceRow : podRow.getDetail().getOwnerReferences()) {
						ownerName = ownerReferenceRow.getName();
                        break;
                    }
                }
				if (StringUtils.isBlank(ownerName)) {
					ownerName = podRow.getPodName();
				}
				if(StringUtils.isNotBlank(ownerName) && !podMap.containsKey(ownerName)){
					podMap.put(ownerName, new ArrayList<>());
				}

				if(StringUtils.isNotBlank(ownerName) && podMap.get(ownerName) != null){
					if (MapUtils.isNotEmpty(eventMap)) {
						podRow.setEvents(eventMap.get(podRow.getPodName()));
					}
					podMap.get(ownerName).add(podRow);
				}
			}
		}

		return podMap;
	}

	public Map<String, List<K8sReplicaSetVO>> getReplicaSetToMap(List<K8sReplicaSetVO> replicaSets, Map<String, List<K8sReplicaSetVO>> replicaSetMap, Map<String, List<K8sEventVO>> eventMap){
		if(CollectionUtils.isNotEmpty(replicaSets)){
			String ownerName = "";
			for(K8sReplicaSetVO replicaSetRow : replicaSets){
				ownerName = "";
                if (CollectionUtils.isNotEmpty(replicaSetRow.getDetail().getOwnerReferences())) {
                    for (K8sOwnerReferenceVO ownerReferenceRow : replicaSetRow.getDetail().getOwnerReferences()) {
						ownerName = ownerReferenceRow.getName();
                        break;
                    }
                }
				if (StringUtils.isBlank(ownerName)) {
					ownerName = replicaSetRow.getName();
				}
				if(StringUtils.isNotBlank(ownerName) && !replicaSetMap.containsKey(ownerName)){
					replicaSetMap.put(ownerName, new ArrayList<>());
				}

				if(replicaSetMap.get(ownerName) != null){
					replicaSetRow.setEvents(eventMap.get(replicaSetRow.getName()));
					replicaSetMap.get(ownerName).add(replicaSetRow);
				}
			}
		}

		return replicaSetMap;
	}

	protected Map<String, List<K8sJobVO>> getJobToMap(List<K8sJobVO> allJobs, List<K8sJobVO> jobs, Map<String, List<K8sJobVO>> jobMap, Map<String, List<K8sEventVO>> eventMap, String addonName){
		if(CollectionUtils.isNotEmpty(allJobs)){
			String ownerName = "";
			for(K8sJobVO jobRow : allJobs){
				ownerName = "";
				// Set event
				jobRow.setEvents(eventMap.get(jobRow.getName()));
                if (CollectionUtils.isNotEmpty(jobRow.getDetail().getOwnerReferences())) {
                    for (K8sOwnerReferenceVO ownerReferenceRow : jobRow.getDetail().getOwnerReferences()) {
						ownerName = ownerReferenceRow.getName();
                        break;
                    }
                } else {
					if (StringUtils.equals(MapUtils.getString(jobRow.getLabels(), KubeConstants.LABELS_ADDON_INSTANCE_KEY, ""), addonName)
							&& StringUtils.equals(MapUtils.getString(jobRow.getLabels(), KubeConstants.LABELS_ADDON_NAME_KEY, ""), addonName) ) {
						jobs.add(jobRow);
					}
				}
				if(StringUtils.isNotBlank(ownerName) && !jobMap.containsKey(ownerName)){
					jobMap.put(ownerName, new ArrayList<>());
				}

				if(jobMap.get(ownerName) != null){
					jobMap.get(ownerName).add(jobRow);
				}
			}
		}

		return jobMap;
	}

	public void addPod(List<K8sPodVO> pods, Map<String, List<K8sPodVO>> podMap, String key){
		if (MapUtils.getObject(podMap, key, null) != null) {
			pods.addAll(MapUtils.getObject(podMap, key, null));
		}
	}

	public void addJob(List<K8sJobVO> jobs, Map<String, List<K8sJobVO>> jobMap, String key){
		if (MapUtils.getObject(jobMap, key, null) != null) {
			jobs.addAll(MapUtils.getObject(jobMap, key, null));
		}
	}

	/**
	 * Dynamic Value가 존재하는지 확인하고 있으면 치환..
	 * @param value
	 * @param dynamicValues
	 * @return
	 * @throws Exception
	 */
	public String convertDynamicValues(String value, Map<String, String> dynamicValues) throws Exception {
		/** Dynamic Value로 설정된 내용이 있으면 찾아서 Replace 처리.. **/
		List<String> foundDynamicValues = this.findIncludedDynamicValue(value);
		for(String dynamicValueKey : foundDynamicValues) {
			String foundValue = MapUtils.getString(dynamicValues, dynamicValueKey, null);
			if(StringUtils.isBlank(foundValue)) { // Dynamic Value로 설정된 값이 존재하지 않으면 Log 기록
				log.warn("Dynamic Values not found : " + dynamicValueKey + " : " + foundValue);
			}
			value = StringUtils.replace(value, dynamicValueKey, foundValue); // dynamicValue로 설정된 값을 foundValue로 교체...
		}

		return value;
	}

	/**
	 * Dynamic Value로 설정된 값이 있으면 찾아서 응답...
	 * @param value
	 * @return
	 */
	public List<String> findIncludedDynamicValue(String value) {
		List<String> names = new ArrayList<>();
		for(String dynamicValue : AddonDynamicValueType.getAddonDynamicValueTypeValues()) {
			if(value.contains(dynamicValue)) {
				names.add(dynamicValue);
			}
		}

		return names;
	}

	public Map<String, Object> generateValuesMap(AddonInstallVO addonInstall, ClusterVO cluster, Map<String, String> dynamicValues, CRUDCommand command) throws Exception {
		/** value.yaml 편집 : addon.toml 내용과 병합하여 설치함.**/
		String valuesYaml = this.generateAddonValues(addonInstall.getReleaseName(), addonInstall.getDefaultValueYaml(), addonInstall.getAddonToml(), cluster, command);

		/** dynamic value에 해당하는 값이 있으면 replace **/
		valuesYaml = this.convertDynamicValues(valuesYaml, dynamicValues);
		valuesYaml = this.setDefaultValuesYaml(valuesYaml); // 특정 키값들은 사용자로부터 입력값이 없으면 반드시 기본값을 셋팅해주어야 함... TODO : 표준화...

		Map<String, Object> valuesMap = Yaml.getSnakeYaml().load(valuesYaml);

		return valuesMap;
	}

	/**
	 * Generate Values.yaml
	 * @param addonInstall
	 * @param cluster
	 * @param dynamicValues
	 * @param command
	 * @return
	 * @throws Exception
	 */
	public String generateValuesYaml(AddonInstallVO addonInstall, ClusterVO cluster, Map<String, String> dynamicValues, CRUDCommand command) throws Exception {
		Map<String, Object> valuesMap = this.generateValuesMap(addonInstall, cluster, dynamicValues, command);
		String valuesYaml = AddonUtils.genAddonConfigMapYaml(valuesMap); // set Block Style

		return valuesYaml;
	}

	/**
	 * Yaml의 특정 Key Field에 입력이 없으면 Default Value를 설정함...
	 * @param valuesYaml
	 * @return
	 * @throws Exception
	 */
	public String setDefaultValuesYaml(String valuesYaml) throws Exception {
		if(StringUtils.contains(valuesYaml, "#{kubeEtcd.endpoints}")) {
			valuesYaml = StringUtils.replace(valuesYaml, "#{kubeEtcd.endpoints}", "[]");
		}
		if(StringUtils.contains(valuesYaml, "#{kubeEtcd.caCrt}")) {
			valuesYaml = StringUtils.replace(valuesYaml, "#{kubeEtcd.caCrt}", "\"\"");
		}
		if(StringUtils.contains(valuesYaml, "#{kubeEtcd.healthcheckClientCrt}")) {
			valuesYaml = StringUtils.replace(valuesYaml, "#{kubeEtcd.healthcheckClientCrt}", "\"\"");
		}
		if(StringUtils.contains(valuesYaml, "#{kubeEtcd.healthcheckClientKey}")) {
			valuesYaml = StringUtils.replace(valuesYaml, "#{kubeEtcd.healthcheckClientKey}", "\"\"");
		}

		return valuesYaml;
	}

	/**
	 * Toml 파일에 설정된 데이터를 Yaml로 Merge...
	 * @param yaml
	 * @param toml
	 * @return
	 * @throws Exception
	 */
	public String generateAddonValues(String releaseName, String yaml, String toml, ClusterVO cluster, CRUDCommand command) throws Exception {
		Toml addonToml = new Toml().read(toml);
		Toml release = addonToml.getTable(AddonConstants.ADDON_VALUE_RELEASE);
		Toml setup = addonToml.getTable(AddonConstants.ADDON_VALUE_SETUP);
		Toml detail = addonToml.getTable(AddonConstants.ADDON_VALUE_DETAIL);
		Toml specific = addonToml.getTable(AddonConstants.ADDON_VALUE_SPECIFIC);

		/** Release 설정 **/
		// release Name은 RequestBody로 넘어온 정보를 사용하므로 Skip..
		if(release != null) {
			Toml releaseInfo = release.getTable(AddonConstants.ADDON_VALUE_RELEASE_NAME);
		}

		/**
		 * Specific 설정
		 * - Detail보다 Specific 설정을 먼저 해주어야 함...
		 * - Specific은 Detail에 설정된 값을 특성에 맞도록 보정하는 용도인데...
		 * - Detail에서 이미 값이 치환되어 버리면 Specific 설정에서 치환할 수 없기 때문...
		 **/
		if(specific != null) {
			String cubeType = cluster.getCubeType().getCode();

			Toml tomlCubeType = specific.getTable("cubeType");
			Toml tomlCubeTypeSelect = tomlCubeType.getTable(cubeType);

			if(tomlCubeTypeSelect != null) {
				detail = this.overWriteSpecificToDetail(tomlCubeTypeSelect, detail); // overwrite specific to detail
				yaml = this.mergeYamlWithToml(releaseName, tomlCubeTypeSelect, yaml, addonToml, cluster, command);
			}
		}

		/** Setup Data 설정 **/
		if(setup != null) {
			yaml = this.mergeYamlWithToml(releaseName, setup, yaml, addonToml, cluster, command);
		}

		/** Detail 설정 **/
		if(detail != null) {
			yaml = this.mergeYamlWithToml(releaseName, detail, yaml, addonToml, cluster, command);
		}

		return yaml;
	}

	/**
	 * Specific 내용중 Detail로 Overwrite할 내용이 있으면 치환...
	 * @param specific
	 * @param detail
	 */
	public Toml overWriteSpecificToDetail(Toml specific, Toml detail) throws Exception {
		Map<String, Object> specificData = specific.toMap();
		Map<String, Object> detailData = detail.toMap();
		for (String key : specificData.keySet()) {
			// Target이 없으면 continue...
			if(!detail.containsTable(key)) {
				continue;
			}
			Map<String, Object> targetKeyMap = (Map<String, Object>)detailData.get(key);
			String dataType = specific.getString(key + ".type");
			if(StringUtils.equalsIgnoreCase("String", dataType)) {
				String fromValue = specific.getString(key + ".value");
				targetKeyMap.put("value", fromValue);
			}
			else if(StringUtils.equalsIgnoreCase("Long", dataType)) {
				Long fromValue = specific.getLong(key + ".value");
				targetKeyMap.put("value", fromValue);
			}
			else if(StringUtils.equalsIgnoreCase("Boolean", dataType)) {
				Boolean fromValue = specific.getBoolean(key + ".value");
				targetKeyMap.put("value", fromValue);
			}
		}

		return detail;
	}

	/**
	 * Addon.toml 파일의 Data field 정의 객체들을 loop 둘면서 Parsing 하고 Yaml matcher에 merge 해주는 Method..
	 * ex)
	 * 	[detail.kubeEtcd_healthcheckClientKey]                              # kubeEtcd_healthcheckClientKey 필드에 대한 입력 처리..
	 * 		visible	= true
	 * 		display = "kubeEtcd Healthcheck Client Key"
	 * 		form    = "TextArea"
	 * 		type    = "String"                                              # Data Type
	 * 		value   = ""                                                    # Value
	 * 		matcher = "#{kubeEtcd.healthcheckClientKey}"                    # defaultValue.yaml과의 매칭 Key..
	 * 		encoder = "Base64"                                              # encoder 옵션이 있으면 해당 encoder로 인코드
	 * 		decoder = "Base64"                                              # decoder 옵션이 있으면 해당 decoder로 디코드
	 *
	 * 		[[detail.kubeEtcd_healthcheckClientKey.visibleConditions]]      # UI에서 입력 필드를 보여줄지 여부에 사용.. 해당 조건이 모두 true일때만 Field를 화면에 노출.. 하위 필드 설명은 enableCondition 참조..
	 * 			ref   = "SELF"
	 * 			type  = "Boolean"
	 * 			field = "detail.kubeEtcd_enabled.value"
	 * 			matchValue = true
	 * 		    comparisonOperator = "equalTo"
	 *
	 * 		[[detail.kubeEtcd_healthcheckClientKey.enableConditions]]       # enableConditions가 존재하면 해당 조건이 모두 true일때만 값 사용...
	 * 			ref   = "SELF"                                              # SELF : 현재 Toml 문서 내에서 참조.. / SYSTEM : System의 어떤(?) 값을 사용 => field가 추가될때마다 코딩해주어야 함..
	 * 			type  = "Boolean"                                           # Boolean 값으로 비교
	 * 			field = "detail.kubeEtcd_enabled.value"                     # 비교할 필드
	 * 			matchValue = true                                           # 위에 정의한 필드의 값고, matchValue의 값이 아래 comparisonOperator의 조건에 맞는지 체크 (comparisonOperator가 없으면 default는 equalTo)
	 * 		    comparisonOperator = "equalTo"                              # comparisonOperator (equalTo {field}, unequalTo {field}, lessThan {field}, greaterThan {field}, greaterThanOrEqualTo {field}, lessThanOrEqualTo {field})
	 *
	 * @param detail
	 * @param yaml
	 * @param addonToml
	 * @return
	 * @throws Exception
	 */
	public String mergeYamlWithToml(String releaseName, Toml detail, String yaml, Toml addonToml, ClusterVO cluster, CRUDCommand command) throws Exception {
		Map<String, ConfigMapGuiVO> configMapTypeCache = new HashMap<>();
		Map<String, SecretGuiVO> secretTypeCache = new HashMap<>();
		Map<String, String> fileSystemCache = new HashMap<>();

		Map<String, Object> detailData = detail.toMap();
		for (String key : detailData.keySet()) {
			if(StringUtils.equalsAnyIgnoreCase(key, "visible", "display")) { // yaml과 merge되지 않는 Key...
				continue;
			}

			/** 01. 사용할 수 있는 입력 필드인지 확인 (enableConditions) **/
			List<Toml> enableConditions = detail.getTables(key + "." + AddonConstants.ADDON_VALUE_ENABLECONDATIONS);
			if(CollectionUtils.isNotEmpty(enableConditions)) {
				boolean isEnable = false;
				for(Toml enableCondition : enableConditions) {
					String ref = enableCondition.getString("ref");
					String type = enableCondition.getString("type");
					String field = enableCondition.getString("field");
					String operator = enableCondition.getString("comparisonOperator");
					if(StringUtils.isBlank(ref)) {
						log.warn("EnableCondition : ref Value is null");
						isEnable = false;
						break;
					}
					if(StringUtils.isBlank(type)) {
						log.warn("EnableCondition : type Value is null");
						isEnable = false;
						break;
					}
					if(StringUtils.isBlank(field)) {
						log.warn("EnableCondition : field Value is null");
						isEnable = false;
						break;
					}
					if(StringUtils.isBlank(operator)) {
						// operator가 지정되어 있지 않으면 Default로 equalTo를 적용한다...
						operator = ComparisonOperator.equalTo.getValue();
					}

					if(StringUtils.equalsIgnoreCase("String", type)) {
						String sLeftVal = enableCondition.getString("matchValue");
						if(StringUtils.isBlank(sLeftVal)) {
							log.warn("EnableCondition : match Value is null");
							isEnable = false; // Refrence Type이 알 수 없는 유형이면 해당 값을 사용하지 않고 종료...
							break;
						}
						String sRightVal = "";
						if(StringUtils.equalsIgnoreCase("SELF", ref)) {
							sRightVal = addonToml.getString(field);
						}
						else if(StringUtils.equalsIgnoreCase("SYSTEM", ref)) {
							if(StringUtils.equalsIgnoreCase("cluster.cubeType", field)) {
								sRightVal = cluster.getCubeType().getCode();
							}
						}
						else {
							log.warn("EnableCondition : Invalid Refrence Type");
							isEnable = false; // Refrence Type이 알 수 없는 유형이면 해당 값을 사용하지 않고 종료...
							break;
						}

						ComparisonOperator compOper = null;
						try {
							compOper = ComparisonOperator.valueOf(operator);
						}
						catch(Exception ex) {
							log.warn("can't get ComparisonOperator : " + operator);
						}

						if(compOper != null) {
							switch (compOper) {
								case equalTo:
									if(StringUtils.equals(sLeftVal, sRightVal)) {
										isEnable = true;
									}
									else {
										isEnable = false;
									}
									break;
								case unequalTo:
									if(!StringUtils.equals(sLeftVal, sRightVal)) {
										isEnable = true;
									}
									else {
										isEnable = false;
									}
									break;
								// String은 equals unequals 만 비교한다... 나머지 비교 요청은 false 설정...
								case lessThan:
								case lessThanOrEqualTo:
								case greaterThan:
								case greaterThanOrEqualTo:
								default:
									isEnable = false;
									break;
							}
						}
					}
					else if(StringUtils.equalsIgnoreCase("Long", type)) {
						Long longLeftVal = enableCondition.getLong("matchValue");
						if(longLeftVal == null) {
							log.warn("EnableCondition : match Value is null");
							isEnable = false;
							break;
						}
						long lLeftVal = longLeftVal.longValue();
						long lRightVal = lLeftVal+1; // LeftValue와 다른 값을 default로 설정..

						if(StringUtils.equalsIgnoreCase("SELF", ref)) {
							Long longRightVal = addonToml.getLong(field);
							if(longRightVal == null) {
								log.warn("EnableCondition : field Value is null");
								isEnable = false;
								break;
							}
							lRightVal = longRightVal.longValue();
						}
//						else if(StringUtils.equalsIgnoreCase("SYSTEM", ref)) {
//							// 아직 케이스 없음...
//						}
						else {
							log.warn("EnableCondition : Invalid Refrence Type");
							isEnable = false; // Refrence Type이 알 수 없는 유형이면 해당 값을 사용하지 않고 종료...
							break;
						}

						ComparisonOperator compOper = null;
						try {
							compOper = ComparisonOperator.valueOf(operator);
						}
						catch(Exception ex) {
							log.warn("can't get ComparisonOperator : " + operator);
						}

						if(compOper != null) {
							switch (compOper) {
								case equalTo:
									if(lLeftVal == lRightVal) {
										isEnable = true;
									}
									else {
										isEnable = false;
									}
									break;
								case unequalTo:
									if(lLeftVal != lRightVal) {
										isEnable = true;
									}
									else {
										isEnable = false;
									}
									break;
								case lessThan:
									if(lLeftVal < lRightVal) {
										isEnable = true;
									}
									else {
										isEnable = false;
									}
									break;
								case lessThanOrEqualTo:
									if(lLeftVal <= lRightVal) {
										isEnable = true;
									}
									else {
										isEnable = false;
									}
									break;
								case greaterThan:
									if(lLeftVal > lRightVal) {
										isEnable = true;
									}
									else {
										isEnable = false;
									}
									break;
								case greaterThanOrEqualTo:
									if(lLeftVal >= lRightVal) {
										isEnable = true;
									}
									else {
										isEnable = false;
									}
									break;
								default:
									// 연산자가 맞지 않으면 default는 false...
									isEnable = false;
									break;
							}
						}
					}
					else if(StringUtils.equalsIgnoreCase("Boolean", type)) {
						Boolean bLeftVal = enableCondition.getBoolean("matchValue");
						Boolean bRightVal = null;

						if(bLeftVal == null) {
							log.warn("EnableCondition : match Value is null");
							isEnable = false; //참조 값이 없으면 사용안함...
							break;
						}
						else if(bLeftVal.booleanValue()) {
							// Default Value를 bLeftVal과 반대 값으로 설정하여 not equal 되도록 처리...
							bRightVal = Boolean.FALSE;
						}
						else {
							bRightVal = Boolean.TRUE;
						}

						if(StringUtils.equalsIgnoreCase("SELF", ref)) {
							bRightVal = addonToml.getBoolean(field);
							if(bRightVal == null) {
								log.warn("EnableCondition : field Value is null");
								isEnable = false; //Field의 값이 없으면 사용안함...
								break;
							}
						}
//						else if(StringUtils.equalsIgnoreCase("SYSTEM", ref)) {
//							// 아직 케이스 없음...
//						}
						else {
							log.warn("EnableCondition : Invalid Refrence Type");
							isEnable = false; // Refrence Type이 알 수 없는 유형이면 해당 값을 사용하지 않고 종료...
							break;
						}

						ComparisonOperator compOper = null;
						try {
							compOper = ComparisonOperator.valueOf(operator);
						}
						catch(Exception ex) {
							log.warn("can't get ComparisonOperator : " + operator);
						}

						if(compOper != null) {
							switch (compOper) {
								case equalTo:
									if(bLeftVal.booleanValue() == bRightVal.booleanValue()) {
										isEnable = true;
									}
									else {
										isEnable = false;
									}
									break;
								case unequalTo:
									if(bLeftVal.booleanValue() != bRightVal.booleanValue()) {
										isEnable = true;
									}
									else {
										isEnable = false;
									}
									break;
								// Boolaen은 equals unequals 만 비교한다... 나머지 비교 요청은 false 설정...
								case lessThan:
								case lessThanOrEqualTo:
								case greaterThan:
								case greaterThanOrEqualTo:
								default:
									isEnable = false;
									break;
							}
						}
					}

					if(!isEnable) { // false면 loop 종료 (enableConditions는 모든 조건이 true 일때만 동작)
						break;
					}
				}

				if(!isEnable) {
					// isEnabled가 false 이면 사용하지 않음..
					continue;
				}
				// isEnabled가 true 이면 다음 Step으로 진입...
			}

			/** 02. Validation 설정이 있다면 지정한 Validator를 사용하여 Validation을 체크한다 **/
			List<Toml> validations = detail.getTables(key + "." + AddonConstants.ADDON_VALUE_VALIDATIONS);
			if(CollectionUtils.isNotEmpty(validations)) {
				for (Toml validation : validations) {
					String ref = validation.getString("ref");
					String type = validation.getString("type");
					String field = validation.getString("field");
					String validator = validation.getString("validator");

					if (StringUtils.isBlank(ref)) {
						log.warn("validation : ref Value is null");
						break;
					}
					if (StringUtils.isBlank(type)) {
						log.warn("validation : type Value is null");
						break;
					}
					if (StringUtils.isBlank(field)) {
						log.warn("validation : field Value is null");
						break;
					}
					if (StringUtils.isBlank(validator)) {
						log.warn("validation : validator Value is null");
						break;
					}

					if (StringUtils.equalsIgnoreCase(validator, "nodePortsExists")) {
						/** NodePort 중복 체크 **/
						if (!StringUtils.equalsIgnoreCase("Long", type)) {
							log.warn("validation : Node Port Exists Parameter type must be Long : current Type is " + type);
							break;
						}

						if (StringUtils.equalsIgnoreCase("SELF", ref)) {
							Long nodePort = addonToml.getLong(field);
							if (nodePort == null) {
								log.warn("validation : Node Port Value is null");
								break;
							}

							this.nodePortValidator(cluster, nodePort.intValue(), command);
						}
					}
				} // for
			} // if exists validations


			/** 03. Data Type에 따라 Value를 얻어오는 처리 **/
			String dataType = detail.getString(key + ".type");
			String dataForm = detail.getString(key + ".form");
			String dataRef = detail.getString(key + ".ref"); // 데이터 Reference : ConfigMap or Secret
			String value = null;
			if(StringUtils.equalsIgnoreCase("String", dataType)) {
				value = detail.getString(key + ".value");

				/** Data Form이 kubernetes Resource일 경우 **/
				if(StringUtils.equalsAnyIgnoreCase(dataRef, "Secret", "ConfigMap", "FileSystem", "Token")) {
					String namespace = detail.getString(key + ".namespace");
					String name = detail.getString(key + ".name");
					String dataKey = detail.getString(key + ".key");
					String path = detail.getString(key + ".path");
					String cacheMapKey = namespace + name + path;

					if(StringUtils.equalsIgnoreCase(dataRef, "ConfigMap")) {
						ConfigMapGuiVO tempCm;
						if(configMapTypeCache.containsKey(cacheMapKey)) { // is Exist in cache
							tempCm = MapUtils.getObject(configMapTypeCache, cacheMapKey);
						}
						else {
							tempCm = configMapService.getConfigMap(cluster, namespace, name);
							if(tempCm != null) {
								configMapTypeCache.put(cacheMapKey, tempCm);
							}
							else {
								throw new CocktailException(String.format("Could not found ConfigMap [%s] in namespace [%s]", name, namespace), ExceptionType.K8sConfigMapNotFound);
							}
						}
						if(MapUtils.isNotEmpty(tempCm.getData())
							&& tempCm.getData().containsKey(dataKey)) {
							value = tempCm.getData().get(dataKey);
						}
						else {
							throw new CocktailException(String.format("Could not found ConfigMap [%s] and Key [%s] in namespace [%s]", name, dataKey, namespace), ExceptionType.K8sConfigMapKeyInvalid);
						}
					}
					else if(StringUtils.equalsIgnoreCase(dataRef, "Secret")) {
						SecretGuiVO tempSecret;
						if(secretTypeCache.containsKey(cacheMapKey)) { // is Exist in cache
							tempSecret = MapUtils.getObject(secretTypeCache, cacheMapKey);
						}
						else {
							tempSecret = secretService.getSecret(cluster, namespace, name, true);
							if(tempSecret != null) {
								secretTypeCache.put(cacheMapKey, tempSecret);
							}
							else {
								throw new CocktailException(String.format("Could not found Secret [%s] in namespace [%s]", name, namespace), ExceptionType.K8sSecretNotFound);
							}
						}
						if(MapUtils.isNotEmpty(tempSecret.getData())
							&& tempSecret.getData().containsKey(dataKey)) {
							value = tempSecret.getData().get(dataKey);
							if(Utils.isBase64Encoded(value)) { // base64 encode된 값이면 decode.
								value = new String(Base64Utils.decodeFromString(value), "UTF-8");
							}
						}
						else {
							throw new CocktailException(String.format("Could not found Secret [%s] and Key [%s] in namespace [%s]", name, dataKey, namespace), ExceptionType.K8sSecretNotFound);
						}
					}
					else if(StringUtils.equalsIgnoreCase(dataRef, "FileSystem")) {
						String tempString;
						if(fileSystemCache.containsKey(cacheMapKey)) { // is Exist in cache
							tempString = MapUtils.getString(fileSystemCache, cacheMapKey, "DataNotFound");
						}
						else {
							tempString = FileUtils.readAllLines(path);
							if(StringUtils.isNotBlank(tempString)) {
								fileSystemCache.put(cacheMapKey, tempString);
							}
							else {
								throw new CocktailException(String.format("Could not found File [%s]", name), ExceptionType.FileNotFound);
							}
						}
						if(StringUtils.isNotBlank(tempString)) {
							value = tempString;
							if(Utils.isBase64Encoded(value)) { // base64 encode된 값이면 decode.
								value = new String(Base64Utils.decodeFromString(value), "UTF-8");
							}
						}
						else {
							throw new CocktailException(String.format("Could not found File [%s]", name), ExceptionType.FileNotFound);
						}
					}
					else if(StringUtils.equalsIgnoreCase(dataRef, "Token")) {
						Factory<String> getTokenType = () -> {
							String tokenType = detail.getString(key + ".tokenType");
							if (StringUtils.isBlank(tokenType))
								throw new CocktailException(String.format("Could not found token type [%s]", releaseName), ExceptionType.InvalidAddonConfigurationValue);

							return tokenType;
						};

						// create command 여부 검사를 위 조건과 같이 묶을 경우 마지막 else 예외처리로 넘어가므로 분리
						if (command == CRUDCommand.C) {
							String tokenType = getTokenType.create();
							if (tokenType.equals("logToken")) {
								value = logAgentService.addAddonLogAgentToken(releaseName, cluster);
							}
						}

						// 수정인 경우 이미 배포된 애드온 에이전트의 토큰 값을 조회해서 넣어준다.
						if (command == CRUDCommand.U) {
							String tokenType = getTokenType.create();
							if (tokenType.equals("logToken")) {
								value = logAgentService.getAddonLogAgentToken(releaseName, cluster);
							}
						}

					} else {
						throw new CocktailException(String.format("UnSupported Resource [%s]", dataRef), ExceptionType.CommonNotSupported);
					}

					// value의 첫 문자가 Yaml value의 시작값으로 사용 불가능한 특수문자일 경우 "" 처리.
					if(StringUtils.startsWithAny(value, "[","]","{","}",">","|","*","&","!","%","#","`","@",",",".")) {
						if(!StringUtils.startsWithIgnoreCase(dataForm, "Array")) { // Type이 Array일 경우는 예외.
							value = "\"" + value + "\"";
						}
					}

				}
				/** Kubernetes Resource 참조가 아닐 경우 기존 Logic 처리 **/
				else {
					Long indent = detail.getLong(key + ".indent");
					// 값이 없으면 Array type일때와 일반 String일때를 구분하여 default Value 설정한다...
					if (StringUtils.isBlank(value)) {
						if (StringUtils.startsWithIgnoreCase(dataForm, "Array")) {
							// 값이 없으면 [] 처리... (Array일 경우)
							value = "[]";
						}
						else {
							// 값이 없으면 "" 처리...
							value = "\"\"";
						}
					}

					/** R4.4.0 : Indent Spec이 존재하면 처리해줌.. **/
					if (indent != null && indent.intValue() > 0) {
						String indentStr = "";
						for (int i = 0; i < indent.intValue(); i++) {
							indentStr += AddonConstants.ADDON_VALUE_INDENT_CHAR;
						}

//					value = indentStr + value; // defaultValue.yaml에 indent를 주고 설정한다...
						value = StringUtils.replace(value, "\n", "\n" + indentStr);
					}
				}
			}
			else if(StringUtils.equalsIgnoreCase("Long", dataType)) {
				Long lValue = detail.getLong(key + ".value");
				if(lValue != null) {
					value = lValue.toString();
				}
			}
			else if(StringUtils.equalsIgnoreCase("Boolean", dataType)) {
				Boolean bValue = detail.getBoolean(key + ".value");
				if(bValue != null) {
					value = bValue.toString();
				}
			}

			/** 04. 입력 Form이 Array (ArrayList / ArrayList Map 등) 일 경우 처리.
			 *    - value를 JsonArray 형태로 입력받아 처리함
			 *    - yaml에 Array 적용을 위해서는 "," 이후에 반드시 공백이 존재해야하므로 해당 처리 진행...
			 *    **/
			if(StringUtils.startsWithIgnoreCase(dataForm, "Array")) {
				if(StringUtils.isNotBlank(value)) {
					if(StringUtils.contains(value, ",")) {
						value = StringUtils.replace(value, ",", ", ");
						for (int i = 0; i < 3; i++) {
							if (StringUtils.contains(value, ",  ")) {
								value = StringUtils.replace(value, ",  ", ", ");
							}
							else {
								break;
							}
						}
					}
				}
			}

			/** 05. Encoder / Decoder 설정에 따른 Data Encode, Decode **/
			if(StringUtils.isNotBlank(value)
				&& !StringUtils.equals("[]", value)
				&& !StringUtils.equals("\"\"", value)
			)
			{
				String dataEncoder = detail.getString(key + ".encoder");
				if (StringUtils.isNotBlank(dataEncoder)) {
					if (StringUtils.equalsIgnoreCase("Base64", dataEncoder)) {
						// 이미 Base64 encode되어 있으면 encode하지 않음..
						if(!Utils.isBase64Encoded(value)) {
							value = StringUtils.trim(value);
							value = Base64Utils.encodeToString(org.apache.commons.codec.binary.StringUtils.getBytesUtf8(value));
						}
					}
				}
				String dataDecoder = detail.getString(key + ".decoder");
				if (StringUtils.isNotBlank(dataDecoder)) {
					if (StringUtils.equalsIgnoreCase("Base64", dataDecoder)) {
						// Base64 encode되어 있을때만 decode
						if(Utils.isBase64Encoded(value)) {
							value = new String(Base64Utils.decodeFromString(value), "UTF-8");
						}
					}
				}
			}

			/** 06. value가 Null이 아니면 뭐라도 값이 있다면... Yaml과 Merge.. **/
			if(value != null) {
				String matcher = detail.getString(key + ".matcher");
				yaml = StringUtils.replace(yaml, matcher, value);
			}
		}

		return yaml;
	}

	/**
	 * set Map Data.
	 * @param target
	 * @param toml
	 * @throws Exception
	 */
	public void setMapData(Map<String, String> target, Toml toml, Map<String, String> dynamicValues) throws Exception {
		if(toml != null) {
			Map<String, Object> configmapDataMap = toml.toMap();
			for(String key : configmapDataMap.keySet()) {
				String value = MapUtils.getString(configmapDataMap, key, null);
				if(StringUtils.isNotBlank(value)) {
					/** Dynamic Value로 설정된 내용이 있으면 찾아서 Replace 처리.. **/
					value = this.convertDynamicValues(value, dynamicValues);
					/** key가 value.yaml 이면 new line Block Style로 value가 저장될 수 있도록 함... **/
					if(StringUtils.equalsIgnoreCase(AddonKeyItem.VALUE_YAML.getValue(), key)) {
						Map<String, Object> valuesMap = Yaml.getSnakeYaml().load(value);
						value = AddonUtils.genAddonConfigMapYaml(valuesMap);
					}
					/** 입력!! **/
					target.put(key, value);
				}
			}
		}
	}

	/**
	 * NodePort 중복 체크 Validator
	 * @param cluster
	 * @param nodePort
	 * @throws Exception
	 */
	public void nodePortValidator(ClusterVO cluster, int nodePort, CRUDCommand command) throws Exception {
		Set<Integer> nodePorts = serviceSpecService.getUsingNodePortOfCluster(cluster, null, null, ContextHolder.exeContext()); // 해당 cluster의 k8s - service node port 조회

		switch (command) {
			case C:
				// validator는 생성일때만 태워서 체크한다...
				if(nodePorts.contains(Integer.valueOf(nodePort))) {
					throw new CocktailException("Node port are duplicated.", ExceptionType.NodePortDuplicated);
				}
				break;
			case U:
				// 수정일때 Validator를 태우려면 중복되는 NodePort가 자기 자신의 것인지 판단이 가능해야 함... => 현재 판단할 수 있는 정보가 없음...
				break;
		}
	}

	/**
	 * 기 존재하는 인증서 정보로 Root CA 구성
	 * @param currAddon
	 * @return
	 * @throws Exception
	 */
	public X500PrivateCredential getServerCredFromExistInfo(ConfigMapGuiVO currAddon) throws Exception {
		X500PrivateCredential serverCred = null;

		if(StringUtils.isNotBlank(currAddon.getData().get(AddonKeyItem.KIALI_CA_PRVATE.getValue())) &&
			StringUtils.isNotBlank(currAddon.getData().get(AddonKeyItem.KIALI_CA_PUBLIC.getValue()))) {
			try {
				String privateKey = CryptoUtils.decryptDefaultAES(currAddon.getData().get(AddonKeyItem.KIALI_CA_PRVATE.getValue()));
				privateKey = StringUtils.replace(privateKey, "-----BEGIN RSA PRIVATE KEY-----", "");
				privateKey = StringUtils.replace(privateKey, "-----END RSA PRIVATE KEY-----", "");
				privateKey = StringUtils.replace(privateKey, "\n","");

				String publicKey = CryptoUtils.decryptDefaultAES(currAddon.getData().get(AddonKeyItem.KIALI_CA_PUBLIC.getValue()));
				publicKey = StringUtils.replace(publicKey, "-----BEGIN RSA PUBLIC KEY-----", "");
				publicKey = StringUtils.replace(publicKey, "-----END RSA PUBLIC KEY-----", "");
				publicKey = StringUtils.replace(publicKey, "\n","");

				serverCred = CertificateUtils.createRootCredential(publicKey, privateKey);
			}
			catch (Exception ex) {
				serverCred = CertificateUtils.createRootCredential(true);
			}
		}
		else {
			serverCred = CertificateUtils.createRootCredential(true);
		}

		return serverCred;
	}

	/**
	 * 인증서 생성 및 생성 정보를 Dynamic Value에 셋팅
	 * @param serverCred
	 * @param dynamicValues
	 * @param domain
	 * @param ipAddress
	 * @return
	 * @throws Exception
	 */
	public Map<String, String> setCertificateToDynamicValue(X500PrivateCredential serverCred, Map<String, String> dynamicValues, String domain, String ipAddress) throws Exception {
		X500PrivateCredential clientCred = CertificateUtils.createEndEntityCredential(serverCred.getPrivateKey(), serverCred.getCertificate(), domain, ipAddress);
		CertificateVO rootCert = CertificateUtils.generatePemCertificate(serverCred.getPrivateKey(), serverCred.getCertificate());
		CertificateVO leafCert = CertificateUtils.generatePemCertificate(clientCred.getPrivateKey(), clientCred.getCertificate());

		String rootCrt = CryptoUtils.encryptDefaultAES(rootCert.getCertificate());
		String rootPublic = CryptoUtils.encryptDefaultAES(rootCert.getPublicKey());
		String rootPrivate = CryptoUtils.encryptDefaultAES(rootCert.getPrivateKey());
		String tlsCrt = Base64Utils.encodeToString(leafCert.getCertificate().getBytes("UTF-8"));
		String tlsKey = Base64Utils.encodeToString(leafCert.getPrivateKey().getBytes("UTF-8"));

		/** DynamicValue에 설정 **/
		dynamicValues.put(AddonDynamicValueType.ISTIO_KIALI_TLS_CRT.getValue(), tlsCrt);
		dynamicValues.put(AddonDynamicValueType.ISTIO_KIALI_TLS_KEY.getValue(), tlsKey);
		dynamicValues.put(AddonDynamicValueType.ISTIO_KIALI_TLS_CA_CRT.getValue(), rootCrt);
		dynamicValues.put(AddonDynamicValueType.ISTIO_KIALI_TLS_CA_PUBLIC.getValue(), rootPublic);
		dynamicValues.put(AddonDynamicValueType.ISTIO_KIALI_TLS_CA_PRIVATE.getValue(), rootPrivate);

		return dynamicValues;
	}

	/**
	 * Addon 수정을 위한 patchBocy 구성
	 * @param patchBody     addon configmap에 patch를 위한 body data
	 * @param cluster       cluster information
	 * @param addonUpgrade  addon upgrade input data
	 * @param currAddon     current addon information
	 * @param dynamicValues dynamicValues
	 * @return
	 * @throws Exception
	 */
	public List<JsonObject> generateAddonPatchData(List<JsonObject> patchBody, ClusterVO cluster, AddonInstallVO addonUpgrade, ConfigMapGuiVO currAddon, Map<String, String> dynamicValues) throws Exception {
		String valuesYaml = this.generateValuesYaml(addonUpgrade, cluster, dynamicValues, CRUDCommand.U);

		return this.generateAddonPatchData(patchBody, addonUpgrade, currAddon, dynamicValues, valuesYaml);
	}

	/**
	 * Addon 수정을 위한 patchBody 구성
	 * @param patchBody     addon configmap에 patch를 위한 body data
	 * @param addonUpgrade  addon upgrade input data
	 * @param currAddon     current addon information
	 * @param dynamicValues dynamicValues
	 * @param valuesYaml    value.yaml data
	 * @return
	 * @throws Exception
	 */
	public List<JsonObject> generateAddonPatchData(List<JsonObject> patchBody, AddonInstallVO addonUpgrade, ConfigMapGuiVO currAddon, Map<String, String> dynamicValues, String valuesYaml) throws Exception {
		if(patchBody == null) {
			patchBody = new ArrayList<>();
		}
		/** 01. values.yaml Patch 구성 **/
		/* 1. values.yaml patchMap 구성 */
		Map<String, Object> pmValuesYaml = new HashMap<>();
		pmValuesYaml.put("op", JsonPatchOp.REPLACE.getValue());
		pmValuesYaml.put("path", String.format("/data/%s", AddonKeyItem.VALUE_YAML.getValue()));
		pmValuesYaml.put("value", valuesYaml);
		patchBody.add((JsonUtils.fromGson(JsonUtils.toGson(pmValuesYaml), JsonElement.class)).getAsJsonObject());
		/* 2. 이전 values.yaml patchMap 구성 */
		if(StringUtils.isNotBlank(currAddon.getData().get(AddonKeyItem.VALUE_YAML.getValue()))) {
			Map<String, Object> pmValuesYamlPrev = new HashMap<>();
			pmValuesYamlPrev.put("op", JsonPatchOp.REPLACE.getValue());
			if (!currAddon.getData().containsKey(AddonKeyItem.VALUE_YAML_PREV.getValue())) {
				pmValuesYamlPrev.put("op", JsonPatchOp.ADD.getValue());
			}
			pmValuesYamlPrev.put("path", String.format("/data/%s", AddonKeyItem.VALUE_YAML_PREV.getValue()));
			pmValuesYamlPrev.put("value", StringUtils.trim(currAddon.getData().get(AddonKeyItem.VALUE_YAML.getValue())));
			patchBody.add((JsonUtils.fromGson(JsonUtils.toGson(pmValuesYamlPrev), JsonElement.class)).getAsJsonObject());
		}

		/** 02. addon.toml Patch 구성 **/
		/* 1. addon.toml patchMap 구성 */
		Map<String, Object> pmAddonToml = new HashMap<>();
		pmAddonToml.put("op", JsonPatchOp.REPLACE.getValue());
		if (!currAddon.getData().containsKey(AddonKeyItem.ADDON_TOML.getValue())) {
			pmAddonToml.put("op", JsonPatchOp.ADD.getValue());
		}
		pmAddonToml.put("path", String.format("/data/%s", AddonKeyItem.ADDON_TOML.getValue()));
		pmAddonToml.put("value", Base64Utils.encodeToString(org.apache.commons.codec.binary.StringUtils.getBytesUtf8(StringUtils.defaultString(addonUpgrade.getAddonToml()))));
		patchBody.add((JsonUtils.fromGson(JsonUtils.toGson(pmAddonToml), JsonElement.class)).getAsJsonObject());

		/* 2. 이전 addon.toml patchMap 구성 */
		if(StringUtils.isNotBlank(currAddon.getData().get(AddonKeyItem.ADDON_TOML.getValue()))) {
			Map<String, Object> pmAddonTomlPrev = new HashMap<>();
			pmAddonTomlPrev.put("op", JsonPatchOp.REPLACE.getValue());
			if (!currAddon.getData().containsKey(AddonKeyItem.ADDON_TOML_PREV.getValue())) {
				pmAddonTomlPrev.put("op", JsonPatchOp.ADD.getValue());
			}
			pmAddonTomlPrev.put("path", String.format("/data/%s", AddonKeyItem.ADDON_TOML_PREV.getValue()));
			pmAddonTomlPrev.put("value", currAddon.getData().get(AddonKeyItem.ADDON_TOML.getValue()));
			patchBody.add((JsonUtils.fromGson(JsonUtils.toGson(pmAddonTomlPrev), JsonElement.class)).getAsJsonObject());
		}

		/** 03. addon.yaml Patch 구성 **/
		/* 1. addon.yaml patchMap 구성 */
		Map<String, Object> pmAddonYaml = new HashMap<>();
		pmAddonYaml.put("op", JsonPatchOp.REPLACE.getValue());
		if (!currAddon.getData().containsKey(AddonKeyItem.ADDON_YAML.getValue())) {
			pmAddonYaml.put("op", JsonPatchOp.ADD.getValue());
		}
		pmAddonYaml.put("path", String.format("/data/%s", AddonKeyItem.ADDON_YAML.getValue()));
		pmAddonYaml.put("value", addonUpgrade.getAddonYaml());
		patchBody.add((JsonUtils.fromGson(JsonUtils.toGson(pmAddonYaml), JsonElement.class)).getAsJsonObject());

		/* 2. 이전 addon.yaml patchMap 구성 */
		if(StringUtils.isNotBlank(currAddon.getData().get(AddonKeyItem.ADDON_YAML.getValue()))) {
			Map<String, Object> pmAddonYamlPrev = new HashMap<>();
			pmAddonYamlPrev.put("op", JsonPatchOp.REPLACE.getValue());
			if (!currAddon.getData().containsKey(AddonKeyItem.ADDON_YAML_PREV.getValue())) {
				pmAddonYamlPrev.put("op", JsonPatchOp.ADD.getValue());
			}
			pmAddonYamlPrev.put("path", String.format("/data/%s", AddonKeyItem.ADDON_YAML_PREV.getValue()));
			pmAddonYamlPrev.put("value", currAddon.getData().get(AddonKeyItem.ADDON_YAML.getValue()));
			patchBody.add((JsonUtils.fromGson(JsonUtils.toGson(pmAddonYamlPrev), JsonElement.class)).getAsJsonObject());
		}

		/** 04. version Patch 구성 **/
		/* 1. version patchMap 구성 */
		Map<String, Object> pmVersion = new HashMap<>();
		pmVersion.put("op", JsonPatchOp.REPLACE.getValue());
		pmVersion.put("path", String.format("/data/%s", AddonKeyItem.VERSION.getValue()));
		pmVersion.put("value", addonUpgrade.getVersion());
		patchBody.add((JsonUtils.fromGson(JsonUtils.toGson(pmVersion), JsonElement.class)).getAsJsonObject());
		/* 2. 이전 version patchMap 구성 */
		if(StringUtils.isNotBlank(currAddon.getData().get(AddonKeyItem.VERSION.getValue()))) {
			Map<String, Object> pmVersionPrev = new HashMap<>();
			pmVersionPrev.put("op", JsonPatchOp.REPLACE.getValue());
			if (!currAddon.getData().containsKey(AddonKeyItem.VERSION_PREV.getValue())) {
				pmVersionPrev.put("op", JsonPatchOp.ADD.getValue());
			}
			pmVersionPrev.put("path", String.format("/data/%s", AddonKeyItem.VERSION_PREV.getValue()));
			pmVersionPrev.put("value", StringUtils.trim(currAddon.getData().get(AddonKeyItem.VERSION.getValue())));
			patchBody.add((JsonUtils.fromGson(JsonUtils.toGson(pmVersionPrev), JsonElement.class)).getAsJsonObject());
		}

		/** 05. Kiali 접속을 위한 인증서 정보가 없을 경우 추가 구성 (기존 배포에서 Upgrade시 대응) **/
		/* 1. CaCrt Data가 없고, 현재 입력 가능한 값이 있으면 입력 */
		if(StringUtils.isBlank(currAddon.getData().get(AddonKeyItem.KIALI_CA_CRT.getValue())) &&
			StringUtils.isNotBlank(dynamicValues.get(AddonDynamicValueType.ISTIO_KIALI_TLS_CA_CRT.getValue()))) {
			Map<String, Object> pmCaCrt = new HashMap<>();
			pmCaCrt.put("op", JsonPatchOp.ADD.getValue());
			pmCaCrt.put("path", String.format("/data/%s", AddonKeyItem.KIALI_CA_CRT.getValue()));
			pmCaCrt.put("value", dynamicValues.get(AddonDynamicValueType.ISTIO_KIALI_TLS_CA_CRT.getValue()));
			patchBody.add((JsonUtils.fromGson(JsonUtils.toGson(pmCaCrt), JsonElement.class)).getAsJsonObject());
		}

		if(StringUtils.isBlank(currAddon.getData().get(AddonKeyItem.KIALI_CA_PUBLIC.getValue())) &&
			StringUtils.isNotBlank(dynamicValues.get(AddonDynamicValueType.ISTIO_KIALI_TLS_CA_PUBLIC.getValue()))) {
			Map<String, Object> pmCaPublic = new HashMap<>();
			pmCaPublic.put("op", JsonPatchOp.ADD.getValue());
			pmCaPublic.put("path", String.format("/data/%s", AddonKeyItem.KIALI_CA_PUBLIC.getValue()));
			pmCaPublic.put("value", dynamicValues.get(AddonDynamicValueType.ISTIO_KIALI_TLS_CA_PUBLIC.getValue()));
			patchBody.add((JsonUtils.fromGson(JsonUtils.toGson(pmCaPublic), JsonElement.class)).getAsJsonObject());
		}

		if(StringUtils.isBlank(currAddon.getData().get(AddonKeyItem.KIALI_CA_PRVATE.getValue())) &&
			StringUtils.isNotBlank(dynamicValues.get(AddonDynamicValueType.ISTIO_KIALI_TLS_CA_PRIVATE.getValue()))) {
			Map<String, Object> pmCaPrivate = new HashMap<>();
			pmCaPrivate.put("op", JsonPatchOp.ADD.getValue());
			pmCaPrivate.put("path", String.format("/data/%s", AddonKeyItem.KIALI_CA_PRVATE.getValue()));
			pmCaPrivate.put("value", dynamicValues.get(AddonDynamicValueType.ISTIO_KIALI_TLS_CA_PRIVATE.getValue()));
			patchBody.add((JsonUtils.fromGson(JsonUtils.toGson(pmCaPrivate), JsonElement.class)).getAsJsonObject());
		}

		if(StringUtils.isNotBlank(dynamicValues.get(AddonDynamicValueType.ISTIO_KIALI_TLS_ADDRESS_LIST.getValue()))) {
			Map<String, Object> pmCaAddress = new HashMap<>();
			pmCaAddress.put("op", JsonPatchOp.REPLACE.getValue());
			if (!currAddon.getData().containsKey(AddonKeyItem.KIALI_ADDRESS_LIST.getValue())) {
				pmCaAddress.put("op", JsonPatchOp.ADD.getValue());
			}
			pmCaAddress.put("path", String.format("/data/%s", AddonKeyItem.KIALI_ADDRESS_LIST.getValue()));
			pmCaAddress.put("value", dynamicValues.get(AddonDynamicValueType.ISTIO_KIALI_TLS_ADDRESS_LIST.getValue()));
			patchBody.add((JsonUtils.fromGson(JsonUtils.toGson(pmCaAddress), JsonElement.class)).getAsJsonObject());
		}

		return patchBody;
	}

	public boolean getSupported(Integer clusterSeq, String addonName) throws Exception {
		boolean isSupported = false;

		if (clusterSeq != null && StringUtils.isNotBlank(addonName)) {
			IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
			ClusterVO cluster =  clusterDao.getCluster(clusterSeq);
			isSupported = this.getSupported(cluster, addonName);
		}

		return isSupported;
	}

	public boolean getSupportedByServicemap(Integer servicemapSeq, String addonName) throws Exception {
		boolean isSupported = false;

		if (servicemapSeq != null && StringUtils.isNotBlank(addonName)) {
			ClusterVO cluster =  k8sResourceService.setupCluster(servicemapSeq);
			isSupported = this.getSupported(cluster, addonName);
		}

		return isSupported;
	}

	public boolean getSupported(ClusterVO cluster, String addonName) throws Exception {
		boolean isSupported = false;

		if (cluster != null && StringUtils.isNotBlank(addonName)) {
			String labels = String.format("%s=%s", KubeConstants.LABELS_ADDON_CHART_KEY, addonName);
			List<ConfigMapGuiVO> addons = this.getAddonConfigMaps(cluster, labels);

			if (CollectionUtils.isNotEmpty(addons)) {
				isSupported = addons.stream().filter(a -> (BooleanUtils.toBoolean(a.getData().get(AddonKeyItem.USE_YN.getValue())))).findFirst().isPresent();
			}
		}

		return isSupported;
	}

	/**
	 * addon configMap 조회
	 *
	 * @param clusterSeq
	 * @return
	 * @throws Exception
	 */
	public List<ConfigMapGuiVO> getAddonConfigMaps(Integer clusterSeq, String labels, String useYn) throws Exception {
		return this.getAddonConfigMaps(clusterSeq, labels, useYn, false, false);
	}

	public List<ConfigMapGuiVO> getAddonConfigMaps(Integer clusterSeq, String labels, String useYn, boolean useResource, boolean useIngress) throws Exception {

		List<ConfigMapGuiVO> addons = new ArrayList<>();

		if (clusterSeq != null) {
			IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
			ClusterVO cluster =  clusterDao.getCluster(clusterSeq);

			if (cluster != null) {
				addons = this.getAddonConfigMaps(cluster, labels, useYn, useResource, useIngress);
			}
		}

		return addons;
	}

	/**
	 * addon configMap 조회
	 *
	 * @param clusterId
	 * @param labels
	 * @return
	 * @throws Exception
	 */
	public List<ConfigMapGuiVO> getAddonConfigMaps(String clusterId, String labels, String useYn) throws Exception {

		List<ConfigMapGuiVO> addons = new ArrayList<>();

		if (StringUtils.isNotBlank(clusterId)) {
			IClusterMapper clusterDao = sqlSession.getMapper(IClusterMapper.class);
			ClusterVO cluster =  clusterDao.getClusterByClusterId(clusterId, "Y");

			if (cluster != null) {
				addons = this.getAddonConfigMaps(cluster, labels, useYn);
			}
		}

		return addons;
	}

	public List<ConfigMapGuiVO> getAddonConfigMapsByServicemap(Integer servicemapSeq, String labels, String useYn) throws Exception {

		List<ConfigMapGuiVO> addons = new ArrayList<>();

		if (servicemapSeq != null) {
			ClusterVO cluster =  k8sResourceService.setupCluster(servicemapSeq);

			if (cluster != null) {
				addons = this.getAddonConfigMaps(cluster, labels, useYn);
			}
		}

		return addons;
	}

	public List<ConfigMapGuiVO> getAddonConfigMaps(Integer clusterSeq) throws Exception {
		return this.getAddonConfigMaps(clusterSeq, null, null);
	}

	public List<ConfigMapGuiVO> getAddonConfigMaps(ClusterVO cluster) throws Exception {
		return this.getAddonConfigMaps(cluster, null, null);
	}

	public List<ConfigMapGuiVO> getAddonConfigMaps(Integer clusterSeq, String labels) throws Exception {
		return this.getAddonConfigMaps(clusterSeq, labels, null);
	}

	public List<ConfigMapGuiVO> getAddonConfigMaps(ClusterVO cluster, String labels) throws Exception {
		return this.getAddonConfigMaps(cluster, labels, null);
	}

	public List<ConfigMapGuiVO> getAddonConfigMaps(ClusterVO cluster, String labels, String useYn) throws Exception {
		return this.getAddonConfigMaps(cluster, labels, useYn, false, false);
	}

	public List<ConfigMapGuiVO> getAddonConfigMaps(ClusterVO cluster, String labels, String useYn, boolean useResource, boolean useIngress) throws Exception {

		/**
		 * 기본 add-on Labels
		 *
		 * agent: agent
		 * chart: xxx
		 * release-id: xxx
		 *
		 */
		String labelSelectors = String.format("%s=%s,%s,%s", KubeConstants.LABELS_ADDON_AGENT_KEY, KubeConstants.LABELS_ADDON_AGENT_KEY, KubeConstants.LABELS_ADDON_CHART_KEY, KubeConstants.LABELS_HELM_CHART_KEY);
		if (StringUtils.isNotBlank(labels)) {
			labelSelectors = String.format("%s,%s", labelSelectors, labels);
		}

		List<ConfigMapGuiVO> addons = configMapService.getConfigMaps(cluster, this.getCocktailAddonNamespace(), null, labelSelectors);

		if (CollectionUtils.isNotEmpty(addons)) {
			// cluster에서 생성된 configMap 일 경우 resourcePrefix와 맞지 않는 것은 삭제 처리
			addons.removeIf(a -> (
				MapUtils.isNotEmpty(a.getLabels())
					&& (
					// 다른 cocktail addon은 목록에서 제거
					(
						StringUtils.isNotBlank(MapUtils.getString(a.getLabels(), KubeConstants.LABELS_COCKTAIL_KEY))
							&& !StringUtils.equals(MapUtils.getString(a.getLabels(), KubeConstants.LABELS_COCKTAIL_KEY), ResourceUtil.getResourcePrefix())
					)
						||
						// 다른 칵테일의 모니터링 addon은 목록에서 제거
						(
							StringUtils.startsWith(a.getName(), cocktailAddonProperties.getMonitoringAgentConfigmapPrefix())
								&& !StringUtils.equalsIgnoreCase(a.getName(), String.format("%s-%s-%d", cocktailAddonProperties.getMonitoringAgentConfigmapPrefix(), ResourceUtil.getResourcePrefix(), cluster.getClusterSeq()))
						)
						||
						// useYn 값과 다른 addon은 목록에서 제거
						(
							StringUtils.isNotBlank(useYn)
								&& !StringUtils.equalsIgnoreCase(a.getData().get(AddonKeyItem.USE_YN.getValue()), useYn)
						)
				)
			));

			for (ConfigMapGuiVO addonRow : addons) {
				/** Resource 목록에 대한 응답 요청 : Manifest를 Parsing 하여 리소스 목록으로 저장 **/
				if(useResource) {
					try {
						String base64EncodedString = Optional.ofNullable(addonRow.getData()).orElseGet(() ->Maps.newHashMap()).get(KubeConstants.ADDON_MANIFEST_KEY);
						String manifestString = Utils.decompressGZipFromString(base64EncodedString, true, "UTF-8"); // Base64 Encode + Gzip Data를 Manifest String으로 변환.
						String namespace = Optional.ofNullable(addonRow.getData()).orElseGet(() ->Maps.newHashMap()).get(KubeConstants.ADDON_NAMESPACE_KEY);
						HelmResourcesVO helmResources = packageK8sService.getHelmResourcesFromManifest(manifestString, cluster.getClusterSeq(), null, namespace, null);
						addonRow.setResources(helmResources);
					}
					catch (Exception ex) {
						// 오류시 log 남기도 다른 데이터는 정상 응답..
						log.error("Get Addon Resource failure. : " + addonRow.getNamespace() + "/" + addonRow.getName());
					}

				}

				/** nginx-ingress Addon일 경우 Ingress 정보를 요청하면 해당 Addon의 Ingress 목록을 응답 **/
				if(StringUtils.equalsIgnoreCase(Optional.ofNullable(addonRow.getLabels()).orElseGet(() ->Maps.newHashMap()).get(KubeConstants.LABELS_ADDON_CHART_KEY), "nginx-ingress") && useIngress) {
					String namespace = Optional.ofNullable(addonRow.getData()).orElseGet(() ->Maps.newHashMap()).get(KubeConstants.ADDON_NAMESPACE_KEY);
					String field = null;
					String label = null;
					List<K8sIngressVO> ingresses = ingressSpecService.getIngresses(cluster, field, label, ContextHolder.exeContext());
					List<K8sIngressVO> ingressList = new ArrayList<>();
					for(K8sIngressVO ingress : ingresses) {
						if(StringUtils.equalsIgnoreCase(ingress.getIngressSpec().getIngressControllerName(), addonRow.getName())) {
							ingressList.add(ingress);
						}
					}

					if(CollectionUtils.isNotEmpty(ingressList)) {
						addonRow.setIngresses(ingressList);
					}
				}

				addonRow.getData().remove(AddonKeyItem.VALUE_YAML.getValue());
				addonRow.getData().remove(AddonKeyItem.VALUE_YAML_PREV.getValue());
				addonRow.getData().remove(AddonKeyItem.ADDON_TOML.getValue());
				addonRow.getData().remove(AddonKeyItem.ADDON_TOML_PREV.getValue());
				addonRow.getData().remove(AddonKeyItem.MANIFEST.getValue());
				addonRow.getData().remove(AddonKeyItem.KIALI_CA_PUBLIC.getValue());
				addonRow.getData().remove(AddonKeyItem.KIALI_CA_PRVATE.getValue());
				addonRow.getData().remove(AddonKeyItem.KIALI_CA_CRT.getValue());
				addonRow.getData().remove(AddonKeyItem.KIALI_ADDRESS_LIST.getValue());
				addonRow.setDeployment(null);
				addonRow.setDeploymentYaml(null);

				addonRow.getData().put(AddonKeyItem.REPOSITORY.getValue(), cocktailAddonProperties.getAddonChartRepoProjectName());
			}
		}

		return addons;
	}

	public String getCocktailAddonNamespace() throws Exception {
		return cocktailAddonProperties.getAddonNamespace();
	}
}