package run.acloud.api.resource.util;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import io.kubernetes.client.custom.Quantity;
import io.kubernetes.client.custom.QuantityFormatter;
import io.kubernetes.client.openapi.JSON;
import io.kubernetes.client.openapi.models.V1LabelSelector;
import io.kubernetes.client.openapi.models.V1ObjectReference;
import io.kubernetes.client.openapi.models.V1OwnerReference;
import io.kubernetes.client.openapi.models.V1Service;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import run.acloud.api.code.service.CodeService;
import run.acloud.api.code.vo.CodeVO;
import run.acloud.api.configuration.enums.IssueType;
import run.acloud.api.configuration.vo.ClusterVO;
import run.acloud.api.cserver.enums.VolumeType;
import run.acloud.api.cserver.enums.WorkloadType;
import run.acloud.api.resource.constants.KubeConstants;
import run.acloud.api.resource.enums.*;
import run.acloud.api.resource.vo.*;
import run.acloud.commons.service.RegistryPropertyService;
import run.acloud.commons.util.Base64Utils;
import run.acloud.commons.util.Utils;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Component
public final class ResourceUtil {
	private static CodeService codeService;
	private static RegistryPropertyService registryProperties;

	@Autowired
	private CodeService injectedCodeService;

	@Autowired
	private RegistryPropertyService injectedRegistryProperties;

	@PostConstruct
	public void init() {
		ResourceUtil.codeService = injectedCodeService;
		ResourceUtil.registryProperties = injectedRegistryProperties;
	}

	private final static String COCKTAIL_ID_FORMAT = "%s-%s-%06d";
	private final static String COCKTAIL_ID_DELIMITER = "-";
	public final static String COCKTAIL_PV_NAME_FORMAT = "pv-%s-%s-%s-%s";
	public final static String COCKTAIL_PVC_NAME_FORMAT = "pvc-%s-%s-%s-%s";
	public final static String COCKTAIL_CUSTOM_PV_NAME_FORMAT = "pv-%s-%s";

	public static String getFormattedName(String name, int idx) {
		return idx == -1 ? name : String.format("%s-%02d", name, idx);
	}

	public static String getUniqueName(String id) {
		if (id == null)
			return null;
		return id.replaceAll("[^0-9a-zA-Z-]", "").toLowerCase();
	}

	public static String getUniqueId(String id) {
//		return getUniqueName(jp, "component.componentId");
		return getUniqueName(id);
	}

	public static String getUniqueName(String id, Integer seq) {
		return getUniqueName(id + '-' + seq);
	}

	public static String getFormattedPVLabelName() {
		return String.format("%s-%d", ResourceUtil.getResourcePrefix(), 0);
	}

	public static String makePortName() {
		return String.format("%s-%s", "port", RandomStringUtils.randomAlphabetic(4).toLowerCase());
	}

	public static String makeHeadlessServiceName(String serverName) {
		return String.format("%s-%s", serverName, "headless");
	}

	public static String makeCocktailId(ComponentType componentType, Integer seq) {
		if (seq == null) {
			return null;
		} else {
			CodeVO code = codeService.getCodeResourcePrefix();
			return String.format(COCKTAIL_ID_FORMAT, code.getValue().toLowerCase(), componentType.getResourceCode(), seq);
		}
	}

	public static String makeCocktailId(ResourceType resourceType, Integer seq) {
		if (seq == null) {
			return null;
		} else {
			CodeVO code = codeService.getCodeResourcePrefix();
			return String.format(COCKTAIL_ID_FORMAT, code.getValue().toLowerCase(), resourceType.getCode(), seq);
		}
	}

	public static int getSeqFromCocktailId(String id) {
		String[] vals = id.split(COCKTAIL_ID_DELIMITER);
		return Integer.valueOf(vals[vals.length - 1]);
	}

	public static String makeNamespaceName(Integer servicemapSeq, Integer clusterSeq) {
		if (servicemapSeq == null || clusterSeq == null) {
			return null;
		}
		CodeVO code = codeService.getCodeResourcePrefix();
		return String.format("%s-%d-%d", code.getValue().toLowerCase(), servicemapSeq, clusterSeq);
	}

