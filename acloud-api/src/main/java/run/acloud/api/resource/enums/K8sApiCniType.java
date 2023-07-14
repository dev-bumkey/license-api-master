package run.acloud.api.resource.enums;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import run.acloud.api.configuration.constants.AddonConstants;
import run.acloud.api.resource.constants.KubeConstants;
import run.acloud.commons.enums.EnumCode;
import run.acloud.commons.util.JsonUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public enum K8sApiCniType implements EnumCode {

	SRIOV("sriov", AddonConstants.CHART_NAME_SR_IOV, K8sApiCniType.getAnnoMap("sriov"), "{\"type\":\"sriov\",\"cniVersion\":\"0.3.1\",\"deviceID\": \"\",\"name\":\"sriov-net\",\"ipam\":{\"type\":\"host-local\",\"rangeStart\":\"192.0.0.10\",\"rangeEnd\":\"192.0.0.15\",\"subnet\":\"192.0.0.0/24\",\"routes\":[{\"dst\":\"0.0.0.0/0\"}],\"gateway\":\"192.0.0.1\"}}")
	;

	@Getter
	private String type;
	@Getter
	private String chartName;
	@Getter
	private Map<String, String> annotations;
	@Getter
	private String example;

	K8sApiCniType(String type, String chartName, Map<String, String> annotations, String example){
		this.type = type;
		this.chartName = chartName;
		this.annotations = annotations;
		this.example = example;
	}

	@Override
	public String getCode() {
		return this.name();
	}

	public Map<String, Object> toMap() {
		Map<String, Object> cniMap = new HashMap<>();
		cniMap.put("name", this.getCode());
		cniMap.put("type", this.getType());
		cniMap.put("chartName", this.getChartName());
		cniMap.put("annotations", this.getAnnotations());
		cniMap.put("example", JsonUtils.toPrettyString(JsonUtils.toJsonObject(this.getExample())));

		return cniMap;
	}

	public static List<Map<String, Object>> toAllList() {
//		List<Map<String, String>> k8sApiCniTypes = Arrays.stream(K8sApiCniType.values()).reduce(new ArrayList<>(), (list, div) -> {
//			list.add(new HashMap<String, String>() {{
//				put("name", div.getCode());
//				put("type", div.getType());
//				put("example", div.getExample());
//			}});
//			return list;
//		}, (a, b) -> a);
//
//		return k8sApiCniTypes;

		return Arrays.stream(K8sApiCniType.values()).map(a -> a.toMap()).collect(Collectors.toList());
	}

	private static Map<String, String> getAnnoMap(String type) {
		Map<String, String> annotations = new HashMap<>();
		if (StringUtils.equals("sriov", type)) {
			annotations.put(KubeConstants.META_ANNOTATIONS_CNI_RESOURCE_NAME, "intel.com/intel_sriov");
		}

		return annotations;
	}

	public static boolean containType(String type) {
		return Arrays.stream(K8sApiCniType.values()).filter(ac -> (StringUtils.equals(ac.getType(), type))).findFirst().isPresent();
	}
}
