package run.acloud.api.resource.enums;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import run.acloud.api.resource.util.ResourceUtil;
import run.acloud.commons.enums.EnumCode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public enum K8sApiVerType implements EnumCode {

	V1_6 ( "1.6", "N"),
	V1_7 ( "1.7", "N"),
	V1_8 ( "1.8", "N"),
	V1_9 ( "1.9", "N"),
	V1_10("1.10", "N"),
	V1_11("1.11", "N"),
	V1_12("1.12", "N"),
	V1_13("1.13", "N"),
	V1_14("1.14", "Y"),
	V1_15("1.15", "Y"),
	V1_16("1.16", "Y"),
	V1_17("1.17", "Y"),
	V1_18("1.18", "Y"),
	V1_19("1.19", "Y"),
	V1_20("1.20", "Y"),
	V1_21("1.21", "Y"),
	V1_22("1.22", "Y"),
	V1_23("1.23", "Y"),
	V1_24("1.24", "Y"),
	V1_25("1.25", "Y"),
	V1_26("1.26", "Y"),
	V1_27("1.27", "Y"),
	V1_28("1.28", "Y"),
	V1_29("1.29", "Y"),
	V1_30("1.30", "Y"),
	V1_31("1.31", "Y"),
	V1_32("1.32", "Y"),
	V1_33("1.33", "Y"),
	V1_34("1.34", "Y"),
	V1_35("1.35", "Y")

	;

	@Getter
	private String version;

	@Getter
	private String useYn;

	K8sApiVerType(String version, String useYn) {
		this.version = version;
		this.useYn = useYn;
	}

	public static K8sApiVerType getApiVerType(String version){
		String matchVersion = ResourceUtil.getMatchVersion(version);
		return Arrays.stream(K8sApiVerType.values())
				.filter(v -> (BooleanUtils.toBoolean(v.getUseYn()) && StringUtils.startsWith(v.getVersion(), matchVersion)))
				.findFirst()
				.orElseGet(() ->null);
	}

	public static List<String> getAllApiVersions(){
		return Arrays.stream(K8sApiVerType.values()).filter(v -> (BooleanUtils.toBoolean(v.getUseYn()))).map(K8sApiVerType::getVersion).collect(Collectors.toList());
	}

	public static EnumSet<K8sApiVerType> getAllApiVersionsToEnumSet(){
		EnumSet<K8sApiVerType> k8sApiVerTypeSet = EnumSet.noneOf(K8sApiVerType.class);
		k8sApiVerTypeSet.addAll(Arrays.stream(K8sApiVerType.values()).filter(v -> (BooleanUtils.toBoolean(v.getUseYn()))).collect(Collectors.toList()));
		return k8sApiVerTypeSet;
	}

	public static EnumSet<K8sApiVerType> getSupportApiVersionUpto(K8sApiVerType baseVersion){
		EnumSet<K8sApiVerType> k8sApiVerTypeSet = EnumSet.noneOf(K8sApiVerType.class);
		if (baseVersion != null) {
			Integer majorBaseVersion = Integer.parseInt(StringUtils.substringBefore(baseVersion.getVersion(), "."));
			Integer minorBaseVersion = Integer.parseInt(StringUtils.substringAfter(baseVersion.getVersion(), "."));
			for (K8sApiVerType k8sApiVerTypeRow : K8sApiVerType.getAllApiVersionsToEnumSet()) {
				try {
					if (Integer.parseInt(StringUtils.substringBefore(k8sApiVerTypeRow.getVersion(), "."))
							>= majorBaseVersion) {
						if (Integer.parseInt(StringUtils.substringAfter(k8sApiVerTypeRow.getVersion(), "."))
								>= minorBaseVersion) {
							k8sApiVerTypeSet.add(k8sApiVerTypeRow);
						}
					}
				} catch (NumberFormatException e) {
					log.error("K8sApiVerType.getSupportApiVersionUpto() baseVersion parseInt error!!", e);
				}
			}
		}

		return k8sApiVerTypeSet;
	}

	/**
	 * support version range enumSet
	 *
	 * @param fromVersion
	 * @param toVersion
	 * @return
	 */
	public static EnumSet<K8sApiVerType> getSupportApiVersionRange(K8sApiVerType fromVersion, K8sApiVerType toVersion){
		EnumSet<K8sApiVerType> k8sApiVerTypeSet = EnumSet.noneOf(K8sApiVerType.class);
		if (fromVersion != null && toVersion != null) {
			Integer majorFromVersion = Integer.parseInt(StringUtils.substringBefore(fromVersion.getVersion(), "."));
			Integer minorFromVersion = Integer.parseInt(StringUtils.substringAfter(fromVersion.getVersion(), "."));
			Integer majorToVersion = Integer.parseInt(StringUtils.substringBefore(toVersion.getVersion(), "."));
			Integer minorToVersion = Integer.parseInt(StringUtils.substringAfter(toVersion.getVersion(), "."));
			for (K8sApiVerType k8sApiVerTypeRow : K8sApiVerType.getAllApiVersionsToEnumSet()) {
				try {
					// fromVersion major 비교
					if (Integer.parseInt(StringUtils.substringBefore(k8sApiVerTypeRow.getVersion(), "."))
							>= majorFromVersion) {
						// fromVersion minor 비교
						if (Integer.parseInt(StringUtils.substringAfter(k8sApiVerTypeRow.getVersion(), "."))
								>= minorFromVersion) {
							// toVersion major 비교
							if (Integer.parseInt(StringUtils.substringBefore(k8sApiVerTypeRow.getVersion(), "."))
									<= majorToVersion) {
								// toVersion minor 비교
								if (Integer.parseInt(StringUtils.substringAfter(k8sApiVerTypeRow.getVersion(), "."))
										<= minorToVersion) {
									k8sApiVerTypeSet.add(k8sApiVerTypeRow);
								}
							}
						}
					}
				} catch (NumberFormatException e) {
					log.error("K8sApiVerType.getSupportApiVersionRange() fromVersion or toVersion parseInt error!!", e);
				}
			}
		}

		return k8sApiVerTypeSet;
	}

	/**
	 * k8sVersion이 apiVerType 기준에 해당하는 지 체크
	 * apiVerType 가 없다면 전체 체크
	 *
	 * @param k8sVersion - x.x.x 형식 ( e.g. 1.14.9 )
	 * @param apiVerType
	 * @return
	 */
	public static boolean isK8sVerSupported(String k8sVersion, K8sApiVerType apiVerType) {
		List<String> apiVersionList = new ArrayList<>();
		boolean isSupported = false;
		if (apiVerType != null) {
			EnumSet<K8sApiVerType> supportVerType = K8sApiVerType.getSupportApiVersionUpto(apiVerType);
			if (supportVerType != null) {
				apiVersionList = supportVerType.stream().map(K8sApiVerType::getVersion).collect(Collectors.toList());
			}
		} else {
			apiVersionList = K8sApiVerType.getAllApiVersions();
		}

		if (CollectionUtils.isNotEmpty(apiVersionList)) {
			if (ResourceUtil.getK8sSupported(k8sVersion, apiVersionList)) {
				isSupported = true;
			}
		}

		return isSupported;
	}


	@Override
	public String getCode() {
		return this.name();
	}
}