	public static String getResourcePrefix() {
		String reasourcePrefix = "";
		CodeVO code = codeService.getCodeResourcePrefix();

		if (code != null) {
			reasourcePrefix = code.getValue().toLowerCase();
		}

		return reasourcePrefix;
	}

	public static String makePersistentVolumeName(String format, String storageName, String serverName, String namespaceName) {
		return String.format(format, StringUtils.deleteWhitespace(storageName.trim()).toLowerCase(), serverName, namespaceName, RandomStringUtils.random(4, true, false).toLowerCase());
	}

	public static String makePersistentVolumeName(String format, String persistentName) {
		return String.format(format, StringUtils.deleteWhitespace(persistentName.trim()).toLowerCase(), RandomStringUtils.random(4, true, false).toLowerCase());
	}

	public static boolean validNamespaceName(String namespaceName) {
		boolean isValid = false;

		if (StringUtils.isNotBlank(namespaceName)) {
			if (namespaceName.matches("^[a-z0-9]([-a-z0-9]*[a-z0-9])?$")) {
				isValid = true;
			}
		}

		return isValid;
	}

	public static String makeRegistryPullUserId(String prefix) {
		return String.format("%s-cocktail-%s", StringUtils.lowerCase(StringUtils.defaultIfBlank(prefix, "default")), Utils.shortUUID().toLowerCase());
	}

	public static String makeRegistryUserId() {
		return String.format("%s%s", getRegistryUserIdPrefix(), Utils.shortUUID().toLowerCase());
	}

	public static String getRegistryUserIdPrefix() {
		return "user-";
	}

	public static String makeRegistryUserPassword() {
		return String.format("%s%s%s%s",
				RandomStringUtils.randomAlphanumeric(3).toLowerCase(),
				RandomStringUtils.randomAlphabetic(3).toUpperCase(),
				RandomStringUtils.randomNumeric(3),
				Utils.shortUUID());
	}

	public static String makeDockerRegistrySecretName(String registryUrl, String registryName) {
		return String.format("docker-%s-%s", StringUtils.replaceAll(ResourceUtil.getRegistryUrl(registryUrl), "\\:", "\\."), StringUtils.replaceAll(registryName, "\\_", "\\-"));
	}

	public static String makeStaticStorageConfigMapName(String storageName) {
		return String.format("%s-%s", KubeConstants.CUSTOM_STATIC_STORAGE_CONFIGMAP_PREFIX_NAME, storageName);
	}

	public static String removeStaticStorageConfigMapNamePrefix(String storageName) {
		return StringUtils.removeStart(storageName, String.format("%s-", KubeConstants.CUSTOM_STATIC_STORAGE_CONFIGMAP_PREFIX_NAME));
	}

	public static String makeAWSUsername(String userId) {
		return String.format("%s-cocktail-%s", userId, RandomStringUtils.randomAlphabetic(4).toLowerCase());
	}

	public static String makeIssueAccountName(String userId, IssueType issueType) {
		// issueType + replace(email 계정명, ‘이메일가능특수문자’, ‘’) + ‘-’ + ‘-’ + rnd(4)
		return String.format("%s-%s-%s"
				, issueType.getCode()
				, StringUtils.replaceAll( StringUtils.left(StringUtils.substringBefore(userId, "@"), 10), "[-~!$%^&*_=+}{\\'?\\.]+(\\.[-~!$%^&*_=+}{\\'?\\.]+)*", "" )
				, RandomStringUtils.randomAlphabetic(8)).toLowerCase();
	}

//	public static long convertResourceAmount(String value) {
////      log.debug("value: {}", value);
//		try {
//			if (StringUtils.endsWithIgnoreCase(value, "m")) {
//				return Long.parseLong(value.substring(0, StringUtils.indexOfIgnoreCase(value, "m")));
//			} else if (StringUtils.endsWithIgnoreCase(value, "Mi")) {
//				return Long.parseLong(value.substring(0, StringUtils.indexOfIgnoreCase(value, "M"))) * 1024 * 1024;
//			} else if (StringUtils.endsWithIgnoreCase(value, "Gi")) {
//				return Long.parseLong(value.substring(0, StringUtils.indexOfIgnoreCase(value, "G"))) * 1024 * 1024 * 1024;
//			} else if (StringUtils.endsWithIgnoreCase(value, "Ki")) {
//				return Long.parseLong(value.substring(0, StringUtils.indexOfIgnoreCase(value, "K"))) * 1024;
//			} else if (StringUtils.isNumeric(value)) { // cpu
//				return Long.parseLong(value) * 1000;
//			} else {
//				log.debug("Unknown format: {}", value);
//				return 0L;
//			}
//		} catch (Exception eo) {
//			log.error("Unknown format: {}", value, eo);
//			return 0L;
//		}
//	}

	public static String getRegistryUrl() {
		return getRegistryUrl(registryProperties.getUrl());
	}

	public static String getRegistryUrl(String registryUrl) {
		if (StringUtils.isNotBlank(registryUrl)) {
			registryUrl = StringUtils.removeEnd(StringUtils.substringAfter(registryUrl, "//"), "/");
			if (StringUtils.contains(registryUrl, ":")) {
				String port = StringUtils.substringAfterLast(registryUrl, ":");
				if (StringUtils.equalsAny(port, "80", "443")) {
					registryUrl = StringUtils.substringBefore(registryUrl, ":");
				}
			}
		}
		return registryUrl;
	}

	public static void mergeContainer(List<ContainerVO> allContainers, List<ContainerVO> initContainers, List<ContainerVO> containers) {
		if (allContainers == null) {
			allContainers = new ArrayList<>();
		}

		if (CollectionUtils.isNotEmpty(initContainers)) {
			allContainers.addAll(initContainers);
		}
		if (CollectionUtils.isNotEmpty(containers)) {
			allContainers.addAll(containers);
		}
	}

	public static void mergeK8sContainer(List<K8sContainerVO> allContainers, List<K8sContainerVO> initContainers, List<K8sContainerVO> containers) {
		if (allContainers == null) {
			allContainers = new ArrayList<>();
		}

		if (CollectionUtils.isNotEmpty(initContainers)) {
			allContainers.addAll(initContainers);
		}
		if (CollectionUtils.isNotEmpty(containers)) {
			allContainers.addAll(containers);
		}
	}

	public static boolean isDeployment(WorkloadType workloadType) {
		if (workloadType == WorkloadType.SINGLE_SERVER || workloadType == WorkloadType.REPLICA_SERVER) {
			return true;
		}

		return false;
	}

	public static boolean isPossibleAutoscaling(WorkloadType workloadType) {
		if (workloadType == WorkloadType.SINGLE_SERVER || workloadType == WorkloadType.REPLICA_SERVER || workloadType == WorkloadType.STATEFUL_SET_SERVER) {
			return true;
		}

		return false;
	}

	public static String getPvcLabelSelector() {
		return String.format("%s,%s=%s", KubeConstants.LABELS_COCKTAIL_KEY, KubeConstants.CUSTOM_VOLUME_TYPE, VolumeType.PERSISTENT_VOLUME_LINKED.getCode());
	}

	public static String getMatchVersion(String k8sVersion) {

		StringBuffer sb = new StringBuffer();

		if (StringUtils.isNotBlank(k8sVersion)) {
			Pattern pattern = Pattern.compile("([1-9]\\d*)+.(\\d+)+.(\\d+)*"); // x.x.x (숫자만)
			Matcher matcher = pattern.matcher(k8sVersion);

			// Find all matches
			while (matcher.find()) {
				// Get the matching string
				sb.append(matcher.group());
			}
		}

		return sb.toString();
	}

	public static boolean getK8sSupported(String k8sVersion, List<String> supportedVersions) {
		boolean supported = false;
		if (StringUtils.isNotBlank(k8sVersion) && CollectionUtils.isNotEmpty(supportedVersions)) {
			String k8sSemVersion = ResourceUtil.getMatchVersion(k8sVersion);
			String[] apiVersions = supportedVersions.toArray(new String[supportedVersions.size()]);
			if (StringUtils.startsWithAny(k8sSemVersion, apiVersions)
					|| StringUtils.containsAny(k8sSemVersion, apiVersions)) {
				supported = true;
			}
		}

		return supported;
	}

	/**
	 * Container Port Name Rule 체크
	 * 1. Pod의 Container Port Naming Rule (IANA_SVC_NAME) 준수 여부
	 * 체크 대상
	 * 1. V1ContainerPort.name
	 *
	 * @param port
	 * @param isThrows
	 * @return
	 * @throws Exception
	 */
	public static Pair<Boolean, ExceptionType> isValidContainerPortNameRule(String port, boolean isThrows) throws Exception {
		ExceptionType exceptionType = null;
		String errorMessage = "";

		// 1. IANA_SVC_NAME Rule 준수 여부 확인 1단계 : 문자열 구성 확인 [a-z0-9-]
		if (!Utils.isIanaSvcName(port)) {
			exceptionType = ExceptionType.ServerPortInvalid;
			errorMessage = String.format("The target of the port is the port name (%s) : The port name does not comply with IANA_SVC_NAME.", port);
		}
		// 2. IANA_SVC_NAME Rule 준수 여부 확인 2단계 : [a-z]을 반드시 하나 이상 포함
		if (!Utils.isContainLetterWithIanaCharactersOnly(port)) {
			exceptionType = ExceptionType.ServerPortInvalid;
			errorMessage = String.format("The target of the port is the port name (%s) : The port name must contain one lowercase letter.", port);
		}
		// 3. 연속된 하이픈이 존재하는지 체크 (연속된 하이픈 불허.)
		if (port.contains("--")) {
			exceptionType = ExceptionType.ServerPortInvalid;
			errorMessage = String.format("The target of the port is the port name (%s) : Must not contain consecutive hyphens", port);
		}
		// 4. 15글자 이상인지 체크 (15글자 이하)
		if (port.length() > 15) {
			exceptionType = ExceptionType.ServerPortInvalid;
			errorMessage = String.format("The target of the port is the port name (%s) : Must be no more than 15 characters", port);
		}

		/** 오류 없음  */
		if (exceptionType == null) {
			return Pair.of(Boolean.TRUE, exceptionType);
		}

		/** 오류 있음 */
		if (isThrows) {
			throw new CocktailException(errorMessage, exceptionType);
		}

		return Pair.of(Boolean.FALSE, exceptionType);
	}

	/**
	 * Container Port Name Rule 체크.
	 * 기본적으로 즉시 throws 하여 예외 처리.
	 * 필요시 exceptionType을 리턴 받아 사용 가능.. (Key값이 TRUE면 정상이므로 exceptionType == null)
	 *
	 * @param port
	 * @return
	 * @throws Exception
	 */
	public static Pair<Boolean, ExceptionType> isValidContainerPortNameRule(String port) throws Exception {
		return ResourceUtil.isValidContainerPortNameRule(port, true);
	}

	/**
	 * Port Validation 체크 : Port에 입력 가능한 값은 Port 번호 or Pod의 Container Port Name 두가지임.
	 * Case 1 : Pod의 Container Port Name일 경우 Naming Rule은 IANA_SVC_NAME 표준을 준수 하여야 함.
	 * Case 2 : Port 번호일 경우 : 1 ~ 5자리 숫자로만 이루어진 포트 인지 체크 and Port Range 체크 : 1 ~ 65535
	 * 체크 대상
	 * 1. V1ServicePort.targetPort
	 * 2. V1HTTPGetAction.port
	 * 3. V1TCPSocketAction.port
	 *
	 * @param port
	 * @param isThrows
	 * @return
	 * @throws Exception
	 */
	public static Pair<Boolean, ExceptionType> isValidPortRule(String port, boolean isThrows) throws Exception {
		ExceptionType exceptionType = null;
		String errorMessage = "";

		/**
		 * CASE A : 숫자로만 구성 = 포트 번호 입력
		 */
		if (NumberUtils.isDigits(port)) {
			// 1. 숫자로 변환
			Integer iPort = Integer.parseInt(port);
			// 2. 숫자 Range 확인 (1-65535)
			if (!(iPort >= 1 && iPort <= 65535)) {
				exceptionType = ExceptionType.ServerPortRangeInvalid;
				errorMessage = String.format("The target of the port is the port number (%s) : The port is out of range for 1-65535", port);
			}
		}
		/**
		 * CASE B : 문자가 포함되어 있음 = ContainerPortName
		 * IANA_SVC_NAME : at most 15 characters, matching regex [a-z0-9]([a-z0-9-]*[a-z0-9])*, it must contain at least one letter [a-z], and hyphens cannot be adjacent to other hyphens : e.g. "http"
		 */
		else {
			Pair<Boolean, ExceptionType> result = ResourceUtil.isValidContainerPortNameRule(port, isThrows);
			exceptionType = result.getValue();
		}

		/** 오류 없음  */
		if (exceptionType == null) {
			return Pair.of(Boolean.TRUE, exceptionType);
		}

		/** 오류 있음 */
		if (isThrows) {
			throw new CocktailException(errorMessage, exceptionType);
		}

		return Pair.of(Boolean.FALSE, exceptionType);
	}


	/**
	 * Port Validation 체크.
	 * 기본적으로 즉시 throws 하여 예외 처리.
	 * 필요시 exceptionType을 리턴 받아 사용 가능.. (Key값이 TRUE면 정상이므로 exceptionType == null)
	 *
	 * @param port
	 * @return
	 * @throws Exception
	 */
	public static Pair<Boolean, ExceptionType> isValidPortRule(String port) throws Exception {
		return ResourceUtil.isValidPortRule(port, true);
	}

	public static K8sApiType getApiType(ClusterVO cluster, K8sApiKindType kindType) throws Exception {
		K8sApiType apiType = null;
		K8sApiVerKindType apiVerKindType = getApiVerKindType(cluster, kindType);
		if (apiVerKindType != null) {
			apiType = apiVerKindType.getApiType();
		}

		return apiType;
	}

	public static K8sApiVerKindType getApiVerKindType(ClusterVO cluster, K8sApiKindType kindType) throws Exception {
		K8sApiVerKindType apiVerKindType = null;
		if (StringUtils.isNotBlank(cluster.getK8sVersion())) {
			String k8sMinorVer = getSemMinorVer(cluster.getK8sVersion());
			apiVerKindType = K8sApiVerKindType.getApiType(kindType, K8sApiVerType.getApiVerType(k8sMinorVer));
		}

		return apiVerKindType;
	}

	public static String getSemMinorVer(String version) {
		String semVersion = getMatchVersion(version);
		String semMinorVer = semVersion;
		if (StringUtils.countMatches(semVersion, ".") > 1) {
			String[] arrSemVersion = StringUtils.split(semVersion,".");
			semMinorVer = String.format("%s.%s", arrSemVersion[0], arrSemVersion[1]);
		}
		return semMinorVer;
	}

	public static String getInvalidNameMsg(String msg) {
		String invalidMsg = "a DNS-1123 label must consist of lower case alphanumeric characters or '-', and must start and end with an alphanumeric character";
		if (StringUtils.isNotBlank(msg)) {
			return String.format("%s (%s)", msg, invalidMsg);
		} else {
			return invalidMsg;
		}
	}


	public static String decodeDescription(Map<String, String> annotations) {
		String description = "";

		try {
			if(MapUtils.isNotEmpty(annotations) && StringUtils.isNotBlank(annotations.get(KubeConstants.ANNOTATION_COCKTAIL_USER_DESCRIPTION))) {
				boolean isBase64 = Utils.isBase64Encoded(MapUtils.getString(annotations, KubeConstants.ANNOTATION_COCKTAIL_USER_DESCRIPTION, ""));
				if(isBase64) {
					description = new String(Base64Utils.decodeFromString(MapUtils.getString(annotations, KubeConstants.ANNOTATION_COCKTAIL_USER_DESCRIPTION, "")), "UTF-8");
				}
				else {
					description = MapUtils.getString(annotations, KubeConstants.ANNOTATION_COCKTAIL_USER_DESCRIPTION, "");
				}
			}
		} catch (UnsupportedEncodingException e) {
			CocktailException ce = new CocktailException("Decode description fail!!", e, ExceptionType.InvalidInputData, "Decode description fail!!");
			log.error(ce.getMessage(), ce);
		} finally {
			return description;
		}
	}

	public static boolean isDefault(Map<String, String> labels) throws Exception {
		boolean isDefault = false;

		try {
			if (MapUtils.isNotEmpty(labels) && labels.containsKey(KubeConstants.META_LABELS_APP_MANAGED_BY)) {
				if (StringUtils.equals(labels.get(KubeConstants.META_LABELS_APP_MANAGED_BY), KubeConstants.LABELS_COCKTAIL_KEY)) {
					isDefault = true;
				}
			}
		} catch (Exception e) {
			CocktailException ce = new CocktailException("Default setting failed!!", e, ExceptionType.InvalidInputData, "Default setting failed!!");
			log.error(ce.getMessage(), ce);
		} finally {
			return isDefault;
		}
	}

	public static boolean isDisplayDefaultPsp(Map<String, String> labels) throws Exception {
		boolean isDefault = false;

		try {
			if (MapUtils.isNotEmpty(labels) && labels.containsKey(KubeConstants.LABELS_ACORNSOFT_PSP_DISPLAY_DEFALUT)) {
				if (StringUtils.equals(labels.get(KubeConstants.LABELS_ACORNSOFT_PSP_DISPLAY_DEFALUT), KubeConstants.LABELS_COCKTAIL_KEY)) {
					isDefault = true;
				}
			}
		} catch (Exception e) {
			CocktailException ce = new CocktailException("Display Default psp setting failed!!", e, ExceptionType.InvalidInputData, "Display Default psp setting failed!!");
			log.error(ce.getMessage(), ce);
		} finally {
			return isDefault;
		}
	}

	public static void validQuantityResourceValue(Map<String, String> resourceMap, QuantityFormatter quantityFormatter) throws Exception {
		if (MapUtils.isNotEmpty(resourceMap)) {
			for (Map.Entry<String, String> entry : resourceMap.entrySet()) {
				try {
					quantityFormatter.parse(entry.getValue());
				} catch (Exception e) {
					throw new CocktailException("The resource value is invalid.", e, ExceptionType.InvalidInputData, String.format("%s - %s [%s]", e.getMessage(), entry.getValue(), entry.getKey()) );
				}
			}
		}
	}

	public static Quantity getQuantityResourceValue(String resourceKey, String resourceValue, QuantityFormatter quantityFormatter) throws Exception {
		if (StringUtils.isNotBlank(resourceValue)) {
			try {
				return quantityFormatter.parse(resourceValue);
			} catch (Exception e) {
				throw new CocktailException("The resource value is invalid.", e, ExceptionType.InvalidInputData, String.format("%s - %s [%s]", e.getMessage(), resourceValue, resourceKey) );
			}
		}

		return null;
	}

	/**
	 * k8s 1.13부터 beta feature로 기본 활성화됨.
	 * 2021.11.28 hojae. 1.13 Not supported로 인하여 1.14부터 체크하도록 수정
	 *
	 * @param k8sVersion
	 * @return
	 */
	public static boolean isSupportedDryRun(String k8sVersion) {
		return K8sApiVerType.isK8sVerSupported(k8sVersion, K8sApiVerType.V1_14);
	}

	public static String getDryRun(boolean dryRun) {
		return dryRun ? "All" : null;
	}

	/**
	 * k8s 1.25부터 psp가 remove되어 기능 제거됨
	 *
	 * @param k8sVersion
	 * @return
	 */
	public static boolean isSupportedPsp(String k8sVersion) {
		return !K8sApiVerType.isK8sVerSupported(k8sVersion, K8sApiVerType.V1_25);
	}


	public static List<K8sOwnerReferenceVO> setOwnerReference(List<V1OwnerReference> v1OwnerReferences) throws Exception {
		List<K8sOwnerReferenceVO> ownerReferences = new ArrayList<>();
		if (CollectionUtils.isNotEmpty(v1OwnerReferences)) {
			for (V1OwnerReference v1OwnerReferenceRow : v1OwnerReferences) {
				K8sOwnerReferenceVO ownerReference = new K8sOwnerReferenceVO();
				ownerReference.setApiVersion(v1OwnerReferenceRow.getApiVersion());
				ownerReference.setBlockOwnerDeletion(v1OwnerReferenceRow.getBlockOwnerDeletion());
				ownerReference.setController(v1OwnerReferenceRow.getController());
				ownerReference.setKind(v1OwnerReferenceRow.getKind());
				ownerReference.setName(v1OwnerReferenceRow.getName());
				ownerReference.setUid(v1OwnerReferenceRow.getUid());
				ownerReferences.add(ownerReference);
			}
		}

		return ownerReferences;
	}

	public static K8sObjectReferenceVO setObjectReference(V1ObjectReference v1ObjectReference) throws Exception {
		K8sObjectReferenceVO objectReference = new K8sObjectReferenceVO();
		if (v1ObjectReference != null) {
			objectReference.setApiVersion(v1ObjectReference.getApiVersion());
			objectReference.setFieldPath(v1ObjectReference.getFieldPath());
			objectReference.setKind(v1ObjectReference.getKind());
			objectReference.setName(v1ObjectReference.getName());
			objectReference.setNamespace(v1ObjectReference.getNamespace());
			objectReference.setResourceVersion(v1ObjectReference.getResourceVersion());
			objectReference.setUid(v1ObjectReference.getUid());
		}

		return objectReference;
	}

	/**
	 * Package Type일 경우 Label을 추가로 생성..
	 * @param v1Service
	 * @param componentName
	 * @param controllerName
	 */
	public static void setPackageSelectorAndLabels(V1Service v1Service, String componentName, String controllerName) {
		/**
		 * Component가 Null이 아닌 경우 = labelSelector가 별도로 지정된 경우 = PackageType 워크로드일 경우.
		 * componentSeq Label을 추가로 붙여서 어떤 워크로드에 매핑된 Service인지 구분할 수 있도록 함..
		 * 기존 워크로드는 LabelSelector가 곧 워크로드이름이므로 해당 정보로 워크로드 매핑을 시켰으나 Package Type은 불가능 하여 Label을 추가로 붙여서 처리함..
		 */
		if(v1Service.getMetadata().getLabels() != null) {
			v1Service.getMetadata().getLabels().put(KubeConstants.LABELS_COCKTAIL_WORKLOAD_NAME, componentName);
			v1Service.getMetadata().getLabels().put(KubeConstants.LABELS_COCKTAIL_WORKLOAD_CONTROLLER, controllerName);
		}

		if(v1Service.getSpec().getSelector() != null) {
			v1Service.getSpec().getSelector().put(KubeConstants.LABELS_PACKAGE_RELEASE_KEY, componentName);
		}
	}

	public static String getLabelFilterOfSelector(V1LabelSelector v1LabelSelector) throws Exception {
		if (v1LabelSelector != null) {
			JSON k8sJson = new JSON();
			String selectorJson = k8sJson.serialize(v1LabelSelector);
			return ResourceUtil.getLabelFilterOfSelector(k8sJson.getGson().fromJson(selectorJson, K8sLabelSelectorVO.class));
		}

		return null;
	}

	public static String getLabelFilterOfSelector(K8sLabelSelectorVO labelSelector) throws Exception {
		String labels = null;
		if (labelSelector != null) {
			List<String> labelsList = Lists.newArrayList();
			if (MapUtils.isNotEmpty(labelSelector.getMatchLabels())) {
				labelsList.addAll(labelSelector.getMatchLabels().entrySet().stream()
						.map(entry -> String.format("%s=%s", entry.getKey(), entry.getValue()))
						.collect(Collectors.toList()));
			}
			if (CollectionUtils.isNotEmpty(labelSelector.getMatchExpressions())) {
				for (K8sLabelSelectorRequirementVO lsrRow : labelSelector.getMatchExpressions()) {
					switch (lsrRow.getOperator()) {
						case "In":
							labelsList.add(String.format("%s in (%s)", lsrRow.getKey(), Joiner.on(",").join(lsrRow.getValues())));
							break;
						case "NotIn":
							labelsList.add(String.format("%s notin (%s)", lsrRow.getKey(), Joiner.on(",").join(lsrRow.getValues())));
							break;
						case "Exists":
							labelsList.add(String.format("%s", lsrRow.getKey()));
							break;
						case "DoesNotExist":
							labelsList.add(String.format("!%s", lsrRow.getKey()));
							break;
					}
				}
			}
			labels = labelsList.stream().collect(Collectors.joining(","));
		}

		return labels;
	}

	public static String commonAddonSearchLabel (String chartType) throws Exception {
		String label = String.format("%s!=%s", KubeConstants.LABELS_ADDON_CHART_KEY, KubeConstants.LABELS_ADDON_CHART_VALUE_COCKTAIL);
		if (StringUtils.isNotBlank(chartType)) {
			label = String.format("%s,%s=%s", label, KubeConstants.LABELS_ADDON_CHART_KEY, chartType);
		}

		return label;
	}
}